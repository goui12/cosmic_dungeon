package net.goui.cosmicdungeon.network.payload;

import net.goui.cosmicdungeon.CosmicDungeonMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record SpawnerPresetKeybindPayload(int slot) implements CustomPacketPayload {
    public static final Type<SpawnerPresetKeybindPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(CosmicDungeonMod.MOD_ID, "spawner_preset_keybind"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SpawnerPresetKeybindPayload> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.VAR_INT, SpawnerPresetKeybindPayload::slot, SpawnerPresetKeybindPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
