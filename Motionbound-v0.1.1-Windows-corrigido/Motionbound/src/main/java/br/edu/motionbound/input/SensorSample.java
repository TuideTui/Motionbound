package br.edu.motionbound.input;

/** Calibrated values from a future device adapter, or the clearly labelled simulator. */
public record SensorSample(float tiltX, float tiltY, float pressure) {
    public SensorSample {
        if(!Float.isFinite(tiltX)||!Float.isFinite(tiltY)||!Float.isFinite(pressure)
                ||Math.abs(tiltX)>1||Math.abs(tiltY)>1||pressure<0||pressure>1)
            throw new IllegalArgumentException("Sensor sample must be finite and normalized");
    }
}
