package net.goui.cosmicdungeon.vendor;

import net.goui.cosmicdungeon.economy.CurrencyService;
import net.goui.cosmicdungeon.menu.VendorMenu;
import net.goui.cosmicdungeon.network.VendorPayloads;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

public final class VendorInteractionEvents {
    @SubscribeEvent
    public void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getHand() != InteractionHand.MAIN_HAND) return;
        if (!(event.getTarget() instanceof Villager villager)) return;
        ResourceLocation profileId = VendorAssignmentService.getProfileId(villager);
        if (profileId == null) return;
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;

        event.setCanceled(true);

        VendorProfile profile = VendorProfileManager.INSTANCE.get(profileId);
        if (profile == null) {
            sp.sendSystemMessage(Component.literal("Vendor shell has unknown profile: " + profileId));
            return;
        }

        VendorMenuState.UnlockResult unlockResult = VendorMenuState.unlockState(sp, profile);
        if (!unlockResult.unlocked()) {
            sp.sendSystemMessage(Component.literal("Vendor locked: " + unlockResult.reason()));
            return;
        }

        VendorPayloads.S2C_OpenVendor open = VendorService.buildOpenPayload(sp, villager, profile);
        sp.connection.send(open);

        sp.openMenu(new VendorProvider(profile.displayName()));
    }

    private record VendorProvider(String title) implements MenuProvider {
        @Override public Component getDisplayName() { return Component.literal(title); }
        @Override public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) { return new VendorMenu(id, inv); }
        @Override public void writeClientSideData(AbstractContainerMenu menu, net.minecraft.network.RegistryFriendlyByteBuf buf) {}
        @Override public boolean shouldTriggerClientSideContainerClosingOnOpen() { return true; }
    }
}
