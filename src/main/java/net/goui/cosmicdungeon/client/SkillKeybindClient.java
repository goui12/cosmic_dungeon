package net.goui.cosmicdungeon.client;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import net.goui.cosmicdungeon.client.SkillInputPolicy.*;
import net.goui.cosmicdungeon.client.screen.ClassInventoryPresentation;
import net.goui.cosmicdungeon.client.screen.ResurrectPickerScreen;
import net.goui.cosmicdungeon.client.screen.crafting.PotionCraftClient;
import net.goui.cosmicdungeon.client.screen.skills.BogatyrClient;
import net.goui.cosmicdungeon.client.screen.skills.SkillsPanelClient;
import net.goui.cosmicdungeon.client.screen.skills.TheurgistClient;
import net.goui.cosmicdungeon.playerclass.api.ClassData;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.client.settings.*;
import org.lwjgl.glfw.GLFW;

/** Native Controls owns persistence; existing class services own all action permissions and cooldowns. */
@EventBusSubscriber(modid = "cosmicdungeon", value = Dist.CLIENT)
public final class SkillKeybindClient {
    private static final EnumMap<Action, KeyMapping> KEYS = new EnumMap<>(Action.class);
    private static final Presses PRESSES = new Presses();
    private SkillKeybindClient() {}

    // MOD bus only. Lazily create native mappings here so model tests do not initialize a native window.
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        for (var action : Action.values()) {
            var mapping = new KeyMapping("key.cosmicdungeon." + action.id, new ClassContext(action.classId),
                    KeyModifier.ALT, InputConstants.Type.KEYSYM, action.defaultKey,
                    CosmicDungeonKeybindCategories.COSMIC_DUNGEON);
            KEYS.put(action, mapping);
            event.register(mapping);
        }
    }
    private record ClassContext(String classId) implements IKeyConflictContext {
        @Override public boolean isActive() {
            var mc = Minecraft.getInstance();
            return mc.screen == null && mc.player != null && classId.equals(ClassData.getClassId(mc.player));
        }
        @Override public boolean conflicts(IKeyConflictContext other) {
            return other == KeyConflictContext.UNIVERSAL || other == KeyConflictContext.IN_GAME
                    || other instanceof ClassContext c && classId.equals(c.classId);
        }
    }
    public static Component hint(String actionId) {
        var action = Action.forId(actionId);
        if (action == null) return Component.empty();
        var mapping = KEYS.get(action);
        var label = mapping == null || mapping.isUnbound() ? Component.translatable("key.cosmicdungeon.unbound")
                : mapping.getTranslatedKeyMessage();
        return Component.literal("<").append(label).append(">").withStyle(ChatFormatting.GRAY);
    }
    private static Modifier modifier(KeyModifier modifier) {
        return switch (modifier) {
            case SHIFT -> Modifier.SHIFT;
            case CONTROL -> Modifier.CONTROL;
            case ALT -> Modifier.ALT;
            default -> Modifier.NONE;
        };
    }
    private static Modifier modifier(int bits, Input input) {
        return SkillInputPolicy.modifier(bits, InputConstants.MOD_CONTROL, input);
    }
    private static Input input(InputConstants.Key key) {
        Device device = key.getType() == InputConstants.Type.MOUSE ? Device.MOUSE
                : key.getType() == InputConstants.Type.SCANCODE ? Device.SCANCODE : Device.KEY;
        return new Input(device, key.getValue());
    }
    private static Input input(KeyEvent key) { return input(InputConstants.getKey(key)); }
    private static Binding binding(Action action, KeyMapping key) {
        return new Binding(action, input(key.getKey()), modifier(key.getKeyModifier()), key.isUnbound());
    }
    private static boolean typing(GuiEventListener listener) {
        if (listener instanceof EditBox || listener instanceof MultiLineEditBox) return true;
        return listener instanceof ContainerEventHandler container && container.getFocused() != null
                && typing(container.getFocused());
    }
    private static boolean allowed(Minecraft mc) {
        Screen screen = mc.screen;
        boolean inventory = ClassInventoryPresentation.supports(screen);
        boolean player = mc.player != null;
        boolean menu = player && (screen == null ? mc.player.containerMenu == mc.player.inventoryMenu
                : inventory && mc.player.containerMenu == ((AbstractContainerScreen<?>) screen).getMenu());
        boolean busy = player && !mc.player.containerMenu.getCarried().isEmpty();
        if (inventory) busy |= screen.isDragging() || SkillsPanelClient.suppressesTooltips(screen)
                || PotionCraftClient.inputCaptured(screen);
        return SkillInputPolicy.allowed(new Context(player && mc.level != null && mc.getConnection() != null,
                player && mc.player.isAlive(), player && mc.player.isSpectator(), mc.isWindowActive(),
                mc.getOverlay() != null, screen == null || inventory, busy,
                screen != null && typing(screen), menu));
    }
    private static boolean conflict(Minecraft mc, Input input, Modifier modifier) {
        for (var mapping : mc.options.keyMappings) {
            if (KEYS.containsValue(mapping) || mapping.isUnbound()) continue;
            // Inventory also uses native in-game bindings (close inventory, swap hotbar, drop).
            if (!mapping.getKeyConflictContext().isActive()
                    && !(mc.screen != null && mapping.getKeyConflictContext() == KeyConflictContext.IN_GAME)) continue;
            if (SkillInputPolicy.matches(binding(Action.PATROL, mapping), input, modifier)) return true;
        }
        return false;
    }
    /** Returns consumed for an unambiguous skill even when the current capability declines it. */
    private static boolean activate(Input input, int bits, boolean execute) {
        var mc = Minecraft.getInstance();
        if (!allowed(mc)) return false;
        var modifier = modifier(bits, input);
        List<Binding> bindings = new ArrayList<>(KEYS.size());
        KEYS.forEach((action, mapping) -> bindings.add(binding(action, mapping)));
        String classId = ClassData.getClassId(mc.player);
        var action = SkillInputPolicy.resolve(classId, bindings, input, modifier, conflict(mc, input, modifier));
        if (action == null) return false;
        if (execute) SkillInputPolicy.dispatch(action, classId, BogatyrClient::activate, TheurgistClient::canOpenPicker,
                () -> mc.setScreen(new ResurrectPickerScreen(mc.screen instanceof AbstractContainerScreen<?> parent ? parent : null)));
        return true;
    }
    @SubscribeEvent public static void key(InputEvent.Key event) {
        var input = input(event.getKeyEvent());
        if (event.getAction() == InputConstants.RELEASE) { PRESSES.release(input); return; }
        // Screen handlers already observed GUI presses. No queued clicks are ever dispatched later.
        if (event.getAction() != InputConstants.PRESS || Minecraft.getInstance().screen != null) return;
        if (PRESSES.press(input)) activate(input, event.getModifiers(), true);
    }
    @SubscribeEvent public static void screenKey(ScreenEvent.KeyPressed.Pre event) {
        var input = input(event.getKeyEvent());
        boolean first = PRESSES.press(input);
        var screen = event.getScreen();
        // Preserve keyboard navigation/activation of focused native widgets.
        int code = event.getKeyCode();
        if (screen.getFocused() instanceof AbstractButton && (code == GLFW.GLFW_KEY_ENTER
                || code == GLFW.GLFW_KEY_KP_ENTER || code == GLFW.GLFW_KEY_SPACE)) return;
        if (activate(input, event.getModifiers(), first)) event.setCanceled(true);
    }
    @SubscribeEvent public static void screenRelease(ScreenEvent.KeyReleased.Pre event) {
        PRESSES.release(input(event.getKeyEvent()));
    }
    @SubscribeEvent public static void mouse(InputEvent.MouseButton.Pre event) {
        var input = new Input(Device.MOUSE, event.getButton());
        if (event.getAction() == InputConstants.RELEASE) { PRESSES.release(input); return; }
        if (event.getAction() != InputConstants.PRESS || !PRESSES.press(input)) return;
        // Primary inventory clicks remain slot/widget interactions even if rebound in Controls.
        if (Minecraft.getInstance().screen != null && event.getButton() < 3) return;
        if (activate(input, event.getModifiers(), true)) event.setCanceled(true);
    }
    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        // Native clicks only maintain native Controls state; raw presses above are the sole dispatcher.
        KEYS.values().forEach(key -> { while (key.consumeClick()) {} });
        var mc = Minecraft.getInstance();
        if (mc.player == null || !mc.isWindowActive()) PRESSES.clear();
    }
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event) { PRESSES.clear(); }
}
