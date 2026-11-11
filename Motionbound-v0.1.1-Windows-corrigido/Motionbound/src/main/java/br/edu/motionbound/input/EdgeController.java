package br.edu.motionbound.input;

import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.math.MathUtils;
import com.fazecast.jSerialComm.SerialPort;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ArrayBlockingQueue;

/** USB work runs off the render thread. All calibration/input state belongs to the render thread. */
public final class EdgeController implements ControlSource,AutoCloseable {
    public static final long STALE_NS=500_000_000L;
    private record Received(EdgeFrame frame,long time){}
    private final ArrayBlockingQueue<Received> queue=new ArrayBlockingQueue<>(64);
    private final Preferences preferences;
    private final TiltSprint sprint=new TiltSprint();
    public final TiltSprint.Consistency consistency=new TiltSprint.Consistency();
    private final java.util.ArrayDeque<Received> motorSamples=new java.util.ArrayDeque<>();
    private Received motorPrevious;
    private volatile boolean running;
    private volatile SerialPort port;
    private volatile String transport="Conecte a Edge pelo USB";
    private Thread worker;
    private EdgeFrame previous;
    private long lastTime,now,calibrationStarted;
    private boolean armed,calibrated,calibrating,attack,magic;
    private int pressed,samples;
    private double sumX,sumY,sumXX,sumYY;
    private float neutralX,neutralY,filteredX,filteredY;
    private String calibrationMessage="Calibre com a mão na posição confortável";
    public boolean swapAxes,invertX,invertY;
    public int sensitivity=1;

    public EdgeController(Preferences preferences) {
        this.preferences=preferences;
        if(preferences!=null){swapAxes=preferences.getBoolean("swap",false);invertX=preferences.getBoolean("invertX",false);
            invertY=preferences.getBoolean("invertY",false);sensitivity=MathUtils.clamp(preferences.getInteger("sensitivity",1),0,2);}
    }
    public void start() {
        if(running||Boolean.getBoolean("motionbound.usb.disabled"))return;
        running=true;worker=new Thread(this::receive,"Motionbound-USB");worker.setDaemon(true);worker.start();
    }
    public void update(long time) {
        motorSamples.clear();
        now=time;pressed=0;attack=magic=false;
        Received received;
        while((received=queue.poll())!=null)if(time-received.time<=STALE_NS)accept(received.frame,received.time);
        now=time;
        if(!connected()){calibrated=calibrating=false;armed=false;filteredX=filteredY=0;pressed=0;attack=magic=false;sprint.reset();interruptMotor();}
    }
    /** Also used by deterministic protocol tests, without opening a physical port. */
    public void accept(EdgeFrame frame,long time) {
        now=time;
        boolean first=previous==null||frame.boot()!=previous.boot()||time-lastTime>STALE_NS;
        if(!first){long distance=EdgeFrame.distance(frame.sequence(),previous.sequence());if(distance==0||distance>0x7fffffffL)return;}
        if(first){calibrated=calibrating=false;armed=false;filteredX=filteredY=0;pressed=0;attack=magic=false;sprint.reset();}
        if(frame.flags()!=7){calibrated=calibrating=false;armed=false;pressed=0;attack=magic=false;sprint.reset();}
        if(first||frame.flags()!=7)interruptMotor();
        if(!first&&armed&&frame.flags()==7) {
            pressed|=frame.buttons()&~previous.buttons();
            long attacks=EdgeFrame.distance(frame.attacks(),previous.attacks()),casts=EdgeFrame.distance(frame.magic(),previous.magic());
            attack|=attacks>0&&attacks<=2;magic|=casts>0&&casts<=2;
        }
        previous=frame;lastTime=time;
        if(ready()){if(motorSamples.size()==64)motorSamples.removeFirst();motorSamples.addLast(new Received(frame,time));}
        if(frame.buttons()==0&&frame.flags()==7)armed=true;
        if(calibrating&&frame.flags()==7) {
            double norm=frame.x()*frame.x()+frame.y()*frame.y()+frame.z()*frame.z();
            if(norm<.5||norm>1.7){beginCalibration();return;}
            if(samples++==0)calibrationStarted=time;
            sumX+=frame.x();sumY+=frame.y();sumXX+=frame.x()*frame.x();sumYY+=frame.y()*frame.y();
            if(time-calibrationStarted>=2_000_000_000L&&samples>=60) {
                double x=sumX/samples,y=sumY/samples;
                if(sumXX/samples-x*x>.0025||sumYY/samples-y*y>.0025){beginCalibration();calibrationMessage="Mantenha a mão parada por 2 segundos";}
                else{neutralX=(float)x;neutralY=(float)y;calibrating=false;calibrated=true;reset();calibrationMessage="Posição neutra registrada. Pronto para jogar!";}
            }
        }
    }
    public boolean connected(){return previous!=null&&previous.flags()==7&&now-lastTime<=STALE_NS;}
    public boolean detected(){return port!=null||previous!=null;}
    public boolean ready(){return connected()&&calibrated&&!calibrating;}
    public boolean calibrating(){return calibrating;}
    public float progress(){return calibrating&&samples>0?MathUtils.clamp((now-calibrationStarted)/2_000_000_000f,0,1):0;}
    public int pressed(){return pressed;}
    public void offer(EdgeFrame frame,long time){if(!queue.offer(new Received(frame,time))){queue.clear();queue.offer(new Received(frame,time));}}
    public String status(){return connected()?"Edge conectada / "+(port==null?"USB":port.getSystemPortName()):previous!=null&&previous.flags()!=7?"Falha em um sensor. Confira os cabos.":transport;}
    public String calibrationMessage(){return calibrationMessage;}
    public float tiltX(){return previous==null?0:previous.x()-neutralX;}
    public float tiltY(){return previous==null?0:previous.y()-neutralY;}
    public float sprintProgress(){return sprint.progress();}
    public boolean sprintNeedsRelease(){return sprint.needsRelease();}
    public void exhaustSprint(){sprint.exhaust();}
    public void beginCalibration(){calibrated=false;calibrating=connected();samples=0;sumX=sumY=sumXX=sumYY=0;calibrationStarted=now;reset();calibrationMessage="Mantenha a mão confortável e parada por 2 segundos";}
    public void saveSettings(){reset();if(preferences!=null)preferences.putBoolean("swap",swapAxes).putBoolean("invertX",invertX)
            .putBoolean("invertY",invertY).putInteger("sensitivity",sensitivity).flush();}
    public String sensitivityLabel(){return sensitivity==0?"Suave (mais inclinação)":sensitivity==1?"Média":"Alta (menos inclinação)";}
    public float tiltRange(){return sensitivity==0?.50f:sensitivity==1?.35f:.23f;}
    @Override public PlayerCommand read(float delta) {
        PlayerCommand command=new PlayerCommand();command.assistedAim=true;
        if(!ready()){sprint.reset();return command;}
        float x=tiltX(),y=tiltY();if(swapAxes){float t=x;x=y;y=t;}if(invertX)x=-x;if(invertY)y=-y;
        float range=tiltRange();
        for(Received sample:motorSamples) {
            if(motorPrevious!=null&&sample.frame.boot()==motorPrevious.frame.boot()
                    &&EdgeFrame.distance(sample.frame.sequence(),motorPrevious.frame.sequence())==1) {
                double duration=(sample.time-motorPrevious.time)/1_000_000_000d;
                float mx=sample.frame.x()-neutralX,my=sample.frame.y()-neutralY;
                if(swapAxes){float t=mx;mx=my;my=t;}if(invertX)mx=-mx;if(invertY)my=-my;
                consistency.sample(mx,my,duration,range,!sprint.needsRelease());
            }
            motorPrevious=sample;
        }
        motorSamples.clear();
        float smoothing=1-(float)Math.exp(-14*Math.max(0,delta));
        filteredX=MathUtils.lerp(filteredX,axis(x,range),smoothing);filteredY=MathUtils.lerp(filteredY,axis(y,range),smoothing);
        command.direction.set(filteredX,filteredY).limit(1);command.aim.set(command.direction);
        command.sprint=sprint.update(x/range,y/range,delta);
        command.attack=attack;command.magic=magic;attack=magic=false;return command;
    }
    private static float axis(float value,float range){return Math.abs(value)<=.07f?0:Math.signum(value)*MathUtils.clamp((Math.abs(value)-.07f)/(range-.07f),0,1);}
    private void interruptMotor(){motorSamples.clear();motorPrevious=null;consistency.interrupt();}
    @Override public void reset(){attack=magic=false;filteredX=filteredY=0;sprint.reset();interruptMotor();}

    private void receive() {
        try {
            while(running) {
                SerialPort selected=null;
                for(SerialPort candidate:SerialPort.getCommPorts())if(candidate.getVendorID()==0x10c4&&candidate.getProductID()==0xea60){selected=candidate;break;}
                if(selected==null){transport="Conecte a Edge pelo USB";Thread.sleep(700);continue;}
                selected.setComPortParameters(115200,8,SerialPort.ONE_STOP_BIT,SerialPort.NO_PARITY);
                selected.setFlowControl(SerialPort.FLOW_CONTROL_DISABLED);
                selected.setComPortTimeouts(SerialPort.TIMEOUT_NONBLOCKING,0,0);
                selected.clearDTR();selected.clearRTS();
                if(!selected.openPort()){transport="Feche o Monitor Serial para liberar "+selected.getSystemPortName();Thread.sleep(900);continue;}
                port=selected;transport="Conectada. Aguardando firmware Motionbound...";
                try {
                    byte[] bytes=new byte[512],request="MB_START\n".getBytes(StandardCharsets.US_ASCII);
                    StringBuilder line=new StringBuilder();boolean discard=false;long requested=0,lastReceived=System.nanoTime();
                    while(running&&selected.isOpen()) {
                        long stamp=System.nanoTime();
                        if(stamp-requested>1_000_000_000L){selected.writeBytes(request,request.length);requested=stamp;}
                        int count=selected.readBytes(bytes,bytes.length);if(count<0)break;
                        for(int i=0;i<count;i++) {
                            int c=bytes[i]&255;
                            if(c=='\n') {
                                if(!discard){EdgeFrame frame=EdgeFrame.parse(line.toString());if(frame!=null){offer(frame,stamp);lastReceived=stamp;}}
                                line.setLength(0);discard=false;
                            } else if(c!='\r'){if(c<32||c>126||line.length()>=180)discard=true;else if(!discard)line.append((char)c);}
                        }
                        if(stamp-lastReceived>5_000_000_000L)transport="Grave o firmware atualizado na Edge";
                        Thread.sleep(5);
                    }
                }finally{selected.closePort();port=null;queue.clear();}
                transport="Controle desconectado. Aguardando reconexão...";Thread.sleep(500);
            }
        }catch(InterruptedException e){Thread.currentThread().interrupt();}
        catch(Throwable e){transport="USB indisponível: "+e.getClass().getSimpleName();}
        finally{SerialPort current=port;if(current!=null)current.closePort();port=null;}
    }
    @Override public void close(){running=false;Thread t=worker;if(t!=null)t.interrupt();SerialPort current=port;if(current!=null)current.closePort();}
}
