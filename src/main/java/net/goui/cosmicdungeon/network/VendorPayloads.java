package net.goui.cosmicdungeon.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public final class VendorPayloads {
    private VendorPayloads() {}

    public record C2S_RequestVendorPurchase(int vendorEntityId, String offerId) implements CustomPacketPayload {
        public static final Type<C2S_RequestVendorPurchase> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("cosmicdungeon", "vendor_purchase_request"));
        public static final StreamCodec<ByteBuf, C2S_RequestVendorPurchase> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.INT, C2S_RequestVendorPurchase::vendorEntityId,
                ByteBufCodecs.STRING_UTF8, C2S_RequestVendorPurchase::offerId,
                C2S_RequestVendorPurchase::new
        );
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record C2S_RequestVendorSellSlot(int vendorEntityId, int slotIndex) implements CustomPacketPayload {
        public static final Type<C2S_RequestVendorSellSlot> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("cosmicdungeon", "vendor_sell_slot_request"));
        public static final StreamCodec<ByteBuf, C2S_RequestVendorSellSlot> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.INT, C2S_RequestVendorSellSlot::vendorEntityId,
                ByteBufCodecs.INT, C2S_RequestVendorSellSlot::slotIndex,
                C2S_RequestVendorSellSlot::new
        );

        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record C2S_RequestVendorSellDetectedSet(int vendorEntityId, String setId) implements CustomPacketPayload {
        public static final Type<C2S_RequestVendorSellDetectedSet> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("cosmicdungeon", "vendor_sell_set_request"));
        public static final StreamCodec<ByteBuf, C2S_RequestVendorSellDetectedSet> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.INT, C2S_RequestVendorSellDetectedSet::vendorEntityId,
                ByteBufCodecs.STRING_UTF8, C2S_RequestVendorSellDetectedSet::setId,
                C2S_RequestVendorSellDetectedSet::new
        );

        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record S2C_OpenVendor(int vendorEntityId, String profileId, String displayName, long balanceTrace, String pricingGroup, List<OfferView> offers, List<String> unlockedOffers) implements CustomPacketPayload {
        public record OfferView(String offerId, ItemStack stack, String itemDisplayName, int count, long costAmount, String costDenomination) {}
        public static final StreamCodec<RegistryFriendlyByteBuf, OfferView> OFFER_VIEW_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, OfferView::offerId,
                ItemStack.STREAM_CODEC, OfferView::stack,
                ByteBufCodecs.STRING_UTF8, OfferView::itemDisplayName,
                ByteBufCodecs.INT, OfferView::count,
                ByteBufCodecs.VAR_LONG, OfferView::costAmount,
                ByteBufCodecs.STRING_UTF8, OfferView::costDenomination,
                OfferView::new
        );
        public static final Type<S2C_OpenVendor> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("cosmicdungeon", "vendor_open"));
        public static final StreamCodec<RegistryFriendlyByteBuf, S2C_OpenVendor> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.INT, S2C_OpenVendor::vendorEntityId,
                ByteBufCodecs.STRING_UTF8, S2C_OpenVendor::profileId,
                ByteBufCodecs.STRING_UTF8, S2C_OpenVendor::displayName,
                ByteBufCodecs.VAR_LONG, S2C_OpenVendor::balanceTrace,
                ByteBufCodecs.STRING_UTF8, S2C_OpenVendor::pricingGroup,
                OFFER_VIEW_CODEC.apply(ByteBufCodecs.list()), S2C_OpenVendor::offers,
                ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), S2C_OpenVendor::unlockedOffers,
                S2C_OpenVendor::new
        );
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record S2C_VendorPurchaseResult(boolean ok, String message, long newBalanceTrace) implements CustomPacketPayload {
        public static final Type<S2C_VendorPurchaseResult> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("cosmicdungeon", "vendor_purchase_result"));
        public static final StreamCodec<ByteBuf, S2C_VendorPurchaseResult> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.BOOL, S2C_VendorPurchaseResult::ok,
                ByteBufCodecs.STRING_UTF8, S2C_VendorPurchaseResult::message,
                ByteBufCodecs.VAR_LONG, S2C_VendorPurchaseResult::newBalanceTrace,
                S2C_VendorPurchaseResult::new
        );
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }
}
