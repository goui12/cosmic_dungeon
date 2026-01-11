package net.goui.cosmicdungeon.client.network;

import net.goui.cosmicdungeon.client.ClientShakeHandler;
import net.goui.cosmicdungeon.client.rift.RiftConfigScreen;
import net.goui.cosmicdungeon.client.screen.ClassSelectorScreen;
import net.goui.cosmicdungeon.network.ClassPayloads;
import net.goui.cosmicdungeon.network.RiftPayloads;
import net.goui.cosmicdungeon.network.ShakeScreenPayload;
import net.goui.cosmicdungeon.network.payload.RegionLookAllPayload;
import net.goui.cosmicdungeon.network.payload.RegionLookPayload;
import net.goui.cosmicdungeon.region.client.RegionLookClient;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/**
 * Client-only implementations called reflectively from common code.
 * Keep ALL client-only imports here.
 */
public final class ModNetworkClient {
    private ModNetworkClient() {}

    /* -------------------- common -> client S2C handlers -------------------- */

    public static void onShakeScreen(ShakeScreenPayload payload) {
        Minecraft.getInstance().execute(() -> ClientShakeHandler.startShake(2.0F, 1.0F));
    }

    public static void onClassSelectorData(ClassPayloads.S2C_SelectorData payload) {
        Minecraft.getInstance().execute(() -> ClassSelectorScreen.onSelectorData(payload));
    }

    public static void onClassSelectorResult(ClassPayloads.S2C_SelectResult payload) {
        Minecraft.getInstance().execute(() -> ClassSelectorScreen.onSelectResult(payload));
    }

    public static void onRegionLook(RegionLookPayload payload) {
        Minecraft.getInstance().execute(() -> RegionLookClient.handle(payload));
    }

    public static void onRegionLookAll(RegionLookAllPayload payload) {
        Minecraft.getInstance().execute(() -> RegionLookClient.handleAll(payload));
    }

    public static void onRiftConfig(RiftPayloads.S2C_RiftConfig payload) {
        Minecraft.getInstance().execute(() -> RiftConfigScreen.onServerConfig(payload));
    }

    public static void onOpenRiftConfig(RiftPayloads.S2C_OpenRiftConfig payload) {
        Minecraft.getInstance().execute(() -> RiftConfigScreen.openForClickedTile(payload.clickedTilePos()));
    }

    public static void onRiftSaveResult(RiftPayloads.S2C_SaveResult payload) {
        Minecraft.getInstance().execute(() -> RiftConfigScreen.onServerSaveResult(payload));
    }

    /* -------------------- client -> server sender -------------------- */

    public static void sendToServer(CustomPacketPayload payload) {
        ClientPacketDistributor.sendToServer(payload);
    }
}
