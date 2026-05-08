package net.minecraft.core.dispenser;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.phys.AABB;

public class EquipmentDispenseItemBehavior extends DefaultDispenseItemBehavior {
    public static final EquipmentDispenseItemBehavior INSTANCE = new EquipmentDispenseItemBehavior();

    @Override
    protected ItemStack execute(BlockSource blockSource, ItemStack item) {
        return dispenseEquipment(blockSource, item) ? item : super.execute(blockSource, item);
    }

    public static boolean dispenseEquipment(BlockSource blockSource, ItemStack item) {
        BlockPos blockpos = blockSource.pos().relative(blockSource.state().getValue(DispenserBlock.FACING));
        List<LivingEntity> list = blockSource.level()
            .getEntitiesOfClass(LivingEntity.class, new AABB(blockpos), p_371713_ -> p_371713_.canEquipWithDispenser(item));
        if (list.isEmpty()) {
            return false;
        } else {
            LivingEntity livingentity = list.getFirst();
            EquipmentSlot equipmentslot = livingentity.getEquipmentSlotForItem(item);
            // Neo: Respect IItemExtension#canEquip in dispenseArmor
            if (!item.canEquip(equipmentslot, livingentity)) return false;
            ItemStack itemstack = item.split(1);
            livingentity.setItemSlot(equipmentslot, itemstack);
            if (livingentity instanceof Mob mob) {
                mob.setGuaranteedDrop(equipmentslot);
                mob.setPersistenceRequired();
            }

            return true;
        }
    }
}
