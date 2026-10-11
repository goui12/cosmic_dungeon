package net.goui.cosmicdungeon.client.screen.crafting;

import java.util.function.Consumer;
import net.goui.cosmicdungeon.client.screen.ClassInventoryPresentation;
import net.goui.cosmicdungeon.client.screen.DeferredTooltipAccess;
import net.goui.cosmicdungeon.network.PotionCraftPayloads.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.*;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;

/** Client-only palette; recipes, prices, custody and inventory placement remain server authoritative. */
@EventBusSubscriber(modid = "cosmicdungeon", value = Dist.CLIENT)
public final class PotionCraftClient {
    private static final PotionPaletteState STATE = new PotionPaletteState();
    private static AbstractContainerScreen<?> screen;
    private static int pressedButtons;
    private PotionCraftClient() {}
    public static void actions(Consumer<Action> actions, Consumer<Request> requests) { STATE.actions(actions, requests); }
    public static void receive(View view) { STATE.receive(view); }
    public static void clear() { STATE.clear(); screen = null; pressedButtons = 0; }
    public static boolean brewer(String id) { return "theurgist".equals(id) || "venefex".equals(id); }
    private static boolean dragging(AbstractContainerScreen<?> candidate) {
        return !((net.goui.cosmicdungeon.mixin.client.InventoryDragAccess)candidate).cosmicdungeon$draggingItem().isEmpty();
    }
    private static boolean eligible(AbstractContainerScreen<?> candidate) {
        var mc = Minecraft.getInstance();
        return mc.screen == candidate && mc.player != null && mc.player.isAlive()
                && mc.player.containerMenu == candidate.getMenu() && brewer(ClassInventoryPresentation.theme().classId());
    }
    private static boolean current(Screen candidate) {
        if (!STATE.open() || candidate != screen) return false;
        if (eligible(screen)) return true;
        close(); return false;
    }
    private static void close() { STATE.close(); screen = null; pressedButtons = 0; }
    private static PotionPaletteLayout layout() {
        return PotionPaletteLayout.of(screen.width, screen.height, screen.getGuiLeft(), screen.getGuiTop(), screen.getXSize(), STATE.entries().size());
    }
    public static boolean ownsInput(Screen candidate, double x, double y) {
        return current(candidate) && layout().panel().contains(x, y);
    }
    @SubscribeEvent public static void init(ScreenEvent.Init.Post event) {
        if (ClassInventoryPresentation.supports(event.getScreen()))
            event.addListener(new CraftButton((AbstractContainerScreen<?>)event.getScreen()));
    }
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void render(ScreenEvent.Render.Post event) {
        if (!current(event.getScreen())) return;
        var geometry = layout(); var graphics = event.getGuiGraphics();
        int mx = event.getMouseX(), my = event.getMouseY();
        if (geometry.panel().contains(mx, my)) ((DeferredTooltipAccess)graphics).cosmicdungeon$clearTooltip();
        PotionPaletteComponent.draw(graphics, geometry, STATE, dragging(screen) ? -1 : mx, dragging(screen) ? -1 : my);
        screen.renderCarriedItem(graphics, mx, my);
    }
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void press(ScreenEvent.MouseButtonPressed.Pre event) {
        if (!ownsInput(event.getScreen(), event.getMouseX(), event.getMouseY())) return;
        event.setCanceled(true);
        int button = event.getMouseButtonEvent().button();
        if (button >= 0 && button < 8) pressedButtons |= 1 << button;
        if (button != 0 || dragging(screen)) return;
        var geometry = layout();
        for (int i = 0; i < 3; i++) if (geometry.variants().get(i).contains(event.getMouseX(), event.getMouseY())) {
            STATE.variant(PotionPaletteModel.Variant.values()[i]); return;
        }
        var entries = STATE.entries();
        for (int i = 0; i < entries.size(); i++) if (geometry.tiles().get(i).contains(event.getMouseX(), event.getMouseY())) {
            STATE.craft(entries.get(i).targetId(), Minecraft.getInstance().hasShiftDown()); return;
        }
    }
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void release(ScreenEvent.MouseButtonReleased.Pre event) {
        int button = event.getButton();
        if (button < 0 || button >= 8) return;
        int mask = 1 << button;
        if ((pressedButtons & mask) != 0) { event.setCanceled(true); pressedButtons &= ~mask; }
    }
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void drag(ScreenEvent.MouseDragged.Pre event) {
        if (current(event.getScreen()) && (pressedButtons != 0 || ownsInput(event.getScreen(), event.getMouseX(), event.getMouseY())))
            event.setCanceled(true);
    }
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void scroll(ScreenEvent.MouseScrolled.Pre event) {
        if (ownsInput(event.getScreen(), event.getMouseX(), event.getMouseY())) event.setCanceled(true);
    }
    @SubscribeEvent public static void closing(ScreenEvent.Closing event) { if (screen == event.getScreen()) close(); }
    @SubscribeEvent public static void login(ClientPlayerNetworkEvent.LoggingIn event) { clear(); }
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event) { clear(); }

    private static final class CraftButton extends Button {
        private final AbstractContainerScreen<?> parent;
        CraftButton(AbstractContainerScreen<?> parent) {
            super(parent.getGuiLeft() + 98, parent.getGuiTop() + 25, 73, 20, Component.literal("Craft..."),
                    button -> ((CraftButton)button).toggle(), DEFAULT_NARRATION);
            this.parent = parent;
            setTooltip(Tooltip.create(Component.literal("Open potion recipes. Click to craft one; Shift-click places one in your inventory, hotbar first.")));
        }
        private void toggle() {
            if (!eligible(parent) || dragging(parent)) return;
            if (screen != null && screen != parent) close();
            screen = parent; STATE.toggle();
            if (!STATE.open()) screen = null;
        }
        @Override protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            active = eligible(parent) && !dragging(parent);
            if (!brewer(ClassInventoryPresentation.theme().classId())) return;
            setMessage(Component.literal(STATE.open() && screen == parent ? "Craft -" : "Craft..."));
            super.renderWidget(graphics, mouseX, mouseY, partialTick);
        }
    }
}
