package br.edu.motionbound.input;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;

/** Conventional mode: movement and aim are independent. */
public final class KeyboardControlSource implements ControlSource {
    private boolean key(int a,int b) {
        return Gdx.input.isKeyPressed(a)||Gdx.input.isKeyPressed(b);
    }
    @Override public PlayerCommand read(float delta) {
        PlayerCommand c = new PlayerCommand();
        if(key(Input.Keys.A, Input.Keys.LEFT)) c.direction.x -= 1;
        if(key(Input.Keys.D, Input.Keys.RIGHT)) c.direction.x += 1;
        if(key(Input.Keys.W, Input.Keys.UP)) c.direction.y += 1;
        if(key(Input.Keys.S, Input.Keys.DOWN)) c.direction.y -= 1;
        if (c.direction.len2()>0) c.direction.nor();
        c.sprint = key(Input.Keys.SHIFT_LEFT,Input.Keys.SHIFT_RIGHT);
        c.cursorAim=true;c.aim.set(Gdx.input.getX(),Gdx.input.getY());
        c.attack=Gdx.input.isButtonJustPressed(Input.Buttons.LEFT);
        c.magic=Gdx.input.isButtonJustPressed(Input.Buttons.RIGHT);
        c.heal=Gdx.input.isKeyJustPressed(Input.Keys.SPACE);
        return c;
    }
}
