package br.edu.motionbound.game;

import com.badlogic.gdx.files.FileHandle;
import java.nio.file.Files;
import java.nio.file.Path;

/** Offline regression checks; no test framework or graphics context required. */
public final class SaveDataTest {
    public static void main(String[] args) throws Exception {
        SaveData data = checkpoint();
        SaveData loaded = SaveData.decode(data.encode());
        require(loaded.hp == 4 && loaded.heals == 2 && loaded.defeated == 6, "Player state");
        require(loaded.elapsed == 135.5f && loaded.mana == 75 && loaded.stamina == 36, "Resources and time");
        require(loaded.enemies.size == 1 && loaded.enemies.first().hp == 1, "Enemy state");
        require(loaded.enemies.first().kickX==CombatRules.MAGIC_KNOCKBACK,"Magic knockback survives saving");
        require(loaded.spells.size == 1 && loaded.spells.first().vy == 170, "Active projectile");
        require(loaded.magicCd == .5f && loaded.hitInvuln == .4f, "Combat timers");
        require(loaded.version==7&&loaded.hearts.size==1&&loaded.heartsCollected==1,"Heart drops persisted");
        require(loaded.swingTime==.13f&&!loaded.swingImpacted&&loaded.attacks==5&&loaded.hits==3,"In-progress swing persisted");
        SaveData legacy=SaveData.decode("{\"version\":1,\"x\":120,\"y\":136,\"hp\":4,\"heals\":2,\"facing\":1,\"defeated\":6,\"enemies\":[{\"x\":216,\"y\":200,\"hp\":1,\"type\":0}],\"spells\":[]}");
        require(legacy.version==7&&legacy.stage==1&&legacy.aimY==1&&legacy.hearts.isEmpty(),"Version one migration");
        SaveData versionTwo=SaveData.decode(data.encode().replace("\"version\": 7","\"version\": 2"));
        require(versionTwo.version==7&&versionTwo.stage==1&&versionTwo.introRemaining==0,"Version two migration keeps the original stage");
        villageMigration();
        fortressMigration();
        casterCheckpoint();
        groundAreaCheckpoint();
        data.hp=4.5f;data.effects.apply(PotionEffect.POISON);data.effects.advance(.4f);
        SaveData poisoned=SaveData.decode(data.encode());
        require(poisoned.hp==4.5f&&poisoned.effects.ticksRemaining==4&&poisoned.effects.damageEffect==PotionEffect.POISON,"Half hearts and afflictions round-trip");
        data.hp=4;data.effects.cleanseDamage();

        reject("null"); reject("{}"); reject("{version:3}"); reject("{hp:4,class:java.lang.Runtime}");
        data.version = 9; invalid(data); data.version = 7;
        data.stage=6;invalid(data);data.stage=1;
        data.introRemaining=Float.NaN;invalid(data);data.introRemaining=0;
        data.x = Float.NaN; invalid(data); data.x = 120;
        data.hp = 0; invalid(data); data.hp = 4;
        data.defeated = 5; invalid(data); data.defeated = 6;
        data.enemies.first().x = 0; invalid(data); data.enemies.first().x = 216;
        data.enemies.first().kickX=CombatRules.MAGIC_KNOCKBACK+2;invalid(data);data.enemies.first().kickX=CombatRules.MAGIC_KNOCKBACK;
        data.spells.first().vx = Float.POSITIVE_INFINITY; invalid(data); data.spells.first().vx = 0;
        data.swingImpacted=true;invalid(data);data.swingImpacted=false;
        data.pressureInBandSeconds=3;invalid(data);data.pressureInBandSeconds=1;
        data.mode=br.edu.motionbound.input.GameMode.REHABILITATION;invalid(data);
        data.simulatedSensors=true;
        require(SaveData.decode(data.encode()).simulatedSensors,"Synthetic sensor data must be explicitly identified");
        data.mode=br.edu.motionbound.input.GameMode.CONVENTIONAL;data.simulatedSensors=false;

        Path directory = Files.createTempDirectory(Path.of(args[0]), "save-test-");
        FileHandle file = new FileHandle(directory.resolve("expedition.json").toFile());
        data.write(file);
        require(SaveData.read(file).hp == 4, "First checkpoint written");
        data.hp = 3; data.write(file);
        require(SaveData.read(file).hp == 3, "Existing checkpoint replaced");
        data.hp = 0;
        try { data.write(file); throw new AssertionError("Invalid write accepted"); }
        catch (IllegalArgumentException expected) { }
        require(SaveData.read(file).hp == 3, "Last good checkpoint preserved");
        Files.delete(file.file().toPath()); Files.delete(directory);
        System.out.println("PASS: save round-trip, combat state, invalid data and safe replacement");
    }
    private static void fortressMigration() {
        SaveData old=new SaveData();old.version=4;old.stage=5;old.hp=5;old.heals=2;old.mana=43;old.defeated=21;
        old.x=30*16+8;old.y=6*16+8;
        SaveData.EnemyData enemy=new SaveData.EnemyData();enemy.x=10*16+8;enemy.y=20*16+8;enemy.type=1;enemy.hp=3;old.enemies.add(enemy);
        com.badlogic.gdx.utils.Json json=new com.badlogic.gdx.utils.Json();json.setTypeName(null);json.setUsePrototypes(false);
        SaveData migrated=SaveData.decode(json.toJson(old));DungeonMap map=new DungeonMap(5);
        require(migrated.version==7&&migrated.stage==5&&migrated.defeated==21&&migrated.hp==5&&migrated.mana==43,"Old fortress retains campaign progress");
        require(map.canStand(migrated.x,migrated.y,5),"Old fortress position is relocated out of new ruins");
    }
    private static void casterCheckpoint() {
        SaveData data=checkpoint();SaveData.EnemyData mage=data.enemies.first();
        mage.type=EnemyKind.MAGE.ordinal();mage.attack=CasterAttack.LIGHTNING_FAN;
        mage.actionTime=.45f;mage.burstRemaining=3;mage.burstAngle=180;mage.burstTimer=.08f;
        mage.targetX=120;mage.targetY=136;mage.defensiveCd=2;mage.comboPending=true;
        SaveData.LightningData bolt=new SaveData.LightningData();bolt.x=120;bolt.y=150;bolt.vx=150;bolt.life=1.2f;data.lightning.add(bolt);
        SaveData.GroundAreaData fire=new SaveData.GroundAreaData();fire.x=120;fire.y=136;fire.remaining=3.2f;fire.effect=PotionEffect.FIRE;data.groundAreas.add(fire);
        SaveData.VialData vial=new SaveData.VialData();vial.fromX=120;vial.fromY=136;vial.toX=130;vial.toY=136;
        vial.flight=.28f;vial.elapsed=.12f;vial.effect=PotionEffect.SLOW;vial.direct=true;data.vials.add(vial);
        SaveData restored=SaveData.decode(data.encode());
        require(restored.enemies.first().burstRemaining==3&&restored.enemies.first().burstAngle==180
                &&restored.enemies.first().comboPending&&restored.enemies.first().defensiveCd==2,"Burst and combo checkpoints round-trip");
        require(restored.lightning.first().vx==150&&restored.groundAreas.first().remaining==3.2f
                &&restored.vials.first().direct&&restored.vials.first().flight==.28f,"Bolts, persistent fire and direct grey vial round-trip");
        fire.remaining=5.1f;invalid(data);fire.remaining=3.2f;
        bolt.vx=Float.NaN;invalid(data);bolt.vx=150;
        mage.burstRemaining=6;invalid(data);mage.burstRemaining=3;
        mage.burstTimer=Float.NaN;invalid(data);mage.burstTimer=.08f;
        vial.flight=.1f;invalid(data);vial.flight=.28f;
        // A version-five checkpoint can be continued without losing progress or an in-flight throw.
        data=checkpoint();data.version=5;mage=data.enemies.first();mage.type=EnemyKind.MAGE.ordinal();
        mage.windup=.4f;mage.targetX=120;mage.targetY=136;mage.potion=PotionEffect.FIRE;
        com.badlogic.gdx.utils.Json json=new com.badlogic.gdx.utils.Json();json.setTypeName(null);json.setUsePrototypes(false);
        restored=SaveData.decode(json.toJson(data));
        require(restored.version==7&&restored.defeated==6&&restored.hp==4
                &&restored.enemies.first().attack==CasterAttack.POISON_VIAL&&restored.enemies.first().potion==PotionEffect.POISON,
                "Version-five pending mage cast migrates to green while preserving progress");
    }
    private static void villageMigration() {
        SaveData old=new SaveData();old.version=3;old.stage=2;old.x=16*16+8;old.y=26*16+8;
        old.hp=4;old.heals=2;old.mana=37;old.stamina=65;old.elapsed=92;old.defeated=1;old.heartsCollected=0;
        for(int i=0;i<9;i++) {
            SaveData.EnemyData enemy=new SaveData.EnemyData();enemy.x=8*16+8;enemy.y=28*16+8;enemy.hp=2;old.enemies.add(enemy);
        }
        SaveData.HeartData heart=new SaveData.HeartData();heart.x=old.x;heart.y=old.y;old.hearts.add(heart);
        com.badlogic.gdx.utils.Json json=new com.badlogic.gdx.utils.Json();json.setTypeName(null);json.setUsePrototypes(false);
        SaveData migrated=SaveData.decode(json.toJson(old));DungeonMap map=new DungeonMap(2);
        require(migrated.version==7&&migrated.stage==2&&migrated.hp==4&&migrated.defeated==1&&migrated.enemies.size==9,"Old village retains progress");
        require(map.canStand(migrated.x,migrated.y,5)&&map.canStand(migrated.enemies.first().x,migrated.enemies.first().y,5)
                &&map.canStand(migrated.hearts.first().x,migrated.hearts.first().y,0),"Remodelled village relocates blocked save positions");
        require(migrated.mana==37&&migrated.elapsed==92&&migrated.heals==2,"Village migration preserves resources and time");
        require(SaveData.decode(migrated.encode()).x==migrated.x,"Village migration runs only once");
    }
    private static void groundAreaCheckpoint() {
        SaveData data=checkpoint();
        for(PotionEffect effect:PotionEffect.values()) {
            SaveData.GroundAreaData area=new SaveData.GroundAreaData();area.x=120;area.y=136;area.remaining=4.8f;
            area.effect=effect;data.groundAreas.add(area);
        }
        SaveData restored=SaveData.decode(data.encode());
        require(restored.groundAreas.size==4&&restored.groundAreas.get(1).effect==PotionEffect.POISON
                &&restored.groundAreas.first().remaining==4.8f,"All four persistent area types round-trip");
        data.groundAreas.first().effect=null;invalid(data);data.groundAreas.first().effect=PotionEffect.FIRE;
        data.groundAreas.first().summoned=true;invalid(data);data.groundAreas.first().summoned=false;
        data.groundAreas.first().remaining=5.1f;invalid(data);
        data=checkpoint();data.version=6;
        SaveData.GroundFireData oldFire=new SaveData.GroundFireData();oldFire.x=120;oldFire.y=136;oldFire.remaining=2.3f;data.groundFires.add(oldFire);
        com.badlogic.gdx.utils.Json json=new com.badlogic.gdx.utils.Json();json.setTypeName(null);json.setUsePrototypes(false);
        restored=SaveData.decode(json.toJson(data));
        require(restored.version==7&&restored.groundFires.isEmpty()&&restored.groundAreas.size==1
                &&restored.groundAreas.first().effect==PotionEffect.FIRE&&restored.groundAreas.first().remaining==2.3f
                &&restored.hp==4&&restored.defeated==6,"Version-six flames migrate without restarting their lifetime or losing progress");
        oldFire.remaining=4.1f;reject(json.toJson(data));
    }

    private static SaveData checkpoint() {
        SaveData data = new SaveData();
        data.x = 120; data.y = 136; data.hp = 4; data.heals = 2; data.facing = 1;
        data.elapsed = 135.5f; data.stamina = 36; data.mana = 75; data.defeated = 6;
        data.hitInvuln = .4f; data.magicCd = .5f;
        data.swingTime=.13f;data.attacks=5;data.hits=3;data.pressureSeconds=2;data.pressureInBandSeconds=1;
        data.heartsCollected=1;
        SaveData.HeartData heart=new SaveData.HeartData();heart.x=130;heart.y=140;data.hearts.add(heart);
        SaveData.EnemyData enemy = new SaveData.EnemyData();
        enemy.x = 216; enemy.y = 200; enemy.type = 0; enemy.hp = 1; enemy.attackCd = .2f;
        enemy.kickX=CombatRules.MAGIC_KNOCKBACK;
        data.enemies.add(enemy);
        SaveData.SpellData spell = new SaveData.SpellData();
        spell.x = 120; spell.y = 150; spell.vy = 170; spell.life = .8f;
        data.spells.add(spell);
        return data;
    }
    private static void require(boolean valid, String label) { if (!valid) throw new AssertionError(label); }
    private static void invalid(SaveData data) {
        try { data.validate(); throw new AssertionError("Invalid state accepted"); }
        catch (IllegalArgumentException expected) { }
    }
    private static void reject(String text) {
        try { SaveData.decode(text); throw new AssertionError("Invalid JSON accepted: " + text); }
        catch (RuntimeException expected) { }
    }
}
