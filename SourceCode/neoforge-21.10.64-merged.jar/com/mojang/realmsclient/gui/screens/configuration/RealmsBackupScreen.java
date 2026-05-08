package com.mojang.realmsclient.gui.screens.configuration;

import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.client.RealmsClient;
import com.mojang.realmsclient.dto.Backup;
import com.mojang.realmsclient.dto.RealmsServer;
import com.mojang.realmsclient.exception.RealmsServiceException;
import com.mojang.realmsclient.gui.screens.RealmsLongRunningMcoTaskScreen;
import com.mojang.realmsclient.gui.screens.RealmsPopups;
import com.mojang.realmsclient.util.RealmsUtil;
import com.mojang.realmsclient.util.task.DownloadTask;
import com.mojang.realmsclient.util.task.RestoreTask;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.PopupScreen;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.realms.RealmsScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class RealmsBackupScreen extends RealmsScreen {
    static final Logger LOGGER = LogUtils.getLogger();
    private static final Component TITLE = Component.translatable("mco.configure.world.backup");
    static final Component RESTORE_TOOLTIP = Component.translatable("mco.backup.button.restore");
    static final Component HAS_CHANGES_TOOLTIP = Component.translatable("mco.backup.changes.tooltip");
    private static final Component NO_BACKUPS_LABEL = Component.translatable("mco.backup.nobackups");
    private static final Component DOWNLOAD_LATEST = Component.translatable("mco.backup.button.download");
    private static final String UPLOADED_KEY = "uploaded";
    private static final int PADDING = 8;
    final RealmsConfigureWorldScreen lastScreen;
    List<Backup> backups = Collections.emptyList();
    @Nullable
    RealmsBackupScreen.BackupObjectSelectionList backupList;
    final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);
    private final int slotId;
    @Nullable
    Button downloadButton;
    final RealmsServer serverData;
    boolean noBackups = false;

    public RealmsBackupScreen(RealmsConfigureWorldScreen lastScreen, RealmsServer serverData, int slotId) {
        super(TITLE);
        this.lastScreen = lastScreen;
        this.serverData = serverData;
        this.slotId = slotId;
    }

    @Override
    public void init() {
        this.layout.addTitleHeader(TITLE, this.font);
        this.backupList = this.layout.addToContents(new RealmsBackupScreen.BackupObjectSelectionList());
        LinearLayout linearlayout = this.layout.addToFooter(LinearLayout.horizontal().spacing(8));
        this.downloadButton = linearlayout.addChild(Button.builder(DOWNLOAD_LATEST, p_419575_ -> this.downloadClicked()).build());
        this.downloadButton.active = false;
        linearlayout.addChild(Button.builder(CommonComponents.GUI_BACK, p_419954_ -> this.onClose()).build());
        this.layout.visitWidgets(p_419733_ -> {
            AbstractWidget abstractwidget = this.addRenderableWidget(p_419733_);
        });
        this.repositionElements();
        this.fetchRealmsBackups();
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
        if (this.noBackups && this.backupList != null) {
            guiGraphics.drawString(
                this.font,
                NO_BACKUPS_LABEL,
                this.width / 2 - this.font.width(NO_BACKUPS_LABEL) / 2,
                this.backupList.getY() + this.backupList.getHeight() / 2 - 9 / 2,
                -1
            );
        }
    }

    @Override
    protected void repositionElements() {
        this.layout.arrangeElements();
        if (this.backupList != null) {
            this.backupList.updateSize(this.width, this.layout);
        }
    }

    private void fetchRealmsBackups() {
        (new Thread("Realms-fetch-backups") {
                @Override
                public void run() {
                    RealmsClient realmsclient = RealmsClient.getOrCreate();

                    try {
                        List<Backup> list = realmsclient.backupsFor(RealmsBackupScreen.this.serverData.id).backups;
                        RealmsBackupScreen.this.minecraft
                            .execute(
                                () -> {
                                    RealmsBackupScreen.this.backups = list;
                                    RealmsBackupScreen.this.noBackups = RealmsBackupScreen.this.backups.isEmpty();
                                    if (!RealmsBackupScreen.this.noBackups && RealmsBackupScreen.this.downloadButton != null) {
                                        RealmsBackupScreen.this.downloadButton.active = true;
                                    }

                                    if (RealmsBackupScreen.this.backupList != null) {
                                        RealmsBackupScreen.this.backupList
                                            .replaceEntries(
                                                RealmsBackupScreen.this.backups
                                                    .stream()
                                                    .map(p_419757_ -> RealmsBackupScreen.this.new Entry(p_419757_))
                                                    .toList()
                                            );
                                    }
                                }
                            );
                    } catch (RealmsServiceException realmsserviceexception) {
                        RealmsBackupScreen.LOGGER.error("Couldn't request backups", (Throwable)realmsserviceexception);
                    }
                }
            })
            .start();
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.lastScreen);
    }

    private void downloadClicked() {
        this.minecraft
            .setScreen(
                RealmsPopups.infoPopupScreen(
                    this,
                    Component.translatable("mco.configure.world.restore.download.question.line1"),
                    p_419482_ -> this.minecraft
                        .setScreen(
                            new RealmsLongRunningMcoTaskScreen(
                                this.lastScreen.getNewScreen(),
                                new DownloadTask(
                                    this.serverData.id,
                                    this.slotId,
                                    Objects.requireNonNullElse(this.serverData.name, "")
                                        + " ("
                                        + this.serverData.slots.get(this.serverData.activeSlot).options.getSlotName(this.serverData.activeSlot)
                                        + ")",
                                    this
                                )
                            )
                        )
                )
            );
    }

    @OnlyIn(Dist.CLIENT)
    class BackupObjectSelectionList extends ContainerObjectSelectionList<RealmsBackupScreen.Entry> {
        private static final int ITEM_HEIGHT = 36;

        public BackupObjectSelectionList() {
            super(
                Minecraft.getInstance(),
                RealmsBackupScreen.this.width,
                RealmsBackupScreen.this.layout.getContentHeight(),
                RealmsBackupScreen.this.layout.getHeaderHeight(),
                36
            );
        }

        @Override
        public int getRowWidth() {
            return 300;
        }
    }

    @OnlyIn(Dist.CLIENT)
    class Entry extends ContainerObjectSelectionList.Entry<RealmsBackupScreen.Entry> {
        private static final int Y_PADDING = 2;
        private final Backup backup;
        @Nullable
        private Button restoreButton;
        @Nullable
        private Button changesButton;
        private final List<AbstractWidget> children = new ArrayList<>();

        public Entry(Backup backup) {
            this.backup = backup;
            this.populateChangeList(backup);
            if (!backup.changeList.isEmpty()) {
                this.changesButton = Button.builder(
                        RealmsBackupScreen.HAS_CHANGES_TOOLTIP,
                        p_419478_ -> RealmsBackupScreen.this.minecraft.setScreen(new RealmsBackupInfoScreen(RealmsBackupScreen.this, this.backup))
                    )
                    .width(8 + RealmsBackupScreen.this.font.width(RealmsBackupScreen.HAS_CHANGES_TOOLTIP))
                    .createNarration(
                        p_419597_ -> CommonComponents.joinForNarration(
                            Component.translatable("mco.backup.narration", this.getShortBackupDate()), p_419597_.get()
                        )
                    )
                    .build();
                this.children.add(this.changesButton);
            }

            if (!RealmsBackupScreen.this.serverData.expired) {
                this.restoreButton = Button.builder(RealmsBackupScreen.RESTORE_TOOLTIP, p_419643_ -> this.restoreClicked())
                    .width(8 + RealmsBackupScreen.this.font.width(RealmsBackupScreen.HAS_CHANGES_TOOLTIP))
                    .createNarration(
                        p_419587_ -> CommonComponents.joinForNarration(
                            Component.translatable("mco.backup.narration", this.getShortBackupDate()), p_419587_.get()
                        )
                    )
                    .build();
                this.children.add(this.restoreButton);
            }
        }

        private void populateChangeList(Backup p_backup) {
            int i = RealmsBackupScreen.this.backups.indexOf(p_backup);
            if (i != RealmsBackupScreen.this.backups.size() - 1) {
                Backup backup = RealmsBackupScreen.this.backups.get(i + 1);

                for (String s : p_backup.metadata.keySet()) {
                    if (!s.contains("uploaded") && backup.metadata.containsKey(s)) {
                        if (!p_backup.metadata.get(s).equals(backup.metadata.get(s))) {
                            this.addToChangeList(s);
                        }
                    } else {
                        this.addToChangeList(s);
                    }
                }
            }
        }

        private void addToChangeList(String key) {
            if (key.contains("uploaded")) {
                String s = DateFormat.getDateTimeInstance(3, 3).format(this.backup.lastModifiedDate);
                this.backup.changeList.put(key, s);
                this.backup.setUploadedVersion(true);
            } else {
                this.backup.changeList.put(key, this.backup.metadata.get(key));
            }
        }

        private String getShortBackupDate() {
            return DateFormat.getDateTimeInstance(3, 3).format(this.backup.lastModifiedDate);
        }

        private void restoreClicked() {
            Component component = RealmsUtil.convertToAgePresentationFromInstant(this.backup.lastModifiedDate);
            Component component1 = Component.translatable("mco.configure.world.restore.question.line1", this.getShortBackupDate(), component);
            RealmsBackupScreen.this.minecraft
                .setScreen(
                    RealmsPopups.warningPopupScreen(
                        RealmsBackupScreen.this,
                        component1,
                        p_428647_ -> {
                            RealmsConfigureWorldScreen realmsconfigureworldscreen = RealmsBackupScreen.this.lastScreen.getNewScreen();
                            RealmsBackupScreen.this.minecraft
                                .setScreen(
                                    new RealmsLongRunningMcoTaskScreen(
                                        realmsconfigureworldscreen,
                                        new RestoreTask(this.backup, RealmsBackupScreen.this.serverData.id, realmsconfigureworldscreen)
                                    )
                                );
                        }
                    )
                );
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
            int i = this.getContentYMiddle();
            int j = i - 9 - 2;
            int k = i + 2;
            int l = this.backup.isUploadedVersion() ? -8388737 : -1;
            guiGraphics.drawString(
                RealmsBackupScreen.this.font,
                Component.translatable("mco.backup.entry", RealmsUtil.convertToAgePresentationFromInstant(this.backup.lastModifiedDate)),
                this.getContentX(),
                j,
                l
            );
            guiGraphics.drawString(RealmsBackupScreen.this.font, this.getMediumDatePresentation(this.backup.lastModifiedDate), this.getContentX(), k, -11776948);
            int i1 = 0;
            int j1 = this.getContentYMiddle() - 10;
            if (this.restoreButton != null) {
                i1 += this.restoreButton.getWidth() + 8;
                this.restoreButton.setX(this.getContentRight() - i1);
                this.restoreButton.setY(j1);
                this.restoreButton.render(guiGraphics, mouseX, mouseY, partialTick);
            }

            if (this.changesButton != null) {
                i1 += this.changesButton.getWidth() + 8;
                this.changesButton.setX(this.getContentRight() - i1);
                this.changesButton.setY(j1);
                this.changesButton.render(guiGraphics, mouseX, mouseY, partialTick);
            }
        }

        private String getMediumDatePresentation(Date date) {
            return DateFormat.getDateTimeInstance(3, 3).format(date);
        }
    }
}
