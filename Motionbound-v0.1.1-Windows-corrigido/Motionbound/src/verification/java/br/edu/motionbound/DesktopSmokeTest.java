package br.edu.motionbound;

import br.edu.motionbound.game.SaveData;
import br.edu.motionbound.ui.GameUi.Screen;
import br.edu.motionbound.ui.GameUi;
import com.badlogic.gdx.Application;
import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.ScreenUtils;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;

/** Opt-in GL integration test. Inputs are simulated inside this game, never at OS level. */
public final class DesktopSmokeTest extends ApplicationAdapter {
    private final MotionboundGame game = new MotionboundGame();
    private final FileHandle output;
    private Application application;
    private Input input;
    private Input simulatedInput;
    private Application simulatedApplication;
    private int frame, key = -1, mouseX, mouseY;
    private boolean click, exitRequested;
    private float pausedTime;
    private SaveData saved;
    private Throwable failure;

    private DesktopSmokeTest(Path path) { output = new FileHandle(path.toFile()); }
    public static void main(String[] args) throws Exception {
        Files.createDirectories(Path.of(args[0]));
        DesktopSmokeTest test = new DesktopSmokeTest(Path.of(args[0]));
        new Lwjgl3Application(test, DesktopLauncher.configuration());
        if (test.failure != null) throw new AssertionError("Desktop verification failed", test.failure);
        System.out.println("PASS: fullscreen, HUD, mouse/keyboard pause, frozen time, tutorial, settings, resize and save/continue");
    }

    @Override public void create() {
        application = Gdx.app; input = Gdx.input;
        game.create();
        set(game, "saveFile", output.child("test-expedition.json"));
        set(game, "hasSave", false); set(game, "message", "");
        simulatedInput = (Input)Proxy.newProxyInstance(Input.class.getClassLoader(), new Class<?>[]{Input.class}, (proxy, method, args) -> {
            return switch (method.getName()) {
                case "isKeyJustPressed" -> (int)args[0] == key;
                case "isKeyPressed" -> false;
                case "isButtonJustPressed" -> click && (int)args[0] == Input.Buttons.LEFT;
                case "getX" -> mouseX;
                case "getY" -> mouseY;
                case "getDeltaX", "getDeltaY" -> 0;
                default -> method.invoke(input, args);
            };
        });
        simulatedApplication = (Application)Proxy.newProxyInstance(Application.class.getClassLoader(), new Class<?>[]{Application.class}, (proxy, method, args) -> {
            if (method.getName().equals("exit")) { exitRequested = true; return null; }
            return method.invoke(application, args);
        });
    }

    @Override public void render() {
        try {
            // LWJGL restores its globals when a fullscreen transition rebinds the window.
            Gdx.input = simulatedInput; Gdx.app = simulatedApplication;
            frame++; key = -1; click = false;
            switch (frame) {
                case 5 -> { require(Gdx.graphics.isFullscreen(), "Fullscreen by default"); }
                case 8 -> key = Input.Keys.ENTER;
                case 12 -> key = Input.Keys.ESCAPE;
                case 14 -> { require(scene() == Screen.PAUSED, "ESC pauses"); pausedTime = (float)get(game, "elapsed"); }
                case 20 -> { require((float)get(game, "elapsed") == pausedTime, "Time frozen in pause"); }
                case 22, 24, 30 -> key = Input.Keys.DOWN;
                case 26, 32 -> key = Input.Keys.ENTER;
                case 36, 40 -> key = Input.Keys.ESCAPE;
                case 38 -> require(scene() == Screen.PAUSED, "ESC from tutorial keeps game paused");
                case 42 -> require(scene() == Screen.PLAYING, "ESC from summary resumes");
                case 44 -> key = Input.Keys.F11;
                case 52 -> require(!Gdx.graphics.isFullscreen(), "F11 switches to window");
                case 56 -> { mouseX = Gdx.graphics.getWidth() - 100; mouseY = Gdx.graphics.getHeight() - 50; click = true; }
                case 60 -> require(scene() == Screen.PAUSED, "Mouse opens pause menu");
                case 62 -> { mouseX = 210; mouseY = 458; click = true; }
                case 66 -> Gdx.graphics.setWindowedMode(960, 540);
                case 76 -> Gdx.graphics.setWindowedMode(1440, 720);
                case 86 -> Gdx.graphics.setWindowedMode(1280, 720);
                case 92 -> key = Input.Keys.ESCAPE;
                case 94 -> {
                    Object player = get(game, "player");
                    set(player, "hp", 4); set(player, "mana", 42f); set(player, "stamina", 63f);
                    saved = (SaveData)call(game, "captureSave");
                    saved.write((FileHandle)get(game, "saveFile"));
                    set(game, "scene", Screen.MENU); set(game, "hasSave", true);
                }
                case 98 -> key = Input.Keys.ENTER;
                case 99 -> {
                    SaveData loaded = (SaveData)call(game, "captureSave");
                    require(scene() == Screen.PLAYING && loaded.hp == saved.hp && loaded.mana == saved.mana
                            && loaded.stamina == saved.stamina && loaded.elapsed == saved.elapsed
                            && loaded.x == saved.x && loaded.y == saved.y && loaded.enemies.size == saved.enemies.size,
                            "Continue restores actual game state");
                    key = Input.Keys.ESCAPE;
                }
                case 102 -> {
                    // Make the target unwritable. Saving must leave the paused game open.
                    set(game, "saveFile", output);
                    call(game, "saveAndExit");
                    require(!exitRequested && scene() == Screen.PAUSED && !((String)get(game, "message")).isEmpty(),
                            "Failed save does not exit the game");
                    set(game, "saveFile", output.child("test-expedition.json"));
                    set(game, "message", "");
                }
                case 104 -> {
                    // Select Save and exit through the same keyboard path the player uses.
                    key = Input.Keys.UP;
                }
                case 106 -> key = Input.Keys.ENTER;
                case 112 -> throw new AssertionError("Save and exit did not request exit");
                default -> { }
            }
            game.render();
            switch (frame) {
                case 5 -> screenshot("01-title-fullscreen");
                case 10 -> screenshot("02-hud-fullscreen");
                case 20 -> screenshot("03-pause-summary");
                case 28 -> screenshot("04-settings");
                case 34 -> screenshot("05-tutorial-fullscreen");
                case 54 -> screenshot("06-hud-window");
                case 64 -> screenshot("07-tutorial-mouse");
                case 74 -> screenshot("08-tutorial-960x540");
                case 84 -> screenshot("09-tutorial-wide");
                case 96 -> screenshot("10-continue-menu");
                case 100 -> {
                    screenshot("11-restored-expedition");
                    roomScreenshot("12-guard-post", 192, 150);
                    roomScreenshot("13-burial-hall", 568, 142);
                    roomScreenshot("14-overgrown-chapel", 232, 400);
                    roomScreenshot("15-reliquary", 672, 400);
                }
                default -> { }
            }
            if (exitRequested) {
                require(frame == 106, "Save and exit action");
                SaveData data = SaveData.read((FileHandle)get(game, "saveFile"));
                require(data.hp == 4 && data.mana == 42, "Save and exit persists state");
                application.exit();
            }
        } catch (Throwable e) { failure = e; e.printStackTrace(); application.exit(); }
    }

    private Screen scene() { return (Screen)get(game, "scene"); }
    private void roomScreenshot(String name, float x, float y) {
        OrthographicCamera camera=(OrthographicCamera)get(game,"worldCamera");
        float oldX=camera.position.x,oldY=camera.position.y;
        Object player=get(game,"player");
        float oldPX=(float)get(player,"x"),oldPY=(float)get(player,"y");
        GameUi.View view=(GameUi.View)get(game,"uiView");
        float oldVX=view.x,oldVY=view.y;
        try {
            set(player,"x",x);set(player,"y",y);view.x=x;view.y=y;
            camera.position.set(x,y,0);camera.update();
            Gdx.gl.glClearColor(.07f,.06f,.09f,1);Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
            ((FitViewport)get(game,"viewport")).apply();
            for(String method:new String[]{"drawMap","drawGroundShadows","drawEntities","drawWorldEffects","drawAmbientLighting"})call(game,method);
            ((GameUi)get(game,"ui")).draw(Screen.PLAYING,view);
            screenshot(name);
        } finally {
            set(player,"x",oldPX);set(player,"y",oldPY);view.x=oldVX;view.y=oldVY;
            camera.position.set(oldX,oldY,0);camera.update();
        }
    }
    private void screenshot(String name) {
        int w = Gdx.graphics.getBackBufferWidth(), h = Gdx.graphics.getBackBufferHeight();
        byte[] pixels = ScreenUtils.getFrameBufferPixels(0, 0, w, h, true);
        Pixmap image = new Pixmap(w, h, Pixmap.Format.RGBA8888);
        image.getPixels().put(pixels);
        PixmapIO.writePNG(output.child(name + ".png"), image);
        image.dispose();
    }
    private static Object get(Object target, String name) {
        try { Field field = target.getClass().getDeclaredField(name); field.setAccessible(true); return field.get(target); }
        catch (ReflectiveOperationException e) { throw new AssertionError(e); }
    }
    private static void set(Object target, String name, Object value) {
        try { Field field = target.getClass().getDeclaredField(name); field.setAccessible(true); field.set(target, value); }
        catch (ReflectiveOperationException e) { throw new AssertionError(e); }
    }
    private static Object call(Object target, String name) {
        try { Method method = target.getClass().getDeclaredMethod(name); method.setAccessible(true); return method.invoke(target); }
        catch (ReflectiveOperationException e) { throw new AssertionError(e); }
    }
    private static void require(boolean value, String message) { if (!value) throw new AssertionError(message); }
    @Override public void resize(int width, int height) { game.resize(width, height); }
    @Override public void dispose() { Gdx.input = input; Gdx.app = application; game.dispose(); }
}
