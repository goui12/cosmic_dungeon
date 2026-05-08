package com.mojang.realmsclient;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.RateLimiter;
import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.client.Ping;
import com.mojang.realmsclient.client.RealmsClient;
import com.mojang.realmsclient.dto.PingResult;
import com.mojang.realmsclient.dto.RealmsNews;
import com.mojang.realmsclient.dto.RealmsNotification;
import com.mojang.realmsclient.dto.RealmsServer;
import com.mojang.realmsclient.dto.RealmsServerPlayerLists;
import com.mojang.realmsclient.dto.RegionPingResult;
import com.mojang.realmsclient.exception.RealmsServiceException;
import com.mojang.realmsclient.gui.RealmsDataFetcher;
import com.mojang.realmsclient.gui.RealmsServerList;
import com.mojang.realmsclient.gui.screens.AddRealmPopupScreen;
import com.mojang.realmsclient.gui.screens.RealmsCreateRealmScreen;
import com.mojang.realmsclient.gui.screens.RealmsGenericErrorScreen;
import com.mojang.realmsclient.gui.screens.RealmsLongRunningMcoTaskScreen;
import com.mojang.realmsclient.gui.screens.RealmsPendingInvitesScreen;
import com.mojang.realmsclient.gui.screens.RealmsPopups;
import com.mojang.realmsclient.gui.screens.configuration.RealmsConfigureWorldScreen;
import com.mojang.realmsclient.gui.task.DataFetcher;
import com.mojang.realmsclient.util.RealmsPersistence;
import com.mojang.realmsclient.util.RealmsUtil;
import com.mojang.realmsclient.util.task.GetServerDetailsTask;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.FocusableTextWidget;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.ImageWidget;
import net.minecraft.client.gui.components.LoadingDotsWidget;
import net.minecraft.client.gui.components.MultiLineTextWidget;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.gui.components.PopupScreen;
import net.minecraft.client.gui.components.SpriteIconButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.components.WidgetTooltipHolder;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.FrameLayout;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.Layout;
import net.minecraft.client.gui.layouts.LayoutSettings;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.layouts.SpacerElement;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientActivePlayersTooltip;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.PlayerSkinRenderCache;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.realms.RealmsScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.CommonLinks;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.GameType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class RealmsMainScreen extends RealmsScreen {
    static final ResourceLocation INFO_SPRITE = ResourceLocation.withDefaultNamespace("icon/info");
    static final ResourceLocation NEW_REALM_SPRITE = ResourceLocation.withDefaultNamespace("icon/new_realm");
    static final ResourceLocation EXPIRED_SPRITE = ResourceLocation.withDefaultNamespace("realm_status/expired");
    static final ResourceLocation EXPIRES_SOON_SPRITE = ResourceLocation.withDefaultNamespace("realm_status/expires_soon");
    static final ResourceLocation OPEN_SPRITE = ResourceLocation.withDefaultNamespace("realm_status/open");
    static final ResourceLocation CLOSED_SPRITE = ResourceLocation.withDefaultNamespace("realm_status/closed");
    private static final ResourceLocation INVITE_SPRITE = ResourceLocation.withDefaultNamespace("icon/invite");
    private static final ResourceLocation NEWS_SPRITE = ResourceLocation.withDefaultNamespace("icon/news");
    public static final ResourceLocation HARDCORE_MODE_SPRITE = ResourceLocation.withDefaultNamespace("hud/heart/hardcore_full");
    static final Logger LOGGER = LogUtils.getLogger();
    private static final ResourceLocation NO_REALMS_LOCATION = ResourceLocation.withDefaultNamespace("textures/gui/realms/no_realms.png");
    private static final Component TITLE = Component.translatable("menu.online");
    private static final Component LOADING_TEXT = Component.translatable("mco.selectServer.loading");
    static final Component SERVER_UNITIALIZED_TEXT = Component.translatable("mco.selectServer.uninitialized");
    static final Component SUBSCRIPTION_EXPIRED_TEXT = Component.translatable("mco.selectServer.expiredList");
    private static final Component SUBSCRIPTION_RENEW_TEXT = Component.translatable("mco.selectServer.expiredRenew");
    static final Component TRIAL_EXPIRED_TEXT = Component.translatable("mco.selectServer.expiredTrial");
    private static final Component PLAY_TEXT = Component.translatable("mco.selectServer.play");
    private static final Component LEAVE_SERVER_TEXT = Component.translatable("mco.selectServer.leave");
    private static final Component CONFIGURE_SERVER_TEXT = Component.translatable("mco.selectServer.configure");
    static final Component SERVER_EXPIRED_TOOLTIP = Component.translatable("mco.selectServer.expired");
    static final Component SERVER_EXPIRES_SOON_TOOLTIP = Component.translatable("mco.selectServer.expires.soon");
    static final Component SERVER_EXPIRES_IN_DAY_TOOLTIP = Component.translatable("mco.selectServer.expires.day");
    static final Component SERVER_OPEN_TOOLTIP = Component.translatable("mco.selectServer.open");
    static final Component SERVER_CLOSED_TOOLTIP = Component.translatable("mco.selectServer.closed");
    static final Component UNITIALIZED_WORLD_NARRATION = Component.translatable("gui.narrate.button", SERVER_UNITIALIZED_TEXT);
    private static final Component NO_REALMS_TEXT = Component.translatable("mco.selectServer.noRealms");
    private static final Component NO_PENDING_INVITES = Component.translatable("mco.invites.nopending");
    private static final Component PENDING_INVITES = Component.translatable("mco.invites.pending");
    private static final Component INCOMPATIBLE_POPUP_TITLE = Component.translatable("mco.compatibility.incompatible.popup.title");
    private static final Component INCOMPATIBLE_RELEASE_TYPE_POPUP_MESSAGE = Component.translatable("mco.compatibility.incompatible.releaseType.popup.message");
    private static final int BUTTON_WIDTH = 100;
    private static final int BUTTON_COLUMNS = 3;
    private static final int BUTTON_SPACING = 4;
    private static final int CONTENT_WIDTH = 308;
    private static final int LOGO_PADDING = 5;
    private static final int HEADER_HEIGHT = 44;
    private static final int FOOTER_PADDING = 11;
    private static final int NEW_REALM_SPRITE_WIDTH = 40;
    private static final int NEW_REALM_SPRITE_HEIGHT = 20;
    private static final boolean SNAPSHOT = !SharedConstants.getCurrentVersion().stable();
    private static boolean snapshotToggle = SNAPSHOT;
    private final CompletableFuture<RealmsAvailability.Result> availability = RealmsAvailability.get();
    @Nullable
    private DataFetcher.Subscription dataSubscription;
    private final Set<UUID> handledSeenNotifications = new HashSet<>();
    private static boolean regionsPinged;
    private final RateLimiter inviteNarrationLimiter;
    private final Screen lastScreen;
    private Button playButton;
    private Button backButton;
    private Button renewButton;
    private Button configureButton;
    private Button leaveButton;
    RealmsMainScreen.RealmSelectionList realmSelectionList;
    RealmsServerList serverList;
    List<RealmsServer> availableSnapshotServers = List.of();
    RealmsServerPlayerLists onlinePlayersPerRealm = new RealmsServerPlayerLists();
    private volatile boolean trialsAvailable;
    @Nullable
    private volatile String newsLink;
    final List<RealmsNotification> notifications = new ArrayList<>();
    private Button addRealmButton;
    private RealmsMainScreen.NotificationButton pendingInvitesButton;
    private RealmsMainScreen.NotificationButton newsButton;
    private RealmsMainScreen.LayoutState activeLayoutState;
    @Nullable
    private HeaderAndFooterLayout layout;

    public RealmsMainScreen(Screen lastScreen) {
        super(TITLE);
        this.lastScreen = lastScreen;
        this.inviteNarrationLimiter = RateLimiter.create(0.016666668F);
    }

    @Override
    public void init() {
        this.serverList = new RealmsServerList(this.minecraft);
        this.realmSelectionList = new RealmsMainScreen.RealmSelectionList();
        Component component = Component.translatable("mco.invites.title");
        this.pendingInvitesButton = new RealmsMainScreen.NotificationButton(
            component, INVITE_SPRITE, p_293547_ -> this.minecraft.setScreen(new RealmsPendingInvitesScreen(this, component)), null
        );
        Component component1 = Component.translatable("mco.news");
        this.newsButton = new RealmsMainScreen.NotificationButton(component1, NEWS_SPRITE, p_307015_ -> {
            String s = this.newsLink;
            if (s != null) {
                ConfirmLinkScreen.confirmLinkNow(this, s);
                if (this.newsButton.notificationCount() != 0) {
                    RealmsPersistence.RealmsPersistenceData realmspersistence$realmspersistencedata = RealmsPersistence.readFile();
                    realmspersistence$realmspersistencedata.hasUnreadNews = false;
                    RealmsPersistence.writeFile(realmspersistence$realmspersistencedata);
                    this.newsButton.setNotificationCount(0);
                }
            }
        }, component1);
        this.playButton = Button.builder(PLAY_TEXT, p_302303_ -> play(this.getSelectedServer(), this)).width(100).build();
        this.configureButton = Button.builder(CONFIGURE_SERVER_TEXT, p_86672_ -> this.configureClicked(this.getSelectedServer())).width(100).build();
        this.renewButton = Button.builder(SUBSCRIPTION_RENEW_TEXT, p_86622_ -> this.onRenew(this.getSelectedServer())).width(100).build();
        this.leaveButton = Button.builder(LEAVE_SERVER_TEXT, p_86679_ -> this.leaveClicked(this.getSelectedServer())).width(100).build();
        this.addRealmButton = Button.builder(Component.translatable("mco.selectServer.purchase"), p_300620_ -> this.openTrialAvailablePopup())
            .size(100, 20)
            .build();
        this.backButton = Button.builder(CommonComponents.GUI_BACK, p_315807_ -> this.onClose()).width(100).build();
        if (RealmsClient.ENVIRONMENT == RealmsClient.Environment.STAGE) {
            this.addRenderableWidget(
                CycleButton.booleanBuilder(Component.literal("Snapshot"), Component.literal("Release"))
                    .create(5, 5, 100, 20, Component.literal("Realm"), (p_305606_, p_305607_) -> {
                        snapshotToggle = p_305607_;
                        this.availableSnapshotServers = List.of();
                        this.debugRefreshDataFetchers();
                    })
            );
        }

        this.updateLayout(RealmsMainScreen.LayoutState.LOADING);
        this.updateButtonStates();
        this.availability.thenAcceptAsync(p_293549_ -> {
            Screen screen = p_293549_.createErrorScreen(this.lastScreen);
            if (screen == null) {
                this.dataSubscription = this.initDataFetcher(this.minecraft.realmsDataFetcher());
            } else {
                this.minecraft.setScreen(screen);
            }
        }, this.screenExecutor);
    }

    public static boolean isSnapshot() {
        return SNAPSHOT && snapshotToggle;
    }

    @Override
    protected void repositionElements() {
        if (this.layout != null) {
            this.realmSelectionList.updateSize(this.width, this.layout);
            this.layout.arrangeElements();
        }
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.lastScreen);
    }

    private void updateLayout() {
        if (this.serverList.isEmpty() && this.availableSnapshotServers.isEmpty() && this.notifications.isEmpty()) {
            this.updateLayout(RealmsMainScreen.LayoutState.NO_REALMS);
        } else {
            this.updateLayout(RealmsMainScreen.LayoutState.LIST);
        }
    }

    private void updateLayout(RealmsMainScreen.LayoutState layoutState) {
        if (this.activeLayoutState != layoutState) {
            if (this.layout != null) {
                this.layout.visitWidgets(p_321332_ -> this.removeWidget(p_321332_));
            }

            this.layout = this.createLayout(layoutState);
            this.activeLayoutState = layoutState;
            this.layout.visitWidgets(p_321334_ -> {
                AbstractWidget abstractwidget = this.addRenderableWidget(p_321334_);
            });
            this.repositionElements();
        }
    }

    private HeaderAndFooterLayout createLayout(RealmsMainScreen.LayoutState layoutState) {
        HeaderAndFooterLayout headerandfooterlayout = new HeaderAndFooterLayout(this);
        headerandfooterlayout.setHeaderHeight(44);
        headerandfooterlayout.addToHeader(this.createHeader());
        Layout layout = this.createFooter(layoutState);
        layout.arrangeElements();
        headerandfooterlayout.setFooterHeight(layout.getHeight() + 22);
        headerandfooterlayout.addToFooter(layout);
        switch (layoutState) {
            case LOADING:
                headerandfooterlayout.addToContents(new LoadingDotsWidget(this.font, LOADING_TEXT));
                break;
            case NO_REALMS:
                headerandfooterlayout.addToContents(this.createNoRealmsContent());
                break;
            case LIST:
                headerandfooterlayout.addToContents(this.realmSelectionList);
        }

        return headerandfooterlayout;
    }

    private Layout createHeader() {
        int i = 90;
        LinearLayout linearlayout = LinearLayout.horizontal().spacing(4);
        linearlayout.defaultCellSetting().alignVerticallyMiddle();
        linearlayout.addChild(this.pendingInvitesButton);
        linearlayout.addChild(this.newsButton);
        LinearLayout linearlayout1 = LinearLayout.horizontal();
        linearlayout1.defaultCellSetting().alignVerticallyMiddle();
        linearlayout1.addChild(SpacerElement.width(90));
        linearlayout1.addChild(realmsLogo(), LayoutSettings::alignHorizontallyCenter);
        linearlayout1.addChild(new FrameLayout(90, 44)).addChild(linearlayout, LayoutSettings::alignHorizontallyRight);
        return linearlayout1;
    }

    private Layout createFooter(RealmsMainScreen.LayoutState layoutState) {
        GridLayout gridlayout = new GridLayout().spacing(4);
        GridLayout.RowHelper gridlayout$rowhelper = gridlayout.createRowHelper(3);
        if (layoutState == RealmsMainScreen.LayoutState.LIST) {
            gridlayout$rowhelper.addChild(this.playButton);
            gridlayout$rowhelper.addChild(this.configureButton);
            gridlayout$rowhelper.addChild(this.renewButton);
            gridlayout$rowhelper.addChild(this.leaveButton);
        }

        gridlayout$rowhelper.addChild(this.addRealmButton);
        gridlayout$rowhelper.addChild(this.backButton);
        return gridlayout;
    }

    private LinearLayout createNoRealmsContent() {
        LinearLayout linearlayout = LinearLayout.vertical().spacing(8);
        linearlayout.defaultCellSetting().alignHorizontallyCenter();
        linearlayout.addChild(ImageWidget.texture(130, 64, NO_REALMS_LOCATION, 130, 64));
        FocusableTextWidget focusabletextwidget = new FocusableTextWidget(308, NO_REALMS_TEXT, this.font, false, FocusableTextWidget.BackgroundFill.NEVER, 4);
        linearlayout.addChild(focusabletextwidget);
        return linearlayout;
    }

    void updateButtonStates() {
        RealmsServer realmsserver = this.getSelectedServer();
        boolean flag = realmsserver != null;
        this.addRealmButton.active = this.activeLayoutState != RealmsMainScreen.LayoutState.LOADING;
        this.playButton.active = flag && realmsserver.shouldPlayButtonBeActive();
        if (!this.playButton.active && flag && realmsserver.state == RealmsServer.State.CLOSED) {
            this.playButton.setTooltip(Tooltip.create(RealmsServer.WORLD_CLOSED_COMPONENT));
        }

        this.renewButton.active = flag && this.shouldRenewButtonBeActive(realmsserver);
        this.leaveButton.active = flag && this.shouldLeaveButtonBeActive(realmsserver);
        this.configureButton.active = flag && this.shouldConfigureButtonBeActive(realmsserver);
    }

    private boolean shouldRenewButtonBeActive(RealmsServer realmsServer) {
        return realmsServer.expired && isSelfOwnedServer(realmsServer);
    }

    private boolean shouldConfigureButtonBeActive(RealmsServer realmsServer) {
        return isSelfOwnedServer(realmsServer) && realmsServer.state != RealmsServer.State.UNINITIALIZED;
    }

    private boolean shouldLeaveButtonBeActive(RealmsServer realmsServer) {
        return !isSelfOwnedServer(realmsServer);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.dataSubscription != null) {
            this.dataSubscription.tick();
        }
    }

    public static void refreshPendingInvites() {
        Minecraft.getInstance().realmsDataFetcher().pendingInvitesTask.reset();
    }

    public static void refreshServerList() {
        Minecraft.getInstance().realmsDataFetcher().serverListUpdateTask.reset();
    }

    private void debugRefreshDataFetchers() {
        for (DataFetcher.Task<?> task : this.minecraft.realmsDataFetcher().getTasks()) {
            task.reset();
        }
    }

    private DataFetcher.Subscription initDataFetcher(RealmsDataFetcher dataFetcher) {
        DataFetcher.Subscription datafetcher$subscription = dataFetcher.dataFetcher.createSubscription();
        datafetcher$subscription.subscribe(dataFetcher.serverListUpdateTask, p_305616_ -> {
            this.serverList.updateServersList(p_305616_.serverList());
            this.availableSnapshotServers = p_305616_.availableSnapshotServers();
            this.refreshListAndLayout();
            boolean flag = false;

            for (RealmsServer realmsserver : this.serverList) {
                if (this.isSelfOwnedNonExpiredServer(realmsserver)) {
                    flag = true;
                }
            }

            if (!regionsPinged && flag) {
                regionsPinged = true;
                this.pingRegions();
            }
        });
        callRealmsClient(RealmsClient::getNotifications, p_304053_ -> {
            this.notifications.clear();
            this.notifications.addAll(p_304053_);

            for (RealmsNotification realmsnotification : p_304053_) {
                if (realmsnotification instanceof RealmsNotification.InfoPopup realmsnotification$infopopup) {
                    PopupScreen popupscreen = realmsnotification$infopopup.buildScreen(this, this::dismissNotification);
                    if (popupscreen != null) {
                        this.minecraft.setScreen(popupscreen);
                        this.markNotificationsAsSeen(List.of(realmsnotification));
                        break;
                    }
                }
            }

            if (!this.notifications.isEmpty() && this.activeLayoutState != RealmsMainScreen.LayoutState.LOADING) {
                this.refreshListAndLayout();
            }
        });
        datafetcher$subscription.subscribe(dataFetcher.pendingInvitesTask, p_300619_ -> {
            this.pendingInvitesButton.setNotificationCount(p_300619_);
            this.pendingInvitesButton.setTooltip(p_300619_ == 0 ? Tooltip.create(NO_PENDING_INVITES) : Tooltip.create(PENDING_INVITES));
            if (p_300619_ > 0 && this.inviteNarrationLimiter.tryAcquire(1)) {
                this.minecraft.getNarrator().saySystemNow(Component.translatable("mco.configure.world.invite.narration", p_300619_));
            }
        });
        datafetcher$subscription.subscribe(dataFetcher.trialAvailabilityTask, p_293548_ -> this.trialsAvailable = p_293548_);
        datafetcher$subscription.subscribe(dataFetcher.onlinePlayersTask, p_349772_ -> this.onlinePlayersPerRealm = p_349772_);
        datafetcher$subscription.subscribe(dataFetcher.newsTask, p_300622_ -> {
            dataFetcher.newsManager.updateUnreadNews(p_300622_);
            this.newsLink = dataFetcher.newsManager.newsLink();
            this.newsButton.setNotificationCount(dataFetcher.newsManager.hasUnreadNews() ? Integer.MAX_VALUE : 0);
        });
        return datafetcher$subscription;
    }

    void markNotificationsAsSeen(Collection<RealmsNotification> notifications) {
        List<UUID> list = new ArrayList<>(notifications.size());

        for (RealmsNotification realmsnotification : notifications) {
            if (!realmsnotification.seen() && !this.handledSeenNotifications.contains(realmsnotification.uuid())) {
                list.add(realmsnotification.uuid());
            }
        }

        if (!list.isEmpty()) {
            callRealmsClient(p_274625_ -> {
                p_274625_.notificationsSeen(list);
                return null;
            }, p_274630_ -> this.handledSeenNotifications.addAll(list));
        }
    }

    private static <T> void callRealmsClient(RealmsMainScreen.RealmsCall<T> call, Consumer<T> onFinish) {
        Minecraft minecraft = Minecraft.getInstance();
        CompletableFuture.<T>supplyAsync(() -> {
            try {
                return call.request(RealmsClient.getOrCreate(minecraft));
            } catch (RealmsServiceException realmsserviceexception) {
                throw new RuntimeException(realmsserviceexception);
            }
        }).thenAcceptAsync(onFinish, minecraft).exceptionally(p_274626_ -> {
            LOGGER.error("Failed to execute call to Realms Service", p_274626_);
            return null;
        });
    }

    private void refreshListAndLayout() {
        this.realmSelectionList.refreshEntries(this);
        this.updateLayout();
        this.updateButtonStates();
    }

    private void pingRegions() {
        new Thread(() -> {
            List<RegionPingResult> list = Ping.pingAllRegions();
            RealmsClient realmsclient = RealmsClient.getOrCreate();
            PingResult pingresult = new PingResult();
            pingresult.pingResults = list;
            pingresult.realmIds = this.getOwnedNonExpiredRealmIds();

            try {
                realmsclient.sendPingResults(pingresult);
            } catch (Throwable throwable) {
                LOGGER.warn("Could not send ping result to Realms: ", throwable);
            }
        }).start();
    }

    private List<Long> getOwnedNonExpiredRealmIds() {
        List<Long> list = Lists.newArrayList();

        for (RealmsServer realmsserver : this.serverList) {
            if (this.isSelfOwnedNonExpiredServer(realmsserver)) {
                list.add(realmsserver.id);
            }
        }

        return list;
    }

    private void onRenew(@Nullable RealmsServer realmsServer) {
        if (realmsServer != null) {
            String s = CommonLinks.extendRealms(realmsServer.remoteSubscriptionId, this.minecraft.getUser().getProfileId(), realmsServer.expiredTrial);
            this.minecraft.setScreen(new ConfirmLinkScreen(p_438676_ -> {
                if (p_438676_) {
                    Util.getPlatform().openUri(s);
                } else {
                    this.minecraft.setScreen(this);
                }
            }, s, true));
        }
    }

    private void configureClicked(@Nullable RealmsServer realmsServer) {
        if (realmsServer != null && this.minecraft.isLocalPlayer(realmsServer.ownerUUID)) {
            this.minecraft.setScreen(new RealmsConfigureWorldScreen(this, realmsServer.id));
        }
    }

    private void leaveClicked(@Nullable RealmsServer realmsServer) {
        if (realmsServer != null && !this.minecraft.isLocalPlayer(realmsServer.ownerUUID)) {
            Component component = Component.translatable("mco.configure.world.leave.question.line1");
            this.minecraft.setScreen(RealmsPopups.infoPopupScreen(this, component, p_344113_ -> this.leaveServer(realmsServer)));
        }
    }

    @Nullable
    private RealmsServer getSelectedServer() {
        return this.realmSelectionList.getSelected() instanceof RealmsMainScreen.ServerEntry realmsmainscreen$serverentry
            ? realmsmainscreen$serverentry.getServer()
            : null;
    }

    private void leaveServer(final RealmsServer server) {
        (new Thread("Realms-leave-server") {
                @Override
                public void run() {
                    try {
                        RealmsClient realmsclient = RealmsClient.getOrCreate();
                        realmsclient.uninviteMyselfFrom(server.id);
                        RealmsMainScreen.this.minecraft.execute(RealmsMainScreen::refreshServerList);
                    } catch (RealmsServiceException realmsserviceexception) {
                        RealmsMainScreen.LOGGER.error("Couldn't configure world", (Throwable)realmsserviceexception);
                        RealmsMainScreen.this.minecraft
                            .execute(
                                () -> RealmsMainScreen.this.minecraft.setScreen(new RealmsGenericErrorScreen(realmsserviceexception, RealmsMainScreen.this))
                            );
                    }
                }
            })
            .start();
        this.minecraft.setScreen(this);
    }

    void dismissNotification(UUID uuid) {
        callRealmsClient(p_274628_ -> {
            p_274628_.notificationsDismiss(List.of(uuid));
            return null;
        }, p_305610_ -> {
            this.notifications.removeIf(p_274621_ -> p_274621_.dismissable() && uuid.equals(p_274621_.uuid()));
            this.refreshListAndLayout();
        });
    }

    public void resetScreen() {
        this.realmSelectionList.setSelected(null);
        refreshServerList();
    }

    @Override
    public Component getNarrationMessage() {
        return (Component)(switch (this.activeLayoutState) {
            case LOADING -> CommonComponents.joinForNarration(super.getNarrationMessage(), LOADING_TEXT);
            case NO_REALMS -> CommonComponents.joinForNarration(super.getNarrationMessage(), NO_REALMS_TEXT);
            case LIST -> super.getNarrationMessage();
        });
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
        if (isSnapshot()) {
            guiGraphics.drawString(this.font, "Minecraft " + SharedConstants.getCurrentVersion().name(), 2, this.height - 10, -1);
        }

        if (this.trialsAvailable && this.addRealmButton.active) {
            AddRealmPopupScreen.renderDiamond(guiGraphics, this.addRealmButton);
        }

        switch (RealmsClient.ENVIRONMENT) {
            case STAGE:
                this.renderEnvironment(guiGraphics, "STAGE!", -256);
                break;
            case LOCAL:
                this.renderEnvironment(guiGraphics, "LOCAL!", -8388737);
        }
    }

    private void openTrialAvailablePopup() {
        this.minecraft.setScreen(new AddRealmPopupScreen(this, this.trialsAvailable));
    }

    public static void play(@Nullable RealmsServer realmsServer, Screen lastScreen) {
        play(realmsServer, lastScreen, false);
    }

    public static void play(@Nullable RealmsServer realmsServer, Screen lastScreen, boolean allowSnapshots) {
        if (realmsServer != null) {
            if (!isSnapshot() || allowSnapshots || realmsServer.isMinigameActive()) {
                Minecraft.getInstance().setScreen(new RealmsLongRunningMcoTaskScreen(lastScreen, new GetServerDetailsTask(lastScreen, realmsServer)));
                return;
            }

            switch (realmsServer.compatibility) {
                case COMPATIBLE:
                    Minecraft.getInstance().setScreen(new RealmsLongRunningMcoTaskScreen(lastScreen, new GetServerDetailsTask(lastScreen, realmsServer)));
                    break;
                case UNVERIFIABLE:
                    confirmToPlay(
                        realmsServer,
                        lastScreen,
                        Component.translatable("mco.compatibility.unverifiable.title").withColor(-171),
                        Component.translatable("mco.compatibility.unverifiable.message"),
                        CommonComponents.GUI_CONTINUE
                    );
                    break;
                case NEEDS_DOWNGRADE:
                    confirmToPlay(
                        realmsServer,
                        lastScreen,
                        Component.translatable("selectWorld.backupQuestion.downgrade").withColor(-2142128),
                        Component.translatable(
                            "mco.compatibility.downgrade.description",
                            Component.literal(realmsServer.activeVersion).withColor(-171),
                            Component.literal(SharedConstants.getCurrentVersion().name()).withColor(-171)
                        ),
                        Component.translatable("mco.compatibility.downgrade")
                    );
                    break;
                case NEEDS_UPGRADE:
                    upgradeRealmAndPlay(realmsServer, lastScreen);
                    break;
                case INCOMPATIBLE:
                    Minecraft.getInstance()
                        .setScreen(
                            new PopupScreen.Builder(lastScreen, INCOMPATIBLE_POPUP_TITLE)
                                .setMessage(
                                    Component.translatable(
                                        "mco.compatibility.incompatible.series.popup.message",
                                        Component.literal(realmsServer.activeVersion).withColor(-171),
                                        Component.literal(SharedConstants.getCurrentVersion().name()).withColor(-171)
                                    )
                                )
                                .addButton(CommonComponents.GUI_BACK, PopupScreen::onClose)
                                .build()
                        );
                    break;
                case RELEASE_TYPE_INCOMPATIBLE:
                    Minecraft.getInstance()
                        .setScreen(
                            new PopupScreen.Builder(lastScreen, INCOMPATIBLE_POPUP_TITLE)
                                .setMessage(INCOMPATIBLE_RELEASE_TYPE_POPUP_MESSAGE)
                                .addButton(CommonComponents.GUI_BACK, PopupScreen::onClose)
                                .build()
                        );
            }
        }
    }

    private static void confirmToPlay(RealmsServer realmsServer, Screen lastScreen, Component title, Component message, Component confirmButton) {
        Minecraft.getInstance().setScreen(new PopupScreen.Builder(lastScreen, title).setMessage(message).addButton(confirmButton, p_349775_ -> {
            Minecraft.getInstance().setScreen(new RealmsLongRunningMcoTaskScreen(lastScreen, new GetServerDetailsTask(lastScreen, realmsServer)));
            refreshServerList();
        }).addButton(CommonComponents.GUI_CANCEL, PopupScreen::onClose).build());
    }

    private static void upgradeRealmAndPlay(RealmsServer server, Screen lastScreen) {
        Component component = Component.translatable("mco.compatibility.upgrade.title").withColor(-171);
        Component component1 = Component.translatable("mco.compatibility.upgrade");
        Component component2 = Component.literal(server.activeVersion).withColor(-171);
        Component component3 = Component.literal(SharedConstants.getCurrentVersion().name()).withColor(-171);
        Component component4 = isSelfOwnedServer(server)
            ? Component.translatable("mco.compatibility.upgrade.description", component2, component3)
            : Component.translatable("mco.compatibility.upgrade.friend.description", component2, component3);
        confirmToPlay(server, lastScreen, component, component4, component1);
    }

    public static Component getVersionComponent(String version, boolean compatible) {
        return getVersionComponent(version, compatible ? -8355712 : -2142128);
    }

    public static Component getVersionComponent(String version, int color) {
        return (Component)(StringUtils.isBlank(version) ? CommonComponents.EMPTY : Component.literal(version).withColor(color));
    }

    public static Component getGameModeComponent(int gamemode, boolean hardcore) {
        return (Component)(hardcore ? Component.translatable("gameMode.hardcore").withColor(-65536) : GameType.byId(gamemode).getLongDisplayName());
    }

    static boolean isSelfOwnedServer(RealmsServer server) {
        return Minecraft.getInstance().isLocalPlayer(server.ownerUUID);
    }

    private boolean isSelfOwnedNonExpiredServer(RealmsServer server) {
        return isSelfOwnedServer(server) && !server.expired;
    }

    private void renderEnvironment(GuiGraphics guiGraphics, String text, int color) {
        guiGraphics.pose().pushMatrix();
        guiGraphics.pose().translate(this.width / 2 - 25, 20.0F);
        guiGraphics.pose().rotate((float) (-Math.PI / 9));
        guiGraphics.pose().scale(1.5F, 1.5F);
        guiGraphics.drawString(this.font, text, 0, 0, color);
        guiGraphics.pose().popMatrix();
    }

    @OnlyIn(Dist.CLIENT)
    class AvailableSnapshotEntry extends RealmsMainScreen.Entry {
        private static final Component START_SNAPSHOT_REALM = Component.translatable("mco.snapshot.start");
        private static final int TEXT_PADDING = 5;
        private final WidgetTooltipHolder tooltip = new WidgetTooltipHolder();
        private final RealmsServer parent;

        public AvailableSnapshotEntry(RealmsServer parent) {
            this.parent = parent;
            this.tooltip.set(Tooltip.create(Component.translatable("mco.snapshot.tooltip")));
        }

        @Override
        public void renderContent(GuiGraphics guiGraphics, int mouseX, int mouseY, boolean isHovering, float partialTick) {
            guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, RealmsMainScreen.NEW_REALM_SPRITE, this.getContentX() - 5, this.getContentYMiddle() - 10, 40, 20);
            int i = this.getContentYMiddle() - 9 / 2;
            guiGraphics.drawString(RealmsMainScreen.this.font, START_SNAPSHOT_REALM, this.getContentX() + 40 - 2, i - 5, -8388737);
            guiGraphics.drawString(
                RealmsMainScreen.this.font,
                Component.translatable("mco.snapshot.description", Objects.requireNonNullElse(this.parent.name, "unknown server")),
                this.getContentX() + 40 - 2,
                i + 5,
                -8355712
            );
            this.tooltip
                .refreshTooltipForNextRenderPass(
                    guiGraphics,
                    mouseX,
                    mouseY,
                    isHovering,
                    this.isFocused(),
                    new ScreenRectangle(this.getContentX(), this.getContentY(), this.getContentWidth(), this.getContentHeight())
                );
        }

        @Override
        public boolean mouseClicked(MouseButtonEvent event, boolean isDoubleClick) {
            this.addSnapshotRealm();
            return true;
        }

        @Override
        public boolean keyPressed(KeyEvent event) {
            if (event.isSelection()) {
                this.addSnapshotRealm();
                return false;
            } else {
                return super.keyPressed(event);
            }
        }

        private void addSnapshotRealm() {
            RealmsMainScreen.this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            RealmsMainScreen.this.minecraft
                .setScreen(
                    new PopupScreen.Builder(RealmsMainScreen.this, Component.translatable("mco.snapshot.createSnapshotPopup.title"))
                        .setMessage(Component.translatable("mco.snapshot.createSnapshotPopup.text"))
                        .addButton(
                            Component.translatable("mco.selectServer.create"),
                            p_438677_ -> RealmsMainScreen.this.minecraft.setScreen(new RealmsCreateRealmScreen(RealmsMainScreen.this, this.parent, true))
                        )
                        .addButton(CommonComponents.GUI_CANCEL, PopupScreen::onClose)
                        .build()
                );
        }

        @Override
        public Component getNarration() {
            return Component.translatable(
                "gui.narrate.button",
                CommonComponents.joinForNarration(
                    START_SNAPSHOT_REALM, Component.translatable("mco.snapshot.description", Objects.requireNonNullElse(this.parent.name, "unknown server"))
                )
            );
        }
    }

    @OnlyIn(Dist.CLIENT)
    static class CrossButton extends ImageButton {
        private static final WidgetSprites SPRITES = new WidgetSprites(
            ResourceLocation.withDefaultNamespace("widget/cross_button"), ResourceLocation.withDefaultNamespace("widget/cross_button_highlighted")
        );

        protected CrossButton(Button.OnPress onPress, Component message) {
            super(0, 0, 14, 14, SPRITES, onPress);
            this.setTooltip(Tooltip.create(message));
        }
    }

    @OnlyIn(Dist.CLIENT)
    abstract class Entry extends ObjectSelectionList.Entry<RealmsMainScreen.Entry> {
        protected static final int STATUS_LIGHT_WIDTH = 10;
        private static final int STATUS_LIGHT_HEIGHT = 28;
        protected static final int PADDING_X = 7;
        protected static final int PADDING_Y = 2;

        protected void renderStatusLights(RealmsServer realmsServer, GuiGraphics guiGraphics, int x, int y, int mouseX, int mouseY) {
            int i = x - 10 - 7;
            int j = y + 2;
            if (realmsServer.expired) {
                this.drawRealmStatus(guiGraphics, i, j, mouseX, mouseY, RealmsMainScreen.EXPIRED_SPRITE, () -> RealmsMainScreen.SERVER_EXPIRED_TOOLTIP);
            } else if (realmsServer.state == RealmsServer.State.CLOSED) {
                this.drawRealmStatus(guiGraphics, i, j, mouseX, mouseY, RealmsMainScreen.CLOSED_SPRITE, () -> RealmsMainScreen.SERVER_CLOSED_TOOLTIP);
            } else if (RealmsMainScreen.isSelfOwnedServer(realmsServer) && realmsServer.daysLeft < 7) {
                this.drawRealmStatus(
                    guiGraphics,
                    i,
                    j,
                    mouseX,
                    mouseY,
                    RealmsMainScreen.EXPIRES_SOON_SPRITE,
                    () -> {
                        if (realmsServer.daysLeft <= 0) {
                            return RealmsMainScreen.SERVER_EXPIRES_SOON_TOOLTIP;
                        } else {
                            return (Component)(realmsServer.daysLeft == 1
                                ? RealmsMainScreen.SERVER_EXPIRES_IN_DAY_TOOLTIP
                                : Component.translatable("mco.selectServer.expires.days", realmsServer.daysLeft));
                        }
                    }
                );
            } else if (realmsServer.state == RealmsServer.State.OPEN) {
                this.drawRealmStatus(guiGraphics, i, j, mouseX, mouseY, RealmsMainScreen.OPEN_SPRITE, () -> RealmsMainScreen.SERVER_OPEN_TOOLTIP);
            }
        }

        private void drawRealmStatus(
            GuiGraphics guiGraphics, int x, int y, int mouseX, int mouseY, ResourceLocation spriteLocation, Supplier<Component> tooltipSupplier
        ) {
            guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, spriteLocation, x, y, 10, 28);
            if (RealmsMainScreen.this.realmSelectionList.isMouseOver(mouseX, mouseY)
                && mouseX >= x
                && mouseX <= x + 10
                && mouseY >= y
                && mouseY <= y + 28) {
                guiGraphics.setTooltipForNextFrame(tooltipSupplier.get(), mouseX, mouseY);
            }
        }

        protected void renderFirstLine(GuiGraphics guiGraphics, int top, int left, int width, int serverNameColor, RealmsServer server) {
            int i = this.textX(left);
            int j = this.firstLineY(top);
            Component component = RealmsMainScreen.getVersionComponent(server.activeVersion, server.isCompatible());
            int k = this.versionTextX(left, width, component);
            this.renderClampedString(guiGraphics, server.getName(), i, j, k, serverNameColor);
            if (component != CommonComponents.EMPTY && !server.isMinigameActive()) {
                guiGraphics.drawString(RealmsMainScreen.this.font, component, k, j, -8355712);
            }
        }

        protected void renderSecondLine(GuiGraphics guiGraphics, int top, int left, int width, RealmsServer server) {
            int i = this.textX(left);
            int j = this.firstLineY(top);
            int k = this.secondLineY(j);
            String s = server.getMinigameName();
            boolean flag = server.isMinigameActive();
            if (flag && s != null) {
                Component component = Component.literal(s).withStyle(ChatFormatting.GRAY);
                guiGraphics.drawString(RealmsMainScreen.this.font, Component.translatable("mco.selectServer.minigameName", component).withColor(-171), i, k, -1);
            } else {
                int l = this.renderGameMode(server, guiGraphics, left, width, j);
                this.renderClampedString(guiGraphics, server.getDescription(), i, this.secondLineY(j), l, -8355712);
            }
        }

        protected void renderThirdLine(GuiGraphics guiGraphics, int top, int left, RealmsServer server) {
            int i = this.textX(left);
            int j = this.firstLineY(top);
            int k = this.thirdLineY(j);
            if (!RealmsMainScreen.isSelfOwnedServer(server)) {
                guiGraphics.drawString(RealmsMainScreen.this.font, server.owner, i, this.thirdLineY(j), -8355712);
            } else if (server.expired) {
                Component component = server.expiredTrial ? RealmsMainScreen.TRIAL_EXPIRED_TEXT : RealmsMainScreen.SUBSCRIPTION_EXPIRED_TEXT;
                guiGraphics.drawString(RealmsMainScreen.this.font, component, i, k, -2142128);
            }
        }

        protected void renderClampedString(GuiGraphics guiGraphics, @Nullable String text, int minX, int y, int maxX, int color) {
            if (text != null) {
                int i = maxX - minX;
                if (RealmsMainScreen.this.font.width(text) > i) {
                    String s = RealmsMainScreen.this.font.plainSubstrByWidth(text, i - RealmsMainScreen.this.font.width("... "));
                    guiGraphics.drawString(RealmsMainScreen.this.font, s + "...", minX, y, color);
                } else {
                    guiGraphics.drawString(RealmsMainScreen.this.font, text, minX, y, color);
                }
            }
        }

        protected int versionTextX(int left, int width, Component versionComponent) {
            return left + width - RealmsMainScreen.this.font.width(versionComponent) - 20;
        }

        protected int gameModeTextX(int left, int width, Component component) {
            return left + width - RealmsMainScreen.this.font.width(component) - 20;
        }

        protected int renderGameMode(RealmsServer server, GuiGraphics guiGraphics, int left, int width, int firstLineY) {
            boolean flag = server.isHardcore;
            int i = server.gameMode;
            int j = left;
            if (GameType.isValidId(i)) {
                Component component = RealmsMainScreen.getGameModeComponent(i, flag);
                j = this.gameModeTextX(left, width, component);
                guiGraphics.drawString(RealmsMainScreen.this.font, component, j, this.secondLineY(firstLineY), -8355712);
            }

            if (flag) {
                j -= 10;
                guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, RealmsMainScreen.HARDCORE_MODE_SPRITE, j, this.secondLineY(firstLineY), 8, 8);
            }

            return j;
        }

        protected int firstLineY(int top) {
            return top + 1;
        }

        protected int lineHeight() {
            return 2 + 9;
        }

        protected int textX(int left) {
            return left + 36 + 2;
        }

        protected int secondLineY(int firstLineY) {
            return firstLineY + this.lineHeight();
        }

        protected int thirdLineY(int firstLineY) {
            return firstLineY + this.lineHeight() * 2;
        }
    }

    @OnlyIn(Dist.CLIENT)
    static enum LayoutState {
        LOADING,
        NO_REALMS,
        LIST;
    }

    @OnlyIn(Dist.CLIENT)
    static class NotificationButton extends SpriteIconButton.CenteredIcon {
        private static final ResourceLocation[] NOTIFICATION_ICONS = new ResourceLocation[]{
            ResourceLocation.withDefaultNamespace("notification/1"),
            ResourceLocation.withDefaultNamespace("notification/2"),
            ResourceLocation.withDefaultNamespace("notification/3"),
            ResourceLocation.withDefaultNamespace("notification/4"),
            ResourceLocation.withDefaultNamespace("notification/5"),
            ResourceLocation.withDefaultNamespace("notification/more")
        };
        private static final int UNKNOWN_COUNT = Integer.MAX_VALUE;
        private static final int SIZE = 20;
        private static final int SPRITE_SIZE = 14;
        private int notificationCount;

        public NotificationButton(Component message, ResourceLocation sprite, Button.OnPress onPress, @Nullable Component tooltip) {
            super(20, 20, message, 14, 14, new WidgetSprites(sprite), onPress, tooltip, null);
        }

        int notificationCount() {
            return this.notificationCount;
        }

        public void setNotificationCount(int notificationCount) {
            this.notificationCount = notificationCount;
        }

        @Override
        public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            super.renderWidget(guiGraphics, mouseX, mouseY, partialTick);
            if (this.active && this.notificationCount != 0) {
                this.drawNotificationCounter(guiGraphics);
            }
        }

        private void drawNotificationCounter(GuiGraphics guiGraphics) {
            guiGraphics.blitSprite(
                RenderPipelines.GUI_TEXTURED,
                NOTIFICATION_ICONS[Math.min(this.notificationCount, 6) - 1],
                this.getX() + this.getWidth() - 5,
                this.getY() - 3,
                8,
                8
            );
        }
    }

    @OnlyIn(Dist.CLIENT)
    class NotificationMessageEntry extends RealmsMainScreen.Entry {
        private static final int SIDE_MARGINS = 40;
        public static final int PADDING = 7;
        public static final int HEIGHT_WITHOUT_TEXT = 38;
        private final Component text;
        private final List<AbstractWidget> children = new ArrayList<>();
        @Nullable
        private final RealmsMainScreen.CrossButton dismissButton;
        private final MultiLineTextWidget textWidget;
        private final GridLayout gridLayout;
        private final FrameLayout textFrame;
        private final Button button;
        private int lastEntryWidth = -1;

        public NotificationMessageEntry(RealmsMainScreen screen, int frameItemHeight, Component text, RealmsNotification.VisitUrl visitUrl) {
            this.text = text;
            this.gridLayout = new GridLayout();
            this.gridLayout.addChild(ImageWidget.sprite(20, 20, RealmsMainScreen.INFO_SPRITE), 0, 0, this.gridLayout.newCellSettings().padding(7, 7, 0, 0));
            this.gridLayout.addChild(SpacerElement.width(40), 0, 0);
            this.textFrame = this.gridLayout.addChild(new FrameLayout(0, frameItemHeight), 0, 1, this.gridLayout.newCellSettings().paddingTop(7));
            this.textWidget = this.textFrame
                .addChild(
                    new MultiLineTextWidget(text, RealmsMainScreen.this.font).setCentered(true),
                    this.textFrame.newChildLayoutSettings().alignHorizontallyCenter().alignVerticallyTop()
                );
            this.gridLayout.addChild(SpacerElement.width(40), 0, 2);
            if (visitUrl.dismissable()) {
                this.dismissButton = this.gridLayout
                    .addChild(
                        new RealmsMainScreen.CrossButton(
                            p_438679_ -> RealmsMainScreen.this.dismissNotification(visitUrl.uuid()), Component.translatable("mco.notification.dismiss")
                        ),
                        0,
                        2,
                        this.gridLayout.newCellSettings().alignHorizontallyRight().padding(0, 7, 7, 0)
                    );
            } else {
                this.dismissButton = null;
            }

            this.button = this.gridLayout
                .addChild(visitUrl.buildOpenLinkButton(screen), 1, 1, this.gridLayout.newCellSettings().alignHorizontallyCenter().padding(4));
            this.gridLayout.visitWidgets(this.children::add);
        }

        @Override
        public boolean keyPressed(KeyEvent event) {
            if (this.dismissButton != null && this.dismissButton.keyPressed(event)) {
                return true;
            } else {
                return this.button.keyPressed(event) ? true : super.keyPressed(event);
            }
        }

        private void updateEntryWidth() {
            int i = this.getContentWidth();
            if (this.lastEntryWidth != i) {
                this.refreshLayout(i);
                this.lastEntryWidth = i;
            }
        }

        private void refreshLayout(int width) {
            int i = textWidth(width);
            this.textFrame.setMinWidth(i);
            this.textWidget.setMaxWidth(i);
            this.gridLayout.arrangeElements();
        }

        public static int textWidth(int width) {
            return width - 80;
        }

        @Override
        public void renderContent(GuiGraphics guiGraphics, int mouseX, int mouseY, boolean isHovering, float partialTick) {
            this.gridLayout.setPosition(this.getContentX(), this.getContentY());
            this.updateEntryWidth();
            this.children.forEach(p_280688_ -> p_280688_.render(guiGraphics, mouseX, mouseY, partialTick));
        }

        @Override
        public boolean mouseClicked(MouseButtonEvent event, boolean isDoubleClick) {
            if (this.dismissButton != null && this.dismissButton.mouseClicked(event, isDoubleClick)) {
                return true;
            } else {
                return this.button.mouseClicked(event, isDoubleClick) ? true : super.mouseClicked(event, isDoubleClick);
            }
        }

        public Component getText() {
            return this.text;
        }

        @Override
        public Component getNarration() {
            return this.getText();
        }
    }

    @OnlyIn(Dist.CLIENT)
    class ParentEntry extends RealmsMainScreen.Entry {
        private final RealmsServer server;
        private final WidgetTooltipHolder tooltip = new WidgetTooltipHolder();

        public ParentEntry(RealmsServer server) {
            this.server = server;
            if (!server.expired) {
                this.tooltip.set(Tooltip.create(Component.translatable("mco.snapshot.parent.tooltip")));
            }
        }

        @Override
        public void renderContent(GuiGraphics guiGraphics, int mouseX, int mouseY, boolean isHovering, float partialTick) {
            this.renderStatusLights(this.server, guiGraphics, this.getContentRight(), this.getContentY(), mouseX, mouseY);
            RealmsUtil.renderPlayerFace(guiGraphics, this.getContentX(), this.getContentY(), 32, this.server.ownerUUID);
            this.renderFirstLine(guiGraphics, this.getContentY(), this.getContentX(), this.getContentWidth(), -8355712, this.server);
            this.renderSecondLine(guiGraphics, this.getContentY(), this.getContentX(), this.getContentWidth(), this.server);
            this.renderThirdLine(guiGraphics, this.getContentY(), this.getContentX(), this.server);
            this.tooltip
                .refreshTooltipForNextRenderPass(
                    guiGraphics,
                    mouseX,
                    mouseY,
                    isHovering,
                    this.isFocused(),
                    new ScreenRectangle(this.getContentX(), this.getContentY(), this.getContentWidth(), this.getContentHeight())
                );
        }

        @Override
        public Component getNarration() {
            return Component.literal(Objects.requireNonNullElse(this.server.name, "unknown server"));
        }
    }

    @OnlyIn(Dist.CLIENT)
    class RealmSelectionList extends ObjectSelectionList<RealmsMainScreen.Entry> {
        public RealmSelectionList() {
            super(Minecraft.getInstance(), RealmsMainScreen.this.width, RealmsMainScreen.this.height, 0, 36);
        }

        public void setSelected(@Nullable RealmsMainScreen.Entry selected) {
            super.setSelected(selected);
            RealmsMainScreen.this.updateButtonStates();
        }

        @Override
        public int getRowWidth() {
            return 300;
        }

        void refreshEntries(RealmsMainScreen screen) {
            RealmsMainScreen.Entry realmsmainscreen$entry = this.getSelected();
            this.clearEntries();

            for (RealmsNotification realmsnotification : RealmsMainScreen.this.notifications) {
                if (realmsnotification instanceof RealmsNotification.VisitUrl realmsnotification$visiturl) {
                    this.addEntriesForNotification(realmsnotification$visiturl, screen, realmsmainscreen$entry);
                    RealmsMainScreen.this.markNotificationsAsSeen(List.of(realmsnotification));
                    break;
                }
            }

            this.refreshServerEntries(realmsmainscreen$entry);
        }

        private void addEntriesForNotification(RealmsNotification.VisitUrl visitUrl, RealmsMainScreen screen, @Nullable RealmsMainScreen.Entry entry) {
            Component component = visitUrl.getMessage();
            int i = RealmsMainScreen.this.font.wordWrapHeight(component, RealmsMainScreen.NotificationMessageEntry.textWidth(this.getRowWidth()));
            RealmsMainScreen.NotificationMessageEntry realmsmainscreen$notificationmessageentry = RealmsMainScreen.this.new NotificationMessageEntry(
                screen, i, component, visitUrl
            );
            this.addEntry(realmsmainscreen$notificationmessageentry, 38 + i);
            if (entry instanceof RealmsMainScreen.NotificationMessageEntry realmsmainscreen$notificationmessageentry1
                && realmsmainscreen$notificationmessageentry1.getText().equals(component)) {
                this.setSelected((RealmsMainScreen.Entry)realmsmainscreen$notificationmessageentry);
            }
        }

        private void refreshServerEntries(@Nullable RealmsMainScreen.Entry entry) {
            for (RealmsServer realmsserver : RealmsMainScreen.this.availableSnapshotServers) {
                this.addEntry(RealmsMainScreen.this.new AvailableSnapshotEntry(realmsserver));
            }

            for (RealmsServer realmsserver1 : RealmsMainScreen.this.serverList) {
                RealmsMainScreen.Entry realmsmainscreen$entry;
                if (RealmsMainScreen.isSnapshot() && !realmsserver1.isSnapshotRealm()) {
                    if (realmsserver1.state == RealmsServer.State.UNINITIALIZED) {
                        continue;
                    }

                    realmsmainscreen$entry = RealmsMainScreen.this.new ParentEntry(realmsserver1);
                } else {
                    realmsmainscreen$entry = RealmsMainScreen.this.new ServerEntry(realmsserver1);
                }

                this.addEntry(realmsmainscreen$entry);
                if (entry instanceof RealmsMainScreen.ServerEntry realmsmainscreen$serverentry
                    && realmsmainscreen$serverentry.serverData.id == realmsserver1.id) {
                    this.setSelected(realmsmainscreen$entry);
                }
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    interface RealmsCall<T> {
        T request(RealmsClient realmsClient) throws RealmsServiceException;
    }

    @OnlyIn(Dist.CLIENT)
    class ServerEntry extends RealmsMainScreen.Entry {
        private static final Component ONLINE_PLAYERS_TOOLTIP_HEADER = Component.translatable("mco.onlinePlayers");
        private static final int PLAYERS_ONLINE_SPRITE_SIZE = 9;
        private static final int PLAYERS_ONLINE_SPRITE_SEPARATION = 3;
        private static final int SKIN_HEAD_LARGE_WIDTH = 36;
        final RealmsServer serverData;
        private final WidgetTooltipHolder tooltip = new WidgetTooltipHolder();

        public ServerEntry(RealmsServer serverData) {
            this.serverData = serverData;
            boolean flag = RealmsMainScreen.isSelfOwnedServer(serverData);
            if (RealmsMainScreen.isSnapshot() && flag && serverData.isSnapshotRealm()) {
                this.tooltip.set(Tooltip.create(Component.translatable("mco.snapshot.paired", serverData.parentWorldName)));
            } else if (!flag && serverData.needsDowngrade()) {
                this.tooltip.set(Tooltip.create(Component.translatable("mco.snapshot.friendsRealm.downgrade", serverData.activeVersion)));
            }
        }

        @Override
        public void renderContent(GuiGraphics guiGraphics, int mouseX, int mouseY, boolean isHovering, float partialTick) {
            if (this.serverData.state == RealmsServer.State.UNINITIALIZED) {
                guiGraphics.blitSprite(
                    RenderPipelines.GUI_TEXTURED, RealmsMainScreen.NEW_REALM_SPRITE, this.getContentX() - 5, this.getContentYMiddle() - 10, 40, 20
                );
                int i = this.getContentYMiddle() - 9 / 2;
                guiGraphics.drawString(RealmsMainScreen.this.font, RealmsMainScreen.SERVER_UNITIALIZED_TEXT, this.getContentX() + 40 - 2, i, -8388737);
            } else {
                RealmsUtil.renderPlayerFace(guiGraphics, this.getContentX(), this.getContentY(), 32, this.serverData.ownerUUID);
                this.renderFirstLine(guiGraphics, this.getContentY(), this.getContentX(), this.getContentWidth(), -1, this.serverData);
                this.renderSecondLine(guiGraphics, this.getContentY(), this.getContentX(), this.getContentWidth(), this.serverData);
                this.renderThirdLine(guiGraphics, this.getContentY(), this.getContentX(), this.serverData);
                this.renderStatusLights(this.serverData, guiGraphics, this.getContentRight(), this.getContentY(), mouseX, mouseY);
                boolean flag = this.renderOnlinePlayers(
                    guiGraphics, this.getContentY(), this.getContentX(), this.getContentWidth(), this.getContentHeight(), mouseX, mouseY, partialTick
                );
                if (!flag) {
                    this.tooltip
                        .refreshTooltipForNextRenderPass(
                            guiGraphics,
                            mouseX,
                            mouseY,
                            isHovering,
                            this.isFocused(),
                            new ScreenRectangle(this.getContentX(), this.getContentY(), this.getContentWidth(), this.getContentHeight())
                        );
                }
            }
        }

        private boolean renderOnlinePlayers(
            GuiGraphics guiGraphics, int top, int left, int width, int height, int mouseX, int mouseY, float partialTick
        ) {
            List<ResolvableProfile> list = RealmsMainScreen.this.onlinePlayersPerRealm.getProfileResultsFor(this.serverData.id);
            int i = list.size();
            if (i > 0) {
                int j = left + width - 21;
                int k = top + height - 9 - 2;
                int l = 9 * i + 3 * (i - 1);
                int i1 = j - l;
                List<PlayerSkinRenderCache.RenderInfo> list1;
                if (mouseX >= i1 && mouseX <= j && mouseY >= k && mouseY <= k + 9) {
                    list1 = new ArrayList<>(i);
                } else {
                    list1 = null;
                }

                PlayerSkinRenderCache playerskinrendercache = RealmsMainScreen.this.minecraft.playerSkinRenderCache();

                for (int j1 = 0; j1 < list.size(); j1++) {
                    ResolvableProfile resolvableprofile = list.get(j1);
                    PlayerSkinRenderCache.RenderInfo playerskinrendercache$renderinfo = playerskinrendercache.getOrDefault(resolvableprofile);
                    int k1 = i1 + 12 * j1;
                    PlayerFaceRenderer.draw(guiGraphics, playerskinrendercache$renderinfo.playerSkin(), k1, k, 9);
                    if (list1 != null) {
                        list1.add(playerskinrendercache$renderinfo);
                    }
                }

                if (list1 != null) {
                    guiGraphics.setTooltipForNextFrame(
                        RealmsMainScreen.this.font,
                        List.of(ONLINE_PLAYERS_TOOLTIP_HEADER),
                        Optional.of(new ClientActivePlayersTooltip.ActivePlayersTooltip(list1)),
                        mouseX,
                        mouseY
                    );
                    return true;
                }
            }

            return false;
        }

        private void playRealm() {
            RealmsMainScreen.this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            RealmsMainScreen.play(this.serverData, RealmsMainScreen.this);
        }

        private void createUnitializedRealm() {
            RealmsMainScreen.this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            RealmsCreateRealmScreen realmscreaterealmscreen = new RealmsCreateRealmScreen(
                RealmsMainScreen.this, this.serverData, this.serverData.isSnapshotRealm()
            );
            RealmsMainScreen.this.minecraft.setScreen(realmscreaterealmscreen);
        }

        @Override
        public boolean mouseClicked(MouseButtonEvent event, boolean isDoubleClick) {
            if (this.serverData.state == RealmsServer.State.UNINITIALIZED) {
                this.createUnitializedRealm();
            } else if (this.serverData.shouldPlayButtonBeActive() && isDoubleClick && this.isFocused()) {
                this.playRealm();
            }

            return true;
        }

        @Override
        public boolean keyPressed(KeyEvent event) {
            if (event.isSelection()) {
                if (this.serverData.state == RealmsServer.State.UNINITIALIZED) {
                    this.createUnitializedRealm();
                    return true;
                }

                if (this.serverData.shouldPlayButtonBeActive()) {
                    this.playRealm();
                    return true;
                }
            }

            return super.keyPressed(event);
        }

        @Override
        public Component getNarration() {
            return (Component)(this.serverData.state == RealmsServer.State.UNINITIALIZED
                ? RealmsMainScreen.UNITIALIZED_WORLD_NARRATION
                : Component.translatable("narrator.select", Objects.requireNonNullElse(this.serverData.name, "unknown server")));
        }

        public RealmsServer getServer() {
            return this.serverData;
        }
    }
}
