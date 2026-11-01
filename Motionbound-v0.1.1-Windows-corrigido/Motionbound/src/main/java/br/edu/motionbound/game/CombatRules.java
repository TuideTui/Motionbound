package br.edu.motionbound.game;

import com.badlogic.gdx.math.Vector2;

/** Geometry shared by mouse and assisted sensor input, independent of the rendered sprite. */
public final class CombatRules {
    public static final float REACH=28*1.15f, HALF_ARC_DEGREES=60;
    public static final int MAGIC_DAMAGE=1;
    public static final float MAGIC_KNOCKBACK=240;
    private static final float MIN_DOT=(float)Math.cos(Math.toRadians(HALF_ARC_DEGREES));
    private CombatRules() { }
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
