package net.goui.cosmicdungeon.economy;
import com.google.gson.*;
import com.mojang.serialization.*;
import net.minecraft.nbt.NbtOps;
import net.goui.cosmicdungeon.playerclass.dragoon.repair.RepairQuoteRules;
import java.util.*;
public final class AccountTransferChecks {
    private static int checks;
    private static final UUID A=new UUID(0,1),B=new UUID(0,2),C=new UUID(0,3);
    private static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    private static void rejects(Runnable action,String why){boolean threw=false;try{action.run();}catch(RuntimeException expected){threw=true;}check(threw,why);}
    @SuppressWarnings("unchecked") private static Codec<PlayerCurrencyData> codec()throws Exception{
        var field=PlayerCurrencyData.class.getDeclaredField("CODEC");field.setAccessible(true);return (Codec<PlayerCurrencyData>)field.get(null);
    }
    private static PlayerCurrencyData fresh(long a,long b,long cap)throws Exception{
        var data=codec().parse(JsonOps.INSTANCE,new JsonObject()).getOrThrow();
        for(UUID id:List.of(A,B,C))data.setCapacityTrace(id,cap);
        data.setBalanceTrace(A,a);data.setBalanceTrace(B,b);return data;
    }
    private static PlayerCurrencyData reload(PlayerCurrencyData data)throws Exception{
        var saved=codec().encodeStart(NbtOps.INSTANCE,data).getOrThrow();return codec().parse(NbtOps.INSTANCE,saved).getOrThrow();
    }
    private static AccountTransfer.Terms terms(long a,long b){return new AccountTransfer.Terms(A,B,a,b,"dragoon_repair",12,"exact-quote-1");}
    public static void main(String[] args)throws Exception{
        var data=fresh(100,20,150);UUID id=new UUID(1,1);var terms=terms(70,10);
        var reserved=data.reserve(id,terms,1);
        check(reserved.reserved(),"Both outgoing amounts and full incoming amounts reserve");
        check(data.getBalanceTrace(A)==100&&data.getBalanceTrace(B)==20,"Ready never transfers funds");
        check(data.availableTrace(A)==30&&data.availableTrace(B)==10,"Reserved outgoing funds are unavailable");
        check(data.availableCapacity(A)==40&&data.availableCapacity(B)==60,"Incoming capacity held independently");
        check(data.reserve(id,terms,2).equals(reserved)&&data.reservedDebit(A)==70,"Repeated Ready receipt cannot double reserve");
        rejects(()->data.reserve(id,terms(71,10),3),"Same identifier cannot change fee");
        rejects(()->data.reserve(id,new AccountTransfer.Terms(A,B,70,10,"dragoon_repair",12,"other-item"),3),"Quote identity mismatch rejected");
        rejects(()->data.setBalanceTrace(A,69),"Administrator cannot erase held payment");
        rejects(()->data.setBalanceTrace(B,81),"Administrator cannot consume held receiving room");
        rejects(()->data.clear(A),"Clear cannot delete a reserved account");
        check(data.change(A,"A",-31,"test","",0,"spend-1",false)==-1,"Unrelated spending cannot spend holds");
        check(data.change(B,"B",61,"test","",0,"earn-1",false)==-1,"Unrelated deposits cannot consume holds");
        check(data.change(B,"B",70,"test","",0,"earn-2",true)==60,"Partial reward uses only unreserved room");
        check(data.reservationValid(id,terms),"Partial reward preserves transfer promise");
        var saved=reload(data);
        check(saved.reservedDebit(A)==70&&saved.reservedCredit(B)==70,"Held indexes reconstruct from NBT receipts");
        check(saved.availableTrace(A)==30&&saved.getBalanceTrace(B)==80,"Total and available survive independently");
        var committed=saved.commitTransfer(id,terms,5);
        check(committed.status().equals(AccountTransfer.COMMITTED),"Reserved payment commits");
        check(saved.getBalanceTrace(A)==40&&saved.getBalanceTrace(B)==140,"Both balances apply from current values");
        check(saved.reservedDebit(A)==0&&saved.reservedCredit(B)==0,"Commit releases holds");
        check(saved.getBalanceTrace(A)+saved.getBalanceTrace(B)==180,"Paired transfer neither mints nor destroys supply");
        saved.change(A,"A",3,"test","",0,"later-credit",false);
        check(saved.commitTransfer(id,terms,6).equals(committed)&&saved.getBalanceTrace(A)==43,"Replay cannot overwrite newer balance");
        check(saved.cancelTransfer(id,terms,7,"late cancel").equals(committed),"Cancel cannot refund committed payment");
        var restart=reload(saved);
        check(restart.commitTransfer(id,terms,8).equals(committed)&&restart.getBalanceTrace(A)==43,"Restart replay remains terminal");
        check(restart.reserve(id,terms,9).equals(committed)&&restart.reservedDebit(A)==0,"Terminal receipt never re-reserves");
        var cancel=fresh(100,0,100);UUID cancelledId=new UUID(1,2);
        cancel.reserve(cancelledId,terms(60,0),1);cancel.cancelTransfer(cancelledId,terms(60,0),2,"unready");
        check(cancel.getBalanceTrace(A)==100&&cancel.availableTrace(A)==100&&cancel.availableCapacity(B)==100,"Cancel releases both sides without payment");
        check(cancel.commitTransfer(cancelledId,terms(60,0),3).status().equals(AccountTransfer.CANCELLED),"Cancelled ID cannot commit");
        check(reload(cancel).transfer(cancelledId).orElseThrow().reason().equals("unready"),"Cancellation reason survives restart");
        var full=fresh(100,100,100);UUID fullId=new UUID(1,3);
        check(full.reserve(fullId,terms(20,20),1).status().equals(AccountTransfer.REJECTED),"Net-zero exchange cannot bypass full incoming-cap check");
        check(full.reservedCredit(A)==0&&full.reservedDebit(B)==0,"Rejected transfer reserves nothing");
        check(full.reserve(fullId,terms(20,20),2).status().equals(AccountTransfer.REJECTED),"Rejected identifier remains rejected");
        UUID free=new UUID(1,4);
        check(full.reserve(free,terms(0,0),1).reserved(),"Free repair works with full accounts");
        check(full.commitTransfer(free,terms(0,0),2).status().equals(AccountTransfer.COMMITTED),"Free repair still gets a replay-safe receipt");
        var changed=fresh(100,0,100);UUID capId=new UUID(1,5);
        changed.reserve(capId,terms(70,0),1);changed.setCapacityTrace(B,60);
        check(!changed.reservationValid(capId,terms(70,0)),"Lowered cap invalidates Ready promise");
        check(changed.commitTransfer(capId,terms(70,0),2).reserved()&&changed.getBalanceTrace(A)==100,"Invalid commit changes neither balance");
        changed.cancelTransfer(capId,terms(70,0),3,"capacity changed");
        check(changed.availableTrace(A)==100&&changed.reservedCredit(B)==0,"Invalidated quote can release cleanly");
        var old=fresh(1000,0,100);
        check(old.getBalanceTrace(A)==1000&&old.availableCapacity(A)==0,"Legacy/admin over-cap balance preserved");
        UUID oldId=new UUID(1,6);old.reserve(oldId,terms(70,0),1);
        check(old.reservationValid(oldId,terms(70,0)),"Over-cap account can still pay a debit-only fee");
        old.commitTransfer(oldId,terms(70,0),2);
        check(old.getBalanceTrace(A)==930&&old.getBalanceTrace(B)==70,"Debit cannot truncate an old balance");
        var many=fresh(100,0,100);var m1=new UUID(2,1);var m2=new UUID(2,2);
        many.reserve(m1,terms(60,0),1);
        check(many.reserve(m2,terms(50,0),1).status().equals(AccountTransfer.REJECTED),"Concurrent quotes cannot oversubscribe a customer");
        many.setBalanceTrace(C,80);
        var fromC=new AccountTransfer.Terms(C,B,50,0,"trade",12,"other");
        check(many.reserve(new UUID(2,3),fromC,1).status().equals(AccountTransfer.REJECTED),"Different customer cannot oversubscribe recipient");
        var fromCsmall=new AccountTransfer.Terms(C,B,40,0,"trade",12,"small");
        var tradeId=new UUID(2,4);check(many.reserve(tradeId,fromCsmall,1).reserved(),"Exact remaining capacity can reserve");
        check(many.cancelReservations("dragoon_repair",4,"restart")==1,"Restart cancels only unfinished repair reservations");
        check(many.transfer(tradeId).orElseThrow().reserved()&&many.reservedCredit(B)==40,"Other workflow reservations survive targeted cleanup");
        check(many.cancelReservations("dragoon_repair",5,"restart")==0,"Restart release is idempotent");
        var max=fresh(Long.MAX_VALUE,0,Long.MAX_VALUE);UUID maxId=new UUID(3,1);
        check(max.reserve(maxId,terms(Long.MAX_VALUE,0),1).reserved(),"Signed-64 maximum payment fits");
        max=reload(max);max.commitTransfer(maxId,terms(Long.MAX_VALUE,0),2);
        check(max.getBalanceTrace(A)==0&&max.getBalanceTrace(B)==Long.MAX_VALUE,"Maximum payment cannot overflow");
        check(reload(max).transfer(maxId).orElseThrow().secondAfter()==Long.MAX_VALUE,"Maximum receipt round-trips exactly");
        rejects(()->terms(-1,0),"Negative payment rejected");
        rejects(()->new AccountTransfer.Terms(A,A,1,0,"repair",0,"x"),"Self-payment cannot fabricate capacity");
        rejects(()->new AccountTransfer(terms(5,0),AccountTransfer.COMMITTED,1,10,0,6,5,""),"Malformed nonconserving receipt rejected");
        rejects(()->new AccountTransfer(terms(5,5),AccountTransfer.COMMITTED,1,0,0,0,0,""),"Netting cannot hide unfunded receipt");
        rejects(()->new AccountTransfer(terms(5,0),AccountTransfer.RESERVED,1,10,0,5,5,""),"Reserved receipt cannot move funds");
        var oldJson=JsonParser.parseString("{\"balances\":{\""+A+"\":1000},\"capacity_overrides\":{\""+A+"\":100}}");
        var migrated=codec().parse(JsonOps.INSTANCE,oldJson).getOrThrow();
        check(migrated.getBalanceTrace(A)==1000&&migrated.reservedDebit(A)==0,"Pre-reservation save loads without migration loss");
        check(reload(migrated).getBalanceTrace(A)==1000,"Legacy shape safely resaves");
        check(!migrated.flushVerified(),"Unbound data never claims verified persistence");
        for(int delta:new int[]{Integer.MIN_VALUE,-100,-1,0,1,100,Integer.MAX_VALUE}){
            long fee=RepairQuoteRules.adjustFee(Long.MAX_VALUE-1,Long.MAX_VALUE,delta,Long.MAX_VALUE);
            check(fee>=0&&fee<=Long.MAX_VALUE,"Hostile delta stays within whole-Trace bounds");
            check(RepairQuoteRules.adjustFee(50,10,delta,0)==0,"No available balance clamps every delta");
        }
        check(RepairQuoteRules.adjustFee(100,100,1,1000)==200,"Ordinary fee increment");
        check(RepairQuoteRules.adjustFee(100,100,-1,1000)==0,"Ordinary fee decrement");
        check(!RepairQuoteRules.expired(599,600)&&RepairQuoteRules.expired(600,600)&&RepairQuoteRules.expired(601,600),"Start checks exact deadline, independent of tick polling");
        for(long fee:new long[]{0,1,20,100,100000000}){
            var scenario=fresh(100000000,0,100000000);UUID payment=new UUID(4,fee);
            var quote=terms(fee,0);scenario.reserve(payment,quote,1);scenario=reload(scenario);
            check(scenario.reservationValid(payment,quote),"Every configured whole-Trace fee survives Ready restart");
            scenario.commitTransfer(payment,quote,2);scenario=reload(scenario);
            check(scenario.getBalanceTrace(A)==100000000-fee&&scenario.getBalanceTrace(B)==fee,"Fee commits once across save boundaries");
            scenario.commitTransfer(payment,quote,3);
            check(scenario.getBalanceTrace(A)+scenario.getBalanceTrace(B)==100000000,"Repeated fee preserves supply");
        }
        System.out.println(checks+" account reservation and repair quote checks passed");
    }
}
