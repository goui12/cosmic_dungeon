package net.minecraft.world.entity;

import com.mojang.serialization.Codec;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.Util;
import net.minecraft.util.ExtraCodecs;

public record DropChances(Map<EquipmentSlot, Float> byEquipment) {
    public static final float DEFAULT_EQUIPMENT_DROP_CHANCE = 0.085F;
    public static final float PRESERVE_ITEM_DROP_CHANCE_THRESHOLD = 1.0F;
    public static final int PRESERVE_ITEM_DROP_CHANCE = 2;
    public static final DropChances DEFAULT = new DropChances(Util.makeEnumMap(EquipmentSlot.class, p_393572_ -> 0.085F));
    public static final Codec<DropChances> CODEC = Codec.unboundedMap(EquipmentSlot.CODEC, ExtraCodecs.NON_NEGATIVE_FLOAT)
        .xmap(DropChances::toEnumMap, DropChances::filterDefaultValues)
        .xmap(DropChances::new, DropChances::byEquipment);

    private static Map<EquipmentSlot, Float> filterDefaultValues(Map<EquipmentSlot, Float> chances) {
        Map<EquipmentSlot, Float> map = new HashMap<>(chances);
        map.values().removeIf(p_393748_ -> p_393748_ == 0.085F);
        return map;
    }

    private static Map<EquipmentSlot, Float> toEnumMap(Map<EquipmentSlot, Float> chances) {
        return Util.makeEnumMap(EquipmentSlot.class, p_394429_ -> chances.getOrDefault(p_394429_, 0.085F));
    }

    public DropChances withGuaranteedDrop(EquipmentSlot slot) {
        return this.withEquipmentChance(slot, 2.0F);
    }

    public DropChances withEquipmentChance(EquipmentSlot slot, float chance) {
        if (chance < 0.0F) {
            throw new IllegalArgumentException("Tried to set invalid equipment chance " + chance + " for " + slot);
        } else {
            return this.byEquipment(slot) == chance
                ? this
                : new DropChances(Util.makeEnumMap(EquipmentSlot.class, p_393769_ -> p_393769_ == slot ? chance : this.byEquipment(p_393769_)));
        }
    }

    public float byEquipment(EquipmentSlot slot) {
        return this.byEquipment.getOrDefault(slot, 0.085F);
    }

    public boolean isPreserved(EquipmentSlot slot) {
        return this.byEquipment(slot) > 1.0F;
    }
}
