package br.edu.motionbound.input;

import com.badlogic.gdx.math.Vector2;

/** Hardware-independent controls; an Arduino controller can implement this same API. */
public final class PlayerCommand {
    public final Vector2 direction = new Vector2();
    public boolean sprint;
    public boolean attack;
    public boolean magic;
    public boolean heal;
}
