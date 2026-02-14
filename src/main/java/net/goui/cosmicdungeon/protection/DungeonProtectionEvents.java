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
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.entity.player.UseItemOnBlockEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.level.block.CropGrowEvent;
import net.neoforged.neoforge.event.level.BlockGrowFeatureEvent;

import java.util.Iterator;
import java.util.List;

@EventBusSubscriber(modid = CosmicDungeonMod.MOD_ID)
public final class DungeonProtectionEvents {
    private DungeonProtectionEvents() {}

    // ---- Main flags ----
    private static final String FLAG_PLACE    = "place";
    private static final String FLAG_BREAK    = "break";
    private static final String FLAG_INTERACT = "interact";
    private static final String FLAG_EXPLODE  = "explode";
    private static final String FLAG_SPREAD   = "spread";
    private static final String FLAG_BURN     = "burn";

    // ---- Exception keys (stored in RegionRegistryData flags map) ----
    private static final String KEY_PLACE_EX_PREFIX = "place.ex.";
    private static final String KEY_BREAK_EX_PREFIX = "break.ex.";

    private static final String EX_TORCH  = "torch";
    private static final String EX_LADDER = "ladder";
    private static final String EX_WATER  = "water";

    /* ===================================================================================== */

    private static RegionRegistryData.Region effectiveRegion(ServerLevel level, BlockPos pos) {
        RegionRegistryData data = RegionRegistryData.get(level);
        List<RegionRegistryData.Region> regions = data.regionsAt(level, pos);
        if (regions.isEmpty()) return null;
        return data.effectiveRegionFromList(regions);
    }

    private static boolean resolveFlag(ServerLevel level, RegionRegistryData.Region region, String key, boolean def) {
        return RegionRegistryData.get(level).resolveFlagBool(region, key, def).value();
    }

    private static boolean resolveException(ServerLevel level, RegionRegistryData.Region region, String scope, String ex, boolean def) {
        String flagKey = ("place".equals(scope) ? KEY_PLACE_EX_PREFIX : KEY_BREAK_EX_PREFIX) + ex;
        return RegionRegistryData.get(level).resolveExceptionBool(region, flagKey, def).value();
    }

    /**
     * IMPORTANT:
     * Don't let RightClickBlock (interact gate) block actions that should be controlled
     * by place/break/burn/tool/bucket events.
     */
    private static boolean isPlacementOrWorldModifyAttempt(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        if (stack.getItem() instanceof BlockItem) return true;
        if (stack.getItem() instanceof BucketItem) return true;
        return stack.is(Items.FLINT_AND_STEEL) || stack.is(Items.FIRE_CHARGE);
    }

    /* =====================================================================================
       Classification helpers
       ===================================================================================== */

    private static boolean isTorchLike(BlockState state) {
        if (state == null) return false;

        // Preferred: tag-driven
        if (state.is(ModTags.Blocks.EX_PLACE_TORCH)
                || state.is(ModTags.Blocks.EX_BREAK_TORCH)
                || state.is(ModTags.Blocks.REGION_EX_TORCH_BLOCKS)) {
            return true;
        }

        // Hard fallback
        Block b = state.getBlock();
        return b == Blocks.TORCH
                || b == Blocks.WALL_TORCH
                || b == Blocks.SOUL_TORCH
                || b == Blocks.SOUL_WALL_TORCH
                || b == Blocks.REDSTONE_TORCH
                || b == Blocks.REDSTONE_WALL_TORCH;
    }

    private static boolean isLadderLike(BlockState state) {
        if (state == null) return false;

        // Preferred: tag-driven
        if (state.is(ModTags.Blocks.EX_PLACE_LADDER)
                || state.is(ModTags.Blocks.EX_BREAK_LADDER)
                || state.is(ModTags.Blocks.REGION_EX_LADDER_BLOCKS)) {
            return true;
        }

        // Hard fallback (mirrors your region_ex_ladder_blocks list + common “plant” variants)
        Block b = state.getBlock();
        return b == Blocks.LADDER
                || b == Blocks.VINE
                || b == Blocks.WEEPING_VINES
                || b == Blocks.WEEPING_VINES_PLANT
                || b == Blocks.TWISTING_VINES
                || b == Blocks.TWISTING_VINES_PLANT
                || b == Blocks.SCAFFOLDING;
    }

    private static boolean isWaterLike(BlockState state) {
        if (state == null) return false;

        // Preferred: tag-driven
        if (state.is(ModTags.Blocks.EX_PLACE_WATER)) return true;

        // Hard fallback for FluidPlaceBlockEvent
        Block b = state.getBlock();
        return b == Blocks.WATER || b == Blocks.BUBBLE_COLUMN;
    }

    /* =====================================================================================
       BREAK
       ===================================================================================== */

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onBlockBreak(BlockEvent.BreakEvent e) {
        if (!(e.getPlayer() instanceof ServerPlayer sp)) return;
        if (!(e.getLevel() instanceof ServerLevel level)) return;
        if (AccessPolicy.isDeveloper(sp)) return;

        BlockPos pos = e.getPos();
        RegionRegistryData.Region region = effectiveRegion(level, pos);
        if (region == null) return;

        boolean breakAllowed = resolveFlag(level, region, FLAG_BREAK, false); // default deny
        if (breakAllowed) return;

        BlockState state = e.getState();

        // Exceptions (default allow ONLY for torch)
        boolean torchEx = resolveException(level, region, "break", EX_TORCH, true);
        if (torchEx && isTorchLike(state)) return;

        boolean ladderEx = resolveException(level, region, "break", EX_LADDER, false);
        if (ladderEx && isLadderLike(state)) return;

        // Designer allowlist tag
        if (state.is(ModTags.Blocks.DUNGEONEER_BREAKABLE)) return;

        e.setCanceled(true);
        AccessPolicy.deny(sp, "You cannot break blocks here.");
    }

    /* =====================================================================================
       PLACE (blocks)
       ===================================================================================== */

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent e) {
        Entity ent = e.getEntity();
        if (!(ent instanceof ServerPlayer sp)) return;
        if (!(e.getLevel() instanceof ServerLevel level)) return;
        if (AccessPolicy.isDeveloper(sp)) return;

        BlockPos pos = e.getPos();
        RegionRegistryData.Region region = effectiveRegion(level, pos);
        if (region == null) return;

        boolean placeAllowed = resolveFlag(level, region, FLAG_PLACE, false); // default deny
        if (placeAllowed) return;

        BlockState placed = e.getPlacedBlock();

        // Exceptions (default allow ONLY for torch)
        boolean torchEx = resolveException(level, region, "place", EX_TORCH, true);
        if (torchEx && isTorchLike(placed)) return;

        boolean ladderEx = resolveException(level, region, "place", EX_LADDER, false);
        if (ladderEx && isLadderLike(placed)) return;

        // Designer allowlist tag
        if (placed.is(ModTags.Blocks.DUNGEONEER_PLACEABLE)) return;

        e.setCanceled(true);
        AccessPolicy.deny(sp, "You cannot place blocks here.");
    }

    /* =====================================================================================
       BUCKETS / ITEM-ON-BLOCK
       ===================================================================================== */

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onUseItemOnBlock(UseItemOnBlockEvent e) {
        if (!(e.getLevel() instanceof ServerLevel level)) return;
        if (!(e.getPlayer() instanceof ServerPlayer sp)) return;
        if (AccessPolicy.isDeveloper(sp)) return;

        ItemStack held = e.getItemStack();
        if (held == null || held.isEmpty()) return;
        if (!(held.getItem() instanceof BucketItem)) return;

        BlockPos pos = e.getPos();
        RegionRegistryData.Region region = effectiveRegion(level, pos);
        if (region == null) return;

        boolean placeAllowed = resolveFlag(level, region, FLAG_PLACE, false);
        if (placeAllowed) return;

        // Allow ONLY water bucket if exception says so
        if (held.is(Items.WATER_BUCKET)) {
            boolean waterEx = resolveException(level, region, "place", EX_WATER, false);
            if (waterEx) return;
        }

        e.cancelWithResult(InteractionResult.FAIL);
        AccessPolicy.deny(sp, "You cannot use buckets here.");
    }

    /* =====================================================================================
       FLUID PLACE BACKSTOP (spread / dispensers / bucket follow-up)
       ===================================================================================== */

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onFluidPlace(BlockEvent.FluidPlaceBlockEvent e) {
        if (!(e.getLevel() instanceof ServerLevel level)) return;

        BlockPos pos = e.getPos();
        RegionRegistryData.Region region = effectiveRegion(level, pos);
        if (region == null) return;

        boolean placeAllowed = resolveFlag(level, region, FLAG_PLACE, false);
        if (placeAllowed) return;

        BlockState newState = e.getNewState();

        // If water is explicitly allowed, do NOT block the resulting fluid placement.
        if (isWaterLike(newState)) {
            boolean waterEx = resolveException(level, region, "place", EX_WATER, false);
            if (waterEx) return;
        }

        e.setCanceled(true);
    }

    /* =====================================================================================
       TOOL MODIFICATION (stripping, flattening, etc)
       ===================================================================================== */

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onToolModify(BlockEvent.BlockToolModificationEvent e) {
        if (!(e.getPlayer() instanceof ServerPlayer sp)) return;
        if (!(e.getLevel() instanceof ServerLevel level)) return;
        if (AccessPolicy.isDeveloper(sp)) return;

        BlockPos pos = e.getPos();
        RegionRegistryData.Region region = effectiveRegion(level, pos);
        if (region == null) return;

        boolean interactAllowed = resolveFlag(level, region, FLAG_INTERACT, false);
        if (interactAllowed) return;

        e.setCanceled(true);
        AccessPolicy.deny(sp, "You cannot modify blocks here.");
    }

    /* =====================================================================================
       INTERACT (right click block)
       ===================================================================================== */

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock e) {
        if (!(e.getEntity() instanceof ServerPlayer sp)) return;
        if (!(e.getLevel() instanceof ServerLevel level)) return;
        if (AccessPolicy.isDeveloper(sp)) return;

        BlockPos pos = e.getPos();
        RegionRegistryData.Region region = effectiveRegion(level, pos);
        if (region == null) return;

        ItemStack held = e.getItemStack();

        if (isPlacementOrWorldModifyAttempt(held)) {
            // Ignition is gated by BURN, not INTERACT
            if (held.is(Items.FLINT_AND_STEEL) || held.is(Items.FIRE_CHARGE)) {
                boolean burnAllowed = resolveFlag(level, region, FLAG_BURN, false);
                if (!burnAllowed) {
                    e.setCanceled(true);
                    e.setCancellationResult(InteractionResult.FAIL);
                    AccessPolicy.deny(sp, "You cannot ignite blocks here.");
                }
            }
            return;
        }

        boolean interactAllowed = resolveFlag(level, region, FLAG_INTERACT, false);
        if (interactAllowed) return;

        BlockState state = level.getBlockState(pos);

        if (!AccessPolicy.allowClassGatedVanillaUse(sp, state.getBlock())) {
            e.setCanceled(true);
            e.setCancellationResult(InteractionResult.FAIL);
            return;
        }

        if (state.is(ModTags.Blocks.DUNGEONEER_INTERACTABLE)) return;

        e.setCanceled(true);
        e.setCancellationResult(InteractionResult.FAIL);
        AccessPolicy.deny(sp, "You cannot use that here.");
    }

    /* =====================================================================================
       ENTITY BREAKING (armor stands, item frames, paintings, etc)
       ===================================================================================== */

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onAttackEntity(AttackEntityEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer sp)) return;
        if (!(sp.level() instanceof ServerLevel level)) return;
        if (AccessPolicy.isDeveloper(sp)) return;

        Entity target = e.getTarget();
        if (target == null) return;

        if (!(target instanceof ArmorStand) && !(target instanceof HangingEntity)) return;

        BlockPos pos = target.blockPosition();
        RegionRegistryData.Region region = effectiveRegion(level, pos);
        if (region == null) return;

        boolean interactAllowed = resolveFlag(level, region, FLAG_INTERACT, false);
        if (interactAllowed) return;

        e.setCanceled(true);
        AccessPolicy.deny(sp, "You cannot break that here.");
    }

    /* =====================================================================================
       EXPLOSIONS (remove block damage only; keep entity damage)
       ===================================================================================== */

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onExplosionDetonate(ExplosionEvent.Detonate e) {
        if (!(e.getLevel() instanceof ServerLevel level)) return;

        List<BlockPos> blocks = e.getAffectedBlocks();
        if (blocks.isEmpty()) return;

        for (Iterator<BlockPos> it = blocks.iterator(); it.hasNext();) {
            BlockPos pos = it.next();
            RegionRegistryData.Region region = effectiveRegion(level, pos);
            if (region == null) continue;

            boolean explodeAllowed = RegionRegistryData.get(level).resolveFlagBool(region, FLAG_EXPLODE, false).value();
            if (explodeAllowed) continue;

            it.remove();
        }
    }

    /* =====================================================================================
       SPREAD / GROWTH
       ===================================================================================== */

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onCropGrow(CropGrowEvent.Pre e) {
        if (!(e.getLevel() instanceof ServerLevel level)) return;

        BlockPos pos = e.getPos();
        RegionRegistryData.Region region = effectiveRegion(level, pos);
        if (region == null) return;

        boolean spreadAllowed = RegionRegistryData.get(level).resolveFlagBool(region, FLAG_SPREAD, false).value();
        if (spreadAllowed) return;

        e.setResult(CropGrowEvent.Pre.Result.DO_NOT_GROW);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onBlockGrowFeature(BlockGrowFeatureEvent e) {
        if (!(e.getLevel() instanceof ServerLevel level)) return;

        BlockPos pos = e.getPos();
        RegionRegistryData.Region region = effectiveRegion(level, pos);
        if (region == null) return;

        boolean spreadAllowed = RegionRegistryData.get(level).resolveFlagBool(region, FLAG_SPREAD, false).value();
        if (spreadAllowed) return;

        e.setCanceled(true);
    }
}