package net.goui.cosmicdungeon.vendor;

import net.minecraft.resources.ResourceLocation;

import java.util.List;

public record VendorProfile(
        ResourceLocation id,
        String displayName,
        String vendorType,
        boolean requiredVillageAccess,
        String requiredNpcSystem,
        Integer requiredNpcTier,
        ResourceLocation requiredFactionId,
        Integer requiredFactionTier,
        List<VendorOffer> buyOffers,
        BuybackConfig buyback
) {
    public record BuybackConfig(String pricingGroup, List<BuybackRule> rules) {}

    public record BuybackRule(ResourceLocation itemId, long minTraceValue, long maxTraceValue, double multiplier) {}
}
