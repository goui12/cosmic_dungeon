package net.goui.cosmicdungeon.trade;
import net.goui.cosmicdungeon.item.identity.ProtectedItemRecovery;
import net.goui.cosmicdungeon.playerclass.api.ClassData;
import net.goui.cosmicdungeon.playerclass.dragoon.repair.RepairCustody;
import net.goui.cosmicdungeon.transaction.PlayerSaveProof;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import java.util.*;
/** Offers and each owner's cursor are serialized in the same file as their remaining inventory. */
public final class TradeCustody {
    public static final String KEY="trade_custody_v1";
    private static final Set<ServerPlayer> HOLDS=Collections.newSetFromMap(new WeakHashMap<>());
    private TradeCustody(){}
    public static CompoundTag read(ServerPlayer p){return p.getPersistentData().getCompoundOrEmpty(ClassData.ROOT_TAG).getCompoundOrEmpty(KEY).copy();}
    public static boolean pending(ServerPlayer p){return p.getPersistentData().getCompoundOrEmpty(ClassData.ROOT_TAG).contains(KEY);}
    public static boolean held(ServerPlayer p){return HOLDS.contains(p);}
    public static void write(ServerPlayer p,CompoundTag state){
        if(!state.isEmpty())TradeCustodyImages.validate(state,p.getUUID());
        var root=p.getPersistentData().getCompoundOrEmpty(ClassData.ROOT_TAG).copy();
        if(state.isEmpty())root.remove(KEY);else root.put(KEY,state.copy());p.getPersistentData().put(ClassData.ROOT_TAG,root);
    }
    public static void begin(ServerPlayer p,UUID session,UUID peer){
        if(pending(p)||held(p))throw new IllegalStateException("Prior trade custody needs recovery");
        write(p,TradeCustodyImages.open(session,p.getUUID(),peer,ProtectedItemRecovery.scope(p)));
    }
    public static void capture(ServerPlayer p){
        var session=TradeSessionData.get(p);
        if(session==null||!(p.containerMenu instanceof TradeMenu menu)||!menu.belongsTo(session))return;
        var state=read(p);TradeCustodyImages.validate(state,p.getUUID());
        if(!state.getStringOr("session","").equals(session.id().toString()))throw new IllegalStateException("Wrong trade session custody");
        var offers=new ListTag();var container=session.getContainerFor(p,true);
        for(int i=0;i<9;i++)offers.add(RepairCustody.encode(p,container.getItem(i)));
        var next=TradeCustodyImages.live(state,offers,RepairCustody.encode(p,menu.getCarried()));
        if(!next.equals(state))write(p,next);
    }
    public static void hold(ServerPlayer p,String reason){
        HOLDS.add(p);com.mojang.logging.LogUtils.getLogger().error("Trade recovery held for {}: {}",p.getUUID(),reason);
        p.connection.disconnect(Component.literal("Your trade needs a save check. Item and payment records are preserved; reconnect to recover."));
    }
    /** Pure owner-root transition before any save; no inventory/world item writes. */
    public static boolean release(ServerPlayer p){
        if(!pending(p))return true;var before=p.getPersistentData().getCompoundOrEmpty(ClassData.ROOT_TAG).copy();
        try{
            var state=read(p);TradeCustodyImages.validate(state,p.getUUID());var items=new ArrayList<ItemStack>();
            for(var image:TradeCustodyImages.held(state)){var item=RepairCustody.decode(p,image);ProtectedItemRecovery.validateSerializable(p,item);items.add(item);}
            write(p,new CompoundTag());for(var item:items)ProtectedItemRecovery.queueDetached(p,item,state.getLongOr("run",-1));return true;
        }catch(RuntimeException error){p.getPersistentData().put(ClassData.ROOT_TAG,before);hold(p,error.getMessage());return false;}
    }
    public static boolean reconcile(ServerPlayer p){return TradeTransactions.reconcile(p);}
    public static boolean beforeInventoryChange(ServerPlayer p){
        if(held(p))return false;p.closeContainer();return reconcile(p)&&!TradeTransactions.blocked(p);
    }
    public static int claim(ServerPlayer p){
        if(p.containerMenu!=p.inventoryMenu||!p.inventoryMenu.getCarried().isEmpty()||!reconcile(p))return 0;
        TradeRecoveryData.get(p.level().getServer()).notifyLegacy(p);
        return p.isAlive()?ProtectedItemRecovery.claim(p):0;
    }
}
