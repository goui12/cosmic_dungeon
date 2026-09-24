package net.goui.cosmicdungeon.client;

import net.goui.cosmicdungeon.block.custom.ClassSelectorShape;
import net.goui.cosmicdungeon.playerclass.api.ClassKeys;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Pure, bounded presentation rules; never performs another world raycast or changes player data. */
public final class ClassSelectorHoverSelection {
    public static final double MAX_REACH = 5.0;

    public boolean canShow(BlockPos selector, HitResult target, Vec3 eye, Vec3 direction, double playerReach) {
        if (!(target instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK
                || !selector.equals(hit.getBlockPos()) || !finite(eye) || !finite(direction)
                || !Double.isFinite(playerReach) || playerReach <= 0) return false;
        double lengthSquared = direction.lengthSqr();
        if (!Double.isFinite(lengthSquared) || lengthSquared < 1.0e-12) return false;
        double reach = Math.min(playerReach, MAX_REACH);
        if (!finite(hit.getLocation()) || eye.distanceToSqr(hit.getLocation()) > reach * reach) return false;
        // The existing selector pick may lower an upper-tip hit for the server packet envelope.
        // Check the real fitted surface too, within this strictly bounded segment.
        Vec3 end = eye.add(direction.normalize().scale(reach));
        return ClassSelectorShape.INSTANCE.shape().clip(eye, end, selector) != null;
    }

    public Component label(String syncedClassId) {
        return Component.literal("Class: ").append(
                Component.translatable("playerclass.cosmicdungeon." + ClassKeys.clamp(syncedClassId)));
    }

    private boolean finite(Vec3 vector) {
        return vector != null && Double.isFinite(vector.x) && Double.isFinite(vector.y) && Double.isFinite(vector.z);
    }
}
