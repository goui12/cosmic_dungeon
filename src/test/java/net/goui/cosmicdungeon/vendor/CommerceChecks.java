package net.goui.cosmicdungeon.vendor;

import com.mojang.serialization.Codec;
import net.goui.cosmicdungeon.economy.*;
import net.goui.cosmicdungeon.playerclass.dragoon.repair.RepairTransactions;
import net.minecraft.nbt.*;
import java.nio.file.*;
import java.util.*;

public final class CommerceChecks {
    private static int checks;
    private static final UUID OWNER=new UUID(0,51), OTHER=new UUID(0,52), TX=new UUID(0,53);
    private static void check(boolean value,String why){checks++;if(!value)throw new AssertionError(why);}
    private static void reject(Runnable action,String why){boolean failed=false;try{action.run();}catch(RuntimeException expected){failed=true;}check(failed,why);}
    @SuppressWarnings("unchecked") private static Codec<PlayerCurrencyData> codec()throws Exception{
        var field=PlayerCurrencyData.class.getDeclaredField("CODEC");field.setAccessible(true);return (Codec<PlayerCurrencyData>)field.get(null);
    }
    private static CompoundTag encode(Codec<PlayerCurrencyData> c,PlayerCurrencyData d){return (CompoundTag)c.encodeStart(NbtOps.INSTANCE,d).getOrThrow();}
    private static CompoundTag item(int damage){
        var item=new CompoundTag();item.putString("id","minecraft:iron_sword");item.putInt("count",1);
        var components=new CompoundTag();components.putInt("minecraft:damage",damage);
        var custom=new CompoundTag();custom.putByteArray("identity",new byte[]{-1,0,127});custom.putLong("serial",Long.MAX_VALUE);
        components.put("minecraft:custom_data",custom);item.put("components",components);return item;
    }
    private static ListTag lots(CompoundTag item){var lots=new ListTag();lots.add(CommerceCustodyImages.lot(8,item));return lots;}
    private static CompoundTag player(ListTag items,CompoundTag custody){var p=new CompoundTag();p.put("inventory",items.copy());if(!custody.isEmpty())p.put("custody",custody.copy());p.put("stock",new CompoundTag());return p;}
    private static CompoundTag apply(AccountOperation op,UUID id,CompoundTag source){
        var p=source.copy();String outcome=op.status().equals(AccountTransfer.COMMITTED)?"committed":"cancelled";
        var returns=CommerceCustodyImages.recoverable(op,id,OWNER,p.getCompoundOrEmpty("custody"),p.getCompoundOrEmpty("receipt"));
        if(RepairTransactions.receiptMatches(p.getCompoundOrEmpty("receipt"),OWNER,id,outcome))return p;
        if(op.status().equals(AccountTransfer.COMMITTED))p.put("stock",VendorStock.project(p.getCompoundOrEmpty("stock"),op.plan().getCompoundOrEmpty("details")));
        for(var lot:returns)((ListTag)p.get("inventory")).add(lot.copy());
        p.remove("custody");p.put("receipt",RepairTransactions.receiptImage(OWNER,id,outcome));return p;
    }
    private record Cut(String label,CompoundTag account,CompoundTag player){}
    private static Cut cut(String label,Codec<PlayerCurrencyData> c,PlayerCurrencyData d,CompoundTag p){return new Cut(label,encode(c,d),p.copy());}
    private static PlayerCurrencyData fresh(Codec<PlayerCurrencyData> c){var d=c.parse(NbtOps.INSTANCE,new CompoundTag()).getOrThrow();d.setBalanceTrace(OWNER,100);d.setCapacityTrace(OWNER,1000);return d;}
    private static void interrupted(Codec<PlayerCurrencyData> c,String kind,long delta,ListTag inputs,ListTag outputs)throws Exception{
        var details=new CompoundTag();if(kind.equals("vendor_retail")){
            details.putString("stock_key","vendor|offer");details.put("stock_before",new CompoundTag());details.put("stock_after",VendorStock.next(new CompoundTag(),2,0));
        }
        var plan=CommerceCustodyImages.create(TX,OWNER,9,inputs,outputs,details);
        var d=fresh(c);var original=player(inputs,new CompoundTag());var detached=player(new ListTag(),plan);
        d.reserveOperation(TX,OWNER,delta,kind,"vendor",9,plan,1);
        var cuts=new ArrayList<Cut>();cuts.add(cut("reservation saved",c,d,original));cuts.add(cut("input save not complete",c,d,original));
        cuts.add(cut("input save complete",c,d,detached));cuts.add(cut("prepared save not complete",c,d,detached));
        d.prepareOperation(TX);cuts.add(cut("prepared saved",c,d,detached));cuts.add(cut("decision save not complete",c,d,detached));
        var op=d.decideOperation(TX,true,2);cuts.add(cut("decision saved",c,d,detached));cuts.add(cut("delivery save not complete",c,d,detached));
        var delivered=apply(op,TX,detached);cuts.add(cut("delivery saved",c,d,delivered));cuts.add(cut("ack save not complete",c,d,delivered));
        d.acknowledgeOperation(TX,OWNER);cuts.add(cut("ack saved",c,d,delivered));
        Path dir=Files.createTempDirectory("commerce-interruptions-");
        try{
            for(var cut:cuts){
                Path money=dir.resolve("account.dat"), owner=dir.resolve("player.dat");
                NbtIo.writeCompressed(cut.account(),money);NbtIo.writeCompressed(cut.player(),owner);
                var loaded=c.parse(NbtOps.INSTANCE,NbtIo.readCompressed(money,NbtAccounter.unlimitedHeap())).getOrThrow();
                var p=NbtIo.readCompressed(owner,NbtAccounter.unlimitedHeap());var receipt=loaded.operation(TX).orElseThrow();
                if(receipt.reserved())receipt=loaded.decideOperation(TX,false,3);
                boolean committed=receipt.status().equals(AccountTransfer.COMMITTED);
                if(loaded.pendingOperation(OWNER).isPresent()){p=apply(receipt,TX,p);loaded.acknowledgeOperation(TX,OWNER);}
                String label=kind+" / "+cut.label();
                check(loaded.getBalanceTrace(OWNER)==(committed?100+delta:100),label+" preserves payment outcome");
                check(p.get("inventory").equals(committed?outputs:inputs),label+" exact one-time input/output components");
                check(!p.contains("custody"),label+" released custody");
                check(loaded.pendingOperation(OWNER).isEmpty(),label+" owner index settled");
                check(apply(receipt,TX,p).equals(p),label+" duplicate recovery does not redeliver");
                check(loaded.supplySnapshot().getStringOr("difference","").equals("0"),label+" supply reconciles");
                var restart=c.parse(NbtOps.INSTANCE,encode(c,loaded)).getOrThrow();
                check(restart.pendingOperation(OWNER).isEmpty(),label+" acknowledgement persisted");
                long before=restart.getBalanceTrace(OWNER);restart.decideOperation(TX,true,4);
                check(restart.getBalanceTrace(OWNER)==before,label+" terminal decision replay");
                if(kind.equals("vendor_retail"))check(p.getCompoundOrEmpty("stock").getCompoundOrEmpty("vendor|offer").getIntOr("count",0)==(committed?1:0),label+" stock shares delivery outcome");
            }
        }finally{Files.deleteIfExists(dir.resolve("account.dat"));Files.deleteIfExists(dir.resolve("player.dat"));Files.deleteIfExists(dir);}
    }
    public static void main(String[] args)throws Exception{
        var c=codec();var input=lots(item(200));var output=lots(item(0));
        interrupted(c,"vendor_sale",7,input,new ListTag());interrupted(c,"vendor_retail",-10,new ListTag(),output);
        interrupted(c,"direct_repair",-12,input,output);interrupted(c,"vendor_sale",0,input,new ListTag());
        var plan=CommerceCustodyImages.create(TX,OWNER,9,input,output,new CompoundTag());
        var d=fresh(c);d.reserveOperation(TX,OWNER,-50,"direct_repair","vendor",9,plan,1);
        check(d.availableTrace(OWNER)==50,"Debit held before input removal");
        reject(()->d.decideOperation(TX,true,2),"Cannot debit without verified owner preparation");
        reject(()->d.acknowledgeOperation(TX,OWNER),"Cannot acknowledge undecided payment");
        reject(()->d.reserveOperation(TX,OWNER,-51,"direct_repair","vendor",9,plan,1),"Cannot change reserved terms");
        reject(()->d.reserveOperation(new UUID(0,54),OWNER,-1,"vendor_retail","vendor",9,CommerceCustodyImages.create(new UUID(0,54),OWNER,9,new ListTag(),output,new CompoundTag()),1),"One pending owner transaction");
        d.prepareOperation(TX);var op=d.decideOperation(TX,true,2);
        reject(()->CommerceCustodyImages.recoverable(op,TX,OTHER,plan,new CompoundTag()),"Foreign owner cannot recover");
        reject(()->CommerceCustodyImages.recoverable(op,TX,OWNER,new CompoundTag(),new CompoundTag()),"Missing committed player evidence is held");
        var altered=plan.copy();((CompoundTag)((CompoundTag)((ListTag)altered.get("inputs")).get(0)).get("item")).putInt("count",2);
        reject(()->CommerceCustodyImages.recoverable(op,TX,OWNER,altered,new CompoundTag()),"Changed exact stack is held");
        var receipt=RepairTransactions.receiptImage(OWNER,TX,"committed");
        reject(()->CommerceCustodyImages.recoverable(op,TX,OWNER,plan,receipt),"Receipt plus custody cannot duplicate items");
        reject(()->d.acknowledgeOperation(TX,OTHER),"Foreign owner cannot acknowledge");
        var copied=op.plan();copied.remove("inputs");check(op.plan().contains("inputs"),"Plan accessor is defensive");
        var duplicate=input.copy();duplicate.add(input.get(0).copy());
        reject(()->CommerceCustodyImages.create(TX,OWNER,9,duplicate,output,new CompoundTag()),"Duplicate input slot rejected");
        var badSlot=input.copy();((CompoundTag)badSlot.get(0)).putInt("slot",41);
        reject(()->CommerceCustodyImages.create(TX,OWNER,9,badSlot,output,new CompoundTag()),"Out-of-range inventory slot rejected");
        reject(()->CommerceCustodyImages.create(TX,OWNER,-1,input,output,new CompoundTag()),"Negative instance rejected");
        var credit=fresh(c);credit.setCapacityTrace(OWNER,105);var denied=credit.reserveOperation(TX,OWNER,6,"vendor_sale","vendor",9,plan,1);
        check(denied.status().equals(AccountTransfer.REJECTED)&&credit.getBalanceTrace(OWNER)==100,"Over-cap sale rejects whole payout");
        credit.acknowledgeOperation(TX,OWNER);check(credit.availableCapacity(OWNER)==5,"Rejected sale releases index without capacity hold");
        var zero=fresh(c);zero.setCapacityTrace(OWNER,100);
        check(zero.reserveOperation(TX,OWNER,0,"vendor_sale","vendor",9,plan,1).reserved(),"Confirmed zero-price surrender works at cap");
        var stock=VendorStock.next(new CompoundTag(),10,3);
        check(stock.getIntOr("count",0)==4,"Legacy daily purchases seed first local counter");
        check(VendorStock.count(stock,10,3)==4,"Local counter replaces legacy after migration");
        check(VendorStock.count(stock,9,0)==4,"Clock rollback cannot replenish stock");
        check(VendorStock.next(stock,11,0).getIntOr("count",0)==1,"New morning replenishes stock");
        var full=stock.copy();full.putInt("count",Integer.MAX_VALUE);
        reject(()->VendorStock.next(full,10,0),"Counter overflow rejects purchase");
        var bad=stock.copy();bad.putInt("count",-1);reject(()->VendorStock.count(bad,10,0),"Corrupt stock does not become free inventory");
        var malformed=encode(c,d);malformed.getCompoundOrEmpty("ledger").getCompoundOrEmpty("outbox").put("999999",new CompoundTag());
        reject(()->c.parse(NbtOps.INSTANCE,malformed).getOrThrow(),"Malformed saved ledger rejected before fallback");
        System.out.println(checks+" commerce custody, stock and interrupted-save checks passed");
    }
}
