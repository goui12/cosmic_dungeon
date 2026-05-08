package net.minecraft.world.entity;

import com.mojang.serialization.Codec;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.Map.Entry;
import net.minecraft.world.item.ItemStack;

public class EntityEquipment {
    public static final Codec<EntityEquipment> CODEC = Codec.unboundedMap(EquipmentSlot.CODEC, ItemStack.CODEC).xmap(p_398046_ -> {
        EnumMap<EquipmentSlot, ItemStack> enummap = new EnumMap<>(EquipmentSlot.class);
        enummap.putAll((Map<? extends EquipmentSlot, ? extends ItemStack>)p_398046_);
        return new EntityEquipment(enummap);
    }, p_397308_ -> {
        Map<EquipmentSlot, ItemStack> map = new EnumMap<>(p_397308_.items);
        map.values().removeIf(ItemStack::isEmpty);
        return map;
    });
    private final EnumMap<EquipmentSlot, ItemStack> items;

    private EntityEquipment(EnumMap<EquipmentSlot, ItemStack> items) {
        this.items = items;
    }

    public EntityEquipment() {
        this(new EnumMap<>(EquipmentSlot.class));
    }

    public ItemStack set(EquipmentSlot slot, ItemStack stack) {
        return Objects.requireNonNullElse(this.items.put(slot, stack), ItemStack.EMPTY);
    }

    public ItemStack get(EquipmentSlot slot) {
        return this.items.getOrDefault(slot, ItemStack.EMPTY);
    }

    public boolean isEmpty() {
        for (ItemStack itemstack : this.items.values()) {
            if (!itemstack.isEmpty()) {
                return false;
            }
        }

        return true;
    }

    public void tick(Entity entity) {
        for (Entry<EquipmentSlot, ItemStack> entry : this.items.entrySet()) {
            ItemStack itemstack = entry.getValue();
            if (!itemstack.isEmpty()) {
                itemstack.inventoryTick(entity.level(), entity, entry.getKey());
            }
        }
    }

    public void setAll(EntityEquipment equipment) {
        this.items.clear();
        this.items.putAll(equipment.items);
    }

    public void dropAll(LivingEntity entity) {
        for (ItemStack itemstack : this.items.values()) {
            entity.drop(itemstack, true, false);
        }

        this.clear();
    }

    public void clear() {
        this.items.replaceAll((p_401733_, p_401734_) -> ItemStack.EMPTY);
    }
}
