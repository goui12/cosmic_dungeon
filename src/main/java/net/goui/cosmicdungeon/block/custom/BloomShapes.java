package net.goui.cosmicdungeon.block.custom;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.VoxelShape;

final class BloomShapes {
    static final VoxelShape FLOWER = Block.box(2.0D, 0.0D, 2.0D, 14.0D, 13.0D, 14.0D);

    private BloomShapes() {
    }
}
