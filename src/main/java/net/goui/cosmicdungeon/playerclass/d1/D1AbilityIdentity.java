package net.goui.cosmicdungeon.playerclass.d1;

import net.goui.cosmicdungeon.component.ModDataComponents;
import net.goui.cosmicdungeon.item.identity.ItemProvenanceService;
import net.goui.cosmicdungeon.playerclass.api.ClassItemUtil;
import net.goui.cosmicdungeon.playerclass.dragoon.repair.RepairComponents;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

/** Compatibility for already-authored gear. Never a sale-price or loot-provenance check. */
public final class D1AbilityIdentity {
    private D1AbilityIdentity() {}
    public static D1AmmunitionCatalog.Facts facts(ItemStack stack) {
        var potion = stack.get(DataComponents.POTION_CONTENTS);
        var fireworks = stack.get(DataComponents.FIREWORKS);
        var name = stack.get(DataComponents.CUSTOM_NAME);
        return new D1AmmunitionCatalog.Facts(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(),
                stack.get(ModDataComponents.D1_ABILITY.get()), name == null ? "" : name.getString(),
                potion == null ? "" : potion.potion().flatMap(p -> p.unwrapKey()).map(k -> k.location().toString()).orElse(""),
                potion != null && !potion.customEffects().isEmpty(), fireworks == null ? 0 : fireworks.explosions().size());
    }
    public static String identify(ItemStack stack) {
        if (stack == null || stack.isEmpty()
                || !D1AmmunitionCatalog.candidate(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString())) return null;
        return D1AmmunitionCatalog.identify(facts(stack));
    }
    /** Developer-selected vanilla stack only; append one marker without rebuilding any component. */
    public static ItemStack adoptCopy(ItemStack original, String id) {
        if (original == null || original.isEmpty() || !D1AmmunitionCatalog.canAdopt(facts(original), id))
            throw new IllegalArgumentException("Unknown, already marked, conflicting identity or wrong ammunition signature");
        if (ItemProvenanceService.present(original) || RepairComponents.marked(original))
            throw new IllegalArgumentException("Loot/provenance and repair items cannot become class ammunition");
        if (!D1AmmunitionCatalog.bindingAllowed(id, ClassItemUtil.hasAnyAttunementMetadata(original),
                ClassItemUtil.hasCompleteValidAttunement(original), ClassItemUtil.getClassAttunement(original),
                ClassItemUtil.getDungeon(original), ClassItemUtil.getTier(original)))
            throw new IllegalArgumentException("Incomplete, foreign-class or non-D1 attunement must be reviewed");
        var copy = original.copy();
        copy.set(ModDataComponents.D1_ABILITY.get(), id);
        return copy;
    }
    // TODO(M72, world adoption): held preview/apply/undo is an authoring tool, not a migration.
    // Preserve unknown markers and original stacks in unloaded/nested storage and preset files;
    // establish trusted template/drop ownership and back up the world before any later rollout.
    // Existing legacy names require matching signatures; the native anvil guard remains active.
}
