package net.minecraft.client.gui.screens.worldselection;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.CrashReport;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.AlertScreen;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.ErrorScreen;
import net.minecraft.client.gui.screens.FaviconTexture;
import net.minecraft.client.gui.screens.GenericMessageScreen;
import net.minecraft.client.gui.screens.LoadingDotsText;
import net.minecraft.client.gui.screens.NoticeWithLinkScreen;
import net.minecraft.client.gui.screens.ProgressScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.nbt.NbtException;
import net.minecraft.nbt.ReportedNbtException;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.LevelStorageException;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.LevelSummary;
import net.minecraft.world.level.validation.ContentValidationException;
import net.minecraft.world.level.validation.ForbiddenSymlinkInfo;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class WorldSelectionList extends ObjectSelectionList<WorldSelectionList.Entry> {
    public static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT).withZone(ZoneId.systemDefault());
    static final ResourceLocation ERROR_HIGHLIGHTED_SPRITE = ResourceLocation.withDefaultNamespace("world_list/error_highlighted");
    static final ResourceLocation ERROR_SPRITE = ResourceLocation.withDefaultNamespace("world_list/error");
    static final ResourceLocation MARKED_JOIN_HIGHLIGHTED_SPRITE = ResourceLocation.withDefaultNamespace("world_list/marked_join_highlighted");
    static final ResourceLocation MARKED_JOIN_SPRITE = ResourceLocation.withDefaultNamespace("world_list/marked_join");
    static final ResourceLocation WARNING_HIGHLIGHTED_SPRITE = ResourceLocation.withDefaultNamespace("world_list/warning_highlighted");
    static final ResourceLocation WARNING_SPRITE = ResourceLocation.withDefaultNamespace("world_list/warning");
    static final ResourceLocation JOIN_HIGHLIGHTED_SPRITE = ResourceLocation.withDefaultNamespace("world_list/join_highlighted");
    static final ResourceLocation JOIN_SPRITE = ResourceLocation.withDefaultNamespace("world_list/join");
    private static final ResourceLocation FORGE_EXPERIMENTAL_WARNING_ICON = ResourceLocation.fromNamespaceAndPath("neoforge","textures/gui/experimental_warning.png");
    static final Logger LOGGER = LogUtils.getLogger();
    static final Component FROM_NEWER_TOOLTIP_1 = Component.translatable("selectWorld.tooltip.fromNewerVersion1").withStyle(ChatFormatting.RED);
    static final Component FROM_NEWER_TOOLTIP_2 = Component.translatable("selectWorld.tooltip.fromNewerVersion2").withStyle(ChatFormatting.RED);
    static final Component SNAPSHOT_TOOLTIP_1 = Component.translatable("selectWorld.tooltip.snapshot1").withStyle(ChatFormatting.GOLD);
    static final Component SNAPSHOT_TOOLTIP_2 = Component.translatable("selectWorld.tooltip.snapshot2").withStyle(ChatFormatting.GOLD);
    static final Component WORLD_LOCKED_TOOLTIP = Component.translatable("selectWorld.locked").withStyle(ChatFormatting.RED);
    static final Component WORLD_REQUIRES_CONVERSION = Component.translatable("selectWorld.conversion.tooltip").withStyle(ChatFormatting.RED);
    static final Component INCOMPATIBLE_VERSION_TOOLTIP = Component.translatable("selectWorld.incompatible.tooltip").withStyle(ChatFormatting.RED);
    static final Component WORLD_EXPERIMENTAL = Component.translatable("selectWorld.experimental");
    private final Screen screen;
    private CompletableFuture<List<LevelSummary>> pendingLevels;
    @Nullable
    private List<LevelSummary> currentlyDisplayedLevels;
    private final WorldSelectionList.LoadingHeader loadingHeader;
    final WorldSelectionList.EntryType entryType;
    private String filter;
    private boolean hasPolled;
    @Nullable
    private final Consumer<LevelSummary> onEntrySelect;
    @Nullable
    final Consumer<WorldSelectionList.WorldListEntry> onEntryInteract;

    WorldSelectionList(
        Screen screen,
        Minecraft minecraft,
        int width,
        int height,
        String filter,
        @Nullable WorldSelectionList oldList,
        @Nullable Consumer<LevelSummary> onEntrySelect,
        @Nullable Consumer<WorldSelectionList.WorldListEntry> onEntryInteract,
        WorldSelectionList.EntryType entryType
    ) {
        super(minecraft, width, height, 0, 36);
        this.screen = screen;
        this.loadingHeader = new WorldSelectionList.LoadingHeader(minecraft);
        this.filter = filter;
        this.onEntrySelect = onEntrySelect;
        this.onEntryInteract = onEntryInteract;
        this.entryType = entryType;
        if (oldList != null) {
            this.pendingLevels = oldList.pendingLevels;
        } else {
            this.pendingLevels = this.loadLevels();
        }

        this.addEntry(this.loadingHeader);
        this.handleNewLevels(this.pollLevelsIgnoreErrors());
    }

    @Override
    public void clearEntries() {
        this.children().forEach(WorldSelectionList.Entry::close);
        super.clearEntries();
    }

    @Nullable
    private List<LevelSummary> pollLevelsIgnoreErrors() {
        try {
            List<LevelSummary> list = this.pendingLevels.getNow(null);
            if (this.entryType == WorldSelectionList.EntryType.UPLOAD_WORLD) {
                if (list == null || this.hasPolled) {
                    return null;
                }

                this.hasPolled = true;
                list = list.stream().filter(LevelSummary::canUpload).toList();
            }

            return list;
        } catch (CancellationException | CompletionException completionexception) {
            return null;
        }
    }

    public void reloadWorldList() {
        this.pendingLevels = this.loadLevels();
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        List<LevelSummary> list = this.pollLevelsIgnoreErrors();
        if (list != this.currentlyDisplayedLevels) {
            this.handleNewLevels(list);
        }

        super.renderWidget(guiGraphics, mouseX, mouseY, partialTick);
    }

    private void handleNewLevels(@Nullable List<LevelSummary> levels) {
        if (levels != null) {
            if (levels.isEmpty()) {
                switch (this.entryType) {
                    case SINGLEPLAYER:
                        CreateWorldScreen.openFresh(this.minecraft, () -> this.minecraft.setScreen(null));
                        break;
                    case UPLOAD_WORLD:
                        this.clearEntries();
                        this.addEntry(new WorldSelectionList.NoWorldsEntry(Component.translatable("mco.upload.select.world.none"), this.screen.getFont()));
                }
            } else {
                this.fillLevels(this.filter, levels);
                this.currentlyDisplayedLevels = levels;
            }
        }
    }

    public void updateFilter(String filter) {
        if (this.currentlyDisplayedLevels != null && !filter.equals(this.filter)) {
            this.fillLevels(filter, this.currentlyDisplayedLevels);
        }

        this.filter = filter;
    }

    private CompletableFuture<List<LevelSummary>> loadLevels() {
        LevelStorageSource.LevelCandidates levelstoragesource$levelcandidates;
        try {
            levelstoragesource$levelcandidates = this.minecraft.getLevelSource().findLevelCandidates();
        } catch (LevelStorageException levelstorageexception) {
            LOGGER.error("Couldn't load level list", (Throwable)levelstorageexception);
            this.handleLevelLoadFailure(levelstorageexception.getMessageComponent());
            return CompletableFuture.completedFuture(List.of());
        }

        return this.minecraft.getLevelSource().loadLevelSummaries(levelstoragesource$levelcandidates).exceptionally(p_233202_ -> {
            this.minecraft.delayCrash(CrashReport.forThrowable(p_233202_, "Couldn't load level list"));
            return List.of();
        });
    }

    private void fillLevels(String filter, List<LevelSummary> levels) {
        List<WorldSelectionList.Entry> list = new ArrayList<>();
        Optional<WorldSelectionList.WorldListEntry> optional = this.getSelectedOpt();
        WorldSelectionList.WorldListEntry worldselectionlist$worldlistentry = null;

        for (LevelSummary levelsummary : levels.stream().filter(p_438758_ -> this.filterAccepts(filter.toLowerCase(Locale.ROOT), p_438758_)).toList()) {
            WorldSelectionList.WorldListEntry worldselectionlist$worldlistentry1 = new WorldSelectionList.WorldListEntry(this, levelsummary);
            if (optional.isPresent() && optional.get().getLevelSummary().getLevelId().equals(worldselectionlist$worldlistentry1.getLevelSummary().getLevelId())
                )
             {
                worldselectionlist$worldlistentry = worldselectionlist$worldlistentry1;
            }

            list.add(worldselectionlist$worldlistentry1);
        }

        this.removeEntries(this.children().stream().filter(p_438755_ -> !list.contains(p_438755_)).toList());
        list.forEach(p_438756_ -> {
            if (!this.children().contains(p_438756_)) {
                this.addEntry(p_438756_);
            }
        });
        this.setSelected((WorldSelectionList.Entry)worldselectionlist$worldlistentry);
        this.notifyListUpdated();
    }

    private boolean filterAccepts(String filter, LevelSummary level) {
        return level.getLevelName().toLowerCase(Locale.ROOT).contains(filter) || level.getLevelId().toLowerCase(Locale.ROOT).contains(filter);
    }

    private void notifyListUpdated() {
        this.refreshScrollAmount();
        this.screen.triggerImmediateNarration(true);
    }

    private void handleLevelLoadFailure(Component exceptionMessage) {
        this.minecraft.setScreen(new ErrorScreen(Component.translatable("selectWorld.unable_to_load"), exceptionMessage));
    }

    @Override
    public int getRowWidth() {
        return 270;
    }

    public void setSelected(@Nullable WorldSelectionList.Entry selected) {
        super.setSelected(selected);
        if (this.onEntrySelect != null) {
            this.onEntrySelect
                .accept(
                    selected instanceof WorldSelectionList.WorldListEntry worldselectionlist$worldlistentry ? worldselectionlist$worldlistentry.summary : null
                );
        }
    }

    public Optional<WorldSelectionList.WorldListEntry> getSelectedOpt() {
        WorldSelectionList.Entry worldselectionlist$entry = this.getSelected();
        return worldselectionlist$entry instanceof WorldSelectionList.WorldListEntry worldselectionlist$worldlistentry
            ? Optional.of(worldselectionlist$worldlistentry)
            : Optional.empty();
    }

    public void returnToScreen() {
        this.reloadWorldList();
        this.minecraft.setScreen(this.screen);
    }

    public Screen getScreen() {
        return this.screen;
    }

    @Override
    public void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        if (this.children().contains(this.loadingHeader)) {
            this.loadingHeader.updateNarration(narrationElementOutput);
        } else {
            super.updateWidgetNarration(narrationElementOutput);
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class Builder {
        private final Minecraft minecraft;
        private final Screen screen;
        private int width;
        private int height;
        private String filter = "";
        private WorldSelectionList.EntryType type = WorldSelectionList.EntryType.SINGLEPLAYER;
        @Nullable
        private WorldSelectionList oldList = null;
        @Nullable
        private Consumer<LevelSummary> onEntrySelect = null;
        @Nullable
        private Consumer<WorldSelectionList.WorldListEntry> onEntryInteract = null;

        public Builder(Minecraft minecraft, Screen screen) {
            this.minecraft = minecraft;
            this.screen = screen;
        }

        public WorldSelectionList.Builder width(int width) {
            this.width = width;
            return this;
        }

        public WorldSelectionList.Builder height(int height) {
            this.height = height;
            return this;
        }

        public WorldSelectionList.Builder filter(String filter) {
            this.filter = filter;
            return this;
        }

        public WorldSelectionList.Builder oldList(@Nullable WorldSelectionList oldList) {
            this.oldList = oldList;
            return this;
        }

        public WorldSelectionList.Builder onEntrySelect(Consumer<LevelSummary> onEntrySelect) {
            this.onEntrySelect = onEntrySelect;
            return this;
        }

        public WorldSelectionList.Builder onEntryInteract(Consumer<WorldSelectionList.WorldListEntry> onEntryInteract) {
            this.onEntryInteract = onEntryInteract;
            return this;
        }

        public WorldSelectionList.Builder uploadWorld() {
            this.type = WorldSelectionList.EntryType.UPLOAD_WORLD;
            return this;
        }

        public WorldSelectionList build() {
            return new WorldSelectionList(
                this.screen, this.minecraft, this.width, this.height, this.filter, this.oldList, this.onEntrySelect, this.onEntryInteract, this.type
            );
        }
    }

    @OnlyIn(Dist.CLIENT)
    public abstract static class Entry extends ObjectSelectionList.Entry<WorldSelectionList.Entry> implements AutoCloseable {
        @Override
        public void close() {
        }

        @Nullable
        public LevelSummary getLevelSummary() {
            return null;
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static enum EntryType {
        SINGLEPLAYER,
        UPLOAD_WORLD;
    }

    @OnlyIn(Dist.CLIENT)
    public static class LoadingHeader extends WorldSelectionList.Entry {
        private static final Component LOADING_LABEL = Component.translatable("selectWorld.loading_list");
        private final Minecraft minecraft;

        public LoadingHeader(Minecraft minecraft) {
            this.minecraft = minecraft;
        }

        @Override
        public void renderContent(GuiGraphics guiGraphics, int mouseX, int mouseY, boolean isHovering, float partialTick) {
            int i = (this.minecraft.screen.width - this.minecraft.font.width(LOADING_LABEL)) / 2;
            int j = this.getContentY() + (this.getContentHeight() - 9) / 2;
            guiGraphics.drawString(this.minecraft.font, LOADING_LABEL, i, j, -1);
            String s = LoadingDotsText.get(Util.getMillis());
            int k = (this.minecraft.screen.width - this.minecraft.font.width(s)) / 2;
            int l = j + 9;
            guiGraphics.drawString(this.minecraft.font, s, k, l, -8355712);
        }

        @Override
        public Component getNarration() {
            return LOADING_LABEL;
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static final class NoWorldsEntry extends WorldSelectionList.Entry {
        private final StringWidget stringWidget;

        public NoWorldsEntry(Component message, Font font) {
            this.stringWidget = new StringWidget(message, font);
        }

        @Override
        public Component getNarration() {
            return this.stringWidget.getMessage();
        }

        @Override
        public void renderContent(GuiGraphics guiGraphics, int mouseX, int mouseY, boolean isHovering, float partialTick) {
            this.stringWidget
                .setPosition(this.getContentXMiddle() - this.stringWidget.getWidth() / 2, this.getContentYMiddle() - this.stringWidget.getHeight() / 2);
            this.stringWidget.render(guiGraphics, mouseX, mouseY, partialTick);
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static final class WorldListEntry extends WorldSelectionList.Entry {
        private static final int ICON_WIDTH = 32;
        private static final int ICON_HEIGHT = 32;
        private final WorldSelectionList list;
        private final Minecraft minecraft;
        private final Screen screen;
        final LevelSummary summary;
        private final FaviconTexture icon;
        private final StringWidget worldNameText;
        private final StringWidget idAndLastPlayedText;
        private final StringWidget infoText;
        @Nullable
        private Path iconFile;

        public WorldListEntry(WorldSelectionList list, LevelSummary summary) {
            this.list = list;
            this.minecraft = list.minecraft;
            this.screen = list.getScreen();
            this.summary = summary;
            this.icon = FaviconTexture.forWorld(this.minecraft.getTextureManager(), summary.getLevelId());
            this.iconFile = summary.getIcon();
            int i = list.getRowWidth() - this.getTextX() - 2;
            Component component = Component.literal(summary.getLevelName());
            this.worldNameText = new StringWidget(component, this.minecraft.font);
            this.worldNameText.setMaxWidth(i);
            if (this.minecraft.font.width(component) > i) {
                this.worldNameText.setTooltip(Tooltip.create(component));
            }

            String s = summary.getLevelId();
            long j = summary.getLastPlayed();
            if (j != -1L) {
                s = s + " (" + WorldSelectionList.DATE_FORMAT.format(Instant.ofEpochMilli(j)) + ")";
            }

            Component component1 = Component.literal(s);
            this.idAndLastPlayedText = new StringWidget(component1, this.minecraft.font).setColor(-8355712);
            this.idAndLastPlayedText.setMaxWidth(i);
            if (this.minecraft.font.width(s) > i) {
                this.idAndLastPlayedText.setTooltip(Tooltip.create(component1));
            }

            Component component2 = summary.getInfo();
            this.infoText = new StringWidget(component2, this.minecraft.font).setColor(-8355712);
            this.infoText.setMaxWidth(i);
            if (this.minecraft.font.width(component2) > i) {
                this.infoText.setTooltip(Tooltip.create(component2));
            }

            this.validateIconFile();
            this.loadIcon();
        }

        private void validateIconFile() {
            if (this.iconFile != null) {
                try {
                    BasicFileAttributes basicfileattributes = Files.readAttributes(this.iconFile, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
                    if (basicfileattributes.isSymbolicLink()) {
                        List<ForbiddenSymlinkInfo> list = this.minecraft.directoryValidator().validateSymlink(this.iconFile);
                        if (!list.isEmpty()) {
                            WorldSelectionList.LOGGER.warn("{}", ContentValidationException.getMessage(this.iconFile, list));
                            this.iconFile = null;
                        } else {
                            basicfileattributes = Files.readAttributes(this.iconFile, BasicFileAttributes.class);
                        }
                    }

                    if (!basicfileattributes.isRegularFile()) {
                        this.iconFile = null;
                    }
                } catch (NoSuchFileException nosuchfileexception) {
                    this.iconFile = null;
                } catch (IOException ioexception) {
                    WorldSelectionList.LOGGER.error("could not validate symlink", (Throwable)ioexception);
                    this.iconFile = null;
                }
            }
        }

        @Override
        public Component getNarration() {
            Component component = Component.translatable(
                "narrator.select.world_info",
                this.summary.getLevelName(),
                Component.translationArg(new Date(this.summary.getLastPlayed())),
                this.summary.getInfo()
            );
            if (this.summary.isLocked()) {
                component = CommonComponents.joinForNarration(component, WorldSelectionList.WORLD_LOCKED_TOOLTIP);
            }

            if (this.summary.isExperimental()) {
                component = CommonComponents.joinForNarration(component, WorldSelectionList.WORLD_EXPERIMENTAL);
            }

            return Component.translatable("narrator.select", component);
        }

        @Override
        public void renderContent(GuiGraphics guiGraphics, int mouseX, int mouseY, boolean isHovering, float partialTick) {
            int i = this.getTextX();
            this.worldNameText.setPosition(i, this.getContentY() + 1);
            this.worldNameText.render(guiGraphics, mouseX, mouseY, partialTick);
            this.idAndLastPlayedText.setPosition(i, this.getContentY() + 9 + 3);
            this.idAndLastPlayedText.render(guiGraphics, mouseX, mouseY, partialTick);
            this.infoText.setPosition(i, this.getContentY() + 9 + 9 + 3);
            this.infoText.render(guiGraphics, mouseX, mouseY, partialTick);
            guiGraphics.blit(RenderPipelines.GUI_TEXTURED, this.icon.textureLocation(), this.getContentX(), this.getContentY(), 0.0F, 0.0F, 32, 32, 32, 32);
            renderExperimentalWarning(guiGraphics, mouseX, mouseY, isHovering);
            if (this.list.entryType == WorldSelectionList.EntryType.SINGLEPLAYER && (this.minecraft.options.touchscreen().get() || isHovering)) {
                guiGraphics.fill(this.getContentX(), this.getContentY(), this.getContentX() + 32, this.getContentY() + 32, -1601138544);
                int j = mouseX - this.getContentX();
                boolean flag = j < 32;
                ResourceLocation resourcelocation = flag ? WorldSelectionList.JOIN_HIGHLIGHTED_SPRITE : WorldSelectionList.JOIN_SPRITE;
                ResourceLocation resourcelocation1 = flag ? WorldSelectionList.WARNING_HIGHLIGHTED_SPRITE : WorldSelectionList.WARNING_SPRITE;
                ResourceLocation resourcelocation2 = flag ? WorldSelectionList.ERROR_HIGHLIGHTED_SPRITE : WorldSelectionList.ERROR_SPRITE;
                ResourceLocation resourcelocation3 = flag ? WorldSelectionList.MARKED_JOIN_HIGHLIGHTED_SPRITE : WorldSelectionList.MARKED_JOIN_SPRITE;
                if (this.summary instanceof LevelSummary.SymlinkLevelSummary || this.summary instanceof LevelSummary.CorruptedLevelSummary) {
                    guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, resourcelocation2, this.getContentX(), this.getContentY(), 32, 32);
                    guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, resourcelocation3, this.getContentX(), this.getContentY(), 32, 32);
                    return;
                }

                if (this.summary.isLocked()) {
                    guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, resourcelocation2, this.getContentX(), this.getContentY(), 32, 32);
                    if (flag) {
                        guiGraphics.setTooltipForNextFrame(this.minecraft.font.split(WorldSelectionList.WORLD_LOCKED_TOOLTIP, 175), mouseX, mouseY);
                    }
                } else if (this.summary.requiresManualConversion()) {
                    guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, resourcelocation2, this.getContentX(), this.getContentY(), 32, 32);
                    if (flag) {
                        guiGraphics.setTooltipForNextFrame(this.minecraft.font.split(WorldSelectionList.WORLD_REQUIRES_CONVERSION, 175), mouseX, mouseY);
                    }
                } else if (!this.summary.isCompatible()) {
                    guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, resourcelocation2, this.getContentX(), this.getContentY(), 32, 32);
                    if (flag) {
                        guiGraphics.setTooltipForNextFrame(this.minecraft.font.split(WorldSelectionList.INCOMPATIBLE_VERSION_TOOLTIP, 175), mouseX, mouseY);
                    }
                } else if (this.summary.shouldBackup()) {
                    guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, resourcelocation3, this.getContentX(), this.getContentY(), 32, 32);
                    if (this.summary.isDowngrade()) {
                        guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, resourcelocation2, this.getContentX(), this.getContentY(), 32, 32);
                        if (flag) {
                            guiGraphics.setTooltipForNextFrame(
                                ImmutableList.of(
                                    WorldSelectionList.FROM_NEWER_TOOLTIP_1.getVisualOrderText(), WorldSelectionList.FROM_NEWER_TOOLTIP_2.getVisualOrderText()
                                ),
                                mouseX,
                                mouseY
                            );
                        }
                    } else if (!SharedConstants.getCurrentVersion().stable()) {
                        guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, resourcelocation1, this.getContentX(), this.getContentY(), 32, 32);
                        if (flag) {
                            guiGraphics.setTooltipForNextFrame(
                                ImmutableList.of(
                                    WorldSelectionList.SNAPSHOT_TOOLTIP_1.getVisualOrderText(), WorldSelectionList.SNAPSHOT_TOOLTIP_2.getVisualOrderText()
                                ),
                                mouseX,
                                mouseY
                            );
                        }
                    }
                } else {
                    guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, resourcelocation, this.getContentX(), this.getContentY(), 32, 32);
                }
            }
        }

        private int getTextX() {
            return this.getContentX() + 32 + 3;
        }

        @Override
        public boolean mouseClicked(MouseButtonEvent event, boolean isDoubleClick) {
            if (this.canInteract()
                && (isDoubleClick || event.x() - this.list.getRowLeft() <= 32.0 && this.list.entryType == WorldSelectionList.EntryType.SINGLEPLAYER)) {
                this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                Consumer<WorldSelectionList.WorldListEntry> consumer = this.list.onEntryInteract;
                if (consumer != null) {
                    consumer.accept(this);
                    return true;
                }
            }

            return super.mouseClicked(event, isDoubleClick);
        }

        @Override
        public boolean keyPressed(KeyEvent event) {
            if (event.isSelection() && this.canInteract()) {
                this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                Consumer<WorldSelectionList.WorldListEntry> consumer = this.list.onEntryInteract;
                if (consumer != null) {
                    consumer.accept(this);
                    return true;
                }
            }

            return super.keyPressed(event);
        }

        public boolean canInteract() {
            return this.summary.primaryActionActive() || this.list.entryType == WorldSelectionList.EntryType.UPLOAD_WORLD;
        }

        public void joinWorld() {
            if (this.summary.primaryActionActive()) {
                if (this.summary instanceof LevelSummary.SymlinkLevelSummary) {
                    this.minecraft.setScreen(NoticeWithLinkScreen.createWorldSymlinkWarningScreen(() -> this.minecraft.setScreen(this.screen)));
                } else {
                    this.minecraft.createWorldOpenFlows().openWorld(this.summary.getLevelId(), this.list::returnToScreen);
                }
            }
        }

        public void deleteWorld() {
            this.minecraft
                .setScreen(
                    new ConfirmScreen(
                        p_438759_ -> {
                            if (p_438759_) {
                                this.minecraft.setScreen(new ProgressScreen(true));
                                this.doDeleteWorld();
                            }

                            this.list.returnToScreen();
                        },
                        Component.translatable("selectWorld.deleteQuestion"),
                        Component.translatable("selectWorld.deleteWarning", this.summary.getLevelName()),
                        Component.translatable("selectWorld.deleteButton"),
                        CommonComponents.GUI_CANCEL
                    )
                );
        }

        public void doDeleteWorld() {
            LevelStorageSource levelstoragesource = this.minecraft.getLevelSource();
            String s = this.summary.getLevelId();

            try (LevelStorageSource.LevelStorageAccess levelstoragesource$levelstorageaccess = levelstoragesource.createAccess(s)) {
                levelstoragesource$levelstorageaccess.deleteLevel();
            } catch (IOException ioexception) {
                SystemToast.onWorldDeleteFailure(this.minecraft, s);
                WorldSelectionList.LOGGER.error("Failed to delete world {}", s, ioexception);
            }
        }

        public void editWorld() {
            this.queueLoadScreen();
            String s = this.summary.getLevelId();

            LevelStorageSource.LevelStorageAccess levelstoragesource$levelstorageaccess;
            try {
                levelstoragesource$levelstorageaccess = this.minecraft.getLevelSource().validateAndCreateAccess(s);
            } catch (IOException ioexception1) {
                SystemToast.onWorldAccessFailure(this.minecraft, s);
                WorldSelectionList.LOGGER.error("Failed to access level {}", s, ioexception1);
                this.list.reloadWorldList();
                return;
            } catch (ContentValidationException contentvalidationexception) {
                WorldSelectionList.LOGGER.warn("{}", contentvalidationexception.getMessage());
                this.minecraft.setScreen(NoticeWithLinkScreen.createWorldSymlinkWarningScreen(() -> this.minecraft.setScreen(this.screen)));
                return;
            }

            EditWorldScreen editworldscreen;
            try {
                editworldscreen = EditWorldScreen.create(this.minecraft, levelstoragesource$levelstorageaccess, p_438765_ -> {
                    levelstoragesource$levelstorageaccess.safeClose();
                    this.list.returnToScreen();
                });
            } catch (NbtException | ReportedNbtException | IOException ioexception) {
                levelstoragesource$levelstorageaccess.safeClose();
                SystemToast.onWorldAccessFailure(this.minecraft, s);
                WorldSelectionList.LOGGER.error("Failed to load world data {}", s, ioexception);
                this.list.reloadWorldList();
                return;
            }

            this.minecraft.setScreen(editworldscreen);
        }

        public void recreateWorld() {
            this.queueLoadScreen();

            try (LevelStorageSource.LevelStorageAccess levelstoragesource$levelstorageaccess = this.minecraft
                    .getLevelSource()
                    .validateAndCreateAccess(this.summary.getLevelId())) {
                Pair<LevelSettings, WorldCreationContext> pair = this.minecraft.createWorldOpenFlows().recreateWorldData(levelstoragesource$levelstorageaccess);
                LevelSettings levelsettings = pair.getFirst();
                WorldCreationContext worldcreationcontext = pair.getSecond();
                Path path = CreateWorldScreen.createTempDataPackDirFromExistingWorld(
                    levelstoragesource$levelstorageaccess.getLevelPath(LevelResource.DATAPACK_DIR), this.minecraft
                );
                worldcreationcontext.validate();
                if (worldcreationcontext.options().isOldCustomizedWorld()) {
                    this.minecraft
                        .setScreen(
                            new ConfirmScreen(
                                p_438763_ -> this.minecraft
                                    .setScreen(
                                        (Screen)(p_438763_
                                            ? CreateWorldScreen.createFromExisting(
                                                this.minecraft, this.list::returnToScreen, levelsettings, worldcreationcontext, path
                                            )
                                            : this.screen)
                                    ),
                                Component.translatable("selectWorld.recreate.customized.title"),
                                Component.translatable("selectWorld.recreate.customized.text"),
                                CommonComponents.GUI_PROCEED,
                                CommonComponents.GUI_CANCEL
                            )
                        );
                } else {
                    this.minecraft
                        .setScreen(CreateWorldScreen.createFromExisting(this.minecraft, this.list::returnToScreen, levelsettings, worldcreationcontext, path));
                }
            } catch (ContentValidationException contentvalidationexception) {
                WorldSelectionList.LOGGER.warn("{}", contentvalidationexception.getMessage());
                this.minecraft.setScreen(NoticeWithLinkScreen.createWorldSymlinkWarningScreen(() -> this.minecraft.setScreen(this.screen)));
            } catch (Exception exception) {
                WorldSelectionList.LOGGER.error("Unable to recreate world", (Throwable)exception);
                this.minecraft
                    .setScreen(
                        new AlertScreen(
                            () -> this.minecraft.setScreen(this.screen),
                            Component.translatable("selectWorld.recreate.error.title"),
                            Component.translatable("selectWorld.recreate.error.text")
                        )
                    );
            }
        }

        private void queueLoadScreen() {
            this.minecraft.setScreenAndShow(new GenericMessageScreen(Component.translatable("selectWorld.data_read")));
        }

        private void loadIcon() {
            boolean flag = this.iconFile != null && Files.isRegularFile(this.iconFile);
            if (flag) {
                try (InputStream inputstream = Files.newInputStream(this.iconFile)) {
                    this.icon.upload(NativeImage.read(inputstream));
                } catch (Throwable throwable) {
                    WorldSelectionList.LOGGER.error("Invalid icon for world {}", this.summary.getLevelId(), throwable);
                    this.iconFile = null;
                }
            } else {
                this.icon.clear();
            }
        }

        @Override
        public void close() {
            if (!this.icon.isClosed()) {
                this.icon.close();
            }
        }

        public String getLevelName() {
            return this.summary.getLevelName();
        }

        @Override
        public LevelSummary getLevelSummary() {
            return this.summary;
        }

        // FORGE: Patch in experimental warning icon for worlds in the world selection screen
        private void renderExperimentalWarning(GuiGraphics guiGraphics, int mouseX, int mouseY, boolean hovered) {
            if (this.summary.getSettings() != null && this.summary.getSettings().getLifecycle().equals(com.mojang.serialization.Lifecycle.experimental())) {
                int leftStart = this.getContentX() + this.getContentWidth();
                guiGraphics.blit(RenderPipelines.GUI_TEXTURED, WorldSelectionList.FORGE_EXPERIMENTAL_WARNING_ICON, leftStart - 36, this.getContentY(), 0.0F, 0.0F, 32, 32, 32, 32);
                if (hovered && mouseX > leftStart - 36 && mouseX < leftStart) {
                    var font = Minecraft.getInstance().font;
                    List<net.minecraft.util.FormattedCharSequence> tooltip = font.split(Component.translatable("neoforge.experimentalsettings.tooltip"), 200);
                    guiGraphics.setTooltipForNextFrame(font, tooltip, mouseX, mouseY);
                }
            }
        }
    }
}
