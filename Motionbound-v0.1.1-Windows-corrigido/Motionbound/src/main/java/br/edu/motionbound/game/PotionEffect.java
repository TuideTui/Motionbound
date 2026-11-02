package br.edu.motionbound.game;

import com.badlogic.gdx.graphics.Color;

public enum PotionEffect {
    FIRE("FOGO",115,"f28b46"), POISON("VENENO",114,"83c75d"),
    SLOW("LENTIDÃO",113,"bcc0cf"), RIFT("FENDA",116,"76c7ff");
    public final String label;
    public final int tile;
    public final Color color;
    PotionEffect(String label,int tile,String color) {this.label=label;this.tile=tile;this.color=Color.valueOf(color);}
}
