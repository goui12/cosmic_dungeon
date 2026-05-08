package net.minecraft.world.entity;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Maps;
import it.unimi.dsi.fastutil.objects.Object2IntMap.Entry;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Vec3i;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.debug.DebugBrainDump;
import net.minecraft.util.debug.DebugGoalInfo;
import net.minecraft.util.debug.DebugPathInfo;
import net.minecraft.util.debug.DebugSubscriptions;
import net.minecraft.util.debug.DebugValueSource;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.Container;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.BodyRotationControl;
import net.minecraft.world.entity.ai.control.JumpControl;
import net.minecraft.world.entity.ai.control.LookControl;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.sensing.Sensing;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.AbstractBoat;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.UseRemainder;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.enchantment.providers.VanillaEnchantmentProviders;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.ticks.ContainerSingleItem;

public abstract class Mob extends LivingEntity implements EquipmentUser, Leashable, Targeting {
    private static final EntityDataAccessor<Byte> DATA_MOB_FLAGS_ID = SynchedEntityData.defineId(Mob.class, EntityDataSerializers.BYTE);
    private static final int MOB_FLAG_NO_AI = 1;
    private static final int MOB_FLAG_LEFTHANDED = 2;
    private static final int MOB_FLAG_AGGRESSIVE = 4;
    protected static final int PICKUP_REACH = 1;
    private static final Vec3i ITEM_PICKUP_REACH = new Vec3i(1, 0, 1);
    private static final List<EquipmentSlot> EQUIPMENT_POPULATION_ORDER = List.of(
        EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    );
    public static final float MAX_WEARING_ARMOR_CHANCE = 0.15F;
    public static final float WEARING_ARMOR_UPGRADE_MATERIAL_CHANCE = 0.1087F;
    public static final float WEARING_ARMOR_UPGRADE_MATERIAL_ATTEMPTS = 3.0F;
    public static final float MAX_PICKUP_LOOT_CHANCE = 0.55F;
    public static final float MAX_ENCHANTED_ARMOR_CHANCE = 0.5F;
    public static final float MAX_ENCHANTED_WEAPON_CHANCE = 0.25F;
    public static final int UPDATE_GOAL_SELECTOR_EVERY_N_TICKS = 2;
    private static final double DEFAULT_ATTACK_REACH = Math.sqrt(2.04F) - 0.6F;
    private static final boolean DEFAULT_CAN_PICK_UP_LOOT = false;
    private static final boolean DEFAULT_PERSISTENCE_REQUIRED = false;
    private static final boolean DEFAULT_LEFT_HANDED = false;
    private static final boolean DEFAULT_NO_AI = false;
    protected static final ResourceLocation RANDOM_SPAWN_BONUS_ID = ResourceLocation.withDefaultNamespace("random_spawn_bonus");
    public static final String TAG_DROP_CHANCES = "drop_chances";
    public static final String TAG_LEFT_HANDED = "LeftHanded";
    public static final String TAG_CAN_PICK_UP_LOOT = "CanPickUpLoot";
    public static final String TAG_NO_AI = "NoAI";
    public int ambientSoundTime;
    protected int xpReward;
    protected LookControl lookControl;
    protected MoveControl moveControl;
    protected JumpControl jumpControl;
    private final BodyRotationControl bodyRotationControl;
    protected PathNavigation navigation;
    public final GoalSelector goalSelector;
    public final GoalSelector targetSelector;
    @Nullable
    private LivingEntity target;
    private final Sensing sensing;
    private DropChances dropChances = DropChances.DEFAULT;
    private boolean canPickUpLoot = false;
    private boolean persistenceRequired = false;
    private final Map<PathType, Float> pathfindingMalus = Maps.newEnumMap(PathType.class);
    private Optional<ResourceKey<LootTable>> lootTable = Optional.empty();
    private long lootTableSeed;
    @Nullable
    private Leashable.LeashData leashData;
    private BlockPos homePosition = BlockPos.ZERO;
    private int homeRadius = -1;
    @Nullable
    private EntitySpawnReason spawnType;
    private boolean spawnCancelled = false;
    /** Neo: Prevent immediate spawning from conversions to capture conversion results for events */
    protected boolean preventConversionSpawns = false;

    protected Mob(EntityType<? extends Mob> entityType, Level level) {
        super(entityType, level);
        this.goalSelector = new GoalSelector();
        this.targetSelector = new GoalSelector();
        this.lookControl = new LookControl(this);
        this.moveControl = new MoveControl(this);
        this.jumpControl = new JumpControl(this);
        this.bodyRotationControl = this.createBodyControl();
        this.navigation = this.createNavigation(level);
        this.sensing = new Sensing(this);
        if (level instanceof ServerLevel) {
            this.registerGoals();
        }
    }

    protected void registerGoals() {
    }

    public static AttributeSupplier.Builder createMobAttributes() {
        return LivingEntity.createLivingAttributes().add(Attributes.FOLLOW_RANGE, 16.0);
    }

    protected PathNavigation createNavigation(Level level) {
        return new GroundPathNavigation(this, level);
    }

    protected boolean shouldPassengersInheritMalus() {
        return false;
    }

    public float getPathfindingMalus(PathType pathType) {
        Mob mob;
        if (this.getControlledVehicle() instanceof Mob mob1 && mob1.shouldPassengersInheritMalus()) {
            mob = mob1;
        } else {
            mob = this;
        }

        Float f = mob.pathfindingMalus.get(pathType);
        return f == null ? pathType.getMalus() : f;
    }

    public void setPathfindingMalus(PathType pathType, float malus) {
        this.pathfindingMalus.put(pathType, malus);
    }

    public void onPathfindingStart() {
    }

    public void onPathfindingDone() {
    }

    protected BodyRotationControl createBodyControl() {
        return new BodyRotationControl(this);
    }

    public LookControl getLookControl() {
        return this.lookControl;
    }

    public MoveControl getMoveControl() {
        return this.getControlledVehicle() instanceof Mob mob ? mob.getMoveControl() : this.moveControl;
    }

    public JumpControl getJumpControl() {
        return this.jumpControl;
    }

    public PathNavigation getNavigation() {
        return this.getControlledVehicle() instanceof Mob mob ? mob.getNavigation() : this.navigation;
    }

    @Nullable
    @Override
    public LivingEntity getControllingPassenger() {
        Entity entity = this.getFirstPassenger();
        return !this.isNoAi() && entity instanceof Mob mob && entity.canControlVehicle() ? mob : null;
    }

    public Sensing getSensing() {
        return this.sensing;
    }

    @Nullable
    @Override
    public LivingEntity getTarget() {
        return this.target;
    }

    @Nullable
    protected final LivingEntity getTargetFromBrain() {
        return this.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).orElse(null);
    }

    /**
     * Sets the active target the Goal system uses for tracking
     */
    public void setTarget(@Nullable LivingEntity target) {
        net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent changeTargetEvent = net.neoforged.neoforge.common.CommonHooks.onLivingChangeTarget(this, target, net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent.LivingTargetType.MOB_TARGET);
        if(!changeTargetEvent.isCanceled()) {
             this.target = changeTargetEvent.getNewAboutToBeSetTarget();
        }
    }

    @Override
    public boolean canAttackType(EntityType<?> type) {
        return type != EntityType.GHAST;
    }

    public boolean canFireProjectileWeapon(ProjectileWeaponItem projectileWeapon) {
        return false;
    }

    public void ate() {
        this.gameEvent(GameEvent.EAT);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_MOB_FLAGS_ID, (byte)0);
    }

    public int getAmbientSoundInterval() {
        return 80;
    }

    public void playAmbientSound() {
        this.makeSound(this.getAmbientSound());
    }

    @Override
    public void baseTick() {
        super.baseTick();
        ProfilerFiller profilerfiller = Profiler.get();
        profilerfiller.push("mobBaseTick");
        if (this.isAlive() && this.random.nextInt(1000) < this.ambientSoundTime++) {
            this.resetAmbientSoundTime();
            this.playAmbientSound();
        }

        profilerfiller.pop();
    }

    @Override
    protected void playHurtSound(DamageSource source) {
        this.resetAmbientSoundTime();
        super.playHurtSound(source);
    }

    private void resetAmbientSoundTime() {
        this.ambientSoundTime = -this.getAmbientSoundInterval();
    }

    @Override
    protected int getBaseExperienceReward(ServerLevel level) {
        if (this.xpReward > 0) {
            int i = this.xpReward;

            for (EquipmentSlot equipmentslot : EquipmentSlot.VALUES) {
                if (equipmentslot.canIncreaseExperience()) {
                    ItemStack itemstack = this.getItemBySlot(equipmentslot);
                    if (!itemstack.isEmpty() && this.dropChances.byEquipment(equipmentslot) <= 1.0F) {
                        i += 1 + this.random.nextInt(3);
                    }
                }
            }

            return i;
        } else {
            return this.xpReward;
        }
    }

    public void spawnAnim() {
        if (this.level().isClientSide()) {
            this.makePoofParticles();
        } else {
            this.level().broadcastEntityEvent(this, (byte)20);
        }
    }

    /**
     * Handler for {@link World#setEntityState}
     */
    @Override
    public void handleEntityEvent(byte id) {
        if (id == 20) {
            this.spawnAnim();
        } else {
            super.handleEntityEvent(id);
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide() && this.tickCount % 5 == 0) {
            this.updateControlFlags();
        }

        // Neo: Animal armor tick patch
        if (this.canUseSlot(EquipmentSlot.BODY)) {
            ItemStack stack = this.getBodyArmorItem();
            if (stack.has(DataComponents.EQUIPPABLE)) stack.onAnimalArmorTick(level(), this);
        }
    }

    protected void updateControlFlags() {
        boolean flag = !(this.getControllingPassenger() instanceof Mob);
        boolean flag1 = !(this.getVehicle() instanceof AbstractBoat);
        this.goalSelector.setControlFlag(Goal.Flag.MOVE, flag);
        this.goalSelector.setControlFlag(Goal.Flag.JUMP, flag && flag1);
        this.goalSelector.setControlFlag(Goal.Flag.LOOK, flag);
    }

    @Override
    protected void tickHeadTurn(float yBodyRot) {
        this.bodyRotationControl.clientTick();
    }

    @Nullable
    protected SoundEvent getAmbientSound() {
        return null;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putBoolean("CanPickUpLoot", this.canPickUpLoot());
        output.putBoolean("PersistenceRequired", this.persistenceRequired);
        if (!this.dropChances.equals(DropChances.DEFAULT)) {
            output.store("drop_chances", DropChances.CODEC, this.dropChances);
        }

        this.writeLeashData(output, this.leashData);
        if (this.hasHome()) {
            output.putInt("home_radius", this.homeRadius);
            output.store("home_pos", BlockPos.CODEC, this.homePosition);
        }

        output.putBoolean("LeftHanded", this.isLeftHanded());
        this.lootTable.ifPresent(p_421366_ -> output.store("DeathLootTable", LootTable.KEY_CODEC, (ResourceKey<LootTable>)p_421366_));
        if (this.lootTableSeed != 0L) {
            output.putLong("DeathLootTableSeed", this.lootTableSeed);
        }

        if (this.isNoAi()) {
            output.putBoolean("NoAI", this.isNoAi());
        }
        if (this.spawnType != null) {
            output.putString("neoforge:spawn_type", this.spawnType.name());
        }
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.setCanPickUpLoot(input.getBooleanOr("CanPickUpLoot", false));
        this.persistenceRequired = input.getBooleanOr("PersistenceRequired", false);
        this.dropChances = input.read("drop_chances", DropChances.CODEC).orElse(DropChances.DEFAULT);
        this.readLeashData(input);
        this.homeRadius = input.getIntOr("home_radius", -1);
        if (this.homeRadius >= 0) {
            this.homePosition = input.read("home_pos", BlockPos.CODEC).orElse(BlockPos.ZERO);
        }

        this.setLeftHanded(input.getBooleanOr("LeftHanded", false));
        this.lootTable = input.read("DeathLootTable", LootTable.KEY_CODEC);
        this.lootTableSeed = input.getLongOr("DeathLootTableSeed", 0L);
        this.setNoAi(input.getBooleanOr("NoAI", false));

        input.getString("neoforge:spawn_type")
                .ifPresent(spawnType -> {
                    try {
                        this.spawnType = EntitySpawnReason.valueOf(spawnType);
                    } catch (Exception ignored) {
                    }
                });
    }

    @Override
    protected void dropFromLootTable(ServerLevel level, DamageSource damageSource, boolean playerKill) {
        super.dropFromLootTable(level, damageSource, playerKill);
        this.lootTable = Optional.empty();
    }

    @Override
    public final Optional<ResourceKey<LootTable>> getLootTable() {
        return this.lootTable.isPresent() ? this.lootTable : super.getLootTable();
    }

    @Override
    public long getLootTableSeed() {
        return this.lootTableSeed;
    }

    public void setZza(float amount) {
        this.zza = amount;
    }

    public void setYya(float amount) {
        this.yya = amount;
    }

    public void setXxa(float amount) {
        this.xxa = amount;
    }

    /**
     * Sets the movespeed used for the new AI system.
     */
    @Override
    public void setSpeed(float speed) {
        super.setSpeed(speed);
        this.setZza(speed);
    }

    public void stopInPlace() {
        this.getNavigation().stop();
        this.setXxa(0.0F);
        this.setYya(0.0F);
        this.setSpeed(0.0F);
        this.setDeltaMovement(0.0, 0.0, 0.0);
        this.resetAngularLeashMomentum();
    }

    @Override
    public void aiStep() {
        super.aiStep();
        ProfilerFiller profilerfiller = Profiler.get();
        profilerfiller.push("looting");
        if (this.level() instanceof ServerLevel serverlevel
            && this.canPickUpLoot()
            && this.isAlive()
            && !this.dead
            && net.neoforged.neoforge.event.EventHooks.canEntityGrief(serverlevel, this)) {
            Vec3i vec3i = this.getPickupReach();

            for (ItemEntity itementity : this.level()
                .getEntitiesOfClass(ItemEntity.class, this.getBoundingBox().inflate(vec3i.getX(), vec3i.getY(), vec3i.getZ()))) {
                if (!itementity.isRemoved()
                    && !itementity.getItem().isEmpty()
                    && !itementity.hasPickUpDelay()
                    && this.wantsToPickUp(serverlevel, itementity.getItem())) {
                    this.pickUpItem(serverlevel, itementity);
                }
            }
        }

        profilerfiller.pop();
    }

    protected Vec3i getPickupReach() {
        return ITEM_PICKUP_REACH;
    }

    protected void pickUpItem(ServerLevel level, ItemEntity entity) {
        ItemStack itemstack = entity.getItem();
        ItemStack itemstack1 = this.equipItemIfPossible(level, itemstack.copy());
        if (!itemstack1.isEmpty()) {
            this.onItemPickup(entity);
            this.take(entity, itemstack1.getCount());
            itemstack.shrink(itemstack1.getCount());
            if (itemstack.isEmpty()) {
                entity.discard();
            }
        }
    }

    public ItemStack equipItemIfPossible(ServerLevel level, ItemStack stack) {
        EquipmentSlot equipmentslot = this.getEquipmentSlotForItem(stack);
        if (!this.isEquippableInSlot(stack, equipmentslot)) {
            return ItemStack.EMPTY;
        } else {
            ItemStack itemstack = this.getItemBySlot(equipmentslot);
            boolean flag = this.canReplaceCurrentItem(stack, itemstack, equipmentslot);
            if (equipmentslot.isArmor() && !flag) {
                equipmentslot = EquipmentSlot.MAINHAND;
                itemstack = this.getItemBySlot(equipmentslot);
                flag = itemstack.isEmpty();
            }

            if (flag && this.canHoldItem(stack)) {
                double d0 = this.dropChances.byEquipment(equipmentslot);
                if (!itemstack.isEmpty() && Math.max(this.random.nextFloat() - 0.1F, 0.0F) < d0) {
                    this.spawnAtLocation(level, itemstack);
                }

                ItemStack itemstack1 = equipmentslot.limit(stack);
                this.setItemSlotAndDropWhenKilled(equipmentslot, itemstack1);
                return itemstack1;
            } else {
                return ItemStack.EMPTY;
            }
        }
    }

    protected void setItemSlotAndDropWhenKilled(EquipmentSlot slot, ItemStack stack) {
        this.setItemSlot(slot, stack);
        this.setGuaranteedDrop(slot);
        this.persistenceRequired = true;
    }

    protected boolean canShearEquipment(Player player) {
        return !this.isVehicle();
    }

    public void setGuaranteedDrop(EquipmentSlot slot) {
        this.dropChances = this.dropChances.withGuaranteedDrop(slot);
    }

    protected boolean canReplaceCurrentItem(ItemStack newItem, ItemStack currentItem, EquipmentSlot slot) {
        if (currentItem.isEmpty()) {
            return true;
        } else if (slot.isArmor()) {
            return this.compareArmor(newItem, currentItem, slot);
        } else {
            return slot == EquipmentSlot.MAINHAND ? this.compareWeapons(newItem, currentItem, slot) : false;
        }
    }

    private boolean compareArmor(ItemStack newItem, ItemStack currentItem, EquipmentSlot slot) {
        if (EnchantmentHelper.has(currentItem, EnchantmentEffectComponents.PREVENT_ARMOR_CHANGE)) {
            return false;
        } else {
            double d0 = this.getApproximateAttributeWith(newItem, Attributes.ARMOR, slot);
            double d1 = this.getApproximateAttributeWith(currentItem, Attributes.ARMOR, slot);
            double d2 = this.getApproximateAttributeWith(newItem, Attributes.ARMOR_TOUGHNESS, slot);
            double d3 = this.getApproximateAttributeWith(currentItem, Attributes.ARMOR_TOUGHNESS, slot);
            if (d0 != d1) {
                return d0 > d1;
            } else {
                return d2 != d3 ? d2 > d3 : this.canReplaceEqualItem(newItem, currentItem);
            }
        }
    }

    private boolean compareWeapons(ItemStack newItem, ItemStack currentItem, EquipmentSlot slot) {
        TagKey<Item> tagkey = this.getPreferredWeaponType();
        if (tagkey != null) {
            if (currentItem.is(tagkey) && !newItem.is(tagkey)) {
                return false;
            }

            if (!currentItem.is(tagkey) && newItem.is(tagkey)) {
                return true;
            }
        }

        double d0 = this.getApproximateAttributeWith(newItem, Attributes.ATTACK_DAMAGE, slot);
        double d1 = this.getApproximateAttributeWith(currentItem, Attributes.ATTACK_DAMAGE, slot);
        return d0 != d1 ? d0 > d1 : this.canReplaceEqualItem(newItem, currentItem);
    }

    private double getApproximateAttributeWith(ItemStack item, Holder<Attribute> attribute, EquipmentSlot slot) {
        double d0 = this.getAttributes().hasAttribute(attribute) ? this.getAttributeBaseValue(attribute) : 0.0;
        ItemAttributeModifiers itemattributemodifiers = item.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
        // Neo: Respect gameplay modifiers
        itemattributemodifiers = item.getAttributeModifiers();
        return itemattributemodifiers.compute(d0, slot);
    }

    public boolean canReplaceEqualItem(ItemStack candidate, ItemStack existing) {
        Set<Entry<Holder<Enchantment>>> set = existing.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY).entrySet();
        Set<Entry<Holder<Enchantment>>> set1 = candidate.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY).entrySet();
        if (set1.size() != set.size()) {
            return set1.size() > set.size();
        } else {
            int i = candidate.getDamageValue();
            int j = existing.getDamageValue();
            return i != j ? i < j : candidate.has(DataComponents.CUSTOM_NAME) && !existing.has(DataComponents.CUSTOM_NAME);
        }
    }

    public boolean canHoldItem(ItemStack stack) {
        return true;
    }

    public boolean wantsToPickUp(ServerLevel level, ItemStack stack) {
        return this.canHoldItem(stack);
    }

    @Nullable
    public TagKey<Item> getPreferredWeaponType() {
        return null;
    }

    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return true;
    }

    public boolean requiresCustomPersistence() {
        return this.isPassenger();
    }

    @Override
    public void checkDespawn() {
        if (net.neoforged.neoforge.event.EventHooks.checkMobDespawn(this)) return;
        if (this.level().getDifficulty() == Difficulty.PEACEFUL && !this.getType().isAllowedInPeaceful()) {
            this.discard();
        } else if (!this.isPersistenceRequired() && !this.requiresCustomPersistence()) {
            Entity entity = this.level().getNearestPlayer(this, -1.0);
            if (entity != null) {
                double d0 = entity.distanceToSqr(this);
                int i = this.getType().getCategory().getDespawnDistance();
                int j = i * i;
                if (d0 > j && this.removeWhenFarAway(d0)) {
                    this.discard();
                }

                int k = this.getType().getCategory().getNoDespawnDistance();
                int l = k * k;
                if (this.noActionTime > 600 && this.random.nextInt(800) == 0 && d0 > l && this.removeWhenFarAway(d0)) {
                    this.discard();
                } else if (d0 < l) {
                    this.noActionTime = 0;
                }
            }
        } else {
            this.noActionTime = 0;
        }
    }

    @Override
    protected final void serverAiStep() {
        this.noActionTime++;
        ProfilerFiller profilerfiller = Profiler.get();
        profilerfiller.push("sensing");
        this.sensing.tick();
        profilerfiller.pop();
        int i = this.tickCount + this.getId();
        if (i % 2 != 0 && this.tickCount > 1) {
            profilerfiller.push("targetSelector");
            this.targetSelector.tickRunningGoals(false);
            profilerfiller.pop();
            profilerfiller.push("goalSelector");
            this.goalSelector.tickRunningGoals(false);
            profilerfiller.pop();
        } else {
            profilerfiller.push("targetSelector");
            this.targetSelector.tick();
            profilerfiller.pop();
            profilerfiller.push("goalSelector");
            this.goalSelector.tick();
            profilerfiller.pop();
        }

        profilerfiller.push("navigation");
        this.navigation.tick();
        profilerfiller.pop();
        profilerfiller.push("mob tick");
        this.customServerAiStep((ServerLevel)this.level());
        profilerfiller.pop();
        profilerfiller.push("controls");
        profilerfiller.push("move");
        this.moveControl.tick();
        profilerfiller.popPush("look");
        this.lookControl.tick();
        profilerfiller.popPush("jump");
        this.jumpControl.tick();
        profilerfiller.pop();
        profilerfiller.pop();
    }

    protected void customServerAiStep(ServerLevel level) {
    }

    public int getMaxHeadXRot() {
        return 40;
    }

    public int getMaxHeadYRot() {
        return 75;
    }

    protected void clampHeadRotationToBody() {
        float f = this.getMaxHeadYRot();
        float f1 = this.getYHeadRot();
        float f2 = Mth.wrapDegrees(this.yBodyRot - f1);
        float f3 = Mth.clamp(Mth.wrapDegrees(this.yBodyRot - f1), -f, f);
        float f4 = f1 + f2 - f3;
        this.setYHeadRot(f4);
    }

    public int getHeadRotSpeed() {
        return 10;
    }

    /**
     * Changes the X and Y rotation so that this entity is facing the given entity.
     */
    public void lookAt(Entity entity, float maxYRotIncrease, float maxXRotIncrease) {
        double d0 = entity.getX() - this.getX();
        double d2 = entity.getZ() - this.getZ();
        double d1;
        if (entity instanceof LivingEntity livingentity) {
            d1 = livingentity.getEyeY() - this.getEyeY();
        } else {
            d1 = (entity.getBoundingBox().minY + entity.getBoundingBox().maxY) / 2.0 - this.getEyeY();
        }

        double d3 = Math.sqrt(d0 * d0 + d2 * d2);
        float f = (float)(Mth.atan2(d2, d0) * 180.0F / (float)Math.PI) - 90.0F;
        float f1 = (float)(-(Mth.atan2(d1, d3) * 180.0F / (float)Math.PI));
        this.setXRot(this.rotlerp(this.getXRot(), f1, maxXRotIncrease));
        this.setYRot(this.rotlerp(this.getYRot(), f, maxYRotIncrease));
    }

    /**
     * Arguments: current rotation, intended rotation, max increment.
     */
    private float rotlerp(float angle, float targetAngle, float maxIncrease) {
        float f = Mth.wrapDegrees(targetAngle - angle);
        if (f > maxIncrease) {
            f = maxIncrease;
        }

        if (f < -maxIncrease) {
            f = -maxIncrease;
        }

        return angle + f;
    }

    public static boolean checkMobSpawnRules(
        EntityType<? extends Mob> entityType, LevelAccessor level, EntitySpawnReason spawnReason, BlockPos pos, RandomSource random
    ) {
        BlockPos blockpos = pos.below();
        return EntitySpawnReason.isSpawner(spawnReason) || level.getBlockState(blockpos).isValidSpawn(level, blockpos, entityType);
    }

    public boolean checkSpawnRules(LevelAccessor level, EntitySpawnReason spawnReason) {
        return true;
    }

    public boolean checkSpawnObstruction(LevelReader level) {
        return !level.containsAnyLiquid(this.getBoundingBox()) && level.isUnobstructed(this);
    }

    public int getMaxSpawnClusterSize() {
        return 4;
    }

    public boolean isMaxGroupSizeReached(int size) {
        return false;
    }

    @Override
    public int getMaxFallDistance() {
        if (this.getTarget() == null) {
            return this.getComfortableFallDistance(0.0F);
        } else {
            int i = (int)(this.getHealth() - this.getMaxHealth() * 0.33F);
            i -= (3 - this.level().getDifficulty().getId()) * 4;
            if (i < 0) {
                i = 0;
            }

            return this.getComfortableFallDistance(i);
        }
    }

    public ItemStack getBodyArmorItem() {
        return this.getItemBySlot(EquipmentSlot.BODY);
    }

    public boolean isSaddled() {
        return this.hasValidEquippableItemForSlot(EquipmentSlot.SADDLE);
    }

    public boolean isWearingBodyArmor() {
        return this.hasValidEquippableItemForSlot(EquipmentSlot.BODY);
    }

    private boolean hasValidEquippableItemForSlot(EquipmentSlot slot) {
        return this.hasItemInSlot(slot) && this.isEquippableInSlot(this.getItemBySlot(slot), slot);
    }

    public void setBodyArmorItem(ItemStack stack) {
        this.setItemSlotAndDropWhenKilled(EquipmentSlot.BODY, stack);
    }

    public Container createEquipmentSlotContainer(final EquipmentSlot slot) {
        return new ContainerSingleItem() {
            @Override
            public ItemStack getTheItem() {
                return Mob.this.getItemBySlot(slot);
            }

            @Override
            public void setTheItem(ItemStack p_396982_) {
                Mob.this.setItemSlot(slot, p_396982_);
                if (!p_396982_.isEmpty()) {
                    Mob.this.setGuaranteedDrop(slot);
                    Mob.this.setPersistenceRequired();
                }
            }

            @Override
            public void setChanged() {
            }

            @Override
            public boolean stillValid(Player p_397156_) {
                return p_397156_.getVehicle() == Mob.this || p_397156_.canInteractWithEntity(Mob.this, 4.0);
            }
        };
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource damageSource, boolean recentlyHit) {
        super.dropCustomDeathLoot(level, damageSource, recentlyHit);

        for (EquipmentSlot equipmentslot : EquipmentSlot.VALUES) {
            ItemStack itemstack = this.getItemBySlot(equipmentslot);
            float f = this.dropChances.byEquipment(equipmentslot);
            if (f != 0.0F) {
                boolean flag = this.dropChances.isPreserved(equipmentslot);
                if (damageSource.getEntity() instanceof LivingEntity livingentity && this.level() instanceof ServerLevel serverlevel) {
                    f = EnchantmentHelper.processEquipmentDropChance(serverlevel, livingentity, damageSource, f);
                }

                if (!itemstack.isEmpty()
                    && !EnchantmentHelper.has(itemstack, EnchantmentEffectComponents.PREVENT_EQUIPMENT_DROP)
                    && (recentlyHit || flag)
                    && this.random.nextFloat() < f) {
                    if (!flag && itemstack.isDamageableItem()) {
                        itemstack.setDamageValue(
                            itemstack.getMaxDamage() - this.random.nextInt(1 + this.random.nextInt(Math.max(itemstack.getMaxDamage() - 3, 1)))
                        );
                    }

                    this.spawnAtLocation(level, itemstack);
                    this.setItemSlot(equipmentslot, ItemStack.EMPTY);
                }
            }
        }
    }

    public DropChances getDropChances() {
        return this.dropChances;
    }

    public void dropPreservedEquipment(ServerLevel level) {
        this.dropPreservedEquipment(level, p_352412_ -> true);
    }

    public Set<EquipmentSlot> dropPreservedEquipment(ServerLevel level, Predicate<ItemStack> filter) {
        Set<EquipmentSlot> set = new HashSet<>();

        for (EquipmentSlot equipmentslot : EquipmentSlot.VALUES) {
            ItemStack itemstack = this.getItemBySlot(equipmentslot);
            if (!itemstack.isEmpty()) {
                if (!filter.test(itemstack)) {
                    set.add(equipmentslot);
                } else if (this.dropChances.isPreserved(equipmentslot)) {
                    this.setItemSlot(equipmentslot, ItemStack.EMPTY);
                    this.spawnAtLocation(level, itemstack);
                }
            }
        }

        return set;
    }

    private LootParams createEquipmentParams(ServerLevel level) {
        return new LootParams.Builder(level)
            .withParameter(LootContextParams.ORIGIN, this.position())
            .withParameter(LootContextParams.THIS_ENTITY, this)
            .create(LootContextParamSets.EQUIPMENT);
    }

    public void equip(EquipmentTable equipmentTable) {
        this.equip(equipmentTable.lootTable(), equipmentTable.slotDropChances());
    }

    public void equip(ResourceKey<LootTable> equipmentLootTable, Map<EquipmentSlot, Float> slotDropChances) {
        if (this.level() instanceof ServerLevel serverlevel) {
            this.equip(equipmentLootTable, this.createEquipmentParams(serverlevel), slotDropChances);
        }
    }

    protected void populateDefaultEquipmentSlots(RandomSource random, DifficultyInstance difficulty) {
        if (random.nextFloat() < 0.15F * difficulty.getSpecialMultiplier()) {
            int i = random.nextInt(3);

            for (int j = 1; j <= 3.0F; j++) {
                if (random.nextFloat() < 0.1087F) {
                    i++;
                }
            }

            float f = this.level().getDifficulty() == Difficulty.HARD ? 0.1F : 0.25F;
            boolean flag = true;

            for (EquipmentSlot equipmentslot : EQUIPMENT_POPULATION_ORDER) {
                ItemStack itemstack = this.getItemBySlot(equipmentslot);
                if (!flag && random.nextFloat() < f) {
                    break;
                }

                flag = false;
                if (itemstack.isEmpty()) {
                    Item item = getEquipmentForSlot(equipmentslot, i);
                    if (item != null) {
                        this.setItemSlot(equipmentslot, new ItemStack(item));
                    }
                }
            }
        }
    }

    @Nullable
    public static Item getEquipmentForSlot(EquipmentSlot slot, int chance) {
        switch (slot) {
            case HEAD:
                if (chance == 0) {
                    return Items.LEATHER_HELMET;
                } else if (chance == 1) {
                    return Items.COPPER_HELMET;
                } else if (chance == 2) {
                    return Items.GOLDEN_HELMET;
                } else if (chance == 3) {
                    return Items.CHAINMAIL_HELMET;
                } else if (chance == 4) {
                    return Items.IRON_HELMET;
                } else if (chance == 5) {
                    return Items.DIAMOND_HELMET;
                }
            case CHEST:
                if (chance == 0) {
                    return Items.LEATHER_CHESTPLATE;
                } else if (chance == 1) {
                    return Items.COPPER_CHESTPLATE;
                } else if (chance == 2) {
                    return Items.GOLDEN_CHESTPLATE;
                } else if (chance == 3) {
                    return Items.CHAINMAIL_CHESTPLATE;
                } else if (chance == 4) {
                    return Items.IRON_CHESTPLATE;
                } else if (chance == 5) {
                    return Items.DIAMOND_CHESTPLATE;
                }
            case LEGS:
                if (chance == 0) {
                    return Items.LEATHER_LEGGINGS;
                } else if (chance == 1) {
                    return Items.COPPER_LEGGINGS;
                } else if (chance == 2) {
                    return Items.GOLDEN_LEGGINGS;
                } else if (chance == 3) {
                    return Items.CHAINMAIL_LEGGINGS;
                } else if (chance == 4) {
                    return Items.IRON_LEGGINGS;
                } else if (chance == 5) {
                    return Items.DIAMOND_LEGGINGS;
                }
            case FEET:
                if (chance == 0) {
                    return Items.LEATHER_BOOTS;
                } else if (chance == 1) {
                    return Items.COPPER_BOOTS;
                } else if (chance == 2) {
                    return Items.GOLDEN_BOOTS;
                } else if (chance == 3) {
                    return Items.CHAINMAIL_BOOTS;
                } else if (chance == 4) {
                    return Items.IRON_BOOTS;
                } else if (chance == 5) {
                    return Items.DIAMOND_BOOTS;
                }
            default:
                return null;
        }
    }

    protected void populateDefaultEquipmentEnchantments(ServerLevelAccessor level, RandomSource random, DifficultyInstance difficulty) {
        this.enchantSpawnedWeapon(level, random, difficulty);

        for (EquipmentSlot equipmentslot : EquipmentSlot.VALUES) {
            if (equipmentslot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR) {
                this.enchantSpawnedArmor(level, random, equipmentslot, difficulty);
            }
        }
    }

    protected void enchantSpawnedWeapon(ServerLevelAccessor level, RandomSource random, DifficultyInstance difficulty) {
        this.enchantSpawnedEquipment(level, EquipmentSlot.MAINHAND, random, 0.25F, difficulty);
    }

    protected void enchantSpawnedArmor(ServerLevelAccessor level, RandomSource random, EquipmentSlot slot, DifficultyInstance difficulty) {
        this.enchantSpawnedEquipment(level, slot, random, 0.5F, difficulty);
    }

    private void enchantSpawnedEquipment(
        ServerLevelAccessor level, EquipmentSlot slot, RandomSource random, float enchantChance, DifficultyInstance difficulty
    ) {
        ItemStack itemstack = this.getItemBySlot(slot);
        if (!itemstack.isEmpty() && random.nextFloat() < enchantChance * difficulty.getSpecialMultiplier()) {
            EnchantmentHelper.enchantItemFromProvider(
                itemstack, level.registryAccess(), VanillaEnchantmentProviders.MOB_SPAWN_EQUIPMENT, difficulty, random
            );
            this.setItemSlot(slot, itemstack);
        }
    }

    /**
     * @deprecated Override-Only. External callers should call via {@link
     *             net.neoforged.neoforge.event.EventHooks#finalizeMobSpawn}.
     */
    @Deprecated
    @org.jetbrains.annotations.ApiStatus.OverrideOnly
    @Nullable
    public SpawnGroupData finalizeSpawn(
        ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason spawnReason, @Nullable SpawnGroupData spawnGroupData
    ) {
        RandomSource randomsource = level.getRandom();
        AttributeInstance attributeinstance = Objects.requireNonNull(this.getAttribute(Attributes.FOLLOW_RANGE));
        if (!attributeinstance.hasModifier(RANDOM_SPAWN_BONUS_ID)) {
            attributeinstance.addPermanentModifier(
                new AttributeModifier(RANDOM_SPAWN_BONUS_ID, randomsource.triangle(0.0, 0.11485000000000001), AttributeModifier.Operation.ADD_MULTIPLIED_BASE)
            );
        }

        this.setLeftHanded(randomsource.nextFloat() < 0.05F);
        this.spawnType = spawnReason;
        return spawnGroupData;
    }

    public void setPersistenceRequired() {
        this.persistenceRequired = true;
    }

    @Override
    public void setDropChance(EquipmentSlot slot, float chance) {
        this.dropChances = this.dropChances.withEquipmentChance(slot, chance);
    }

    @Override
    public boolean canPickUpLoot() {
        return this.canPickUpLoot;
    }

    public void setCanPickUpLoot(boolean canPickUpLoot) {
        this.canPickUpLoot = canPickUpLoot;
    }

    @Override
    protected boolean canDispenserEquipIntoSlot(EquipmentSlot slot) {
        return this.canPickUpLoot();
    }

    public boolean isPersistenceRequired() {
        return this.persistenceRequired;
    }

    @Override
    public final InteractionResult interact(Player player, InteractionHand hand) {
        if (!this.isAlive()) {
            return InteractionResult.PASS;
        } else {
            InteractionResult interactionresult = this.checkAndHandleImportantInteractions(player, hand);
            if (interactionresult.consumesAction()) {
                this.gameEvent(GameEvent.ENTITY_INTERACT, player);
                return interactionresult;
            } else {
                InteractionResult interactionresult1 = super.interact(player, hand);
                if (interactionresult1 != InteractionResult.PASS) {
                    return interactionresult1;
                } else {
                    interactionresult = this.mobInteract(player, hand);
                    if (interactionresult.consumesAction()) {
                        this.gameEvent(GameEvent.ENTITY_INTERACT, player);
                        return interactionresult;
                    } else {
                        return InteractionResult.PASS;
                    }
                }
            }
        }
    }

    private InteractionResult checkAndHandleImportantInteractions(Player player, InteractionHand hand) {
        ItemStack itemstack = player.getItemInHand(hand);
        if (itemstack.is(Items.NAME_TAG)) {
            InteractionResult interactionresult = itemstack.interactLivingEntity(player, this, hand);
            if (interactionresult.consumesAction()) {
                return interactionresult;
            }
        }

        if (itemstack.getItem() instanceof SpawnEggItem) {
            if (this.level() instanceof ServerLevel) {
                SpawnEggItem spawneggitem = (SpawnEggItem)itemstack.getItem();
                Optional<Mob> optional = spawneggitem.spawnOffspringFromSpawnEgg(
                    player, this, (EntityType<? extends Mob>)this.getType(), (ServerLevel)this.level(), this.position(), itemstack
                );
                optional.ifPresent(p_21476_ -> this.onOffspringSpawnedFromEgg(player, p_21476_));
                if (optional.isEmpty()) {
                    return InteractionResult.PASS;
                }
            }

            return InteractionResult.SUCCESS_SERVER;
        } else {
            return InteractionResult.PASS;
        }
    }

    protected void onOffspringSpawnedFromEgg(Player player, Mob child) {
    }

    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        return InteractionResult.PASS;
    }

    protected void usePlayerItem(Player player, InteractionHand hand, ItemStack stack) {
        int i = stack.getCount();
        UseRemainder useremainder = stack.get(DataComponents.USE_REMAINDER);
        stack.consume(1, player);
        if (useremainder != null) {
            ItemStack itemstack = useremainder.convertIntoRemainder(stack, i, player.hasInfiniteMaterials(), player::handleExtraItemsCreatedOnUse);
            player.setItemInHand(hand, itemstack);
        }
    }

    public boolean isWithinHome() {
        return this.isWithinHome(this.blockPosition());
    }

    public boolean isWithinHome(BlockPos pos) {
        return this.homeRadius == -1 ? true : this.homePosition.distSqr(pos) < this.homeRadius * this.homeRadius;
    }

    public boolean isWithinHome(Vec3 pos) {
        return this.homeRadius == -1 ? true : this.homePosition.distToCenterSqr(pos) < this.homeRadius * this.homeRadius;
    }

    public void setHomeTo(BlockPos pos, int radius) {
        this.homePosition = pos;
        this.homeRadius = radius;
    }

    public BlockPos getHomePosition() {
        return this.homePosition;
    }

    public int getHomeRadius() {
        return this.homeRadius;
    }

    public void clearHome() {
        this.homeRadius = -1;
    }

    public boolean hasHome() {
        return this.homeRadius != -1;
    }

    @Nullable
    public <T extends Mob> T convertTo(
        EntityType<T> entityType, ConversionParams conversionParams, EntitySpawnReason spawnReason, ConversionParams.AfterConversion<T> afterConversion
    ) {
        if (this.isRemoved()) {
            return null;
        } else {
            T t = (T)entityType.create(this.level(), spawnReason);
            if (t == null) {
                return null;
            } else {
                conversionParams.type().convert(this, t, conversionParams);
                afterConversion.finalizeConversion(t);
                if (!preventConversionSpawns)
                if (this.level() instanceof ServerLevel serverlevel) {
                    serverlevel.addFreshEntity(t);
                }

                if (conversionParams.type().shouldDiscardAfterConversion()) {
                    this.discard();
                }

                return t;
            }
        }
    }

    @Nullable
    public <T extends Mob> T convertTo(EntityType<T> entityType, ConversionParams conversionParams, ConversionParams.AfterConversion<T> afterConversion) {
        return this.convertTo(entityType, conversionParams, EntitySpawnReason.CONVERSION, afterConversion);
    }

    @Nullable
    @Override
    public Leashable.LeashData getLeashData() {
        return this.leashData;
    }

    private void resetAngularLeashMomentum() {
        if (this.leashData != null) {
            this.leashData.angularMomentum = 0.0;
        }
    }

    @Override
    public void setLeashData(@Nullable Leashable.LeashData leashData) {
        this.leashData = leashData;
    }

    @Override
    public void onLeashRemoved() {
        if (this.getLeashData() == null) {
            this.clearHome();
        }
    }

    @Override
    public void leashTooFarBehaviour() {
        Leashable.super.leashTooFarBehaviour();
        this.goalSelector.disableControlFlag(Goal.Flag.MOVE);
    }

    @Override
    public boolean canBeLeashed() {
        return !(this instanceof Enemy);
    }

    @Override
    public boolean startRiding(Entity vehicle, boolean force, boolean sendGameEvent) {
        boolean flag = super.startRiding(vehicle, force, sendGameEvent);
        if (flag && this.isLeashed()) {
            this.dropLeash();
        }

        return flag;
    }

    @Override
    public boolean isEffectiveAi() {
        return super.isEffectiveAi() && !this.isNoAi();
    }

    /**
     * Set whether this Entity's AI is disabled
     */
    public void setNoAi(boolean noAi) {
        byte b0 = this.entityData.get(DATA_MOB_FLAGS_ID);
        this.entityData.set(DATA_MOB_FLAGS_ID, noAi ? (byte)(b0 | 1) : (byte)(b0 & -2));
    }

    public void setLeftHanded(boolean leftHanded) {
        byte b0 = this.entityData.get(DATA_MOB_FLAGS_ID);
        this.entityData.set(DATA_MOB_FLAGS_ID, leftHanded ? (byte)(b0 | 2) : (byte)(b0 & -3));
    }

    public void setAggressive(boolean aggressive) {
        byte b0 = this.entityData.get(DATA_MOB_FLAGS_ID);
        this.entityData.set(DATA_MOB_FLAGS_ID, aggressive ? (byte)(b0 | 4) : (byte)(b0 & -5));
    }

    public boolean isNoAi() {
        return (this.entityData.get(DATA_MOB_FLAGS_ID) & 1) != 0;
    }

    public boolean isLeftHanded() {
        return (this.entityData.get(DATA_MOB_FLAGS_ID) & 2) != 0;
    }

    public boolean isAggressive() {
        return (this.entityData.get(DATA_MOB_FLAGS_ID) & 4) != 0;
    }

    /**
     * Set whether this mob is a child.
     */
    public void setBaby(boolean baby) {
    }

    @Override
    public HumanoidArm getMainArm() {
        return this.isLeftHanded() ? HumanoidArm.LEFT : HumanoidArm.RIGHT;
    }

    public boolean isWithinMeleeAttackRange(LivingEntity entity) {
        return this.getAttackBoundingBox().intersects(entity.getHitbox());
    }

    protected AABB getAttackBoundingBox() {
        Entity entity = this.getVehicle();
        AABB aabb;
        if (entity != null) {
            AABB aabb1 = entity.getBoundingBox();
            AABB aabb2 = this.getBoundingBox();
            aabb = new AABB(
                Math.min(aabb2.minX, aabb1.minX),
                aabb2.minY,
                Math.min(aabb2.minZ, aabb1.minZ),
                Math.max(aabb2.maxX, aabb1.maxX),
                aabb2.maxY,
                Math.max(aabb2.maxZ, aabb1.maxZ)
            );
        } else {
            aabb = this.getBoundingBox();
        }

        return aabb.inflate(DEFAULT_ATTACK_REACH, 0.0, DEFAULT_ATTACK_REACH);
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity source) {
        float f = (float)this.getAttributeValue(Attributes.ATTACK_DAMAGE);
        ItemStack itemstack = this.getWeaponItem();
        DamageSource damagesource = Optional.ofNullable(itemstack.getItem().getDamageSource(this)).orElse(this.damageSources().mobAttack(this));
        f = EnchantmentHelper.modifyDamage(level, itemstack, source, damagesource, f);
        f += itemstack.getItem().getAttackDamageBonus(source, f, damagesource);
        boolean flag = source.hurtServer(level, damagesource, f);
        if (flag) {
            float f1 = this.getKnockback(source, damagesource);
            if (f1 > 0.0F && source instanceof LivingEntity livingentity) {
                livingentity.knockback(f1 * 0.5F, Mth.sin(this.getYRot() * (float) (Math.PI / 180.0)), -Mth.cos(this.getYRot() * (float) (Math.PI / 180.0)));
                this.setDeltaMovement(this.getDeltaMovement().multiply(0.6, 1.0, 0.6));
            }

            if (source instanceof LivingEntity livingentity1) {
                itemstack.hurtEnemy(livingentity1, this);
            }

            EnchantmentHelper.doPostAttackEffects(level, source, damagesource);
            this.setLastHurtMob(source);
            this.playAttackSound();
        }

        return flag;
    }

    protected void playAttackSound() {
    }

    protected boolean isSunBurnTick() {
        if (this.level().isBrightOutside() && !this.level().isClientSide()) {
            float f = this.getLightLevelDependentMagicValue();
            BlockPos blockpos = BlockPos.containing(this.getX(), this.getEyeY(), this.getZ());
            boolean flag = this.isInWaterOrRain() || this.isInPowderSnow || this.wasInPowderSnow;
            if (f > 0.5F && this.random.nextFloat() * 30.0F < (f - 0.4F) * 2.0F && !flag && this.level().canSeeSky(blockpos)) {
                return true;
            }
        }

        return false;
    }

    @Override
    @Deprecated // FORGE: use jumpInFluid instead
    protected void jumpInLiquid(TagKey<Fluid> fluidTag) {
        this.jumpInLiquidInternal(() -> super.jumpInLiquid(fluidTag));
    }

    private void jumpInLiquidInternal(Runnable onSuper) {
        if (this.getNavigation().canFloat()) {
            onSuper.run();
        } else {
            this.setDeltaMovement(this.getDeltaMovement().add(0.0, 0.3, 0.0));
        }
    }

    @Override
    public void jumpInFluid(net.neoforged.neoforge.fluids.FluidType type) {
        this.jumpInLiquidInternal(() -> super.jumpInFluid(type));
    }

    @VisibleForTesting
    public void removeFreeWill() {
        this.removeAllGoals(p_351790_ -> true);
        this.getBrain().removeAllBehaviors();
    }

    public void removeAllGoals(Predicate<Goal> filter) {
        this.goalSelector.removeAllGoals(filter);
    }

    @Override
    protected void removeAfterChangingDimensions() {
        super.removeAfterChangingDimensions();

        for (EquipmentSlot equipmentslot : EquipmentSlot.VALUES) {
            ItemStack itemstack = this.getItemBySlot(equipmentslot);
            if (!itemstack.isEmpty()) {
                itemstack.setCount(0);
            }
        }
    }

    @Nullable
    @Override
    public ItemStack getPickResult() {
        SpawnEggItem spawneggitem = SpawnEggItem.byId(this.getType());
        return spawneggitem == null ? null : new ItemStack(spawneggitem);
    }

    @Override
    protected void onAttributeUpdated(Holder<Attribute> attribute) {
        super.onAttributeUpdated(attribute);
        if (attribute.is(Attributes.FOLLOW_RANGE) || attribute.is(Attributes.TEMPT_RANGE)) {
            this.getNavigation().updatePathfinderMaxVisitedNodes();
        }
    }

    @Override
    public void registerDebugValues(ServerLevel level, DebugValueSource.Registration registration) {
        registration.register(DebugSubscriptions.ENTITY_PATHS, () -> {
            Path path = this.getNavigation().getPath();
            return path != null && path.debugData() != null ? new DebugPathInfo(path.copy(), this.getNavigation().getMaxDistanceToWaypoint()) : null;
        });
        registration.register(
            DebugSubscriptions.GOAL_SELECTORS,
            () -> {
                Set<WrappedGoal> set = this.goalSelector.getAvailableGoals();
                List<DebugGoalInfo.DebugGoal> list = new ArrayList<>(set.size());
                set.forEach(
                    p_448928_ -> list.add(
                        new DebugGoalInfo.DebugGoal(p_448928_.getPriority(), p_448928_.isRunning(), p_448928_.getGoal().getClass().getSimpleName())
                    )
                );
                return new DebugGoalInfo(list);
            }
        );
        if (!this.brain.isBrainDead()) {
            registration.register(DebugSubscriptions.BRAINS, () -> DebugBrainDump.takeBrainDump(level, this));
        }
    }

    /**
     * Returns the type of spawn that created this mob, if applicable.
     * If it could not be determined, this will return null.
     * <p>
     * This is set via {@link Mob#finalizeSpawn}, so you should not call this from within that method, instead using the parameter.
     */
    @Nullable
    public final EntitySpawnReason getSpawnType() {
        return this.spawnType;
    }

    /**
     * This method exists so that spawns can be cancelled from the {@link net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent FinalizeSpawnEvent}
     * without needing to hook up an additional handler for the {@link net.neoforged.neoforge.event.entity.EntityJoinLevelEvent EntityJoinLevelEvent}.
     * @return if this mob will be blocked from spawning during {@link Level#addFreshEntity(Entity)}
     * @apiNote Not public-facing API.
     */
    @org.jetbrains.annotations.ApiStatus.Internal
    public final boolean isSpawnCancelled() {
        return this.spawnCancelled;
    }

    /**
     * Marks this mob as being disallowed to spawn during {@link Level#addFreshEntity(Entity)}.<p>
     * @throws UnsupportedOperationException if this entity has already been {@link Entity#isAddedToLevel()} added to the level.
     * @apiNote Not public-facing API.
     */
    @org.jetbrains.annotations.ApiStatus.Internal
    public final void setSpawnCancelled(boolean cancel) {
        if (this.isAddedToLevel()) {
            throw new UnsupportedOperationException("Late invocations of Mob#setSpawnCancelled are not permitted.");
        }
        this.spawnCancelled = cancel;
    }
}
