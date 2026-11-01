package br.edu.motionbound;

import br.edu.motionbound.game.DungeonMap;
import br.edu.motionbound.game.GameAssets;
import br.edu.motionbound.game.SaveData;
import br.edu.motionbound.ui.GameUi;
import br.edu.motionbound.ui.GameUi.Screen;
import br.edu.motionbound.input.ControlSource;
import br.edu.motionbound.input.KeyboardControlSource;
import br.edu.motionbound.input.PlayerCommand;
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
    private Screen scene=Screen.MENU;
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
    private final Vector2 tmp=new Vector2();
    private float elapsed,screenShake,uiTime;
    private int defeated,totalEnemies;
    private static final Color VOID=new Color(.055f,.064f,.10f,1f);
    private static final Color GOLD=new Color(.98f,.75f,.40f,1f);
    private static final Color ICE=new Color(.35f,.79f,1f,1f);
    private static final Color RED=new Color(.88f,.31f,.37f,1f);
    private static final Color GREEN=new Color(.40f,.85f,.60f,1f);

    private static final class Hero {
        float x,y,walk,hitInvuln,attackCd,attackFx,magicCd,healCd,stamina=100,mana=100;
        int hp=6,maxHp=6,heals=3,facing=0;
        boolean moving,running;
    }
    private static final class Enemy {
        float x,y,speed,moveTimer,attackCd,hitFlash;
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
            try { SaveData.read(saveFile);hasSave=true; }
            catch(RuntimeException e) {message="O save não pôde ser lido. Você pode iniciar uma nova aventura.";}
        }
        controls=new KeyboardControlSource();
        resetWorld();
        viewport.update(Gdx.graphics.getWidth(),Gdx.graphics.getHeight(),true);
        ui.resize(Gdx.graphics.getWidth(),Gdx.graphics.getHeight());
    }
    private void resetWorld() {
        dungeon=new DungeonMap();
        player=new Hero();player.x=7*TILE+8;player.y=8*TILE+8;
        enemies.clear(); spells.clear();sparks.clear();
        spawn(13,12,0);spawn(30,8,0);spawn(39,12,1);
        spawn(8,27,0);spawn(20,21,1);spawn(38,23,0);spawn(46,26,1);
        totalEnemies=enemies.size; defeated=0;elapsed=0;screenShake=0;
        worldCamera.position.set(player.x,player.y,0);
        worldCamera.update();
    }
    private void spawn(int tileX,int tileY,int type) {
        enemies.add(new Enemy(tileX*TILE+8,tileY*TILE+8,type));
    }
    @Override public void render() {
        float dt=Math.min(Gdx.graphics.getDeltaTime(),1f/30f);
        if(scene!=Screen.PAUSED) uiTime+=dt;
        if(Gdx.input.isKeyJustPressed(Input.Keys.F11)) toggleFullscreen();
        if(Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            if(scene==Screen.PLAYING) scene=Screen.PAUSED;
            else if(scene==Screen.PAUSED && !ui.backToSummary()) scene=Screen.PLAYING;
        }
        if((scene==Screen.DEFEAT||scene==Screen.VICTORY) && Gdx.input.isKeyJustPressed(Input.Keys.R)) {
            resetWorld();scene=Screen.PLAYING;
        }
        if(scene==Screen.PLAYING) {
            if(Gdx.input.isKeyJustPressed(Input.Keys.R)){resetWorld();}
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
        switch(ui.draw(scene,uiView)) {
            case NEW_GAME -> {resetWorld();message="";scene=Screen.PLAYING;}
            case RESUME -> scene=Screen.PLAYING;
            case PAUSE -> scene=Screen.PAUSED;
            case LOAD -> loadGame();
            case SAVE_EXIT -> saveAndExit();
            case FULLSCREEN -> toggleFullscreen();
            default -> { }
        }
    }
    private void update(float dt) {
        elapsed+=dt;
        player.hitInvuln=Math.max(0,player.hitInvuln-dt);
        player.attackCd=Math.max(0,player.attackCd-dt);
        player.attackFx=Math.max(0,player.attackFx-dt);
        player.magicCd=Math.max(0,player.magicCd-dt);
        player.healCd=Math.max(0,player.healCd-dt);
        player.mana=Math.min(100,player.mana+dt*12f);
        PlayerCommand cmd=controls.read();
        player.moving=cmd.direction.len2()>.01f;
        player.running=player.moving && cmd.sprint && player.stamina>1;
        if(player.running) player.stamina=Math.max(0,player.stamina-38*dt);
        else player.stamina=Math.min(100,player.stamina+25*dt);
        if(player.moving) {
            int fx=Math.abs(cmd.direction.x)>.45f ? (cmd.direction.x>0?3:2) : -1;
            int fy=Math.abs(cmd.direction.y)>.45f ? (cmd.direction.y>0?1:0) : -1;
            if(fx!=-1 && Math.abs(cmd.direction.x)>Math.abs(cmd.direction.y))player.facing=fx;
            else if(fy!=-1) player.facing=fy;
            float speed=player.running?104:66;
            float nx=player.x+cmd.direction.x*speed*dt;
            player.x=dungeon.tryX(player.x,player.y,nx,5);
            float ny=player.y+cmd.direction.y*speed*dt;
            player.y=dungeon.tryY(player.x,player.y,ny,5);
            player.walk+=dt*(player.running?13:8);
        } else player.walk=0;
        if(cmd.attack && player.attackCd<=0) attack();
        if(cmd.magic && player.magicCd<=0 && player.mana>=25) shoot();
        if(cmd.heal && player.healCd<=0 && player.heals>0 && player.hp<player.maxHp) {
            player.healCd=1.7f;player.heals--;player.hp=Math.min(player.maxHp,player.hp+2);
            burst(player.x,player.y,GREEN,11);
        }
        updateEnemies(dt);
        updateSpells(dt);
        for(int i=sparks.size-1;i>=0;i--) {
            Spark p=sparks.get(i);p.life-=dt;p.x+=p.vx*dt;p.y+=p.vy*dt;
            if(p.life<=0)sparks.removeIndex(i);
        }
        if(defeated==totalEnemies && Vector2.dst2(player.x,player.y,47*TILE+8,27*TILE+8)<(18*18))scene=Screen.VICTORY;
        if(player.hp<=0)scene=Screen.DEFEAT;
    }
    private void updateEnemies(float dt) {
        for(int i=enemies.size-1;i>=0;i--) {
            Enemy e=enemies.get(i);
            e.attackCd=Math.max(0,e.attackCd-dt);
            e.hitFlash=Math.max(0,e.hitFlash-dt);
            float dx=player.x-e.x,dy=player.y-e.y;
            float dist2=dx*dx+dy*dy;
            if(dist2<150*150 && dist2>11*11) {
                float length=(float)Math.sqrt(dist2);
                float vx=dx/length*e.speed*dt,vy=dy/length*e.speed*dt;
                e.x=dungeon.tryX(e.x,e.y,e.x+vx,5);
                e.y=dungeon.tryY(e.x,e.y,e.y+vy,5);
            }
            if(dist2<14*14 && e.attackCd<=0 && player.hitInvuln<=0) {
                player.hp--;player.hitInvuln=.95f;e.attackCd=1.1f;
                screenShake=1.8f;
                burst(player.x,player.y,RED,8);
            }
            if(e.hp<=0) {burst(e.x,e.y,GOLD,9);enemies.removeIndex(i);defeated++;}
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
                if(Vector2.dst2(s.x,s.y,e.x,e.y)<12*12) {
                    e.hp-=2;e.hitFlash=.16f;burst(e.x,e.y,ICE,8);hit=true;break;
                }
            }
            if(hit)spells.removeIndex(i);
        }
    }
    private void attack() {
        player.attackCd=.34f;player.attackFx=.14f;
        Vector2 dir=facingVector();
        for(Enemy e:enemies) {
            tmp.set(e.x-player.x,e.y-player.y);
            if(tmp.len2()<27*27 && (tmp.len2()<3 || tmp.nor().dot(dir)>.12f)) {
                e.hp--;e.hitFlash=.18f;
                burst(e.x,e.y,GOLD,5);
                screenShake=.7f;
            }
        }
    }
    private void shoot() {
        player.magicCd=.75f;player.mana-=25;
        Vector2 dir=facingVector();
        spells.add(new Spell(player.x+dir.x*10,player.y+dir.y*10,dir.x*170,dir.y*170));
        burst(player.x,player.y,ICE,5);
    }
    private Vector2 facingVector() {
        return switch(player.facing){case 1->new Vector2(0,1);case 2->new Vector2(-1,0);case 3->new Vector2(1,0);default->new Vector2(0,-1);};
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
        int minX=Math.max(0,MathUtils.floor((worldCamera.position.x-VIEW_W/2f)/TILE)-2);
        int maxX=Math.min(DungeonMap.WIDTH-1,MathUtils.floor((worldCamera.position.x+VIEW_W/2f)/TILE)+2);
        int minY=Math.max(0,MathUtils.floor((worldCamera.position.y-VIEW_H/2f)/TILE)-2);
        int maxY=Math.min(DungeonMap.HEIGHT-1,MathUtils.floor((worldCamera.position.y+VIEW_H/2f)/TILE)+2);
        for(int x=minX;x<=maxX;x++) for(int y=minY;y<=maxY;y++) {
            boolean floor=dungeon.isFloor(x,y);
            int tile=floor?dungeon.floorTile(x,y):dungeon.wallTile(x,y);
            if(floor) batch.setColor(dungeon.overgrown(x,y)?.78f:.91f,dungeon.overgrown(x,y)?.85f:.84f,.87f,1);
            else batch.setColor(.78f,.79f,.88f,1);
            batch.draw(art.tile(tile),x*TILE,y*TILE,TILE,TILE);
            if(floor && dungeon.detailAt(x,y)>=0) {
                batch.setColor(1,1,1,.85f);batch.draw(art.detail(dungeon.detailAt(x,y)),x*TILE,y*TILE,TILE,TILE);
            }
            int prop=dungeon.propAt(x,y);
            if(prop!=0) {batch.setColor(Color.WHITE);batch.draw(art.prop(prop),x*TILE,y*TILE,TILE,TILE);}
        }
        batch.setColor(Color.WHITE);
        batch.end();
    }
    private void drawEntities() {
        batch.setProjectionMatrix(worldCamera.combined);batch.begin();
        float portalX=47*TILE+8,portalY=27*TILE+8;
        boolean ready=defeated==totalEnemies;
        batch.setColor(ready?new Color(.6f,1f,.85f,1f):new Color(.48f,.50f,.58f,1f));
        float bob=ready?MathUtils.sin(uiTime*3f):0;
        batch.draw(art.tile(ready?91:90),portalX-10,portalY-10+bob,20,20);
        batch.setColor(Color.WHITE);
        for(Enemy e:enemies) {
            int frame=(int)(uiTime*(e.type==0?4:6))%4;
            TextureRegion spr=e.type==0?art.slime[0][frame]:art.skeleton[0][frame];
            batch.setColor(e.hitFlash>0?RED:Color.WHITE);
            batch.draw(spr,e.x-8,e.y-6,16,16);
            batch.setColor(Color.WHITE);
        }
        int row=player.facing,frame=player.moving ? (int)player.walk%4 : 0;
        if(player.hitInvuln>0 && ((int)(uiTime*12))%2==0) batch.setColor(1,1,1,.35f);
        batch.draw(art.hero[row][frame],player.x-8,player.y-6,16,16);
        batch.setColor(Color.WHITE);
        batch.end();
    }
    private void drawGroundShadows() {
        shapes.setProjectionMatrix(worldCamera.combined);
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA,GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(.025f,.035f,.06f,.30f);
        shapes.ellipse(player.x-7,player.y-7,14,5,20);
        for(Enemy enemy:enemies) shapes.ellipse(enemy.x-7,enemy.y-7,14,5,20);
        shapes.end();Gdx.gl.glDisable(GL20.GL_BLEND);
    }
    private void drawWorldEffects() {
        shapes.setProjectionMatrix(worldCamera.combined);
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA,GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        float portalX=47*TILE+8,portalY=27*TILE+8;
        shapes.setColor(defeated==totalEnemies ? new Color(.25f,.98f,.75f,.3f) : new Color(.5f,.5f,.6f,.15f));
        shapes.rect(portalX-12,portalY-10,24,20);
        shapes.rect(portalX-10,portalY-12,20,24);
        if(player.attackFx>0) {
            Vector2 dir=facingVector();
            shapes.setColor(new Color(1,.82f,.45f,Math.min(1,player.attackFx*6)));
            float ax=player.x+dir.x*16,ay=player.y+dir.y*16;
            shapes.rect(ax-7,ay-3,14,6);shapes.rect(ax-3,ay-7,6,14);
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
                shapes.setColor(RED);shapes.rect(e.x-8,e.y+10,16f*e.hp/e.maxHp,3);
            }
        }
        shapes.end();Gdx.gl.glDisable(GL20.GL_BLEND);
    }
    private void drawAmbientLighting() {
        batch.setProjectionMatrix(worldCamera.combined);
        batch.setBlendFunction(GL20.GL_SRC_ALPHA,GL20.GL_ONE);
        batch.begin();
        for(int[] torch:DungeonMap.TORCHES) {
            float x=torch[0]*TILE+8,y=torch[1]*TILE+7;
            float radius=34+MathUtils.sin(uiTime*5.1f+x)*2+MathUtils.sin(uiTime*9.7f+y);
            if(torch[2]==0)batch.setColor(1,.52f,.20f,.70f);
            else batch.setColor(.28f,.85f,.70f,.65f);
            batch.draw(art.glow,x-radius,y-radius,radius*2,radius*2);
        }
        if(defeated==totalEnemies) {
            batch.setColor(.40f,.95f,.70f,.6f);
            batch.draw(art.glow,47*TILE-28,27*TILE-28,72,72);
        }
        batch.setColor(Color.WHITE);batch.end();
        batch.setBlendFunction(GL20.GL_SRC_ALPHA,GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapes.setProjectionMatrix(worldCamera.combined);
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA,GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        for(int[] torch:DungeonMap.TORCHES) {
            float x=torch[0]*TILE+8,y=torch[1]*TILE+7;
            float cycle=(uiTime*.6f+torch[0]*.13f)%1;
            shapes.setColor(torch[2]==0?GOLD:GREEN);
            shapes.rect((float)Math.floor(x+MathUtils.sin(uiTime*2+x)*2),
                    (float)Math.floor(y+cycle*12),1,1);
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
        data.x=player.x;data.y=player.y;data.hp=player.hp;data.heals=player.heals;
        data.facing=player.facing;data.stamina=player.stamina;data.mana=player.mana;
        data.elapsed=elapsed;data.defeated=defeated;data.hitInvuln=player.hitInvuln;
        data.attackCd=player.attackCd;data.magicCd=player.magicCd;data.healCd=player.healCd;
        for(Enemy enemy:enemies) {
            if(enemy.hp<=0) { data.defeated++;continue; }
            SaveData.EnemyData saved=new SaveData.EnemyData();
            saved.x=enemy.x;saved.y=enemy.y;saved.hp=enemy.hp;saved.type=enemy.type;
            saved.attackCd=enemy.attackCd;data.enemies.add(saved);
        }
        for(Spell spell:spells) {
            SaveData.SpellData saved=new SaveData.SpellData();
            saved.x=spell.x;saved.y=spell.y;saved.vx=spell.vx;saved.vy=spell.vy;saved.life=spell.life;
            data.spells.add(saved);
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
            resetWorld();
            player.x=data.x;player.y=data.y;player.hp=data.hp;player.heals=data.heals;
            player.facing=data.facing;player.stamina=data.stamina;player.mana=data.mana;
            player.hitInvuln=data.hitInvuln;player.attackCd=data.attackCd;
            player.magicCd=data.magicCd;player.healCd=data.healCd;
            elapsed=data.elapsed;defeated=data.defeated;
            enemies.clear();
            for(SaveData.EnemyData saved:data.enemies) {
                Enemy enemy=new Enemy(saved.x,saved.y,saved.type);
                enemy.hp=saved.hp;enemy.attackCd=saved.attackCd;enemies.add(enemy);
            }
            for(SaveData.SpellData saved:data.spells) {
                Spell spell=new Spell(saved.x,saved.y,saved.vx,saved.vy);
                spell.life=saved.life;spells.add(spell);
            }
            worldCamera.position.set(player.x,player.y,0);updateCamera(1);
            message="";scene=Screen.PLAYING;
        } catch(RuntimeException e) {
            hasSave=false;
            message="O save não pôde ser lido. Você pode iniciar uma nova aventura.";
            Gdx.app.error("Motionbound", "Falha ao carregar a expedição",e);
        }
    }
    @Override public void pause() {
        if(scene==Screen.PLAYING) scene=Screen.PAUSED;
    }
    @Override public void resize(int width,int height) {
        if(width<=0||height<=0)return;
        viewport.update(width,height,false);ui.resize(width,height);
    }
    @Override public void dispose() {
        batch.dispose();shapes.dispose();art.dispose();ui.dispose();
    }
}
