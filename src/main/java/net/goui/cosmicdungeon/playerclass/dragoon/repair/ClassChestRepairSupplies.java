package net.goui.cosmicdungeon.playerclass.dragoon.repair;

import net.goui.cosmicdungeon.block.custom.ClassLocked;
import net.goui.cosmicdungeon.block.entity.ClassLockedChestBlockEntity;
import net.goui.cosmicdungeon.dungeon.d1.D1Members;
import net.goui.cosmicdungeon.item.identity.ItemMovementRules;
import net.goui.cosmicdungeon.item.identity.ItemProvenanceService;
import net.goui.cosmicdungeon.playerclass.api.ClassKeys;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.enchantment.ItemEnchantments;

/**
 * Cameron 2026-09-20: reconcile existing D1 supplies with Dad's latest Repair2.0 rules.
 * Dragoon Class Chests 1E6YgHK0CEpirhbvAmy9Ihg9oQpr-p_YUymbUbDEqhVI lists raw materials;
 * Repair2.0 1Gbcq7Piqg2uHO1smx5oHOyeGxoT9g93cH-_G8WvhOoo (2026-08-18) requires marked stock
 * and explicitly rejects unmarked substitutes. The run's Dragoon chest is the trusted
 * supply context. Preserve authored templates; classify copies in an active D1 instance.
 */
public final class ClassChestRepairSupplies {
    private ClassChestRepairSupplies() {}

    public static void prepare(ServerPlayer player, ClassLockedChestBlockEntity chest) {
        if (!chest.canOpen(player) || !(chest.getBlockState().getBlock() instanceof ClassLocked locked)
                || !ClassKeys.CLASS_ID_DRAGOON.equals(locked.requiredClassId())) return;
        var run = D1Members.run(player.level()).orElse(null);
        if (run == null || !D1Members.inside(player, run)) return;
        int changed = 0;
        // Fixed27-slot event-driven work, never an inventory/world tick scan.
        for (int index = 0; index < chest.getContainerSize(); index++) {
            var original = chest.getItem(index);
            // Container.setItem clamps oversized stacks; retain ambiguous legacy counts intact.
            if (original.isEmpty() || !ChestRepairSupplyRules.countFits(
                    original.getCount(), chest.getMaxStackSize(original))) continue;
            String key = ChestRepairSupplyRules.key(BuiltInRegistries.ITEM.getKey(original.getItem()).toString(),
                    RepairComponents.marked(original), ItemMovementRules.flags(original).privateStorage()
                            || original.has(DataComponents.CONTAINER) || original.has(DataComponents.BUNDLE_CONTENTS)
                            || original.has(DataComponents.CUSTOM_DATA),
                    ItemProvenanceService.present(original),
                    !original.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY).isEmpty()
                            || !original.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY).isEmpty(),
                    original.getDamageValue() != 0);
            if (key == null) continue;
            var marked = original.copy(); // Retain exact count, names, lore and unrelated components.
            RepairComponents.mark(marked, key);
            chest.setItem(index, marked);
            changed++;
        }
        if (changed > 0) {
            chest.setChanged();
            com.mojang.logging.LogUtils.getLogger().info(
                    "Prepared {} D1 Dragoon repair supply stacks at {} {} for run {}",
                    changed, player.level().dimension().location(), chest.getBlockPos(), run.runId());
        }
    }

    // TODO(R01/M23, licensed TEST): open an old authored Dragoon chest in each leased D1 slot,
    // then repair with its supplies, split/shift them, cancel/restart a repair and reload the chest.
    // Verify exact quantities/components and idempotent marking; templates/developer visits,
    // other classes/dungeons, protected identities, malformed markers and weapons stay untouched.
    // This source-strict choice supersedes D08's no-marking answer under Cameron's new direction;
    // it never weakens global component, ingredient, fuel, equipment or vendor validation.
}
