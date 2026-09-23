package net.goui.cosmicdungeon.item.custom;

import net.goui.cosmicdungeon.dungeon.FarrowsChopTravelService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class FarrowsChopItem extends Item {
    public FarrowsChopItem(Properties properties) {
        super(properties);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!(entity instanceof ServerPlayer sp)) return stack;
        var hand=sp.getUsedItemHand();
        if (!FarrowsChopTravelService.returnToDungeon(sp, stack))
            return net.goui.cosmicdungeon.dungeon.ChopTravelRecovery.blocked(sp)?sp.getItemInHand(hand):stack;

        sp.awardStat(Stats.ITEM_USED.get(this));
        return sp.getItemInHand(hand);
    }
}
