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
    public int version = 2;
    public float x, y, elapsed, stamina, mana, hitInvuln, attackCd, magicCd, healCd;
    public int hp, heals, facing, defeated;
    public GameMode mode=GameMode.CONVENTIONAL;
    public boolean simulatedSensors;
    public float aimX, aimY=-1, swingTime=-1, swingX, swingY=-1, castFx, healFx;
    public boolean swingImpacted;
    public int attacks, hits, magicCasts, healsUsed, heartsCollected;
    public float pressureSeconds, pressureInBandSeconds;
    public Array<EnemyData> enemies = new Array<>();
    public Array<SpellData> spells = new Array<>();
    public Array<HeartData> hearts = new Array<>();

    public static final class EnemyData {
        public float x, y, attackCd, kickX, kickY;
        public int hp, type;
    }
    public static final class SpellData {
        public float x, y, vx, vy, life;
    }
    public static final class HeartData {public float x,y;}

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
        data.validate();
        return data;
    }

    public void validate() {
        DungeonMap map = new DungeonMap();
        if (version != 2 || !position(map, x, y, 5) || hp < 1 || hp > 6 || heals < 0 || heals > 3
                || facing < 0 || facing > 3 || !range(stamina, 100) || !range(mana, 100)
                || !range(elapsed, 31536000) || !range(hitInvuln, 1) || !range(attackCd, 1)
                || !range(magicCd, 1) || !range(healCd, 2) || enemies == null || spells == null
                || defeated < 0 || defeated > 7 || enemies.size + defeated != 7 || spells.size > 8
                || mode==null || simulatedSensors!=(mode==GameMode.REHABILITATION) || !unit(aimX,aimY) || !unit(swingX,swingY)
                || !(swingTime==-1||range(swingTime,MeleeSwing.DURATION))
                || !range(castFx,.4f)||!range(healFx,.5f)||hearts==null||hearts.size>7
                || attacks<0||hits<0||hits>attacks||magicCasts<0||healsUsed<0||healsUsed>3
                || heartsCollected<0||heartsCollected>7||hearts.size+heartsCollected>defeated
                || !range(pressureSeconds,31536000)||!range(pressureInBandSeconds,pressureSeconds)
                || (swingTime>=0&&swingImpacted!=(swingTime>=MeleeSwing.IMPACT)))
            throw new IllegalArgumentException("Save inválido ou incompatível");
        for (EnemyData enemy : enemies) {
            if (enemy == null || !position(map, enemy.x, enemy.y, 5) || enemy.type < 0 || enemy.type > 1
                    || enemy.hp < 1 || enemy.hp > (enemy.type == 0 ? 2 : 3) || !range(enemy.attackCd, 2)
                    || !Float.isFinite(enemy.kickX)||!Float.isFinite(enemy.kickY)
                    || Math.abs(enemy.kickX)>CombatRules.MAGIC_KNOCKBACK+1||Math.abs(enemy.kickY)>CombatRules.MAGIC_KNOCKBACK+1)
                throw new IllegalArgumentException("Inimigo inválido no save");
        }
        for (SpellData spell : spells) {
            if (spell == null || !position(map, spell.x, spell.y, 0) || !range(spell.life, 1.2f)
                    || !Float.isFinite(spell.vx) || !Float.isFinite(spell.vy)
                    || Math.abs(spell.vx) > 171 || Math.abs(spell.vy) > 171)
                throw new IllegalArgumentException("Magia inválida no save");
        }
        for(HeartData heart:hearts)if(heart==null||!position(map,heart.x,heart.y,0))
            throw new IllegalArgumentException("Coração inválido no save");
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
