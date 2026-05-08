package net.minecraft.client.gui.screens.recipebook;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import java.util.List;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.client.ClientRecipeBook;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.StateSwitchingButton;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.display.RecipeDisplayId;
import net.minecraft.world.item.crafting.display.SlotDisplayContext;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class RecipeBookPage {
    public static final int ITEMS_PER_PAGE = 20;
    private static final WidgetSprites PAGE_FORWARD_SPRITES = new WidgetSprites(
        ResourceLocation.withDefaultNamespace("recipe_book/page_forward"), ResourceLocation.withDefaultNamespace("recipe_book/page_forward_highlighted")
    );
    private static final WidgetSprites PAGE_BACKWARD_SPRITES = new WidgetSprites(
        ResourceLocation.withDefaultNamespace("recipe_book/page_backward"), ResourceLocation.withDefaultNamespace("recipe_book/page_backward_highlighted")
    );
    private final List<RecipeButton> buttons = Lists.newArrayListWithCapacity(20);
    @Nullable
    private RecipeButton hoveredButton;
    private final OverlayRecipeComponent overlay;
    private Minecraft minecraft;
    private final RecipeBookComponent<?> parent;
    private List<RecipeCollection> recipeCollections = ImmutableList.of();
    private StateSwitchingButton forwardButton;
    private StateSwitchingButton backButton;
    private int totalPages;
    private int currentPage;
    private ClientRecipeBook recipeBook;
    @Nullable
    private RecipeDisplayId lastClickedRecipe;
    @Nullable
    private RecipeCollection lastClickedRecipeCollection;
    private boolean isFiltering;

    public RecipeBookPage(RecipeBookComponent<?> parent, SlotSelectTime slotSelectTime, boolean isFurnaceMenu) {
        this.parent = parent;
        this.overlay = new OverlayRecipeComponent(slotSelectTime, isFurnaceMenu);

        for (int i = 0; i < 20; i++) {
            this.buttons.add(new RecipeButton(slotSelectTime));
        }
    }

    public void init(Minecraft minecraft, int x, int y) {
        this.minecraft = minecraft;
        this.recipeBook = minecraft.player.getRecipeBook();

        for (int i = 0; i < this.buttons.size(); i++) {
            this.buttons.get(i).setPosition(x + 11 + 25 * (i % 5), y + 31 + 25 * (i / 5));
        }

        this.forwardButton = new StateSwitchingButton(x + 93, y + 137, 12, 17, false);
        this.forwardButton.initTextureValues(PAGE_FORWARD_SPRITES);
        this.backButton = new StateSwitchingButton(x + 38, y + 137, 12, 17, true);
        this.backButton.initTextureValues(PAGE_BACKWARD_SPRITES);
    }

    public void updateCollections(List<RecipeCollection> recipeCollections, boolean resetPageNumber, boolean isFiltering) {
        this.recipeCollections = recipeCollections;
        this.isFiltering = isFiltering;
        this.totalPages = (int)Math.ceil(recipeCollections.size() / 20.0);
        if (this.totalPages <= this.currentPage || resetPageNumber) {
            this.currentPage = 0;
        }

        this.updateButtonsForPage();
    }

    private void updateButtonsForPage() {
        int i = 20 * this.currentPage;
        ContextMap contextmap = SlotDisplayContext.fromLevel(this.minecraft.level);

        for (int j = 0; j < this.buttons.size(); j++) {
            RecipeButton recipebutton = this.buttons.get(j);
            if (i + j < this.recipeCollections.size()) {
                RecipeCollection recipecollection = this.recipeCollections.get(i + j);
                recipebutton.init(recipecollection, this.isFiltering, this, contextmap);
                recipebutton.visible = true;
            } else {
                recipebutton.visible = false;
            }
        }

        this.updateArrowButtons();
    }

    private void updateArrowButtons() {
        this.forwardButton.visible = this.totalPages > 1 && this.currentPage < this.totalPages - 1;
        this.backButton.visible = this.totalPages > 1 && this.currentPage > 0;
    }

    public void render(GuiGraphics guiGraphics, int x, int y, int mouseX, int mouseY, float partialTick) {
        if (this.totalPages > 1) {
            Component component = Component.translatable("gui.recipebook.page", this.currentPage + 1, this.totalPages);
            int i = this.minecraft.font.width(component);
            guiGraphics.drawString(this.minecraft.font, component, x - i / 2 + 73, y + 141, -1);
        }

        this.hoveredButton = null;

        for (RecipeButton recipebutton : this.buttons) {
            recipebutton.render(guiGraphics, mouseX, mouseY, partialTick);
            if (recipebutton.visible && recipebutton.isHoveredOrFocused()) {
                this.hoveredButton = recipebutton;
            }
        }

        this.backButton.render(guiGraphics, mouseX, mouseY, partialTick);
        this.forwardButton.render(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.nextStratum();
        this.overlay.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    public void renderTooltip(GuiGraphics guiGraphics, int x, int y) {
        if (this.minecraft.screen != null && this.hoveredButton != null && !this.overlay.isVisible()) {
            ItemStack itemstack = this.hoveredButton.getDisplayStack();
            ResourceLocation resourcelocation = itemstack.get(DataComponents.TOOLTIP_STYLE);
            guiGraphics.setComponentTooltipForNextFrame(this.minecraft.font, this.hoveredButton.getTooltipText(itemstack), x, y, resourcelocation);
        }
    }

    @Nullable
    public RecipeDisplayId getLastClickedRecipe() {
        return this.lastClickedRecipe;
    }

    @Nullable
    public RecipeCollection getLastClickedRecipeCollection() {
        return this.lastClickedRecipeCollection;
    }

    public void setInvisible() {
        this.overlay.setVisible(false);
    }

    public boolean mouseClicked(MouseButtonEvent event, int x, int y, int width, int height, boolean isDoubleClick) {
        this.lastClickedRecipe = null;
        this.lastClickedRecipeCollection = null;
        if (this.overlay.isVisible()) {
            if (this.overlay.mouseClicked(event, isDoubleClick)) {
                this.lastClickedRecipe = this.overlay.getLastRecipeClicked();
                this.lastClickedRecipeCollection = this.overlay.getRecipeCollection();
            } else {
                this.overlay.setVisible(false);
            }

            return true;
        } else if (this.forwardButton.mouseClicked(event, isDoubleClick)) {
            this.currentPage++;
            this.updateButtonsForPage();
            return true;
        } else if (this.backButton.mouseClicked(event, isDoubleClick)) {
            this.currentPage--;
            this.updateButtonsForPage();
            return true;
        } else {
            ContextMap contextmap = SlotDisplayContext.fromLevel(this.minecraft.level);

            for (RecipeButton recipebutton : this.buttons) {
                if (recipebutton.mouseClicked(event, isDoubleClick)) {
                    if (event.button() == 0) {
                        this.lastClickedRecipe = recipebutton.getCurrentRecipe();
                        this.lastClickedRecipeCollection = recipebutton.getCollection();
                    } else if (event.button() == 1 && !this.overlay.isVisible() && !recipebutton.isOnlyOption()) {
                        this.overlay
                            .init(
                                recipebutton.getCollection(),
                                contextmap,
                                this.isFiltering,
                                recipebutton.getX(),
                                recipebutton.getY(),
                                x + width / 2,
                                y + 13 + height / 2,
                                recipebutton.getWidth()
                            );
                    }

                    return true;
                }
            }

            return false;
        }
    }

    public void recipeShown(RecipeDisplayId recipe) {
        this.parent.recipeShown(recipe);
    }

    public ClientRecipeBook getRecipeBook() {
        return this.recipeBook;
    }

    protected void listButtons(Consumer<AbstractWidget> consumer) {
        consumer.accept(this.forwardButton);
        consumer.accept(this.backButton);
        this.buttons.forEach(consumer);
    }
}
