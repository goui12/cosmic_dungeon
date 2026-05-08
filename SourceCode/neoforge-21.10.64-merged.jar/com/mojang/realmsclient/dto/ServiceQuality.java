package com.mojang.realmsclient.dto;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public enum ServiceQuality {
    GREAT(1, "icon/ping_5"),
    GOOD(2, "icon/ping_4"),
    OKAY(3, "icon/ping_3"),
    POOR(4, "icon/ping_2"),
    UNKNOWN(5, "icon/ping_unknown");

    final int value;
    private final ResourceLocation icon;

    private ServiceQuality(int value, String icon) {
        this.value = value;
        this.icon = ResourceLocation.withDefaultNamespace(icon);
    }

    @Nullable
    public static ServiceQuality byValue(int value) {
        for (ServiceQuality servicequality : values()) {
            if (servicequality.getValue() == value) {
                return servicequality;
            }
        }

        return null;
    }

    public int getValue() {
        return this.value;
    }

    public ResourceLocation getIcon() {
        return this.icon;
    }

    @OnlyIn(Dist.CLIENT)
    public static class RealmsServiceQualityJsonAdapter extends TypeAdapter<ServiceQuality> {
        private static final Logger LOGGER = LogUtils.getLogger();

        public void write(JsonWriter writer, ServiceQuality serviceQuality) throws IOException {
            writer.value((long)serviceQuality.value);
        }

        public ServiceQuality read(JsonReader reader) throws IOException {
            int i = reader.nextInt();
            ServiceQuality servicequality = ServiceQuality.byValue(i);
            if (servicequality == null) {
                LOGGER.warn("Unsupported ServiceQuality {}", i);
                return ServiceQuality.UNKNOWN;
            } else {
                return servicequality;
            }
        }
    }
}
