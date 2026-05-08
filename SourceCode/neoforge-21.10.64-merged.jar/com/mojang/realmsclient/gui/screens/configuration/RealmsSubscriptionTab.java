package com.mojang.realmsclient.gui.screens.configuration;

import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.RealmsMainScreen;
import com.mojang.realmsclient.client.RealmsClient;
import com.mojang.realmsclient.dto.RealmsServer;
import com.mojang.realmsclient.dto.Subscription;
import com.mojang.realmsclient.exception.RealmsServiceException;
import com.mojang.realmsclient.gui.screens.RealmsPopups;
import com.mojang.realmsclient.util.RealmsUtil;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.FocusableTextWidget;
import net.minecraft.client.gui.components.PopupScreen;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.tabs.GridLayoutTab;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.layouts.LayoutSettings;
import net.minecraft.client.gui.layouts.SpacerElement;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.CommonLinks;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
class RealmsSubscriptionTab extends GridLayoutTab implements RealmsConfigurationTab {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int DEFAULT_COMPONENT_WIDTH = 200;
    private static final int EXTRA_SPACING = 2;
    private static final int DEFAULT_SPACING = 6;
    static final Component TITLE = Component.translatable("mco.configure.world.subscription.tab");
    private static final Component SUBSCRIPTION_START_LABEL = Component.translatable("mco.configure.world.subscription.start");
    private static final Component TIME_LEFT_LABEL = Component.translatable("mco.configure.world.subscription.timeleft");
    private static final Component DAYS_LEFT_LABEL = Component.translatable("mco.configure.world.subscription.recurring.daysleft");
    private static final Component SUBSCRIPTION_EXPIRED_TEXT = Component.translatable("mco.configure.world.subscription.expired")
        .withStyle(ChatFormatting.GRAY);
    private static final Component SUBSCRIPTION_LESS_THAN_A_DAY_TEXT = Component.translatable("mco.configure.world.subscription.less_than_a_day")
        .withStyle(ChatFormatting.GRAY);
    private static final Component UNKNOWN = Component.translatable("mco.configure.world.subscription.unknown");
    private static final Component RECURRING_INFO = Component.translatable("mco.configure.world.subscription.recurring.info");
    private final RealmsConfigureWorldScreen configurationScreen;
    private final Minecraft minecraft;
    private final Button deleteButton;
    private final FocusableTextWidget subscriptionInfo;
    private final StringWidget startDateWidget;
    private final StringWidget daysLeftLabelWidget;
    private final StringWidget daysLeftWidget;
    private RealmsServer serverData;
    private Component daysLeft = UNKNOWN;
    private Component startDate = UNKNOWN;
    @Nullable
    private Subscription.SubscriptionType type;

    RealmsSubscriptionTab(RealmsConfigureWorldScreen configurationScreen, Minecraft minecraft, RealmsServer serverData) {
        super(TITLE);
        this.configurationScreen = configurationScreen;
        this.minecraft = minecraft;
        this.serverData = serverData;
        GridLayout.RowHelper gridlayout$rowhelper = this.layout.rowSpacing(6).createRowHelper(1);
        Font font = configurationScreen.getFont();
        gridlayout$rowhelper.addChild(new StringWidget(200, 9, SUBSCRIPTION_START_LABEL, font));
        this.startDateWidget = gridlayout$rowhelper.addChild(new StringWidget(200, 9, this.startDate, font));
        gridlayout$rowhelper.addChild(SpacerElement.height(2));
        this.daysLeftLabelWidget = gridlayout$rowhelper.addChild(new StringWidget(200, 9, TIME_LEFT_LABEL, font));
        this.daysLeftWidget = gridlayout$rowhelper.addChild(new StringWidget(200, 9, this.daysLeft, font));
        gridlayout$rowhelper.addChild(SpacerElement.height(2));
        gridlayout$rowhelper.addChild(
            Button.builder(
                    Component.translatable("mco.configure.world.subscription.extend"),
                    p_419781_ -> ConfirmLinkScreen.confirmLinkNow(
                        configurationScreen, CommonLinks.extendRealms(serverData.remoteSubscriptionId, minecraft.getUser().getProfileId())
                    )
                )
                .bounds(0, 0, 200, 20)
                .build()
        );
        gridlayout$rowhelper.addChild(SpacerElement.height(2));
        this.deleteButton = gridlayout$rowhelper.addChild(
            Button.builder(
                    Component.translatable("mco.configure.world.delete.button"),
                    p_420051_ -> minecraft.setScreen(
                        RealmsPopups.warningPopupScreen(
                            configurationScreen, Component.translatable("mco.configure.world.delete.question.line1"), p_419742_ -> this.deleteRealm()
                        )
                    )
                )
                .bounds(0, 0, 200, 20)
                .build()
        );
        gridlayout$rowhelper.addChild(SpacerElement.height(2));
        this.subscriptionInfo = gridlayout$rowhelper.addChild(
            new FocusableTextWidget(200, Component.empty(), font), LayoutSettings.defaults().alignHorizontallyCenter()
        );
        this.subscriptionInfo.setMaxWidth(200);
        this.subscriptionInfo.setCentered(false);
        this.updateData(serverData);
    }

    private void deleteRealm() {
        RealmsUtil.runAsync(
                p_428664_ -> p_428664_.deleteRealm(this.serverData.id),
                RealmsUtil.openScreenAndLogOnFailure(this.configurationScreen::createErrorScreen, "Couldn't delete world")
            )
            .thenRunAsync(() -> this.minecraft.setScreen(this.configurationScreen.getLastScreen()), this.minecraft);
        this.minecraft.setScreen(this.configurationScreen);
    }

    private void getSubscription(long serverId) {
        RealmsClient realmsclient = RealmsClient.getOrCreate();

        try {
            Subscription subscription = realmsclient.subscriptionFor(serverId);
            this.daysLeft = this.daysLeftPresentation(subscription.daysLeft);
            this.startDate = localPresentation(subscription.startDate);
            this.type = subscription.type;
        } catch (RealmsServiceException realmsserviceexception) {
            LOGGER.error("Couldn't get subscription", (Throwable)realmsserviceexception);
            this.minecraft.setScreen(this.configurationScreen.createErrorScreen(realmsserviceexception));
        }
    }

    private static Component localPresentation(long time) {
        Calendar calendar = new GregorianCalendar(TimeZone.getDefault());
        calendar.setTimeInMillis(time);
        return Component.literal(DateFormat.getDateTimeInstance().format(calendar.getTime())).withStyle(ChatFormatting.GRAY);
    }

    private Component daysLeftPresentation(int daysLeft) {
        if (daysLeft < 0 && this.serverData.expired) {
            return SUBSCRIPTION_EXPIRED_TEXT;
        } else if (daysLeft <= 1) {
            return SUBSCRIPTION_LESS_THAN_A_DAY_TEXT;
        } else {
            int i = daysLeft / 30;
            int j = daysLeft % 30;
            boolean flag = i > 0;
            boolean flag1 = j > 0;
            if (flag && flag1) {
                return Component.translatable("mco.configure.world.subscription.remaining.months.days", i, j).withStyle(ChatFormatting.GRAY);
            } else if (flag) {
                return Component.translatable("mco.configure.world.subscription.remaining.months", i).withStyle(ChatFormatting.GRAY);
            } else {
                return flag1 ? Component.translatable("mco.configure.world.subscription.remaining.days", j).withStyle(ChatFormatting.GRAY) : Component.empty();
            }
        }
    }

    @Override
    public void updateData(RealmsServer server) {
        this.serverData = server;
        this.getSubscription(server.id);
        this.startDateWidget.setMessage(this.startDate);
        if (this.type == Subscription.SubscriptionType.NORMAL) {
            this.daysLeftLabelWidget.setMessage(TIME_LEFT_LABEL);
        } else if (this.type == Subscription.SubscriptionType.RECURRING) {
            this.daysLeftLabelWidget.setMessage(DAYS_LEFT_LABEL);
        }

        this.daysLeftWidget.setMessage(this.daysLeft);
        boolean flag = RealmsMainScreen.isSnapshot() && server.parentWorldName != null;
        this.deleteButton.active = server.expired;
        if (flag) {
            this.subscriptionInfo.setMessage(Component.translatable("mco.snapshot.subscription.info", server.parentWorldName));
        } else {
            this.subscriptionInfo.setMessage(RECURRING_INFO);
        }

        this.layout.arrangeElements();
    }

    @Override
    public Component getTabExtraNarration() {
        return CommonComponents.joinLines(TITLE, SUBSCRIPTION_START_LABEL, this.startDate, TIME_LEFT_LABEL, this.daysLeft);
    }
}
