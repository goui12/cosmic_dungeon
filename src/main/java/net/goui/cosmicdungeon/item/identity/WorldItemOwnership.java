package net.goui.cosmicdungeon.item.identity;

import net.goui.cosmicdungeon.CosmicDungeonMod;
import net.goui.cosmicdungeon.auth.AccessPolicy;
import net.goui.cosmicdungeon.component.ModDataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;

/** World dropping cannot bypass protected equipment trade (Gear Trading 2.0).
 * Vanilla ItemEntity.Owner persists and prevents merging between different owners.
 * Ordinary environmental destruction/despawn still applies; no extra immunity is implied. */
@EventBusSubscriber(modid = CosmicDungeonMod.MOD_ID)
public final class WorldItemOwnership {
    private WorldItemOwnership() {}
    private static void protect(ServerPlayer player, ItemEntity entity) {
        if (AccessPolicy.isDeveloper(player)) return;
        var stack = entity.getItem();
        if (!ItemProvenanceService.requiresProvenance(stack) && !ItemProvenanceService.present(stack)
                && !ItemMovementRules.flags(stack).privateStorage()) return;
        // Preserve explicit Chop ownership and any previously authored world owner.
        var chopOwner = stack.get(ModDataComponents.CHOP_OWNER.get());
        if (chopOwner != null) entity.setTarget(chopOwner);
        else if (entity.getTarget() == null) entity.setTarget(player.getUUID());
    }
    @SubscribeEvent public static void toss(ItemTossEvent event) {
        if (event.getPlayer() instanceof ServerPlayer player) protect(player, event.getEntity());
    }
    @SubscribeEvent public static void death(LivingDropsEvent event) {
        if (event.getEntity() instanceof ServerPlayer player)
            for (var item : event.getDrops()) protect(player, item);
    }
}
