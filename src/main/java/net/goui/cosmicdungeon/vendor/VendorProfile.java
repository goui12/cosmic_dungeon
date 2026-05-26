package net.goui.cosmicdungeon.vendor;

import net.minecraft.resources.ResourceLocation;

import java.util.List;

public record VendorProfile(
        ResourceLocation id,
        String displayName,
        String vendorType,
        ResourceLocation requiredFactionId,
        Integer requiredFactionTier,
        String requiredProgressionFlag,
        Integer requiredNpcTier,
        List<VendorOffer> buyOffers,
        BuybackConfig buyback
) {
    public record BuybackConfig(String pricingGroup, List<BuybackRule> rules) {}

    public record BuybackRule(ResourceLocation itemId, long minTraceValue, long maxTraceValue, double multiplier) {}
}
