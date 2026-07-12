package net.goui.cosmicdungeon.item.custom;

import net.goui.cosmicdungeon.achievement.CosmicAchievementIds;
import net.goui.cosmicdungeon.achievement.CosmicAdvancementUtil;
import net.goui.cosmicdungeon.rift.DefaultRiftDestinations;
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
        if (!DefaultRiftDestinations.teleportToMainVillage(sp)) return stack;

        sp.awardStat(Stats.ITEM_USED.get(this));
        CosmicAdvancementUtil.grant(sp, CosmicAchievementIds.NOSTALGIA_BAIT);
        if (!sp.getAbilities().instabuild) stack.shrink(1);
        return stack;
    }
}
