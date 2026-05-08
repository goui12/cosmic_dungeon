package net.minecraft.world.entity;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JavaOps;
import it.unimi.dsi.fastutil.doubles.DoubleDoubleImmutablePair;
import it.unimi.dsi.fastutil.objects.Reference2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.BlockUtil;
import net.minecraft.Util;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundAnimatePacket;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.network.protocol.game.ClientboundRemoveMobEffectPacket;
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket;
import net.minecraft.network.protocol.game.ClientboundTakeItemEntityPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateMobEffectPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.waypoints.ServerWaypointManager;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.damagesource.CombatTracker;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;
import net.minecraft.world.entity.animal.FlyingAnimal;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.BlocksAttacks;
import net.minecraft.world.item.component.DeathProtection;
import net.minecraft.world.item.component.Weapon;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.effects.EnchantmentLocationBasedEffect;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HoneyBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.PowderSnowBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.waypoints.Waypoint;
import net.minecraft.world.waypoints.WaypointTransmitter;
import org.jetbrains.annotations.Contract;
import org.slf4j.Logger;

public abstract class LivingEntity extends Entity implements Attackable, WaypointTransmitter, net.neoforged.neoforge.common.extensions.ILivingEntityExtension {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String TAG_ACTIVE_EFFECTS = "active_effects";
    public static final String TAG_ATTRIBUTES = "attributes";
    public static final String TAG_SLEEPING_POS = "sleeping_pos";
    public static final String TAG_EQUIPMENT = "equipment";
    public static final String TAG_BRAIN = "Brain";
    public static final String TAG_FALL_FLYING = "FallFlying";
    public static final String TAG_HURT_TIME = "HurtTime";
    public static final String TAG_DEATH_TIME = "DeathTime";
    public static final String TAG_HURT_BY_TIMESTAMP = "HurtByTimestamp";
    public static final String TAG_HEALTH = "Health";
    private static final ResourceLocation SPEED_MODIFIER_POWDER_SNOW_ID = ResourceLocation.withDefaultNamespace("powder_snow");
    private static final ResourceLocation SPRINTING_MODIFIER_ID = ResourceLocation.withDefaultNamespace("sprinting");
    private static final AttributeModifier SPEED_MODIFIER_SPRINTING = new AttributeModifier(
        SPRINTING_MODIFIER_ID, 0.3F, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
    );
    public static final int EQUIPMENT_SLOT_OFFSET = 98;
    public static final int ARMOR_SLOT_OFFSET = 100;
    public static final int BODY_ARMOR_OFFSET = 105;
    public static final int SADDLE_OFFSET = 106;
    public static final int SWING_DURATION = 6;
    public static final int PLAYER_HURT_EXPERIENCE_TIME = 100;
    private static final int DAMAGE_SOURCE_TIMEOUT = 40;
    public static final double MIN_MOVEMENT_DISTANCE = 0.003;
    public static final double DEFAULT_BASE_GRAVITY = 0.08;
    public static final int DEATH_DURATION = 20;
    protected static final float INPUT_FRICTION = 0.98F;
    private static final int TICKS_PER_ELYTRA_FREE_FALL_EVENT = 10;
    private static final int FREE_FALL_EVENTS_PER_ELYTRA_BREAK = 2;
    public static final float BASE_JUMP_POWER = 0.42F;
    private static final double MAX_LINE_OF_SIGHT_TEST_RANGE = 128.0;
    protected static final int LIVING_ENTITY_FLAG_IS_USING = 1;
    protected static final int LIVING_ENTITY_FLAG_OFF_HAND = 2;
    protected static final int LIVING_ENTITY_FLAG_SPIN_ATTACK = 4;
    protected static final EntityDataAccessor<Byte> DATA_LIVING_ENTITY_FLAGS = SynchedEntityData.defineId(LivingEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Float> DATA_HEALTH_ID = SynchedEntityData.defineId(LivingEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<List<ParticleOptions>> DATA_EFFECT_PARTICLES = SynchedEntityData.defineId(
        LivingEntity.class, EntityDataSerializers.PARTICLES
    );
    private static final EntityDataAccessor<Boolean> DATA_EFFECT_AMBIENCE_ID = SynchedEntityData.defineId(LivingEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_ARROW_COUNT_ID = SynchedEntityData.defineId(LivingEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_STINGER_COUNT_ID = SynchedEntityData.defineId(LivingEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Optional<BlockPos>> SLEEPING_POS_ID = SynchedEntityData.defineId(
        LivingEntity.class, EntityDataSerializers.OPTIONAL_BLOCK_POS
    );
    private static final int PARTICLE_FREQUENCY_WHEN_INVISIBLE = 15;
    protected static final EntityDimensions SLEEPING_DIMENSIONS = EntityDimensions.fixed(0.2F, 0.2F).withEyeHeight(0.2F);
    public static final float EXTRA_RENDER_CULLING_SIZE_WITH_BIG_HAT = 0.5F;
    public static final float DEFAULT_BABY_SCALE = 0.5F;
    // Neo: Support IItemExtension#isGazeDisguise
    public static final java.util.function.BiPredicate<LivingEntity, @org.jetbrains.annotations.Nullable LivingEntity> PLAYER_NOT_WEARING_DISGUISE_ITEM_FOR_TARGET = (p_401735_, target) -> {
        if (p_401735_ instanceof Player player) {
            ItemStack itemstack = player.getItemBySlot(EquipmentSlot.HEAD);
            return !itemstack.isGazeDisguise(player, target);
        } else {
            return true;
        }
    };
    /** @deprecated Neo: use {@link #PLAYER_NOT_WEARING_DISGUISE_ITEM_FOR_TARGET} with target info instead */
    @Deprecated
    public static final Predicate<LivingEntity> PLAYER_NOT_WEARING_DISGUISE_ITEM = p_401735_ -> {
        return PLAYER_NOT_WEARING_DISGUISE_ITEM_FOR_TARGET.test(p_401735_, null);
    };
    private static final Dynamic<?> EMPTY_BRAIN = new Dynamic<>(JavaOps.INSTANCE, Map.of("memories", Map.of()));
    private final AttributeMap attributes;
    private final CombatTracker combatTracker = new CombatTracker(this);
    private final Map<Holder<MobEffect>, MobEffectInstance> activeEffects = Maps.newHashMap();
    private final Map<EquipmentSlot, ItemStack> lastEquipmentItems = Util.makeEnumMap(EquipmentSlot.class, p_396671_ -> ItemStack.EMPTY);
    public boolean swinging;
    private boolean discardFriction = false;
    public InteractionHand swingingArm;
    public int swingTime;
    public int removeArrowTime;
    public int removeStingerTime;
    public int hurtTime;
    public int hurtDuration;
    public int deathTime;
    public float oAttackAnim;
    public float attackAnim;
    protected int attackStrengthTicker;
    public final WalkAnimationState walkAnimation = new WalkAnimationState();
    public final int invulnerableDuration = 20;
    public float yBodyRot;
    public float yBodyRotO;
    public float yHeadRot;
    public float yHeadRotO;
    public final ElytraAnimationState elytraAnimationState = new ElytraAnimationState(this);
    @Nullable
    protected EntityReference<Player> lastHurtByPlayer;
    protected int lastHurtByPlayerMemoryTime;
    protected boolean dead;
    protected int noActionTime;
    /**
     * Damage taken in the last hit. Mobs are resistant to damage less than this for a short time after taking damage.
     */
    protected float lastHurt;
    protected boolean jumping;
    public float xxa;
    public float yya;
    public float zza;
    protected InterpolationHandler interpolation = new InterpolationHandler(this);
    protected double lerpYHeadRot;
    protected int lerpHeadSteps;
    private boolean effectsDirty = true;
    @Nullable
    private EntityReference<LivingEntity> lastHurtByMob;
    private int lastHurtByMobTimestamp;
    @Nullable
    private LivingEntity lastHurtMob;
    /**
     * Holds the value of ticksExisted when setLastAttacker was last called.
     */
    private int lastHurtMobTimestamp;
    private float speed;
    private int noJumpDelay;
    private float absorptionAmount;
    protected ItemStack useItem = ItemStack.EMPTY;
    protected int useItemRemaining;
    protected int fallFlyTicks;
    private BlockPos lastPos;
    private Optional<BlockPos> lastClimbablePos = Optional.empty();
    @Nullable
    private DamageSource lastDamageSource;
    private long lastDamageStamp;
    protected int autoSpinAttackTicks;
    protected float autoSpinAttackDmg;
    @Nullable
    protected ItemStack autoSpinAttackItemStack;
    private float swimAmount;
    private float swimAmountO;
    protected Brain<?> brain;
    private boolean skipDropExperience;
    private final EnumMap<EquipmentSlot, Reference2ObjectMap<Enchantment, Set<EnchantmentLocationBasedEffect>>> activeLocationDependentEnchantments = new EnumMap<>(
        EquipmentSlot.class
    );
    protected final EntityEquipment equipment;
    private Waypoint.Icon locatorBarIcon = new Waypoint.Icon();
    /**
     * This field stores information about damage dealt to this entity.
     * a new {@link net.neoforged.neoforge.common.damagesource.DamageContainer} is instantiated
     * via {@link #hurt(DamageSource, float)} after invulnerability checks, and is removed from
     * the stack before the method's return.
     **/
    @Nullable
    protected java.util.Stack<net.neoforged.neoforge.common.damagesource.DamageContainer> damageContainers = new java.util.Stack<>();

    protected LivingEntity(EntityType<? extends LivingEntity> entityType, Level level) {
        super(entityType, level);
        this.attributes = new AttributeMap(DefaultAttributes.getSupplier(entityType));
        this.setHealth(this.getMaxHealth());
        this.equipment = this.createEquipment();
        this.blocksBuilding = true;
        this.reapplyPosition();
        this.setYRot((float)(Math.random() * (float) (Math.PI * 2)));
        this.yHeadRot = this.getYRot();
        this.brain = this.makeBrain(EMPTY_BRAIN);
    }

    @Nullable
    @Override
    public LivingEntity asLivingEntity() {
        return this;
    }

    @Contract(
        pure = true
    )
    protected EntityEquipment createEquipment() {
        return new EntityEquipment();
    }

    public Brain<?> getBrain() {
        return this.brain;
    }

    protected Brain.Provider<?> brainProvider() {
        return Brain.provider(ImmutableList.of(), ImmutableList.of());
    }

    protected Brain<?> makeBrain(Dynamic<?> dynamic) {
        return this.brainProvider().makeBrain(dynamic);
    }

    @Override
    public void kill(ServerLevel level) {
        this.hurtServer(level, this.damageSources().genericKill(), Float.MAX_VALUE);
    }

    public boolean canAttackType(EntityType<?> entityType) {
        return true;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_LIVING_ENTITY_FLAGS, (byte)0);
        builder.define(DATA_EFFECT_PARTICLES, List.of());
        builder.define(DATA_EFFECT_AMBIENCE_ID, false);
        builder.define(DATA_ARROW_COUNT_ID, 0);
        builder.define(DATA_STINGER_COUNT_ID, 0);
        builder.define(DATA_HEALTH_ID, 1.0F);
        builder.define(SLEEPING_POS_ID, Optional.empty());
    }

    public static AttributeSupplier.Builder createLivingAttributes() {
        return AttributeSupplier.builder()
            .add(Attributes.MAX_HEALTH)
            .add(Attributes.KNOCKBACK_RESISTANCE)
            .add(Attributes.MOVEMENT_SPEED)
            .add(Attributes.ARMOR)
            .add(Attributes.ARMOR_TOUGHNESS)
            .add(Attributes.MAX_ABSORPTION)
            .add(Attributes.STEP_HEIGHT)
            .add(Attributes.SCALE)
            .add(Attributes.GRAVITY)
            .add(Attributes.SAFE_FALL_DISTANCE)
            .add(Attributes.FALL_DAMAGE_MULTIPLIER)
            .add(Attributes.JUMP_STRENGTH)
            .add(Attributes.OXYGEN_BONUS)
            .add(Attributes.BURNING_TIME)
            .add(Attributes.EXPLOSION_KNOCKBACK_RESISTANCE)
            .add(Attributes.WATER_MOVEMENT_EFFICIENCY)
            .add(Attributes.MOVEMENT_EFFICIENCY)
            .add(Attributes.ATTACK_KNOCKBACK)
            .add(Attributes.CAMERA_DISTANCE)
            .add(Attributes.WAYPOINT_TRANSMIT_RANGE)
            .add(net.neoforged.neoforge.common.NeoForgeMod.SWIM_SPEED)
            .add(net.neoforged.neoforge.common.NeoForgeMod.NAMETAG_DISTANCE);
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, BlockState state, BlockPos pos) {
        if (!this.isInWater()) {
            this.updateInWaterStateAndDoWaterCurrentPushing(false);
        }

        if (this.level() instanceof ServerLevel serverlevel && onGround && this.fallDistance > 0.0) {
            this.onChangedBlock(serverlevel, pos);
            double d6 = Math.max(0, Mth.floor(this.calculateFallPower(this.fallDistance)));
            if (d6 > 0.0 && !state.isAir()) {
                double d0 = this.getX();
                double d1 = this.getY();
                double d2 = this.getZ();
                BlockPos blockpos = this.blockPosition();
                if (pos.getX() != blockpos.getX() || pos.getZ() != blockpos.getZ()) {
                    double d3 = d0 - pos.getX() - 0.5;
                    double d4 = d2 - pos.getZ() - 0.5;
                    double d5 = Math.max(Math.abs(d3), Math.abs(d4));
                    d0 = pos.getX() + 0.5 + d3 / d5 * 0.5;
                    d2 = pos.getZ() + 0.5 + d4 / d5 * 0.5;
                }

                double d7 = Math.min(0.2F + d6 / 15.0, 2.5);
                int i = (int)(150.0 * d7);
                if (!state.addLandingEffects(serverlevel, pos, state, this, i))
                serverlevel.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state, pos), d0, d1, d2, i, 0.0, 0.0, 0.0, 0.15F);
            }
        }

        super.checkFallDamage(y, onGround, state, pos);
        if (onGround) {
            this.lastClimbablePos = Optional.empty();
        }
    }

    @Deprecated //FORGE: Use canDrownInFluidType instead
    public boolean canBreatheUnderwater() {
        return this.getType().is(EntityTypeTags.CAN_BREATHE_UNDER_WATER);
    }

    public float getSwimAmount(float partialTicks) {
        return Mth.lerp(partialTicks, this.swimAmountO, this.swimAmount);
    }

    public boolean hasLandedInLiquid() {
        return this.getDeltaMovement().y() < 1.0E-5F && this.isInLiquid();
    }

    @Override
    public void baseTick() {
        this.oAttackAnim = this.attackAnim;
        if (this.firstTick) {
            this.getSleepingPos().ifPresent(this::setPosToBed);
        }

        if (this.level() instanceof ServerLevel serverlevel) {
            EnchantmentHelper.tickEffects(serverlevel, this);
        }

        super.baseTick();
        ProfilerFiller profilerfiller = Profiler.get();
        profilerfiller.push("livingEntityBaseTick");
        if (this.isAlive() && this.level() instanceof ServerLevel serverlevel1) {
            boolean flag = this instanceof Player;
            if (this.isInWall()) {
                this.hurtServer(serverlevel1, this.damageSources().inWall(), 1.0F);
            } else if (flag && !serverlevel1.getWorldBorder().isWithinBounds(this.getBoundingBox())) {
                double d0 = serverlevel1.getWorldBorder().getDistanceToBorder(this) + serverlevel1.getWorldBorder().getSafeZone();
                if (d0 < 0.0) {
                    double d1 = serverlevel1.getWorldBorder().getDamagePerBlock();
                    if (d1 > 0.0) {
                        this.hurtServer(serverlevel1, this.damageSources().outOfBorder(), Math.max(1, Mth.floor(-d0 * d1)));
                    }
                }
            }

            int airSupply = this.getAirSupply();
            net.neoforged.neoforge.common.CommonHooks.onLivingBreathe(this, airSupply - decreaseAirSupply(airSupply), increaseAirSupply(airSupply) - airSupply);
            if (false) // Forge: Handled in ForgeHooks#onLivingBreathe(LivingEntity, int, int)
            if (this.isEyeInFluid(FluidTags.WATER)
                && !serverlevel1.getBlockState(BlockPos.containing(this.getX(), this.getEyeY(), this.getZ())).is(Blocks.BUBBLE_COLUMN)) {
                boolean flag1 = !this.canBreatheUnderwater()
                    && !MobEffectUtil.hasWaterBreathing(this)
                    && (!flag || !((Player)this).getAbilities().invulnerable);
                if (flag1) {
                    this.setAirSupply(this.decreaseAirSupply(this.getAirSupply()));
                    if (this.shouldTakeDrowningDamage()) {
                        this.setAirSupply(0);
                        serverlevel1.broadcastEntityEvent(this, (byte)67);
                        this.hurtServer(serverlevel1, this.damageSources().drown(), 2.0F);
                    }
                } else if (this.getAirSupply() < this.getMaxAirSupply()) {
                    this.setAirSupply(this.increaseAirSupply(this.getAirSupply()));
                }

                if (this.isPassenger() && this.getVehicle() != null && this.getVehicle().dismountsUnderwater()) {
                    this.stopRiding();
                }
            } else if (this.getAirSupply() < this.getMaxAirSupply()) {
                this.setAirSupply(this.increaseAirSupply(this.getAirSupply()));
            }

            BlockPos blockpos = this.blockPosition();
            if (!Objects.equal(this.lastPos, blockpos)) {
                this.lastPos = blockpos;
                this.onChangedBlock(serverlevel1, blockpos);
            }
        }

        if (this.hurtTime > 0) {
            this.hurtTime--;
        }

        if (this.invulnerableTime > 0 && !(this instanceof ServerPlayer)) {
            this.invulnerableTime--;
        }

        if (this.isDeadOrDying() && this.level().shouldTickDeath(this)) {
            this.tickDeath();
        }

        if (this.lastHurtByPlayerMemoryTime > 0) {
            this.lastHurtByPlayerMemoryTime--;
        } else {
            this.lastHurtByPlayer = null;
        }

        if (this.lastHurtMob != null && !this.lastHurtMob.isAlive()) {
            this.lastHurtMob = null;
        }

        LivingEntity livingentity = this.getLastHurtByMob();
        if (livingentity != null) {
            if (!livingentity.isAlive()) {
                this.setLastHurtByMob(null);
            } else if (this.tickCount - this.lastHurtByMobTimestamp > 100) {
                this.setLastHurtByMob(null);
            }
        }

        this.tickEffects();
        this.yHeadRotO = this.yHeadRot;
        this.yBodyRotO = this.yBodyRot;
        this.yRotO = this.getYRot();
        this.xRotO = this.getXRot();
        profilerfiller.pop();
    }

    protected boolean shouldTakeDrowningDamage() {
        return this.getAirSupply() <= -20;
    }

    @Override
    protected float getBlockSpeedFactor() {
        return Mth.lerp((float)this.getAttributeValue(Attributes.MOVEMENT_EFFICIENCY), super.getBlockSpeedFactor(), 1.0F);
    }

    public float getLuck() {
        return 0.0F;
    }

    protected void removeFrost() {
        AttributeInstance attributeinstance = this.getAttribute(Attributes.MOVEMENT_SPEED);
        if (attributeinstance != null) {
            if (attributeinstance.getModifier(SPEED_MODIFIER_POWDER_SNOW_ID) != null) {
                attributeinstance.removeModifier(SPEED_MODIFIER_POWDER_SNOW_ID);
            }
        }
    }

    protected void tryAddFrost() {
        if (!this.getBlockStateOnLegacy().isAir()) {
            int i = this.getTicksFrozen();
            if (i > 0) {
                AttributeInstance attributeinstance = this.getAttribute(Attributes.MOVEMENT_SPEED);
                if (attributeinstance == null) {
                    return;
                }

                float f = -0.05F * this.getPercentFrozen();
                attributeinstance.addTransientModifier(new AttributeModifier(SPEED_MODIFIER_POWDER_SNOW_ID, f, AttributeModifier.Operation.ADD_VALUE));
            }
        }
    }

    protected void onChangedBlock(ServerLevel level, BlockPos pos) {
        EnchantmentHelper.runLocationChangedEffects(level, this);
    }

    public boolean isBaby() {
        return false;
    }

    public float getAgeScale() {
        return this.isBaby() ? 0.5F : 1.0F;
    }

    public final float getScale() {
        AttributeMap attributemap = this.getAttributes();
        return attributemap == null ? 1.0F : this.sanitizeScale((float)attributemap.getValue(Attributes.SCALE));
    }

    protected float sanitizeScale(float scale) {
        return scale;
    }

    public boolean isAffectedByFluids() {
        return true;
    }

    protected void tickDeath() {
        this.deathTime++;
        if (this.deathTime >= 20 && !this.level().isClientSide() && !this.isRemoved()) {
            this.level().broadcastEntityEvent(this, (byte)60);
            this.remove(Entity.RemovalReason.KILLED);
        }
    }

    public boolean shouldDropExperience() {
        return !this.isBaby();
    }

    protected boolean shouldDropLoot(ServerLevel level) {
        return !this.isBaby() && level.getGameRules().getBoolean(GameRules.RULE_DOMOBLOOT);
    }

    /**
     * Decrements the entity's air supply when underwater
     */
    protected int decreaseAirSupply(int currentAir) {
        AttributeInstance attributeinstance = this.getAttribute(Attributes.OXYGEN_BONUS);
        double d0;
        if (attributeinstance != null) {
            d0 = attributeinstance.getValue();
        } else {
            d0 = 0.0;
        }

        return d0 > 0.0 && this.random.nextDouble() >= 1.0 / (d0 + 1.0) ? currentAir : currentAir - 1;
    }

    protected int increaseAirSupply(int currentAir) {
        return Math.min(currentAir + 4, this.getMaxAirSupply());
    }

    public final int getExperienceReward(ServerLevel level, @Nullable Entity killer) {
        return EnchantmentHelper.processMobExperience(level, killer, this, this.getBaseExperienceReward(level));
    }

    protected int getBaseExperienceReward(ServerLevel level) {
        return 0;
    }

    protected boolean isAlwaysExperienceDropper() {
        return false;
    }

    @Nullable
    public LivingEntity getLastHurtByMob() {
        return EntityReference.getLivingEntity(this.lastHurtByMob, this.level());
    }

    @Nullable
    public Player getLastHurtByPlayer() {
        return EntityReference.getPlayer(this.lastHurtByPlayer, this.level());
    }

    @Override
    public LivingEntity getLastAttacker() {
        return this.getLastHurtByMob();
    }

    public int getLastHurtByMobTimestamp() {
        return this.lastHurtByMobTimestamp;
    }

    public void setLastHurtByPlayer(Player player, int memoryTime) {
        this.setLastHurtByPlayer(EntityReference.of(player), memoryTime);
    }

    public void setLastHurtByPlayer(UUID uuid, int memoryTime) {
        this.setLastHurtByPlayer(EntityReference.of(uuid), memoryTime);
    }

    private void setLastHurtByPlayer(EntityReference<Player> player, int memoryTime) {
        this.lastHurtByPlayer = player;
        this.lastHurtByPlayerMemoryTime = memoryTime;
    }

    /**
     * Hint to AI tasks that we were attacked by the passed EntityLivingBase and should retaliate. Is not guaranteed to change our actual active target (for example if we are currently busy attacking someone else)
     */
    public void setLastHurtByMob(@Nullable LivingEntity livingEntity) {
        this.lastHurtByMob = EntityReference.of(livingEntity);
        this.lastHurtByMobTimestamp = this.tickCount;
    }

    @Nullable
    public LivingEntity getLastHurtMob() {
        return this.lastHurtMob;
    }

    public int getLastHurtMobTimestamp() {
        return this.lastHurtMobTimestamp;
    }

    public void setLastHurtMob(Entity entity) {
        if (entity instanceof LivingEntity) {
            this.lastHurtMob = (LivingEntity)entity;
        } else {
            this.lastHurtMob = null;
        }

        this.lastHurtMobTimestamp = this.tickCount;
    }

    public int getNoActionTime() {
        return this.noActionTime;
    }

    public void setNoActionTime(int idleTime) {
        this.noActionTime = idleTime;
    }

    public boolean shouldDiscardFriction() {
        return this.discardFriction;
    }

    public void setDiscardFriction(boolean discardFriction) {
        this.discardFriction = discardFriction;
    }

    protected boolean doesEmitEquipEvent(EquipmentSlot slot) {
        return true;
    }

    public void onEquipItem(EquipmentSlot slot, ItemStack oldItem, ItemStack newItem) {
        if (!this.level().isClientSide() && !this.isSpectator()) {
            if (!ItemStack.isSameItemSameComponents(oldItem, newItem) && !this.firstTick) {
                Equippable equippable = newItem.get(DataComponents.EQUIPPABLE);
                if (!this.isSilent() && equippable != null && slot == equippable.slot()) {
                    this.level()
                        .playSeededSound(
                            null,
                            this.getX(),
                            this.getY(),
                            this.getZ(),
                            this.getEquipSound(slot, newItem, equippable),
                            this.getSoundSource(),
                            1.0F,
                            1.0F,
                            this.random.nextLong()
                        );
                }

                if (this.doesEmitEquipEvent(slot)) {
                    this.gameEvent(equippable != null ? GameEvent.EQUIP : GameEvent.UNEQUIP);
                }
            }
        }
    }

    protected Holder<SoundEvent> getEquipSound(EquipmentSlot slot, ItemStack stack, Equippable equippable) {
        return equippable.equipSound();
    }

    @Override
    public void remove(Entity.RemovalReason reason) {
        if ((reason == Entity.RemovalReason.KILLED || reason == Entity.RemovalReason.DISCARDED) && this.level() instanceof ServerLevel serverlevel) {
            this.triggerOnDeathMobEffects(serverlevel, reason);
        }

        super.remove(reason);
        this.brain.clearMemories();
    }

    @Override
    public void onRemoval(Entity.RemovalReason reason) {
        super.onRemoval(reason);
        if (this.level() instanceof ServerLevel serverlevel) {
            serverlevel.getWaypointManager().untrackWaypoint((WaypointTransmitter)this);
        }
    }

    protected void triggerOnDeathMobEffects(ServerLevel level, Entity.RemovalReason removalReason) {
        for (MobEffectInstance mobeffectinstance : this.getActiveEffects()) {
            mobeffectinstance.onMobRemoved(level, this, removalReason);
        }

        this.activeEffects.clear();
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        output.putFloat("Health", this.getHealth());
        output.putShort("HurtTime", (short)this.hurtTime);
        output.putInt("HurtByTimestamp", this.lastHurtByMobTimestamp);
        output.putShort("DeathTime", (short)this.deathTime);
        output.putFloat("AbsorptionAmount", this.getAbsorptionAmount());
        output.store("attributes", AttributeInstance.Packed.LIST_CODEC, this.getAttributes().pack());
        if (!this.activeEffects.isEmpty()) {
            output.store("active_effects", MobEffectInstance.CODEC.listOf(), List.copyOf(this.activeEffects.values()));
        }

        output.putBoolean("FallFlying", this.isFallFlying());
        this.getSleepingPos().ifPresent(p_421363_ -> output.store("sleeping_pos", BlockPos.CODEC, p_421363_));
        DataResult<Dynamic<?>> dataresult = this.brain.serializeStart(NbtOps.INSTANCE).map(p_421361_ -> new Dynamic<>(NbtOps.INSTANCE, p_421361_));
        dataresult.resultOrPartial(LOGGER::error).ifPresent(p_421360_ -> output.store("Brain", Codec.PASSTHROUGH, (Dynamic<?>)p_421360_));
        if (this.lastHurtByPlayer != null) {
            this.lastHurtByPlayer.store(output, "last_hurt_by_player");
            output.putInt("last_hurt_by_player_memory_time", this.lastHurtByPlayerMemoryTime);
        }

        if (this.lastHurtByMob != null) {
            this.lastHurtByMob.store(output, "last_hurt_by_mob");
            output.putInt("ticks_since_last_hurt_by_mob", this.tickCount - this.lastHurtByMobTimestamp);
        }

        if (!this.equipment.isEmpty()) {
            output.store("equipment", EntityEquipment.CODEC, this.equipment);
        }

        if (this.locatorBarIcon.hasData()) {
            output.store("locator_bar_icon", Waypoint.Icon.CODEC, this.locatorBarIcon);
        }
    }

    @Nullable
    public ItemEntity drop(ItemStack stack, boolean randomizeMotion, boolean includeThrower) {
        if (stack.isEmpty()) {
            return null;
        } else if (this.level().isClientSide()) {
            this.swing(InteractionHand.MAIN_HAND);
            return null;
        } else {
            ItemEntity itementity = this.createItemStackToDrop(stack, randomizeMotion, includeThrower);
            if (itementity != null) {
                if (captureDrops() != null) {
                    captureDrops().add(itementity);
                } else {
                    this.level().addFreshEntity(itementity);
                }
            }

            return itementity;
        }
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        this.internalSetAbsorptionAmount(input.getFloatOr("AbsorptionAmount", 0.0F));
        if (this.level() != null && !this.level().isClientSide()) {
            input.read("attributes", AttributeInstance.Packed.LIST_CODEC).ifPresent(this.getAttributes()::apply);
        }

        List<MobEffectInstance> list = input.read("active_effects", MobEffectInstance.CODEC.listOf()).orElse(List.of());
        this.activeEffects.clear();

        for (MobEffectInstance mobeffectinstance : list) {
            this.activeEffects.put(mobeffectinstance.getEffect(), mobeffectinstance);
        }

        this.setHealth(input.getFloatOr("Health", this.getMaxHealth()));
        this.hurtTime = input.getShortOr("HurtTime", (short)0);
        this.deathTime = input.getShortOr("DeathTime", (short)0);
        this.lastHurtByMobTimestamp = input.getIntOr("HurtByTimestamp", 0);
        input.getString("Team").ifPresent(p_426978_ -> {
            Scoreboard scoreboard = this.level().getScoreboard();
            PlayerTeam playerteam = scoreboard.getPlayerTeam(p_426978_);
            boolean flag = playerteam != null && scoreboard.addPlayerToTeam(this.getStringUUID(), playerteam);
            if (!flag) {
                LOGGER.warn("Unable to add mob to team \"{}\" (that team probably doesn't exist)", p_426978_);
            }
        });
        this.setSharedFlag(7, input.getBooleanOr("FallFlying", false));
        input.read("sleeping_pos", BlockPos.CODEC).ifPresentOrElse(p_404278_ -> {
            this.setSleepingPos(p_404278_);
            this.entityData.set(DATA_POSE, Pose.SLEEPING);
            if (!this.firstTick) {
                this.setPosToBed(p_404278_);
            }
        }, this::clearSleepingPos);
        input.read("Brain", Codec.PASSTHROUGH).ifPresent(p_421364_ -> this.brain = this.makeBrain((Dynamic<?>)p_421364_));
        this.lastHurtByPlayer = EntityReference.read(input, "last_hurt_by_player");
        this.lastHurtByPlayerMemoryTime = input.getIntOr("last_hurt_by_player_memory_time", 0);
        this.lastHurtByMob = EntityReference.read(input, "last_hurt_by_mob");
        this.lastHurtByMobTimestamp = input.getIntOr("ticks_since_last_hurt_by_mob", 0) + this.tickCount;
        this.equipment.setAll(input.read("equipment", EntityEquipment.CODEC).orElseGet(EntityEquipment::new));
        this.locatorBarIcon = input.read("locator_bar_icon", Waypoint.Icon.CODEC).orElseGet(Waypoint.Icon::new);
    }

    protected void tickEffects() {
        if (this.level() instanceof ServerLevel serverlevel) {
            Iterator<Holder<MobEffect>> iterator = this.activeEffects.keySet().iterator();

            try {
                while (iterator.hasNext()) {
                    Holder<MobEffect> holder = iterator.next();
                    MobEffectInstance mobeffectinstance = this.activeEffects.get(holder);
                    if (!mobeffectinstance.tickServer(serverlevel, this, () -> this.onEffectUpdated(mobeffectinstance, true, null))) {
                        if (!net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.entity.living.MobEffectEvent.Expired(this, mobeffectinstance)).isCanceled()) {
                        iterator.remove();
                        this.onEffectsRemoved(List.of(mobeffectinstance));
                        }
                    } else if (mobeffectinstance.getDuration() % 600 == 0) {
                        this.onEffectUpdated(mobeffectinstance, false, null);
                    }
                }
            } catch (ConcurrentModificationException concurrentmodificationexception) {
            }

            if (this.effectsDirty) {
                this.updateInvisibilityStatus();
                this.updateGlowingStatus();
                this.effectsDirty = false;
            }
        } else {
            for (MobEffectInstance mobeffectinstance1 : this.activeEffects.values()) {
                mobeffectinstance1.tickClient();
            }

            List<ParticleOptions> list = this.entityData.get(DATA_EFFECT_PARTICLES);
            if (!list.isEmpty()) {
                boolean flag = this.entityData.get(DATA_EFFECT_AMBIENCE_ID);
                int j = this.isInvisible() ? 15 : 4;
                int i = flag ? 5 : 1;
                if (this.random.nextInt(j * i) == 0) {
                    this.level().addParticle(Util.getRandom(list, this.random), this.getRandomX(0.5), this.getRandomY(), this.getRandomZ(0.5), 1.0, 1.0, 1.0);
                }
            }
        }
    }

    protected void updateInvisibilityStatus() {
        if (this.activeEffects.isEmpty()) {
            this.removeEffectParticles();
            this.setInvisible(false);
        } else {
            this.setInvisible(this.hasEffect(MobEffects.INVISIBILITY));
            this.updateSynchronizedMobEffectParticles();
        }
    }

    private void updateSynchronizedMobEffectParticles() {
        List<ParticleOptions> list = this.activeEffects
            .values()
            .stream()
            .map(effect -> net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.entity.living.EffectParticleModificationEvent(this, effect)))
            .filter(net.neoforged.neoforge.event.entity.living.EffectParticleModificationEvent::isVisible)
            .map(net.neoforged.neoforge.event.entity.living.EffectParticleModificationEvent::getParticleOptions)
            .toList();
        this.entityData.set(DATA_EFFECT_PARTICLES, list);
        this.entityData.set(DATA_EFFECT_AMBIENCE_ID, areAllEffectsAmbient(this.activeEffects.values()));
    }

    private void updateGlowingStatus() {
        boolean flag = this.isCurrentlyGlowing();
        if (this.getSharedFlag(6) != flag) {
            this.setSharedFlag(6, flag);
        }
    }

    public double getVisibilityPercent(@Nullable Entity lookingEntity) {
        double d0 = 1.0;
        if (this.isDiscrete()) {
            d0 *= 0.8;
        }

        if (this.isInvisible()) {
            float f = this.getArmorCoverPercentage();
            if (f < 0.1F) {
                f = 0.1F;
            }

            d0 *= 0.7 * f;
        }

        if (lookingEntity != null) {
            ItemStack itemstack = this.getItemBySlot(EquipmentSlot.HEAD);
            EntityType<?> entitytype = lookingEntity.getType();
            if (entitytype == EntityType.SKELETON && itemstack.is(Items.SKELETON_SKULL)
                || entitytype == EntityType.ZOMBIE && itemstack.is(Items.ZOMBIE_HEAD)
                || entitytype == EntityType.PIGLIN && itemstack.is(Items.PIGLIN_HEAD)
                || entitytype == EntityType.PIGLIN_BRUTE && itemstack.is(Items.PIGLIN_HEAD)
                || entitytype == EntityType.CREEPER && itemstack.is(Items.CREEPER_HEAD)) {
                d0 *= 0.5;
            }
        }

        d0 = net.neoforged.neoforge.common.CommonHooks.getEntityVisibilityMultiplier(this, lookingEntity, d0);
        return d0;
    }

    public boolean canAttack(LivingEntity target) {
        return target instanceof Player && this.level().getDifficulty() == Difficulty.PEACEFUL ? false : target.canBeSeenAsEnemy();
    }

    public boolean canBeSeenAsEnemy() {
        return !this.isInvulnerable() && this.canBeSeenByAnyone();
    }

    public boolean canBeSeenByAnyone() {
        return !this.isSpectator() && this.isAlive();
    }

    /**
     * Returns {@code true} if all the potion effects in the specified collection are ambient.
     */
    public static boolean areAllEffectsAmbient(Collection<MobEffectInstance> potionEffects) {
        for (MobEffectInstance mobeffectinstance : potionEffects) {
            if (mobeffectinstance.isVisible() && !mobeffectinstance.isAmbient()) {
                return false;
            }
        }

        return true;
    }

    protected void removeEffectParticles() {
        this.entityData.set(DATA_EFFECT_PARTICLES, List.of());
    }

    public boolean removeAllEffects() {
        if (this.level().isClientSide()) {
            return false;
        } else if (this.activeEffects.isEmpty()) {
            return false;
        } else {
            Map<Holder<MobEffect>, MobEffectInstance> map = new java.util.HashMap<>(this.activeEffects.size());
            for (Map.Entry<Holder<MobEffect>, MobEffectInstance> entry : this.activeEffects.entrySet()) {
                if (!net.neoforged.neoforge.event.EventHooks.onEffectRemoved(this, entry.getValue())) {
                    map.put(entry.getKey(), entry.getValue());
                }
            }
            map.keySet().forEach(this.activeEffects::remove);
            this.onEffectsRemoved(map.values());
            return true;
        }
    }

    public Collection<MobEffectInstance> getActiveEffects() {
        return this.activeEffects.values();
    }

    public Map<Holder<MobEffect>, MobEffectInstance> getActiveEffectsMap() {
        return this.activeEffects;
    }

    public boolean hasEffect(Holder<MobEffect> effect) {
        return this.activeEffects.containsKey(effect);
    }

    @Nullable
    public MobEffectInstance getEffect(Holder<MobEffect> effect) {
        return this.activeEffects.get(effect);
    }

    public float getEffectBlendFactor(Holder<MobEffect> effect, float partialTick) {
        MobEffectInstance mobeffectinstance = this.getEffect(effect);
        return mobeffectinstance != null ? mobeffectinstance.getBlendFactor(this, partialTick) : 0.0F;
    }

    public final boolean addEffect(MobEffectInstance effectInstance) {
        return this.addEffect(effectInstance, null);
    }

    public boolean addEffect(MobEffectInstance effectInstance, @Nullable Entity entity) {
        if (!net.neoforged.neoforge.common.CommonHooks.canMobEffectBeApplied(this, effectInstance, entity)) {
            return false;
        } else {
            MobEffectInstance mobeffectinstance = this.activeEffects.get(effectInstance.getEffect());
            boolean flag = false;
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.entity.living.MobEffectEvent.Added(this, mobeffectinstance, effectInstance, entity));
            if (mobeffectinstance == null) {
                this.activeEffects.put(effectInstance.getEffect(), effectInstance);
                this.onEffectAdded(effectInstance, entity);
                flag = true;
                effectInstance.onEffectAdded(this);
            } else if (mobeffectinstance.update(effectInstance)) {
                this.onEffectUpdated(mobeffectinstance, true, entity);
                flag = true;
            }

            effectInstance.onEffectStarted(this);
            return flag;
        }
    }

    /**
     * Neo: Override-Only. Call via
     * {@link net.neoforged.neoforge.common.CommonHooks#canMobEffectBeApplied(LivingEntity, MobEffectInstance, Entity)}
     *
     * @param effectInstance A mob effect instance
     * @return If the mob effect instance can be applied to this entity
     */
    @Deprecated
    @org.jetbrains.annotations.ApiStatus.OverrideOnly
    public boolean canBeAffected(MobEffectInstance effectInstance) {
        if (this.getType().is(EntityTypeTags.IMMUNE_TO_INFESTED)) {
            return !effectInstance.is(MobEffects.INFESTED);
        } else if (this.getType().is(EntityTypeTags.IMMUNE_TO_OOZING)) {
            return !effectInstance.is(MobEffects.OOZING);
        } else {
            return !this.getType().is(EntityTypeTags.IGNORES_POISON_AND_REGEN)
                ? true
                : !effectInstance.is(MobEffects.REGENERATION) && !effectInstance.is(MobEffects.POISON);
        }
    }

    public void forceAddEffect(MobEffectInstance instance, @Nullable Entity entity) {
        if (net.neoforged.neoforge.common.CommonHooks.canMobEffectBeApplied(this, instance, entity)) {
            MobEffectInstance mobeffectinstance = this.activeEffects.put(instance.getEffect(), instance);
            if (mobeffectinstance == null) {
                this.onEffectAdded(instance, entity);
            } else {
                instance.copyBlendState(mobeffectinstance);
                this.onEffectUpdated(instance, true, entity);
            }
        }
    }

    public boolean isInvertedHealAndHarm() {
        return this.getType().is(EntityTypeTags.INVERTED_HEALING_AND_HARM);
    }

    @Nullable
    public final MobEffectInstance removeEffectNoUpdate(Holder<MobEffect> effect) {
        return this.activeEffects.remove(effect);
    }

    public boolean removeEffect(Holder<MobEffect> effect) {
        if (net.neoforged.neoforge.event.EventHooks.onEffectRemoved(this, effect)) return false;
        MobEffectInstance mobeffectinstance = this.removeEffectNoUpdate(effect);
        if (mobeffectinstance != null) {
            this.onEffectsRemoved(List.of(mobeffectinstance));
            return true;
        } else {
            return false;
        }
    }

    protected void onEffectAdded(MobEffectInstance effectInstance, @Nullable Entity entity) {
        if (!this.level().isClientSide()) {
            this.effectsDirty = true;
            effectInstance.getEffect().value().addAttributeModifiers(this.getAttributes(), effectInstance.getAmplifier());
            this.sendEffectToPassengers(effectInstance);
        }
    }

    public void sendEffectToPassengers(MobEffectInstance effectInstance) {
        for (Entity entity : this.getPassengers()) {
            if (entity instanceof ServerPlayer serverplayer) {
                serverplayer.connection.send(new ClientboundUpdateMobEffectPacket(this.getId(), effectInstance, false));
            }
        }
    }

    protected void onEffectUpdated(MobEffectInstance effectInstance, boolean forced, @Nullable Entity entity) {
        if (!this.level().isClientSide()) {
            this.effectsDirty = true;
            if (forced) {
                MobEffect mobeffect = effectInstance.getEffect().value();
                mobeffect.removeAttributeModifiers(this.getAttributes());
                mobeffect.addAttributeModifiers(this.getAttributes(), effectInstance.getAmplifier());
                this.refreshDirtyAttributes();
            }

            this.sendEffectToPassengers(effectInstance);
        }
    }

    protected void onEffectsRemoved(Collection<MobEffectInstance> effects) {
        if (!this.level().isClientSide()) {
            this.effectsDirty = true;

            for (MobEffectInstance mobeffectinstance : effects) {
                mobeffectinstance.getEffect().value().removeAttributeModifiers(this.getAttributes());

                for (Entity entity : this.getPassengers()) {
                    if (entity instanceof ServerPlayer serverplayer) {
                        serverplayer.connection.send(new ClientboundRemoveMobEffectPacket(this.getId(), mobeffectinstance.getEffect()));
                    }
                }
            }

            this.refreshDirtyAttributes();
        }
    }

    private void refreshDirtyAttributes() {
        Set<AttributeInstance> set = this.getAttributes().getAttributesToUpdate();

        for (AttributeInstance attributeinstance : set) {
            this.onAttributeUpdated(attributeinstance.getAttribute());
        }

        set.clear();
    }

    protected void onAttributeUpdated(Holder<Attribute> attribute) {
        if (attribute.is(Attributes.MAX_HEALTH)) {
            float f = this.getMaxHealth();
            if (this.getHealth() > f) {
                this.setHealth(f);
            }
        } else if (attribute.is(Attributes.MAX_ABSORPTION)) {
            float f1 = this.getMaxAbsorption();
            if (this.getAbsorptionAmount() > f1) {
                this.setAbsorptionAmount(f1);
            }
        } else if (attribute.is(Attributes.SCALE)) {
            this.refreshDimensions();
        } else if (attribute.is(Attributes.WAYPOINT_TRANSMIT_RANGE) && this.level() instanceof ServerLevel serverlevel) {
            ServerWaypointManager serverwaypointmanager = serverlevel.getWaypointManager();
            if (this.attributes.getValue(attribute) > 0.0) {
                serverwaypointmanager.trackWaypoint((WaypointTransmitter)this);
            } else {
                serverwaypointmanager.untrackWaypoint((WaypointTransmitter)this);
            }
        }
    }

    /**
     * Heal living entity (param: amount of half-hearts)
     */
    public void heal(float healAmount) {
        healAmount = net.neoforged.neoforge.event.EventHooks.onLivingHeal(this, healAmount);
        if (healAmount <= 0) return;
        float f = this.getHealth();
        if (f > 0.0F) {
            this.setHealth(f + healAmount);
        }
    }

    public float getHealth() {
        return this.entityData.get(DATA_HEALTH_ID);
    }

    public void setHealth(float health) {
        this.entityData.set(DATA_HEALTH_ID, Mth.clamp(health, 0.0F, this.getMaxHealth()));
    }

    public boolean isDeadOrDying() {
        return this.getHealth() <= 0.0F;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource damageSource, float amount) {
        if (this.isInvulnerableTo(level, damageSource)) {
            return false;
        } else if (this.isDeadOrDying()) {
            return false;
        } else if (damageSource.is(DamageTypeTags.IS_FIRE) && this.hasEffect(MobEffects.FIRE_RESISTANCE)) {
            return false;
        } else {
            this.damageContainers.push(new net.neoforged.neoforge.common.damagesource.DamageContainer(damageSource, amount));
            if (net.neoforged.neoforge.common.CommonHooks.onEntityIncomingDamage(this, this.damageContainers.peek())) return false;
            if (this.isSleeping()) {
                this.stopSleeping();
            }

            this.noActionTime = 0;
            amount = this.damageContainers.peek().getNewDamage(); //Neo: enforce damage container as source of truth for damage amount
            if (amount < 0.0F) {
                amount = 0.0F;
            }

            ItemStack itemstack = this.getUseItem();
            float f = this.applyItemBlocking(level, damageSource, amount);
            amount -= f;
            boolean flag = f > 0.0F;
            if (damageSource.is(DamageTypeTags.IS_FREEZING) && this.getType().is(EntityTypeTags.FREEZE_HURTS_EXTRA_TYPES)) {
                amount *= 5.0F;
            }

            if (damageSource.is(DamageTypeTags.DAMAGES_HELMET) && !this.getItemBySlot(EquipmentSlot.HEAD).isEmpty()) {
                this.hurtHelmet(damageSource, amount);
                amount *= 0.75F;
            }

            if (Float.isNaN(amount) || Float.isInfinite(amount)) {
                amount = Float.MAX_VALUE;
            }
            this.damageContainers.peek().setNewDamage(amount); //update container with vanilla changes

            boolean flag1 = true;
            if (this.invulnerableTime > 10.0F && !damageSource.is(DamageTypeTags.BYPASSES_COOLDOWN)) {
                if (amount <= this.lastHurt) {
                    this.damageContainers.pop();
                    return false;
                }

                this.damageContainers.peek().setReduction(net.neoforged.neoforge.common.damagesource.DamageContainer.Reduction.INVULNERABILITY, this.lastHurt);
                this.actuallyHurt(level, damageSource, amount - this.lastHurt);
                this.lastHurt = amount;
                flag1 = false;
            } else {
                this.lastHurt = amount;
                this.invulnerableTime = this.damageContainers.peek().getPostAttackInvulnerabilityTicks();
                this.actuallyHurt(level, damageSource, amount);
                this.hurtDuration = 10;
                this.hurtTime = this.hurtDuration;
            }

            amount = this.damageContainers.peek().getNewDamage(); //update local with container value
            this.resolveMobResponsibleForDamage(damageSource);
            this.resolvePlayerResponsibleForDamage(damageSource);
            if (flag1) {
                BlocksAttacks blocksattacks = itemstack.get(DataComponents.BLOCKS_ATTACKS);
                if (flag && blocksattacks != null) {
                    blocksattacks.onBlocked(level, this);
                } else {
                    level.broadcastDamageEvent(this, damageSource);
                }

                if (!damageSource.is(DamageTypeTags.NO_IMPACT) && (!flag || amount > 0.0F)) {
                    this.markHurt();
                }

                if (!damageSource.is(DamageTypeTags.NO_KNOCKBACK)) {
                    double d0 = 0.0;
                    double d1 = 0.0;
                    if (damageSource.getDirectEntity() instanceof Projectile projectile) {
                        DoubleDoubleImmutablePair doubledoubleimmutablepair = projectile.calculateHorizontalHurtKnockbackDirection(this, damageSource);
                        d0 = -doubledoubleimmutablepair.leftDouble();
                        d1 = -doubledoubleimmutablepair.rightDouble();
                    } else if (damageSource.getSourcePosition() != null) {
                        d0 = damageSource.getSourcePosition().x() - this.getX();
                        d1 = damageSource.getSourcePosition().z() - this.getZ();
                    }

                    this.knockback(0.4F, d0, d1);
                    if (!flag) {
                        this.indicateDamage(d0, d1);
                    }
                }
            }

            if (this.isDeadOrDying()) {
                if (!this.checkTotemDeathProtection(damageSource)) {
                    if (flag1) {
                        this.makeSound(this.getDeathSound());
                        this.playSecondaryHurtSound(damageSource);
                    }

                    this.die(damageSource);
                }
            } else if (flag1) {
                this.playHurtSound(damageSource);
                this.playSecondaryHurtSound(damageSource);
            }

            boolean flag2 = !flag || amount > 0.0F;
            if (flag2) {
                this.lastDamageSource = damageSource;
                this.lastDamageStamp = this.level().getGameTime();

                for (MobEffectInstance mobeffectinstance : this.getActiveEffects()) {
                    mobeffectinstance.onMobHurt(level, this, damageSource, amount);
                }
            }

            if (this instanceof ServerPlayer serverplayer) {
                CriteriaTriggers.ENTITY_HURT_PLAYER.trigger(serverplayer, damageSource, amount, amount, flag);
                if (f > 0.0F && f < 3.4028235E37F) {
                    serverplayer.awardStat(Stats.DAMAGE_BLOCKED_BY_SHIELD, Math.round(f * 10.0F));
                }
            }

            if (damageSource.getEntity() instanceof ServerPlayer serverplayer1) {
                CriteriaTriggers.PLAYER_HURT_ENTITY.trigger(serverplayer1, this, damageSource, amount, amount, flag);
            }

            this.damageContainers.pop();
            return flag2;
        }
    }

    public float applyItemBlocking(ServerLevel level, DamageSource damageSource, float damageAmount) {
        if (damageAmount <= 0.0F) {
            return 0.0F;
        } else {
            ItemStack itemstack = this.getItemBlockingWith();
            if (itemstack == null) {
                return 0.0F;
            } else {
                BlocksAttacks blocksattacks = itemstack.get(DataComponents.BLOCKS_ATTACKS);
                if (blocksattacks != null) {
                    if (damageSource.getDirectEntity() instanceof AbstractArrow abstractarrow && abstractarrow.getPierceLevel() > 0) {
                        return 0.0F;
                    } else {
                        Vec3 vec3 = damageSource.getSourcePosition();
                        double d0;
                        if (vec3 != null) {
                            Vec3 vec31 = this.calculateViewVector(0.0F, this.getYHeadRot());
                            Vec3 vec32 = vec3.subtract(this.position());
                            vec32 = new Vec3(vec32.x, 0.0, vec32.z).normalize();
                            d0 = Math.acos(vec32.dot(vec31));
                        } else {
                            d0 = (float) Math.PI;
                        }

                        float f = blocksattacks.resolveBlockedDamage(damageSource, damageAmount, d0);

                        var ev = net.neoforged.neoforge.common.CommonHooks.onDamageBlock(this, this.damageContainers.peek(), f, !blocksattacks.bypassedBy().map(damageSource::is).orElse(false));
                        if (!ev.getBlocked()) return 0.0F;

                        f = ev.getBlockedDamage();
                        this.damageContainers.peek().setBlockedDamage(ev);

                        blocksattacks.hurtBlockingItem(this.level(), itemstack, this, this.getUsedItemHand(), f, ev.shieldDamage());
                        if (f > 0.0F && !damageSource.is(DamageTypeTags.IS_PROJECTILE) && damageSource.getDirectEntity() instanceof LivingEntity livingentity) {
                            this.blockUsingItem(level, livingentity);
                        }

                        return f;
                    }
                } else {
                    return 0.0F;
                }
            }
        }
    }

    private void playSecondaryHurtSound(DamageSource damageSource) {
        if (damageSource.is(DamageTypes.THORNS)) {
            SoundSource soundsource = this instanceof Player ? SoundSource.PLAYERS : SoundSource.HOSTILE;
            this.level().playSound(null, this.position().x, this.position().y, this.position().z, SoundEvents.THORNS_HIT, soundsource);
        }
    }

    protected void resolveMobResponsibleForDamage(DamageSource damageSource) {
        if (damageSource.getEntity() instanceof LivingEntity livingentity
            && !damageSource.is(DamageTypeTags.NO_ANGER)
            && (!damageSource.is(DamageTypes.WIND_CHARGE) || !this.getType().is(EntityTypeTags.NO_ANGER_FROM_WIND_CHARGE))) {
            this.setLastHurtByMob(livingentity);
        }
    }

    @Nullable
    protected Player resolvePlayerResponsibleForDamage(DamageSource damageSource) {
        Entity entity = damageSource.getEntity();
        if (entity instanceof Player player) {
            this.setLastHurtByPlayer(player, 100);
        } else if (entity instanceof TamableAnimal tamableAnimal && tamableAnimal.isTame()) {
            if (tamableAnimal.getOwnerReference() != null) {
                this.setLastHurtByPlayer(tamableAnimal.getOwnerReference().getUUID(), 100);
            } else {
                this.lastHurtByPlayer = null;
                this.lastHurtByPlayerMemoryTime = 0;
            }
        }

        return EntityReference.getPlayer(this.lastHurtByPlayer, this.level());
    }

    protected void blockUsingItem(ServerLevel level, LivingEntity entity) {
        entity.blockedByItem(this);
    }

    protected void blockedByItem(LivingEntity entity) {
        entity.knockback(0.5, entity.getX() - this.getX(), entity.getZ() - this.getZ());
    }

    private boolean checkTotemDeathProtection(DamageSource damageSource) {
        if (damageSource.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return false;
        } else {
            ItemStack itemstack = null;
            DeathProtection deathprotection = null;

            for (InteractionHand interactionhand : InteractionHand.values()) {
                ItemStack itemstack1 = this.getItemInHand(interactionhand);
                deathprotection = itemstack1.get(DataComponents.DEATH_PROTECTION);
                if (deathprotection != null && net.neoforged.neoforge.common.CommonHooks.onLivingUseTotem(this, damageSource, itemstack1, interactionhand)) {
                    itemstack = itemstack1.copy();
                    itemstack1.shrink(1);
                    break;
                }
            }

            if (itemstack != null) {
                if (this instanceof ServerPlayer serverplayer) {
                    serverplayer.awardStat(Stats.ITEM_USED.get(itemstack.getItem()));
                    CriteriaTriggers.USED_TOTEM.trigger(serverplayer, itemstack);
                    this.gameEvent(GameEvent.ITEM_INTERACT_FINISH);
                }

                this.setHealth(1.0F);
                deathprotection.applyEffects(itemstack, this);
                this.level().broadcastEntityEvent(this, (byte)35);
            }

            return deathprotection != null;
        }
    }

    @Nullable
    public DamageSource getLastDamageSource() {
        if (this.level().getGameTime() - this.lastDamageStamp > 40L) {
            this.lastDamageSource = null;
        }

        return this.lastDamageSource;
    }

    protected void playHurtSound(DamageSource source) {
        this.makeSound(this.getHurtSound(source));
    }

    public void makeSound(@Nullable SoundEvent sound) {
        if (sound != null) {
            this.playSound(sound, this.getSoundVolume(), this.getVoicePitch());
        }
    }

    /**
     * Renders broken item particles using the given ItemStack
     */
    private void breakItem(ItemStack stack) {
        if (!stack.isEmpty()) {
            Holder<SoundEvent> holder = stack.get(DataComponents.BREAK_SOUND);
            if (holder != null && !this.isSilent()) {
                this.level()
                    .playLocalSound(
                        this.getX(),
                        this.getY(),
                        this.getZ(),
                        holder.value(),
                        this.getSoundSource(),
                        0.8F,
                        0.8F + this.level().random.nextFloat() * 0.4F,
                        false
                    );
            }

            this.spawnItemParticles(stack, 5);
        }
    }

    /**
     * Called when the mob's health reaches 0.
     */
    public void die(DamageSource damageSource) {
        if (net.neoforged.neoforge.common.CommonHooks.onLivingDeath(this, damageSource)) return;
        if (!this.isRemoved() && !this.dead) {
            Entity entity = damageSource.getEntity();
            LivingEntity livingentity = this.getKillCredit();
            if (livingentity != null) {
                livingentity.awardKillScore(this, damageSource);
            }

            if (this.isSleeping()) {
                this.stopSleeping();
            }

            if (!this.level().isClientSide() && this.hasCustomName()) {
                LOGGER.info("Named entity {} died: {}", this, this.getCombatTracker().getDeathMessage().getString());
            }

            this.dead = true;
            this.getCombatTracker().recheckStatus();
            if (this.level() instanceof ServerLevel serverlevel) {
                if (entity == null || entity.killedEntity(serverlevel, this, damageSource)) {
                    this.gameEvent(GameEvent.ENTITY_DIE);
                    this.dropAllDeathLoot(serverlevel, damageSource);
                    this.createWitherRose(livingentity);
                }

                this.level().broadcastEntityEvent(this, (byte)3);
            }

            this.setPose(Pose.DYING);
        }
    }

    protected void createWitherRose(@Nullable LivingEntity entitySource) {
        if (this.level() instanceof ServerLevel serverlevel) {
            boolean flag = false;
            if (entitySource instanceof WitherBoss) {
                if (net.neoforged.neoforge.event.EventHooks.canEntityGrief(serverlevel, entitySource)) {
                    BlockPos blockpos = this.blockPosition();
                    BlockState blockstate = Blocks.WITHER_ROSE.defaultBlockState();
                    if (this.level().getBlockState(blockpos).isAir() && blockstate.canSurvive(this.level(), blockpos)) {
                        this.level().setBlock(blockpos, blockstate, 3);
                        flag = true;
                    }
                }

                if (!flag) {
                    ItemEntity itementity = new ItemEntity(this.level(), this.getX(), this.getY(), this.getZ(), new ItemStack(Items.WITHER_ROSE));
                    this.level().addFreshEntity(itementity);
                }
            }
        }
    }

    protected void dropAllDeathLoot(ServerLevel p_level, DamageSource damageSource) {
        this.captureDrops(new java.util.ArrayList<>());
        boolean flag = this.lastHurtByPlayerMemoryTime > 0;
        if (this.shouldDropLoot(p_level)) {
            this.dropFromLootTable(p_level, damageSource, flag);
            this.dropCustomDeathLoot(p_level, damageSource, flag);
        }

        this.dropEquipment(p_level);
        this.dropExperience(p_level, damageSource.getEntity());

        Collection<ItemEntity> drops = captureDrops(null);
        if (!net.neoforged.neoforge.common.CommonHooks.onLivingDrops(this, damageSource, drops, lastHurtByPlayerMemoryTime > 0))
            drops.forEach(e -> level().addFreshEntity(e));
    }

    protected void dropEquipment(ServerLevel level) {
    }

    protected void dropExperience(ServerLevel level, @Nullable Entity entity) {
        if (!this.wasExperienceConsumed()
            && (
                this.isAlwaysExperienceDropper()
                    || this.lastHurtByPlayerMemoryTime > 0 && this.shouldDropExperience() && level.getGameRules().getBoolean(GameRules.RULE_DOMOBLOOT)
            )) {
            int reward = net.neoforged.neoforge.event.EventHooks.getExperienceDrop(this, this.getLastHurtByPlayer(), this.getExperienceReward(level, entity));
            ExperienceOrb.award((ServerLevel) this.level(), this.position(), reward);
        }
    }

    protected void dropCustomDeathLoot(ServerLevel level, DamageSource damageSource, boolean recentlyHit) {
    }

    public long getLootTableSeed() {
        return 0L;
    }

    protected float getKnockback(Entity attacker, DamageSource damageSource) {
        float f = (float)this.getAttributeValue(Attributes.ATTACK_KNOCKBACK);
        return this.level() instanceof ServerLevel serverlevel
            ? EnchantmentHelper.modifyKnockback(serverlevel, this.getWeaponItem(), attacker, damageSource, f)
            : f;
    }

    protected void dropFromLootTable(ServerLevel level, DamageSource damageSource, boolean playerKill) {
        Optional<ResourceKey<LootTable>> optional = this.getLootTable();
        if (!optional.isEmpty()) {
            this.dropFromLootTable(level, damageSource, playerKill, optional.get());
        }
    }

    public void dropFromLootTable(ServerLevel level, DamageSource damageSource, boolean playerKill, ResourceKey<LootTable> lootTable) {
        this.dropFromLootTable(level, damageSource, playerKill, lootTable, p_375574_ -> this.spawnAtLocation(level, p_375574_));
    }

    public void dropFromLootTable(
        ServerLevel level, DamageSource damageSource, boolean playerKill, ResourceKey<LootTable> lootTable, Consumer<ItemStack> dropConsumer
    ) {
        LootTable loottable = level.getServer().reloadableRegistries().getLootTable(lootTable);
        LootParams.Builder lootparams$builder = new LootParams.Builder(level)
            .withParameter(LootContextParams.THIS_ENTITY, this)
            .withParameter(LootContextParams.ORIGIN, this.position())
            .withParameter(LootContextParams.DAMAGE_SOURCE, damageSource)
            .withOptionalParameter(LootContextParams.ATTACKING_ENTITY, damageSource.getEntity())
            .withOptionalParameter(LootContextParams.DIRECT_ATTACKING_ENTITY, damageSource.getDirectEntity());
        Player player = this.getLastHurtByPlayer();
        if (playerKill && player != null) {
            lootparams$builder = lootparams$builder.withParameter(LootContextParams.LAST_DAMAGE_PLAYER, player).withLuck(player.getLuck());
        }

        LootParams lootparams = lootparams$builder.create(LootContextParamSets.ENTITY);
        loottable.getRandomItems(lootparams, this.getLootTableSeed(), dropConsumer);
    }

    public boolean dropFromEntityInteractLootTable(
        ServerLevel level, ResourceKey<LootTable> lootTable, @Nullable Entity entity, ItemStack tool, BiConsumer<ServerLevel, ItemStack> dropConsumer
    ) {
        return this.dropFromLootTable(
            level,
            lootTable,
            p_432527_ -> p_432527_.withParameter(LootContextParams.TARGET_ENTITY, this)
                .withOptionalParameter(LootContextParams.INTERACTING_ENTITY, entity)
                .withParameter(LootContextParams.TOOL, tool)
                .create(LootContextParamSets.ENTITY_INTERACT),
            dropConsumer
        );
    }

    public boolean dropFromGiftLootTable(ServerLevel level, ResourceKey<LootTable> lootTable, BiConsumer<ServerLevel, ItemStack> dropConsumer) {
        return this.dropFromLootTable(
            level,
            lootTable,
            p_426981_ -> p_426981_.withParameter(LootContextParams.ORIGIN, this.position())
                .withParameter(LootContextParams.THIS_ENTITY, this)
                .create(LootContextParamSets.GIFT),
            dropConsumer
        );
    }

    protected void dropFromShearingLootTable(
        ServerLevel level, ResourceKey<LootTable> lootTable, ItemStack shears, BiConsumer<ServerLevel, ItemStack> dropConsumer
    ) {
        this.dropFromLootTable(
            level,
            lootTable,
            p_426980_ -> p_426980_.withParameter(LootContextParams.ORIGIN, this.position())
                .withParameter(LootContextParams.THIS_ENTITY, this)
                .withParameter(LootContextParams.TOOL, shears)
                .create(LootContextParamSets.SHEARING),
            dropConsumer
        );
    }

    protected boolean dropFromLootTable(
        ServerLevel level,
        ResourceKey<LootTable> lootTable,
        Function<LootParams.Builder, LootParams> paramsBuilder,
        BiConsumer<ServerLevel, ItemStack> dropConsumer
    ) {
        LootTable loottable = level.getServer().reloadableRegistries().getLootTable(lootTable);
        LootParams lootparams = paramsBuilder.apply(new LootParams.Builder(level));
        List<ItemStack> list = loottable.getRandomItems(lootparams);
        if (!list.isEmpty()) {
            list.forEach(p_375572_ -> dropConsumer.accept(level, p_375572_));
            return true;
        } else {
            return false;
        }
    }

    public void knockback(double strength, double x, double z) {
        net.neoforged.neoforge.event.entity.living.LivingKnockBackEvent event = net.neoforged.neoforge.common.CommonHooks.onLivingKnockBack(this, (float) strength, x, z);
        if(event.isCanceled()) return;
        strength = event.getStrength();
        x = event.getRatioX();
        z = event.getRatioZ();
        strength *= 1.0 - this.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE);
        if (!(strength <= 0.0)) {
            this.hasImpulse = true;
            Vec3 vec3 = this.getDeltaMovement();

            while (x * x + z * z < 1.0E-5F) {
                x = (Math.random() - Math.random()) * 0.01;
                z = (Math.random() - Math.random()) * 0.01;
            }

            Vec3 vec31 = new Vec3(x, 0.0, z).normalize().scale(strength);
            this.setDeltaMovement(vec3.x / 2.0 - vec31.x, this.onGround() ? Math.min(0.4, vec3.y / 2.0 + strength) : vec3.y, vec3.z / 2.0 - vec31.z);
        }
    }

    public void indicateDamage(double xDistance, double zDistance) {
    }

    @Nullable
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return SoundEvents.GENERIC_HURT;
    }

    @Nullable
    protected SoundEvent getDeathSound() {
        return SoundEvents.GENERIC_DEATH;
    }

    private SoundEvent getFallDamageSound(int height) {
        return height > 4 ? this.getFallSounds().big() : this.getFallSounds().small();
    }

    public void skipDropExperience() {
        this.skipDropExperience = true;
    }

    public boolean wasExperienceConsumed() {
        return this.skipDropExperience;
    }

    public float getHurtDir() {
        return 0.0F;
    }

    public AABB getHitbox() {
        AABB aabb = this.getBoundingBox();
        Entity entity = this.getVehicle();
        if (entity != null) {
            Vec3 vec3 = entity.getPassengerRidingPosition(this);
            return aabb.setMinY(Math.max(vec3.y, aabb.minY));
        } else {
            return aabb;
        }
    }

    public Map<Enchantment, Set<EnchantmentLocationBasedEffect>> activeLocationDependentEnchantments(EquipmentSlot slot) {
        return this.activeLocationDependentEnchantments.computeIfAbsent(slot, p_359700_ -> new Reference2ObjectArrayMap<>());
    }

    public LivingEntity.Fallsounds getFallSounds() {
        return new LivingEntity.Fallsounds(SoundEvents.GENERIC_SMALL_FALL, SoundEvents.GENERIC_BIG_FALL);
    }

    public Optional<BlockPos> getLastClimbablePos() {
        return this.lastClimbablePos;
    }

    public boolean onClimbable() {
        if (this.isSpectator()) {
            return false;
        } else {
            BlockPos blockpos = this.blockPosition();
            BlockState blockstate = this.getInBlockState();
            Optional<BlockPos> ladderPos = net.neoforged.neoforge.common.CommonHooks.isLivingOnLadder(blockstate, level(), blockpos, this);
            if (ladderPos.isPresent()) this.lastClimbablePos = ladderPos;
            return ladderPos.isPresent();
        }
    }

    private boolean trapdoorUsableAsLadder(BlockPos pos, BlockState state) {
        if (!state.getValue(TrapDoorBlock.OPEN)) {
            return false;
        } else {
            BlockState blockstate = this.level().getBlockState(pos.below());
            return blockstate.is(Blocks.LADDER) && blockstate.getValue(LadderBlock.FACING) == state.getValue(TrapDoorBlock.FACING);
        }
    }

    @Override
    public boolean isAlive() {
        return !this.isRemoved() && this.getHealth() > 0.0F;
    }

    public boolean isLookingAtMe(LivingEntity entity, double tolerance, boolean scaleByDistance, boolean visual, double... yValues) {
        Vec3 vec3 = entity.getViewVector(1.0F).normalize();

        for (double d0 : yValues) {
            Vec3 vec31 = new Vec3(this.getX() - entity.getX(), d0 - entity.getEyeY(), this.getZ() - entity.getZ());
            double d1 = vec31.length();
            vec31 = vec31.normalize();
            double d2 = vec3.dot(vec31);
            if (d2 > 1.0 - tolerance / (scaleByDistance ? d1 : 1.0)
                && entity.hasLineOfSight(this, visual ? ClipContext.Block.VISUAL : ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, d0)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public int getMaxFallDistance() {
        return this.getComfortableFallDistance(0.0F);
    }

    protected final int getComfortableFallDistance(float health) {
        return Mth.floor(health + 3.0F);
    }

    @Override
    public boolean causeFallDamage(double fallDistance, float damageMultiplier, DamageSource damageSource) {
        var event = net.neoforged.neoforge.common.CommonHooks.onLivingFall(this, fallDistance, damageMultiplier);
        if (event.isCanceled()) return false;
        fallDistance = event.getDistance();
        damageMultiplier = event.getDamageMultiplier();

        boolean flag = super.causeFallDamage(fallDistance, damageMultiplier, damageSource);
        int i = this.calculateFallDamage(fallDistance, damageMultiplier);
        if (i > 0) {
            this.playSound(this.getFallDamageSound(i), 1.0F, 1.0F);
            this.playBlockFallSound();
            this.hurt(damageSource, i);
            return true;
        } else {
            return flag;
        }
    }

    protected int calculateFallDamage(double fallDistance, float damageMultiplier) {
        if (this.getType().is(EntityTypeTags.FALL_DAMAGE_IMMUNE)) {
            return 0;
        } else {
            double d0 = this.calculateFallPower(fallDistance);
            return Mth.floor(d0 * damageMultiplier * this.getAttributeValue(Attributes.FALL_DAMAGE_MULTIPLIER));
        }
    }

    private double calculateFallPower(double fallDistance) {
        return fallDistance + 1.0E-6 - this.getAttributeValue(Attributes.SAFE_FALL_DISTANCE);
    }

    protected void playBlockFallSound() {
        if (!this.isSilent()) {
            int i = Mth.floor(this.getX());
            int j = Mth.floor(this.getY() - 0.2F);
            int k = Mth.floor(this.getZ());
            BlockPos pos = new BlockPos(i, j, k);
            BlockState blockstate = this.level().getBlockState(pos);
            if (!blockstate.isAir()) {
                blockstate.playFallSound(this.level(), pos, this);
            }
        }
    }

    @Override
    public void animateHurt(float yaw) {
        this.hurtDuration = 10;
        this.hurtTime = this.hurtDuration;
    }

    public int getArmorValue() {
        return Mth.floor(this.getAttributeValue(Attributes.ARMOR));
    }

    protected void hurtArmor(DamageSource damageSource, float damageAmount) {
    }

    protected void hurtHelmet(DamageSource damageSource, float damageAmount) {
    }

    protected void doHurtEquipment(DamageSource damageSource, float damageAmount, EquipmentSlot... slots) {
        if (!(damageAmount <= 0.0F)) {
            int i = (int)Math.max(1.0F, damageAmount / 4.0F);

            net.neoforged.neoforge.common.CommonHooks.onArmorHurt(damageSource, slots, i, this);
            if (true) return; //Neo: Invalidates the loop. Armor damage happens in common hook.
            for (EquipmentSlot equipmentslot : slots) {
                ItemStack itemstack = this.getItemBySlot(equipmentslot);
                Equippable equippable = itemstack.get(DataComponents.EQUIPPABLE);
                if (equippable != null && equippable.damageOnHurt() && itemstack.isDamageableItem() && itemstack.canBeHurtBy(damageSource)) {
                    itemstack.hurtAndBreak(i, this, equipmentslot);
                }
            }
        }
    }

    /**
     * Reduces damage, depending on armor
     */
    protected float getDamageAfterArmorAbsorb(DamageSource damageSource, float damageAmount) {
        if (!damageSource.is(DamageTypeTags.BYPASSES_ARMOR)) {
            this.hurtArmor(damageSource, damageAmount);
            damageAmount = CombatRules.getDamageAfterAbsorb(
                this, damageAmount, damageSource, this.getArmorValue(), (float)this.getAttributeValue(Attributes.ARMOR_TOUGHNESS)
            );
        }

        return damageAmount;
    }

    /**
     * Reduces damage, depending on potions
     */
    protected float getDamageAfterMagicAbsorb(DamageSource damageSource, float damageAmount) {
        if (damageSource.is(DamageTypeTags.BYPASSES_EFFECTS)) {
            return damageAmount;
        } else {
            if (this.hasEffect(MobEffects.RESISTANCE) && !damageSource.is(DamageTypeTags.BYPASSES_RESISTANCE)) {
                int i = (this.getEffect(MobEffects.RESISTANCE).getAmplifier() + 1) * 5;
                int j = 25 - i;
                float f = damageAmount * j;
                float f1 = damageAmount;
                damageAmount = Math.max(f / 25.0F, 0.0F);
                float f2 = f1 - damageAmount;
                if (f2 > 0.0F && f2 < 3.4028235E37F) {
                    this.damageContainers.peek().setReduction(net.neoforged.neoforge.common.damagesource.DamageContainer.Reduction.MOB_EFFECTS, f2);
                    if (this instanceof ServerPlayer) {
                        ((ServerPlayer)this).awardStat(Stats.DAMAGE_RESISTED, Math.round(f2 * 10.0F));
                    } else if (damageSource.getEntity() instanceof ServerPlayer) {
                        ((ServerPlayer)damageSource.getEntity()).awardStat(Stats.DAMAGE_DEALT_RESISTED, Math.round(f2 * 10.0F));
                    }
                }
            }

            if (damageAmount <= 0.0F) {
                return 0.0F;
            } else if (damageSource.is(DamageTypeTags.BYPASSES_ENCHANTMENTS)) {
                return damageAmount;
            } else {
                float f3;
                if (this.level() instanceof ServerLevel serverlevel) {
                    f3 = EnchantmentHelper.getDamageProtection(serverlevel, this, damageSource);
                } else {
                    f3 = 0.0F;
                }

                if (f3 > 0.0F) {
                    damageAmount = CombatRules.getDamageAfterMagicAbsorb(damageAmount, f3);
                    this.damageContainers.peek().setReduction(net.neoforged.neoforge.common.damagesource.DamageContainer.Reduction.ENCHANTMENTS,this.damageContainers.peek().getNewDamage() - damageAmount);
                }

                return damageAmount;
            }
        }
    }

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
            if (f > 0.0F && f < 3.4028235E37F && damageSource.getEntity() instanceof ServerPlayer serverplayer) {
                serverplayer.awardStat(Stats.DAMAGE_DEALT_ABSORBED, Math.round(f * 10.0F));
            }

            if (f1 != 0.0F) {
                this.getCombatTracker().recordDamage(damageSource, f1);
                this.setHealth(this.getHealth() - f1);
                this.gameEvent(GameEvent.ENTITY_DAMAGE);
                this.onDamageTaken(this.damageContainers.peek());
            }
            net.neoforged.neoforge.common.CommonHooks.onLivingDamagePost(this, this.damageContainers.peek());
        }
    }

    public CombatTracker getCombatTracker() {
        return this.combatTracker;
    }

    @Nullable
    public LivingEntity getKillCredit() {
        if (this.lastHurtByPlayer != null) {
            return this.lastHurtByPlayer.getEntity(this.level(), Player.class);
        } else {
            return this.lastHurtByMob != null ? this.lastHurtByMob.getEntity(this.level(), LivingEntity.class) : null;
        }
    }

    public final float getMaxHealth() {
        return (float)this.getAttributeValue(Attributes.MAX_HEALTH);
    }

    public final float getMaxAbsorption() {
        return (float)this.getAttributeValue(Attributes.MAX_ABSORPTION);
    }

    public final int getArrowCount() {
        return this.entityData.get(DATA_ARROW_COUNT_ID);
    }

    /**
     * Sets the amount of arrows stuck in the entity. Used for rendering those.
     */
    public final void setArrowCount(int count) {
        this.entityData.set(DATA_ARROW_COUNT_ID, count);
    }

    public final int getStingerCount() {
        return this.entityData.get(DATA_STINGER_COUNT_ID);
    }

    public final void setStingerCount(int stingerCount) {
        this.entityData.set(DATA_STINGER_COUNT_ID, stingerCount);
    }

    public int getCurrentSwingDuration() {
        if (MobEffectUtil.hasDigSpeed(this)) {
            return 6 - (1 + MobEffectUtil.getDigSpeedAmplification(this));
        } else {
            return this.hasEffect(MobEffects.MINING_FATIGUE) ? 6 + (1 + this.getEffect(MobEffects.MINING_FATIGUE).getAmplifier()) * 2 : 6;
        }
    }

    public void swing(InteractionHand hand) {
        this.swing(hand, false);
    }

    public void swing(InteractionHand hand, boolean updateSelf) {
        ItemStack stack = this.getItemInHand(hand);
        if (!stack.isEmpty() && stack.onEntitySwing(this, hand)) return;
        if (!this.swinging || this.swingTime >= this.getCurrentSwingDuration() / 2 || this.swingTime < 0) {
            this.swingTime = -1;
            this.swinging = true;
            this.swingingArm = hand;
            if (this.level() instanceof ServerLevel) {
                ClientboundAnimatePacket clientboundanimatepacket = new ClientboundAnimatePacket(this, hand == InteractionHand.MAIN_HAND ? 0 : 3);
                ServerChunkCache serverchunkcache = ((ServerLevel)this.level()).getChunkSource();
                if (updateSelf) {
                    serverchunkcache.sendToTrackingPlayersAndSelf(this, clientboundanimatepacket);
                } else {
                    serverchunkcache.sendToTrackingPlayers(this, clientboundanimatepacket);
                }
            }
        }
    }

    @Override
    public void handleDamageEvent(DamageSource damageSource) {
        this.walkAnimation.setSpeed(1.5F);
        this.invulnerableTime = 20;
        this.hurtDuration = 10;
        this.hurtTime = this.hurtDuration;
        SoundEvent soundevent = this.getHurtSound(damageSource);
        if (soundevent != null) {
            this.playSound(soundevent, this.getSoundVolume(), (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.0F);
        }

        this.lastDamageSource = damageSource;
        this.lastDamageStamp = this.level().getGameTime();
    }

    /**
     * Handler for {@link World#setEntityState}
     */
    @Override
    public void handleEntityEvent(byte id) {
        switch (id) {
            case 3:
                SoundEvent soundevent = this.getDeathSound();
                if (soundevent != null) {
                    this.playSound(soundevent, this.getSoundVolume(), (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.0F);
                }

                if (!(this instanceof Player)) {
                    this.setHealth(0.0F);
                    this.die(this.damageSources().generic());
                }
                break;
            case 46:
                int i = 128;

                for (int j = 0; j < 128; j++) {
                    double d0 = j / 127.0;
                    float f = (this.random.nextFloat() - 0.5F) * 0.2F;
                    float f1 = (this.random.nextFloat() - 0.5F) * 0.2F;
                    float f2 = (this.random.nextFloat() - 0.5F) * 0.2F;
                    double d1 = Mth.lerp(d0, this.xo, this.getX()) + (this.random.nextDouble() - 0.5) * this.getBbWidth() * 2.0;
                    double d2 = Mth.lerp(d0, this.yo, this.getY()) + this.random.nextDouble() * this.getBbHeight();
                    double d3 = Mth.lerp(d0, this.zo, this.getZ()) + (this.random.nextDouble() - 0.5) * this.getBbWidth() * 2.0;
                    this.level().addParticle(ParticleTypes.PORTAL, d1, d2, d3, f, f1, f2);
                }
                break;
            case 47:
                this.breakItem(this.getItemBySlot(EquipmentSlot.MAINHAND));
                break;
            case 48:
                this.breakItem(this.getItemBySlot(EquipmentSlot.OFFHAND));
                break;
            case 49:
                this.breakItem(this.getItemBySlot(EquipmentSlot.HEAD));
                break;
            case 50:
                this.breakItem(this.getItemBySlot(EquipmentSlot.CHEST));
                break;
            case 51:
                this.breakItem(this.getItemBySlot(EquipmentSlot.LEGS));
                break;
            case 52:
                this.breakItem(this.getItemBySlot(EquipmentSlot.FEET));
                break;
            case 54:
                HoneyBlock.showJumpParticles(this);
                break;
            case 55:
                this.swapHandItems();
                break;
            case 60:
                this.makePoofParticles();
                break;
            case 65:
                this.breakItem(this.getItemBySlot(EquipmentSlot.BODY));
                break;
            case 67:
                this.makeDrownParticles();
                break;
            case 68:
                this.breakItem(this.getItemBySlot(EquipmentSlot.SADDLE));
                break;
            default:
                super.handleEntityEvent(id);
        }
    }

    public void makePoofParticles() {
        for (int i = 0; i < 20; i++) {
            double d0 = this.random.nextGaussian() * 0.02;
            double d1 = this.random.nextGaussian() * 0.02;
            double d2 = this.random.nextGaussian() * 0.02;
            double d3 = 10.0;
            this.level()
                .addParticle(ParticleTypes.POOF, this.getRandomX(1.0) - d0 * 10.0, this.getRandomY() - d1 * 10.0, this.getRandomZ(1.0) - d2 * 10.0, d0, d1, d2);
        }
    }

    private void makeDrownParticles() {
        Vec3 vec3 = this.getDeltaMovement();

        for (int i = 0; i < 8; i++) {
            double d0 = this.random.triangle(0.0, 1.0);
            double d1 = this.random.triangle(0.0, 1.0);
            double d2 = this.random.triangle(0.0, 1.0);
            this.level().addParticle(ParticleTypes.BUBBLE, this.getX() + d0, this.getY() + d1, this.getZ() + d2, vec3.x, vec3.y, vec3.z);
        }
    }

    private void swapHandItems() {
        ItemStack itemstack = this.getItemBySlot(EquipmentSlot.OFFHAND);
        var event = net.neoforged.neoforge.common.CommonHooks.onLivingSwapHandItems(this);
        if (event.isCanceled()) return;
        this.setItemSlot(EquipmentSlot.OFFHAND, event.getItemSwappedToOffHand());
        this.setItemSlot(EquipmentSlot.MAINHAND, event.getItemSwappedToMainHand());
    }

    @Override
    protected void onBelowWorld() {
        this.hurt(this.damageSources().fellOutOfWorld(), 4.0F);
    }

    protected void updateSwingTime() {
        int i = this.getCurrentSwingDuration();
        if (this.swinging) {
            this.swingTime++;
            if (this.swingTime >= i) {
                this.swingTime = 0;
                this.swinging = false;
            }
        } else {
            this.swingTime = 0;
        }

        this.attackAnim = (float)this.swingTime / i;
    }

    @Nullable
    public AttributeInstance getAttribute(Holder<Attribute> attribute) {
        return this.getAttributes().getInstance(attribute);
    }

    public double getAttributeValue(Holder<Attribute> attribute) {
        return this.getAttributes().getValue(attribute);
    }

    public double getAttributeBaseValue(Holder<Attribute> attribute) {
        return this.getAttributes().getBaseValue(attribute);
    }

    public AttributeMap getAttributes() {
        return this.attributes;
    }

    public ItemStack getMainHandItem() {
        return this.getItemBySlot(EquipmentSlot.MAINHAND);
    }

    public ItemStack getOffhandItem() {
        return this.getItemBySlot(EquipmentSlot.OFFHAND);
    }

    public ItemStack getItemHeldByArm(HumanoidArm arm) {
        return this.getMainArm() == arm ? this.getMainHandItem() : this.getOffhandItem();
    }

    @Nonnull
    @Override
    public ItemStack getWeaponItem() {
        return this.getMainHandItem();
    }

    public boolean isHolding(Item item) {
        return this.isHolding(p_147200_ -> p_147200_.is(item));
    }

    public boolean isHolding(Predicate<ItemStack> predicate) {
        return predicate.test(this.getMainHandItem()) || predicate.test(this.getOffhandItem());
    }

    public ItemStack getItemInHand(InteractionHand hand) {
        if (hand == InteractionHand.MAIN_HAND) {
            return this.getItemBySlot(EquipmentSlot.MAINHAND);
        } else if (hand == InteractionHand.OFF_HAND) {
            return this.getItemBySlot(EquipmentSlot.OFFHAND);
        } else {
            throw new IllegalArgumentException("Invalid hand " + hand);
        }
    }

    public void setItemInHand(InteractionHand hand, ItemStack stack) {
        if (hand == InteractionHand.MAIN_HAND) {
            this.setItemSlot(EquipmentSlot.MAINHAND, stack);
        } else {
            if (hand != InteractionHand.OFF_HAND) {
                throw new IllegalArgumentException("Invalid hand " + hand);
            }

            this.setItemSlot(EquipmentSlot.OFFHAND, stack);
        }
    }

    public boolean hasItemInSlot(EquipmentSlot slot) {
        return !this.getItemBySlot(slot).isEmpty();
    }

    public boolean canUseSlot(EquipmentSlot slot) {
        return true;
    }

    public ItemStack getItemBySlot(EquipmentSlot slot) {
        return this.equipment.get(slot);
    }

    public void setItemSlot(EquipmentSlot slot, ItemStack stack) {
        setItemSlot(slot, stack, false);
    }
    /**
     * Neo: Passing
     * {@code true}
     * for
     * {@code insideTransaction}
     * sets the item without side-effects (calling
     * {@link #onEquipItem}
     * ).
     * 

     * Callers are responsible for performing the deferred side-effects, when the active transaction commits.
     */
    public void setItemSlot(EquipmentSlot slot, ItemStack stack, boolean insideTransaction) {
        var oldStack = this.equipment.set(slot, stack);
        if (!insideTransaction) this.onEquipItem(slot, oldStack, stack);
    }

    public float getArmorCoverPercentage() {
        int i = 0;
        int j = 0;

        for (EquipmentSlot equipmentslot : EquipmentSlotGroup.ARMOR) {
            if (equipmentslot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR) {
                ItemStack itemstack = this.getItemBySlot(equipmentslot);
                if (!itemstack.isEmpty()) {
                    j++;
                }

                i++;
            }
        }

        return i > 0 ? (float)j / i : 0.0F;
    }

    /**
     * Set sprinting switch for Entity.
     */
    @Override
    public void setSprinting(boolean sprinting) {
        super.setSprinting(sprinting);
        AttributeInstance attributeinstance = this.getAttribute(Attributes.MOVEMENT_SPEED);
        attributeinstance.removeModifier(SPEED_MODIFIER_SPRINTING.id());
        if (sprinting) {
            attributeinstance.addTransientModifier(SPEED_MODIFIER_SPRINTING);
        }
    }

    protected float getSoundVolume() {
        return 1.0F;
    }

    public float getVoicePitch() {
        return this.isBaby()
            ? (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.5F
            : (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.0F;
    }

    protected boolean isImmobile() {
        return this.isDeadOrDying();
    }

    /**
     * Applies a velocity to the entities, to push them away from each other.
     */
    @Override
    public void push(Entity entity) {
        if (!this.isSleeping()) {
            super.push(entity);
        }
    }

    private void dismountVehicle(Entity vehicle) {
        Vec3 vec3;
        if (this.isRemoved()) {
            vec3 = this.position();
        } else if (!vehicle.isRemoved() && !this.level().getBlockState(vehicle.blockPosition()).is(BlockTags.PORTALS)) {
            vec3 = vehicle.getDismountLocationForPassenger(this);
        } else {
            double d0 = Math.max(this.getY(), vehicle.getY());
            vec3 = new Vec3(this.getX(), d0, this.getZ());
            boolean flag = this.getBbWidth() <= 4.0F && this.getBbHeight() <= 4.0F;
            if (flag) {
                double d1 = this.getBbHeight() / 2.0;
                Vec3 vec31 = vec3.add(0.0, d1, 0.0);
                VoxelShape voxelshape = Shapes.create(AABB.ofSize(vec31, this.getBbWidth(), this.getBbHeight(), this.getBbWidth()));
                vec3 = this.level()
                    .findFreePosition(this, voxelshape, vec31, this.getBbWidth(), this.getBbHeight(), this.getBbWidth())
                    .map(p_359705_ -> p_359705_.add(0.0, -d1, 0.0))
                    .orElse(vec3);
            }
        }

        this.dismountTo(vec3.x, vec3.y, vec3.z);
    }

    @Override
    public boolean shouldShowName() {
        return this.isCustomNameVisible();
    }

    protected float getJumpPower() {
        return this.getJumpPower(1.0F);
    }

    protected float getJumpPower(float multiplier) {
        return (float)this.getAttributeValue(Attributes.JUMP_STRENGTH) * multiplier * this.getBlockJumpFactor() + this.getJumpBoostPower();
    }

    public float getJumpBoostPower() {
        return this.hasEffect(MobEffects.JUMP_BOOST) ? 0.1F * (this.getEffect(MobEffects.JUMP_BOOST).getAmplifier() + 1.0F) : 0.0F;
    }

    @VisibleForTesting
    public void jumpFromGround() {
        float f = this.getJumpPower();
        if (!(f <= 1.0E-5F)) {
            Vec3 vec3 = this.getDeltaMovement();
            this.setDeltaMovement(vec3.x, Math.max((double)f, vec3.y), vec3.z);
            if (this.isSprinting()) {
                float f1 = this.getYRot() * (float) (Math.PI / 180.0);
                this.addDeltaMovement(new Vec3(-Mth.sin(f1) * 0.2, 0.0, Mth.cos(f1) * 0.2));
            }

            this.hasImpulse = true;
            net.neoforged.neoforge.common.CommonHooks.onLivingJump(this);
        }
    }

    @Deprecated // FORGE: use sinkInFluid instead
    protected void goDownInWater() {
        this.sinkInFluid(net.neoforged.neoforge.common.NeoForgeMod.WATER_TYPE.value());
    }

    @Deprecated // FORGE: use jumpInFluid instead
    protected void jumpInLiquid(TagKey<Fluid> fluidTag) {
        this.setDeltaMovement(this.getDeltaMovement().add(0.0D, (double)0.04F * this.getAttributeValue(net.neoforged.neoforge.common.NeoForgeMod.SWIM_SPEED), 0.0D));
    }

    protected float getWaterSlowDown() {
        return 0.8F;
    }

    public boolean canStandOnFluid(FluidState fluidState) {
        return false;
    }

    @Override
    protected double getDefaultGravity() {
        return this.getAttributeValue(Attributes.GRAVITY);
    }

    protected double getEffectiveGravity() {
        boolean flag = this.getDeltaMovement().y <= 0.0;
        return flag && this.hasEffect(MobEffects.SLOW_FALLING) ? Math.min(this.getGravity(), 0.01) : this.getGravity();
    }

    public void travel(Vec3 travelVector) {
        FluidState fluidstate = this.level().getFluidState(this.blockPosition());
        // Neo: Call (patched-in overload of) #travelInFluid for custom fluid types
        if ((this.isInWater() || this.isInLava() || this.isInFluidType(fluidstate)) && this.isAffectedByFluids() && !this.canStandOnFluid(fluidstate)) {
            this.travelInFluid(travelVector);
        } else if (this.isFallFlying()) {
            this.travelFallFlying(travelVector);
        } else {
            this.travelInAir(travelVector);
        }
    }

    protected void travelFlying(Vec3 relative, float amount) {
        this.travelFlying(relative, 0.02F, 0.02F, amount);
    }

    protected void travelFlying(Vec3 relative, float inWaterAmount, float inLavaAmount, float amount) {
        if (this.isInWater()) {
            this.moveRelative(inWaterAmount, relative);
            this.move(MoverType.SELF, this.getDeltaMovement());
            this.setDeltaMovement(this.getDeltaMovement().scale(0.8F));
        } else if (this.isInLava()) {
            this.moveRelative(inLavaAmount, relative);
            this.move(MoverType.SELF, this.getDeltaMovement());
            this.setDeltaMovement(this.getDeltaMovement().scale(0.5));
        } else {
            this.moveRelative(amount, relative);
            this.move(MoverType.SELF, this.getDeltaMovement());
            this.setDeltaMovement(this.getDeltaMovement().scale(0.91F));
        }
    }

    private void travelInAir(Vec3 travelVector) {
        BlockPos blockpos = this.getBlockPosBelowThatAffectsMyMovement();
        float f = this.onGround() ? this.level().getBlockState(blockpos).getFriction(this.level(), blockpos, this) : 1.0F;
        float f1 = f * 0.91F;
        Vec3 vec3 = this.handleRelativeFrictionAndCalculateMovement(travelVector, f);
        double d0 = vec3.y;
        MobEffectInstance mobeffectinstance = this.getEffect(MobEffects.LEVITATION);
        if (mobeffectinstance != null) {
            d0 += (0.05 * (mobeffectinstance.getAmplifier() + 1) - vec3.y) * 0.2;
        } else if (!this.level().isClientSide() || this.level().hasChunkAt(blockpos)) {
            d0 -= this.getEffectiveGravity();
        } else if (this.getY() > this.level().getMinY()) {
            d0 = -0.1;
        } else {
            d0 = 0.0;
        }

        if (this.shouldDiscardFriction()) {
            this.setDeltaMovement(vec3.x, d0, vec3.z);
        } else {
            float f2 = this instanceof FlyingAnimal ? f1 : 0.98F;
            this.setDeltaMovement(vec3.x * f1, d0 * f2, vec3.z * f1);
        }
    }

    /**
     * @deprecated Neo: use {@link #travelInFluid(Vec3, FluidState)} instead
     */
    @Deprecated
    private void travelInFluid(Vec3 travelVector) {
        travelInFluid(travelVector, net.minecraft.world.level.material.Fluids.EMPTY.defaultFluidState());
    }

    private void travelInFluid(Vec3 travelVector, FluidState fluidState) {
        boolean flag = this.getDeltaMovement().y <= 0.0;
        double d0 = this.getY();
        double d1 = this.getEffectiveGravity();
        // Neo: patched in to use the fluid the eye is in because fluidState is always EMPTY fluid.
        if (this.isInWater() || (this.isInFluidType(this.getEyeInFluidType()) && !this.moveInFluid(this.level().getFluidState(BlockPos.containing(this.getEyePosition())), travelVector, d1))) {
            float f = this.isSprinting() ? 0.9F : this.getWaterSlowDown();
            float f1 = 0.02F;
            float f2 = (float)this.getAttributeValue(Attributes.WATER_MOVEMENT_EFFICIENCY);
            if (!this.onGround()) {
                f2 *= 0.5F;
            }

            if (f2 > 0.0F) {
                f += (0.54600006F - f) * f2;
                f1 += (this.getSpeed() - f1) * f2;
            }

            if (this.hasEffect(MobEffects.DOLPHINS_GRACE)) {
                f = 0.96F;
            }

            f1 *= (float)this.getAttributeValue(net.neoforged.neoforge.common.NeoForgeMod.SWIM_SPEED);
            this.moveRelative(f1, travelVector);
            this.move(MoverType.SELF, this.getDeltaMovement());
            Vec3 vec3 = this.getDeltaMovement();
            if (this.horizontalCollision && this.onClimbable()) {
                vec3 = new Vec3(vec3.x, 0.2, vec3.z);
            }

            vec3 = vec3.multiply(f, 0.8F, f);
            this.setDeltaMovement(this.getFluidFallingAdjustedMovement(d1, flag, vec3));
        } else {
            this.moveRelative(0.02F, travelVector);
            this.move(MoverType.SELF, this.getDeltaMovement());
            if (this.getFluidHeight(FluidTags.LAVA) <= this.getFluidJumpThreshold()) {
                this.setDeltaMovement(this.getDeltaMovement().multiply(0.5, 0.8F, 0.5));
                Vec3 vec31 = this.getFluidFallingAdjustedMovement(d1, flag, this.getDeltaMovement());
                this.setDeltaMovement(vec31);
            } else {
                this.setDeltaMovement(this.getDeltaMovement().scale(0.5));
            }

            if (d1 != 0.0) {
                this.setDeltaMovement(this.getDeltaMovement().add(0.0, -d1 / 4.0, 0.0));
            }
        }

        Vec3 vec32 = this.getDeltaMovement();
        if (this.horizontalCollision && this.isFree(vec32.x, vec32.y + 0.6F - this.getY() + d0, vec32.z)) {
            this.setDeltaMovement(vec32.x, 0.3F, vec32.z);
        }
    }

    private void travelFallFlying(Vec3 travelVector) {
        if (this.onClimbable()) {
            this.travelInAir(travelVector);
            this.stopFallFlying();
        } else {
            Vec3 vec3 = this.getDeltaMovement();
            double d0 = vec3.horizontalDistance();
            this.setDeltaMovement(this.updateFallFlyingMovement(vec3));
            this.move(MoverType.SELF, this.getDeltaMovement());
            if (!this.level().isClientSide()) {
                double d1 = this.getDeltaMovement().horizontalDistance();
                this.handleFallFlyingCollisions(d0, d1);
            }
        }
    }

    public void stopFallFlying() {
        this.setSharedFlag(7, true);
        this.setSharedFlag(7, false);
    }

    private Vec3 updateFallFlyingMovement(Vec3 deltaMovement) {
        Vec3 vec3 = this.getLookAngle();
        float f = this.getXRot() * (float) (Math.PI / 180.0);
        double d0 = Math.sqrt(vec3.x * vec3.x + vec3.z * vec3.z);
        double d1 = deltaMovement.horizontalDistance();
        double d2 = this.getEffectiveGravity();
        double d3 = Mth.square(Math.cos(f));
        deltaMovement = deltaMovement.add(0.0, d2 * (-1.0 + d3 * 0.75), 0.0);
        if (deltaMovement.y < 0.0 && d0 > 0.0) {
            double d4 = deltaMovement.y * -0.1 * d3;
            deltaMovement = deltaMovement.add(vec3.x * d4 / d0, d4, vec3.z * d4 / d0);
        }

        if (f < 0.0F && d0 > 0.0) {
            double d5 = d1 * -Mth.sin(f) * 0.04;
            deltaMovement = deltaMovement.add(-vec3.x * d5 / d0, d5 * 3.2, -vec3.z * d5 / d0);
        }

        if (d0 > 0.0) {
            deltaMovement = deltaMovement.add((vec3.x / d0 * d1 - deltaMovement.x) * 0.1, 0.0, (vec3.z / d0 * d1 - deltaMovement.z) * 0.1);
        }

        return deltaMovement.multiply(0.99F, 0.98F, 0.99F);
    }

    private void handleFallFlyingCollisions(double oldSpeed, double newSpeed) {
        if (this.horizontalCollision) {
            double d0 = oldSpeed - newSpeed;
            float f = (float)(d0 * 10.0 - 3.0);
            if (f > 0.0F) {
                this.playSound(this.getFallDamageSound((int)f), 1.0F, 1.0F);
                this.hurt(this.damageSources().flyIntoWall(), f);
            }
        }
    }

    private void travelRidden(Player player, Vec3 travelVector) {
        Vec3 vec3 = this.getRiddenInput(player, travelVector);
        this.tickRidden(player, vec3);
        if (this.canSimulateMovement()) {
            this.setSpeed(this.getRiddenSpeed(player));
            this.travel(vec3);
        } else {
            this.setDeltaMovement(Vec3.ZERO);
        }
    }

    protected void tickRidden(Player player, Vec3 travelVector) {
    }

    protected Vec3 getRiddenInput(Player player, Vec3 travelVector) {
        return travelVector;
    }

    protected float getRiddenSpeed(Player player) {
        return this.getSpeed();
    }

    public void calculateEntityAnimation(boolean includeHeight) {
        float f = (float)Mth.length(this.getX() - this.xo, includeHeight ? this.getY() - this.yo : 0.0, this.getZ() - this.zo);
        if (!this.isPassenger() && this.isAlive()) {
            this.updateWalkAnimation(f);
        } else {
            this.walkAnimation.stop();
        }
    }

    protected void updateWalkAnimation(float partialTick) {
        float f = Math.min(partialTick * 4.0F, 1.0F);
        this.walkAnimation.update(f, 0.4F, this.isBaby() ? 3.0F : 1.0F);
    }

    private Vec3 handleRelativeFrictionAndCalculateMovement(Vec3 deltaMovement, float friction) {
        this.moveRelative(this.getFrictionInfluencedSpeed(friction), deltaMovement);
        this.setDeltaMovement(this.handleOnClimbable(this.getDeltaMovement()));
        this.move(MoverType.SELF, this.getDeltaMovement());
        Vec3 vec3 = this.getDeltaMovement();
        if ((this.horizontalCollision || this.jumping) && (this.onClimbable() || this.wasInPowderSnow && PowderSnowBlock.canEntityWalkOnPowderSnow(this))) {
            vec3 = new Vec3(vec3.x, 0.2, vec3.z);
        }

        return vec3;
    }

    public Vec3 getFluidFallingAdjustedMovement(double gravity, boolean isFalling, Vec3 deltaMovement) {
        if (gravity != 0.0 && !this.isSprinting()) {
            double d0;
            if (isFalling && Math.abs(deltaMovement.y - 0.005) >= 0.003 && Math.abs(deltaMovement.y - gravity / 16.0) < 0.003) {
                d0 = -0.003;
            } else {
                d0 = deltaMovement.y - gravity / 16.0;
            }

            return new Vec3(deltaMovement.x, d0, deltaMovement.z);
        } else {
            return deltaMovement;
        }
    }

    private Vec3 handleOnClimbable(Vec3 deltaMovement) {
        if (this.onClimbable()) {
            this.resetFallDistance();
            float f = 0.15F;
            double d0 = Mth.clamp(deltaMovement.x, -0.15F, 0.15F);
            double d1 = Mth.clamp(deltaMovement.z, -0.15F, 0.15F);
            double d2 = Math.max(deltaMovement.y, -0.15F);
            if (d2 < 0.0D && !this.getInBlockState().isScaffolding(this) && this.isSuppressingSlidingDownLadder() && this instanceof Player) {
                d2 = 0.0;
            }

            deltaMovement = new Vec3(d0, d2, d1);
        }

        return deltaMovement;
    }

    private float getFrictionInfluencedSpeed(float friction) {
        return this.onGround() ? this.getSpeed() * (0.21600002F / (friction * friction * friction)) : this.getFlyingSpeed();
    }

    protected float getFlyingSpeed() {
        return this.getControllingPassenger() instanceof Player ? this.getSpeed() * 0.1F : 0.02F;
    }

    public float getSpeed() {
        return this.speed;
    }

    /**
     * Sets the movespeed used for the new AI system.
     */
    public void setSpeed(float speed) {
        this.speed = speed;
    }

    public boolean doHurtTarget(ServerLevel level, Entity source) {
        this.setLastHurtMob(source);
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        this.updatingUsingItem();
        this.updateSwimAmount();
        if (!this.level().isClientSide()) {
            int i = this.getArrowCount();
            if (i > 0) {
                if (this.removeArrowTime <= 0) {
                    this.removeArrowTime = 20 * (30 - i);
                }

                this.removeArrowTime--;
                if (this.removeArrowTime <= 0) {
                    this.setArrowCount(i - 1);
                }
            }

            int j = this.getStingerCount();
            if (j > 0) {
                if (this.removeStingerTime <= 0) {
                    this.removeStingerTime = 20 * (30 - j);
                }

                this.removeStingerTime--;
                if (this.removeStingerTime <= 0) {
                    this.setStingerCount(j - 1);
                }
            }

            this.detectEquipmentUpdates();
            if (this.tickCount % 20 == 0) {
                this.getCombatTracker().recheckStatus();
            }

            if (this.isSleeping() && (!this.canInteractWithLevel() || !this.checkBedExists())) {
                this.stopSleeping();
            }
        }

        if (!this.isRemoved()) {
            this.aiStep();
        }

        double d1 = this.getX() - this.xo;
        double d0 = this.getZ() - this.zo;
        float f = (float)(d1 * d1 + d0 * d0);
        float f1 = this.yBodyRot;
        if (f > 0.0025000002F) {
            float f2 = (float)Mth.atan2(d0, d1) * (180.0F / (float)Math.PI) - 90.0F;
            float f3 = Mth.abs(Mth.wrapDegrees(this.getYRot()) - f2);
            if (95.0F < f3 && f3 < 265.0F) {
                f1 = f2 - 180.0F;
            } else {
                f1 = f2;
            }
        }

        if (this.attackAnim > 0.0F) {
            f1 = this.getYRot();
        }

        ProfilerFiller profilerfiller = Profiler.get();
        profilerfiller.push("headTurn");
        this.tickHeadTurn(f1);
        profilerfiller.pop();
        profilerfiller.push("rangeChecks");

        while (this.getYRot() - this.yRotO < -180.0F) {
            this.yRotO -= 360.0F;
        }

        while (this.getYRot() - this.yRotO >= 180.0F) {
            this.yRotO += 360.0F;
        }

        while (this.yBodyRot - this.yBodyRotO < -180.0F) {
            this.yBodyRotO -= 360.0F;
        }

        while (this.yBodyRot - this.yBodyRotO >= 180.0F) {
            this.yBodyRotO += 360.0F;
        }

        while (this.getXRot() - this.xRotO < -180.0F) {
            this.xRotO -= 360.0F;
        }

        while (this.getXRot() - this.xRotO >= 180.0F) {
            this.xRotO += 360.0F;
        }

        while (this.yHeadRot - this.yHeadRotO < -180.0F) {
            this.yHeadRotO -= 360.0F;
        }

        while (this.yHeadRot - this.yHeadRotO >= 180.0F) {
            this.yHeadRotO += 360.0F;
        }

        profilerfiller.pop();
        if (this.isFallFlying()) {
            this.fallFlyTicks++;
        } else {
            this.fallFlyTicks = 0;
        }

        if (this.isSleeping()) {
            this.setXRot(0.0F);
        }

        this.refreshDirtyAttributes();
        this.elytraAnimationState.tick();
    }

    private void detectEquipmentUpdates() {
        Map<EquipmentSlot, ItemStack> map = this.collectEquipmentChanges();
        if (map != null) {
            this.handleHandSwap(map);
            if (!map.isEmpty()) {
                this.handleEquipmentChanges(map);
            }
        }
    }

    @Nullable
    private Map<EquipmentSlot, ItemStack> collectEquipmentChanges() {
        Map<EquipmentSlot, ItemStack> map = null;

        for (EquipmentSlot equipmentslot : EquipmentSlot.VALUES) {
            ItemStack itemstack = this.lastEquipmentItems.get(equipmentslot);
            ItemStack itemstack1 = this.getItemBySlot(equipmentslot);
            if (this.equipmentHasChanged(itemstack, itemstack1)) {
                net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent(this, equipmentslot, itemstack, itemstack1));
                if (map == null) {
                    map = Maps.newEnumMap(EquipmentSlot.class);
                }

                map.put(equipmentslot, itemstack1);
                AttributeMap attributemap = this.getAttributes();
                if (!itemstack.isEmpty()) {
                    this.stopLocationBasedEffects(itemstack, equipmentslot, attributemap);
                }
            }
        }

        if (map != null) {
            for (Entry<EquipmentSlot, ItemStack> entry : map.entrySet()) {
                EquipmentSlot equipmentslot1 = entry.getKey();
                ItemStack itemstack2 = entry.getValue();
                if (!itemstack2.isEmpty() && !itemstack2.isBroken()) {
                    itemstack2.forEachModifier(equipmentslot1, (p_359701_, p_359702_) -> {
                        AttributeInstance attributeinstance = this.attributes.getInstance(p_359701_);
                        if (attributeinstance != null) {
                            attributeinstance.removeModifier(p_359702_.id());
                            attributeinstance.addTransientModifier(p_359702_);
                        }
                    });
                    if (this.level() instanceof ServerLevel serverlevel) {
                        EnchantmentHelper.runLocationChangedEffects(serverlevel, itemstack2, this, equipmentslot1);
                    }
                }
            }
        }

        return map;
    }

    public boolean equipmentHasChanged(ItemStack oldItem, ItemStack newItem) {
        return !ItemStack.matches(newItem, oldItem);
    }

    private void handleHandSwap(Map<EquipmentSlot, ItemStack> hands) {
        ItemStack itemstack = hands.get(EquipmentSlot.MAINHAND);
        ItemStack itemstack1 = hands.get(EquipmentSlot.OFFHAND);
        if (itemstack != null
            && itemstack1 != null
            && ItemStack.matches(itemstack, this.lastEquipmentItems.get(EquipmentSlot.OFFHAND))
            && ItemStack.matches(itemstack1, this.lastEquipmentItems.get(EquipmentSlot.MAINHAND))) {
            ((ServerLevel)this.level()).getChunkSource().sendToTrackingPlayers(this, new ClientboundEntityEventPacket(this, (byte)55));
            hands.remove(EquipmentSlot.MAINHAND);
            hands.remove(EquipmentSlot.OFFHAND);
            this.lastEquipmentItems.put(EquipmentSlot.MAINHAND, itemstack.copy());
            this.lastEquipmentItems.put(EquipmentSlot.OFFHAND, itemstack1.copy());
        }
    }

    private void handleEquipmentChanges(Map<EquipmentSlot, ItemStack> equipments) {
        List<Pair<EquipmentSlot, ItemStack>> list = Lists.newArrayListWithCapacity(equipments.size());
        equipments.forEach((p_396675_, p_396676_) -> {
            ItemStack itemstack = p_396676_.copy();
            list.add(Pair.of(p_396675_, itemstack));
            this.lastEquipmentItems.put(p_396675_, itemstack);
        });
        ((ServerLevel)this.level()).getChunkSource().sendToTrackingPlayers(this, new ClientboundSetEquipmentPacket(this.getId(), list));
    }

    protected void tickHeadTurn(float yBodyRot) {
        float f = Mth.wrapDegrees(yBodyRot - this.yBodyRot);
        this.yBodyRot += f * 0.3F;
        float f1 = Mth.wrapDegrees(this.getYRot() - this.yBodyRot);
        float f2 = this.getMaxHeadRotationRelativeToBody();
        if (Math.abs(f1) > f2) {
            this.yBodyRot = this.yBodyRot + (f1 - Mth.sign(f1) * f2);
        }
    }

    protected float getMaxHeadRotationRelativeToBody() {
        return 50.0F;
    }

    public void aiStep() {
        if (this.noJumpDelay > 0) {
            this.noJumpDelay--;
        }

        if (this.isInterpolating()) {
            this.getInterpolation().interpolate();
        } else if (!this.canSimulateMovement()) {
            this.setDeltaMovement(this.getDeltaMovement().scale(0.98));
        }

        if (this.lerpHeadSteps > 0) {
            this.lerpHeadRotationStep(this.lerpHeadSteps, this.lerpYHeadRot);
            this.lerpHeadSteps--;
        }

        this.equipment.tick(this);
        Vec3 vec3 = this.getDeltaMovement();
        double d0 = vec3.x;
        double d1 = vec3.y;
        double d2 = vec3.z;
        if (this.getType().equals(EntityType.PLAYER)) {
            if (vec3.horizontalDistanceSqr() < 9.0E-6) {
                d0 = 0.0;
                d2 = 0.0;
            }
        } else {
            if (Math.abs(vec3.x) < 0.003) {
                d0 = 0.0;
            }

            if (Math.abs(vec3.z) < 0.003) {
                d2 = 0.0;
            }
        }

        if (Math.abs(vec3.y) < 0.003) {
            d1 = 0.0;
        }

        this.setDeltaMovement(d0, d1, d2);
        ProfilerFiller profilerfiller = Profiler.get();
        profilerfiller.push("ai");
        this.applyInput();
        if (this.isImmobile()) {
            this.jumping = false;
            this.xxa = 0.0F;
            this.zza = 0.0F;
        } else if (this.isEffectiveAi() && !this.level().isClientSide()) {
            profilerfiller.push("newAi");
            this.serverAiStep();
            profilerfiller.pop();
        }

        profilerfiller.pop();
        profilerfiller.push("jump");
        if (this.jumping && this.isAffectedByFluids()) {
            double d3;
            net.neoforged.neoforge.fluids.FluidType fluidType = this.getMaxHeightFluidType();
            if (!fluidType.isAir()) d3 = this.getFluidTypeHeight(fluidType);
            else
            if (this.isInLava()) {
                d3 = this.getFluidHeight(FluidTags.LAVA);
            } else {
                d3 = this.getFluidHeight(FluidTags.WATER);
            }

            boolean flag = this.isInWater() && d3 > 0.0;
            double d4 = this.getFluidJumpThreshold();
            if (!flag || this.onGround() && !(d3 > d4)) {
                if (!this.isInLava() || this.onGround() && !(d3 > d4)) {
                    if (fluidType.isAir() || this.onGround() && !(d3 > d4)) {
                    if ((this.onGround() || flag && d3 <= d4) && this.noJumpDelay == 0) {
                        this.jumpFromGround();
                        this.noJumpDelay = 10;
                    }
                    } else this.jumpInFluid(fluidType);
                } else {
                    this.jumpInFluid(net.neoforged.neoforge.common.NeoForgeMod.LAVA_TYPE.value());
                }
            } else {
                this.jumpInFluid(net.neoforged.neoforge.common.NeoForgeMod.WATER_TYPE.value());
            }
        } else {
            this.noJumpDelay = 0;
        }

        profilerfiller.pop();
        profilerfiller.push("travel");
        if (this.isFallFlying()) {
            this.updateFallFlying();
        }

        AABB aabb = this.getBoundingBox();
        Vec3 vec31 = new Vec3(this.xxa, this.yya, this.zza);
        if (this.hasEffect(MobEffects.SLOW_FALLING) || this.hasEffect(MobEffects.LEVITATION)) {
            this.resetFallDistance();
        }

        if (this.getControllingPassenger() instanceof Player player && this.isAlive()) {
            this.travelRidden(player, vec31);
        } else if (this.canSimulateMovement() && this.isEffectiveAi()) {
            this.travel(vec31);
        }

        if (!this.level().isClientSide() || this.isLocalInstanceAuthoritative()) {
            this.applyEffectsFromBlocks();
        }

        if (this.level().isClientSide()) {
            this.calculateEntityAnimation(this instanceof FlyingAnimal);
        }

        profilerfiller.pop();
        if (this.level() instanceof ServerLevel serverlevel) {
            profilerfiller.push("freezing");
            if (!this.isInPowderSnow || !this.canFreeze()) {
                this.setTicksFrozen(Math.max(0, this.getTicksFrozen() - 2));
            }

            this.removeFrost();
            this.tryAddFrost();
            if (this.tickCount % 40 == 0 && this.isFullyFrozen() && this.canFreeze()) {
                this.hurtServer(serverlevel, this.damageSources().freeze(), 1.0F);
            }

            profilerfiller.pop();
        }

        profilerfiller.push("push");
        if (this.autoSpinAttackTicks > 0) {
            this.autoSpinAttackTicks--;
            this.checkAutoSpinAttack(aabb, this.getBoundingBox());
        }

        this.pushEntities();
        profilerfiller.pop();
        if (this.level() instanceof ServerLevel serverlevel1 && this.isSensitiveToWater() && this.isInWaterOrRain()) {
            this.hurtServer(serverlevel1, this.damageSources().drown(), 1.0F);
        }
    }

    protected void applyInput() {
        this.xxa *= 0.98F;
        this.zza *= 0.98F;
    }

    public boolean isSensitiveToWater() {
        return false;
    }

    public boolean isJumping() {
        return this.jumping;
    }

    protected void updateFallFlying() {
        this.checkFallDistanceAccumulation();
        if (!this.level().isClientSide()) {
            if (!this.canGlide()) {
                this.setSharedFlag(7, false);
                return;
            }

            int i = this.fallFlyTicks + 1;
            if (i % 10 == 0) {
                int j = i / 10;
                if (j % 2 == 0) {
                    List<EquipmentSlot> list = EquipmentSlot.VALUES
                        .stream()
                        .filter(p_370507_ -> canGlideUsing(this.getItemBySlot(p_370507_), p_370507_))
                        .toList();
                    EquipmentSlot equipmentslot = Util.getRandom(list, this.random);
                    this.getItemBySlot(equipmentslot).hurtAndBreak(1, this, equipmentslot);
                }

                this.gameEvent(GameEvent.ELYTRA_GLIDE);
            }
        }
    }

    protected boolean canGlide() {
        if (!this.onGround() && !this.isPassenger() && !this.hasEffect(MobEffects.LEVITATION)) {
            for (EquipmentSlot equipmentslot : EquipmentSlot.VALUES) {
                if (canGlideUsing(this.getItemBySlot(equipmentslot), equipmentslot)) {
                    return true;
                }
            }

            return false;
        } else {
            return false;
        }
    }

    protected void serverAiStep() {
    }

    protected void pushEntities() {
        List<Entity> list = this.level().getPushableEntities(this, this.getBoundingBox());
        if (!list.isEmpty()) {
            if (this.level() instanceof ServerLevel serverlevel) {
                int j = serverlevel.getGameRules().getInt(GameRules.RULE_MAX_ENTITY_CRAMMING);
                if (j > 0 && list.size() > j - 1 && this.random.nextInt(4) == 0) {
                    int i = 0;

                    for (Entity entity : list) {
                        if (!entity.isPassenger()) {
                            i++;
                        }
                    }

                    if (i > j - 1) {
                        this.hurtServer(serverlevel, this.damageSources().cramming(), 6.0F);
                    }
                }
            }

            for (Entity entity1 : list) {
                this.doPush(entity1);
            }
        }
    }

    protected void checkAutoSpinAttack(AABB boundingBoxBeforeSpin, AABB boundingBoxAfterSpin) {
        AABB aabb = boundingBoxBeforeSpin.minmax(boundingBoxAfterSpin);
        List<Entity> list = this.level().getEntities(this, aabb);
        if (!list.isEmpty()) {
            for (Entity entity : list) {
                if (entity instanceof LivingEntity) {
                    this.doAutoAttackOnTouch((LivingEntity)entity);
                    this.autoSpinAttackTicks = 0;
                    this.setDeltaMovement(this.getDeltaMovement().scale(-0.2));
                    break;
                }
            }
        } else if (this.horizontalCollision) {
            this.autoSpinAttackTicks = 0;
        }

        if (!this.level().isClientSide() && this.autoSpinAttackTicks <= 0) {
            this.setLivingEntityFlag(4, false);
            this.autoSpinAttackDmg = 0.0F;
            this.autoSpinAttackItemStack = null;
        }
    }

    protected void doPush(Entity entity) {
        entity.push(this);
    }

    protected void doAutoAttackOnTouch(LivingEntity target) {
    }

    public boolean isAutoSpinAttack() {
        return (this.entityData.get(DATA_LIVING_ENTITY_FLAGS) & 4) != 0;
    }

    @Override
    public void stopRiding() {
        Entity entity = this.getVehicle();
        super.stopRiding();
        if (entity != null && entity != this.getVehicle() && !this.level().isClientSide()) {
            this.dismountVehicle(entity);
        }
    }

    @Override
    public void rideTick() {
        super.rideTick();
        this.resetFallDistance();
    }

    @Override
    public InterpolationHandler getInterpolation() {
        return this.interpolation;
    }

    @Override
    public void lerpHeadTo(float yaw, int pitch) {
        this.lerpYHeadRot = yaw;
        this.lerpHeadSteps = pitch;
    }

    public void setJumping(boolean jumping) {
        this.jumping = jumping;
    }

    public void onItemPickup(ItemEntity itemEntity) {
        Entity entity = itemEntity.getOwner();
        if (entity instanceof ServerPlayer) {
            CriteriaTriggers.THROWN_ITEM_PICKED_UP_BY_ENTITY.trigger((ServerPlayer)entity, itemEntity.getItem(), this);
        }
    }

    /**
     * Called when the entity picks up an item.
     */
    public void take(Entity entity, int amount) {
        if (!entity.isRemoved()
            && !this.level().isClientSide()
            && (entity instanceof ItemEntity || entity instanceof AbstractArrow || entity instanceof ExperienceOrb)) {
            ((ServerLevel)this.level())
                .getChunkSource()
                .sendToTrackingPlayers(entity, new ClientboundTakeItemEntityPacket(entity.getId(), this.getId(), amount));
        }
    }

    public boolean hasLineOfSight(Entity entity) {
        return this.hasLineOfSight(entity, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, entity.getEyeY());
    }

    public boolean hasLineOfSight(Entity entity, ClipContext.Block block, ClipContext.Fluid fluid, double y) {
        if (entity.level() != this.level()) {
            return false;
        } else {
            Vec3 vec3 = new Vec3(this.getX(), this.getEyeY(), this.getZ());
            Vec3 vec31 = new Vec3(entity.getX(), y, entity.getZ());
            return vec31.distanceTo(vec3) > 128.0
                ? false
                : this.level().clip(new ClipContext(vec3, vec31, block, fluid, this)).getType() == HitResult.Type.MISS;
        }
    }

    /**
     * Gets the current yaw of the entity
     */
    @Override
    public float getViewYRot(float partialTicks) {
        return partialTicks == 1.0F ? this.yHeadRot : Mth.rotLerp(partialTicks, this.yHeadRotO, this.yHeadRot);
    }

    /**
     * Gets the progression of the swing animation, ranges from 0.0 to 1.0.
     */
    public float getAttackAnim(float partialTick) {
        float f = this.attackAnim - this.oAttackAnim;
        if (f < 0.0F) {
            f++;
        }

        return this.oAttackAnim + f * partialTick;
    }

    @Override
    public boolean isPickable() {
        return !this.isRemoved();
    }

    @Override
    public boolean isPushable() {
        return this.isAlive() && !this.isSpectator() && !this.onClimbable();
    }

    @Override
    public float getYHeadRot() {
        return this.yHeadRot;
    }

    /**
     * Sets the head's yaw rotation of the entity.
     */
    @Override
    public void setYHeadRot(float rotation) {
        this.yHeadRot = rotation;
    }

    /**
     * Set the render yaw offset
     */
    @Override
    public void setYBodyRot(float offset) {
        this.yBodyRot = offset;
    }

    @Override
    public Vec3 getRelativePortalPosition(Direction.Axis axis, BlockUtil.FoundRectangle portal) {
        return resetForwardDirectionOfRelativePortalPosition(super.getRelativePortalPosition(axis, portal));
    }

    public static Vec3 resetForwardDirectionOfRelativePortalPosition(Vec3 relativePortalPosition) {
        return new Vec3(relativePortalPosition.x, relativePortalPosition.y, 0.0);
    }

    public float getAbsorptionAmount() {
        return this.absorptionAmount;
    }

    public final void setAbsorptionAmount(float absorptionAmount) {
        this.internalSetAbsorptionAmount(Mth.clamp(absorptionAmount, 0.0F, this.getMaxAbsorption()));
    }

    protected void internalSetAbsorptionAmount(float absorptionAmount) {
        this.absorptionAmount = absorptionAmount;
    }

    public void onEnterCombat() {
    }

    public void onLeaveCombat() {
    }

    protected void updateEffectVisibility() {
        this.effectsDirty = true;
    }

    public abstract HumanoidArm getMainArm();

    public boolean isUsingItem() {
        return (this.entityData.get(DATA_LIVING_ENTITY_FLAGS) & 1) > 0;
    }

    public InteractionHand getUsedItemHand() {
        return (this.entityData.get(DATA_LIVING_ENTITY_FLAGS) & 2) > 0 ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
    }

    private void updatingUsingItem() {
        if (this.isUsingItem()) {
            ItemStack itemStack = this.getItemInHand(this.getUsedItemHand());
            if (net.neoforged.neoforge.common.CommonHooks.canContinueUsing(this.useItem, itemStack)) {
                this.useItem = itemStack;
            }
            if (itemStack == this.useItem) {
                this.updateUsingItem(this.useItem);
            } else {
                this.stopUsingItem();
            }
        }
    }

    @Nullable
    private ItemEntity createItemStackToDrop(ItemStack stack, boolean randomizeMotion, boolean includeThrower) {
        if (stack.isEmpty()) {
            return null;
        } else {
            double d0 = this.getEyeY() - 0.3F;
            ItemEntity itementity = new ItemEntity(this.level(), this.getX(), d0, this.getZ(), stack);
            itementity.setPickUpDelay(40);
            if (includeThrower) {
                itementity.setThrower(this);
            }

            if (randomizeMotion) {
                float f = this.random.nextFloat() * 0.5F;
                float f1 = this.random.nextFloat() * (float) (Math.PI * 2);
                itementity.setDeltaMovement(-Mth.sin(f1) * f, 0.2F, Mth.cos(f1) * f);
            } else {
                float f7 = 0.3F;
                float f8 = Mth.sin(this.getXRot() * (float) (Math.PI / 180.0));
                float f2 = Mth.cos(this.getXRot() * (float) (Math.PI / 180.0));
                float f3 = Mth.sin(this.getYRot() * (float) (Math.PI / 180.0));
                float f4 = Mth.cos(this.getYRot() * (float) (Math.PI / 180.0));
                float f5 = this.random.nextFloat() * (float) (Math.PI * 2);
                float f6 = 0.02F * this.random.nextFloat();
                itementity.setDeltaMovement(
                    -f3 * f2 * 0.3F + Math.cos(f5) * f6,
                    -f8 * 0.3F + 0.1F + (this.random.nextFloat() - this.random.nextFloat()) * 0.1F,
                    f4 * f2 * 0.3F + Math.sin(f5) * f6
                );
            }

            return itementity;
        }
    }

    protected void updateUsingItem(ItemStack usingItem) {
        if (!usingItem.isEmpty())
            this.useItemRemaining = net.neoforged.neoforge.event.EventHooks.onItemUseTick(this, usingItem, this.getUseItemRemainingTicks());
        if (this.getUseItemRemainingTicks() > 0)
        usingItem.onUseTick(this.level(), this, this.getUseItemRemainingTicks());
        if (--this.useItemRemaining <= 0 && !this.level().isClientSide() && !usingItem.useOnRelease()) {
            this.completeUsingItem();
        }
    }

    private void updateSwimAmount() {
        this.swimAmountO = this.swimAmount;
        if (this.isVisuallySwimming()) {
            this.swimAmount = Math.min(1.0F, this.swimAmount + 0.09F);
        } else {
            this.swimAmount = Math.max(0.0F, this.swimAmount - 0.09F);
        }
    }

    protected void setLivingEntityFlag(int key, boolean value) {
        int i = this.entityData.get(DATA_LIVING_ENTITY_FLAGS);
        if (value) {
            i |= key;
        } else {
            i &= ~key;
        }

        this.entityData.set(DATA_LIVING_ENTITY_FLAGS, (byte)i);
    }

    public void startUsingItem(InteractionHand hand) {
        ItemStack itemstack = this.getItemInHand(hand);
        if (!itemstack.isEmpty() && !this.isUsingItem()) {
            int duration = net.neoforged.neoforge.event.EventHooks.onItemUseStart(this, itemstack, hand, itemstack.getUseDuration(this));
            if (duration < 0) return; // Neo: Early return for negative values, as that indicates event cancellation.
            this.useItem = itemstack;
            this.useItemRemaining = duration;
            if (!this.level().isClientSide()) {
                this.setLivingEntityFlag(1, true);
                this.setLivingEntityFlag(2, hand == InteractionHand.OFF_HAND);
                this.gameEvent(GameEvent.ITEM_INTERACT_START);
            }
        }
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (SLEEPING_POS_ID.equals(key)) {
            if (this.level().isClientSide()) {
                this.getSleepingPos().ifPresent(this::setPosToBed);
            }
        } else if (DATA_LIVING_ENTITY_FLAGS.equals(key) && this.level().isClientSide()) {
            if (this.isUsingItem() && this.useItem.isEmpty()) {
                this.useItem = this.getItemInHand(this.getUsedItemHand());
                if (!this.useItem.isEmpty()) {
                    this.useItemRemaining = this.useItem.getUseDuration(this);
                }
            } else if (!this.isUsingItem() && !this.useItem.isEmpty()) {
                this.useItem = ItemStack.EMPTY;
                this.useItemRemaining = 0;
            }
        }
    }

    @Override
    public void lookAt(EntityAnchorArgument.Anchor anchor, Vec3 target) {
        super.lookAt(anchor, target);
        this.yHeadRotO = this.yHeadRot;
        this.yBodyRot = this.yHeadRot;
        this.yBodyRotO = this.yBodyRot;
    }

    @Override
    public float getPreciseBodyRotation(float partialTick) {
        return Mth.lerp(partialTick, this.yBodyRotO, this.yBodyRot);
    }

    public void spawnItemParticles(ItemStack stack, int amount) {
        for (int i = 0; i < amount; i++) {
            Vec3 vec3 = new Vec3((this.random.nextFloat() - 0.5) * 0.1, Math.random() * 0.1 + 0.1, 0.0);
            vec3 = vec3.xRot(-this.getXRot() * (float) (Math.PI / 180.0));
            vec3 = vec3.yRot(-this.getYRot() * (float) (Math.PI / 180.0));
            double d0 = -this.random.nextFloat() * 0.6 - 0.3;
            Vec3 vec31 = new Vec3((this.random.nextFloat() - 0.5) * 0.3, d0, 0.6);
            vec31 = vec31.xRot(-this.getXRot() * (float) (Math.PI / 180.0));
            vec31 = vec31.yRot(-this.getYRot() * (float) (Math.PI / 180.0));
            vec31 = vec31.add(this.getX(), this.getEyeY(), this.getZ());
            this.level().addParticle(new ItemParticleOption(ParticleTypes.ITEM, stack), vec31.x, vec31.y, vec31.z, vec3.x, vec3.y + 0.05, vec3.z);
        }
    }

    protected void completeUsingItem() {
        if (!this.level().isClientSide() || this.isUsingItem()) {
            InteractionHand interactionhand = this.getUsedItemHand();
            if (!this.useItem.equals(this.getItemInHand(interactionhand))) {
                this.releaseUsingItem();
            } else {
                if (!this.useItem.isEmpty() && this.isUsingItem()) {
                    ItemStack copy = this.useItem.copy();
                    ItemStack itemstack = net.neoforged.neoforge.event.EventHooks.onItemUseFinish(this, copy, getUseItemRemainingTicks(), this.useItem.finishUsingItem(this.level(), this));
                    if (itemstack != this.useItem) {
                        this.setItemInHand(interactionhand, itemstack);
                    }

                    this.stopUsingItem();
                }
            }
        }
    }

    public void handleExtraItemsCreatedOnUse(ItemStack stack) {
    }

    public ItemStack getUseItem() {
        return this.useItem;
    }

    public int getUseItemRemainingTicks() {
        return this.useItemRemaining;
    }

    public int getTicksUsingItem() {
        return this.isUsingItem() ? this.useItem.getUseDuration(this) - this.getUseItemRemainingTicks() : 0;
    }

    public void releaseUsingItem() {
        ItemStack itemstack = this.getItemInHand(this.getUsedItemHand());
        if (!this.useItem.isEmpty() && ItemStack.isSameItem(itemstack, this.useItem)) {
            this.useItem = itemstack;
            if (!net.neoforged.neoforge.event.EventHooks.onUseItemStop(this, useItem, this.getUseItemRemainingTicks())) {
            ItemStack copy = this instanceof Player ? useItem.copy() : null;
            this.useItem.releaseUsing(this.level(), this, this.getUseItemRemainingTicks());
            if (copy != null && useItem.isEmpty()) net.neoforged.neoforge.event.EventHooks.onPlayerDestroyItem((Player)this, copy, getUsedItemHand());
            }
            if (this.useItem.useOnRelease()) {
                this.updatingUsingItem();
            }
        }

        this.stopUsingItem();
    }

    public void stopUsingItem() {
        if (this.isUsingItem() && !this.useItem.isEmpty()) this.useItem.onStopUsing(this, useItemRemaining);
        if (!this.level().isClientSide()) {
            boolean flag = this.isUsingItem();
            this.setLivingEntityFlag(1, false);
            if (flag) {
                this.gameEvent(GameEvent.ITEM_INTERACT_FINISH);
            }
        }

        this.useItem = ItemStack.EMPTY;
        this.useItemRemaining = 0;
    }

    public boolean isBlocking() {
        return this.getItemBlockingWith() != null;
    }

    @Nullable
    public ItemStack getItemBlockingWith() {
        if (!this.isUsingItem()) {
            return null;
        } else {
            BlocksAttacks blocksattacks = this.useItem.get(DataComponents.BLOCKS_ATTACKS);
            if (blocksattacks != null) {
                int i = this.useItem.getItem().getUseDuration(this.useItem, this) - this.useItemRemaining;
                if (i >= blocksattacks.blockDelayTicks()) {
                    return this.useItem;
                }
            }

            return null;
        }
    }

    public boolean isSuppressingSlidingDownLadder() {
        return this.isShiftKeyDown();
    }

    public boolean isFallFlying() {
        return this.getSharedFlag(7);
    }

    @Override
    public boolean isVisuallySwimming() {
        return super.isVisuallySwimming() || !this.isFallFlying() && this.hasPose(Pose.FALL_FLYING);
    }

    public int getFallFlyingTicks() {
        return this.fallFlyTicks;
    }

    public boolean randomTeleport(double x, double y, double z, boolean broadcastTeleport) {
        double d0 = this.getX();
        double d1 = this.getY();
        double d2 = this.getZ();
        double d3 = y;
        boolean flag = false;
        BlockPos blockpos = BlockPos.containing(x, y, z);
        Level level = this.level();
        if (level.hasChunkAt(blockpos)) {
            boolean flag1 = false;

            while (!flag1 && blockpos.getY() > level.getMinY()) {
                BlockPos blockpos1 = blockpos.below();
                BlockState blockstate = level.getBlockState(blockpos1);
                if (blockstate.blocksMotion()) {
                    flag1 = true;
                } else {
                    d3--;
                    blockpos = blockpos1;
                }
            }

            if (flag1) {
                this.teleportTo(x, d3, z);
                if (level.noCollision(this) && !level.containsAnyLiquid(this.getBoundingBox())) {
                    flag = true;
                }
            }
        }

        if (!flag) {
            this.teleportTo(d0, d1, d2);
            return false;
        } else {
            if (broadcastTeleport) {
                level.broadcastEntityEvent(this, (byte)46);
            }

            if (this instanceof PathfinderMob pathfindermob) {
                pathfindermob.getNavigation().stop();
            }

            return true;
        }
    }

    public boolean isAffectedByPotions() {
        return !this.isDeadOrDying();
    }

    public boolean attackable() {
        return true;
    }

    /**
     * Called when a record starts or stops playing. Used to make parrots start or stop partying.
     */
    public void setRecordPlayingNearby(BlockPos jukebox, boolean partyParrot) {
    }

    public boolean canPickUpLoot() {
        return false;
    }

    @Override
    public final EntityDimensions getDimensions(Pose pose) {
        return pose == Pose.SLEEPING ? SLEEPING_DIMENSIONS : this.getDefaultDimensions(pose).scale(this.getScale());
    }

    protected EntityDimensions getDefaultDimensions(Pose pose) {
        return this.getType().getDimensions().scale(this.getAgeScale());
    }

    public ImmutableList<Pose> getDismountPoses() {
        return ImmutableList.of(Pose.STANDING);
    }

    public AABB getLocalBoundsForPose(Pose pose) {
        EntityDimensions entitydimensions = this.getDimensions(pose);
        return new AABB(
            -entitydimensions.width() / 2.0F,
            0.0,
            -entitydimensions.width() / 2.0F,
            entitydimensions.width() / 2.0F,
            entitydimensions.height(),
            entitydimensions.width() / 2.0F
        );
    }

    protected boolean wouldNotSuffocateAtTargetPose(Pose pose) {
        AABB aabb = this.getDimensions(pose).makeBoundingBox(this.position());
        return this.level().noBlockCollision(this, aabb);
    }

    @Override
    public boolean canUsePortal(boolean allowPassengers) {
        return super.canUsePortal(allowPassengers) && !this.isSleeping();
    }

    public Optional<BlockPos> getSleepingPos() {
        return this.entityData.get(SLEEPING_POS_ID);
    }

    public void setSleepingPos(BlockPos pos) {
        this.entityData.set(SLEEPING_POS_ID, Optional.of(pos));
    }

    public void clearSleepingPos() {
        this.entityData.set(SLEEPING_POS_ID, Optional.empty());
    }

    public boolean isSleeping() {
        return this.getSleepingPos().isPresent();
    }

    public void startSleeping(BlockPos pos) {
        if (this.isPassenger()) {
            this.stopRiding();
        }

        BlockState blockstate = this.level().getBlockState(pos);
        if (blockstate.isBed(level(), pos, this)) {
            blockstate.setBedOccupied(level(), pos, this, true);
        }

        this.setPose(Pose.SLEEPING);
        this.setPosToBed(pos);
        this.setSleepingPos(pos);
        this.setDeltaMovement(Vec3.ZERO);
        this.hasImpulse = true;
    }

    /**
     * Sets entity position to a supplied BlockPos plus a little offset
     */
    private void setPosToBed(BlockPos pos) {
        this.setPos(pos.getX() + 0.5, pos.getY() + 0.6875, pos.getZ() + 0.5);
    }

    private boolean checkBedExists() {
        // Neo: Overwrite the vanilla instanceof BedBlock check with isBed and fire the CanContinueSleepingEvent.
        boolean hasBed = this.getSleepingPos().map(pos -> this.level().getBlockState(pos).isBed(this.level(), pos, this)).orElse(false);
        return net.neoforged.neoforge.event.EventHooks.canEntityContinueSleeping(this, hasBed ? null : Player.BedSleepingProblem.NOT_POSSIBLE_HERE);
    }

    public void stopSleeping() {
        this.getSleepingPos().filter(this.level()::hasChunkAt).ifPresent(p_261435_ -> {
            BlockState blockstate = this.level().getBlockState(p_261435_);
            if (blockstate.isBed(level(), p_261435_, this)) {
                Direction direction = blockstate.getValue(BedBlock.FACING);
                blockstate.setBedOccupied(level(), p_261435_, this, false);
                Vec3 vec31 = BedBlock.findStandUpPosition(this.getType(), this.level(), p_261435_, direction, this.getYRot()).orElseGet(() -> {
                    BlockPos blockpos = p_261435_.above();
                    return new Vec3(blockpos.getX() + 0.5, blockpos.getY() + 0.1, blockpos.getZ() + 0.5);
                });
                Vec3 vec32 = Vec3.atBottomCenterOf(p_261435_).subtract(vec31).normalize();
                float f = (float)Mth.wrapDegrees(Mth.atan2(vec32.z, vec32.x) * 180.0F / (float)Math.PI - 90.0);
                this.setPos(vec31.x, vec31.y, vec31.z);
                this.setYRot(f);
                this.setXRot(0.0F);
            }
        });
        Vec3 vec3 = this.position();
        this.setPose(Pose.STANDING);
        this.setPos(vec3.x, vec3.y, vec3.z);
        this.clearSleepingPos();
    }

    @Nullable
    public Direction getBedOrientation() {
        BlockPos blockpos = this.getSleepingPos().orElse(null);
        if (blockpos == null) return null;
        BlockState state = this.level().getBlockState(blockpos);
        return !state.isBed(level(), blockpos, this) ? null : state.getBedDirection(level(), blockpos);
    }

    @Override
    public boolean isInWall() {
        return !this.isSleeping() && super.isInWall();
    }

    public ItemStack getProjectile(ItemStack weaponStack) {
        return net.neoforged.neoforge.common.CommonHooks.getProjectile(this, weaponStack, ItemStack.EMPTY);
    }

    private static byte entityEventForEquipmentBreak(EquipmentSlot slot) {
        return switch (slot) {
            case MAINHAND -> 47;
            case OFFHAND -> 48;
            case HEAD -> 49;
            case CHEST -> 50;
            case FEET -> 52;
            case LEGS -> 51;
            case BODY -> 65;
            case SADDLE -> 68;
        };
    }

    public void onEquippedItemBroken(Item item, EquipmentSlot slot) {
        this.level().broadcastEntityEvent(this, entityEventForEquipmentBreak(slot));
        this.stopLocationBasedEffects(this.getItemBySlot(slot), slot, this.attributes);
    }

    private void stopLocationBasedEffects(ItemStack stack, EquipmentSlot slot, AttributeMap attributeMap) {
        stack.forEachModifier(slot, (p_359698_, p_359699_) -> {
            AttributeInstance attributeinstance = attributeMap.getInstance(p_359698_);
            if (attributeinstance != null) {
                attributeinstance.removeModifier(p_359699_);
            }
        });
        EnchantmentHelper.stopLocationBasedEffects(stack, this, slot);
    }

    public final boolean canEquipWithDispenser(ItemStack stack) {
        if (this.isAlive() && !this.isSpectator()) {
            Equippable equippable = stack.get(DataComponents.EQUIPPABLE);
            if (equippable != null && equippable.dispensable()) {
                EquipmentSlot equipmentslot = equippable.slot();
                return this.canUseSlot(equipmentslot) && equippable.canBeEquippedBy(this.getType())
                    ? this.getItemBySlot(equipmentslot).isEmpty() && this.canDispenserEquipIntoSlot(equipmentslot)
                    : false;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    protected boolean canDispenserEquipIntoSlot(EquipmentSlot slot) {
        return true;
    }

    public final EquipmentSlot getEquipmentSlotForItem(ItemStack stack) {
        final EquipmentSlot slot = stack.getEquipmentSlot();
        if (slot != null) return slot; // FORGE: Allow modders to set a non-default equipment slot for a stack; e.g. a non-armor chestplate-slot item
        Equippable equippable = stack.get(DataComponents.EQUIPPABLE);
        return equippable != null && this.canUseSlot(equippable.slot()) ? equippable.slot() : EquipmentSlot.MAINHAND;
    }

    public final boolean isEquippableInSlot(ItemStack stack, EquipmentSlot slot) {
        Equippable equippable = stack.get(DataComponents.EQUIPPABLE);
        return equippable == null
            ? slot == EquipmentSlot.MAINHAND && this.canUseSlot(EquipmentSlot.MAINHAND)
            : slot == equippable.slot() && this.canUseSlot(equippable.slot()) && equippable.canBeEquippedBy(this.getType());
    }

    private static SlotAccess createEquipmentSlotAccess(LivingEntity entity, EquipmentSlot slot) {
        return slot != EquipmentSlot.HEAD && slot != EquipmentSlot.MAINHAND && slot != EquipmentSlot.OFFHAND
            ? SlotAccess.forEquipmentSlot(entity, slot, p_348156_ -> p_348156_.isEmpty() || entity.getEquipmentSlotForItem(p_348156_) == slot)
            : SlotAccess.forEquipmentSlot(entity, slot);
    }

    @Nullable
    private static EquipmentSlot getEquipmentSlot(int index) {
        if (index == 100 + EquipmentSlot.HEAD.getIndex()) {
            return EquipmentSlot.HEAD;
        } else if (index == 100 + EquipmentSlot.CHEST.getIndex()) {
            return EquipmentSlot.CHEST;
        } else if (index == 100 + EquipmentSlot.LEGS.getIndex()) {
            return EquipmentSlot.LEGS;
        } else if (index == 100 + EquipmentSlot.FEET.getIndex()) {
            return EquipmentSlot.FEET;
        } else if (index == 98) {
            return EquipmentSlot.MAINHAND;
        } else if (index == 99) {
            return EquipmentSlot.OFFHAND;
        } else if (index == 105) {
            return EquipmentSlot.BODY;
        } else {
            return index == 106 ? EquipmentSlot.SADDLE : null;
        }
    }

    @Override
    public SlotAccess getSlot(int slot) {
        EquipmentSlot equipmentslot = getEquipmentSlot(slot);
        return equipmentslot != null ? createEquipmentSlotAccess(this, equipmentslot) : super.getSlot(slot);
    }

    @Override
    public boolean canFreeze() {
        if (this.isSpectator()) {
            return false;
        } else {
            for (EquipmentSlot equipmentslot : EquipmentSlotGroup.ARMOR) {
                if (this.getItemBySlot(equipmentslot).is(ItemTags.FREEZE_IMMUNE_WEARABLES)) {
                    return false;
                }
            }

            return super.canFreeze();
        }
    }

    @Override
    public boolean isCurrentlyGlowing() {
        return !this.level().isClientSide() && this.hasEffect(MobEffects.GLOWING) || super.isCurrentlyGlowing();
    }

    @Override
    public float getVisualRotationYInDegrees() {
        return this.yBodyRot;
    }

    @Override
    public void recreateFromPacket(ClientboundAddEntityPacket packet) {
        double d0 = packet.getX();
        double d1 = packet.getY();
        double d2 = packet.getZ();
        float f = packet.getYRot();
        float f1 = packet.getXRot();
        this.syncPacketPositionCodec(d0, d1, d2);
        this.yBodyRot = packet.getYHeadRot();
        this.yHeadRot = packet.getYHeadRot();
        this.yBodyRotO = this.yBodyRot;
        this.yHeadRotO = this.yHeadRot;
        this.setId(packet.getId());
        this.setUUID(packet.getUUID());
        this.absSnapTo(d0, d1, d2, f, f1);
        this.setDeltaMovement(packet.getMovement());
    }

    public float getSecondsToDisableBlocking() {
        Weapon weapon = this.getWeaponItem().get(DataComponents.WEAPON);
        return weapon != null ? weapon.disableBlockingForSeconds() : 0.0F;
    }

    @Override
    public float maxUpStep() {
        float f = (float)this.getAttributeValue(Attributes.STEP_HEIGHT);
        return this.getControllingPassenger() instanceof Player ? Math.max(f, 1.0F) : f;
    }

    @Override
    public Vec3 getPassengerRidingPosition(Entity entity) {
        return this.position().add(this.getPassengerAttachmentPoint(entity, this.getDimensions(this.getPose()), this.getScale() * this.getAgeScale()));
    }

    protected void lerpHeadRotationStep(int lerpHeadSteps, double lerpYHeadRot) {
        this.yHeadRot = (float)Mth.rotLerp(1.0 / lerpHeadSteps, (double)this.yHeadRot, lerpYHeadRot);
    }

    @Override
    public void igniteForTicks(int ticks) {
        super.igniteForTicks(Mth.ceil(ticks * this.getAttributeValue(Attributes.BURNING_TIME)));
    }

    public boolean hasInfiniteMaterials() {
        return false;
    }

    public boolean isInvulnerableTo(ServerLevel level, DamageSource damageSource) {
        return this.isInvulnerableToBase(damageSource) || EnchantmentHelper.isImmuneToDamage(level, this, damageSource);
    }

    public static boolean canGlideUsing(ItemStack stack, EquipmentSlot slot) {
        if (!stack.has(DataComponents.GLIDER)) {
            return false;
        } else {
            Equippable equippable = stack.get(DataComponents.EQUIPPABLE);
            return equippable != null && slot == equippable.slot() && !stack.nextDamageWillBreak();
        }
    }

    @VisibleForTesting
    public int getLastHurtByPlayerMemoryTime() {
        return this.lastHurtByPlayerMemoryTime;
    }

    @Override
    public boolean isTransmittingWaypoint() {
        return this.getAttributeValue(Attributes.WAYPOINT_TRANSMIT_RANGE) > 0.0;
    }

    @Override
    public Optional<WaypointTransmitter.Connection> makeWaypointConnectionWith(ServerPlayer player) {
        if (this.firstTick || player == this) {
            return Optional.empty();
        } else if (WaypointTransmitter.doesSourceIgnoreReceiver(this, player)) {
            return Optional.empty();
        } else {
            Waypoint.Icon waypoint$icon = this.locatorBarIcon.cloneAndAssignStyle(this);
            if (WaypointTransmitter.isReallyFar(this, player)) {
                return Optional.of(new WaypointTransmitter.EntityAzimuthConnection(this, waypoint$icon, player));
            } else {
                return !WaypointTransmitter.isChunkVisible(this.chunkPosition(), player)
                    ? Optional.of(new WaypointTransmitter.EntityChunkConnection(this, waypoint$icon, player))
                    : Optional.of(new WaypointTransmitter.EntityBlockConnection(this, waypoint$icon, player));
            }
        }
    }

    @Override
    public Waypoint.Icon waypointIcon() {
        return this.locatorBarIcon;
    }

    public record Fallsounds(SoundEvent small, SoundEvent big) {
    }
}
