package net.goui.cosmicdungeon.vendor;

import net.goui.cosmicdungeon.economy.CurrencyDenomination;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public record VendorOffer(
        ResourceLocation id,
        ItemStack result,
        Cost cost,
        Integer maxUses,
        Integer maxPurchasesPerPlayer,
        Integer requiredFactionTier,
        String requiredProgressionFlag,
        Integer requiredNpcTier,
        List<String> requiredClasses
) {
    public record Cost(long amount, CurrencyDenomination denomination) {}
}
