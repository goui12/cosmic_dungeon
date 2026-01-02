package net.goui.cosmicdungeon.playerclass.ore;

import net.goui.cosmicdungeon.CosmicDungeonMod;
import net.goui.cosmicdungeon.playerclass.api.ClassNbtUtil;
import net.goui.cosmicdungeon.playerclass.metalmancer.MetalmancerResonanceTracker;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Stationary Resonance:
 *   +0.5 ore per second (~30 ore/min) while the Metalmancer is standing still on solid ground.
 *
 * If the Metalmancer's golem is co-resonating (standing still in resonance range),
 * this rate is doubled to 1.0 ore per second (~60 ore/min).
 *
 * This works whether the satchel is in the main inventory OR in the extra 3 slots
 * (via SatchelApi.get/capacity).
 */
@EventBusSubscriber(modid = CosmicDungeonMod.MOD_ID)
public final class SatchelIdleTicker {

    private SatchelIdleTicker() {}

    private static final class IdleState {
        double anchorX, anchorY, anchorZ;
        int ticks;
        boolean hasAnchor;

        void setAnchor(double x, double y, double z) {
            this.anchorX = x;
            this.anchorY = y;
            this.anchorZ = z;
            this.hasAnchor = true;
        }

        void reset(double x, double y, double z) {
            setAnchor(x, y, z);
            this.ticks = 0;
        }
    }

    private static final Map<UUID, IdleState> STATES = new HashMap<>();

    /** Players currently satisfying the Stationary Resonance conditions. */
    private static final Set<UUID> RESONATING_PLAYERS = new HashSet<>();

    /** Max movement per axis before we consider the player "not idle". */
    private static final double AXIS_THRESHOLD = 0.05;   // much tighter for "standing still"

    /** 20 ticks = 1 second at normal tickrate. */
    private static final int TICKS_PER_SECOND = 20;

    /** We want 0.5 ore / second => 1 ore every 2 seconds => 40 ticks. */
    private static final int TICKS_PER_ORE = TICKS_PER_SECOND * 2;

    /**
     * Returns true if the given Metalmancer is currently considered in
     * Stationary Resonance for ore income purposes:
     *  - has an IdleState tracked
     *  - is grounded
     *
     * We let the tick loop clear STATES when moving, jumping, or losing the satchel,
     * so presence in STATES is effectively "currently resonating".
     */
    public static boolean isPlayerResonating(ServerPlayer sp) {
        IdleState state = STATES.get(sp.getUUID());
        if (state == null || !state.hasAnchor) return false;
        if (!sp.onGround()) return false;
        return true;
    }


    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        if (sp.level().isClientSide()) return; // server logical side only

        UUID id = sp.getUUID();

        // Only Metalmancers gain Stationary Resonance
        if (!ClassNbtUtil.isMetalmancer(sp)) {
            STATES.remove(id);
            RESONATING_PLAYERS.remove(id);
            return;
        }

        if (sp.isDeadOrDying()) {
            STATES.remove(id);
            RESONATING_PLAYERS.remove(id);
            return;
        }

        // Must have *some* satchel (main inv OR extra PD). If none, clear state.
        boolean hasSatchel =
                !SatchelApi.findSatchelInMainInv(sp).isEmpty()
                        || SatchelApi.capacity(sp) > 0;

        if (!hasSatchel) {
            STATES.remove(id);
            RESONATING_PLAYERS.remove(id);
            return;
        }

        IdleState state = STATES.computeIfAbsent(id, k -> new IdleState());

        double x = sp.getX();
        double y = sp.getY();
        double z = sp.getZ();

        if (!state.hasAnchor) {
            state.reset(x, y, z);
        }

        // Must be grounded on terrain (no flying / swimming resonance)
        if (!sp.onGround()) {
            state.reset(x, y, z);
            RESONATING_PLAYERS.remove(id);
            return;
        }

        boolean moved =
                Math.abs(x - state.anchorX) > AXIS_THRESHOLD ||
                        Math.abs(y - state.anchorY) > AXIS_THRESHOLD ||
                        Math.abs(z - state.anchorZ) > AXIS_THRESHOLD;

        if (moved) {
            state.reset(x, y, z);
            RESONATING_PLAYERS.remove(id);
            return;
        }

        // At this point, the player is satisfying Stationary Resonance conditions.
        RESONATING_PLAYERS.add(id);

        // Idle accumulation
        if (++state.ticks >= TICKS_PER_ORE) {
            state.ticks = 0;

            int ore = SatchelApi.get(sp);
            int cap = SatchelApi.capacity(sp);
            if (cap <= 0) cap = SatchelOfSamplesItem.DEFAULT_CAPACITY;

            if (ore < cap) {
                // If the golem is co-resonating, double the payout per step.
                int perStep = MetalmancerResonanceTracker.isGolemResonatingNear(sp) ? 2 : 1;

                // Adds in main inventory OR extra PD, and syncs appropriately.
                SatchelApi.add(sp, perStep);
            }
        }

    }

}
