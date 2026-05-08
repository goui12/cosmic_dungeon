package net.minecraft.client.gui.screens.recipebook;

import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.recipebook.PlaceRecipeHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.display.FurnaceRecipeDisplay;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.RecipeDisplayEntry;
import net.minecraft.world.item.crafting.display.RecipeDisplayId;
import net.minecraft.world.item.crafting.display.ShapedCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.ShapelessCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class OverlayRecipeComponent implements Renderable, GuiEventListener {
    private static final ResourceLocation OVERLAY_RECIPE_SPRITE = ResourceLocation.withDefaultNamespace("recipe_book/overlay_recipe");
    private static final int MAX_ROW = 4;
    private static final int MAX_ROW_LARGE = 5;
    private static final float ITEM_RENDER_SCALE = 0.375F;
    public static final int BUTTON_SIZE = 25;
    private final List<OverlayRecipeComponent.OverlayRecipeButton> recipeButtons = Lists.newArrayList();
    private boolean isVisible;
    private int x;
    private int y;
    private RecipeCollection collection = RecipeCollection.EMPTY;
    @Nullable
    private RecipeDisplayId lastRecipeClicked;
    final SlotSelectTime slotSelectTime;
    private final boolean isFurnaceMenu;

    public OverlayRecipeComponent(SlotSelectTime slotSelectTime, boolean isFurnaceMenu) {
        this.slotSelectTime = slotSelectTime;
        this.isFurnaceMenu = isFurnaceMenu;
    }

    public void init(
        RecipeCollection collection, ContextMap contextMap, boolean isFiltering, int x, int y, int overlayX, int overlayY, float width
    ) {
        this.collection = collection;
        List<RecipeDisplayEntry> list = collection.getSelectedRecipes(RecipeCollection.CraftableStatus.CRAFTABLE);
        List<RecipeDisplayEntry> list1 = isFiltering ? Collections.emptyList() : collection.getSelectedRecipes(RecipeCollection.CraftableStatus.NOT_CRAFTABLE);
        int i = list.size();
        int j = i + list1.size();
        int k = j <= 16 ? 4 : 5;
        int l = (int)Math.ceil((float)j / k);
        this.x = x;
        this.y = y;
        float f = this.x + Math.min(j, k) * 25;
        float f1 = overlayX + 50;
        if (f > f1) {
            this.x = (int)(this.x - width * (int)((f - f1) / width));
        }

        float f2 = this.y + l * 25;
        float f3 = overlayY + 50;
        if (f2 > f3) {
            this.y = (int)(this.y - width * Mth.ceil((f2 - f3) / width));
        }

        float f4 = this.y;
        float f5 = overlayY - 100;
        if (f4 < f5) {
            this.y = (int)(this.y - width * Mth.ceil((f4 - f5) / width));
        }

        this.isVisible = true;
        this.recipeButtons.clear();

        for (int i1 = 0; i1 < j; i1++) {
            boolean flag = i1 < i;
            RecipeDisplayEntry recipedisplayentry = flag ? list.get(i1) : list1.get(i1 - i);
            int j1 = this.x + 4 + 25 * (i1 % k);
            int k1 = this.y + 5 + 25 * (i1 / k);
            if (this.isFurnaceMenu) {
                this.recipeButtons
                    .add(new OverlayRecipeComponent.OverlaySmeltingRecipeButton(j1, k1, recipedisplayentry.id(), recipedisplayentry.display(), contextMap, flag));
            } else {
                this.recipeButtons
                    .add(new OverlayRecipeComponent.OverlayCraftingRecipeButton(j1, k1, recipedisplayentry.id(), recipedisplayentry.display(), contextMap, flag));
            }
        }

        this.lastRecipeClicked = null;
    }

    public RecipeCollection getRecipeCollection() {
        return this.collection;
    }

    @Nullable
    public RecipeDisplayId getLastRecipeClicked() {
        return this.lastRecipeClicked;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean isDoubleClick) {
        if (event.button() != 0) {
            return false;
        } else {
            for (OverlayRecipeComponent.OverlayRecipeButton overlayrecipecomponent$overlayrecipebutton : this.recipeButtons) {
                if (overlayrecipecomponent$overlayrecipebutton.mouseClicked(event, isDoubleClick)) {
                    this.lastRecipeClicked = overlayrecipecomponent$overlayrecipebutton.recipe;
                    return true;
                }
            }

            return false;
        }
    }

    /**
     * Checks if the given mouse coordinates are over the GUI element.
     * <p>
     * @return {@code true} if the mouse is over the GUI element, {@code false} otherwise.
     *
     * @param mouseX the X coordinate of the mouse.
     * @param mouseY the Y coordinate of the mouse.
     */
    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return false;
    }

    /**
     * Renders the graphical user interface (GUI) element.
     *
     * @param guiGraphics the GuiGraphics object used for rendering.
     * @param mouseX      the x-coordinate of the mouse cursor.
     * @param mouseY      the y-coordinate of the mouse cursor.
     * @param partialTick the partial tick time.
     */
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (this.isVisible) {
            int i = this.recipeButtons.size() <= 16 ? 4 : 5;
            int j = Math.min(this.recipeButtons.size(), i);
            int k = Mth.ceil((float)this.recipeButtons.size() / i);
            int l = 4;
            guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, OVERLAY_RECIPE_SPRITE, this.x, this.y, j * 25 + 8, k * 25 + 8);

            for (OverlayRecipeComponent.OverlayRecipeButton overlayrecipecomponent$overlayrecipebutton : this.recipeButtons) {
                overlayrecipecomponent$overlayrecipebutton.render(guiGraphics, mouseX, mouseY, partialTick);
            }
        }
    }

    public void setVisible(boolean isVisible) {
        this.isVisible = isVisible;
    }

    public boolean isVisible() {
        return this.isVisible;
    }

    /**
     * Sets the focus state of the GUI element.
     *
     * @param focused {@code true} to apply focus, {@code false} to remove focus
     */
    @Override
    public void setFocused(boolean focused) {
    }

    @Override
    public boolean isFocused() {
        return false;
    }

    @OnlyIn(Dist.CLIENT)
    class OverlayCraftingRecipeButton extends OverlayRecipeComponent.OverlayRecipeButton {
        private static final ResourceLocation ENABLED_SPRITE = ResourceLocation.withDefaultNamespace("recipe_book/crafting_overlay");
        private static final ResourceLocation HIGHLIGHTED_ENABLED_SPRITE = ResourceLocation.withDefaultNamespace("recipe_book/crafting_overlay_highlighted");
        private static final ResourceLocation DISABLED_SPRITE = ResourceLocation.withDefaultNamespace("recipe_book/crafting_overlay_disabled");
        private static final ResourceLocation HIGHLIGHTED_DISABLED_SPRITE = ResourceLocation.withDefaultNamespace(
            "recipe_book/crafting_overlay_disabled_highlighted"
        );
        private static final int GRID_WIDTH = 3;
        private static final int GRID_HEIGHT = 3;

        public OverlayCraftingRecipeButton(
            int x, int y, RecipeDisplayId recipe, RecipeDisplay recipeDisplay, ContextMap contextMap, boolean isCraftable
        ) {
            super(x, y, recipe, isCraftable, calculateIngredientsPositions(recipeDisplay, contextMap));
        }

        private static List<OverlayRecipeComponent.OverlayRecipeButton.Pos> calculateIngredientsPositions(RecipeDisplay recipeDisplay, ContextMap contextMap) {
            List<OverlayRecipeComponent.OverlayRecipeButton.Pos> list = new ArrayList<>();
            switch (recipeDisplay) {
                case ShapedCraftingRecipeDisplay shapedcraftingrecipedisplay:
                    PlaceRecipeHelper.placeRecipe(
                        3,
                        3,
                        shapedcraftingrecipedisplay.width(),
                        shapedcraftingrecipedisplay.height(),
                        shapedcraftingrecipedisplay.ingredients(),
                        (p_380792_, p_380793_, p_380794_, p_380795_) -> {
                            List<ItemStack> list3 = p_380792_.resolveForStacks(contextMap);
                            if (!list3.isEmpty()) {
                                list.add(createGridPos(p_380794_, p_380795_, list3));
                            }
                        }
                    );
                    break;
                case ShapelessCraftingRecipeDisplay shapelesscraftingrecipedisplay:
                    label19: {
                        List<SlotDisplay> list1 = shapelesscraftingrecipedisplay.ingredients();

                        for (int i = 0; i < list1.size(); i++) {
                            List<ItemStack> list2 = list1.get(i).resolveForStacks(contextMap);
                            if (!list2.isEmpty()) {
                                list.add(createGridPos(i % 3, i / 3, list2));
                            }
                        }
                        break label19;
                    }
                default:
            }

            return list;
        }

        @Override
        protected ResourceLocation getSprite(boolean enabled) {
            if (enabled) {
                return this.isHoveredOrFocused() ? HIGHLIGHTED_ENABLED_SPRITE : ENABLED_SPRITE;
            } else {
                return this.isHoveredOrFocused() ? HIGHLIGHTED_DISABLED_SPRITE : DISABLED_SPRITE;
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    abstract class OverlayRecipeButton extends AbstractWidget {
        final RecipeDisplayId recipe;
        private final boolean isCraftable;
        private final List<OverlayRecipeComponent.OverlayRecipeButton.Pos> slots;

        public OverlayRecipeButton(
            int x, int y, RecipeDisplayId recipe, boolean isCraftable, List<OverlayRecipeComponent.OverlayRecipeButton.Pos> slots
        ) {
            super(x, y, 24, 24, CommonComponents.EMPTY);
            this.slots = slots;
            this.recipe = recipe;
            this.isCraftable = isCraftable;
        }

        protected static OverlayRecipeComponent.OverlayRecipeButton.Pos createGridPos(int x, int y, List<ItemStack> possibleItems) {
            return new OverlayRecipeComponent.OverlayRecipeButton.Pos(3 + x * 7, 3 + y * 7, possibleItems);
        }

        protected abstract ResourceLocation getSprite(boolean enabled);

        @Override
        public void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
            this.defaultButtonNarrationText(narrationElementOutput);
        }

        @Override
        public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, this.getSprite(this.isCraftable), this.getX(), this.getY(), this.width, this.height);
            float f = this.getX() + 2;
            float f1 = this.getY() + 2;

            for (OverlayRecipeComponent.OverlayRecipeButton.Pos overlayrecipecomponent$overlayrecipebutton$pos : this.slots) {
                guiGraphics.pose().pushMatrix();
                guiGraphics.pose().translate(f + overlayrecipecomponent$overlayrecipebutton$pos.x, f1 + overlayrecipecomponent$overlayrecipebutton$pos.y);
                guiGraphics.pose().scale(0.375F, 0.375F);
                guiGraphics.pose().translate(-8.0F, -8.0F);
                guiGraphics.renderItem(
                    overlayrecipecomponent$overlayrecipebutton$pos.selectIngredient(OverlayRecipeComponent.this.slotSelectTime.currentIndex()), 0, 0
                );
                guiGraphics.pose().popMatrix();
            }
        }

        @OnlyIn(Dist.CLIENT)
        protected record Pos(int x, int y, List<ItemStack> ingredients) {
            public Pos(int x, int y, List<ItemStack> ingredients) {
                if (ingredients.isEmpty()) {
                    throw new IllegalArgumentException("Ingredient list must be non-empty");
                } else {
                    this.x = x;
                    this.y = y;
                    this.ingredients = ingredients;
                }
            }

            public ItemStack selectIngredient(int index) {
                return this.ingredients.get(index % this.ingredients.size());
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    class OverlaySmeltingRecipeButton extends OverlayRecipeComponent.OverlayRecipeButton {
        private static final ResourceLocation ENABLED_SPRITE = ResourceLocation.withDefaultNamespace("recipe_book/furnace_overlay");
        private static final ResourceLocation HIGHLIGHTED_ENABLED_SPRITE = ResourceLocation.withDefaultNamespace("recipe_book/furnace_overlay_highlighted");
        private static final ResourceLocation DISABLED_SPRITE = ResourceLocation.withDefaultNamespace("recipe_book/furnace_overlay_disabled");
        private static final ResourceLocation HIGHLIGHTED_DISABLED_SPRITE = ResourceLocation.withDefaultNamespace(
            "recipe_book/furnace_overlay_disabled_highlighted"
        );

        public OverlaySmeltingRecipeButton(
            int x, int y, RecipeDisplayId recipe, RecipeDisplay recipeDisplay, ContextMap contextMap, boolean isCraftable
        ) {
            super(x, y, recipe, isCraftable, calculateIngredientsPositions(recipeDisplay, contextMap));
        }

        private static List<OverlayRecipeComponent.OverlayRecipeButton.Pos> calculateIngredientsPositions(RecipeDisplay recipeDisplay, ContextMap contextMap) {
            if (recipeDisplay instanceof FurnaceRecipeDisplay furnacerecipedisplay) {
                List<ItemStack> list = furnacerecipedisplay.ingredient().resolveForStacks(contextMap);
                if (!list.isEmpty()) {
                    return List.of(createGridPos(1, 1, list));
                }
            }

            return List.of();
        }

        @Override
        protected ResourceLocation getSprite(boolean enabled) {
            if (enabled) {
                return this.isHoveredOrFocused() ? HIGHLIGHTED_ENABLED_SPRITE : ENABLED_SPRITE;
            } else {
                return this.isHoveredOrFocused() ? HIGHLIGHTED_DISABLED_SPRITE : DISABLED_SPRITE;
            }
        }
    }
}
