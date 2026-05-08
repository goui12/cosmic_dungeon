package net.minecraft.client.gui.components;

import javax.annotation.Nullable;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public abstract class AbstractContainerWidget extends AbstractScrollArea implements ContainerEventHandler {
    @Nullable
    private GuiEventListener focused;
    private boolean isDragging;

    public AbstractContainerWidget(int p_313730_, int p_313819_, int p_313847_, int p_313718_, Component p_313894_) {
        super(p_313730_, p_313819_, p_313847_, p_313718_, p_313894_);
    }

    @Override
    public final boolean isDragging() {
        return this.isDragging;
    }

    @Override
    public final void setDragging(boolean p_313698_) {
        this.isDragging = p_313698_;
    }

    @Nullable
    @Override
    public GuiEventListener getFocused() {
        return this.focused;
    }

    @Override
    public void setFocused(@Nullable GuiEventListener p_313725_) {
        if (this.focused != null) {
            this.focused.setFocused(false);
        }

        if (p_313725_ != null) {
            p_313725_.setFocused(true);
        }

        this.focused = p_313725_;
    }

    @Nullable
    @Override
    public ComponentPath nextFocusPath(FocusNavigationEvent p_313949_) {
        return ContainerEventHandler.super.nextFocusPath(p_313949_);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent p_446698_, boolean p_435133_) {
        boolean flag = this.updateScrolling(p_446698_);
        return ContainerEventHandler.super.mouseClicked(p_446698_, p_435133_) || flag;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent p_446281_) {
        super.mouseReleased(p_446281_);
        return ContainerEventHandler.super.mouseReleased(p_446281_);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent p_446509_, double p_313749_, double p_313887_) {
        super.mouseDragged(p_446509_, p_313749_, p_313887_);
        return ContainerEventHandler.super.mouseDragged(p_446509_, p_313749_, p_313887_);
    }

    @Override
    public boolean isFocused() {
        return ContainerEventHandler.super.isFocused();
    }

    @Override
    public void setFocused(boolean p_313936_) {
        ContainerEventHandler.super.setFocused(p_313936_);
    }
}
