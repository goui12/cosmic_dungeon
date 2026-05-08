package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.function.TriConsumer;

public class TransportItemsBetweenContainers extends Behavior<PathfinderMob> {
    public static final int TARGET_INTERACTION_TIME = 60;
    private static final int VISITED_POSITIONS_MEMORY_TIME = 6000;
    private static final int TRANSPORTED_ITEM_MAX_STACK_SIZE = 16;
    private static final int MAX_VISITED_POSITIONS = 10;
    private static final int MAX_UNREACHABLE_POSITIONS = 50;
    private static final int PASSENGER_MOB_TARGET_SEARCH_DISTANCE = 1;
    private static final int IDLE_COOLDOWN = 140;
    private static final double CLOSE_ENOUGH_TO_START_QUEUING_DISTANCE = 3.0;
    private static final double CLOSE_ENOUGH_TO_START_INTERACTING_WITH_TARGET_DISTANCE = 0.5;
    private static final double CLOSE_ENOUGH_TO_START_INTERACTING_WITH_TARGET_PATH_END_DISTANCE = 1.0;
    private static final double CLOSE_ENOUGH_TO_CONTINUE_INTERACTING_WITH_TARGET = 2.0;
    private final float speedModifier;
    private final int horizontalSearchDistance;
    private final int verticalSearchDistance;
    private final Predicate<BlockState> sourceBlockType;
    private final Predicate<BlockState> destinationBlockType;
    private final Predicate<TransportItemsBetweenContainers.TransportItemTarget> shouldQueueForTarget;
    private final Consumer<PathfinderMob> onStartTravelling;
    private final Map<TransportItemsBetweenContainers.ContainerInteractionState, TransportItemsBetweenContainers.OnTargetReachedInteraction> onTargetInteractionActions;
    @Nullable
    private TransportItemsBetweenContainers.TransportItemTarget target = null;
    private TransportItemsBetweenContainers.TransportItemState state;
    @Nullable
    private TransportItemsBetweenContainers.ContainerInteractionState interactionState;
    private int ticksSinceReachingTarget;

    public TransportItemsBetweenContainers(
        float speedModifier,
        Predicate<BlockState> sourceBlockType,
        Predicate<BlockState> destinationBlockType,
        int horizontalSearchDistance,
        int verticalSearchDistance,
        Map<TransportItemsBetweenContainers.ContainerInteractionState, TransportItemsBetweenContainers.OnTargetReachedInteraction> onTargetInteractionActions,
        Consumer<PathfinderMob> onStartTravelling,
        Predicate<TransportItemsBetweenContainers.TransportItemTarget> shouldQueueForTarget
    ) {
        super(
            ImmutableMap.of(
                MemoryModuleType.VISITED_BLOCK_POSITIONS,
                MemoryStatus.REGISTERED,
                MemoryModuleType.UNREACHABLE_TRANSPORT_BLOCK_POSITIONS,
                MemoryStatus.REGISTERED,
                MemoryModuleType.TRANSPORT_ITEMS_COOLDOWN_TICKS,
                MemoryStatus.VALUE_ABSENT,
                MemoryModuleType.IS_PANICKING,
                MemoryStatus.VALUE_ABSENT
            )
        );
        this.speedModifier = speedModifier;
        this.sourceBlockType = sourceBlockType;
        this.destinationBlockType = destinationBlockType;
        this.horizontalSearchDistance = horizontalSearchDistance;
        this.verticalSearchDistance = verticalSearchDistance;
        this.onStartTravelling = onStartTravelling;
        this.shouldQueueForTarget = shouldQueueForTarget;
        this.onTargetInteractionActions = onTargetInteractionActions;
        this.state = TransportItemsBetweenContainers.TransportItemState.TRAVELLING;
    }

    protected void start(ServerLevel level, PathfinderMob entity, long gameTime) {
        if (entity.getNavigation() instanceof GroundPathNavigation groundpathnavigation) {
            groundpathnavigation.setCanPathToTargetsBelowSurface(true);
        }
    }

    protected boolean checkExtraStartConditions(ServerLevel level, PathfinderMob owner) {
        return !owner.isLeashed();
    }

    protected boolean canStillUse(ServerLevel level, PathfinderMob entity, long gameTime) {
        return entity.getBrain().getMemory(MemoryModuleType.TRANSPORT_ITEMS_COOLDOWN_TICKS).isEmpty() && !entity.isPanicking() && !entity.isLeashed();
    }

    @Override
    protected boolean timedOut(long gameTime) {
        return false;
    }

    protected void tick(ServerLevel level, PathfinderMob owner, long gameTime) {
        boolean flag = this.updateInvalidTarget(level, owner);
        if (this.target == null) {
            this.stop(level, owner, gameTime);
        } else if (!flag) {
            if (this.state.equals(TransportItemsBetweenContainers.TransportItemState.QUEUING)) {
                this.onQueuingForTarget(this.target, level, owner);
            }

            if (this.state.equals(TransportItemsBetweenContainers.TransportItemState.TRAVELLING)) {
                this.onTravelToTarget(this.target, level, owner);
            }

            if (this.state.equals(TransportItemsBetweenContainers.TransportItemState.INTERACTING)) {
                this.onReachedTarget(this.target, level, owner);
            }
        }
    }

    private boolean updateInvalidTarget(ServerLevel level, PathfinderMob mob) {
        if (!this.hasValidTarget(level, mob)) {
            this.stopTargetingCurrentTarget(mob);
            Optional<TransportItemsBetweenContainers.TransportItemTarget> optional = this.getTransportTarget(level, mob);
            if (optional.isPresent()) {
                this.target = optional.get();
                this.onStartTravelling(mob);
                this.setVisitedBlockPos(mob, level, this.target.pos);
                return true;
            } else {
                this.enterCooldownAfterNoMatchingTargetFound(mob);
                return true;
            }
        } else {
            return false;
        }
    }

    private void onQueuingForTarget(TransportItemsBetweenContainers.TransportItemTarget target, Level level, PathfinderMob mob) {
        if (!this.isAnotherMobInteractingWithTarget(target, level)) {
            this.resumeTravelling(mob);
        }
    }

    protected void onTravelToTarget(TransportItemsBetweenContainers.TransportItemTarget target, Level level, PathfinderMob mob) {
        if (this.isWithinTargetDistance(3.0, target, level, mob, this.getCenterPos(mob))
            && this.isAnotherMobInteractingWithTarget(target, level)) {
            this.startQueuing(mob);
        } else if (this.isWithinTargetDistance(getInteractionRange(mob), target, level, mob, this.getCenterPos(mob))) {
            this.startOnReachedTargetInteraction(target, mob);
        } else {
            this.walkTowardsTarget(mob);
        }
    }

    private Vec3 getCenterPos(PathfinderMob mob) {
        return this.setMiddleYPosition(mob, mob.position());
    }

    protected void onReachedTarget(TransportItemsBetweenContainers.TransportItemTarget target, Level level, PathfinderMob mob) {
        if (!this.isWithinTargetDistance(2.0, target, level, mob, this.getCenterPos(mob))) {
            this.onStartTravelling(mob);
        } else {
            this.ticksSinceReachingTarget++;
            this.onTargetInteraction(target, mob);
            if (this.ticksSinceReachingTarget >= 60) {
                this.doReachedTargetInteraction(
                    mob,
                    target.container,
                    this::pickUpItems,
                    (p_442454_, p_442455_) -> this.stopTargetingCurrentTarget(mob),
                    this::putDownItem,
                    (p_442440_, p_442441_) -> this.stopTargetingCurrentTarget(mob)
                );
                this.onStartTravelling(mob);
            }
        }
    }

    private void startQueuing(PathfinderMob mob) {
        this.stopInPlace(mob);
        this.setTransportingState(TransportItemsBetweenContainers.TransportItemState.QUEUING);
    }

    private void resumeTravelling(PathfinderMob mob) {
        this.setTransportingState(TransportItemsBetweenContainers.TransportItemState.TRAVELLING);
        this.walkTowardsTarget(mob);
    }

    private void walkTowardsTarget(PathfinderMob mob) {
        if (this.target != null) {
            BehaviorUtils.setWalkAndLookTargetMemories(mob, this.target.pos, this.speedModifier, 0);
        }
    }

    private void startOnReachedTargetInteraction(TransportItemsBetweenContainers.TransportItemTarget target, PathfinderMob mob) {
        this.doReachedTargetInteraction(
            mob,
            target.container,
            this.onReachedInteraction(TransportItemsBetweenContainers.ContainerInteractionState.PICKUP_ITEM),
            this.onReachedInteraction(TransportItemsBetweenContainers.ContainerInteractionState.PICKUP_NO_ITEM),
            this.onReachedInteraction(TransportItemsBetweenContainers.ContainerInteractionState.PLACE_ITEM),
            this.onReachedInteraction(TransportItemsBetweenContainers.ContainerInteractionState.PLACE_NO_ITEM)
        );
        this.setTransportingState(TransportItemsBetweenContainers.TransportItemState.INTERACTING);
    }

    private void onStartTravelling(PathfinderMob mob) {
        this.onStartTravelling.accept(mob);
        this.setTransportingState(TransportItemsBetweenContainers.TransportItemState.TRAVELLING);
        this.interactionState = null;
        this.ticksSinceReachingTarget = 0;
    }

    private BiConsumer<PathfinderMob, Container> onReachedInteraction(TransportItemsBetweenContainers.ContainerInteractionState interactionState) {
        return (p_435151_, p_432961_) -> this.setInteractionState(interactionState);
    }

    private void setTransportingState(TransportItemsBetweenContainers.TransportItemState transportingState) {
        this.state = transportingState;
    }

    private void setInteractionState(TransportItemsBetweenContainers.ContainerInteractionState interactionState) {
        this.interactionState = interactionState;
    }

    private void onTargetInteraction(TransportItemsBetweenContainers.TransportItemTarget target, PathfinderMob mob) {
        mob.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new BlockPosTracker(target.pos));
        this.stopInPlace(mob);
        if (this.interactionState != null) {
            Optional.ofNullable(this.onTargetInteractionActions.get(this.interactionState))
                .ifPresent(p_435805_ -> p_435805_.accept(mob, target, this.ticksSinceReachingTarget));
        }
    }

    private void doReachedTargetInteraction(
        PathfinderMob mob,
        Container container,
        BiConsumer<PathfinderMob, Container> pickupItem,
        BiConsumer<PathfinderMob, Container> pickupNoItem,
        BiConsumer<PathfinderMob, Container> placeItem,
        BiConsumer<PathfinderMob, Container> placeNoItem
    ) {
        if (isPickingUpItems(mob)) {
            if (matchesGettingItemsRequirement(container)) {
                pickupItem.accept(mob, container);
            } else {
                pickupNoItem.accept(mob, container);
            }
        } else if (matchesLeavingItemsRequirement(mob, container)) {
            placeItem.accept(mob, container);
        } else {
            placeNoItem.accept(mob, container);
        }
    }

    private Optional<TransportItemsBetweenContainers.TransportItemTarget> getTransportTarget(ServerLevel level, PathfinderMob mob) {
        AABB aabb = this.getTargetSearchArea(mob);
        Set<GlobalPos> set = getVisitedPositions(mob);
        Set<GlobalPos> set1 = getUnreachablePositions(mob);
        List<ChunkPos> list = ChunkPos.rangeClosed(new ChunkPos(mob.blockPosition()), Math.floorDiv(this.getHorizontalSearchDistance(mob), 16) + 1)
            .toList();
        TransportItemsBetweenContainers.TransportItemTarget transportitemsbetweencontainers$transportitemtarget = null;
        double d0 = Float.MAX_VALUE;

        for (ChunkPos chunkpos : list) {
            LevelChunk levelchunk = level.getChunkSource().getChunkNow(chunkpos.x, chunkpos.z);
            if (levelchunk != null) {
                for (BlockEntity blockentity : levelchunk.getBlockEntities().values()) {
                    if (blockentity instanceof ChestBlockEntity chestblockentity) {
                        double d1 = chestblockentity.getBlockPos().distToCenterSqr(mob.position());
                        if (d1 < d0) {
                            TransportItemsBetweenContainers.TransportItemTarget transportitemsbetweencontainers$transportitemtarget1 = this.isTargetValidToPick(
                                mob, level, chestblockentity, set, set1, aabb
                            );
                            if (transportitemsbetweencontainers$transportitemtarget1 != null) {
                                transportitemsbetweencontainers$transportitemtarget = transportitemsbetweencontainers$transportitemtarget1;
                                d0 = d1;
                            }
                        }
                    }
                }
            }
        }

        return transportitemsbetweencontainers$transportitemtarget == null
            ? Optional.empty()
            : Optional.of(transportitemsbetweencontainers$transportitemtarget);
    }

    @Nullable
    private TransportItemsBetweenContainers.TransportItemTarget isTargetValidToPick(
        PathfinderMob mob, Level level, BlockEntity blockEntity, Set<GlobalPos> visited, Set<GlobalPos> unreachable, AABB searchArea
    ) {
        BlockPos blockpos = blockEntity.getBlockPos();
        boolean flag = searchArea.contains(blockpos.getX(), blockpos.getY(), blockpos.getZ());
        if (!flag) {
            return null;
        } else {
            TransportItemsBetweenContainers.TransportItemTarget transportitemsbetweencontainers$transportitemtarget = TransportItemsBetweenContainers.TransportItemTarget.tryCreatePossibleTarget(
                blockEntity, level
            );
            if (transportitemsbetweencontainers$transportitemtarget == null) {
                return null;
            } else {
                boolean flag1 = this.isWantedBlock(mob, transportitemsbetweencontainers$transportitemtarget.state)
                    && !this.isPositionAlreadyVisited(visited, unreachable, transportitemsbetweencontainers$transportitemtarget, level)
                    && !this.isContainerLocked(transportitemsbetweencontainers$transportitemtarget);
                return flag1 ? transportitemsbetweencontainers$transportitemtarget : null;
            }
        }
    }

    private boolean isContainerLocked(TransportItemsBetweenContainers.TransportItemTarget target) {
        return target.blockEntity instanceof BaseContainerBlockEntity basecontainerblockentity && basecontainerblockentity.isLocked();
    }

    private boolean hasValidTarget(Level level, PathfinderMob mob) {
        boolean flag = this.target != null && this.isWantedBlock(mob, this.target.state) && this.targetHasNotChanged(level, this.target);
        if (flag && !this.isTargetBlocked(level, this.target)) {
            if (!this.state.equals(TransportItemsBetweenContainers.TransportItemState.TRAVELLING)) {
                return true;
            }

            if (this.hasValidTravellingPath(level, this.target, mob)) {
                return true;
            }

            this.markVisitedBlockPosAsUnreachable(mob, level, this.target.pos);
        }

        return false;
    }

    private boolean hasValidTravellingPath(Level level, TransportItemsBetweenContainers.TransportItemTarget target, PathfinderMob mob) {
        Path path = mob.getNavigation().getPath() == null ? mob.getNavigation().createPath(target.pos, 0) : mob.getNavigation().getPath();
        Vec3 vec3 = this.getPositionToReachTargetFrom(path, mob);
        boolean flag = this.isWithinTargetDistance(getInteractionRange(mob), target, level, mob, vec3);
        boolean flag1 = path == null && !flag;
        return flag1 || this.targetIsReachableFromPosition(level, flag, vec3, target, mob);
    }

    private Vec3 getPositionToReachTargetFrom(@Nullable Path path, PathfinderMob mob) {
        boolean flag = path == null || path.getEndNode() == null;
        Vec3 vec3 = flag ? mob.position() : path.getEndNode().asBlockPos().getBottomCenter();
        return this.setMiddleYPosition(mob, vec3);
    }

    private Vec3 setMiddleYPosition(PathfinderMob mob, Vec3 pos) {
        return pos.add(0.0, mob.getBoundingBox().getYsize() / 2.0, 0.0);
    }

    private boolean isTargetBlocked(Level level, TransportItemsBetweenContainers.TransportItemTarget target) {
        return ChestBlock.isChestBlockedAt(level, target.pos);
    }

    private boolean targetHasNotChanged(Level level, TransportItemsBetweenContainers.TransportItemTarget target) {
        return target.blockEntity.equals(level.getBlockEntity(target.pos));
    }

    private Stream<TransportItemsBetweenContainers.TransportItemTarget> getConnectedTargets(
        TransportItemsBetweenContainers.TransportItemTarget target, Level level
    ) {
        if (target.state.getValueOrElse(ChestBlock.TYPE, ChestType.SINGLE) != ChestType.SINGLE) {
            TransportItemsBetweenContainers.TransportItemTarget transportitemsbetweencontainers$transportitemtarget = TransportItemsBetweenContainers.TransportItemTarget.tryCreatePossibleTarget(
                ChestBlock.getConnectedBlockPos(target.pos, target.state), level
            );
            return transportitemsbetweencontainers$transportitemtarget != null
                ? Stream.of(target, transportitemsbetweencontainers$transportitemtarget)
                : Stream.of(target);
        } else {
            return Stream.of(target);
        }
    }

    private AABB getTargetSearchArea(PathfinderMob mob) {
        int i = this.getHorizontalSearchDistance(mob);
        return new AABB(mob.blockPosition()).inflate(i, this.getVerticalSearchDistance(mob), i);
    }

    private int getHorizontalSearchDistance(PathfinderMob mob) {
        return mob.isPassenger() ? 1 : this.horizontalSearchDistance;
    }

    private int getVerticalSearchDistance(PathfinderMob mob) {
        return mob.isPassenger() ? 1 : this.verticalSearchDistance;
    }

    private static Set<GlobalPos> getVisitedPositions(PathfinderMob mob) {
        return mob.getBrain().getMemory(MemoryModuleType.VISITED_BLOCK_POSITIONS).orElse(Set.of());
    }

    private static Set<GlobalPos> getUnreachablePositions(PathfinderMob mob) {
        return mob.getBrain().getMemory(MemoryModuleType.UNREACHABLE_TRANSPORT_BLOCK_POSITIONS).orElse(Set.of());
    }

    private boolean isPositionAlreadyVisited(
        Set<GlobalPos> visited, Set<GlobalPos> unreachable, TransportItemsBetweenContainers.TransportItemTarget target, Level level
    ) {
        return this.getConnectedTargets(target, level)
            .map(p_433162_ -> new GlobalPos(level.dimension(), p_433162_.pos))
            .anyMatch(p_442446_ -> visited.contains(p_442446_) || unreachable.contains(p_442446_));
    }

    private static boolean hasFinishedPath(PathfinderMob mob) {
        return mob.getNavigation().getPath() != null && mob.getNavigation().getPath().isDone();
    }

    protected void setVisitedBlockPos(PathfinderMob mob, Level level, BlockPos pos) {
        Set<GlobalPos> set = new HashSet<>(getVisitedPositions(mob));
        set.add(new GlobalPos(level.dimension(), pos));
        if (set.size() > 10) {
            this.enterCooldownAfterNoMatchingTargetFound(mob);
        } else {
            mob.getBrain().setMemoryWithExpiry(MemoryModuleType.VISITED_BLOCK_POSITIONS, set, 6000L);
        }
    }

    protected void markVisitedBlockPosAsUnreachable(PathfinderMob mob, Level level, BlockPos pos) {
        Set<GlobalPos> set = new HashSet<>(getVisitedPositions(mob));
        set.remove(new GlobalPos(level.dimension(), pos));
        Set<GlobalPos> set1 = new HashSet<>(getUnreachablePositions(mob));
        set1.add(new GlobalPos(level.dimension(), pos));
        if (set1.size() > 50) {
            this.enterCooldownAfterNoMatchingTargetFound(mob);
        } else {
            mob.getBrain().setMemoryWithExpiry(MemoryModuleType.VISITED_BLOCK_POSITIONS, set, 6000L);
            mob.getBrain().setMemoryWithExpiry(MemoryModuleType.UNREACHABLE_TRANSPORT_BLOCK_POSITIONS, set1, 6000L);
        }
    }

    private boolean isWantedBlock(PathfinderMob mob, BlockState state) {
        return isPickingUpItems(mob) ? this.sourceBlockType.test(state) : this.destinationBlockType.test(state);
    }

    private static double getInteractionRange(PathfinderMob mob) {
        return hasFinishedPath(mob) ? 1.0 : 0.5;
    }

    private boolean isWithinTargetDistance(
        double distance, TransportItemsBetweenContainers.TransportItemTarget target, Level level, PathfinderMob mob, Vec3 center
    ) {
        AABB aabb = mob.getBoundingBox();
        AABB aabb1 = AABB.ofSize(center, aabb.getXsize(), aabb.getYsize(), aabb.getZsize());
        return target.state.getCollisionShape(level, target.pos).bounds().inflate(distance, 0.5, distance).move(target.pos).intersects(aabb1);
    }

    private boolean targetIsReachableFromPosition(
        Level level, boolean withinDistance, Vec3 targetPos, TransportItemsBetweenContainers.TransportItemTarget target, PathfinderMob mob
    ) {
        return withinDistance && this.canSeeAnyTargetSide(target, level, mob, targetPos);
    }

    private boolean canSeeAnyTargetSide(TransportItemsBetweenContainers.TransportItemTarget target, Level level, PathfinderMob mob, Vec3 pos) {
        Vec3 vec3 = target.pos.getCenter();
        return Direction.stream()
            .map(p_450860_ -> vec3.add(0.5 * p_450860_.getStepX(), 0.5 * p_450860_.getStepY(), 0.5 * p_450860_.getStepZ()))
            .map(p_450864_ -> level.clip(new ClipContext(pos, p_450864_, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mob)))
            .anyMatch(p_450866_ -> p_450866_.getType() == HitResult.Type.BLOCK && p_450866_.getBlockPos().equals(target.pos));
    }

    private boolean isAnotherMobInteractingWithTarget(TransportItemsBetweenContainers.TransportItemTarget target, Level level) {
        return this.getConnectedTargets(target, level).anyMatch(this.shouldQueueForTarget);
    }

    private static boolean isPickingUpItems(PathfinderMob mob) {
        return mob.getMainHandItem().isEmpty();
    }

    private static boolean matchesGettingItemsRequirement(Container container) {
        return !container.isEmpty();
    }

    private static boolean matchesLeavingItemsRequirement(PathfinderMob mob, Container container) {
        return container.isEmpty() || hasItemMatchingHandItem(mob, container);
    }

    private static boolean hasItemMatchingHandItem(PathfinderMob mob, Container container) {
        ItemStack itemstack = mob.getMainHandItem();

        for (ItemStack itemstack1 : container) {
            if (ItemStack.isSameItem(itemstack1, itemstack)) {
                return true;
            }
        }

        return false;
    }

    private void pickUpItems(PathfinderMob mob, Container container) {
        mob.setItemSlot(EquipmentSlot.MAINHAND, pickupItemFromContainer(container));
        mob.setGuaranteedDrop(EquipmentSlot.MAINHAND);
        container.setChanged();
        this.clearMemoriesAfterMatchingTargetFound(mob);
    }

    private void putDownItem(PathfinderMob mob, Container container) {
        ItemStack itemstack = addItemsToContainer(mob, container);
        container.setChanged();
        mob.setItemSlot(EquipmentSlot.MAINHAND, itemstack);
        if (itemstack.isEmpty()) {
            this.clearMemoriesAfterMatchingTargetFound(mob);
        } else {
            this.stopTargetingCurrentTarget(mob);
        }
    }

    private static ItemStack pickupItemFromContainer(Container container) {
        int i = 0;

        for (ItemStack itemstack : container) {
            if (!itemstack.isEmpty()) {
                int j = Math.min(itemstack.getCount(), 16);
                return container.removeItem(i, j);
            }

            i++;
        }

        return ItemStack.EMPTY;
    }

    private static ItemStack addItemsToContainer(PathfinderMob mob, Container container) {
        int i = 0;
        ItemStack itemstack = mob.getMainHandItem();

        for (ItemStack itemstack1 : container) {
            if (itemstack1.isEmpty()) {
                container.setItem(i, itemstack);
                return ItemStack.EMPTY;
            }

            if (ItemStack.isSameItemSameComponents(itemstack1, itemstack) && itemstack1.getCount() < itemstack1.getMaxStackSize()) {
                int j = itemstack1.getMaxStackSize() - itemstack1.getCount();
                int k = Math.min(j, itemstack.getCount());
                itemstack1.setCount(itemstack1.getCount() + k);
                itemstack.setCount(itemstack.getCount() - j);
                container.setItem(i, itemstack1);
                if (itemstack.isEmpty()) {
                    return ItemStack.EMPTY;
                }
            }

            i++;
        }

        return itemstack;
    }

    protected void stopTargetingCurrentTarget(PathfinderMob mob) {
        this.ticksSinceReachingTarget = 0;
        this.target = null;
        mob.getNavigation().stop();
        mob.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
    }

    protected void clearMemoriesAfterMatchingTargetFound(PathfinderMob mob) {
        this.stopTargetingCurrentTarget(mob);
        mob.getBrain().eraseMemory(MemoryModuleType.VISITED_BLOCK_POSITIONS);
        mob.getBrain().eraseMemory(MemoryModuleType.UNREACHABLE_TRANSPORT_BLOCK_POSITIONS);
    }

    private void enterCooldownAfterNoMatchingTargetFound(PathfinderMob mob) {
        this.stopTargetingCurrentTarget(mob);
        mob.getBrain().setMemory(MemoryModuleType.TRANSPORT_ITEMS_COOLDOWN_TICKS, 140);
        mob.getBrain().eraseMemory(MemoryModuleType.VISITED_BLOCK_POSITIONS);
        mob.getBrain().eraseMemory(MemoryModuleType.UNREACHABLE_TRANSPORT_BLOCK_POSITIONS);
    }

    protected void stop(ServerLevel level, PathfinderMob entity, long gameTime) {
        this.onStartTravelling(entity);
        if (entity.getNavigation() instanceof GroundPathNavigation groundpathnavigation) {
            groundpathnavigation.setCanPathToTargetsBelowSurface(false);
        }
    }

    private void stopInPlace(PathfinderMob mob) {
        mob.getNavigation().stop();
        mob.setXxa(0.0F);
        mob.setYya(0.0F);
        mob.setSpeed(0.0F);
        mob.setDeltaMovement(0.0, mob.getDeltaMovement().y, 0.0);
    }

    public static enum ContainerInteractionState {
        PICKUP_ITEM,
        PICKUP_NO_ITEM,
        PLACE_ITEM,
        PLACE_NO_ITEM;
    }

    @FunctionalInterface
    public interface OnTargetReachedInteraction extends TriConsumer<PathfinderMob, TransportItemsBetweenContainers.TransportItemTarget, Integer> {
    }

    public static enum TransportItemState {
        TRAVELLING,
        QUEUING,
        INTERACTING;
    }

    public record TransportItemTarget(BlockPos pos, Container container, BlockEntity blockEntity, BlockState state) {
        @Nullable
        public static TransportItemsBetweenContainers.TransportItemTarget tryCreatePossibleTarget(BlockEntity blockEntity, Level level) {
            BlockPos blockpos = blockEntity.getBlockPos();
            BlockState blockstate = blockEntity.getBlockState();
            Container container = getBlockEntityContainer(blockEntity, blockstate, level, blockpos);
            return container != null ? new TransportItemsBetweenContainers.TransportItemTarget(blockpos, container, blockEntity, blockstate) : null;
        }

        @Nullable
        public static TransportItemsBetweenContainers.TransportItemTarget tryCreatePossibleTarget(BlockPos pos, Level level) {
            BlockEntity blockentity = level.getBlockEntity(pos);
            return blockentity == null ? null : tryCreatePossibleTarget(blockentity, level);
        }

        @Nullable
        private static Container getBlockEntityContainer(BlockEntity blockEntity, BlockState state, Level level, BlockPos pos) {
            if (state.getBlock() instanceof ChestBlock chestblock) {
                return ChestBlock.getContainer(chestblock, state, level, pos, false);
            } else {
                return blockEntity instanceof Container container ? container : null;
            }
        }
    }
}
