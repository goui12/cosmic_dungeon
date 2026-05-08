package com.mojang.realmsclient.gui.screens.configuration;

import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.RealmsMainScreen;
import com.mojang.realmsclient.client.RealmsClient;
import com.mojang.realmsclient.client.RealmsError;
import com.mojang.realmsclient.dto.PlayerInfo;
import com.mojang.realmsclient.dto.PreferredRegionsDto;
import com.mojang.realmsclient.dto.RealmsRegion;
import com.mojang.realmsclient.dto.RealmsServer;
import com.mojang.realmsclient.dto.RealmsSlot;
import com.mojang.realmsclient.dto.RegionDataDto;
import com.mojang.realmsclient.dto.RegionSelectionPreference;
import com.mojang.realmsclient.dto.RegionSelectionPreferenceDto;
import com.mojang.realmsclient.dto.ServiceQuality;
import com.mojang.realmsclient.exception.RealmsServiceException;
import com.mojang.realmsclient.gui.screens.RealmsGenericErrorScreen;
import com.mojang.realmsclient.gui.screens.RealmsLongRunningMcoTaskScreen;
import com.mojang.realmsclient.util.RealmsUtil;
import com.mojang.realmsclient.util.task.CloseServerTask;
import com.mojang.realmsclient.util.task.OpenServerTask;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nullable;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.components.tabs.LoadingTab;
import net.minecraft.client.gui.components.tabs.Tab;
import net.minecraft.client.gui.components.tabs.TabManager;
import net.minecraft.client.gui.components.tabs.TabNavigationBar;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.realms.RealmsScreen;
import net.minecraft.util.StringUtil;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class RealmsConfigureWorldScreen extends RealmsScreen {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Component PLAY_TEXT = Component.translatable("mco.selectServer.play");
    private final RealmsMainScreen lastScreen;
    @Nullable
    private RealmsServer serverData;
    @Nullable
    private PreferredRegionsDto regions;
    private final Map<RealmsRegion, ServiceQuality> regionServiceQuality = new LinkedHashMap<>();
    private final long serverId;
    private boolean stateChanged;
    private final TabManager tabManager = new TabManager(p_419553_ -> {
        AbstractWidget abstractwidget = this.addRenderableWidget(p_419553_);
    }, p_419490_ -> this.removeWidget(p_419490_), this::onTabSelected, this::onTabDeselected);
    @Nullable
    private Button playButton;
    @Nullable
    private TabNavigationBar tabNavigationBar;
    final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);

    public RealmsConfigureWorldScreen(RealmsMainScreen lastScreen, long serverId, @Nullable RealmsServer serverData, @Nullable PreferredRegionsDto regions) {
        super(Component.empty());
        this.lastScreen = lastScreen;
        this.serverId = serverId;
        this.serverData = serverData;
        this.regions = regions;
    }

    public RealmsConfigureWorldScreen(RealmsMainScreen lastScreen, long serverId) {
        this(lastScreen, serverId, null, null);
    }

    @Override
    public void init() {
        if (this.serverData == null) {
            this.fetchServerData(this.serverId);
        }

        if (this.regions == null) {
            this.fetchRegionData();
        }

        Component component = Component.translatable("mco.configure.world.loading");
        this.tabNavigationBar = TabNavigationBar.builder(this.tabManager, this.width)
            .addTabs(
                new LoadingTab(this.getFont(), RealmsWorldsTab.TITLE, component),
                new LoadingTab(this.getFont(), RealmsPlayersTab.TITLE, component),
                new LoadingTab(this.getFont(), RealmsSubscriptionTab.TITLE, component),
                new LoadingTab(this.getFont(), RealmsSettingsTab.TITLE, component)
            )
            .build();
        this.addRenderableWidget(this.tabNavigationBar);
        LinearLayout linearlayout = this.layout.addToFooter(LinearLayout.horizontal().spacing(8));
        this.playButton = linearlayout.addChild(Button.builder(PLAY_TEXT, p_419811_ -> {
            this.onClose();
            RealmsMainScreen.play(this.serverData, this);
        }).width(150).build());
        this.playButton.active = false;
        linearlayout.addChild(Button.builder(CommonComponents.GUI_BACK, p_419967_ -> this.onClose()).build());
        this.layout.visitWidgets(p_419870_ -> {
            p_419870_.setTabOrderGroup(1);
            this.addRenderableWidget(p_419870_);
        });
        this.tabNavigationBar.selectTab(0, false);
        this.repositionElements();
        if (this.serverData != null && this.regions != null) {
            this.onRealmsDataFetched();
        }
    }

    private void onTabSelected(Tab tab) {
        if (this.serverData != null && tab instanceof RealmsConfigurationTab realmsconfigurationtab) {
            realmsconfigurationtab.onSelected(this.serverData);
        }
    }

    private void onTabDeselected(Tab tab) {
        if (this.serverData != null && tab instanceof RealmsConfigurationTab realmsconfigurationtab) {
            realmsconfigurationtab.onDeselected(this.serverData);
        }
    }

    public int getContentHeight() {
        return this.layout.getContentHeight();
    }

    public int getHeaderHeight() {
        return this.layout.getHeaderHeight();
    }

    public Screen getLastScreen() {
        return this.lastScreen;
    }

    public Screen createErrorScreen(RealmsServiceException exception) {
        return new RealmsGenericErrorScreen(exception, this.lastScreen);
    }

    @Override
    public void repositionElements() {
        if (this.tabNavigationBar != null) {
            this.tabNavigationBar.setWidth(this.width);
            this.tabNavigationBar.arrangeElements();
            int i = this.tabNavigationBar.getRectangle().bottom();
            ScreenRectangle screenrectangle = new ScreenRectangle(0, i, this.width, this.height - this.layout.getFooterHeight() - i);
            this.tabManager.setTabArea(screenrectangle);
            this.layout.setHeaderHeight(i);
            this.layout.arrangeElements();
        }
    }

    private void updateButtonStates() {
        if (this.serverData != null && this.playButton != null) {
            this.playButton.active = this.serverData.shouldPlayButtonBeActive();
            if (!this.playButton.active && this.serverData.state == RealmsServer.State.CLOSED) {
                this.playButton.setTooltip(Tooltip.create(RealmsServer.WORLD_CLOSED_COMPONENT));
            }
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
        guiGraphics.blit(
            RenderPipelines.GUI_TEXTURED, Screen.FOOTER_SEPARATOR, 0, this.height - this.layout.getFooterHeight() - 2, 0.0F, 0.0F, this.width, 2, 32, 2
        );
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        return this.tabNavigationBar.keyPressed(event) ? true : super.keyPressed(event);
    }

    @Override
    protected void renderMenuBackground(GuiGraphics partialTick) {
        partialTick.blit(
            RenderPipelines.GUI_TEXTURED, CreateWorldScreen.TAB_HEADER_BACKGROUND, 0, 0, 0.0F, 0.0F, this.width, this.layout.getHeaderHeight(), 16, 16
        );
        this.renderMenuBackground(partialTick, 0, this.layout.getHeaderHeight(), this.width, this.height);
    }

    @Override
    public void onClose() {
        if (this.serverData != null && this.tabManager.getCurrentTab() instanceof RealmsConfigurationTab realmsconfigurationtab) {
            realmsconfigurationtab.onDeselected(this.serverData);
        }

        this.minecraft.setScreen(this.lastScreen);
        if (this.stateChanged) {
            this.lastScreen.resetScreen();
        }
    }

    public void fetchRegionData() {
        RealmsUtil.supplyAsync(
                RealmsClient::getPreferredRegionSelections, RealmsUtil.openScreenAndLogOnFailure(this::createErrorScreen, "Couldn't get realms region data")
            )
            .thenAcceptAsync(p_426844_ -> {
                this.regions = p_426844_;
                this.onRealmsDataFetched();
            }, this.minecraft);
    }

    public void fetchServerData(long serverId) {
        RealmsUtil.<RealmsServer>supplyAsync(
                p_428649_ -> p_428649_.getOwnRealm(serverId), RealmsUtil.openScreenAndLogOnFailure(this::createErrorScreen, "Couldn't get own world")
            )
            .thenAcceptAsync(p_419461_ -> {
                this.serverData = p_419461_;
                this.onRealmsDataFetched();
            }, this.minecraft);
    }

    private void onRealmsDataFetched() {
        if (this.serverData != null && this.regions != null) {
            this.regionServiceQuality.clear();

            for (RegionDataDto regiondatadto : this.regions.regionData()) {
                if (regiondatadto.region() != RealmsRegion.INVALID_REGION) {
                    this.regionServiceQuality.put(regiondatadto.region(), regiondatadto.serviceQuality());
                }
            }

            int i = -1;
            if (this.tabNavigationBar != null) {
                i = this.tabNavigationBar.getTabs().indexOf(this.tabManager.getCurrentTab());
            }

            if (this.tabNavigationBar != null) {
                this.removeWidget(this.tabNavigationBar);
            }

            this.tabNavigationBar = this.addRenderableWidget(
                TabNavigationBar.builder(this.tabManager, this.width)
                    .addTabs(
                        new RealmsWorldsTab(this, Objects.requireNonNull(this.minecraft), this.serverData),
                        new RealmsPlayersTab(this, this.minecraft, this.serverData),
                        new RealmsSubscriptionTab(this, this.minecraft, this.serverData),
                        new RealmsSettingsTab(this, this.minecraft, this.serverData, this.regionServiceQuality)
                    )
                    .build()
            );
            this.setFocused(this.tabNavigationBar);
            if (i != -1) {
                this.tabNavigationBar.selectTab(i, false);
            }

            this.tabNavigationBar.setTabActiveState(3, !this.serverData.expired);
            if (this.serverData.expired) {
                this.tabNavigationBar.setTabTooltip(3, Tooltip.create(Component.translatable("mco.configure.world.settings.expired")));
            } else {
                this.tabNavigationBar.setTabTooltip(3, null);
            }

            this.updateButtonStates();
            this.repositionElements();
        }
    }

    public void saveSlotSettings(RealmsSlot slot) {
        RealmsSlot realmsslot = this.serverData.slots.get(this.serverData.activeSlot);
        slot.options.templateId = realmsslot.options.templateId;
        slot.options.templateImage = realmsslot.options.templateImage;
        RealmsClient realmsclient = RealmsClient.getOrCreate();

        try {
            if (this.serverData.activeSlot != slot.slotId) {
                throw new RealmsServiceException(RealmsError.CustomError.configurationError());
            }

            realmsclient.updateSlot(this.serverData.id, slot.slotId, slot.options, slot.settings);
            this.serverData.slots.put(this.serverData.activeSlot, slot);
            if (slot.options.gameMode != realmsslot.options.gameMode || slot.isHardcore() != realmsslot.isHardcore()) {
                RealmsMainScreen.refreshServerList();
            }

            this.stateChanged();
        } catch (RealmsServiceException realmsserviceexception) {
            LOGGER.error("Couldn't save slot settings", (Throwable)realmsserviceexception);
            this.minecraft.setScreen(new RealmsGenericErrorScreen(realmsserviceexception, this));
            return;
        }

        this.minecraft.setScreen(this);
    }

    public void saveSettings(String name, String description, RegionSelectionPreference regionSelectionPreference, @Nullable RealmsRegion preferredRegiom) {
        String s = StringUtil.isBlank(description) ? "" : description;
        String s1 = StringUtil.isBlank(name) ? "" : name;
        RealmsClient realmsclient = RealmsClient.getOrCreate();

        try {
            RealmsSlot realmsslot = this.serverData.slots.get(this.serverData.activeSlot);
            RealmsRegion realmsregion = regionSelectionPreference == RegionSelectionPreference.MANUAL ? preferredRegiom : null;
            RegionSelectionPreferenceDto regionselectionpreferencedto = new RegionSelectionPreferenceDto(regionSelectionPreference, realmsregion);
            realmsclient.updateConfiguration(
                this.serverData.id, s1, s, regionselectionpreferencedto, realmsslot.slotId, realmsslot.options, realmsslot.settings
            );
            this.serverData.regionSelectionPreference = regionselectionpreferencedto;
            this.serverData.name = name;
            this.serverData.motd = s;
            this.stateChanged();
        } catch (RealmsServiceException realmsserviceexception) {
            LOGGER.error("Couldn't save settings", (Throwable)realmsserviceexception);
            this.minecraft.setScreen(new RealmsGenericErrorScreen(realmsserviceexception, this));
            return;
        }

        this.minecraft.setScreen(this);
    }

    public void openTheWorld(boolean join) {
        RealmsConfigureWorldScreen realmsconfigureworldscreen = this.getNewScreenWithKnownData(this.serverData);
        this.minecraft
            .setScreen(
                new RealmsLongRunningMcoTaskScreen(
                    this.getNewScreen(), new OpenServerTask(this.serverData, realmsconfigureworldscreen, join, this.minecraft)
                )
            );
    }

    public void closeTheWorld() {
        RealmsConfigureWorldScreen realmsconfigureworldscreen = this.getNewScreenWithKnownData(this.serverData);
        this.minecraft.setScreen(new RealmsLongRunningMcoTaskScreen(this.getNewScreen(), new CloseServerTask(this.serverData, realmsconfigureworldscreen)));
    }

    public void stateChanged() {
        this.stateChanged = true;
        if (this.tabNavigationBar != null) {
            for (Tab tab : this.tabNavigationBar.getTabs()) {
                if (tab instanceof RealmsConfigurationTab realmsconfigurationtab) {
                    realmsconfigurationtab.updateData(this.serverData);
                }
            }
        }
    }

    public boolean invitePlayer(long worldId, String playerName) {
        RealmsClient realmsclient = RealmsClient.getOrCreate();

        try {
            List<PlayerInfo> list = realmsclient.invite(worldId, playerName);
            if (this.serverData != null) {
                this.serverData.players = list;
            } else {
                this.serverData = realmsclient.getOwnRealm(worldId);
            }

            this.stateChanged();
            return true;
        } catch (RealmsServiceException realmsserviceexception) {
            LOGGER.error("Couldn't invite user", (Throwable)realmsserviceexception);
            return false;
        }
    }

    public RealmsConfigureWorldScreen getNewScreen() {
        RealmsConfigureWorldScreen realmsconfigureworldscreen = new RealmsConfigureWorldScreen(this.lastScreen, this.serverId);
        realmsconfigureworldscreen.stateChanged = this.stateChanged;
        return realmsconfigureworldscreen;
    }

    public RealmsConfigureWorldScreen getNewScreenWithKnownData(RealmsServer server) {
        RealmsConfigureWorldScreen realmsconfigureworldscreen = new RealmsConfigureWorldScreen(this.lastScreen, this.serverId, server, this.regions);
        realmsconfigureworldscreen.stateChanged = this.stateChanged;
        return realmsconfigureworldscreen;
    }
}
