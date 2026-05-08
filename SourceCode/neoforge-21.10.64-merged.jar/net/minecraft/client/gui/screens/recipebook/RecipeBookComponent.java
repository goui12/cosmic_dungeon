package net.minecraft.client.gui.screens.recipebook;

import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.client.ClientRecipeBook;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.StateSwitchingButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.navigation.ScreenAxis;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.language.LanguageInfo;
import net.minecraft.client.resources.language.LanguageManager;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundRecipeBookChangeSettingsPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.entity.player.StackedItemContents;
import net.minecraft.world.inventory.AbstractFurnaceMenu;
import net.minecraft.world.inventory.RecipeBookMenu;
import net.minecraft.world.inventory.RecipeBookType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.ExtendedRecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.RecipeDisplayId;
import net.minecraft.world.item.crafting.display.SlotDisplayContext;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public abstract class RecipeBookComponent<T extends RecipeBookMenu> implements Renderable, GuiEventListener, NarratableEntry {
    public static final WidgetSprites RECIPE_BUTTON_SPRITES = new WidgetSprites(
        ResourceLocation.withDefaultNamespace("recipe_book/button"), ResourceLocation.withDefaultNamespace("recipe_book/button_highlighted")
    );
    protected static final ResourceLocation RECIPE_BOOK_LOCATION = ResourceLocation.withDefaultNamespace("textures/gui/recipe_book.png");
    private static final int BACKGROUND_TEXTURE_WIDTH = 256;
    private static final int BACKGROUND_TEXTURE_HEIGHT = 256;
    private static final Component SEARCH_HINT = Component.translatable("gui.recipebook.search_hint").withStyle(EditBox.SEARCH_HINT_STYLE);
    public static final int IMAGE_WIDTH = 147;
    public static final int IMAGE_HEIGHT = 166;
    private static final int OFFSET_X_POSITION = 86;
    private static final int BORDER_WIDTH = 8;
    private static final Component ALL_RECIPES_TOOLTIP = Component.translatable("gui.recipebook.toggleRecipes.all");
    private static final int TICKS_TO_SWAP_SLOT = 30;
    private int xOffset;
    private int width;
    private int height;
    private float time;
    @Nullable
    private RecipeDisplayId lastPlacedRecipe;
    private final GhostSlots ghostSlots;
    private final List<RecipeBookTabButton> tabButtons = Lists.newArrayList();
    @Nullable
    private RecipeBookTabButton selectedTab;
    protected StateSwitchingButton filterButton;
    protected final T menu;
    protected Minecraft minecraft;
    @Nullable
    private EditBox searchBox;
    private String lastSearch = "";
    private final List<RecipeBookComponent.TabInfo> tabInfos;
    private ClientRecipeBook book;
    private final RecipeBookPage recipeBookPage;
    @Nullable
    private RecipeDisplayId lastRecipe;
    @Nullable
    private RecipeCollection lastRecipeCollection;
    private final StackedItemContents stackedContents = new StackedItemContents();
    private int timesInventoryChanged;
    private boolean ignoreTextInput;
    private boolean visible;
    private boolean widthTooNarrow;
    @Nullable
    private ScreenRectangle magnifierIconPlacement;

    public RecipeBookComponent(T menu, List<RecipeBookComponent.TabInfo> tabInfos) {
        this.menu = menu;
        this.tabInfos = tabInfos;
        SlotSelectTime slotselecttime = () -> Mth.floor(this.time / 30.0F);
        this.ghostSlots = new GhostSlots(slotselecttime);
        this.recipeBookPage = new RecipeBookPage(this, slotselecttime, menu instanceof AbstractFurnaceMenu);
    }

    public void init(int width, int height, Minecraft minecraft, boolean widthTooNarrow) {
        this.minecraft = minecraft;
        this.width = width;
        this.height = height;
        this.widthTooNarrow = widthTooNarrow;
        this.book = minecraft.player.getRecipeBook();
        this.timesInventoryChanged = minecraft.player.getInventory().getTimesChanged();
        this.visible = this.isVisibleAccordingToBookData();
        if (this.visible) {
            this.initVisuals();
        }
    }

    private void initVisuals() {
        boolean flag = this.isFiltering();
        this.xOffset = this.widthTooNarrow ? 0 : 86;
        int i = this.getXOrigin();
        int j = this.getYOrigin();
        this.stackedContents.clear();
        this.minecraft.player.getInventory().fillStackedContents(this.stackedContents);
        this.menu.fillCraftSlotsStackedContents(this.stackedContents);
        String s = this.searchBox != null ? this.searchBox.getValue() : "";
        this.searchBox = new EditBox(this.minecraft.font, i + 25, j + 13, 81, 9 + 5, Component.translatable("itemGroup.search"));
        this.searchBox.setMaxLength(50);
        this.searchBox.setVisible(true);
        this.searchBox.setTextColor(-1);
        this.searchBox.setValue(s);
        this.searchBox.setHint(SEARCH_HINT);
        this.magnifierIconPlacement = ScreenRectangle.of(
            ScreenAxis.HORIZONTAL, i + 8, this.searchBox.getY(), this.searchBox.getX() - this.getXOrigin(), this.searchBox.getHeight()
        );
        this.recipeBookPage.init(this.minecraft, i, j);
        this.filterButton = new StateSwitchingButton(i + 110, j + 12, 26, 16, flag);
        this.updateFilterButtonTooltip();
        this.initFilterButtonTextures();
        this.tabButtons.clear();

        for (RecipeBookComponent.TabInfo recipebookcomponent$tabinfo : this.tabInfos) {
            this.tabButtons.add(new RecipeBookTabButton(recipebookcomponent$tabinfo));
        }

        if (this.selectedTab != null) {
            this.selectedTab = this.tabButtons
                .stream()
                .filter(p_380796_ -> p_380796_.getCategory().equals(this.selectedTab.getCategory()))
                .findFirst()
                .orElse(null);
        }

        if (this.selectedTab == null) {
            this.selectedTab = this.tabButtons.get(0);
        }

        this.selectedTab.setStateTriggered(true);
        this.selectMatchingRecipes();
        this.updateTabs(flag);
        this.updateCollections(false, flag);
    }

    private int getYOrigin() {
        return (this.height - 166) / 2;
    }

    private int getXOrigin() {
        return (this.width - 147) / 2 - this.xOffset;
    }

    private void updateFilterButtonTooltip() {
        this.filterButton.setTooltip(this.filterButton.isStateTriggered() ? Tooltip.create(this.getRecipeFilterName()) : Tooltip.create(ALL_RECIPES_TOOLTIP));
    }

    protected abstract void initFilterButtonTextures();

    public int updateScreenPosition(int width, int imageWidth) {
        int i;
        if (this.isVisible() && !this.widthTooNarrow) {
            i = 177 + (width - imageWidth - 200) / 2;
        } else {
            i = (width - imageWidth) / 2;
        }

        return i;
    }

    public void toggleVisibility() {
        this.setVisible(!this.isVisible());
    }

    public boolean isVisible() {
        return this.visible;
    }

    private boolean isVisibleAccordingToBookData() {
        return this.book.isOpen(this.menu.getRecipeBookType());
    }

    protected void setVisible(boolean visible) {
        if (visible) {
            this.initVisuals();
        }

        this.visible = visible;
        this.book.setOpen(this.menu.getRecipeBookType(), visible);
        if (!visible) {
            this.recipeBookPage.setInvisible();
        }

        this.sendUpdateSettings();
    }

    protected abstract boolean isCraftingSlot(Slot slot);

    public void slotClicked(@Nullable Slot slot) {
        if (slot != null && this.isCraftingSlot(slot)) {
            this.lastPlacedRecipe = null;
            this.ghostSlots.clear();
            if (this.isVisible()) {
                this.updateStackedContents();
            }
        }
    }

    private void selectMatchingRecipes() {
        for (RecipeBookComponent.TabInfo recipebookcomponent$tabinfo : this.tabInfos) {
            for (RecipeCollection recipecollection : this.book.getCollection(recipebookcomponent$tabinfo.category())) {
                this.selectMatchingRecipes(recipecollection, this.stackedContents);
            }
        }
    }

    protected abstract void selectMatchingRecipes(RecipeCollection possibleRecipes, StackedItemContents stackedItemContents);

    private void updateCollections(boolean resetPageNumber, boolean isFiltering) {
        List<RecipeCollection> list = this.book.getCollection(this.selectedTab.getCategory());
        List<RecipeCollection> list1 = Lists.newArrayList(list);
        list1.removeIf(p_378795_ -> !p_378795_.hasAnySelected());
        String s = this.searchBox.getValue();
        if (!s.isEmpty()) {
            ClientPacketListener clientpacketlistener = this.minecraft.getConnection();
            if (clientpacketlistener != null) {
                ObjectSet<RecipeCollection> objectset = new ObjectLinkedOpenHashSet<>(
                    clientpacketlistener.searchTrees().recipes().search(s.toLowerCase(Locale.ROOT))
                );
                list1.removeIf(p_302148_ -> !objectset.contains(p_302148_));
            }
        }

        if (isFiltering) {
            list1.removeIf(p_100331_ -> !p_100331_.hasCraftable());
        }

        this.recipeBookPage.updateCollections(list1, resetPageNumber, isFiltering);
    }

    private void updateTabs(boolean isFiltering) {
        int i = (this.width - 147) / 2 - this.xOffset - 30;
        int j = (this.height - 166) / 2 + 3;
        int k = 27;
        int l = 0;

        for (RecipeBookTabButton recipebooktabbutton : this.tabButtons) {
            ExtendedRecipeBookCategory extendedrecipebookcategory = recipebooktabbutton.getCategory();
            // Neo: Add support for modded search categories.
            if (extendedrecipebookcategory instanceof SearchRecipeBookCategory || net.neoforged.neoforge.client.RecipeBookManager.getSearchCategories().containsKey(extendedrecipebookcategory)) {
                recipebooktabbutton.visible = true;
                recipebooktabbutton.setPosition(i, j + 27 * l++);
            } else if (recipebooktabbutton.updateVisibility(this.book)) {
                recipebooktabbutton.setPosition(i, j + 27 * l++);
                recipebooktabbutton.startAnimation(this.book, isFiltering);
            }
        }
    }

    public void tick() {
        boolean flag = this.isVisibleAccordingToBookData();
        if (this.isVisible() != flag) {
            this.setVisible(flag);
        }

        if (this.isVisible()) {
            if (this.timesInventoryChanged != this.minecraft.player.getInventory().getTimesChanged()) {
                this.updateStackedContents();
                this.timesInventoryChanged = this.minecraft.player.getInventory().getTimesChanged();
            }
        }
    }

    private void updateStackedContents() {
        this.stackedContents.clear();
        this.minecraft.player.getInventory().fillStackedContents(this.stackedContents);
        this.menu.fillCraftSlotsStackedContents(this.stackedContents);
        this.selectMatchingRecipes();
        this.updateCollections(false, this.isFiltering());
    }

    private boolean isFiltering() {
        return this.book.isFiltering(this.menu.getRecipeBookType());
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
        if (this.isVisible()) {
            if (!this.minecraft.hasControlDown()) {
                this.time += partialTick;
            }

            int i = this.getXOrigin();
            int j = this.getYOrigin();
            guiGraphics.blit(RenderPipelines.GUI_TEXTURED, RECIPE_BOOK_LOCATION, i, j, 1.0F, 1.0F, 147, 166, 256, 256);
            this.searchBox.render(guiGraphics, mouseX, mouseY, partialTick);

            for (RecipeBookTabButton recipebooktabbutton : this.tabButtons) {
                recipebooktabbutton.render(guiGraphics, mouseX, mouseY, partialTick);
            }

            this.filterButton.render(guiGraphics, mouseX, mouseY, partialTick);
            this.recipeBookPage.render(guiGraphics, i, j, mouseX, mouseY, partialTick);
        }
    }

    public void renderTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY, @Nullable Slot slot) {
        if (this.isVisible()) {
            this.recipeBookPage.renderTooltip(guiGraphics, mouseX, mouseY);
            this.ghostSlots.renderTooltip(guiGraphics, this.minecraft, mouseX, mouseY, slot);
        }
    }

    protected abstract Component getRecipeFilterName();

    public void renderGhostRecipe(GuiGraphics guiGraphics, boolean isBiggerResultSlot) {
        this.ghostSlots.render(guiGraphics, this.minecraft, isBiggerResultSlot);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean isDoubleClick) {
        if (this.isVisible() && !this.minecraft.player.isSpectator()) {
            if (this.recipeBookPage.mouseClicked(event, this.getXOrigin(), this.getYOrigin(), 147, 166, isDoubleClick)) {
                RecipeDisplayId recipedisplayid = this.recipeBookPage.getLastClickedRecipe();
                RecipeCollection recipecollection = this.recipeBookPage.getLastClickedRecipeCollection();
                if (recipedisplayid != null && recipecollection != null) {
                    if (!this.tryPlaceRecipe(recipecollection, recipedisplayid, event.hasShiftDown())) {
                        return false;
                    }

                    this.lastRecipeCollection = recipecollection;
                    this.lastRecipe = recipedisplayid;
                    if (!this.isOffsetNextToMainGUI()) {
                        this.setVisible(false);
                    }
                }

                return true;
            } else {
                if (this.searchBox != null) {
                    boolean flag = this.magnifierIconPlacement != null
                        && this.magnifierIconPlacement.containsPoint(Mth.floor(event.x()), Mth.floor(event.y()));
                    if (flag || this.searchBox.mouseClicked(event, isDoubleClick)) {
                        this.searchBox.setFocused(true);
                        return true;
                    }

                    this.searchBox.setFocused(false);
                }

                if (this.filterButton.mouseClicked(event, isDoubleClick)) {
                    boolean flag1 = this.toggleFiltering();
                    this.filterButton.setStateTriggered(flag1);
                    this.updateFilterButtonTooltip();
                    this.sendUpdateSettings();
                    this.updateCollections(false, flag1);
                    return true;
                } else {
                    for (RecipeBookTabButton recipebooktabbutton : this.tabButtons) {
                        if (recipebooktabbutton.mouseClicked(event, isDoubleClick)) {
                            if (this.selectedTab != recipebooktabbutton) {
                                if (this.selectedTab != null) {
                                    this.selectedTab.setStateTriggered(false);
                                }

                                this.selectedTab = recipebooktabbutton;
                                this.selectedTab.setStateTriggered(true);
                                this.updateCollections(true, this.isFiltering());
                            }

                            return true;
                        }
                    }

                    return false;
                }
            }
        } else {
            return false;
        }
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double mouseX, double mouseY) {
        return this.searchBox != null && this.searchBox.isFocused() ? this.searchBox.mouseDragged(event, mouseX, mouseY) : false;
    }

    private boolean tryPlaceRecipe(RecipeCollection recipeCollection, RecipeDisplayId recipe, boolean useMaxItems) {
        if (!recipeCollection.isCraftable(recipe) && recipe.equals(this.lastPlacedRecipe)) {
            return false;
        } else {
            this.lastPlacedRecipe = recipe;
            this.ghostSlots.clear();
            this.minecraft.gameMode.handlePlaceRecipe(this.minecraft.player.containerMenu.containerId, recipe, useMaxItems);
            return true;
        }
    }

    private boolean toggleFiltering() {
        RecipeBookType recipebooktype = this.menu.getRecipeBookType();
        boolean flag = !this.book.isFiltering(recipebooktype);
        this.book.setFiltering(recipebooktype, flag);
        return flag;
    }

    public boolean hasClickedOutside(double x, double y, int left, int top, int width, int height) {
        if (!this.isVisible()) {
            return true;
        } else {
            boolean flag = x < left || y < top || x >= left + width || y >= top + height;
            boolean flag1 = left - 147 < x && x < left && top < y && y < top + height;
            return flag && !flag1 && !this.selectedTab.isHoveredOrFocused();
        }
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        this.ignoreTextInput = false;
        if (!this.isVisible() || this.minecraft.player.isSpectator()) {
            return false;
        } else if (event.isEscape() && !this.isOffsetNextToMainGUI()) {
            this.setVisible(false);
            return true;
        } else if (this.searchBox.keyPressed(event)) {
            this.checkSearchStringUpdate();
            return true;
        } else if (this.searchBox.isFocused() && this.searchBox.isVisible() && !event.isEscape()) {
            return true;
        } else if (this.minecraft.options.keyChat.matches(event) && !this.searchBox.isFocused()) {
            this.ignoreTextInput = true;
            this.searchBox.setFocused(true);
            return true;
        } else if (event.isSelection() && this.lastRecipeCollection != null && this.lastRecipe != null) {
            AbstractWidget.playButtonClickSound(Minecraft.getInstance().getSoundManager());
            return this.tryPlaceRecipe(this.lastRecipeCollection, this.lastRecipe, event.hasShiftDown());
        } else {
            return false;
        }
    }

    @Override
    public boolean keyReleased(KeyEvent event) {
        this.ignoreTextInput = false;
        return GuiEventListener.super.keyReleased(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (this.ignoreTextInput) {
            return false;
        } else if (!this.isVisible() || this.minecraft.player.isSpectator()) {
            return false;
        } else if (this.searchBox.charTyped(event)) {
            this.checkSearchStringUpdate();
            return true;
        } else {
            return GuiEventListener.super.charTyped(event);
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

    private void checkSearchStringUpdate() {
        String s = this.searchBox.getValue().toLowerCase(Locale.ROOT);
        this.pirateSpeechForThePeople(s);
        if (!s.equals(this.lastSearch)) {
            this.updateCollections(false, this.isFiltering());
            this.lastSearch = s;
        }
    }

    /**
     * Check if we should activate the pirate speak easter egg.
     */
    private void pirateSpeechForThePeople(String text) {
        if ("excitedze".equals(text)) {
            LanguageManager languagemanager = this.minecraft.getLanguageManager();
            String s = "en_pt";
            LanguageInfo languageinfo = languagemanager.getLanguage("en_pt");
            if (languageinfo == null || languagemanager.getSelected().equals("en_pt")) {
                return;
            }

            languagemanager.setSelected("en_pt");
            this.minecraft.options.languageCode = "en_pt";
            this.minecraft.reloadResourcePacks();
            this.minecraft.options.save();
        }
    }

    private boolean isOffsetNextToMainGUI() {
        return this.xOffset == 86;
    }

    public void recipesUpdated() {
        this.selectMatchingRecipes();
        this.updateTabs(this.isFiltering());
        if (this.isVisible()) {
            this.updateCollections(false, this.isFiltering());
        }
    }

    public void recipeShown(RecipeDisplayId recipe) {
        this.minecraft.player.removeRecipeHighlight(recipe);
    }

    public void fillGhostRecipe(RecipeDisplay recipeDisplay) {
        this.ghostSlots.clear();
        ContextMap contextmap = SlotDisplayContext.fromLevel(Objects.requireNonNull(this.minecraft.level));
        this.fillGhostRecipe(this.ghostSlots, recipeDisplay, contextmap);
    }

    protected abstract void fillGhostRecipe(GhostSlots ghostSlots, RecipeDisplay recipeDisplay, ContextMap contextMap);

    protected void sendUpdateSettings() {
        if (this.minecraft.getConnection() != null) {
            RecipeBookType recipebooktype = this.menu.getRecipeBookType();
            boolean flag = this.book.getBookSettings().isOpen(recipebooktype);
            boolean flag1 = this.book.getBookSettings().isFiltering(recipebooktype);
            this.minecraft.getConnection().send(new ServerboundRecipeBookChangeSettingsPacket(recipebooktype, flag, flag1));
        }
    }

    @Override
    public NarratableEntry.NarrationPriority narrationPriority() {
        return this.visible ? NarratableEntry.NarrationPriority.HOVERED : NarratableEntry.NarrationPriority.NONE;
    }

    /**
     * Updates the narration output with the current narration information.
     *
     * @param narrationElementOutput the output to update with narration information.
     */
    @Override
    public void updateNarration(NarrationElementOutput narrationElementOutput) {
        List<NarratableEntry> list = Lists.newArrayList();
        this.recipeBookPage.listButtons(p_170049_ -> {
            if (p_170049_.isActive()) {
                list.add(p_170049_);
            }
        });
        list.add(this.searchBox);
        list.add(this.filterButton);
        list.addAll(this.tabButtons);
        Screen.NarratableSearchResult screen$narratablesearchresult = Screen.findNarratableWidget(list, null);
        if (screen$narratablesearchresult != null) {
            screen$narratablesearchresult.entry().updateNarration(narrationElementOutput.nest());
        }
    }

    @OnlyIn(Dist.CLIENT)
    public record TabInfo(ItemStack primaryIcon, Optional<ItemStack> secondaryIcon, ExtendedRecipeBookCategory category) {
        public TabInfo(SearchRecipeBookCategory p_380261_) {
            this(new ItemStack(Items.COMPASS), Optional.empty(), p_380261_);
        }

        public TabInfo(Item p_379892_, RecipeBookCategory p_381074_) {
            this(new ItemStack(p_379892_), Optional.empty(), p_381074_);
        }

        public TabInfo(Item p_380069_, Item p_381165_, RecipeBookCategory p_381126_) {
            this(new ItemStack(p_380069_), Optional.of(new ItemStack(p_381165_)), p_381126_);
        }
    }
}
