package net.minecraft.client.gui.screens;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.mojang.logging.LogUtils;
import java.net.URI;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.NarratorStatus;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.TabOrderedElement;
import net.minecraft.client.gui.components.events.AbstractContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.narration.ScreenNarrationCollector;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.gui.navigation.ScreenDirection;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.protocol.common.ServerboundCustomClickActionPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.Music;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public abstract class Screen extends AbstractContainerEventHandler implements Renderable {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Component USAGE_NARRATION = Component.translatable("narrator.screen.usage");
    public static final ResourceLocation MENU_BACKGROUND = ResourceLocation.withDefaultNamespace("textures/gui/menu_background.png");
    public static final ResourceLocation HEADER_SEPARATOR = ResourceLocation.withDefaultNamespace("textures/gui/header_separator.png");
    public static final ResourceLocation FOOTER_SEPARATOR = ResourceLocation.withDefaultNamespace("textures/gui/footer_separator.png");
    private static final ResourceLocation INWORLD_MENU_BACKGROUND = ResourceLocation.withDefaultNamespace("textures/gui/inworld_menu_background.png");
    public static final ResourceLocation INWORLD_HEADER_SEPARATOR = ResourceLocation.withDefaultNamespace("textures/gui/inworld_header_separator.png");
    public static final ResourceLocation INWORLD_FOOTER_SEPARATOR = ResourceLocation.withDefaultNamespace("textures/gui/inworld_footer_separator.png");
    protected static final float FADE_IN_TIME = 2000.0F;
    protected final Component title;
    private final List<GuiEventListener> children = Lists.newArrayList();
    private final List<NarratableEntry> narratables = Lists.newArrayList();
    @Nullable
    protected Minecraft minecraft;
    private boolean initialized;
    public int width;
    public int height;
    public final List<Renderable> renderables = Lists.newArrayList();
    protected Font font;
    private static final long NARRATE_SUPPRESS_AFTER_INIT_TIME = TimeUnit.SECONDS.toMillis(2L);
    private static final long NARRATE_DELAY_NARRATOR_ENABLED = NARRATE_SUPPRESS_AFTER_INIT_TIME;
    private static final long NARRATE_DELAY_MOUSE_MOVE = 750L;
    private static final long NARRATE_DELAY_MOUSE_ACTION = 200L;
    private static final long NARRATE_DELAY_KEYBOARD_ACTION = 200L;
    private final ScreenNarrationCollector narrationState = new ScreenNarrationCollector();
    private long narrationSuppressTime = Long.MIN_VALUE;
    private long nextNarrationTime = Long.MAX_VALUE;
    @Nullable
    protected CycleButton<NarratorStatus> narratorButton;
    @Nullable
    private NarratableEntry lastNarratable;
    protected final Executor screenExecutor = p_289626_ -> this.minecraft.execute(() -> {
        if (this.minecraft.screen == this) {
            p_289626_.run();
        }
    });

    protected Screen(Component title) {
        this.title = title;
    }

    public Component getTitle() {
        return this.title;
    }

    public Component getNarrationMessage() {
        return this.getTitle();
    }

    public final void renderWithTooltipAndSubtitles(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        guiGraphics.nextStratum();
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.client.event.ScreenEvent.Render.Background(this, guiGraphics, mouseX, mouseY, partialTick));
        guiGraphics.nextStratum();
        this.render(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.renderDeferredElements();
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
        for (Renderable renderable : this.renderables) {
            renderable.render(guiGraphics, mouseX, mouseY, partialTick);
        }
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.isEscape() && this.shouldCloseOnEsc()) {
            this.onClose();
            return true;
        } else if (super.keyPressed(event)) {
            return true;
        } else {
            FocusNavigationEvent focusnavigationevent = (FocusNavigationEvent)(switch (event.key()) {
                case 258 -> this.createTabEvent(!event.hasShiftDown());
                default -> null;
                case 262 -> this.createArrowEvent(ScreenDirection.RIGHT);
                case 263 -> this.createArrowEvent(ScreenDirection.LEFT);
                case 264 -> this.createArrowEvent(ScreenDirection.DOWN);
                case 265 -> this.createArrowEvent(ScreenDirection.UP);
            });
            if (focusnavigationevent != null) {
                ComponentPath componentpath = super.nextFocusPath(focusnavigationevent);
                if (componentpath == null && focusnavigationevent instanceof FocusNavigationEvent.TabNavigation) {
                    this.clearFocus();
                    componentpath = super.nextFocusPath(focusnavigationevent);
                }

                if (componentpath != null) {
                    this.changeFocus(componentpath);
                }
            }

            return false;
        }
    }

    private FocusNavigationEvent.TabNavigation createTabEvent(boolean forward) {
        return new FocusNavigationEvent.TabNavigation(forward);
    }

    private FocusNavigationEvent.ArrowNavigation createArrowEvent(ScreenDirection direction) {
        return new FocusNavigationEvent.ArrowNavigation(direction);
    }

    protected void setInitialFocus() {
        if (this.minecraft.getLastInputType().isKeyboard()) {
            FocusNavigationEvent.TabNavigation focusnavigationevent$tabnavigation = new FocusNavigationEvent.TabNavigation(true);
            ComponentPath componentpath = super.nextFocusPath(focusnavigationevent$tabnavigation);
            if (componentpath != null) {
                this.changeFocus(componentpath);
            }
        }
    }

    protected void setInitialFocus(GuiEventListener listener) {
        ComponentPath componentpath = ComponentPath.path(this, listener.nextFocusPath(new FocusNavigationEvent.InitialFocus()));
        if (componentpath != null) {
            this.changeFocus(componentpath);
        }
    }

    public void clearFocus() {
        ComponentPath componentpath = this.getCurrentFocusPath();
        if (componentpath != null) {
            componentpath.applyFocus(false);
        }
    }

    @VisibleForTesting
    protected void changeFocus(ComponentPath path) {
        this.clearFocus();
        path.applyFocus(true);
    }

    public boolean shouldCloseOnEsc() {
        return true;
    }

    public void onClose() {
        this.minecraft.popGuiLayer();
    }

    protected <T extends GuiEventListener & Renderable & NarratableEntry> T addRenderableWidget(T widget) {
        this.renderables.add(widget);
        return this.addWidget(widget);
    }

    protected <T extends Renderable> T addRenderableOnly(T renderable) {
        this.renderables.add(renderable);
        return renderable;
    }

    protected <T extends GuiEventListener & NarratableEntry> T addWidget(T listener) {
        this.children.add(listener);
        this.narratables.add(listener);
        return listener;
    }

    protected void removeWidget(GuiEventListener listener) {
        if (listener instanceof Renderable) {
            this.renderables.remove((Renderable)listener);
        }

        if (listener instanceof NarratableEntry) {
            this.narratables.remove((NarratableEntry)listener);
        }

        if (this.getFocused() == listener) {
            this.clearFocus();
        }

        this.children.remove(listener);
    }

    protected void clearWidgets() {
        this.renderables.clear();
        this.children.clear();
        this.narratables.clear();
    }

    public static List<Component> getTooltipFromItem(Minecraft minecraft, ItemStack item) {
        return item.getTooltipLines(
            Item.TooltipContext.of(minecraft.level),
            minecraft.player,
            net.neoforged.neoforge.client.ClientTooltipFlag.of(minecraft.options.advancedItemTooltips ? TooltipFlag.Default.ADVANCED : TooltipFlag.Default.NORMAL)
        );
    }

    protected void insertText(String text, boolean overwrite) {
    }

    public boolean handleComponentClicked(Style style) {
        ClickEvent clickevent = style.getClickEvent();
        if (this.minecraft.hasShiftDown()) {
            if (style.getInsertion() != null) {
                this.insertText(style.getInsertion(), false);
            }
        } else if (clickevent != null) {
            this.handleClickEvent(this.minecraft, clickevent);
            return true;
        }

        return false;
    }

    protected void handleClickEvent(Minecraft minecraft, ClickEvent clickEvent) {
        defaultHandleGameClickEvent(clickEvent, minecraft, this);
    }

    protected static void defaultHandleGameClickEvent(ClickEvent clickEvent, Minecraft minecraft, @Nullable Screen screen) {
        LocalPlayer localplayer = Objects.requireNonNull(minecraft.player, "Player not available");
        switch (clickEvent) {
            case ClickEvent.RunCommand(String s):
                clickCommandAction(localplayer, s, screen);
                break;
            case ClickEvent.ShowDialog clickevent$showdialog:
                localplayer.connection.showDialog(clickevent$showdialog.dialog(), screen);
                break;
            case ClickEvent.Custom clickevent$custom:
                localplayer.connection.send(new ServerboundCustomClickActionPacket(clickevent$custom.id(), clickevent$custom.payload()));
                if (minecraft.screen != screen) {
                    minecraft.setScreen(screen);
                }
                break;
            default:
                defaultHandleClickEvent(clickEvent, minecraft, screen);
        }
    }

    protected static void defaultHandleClickEvent(ClickEvent clickEvent, Minecraft minecraft, @Nullable Screen screen) {
        boolean flag = switch (clickEvent) {
            case ClickEvent.OpenUrl(URI uri) -> {
                clickUrlAction(minecraft, screen, uri);
                yield false;
            }
            case ClickEvent.OpenFile clickevent$openfile -> {
                Util.getPlatform().openFile(clickevent$openfile.file());
                yield true;
            }
            case ClickEvent.SuggestCommand(String s2) -> {
                String s1 = s2;
                if (screen != null) {
                    screen.insertText(s1, true);
                }

                yield true;
            }
            case ClickEvent.CopyToClipboard(String s) -> {
                minecraft.keyboardHandler.setClipboard(s);
                yield true;
            }
            default -> {
                LOGGER.error("Don't know how to handle {}", clickEvent);
                yield true;
            }
        };
        if (flag && minecraft.screen != screen) {
            minecraft.setScreen(screen);
        }
    }

    protected static boolean clickUrlAction(Minecraft minecraft, @Nullable Screen screen, URI url) {
        if (!minecraft.options.chatLinks().get()) {
            return false;
        } else {
            if (minecraft.options.chatLinksPrompt().get()) {
                minecraft.setScreen(new ConfirmLinkScreen(p_425143_ -> {
                    if (p_425143_) {
                        Util.getPlatform().openUri(url);
                    }

                    minecraft.setScreen(screen);
                }, url.toString(), false));
            } else {
                Util.getPlatform().openUri(url);
            }

            return true;
        }
    }

    protected static void clickCommandAction(LocalPlayer player, String command, @Nullable Screen screen) {
        player.connection.sendUnattendedCommand(Commands.trimOptionalPrefix(command), screen);
    }

    public final void init(Minecraft minecraft, int width, int height) {
        this.minecraft = minecraft;
        this.font = minecraft.font;
        this.width = width;
        this.height = height;
        if (!this.initialized) {
            if (!net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.client.event.ScreenEvent.Init.Pre(this, this.children, this::addEventWidget, this::removeWidget)).isCanceled()) {
            this.init();
            this.setInitialFocus();
            }
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.client.event.ScreenEvent.Init.Post(this, this.children, this::addEventWidget, this::removeWidget));
        } else {
            this.repositionElements();
        }

        this.initialized = true;
        this.triggerImmediateNarration(false);
        if (minecraft.getLastInputType().isKeyboard()) {
            this.setNarrationSuppressTime(Long.MAX_VALUE);
        } else {
            this.suppressNarration(NARRATE_SUPPRESS_AFTER_INIT_TIME);
        }
    }

    protected void rebuildWidgets() {
        this.clearWidgets();
        this.clearFocus();
        if (!net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.client.event.ScreenEvent.Init.Pre(this, this.children, this::addEventWidget, this::removeWidget)).isCanceled()) {
        this.init();
        this.setInitialFocus();
        }
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.client.event.ScreenEvent.Init.Post(this, this.children, this::addEventWidget, this::removeWidget));
    }

    protected void fadeWidgets(float fade) {
        for (GuiEventListener guieventlistener : this.children()) {
            if (guieventlistener instanceof AbstractWidget abstractwidget) {
                abstractwidget.setAlpha(fade);
            }
        }
    }

    @Override
    public List<? extends GuiEventListener> children() {
        return this.children;
    }

    protected void init() {
    }

    public void tick() {
    }

    public void removed() {
    }

    public void added() {
    }

    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (this.isInGameUi()) {
            this.renderTransparentBackground(guiGraphics);
        } else {
            if (this.minecraft.level == null) {
                this.renderPanorama(guiGraphics, partialTick);
            }

            this.renderBlurredBackground(guiGraphics);
            this.renderMenuBackground(guiGraphics);
        }

        this.minecraft.gui.renderDeferredSubtitles();
    }

    protected void renderBlurredBackground(GuiGraphics guiGraphics) {
        float f = this.minecraft.options.getMenuBackgroundBlurriness();
        if (f >= 1.0F) {
            guiGraphics.blurBeforeThisStratum();
        }
    }

    protected void renderPanorama(GuiGraphics guiGraphics, float partialTick) {
        this.minecraft.gameRenderer.getPanorama().render(guiGraphics, this.width, this.height, this.panoramaShouldSpin());
    }

    protected void renderMenuBackground(GuiGraphics partialTick) {
        this.renderMenuBackground(partialTick, 0, 0, this.width, this.height);
    }

    protected void renderMenuBackground(GuiGraphics guiGraphics, int x, int y, int width, int height) {
        renderMenuBackgroundTexture(
            guiGraphics, this.minecraft.level == null ? MENU_BACKGROUND : INWORLD_MENU_BACKGROUND, x, y, 0.0F, 0.0F, width, height
        );
    }

    public static void renderMenuBackgroundTexture(
        GuiGraphics guiGraphics, ResourceLocation texture, int x, int y, float uOffset, float vOffset, int width, int height
    ) {
        int i = 32;
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, uOffset, vOffset, width, height, 32, 32);
    }

    public void renderTransparentBackground(GuiGraphics guiGraphics) {
        guiGraphics.fillGradient(0, 0, this.width, this.height, -1072689136, -804253680);
    }

    public boolean isPauseScreen() {
        return true;
    }

    public boolean isInGameUi() {
        return false;
    }

    protected boolean panoramaShouldSpin() {
        return true;
    }

    public boolean isAllowedInPortal() {
        return this.isPauseScreen();
    }

    protected void repositionElements() {
        this.rebuildWidgets();
    }

    public void resize(Minecraft minecraft, int width, int height) {
        this.width = width;
        this.height = height;
        this.repositionElements();
    }

    public void fillCrashDetails(CrashReport crashReport) {
        CrashReportCategory crashreportcategory = crashReport.addCategory("Affected screen", 1);
        crashreportcategory.setDetail("Screen name", () -> this.getClass().getCanonicalName());
    }

    protected boolean isValidCharacterForName(String text, int codepoint, int position) {
        int i = text.indexOf(58);
        int j = text.indexOf(47);
        if (codepoint == 58) {
            return (j == -1 || position <= j) && i == -1;
        } else {
            return codepoint == 47
                ? position > i
                : codepoint == 95 || codepoint == 45 || codepoint >= 97 && codepoint <= 122 || codepoint >= 48 && codepoint <= 57 || codepoint == 46;
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
        return true;
    }

    public void onFilesDrop(List<Path> packs) {
    }

    public Minecraft getMinecraft() {
        return this.minecraft;
    }

    private void scheduleNarration(long delay, boolean stopSuppression) {
        this.nextNarrationTime = Util.getMillis() + delay;
        if (stopSuppression) {
            this.narrationSuppressTime = Long.MIN_VALUE;
        }
    }

    private void suppressNarration(long time) {
        this.setNarrationSuppressTime(Util.getMillis() + time);
    }

    private void setNarrationSuppressTime(long narrationSuppressTime) {
        this.narrationSuppressTime = narrationSuppressTime;
    }

    public void afterMouseMove() {
        this.scheduleNarration(750L, false);
    }

    public void afterMouseAction() {
        this.scheduleNarration(200L, true);
    }

    public void afterKeyboardAction() {
        this.scheduleNarration(200L, true);
    }

    private boolean shouldRunNarration() {
        return SharedConstants.DEBUG_UI_NARRATION || this.minecraft.getNarrator().isActive();
    }

    public void handleDelayedNarration() {
        if (this.shouldRunNarration()) {
            long i = Util.getMillis();
            if (i > this.nextNarrationTime && i > this.narrationSuppressTime) {
                this.runNarration(true);
                this.nextNarrationTime = Long.MAX_VALUE;
            }
        }
    }

    public void triggerImmediateNarration(boolean onlyNarrateNew) {
        if (this.shouldRunNarration()) {
            this.runNarration(onlyNarrateNew);
        }
    }

    private void runNarration(boolean onlyNarrateNew) {
        this.narrationState.update(this::updateNarrationState);
        String s = this.narrationState.collectNarrationText(!onlyNarrateNew);
        if (!s.isEmpty()) {
            this.minecraft.getNarrator().saySystemNow(s);
        }
    }

    protected boolean shouldNarrateNavigation() {
        return true;
    }

    protected void updateNarrationState(NarrationElementOutput output) {
        output.add(NarratedElementType.TITLE, this.getNarrationMessage());
        if (this.shouldNarrateNavigation()) {
            output.add(NarratedElementType.USAGE, USAGE_NARRATION);
        }

        this.updateNarratedWidget(output);
    }

    protected void updateNarratedWidget(NarrationElementOutput narrationElementOutput) {
        List<? extends NarratableEntry> list = this.narratables
            .stream()
            .flatMap(p_386212_ -> p_386212_.getNarratables().stream())
            .filter(NarratableEntry::isActive)
            .sorted(Comparator.comparingInt(TabOrderedElement::getTabOrderGroup))
            .toList();
        Screen.NarratableSearchResult screen$narratablesearchresult = findNarratableWidget(list, this.lastNarratable);
        if (screen$narratablesearchresult != null) {
            if (screen$narratablesearchresult.priority.isTerminal()) {
                this.lastNarratable = screen$narratablesearchresult.entry;
            }

            if (list.size() > 1) {
                narrationElementOutput.add(
                    NarratedElementType.POSITION, Component.translatable("narrator.position.screen", screen$narratablesearchresult.index + 1, list.size())
                );
                if (screen$narratablesearchresult.priority == NarratableEntry.NarrationPriority.FOCUSED) {
                    narrationElementOutput.add(NarratedElementType.USAGE, this.getUsageNarration());
                }
            }

            screen$narratablesearchresult.entry.updateNarration(narrationElementOutput.nest());
        }
    }

    protected Component getUsageNarration() {
        return Component.translatable("narration.component_list.usage");
    }

    @Nullable
    public static Screen.NarratableSearchResult findNarratableWidget(List<? extends NarratableEntry> entries, @Nullable NarratableEntry target) {
        Screen.NarratableSearchResult screen$narratablesearchresult = null;
        Screen.NarratableSearchResult screen$narratablesearchresult1 = null;
        int i = 0;

        for (int j = entries.size(); i < j; i++) {
            NarratableEntry narratableentry = entries.get(i);
            NarratableEntry.NarrationPriority narratableentry$narrationpriority = narratableentry.narrationPriority();
            if (narratableentry$narrationpriority.isTerminal()) {
                if (narratableentry != target) {
                    return new Screen.NarratableSearchResult(narratableentry, i, narratableentry$narrationpriority);
                }

                screen$narratablesearchresult1 = new Screen.NarratableSearchResult(narratableentry, i, narratableentry$narrationpriority);
            } else if (narratableentry$narrationpriority.compareTo(
                    screen$narratablesearchresult != null ? screen$narratablesearchresult.priority : NarratableEntry.NarrationPriority.NONE
                )
                > 0) {
                screen$narratablesearchresult = new Screen.NarratableSearchResult(narratableentry, i, narratableentry$narrationpriority);
            }
        }

        return screen$narratablesearchresult != null ? screen$narratablesearchresult : screen$narratablesearchresult1;
    }

    public void updateNarratorStatus(boolean narratorEnabled) {
        if (narratorEnabled) {
            this.scheduleNarration(NARRATE_DELAY_NARRATOR_ENABLED, false);
        }

        if (this.narratorButton != null) {
            this.narratorButton.setValue(this.minecraft.options.narrator().get());
        }
    }

    public Font getFont() {
        return this.font;
    }

    public boolean showsActiveEffects() {
        return false;
    }

    public boolean canInterruptWithAnotherScreen() {
        return this.shouldCloseOnEsc();
    }

    @Override
    public ScreenRectangle getRectangle() {
        return new ScreenRectangle(0, 0, this.width, this.height);
    }

    @Nullable
    public Music getBackgroundMusic() {
        return null;
    }

    private void addEventWidget(GuiEventListener b) {
        if (b instanceof Renderable r)
            this.renderables.add(r);
        if (b instanceof NarratableEntry ne)
            this.narratables.add(ne);
        children.add(b);
    }

    @OnlyIn(Dist.CLIENT)
    public record NarratableSearchResult(NarratableEntry entry, int index, NarratableEntry.NarrationPriority priority) {
    }
}
