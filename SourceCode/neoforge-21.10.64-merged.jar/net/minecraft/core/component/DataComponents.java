package net.minecraft.core.component;

import com.mojang.serialization.Codec;
import java.util.List;
import java.util.function.UnaryOperator;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.util.EncoderCache;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.Unit;
import net.minecraft.world.LockCode;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.CatVariant;
import net.minecraft.world.entity.animal.ChickenVariant;
import net.minecraft.world.entity.animal.CowVariant;
import net.minecraft.world.entity.animal.Fox;
import net.minecraft.world.entity.animal.MushroomCow;
import net.minecraft.world.entity.animal.Parrot;
import net.minecraft.world.entity.animal.PigVariant;
import net.minecraft.world.entity.animal.Rabbit;
import net.minecraft.world.entity.animal.Salmon;
import net.minecraft.world.entity.animal.TropicalFish;
import net.minecraft.world.entity.animal.axolotl.Axolotl;
import net.minecraft.world.entity.animal.frog.FrogVariant;
import net.minecraft.world.entity.animal.horse.Llama;
import net.minecraft.world.entity.animal.horse.Variant;
import net.minecraft.world.entity.animal.wolf.WolfSoundVariant;
import net.minecraft.world.entity.animal.wolf.WolfVariant;
import net.minecraft.world.entity.decoration.PaintingVariant;
import net.minecraft.world.entity.npc.VillagerType;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.AdventureModePredicate;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.EitherHolder;
import net.minecraft.world.item.JukeboxPlayable;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.component.Bees;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.item.component.BlocksAttacks;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.component.ChargedProjectiles;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.component.DamageResistant;
import net.minecraft.world.item.component.DeathProtection;
import net.minecraft.world.item.component.DebugStickState;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.component.FireworkExplosion;
import net.minecraft.world.item.component.Fireworks;
import net.minecraft.world.item.component.InstrumentComponent;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.component.LodestoneTracker;
import net.minecraft.world.item.component.MapDecorations;
import net.minecraft.world.item.component.MapItemColor;
import net.minecraft.world.item.component.MapPostProcessing;
import net.minecraft.world.item.component.OminousBottleAmplifier;
import net.minecraft.world.item.component.ProvidesTrimMaterial;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.item.component.SeededContainerLoot;
import net.minecraft.world.item.component.SuspiciousStewEffects;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.item.component.UseCooldown;
import net.minecraft.world.item.component.UseRemainder;
import net.minecraft.world.item.component.Weapon;
import net.minecraft.world.item.component.WritableBookContent;
import net.minecraft.world.item.component.WrittenBookContent;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.enchantment.Enchantable;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.enchantment.Repairable;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.item.equipment.trim.ArmorTrim;
import net.minecraft.world.level.block.entity.BannerPattern;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.PotDecorations;
import net.minecraft.world.level.saveddata.maps.MapId;

public class DataComponents {
    static final EncoderCache ENCODER_CACHE = new EncoderCache(512);
    public static final DataComponentType<CustomData> CUSTOM_DATA = register("custom_data", p_331418_ -> p_331418_.persistent(CustomData.CODEC));
    public static final DataComponentType<Integer> MAX_STACK_SIZE = register(
        "max_stack_size", p_335179_ -> p_335179_.persistent(ExtraCodecs.intRange(1, 99)).networkSynchronized(ByteBufCodecs.VAR_INT)
    );
    public static final DataComponentType<Integer> MAX_DAMAGE = register(
        "max_damage", p_335177_ -> p_335177_.persistent(ExtraCodecs.POSITIVE_INT).networkSynchronized(ByteBufCodecs.VAR_INT)
    );
    public static final DataComponentType<Integer> DAMAGE = register(
        "damage", p_331382_ -> p_331382_.persistent(ExtraCodecs.NON_NEGATIVE_INT).networkSynchronized(ByteBufCodecs.VAR_INT)
    );
    public static final DataComponentType<Unit> UNBREAKABLE = register(
        "unbreakable", p_392587_ -> p_392587_.persistent(Unit.CODEC).networkSynchronized(Unit.STREAM_CODEC)
    );
    public static final DataComponentType<Component> CUSTOM_NAME = register(
        "custom_name", p_392585_ -> p_392585_.persistent(ComponentSerialization.CODEC).networkSynchronized(ComponentSerialization.STREAM_CODEC).cacheEncoding()
    );
    public static final DataComponentType<Component> ITEM_NAME = register(
        "item_name", p_392590_ -> p_392590_.persistent(ComponentSerialization.CODEC).networkSynchronized(ComponentSerialization.STREAM_CODEC).cacheEncoding()
    );
    public static final DataComponentType<ResourceLocation> ITEM_MODEL = register(
        "item_model", p_370373_ -> p_370373_.persistent(ResourceLocation.CODEC).networkSynchronized(ResourceLocation.STREAM_CODEC).cacheEncoding()
    );
    public static final DataComponentType<ItemLore> LORE = register(
        "lore", p_341842_ -> p_341842_.persistent(ItemLore.CODEC).networkSynchronized(ItemLore.STREAM_CODEC).cacheEncoding()
    );
    public static final DataComponentType<Rarity> RARITY = register(
        "rarity", p_335176_ -> p_335176_.persistent(Rarity.CODEC).networkSynchronized(Rarity.STREAM_CODEC)
    );
    public static final DataComponentType<ItemEnchantments> ENCHANTMENTS = register(
        "enchantments", p_341841_ -> p_341841_.persistent(ItemEnchantments.CODEC).networkSynchronized(ItemEnchantments.STREAM_CODEC).cacheEncoding()
    );
    public static final DataComponentType<AdventureModePredicate> CAN_PLACE_ON = register(
        "can_place_on",
        p_341861_ -> p_341861_.persistent(AdventureModePredicate.CODEC).networkSynchronized(AdventureModePredicate.STREAM_CODEC).cacheEncoding()
    );
    public static final DataComponentType<AdventureModePredicate> CAN_BREAK = register(
        "can_break", p_341837_ -> p_341837_.persistent(AdventureModePredicate.CODEC).networkSynchronized(AdventureModePredicate.STREAM_CODEC).cacheEncoding()
    );
    public static final DataComponentType<ItemAttributeModifiers> ATTRIBUTE_MODIFIERS = register(
        "attribute_modifiers",
        p_341845_ -> p_341845_.persistent(ItemAttributeModifiers.CODEC).networkSynchronized(ItemAttributeModifiers.STREAM_CODEC).cacheEncoding()
    );
    public static final DataComponentType<CustomModelData> CUSTOM_MODEL_DATA = register(
        "custom_model_data", p_330559_ -> p_330559_.persistent(CustomModelData.CODEC).networkSynchronized(CustomModelData.STREAM_CODEC)
    );
    public static final DataComponentType<TooltipDisplay> TOOLTIP_DISPLAY = register(
        "tooltip_display", p_399372_ -> p_399372_.persistent(TooltipDisplay.CODEC).networkSynchronized(TooltipDisplay.STREAM_CODEC).cacheEncoding()
    );
    public static final DataComponentType<Integer> REPAIR_COST = register(
        "repair_cost", p_331555_ -> p_331555_.persistent(ExtraCodecs.NON_NEGATIVE_INT).networkSynchronized(ByteBufCodecs.VAR_INT)
    );
    public static final DataComponentType<Unit> CREATIVE_SLOT_LOCK = register(
        "creative_slot_lock", p_392584_ -> p_392584_.networkSynchronized(Unit.STREAM_CODEC)
    );
    public static final DataComponentType<Boolean> ENCHANTMENT_GLINT_OVERRIDE = register(
        "enchantment_glint_override", p_330231_ -> p_330231_.persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL)
    );
    public static final DataComponentType<Unit> INTANGIBLE_PROJECTILE = register("intangible_projectile", p_344189_ -> p_344189_.persistent(Unit.CODEC));
    public static final DataComponentType<FoodProperties> FOOD = register(
        "food", p_341858_ -> p_341858_.persistent(FoodProperties.DIRECT_CODEC).networkSynchronized(FoodProperties.DIRECT_STREAM_CODEC).cacheEncoding()
    );
    public static final DataComponentType<Consumable> CONSUMABLE = register(
        "consumable", p_366370_ -> p_366370_.persistent(Consumable.CODEC).networkSynchronized(Consumable.STREAM_CODEC).cacheEncoding()
    );
    public static final DataComponentType<UseRemainder> USE_REMAINDER = register(
        "use_remainder", p_366368_ -> p_366368_.persistent(UseRemainder.CODEC).networkSynchronized(UseRemainder.STREAM_CODEC).cacheEncoding()
    );
    public static final DataComponentType<UseCooldown> USE_COOLDOWN = register(
        "use_cooldown", p_366367_ -> p_366367_.persistent(UseCooldown.CODEC).networkSynchronized(UseCooldown.STREAM_CODEC).cacheEncoding()
    );
    public static final DataComponentType<DamageResistant> DAMAGE_RESISTANT = register(
        "damage_resistant", p_372568_ -> p_372568_.persistent(DamageResistant.CODEC).networkSynchronized(DamageResistant.STREAM_CODEC).cacheEncoding()
    );
    public static final DataComponentType<Tool> TOOL = register(
        "tool", p_341839_ -> p_341839_.persistent(Tool.CODEC).networkSynchronized(Tool.STREAM_CODEC).cacheEncoding()
    );
    public static final DataComponentType<Weapon> WEAPON = register(
        "weapon", p_392589_ -> p_392589_.persistent(Weapon.CODEC).networkSynchronized(Weapon.STREAM_CODEC).cacheEncoding()
    );
    public static final DataComponentType<Enchantable> ENCHANTABLE = register(
        "enchantable", p_359365_ -> p_359365_.persistent(Enchantable.CODEC).networkSynchronized(Enchantable.STREAM_CODEC).cacheEncoding()
    );
    public static final DataComponentType<Equippable> EQUIPPABLE = register(
        "equippable", p_370371_ -> p_370371_.persistent(Equippable.CODEC).networkSynchronized(Equippable.STREAM_CODEC).cacheEncoding()
    );
    public static final DataComponentType<Repairable> REPAIRABLE = register(
        "repairable", p_359366_ -> p_359366_.persistent(Repairable.CODEC).networkSynchronized(Repairable.STREAM_CODEC).cacheEncoding()
    );
    public static final DataComponentType<Unit> GLIDER = register(
        "glider", p_392586_ -> p_392586_.persistent(Unit.CODEC).networkSynchronized(Unit.STREAM_CODEC)
    );
    public static final DataComponentType<ResourceLocation> TOOLTIP_STYLE = register(
        "tooltip_style", p_370372_ -> p_370372_.persistent(ResourceLocation.CODEC).networkSynchronized(ResourceLocation.STREAM_CODEC).cacheEncoding()
    );
    public static final DataComponentType<DeathProtection> DEATH_PROTECTION = register(
        "death_protection", p_372569_ -> p_372569_.persistent(DeathProtection.CODEC).networkSynchronized(DeathProtection.STREAM_CODEC).cacheEncoding()
    );
    public static final DataComponentType<BlocksAttacks> BLOCKS_ATTACKS = register(
        "blocks_attacks", p_399369_ -> p_399369_.persistent(BlocksAttacks.CODEC).networkSynchronized(BlocksAttacks.STREAM_CODEC).cacheEncoding()
    );
    public static final DataComponentType<ItemEnchantments> STORED_ENCHANTMENTS = register(
        "stored_enchantments", p_341840_ -> p_341840_.persistent(ItemEnchantments.CODEC).networkSynchronized(ItemEnchantments.STREAM_CODEC).cacheEncoding()
    );
    public static final DataComponentType<DyedItemColor> DYED_COLOR = register(
        "dyed_color", p_331088_ -> p_331088_.persistent(DyedItemColor.CODEC).networkSynchronized(DyedItemColor.STREAM_CODEC)
    );
    public static final DataComponentType<MapItemColor> MAP_COLOR = register(
        "map_color", p_330449_ -> p_330449_.persistent(MapItemColor.CODEC).networkSynchronized(MapItemColor.STREAM_CODEC)
    );
    public static final DataComponentType<MapId> MAP_ID = register(
        "map_id", p_330363_ -> p_330363_.persistent(MapId.CODEC).networkSynchronized(MapId.STREAM_CODEC)
    );
    public static final DataComponentType<MapDecorations> MAP_DECORATIONS = register(
        "map_decorations", p_341862_ -> p_341862_.persistent(MapDecorations.CODEC).cacheEncoding()
    );
    public static final DataComponentType<MapPostProcessing> MAP_POST_PROCESSING = register(
        "map_post_processing", p_331962_ -> p_331962_.networkSynchronized(MapPostProcessing.STREAM_CODEC)
    );
    public static final DataComponentType<ChargedProjectiles> CHARGED_PROJECTILES = register(
        "charged_projectiles", p_341859_ -> p_341859_.persistent(ChargedProjectiles.CODEC).networkSynchronized(ChargedProjectiles.STREAM_CODEC).cacheEncoding()
    );
    public static final DataComponentType<BundleContents> BUNDLE_CONTENTS = register(
        "bundle_contents", p_341857_ -> p_341857_.persistent(BundleContents.CODEC).networkSynchronized(BundleContents.STREAM_CODEC).cacheEncoding()
    );
    public static final DataComponentType<PotionContents> POTION_CONTENTS = register(
        "potion_contents", p_341836_ -> p_341836_.persistent(PotionContents.CODEC).networkSynchronized(PotionContents.STREAM_CODEC).cacheEncoding()
    );
    public static final DataComponentType<Float> POTION_DURATION_SCALE = register(
        "potion_duration_scale", p_392588_ -> p_392588_.persistent(ExtraCodecs.NON_NEGATIVE_FLOAT).networkSynchronized(ByteBufCodecs.FLOAT).cacheEncoding()
    );
    public static final DataComponentType<SuspiciousStewEffects> SUSPICIOUS_STEW_EFFECTS = register(
        "suspicious_stew_effects",
        p_341847_ -> p_341847_.persistent(SuspiciousStewEffects.CODEC).networkSynchronized(SuspiciousStewEffects.STREAM_CODEC).cacheEncoding()
    );
    public static final DataComponentType<WritableBookContent> WRITABLE_BOOK_CONTENT = register(
        "writable_book_content",
        p_341848_ -> p_341848_.persistent(WritableBookContent.CODEC).networkSynchronized(WritableBookContent.STREAM_CODEC).cacheEncoding()
    );
    public static final DataComponentType<WrittenBookContent> WRITTEN_BOOK_CONTENT = register(
        "written_book_content",
        p_341852_ -> p_341852_.persistent(WrittenBookContent.CODEC).networkSynchronized(WrittenBookContent.STREAM_CODEC).cacheEncoding()
    );
    public static final DataComponentType<ArmorTrim> TRIM = register(
        "trim", p_370370_ -> p_370370_.persistent(ArmorTrim.CODEC).networkSynchronized(ArmorTrim.STREAM_CODEC).cacheEncoding()
    );
    public static final DataComponentType<DebugStickState> DEBUG_STICK_STATE = register(
        "debug_stick_state", p_341865_ -> p_341865_.persistent(DebugStickState.CODEC).cacheEncoding()
    );
    public static final DataComponentType<TypedEntityData<EntityType<?>>> ENTITY_DATA = register(
        "entity_data",
        p_432338_ -> p_432338_.persistent(TypedEntityData.codec(EntityType.CODEC)).networkSynchronized(TypedEntityData.streamCodec(EntityType.STREAM_CODEC))
    );
    public static final DataComponentType<CustomData> BUCKET_ENTITY_DATA = register(
        "bucket_entity_data", p_331109_ -> p_331109_.persistent(CustomData.CODEC).networkSynchronized(CustomData.STREAM_CODEC)
    );
    public static final DataComponentType<TypedEntityData<BlockEntityType<?>>> BLOCK_ENTITY_DATA = register(
        "block_entity_data",
        p_432339_ -> p_432339_.persistent(TypedEntityData.codec(BuiltInRegistries.BLOCK_ENTITY_TYPE.byNameCodec()))
            .networkSynchronized(TypedEntityData.streamCodec(ByteBufCodecs.registry(Registries.BLOCK_ENTITY_TYPE)))
    );
    public static final DataComponentType<InstrumentComponent> INSTRUMENT = register(
        "instrument", p_399370_ -> p_399370_.persistent(InstrumentComponent.CODEC).networkSynchronized(InstrumentComponent.STREAM_CODEC).cacheEncoding()
    );
    public static final DataComponentType<ProvidesTrimMaterial> PROVIDES_TRIM_MATERIAL = register(
        "provides_trim_material",
        p_399371_ -> p_399371_.persistent(ProvidesTrimMaterial.CODEC).networkSynchronized(ProvidesTrimMaterial.STREAM_CODEC).cacheEncoding()
    );
    public static final DataComponentType<OminousBottleAmplifier> OMINOUS_BOTTLE_AMPLIFIER = register(
        "ominous_bottle_amplifier", p_366369_ -> p_366369_.persistent(OminousBottleAmplifier.CODEC).networkSynchronized(OminousBottleAmplifier.STREAM_CODEC)
    );
    public static final DataComponentType<JukeboxPlayable> JUKEBOX_PLAYABLE = register(
        "jukebox_playable", p_349913_ -> p_349913_.persistent(JukeboxPlayable.CODEC).networkSynchronized(JukeboxPlayable.STREAM_CODEC)
    );
    public static final DataComponentType<TagKey<BannerPattern>> PROVIDES_BANNER_PATTERNS = register(
        "provides_banner_patterns",
        p_400875_ -> p_400875_.persistent(TagKey.hashedCodec(Registries.BANNER_PATTERN))
            .networkSynchronized(TagKey.streamCodec(Registries.BANNER_PATTERN))
            .cacheEncoding()
    );
    public static final DataComponentType<List<ResourceKey<Recipe<?>>>> RECIPES = register(
        "recipes", p_404167_ -> p_404167_.persistent(Recipe.KEY_CODEC.listOf()).cacheEncoding()
    );
    public static final DataComponentType<LodestoneTracker> LODESTONE_TRACKER = register(
        "lodestone_tracker", p_341854_ -> p_341854_.persistent(LodestoneTracker.CODEC).networkSynchronized(LodestoneTracker.STREAM_CODEC).cacheEncoding()
    );
    public static final DataComponentType<FireworkExplosion> FIREWORK_EXPLOSION = register(
        "firework_explosion", p_341843_ -> p_341843_.persistent(FireworkExplosion.CODEC).networkSynchronized(FireworkExplosion.STREAM_CODEC).cacheEncoding()
    );
    public static final DataComponentType<Fireworks> FIREWORKS = register(
        "fireworks", p_341860_ -> p_341860_.persistent(Fireworks.CODEC).networkSynchronized(Fireworks.STREAM_CODEC).cacheEncoding()
    );
    public static final DataComponentType<ResolvableProfile> PROFILE = register(
        "profile", p_341851_ -> p_341851_.persistent(ResolvableProfile.CODEC).networkSynchronized(ResolvableProfile.STREAM_CODEC).cacheEncoding()
    );
    public static final DataComponentType<ResourceLocation> NOTE_BLOCK_SOUND = register(
        "note_block_sound", p_330798_ -> p_330798_.persistent(ResourceLocation.CODEC).networkSynchronized(ResourceLocation.STREAM_CODEC)
    );
    public static final DataComponentType<BannerPatternLayers> BANNER_PATTERNS = register(
        "banner_patterns", p_341863_ -> p_341863_.persistent(BannerPatternLayers.CODEC).networkSynchronized(BannerPatternLayers.STREAM_CODEC).cacheEncoding()
    );
    public static final DataComponentType<DyeColor> BASE_COLOR = register(
        "base_color", p_331467_ -> p_331467_.persistent(DyeColor.CODEC).networkSynchronized(DyeColor.STREAM_CODEC)
    );
    public static final DataComponentType<PotDecorations> POT_DECORATIONS = register(
        "pot_decorations", p_341864_ -> p_341864_.persistent(PotDecorations.CODEC).networkSynchronized(PotDecorations.STREAM_CODEC).cacheEncoding()
    );
    public static final DataComponentType<ItemContainerContents> CONTAINER = register(
        "container", p_341846_ -> p_341846_.persistent(ItemContainerContents.CODEC).networkSynchronized(ItemContainerContents.STREAM_CODEC).cacheEncoding()
    );
    public static final DataComponentType<BlockItemStateProperties> BLOCK_STATE = register(
        "block_state",
        p_341856_ -> p_341856_.persistent(BlockItemStateProperties.CODEC).networkSynchronized(BlockItemStateProperties.STREAM_CODEC).cacheEncoding()
    );
    public static final DataComponentType<Bees> BEES = register(
        "bees", p_399367_ -> p_399367_.persistent(Bees.CODEC).networkSynchronized(Bees.STREAM_CODEC).cacheEncoding()
    );
    public static final DataComponentType<LockCode> LOCK = register("lock", p_330909_ -> p_330909_.persistent(LockCode.CODEC));
    public static final DataComponentType<SeededContainerLoot> CONTAINER_LOOT = register(
        "container_loot", p_331929_ -> p_331929_.persistent(SeededContainerLoot.CODEC)
    );
    public static final DataComponentType<Holder<SoundEvent>> BREAK_SOUND = register(
        "break_sound", p_399366_ -> p_399366_.persistent(SoundEvent.CODEC).networkSynchronized(SoundEvent.STREAM_CODEC).cacheEncoding()
    );
    public static final DataComponentType<Holder<VillagerType>> VILLAGER_VARIANT = register(
        "villager/variant", p_396337_ -> p_396337_.persistent(VillagerType.CODEC).networkSynchronized(VillagerType.STREAM_CODEC)
    );
    public static final DataComponentType<Holder<WolfVariant>> WOLF_VARIANT = register(
        "wolf/variant", p_406192_ -> p_406192_.persistent(WolfVariant.CODEC).networkSynchronized(WolfVariant.STREAM_CODEC)
    );
    public static final DataComponentType<Holder<WolfSoundVariant>> WOLF_SOUND_VARIANT = register(
        "wolf/sound_variant", p_406193_ -> p_406193_.persistent(WolfSoundVariant.CODEC).networkSynchronized(WolfSoundVariant.STREAM_CODEC)
    );
    public static final DataComponentType<DyeColor> WOLF_COLLAR = register(
        "wolf/collar", p_396329_ -> p_396329_.persistent(DyeColor.CODEC).networkSynchronized(DyeColor.STREAM_CODEC)
    );
    public static final DataComponentType<Fox.Variant> FOX_VARIANT = register(
        "fox/variant", p_396338_ -> p_396338_.persistent(Fox.Variant.CODEC).networkSynchronized(Fox.Variant.STREAM_CODEC)
    );
    public static final DataComponentType<Salmon.Variant> SALMON_SIZE = register(
        "salmon/size", p_396344_ -> p_396344_.persistent(Salmon.Variant.CODEC).networkSynchronized(Salmon.Variant.STREAM_CODEC)
    );
    public static final DataComponentType<Parrot.Variant> PARROT_VARIANT = register(
        "parrot/variant", p_396334_ -> p_396334_.persistent(Parrot.Variant.CODEC).networkSynchronized(Parrot.Variant.STREAM_CODEC)
    );
    public static final DataComponentType<TropicalFish.Pattern> TROPICAL_FISH_PATTERN = register(
        "tropical_fish/pattern", p_396345_ -> p_396345_.persistent(TropicalFish.Pattern.CODEC).networkSynchronized(TropicalFish.Pattern.STREAM_CODEC)
    );
    public static final DataComponentType<DyeColor> TROPICAL_FISH_BASE_COLOR = register(
        "tropical_fish/base_color", p_396335_ -> p_396335_.persistent(DyeColor.CODEC).networkSynchronized(DyeColor.STREAM_CODEC)
    );
    public static final DataComponentType<DyeColor> TROPICAL_FISH_PATTERN_COLOR = register(
        "tropical_fish/pattern_color", p_396327_ -> p_396327_.persistent(DyeColor.CODEC).networkSynchronized(DyeColor.STREAM_CODEC)
    );
    public static final DataComponentType<MushroomCow.Variant> MOOSHROOM_VARIANT = register(
        "mooshroom/variant", p_404166_ -> p_404166_.persistent(MushroomCow.Variant.CODEC).networkSynchronized(MushroomCow.Variant.STREAM_CODEC)
    );
    public static final DataComponentType<Rabbit.Variant> RABBIT_VARIANT = register(
        "rabbit/variant", p_396342_ -> p_396342_.persistent(Rabbit.Variant.CODEC).networkSynchronized(Rabbit.Variant.STREAM_CODEC)
    );
    public static final DataComponentType<Holder<PigVariant>> PIG_VARIANT = register(
        "pig/variant", p_396332_ -> p_396332_.persistent(PigVariant.CODEC).networkSynchronized(PigVariant.STREAM_CODEC)
    );
    public static final DataComponentType<Holder<CowVariant>> COW_VARIANT = register(
        "cow/variant", p_400874_ -> p_400874_.persistent(CowVariant.CODEC).networkSynchronized(CowVariant.STREAM_CODEC)
    );
    public static final DataComponentType<EitherHolder<ChickenVariant>> CHICKEN_VARIANT = register(
        "chicken/variant",
        p_401712_ -> p_401712_.persistent(EitherHolder.codec(Registries.CHICKEN_VARIANT, ChickenVariant.CODEC))
            .networkSynchronized(EitherHolder.streamCodec(Registries.CHICKEN_VARIANT, ChickenVariant.STREAM_CODEC))
    );
    public static final DataComponentType<Holder<FrogVariant>> FROG_VARIANT = register(
        "frog/variant", p_399373_ -> p_399373_.persistent(FrogVariant.CODEC).networkSynchronized(FrogVariant.STREAM_CODEC)
    );
    public static final DataComponentType<Variant> HORSE_VARIANT = register(
        "horse/variant", p_396333_ -> p_396333_.persistent(Variant.CODEC).networkSynchronized(Variant.STREAM_CODEC)
    );
    public static final DataComponentType<Holder<PaintingVariant>> PAINTING_VARIANT = register(
        "painting/variant", p_396326_ -> p_396326_.persistent(PaintingVariant.CODEC).networkSynchronized(PaintingVariant.STREAM_CODEC)
    );
    public static final DataComponentType<Llama.Variant> LLAMA_VARIANT = register(
        "llama/variant", p_396341_ -> p_396341_.persistent(Llama.Variant.CODEC).networkSynchronized(Llama.Variant.STREAM_CODEC)
    );
    public static final DataComponentType<Axolotl.Variant> AXOLOTL_VARIANT = register(
        "axolotl/variant", p_396328_ -> p_396328_.persistent(Axolotl.Variant.CODEC).networkSynchronized(Axolotl.Variant.STREAM_CODEC)
    );
    public static final DataComponentType<Holder<CatVariant>> CAT_VARIANT = register(
        "cat/variant", p_396339_ -> p_396339_.persistent(CatVariant.CODEC).networkSynchronized(CatVariant.STREAM_CODEC)
    );
    public static final DataComponentType<DyeColor> CAT_COLLAR = register(
        "cat/collar", p_396340_ -> p_396340_.persistent(DyeColor.CODEC).networkSynchronized(DyeColor.STREAM_CODEC)
    );
    public static final DataComponentType<DyeColor> SHEEP_COLOR = register(
        "sheep/color", p_396343_ -> p_396343_.persistent(DyeColor.CODEC).networkSynchronized(DyeColor.STREAM_CODEC)
    );
    public static final DataComponentType<DyeColor> SHULKER_COLOR = register(
        "shulker/color", p_396331_ -> p_396331_.persistent(DyeColor.CODEC).networkSynchronized(DyeColor.STREAM_CODEC)
    );
    public static final DataComponentMap COMMON_ITEM_COMPONENTS = DataComponentMap.builder()
        .set(MAX_STACK_SIZE, 64)
        .set(LORE, ItemLore.EMPTY)
        .set(ENCHANTMENTS, ItemEnchantments.EMPTY)
        .set(REPAIR_COST, 0)
        .set(ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY)
        .set(RARITY, Rarity.COMMON)
        .set(BREAK_SOUND, SoundEvents.ITEM_BREAK)
        .set(TOOLTIP_DISPLAY, TooltipDisplay.DEFAULT)
        .build();

    public static DataComponentType<?> bootstrap(Registry<DataComponentType<?>> registry) {
        return CUSTOM_DATA;
    }

    private static <T> DataComponentType<T> register(String name, UnaryOperator<DataComponentType.Builder<T>> builder) {
        return Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, name, builder.apply(DataComponentType.builder()).build());
    }
}
