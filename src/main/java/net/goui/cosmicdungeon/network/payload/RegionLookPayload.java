package net.goui.cosmicdungeon.network.payload;

import net.goui.cosmicdungeon.CosmicDungeonMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

public record RegionLookPayload(
        boolean enabled,
        String regionName,
        ResourceKey<Level> dimension,
        BlockPos min,
        BlockPos max
) implements CustomPacketPayload {

    public static final Type<RegionLookPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(CosmicDungeonMod.MOD_ID, "region_look"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RegionLookPayload> STREAM_CODEC =
            StreamCodec.of(RegionLookPayload::encode, RegionLookPayload::decode);

    private static void encode(RegistryFriendlyByteBuf buf, RegionLookPayload p) {
        buf.writeBoolean(p.enabled());
        buf.writeUtf(p.regionName(), 256);
        buf.writeResourceLocation(p.dimension().location());
        buf.writeBlockPos(p.min());
        buf.writeBlockPos(p.max());
    }

    private static RegionLookPayload decode(RegistryFriendlyByteBuf buf) {
        boolean enabled = buf.readBoolean();
        String name = buf.readUtf(256);

        ResourceLocation dimId = buf.readResourceLocation();
        ResourceKey<Level> dimKey = ResourceKey.create(Registries.DIMENSION, dimId);

        BlockPos min = buf.readBlockPos();
        BlockPos max = buf.readBlockPos();
        return new RegionLookPayload(enabled, name, dimKey, min, max);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
