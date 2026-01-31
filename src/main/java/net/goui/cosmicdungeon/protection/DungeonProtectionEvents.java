// file: src/main/java/net/goui/cosmicdungeon/protection/DungeonProtectionEvents.java
package net.goui.cosmicdungeon.protection;

import net.goui.cosmicdungeon.CosmicDungeonMod;
import net.goui.cosmicdungeon.auth.AccessPolicy;
import net.goui.cosmicdungeon.region.RegionRegistryData;
import net.goui.cosmicdungeon.util.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityMobGriefingEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.BlockGrowFeatureEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.level.block.CropGrowEvent;

import java.util.Iterator;
import java.util.List;

@EventBusSubscriber(modid = CosmicDungeonMod.MOD_ID)
public final class DungeonProtectionEvents {

    private DungeonProtectionEvents() {}

    // Base flags
    private static final String FLAG_PLACE = "place";
    private static final String FLAG_BREAK = "break";
    private static final String FLAG_BUILD_LEGACY = "build";

    private static final String FLAG_INTERACT = "interact";
    private static final String FLAG_EXPLODE  = "explode";
    private static final String FLAG_MOBGRIEF = "mobgrief";
    private static final String FLAG_SPREAD   = "spread";

    // Exception keys
    private static final String EX_PLACE_TORCH  = "place.ex.torch";
    private static final String EX_PLACE_LADDER = "place.ex.ladder";
    private static final String EX_PLACE_WATER  = "place.ex.water";

    private static final String EX_BREAK_TORCH  = "break.ex.torch";
    private static final String EX_BREAK_LADDER = "break.ex.ladder";

    private static RegionRegistryData.Region effectiveRegionAt(RegionRegistryData data, ServerLevel level, BlockPos pos) {
        List<RegionRegistryData.Region> regions = data.regionsAt(level, pos);
        if (regions.isEmpty()) return null;
        return data.effectiveRegionAt(level, pos);
    }

    private static boolean allowPlaceResolved(RegionRegistryData data, RegionRegistryData.Region r) {
        boolean place = data.resolveFlagBool(r, FLAG_PLACE, false).value();
        if (place) return true;
        return data.resolveFlagBool(r, FLAG_BUILD_LEGACY, false).value();
    }

    private static boolean allowBreakResolved(RegionRegistryData data, RegionRegistryData.Region r) {
        boolean brk = data.resolveFlagBool(r, FLAG_BREAK, false).value();
        if (brk) return true;
        return data.resolveFlagBool(r, FLAG_BUILD_LEGACY, false).value();
    }

    /* -------------------- BREAK -------------------- */

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onBlockBreak(BlockEvent.BreakEvent e) {
        if (!(e.getPlayer() instanceof ServerPlayer sp)) return;
        if (!(e.getLevel() instanceof ServerLevel level)) return;
        if (AccessPolicy.isDeveloper(sp)) return;

        BlockPos pos = e.getPos();
        RegionRegistryData data = RegionRegistryData.get(level);

        RegionRegistryData.Region region = effectiveRegionAt(data, level, pos);
        if (region == null) return;

        if (allowBreakResolved(data, region)) return;

        BlockState state = e.getState();

        if (state.is(ModTags.Blocks.DUNGEONEER_BREAKABLE)) return;

        // Exceptions: torch default allow, ladder default deny
        if (state.is(ModTags.Blocks.EX_BREAK_TORCH)
                && data.resolveExceptionBool(region, EX_BREAK_TORCH, true).value()) return;

        if (state.is(ModTags.Blocks.EX_BREAK_LADDER)
                && data.resolveExceptionBool(region, EX_BREAK_LADDER, false).value()) return;

        e.setCanceled(true);
        AccessPolicy.deny(sp, "You cannot break blocks here.");
    }

    /* -------------------- PLACE -------------------- */

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent e) {
        Entity ent = e.getEntity();
        if (!(ent instanceof ServerPlayer sp)) return;

        LevelAccessor accessor = e.getLevel();
        if (!(accessor instanceof ServerLevel level)) return;
        if (AccessPolicy.isDeveloper(sp)) return;

        BlockPos pos = e.getPos();
        RegionRegistryData data = RegionRegistryData.get(level);

        RegionRegistryData.Region region = effectiveRegionAt(data, level, pos);
        if (region == null) return;

        if (allowPlaceResolved(data, region)) return;

        BlockState placed = e.getPlacedBlock();

        if (placed.is(ModTags.Blocks.DUNGEONEER_PLACEABLE)) return;

        if (placed.is(ModTags.Blocks.EX_PLACE_TORCH)
                && data.resolveExceptionBool(region, EX_PLACE_TORCH, true).value()) return;

        if (placed.is(ModTags.Blocks.EX_PLACE_LADDER)
                && data.resolveExceptionBool(region, EX_PLACE_LADDER, false).value()) return;

        e.setCanceled(true);
        AccessPolicy.deny(sp, "You cannot place blocks here.");
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onFluidPlace(BlockEvent.FluidPlaceBlockEvent e) {
        LevelAccessor accessor = e.getLevel();
        if (!(accessor instanceof ServerLevel level)) return;

        BlockPos pos = e.getPos();
        RegionRegistryData data = RegionRegistryData.get(level);

        RegionRegistryData.Region region = effectiveRegionAt(data, level, pos);
        if (region == null) return;

        if (allowPlaceResolved(data, region)) return;

        // Water exception: default deny
        if (data.resolveExceptionBool(region, EX_PLACE_WATER, false).value()) return;

        e.setCanceled(true);
    }

    /* -------------------- INTERACT -------------------- */

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock e) {
        if (!(e.getEntity() instanceof ServerPlayer sp)) return;

        Level lvl = e.getLevel();
        if (!(lvl instanceof ServerLevel level)) return;

        BlockPos pos = e.getPos();
        RegionRegistryData data = RegionRegistryData.get(level);

        RegionRegistryData.Region region = effectiveRegionAt(data, level, pos);
        if (region == null) return;

        if (AccessPolicy.isDeveloper(sp)) return;

        BlockState state = level.getBlockState(pos);

        if (!AccessPolicy.allowClassGatedVanillaUse(sp, state.getBlock())) {
            e.setCanceled(true);
            e.setCancellationResult(InteractionResult.FAIL);
            return;
        }

        if (data.resolveFlagBool(region, FLAG_INTERACT, false).value()) return;
        if (state.is(ModTags.Blocks.DUNGEONEER_INTERACTABLE)) return;

        e.setCanceled(true);
        e.setCancellationResult(InteractionResult.FAIL);
        AccessPolicy.deny(sp, "You cannot use that here.");
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract e) {
        if (!(e.getEntity() instanceof ServerPlayer sp)) return;
        if (AccessPolicy.isDeveloper(sp)) return;
        if (!(sp.level() instanceof ServerLevel level)) return;

        Entity target = e.getTarget();
        if (target == null) return;

        BlockPos pos = target.blockPosition();
        RegionRegistryData data = RegionRegistryData.get(level);

        RegionRegistryData.Region region = effectiveRegionAt(data, level, pos);
        if (region == null) return;

        if (data.resolveFlagBool(region, FLAG_INTERACT, false).value()) return;

        e.setCanceled(true);
        e.setCancellationResult(InteractionResult.FAIL);
        AccessPolicy.deny(sp, "You cannot interact with that here.");
    }

    /* -------------------- EXPLODE -------------------- */

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onExplosionDetonate(ExplosionEvent.Detonate e) {
        if (!(e.getLevel() instanceof ServerLevel level)) return;

        RegionRegistryData data = RegionRegistryData.get(level);
        List<BlockPos> blocks = e.getAffectedBlocks();
        if (blocks.isEmpty()) return;

        for (Iterator<BlockPos> it = blocks.iterator(); it.hasNext();) {
            BlockPos pos = it.next();

            RegionRegistryData.Region region = effectiveRegionAt(data, level, pos);
            if (region == null) continue;

            if (data.resolveFlagBool(region, FLAG_EXPLODE, false).value()) continue;

            it.remove();
        }
    }

    /* -------------------- MOBGRIEF -------------------- */

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onMobGrief(EntityMobGriefingEvent e) {
        if (!(e.getEntity().level() instanceof ServerLevel level)) return;
        if (!e.canGrief()) return;

        RegionRegistryData data = RegionRegistryData.get(level);
        BlockPos pos = e.getEntity().blockPosition();

        RegionRegistryData.Region region = effectiveRegionAt(data, level, pos);
        if (region == null) return;

        if (data.resolveFlagBool(region, FLAG_MOBGRIEF, false).value()) return;

        e.setCanGrief(false);
    }

    /* -------------------- SPREAD / GROW -------------------- */

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onCropGrow(CropGrowEvent.Pre e) {
        LevelAccessor accessor = e.getLevel();
        if (!(accessor instanceof ServerLevel level)) return;

        BlockPos pos = e.getPos();
        RegionRegistryData data = RegionRegistryData.get(level);

        RegionRegistryData.Region region = effectiveRegionAt(data, level, pos);
        if (region == null) return;

        if (data.resolveFlagBool(region, FLAG_SPREAD, false).value()) return;

        e.setResult(CropGrowEvent.Pre.Result.DO_NOT_GROW);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onBlockGrowFeature(BlockGrowFeatureEvent e) {
        LevelAccessor accessor = e.getLevel();
        if (!(accessor instanceof ServerLevel level)) return;

        BlockPos pos = e.getPos();
        RegionRegistryData data = RegionRegistryData.get(level);

        RegionRegistryData.Region region = effectiveRegionAt(data, level, pos);
        if (region == null) return;

        if (data.resolveFlagBool(region, FLAG_SPREAD, false).value()) return;

        e.setCanceled(true);
    }
}
