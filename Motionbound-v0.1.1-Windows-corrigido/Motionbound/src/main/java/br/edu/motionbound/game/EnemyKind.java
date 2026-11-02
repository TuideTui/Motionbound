package br.edu.motionbound.game;

/** Stable IDs are also used by checkpoints; the first two retain the original enemy IDs. */
public enum EnemyKind {
    SLIME(2,27,.45f,42), SKELETON(3,37,.48f,27), MAGE(2,25,.8f,128),
    WARLOCK(3,23,.95f,140), CYCLOPS(4,21,.85f,34), GHOST(2,31,.6f,30), BAT(1,49,.4f,38);
    public final int maxHp;
    public final float speed,windup,attackRange;
    EnemyKind(int hp,float speed,float windup,float range) {maxHp=hp;this.speed=speed;this.windup=windup;attackRange=range;}
    public boolean caster() {return this==MAGE||this==WARLOCK;}
    private static final EnemyKind[] KINDS=values();
    public static EnemyKind of(int id) {
        if(id<0||id>=KINDS.length)throw new IllegalArgumentException("Tipo de inimigo inválido");
        return KINDS[id];
    }
}
