// file: src/main/java/net/goui/cosmicdungeon/client/screen/ClassSelectorScreen.java
package net.goui.cosmicdungeon.client.screen;

import net.goui.cosmicdungeon.menu.ClassSelectorMenu;
import net.goui.cosmicdungeon.network.ClassPayloads;
import net.goui.cosmicdungeon.playerclass.api.ClassKeys;
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

    // --- scrolling list state ---
    private final List<Button> classButtons = new ArrayList<>();
    private int scrollOffsetPx = 0;
    private int maxScrollPx = 0;

    // list viewport (computed during rebuild)
    private int listX, listY, listW, listH;

    // layout tuning
    private static final int HEADER_H = 34;          // top padding + title/current text zone
    private static final int LIST_PAD_BOTTOM = 10;   // bottom padding
    private static final int ROW_SPACING = 24;       // vertical step between buttons
    private static final int BTN_H = 20;

    // scissor handling (we enable inside renderBg and disable after super.render)
    private boolean scissorEnabledThisFrame = false;

    public ClassSelectorScreen(ClassSelectorMenu menu, Inventory inv, Component title) {
        super(menu, inv, Component.empty());
        this.imageWidth = 230;
        this.imageHeight = 190;
    }

    /* -------------------- localization helpers -------------------- */

    private static Component className(String classId) {
        if (classId == null || classId.isBlank()) return Component.empty();

        // Normalize to known IDs so hacked/unknown ids don't become missing lang spam.
        classId = ClassKeys.clamp(classId);

        if (ClassKeys.CLASS_ID_NONE.equals(classId)) {
            return Component.translatable("playerclass.cosmicdungeon.none");
        }

        return Component.translatable("playerclass.cosmicdungeon." + classId);
    }

    private static Component classButtonLabel(String classId, boolean isActive) {
        Component name = className(classId);

        // Keep your "✓ " prefix, but don't lose localization.
        if (isActive) {
            return Component.literal("✓ ").append(name);
        }
        return name;
    }

    private static boolean isDisabledClassSelection(String classId) {
        return ClassKeys.CLASS_ID_METALMANCER.equals(classId)
                || ClassKeys.CLASS_ID_DEADEYE.equals(classId);
    }

    @Override
    protected void init() {
        super.init();
        this.loading = true;
        this.available.clear();
        this.scrollOffsetPx = 0;
        this.maxScrollPx = 0;
        this.clearWidgets();
        this.classButtons.clear();

        rebuildSelectorWidgets();
        ClassNet.requestSelectorData();
    }

    // IMPORTANT: Screen has a protected rebuildWidgets(), so do NOT define a private method with that name.
    private void rebuildSelectorWidgets() {
        this.clearWidgets();
        this.classButtons.clear();
        this.scissorEnabledThisFrame = false;

        int x = this.leftPos;
        int y = this.topPos;

        // Close
        addRenderableWidget(Button.builder(Component.literal("Close"), b -> onClose())
                .bounds(x + this.imageWidth - 54, y + 6, 48, 18)
                .build());

        // Refresh
        addRenderableWidget(Button.builder(Component.literal("↻"), b -> {
                    this.loading = true;
                    this.scrollOffsetPx = 0;
                    rebuildSelectorWidgets();
                    ClassNet.requestSelectorData();
                })
                .bounds(x + this.imageWidth - 78, y + 6, 20, 18)
                .build());

        // Compute list viewport area (used for scissor + scroll hit-test)
        this.listX = x + 18;
        this.listY = y + HEADER_H;
        this.listW = this.imageWidth - 36;
        this.listH = this.imageHeight - HEADER_H - LIST_PAD_BOTTOM;

        if (loading) {
            addRenderableWidget(Button.builder(Component.literal("Contacting server…"), b -> {})
                    .bounds(x + 18, y + HEADER_H, this.imageWidth - 36, 20)
                    .build());
            return;
        }

        // Build class buttons (positions set in updateButtonLayout())
        int i = 0;
        for (String cls : available) {
            boolean isActive = cls != null && cls.equals(activeClass);

            Button btn = Button.builder(classButtonLabel(cls, isActive), b -> {
                        if (isDisabledClassSelection(cls)) return;

                        this.loading = true;
                        rebuildSelectorWidgets();

                        // IMPORTANT: still send the RAW ID to server
                        ClassNet.requestSelectClass(cls);
                    })
                    // temporary bounds; real y is applied in updateButtonLayout()
                    .bounds(listX, listY + (i * ROW_SPACING), listW, BTN_H)
                    .build();

            this.classButtons.add(btn);
            addRenderableWidget(btn);
            i++;
        }

        recomputeMaxScroll();
        clampScroll();
        updateButtonLayout();
    }

    private void recomputeMaxScroll() {
        int totalContentH = this.available.size() * ROW_SPACING;
        this.maxScrollPx = Math.max(0, totalContentH - this.listH);
    }

    private void clampScroll() {
        if (this.scrollOffsetPx < 0) this.scrollOffsetPx = 0;
        if (this.scrollOffsetPx > this.maxScrollPx) this.scrollOffsetPx = this.maxScrollPx;
    }

    private void updateButtonLayout() {
        for (int i = 0; i < this.classButtons.size(); i++) {
            Button btn = this.classButtons.get(i);

            int y = this.listY + (i * ROW_SPACING) - this.scrollOffsetPx;

            // move button
            btn.setX(this.listX);
            btn.setY(y);

            // only show buttons inside (or slightly overlapping) the viewport
            boolean inView = (y + BTN_H) > this.listY && y < (this.listY + this.listH);
            btn.visible = inView;
            btn.active = !isDisabledClassSelection(this.available.get(i)); // disabled class buttons stay shaded and unclickable
        }
    }

    private boolean isMouseOverList(double mouseX, double mouseY) {
        return mouseX >= this.listX && mouseX < (this.listX + this.listW)
                && mouseY >= this.listY && mouseY < (this.listY + this.listH);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        // Only scroll when hovering the list area and there is something to scroll
        if (!this.loading && this.maxScrollPx > 0 && isMouseOverList(mouseX, mouseY)) {
            // wheel up (positive) should move content up => decrease offset
            int delta = (int) Math.round(scrollY * ROW_SPACING);
            this.scrollOffsetPx -= delta;

            clampScroll();
            updateButtonLayout();
            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x1 = this.leftPos;
        int y1 = this.topPos;
        int x2 = x1 + this.imageWidth;
        int y2 = y1 + this.imageHeight;

        // panel background
        g.fill(x1, y1, x2, y2, 0xAA000000);

        // header text
        g.drawString(this.font, "Class Selector", x1 + 10, y1 + 10, 0xFFFFFFFF, false);

        if (!loading) {
            Component current = className(activeClass);
            Component line = Component.literal("Current: ").append(current);

            g.drawString(this.font, line, x1 + 10, y1 + 22, 0xFFCCCCCC, false);
        }

        // list viewport background (slightly different shade)
        g.fill(this.listX, this.listY, this.listX + this.listW, this.listY + this.listH, 0x33000000);

        // enable scissor so the button list clips inside list viewport
        g.enableScissor(this.listX, this.listY, this.listX + this.listW, this.listY + this.listH);
        this.scissorEnabledThisFrame = true;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g, mouseX, mouseY, partialTick);

        // super.render will call renderBg (which enables scissor), then render widgets (clipped)
        super.render(g, mouseX, mouseY, partialTick);

        // IMPORTANT: disable scissor after widgets so tooltips + other overlays render normally
        if (this.scissorEnabledThisFrame) {
            g.disableScissor();
            this.scissorEnabledThisFrame = false;
        }

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

            // reset scroll when new data arrives (optional; remove if you want to keep scroll position)
            screen.scrollOffsetPx = 0;

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
                screen.scrollOffsetPx = 0;
                screen.rebuildSelectorWidgets();
                ClassNet.requestSelectorData();
            }
        });
    }
}
