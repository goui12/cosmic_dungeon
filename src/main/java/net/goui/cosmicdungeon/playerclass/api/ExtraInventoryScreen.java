package net.goui.cosmicdungeon.playerclass.api;

import net.goui.cosmicdungeon.client.screen.ClassInventoryPresentation;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class ExtraInventoryScreen extends AbstractContainerScreen<ExtraInventoryMenu> {
    // store mouse like vanilla; renderBg will read these
    private float xMouse;
    private float yMouse;

    public ExtraInventoryScreen(ExtraInventoryMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 184; // one row taller than vanilla
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics g, float pt, int mx, int my) {
        // backdrop
        ClassInventoryPresentation.background(g, this);

        // ---- Player preview (use the 10-arg rectangle API) ----
        // Pick a rectangle inside your black box (tweak if needed to fit your PNG)
        int x1 = this.leftPos + 22;  // left
        int y1 = this.topPos  + 2;  // top
        int x2 = this.leftPos + 78;  // right
        int y2 = this.topPos  + 82;  // bottom
        int scale = 28;
        float yOffset = 0.0625F;     // same offset vanilla uses

        InventoryScreen.renderEntityInInventoryFollowsMouse(
                g, x1, y1, x2, y2, scale, yOffset, this.xMouse, this.yMouse, this.minecraft.player);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        // Suppress vanilla labels and draw your own
        ClassInventoryPresentation.labels(g);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g, mouseX, mouseY, partialTick);
        super.render(g, mouseX, mouseY, partialTick);

        // update after super (matches vanilla pattern; renderBg uses previous values)
        this.xMouse = mouseX;
        this.yMouse = mouseY;

        this.renderTooltip(g, mouseX, mouseY);
    }
}
