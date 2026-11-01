package br.edu.motionbound.input;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;

/** Bench testing only, visibly labelled in every screen and checkpoint. */
public final class SimulatedSensorSource implements ControlSource {
    public final SensorControlSource sensors=new SensorControlSource();
    @Override public PlayerCommand read(float delta) {
        float x=(Gdx.input.isKeyPressed(Input.Keys.L)?1:0)-(Gdx.input.isKeyPressed(Input.Keys.J)?1:0);
        float y=(Gdx.input.isKeyPressed(Input.Keys.I)?1:0)-(Gdx.input.isKeyPressed(Input.Keys.K)?1:0);
        float pressure=Gdx.input.isKeyPressed(Input.Keys.F)?
                (Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT)||Gdx.input.isKeyPressed(Input.Keys.SHIFT_RIGHT)?1:.65f):0;
        sensors.accept(new SensorSample(x,y,pressure));
        return sensors.read(delta);
    }
    @Override public void reset() {sensors.reset();}
}
