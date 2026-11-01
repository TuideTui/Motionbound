package br.edu.motionbound.game;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;

/** One locked direction per swing and one impact event, even across large simulation steps. */
public final class MeleeSwing {
    public static final float DURATION=.36f, IMPACT=.16f, COOLDOWN=.40f;
    public final Vector2 direction=new Vector2(0,-1);
    public float elapsed=-1;
    public boolean impacted;
    public boolean active() {return elapsed>=0&&elapsed<DURATION;}
    public void begin(Vector2 aim) {direction.set(aim).nor();elapsed=0;impacted=false;}
    public boolean advance(float delta) {
        if(!active())return false;
        elapsed=Math.min(DURATION,elapsed+Math.max(0,delta));
        if(!impacted&&elapsed>=IMPACT) {impacted=true;return true;}
        return false;
    }
    public int pose() {return !active()?0:elapsed<.12f?3:elapsed<.24f?4:5;}
    public float bladeAngle() {
        float base=direction.angleDeg();
        if(elapsed<.10f)return base+65;
        return base+MathUtils.lerp(65,-65,MathUtils.clamp((elapsed-.10f)/.14f,0,1));
    }
    public float trail() {return active()&&elapsed>=.12f&&elapsed<.26f?1-(elapsed-.12f)/.14f:0;}
}
