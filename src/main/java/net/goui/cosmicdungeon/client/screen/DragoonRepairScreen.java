package net.goui.cosmicdungeon.client.screen;

import net.goui.cosmicdungeon.CosmicDungeonMod;
import net.goui.cosmicdungeon.economy.CurrencyAmount;
import net.goui.cosmicdungeon.network.DragoonRepairPayloads;
import net.goui.cosmicdungeon.network.ModNetwork;
import net.goui.cosmicdungeon.playerclass.dragoon.repair.DragoonRepairMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.UUID;
import java.util.function.BooleanSupplier;

public class DragoonRepairScreen extends AbstractContainerScreen<DragoonRepairMenu> {
    private static final ResourceLocation BG = ResourceLocation.fromNamespaceAndPath(CosmicDungeonMod.MOD_ID, "textures/gui/container/repair_affinity_window.png");
    private static final ResourceLocation BUTTON = ResourceLocation.fromNamespaceAndPath(CosmicDungeonMod.MOD_ID, "textures/gui/repair_affinity_button.png");
    private static final ResourceLocation BUTTON_HOVER = ResourceLocation.fromNamespaceAndPath(CosmicDungeonMod.MOD_ID, "textures/gui/repair_affinity_button_hover.png");
    private static final ResourceLocation BUTTON_DISABLED = ResourceLocation.fromNamespaceAndPath(CosmicDungeonMod.MOD_ID, "textures/gui/repair_affinity_button_disabled.png");
    private static final ResourceLocation BUTTON_SELECTED = ResourceLocation.fromNamespaceAndPath(CosmicDungeonMod.MOD_ID, "textures/gui/repair_affinity_button_selected.png");
    private static final ResourceLocation SMALL_BUTTON = ResourceLocation.fromNamespaceAndPath(CosmicDungeonMod.MOD_ID, "textures/gui/repair_affinity_small_button.png");
    private static final ResourceLocation SMALL_BUTTON_HOVER = ResourceLocation.fromNamespaceAndPath(CosmicDungeonMod.MOD_ID, "textures/gui/repair_affinity_small_button_hover.png");
    private static final ResourceLocation SMALL_BUTTON_DISABLED = ResourceLocation.fromNamespaceAndPath(CosmicDungeonMod.MOD_ID, "textures/gui/repair_affinity_small_button_disabled.png");

    public DragoonRepairScreen(DragoonRepairMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        imageWidth = 256;
        imageHeight = 256;
        inventoryLabelY = 162;
    }

    @Override
    protected void init() {
        RepairClientState.clear();
        super.init();
        int x = leftPos;
        int y = topPos;
        addRenderableWidget(smallButton(x + 24, y + 92, "-M", () -> canCustomerEdit(), () -> false, () -> ModNetwork.sendToServer(new DragoonRepairPayloads.C2S_AdjustFee("mark", -1))));
        addRenderableWidget(smallButton(x + 54, y + 92, "+M", () -> canCustomerEdit(), () -> false, () -> ModNetwork.sendToServer(new DragoonRepairPayloads.C2S_AdjustFee("mark", 1))));
        addRenderableWidget(smallButton(x + 84, y + 92, "-T", () -> canCustomerEdit(), () -> false, () -> ModNetwork.sendToServer(new DragoonRepairPayloads.C2S_AdjustFee("trace", -1))));
        addRenderableWidget(smallButton(x + 114, y + 92, "+T", () -> canCustomerEdit(), () -> false, () -> ModNetwork.sendToServer(new DragoonRepairPayloads.C2S_AdjustFee("trace", 1))));
        for (int i = 1; i <= 4; i++) {
            final int units = i;
            addRenderableWidget(repairButton(x + 24 + (i - 1) * 52, y + 116, label(i), () -> canCustomerEdit() && hasRepairItem(), () -> currentUnits() == units, () -> ModNetwork.sendToServer(new DragoonRepairPayloads.C2S_SelectUnits(units))));
        }
        addRenderableWidget(repairButton(x + 34, y + 140, "Ready", () -> canCustomerEdit(), () -> currentViewReady(), () -> ModNetwork.sendToServer(new DragoonRepairPayloads.C2S_TargetReady(true))));
        addRenderableWidget(repairButton(x + 158, y + 140, "Repair", () -> canDragoonRepair(), () -> false, () -> ModNetwork.sendToServer(new DragoonRepairPayloads.C2S_Repair())));
        addRenderableWidget(repairButton(x + 96, y + 8, "Cancel", () -> RepairClientState.currentFor(menu.containerId) != null, () -> false, () -> ModNetwork.sendToServer(new DragoonRepairPayloads.C2S_Cancel())));
    }

    @Override
    public void removed() {
        RepairClientState.clear();
        super.removed();
    }

    public int containerId() {
        return menu.containerId;
    }

    private TexturedRepairButton repairButton(int x, int y, String label, BooleanSupplier active, BooleanSupplier selected, Runnable press) {
        return new TexturedRepairButton(x, y, 64, 18, Component.literal(label), press, false, active, selected);
    }

    private TexturedRepairButton smallButton(int x, int y, String label, BooleanSupplier active, BooleanSupplier selected, Runnable press) {
        return new TexturedRepairButton(x, y, 18, 18, Component.literal(label), press, true, active, selected);
    }

    private boolean canCustomerEdit() {
        RepairClientState.View view = RepairClientState.currentFor(menu.containerId);
        return view != null && !view.viewerDragoon() && !view.targetReady() && !view.dragoonRepairing();
    }

    private boolean canDragoonRepair() {
        RepairClientState.View view = RepairClientState.currentFor(menu.containerId);
        return view != null && view.viewerDragoon() && view.targetReady() && !view.dragoonRepairing();
    }

    private boolean hasRepairItem() {
        RepairClientState.View view = RepairClientState.currentFor(menu.containerId);
        return view != null && view.requiredUnitsToFull() > 0;
    }

    private int currentUnits() {
        RepairClientState.View view = RepairClientState.currentFor(menu.containerId);
        return view == null ? -1 : view.selectedUnits();
    }

    private boolean currentViewReady() {
        RepairClientState.View view = RepairClientState.currentFor(menu.containerId);
        return view != null && view.targetReady();
    }

    private static String label(int i) { return switch (i) { case 1 -> "Light"; case 2 -> "Standard"; case 3 -> "Heavy"; default -> "Full"; }; }

    @Override
    protected void renderBg(GuiGraphics g, float partial, int mouseX, int mouseY) {
        g.blit(RenderPipelines.GUI_TEXTURED, BG, leftPos, topPos, 0.0F, 0.0F, imageWidth, imageHeight, imageWidth, imageHeight);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        RepairClientState.View v = RepairClientState.currentFor(menu.containerId);
        g.drawString(font, "Repair Affinity", 88, 16, 0x404040, false);
        if (v == null) { g.drawString(font, "Waiting for server...", 70, 72, 0xAA0000, false); return; }
        g.drawString(font, v.viewerDragoon() ? "You are the Dragoon" : "You are the customer", 22, 30, 0x404040, false);
        g.drawString(font, "Customer: " + v.targetName(), 22, 64, 0x404040, false);
        g.drawString(font, "Fee: " + CurrencyAmount.ofTrace(v.offeredFeeTrace()).formatNormalized(), 22, 82, 0x404040, false);
        g.drawString(font, "Dragoon: " + v.dragoonName(), 132, 48, 0x404040, false);
        g.drawString(font, "Material: " + v.materialDisplay() + " x" + v.requiredMaterialCount(), 132, 66, v.dragoonHasMaterial() ? 0x007700 : 0xAA0000, false);
        g.drawString(font, "Units: " + v.selectedUnits() + "/" + v.requiredUnitsToFull(), 94, 111, 0x404040, false);
        g.drawString(font, v.targetReady() ? "Customer ready" : "Customer not ready", 132, 84, v.targetReady() ? 0x007700 : 0xAA0000, false);
        g.drawString(font, v.statusMessage(), 22, 154, 0x404040, false);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        renderBackground(g, mouseX, mouseY, partial);
        super.render(g, mouseX, mouseY, partial);
        renderTooltip(g, mouseX, mouseY);
    }

    private static final class TexturedRepairButton extends Button {
        private final boolean small;
        private final BooleanSupplier activeSupplier;
        private final BooleanSupplier selectedSupplier;

        private TexturedRepairButton(int x, int y, int w, int h, Component msg, Runnable press, boolean small, BooleanSupplier activeSupplier, BooleanSupplier selectedSupplier) {
            super(x, y, w, h, msg, button -> press.run(), DEFAULT_NARRATION);
            this.small = small;
            this.activeSupplier = activeSupplier;
            this.selectedSupplier = selectedSupplier;
        }


        @Override
        protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
            active = activeSupplier.getAsBoolean();
            ResourceLocation texture = texture();
            int textureWidth = small ? 18 : 64;
            g.blit(RenderPipelines.GUI_TEXTURED, texture, getX(), getY(), 0.0F, 0.0F, width, height, textureWidth, 18);
            int color = active ? 0xFFFFFF : 0xA0A0A0;
            g.drawCenteredString(Minecraft.getInstance().font, getMessage(), getX() + width / 2, getY() + (height - 8) / 2, color);
        }

        private ResourceLocation texture() {
            if (!active) return small ? SMALL_BUTTON_DISABLED : BUTTON_DISABLED;
            if (selectedSupplier.getAsBoolean() && !small) return BUTTON_SELECTED;
            if (isHoveredOrFocused()) return small ? SMALL_BUTTON_HOVER : BUTTON_HOVER;
            return small ? SMALL_BUTTON : BUTTON;
        }
    }

    public static final class RepairClientState {
        private static View current;
        public static void clear() { current = null; }
        public static void set(View v) { current = v; }
        public static boolean setIfCurrent(int containerId, View v) { if (!isCurrentRepairContainer(containerId)) return false; current = v; return true; }
        public static View current() { return current; }
        public static View currentFor(int containerId) { return current != null && current.containerId() == containerId ? current : null; }
        private static boolean isCurrentRepairContainer(int containerId) { Screen screen = Minecraft.getInstance().screen; return screen instanceof DragoonRepairScreen repairScreen && repairScreen.containerId() == containerId; }
        public record View(int containerId, UUID sessionId, String dragoonName, String targetName, boolean viewerDragoon, long offeredFeeTrace, long targetBalanceTrace, long dragoonCapacityTrace, int selectedUnits, int requiredUnitsToFull, String materialItemId, String materialDisplay, int requiredMaterialCount, boolean dragoonHasMaterial, boolean targetReady, boolean dragoonRepairing, String statusMessage) {}
    }
}
