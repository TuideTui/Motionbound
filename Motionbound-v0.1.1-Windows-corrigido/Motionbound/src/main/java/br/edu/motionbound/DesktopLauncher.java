package br.edu.motionbound;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;

/** Entry point for Windows, Linux and macOS desktop builds. */
public final class DesktopLauncher {
    private DesktopLauncher() { }

    public static void main(String[] args) {
        new Lwjgl3Application(new MotionboundGame(), configuration());
    }

    static Lwjgl3ApplicationConfiguration configuration() {
        Lwjgl3ApplicationConfiguration cfg = new Lwjgl3ApplicationConfiguration();
        cfg.setTitle("Motionbound | Prototipo 0.1.1");
        cfg.setWindowedMode(1280, 720);
        cfg.setWindowSizeLimits(960, 540, -1, -1);
        cfg.setFullscreenMode(Lwjgl3ApplicationConfiguration.getDisplayMode());
        cfg.setResizable(true);
        cfg.useVsync(true);
        cfg.setForegroundFPS(60);
        cfg.setWindowIcon("icon.png");
        return cfg;
    }
}
