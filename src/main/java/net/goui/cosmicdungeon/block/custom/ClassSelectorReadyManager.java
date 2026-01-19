// file: src/main/java/net/goui/cosmicdungeon/block/custom/ClassSelectorReadyManager.java
package net.goui.cosmicdungeon.block.custom;

import net.goui.cosmicdungeon.auth.AccessPolicy;
import net.goui.cosmicdungeon.block.entity.ClassSelectorBlockEntity;
import net.goui.cosmicdungeon.rift.RiftRegistryData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.*;

public final class ClassSelectorReadyManager {
    private ClassSelectorReadyManager() {}

    /** 5 second countdown before teleport once party is full. */
    private static final int COUNTDOWN_SECONDS = 5;
    private static final long COUNTDOWN_TICKS = COUNTDOWN_SECONDS * 20L;

    /** key = selector dimension + selector pos */
    public record SelectorKey(String dimId, long posLong) {}

    private static final Map<SelectorKey, ReadyState> STATES = new HashMap<>();

    private static final class ReadyState {
        final LinkedHashSet<UUID> ordered = new LinkedHashSet<>();
        long countdownEndTick = -1L; // -1 = not started
        int lastAnnouncedSeconds = Integer.MIN_VALUE;
    }

    public static void clearFor(ServerLevel selectorLevel, BlockPos selectorPos) {
        if (selectorLevel == null || selectorPos == null) return;
        SelectorKey key = new SelectorKey(selectorLevel.dimension().location().toString(), selectorPos.asLong());
        STATES.remove(key);
    }

    /** Returns the 1-based slot number for this player (existing if already ready). */
    public static int markReady(ServerPlayer sp, ServerLevel selectorLevel, BlockPos selectorPos, ClassSelectorBlockEntity csbe, String classId) {
        MinecraftServer server = selectorLevel.getServer();
        if (server == null) return -1;

        SelectorKey key = new SelectorKey(selectorLevel.dimension().location().toString(), selectorPos.asLong());
        ReadyState st = STATES.computeIfAbsent(key, k -> new ReadyState());

        if (!st.ordered.contains(sp.getUUID())) st.ordered.add(sp.getUUID());

        int slot = 1;
        for (UUID id : st.ordered) {
            if (id.equals(sp.getUUID())) break;
            slot++;
        }

        int max = Math.max(1, Math.min(64, csbe.getMaxPlayers()));
        int ready = st.ordered.size();

        // Close UI immediately when committed
        sp.closeContainer();

        // "Player N, the <className> is ready!"
        var className = Component.translatable("playerclass.cosmicdungeon." + classId);

        var readyMsg = Component.literal("Player ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(String.valueOf(slot)).withStyle(ChatFormatting.AQUA))
                .append(Component.literal(", the ").withStyle(ChatFormatting.GRAY))
                .append(className.copy().withStyle(ChatFormatting.YELLOW))
                .append(Component.literal(" is ready!").withStyle(ChatFormatting.GRAY));

        sp.displayClientMessage(readyMsg, false);

        broadcastReadyProgress(server, st, ready, max);

        // Party full => start countdown (if not already started)
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

    /** Called from a ServerTickEvent hook. */
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

            int max = Math.max(1, Math.min(64, csbe.getMaxPlayers()));
            int ready = st.ordered.size();

            // Countdown running but party no longer full => cancel countdown
            if (st.countdownEndTick >= 0L && ready < max) {
                cancelCountdown(server, st, "Countdown canceled (party not full).");
                continue;
            }

            // Party full but countdown not running => start countdown
            if (ready >= max && st.countdownEndTick < 0L) {
                long nowTick = selectorLevel.getGameTime();
                st.countdownEndTick = nowTick + COUNTDOWN_TICKS;
                st.lastAnnouncedSeconds = Integer.MIN_VALUE;

                var msg = Component.literal("All players ready — teleporting in ").withStyle(ChatFormatting.AQUA)
                        .append(Component.literal(String.valueOf(COUNTDOWN_SECONDS)).withStyle(ChatFormatting.YELLOW))
                        .append(Component.literal("...").withStyle(ChatFormatting.AQUA));

                for (UUID id : st.ordered) {
                    ServerPlayer p = server.getPlayerList().getPlayer(id);
                    if (p != null) p.sendSystemMessage(msg);
                }
            }

            // Countdown tick
            if (st.countdownEndTick >= 0L) {
                long now = selectorLevel.getGameTime();
                long remainingTicks = st.countdownEndTick - now;

                if (remainingTicks <= 0L) {
                    boolean ok = teleportAllReadyBySlot(server, selectorLevel, selectorPos, csbe, st);
                    if (ok) {
                        it.remove(); // success clears state
                    } else {
                        // teleport failed due to config (missing slot destinations etc.)
                        // cancel countdown but keep ready state so dev can fix without players re-selecting
                        cancelCountdown(server, st, "Teleport aborted (missing/invalid slot destinations).");
                    }
                    continue;
                }

                int remainingSeconds = (int) ((remainingTicks + 19L) / 20L);
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

    private static void cancelCountdown(MinecraftServer server, ReadyState st, String msg) {
        st.countdownEndTick = -1L;
        st.lastAnnouncedSeconds = Integer.MIN_VALUE;

        var c = Component.literal(msg).withStyle(ChatFormatting.RED);
        for (UUID id : st.ordered) {
            ServerPlayer p = server.getPlayerList().getPlayer(id);
            if (p != null) p.sendSystemMessage(c);
        }
    }

    private static void pruneOffline(MinecraftServer server, ReadyState st) {
        Iterator<UUID> it = st.ordered.iterator();
        while (it.hasNext()) {
            UUID id = it.next();
            if (server.getPlayerList().getPlayer(id) == null) it.remove();
        }
    }

    private static void broadcastReadyProgress(MinecraftServer server, ReadyState st, int ready, int max) {
        var msg = Component.literal("Ready: ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(ready + "/" + max).withStyle(ChatFormatting.GREEN));

        for (UUID id : st.ordered) {
            ServerPlayer p = server.getPlayerList().getPlayer(id);
            if (p != null) p.sendSystemMessage(msg);
        }
    }

    /**
     * Slot-based teleport:
     * - player in slot i uses csbe.getSlotDestination(i)
     * - if missing/invalid -> abort and message devs/players (no partial teleport)
     */
    private static boolean teleportAllReadyBySlot(MinecraftServer server, ServerLevel selectorLevel, BlockPos selectorPos, ClassSelectorBlockEntity csbe, ReadyState st) {
        int max = Math.max(1, Math.min(64, csbe.getMaxPlayers()));
        int ready = st.ordered.size();
        if (ready < max) return false;

        RiftRegistryData data = RiftRegistryData.get(server);

        // Resolve destinations for each slot first, abort if any missing/invalid.
        List<RiftRegistryData.DestinationRecord> resolved = new ArrayList<>(max);
        List<Integer> missingSlots = new ArrayList<>();
        List<Integer> invalidSlots = new ArrayList<>();

        for (int slot = 1; slot <= max; slot++) {
            String name = csbe.getSlotDestination(slot);

            // Optional fallback for older setups (keeps old behavior if you want)
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

            resolved.add(destOpt.get());
        }

        if (!missingSlots.isEmpty() || !invalidSlots.isEmpty()) {
            // Tell ready players
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

            // Tell devs (more context)
            var devMsg = Component.literal("Teleport aborted: configure slot destinations for Class Selector @ ")
                    .withStyle(ChatFormatting.RED)
                    .append(Component.literal(selectorLevel.dimension().location().toString()).withStyle(ChatFormatting.AQUA))
                    .append(Component.literal(" ").withStyle(ChatFormatting.RED))
                    .append(Component.literal(selectorPos.toShortString()).withStyle(ChatFormatting.AQUA))
                    .append(Component.literal(". Use: ").withStyle(ChatFormatting.RED))
                    .append(Component.literal("/classselector ui dest " + selectorPos.getX() + " " + selectorPos.getY() + " " + selectorPos.getZ())
                            .withStyle(ChatFormatting.YELLOW));

            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                if (!AccessPolicy.isDeveloper(p)) continue;
                p.sendSystemMessage(devMsg);
            }

            return false;
        }

        // Teleport (no partial failures)
        var go = Component.literal("Teleporting!").withStyle(ChatFormatting.AQUA);

        int slot = 1;
        for (UUID id : st.ordered) {
            if (slot > max) break; // safety
            ServerPlayer p = server.getPlayerList().getPlayer(id);
            if (p == null) { slot++; continue; }

            RiftRegistryData.DestinationRecord dest = resolved.get(slot - 1);
            ServerLevel target = ClassSelectorTeleportUtil.resolveLevel(server, dest.dimensionId());
            if (target == null) {
                // This should be very rare; abort similarly
                p.sendSystemMessage(Component.literal("Teleport failed: target dimension not loaded.").withStyle(ChatFormatting.RED));
                return false;
            }

            BlockPos tp = dest.pos();
            target.getChunk(tp);

            p.sendSystemMessage(go);
            p.stopRiding();
            p.fallDistance = 0;

            p.teleportTo(
                    target,
                    tp.getX() + 0.5D,
                    (double) tp.getY(),
                    tp.getZ() + 0.5D,
                    Set.of(),
                    p.getYRot(),
                    p.getXRot(),
                    false
            );

            slot++;
        }

        return true;
    }
}
