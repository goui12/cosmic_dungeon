package net.goui.cosmicdungeon.achievement.d1;

import net.goui.cosmicdungeon.Config;
import net.goui.cosmicdungeon.CosmicDungeonMod;
import net.goui.cosmicdungeon.achievement.CosmicAchievementIds;
import net.goui.cosmicdungeon.dungeon.DungeonInstanceSlots;
import net.goui.cosmicdungeon.dungeon.d1.D1Members;
import net.goui.cosmicdungeon.dungeon.d1.D1RunData;
import net.goui.cosmicdungeon.region.RegionRegistryData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractCandleBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.tags.BlockTags;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.VanillaGameEvent;
import java.util.*;

/** Q&A D71: any six different dyed colors, lit on chiseled tuff, in the authored room.
 * Scan the complete room incrementally. Revalidate all qualifying candles together before credit. */
@EventBusSubscriber(modid = CosmicDungeonMod.MOD_ID)
public final class SixfoldVigilTracker {
    private SixfoldVigilTracker() {}
    private static final Map<ResourceKey<Level>, Job> PENDING = new LinkedHashMap<>();
    private static final class Job {
        final long runId;
        final RegionRegistryData.Region bounds;
        final RoomScanCursor cursor;
        final Map<String, BlockPos> colors = new HashMap<>();
        boolean rescan;
        Job(long runId, RegionRegistryData.Region bounds) {
            this.runId = runId; this.bounds = bounds;
            cursor = new RoomScanCursor(bounds.min().getX(), bounds.min().getY(), bounds.min().getZ(),
                    bounds.max().getX(), bounds.max().getY(), bounds.max().getZ(), Config.CANDLE_SCAN_MAX_VOLUME.get());
        }
    }
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onCandleChanged(VanillaGameEvent event) {
        if (event.getLevel() instanceof ServerLevel level
                && (event.getVanillaEvent().equals(net.minecraft.world.level.gameevent.GameEvent.BLOCK_CHANGE)
                || event.getVanillaEvent().equals(net.minecraft.world.level.gameevent.GameEvent.BLOCK_PLACE)))
            schedule(level, BlockPos.containing(event.getEventPosition()));
    }
    public static void schedule(ServerLevel level, BlockPos pos) {
        if (!level.hasChunkAt(pos) || !level.getBlockState(pos).is(BlockTags.CANDLES)
                || !D1AchievementRegionService.inRegion(level, pos, D1AchievementRegionService.WITHER_ROOM)) return;
        var run = D1Members.run(level).orElse(null);
        if (run == null || !D1InstanceAchievements.hasOnlineParticipant(level, run)
                || D1RunData.get(level.getServer()).values(run.runId(), "awarded")
                .contains(CosmicAchievementIds.SIXFOLD_VIGIL.toString())) return;
        var bounds = RegionRegistryData.get(level).get(D1AchievementRegionService.WITHER_ROOM).orElse(null);
        if (bounds == null) return;
        var old = PENDING.get(level.dimension());
        if (old != null && old.runId == run.runId() && sameBounds(old.bounds, bounds)) { old.rescan = true; return; }
        try { PENDING.put(level.dimension(), new Job(run.runId(), bounds)); }
        catch (IllegalArgumentException invalid) {
            org.slf4j.LoggerFactory.getLogger("CosmicDungeon|SixfoldVigil")
                    .warn("Cannot scan configured Wither room in {}: {}", level.dimension().location(), invalid.getMessage());
        }
    }
    private static boolean sameBounds(RegionRegistryData.Region a, RegionRegistryData.Region b) {
        return a.dimensionId().equals(b.dimensionId()) && a.min().equals(b.min()) && a.max().equals(b.max())
                && a.createdOrder() == b.createdOrder();
    }
    private static String color(ServerLevel level, BlockPos pos) {
        if (!level.hasChunkAt(pos) || !level.hasChunkAt(pos.below())) return null;
        var state = level.getBlockState(pos);
        return state.is(BlockTags.CANDLES) && !state.is(Blocks.CANDLE) && AbstractCandleBlock.isLit(state)
                && level.getBlockState(pos.below()).is(Blocks.CHISELED_TUFF)
                ? BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString() : null;
    }
    @SubscribeEvent public static void tick(net.neoforged.neoforge.event.tick.ServerTickEvent.Post event) {
        int remaining = Config.CANDLE_SCAN_BUDGET.get();
        // Round-robin dimensions, with a global per-tick budget and one queued job per physical room.
        for (var dimension : new ArrayList<>(PENDING.keySet())) {
            if (remaining <= 0) break;
            var job = PENDING.remove(dimension);
            var level = event.getServer().getLevel(dimension);
            if (job == null || level == null) continue;
            var run = D1Members.run(level).orElse(null);
            var current = RegionRegistryData.get(level).get(D1AchievementRegionService.WITHER_ROOM).orElse(null);
            if (run == null || run.runId() != job.runId || current == null || !sameBounds(job.bounds, current)
                    || !current.dimensionId().equals(DungeonInstanceSlots.templateDimensionForPhysical(
                            event.getServer(), dimension).location().toString())
                    || D1RunData.get(event.getServer()).values(run.runId(), "awarded")
                            .contains(CosmicAchievementIds.SIXFOLD_VIGIL.toString())) continue;
            int slice = Math.min(remaining, 256);
            boolean awarded = false;
            while (slice-- > 0 && job.cursor.hasNext()) {
                remaining--;
                var point = job.cursor.next();
                var pos = new BlockPos(point.x(), point.y(), point.z());
                String color = color(level, pos);
                if (color == null) continue;
                job.colors.put(color, pos);
                if (job.colors.size() < Config.CANDLE_COLOR_COUNT.get()) continue;
                // Lighting/extinguishing/unloading during a multi-tick scan cannot create stale credit.
                job.colors.entrySet().removeIf(e -> !e.getKey().equals(color(level, e.getValue())));
                if (job.colors.size() >= Config.CANDLE_COLOR_COUNT.get()) {
                    D1InstanceAchievements.grant(level, run, CosmicAchievementIds.SIXFOLD_VIGIL);
                    awarded = true; break;
                }
            }
            if (awarded) continue;
            if (job.cursor.hasNext()) PENDING.put(dimension, job);
            else if (job.rescan) {
                try { PENDING.put(dimension, new Job(job.runId, current)); }
                catch (IllegalArgumentException invalid) { /* Operator changed the limit; next lighting event diagnoses it. */ }
            }
        }
    }
    @SubscribeEvent public static void stopped(net.neoforged.neoforge.event.server.ServerStoppedEvent event) { PENDING.clear(); }
    // TODO(D71/M77): source 1YkiyPfomO7rnSenj4A9tySkh3e5mcKy4JNXSXv3ScR0 defines zero/one/two-Wither variants.
    // Cameron explicitly deferred Withers. Do not grant After Dissolution, Lone Adversary or Twin
    // Manifestation from nearby entity counts. Implement the authorized encounter lifecycle,
    // distinct summon identities, color condition and eligible instance recipients together later.
}
