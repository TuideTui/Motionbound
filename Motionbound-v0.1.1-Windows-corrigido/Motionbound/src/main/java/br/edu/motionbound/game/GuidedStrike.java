package br.edu.motionbound.game;

import com.badlogic.gdx.math.Vector2;

/** Bounded, collision-safe assistance. A gesture locks one direction, never homes forever. */
public final class GuidedStrike {
    public static final float RANGE=CombatRules.REACH*3, STOP_DISTANCE=18, RETREAT_DISTANCE=18;
    public static final float COOLDOWN=.8f;
    public static final int IDLE=0, APPROACH=1, STRIKE=2, RETREAT=3;
    public int phase;
    public float remaining, dx=0, dy=-1;
    public boolean retreatAfter;

    public boolean active() {return phase!=IDLE;}
    public boolean moving() {return phase==APPROACH||phase==RETREAT;}
    public void reset() {phase=IDLE;remaining=0;dx=0;dy=-1;retreatAfter=false;}

    /** Returns true when the sword can begin immediately. */
    public boolean begin(float targetDx,float targetDy,Vector2 fallback) {
        float distance=Vector2.len(targetDx,targetDy);
        dx=distance>.001f?targetDx/distance:fallback.x;
        dy=distance>.001f?targetDy/distance:fallback.y;
        retreatAfter=distance<=CombatRules.REACH;
        remaining=retreatAfter?0:Math.min(RANGE-STOP_DISTANCE,distance-STOP_DISTANCE);
        phase=retreatAfter?STRIKE:APPROACH;
        return retreatAfter;
    }

    /** Substeps prevent tunnelling even on long frames. Collisions stop instead of sliding sideways. */
    public boolean advance(DungeonMap map,float x,float y,float delta,Vector2 position) {
        position.set(x,y);
        if(!moving())return false;
        float travel=Math.min(remaining,Math.max(0,delta)*(phase==APPROACH?340:180));
        float sign=phase==RETREAT?-1:1;
        while(travel>.0001f) {
            float step=Math.min(2,travel);
            float nx=position.x+dx*step*sign,ny=position.y+dy*step*sign;
            if(!map.canStand(nx,ny,5)){remaining=0;break;}
            position.set(nx,ny);remaining-=step;travel-=step;
        }
        if(remaining>.001f)return false;
        remaining=0;
        if(phase==APPROACH){phase=STRIKE;return true;}
        reset();return false;
    }

    public void impact() {
        if(phase!=STRIKE)return;
        if(retreatAfter){phase=RETREAT;remaining=RETREAT_DISTANCE;retreatAfter=false;}
        else reset();
    }

    public void copyFrom(GuidedStrike other) {
        phase=other.phase;remaining=other.remaining;dx=other.dx;dy=other.dy;retreatAfter=other.retreatAfter;
    }
    public void validate() {
        float length=dx*dx+dy*dy;
        if(phase<IDLE||phase>RETREAT||!Float.isFinite(remaining)||remaining<0
                ||remaining>RANGE-STOP_DISTANCE||!Float.isFinite(length)||Math.abs(length-1)>.01f
                ||(phase==IDLE||phase==STRIKE)&&remaining!=0
                ||phase==RETREAT&&remaining>RETREAT_DISTANCE
                ||retreatAfter&&phase!=STRIKE)
            throw new IllegalArgumentException("Invalid guided strike");
    }
}
