package net.goui.cosmicdungeon.client.branding;

import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import javax.imageio.ImageIO;
import net.neoforged.fml.earlydisplay.theme.Theme;
import net.neoforged.fml.earlydisplay.theme.ThemeLoader;
import net.neoforged.fml.earlydisplay.theme.elements.ThemeImageElement;

/** Offline packaging and real FML theme-parser checks. Never creates a Minecraft client or GL context. */
public final class MenuBrandingChecks {
    private int checks;

    public static void main(String[] args) throws Exception {
        new MenuBrandingChecks().run(Path.of(args[0]), Path.of(args[1]));
    }

    private void run(Path root, Path configDirectory) throws Exception {
        Path authored = root.resolve("src/main/resources/assets/cosmicdungeon");
        Path packaged = root.resolve("build/resources/main/assets");
        for (int face = 0; face < 6; face++) {
            String name = "textures/gui/title/background/panorama_" + face + ".png";
            require(Arrays.equals(Files.readAllBytes(authored.resolve(name)),
                    Files.readAllBytes(packaged.resolve("minecraft").resolve(name))), "Panorama alias " + face);
            var image = ImageIO.read(authored.resolve(name).toFile());
            require(image.getWidth() == 1024 && image.getHeight() == 1024, "Panorama dimensions " + face);
        }
        require(Arrays.equals(Files.readAllBytes(authored.resolve("texts/splashes.txt")),
                Files.readAllBytes(packaged.resolve("minecraft/texts/splashes.txt"))), "Authored splash alias");

        var soundRoot = JsonParser.parseString(Files.readString(packaged.resolve("minecraft/sounds.json"))).getAsJsonObject();
        require(soundRoot.size() == 1 && soundRoot.has("music.menu"), "Only menu music is overridden");
        var menu = soundRoot.getAsJsonObject("music.menu");
        require(menu.get("replace").getAsBoolean(), "Vanilla menu playlist is replaced");
        var sounds = menu.getAsJsonArray("sounds");
        require(sounds.size() == 1, "One authored menu track");
        var sound = sounds.get(0).getAsJsonObject();
        require(sound.get("stream").getAsBoolean(), "Soundtrack is streamed");
        require(sound.get("name").getAsString().equals("cosmicdungeon:music/cd_menu_theme"), "Soundtrack resource");
        require(Files.size(packaged.resolve("cosmicdungeon/sounds/music/cd_menu_theme.ogg")) > 0, "Soundtrack packaged");

        Path themeDirectory = configDirectory.resolve("fml");
        Theme theme = ThemeLoader.load(themeDirectory, "cosmicdungeon");
        require(!theme.loadingScreen().mojangLogo().visible(), "Default logo hidden");
        require(theme.windowIcon().path().equals("neoforged_icon.png"), "NeoForge window icon retained");
        require(theme.loadingScreen().decoration().get("fox").visible(), "NeoForge fox retained");
        require(theme.loadingScreen().decoration().get("version").visible(), "NeoForge version retained");
        require(theme.loadingScreen().decoration().get("neoforgeCredit").visible(), "NeoForge credit retained");
        var title = (ThemeImageElement) theme.loadingScreen().decoration().get("cosmicTitle");
        require(title.visible() && title.centerHorizontally(), "Custom title visible and centered");
        require(Arrays.equals(Files.readAllBytes(authored.resolve("textures/gui/title/cd_minecraft.png")),
                Files.readAllBytes(themeDirectory.resolve(title.texture().resource().path()))), "Early title copy");
        require(Arrays.equals(Files.readAllBytes(authored.resolve("textures/gui/loading/cd_progress_bar_bg.png")),
                Files.readAllBytes(themeDirectory.resolve(theme.sprites().progressBarBackground().resource().path()))), "Early background bar");
        require(Arrays.equals(Files.readAllBytes(authored.resolve("textures/gui/loading/cd_progress_bar_fg.png")),
                Files.readAllBytes(themeDirectory.resolve(theme.sprites().progressBarForeground().resource().path()))), "Early foreground bar");
        require(Files.readString(configDirectory.resolve("fml.toml")).contains("earlyLoadingScreenTheme = \"cosmicdungeon\""),
                "Early theme selected");
        System.out.println("Menu branding: " + checks + " offline checks passed; no client/server was launched.");
    }

    private void require(boolean condition, String description) {
        if (!condition) {
            throw new AssertionError(description);
        }
        checks++;
    }
}
