package net.minecraft;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.UUID;
import net.minecraft.server.packs.metadata.pack.PackFormat;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.level.storage.DataVersion;
import org.slf4j.Logger;

public class DetectedVersion {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final WorldVersion BUILT_IN = createBuiltIn(UUID.randomUUID().toString().replaceAll("-", ""), "Development Version");

    public static WorldVersion createBuiltIn(String id, String name) {
        return createBuiltIn(id, name, true);
    }

    public static WorldVersion createBuiltIn(String id, String name, boolean stable) {
        return new WorldVersion.Simple(
            id,
            name,
            new DataVersion(4556, "main"),
            SharedConstants.getProtocolVersion(),
            PackFormat.of(69, 0),
            PackFormat.of(88, 0),
            new Date(),
            stable
        );
    }

    private static WorldVersion createFromJson(JsonObject json) {
        JsonObject jsonobject = GsonHelper.getAsJsonObject(json, "pack_version");
        return new WorldVersion.Simple(
            GsonHelper.getAsString(json, "id"),
            GsonHelper.getAsString(json, "name"),
            new DataVersion(GsonHelper.getAsInt(json, "world_version"), GsonHelper.getAsString(json, "series_id", "main")),
            GsonHelper.getAsInt(json, "protocol_version"),
            PackFormat.of(GsonHelper.getAsInt(jsonobject, "resource_major"), GsonHelper.getAsInt(jsonobject, "resource_minor")),
            PackFormat.of(GsonHelper.getAsInt(jsonobject, "data_major"), GsonHelper.getAsInt(jsonobject, "data_minor")),
            Date.from(ZonedDateTime.parse(GsonHelper.getAsString(json, "build_time")).toInstant()),
            GsonHelper.getAsBoolean(json, "stable")
        );
    }

    public static WorldVersion tryDetectVersion() {
        try {
            WorldVersion worldversion;
            try (InputStream inputstream = DetectedVersion.class.getResourceAsStream("/version.json")) {
                if (inputstream == null) {
                    LOGGER.warn("Missing version information!");
                    return BUILT_IN;
                }

                try (InputStreamReader inputstreamreader = new InputStreamReader(inputstream)) {
                    worldversion = createFromJson(GsonHelper.parse(inputstreamreader));
                }
            }

            return worldversion;
        } catch (JsonParseException | IOException ioexception) {
            throw new IllegalStateException("Game version information is corrupt", ioexception);
        }
    }
}
