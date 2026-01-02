package net.goui.cosmicdungeon.playerclass.api;

import net.goui.cosmicdungeon.CosmicDungeonMod;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class ExtraInventoryScreen extends AbstractContainerScreen<ExtraInventoryMenu> {
    private static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath(
            CosmicDungeonMod.MOD_ID, "textures/gui/container/metalmancer_inventory.png");

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
        g.blit(RenderPipelines.GUI_TEXTURED, TEX, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight, 256, 256);

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
        g.drawString(this.font, Component.literal("Metalmancer"), 8, 6, 0x404040, false);
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
