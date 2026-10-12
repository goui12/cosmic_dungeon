package net.goui.cosmicdungeon.crafting;

import java.util.*;
import net.goui.cosmicdungeon.network.*;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Only explicitly opened palettes receive bounded delta quotes. Revisions never reset on close/reopen. */
@EventBusSubscriber(modid = "cosmicdungeon")
public final class PotionCraftActions {
    private static final class Session {
        boolean open, warned, acknowledgePending; long revision, lastAction = Long.MIN_VALUE;
        PotionCraftPayloads.View sent;
    }
    private static final Map<ServerPlayer, Session> SESSIONS = new WeakHashMap<>();
    private PotionCraftActions() {}
    public static void request(ServerPlayer p, PotionCraftPayloads.Request request) {
        var session = SESSIONS.computeIfAbsent(p, ignored -> new Session());
        if (!request.open()) { session.open = false; session.sent = null; session.acknowledgePending = false; session.revision++; PotionCrafting.forget(p); return; }
        if (session.open) return;
        session.open = true; sync(p, session, true);
    }
    private static void sync(ServerPlayer p, Session session, boolean acknowledge) {
        if (!session.open) return;
        acknowledge |= session.acknowledgePending;
        session.acknowledgePending = false;
        PotionCraftPayloads.View next;
        try { next = PotionCrafting.view(p, session.revision); }
        catch (RuntimeException ex) {
            if (!session.warned) {
                session.warned = true;
                com.mojang.logging.LogUtils.getLogger().error("Potion palette unavailable; existing items retained for {}", p.getUUID(), ex);
            }
            next = PotionCraftPayloads.View.empty(session.revision);
        }
        if (!acknowledge && next.equals(session.sent)) return;
        session.revision = Math.incrementExact(session.revision);
        session.sent = new PotionCraftPayloads.View(next.run(), session.revision, next.classId(), next.charges(), next.perPowder(), next.entries());
        ModNetwork.sendTo(p, session.sent);
        if (next.run() == 0) session.open = false;
    }
    public static void action(ServerPlayer p, PotionCraftPayloads.Action request) {
        var session = SESSIONS.get(p); if (session == null || !session.open) return;
        // Reject repeated input before the acknowledgement finally block: rejected duplicates
        // must not rebuild and resend the entire palette once per packet.
        long tick = p.level().getServer().getTickCount();
        if (tick == session.lastAction) {
            // A second legitimate click may arrive after the first reply in the same tick.
            // Coalesce its acknowledgement into the next bounded periodic sync so the UI unlocks.
            session.acknowledgePending = true;
            return;
        }
        session.lastAction = tick;
        try {
            var sent = session.sent;
            if (sent == null || sent.run() != request.run() || sent.revision() != request.revision()) return;
            var entry = sent.entries().stream().filter(e -> e.targetId().equals(request.targetId())).findFirst().orElse(null);
            if (entry == null || entry.own() == 0 && entry.shared() == 0) return;
            PotionCrafting.craft(p, request.run(), request.targetId(), request.shift());
        } finally {
            // Acknowledge even stale/full/mismatched requests so the UI cannot remain stuck waiting.
            sync(p, session, true);
        }
    }
    @SubscribeEvent public static void tick(ServerTickEvent.Post e) {
        if (e.getServer().getTickCount() % 5 != 0) return;
        for (var row : List.copyOf(SESSIONS.entrySet())) {
            var p = row.getKey();
            if (p.level().getServer() == e.getServer() && row.getValue().open) sync(p, row.getValue(), false);
        }
    }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) { SESSIONS.remove(p); PotionCrafting.forget(p); }
    }
    public static void invalidate(ServerPlayer p) {
        var session = SESSIONS.get(p);
        if (session != null) { session.open = false; session.sent = null; session.acknowledgePending = false; session.revision = Math.incrementExact(session.revision); }
        PotionCrafting.forget(p);
    }
    public static void transfer(ServerPlayer original, ServerPlayer replacement) {
        invalidate(original); var session = SESSIONS.remove(original);
        if (session != null) SESSIONS.put(replacement, session);
    }
    @SubscribeEvent public static void dimension(PlayerEvent.PlayerChangedDimensionEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) invalidate(p);
    }
    @SubscribeEvent public static void clone(PlayerEvent.Clone e) {
        if (e.getOriginal() instanceof ServerPlayer original && e.getEntity() instanceof ServerPlayer replacement)
            transfer(original, replacement);
    }
    public static void forget(ServerPlayer p) { SESSIONS.remove(p); PotionCrafting.forget(p); }
    public static void stop() { SESSIONS.clear(); }
}
