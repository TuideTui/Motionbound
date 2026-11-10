package br.edu.motionbound;

import br.edu.motionbound.game.SaveData;
import br.edu.motionbound.game.MeleeSwing;
import br.edu.motionbound.game.GuidedStrike;
import br.edu.motionbound.input.ControlSource;
import br.edu.motionbound.input.PlayerCommand;
import br.edu.motionbound.game.CampaignLevel;
import br.edu.motionbound.game.DungeonMap;
import br.edu.motionbound.game.EnemyKind;
import br.edu.motionbound.game.PotionEffect;
import br.edu.motionbound.game.StatusEffects;
import br.edu.motionbound.game.CasterAttack;
import br.edu.motionbound.game.CameraDistortion;
import br.edu.motionbound.input.GameMode;
import br.edu.motionbound.input.EdgeController;
import br.edu.motionbound.input.EdgeFrame;
import br.edu.motionbound.ui.GameUi.Screen;
import br.edu.motionbound.ui.GameUi;
import br.edu.motionbound.ui.ScreenStatusEffects;
import br.edu.motionbound.ui.HudFeedback;
import com.badlogic.gdx.Application;
import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Graphics;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWWindowFocusCallback;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.utils.viewport.Viewport;
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
    private boolean verifyingGuides;
    private boolean rendering;
    private final java.util.Set<Integer> held=new java.util.HashSet<>();
    private boolean click, exitRequested;
    private float pausedTime;
    private SaveData saved;
    private Throwable failure;
    private long usbSequence,usbAttacks,usbMagic;

    private DesktopSmokeTest(Path path) { output = new FileHandle(path.toFile()); }
    public static void main(String[] args) throws Exception {
        System.setProperty("motionbound.usb.disabled","true");
        Files.createDirectories(Path.of(args[0]));
        DesktopSmokeTest test = new DesktopSmokeTest(Path.of(args[0]));
        var configuration=DesktopLauncher.configuration();
        configuration.setPreferencesConfig(Path.of(args[0],"preferences").toString(),com.badlogic.gdx.Files.FileType.Absolute);
        new Lwjgl3Application(test, configuration);
        if (test.failure != null) throw new AssertionError("Desktop verification failed", test.failure);
        System.out.println("PASS: fullscreen, HUD, combat, knockback, five-stage campaign, countdown, retry, both control modes and save/continue");
    }

    @Override public void create() {
        application = Gdx.app; input = Gdx.input;
        game.create();
        set(game, "saveFile", output.child("test-expedition.json"));
        set(game, "hasSave", false);set(game,"savedMode",GameMode.CONVENTIONAL);set(game, "message", "");
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
        // GLFW may request another application render from inside a window resize callback.
        if(verifyingGuides||rendering)return;
        rendering=true;
        try {
            // LWJGL restores its globals when a fullscreen transition rebinds the window.
            Gdx.input = simulatedInput; Gdx.app = simulatedApplication;
            frame++; key = -1; click = false;
            switch (frame) {
                case 5 -> verifyBorderlessFullscreen();
                case 6 -> key=Input.Keys.ENTER;
                case 8 -> key = Input.Keys.ENTER;
                case 9 -> verifyIntro();
                case 12 -> key = Input.Keys.ESCAPE;
                case 14 -> { require(scene() == Screen.PAUSED, "ESC pauses"); pausedTime = (float)get(game, "elapsed"); }
                case 20 -> { require((float)get(game, "elapsed") == pausedTime, "Time frozen in pause"); }
                case 22, 24, 30 -> key = Input.Keys.DOWN;
                case 26, 32 -> key = Input.Keys.ENTER;
                case 36, 40 -> key = Input.Keys.ESCAPE;
                case 38 -> require(scene() == Screen.PAUSED, "ESC from tutorial keeps game paused");
                case 42 -> require(scene() == Screen.PLAYING, "ESC from summary resumes");
                case 44 -> key = Input.Keys.F11;
                case 46 -> key = Input.Keys.F11;
                case 48 -> {verifyBorderlessFullscreen();key=Input.Keys.F11;}
                case 52 -> {
                    long window=((Lwjgl3Graphics)Gdx.graphics).getWindow().getWindowHandle();
                    require(!(boolean)get(game,"fullscreenWindow")&&GLFW.glfwGetWindowAttrib(window,GLFW.GLFW_DECORATED)==GLFW.GLFW_TRUE,
                            "F11 switches back to a decorated window after borderless fullscreen");
                }
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
                case 10 -> {screenshot("02-hud-fullscreen");verifyCaptureFocus();}
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
                    verifyEnemyEffectsFlow();
                    verifyCasterCombosFlow();
                    verifyGroundAreasFlow();
                    verifyTeleportCinematic();
                    verifyCampaignFlow();
                    verifyingGuides=true;
                    try {verifyScreenStatusEffectsFlow();verifyGuideMenus();verifyPresentationFlow();verifyAutomaticHealing();verifyPhysicalController();verifyGuidedCombat();}finally {verifyingGuides=false;}
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
        finally {rendering=false;}
    }

    private Screen scene() { return (Screen)get(game, "scene"); }
    private void verifyBorderlessFullscreen() {
        var window=((Lwjgl3Graphics)Gdx.graphics).getWindow();
        var monitor=Gdx.graphics.getMonitor();var display=Gdx.graphics.getDisplayMode(monitor);
        require(!Gdx.graphics.isFullscreen()&&(boolean)get(game,"fullscreenWindow")
                &&GLFW.glfwGetWindowAttrib(window.getWindowHandle(),GLFW.GLFW_DECORATED)==GLFW.GLFW_FALSE
                &&Gdx.graphics.getWidth()==display.width&&Gdx.graphics.getHeight()==display.height
                &&window.getPositionX()==monitor.virtualX&&window.getPositionY()==monitor.virtualY,
                "Borderless fullscreen fills the monitor without exclusive display mode: active="+get(game,"fullscreenWindow")
                +", decorated="+GLFW.glfwGetWindowAttrib(window.getWindowHandle(),GLFW.GLFW_DECORATED)
                +", size="+Gdx.graphics.getWidth()+"x"+Gdx.graphics.getHeight()+", display="+display.width+"x"+display.height
                +", position="+window.getPositionX()+","+window.getPositionY()+", monitor="+monitor.virtualX+","+monitor.virtualY);
    }
    private void verifyCaptureFocus() {
        var window=((Lwjgl3Graphics)Gdx.graphics).getWindow();
        Screen previous=scene();float time=(float)get(game,"elapsed");
        try {
            set(game,"scene",Screen.PLAYING);
            deliverFocus(false);
            require(!window.isFocused()&&!window.isIconified()&&scene()==Screen.PLAYING,
                    "Losing focus to a capture overlay neither minimizes nor pauses the game");
            game.render();
            require(scene()==Screen.PLAYING&&(float)get(game,"elapsed")>time,
                    "Gameplay continues rendering and advancing while the capture overlay has focus");
            screenshot("60-capture-overlay-focus");
        } finally {
            deliverFocus(true);set(game,"scene",previous);set(game,"elapsed",time);
        }
    }
    private void deliverFocus(boolean focused) {
        var window=((Lwjgl3Graphics)Gdx.graphics).getWindow();
        Array<?> queue=(Array<?>)get(window,"runnables");int next=queue.size;
        ((GLFWWindowFocusCallback)get(window,"focusCallback")).invoke(window.getWindowHandle(),focused);
        // GLFW queues focus notifications; deliver this event before asserting its result.
        ((Runnable)queue.removeIndex(next)).run();
    }
    private void verifyEdgePixels() {
        int w=1280,h=720;
        FrameBuffer buffer=new FrameBuffer(Pixmap.Format.RGBA8888,w,h,false);
        SpriteBatch effectBatch=new SpriteBatch();
        OrthographicCamera camera=new OrthographicCamera();camera.setToOrtho(false,w,h);
        effectBatch.setProjectionMatrix(camera.combined);
        Texture pixel=(Texture)get(get(game,"ui"),"white");
        try {
            buffer.begin();Gdx.gl.glClearColor(.04f,.03f,.07f,1);Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
            byte[] baseline=ScreenUtils.getFrameBufferPixels(0,0,w,h,false);
            for(int scenario=0;scenario<5;scenario++) {
                StatusEffects state=new StatusEffects();
                if(scenario<4)state.apply(PotionEffect.values()[scenario]);
                else {state.apply(PotionEffect.POISON);state.apply(PotionEffect.SLOW);state.apply(PotionEffect.RIFT);}
                ScreenStatusEffects layer=new ScreenStatusEffects(pixel,((br.edu.motionbound.game.GameAssets)get(game,"art")).glow);layer.update(state,.25f);
                Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);effectBatch.begin();layer.draw(effectBatch,w,h,1);effectBatch.end();
                byte[] active=ScreenUtils.getFrameBufferPixels(0,0,w,h,false);
                int changed=0;
                for(int y=0;y<h;y++)for(int x=0;x<w;x++) {
                    int p=(y*w+x)*4;
                    boolean difference=false;for(int c=0;c<4;c++)if(active[p+c]!=baseline[p+c])difference=true;
                    if(difference)changed++;
                    if(x>=110&&x<w-110)require(!difference,"Status particles preserve a clear central playfield");
                }
                boolean rift=scenario==PotionEffect.RIFT.ordinal();
                require(rift?changed==0:changed>1000,"Rift uses camera distortion only; other statuses have visible edge feedback");
                layer.update(state,.2f);Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
                effectBatch.begin();layer.draw(effectBatch,w,h,1);effectBatch.end();
                if(!rift)require(!java.util.Arrays.equals(active,ScreenUtils.getFrameBufferPixels(0,0,w,h,false)),"Pixel frames and particle positions animate with elapsed time");
                layer.update(new StatusEffects(),1);
                Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);effectBatch.begin();layer.draw(effectBatch,w,h,1);effectBatch.end();
                require(java.util.Arrays.equals(baseline,ScreenUtils.getFrameBufferPixels(0,0,w,h,false)),"Ending or cleansing statuses fully removes their edge layer");
                layer.dispose();
            }
        } finally {buffer.end();effectBatch.dispose();buffer.dispose();}
    }
    private void verifyCameraDistortion() {
        CameraDistortion distortion=(CameraDistortion)get(game,"cameraDistortion");
        int w=Gdx.graphics.getBackBufferWidth(),h=Gdx.graphics.getBackBufferHeight();
        Texture pixel=(Texture)get(get(game,"ui"),"white");
        SpriteBatch pattern=new SpriteBatch();
        OrthographicCamera camera=new OrthographicCamera();camera.setToOrtho(false,w,h);
        pattern.setProjectionMatrix(camera.combined);pattern.disableBlending();
        Runnable draw=()->{
            Gdx.gl.glViewport(0,0,w,h);pattern.begin();
            for(int y=0;y<h;y+=32)for(int x=0;x<w;x+=32) {
                pattern.setColor(x<w/3?1:0,((x/32+y/32)%2==0)?1:0,y<h/2?1:0,1);
                pattern.draw(pixel,x,y,32,32);
            }
            pattern.end();
        };
        try {
            draw.run();byte[] baseline=ScreenUtils.getFrameBufferPixels(0,0,w,h,false);
            for(int scenario=0;scenario<4;scenario++) {
                distortion.begin(w,h);draw.run();
                distortion.present(scenario==1,scenario==1?StatusEffects.RIFT_DURATION-.27f:scenario==2?1f:0);
                byte[] actual=ScreenUtils.getFrameBufferPixels(0,0,w,h,false);
                int changed=0;
                for(int y=0;y<h;y++)for(int x=0;x<w;x++) {
                    int p=(y*w+x)*4;boolean different=false;
                    for(int c=0;c<3;c++) {
                        if(actual[p+c]!=baseline[p+c])different=true;
                        int channel=actual[p+c]&255;
                        require(channel==0||channel==255,"Camera distortion reuses scene pixels without blue tint or blur");
                    }
                    if(different)changed++;
                    if((x<2||x>=w-2)&&(y<2||y>=h-2))require(!different,"Distortion pins camera corners without empty borders");
                }
                require(scenario==1||scenario==2?changed>w*h/20:changed==0,
                        "Suction and arrival warp the image; the completed transition restores its exact orientation and pixels");
            }
        } finally {pattern.dispose();}
    }
    private void verifyScreenStatusEffectsFlow() throws Exception {
        verifyEdgePixels();
        verifyCameraDistortion();
        FileHandle previous=(FileHandle)get(game,"saveFile"),backup=output.child("before-screen-status-effects.json");
        ((SaveData)call(game,"captureSave")).write(backup);
        try {
            key=-1;click=false;held.clear();set(game,"stage",1);call(game,"resetWorld");
            quietEnemies((Array<?>)get(game,"enemies"));
            ScreenStatusEffects layer=(ScreenStatusEffects)get(get(game,"ui"),"screenEffects");
            for(PotionEffect type:PotionEffect.values()) {
                layer.clear();Object player=get(game,"player");StatusEffects effects=(StatusEffects)get(player,"effects");
                effects.cleanseDamage();effects.slowRemaining=0;effects.riftRemaining=0;effects.apply(type);
                set(game,"scene",Screen.PLAYING);
                for(int n=0;n<24;n++)game.render();
                float strength=((float[])get(layer,"strength"))[type.ordinal()];
                require(type==PotionEffect.RIFT?strength==0:strength>0,"Gameplay feeds edge feedback without blue screen particles");
                screenshot("55-screen-status-"+type);
            }
            Object player=get(game,"player");StatusEffects effects=(StatusEffects)get(player,"effects");
            effects.cleanseDamage();effects.riftRemaining=0;effects.apply(PotionEffect.POISON);effects.apply(PotionEffect.SLOW);effects.apply(PotionEffect.RIFT);
            for(int n=0;n<16;n++)game.render();screenshot("56-screen-status-combined");
            game.pause();
            float[] phase=((float[])get(layer,"phase")).clone(),strength=((float[])get(layer,"strength")).clone();
            for(int n=0;n<8;n++)game.render();
            require(java.util.Arrays.equals(phase,(float[])get(layer,"phase"))&&java.util.Arrays.equals(strength,(float[])get(layer,"strength")),"Pause freezes the edge particles and their fades");
            FileHandle checkpoint=output.child("screen-status-checkpoint.json");((SaveData)call(game,"captureSave")).write(checkpoint);
            set(game,"scene",Screen.MENU);game.render();
            for(float value:(float[])get(layer,"strength"))require(value==0,"Returning to the menu clears screen effects");
            set(game,"saveFile",checkpoint);call(game,"loadGame");quietEnemies((Array<?>)get(game,"enemies"));
            for(int n=0;n<16;n++)game.render();
            require(((float[])get(layer,"strength"))[PotionEffect.POISON.ordinal()]>0,"Loading an active status restores its screen feedback");
            for(int[] size:new int[][]{{960,540},{1280,960},{1440,720},{1920,1080},{1920,1200}}) {
                // Keep a visible dose while verifying the corners at each display density/aspect.
                effects=(StatusEffects)get(get(game,"player"),"effects");effects.riftRemaining=1.4f;
                Gdx.graphics.setWindowedMode(size[0],size[1]);Gdx.input=simulatedInput;Gdx.app=simulatedApplication;
                game.resize(size[0],size[1]);game.render();
                Viewport world=(Viewport)get(game,"viewport");
                require(world.getScreenX()==0&&world.getScreenY()==0&&world.getScreenWidth()==size[0]&&world.getScreenHeight()==size[1],
                        "The world fills the entire window without letterboxing at "+size[0]+"x"+size[1]);
                OrthographicCamera camera=(OrthographicCamera)get(game,"worldCamera");
                require(Math.abs(camera.viewportWidth/camera.viewportHeight-size[0]/(float)size[1])<.001f,"World proportions match the screen without distorting sprites");
                Vector2 projected=world.project(new Vector2(200,150));
                require(world.unproject(new Vector2(projected.x,Gdx.graphics.getHeight()-projected.y)).dst2(200,150)<.01f,"Mouse aiming round-trips after filling the new aspect ratio");
                screenshot("57-screen-status-"+size[0]+"x"+size[1]);
            }
            captureStatusAnimation(layer,PotionEffect.FIRE,"fire-motion");
            captureStatusAnimation(layer,PotionEffect.SLOW,"smoke-motion");
            captureStatusAnimation(layer,PotionEffect.POISON,"poison-motion");
            captureTeleportAnimation();
            set(game,"stage",2);call(game,"startStage");game.render();
            for(float value:(float[])get(layer,"strength"))require(value==0,"A fresh stage never inherits old status borders");
            Gdx.graphics.setWindowedMode(1280,720);Gdx.input=simulatedInput;Gdx.app=simulatedApplication;game.resize(1280,720);
        } finally {
            key=-1;click=false;held.clear();set(game,"saveFile",backup);call(game,"loadGame");set(game,"saveFile",previous);set(game,"scene",Screen.PAUSED);
        }
    }
    private void captureStatusAnimation(ScreenStatusEffects layer,PotionEffect type,String folder) throws Exception {
        Gdx.graphics.setWindowedMode(1280,800);Gdx.input=simulatedInput;Gdx.app=simulatedApplication;game.resize(1280,800);
        Object player=get(game,"player");StatusEffects effects=(StatusEffects)get(player,"effects");
        effects.cleanseDamage();effects.slowRemaining=0;effects.riftRemaining=0;effects.apply(type);layer.clear();
        FileHandle frames=output.child(folder);frames.mkdirs();
        for(int n=0;n<48;n++) {
            ((float[])get(layer,"phase"))[type.ordinal()]=n/30f;
            layer.update(effects,.2f);game.render();
            Pixmap frame=ScreenUtils.getFrameBufferPixmap(0,0,Gdx.graphics.getBackBufferWidth(),Gdx.graphics.getBackBufferHeight());
            PixmapIO.writePNG(frames.child(String.format(java.util.Locale.ROOT,"fire-%03d.png",n)),frame,-1,true);frame.dispose();
        }
    }
    private void captureTeleportAnimation() throws Exception {
        ((ScreenStatusEffects)get(get(game,"ui"),"screenEffects")).clear();
        set(game,"stage",4);call(game,"resetWorld");set(game,"scene",Screen.PLAYING);
        quietEnemies((Array<?>)get(game,"enemies"));Object player=get(game,"player");set(player,"x",192f);set(player,"y",136f);set(game,"riftsCast",1);
        addVial(PotionEffect.RIFT);step(.02f);
        FileHandle frames=output.child("teleport-motion");frames.mkdirs();
        for(int n=0;n<48;n++) {
            float renderStep=Math.min(Gdx.graphics.getDeltaTime(),1f/30);
            step(Math.max(0,1f/30-renderStep));game.render();
            Pixmap frame=ScreenUtils.getFrameBufferPixmap(0,0,Gdx.graphics.getBackBufferWidth(),Gdx.graphics.getBackBufferHeight());
            PixmapIO.writePNG(frames.child(String.format(java.util.Locale.ROOT,"fire-%03d.png",n)),frame,-1,true);frame.dispose();
        }
    }
    private void finishRift() {
        for(int n=0;n<30&&(boolean)get(game,"teleportPending");n++)step(.02f);
        require(!(boolean)get(game,"teleportPending"),"The suction phase completes its teleport");
    }
    private void verifyTeleportCinematic() throws Exception {
        FileHandle previous=(FileHandle)get(game,"saveFile"),backup=output.child("before-teleport-cinematic.json");
        ((SaveData)call(game,"captureSave")).write(backup);
        try {
            key=-1;click=false;held.clear();set(game,"stage",4);call(game,"resetWorld");set(game,"scene",Screen.PLAYING);
            quietEnemies((Array<?>)get(game,"enemies"));Object player=get(game,"player");set(player,"x",192f);set(player,"y",136f);set(game,"riftsCast",1);
            addVial(PotionEffect.RIFT);step(.02f);
            require((boolean)get(game,"teleportPending")&&(float)get(player,"x")==192&&(float)get(player,"y")==136,"Blue hit begins suction before moving the hero");
            float targetX=(float)get(game,"teleportTargetX"),targetY=(float)get(game,"teleportTargetY");
            held.add(Input.Keys.D);click=true;step(.1f);held.clear();click=false;
            require((float)get(player,"x")==192&&(float)get(player,"y")==136&&(int)get(game,"attacks")==0,"Suction briefly locks movement and attacks while protecting the hero");
            step(.07f);game.render();screenshot("58-rift-suction");game.pause();
            float timer=((StatusEffects)get(player,"effects")).riftRemaining;for(int n=0;n<6;n++)game.render();
            require(((StatusEffects)get(player,"effects")).riftRemaining==timer&&(boolean)get(game,"teleportPending"),"Pause freezes the suction before arrival");
            FileHandle checkpoint=output.child("pending-teleport.json");((SaveData)call(game,"captureSave")).write(checkpoint);
            set(game,"saveFile",checkpoint);call(game,"loadGame");player=get(game,"player");
            require((boolean)get(game,"teleportPending")&&(float)get(player,"x")==192&&(float)get(game,"teleportTargetX")==targetX,"Loading resumes the same reserved destination and suction");
            finishRift();game.render();screenshot("59-rift-arrival");
            require(Vector2.dst2((float)get(player,"x"),(float)get(player,"y"),targetX,targetY)<.01f
                    &&(int)get(game,"summonedBats")==3&&(float)get(player,"hitInvuln")>0,"Arrival moves once, releases the screen effect and preserves protection without duplicating bats");
            SaveData saved=SaveData.decode(((SaveData)call(game,"captureSave")).encode());
            require(!saved.teleportPending&&saved.teleportTargetX==0&&saved.teleportTargetY==0,"Completed teleport clears its pending destination");
        } finally {
            key=-1;click=false;held.clear();set(game,"saveFile",backup);call(game,"loadGame");set(game,"saveFile",previous);set(game,"scene",Screen.PAUSED);
        }
    }
    private void clickPause(float localX,float localY) {
        GameUi ui=(GameUi)get(game,"ui");
        com.badlogic.gdx.utils.viewport.ExtendViewport uiViewport=(com.badlogic.gdx.utils.viewport.ExtendViewport)get(ui,"viewport");
        Vector2 point=new Vector2((uiViewport.getWorldWidth()-1060)/2+localX,(uiViewport.getWorldHeight()-552)/2+localY);
        uiViewport.project(point);mouseX=Math.round(point.x);mouseY=Gdx.graphics.getHeight()-Math.round(point.y);
        click=true;game.render();click=false;game.render();
    }
    private void guideKey(int next) {key=next;game.render();key=-1;game.render();}
    private void verifyPresentationFlow() throws Exception {
        FileHandle previous=(FileHandle)get(game,"saveFile"),backup=output.child("before-presentation.json");
        ((SaveData)call(game,"captureSave")).write(backup);
        GameUi ui=(GameUi)get(game,"ui");
        var preferences=(com.badlogic.gdx.Preferences)get(ui,"visualPreferences");
        boolean hadPreference=preferences.contains("reducedEffects"),previousPreference=preferences.getBoolean("reducedEffects",false);
        try {
            key=-1;click=false;held.clear();preferences.remove("reducedEffects");preferences.flush();
            ui.modeChanged(GameMode.REHABILITATION);require(ui.reducedEffects(),"Rehabilitation defaults to reduced visual effects");
            ui.modeChanged(GameMode.CONVENTIONAL);require(!ui.reducedEffects(),"Conventional mode keeps full effects before a manual preference");
            set(game,"stage",1);call(game,"resetWorld");set(game,"scene",Screen.PLAYING);
            quietEnemies((Array<?>)get(game,"enemies"));Object player=get(game,"player");
            set(player,"x",192f);set(player,"y",136f);set(player,"hp",4f);game.render();
            HudFeedback feedback=(HudFeedback)get(ui,"feedback");
            addHeart(192,136);call(game,"collectHearts");
            require((float)get(player,"hp")==5&&feedback.flights.size==1,"Pickup heals immediately and starts exactly one visual flight");
            call(game,"collectHearts");require(feedback.flights.size==1,"Consumed hearts cannot queue a second flight");
            game.render();game.pause();float flightAge=feedback.flights.first().age;
            for(int i=0;i<4;i++)game.render();
            require(feedback.flights.first().age==flightAge,"Pause freezes the collected heart in flight");
            set(game,"scene",Screen.PLAYING);
            Gdx.graphics.setWindowedMode(1280,800);Gdx.input=simulatedInput;Gdx.app=simulatedApplication;game.resize(1280,800);
            FileHandle frames=output.child("collection-motion");frames.mkdirs();
            for(int n=0;n<48;n++) {
                feedback.update((GameUi.View)get(game,"uiView"),Math.max(0,1f/30-Math.min(Gdx.graphics.getDeltaTime(),1f/30)));
                game.render();
                if(n==8)screenshot("61-heart-flight");if(n==23)screenshot("62-heart-hud-arrival");
                writeAnimationFrame(frames,n);
            }
            require(feedback.flights.isEmpty()&&(float)get(player,"hp")==5&&(int)get(game,"heartsCollected")==1,
                    "Flight completes at the HUD without applying healing or incrementing collection again");
            set(player,"mana",100f);set(player,"magicCd",0f);game.render();
            set(player,"mana",75f);set(player,"magicCd",.1f);game.render();
            require(feedback.manaPulse>0&&feedback.lagMana>75,"Spending mana leaves a fading crystal trail");
            set(player,"magicCd",0f);game.render();require(feedback.readyPulse>0,"Magic cooldown completion triggers a brief ready accent");
            set(player,"hp",4f);game.render();require(feedback.hurtPulse>0,"Damage animates the HUD without modifying life");
            game.pause();clickPause(148,232);require(get(ui,"page").toString().equals("SETTINGS"),"Visual comfort is available in Settings");
            clickPause(660,115);require(ui.reducedEffects()&&preferences.getBoolean("reducedEffects"),"The accessible toggle persists the user's preference");
            ui.modeChanged(GameMode.CONVENTIONAL);require(ui.reducedEffects(),"An explicit preference survives changing control modes");
            GameUi reopened=new GameUi((br.edu.motionbound.game.GameAssets)get(game,"art"));
            try{require(reopened.reducedEffects(),"Recreating the interface restores the visual preference");}finally{reopened.dispose();}
            screenshot("63-reduced-effects-settings");
            set(game,"scene",Screen.PLAYING);set(game,"screenShake",3f);set(game,"hitStop",.045f);
            float elapsed=(float)get(game,"elapsed");game.render();
            require((float)get(game,"screenShake")==0&&(float)get(game,"elapsed")>elapsed,"Reduced effects remove shaking and impact freeze without suspending gameplay");
            set(ui,"reducedEffects",false);
            Array<?> enemies=(Array<?>)get(game,"enemies");
            for(int n=0;n<enemies.size;n++){Object enemy=enemies.get(n);set(enemy,"type",n%7);set(enemy,"x",90f+n*24);set(enemy,"y",150f);set(enemy,"hp",0);}
            call(game,"resolveDeaths");tick("updateTransient",.12f);game.render();screenshot("64-distinct-enemy-defeats");
            require(((Array<?>)get(game,"deaths")).size==7,"Every enemy kind has a visible defeat animation");
            set(game,"stage",2);call(game,"startStage");set(game,"irisOpen",.3f);game.render();screenshot("65-pixel-iris-opening");
            require(feedback.flights.isEmpty(),"Stage changes clear visual pickups without carrying them into another map");
            set(game,"irisOpen",0f);set(game,"scene",Screen.PLAYING);
            player=get(game,"player");DungeonMap map=(DungeonMap)get(game,"dungeon");
            set(player,"x",map.exitX());set(player,"y",map.exitY());
            enemies=(Array<?>)get(game,"enemies");for(Object enemy:enemies)set(enemy,"hp",0);call(game,"resolveDeaths");tick("updateCamera",1f);step(.02f);
            require(scene()==Screen.LEVEL_COMPLETE&&(float)get(game,"irisClose")>0,"Reaching an unlocked exit starts the closing pixel iris");
            set(game,"irisClose",.25f);game.render();screenshot("66-pixel-iris-closing");
            key=Input.Keys.ENTER;game.render();key=-1;
            require(scene()==Screen.LEVEL_COMPLETE,"Confirming during the closing iris cannot activate the hidden HUD pause button");
            game.pause();float close=(float)get(game,"irisClose");game.render();require((float)get(game,"irisClose")==close,"Pausing also freezes the stage transition");
            set(game,"scene",Screen.LEVEL_COMPLETE);set(game,"irisClose",0f);game.render();set(ui,"screenAge",1f);game.render();screenshot("67-expedition-results");
            FileHandle transitionFrames=output.child("transition-motion");transitionFrames.mkdirs();
            for(int n=0;n<48;n++) {
                if(n<18)set(game,"irisClose",.5f*(1-n/18f));
                else {
                    if(n==18)call(game,"nextStage");
                    set(game,"irisOpen",Math.max(0,.6f*(1-(n-18)/20f)));
                }
                game.render();writeAnimationFrame(transitionFrames,n);
            }
        } finally {
            key=-1;click=false;held.clear();
            if(hadPreference)preferences.putBoolean("reducedEffects",previousPreference);else preferences.remove("reducedEffects");
            preferences.flush();set(game,"saveFile",backup);call(game,"loadGame");set(game,"saveFile",previous);set(game,"scene",Screen.PAUSED);
            Gdx.graphics.setWindowedMode(1280,720);Gdx.input=simulatedInput;Gdx.app=simulatedApplication;game.resize(1280,720);
        }
    }
    private void verifyAutomaticHealing() throws Exception {
        FileHandle previous=(FileHandle)get(game,"saveFile"),backup=output.child("before-auto-heal.json");
        ((SaveData)call(game,"captureSave")).write(backup);
        GameUi ui=(GameUi)get(game,"ui");boolean reduced=ui.reducedEffects();
        try {
            key=-1;click=false;held.clear();
            set(game,"mode",GameMode.REHABILITATION);set(game,"stage",1);call(game,"resetWorld");
            set(game,"scene",Screen.PLAYING);quietEnemies((Array<?>)get(game,"enemies"));
            Object player=get(game,"player");StatusEffects effects=(StatusEffects)get(player,"effects");
            HudFeedback feedback=(HudFeedback)get(ui,"feedback");
            Method damage=game.getClass().getDeclaredMethod("hurtPlayer",float.class,boolean.class);damage.setAccessible(true);
            set(player,"hp",2f);step(.02f);
            require((int)get(player,"heals")==3,"Rehabilitation does not consume a charge above one heart");
            effects.apply(PotionEffect.POISON);set(player,"hp",1.5f);effects.untilTick=.01f;step(.02f);
            require((float)get(player,"hp")==3&&(int)get(player,"heals")==2&&(int)get(game,"healsUsed")==1,
                    "A poison tick reaching one heart automatically restores two hearts and consumes one charge");
            require(effects.damageEffect==null&&effects.ticksRemaining==0,"Automatic healing cleanses damaging status effects");
            require(feedback.autoHealCharges==2&&feedback.autoHealRemaining>0,"Automatic healing queues the remaining-charge notice");
            set(ui,"reducedEffects",false);game.render();screenshot("68-automatic-heal-notice");
            game.pause();float frozen=feedback.autoHealRemaining;game.render();game.render();
            require(feedback.autoHealRemaining==frozen,"Pause freezes the automatic-heal notice");
            set(game,"scene",Screen.PLAYING);set(player,"hp",1f);step(.02f);
            require((float)get(player,"hp")==1&&(int)get(player,"heals")==2,"Healing cooldown prevents duplicate consumption");
            set(player,"healCd",.01f);step(.02f);
            require((float)get(player,"hp")==3&&(int)get(player,"heals")==1,"Low health heals when the cooldown expires");
            FileHandle checkpoint=output.child("automatic-heal-checkpoint.json");
            ((SaveData)call(game,"captureSave")).write(checkpoint);set(game,"saveFile",checkpoint);call(game,"loadGame");
            player=get(game,"player");feedback=(HudFeedback)get(ui,"feedback");
            require((int)get(player,"heals")==1&&(int)get(game,"healsUsed")==2&&feedback.autoHealRemaining==0,
                    "Loading preserves charges and statistics without replaying an old healing notice");
            set(game,"scene",Screen.PLAYING);quietEnemies((Array<?>)get(game,"enemies"));
            set(player,"hp",1.5f);set(player,"healCd",0f);set(player,"hitInvuln",0f);
            damage.invoke(game,1f,false);
            require((float)get(player,"hp")==2.5f&&(int)get(player,"heals")==0,"Damage crossing below one heart consumes the final charge");
            game.render();screenshot("69-automatic-heal-last-charge");
            set(player,"hp",1f);set(player,"healCd",0f);step(.02f);
            require((float)get(player,"hp")==1&&(int)get(player,"heals")==0,"An empty supply never grants free healing");
            set(player,"hp",.5f);set(player,"heals",1);set(player,"hitInvuln",0f);damage.invoke(game,.5f,false);
            require((float)get(player,"hp")==0&&(int)get(player,"heals")==1,"Automatic healing cannot revive lethal damage");
            set(game,"mode",GameMode.CONVENTIONAL);set(player,"hp",1.5f);set(player,"healCd",0f);
            damage.invoke(game,.5f,true);step(.02f);
            require((float)get(player,"hp")==1&&(int)get(player,"heals")==1,"Conventional mode keeps manual healing");
            set(game,"mode",GameMode.REHABILITATION);set(player,"hp",1f);set(player,"heals",1);
            ((SaveData)call(game,"captureSave")).write(checkpoint);set(game,"saveFile",checkpoint);call(game,"loadGame");
            player=get(game,"player");set(game,"scene",Screen.PAUSED);game.render();
            require((float)get(player,"hp")==1&&(int)get(player,"heals")==1,"A paused low-health checkpoint cannot spend a charge");
            set(game,"scene",Screen.PLAYING);step(.02f);
            require((float)get(player,"hp")==3&&(int)get(player,"heals")==0,"A low-health checkpoint heals once after resuming");
            call(game,"startStage");
            require((int)get(get(game,"player"),"heals")==3&&feedback.autoHealRemaining==0,"A new stage restores three charges and clears the notice");
        } finally {
            key=-1;click=false;held.clear();set(ui,"reducedEffects",reduced);
            set(game,"saveFile",backup);call(game,"loadGame");set(game,"saveFile",previous);set(game,"scene",Screen.PAUSED);
        }
    }
    private void physicalPacket(int buttons,float x,float y) {
        ((EdgeController)get(game,"edge")).offer(new EdgeFrame(99,usbSequence++,x,y,1,buttons,usbAttacks,usbMagic,7),System.nanoTime());
    }
    private void physicalButton(int buttons) {
        // PNG capture and map setup block this synthetic sender; a real USB worker
        // keeps receiving packets meanwhile. Model that continuing heartbeat here.
        set(get(game,"edge"),"lastTime",System.nanoTime());
        physicalPacket(0,.02f,-.03f);game.render();physicalPacket(buttons,.02f,-.03f);game.render();physicalPacket(0,.02f,-.03f);game.render();
    }
    private void verifyPhysicalController() throws Exception {
        FileHandle previous=(FileHandle)get(game,"saveFile"),backup=output.child("before-physical-controller.json");
        ((SaveData)call(game,"captureSave")).write(backup);
        EdgeController previousEdge=(EdgeController)get(game,"edge"),controller=new EdgeController(null);
        boolean hadSave=(boolean)get(game,"hasSave");GameUi ui=(GameUi)get(game,"ui");
        try {
            key=-1;click=false;held.clear();set(game,"edge",controller);set(game,"hasSave",false);
            set(game,"physicalControls",false);set(game,"scene",Screen.MODE_SELECT);set(ui,"previous",null);
            physicalPacket(0,.02f,-.03f);game.render();physicalButton(8);physicalButton(4);
            require(scene()==Screen.REHAB_SETUP,"Keypad alone selects rehabilitation from the opening menu");
            screenshot("70-usb-controller-setup");physicalButton(4);
            require(controller.calibrating(),"Confirm starts neutral calibration without a keyboard");
            set(controller,"calibrationStarted",System.nanoTime()-3_000_000_000L);
            for(int batch=0;batch<4;batch++){for(int n=0;n<20;n++)physicalPacket(0,.02f,-.03f);game.render();}
            require(controller.ready(),"Stable USB samples finish the calibration");screenshot("71-usb-controller-calibrated");
            physicalButton(4);require((scene()==Screen.LEVEL_INTRO||scene()==Screen.PLAYING)&&(boolean)get(game,"physicalControls"),"Keypad starts a real-controller expedition: "+scene()+", ready="+controller.ready()+", physical="+get(game,"physicalControls"));
            if(scene()==Screen.LEVEL_INTRO)advanceIntro(CampaignLevel.INTRO_SECONDS);
            quietEnemies((Array<?>)get(game,"enemies"));
            Object player=get(game,"player");verifyPhysicalSprint(controller,player);
            float x=(float)get(player,"x"),y=(float)get(player,"y");
            physicalPacket(0,.37f,.32f);game.render();step(.15f);
            require((float)get(player,"x")>x&&(float)get(player,"y")>y,"Physical tilt moves diagonally");
            int attacks=(int)get(game,"attacks");usbAttacks++;physicalPacket(0,.02f,-.03f);game.render();
            require((int)get(game,"attacks")==attacks+1,"A USB swipe produces a sword attack");
            step(.5f);int casts=(int)get(game,"magicCasts");usbMagic++;physicalPacket(0,.02f,-.03f);game.render();
            require((int)get(game,"magicCasts")==casts+1,"An upward USB swipe produces magic");
            screenshot("72-usb-controller-hud");
            physicalButton(1);require(scene()==Screen.PAUSED,"Keypad pauses the game");
            physicalButton(8);physicalButton(8);physicalButton(8);physicalButton(4);
            require(get(ui,"page").toString().equals("TUTORIAL"),"Keypad enters Tutorial");
            physicalButton(8);physicalButton(8);physicalButton(8);physicalButton(4);
            require(get(ui,"tutorialMode")==GameMode.CONVENTIONAL,"Keypad navigates and confirms a tutorial tab");
            screenshot("73-usb-keypad-tutorial");physicalButton(1);physicalButton(1);
            require(scene()==Screen.PLAYING,"Keypad returns from a subpage and resumes");
            physicalButton(1);physicalButton(8);physicalButton(8);physicalButton(4);
            for(int i=0;i<4;i++)physicalButton(8);physicalButton(4);
            require(scene()==Screen.CONTROLLER,"Keypad opens controller settings from pause");
            screenshot("74-usb-controller-settings");int sensitivity=controller.sensitivity;
            physicalButton(8);physicalButton(4);require(controller.sensitivity!=sensitivity,"Keypad adjusts sensitivity");
            physicalButton(1);physicalButton(1);require(scene()==Screen.PLAYING,"Keypad returns from controller settings");
            float elapsed=(float)get(game,"elapsed");set(controller,"lastTime",System.nanoTime()-EdgeController.STALE_NS-1);
            game.render();require(scene()==Screen.PAUSED&&(float)get(game,"elapsed")==elapsed,"Lost USB stream pauses before advancing gameplay");
            SaveData data=(SaveData)call(game,"captureSave");FileHandle checkpoint=output.child("physical-controller-checkpoint.json");data.write(checkpoint);
            require(data.physicalController&&!data.simulatedSensors,"Physical gameplay saves are labelled without fabricated pressure metrics");
            set(game,"saveFile",checkpoint);call(game,"loadGame");
            require(scene()==Screen.REHAB_SETUP,"Physical checkpoints require a ready controller before resuming");
        } finally {
            key=-1;click=false;held.clear();controller.close();set(game,"edge",previousEdge);set(game,"saveFile",backup);
            call(game,"loadGame");set(game,"saveFile",previous);set(game,"hasSave",hadSave);set(game,"scene",Screen.PAUSED);
        }
    }
    private void verifyGuidedCombat() throws Exception {
        FileHandle previous=(FileHandle)get(game,"saveFile"),backup=output.child("before-guided-combat.json");
        ((SaveData)call(game,"captureSave")).write(backup);
        try {
            held.clear();key=-1;click=false;set(game,"stage",1);set(game,"mode",GameMode.REHABILITATION);
            set(game,"physicalControls",false);call(game,"resetWorld");set(game,"scene",Screen.PLAYING);
            PlayerCommand command=new PlayerCommand();command.assistedAim=true;command.aim.set(-1,0);
            ControlSource input=delta->command;set(game,"controls",input);
            Object player=get(game,"player");set(player,"x",192f);set(player,"y",136f);
            Array<?> enemies=(Array<?>)get(game,"enemies");
            for(Object enemy:enemies){set(enemy,"x",600f);set(enemy,"y",400f);set(enemy,"speed",0f);set(enemy,"attackCd",6f);}
            Object target=enemies.get(0),neighbor=enemies.get(1),farther=enemies.get(2);
            set(target,"x",262f);set(target,"y",136f);
            set(neighbor,"x",267f);set(neighbor,"y",145f);
            set(farther,"x",290f);set(farther,"y",136f);
            step(.01f);require(get(game,"guidedTarget")==target,"Nearest visible enemy is marked even behind movement direction");
            game.render();screenshot("77-guided-target");
            command.attack=true;step(.02f);command.attack=false;
            GuidedStrike guided=(GuidedStrike)get(game,"guidedStrike");
            require(guided.phase==GuidedStrike.APPROACH&&(int)get(target,"hp")==2,"Gesture starts dash without remote damage");
            step(.06f);float pausedX=(float)get(player,"x"),remaining=guided.remaining;
            game.pause();game.render();
            require((float)get(player,"x")==pausedX&&guided.remaining==remaining,"Pause freezes the dash");
            FileHandle middle=output.child("guided-approach.json");((SaveData)call(game,"captureSave")).write(middle);
            set(game,"saveFile",middle);call(game,"loadGame");set(game,"controls",input);set(game,"scene",Screen.PLAYING);
            player=get(game,"player");enemies=(Array<?>)get(game,"enemies");target=enemies.get(0);neighbor=enemies.get(1);farther=enemies.get(2);
            for(Object enemy:enemies)set(enemy,"speed",0f);
            require(get(game,"guidedTarget")==target&&Math.abs(guided.remaining-remaining)<.001f,"Load resumes locked target and remaining travel");
            for(int n=0;n<30;n++)step(.02f);
            require((int)get(target,"hp")==1&&(int)get(neighbor,"hp")==1&&(int)get(farther,"hp")==3,
                    "One sword impact damages the group, never a distant enemy");
            require((int)get(game,"attacks")==1&&(int)get(game,"hits")==1,"Loading mid-dash cannot duplicate the gesture or hit");
            require(Math.abs((float)get(player,"x")-244)<.1f,"Forward dash stops before the enemy");
            game.render();screenshot("78-guided-impact");

            // Prepare a fresh close encounter: no forward travel, then retreat after damage.
            call(game,"resetWorld");set(game,"controls",input);set(game,"scene",Screen.PLAYING);
            player=get(game,"player");set(player,"x",192f);set(player,"y",136f);
            enemies=(Array<?>)get(game,"enemies");
            for(Object enemy:enemies){set(enemy,"x",600f);set(enemy,"y",400f);set(enemy,"speed",0f);set(enemy,"attackCd",6f);}
            target=enemies.first();set(target,"x",214f);set(target,"y",136f);
            step(.01f);game.render();screenshot("79-guided-close-target");
            command.attack=true;step(.02f);command.attack=false;
            require((float)get(player,"x")==192&&guided.phase==GuidedStrike.STRIKE,"Close enemy skips forward dash");
            for(int n=0;n<12&&guided.phase==GuidedStrike.STRIKE;n++)step(.02f);
            require((int)get(target,"hp")==1&&guided.phase==GuidedStrike.RETREAT&&(float)get(player,"x")==192,
                    "Sword damage occurs before the backward dash");
            for(int n=0;n<6;n++)step(.02f);
            require(Math.abs((float)get(player,"x")-174)<.1f&&!guided.active(),"Close hit retreats 18 units");

            // Occlusion, range, fallback and conventional combat.
            set(target,"x",274f);set(target,"y",136f);step(.01f);
            require(get(game,"guidedTarget")==null,"Beyond triple reach there is no marker");
            set(player,"x",280f);set(player,"y",200f);set(target,"x",360f);set(target,"y",200f);step(.01f);
            require(get(game,"guidedTarget")==null,"Wall hides a target even inside assisted range");
            set(player,"attackCd",0f);((MeleeSwing)get(game,"swing")).elapsed=-1;
            command.attack=true;step(.01f);command.attack=false;
            require(!guided.active(),"No target gives a normal sword attack without dash");
            set(game,"mode",GameMode.CONVENTIONAL);set(player,"attackCd",0f);((MeleeSwing)get(game,"swing")).elapsed=-1;
            set(player,"x",192f);set(player,"y",136f);set(target,"x",262f);set(target,"y",136f);
            command.attack=true;step(.01f);command.attack=false;
            require(!guided.active()&&(float)get(player,"x")==192,"Conventional combat never gains guided dash");
        } finally {
            set(game,"saveFile",backup);call(game,"loadGame");set(game,"saveFile",previous);set(game,"scene",Screen.PAUSED);
        }
    }
    private void verifyPhysicalSprint(EdgeController controller,Object player) {
        float startX=(float)get(player,"x"),startY=(float)get(player,"y");controller.reset();
        for(int i=0;i<150;i++) {
            set(player,"x",192f);set(player,"y",136f);
            physicalPacket(0,.37f,-.03f);controller.update(System.nanoTime());step(.02f);
        }
        require(!(boolean)get(player,"running")&&controller.sprintProgress()>.74f&&controller.sprintProgress()<.76f,
                "Sustained strong tilt is still walking after three seconds");
        physicalPacket(0,.37f,-.03f);game.render();screenshot("75-tilt-sprint-countdown");
        set(controller,"lastTime",System.nanoTime());
        for(int i=0;i<60&&!((boolean)get(player,"running"));i++) {
            set(player,"x",192f);physicalPacket(0,.37f,-.03f);controller.update(System.nanoTime());step(.02f);
        }
        require((boolean)get(player,"running"),"Sustained tilt starts the actual running animation and movement state");
        set(player,"x",192f);float vigor=(float)get(player,"stamina");step(.1f);
        require((float)get(player,"x")>200&&(float)get(player,"stamina")<vigor,"Physical sprint increases speed and spends vigor");
        physicalPacket(0,.37f,-.03f);game.render();screenshot("76-tilt-sprint-active");
        set(controller,"lastTime",System.nanoTime());
        physicalPacket(0,-.33f,-.03f);controller.update(System.nanoTime());step(.02f);
        require(!(boolean)get(player,"running")&&controller.sprintProgress()<.01f,"Reversing tilt immediately cancels sprint despite movement smoothing");
        for(int i=0;i<201;i++) {
            set(player,"x",192f);physicalPacket(0,.37f,-.03f);controller.update(System.nanoTime());step(.02f);
        }
        require((boolean)get(player,"running"),"A new direction needs and completes its own charge");
        set(player,"stamina",.5f);step(.02f);
        require(!(boolean)get(player,"running")&&controller.sprintNeedsRelease(),"Exhaustion cancels running and requests a relaxed hand");
        for(int i=0;i<60;i++) {
            set(player,"x",192f);physicalPacket(0,.37f,-.03f);controller.update(System.nanoTime());step(.02f);
            require(!(boolean)get(player,"running")&&controller.sprintProgress()==0,"Held tilt cannot pulse the exhausted sprint");
        }
        physicalPacket(0,.02f,-.03f);controller.update(System.nanoTime());step(.02f);
        require(!controller.sprintNeedsRelease(),"Relaxing the hand re-arms running");
        controller.reset();set(player,"x",startX);set(player,"y",startY);set(player,"stamina",100f);
    }
    private void writeAnimationFrame(FileHandle frames,int index) {
        Pixmap image=ScreenUtils.getFrameBufferPixmap(0,0,Gdx.graphics.getBackBufferWidth(),Gdx.graphics.getBackBufferHeight());
        PixmapIO.writePNG(frames.child(String.format(java.util.Locale.ROOT,"fire-%03d.png",index)),image,-1,true);image.dispose();
    }
    private void verifyGuideMenus() {
        GameUi ui=(GameUi)get(game,"ui");set(game,"scene",Screen.PAUSED);key=-1;click=false;game.render();
        SaveData before=(SaveData)call(game,"captureSave");
        clickPause(148,178);
        require(get(ui,"page").toString().equals("TUTORIAL"),"Mouse opens the tutorial");
        require(get(ui,"tutorialMode")==before.mode,"Tutorial starts on the current expedition mode");
        screenshot("44-tutorial-conventional-icons");
        clickPause(843,424);require(get(ui,"tutorialMode")==GameMode.REHABILITATION,"Rehabilitation tab can be read independently of the expedition mode");
        screenshot("45-tutorial-rehabilitation-gestures");
        guideKey(Input.Keys.LEFT);require(get(ui,"tutorialMode")==GameMode.CONVENTIONAL,"Left/right switches tutorial tabs");
        guideKey(Input.Keys.TAB);require((int)get(ui,"focus")==7,"Tab reaches the next guide control");
        guideKey(Input.Keys.ENTER);require(get(ui,"tutorialMode")==GameMode.REHABILITATION,"Enter activates a focused tab");
        clickPause(148,124);require(get(ui,"page").toString().equals("BESTIARY"),"Mouse opens the bestiary catalogue");
        clickPause(507,424);screenshot("46-bestiary-catalogue");
        for(int entry=0;entry<7;entry++) {
            clickPause(451+(entry%3)*224,348-(entry/3)*96);
            require((int)get(ui,"detailEntry")==entry,"Every enemy has an expandable detail entry, including bats");
            if(entry==2||entry==3||entry==6)screenshot("47-enemy-detail-"+entry);
            guideKey(Input.Keys.ESCAPE);
            require(scene()==Screen.PAUSED&&get(ui,"page").toString().equals("BESTIARY")&&(int)get(ui,"detailEntry")==-1,"Escape closes details and keeps the catalogue paused");
        }
        clickPause(843,424);require((int)get(ui,"catalogueTab")==1,"Potion catalogue has a separate tab");
        screenshot("48-potions-catalogue");
        for(int entry=0;entry<4;entry++) {
            clickPause(507+(entry%2)*336,324-(entry/2)*144);
            require((int)get(ui,"detailEntry")==entry,"All four potion entries open their own details");
            screenshot("49-potion-detail-"+entry);guideKey(Input.Keys.ESCAPE);
        }
        guideKey(Input.Keys.LEFT);require((int)get(ui,"catalogueTab")==0,"Keyboard switches back to enemies");
        guideKey(Input.Keys.TAB);guideKey(Input.Keys.TAB);guideKey(Input.Keys.ENTER);
        require((int)get(ui,"detailEntry")==0,"Keyboard can open the first catalogue entry");
        for(int[] size:new int[][]{{960,540},{1440,720},{1920,1080}}) {
            Gdx.graphics.setWindowedMode(size[0],size[1]);Gdx.input=simulatedInput;Gdx.app=simulatedApplication;
            game.resize(size[0],size[1]);game.render();
            screenshot("50-guide-detail-"+size[0]);guideKey(Input.Keys.ESCAPE);
            clickPause(148,178);clickPause(507,424);
            require(get(ui,"page").toString().equals("TUTORIAL")&&get(ui,"tutorialMode")==GameMode.CONVENTIONAL,"Tutorial controls remain interactive after resizing");
            screenshot("51-tutorial-icons-"+size[0]);
            clickPause(843,424);screenshot("52-tutorial-gestures-"+size[0]);
            clickPause(148,124);clickPause(507,424);clickPause(451,348);
        }
        guideKey(Input.Keys.ESCAPE);guideKey(Input.Keys.ESCAPE);
        require(scene()==Screen.PAUSED&&get(ui,"page").toString().equals("SUMMARY"),"Escape goes from detail to catalogue to expedition summary");
        SaveData after=(SaveData)call(game,"captureSave");
        require(after.mode==before.mode&&after.hp==before.hp&&after.elapsed==before.elapsed&&after.attacks==before.attacks
                &&after.mana==before.mana&&after.enemies.size==before.enemies.size,"Reading both modes and catalogues never changes gameplay or advances time");
        Gdx.graphics.setWindowedMode(1280,720);Gdx.input=simulatedInput;Gdx.app=simulatedApplication;
        game.resize(1280,720);key=-1;click=false;game.render();set(ui,"focus",0);
    }
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
            require((float)get(player,"hp")==5&&(int)get(player,"heals")==2,"Space consumes one potion and restores two hearts");
            game.render();require(scene()==Screen.PLAYING,"Space healing cannot activate the pause button");
            screenshot("19-healing-animation");key=-1;

            Array<?> hearts=(Array<?>)get(game,"hearts");hearts.clear();set(game,"heartsCollected",0);
            set(player,"hp",6);addHeart(192,136);call(game,"collectHearts");
            require(hearts.size==1&&(float)get(player,"hp")==6,"Full life does not consume a drop");
            set(player,"hp",5);call(game,"collectHearts");
            require(hearts.size==0&&(float)get(player,"hp")==6&&(int)get(game,"heartsCollected")==1,"Pickup restores one heart with a hard life cap");
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
            require(scene()==Screen.LEVEL_INTRO&&get(game,"mode")==GameMode.REHABILITATION,"Explicit simulator entry starts the same campaign");
            advanceIntro(CampaignLevel.INTRO_SECONDS);
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
            // Both modes use the same campaign transitions, without changing sensor thresholds.
            set(game,"scene",Screen.PLAYING);
            for(Object enemy:enemies)set(enemy,"hp",0);
            call(game,"resolveDeaths");
            DungeonMap map=(DungeonMap)get(game,"dungeon");set(player,"x",map.exitX());set(player,"y",map.exitY());step(.02f);
            call(game,"nextStage");
            require((int)get(game,"stage")==2&&scene()==Screen.LEVEL_INTRO&&get(game,"mode")==GameMode.REHABILITATION,
                    "Rehabilitation mode survives stage progression");
        } finally {
            held.clear();key=-1;click=false;mouseButton=Input.Buttons.LEFT;
            set(game,"saveFile",backup);call(game,"loadGame");set(game,"saveFile",previousFile);set(game,"scene",Screen.PAUSED);
        }
    }
    private void advanceIntro(float delta) {
        try {Method method=game.getClass().getDeclaredMethod("updateIntro",float.class);method.setAccessible(true);method.invoke(game,delta);}
        catch(ReflectiveOperationException e){throw new AssertionError(e);}
    }
    private void verifyEnemyEffectsFlow() throws Exception {
        FileHandle previous=(FileHandle)get(game,"saveFile"),backup=output.child("before-effects-check.json");
        ((SaveData)call(game,"captureSave")).write(backup);
        try {
            key=-1;click=false;held.clear();set(game,"stage",4);call(game,"resetWorld");set(game,"scene",Screen.PLAYING);
            Object player=get(game,"player");set(player,"x",192f);set(player,"y",136f);
            Array<?> enemies=(Array<?>)get(game,"enemies");quietEnemies(enemies);
            Object skeleton=enemies.first();set(skeleton,"type",1);set(skeleton,"hp",3);set(skeleton,"maxHp",3);
            set(skeleton,"x",215f);set(skeleton,"y",136f);set(skeleton,"attackCd",0f);
            step(.02f);require((float)get(skeleton,"windup")>0&&(float)get(player,"hp")==6,"Enemy warns before contact damage");
            game.render();screenshot("31-enemy-windup");
            set(player,"y",100f);for(int i=0;i<35;i++)step(.02f);
            require((float)get(player,"hp")==6,"Moving away dodges the prepared melee attack");
            set(player,"y",136f);set(skeleton,"attackCd",0f);set(skeleton,"actionTime",0f);step(.02f);
            pointAt(245,136);click=true;step(.02f);click=false;for(int i=0;i<10;i++)step(.02f);
            require((float)get(skeleton,"windup")==0&&(int)get(skeleton,"hp")==2,"Sword impact interrupts enemy preparation");
            quietEnemies(enemies);
            Object mage=enemies.get(1);set(mage,"type",EnemyKind.MAGE.ordinal());set(mage,"hp",2);set(mage,"maxHp",2);
            set(mage,"x",267f);set(mage,"y",136f);set(mage,"attackCd",0f);
            step(.02f);game.render();screenshot("32-mage-target-warning");
            SaveData warning=SaveData.decode(((SaveData)call(game,"captureSave")).encode());
            require(warning.enemies.get(1).windup>0&&warning.enemies.get(1).potion==PotionEffect.POISON,"Wind-up and locked potion are saved");
            Array<?> vials=(Array<?>)get(game,"vials");
            for(int i=0;i<80&&vials.isEmpty();i++)step(.02f);
            require(vials.size==1,"Mage releases a physical vial after warning");
            SaveData airborne=(SaveData)call(game,"captureSave");FileHandle airborneFile=output.child("airborne-vial-check.json");airborne.write(airborneFile);
            set(game,"saveFile",airborneFile);call(game,"loadGame");
            player=get(game,"player");enemies=(Array<?>)get(game,"enemies");quietEnemies(enemies);vials=(Array<?>)get(game,"vials");
            require(vials.size==1,"Loading resumes the same airborne vial");
            game.render();screenshot("33-vial-flight");
            set(player,"y",112f);for(int i=0;i<55;i++)step(.02f);
            StatusEffects effects=(StatusEffects)get(player,"effects");
            require(vials.isEmpty()&&effects.ticksRemaining==0&&(float)get(player,"hp")==6,"Dodging the landing circle avoids the effect");

            set(player,"hitInvuln",0f);addVial(PotionEffect.FIRE);step(.02f);
            require(effects.damageEffect==PotionEffect.FIRE&&effects.ticksRemaining==4,"Red vial applies burning");
            while(effects.ticksRemaining==4)step(.02f);
            require((float)get(player,"hp")==5.5f,"Burning removes half a heart per tick");
            game.render();screenshot("34-burning-half-heart");
            FileHandle effectFile=output.child("active-effect-check.json");((SaveData)call(game,"captureSave")).write(effectFile);
            set(game,"saveFile",effectFile);call(game,"loadGame");player=get(game,"player");effects=(StatusEffects)get(player,"effects");
            require(effects.ticksRemaining==3&&(float)get(player,"hp")==5.5f,"Half hearts and remaining dose survive loading");
            quietEnemies((Array<?>)get(game,"enemies"));
            set(player,"x",192f);set(player,"y",184f);
            while(effects.ticksRemaining>0)step(.02f);
            require((float)get(player,"hp")==4,"Complete burn totals exactly two hearts");
            set(player,"hp",6f);set(player,"hitInvuln",0f);addVial(PotionEffect.POISON);step(.02f);
            key=Input.Keys.ESCAPE;game.render();key=-1;
            float frozen=effects.untilTick;for(int i=0;i<10;i++)game.render();
            require(scene()==Screen.PAUSED&&effects.untilTick==frozen&&(float)get(player,"hp")==6,"Pause freezes poison and health");
            key=Input.Keys.ESCAPE;game.render();key=-1;
            set(player,"hitInvuln",0f);addVial(PotionEffect.SLOW);step(.02f);
            float beforeX=(float)get(player,"x");held.add(Input.Keys.D);for(int i=0;i<10;i++)step(.01f);held.clear();
            float moved=(float)get(player,"x")-beforeX;
            require(moved>3.8f&&moved<4.1f,"Grey vial actually slows movement to sixty percent");
            game.render();screenshot("35-poison-and-slow-indicators");
            ((Array<?>)get(game,"groundAreas")).clear();
            set(player,"hp",4f);key=Input.Keys.SPACE;step(.02f);key=-1;
            require((float)get(player,"hp")==6&&effects.ticksRemaining==0&&effects.slowRemaining>0,"Healing cleanses damaging effects");
            effects.slowRemaining=0;set(player,"hitInvuln",0f);set(game,"riftsCast",1);
            float originX=(float)get(player,"x"),originY=(float)get(player,"y");addVial(PotionEffect.RIFT);step(.02f);
            finishRift();
            DungeonMap map=(DungeonMap)get(game,"dungeon");
            require((int)get(game,"summonedBats")==3&&(int)get(game,"totalEnemies")==21
                    &&Vector2.dst2(originX,originY,(float)get(player,"x"),(float)get(player,"y"))>=96*96
                    &&map.canStand((float)get(player,"x"),(float)get(player,"y"),5),"Blue vial teleports safely and creates exactly three bats: bats="
                            +get(game,"summonedBats")+", total="+get(game,"totalEnemies")+", distance="+Vector2.dst(originX,originY,(float)get(player,"x"),(float)get(player,"y")));
            game.render();screenshot("36-rift-and-bat-arrival");
            FileHandle riftFile=output.child("rift-bats-check.json");((SaveData)call(game,"captureSave")).write(riftFile);
            set(game,"saveFile",riftFile);call(game,"loadGame");player=get(game,"player");
            require((int)get(game,"summonedBats")==3&&(int)get(game,"totalEnemies")==21,"Summoned enemies survive loading without duplication");
            set(player,"hitInvuln",0f);((StatusEffects)get(player,"effects")).riftRemaining=0;
            set(game,"riftsCast",2);addVial(PotionEffect.RIFT);step(.02f);
            finishRift();
            require((int)get(game,"summonedBats")==6,"Second rift creates a second wave");
            set(player,"hitInvuln",0f);addVial(PotionEffect.RIFT);step(.02f);
            require((int)get(game,"summonedBats")==6,"Rifts cannot create infinite waves");
            enemies=(Array<?>)get(game,"enemies");int heartsBefore=((Array<?>)get(game,"hearts")).size;
            for(Object enemy:enemies)if((int)get(enemy,"type")==EnemyKind.BAT.ordinal())set(enemy,"hp",0);
            call(game,"resolveDeaths");require(((Array<?>)get(game,"hearts")).size==heartsBefore,"Summoned bats do not farm health drops");
            for(Object enemy:enemies)set(enemy,"hp",0);call(game,"resolveDeaths");
            set(player,"x",map.exitX());set(player,"y",map.exitY());step(.02f);
            require(scene()==Screen.LEVEL_COMPLETE&&(int)get(game,"defeated")==24,"Stage exit accounts for the summoned enemies");
            key=Input.Keys.ESCAPE;game.render();key=-1;game.render();
            for(int i=0;i<4;i++){key=Input.Keys.DOWN;game.render();key=-1;game.render();}
            key=Input.Keys.ENTER;game.render();key=-1;game.render();screenshot("37-bestiary-and-potions");
        } finally {
            held.clear();key=-1;click=false;
            set(game,"saveFile",backup);call(game,"loadGame");set(game,"saveFile",previous);set(game,"scene",Screen.PAUSED);
        }
    }
    private void quietEnemies(Array<?> enemies) {
        for(Object enemy:enemies) {
            set(enemy,"x",600f);set(enemy,"y",400f);set(enemy,"speed",0f);set(enemy,"attackCd",5f);
            set(enemy,"windup",0f);set(enemy,"actionTime",0f);set(enemy,"kickX",0f);set(enemy,"kickY",0f);
            set(enemy,"burstRemaining",0);set(enemy,"burstTimer",0f);set(enemy,"comboPending",false);set(enemy,"attack",CasterAttack.NONE);
        }
    }
    private void verifyGroundAreasFlow() throws Exception {
        FileHandle previous=(FileHandle)get(game,"saveFile"),backup=output.child("before-ground-areas.json");
        ((SaveData)call(game,"captureSave")).write(backup);
        try {
            held.clear();key=-1;click=false;
            for(PotionEffect type:new PotionEffect[]{PotionEffect.FIRE,PotionEffect.POISON,PotionEffect.SLOW}) {
                set(game,"stage",4);call(game,"resetWorld");set(game,"scene",Screen.PLAYING);
                quietEnemies((Array<?>)get(game,"enemies"));Object player=get(game,"player");set(player,"x",192f);set(player,"y",136f);
                addVial(type);step(.02f);Array<?> areas=(Array<?>)get(game,"groundAreas");
                StatusEffects effects=(StatusEffects)get(player,"effects");
                require(areas.size==1&&get(areas.first(),"effect")==type&&(float)get(areas.first(),"remaining")==5,"Direct impact applies the status AND creates a five-second area: "+type);
                effects.advance(.2f);float clock=type==PotionEffect.SLOW?effects.slowRemaining:effects.untilTick;
                tick("updateGroundAreas",.2f);
                require((type==PotionEffect.SLOW?effects.slowRemaining:effects.untilTick)==clock,"Standing in an area cannot refresh an active status: "+type);
                FileHandle checkpoint=output.child("ground-area-"+type+".json");((SaveData)call(game,"captureSave")).write(checkpoint);
                set(game,"saveFile",checkpoint);call(game,"loadGame");player=get(game,"player");effects=(StatusEffects)get(player,"effects");
                areas=(Array<?>)get(game,"groundAreas");
                require(areas.size==1&&(float)get(areas.first(),"remaining")==4.8f,"Area lifetime is retained by loading: "+type);
                game.pause();float life=(float)get(areas.first(),"remaining");game.render();game.render();
                require((float)get(areas.first(),"remaining")==life,"Pause freezes area lifetime and animation clock: "+type);
                set(game,"scene",Screen.PLAYING);quietEnemies((Array<?>)get(game,"enemies"));set(player,"x",230f);
                for(int n=0;n<192;n++)step(.02f);
                require((type==PotionEffect.SLOW?effects.slowRemaining==0:effects.ticksRemaining==0)&&!areas.isEmpty(),"The original status expires before its five-second source area: "+type);
                set(player,"x",192f);set(player,"hitInvuln",0f);step(.02f);
                require(type==PotionEffect.SLOW?effects.slowRemaining==4:effects.ticksRemaining==4,"Re-entering reacquires the effect only after expiry: "+type);
                set(player,"x",230f);for(int n=0;n<52;n++)step(.02f);
                require(areas.isEmpty()&&(type==PotionEffect.SLOW?effects.slowRemaining>0:effects.ticksRemaining>0),"Ground expires after five seconds without removing an acquired status: "+type);
                effects.cleanseDamage();effects.slowRemaining=0;set(player,"hitInvuln",0f);
                set(player,"x",192f);addVial(type);set(player,"x",230f);tick("updateVials",.02f);
                require(areas.size==1&&effects.ticksRemaining==0&&effects.slowRemaining==0,"A missed vial still leaves its area without immediately applying a status: "+type);
                set(player,"x",192f);tick("updateGroundAreas",.02f);
                require(type==PotionEffect.SLOW?effects.slowRemaining==4:effects.damageEffect==type,"Entering a previously missed vial's area applies the first dose: "+type);
            }
            set(game,"stage",4);call(game,"resetWorld");set(game,"scene",Screen.PLAYING);
            quietEnemies((Array<?>)get(game,"enemies"));Object player=get(game,"player");set(player,"x",192f);set(player,"y",136f);set(game,"riftsCast",1);
            addVial(PotionEffect.RIFT);step(.02f);Array<?> areas=(Array<?>)get(game,"groundAreas");Object rift=areas.first();
            finishRift();
            require(areas.size==1&&(boolean)get(rift,"summoned")&&(int)get(game,"summonedBats")==3,"Direct blue impact leaves a reusable portal and summons one wave");
            FileHandle checkpoint=output.child("used-ground-rift.json");((SaveData)call(game,"captureSave")).write(checkpoint);
            set(game,"saveFile",checkpoint);call(game,"loadGame");player=get(game,"player");areas=(Array<?>)get(game,"groundAreas");rift=areas.first();
            require((boolean)get(rift,"summoned"),"Loading remembers which ground rift already summoned its bats");
            set(player,"x",192f);set(player,"y",136f);set(player,"hitInvuln",0f);tick("updateGroundAreas",.02f);
            require((float)get(player,"x")==192&&(float)get(player,"y")==136,"Arrival protection prevents immediate portal chains");
            ((StatusEffects)get(player,"effects")).advance(1.5f);tick("updateGroundAreas",.02f);
            finishRift();
            require(Vector2.dst2(192,136,(float)get(player,"x"),(float)get(player,"y"))>=96*96&&(int)get(game,"summonedBats")==3,"The same portal teleports again after arrival without duplicating bats");
            ((StatusEffects)get(player,"effects")).advance(1.5f);set(player,"hitInvuln",0f);set(game,"riftsCast",2);
            addVial(PotionEffect.RIFT);tick("updateVials",.02f);finishRift();require((int)get(game,"summonedBats")==6,"A second distinct rift can summon the second wave");
            ((StatusEffects)get(player,"effects")).advance(1.5f);set(player,"hitInvuln",0f);set(player,"x",192f);set(player,"y",136f);
            tick("updateGroundAreas",.02f);
            finishRift();
            require(Vector2.dst2(192,136,(float)get(player,"x"),(float)get(player,"y"))>=96*96&&(int)get(game,"summonedBats")==6,"The portal still teleports when the six-bat cap is reached");
            SaveData.decode(((SaveData)call(game,"captureSave")).encode());
            tick("updateGroundAreas",5.1f);require(areas.isEmpty(),"Blue portals disappear after five seconds");
            // Show the four real rendered areas together, without applying them to the player.
            set(game,"stage",1);call(game,"resetWorld");set(game,"scene",Screen.PLAYING);quietEnemies((Array<?>)get(game,"enemies"));
            player=get(game,"player");int n=0;
            for(PotionEffect type:PotionEffect.values()) {
                set(player,"x",128f+n++*48);set(player,"y",136f);addVial(type);set(player,"x",192f);set(player,"y",184f);tick("updateVials",.02f);
            }
            tick("updateGroundAreas",1.1f);set(game,"scene",Screen.PAUSED);game.render();
            roomScreenshot("53-four-persistent-potion-areas",208,136);
        } finally {
            held.clear();key=-1;click=false;set(game,"saveFile",backup);call(game,"loadGame");set(game,"saveFile",previous);set(game,"scene",Screen.PAUSED);
        }
    }
    private void tick(String method,float dt) {
        try {Method m=game.getClass().getDeclaredMethod(method,float.class);m.setAccessible(true);m.invoke(game,dt);}
        catch(ReflectiveOperationException e){throw new AssertionError(e);}
    }
    private void verifyCasterCombosFlow() throws Exception {
        FileHandle previous=(FileHandle)get(game,"saveFile"),backup=output.child("before-combos-check.json");
        ((SaveData)call(game,"captureSave")).write(backup);
        try {
            held.clear();key=-1;click=false;set(game,"stage",4);call(game,"resetWorld");set(game,"scene",Screen.PLAYING);
            Object player=get(game,"player");set(player,"x",192f);set(player,"y",136f);
            Array<?> enemies=(Array<?>)get(game,"enemies");quietEnemies(enemies);
            Object mage=enemies.first();set(mage,"type",EnemyKind.MAGE.ordinal());set(mage,"hp",2);set(mage,"maxHp",2);
            set(mage,"x",267f);set(mage,"y",136f);set(mage,"cycle",0);
            Array<?> vials=(Array<?>)get(game,"vials"),bolts=(Array<?>)get(game,"lightning");
            for(int cast=0;cast<3;cast++) {
                set(mage,"actionTime",0f);set(mage,"attackCd",0f);vials.clear();
                tick("updateEnemies",.02f);
                require(get(mage,"attack")== (cast<2?CasterAttack.POISON_VIAL:CasterAttack.LIGHTNING_FAN),"Mage cycle: two green throws then lightning");
                if(cast==2)roomScreenshot("38-mage-lightning-warning",235,136);
                for(int n=0;n<80&&(float)get(mage,"windup")>0;n++)tick("updateEnemies",.02f);
                if(cast<2)require(vials.size==1&&get(vials.first(),"effect")==PotionEffect.POISON,"Mage never substitutes fire or blue for poison");
            }
            tick("updateEnemies",.02f);
            require(bolts.size==1&&(int)get(mage,"burstRemaining")==4,"Lightning starts a timed five-shot burst");
            float locked=(float)get(mage,"burstAngle");
            FileHandle burstFile=output.child("lightning-burst-check.json");((SaveData)call(game,"captureSave")).write(burstFile);
            set(game,"saveFile",burstFile);call(game,"loadGame");
            player=get(game,"player");enemies=(Array<?>)get(game,"enemies");mage=enemies.first();bolts=(Array<?>)get(game,"lightning");
            require(bolts.size==1&&(int)get(mage,"burstRemaining")==4,"Loading keeps emitted bolts and remaining burst");
            set(player,"y",180f);
            for(int n=0;n<25;n++)tick("updateEnemies",.02f);
            require(bolts.size==5&&(int)get(mage,"cycle")==3,"Exactly five bolts, one completed cycle");
            for(int n=0;n<5;n++) {
                Object bolt=bolts.get(n);
                float angle=new Vector2((float)get(bolt,"vx"),(float)get(bolt,"vy")).angleDeg();
                require(Math.abs(angle-(locked-22.5f+n*11.25f))<.1f,"Each bolt keeps its original 45-degree fan direction despite player movement");
            }
            roomScreenshot("39-mage-lightning-fan",235,136);
            // Sweep collision cannot skip the player even with a longer frame.
            Object first=bolts.first();set(first,"x",240f);set(first,"y",180f);set(first,"vx",-150f);set(first,"vy",0f);
            set(player,"x",220f);set(player,"hitInvuln",0f);tick("updateLightning",.2f);
            require((float)get(player,"hp")==5.5f,"Lightning deals a single half-heart hit along its swept path");
            bolts.clear();((Array<?>)get(game,"vials")).clear();quietEnemies(enemies);
            set(player,"x",192f);set(player,"y",136f);set(player,"hp",6f);set(player,"hitInvuln",0f);
            Object warlock=enemies.get(1);set(warlock,"type",EnemyKind.WARLOCK.ordinal());set(warlock,"hp",3);set(warlock,"maxHp",3);
            set(warlock,"x",230f);set(warlock,"y",136f);set(warlock,"defensiveCd",0f);
            tick("updateEnemies",.02f);
            require(get(warlock,"attack")==CasterAttack.QUICK_SLOW,"Close approach triggers quick grey counter even during cooldown");
            roomScreenshot("40-warlock-close-counter",213,136);
            vials=(Array<?>)get(game,"vials");
            for(int n=0;n<30&&vials.isEmpty();n++)tick("updateEnemies",.02f);
            require(vials.size==1&&(boolean)get(vials.first(),"direct")&&(float)get(vials.first(),"flight")<.3f,"Quick grey vial travels directly and quickly");
            set(player,"x",210f);tick("updateVials",.1f);
            StatusEffects effects=(StatusEffects)get(player,"effects");
            require(vials.isEmpty()&&effects.slowRemaining==4,"Grey counter hits during flight, before its landing time");
            ((Array<?>)get(game,"groundAreas")).clear();
            for(int n=0;n<60&&(float)get(warlock,"windup")==0;n++)tick("updateEnemies",.02f);
            require(get(warlock,"attack")==CasterAttack.FIRE_FAN,"Grey counter is followed by red fan");
            roomScreenshot("41-warlock-fire-warning",205,136);
            for(int n=0;n<60&&vials.isEmpty();n++)tick("updateEnemies",.02f);
            require(vials.size==3,"Warlock releases three fire vials simultaneously");
            for(int n=0;n<3;n++) {
                Object vial=vials.get(n);
                float a=new Vector2((float)get(vial,"toX")-(float)get(vial,"fromX"),(float)get(vial,"toY")-(float)get(vial,"fromY")).angleDeg();
                require(get(vial,"effect")==PotionEffect.FIRE&&Math.abs(a-(157.5f+n*22.5f))<.15f,"Three red vials cover the fixed 45-degree fan");
            }
            set(player,"x",192f);set(player,"y",184f);tick("updateVials",1.1f);
            Array<?> fires=(Array<?>)get(game,"groundAreas");require(fires.size==3,"Each red vial leaves a ground fire");
            Object fire=fires.first();set(player,"x",get(fire,"x"));set(player,"y",get(fire,"y"));
            tick("updateGroundAreas",.1f);require(effects.damageEffect==PotionEffect.FIRE,"Walking into an existing fire ignites the player");
            quietEnemies(enemies);roomScreenshot("42-fire-ground-and-burning",205,136);
            FileHandle fireFile=output.child("ground-fire-check.json");((SaveData)call(game,"captureSave")).write(fireFile);
            set(game,"saveFile",fireFile);call(game,"loadGame");
            player=get(game,"player");effects=(StatusEffects)get(player,"effects");fires=(Array<?>)get(game,"groundAreas");
            require(fires.size==3&&effects.ticksRemaining==4,"Ground duration and burn dose both survive saving");
            game.pause();float remaining=(float)get(fires.first(),"remaining");game.render();game.render();
            require(scene()==Screen.PAUSED&&(float)get(fires.first(),"remaining")==remaining,"Pause freezes ground fire");
            set(player,"x",192f);set(player,"y",184f);set(game,"scene",Screen.PLAYING);
            tick("updateGroundAreas",4.91f);require(fires.isEmpty()&&effects.ticksRemaining==4,"Ground expires at five seconds without cancelling an acquired burn");
            for(int n=0;n<202;n++)step(.02f);
            require((float)get(player,"hp")==4&&effects.ticksRemaining==0,"Burn keeps dealing exactly two hearts after leaving the fire");
            // The blue attack belongs to ghosts only in the last two stages.
            call(game,"resetWorld");set(game,"scene",Screen.PLAYING);player=get(game,"player");set(player,"x",192f);set(player,"y",136f);
            enemies=(Array<?>)get(game,"enemies");quietEnemies(enemies);
            Object ghost=enemies.first();set(ghost,"type",EnemyKind.GHOST.ordinal());set(ghost,"hp",2);set(ghost,"maxHp",2);
            set(ghost,"x",267f);set(ghost,"y",136f);set(ghost,"attackCd",0f);set(game,"stage",3);
            tick("updateEnemies",.02f);require((float)get(ghost,"windup")==0,"Ghost has no blue ranged attack before stage four");
            set(game,"stage",4);tick("updateEnemies",.02f);
            require(get(ghost,"attack")==CasterAttack.RIFT_VIAL&&(int)get(game,"riftsCast")==1&&(boolean)get(ghost,"portalUsed"),"Stage-four ghost reserves the blue vial");
            roomScreenshot("43-ghost-blue-warning",235,136);
            vials=(Array<?>)get(game,"vials");
            for(int n=0;n<90&&vials.isEmpty();n++)tick("updateEnemies",.02f);
            require(vials.size==1&&get(vials.first(),"effect")==PotionEffect.RIFT,"Ghost releases a physical blue vial");
            tick("updateVials",1.1f);
            finishRift();
            require((int)get(game,"summonedBats")==3,"Ghost blue vial teleports and summons three bats");
            set(ghost,"actionTime",0f);set(ghost,"attackCd",0f);set(ghost,"x",(float)get(player,"x")+20);set(ghost,"y",get(player,"y"));
            tick("updateEnemies",.02f);require(get(ghost,"attack")==CasterAttack.NONE,"Ghost returns to melee after using its one blue vial");
            set(game,"stage",5);call(game,"resetWorld");enemies=(Array<?>)get(game,"enemies");quietEnemies(enemies);
            player=get(game,"player");ghost=enemies.first();set(ghost,"type",EnemyKind.GHOST.ordinal());set(ghost,"hp",2);set(ghost,"maxHp",2);
            set(ghost,"x",get(player,"x"));set(ghost,"y",(float)get(player,"y")+20);set(ghost,"attackCd",0f);
            tick("updateEnemies",.02f);require(get(ghost,"attack")==CasterAttack.RIFT_VIAL,"Stage-five ghost also uses the blue vial");
        } finally {
            held.clear();key=-1;click=false;set(game,"saveFile",backup);call(game,"loadGame");set(game,"saveFile",previous);set(game,"scene",Screen.PAUSED);
        }
    }
    @SuppressWarnings({"rawtypes","unchecked"})
    private void addVial(PotionEffect effect) throws Exception {
        Object player=get(game,"player");float x=(float)get(player,"x"),y=(float)get(player,"y");
        Class<?> type=Class.forName("br.edu.motionbound.MotionboundGame$Vial");
        var constructor=type.getDeclaredConstructor(float.class,float.class,float.class,float.class,float.class,PotionEffect.class);
        constructor.setAccessible(true);Object vial=constructor.newInstance(x,y,x,y,.5f,effect);set(vial,"elapsed",.49f);
        ((Array)get(game,"vials")).add(vial);
    }
    private void verifyIntro() {
        require(scene()==Screen.LEVEL_INTRO,"New campaign starts with a level introduction");
        Object player=get(game,"player");Object enemy=((Array<?>)get(game,"enemies")).first();
        float x=(float)get(player,"x"),enemyX=(float)get(enemy,"x");
        key=Input.Keys.SPACE;held.add(Input.Keys.D);click=true;pointAt(x+40,(float)get(player,"y"));
        game.render();screenshot("27-phase-1-introduction");
        require(scene()==Screen.LEVEL_INTRO&&(float)get(player,"x")==x&&(float)get(enemy,"x")==enemyX
                &&(int)get(game,"attacks")==0&&(float)get(game,"elapsed")==0,"Countdown freezes movement, combat and active time");
        held.clear();click=false;key=-1;
        advanceIntro(1.3f);game.render();screenshot("28-countdown-3");
        advanceIntro(1f);game.render();screenshot("29-countdown-2");
        advanceIntro(1f);game.render();screenshot("30-countdown-1");
        require(scene()==Screen.LEVEL_INTRO&&(float)get(game,"elapsed")==0,"All countdown digits precede gameplay");
        advanceIntro(1f);require(scene()==Screen.PLAYING,"Countdown starts gameplay automatically");
    }
    private void verifyCampaignFlow() throws Exception {
        FileHandle previous=(FileHandle)get(game,"saveFile"),backup=output.child("before-campaign-check.json");
        ((SaveData)call(game,"captureSave")).write(backup);
        try {
            key=-1;click=false;held.clear();call(game,"newCampaign");
            for(int stage=1;stage<=CampaignLevel.COUNT;stage++) {
                require((int)get(game,"stage")==stage&&scene()==Screen.LEVEL_INTRO,"Sequential stage entry: "+stage);
                if(stage==2) {
                    advanceIntro(1.4f);
                    FileHandle introFile=output.child("stage-2-intro-check.json");
                    float remaining=(float)get(game,"introRemaining");
                    ((SaveData)call(game,"captureSave")).write(introFile);
                    set(game,"saveFile",introFile);call(game,"loadGame");
                    require(scene()==Screen.LEVEL_INTRO&&(float)get(game,"introRemaining")==remaining,"Loading resumes the saved countdown");
                    key=Input.Keys.ESCAPE;game.render();key=-1;
                    float frozen=(float)get(game,"introRemaining");game.render();
                    require(scene()==Screen.PAUSED&&(float)get(game,"introRemaining")==frozen,"Pause freezes countdown");
                    key=Input.Keys.ESCAPE;game.render();key=-1;
                    require(scene()==Screen.LEVEL_INTRO,"Resume returns to the countdown");
                }
                advanceIntro(CampaignLevel.INTRO_SECONDS);game.render();
                Object player=get(game,"player");Array<?> enemies=(Array<?>)get(game,"enemies");
                DungeonMap map=(DungeonMap)get(game,"dungeon");
                require(enemies.size==map.level.enemyCount()&&(float)get(player,"hp")==6&&(int)get(player,"heals")==3,
                        "Stage encounters and replenishment: "+stage);
                levelOverviewScreenshot("stage-"+stage+"-map");
                screenshotAfterRender("stage-"+stage+"-hud");
                if(stage==5) {
                    key=Input.Keys.ESCAPE;game.render();key=-1;game.render();screenshot("stage-5-pause-summary");
                    key=Input.Keys.ESCAPE;game.render();key=-1;
                    require(scene()==Screen.PLAYING,"Last-stage summary supports all 22 guardians");
                }
                if(stage==3) {
                    set(player,"hp",0);step(.02f);game.render();
                    require(scene()==Screen.DEFEAT,"Defeat ends the current attempt");
                    key=Input.Keys.ENTER;game.render();key=-1;
                    require((int)get(game,"stage")==3&&scene()==Screen.LEVEL_INTRO,"Retry preserves current stage");
                    advanceIntro(CampaignLevel.INTRO_SECONDS);game.render();
                    player=get(game,"player");enemies=(Array<?>)get(game,"enemies");
                }
                set(player,"x",map.exitX());set(player,"y",map.exitY());step(.02f);
                require(scene()==Screen.PLAYING,"Exit stays locked while any guardian lives");
                for(Object enemy:enemies)set(enemy,"hp",0);call(game,"resolveDeaths");step(.02f);
                require(scene()==(stage<CampaignLevel.COUNT?Screen.LEVEL_COMPLETE:Screen.VICTORY),"Stage/final completion: "+stage);
                FileHandle checkpoint=output.child("stage-"+stage+"-complete-check.json");
                ((SaveData)call(game,"captureSave")).write(checkpoint);
                set(game,"saveFile",checkpoint);call(game,"resetWorld");call(game,"loadGame");
                require((int)get(game,"stage")==stage&&(boolean)get(game,"stageComplete"),"Completed checkpoint restores this stage");
                game.render();screenshot("stage-"+stage+"-completed");
                if(stage<CampaignLevel.COUNT) {
                    key=Input.Keys.ENTER;game.render();key=-1;
                    require(scene()==Screen.LEVEL_INTRO,"Continue button starts the next countdown");
                }
            }
            call(game,"nextStage");require((int)get(game,"stage")==5,"Final stage cannot advance beyond campaign");
            key=Input.Keys.ENTER;game.render();key=-1;
            require((int)get(game,"stage")==1&&scene()==Screen.LEVEL_INTRO,"Final victory offers a fresh campaign");
        } finally {
            held.clear();key=-1;click=false;
            set(game,"saveFile",backup);call(game,"loadGame");set(game,"saveFile",previous);set(game,"scene",Screen.PAUSED);
        }
    }
    private void screenshotAfterRender(String name) {game.render();screenshot(name);}
    private void levelOverviewScreenshot(String name) {
        OrthographicCamera camera=(OrthographicCamera)get(game,"worldCamera");
        float x=camera.position.x,y=camera.position.y,zoom=camera.zoom;
        try {
            camera.position.set(DungeonMap.WIDTH*8,DungeonMap.HEIGHT*8,0);camera.zoom=2.7f;camera.update();
            ((Viewport)get(game,"viewport")).apply();
            Gdx.gl.glClearColor(.07f,.06f,.09f,1);Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
            for(String method:new String[]{"drawMap","drawGroundAreas","drawGroundShadows","drawEntities","drawWorldEffects","drawAmbientLighting"})call(game,method);
            screenshot(name);
        } finally {camera.position.set(x,y,0);camera.zoom=zoom;camera.update();}
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
            ((Viewport)get(game,"viewport")).apply();
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
        Vector2 point=((Viewport)get(game,"viewport")).project(new Vector2(x,y));
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
            ((Viewport)get(game,"viewport")).apply();
            for(String method:new String[]{"drawMap","drawGroundAreas","drawGroundShadows","drawEntities","drawWorldEffects","drawAmbientLighting"})call(game,method);
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
    @Override public void pause() { game.pause(); }
    @Override public void dispose() { Gdx.input = input; Gdx.app = application; game.dispose(); }
}
