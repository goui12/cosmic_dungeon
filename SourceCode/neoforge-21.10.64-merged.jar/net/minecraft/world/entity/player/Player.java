package net.minecraft.world.entity.player;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.math.IntMath;
import com.mojang.authlib.GameProfile;
import com.mojang.datafixers.util.Either;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Predicate;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.dialog.Dialog;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.NameAndId;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stat;
import net.minecraft.stats.Stats;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.util.Unit;
import net.minecraft.world.Container;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemStackWithSlot;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.ContainerUser;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityEquipment;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Parrot;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.boss.EnderDragonPart;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.warden.WardenSpawnTracker;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileDeflection;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.PlayerEnderChestContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemCooldowns;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.component.BlocksAttacks;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.BaseCommandBlock;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.CommandBlockEntity;
import net.minecraft.world.level.block.entity.ContainerOpenersCounter;
import net.minecraft.world.level.block.entity.JigsawBlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.StructureBlockEntity;
import net.minecraft.world.level.block.entity.TestBlockEntity;
import net.minecraft.world.level.block.entity.TestInstanceBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Team;

public abstract class Player extends Avatar implements ContainerUser, net.neoforged.neoforge.common.extensions.IPlayerExtension {
    public static final String PERSISTED_NBT_TAG = "PlayerPersisted";
    public static final int MAX_HEALTH = 20;
    public static final int SLEEP_DURATION = 100;
    public static final int WAKE_UP_DURATION = 10;
    public static final int ENDER_SLOT_OFFSET = 200;
    public static final int HELD_ITEM_SLOT = 499;
    public static final int CRAFTING_SLOT_OFFSET = 500;
    public static final float DEFAULT_BLOCK_INTERACTION_RANGE = 4.5F;
    public static final float DEFAULT_ENTITY_INTERACTION_RANGE = 3.0F;
    private static final int CURRENT_IMPULSE_CONTEXT_RESET_GRACE_TIME_TICKS = 40;
    private static final EntityDataAccessor<Float> DATA_PLAYER_ABSORPTION_ID = SynchedEntityData.defineId(Player.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_SCORE_ID = SynchedEntityData.defineId(Player.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<OptionalInt> DATA_SHOULDER_PARROT_LEFT = SynchedEntityData.defineId(
        Player.class, EntityDataSerializers.OPTIONAL_UNSIGNED_INT
    );
    private static final EntityDataAccessor<OptionalInt> DATA_SHOULDER_PARROT_RIGHT = SynchedEntityData.defineId(
        Player.class, EntityDataSerializers.OPTIONAL_UNSIGNED_INT
    );
    public static final int CLIENT_LOADED_TIMEOUT_TIME = 60;
    private static final short DEFAULT_SLEEP_TIMER = 0;
    private static final float DEFAULT_EXPERIENCE_PROGRESS = 0.0F;
    private static final int DEFAULT_EXPERIENCE_LEVEL = 0;
    private static final int DEFAULT_TOTAL_EXPERIENCE = 0;
    private static final int NO_ENCHANTMENT_SEED = 0;
    private static final int DEFAULT_SELECTED_SLOT = 0;
    private static final int DEFAULT_SCORE = 0;
    private static final boolean DEFAULT_IGNORE_FALL_DAMAGE_FROM_CURRENT_IMPULSE = false;
    private static final int DEFAULT_CURRENT_IMPULSE_CONTEXT_RESET_GRACE_TIME = 0;
    final Inventory inventory;
    protected PlayerEnderChestContainer enderChestInventory = new PlayerEnderChestContainer();
    public final InventoryMenu inventoryMenu;
    public AbstractContainerMenu containerMenu;
    protected FoodData foodData = new FoodData();
    protected int jumpTriggerTime;
    private boolean clientLoaded = false;
    protected int clientLoadedTimeoutTimer = 60;
    public int takeXpDelay;
    private int sleepCounter = 0;
    protected boolean wasUnderwater;
    private final Abilities abilities = new Abilities();
    public int experienceLevel = 0;
    public int totalExperience = 0;
    public float experienceProgress = 0.0F;
    protected int enchantmentSeed = 0;
    protected final float defaultFlySpeed = 0.02F;
    private int lastLevelUpTime;
    /**
     * The player's unique game profile
     */
    private final GameProfile gameProfile;
    private boolean reducedDebugInfo;
    private ItemStack lastItemInMainHand = ItemStack.EMPTY;
    private final ItemCooldowns cooldowns = this.createItemCooldowns();
    private Optional<GlobalPos> lastDeathLocation = Optional.empty();
    @Nullable
    public FishingHook fishing;
    protected float hurtDir;
    @Nullable
    public Vec3 currentImpulseImpactPos;
    @Nullable
    public Entity currentExplosionCause;
    private boolean ignoreFallDamageFromCurrentImpulse = false;
    private int currentImpulseContextResetGraceTime = 0;
    private final java.util.Collection<MutableComponent> prefixes = new java.util.LinkedList<>();
    private final java.util.Collection<MutableComponent> suffixes = new java.util.LinkedList<>();
    @Nullable
    private Pose forcedPose = null;

    public Player(Level level, GameProfile gameProfile) {
        super(EntityType.PLAYER, level);
        this.setUUID(gameProfile.id());
        this.gameProfile = gameProfile;
        this.inventory = new Inventory(this, this.equipment);
        this.inventoryMenu = new InventoryMenu(this.inventory, !level.isClientSide(), this);
        this.containerMenu = this.inventoryMenu;
    }

    @Override
    protected EntityEquipment createEquipment() {
        return new PlayerEquipment(this);
    }

    public boolean blockActionRestricted(Level level, BlockPos pos, GameType gameMode) {
        if (!gameMode.isBlockPlacingRestricted()) {
            return false;
        } else if (gameMode == GameType.SPECTATOR) {
            return true;
        } else if (this.mayBuild()) {
            return false;
        } else {
            ItemStack itemstack = this.getMainHandItem();
            return itemstack.isEmpty() || !itemstack.canBreakBlockInAdventureMode(new BlockInWorld(level, pos, false));
        }
    }

    public static AttributeSupplier.Builder createAttributes() {
        return LivingEntity.createLivingAttributes()
            .add(Attributes.ATTACK_DAMAGE, 1.0)
            .add(Attributes.MOVEMENT_SPEED, 0.1F)
            .add(Attributes.ATTACK_SPEED)
            .add(Attributes.LUCK)
            .add(Attributes.BLOCK_INTERACTION_RANGE, 4.5)
            .add(Attributes.ENTITY_INTERACTION_RANGE, 3.0)
            .add(Attributes.BLOCK_BREAK_SPEED)
            .add(Attributes.SUBMERGED_MINING_SPEED)
            .add(Attributes.SNEAKING_SPEED)
            .add(Attributes.MINING_EFFICIENCY)
            .add(Attributes.SWEEPING_DAMAGE_RATIO)
            .add(Attributes.WAYPOINT_TRANSMIT_RANGE, 6.0E7)
            .add(Attributes.WAYPOINT_RECEIVE_RANGE, 6.0E7)
            .add(net.neoforged.neoforge.common.NeoForgeMod.CREATIVE_FLIGHT);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_PLAYER_ABSORPTION_ID, 0.0F);
        builder.define(DATA_SCORE_ID, 0);
        builder.define(DATA_SHOULDER_PARROT_LEFT, OptionalInt.empty());
        builder.define(DATA_SHOULDER_PARROT_RIGHT, OptionalInt.empty());
    }

    @Override
    public void tick() {
        net.neoforged.neoforge.event.EventHooks.firePlayerTickPre(this);
        this.noPhysics = this.isSpectator();
        if (this.isSpectator() || this.isPassenger()) {
            this.setOnGround(false);
        }

        if (this.takeXpDelay > 0) {
            this.takeXpDelay--;
        }

        if (this.isSleeping()) {
            this.sleepCounter++;
            if (this.sleepCounter > 100) {
                this.sleepCounter = 100;
            }

            if (!this.level().isClientSide() && !net.neoforged.neoforge.event.EventHooks.canEntityContinueSleeping(this, this.level().isBrightOutside() ? BedSleepingProblem.NOT_POSSIBLE_NOW : null)) {
                this.stopSleepInBed(false, true);
            }
        } else if (this.sleepCounter > 0) {
            this.sleepCounter++;
            if (this.sleepCounter >= 110) {
                this.sleepCounter = 0;
            }
        }

        this.updateIsUnderwater();
        super.tick();
        int i = 29999999;
        double d0 = Mth.clamp(this.getX(), -2.9999999E7, 2.9999999E7);
        double d1 = Mth.clamp(this.getZ(), -2.9999999E7, 2.9999999E7);
        if (d0 != this.getX() || d1 != this.getZ()) {
            this.setPos(d0, this.getY(), d1);
        }

        this.attackStrengthTicker++;
        ItemStack itemstack = this.getMainHandItem();
        if (!ItemStack.matches(this.lastItemInMainHand, itemstack)) {
            if (!ItemStack.isSameItem(this.lastItemInMainHand, itemstack)) {
                this.resetAttackStrengthTicker();
            }

            this.lastItemInMainHand = itemstack.copy();
        }

        if (!this.isEyeInFluid(FluidTags.WATER) && this.isEquipped(Items.TURTLE_HELMET)) {
            this.turtleHelmetTick();
        }

        this.cooldowns.tick();
        this.updatePlayerPose();
        if (this.currentImpulseContextResetGraceTime > 0) {
            this.currentImpulseContextResetGraceTime--;
        }
        net.neoforged.neoforge.event.EventHooks.firePlayerTickPost(this);
    }

    @Override
    protected float getMaxHeadRotationRelativeToBody() {
        return this.isBlocking() ? 15.0F : super.getMaxHeadRotationRelativeToBody();
    }

    public boolean isSecondaryUseActive() {
        return this.isShiftKeyDown();
    }

    protected boolean wantsToStopRiding() {
        return this.isShiftKeyDown();
    }

    protected boolean isStayingOnGroundSurface() {
        return this.isShiftKeyDown();
    }

    protected boolean updateIsUnderwater() {
        this.wasUnderwater = this.getEyeInFluidType() != null && this.getEyeInFluidType().canSwim(this);
        return this.wasUnderwater;
    }

    @Override
    public void onAboveBubbleColumn(boolean downwards, BlockPos pos) {
        if (!this.getAbilities().flying) {
            super.onAboveBubbleColumn(downwards, pos);
        }
    }

    @Override
    public void onInsideBubbleColumn(boolean downwards) {
        if (!this.getAbilities().flying) {
            super.onInsideBubbleColumn(downwards);
        }
    }

    private void turtleHelmetTick() {
        this.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 200, 0, false, false, true));
    }

    private boolean isEquipped(Item item) {
        for (EquipmentSlot equipmentslot : EquipmentSlot.VALUES) {
            ItemStack itemstack = this.getItemBySlot(equipmentslot);
            Equippable equippable = itemstack.get(DataComponents.EQUIPPABLE);
            if (itemstack.is(item) && equippable != null && equippable.slot() == equipmentslot) {
                return true;
            }
        }

        return false;
    }

    protected ItemCooldowns createItemCooldowns() {
        return new ItemCooldowns();
    }

    protected void updatePlayerPose() {
        if (forcedPose != null) {
            this.setPose(forcedPose);
            return;
        }
        if (this.canPlayerFitWithinBlocksAndEntitiesWhen(Pose.SWIMMING)) {
            Pose pose = this.getDesiredPose();
            Pose pose1;
            if (this.isSpectator() || this.isPassenger() || this.canPlayerFitWithinBlocksAndEntitiesWhen(pose)) {
                pose1 = pose;
            } else if (this.canPlayerFitWithinBlocksAndEntitiesWhen(Pose.CROUCHING)) {
                pose1 = Pose.CROUCHING;
            } else {
                pose1 = Pose.SWIMMING;
            }

            this.setPose(pose1);
        }
    }

    private Pose getDesiredPose() {
        if (this.isSleeping()) {
            return Pose.SLEEPING;
        } else if (this.isSwimming()) {
            return Pose.SWIMMING;
        } else if (this.isFallFlying()) {
            return Pose.FALL_FLYING;
        } else if (this.isAutoSpinAttack()) {
            return Pose.SPIN_ATTACK;
        } else {
            return this.isShiftKeyDown() && !this.abilities.flying ? Pose.CROUCHING : Pose.STANDING;
        }
    }

    protected boolean canPlayerFitWithinBlocksAndEntitiesWhen(Pose pose) {
        return this.level().noCollision(this, this.getDimensions(pose).makeBoundingBox(this.position()).deflate(1.0E-7));
    }

    @Override
    protected SoundEvent getSwimSound() {
        return SoundEvents.PLAYER_SWIM;
    }

    @Override
    protected SoundEvent getSwimSplashSound() {
        return SoundEvents.PLAYER_SPLASH;
    }

    @Override
    protected SoundEvent getSwimHighSpeedSplashSound() {
        return SoundEvents.PLAYER_SPLASH_HIGH_SPEED;
    }

    @Override
    public int getDimensionChangingDelay() {
        return 10;
    }

    @Override
    public void playSound(SoundEvent sound, float volume, float pitch) {
        this.level().playSound(this, this.getX(), this.getY(), this.getZ(), sound, this.getSoundSource(), volume, pitch);
    }

    public void playNotifySound(SoundEvent sound, SoundSource source, float volume, float pitch) {
    }

    @Override
    public SoundSource getSoundSource() {
        return SoundSource.PLAYERS;
    }

    @Override
    protected int getFireImmuneTicks() {
        return 20;
    }

    /**
     * Handler for {@link World#setEntityState}
     */
    @Override
    public void handleEntityEvent(byte id) {
        if (id == 9) {
            this.completeUsingItem();
        } else if (id == 23) {
            this.setReducedDebugInfo(false);
        } else if (id == 22) {
            this.setReducedDebugInfo(true);
        } else {
            super.handleEntityEvent(id);
        }
    }

    public void closeContainer() {
        this.containerMenu = this.inventoryMenu;
    }

    protected void doCloseContainer() {
    }

    @Override
    public void rideTick() {
        if (!this.level().isClientSide() && this.wantsToStopRiding() && this.isPassenger()) {
            this.stopRiding();
            this.setShiftKeyDown(false);
        } else {
            super.rideTick();
        }
    }

    @Override
    public void aiStep() {
        if (this.jumpTriggerTime > 0) {
            this.jumpTriggerTime--;
        }

        this.tickRegeneration();
        this.inventory.tick();
        if (this.abilities.flying && !this.isPassenger()) {
            this.resetFallDistance();
        }

        super.aiStep();
        this.updateSwingTime();
        this.yHeadRot = this.getYRot();
        this.setSpeed((float)this.getAttributeValue(Attributes.MOVEMENT_SPEED));
        if (this.getHealth() > 0.0F && !this.isSpectator()) {
            AABB aabb;
            if (this.isPassenger() && !this.getVehicle().isRemoved()) {
                aabb = this.getBoundingBox().minmax(this.getVehicle().getBoundingBox()).inflate(1.0, 0.0, 1.0);
            } else {
                aabb = this.getBoundingBox().inflate(1.0, 0.5, 1.0);
            }

            List<Entity> list = this.level().getEntities(this, aabb);
            List<Entity> list1 = Lists.newArrayList();

            for (Entity entity : list) {
                if (entity.getType() == EntityType.EXPERIENCE_ORB) {
                    list1.add(entity);
                } else if (!entity.isRemoved()) {
                    this.touch(entity);
                }
            }

            if (!list1.isEmpty()) {
                this.touch(Util.getRandom(list1, this.random));
            }
        }

        this.handleShoulderEntities();
    }

    protected void tickRegeneration() {
    }

    public void handleShoulderEntities() {
    }

    protected void removeEntitiesOnShoulder() {
    }

    private void touch(Entity entity) {
        entity.playerTouch(this);
    }

    public int getScore() {
        return this.entityData.get(DATA_SCORE_ID);
    }

    /**
     * Set player's score
     */
    public void setScore(int score) {
        this.entityData.set(DATA_SCORE_ID, score);
    }

    /**
     * Add to player's score
     */
    public void increaseScore(int score) {
        int i = this.getScore();
        this.entityData.set(DATA_SCORE_ID, i + score);
    }

    public void startAutoSpinAttack(int ticks, float damage, ItemStack itemStack) {
        this.autoSpinAttackTicks = ticks;
        this.autoSpinAttackDmg = damage;
        this.autoSpinAttackItemStack = itemStack;
        if (!this.level().isClientSide()) {
            this.removeEntitiesOnShoulder();
            this.setLivingEntityFlag(4, true);
        }
    }

    @Nonnull
    @Override
    public ItemStack getWeaponItem() {
        return this.isAutoSpinAttack() && this.autoSpinAttackItemStack != null ? this.autoSpinAttackItemStack : super.getWeaponItem();
    }

    /**
     * Called when the mob's health reaches 0.
     */
    @Override
    public void die(DamageSource cause) {
        if (net.neoforged.neoforge.common.CommonHooks.onLivingDeath(this, cause)) return;
        super.die(cause);
        this.reapplyPosition();
        if (!this.isSpectator() && this.level() instanceof ServerLevel serverlevel) {
            this.dropAllDeathLoot(serverlevel, cause);
        }

        if (cause != null) {
            this.setDeltaMovement(
                -Mth.cos((this.getHurtDir() + this.getYRot()) * (float) (Math.PI / 180.0)) * 0.1F,
                0.1F,
                -Mth.sin((this.getHurtDir() + this.getYRot()) * (float) (Math.PI / 180.0)) * 0.1F
            );
        } else {
            this.setDeltaMovement(0.0, 0.1, 0.0);
        }

        this.awardStat(Stats.DEATHS);
        this.resetStat(Stats.CUSTOM.get(Stats.TIME_SINCE_DEATH));
        this.resetStat(Stats.CUSTOM.get(Stats.TIME_SINCE_REST));
        this.clearFire();
        this.setSharedFlagOnFire(false);
        this.setLastDeathLocation(Optional.of(GlobalPos.of(this.level().dimension(), this.blockPosition())));
    }

    @Override
    protected void dropEquipment(ServerLevel level) {
        super.dropEquipment(level);
        if (!level.getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY)) {
            this.destroyVanishingCursedItems();
            this.inventory.dropAll();
        }
    }

    protected void destroyVanishingCursedItems() {
        for (int i = 0; i < this.inventory.getContainerSize(); i++) {
            ItemStack itemstack = this.inventory.getItem(i);
            if (!itemstack.isEmpty() && EnchantmentHelper.has(itemstack, EnchantmentEffectComponents.PREVENT_EQUIPMENT_DROP)) {
                this.inventory.removeItemNoUpdate(i);
            }
        }
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return damageSource.type().effects().sound();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.PLAYER_DEATH;
    }

    public void handleCreativeModeItemDrop(ItemStack stack) {
    }

    /**
     * Drops an item into the world.
     */
    @Nullable
    public ItemEntity drop(ItemStack itemStack, boolean includeThrowerName) {
        return net.neoforged.neoforge.common.CommonHooks.onPlayerTossEvent(this, itemStack, false, includeThrowerName);
    }

    /**
     * @deprecated Neo: {@link #getDestroySpeed(BlockState, BlockPos)} instead
     */
    @Deprecated
    public float getDestroySpeed(BlockState state) {
        return getDestroySpeed(state, null);
    }

    public float getDestroySpeed(BlockState state, @Nullable BlockPos pos) {
        float f = this.inventory.getSelectedItem().getDestroySpeed(state);
        if (f > 1.0F) {
            f += (float)this.getAttributeValue(Attributes.MINING_EFFICIENCY);
        }

        if (MobEffectUtil.hasDigSpeed(this)) {
            f *= 1.0F + (MobEffectUtil.getDigSpeedAmplification(this) + 1) * 0.2F;
        }

        if (this.hasEffect(MobEffects.MINING_FATIGUE)) {
            float f1 = switch (this.getEffect(MobEffects.MINING_FATIGUE).getAmplifier()) {
                case 0 -> 0.3F;
                case 1 -> 0.09F;
                case 2 -> 0.0027F;
                default -> 8.1E-4F;
            };
            f *= f1;
        }

        f *= (float)this.getAttributeValue(Attributes.BLOCK_BREAK_SPEED);
        if (this.isEyeInFluid(FluidTags.WATER)) {
            f *= (float)this.getAttribute(Attributes.SUBMERGED_MINING_SPEED).getValue();
        }

        if (!this.onGround()) {
            f /= 5.0F;
        }

        f = net.neoforged.neoforge.event.EventHooks.getBreakSpeed(this, state, f, pos);
        return f;
    }

    @Deprecated // Neo: use position sensitive version below
    public boolean hasCorrectToolForDrops(BlockState state) {
        return !state.requiresCorrectToolForDrops() || this.inventory.getSelectedItem().isCorrectToolForDrops(state);
    }

    public boolean hasCorrectToolForDrops(BlockState p_state, Level level, BlockPos pos) {
        return net.neoforged.neoforge.event.EventHooks.doPlayerHarvestCheck(this, p_state, level, pos);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.setUUID(this.gameProfile.id());
        this.inventory.load(input.listOrEmpty("Inventory", ItemStackWithSlot.CODEC));
        this.inventory.setSelectedSlot(input.getIntOr("SelectedItemSlot", 0));
        this.sleepCounter = input.getShortOr("SleepTimer", (short)0);
        this.experienceProgress = input.getFloatOr("XpP", 0.0F);
        this.experienceLevel = input.getIntOr("XpLevel", 0);
        this.totalExperience = input.getIntOr("XpTotal", 0);
        this.enchantmentSeed = input.getIntOr("XpSeed", 0);
        if (this.enchantmentSeed == 0) {
            this.enchantmentSeed = this.random.nextInt();
        }

        this.setScore(input.getIntOr("Score", 0));
        this.foodData.readAdditionalSaveData(input);
        input.read("abilities", Abilities.Packed.CODEC).ifPresent(this.abilities::apply);
        this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(this.abilities.getWalkingSpeed());
        this.enderChestInventory.fromSlots(input.listOrEmpty("EnderItems", ItemStackWithSlot.CODEC));
        this.setLastDeathLocation(input.read("LastDeathLocation", GlobalPos.CODEC));
        this.currentImpulseImpactPos = input.read("current_explosion_impact_pos", Vec3.CODEC).orElse(null);
        this.ignoreFallDamageFromCurrentImpulse = input.getBooleanOr("ignore_fall_damage_from_current_explosion", false);
        this.currentImpulseContextResetGraceTime = input.getIntOr("current_impulse_context_reset_grace_time", 0);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        NbtUtils.addCurrentDataVersion(output);
        this.inventory.save(output.list("Inventory", ItemStackWithSlot.CODEC));
        output.putInt("SelectedItemSlot", this.inventory.getSelectedSlot());
        output.putShort("SleepTimer", (short)this.sleepCounter);
        output.putFloat("XpP", this.experienceProgress);
        output.putInt("XpLevel", this.experienceLevel);
        output.putInt("XpTotal", this.totalExperience);
        output.putInt("XpSeed", this.enchantmentSeed);
        output.putInt("Score", this.getScore());
        this.foodData.addAdditionalSaveData(output);
        output.store("abilities", Abilities.Packed.CODEC, this.abilities.pack());
        this.enderChestInventory.storeAsSlots(output.list("EnderItems", ItemStackWithSlot.CODEC));
        this.lastDeathLocation.ifPresent(p_421397_ -> output.store("LastDeathLocation", GlobalPos.CODEC, p_421397_));
        output.storeNullable("current_explosion_impact_pos", Vec3.CODEC, this.currentImpulseImpactPos);
        output.putBoolean("ignore_fall_damage_from_current_explosion", this.ignoreFallDamageFromCurrentImpulse);
        output.putInt("current_impulse_context_reset_grace_time", this.currentImpulseContextResetGraceTime);
    }

    @Override
    public boolean isInvulnerableTo(ServerLevel level, DamageSource damageSource) {
        if (super.isInvulnerableTo(level, damageSource)) {
            return true;
        } else if (damageSource.is(DamageTypeTags.IS_DROWNING)) {
            return !level.getGameRules().getBoolean(GameRules.RULE_DROWNING_DAMAGE);
        } else if (damageSource.is(DamageTypeTags.IS_FALL)) {
            return !level.getGameRules().getBoolean(GameRules.RULE_FALL_DAMAGE);
        } else if (damageSource.is(DamageTypeTags.IS_FIRE)) {
            return !level.getGameRules().getBoolean(GameRules.RULE_FIRE_DAMAGE);
        } else {
            return damageSource.is(DamageTypeTags.IS_FREEZING) ? !level.getGameRules().getBoolean(GameRules.RULE_FREEZE_DAMAGE) : false;
        }
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource damageSource, float amount) {
        if (this.isInvulnerableTo(level, damageSource)) {
            return false;
        } else if (this.abilities.invulnerable && !damageSource.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return false;
        } else {
            this.noActionTime = 0;
            if (this.isDeadOrDying()) {
                return false;
            } else {
                this.removeEntitiesOnShoulder();
                amount = Math.max(0.0F, damageSource.type().scaling().getScalingFunction().scaleDamage(damageSource, this, amount, this.level().getDifficulty()));
                if (false && damageSource.scalesWithDifficulty()) {
                    if (level.getDifficulty() == Difficulty.PEACEFUL) {
                        amount = 0.0F;
                    }

                    if (level.getDifficulty() == Difficulty.EASY) {
                        amount = Math.min(amount / 2.0F + 1.0F, amount);
                    }

                    if (level.getDifficulty() == Difficulty.HARD) {
                        amount = amount * 3.0F / 2.0F;
                    }
                }

                return amount == 0.0F ? false : super.hurtServer(level, damageSource, amount);
            }
        }
    }

    @Override
    protected void blockUsingItem(ServerLevel level, LivingEntity entity) {
        super.blockUsingItem(level, entity);
        ItemStack itemstack = this.getItemBlockingWith();
        BlocksAttacks blocksattacks = itemstack != null ? itemstack.get(DataComponents.BLOCKS_ATTACKS) : null;
        float f = entity.getSecondsToDisableBlocking();
        if (f > 0.0F && blocksattacks != null) {
            blocksattacks.disable(level, this, f, itemstack);
        }
    }

    @Override
    public boolean canBeSeenAsEnemy() {
        return !this.getAbilities().invulnerable && super.canBeSeenAsEnemy();
    }

    public boolean canHarmPlayer(Player other) {
        Team team = this.getTeam();
        Team team1 = other.getTeam();
        if (team == null) {
            return true;
        } else {
            return !team.isAlliedTo(team1) ? true : team.isAllowFriendlyFire();
        }
    }

    @Override
    protected void hurtArmor(DamageSource damageSource, float damage) {
        this.doHurtEquipment(damageSource, damage, new EquipmentSlot[]{EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD});
    }

    @Override
    protected void hurtHelmet(DamageSource damageSource, float damageAmount) {
        this.doHurtEquipment(damageSource, damageAmount, new EquipmentSlot[]{EquipmentSlot.HEAD});
    }

    @Override
    protected void actuallyHurt(ServerLevel level, DamageSource damageSource, float amount) {
        if (!this.isInvulnerableTo(level, damageSource)) {
            this.damageContainers.peek().setReduction(net.neoforged.neoforge.common.damagesource.DamageContainer.Reduction.ARMOR, this.damageContainers.peek().getNewDamage() - this.getDamageAfterArmorAbsorb(damageSource, this.damageContainers.peek().getNewDamage()));
            this.getDamageAfterMagicAbsorb(damageSource, this.damageContainers.peek().getNewDamage());
            float damage = net.neoforged.neoforge.common.CommonHooks.onLivingDamagePre(this, this.damageContainers.peek());
            this.damageContainers.peek().setReduction(net.neoforged.neoforge.common.damagesource.DamageContainer.Reduction.ABSORPTION, Math.min(this.getAbsorptionAmount(), damage));
            float absorbed = Math.min(damage, this.damageContainers.peek().getReduction(net.neoforged.neoforge.common.damagesource.DamageContainer.Reduction.ABSORPTION));
            this.setAbsorptionAmount(Math.max(0, this.getAbsorptionAmount() - absorbed));
            float f1 = this.damageContainers.peek().getNewDamage();
            float f = absorbed;
            if (f > 0.0F && f < 3.4028235E37F) {
                this.awardStat(Stats.DAMAGE_ABSORBED, Math.round(f * 10.0F));
            }

            if (f1 != 0.0F) {
                this.causeFoodExhaustion(damageSource.getFoodExhaustion());
                this.getCombatTracker().recordDamage(damageSource, f1);
                this.setHealth(this.getHealth() - f1);
                if (f1 < 3.4028235E37F) {
                    this.awardStat(Stats.DAMAGE_TAKEN, Math.round(f1 * 10.0F));
                }

                this.gameEvent(GameEvent.ENTITY_DAMAGE);
                this.onDamageTaken(this.damageContainers.peek());
            }
            net.neoforged.neoforge.common.CommonHooks.onLivingDamagePost(this, this.damageContainers.peek());
        }
    }

    public boolean isTextFilteringEnabled() {
        return false;
    }

    public void openTextEdit(SignBlockEntity signEntity, boolean isFrontText) {
    }

    public void openMinecartCommandBlock(BaseCommandBlock commandEntity) {
    }

    public void openCommandBlock(CommandBlockEntity commandBlockEntity) {
    }

    public void openStructureBlock(StructureBlockEntity structureEntity) {
    }

    public void openTestBlock(TestBlockEntity testBlockEntity) {
    }

    public void openTestInstanceBlock(TestInstanceBlockEntity testInstanceBlockEntity) {
    }

    public void openJigsawBlock(JigsawBlockEntity jigsawBlockEntity) {
    }

    public void openHorseInventory(AbstractHorse horse, Container inventory) {
    }

    public OptionalInt openMenu(@Nullable MenuProvider menu) {
        return OptionalInt.empty();
    }

    public void openDialog(Holder<Dialog> dialog) {
    }

    public void sendMerchantOffers(int containerId, MerchantOffers offers, int villagerLevel, int villagerXp, boolean showProgress, boolean canRestock) {
    }

    public void openItemGui(ItemStack stack, InteractionHand hand) {
    }

    public InteractionResult interactOn(Entity entityToInteractOn, InteractionHand hand) {
        if (this.isSpectator()) {
            if (entityToInteractOn instanceof MenuProvider) {
                this.openMenu((MenuProvider)entityToInteractOn);
            }

            return InteractionResult.PASS;
        } else {
            InteractionResult cancelResult = net.neoforged.neoforge.common.CommonHooks.onInteractEntity(this, entityToInteractOn, hand);
            if (cancelResult != null) return cancelResult;
            ItemStack itemstack = this.getItemInHand(hand);
            ItemStack itemstack1 = itemstack.copy();
            InteractionResult interactionresult = entityToInteractOn.interact(this, hand);
            if (interactionresult.consumesAction()) {
                if (this.hasInfiniteMaterials() && itemstack == this.getItemInHand(hand) && itemstack.getCount() < itemstack1.getCount()) {
                    itemstack.setCount(itemstack1.getCount());
                }

                if (!this.abilities.instabuild && itemstack.isEmpty()) {
                    net.neoforged.neoforge.event.EventHooks.onPlayerDestroyItem(this, itemstack1, hand);
                }
                return interactionresult;
            } else {
                if (!itemstack.isEmpty() && entityToInteractOn instanceof LivingEntity) {
                    if (this.hasInfiniteMaterials()) {
                        itemstack = itemstack1;
                    }

                    InteractionResult interactionresult1 = itemstack.interactLivingEntity(this, (LivingEntity)entityToInteractOn, hand);
                    if (interactionresult1.consumesAction()) {
                        this.level().gameEvent(GameEvent.ENTITY_INTERACT, entityToInteractOn.position(), GameEvent.Context.of(this));
                        if (itemstack.isEmpty() && !this.hasInfiniteMaterials()) {
                            net.neoforged.neoforge.event.EventHooks.onPlayerDestroyItem(this, itemstack1, hand);
                            this.setItemInHand(hand, ItemStack.EMPTY);
                        }

                        return interactionresult1;
                    }
                }

                return InteractionResult.PASS;
            }
        }
    }

    @Override
    public void removeVehicle() {
        super.removeVehicle();
        this.boardingCooldown = 0;
    }

    @Override
    protected boolean isImmobile() {
        return super.isImmobile() || this.isSleeping();
    }

    @Override
    public boolean isAffectedByFluids() {
        return !this.abilities.flying;
    }

    @Override
    // Forge: Don't update this method to use IForgeEntity#getStepHeight() - https://github.com/MinecraftForge/MinecraftForge/issues/8922
    protected Vec3 maybeBackOffFromEdge(Vec3 vec, MoverType mover) {
        float f = this.maxUpStep();
        if (!this.abilities.flying
            && !(vec.y > 0.0)
            && (mover == MoverType.SELF || mover == MoverType.PLAYER)
            && this.isStayingOnGroundSurface()
            && this.isAboveGround(f)) {
            double d0 = vec.x;
            double d1 = vec.z;
            double d2 = 0.05;
            double d3 = Math.signum(d0) * 0.05;

            double d4;
            for (d4 = Math.signum(d1) * 0.05; d0 != 0.0 && this.canFallAtLeast(d0, 0.0, f); d0 -= d3) {
                if (Math.abs(d0) <= 0.05) {
                    d0 = 0.0;
                    break;
                }
            }

            while (d1 != 0.0 && this.canFallAtLeast(0.0, d1, f)) {
                if (Math.abs(d1) <= 0.05) {
                    d1 = 0.0;
                    break;
                }

                d1 -= d4;
            }

            while (d0 != 0.0 && d1 != 0.0 && this.canFallAtLeast(d0, d1, f)) {
                if (Math.abs(d0) <= 0.05) {
                    d0 = 0.0;
                } else {
                    d0 -= d3;
                }

                if (Math.abs(d1) <= 0.05) {
                    d1 = 0.0;
                } else {
                    d1 -= d4;
                }
            }

            return new Vec3(d0, vec.y, d1);
        } else {
            return vec;
        }
    }

    // Forge: Don't update this method to use IForgeEntity#getStepHeight() - https://github.com/MinecraftForge/MinecraftForge/issues/9376
    private boolean isAboveGround(float maxUpStep) {
        return this.onGround() || this.fallDistance < maxUpStep && !this.canFallAtLeast(0.0, 0.0, maxUpStep - this.fallDistance);
    }

    private boolean canFallAtLeast(double x, double z, double distance) {
        AABB aabb = this.getBoundingBox();
        return this.level()
            .noCollision(
                this,
                new AABB(
                    aabb.minX + 1.0E-7 + x,
                    aabb.minY - distance - 1.0E-7,
                    aabb.minZ + 1.0E-7 + z,
                    aabb.maxX - 1.0E-7 + x,
                    aabb.minY,
                    aabb.maxZ - 1.0E-7 + z
                )
            );
    }

    /**
     * Attacks for the player the targeted entity with the currently equipped item.  The equipped item has hitEntity called on it. Args: targetEntity
     */
    public void attack(Entity target) {
        if (!net.neoforged.neoforge.common.CommonHooks.onPlayerAttackTarget(this, target)) return;
        if (target.isAttackable()) {
            if (!target.skipAttackInteraction(this)) {
                float f = this.isAutoSpinAttack() ? this.autoSpinAttackDmg : (float)this.getAttributeValue(Attributes.ATTACK_DAMAGE);
                ItemStack itemstack = this.getWeaponItem();
                DamageSource damagesource = Optional.ofNullable(itemstack.getItem().getDamageSource(this)).orElse(this.damageSources().playerAttack(this));
                float f1 = this.getEnchantedDamage(target, f, damagesource) - f;
                float f2 = this.getAttackStrengthScale(0.5F);
                f *= 0.2F + f2 * f2 * 0.8F;
                f1 *= f2;
                if (target.getType().is(EntityTypeTags.REDIRECTABLE_PROJECTILE)
                    && target instanceof Projectile projectile
                    && projectile.deflect(ProjectileDeflection.AIM_DEFLECT, this, EntityReference.of(this), true)) {
                    this.level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.PLAYER_ATTACK_NODAMAGE, this.getSoundSource());
                } else {
                    if (f > 0.0F || f1 > 0.0F) {
                        boolean flag3 = f2 > 0.9F;
                        boolean flag;
                        if (this.isSprinting() && flag3) {
                            this.level()
                                .playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.PLAYER_ATTACK_KNOCKBACK, this.getSoundSource(), 1.0F, 1.0F);
                            flag = true;
                        } else {
                            flag = false;
                        }

                        f += itemstack.getItem().getAttackDamageBonus(target, f, damagesource);
                        boolean flag1 = flag3
                            && this.fallDistance > 0.0
                            && !this.onGround()
                            && !this.onClimbable()
                            && !this.isInWater()
                            && !this.isMobilityRestricted()
                            && !this.isPassenger()
                            && target instanceof LivingEntity
                            && !this.isSprinting();
                        // Neo: Fire the critical hit event and override the critical hit status and damage multiplier based on the event.
                        // The boolean local above (flag1) is the vanilla critical hit result.
                        var critEvent = net.neoforged.neoforge.common.CommonHooks.fireCriticalHit(this, target, flag1, flag1 ? 1.5F : 1.0F);
                        flag1 = critEvent.isCriticalHit();
                        if (flag1) {
                            f *= critEvent.getDamageMultiplier();
                        }

                        float f3 = f + f1;
                        boolean flag2 = false;
                        // Neo: Replace !flag1 (!isCriticalHit) with the logic from the CriticalHitEvent.
                        boolean critBlocksSweep = critEvent.isCriticalHit() && critEvent.disableSweep();
                        if (flag3 && !critBlocksSweep && !flag && this.onGround()) {
                            double d0 = this.getKnownMovement().horizontalDistanceSqr();
                            double d1 = this.getSpeed() * 2.5;
                            // Neo: Make sweep attacks check SWORD_SWEEP instead of ItemTags.SWORDS.
                            if (d0 < Mth.square(d1) && this.getItemInHand(InteractionHand.MAIN_HAND).canPerformAction(net.neoforged.neoforge.common.ItemAbilities.SWORD_SWEEP)) {
                                flag2 = true;
                            }
                        }

                        // Neo: Fire the SweepAttackEvent and overwrite the value of flag2 (the local controlling if a sweep will occur).
                        var sweepEvent = net.neoforged.neoforge.common.CommonHooks.fireSweepAttack(this, target, flag2);
                        flag2 = sweepEvent.isSweeping();

                        float f5 = 0.0F;
                        if (target instanceof LivingEntity livingentity) {
                            f5 = livingentity.getHealth();
                        }

                        Vec3 vec3 = target.getDeltaMovement();
                        boolean flag4 = target.hurtOrSimulate(damagesource, f3);
                        if (flag4) {
                            float f4 = this.getKnockback(target, damagesource) + (flag ? 1.0F : 0.0F);
                            if (f4 > 0.0F) {
                                if (target instanceof LivingEntity livingentity1) {
                                    livingentity1.knockback(
                                        f4 * 0.5F, Mth.sin(this.getYRot() * (float) (Math.PI / 180.0)), -Mth.cos(this.getYRot() * (float) (Math.PI / 180.0))
                                    );
                                } else {
                                    target.push(
                                        -Mth.sin(this.getYRot() * (float) (Math.PI / 180.0)) * f4 * 0.5F,
                                        0.1,
                                        Mth.cos(this.getYRot() * (float) (Math.PI / 180.0)) * f4 * 0.5F
                                    );
                                }

                                this.setDeltaMovement(this.getDeltaMovement().multiply(0.6, 1.0, 0.6));
                                this.setSprinting(false);
                            }

                            if (flag2) {
                                float f6 = 1.0F + (float)this.getAttributeValue(Attributes.SWEEPING_DAMAGE_RATIO) * f;

                                for (LivingEntity livingentity2 : this.level()
                                    .getEntitiesOfClass(LivingEntity.class, itemstack.getSweepHitBox(this, target))) { // Neo: Patch in item extension for custom sweep hit box
                                    double entityReachSq = Mth.square(this.entityInteractionRange()); // Use entity reach instead of constant 9.0. Vanilla uses bottom center-to-center checks here, so don't update this to use canReach, since it uses closest-corner checks.
                                    if (livingentity2 != this
                                        && livingentity2 != target
                                        && !this.isAlliedTo(livingentity2)
                                        && !(livingentity2 instanceof ArmorStand armorstand && armorstand.isMarker())
                                        && this.distanceToSqr(livingentity2) < entityReachSq) {
                                        float f8 = this.getEnchantedDamage(livingentity2, f6, damagesource) * f2;
                                        if (this.level() instanceof ServerLevel serverlevel && livingentity2.hurtServer(serverlevel, damagesource, f8)) {
                                            livingentity2.knockback(
                                                0.4F, Mth.sin(this.getYRot() * (float) (Math.PI / 180.0)), -Mth.cos(this.getYRot() * (float) (Math.PI / 180.0))
                                            );
                                            EnchantmentHelper.doPostAttackEffects(serverlevel, livingentity2, damagesource);
                                        }
                                    }
                                }

                                this.level()
                                    .playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, this.getSoundSource(), 1.0F, 1.0F);
                                this.sweepAttack();
                            }

                            if (target instanceof ServerPlayer && target.hurtMarked) {
                                ((ServerPlayer)target).connection.send(new ClientboundSetEntityMotionPacket(target));
                                target.hurtMarked = false;
                                target.setDeltaMovement(vec3);
                            }

                            if (flag1) {
                                this.level()
                                    .playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.PLAYER_ATTACK_CRIT, this.getSoundSource(), 1.0F, 1.0F);
                                this.crit(target);
                            }

                            if (!flag1 && !flag2) {
                                if (flag3) {
                                    this.level()
                                        .playSound(
                                            null, this.getX(), this.getY(), this.getZ(), SoundEvents.PLAYER_ATTACK_STRONG, this.getSoundSource(), 1.0F, 1.0F
                                        );
                                } else {
                                    this.level()
                                        .playSound(
                                            null, this.getX(), this.getY(), this.getZ(), SoundEvents.PLAYER_ATTACK_WEAK, this.getSoundSource(), 1.0F, 1.0F
                                        );
                                }
                            }

                            if (f1 > 0.0F) {
                                this.magicCrit(target);
                            }

                            this.setLastHurtMob(target);
                            Entity entity = target;
                            if (target instanceof net.neoforged.neoforge.entity.PartEntity) {
                                entity = ((net.neoforged.neoforge.entity.PartEntity<?>) target).getParent();
                            }

                            boolean flag5 = false;
                            ItemStack copy = itemstack.copy();
                            if (this.level() instanceof ServerLevel serverlevel1) {
                                if (entity instanceof LivingEntity livingentity3) {
                                    flag5 = itemstack.hurtEnemy(livingentity3, this);
                                }

                                EnchantmentHelper.doPostAttackEffects(serverlevel1, target, damagesource);
                            }

                            if (!this.level().isClientSide() && !itemstack.isEmpty() && entity instanceof LivingEntity) {
                                if (flag5) {
                                    itemstack.postHurtEnemy((LivingEntity)entity, this);
                                }

                                if (itemstack.isEmpty()) {
                                    net.neoforged.neoforge.event.EventHooks.onPlayerDestroyItem(this, copy, itemstack == this.getMainHandItem() ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND);
                                    if (itemstack == this.getMainHandItem()) {
                                        this.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                                    } else {
                                        this.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
                                    }
                                }
                            }

                            if (target instanceof LivingEntity) {
                                float f7 = f5 - ((LivingEntity)target).getHealth();
                                this.awardStat(Stats.DAMAGE_DEALT, Math.round(f7 * 10.0F));
                                if (this.level() instanceof ServerLevel && f7 > 2.0F) {
                                    int i = (int)(f7 * 0.5);
                                    ((ServerLevel)this.level())
                                        .sendParticles(
                                            ParticleTypes.DAMAGE_INDICATOR, target.getX(), target.getY(0.5), target.getZ(), i, 0.1, 0.0, 0.1, 0.2
                                        );
                                }
                            }

                            this.causeFoodExhaustion(0.1F);
                        } else {
                            this.level()
                                .playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.PLAYER_ATTACK_NODAMAGE, this.getSoundSource(), 1.0F, 1.0F);
                        }
                    }
                }
                this.resetAttackStrengthTicker(); // FORGE: Moved from beginning of attack() so that getAttackStrengthScale() returns an accurate value during all attack events
            }
        }
    }

    protected float getEnchantedDamage(Entity entity, float damage, DamageSource damageSource) {
        return damage;
    }

    @Override
    protected void doAutoAttackOnTouch(LivingEntity target) {
        this.attack(target);
    }

    /**
     * Called when the entity is dealt a critical hit.
     */
    public void crit(Entity entityHit) {
    }

    public void magicCrit(Entity entityHit) {
    }

    public void sweepAttack() {
        double d0 = -Mth.sin(this.getYRot() * (float) (Math.PI / 180.0));
        double d1 = Mth.cos(this.getYRot() * (float) (Math.PI / 180.0));
        if (this.level() instanceof ServerLevel) {
            ((ServerLevel)this.level()).sendParticles(ParticleTypes.SWEEP_ATTACK, this.getX() + d0, this.getY(0.5), this.getZ() + d1, 0, d0, 0.0, d1, 0.0);
        }
    }

    public void respawn() {
    }

    @Override
    public void remove(Entity.RemovalReason reason) {
        super.remove(reason);
        this.inventoryMenu.removed(this);
        if (this.hasContainerOpen()) {
            this.doCloseContainer();
        }
    }

    @Override
    public boolean isClientAuthoritative() {
        return true;
    }

    @Override
    protected boolean isLocalClientAuthoritative() {
        return this.isLocalPlayer();
    }

    public boolean isLocalPlayer() {
        return false;
    }

    @Override
    public boolean canSimulateMovement() {
        return !this.level().isClientSide() || this.isLocalPlayer();
    }

    @Override
    public boolean isEffectiveAi() {
        return !this.level().isClientSide() || this.isLocalPlayer();
    }

    public GameProfile getGameProfile() {
        return this.gameProfile;
    }

    public NameAndId nameAndId() {
        return new NameAndId(this.gameProfile);
    }

    public Inventory getInventory() {
        return this.inventory;
    }

    public Abilities getAbilities() {
        return this.abilities;
    }

    @Override
    public boolean hasInfiniteMaterials() {
        return this.abilities.instabuild;
    }

    public boolean preventsBlockDrops() {
        return this.abilities.instabuild;
    }

    public void updateTutorialInventoryAction(ItemStack carried, ItemStack clicked, ClickAction action) {
    }

    public boolean hasContainerOpen() {
        return this.containerMenu != this.inventoryMenu;
    }

    public boolean canDropItems() {
        return true;
    }

    public Either<Player.BedSleepingProblem, Unit> startSleepInBed(BlockPos bedPos) {
        this.startSleeping(bedPos);
        this.sleepCounter = 0;
        return Either.right(Unit.INSTANCE);
    }

    public void stopSleepInBed(boolean wakeImmediately, boolean updateLevelForSleepingPlayers) {
        net.neoforged.neoforge.event.EventHooks.onPlayerWakeup(this, wakeImmediately, updateLevelForSleepingPlayers);
        super.stopSleeping();
        if (this.level() instanceof ServerLevel && updateLevelForSleepingPlayers) {
            ((ServerLevel)this.level()).updateSleepingPlayerList();
        }

        this.sleepCounter = wakeImmediately ? 0 : 100;
    }

    @Override
    public void stopSleeping() {
        this.stopSleepInBed(true, true);
    }

    public boolean isSleepingLongEnough() {
        return this.isSleeping() && this.sleepCounter >= 100;
    }

    public int getSleepTimer() {
        return this.sleepCounter;
    }

    public void displayClientMessage(Component chatComponent, boolean actionBar) {
    }

    public void awardStat(ResourceLocation statKey) {
        this.awardStat(Stats.CUSTOM.get(statKey));
    }

    public void awardStat(ResourceLocation stat, int increment) {
        this.awardStat(Stats.CUSTOM.get(stat), increment);
    }

    /**
     * Add a stat once
     */
    public void awardStat(Stat<?> stat) {
        this.awardStat(stat, 1);
    }

    /**
     * Adds a value to a statistic field.
     */
    public void awardStat(Stat<?> stat, int increment) {
    }

    public void resetStat(Stat<?> stat) {
    }

    public int awardRecipes(Collection<RecipeHolder<?>> recipes) {
        return 0;
    }

    public void triggerRecipeCrafted(RecipeHolder<?> recipe, List<ItemStack> items) {
    }

    public void awardRecipesByKey(List<ResourceKey<Recipe<?>>> recipes) {
    }

    public int resetRecipes(Collection<RecipeHolder<?>> recipes) {
        return 0;
    }

    @Override
    public void travel(Vec3 travelVector) {
        if (this.isPassenger()) {
            super.travel(travelVector);
        } else {
            if (this.isSwimming()) {
                double d0 = this.getLookAngle().y;
                double d1 = d0 < -0.2 ? 0.085 : 0.06;
                if (d0 <= 0.0 || this.jumping || !this.level().getFluidState(BlockPos.containing(this.getX(), this.getY() + 1.0 - 0.1, this.getZ())).isEmpty()) {
                    Vec3 vec3 = this.getDeltaMovement();
                    this.setDeltaMovement(vec3.add(0.0, (d0 - vec3.y) * d1, 0.0));
                }
            }

            if (this.getAbilities().flying) {
                double d2 = this.getDeltaMovement().y;
                super.travel(travelVector);
                this.setDeltaMovement(this.getDeltaMovement().with(Direction.Axis.Y, d2 * 0.6));
            } else {
                super.travel(travelVector);
            }
        }
    }

    @Override
    protected boolean canGlide() {
        return !this.abilities.flying && super.canGlide();
    }

    @Override
    public void updateSwimming() {
        if (this.abilities.flying) {
            this.setSwimming(false);
        } else {
            super.updateSwimming();
        }
    }

    protected boolean freeAt(BlockPos pos) {
        return !this.level().getBlockState(pos).isSuffocating(this.level(), pos);
    }

    @Override
    public float getSpeed() {
        return (float)this.getAttributeValue(Attributes.MOVEMENT_SPEED);
    }

    @Override
    public boolean causeFallDamage(double fallDistance, float damageMultiplier, DamageSource damageSource) {
        if (this.mayFly()) {
            net.neoforged.neoforge.event.EventHooks.onPlayerFall(this, damageMultiplier, damageMultiplier);
            return false;
        } else {
            if (fallDistance >= 2.0) {
                this.awardStat(Stats.FALL_ONE_CM, (int)Math.round(fallDistance * 100.0));
            }

            boolean flag = this.currentImpulseImpactPos != null && this.ignoreFallDamageFromCurrentImpulse;
            double d0;
            if (flag) {
                d0 = Math.min(fallDistance, this.currentImpulseImpactPos.y - this.getY());
                boolean flag1 = d0 <= 0.0;
                if (flag1) {
                    this.resetCurrentImpulseContext();
                } else {
                    this.tryResetCurrentImpulseContext();
                }
            } else {
                d0 = fallDistance;
            }

            if (d0 > 0.0 && super.causeFallDamage(d0, damageMultiplier, damageSource)) {
                this.resetCurrentImpulseContext();
                return true;
            } else {
                this.propagateFallToPassengers(fallDistance, damageMultiplier, damageSource);
                return false;
            }
        }
    }

    public boolean tryToStartFallFlying() {
        if (!this.isFallFlying() && this.canGlide() && !this.isInWater()) {
            this.startFallFlying();
            return true;
        } else {
            return false;
        }
    }

    public void startFallFlying() {
        this.setSharedFlag(7, true);
    }

    @Override
    protected void doWaterSplashEffect() {
        if (!this.isSpectator()) {
            super.doWaterSplashEffect();
        }
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        if (this.isInWater()) {
            this.waterSwimSound();
            this.playMuffledStepSound(state, pos);
        } else {
            BlockPos blockpos = this.getPrimaryStepSoundBlockPos(pos);
            if (!pos.equals(blockpos)) {
                BlockState blockstate = this.level().getBlockState(blockpos);
                if (blockstate.is(BlockTags.COMBINATION_STEP_SOUND_BLOCKS)) {
                    this.playCombinationStepSounds(blockstate, state, blockpos, pos);
                } else {
                    super.playStepSound(blockpos, blockstate);
                }
            } else {
                super.playStepSound(pos, state);
            }
        }
    }

    @Override
    public LivingEntity.Fallsounds getFallSounds() {
        return new LivingEntity.Fallsounds(SoundEvents.PLAYER_SMALL_FALL, SoundEvents.PLAYER_BIG_FALL);
    }

    @Override
    public boolean killedEntity(ServerLevel level, LivingEntity entity, DamageSource damageSource) {
        this.awardStat(Stats.ENTITY_KILLED.get(entity.getType()));
        return true;
    }

    @Override
    public void makeStuckInBlock(BlockState state, Vec3 motionMultiplier) {
        if (!this.abilities.flying) {
            super.makeStuckInBlock(state, motionMultiplier);
        }

        this.tryResetCurrentImpulseContext();
    }

    public void giveExperiencePoints(int xpPoints) {
        net.neoforged.neoforge.event.entity.player.PlayerXpEvent.XpChange event = new net.neoforged.neoforge.event.entity.player.PlayerXpEvent.XpChange(this, xpPoints);
        if (net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(event).isCanceled()) return;
        xpPoints = event.getAmount();

        this.increaseScore(xpPoints);
        this.experienceProgress = this.experienceProgress + (float)xpPoints / this.getXpNeededForNextLevel();
        this.totalExperience = Mth.clamp(this.totalExperience + xpPoints, 0, Integer.MAX_VALUE);

        while (this.experienceProgress < 0.0F) {
            float f = this.experienceProgress * this.getXpNeededForNextLevel();
            if (this.experienceLevel > 0) {
                this.giveExperienceLevels(-1);
                this.experienceProgress = 1.0F + f / this.getXpNeededForNextLevel();
            } else {
                this.giveExperienceLevels(-1);
                this.experienceProgress = 0.0F;
            }
        }

        while (this.experienceProgress >= 1.0F) {
            this.experienceProgress = (this.experienceProgress - 1.0F) * this.getXpNeededForNextLevel();
            this.giveExperienceLevels(1);
            this.experienceProgress = this.experienceProgress / this.getXpNeededForNextLevel();
        }
    }

    public int getEnchantmentSeed() {
        return this.enchantmentSeed;
    }

    public void onEnchantmentPerformed(ItemStack enchantedItem, int levelCost) {
        giveExperienceLevels(-levelCost);
        if (this.experienceLevel < 0) {
            this.experienceLevel = 0;
            this.experienceProgress = 0.0F;
            this.totalExperience = 0;
        }

        this.enchantmentSeed = this.random.nextInt();
    }

    /**
     * Add experience levels to this player.
     */
    public void giveExperienceLevels(int levels) {
        net.neoforged.neoforge.event.entity.player.PlayerXpEvent.LevelChange event = new net.neoforged.neoforge.event.entity.player.PlayerXpEvent.LevelChange(this, levels);
        if (net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(event).isCanceled()) return;
        levels = event.getLevels();

        this.experienceLevel = IntMath.saturatedAdd(this.experienceLevel, levels);
        if (this.experienceLevel < 0) {
            this.experienceLevel = 0;
            this.experienceProgress = 0.0F;
            this.totalExperience = 0;
        }

        if (levels > 0 && this.experienceLevel % 5 == 0 && this.lastLevelUpTime < this.tickCount - 100.0F) {
            float f = this.experienceLevel > 30 ? 1.0F : this.experienceLevel / 30.0F;
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.PLAYER_LEVELUP, this.getSoundSource(), f * 0.75F, 1.0F);
            this.lastLevelUpTime = this.tickCount;
        }
    }

    public int getXpNeededForNextLevel() {
        if (this.experienceLevel >= 30) {
            return 112 + (this.experienceLevel - 30) * 9;
        } else {
            return this.experienceLevel >= 15 ? 37 + (this.experienceLevel - 15) * 5 : 7 + this.experienceLevel * 2;
        }
    }

    /**
     * Increases exhaustion level by the supplied amount.
     */
    public void causeFoodExhaustion(float exhaustion) {
        if (!this.abilities.invulnerable) {
            if (!this.level().isClientSide()) {
                this.foodData.addExhaustion(exhaustion);
            }
        }
    }

    public Optional<WardenSpawnTracker> getWardenSpawnTracker() {
        return Optional.empty();
    }

    public FoodData getFoodData() {
        return this.foodData;
    }

    public boolean canEat(boolean canAlwaysEat) {
        return this.abilities.invulnerable || canAlwaysEat || this.foodData.needsFood();
    }

    public boolean isHurt() {
        return this.getHealth() > 0.0F && this.getHealth() < this.getMaxHealth();
    }

    public boolean mayBuild() {
        return this.abilities.mayBuild;
    }

    /**
     * Returns whether this player can modify the block at a certain location with the given stack.
     * <p>
     * The position being queried is {@code pos.offset(facing.getOpposite())}.
     *
     * @return Whether this player may modify the queried location in the current world
     * @see ItemStack#canPlaceOn(Block)
     * @see ItemStack#canEditBlocks()
     * @see PlayerCapabilities#allowEdit
     */
    public boolean mayUseItemAt(BlockPos pos, Direction facing, ItemStack stack) {
        if (this.abilities.mayBuild) {
            return true;
        } else {
            BlockPos blockpos = pos.relative(facing.getOpposite());
            BlockInWorld blockinworld = new BlockInWorld(this.level(), blockpos, false);
            return stack.canPlaceOnBlockInAdventureMode(blockinworld);
        }
    }

    @Override
    protected int getBaseExperienceReward(ServerLevel level) {
        return !level.getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY) && !this.isSpectator() ? Math.min(this.experienceLevel * 7, 100) : 0;
    }

    @Override
    protected boolean isAlwaysExperienceDropper() {
        return true;
    }

    @Override
    public boolean shouldShowName() {
        return true;
    }

    @Override
    protected Entity.MovementEmission getMovementEmission() {
        return this.abilities.flying || this.onGround() && this.isDiscrete() ? Entity.MovementEmission.NONE : Entity.MovementEmission.ALL;
    }

    public void onUpdateAbilities() {
    }

    @Override
    public Component getName() {
        return Component.literal(this.gameProfile.name());
    }

    @Override
    public String getPlainTextName() {
        return this.gameProfile.name();
    }

    public PlayerEnderChestContainer getEnderChestInventory() {
        return this.enderChestInventory;
    }

    @Override
    protected boolean doesEmitEquipEvent(EquipmentSlot slot) {
        return slot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR;
    }

    public boolean addItem(ItemStack stack) {
        return this.inventory.add(stack);
    }

    @Nullable
    public abstract GameType gameMode();

    @Override
    public boolean isSpectator() {
        return this.gameMode() == GameType.SPECTATOR;
    }

    @Override
    public boolean canBeHitByProjectile() {
        return !this.isSpectator() && super.canBeHitByProjectile();
    }

    @Override
    public boolean isSwimming() {
        return !this.abilities.flying && !this.isSpectator() && super.isSwimming();
    }

    public boolean isCreative() {
        return this.gameMode() == GameType.CREATIVE;
    }

    @Override
    public boolean isPushedByFluid() {
        return !this.abilities.flying;
    }

    @Override
    public Component getDisplayName() {
        if (this.displayname == null) this.displayname = net.neoforged.neoforge.event.EventHooks.getPlayerDisplayName(this, this.getName());
        MutableComponent mutablecomponent = Component.literal("");
        mutablecomponent = prefixes.stream().reduce(mutablecomponent, MutableComponent::append);
        mutablecomponent = mutablecomponent.append(PlayerTeam.formatNameForTeam(this.getTeam(), this.displayname));
        mutablecomponent = suffixes.stream().reduce(mutablecomponent, MutableComponent::append);
        return this.decorateDisplayNameComponent(mutablecomponent);
    }

    private MutableComponent decorateDisplayNameComponent(MutableComponent displayName) {
        String s = this.getGameProfile().name();
        return displayName.withStyle(
            p_427131_ -> p_427131_.withClickEvent(new ClickEvent.SuggestCommand("/tell " + s + " ")).withHoverEvent(this.createHoverEvent()).withInsertion(s)
        );
    }

    @Override
    public String getScoreboardName() {
        return this.getGameProfile().name();
    }

    @Override
    protected void internalSetAbsorptionAmount(float absorptionAmount) {
        this.getEntityData().set(DATA_PLAYER_ABSORPTION_ID, absorptionAmount);
    }

    @Override
    public float getAbsorptionAmount() {
        return this.getEntityData().get(DATA_PLAYER_ABSORPTION_ID);
    }

    @Override
    public SlotAccess getSlot(int slot) {
        if (slot == 499) {
            return new SlotAccess() {
                @Override
                public ItemStack get() {
                    return Player.this.containerMenu.getCarried();
                }

                @Override
                public boolean set(ItemStack p_332675_) {
                    Player.this.containerMenu.setCarried(p_332675_);
                    return true;
                }
            };
        } else {
            final int i = slot - 500;
            if (i >= 0 && i < 4) {
                return new SlotAccess() {
                    @Override
                    public ItemStack get() {
                        return Player.this.inventoryMenu.getCraftSlots().getItem(i);
                    }

                    @Override
                    public boolean set(ItemStack p_332810_) {
                        Player.this.inventoryMenu.getCraftSlots().setItem(i, p_332810_);
                        Player.this.inventoryMenu.slotsChanged(Player.this.inventory);
                        return true;
                    }
                };
            } else if (slot >= 0 && slot < this.inventory.getNonEquipmentItems().size()) {
                return SlotAccess.forContainer(this.inventory, slot);
            } else {
                int j = slot - 200;
                return j >= 0 && j < this.enderChestInventory.getContainerSize()
                    ? SlotAccess.forContainer(this.enderChestInventory, j)
                    : super.getSlot(slot);
            }
        }
    }

    public boolean isReducedDebugInfo() {
        return this.reducedDebugInfo;
    }

    public void setReducedDebugInfo(boolean reducedDebugInfo) {
        this.reducedDebugInfo = reducedDebugInfo;
    }

    @Override
    public void setRemainingFireTicks(int ticks) {
        super.setRemainingFireTicks(this.abilities.invulnerable ? Math.min(ticks, 1) : ticks);
    }

    protected static Optional<Parrot.Variant> extractParrotVariant(CompoundTag tag) {
        if (!tag.isEmpty()) {
            EntityType<?> entitytype = tag.read("id", EntityType.CODEC).orElse(null);
            if (entitytype == EntityType.PARROT) {
                return tag.read("Variant", Parrot.Variant.LEGACY_CODEC);
            }
        }

        return Optional.empty();
    }

    protected static OptionalInt convertParrotVariant(Optional<Parrot.Variant> variant) {
        return variant.<OptionalInt>map(p_445345_ -> OptionalInt.of(p_445345_.getId())).orElse(OptionalInt.empty());
    }

    private static Optional<Parrot.Variant> convertParrotVariant(OptionalInt id) {
        return id.isPresent() ? Optional.of(Parrot.Variant.byId(id.getAsInt())) : Optional.empty();
    }

    public void setShoulderParrotLeft(Optional<Parrot.Variant> variant) {
        this.entityData.set(DATA_SHOULDER_PARROT_LEFT, convertParrotVariant(variant));
    }

    public Optional<Parrot.Variant> getShoulderParrotLeft() {
        return convertParrotVariant(this.entityData.get(DATA_SHOULDER_PARROT_LEFT));
    }

    public void setShoulderParrotRight(Optional<Parrot.Variant> variant) {
        this.entityData.set(DATA_SHOULDER_PARROT_RIGHT, convertParrotVariant(variant));
    }

    public Optional<Parrot.Variant> getShoulderParrotRight() {
        return convertParrotVariant(this.entityData.get(DATA_SHOULDER_PARROT_RIGHT));
    }

    public float getCurrentItemAttackStrengthDelay() {
        return (float)(1.0 / this.getAttributeValue(Attributes.ATTACK_SPEED) * 20.0);
    }

    /**
     * Returns the percentage of attack power available based on the cooldown (zero to one).
     */
    public float getAttackStrengthScale(float adjustTicks) {
        return Mth.clamp((this.attackStrengthTicker + adjustTicks) / this.getCurrentItemAttackStrengthDelay(), 0.0F, 1.0F);
    }

    public void resetAttackStrengthTicker() {
        this.attackStrengthTicker = 0;
    }

    public ItemCooldowns getCooldowns() {
        return this.cooldowns;
    }

    @Override
    protected float getBlockSpeedFactor() {
        return !this.abilities.flying && !this.isFallFlying() ? super.getBlockSpeedFactor() : 1.0F;
    }

    @Override
    public float getLuck() {
        return (float)this.getAttributeValue(Attributes.LUCK);
    }

    public boolean canUseGameMasterBlocks() {
        return this.abilities.instabuild && this.getPermissionLevel() >= 2;
    }

    public int getPermissionLevel() {
        return 0;
    }

    public boolean hasPermissions(int permissionLevel) {
        return this.getPermissionLevel() >= permissionLevel;
    }

    @Override
    public ImmutableList<Pose> getDismountPoses() {
        return ImmutableList.of(Pose.STANDING, Pose.CROUCHING, Pose.SWIMMING);
    }

    @Override
    public ItemStack getProjectile(ItemStack shootable) {
        if (!(shootable.getItem() instanceof ProjectileWeaponItem)) {
            return ItemStack.EMPTY;
        } else {
            Predicate<ItemStack> predicate = ((ProjectileWeaponItem)shootable.getItem()).getSupportedHeldProjectiles(shootable);
            ItemStack itemstack = ProjectileWeaponItem.getHeldProjectile(this, predicate);
            if (!itemstack.isEmpty()) {
                return net.neoforged.neoforge.common.CommonHooks.getProjectile(this, shootable, itemstack);
            } else {
                predicate = ((ProjectileWeaponItem)shootable.getItem()).getAllSupportedProjectiles(shootable);

                for (int i = 0; i < this.inventory.getContainerSize(); i++) {
                    ItemStack itemstack1 = this.inventory.getItem(i);
                    if (predicate.test(itemstack1)) {
                        return net.neoforged.neoforge.common.CommonHooks.getProjectile(this, shootable, itemstack1);
                    }
                }

                return net.neoforged.neoforge.common.CommonHooks.getProjectile(this, shootable, this.abilities.instabuild ? ((ProjectileWeaponItem)shootable.getItem()).getDefaultCreativeAmmo(this, shootable) : ItemStack.EMPTY);
            }
        }
    }

    @Override
    public Vec3 getRopeHoldPosition(float partialTicks) {
        double d0 = 0.22 * (this.getMainArm() == HumanoidArm.RIGHT ? -1.0 : 1.0);
        float f = Mth.lerp(partialTicks * 0.5F, this.getXRot(), this.xRotO) * (float) (Math.PI / 180.0);
        float f1 = Mth.lerp(partialTicks, this.yBodyRotO, this.yBodyRot) * (float) (Math.PI / 180.0);
        if (this.isFallFlying() || this.isAutoSpinAttack()) {
            Vec3 vec31 = this.getViewVector(partialTicks);
            Vec3 vec3 = this.getDeltaMovement();
            double d6 = vec3.horizontalDistanceSqr();
            double d3 = vec31.horizontalDistanceSqr();
            float f2;
            if (d6 > 0.0 && d3 > 0.0) {
                double d4 = (vec3.x * vec31.x + vec3.z * vec31.z) / Math.sqrt(d6 * d3);
                double d5 = vec3.x * vec31.z - vec3.z * vec31.x;
                f2 = (float)(Math.signum(d5) * Math.acos(d4));
            } else {
                f2 = 0.0F;
            }

            return this.getPosition(partialTicks).add(new Vec3(d0, -0.11, 0.85).zRot(-f2).xRot(-f).yRot(-f1));
        } else if (this.isVisuallySwimming()) {
            return this.getPosition(partialTicks).add(new Vec3(d0, 0.2, -0.15).xRot(-f).yRot(-f1));
        } else {
            double d1 = this.getBoundingBox().getYsize() - 1.0;
            double d2 = this.isCrouching() ? -0.2 : 0.07;
            return this.getPosition(partialTicks).add(new Vec3(d0, d1, d2).yRot(-f1));
        }
    }

    @Override
    public boolean isAlwaysTicking() {
        return true;
    }

    public boolean isScoping() {
        return this.isUsingItem() && this.getUseItem().canPerformAction(net.neoforged.neoforge.common.ItemAbilities.SPYGLASS_SCOPE);
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    public Optional<GlobalPos> getLastDeathLocation() {
        return this.lastDeathLocation;
    }

    public void setLastDeathLocation(Optional<GlobalPos> lastDeathLocation) {
        this.lastDeathLocation = lastDeathLocation;
    }

    @Override
    public float getHurtDir() {
        return this.hurtDir;
    }

    @Override
    public void animateHurt(float yaw) {
        super.animateHurt(yaw);
        this.hurtDir = yaw;
    }

    public boolean isMobilityRestricted() {
        return this.hasEffect(MobEffects.BLINDNESS);
    }

    @Override
    public boolean canSprint() {
        return true;
    }

    @Override
    protected float getFlyingSpeed() {
        if (this.abilities.flying && !this.isPassenger()) {
            return this.isSprinting() ? this.abilities.getFlyingSpeed() * 2.0F : this.abilities.getFlyingSpeed();
        } else {
            return this.isSprinting() ? 0.025999999F : 0.02F;
        }
    }

    public boolean hasClientLoaded() {
        return this.clientLoaded || this.clientLoadedTimeoutTimer <= 0;
    }

    public void tickClientLoadTimeout() {
        if (!this.clientLoaded) {
            this.clientLoadedTimeoutTimer--;
        }
    }

    public void setClientLoaded(boolean clientLoaded) {
        this.clientLoaded = clientLoaded;
        if (!this.clientLoaded) {
            this.clientLoadedTimeoutTimer = 60;
        }
    }

    @Override
    public boolean hasContainerOpen(ContainerOpenersCounter openCounter, BlockPos pos) {
        return openCounter.isOwnContainer(this);
    }

    @Override
    public double getContainerInteractionRange() {
        return this.blockInteractionRange();
    }

    public double blockInteractionRange() {
        return this.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE);
    }

    public double entityInteractionRange() {
        return this.getAttributeValue(Attributes.ENTITY_INTERACTION_RANGE);
    }

    public boolean canInteractWithEntity(Entity entity, double distance) {
        return entity.isRemoved() ? false : this.canInteractWithEntity(entity.getBoundingBox(), distance);
    }

    public boolean canInteractWithEntity(AABB boundingBox, double distance) {
        double d0 = this.entityInteractionRange() + distance;
        return boundingBox.distanceToSqr(this.getEyePosition()) < d0 * d0;
    }

    public boolean canInteractWithBlock(BlockPos pos, double distance) {
        double d0 = this.blockInteractionRange() + distance;
        return new AABB(pos).distanceToSqr(this.getEyePosition()) < d0 * d0;
    }

    public void setIgnoreFallDamageFromCurrentImpulse(boolean ignoreFallDamageFromCurrentImpulse) {
        this.ignoreFallDamageFromCurrentImpulse = ignoreFallDamageFromCurrentImpulse;
        if (ignoreFallDamageFromCurrentImpulse) {
            this.currentImpulseContextResetGraceTime = 40;
        } else {
            this.currentImpulseContextResetGraceTime = 0;
        }
    }

    public boolean isIgnoringFallDamageFromCurrentImpulse() {
        return this.ignoreFallDamageFromCurrentImpulse;
    }

    public void tryResetCurrentImpulseContext() {
        if (this.currentImpulseContextResetGraceTime == 0) {
            this.resetCurrentImpulseContext();
        }
    }

    public void resetCurrentImpulseContext() {
        this.currentImpulseContextResetGraceTime = 0;
        this.currentExplosionCause = null;
        this.currentImpulseImpactPos = null;
        this.ignoreFallDamageFromCurrentImpulse = false;
    }

    public boolean shouldRotateWithMinecart() {
        return false;
    }

    @Override
    public boolean onClimbable() {
        return this.abilities.flying ? false : super.onClimbable();
    }

    public String debugInfo() {
        return MoreObjects.toStringHelper(this)
            .add("name", this.getPlainTextName())
            .add("id", this.getId())
            .add("pos", this.position())
            .add("mode", this.gameMode())
            .add("permission", this.getPermissionLevel())
            .toString();
    }

    public static enum BedSleepingProblem {
        NOT_POSSIBLE_HERE,
        NOT_POSSIBLE_NOW(Component.translatable("block.minecraft.bed.no_sleep")),
        TOO_FAR_AWAY(Component.translatable("block.minecraft.bed.too_far_away")),
        OBSTRUCTED(Component.translatable("block.minecraft.bed.obstructed")),
        OTHER_PROBLEM,
        NOT_SAFE(Component.translatable("block.minecraft.bed.not_safe"));

        @Nullable
        private final Component message;

        private BedSleepingProblem() {
            this.message = null;
        }

        private BedSleepingProblem(Component message) {
            this.message = message;
        }

        @Nullable
        public Component getMessage() {
            return this.message;
        }
    }

    // Neo: Getters for the Player's name prefixes and suffixes
    public Collection<MutableComponent> getPrefixes() {
         return this.prefixes;
    }

    public Collection<MutableComponent> getSuffixes() {
         return this.suffixes;
    }

    private Component displayname = null;

    /**
     * Neo: Force the displayed name to refresh, by firing {@link net.neoforged.neoforge.event.entity.player.PlayerEvent.NameFormat}, using the real player name as event parameter.
     */
    public void refreshDisplayName() {
        this.displayname = net.neoforged.neoforge.event.EventHooks.getPlayerDisplayName(this, this.getName());
    }

    /**
     * Neo: Force a pose for the player. If set, the vanilla pose determination and clearance check is skipped. Make sure the pose is clear yourself (e.g. in PlayerTick).
     * This has to be set just once, do not set it every tick.
     * Make sure to clear (null) the pose if not required anymore and only use if necessary.
     */
    public void setForcedPose(@Nullable Pose pose) {
        this.forcedPose = pose;
    }

    /**
     * Neo:
     * @return The forced pose if set, null otherwise
     */
    @Nullable
    public Pose getForcedPose() {
        return this.forcedPose;
    }
}
