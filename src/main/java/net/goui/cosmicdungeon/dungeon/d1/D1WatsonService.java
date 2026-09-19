package net.goui.cosmicdungeon.dungeon.d1;

import net.goui.cosmicdungeon.Config;
import net.goui.cosmicdungeon.CosmicDungeonMod;
import net.goui.cosmicdungeon.auth.AccessPolicy;
import net.goui.cosmicdungeon.dungeon.*;
import net.goui.cosmicdungeon.progression.ProgressionService;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import java.util.*;

/** Q&A D23/D56: all six real Bloom items, one hand-in, one outcome for this instance. */
@EventBusSubscriber(modid = CosmicDungeonMod.MOD_ID)
public final class D1WatsonService {
    private static final String RUN_TAG = "cosmicdungeon_d1_watson_run";
    private static final String ENTITY_KEY = "watson_entity";
    private static final List<String> BLOOMS = List.of(
            "bloom_of_quiet_assurance", "bloom_of_gentle_lies", "bloom_of_waning_mercy",
            "bloom_of_constricting_bonds", "bloom_of_unspoken_resignation", "bloom_of_elegy");
    private D1WatsonService() {}

    @SubscribeEvent
    public static void tick(ServerTickEvent.Post event) {
        var server = event.getServer();
        if (server.overworld().getGameTime() % Config.WATSON_POLL_TICKS.get() != 0) return;
        var binding = D1WatsonData.get(server);
        if (!binding.configured()) return;
        for (var run : DungeonRunRegistryData.get(server).listAllRuns()) {
            if (!run.dungeonId().equals("dungeon_1") || run.stateEnum() != DungeonRunState.ACTIVE || run.instanceSlot() < 1) continue;
            var level = level(server, run, binding);
            if (level == null || !level.hasChunkAt(binding.pos())) continue;
            var data = D1RunData.get(server);
            Entity existing = existing(level, data, run.runId());
            var members = net.goui.cosmicdungeon.achievement.plantflags.PlantFlagData.get(server).completed(run.runId())
                    ? gathered(server, run, level, binding.pos()) : List.<ServerPlayer>of();
            if (members.isEmpty()) {
                if (existing != null) existing.discard();
                data.setValue(run.runId(), ENTITY_KEY, "");
                continue;
            }
            if (existing == null) {
                Villager watson = EntityType.VILLAGER.create(level, EntitySpawnReason.TRIGGERED);
                if (watson == null) continue;
                watson.setPos(binding.pos().getX() + 0.5, binding.pos().getY(), binding.pos().getZ() + 0.5);
                watson.setCustomName(Component.literal("John Hamish Watson"));
                watson.setCustomNameVisible(true);
                watson.setNoAi(true);
                watson.setInvulnerable(true);
                watson.setSilent(true);
                watson.setPersistenceRequired();
                watson.getPersistentData().putLong(RUN_TAG, run.runId());
                if (level.addFreshEntity(watson)) data.setValue(run.runId(), ENTITY_KEY, watson.getUUID().toString());
            }
        }
    }

    private static ServerLevel level(MinecraftServer server, DungeonRunRegistryData.RunRecord run, D1WatsonData binding) {
        return DungeonInstanceSlots.mapping(DungeonDefinitions.DUNGEON_1, run.instanceSlot()).entrySet().stream()
                .filter(e -> e.getKey().location().toString().equals(binding.dimension()))
                .map(e -> server.getLevel(e.getValue())).filter(Objects::nonNull).findFirst().orElse(null);
    }
    @SubscribeEvent public static void joined(net.neoforged.neoforge.event.entity.EntityJoinLevelEvent event){
        if(!(event.getLevel() instanceof ServerLevel level))return;
        var entity=event.getEntity();long runId=entity.getPersistentData().getLongOr(RUN_TAG,0);
        if(runId<=0)return;
        var run=DungeonRunRegistryData.get(level.getServer()).getRun(runId).orElse(null);
        var data=D1RunData.get(level.getServer());var ids=data.values(runId,ENTITY_KEY);
        if(run==null||run.stateEnum()!=DungeonRunState.ACTIVE||!run.containsDimension(level.dimension())
                ||(!ids.isEmpty()&&!ids.getFirst().equals(entity.getUUID().toString()))){event.setCanceled(true);return;}
        data.setValue(runId,ENTITY_KEY,entity.getUUID().toString());
    }

    private static Entity existing(ServerLevel level, D1RunData data, long runId) {
        var ids = data.values(runId, ENTITY_KEY);
        if (ids.isEmpty()) return null;
        try {
            Entity entity = level.getEntity(UUID.fromString(ids.getFirst()));
            return entity != null && entity.isAlive() && entity.getPersistentData().getLongOr(RUN_TAG, 0L) == runId ? entity : null;
        } catch (IllegalArgumentException invalid) { return null; }
    }

    private static List<ServerPlayer> gathered(MinecraftServer server, DungeonRunRegistryData.RunRecord run,
                                               ServerLevel level, BlockPos pos) {
        List<ServerPlayer> members = new ArrayList<>();
        double radiusSquared = Config.WATSON_RADIUS.get() * Config.WATSON_RADIUS.get();
        for (UUID id : run.orderedPlayers()) {
            var member = server.getPlayerList().getPlayer(id);
            // A disconnected member still belongs to the run until lifecycle removal. They block completion.
            if (member == null) return List.of();
            if (AccessPolicy.isDeveloper(member)) continue;
            if (!member.isAlive() || member.isSpectator() || member.level() != level
                    || member.distanceToSqr(Vec3.atBottomCenterOf(pos)) > radiusSquared) return List.of();
            members.add(member);
        }
        return List.copyOf(members);
    }

    @SubscribeEvent
    public static void interact(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || event.getHand() != InteractionHand.MAIN_HAND) return;
        Entity watson = event.getTarget();
        long runId = watson.getPersistentData().getLongOr(RUN_TAG, 0L);
        if (runId <= 0) return;
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (AccessPolicy.isDeveloper(player)) return;
        var server = player.level().getServer();
        var run = DungeonRunRegistryData.get(server).getRun(runId).orElse(null);
        var data = D1RunData.get(server);
        var binding = D1WatsonData.get(server);
        if (run == null || run.stateEnum() != DungeonRunState.ACTIVE || !run.containsPlayer(player.getUUID())
                || !run.containsDimension(player.level().dimension()) || !binding.configured()
                || existing(player.level(), data, runId) != watson || player.distanceToSqr(watson) > 64.0) return;
        var members = gathered(server, run, player.level(), binding.pos());
        if (!members.contains(player)) {
            player.sendSystemMessage(Component.literal("Every dungeoneer in this instance must gather near Watson."));
            return;
        }
        if (!data.values(runId, "watson_outcome").isEmpty()) return;
        members.forEach(ServerPlayer::closeContainer);
        if (!net.goui.cosmicdungeon.transaction.InventoryTransactionGuard.readyForCleanup(server, run.orderedPlayers())
                || DungeonRunRegistryData.get(server).starting(runId)) {
            player.sendSystemMessage(Component.literal("Finish pending item recovery before handing in the Blooms."));
            return;
        }
        record Slot(ServerPlayer player, int index) {}
        Map<String, Slot> found = new LinkedHashMap<>();
        for (var member : members) {
            for (int slot = 0; slot < member.getInventory().getContainerSize(); slot++) {
                var stack = member.getInventory().getItem(slot);
                if (stack.isEmpty()) continue;
                var id = BuiltInRegistries.ITEM.getKey(stack.getItem());
                if (id.getNamespace().equals(CosmicDungeonMod.MOD_ID) && BLOOMS.contains(id.getPath()))
                    found.putIfAbsent(id.getPath(), new Slot(member, slot));
            }
        }
        boolean success = found.size() == BLOOMS.size();
        // TODO(M93/M03, final outcome transaction): Q&A D20/D23 (2026-09-16) requires
        // success-only lifetime kills/Blooms/completion and consumption of all six physical Blooms.
        // Batch28 journals the resulting inventory cleanup, not this earlier multi-store reward
        // decision. Persist one outcome and receipt its Bloom inputs, lifetime/progression/faction
        // projections before accepting native interrupted-save QA; do not reset lifetime totals.
        data.setValue(runId, "watson_outcome", success ? "success" : "failure");
        // All validation completes before any Bloom is removed.
        found.values().forEach(slot -> slot.player().getInventory().getItem(slot.index()).shrink(1));
        for (var member : members) {
            member.getInventory().setChanged();
            member.inventoryMenu.broadcastChanges();
            if (success) net.goui.cosmicdungeon.npc.tamsin.TamsinTaxProgress.successfulRun(member, runId);
            if (success && D1LifetimeData.get(server).complete(member.getUUID(), runId, data.count(runId, "kills:" + member.getUUID()))) {
                ProgressionService.markD1Completed(member, 6);
                int collected = data.count(runId, "lesser:" + member.getUUID());
                int rounded = D1ObjectiveRules.roundedLesserBlooms(collected);
                ProgressionService.addLesserBlooms(member, rounded);
                net.goui.cosmicdungeon.faction.NpcFactionService.blooms(member, rounded - collected);
                D1Scoreboards.lifetime(member);
            }
            member.sendSystemMessage(Component.literal(success
                    ? "All six Blooms are returned. Watson is restored. Dungeon 1 completed!"
                    : "The six Blooms are incomplete. This Dungeon 1 run has failed."));
        }
        watson.discard();
        DungeonLifecycleService.resolveD1Run(server, runId, success);
    }

    // TODO(M89-M92/D52-D55, D2+): Story Template 1VwuK2NIRUTIxIMOFviWlVPZ_gz09yRjmYdSNlxxQkyY
    // (2026-07-06) and Watson 1e1po-TpjWQpTJ6ueIfnpTDNRZ1za3J7434Y4nFXvSmo (2026-07-10):
    // after restoring Watson, Atlach-Nacha's thread remains. Later quests require Shattered
    // Realities from Shudde M'ell, Dagon/Hydra, Atlach-Nacha, and Nyarlathotep. Do not unlock
    // D2 party ready flow, Anchors, the final trap or ending rewards during this D1 pass.
}
