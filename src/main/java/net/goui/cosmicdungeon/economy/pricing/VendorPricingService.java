package net.goui.cosmicdungeon.economy.pricing;

import net.goui.cosmicdungeon.playerclass.api.ClassItemUtil;
import net.goui.cosmicdungeon.util.ModTags;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class VendorPricingService {
    private VendorPricingService() {}

    private static final String DEFAULT_VENDOR_TYPE = "default";

    private static final Map<String, List<GearSetDefinition>> SET_DEFINITIONS_BY_VENDOR = Map.of(
            DEFAULT_VENDOR_TYPE, List.of()
    );

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

    public static List<CompleteSetValue> detectCompleteSets(Player player, String vendorType) {
        List<CompleteSetValue> detected = new ArrayList<>();
        for (GearSetDefinition definition : setDefinitionsFor(vendorType)) {
            // No class item set ids or equipment-slot definitions exist yet; keep this stable for future data-backed sets.
        }
        return detected;
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

    private static List<GearSetDefinition> setDefinitionsFor(String vendorType) {
        String key = vendorType == null || vendorType.isBlank() ? DEFAULT_VENDOR_TYPE : vendorType.toLowerCase(Locale.ROOT);
        return SET_DEFINITIONS_BY_VENDOR.getOrDefault(key, SET_DEFINITIONS_BY_VENDOR.get(DEFAULT_VENDOR_TYPE));
    }

    private static String itemId(ItemStack stack) {
        ResourceLocation key = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return key == null ? "minecraft:air" : key.toString();
    }

    public record CompleteSetValue(String setId, long traceValue, int pieceCount) {}

    public static Optional<GearSetDefinition> findSetDefinition(String vendorType, String setId) {
        if (setId == null || setId.isBlank()) return Optional.empty();
        return setDefinitionsFor(vendorType).stream().filter(def -> def.id().equals(setId)).findFirst();
    }

    public static record GearSetDefinition(String id, Set<String> pieceItemIds, long fullSetTraceValue, long individualPieceTraceValue) {}

}
