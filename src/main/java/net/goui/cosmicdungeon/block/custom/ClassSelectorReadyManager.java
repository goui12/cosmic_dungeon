package net.goui.cosmicdungeon.block.custom;

import net.goui.cosmicdungeon.block.entity.ClassSelectorBlockEntity;
import net.goui.cosmicdungeon.dungeon.DungeonDefinition;
import net.goui.cosmicdungeon.dungeon.DungeonDefinitions;
import net.goui.cosmicdungeon.dungeon.DungeonLifecycleService;
import net.goui.cosmicdungeon.dungeon.DungeonStarterRoomPaster;
import net.goui.cosmicdungeon.rift.RiftRegistryData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.*;

public final class ClassSelectorReadyManager {
    private ClassSelectorReadyManager() {}

    private static final int COUNTDOWN_SECONDS = 5;
    private static final long COUNTDOWN_TICKS = COUNTDOWN_SECONDS * 20L;

    public record SelectorKey(String dimId, long posLong) {}

    private static final Map<SelectorKey, ReadyState> STATES = new HashMap<>();

    private static final class ReadyState {
        final LinkedHashSet<UUID> ordered = new LinkedHashSet<>();
        final Map<UUID, String> classByPlayer = new HashMap<>();

        long countdownEndTick = -1L;
        int lastAnnouncedSeconds = Integer.MIN_VALUE;
    }

    private record TeleportTarget(ServerLevel level, BlockPos safePos) {}

    public static void clearFor(ServerLevel selectorLevel, BlockPos selectorPos) {
        if (selectorLevel == null || selectorPos == null) return;
        SelectorKey key = new SelectorKey(selectorLevel.dimension().location().toString(), selectorPos.asLong());
        STATES.remove(key);
    }

    public static int markReady(ServerPlayer sp,
                                ServerLevel selectorLevel,
                                BlockPos selectorPos,
                                ClassSelectorBlockEntity csbe,
                                String classId) {
        MinecraftServer server = selectorLevel.getServer();
        if (server == null) return -1;

        SelectorKey key = new SelectorKey(selectorLevel.dimension().location().toString(), selectorPos.asLong());
        ReadyState st = STATES.computeIfAbsent(key, k -> new ReadyState());

        if (!st.ordered.contains(sp.getUUID())) st.ordered.add(sp.getUUID());
        st.classByPlayer.put(sp.getUUID(), classId);

        int slot = 1;
        for (UUID id : st.ordered) {
            if (id.equals(sp.getUUID())) break;
            slot++;
        }

        int max = Math.max(1, Math.min(6, csbe.getMaxPlayers()));
        int ready = st.ordered.size();

        sp.closeContainer();

        var className = Component.translatable("playerclass.cosmicdungeon." + classId);

        var readyMsg = Component.literal("Player ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(String.valueOf(slot)).withStyle(ChatFormatting.AQUA))
                .append(Component.literal(", the ").withStyle(ChatFormatting.GRAY))
                .append(className.copy().withStyle(ChatFormatting.YELLOW))
                .append(Component.literal(" is ready!").withStyle(ChatFormatting.GRAY));

        sp.displayClientMessage(readyMsg, false);

        broadcastReadyProgress(server, st, ready, max);

        if (ready >= max && st.countdownEndTick < 0L) {
            long nowTick = selectorLevel.getGameTime();
            st.countdownEndTick = nowTick + COUNTDOWN_TICKS;
            st.lastAnnouncedSeconds = Integer.MIN_VALUE;

            var msg = Component.literal("All players ready — teleporting in ")
                    .withStyle(ChatFormatting.AQUA)
                    .append(Component.literal(String.valueOf(COUNTDOWN_SECONDS)).withStyle(ChatFormatting.YELLOW))
                    .append(Component.literal("...").withStyle(ChatFormatting.AQUA));

            for (UUID id : st.ordered) {
                ServerPlayer p = server.getPlayerList().getPlayer(id);
                if (p != null) p.sendSystemMessage(msg);
            }
        }

        return slot;
    }

    public static void tick(MinecraftServer server) {
        if (server == null) return;
        if (STATES.isEmpty()) return;

        Iterator<Map.Entry<SelectorKey, ReadyState>> it = STATES.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<SelectorKey, ReadyState> entry = it.next();
            SelectorKey key = entry.getKey();
            ReadyState st = entry.getValue();

            ServerLevel selectorLevel = ClassSelectorTeleportUtil.resolveLevel(server, key.dimId());
            if (selectorLevel == null) {
                it.remove();
                continue;
            }

            BlockPos selectorPos = BlockPos.of(key.posLong());
            var be = selectorLevel.getBlockEntity(selectorPos);
            if (!(be instanceof ClassSelectorBlockEntity csbe)) {
                it.remove();
                continue;
            }

            pruneOffline(server, st);

            if (st.ordered.isEmpty()) {
                it.remove();
                continue;
            }

            int max = Math.max(1, Math.min(6, csbe.getMaxPlayers()));
            int ready = st.ordered.size();

            if (st.countdownEndTick >= 0L && ready < max) {
                cancelCountdown(server, st, "Countdown canceled (party not full).");
                continue;
            }

            if (ready >= max && st.countdownEndTick < 0L) {
                long nowTick = selectorLevel.getGameTime();
                st.countdownEndTick = nowTick + COUNTDOWN_TICKS;
                st.lastAnnouncedSeconds = Integer.MIN_VALUE;

                var msg = Component.literal("All players ready — teleporting in ")
                        .withStyle(ChatFormatting.AQUA)
                        .append(Component.literal(String.valueOf(COUNTDOWN_SECONDS)).withStyle(ChatFormatting.YELLOW))
                        .append(Component.literal("...").withStyle(ChatFormatting.AQUA));

                for (UUID id : st.ordered) {
                    ServerPlayer p = server.getPlayerList().getPlayer(id);
                    if (p != null) p.sendSystemMessage(msg);
                }
            }

            if (st.countdownEndTick >= 0L) {
                long now = selectorLevel.getGameTime();
                long remainingTicks = st.countdownEndTick - now;

                if (remainingTicks <= 0L) {
                    boolean ok = teleportAllReadyBySlot(server, selectorLevel, selectorPos, csbe, st);
                    if (ok) {
                        it.remove();
                    } else {
                        cancelCountdown(server, st, "Teleport aborted. Fix configuration and ready up again.");
                    }
                } else {
                    int remainingSeconds = (int) Math.ceil(remainingTicks / 20.0D);
                    if (remainingSeconds != st.lastAnnouncedSeconds) {
                        st.lastAnnouncedSeconds = remainingSeconds;

                        var msg = Component.literal("Teleporting in ").withStyle(ChatFormatting.AQUA)
                                .append(Component.literal(String.valueOf(remainingSeconds)).withStyle(ChatFormatting.YELLOW))
                                .append(Component.literal("...").withStyle(ChatFormatting.AQUA));

                        for (UUID id : st.ordered) {
                            ServerPlayer p = server.getPlayerList().getPlayer(id);
                            if (p != null) p.sendSystemMessage(msg);
                        }
                    }
                }
            }
        }
    }

    public static void cancelCountdown(MinecraftServer server, ReadyState st, String msg) {
        st.countdownEndTick = -1L;
        st.lastAnnouncedSeconds = Integer.MIN_VALUE;

        var c = Component.literal(msg).withStyle(ChatFormatting.RED);
        for (UUID id : st.ordered) {
            ServerPlayer p = server.getPlayerList().getPlayer(id);
            if (p != null) p.sendSystemMessage(c);
        }
    }

    public static void pruneOffline(MinecraftServer server, ReadyState st) {
        Iterator<UUID> it = st.ordered.iterator();
        while (it.hasNext()) {
            UUID id = it.next();
            if (server.getPlayerList().getPlayer(id) == null) it.remove();
        }
    }

    public static void broadcastReadyProgress(MinecraftServer server, ReadyState st, int ready, int max) {
        var msg = Component.literal("Ready: ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(ready + "/" + max).withStyle(ChatFormatting.GREEN));

        for (UUID id : st.ordered) {
            ServerPlayer p = server.getPlayerList().getPlayer(id);
            if (p != null) p.sendSystemMessage(msg);
        }
    }

    private static boolean teleportAllReadyBySlot(MinecraftServer server,
                                                  ServerLevel selectorLevel,
                                                  BlockPos selectorPos,
                                                  ClassSelectorBlockEntity csbe,
                                                  ReadyState st) {
        int max = Math.max(1, Math.min(6, csbe.getMaxPlayers()));
        int ready = st.ordered.size();
        if (ready < max) return false;

        RiftRegistryData data = RiftRegistryData.get(server);

        List<RiftRegistryData.DestinationRecord> resolved = new ArrayList<>(max);
        List<Integer> missingSlots = new ArrayList<>();
        List<Integer> invalidSlots = new ArrayList<>();
        List<TeleportTarget> teleportTargets = new ArrayList<>(max);

        for (int slot = 1; slot <= max; slot++) {
            String name = csbe.getSlotDestination(slot);

            if (name == null || name.isBlank()) {
                name = csbe.getDestinationName();
            }

            if (name == null || name.isBlank()) {
                missingSlots.add(slot);
                resolved.add(null);
                teleportTargets.add(null);
                continue;
            }

            var destOpt = data.getDestination(name);
            if (destOpt.isEmpty()) {
                invalidSlots.add(slot);
                resolved.add(null);
                teleportTargets.add(null);
                continue;
            }

            RiftRegistryData.DestinationRecord dest = destOpt.get();
            resolved.add(dest);

            ServerLevel targetLevel = ClassSelectorTeleportUtil.resolveLevel(server, dest.dimensionId());
            if (targetLevel == null) {
                invalidSlots.add(slot);
                teleportTargets.add(null);
                continue;
            }

            BlockPos safe = ensureStandable(targetLevel, dest.pos());
            teleportTargets.add(new TeleportTarget(targetLevel, safe));
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

            for (UUID id : st.ordered) {
                ServerPlayer p = server.getPlayerList().getPlayer(id);
                if (p != null) p.sendSystemMessage(m);
            }

            return false;
        }

        RiftRegistryData.DestinationRecord firstDest = resolved.get(0);
        ServerLevel dungeonLevel = ClassSelectorTeleportUtil.resolveLevel(server, firstDest.dimensionId());
        if (dungeonLevel == null) {
            for (UUID id : st.ordered) {
                ServerPlayer p = server.getPlayerList().getPlayer(id);
                if (p != null) {
                    p.sendSystemMessage(Component.literal("Failed to resolve dungeon dimension.").withStyle(ChatFormatting.RED));
                }
            }
            return false;
        }

        List<ServerPlayer> finalParty = new ArrayList<>();
        for (UUID id : st.ordered) {
            if (finalParty.size() >= max) break;
            ServerPlayer p = server.getPlayerList().getPlayer(id);
            if (p != null) {
                finalParty.add(p);
            }
        }

        if (finalParty.size() < max) {
            for (ServerPlayer p : finalParty) {
                p.sendSystemMessage(Component.literal("Party changed before teleport. Ready up again.").withStyle(ChatFormatting.RED));
            }
            return false;
        }

        String lifecycleBlocker = DungeonLifecycleService.getStartRunBlocker(server, dungeonLevel.dimension(), st.ordered);
        if (lifecycleBlocker != null) {
            Component msg = Component.literal(lifecycleBlocker).withStyle(ChatFormatting.RED);
            for (ServerPlayer p : finalParty) {
                p.sendSystemMessage(msg);
            }
            return false;
        }

        String[] slotClasses = new String[6];
        int idx = 0;
        for (UUID id : st.ordered) {
            if (idx >= 6) break;
            slotClasses[idx] = st.classByPlayer.getOrDefault(id, "blankslot");
            idx++;
        }
        for (; idx < 6; idx++) {
            slotClasses[idx] = "blankslot";
        }

        ServerPlayer pasteActor = finalParty.getFirst();

        try {
            DungeonStarterRoomPaster.pasteRooms(dungeonLevel, pasteActor, slotClasses);
        } catch (Exception e) {
            System.err.println("[CosmicDungeon] Paste failed:");
            e.printStackTrace();

            for (ServerPlayer p : finalParty) {
                p.sendSystemMessage(Component.literal("Dungeon paste failed.").withStyle(ChatFormatting.RED));
            }
            return false;
        }

        String err = DungeonLifecycleService.startRun(
                server,
                selectorLevel.dimension(),
                selectorPos.asLong(),
                dungeonLevel.dimension(),
                finalParty
        );

        if (err != null) {
            Component msg = Component.literal(err).withStyle(ChatFormatting.RED);
            for (ServerPlayer p : finalParty) {
                p.sendSystemMessage(msg);
            }
            return false;
        }

        for (int slotIndex = 0; slotIndex < finalParty.size(); slotIndex++) {
            ServerPlayer p = finalParty.get(slotIndex);
            TeleportTarget tp = teleportTargets.get(slotIndex);
            if (tp == null || tp.level() == null || tp.safePos() == null) {
                p.sendSystemMessage(Component.literal("Teleport target missing for your slot.").withStyle(ChatFormatting.RED));
                DungeonLifecycleService.manualReset(server, DungeonDefinitions.byDimension(dungeonLevel.dimension()).map(DungeonDefinition::id).orElse(dungeonLevel.dimension().location().getPath()), null);
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
                DungeonLifecycleService.manualReset(server, DungeonDefinitions.byDimension(dungeonLevel.dimension()).map(DungeonDefinition::id).orElse(dungeonLevel.dimension().location().getPath()), null);
                return false;
            }

            DungeonLifecycleService.clearPlayerInventory(p);
            DungeonLifecycleService.setPlayerRespawnTo(p, tp.level(), safe, p.getYRot(), p.getXRot());
        }

        return true;
    }

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

        return m.immutable();
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
