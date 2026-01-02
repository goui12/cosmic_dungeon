// file: src/main/java/net/goui/cosmicdungeon/network/payload/RegionLookAllRequestPayload.java
package net.goui.cosmicdungeon.network.payload;

import net.goui.cosmicdungeon.CosmicDungeonMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

public record RegionLookAllRequestPayload(
        ResourceKey<Level> dimension,
        int centerChunkX,
        int centerChunkZ,
        int radiusChunks
) implements CustomPacketPayload {

    public static final Type<RegionLookAllRequestPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(CosmicDungeonMod.MOD_ID, "region_look_all_req"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RegionLookAllRequestPayload> STREAM_CODEC =
            StreamCodec.of(RegionLookAllRequestPayload::encode, RegionLookAllRequestPayload::decode);

    private static void encode(RegistryFriendlyByteBuf buf, RegionLookAllRequestPayload p) {
        buf.writeResourceLocation(p.dimension().location());
        buf.writeVarInt(p.centerChunkX());
        buf.writeVarInt(p.centerChunkZ());
        buf.writeVarInt(p.radiusChunks());
    }

    private static RegionLookAllRequestPayload decode(RegistryFriendlyByteBuf buf) {
        ResourceLocation dimId = buf.readResourceLocation();
        ResourceKey<Level> dimKey = ResourceKey.create(Registries.DIMENSION, dimId);

        int cx = buf.readVarInt();
        int cz = buf.readVarInt();
        int r = buf.readVarInt();

        return new RegionLookAllRequestPayload(dimKey, cx, cz, r);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
