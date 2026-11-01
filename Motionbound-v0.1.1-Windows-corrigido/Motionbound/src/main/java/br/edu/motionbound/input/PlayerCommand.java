package br.edu.motionbound.input;

import com.badlogic.gdx.math.Vector2;

/** Hardware-independent controls; an Arduino controller can implement this same API. */
public final class PlayerCommand {
    public final Vector2 direction = new Vector2();
    /** Screen coordinates for mouse input, or a direction for a sensor source. */
    public final Vector2 aim = new Vector2();
    public boolean cursorAim;
    public boolean assistedAim;
    public boolean sprint;
    public boolean attack;
    public boolean magic;
    public boolean heal;
}
