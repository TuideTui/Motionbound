package br.edu.motionbound;

import br.edu.motionbound.input.EdgeController;
import br.edu.motionbound.input.EdgeFrame;
import br.edu.motionbound.input.PlayerCommand;

public final class EdgeControllerTest {
    private static long time=1_000_000_000L,sequence;
    private static EdgeFrame frame(float x,float y,int buttons,long attacks,long magic){return new EdgeFrame(1,sequence++,x,y,1,buttons,attacks,magic,7);}
    private static void accept(EdgeController c,float x,float y,int b,long a,long m){time+=20_000_000;c.accept(frame(x,y,b,a,m),time);}
    private static void require(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    public static void main(String[] args) {
        TiltSprintTest.verify();
        verifyMotorSamples();
        require(EdgeFrame.parse("MB1;1;2;0.1;-0.2;1;0;0;0;7")!=null,"Valid firmware packet");
        for(String bad:new String[]{"[OK] APDS","MB2;1;2;0;0;1;0;0;0;7","MB1;1;2;NaN;0;1;0;0;0;7",
                "MB1;1;2;0;0;1;16;0;0;7","MB1;4294967296;2;0;0;1;0;0;0;7","MB1;1;2;0;0;1;0;0;0;7;extra"})
            require(EdgeFrame.parse(bad)==null,"Reject malformed/unidentified serial data");
        EdgeController c=new EdgeController(null);
        accept(c,0,0,4,10,5);require(c.pressed()==0&&!c.ready(),"No phantom confirm on connection");
        accept(c,0,0,0,10,5);c.beginCalibration();
        for(int i=0;i<105;i++)accept(c,.02f,-.03f,0,10,5);
        require(c.ready(),"Stable neutral calibration after two seconds");
        accept(c,.02f,-.03f,0,10,5);require(c.read(.02f).direction.isZero(),"Neutral position stays still");
        accept(c,.37f,.32f,0,11,6);PlayerCommand command=c.read(.2f);
        require(command.attack&&command.magic&&command.assistedAim&&command.direction.x>0&&command.direction.y>0&&command.direction.len()<=1.001f,"Diagonal tilt, sword and magic");
        require(!c.read(.02f).attack&&!c.read(.02f).magic,"A counter increment is consumed once");
        accept(c,.02f,-.03f,0,11,6);require(!c.read(.02f).attack,"Repeated firmware counter cannot attack again");
        c.swapAxes=true;c.invertX=true;accept(c,.37f,-.03f,0,11,6);c.reset();command=c.read(.2f);
        require(Math.abs(command.direction.x)<.01f&&command.direction.y>0,"Axis settings change movement predictably");
        c.swapAxes=false;c.invertX=false;c.reset();
        for(int i=0;i<199;i++){accept(c,.37f,-.03f,0,11,6);require(!c.read(.02f).sprint,"USB tilt must not sprint early");}
        accept(c,.37f,-.03f,0,11,6);require(c.read(.02f).sprint,"USB input requests sprint after four seconds");
        c.reset();require(c.sprintProgress()==0,"Resetting controller input clears the sprint timer");
        c.update(time+EdgeController.STALE_NS+1);require(!c.ready()&&c.read(.2f).direction.isZero(),"Stale input stops movement and invalidates calibration");
        accept(c,0,0,4,20,8);require(c.pressed()==0,"Reconnect with a held button does not navigate");
        accept(c,0,0,0,20,8);c.update(time);accept(c,0,0,2,20,8);require(c.pressed()==2,"Menu button edge works after release");
        c.update(time);accept(c,0,0,2,20,8);require(c.pressed()==0,"Holding a menu button does not skip entries");
        EdgeFrame old=frame(0,0,0,20,8);c.accept(old,time);c.update(time);c.accept(old,time);require(c.pressed()==0,"Duplicate packets are ignored");
        require(EdgeFrame.distance(0,0xffffffffL)==1,"Unsigned event counters wrap safely");
        c.beginCalibration();for(int i=0;i<105;i++)accept(c,i%2==0?.3f:-.3f,0,0,20,8);
        require(!c.ready(),"A moving hand cannot calibrate as neutral");
        c.accept(new EdgeFrame(2,0,0,0,1,4,0,0,7),time+20_000_000);
        require(!c.ready()&&c.pressed()==0,"Device reboot needs fresh calibration and release");
        c.close();System.out.println("PASS: USB framing, counters, calibration, movement, gestures, menu edges, stale data and reboot");
    }
    private static void verifyMotorSamples() {
        EdgeController c=new EdgeController(null);accept(c,0,0,0,0,0);c.beginCalibration();
        for(int i=0;i<105;i++)accept(c,0,0,0,0,0);
        c.reset();
        for(int i=0;i<201;i++){accept(c,.35f,0,0,0,0);c.read(.02f);}
        require(c.consistency.score(1)==100,"Real consecutive accelerometer frames produce the score");
        double measured=c.consistency.seconds[0];
        for(int i=0;i<300;i++)c.read(.02f);
        require(c.consistency.seconds[0]==measured,"Render frames cannot re-count the last sensor sample");
        c.reset();c.consistency.beginStage(2);
        for(int i=0;i<300;i++)accept(c,.35f,0,0,0,0);
        c.update(time);c.read(.02f);
        require(c.consistency.seconds[1]==0,"Packets received in menus cannot become measurements on resume");
        EdgeFrame duplicate=frame(.35f,0,0,0,0);time+=20_000_000;c.accept(duplicate,time);c.read(.02f);
        measured=c.consistency.seconds[1];c.accept(duplicate,time+20_000_000);c.read(.02f);
        require(c.consistency.seconds[1]==measured,"Duplicate sequence numbers do not count");
        c.update(time+EdgeController.STALE_NS+1);c.read(4);
        require(c.consistency.seconds[1]==measured,"Disconnect excludes the entire missing interval");
        c.close();
    }
}
