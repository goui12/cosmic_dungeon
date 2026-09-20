package net.goui.cosmicdungeon.menu;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuConstructor;

/** Sends the opening nonce before any screen state; container IDs alone wrap/recur. */
public record SessionMenuProvider(MenuConstructor constructor, Component title) implements MenuProvider {
    @Override public Component getDisplayName() { return title; }
    @Override public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return constructor.createMenu(id, inv, player);
    }
    @Override public void writeClientSideData(AbstractContainerMenu menu, RegistryFriendlyByteBuf buf) {
        buf.writeUUID(((SessionMenu) menu).sessionId());
    }
}
