package net.goui.cosmicdungeon.client.screen;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class GroupSkillPreferencesTest {
    @Test void overrideRestoresHumanChoicesAndMercenariesResetEachRun(@TempDir Path dir) {
        var path = dir.resolve("group.properties");
        var prefs = new GroupSkillPreferences(path);
        prefs.enterRun(8);
        String toxxulia = "25ef538b-6d24-40ed-9340-f108bc2e067c";
        String miche = "cbb74ae7-baf2-4c4e-944f-4c3ac89b54f4";
        assertFalse(prefs.collapsed(toxxulia, true));
        prefs.toggle(toxxulia, true);
        prefs.toggle("merc", false);
        prefs.enterRun(8);
        assertTrue(prefs.collapsed("merc", false));
        prefs.enterRun(9);
        assertFalse(prefs.collapsed("merc", false));
        prefs.setDefaultCollapsed(true);
        assertTrue(prefs.collapsed(miche, true));
        prefs.toggle(toxxulia, true);
        assertFalse(prefs.collapsed(toxxulia, true));
        prefs.setDefaultCollapsed(false);
        assertTrue(prefs.collapsed(toxxulia, true));
        assertFalse(prefs.collapsed(miche, true));
        var loaded = new GroupSkillPreferences(path);
        assertTrue(loaded.collapsed(toxxulia, true));
        assertFalse(loaded.collapsed(miche, true));
        assertFalse(loaded.collapsed("merc", false));
    }
}
