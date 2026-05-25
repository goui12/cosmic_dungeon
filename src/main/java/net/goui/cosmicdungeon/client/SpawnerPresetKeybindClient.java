package net.goui.cosmicdungeon.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.goui.cosmicdungeon.network.payload.SpawnerPresetKeybindPayload;
import net.minecraft.client.KeyMapping;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

public final class SpawnerPresetKeybindClient {
    private SpawnerPresetKeybindClient() {}

    private static final String CATEGORY = "key.categories.cosmicdungeon";
    private static final KeyMapping[] KEYBINDS = new KeyMapping[]{
            new KeyMapping("key.cosmicdungeon.spawner_preset_1", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_KP_1, CATEGORY),
            new KeyMapping("key.cosmicdungeon.spawner_preset_2", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_KP_2, CATEGORY),
            new KeyMapping("key.cosmicdungeon.spawner_preset_3", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_KP_3, CATEGORY),
            new KeyMapping("key.cosmicdungeon.spawner_preset_4", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_KP_4, CATEGORY),
            new KeyMapping("key.cosmicdungeon.spawner_preset_5", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_KP_5, CATEGORY)
    };

    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        for (KeyMapping km : KEYBINDS) event.register(km);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        for (int i = 0; i < KEYBINDS.length; i++) {
            while (KEYBINDS[i].consumeClick()) {
                PacketDistributor.sendToServer(new SpawnerPresetKeybindPayload(i + 1));
            }
        }
    }
}
