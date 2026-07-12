package net.goui.cosmicdungeon.block.custom;

import net.goui.cosmicdungeon.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class BeatrixCampfireBlock extends CampfireBlock {
    public BeatrixCampfireBlock(Properties properties) {
        super(true, 1, properties);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (stack.is(ModItems.RAW_FARROWS_CHOP.get()) && Boolean.TRUE.equals(state.getValue(LIT))) {
            if (!level.isClientSide()) {
                if (!player.getAbilities().instabuild) stack.shrink(1);
                ItemStack cooked = new ItemStack(ModItems.FARROWS_CHOP.get());
                if (!player.getInventory().add(cooked)) player.drop(cooked, false);
                level.playSound(null, pos, SoundEvents.CAMPFIRE_CRACKLE, SoundSource.BLOCKS, 1.0F, 1.0F);
            }
            return InteractionResult.SUCCESS;
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hit);
    }
}
