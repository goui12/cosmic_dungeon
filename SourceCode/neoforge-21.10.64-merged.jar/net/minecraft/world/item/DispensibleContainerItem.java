package net.minecraft.world.item;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;

public interface DispensibleContainerItem extends net.neoforged.neoforge.common.extensions.IDispensibleContainerItemExtension {
    default void checkExtraContent(@Nullable LivingEntity entity, Level level, ItemStack stack, BlockPos pos) {
    }

    @Deprecated //Forge: use the ItemStack sensitive version
    boolean emptyContents(@Nullable LivingEntity entity, Level level, BlockPos pos, @Nullable BlockHitResult hitResult);
}
