package net.minecraft.world.item;

import com.google.common.collect.Lists;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.DataResult.Error;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentHolder;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.component.PatchedDataComponentMap;
import net.minecraft.core.component.TypedDataComponent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.Mth;
import net.minecraft.util.NullOps;
import net.minecraft.util.StringUtil;
import net.minecraft.util.Unit;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.component.DamageResistant;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.component.TooltipProvider;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.item.component.UseCooldown;
import net.minecraft.world.item.component.UseRemainder;
import net.minecraft.world.item.component.Weapon;
import net.minecraft.world.item.component.WrittenBookContent;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.enchantment.Repairable;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.Spawner;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import org.apache.commons.lang3.function.TriConsumer;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.slf4j.Logger;

public final class ItemStack implements DataComponentHolder, net.neoforged.neoforge.common.MutableDataComponentHolder, net.neoforged.neoforge.common.extensions.IItemStackExtension {
    private static final List<Component> OP_NBT_WARNING = List.of(
        Component.translatable("item.op_warning.line1").withStyle(ChatFormatting.RED, ChatFormatting.BOLD),
        Component.translatable("item.op_warning.line2").withStyle(ChatFormatting.RED),
        Component.translatable("item.op_warning.line3").withStyle(ChatFormatting.RED)
    );
    private static final Component UNBREAKABLE_TOOLTIP = Component.translatable("item.unbreakable").withStyle(ChatFormatting.BLUE);
    public static final MapCodec<ItemStack> MAP_CODEC = MapCodec.recursive(
        "ItemStack",
        p_393271_ -> RecordCodecBuilder.mapCodec(
            p_381569_ -> p_381569_.group(
                    Item.CODEC.fieldOf("id").forGetter(ItemStack::getItemHolder),
                    ExtraCodecs.intRange(1, 99).fieldOf("count").orElse(1).forGetter(ItemStack::getCount),
                    DataComponentPatch.CODEC.optionalFieldOf("components", DataComponentPatch.EMPTY).forGetter(p_330103_ -> p_330103_.components.asPatch())
                )
                .apply(p_381569_, ItemStack::new)
        )
    );
    public static final Codec<ItemStack> CODEC = Codec.lazyInitialized(MAP_CODEC::codec);
    public static final Codec<ItemStack> SINGLE_ITEM_CODEC = Codec.lazyInitialized(
        () -> RecordCodecBuilder.create(
            p_381570_ -> p_381570_.group(
                    Item.CODEC.fieldOf("id").forGetter(ItemStack::getItemHolder),
                    DataComponentPatch.CODEC.optionalFieldOf("components", DataComponentPatch.EMPTY).forGetter(p_332616_ -> p_332616_.components.asPatch())
                )
                .apply(p_381570_, (p_332614_, p_332615_) -> new ItemStack(p_332614_, 1, p_332615_))
        )
    );
    public static final Codec<ItemStack> STRICT_CODEC = CODEC.validate(ItemStack::validateStrict);
    public static final Codec<ItemStack> STRICT_SINGLE_ITEM_CODEC = SINGLE_ITEM_CODEC.validate(ItemStack::validateStrict);
    public static final Codec<ItemStack> OPTIONAL_CODEC = ExtraCodecs.optionalEmptyMap(CODEC)
        .xmap(p_330099_ -> p_330099_.orElse(ItemStack.EMPTY), p_330101_ -> p_330101_.isEmpty() ? Optional.empty() : Optional.of(p_330101_));
    public static final Codec<ItemStack> SIMPLE_ITEM_CODEC = Item.CODEC.xmap(ItemStack::new, ItemStack::getItemHolder);
    public static final StreamCodec<RegistryFriendlyByteBuf, ItemStack> OPTIONAL_STREAM_CODEC = createOptionalStreamCodec(DataComponentPatch.STREAM_CODEC);
    public static final StreamCodec<RegistryFriendlyByteBuf, ItemStack> OPTIONAL_UNTRUSTED_STREAM_CODEC = createOptionalStreamCodec(
        DataComponentPatch.DELIMITED_STREAM_CODEC
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, ItemStack> STREAM_CODEC = new StreamCodec<RegistryFriendlyByteBuf, ItemStack>() {
        public ItemStack decode(RegistryFriendlyByteBuf p_330597_) {
            ItemStack itemstack = ItemStack.OPTIONAL_STREAM_CODEC.decode(p_330597_);
            if (itemstack.isEmpty()) {
                throw new DecoderException("Empty ItemStack not allowed");
            } else {
                return itemstack;
            }
        }

        public void encode(RegistryFriendlyByteBuf p_331762_, ItemStack p_331138_) {
            if (p_331138_.isEmpty()) {
                throw new EncoderException("Empty ItemStack not allowed");
            } else {
                ItemStack.OPTIONAL_STREAM_CODEC.encode(p_331762_, p_331138_);
            }
        }
    };
    public static final StreamCodec<RegistryFriendlyByteBuf, List<ItemStack>> OPTIONAL_LIST_STREAM_CODEC = OPTIONAL_STREAM_CODEC.apply(
        ByteBufCodecs.collection(NonNullList::createWithCapacity)
    );
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final ItemStack EMPTY = new ItemStack((Void)null);
    private static final Component DISABLED_ITEM_TOOLTIP = Component.translatable("item.disabled").withStyle(ChatFormatting.RED);
    private int count;
    private int popTime;
    @Deprecated
    @Nullable
    private final Item item;
    final PatchedDataComponentMap components;
    /**
     * The entity the item is attached to, like an Item Frame.
     */
    @Nullable
    private Entity entityRepresentation;

    public static DataResult<ItemStack> validateStrict(ItemStack stack) {
        DataResult<Unit> dataresult = validateComponents(stack.getComponents());
        if (dataresult.isError()) {
            return dataresult.map(p_340777_ -> stack);
        } else {
            return stack.getCount() > stack.getMaxStackSize()
                ? DataResult.error(() -> "Item stack with stack size of " + stack.getCount() + " was larger than maximum: " + stack.getMaxStackSize())
                : DataResult.success(stack);
        }
    }

    private static StreamCodec<RegistryFriendlyByteBuf, ItemStack> createOptionalStreamCodec(
        final StreamCodec<RegistryFriendlyByteBuf, DataComponentPatch> codec
    ) {
        return new StreamCodec<RegistryFriendlyByteBuf, ItemStack>() {
            public ItemStack decode(RegistryFriendlyByteBuf p_320491_) {
                int i = p_320491_.readVarInt();
                if (i <= 0) {
                    return ItemStack.EMPTY;
                } else {
                    Holder<Item> holder = Item.STREAM_CODEC.decode(p_320491_);
                    DataComponentPatch datacomponentpatch = codec.decode(p_320491_);
                    return new ItemStack(holder, i, datacomponentpatch);
                }
            }

            public void encode(RegistryFriendlyByteBuf p_320527_, ItemStack p_320873_) {
                if (p_320873_.isEmpty()) {
                    p_320527_.writeVarInt(0);
                } else {
                    p_320527_.writeVarInt(p_320873_.getCount());
                    Item.STREAM_CODEC.encode(p_320527_, p_320873_.getItemHolder());
                    codec.encode(p_320527_, p_320873_.components.asPatch());
                }
            }
        };
    }

    public static StreamCodec<RegistryFriendlyByteBuf, ItemStack> validatedStreamCodec(final StreamCodec<RegistryFriendlyByteBuf, ItemStack> codec) {
        return new StreamCodec<RegistryFriendlyByteBuf, ItemStack>() {
            public ItemStack decode(RegistryFriendlyByteBuf p_341238_) {
                ItemStack itemstack = codec.decode(p_341238_);
                if (!itemstack.isEmpty()) {
                    RegistryOps<Unit> registryops = p_341238_.registryAccess().createSerializationContext(NullOps.INSTANCE);
                    ItemStack.CODEC.encodeStart(registryops, itemstack).getOrThrow(DecoderException::new);
                }

                return itemstack;
            }

            public void encode(RegistryFriendlyByteBuf p_341112_, ItemStack p_341358_) {
                codec.encode(p_341112_, p_341358_);
            }
        };
    }

    public Optional<TooltipComponent> getTooltipImage() {
        return this.getItem().getTooltipImage(this);
    }

    @Override
    public DataComponentMap getComponents() {
        return (DataComponentMap)(!this.isEmpty() ? this.components : DataComponentMap.EMPTY);
    }

    public DataComponentMap getPrototype() {
        return !this.isEmpty() ? this.getItem().components() : DataComponentMap.EMPTY;
    }

    public DataComponentPatch getComponentsPatch() {
        return !this.isEmpty() ? this.components.asPatch() : DataComponentPatch.EMPTY;
    }

    public DataComponentMap immutableComponents() {
        return !this.isEmpty() ? this.components.toImmutableMap() : DataComponentMap.EMPTY;
    }

    public boolean hasNonDefault(DataComponentType<?> component) {
        return !this.isEmpty() && this.components.hasNonDefault(component);
    }

    public boolean isComponentsPatchEmpty() {
        return this.isEmpty() || this.components.isPatchEmpty();
    }

    public ItemStack(ItemLike item) {
        this(item, 1);
    }

    public ItemStack(Holder<Item> tag) {
        this(tag.value(), 1);
    }

    public ItemStack(Holder<Item> tag, int count, DataComponentPatch components) {
        this(tag.value(), count, PatchedDataComponentMap.fromPatch(tag.value().components(), components));
    }

    public ItemStack(Holder<Item> item, int count) {
        this(item.value(), count);
    }

    public ItemStack(ItemLike item, int count) {
        this(item, count, new PatchedDataComponentMap(item.asItem().components()));
    }

    private ItemStack(ItemLike item, int count, PatchedDataComponentMap components) {
        this.item = item.asItem();
        this.count = count;
        this.components = components;
    }

    private ItemStack(@Nullable Void unused) {
        this.item = null;
        this.components = new PatchedDataComponentMap(DataComponentMap.EMPTY);
    }

    public static DataResult<Unit> validateComponents(DataComponentMap components) {
        if (components.has(DataComponents.MAX_DAMAGE) && components.getOrDefault(DataComponents.MAX_STACK_SIZE, 1) > 1) {
            return DataResult.error(() -> "Item cannot be both damageable and stackable");
        } else {
            ItemContainerContents itemcontainercontents = components.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY);

            for (ItemStack itemstack : itemcontainercontents.nonEmptyItems()) {
                int i = itemstack.getCount();
                int j = itemstack.getMaxStackSize();
                if (i > j) {
                    return DataResult.error(() -> "Item stack with count of " + i + " was larger than maximum: " + j);
                }
            }

            return DataResult.success(Unit.INSTANCE);
        }
    }

    public boolean isEmpty() {
        return this == EMPTY || this.item == Items.AIR || this.count <= 0;
    }

    public boolean isItemEnabled(FeatureFlagSet enabledFlags) {
        return this.isEmpty() || this.getItem().isEnabled(enabledFlags);
    }

    /**
     * Splits off a stack of the given amount of this stack and reduces this stack by the amount.
     */
    public ItemStack split(int amount) {
        int i = Math.min(amount, this.getCount());
        ItemStack itemstack = this.copyWithCount(i);
        this.shrink(i);
        return itemstack;
    }

    public ItemStack copyAndClear() {
        if (this.isEmpty()) {
            return EMPTY;
        } else {
            ItemStack itemstack = this.copy();
            this.setCount(0);
            return itemstack;
        }
    }

    public Item getItem() {
        return this.isEmpty() ? Items.AIR : this.item;
    }

    public Holder<Item> getItemHolder() {
        return this.getItem().builtInRegistryHolder();
    }

    public boolean is(TagKey<Item> tag) {
        return this.getItem().builtInRegistryHolder().is(tag);
    }

    public boolean is(Item item) {
        return this.getItem() == item;
    }

    public boolean is(Predicate<Holder<Item>> item) {
        return item.test(this.getItem().builtInRegistryHolder());
    }

    public boolean is(Holder<Item> item) {
        return is(item.value()); // Neo: Fix comparing for custom holders such as DeferredHolders
    }

    public boolean is(HolderSet<Item> item) {
        return item.contains(this.getItemHolder());
    }

    public Stream<TagKey<Item>> getTags() {
        return this.getItem().builtInRegistryHolder().tags();
    }

    public InteractionResult useOn(UseOnContext context) {
        var e = net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.entity.player.UseItemOnBlockEvent(context, net.neoforged.neoforge.event.entity.player.UseItemOnBlockEvent.UsePhase.ITEM_AFTER_BLOCK));
        if (e.isCanceled()) return e.getCancellationResult();
        if (!context.getLevel().isClientSide()) return net.neoforged.neoforge.common.CommonHooks.onPlaceItemIntoWorld(context);
        return onItemUse(context, (c) -> getItem().useOn(context));
    }

    @Override
    public InteractionResult onItemUseFirst(UseOnContext p_41662_) {
        var e = net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.entity.player.UseItemOnBlockEvent(p_41662_, net.neoforged.neoforge.event.entity.player.UseItemOnBlockEvent.UsePhase.ITEM_BEFORE_BLOCK));
        if (e.isCanceled()) return e.getCancellationResult();
        return onItemUse(p_41662_, (c) -> getItem().onItemUseFirst(this, p_41662_));
    }

    private InteractionResult onItemUse(UseOnContext p_41662_, java.util.function.Function<UseOnContext, InteractionResult> callback) {
        Player player = p_41662_.getPlayer();
        BlockPos blockpos = p_41662_.getClickedPos();
        if (player != null && !player.getAbilities().mayBuild && !this.canPlaceOnBlockInAdventureMode(new BlockInWorld(p_41662_.getLevel(), blockpos, false))) {
            return InteractionResult.PASS;
        } else {
            Item item = this.getItem();
            InteractionResult interactionresult = callback.apply(p_41662_);
            if (player != null
                && interactionresult instanceof InteractionResult.Success interactionresult$success
                && interactionresult$success.wasItemInteraction()) {
                player.awardStat(Stats.ITEM_USED.get(item));
            }

            return interactionresult;
        }
    }

    public float getDestroySpeed(BlockState state) {
        return this.getItem().getDestroySpeed(this, state);
    }

    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack itemstack = this.copy();
        boolean flag = this.getUseDuration(player) <= 0;
        InteractionResult interactionresult = this.getItem().use(level, player, hand);
        return (InteractionResult)(flag && interactionresult instanceof InteractionResult.Success interactionresult$success
            ? interactionresult$success.heldItemTransformedTo(
                interactionresult$success.heldItemTransformedTo() == null
                    ? this.applyAfterUseComponentSideEffects(player, itemstack)
                    : interactionresult$success.heldItemTransformedTo().applyAfterUseComponentSideEffects(player, itemstack)
            )
            : interactionresult);
    }

    /**
     * Called when the item in use count reach 0, e.g. item food eaten. Return the new ItemStack. Args : world, entity
     */
    public ItemStack finishUsingItem(Level level, LivingEntity livingEntity) {
        ItemStack itemstack = this.copy();
        ItemStack itemstack1 = this.getItem().finishUsingItem(this, level, livingEntity);
        return itemstack1.applyAfterUseComponentSideEffects(livingEntity, itemstack);
    }

    private ItemStack applyAfterUseComponentSideEffects(LivingEntity entity, ItemStack stack) {
        UseRemainder useremainder = stack.get(DataComponents.USE_REMAINDER);
        UseCooldown usecooldown = stack.get(DataComponents.USE_COOLDOWN);
        int i = stack.getCount();
        ItemStack itemstack = this;
        if (useremainder != null) {
            itemstack = useremainder.convertIntoRemainder(this, i, entity.hasInfiniteMaterials(), entity::handleExtraItemsCreatedOnUse);
        }

        if (usecooldown != null) {
            usecooldown.apply(stack, entity);
        }

        return itemstack;
    }

    public int getMaxStackSize() {
        return this.getItem().getMaxStackSize(this);
    }

    public boolean isStackable() {
        return this.getMaxStackSize() > 1 && (!this.isDamageableItem() || !this.isDamaged());
    }

    public boolean isDamageableItem() {
        return this.has(DataComponents.MAX_DAMAGE) && !this.has(DataComponents.UNBREAKABLE) && this.has(DataComponents.DAMAGE);
    }

    public boolean isDamaged() {
        return this.isDamageableItem() && getItem().isDamaged(this);
    }

    public int getDamageValue() {
        return this.getItem().getDamage(this);
    }

    public void setDamageValue(int damage) {
        this.getItem().setDamage(this, damage);
    }

    public int getMaxDamage() {
        return this.getItem().getMaxDamage(this);
    }

    public boolean isBroken() {
        return this.isDamageableItem() && this.getDamageValue() >= this.getMaxDamage();
    }

    public boolean nextDamageWillBreak() {
        return this.isDamageableItem() && this.getDamageValue() >= this.getMaxDamage() - 1;
    }

    public void hurtAndBreak(int damage, ServerLevel level, @Nullable ServerPlayer player, Consumer<Item> onBreak) {
        this.hurtAndBreak(damage, level, (LivingEntity) player, onBreak);
    }

    public void hurtAndBreak(int p_220158_, ServerLevel p_346256_, @Nullable LivingEntity p_220160_, Consumer<Item> p_348596_) {
        p_220158_ = getItem().damageItem(this, p_220158_, p_220160_, p_348596_);
        int i = this.processDurabilityChange(p_220158_, p_346256_, p_220160_);
        if (i != 0) {
            this.applyDamage(this.getDamageValue() + i, p_220160_, p_348596_);
        }
    }

    private int processDurabilityChange(int damage, ServerLevel level, @Nullable ServerPlayer player) {
        return processDurabilityChange(damage, level, (LivingEntity) player);
    }

    private int processDurabilityChange(int p_361290_, ServerLevel p_361409_, @Nullable LivingEntity p_364940_) {
        if (!this.isDamageableItem()) {
            return 0;
        } else if (p_364940_ != null && p_364940_.hasInfiniteMaterials()) {
            return 0;
        } else {
            return p_361290_ > 0 ? EnchantmentHelper.processDurabilityChange(p_361409_, this, p_361290_) : p_361290_;
        }
    }

    private void applyDamage(int damage, @Nullable ServerPlayer player, Consumer<Item> onBreak) {
        applyDamage(damage, (LivingEntity) player, onBreak);
    }

    private void applyDamage(int p_361754_, @Nullable LivingEntity p_364853_, Consumer<Item> p_360895_) {
        if (p_364853_ instanceof ServerPlayer serverPlayer) {
            CriteriaTriggers.ITEM_DURABILITY_CHANGED.trigger(serverPlayer, this, p_361754_);
        }

        this.setDamageValue(p_361754_);
        if (this.isBroken()) {
            Item item = this.getItem();
            this.shrink(1);
            p_360895_.accept(item);
        }
    }

    public void hurtWithoutBreaking(int damage, Player player) {
        if (player instanceof ServerPlayer serverplayer) {
            int i = this.processDurabilityChange(damage, serverplayer.level(), serverplayer);
            if (i == 0) {
                return;
            }

            int j = Math.min(this.getDamageValue() + i, this.getMaxDamage() - 1);
            this.applyDamage(j, serverplayer, p_360034_ -> {});
        }
    }

    public void hurtAndBreak(int amount, LivingEntity entity, InteractionHand hand) {
        this.hurtAndBreak(amount, entity, hand.asEquipmentSlot());
    }

    public void hurtAndBreak(int amount, LivingEntity entity, EquipmentSlot slot) {
        if (entity.level() instanceof ServerLevel serverlevel) {
            this.hurtAndBreak(
                amount,
                serverlevel,
                entity,
                p_348383_ -> entity.onEquippedItemBroken(p_348383_, slot)
            );
        }
    }

    public ItemStack hurtAndConvertOnBreak(int amount, ItemLike item, LivingEntity entity, EquipmentSlot slot) {
        this.hurtAndBreak(amount, entity, slot);
        if (this.isEmpty()) {
            ItemStack itemstack = this.transmuteCopyIgnoreEmpty(item, 1);
            if (itemstack.isDamageableItem()) {
                itemstack.setDamageValue(0);
            }

            return itemstack;
        } else {
            return this;
        }
    }

    public boolean isBarVisible() {
        return this.getItem().isBarVisible(this);
    }

    public int getBarWidth() {
        return this.getItem().getBarWidth(this);
    }

    public int getBarColor() {
        return this.getItem().getBarColor(this);
    }

    public boolean overrideStackedOnOther(Slot slot, ClickAction action, Player player) {
        return this.getItem().overrideStackedOnOther(this, slot, action, player);
    }

    public boolean overrideOtherStackedOnMe(ItemStack stack, Slot slot, ClickAction action, Player player, SlotAccess access) {
        return this.getItem().overrideOtherStackedOnMe(this, stack, slot, action, player, access);
    }

    public boolean hurtEnemy(LivingEntity enemy, LivingEntity attacker) {
        Item item = this.getItem();
        item.hurtEnemy(this, enemy, attacker);
        if (this.has(DataComponents.WEAPON)) {
            if (attacker instanceof Player player) {
                player.awardStat(Stats.ITEM_USED.get(item));
            }

            return true;
        } else {
            return false;
        }
    }

    public void postHurtEnemy(LivingEntity enemy, LivingEntity attacker) {
        this.getItem().postHurtEnemy(this, enemy, attacker);
        Weapon weapon = this.get(DataComponents.WEAPON);
        if (weapon != null) {
            this.hurtAndBreak(weapon.itemDamagePerAttack(), attacker, EquipmentSlot.MAINHAND);
        }
    }

    /**
     * Called when a Block is destroyed using this ItemStack
     */
    public void mineBlock(Level level, BlockState state, BlockPos pos, Player player) {
        Item item = this.getItem();
        if (item.mineBlock(this, level, state, pos, player)) {
            player.awardStat(Stats.ITEM_USED.get(item));
        }
    }

    /**
     * Check whether the given Block can be harvested using this ItemStack.
     */
    public boolean isCorrectToolForDrops(BlockState state) {
        return this.getItem().isCorrectToolForDrops(this, state);
    }

    public InteractionResult interactLivingEntity(Player player, LivingEntity entity, InteractionHand usedHand) {
        Equippable equippable = this.get(DataComponents.EQUIPPABLE);
        if (equippable != null && equippable.equipOnInteract()) {
            InteractionResult interactionresult = equippable.equipOnTarget(player, entity, this);
            if (interactionresult != InteractionResult.PASS) {
                return interactionresult;
            }
        }

        return this.getItem().interactLivingEntity(this, player, entity, usedHand);
    }

    public ItemStack copy() {
        if (this.isEmpty()) {
            return EMPTY;
        } else {
            ItemStack itemstack = new ItemStack(this.getItem(), this.count, this.components.copy());
            itemstack.setPopTime(this.getPopTime());
            return itemstack;
        }
    }

    public ItemStack copyWithCount(int count) {
        if (this.isEmpty()) {
            return EMPTY;
        } else {
            ItemStack itemstack = this.copy();
            itemstack.setCount(count);
            return itemstack;
        }
    }

    public ItemStack transmuteCopy(ItemLike item) {
        return this.transmuteCopy(item, this.getCount());
    }

    public ItemStack transmuteCopy(ItemLike item, int count) {
        return this.isEmpty() ? EMPTY : this.transmuteCopyIgnoreEmpty(item, count);
    }

    private ItemStack transmuteCopyIgnoreEmpty(ItemLike item, int count) {
        return new ItemStack(item.asItem().builtInRegistryHolder(), count, this.components.asPatch());
    }

    /**
     * Compares both {@code ItemStacks}, returns {@code true} if both {@code ItemStacks} are equal.
     */
    public static boolean matches(ItemStack stack, ItemStack other) {
        if (stack == other) {
            return true;
        } else {
            return stack.getCount() != other.getCount() ? false : isSameItemSameComponents(stack, other);
        }
    }

    @Deprecated
    public static boolean listMatches(List<ItemStack> list, List<ItemStack> other) {
        if (list.size() != other.size()) {
            return false;
        } else {
            for (int i = 0; i < list.size(); i++) {
                if (!matches(list.get(i), other.get(i))) {
                    return false;
                }
            }

            return true;
        }
    }

    public static boolean isSameItem(ItemStack stack, ItemStack other) {
        return stack.is(other.getItem());
    }

    public static boolean isSameItemSameComponents(ItemStack stack, ItemStack other) {
        if (!stack.is(other.getItem())) {
            return false;
        } else {
            return stack.isEmpty() && other.isEmpty() ? true : Objects.equals(stack.components, other.components);
        }
    }

    public static MapCodec<ItemStack> lenientOptionalFieldOf(String fieldName) {
        return CODEC.lenientOptionalFieldOf(fieldName)
            .xmap(p_323389_ -> p_323389_.orElse(EMPTY), p_323388_ -> p_323388_.isEmpty() ? Optional.empty() : Optional.of(p_323388_));
    }

    public static int hashItemAndComponents(@Nullable ItemStack stack) {
        if (stack != null) {
            int i = 31 + stack.getItem().hashCode();
            return 31 * i + stack.getComponents().hashCode();
        } else {
            return 0;
        }
    }

    @Deprecated
    public static int hashStackList(List<ItemStack> list) {
        int i = 0;

        for (ItemStack itemstack : list) {
            i = i * 31 + hashItemAndComponents(itemstack);
        }

        return i;
    }

    @Override
    public String toString() {
        return this.getCount() + " " + this.getItem();
    }

    public void inventoryTick(Level level, Entity entity, @Nullable EquipmentSlot slot) {
        if (this.popTime > 0) {
            this.popTime--;
        }

        if (level instanceof ServerLevel serverlevel) {
            this.getItem().inventoryTick(this, serverlevel, entity, slot);
        }
    }

    public void onCraftedBy(Player player, int amount) {
        player.awardStat(Stats.ITEM_CRAFTED.get(this.getItem()), amount);
        this.getItem().onCraftedBy(this, player);
    }

    public void onCraftedBySystem(Level level) {
        this.getItem().onCraftedPostProcess(this, level);
    }

    public int getUseDuration(LivingEntity entity) {
        return this.getItem().getUseDuration(this, entity);
    }

    public ItemUseAnimation getUseAnimation() {
        return this.getItem().getUseAnimation(this);
    }

    /**
     * Called when the player releases the use item button.
     */
    public void releaseUsing(Level level, LivingEntity livingEntity, int timeLeft) {
        ItemStack itemstack = this.copy();
        if (this.getItem().releaseUsing(this, level, livingEntity, timeLeft)) {
            ItemStack itemstack1 = this.applyAfterUseComponentSideEffects(livingEntity, itemstack);
            if (itemstack1 != this) {
                livingEntity.setItemInHand(livingEntity.getUsedItemHand(), itemstack1);
            }
        }
    }

    public boolean useOnRelease() {
        return this.getItem().useOnRelease(this);
    }

    @Nullable
    public <T> T set(DataComponentType<T> component, @Nullable T value) {
        return this.components.set(component, value);
    }

    @Nullable
    public <T> T set(TypedDataComponent<T> component) {
        return this.components.set(component);
    }

    public <T> void copyFrom(DataComponentType<T> componentType, DataComponentGetter componentGetter) {
        this.set(componentType, componentGetter.get(componentType));
    }

    @Nullable
    public <T, U> T update(DataComponentType<T> component, T defaultValue, U updateValue, BiFunction<T, U, T> updater) {
        return this.set(component, updater.apply(this.getOrDefault(component, defaultValue), updateValue));
    }

    @Nullable
    public <T> T update(DataComponentType<T> component, T defaultValue, UnaryOperator<T> updater) {
        T t = this.getOrDefault(component, defaultValue);
        return this.set(component, updater.apply(t));
    }

    @Nullable
    public <T> T remove(DataComponentType<? extends T> component) {
        return this.components.remove(component);
    }

    public void applyComponentsAndValidate(DataComponentPatch components) {
        DataComponentPatch datacomponentpatch = this.components.asPatch();
        this.components.applyPatch(components);
        Optional<Error<ItemStack>> optional = validateStrict(this).error();
        if (optional.isPresent()) {
            LOGGER.error("Failed to apply component patch '{}' to item: '{}'", components, optional.get().message());
            this.components.restorePatch(datacomponentpatch);
        }
    }

    public void applyComponents(DataComponentPatch components) {
        this.components.applyPatch(components);
    }

    public void applyComponents(DataComponentMap components) {
        this.components.setAll(components);
    }

    public Component getHoverName() {
        Component component = this.getCustomName();
        return component != null ? component : this.getItemName();
    }

    @Nullable
    public Component getCustomName() {
        Component component = this.get(DataComponents.CUSTOM_NAME);
        if (component != null) {
            return component;
        } else {
            WrittenBookContent writtenbookcontent = this.get(DataComponents.WRITTEN_BOOK_CONTENT);
            if (writtenbookcontent != null) {
                String s = writtenbookcontent.title().raw();
                if (!StringUtil.isBlank(s)) {
                    return Component.literal(s);
                }
            }

            return null;
        }
    }

    public Component getItemName() {
        return this.getItem().getName(this);
    }

    public Component getStyledHoverName() {
        MutableComponent mutablecomponent = Component.empty().append(this.getHoverName()).withStyle(this.getRarity().getStyleModifier());
        if (this.has(DataComponents.CUSTOM_NAME)) {
            mutablecomponent.withStyle(ChatFormatting.ITALIC);
        }

        return mutablecomponent;
    }

    public <T extends TooltipProvider> void addToTooltip(
        DataComponentType<T> component, Item.TooltipContext context, TooltipDisplay tooltipDisplay, Consumer<Component> tooltipAdder, TooltipFlag tooltipFlag
    ) {
        T t = (T)this.get(component);
        if (t != null && tooltipDisplay.shows(component)) {
            t.addToTooltip(context, tooltipAdder, tooltipFlag, this.components);
        }
    }

    public List<Component> getTooltipLines(Item.TooltipContext tooltipContext, @Nullable Player player, TooltipFlag tooltipFlag) {
        TooltipDisplay tooltipdisplay = this.getOrDefault(DataComponents.TOOLTIP_DISPLAY, TooltipDisplay.DEFAULT);
        if (!tooltipFlag.isCreative() && tooltipdisplay.hideTooltip()) {
            boolean flag = this.getItem().shouldPrintOpWarning(this, player);
            return flag ? OP_NBT_WARNING : List.of();
        } else {
            List<Component> list = Lists.newArrayList();
            list.add(this.getStyledHoverName());
            this.addDetailsToTooltip(tooltipContext, tooltipdisplay, player, tooltipFlag, list::add);
            net.neoforged.neoforge.event.EventHooks.onItemTooltip(this, player, list, tooltipFlag, tooltipContext);
            return list;
        }
    }

    public void addDetailsToTooltip(
        Item.TooltipContext context, TooltipDisplay tooltipDisplay, @Nullable Player player, TooltipFlag tooltipFlag, Consumer<Component> tooltipAdder
    ) {
        this.getItem().appendHoverText(this, context, tooltipDisplay, tooltipAdder, tooltipFlag);
        this.addToTooltip(DataComponents.TROPICAL_FISH_PATTERN, context, tooltipDisplay, tooltipAdder, tooltipFlag);
        this.addToTooltip(DataComponents.INSTRUMENT, context, tooltipDisplay, tooltipAdder, tooltipFlag);
        this.addToTooltip(DataComponents.MAP_ID, context, tooltipDisplay, tooltipAdder, tooltipFlag);
        this.addToTooltip(DataComponents.BEES, context, tooltipDisplay, tooltipAdder, tooltipFlag);
        this.addToTooltip(DataComponents.CONTAINER_LOOT, context, tooltipDisplay, tooltipAdder, tooltipFlag);
        this.addToTooltip(DataComponents.CONTAINER, context, tooltipDisplay, tooltipAdder, tooltipFlag);
        this.addToTooltip(DataComponents.BANNER_PATTERNS, context, tooltipDisplay, tooltipAdder, tooltipFlag);
        this.addToTooltip(DataComponents.POT_DECORATIONS, context, tooltipDisplay, tooltipAdder, tooltipFlag);
        this.addToTooltip(DataComponents.WRITTEN_BOOK_CONTENT, context, tooltipDisplay, tooltipAdder, tooltipFlag);
        this.addToTooltip(DataComponents.CHARGED_PROJECTILES, context, tooltipDisplay, tooltipAdder, tooltipFlag);
        this.addToTooltip(DataComponents.FIREWORKS, context, tooltipDisplay, tooltipAdder, tooltipFlag);
        this.addToTooltip(DataComponents.FIREWORK_EXPLOSION, context, tooltipDisplay, tooltipAdder, tooltipFlag);
        this.addToTooltip(DataComponents.POTION_CONTENTS, context, tooltipDisplay, tooltipAdder, tooltipFlag);
        this.addToTooltip(DataComponents.JUKEBOX_PLAYABLE, context, tooltipDisplay, tooltipAdder, tooltipFlag);
        this.addToTooltip(DataComponents.TRIM, context, tooltipDisplay, tooltipAdder, tooltipFlag);
        this.addToTooltip(DataComponents.STORED_ENCHANTMENTS, context, tooltipDisplay, tooltipAdder, tooltipFlag);
        this.addToTooltip(DataComponents.ENCHANTMENTS, context, tooltipDisplay, tooltipAdder, tooltipFlag);
        this.addToTooltip(DataComponents.DYED_COLOR, context, tooltipDisplay, tooltipAdder, tooltipFlag);
        this.addToTooltip(DataComponents.PROFILE, context, tooltipDisplay, tooltipAdder, tooltipFlag);
        this.addToTooltip(DataComponents.LORE, context, tooltipDisplay, tooltipAdder, tooltipFlag);
        // Neo: Replace attribute tooltips with custom handling
        net.neoforged.neoforge.common.util.AttributeUtil.addAttributeTooltips(this, tooltipAdder, tooltipDisplay, net.neoforged.neoforge.common.util.AttributeTooltipContext.of(player, context, tooltipDisplay, tooltipFlag));
        if (this.has(DataComponents.UNBREAKABLE) && tooltipDisplay.shows(DataComponents.UNBREAKABLE)) {
            tooltipAdder.accept(UNBREAKABLE_TOOLTIP);
        }

        this.addToTooltip(DataComponents.OMINOUS_BOTTLE_AMPLIFIER, context, tooltipDisplay, tooltipAdder, tooltipFlag);
        this.addToTooltip(DataComponents.SUSPICIOUS_STEW_EFFECTS, context, tooltipDisplay, tooltipAdder, tooltipFlag);
        this.addToTooltip(DataComponents.BLOCK_STATE, context, tooltipDisplay, tooltipAdder, tooltipFlag);
        this.addToTooltip(DataComponents.ENTITY_DATA, context, tooltipDisplay, tooltipAdder, tooltipFlag);
        if ((this.is(Items.SPAWNER) || this.is(Items.TRIAL_SPAWNER)) && tooltipDisplay.shows(DataComponents.BLOCK_ENTITY_DATA)) {
            TypedEntityData<BlockEntityType<?>> typedentitydata = this.get(DataComponents.BLOCK_ENTITY_DATA);
            Spawner.appendHoverText(typedentitydata, tooltipAdder, "SpawnData");
        }

        AdventureModePredicate adventuremodepredicate1 = this.get(DataComponents.CAN_BREAK);
        if (adventuremodepredicate1 != null && tooltipDisplay.shows(DataComponents.CAN_BREAK)) {
            tooltipAdder.accept(CommonComponents.EMPTY);
            tooltipAdder.accept(AdventureModePredicate.CAN_BREAK_HEADER);
            adventuremodepredicate1.addToTooltip(tooltipAdder);
        }

        AdventureModePredicate adventuremodepredicate = this.get(DataComponents.CAN_PLACE_ON);
        if (adventuremodepredicate != null && tooltipDisplay.shows(DataComponents.CAN_PLACE_ON)) {
            tooltipAdder.accept(CommonComponents.EMPTY);
            tooltipAdder.accept(AdventureModePredicate.CAN_PLACE_HEADER);
            adventuremodepredicate.addToTooltip(tooltipAdder);
        }

        if (tooltipFlag.isAdvanced()) {
            if (this.isDamaged() && tooltipDisplay.shows(DataComponents.DAMAGE)) {
                tooltipAdder.accept(Component.translatable("item.durability", this.getMaxDamage() - this.getDamageValue(), this.getMaxDamage()));
            }

            tooltipAdder.accept(Component.literal(BuiltInRegistries.ITEM.getKey(this.getItem()).toString()).withStyle(ChatFormatting.DARK_GRAY));
            int i = this.components.size();
            if (i > 0) {
                tooltipAdder.accept(Component.translatable("item.components", i).withStyle(ChatFormatting.DARK_GRAY));
            }
        }

        if (player != null && !this.getItem().isEnabled(player.level().enabledFeatures())) {
            tooltipAdder.accept(DISABLED_ITEM_TOOLTIP);
        }

        boolean flag = this.getItem().shouldPrintOpWarning(this, player);
        if (flag) {
            OP_NBT_WARNING.forEach(tooltipAdder);
        }
    }
    /**
     * @deprecated Neo: Use {@link net.neoforged.neoforge.common.util.AttributeUtil#
     *             addAttributeTooltips}
     */
    @Deprecated
    private void addAttributeTooltips(Consumer<Component> tooltipAdder, TooltipDisplay tooltipDisplay, @Nullable Player player) {
        if (tooltipDisplay.shows(DataComponents.ATTRIBUTE_MODIFIERS)) {
            for (EquipmentSlotGroup equipmentslotgroup : EquipmentSlotGroup.values()) {
                MutableBoolean mutableboolean = new MutableBoolean(true);
                this.forEachModifier(equipmentslotgroup, (p_415402_, p_415403_, p_415404_) -> {
                    if (p_415404_ != ItemAttributeModifiers.Display.hidden()) {
                        if (mutableboolean.isTrue()) {
                            tooltipAdder.accept(CommonComponents.EMPTY);
                            tooltipAdder.accept(Component.translatable("item.modifiers." + equipmentslotgroup.getSerializedName()).withStyle(ChatFormatting.GRAY));
                            mutableboolean.setFalse();
                        }

                        p_415404_.apply(tooltipAdder, player, p_415402_, p_415403_);
                    }
                });
            }
        }
    }

    public boolean hasFoil() {
        Boolean obool = this.get(DataComponents.ENCHANTMENT_GLINT_OVERRIDE);
        return obool != null ? obool : this.getItem().isFoil(this);
    }

    public Rarity getRarity() {
        Rarity rarity = this.getOrDefault(DataComponents.RARITY, Rarity.COMMON);
        if (!this.isEnchanted()) {
            return rarity;
        } else {
            return switch (rarity) {
                case COMMON, UNCOMMON -> Rarity.RARE;
                case RARE -> Rarity.EPIC;
                default -> rarity;
            };
        }
    }

    public boolean isEnchantable() {
        if (!this.has(DataComponents.ENCHANTABLE)) {
            return false;
        } else {
            ItemEnchantments itemenchantments = this.get(DataComponents.ENCHANTMENTS);
            return itemenchantments != null && itemenchantments.isEmpty();
        }
    }

    public void enchant(Holder<Enchantment> enchantment, int level) {
        EnchantmentHelper.updateEnchantments(this, p_344404_ -> p_344404_.upgrade(enchantment, level));
    }

    public boolean isEnchanted() {
        return !this.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY).isEmpty();
    }

    /**
     * Gets all enchantments from NBT. Use {@link ItemStack#getAllEnchantments} for gameplay logic.
     */
    public ItemEnchantments getTagEnchantments() {
        return getEnchantments();
    }

    /**
     * @deprecated Neo: Use {@link #getTagEnchantments()} for NBT enchantments, or {@link #getAllEnchantments} for gameplay.
     */
    @Deprecated
    public ItemEnchantments getEnchantments() {
        return this.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
    }

    public boolean isFramed() {
        return this.entityRepresentation instanceof ItemFrame;
    }

    public void setEntityRepresentation(@Nullable Entity entity) {
        if (!this.isEmpty()) {
            this.entityRepresentation = entity;
        }
    }

    @Nullable
    public ItemFrame getFrame() {
        return this.entityRepresentation instanceof ItemFrame ? (ItemFrame)this.getEntityRepresentation() : null;
    }

    @Nullable
    public Entity getEntityRepresentation() {
        return !this.isEmpty() ? this.entityRepresentation : null;
    }

    public void forEachModifier(EquipmentSlotGroup slot, TriConsumer<Holder<Attribute>, AttributeModifier, ItemAttributeModifiers.Display> action) {
        // Neo: Reflect real attribute modifiers when doing iteration
        this.getAttributeModifiers().forEach(slot, action);
        if (false) {
        // Start disabled vanilla code
        ItemAttributeModifiers itemattributemodifiers = this.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
        itemattributemodifiers.forEach(slot, action);
        // end disabled vanilla code
        }
        EnchantmentHelper.forEachModifier(
            this, slot, (p_415406_, p_415407_) -> action.accept(p_415406_, p_415407_, ItemAttributeModifiers.Display.attributeModifiers())
        );
    }

    public void forEachModifier(EquipmentSlot equipmentSLot, BiConsumer<Holder<Attribute>, AttributeModifier> action) {
        // Neo: Reflect real attribute modifiers when doing iteration
        this.getAttributeModifiers().forEach(equipmentSLot, action);
        if (false) {
        // Start disabled vanilla code
        ItemAttributeModifiers itemattributemodifiers = this.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
        itemattributemodifiers.forEach(equipmentSLot, action);
        // end disabled vanilla code
        }
        EnchantmentHelper.forEachModifier(this, equipmentSLot, action);
    }

    public Component getDisplayName() {
        MutableComponent mutablecomponent = Component.empty().append(this.getHoverName());
        if (this.has(DataComponents.CUSTOM_NAME)) {
            mutablecomponent.withStyle(ChatFormatting.ITALIC);
        }

        MutableComponent mutablecomponent1 = ComponentUtils.wrapInSquareBrackets(mutablecomponent);
        if (!this.isEmpty()) {
            mutablecomponent1.withStyle(this.getRarity().getStyleModifier()).withStyle(p_393272_ -> p_393272_.withHoverEvent(new HoverEvent.ShowItem(this)));
        }

        return mutablecomponent1;
    }

    public boolean canPlaceOnBlockInAdventureMode(BlockInWorld block) {
        AdventureModePredicate adventuremodepredicate = this.get(DataComponents.CAN_PLACE_ON);
        return adventuremodepredicate != null && adventuremodepredicate.test(block);
    }

    public boolean canBreakBlockInAdventureMode(BlockInWorld block) {
        AdventureModePredicate adventuremodepredicate = this.get(DataComponents.CAN_BREAK);
        return adventuremodepredicate != null && adventuremodepredicate.test(block);
    }

    public int getPopTime() {
        return this.popTime;
    }

    public void setPopTime(int popTime) {
        this.popTime = popTime;
    }

    public int getCount() {
        return this.isEmpty() ? 0 : this.count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public void limitSize(int maxSize) {
        if (!this.isEmpty() && this.getCount() > maxSize) {
            this.setCount(maxSize);
        }
    }

    public void grow(int increment) {
        this.setCount(this.getCount() + increment);
    }

    public void shrink(int decrement) {
        this.grow(-decrement);
    }

    public void consume(int amount, @Nullable LivingEntity entity) {
        if (entity == null || !entity.hasInfiniteMaterials()) {
            this.shrink(amount);
        }
    }

    public ItemStack consumeAndReturn(int amount, @Nullable LivingEntity entity) {
        ItemStack itemstack = this.copyWithCount(amount);
        this.consume(amount, entity);
        return itemstack;
    }

    /**
     * Called as the stack is being used by an entity.
     */
    public void onUseTick(Level level, LivingEntity livingEntity, int remainingUseDuration) {
        Consumable consumable = this.get(DataComponents.CONSUMABLE);
        if (consumable != null && consumable.shouldEmitParticlesAndSounds(remainingUseDuration)) {
            consumable.emitParticlesAndSounds(livingEntity.getRandom(), livingEntity, this, 5);
        }

        this.getItem().onUseTick(level, livingEntity, this, remainingUseDuration);
    }

    /**
 * @deprecated Forge: Use {@linkplain
 *             net.neoforged.neoforge.common.extensions.IItemStackExtension#
 *             onDestroyed(ItemEntity,
 *             net.minecraft.world.damagesource.DamageSource) damage source
 *             sensitive version}
 */
    @Deprecated
    public void onDestroyed(ItemEntity itemEntity) {
        this.getItem().onDestroyed(itemEntity);
    }

    public boolean canBeHurtBy(DamageSource damageSource) {
        if (!getItem().canBeHurtBy(this, damageSource)) return false;
        DamageResistant damageresistant = this.get(DataComponents.DAMAGE_RESISTANT);
        return damageresistant == null || !damageresistant.isResistantTo(damageSource);
    }

    public boolean isValidRepairItem(ItemStack item) {
        Repairable repairable = this.get(DataComponents.REPAIRABLE);
        return repairable != null && repairable.isValidRepairItem(item);
    }

    public boolean canDestroyBlock(BlockState state, Level level, BlockPos pos, Player player) {
        return this.getItem().canDestroyBlock(this, state, level, pos, player);
    }
}
