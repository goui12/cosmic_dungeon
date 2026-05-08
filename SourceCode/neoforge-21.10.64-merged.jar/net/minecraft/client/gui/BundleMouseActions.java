package net.minecraft.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.ScrollWheelHandler;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ServerboundSelectBundleItemPacket;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BundleItem;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Vector2i;

@OnlyIn(Dist.CLIENT)
public class BundleMouseActions implements ItemSlotMouseAction {
    private final Minecraft minecraft;
    private final ScrollWheelHandler scrollWheelHandler;

    public BundleMouseActions(Minecraft minecraft) {
        this.minecraft = minecraft;
        this.scrollWheelHandler = new ScrollWheelHandler();
    }

    @Override
    public boolean matches(Slot slot) {
        return slot.getItem().is(ItemTags.BUNDLES);
    }

    @Override
    public boolean onMouseScrolled(double xOffset, double yOffset, int hoveredSlotIndex, ItemStack hoveredSlotItem) {
        int i = BundleItem.getNumberOfItemsToShow(hoveredSlotItem);
        if (i == 0) {
            return false;
        } else {
            Vector2i vector2i = this.scrollWheelHandler.onMouseScroll(xOffset, yOffset);
            int j = vector2i.y == 0 ? -vector2i.x : vector2i.y;
            if (j != 0) {
                int k = BundleItem.getSelectedItem(hoveredSlotItem);
                int l = ScrollWheelHandler.getNextScrollWheelSelection(j, k, i);
                if (k != l) {
                    this.toggleSelectedBundleItem(hoveredSlotItem, hoveredSlotIndex, l);
                }
            }

            return true;
        }
    }

    @Override
    public void onStopHovering(Slot slot) {
        this.unselectedBundleItem(slot.getItem(), slot.index);
    }

    @Override
    public void onSlotClicked(Slot slot, ClickType clickType) {
        if (clickType == ClickType.QUICK_MOVE || clickType == ClickType.SWAP) {
            this.unselectedBundleItem(slot.getItem(), slot.index);
        }
    }

    private void toggleSelectedBundleItem(ItemStack stack, int index, int nextIndex) {
        if (this.minecraft.getConnection() != null && nextIndex < BundleItem.getNumberOfItemsToShow(stack)) {
            ClientPacketListener clientpacketlistener = this.minecraft.getConnection();
            BundleItem.toggleSelectedItem(stack, nextIndex);
            clientpacketlistener.send(new ServerboundSelectBundleItemPacket(index, nextIndex));
        }
    }

    public void unselectedBundleItem(ItemStack bundle, int slotIndex) {
        this.toggleSelectedBundleItem(bundle, slotIndex, -1);
    }
}
