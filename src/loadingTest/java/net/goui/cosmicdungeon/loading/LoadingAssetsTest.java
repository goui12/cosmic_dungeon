package net.goui.cosmicdungeon.loading;

import static org.junit.jupiter.api.Assertions.*;

import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LoadingAssetsTest {
    @TempDir Path config;

    // Test the actual standalone artifact: the gameplay test module deliberately excludes it.
    private URLClassLoader helper() throws Exception {
        return new URLClassLoader(new java.net.URL[] {
                Path.of(System.getProperty("cosmic.loadingJar")).toUri().toURL()
        }, ClassLoader.getPlatformClassLoader());
    }

    private void prepare(URLClassLoader helper) throws Exception {
        helper.loadClass("net.goui.cosmicdungeon.loading.CosmicLoadingAssets")
                .getMethod("prepare", Path.class).invoke(null, config);
    }

    @Test
    void installsBundledAssetsWithoutChangingUnrelatedSettings() throws Exception {
        Path fmlConfig = config.resolve("fml.toml");
        Files.writeString(fmlConfig, "unrelated = true\n");
        try (var helper = helper()) {
            prepare(helper);
            assertEquals("unrelated = true\n", Files.readString(fmlConfig));
            assertTrue(Files.size(config.resolve("fml/theme-cosmicdungeon.json")) > 0);
            try (var expected = helper.getResourceAsStream("cosmic-loading/cd_minecraft.png")) {
                assertNotNull(expected);
                assertArrayEquals(expected.readAllBytes(), Files.readAllBytes(config.resolve("fml/cosmicdungeon/cd_minecraft.png")));
            }
        }
    }

    @Test
    void upgradesOwnedAssetsAndLeavesUnrelatedThemeFilesAlone() throws Exception {
        try (var helper = helper()) {
            prepare(helper);
            Path background = config.resolve("fml/cosmicdungeon/cd_loading_background.png");
            byte[] expected = Files.readAllBytes(background);
            Path unrelated = config.resolve("fml/another-theme.json");
            Files.writeString(unrelated, "keep this");
            Files.writeString(background, "previous version");
            prepare(helper);
            assertArrayEquals(expected, Files.readAllBytes(background));
            assertEquals("keep this", Files.readString(unrelated));
            var timestamp = Files.getLastModifiedTime(background);
            prepare(helper);
            assertEquals(timestamp, Files.getLastModifiedTime(background));
        }
    }
}
