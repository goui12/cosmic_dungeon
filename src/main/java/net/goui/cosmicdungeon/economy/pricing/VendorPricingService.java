package net.goui.cosmicdungeon.economy.pricing;

import net.goui.cosmicdungeon.playerclass.api.ClassItemUtil;
import net.goui.cosmicdungeon.util.ModTags;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public final class VendorPricingService {
    private VendorPricingService() {}

    public static VendorPrice getSellValue(ItemStack stack, String vendorType) {
        if (stack.isEmpty()) {
            return new VendorPrice(0L, "empty_stack");
        }

        if (ClassItemUtil.hasCompleteValidAttunement(stack)) {
            String classId = ClassItemUtil.getClassAttunement(stack);
            int dungeon = ClassItemUtil.getDungeon(stack);
            int tier = ClassItemUtil.getTier(stack);
            long trace = ClassItemUtil.getTraceValue(stack);
            String source = classId + ":d" + dungeon + ":t" + tier;
            if (trace > 0L) {
                return new VendorPrice(trace, "class_attuned:" + source);
            }
            return new VendorPrice(0L, "class_attuned_zero:" + source);
        }

        String itemId = itemId(stack);
        if (ClassItemUtil.hasAnyAttunementMetadata(stack)) {
            return new VendorPrice(0L, "unsupported:invalid_class_attunement:" + itemId);
        }

        VendorValueCategory category = categorize(stack);
        if (category == VendorValueCategory.CLASS_ISSUED_GEAR) {
            return new VendorPrice(0L, "class_issued_tag:" + itemId);
        }

        return new VendorPrice(0L, "unsupported:" + category + ":" + itemId);
    }

    private static VendorValueCategory categorize(ItemStack stack) {
        if (stack.is(ModTags.Items.CLASS_RESTRICTED_JUDICATOR)
                || stack.is(ModTags.Items.CLASS_RESTRICTED_METALMANCER)
                || stack.is(ModTags.Items.CLASS_RESTRICTED_BOGATYR)
                || stack.is(ModTags.Items.CLASS_RESTRICTED_DEADEYE)
                || stack.is(ModTags.Items.CLASS_RESTRICTED_DRAGOON)
                || stack.is(ModTags.Items.CLASS_RESTRICTED_PYROCLAST)
                || stack.is(ModTags.Items.CLASS_RESTRICTED_THEURGIST)
                || stack.is(ModTags.Items.CLASS_RESTRICTED_VENEFEX)) {
            return VendorValueCategory.CLASS_ISSUED_GEAR;
        }
        return VendorValueCategory.UNSUPPORTED;
    }

    private static String itemId(ItemStack stack) {
        ResourceLocation key = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return key == null ? "minecraft:air" : key.toString();
    }

}
