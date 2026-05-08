package net.minecraft.client.gui.components.toasts;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplayContext;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class RecipeToast implements Toast {
    private static final ResourceLocation BACKGROUND_SPRITE = ResourceLocation.withDefaultNamespace("toast/recipe");
    private static final long DISPLAY_TIME = 5000L;
    private static final Component TITLE_TEXT = Component.translatable("recipe.toast.title");
    private static final Component DESCRIPTION_TEXT = Component.translatable("recipe.toast.description");
    private final List<RecipeToast.Entry> recipeItems = new ArrayList<>();
    private long lastChanged;
    private boolean changed;
    private Toast.Visibility wantedVisibility = Toast.Visibility.HIDE;
    private int displayedRecipeIndex;

    private RecipeToast() {
    }

    @Override
    public Toast.Visibility getWantedVisibility() {
        return this.wantedVisibility;
    }

    @Override
    public void update(ToastManager toastManager, long visibilityTime) {
        if (this.changed) {
            this.lastChanged = visibilityTime;
            this.changed = false;
        }

        if (this.recipeItems.isEmpty()) {
            this.wantedVisibility = Toast.Visibility.HIDE;
        } else {
            this.wantedVisibility = visibilityTime - this.lastChanged >= 5000.0 * toastManager.getNotificationDisplayTimeMultiplier()
                ? Toast.Visibility.HIDE
                : Toast.Visibility.SHOW;
        }

        this.displayedRecipeIndex = (int)(
            visibilityTime / Math.max(1.0, 5000.0 * toastManager.getNotificationDisplayTimeMultiplier() / this.recipeItems.size()) % this.recipeItems.size()
        );
    }

    @Override
    public void render(GuiGraphics guiGraphics, Font font, long visibilityTime) {
        guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, BACKGROUND_SPRITE, 0, 0, this.width(), this.height());
        guiGraphics.drawString(font, TITLE_TEXT, 30, 7, -11534256, false);
        guiGraphics.drawString(font, DESCRIPTION_TEXT, 30, 18, -16777216, false);
        RecipeToast.Entry recipetoast$entry = this.recipeItems.get(this.displayedRecipeIndex);
        guiGraphics.pose().pushMatrix();
        guiGraphics.pose().scale(0.6F, 0.6F);
        guiGraphics.renderFakeItem(recipetoast$entry.categoryItem(), 3, 3);
        guiGraphics.pose().popMatrix();
        guiGraphics.renderFakeItem(recipetoast$entry.unlockedItem(), 8, 8);
    }

    private void addItem(ItemStack categoryItem, ItemStack unlockedItem) {
        this.recipeItems.add(new RecipeToast.Entry(categoryItem, unlockedItem));
        this.changed = true;
    }

    public static void addOrUpdate(ToastManager toastManager, RecipeDisplay recipeDisplay) {
        RecipeToast recipetoast = toastManager.getToast(RecipeToast.class, NO_TOKEN);
        if (recipetoast == null) {
            recipetoast = new RecipeToast();
            toastManager.addToast(recipetoast);
        }

        ContextMap contextmap = SlotDisplayContext.fromLevel(toastManager.getMinecraft().level);
        ItemStack itemstack = recipeDisplay.craftingStation().resolveForFirstStack(contextmap);
        ItemStack itemstack1 = recipeDisplay.result().resolveForFirstStack(contextmap);
        recipetoast.addItem(itemstack, itemstack1);
    }

    @OnlyIn(Dist.CLIENT)
    record Entry(ItemStack categoryItem, ItemStack unlockedItem) {
    }
}
