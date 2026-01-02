// file: src/main/java/net/goui/cosmicdungeon/network/payload/RegionLookAllPayload.java
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

import java.util.ArrayList;
import java.util.List;

public record RegionLookAllPayload(
        boolean enabled,
        ResourceKey<Level> dimension,
        List<Entry> regions
) implements CustomPacketPayload {

    public record Entry(String name, BlockPos min, BlockPos max) {}

    public static final Type<RegionLookAllPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(CosmicDungeonMod.MOD_ID, "region_look_all"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RegionLookAllPayload> STREAM_CODEC =
            StreamCodec.of(RegionLookAllPayload::encode, RegionLookAllPayload::decode);

    private static void encode(RegistryFriendlyByteBuf buf, RegionLookAllPayload p) {
        buf.writeBoolean(p.enabled());
        buf.writeResourceLocation(p.dimension().location());

        List<Entry> list = p.regions() == null ? List.of() : p.regions();
        buf.writeVarInt(list.size());
        for (Entry e : list) {
            buf.writeUtf(e.name(), 256);
            buf.writeBlockPos(e.min());
            buf.writeBlockPos(e.max());
        }
    }

    private static RegionLookAllPayload decode(RegistryFriendlyByteBuf buf) {
        boolean enabled = buf.readBoolean();

        ResourceLocation dimId = buf.readResourceLocation();
        ResourceKey<Level> dimKey = ResourceKey.create(Registries.DIMENSION, dimId);

        int size = buf.readVarInt();
        List<Entry> list = new ArrayList<>(Math.max(0, size));
        for (int i = 0; i < size; i++) {
            String name = buf.readUtf(256);
            BlockPos min = buf.readBlockPos();
            BlockPos max = buf.readBlockPos();
            list.add(new Entry(name, min, max));
        }

        return new RegionLookAllPayload(enabled, dimKey, list);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
