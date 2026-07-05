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
import net.minecraft.world.item.Item;
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
import net.neoforged.neoforge.event.level.BlockGrowFeatureEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.level.block.CropGrowEvent;

import java.util.Iterator;
import java.util.List;

@EventBusSubscriber(modid = CosmicDungeonMod.MOD_ID)
public final class DungeonProtectionEvents {
    private DungeonProtectionEvents() {}

    // ---- Core region flags ----
    private static final String FLAG_PLACE    = "place";
    private static final String FLAG_BREAK    = "break";
    private static final String FLAG_INTERACT = "interact";
    private static final String FLAG_EXPLODE  = "explode";
    private static final String FLAG_SPREAD   = "spread";
    private static final String FLAG_BURN     = "burn";

    // ---- Exception flag key prefixes ----
    // Stored inside RegionRegistryData as:
    //   place.ex.<name>
    //   break.ex.<name>
    private static final String KEY_PLACE_EX_PREFIX = "place.ex.";
    private static final String KEY_BREAK_EX_PREFIX = "break.ex.";

    // ---- Exception identifiers ----
    private static final String EX_TORCH  = "torch";
    private static final String EX_LADDER = "ladder";
    private static final String EX_WATER  = "water";

    /* ===================================================================================== */

    /**
     * Returns the effective region at a position,
     * resolving overlaps via RegionRegistryData rules.
     */
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
     * Identifies items that modify the world (placement / buckets / ignition).
     * These are handled by placement-specific events, not interact gating.
     */
    private static boolean isPlacementOrWorldModifyAttempt(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        if (stack.getItem() instanceof BlockItem) return true;
        if (stack.getItem() instanceof BucketItem) return true;
        return stack.is(Items.FLINT_AND_STEEL) || stack.is(Items.FIRE_CHARGE);
    }

    /* =====================================================================================
       BLOCK CLASSIFICATION HELPERS
       ===================================================================================== */

    /**
     * Determines whether a BlockState is considered torch-like.
     * Used in break/place backstop events.
     */
    private static boolean isTorchLike(BlockState state) {
        if (state == null) return false;

        // Preferred: tag-driven classification
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

    /**
     * Determines whether a BlockState is considered ladder-like.
     */
    private static boolean isLadderLike(BlockState state) {
        if (state == null) return false;

        if (state.is(ModTags.Blocks.EX_PLACE_LADDER)
                || state.is(ModTags.Blocks.EX_BREAK_LADDER)
                || state.is(ModTags.Blocks.REGION_EX_LADDER_BLOCKS)) {
            return true;
        }

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
        if (state.is(ModTags.Blocks.EX_PLACE_WATER)) return true;

        Block b = state.getBlock();
        return b == Blocks.WATER || b == Blocks.BUBBLE_COLUMN;
    }

    /* =====================================================================================
       ITEM CLASSIFICATION HELPERS
       ===================================================================================== */

    /**
     * Detects whether a held ItemStack will place a torch-like block.
     * Uses direct item checks first (most reliable),
     * then falls back to block/tag detection.
     */
    private static boolean isTorchItem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;

        if (stack.is(Items.TORCH)
                || stack.is(Items.SOUL_TORCH)
                || stack.is(Items.REDSTONE_TORCH)) {
            return true;
        }

        Item item = stack.getItem();
        if (item instanceof BlockItem bi) {
            Block b = bi.getBlock();
            BlockState defaultState = b.defaultBlockState();
            if (isTorchLike(defaultState)) return true;

            return b == Blocks.TORCH
                    || b == Blocks.WALL_TORCH
                    || b == Blocks.SOUL_TORCH
                    || b == Blocks.SOUL_WALL_TORCH
                    || b == Blocks.REDSTONE_TORCH
                    || b == Blocks.REDSTONE_WALL_TORCH;
        }

        return false;
    }

    /**
     * Detects whether a held ItemStack will place a ladder-like block.
     */
    private static boolean isLadderItem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        Item item = stack.getItem();

        if (item instanceof BlockItem bi) {
            Block b = bi.getBlock();
            BlockState defaultState = b.defaultBlockState();
            if (isLadderLike(defaultState)) return true;

            return b == Blocks.LADDER
                    || b == Blocks.VINE
                    || b == Blocks.SCAFFOLDING;
        }
        return false;
    }

    /* =====================================================================================
       USE-ITEM-ON-BLOCK
       ===================================================================================== */

    /**
     * This is the EARLIEST safe interception point for block placement and bucket usage.
     *
     * NeoForge fires UseItemOnBlockEvent in three phases:
     *   - ITEM_BEFORE_BLOCK  (before the item's useOn logic runs)
     *   - BLOCK              (block interaction phase)
     *   - ITEM_AFTER_BLOCK   (after item logic runs)
     *
     * To prevent item consumption, we MUST cancel during ITEM_BEFORE_BLOCK.
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onUseItemOnBlock(UseItemOnBlockEvent e) {

        // Only intercept before the item use executes.
        if (e.getUsePhase() != UseItemOnBlockEvent.UsePhase.ITEM_BEFORE_BLOCK) return;

        if (!(e.getLevel() instanceof ServerLevel level)) return;
        if (!(e.getPlayer() instanceof ServerPlayer sp)) return;
        if (AccessPolicy.isDeveloper(sp)) return;

        ItemStack held = e.getItemStack();
        if (held == null || held.isEmpty()) return;

        // Placement occurs in the adjacent position (usually air),
        // not the clicked block itself.
        BlockPos clickedPos = e.getPos();
        BlockPos placePos = (e.getFace() != null)
                ? clickedPos.relative(e.getFace())
                : clickedPos;

        // --- Bucket handling ---
        if (held.getItem() instanceof BucketItem) {
            RegionRegistryData.Region region = effectiveRegion(level, placePos);
            if (region == null) return;

            boolean placeAllowed = resolveFlag(level, region, FLAG_PLACE, false);
            if (placeAllowed) return;

            if (held.is(Items.WATER_BUCKET)) {
                boolean waterEx = resolveException(level, region, "place", EX_WATER, false);
                if (waterEx) return;
            }

            e.cancelWithResult(InteractionResult.FAIL);
            AccessPolicy.deny(sp, "You cannot use buckets here.");
            return;
        }

        // --- BlockItem handling ---
        if (held.getItem() instanceof BlockItem) {
            RegionRegistryData.Region region = effectiveRegion(level, placePos);
            if (region == null) return;

            boolean placeAllowed = resolveFlag(level, region, FLAG_PLACE, false);
            if (placeAllowed) return;

            if (isTorchItem(held)) {
                boolean torchEx = resolveException(level, region, "place", EX_TORCH, true);
                if (torchEx) return;
            }

            if (isLadderItem(held)) {
                boolean ladderEx = resolveException(level, region, "place", EX_LADDER, false);
                if (ladderEx) return;
            }

            BlockItem bi = (BlockItem) held.getItem();
            BlockState wouldPlace = bi.getBlock().defaultBlockState();
            if (wouldPlace.is(ModTags.Blocks.DUNGEONEER_PLACEABLE)) return;

            e.cancelWithResult(InteractionResult.FAIL);
            AccessPolicy.deny(sp, "You cannot place blocks here.");
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onBlockPlaced(BlockEvent.EntityPlaceEvent e) {
        if (!(e.getLevel() instanceof ServerLevel level)) return;
        if (!(e.getEntity() instanceof ServerPlayer sp)) return;
        if (AccessPolicy.isDeveloper(sp)) return;

        BlockPos pos = e.getPos();
        RegionRegistryData.Region region = effectiveRegion(level, pos);
        if (region == null) return;

        BlockState placed = e.getPlacedBlock();
        if (resolveFlag(level, region, FLAG_PLACE, false)) return;

        if (isTorchLike(placed) && resolveException(level, region, "place", EX_TORCH, true)) return;
        if (isLadderLike(placed) && resolveException(level, region, "place", EX_LADDER, false)) return;
        if (isWaterLike(placed) && resolveException(level, region, "place", EX_WATER, false)) return;
        if (placed.is(ModTags.Blocks.DUNGEONEER_PLACEABLE)) return;

        e.setCanceled(true);
        AccessPolicy.deny(sp, "You cannot place blocks here.");
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onBreakBlock(BlockEvent.BreakEvent e) {
        if (!(e.getLevel() instanceof ServerLevel level)) return;
        if (!(e.getPlayer() instanceof ServerPlayer sp)) return;
        if (AccessPolicy.isDeveloper(sp)) return;

        BlockPos pos = e.getPos();
        RegionRegistryData.Region region = effectiveRegion(level, pos);
        if (region == null) return;

        BlockState state = e.getState();
        Block block = state.getBlock();

        if (AccessPolicy.isBreakProtectedDevice(block) && !AccessPolicy.canBreakProtectedDevices(sp)) {
            e.setCanceled(true);
            AccessPolicy.deny(sp, "You cannot break that device.");
            return;
        }

        if (resolveFlag(level, region, FLAG_BREAK, false)) return;

        if (isTorchLike(state) && resolveException(level, region, "break", EX_TORCH, true)) return;
        if (isLadderLike(state) && resolveException(level, region, "break", EX_LADDER, false)) return;
        if (state.is(ModTags.Blocks.DUNGEONEER_BREAKABLE)) return;

        e.setCanceled(true);
        AccessPolicy.deny(sp, "You cannot break blocks here.");
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock e) {
        if (!(e.getLevel() instanceof ServerLevel level)) return;
        if (!(e.getEntity() instanceof ServerPlayer sp)) return;
        if (AccessPolicy.isDeveloper(sp)) return;

        ItemStack held = e.getItemStack();
        if (isPlacementOrWorldModifyAttempt(held)) return;

        BlockPos pos = e.getPos();
        RegionRegistryData.Region region = effectiveRegion(level, pos);
        if (region == null) return;

        BlockState state = level.getBlockState(pos);
        Block block = state.getBlock();

        if (AccessPolicy.isUseProtectedDevice(block) && !AccessPolicy.canUseProtectedDevices(sp)) {
            e.setCanceled(true);
            e.setCancellationResult(InteractionResult.FAIL);
            AccessPolicy.deny(sp, "You cannot use that device.");
            return;
        }

        if (state.is(ModTags.Blocks.DUNGEONEER_INTERACTABLE)) return;
        if (resolveFlag(level, region, FLAG_INTERACT, false)) return;

        e.setCanceled(true);
        e.setCancellationResult(InteractionResult.FAIL);
        AccessPolicy.deny(sp, "You cannot interact here.");
    }
}
