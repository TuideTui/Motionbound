package br.edu.motionbound.ui;

import br.edu.motionbound.game.DungeonMap;
import br.edu.motionbound.game.GameAssets;
import br.edu.motionbound.input.GameMode;
import br.edu.motionbound.input.EdgeController;
import br.edu.motionbound.game.MeleeSwing;
import br.edu.motionbound.game.CampaignLevel;
import br.edu.motionbound.game.StatusEffects;
import br.edu.motionbound.game.PotionEffect;
import br.edu.motionbound.game.EnemyKind;
import br.edu.motionbound.ui.UiIcons.Icon;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.viewport.ExtendViewport;

/** Pixel dungeon interface: carved stone, bronze fittings and parchment field notes. */
public final class GameUi implements Disposable {
    public enum Screen { MODE_SELECT, REHAB_SETUP, CONTROLLER, MENU, LEVEL_INTRO, PLAYING, PAUSED, LEVEL_COMPLETE, DEFEAT, VICTORY }
    public enum Action { NONE, RESUME, NEW_GAME, LOAD, SAVE_EXIT, FULLSCREEN, PAUSE,
        CONVENTIONAL, REHABILITATION, MODE_SELECT, START_SIMULATION, NEXT_STAGE, RETRY_STAGE,
        START_DEVICE, LOAD_DEVICE, CONFIG_DEVICE, CALIBRATE, SENSITIVITY, SWAP_AXES, INVERT_X, INVERT_Y, DEVICE_BACK }
    private enum Page { SUMMARY, SETTINGS, TUTORIAL, BESTIARY }
    public static final class View {
        public float hp,maxHp;
        public EdgeController controller;
        public boolean physicalControls;
        public float sprintProgress;
        public boolean sprinting,sprintNeedsRelease;
        public int heals, defeated, total;
        public final StatusEffects effects=new StatusEffects();
        public boolean teleportPending;
        public float stamina, mana, elapsed, x, y, attackCd, magicCd, healCd;
        public boolean hasSave;
        public GameMode mode=GameMode.CONVENTIONAL,savedMode=GameMode.CONVENTIONAL;
        public int attacks,hits,heartsCollected;
        public float pressure,pressureAccuracy,pressureSeconds;
        public String message = "";
        public DungeonMap dungeon;
        public CampaignLevel level=CampaignLevel.of(1);
        public float introRemaining;
        public float irisOpen,irisClose,heroScreenX,heroScreenY;
    }

    private static final Color INK = Color.valueOf("17141e");
    private static final Color PANEL = Color.valueOf("2b2530");
    private static final Color CARD = Color.valueOf("3c3036");
    private static final Color LINE = Color.valueOf("655261");
    private static final Color WHITE = Color.valueOf("f5dfaf");
    private static final Color MUTED = Color.valueOf("b9a18c");
    private static final Color GOLD = Color.valueOf("d7a05b");
    private static final Color MINT = Color.valueOf("a8c87b");
    private static final Color RED = Color.valueOf("ed7a89");
    private static final Color BLUE = Color.valueOf("7fc5ed");
    private static final Color PAPER = Color.valueOf("d7bb87");
    private static final Color PAPER_TEXT = Color.valueOf("48313e");
    private static final Color PAPER_MUTED = Color.valueOf("785d4b");
    private final ExtendViewport viewport = new ExtendViewport(1280, 720);
    private final SpriteBatch batch = new SpriteBatch();
    private final Texture white;
    private final ScreenStatusEffects screenEffects;
    private final GameAssets art;
    private final UiIcons icons = new UiIcons();
    private final InputPrompts prompts=new InputPrompts();
    private final Vector2 pointer = new Vector2();
    private UiFont small, body, heading, display;
    private float density;
    private float width, height;
    private int focus, buttonIndex;
    private Page page = Page.SUMMARY;
    private Screen previous;
    private Action action;
    private boolean statusVisible;
    private GameMode tutorialMode=GameMode.CONVENTIONAL;
    private int catalogueTab,detailEntry=-1;
    private final HudFeedback feedback=new HudFeedback();
    private final Preferences visualPreferences=Gdx.app.getPreferences("motionbound-visuals");
    private boolean reducedEffects=visualPreferences.getBoolean("reducedEffects",false);
    private float menuTime,screenAge,pageAge;
    private int controllerButtons;
    public void controllerButtons(int pressed){controllerButtons=pressed;}

    public boolean reducedEffects(){return reducedEffects;}
    public void modeChanged(GameMode mode) {
        if(!visualPreferences.contains("reducedEffects"))reducedEffects=mode==GameMode.REHABILITATION;
    }
    public void resetPresentation(){feedback.reset();}
    public void automaticHeal(int remaining){feedback.automaticHeal(remaining);}
    public void collectHeart(float screenX,float screenY,float restoredHp) {
        Vector2 point=viewport.unproject(new Vector2(screenX,Gdx.graphics.getHeight()-screenY));
        feedback.collect(point.x/width,point.y/height,restoredHp);
    }
    private void toggleReducedEffects() {
        reducedEffects=!reducedEffects;
        visualPreferences.putBoolean("reducedEffects",reducedEffects).flush();
    }

    public GameUi(GameAssets art) {
        this.art = art;
        Pixmap pixel = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixel.setColor(Color.WHITE); pixel.fill();
        white = new Texture(pixel); pixel.dispose();
        screenEffects=new ScreenStatusEffects(white,art.glow);
    }

    public void resize(int w, int h) {
        if (w <= 0 || h <= 0) return;
        viewport.update(w, h, true);
        width = viewport.getWorldWidth(); height = viewport.getWorldHeight();
        float next = Gdx.graphics.getBackBufferWidth() / width;
        if (body == null || Math.abs(next - density) > .015f) {
            disposeFonts(); density = next;
            small = new UiFont(22, false, density);
            body = new UiFont(28, true, density);
            heading = new UiFont(52, true, density);
            display = new UiFont(100, true, density);
        }
    }

    /** ESC returns from a subpage first, then resumes the expedition. */
    public boolean backToSummary() {
        if(page==Page.BESTIARY&&detailEntry>=0){focus=8+detailEntry;detailEntry=-1;return true;}
        if (page == Page.SUMMARY) return false;
        page = Page.SUMMARY; focus = 1;pageAge=0;
        return true;
    }

    /** Gameplay never consumes clicks intended for a HUD panel or the pause button. */
    public boolean blocksGameplayPointer() {
        pointer.set(Gdx.input.getX(),Gdx.input.getY());viewport.unproject(pointer);
        float top=height-28,cx=width/2;
        return contains(28,top-154,354,154)||contains(width-248,top-214,220,214)
                ||contains(cx-242,height-103,484,75)||contains(cx-308,28,616,80)
                ||contains(width-181,28,153,44)||contains(20,24,180,60)
                ||statusVisible&&contains(28,height-246,354,54);
    }
    private boolean contains(float x,float y,float w,float h) {
        return pointer.x>=x&&pointer.x<=x+w&&pointer.y>=y&&pointer.y<=y+h;
    }
    private boolean canContinue(View view) {return view.hasSave&&view.savedMode==view.mode;}

    private void modeSelect(View view) {
        float x=(width-1040)/2,y=(height-550)/2;
        panel(x,y,1040,550);
        center(display,"MOTIONBOUND",width/2,y+506,WHITE);
        center(heading,"Escolha sua jornada",width/2,y+414,GOLD);
        center(small,"Cinco fases. Duas formas de viver a aventura.",width/2,y+367,MUTED);
        parchment(x+28,y+144,476,194);
        parchment(x+536,y+144,476,194);
        item(103,x+45,y+278,42);
        label(body,"MODO CONVENCIONAL",x+101,y+317,PAPER_TEXT);
        label(small,"Mouse e teclado. Mira livre em 360 graus.",x+48,y+267,PAPER_MUTED);
        label(small,"Espada, magia e cura com novas animações.",x+48,y+243,PAPER_MUTED);
        if(button(x+49,y+165,434,52,"Jogar com mouse e teclado",true,false))action=Action.CONVENTIONAL;
        icons.draw(batch,Icon.COG,x+553,y+278,42);
        label(body,"MODO REABILITAÇÃO",x+609,y+317,PAPER_TEXT);
        label(small,"Inclinação e gestos. Mira assistida.",x+556,y+267,PAPER_MUTED);
        label(small,view.controller==null?"Controle USB / simulação disponível.":view.controller.status(),x+556,y+243,PAPER_MUTED);
        if(button(x+557,y+165,434,52,"Preparar reabilitação",false,false))action=Action.REHABILITATION;
        if(button(width/2-205,y+57,410,44,"Sair do jogo",false,true))Gdx.app.exit();
        center(small,view.controller!=null&&view.controller.connected()?"KEYPAD: 2 anterior / 3 confirmar / 4 próximo":"SETAS: navegar   ENTER: escolher   F11: tela cheia",width/2,y+30,MUTED);
        if(!view.message.isEmpty())center(small,view.message,width/2,y-18,RED);
    }

    private void rehabSetup(View view) {
        if(view.controller!=null&&view.controller.detected()){deviceSetup(view);return;}
        float x=(width-1040)/2,y=(height-550)/2;
        panel(x,y,1040,550);
        center(small,"MODO REABILITAÇÃO / PREPARAÇÃO",width/2,y+510,MINT);
        center(heading,"A próxima jornada começa aqui",width/2,y+468,WHITE);
        parchment(x+30,y+207,980,197);
        icons.draw(batch,Icon.COG,x+49,y+320,56);
        label(body,"BlackBoard Edge + Keypad + APDS",x+125,y+375,PAPER_TEXT);
        label(small,"Inclinação para mover. Aperto para atacar. Mira assistida.",x+125,y+339,PAPER_MUTED);
        label(small,view.controller==null?"Conecte sua Edge por USB para jogar com o controle.":view.controller.status(),x+52,y+292,PAPER_TEXT);
        label(small,"Você pode testar agora uma SIMULAÇÃO pelo teclado.",x+52,y+262,PAPER_MUTED);
        label(small,"Dados artificiais: os resultados não representam uma avaliação clínica.",x+52,y+237,PAPER_MUTED);
        center(small,"SIMULAÇÃO: I J K L = inclinar / F = apertar / SHIFT + F = pressão alta",width/2,y+184,MUTED);
        float by=y+116;
        if(canContinue(view)) {
            if(button(x+40,by,464,52,"Continuar simulação salva",true,false))action=Action.LOAD;
            if(button(x+536,by,464,52,"Nova simulação",false,false))action=Action.START_SIMULATION;
        } else if(button(width/2-232,by,464,52,"Experimentar simulação",true,false))action=Action.START_SIMULATION;
        if(button(width/2-232,y+48,464,46,"Voltar à escolha de modo",false,false))action=Action.MODE_SELECT;
    }

    private void deviceSetup(View v) {
        EdgeController c=v.controller;float x=(width-1040)/2,y=(height-550)/2;
        panel(x,y,1040,550);
        center(small,"MODO REABILITAÇÃO / CONTROLE USB",width/2,y+510,MINT);
        center(heading,"Prepare sua manopla",width/2,y+459,WHITE);
        parchment(x+30,y+216,980,198);
        label(body,c.status(),x+54,y+389,PAPER_TEXT);
        wrapped(c.calibrationMessage(),x+54,y+348,880,24,PAPER_MUTED);
        label(small,"Incline: mover / forte por 4s: correr / gesto lateral: espada / cima: magia",x+54,y+301,PAPER_TEXT);
        label(small,"Keypad: 1 voltar / 2 anterior / 3 confirmar / 4 próximo",x+54,y+270,PAPER_MUTED);
        if(c.calibrating()){rect(x+54,y+236,880,8,PAPER_MUTED);rect(x+54,y+236,880*c.progress(),8,MINT);}
        if(button(x+40,y+150,464,46,c.ready()?"Iniciar com a manopla":"Calibrar a posição da mão",true,false))action=c.ready()?Action.START_DEVICE:Action.CALIBRATE;
        if(button(x+536,y+150,464,46,"Ajustar controle",false,false))action=Action.CONFIG_DEVICE;
        if(canContinue(v)&&button(x+40,y+88,464,46,"Continuar com a manopla",false,false))action=Action.LOAD_DEVICE;
        if(button(x+536,y+88,464,46,"Voltar à escolha de modo",false,false))action=Action.MODE_SELECT;
        center(small,"Cura automática com 1 coração ou menos / 3 cargas por fase",width/2,y+44,MINT);
    }

    private void controllerSettings(View v) {
        EdgeController c=v.controller;float x=(width-1040)/2,y=(height-550)/2;
        panel(x,y,1040,550);center(heading,"Ajustes da manopla",width/2,y+505,WHITE);
        center(small,c.status(),width/2,y+452,c.connected()?MINT:RED);
        center(small,c.calibrationMessage(),width/2,y+418,MUTED);
        if(c.calibrating()){rect(x+70,y+382,900,8,LINE);rect(x+70,y+382,900*c.progress(),8,MINT);}
        else center(small,String.format(java.util.Locale.ROOT,"Inclinação: X %+.2f / Y %+.2f",c.tiltX(),c.tiltY()),width/2,y+386,GOLD);
        center(small,"Corrida: 4s acima de 60% da faixa. Alivie abaixo de 40% para andar.",width/2,y+365,MUTED);
        String[] labels={"Calibrar posição neutra","Sensibilidade: "+c.sensitivityLabel(),"Eixos: "+(c.swapAxes?"trocados":"normais"),
                "Esquerda/direita: "+(c.invertX?"invertido":"normal"),"Frente/trás: "+(c.invertY?"invertido":"normal"),"Voltar"};
        Action[] actions={Action.CALIBRATE,Action.SENSITIVITY,Action.SWAP_AXES,Action.INVERT_X,Action.INVERT_Y,Action.DEVICE_BACK};
        for(int i=0;i<labels.length;i++)if(button(x+90,y+309-i*50,860,40,labels[i],i==0,false))action=actions[i];
        center(small,"2 anterior / 3 alterar ou confirmar / 4 próximo / 1 voltar",width/2,y+29,MUTED);
    }

    public Action draw(Screen screen, View view) {
        action = Action.NONE;
        float delta=MathUtils.clamp(Gdx.graphics.getDeltaTime(),0,1f/30f);
        menuTime+=delta;screenAge+=delta;pageAge+=delta;
        if (screen != previous) { focus = 0; page = Page.SUMMARY; detailEntry=-1;previous = screen;screenAge=0; }
        if(screen==Screen.PLAYING)feedback.update(view,delta);
        if(screen==Screen.PAUSED&&(page==Page.TUTORIAL||page==Page.BESTIARY)) {
            if(Gdx.input.isKeyJustPressed(Input.Keys.LEFT)||Gdx.input.isKeyJustPressed(Input.Keys.RIGHT)) {
                if(page==Page.TUTORIAL)tutorialMode=tutorialMode==GameMode.CONVENTIONAL?GameMode.REHABILITATION:GameMode.CONVENTIONAL;
                else {catalogueTab=1-catalogueTab;detailEntry=-1;}
                pageAge=0;
                focus=6+(page==Page.TUTORIAL?(tutorialMode==GameMode.CONVENTIONAL?0:1):catalogueTab);
            }
        }
        int count = screen == Screen.PAUSED ? pauseButtonCount() : screen == Screen.MENU ? (canContinue(view) ? 4 : 3)
                : screen == Screen.MODE_SELECT ? 3 : screen==Screen.CONTROLLER?6
                : screen == Screen.REHAB_SETUP ? (view.controller!=null&&view.controller.detected()?(canContinue(view)?4:3):(canContinue(view)?3:2))
                : screen == Screen.PLAYING || screen==Screen.LEVEL_INTRO ? 1 : 2;
        focus=MathUtils.clamp(focus,0,count-1);
        if(Gdx.input.isKeyJustPressed(Input.Keys.TAB)) {
            boolean backwards=Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT)||Gdx.input.isKeyPressed(Input.Keys.SHIFT_RIGHT);
            focus=(focus+(backwards?count-1:1))%count;
        }
        if ((controllerButtons&8)!=0 || Gdx.input.isKeyJustPressed(Input.Keys.DOWN) || Gdx.input.isKeyJustPressed(Input.Keys.S)) focus = (focus + 1) % count;
        if ((controllerButtons&2)!=0 || Gdx.input.isKeyJustPressed(Input.Keys.UP) || Gdx.input.isKeyJustPressed(Input.Keys.W)) focus = (focus + count - 1) % count;
        pointer.set(Gdx.input.getX(), Gdx.input.getY()); viewport.unproject(pointer);
        buttonIndex = 0;
        statusVisible=false;
        viewport.apply(); batch.setProjectionMatrix(viewport.getCamera().combined);
        Gdx.gl.glEnable(GL20.GL_BLEND);
        batch.begin();
        screenEffects.setReduced(reducedEffects);
        if(screen==Screen.PLAYING)screenEffects.update(view.effects,delta);
        else if(screen!=Screen.PAUSED)screenEffects.clear();
        if(screen==Screen.PLAYING||screen==Screen.PAUSED)screenEffects.draw(batch,width,height,density);
        boolean closing=(screen==Screen.LEVEL_COMPLETE||screen==Screen.VICTORY)&&view.irisClose>0&&!reducedEffects;
        if(closing){hud(view);drawIris(1-view.irisClose/.5f,view);action=Action.NONE;}
        else if (screen == Screen.PLAYING) {hud(view);drawHeartFlights();}
        else if(screen==Screen.LEVEL_INTRO) {drawIris(view.irisOpen/.6f,view);levelIntro(view);}
        else {
            if((screen==Screen.LEVEL_COMPLETE||screen==Screen.VICTORY)&&!reducedEffects)rect(0,0,width,height,INK);
            rect(0, 0, width, height, INK, .86f);
            for (int i = 0; i < 5; i++) {
                rune(36, height / 2 - 100 + i * 48, GOLD, .4f);
                rune(width - 44, height / 2 - 100 + i * 48, GOLD, .4f);
            }
            if(screen==Screen.MODE_SELECT)modeSelect(view);
            else if(screen==Screen.CONTROLLER)controllerSettings(view);
            else if(screen==Screen.REHAB_SETUP)rehabSetup(view);
            else if (screen == Screen.PAUSED) pause(view);
            else if (screen == Screen.MENU) welcome(view);
            else result(screen, view);
        }
        batch.end();
        return action;
    }
    private int pauseButtonCount() {
        if(page==Page.TUTORIAL)return 8;
        if(page==Page.BESTIARY)return detailEntry>=0?9:8+catalogueEntries().length;
        return page==Page.SETTINGS?8:6;
    }
    private FieldGuide.Entry[] catalogueEntries(){return catalogueTab==0?FieldGuide.ENEMIES:FieldGuide.POTIONS;}
    private void openPage(Page next,View view) {
        if(page==next)return;
        page=next;detailEntry=-1;pageAge=0;
        if(next==Page.TUTORIAL)tutorialMode=view.mode;
    }

    private void campaignTrail(CampaignLevel level,float y) {
        float left=width/2-142;
        for(int i=1;i<=CampaignLevel.COUNT;i++) {
            float x=left+(i-1)*64;
            if(i<CampaignLevel.COUNT)rect(x+22,y+10,42,2,i<level.number()?MINT:LINE);
            socket(x,y,28);
            if(i==level.number())rect(x+3,y+3,22,22,GOLD);
            center(body,""+i,x+14,y+24,i<level.number()?MINT:i==level.number()?INK:MUTED);
        }
    }
    private void levelIntro(View view) {
        rect(0,0,width,height,INK,.65f);
        float x=(width-1000)/2,y=(height-440)/2;
        panel(x,y,1000,440);
        center(small,"NOVA ETAPA DA JORNADA",width/2,y+403,GOLD);
        center(heading,"Fase "+view.level.number()+" - Nível "+view.level.difficulty().toLowerCase(java.util.Locale.ROOT),width/2,y+356,WHITE);
        center(body,view.level.title(),width/2,y+296,GOLD);
        ornament(x+170,y+257,660,LINE);
        if(view.introRemaining>3)center(heading,"PREPARE-SE",width/2,y+217,MUTED);
        else {
            int digit=MathUtils.clamp((int)Math.ceil(view.introRemaining),1,3);
            batch.setColor(Color.WHITE);batch.draw(art.countdownDigit(digit),width/2-64,y+122,128,128);
        }
        center(small,"Derrote "+view.total+" guardiões e encontre o baú marcado no mapa.",width/2,y+103,MUTED);
        campaignTrail(view.level,y+45);
        if(view.mode==GameMode.REHABILITATION)
            center(small,view.physicalControls?"EDGE / incline para mover / gesto lateral: espada / para cima: magia":"SIMULAÇÃO / dados artificiais / IJKL: inclinar / F: apertar",width/2,y-22,BLUE);
        if(button(width-181,28,153,44,"ESC  Menu",false,false))action=Action.PAUSE;
    }

    /** Pixel tiles form the iris; the centre follows the hero's projected position. */
    private void drawIris(float coverage,View view) {
        if(reducedEffects||coverage<=0)return;
        float cx=MathUtils.clamp(view.heroScreenX,0,1)*width,cy=MathUtils.clamp(view.heroScreenY,0,1)*height;
        float reach=Math.max(Math.max(Vector2.dst(cx,cy,0,0),Vector2.dst(cx,cy,width,0)),
                Math.max(Vector2.dst(cx,cy,0,height),Vector2.dst(cx,cy,width,height)))+48;
        float radius=reach*(1-MathUtils.clamp(coverage,0,1));
        for(int y=0;y<height;y+=24)for(int x=0;x<width;x+=24)
            if(Vector2.dst2(x+12,y+12,cx,cy)>radius*radius)rect(x,y,24,24,INK);
    }
    private void drawHeartFlights() {
        for(HudFeedback.HeartFlight flight:feedback.flights) {
            float t=flight.progress(),ease=t*t*(3-2*t),sx=flight.startX*width,sy=flight.startY*height;
            float tx=144+30*flight.slot,ty=height-91;
            float jump=MathUtils.sin(Math.min(1,flight.age/.14f)*MathUtils.PI)*24;
            float x=MathUtils.lerp(sx,tx,ease)+MathUtils.sin(t*MathUtils.PI)*48;
            float y=MathUtils.lerp(sy,ty,ease)+MathUtils.sin(t*MathUtils.PI)*70+jump;
            if(reducedEffects){x=tx;y=ty;}
            float size=MathUtils.lerp(36,24,t);
            for(int n=1;n<=3&&!reducedEffects;n++) {
                float tail=Math.max(0,t-n*.035f),q=tail*tail*(3-2*tail);
                float px=MathUtils.lerp(sx,tx,q)+MathUtils.sin(tail*MathUtils.PI)*48;
                float py=MathUtils.lerp(sy,ty,q)+MathUtils.sin(tail*MathUtils.PI)*70;
                rect(px-2,py-2,4,4,n==1?WHITE:RED,(1-n*.22f)*.65f);
            }
            batch.setColor(1,1,1,reducedEffects?1-t:1);
            batch.draw(art.heartTexture,Math.round(x-size/2),Math.round(y-size/2),size,size);
        }
        batch.setColor(Color.WHITE);
    }

    private void hud(View v) {
        float top = height - 28;
        float extra=v.physicalControls?26:0;
        panel(28, top - 154-extra, 354, 154+extra);
        item(101, 45, top - 35, 26);
        label(small, v.mode==GameMode.CONVENTIONAL?"EXPLORADOR / MOUSE":v.physicalControls?"REABILITAÇÃO / EDGE":"REABILITAÇÃO / DEMO", 80, top - 13, GOLD);
        rect(48, top - 41, 310, 2, LINE);
        label(small, "VIDA", 49, top - 50, RED);
        for (int i = 0; i < v.maxHp; i++) {
            float pulse=reducedEffects?0:MathUtils.sin(feedback.heartPulse[i]/.45f*MathUtils.PI)*7;
            icons.fill(batch,Icon.HEART,130+i*30-pulse/2,top-77-pulse/2,28+pulse,v.hp-i);
            if(feedback.heartLost[i]>0&&!reducedEffects) {
                float p=1-feedback.heartLost[i]/.35f;
                batch.setColor(1,.55f,.55f,1-p);batch.draw(art.heartTexture,134+i*30,top-72-p*15,20,20);
                rect(144+i*30,top-68-p*15,2,9,INK,1-p);
            }
        }
        if(feedback.healPulse>0)rect(130,top-81,180,2,MINT,feedback.healPulse*2);
        if(feedback.hurtPulse>0)rect(130,top-81,180,2,RED,feedback.hurtPulse*2);
        right(small, health(v.hp) + "/" + health(v.maxHp), 364, top - 51, WHITE);
        icons.draw(batch, Icon.BOLT, 46, top - 111, 28);
        label(small, "VIGOR", 80, top - 89, GOLD);
        for (int i = 0; i < 8; i++) {
            float charge = MathUtils.clamp(v.stamina / 12.5f - i, 0, 1);
            rect(134 + i * 24, top - 109, 18, 19, INK);
            rect(136 + i * 24, top - 107, 14 * charge, 15, GOLD);
            if (charge > 0) rect(136 + i * 24, top - 95, 14 * charge, 3, WHITE);
            if(feedback.vigorPulse>0&&charge>0)rect(136+i*24,top-107,14*charge,15,WHITE,feedback.vigorPulse*1.6f);
        }
        right(small, "" + Math.round(v.stamina), 364, top - 89, GOLD);
        if(v.physicalControls)drawSprintCounter(v,top);
        icons.draw(batch, Icon.CRYSTAL, 46, top - 145-extra, 28);
        label(small, "MANA", 80, top - 124-extra, BLUE);
        for (int i = 0; i < 4; i++) {
            icons.fill(batch,Icon.CRYSTAL,139+i*44,top-145-extra,28,v.mana/25-i);
            icons.spent(batch,Icon.CRYSTAL,139+i*44,top-145-extra,28,v.mana/25-i,feedback.lagMana/25-i,.5f);
            if(feedback.manaPulse>0)rect(147+i*44,top-145-extra+feedback.manaPulse*20,3,3,BLUE,feedback.manaPulse*1.8f);
        }
        right(small, "" + Math.round(v.mana), 364, top - 124-extra, BLUE);
        int effectIndex=0;
        if(v.effects.damageEffect!=null)effectBadge(v.effects.damageEffect,v.effects.damageRemaining(),effectIndex++,extra);
        if(v.effects.slowRemaining>0)effectBadge(PotionEffect.SLOW,v.effects.slowRemaining,effectIndex++,extra);
        if(v.effects.riftRemaining>0)effectBadge(PotionEffect.RIFT,v.effects.riftRemaining,effectIndex++,extra);
        statusVisible=effectIndex>0;
        if(v.mode==GameMode.REHABILITATION)drawAutomaticHealNotice(extra);

        float mx = width - 248;
        panel(mx, top - 214, 220, 214);
        label(small, "MAPA / FASE "+v.level.number(), mx + 19, top - 13, GOLD);
        float scale = 2.9f, x0 = mx + 30, y0 = top - 140;
        rect(x0 - 5, y0 - 5, 170, 112, PAPER_MUTED);
        rect(x0 - 2, y0 - 2, 164, 106, PAPER);
        for (int x = 0; x < DungeonMap.WIDTH; x++) for (int y = 0; y < DungeonMap.HEIGHT; y++) {
            if (v.dungeon.isFloor(x, y)) rect(x0 + x * scale, y0 + y * scale, scale, scale, PAPER_MUTED);
        }
        rect(x0 + v.dungeon.exitTileX * scale - 2, y0 + v.dungeon.exitTileY * scale - 2, 6, 6, v.defeated == v.total ? MINT : RED);
        rect(x0 + v.x / 16 * scale - 2, y0 + v.y / 16 * scale - 2, 5, 5, INK);
        label(small, "VOCÊ", mx + 20, top - 154, WHITE);
        right(small, "SAÍDA", mx + 200, top - 154, GOLD);
        rect(mx + 16, top - 178, 188, 1, LINE);
        label(small, "ABATES", mx + 16, top - 180, MUTED);
        right(small, v.defeated + " / " + v.total, mx + 204, top - 180, MINT);

        float cx = width / 2;
        parchment(cx - 242, height - 103, 484, 75);
        icons.draw(batch, Icon.SCROLL, cx - 230, height - 86, 42);
        label(small, "FASE "+v.level.number()+" / "+CampaignLevel.COUNT+" - "+v.level.title(), cx - 178, height - 42, PAPER_MUTED);
        label(body, v.defeated == v.total ? "Encontre o baú da saída" : "Derrote os "+v.total+" guardiões", cx - 178, height - 65, PAPER_TEXT);

        float start = cx - 308;
        boolean demo=v.mode==GameMode.REHABILITATION;
        skill(start, 28, v.physicalControls?"GESTO LATERAL":demo?"F (DEMO)":"MOUSE ESQ.", "Espada", v.attackCd, demo?br.edu.motionbound.game.GuidedStrike.COOLDOWN:MeleeSwing.COOLDOWN, GOLD, demo?"Avanço e recuo":"Mira em 360 graus");
        skill(start + 208, 28, v.physicalControls?"GESTO CIMA":demo?"EM BREVE":"MOUSE DIR.", "Magia", v.magicCd, .75f, BLUE, demo&&!v.physicalControls?"Etapa seguinte":"25 de mana");
        skill(start + 416, 28, demo?"AUTOMÁTICA":"ESPAÇO", "Cura", v.healCd, 1.7f, MINT, demo?v.heals+" restantes":v.heals + " poções");
        label(small, v.physicalControls?"EDGE / FASE "+v.level.number():demo?"DEMO / FASE "+v.level.number():"FASE "+v.level.number()+" / "+CampaignLevel.COUNT, 28, 66, demo?BLUE:GOLD);
        label(body, time(v.elapsed), 28, 46, WHITE);
        if(demo&&!v.physicalControls) {
            panel(width/2-308,121,616,51);
            label(small,"IJKL: inclinar   F: apertar   SHIFT+F: pressão alta",width/2-290,159,MUTED);
            rect(width/2-290,132,150,6,INK);
            rect(width/2-290,132,150*v.pressure,6,v.pressure>.8f?RED:MINT);
            label(small,"Pressão na faixa: "+Math.round(v.pressureAccuracy)+"%",width/2-120,146,MINT);
        }
        if (button(width - 181, 28, 153, 44, v.physicalControls?"1  Menu":"ESC  Menu", false, false)) action = Action.PAUSE;
    }
    private void drawSprintCounter(View v,float top) {
        Color color=v.sprinting?MINT:v.sprintNeedsRelease?MUTED:GOLD;
        label(small,"CORRIDA",49,top-118,color);
        for(int i=0;i<4;i++) {
            float filled=MathUtils.clamp(v.sprintProgress*4-i,0,1),x=134+i*46;
            rect(x,top-133,42,12,INK);
            rect(x+2,top-131,Math.round(38*filled),8,color);
            if(filled>0)rect(x+2,top-125,Math.round(38*filled),2,WHITE,.6f);
        }
        String status=v.sprinting?"ATIVA":v.sprintNeedsRelease?"RELAXE":Math.max(1,(int)Math.ceil(4*(1-v.sprintProgress)))+"s";
        right(small,status,364,top-118,color);
        if(v.sprinting&&!reducedEffects)rect(46,top-112,29,2,MINT,.35f+.25f*MathUtils.sin(menuTime*6));
    }
    private void drawAutomaticHealNotice(float extra) {
        if(feedback.autoHealRemaining<=0)return;
        float age=HudFeedback.AUTO_HEAL_SECONDS-feedback.autoHealRemaining;
        float entrance=MathUtils.clamp(age/.22f,0,1),exit=MathUtils.clamp(feedback.autoHealRemaining/.3f,0,1);
        float x=28-(reducedEffects?0:22*(1-entrance*entrance*(3-2*entrance)*exit));
        float y=height-336-extra;
        panel(x,y,354,76);
        rect(x+4,y+4,3,68,MINT);
        socket(x+15,y+17,40);
        float lift=reducedEffects?0:MathUtils.sin(Math.min(age/.45f,1)*MathUtils.PI)*6;
        item(115,x+19,y+21+lift,32);
        label(small,"CURA AUTOMÁTICA",x+69,y+58,MINT);
        String charges=feedback.autoHealCharges==1?"Resta 1 cura":"Restam "+feedback.autoHealCharges+" curas";
        label(body,charges,x+69,y+36,WHITE);
        rect(x+69,y+12,264,3,LINE);
        rect(x+69,y+12,264*feedback.autoHealRemaining/HudFeedback.AUTO_HEAL_SECONDS,3,MINT);
        if(!reducedEffects)for(int i=0;i<3;i++) {
            float p=MathUtils.clamp((age-i*.1f)/.7f,0,1);
            if(p>0&&p<1)rect(x+21+i*12,y+37+p*25,3,3,MINT,1-p);
        }
    }
    private void effectBadge(PotionEffect effect,float remaining,int index,float extra) {
        float x=28+index*118,y=height-246-extra;
        panel(x,y,112,52);item(effect.tile,x+3,y+9,32);
        label(small,effect.label,x+37,y+42,effect.color);
        label(small,effect==PotionEffect.RIFT?"3 MORC.":Math.max(1,(int)Math.ceil(remaining))+"s",x+37,y+22,WHITE);
    }
    private static String health(float value) {return value==(int)value?""+(int)value:String.format(java.util.Locale.ROOT,"%.1f",value).replace('.',',');}

    private void skill(float x, float y, String key, String name, float cooldown, float max, Color color, String detail) {
        panel(x, y, 200, 80);
        socket(x + 12, y + 16, 48);
        if(key.equals("COLETA"))batch.draw(art.heartTexture,x+21,y+25,30,30);
        else item(name.equals("Espada") ? 103 : name.equals("Magia") ? 116 : 115, x + 12, y + 16, 48);
        if(key.equals("EM BREVE"))icons.draw(batch,Icon.LOCK,x+27,y+28,28);
        if(name.equals("Magia")&&!key.equals("EM BREVE")&&feedback.readyPulse>0) {
            float p=feedback.readyPulse/.5f;
            rect(x+10,y+65,52,2,GOLD,p);rect(x+10,y+14,52,2,GOLD,p);
            if(!reducedEffects){rune(x+65,y+50+(1-p)*12,GOLD,p);rune(x+4,y+24-(1-p)*8,BLUE,p);}
        }
        if (cooldown > 0) rect(x + 16, y + 20, 40, 40 * MathUtils.clamp(cooldown / max, 0, 1), INK, .75f);
        rect(x + 68, y + 7, 118 * (1 - MathUtils.clamp(cooldown / max, 0, 1)), 3, color);
        label(small, key, x + 72, y + 70, color);
        label(body, name, x + 72, y + 51, WHITE);
        label(small, detail, x + 72, y + 29, MUTED);
    }

    private void pause(View v) {
        float x = (width - 1060) / 2, y = (height - 552) / 2;
        panel(x, y, 1060, 552);
        label(small, "MOTIONBOUND", x + 32, y + 522, GOLD);
        icons.draw(batch, Icon.PAUSE, x + 308, y + 504, 24);
        label(small, "UM RESPIRO NA JORNADA", x + 340, y + 522, MINT);
        right(small, "FASE "+v.level.number()+" / "+CampaignLevel.COUNT+" - "+v.level.title(), x + 1028, y + 522, MUTED);
        rect(x + 294, y + 30, 4, 450, INK);
        rect(x + 298, y + 30, 2, 450, GOLD);
        socket(x + 116, y + 430, 64);
        batch.setColor(Color.WHITE); batch.draw(art.heroPortrait, x + 124, y + 434, 48, 56);
        center(body, "ACAMPAMENTO", x + 148, y + 422, GOLD);
        center(small, "A espada pode descansar.", x + 148, y + 396, MUTED);
        if (button(x + 28, y + 322, 240, 54, "Retomar aventura", true, false)) action = Action.RESUME;
        if (button(x + 28, y + 262, 240, 48, "Expedição", page == Page.SUMMARY, false)) openPage(Page.SUMMARY,v);
        if (button(x + 28, y + 208, 240, 48, "Configurações", page == Page.SETTINGS, false)) openPage(Page.SETTINGS,v);
        if (button(x + 28, y + 154, 240, 48, "Tutorial", page == Page.TUTORIAL, false)) openPage(Page.TUTORIAL,v);
        if (button(x + 28, y + 100, 240, 48, "Bestiário", page == Page.BESTIARY, false)) openPage(Page.BESTIARY,v);
        if (button(x + 28, y + 48, 240, 48, "Salvar e sair", false, true)) action = Action.SAVE_EXIT;
        float content = x + 330;
        parchment(content - 10, y + 55, 716, 435);
        if(pageAge<.22f&&!reducedEffects) {
            float fold=(1-pageAge/.22f)*18;
            rect(content+4,y+476-fold,688,fold,PAPER_MUTED,.35f);
        }
        if (page == Page.SUMMARY) summary(v, content, y);
        else if (page == Page.SETTINGS) settings(v,content, y);
        else if(page==Page.TUTORIAL)tutorial(v,content, y);
        else bestiary(content,y);
        center(small, v.physicalControls?"KEYPAD: 1 voltar / 2 anterior / 3 confirmar / 4 próximo / abas também são selecionáveis"
                :page==Page.TUTORIAL||page==Page.BESTIARY?"TAB / CIMA / BAIXO: navegar   ESQ. / DIR.: abas   ENTER: abrir   ESC: voltar"
                :"SETAS: navegar   ENTER: escolher   ESC: voltar   F11: tela cheia", width / 2, y - 20, MUTED);
        if (!v.message.isEmpty()) label(small, v.message, content, y + 36, RED);
    }

    private void summary(View v, float x, float y) {
        label(heading, "Diário da expedição", x + 14, y + 464, PAPER_TEXT);
        label(small, v.mode==GameMode.CONVENTIONAL?"Combate com mira livre / mouse e teclado.":v.physicalControls?"EDGE / inclinação, gestos e cura automática.":"SIMULAÇÃO DE SENSORES / dados artificiais.", x + 16, y + 410, PAPER_MUTED);
        journalStat(x + 12, y + 268, "GUARDIÕES", v.defeated + " / " + v.total, Icon.SKULL);
        journalStat(x + 244, y + 268, "TEMPO ATIVO", time(v.elapsed), Icon.CLOCK);
        if(v.mode==GameMode.REHABILITATION&&!v.physicalControls)
            journalStat(x+476,y+268,"NA FAIXA",v.pressureSeconds>0?Math.round(v.pressureAccuracy)+"%":"--",Icon.BOLT);
        else journalStat(x + 476, y + 268, "FRASCOS", "0" + v.heals, Icon.HEART);
        ornament(x + 20, y + 258, 650, PAPER_MUTED);
        icons.draw(batch, Icon.SCROLL, x + 12, y + 186, 48);
        label(small, v.defeated == v.total ? "O CAMINHO ESTÁ LIVRE" : "JURAMENTO DO EXPLORADOR", x + 76, y + 236, PAPER_MUTED);
        label(body, v.defeated == v.total ? "Encontre o baú ao nordeste." : "Derrote os " + (v.total - v.defeated) + " guardiões restantes.", x + 76, y + 208, PAPER_TEXT);
        float spacing=Math.min(36,560f/v.total);
        for (int i = 0; i < v.total; i++) icons.fill(batch, Icon.SKULL, x + 78 + i * spacing, y + 153, Math.min(28,spacing-3), i < v.defeated ? 1 : 0);
        label(small, "VIDA " + health(v.hp) + "/" + health(v.maxHp) + "    VIGOR " + Math.round(v.stamina) + "%    MANA " + Math.round(v.mana) + "%", x + 20, y + 119, PAPER_TEXT);
        String accuracy=v.attacks==0?"--":Math.round(100f*v.hits/v.attacks)+"%";
        label(small, "GOLPES "+v.attacks+"   ACERTOS "+accuracy+"   CORAÇÕES "+v.heartsCollected, x + 20, y + 90, PAPER_MUTED);
    }

    private void journalStat(float x, float y, String title, String value, Icon icon) {
        if(title.equals("FRASCOS"))item(115,x,y+36,42);
        else icons.draw(batch, icon, x, y + 36, 42);
        label(small, title, x + 50, y + 91, PAPER_MUTED);
        label(heading, value, x + 50, y + 62, PAPER_TEXT);
    }
    private void bestiary(float x,float y) {
        label(heading,"Códice da masmorra",x+14,y+490,PAPER_TEXT);
        if(paperButton(x+14,y+405,326,38,"Bestiário",catalogueTab==0)){catalogueTab=0;detailEntry=-1;}
        if(paperButton(x+350,y+405,326,38,"Frascos",catalogueTab==1)){catalogueTab=1;detailEntry=-1;}
        if(detailEntry>=0) {catalogueDetail(x,y,catalogueEntries()[detailEntry]);return;}
        FieldGuide.Entry[] entries=catalogueEntries();
        for(int i=0;i<entries.length;i++) {
            FieldGuide.Entry entry=entries[i];
            int cols=catalogueTab==0?3:2;
            float w=catalogueTab==0?214:326,h=catalogueTab==0?86:134;
            float cx=x+14+(i%cols)*(w+10),cy=y+391-h-(i/cols)*(h+10);
            boolean hover=contains(cx,cy,w,h),selected=focus==buttonIndex;
            cutRect(cx,cy,w,h,4,selected||hover?PAPER_TEXT:PAPER_MUTED);
            cutRect(cx+2,cy+2,w-4,h-4,3,selected||hover?WHITE:PAPER);
            rect(cx+4,cy+4,3,h-8,entry.potion()==null?GOLD:entry.potion().color);
            drawEntry(entry,cx+12,cy+h-52,40);
            label(body,entry.name(),cx+60,cy+h-10,PAPER_TEXT);
            if(catalogueTab==0)label(small,entry.note(),cx+60,cy+h-35,PAPER_MUTED);
            else {
                wrapped(entry.note(),cx+60,cy+h-40,w-72,20,PAPER_MUTED);
                label(small,entry.stat(),cx+14,cy+52,PAPER_MUTED);
            }
            rect(cx+12,cy+30,w-24,1,PAPER_MUTED,.35f);
            label(small,"Saiba mais  +",cx+14,cy+24,PAPER_TEXT);
            right(small,String.format(java.util.Locale.ROOT,"%02d",i+1),cx+w-12,cy+24,PAPER_MUTED);
            if(hitControl(cx,cy,w,h)) {detailEntry=i;focus=8;}
        }
        if(catalogueTab==0) {
            label(body,"Observe antes de atacar",x+252,y+179,PAPER_TEXT);
            label(small,"As marcas no chão revelam o perigo.",x+252,y+148,PAPER_MUTED);
            label(small,"A magia afasta e interrompe inimigos.",x+252,y+125,PAPER_MUTED);
        }
        label(small,catalogueTab==0?"7 criaturas / abra uma ficha para conhecer seus ataques.":"4 frascos / efeitos, duração e formas de escapar.",x+16,y+86,PAPER_MUTED);
    }
    private void catalogueDetail(float x,float y,FieldGuide.Entry entry) {
        socket(x+16,y+315,72);drawEntry(entry,x+24,y+323,56);
        label(body,entry.name(),x+106,y+376,PAPER_TEXT);
        label(small,entry.note(),x+106,y+347,PAPER_MUTED);
        if(paperButton(x+550,y+351,126,34,"Voltar  -",false)) {focus=8+detailEntry;detailEntry=-1;}
        rect(x+16,y+288,660,24,PAPER_MUTED,.14f);
        label(small,entry.stat(),x+28,y+309,PAPER_TEXT);
        label(body,entry.potion()==null?"Como ele ataca":"O que acontece",x+16,y+272,PAPER_TEXT);
        wrapped(entry.behavior(),x+16,y+244,652,22,PAPER_MUTED);
        label(body,entry.potion()==null?"Sua estratégia":"Como lidar com o efeito",x+16,y+167,PAPER_TEXT);
        wrapped(entry.advice(),x+16,y+139,652,22,PAPER_MUTED);
    }
    private void drawEntry(FieldGuide.Entry entry,float x,float y,float size) {
        batch.setColor(Color.WHITE);
        batch.draw(entry.potion()==null?art.enemy(entry.enemy(),0):art.tile(entry.potion().tile),x,y,size,size);
    }

    private void stat(float x, float y, String label, String value, Color color) {
        socket(x + 8, y + 28, 56);
        icons.draw(batch, label.equals("GUARDIÕES") ? Icon.SKULL : Icon.CLOCK, x + 12, y + 32, 48);
        label(small, label, x + 76, y + 90, MUTED);
        label(heading, value, x + 76, y + 60, color);
    }

    private void settings(View view,float x, float y) {
        label(heading, "Ajustes do aventureiro", x + 14, y + 464, PAPER_TEXT);
        label(small, "Escolha o conforto visual da sua jornada.", x + 16, y + 410, PAPER_MUTED);
        settingCard(x, y + 292, "Áudio", "Volume geral, música e efeitos sonoros.");
        if(paperButton(x+14,y+223,660,48,"Controle USB / calibração e sensibilidade",false))action=Action.CONFIG_DEVICE;
        label(body, "Exibição", x + 20, y + 190, PAPER_TEXT);
        label(small, "F11 alterna entre tela cheia e janela.", x + 20, y + 160, PAPER_MUTED);
        if(paperButton(x+14,y+99,660,32,"Efeitos reduzidos: "+(reducedEffects?"LIGADOS":"DESLIGADOS"),reducedEffects))toggleReducedEffects();
        label(small,"Reduz tremores, flashes, partículas e distorções.",x+20,y+94,PAPER_MUTED);
    }

    private void settingCard(float x, float y, String title, String description) {
        rect(x + 12, y + 8, 672, 74, PAPER_MUTED, .15f);
        icons.draw(batch, Icon.LOCK, x + 24, y + 25, 36);
        label(body, title, x + 78, y + 72, PAPER_TEXT);
        label(small, description, x + 78, y + 41, PAPER_MUTED);
        right(small, "EM BREVE", x + 666, y + 71, PAPER_MUTED);
    }

    private void tutorial(View view,float x, float y) {
        label(heading,"Grimório do explorador",x+14,y+490,PAPER_TEXT);
        if(paperButton(x+14,y+405,326,38,"Modo Convencional",tutorialMode==GameMode.CONVENTIONAL))tutorialMode=GameMode.CONVENTIONAL;
        if(paperButton(x+350,y+405,326,38,"Modo Reabilitação",tutorialMode==GameMode.REHABILITATION))tutorialMode=GameMode.REHABILITATION;
        if(tutorialMode==GameMode.REHABILITATION) {
            for(int i=0;i<6;i++) {
                float cx=x+14+(i%2)*336,cy=y+297-(i/2)*96;
                switch(i) {
                    case 0 -> {lesson(cx,cy,"Inclinar a mão","Incline para frente ou trás para mover o personagem.");prompts.draw(batch,InputPrompts.TILT_UP,cx+18,cy+45,32);prompts.draw(batch,InputPrompts.TILT_DOWN,cx+50,cy+13,32);}
                    case 1 -> {lesson(cx,cy,"Virar para os lados","Incline aos lados para mover o personagem.");prompts.draw(batch,InputPrompts.TURN_LEFT,cx+12,cy+29,32);prompts.draw(batch,InputPrompts.TURN_RIGHT,cx+48,cy+29,32);}
                    case 2 -> {lesson(cx,cy,"Gesto de ataque","Passe a mão sobre o sensor para a direita: ataque.");item(103,cx+16,cy+22,44);icons.draw(batch,Icon.RIGHT_ARROW,cx+54,cy+47,28);}
                    case 3 -> {lesson(cx,cy,"Marcação guiada","Marca o inimigo mais próximo para guiar o ataque.");icons.draw(batch,Icon.SKULL,cx+15,cy+16,54);icons.draw(batch,Icon.CROSSHAIR,cx+54,cy+47,28);}
                    case 4 -> {lesson(cx,cy,"Menu pelo Keypad","1: pausa/volta. 2/4: navega. 3: confirma, inclusive abas.");icons.draw(batch,Icon.PAUSE,cx+27,cy+25,40);}
                    default -> {lesson(cx,cy,"Cura automática","1 coração: cura +2 e limpa fogo/veneno. 3 usos.");item(115,cx+26,cy+22,44);}
                }
            }
            label(small,"Alvo dourado: avanço. Verde: golpe e recuo. Alcance auxiliar: 3x a espada.",x+16,y+103,PAPER_TEXT);
            label(small,"Magia: gesto para cima (25 mana). Corrida: incline forte por 4s. APDS: 2-5 cm; retire a mão.",x+16,y+84,PAPER_MUTED);
        } else {
            for(int i=0;i<6;i++) {
                float cx=x+14+(i%2)*336,cy=y+297-(i/2)*96;
                switch(i) {
                    case 0 -> {lesson(cx,cy,"Mover","Use também as setas. Explore nas diagonais.");promptDirections(cx+12,cy+17,false);}
                    case 1 -> {lesson(cx,cy,"Correr","Segure para correr. Consome vigor, que se recupera.");prompts.wide(batch,255,256,cx+13,cy+29,32);}
                    case 2 -> {lesson(cx,cy,"Espada","Clique: ataque na direção do cursor.");prompts.draw(batch,InputPrompts.MOUSE_LEFT,cx+32,cy+26,32);}
                    case 3 -> {lesson(cx,cy,"Magia","Clique: 25 de mana, dano leve e forte recuo.");prompts.draw(batch,InputPrompts.MOUSE_RIGHT,cx+32,cy+26,32);}
                    case 4 -> {lesson(cx,cy,"Curar","Recupera 2 corações e remove fogo ou veneno. 3 usos.");prompts.wide(batch,235,237,cx+13,cy+29,32);}
                    default -> {lesson(cx,cy,"Pausa / Tela cheia","Esc abre o menu. F11 alterna a exibição.");prompts.draw(batch,InputPrompts.ESC,cx+14,cy+29,32);prompts.draw(batch,InputPrompts.F11,cx+50,cy+29,32);}
                }
            }
            wrapped("Derrote os guardiões e alcance o baú. Corações: +1 de vida (30% de chance). R reinicia a fase.",x+16,y+86,660,18,PAPER_MUTED);
        }
    }
    private void lesson(float x,float y,String title,String detail) {
        cutRect(x,y,326,88,4,PAPER_MUTED);
        cutRect(x+2,y+2,322,84,3,PAPER);
        rect(x+7,y+7,82,74,PAPER_TEXT,.1f);
        label(body,title,x+102,y+76,PAPER_TEXT);
        wrapped(detail,x+102,y+48,212,19,PAPER_MUTED);
    }
    private void promptDirections(float x,float y,boolean simulation) {
        prompts.draw(batch,simulation?InputPrompts.I:InputPrompts.W,x+24,y+27,26);
        prompts.draw(batch,simulation?InputPrompts.J:InputPrompts.A,x,y,26);
        prompts.draw(batch,simulation?InputPrompts.K:InputPrompts.S,x+24,y,26);
        prompts.draw(batch,simulation?InputPrompts.L:InputPrompts.D,x+48,y,26);
    }
    private void wrapped(String text,float x,float top,float maxWidth,float lineHeight,Color color) {
        String line="";
        for(String word:text.split(" ")) {
            String candidate=line.isEmpty()?word:line+" "+word;
            if(!line.isEmpty()&&small.width(candidate)>maxWidth) {label(small,line,x,top,color);top-=lineHeight;line=word;}
            else line=candidate;
        }
        if(!line.isEmpty())label(small,line,x,top,color);
    }
    private boolean paperButton(float x,float y,float w,float h,String text,boolean active) {
        boolean selected=focus==buttonIndex,hover=contains(x,y,w,h);
        cutRect(x,y,w,h,4,selected||hover?PAPER_TEXT:PAPER_MUTED);
        cutRect(x+2,y+2,w-4,h-4,3,active?PAPER_TEXT:selected||hover?WHITE:PAPER);
        if(active)rect(x+5,y+3,w-10,3,GOLD);
        if(selected)rect(x+8,y+h/2-3,4,6,active?GOLD:PAPER_TEXT);
        center(body,text,x+w/2,y+h/2+11,active?WHITE:PAPER_TEXT);
        boolean activated=hitControl(x,y,w,h);
        if(activated&&!active)pageAge=0;
        return activated;
    }
    private boolean hitControl(float x,float y,float w,float h) {
        int index=buttonIndex++;
        boolean hover=contains(x,y,w,h);
        if(hover&&(Gdx.input.getDeltaX()!=0||Gdx.input.getDeltaY()!=0))focus=index;
        if(hover&&Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)){focus=index;return true;}
        return focus==index&&((controllerButtons&4)!=0||Gdx.input.isKeyJustPressed(Input.Keys.ENTER)||Gdx.input.isKeyJustPressed(Input.Keys.SPACE));
    }

    private void welcome(View v) {
        float x = (width - 920) / 2, y = (height - 556) / 2;
        panel(x, y, 920, 556);
        item(29, x + 44, y + 379, 64);
        item(29, x + 812, y + 379, 64);
        center(small, "CRÔNICAS DE UM MUNDO ESQUECIDO", width / 2, y + 511, GOLD);
        center(display, "MOTIONBOUND", width / 2 + 3, y + 452, PAPER_TEXT);
        center(display, "MOTIONBOUND", width / 2, y + 456, WHITE);
        center(body, "CINCO ETAPAS ATÉ O CREPÚSCULO", width / 2, y + 366, GOLD);
        center(small, "Cripta / Vila / Fazenda / Neve / Fortaleza", width / 2, y + 318, MUTED);
        ornament(x + 110, y + 282, 700, GOLD);
        float by = y + 216;
        if (canContinue(v)) {
            if (button(width / 2 - 205, by, 410, 54, "Continuar expedição", true, false)) action = Action.LOAD;
            by -= 64;
        }
        if (button(width / 2 - 205, by, 410, 54, "Nova aventura", !v.hasSave, false)) action = Action.NEW_GAME;
        if (button(width / 2 - 205, by - 58, 410, 44, "Escolher outro modo", false, false)) action=Action.MODE_SELECT;
        if (button(width / 2 - 205, by - 110, 410, 44, "Sair do jogo", false, true)) Gdx.app.exit();
        center(small, v.message.isEmpty()?"MOUSE ESQ.: espada   MOUSE DIR.: magia   ESPAÇO: cura":v.message,
                width / 2, y - 20, v.message.isEmpty()?MUTED:RED);
    }

    private void result(Screen screen, View v) {
        boolean won = screen == Screen.VICTORY;
        boolean completed=screen==Screen.LEVEL_COMPLETE;
        float x = (width - 840) / 2, y = (height - 490) / 2;
        panel(x, y, 840, 490);
        rect(x, y + 487, 840, 3, won||completed ? MINT : RED);
        center(small, won ? "CAMPANHA CONCLUÍDA / 5 DE 5" : completed?"FASE "+v.level.number()+" CONCLUÍDA":"FASE "+v.level.number()+" / NOVA TENTATIVA", width / 2, y + 446, won||completed ? MINT : RED);
        String title=won?"O crepúsculo foi vencido.":completed?"Mais um caminho foi aberto.":"Sua jornada ainda continua.";
        int letters=reducedEffects?title.length():Math.min(title.length(),(int)(screenAge*65));
        label(heading,title.substring(0,letters),width/2-heading.width(title)/2,y+398,WHITE);
        center(small, completed?"A seguir: "+CampaignLevel.of(v.level.number()+1).title():won?"Você atravessou as cinco fases da jornada.":"Recupere o fôlego e tente uma nova estratégia.", width / 2, y + 338, MUTED);
        if(reducedEffects||screenAge>.2f)stat(x + 185, y + 185, "GUARDIÕES", v.defeated + " / " + v.total, MINT);
        if(reducedEffects||screenAge>.4f)stat(x + 435, y + 185, "TEMPO ATIVO", time(v.elapsed), GOLD);
        if(v.mode==GameMode.REHABILITATION)
            center(small,v.physicalControls?"EDGE / inclinação e gestos / curas automáticas":"SIMULAÇÃO / pressão na faixa: "+Math.round(v.pressureAccuracy)+"% / dados artificiais",width/2,y+186,BLUE);
        else if(completed)center(small,"Descanse: vida, mana e poções renovadas na próxima fase.",width/2,y+186,MUTED);
        if (button(width / 2 - 205, y + 99, 410, 54, completed?"Seguir para a fase "+(v.level.number()+1):won?"Nova campanha":"Tentar esta fase", true, false))
            action=completed?Action.NEXT_STAGE:won?Action.NEW_GAME:Action.RETRY_STAGE;
        if (button(width / 2 - 205, y + 40, 410, 44, completed?"Salvar e sair":"Sair do jogo", false, true)) {
            if(completed)action=Action.SAVE_EXIT;else Gdx.app.exit();
        }
        if(!v.message.isEmpty())center(small,v.message,width/2,y-22,RED);
    }

    private boolean button(float x, float y, float w, float h, String text, boolean active, boolean danger) {
        int index = buttonIndex++;
        boolean hover = pointer.x >= x && pointer.x <= x + w && pointer.y >= y && pointer.y <= y + h;
        if (hover && (Gdx.input.getDeltaX() != 0 || Gdx.input.getDeltaY() != 0)) focus = index;
        boolean selected = focus == index;
        float marker=reducedEffects?0:Math.round(MathUtils.sin(menuTime*7)*3);
        Color accent = danger ? RED : GOLD;
        cutRect(x + 3, y - 4, w, h, 4, INK);
        cutRect(x, y, w, h, 4, selected || hover ? accent : LINE);
        cutRect(x + 3, y + 3, w - 6, h - 6, 3, selected || hover ? Color.valueOf("654536") : CARD);
        rect(x + 7, y + h - 6, w - 14, 2, selected || hover ? PAPER : LINE);
        rect(x + 7, y + 5, w - 14, 2, INK);
        if(selected||hover) {
            rect(x-9+marker,y+h/2-4,4,8,accent);
            rect(x-5+marker,y+h/2-2,3,4,accent);
            if(!reducedEffects)rect(x+8+(menuTime*90)%(w-20),y+h-7,10,2,WHITE,.65f);
        }
        if (active) rune(x + w - 24, y + h / 2 - 5, accent, 1);
        if (text.contains("aventura") || text.contains("Jogar")) item(103, x + 10, y + (h - 32) / 2, 32);
        else if (text.contains("Continuar")) item(90, x + 10, y + (h - 32) / 2, 32);
        else icons.draw(batch, text.contains("Salvar") || text.contains("Sair") ? Icon.DOOR
                : text.contains("Configurações") ? Icon.COG : text.contains("ESC")||text.endsWith("Menu") ? Icon.PAUSE : Icon.SCROLL,
                x + 10, y + (h - 28) / 2, 28);
        label(body, text, x + 48, y + h / 2 + 11, danger ? RED : selected || active || hover ? WHITE : MUTED);
        // Only the visible, focused action receives keyboard activation.
        if(hover&&Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)){focus=index;return true;}
        return selected && ((controllerButtons&4)!=0 && previous!=Screen.PLAYING&&previous!=Screen.LEVEL_INTRO || Gdx.input.isKeyJustPressed(Input.Keys.ENTER)
                || (previous != Screen.PLAYING && previous!=Screen.LEVEL_INTRO && Gdx.input.isKeyJustPressed(Input.Keys.SPACE)));
    }

    private void panel(float x, float y, float w, float h) {
        cutRect(x + 7, y - 8, w, h, 8, INK);
        cutRect(x, y, w, h, 8, INK);
        cutRect(x + 3, y + 3, w - 6, h - 6, 6, LINE);
        cutRect(x + 7, y + 7, w - 14, h - 14, 4, PANEL);
        rect(x + 12, y + h - 6, w - 24, 2, MUTED);
        rect(x + 12, y + 5, w - 24, 2, Color.valueOf("3f333d"));
        // Masonry seams and bronze rivets are deliberately drawn on the pixel grid.
        for (int i = 48; i < w - 20; i += 64) {
            rect(x + i, y + 3, 2, 5, INK);
            rect(x + i + 12, y + h - 8, 2, 5, INK);
        }
        for (int i = 40; i < h - 12; i += 48) {
            rect(x + 3, y + i, 5, 2, INK); rect(x + w - 8, y + i + 12, 5, 2, INK);
        }
        rivet(x + 9, y + 9); rivet(x + w - 15, y + 9);
        rivet(x + 9, y + h - 15); rivet(x + w - 15, y + h - 15);
    }

    private void parchment(float x, float y, float w, float h) {
        cutRect(x + 5, y - 5, w, h, 6, INK);
        cutRect(x, y, w, h, 6, PAPER_MUTED);
        cutRect(x + 4, y + 4, w - 8, h - 8, 4, PAPER);
        rect(x + 9, y + h - 12, w - 18, 6, WHITE, .45f);
        rect(x + 9, y + 8, w - 18, 4, GOLD, .7f);
        for (int i = 24; i < h - 20; i += 21) {
            rect(x + 7, y + i, 4, 2, PAPER_MUTED, .4f);
            rect(x + w - 15, y + i + 5, 8, 2, PAPER_MUTED, .4f);
        }
        // Rolled ends make quest panels and the field journal read as physical parchment.
        rect(x - 3, y + h - 5, w + 6, 5, GOLD);
        rect(x - 3, y + 2, w + 6, 4, PAPER_MUTED);
    }

    private void socket(float x, float y, float size) {
        cutRect(x, y, size, size, 4, GOLD);
        cutRect(x + 3, y + 3, size - 6, size - 6, 3, INK);
        rect(x + 6, y + 6, size - 12, size - 12, CARD);
    }
    private void item(int index, float x, float y, float size) {
        batch.setColor(Color.WHITE); batch.draw(art.tile(index), x, y, size, size);
    }
    private void rivet(float x, float y) {
        rect(x, y, 6, 6, INK); rect(x, y + 2, 4, 4, GOLD); rect(x, y + 4, 2, 2, WHITE);
    }
    private void rune(float x, float y, Color color, float alpha) {
        rect(x + 4, y, 4, 12, color, alpha); rect(x, y + 4, 12, 4, color, alpha);
        rect(x + 4, y + 4, 4, 4, INK, alpha);
    }
    private void ornament(float x, float y, float w, Color color) {
        rect(x, y + 5, w / 2 - 18, 2, color); rect(x + w / 2 + 18, y + 5, w / 2 - 18, 2, color);
        rune(x + w / 2 - 6, y, color, 1);
    }
    private void cutRect(float x, float y, float w, float h, float cut, Color color) {
        rect(x + cut, y, w - 2 * cut, h, color); rect(x, y + cut, w, h - 2 * cut, color);
    }
    private void rect(float x, float y, float w, float h, Color c) { rect(x, y, w, h, c, 1); }
    private void rect(float x, float y, float w, float h, Color c, float alpha) {
        float x0 = Math.round(x * density) / density, y0 = Math.round(y * density) / density;
        float x1 = Math.round((x + w) * density) / density, y1 = Math.round((y + h) * density) / density;
        batch.setColor(c.r, c.g, c.b, alpha); batch.draw(white, x0, y0, x1 - x0, y1 - y0); batch.setColor(Color.WHITE);
    }
    private void label(UiFont font, String text, float x, float y, Color color) { font.draw(batch, text, x, y, color); }
    private void right(UiFont font, String text, float x, float y, Color color) { label(font, text, x - font.width(text), y, color); }
    private void center(UiFont font, String text, float x, float y, Color color) { label(font, text, x - font.width(text) / 2, y, color); }
    public static String time(float elapsed) { return String.format(java.util.Locale.ROOT, "%02d:%02d", (int)elapsed / 60, (int)elapsed % 60); }
    private void disposeFonts() {
        if (small != null) small.dispose(); if (body != null) body.dispose();
        if (heading != null) heading.dispose(); if (display != null) display.dispose();
    }
    @Override public void dispose() { disposeFonts(); screenEffects.dispose();prompts.dispose();icons.dispose(); white.dispose(); batch.dispose(); }
}
