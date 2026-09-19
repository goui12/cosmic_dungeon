package net.goui.cosmicdungeon.economy.pricing;
import net.goui.cosmicdungeon.config.VendorCatalog;
import net.goui.cosmicdungeon.config.VendorPricesConfig;
import net.goui.cosmicdungeon.playerclass.dragoon.repair.RepairComponents;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import java.util.ArrayList;

/** Universal prices, server revalidated. SERVER config is also synchronized for client previews. */
public final class VendorPricingService {
    private VendorPricingService() {}
    public static VendorPrice getSellValue(ItemStack stack,String vendorType) {
        try { return calculateSellValue(stack, vendorType, ItemTransferPolicy.Action.VENDOR_SALE); }
        catch (ArithmeticException overflow) { return new VendorPrice(0, "price_overflow"); }
    }
    public static boolean tradePriced(ItemStack stack){
        try{return calculateSellValue(stack,"",ItemTransferPolicy.Action.TRADE).approved();}
        catch(ArithmeticException overflow){return false;}
    }
    private static VendorPrice calculateSellValue(ItemStack stack,String vendorType,ItemTransferPolicy.Action action) {
        if(stack.is(net.goui.cosmicdungeon.item.ModItems.RAW_FARROWS_CHOP.get())
                &&!stack.has(net.goui.cosmicdungeon.component.ModDataComponents.DUNGEON_RETURN_TARGET.get())
                &&stack.getCount()==1)
            return new VendorPrice(VendorPricesConfig.FARROW_RAW_PURCHASE.get(),"raw_chop_owner_checked_at_sale",true);
        var rejection = ItemTransferRules.rejection(stack, action);
        if (rejection != ItemTransferPolicy.Rejection.NONE) return new VendorPrice(0, "restricted:" + rejection);
        String enchantmentError = validateEnchantments(stack);
        if (enchantmentError != null) return new VendorPrice(0, enchantmentError);
        // Ensure the shared spec/catalog has been initialized.
        var spec=VendorPricesConfig.SPEC;
        var named = net.goui.cosmicdungeon.item.identity.ItemProvenanceService.named(stack);
        if (named != null) {
            var configured = VendorCatalog.namedD1(named.id());
            if (configured == null) return new VendorPrice(0, "unpriced_named:" + named.id());
            return NamedLootPricing.quote(named.id(),
                    net.goui.cosmicdungeon.item.identity.ItemProvenanceService.itemKey(stack), configured.get(),
                    stack.get(net.goui.cosmicdungeon.component.ModDataComponents.VENDOR_PURCHASE_CAP.get()), stack.getCount());
        }
        String key;
        if (RepairComponents.marked(stack)) {
            key=RepairComponents.key(stack);
            if (key==null) return new VendorPrice(0,"invalid_repair_component");
            key="repair__"+key;
        } else {
            var id=BuiltInRegistries.ITEM.getKey(stack.getItem());
            if(!id.getNamespace().equals("minecraft")) return new VendorPrice(0,"unpriced:"+id);
            key=id.getPath();
            var potion=stack.get(DataComponents.POTION_CONTENTS);
            if (potion!=null) {
                if (!potion.customEffects().isEmpty() || potion.potion().isEmpty()
                        || stack.getOrDefault(DataComponents.POTION_DURATION_SCALE,1.0F)!=1.0F)
                    return new VendorPrice(0,"unpriced_custom_potion");
                var potionId=potion.potion().get().unwrapKey().orElse(null);
                if(potionId==null || !potionId.location().getNamespace().equals("minecraft")) return new VendorPrice(0,"unpriced_potion");
                key+="__"+potionId.location().getPath();
            }
        }
        var base=VendorCatalog.item(key);
        if(base==null || base.purchase().get()<0) return new VendorPrice(0,"unpriced:"+key);
        long basePrice = base.purchase().get();
        long baseAdjustment = 0;
        long enchantmentPrice = 0;
        long cursePrice = 0;
        // D83: drinking milk cannot turn a 3-Trace input into a 50-Trace output.
        if(key.equals("bucket"))
            baseAdjustment = Math.subtractExact(Math.min(basePrice, VendorCatalog.item("milk_bucket").purchase().get()), basePrice);
        var enchants=stack.getOrDefault(DataComponents.ENCHANTMENTS,ItemEnchantments.EMPTY);
        for(var entry:enchants.entrySet()) {
            var holder=entry.getKey(); int level=entry.getIntValue();
            var id=holder.unwrapKey().orElseThrow();
            var adjustment=VendorCatalog.enchantment(id.location().getPath());
            long amount = Math.multiplyExact(adjustment.purchase().get(), level);
            if (holder.is(net.minecraft.tags.EnchantmentTags.CURSE)) cursePrice = Math.addExact(cursePrice, amount);
            else enchantmentPrice = Math.addExact(enchantmentPrice, amount);
        }
        Long conversionCap=stack.get(net.goui.cosmicdungeon.component.ModDataComponents.VENDOR_PURCHASE_CAP.get());
        var breakdown = VendorPriceBreakdown.calculate(basePrice, enchantmentPrice, cursePrice,
                baseAdjustment, conversionCap, stack.getCount());
        return new VendorPrice(breakdown.total(), "catalog:" + key, true, breakdown);
    }
    /** The named table is not permission to sell impossible enchantments. */
    private static String validateEnchantments(ItemStack stack) {
        var spec = VendorPricesConfig.SPEC;
        var checked = new ArrayList<net.minecraft.core.Holder<Enchantment>>();
        for (var entry : stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY).entrySet()) {
            var holder = entry.getKey();
            var id = holder.unwrapKey().orElse(null);
            int level = entry.getIntValue();
            if (id == null || !id.location().getNamespace().equals("minecraft") || level < 1
                    || level > holder.value().getMaxLevel() || !stack.supportsEnchantment(holder))
                return "invalid_enchantment";
            for (var previous : checked) if (!Enchantment.areCompatible(previous, holder))
                return "incompatible_enchantments";
            checked.add(holder);
            if (id.location().getPath().equals("fortune")) return "excluded_fortune";
            if (VendorCatalog.enchantment(id.location().getPath()) == null) return "unpriced_enchantment";
        }
        return null;
    }
    // Approved zero prices now require server-quoted, explicit surrender; bulk sale excludes them.
    // TODO(M03/M118, licensed TEST): Gear Trading 2.0 (2026-08-18) requires exact item
    // records and explicit zero-price consent. Batch25 persists the quote breakdown, full
    // serialized components and account outcome. Verify native interrupted saves/quote UI;
    // no full-save uniqueness claim is possible from manually restored partial backups.
    // Named D1 prices now require trusted provenance; the 2026-08-28 final-item table
    // overrides older base/enchantment totals. No extra enchantment premium or name lookup.
    // TODO(M72): adopt existing authored drops from reviewed context before rollout.
    // The new held-stack developer command does not establish legacy world coverage.
}
