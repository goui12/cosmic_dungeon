package net.goui.cosmicdungeon.client.screen.crafting;

import java.util.*;
import java.util.function.Consumer;
import net.goui.cosmicdungeon.client.screen.ClassInventoryPresentation;
import net.goui.cosmicdungeon.client.screen.DeferredTooltipAccess;
import net.goui.cosmicdungeon.crafting.PotionRecipeCatalog;
import net.goui.cosmicdungeon.network.PotionCraftPayloads.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.alchemy.PotionContents;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.*;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;

/** Always-visible class inventory list; server quotes remain the only authority for crafting. */
@EventBusSubscriber(modid = "cosmicdungeon", value = Dist.CLIENT)
public final class PotionCraftClient {
    private static final PotionPaletteState STATE = new PotionPaletteState();
    private static AbstractContainerScreen<?> screen;
    private static String role = "";
    private static int pressedButtons, retryTicks;
    private static double scrollGrab = -1;
    private static FeatureFlagSet catalogFlags;
    private static PotionRecipeCatalog catalog;
    private PotionCraftClient() {}
    public static void actions(Consumer<Action> actions, Consumer<Request> requests) { STATE.actions(actions, requests); }
    public static void receive(View view) { STATE.receive(view); }
    public static void clear() { STATE.clear(); screen = null; role = ""; pressedButtons = retryTicks = 0; scrollGrab = -1; }
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
        if (!ClassInventoryPresentation.supports(candidate) || !eligible((AbstractContainerScreen<?>)candidate)) {
            if (candidate == screen) close();
            return false;
        }
        String id = ClassInventoryPresentation.theme().classId();
        if (candidate != screen || !id.equals(role)) {
            close(); screen = (AbstractContainerScreen<?>)candidate; role = id;
            var flags = Minecraft.getInstance().level.enabledFeatures();
            if (catalog == null || !flags.equals(catalogFlags)) {
                catalog = PotionRecipeCatalog.vanilla(flags); catalogFlags = flags;
            }
            STATE.preview(catalog.targets().stream().filter(t -> t.classes().contains(id))
                    .map(t -> new Entry(t.id(), PotionContents.createItemStack(t.container(), t.potion()), 0, 0, 0, 0, List.of())).toList());
        }
        return STATE.ensureOpen();
    }
    private static void close() { STATE.close(); screen = null; role = ""; pressedButtons = retryTicks = 0; scrollGrab = -1; }
    private static PotionPaletteLayout layout() {
        var geometry = PotionPaletteLayout.of(screen.width, screen.height, screen.getGuiLeft(), screen.getGuiTop(), screen.getXSize(), STATE.entries().size(), STATE.first());
        STATE.first(geometry.first()); return geometry;
    }
    public static boolean inputCaptured(Screen candidate) { return candidate == screen && pressedButtons != 0; }
    public static boolean ownsInput(Screen candidate, double x, double y) { return current(candidate) && layout().panel().contains(x, y); }
    @SubscribeEvent public static void init(ScreenEvent.Init.Post event) { current(event.getScreen()); }
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void render(ScreenEvent.Render.Post event) {
        if (!current(event.getScreen())) return;
        var geometry = layout(); var graphics = event.getGuiGraphics();
        int mx = event.getMouseX(), my = event.getMouseY();
        if (geometry.panel().contains(mx, my)) ((DeferredTooltipAccess)graphics).cosmicdungeon$clearTooltip();
        PotionPaletteComponent.draw(graphics, geometry, STATE, dragging(screen) || scrollGrab >= 0 ? -1 : mx, dragging(screen) || scrollGrab >= 0 ? -1 : my);
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
        if (geometry.maxFirst() > 0 && geometry.track().contains(event.getMouseX(), event.getMouseY())) {
            scrollGrab = geometry.thumb().contains(event.getMouseX(), event.getMouseY()) ? event.getMouseY() - geometry.thumb().y() : geometry.thumb().height() / 2.0;
            STATE.first(geometry.offsetAt(event.getMouseY(), scrollGrab)); return;
        }
        int index = geometry.indexAt(event.getMouseX(), event.getMouseY());
        if (index >= 0) STATE.craft(STATE.entries().get(index).targetId(), Minecraft.getInstance().hasShiftDown());
    }
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void release(ScreenEvent.MouseButtonReleased.Pre event) {
        if (event.getScreen() != screen) return;
        int button = event.getButton();
        if (button < 0 || button >= 8) return;
        int mask = 1 << button;
        if ((pressedButtons & mask) != 0) { event.setCanceled(true); pressedButtons &= ~mask; }
        if (button == 0) scrollGrab = -1;
    }
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void drag(ScreenEvent.MouseDragged.Pre event) {
        if (!current(event.getScreen())) return;
        if (pressedButtons != 0 || ownsInput(event.getScreen(), event.getMouseX(), event.getMouseY())) {
            event.setCanceled(true);
            if (scrollGrab >= 0 && event.getMouseButton() == 0 && !dragging(screen))
                STATE.first(layout().offsetAt(event.getMouseY(), scrollGrab));
        }
    }
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void scroll(ScreenEvent.MouseScrolled.Pre event) {
        if (!ownsInput(event.getScreen(), event.getMouseX(), event.getMouseY())) return;
        event.setCanceled(true);
        if (!dragging(screen) && event.getScrollDeltaY() != 0)
            STATE.first(Math.clamp(STATE.first() + (event.getScrollDeltaY() > 0 ? -1 : 1), 0, layout().maxFirst()));
    }
    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        if (screen != null && Minecraft.getInstance().screen != screen) { close(); return; }
        // Invalid/temporarily unavailable server sessions close themselves. Retry at most once per second.
        if (screen != null && ++retryTicks >= 20) { retryTicks = 0; STATE.refreshUnavailable(); }
    }
    @SubscribeEvent public static void closing(ScreenEvent.Closing event) { if (screen == event.getScreen()) close(); }
    @SubscribeEvent public static void clone(ClientPlayerNetworkEvent.Clone event) { clear(); }
    @SubscribeEvent public static void login(ClientPlayerNetworkEvent.LoggingIn event) { clear(); catalog = null; catalogFlags = null; }
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event) { clear(); catalog = null; catalogFlags = null; }
}
