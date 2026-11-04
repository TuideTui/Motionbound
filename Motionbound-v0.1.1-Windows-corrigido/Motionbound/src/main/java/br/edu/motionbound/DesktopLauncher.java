package br.edu.motionbound;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.Graphics;

/** Entry point for Windows, Linux and macOS desktop builds. */
public final class DesktopLauncher {
    private DesktopLauncher() { }

    public static void main(String[] args) {
        new Lwjgl3Application(new MotionboundGame(), configuration());
    }

    static Lwjgl3ApplicationConfiguration configuration() {
        Lwjgl3ApplicationConfiguration cfg = new Lwjgl3ApplicationConfiguration();
        cfg.setTitle("Motionbound | Prototipo 0.1.1");
        Graphics.Monitor monitor=Lwjgl3ApplicationConfiguration.getPrimaryMonitor();
        Graphics.DisplayMode display=Lwjgl3ApplicationConfiguration.getDisplayMode(monitor);
        // Borderless fullscreen lets Windows capture overlays appear without changing display modes.
        cfg.setWindowedMode(display.width,display.height);
        cfg.setWindowPosition(monitor.virtualX,monitor.virtualY);
        cfg.setDecorated(false);
        cfg.setWindowSizeLimits(960, 540, -1, -1);
        cfg.setAutoIconify(false);
        cfg.setPauseWhenLostFocus(false);
        cfg.setPauseWhenMinimized(false);
        cfg.setResizable(true);
        cfg.useVsync(true);
        cfg.setForegroundFPS(60);
        cfg.setIdleFPS(60);
        cfg.setWindowIcon("icon.png");
        return cfg;
    }
}
