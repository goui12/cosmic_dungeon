package net.goui.cosmicdungeon.item.custom;

import net.goui.cosmicdungeon.block.ModBlocks;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.Map;
import java.util.function.Consumer;

public class ChiselItem extends Item {
    private static final Map<Block, Block> CHISEL_MAP =
            Map.of(
                    Blocks.STONE, Blocks.STONE_BRICKS,
                    Blocks.END_STONE, Blocks.END_STONE_BRICKS,
                    Blocks.DEEPSLATE, Blocks.DEEPSLATE_BRICKS,
                    Blocks.GOLD_BLOCK, Blocks.IRON_BLOCK,
                    Blocks.IRON_BLOCK, Blocks.STONE,
                    Blocks.NETHERRACK, ModBlocks.BISMUTH_BLOCK.get()
            );

    public ChiselItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Block clickedBlock = level.getBlockState(context.getClickedPos()).getBlock();

        if (CHISEL_MAP.containsKey(clickedBlock)) {
            if (!level.isClientSide()) {
                // Replace block
                level.setBlockAndUpdate(context.getClickedPos(), CHISEL_MAP.get(clickedBlock).defaultBlockState());

                // Damage the tool (null-safe for player)
                ServerLevel server = (ServerLevel) level;
                context.getItemInHand().hurtAndBreak(1, server, context.getPlayer(), brokenItem -> {
                    if (context.getPlayer() != null) {
                        context.getPlayer().onEquippedItemBroken(brokenItem, EquipmentSlot.MAINHAND);
                    }
                });

                // Play feedback sound
                level.playSound(null, context.getClickedPos(), SoundEvents.GRINDSTONE_USE, SoundSource.BLOCKS);
            }
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

    // 1.21.9+ tooltip override (new signature)
    @Override
    public void appendHoverText(ItemStack stack,
                                Item.TooltipContext ctx,
                                TooltipDisplay display,
                                Consumer<Component> adder,
                                TooltipFlag flag) {
        boolean showMore = flag.isAdvanced(); // F3+H
        if (showMore) {
            adder.accept(Component.translatable("tooltip.cosmicdungeon.chisel.shift_down"));
        } else {
            adder.accept(Component.translatable("tooltip.cosmicdungeon.chisel"));
        }
    }
}
