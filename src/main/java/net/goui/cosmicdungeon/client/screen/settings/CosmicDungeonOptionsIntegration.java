package net.goui.cosmicdungeon.client.screen.settings;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

import java.util.function.Supplier;

public final class CosmicDungeonOptionsIntegration {
    private static final Component BUTTON_LABEL = Component.literal("Cosmic Dungeon");

    private CosmicDungeonOptionsIntegration() {}

    public static void registerConfigScreen(ModContainer container) {
        Supplier<IConfigScreenFactory> factory = () -> (modContainer, parent) -> new CosmicDungeonOptionsScreen(parent);
        container.registerExtensionPoint(IConfigScreenFactory.class, factory);
    }

    public static void onScreenInit(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof OptionsScreen optionsScreen)) return;
        if (hasButton(event)) return;

        int buttonWidth = 150;
        int buttonHeight = 20;
        int x = Math.max(4, optionsScreen.width / 2 - buttonWidth / 2);
        int y = Math.max(4, optionsScreen.height - 52);

        event.addListener(Button.builder(BUTTON_LABEL, b -> Minecraft.getInstance().setScreen(new CosmicDungeonOptionsScreen(optionsScreen)))
                .bounds(x, y, buttonWidth, buttonHeight)
                .build());
    }

    private static boolean hasButton(ScreenEvent.Init.Post event) {
        for (GuiEventListener listener : event.getListenersList()) {
            if (listener instanceof Button button && BUTTON_LABEL.equals(button.getMessage())) return true;
        }
        return false;
    }
}
