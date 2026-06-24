package net.goui.cosmicdungeon.playerclass.api;

import net.goui.cosmicdungeon.CosmicDungeonMod;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

@EventBusSubscriber(modid = CosmicDungeonMod.MOD_ID)
public final class PlacedAttunementEvents {
    private PlacedAttunementEvents() {}

    private static final long CACHE_TTL_TICKS = 5L;
    private static final Map<CacheKey, CachedStack> PLACEMENT_CACHE = new HashMap<>();

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock e) {
        if (!(e.getEntity() instanceof ServerPlayer sp)) return;
        if (sp.level().isClientSide()) return;

        ItemStack stack = e.getItemStack();
        if (!(stack.getItem() instanceof BlockItem)) return;
        if (!ClassItemUtil.hasCompleteValidAttunement(stack)) return;

        long now = sp.level().getGameTime();
        prune(now);
        PLACEMENT_CACHE.put(new CacheKey(sp.getUUID(), e.getHand()),
                new CachedStack(stack.copy(), sp.level().dimension().location().toString(), now));
    }

    @SubscribeEvent
    public static void onBlockPlaced(BlockEvent.EntityPlaceEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer sp)) return;
        if (sp.level().isClientSide()) return;

        ItemStack placedStack = consumeCachedStack(sp);
        if (placedStack.isEmpty()) return;
        PlacedAttunementData.get(sp.level()).put(sp.level(), e.getPos(), placedStack);
    }

    @SubscribeEvent
    public static void onBlockDrops(BlockDropsEvent e) {
        Block block = e.getState().getBlock();
        if (block.asItem() == null) return;

        for (ItemEntity drop : e.getDrops()) {
            ItemStack stack = drop.getItem();
            if (!stack.is(block.asItem())) continue;
            if (PlacedAttunementData.get(e.getLevel()).applyAndRemove(e.getLevel(), e.getPos(), stack)) return;
        }

        PlacedAttunementData.get(e.getLevel()).remove(e.getLevel(), e.getPos());
    }

    private static ItemStack consumeCachedStack(ServerPlayer sp) {
        long now = sp.level().getGameTime();
        prune(now);
        for (InteractionHand hand : InteractionHand.values()) {
            CachedStack cached = PLACEMENT_CACHE.remove(new CacheKey(sp.getUUID(), hand));
            if (cached == null) continue;
            if (!cached.dimensionId().equals(sp.level().dimension().location().toString())) continue;
            if (now - cached.gameTime() <= CACHE_TTL_TICKS) return cached.stack();
        }
        return ItemStack.EMPTY;
    }

    private static void prune(long now) {
        Iterator<Map.Entry<CacheKey, CachedStack>> it = PLACEMENT_CACHE.entrySet().iterator();
        while (it.hasNext()) {
            if (now - it.next().getValue().gameTime() > CACHE_TTL_TICKS) it.remove();
        }
    }

    private record CacheKey(UUID playerId, InteractionHand hand) {}
    private record CachedStack(ItemStack stack, String dimensionId, long gameTime) {}
}
