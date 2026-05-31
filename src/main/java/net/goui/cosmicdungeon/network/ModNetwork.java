// file: src/main/java/net/goui/cosmicdungeon/network/ModNetwork.java
package net.goui.cosmicdungeon.network;

import net.goui.cosmicdungeon.auth.AccessPolicy;
import net.goui.cosmicdungeon.command.SpawnerCommand;
import net.goui.cosmicdungeon.client.SpawnerLabelState;
import net.goui.cosmicdungeon.network.handler.RegionLookAllClientPayloadHandler;
import net.goui.cosmicdungeon.network.handler.RegionLookAllServerPayloadHandler;
import net.goui.cosmicdungeon.network.handler.RegionLookClientPayloadHandler;
import net.goui.cosmicdungeon.network.payload.RegionLookAllPayload;
import net.goui.cosmicdungeon.network.payload.RegionLookAllRequestPayload;
import net.goui.cosmicdungeon.network.payload.RegionLookPayload;
import net.goui.cosmicdungeon.network.payload.SpawnerLabelPayload;
import net.goui.cosmicdungeon.network.payload.SpawnerPresetKeybindPayload;
import net.goui.cosmicdungeon.playerclass.api.ClassData;
import net.goui.cosmicdungeon.playerclass.api.ClassKeys;
import net.goui.cosmicdungeon.playerclass.api.ClassNet;
import net.goui.cosmicdungeon.playerclass.api.ClassNbtUtil;
import net.goui.cosmicdungeon.redstone.rf.RfNet;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.List;
import java.util.Objects;

public final class ModNetwork {
    private ModNetwork() {}

    public static void registerPayloadHandlers(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar("1");

        /* =====================================================================================
         * RF (central registration; feature-local packet definitions)
         * ===================================================================================== */
        RfNet.register(registrar);

        /* ===================== SHAKE ===================== */

        registrar.playToClient(
                ShakeScreenPayload.TYPE,
                ShakeScreenPayload.STREAM_CODEC,
                (payload, ctx) -> ctx.enqueueWork(() ->
                        ClientNetworkDispatch.dispatch("onShakeScreen", payload)
                )
        );

        /* ===================== SPAWNER LABEL (CLIENT TOGGLE; SERVER-AUTHORITATIVE) ===================== */

        registrar.playToClient(
                SpawnerLabelPayload.TYPE,
                SpawnerLabelPayload.STREAM_CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> {
                    // Hard-set state directly (no reflection needed).
                    SpawnerLabelState.setEnabled(payload.enabled());

                    // Keep your reflective path too (harmless, and helps if you want extra behavior there later).
                    ClientNetworkDispatch.dispatch("onSpawnerLabel", payload);
                })
        );

        
        registrar.playToServer(
                SpawnerPresetKeybindPayload.TYPE,
                SpawnerPresetKeybindPayload.STREAM_CODEC,
                (payload, ctx) -> {
                    if (!(ctx.player() instanceof ServerPlayer sp)) return;
                    int slot = payload.slot();
                    if (slot < 1 || slot > 5) return;
                    SpawnerCommand.loadPresetFromKeybind(sp, slot);
                }
        );

/* ===================== RIFT (SERVER-AUTHORITATIVE) ===================== */

        registrar.playToServer(
                RiftPayloads.C2S_RequestRiftConfig.TYPE,
                RiftPayloads.C2S_RequestRiftConfig.STREAM_CODEC,
                (payload, ctx) -> {
                    if (!(ctx.player() instanceof ServerPlayer sp)) return;
                    if (!(sp.level() instanceof ServerLevel level)) return;

                    var data = net.goui.cosmicdungeon.rift.RiftRegistryData.get(level);
                    var clicked = payload.clickedTilePos();

                    List<String> destinations = data.listDestinationNamesSorted();

                    if (!AccessPolicy.isDeveloper(sp)) {
                        ctx.reply(new RiftPayloads.S2C_RiftConfig(
                                clicked,
                                clicked,
                                "NO PERMISSION",
                                "",
                                false,
                                destinations
                        ));
                        return;
                    }

                    if (sp.blockPosition().distManhattan(clicked) > 16) {
                        ctx.reply(new RiftPayloads.S2C_RiftConfig(
                                clicked,
                                clicked,
                                "TOO FAR",
                                "",
                                false,
                                destinations
                        ));
                        return;
                    }

                    var anchorOpt = data.getAnchorForTile(level, clicked);

                    if (anchorOpt.isEmpty()) {
                        ctx.reply(new RiftPayloads.S2C_RiftConfig(
                                clicked,
                                clicked,
                                "",
                                "",
                                false,
                                destinations
                        ));
                        return;
                    }

                    var anchor = net.minecraft.core.BlockPos.of(anchorOpt.getAsLong());
                    var portal = data.getPortal(level, anchor).orElse(null);

                    String name = portal == null ? "" : portal.portalName();
                    String dest = portal == null ? "" : portal.destinationName();
                    boolean reset = portal != null && portal.resetTrigger();

                    ctx.reply(new RiftPayloads.S2C_RiftConfig(
                            clicked,
                            anchor,
                            name == null ? "" : name,
                            dest == null ? "" : dest,
                            reset,
                            destinations
                    ));
                }
        );

        registrar.playToServer(
                RiftPayloads.C2S_SaveRiftConfig.TYPE,
                RiftPayloads.C2S_SaveRiftConfig.STREAM_CODEC,
                (payload, ctx) -> {
                    if (!(ctx.player() instanceof ServerPlayer sp)) return;
                    if (!(sp.level() instanceof ServerLevel level)) return;

                    if (!AccessPolicy.isDeveloper(sp)) {
                        ctx.reply(new RiftPayloads.S2C_SaveResult(payload.anchorPos(), false, "No permission."));
                        return;
                    }

                    var data = net.goui.cosmicdungeon.rift.RiftRegistryData.get(level);
                    var anchor = payload.anchorPos();

                    if (sp.blockPosition().distManhattan(anchor) > 16) {
                        ctx.reply(new RiftPayloads.S2C_SaveResult(anchor, false, "Too far from rift to configure."));
                        return;
                    }

                    var result = data.setPortalConfig(
                            level,
                            anchor,
                            payload.riftName(),
                            payload.destinationName(),
                            payload.resetTrigger()
                    );

                    if (result instanceof net.goui.cosmicdungeon.rift.RiftRegistryData.SaveResult.Ok) {
                        ctx.reply(new RiftPayloads.S2C_SaveResult(anchor, true, "Rift saved."));
                    } else if (result instanceof net.goui.cosmicdungeon.rift.RiftRegistryData.SaveResult.BadDestination bd) {
                        ctx.reply(new RiftPayloads.S2C_SaveResult(anchor, false, "Unknown destination: " + bd.name()));
                    } else {
                        ctx.reply(new RiftPayloads.S2C_SaveResult(anchor, false, "Rift not found."));
                    }
                }
        );

        registrar.playToClient(
                RiftPayloads.S2C_RiftConfig.TYPE,
                RiftPayloads.S2C_RiftConfig.STREAM_CODEC,
                (payload, ctx) -> ctx.enqueueWork(() ->
                        ClientNetworkDispatch.dispatch("onRiftConfig", payload)
                )
        );

        registrar.playToClient(
                RiftPayloads.S2C_SaveResult.TYPE,
                RiftPayloads.S2C_SaveResult.STREAM_CODEC,
                (payload, ctx) -> ctx.enqueueWork(() ->
                        ClientNetworkDispatch.dispatch("onRiftSaveResult", payload)
                )
        );

        /* ===================== CLASS (SERVER-AUTHORITATIVE) ===================== */

        registrar.playToClient(
                ClassPayloads.S2C_ClassSync.TYPE,
                ClassPayloads.S2C_ClassSync.STREAM_CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> {
                    var player = ctx.player();
                    if (player == null) return;

                    String cls = Objects.requireNonNullElse(payload.classId(), ClassKeys.CLASS_ID_NONE);
                    ClassNbtUtil.setClassId(player, cls);

                    CompoundTag pd = player.getPersistentData();
                    CompoundTag root = pd.getCompoundOrEmpty(ClassData.ROOT_TAG).copy();

                    if (ClassKeys.CLASS_ID_METALMANCER.equals(cls)) {
                        root.put(ClassData.KEY_EXTRA, payload.extraNbt() == null ? new CompoundTag() : payload.extraNbt());
                    } else {
                        root.remove(ClassData.KEY_EXTRA);
                    }

                    pd.put(ClassData.ROOT_TAG, root);
                })
        );

        registrar.playToServer(
                ClassPayloads.C2S_RequestClass.TYPE,
                ClassPayloads.C2S_RequestClass.STREAM_CODEC,
                (payload, ctx) -> {
                    if (!(ctx.player() instanceof ServerPlayer sp)) return;
                    ClassNet.sendFullTo(sp);
                }
        );

        registrar.playToServer(
                ClassPayloads.C2S_Action.TYPE,
                ClassPayloads.C2S_Action.STREAM_CODEC,
                (payload, ctx) -> {
                    if (!(ctx.player() instanceof ServerPlayer sp)) return;
                    ClassNet.handleMetalmancerAction(sp, payload.actionId());
                }
        );

        registrar.playToServer(
                ClassPayloads.C2S_OpenMetalmancerInventory.TYPE,
                ClassPayloads.C2S_OpenMetalmancerInventory.STREAM_CODEC,
                (payload, ctx) -> {
                    if (!(ctx.player() instanceof ServerPlayer sp)) return;
                    ClassNet.openMetalmancerInventory(sp);
                }
        );

        registrar.playToServer(
                ClassPayloads.C2S_RequestSelectorData.TYPE,
                ClassPayloads.C2S_RequestSelectorData.STREAM_CODEC,
                (payload, ctx) -> {
                    if (!(ctx.player() instanceof ServerPlayer sp)) return;
                    ctx.reply(new ClassPayloads.S2C_SelectorData(
                            Objects.requireNonNullElse(ClassNbtUtil.getClassId(sp), ClassKeys.CLASS_ID_NONE),
                            ClassNet.getSelectableClasses(sp)
                    ));
                }
        );

        registrar.playToServer(
                ClassPayloads.C2S_SelectClass.TYPE,
                ClassPayloads.C2S_SelectClass.STREAM_CODEC,
                (payload, ctx) -> {
                    if (!(ctx.player() instanceof ServerPlayer sp)) return;

                    String requested = payload.classId();
                    String normalized = ClassNet.normalizeRequestedClass(sp, requested);

                    ClassNet.applySelectedClass(sp, normalized);

                    String now = Objects.requireNonNullElse(ClassNbtUtil.getClassId(sp), ClassKeys.CLASS_ID_NONE);
                    ctx.reply(new ClassPayloads.S2C_SelectResult(true, "Class selected: " + now, now));
                    ctx.reply(new ClassPayloads.S2C_SelectorData(now, ClassNet.getSelectableClasses(sp)));

                    net.goui.cosmicdungeon.block.custom.ClassSelectorTeleportUtil.onClassSelected(sp, now);
                }
        );

        registrar.playToClient(
                ClassPayloads.S2C_SelectorData.TYPE,
                ClassPayloads.S2C_SelectorData.STREAM_CODEC,
                (payload, ctx) -> ctx.enqueueWork(() ->
                        ClientNetworkDispatch.dispatch("onClassSelectorData", payload)
                )
        );

        registrar.playToClient(
                ClassPayloads.S2C_SelectResult.TYPE,
                ClassPayloads.S2C_SelectResult.STREAM_CODEC,
                (payload, ctx) -> ctx.enqueueWork(() ->
                        ClientNetworkDispatch.dispatch("onClassSelectorResult", payload)
                )
        );


        /* ===================== VENDOR ===================== */

        registrar.playToServer(
                VendorPayloads.C2S_RequestVendorPurchase.TYPE,
                VendorPayloads.C2S_RequestVendorPurchase.STREAM_CODEC,
                (payload, ctx) -> {
                    if (!(ctx.player() instanceof ServerPlayer sp)) return;
                    ctx.reply(net.goui.cosmicdungeon.vendor.VendorService.tryPurchase(sp, payload.vendorEntityId(), payload.offerId()));
                }
        );

        registrar.playToServer(
                VendorPayloads.C2S_RequestVendorSellSlot.TYPE,
                VendorPayloads.C2S_RequestVendorSellSlot.STREAM_CODEC,
                (payload, ctx) -> {
                    if (!(ctx.player() instanceof ServerPlayer sp)) return;
                    ctx.reply(net.goui.cosmicdungeon.vendor.VendorService.trySellSlot(sp, payload.vendorEntityId(), payload.slotIndex()));
                }
        );

        registrar.playToServer(
                VendorPayloads.C2S_RequestVendorSellDetectedSet.TYPE,
                VendorPayloads.C2S_RequestVendorSellDetectedSet.STREAM_CODEC,
                (payload, ctx) -> {
                    if (!(ctx.player() instanceof ServerPlayer sp)) return;
                    ctx.reply(net.goui.cosmicdungeon.vendor.VendorService.trySellDetectedSet(sp, payload.vendorEntityId(), payload.setId()));
                }
        );

        registrar.playToClient(
                VendorPayloads.S2C_OpenVendor.TYPE,
                VendorPayloads.S2C_OpenVendor.STREAM_CODEC,
                (payload, ctx) -> ctx.enqueueWork(() ->
                        ClientNetworkDispatch.dispatch("onOpenVendor", payload)
                )
        );

        registrar.playToClient(
                VendorPayloads.S2C_VendorPurchaseResult.TYPE,
                VendorPayloads.S2C_VendorPurchaseResult.STREAM_CODEC,
                (payload, ctx) -> ctx.enqueueWork(() ->
                        ClientNetworkDispatch.dispatch("onVendorPurchaseResult", payload)
                )
        );

        registrar.playToServer(TradePayloads.C2S_RequestTrade.TYPE, TradePayloads.C2S_RequestTrade.STREAM_CODEC, (payload, ctx) -> {
            if (!(ctx.player() instanceof ServerPlayer sp)) return;
            var server = sp.level().getServer();
            if (server == null) return;
            var target = server.getPlayerList().getPlayerByName(payload.targetName());
            if (target == null || target == sp) {
                sp.sendSystemMessage(net.minecraft.network.chat.Component.literal("Unable to trade with that player right now."));
                return;
            }
            net.goui.cosmicdungeon.trade.TradeSessionData.invite(sp, target);
        });
        registrar.playToServer(TradePayloads.C2S_RequestLookTrade.TYPE, TradePayloads.C2S_RequestLookTrade.STREAM_CODEC, (payload, ctx) -> {
            if (!(ctx.player() instanceof ServerPlayer sp)) return;
            handleLookTradeRequest(sp, payload.targetPlayerId());
        });
        registrar.playToServer(TradePayloads.C2S_AcceptTrade.TYPE, TradePayloads.C2S_AcceptTrade.STREAM_CODEC, (payload, ctx) -> {
            if (!(ctx.player() instanceof ServerPlayer sp)) return;
            var server = sp.level().getServer();
            if (server == null) return;
            var inviter = server.getPlayerList().getPlayerByName(payload.inviterName());
            if (inviter == null) {
                sp.sendSystemMessage(net.minecraft.network.chat.Component.literal("That trade inviter is no longer online."));
                return;
            }
            net.goui.cosmicdungeon.trade.TradeSessionData.acceptInvite(sp, inviter);
        });
        registrar.playToServer(TradePayloads.C2S_UpdateCurrencyOffer.TYPE, TradePayloads.C2S_UpdateCurrencyOffer.STREAM_CODEC, (payload, ctx) -> { if (ctx.player() instanceof ServerPlayer sp) { var s0=net.goui.cosmicdungeon.trade.TradeSessionData.get(sp); if (s0!=null) s0.setCurrency(sp, payload.traceAmount()); } });
        registrar.playToServer(TradePayloads.C2S_Ready.TYPE, TradePayloads.C2S_Ready.STREAM_CODEC, (payload, ctx) -> { if (ctx.player() instanceof ServerPlayer sp) { var s0=net.goui.cosmicdungeon.trade.TradeSessionData.get(sp); if (s0!=null) s0.setReady(sp, payload.ready()); } });
        registrar.playToServer(TradePayloads.C2S_Confirm.TYPE, TradePayloads.C2S_Confirm.STREAM_CODEC, (payload, ctx) -> { if (ctx.player() instanceof ServerPlayer sp) { var s0=net.goui.cosmicdungeon.trade.TradeSessionData.get(sp); if (s0!=null) s0.setConfirm(sp, payload.confirm()); } });
        registrar.playToServer(TradePayloads.C2S_Cancel.TYPE, TradePayloads.C2S_Cancel.STREAM_CODEC, (payload, ctx) -> { if (ctx.player() instanceof ServerPlayer sp) net.goui.cosmicdungeon.trade.TradeSessionData.cancel(sp, "Cancelled"); });
        registrar.playToClient(
                TradePayloads.S2C_TradeState.TYPE,
                TradePayloads.S2C_TradeState.STREAM_CODEC,
                (payload, ctx) -> ctx.enqueueWork(() ->
                        ClientNetworkDispatch.dispatch("onTradeState", payload)
                )
        );

        /* ===================== REGION LOOK ===================== */

        registrar.playToClient(
                RegionLookPayload.TYPE,
                RegionLookPayload.STREAM_CODEC,
                RegionLookClientPayloadHandler::handle
        );

        registrar.playToServer(
                RegionLookAllRequestPayload.TYPE,
                RegionLookAllRequestPayload.STREAM_CODEC,
                RegionLookAllServerPayloadHandler::handle
        );

        registrar.playToClient(
                RegionLookAllPayload.TYPE,
                RegionLookAllPayload.STREAM_CODEC,
                RegionLookAllClientPayloadHandler::handle
        );
    }

    private static void handleLookTradeRequest(ServerPlayer sender, java.util.UUID targetPlayerId) {
        var server = sender.level().getServer();
        if (server == null) return;

        ServerPlayer target = server.getPlayerList().getPlayer(targetPlayerId);
        if (target == null || target == sender) {
            sender.sendSystemMessage(net.minecraft.network.chat.Component.literal("Unable to trade with that player right now."));
            return;
        }
        if (!sender.level().dimension().equals(target.level().dimension())) {
            sender.sendSystemMessage(net.minecraft.network.chat.Component.literal("Unable to trade with that player right now."));
            return;
        }
        if (sender.distanceToSqr(target) > 3.25D * 3.25D) {
            sender.sendSystemMessage(net.minecraft.network.chat.Component.literal("Look at a player within 3 blocks to request a trade."));
            return;
        }
        if (!isApproximatelyLookingAt(sender, target)) {
            sender.sendSystemMessage(net.minecraft.network.chat.Component.literal("Look at a player within 3 blocks to request a trade."));
            return;
        }

        net.goui.cosmicdungeon.trade.TradeSessionData.invite(sender, target);
    }

    private static boolean isApproximatelyLookingAt(ServerPlayer sender, ServerPlayer target) {
        Vec3 eye = sender.getEyePosition();
        Vec3 end = eye.add(sender.getLookAngle().scale(3.25D));
        return target.getBoundingBox().inflate(0.25D).clip(eye, end).isPresent();
    }

    public static void sendToServer(CustomPacketPayload payload) {
        ClientNetworkDispatch.sendToServer(payload);
    }

    public static void sendTo(ServerPlayer player, CustomPacketPayload payload) {
        PacketDistributor.sendToPlayer(player, payload);
    }
}