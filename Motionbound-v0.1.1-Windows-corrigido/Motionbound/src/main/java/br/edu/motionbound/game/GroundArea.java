package br.edu.motionbound.game;

/** A vial's persistent footprint. The acquired status has its own independent timer. */
public final class GroundArea {
    public static final float DURATION=5, RADIUS=14;
    public final float x,y;
    public final PotionEffect effect;
    public float remaining=DURATION;
    /** A rift can teleport again, but each vial summons its bats only once. */
    public boolean summoned;
    public GroundArea(float x,float y,PotionEffect effect) {this.x=x;this.y=y;this.effect=effect;}
}
