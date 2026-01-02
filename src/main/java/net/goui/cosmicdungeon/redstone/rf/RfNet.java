package net.goui.cosmicdungeon.redstone.rf;

import io.netty.buffer.ByteBuf;
import net.goui.cosmicdungeon.CosmicDungeonMod;
import net.goui.cosmicdungeon.auth.AccessPolicy;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

import java.util.HashMap;
import java.util.Map;

@EventBusSubscriber(modid = CosmicDungeonMod.MOD_ID)
public final class RfNet {
    private RfNet() {}

    /* -------- BlockPos codec (VAR_LONG <-> BlockPos) -------- */
    private static final StreamCodec<ByteBuf, BlockPos> BLOCK_POS_STREAM =
            ByteBufCodecs.VAR_LONG.map(BlockPos::of, BlockPos::asLong);

    /* =====================  PAYLOADS  ===================== */

    /** Client -> Server: set Hz for a transmitter/receiver at pos. */
    public record C2S_SetHz(BlockPos pos, int hz) implements CustomPacketPayload {
        public static final Type<C2S_SetHz> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath(CosmicDungeonMod.MOD_ID, "set_hz"));

        public static final StreamCodec<ByteBuf, C2S_SetHz> STREAM_CODEC = StreamCodec.composite(
                BLOCK_POS_STREAM, C2S_SetHz::pos,
                ByteBufCodecs.VAR_INT,  C2S_SetHz::hz,
                C2S_SetHz::new
        );

        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** Client -> Server: request current Hz at pos (server replies with S2C_HzSync). */
    public record C2S_RequestHz(BlockPos pos) implements CustomPacketPayload {
        public static final Type<C2S_RequestHz> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath(CosmicDungeonMod.MOD_ID, "request_hz"));

        public static final StreamCodec<ByteBuf, C2S_RequestHz> STREAM_CODEC = StreamCodec.composite(
                BLOCK_POS_STREAM, C2S_RequestHz::pos,
                C2S_RequestHz::new
        );

        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** Server -> Client: authoritative Hz for pos (keeps UI/cache correct). */
    public record S2C_HzSync(BlockPos pos, int hz) implements CustomPacketPayload {
        public static final Type<S2C_HzSync> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath(CosmicDungeonMod.MOD_ID, "hz_sync"));

        public static final StreamCodec<ByteBuf, S2C_HzSync> STREAM_CODEC = StreamCodec.composite(
                BLOCK_POS_STREAM, S2C_HzSync::pos,
                ByteBufCodecs.VAR_INT,  S2C_HzSync::hz,
                S2C_HzSync::new
        );

        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /* =====================  REGISTRATION (COMMON)  ===================== */

    @SubscribeEvent
    public static void register(final RegisterPayloadHandlersEvent event) {
        final var registrar = event.registrar("1");

        // C2S: set Hz (MUST be server-authoritative + permission-checked)
        registrar.playToServer(
                C2S_SetHz.TYPE,
                C2S_SetHz.STREAM_CODEC,
                (payload, ctx) -> {
                    if (!(ctx.player() instanceof ServerPlayer sp)) return;
                    if (!AccessPolicy.isDeveloper(sp)) {
                        // silent drop = secure + no spam; DeviceAccessEvents already gives UX on click
                        return;
                    }

                    if (!(sp.level() instanceof ServerLevel level)) return;

                    // Optional proximity check: prevents remote edits from across the world.
                    if (sp.blockPosition().distManhattan(payload.pos()) > 16) return;

                    final BlockEntity be = level.getBlockEntity(payload.pos());

                    if (be instanceof RedstoneTransmitterBE t) {
                        t.setHz(level, payload.hz());
                    } else if (be instanceof RedstoneReceiverBE r) {
                        r.setHz(level, payload.hz());
                    }
                }
        );

        // C2S: request current Hz (GUI open)
        registrar.playToServer(
                C2S_RequestHz.TYPE,
                C2S_RequestHz.STREAM_CODEC,
                (payload, ctx) -> {
                    if (!(ctx.player() instanceof ServerPlayer sp)) return;

                    // Viewing could be allowed for everyone, but your requirement is:
                    // "dungeoneer shouldn't open GUI" → so lock this too.
                    if (!AccessPolicy.isDeveloper(sp)) return;

                    if (!(sp.level() instanceof ServerLevel level)) return;

                    // Optional proximity check
                    if (sp.blockPosition().distManhattan(payload.pos()) > 16) return;

                    final BlockEntity be = level.getBlockEntity(payload.pos());

                    int hz = -1; // -1 means "NULL" for client display
                    if (be instanceof RedstoneTransmitterBE t) hz = t.getHz();
                    else if (be instanceof RedstoneReceiverBE r) hz = r.getHz();

                    ctx.reply(new S2C_HzSync(payload.pos(), hz));
                }
        );

        // S2C: apply authoritative Hz on the client
        registrar.playToClient(
                S2C_HzSync.TYPE,
                S2C_HzSync.STREAM_CODEC,
                (payload, ctx) -> {
                    RfNet.ClientCache.putHz(payload.pos(), payload.hz());
                    HzConfigScreen.onServerHz(payload.pos(), payload.hz());
                }
        );
    }

    /* =====================  SERVER → CLIENT BROADCAST HELPERS  ===================== */

    /** Send an Hz sync to all players tracking the chunk containing {@code pos}. */
    public static void broadcastHz(ServerLevel level, BlockPos pos, int hz) {
        var pkt = new S2C_HzSync(pos, hz);
        var chunkPos = new ChunkPos(pos);
        level.getChunkSource().chunkMap
                .getPlayers(chunkPos, false)
                .forEach(p -> PacketDistributor.sendToPlayer(p, pkt));
    }

    /* =====================  CLIENT CACHE (UI helper)  ===================== */

    public static final class ClientCache {
        private static final Map<Long, Integer> HZ = new HashMap<>();
        public static Integer getHz(BlockPos pos) { return HZ.get(pos.asLong()); }
        public static void putHz(BlockPos pos, int hz) { HZ.put(pos.asLong(), hz); }
        public static void clear(BlockPos pos) { HZ.remove(pos.asLong()); }
        public static void clearAll() { HZ.clear(); }
    }
}
