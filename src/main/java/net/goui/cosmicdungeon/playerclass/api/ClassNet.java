// file: src/main/java/net/goui/cosmicdungeon/playerclass/api/ClassNet.java
package net.goui.cosmicdungeon.playerclass.api;

import io.netty.buffer.ByteBuf;
import net.goui.cosmicdungeon.CosmicDungeonMod;
import net.goui.cosmicdungeon.playerclass.metalmancer.MetalmancerActions;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
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
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

import java.util.Objects;

@EventBusSubscriber(modid = CosmicDungeonMod.MOD_ID)
public final class ClassNet {
    private ClassNet() {}

    /* ---------- ids ---------- */

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
        CompoundTag root = sp.getPersistentData().getCompoundOrEmpty(ClassData.ROOT_TAG);
        return root.getCompound(ClassData.KEY_EXTRA).orElseGet(CompoundTag::new);
    }

    /* ---------- seeding (server-side) ---------- */

    public static void seedMetalmancerExtra(ServerPlayer sp) {
        CompoundTag root     = sp.getPersistentData().getCompoundOrEmpty(ClassData.ROOT_TAG);
        CompoundTag extraTag = root.getCompound(ClassData.KEY_EXTRA).orElseGet(CompoundTag::new);

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

        TagValueOutput out = TagValueOutput.createWithoutContext(ProblemReporter.DISCARDING);
        ContainerHelper.saveAllItems(out, list);
        CompoundTag newExtra = out.buildResult();

        CompoundTag newRoot = sp.getPersistentData().getCompoundOrEmpty(ClassData.ROOT_TAG).copy();
        newRoot.put(ClassData.KEY_EXTRA, newExtra);
        sp.getPersistentData().put(ClassData.ROOT_TAG, newRoot);
    }

    /* ---------- public server helpers ---------- */

    public static void enableMetalmancer(ServerPlayer sp) {
        setActiveClass(sp, ClassKeys.CLASS_ID_METALMANCER);
        seedMetalmancerExtra(sp);
        PacketDistributor.sendToPlayer(sp, new S2C_ClassSync(ClassKeys.CLASS_ID_METALMANCER, readServerExtraNbt(sp)));
    }

    public static void disableMetalmancer(ServerPlayer sp) {
        setActiveClass(sp, ClassKeys.CLASS_ID_NONE);

        CompoundTag pd = sp.getPersistentData();
        CompoundTag root = pd.getCompoundOrEmpty(ClassData.ROOT_TAG).copy();
        root.remove(ClassData.KEY_EXTRA);
        pd.put(ClassData.ROOT_TAG, root);

        PacketDistributor.sendToPlayer(sp, new S2C_ClassSync(ClassKeys.CLASS_ID_NONE, new CompoundTag()));
    }

    public static void sendFullTo(ServerPlayer sp) {
        String cls = getActiveClass(sp);
        PacketDistributor.sendToPlayer(sp, new S2C_ClassSync(cls, readServerExtraNbt(sp)));
    }

    /* ---------- payloads ---------- */

    public record C2S_SetClass(String classId) implements CustomPacketPayload {
        public static final Type<C2S_SetClass> TYPE = new Type<>(id("c2s_set_class"));
        public static final StreamCodec<ByteBuf, C2S_SetClass> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, C2S_SetClass::classId,
                C2S_SetClass::new
        );
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record C2S_Action(String actionId) implements CustomPacketPayload {
        public static final Type<C2S_Action> TYPE = new Type<>(id("c2s_action"));
        public static final StreamCodec<ByteBuf, C2S_Action> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, C2S_Action::actionId,
                C2S_Action::new
        );
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record C2S_RequestClass() implements CustomPacketPayload {
        public static final Type<C2S_RequestClass> TYPE = new Type<>(id("c2s_request_class"));
        public static final StreamCodec<ByteBuf, C2S_RequestClass> STREAM_CODEC =
                StreamCodec.unit(new C2S_RequestClass());
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record C2S_OpenMetalmancerInventory() implements CustomPacketPayload {
        public static final Type<C2S_OpenMetalmancerInventory> TYPE = new Type<>(id("c2s_open_mm_inventory"));
        public static final StreamCodec<ByteBuf, C2S_OpenMetalmancerInventory> STREAM_CODEC =
                StreamCodec.unit(new C2S_OpenMetalmancerInventory());
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record S2C_ClassSync(String classId, CompoundTag extraNbt) implements CustomPacketPayload {
        public static final Type<S2C_ClassSync> TYPE = new Type<>(id("s2c_class_sync"));
        public static final StreamCodec<ByteBuf, S2C_ClassSync> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, S2C_ClassSync::classId,
                ByteBufCodecs.COMPOUND_TAG, S2C_ClassSync::extraNbt,
                S2C_ClassSync::new
        );
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /* ---------- registration (runs on BOTH sides; safe) ---------- */

    @SubscribeEvent
    public static void register(final RegisterPayloadHandlersEvent e) {
        var reg = e.registrar(CosmicDungeonMod.MOD_ID);

        // Register S2C type on both sides. Handler will only run client-side (because it's playToClient).
        reg.playToClient(S2C_ClassSync.TYPE, S2C_ClassSync.STREAM_CODEC, (pkt, ctx) -> {
            var player = ctx.player();
            if (player == null) return;

            String cls = Objects.requireNonNullElse(pkt.classId(), ClassKeys.CLASS_ID_NONE);
            setActiveClass(player, cls);

            CompoundTag pd = player.getPersistentData();
            CompoundTag root = pd.getCompoundOrEmpty(ClassData.ROOT_TAG).copy();

            if (ClassKeys.CLASS_ID_METALMANCER.equals(cls)) {
                root.put(ClassData.KEY_EXTRA, pkt.extraNbt() == null ? new CompoundTag() : pkt.extraNbt());
            } else {
                root.remove(ClassData.KEY_EXTRA);
            }

            pd.put(ClassData.ROOT_TAG, root);
        });

        // C2S: Set/clear class
        reg.playToServer(C2S_SetClass.TYPE, C2S_SetClass.STREAM_CODEC, (pkt, ctx) -> {
            if (!(ctx.player() instanceof ServerPlayer sp)) return;

            String cls = Objects.requireNonNullElse(pkt.classId(), ClassKeys.CLASS_ID_NONE);
            if (!ClassKeys.CLASS_ID_METALMANCER.equals(cls)) cls = ClassKeys.CLASS_ID_NONE;

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

        // C2S: Metalmancer actions
        reg.playToServer(C2S_Action.TYPE, C2S_Action.STREAM_CODEC, (pkt, ctx) -> {
            if (!(ctx.player() instanceof ServerPlayer sp)) return;
            if (!isMetalmancer(sp)) return;
            MetalmancerActions.handleAction(sp, pkt.actionId());
        });

        // C2S: Client asks for current class + extra
        reg.playToServer(C2S_RequestClass.TYPE, C2S_RequestClass.STREAM_CODEC, (pkt, ctx) -> {
            if (!(ctx.player() instanceof ServerPlayer sp)) return;
            String cls = getActiveClass(sp);
            PacketDistributor.sendToPlayer(sp, new S2C_ClassSync(cls, readServerExtraNbt(sp)));
        });

        // C2S: Open MM inventory
        reg.playToServer(C2S_OpenMetalmancerInventory.TYPE, C2S_OpenMetalmancerInventory.STREAM_CODEC, (pkt, ctx) -> {
            if (!(ctx.player() instanceof ServerPlayer sp)) return;
            if (!isMetalmancer(sp)) return;
            ExtraInventoryMenu.open(sp);
        });
    }

    /* ---------- client send helpers (safe: implemented in nested client-only class) ---------- */

    public static void requestOpenMetalmancerInventory() {
        Client.sendOpenMetalmancerInventory();
    }

    public static void sendActionToServer(String actionId) {
        Client.sendAction(actionId);
    }

    public static void requestSetClass(String classId) {
        Client.sendSetClass(classId);
    }

    public static void requestClassSync() {
        Client.sendRequestClass();
    }

    /**
     * This nested class only loads when those methods are called on the client.
     * Dedicated servers will not touch it.
     */
    private static final class Client {
        private Client() {}

        private static void sendOpenMetalmancerInventory() {
            sendToServer(new C2S_OpenMetalmancerInventory());
        }

        private static void sendAction(String actionId) {
            if (actionId == null) return;
            sendToServer(new C2S_Action(actionId));
        }

        private static void sendSetClass(String classId) {
            sendToServer(new C2S_SetClass(classId));
        }

        private static void sendRequestClass() {
            sendToServer(new C2S_RequestClass());
        }

        private static void sendToServer(CustomPacketPayload payload) {
            // No PacketDistributor.sendToServer in this NeoForge version.
            // Use the vanilla connection packet.
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            if (mc == null) return;

            net.minecraft.client.multiplayer.ClientPacketListener listener = mc.getConnection();
            if (listener == null) return;

            net.minecraft.network.Connection connection = listener.getConnection();
            if (connection == null) return;

            connection.send(new net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket(payload));
        }
    }
}
