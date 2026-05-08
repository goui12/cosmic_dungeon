package net.minecraft.client.gui.components.tabs;

import java.util.Objects;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class TabManager {
    private final Consumer<AbstractWidget> addWidget;
    private final Consumer<AbstractWidget> removeWidget;
    private final Consumer<Tab> onSelected;
    private final Consumer<Tab> onDeselected;
    @Nullable
    private Tab currentTab;
    @Nullable
    private ScreenRectangle tabArea;

    public TabManager(Consumer<AbstractWidget> addWidget, Consumer<AbstractWidget> removeWidget) {
        this(addWidget, removeWidget, p_419709_ -> {}, p_419497_ -> {});
    }

    public TabManager(Consumer<AbstractWidget> addWidget, Consumer<AbstractWidget> removeWidget, Consumer<Tab> onSelected, Consumer<Tab> onDeselected) {
        this.addWidget = addWidget;
        this.removeWidget = removeWidget;
        this.onSelected = onSelected;
        this.onDeselected = onDeselected;
    }

    public void setTabArea(ScreenRectangle tabArea) {
        this.tabArea = tabArea;
        Tab tab = this.getCurrentTab();
        if (tab != null) {
            tab.doLayout(tabArea);
        }
    }

    public void setCurrentTab(Tab p_tab, boolean playClickSound) {
        if (!Objects.equals(this.currentTab, p_tab)) {
            if (this.currentTab != null) {
                this.currentTab.visitChildren(this.removeWidget);
            }

            Tab tab = this.currentTab;
            this.currentTab = p_tab;
            p_tab.visitChildren(this.addWidget);
            if (this.tabArea != null) {
                p_tab.doLayout(this.tabArea);
            }

            if (playClickSound) {
                Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            }

            this.onDeselected.accept(tab);
            this.onSelected.accept(this.currentTab);
        }
    }

    @Nullable
    public Tab getCurrentTab() {
        return this.currentTab;
    }
}
