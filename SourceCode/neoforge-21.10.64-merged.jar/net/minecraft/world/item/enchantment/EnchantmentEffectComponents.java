package net.minecraft.world.item.enchantment;

import com.mojang.serialization.Codec;
import java.util.List;
import java.util.function.UnaryOperator;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Unit;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.enchantment.effects.DamageImmunity;
import net.minecraft.world.item.enchantment.effects.EnchantmentAttributeEffect;
import net.minecraft.world.item.enchantment.effects.EnchantmentEntityEffect;
import net.minecraft.world.item.enchantment.effects.EnchantmentLocationBasedEffect;
import net.minecraft.world.item.enchantment.effects.EnchantmentValueEffect;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;

public interface EnchantmentEffectComponents {
    Codec<DataComponentType<?>> COMPONENT_CODEC = Codec.lazyInitialized(() -> BuiltInRegistries.ENCHANTMENT_EFFECT_COMPONENT_TYPE.byNameCodec());
    Codec<DataComponentMap> CODEC = DataComponentMap.makeCodec(COMPONENT_CODEC);
    DataComponentType<List<ConditionalEffect<EnchantmentValueEffect>>> DAMAGE_PROTECTION = register(
        "damage_protection",
        p_380888_ -> p_380888_.persistent(ConditionalEffect.codec(EnchantmentValueEffect.CODEC, LootContextParamSets.ENCHANTED_DAMAGE).listOf())
    );
    DataComponentType<List<ConditionalEffect<DamageImmunity>>> DAMAGE_IMMUNITY = register(
        "damage_immunity", p_380885_ -> p_380885_.persistent(ConditionalEffect.codec(DamageImmunity.CODEC, LootContextParamSets.ENCHANTED_DAMAGE).listOf())
    );
    DataComponentType<List<ConditionalEffect<EnchantmentValueEffect>>> DAMAGE = register(
        "damage", p_380877_ -> p_380877_.persistent(ConditionalEffect.codec(EnchantmentValueEffect.CODEC, LootContextParamSets.ENCHANTED_DAMAGE).listOf())
    );
    DataComponentType<List<ConditionalEffect<EnchantmentValueEffect>>> SMASH_DAMAGE_PER_FALLEN_BLOCK = register(
        "smash_damage_per_fallen_block",
        p_380881_ -> p_380881_.persistent(ConditionalEffect.codec(EnchantmentValueEffect.CODEC, LootContextParamSets.ENCHANTED_DAMAGE).listOf())
    );
    DataComponentType<List<ConditionalEffect<EnchantmentValueEffect>>> KNOCKBACK = register(
        "knockback", p_380869_ -> p_380869_.persistent(ConditionalEffect.codec(EnchantmentValueEffect.CODEC, LootContextParamSets.ENCHANTED_DAMAGE).listOf())
    );
    DataComponentType<List<ConditionalEffect<EnchantmentValueEffect>>> ARMOR_EFFECTIVENESS = register(
        "armor_effectiveness",
        p_380887_ -> p_380887_.persistent(ConditionalEffect.codec(EnchantmentValueEffect.CODEC, LootContextParamSets.ENCHANTED_DAMAGE).listOf())
    );
    DataComponentType<List<TargetedConditionalEffect<EnchantmentEntityEffect>>> POST_ATTACK = register(
        "post_attack",
        p_380879_ -> p_380879_.persistent(TargetedConditionalEffect.codec(EnchantmentEntityEffect.CODEC, LootContextParamSets.ENCHANTED_DAMAGE).listOf())
    );
    DataComponentType<List<ConditionalEffect<EnchantmentEntityEffect>>> HIT_BLOCK = register(
        "hit_block", p_380872_ -> p_380872_.persistent(ConditionalEffect.codec(EnchantmentEntityEffect.CODEC, LootContextParamSets.HIT_BLOCK).listOf())
    );
    DataComponentType<List<ConditionalEffect<EnchantmentValueEffect>>> ITEM_DAMAGE = register(
        "item_damage", p_380886_ -> p_380886_.persistent(ConditionalEffect.codec(EnchantmentValueEffect.CODEC, LootContextParamSets.ENCHANTED_ITEM).listOf())
    );
    DataComponentType<List<EnchantmentAttributeEffect>> ATTRIBUTES = register(
        "attributes", p_345468_ -> p_345468_.persistent(EnchantmentAttributeEffect.CODEC.codec().listOf())
    );
    DataComponentType<List<TargetedConditionalEffect<EnchantmentValueEffect>>> EQUIPMENT_DROPS = register(
        "equipment_drops",
        p_380889_ -> p_380889_.persistent(
            TargetedConditionalEffect.equipmentDropsCodec(EnchantmentValueEffect.CODEC, LootContextParamSets.ENCHANTED_DAMAGE).listOf()
        )
    );
    DataComponentType<List<ConditionalEffect<EnchantmentLocationBasedEffect>>> LOCATION_CHANGED = register(
        "location_changed",
        p_380870_ -> p_380870_.persistent(ConditionalEffect.codec(EnchantmentLocationBasedEffect.CODEC, LootContextParamSets.ENCHANTED_LOCATION).listOf())
    );
    DataComponentType<List<ConditionalEffect<EnchantmentEntityEffect>>> TICK = register(
        "tick", p_380878_ -> p_380878_.persistent(ConditionalEffect.codec(EnchantmentEntityEffect.CODEC, LootContextParamSets.ENCHANTED_ENTITY).listOf())
    );
    DataComponentType<List<ConditionalEffect<EnchantmentValueEffect>>> AMMO_USE = register(
        "ammo_use", p_380873_ -> p_380873_.persistent(ConditionalEffect.codec(EnchantmentValueEffect.CODEC, LootContextParamSets.ENCHANTED_ITEM).listOf())
    );
    DataComponentType<List<ConditionalEffect<EnchantmentValueEffect>>> PROJECTILE_PIERCING = register(
        "projectile_piercing",
        p_380876_ -> p_380876_.persistent(ConditionalEffect.codec(EnchantmentValueEffect.CODEC, LootContextParamSets.ENCHANTED_ITEM).listOf())
    );
    DataComponentType<List<ConditionalEffect<EnchantmentEntityEffect>>> PROJECTILE_SPAWNED = register(
        "projectile_spawned",
        p_380868_ -> p_380868_.persistent(ConditionalEffect.codec(EnchantmentEntityEffect.CODEC, LootContextParamSets.ENCHANTED_ENTITY).listOf())
    );
    DataComponentType<List<ConditionalEffect<EnchantmentValueEffect>>> PROJECTILE_SPREAD = register(
        "projectile_spread",
        p_380884_ -> p_380884_.persistent(ConditionalEffect.codec(EnchantmentValueEffect.CODEC, LootContextParamSets.ENCHANTED_ENTITY).listOf())
    );
    DataComponentType<List<ConditionalEffect<EnchantmentValueEffect>>> PROJECTILE_COUNT = register(
        "projectile_count",
        p_380874_ -> p_380874_.persistent(ConditionalEffect.codec(EnchantmentValueEffect.CODEC, LootContextParamSets.ENCHANTED_ENTITY).listOf())
    );
    DataComponentType<List<ConditionalEffect<EnchantmentValueEffect>>> TRIDENT_RETURN_ACCELERATION = register(
        "trident_return_acceleration",
        p_380883_ -> p_380883_.persistent(ConditionalEffect.codec(EnchantmentValueEffect.CODEC, LootContextParamSets.ENCHANTED_ENTITY).listOf())
    );
    DataComponentType<List<ConditionalEffect<EnchantmentValueEffect>>> FISHING_TIME_REDUCTION = register(
        "fishing_time_reduction",
        p_380867_ -> p_380867_.persistent(ConditionalEffect.codec(EnchantmentValueEffect.CODEC, LootContextParamSets.ENCHANTED_ENTITY).listOf())
    );
    DataComponentType<List<ConditionalEffect<EnchantmentValueEffect>>> FISHING_LUCK_BONUS = register(
        "fishing_luck_bonus",
        p_380882_ -> p_380882_.persistent(ConditionalEffect.codec(EnchantmentValueEffect.CODEC, LootContextParamSets.ENCHANTED_ENTITY).listOf())
    );
    DataComponentType<List<ConditionalEffect<EnchantmentValueEffect>>> BLOCK_EXPERIENCE = register(
        "block_experience",
        p_380871_ -> p_380871_.persistent(ConditionalEffect.codec(EnchantmentValueEffect.CODEC, LootContextParamSets.ENCHANTED_ITEM).listOf())
    );
    DataComponentType<List<ConditionalEffect<EnchantmentValueEffect>>> MOB_EXPERIENCE = register(
        "mob_experience",
        p_380875_ -> p_380875_.persistent(ConditionalEffect.codec(EnchantmentValueEffect.CODEC, LootContextParamSets.ENCHANTED_ENTITY).listOf())
    );
    DataComponentType<List<ConditionalEffect<EnchantmentValueEffect>>> REPAIR_WITH_XP = register(
        "repair_with_xp",
        p_380880_ -> p_380880_.persistent(ConditionalEffect.codec(EnchantmentValueEffect.CODEC, LootContextParamSets.ENCHANTED_ITEM).listOf())
    );
    DataComponentType<EnchantmentValueEffect> CROSSBOW_CHARGE_TIME = register(
        "crossbow_charge_time", p_347314_ -> p_347314_.persistent(EnchantmentValueEffect.CODEC)
    );
    DataComponentType<List<CrossbowItem.ChargingSounds>> CROSSBOW_CHARGING_SOUNDS = register(
        "crossbow_charging_sounds", p_345990_ -> p_345990_.persistent(CrossbowItem.ChargingSounds.CODEC.listOf())
    );
    DataComponentType<List<Holder<SoundEvent>>> TRIDENT_SOUND = register("trident_sound", p_345208_ -> p_345208_.persistent(SoundEvent.CODEC.listOf()));
    DataComponentType<Unit> PREVENT_EQUIPMENT_DROP = register("prevent_equipment_drop", p_346368_ -> p_346368_.persistent(Unit.CODEC));
    DataComponentType<Unit> PREVENT_ARMOR_CHANGE = register("prevent_armor_change", p_345721_ -> p_345721_.persistent(Unit.CODEC));
    DataComponentType<EnchantmentValueEffect> TRIDENT_SPIN_ATTACK_STRENGTH = register(
        "trident_spin_attack_strength", p_347313_ -> p_347313_.persistent(EnchantmentValueEffect.CODEC)
    );

    static DataComponentType<?> bootstrap(Registry<DataComponentType<?>> registry) {
        return DAMAGE_PROTECTION;
    }

    private static <T> DataComponentType<T> register(String name, UnaryOperator<DataComponentType.Builder<T>> operator) {
        return Registry.register(BuiltInRegistries.ENCHANTMENT_EFFECT_COMPONENT_TYPE, name, operator.apply(DataComponentType.builder()).build());
    }
}
