package net.goui.cosmicdungeon.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.goui.cosmicdungeon.client.screen.HelpMenuScreen;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

public final class HelpMenuKeybindClient {
    private static final KeyMapping HELP_MENU_KEY = new KeyMapping(
            "key.cosmicdungeon.help_menu",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_H,
            CosmicDungeonKeybindCategories.COSMIC_DUNGEON
    );

    private HelpMenuKeybindClient() {}

    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(HELP_MENU_KEY);
    }

    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        while (HELP_MENU_KEY.consumeClick()) {
            if (minecraft.screen instanceof HelpMenuScreen) {
                minecraft.setScreen(null);
            } else if (minecraft.screen == null) {
                minecraft.setScreen(new HelpMenuScreen());
            }
        }
    }

    public static boolean matchesHelpMenuKey(int keyCode, int scanCode) {
        return HELP_MENU_KEY.matches(keyCode, scanCode);
    }
}
