package net.minecraft.world.item;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Maps;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.DependantName;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureElement;
import net.minecraft.world.flag.FeatureFlag;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.component.BlocksAttacks;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.component.Consumables;
import net.minecraft.world.item.component.DamageResistant;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.ProvidesTrimMaterial;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.item.component.UseCooldown;
import net.minecraft.world.item.component.UseRemainder;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.enchantment.Enchantable;
import net.minecraft.world.item.enchantment.Repairable;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.item.equipment.trim.TrimMaterial;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

public class Item implements FeatureElement, ItemLike, net.neoforged.neoforge.common.extensions.IItemExtension {
    public static final Codec<Holder<Item>> CODEC = BuiltInRegistries.ITEM
        .holderByNameCodec()
        .validate(
            p_381630_ -> p_381630_.is(Items.AIR.builtInRegistryHolder())
                ? DataResult.error(() -> "Item must not be minecraft:air")
                : DataResult.success(p_381630_)
        );
    public static final StreamCodec<RegistryFriendlyByteBuf, Holder<Item>> STREAM_CODEC = ByteBufCodecs.holderRegistry(Registries.ITEM);
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final Map<Block, Item> BY_BLOCK = net.neoforged.neoforge.registries.GameData.getBlockItemMap();
    public static final ResourceLocation BASE_ATTACK_DAMAGE_ID = ResourceLocation.withDefaultNamespace("base_attack_damage");
    public static final ResourceLocation BASE_ATTACK_SPEED_ID = ResourceLocation.withDefaultNamespace("base_attack_speed");
    public static final int DEFAULT_MAX_STACK_SIZE = 64;
    public static final int ABSOLUTE_MAX_STACK_SIZE = 99;
    public static final int MAX_BAR_WIDTH = 13;
    protected static final int APPROXIMATELY_INFINITE_USE_DURATION = 72000;
    private final Holder.Reference<Item> builtInRegistryHolder = BuiltInRegistries.ITEM.createIntrusiveHolder(this);
    private DataComponentMap components;
    @Nullable
    private final Item craftingRemainingItem;
    protected final String descriptionId;
    private final FeatureFlagSet requiredFeatures;
    @Nullable
    private net.neoforged.neoforge.transfer.item.ItemResource defaultResource;

    public static int getId(Item item) {
        return item == null ? 0 : BuiltInRegistries.ITEM.getId(item);
    }

    public static Item byId(int id) {
        return BuiltInRegistries.ITEM.byId(id);
    }

    @Deprecated
    public static Item byBlock(Block block) {
        return BY_BLOCK.getOrDefault(block, Items.AIR);
    }

    public Item(Item.Properties properties) {
        this.descriptionId = properties.effectiveDescriptionId();
        this.components = properties.buildAndValidateComponents(Component.translatable(this.descriptionId), properties.effectiveModel());
        this.craftingRemainingItem = properties.craftingRemainingItem;
        this.requiredFeatures = properties.requiredFeatures;
        if (SharedConstants.IS_RUNNING_IN_IDE && false) {
            String s = this.getClass().getSimpleName();
            if (!s.endsWith("Item")) {
                LOGGER.error("Item classes should end with Item and {} doesn't.", s);
            }
        }
        this.canCombineRepair = properties.canCombineRepair;
    }

    @Deprecated
    public Holder.Reference<Item> builtInRegistryHolder() {
        return this.builtInRegistryHolder;
    }

    public DataComponentMap components() {
        return this.components;
    }

    /** @deprecated Neo: do not use, use {@link net.neoforged.neoforge.event.ModifyDefaultComponentsEvent the event} instead */
    @org.jetbrains.annotations.ApiStatus.Internal @Deprecated
    public void modifyDefaultComponentsFrom(net.minecraft.core.component.DataComponentPatch patch) {
        if (!net.neoforged.neoforge.internal.RegistrationEvents.canModifyComponents()) throw new IllegalStateException("Default components cannot be modified now!");
        var builder = DataComponentMap.builder().addAll(components);
        patch.entrySet().forEach(entry -> builder.set((DataComponentType)entry.getKey(), entry.getValue().orElse(null)));
        components = Properties.validateComponents(builder.build());
        defaultResource = null;
    }

    /** @deprecated Neo: do not use, use {@link net.neoforged.neoforge.transfer.item.ItemResource#of} instead. */
    @org.jetbrains.annotations.ApiStatus.Internal @Deprecated
    public net.neoforged.neoforge.transfer.item.ItemResource computeDefaultResource(java.util.function.Function<Item, net.neoforged.neoforge.transfer.item.ItemResource> resourceConstructor) {
        if (this.defaultResource == null) {
            this.defaultResource = resourceConstructor.apply(this);
        }
        return this.defaultResource;
    }

    public int getDefaultMaxStackSize() {
        return this.components.getOrDefault(DataComponents.MAX_STACK_SIZE, 1);
    }

    /**
     * Called as the item is being used by an entity.
     */
    public void onUseTick(Level level, LivingEntity livingEntity, ItemStack stack, int remainingUseDuration) {
    }

    /**
 * @deprecated Forge: {@link
 *             net.neoforged.neoforge.common.extensions.IItemExtension#onDestroyed
 *             (ItemEntity, DamageSource) Use damage source sensitive version}
 */
    @Deprecated
    public void onDestroyed(ItemEntity itemEntity) {
    }

    public boolean canDestroyBlock(ItemStack stack, BlockState state, Level level, BlockPos pos, LivingEntity entity) {
        Tool tool = stack.get(DataComponents.TOOL);
        return tool != null && !tool.canDestroyBlocksInCreative() ? !(entity instanceof Player player && player.getAbilities().instabuild) : true;
    }

    @Override
    public Item asItem() {
        return this;
    }

    /**
     * Called when this item is used when targeting a Block
     */
    public InteractionResult useOn(UseOnContext context) {
        return InteractionResult.PASS;
    }

    public float getDestroySpeed(ItemStack stack, BlockState state) {
        Tool tool = stack.get(DataComponents.TOOL);
        return tool != null ? tool.getMiningSpeed(state) : 1.0F;
    }

    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack itemstack = player.getItemInHand(hand);
        Consumable consumable = itemstack.get(DataComponents.CONSUMABLE);
        if (consumable != null) {
            return consumable.startConsuming(player, itemstack, hand);
        } else {
            Equippable equippable = itemstack.get(DataComponents.EQUIPPABLE);
            if (equippable != null && equippable.swappable()) {
                return equippable.swapWithEquipmentSlot(itemstack, player);
            } else {
                BlocksAttacks blocksattacks = itemstack.get(DataComponents.BLOCKS_ATTACKS);
                if (blocksattacks != null) {
                    player.startUsingItem(hand);
                    return InteractionResult.CONSUME;
                } else {
                    return InteractionResult.PASS;
                }
            }
        }
    }

    /**
     * Called when the player finishes using this Item (E.g. finishes eating.). Not called when the player stops using the Item before the action is complete.
     */
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity livingEntity) {
        Consumable consumable = stack.get(DataComponents.CONSUMABLE);
        return consumable != null ? consumable.onConsume(level, livingEntity, stack) : stack;
    }

    public boolean isBarVisible(ItemStack stack) {
        return stack.isDamaged();
    }

    public int getBarWidth(ItemStack stack) {
        return Mth.clamp(Math.round(13.0F - stack.getDamageValue() * 13.0F / stack.getMaxDamage()), 0, 13);
    }

    public int getBarColor(ItemStack stack) {
        int i = stack.getMaxDamage();
        float f = Math.max(0.0F, ((float)i - stack.getDamageValue()) / i);
        return Mth.hsvToRgb(f / 3.0F, 1.0F, 1.0F);
    }

    public boolean overrideStackedOnOther(ItemStack stack, Slot slot, ClickAction action, Player player) {
        return false;
    }

    public boolean overrideOtherStackedOnMe(
        ItemStack stack, ItemStack other, Slot slot, ClickAction action, Player player, SlotAccess access
    ) {
        return false;
    }

    public float getAttackDamageBonus(Entity target, float damage, DamageSource damageSource) {
        return 0.0F;
    }

    @Nullable
    public DamageSource getDamageSource(LivingEntity entity) {
        return null;
    }

    public void hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
    }

    public void postHurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
    }

    /**
     * Called when a {@link net.minecraft.world.level.block.Block} is destroyed using this Item. Return {@code true} to trigger the "Use Item" statistic.
     */
    public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity miningEntity) {
        Tool tool = stack.get(DataComponents.TOOL);
        if (tool == null) {
            return false;
        } else {
            if (!level.isClientSide() && state.getDestroySpeed(level, pos) != 0.0F && tool.damagePerBlock() > 0) {
                stack.hurtAndBreak(tool.damagePerBlock(), miningEntity, EquipmentSlot.MAINHAND);
            }

            return true;
        }
    }

    public boolean isCorrectToolForDrops(ItemStack stack, BlockState state) {
        Tool tool = stack.get(DataComponents.TOOL);
        return tool != null && tool.isCorrectForDrops(state);
    }

    /**
     * Try interacting with given entity. Return {@code InteractionResult.PASS} if nothing should happen.
     */
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity interactionTarget, InteractionHand usedHand) {
        return InteractionResult.PASS;
    }

    @Override
    public String toString() {
        return BuiltInRegistries.ITEM.wrapAsHolder(this).getRegisteredName();
    }

    @Deprecated // Use ItemStack sensitive version.
    public final ItemStack getCraftingRemainder() {
        return this.craftingRemainingItem == null ? ItemStack.EMPTY : new ItemStack(this.craftingRemainingItem);
    }

    public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, @Nullable EquipmentSlot slot) {
    }

    public void onCraftedBy(ItemStack stack, Player player) {
        this.onCraftedPostProcess(stack, player.level());
    }

    public void onCraftedPostProcess(ItemStack stack, Level level) {
    }

    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        Consumable consumable = stack.get(DataComponents.CONSUMABLE);
        if (consumable != null) {
            return consumable.animation();
        } else {
            BlocksAttacks blocksattacks = stack.get(DataComponents.BLOCKS_ATTACKS);
            return blocksattacks != null ? ItemUseAnimation.BLOCK : ItemUseAnimation.NONE;
        }
    }

    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        Consumable consumable = stack.get(DataComponents.CONSUMABLE);
        if (consumable != null) {
            return consumable.consumeTicks();
        } else {
            BlocksAttacks blocksattacks = stack.get(DataComponents.BLOCKS_ATTACKS);
            return blocksattacks != null ? 72000 : 0;
        }
    }

    public boolean releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        return false;
    }

    @Deprecated
    public void appendHoverText(
        ItemStack stack, Item.TooltipContext context, TooltipDisplay tooltipDisplay, Consumer<Component> tooltipAdder, TooltipFlag flag
    ) {
    }

    public Optional<TooltipComponent> getTooltipImage(ItemStack stack) {
        return Optional.empty();
    }

    @VisibleForTesting
    public final String getDescriptionId() {
        return this.descriptionId;
    }

    public final Component getName() {
        return this.components.getOrDefault(DataComponents.ITEM_NAME, CommonComponents.EMPTY);
    }

    public Component getName(ItemStack stack) {
        return stack.getComponents().getOrDefault(DataComponents.ITEM_NAME, CommonComponents.EMPTY);
    }

    /**
     * Returns {@code true} if this item has an enchantment glint. By default, this returns {@code stack.isEnchanted()}, but other items can override it (for instance, written books always return true).
     *
     * Note that if you override this method, you generally want to also call the super version (on {@link Item}) to get the glint for enchanted items. Of course, that is unnecessary if the overwritten version always returns true.
     */
    public boolean isFoil(ItemStack stack) {
        return stack.isEnchanted();
    }

    public static BlockHitResult getPlayerPOVHitResult(Level level, Player player, ClipContext.Fluid fluidMode) {
        Vec3 vec3 = player.getEyePosition();
        Vec3 vec31 = vec3.add(player.calculateViewVector(player.getXRot(), player.getYRot()).scale(player.blockInteractionRange()));
        return level.clip(new ClipContext(vec3, vec31, ClipContext.Block.OUTLINE, fluidMode, player));
    }

    /**
     * If this stack's item is a crossbow
     */
    public boolean useOnRelease(ItemStack stack) {
        return stack.getItem() == Items.CROSSBOW;
    }

    protected final boolean canCombineRepair;

    @Override
    public boolean isCombineRepairable(ItemStack stack) {
        return canCombineRepair && isDamageable(stack);
    }

    public ItemStack getDefaultInstance() {
        return new ItemStack(this);
    }

    /**
     * @deprecated Neo: call {@link net.neoforged.neoforge.common.extensions.IItemStackExtension#canFitInsideContainerItems()} instead,
     *             prefer overriding {@link net.neoforged.neoforge.common.extensions.IItemExtension#canFitInsideContainerItems(ItemStack)}
     * @implNote It is recommended to still override this to ensure mods calling this instead of the extension still behave safely.
     */
    @Deprecated
    public boolean canFitInsideContainerItems() {
        return true;
    }

    @Override
    public FeatureFlagSet requiredFeatures() {
        return this.requiredFeatures;
    }

    public boolean shouldPrintOpWarning(ItemStack stack, @Nullable Player player) {
        return false;
    }

    public static class Properties implements net.neoforged.neoforge.common.extensions.IItemPropertiesExtensions {
        private static final DependantName<Item, String> BLOCK_DESCRIPTION_ID = p_371954_ -> Util.makeDescriptionId("block", p_371954_.location());
        private static final DependantName<Item, String> ITEM_DESCRIPTION_ID = p_371511_ -> Util.makeDescriptionId("item", p_371511_.location());
        private final DataComponentMap.Builder components = DataComponentMap.builder().addAll(DataComponents.COMMON_ITEM_COMPONENTS);
        @Nullable
        Item craftingRemainingItem;
        FeatureFlagSet requiredFeatures = FeatureFlags.VANILLA_SET;
        @Nullable
        private ResourceKey<Item> id;
        private DependantName<Item, String> descriptionId = ITEM_DESCRIPTION_ID;
        private DependantName<Item, ResourceLocation> model = ResourceKey::location;
        private boolean canCombineRepair = true;

        public Item.Properties setNoCombineRepair() {
            canCombineRepair = false;
            return this;
        }

        public Item.Properties food(FoodProperties food) {
            return this.food(food, Consumables.DEFAULT_FOOD);
        }

        public Item.Properties food(FoodProperties food, Consumable consumable) {
            return this.component(DataComponents.FOOD, food).component(DataComponents.CONSUMABLE, consumable);
        }

        public Item.Properties usingConvertsTo(Item usingConvertsTo) {
            return this.component(DataComponents.USE_REMAINDER, new UseRemainder(new ItemStack(usingConvertsTo)));
        }

        public Item.Properties useCooldown(float useCooldown) {
            return this.component(DataComponents.USE_COOLDOWN, new UseCooldown(useCooldown));
        }

        public Item.Properties stacksTo(int maxStackSize) {
            return this.component(DataComponents.MAX_STACK_SIZE, maxStackSize);
        }

        public Item.Properties durability(int maxDamage) {
            this.component(DataComponents.MAX_DAMAGE, maxDamage);
            this.component(DataComponents.MAX_STACK_SIZE, 1);
            this.component(DataComponents.DAMAGE, 0);
            return this;
        }

        public Item.Properties craftRemainder(Item craftingRemainingItem) {
            this.craftingRemainingItem = craftingRemainingItem;
            return this;
        }

        public Item.Properties rarity(Rarity rarity) {
            return this.component(DataComponents.RARITY, rarity);
        }

        public Item.Properties fireResistant() {
            return this.component(DataComponents.DAMAGE_RESISTANT, new DamageResistant(DamageTypeTags.IS_FIRE));
        }

        public Item.Properties jukeboxPlayable(ResourceKey<JukeboxSong> song) {
            return this.component(DataComponents.JUKEBOX_PLAYABLE, new JukeboxPlayable(new EitherHolder<>(song)));
        }

        public Item.Properties enchantable(int enchantmentValue) {
            return this.component(DataComponents.ENCHANTABLE, new Enchantable(enchantmentValue));
        }

        public Item.Properties repairable(Item repairItem) {
            return this.component(DataComponents.REPAIRABLE, new Repairable(HolderSet.direct(repairItem.builtInRegistryHolder())));
        }

        public Item.Properties repairable(TagKey<Item> repairItems) {
            HolderGetter<Item> holdergetter = BuiltInRegistries.acquireBootstrapRegistrationLookup(BuiltInRegistries.ITEM);
            return this.component(DataComponents.REPAIRABLE, new Repairable(holdergetter.getOrThrow(repairItems)));
        }

        public Item.Properties equippable(EquipmentSlot slot) {
            return this.component(DataComponents.EQUIPPABLE, Equippable.builder(slot).build());
        }

        public Item.Properties equippableUnswappable(EquipmentSlot slot) {
            return this.component(DataComponents.EQUIPPABLE, Equippable.builder(slot).setSwappable(false).build());
        }

        public Item.Properties tool(ToolMaterial material, TagKey<Block> mineableBlocks, float attackDamage, float attackSpeed, float disableBlockingForSeconds) {
            return material.applyToolProperties(this, mineableBlocks, attackDamage, attackSpeed, disableBlockingForSeconds);
        }

        public Item.Properties pickaxe(ToolMaterial material, float attackDamage, float attackSpeed) {
            return this.tool(material, BlockTags.MINEABLE_WITH_PICKAXE, attackDamage, attackSpeed, 0.0F);
        }

        public Item.Properties axe(ToolMaterial material, float attackDamage, float attackSpeed) {
            return this.tool(material, BlockTags.MINEABLE_WITH_AXE, attackDamage, attackSpeed, 5.0F);
        }

        public Item.Properties hoe(ToolMaterial material, float attackDamage, float attackSpeed) {
            return this.tool(material, BlockTags.MINEABLE_WITH_HOE, attackDamage, attackSpeed, 0.0F);
        }

        public Item.Properties shovel(ToolMaterial material, float attackDamage, float attackSpeed) {
            return this.tool(material, BlockTags.MINEABLE_WITH_SHOVEL, attackDamage, attackSpeed, 0.0F);
        }

        public Item.Properties sword(ToolMaterial material, float attackDamage, float attackSpeed) {
            return material.applySwordProperties(this, attackDamage, attackSpeed);
        }

        public Item.Properties spawnEgg(EntityType<?> entityType) {
            return this.component(DataComponents.ENTITY_DATA, TypedEntityData.of(entityType, new CompoundTag()));
        }

        public Item.Properties humanoidArmor(ArmorMaterial material, ArmorType type) {
            return this.durability(type.getDurability(material.durability()))
                .attributes(material.createAttributes(type))
                .enchantable(material.enchantmentValue())
                .component(
                    DataComponents.EQUIPPABLE,
                    Equippable.builder(type.getSlot()).setEquipSound(material.equipSound()).setAsset(material.assetId()).build()
                )
                .repairable(material.repairIngredient());
        }

        public Item.Properties wolfArmor(ArmorMaterial material) {
            return this.durability(ArmorType.BODY.getDurability(material.durability()))
                .attributes(material.createAttributes(ArmorType.BODY))
                .repairable(material.repairIngredient())
                .component(
                    DataComponents.EQUIPPABLE,
                    Equippable.builder(EquipmentSlot.BODY)
                        .setEquipSound(material.equipSound())
                        .setAsset(material.assetId())
                        .setAllowedEntities(HolderSet.direct(EntityType.WOLF.builtInRegistryHolder()))
                        .setCanBeSheared(true)
                        .setShearingSound(BuiltInRegistries.SOUND_EVENT.wrapAsHolder(SoundEvents.ARMOR_UNEQUIP_WOLF))
                        .build()
                )
                .component(DataComponents.BREAK_SOUND, SoundEvents.WOLF_ARMOR_BREAK)
                .stacksTo(1);
        }

        public Item.Properties horseArmor(ArmorMaterial material) {
            HolderGetter<EntityType<?>> holdergetter = BuiltInRegistries.acquireBootstrapRegistrationLookup(BuiltInRegistries.ENTITY_TYPE);
            return this.attributes(material.createAttributes(ArmorType.BODY))
                .component(
                    DataComponents.EQUIPPABLE,
                    Equippable.builder(EquipmentSlot.BODY)
                        .setEquipSound(SoundEvents.HORSE_ARMOR)
                        .setAsset(material.assetId())
                        .setAllowedEntities(holdergetter.getOrThrow(EntityTypeTags.CAN_WEAR_HORSE_ARMOR))
                        .setDamageOnHurt(false)
                        .setCanBeSheared(true)
                        .setShearingSound(SoundEvents.HORSE_ARMOR_UNEQUIP)
                        .build()
                )
                .stacksTo(1);
        }

        public Item.Properties trimMaterial(ResourceKey<TrimMaterial> trimMaterial) {
            return this.component(DataComponents.PROVIDES_TRIM_MATERIAL, new ProvidesTrimMaterial(trimMaterial));
        }

        public Item.Properties requiredFeatures(FeatureFlag... requiredFeatures) {
            this.requiredFeatures = FeatureFlags.REGISTRY.subset(requiredFeatures);
            return this;
        }

        public Item.Properties setId(ResourceKey<Item> id) {
            this.id = id;
            return this;
        }

        public Item.Properties overrideDescription(String description) {
            this.descriptionId = DependantName.fixed(description);
            return this;
        }

        public Item.Properties useBlockDescriptionPrefix() {
            this.descriptionId = BLOCK_DESCRIPTION_ID;
            return this;
        }

        public Item.Properties useItemDescriptionPrefix() {
            this.descriptionId = ITEM_DESCRIPTION_ID;
            return this;
        }

        protected String effectiveDescriptionId() {
            return this.descriptionId.get(Objects.requireNonNull(this.id, "Item id not set"));
        }

        public ResourceLocation effectiveModel() {
            return this.model.get(Objects.requireNonNull(this.id, "Item id not set"));
        }

        public <T> Item.Properties component(DataComponentType<T> component, T value) {
            net.neoforged.neoforge.common.CommonHooks.validateComponent(value);
            this.components.set(component, value);
            return this;
        }

        public Item.Properties attributes(ItemAttributeModifiers attributes) {
            return this.component(DataComponents.ATTRIBUTE_MODIFIERS, attributes);
        }

        DataComponentMap buildAndValidateComponents(Component itemName, ResourceLocation itemModel) {
            DataComponentMap datacomponentmap = this.components.set(DataComponents.ITEM_NAME, itemName).set(DataComponents.ITEM_MODEL, itemModel).build();
            return validateComponents(datacomponentmap);
        }

        public static DataComponentMap validateComponents(DataComponentMap datacomponentmap) {
            if (datacomponentmap.has(DataComponents.DAMAGE) && datacomponentmap.getOrDefault(DataComponents.MAX_STACK_SIZE, 1) > 1) {
                throw new IllegalStateException("Item cannot have both durability and be stackable");
            } else {
                return datacomponentmap;
            }
        }
    }

    public interface TooltipContext {
        Item.TooltipContext EMPTY = new Item.TooltipContext() {
            @Nullable
            @Override
            public HolderLookup.Provider registries() {
                return null;
            }

            @Override
            public float tickRate() {
                return 20.0F;
            }

            @Nullable
            @Override
            public MapItemSavedData mapData(MapId p_339618_) {
                return null;
            }

            @Override
            public boolean isPeaceful() {
                return false;
            }
        };

        @Nullable
        HolderLookup.Provider registries();

        float tickRate();

        @Nullable
        MapItemSavedData mapData(MapId mapId);

        boolean isPeaceful();

        /**
         * Neo: Returns the level if it's available.
         */
        @Nullable
        default Level level() {
            return null;
        }

        static Item.TooltipContext of(@Nullable final Level level) {
            return level == null ? EMPTY : new Item.TooltipContext() {
                @Override
                public HolderLookup.Provider registries() {
                    return level.registryAccess();
                }

                @Override
                public float tickRate() {
                    return level.tickRateManager().tickrate();
                }

                @Override
                public MapItemSavedData mapData(MapId p_339628_) {
                    return level.getMapData(p_339628_);
                }

                @Override
                public boolean isPeaceful() {
                    return level.getDifficulty() == Difficulty.PEACEFUL;
                }

                @Override
                public Level level() {
                    return level;
                }
            };
        }

        static Item.TooltipContext of(final HolderLookup.Provider registries) {
            return new Item.TooltipContext() {
                @Override
                public HolderLookup.Provider registries() {
                    return registries;
                }

                @Override
                public float tickRate() {
                    return 20.0F;
                }

                @Nullable
                @Override
                public MapItemSavedData mapData(MapId p_339679_) {
                    return null;
                }

                @Override
                public boolean isPeaceful() {
                    return false;
                }
            };
        }
    }
}
