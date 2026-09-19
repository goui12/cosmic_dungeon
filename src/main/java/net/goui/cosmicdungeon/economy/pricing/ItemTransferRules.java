package net.goui.cosmicdungeon.economy.pricing;
import net.goui.cosmicdungeon.component.ModDataComponents;
import net.goui.cosmicdungeon.playerclass.api.ClassItemUtil;
import net.goui.cosmicdungeon.playerclass.api.ClassBoundItem;
import net.goui.cosmicdungeon.util.ModTags;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

/** Restrictions precede pricing; display names never grant sale/trade eligibility. */
public final class ItemTransferRules {
    private ItemTransferRules() {}
    public static ItemTransferPolicy.Rejection rejection(ItemStack stack, ItemTransferPolicy.Action action) {
        if (stack == null || stack.isEmpty()) return ItemTransferPolicy.Rejection.EMPTY;
        return ItemTransferPolicy.evaluate(action, facts(stack));
    }
    public static boolean tradeEligible(ItemStack stack) {
        return rejection(stack, ItemTransferPolicy.Action.TRADE) == ItemTransferPolicy.Rejection.NONE
                && VendorPricingService.tradePriced(stack);
    }
    public static boolean vendorEligible(ItemStack stack) {
        return rejection(stack, ItemTransferPolicy.Action.VENDOR_SALE) == ItemTransferPolicy.Rejection.NONE;
    }
    private static boolean marker(ItemStack stack, String key) {
        var data = stack.get(DataComponents.CUSTOM_DATA);
        return data != null && data.copyTag().contains(key);
    }
    public static ItemTransferPolicy.Facts facts(ItemStack stack) {
        boolean repair = net.goui.cosmicdungeon.playerclass.dragoon.repair.RepairComponents.marked(stack);
        return new ItemTransferPolicy.Facts(stack.isEmpty(), commonProtected(stack),
                marker(stack, "no_trade"), marker(stack, "no_sale"),
                net.goui.cosmicdungeon.item.identity.ItemProvenanceService.present(stack),
                net.goui.cosmicdungeon.item.identity.ItemProvenanceService.read(stack) != null,
                net.goui.cosmicdungeon.item.identity.ItemProvenanceService.requiresProvenance(stack),
                repair, repair && net.goui.cosmicdungeon.playerclass.dragoon.repair.RepairComponents.key(stack) != null);
    }
    private static boolean commonProtected(ItemStack stack) {
        if (stack.isEmpty() || stack.has(ModDataComponents.D1_ABILITY.get()) || net.goui.cosmicdungeon.playerclass.d1.D1AbilityIdentity.identify(stack)!=null
                || ClassItemUtil.hasAnyAttunementMetadata(stack) || stack.getItem() instanceof ClassBoundItem
                || stack.has(ModDataComponents.DUNGEON_RETURN_TARGET.get())) return true;
        var id=BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (id.getNamespace().equals("cosmicdungeon") && (id.getPath().contains("farrows_chop")
                || id.getPath().startsWith("bloom_") || id.getPath().equals("lesser_bloom"))) return true;
        if (stack.is(ModTags.Items.CLASS_RESTRICTED_JUDICATOR) || stack.is(ModTags.Items.CLASS_RESTRICTED_METALMANCER)
                || stack.is(ModTags.Items.CLASS_RESTRICTED_BOGATYR) || stack.is(ModTags.Items.CLASS_RESTRICTED_DEADEYE)
                || stack.is(ModTags.Items.CLASS_RESTRICTED_DRAGOON) || stack.is(ModTags.Items.CLASS_RESTRICTED_PYROCLAST)
                || stack.is(ModTags.Items.CLASS_RESTRICTED_THEURGIST) || stack.is(ModTags.Items.CLASS_RESTRICTED_VENEFEX)) return true;
        var data=stack.get(DataComponents.CUSTOM_DATA);
        if(data!=null) {
            var tag=data.copyTag();
            for(String marker:new String[]{"owner","owner_uuid","bound","quest_bound","class_issued","no_drop"})
                if(tag.contains(marker)) return true;
        }
        return false;
    }
}
