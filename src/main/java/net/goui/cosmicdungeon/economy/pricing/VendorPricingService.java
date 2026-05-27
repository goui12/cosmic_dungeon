package net.goui.cosmicdungeon.economy.pricing;

import net.goui.cosmicdungeon.util.ModTags;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class VendorPricingService {
    private VendorPricingService() {}

    private static final String DEFAULT_VENDOR_TYPE = "default";

    private static final Map<String, List<GearSetDefinition>> SET_DEFINITIONS_BY_VENDOR = Map.of(
            DEFAULT_VENDOR_TYPE,
            List.of(new GearSetDefinition(
                    "judicator_d2_t1_chainmail",
                    Set.of(
                            "cosmicdungeon:visor_of_the_resolute",
                            "cosmicdungeon:cuirass_of_purpose",
                            "cosmicdungeon:chausses_of_the_pledge",
                            "cosmicdungeon:sabatons_of_the_unheard_oath"
                    ),
                    100L,
                    10L
            ))
    );

    public static VendorPrice getSellValue(ItemStack stack, String vendorType) {
        if (stack.isEmpty()) {
            return new VendorPrice(0L, "empty_stack");
        }

        String itemId = itemId(stack);
        for (GearSetDefinition definition : setDefinitionsFor(vendorType)) {
            if (definition.pieceItemIds().contains(itemId)) {
                return new VendorPrice(definition.individualPieceTraceValue(), "set_piece:" + definition.id());
            }
        }

        VendorValueCategory category = categorize(stack);
        if (category == VendorValueCategory.CLASS_ISSUED_GEAR) {
            return new VendorPrice(0L, "class_issued_tag:" + itemId);
        }

        return new VendorPrice(0L, "unsupported:" + category + ":" + itemId);
    }

    public static List<CompleteSetValue> detectCompleteSets(Player player, String vendorType) {
        Set<String> itemIds = new HashSet<>();
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty()) {
                itemIds.add(itemId(stack));
            }
        }

        List<CompleteSetValue> detected = new ArrayList<>();
        for (GearSetDefinition definition : setDefinitionsFor(vendorType)) {
            if (itemIds.containsAll(definition.pieceItemIds())) {
                detected.add(new CompleteSetValue(definition.id(), definition.fullSetTraceValue(), definition.pieceItemIds().size()));
            }
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
