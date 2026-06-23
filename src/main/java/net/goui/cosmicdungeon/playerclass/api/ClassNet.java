package net.goui.cosmicdungeon.playerclass.api;

import net.goui.cosmicdungeon.CosmicDungeonMod;
import net.goui.cosmicdungeon.network.ClassPayloads;
import net.goui.cosmicdungeon.network.ModNetwork;
import net.goui.cosmicdungeon.playerclass.metalmancer.MetalmancerActions;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
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

import java.util.List;
import java.util.Objects;

public final class ClassNet {
    private ClassNet() {}

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

    /* ---------- selectable classes ---------- */

    public static boolean isDisabledClassSelection(String classId) {
        return ClassKeys.CLASS_ID_METALMANCER.equals(classId)
                || ClassKeys.CLASS_ID_DEADEYE.equals(classId);
    }

    public static List<String> getSelectableClasses(ServerPlayer sp) {
        // Server-authoritative list the selector UI will display.
        // Expand/lock down later with ready-room rules if desired.
        // Disabled classes remain listed so the client can shade them instead of hiding them.
        return ClassKeys.ORDERED;
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

    /* ---------- apply helpers ---------- */

    private static void sendSync(ServerPlayer sp, String classId) {
        String cls = Objects.requireNonNullElse(classId, ClassKeys.CLASS_ID_NONE);
        CompoundTag extra = ClassKeys.CLASS_ID_METALMANCER.equals(cls) ? readServerExtraNbt(sp) : new CompoundTag();
        ModNetwork.sendTo(sp, new ClassPayloads.S2C_ClassSync(cls, extra));
    }

    private static void clearExtra(ServerPlayer sp) {
        CompoundTag pd = sp.getPersistentData();
        CompoundTag root = pd.getCompoundOrEmpty(ClassData.ROOT_TAG).copy();
        root.remove(ClassData.KEY_EXTRA);
        pd.put(ClassData.ROOT_TAG, root);
    }

    public static void enableMetalmancer(ServerPlayer sp) {
        setActiveClass(sp, ClassKeys.CLASS_ID_METALMANCER);
        seedMetalmancerExtra(sp);
        sendSync(sp, ClassKeys.CLASS_ID_METALMANCER);
    }

    public static void disableMetalmancer(ServerPlayer sp) {
        setActiveClass(sp, ClassKeys.CLASS_ID_NONE);
        clearExtra(sp);
        sendSync(sp, ClassKeys.CLASS_ID_NONE);
    }

    /** Generic class set (no special inventory systems). */
    public static void setGenericClass(ServerPlayer sp, String classId) {
        String cls = ClassKeys.clamp(classId);
        setActiveClass(sp, cls);

        // If we are leaving Metalmancer, strip its extra tag so you don't carry it across classes.
        if (!ClassKeys.CLASS_ID_METALMANCER.equals(cls)) {
            clearExtra(sp);
        }

        sendSync(sp, cls);
    }

    public static void sendFullTo(ServerPlayer sp) {
        String cls = Objects.requireNonNullElse(getActiveClass(sp), ClassKeys.CLASS_ID_NONE);
        sendSync(sp, cls);
    }

    public static void sendSelectorDataTo(ServerPlayer sp) {
        String active = Objects.requireNonNullElse(getActiveClass(sp), ClassKeys.CLASS_ID_NONE);
        ModNetwork.sendTo(sp, new ClassPayloads.S2C_SelectorData(active, getSelectableClasses(sp)));
    }

    public static String normalizeRequestedClass(ServerPlayer sp, String requested) {
        String cls = Objects.requireNonNullElse(requested, ClassKeys.CLASS_ID_NONE);
        cls = ClassKeys.clamp(cls);
        if (!getSelectableClasses(sp).contains(cls)) return ClassKeys.CLASS_ID_NONE;
        if (isDisabledClassSelection(cls)) return ClassKeys.CLASS_ID_NONE;
        return cls;
    }

    public static void applySelectedClass(ServerPlayer sp, String requested) {
        String cls = normalizeRequestedClass(sp, requested);

        if (ClassKeys.CLASS_ID_METALMANCER.equals(cls)) {
            enableMetalmancer(sp);
        } else if (ClassKeys.CLASS_ID_NONE.equals(cls)) {
            disableMetalmancer(sp);
        } else {
            setGenericClass(sp, cls);
        }
    }

    /* ---------- server-side action entry ---------- */

    public static void handleMetalmancerAction(ServerPlayer sp, String actionId) {
        if (!isMetalmancer(sp)) return;
        MetalmancerActions.handleAction(sp, actionId);
    }

    public static void openMetalmancerInventory(ServerPlayer sp) {
        if (!isMetalmancer(sp)) return;
        ExtraInventoryMenu.open(sp);
    }

    /* ---------- client send helpers ---------- */

    public static void requestClassSync() {
        ModNetwork.sendToServer(new ClassPayloads.C2S_RequestClass());
    }

    public static void requestOpenMetalmancerInventory() {
        ModNetwork.sendToServer(new ClassPayloads.C2S_OpenMetalmancerInventory());
    }

    public static void sendActionToServer(String actionId) {
        if (actionId == null) return;
        ModNetwork.sendToServer(new ClassPayloads.C2S_Action(actionId));
    }

    public static void requestSelectorData() {
        ModNetwork.sendToServer(new ClassPayloads.C2S_RequestSelectorData());
    }

    public static void requestSelectClass(String classId) {
        ModNetwork.sendToServer(new ClassPayloads.C2S_SelectClass(classId == null ? "" : classId));
    }
}
