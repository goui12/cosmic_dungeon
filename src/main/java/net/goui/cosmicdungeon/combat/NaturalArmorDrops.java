package net.goui.cosmicdungeon.combat;

import net.goui.cosmicdungeon.Config;
import net.minecraft.world.entity.DropChances;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;

/** Called only where vanilla creates difficulty-based armor, before authored presets apply. */
public final class NaturalArmorDrops {
    private NaturalArmorDrops() {}
    public static void generated(Mob mob, EquipmentSlot slot) {
        if (slot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR
                && mob.getDropChances().byEquipment(slot) == DropChances.DEFAULT_EQUIPMENT_DROP_CHANCE)
            mob.setDropChance(slot, Config.NATURAL_ARMOR_DROP_CHANCE.get().floatValue());
    }
}
