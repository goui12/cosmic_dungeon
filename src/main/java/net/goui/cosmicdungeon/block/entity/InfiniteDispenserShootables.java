package net.goui.cosmicdungeon.block.entity;

import net.goui.cosmicdungeon.util.ModTags;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.Level;

/**
 * Server/client-neutral shootability checks for Infinite Dispenser contents.
 *
 * <p>Spawn eggs are intentionally recognized by item class instead of by a
 * generated list, so vanilla, NeoForge, and modded mob eggs can all be fired
 * without duplicating registry data or requiring a storage/network migration.</p>
 */
final class InfiniteDispenserShootables {
    private InfiniteDispenserShootables() {
    }

    static boolean isShootable(Level level, ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

        if (isBuiltInProjectile(stack) || isSpawnEgg(stack)) {
            return true;
        }

        return isConfiguredShootable(level, stack);
    }

    private static boolean isBuiltInProjectile(ItemStack stack) {
        return stack.is(Items.ARROW)
                || stack.is(Items.TIPPED_ARROW)
                || stack.is(Items.SPECTRAL_ARROW);
    }

    private static boolean isSpawnEgg(ItemStack stack) {
        return stack.getItem() instanceof SpawnEggItem;
    }

    private static boolean isConfiguredShootable(Level level, ItemStack stack) {
        try {
            var lookup = level.registryAccess().lookupOrThrow(Registries.ITEM);
            HolderSet<Item> set = lookup.getOrThrow(ModTags.Items.INFINITE_SHOOTABLES);
            return set.size() > 0 && stack.is(ModTags.Items.INFINITE_SHOOTABLES);
        } catch (RuntimeException ignored) {
            return false;
        }
    }
}
