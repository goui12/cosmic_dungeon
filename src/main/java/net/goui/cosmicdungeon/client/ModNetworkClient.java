package net.goui.cosmicdungeon.client;

import net.goui.cosmicdungeon.client.rift.RiftConfigScreen;
import net.goui.cosmicdungeon.client.screen.ClassSelectorScreen;
import net.goui.cosmicdungeon.network.ClassPayloads;
import net.goui.cosmicdungeon.network.RiftPayloads;
import net.goui.cosmicdungeon.network.ShakeScreenPayload;
import net.goui.cosmicdungeon.network.payload.RegionLookAllPayload;
import net.goui.cosmicdungeon.network.payload.RegionLookPayload;
import net.goui.cosmicdungeon.region.client.RegionLookClient;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/**
 * Client-only implementations called reflectively from common code.
 * Keep ALL client-only imports here.
 *
 * IMPORTANT:
 * - ModNetwork already calls ctx.enqueueWork (main thread).
 * - So these methods should run immediately (no extra Minecraft.execute nesting).
 */
public final class ModNetworkClient {
    private ModNetworkClient() {}

    public static void onShakeScreen(ShakeScreenPayload payload) {
        ClientShakeHandler.startShake(2.0F, 1.0F);
    }

    public static void onClassSelectorData(ClassPayloads.S2C_SelectorData payload) {
        ClassSelectorScreen.onSelectorData(payload);
    }

    public static void onClassSelectorResult(ClassPayloads.S2C_SelectResult payload) {
        ClassSelectorScreen.onSelectResult(payload);
    }

    public static void onRegionLook(RegionLookPayload payload) {
        RegionLookClient.handle(payload);
    }

    public static void onRegionLookAll(RegionLookAllPayload payload) {
        RegionLookClient.handleAll(payload);
    }

    public static void onRiftConfig(RiftPayloads.S2C_RiftConfig payload) {
        RiftConfigScreen.onServerConfig(payload);
    }

    public static void onRiftSaveResult(RiftPayloads.S2C_SaveResult payload) {
        RiftConfigScreen.onServerSaveResult(payload);
    }

    public static void sendToServer(CustomPacketPayload payload) {
        ClientPacketDistributor.sendToServer(payload);
    }
}
