package net.goui.cosmicdungeon.client.screen;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import net.minecraft.resources.ResourceLocation;

/**
 * Packaged artwork contract for the Batch19 help renderer. Loading this catalog
 * reads metadata only; it never decodes, uploads or preloads a texture.
 * Page content and availability remain exclusively in HelpMenuContent.
 */
public final class HelpMenuArtwork {
    public static final int WIDTH = 480;
    public static final int HEIGHT = 270;
    /** Apply underneath text after drawing the background, never over glyphs/emblems. */
    public static final int TEXT_SCRIM = 0xC0000000;
    private static final Catalog CATALOG = load();

    private HelpMenuArtwork() {}

    public static Theme forPage(String pageId) {
        return CATALOG.themes().get(CATALOG.pages().getOrDefault(
                pageId == null ? "" : pageId, CATALOG.fallback()));
    }

    public static Map<String, String> pageThemes() { return CATALOG.pages(); }
    public static Map<String, Theme> themes() { return CATALOG.themes(); }

    /**
     * Cover the target without stretching. UVs are normalized for any resolution
     * override of the same16:9 texture; clipped detail is decorative, never text.
     */
    public static Crop crop(int targetWidth, int targetHeight) {
        if (targetWidth <= 0 || targetHeight <= 0)
            throw new IllegalArgumentException("Artwork target must have positive dimensions");
        double targetAspect = (double) targetWidth / targetHeight;
        double sourceAspect = (double) WIDTH / HEIGHT;
        double width = Math.min(1.0, targetAspect / sourceAspect);
        double height = Math.min(1.0, sourceAspect / targetAspect);
        return new Crop((1.0 - width) / 2.0, (1.0 - height) / 2.0, width, height);
    }

    public record Crop(double u, double v, double width, double height) {}
    public record Theme(String id, ResourceLocation texture, ResourceLocation emblem) {}
    private record Catalog(String fallback, Map<String, Theme> themes, Map<String, String> pages) {}

    private static Catalog load() {
        try (var input = Objects.requireNonNull(HelpMenuArtwork.class.getResourceAsStream(
                "/assets/cosmicdungeon/help_artwork.json"), "Missing help artwork manifest");
             var reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            if (json.get("schema_version").getAsInt() != 1
                    || json.get("width").getAsInt() != WIDTH
                    || json.get("height").getAsInt() != HEIGHT
                    || !"center_cover".equals(json.get("crop").getAsString())
                    || Integer.parseUnsignedInt(json.get("text_scrim_argb").getAsString(), 16) != TEXT_SCRIM)
                throw new IllegalStateException("Unsupported help artwork contract");
            Map<String, Theme> themes = new LinkedHashMap<>();
            json.getAsJsonObject("themes").entrySet().forEach(entry -> {
                var value = entry.getValue().getAsJsonObject();
                String path = value.get("texture").getAsString();
                if (!path.equals("textures/gui/help/" + entry.getKey() + ".png"))
                    throw new IllegalStateException("Unexpected help texture path: " + path);
                var emblem = value.get("emblem");
                themes.put(entry.getKey(), new Theme(entry.getKey(), asset(path),
                        emblem == null || emblem.isJsonNull() ? null : asset(emblem.getAsString())));
            });
            String fallback = json.get("fallback").getAsString();
            if (!themes.containsKey(fallback)) throw new IllegalStateException("Missing fallback theme");
            Map<String, String> pages = new LinkedHashMap<>();
            json.getAsJsonObject("pages").entrySet().forEach(entry -> {
                String theme = entry.getValue().getAsString();
                if (!themes.containsKey(theme)) throw new IllegalStateException("Unknown theme: " + theme);
                pages.put(entry.getKey(), theme);
            });
            return new Catalog(fallback, Collections.unmodifiableMap(themes), Collections.unmodifiableMap(pages));
        } catch (java.io.IOException exception) {
            throw new IllegalStateException("Cannot load help artwork manifest", exception);
        }
    }

    private static ResourceLocation asset(String path) {
        return ResourceLocation.fromNamespaceAndPath("cosmicdungeon", path);
    }
}
