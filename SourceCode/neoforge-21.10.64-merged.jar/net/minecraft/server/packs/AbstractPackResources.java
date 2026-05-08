package net.minecraft.server.packs;

import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.DataResult.Error;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import javax.annotation.Nullable;
import net.minecraft.server.packs.metadata.MetadataSectionType;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraft.util.GsonHelper;
import org.slf4j.Logger;

public abstract class AbstractPackResources implements PackResources {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final PackLocationInfo location;

    protected AbstractPackResources(PackLocationInfo location) {
        this.location = location;
    }

    @Nullable
    @Override
    public <T> T getMetadataSection(MetadataSectionType<T> type) throws IOException {
        IoSupplier<InputStream> iosupplier = this.getRootResource(new String[]{"pack.mcmeta"});
        if (iosupplier == null) {
            return null;
        } else {
            Object object;
            try (InputStream inputstream = iosupplier.get()) {
                object = getMetadataFromStream(type, inputstream, this.location);
            }

            return (T)object;
        }
    }

    @Nullable
    public static <T> T getMetadataFromStream(MetadataSectionType<T> type, InputStream stream, PackLocationInfo location) {
        JsonObject jsonobject;
        try (BufferedReader bufferedreader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            jsonobject = GsonHelper.parse(bufferedreader);
        } catch (Exception exception) {
            LOGGER.error("Couldn't load {} {} metadata: {}", location.id(), type.name(), exception.getMessage());
            return null;
        }

        return !jsonobject.has(type.name())
            ? null
            : type.codec()
                .parse(JsonOps.INSTANCE, jsonobject.get(type.name()))
                .ifError(p_432469_ -> LOGGER.error("Couldn't load {} {} metadata: {}", location.id(), type.name(), p_432469_.message()))
                .result()
                .orElse(null);
    }

    @Override
    public PackLocationInfo location() {
        return this.location;
    }

    @Override
    public String toString() {
        return String.format(java.util.Locale.ROOT, "%s: %s", getClass().getName(), location.id());
    }
}
