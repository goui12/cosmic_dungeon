package net.goui.cosmicdungeon.crafting;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Applied only to a newly crafted output; never scans or normalizes existing item stacks. */
public final class PotionStacks {
    public static final int LIMIT = 64;
    private PotionStacks() {}
    /** Keep the exact selected bottle's authored components; only recipe/container and stack limit change. */
    public static ItemStack brewed(ItemStack source, ItemStack target) {
        if (!(source.is(Items.POTION) || source.is(Items.SPLASH_POTION) || source.is(Items.LINGERING_POTION)))
            throw new IllegalArgumentException("Expected a potion source");
        var result = source.transmuteCopy(target.getItem(), 1);
        result.set(DataComponents.POTION_CONTENTS, target.get(DataComponents.POTION_CONTENTS));
        return crafted(result);
    }
    public static ItemStack crafted(ItemStack single) {
        if (single.isEmpty() || single.getCount() != 1
                || !(single.is(Items.POTION) || single.is(Items.SPLASH_POTION) || single.is(Items.LINGERING_POTION)))
            throw new IllegalArgumentException("Expected one newly crafted potion");
        var result = single.copy();
        result.set(DataComponents.MAX_STACK_SIZE, LIMIT);
        return result;
    }
}
