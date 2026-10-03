package net.goui.cosmicdungeon.item.identity;

import net.goui.cosmicdungeon.CosmicDungeonMod;
import net.goui.cosmicdungeon.auth.AccessPolicy;
import net.goui.cosmicdungeon.component.ModDataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;

/** Item ownership persists on the stack as well as the dropped entity. */
@EventBusSubscriber(modid = CosmicDungeonMod.MOD_ID)
public final class WorldItemOwnership {
    private WorldItemOwnership() {}
    private static void protect(ServerPlayer player, ItemEntity entity) {
        var stack = entity.getItem();
        ClassItemOwnership.bind(player, stack);
        if (ClassItemOwnership.present(stack)) {
            var owner = ClassItemOwnership.owner(stack);
            if (owner != null) entity.setTarget(owner);
            return; // Invalid owner data remains intact and cannot be collected.
        }
        if (AccessPolicy.isDeveloper(player)) return;
        if (!ItemProvenanceService.requiresProvenance(stack) && !ItemProvenanceService.present(stack)
                && !ItemMovementRules.flags(stack).privateStorage()) return;
        var chopOwner = stack.get(ModDataComponents.CHOP_OWNER.get());
        if (chopOwner != null) entity.setTarget(chopOwner);
        else if (entity.getTarget() == null) entity.setTarget(player.getUUID());
    }
    public static void centerDeathDrop(ServerPlayer player, ItemEntity item) {
        item.setPos(Vec3.atBottomCenterOf(player.blockPosition()).add(0, 0.1, 0));
        item.setDeltaMovement(Vec3.ZERO);
    }
    @SubscribeEvent public static void toss(ItemTossEvent event) {
        if (event.getPlayer() instanceof ServerPlayer player) protect(player, event.getEntity());
    }
    @SubscribeEvent(priority = EventPriority.LOWEST) public static void death(LivingDropsEvent event) {
        if (event.getEntity() instanceof ServerPlayer player)
            for (var item : event.getDrops()) {
                protect(player, item);
                centerDeathDrop(player, item);
            }
    }
}
