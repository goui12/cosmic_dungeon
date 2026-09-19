package net.goui.cosmicdungeon.block.custom;

import com.mojang.logging.LogUtils;
import net.goui.cosmicdungeon.block.entity.ClassSelectorBlockEntity;
import net.goui.cosmicdungeon.dungeon.DungeonLifecycleService;
import net.goui.cosmicdungeon.dungeon.DungeonStartupSchematicPlan;
import net.goui.cosmicdungeon.dungeon.DungeonStartupSchematicPipeline;
import net.goui.cosmicdungeon.rift.RiftRegistryData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.Heightmap;
import org.slf4j.Logger;

import java.util.*;

/** Existing slot/schematic pipeline, entered only with a server-locked invited roster. */
public final class ClassSelectorEntryService {
    private static final Logger LOGGER = LogUtils.getLogger();
    private ClassSelectorEntryService() {}
    private record TeleportTarget(ServerLevel level, BlockPos safePos) {}

    public static boolean enter(MinecraftServer server, net.goui.cosmicdungeon.npc.tamsin.D1PartyLobby.Anchor anchor,
                                List<UUID> ordered, Map<UUID, String> classes,
                                java.util.function.BooleanSupplier validRoster) {
        if (!server.isSameThread() || !validRoster.getAsBoolean()) return false;
        try {
            return enterPrepared(server, anchor, ordered, classes, validRoster);
        } catch (RuntimeException failure) {
            LOGGER.error("D1 entry interrupted; registered rollback and saved inventories retained", failure);
            rollbackRegistered(server, ordered);
            return false;
        }
    }
    private static void rollbackRegistered(MinecraftServer server, List<UUID> ordered) {
        var runs = net.goui.cosmicdungeon.dungeon.DungeonRunRegistryData.get(server);
        for (UUID owner : ordered) {
            var run = runs.findRunForPlayer(owner).orElse(null);
            if (run != null && runs.starting(run.runId())) {
                var online = server.getPlayerList().getPlayer(owner);
                if (online != null) { DungeonLifecycleService.abortActiveRunForPlayer(online); return; }
            }
        }
    }
    private static boolean enterPrepared(MinecraftServer server, net.goui.cosmicdungeon.npc.tamsin.D1PartyLobby.Anchor anchor,
            List<UUID> ordered, Map<UUID, String> classes, java.util.function.BooleanSupplier validRoster) {
        ServerLevel selectorLevel = ClassSelectorTeleportUtil.resolveLevel(server, anchor.dimension());
        BlockPos selectorPos = BlockPos.of(anchor.selector());
        if (selectorLevel == null || !(selectorLevel.getBlockEntity(selectorPos) instanceof ClassSelectorBlockEntity csbe)) return false;
        int max = ordered.size();
        if (max < net.goui.cosmicdungeon.Config.MIN_PARTY.get() || max > csbe.getMaxPlayers()) return false;

        RiftRegistryData data = RiftRegistryData.get(server);

        List<RiftRegistryData.DestinationRecord> resolved = new ArrayList<>(max);
        List<Integer> missingSlots = new ArrayList<>();
        List<Integer> invalidSlots = new ArrayList<>();

        for (int slot = 1; slot <= max; slot++) {
            String name = csbe.getSlotDestination(slot);

            if (name == null || name.isBlank()) {
                name = csbe.getDestinationName();
            }

            if (name == null || name.isBlank()) {
                missingSlots.add(slot);
                resolved.add(null);
                continue;
            }

            var destOpt = data.getDestination(name);
            if (destOpt.isEmpty()) {
                invalidSlots.add(slot);
                resolved.add(null);
                continue;
            }

            RiftRegistryData.DestinationRecord dest = destOpt.get();
            resolved.add(dest);

            if (ClassSelectorTeleportUtil.resolveLevel(server, dest.dimensionId()) == null) {
                invalidSlots.add(slot);
            }
        }

        if (!missingSlots.isEmpty() || !invalidSlots.isEmpty()) {
            var m = Component.literal("Class Selector teleport not configured: ").withStyle(ChatFormatting.RED);

            if (!missingSlots.isEmpty()) {
                m = m.append(Component.literal("missing slots " + missingSlots).withStyle(ChatFormatting.YELLOW));
            }
            if (!invalidSlots.isEmpty()) {
                if (!missingSlots.isEmpty()) m = m.append(Component.literal("; ").withStyle(ChatFormatting.RED));
                m = m.append(Component.literal("invalid destinations for slots " + invalidSlots).withStyle(ChatFormatting.YELLOW));
            }

            for (UUID id : ordered) {
                ServerPlayer p = server.getPlayerList().getPlayer(id);
                if (p != null) p.sendSystemMessage(m);
            }

            return false;
        }

        RiftRegistryData.DestinationRecord firstDest = resolved.get(0);
        ServerLevel dungeonLevel = ClassSelectorTeleportUtil.resolveLevel(server, firstDest.dimensionId());
        if (dungeonLevel == null || !net.goui.cosmicdungeon.dungeon.DungeonDefinitions.DUNGEON_1.containsDimension(dungeonLevel.dimension())) {
            for (UUID id : ordered) {
                ServerPlayer p = server.getPlayerList().getPlayer(id);
                if (p != null) {
                    p.sendSystemMessage(Component.literal("Failed to resolve dungeon dimension.").withStyle(ChatFormatting.RED));
                }
            }
            return false;
        }

        List<ServerPlayer> finalParty = new ArrayList<>();
        for (UUID id : ordered) {
            if (finalParty.size() >= max) break;
            ServerPlayer p = server.getPlayerList().getPlayer(id);
            if (p != null && p.connection.isAcceptingMessages() && p.isAlive() && !p.isSpectator() && p.level()==selectorLevel
                    && !net.goui.cosmicdungeon.auth.AccessPolicy.isDeveloper(p)
                    && p.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(selectorPos))<=
                        net.goui.cosmicdungeon.Config.SELECTOR_RANGE.get()*net.goui.cosmicdungeon.Config.SELECTOR_RANGE.get()
                    && ClassSelectorTeleportUtil.isReadyEligibleClass(classes.get(id))
                    && java.util.Objects.equals(classes.get(id),net.goui.cosmicdungeon.playerclass.api.ClassData.getClassId(p))) {
                finalParty.add(p);
            }
        }

        if (finalParty.size() < max) {
            for (ServerPlayer p : finalParty) {
                p.sendSystemMessage(Component.literal("Party changed before teleport. Ready up again.").withStyle(ChatFormatting.RED));
            }
            return false;
        }

        if (!validRoster.getAsBoolean()) return false;
        DungeonLifecycleService.InstancePreparation preparationResult =
                DungeonLifecycleService.prepareRunInstance(server, dungeonLevel.dimension(), ordered);
        if (preparationResult instanceof DungeonLifecycleService.PreparationError preparationError) {
            Component msg = Component.literal(preparationError.message()).withStyle(ChatFormatting.RED);
            for (ServerPlayer p : finalParty) {
                p.sendSystemMessage(msg);
            }
            return false;
        }
        DungeonLifecycleService.PreparedInstance prepared = (DungeonLifecycleService.PreparedInstance) preparationResult;
        if (!validRoster.getAsBoolean()) return false;
        List<TeleportTarget> teleportTargets = new ArrayList<>(max);
        for (RiftRegistryData.DestinationRecord destination : resolved) {
            ServerLevel templateLevel = ClassSelectorTeleportUtil.resolveLevel(server, destination.dimensionId());
            ServerLevel instanceLevel = templateLevel == null ? null : prepared.resolve(server, templateLevel.dimension());
            if (instanceLevel == null) {
                for (ServerPlayer p : finalParty) {
                    p.sendSystemMessage(Component.literal("A class-selector destination is outside the selected dungeon template.")
                            .withStyle(ChatFormatting.RED));
                }
                return false;
            }
            teleportTargets.add(new TeleportTarget(instanceLevel,destination.pos()));
        }
        dungeonLevel = prepared.resolve(server, prepared.definition().primaryDimension());
        if (dungeonLevel == null) return false;

        List<String> orderedClasses = new ArrayList<>(finalParty.size());
        for (ServerPlayer partyMember : finalParty) {
            orderedClasses.add(classes.get(partyMember.getUUID()));
        }

        DungeonStartupSchematicPlan.StartupPastePlan pastePlan;
        try {
            pastePlan = DungeonStartupSchematicPlan.buildPlan(orderedClasses);
        } catch (IllegalArgumentException exception) {
            LOGGER.error("[DungeonStartupSchematics] Could not build the 36-operation startup plan.", exception);
            for (ServerPlayer p : finalParty) {
                p.sendSystemMessage(Component.literal("Dungeon paste failed.").withStyle(ChatFormatting.RED));
            }
            return false;
        }

        DungeonStartupSchematicPipeline.PasteBatchResult pasteResult =
                DungeonStartupSchematicPipeline.execute(dungeonLevel, pastePlan);
        if (!(pasteResult instanceof DungeonStartupSchematicPipeline.PasteBatchSuccess success)
                || success.completedOperations() != DungeonStartupSchematicPlan.EXPECTED_OPERATION_COUNT) {
            for (ServerPlayer p : finalParty) {
                p.sendSystemMessage(Component.literal("Dungeon paste failed. Please try again.")
                        .withStyle(ChatFormatting.RED));
            }
            return false;
        }

        if (!validRoster.getAsBoolean()) return false;
        for (int index = 0; index < teleportTargets.size(); index++) {
            var target = teleportTargets.get(index);
            var safe = ensureStandable(target.level(), target.safePos());
            if (safe == null) {
                finalParty.forEach(p -> p.sendSystemMessage(Component.literal("The entry point is obstructed; a developer must fix its placement.")));
                return false;
            }
            teleportTargets.set(index, new TeleportTarget(target.level(), safe));
        }
        if (!validRoster.getAsBoolean()) return false;
        finalParty.forEach(ServerPlayer::closeContainer);
        String err = DungeonLifecycleService.startRun(
                server,
                selectorLevel.dimension(),
                selectorPos.asLong(),
                prepared.definition().primaryDimension(),
                prepared,
                finalParty
        );

        if (err != null) {
            rollbackRegistered(server, ordered);
            Component msg = Component.literal(err).withStyle(ChatFormatting.RED);
            for (ServerPlayer p : finalParty) {
                p.sendSystemMessage(msg);
            }
            return false;
        }

        for (int slotIndex = 0; slotIndex < finalParty.size(); slotIndex++) {
            ServerPlayer p = finalParty.get(slotIndex);
            TeleportTarget tp = teleportTargets.get(slotIndex);
            if (!p.connection.isAcceptingMessages() || !p.isAlive() || tp == null || tp.level() == null || tp.safePos() == null) {
                p.sendSystemMessage(Component.literal("Teleport target missing for your slot.").withStyle(ChatFormatting.RED));
                DungeonLifecycleService.abortActiveRunForPlayer(finalParty.getFirst());
                return false;
            }

            BlockPos safe = tp.safePos();
            boolean ok = p.teleportTo(
                    tp.level(),
                    safe.getX() + 0.5D,
                    safe.getY(),
                    safe.getZ() + 0.5D,
                    Set.of(),
                    p.getYRot(),
                    p.getXRot(),
                    true
            );

            if (!ok) {
                p.sendSystemMessage(Component.literal("Teleport failed for your slot.").withStyle(ChatFormatting.RED));
                DungeonLifecycleService.abortActiveRunForPlayer(finalParty.getFirst());
                return false;
            }

            net.goui.cosmicdungeon.dungeon.ChopOwnershipService.clearForDungeonEntry(p);
            DungeonLifecycleService.setPlayerRespawnTo(p, tp.level(), safe, p.getYRot(), p.getXRot());
        }

        var run = net.goui.cosmicdungeon.dungeon.DungeonRunRegistryData.get(server)
                .findRunForPlayer(finalParty.getFirst().getUUID()).orElseThrow();
        if (!DungeonLifecycleService.completeStartup(server, run.runId())) {
            DungeonLifecycleService.abortActiveRunForPlayer(finalParty.getFirst());
            return false;
        }
        return true;
    }

    // TODO(M102): licensed TEST failure-injection at snapshot refresh, each paste, run registration
    // and each teleport/player save, including failures before/after the final startup receipt.
    // Tamsin 1-FcHP73pFytPfoM2KhUPa6tt_2licsgWmWokto4YzE4 (2026-08-19) requires one locked
    // roster. Its full pre-entry inventories remain saved until every owner entered. On restart an
    // incomplete marker rolls back the entire roster. Pre-registration paste failures change no
    // inventory; the unused slot is refreshed before reuse. Actual authored 36-room bindings remain M81.
    private static BlockPos ensureStandable(ServerLevel level, BlockPos pos) {
        if (isStandable(level, pos)) return pos;

        var m = pos.mutable();
        int minY = level.getMinY();
        int maxY = minY + level.getLogicalHeight() - 1;

        for (int y = Math.max(minY + 1, m.getY()); y < maxY - 1; y++) {
            m.setY(y);
            if (isStandable(level, m)) return m.immutable();
        }

        int surfaceY = level.getHeight(Heightmap.Types.WORLD_SURFACE, pos.getX(), pos.getZ());
        m.set(pos.getX(), Math.max(surfaceY, minY + 1), pos.getZ());

        for (int y = m.getY(); y < Math.min(m.getY() + 8, maxY - 1); y++) {
            m.setY(y);
            if (isStandable(level, m)) return m.immutable();
        }

        return null;
    }

    private static boolean isStandable(ServerLevel level, BlockPos pos) {
        var below = pos.below();
        var feet = level.getBlockState(pos);
        var head = level.getBlockState(pos.above());

        boolean sturdyBelow = level.getBlockState(below).isFaceSturdy(level, below, net.minecraft.core.Direction.UP);
        boolean noFluid = level.getFluidState(pos).isEmpty() && level.getFluidState(pos.above()).isEmpty();
        boolean emptySpace = feet.getCollisionShape(level, pos).isEmpty()
                && head.getCollisionShape(level, pos.above()).isEmpty();

        return sturdyBelow && noFluid && emptySpace;
    }
}
