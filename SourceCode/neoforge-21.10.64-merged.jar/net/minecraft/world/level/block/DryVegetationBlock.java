package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.sounds.AmbientDesertBlockSoundsPlayer;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class DryVegetationBlock extends VegetationBlock {
    public static final MapCodec<DryVegetationBlock> CODEC = simpleCodec(DryVegetationBlock::new);
    private static final VoxelShape SHAPE = Block.column(12.0, 0.0, 13.0);

    @Override
    public MapCodec<? extends DryVegetationBlock> codec() {
        return CODEC;
    }

    public DryVegetationBlock(BlockBehaviour.Properties p_401864_) {
        super(p_401864_);
    }

    @Override
    protected VoxelShape getShape(BlockState p_401767_, BlockGetter p_401764_, BlockPos p_401758_, CollisionContext p_401896_) {
        return SHAPE;
    }

    @Override
    protected boolean mayPlaceOn(BlockState p_401820_, BlockGetter p_401945_, BlockPos p_401852_) {
        return p_401820_.is(BlockTags.DRY_VEGETATION_MAY_PLACE_ON);
    }

    @Override
    public void animateTick(BlockState p_401875_, Level p_401809_, BlockPos p_401789_, RandomSource p_401918_) {
        AmbientDesertBlockSoundsPlayer.playAmbientDeadBushSounds(p_401809_, p_401789_, p_401918_);
    }
}
