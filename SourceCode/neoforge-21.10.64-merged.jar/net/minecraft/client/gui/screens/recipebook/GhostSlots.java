package net.minecraft.client.gui.screens.recipebook;

import it.unimi.dsi.fastutil.objects.Reference2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class GhostSlots {
    private final Reference2ObjectMap<Slot, GhostSlots.GhostSlot> ingredients = new Reference2ObjectArrayMap<>();
    private final SlotSelectTime slotSelectTime;

    public GhostSlots(SlotSelectTime slotSelectTime) {
        this.slotSelectTime = slotSelectTime;
    }

    public void clear() {
        this.ingredients.clear();
    }

    private void setSlot(Slot slot, ContextMap contextMap, SlotDisplay slotDisplay, boolean isResultSlot) {
        List<ItemStack> list = slotDisplay.resolveForStacks(contextMap);
        if (!list.isEmpty()) {
            this.ingredients.put(slot, new GhostSlots.GhostSlot(list, isResultSlot));
        }
    }

    public void setInput(Slot slot, ContextMap contextMap, SlotDisplay slotDisplay) {
        this.setSlot(slot, contextMap, slotDisplay, false);
    }

    public void setResult(Slot slot, ContextMap contextMap, SlotDisplay slotDisplay) {
        this.setSlot(slot, contextMap, slotDisplay, true);
    }

    public void render(GuiGraphics guiGraphics, Minecraft minecraft, boolean isBiggerResultSlot) {
        this.ingredients.forEach((p_421286_, p_421287_) -> {
            int i = p_421286_.x;
            int j = p_421286_.y;
            if (p_421287_.isResultSlot && isBiggerResultSlot) {
                guiGraphics.fill(i - 4, j - 4, i + 20, j + 20, 822018048);
            } else {
                guiGraphics.fill(i, j, i + 16, j + 16, 822018048);
            }

            ItemStack itemstack = p_421287_.getItem(this.slotSelectTime.currentIndex());
            guiGraphics.renderFakeItem(itemstack, i, j);
            guiGraphics.fill(i, j, i + 16, j + 16, 822083583);
            if (p_421287_.isResultSlot) {
                guiGraphics.renderItemDecorations(minecraft.font, itemstack, i, j);
            }
        });
    }

    public void renderTooltip(GuiGraphics guiGraphics, Minecraft minecraft, int mouseX, int mouseY, @Nullable Slot slot) {
        if (slot != null) {
            GhostSlots.GhostSlot ghostslots$ghostslot = this.ingredients.get(slot);
            if (ghostslots$ghostslot != null) {
                ItemStack itemstack = ghostslots$ghostslot.getItem(this.slotSelectTime.currentIndex());
                guiGraphics.setComponentTooltipForNextFrame(
                    minecraft.font, Screen.getTooltipFromItem(minecraft, itemstack), mouseX, mouseY, itemstack, itemstack.get(DataComponents.TOOLTIP_STYLE)
                );
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    record GhostSlot(List<ItemStack> items, boolean isResultSlot) {
        public ItemStack getItem(int index) {
            int i = this.items.size();
            return i == 0 ? ItemStack.EMPTY : this.items.get(index % i);
        }
    }
}
