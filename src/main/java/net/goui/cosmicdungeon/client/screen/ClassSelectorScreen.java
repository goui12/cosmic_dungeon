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
    private final D1PartyPanel partyPanel = new D1PartyPanel();
    private final TamsinTaxPanel taxPanel = new TamsinTaxPanel();
    private String activeClass = "";
    private net.goui.cosmicdungeon.npc.tamsin.TamsinFlow.Stage stage =
            net.goui.cosmicdungeon.npc.tamsin.TamsinFlow.Stage.SELECTOR;
    private static final net.minecraft.resources.ResourceLocation TAMSIN_MAP =
            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("cosmicdungeon", "textures/gui/tamsin_d1_map.png");
    private boolean mapArtwork;

    private final List<String> available = new ArrayList<>();

    // --- scrolling list state ---
    private final List<Button> classButtons = new ArrayList<>();
    private int scrollOffsetPx = 0;
    private int maxScrollPx = 0;

    // list viewport (computed during rebuild)
    private int listX, listY, listW, listH;

    // layout tuning
    private static final int HEADER_H = 34;          // top padding + title/current text zone
    private static final int LIST_PAD_BOTTOM = 36;   // bottom padding
    private static final int ROW_SPACING = 24;       // vertical step between buttons
    private static final int BTN_H = 20;

    // scissor handling (we enable inside renderBg and disable after super.render)
    private boolean scissorEnabledThisFrame = false;

    public ClassSelectorScreen(ClassSelectorMenu menu, Inventory inv, Component title) {
        super(menu, inv, Component.empty());
        this.imageWidth = 360;
        this.imageHeight = 240;
    }

    /* -------------------- localization helpers -------------------- */

    static Component className(String classId) {
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
        this.mapArtwork = this.minecraft != null && this.minecraft.getResourceManager().getResource(TAMSIN_MAP).isPresent();
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

        if (!loading && stage == net.goui.cosmicdungeon.npc.tamsin.TamsinFlow.Stage.MAP) {
            var map = TamsinMapLayout.forViewport(this.width, this.height);
            this.imageWidth = map.width();
            this.imageHeight = map.height();
        } else {
            this.imageWidth = 360;
            this.imageHeight = 240;
        }
        this.leftPos = (this.width - this.imageWidth) / 2;
        this.topPos = (this.height - this.imageHeight) / 2;
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

        if (stage == net.goui.cosmicdungeon.npc.tamsin.TamsinFlow.Stage.AGREEMENT) {
            addRenderableWidget(Button.builder(Component.literal("Yes"), b -> tamsinAction("yes"))
                    .bounds(x + 18, y + imageHeight - 27, 92, 20).build());
            addRenderableWidget(Button.builder(Component.literal("No"), b -> tamsinAction("no"))
                    .bounds(x + 120, y + imageHeight - 27, 92, 20).build());
            return;
        }
        if (stage == net.goui.cosmicdungeon.npc.tamsin.TamsinFlow.Stage.MAP) {
            addRenderableWidget(Button.builder(Component.literal("Continue"), b -> tamsinAction("continue"))
                    .bounds(x + 18, y + imageHeight - 27, imageWidth - 36, 20).build());
            return;
        }

        if (stage == net.goui.cosmicdungeon.npc.tamsin.TamsinFlow.Stage.TAX) {
            taxPanel.build(font, widget -> addRenderableWidget(widget), this::rebuildSelectorWidgets, x, y, menu.containerId);
            return;
        }
        if (stage == net.goui.cosmicdungeon.npc.tamsin.TamsinFlow.Stage.READY) {
            partyPanel.build(font, widget -> addRenderableWidget(widget), x, y, menu.containerId);
            return;
        }
        var group = addRenderableWidget(Button.builder(Component.literal("Group"), button ->
                net.goui.cosmicdungeon.network.ModNetwork.sendToServer(
                        new net.goui.cosmicdungeon.network.PartyPayloads.Action(menu.containerId, 0, "group", "")))
                .bounds(x + 18, y + imageHeight - 27, imageWidth - 36, 20).build());
        group.active = activeClass != null && !activeClass.isBlank() && !ClassKeys.CLASS_ID_NONE.equals(activeClass)
                && !isDisabledClassSelection(activeClass);
        // Build class buttons (positions set in updateButtonLayout())
        int i = 0;
        for (String cls : available) {
            if (isDisabledClassSelection(cls) || ClassKeys.CLASS_ID_NONE.equals(cls)) continue;
            boolean isActive = cls != null && cls.equals(activeClass);

            Button btn = Button.builder(classButtonLabel(cls, isActive), b -> {
                        if (isDisabledClassSelection(cls)) return;

                        this.loading = true;
                        rebuildSelectorWidgets();

                        // IMPORTANT: still send the RAW ID to server
                        ClassNet.requestSelectClass(menu.containerId, cls);
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

    private void tamsinAction(String action) {
        this.loading = true;
        rebuildSelectorWidgets();
        net.goui.cosmicdungeon.network.ModNetwork.sendToServer(new ClassPayloads.C2S_TamsinAction(menu.containerId, action));
    }

    private int paragraph(GuiGraphics graphics, String text, int x, int y, int width, int color) {
        for (var line : font.split(Component.literal(text), width)) {
            graphics.drawString(font, line, x, y, color, false);
            y += font.lineHeight + 3;
        }
        return y;
    }

    private boolean renderConversation(GuiGraphics g, int x, int y) {
        switch (stage) {
            case AGREEMENT -> {
                int next = paragraph(g, "I found a map to something valuable underground.",
                        x + 18, y + 43, imageWidth - 36, 0xFFFFFFFF);
                next = paragraph(g, "If you survive, I want a cut of what you bring back.",
                        x + 18, next + 12, imageWidth - 36, 0xFFFFFFFF);
                paragraph(g, "Do we have an agreement?", x + 18, next + 12, imageWidth - 36, 0xFFFFFFAA);
                return true;
            }
            case MAP -> {
                g.drawString(font, "A route into the depths", x + 18, y + 32, 0xFFFFFFFF, false);
                int edge = imageHeight - 90;
                int mx = x + (imageWidth - edge) / 2, my = y + 44;
                if (mapArtwork) {
                    // Full square 516px artwork; transparent tattered edges are preserved.
                    g.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, TAMSIN_MAP,
                            mx, my, 0.0F, 0.0F, edge, edge, TamsinMapLayout.TEXTURE_SIZE,
                            TamsinMapLayout.TEXTURE_SIZE, TamsinMapLayout.TEXTURE_SIZE, TamsinMapLayout.TEXTURE_SIZE);
                } else {
                    g.drawString(font, "Map artwork unavailable.", mx, my + edge / 2, 0xFFCCCCCC, false);
                }
                g.drawString(font, "Choose a class, then start your adventure.", x + 18,
                        my + edge + 7, 0xFFCCCCCC, false);
                return true;
            }
            case TAX -> { taxPanel.render(g, font, x, y); return true; }
            case READY -> {
                partyPanel.render(g, font, x, y);
                return true;
            }
            default -> { return false; }
        }
    }

    private void recomputeMaxScroll() {
        int totalContentH = this.classButtons.size() * ROW_SPACING;
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
            boolean inView = y >= this.listY && y + BTN_H <= this.listY + this.listH;
            btn.visible = inView;
            btn.active = true; // disabled class buttons stay shaded and unclickable
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
        g.drawString(this.font, stage == net.goui.cosmicdungeon.npc.tamsin.TamsinFlow.Stage.SELECTOR
                ? "D1 Class Selector" : "Tamsin Vane", x1 + 10, y1 + 10, 0xFFFFFFFF, false);

        if (!loading && renderConversation(g, x1, y1)) return;
        if (!loading) {
            Component current = className(activeClass);
            Component line = Component.literal("Current: ").append(current);

            g.drawString(this.font, line, x1 + 10, y1 + 22, 0xFFCCCCCC, false);
        }

        // list viewport background (slightly different shade)
        g.fill(this.listX, this.listY, this.listX + this.listW, this.listY + this.listH, 0x33000000);

        // enable scissor so the button list clips inside list viewport
        // Class rows are fully bounded; header and Ready buttons must not be scissored away.
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

    public static void onTaxView(net.goui.cosmicdungeon.network.TamsinTaxPayloads.View payload) {
        Minecraft.getInstance().execute(() -> {
            if (!(Minecraft.getInstance().screen instanceof ClassSelectorScreen screen)
                    || screen.menu.containerId != payload.containerId()) return;
            screen.taxPanel.setView(payload);
            screen.rebuildSelectorWidgets();
        });
    }
    public static void onPartyView(net.goui.cosmicdungeon.network.PartyPayloads.View payload) {
        Minecraft.getInstance().execute(() -> {
            if (!(Minecraft.getInstance().screen instanceof ClassSelectorScreen screen)
                    || screen.menu.containerId != payload.containerId()) return;
            screen.partyPanel.setView(payload);
            screen.rebuildSelectorWidgets();
        });
    }

    public static void onSelectorData(ClassPayloads.S2C_SelectorData payload) {
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> {
            if (!(mc.screen instanceof ClassSelectorScreen screen) || screen.menu.containerId != payload.containerId()) return;
            try { screen.stage = net.goui.cosmicdungeon.npc.tamsin.TamsinFlow.Stage.valueOf(payload.stage()); }
            catch (IllegalArgumentException invalid) { return; }
            screen.loading = false;
            screen.activeClass = payload.activeClassId() == null ? "" : payload.activeClassId();

            screen.available.clear();
            if (payload.availableClassIds() != null) screen.available.addAll(payload.availableClassIds().stream()
                    .filter(id -> !isDisabledClassSelection(id) && !ClassKeys.CLASS_ID_NONE.equals(id)).toList());

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
