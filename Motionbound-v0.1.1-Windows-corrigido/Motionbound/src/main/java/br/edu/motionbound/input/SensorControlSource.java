package br.edu.motionbound.input;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;

/** Hardware-independent sensor pipeline. No mouse reads, serial port or clinical assumptions. */
public final class SensorControlSource implements ControlSource {
    public static final float STALE_SECONDS=.35f, PRESS_THRESHOLD=.45f, RELEASE_THRESHOLD=.25f;
    private SensorSample sample=new SensorSample(0,0,0);
    private final Vector2 filtered=new Vector2();
    private float sinceSample=STALE_SECONDS;
    private boolean needsRelease=true;
    public float pressureSeconds, pressureInBandSeconds;

    public void accept(SensorSample sample) {this.sample=sample;sinceSample=0;}
    public boolean connected() {return sinceSample<STALE_SECONDS;}
    public float pressure() {return connected()?sample.pressure():0;}
    public float pressureAccuracy() {return pressureSeconds>0?100*pressureInBandSeconds/pressureSeconds:0;}

    @Override public PlayerCommand read(float delta) {
        PlayerCommand command=new PlayerCommand();command.assistedAim=true;
        sinceSample+=Math.max(0,delta);
        if(!connected()) {filtered.setZero();needsRelease=true;return command;}
        float smoothing=1-(float)Math.exp(-18*delta);
        filtered.lerp(new Vector2(sample.tiltX(),sample.tiltY()),smoothing);
        command.direction.set(deadzone(filtered.x),deadzone(filtered.y)).limit(1);
        command.aim.set(command.direction);
        if(sample.pressure()<=RELEASE_THRESHOLD)needsRelease=false;
        if(!needsRelease&&sample.pressure()>=PRESS_THRESHOLD) {command.attack=true;needsRelease=true;}
        if(sample.pressure()>RELEASE_THRESHOLD) {
            pressureSeconds+=delta;
            if(sample.pressure()>=PRESS_THRESHOLD&&sample.pressure()<=.8f)pressureInBandSeconds+=delta;
        }
        return command;
    }
    private static float deadzone(float value) {
        return Math.abs(value)<=.12f?0:Math.signum(value)*MathUtils.clamp((Math.abs(value)-.12f)/.88f,0,1);
    }
    @Override public void reset() {needsRelease=true;filtered.setZero();}
    public void clearMetrics() {pressureSeconds=0;pressureInBandSeconds=0;reset();}
}
