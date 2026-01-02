// file: src/main/java/net/goui/cosmicdungeon/network/ClassPayloads.java
package net.goui.cosmicdungeon.network;

import io.netty.buffer.ByteBuf;
import net.goui.cosmicdungeon.CosmicDungeonMod;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public final class ClassPayloads {
    private ClassPayloads() {}

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(CosmicDungeonMod.MOD_ID, path);
    }

    /* ===================== CORE CLASS SYNC ===================== */

    public record C2S_RequestClass() implements CustomPacketPayload {
        public static final Type<C2S_RequestClass> TYPE = new Type<>(id("c2s_request_class"));
        public static final StreamCodec<ByteBuf, C2S_RequestClass> STREAM_CODEC =
                StreamCodec.unit(new C2S_RequestClass());
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record C2S_Action(String actionId) implements CustomPacketPayload {
        public static final Type<C2S_Action> TYPE = new Type<>(id("c2s_action"));
        public static final StreamCodec<ByteBuf, C2S_Action> STREAM_CODEC =
                StreamCodec.composite(
                        ByteBufCodecs.STRING_UTF8, C2S_Action::actionId,
                        C2S_Action::new
                );
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record C2S_OpenMetalmancerInventory() implements CustomPacketPayload {
        public static final Type<C2S_OpenMetalmancerInventory> TYPE = new Type<>(id("c2s_open_mm_inventory"));
        public static final StreamCodec<ByteBuf, C2S_OpenMetalmancerInventory> STREAM_CODEC =
                StreamCodec.unit(new C2S_OpenMetalmancerInventory());
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record S2C_ClassSync(String classId, net.minecraft.nbt.CompoundTag extraNbt) implements CustomPacketPayload {
        public static final Type<S2C_ClassSync> TYPE = new Type<>(id("s2c_class_sync"));
        public static final StreamCodec<ByteBuf, S2C_ClassSync> STREAM_CODEC =
                StreamCodec.composite(
                        ByteBufCodecs.STRING_UTF8, S2C_ClassSync::classId,
                        ByteBufCodecs.COMPOUND_TAG, S2C_ClassSync::extraNbt,
                        S2C_ClassSync::new
                );
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /* ===================== CLASS SELECTOR ===================== */

    /** Client asks for selector data (server returns active class + list). */
    public record C2S_RequestSelectorData() implements CustomPacketPayload {
        public static final Type<C2S_RequestSelectorData> TYPE = new Type<>(id("class_selector_request"));
        public static final StreamCodec<ByteBuf, C2S_RequestSelectorData> STREAM_CODEC =
                StreamCodec.unit(new C2S_RequestSelectorData());
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** Server responds with authoritative selector data. */
    public record S2C_SelectorData(String activeClassId, List<String> availableClassIds) implements CustomPacketPayload {
        public static final Type<S2C_SelectorData> TYPE = new Type<>(id("class_selector_data"));
        public static final StreamCodec<ByteBuf, S2C_SelectorData> STREAM_CODEC =
                StreamCodec.composite(
                        ByteBufCodecs.STRING_UTF8, S2C_SelectorData::activeClassId,
                        ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), S2C_SelectorData::availableClassIds,
                        S2C_SelectorData::new
                );
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** Client requests selecting a class (server validates + applies). */
    public record C2S_SelectClass(String classId) implements CustomPacketPayload {
        public static final Type<C2S_SelectClass> TYPE = new Type<>(id("class_selector_select"));
        public static final StreamCodec<ByteBuf, C2S_SelectClass> STREAM_CODEC =
                StreamCodec.composite(
                        ByteBufCodecs.STRING_UTF8, C2S_SelectClass::classId,
                        C2S_SelectClass::new
                );
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** Server returns result message (ClassNet also sends S2C_ClassSync separately). */
    public record S2C_SelectResult(boolean ok, String message, String newClassId) implements CustomPacketPayload {
        public static final Type<S2C_SelectResult> TYPE = new Type<>(id("class_selector_result"));
        public static final StreamCodec<ByteBuf, S2C_SelectResult> STREAM_CODEC =
                StreamCodec.composite(
                        ByteBufCodecs.BOOL, S2C_SelectResult::ok,
                        ByteBufCodecs.STRING_UTF8, S2C_SelectResult::message,
                        ByteBufCodecs.STRING_UTF8, S2C_SelectResult::newClassId,
                        S2C_SelectResult::new
                );
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }
}
