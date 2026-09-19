package net.goui.cosmicdungeon.dungeon.d1;

import net.goui.cosmicdungeon.CosmicDungeonMod;
import net.goui.cosmicdungeon.block.ModBlocks;
import net.goui.cosmicdungeon.progression.ProgressionService;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import java.util.*;

/** Authoritative world-source accounting; dropping/re-picking an item cannot create extra credit. */
@EventBusSubscriber(modid = CosmicDungeonMod.MOD_ID)
public final class D1LesserBloomEvents {
    private record Harvest(long runId, UUID actor, BlockPos pos) {}
    private static final Map<ServerLevel, List<Harvest>> PENDING = new IdentityHashMap<>();
    private D1LesserBloomEvents() {}
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onBreak(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)
                || !event.getState().is(ModBlocks.LESSER_BLOOM.get())) return;
        D1Members.run(player.level()).filter(r -> D1Members.inside(player, r)).ifPresent(run ->
                PENDING.computeIfAbsent(player.level(), ignored -> new ArrayList<>())
                        .add(new Harvest(run.runId(), player.getUUID(), event.getPos().immutable())));
    }
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onPlaced(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !event.getPlacedBlock().is(ModBlocks.LESSER_BLOOM.get())) return;
        D1Members.run(level).ifPresent(run -> D1RunData.get(level.getServer()).recordUnique(run.runId(),
                "lesser_placed", level.dimension().location() + ":" + event.getPos().asLong()));
    }
    @SubscribeEvent
    public static void afterLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        var harvests = PENDING.remove(level);
        if (harvests == null) return;
        for (Harvest harvest : harvests) {
            var run = D1Members.run(level).filter(r -> r.runId() == harvest.runId()).orElse(null);
            if (run == null || level.getBlockState(harvest.pos()).is(ModBlocks.LESSER_BLOOM.get())) continue;
            var actor = level.getServer().getPlayerList().getPlayer(harvest.actor());
            if (actor == null || !D1Members.inside(actor, run)) continue;
            var data = D1RunData.get(level.getServer());
            String source = level.dimension().location() + ":" + harvest.pos().asLong();
            if (data.values(run.runId(), "lesser_placed").contains(source)
                    || !data.recordUnique(run.runId(), "lesser_harvests", source)) continue;
            for (var member : D1Members.active(level.getServer(), run)) {
                if (!run.containsDimension(member.level().dimension())) continue;
                String key = "lesser:" + member.getUUID();
                int old = data.count(run.runId(), key);
                data.setCount(run.runId(), key, old == Integer.MAX_VALUE ? old : old + 1);
                D1LifetimeData.get(level.getServer()).recordLesserBlooms(member.getUUID(), 1);
                net.goui.cosmicdungeon.faction.NpcFactionService.blooms(member, 1);
                D1Scoreboards.lifetime(member);
            }
        }
    }
    @SubscribeEvent
    public static void stopped(ServerStoppedEvent event) { PENDING.clear(); }
}
