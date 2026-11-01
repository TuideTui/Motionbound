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
        require(loaded.spells.size == 1 && loaded.spells.first().vy == 170, "Active projectile");
        require(loaded.magicCd == .5f && loaded.hitInvuln == .4f, "Combat timers");

        reject("null"); reject("{}"); reject("{version:2}"); reject("{hp:4,class:java.lang.Runtime}");
        data.version = 9; invalid(data); data.version = 1;
        data.x = Float.NaN; invalid(data); data.x = 120;
        data.hp = 0; invalid(data); data.hp = 4;
        data.defeated = 5; invalid(data); data.defeated = 6;
        data.enemies.first().x = 0; invalid(data); data.enemies.first().x = 216;
        data.spells.first().vx = Float.POSITIVE_INFINITY; invalid(data); data.spells.first().vx = 0;

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

    private static SaveData checkpoint() {
        SaveData data = new SaveData();
        data.x = 120; data.y = 136; data.hp = 4; data.heals = 2; data.facing = 1;
        data.elapsed = 135.5f; data.stamina = 36; data.mana = 75; data.defeated = 6;
        data.hitInvuln = .4f; data.magicCd = .5f;
        SaveData.EnemyData enemy = new SaveData.EnemyData();
        enemy.x = 216; enemy.y = 200; enemy.type = 0; enemy.hp = 1; enemy.attackCd = .2f;
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
