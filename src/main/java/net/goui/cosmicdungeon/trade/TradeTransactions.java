package net.goui.cosmicdungeon.trade;
import net.goui.cosmicdungeon.economy.*;
import net.goui.cosmicdungeon.playerclass.api.ClassData;
import net.goui.cosmicdungeon.playerclass.dragoon.repair.RepairTransactions;
import net.goui.cosmicdungeon.transaction.PlayerSaveProof;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerPlayer;
import java.util.*;
/** Existing shared-account reservations, immutable item decision, verified owner receipts. */
public final class TradeTransactions {
    public static final String RECEIPT="trade_receipt_v1";
    private TradeTransactions(){}
    private static PlayerCurrencyData data(ServerPlayer p){return PlayerCurrencyData.get(p.level().getServer());}
    public static boolean blocked(ServerPlayer p){return TradeCustody.held(p)||TradeCustody.pending(p)||data(p).pendingTrade(p.getUUID()).isPresent();}
    public static boolean decide(ServerPlayer a,ServerPlayer b,UUID id,long aPays,long bPays){
        var data=data(a);
        try{
            TradeCustody.capture(a);TradeCustody.capture(b);
            CompoundTag chops;
            try{chops=ChopTradeCustody.prepare(a,b);}
            catch(IllegalArgumentException invalid){
                var message=net.minecraft.network.chat.Component.literal("Trade cancelled: "+invalid.getMessage());
                a.sendSystemMessage(message);b.sendSystemMessage(message);return false;
            }
            var terms=new AccountTransfer.Terms(a.getUUID(),b.getUUID(),aPays,bPays,"player_trade",TradeCustody.read(a).getLongOr("run",0),id.toString());
            var receipt=data.reserve(id,terms,System.currentTimeMillis());
            if(!data.flushVerified())throw new IllegalStateException("account reservation save not verified");
            if(!receipt.reserved()){data.reportTransfer(id,receipt);return false;}
            TradeCustody.write(a,TradeCustodyImages.bind(TradeCustody.read(a),id));
            TradeCustody.write(b,TradeCustodyImages.bind(TradeCustody.read(b),id));
            if(!PlayerSaveProof.save(a)||!PlayerSaveProof.save(b))throw new IllegalStateException("owner reservation save not verified");
            var plan=new TradeCommitPlan(a.getUUID(),b.getUUID(),TradeCustody.read(a),TradeCustody.read(b),false,false,chops);
            data.prepareTrade(id,plan);if(!data.flushVerified())throw new IllegalStateException("item decision preparation not verified");
            receipt=data.commitTrade(id,System.currentTimeMillis());
            if(!data.flushVerified())throw new IllegalStateException("commit decision save not verified");
            data.reportTransfer(id,receipt);return receipt.status().equals(AccountTransfer.COMMITTED);
        }catch(RuntimeException failure){TradeCustody.hold(a,failure.getMessage());TradeCustody.hold(b,failure.getMessage());return false;}
    }
    private static CompoundTag receipt(ServerPlayer p){
        var owner=PlayerSaveProof.ownerCompound(p.getPersistentData(),ClassData.ROOT_TAG);
        var receipt=PlayerSaveProof.ownerReceipt(owner,RECEIPT,p.getUUID(),1,"transaction");
        if(!receipt.isEmpty()&&!Set.of(AccountTransfer.COMMITTED,AccountTransfer.CANCELLED).contains(receipt.getStringOr("outcome","")))
            throw new IllegalStateException("Unsupported owner receipt field "+RECEIPT+"; original save requires review.");
        return receipt;
    }
    public static boolean reconcile(ServerPlayer p){
        if(TradeCustody.held(p)||TradeSessionData.get(p)!=null)return false;
        try{
            var savedReceipt=receipt(p); // Fail closed before cancelling/committing any world outcome.
            var owner=PlayerSaveProof.ownerCompound(p.getPersistentData(),ClassData.ROOT_TAG);
            var custody=PlayerSaveProof.ownerCompound(owner,TradeCustody.KEY);
            if(owner.contains(TradeCustody.KEY))TradeCustodyImages.validate(custody,p.getUUID());
            var data=data(p);UUID id=data.pendingTrade(p.getUUID()).orElse(null);
            if(id!=null){
                var plan=data.tradePlan(id).orElseThrow();var transfer=data.transfer(id).orElseThrow();
                String outcome=transfer.reserved()?AccountTransfer.CANCELLED:transfer.status();
                if(!Set.of(AccountTransfer.COMMITTED,AccountTransfer.CANCELLED).contains(outcome))throw new IllegalStateException("Invalid trade outcome");
                boolean applied=RepairTransactions.receiptMatches(savedReceipt,p.getUUID(),id,outcome);
                CompoundTag projected=null;
                if(applied){
                    if(owner.contains(TradeCustody.KEY))throw new IllegalStateException("Applied trade still has custody");
                }else{
                    if(!owner.contains(TradeCustody.KEY))throw new IllegalStateException("Verified owner custody is missing; restore review required");
                    // Pure projection verifies the exact reservation before changing any account outcome.
                    projected=plan.project(p.getUUID(),custody,outcome.equals(AccountTransfer.COMMITTED));
                }
                if(transfer.reserved())data.cancelTransfer(id,transfer.terms(),System.currentTimeMillis(),"trade interface ended before commit");
                if(!data.flushVerified())throw new IllegalStateException("trade outcome save not verified");
                if(!applied){
                    // Only a durable COMMITTED decision may change offered item owners.
                    // The saved before-images and this owner's cursor remain untouched.
                    if(outcome.equals(AccountTransfer.COMMITTED)) {
                        ChopTradeCustody.apply(p,plan);
                        projected=TradeItemTransfer.projectOffers(p,projected);
                    }
                    var root=p.getPersistentData().getCompoundOrEmpty(ClassData.ROOT_TAG).copy();
                    TradeCustody.write(p,projected);
                    if(!TradeCustody.release(p)){p.getPersistentData().put(ClassData.ROOT_TAG,root);return false;}
                    var next=p.getPersistentData().getCompoundOrEmpty(ClassData.ROOT_TAG).copy();
                    next.put(RECEIPT,RepairTransactions.receiptImage(p.getUUID(),id,outcome));p.getPersistentData().put(ClassData.ROOT_TAG,next);
                }
                if(outcome.equals(AccountTransfer.COMMITTED))net.goui.cosmicdungeon.achievement.TradeAchievementService.recordCompleted(p);
                if(!PlayerSaveProof.save(p))throw new IllegalStateException("trade receipt/player save not verified");
                data.acknowledgeTrade(id,p.getUUID());if(!data.flushVerified())throw new IllegalStateException("trade acknowledgement not verified");
                if(outcome.equals(AccountTransfer.COMMITTED))net.goui.cosmicdungeon.achievement.TradeAchievementService.onSuccessfulTrade(p,null);
            }else if(TradeCustody.pending(p)){
                var state=TradeCustody.read(p);TradeCustodyImages.validate(state,p.getUUID());String tx=state.getStringOr("transaction","");
                if(!tx.isEmpty()){
                    UUID pending=UUID.fromString(tx);var transfer=data.transfer(pending).orElseThrow();
                    if(!transfer.terms().type().equals("player_trade")||(!transfer.terms().first().equals(p.getUUID())&&!transfer.terms().second().equals(p.getUUID())))throw new IllegalStateException("Foreign trade reservation");
                    if(transfer.status().equals(AccountTransfer.COMMITTED))throw new IllegalStateException("Committed trade lacks owner decision");
                    if(transfer.reserved())data.cancelTransfer(pending,transfer.terms(),System.currentTimeMillis(),"incomplete trade preparation");
                    if(!data.flushVerified())throw new IllegalStateException("trade cancellation save not verified");
                }
                if(!TradeCustody.release(p)||!PlayerSaveProof.save(p))throw new IllegalStateException("cancelled owner trade save not verified");
            }
            return true;
        }catch(RuntimeException failure){TradeCustody.hold(p,failure.getMessage());return false;}
    }
    // TODO(M115, licensed TEST): forced process exits at every actual native save boundary,
    // dedicated/integrated owner files, logout/death/full inventories and pending run reset.
    // Manual partial backup restores/corrupt NBT cannot prove ownership and stay held for review.
}
