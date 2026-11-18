package net.goui.cosmicdungeon.client.screen;

import java.io.IOException;
import java.nio.file.*;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import net.neoforged.fml.loading.FMLPaths;

public final class GroupSkillPreferences {
    private static class Client {
        static final GroupSkillPreferences INSTANCE = new GroupSkillPreferences(
                FMLPaths.CONFIGDIR.get().resolve("cosmicdungeon-group-skills.properties"));
    }

    public static GroupSkillPreferences client() { return Client.INSTANCE; }
    private final Path path;
    private final Properties saved = new Properties();
    private final Set<String> temporary = new HashSet<>();
    private boolean writable = true;
    private long run;

    GroupSkillPreferences(Path path) {
        this.path = path;
        if (Files.exists(path)) {
            try (var reader = Files.newBufferedReader(path)) { saved.load(reader); }
            catch (IOException | IllegalArgumentException invalid) {
                writable = false;
                com.mojang.logging.LogUtils.getLogger().warn("Could not read group skill preferences", invalid);
            }
        }
    }

    public boolean defaultCollapsed() { return Boolean.parseBoolean(saved.getProperty("defaultCollapseSkills")); }
    public void setDefaultCollapsed(boolean value) {
        if (value == defaultCollapsed()) return;
        saved.setProperty("defaultCollapseSkills", Boolean.toString(value));
        temporary.clear();
        persist();
    }
    void enterRun(long next) {
        if (next != run) {
            run = next;
            temporary.clear();
        }
    }
    boolean collapsed(String key, boolean human) {
        if (defaultCollapsed()) return !temporary.contains(key);
        return human ? saved.containsKey("player." + key) : temporary.contains(key);
    }
    void toggle(String key, boolean human) {
        if (defaultCollapsed() || !human) {
            if (!temporary.add(key)) temporary.remove(key);
        } else {
            if (collapsed(key, true)) saved.remove("player." + key);
            else saved.setProperty("player." + key, "true");
            persist();
        }
    }
    private void persist() {
        if (!writable) return;
        try {
            Files.createDirectories(path.getParent());
            var temp = Files.createTempFile(path.getParent(), "group-skills-", ".tmp");
            try {
                try (var writer = Files.newBufferedWriter(temp)) { saved.store(writer, "Group skill preferences"); }
                try { Files.move(temp, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
                catch (AtomicMoveNotSupportedException unsupported) { Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING); }
            } finally { Files.deleteIfExists(temp); }
        } catch (IOException failure) { com.mojang.logging.LogUtils.getLogger().warn("Could not save group skills", failure); }
    }
}
