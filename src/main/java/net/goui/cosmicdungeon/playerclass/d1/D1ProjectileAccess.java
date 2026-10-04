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
        if(D1AmmunitionCatalog.find(id)==null){
            String item=net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
            return D1AmmunitionCatalog.registered(item)?D1CombatRules.Ammunition.DENIED:D1CombatRules.Ammunition.VANILLA;
        }
        if(projectile.getOwner() instanceof net.minecraft.world.entity.LivingEntity owner&&owner.level()==projectile.level())
            return permission(owner,stack,id);
        return D1CombatRules.Ammunition.DENIED;
    }
    public static String classId(net.minecraft.world.entity.LivingEntity owner){
        return owner instanceof ServerPlayer player?ClassData.getClassId(player)
            :owner instanceof net.goui.cosmicdungeon.mercenary.MercenaryEntity mercenary&&mercenary.contract()!=null
            ?mercenary.contract().classId():"";
    }
    public static D1CombatRules.Ammunition permission(net.minecraft.world.entity.LivingEntity owner,ItemStack stack,String id){
        if (D1AmmunitionCatalog.find(id) == null) {
            String item = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
            return D1AmmunitionCatalog.registered(item)
                    ? D1CombatRules.Ammunition.DENIED : D1CombatRules.Ammunition.VANILLA;
        }
        String cls=classId(owner);
        boolean active=false;
        if(owner instanceof ServerPlayer player){
            var run=D1Members.run(player.level()).orElse(null);
            active=run!=null&&D1Members.inside(player,run);
        }else if(owner instanceof net.goui.cosmicdungeon.mercenary.MercenaryEntity mercenary){
            active=net.goui.cosmicdungeon.mercenary.MercenaryBrain.hirer(mercenary)!=null
                &&net.goui.cosmicdungeon.mercenary.MercenaryInventory.permitted(stack,mercenary.contract());
        }
        boolean metadata = ClassItemUtil.hasAnyAttunementMetadata(stack);
        boolean bound = D1AmmunitionCatalog.bindingAllowed(id, metadata,
                ClassItemUtil.hasCompleteValidAttunement(stack), ClassItemUtil.getClassAttunement(stack),
                ClassItemUtil.getDungeon(stack), ClassItemUtil.getTier(stack))
                && (!metadata || cls.equals(ClassItemUtil.getClassAttunement(stack)))
                && !RepairComponents.marked(stack) && !ItemProvenanceService.present(stack);
        return D1CombatRules.ammunition(id, cls, active, bound);
    }
}
