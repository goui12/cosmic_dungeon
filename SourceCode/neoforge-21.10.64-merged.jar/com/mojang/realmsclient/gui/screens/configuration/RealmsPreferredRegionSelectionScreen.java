package com.mojang.realmsclient.gui.screens.configuration;

import com.mojang.realmsclient.dto.RealmsRegion;
import com.mojang.realmsclient.dto.RegionSelectionPreference;
import com.mojang.realmsclient.dto.ServiceQuality;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import javax.annotation.Nullable;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class RealmsPreferredRegionSelectionScreen extends Screen {
    private static final Component REGION_SELECTION_LABEL = Component.translatable("mco.configure.world.region_preference.title");
    private static final int SPACING = 8;
    private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);
    private final Screen parent;
    private final BiConsumer<RegionSelectionPreference, RealmsRegion> applySettings;
    final Map<RealmsRegion, ServiceQuality> regionServiceQuality;
    @Nullable
    private RealmsPreferredRegionSelectionScreen.RegionSelectionList list;
    RealmsSettingsTab.RegionSelection selection;
    @Nullable
    private Button doneButton;

    public RealmsPreferredRegionSelectionScreen(
        Screen parent,
        BiConsumer<RegionSelectionPreference, RealmsRegion> applySettings,
        Map<RealmsRegion, ServiceQuality> regionServiceQuality,
        RealmsSettingsTab.RegionSelection selection
    ) {
        super(REGION_SELECTION_LABEL);
        this.parent = parent;
        this.applySettings = applySettings;
        this.regionServiceQuality = regionServiceQuality;
        this.selection = selection;
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.parent);
    }

    @Override
    protected void init() {
        LinearLayout linearlayout = this.layout.addToHeader(LinearLayout.vertical().spacing(8));
        linearlayout.defaultCellSetting().alignHorizontallyCenter();
        linearlayout.addChild(new StringWidget(this.getTitle(), this.font));
        this.list = this.layout.addToContents(new RealmsPreferredRegionSelectionScreen.RegionSelectionList());
        LinearLayout linearlayout1 = this.layout.addToFooter(LinearLayout.horizontal().spacing(8));
        this.doneButton = linearlayout1.addChild(Button.builder(CommonComponents.GUI_DONE, p_425124_ -> {
            this.applySettings.accept(this.selection.preference(), this.selection.region());
            this.onClose();
        }).build());
        linearlayout1.addChild(Button.builder(CommonComponents.GUI_CANCEL, p_419746_ -> this.onClose()).build());
        this.list
            .setSelected(this.list.children().stream().filter(p_419913_ -> Objects.equals(p_419913_.regionSelection, this.selection)).findFirst().orElse(null));
        this.layout.visitWidgets(p_419955_ -> {
            AbstractWidget abstractwidget = this.addRenderableWidget(p_419955_);
        });
        this.repositionElements();
    }

    @Override
    protected void repositionElements() {
        this.layout.arrangeElements();
        this.list.updateSize(this.width, this.layout);
    }

    void updateButtonValidity() {
        this.doneButton.active = this.list.getSelected() != null;
    }

    @OnlyIn(Dist.CLIENT)
    class RegionSelectionList extends ObjectSelectionList<RealmsPreferredRegionSelectionScreen.RegionSelectionList.Entry> {
        RegionSelectionList() {
            super(
                RealmsPreferredRegionSelectionScreen.this.minecraft,
                RealmsPreferredRegionSelectionScreen.this.width,
                RealmsPreferredRegionSelectionScreen.this.height - 77,
                40,
                16
            );
            this.addEntry(new RealmsPreferredRegionSelectionScreen.RegionSelectionList.Entry(RegionSelectionPreference.AUTOMATIC_PLAYER, null));
            this.addEntry(new RealmsPreferredRegionSelectionScreen.RegionSelectionList.Entry(RegionSelectionPreference.AUTOMATIC_OWNER, null));
            RealmsPreferredRegionSelectionScreen.this.regionServiceQuality
                .keySet()
                .stream()
                .map(p_425125_ -> new RealmsPreferredRegionSelectionScreen.RegionSelectionList.Entry(RegionSelectionPreference.MANUAL, p_425125_))
                .forEach(p_419732_ -> this.addEntry(p_419732_));
        }

        public void setSelected(@Nullable RealmsPreferredRegionSelectionScreen.RegionSelectionList.Entry selected) {
            super.setSelected(selected);
            if (selected != null) {
                RealmsPreferredRegionSelectionScreen.this.selection = selected.regionSelection;
            }

            RealmsPreferredRegionSelectionScreen.this.updateButtonValidity();
        }

        @OnlyIn(Dist.CLIENT)
        class Entry extends ObjectSelectionList.Entry<RealmsPreferredRegionSelectionScreen.RegionSelectionList.Entry> {
            final RealmsSettingsTab.RegionSelection regionSelection;
            private final Component name;

            public Entry(RegionSelectionPreference regionSelectionPreference, @Nullable RealmsRegion preferredRegion) {
                this(new RealmsSettingsTab.RegionSelection(regionSelectionPreference, preferredRegion));
            }

            public Entry(RealmsSettingsTab.RegionSelection regionSelection) {
                this.regionSelection = regionSelection;
                if (regionSelection.preference() == RegionSelectionPreference.MANUAL) {
                    if (regionSelection.region() != null) {
                        this.name = Component.translatable(regionSelection.region().translationKey);
                    } else {
                        this.name = Component.empty();
                    }
                } else {
                    this.name = Component.translatable(regionSelection.preference().translationKey);
                }
            }

            @Override
            public Component getNarration() {
                return Component.translatable("narrator.select", this.name);
            }

            @Override
            public void renderContent(GuiGraphics guiGraphics, int mouseX, int mouseY, boolean isHovering, float partialTick) {
                guiGraphics.drawString(RealmsPreferredRegionSelectionScreen.this.font, this.name, this.getContentX() + 5, this.getContentY() + 2, -1);
                if (this.regionSelection.region() != null
                    && RealmsPreferredRegionSelectionScreen.this.regionServiceQuality.containsKey(this.regionSelection.region())) {
                    ServiceQuality servicequality = RealmsPreferredRegionSelectionScreen.this.regionServiceQuality
                        .getOrDefault(this.regionSelection.region(), ServiceQuality.UNKNOWN);
                    guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, servicequality.getIcon(), this.getContentRight() - 18, this.getContentY() + 2, 10, 8);
                }
            }

            @Override
            public boolean mouseClicked(MouseButtonEvent event, boolean isDoubleClick) {
                RegionSelectionList.this.setSelected(this);
                return super.mouseClicked(event, isDoubleClick);
            }
        }
    }
}
