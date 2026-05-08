package net.minecraft.client.gui.components.spectator;

import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.spectator.SpectatorMenu;
import net.minecraft.client.gui.spectator.SpectatorMenuItem;
import net.minecraft.client.gui.spectator.SpectatorMenuListener;
import net.minecraft.client.gui.spectator.categories.SpectatorPage;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SpectatorGui implements SpectatorMenuListener {
    private static final ResourceLocation HOTBAR_SPRITE = ResourceLocation.withDefaultNamespace("hud/hotbar");
    private static final ResourceLocation HOTBAR_SELECTION_SPRITE = ResourceLocation.withDefaultNamespace("hud/hotbar_selection");
    private static final long FADE_OUT_DELAY = 5000L;
    private static final long FADE_OUT_TIME = 2000L;
    private final Minecraft minecraft;
    private long lastSelectionTime;
    @Nullable
    private SpectatorMenu menu;

    public SpectatorGui(Minecraft minecraft) {
        this.minecraft = minecraft;
    }

    public void onHotbarSelected(int slot) {
        this.lastSelectionTime = Util.getMillis();
        if (this.menu != null) {
            this.menu.selectSlot(slot);
        } else {
            this.menu = new SpectatorMenu(this);
        }
    }

    private float getHotbarAlpha() {
        long i = this.lastSelectionTime - Util.getMillis() + 5000L;
        return Mth.clamp((float)i / 2000.0F, 0.0F, 1.0F);
    }

    public void renderHotbar(GuiGraphics guiGraphics) {
        if (this.menu != null) {
            float f = this.getHotbarAlpha();
            if (f <= 0.0F) {
                this.menu.exit();
            } else {
                int i = guiGraphics.guiWidth() / 2;
                int j = Mth.floor(guiGraphics.guiHeight() - 22.0F * f);
                SpectatorPage spectatorpage = this.menu.getCurrentPage();
                this.renderPage(guiGraphics, f, i, j, spectatorpage);
            }
        }
    }

    protected void renderPage(GuiGraphics guiGraphics, float alpha, int x, int y, SpectatorPage spectatorPage) {
        int i = ARGB.white(alpha);
        guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, HOTBAR_SPRITE, x - 91, y, 182, 22, i);
        if (spectatorPage.getSelectedSlot() >= 0) {
            guiGraphics.blitSprite(
                RenderPipelines.GUI_TEXTURED, HOTBAR_SELECTION_SPRITE, x - 91 - 1 + spectatorPage.getSelectedSlot() * 20, y - 1, 24, 23, i
            );
        }

        for (int j = 0; j < 9; j++) {
            this.renderSlot(guiGraphics, j, guiGraphics.guiWidth() / 2 - 90 + j * 20 + 2, y + 3, alpha, spectatorPage.getItem(j));
        }
    }

    private void renderSlot(GuiGraphics guiGraphics, int slot, int x, float y, float alpha, SpectatorMenuItem spectatorMenuItem) {
        if (spectatorMenuItem != SpectatorMenu.EMPTY_SLOT) {
            guiGraphics.pose().pushMatrix();
            guiGraphics.pose().translate(x, y);
            float f = spectatorMenuItem.isEnabled() ? 1.0F : 0.25F;
            spectatorMenuItem.renderIcon(guiGraphics, f, alpha);
            guiGraphics.pose().popMatrix();
            if (alpha > 0.0F && spectatorMenuItem.isEnabled()) {
                Component component = this.minecraft.options.keyHotbarSlots[slot].getTranslatedKeyMessage();
                guiGraphics.drawString(
                    this.minecraft.font,
                    component,
                    x + 19 - 2 - this.minecraft.font.width(component),
                    (int)y + 6 + 3,
                    ARGB.color(alpha, -1)
                );
            }
        }
    }

    public void renderAction(GuiGraphics guiGraphics) {
        float f = this.getHotbarAlpha();
        if (f > 0.0F && this.menu != null) {
            SpectatorMenuItem spectatormenuitem = this.menu.getSelectedItem();
            Component component = spectatormenuitem == SpectatorMenu.EMPTY_SLOT ? this.menu.getSelectedCategory().getPrompt() : spectatormenuitem.getName();
            int i = this.minecraft.font.width(component);
            int j = (guiGraphics.guiWidth() - i) / 2;
            int k = guiGraphics.guiHeight() - 35;
            guiGraphics.drawStringWithBackdrop(this.minecraft.font, component, j, k, i, ARGB.color(f, -1));
        }
    }

    @Override
    public void onSpectatorMenuClosed(SpectatorMenu menu) {
        this.menu = null;
        this.lastSelectionTime = 0L;
    }

    public boolean isMenuActive() {
        return this.menu != null;
    }

    public void onMouseScrolled(int amount) {
        int i = this.menu.getSelectedSlot() + amount;

        while (i >= 0 && i <= 8 && (this.menu.getItem(i) == SpectatorMenu.EMPTY_SLOT || !this.menu.getItem(i).isEnabled())) {
            i += amount;
        }

        if (i >= 0 && i <= 8) {
            this.menu.selectSlot(i);
            this.lastSelectionTime = Util.getMillis();
        }
    }

    public void onHotbarActionKeyPressed() {
        this.lastSelectionTime = Util.getMillis();
        if (this.isMenuActive()) {
            int i = this.menu.getSelectedSlot();
            if (i != -1) {
                this.menu.selectSlot(i);
            }
        } else {
            this.menu = new SpectatorMenu(this);
        }
    }
}
