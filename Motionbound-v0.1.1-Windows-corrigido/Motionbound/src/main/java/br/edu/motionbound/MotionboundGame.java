package br.edu.motionbound;

import br.edu.motionbound.game.DungeonMap;
import br.edu.motionbound.game.GameAssets;
import br.edu.motionbound.game.SaveData;
import br.edu.motionbound.game.CombatRules;
import br.edu.motionbound.game.MeleeSwing;
import br.edu.motionbound.game.CampaignLevel;
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
    private ControlSource controls;
    private Hero player;
    private final Array<Enemy> enemies=new Array<>();
    private final Array<Spell> spells=new Array<>();
    private final Array<Spark> sparks=new Array<>();
    private final Array<HeartDrop> hearts=new Array<>();
    private final Array<DeathEcho> deaths=new Array<>();
    private final Vector2 tmp=new Vector2();
    private final Vector2 aim=new Vector2(0,-1),cursorWorld=new Vector2();
    private final MeleeSwing swing=new MeleeSwing();
    private boolean cursorVisible;
    private float elapsed,screenShake,uiTime;
    private int defeated,totalEnemies;
    private int attacks,hits,magicCasts,healsUsed,heartsCollected;
    private static final Color VOID=new Color(.055f,.064f,.10f,1f);
    private static final Color GOLD=new Color(.98f,.75f,.40f,1f);
    private static final Color ICE=new Color(.35f,.79f,1f,1f);
    private static final Color RED=new Color(.88f,.31f,.37f,1f);
    private static final Color GREEN=new Color(.40f,.85f,.60f,1f);

    private static final class Hero {
        float x,y,walk,hitInvuln,attackCd,magicCd,healCd,castFx,healFx,stamina=100,mana=100;
        int hp=6,maxHp=6,heals=3,facing=0;
        boolean moving,running;
    }
    private static final class Enemy {
        float x,y,speed,moveTimer,attackCd,hitFlash,kickX,kickY;
        int hp,maxHp,type;
        Enemy(float x,float y,int type) {
            this.x=x;this.y=y;this.type=type;
            hp= type==0?2:3;maxHp=hp;
            speed=type==0?27:37;
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
        player=new Hero();player.x=dungeon.startX();player.y=dungeon.startY();
        introRemaining=0;stageComplete=false;resumeScene=Screen.PLAYING;cursorVisible=false;
        enemies.clear(); spells.clear();sparks.clear();hearts.clear();deaths.clear();
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
            burst(player.x,player.y,GREEN,11);
        }
        if(swing.advance(dt))applyMeleeImpact();
        updateFacing();
        updateEnemies(dt);
        updateSpells(dt);
        resolveDeaths();
        collectHearts();
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
        for(int i=enemies.size-1;i>=0;i--) {
            Enemy e=enemies.get(i);
            if(e.hp<=0)continue;
            e.attackCd=Math.max(0,e.attackCd-dt);
            e.hitFlash=Math.max(0,e.hitFlash-dt);
            float dx=player.x-e.x,dy=player.y-e.y;
            float dist2=dx*dx+dy*dy;
            boolean recoiling=e.kickX*e.kickX+e.kickY*e.kickY>20*20;
            if(!recoiling && dist2<150*150 && dist2>11*11) {
                float length=(float)Math.sqrt(dist2);
                float pace=mode==GameMode.REHABILITATION?.70f:1;
                float vx=dx/length*e.speed*pace*dt,vy=dy/length*e.speed*pace*dt;
                e.x=dungeon.tryX(e.x,e.y,e.x+vx,5);
                e.y=dungeon.tryY(e.x,e.y,e.y+vy,5);
            }
            e.x=dungeon.tryX(e.x,e.y,e.x+e.kickX*dt,5);
            e.y=dungeon.tryY(e.x,e.y,e.y+e.kickY*dt,5);
            float decay=(float)Math.exp(-10*dt);
            e.kickX*=decay;e.kickY*=decay;
            if(!recoiling && Vector2.dst2(player.x,player.y,e.x,e.y)<14*14 && e.attackCd<=0 && player.hitInvuln<=0) {
                player.hp--;player.hitInvuln=.95f;e.attackCd=1.1f;
                screenShake=1.8f;
                burst(player.x,player.y,RED,8);
            }
        }
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
            if(shouldDropHeart(MathUtils.random()))hearts.add(new HeartDrop(e.x,e.y));
        }
    }
    static boolean shouldDropHeart(float roll) {return roll>=0&&roll<.30f;}
    private void collectHearts() {
        if(player.hp<=0||player.hp>=player.maxHp)return;
        for(int i=hearts.size-1;i>=0&&player.hp<player.maxHp;i--) {
            HeartDrop heart=hearts.get(i);
            if(Vector2.dst2(player.x,player.y,heart.x,heart.y)<=11*11) {
                player.hp++;heartsCollected++;hearts.removeIndex(i);burst(player.x,player.y,GREEN,8);
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
            batch.draw(death.type==0?art.slime[0][0]:art.skeleton[0][0],death.x-size/2,death.y-6,size,size);
        }
        batch.setColor(Color.WHITE);
        for(Enemy e:enemies) {
            int frame=(int)(uiTime*(e.type==0?4:6))%4;
            TextureRegion spr=e.type==0?art.slime[0][frame]:art.skeleton[0][frame];
            batch.setColor(e.hitFlash>0?RED:Color.WHITE);
            batch.draw(spr,e.x-8,e.y-6,16,16);
            batch.setColor(Color.WHITE);
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
        shapes.end();Gdx.gl.glDisable(GL20.GL_BLEND);
    }
    private void drawWorldEffects() {
        shapes.setProjectionMatrix(worldCamera.combined);
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA,GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
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
        }
        for(Spell spell:spells) {
            SaveData.SpellData saved=new SaveData.SpellData();
            saved.x=spell.x;saved.y=spell.y;saved.vx=spell.vx;saved.vy=spell.vy;saved.life=spell.life;
            data.spells.add(saved);
        }
        for(HeartDrop heart:hearts) {
            SaveData.HeartData saved=new SaveData.HeartData();saved.x=heart.x;saved.y=heart.y;data.hearts.add(saved);
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
            }
            for(SaveData.SpellData saved:data.spells) {
                Spell spell=new Spell(saved.x,saved.y,saved.vx,saved.vy);
                spell.life=saved.life;spells.add(spell);
            }
            for(SaveData.HeartData saved:data.hearts)hearts.add(new HeartDrop(saved.x,saved.y));
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
