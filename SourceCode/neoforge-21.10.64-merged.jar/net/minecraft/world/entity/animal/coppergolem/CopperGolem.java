package net.minecraft.world.entity.animal.coppergolem;

import com.mojang.serialization.Dynamic;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.RandomSource;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.ContainerUser;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Shearable;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.behavior.BehaviorUtils;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.animal.AbstractGolem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.CopperGolemStatueBlock;
import net.minecraft.world.level.block.WeatheringCopper;
import net.minecraft.world.level.block.entity.ContainerOpenersCounter;
import net.minecraft.world.level.block.entity.CopperGolemStatueBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

public class CopperGolem extends AbstractGolem implements ContainerUser, Shearable {
    private static final long IGNORE_WEATHERING_TICK = -2L;
    private static final long UNSET_WEATHERING_TICK = -1L;
    private static final int WEATHERING_TICK_FROM = 504000;
    private static final int WEATHERING_TICK_TO = 552000;
    private static final int SPIN_ANIMATION_MIN_COOLDOWN = 200;
    private static final int SPIN_ANIMATION_MAX_COOLDOWN = 240;
    private static final float SPIN_SOUND_TIME_INTERVAL_OFFSET = 10.0F;
    private static final float TURN_TO_STATUE_CHANCE = 0.0058F;
    private static final int SPAWN_COOLDOWN_MIN = 60;
    private static final int SPAWN_COOLDOWN_MAX = 100;
    private static final EntityDataAccessor<WeatheringCopper.WeatherState> DATA_WEATHER_STATE = SynchedEntityData.defineId(
        CopperGolem.class, EntityDataSerializers.WEATHERING_COPPER_STATE
    );
    private static final EntityDataAccessor<CopperGolemState> COPPER_GOLEM_STATE = SynchedEntityData.defineId(
        CopperGolem.class, EntityDataSerializers.COPPER_GOLEM_STATE
    );
    @Nullable
    private BlockPos openedChestPos;
    @Nullable
    private UUID lastLightningBoltUUID;
    private long nextWeatheringTick = -1L;
    private int idleAnimationStartTick = 0;
    private final AnimationState idleAnimationState = new AnimationState();
    private final AnimationState interactionGetItemAnimationState = new AnimationState();
    private final AnimationState interactionGetNoItemAnimationState = new AnimationState();
    private final AnimationState interactionDropItemAnimationState = new AnimationState();
    private final AnimationState interactionDropNoItemAnimationState = new AnimationState();
    public static final EquipmentSlot EQUIPMENT_SLOT_ANTENNA = EquipmentSlot.SADDLE;

    public CopperGolem(EntityType<? extends AbstractGolem> entityType, Level level) {
        super(entityType, level);
        this.getNavigation().setRequiredPathLength(48.0F);
        this.getNavigation().setCanOpenDoors(true);
        this.setPersistenceRequired();
        this.setState(CopperGolemState.IDLE);
        this.setPathfindingMalus(PathType.DANGER_FIRE, 16.0F);
        this.setPathfindingMalus(PathType.DANGER_OTHER, 16.0F);
        this.setPathfindingMalus(PathType.DAMAGE_FIRE, -1.0F);
        this.getBrain().setMemory(MemoryModuleType.TRANSPORT_ITEMS_COOLDOWN_TICKS, this.getRandom().nextInt(60, 100));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MOVEMENT_SPEED, 0.2F).add(Attributes.STEP_HEIGHT, 1.0).add(Attributes.MAX_HEALTH, 12.0);
    }

    public CopperGolemState getState() {
        return this.entityData.get(COPPER_GOLEM_STATE);
    }

    public void setState(CopperGolemState state) {
        this.entityData.set(COPPER_GOLEM_STATE, state);
    }

    public WeatheringCopper.WeatherState getWeatherState() {
        return this.entityData.get(DATA_WEATHER_STATE);
    }

    public void setWeatherState(WeatheringCopper.WeatherState weatherState) {
        this.entityData.set(DATA_WEATHER_STATE, weatherState);
    }

    public void setOpenedChestPos(BlockPos openedChestPos) {
        this.openedChestPos = openedChestPos;
    }

    public void clearOpenedChestPos() {
        this.openedChestPos = null;
    }

    public AnimationState getIdleAnimationState() {
        return this.idleAnimationState;
    }

    public AnimationState getInteractionGetItemAnimationState() {
        return this.interactionGetItemAnimationState;
    }

    public AnimationState getInteractionGetNoItemAnimationState() {
        return this.interactionGetNoItemAnimationState;
    }

    public AnimationState getInteractionDropItemAnimationState() {
        return this.interactionDropItemAnimationState;
    }

    public AnimationState getInteractionDropNoItemAnimationState() {
        return this.interactionDropNoItemAnimationState;
    }

    @Override
    protected Brain.Provider<CopperGolem> brainProvider() {
        return CopperGolemAi.brainProvider();
    }

    @Override
    protected Brain<?> makeBrain(Dynamic<?> dynamic) {
        return CopperGolemAi.makeBrain(this.brainProvider().makeBrain(dynamic));
    }

    @Override
    public Brain<CopperGolem> getBrain() {
        return (Brain<CopperGolem>)super.getBrain();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_WEATHER_STATE, WeatheringCopper.WeatherState.UNAFFECTED);
        builder.define(COPPER_GOLEM_STATE, CopperGolemState.IDLE);
    }

    @Override
    public void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putLong("next_weather_age", this.nextWeatheringTick);
        output.store("weather_state", WeatheringCopper.WeatherState.CODEC, this.getWeatherState());
    }

    @Override
    public void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.nextWeatheringTick = input.getLongOr("next_weather_age", -1L);
        this.setWeatherState(input.read("weather_state", WeatheringCopper.WeatherState.CODEC).orElse(WeatheringCopper.WeatherState.UNAFFECTED));
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        ProfilerFiller profilerfiller = Profiler.get();
        profilerfiller.push("copperGolemBrain");
        this.getBrain().tick(level, this);
        profilerfiller.pop();
        profilerfiller.push("copperGolemActivityUpdate");
        CopperGolemAi.updateActivity(this);
        profilerfiller.pop();
        super.customServerAiStep(level);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            if (!this.isNoAi()) {
                this.setupAnimationStates();
            }
        } else {
            this.updateWeathering((ServerLevel)this.level(), this.level().getRandom(), this.level().getGameTime());
        }
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack itemstack = player.getItemInHand(hand);
        if (itemstack.isEmpty()) {
            ItemStack itemstack1 = this.getMainHandItem();
            if (!itemstack1.isEmpty()) {
                BehaviorUtils.throwItem(this, itemstack1, player.position());
                this.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                return InteractionResult.SUCCESS;
            }
        }

        Level level = this.level();
        if (itemstack.is(Items.SHEARS) && this.readyForShearing()) {
            if (level instanceof ServerLevel serverlevel) {
                this.shear(serverlevel, SoundSource.PLAYERS, itemstack);
                this.gameEvent(GameEvent.SHEAR, player);
                itemstack.hurtAndBreak(1, player, hand);
            }

            return InteractionResult.SUCCESS;
        } else if (level.isClientSide()) {
            return InteractionResult.PASS;
        } else if (itemstack.is(Items.HONEYCOMB) && this.nextWeatheringTick != -2L) {
            level.levelEvent(this, 3003, this.blockPosition(), 0);
            this.nextWeatheringTick = -2L;
            this.usePlayerItem(player, hand, itemstack);
            return InteractionResult.SUCCESS_SERVER;
        } else if (itemstack.is(ItemTags.AXES) && this.nextWeatheringTick == -2L) {
            level.playSound(null, this, SoundEvents.AXE_SCRAPE, this.getSoundSource(), 1.0F, 1.0F);
            level.levelEvent(this, 3004, this.blockPosition(), 0);
            this.nextWeatheringTick = -1L;
            itemstack.hurtAndBreak(1, player, hand.asEquipmentSlot());
            return InteractionResult.SUCCESS_SERVER;
        } else {
            if (itemstack.is(ItemTags.AXES)) {
                WeatheringCopper.WeatherState weatheringcopper$weatherstate = this.getWeatherState();
                if (weatheringcopper$weatherstate != WeatheringCopper.WeatherState.UNAFFECTED) {
                    level.playSound(null, this, SoundEvents.AXE_SCRAPE, this.getSoundSource(), 1.0F, 1.0F);
                    level.levelEvent(this, 3005, this.blockPosition(), 0);
                    this.nextWeatheringTick = -1L;
                    this.entityData.set(DATA_WEATHER_STATE, weatheringcopper$weatherstate.previous(), true);
                    itemstack.hurtAndBreak(1, player, hand.asEquipmentSlot());
                    return InteractionResult.SUCCESS_SERVER;
                }
            }

            return super.mobInteract(player, hand);
        }
    }

    private void updateWeathering(ServerLevel level, RandomSource random, long dayTime) {
        if (this.nextWeatheringTick != -2L) {
            if (this.nextWeatheringTick == -1L) {
                this.nextWeatheringTick = dayTime + random.nextIntBetweenInclusive(504000, 552000);
            } else {
                WeatheringCopper.WeatherState weatheringcopper$weatherstate = this.entityData.get(DATA_WEATHER_STATE);
                boolean flag = weatheringcopper$weatherstate.equals(WeatheringCopper.WeatherState.OXIDIZED);
                if (dayTime >= this.nextWeatheringTick && !flag) {
                    WeatheringCopper.WeatherState weatheringcopper$weatherstate1 = weatheringcopper$weatherstate.next();
                    boolean flag1 = weatheringcopper$weatherstate1.equals(WeatheringCopper.WeatherState.OXIDIZED);
                    this.setWeatherState(weatheringcopper$weatherstate1);
                    this.nextWeatheringTick = flag1 ? 0L : this.nextWeatheringTick + random.nextIntBetweenInclusive(504000, 552000);
                }

                if (flag && this.canTurnToStatue(level)) {
                    this.turnToStatue(level);
                }
            }
        }
    }

    private boolean canTurnToStatue(Level level) {
        return level.getBlockState(this.blockPosition()).is(Blocks.AIR) && level.random.nextFloat() <= 0.0058F;
    }

    private void turnToStatue(ServerLevel level) {
        BlockPos blockpos = this.blockPosition();
        level.setBlock(
            blockpos,
            Blocks.OXIDIZED_COPPER_GOLEM_STATUE
                .defaultBlockState()
                .setValue(
                    CopperGolemStatueBlock.POSE, CopperGolemStatueBlock.Pose.values()[this.random.nextInt(0, CopperGolemStatueBlock.Pose.values().length)]
                )
                .setValue(CopperGolemStatueBlock.FACING, Direction.fromYRot(this.getYRot())),
            3
        );
        if (level.getBlockEntity(blockpos) instanceof CopperGolemStatueBlockEntity coppergolemstatueblockentity) {
            coppergolemstatueblockentity.createStatue(this);
            this.dropPreservedEquipment(level);
            this.discard();
            this.playSound(SoundEvents.COPPER_GOLEM_BECOME_STATUE);
            if (this.isLeashed()) {
                if (level.getGameRules().getBoolean(GameRules.RULE_DOENTITYDROPS)) {
                    this.dropLeash();
                } else {
                    this.removeLeash();
                }
            }
        }
    }

    private void setupAnimationStates() {
        switch (this.getState()) {
            case IDLE:
                this.interactionGetNoItemAnimationState.stop();
                this.interactionGetItemAnimationState.stop();
                this.interactionDropItemAnimationState.stop();
                this.interactionDropNoItemAnimationState.stop();
                if (this.idleAnimationStartTick == this.tickCount) {
                    this.idleAnimationState.start(this.tickCount);
                } else if (this.idleAnimationStartTick == 0) {
                    this.idleAnimationStartTick = this.tickCount + this.random.nextInt(200, 240);
                }

                if (this.tickCount == this.idleAnimationStartTick + 10.0F) {
                    this.playHeadSpinSound();
                    this.idleAnimationStartTick = 0;
                }
                break;
            case GETTING_ITEM:
                this.idleAnimationState.stop();
                this.idleAnimationStartTick = 0;
                this.interactionGetNoItemAnimationState.stop();
                this.interactionDropItemAnimationState.stop();
                this.interactionDropNoItemAnimationState.stop();
                this.interactionGetItemAnimationState.startIfStopped(this.tickCount);
                break;
            case GETTING_NO_ITEM:
                this.idleAnimationState.stop();
                this.idleAnimationStartTick = 0;
                this.interactionGetItemAnimationState.stop();
                this.interactionDropNoItemAnimationState.stop();
                this.interactionDropItemAnimationState.stop();
                this.interactionGetNoItemAnimationState.startIfStopped(this.tickCount);
                break;
            case DROPPING_ITEM:
                this.idleAnimationState.stop();
                this.idleAnimationStartTick = 0;
                this.interactionGetItemAnimationState.stop();
                this.interactionGetNoItemAnimationState.stop();
                this.interactionDropNoItemAnimationState.stop();
                this.interactionDropItemAnimationState.startIfStopped(this.tickCount);
                break;
            case DROPPING_NO_ITEM:
                this.idleAnimationState.stop();
                this.idleAnimationStartTick = 0;
                this.interactionGetItemAnimationState.stop();
                this.interactionGetNoItemAnimationState.stop();
                this.interactionDropItemAnimationState.stop();
                this.interactionDropNoItemAnimationState.startIfStopped(this.tickCount);
        }
    }

    public void spawn(WeatheringCopper.WeatherState weatherState) {
        this.setWeatherState(weatherState);
        this.playSpawnSound();
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(
        ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason spawnReason, @Nullable SpawnGroupData spawnGroupData
    ) {
        this.playSpawnSound();
        return super.finalizeSpawn(level, difficulty, spawnReason, spawnGroupData);
    }

    public void playSpawnSound() {
        this.playSound(SoundEvents.COPPER_GOLEM_SPAWN);
    }

    private void playHeadSpinSound() {
        if (!this.isSilent()) {
            this.level().playLocalSound(this.getX(), this.getY(), this.getZ(), this.getSpinHeadSound(), this.getSoundSource(), 1.0F, 1.0F, false);
        }
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return CopperGolemOxidationLevels.getOxidationLevel(this.getWeatherState()).hurtSound();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return CopperGolemOxidationLevels.getOxidationLevel(this.getWeatherState()).deathSound();
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        this.playSound(CopperGolemOxidationLevels.getOxidationLevel(this.getWeatherState()).stepSound(), 1.0F, 1.0F);
    }

    private SoundEvent getSpinHeadSound() {
        return CopperGolemOxidationLevels.getOxidationLevel(this.getWeatherState()).spinHeadSound();
    }

    @Override
    public Vec3 getLeashOffset() {
        return new Vec3(0.0, 0.75F * this.getEyeHeight(), 0.0);
    }

    @Override
    public boolean hasContainerOpen(ContainerOpenersCounter openCounter, BlockPos pos) {
        if (this.openedChestPos == null) {
            return false;
        } else {
            BlockState blockstate = this.level().getBlockState(this.openedChestPos);
            return this.openedChestPos.equals(pos)
                || blockstate.getBlock() instanceof ChestBlock
                    && blockstate.getValue(ChestBlock.TYPE) != ChestType.SINGLE
                    && ChestBlock.getConnectedBlockPos(this.openedChestPos, blockstate).equals(pos);
        }
    }

    @Override
    public double getContainerInteractionRange() {
        return 3.0;
    }

    @Override
    public void shear(ServerLevel level, SoundSource soundSource, ItemStack shears) {
        level.playSound(null, this, SoundEvents.COPPER_GOLEM_SHEAR, soundSource, 1.0F, 1.0F);
        ItemStack itemstack = this.getItemBySlot(EQUIPMENT_SLOT_ANTENNA);
        this.setItemSlot(EQUIPMENT_SLOT_ANTENNA, ItemStack.EMPTY);
        this.spawnAtLocation(level, itemstack, 1.5F);
    }

    @Override
    public boolean readyForShearing() {
        return this.isAlive() && this.getItemBySlot(EQUIPMENT_SLOT_ANTENNA).is(ItemTags.SHEARABLE_FROM_COPPER_GOLEM);
    }

    @Override
    protected void dropEquipment(ServerLevel level) {
        super.dropEquipment(level);
        this.dropPreservedEquipment(level);
    }

    @Override
    protected void actuallyHurt(ServerLevel level, DamageSource damageSource, float amount) {
        super.actuallyHurt(level, damageSource, amount);
        this.setState(CopperGolemState.IDLE);
    }

    @Override
    public void thunderHit(ServerLevel level, LightningBolt lightning) {
        super.thunderHit(level, lightning);
        UUID uuid = lightning.getUUID();
        if (!uuid.equals(this.lastLightningBoltUUID)) {
            this.lastLightningBoltUUID = uuid;
            WeatheringCopper.WeatherState weatheringcopper$weatherstate = this.getWeatherState();
            if (weatheringcopper$weatherstate != WeatheringCopper.WeatherState.UNAFFECTED) {
                this.nextWeatheringTick = -1L;
                this.entityData.set(DATA_WEATHER_STATE, weatheringcopper$weatherstate.previous(), true);
            }
        }
    }
}
