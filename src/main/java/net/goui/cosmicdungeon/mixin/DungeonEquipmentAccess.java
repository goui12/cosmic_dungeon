package net.goui.cosmicdungeon.mixin;

import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(LivingEntity.class)
public interface DungeonEquipmentAccess {
    @Invoker("detectEquipmentUpdates")
    void cosmicdungeon$detectEquipmentUpdates();
}
