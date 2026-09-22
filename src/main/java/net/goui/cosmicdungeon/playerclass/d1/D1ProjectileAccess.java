package net.goui.cosmicdungeon.playerclass.d1;

import net.goui.cosmicdungeon.dungeon.d1.D1Members;
import net.goui.cosmicdungeon.item.identity.ItemProvenanceService;
import net.goui.cosmicdungeon.playerclass.api.ClassData;
import net.goui.cosmicdungeon.playerclass.api.ClassItemUtil;
import net.goui.cosmicdungeon.playerclass.dragoon.repair.RepairComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;

/** Resolve at impact: a logged-out, departed, dead or class-switched owner gets no D1 effect. */
public final class D1ProjectileAccess {
    private D1ProjectileAccess() {}

    public static D1CombatRules.Ammunition permission(Projectile projectile, ItemStack stack, String id) {
        if (D1AmmunitionCatalog.find(id) == null) {
            String item = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
            return D1AmmunitionCatalog.registered(item)
                    ? D1CombatRules.Ammunition.DENIED : D1CombatRules.Ammunition.VANILLA;
        }
        if (!(projectile.getOwner() instanceof ServerPlayer owner))
            return D1CombatRules.Ammunition.DENIED;
        var run = D1Members.run(owner.level()).orElse(null);
        String cls = ClassData.getClassId(owner);
        boolean active = projectile.level() == owner.level() && run != null && D1Members.inside(owner, run);
        boolean metadata = ClassItemUtil.hasAnyAttunementMetadata(stack);
        boolean bound = D1AmmunitionCatalog.bindingAllowed(id, metadata,
                ClassItemUtil.hasCompleteValidAttunement(stack), ClassItemUtil.getClassAttunement(stack),
                ClassItemUtil.getDungeon(stack), ClassItemUtil.getTier(stack))
                && (!metadata || cls.equals(ClassItemUtil.getClassAttunement(stack)))
                && !RepairComponents.marked(stack) && !ItemProvenanceService.present(stack);
        return D1CombatRules.ammunition(id, cls, active, bound);
    }
}
