// file: net/goui/cosmicdungeon/playerclass/ore/SatchelApi.java
package net.goui.cosmicdungeon.playerclass.ore;

import net.goui.cosmicdungeon.playerclass.api.ClassData;
import net.goui.cosmicdungeon.playerclass.api.ClassNet;
import net.goui.cosmicdungeon.playerclass.api.ExtraInventoryMenu;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;

public final class SatchelApi {
    private SatchelApi() {}

    // Keep this in sync with ExtraInventoryMenu / HotbarOverlay
    // TODO: If you add per-class different extra sizes, this will need to move into ClassData or a class definition.
    private static final int EXTRA_SLOT_COUNT = 3;

    /* -------- MAIN INVENTORY -------- */

    // TODO: when you add more satchel types, consider an AbstractSatchelItem base
    // and check instanceof that instead of SatchelOfSamplesItem directly.
    public static ItemStack findSatchelInMainInv(Player p) {
        var inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.getItem() instanceof SatchelOfSamplesItem) return s;
        }
        return ItemStack.EMPTY;
    }

    private static void syncMainInv(Player p) {
        p.getInventory().setChanged();
    }

    /* -------- EXTRA: OPEN MENU (authoritative while open) -------- */

    private static ItemStack findSatchelInOpenMenu(ServerPlayer sp) {
        ExtraInventoryMenu m = ExtraInventoryMenu.getOpen(sp);
        if (m == null) return ItemStack.EMPTY;
        var cont = m.getExtraContainer();
        for (int i = 0; i < cont.getContainerSize(); i++) {
            ItemStack s = cont.getItem(i);
            if (!s.isEmpty() && s.getItem() instanceof SatchelOfSamplesItem) return s;
        }
        return ItemStack.EMPTY;
    }

    private static boolean mutateSatchelInOpenMenu(ServerPlayer sp,
                                                   java.util.function.IntUnaryOperator oreMutator,
                                                   boolean mutateCapacity,
                                                   java.util.function.IntUnaryOperator capMutator) {
        ExtraInventoryMenu m = ExtraInventoryMenu.getOpen(sp);
        if (m == null) return false;
        var cont = m.getExtraContainer();

        for (int i = 0; i < cont.getContainerSize(); i++) {
            ItemStack s = cont.getItem(i);
            if (!s.isEmpty() && s.getItem() instanceof SatchelOfSamplesItem) {
                int ore = SatchelOfSamplesItem.getOre(s);
                int cap = SatchelOfSamplesItem.getCapacity(s);

                if (mutateCapacity) cap = Math.max(1, capMutator.applyAsInt(cap));
                int newOre = Math.max(0, oreMutator.applyAsInt(ore));
                if (cap <= 0) cap = SatchelOfSamplesItem.DEFAULT_CAPACITY;
                if (newOre > cap) newOre = cap;

                boolean changed = false;
                if (mutateCapacity && SatchelOfSamplesItem.getCapacity(s) != cap) {
                    SatchelOfSamplesItem.setCapacity(s, cap);
                    changed = true;
                }
                if (SatchelOfSamplesItem.getOre(s) != newOre) {
                    SatchelOfSamplesItem.setOre(s, newOre);
                    changed = true;
                }
                if (changed) {
                    cont.setItem(i, s);
                    sp.containerMenu.broadcastChanges();
                }
                return true;
            }
        }
        return false;
    }

    /* -------- EXTRA: PERSISTENT DATA mirror (when menu is NOT open) -------- */

    private static NonNullList<ItemStack> readExtraList(Player p) {
        CompoundTag root = p.getPersistentData().getCompoundOrEmpty(ClassData.ROOT_TAG);
        CompoundTag extraTag = root.getCompound(ClassData.KEY_EXTRA).orElseGet(CompoundTag::new);

        NonNullList<ItemStack> list = NonNullList.withSize(EXTRA_SLOT_COUNT, ItemStack.EMPTY);
        ValueInput in = TagValueInput.create(ProblemReporter.DISCARDING, p.level().registryAccess(), extraTag);
        ContainerHelper.loadAllItems(in, list);
        return list;
    }

    private static void writeExtraList(ServerPlayer sp, NonNullList<ItemStack> list) {
        TagValueOutput out = TagValueOutput.createWithoutContext(ProblemReporter.DISCARDING);
        ContainerHelper.saveAllItems(out, list);
        CompoundTag newExtra = out.buildResult();

        CompoundTag newRoot = sp.getPersistentData().getCompoundOrEmpty(ClassData.ROOT_TAG).copy();
        newRoot.put(ClassData.KEY_EXTRA, newExtra);
        sp.getPersistentData().put(ClassData.ROOT_TAG, newRoot);

        // Push to client overlays/tooltips (outside of the open-menu path)
        ClassNet.sendFullTo(sp);
    }

    private static int indexOfSatchel(NonNullList<ItemStack> list) {
        for (int i = 0; i < list.size(); i++) {
            ItemStack s = list.get(i);
            if (!s.isEmpty() && s.getItem() instanceof SatchelOfSamplesItem) return i;
        }
        return -1;
    }

    /* -------- PUBLIC ANYWHERE API -------- */

    public static int get(Player p) {
        ItemStack inInv = findSatchelInMainInv(p);
        if (!inInv.isEmpty()) return SatchelOfSamplesItem.getOre(inInv);

        if (p instanceof ServerPlayer sp) {
            ItemStack live = findSatchelInOpenMenu(sp);
            if (!live.isEmpty()) return SatchelOfSamplesItem.getOre(live);
        }

        var list = readExtraList(p);
        int idx = indexOfSatchel(list);
        return (idx >= 0) ? SatchelOfSamplesItem.getOre(list.get(idx)) : 0;
    }

    public static int capacity(Player p) {
        ItemStack inInv = findSatchelInMainInv(p);
        if (!inInv.isEmpty()) return SatchelOfSamplesItem.getCapacity(inInv);

        if (p instanceof ServerPlayer sp) {
            ItemStack live = findSatchelInOpenMenu(sp);
            if (!live.isEmpty()) return SatchelOfSamplesItem.getCapacity(live);
        }

        var list = readExtraList(p);
        int idx = indexOfSatchel(list);
        return (idx >= 0) ? SatchelOfSamplesItem.getCapacity(list.get(idx)) : 0;
    }

    /**
     * Dev / helper API: set ore in the satchel to a specific value,
     * clamped to [0, capacity]. Works if satchel is in main inventory,
     * in the open extra menu, or in the PD mirror.
     */
    public static void set(Player p, int value) {
        int target = Math.max(0, value);

        // Prefer main inventory
        ItemStack inInv = findSatchelInMainInv(p);
        if (!inInv.isEmpty()) {
            int cap = SatchelOfSamplesItem.getCapacity(inInv);
            if (cap <= 0) cap = SatchelOfSamplesItem.DEFAULT_CAPACITY;
            int newOre = Math.min(target, cap);
            SatchelOfSamplesItem.setOre(inInv, newOre);
            syncMainInv(p);
            return;
        }

        if (p instanceof ServerPlayer sp) {
            final int t = target;

            // If menu is open, mutate the live container
            boolean changed = mutateSatchelInOpenMenu(sp, ore -> t, false, null);
            if (changed) return;

            // Else PD mirror
            var list = readExtraList(sp);
            int idx = indexOfSatchel(list);
            if (idx < 0) return;

            ItemStack satchel = list.get(idx);
            int cap = SatchelOfSamplesItem.getCapacity(satchel);
            if (cap <= 0) cap = SatchelOfSamplesItem.DEFAULT_CAPACITY;
            int newOre = Math.min(t, cap);
            SatchelOfSamplesItem.setOre(satchel, newOre);
            list.set(idx, satchel);
            writeExtraList(sp, list);
        }
    }

    public static boolean trySpend(Player p, int cost) {
        if (cost <= 0) return true;

        ItemStack inInv = findSatchelInMainInv(p);
        if (!inInv.isEmpty()) {
            int have = SatchelOfSamplesItem.getOre(inInv);
            if (have < cost) return false;
            SatchelOfSamplesItem.setOre(inInv, have - cost);
            syncMainInv(p);
            return true;
        }

        if (p instanceof ServerPlayer sp) {
            ExtraInventoryMenu m = ExtraInventoryMenu.getOpen(sp);
            if (m != null) {
                var cont = m.getExtraContainer();
                for (int i = 0; i < cont.getContainerSize(); i++) {
                    ItemStack s = cont.getItem(i);
                    if (s.isEmpty() || !(s.getItem() instanceof SatchelOfSamplesItem)) continue;

                    int have = SatchelOfSamplesItem.getOre(s);
                    if (have < cost) return false;

                    SatchelOfSamplesItem.setOre(s, have - cost);
                    cont.setItem(i, s);
                    sp.containerMenu.broadcastChanges();
                    return true;
                }
                return false;
            }

            var list = readExtraList(sp);
            int idx = indexOfSatchel(list);
            if (idx < 0) return false;

            ItemStack satchel = list.get(idx);
            int have = SatchelOfSamplesItem.getOre(satchel);
            if (have < cost) return false;

            SatchelOfSamplesItem.setOre(satchel, have - cost);
            list.set(idx, satchel);
            writeExtraList(sp, list);
            return true;
        }

        return false;
    }

    public static void add(Player p, int amount) {
        if (amount <= 0) return;

        // Prefer main inventory
        ItemStack inInv = findSatchelInMainInv(p);
        if (!inInv.isEmpty()) {
            int ore = SatchelOfSamplesItem.getOre(inInv);
            int cap = SatchelOfSamplesItem.getCapacity(inInv);
            if (cap <= 0) cap = SatchelOfSamplesItem.DEFAULT_CAPACITY;
            int newOre = Math.min(cap, Math.max(0, ore + amount));
            if (newOre != ore) {
                SatchelOfSamplesItem.setOre(inInv, newOre);
                syncMainInv(p);
            }
            return;
        }

        if (p instanceof ServerPlayer sp) {
            // If menu open, mutate live container + broadcast
            boolean changed = mutateSatchelInOpenMenu(sp, ore -> ore + amount, false, null);
            if (changed) return;

            // Else PD mirror
            var list = readExtraList(sp);
            int idx = indexOfSatchel(list);
            if (idx < 0) return;
            ItemStack satchel = list.get(idx);
            int ore = SatchelOfSamplesItem.getOre(satchel);
            int cap = SatchelOfSamplesItem.getCapacity(satchel);
            if (cap <= 0) cap = SatchelOfSamplesItem.DEFAULT_CAPACITY;
            int newOre = Math.min(cap, Math.max(0, ore + amount));
            if (newOre != ore) {
                SatchelOfSamplesItem.setOre(satchel, newOre);
                list.set(idx, satchel);
                writeExtraList(sp, list);
            }
        }
    }

    public static void setCapacity(Player p, int cap) {
        int clamped = Math.max(1, cap);

        ItemStack inInv = findSatchelInMainInv(p);
        if (!inInv.isEmpty()) {
            SatchelOfSamplesItem.setCapacity(inInv, clamped);
            syncMainInv(p);
            return;
        }

        if (p instanceof ServerPlayer sp) {
            if (mutateSatchelInOpenMenu(sp, ore -> ore, true, oldCap -> clamped)) return;

            var list = readExtraList(sp);
            int idx = indexOfSatchel(list);
            if (idx < 0) return;
            ItemStack satchel = list.get(idx);
            SatchelOfSamplesItem.setCapacity(satchel, clamped);
            list.set(idx, satchel);
            writeExtraList(sp, list);
        }
    }
}
