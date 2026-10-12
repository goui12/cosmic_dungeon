package net.goui.cosmicdungeon.crafting;

import java.util.*;
import net.goui.cosmicdungeon.playerclass.api.ClassData;
import net.goui.cosmicdungeon.playerclass.dragoon.repair.RepairCustody;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/** Inventory-menu cursor is not native player data. Capture it in the same owner save as inventory. */
public final class PotionCursorCustody {
    public static final String KEY = "potion_cursor_custody_v1";
    private static final Set<ServerPlayer> LIVE = Collections.newSetFromMap(new WeakHashMap<>());
    private PotionCursorCustody() {}
    private static CompoundTag root(ServerPlayer p) { return p.getPersistentData().getCompoundOrEmpty(ClassData.ROOT_TAG); }
    public static void restore(ServerPlayer p) {
        if (LIVE.contains(p)) return;
        var root = root(p);
        if (root.contains(KEY)) {
            if (!(root.get(KEY) instanceof CompoundTag saved) || saved.getIntOr("schema", 0) != 1
                    || !saved.getStringOr("owner", "").equals(p.getStringUUID())
                    || !(saved.get("item") instanceof CompoundTag item))
                throw new IllegalStateException("Unsupported potion cursor custody; preserve owner data");
            if (p.containerMenu != p.inventoryMenu || !p.inventoryMenu.getCarried().isEmpty())
                throw new IllegalStateException("Occupied recovery cursor");
            p.inventoryMenu.setCarried(RepairCustody.decode(p, item));
            LIVE.add(p);
        }
    }
    public static void track(ServerPlayer p) { restore(p); LIVE.add(p); }
    /** Invoked from native Entity.saveWithoutId before both Inventory and NeoForgeData serialization. */
    public static void capture(ServerPlayer p) {
        if (!LIVE.contains(p)) return; // Do not erase an unloaded or frozen recovery record.
        var root = root(p).copy();
        var cursor = p.inventoryMenu.getCarried(); // A restored cursor can remain here while another menu is open.
        if (cursor.isEmpty()) root.remove(KEY);
        else {
            var saved = root.getCompoundOrEmpty(KEY).copy();
            saved.putInt("schema", 1); saved.putString("owner", p.getStringUUID());
            saved.put("item", RepairCustody.encode(p, cursor)); root.put(KEY, saved);
        }
        p.getPersistentData().put(ClassData.ROOT_TAG, root);
    }
    /** Uncertain transactions disconnect with the cursor still owned by its paired saved image. */
    public static void freeze(ServerPlayer p) {
        if (LIVE.contains(p)) { capture(p); LIVE.remove(p); p.inventoryMenu.setCarried(ItemStack.EMPTY); }
    }
    public static CompoundTag image(ServerPlayer p) { return RepairCustody.encode(p, p.inventoryMenu.getCarried()); }
    public static void apply(ServerPlayer p, CompoundTag image) {
        track(p); p.inventoryMenu.setCarried(RepairCustody.decode(p, image));
    }
    public static void closeBeforeDeath(ServerPlayer p) {
        if (LIVE.contains(p) && !p.inventoryMenu.getCarried().isEmpty()) {
            p.closeContainer();
            if (!p.inventoryMenu.getCarried().isEmpty()) p.inventoryMenu.removed(p);
            capture(p);
        }
    }
    public static void stopped() { LIVE.clear(); }
}
