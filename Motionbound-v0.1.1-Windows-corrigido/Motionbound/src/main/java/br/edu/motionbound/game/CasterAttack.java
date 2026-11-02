package br.edu.motionbound.game;

/** The prepared action is saved explicitly, so loading cannot retarget or restart a volley. */
public enum CasterAttack {
    NONE, POISON_VIAL, SLOW_VIAL, QUICK_SLOW, FIRE_FAN, LIGHTNING_FAN, RIFT_VIAL;
    public PotionEffect potion() {
        return switch(this) {
            case POISON_VIAL -> PotionEffect.POISON;
            case SLOW_VIAL, QUICK_SLOW -> PotionEffect.SLOW;
            case FIRE_FAN -> PotionEffect.FIRE;
            case RIFT_VIAL -> PotionEffect.RIFT;
            default -> null;
        };
    }
}
