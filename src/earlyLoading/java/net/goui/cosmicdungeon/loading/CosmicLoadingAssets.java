package net.goui.cosmicdungeon.loading;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Map;

/** Version-matched startup assets, available before normal mod resource discovery. */
public final class CosmicLoadingAssets {
    private static final Map<String, String> FILES = Map.of(
            "theme-cosmicdungeon.json", "theme-cosmicdungeon.json",
            "cd_minecraft.png", "cosmicdungeon/cd_minecraft.png",
            "cd_loading_background.png", "cosmicdungeon/cd_loading_background.png",
            "cd_progress_bar_bg.png", "cosmicdungeon/cd_progress_bar_bg.png",
            "cd_progress_bar_fg.png", "cosmicdungeon/cd_progress_bar_fg.png");

    private CosmicLoadingAssets() {}

    public static void prepare(Path configDirectory) throws IOException {
        Path themes = configDirectory.resolve("fml");
        for (var file : FILES.entrySet()) {
            byte[] contents;
            try (var input = CosmicLoadingAssets.class.getResourceAsStream("/cosmic-loading/" + file.getKey())) {
                if (input == null) {
                    throw new IOException("Missing bundled loading asset: " + file.getKey());
                }
                contents = input.readAllBytes();
            }
            Path target = themes.resolve(file.getValue());
            Files.createDirectories(target.getParent());
            if (Files.isRegularFile(target) && Arrays.equals(Files.readAllBytes(target), contents)) {
                continue;
            }
            Path temporary = Files.createTempFile(target.getParent(), ".cosmic-loading-", ".tmp");
            try {
                Files.write(temporary, contents);
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
            } finally {
                Files.deleteIfExists(temporary);
            }
        }
    }
}
