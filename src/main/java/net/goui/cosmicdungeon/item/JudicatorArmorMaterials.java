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

    // ===== Asset keys (namespaced) =====
    public static final ResourceKey<EquipmentAsset> JUDICATOR_CHAINMAIL_ASSET =
            ResourceKey.create(
                    EquipmentAssets.ROOT_ID,
                    ResourceLocation.fromNamespaceAndPath(CosmicDungeonMod.MOD_ID, "judicator_chainmail")
            );

    public static final ResourceKey<EquipmentAsset> JUDICATOR_RUSTIC_IRON_ASSET =
            ResourceKey.create(
                    EquipmentAssets.ROOT_ID,
                    ResourceLocation.fromNamespaceAndPath(CosmicDungeonMod.MOD_ID, "judicator_rustic_iron")
            );

    public static final ResourceKey<EquipmentAsset> JUDICATOR_OCEAN_TEMPLAR_DIAMOND_ASSET =
            ResourceKey.create(
                    EquipmentAssets.ROOT_ID,
                    ResourceLocation.fromNamespaceAndPath(CosmicDungeonMod.MOD_ID, "judicator_ocean_templar_diamond")
            );

    public static final ResourceKey<EquipmentAsset> JUDICATOR_OCEAN_TEMPLAR_NETHERITE_ASSET =
            ResourceKey.create(
                    EquipmentAssets.ROOT_ID,
                    ResourceLocation.fromNamespaceAndPath(CosmicDungeonMod.MOD_ID, "judicator_ocean_templar_netherite")
            );

    // ===== Materials =====
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
            JUDICATOR_CHAINMAIL_ASSET
    );

    // Rustic Iron — mirror vanilla IRON but with your own asset
    public static final ArmorMaterial JUDICATOR_RUSTIC_IRON = new ArmorMaterial(
            15, // durability multiplier (iron)
            Map.of(
                    ArmorType.BOOTS, 2,
                    ArmorType.LEGGINGS, 5,
                    ArmorType.CHESTPLATE, 6,
                    ArmorType.HELMET, 2,
                    ArmorType.BODY, 0
            ),
            9, // enchantability (iron)
            SoundEvents.ARMOR_EQUIP_IRON,
            0.0F, // toughness (iron)
            0.0F, // knockback resistance (iron)
            ItemTags.REPAIRS_IRON_ARMOR,
            JUDICATOR_RUSTIC_IRON_ASSET
    );

    // Ocean Templar Diamond — mirror vanilla DIAMOND but with your own asset
    public static final ArmorMaterial JUDICATOR_OCEAN_TEMPLAR_DIAMOND = new ArmorMaterial(
            33, // durability multiplier (diamond)
            Map.of(
                    ArmorType.BOOTS, 3,
                    ArmorType.LEGGINGS, 6,
                    ArmorType.CHESTPLATE, 8,
                    ArmorType.HELMET, 3,
                    ArmorType.BODY, 0
            ),
            10, // enchantability (diamond)
            SoundEvents.ARMOR_EQUIP_DIAMOND,
            2.0F, // toughness (diamond)
            0.0F, // knockback resistance (diamond)
            ItemTags.REPAIRS_DIAMOND_ARMOR,
            JUDICATOR_OCEAN_TEMPLAR_DIAMOND_ASSET
    );

    // Ocean Templar Netherite — mirror vanilla NETHERITE but with your own asset
    public static final ArmorMaterial JUDICATOR_OCEAN_TEMPLAR_NETHERITE = new ArmorMaterial(
            37, // durability multiplier (netherite)
            Map.of(
                    ArmorType.BOOTS, 3,
                    ArmorType.LEGGINGS, 6,
                    ArmorType.CHESTPLATE, 8,
                    ArmorType.HELMET, 3,
                    ArmorType.BODY, 0
            ),
            15, // enchantability (netherite)
            SoundEvents.ARMOR_EQUIP_NETHERITE,
            3.0F,  // toughness (netherite)
            0.1F,  // knockback resistance (netherite)
            ItemTags.REPAIRS_NETHERITE_ARMOR,
            JUDICATOR_OCEAN_TEMPLAR_NETHERITE_ASSET
    );

    private JudicatorArmorMaterials() {}
}
