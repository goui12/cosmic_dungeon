package net.goui.cosmicdungeon.achievement.plantflags;

import net.goui.cosmicdungeon.CosmicDungeonMod;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BannerItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.AbstractBannerBlock;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

@EventBusSubscriber(modid = CosmicDungeonMod.MOD_ID)
public final class PlantFlagEvents {
    private PlantFlagEvents() {}

    private static final long CACHE_TTL_TICKS = 5L;
    private static final Map<CacheKey, CachedAttunement> PLACEMENT_CACHE = new HashMap<>();

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock e) {
        if (!(e.getEntity() instanceof ServerPlayer sp)) return;
        if (sp.level().isClientSide()) return;
        ItemStack stack = e.getItemStack();
        if (!(stack.getItem() instanceof BannerItem)) return;
        PlantedBannerAttunement attunement = PlantedBannerAttunement.from(stack);
        if (attunement == null) return;
        prune(sp.level().getGameTime());
        PLACEMENT_CACHE.put(new CacheKey(sp.getUUID(), e.getHand()), new CachedAttunement(attunement, sp.level().dimension().location().toString(), sp.level().getGameTime()));
    }

    @SubscribeEvent
    public static void onBannerPlaced(BlockEvent.EntityPlaceEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer sp)) return;
        if (sp.level().isClientSide()) return;
        if (!(e.getPlacedBlock().getBlock() instanceof AbstractBannerBlock)) return;
        PlantedBannerAttunement attunement = consumeCachedAttunement(sp);
        if (attunement == null) return;
        PlantFlagService.recordFlagPlanted(sp, e.getPos(), attunement);
    }

    private static PlantedBannerAttunement consumeCachedAttunement(ServerPlayer sp) {
        long now = sp.level().getGameTime();
        prune(now);
        for (InteractionHand hand : InteractionHand.values()) {
            CachedAttunement cached = PLACEMENT_CACHE.remove(new CacheKey(sp.getUUID(), hand));
            if (cached == null) continue;
            if (!cached.dimensionId().equals(sp.level().dimension().location().toString())) continue;
            if (now - cached.gameTime() <= CACHE_TTL_TICKS) return cached.attunement();
        }
        return null;
    }

    private static void prune(long now) {
        Iterator<Map.Entry<CacheKey, CachedAttunement>> it = PLACEMENT_CACHE.entrySet().iterator();
        while (it.hasNext()) {
            if (now - it.next().getValue().gameTime() > CACHE_TTL_TICKS) it.remove();
        }
    }

    private record CacheKey(UUID playerId, InteractionHand hand) {}
    private record CachedAttunement(PlantedBannerAttunement attunement, String dimensionId, long gameTime) {}
}
