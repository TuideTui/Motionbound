package br.edu.motionbound.game;

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
    public int version = 1;
    public float x, y, elapsed, stamina, mana, hitInvuln, attackCd, magicCd, healCd;
    public int hp, heals, facing, defeated;
    public Array<EnemyData> enemies = new Array<>();
    public Array<SpellData> spells = new Array<>();

    public static final class EnemyData {
        public float x, y, attackCd;
        public int hp, type;
    }
    public static final class SpellData {
        public float x, y, vx, vy, life;
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
        data.validate();
        return data;
    }

    public void validate() {
        DungeonMap map = new DungeonMap();
        if (version != 1 || !position(map, x, y, 5) || hp < 1 || hp > 6 || heals < 0 || heals > 3
                || facing < 0 || facing > 3 || !range(stamina, 100) || !range(mana, 100)
                || !range(elapsed, 31536000) || !range(hitInvuln, 1) || !range(attackCd, 1)
                || !range(magicCd, 1) || !range(healCd, 2) || enemies == null || spells == null
                || defeated < 0 || defeated > 7 || enemies.size + defeated != 7 || spells.size > 8)
            throw new IllegalArgumentException("Save inválido ou incompatível");
        for (EnemyData enemy : enemies) {
            if (enemy == null || !position(map, enemy.x, enemy.y, 5) || enemy.type < 0 || enemy.type > 1
                    || enemy.hp < 1 || enemy.hp > (enemy.type == 0 ? 2 : 3) || !range(enemy.attackCd, 2))
                throw new IllegalArgumentException("Inimigo inválido no save");
        }
        for (SpellData spell : spells) {
            if (spell == null || !position(map, spell.x, spell.y, 0) || !range(spell.life, 1.2f)
                    || !Float.isFinite(spell.vx) || !Float.isFinite(spell.vy)
                    || Math.abs(spell.vx) > 171 || Math.abs(spell.vy) > 171)
                throw new IllegalArgumentException("Magia inválida no save");
        }
    }

    private static boolean range(float value, float max) { return Float.isFinite(value) && value >= 0 && value <= max; }
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
