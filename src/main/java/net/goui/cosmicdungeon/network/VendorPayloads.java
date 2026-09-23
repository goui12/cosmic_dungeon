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
    private static final StreamCodec<ByteBuf, java.util.UUID> UUID_CODEC = StreamCodec.of(
            (buf, id) -> { buf.writeLong(id.getMostSignificantBits()); buf.writeLong(id.getLeastSignificantBits()); },
            buf -> new java.util.UUID(buf.readLong(), buf.readLong()));

    public record C2S_RequestVendorPurchase(int vendorEntityId, String offerId) implements CustomPacketPayload {
        public static final Type<C2S_RequestVendorPurchase> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("cosmicdungeon", "vendor_purchase_request"));
        public static final StreamCodec<ByteBuf, C2S_RequestVendorPurchase> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.INT, C2S_RequestVendorPurchase::vendorEntityId,
                ByteBufCodecs.STRING_UTF8, C2S_RequestVendorPurchase::offerId,
                C2S_RequestVendorPurchase::new
        );
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record C2S_RequestVendorSellSelected(int vendorEntityId, List<Integer> slotIndexes) implements CustomPacketPayload {
        public static final Type<C2S_RequestVendorSellSelected> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("cosmicdungeon", "vendor_sell_selected_request"));
        public static final StreamCodec<ByteBuf, C2S_RequestVendorSellSelected> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.INT, C2S_RequestVendorSellSelected::vendorEntityId,
                ByteBufCodecs.INT.apply(ByteBufCodecs.list(41)), C2S_RequestVendorSellSelected::slotIndexes,
                C2S_RequestVendorSellSelected::new
        );

        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record C2S_RequestVendorSellAll(int vendorEntityId) implements CustomPacketPayload {
        public static final Type<C2S_RequestVendorSellAll> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("cosmicdungeon", "vendor_sell_all_request"));
        public static final StreamCodec<ByteBuf, C2S_RequestVendorSellAll> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.INT, C2S_RequestVendorSellAll::vendorEntityId,
                C2S_RequestVendorSellAll::new
        );

        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record S2C_OpenVendor(int containerId, java.util.UUID sessionId, int vendorEntityId, String profileId, String vendorDisplayName, String storeDisplayName, long balanceTrace, String pricingGroup, List<OfferView> offers, List<String> unlockedOffers) implements CustomPacketPayload {
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
        public static final StreamCodec<RegistryFriendlyByteBuf, S2C_OpenVendor> STREAM_CODEC = StreamCodec.of((buf, p) -> {
            ByteBufCodecs.INT.encode(buf, p.containerId); UUID_CODEC.encode(buf, p.sessionId);
            ByteBufCodecs.INT.encode(buf, p.vendorEntityId);
            ByteBufCodecs.STRING_UTF8.encode(buf, p.profileId);
            ByteBufCodecs.STRING_UTF8.encode(buf, p.vendorDisplayName);
            ByteBufCodecs.STRING_UTF8.encode(buf, p.storeDisplayName);
            ByteBufCodecs.VAR_LONG.encode(buf, p.balanceTrace);
            ByteBufCodecs.STRING_UTF8.encode(buf, p.pricingGroup);
            OFFER_VIEW_CODEC.apply(ByteBufCodecs.list()).encode(buf, p.offers);
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()).encode(buf, p.unlockedOffers);
        }, buf -> new S2C_OpenVendor(ByteBufCodecs.INT.decode(buf), UUID_CODEC.decode(buf),
                ByteBufCodecs.INT.decode(buf), ByteBufCodecs.STRING_UTF8.decode(buf),
                ByteBufCodecs.STRING_UTF8.decode(buf), ByteBufCodecs.STRING_UTF8.decode(buf),
                ByteBufCodecs.VAR_LONG.decode(buf), ByteBufCodecs.STRING_UTF8.decode(buf),
                OFFER_VIEW_CODEC.apply(ByteBufCodecs.list()).decode(buf),
                ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()).decode(buf)));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record S2C_VendorPurchaseResult(int containerId, java.util.UUID sessionId, boolean ok, String message, long newBalanceTrace) implements CustomPacketPayload {
        public static final Type<S2C_VendorPurchaseResult> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("cosmicdungeon", "vendor_purchase_result"));
        public static final StreamCodec<ByteBuf, S2C_VendorPurchaseResult> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.INT, S2C_VendorPurchaseResult::containerId,
                UUID_CODEC, S2C_VendorPurchaseResult::sessionId,
                ByteBufCodecs.BOOL, S2C_VendorPurchaseResult::ok,
                ByteBufCodecs.STRING_UTF8, S2C_VendorPurchaseResult::message,
                ByteBufCodecs.VAR_LONG, S2C_VendorPurchaseResult::newBalanceTrace,
                S2C_VendorPurchaseResult::new
        );
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record S2C_VendorBalance(int containerId, java.util.UUID sessionId, long balanceTrace, long capacityTrace) implements CustomPacketPayload {
        public static final Type<S2C_VendorBalance> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("cosmicdungeon", "vendor_balance"));
        public static final StreamCodec<ByteBuf, S2C_VendorBalance> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.INT, S2C_VendorBalance::containerId, UUID_CODEC, S2C_VendorBalance::sessionId,
                ByteBufCodecs.VAR_LONG, S2C_VendorBalance::balanceTrace,
                ByteBufCodecs.VAR_LONG, S2C_VendorBalance::capacityTrace, S2C_VendorBalance::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record C2S_VendorSaleDecision(int containerId, String token, boolean confirm) implements CustomPacketPayload {
        public static final Type<C2S_VendorSaleDecision> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("cosmicdungeon", "vendor_sale_decision"));
        public static final StreamCodec<ByteBuf, C2S_VendorSaleDecision> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.INT, C2S_VendorSaleDecision::containerId,
                ByteBufCodecs.stringUtf8(36), C2S_VendorSaleDecision::token,
                ByteBufCodecs.BOOL, C2S_VendorSaleDecision::confirm,
                C2S_VendorSaleDecision::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record S2C_VendorSaleQuote(int containerId, java.util.UUID sessionId, String token, long totalTrace, List<Line> lines) implements CustomPacketPayload {
        public record Line(ItemStack stack, net.goui.cosmicdungeon.economy.pricing.VendorPriceBreakdown breakdown) {
            public long trace() { return breakdown.total(); }
        }
        private static final StreamCodec<io.netty.buffer.ByteBuf, net.goui.cosmicdungeon.economy.pricing.VendorPriceBreakdown>
                BREAKDOWN_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_LONG, net.goui.cosmicdungeon.economy.pricing.VendorPriceBreakdown::base,
                ByteBufCodecs.VAR_LONG, net.goui.cosmicdungeon.economy.pricing.VendorPriceBreakdown::enchantments,
                ByteBufCodecs.VAR_LONG, net.goui.cosmicdungeon.economy.pricing.VendorPriceBreakdown::curses,
                ByteBufCodecs.VAR_LONG, net.goui.cosmicdungeon.economy.pricing.VendorPriceBreakdown::adjustments,
                ByteBufCodecs.VAR_LONG, net.goui.cosmicdungeon.economy.pricing.VendorPriceBreakdown::total,
                net.goui.cosmicdungeon.economy.pricing.VendorPriceBreakdown::new);
        private static final StreamCodec<RegistryFriendlyByteBuf, Line> LINE_CODEC = StreamCodec.composite(
                ItemStack.STREAM_CODEC, Line::stack, BREAKDOWN_CODEC, Line::breakdown, Line::new);
        public static final Type<S2C_VendorSaleQuote> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("cosmicdungeon", "vendor_sale_quote"));
        public static final StreamCodec<RegistryFriendlyByteBuf, S2C_VendorSaleQuote> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.INT, S2C_VendorSaleQuote::containerId,
                UUID_CODEC, S2C_VendorSaleQuote::sessionId,
                ByteBufCodecs.stringUtf8(36), S2C_VendorSaleQuote::token,
                ByteBufCodecs.VAR_LONG, S2C_VendorSaleQuote::totalTrace,
                LINE_CODEC.apply(ByteBufCodecs.list(41)), S2C_VendorSaleQuote::lines,
                S2C_VendorSaleQuote::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }
}
