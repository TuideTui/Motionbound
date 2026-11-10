package br.edu.motionbound.input;

/** Sustained directional intent. Uses game time, never time spent paused/in menus. */
public final class TiltSprint {
    public static final float DELAY_SECONDS=4,START_THRESHOLD=.60f,RELEASE_THRESHOLD=.40f;
    private double seconds;
    private float anchorX,anchorY;
    private boolean needsRelease;

    public boolean update(float x,float y,float delta) {
        if(!Float.isFinite(x)||!Float.isFinite(y)||!Float.isFinite(delta)||delta<0){reset();return false;}
        float strength=Math.max(Math.abs(x),Math.abs(y));
        if(strength<RELEASE_THRESHOLD){reset();return false;}
        if(needsRelease)return false;
        float length=(float)Math.hypot(x,y);
        if(seconds>0&&(x*anchorX+y*anchorY)/length<.65f)clearCharge();
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
}
