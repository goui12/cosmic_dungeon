// JudicatorArmorMaterials.java
package net.goui.cosmicdungeon.item;

import net.goui.cosmicdungeon.CosmicDungeonMod;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;

import java.util.Map;

public final class JudicatorArmorMaterials {

    // Correct: make a namespaced ResourceKey under the equipment_asset registry
    public static final ResourceKey<EquipmentAsset> JUDICATOR_CHAINMAIL_ASSET =
            ResourceKey.create(
                    EquipmentAssets.ROOT_ID,
                    ResourceLocation.fromNamespaceAndPath(CosmicDungeonMod.MOD_ID, "judicator_chainmail")
            );

    // Chainmail-equivalent stats, but using your custom equipment asset id
    public static final ArmorMaterial JUDICATOR_CHAINMAIL = new ArmorMaterial(
            15, // durability multiplier
            Map.of(
                    ArmorType.BOOTS, 1,
                    ArmorType.LEGGINGS, 4,
                    ArmorType.CHESTPLATE, 5,
                    ArmorType.HELMET, 2,
                    ArmorType.BODY, 0
            ),
            12, // enchantability
            SoundEvents.ARMOR_EQUIP_CHAIN,
            0.0F, // toughness
            0.0F, // knockback resistance
            ItemTags.REPAIRS_CHAIN_ARMOR,
            JUDICATOR_CHAINMAIL_ASSET // <-- your namespaced asset key
    );

    private JudicatorArmorMaterials() {}
}
