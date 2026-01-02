// file: src/main/java/net/goui/cosmicdungeon/client/screen/ClassSelectorScreen.java
package net.goui.cosmicdungeon.client.screen;

import net.goui.cosmicdungeon.menu.ClassSelectorMenu;
import net.goui.cosmicdungeon.network.ClassPayloads;
import net.goui.cosmicdungeon.playerclass.api.ClassNet;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.ArrayList;
import java.util.List;

public final class ClassSelectorScreen extends AbstractContainerScreen<ClassSelectorMenu> {

    private boolean loading = true;
    private String activeClass = "";
    private final List<String> available = new ArrayList<>();

    public ClassSelectorScreen(ClassSelectorMenu menu, Inventory inv, Component title) {
        super(menu, inv, Component.empty());
        this.imageWidth = 230;
        this.imageHeight = 190;
    }

    @Override
    protected void init() {
        super.init();
        this.loading = true;
        this.available.clear();
        this.clearWidgets();

        rebuildSelectorWidgets();
        ClassNet.requestSelectorData();
    }

    // IMPORTANT: Screen has a protected rebuildWidgets(), so do NOT define a private method with that name.
    private void rebuildSelectorWidgets() {
        this.clearWidgets();

        int x = this.leftPos;
        int y = this.topPos;

        // Close
        addRenderableWidget(Button.builder(Component.literal("Close"), b -> onClose())
                .bounds(x + this.imageWidth - 54, y + 6, 48, 18)
                .build());

        // Refresh
        addRenderableWidget(Button.builder(Component.literal("↻"), b -> {
                    this.loading = true;
                    rebuildSelectorWidgets();
                    ClassNet.requestSelectorData();
                })
                .bounds(x + this.imageWidth - 78, y + 6, 20, 18)
                .build());

        if (loading) {
            addRenderableWidget(Button.builder(Component.literal("Contacting server…"), b -> {})
                    .bounds(x + 18, y + 34, this.imageWidth - 36, 20)
                    .build());
            return;
        }

        int bx = x + 18;
        int by = y + 34;
        int bw = this.imageWidth - 36;
        int bh = 20;

        int i = 0;
        for (String cls : available) {
            boolean isActive = cls != null && cls.equals(activeClass);
            Component label = Component.literal((isActive ? "✓ " : "") + cls);

            addRenderableWidget(Button.builder(label, b -> {
                        this.loading = true;
                        rebuildSelectorWidgets();
                        ClassNet.requestSelectClass(cls);
                    })
                    .bounds(bx, by + (i * 24), bw, bh)
                    .build());

            i++;
            if (i >= 6) break; // simple for now; expand later if you want scroll
        }
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x1 = this.leftPos;
        int y1 = this.topPos;
        int x2 = x1 + this.imageWidth;
        int y2 = y1 + this.imageHeight;

        g.fill(x1, y1, x2, y2, 0xAA000000);
        g.drawString(this.font, "Class Selector", x1 + 10, y1 + 10, 0xFFFFFFFF, false);

        if (!loading) {
            g.drawString(this.font, "Current: " + (activeClass == null ? "" : activeClass), x1 + 10, y1 + 22, 0xFFCCCCCC, false);
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g, mouseX, mouseY, partialTick);
        super.render(g, mouseX, mouseY, partialTick);
        this.renderTooltip(g, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        // no default labels
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /* -------------------- network entry points -------------------- */

    public static void onSelectorData(ClassPayloads.S2C_SelectorData payload) {
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> {
            if (!(mc.screen instanceof ClassSelectorScreen screen)) return;

            screen.loading = false;
            screen.activeClass = payload.activeClassId() == null ? "" : payload.activeClassId();

            screen.available.clear();
            if (payload.availableClassIds() != null) screen.available.addAll(payload.availableClassIds());

            screen.rebuildSelectorWidgets();
        });
    }

    public static void onSelectResult(ClassPayloads.S2C_SelectResult payload) {
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> {
            if (mc.player != null) {
                ChatFormatting col = payload.ok() ? ChatFormatting.GREEN : ChatFormatting.RED;
                mc.player.displayClientMessage(Component.literal(payload.message()).withStyle(col), true);
            }

            if (mc.screen instanceof ClassSelectorScreen screen) {
                // Always refresh after select attempt
                screen.loading = true;
                screen.rebuildSelectorWidgets();
                ClassNet.requestSelectorData();
            }
        });
    }
}
