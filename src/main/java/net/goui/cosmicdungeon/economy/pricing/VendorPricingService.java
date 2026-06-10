package net.goui.cosmicdungeon.economy.pricing;

import net.goui.cosmicdungeon.playerclass.api.ClassItemUtil;
import net.goui.cosmicdungeon.util.ModTags;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.Equippable;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class VendorPricingService {
    private VendorPricingService() {}

    private static final List<EquipmentSlot> CLASS_ARMOR_SET_SLOTS = List.of(
            EquipmentSlot.HEAD,
            EquipmentSlot.CHEST,
            EquipmentSlot.LEGS,
            EquipmentSlot.FEET
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
        if (player == null) return detected;

        Map<ClassArmorSetKey, EnumMap<EquipmentSlot, ArmorSetPiece>> piecesBySet = collectClassArmorPieces(player);
        for (Map.Entry<ClassArmorSetKey, EnumMap<EquipmentSlot, ArmorSetPiece>> entry : piecesBySet.entrySet()) {
            EnumMap<EquipmentSlot, ArmorSetPiece> pieces = entry.getValue();
            if (!pieces.keySet().containsAll(CLASS_ARMOR_SET_SLOTS)) continue;

            long traceValue = 0L;
            for (EquipmentSlot slot : CLASS_ARMOR_SET_SLOTS) {
                traceValue += pieces.get(slot).traceValue();
            }
            long payout = applyClassArmorSetBonus(traceValue);
            if (payout <= 0L) continue;

            detected.add(new CompleteSetValue(entry.getKey().setId(), payout, CLASS_ARMOR_SET_SLOTS.size()));
        }
        return detected;
    }

    public static Optional<DetectedClassArmorSet> findDetectedClassArmorSet(Player player, String setId) {
        if (player == null || setId == null || setId.isBlank()) return Optional.empty();
        Map<ClassArmorSetKey, EnumMap<EquipmentSlot, ArmorSetPiece>> piecesBySet = collectClassArmorPieces(player);
        for (Map.Entry<ClassArmorSetKey, EnumMap<EquipmentSlot, ArmorSetPiece>> entry : piecesBySet.entrySet()) {
            if (!entry.getKey().setId().equals(setId)) continue;
            EnumMap<EquipmentSlot, ArmorSetPiece> pieces = entry.getValue();
            if (!pieces.keySet().containsAll(CLASS_ARMOR_SET_SLOTS)) return Optional.empty();

            List<Integer> slots = new ArrayList<>();
            long traceValue = 0L;
            for (EquipmentSlot armorSlot : CLASS_ARMOR_SET_SLOTS) {
                ArmorSetPiece piece = pieces.get(armorSlot);
                slots.add(piece.inventorySlot());
                traceValue += piece.traceValue();
            }
            long payout = applyClassArmorSetBonus(traceValue);
            if (payout <= 0L) return Optional.empty();
            return Optional.of(new DetectedClassArmorSet(entry.getKey().setId(), payout, slots));
        }
        return Optional.empty();
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

    private static Map<ClassArmorSetKey, EnumMap<EquipmentSlot, ArmorSetPiece>> collectClassArmorPieces(Player player) {
        Map<ClassArmorSetKey, EnumMap<EquipmentSlot, ArmorSetPiece>> piecesBySet = new HashMap<>();
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!ClassItemUtil.hasCompleteValidAttunement(stack)) continue;

            EquipmentSlot armorSlot = classArmorSetSlot(stack);
            if (armorSlot == null) continue;

            String classId = ClassItemUtil.getClassAttunement(stack);
            Integer dungeon = ClassItemUtil.getDungeon(stack);
            Integer tier = ClassItemUtil.getTier(stack);
            Long trace = ClassItemUtil.getTraceValue(stack);
            if (classId == null || dungeon == null || tier == null || trace == null) continue;

            ClassArmorSetKey key = new ClassArmorSetKey(classId, dungeon, tier);
            EnumMap<EquipmentSlot, ArmorSetPiece> pieces = piecesBySet.computeIfAbsent(key, ignored -> new EnumMap<>(EquipmentSlot.class));
            ArmorSetPiece current = pieces.get(armorSlot);
            if (current == null || trace > current.traceValue()) {
                pieces.put(armorSlot, new ArmorSetPiece(i, trace));
            }
        }
        return piecesBySet;
    }

    private static long applyClassArmorSetBonus(long traceValue) {
        if (traceValue <= 0L) return 0L;
        if (traceValue > Long.MAX_VALUE / 5L) return Long.MAX_VALUE;
        return (traceValue * 5L) / 4L;
    }

    private static EquipmentSlot classArmorSetSlot(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        Equippable equippable = stack.get(DataComponents.EQUIPPABLE);
        if (equippable == null || !CLASS_ARMOR_SET_SLOTS.contains(equippable.slot())) return null;
        return equippable.slot();
    }

    private static String itemId(ItemStack stack) {
        ResourceLocation key = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return key == null ? "minecraft:air" : key.toString();
    }

    public record CompleteSetValue(String setId, long traceValue, int pieceCount) {}

    public record DetectedClassArmorSet(String setId, long traceValue, List<Integer> inventorySlots) {}

    private record ArmorSetPiece(int inventorySlot, long traceValue) {}

    private record ClassArmorSetKey(String classId, int dungeon, int tier) {
        String setId() {
            return "class_armor:" + classId + ":d" + dungeon + ":t" + tier;
        }
    }

}
