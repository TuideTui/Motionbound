package br.edu.motionbound.game;

import com.badlogic.gdx.math.Vector2;

/** Geometry shared by mouse and assisted sensor input, independent of the rendered sprite. */
public final class CombatRules {
    public static final float REACH=28*1.15f, HALF_ARC_DEGREES=60;
    public static final int MAGIC_DAMAGE=1;
    public static final float MAGIC_KNOCKBACK=240;
    private static final float MIN_DOT=(float)Math.cos(Math.toRadians(HALF_ARC_DEGREES));
    private CombatRules() { }
    public static float fanOffset(int shot,int count) {
        if(count<2||shot<0||shot>=count)throw new IllegalArgumentException("Invalid fan shot");
        return -22.5f+45f*shot/(count-1);
    }
    public static Vector2 clippedLanding(DungeonMap map,float x,float y,float angle,float range,Vector2 output) {
        output.set(x,y);
        for(float distance=2;distance<=range;distance+=2) {
            float tx=x+com.badlogic.gdx.math.MathUtils.cosDeg(angle)*distance,ty=y+com.badlogic.gdx.math.MathUtils.sinDeg(angle)*distance;
            if(!map.canStand(tx,ty,2))break;
            output.set(tx,ty);
        }
        return output;
    }
    public static float segmentDistance2(float ax,float ay,float bx,float by,float x,float y) {
        float dx=bx-ax,dy=by-ay,length=dx*dx+dy*dy;
        float t=length==0?0:com.badlogic.gdx.math.MathUtils.clamp(((x-ax)*dx+(y-ay)*dy)/length,0,1);
        return Vector2.dst2(x,y,ax+dx*t,ay+dy*t);
    }
    public static boolean inArc(float dx,float dy,Vector2 direction) {
        float squared=dx*dx+dy*dy;
        return squared<=REACH*REACH&&(squared<.01f||(dx*direction.x+dy*direction.y)/Math.sqrt(squared)>=MIN_DOT);
    }
    public static boolean clearPath(DungeonMap map,float ax,float ay,float bx,float by) {
        int steps=Math.max(1,(int)Math.ceil(Vector2.dst(ax,ay,bx,by)/2));
        for(int i=0;i<=steps;i++) {
            float t=i/(float)steps;
            if(!map.canStand(ax+(bx-ax)*t,ay+(by-ay)*t,0))return false;
        }
        return true;
    }
}
