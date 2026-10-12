package net.goui.cosmicdungeon.trade;

import java.util.*;
import net.goui.cosmicdungeon.component.ModDataComponents;
import net.goui.cosmicdungeon.dungeon.*;
import net.goui.cosmicdungeon.playerclass.dragoon.repair.RepairCustody;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/** Personal Chop registry after-images share the existing trade decision and acknowledgement journal. */
public final class ChopTradeCustody {
    private ChopTradeCustody() {}
    private static List<ItemStack> offered(ServerPlayer reader,CompoundTag custody) {
        var result=new ArrayList<ItemStack>();var budget=new int[]{256};
        for(var tag:(ListTag)custody.get("offers"))collect(RepairCustody.decode(reader,(CompoundTag)tag),result,0,budget);
        return result;
    }
    private static void collect(ItemStack stack,List<ItemStack> result,int depth,int[] budget) {
        if(stack.isEmpty())return;
        if(depth>8||--budget[0]<0)throw new IllegalArgumentException("Trade nesting needs review");
        if(ChopOwnershipService.isChop(stack))result.add(stack);
        var bundle=stack.get(DataComponents.BUNDLE_CONTENTS);
        if(bundle!=null)for(var child:bundle.items())collect(child,result,depth+1,budget);
        var container=stack.get(DataComponents.CONTAINER);
        if(container!=null)for(var child:container.nonEmptyItems())collect(child,result,depth+1,budget);
    }
    private static boolean hasOtherChop(ServerPlayer player) {
        var items=new ArrayList<ItemStack>();var budget=new int[]{256};
        for(int i=0;i<player.getInventory().getContainerSize();i++)collect(player.getInventory().getItem(i),items,0,budget);
        for(int i=0;i<player.getEnderChestInventory().getContainerSize();i++)collect(player.getEnderChestInventory().getItem(i),items,0,budget);
        collect(player.containerMenu.getCarried(),items,0,budget);
        return !items.isEmpty();
    }
    private static void outgoing(ServerPlayer owner,ServerPlayer recipient,List<ItemStack> items,ChopOwnershipData.Entry entry) {
        if(items.isEmpty())return;
        if(items.size()!=1)throw new IllegalArgumentException("Only one personal Chop may be traded");
        var stack=items.getFirst();var token=stack.get(ModDataComponents.CHOP_TOKEN.get());
        if(stack.getCount()!=1||entry==null||entry.deliver()||token==null
                ||!token.toString().equals(entry.token())||!owner.getUUID().equals(stack.get(ModDataComponents.CHOP_OWNER.get()))
                ||ChopTravelRecovery.blocked(owner)||ChopTravelRecovery.blocked(recipient))
            throw new IllegalArgumentException("Chop ownership or pending travel needs review before trading");
        var target=stack.get(ModDataComponents.DUNGEON_RETURN_TARGET.get());
        if(stack.is(net.goui.cosmicdungeon.item.ModItems.RAW_FARROWS_CHOP.get())) {
            if(target!=null||stack.has(ModDataComponents.COORDINATES.get()))
                throw new IllegalArgumentException("Raw Chop has an unresolved return entitlement");
            if(entry.runId()>0) {
                var run=DungeonRunRegistryData.get(owner.level().getServer()).getRun(entry.runId()).orElse(null);
                if(run==null||run.stateEnum()!=DungeonRunState.ACTIVE||!run.containsPlayer(recipient.getUUID())||!run.containsPlayer(owner.getUUID()))
                    throw new IllegalArgumentException("Run-bound Chop requires both players in its active run");
            }
        } else {
            var server=owner.level().getServer();
            var run=target==null?null:DungeonRunRegistryData.get(server).getRun(target.runId()).orElse(null);
            var escrow=target==null?null:DungeonInventoryEscrowData.get(server).get(target.runId(),recipient.getUUID()).orElse(null);
            if(target==null||!target.owner().equals(owner.getUUID())||target.runId()!=entry.runId()
                    ||run==null||run.stateEnum()!=DungeonRunState.ACTIVE||!run.containsPlayer(recipient.getUUID())
                    ||!run.containsPlayer(owner.getUUID())||escrow==null||!escrow.outsideActive())
                throw new IllegalArgumentException("A return Chop requires the recipient's own stored inventory in that active run");
        }
    }
    public static CompoundTag prepare(ServerPlayer first,ServerPlayer second) {
        var left=offered(first,TradeCustody.read(first));var right=offered(second,TradeCustody.read(second));
        if(left.isEmpty()&&right.isEmpty())return new CompoundTag();
        var data=ChopOwnershipData.get(first.level().getServer());
        var a=data.entry(first.getUUID());var b=data.entry(second.getUUID());
        outgoing(first,second,left,a);outgoing(second,first,right,b);
        if(!right.isEmpty()&&(a!=null&&left.isEmpty()||hasOtherChop(first))
                ||!left.isEmpty()&&(b!=null&&right.isEmpty()||hasOtherChop(second)))
            throw new IllegalArgumentException("The recipient already owns a personal Chop; offer it in the same trade");
        var result=new CompoundTag();result.putInt("schema",1);result.putBoolean("applied",false);
        result.put("first_before",data.image(first.getUUID()));result.put("second_before",data.image(second.getUUID()));
        result.put("first_after",ChopOwnershipData.entryImage(!right.isEmpty()?b:!left.isEmpty()?null:a));
        result.put("second_after",ChopOwnershipData.entryImage(!left.isEmpty()?a:!right.isEmpty()?null:b));
        validate(result);return result;
    }
    public static void validate(CompoundTag image) {
        if(image.isEmpty())return; // Legacy ordinary trades require no registry transition.
        if(image.getIntOr("schema",0)!=1||!(image.get("applied") instanceof ByteTag))throw new IllegalArgumentException("Unknown Chop trade schema");
        for(var key:List.of("first_before","second_before","first_after","second_after")) {
            if(!(image.get(key) instanceof CompoundTag value))throw new IllegalArgumentException("Missing Chop trade image");
            if(!value.isEmpty())new ChopOwnershipData.Entry(value.getStringOr("token",""),value.getLongOr("run_id",0),value.getBooleanOr("deliver",false));
        }
        var a=image.getCompoundOrEmpty("first_before");var b=image.getCompoundOrEmpty("second_before");
        if(a.isEmpty()&&b.isEmpty()||!a.isEmpty()&&!b.isEmpty()&&a.getStringOr("token","").equals(b.getStringOr("token","")))
            throw new IllegalArgumentException("Invalid Chop transfer identities");
        if(!image.getCompoundOrEmpty("first_after").equals(b)||!image.getCompoundOrEmpty("second_after").equals(a))
            throw new IllegalArgumentException("Chop custody must exchange the exact registry images");
    }
    static void apply(ServerPlayer reader,TradeCommitPlan plan) {
        var image=plan.chops();
        if(image.isEmpty()) {
            if(!offered(reader,plan.firstBefore()).isEmpty()||!offered(reader,plan.secondBefore()).isEmpty())
                throw new IllegalStateException("Legacy committed Chop trade lacks registry proof; complete-save review required");
            return;
        }
        if(image.getBooleanOr("applied",false))return;
        var registry=ChopOwnershipData.get(reader.level().getServer());
        if(!registry.transferVerified(plan.first(),plan.second(),image.getCompoundOrEmpty("first_before"),image.getCompoundOrEmpty("second_before"),
                image.getCompoundOrEmpty("first_after"),image.getCompoundOrEmpty("second_after")))
            throw new IllegalStateException("Chop registry no longer matches the committed trade");
        var accounts=net.goui.cosmicdungeon.economy.PlayerCurrencyData.get(reader.level().getServer());
        accounts.markTradeChopsApplied(plan.transaction());
        if(!accounts.flushVerified())throw new IllegalStateException("Chop registry acknowledgement save not verified");
    }
}
