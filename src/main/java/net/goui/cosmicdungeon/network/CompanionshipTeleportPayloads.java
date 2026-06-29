package net.goui.cosmicdungeon.network;

import io.netty.buffer.ByteBuf;
import net.goui.cosmicdungeon.CosmicDungeonMod;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import java.util.List;
import java.util.UUID;

public final class CompanionshipTeleportPayloads {
    private CompanionshipTeleportPayloads() {}
    private static final StreamCodec<ByteBuf, UUID> UUID_CODEC = StreamCodec.of((buf, id) -> { buf.writeLong(id.getMostSignificantBits()); buf.writeLong(id.getLeastSignificantBits()); }, buf -> new UUID(buf.readLong(), buf.readLong()));
    public record PlayerEntry(UUID playerId, String name) {}
    public static final StreamCodec<ByteBuf, PlayerEntry> ENTRY_CODEC = StreamCodec.composite(UUID_CODEC, PlayerEntry::playerId, ByteBufCodecs.STRING_UTF8, PlayerEntry::name, PlayerEntry::new);
    public record S2C_Open(List<PlayerEntry> players) implements CustomPacketPayload {
        public static final Type<S2C_Open> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CosmicDungeonMod.MOD_ID, "companionship_teleport_open"));
        public static final StreamCodec<ByteBuf, S2C_Open> STREAM_CODEC = StreamCodec.composite(ENTRY_CODEC.apply(ByteBufCodecs.list()), S2C_Open::players, S2C_Open::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }
    public record C2S_Select(UUID targetPlayerId) implements CustomPacketPayload {
        public static final Type<C2S_Select> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CosmicDungeonMod.MOD_ID, "companionship_teleport_select"));
        public static final StreamCodec<ByteBuf, C2S_Select> STREAM_CODEC = StreamCodec.composite(UUID_CODEC, C2S_Select::targetPlayerId, C2S_Select::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }
}
