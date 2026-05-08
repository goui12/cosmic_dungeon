package net.minecraft.server.players;

import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.UUIDUtil;

public record NameAndId(UUID id, String name) {
    public static final Codec<NameAndId> CODEC = RecordCodecBuilder.create(
        p_443190_ -> p_443190_.group(UUIDUtil.STRING_CODEC.fieldOf("id").forGetter(NameAndId::id), Codec.STRING.fieldOf("name").forGetter(NameAndId::name))
            .apply(p_443190_, NameAndId::new)
    );

    public NameAndId(GameProfile p_433336_) {
        this(p_433336_.id(), p_433336_.name());
    }

    public NameAndId(com.mojang.authlib.yggdrasil.response.NameAndId p_442601_) {
        this(p_442601_.id(), p_442601_.name());
    }

    @Nullable
    public static NameAndId fromJson(JsonObject json) {
        if (json.has("uuid") && json.has("name")) {
            String s = json.get("uuid").getAsString();

            UUID uuid;
            try {
                uuid = UUID.fromString(s);
            } catch (Throwable throwable) {
                return null;
            }

            return new NameAndId(uuid, json.get("name").getAsString());
        } else {
            return null;
        }
    }

    public void appendTo(JsonObject json) {
        json.addProperty("uuid", this.id().toString());
        json.addProperty("name", this.name());
    }

    public static NameAndId createOffline(String username) {
        UUID uuid = UUIDUtil.createOfflinePlayerUUID(username);
        return new NameAndId(uuid, username);
    }
}
