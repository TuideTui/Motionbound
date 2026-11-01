package br.edu.motionbound.input;

/** Future implementations may use Serial/IoT rather than a keyboard. */
public interface ControlSource {
    PlayerCommand read(float delta);
    default void reset() { }
}
