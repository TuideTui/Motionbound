package br.edu.motionbound.game;

/** Exact half-heart ticks, independent of rendering. A second damaging vial never resets/extends a dose. */
public final class StatusEffects {
    public PotionEffect damageEffect;
    public int ticksRemaining;
    public float untilTick,slowRemaining,riftRemaining;
    public boolean apply(PotionEffect effect) {
        if(effect==PotionEffect.FIRE||effect==PotionEffect.POISON) {
            if(ticksRemaining>0)return false;
            damageEffect=effect;ticksRemaining=4;untilTick=1;return true;
        }
        if(effect==PotionEffect.SLOW) {if(slowRemaining>0)return false;slowRemaining=4;return true;}
        if(riftRemaining>0)return false;
        riftRemaining=1.4f;return true;
    }
    public float advance(float delta) {
        if(!Float.isFinite(delta)||delta<0)throw new IllegalArgumentException("Invalid effect delta");
        slowRemaining=Math.max(0,slowRemaining-delta);riftRemaining=Math.max(0,riftRemaining-delta);
        float damage=0;
        if(ticksRemaining>0) {
            untilTick-=delta;
            while(ticksRemaining>0&&untilTick<=0) {damage+=.5f;ticksRemaining--;untilTick+=1;}
            if(ticksRemaining==0)cleanseDamage();
        }
        return damage;
    }
    public float damageRemaining() {return ticksRemaining==0?0:ticksRemaining-1+untilTick;}
    public float movementScale() {return slowRemaining>0?.60f:1;}
    public void cleanseDamage() {damageEffect=null;ticksRemaining=0;untilTick=0;}
    public void copyFrom(StatusEffects other) {
        damageEffect=other.damageEffect;ticksRemaining=other.ticksRemaining;untilTick=other.untilTick;
        slowRemaining=other.slowRemaining;riftRemaining=other.riftRemaining;
    }
    public void validate() {
        if(ticksRemaining<0||ticksRemaining>4||!range(untilTick,1)||!range(slowRemaining,4)||!range(riftRemaining,1.4f)
                ||ticksRemaining==0&&(damageEffect!=null||untilTick!=0)
                ||ticksRemaining>0&&(damageEffect!=PotionEffect.FIRE&&damageEffect!=PotionEffect.POISON))
            throw new IllegalArgumentException("Efeitos inválidos no save");
    }
    private static boolean range(float value,float max) {return Float.isFinite(value)&&value>=0&&value<=max;}
}
