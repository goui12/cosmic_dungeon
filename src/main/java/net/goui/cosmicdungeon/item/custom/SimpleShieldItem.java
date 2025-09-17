package net.goui.cosmicdungeon.item.custom;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.level.Level;

public class SimpleShieldItem extends Item {
    public SimpleShieldItem(Properties props) {
        // tweak durability as you like; vanilla shield is 336
        super(props.stacksTo(1).durability(336));
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        player.startUsingItem(hand);
        return InteractionResult.CONSUME; // <- prevents that initial swipe
    }


    // Hold block effectively forever (like vanilla)
    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 72000;
    }

    // Show the blocking pose while in use
    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.BLOCK;
    }

    // We’re not applying cooldowns/components on release here (return false)
    @Override
    public boolean releaseUsing(ItemStack stack, Level level, LivingEntity user, int timeLeft) {
        return false;
    }
}
