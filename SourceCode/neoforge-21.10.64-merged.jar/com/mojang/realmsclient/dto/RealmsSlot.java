package com.mojang.realmsclient.dto;

import com.google.gson.TypeAdapter;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class RealmsSlot implements ReflectionBasedSerialization {
    @SerializedName("slotId")
    public int slotId;
    @SerializedName("options")
    @JsonAdapter(RealmsSlot.RealmsWorldOptionsJsonAdapter.class)
    public RealmsWorldOptions options;
    @SerializedName("settings")
    public List<RealmsSetting> settings;

    public RealmsSlot(int slotId, RealmsWorldOptions options, List<RealmsSetting> settings) {
        this.slotId = slotId;
        this.options = options;
        this.settings = settings;
    }

    public static RealmsSlot defaults(int slotId) {
        return new RealmsSlot(slotId, RealmsWorldOptions.createEmptyDefaults(), List.of(RealmsSetting.hardcoreSetting(false)));
    }

    public RealmsSlot clone() {
        return new RealmsSlot(this.slotId, this.options.clone(), new ArrayList<>(this.settings));
    }

    public boolean isHardcore() {
        return RealmsSetting.isHardcore(this.settings);
    }

    @OnlyIn(Dist.CLIENT)
    static class RealmsWorldOptionsJsonAdapter extends TypeAdapter<RealmsWorldOptions> {
        private RealmsWorldOptionsJsonAdapter() {
        }

        public void write(JsonWriter writer, RealmsWorldOptions options) throws IOException {
            writer.jsonValue(new GuardedSerializer().toJson(options));
        }

        public RealmsWorldOptions read(JsonReader reader) throws IOException {
            String s = reader.nextString();
            return RealmsWorldOptions.parse(new GuardedSerializer(), s);
        }
    }
}
