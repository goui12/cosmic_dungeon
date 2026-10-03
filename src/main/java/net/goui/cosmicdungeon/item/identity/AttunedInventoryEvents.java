package net.goui.cosmicdungeon.item.identity;

import net.goui.cosmicdungeon.CosmicDungeonMod;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/** One login pass for existing carried equipment; no periodic inventory or world scan. */
@EventBusSubscriber(modid = CosmicDungeonMod.MOD_ID)
public final class AttunedInventoryEvents {
    private AttunedInventoryEvents() {}
    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            var inventory = player.getInventory();
            boolean changed = false;
            for (int slot = 0; slot < inventory.getContainerSize(); slot++)
                changed |= ClassItemOwnership.bind(player, inventory.getItem(slot));
            if (changed) inventory.setChanged();
        }
    }
    @SubscribeEvent public static void equipped(LivingEquipmentChangeEvent event) {
        if (event.getEntity() instanceof ServerPlayer player)
            ClassItemOwnership.bind(player, event.getTo());
    }
}
