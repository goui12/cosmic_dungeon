package net.goui.cosmicdungeon.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

public final class DragoonRepairPayloads {
    private DragoonRepairPayloads() {}
    private static final StreamCodec<ByteBuf, UUID> UUID_CODEC = StreamCodec.of((buf, uuid) -> { buf.writeLong(uuid.getMostSignificantBits()); buf.writeLong(uuid.getLeastSignificantBits()); }, buf -> new UUID(buf.readLong(), buf.readLong()));
    public record C2S_AdjustFee(String denominationId, int deltaCount) implements CustomPacketPayload { public static final Type<C2S_AdjustFee> TYPE = new Type<>(id("repair_fee_adjust")); public static final StreamCodec<ByteBuf,C2S_AdjustFee> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.STRING_UTF8, C2S_AdjustFee::denominationId, ByteBufCodecs.VAR_INT, C2S_AdjustFee::deltaCount, C2S_AdjustFee::new); public Type<? extends CustomPacketPayload> type(){return TYPE;} }
    public record C2S_SelectUnits(int units) implements CustomPacketPayload { public static final Type<C2S_SelectUnits> TYPE = new Type<>(id("repair_units")); public static final StreamCodec<ByteBuf,C2S_SelectUnits> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.VAR_INT, C2S_SelectUnits::units, C2S_SelectUnits::new); public Type<? extends CustomPacketPayload> type(){return TYPE;} }
    public record C2S_TargetReady(boolean ready) implements CustomPacketPayload { public static final Type<C2S_TargetReady> TYPE = new Type<>(id("repair_ready")); public static final StreamCodec<ByteBuf,C2S_TargetReady> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.BOOL, C2S_TargetReady::ready, C2S_TargetReady::new); public Type<? extends CustomPacketPayload> type(){return TYPE;} }
    public record C2S_Repair() implements CustomPacketPayload { public static final Type<C2S_Repair> TYPE = new Type<>(id("repair_do")); public static final StreamCodec<ByteBuf,C2S_Repair> STREAM_CODEC = StreamCodec.unit(new C2S_Repair()); public Type<? extends CustomPacketPayload> type(){return TYPE;} }
    public record C2S_Cancel() implements CustomPacketPayload { public static final Type<C2S_Cancel> TYPE = new Type<>(id("repair_cancel")); public static final StreamCodec<ByteBuf,C2S_Cancel> STREAM_CODEC = StreamCodec.unit(new C2S_Cancel()); public Type<? extends CustomPacketPayload> type(){return TYPE;} }
    public record S2C_State(int containerId, UUID sessionId, String dragoonName, String targetName, boolean viewerDragoon, long offeredFeeTrace, long targetBalanceTrace, long dragoonCapacityTrace, int selectedUnits, int requiredUnitsToFull, String materialItemId, String materialDisplay, int requiredMaterialCount, boolean dragoonHasMaterial, boolean targetReady, boolean dragoonRepairing, String statusMessage) implements CustomPacketPayload {
        public static final Type<S2C_State> TYPE = new Type<>(id("repair_state"));
        public static final StreamCodec<ByteBuf,S2C_State> STREAM_CODEC = StreamCodec.of((buf,p)->{ ByteBufCodecs.VAR_INT.encode(buf,p.containerId); UUID_CODEC.encode(buf,p.sessionId); ByteBufCodecs.STRING_UTF8.encode(buf,p.dragoonName); ByteBufCodecs.STRING_UTF8.encode(buf,p.targetName); ByteBufCodecs.BOOL.encode(buf,p.viewerDragoon); ByteBufCodecs.VAR_LONG.encode(buf,p.offeredFeeTrace); ByteBufCodecs.VAR_LONG.encode(buf,p.targetBalanceTrace); ByteBufCodecs.VAR_LONG.encode(buf,p.dragoonCapacityTrace); ByteBufCodecs.VAR_INT.encode(buf,p.selectedUnits); ByteBufCodecs.VAR_INT.encode(buf,p.requiredUnitsToFull); ByteBufCodecs.STRING_UTF8.encode(buf,p.materialItemId); ByteBufCodecs.STRING_UTF8.encode(buf,p.materialDisplay); ByteBufCodecs.VAR_INT.encode(buf,p.requiredMaterialCount); ByteBufCodecs.BOOL.encode(buf,p.dragoonHasMaterial); ByteBufCodecs.BOOL.encode(buf,p.targetReady); ByteBufCodecs.BOOL.encode(buf,p.dragoonRepairing); ByteBufCodecs.STRING_UTF8.encode(buf,p.statusMessage); }, buf -> new S2C_State(ByteBufCodecs.VAR_INT.decode(buf), UUID_CODEC.decode(buf), ByteBufCodecs.STRING_UTF8.decode(buf), ByteBufCodecs.STRING_UTF8.decode(buf), ByteBufCodecs.BOOL.decode(buf), ByteBufCodecs.VAR_LONG.decode(buf), ByteBufCodecs.VAR_LONG.decode(buf), ByteBufCodecs.VAR_LONG.decode(buf), ByteBufCodecs.VAR_INT.decode(buf), ByteBufCodecs.VAR_INT.decode(buf), ByteBufCodecs.STRING_UTF8.decode(buf), ByteBufCodecs.STRING_UTF8.decode(buf), ByteBufCodecs.VAR_INT.decode(buf), ByteBufCodecs.BOOL.decode(buf), ByteBufCodecs.BOOL.decode(buf), ByteBufCodecs.BOOL.decode(buf), ByteBufCodecs.STRING_UTF8.decode(buf)));
        public Type<? extends CustomPacketPayload> type(){return TYPE;}
    }
    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath("cosmicdungeon", path); }
}
