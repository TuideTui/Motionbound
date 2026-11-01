package br.edu.motionbound;

import br.edu.motionbound.game.SaveData;
import br.edu.motionbound.game.MeleeSwing;
import br.edu.motionbound.input.GameMode;
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
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.math.Vector2;
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
    private int mouseButton=Input.Buttons.LEFT;
    private final java.util.Set<Integer> held=new java.util.HashSet<>();
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
        System.out.println("PASS: fullscreen, HUD, pause, resize, diagonal combat, animation timing, heart pickup, sensor simulation and save/continue");
    }

    @Override public void create() {
        application = Gdx.app; input = Gdx.input;
        game.create();
        set(game, "saveFile", output.child("test-expedition.json"));
        set(game, "hasSave", false); set(game, "message", "");
        simulatedInput = (Input)Proxy.newProxyInstance(Input.class.getClassLoader(), new Class<?>[]{Input.class}, (proxy, method, args) -> {
            return switch (method.getName()) {
                case "isKeyJustPressed" -> (int)args[0] == key;
                case "isKeyPressed" -> held.contains((int)args[0]);
                case "isButtonJustPressed" -> click && (int)args[0] == mouseButton;
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
                case 6 -> key=Input.Keys.ENTER;
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
                case 5 -> screenshot("00-mode-selection-fullscreen");
                case 7 -> screenshot("01-title-fullscreen");
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
                    verifyCombatFlow();
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
    private void verifyCombatFlow() throws Exception {
        FileHandle previousFile=(FileHandle)get(game,"saveFile"),backup=output.child("before-combat-check.json");
        ((SaveData)call(game,"captureSave")).write(backup);
        try {
            call(game,"resetWorld");set(game,"scene",Screen.PLAYING);
            Object player=get(game,"player");set(player,"x",192f);set(player,"y",136f);set(player,"hp",4);
            OrthographicCamera camera=(OrthographicCamera)get(game,"worldCamera");camera.position.set(192,136,0);camera.update();
            Array<?> enemies=(Array<?>)get(game,"enemies");
            for(Object enemy:enemies){set(enemy,"x",600f);set(enemy,"y",400f);set(enemy,"speed",0f);}
            swordGripScreenshot();
            Object target=enemies.first();set(target,"x",209f);set(target,"y",153f);set(target,"hp",2);set(target,"attackCd",1f);
            pointAt(225,169);click=true;mouseButton=Input.Buttons.LEFT;step(.02f);click=false;
            require((int)get(game,"attacks")==1&&(int)get(target,"hp")==2,"Left click starts preparation without immediate damage");
            game.render();screenshot("16-sword-preparation");
            MeleeSwing swing=(MeleeSwing)get(game,"swing");
            pointAt(160,105);step(.02f);
            require(swing.direction.x>0&&swing.direction.y>0,"Changing mouse mid-swing cannot redirect the hit");
            pointAt(225,169);
            // Resume a checkpoint during wind-up: its impact must still happen exactly once.
            FileHandle windupFile=output.child("windup-check.json");
            ((SaveData)call(game,"captureSave")).write(windupFile);
            float windupTime=swing.elapsed;
            set(game,"saveFile",windupFile);call(game,"loadGame");set(game,"saveFile",previousFile);
            player=get(game,"player");enemies=(Array<?>)get(game,"enemies");target=enemies.first();
            for(Object enemy:enemies)set(enemy,"speed",0f);
            require(swing.elapsed==windupTime&&!swing.impacted,"Loading wind-up restores the pending impact");
            while(swing.elapsed<.17f)step(.02f);
            require((int)get(target,"hp")==1&&(int)get(game,"hits")==1,"Diagonal strike lands at impact");
            game.render();screenshot("17-diagonal-sword-impact");
            FileHandle impactFile=output.child("impact-check.json");
            ((SaveData)call(game,"captureSave")).write(impactFile);
            set(game,"saveFile",impactFile);call(game,"loadGame");set(game,"saveFile",previousFile);
            player=get(game,"player");enemies=(Array<?>)get(game,"enemies");target=enemies.first();
            for(Object enemy:enemies)set(enemy,"speed",0f);
            for(int i=0;i<30;i++)step(.02f);
            require((int)get(target,"hp")==1&&(int)get(game,"hits")==1,"Loading recovery cannot repeat the impact");

            set(target,"hp",2);set(player,"mana",100f);mouseButton=Input.Buttons.RIGHT;click=true;step(.02f);click=false;mouseButton=Input.Buttons.LEFT;
            Array<?> spells=(Array<?>)get(game,"spells");
            require(spells.size==1&&(float)get(spells.first(),"vx")>0&&(float)get(spells.first(),"vy")>0
                    &&(float)get(player,"mana")==75,"Right click casts diagonally and costs 25 mana");
            game.render();screenshot("18-magic-cast");
            int projectileSteps=0;
            while(spells.size>0&&projectileSteps++<50)step(.02f);
            require(spells.size==0&&(int)get(target,"hp")==1,"Magic deals one damage and its projectile is consumed");
            require((float)get(target,"kickX")>0&&(float)get(target,"kickY")>0,"Magic knocks back along the diagonal projectile direction");
            SaveData recoil=SaveData.decode(((SaveData)call(game,"captureSave")).encode());
            require(recoil.enemies.first().kickX>61&&recoil.enemies.first().kickY>61,"A checkpoint accepts the stronger magical recoil");
            float targetX=(float)get(target,"x"),targetY=(float)get(target,"y");
            set(target,"speed",37f);
            for(int i=0;i<6;i++)step(.02f);
            require((float)get(target,"x")>targetX+7&&(float)get(target,"y")>targetY+7
                    &&(int)get(target,"hp")==1,"Recoil overrides chasing and never repeats projectile damage");
            game.render();screenshot("26-magic-knockback");
            set(target,"x",298f);set(target,"y",200f);set(target,"kickX",240f);set(target,"kickY",0f);
            for(int i=0;i<12;i++) {
                step(.02f);
                require((float)get(target,"x")<299,"Strong recoil cannot cross the room wall");
            }
            for(int i=0;i<24;i++)step(.02f);
            set(player,"hp",3);key=Input.Keys.SPACE;step(.02f);
            require((int)get(player,"hp")==5&&(int)get(player,"heals")==2,"Space consumes one potion and restores two hearts");
            game.render();require(scene()==Screen.PLAYING,"Space healing cannot activate the pause button");
            screenshot("19-healing-animation");key=-1;

            Array<?> hearts=(Array<?>)get(game,"hearts");hearts.clear();set(game,"heartsCollected",0);
            set(player,"hp",6);addHeart(192,136);call(game,"collectHearts");
            require(hearts.size==1&&(int)get(player,"hp")==6,"Full life does not consume a drop");
            set(player,"hp",5);call(game,"collectHearts");
            require(hearts.size==0&&(int)get(player,"hp")==6&&(int)get(game,"heartsCollected")==1,"Pickup restores one heart with a hard life cap");
            addHeart(209,136);game.render();screenshot("20-heart-drop");
            int deadBefore=(int)get(game,"defeated");
            set(enemies.get(0),"hp",0);set(enemies.get(1),"hp",0);call(game,"resolveDeaths");
            int dropCount=hearts.size;call(game,"resolveDeaths");
            require((int)get(game,"defeated")==deadBefore+2&&hearts.size==dropCount,"Each enemy resolves death and loot only once");

            int attacksBefore=(int)get(game,"attacks");
            mouseX=Gdx.graphics.getWidth()-100;mouseY=Gdx.graphics.getHeight()-50;click=true;game.render();click=false;
            require(scene()==Screen.PAUSED&&(int)get(game,"attacks")==attacksBefore,"Pause click is consumed by HUD");

            set(game,"scene",Screen.MODE_SELECT);key=-1;game.render();
            key=Input.Keys.DOWN;game.render();key=Input.Keys.ENTER;game.render();key=-1;game.render();
            require(scene()==Screen.REHAB_SETUP,"Rehabilitation mode has its own preparation screen");
            screenshot("21-rehabilitation-preparation");
            key=Input.Keys.ENTER;game.render();key=-1;
            require(scene()==Screen.PLAYING&&get(game,"mode")==GameMode.REHABILITATION,"Explicit simulator entry");
            enemies=(Array<?>)get(game,"enemies");
            for(Object enemy:enemies){set(enemy,"x",600f);set(enemy,"y",400f);set(enemy,"speed",0f);}
            step(.02f);held.add(Input.Keys.F);step(.02f);
            require((int)get(game,"attacks")==1,"Simulated pressure triggers attack");
            for(int i=0;i<30;i++)step(.02f);
            require((int)get(game,"attacks")==1,"Holding pressure does not spam attacks");
            held.clear();step(.02f);held.add(Input.Keys.F);step(.02f);
            require((int)get(game,"attacks")==2,"Release and squeeze creates next attack");
            held.add(Input.Keys.SHIFT_LEFT);for(int i=0;i<10;i++)step(.02f);
            held.clear();held.add(Input.Keys.I);held.add(Input.Keys.L);
            player=get(game,"player");float x=(float)get(player,"x"),y=(float)get(player,"y");
            for(int i=0;i<12;i++)step(.02f);
            require((float)get(player,"x")>x&&(float)get(player,"y")>y,"Simulated diagonal tilt moves the player");
            held.clear();int beforeMouse=(int)get(game,"attacks");click=true;step(.02f);click=false;
            require((int)get(game,"attacks")==beforeMouse,"Sensor source ignores mouse attacks");
            game.render();screenshot("22-sensor-simulation-hud");
            SaveData simulation=(SaveData)call(game,"captureSave");
            FileHandle simulationFile=output.child("sensor-simulation-check.json");simulation.write(simulationFile);
            require(SaveData.read(simulationFile).mode==GameMode.REHABILITATION&&simulation.simulatedSensors&&simulation.pressureSeconds>0
                    &&simulation.pressureInBandSeconds<simulation.pressureSeconds,"Simulation and synthetic metrics remain labelled in saves");
            key=Input.Keys.ESCAPE;game.render();key=-1;game.render();screenshot("23-sensor-simulation-summary");
            set(game,"scene",Screen.DEFEAT);game.render();screenshot("24-sensor-simulation-result");
        } finally {
            held.clear();key=-1;click=false;mouseButton=Input.Buttons.LEFT;
            set(game,"saveFile",backup);call(game,"loadGame");set(game,"saveFile",previousFile);set(game,"scene",Screen.PAUSED);
        }
    }
    /** Review all hand attachments at a larger scale, using the actual game renderer. */
    private void swordGripScreenshot() {
        Object player=get(game,"player");
        OrthographicCamera camera=(OrthographicCamera)get(game,"worldCamera");
        MeleeSwing swing=(MeleeSwing)get(game,"swing");
        Vector2 aim=(Vector2)get(game,"aim"),oldAim=new Vector2(aim),oldDirection=new Vector2(swing.direction);
        float oldX=(float)get(player,"x"),oldY=(float)get(player,"y"),oldWalk=(float)get(player,"walk"),oldZoom=camera.zoom,oldSwing=swing.elapsed;
        int oldFacing=(int)get(player,"facing");boolean oldMoving=(boolean)get(player,"moving");
        try {
            camera.zoom=.6f;camera.update();
            ((FitViewport)get(game,"viewport")).apply();
            Gdx.gl.glClearColor(.07f,.06f,.09f,1);Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
            for(int facing=0;facing<4;facing++)for(int pose=0;pose<6;pose++) {
                set(player,"x",192+(pose-2.5f)*34);set(player,"y",136+(1.5f-facing)*32);
                set(player,"facing",facing);set(player,"moving",pose==1||pose==2);set(player,"walk",pose==2?2f:0f);
                aim.set(facing==2?-1:facing==3?1:0,facing==1?1:facing==0?-1:0);swing.direction.set(aim);
                swing.elapsed=pose==3?.06f:pose==4?.18f:pose==5?.30f:-1;
                call(game,"drawEntities");
            }
            screenshot("25-sword-grip-directions");
        } finally {
            set(player,"x",oldX);set(player,"y",oldY);set(player,"facing",oldFacing);set(player,"moving",oldMoving);set(player,"walk",oldWalk);
            aim.set(oldAim);swing.direction.set(oldDirection);swing.elapsed=oldSwing;camera.zoom=oldZoom;camera.update();
        }
    }
    private void pointAt(float x,float y) {
        Vector2 point=((FitViewport)get(game,"viewport")).project(new Vector2(x,y));
        mouseX=Math.round(point.x);mouseY=Gdx.graphics.getHeight()-Math.round(point.y);
    }
    private void step(float delta) {
        try {Method update=game.getClass().getDeclaredMethod("update",float.class);update.setAccessible(true);update.invoke(game,delta);}
        catch(ReflectiveOperationException e){throw new AssertionError(e);}
    }
    @SuppressWarnings({"rawtypes","unchecked"})
    private void addHeart(float x,float y) throws Exception {
        Class<?> type=Class.forName("br.edu.motionbound.MotionboundGame$HeartDrop");
        var constructor=type.getDeclaredConstructor(float.class,float.class);constructor.setAccessible(true);
        ((Array)get(game,"hearts")).add(constructor.newInstance(x,y));
    }
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
