package net.goui.cosmicdungeon.item.custom;

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
        ItemStack stackInHand = context.getItemInHand();

        // Client returns immediately; server does the actions
        if (level.isClientSide() || player == null) {
            return InteractionResult.SUCCESS;
        }

        if (blockState.is(Blocks.GRASS_BLOCK)) {
            if (removeItemFromInventory(player, Items.EGG)) {
                // 1) Use the entity constructor instead of EntityType#create
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

        // 2) Cooldowns now accept ItemStack
        player.getCooldowns().addCooldown(stackInHand, 20);

        return InteractionResult.SUCCESS;
    }

    private boolean removeItemFromInventory(Player player, Item itemToRemove) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack slot = player.getInventory().getItem(i);
            if (slot.is(itemToRemove)) {
                slot.shrink(1);
                return true;
            }
        }
        return false;
    }

    // New 1.21.9 signature for hover text:
    // appendHoverText(ItemStack, Item.TooltipContext, TooltipDisplay, Consumer<Component>, TooltipFlag)
    @Override
    public void appendHoverText(ItemStack stack,
                                Item.TooltipContext ctx,
                                TooltipDisplay display,
                                Consumer<Component> adder,
                                TooltipFlag flag) {
        boolean showMore = flag.isAdvanced(); // F3+H
        if (showMore) {
            adder.accept(Component.translatable("tooltip.cosmicdungeon.brooding_fork.shift_down"));
        } else {
            adder.accept(Component.translatable("tooltip.cosmicdungeon.brooding_fork"));
        }
    }
}
