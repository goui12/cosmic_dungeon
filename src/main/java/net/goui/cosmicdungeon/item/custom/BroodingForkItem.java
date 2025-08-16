package net.goui.cosmicdungeon.item.custom;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.function.Consumer;

public class BroodingForkItem extends Item {

    public BroodingForkItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        BlockState blockState = level.getBlockState(pos);
        ItemStack itemStack = context.getItemInHand();

        if (level.isClientSide || player == null) {
            return InteractionResult.SUCCESS;
        }

        if (blockState.is(Blocks.GRASS_BLOCK)) {
            if (removeItemFromInventory(player, Items.EGG)) {
                Chicken chicken = new Chicken(EntityType.CHICKEN, level);

                chicken.setPos(pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5);
                level.addFreshEntity(chicken);
                level.playSound(null, pos, SoundEvents.CHICKEN_AMBIENT, SoundSource.PLAYERS, 1.0f, 1.0f);
            } else {
                player.displayClientMessage(Component.literal("§cYou need an egg to summon a chicken."), true);
                return InteractionResult.FAIL;
            }

        } else if (blockState.is(Blocks.DIRT)) {
            if (removeItemFromInventory(player, Items.WHEAT)) {
                ItemStack seeds = new ItemStack(Items.WHEAT_SEEDS, 16);
                if (!player.getInventory().add(seeds)) {
                    player.drop(seeds, false);
                }
                level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.5f, 1.2f);
            } else {
                player.displayClientMessage(Component.literal("§cYou need wheat to transmute into seeds."), true);
                return InteractionResult.FAIL;
            }
        }
        player.getCooldowns().addCooldown(context.getItemInHand(), 20);






        return InteractionResult.SUCCESS;
    }

    private boolean removeItemFromInventory(Player player, Item itemToRemove) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(itemToRemove)) {
                stack.shrink(1);
                return true;
            }
        }
        return false;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltipConsumer, TooltipFlag tooltipFlag) {
        if (Screen.hasShiftDown()) {
            tooltipConsumer.accept(Component.translatable("tooltip.cosmicdungeon.brooding_fork.shift_down"));
        } else {
            tooltipConsumer.accept(Component.translatable("tooltip.cosmicdungeon.brooding_fork"));
        }
    }
}
