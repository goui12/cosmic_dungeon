package net.minecraft.client.gui.screens.debug;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.floats.FloatComparators;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.MultiLineTextWidget;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.debug.DebugEntryCategory;
import net.minecraft.client.gui.components.debug.DebugScreenEntries;
import net.minecraft.client.gui.components.debug.DebugScreenEntry;
import net.minecraft.client.gui.components.debug.DebugScreenEntryStatus;
import net.minecraft.client.gui.components.debug.DebugScreenProfile;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LayoutSettings;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.layouts.SpacerElement;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class DebugOptionsScreen extends Screen {
    private static final Component TITLE = Component.translatable("debug.options.title");
    private static final Component SUBTITLE = Component.translatable("debug.options.warning");
    static final Component ENABLED_TEXT = Component.translatable("debug.entry.always");
    static final Component IN_F3_TEXT = Component.translatable("debug.entry.f3");
    static final Component DISABLED_TEXT = CommonComponents.OPTION_OFF;
    static final Component NOT_ALLOWED_TOOLTIP = Component.translatable("debug.options.notAllowed.tooltip");
    private static final Component SEARCH = Component.translatable("debug.options.search").withStyle(EditBox.SEARCH_HINT_STYLE);
    final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this, 61, 33);
    @Nullable
    private DebugOptionsScreen.OptionList optionList;
    private EditBox searchBox;
    final List<Button> profileButtons = new ArrayList<>();

    public DebugOptionsScreen() {
        super(TITLE);
    }

    @Override
    protected void init() {
        LinearLayout linearlayout = this.layout.addToHeader(LinearLayout.vertical().spacing(8));
        this.optionList = new DebugOptionsScreen.OptionList();
        int i = this.optionList.getRowWidth();
        LinearLayout linearlayout1 = LinearLayout.horizontal().spacing(8);
        linearlayout1.addChild(new SpacerElement(i / 3, 1));
        linearlayout1.addChild(new StringWidget(TITLE, this.font), linearlayout1.newCellSettings().alignVerticallyMiddle());
        this.searchBox = new EditBox(this.font, 0, 0, i / 3, 20, this.searchBox, SEARCH);
        this.searchBox.setResponder(p_435276_ -> this.optionList.updateSearch(p_435276_));
        this.searchBox.setHint(SEARCH);
        linearlayout1.addChild(this.searchBox);
        linearlayout.addChild(linearlayout1, LayoutSettings::alignHorizontallyCenter);
        linearlayout.addChild(
            new MultiLineTextWidget(SUBTITLE, this.font).setMaxWidth(i).setCentered(true).setColor(-2142128), LayoutSettings::alignHorizontallyCenter
        );
        this.layout.addToContents(this.optionList);
        LinearLayout linearlayout2 = this.layout.addToFooter(LinearLayout.horizontal().spacing(8));
        this.addProfileButton(DebugScreenProfile.DEFAULT, linearlayout2);
        this.addProfileButton(DebugScreenProfile.PERFORMANCE, linearlayout2);
        linearlayout2.addChild(Button.builder(CommonComponents.GUI_DONE, p_433376_ -> this.onClose()).width(60).build());
        this.layout.visitWidgets(p_434745_ -> {
            AbstractWidget abstractwidget = this.addRenderableWidget(p_434745_);
        });
        this.repositionElements();
    }

    @Override
    public void renderBlurredBackground(GuiGraphics guiGraphics) {
        this.minecraft.gui.renderDebugOverlay(guiGraphics);
        super.renderBlurredBackground(guiGraphics);
    }

    @Override
    protected void setInitialFocus() {
        this.setInitialFocus(this.searchBox);
    }

    private void addProfileButton(DebugScreenProfile profile, LinearLayout layout) {
        Button button = Button.builder(Component.translatable(profile.translationKey()), p_434423_ -> {
            this.minecraft.debugEntries.loadProfile(profile);
            this.minecraft.debugEntries.save();
            this.optionList.refreshEntries();

            for (Button button1 : this.profileButtons) {
                button1.active = true;
            }

            p_434423_.active = false;
        }).width(120).build();
        button.active = !this.minecraft.debugEntries.isUsingProfile(profile);
        this.profileButtons.add(button);
        layout.addChild(button);
    }

    @Override
    protected void repositionElements() {
        this.layout.arrangeElements();
        if (this.optionList != null) {
            this.optionList.updateSize(this.width, this.layout);
        }
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
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @OnlyIn(Dist.CLIENT)
    public abstract static class AbstractOptionEntry extends ContainerObjectSelectionList.Entry<DebugOptionsScreen.AbstractOptionEntry> {
        public abstract void refreshEntry();
    }

    @OnlyIn(Dist.CLIENT)
    class CategoryEntry extends DebugOptionsScreen.AbstractOptionEntry {
        final Component category;

        public CategoryEntry(Component category) {
            this.category = category;
        }

        @Override
        public void renderContent(GuiGraphics guiGraphics, int mouseX, int mouseY, boolean isHovering, float partialTick) {
            guiGraphics.drawCenteredString(
                DebugOptionsScreen.this.minecraft.font, this.category, this.getContentX() + this.getContentWidth() / 2, this.getContentY() + 5, -1
            );
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return ImmutableList.of();
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return ImmutableList.of(new NarratableEntry() {
                @Override
                public NarratableEntry.NarrationPriority narrationPriority() {
                    return NarratableEntry.NarrationPriority.HOVERED;
                }

                @Override
                public void updateNarration(NarrationElementOutput p_434625_) {
                    p_434625_.add(NarratedElementType.TITLE, CategoryEntry.this.category);
                }
            });
        }

        @Override
        public void refreshEntry() {
        }
    }

    @OnlyIn(Dist.CLIENT)
    class OptionEntry extends DebugOptionsScreen.AbstractOptionEntry {
        private final ResourceLocation location;
        protected final List<AbstractWidget> children = Lists.newArrayList();
        private final CycleButton<Boolean> always;
        private final CycleButton<Boolean> f3;
        private final CycleButton<Boolean> never;
        private final String name;
        private final boolean isAllowed;

        public OptionEntry(ResourceLocation location) {
            this.location = location;
            DebugScreenEntry debugscreenentry = DebugScreenEntries.getEntry(location);
            this.isAllowed = debugscreenentry != null && debugscreenentry.isAllowed(DebugOptionsScreen.this.minecraft.showOnlyReducedInfo());
            String s = location.getPath();
            // Neo: Display the full RL for modded entries
            if(!location.getNamespace().equals(ResourceLocation.DEFAULT_NAMESPACE)) {
                s = location.toString();
            }
            if (this.isAllowed) {
                this.name = s;
            } else {
                this.name = ChatFormatting.ITALIC + s;
            }

            this.always = CycleButton.booleanBuilder(
                    DebugOptionsScreen.ENABLED_TEXT.copy().withColor(-2142128), DebugOptionsScreen.ENABLED_TEXT.copy().withColor(-4539718)
                )
                .displayOnlyValue()
                .withCustomNarration(this::narrateButton)
                .create(10, 5, 44, 16, Component.literal(s), (p_433631_, p_434952_) -> this.setValue(location, DebugScreenEntryStatus.ALWAYS_ON));
            this.f3 = CycleButton.booleanBuilder(DebugOptionsScreen.IN_F3_TEXT.copy().withColor(-171), DebugOptionsScreen.IN_F3_TEXT.copy().withColor(-4539718))
                .displayOnlyValue()
                .withCustomNarration(this::narrateButton)
                .create(10, 5, 44, 16, Component.literal(s), (p_434889_, p_432811_) -> this.setValue(location, DebugScreenEntryStatus.IN_F3));
            this.never = CycleButton.booleanBuilder(
                    DebugOptionsScreen.DISABLED_TEXT.copy().withColor(-1), DebugOptionsScreen.DISABLED_TEXT.copy().withColor(-4539718)
                )
                .displayOnlyValue()
                .withCustomNarration(this::narrateButton)
                .create(10, 5, 44, 16, Component.literal(s), (p_433843_, p_434189_) -> this.setValue(location, DebugScreenEntryStatus.NEVER));
            this.children.add(this.never);
            this.children.add(this.f3);
            this.children.add(this.always);
            this.refreshEntry();
        }

        private MutableComponent narrateButton(CycleButton<Boolean> button) {
            DebugScreenEntryStatus debugscreenentrystatus = DebugOptionsScreen.this.minecraft.debugEntries.getStatus(this.location);
            MutableComponent mutablecomponent = Component.translatable("debug.entry.currently." + debugscreenentrystatus.getSerializedName(), this.name);
            return CommonComponents.optionNameValue(mutablecomponent, button.getMessage());
        }

        private void setValue(ResourceLocation entry, DebugScreenEntryStatus status) {
            DebugOptionsScreen.this.minecraft.debugEntries.setStatus(entry, status);

            for (Button button : DebugOptionsScreen.this.profileButtons) {
                button.active = true;
            }

            this.refreshEntry();
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return this.children;
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return this.children;
        }

        @Override
        public void renderContent(GuiGraphics guiGraphics, int mouseX, int mouseY, boolean isHovering, float partialTick) {
            int i = this.getContentX();
            int j = this.getContentY();
            // Neo: Moved 'k' up for 'maxX' calculation for scrolling string render
            int k = i + this.getContentWidth() - this.never.getWidth() - this.f3.getWidth() - this.always.getWidth();
            // Neo: Scroll entry names that go out of bounds
            guiGraphics.drawScrollingString(DebugOptionsScreen.this.minecraft.font, Component.literal(this.name), i, k, j + 5, this.isAllowed ? -1 : -8355712);
            if (!this.isAllowed && isHovering && mouseX < k) {
                guiGraphics.setTooltipForNextFrame(DebugOptionsScreen.NOT_ALLOWED_TOOLTIP, mouseX, mouseY);
            }

            this.never.setX(k);
            this.f3.setX(this.never.getX() + this.never.getWidth());
            this.always.setX(this.f3.getX() + this.f3.getWidth());
            this.always.setY(j);
            this.f3.setY(j);
            this.never.setY(j);
            this.always.render(guiGraphics, mouseX, mouseY, partialTick);
            this.f3.render(guiGraphics, mouseX, mouseY, partialTick);
            this.never.render(guiGraphics, mouseX, mouseY, partialTick);
        }

        @Override
        public void refreshEntry() {
            DebugScreenEntryStatus debugscreenentrystatus = DebugOptionsScreen.this.minecraft.debugEntries.getStatus(this.location);
            this.always.setValue(debugscreenentrystatus == DebugScreenEntryStatus.ALWAYS_ON);
            this.f3.setValue(debugscreenentrystatus == DebugScreenEntryStatus.IN_F3);
            this.never.setValue(debugscreenentrystatus == DebugScreenEntryStatus.NEVER);
            this.always.active = !this.always.getValue();
            this.f3.active = !this.f3.getValue();
            this.never.active = !this.never.getValue();
        }
    }

    @OnlyIn(Dist.CLIENT)
    class OptionList extends ContainerObjectSelectionList<DebugOptionsScreen.AbstractOptionEntry> {
        private static final Comparator<Map.Entry<ResourceLocation, DebugScreenEntry>> COMPARATOR = (p_435578_, p_434178_) -> {
            int i = FloatComparators.NATURAL_COMPARATOR.compare(p_435578_.getValue().category().sortKey(), p_434178_.getValue().category().sortKey());
            return i != 0 ? i : p_435578_.getKey().compareTo(p_434178_.getKey());
        };
        private static final int ITEM_HEIGHT = 20;

        public OptionList() {
            super(
                Minecraft.getInstance(),
                DebugOptionsScreen.this.width,
                DebugOptionsScreen.this.layout.getContentHeight(),
                DebugOptionsScreen.this.layout.getHeaderHeight(),
                20
            );
            this.updateSearch("");
        }

        @Override
        public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            super.renderWidget(guiGraphics, mouseX, mouseY, partialTick);
        }

        @Override
        public int getRowWidth() {
            return 310;
        }

        public void refreshEntries() {
            this.children().forEach(DebugOptionsScreen.AbstractOptionEntry::refreshEntry);
        }

        public void updateSearch(String query) {
            this.clearEntries();
            // Neo: Custom entry appending logic to better handle searching, categories and sorting
            net.neoforged.neoforge.client.ClientHooks.updateDebugScreenEntriesForSearch(query, category -> addEntry(new CategoryEntry(category.label())), id -> addEntry(new OptionEntry(id)));
            if(false){
            List<Map.Entry<ResourceLocation, DebugScreenEntry>> list = new ArrayList<>(DebugScreenEntries.allEntries().entrySet());
            list.sort(COMPARATOR);
            DebugEntryCategory debugentrycategory = null;

            for (Map.Entry<ResourceLocation, DebugScreenEntry> entry : list) {
                if (entry.getKey().getPath().contains(query)) {
                    DebugEntryCategory debugentrycategory1 = entry.getValue().category();
                    if (!debugentrycategory1.equals(debugentrycategory)) {
                        this.addEntry(DebugOptionsScreen.this.new CategoryEntry(debugentrycategory1.label()));
                        debugentrycategory = debugentrycategory1;
                    }

                    this.addEntry(DebugOptionsScreen.this.new OptionEntry(entry.getKey()));
                }
            }
            }

            this.notifyListUpdated();
        }

        private void notifyListUpdated() {
            this.refreshScrollAmount();
            DebugOptionsScreen.this.triggerImmediateNarration(true);
        }
    }
}
