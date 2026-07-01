// file: src/main/java/net/goui/cosmicdungeon/client/ModNetworkClient.java
package net.goui.cosmicdungeon.client;

import net.goui.cosmicdungeon.client.rift.RiftConfigScreen;
import net.goui.cosmicdungeon.client.screen.ClassSelectorScreen;
import net.goui.cosmicdungeon.client.screen.CompanionshipTeleportScreen;
import net.goui.cosmicdungeon.network.ClassPayloads;
import net.goui.cosmicdungeon.network.RiftPayloads;
import net.goui.cosmicdungeon.network.ShakeScreenPayload;
import net.goui.cosmicdungeon.network.TradePayloads;
import net.goui.cosmicdungeon.network.VendorPayloads;
import net.goui.cosmicdungeon.network.CompanionshipTeleportPayloads;
import net.goui.cosmicdungeon.client.screen.VendorScreen;
import net.goui.cosmicdungeon.client.screen.VendorScreen.VendorClientState;
import net.goui.cosmicdungeon.client.screen.TradeScreen.TradeClientState;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.goui.cosmicdungeon.network.payload.RegionLookAllPayload;
import net.goui.cosmicdungeon.network.payload.RegionLookPayload;
import net.goui.cosmicdungeon.network.payload.SpawnerLabelPayload;
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

    public static void onSpawnerLabel(SpawnerLabelPayload payload) {
        CosmicSpawnerHoverOverlay.setEnabled(payload.enabled());
    }

    public static void onOpenCompanionshipTeleport(CompanionshipTeleportPayloads.S2C_Open payload) {
        Minecraft.getInstance().setScreen(new CompanionshipTeleportScreen(payload.players()));
    }

    public static void onOpenVendor(VendorPayloads.S2C_OpenVendor payload) {
        var profileId = ResourceLocation.tryParse(payload.profileId());
        VendorClientState.set(new VendorClientState.VendorView(
                payload.vendorEntityId(),
                profileId,
                payload.vendorDisplayName(),
                payload.storeDisplayName(),
                payload.balanceTrace(),
                payload.pricingGroup(),
                java.util.List.copyOf(payload.offers()),
                new java.util.HashSet<>(payload.unlockedOffers())
        ));
    }

    public static void onVendorPurchaseResult(VendorPayloads.S2C_VendorPurchaseResult payload) {
        var current = VendorClientState.current();
        if (current != null) {
            VendorClientState.set(new VendorClientState.VendorView(current.vendorEntityId(), current.profileId(), current.title(), current.storeDisplayName(), payload.newBalanceTrace(), current.pricingGroup(), current.offers(), current.unlockedOffers()));
        }
        if (payload.ok()) VendorScreen.clearSelectionsIfOpen();
    }

    public static void onTradePromptState(TradePayloads.S2C_TradePromptState payload) {
        TradePromptClientState.setHidePrompt(payload.hidePrompt());
    }

    public static void onTradeState(TradePayloads.S2C_TradeState payload) {
        TradeClientState.set(new TradeClientState.TradeView(
                payload.containerId(),
                payload.sessionId(),
                payload.selfName(),
                payload.otherName(),
                payload.selfBalanceTrace(),
                payload.otherBalanceTrace(),
                payload.selfOfferedTrace(),
                payload.otherOfferedTrace(),
                payload.selfReady(),
                payload.otherReady(),
                payload.selfConfirmed(),
                payload.otherConfirmed(),
                payload.statusMessage()
        ));
    }

    public static void sendToServer(CustomPacketPayload payload) {
        ClientPacketDistributor.sendToServer(payload);
    }
}