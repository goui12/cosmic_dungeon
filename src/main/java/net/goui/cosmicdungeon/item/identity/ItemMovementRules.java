package net.goui.cosmicdungeon.item.identity;

import net.goui.cosmicdungeon.component.ModDataComponents;
import net.goui.cosmicdungeon.playerclass.api.*;
import net.goui.cosmicdungeon.util.ModTags;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;

/** Trading doc 1byHfuC0G_lb0IRrgO3kblLYP06AY8gJWm9bJOMlrFIc (2026-08-18).
 * Class-issued gear stays no-drop. Other explicit bindings require owner-safe storage.
 * Ordinary named loot remains droppable; do not conflate sale eligibility with no-drop. */
public final class ItemMovementRules {
    public record Flags(boolean noDrop, boolean privateStorage) {
        Flags merge(Flags other) { return new Flags(noDrop || other.noDrop, privateStorage || other.privateStorage); }
    }
    private static final Flags FREE = new Flags(false, false), RESTRICTED = new Flags(true, true);
    // Defensive inspection budget, not a gameplay modifier. Unknown deeper content fails closed.
    private static final int MAX_DEPTH = 8, MAX_STACKS = 256;
    private ItemMovementRules() {}
    public static Flags flags(ItemStack stack) { return inspect(stack, 0, new int[]{MAX_STACKS}); }
    private static Flags inspect(ItemStack stack, int depth, int[] budget) {
        if (stack == null || stack.isEmpty()) return FREE;
        if (net.goui.cosmicdungeon.economy.DeathCurrencyService.marked(stack)) return RESTRICTED;
        if (depth > MAX_DEPTH || --budget[0] < 0) return RESTRICTED;
        boolean issued = ClassItemUtil.hasAnyAttunementMetadata(stack) || stack.getItem() instanceof ClassBoundItem
                || stack.has(ModDataComponents.D1_ABILITY.get())
                || stack.is(ModTags.Items.CLASS_RESTRICTED_JUDICATOR) || stack.is(ModTags.Items.CLASS_RESTRICTED_BOGATYR)
                || stack.is(ModTags.Items.CLASS_RESTRICTED_DRAGOON) || stack.is(ModTags.Items.CLASS_RESTRICTED_PYROCLAST)
                || stack.is(ModTags.Items.CLASS_RESTRICTED_THEURGIST) || stack.is(ModTags.Items.CLASS_RESTRICTED_VENEFEX)
                || stack.is(ModTags.Items.CLASS_RESTRICTED_METALMANCER) || stack.is(ModTags.Items.CLASS_RESTRICTED_DEADEYE);
        boolean noDrop = issued;
        boolean privateStorage = issued || stack.has(ModDataComponents.CHOP_OWNER.get())
                || stack.has(ModDataComponents.DUNGEON_RETURN_TARGET.get());
        var custom = stack.get(DataComponents.CUSTOM_DATA);
        if (custom != null) {
            var tag = custom.copyTag();
            noDrop |= tag.contains("no_drop") || tag.contains("class_issued");
            privateStorage |= noDrop || tag.contains("owner") || tag.contains("owner_uuid")
                    || tag.contains("bound") || tag.contains("quest_bound") || tag.contains("no_trade");
        }
        var result = new Flags(noDrop, privateStorage);
        var bundle = stack.get(DataComponents.BUNDLE_CONTENTS);
        if (bundle != null) for (var child : bundle.items()) {
            result = result.merge(inspect(child, depth + 1, budget));
            if (budget[0] < 0) return RESTRICTED;
        }
        var container = stack.get(DataComponents.CONTAINER);
        if (container != null) for (var child : container.nonEmptyItems()) {
            result = result.merge(inspect(child, depth + 1, budget));
            if (budget[0] < 0) return RESTRICTED;
        }
        return result;
    }
    public static boolean portable(ItemStack stack) {
        return stack.has(DataComponents.BUNDLE_CONTENTS) || stack.has(DataComponents.CONTAINER);
    }
}
