package com.mojang.realmsclient.gui.screens;

import com.mojang.realmsclient.dto.RealmsJoinInformation;
import com.mojang.realmsclient.dto.ServiceQuality;
import com.mojang.realmsclient.util.task.LongRunningTask;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.ImageWidget;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.FrameLayout;
import net.minecraft.client.gui.layouts.LayoutSettings;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class RealmsLongRunningMcoConnectTaskScreen extends RealmsLongRunningMcoTaskScreen {
    private final LongRunningTask task;
    private final RealmsJoinInformation serverAddress;
    private final LinearLayout footer = LinearLayout.vertical();

    public RealmsLongRunningMcoConnectTaskScreen(Screen lastScreen, RealmsJoinInformation serverAddress, LongRunningTask task) {
        super(lastScreen, task);
        this.task = task;
        this.serverAddress = serverAddress;
    }

    @Override
    public void init() {
        super.init();
        if (this.serverAddress.regionData() != null && this.serverAddress.regionData().region() != null) {
            LinearLayout linearlayout = LinearLayout.horizontal().spacing(10);
            StringWidget stringwidget = new StringWidget(
                Component.translatable("mco.connect.region", Component.translatable(this.serverAddress.regionData().region().translationKey)), this.font
            );
            linearlayout.addChild(stringwidget);
            ResourceLocation resourcelocation = this.serverAddress.regionData().serviceQuality() != null
                ? this.serverAddress.regionData().serviceQuality().getIcon()
                : ServiceQuality.UNKNOWN.getIcon();
            linearlayout.addChild(ImageWidget.sprite(10, 8, resourcelocation), LayoutSettings::alignVerticallyTop);
            this.footer.addChild(linearlayout, p_425961_ -> p_425961_.paddingTop(40));
            this.footer.visitWidgets(p_425560_ -> {
                AbstractWidget abstractwidget = this.addRenderableWidget(p_425560_);
            });
            this.repositionElements();
        }
    }

    @Override
    protected void repositionElements() {
        super.repositionElements();
        int i = this.layout.getY() + this.layout.getHeight();
        ScreenRectangle screenrectangle = new ScreenRectangle(0, i, this.width, this.height - i);
        this.footer.arrangeElements();
        FrameLayout.alignInRectangle(this.footer, screenrectangle, 0.5F, 0.0F);
    }

    @Override
    public void tick() {
        super.tick();
        this.task.tick();
    }

    @Override
    protected void cancel() {
        this.task.abortTask();
        super.cancel();
    }
}
