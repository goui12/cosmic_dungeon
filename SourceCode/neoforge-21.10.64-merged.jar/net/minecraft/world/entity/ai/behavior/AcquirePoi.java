package net.minecraft.world.entity.ai.behavior;

import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.behavior.declarative.MemoryAccessor;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.level.pathfinder.Path;
import org.apache.commons.lang3.mutable.MutableLong;

public class AcquirePoi {
    public static final int SCAN_RANGE = 48;

    public static BehaviorControl<PathfinderMob> create(
        Predicate<Holder<PoiType>> acquirablePois,
        MemoryModuleType<GlobalPos> acquiringMemory,
        boolean onlyIfAdult,
        Optional<Byte> entityEventId,
        BiPredicate<ServerLevel, BlockPos> predicate
    ) {
        return create(acquirablePois, acquiringMemory, acquiringMemory, onlyIfAdult, entityEventId, predicate);
    }

    public static BehaviorControl<PathfinderMob> create(
        Predicate<Holder<PoiType>> acquirablePois, MemoryModuleType<GlobalPos> acquiringMemory, boolean onlyIfAdult, Optional<Byte> entityEventId
    ) {
        return create(acquirablePois, acquiringMemory, acquiringMemory, onlyIfAdult, entityEventId, (p_390574_, p_390575_) -> true);
    }

    public static BehaviorControl<PathfinderMob> create(
        Predicate<Holder<PoiType>> acquirablePois,
        MemoryModuleType<GlobalPos> existingAbsentMemory,
        MemoryModuleType<GlobalPos> acquiringMemory,
        boolean onlyIfAdult,
        Optional<Byte> entityEventId,
        BiPredicate<ServerLevel, BlockPos> p_predicate
    ) {
        int i = 5;
        int j = 20;
        MutableLong mutablelong = new MutableLong(0L);
        Long2ObjectMap<AcquirePoi.JitteredLinearRetry> long2objectmap = new Long2ObjectOpenHashMap<>();
        OneShot<PathfinderMob> oneshot = BehaviorBuilder.create(
            p_390560_ -> p_390560_.group(p_390560_.absent(acquiringMemory))
                .apply(
                    p_390560_,
                    p_390582_ -> (p_390571_, p_390572_, p_390573_) -> {
                        if (onlyIfAdult && p_390572_.isBaby()) {
                            return false;
                        } else if (mutablelong.getValue() == 0L) {
                            mutablelong.setValue(p_390571_.getGameTime() + p_390571_.random.nextInt(20));
                            return false;
                        } else if (p_390571_.getGameTime() < mutablelong.getValue()) {
                            return false;
                        } else {
                            mutablelong.setValue(p_390573_ + 20L + p_390571_.getRandom().nextInt(20));
                            PoiManager poimanager = p_390571_.getPoiManager();
                            long2objectmap.long2ObjectEntrySet().removeIf(p_22338_ -> !p_22338_.getValue().isStillValid(p_390573_));
                            Predicate<BlockPos> predicate = p_258266_ -> {
                                AcquirePoi.JitteredLinearRetry acquirepoi$jitteredlinearretry = long2objectmap.get(p_258266_.asLong());
                                if (acquirepoi$jitteredlinearretry == null) {
                                    return true;
                                } else if (!acquirepoi$jitteredlinearretry.shouldRetry(p_390573_)) {
                                    return false;
                                } else {
                                    acquirepoi$jitteredlinearretry.markAttempt(p_390573_);
                                    return true;
                                }
                            };
                            Set<Pair<Holder<PoiType>, BlockPos>> set = poimanager.findAllClosestFirstWithType(
                                    acquirablePois, predicate, p_390572_.blockPosition(), 48, PoiManager.Occupancy.HAS_SPACE
                                )
                                .limit(5L)
                                .filter(p_390563_ -> p_predicate.test(p_390571_, p_390563_.getSecond()))
                                .collect(Collectors.toSet());
                            Path path = findPathToPois(p_390572_, set);
                            if (path != null && path.canReach()) {
                                BlockPos blockpos = path.getTarget();
                                poimanager.getType(blockpos).ifPresent(p_448937_ -> {
                                    poimanager.take(acquirablePois, (p_217108_, p_217109_) -> p_217109_.equals(blockpos), blockpos, 1);
                                    p_390582_.set(GlobalPos.of(p_390571_.dimension(), blockpos));
                                    entityEventId.ifPresent(p_147369_ -> p_390571_.broadcastEntityEvent(p_390572_, p_147369_));
                                    long2objectmap.clear();
                                    p_390571_.debugSynchronizers().updatePoi(blockpos);
                                });
                            } else {
                                for (Pair<Holder<PoiType>, BlockPos> pair : set) {
                                    long2objectmap.computeIfAbsent(
                                        pair.getSecond().asLong(), p_264881_ -> new AcquirePoi.JitteredLinearRetry(p_390571_.random, p_390573_)
                                    );
                                }
                            }

                            return true;
                        }
                    }
                )
        );
        return acquiringMemory == existingAbsentMemory
            ? oneshot
            : BehaviorBuilder.create(p_258269_ -> p_258269_.group(p_258269_.absent(existingAbsentMemory)).apply(p_258269_, p_258302_ -> oneshot));
    }

    @Nullable
    public static Path findPathToPois(Mob mob, Set<Pair<Holder<PoiType>, BlockPos>> poiPositions) {
        if (poiPositions.isEmpty()) {
            return null;
        } else {
            Set<BlockPos> set = new HashSet<>();
            int i = 1;

            for (Pair<Holder<PoiType>, BlockPos> pair : poiPositions) {
                i = Math.max(i, pair.getFirst().value().validRange());
                set.add(pair.getSecond());
            }

            return mob.getNavigation().createPath(set, i);
        }
    }

    static class JitteredLinearRetry {
        private static final int MIN_INTERVAL_INCREASE = 40;
        private static final int MAX_INTERVAL_INCREASE = 80;
        private static final int MAX_RETRY_PATHFINDING_INTERVAL = 400;
        private final RandomSource random;
        private long previousAttemptTimestamp;
        private long nextScheduledAttemptTimestamp;
        private int currentDelay;

        JitteredLinearRetry(RandomSource random, long timestamp) {
            this.random = random;
            this.markAttempt(timestamp);
        }

        public void markAttempt(long timestamp) {
            this.previousAttemptTimestamp = timestamp;
            int i = this.currentDelay + this.random.nextInt(40) + 40;
            this.currentDelay = Math.min(i, 400);
            this.nextScheduledAttemptTimestamp = timestamp + this.currentDelay;
        }

        public boolean isStillValid(long timestamp) {
            return timestamp - this.previousAttemptTimestamp < 400L;
        }

        public boolean shouldRetry(long timestamp) {
            return timestamp >= this.nextScheduledAttemptTimestamp;
        }

        @Override
        public String toString() {
            return "RetryMarker{, previousAttemptAt="
                + this.previousAttemptTimestamp
                + ", nextScheduledAttemptAt="
                + this.nextScheduledAttemptTimestamp
                + ", currentDelay="
                + this.currentDelay
                + "}";
        }
    }
}
