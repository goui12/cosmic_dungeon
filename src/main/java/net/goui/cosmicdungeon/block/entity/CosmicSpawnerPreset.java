package net.goui.cosmicdungeon.block.entity;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

public final class CosmicSpawnerPreset {
    public static final int PRESET_VERSION = 1;

    public enum Slot {
        MAINHAND(EquipmentSlot.MAINHAND, "mainhand", 0, true),
        OFFHAND(EquipmentSlot.OFFHAND, "offhand", 1, true),
        HEAD(EquipmentSlot.HEAD, "head", 0, false),
        CHEST(EquipmentSlot.CHEST, "chest", 1, false),
        LEGS(EquipmentSlot.LEGS, "legs", 2, false),
        FEET(EquipmentSlot.FEET, "feet", 3, false);

        public final EquipmentSlot equipmentSlot;
        public final String id;
        public final int dropIndex;
        public final boolean hand;

        Slot(EquipmentSlot equipmentSlot, String id, int dropIndex, boolean hand) {
            this.equipmentSlot = equipmentSlot;
            this.id = id;
            this.dropIndex = dropIndex;
            this.hand = hand;
        }

        public static Slot fromId(String id) {
            for (Slot slot : values()) if (slot.id.equals(id)) return slot;
            return null;
        }
    }

    private ResourceLocation entityTypeId = ResourceLocation.withDefaultNamespace("pig");
    private Component customName;
    private boolean customNameVisible;
    private boolean persistent;
    private boolean silent;
    private boolean glowing;
    private boolean noAi;
    private boolean noGravity;

    private final EnumMap<Slot, ItemStack> equipment = new EnumMap<>(Slot.class);
    private final EnumMap<Slot, Float> dropChances = new EnumMap<>(Slot.class);

    public CosmicSpawnerPreset() {
        for (Slot slot : Slot.values()) {
            equipment.put(slot, ItemStack.EMPTY);
            dropChances.put(slot, 0.085F);
        }
    }
    public ResourceLocation getEntityTypeId() { return entityTypeId; }
    public void setEntityTypeId(ResourceLocation entityTypeId) { this.entityTypeId = entityTypeId; }
    public Component getCustomName() { return customName; }
    public void setCustomName(Component customName) { this.customName = customName; }
    public boolean isCustomNameVisible() { return customNameVisible; }
    public void setCustomNameVisible(boolean customNameVisible) { this.customNameVisible = customNameVisible; }
    public boolean isPersistent() { return persistent; }
    public void setPersistent(boolean persistent) { this.persistent = persistent; }
    public boolean isSilent() { return silent; }
    public void setSilent(boolean silent) { this.silent = silent; }
    public boolean isGlowing() { return glowing; }
    public void setGlowing(boolean glowing) { this.glowing = glowing; }
    public boolean isNoAi() { return noAi; }
    public void setNoAi(boolean noAi) { this.noAi = noAi; }
    public boolean isNoGravity() { return noGravity; }
    public void setNoGravity(boolean noGravity) { this.noGravity = noGravity; }
    public ItemStack getEquipment(Slot slot) { return equipment.get(slot); }
    public void setEquipment(Slot slot, ItemStack stack) { equipment.put(slot, stack.copy()); }
    public float getDropChance(Slot slot) { return dropChances.get(slot); }
    public void setDropChance(Slot slot, float chance) { dropChances.put(slot, Math.max(0F, Math.min(1F, chance))); }

    public CompoundTag save(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("presetVersion", PRESET_VERSION);
        tag.putString("entityType", entityTypeId.toString());
        if (customName != null) tag.putString("customName", Component.Serializer.toJson(customName, registries));
        tag.putBoolean("customNameVisible", customNameVisible);
        tag.putBoolean("persistent", persistent);
        tag.putBoolean("silent", silent);
        tag.putBoolean("glowing", glowing);
        tag.putBoolean("noAi", noAi);
        tag.putBoolean("noGravity", noGravity);
        CompoundTag equipTag = new CompoundTag();
        CompoundTag dropTag = new CompoundTag();
        for (Map.Entry<Slot, ItemStack> e : equipment.entrySet()) if (!e.getValue().isEmpty()) equipTag.put(e.getKey().id, e.getValue().save(registries));
        for (Map.Entry<Slot, Float> e : dropChances.entrySet()) dropTag.putFloat(e.getKey().id, e.getValue());
        tag.put("equipment", equipTag);
        tag.put("dropChances", dropTag);
        return tag;
    }

    public static Optional<CosmicSpawnerPreset> load(CompoundTag tag, HolderLookup.Provider registries) {
        if (tag.isEmpty()) return Optional.empty();
        CosmicSpawnerPreset preset = new CosmicSpawnerPreset();
        ResourceLocation typeId = ResourceLocation.tryParse(tag.getStringOr("entityType", "minecraft:pig"));
        if (typeId == null || !BuiltInRegistries.ENTITY_TYPE.containsKey(typeId)) typeId = ResourceLocation.withDefaultNamespace("pig");
        preset.entityTypeId = typeId;
        String customNameJson = tag.getStringOr("customName", "");
        if (!customNameJson.isBlank()) preset.customName = Component.Serializer.fromJson(customNameJson, registries);
        preset.customNameVisible = tag.getBooleanOr("customNameVisible", false);
        preset.persistent = tag.getBooleanOr("persistent", false);
        preset.silent = tag.getBooleanOr("silent", false);
        preset.glowing = tag.getBooleanOr("glowing", false);
        preset.noAi = tag.getBooleanOr("noAi", false);
        preset.noGravity = tag.getBooleanOr("noGravity", false);
        var equipTag = tag.getCompoundOrEmpty("equipment");
        var dropTag = tag.getCompoundOrEmpty("dropChances");
        for (Slot slot : Slot.values()) {
            equipTag.getCompound(slot.id).ifPresent(c -> preset.equipment.put(slot, ItemStack.parse(registries, c).orElse(ItemStack.EMPTY)));
            preset.dropChances.put(slot, Math.max(0F, Math.min(1F, dropTag.getFloatOr(slot.id, preset.dropChances.get(slot)))));
        }
        return Optional.of(preset);
    }

    public void applyToEntity(Entity entity) {
        if (customName != null) entity.setCustomName(customName);
        entity.setCustomNameVisible(customNameVisible);
        entity.setSilent(silent);
        entity.setGlowingTag(glowing);
        entity.setNoGravity(noGravity);
        if (entity instanceof LivingEntity living) {
            if (persistent) living.setPersistenceRequired();
            living.setNoAi(noAi);
            for (Slot slot : Slot.values()) {
                living.setItemSlot(slot.equipmentSlot, equipment.get(slot).copy());
                living.setDropChance(slot.equipmentSlot, getDropChance(slot));
            }
        }
    }
}
