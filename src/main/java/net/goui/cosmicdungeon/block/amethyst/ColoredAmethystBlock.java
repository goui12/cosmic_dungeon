package net.goui.cosmicdungeon.block.amethyst;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;

public class ColoredAmethystBlock extends Block {
    public ColoredAmethystBlock(Properties props) {
        super(props.strength(1.5F, 6.0F).requiresCorrectToolForDrops().sound(SoundType.AMETHYST));
    }
}
