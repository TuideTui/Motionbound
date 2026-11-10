package br.edu.motionbound.game;

import br.edu.motionbound.input.GameMode;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonWriter;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/** One versioned local checkpoint, including combat state. Never deserialize arbitrary classes. */
public final class SaveData {
    public int version = 8;
    public boolean teleportPending;
    public float teleportTargetX,teleportTargetY;
    public int stage=1;
    public float introRemaining;
    public boolean stageComplete;
    public float x, y, elapsed, stamina, mana, hitInvuln, attackCd, magicCd, healCd;
    public float hp;
    public int heals, facing, defeated;
    public int summonedBats,riftsCast;
    public StatusEffects effects=new StatusEffects();
    public GameMode mode=GameMode.CONVENTIONAL;
    public boolean simulatedSensors;
    public boolean physicalController;
    public float aimX, aimY=-1, swingTime=-1, swingX, swingY=-1, castFx, healFx;
    public boolean swingImpacted;
    public GuidedStrike guidedStrike=new GuidedStrike();
    public int guidedTargetIndex=-1;
    public int attacks, hits, magicCasts, healsUsed, heartsCollected;
    public float pressureSeconds, pressureInBandSeconds;
    public Array<EnemyData> enemies = new Array<>();
    public Array<SpellData> spells = new Array<>();
    public Array<HeartData> hearts = new Array<>();
    public Array<VialData> vials=new Array<>();
    public Array<LightningData> lightning=new Array<>();
    /** Read-only migration input from checkpoints up to version six. */
    public Array<GroundFireData> groundFires=new Array<>();
    public Array<GroundAreaData> groundAreas=new Array<>();

    public static final class EnemyData {
        public float x, y, attackCd, kickX, kickY;
        public int hp, type;
        public float windup,actionTime,targetX,targetY,spawnFx;
        public int cycle;
        public boolean struck,portalUsed;
        public PotionEffect potion=PotionEffect.FIRE;
        public CasterAttack attack=CasterAttack.NONE;
        public float defensiveCd,burstTimer,burstAngle;
        public int burstRemaining;
        public boolean comboPending;
    }
    public static final class SpellData {
        public float x, y, vx, vy, life;
    }
    public static final class HeartData {public float x,y;}
    public static final class VialData {
        public float fromX,fromY,toX,toY,elapsed,flight;
        public PotionEffect effect;
        public boolean direct;
    }
    public static final class LightningData {public float x,y,vx,vy,life;}
    public static final class GroundFireData {public float x,y,remaining;}
    public static final class GroundAreaData {
        public float x,y,remaining;
        public PotionEffect effect;
        public boolean summoned;
    }

    private static Json json() {
        Json json = new Json();
        json.setTypeName(null);
        json.setUsePrototypes(false);
        json.setOutputType(JsonWriter.OutputType.json);
        return json;
    }

    public String encode() { validate(); return json().prettyPrint(this); }
    public static SaveData decode(String text) {
        SaveData data = json().fromJson(SaveData.class, text);
        if (data == null) throw new IllegalArgumentException("Save vazio");
        if(data.version==1) {
            data.version=2;
            data.mode=GameMode.CONVENTIONAL;
            data.aimX=data.facing==2?-1:data.facing==3?1:0;
            data.aimY=data.facing==1?1:data.facing==0?-1:0;
        }
        if(data.version==2) {data.version=3;data.stage=1;data.introRemaining=0;data.stageComplete=false;}
        if(data.version==3) {
            if(data.stage==2)data.relocateResidents(new DungeonMap(2));
            data.version=4;
        }
        if(data.version==4) {
            if(data.stage==5)data.relocateResidents(new DungeonMap(5));
            data.version=5;
        }
        if(data.version==5) {
            // Keep pending legacy throws while assigning the new enemy roles to future attacks.
            if(data.enemies!=null)for(EnemyData enemy:data.enemies)if(enemy!=null&&(enemy.windup>0||enemy.actionTime>0)) {
                if(enemy.type==EnemyKind.MAGE.ordinal()){enemy.attack=CasterAttack.POISON_VIAL;enemy.potion=PotionEffect.POISON;}
                else if(enemy.type==EnemyKind.WARLOCK.ordinal()) {
                    enemy.attack=enemy.potion==PotionEffect.RIFT?CasterAttack.RIFT_VIAL:enemy.potion==PotionEffect.FIRE?CasterAttack.FIRE_FAN:CasterAttack.SLOW_VIAL;
                    enemy.potion=enemy.attack.potion();
                }
            }
            data.version=6;
        }
        if(data.version==6) {
            if(data.groundFires==null||data.groundAreas==null||!data.groundAreas.isEmpty())
                throw new IllegalArgumentException("Áreas inválidas no save antigo");
            for(GroundFireData fire:data.groundFires) {
                if(fire==null||!range(fire.remaining,4))throw new IllegalArgumentException("Fogo inválido no save antigo");
                GroundAreaData area=new GroundAreaData();area.x=fire.x;area.y=fire.y;area.remaining=fire.remaining;
                area.effect=PotionEffect.FIRE;data.groundAreas.add(area);
            }
            data.groundFires.clear();data.version=7;
        }
        if(data.version==7) {data.teleportPending=false;data.teleportTargetX=data.teleportTargetY=0;data.version=8;}
        data.validate();
        return data;
    }

    public void validate() {
        DungeonMap map = new DungeonMap(stage);
        int total=map.level.enemyCount()+summonedBats;
        if (version != 8 || !position(map, x, y, 5) || !Float.isFinite(hp)||hp < .5f || hp > 6 || hp*2!=(int)(hp*2) || heals < 0 || heals > 3
                || facing < 0 || facing > 3 || !range(stamina, 100) || !range(mana, 100)
                || !range(elapsed, 31536000) || !range(hitInvuln, 1.4f) || !range(attackCd, 1)
                || !range(magicCd, 1) || !range(healCd, 2) || enemies == null || spells == null
                || defeated < 0 || defeated > total || enemies.size + defeated != total || spells.size > 8
                || summonedBats<0||summonedBats>6||summonedBats%3!=0||riftsCast<0||riftsCast>2||summonedBats>riftsCast*3
                || effects==null||vials==null||vials.size>32||lightning==null||lightning.size>64||groundFires==null||!groundFires.isEmpty()
                ||groundAreas==null||groundAreas.size>64
                || !range(introRemaining,CampaignLevel.INTRO_SECONDS)
                || stageComplete&&(defeated!=total||introRemaining>0||com.badlogic.gdx.math.Vector2.dst2(x,y,map.exitX(),map.exitY())>=18*18)
                || mode==null || (mode==GameMode.REHABILITATION ? simulatedSensors==physicalController : simulatedSensors||physicalController)
                || !unit(aimX,aimY) || !unit(swingX,swingY)
                || !(swingTime==-1||range(swingTime,MeleeSwing.DURATION))
                || !range(castFx,.4f)||!range(healFx,.5f)||hearts==null||hearts.size>total
                || attacks<0||hits<0||hits>attacks||magicCasts<0||healsUsed<0||healsUsed>3
                || heartsCollected<0||heartsCollected>total||hearts.size+heartsCollected>defeated
                || !range(pressureSeconds,31536000)||!range(pressureInBandSeconds,pressureSeconds)
                || (swingTime>=0&&swingImpacted!=(swingTime>=MeleeSwing.IMPACT)))
            throw new IllegalArgumentException("Save inválido ou incompatível");
        if(guidedStrike==null||guidedTargetIndex< -1||guidedTargetIndex>=enemies.size)
            throw new IllegalArgumentException("Invalid guided target");
        guidedStrike.validate();
        if(guidedStrike.active()&&(mode!=GameMode.REHABILITATION||teleportPending||stageComplete||introRemaining>0)
                ||guidedStrike.phase==GuidedStrike.STRIKE&&(swingTime<0||swingImpacted)
                ||guidedStrike.phase==GuidedStrike.APPROACH&&swingTime>=0&&swingTime<MeleeSwing.DURATION
                ||guidedStrike.phase==GuidedStrike.RETREAT&&!swingImpacted)
            throw new IllegalArgumentException("Inconsistent guided strike");
        effects.validate();int livingBats=0;
        if(!range(teleportTargetX,DungeonMap.WIDTH*16)||!range(teleportTargetY,DungeonMap.HEIGHT*16)
                ||teleportPending&&(!position(map,teleportTargetX,teleportTargetY,5)||effects.riftRemaining<=StatusEffects.RIFT_RELEASE_TIME
                ||stageComplete||introRemaining>0))throw new IllegalArgumentException("Transição de fenda inválida no save");
        for (EnemyData enemy : enemies) {
            if (enemy == null || !position(map, enemy.x, enemy.y, 5) || enemy.type < 0 || enemy.type >=EnemyKind.values().length
                    || enemy.hp < 1 || enemy.hp > EnemyKind.of(enemy.type).maxHp || !range(enemy.attackCd, 6)
                    ||!range(enemy.windup,1.5f)||!range(enemy.actionTime,.7f)||!range(enemy.spawnFx,1)
                    ||enemy.cycle<0||enemy.potion==null||enemy.attack==null||!range(enemy.defensiveCd,6)
                    ||enemy.burstRemaining<0||enemy.burstRemaining>5||!range(enemy.burstTimer,.11f)||!range(enemy.burstAngle,360)
                    ||enemy.burstRemaining>0&&(enemy.attack!=CasterAttack.LIGHTNING_FAN||enemy.actionTime<=0)
                    ||(enemy.windup>0||enemy.actionTime>0)&&!position(map,enemy.targetX,enemy.targetY,0)
                    || !Float.isFinite(enemy.kickX)||!Float.isFinite(enemy.kickY)
                    || Math.abs(enemy.kickX)>CombatRules.MAGIC_KNOCKBACK+1||Math.abs(enemy.kickY)>CombatRules.MAGIC_KNOCKBACK+1)
                throw new IllegalArgumentException("Inimigo inválido no save");
            if(enemy.type==EnemyKind.BAT.ordinal())livingBats++;
        }
        if(livingBats>summonedBats)throw new IllegalArgumentException("Morcegos inválidos no save");
        for (SpellData spell : spells) {
            if (spell == null || !position(map, spell.x, spell.y, 0) || !range(spell.life, 1.2f)
                    || !Float.isFinite(spell.vx) || !Float.isFinite(spell.vy)
                    || Math.abs(spell.vx) > 171 || Math.abs(spell.vy) > 171)
                throw new IllegalArgumentException("Magia inválida no save");
        }
        for(HeartData heart:hearts)if(heart==null||!position(map,heart.x,heart.y,0))
            throw new IllegalArgumentException("Coração inválido no save");
        for(VialData vial:vials)if(vial==null||vial.effect==null||!position(map,vial.fromX,vial.fromY,0)||!position(map,vial.toX,vial.toY,0)
                ||!range(vial.flight,1.2f)||vial.flight<.2f||!range(vial.elapsed,vial.flight))
            throw new IllegalArgumentException("Frasco inválido no save");
        for(LightningData bolt:lightning)if(bolt==null||!position(map,bolt.x,bolt.y,0)||!range(bolt.life,1.6f)
                ||!Float.isFinite(bolt.vx)||!Float.isFinite(bolt.vy)||Math.abs(bolt.vx)>151||Math.abs(bolt.vy)>151)
            throw new IllegalArgumentException("Raio inválido no save");
        for(GroundAreaData area:groundAreas) {
            if(area==null||area.effect==null||!position(map,area.x,area.y,0)||!range(area.remaining,GroundArea.DURATION)
                    ||area.summoned&&area.effect!=PotionEffect.RIFT)
                throw new IllegalArgumentException("Área de frasco inválida no save");
        }
    }

    /** Keep old village progress when its former lawn becomes a house or a tree. */
    private void relocateResidents(DungeonMap map) {
        com.badlogic.gdx.math.Vector2 point=villagePosition(map,x,y,5);x=point.x;y=point.y;
        if(enemies!=null)for(EnemyData enemy:enemies)if(enemy!=null) {
            point=villagePosition(map,enemy.x,enemy.y,5);enemy.x=point.x;enemy.y=point.y;
        }
        if(hearts!=null)for(HeartData heart:hearts)if(heart!=null) {
            point=villagePosition(map,heart.x,heart.y,0);heart.x=point.x;heart.y=point.y;
        }
        if(spells!=null)for(int i=spells.size-1;i>=0;i--) {
            SpellData spell=spells.get(i);
            if(spell!=null&&Float.isFinite(spell.x)&&Float.isFinite(spell.y)&&!map.canStand(spell.x,spell.y,0))spells.removeIndex(i);
        }
    }
    private static com.badlogic.gdx.math.Vector2 villagePosition(DungeonMap map,float x,float y,float radius) {
        com.badlogic.gdx.math.Vector2 point=new com.badlogic.gdx.math.Vector2(x,y);
        // Out-of-bounds/invalid data still fails validation; only relocate inside the old clearing.
        if(!Float.isFinite(x)||!Float.isFinite(y)||x<48+radius||x>=832-radius||y<48+radius||y>=512-radius||map.canStand(x,y,radius))return point;
        float best=Float.POSITIVE_INFINITY;
        for(int tx=3;tx<=51;tx++)for(int ty=3;ty<=31;ty++) {
            float cx=tx*16+8,cy=ty*16+8,distance=com.badlogic.gdx.math.Vector2.dst2(x,y,cx,cy);
            if(distance<best&&map.canStand(cx,cy,radius)){point.set(cx,cy);best=distance;}
        }
        return point;
    }

    private static boolean range(float value, float max) { return Float.isFinite(value) && value >= 0 && value <= max; }
    private static boolean unit(float x,float y) {
        return Float.isFinite(x)&&Float.isFinite(y)&&Math.abs(x*x+y*y-1)<.01f;
    }
    private static boolean position(DungeonMap map, float x, float y, float radius) {
        return range(x, DungeonMap.WIDTH * 16) && range(y, DungeonMap.HEIGHT * 16) && map.canStand(x, y, radius);
    }

    public static SaveData read(FileHandle file) {
        if (file.length() > 65536) throw new IllegalArgumentException("Save muito grande");
        return decode(file.readString("UTF-8"));
    }

    public void write(FileHandle file) throws IOException {
        String content = encode();
        file.parent().mkdirs();
        FileHandle temporary = file.sibling(file.name() + ".tmp");
        temporary.writeString(content, false, "UTF-8");
        // Preserve the previous checkpoint until the replacement is fully written.
        try {
            Files.move(temporary.file().toPath(), file.file().toPath(),
                    StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(temporary.file().toPath(), file.file().toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
