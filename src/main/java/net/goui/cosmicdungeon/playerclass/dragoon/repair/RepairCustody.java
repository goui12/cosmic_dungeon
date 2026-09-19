package net.goui.cosmicdungeon.playerclass.dragoon.repair;
import net.goui.cosmicdungeon.item.identity.ProtectedItemRecovery;
import net.goui.cosmicdungeon.playerclass.api.ClassData;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.item.*;
import net.minecraft.world.level.storage.*;
import java.util.*;
/** Player-local custody replaces the separate live repair-item mirror. No world drops. */
public final class RepairCustody {
    public static final String KEY="repair_custody_v1";
    private RepairCustody(){}
    public static CompoundTag read(ServerPlayer player){
        return player.getPersistentData().getCompoundOrEmpty(ClassData.ROOT_TAG).getCompoundOrEmpty(KEY).copy();
    }
    public static void write(ServerPlayer player,CompoundTag state){
        if(!state.isEmpty())RepairCustodyImages.validate(state,player.getUUID());
        var root=player.getPersistentData().getCompoundOrEmpty(ClassData.ROOT_TAG).copy();
        if(state.isEmpty())root.remove(KEY);else root.put(KEY,state.copy());
        player.getPersistentData().put(ClassData.ROOT_TAG,root);
    }
    public static boolean pending(ServerPlayer player){
        return player.getPersistentData().getCompoundOrEmpty(ClassData.ROOT_TAG).contains(KEY);
    }
    public static void begin(ServerPlayer player,UUID session,boolean customer){
        if(pending(player))throw new IllegalStateException("Prior repair custody needs recovery");
        write(player,RepairCustodyImages.open(session,player.getUUID(),ProtectedItemRecovery.scope(player),customer));
    }
    public static CompoundTag encode(ServerPlayer player,ItemStack stack){
        if(stack.isEmpty())return new CompoundTag();
        var errors=new ProblemReporter.Collector();var output=TagValueOutput.createWithContext(errors,player.registryAccess());
        output.store("item",ItemStack.CODEC,stack);
        if(!errors.isEmpty())throw new IllegalStateException(errors.getReport());return output.buildResult().getCompoundOrEmpty("item");
    }
    public static ItemStack decode(ServerPlayer player,CompoundTag image){
        if(image.isEmpty())return ItemStack.EMPTY;
        var errors=new ProblemReporter.Collector();var wrapper=new CompoundTag();wrapper.put("item",image.copy());
        var item=TagValueInput.create(errors,player.registryAccess(),wrapper).read("item",ItemStack.CODEC).orElse(ItemStack.EMPTY);
        if(!errors.isEmpty()||item.isEmpty())throw new IllegalArgumentException("Unusable repair item image");return item;
    }
    /** Called at native Entity.saveWithoutId HEAD, before NeoForgeData AND Inventory are written. */
    public static void capture(ServerPlayer player){
        var session=DragoonRepairSessionData.get(player);
        if(session==null||!session.isValidFor(player)||!(player.containerMenu instanceof DragoonRepairMenu menu)||!menu.belongsTo(session))return;
        var state=read(player);RepairCustodyImages.validate(state,player.getUUID());
        if(!state.getStringOr("session","").equals(session.id().toString()))throw new IllegalStateException("Wrong live repair custody session");
        var target=state.getBooleanOr("customer",false)?session.repairContainer().getItem(0):ItemStack.EMPTY;
        var next=RepairCustodyImages.live(state,encode(player,target),encode(player,menu.getCarried()));
        if(!next.equals(state))write(player,next);
    }
    public static void bindCustomer(ServerPlayer player,UUID transaction){
        capture(player);write(player,RepairCustodyImages.reserve(read(player),transaction,new ListTag()));
    }
    public static boolean reserveComponents(ServerPlayer player,UUID transaction,Item material,int required){
        if(required<1)return false;capture(player);var state=read(player);
        if(state.getBooleanOr("customer",false))return false;
        var parts=new ListTag();int left=required;
        for(int slot=0;slot<player.getInventory().getContainerSize()&&left>0;slot++){
            var stack=player.getInventory().getItem(slot);if(!RepairComponents.validFor(stack,material))continue;
            int take=Math.min(left,stack.getCount());var part=new CompoundTag();part.putInt("slot",slot);
            part.put("item",encode(player,stack.copyWithCount(take)));parts.add(part);left-=take;
        }
        if(left!=0)return false;
        var next=RepairCustodyImages.reserve(state,transaction,parts); // Encode everything before detaching.
        for(var entry:parts){var part=(CompoundTag)entry;player.getInventory().getItem(part.getIntOr("slot",-1))
                .shrink(part.getCompoundOrEmpty("item").getIntOr("count",1));}
        write(player,next);player.getInventory().setChanged();return true;
    }
    public static boolean componentsValid(ServerPlayer player,UUID transaction,Item material,int required){
        try{
            var state=read(player);RepairCustodyImages.validate(state,player.getUUID());
            if(!RepairCustodyImages.sameReservation(state,transaction)||state.getBooleanOr("customer",true))return false;
            int total=0;for(var image:RepairCustodyImages.held(state,true)){
                var stack=decode(player,image);if(!RepairComponents.validFor(stack,material))return false;
                total=Math.addExact(total,stack.getCount());
            }
            return total==required;
        }catch(RuntimeException malformed){return false;}
    }
    public static void consumeComponents(ServerPlayer player,UUID transaction){
        var state=read(player);
        if(!RepairCustodyImages.sameReservation(state,transaction))throw new IllegalStateException("Wrong component transaction");
        state.put("components",new ListTag());write(player,state);
    }
    /** In-memory rollback is permitted only before a save is started; never restore a disk-uncertain image. */
    public static boolean release(ServerPlayer player,boolean componentsOnly,boolean deliverNow){
        if(!pending(player))return true;
        var state=read(player);var originalRoot=player.getPersistentData().getCompoundOrEmpty(ClassData.ROOT_TAG).copy();
        var inventory=player.getInventory();var originals=new ArrayList<ItemStack>();
        for(int slot=0;slot<inventory.getContainerSize();slot++)originals.add(inventory.getItem(slot).copy());
        try{
            RepairCustodyImages.validate(state,player.getUUID());
            var held=new ArrayList<ItemStack>();
            for(var image:RepairCustodyImages.held(state,componentsOnly)){
                var stack=decode(player,image);ProtectedItemRecovery.validateSerializable(player,stack);held.add(stack);
            }
            long scope=state.getLongOr("run",-1);
            write(player,componentsOnly?RepairCustodyImages.unreserve(state):new CompoundTag());
            for(var stack:held){
                if(deliverNow&&player.isAlive()&&scope==ProtectedItemRecovery.scope(player))ProtectedItemRecovery.returnDetached(player,stack);
                else ProtectedItemRecovery.queueDetached(player,stack,scope);
            }
            inventory.setChanged();return true;
        }catch(RuntimeException failure){
            for(int slot=0;slot<originals.size();slot++)inventory.setItem(slot,originals.get(slot));
            player.getPersistentData().put(ClassData.ROOT_TAG,originalRoot);
            com.mojang.logging.LogUtils.getLogger().error("Repair custody retained for {}",player.getUUID(),failure);
            player.sendSystemMessage(Component.literal("Your repair items need recovery review; their saved record is preserved."));return false;
        }
    }
    public static int claim(ServerPlayer player){
        if(DragoonRepairSessionData.get(player)!=null)return 0;
        if(player.containerMenu!=player.inventoryMenu||!player.inventoryMenu.getCarried().isEmpty()){
            player.sendSystemMessage(Component.literal("Close the open interface before recovering repair items."));return 0;
        }
        if(!RepairTransactions.reconcile(player))return 0;
        RepairRecoveryData.get(player.level().getServer()).notifyLegacy(player);
        return player.isAlive()?ProtectedItemRecovery.claim(player):0;
    }
    // TODO(M25, cumulative licensed TEST): interrupt each account/player save boundary on dedicated
    // and integrated servers; verify death/clone, full inventory and run cleanup. Arbitrary manual
    // restores/corruption cannot establish current ownership: preserve evidence for developer review.
}
