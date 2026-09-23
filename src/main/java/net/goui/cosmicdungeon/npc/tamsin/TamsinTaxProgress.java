package net.goui.cosmicdungeon.npc.tamsin;

import net.goui.cosmicdungeon.Config;
import net.goui.cosmicdungeon.CosmicDungeonMod;
import net.goui.cosmicdungeon.achievement.d1.D1ObjectiveBindings;
import net.goui.cosmicdungeon.dungeon.*;
import net.goui.cosmicdungeon.dungeon.d1.D1Members;
import net.goui.cosmicdungeon.playerclass.api.ClassData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** Permanent, personal proof; never infer discovery from party membership or possession of loot.
 * Stored under the existing clone-preserved class root, outside run_temp and run objectives. */
@EventBusSubscriber(modid = CosmicDungeonMod.MOD_ID)
public final class TamsinTaxProgress {
    public static final String KEY = "tamsin_tax_v1";
    private TamsinTaxProgress() {}
    static CompoundTag read(ServerPlayer player) {
        return player.getPersistentData().getCompoundOrEmpty(ClassData.ROOT_TAG).getCompoundOrEmpty(KEY);
    }
    static void write(ServerPlayer player, CompoundTag state) {
        var root = player.getPersistentData().getCompoundOrEmpty(ClassData.ROOT_TAG).copy();
        root.put(KEY, state);
        player.getPersistentData().put(ClassData.ROOT_TAG, root);
    }
    public static boolean available(ServerPlayer player) {
        var state = read(player);
        return TamsinTaxRules.qualifies(state.getLongOr("camp_run", 0),
                state.getLongOr("success_run", 0), state.contains("receipt"));
    }
    public static void successfulRun(ServerPlayer player, long runId) {
        write(player, successfulImage(read(player), runId));
    }
    public static CompoundTag successfulImage(CompoundTag before, long runId) {
        var state = before.copy();
        long camp = state.getLongOr("camp_run", 0);
        if (camp > 0 && runId >= camp && state.getLongOr("success_run", 0) == 0)
            state.putLong("success_run", runId);
        return state;
    }
    @SubscribeEvent
    public static void tick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || player.tickCount % Config.BASE_CAMP_POLL_TICKS.get() != 0
                || read(player).getLongOr("camp_run", 0) > 0) return;
        var run = D1Members.run(player.level()).orElse(null);
        if (run == null || !D1Members.inside(player, run)) return;
        var place = D1ObjectiveBindings.get(player.level().getServer()).location("base_camp");
        if (place == null || !place.dimension().equals(DungeonInstanceSlots.templateDimensionForPhysical(
                player.level().getServer(), player.level().dimension()).location().toString())) return;
        double radius = Config.BASE_CAMP_RADIUS.get();
        if (player.distanceToSqr(Vec3.atCenterOf(place.pos())) > radius * radius) return;
        var state = read(player).copy(); state.putLong("camp_run", run.runId()); write(player, state);
    }
    // TODO(M39/M81, licensed TEST): bind base_camp to the authored camp via /d1 objective bind.
    // Tax Doc 2026-08-19 requires personal discovery BEFORE first qualifying success. Old saves
    // without that proof remain unqualified until a verified visit and subsequent success; do not
    // backfill from party flags, existing completions, inventory contents, or a guessed coordinate.
}
