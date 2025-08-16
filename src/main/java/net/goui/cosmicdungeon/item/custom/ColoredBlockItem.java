package net.goui.cosmicdungeon.item.custom;

import net.goui.cosmicdungeon.common.color.AmethystColor;
import net.goui.cosmicdungeon.common.properties.ModProperties;
import net.goui.cosmicdungeon.component.ModDataComponents;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.network.chat.Component;

/**
 * BlockItem that remembers and applies an AmethystColor when placed.
 */
public class ColoredBlockItem extends BlockItem {

    public ColoredBlockItem(Block block, Properties props) {
        super(block, props);
    }

    @Override
    protected BlockState getPlacementState(BlockPlaceContext ctx) {
        BlockState base = super.getPlacementState(ctx);
        if (base == null) return null;

        // Only set color if block supports it
        if (base.hasProperty(ModProperties.COLOR)) {
            ItemStack stack = ctx.getItemInHand();
            AmethystColor c = stack.getOrDefault(
                    ModDataComponents.AMETHYST_COLOR.get(),
                    AmethystColor.PURPLE // default if none set
            );
            return base.setValue(ModProperties.COLOR, c);
        }
        return base;
    }
    @Override
    public Component getName(ItemStack stack) {
        AmethystColor color = stack.get(ModDataComponents.AMETHYST_COLOR.get());
        if (color == null) {
            return super.getName(stack);
        }
        // Uses a lang key like: item.cosmicdungeon.colored_amethyst_block.rose
        return Component.translatable(
                this.getDescriptionId() + "." + color.getSerializedName()
        );
    }

}
