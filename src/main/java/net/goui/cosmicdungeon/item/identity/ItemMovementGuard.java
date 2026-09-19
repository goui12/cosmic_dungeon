package net.goui.cosmicdungeon.item.identity;

import net.goui.cosmicdungeon.CosmicDungeonMod;
import net.goui.cosmicdungeon.auth.AccessPolicy;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid = CosmicDungeonMod.MOD_ID)
public final class ItemMovementGuard {
    private ItemMovementGuard() {}
    private static boolean privateSlot(ServerPlayer player, Slot slot) {
        return slot.container == player.getInventory() || slot.container == player.getEnderChestInventory();
    }
    public static boolean denyClick(ServerPlayer player, int slotId, int button, ClickType type) {
        if (AccessPolicy.isDeveloper(player)) return false;
        var menu = player.containerMenu;
        var carried = menu.getCarried();
        var cursor = ItemMovementRules.flags(carried);
        Slot slot = slotId >= 0 && slotId < menu.slots.size() ? menu.slots.get(slotId) : null;
        var clicked = slot == null ? ItemStack.EMPTY : slot.getItem();
        var clickedFlags = ItemMovementRules.flags(clicked);
        // Do not turn overflow recovery into an expandable personal storage interface.
        // Existing carried/input items can still return; make room and claim before lifting more.
        if (carried.isEmpty() && clickedFlags.noDrop() && ProtectedItemRecovery.pendingHere(player)
                && (type == ClickType.PICKUP || type == ClickType.QUICK_MOVE || type == ClickType.PICKUP_ALL)) {
            ProtectedItemRecovery.notifyPending(player);
            return true;
        }
        boolean ownerSlot = slot != null && privateSlot(player, slot);
        // Rename/enchant are owner-local services. Their existing repair guards remain in force.
        boolean serviceSlot = menu instanceof AnvilMenu || menu instanceof EnchantmentMenu
                || menu instanceof net.goui.cosmicdungeon.playerclass.dragoon.repair.DragoonRepairMenu;
        return switch (type) {
            case THROW -> slot != null && !ItemMovementPolicy.mayDrop(clickedFlags.noDrop());
            case PICKUP -> slotId == -999 ? !ItemMovementPolicy.mayDrop(cursor.noDrop())
                    : slot != null && (!ItemMovementPolicy.mayInsert(cursor.privateStorage(), ownerSlot || serviceSlot)
                    || !ItemMovementPolicy.mayNest(cursor.privateStorage(), ItemMovementRules.portable(clicked))
                    || !ItemMovementPolicy.mayNest(clickedFlags.privateStorage(), ItemMovementRules.portable(carried)));
            case QUICK_MOVE -> slot != null && !ItemMovementPolicy.mayShift(clickedFlags.privateStorage(), ownerSlot,
                    menu == player.inventoryMenu || menu.slots.stream().allMatch(s -> privateSlot(player, s)));
            case SWAP -> {
                var hotbar = button >= 0 && button < player.getInventory().getContainerSize()
                        ? player.getInventory().getItem(button) : ItemStack.EMPTY;
                boolean incoming = ItemMovementRules.flags(hotbar).privateStorage();
                // A large swapped-in stack can make vanilla spill the displaced item.
                yield slot != null && (!ItemMovementPolicy.mayInsert(incoming, ownerSlot || serviceSlot)
                        || (clickedFlags.noDrop() && hotbar.getCount() > slot.getMaxStackSize(hotbar)));
            }
            // Refuse the complete drag gesture; no partial placement or hidden queued slots.
            case QUICK_CRAFT -> cursor.privateStorage();
            case PICKUP_ALL, CLONE -> false;
        };
    }
    public static void reject(ServerPlayer player) {
        player.containerMenu.resumeRemoteUpdates();
        player.containerMenu.sendAllDataToRemote();
        player.displayClientMessage(Component.literal("That item must stay with you."), true);
    }
    @SubscribeEvent
    public static void placePortable(PlayerInteractEvent.RightClickBlock event) {
        if (event.getEntity() instanceof ServerPlayer player && !AccessPolicy.isDeveloper(player)) {
            var stack = player.getItemInHand(event.getHand());
            if (ItemMovementRules.portable(stack) && ItemMovementRules.flags(stack).privateStorage()) {
                event.setCanceled(true); event.setCancellationResult(net.minecraft.world.InteractionResult.FAIL);
            }
        }
    }
    private static boolean displayTransfer(net.minecraft.world.entity.Entity entity) {
        return entity instanceof net.minecraft.world.entity.decoration.ArmorStand
                || entity instanceof net.minecraft.world.entity.decoration.ItemFrame
                || entity instanceof net.minecraft.world.entity.animal.allay.Allay;
    }
    @SubscribeEvent
    public static void entity(PlayerInteractEvent.EntityInteract event) {
        if (event.getEntity() instanceof ServerPlayer player && !AccessPolicy.isDeveloper(player)
                && displayTransfer(event.getTarget())
                && ItemMovementRules.flags(player.getItemInHand(event.getHand())).privateStorage()) {
            event.setCanceled(true); event.setCancellationResult(net.minecraft.world.InteractionResult.FAIL);
        }
    }
    @SubscribeEvent
    public static void specific(PlayerInteractEvent.EntityInteractSpecific event) {
        if (event.getEntity() instanceof ServerPlayer player && !AccessPolicy.isDeveloper(player)
                && displayTransfer(event.getTarget())
                && ItemMovementRules.flags(player.getItemInHand(event.getHand())).privateStorage()) {
            event.setCanceled(true); event.setCancellationResult(net.minecraft.world.InteractionResult.FAIL);
        }
    }
    // Protected cursor/forced-unequip returns now use ProtectedItemRecovery in the player's
    // own save and explicit dungeon inventory scope. Voluntary toss is still refused before removal.
    // TODO(M10/M114, licensed TEST): verify every live container/prediction path and installed
    // capability adapter. Gear Trading 2.0 requires no class-gear transfer or wrong-owner recovery.
    // Keep normal droppable gear's environmental loss and intentional failed-run inventory resets.
}
