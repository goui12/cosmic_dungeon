package net.goui.cosmicdungeon.item.identity;

import net.goui.cosmicdungeon.component.ModDataComponents;
import net.goui.cosmicdungeon.playerclass.api.ClassItemEquipmentGuard;
import net.goui.cosmicdungeon.playerclass.dragoon.repair.RepairComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Server-issued category/identity. No name-based migration or routine inventory scan. */
public final class ItemProvenanceService {
    private ItemProvenanceService() {}
    public static boolean requiresProvenance(ItemStack stack) {
        return ClassItemEquipmentGuard.isGuardedEquipment(stack) || stack.is(Items.SPYGLASS);
    }
    public static String itemKey(ItemStack stack) { return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(); }
    public static boolean present(ItemStack stack) { return stack.has(ModDataComponents.ITEM_PROVENANCE.get()); }
    public static ItemProvenance read(ItemStack stack) {
        var value = ItemProvenance.parse(stack.get(ModDataComponents.ITEM_PROVENANCE.get())).orElse(null);
        return value != null && value.matches(itemKey(stack)) ? value : null;
    }
    public static D1LootCatalog.Entry named(ItemStack stack) {
        var provenance = read(stack);
        return provenance == null ? null : D1LootCatalog.find(provenance.itemId());
    }
    /** Copy-first: retain quantity and EVERY existing component, including lore/damage/enchantments. */
    public static ItemStack adoptCopy(ItemStack original, ItemProvenance provenance) {
        if (original.isEmpty() || !provenance.matches(itemKey(original)) || present(original))
            throw new IllegalArgumentException("Wrong base item or already classified");
        if (!requiresProvenance(original) || RepairComponents.marked(original))
            throw new IllegalArgumentException("Not ordinary authored equipment");
        var copy = original.copy();
        copy.set(ModDataComponents.ITEM_PROVENANCE.get(), provenance.encode());
        return copy;
    }
    /** Only the server's configured vendor-delivery path calls this; never a client stack. */
    public static ItemStack vendorCopy(ItemStack configured) {
        if (present(configured)) {
            var provenance = read(configured);
            if (provenance == null || !ItemProvenance.RETAIL.equals(provenance.origin()) || !provenance.itemId().isEmpty())
                throw new IllegalArgumentException("Named dungeon loot or invalid provenance cannot be retail stock");
            return configured.copy();
        }
        if (!requiresProvenance(configured) || RepairComponents.marked(configured)) return configured.copy();
        return adoptCopy(configured, new ItemProvenance(ItemProvenance.RETAIL, "", itemKey(configured)));
    }
    // TODO(M72, legacy adoption): build reviewed template/container/spawner authoring mappings
    // from trusted context, never display names. Preserve counts and every component; include
    // unloaded chunks and stored inventory. No automatic world/preset scan is authorized here.
    // Already class-attuned stacks remain protected without conversion. A malformed/unknown
    // future provenance value stays on its item and is rejected rather than silently repaired.
}
