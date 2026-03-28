// file: src/main/java/net/goui/cosmicdungeon/network/payload/SpawnerLabelPayload.java
package net.goui.cosmicdungeon.network.payload;

import net.goui.cosmicdungeon.CosmicDungeonMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record SpawnerLabelPayload(boolean enabled) implements CustomPacketPayload {

    public static final Type<SpawnerLabelPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(CosmicDungeonMod.MOD_ID, "spawner_label"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SpawnerLabelPayload> STREAM_CODEC =
            StreamCodec.of(SpawnerLabelPayload::encode, SpawnerLabelPayload::decode);

    private static void encode(RegistryFriendlyByteBuf buf, SpawnerLabelPayload p) {
        buf.writeBoolean(p.enabled());
    }

    private static SpawnerLabelPayload decode(RegistryFriendlyByteBuf buf) {
        return new SpawnerLabelPayload(buf.readBoolean());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}