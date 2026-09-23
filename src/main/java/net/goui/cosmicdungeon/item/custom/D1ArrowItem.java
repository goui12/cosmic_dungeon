package net.goui.cosmicdungeon.item.custom;

import javax.annotation.Nullable;
import net.goui.cosmicdungeon.entity.D1ArrowEntity;
import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Dedicated D1 ammunition; effects and permissions remain in the shared server ability service. */
public final class D1ArrowItem extends ArrowItem {
    public D1ArrowItem(Properties properties) { super(properties); }

    @Override
    public AbstractArrow createArrow(Level level, ItemStack ammo, LivingEntity shooter,
                                     @Nullable ItemStack weapon) {
        return new D1ArrowEntity(level, shooter, ammo.copyWithCount(1), weapon);
    }

    @Override
    public Projectile asProjectile(Level level, Position pos, ItemStack stack, Direction direction) {
        var arrow = new D1ArrowEntity(level, pos.x(), pos.y(), pos.z(), stack.copyWithCount(1), null);
        arrow.pickup = AbstractArrow.Pickup.ALLOWED;
        return arrow;
    }
}
