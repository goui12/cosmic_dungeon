package net.goui.cosmicdungeon.playerclass.theurgist;

import net.goui.cosmicdungeon.playerclass.api.ClassData;
import net.goui.cosmicdungeon.playerclass.resource.*;
import net.goui.cosmicdungeon.transaction.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/** One physical splash and its resource debit share the same verified native player save. */
public final class TheurgistCrafting {
    private TheurgistCrafting(){}
    public static int freeSlot(ServerPlayer p){
        // Main/hotbar only: a full inventory must not auto-equip, drop, or spend supplies.
        for(int slot=0;slot<36;slot++)if(p.getInventory().getItem(slot).isEmpty())return slot;
        return -1;
    }
    public static boolean craft(ServerPlayer p,long run,boolean epic){
        if(!ClassData.getClassId(p).equals("theurgist")||!p.isAlive()||p.isDeadOrDying()
                ||ClassResourceService.activeRun(p).filter(r->r.runId()==run).isEmpty()
                ||!InventoryTransactionGuard.beforeCurrentInventoryAction(p))return false;
        int slot=freeSlot(p);if(slot<0)return false;
        var root=p.getPersistentData().getCompoundOrEmpty(ClassData.ROOT_TAG).copy();
        var ledger=ClassResourceLedger.forRun(root,run);int cost=TheurgistPotionCatalog.cost(epic);
        if(ledger.amount(ClassResourceKind.BREWING_SUPPLIES)<cost)return false;
        var potion=TheurgistPotionCatalog.create(epic,p.getRandom());
        if(potion.isEmpty()||potion.getCount()!=1)throw new IllegalStateException("Invalid skill potion delivery");
        try{
            p.getInventory().setItem(slot,potion.copy());
            var next=ledger.withAmount(ClassResourceKind.BREWING_SUPPLIES,ledger.amount(ClassResourceKind.BREWING_SUPPLIES)-cost).nextRevision();
            p.getPersistentData().put(ClassData.ROOT_TAG,next.applyTo(root));
            PlayerSaveProof.snapshot(p);
        }catch(RuntimeException staging){
            p.getInventory().setItem(slot,ItemStack.EMPTY);p.getPersistentData().put(ClassData.ROOT_TAG,root);throw staging;
        }
        if(!PlayerSaveProof.save(p)){
            ClassResourceService.hold(p,new IllegalStateException("Crafted potion save requires reconciliation"));return false;
        }
        p.getInventory().setChanged();p.inventoryMenu.broadcastChanges();
        ClassResourceService.pulse(p,p.level().getServer().getTickCount(),false);return true;
    }
}
