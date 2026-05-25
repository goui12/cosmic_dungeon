// file: src/main/java/net/goui/cosmicdungeon/client/SpawnerPresetKeybindClient.java
package net.goui.cosmicdungeon.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.goui.cosmicdungeon.CosmicDungeonMod;
import net.goui.cosmicdungeon.network.payload.SpawnerPresetKeybindPayload;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.lwjgl.glfw.GLFW;

public final class SpawnerPresetKeybindClient {
    private SpawnerPresetKeybindClient() {}

    private static final KeyMapping.Category CATEGORY =
            new KeyMapping.Category(ResourceLocation.fromNamespaceAndPath(CosmicDungeonMod.MOD_ID, "cosmicdungeon"));

    private static final KeyMapping[] KEYBINDS = new KeyMapping[]{
            new KeyMapping("key.cosmicdungeon.spawner_preset_1", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_KP_1, CATEGORY),
            new KeyMapping("key.cosmicdungeon.spawner_preset_2", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_KP_2, CATEGORY),
            new KeyMapping("key.cosmicdungeon.spawner_preset_3", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_KP_3, CATEGORY),
            new KeyMapping("key.cosmicdungeon.spawner_preset_4", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_KP_4, CATEGORY),
            new KeyMapping("key.cosmicdungeon.spawner_preset_5", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_KP_5, CATEGORY)
    };

    /**
     * MOD BUS event.
     *
     * Registered from CosmicDungeonClient.init(modEventBus) using:
     * modEventBus.addListener(SpawnerPresetKeybindClient::registerKeyMappings);
     */
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.registerCategory(CATEGORY);

        for (KeyMapping keybind : KEYBINDS) {
            event.register(keybind);
        }
    }

    /**
     * NeoForge/common client tick event.
     *
     * Registered from CosmicDungeonClient.init(modEventBus) using:
     * NeoForge.EVENT_BUS.addListener(SpawnerPresetKeybindClient::onClientTick);
     */
    public static void onClientTick(ClientTickEvent.Post event) {
        for (int i = 0; i < KEYBINDS.length; i++) {
            while (KEYBINDS[i].consumeClick()) {
                ClientPacketDistributor.sendToServer(new SpawnerPresetKeybindPayload(i + 1));
            }
        }
    }
}