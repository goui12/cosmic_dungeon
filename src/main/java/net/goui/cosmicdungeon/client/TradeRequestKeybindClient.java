package net.goui.cosmicdungeon.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.goui.cosmicdungeon.CosmicDungeonMod;
import net.goui.cosmicdungeon.network.TradePayloads;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.lwjgl.glfw.GLFW;

public final class TradeRequestKeybindClient {
    private static final double MAX_TRADE_DISTANCE_SQR = 3.0D * 3.0D;

    private static final KeyMapping.Category CATEGORY =
            new KeyMapping.Category(ResourceLocation.fromNamespaceAndPath(CosmicDungeonMod.MOD_ID, "cosmicdungeon"));

    private static final KeyMapping KEY = new KeyMapping(
            "key.cosmicdungeon.trade_request",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_CAPS_LOCK,
            CATEGORY
    );

    private TradeRequestKeybindClient() {}

    /**
     * MOD BUS event.
     *
     * Registered from CosmicDungeonClient.init(modEventBus) using:
     * modEventBus.addListener(TradeRequestKeybindClient::registerKeyMappings);
     */
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.registerCategory(CATEGORY);
        event.register(KEY);
    }

    /**
     * NeoForge/common client tick event.
     *
     * Registered from CosmicDungeonClient.init(modEventBus) using:
     * NeoForge.EVENT_BUS.addListener(TradeRequestKeybindClient::onClientTick);
     */
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.player == null || minecraft.level == null) {
            return;
        }
        if (minecraft.screen != null) {
            return;
        }

        while (KEY.consumeClick()) {
            if (minecraft.hitResult instanceof EntityHitResult entityHitResult
                    && entityHitResult.getEntity() instanceof Player target
                    && target != minecraft.player
                    && minecraft.player.distanceToSqr(target) <= MAX_TRADE_DISTANCE_SQR) {
                ClientPacketDistributor.sendToServer(new TradePayloads.C2S_RequestLookTrade(target.getUUID()));
            } else {
                minecraft.player.displayClientMessage(
                        Component.translatable("message.cosmicdungeon.trade.look_at_player"),
                        true
                );
            }
        }
    }
}
