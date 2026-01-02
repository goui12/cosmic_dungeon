package net.goui.cosmicdungeon.playerclass.api;

import io.netty.buffer.ByteBuf;
import net.goui.cosmicdungeon.CosmicDungeonMod;
import net.goui.cosmicdungeon.playerclass.metalmancer.MetalmancerActions;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

import java.util.Objects;

@EventBusSubscriber(modid = CosmicDungeonMod.MOD_ID)
public final class ClassNet {
    private ClassNet() {}

    /* ---------- ids & keys ---------- */

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(CosmicDungeonMod.MOD_ID, path);
    }

    // Satchel to seed into slot index 1
    private static final ResourceLocation SATCHEL_ID =
            ResourceLocation.fromNamespaceAndPath(CosmicDungeonMod.MOD_ID, "satchel_of_samples");

    /* ---------- PD helpers ---------- */

    private static String getActiveClass(Player player) {
        return ClassNbtUtil.getClassId(player);
    }

    private static void setActiveClass(Player player, String cls) {
        ClassNbtUtil.setClassId(player, cls);
    }

    private static boolean isMetalmancer(Player player) {
        return ClassNbtUtil.isMetalmancer(player);
    }

    private static CompoundTag readServerExtraNbt(ServerPlayer sp) {
        // Always read from the same place ExtraInventoryMenu / SatchelApi expect:
        // cosmicdungeon -> extra
        CompoundTag root = sp.getPersistentData().getCompoundOrEmpty(ClassData.ROOT_TAG);
        return root.getCompound(ClassData.KEY_EXTRA).orElseGet(CompoundTag::new);
    }

    private static void applyClientExtraNbt(LocalPlayer pl, CompoundTag extraNbt) {
        CompoundTag pd   = pl.getPersistentData();
        CompoundTag root = pd.getCompoundOrEmpty(ClassData.ROOT_TAG).copy();
        root.put(ClassData.KEY_EXTRA, extraNbt);
        pd.put(ClassData.ROOT_TAG, root);
    }

    /* ---------- seeding (server-side) ---------- */

    /**
     * Seeds the player's 3-slot extra-hotbar under cosmicdungeon.extra in PD.
     * - Only fills empty slots (no overwrite).
     * - Puts cosmicdungeon:satchel_of_samples in slot index 1 (the "second" slot).
     * Uses ValueInput/ValueOutput (via TagValue* factories) to match 1.21.9/1.21.10 signatures.
     */
    public static void seedMetalmancerExtra(ServerPlayer sp) {
        // Read current PD root + "extra" tag (or empty)
        CompoundTag root     = sp.getPersistentData().getCompoundOrEmpty(ClassData.ROOT_TAG);
        CompoundTag extraTag = root.getCompound(ClassData.KEY_EXTRA).orElseGet(CompoundTag::new);

        // Load list from NBT using ValueInput
        NonNullList<ItemStack> list = NonNullList.withSize(3, ItemStack.EMPTY);
        ValueInput in = TagValueInput.create(ProblemReporter.DISCARDING, sp.level().registryAccess(), extraTag);
        ContainerHelper.loadAllItems(in, list);

        // Slot 1: Satchel of Samples, if empty
        if (list.get(1).isEmpty()) {
            Item satchel = BuiltInRegistries.ITEM.getValue(SATCHEL_ID);
            if (satchel != null) {
                list.set(1, new ItemStack(satchel));
            }
        }

        // Save list back to NBT using ValueOutput
        TagValueOutput out = TagValueOutput.createWithoutContext(ProblemReporter.DISCARDING);
        ContainerHelper.saveAllItems(out, list);
        CompoundTag newExtra = out.buildResult();

        // Write back under cosmicdungeon.extra
        CompoundTag newRoot = sp.getPersistentData().getCompoundOrEmpty(ClassData.ROOT_TAG).copy();
        newRoot.put(ClassData.KEY_EXTRA, newExtra);
        sp.getPersistentData().put(ClassData.ROOT_TAG, newRoot);
    }

    /* ---------- public server helpers ---------- */

    public static void enableMetalmancer(ServerPlayer sp) {
        setActiveClass(sp, ClassKeys.CLASS_ID_METALMANCER);
        seedMetalmancerExtra(sp);
        sendFullTo(sp);
    }

    public static void disableMetalmancer(ServerPlayer sp) {
        setActiveClass(sp, ClassKeys.CLASS_ID_NONE);

        CompoundTag pd   = sp.getPersistentData();
        CompoundTag root = pd.getCompoundOrEmpty(ClassData.ROOT_TAG).copy();
        root.remove(ClassData.KEY_EXTRA);
        pd.put(ClassData.ROOT_TAG, root);

        sendFullTo(sp);
    }

    /* ---------- C2S payloads ---------- */

    public record C2S_SetClass(String classId) implements CustomPacketPayload {
        public static final Type<C2S_SetClass> TYPE = new Type<>(id("c2s_set_class"));
        public static final StreamCodec<ByteBuf, C2S_SetClass> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, C2S_SetClass::classId, C2S_SetClass::new
        );
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record C2S_Action(String actionId) implements CustomPacketPayload {
        public static final Type<C2S_Action> TYPE = new Type<>(id("c2s_action"));
        public static final StreamCodec<ByteBuf, C2S_Action> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, C2S_Action::actionId, C2S_Action::new
        );
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record C2S_RequestClass() implements CustomPacketPayload {
        public static final Type<C2S_RequestClass> TYPE = new Type<>(id("c2s_request_class"));
        public static final StreamCodec<ByteBuf, C2S_RequestClass> STREAM_CODEC = new StreamCodec<>() {
            @Override public C2S_RequestClass decode(ByteBuf buf) { return new C2S_RequestClass(); }
            @Override public void encode(ByteBuf buf, C2S_RequestClass v) {}
        };
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** Ask the server to open the Metalmancer inventory menu */
    public record C2S_OpenMetalmancerInventory() implements CustomPacketPayload {
        public static final Type<C2S_OpenMetalmancerInventory> TYPE = new Type<>(id("c2s_open_mm_inventory"));
        public static final StreamCodec<ByteBuf, C2S_OpenMetalmancerInventory> STREAM_CODEC = new StreamCodec<>() {
            @Override public C2S_OpenMetalmancerInventory decode(ByteBuf buf) { return new C2S_OpenMetalmancerInventory(); }
            @Override public void encode(ByteBuf buf, C2S_OpenMetalmancerInventory v) {}
        };
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /* ---------- S2C payload ---------- */

    public record S2C_ClassSync(String classId, CompoundTag extraNbt) implements CustomPacketPayload {
        public static final Type<S2C_ClassSync> TYPE = new Type<>(id("s2c_class_sync"));
        public static final StreamCodec<ByteBuf, S2C_ClassSync> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, S2C_ClassSync::classId,
                ByteBufCodecs.COMPOUND_TAG, S2C_ClassSync::extraNbt,
                S2C_ClassSync::new
        );
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /* ---------- registration ---------- */

    @SubscribeEvent
    public static void register(final RegisterPayloadHandlersEvent e) {
        var reg = e.registrar(CosmicDungeonMod.MOD_ID);

        // Set/clear class (C2S)
        reg.playToServer(C2S_SetClass.TYPE, C2S_SetClass.STREAM_CODEC, (pkt, ctx) -> {
            if (!(ctx.player() instanceof ServerPlayer sp)) return;

            String cls = Objects.requireNonNullElse(pkt.classId(), ClassKeys.CLASS_ID_NONE);
            if (!ClassKeys.CLASS_ID_METALMANCER.equals(cls)) {
                cls = ClassKeys.CLASS_ID_NONE;
            }

            setActiveClass(sp, cls);

            CompoundTag pd = sp.getPersistentData();
            if (ClassKeys.CLASS_ID_METALMANCER.equals(cls)) {
                seedMetalmancerExtra(sp);
            } else {
                CompoundTag root = pd.getCompoundOrEmpty(ClassData.ROOT_TAG).copy();
                root.remove(ClassData.KEY_EXTRA);
                pd.put(ClassData.ROOT_TAG, root);
            }

            PacketDistributor.sendToPlayer(sp, new S2C_ClassSync(cls, readServerExtraNbt(sp)));
        });

        // Actions while Metalmancer (C2S)
        reg.playToServer(C2S_Action.TYPE, C2S_Action.STREAM_CODEC, (pkt, ctx) -> {
            if (!(ctx.player() instanceof ServerPlayer sp)) return;
            if (!isMetalmancer(sp)) return;

            // Centralized handling (includes cooldowns, ore use, golem logic)
            MetalmancerActions.handleAction(sp, pkt.actionId());
        });

        // Client asks for current class + extra (C2S)
        reg.playToServer(C2S_RequestClass.TYPE, C2S_RequestClass.STREAM_CODEC, (pkt, ctx) -> {
            if (!(ctx.player() instanceof ServerPlayer sp)) return;
            String cls = getActiveClass(sp);
            PacketDistributor.sendToPlayer(sp, new S2C_ClassSync(cls, readServerExtraNbt(sp)));
        });

        // Open MM inventory (C2S)
        reg.playToServer(C2S_OpenMetalmancerInventory.TYPE, C2S_OpenMetalmancerInventory.STREAM_CODEC, (pkt, ctx) -> {
            if (!(ctx.player() instanceof ServerPlayer sp)) return;
            if (!isMetalmancer(sp)) return;
            ExtraInventoryMenu.open(sp); // server opens the real container
        });

        // Server -> Client sync (S2C)
        reg.playToClient(S2C_ClassSync.TYPE, S2C_ClassSync.STREAM_CODEC, (pkt, ctx) -> {
            Minecraft mc = Minecraft.getInstance();
            LocalPlayer pl = mc.player;
            if (pl == null) return;

            ClassNbtUtil.setClassId(pl, pkt.classId());
            applyClientExtraNbt(pl, pkt.extraNbt());
        });
    }

    /* ---------- public client helpers (C2S) ---------- */

    public static void requestOpenMetalmancerInventory() {
        var pc = Minecraft.getInstance().getConnection();
        Connection conn = (pc != null) ? pc.getConnection() : null;
        if (conn != null) {
            conn.send(new ServerboundCustomPayloadPacket(new C2S_OpenMetalmancerInventory()));
        }
    }

    /** Generic helper for C2S_Action packets (used by Metalmancer weapons/staffs). */
    public static void sendActionToServer(String actionId) {
        var pc = Minecraft.getInstance().getConnection();
        Connection conn = (pc != null) ? pc.getConnection() : null;
        if (conn != null && actionId != null && !actionId.isEmpty()) {
            conn.send(new ServerboundCustomPayloadPacket(new C2S_Action(actionId)));
        }
    }

    /* ---------- server helper ---------- */

    public static void sendFullTo(ServerPlayer sp) {
        String cls = getActiveClass(sp);
        PacketDistributor.sendToPlayer(sp, new S2C_ClassSync(cls, readServerExtraNbt(sp)));
    }
}
