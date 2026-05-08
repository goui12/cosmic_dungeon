package net.minecraft.world.level.block.entity;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.coppergolem.CopperGolem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.level.block.CopperGolemStatueBlock;
import net.minecraft.world.level.block.state.BlockState;

public class CopperGolemStatueBlockEntity extends BlockEntity {
    public CopperGolemStatueBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityType.COPPER_GOLEM_STATUE, pos, state);
    }

    public void createStatue(CopperGolem golem) {
        this.setComponents(DataComponentMap.builder().addAll(this.components()).set(DataComponents.CUSTOM_NAME, golem.getCustomName()).build());
        super.setChanged();
    }

    @Nullable
    public CopperGolem removeStatue(BlockState state) {
        CopperGolem coppergolem = EntityType.COPPER_GOLEM.create(this.level, EntitySpawnReason.TRIGGERED);
        if (coppergolem != null) {
            coppergolem.setCustomName(this.components().get(DataComponents.CUSTOM_NAME));
            return this.initCopperGolem(state, coppergolem);
        } else {
            return null;
        }
    }

    private CopperGolem initCopperGolem(BlockState blockState, CopperGolem copperGolem) {
        BlockPos blockpos = this.getBlockPos();
        copperGolem.snapTo(blockpos.getCenter().x, blockpos.getY(), blockpos.getCenter().z, blockState.getValue(CopperGolemStatueBlock.FACING).toYRot(), 0.0F);
        copperGolem.yHeadRot = copperGolem.getYRot();
        copperGolem.yBodyRot = copperGolem.getYRot();
        copperGolem.playSpawnSound();
        return copperGolem;
    }

    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    public ItemStack getItem(ItemStack stack, CopperGolemStatueBlock.Pose pose) {
        stack.applyComponents(this.collectComponents());
        stack.set(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY.with(CopperGolemStatueBlock.POSE, pose));
        return stack;
    }
}
