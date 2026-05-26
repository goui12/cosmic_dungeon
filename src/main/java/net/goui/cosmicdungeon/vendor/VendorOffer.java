package net.goui.cosmicdungeon.vendor;

import net.goui.cosmicdungeon.economy.CurrencyDenomination;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public record VendorOffer(
        ResourceLocation id,
        ItemStack result,
        Cost cost,
        Integer maxUses,
        Integer requiredFactionTier,
        String requiredProgressionFlag,
        Integer requiredNpcTier
) {
    public record Cost(long amount, CurrencyDenomination denomination) {}
}
