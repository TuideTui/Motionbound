package br.edu.motionbound.input;

/** Sustained directional intent. Uses game time, never time spent paused/in menus. */
public final class TiltSprint {
    public static final float DELAY_SECONDS=4,START_THRESHOLD=.60f,RELEASE_THRESHOLD=.40f,DIRECTION_DOT=.65f;
    private double seconds;
    private float anchorX,anchorY;
    private boolean needsRelease;

    public boolean update(float x,float y,float delta) {
        if(!Float.isFinite(x)||!Float.isFinite(y)||!Float.isFinite(delta)||delta<0){reset();return false;}
        float strength=Math.max(Math.abs(x),Math.abs(y));
        if(strength<RELEASE_THRESHOLD){reset();return false;}
        if(needsRelease)return false;
        float length=(float)Math.hypot(x,y);
        if(seconds>0&&(x*anchorX+y*anchorY)/length<DIRECTION_DOT)clearCharge();
        if(seconds==0) {
            if(strength<START_THRESHOLD)return false;
            if(Math.abs(x)>=Math.abs(y)){anchorX=Math.signum(x);anchorY=0;}
            else{anchorX=0;anchorY=Math.signum(y);}
        }
        seconds=Math.min(DELAY_SECONDS,seconds+delta);
        if(seconds>=DELAY_SECONDS-.000001)seconds=DELAY_SECONDS;
        return active();
    }
    public boolean active(){return seconds>=DELAY_SECONDS&&!needsRelease;}
    public float progress(){return(float)(seconds/DELAY_SECONDS);}
    public boolean needsRelease(){return needsRelease;}
    public void exhaust(){clearCharge();needsRelease=true;}
    private void clearCharge(){seconds=0;anchorX=anchorY=0;}
    public void reset(){clearCharge();needsRelease=false;}

    /** Campaign totals; only the Edge's fresh, consecutive samples feed this metric. */
    public static final class Consistency {
        public double[] seconds=new double[5],inBand=new double[5];
        public int[] samples=new int[5];
        public float referenceRange=.35f;
        private transient int stage;
        private transient double window;
        private transient float xAxis,yAxis;
        private transient boolean armed=true;

        public void clear(float range) {
            referenceRange=range;
            java.util.Arrays.fill(seconds,0);java.util.Arrays.fill(inBand,0);java.util.Arrays.fill(samples,0);
            interrupt();
        }
        public void beginStage(int number) {
            stage=number-1;seconds[stage]=inBand[stage]=0;samples[stage]=0;interrupt();
        }
        public void interrupt(){window=0;armed=true;xAxis=yAxis=0;}
        public void sample(float x,float y,double delta,float startRange,boolean canStart) {
            if(!Float.isFinite(x)||!Float.isFinite(y)||!Double.isFinite(delta)||delta<=0||delta>.1)return;
            float strength=Math.max(Math.abs(x),Math.abs(y)),length=(float)Math.hypot(x,y);
            float alignment=length>0?(x*xAxis+y*yAxis)/length:0;
            if(window==0) {
                if(strength/startRange<RELEASE_THRESHOLD||alignment<DIRECTION_DOT)armed=true;
                if(!armed||!canStart||strength/startRange<START_THRESHOLD)return;
                if(Math.abs(x)>=Math.abs(y)){xAxis=Math.signum(x);yAxis=0;}
                else{xAxis=0;yAxis=Math.signum(y);}
                window=DELAY_SECONDS;armed=false;
                alignment=length>0?(x*xAxis+y*yAxis)/length:0;
            }
            double duration=Math.min(delta,window);
            seconds[stage]+=duration;samples[stage]++;
            if(strength/referenceRange>=START_THRESHOLD&&alignment>=DIRECTION_DOT)inBand[stage]+=duration;
            window=Math.max(0,window-duration);
            if(window<.000001)window=0;
        }
        public int score(int number) {
            int index=number-1;
            return seconds[index]+.000001<DELAY_SECONDS||samples[index]<100?-1:
                    Math.max(0,Math.min(100,(int)Math.round(100*inBand[index]/seconds[index])));
        }
        public void copyFrom(Consistency other,int number) {
            System.arraycopy(other.seconds,0,seconds,0,5);System.arraycopy(other.inBand,0,inBand,0,5);
            System.arraycopy(other.samples,0,samples,0,5);referenceRange=other.referenceRange;stage=number-1;interrupt();
        }
        public void validate() {
            if(seconds==null||inBand==null||samples==null||seconds.length!=5||inBand.length!=5||samples.length!=5
                    ||!Float.isFinite(referenceRange)||referenceRange<.23f||referenceRange>.50f)
                throw new IllegalArgumentException("Invalid motor metric");
            for(int i=0;i<5;i++)if(!Double.isFinite(seconds[i])||!Double.isFinite(inBand[i])||seconds[i]<0
                    ||seconds[i]>31536000||inBand[i]<0||inBand[i]>seconds[i]+.000001||samples[i]<0
                    ||seconds[i]>samples[i]*.1+.000001||(samples[i]==0)!=(seconds[i]==0))
                throw new IllegalArgumentException("Invalid motor totals");
        }
    }
}
