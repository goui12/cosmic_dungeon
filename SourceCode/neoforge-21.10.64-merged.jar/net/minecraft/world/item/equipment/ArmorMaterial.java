package net.minecraft.world.item.equipment;

import java.util.Map;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.ItemAttributeModifiers;

public record ArmorMaterial(
    int durability,
    Map<ArmorType, Integer> defense,
    int enchantmentValue,
    Holder<SoundEvent> equipSound,
    float toughness,
    float knockbackResistance,
    TagKey<Item> repairIngredient,
    ResourceKey<EquipmentAsset> assetId
) {
    public ItemAttributeModifiers createAttributes(ArmorType armorType) {
        int i = this.defense.getOrDefault(armorType, 0);
        ItemAttributeModifiers.Builder itemattributemodifiers$builder = ItemAttributeModifiers.builder();
        EquipmentSlotGroup equipmentslotgroup = EquipmentSlotGroup.bySlot(armorType.getSlot());
        ResourceLocation resourcelocation = ResourceLocation.withDefaultNamespace("armor." + armorType.getName());
        itemattributemodifiers$builder.add(
            Attributes.ARMOR, new AttributeModifier(resourcelocation, i, AttributeModifier.Operation.ADD_VALUE), equipmentslotgroup
        );
        itemattributemodifiers$builder.add(
            Attributes.ARMOR_TOUGHNESS, new AttributeModifier(resourcelocation, this.toughness, AttributeModifier.Operation.ADD_VALUE), equipmentslotgroup
        );
        if (this.knockbackResistance > 0.0F) {
            itemattributemodifiers$builder.add(
                Attributes.KNOCKBACK_RESISTANCE,
                new AttributeModifier(resourcelocation, this.knockbackResistance, AttributeModifier.Operation.ADD_VALUE),
                equipmentslotgroup
            );
        }

        return itemattributemodifiers$builder.build();
    }
}
