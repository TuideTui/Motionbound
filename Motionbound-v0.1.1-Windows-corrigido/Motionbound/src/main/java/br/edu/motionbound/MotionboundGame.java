package br.edu.motionbound;

import br.edu.motionbound.game.DungeonMap;
import br.edu.motionbound.game.GameAssets;
import br.edu.motionbound.game.SaveData;
import br.edu.motionbound.game.CombatRules;
import br.edu.motionbound.game.MeleeSwing;
import br.edu.motionbound.game.CampaignLevel;
import br.edu.motionbound.game.EnemyKind;
import br.edu.motionbound.game.PotionEffect;
import br.edu.motionbound.game.GroundArea;
import br.edu.motionbound.game.StatusEffects;
import br.edu.motionbound.game.DungeonNavigator;
import br.edu.motionbound.game.CasterAttack;
import br.edu.motionbound.ui.GameUi;
import br.edu.motionbound.ui.GameUi.Screen;
import br.edu.motionbound.input.ControlSource;
import br.edu.motionbound.input.KeyboardControlSource;
import br.edu.motionbound.input.PlayerCommand;
import br.edu.motionbound.input.GameMode;
import br.edu.motionbound.input.SimulatedSensorSource;
import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.FitViewport;

/** Motionbound: phase-one offline dungeon prototype. No Arduino is required. */
public final class MotionboundGame extends ApplicationAdapter {
    private static final int VIEW_W=384, VIEW_H=216, TILE=16;
    private Screen scene=Screen.MODE_SELECT;
    private Screen resumeScene=Screen.PLAYING;
    private int stage=1;
    private float introRemaining;
    private boolean stageComplete;
    private GameMode mode=GameMode.CONVENTIONAL,savedMode=GameMode.CONVENTIONAL;
    private SpriteBatch batch;
    private ShapeRenderer shapes;
    private OrthographicCamera worldCamera;
    private FitViewport viewport;
    private GameUi ui;
    private final GameUi.View uiView=new GameUi.View();
    private FileHandle saveFile;
    private boolean hasSave;
    private String message="";
    private GameAssets art;
    private DungeonMap dungeon;
    private DungeonNavigator navigator;
    private ControlSource controls;
    private Hero player;
    private final Array<Enemy> enemies=new Array<>();
    private final Array<Spell> spells=new Array<>();
    private final Array<Spark> sparks=new Array<>();
    private final Array<HeartDrop> hearts=new Array<>();
    private final Array<DeathEcho> deaths=new Array<>();
    private final Array<Vial> vials=new Array<>();
    private final Array<Splash> splashes=new Array<>();
    private final Array<Lightning> lightning=new Array<>();
    private final Array<GroundArea> groundAreas=new Array<>();
    private final Vector2 tmp=new Vector2();
    private final Vector2 aim=new Vector2(0,-1),cursorWorld=new Vector2();
    private final MeleeSwing swing=new MeleeSwing();
    private boolean cursorVisible;
    private float elapsed,screenShake,uiTime;
    private int defeated,totalEnemies;
    private int summonedBats,riftsCast;
    private float portalX,portalY,portalFx;
    private int attacks,hits,magicCasts,healsUsed,heartsCollected;
    private static final Color VOID=new Color(.055f,.064f,.10f,1f);
    private static final Color GOLD=new Color(.98f,.75f,.40f,1f);
    private static final Color ICE=new Color(.35f,.79f,1f,1f);
    private static final Color RED=new Color(.88f,.31f,.37f,1f);
    private static final Color GREEN=new Color(.40f,.85f,.60f,1f);
    private static final Color VIOLET=new Color(.77f,.55f,1,1);

    private static final class Hero {
        float x,y,walk,hitInvuln,attackCd,magicCd,healCd,castFx,healFx,stamina=100,mana=100;
        float hp=6,maxHp=6;
        int heals=3,facing=0;
        final StatusEffects effects=new StatusEffects();
        boolean moving,running;
    }
    private static final class Enemy {
        float x,y,speed,moveTimer,attackCd,hitFlash,kickX,kickY;
        float windup,actionTime,targetX,targetY,spawnFx;
        float defensiveCd,burstTimer,burstAngle;
        int burstRemaining;
        boolean comboPending;
        CasterAttack attack=CasterAttack.NONE;
        int cycle;
        boolean struck,portalUsed;
        PotionEffect potion=PotionEffect.FIRE;
        int hp,maxHp,type;
        Enemy(float x,float y,int type) {
            this.x=x;this.y=y;this.type=type;
            EnemyKind kind=EnemyKind.of(type);hp=kind.maxHp;maxHp=hp;speed=kind.speed;
            moveTimer=MathUtils.random(0f,2f);
        }
    }
    private static final class Spell {
        float x,y,vx,vy,life=1.15f;
        Spell(float x,float y,float vx,float vy){this.x=x;this.y=y;this.vx=vx;this.vy=vy;}
    }
    private static final class Spark {
        float x,y,vx,vy,life=.32f,max=.32f;
        Color color;
        Spark(float x,float y,float vx,float vy,Color color){
            this.x=x;this.y=y;this.vx=vx;this.vy=vy;this.color=color;
        }
    }
    private static final class HeartDrop {
        float x,y;
        HeartDrop(float x,float y){this.x=x;this.y=y;}
    }
    private static final class Vial {
        float fromX,fromY,toX,toY,elapsed,flight;
        PotionEffect effect;
        boolean direct;
        Vial(float fromX,float fromY,float toX,float toY,float flight,PotionEffect effect) {
            this.fromX=fromX;this.fromY=fromY;this.toX=toX;this.toY=toY;this.flight=flight;this.effect=effect;
        }
        float progress(){return MathUtils.clamp(elapsed/flight,0,1);}
        float x(){return MathUtils.lerp(fromX,toX,progress());}
        float y(){return MathUtils.lerp(fromY,toY,progress());}
    }
    private static final class Lightning {
        float x,y,vx,vy,life=1.6f;
        Lightning(float x,float y,float vx,float vy){this.x=x;this.y=y;this.vx=vx;this.vy=vy;}
    }
    private static final class Splash {
        float x,y,life=.5f;PotionEffect effect;
        Splash(float x,float y,PotionEffect effect){this.x=x;this.y=y;this.effect=effect;}
    }
    private static final class DeathEcho {
        float x,y,life=.28f;
        int type;
        DeathEcho(Enemy e){x=e.x;y=e.y;type=e.type;}
    }
    @Override public void create() {
        batch=new SpriteBatch();
        shapes=new ShapeRenderer();
        worldCamera=new OrthographicCamera();
        viewport=new FitViewport(VIEW_W,VIEW_H,worldCamera);
        art=new GameAssets();
        ui=new GameUi(art);
        // Stable across Gradle, VS Code and the executable JAR's working directories.
        saveFile=Gdx.files.external(".motionbound/saves/expedition.json");
        if(saveFile.exists()) {
            try { savedMode=SaveData.read(saveFile).mode;hasSave=true; }
            catch(RuntimeException e) {message="O save não pôde ser lido. Você pode iniciar uma nova aventura.";}
        }
        controls=new KeyboardControlSource();
        resetWorld();
        viewport.update(Gdx.graphics.getWidth(),Gdx.graphics.getHeight(),true);
        ui.resize(Gdx.graphics.getWidth(),Gdx.graphics.getHeight());
    }
    private void resetWorld() {
        dungeon=new DungeonMap(stage);
        navigator=new DungeonNavigator(dungeon);
        player=new Hero();player.x=dungeon.startX();player.y=dungeon.startY();
        introRemaining=0;stageComplete=false;resumeScene=Screen.PLAYING;cursorVisible=false;
        enemies.clear(); spells.clear();sparks.clear();hearts.clear();deaths.clear();
        vials.clear();splashes.clear();summonedBats=0;riftsCast=0;portalFx=0;
        lightning.clear();groundAreas.clear();
        aim.set(0,-1);swing.elapsed=-1;swing.impacted=false;
        attacks=0;hits=0;magicCasts=0;healsUsed=0;heartsCollected=0;
        if(controls instanceof SimulatedSensorSource simulated)simulated.sensors.clearMetrics();
        for(CampaignLevel.Spawn enemy:dungeon.level.spawns(dungeon))spawn(enemy.tileX(),enemy.tileY(),enemy.type());
        totalEnemies=enemies.size; defeated=0;elapsed=0;screenShake=0;
        worldCamera.position.set(Math.max(VIEW_W/2f,player.x),Math.max(VIEW_H/2f,player.y),0);
        worldCamera.update();
    }
    private void spawn(int tileX,int tileY,int type) {
        enemies.add(new Enemy(tileX*TILE+8,tileY*TILE+8,type));
    }
    private void startStage() {
        resetWorld();introRemaining=CampaignLevel.INTRO_SECONDS;
        controls.reset();message="";scene=Screen.LEVEL_INTRO;resumeScene=scene;
    }
    private void newCampaign() {stage=1;startStage();}
    private void nextStage() {
        if(!stageComplete||dungeon.level.last())return;
        stage++;startStage();
    }
    private void pauseExpedition() {resumeScene=scene;scene=Screen.PAUSED;controls.reset();}
    private void updateIntro(float delta) {
        introRemaining=Math.max(0,introRemaining-delta);
        if(introRemaining==0){controls.reset();scene=Screen.PLAYING;resumeScene=scene;}
    }
    @Override public void render() {
        float frameDelta=Math.max(0,Gdx.graphics.getDeltaTime());
        float dt=Math.min(frameDelta,1f/30f);
        if(scene!=Screen.PAUSED) uiTime+=dt;
        if(Gdx.input.isKeyJustPressed(Input.Keys.F11)) toggleFullscreen();
        if(Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            if(scene==Screen.PLAYING||scene==Screen.LEVEL_INTRO||scene==Screen.LEVEL_COMPLETE)pauseExpedition();
            else if(scene==Screen.PAUSED && !ui.backToSummary()) {scene=resumeScene;controls.reset();}
            else if(scene==Screen.MENU||scene==Screen.REHAB_SETUP)scene=Screen.MODE_SELECT;
        }
        if((scene==Screen.DEFEAT||scene==Screen.VICTORY) && Gdx.input.isKeyJustPressed(Input.Keys.R)) {
            if(scene==Screen.VICTORY)newCampaign();else startStage();
        }
        if(scene==Screen.LEVEL_INTRO)updateIntro(frameDelta);
        else if(scene==Screen.PLAYING) {
            if(Gdx.input.isKeyJustPressed(Input.Keys.R)){startStage();}
            else update(dt);
        }
        if(scene!=Screen.PAUSED) updateCamera(dt);
        Gdx.gl.glClearColor(VOID.r,VOID.g,VOID.b,1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        viewport.apply();
        drawMap();
        drawGroundAreas();
        drawGroundShadows();
        drawEntities();
        drawWorldEffects();
        drawAmbientLighting();
        uiView.hp=player.hp;uiView.maxHp=player.maxHp;uiView.heals=player.heals;
        uiView.stamina=player.stamina;uiView.mana=player.mana;uiView.elapsed=elapsed;
        uiView.defeated=defeated;uiView.total=totalEnemies;uiView.x=player.x;uiView.y=player.y;
        uiView.attackCd=player.attackCd;uiView.magicCd=player.magicCd;uiView.healCd=player.healCd;
        uiView.dungeon=dungeon;uiView.hasSave=hasSave;uiView.message=message;
        uiView.level=dungeon.level;uiView.introRemaining=introRemaining;
        uiView.effects.copyFrom(player.effects);
        uiView.mode=mode;uiView.savedMode=savedMode;
        uiView.attacks=attacks;uiView.hits=hits;uiView.heartsCollected=heartsCollected;
        uiView.pressure=0;uiView.pressureAccuracy=0;uiView.pressureSeconds=0;
        if(controls instanceof SimulatedSensorSource simulated) {
            uiView.pressure=simulated.sensors.pressure();uiView.pressureAccuracy=simulated.sensors.pressureAccuracy();
            uiView.pressureSeconds=simulated.sensors.pressureSeconds;
        }
        switch(ui.draw(scene,uiView)) {
            case NEW_GAME -> newCampaign();
            case RETRY_STAGE -> startStage();
            case NEXT_STAGE -> nextStage();
            case RESUME -> {controls.reset();scene=resumeScene;}
            case PAUSE -> pauseExpedition();
            case LOAD -> loadGame();
            case SAVE_EXIT -> saveAndExit();
            case FULLSCREEN -> toggleFullscreen();
            case CONVENTIONAL -> {setMode(GameMode.CONVENTIONAL);message="";scene=Screen.MENU;}
            case REHABILITATION -> {setMode(GameMode.REHABILITATION);message="";scene=Screen.REHAB_SETUP;}
            case MODE_SELECT -> {message="";scene=Screen.MODE_SELECT;}
            case START_SIMULATION -> {setMode(GameMode.REHABILITATION);newCampaign();}
            default -> { }
        }
    }
    private void update(float dt) {
        elapsed+=dt;
        float afflictionDamage=player.effects.advance(dt);
        if(afflictionDamage>0)hurtPlayer(afflictionDamage,true);
        portalFx=Math.max(0,portalFx-dt);
        if(player.hp<=0){scene=Screen.DEFEAT;return;}
        player.hitInvuln=Math.max(0,player.hitInvuln-dt);
        player.attackCd=Math.max(0,player.attackCd-dt);
        player.castFx=Math.max(0,player.castFx-dt);
        player.healFx=Math.max(0,player.healFx-dt);
        player.magicCd=Math.max(0,player.magicCd-dt);
        player.healCd=Math.max(0,player.healCd-dt);
        player.mana=Math.min(100,player.mana+dt*12f);
        PlayerCommand cmd=controls.read(dt);
        updateAim(cmd);
        player.moving=cmd.direction.len2()>.01f;
        player.running=player.moving && cmd.sprint && player.stamina>1;
        if(player.running) player.stamina=Math.max(0,player.stamina-38*dt);
        else player.stamina=Math.min(100,player.stamina+25*dt);
        if(player.moving) {
            float speed=player.running?104:66;
            if(mode==GameMode.REHABILITATION)speed=54;
            speed*=player.effects.movementScale();
            float nx=player.x+cmd.direction.x*speed*dt;
            player.x=dungeon.tryX(player.x,player.y,nx,5);
            float ny=player.y+cmd.direction.y*speed*dt;
            player.y=dungeon.tryY(player.x,player.y,ny,5);
            player.walk+=dt*(player.running?13:8);
        } else player.walk=0;
        if(cmd.cursorAim&&ui.blocksGameplayPointer()) {cmd.attack=false;cmd.magic=false;}
        if(cmd.attack && player.attackCd<=0 && player.castFx<=0 && player.healFx<=0) attack();
        if(cmd.magic && !swing.active() && player.healFx<=0 && player.magicCd<=0 && player.mana>=25) shoot();
        if(cmd.heal && !swing.active() && player.castFx<=0 && player.healCd<=0 && player.heals>0 && player.hp<player.maxHp) {
            player.healCd=1.7f;player.heals--;player.hp=Math.min(player.maxHp,player.hp+2);
            player.healFx=.45f;healsUsed++;
            player.effects.cleanseDamage();
            burst(player.x,player.y,GREEN,11);
        }
        if(swing.advance(dt))applyMeleeImpact();
        updateFacing();
        updateEnemies(dt);
        updateSpells(dt);
        updateLightning(dt);
        updateGroundAreas(dt);
        updateVials(dt);
        resolveDeaths();
        collectHearts();
        for(int i=splashes.size-1;i>=0;i--){Splash splash=splashes.get(i);splash.life-=dt;if(splash.life<=0)splashes.removeIndex(i);}
        for(int i=deaths.size-1;i>=0;i--) {DeathEcho death=deaths.get(i);death.life-=dt;if(death.life<=0)deaths.removeIndex(i);}
        for(int i=sparks.size-1;i>=0;i--) {
            Spark p=sparks.get(i);p.life-=dt;p.x+=p.vx*dt;p.y+=p.vy*dt;
            if(p.life<=0)sparks.removeIndex(i);
        }
        if(defeated==totalEnemies && Vector2.dst2(player.x,player.y,dungeon.exitX(),dungeon.exitY())<(18*18)) {
            stageComplete=true;controls.reset();scene=dungeon.level.last()?Screen.VICTORY:Screen.LEVEL_COMPLETE;resumeScene=scene;
        }
        if(player.hp<=0)scene=Screen.DEFEAT;
    }
    private void updateEnemies(float dt) {
        navigator.updateGoal(player.x,player.y);
        for(int i=enemies.size-1;i>=0;i--) {
            Enemy e=enemies.get(i);
            if(e.hp<=0)continue;
            EnemyKind kind=EnemyKind.of(e.type);
            e.attackCd=Math.max(0,e.attackCd-dt);
            e.defensiveCd=Math.max(0,e.defensiveCd-dt);
            e.hitFlash=Math.max(0,e.hitFlash-dt);
            e.spawnFx=Math.max(0,e.spawnFx-dt);
            float dx=player.x-e.x,dy=player.y-e.y;
            float dist2=dx*dx+dy*dy;
            boolean recoiling=e.kickX*e.kickX+e.kickY*e.kickY>20*20;
            if(recoiling){e.windup=0;e.actionTime=0;e.burstRemaining=0;e.attackCd=Math.max(e.attackCd,.6f);}
            if(e.spawnFx>0)continue;
            if(e.windup>0) {
                e.windup=Math.max(0,e.windup-dt);
                if(e.windup==0) {
                    e.struck=false;
                    e.actionTime=kind==EnemyKind.SLIME||kind==EnemyKind.BAT?.25f:.22f;
                    e.attackCd=kind==EnemyKind.CYCLOPS?2.2f:1.35f;
                    if(e.attack!=CasterAttack.NONE)releaseCasterAttack(e);
                }
            } else if(e.actionTime>0) {
                e.actionTime=Math.max(0,e.actionTime-dt);
                if(e.attack==CasterAttack.LIGHTNING_FAN&&e.burstRemaining>0) {
                    e.burstTimer-=dt;
                    while(e.burstRemaining>0&&e.burstTimer<=0) {
                        float angle=e.burstAngle+CombatRules.fanOffset(5-e.burstRemaining,5);
                        float speed=mode==GameMode.REHABILITATION?120:150;
                        lightning.add(new Lightning(e.x,e.y,MathUtils.cosDeg(angle)*speed,MathUtils.sinDeg(angle)*speed));
                        e.burstRemaining--;e.burstTimer+=.1f;
                    }
                    if(e.burstRemaining==0)e.burstTimer=0;
                } else if(kind==EnemyKind.SLIME||kind==EnemyKind.BAT) {
                    tmp.set(e.targetX-e.x,e.targetY-e.y).nor();
                    moveEnemy(e,tmp.x,tmp.y,(kind==EnemyKind.BAT?116:94)*dt);
                    if(!e.struck&&Vector2.dst2(e.x,e.y,player.x,player.y)<14*14) {
                        e.struck=true;hurtPlayer(kind==EnemyKind.BAT?.5f:1,false);
                    }
                } else if(!e.struck&&e.attack==CasterAttack.NONE) {
                    e.struck=true;
                    float radius=kind==EnemyKind.CYCLOPS?33:kind==EnemyKind.GHOST?25:27;
                    boolean facing=kind!=EnemyKind.SKELETON||dist2<4||tmp.set(e.targetX-e.x,e.targetY-e.y).nor().dot(new Vector2(player.x-e.x,player.y-e.y).nor())>.25f;
                    if(facing&&Vector2.dst2(e.x,e.y,player.x,player.y)<=radius*radius&&CombatRules.clearPath(dungeon,e.x,e.y,player.x,player.y))
                        hurtPlayer(kind==EnemyKind.GHOST?.5f:1,false);
                    burst(e.x,e.y,kind==EnemyKind.CYCLOPS?GOLD:RED,kind==EnemyKind.CYCLOPS?12:5);
                }
            } else if(!recoiling&&(e.attackCd<=0||kind==EnemyKind.WARLOCK&&dist2<60*60&&e.defensiveCd<=0&&!e.comboPending&&player.effects.slowRemaining<=0)
                    &&dist2<=attackRange(e,kind)*attackRange(e,kind)
                    &&CombatRules.clearPath(dungeon,e.x,e.y,player.x,player.y)) {
                e.targetX=player.x;e.targetY=player.y;e.attack=chooseCasterAttack(e,kind,dist2);
                if(e.attack.potion()!=null)e.potion=e.attack.potion();
                e.windup=windupDuration(e);
                if(e.attack==CasterAttack.QUICK_SLOW)e.defensiveCd=6;
                if(e.attack==CasterAttack.RIFT_VIAL){riftsCast++;e.portalUsed=true;}
            } else if(!recoiling && dist2<180*180 && dist2>11*11) {
                float pace=mode==GameMode.REHABILITATION?.70f:1;
                navigator.direction(e.x,e.y,player.x,player.y,tmp);
                if(kind.caster()&&dist2<60*60&&CombatRules.clearPath(dungeon,e.x,e.y,player.x,player.y))tmp.scl(-1);
                if((kind==EnemyKind.GHOST||kind==EnemyKind.BAT)&&CombatRules.clearPath(dungeon,e.x,e.y,player.x,player.y))
                    tmp.rotateDeg(MathUtils.sin(elapsed*3+e.moveTimer)*22);
                moveEnemy(e,tmp.x,tmp.y,e.speed*pace*dt);
            }
            e.x=dungeon.tryX(e.x,e.y,e.x+e.kickX*dt,5);
            e.y=dungeon.tryY(e.x,e.y,e.y+e.kickY*dt,5);
            float decay=(float)Math.exp(-10*dt);
            e.kickX*=decay;e.kickY*=decay;
        }
    }
    private void moveEnemy(Enemy e,float dx,float dy,float distance) {
        e.x=dungeon.tryX(e.x,e.y,e.x+dx*distance,5);
        e.y=dungeon.tryY(e.x,e.y,e.y+dy*distance,5);
    }
    private boolean ghostCanRift(Enemy e) {return stage>=4&&!e.portalUsed&&riftsCast<2;}
    private float attackRange(Enemy e,EnemyKind kind) {return kind==EnemyKind.GHOST&&ghostCanRift(e)?140:kind.attackRange;}
    private CasterAttack chooseCasterAttack(Enemy e,EnemyKind kind,float dist2) {
        if(kind==EnemyKind.MAGE)return e.cycle%3==2?CasterAttack.LIGHTNING_FAN:CasterAttack.POISON_VIAL;
        if(kind==EnemyKind.WARLOCK) {
            if(e.comboPending||player.effects.slowRemaining>0)return CasterAttack.FIRE_FAN;
            return dist2<60*60&&e.defensiveCd<=0?CasterAttack.QUICK_SLOW:CasterAttack.SLOW_VIAL;
        }
        return kind==EnemyKind.GHOST&&ghostCanRift(e)?CasterAttack.RIFT_VIAL:CasterAttack.NONE;
    }
    private float windupDuration(Enemy e) {
        float duration=switch(e.attack) {
            case QUICK_SLOW -> .24f;
            case LIGHTNING_FAN -> .9f;
            case FIRE_FAN -> .7f;
            case RIFT_VIAL -> 1.1f;
            case SLOW_VIAL -> .75f;
            case POISON_VIAL -> .8f;
            default -> EnemyKind.of(e.type).windup;
        };
        return duration*(mode==GameMode.REHABILITATION?1.3f:1);
    }
    private void releaseCasterAttack(Enemy e) {
        float flight=mode==GameMode.REHABILITATION?1.05f:.8f;
        e.struck=true;
        switch(e.attack) {
            case LIGHTNING_FAN -> {
                e.burstAngle=tmp.set(e.targetX-e.x,e.targetY-e.y).angleDeg();
                e.burstRemaining=5;e.burstTimer=0;e.actionTime=.65f;e.attackCd=3.2f;e.cycle++;
            }
            case FIRE_FAN -> {
                float angle=tmp.set(e.targetX-e.x,e.targetY-e.y).angleDeg();
                float distance=MathUtils.clamp(Vector2.dst(e.x,e.y,e.targetX,e.targetY),48,128);
                for(int n=0;n<3;n++) {
                    CombatRules.clippedLanding(dungeon,e.x,e.y,angle+CombatRules.fanOffset(n,3),distance,tmp);
                    vials.add(new Vial(e.x,e.y,tmp.x,tmp.y,flight,PotionEffect.FIRE));
                }
                e.comboPending=false;e.attackCd=3.6f;
            }
            case QUICK_SLOW -> {
                // Aim once at release: fast enough to punish approaching, still dodgeable sideways.
                e.targetX=player.x;e.targetY=player.y;
                Vial vial=new Vial(e.x,e.y,e.targetX,e.targetY,mode==GameMode.REHABILITATION?.4f:.28f,PotionEffect.SLOW);
                vial.direct=true;vials.add(vial);e.comboPending=true;e.attackCd=.55f;
            }
            default -> {
                vials.add(new Vial(e.x,e.y,e.targetX,e.targetY,flight,e.potion));
                if(e.attack==CasterAttack.SLOW_VIAL){e.comboPending=true;e.attackCd=.55f;}
                else if(e.attack==CasterAttack.POISON_VIAL){e.cycle++;e.attackCd=2.4f;}
                else e.attackCd=5;
            }
        }
    }
    private boolean hurtPlayer(float damage,boolean periodic) {
        if(player.hp<=0||!periodic&&player.hitInvuln>0)return false;
        player.hp=Math.max(0,player.hp-damage);
        player.hitInvuln=Math.max(player.hitInvuln,periodic?.12f:.85f);
        screenShake=periodic?.5f:1.5f;burst(player.x,player.y,RED,periodic?4:8);return true;
    }
    private void updateVials(float delta) {
        for(int i=vials.size-1;i>=0;i--) {
            Vial vial=vials.get(i);float oldX=vial.x(),oldY=vial.y();vial.elapsed+=delta;
            float x=vial.x(),y=vial.y();
            boolean directHit=vial.direct&&CombatRules.segmentDistance2(oldX,oldY,x,y,player.x,player.y)<=10*10
                    &&CombatRules.clearPath(dungeon,oldX,oldY,player.x,player.y);
            boolean wall=vial.direct&&!CombatRules.clearPath(dungeon,oldX,oldY,x,y);
            if(vial.elapsed<vial.flight&&!directHit&&!wall)continue;
            if(wall){x=oldX;y=oldY;}
            splashes.add(new Splash(x,y,vial.effect));burst(x,y,vial.effect.color,12);
            GroundArea area=new GroundArea(x,y,vial.effect);groundAreas.add(area);
            if(player.hp>0&&player.hitInvuln<=0&&(directHit||Vector2.dst2(player.x,player.y,x,y)<=16*16)
                    &&CombatRules.clearPath(dungeon,x,y,player.x,player.y)) {
                applyGroundEffect(area);
            }
            vials.removeIndex(i);
        }
    }
    private void updateGroundAreas(float dt) {
        for(int i=groundAreas.size-1;i>=0;i--) {
            GroundArea area=groundAreas.get(i);area.remaining-=dt;
            if(area.remaining<=0){groundAreas.removeIndex(i);continue;}
            if(Vector2.dst2(player.x,player.y,area.x,area.y)<=GroundArea.RADIUS*GroundArea.RADIUS
                    &&CombatRules.clearPath(dungeon,area.x,area.y,player.x,player.y))applyGroundEffect(area);
        }
    }
    private void applyGroundEffect(GroundArea area) {
        if(player.hp<=0||player.hitInvuln>0)return;
        if(area.effect==PotionEffect.RIFT) {
            if(player.effects.riftRemaining<=0&&teleportAndSummon(!area.summoned))area.summoned=true;
        } else player.effects.apply(area.effect);
    }
    private void updateLightning(float dt) {
        for(int i=lightning.size-1;i>=0;i--) {
            Lightning bolt=lightning.get(i);bolt.life-=dt;
            boolean consumed=bolt.life<=0;
            int steps=Math.max(1,(int)Math.ceil(Vector2.len(bolt.vx,bolt.vy)*dt/3));
            for(int n=0;n<steps&&!consumed;n++) {
                float x=bolt.x+bolt.vx*dt/steps,y=bolt.y+bolt.vy*dt/steps;
                if(!dungeon.canStand(x,y,1)){consumed=true;break;}
                if(CombatRules.segmentDistance2(bolt.x,bolt.y,x,y,player.x,player.y)<9*9) {
                    hurtPlayer(.5f,false);consumed=true;
                }
                bolt.x=x;bolt.y=y;
            }
            if(consumed){burst(bolt.x,bolt.y,VIOLET,4);lightning.removeIndex(i);}
        }
    }
    private boolean teleportAndSummon(boolean firstUse) {
        boolean summon=firstUse&&summonedBats<6;
        Array<Vector2> candidates=new Array<>();
        for(int x=4;x<DungeonMap.WIDTH-4;x++)for(int y=4;y<DungeonMap.HEIGHT-4;y++) {
            float wx=x*16+8,wy=y*16+8,distance=Vector2.dst2(player.x,player.y,wx,wy);
            if(distance<96*96||distance>220*220)continue;
            boolean clear=true;
            for(int ox=-1;ox<=1;ox++)for(int oy=-1;oy<=1;oy++)
                if(!dungeon.canStand(wx+ox*16,wy+oy*16,5))clear=false;
            if(!clear)continue;
            for(Enemy enemy:enemies)if(enemy.hp>0&&Vector2.dst2(wx,wy,enemy.x,enemy.y)<44*44){clear=false;break;}
            if(clear)candidates.add(new Vector2(wx,wy));
        }
        if(candidates.isEmpty())return false;
        candidates.shuffle();Vector2 arrival=null;Array<Vector2> batPositions=new Array<>();
        for(Vector2 candidate:candidates) {
            batPositions.clear();
            if(!summon){arrival=candidate;break;}
            for(int radius=48;radius<=80&&batPositions.size<3;radius+=16)for(int angle=0;angle<360&&batPositions.size<3;angle+=45) {
                float x=candidate.x+MathUtils.cosDeg(angle)*radius,y=candidate.y+MathUtils.sinDeg(angle)*radius;
                if(!dungeon.canStand(x,y,5))continue;
                boolean separated=true;
                for(Vector2 position:batPositions)if(Vector2.dst2(x,y,position.x,position.y)<24*24)separated=false;
                for(Enemy enemy:enemies)if(enemy.hp>0&&Vector2.dst2(x,y,enemy.x,enemy.y)<14*14)separated=false;
                if(separated)batPositions.add(new Vector2(x,y));
            }
            if(batPositions.size==3){arrival=candidate;break;}
        }
        if(arrival==null)return false;
        portalX=player.x;portalY=player.y;portalFx=.8f;
        burst(player.x,player.y,ICE,16);player.x=arrival.x;player.y=arrival.y;player.hitInvuln=1.2f;
        player.effects.apply(PotionEffect.RIFT);burst(player.x,player.y,ICE,20);
        for(Vector2 point:batPositions) {Enemy bat=new Enemy(point.x,point.y,EnemyKind.BAT.ordinal());bat.spawnFx=.8f;bat.attackCd=1.4f;enemies.add(bat);}
        if(summon){summonedBats+=3;totalEnemies+=3;}
        updateCamera(1);return true;
    }
    private void updateSpells(float dt) {
        for(int i=spells.size-1;i>=0;i--) {
            Spell s=spells.get(i);s.life-=dt;
            s.x+=s.vx*dt;s.y+=s.vy*dt;
            if(s.life<=0 || !dungeon.canStand(s.x,s.y,3)) {
                burst(s.x,s.y,ICE,6);spells.removeIndex(i);continue;
            }
            boolean hit=false;
            for(Enemy e:enemies) {
                if(e.hp<=0)continue;
                if(Vector2.dst2(s.x,s.y,e.x,e.y)<12*12) {
                    e.hp-=CombatRules.MAGIC_DAMAGE;e.hitFlash=.16f;
                    tmp.set(s.vx,s.vy).nor().scl(CombatRules.MAGIC_KNOCKBACK);
                    e.kickX=tmp.x;e.kickY=tmp.y;
                    screenShake=.9f;burst(e.x,e.y,ICE,10);hit=true;break;
                }
            }
            if(hit)spells.removeIndex(i);
        }
    }
    private void attack() {
        player.attackCd=MeleeSwing.COOLDOWN;swing.begin(aim);attacks++;
    }
    private void applyMeleeImpact() {
        boolean hit=false;
        for(Enemy e:enemies) {
            if(e.hp>0&&CombatRules.inArc(e.x-player.x,e.y-player.y,swing.direction)
                    &&CombatRules.clearPath(dungeon,player.x,player.y,e.x,e.y)) {
                e.hp--;e.hitFlash=.18f;
                e.kickX=swing.direction.x*60;e.kickY=swing.direction.y*60;hit=true;
                burst(e.x,e.y,GOLD,5);
                screenShake=.7f;
            }
        }
        if(hit)hits++;
    }
    private void shoot() {
        player.magicCd=.75f;player.mana-=25;player.castFx=.32f;magicCasts++;
        spells.add(new Spell(player.x+aim.x*10,player.y+aim.y*10,aim.x*170,aim.y*170));
        burst(player.x,player.y,ICE,5);
    }
    private void setMode(GameMode next) {
        mode=next;
        controls=next==GameMode.CONVENTIONAL?new KeyboardControlSource():new SimulatedSensorSource();
        cursorVisible=false;
    }
    private void updateAim(PlayerCommand command) {
        cursorVisible=false;
        if(command.cursorAim) {
            float sx=command.aim.x,sy=Gdx.graphics.getHeight()-command.aim.y;
            boolean inside=sx>=viewport.getScreenX()&&sx<viewport.getScreenX()+viewport.getScreenWidth()
                    &&sy>=viewport.getScreenY()&&sy<viewport.getScreenY()+viewport.getScreenHeight();
            if(inside&&!ui.blocksGameplayPointer()) {
                cursorWorld.set(command.aim);viewport.unproject(cursorWorld);
                tmp.set(cursorWorld).sub(player.x,player.y);
                if(tmp.len2()>.5f)aim.set(tmp).nor();
                cursorVisible=true;
            } else {command.attack=false;command.magic=false;}
        } else if(command.aim.len2()>.01f)aim.set(command.aim).nor();
        if(command.assistedAim) {
            Enemy target=null;float best=72*72;
            for(Enemy enemy:enemies) {
                if(enemy.hp<=0)continue;
                tmp.set(enemy.x-player.x,enemy.y-player.y);float distance=tmp.len2();
                if(distance<best&&distance>.01f&&tmp.nor().dot(aim)>.35f
                        &&CombatRules.clearPath(dungeon,player.x,player.y,enemy.x,enemy.y)) {target=enemy;best=distance;}
            }
            if(target!=null)aim.set(target.x-player.x,target.y-player.y).nor();
        }
    }
    private void updateFacing() {
        Vector2 facing=swing.active()?swing.direction:aim;
        player.facing=Math.abs(facing.x)>Math.abs(facing.y)?(facing.x>0?3:2):(facing.y>0?1:0);
    }
    private void resolveDeaths() {
        for(int i=enemies.size-1;i>=0;i--) {
            Enemy e=enemies.get(i);if(e.hp>0)continue;
            burst(e.x,e.y,GOLD,9);deaths.add(new DeathEcho(e));enemies.removeIndex(i);defeated++;
            if(e.type!=EnemyKind.BAT.ordinal()&&shouldDropHeart(MathUtils.random()))hearts.add(new HeartDrop(e.x,e.y));
        }
    }
    static boolean shouldDropHeart(float roll) {return roll>=0&&roll<.30f;}
    private void collectHearts() {
        if(player.hp<=0||player.hp>=player.maxHp)return;
        for(int i=hearts.size-1;i>=0&&player.hp<player.maxHp;i--) {
            HeartDrop heart=hearts.get(i);
            if(Vector2.dst2(player.x,player.y,heart.x,heart.y)<=11*11) {
                player.hp=Math.min(player.maxHp,player.hp+1);heartsCollected++;hearts.removeIndex(i);burst(player.x,player.y,GREEN,8);
            }
        }
    }
    private void burst(float x,float y,Color color,int count) {
        for(int i=0;i<count;i++) {
            float ang=MathUtils.random(0f,MathUtils.PI2),speed=MathUtils.random(18f,62f);
            sparks.add(new Spark(x,y,MathUtils.cos(ang)*speed,MathUtils.sin(ang)*speed,color));
        }
    }
    private void updateCamera(float dt) {
        float desiredX=MathUtils.clamp(player.x,VIEW_W/2f,DungeonMap.WIDTH*TILE-VIEW_W/2f);
        float desiredY=MathUtils.clamp(player.y,VIEW_H/2f,DungeonMap.HEIGHT*TILE-VIEW_H/2f);
        float lerp=Math.min(1f,dt*8.5f);
        worldCamera.position.x=MathUtils.lerp(worldCamera.position.x,desiredX,lerp);
        worldCamera.position.y=MathUtils.lerp(worldCamera.position.y,desiredY,lerp);
        if(screenShake>.01) {
            worldCamera.position.x+=MathUtils.random(-screenShake,screenShake);
            worldCamera.position.y+=MathUtils.random(-screenShake,screenShake);
            screenShake=Math.max(0,screenShake-dt*8);
        }
        worldCamera.update();
    }
    private void drawMap() {
        batch.setProjectionMatrix(worldCamera.combined);batch.begin();
        int minX=Math.max(0,MathUtils.floor((worldCamera.position.x-VIEW_W*worldCamera.zoom/2f)/TILE)-2);
        int maxX=Math.min(DungeonMap.WIDTH-1,MathUtils.floor((worldCamera.position.x+VIEW_W*worldCamera.zoom/2f)/TILE)+2);
        int minY=Math.max(0,MathUtils.floor((worldCamera.position.y-VIEW_H*worldCamera.zoom/2f)/TILE)-2);
        int maxY=Math.min(DungeonMap.HEIGHT-1,MathUtils.floor((worldCamera.position.y+VIEW_H*worldCamera.zoom/2f)/TILE)+2);
        for(int x=minX;x<=maxX;x++) for(int y=minY;y<=maxY;y++) {
            boolean floor=dungeon.isFloor(x,y);
            int tile=floor?dungeon.floorTile(x,y):dungeon.wallTile(x,y);
            if(dungeon.outdoors()) {
                if(floor)batch.setColor(Color.WHITE);
                else if(dungeon.level.theme()==CampaignLevel.Theme.SNOW)batch.setColor(.60f,.73f,.86f,1);
                else batch.setColor(.61f,.72f,.62f,1);
            }
            else if(floor&&stage==5)batch.setColor(Color.WHITE);
            else if(floor) batch.setColor(dungeon.overgrown(x,y)?.78f:.91f,dungeon.overgrown(x,y)?.85f:.84f,.87f,1);
            else batch.setColor(.78f,.79f,.88f,1);
            batch.draw(art.prop(tile),x*TILE,y*TILE,TILE,TILE);
            if(floor && dungeon.detailAt(x,y)>=0) {
                batch.setColor(1,1,1,.85f);batch.draw(art.detail(dungeon.detailAt(x,y)),x*TILE,y*TILE,TILE,TILE);
            }
        }
        // Draw scenery after the ground, north to south, so taller village trees aren't cut by grass.
        batch.setColor(Color.WHITE);
        for(int y=maxY;y>=minY;y--)for(int x=minX;x<=maxX;x++) {
            int prop=dungeon.propAt(x,y);if(prop==0)continue;
            float size=dungeon.level.theme()==CampaignLevel.Theme.VILLAGE&&(prop==DungeonMap.TOWN+28||prop==DungeonMap.TOWN+5)?32:TILE;
            batch.draw(art.prop(prop),x*TILE+(TILE-size)/2,y*TILE,size,size);
        }
        batch.setColor(Color.WHITE);
        batch.end();
    }
    private void drawEntities() {
        batch.setProjectionMatrix(worldCamera.combined);batch.begin();
        float portalX=dungeon.exitX(),portalY=dungeon.exitY();
        boolean ready=defeated==totalEnemies;
        batch.setColor(ready?new Color(.6f,1f,.85f,1f):new Color(.48f,.50f,.58f,1f));
        float bob=ready?MathUtils.sin(uiTime*3f):0;
        batch.draw(art.tile(ready?91:90),portalX-10,portalY-10+bob,20,20);
        batch.setColor(Color.WHITE);
        for(HeartDrop heart:hearts) {
            float floatY=MathUtils.sin(uiTime*3+heart.x)*1.2f;
            batch.draw(art.heartTexture,heart.x-5,heart.y-3+floatY,10,10);
        }
        for(DeathEcho death:deaths) {
            float progress=death.life/.28f,size=16*(.65f+.35f*progress);
            batch.setColor(1,.78f,.55f,progress);
            batch.draw(art.enemy(death.type,0),death.x-size/2,death.y-6,size,size);
        }
        batch.setColor(Color.WHITE);
        for(Enemy e:enemies) {
            int frame=(int)(uiTime*(e.type==0?4:6))%4;
            EnemyKind kind=EnemyKind.of(e.type);
            float size=kind==EnemyKind.CYCLOPS?20:16;
            float enemyLift=kind==EnemyKind.GHOST||kind==EnemyKind.BAT?3+MathUtils.sin(uiTime*5+e.moveTimer)*1.5f:0;
            float stretch=e.windup>0?.82f:e.actionTime>0?1.12f:kind==EnemyKind.SLIME?1+MathUtils.sin(uiTime*5+e.moveTimer)*.07f:1;
            if(e.hitFlash>0)batch.setColor(RED);
            else if(kind==EnemyKind.GHOST)batch.setColor(.78f,1,.88f,e.windup>0?.3f+.4f*e.windup/windupDuration(e):.75f);
            else batch.setColor(1,1,1,e.spawnFx>0?1-e.spawnFx*.7f:1);
            TextureRegion spr=art.enemy(e.type,frame);
            if(kind==EnemyKind.BAT) {
                float flap=.35f+.65f*Math.abs(MathUtils.sin(uiTime*18));
                batch.draw(art.batLeft,e.x-8,e.y-6+enemyLift,5,8,5,16,1,flap,0);
                batch.draw(art.batBody,e.x-3,e.y-6+enemyLift,6,16);
                batch.draw(art.batRight,e.x+3,e.y-6+enemyLift,0,8,5,16,1,flap,0);
            } else batch.draw(spr,e.x-size/2,e.y-6+enemyLift,size,size*stretch);
            batch.setColor(Color.WHITE);
            if(e.attack.potion()!=null&&e.windup>0)batch.draw(art.tile(e.potion.tile),e.x+2,e.y+8+MathUtils.sin(uiTime*8),8,8);
            if(kind==EnemyKind.CYCLOPS&&e.windup>0)batch.draw(art.tile(118),e.x+4,e.y+10,10,10);
        }
        for(Vial vial:vials) {
            float height=MathUtils.sin(vial.progress()*MathUtils.PI)*(vial.direct?3:22);
            batch.draw(art.tile(vial.effect.tile),vial.x()-5,vial.y()+height,5,5,10,10,1,1,vial.progress()*280);
        }
        int pose=heroPose();
        Vector2 direction=swing.active()?swing.direction:aim;
        float lean=heroLean();
        float px=player.x+direction.x*lean,py=player.y+direction.y*lean;
        boolean weapon=player.castFx<=0&&player.healFx<=0;
        float blade=swing.active()?swing.bladeAngle():aim.angleDeg()+35;
        // Depth follows the body, so a raised blade cannot jump across the face mid-swing.
        boolean behind=player.facing==1;
        float opacity=player.hitInvuln>0&&((int)(uiTime*12))%2==0?.35f:1;
        batch.setColor(1,1,1,opacity);
        if(weapon&&behind)drawWeapon(px,py,blade,pose);
        batch.draw(art.actions[player.facing][pose],px-16,py-13,32,32);
        if(weapon&&!behind) {
            drawWeapon(px,py,blade,pose);
            GameAssets.SwordHand hand=art.swordHands[player.facing][pose];
            // Restore the original palm/cuff over the handle to make the grip read as held.
            batch.draw(hand.cover(),px+hand.x()-1.5f,py+hand.y()-1.5f,3,3);
        }
        batch.setColor(Color.WHITE);
        if(player.healFx>0)batch.draw(art.tile(115),px+1,py+5,8,8);
        batch.end();
    }
    private int heroPose() {
        if(swing.active())return swing.pose();
        if(player.castFx>0)return 6;
        if(player.healFx>0)return 7;
        return player.moving?switch((int)player.walk%4){case 0->1;case 2->2;default->0;}:0;
    }
    private float heroLean() {
        return swing.active()?MathUtils.sin(swing.elapsed/MeleeSwing.DURATION*MathUtils.PI)*1.5f:0;
    }
    private void drawWeapon(float x,float y,float angle,int pose) {
        GameAssets.SwordHand hand=art.swordHands[player.facing][pose];
        // The rotation origin is the actual handle pixel, not the centre of the tile.
        float gripX=7.5f,gripY=3.5f;
        batch.draw(art.tile(103),x+hand.x()-gripX,y+hand.y()-gripY,gripX,gripY,16,16,1,1,angle-90);
    }
    private void drawGroundShadows() {
        shapes.setProjectionMatrix(worldCamera.combined);
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA,GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(.025f,.035f,.06f,.30f);
        shapes.ellipse(player.x-7,player.y-7,14,5,20);
        for(Enemy enemy:enemies) shapes.ellipse(enemy.x-7,enemy.y-7,14,5,20);
        for(HeartDrop heart:hearts)shapes.ellipse(heart.x-4,heart.y-4,8,3,12);
        for(Vial vial:vials)shapes.ellipse(vial.x()-4,vial.y()-2,8,3,12);
        shapes.end();Gdx.gl.glDisable(GL20.GL_BLEND);
    }
    /** Pixel footprints sit below the actors; their particles and glow are drawn above. */
    private void drawGroundAreas() {
        shapes.setProjectionMatrix(worldCamera.combined);
        Gdx.gl.glEnable(GL20.GL_BLEND);Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA,GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        for(GroundArea area:groundAreas) {
            Color color=area.effect.color;float age=GroundArea.DURATION-area.remaining,fade=Math.min(1,area.remaining);
            float pulse=.86f+.14f*MathUtils.sin(age*5);
            for(int dx=-14;dx<14;dx+=2)for(int dy=-14;dy<14;dy+=2) {
                float radius2=(dx+1)*(dx+1)+(dy+1)*(dy+1);
                if(radius2>GroundArea.RADIUS*GroundArea.RADIUS||!dungeon.canStand(area.x+dx+1,area.y+dy+1,1))continue;
                boolean edge=radius2>11*11;
                float shade=edge?.85f:.30f;
                shapes.setColor(color.r*shade,color.g*shade,color.b*shade,(edge?.7f:.82f)*fade);
                shapes.rect(Math.round(area.x+dx),Math.round(area.y+dy),2,2);
                if(!edge&&(dx*7+dy*3)%11==0) {
                    shapes.setColor(color.r,color.g,color.b,.35f*pulse*fade);
                    shapes.rect(Math.round(area.x+dx),Math.round(area.y+dy),2,1);
                }
            }
            if(area.effect==PotionEffect.RIFT) {
                for(int arm=0;arm<3;arm++)for(int n=0;n<12;n++) {
                    float radius=2+n*.9f,angle=age*125+arm*120-n*21;
                    float x=area.x+MathUtils.cosDeg(angle)*radius,y=area.y+MathUtils.sinDeg(angle)*radius;
                    if(!dungeon.canStand(x,y,1))continue;
                    shapes.setColor(.3f+n*.045f,.65f+n*.025f,1,fade*(.45f+n*.035f));
                    shapes.rect(Math.round(x),Math.round(y),2,1);
                }
                shapes.setColor(.02f,.04f,.18f,fade);shapes.rect(area.x-2,area.y-2,4,4);
            }
        }
        shapes.end();Gdx.gl.glDisable(GL20.GL_BLEND);
    }
    private void drawAreaParticles(GroundArea area) {
        float age=GroundArea.DURATION-area.remaining,fade=Math.min(1,area.remaining);
        Color color=area.effect.color;
        for(int n=0;n<9;n++) {
            float cycle=(age*(area.effect==PotionEffect.RIFT?.85f:1.25f)+n*.137f)%1;
            float x=area.x+MathUtils.sin(n*2.4f)*10,y=area.y+MathUtils.cos(n*2.4f)*7;
            if(!dungeon.canStand(x,y,1))continue;
            switch(area.effect) {
                case FIRE -> {
                    y+=cycle*15;
                    shapes.setColor(1,.24f+cycle*.48f,.06f,(1-cycle)*fade);shapes.rect(Math.round(x),Math.round(y),3,5-cycle*3);
                    shapes.setColor(1,.94f,.48f,(1-cycle)*fade);shapes.rect(Math.round(x+1),Math.round(y),1,3);
                    if(n%3==0){shapes.setColor(1,.72f,.17f,(1-cycle)*fade);shapes.rect(Math.round(x+MathUtils.sin(age*5+n)*3),Math.round(y+7),1,1);}
                }
                case POISON -> {
                    float radius=1+cycle*2;
                    shapes.setColor(.28f,1,.22f,(1-cycle)*fade);
                    // Squared bubbles grow, rise and pop, with a bright reflection on their rim.
                    x=Math.round(x);y=Math.round(y+cycle*4);
                    shapes.rect(x-radius,y-radius,2*radius,1);shapes.rect(x-radius,y+radius,2*radius,1);
                    shapes.rect(x-radius,y-radius,1,2*radius);shapes.rect(x+radius,y-radius,1,2*radius);
                    shapes.setColor(.8f,1,.55f,(1-cycle)*fade);shapes.rect(x-radius,y+radius,2,1);
                    if(cycle>.8f)shapes.rect(x+radius+2,y+radius+2,1,1);
                }
                case RIFT -> {
                    float radius=(1-cycle)*17,angle=age*180+n*137+cycle*130;
                    x=area.x+MathUtils.cosDeg(angle)*radius;y=area.y+MathUtils.sinDeg(angle)*radius;
                    if(!dungeon.canStand(x,y,1))continue;
                    shapes.setColor(.5f,.85f,1,(.4f+cycle*.6f)*fade);shapes.rect(Math.round(x),Math.round(y),2,2);
                    shapes.setColor(.88f,.98f,1,cycle*fade);shapes.rect(Math.round(x),Math.round(y),1,1);
                }
                case SLOW -> {
                    x+=MathUtils.sin(age*2+n)*3;y+=cycle*9;
                    shapes.setColor(color.r,color.g,color.b,(1-cycle)*.55f*fade);
                    shapes.rect(Math.round(x-3),Math.round(y),7,2);shapes.rect(Math.round(x-1),Math.round(y+2),6,2);
                    shapes.setColor(.9f,.91f,1,(1-cycle)*.65f*fade);shapes.rect(Math.round(x),Math.round(y+3),2,1);
                }
            }
        }
    }
    private void drawWorldEffects() {
        shapes.setProjectionMatrix(worldCamera.combined);
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA,GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        for(Enemy enemy:enemies) {
            EnemyKind kind=EnemyKind.of(enemy.type);
            if(enemy.spawnFx>0)pixelRing(enemy.x,enemy.y,12+(1-enemy.spawnFx)*8,PotionEffect.RIFT.color,enemy.spawnFx);
            if(enemy.windup>0) {
                float duration=windupDuration(enemy);
                shapes.setColor(.12f,.08f,.15f,.85f);shapes.rect(enemy.x-9,enemy.y+17,18,3);
                Color color=enemy.attack==CasterAttack.LIGHTNING_FAN?VIOLET:enemy.attack.potion()!=null?enemy.potion.color:RED;shapes.setColor(color);
                shapes.rect(enemy.x-9,enemy.y+17,18*(1-enemy.windup/duration),3);
                if(enemy.attack==CasterAttack.FIRE_FAN||enemy.attack==CasterAttack.LIGHTNING_FAN) {
                    float angle=tmp.set(enemy.targetX-enemy.x,enemy.targetY-enemy.y).angleDeg();
                    int count=enemy.attack==CasterAttack.FIRE_FAN?3:5;
                    float distance=count==3?MathUtils.clamp(Vector2.dst(enemy.x,enemy.y,enemy.targetX,enemy.targetY),48,128):96;
                    for(int shot=0;shot<count;shot++) {
                        float a=angle+CombatRules.fanOffset(shot,count);
                        CombatRules.clippedLanding(dungeon,enemy.x,enemy.y,a,distance,tmp);
                        float tx=tmp.x,ty=tmp.y;
                        if(count==3)pixelRing(tx,ty,14,color,.65f);
                        else for(int p=10;p<distance;p+=8) {
                            float px=enemy.x+MathUtils.cosDeg(a)*p,py=enemy.y+MathUtils.sinDeg(a)*p;
                            if(!CombatRules.clearPath(dungeon,enemy.x,enemy.y,px,py))break;
                            shapes.setColor(color.r,color.g,color.b,.5f);shapes.rect(Math.round(px),Math.round(py),2,1);
                        }
                    }
                    if(count==5) {pixelRing(enemy.x,enemy.y+10,6,VIOLET,.9f);shapes.setColor(Color.WHITE);shapes.rect(enemy.x-1,enemy.y+8,2,4);}
                }
                else if(enemy.attack.potion()!=null)pixelRing(enemy.targetX,enemy.targetY,16,color,.6f+.3f*MathUtils.sin(uiTime*9));
                else if(kind==EnemyKind.CYCLOPS)pixelRing(enemy.x,enemy.y,33,color,.65f);
                else {
                    float angle=MathUtils.atan2(enemy.targetY-enemy.y,enemy.targetX-enemy.x)*MathUtils.radiansToDegrees;
                    for(int a=-45;a<=45;a+=15)shapes.rect(enemy.x+MathUtils.cosDeg(angle+a)*kind.attackRange,
                            enemy.y+MathUtils.sinDeg(angle+a)*kind.attackRange,2,2);
                }
            }
            if(kind==EnemyKind.CYCLOPS&&enemy.actionTime>0)pixelRing(enemy.x,enemy.y,33*(1-enemy.actionTime/.22f),GOLD,enemy.actionTime*3);
        }
        for(Vial vial:vials)pixelRing(vial.toX,vial.toY,16,vial.effect.color,.5f+vial.progress()*.4f);
        for(GroundArea area:groundAreas)drawAreaParticles(area);
        for(Lightning bolt:lightning) {
            float angle=MathUtils.atan2(bolt.vy,bolt.vx),dx=MathUtils.cos(angle),dy=MathUtils.sin(angle);
            float px=bolt.x-dx*15,py=bolt.y-dy*15;
            for(int n=1;n<=5;n++) {
                float zig=n==5?0:(n%2==0?-2:2);
                float x=bolt.x-dx*(15-n*3)-dy*zig,y=bolt.y-dy*(15-n*3)+dx*zig;
                shapes.setColor(VIOLET);shapes.rectLine(px,py,x,y,3);
                shapes.setColor(Color.WHITE);shapes.rectLine(px,py,x,y,1);
                px=x;py=y;
            }
        }
        for(Splash splash:splashes)pixelRing(splash.x,splash.y,2+(1-splash.life/.5f)*20,splash.effect.color,splash.life*1.5f);
        if(portalFx>0)pixelRing(portalX,portalY,8+(1-portalFx/.8f)*20,ICE,portalFx);
        if(player.effects.damageEffect!=null) {
            Color color=player.effects.damageEffect.color;
            for(int i=0;i<5;i++) {
                float cycle=(uiTime*1.2f+i*.21f)%1;
                shapes.setColor(color.r,color.g,color.b,1-cycle*.6f);
                float x=player.x+MathUtils.sin(i*2+uiTime*3)*6,y=player.y+cycle*17;
                shapes.rect(Math.round(x),Math.round(y),player.effects.damageEffect==PotionEffect.FIRE?2:3,2);
            }
        }
        if(player.effects.slowRemaining>0)pixelRing(player.x,player.y-3,9,PotionEffect.SLOW.color,.8f);
        if(player.effects.riftRemaining>0)pixelRing(player.x,player.y,12+MathUtils.sin(uiTime*12)*3,ICE,.8f);
        float portalX=dungeon.exitX(),portalY=dungeon.exitY();
        shapes.setColor(defeated==totalEnemies ? new Color(.25f,.98f,.75f,.3f) : new Color(.5f,.5f,.6f,.15f));
        shapes.rect(portalX-12,portalY-10,24,20);
        shapes.rect(portalX-10,portalY-12,20,24);
        if(swing.trail()>0) {
            float angle=swing.bladeAngle();
            GameAssets.SwordHand hand=art.swordHands[player.facing][heroPose()];
            float hx=player.x+swing.direction.x*heroLean()+hand.x();
            float hy=player.y+swing.direction.y*heroLean()+hand.y();
            for(int i=0;i<9;i++) {
                float a=angle+i*5,alpha=swing.trail()*(1-i/10f);
                shapes.setColor(1,.76f,.34f,alpha*.65f);
                for(int radius=9;radius<=13;radius+=2)
                    shapes.rect(Math.round(hx+MathUtils.cosDeg(a)*radius),Math.round(hy+MathUtils.sinDeg(a)*radius),2,2);
                shapes.setColor(1,.96f,.76f,alpha);
                shapes.rect(Math.round(hx+MathUtils.cosDeg(a)*13),Math.round(hy+MathUtils.sinDeg(a)*13),2,2);
            }
        }
        if(player.castFx>0) {
            float cx=player.x+aim.x*9,cy=player.y+5+aim.y*7;
            shapes.setColor(ICE);shapes.rect(cx-3,cy-1,6,2);shapes.rect(cx-1,cy-3,2,6);
            shapes.setColor(Color.WHITE);shapes.rect(cx-1,cy-1,2,2);
        }
        if(player.healFx>0) {
            shapes.setColor(GREEN);
            float rising=(1-player.healFx/.45f)*12;
            for(int i=-1;i<=1;i++) {
                float x=player.x+i*7,y=player.y+5+rising-Math.abs(i)*3;
                shapes.rect(x-2,y,5,1);shapes.rect(x,y-2,1,5);
            }
        }
        if(scene==Screen.PLAYING) {
            Vector2 direction=swing.active()?swing.direction:aim;
            shapes.setColor(mode==GameMode.CONVENTIONAL?GOLD:GREEN);
            float tx=player.x+direction.x*(CombatRules.REACH-6),ty=player.y+direction.y*(CombatRules.REACH-6);
            shapes.rect(Math.round(tx),Math.round(ty),2,2);
            if(cursorVisible&&mode==GameMode.CONVENTIONAL) {
                float cx=Math.round(cursorWorld.x),cy=Math.round(cursorWorld.y);
                for(int dx:new int[]{-4,2})for(int dy:new int[]{-4,2}) {
                    shapes.rect(cx+dx,cy+dy,2,1);shapes.rect(cx+dx,cy+dy,1,2);
                }
            }
        }
        for(Spell s:spells) {
            shapes.setColor(new Color(.12f,.43f,1f,.35f));shapes.rect(s.x-5,s.y-3,10,6);shapes.rect(s.x-3,s.y-5,6,10);
            shapes.setColor(ICE);shapes.rect(s.x-2,s.y-2,4,4);
            shapes.setColor(Color.WHITE);shapes.rect(s.x-1,s.y,2,2);
        }
        for(Spark p:sparks) {
            shapes.setColor(p.color.r,p.color.g,p.color.b,MathUtils.clamp(p.life/p.max,0,1));
            shapes.rect(p.x,p.y,2,2);
        }
        for(Enemy e:enemies) {
            if(e.hp<e.maxHp) {
                shapes.setColor(.14f,.11f,.18f,.9f);shapes.rect(e.x-8,e.y+10,16,3);
                shapes.setColor(RED);shapes.rect(e.x-8,e.y+10,16f*Math.max(0,e.hp)/e.maxHp,3);
            }
        }
        shapes.end();Gdx.gl.glDisable(GL20.GL_BLEND);
    }
    private void pixelRing(float x,float y,float radius,Color color,float alpha) {
        shapes.setColor(color.r,color.g,color.b,MathUtils.clamp(alpha,0,1));
        for(int i=0;i<24;i++) {
            float angle=i*15;
            shapes.rect(Math.round(x+MathUtils.cosDeg(angle)*radius),Math.round(y+MathUtils.sinDeg(angle)*radius),2,1);
        }
    }
    private void drawAmbientLighting() {
        batch.setProjectionMatrix(worldCamera.combined);
        batch.setBlendFunction(GL20.GL_SRC_ALPHA,GL20.GL_ONE);
        batch.begin();
        for(int[] torch:dungeon.torches) {
            float x=torch[0]*TILE+8,y=torch[1]*TILE+7;
            float radius=34+MathUtils.sin(uiTime*5.1f+x)*2+MathUtils.sin(uiTime*9.7f+y);
            if(torch[2]==0)batch.setColor(1,.52f,.20f,dungeon.outdoors()?.30f:.70f);
            else batch.setColor(.28f,.85f,.70f,.65f);
            batch.draw(art.glow,x-radius,y-radius,radius*2,radius*2);
        }
        for(GroundArea area:groundAreas) {
            float age=GroundArea.DURATION-area.remaining,fade=Math.min(1,area.remaining),radius=24+MathUtils.sin(age*6)*2;
            Color color=area.effect.color;
            batch.setColor(color.r,color.g,color.b,fade*(area.effect==PotionEffect.SLOW?.32f:.65f));
            batch.draw(art.glow,area.x-radius,area.y-radius,radius*2,radius*2);
        }
        if(defeated==totalEnemies) {
            batch.setColor(.40f,.95f,.70f,.6f);
            batch.draw(art.glow,dungeon.exitX()-36,dungeon.exitY()-36,72,72);
        }
        batch.setColor(Color.WHITE);batch.end();
        batch.setBlendFunction(GL20.GL_SRC_ALPHA,GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapes.setProjectionMatrix(worldCamera.combined);
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA,GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        for(int[] torch:dungeon.torches) {
            float x=torch[0]*TILE+8,y=torch[1]*TILE+7;
            float cycle=(uiTime*.6f+torch[0]*.13f)%1;
            shapes.setColor(torch[2]==0?GOLD:GREEN);
            shapes.rect((float)Math.floor(x+MathUtils.sin(uiTime*2+x)*2),
                    (float)Math.floor(y+cycle*12),1,1);
        }
        if(dungeon.level.theme()==CampaignLevel.Theme.SNOW) {
            shapes.setColor(.90f,.96f,1,.6f);
            for(int i=0;i<90;i++) {
                float x=(i*97+MathUtils.sin(uiTime+i)*5)%880;
                float y=(i*67-uiTime*12)%560;if(y<0)y+=560;
                shapes.rect((int)x,(int)y,1,1);
            }
        }
        for(int i=0;i<30;i++) {
            float x=64+(i*97)%750+MathUtils.sin(uiTime*.4f+i)*4;
            float y=58+(i*67)%430+MathUtils.sin(uiTime*.3f+i*2)*3;
            if(!dungeon.canStand(x,y,0))continue;
            shapes.setColor(.93f,.81f,.58f,.12f+.16f*Math.max(0,MathUtils.sin(uiTime+i)));
            shapes.rect((int)x,(int)y,1,1);
        }
        shapes.end();Gdx.gl.glDisable(GL20.GL_BLEND);
    }
    private void toggleFullscreen() {
        if(Gdx.graphics.isFullscreen()) Gdx.graphics.setWindowedMode(1280,720);
        else Gdx.graphics.setFullscreenMode(Gdx.graphics.getDisplayMode());
    }
    private SaveData captureSave() {
        SaveData data=new SaveData();
        data.stage=stage;data.introRemaining=introRemaining;data.stageComplete=stageComplete;
        data.summonedBats=summonedBats;data.riftsCast=riftsCast;data.effects.copyFrom(player.effects);
        data.x=player.x;data.y=player.y;data.hp=player.hp;data.heals=player.heals;
        data.facing=player.facing;data.stamina=player.stamina;data.mana=player.mana;
        data.elapsed=elapsed;data.defeated=defeated;data.hitInvuln=player.hitInvuln;
        data.attackCd=player.attackCd;data.magicCd=player.magicCd;data.healCd=player.healCd;
        data.mode=mode;data.simulatedSensors=mode==GameMode.REHABILITATION;data.aimX=aim.x;data.aimY=aim.y;
        data.swingTime=swing.elapsed;data.swingX=swing.direction.x;data.swingY=swing.direction.y;
        data.swingImpacted=swing.impacted;data.castFx=player.castFx;data.healFx=player.healFx;
        data.attacks=attacks;data.hits=hits;data.magicCasts=magicCasts;data.healsUsed=healsUsed;data.heartsCollected=heartsCollected;
        if(controls instanceof SimulatedSensorSource simulated) {
            data.pressureSeconds=simulated.sensors.pressureSeconds;data.pressureInBandSeconds=simulated.sensors.pressureInBandSeconds;
        }
        for(Enemy enemy:enemies) {
            if(enemy.hp<=0) { data.defeated++;continue; }
            SaveData.EnemyData saved=new SaveData.EnemyData();
            saved.x=enemy.x;saved.y=enemy.y;saved.hp=enemy.hp;saved.type=enemy.type;
            saved.attackCd=enemy.attackCd;saved.kickX=enemy.kickX;saved.kickY=enemy.kickY;data.enemies.add(saved);
            saved.windup=enemy.windup;saved.actionTime=enemy.actionTime;saved.targetX=enemy.targetX;saved.targetY=enemy.targetY;
            saved.spawnFx=enemy.spawnFx;saved.cycle=enemy.cycle;saved.struck=enemy.struck;saved.portalUsed=enemy.portalUsed;saved.potion=enemy.potion;
            saved.attack=enemy.attack;saved.defensiveCd=enemy.defensiveCd;saved.comboPending=enemy.comboPending;
            saved.burstRemaining=enemy.burstRemaining;saved.burstTimer=enemy.burstTimer;saved.burstAngle=enemy.burstAngle;
        }
        for(Spell spell:spells) {
            SaveData.SpellData saved=new SaveData.SpellData();
            saved.x=spell.x;saved.y=spell.y;saved.vx=spell.vx;saved.vy=spell.vy;saved.life=spell.life;
            data.spells.add(saved);
        }
        for(HeartDrop heart:hearts) {
            SaveData.HeartData saved=new SaveData.HeartData();saved.x=heart.x;saved.y=heart.y;data.hearts.add(saved);
        }
        for(Vial vial:vials) {
            SaveData.VialData saved=new SaveData.VialData();saved.fromX=vial.fromX;saved.fromY=vial.fromY;
            saved.toX=vial.toX;saved.toY=vial.toY;saved.elapsed=vial.elapsed;saved.flight=vial.flight;saved.effect=vial.effect;data.vials.add(saved);
            saved.direct=vial.direct;
        }
        for(Lightning bolt:lightning) {
            SaveData.LightningData saved=new SaveData.LightningData();saved.x=bolt.x;saved.y=bolt.y;
            saved.vx=bolt.vx;saved.vy=bolt.vy;saved.life=bolt.life;data.lightning.add(saved);
        }
        for(GroundArea area:groundAreas) {
            SaveData.GroundAreaData saved=new SaveData.GroundAreaData();saved.x=area.x;saved.y=area.y;saved.remaining=area.remaining;
            saved.effect=area.effect;saved.summoned=area.summoned;data.groundAreas.add(saved);
        }
        return data;
    }
    private void saveAndExit() {
        try {
            captureSave().write(saveFile);
            Gdx.app.exit();
        } catch(Exception e) {
            message="Não foi possível salvar. A partida continua aberta; tente novamente.";
            Gdx.app.error("Motionbound", "Falha ao salvar a expedição",e);
        }
    }
    private void loadGame() {
        try {
            SaveData data=SaveData.read(saveFile);
            setMode(data.mode);
            stage=data.stage;
            resetWorld();
            introRemaining=data.introRemaining;stageComplete=data.stageComplete;
            summonedBats=data.summonedBats;riftsCast=data.riftsCast;totalEnemies=dungeon.level.enemyCount()+summonedBats;
            player.effects.copyFrom(data.effects);
            player.x=data.x;player.y=data.y;player.hp=data.hp;player.heals=data.heals;
            player.facing=data.facing;player.stamina=data.stamina;player.mana=data.mana;
            player.hitInvuln=data.hitInvuln;player.attackCd=data.attackCd;
            player.magicCd=data.magicCd;player.healCd=data.healCd;
            aim.set(data.aimX,data.aimY);swing.elapsed=data.swingTime;swing.direction.set(data.swingX,data.swingY);swing.impacted=data.swingImpacted;
            player.castFx=data.castFx;player.healFx=data.healFx;
            attacks=data.attacks;hits=data.hits;magicCasts=data.magicCasts;healsUsed=data.healsUsed;heartsCollected=data.heartsCollected;
            if(controls instanceof SimulatedSensorSource simulated) {
                simulated.sensors.pressureSeconds=data.pressureSeconds;simulated.sensors.pressureInBandSeconds=data.pressureInBandSeconds;
            }
            elapsed=data.elapsed;defeated=data.defeated;
            enemies.clear();
            for(SaveData.EnemyData saved:data.enemies) {
                Enemy enemy=new Enemy(saved.x,saved.y,saved.type);
                enemy.hp=saved.hp;enemy.attackCd=saved.attackCd;enemy.kickX=saved.kickX;enemy.kickY=saved.kickY;enemies.add(enemy);
                enemy.windup=saved.windup;enemy.actionTime=saved.actionTime;enemy.targetX=saved.targetX;enemy.targetY=saved.targetY;
                enemy.spawnFx=saved.spawnFx;enemy.cycle=saved.cycle;enemy.struck=saved.struck;enemy.portalUsed=saved.portalUsed;enemy.potion=saved.potion;
                enemy.attack=saved.attack;enemy.defensiveCd=saved.defensiveCd;enemy.comboPending=saved.comboPending;
                enemy.burstRemaining=saved.burstRemaining;enemy.burstTimer=saved.burstTimer;enemy.burstAngle=saved.burstAngle;
            }
            for(SaveData.SpellData saved:data.spells) {
                Spell spell=new Spell(saved.x,saved.y,saved.vx,saved.vy);
                spell.life=saved.life;spells.add(spell);
            }
            for(SaveData.HeartData saved:data.hearts)hearts.add(new HeartDrop(saved.x,saved.y));
            for(SaveData.VialData saved:data.vials) {
                Vial vial=new Vial(saved.fromX,saved.fromY,saved.toX,saved.toY,saved.flight,saved.effect);vial.elapsed=saved.elapsed;vial.direct=saved.direct;vials.add(vial);
            }
            for(SaveData.LightningData saved:data.lightning) {
                Lightning bolt=new Lightning(saved.x,saved.y,saved.vx,saved.vy);bolt.life=saved.life;lightning.add(bolt);
            }
            for(SaveData.GroundAreaData saved:data.groundAreas) {
                GroundArea area=new GroundArea(saved.x,saved.y,saved.effect);area.remaining=saved.remaining;area.summoned=saved.summoned;groundAreas.add(area);
            }
            worldCamera.position.set(player.x,player.y,0);updateCamera(1);
            controls.reset();message="";
            scene=stageComplete?(dungeon.level.last()?Screen.VICTORY:Screen.LEVEL_COMPLETE):introRemaining>0?Screen.LEVEL_INTRO:Screen.PLAYING;
            resumeScene=scene;
        } catch(RuntimeException e) {
            hasSave=false;
            message="O save não pôde ser lido. Você pode iniciar uma nova aventura.";
            Gdx.app.error("Motionbound", "Falha ao carregar a expedição",e);
        }
    }
    @Override public void pause() {
        if(scene==Screen.PLAYING||scene==Screen.LEVEL_INTRO||scene==Screen.LEVEL_COMPLETE)pauseExpedition();
    }
    @Override public void resize(int width,int height) {
        if(width<=0||height<=0)return;
        viewport.update(width,height,false);ui.resize(width,height);
    }
    @Override public void dispose() {
        batch.dispose();shapes.dispose();art.dispose();ui.dispose();
    }
}
