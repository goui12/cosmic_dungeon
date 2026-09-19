package net.goui.cosmicdungeon.playerclass.dragoon.repair;
import com.mojang.serialization.*;
import net.goui.cosmicdungeon.economy.*;
import net.goui.cosmicdungeon.playerclass.api.ClassData;
import net.minecraft.nbt.*;
import java.util.*;
public final class RepairCommitChecks {
    private static int checks;
    private static final UUID A=new UUID(0,11),B=new UUID(0,12),SESSION=new UUID(0,13),TX=new UUID(0,14);
    private static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    private static void reject(Runnable action,String why){boolean failed=false;try{action.run();}catch(RuntimeException expected){failed=true;}check(failed,why);}
    @SuppressWarnings("unchecked") private static Codec<PlayerCurrencyData> codec()throws Exception{
        var f=PlayerCurrencyData.class.getDeclaredField("CODEC");f.setAccessible(true);return (Codec<PlayerCurrencyData>)f.get(null);
    }
    private static CompoundTag item(String id,int count,int damage){
        var image=new CompoundTag();image.putString("id",id);image.putInt("count",count);
        if(damage>=0){var c=new CompoundTag();c.putInt("minecraft:damage",damage);c.putInt("minecraft:max_damage",100);
            var data=new CompoundTag();data.putString("bound_owner",A.toString());data.putByteArray("nested",new byte[]{-3,1,100});
            c.put("minecraft:custom_data",data);image.put("components",c);}return image;
    }
    private static RepairCommitPlan plan(){
        var a=RepairCustodyImages.live(RepairCustodyImages.open(SESSION,A,91,true),item("minecraft:iron_sword",1,81),item("minecraft:apple",5,-1));
        var b=RepairCustodyImages.live(RepairCustodyImages.open(SESSION,B,91,false),new CompoundTag(),item("minecraft:bread",2,-1));
        var parts=new ListTag();
        for(int slot:List.of(2,9)){var p=new CompoundTag();p.putInt("slot",slot);p.put("item",item("minecraft:iron_ingot",1,-1));parts.add(p);}
        a=RepairCustodyImages.reserve(a,TX,new ListTag());b=RepairCustodyImages.reserve(b,TX,parts);
        var repaired=a.getCompoundOrEmpty("target").copy();repaired.getCompoundOrEmpty("components").putInt("minecraft:damage",31);
        return new RepairCommitPlan(A,B,a,b,repaired,2,600,-1,80,false,false);
    }
    private static final AccountTransfer.Terms TERMS=new AccountTransfer.Terms(A,B,7,0,"dragoon_repair",91,"immutable-quote");
    private static PlayerCurrencyData fresh(Codec<PlayerCurrencyData> codec){
        var data=codec.parse(NbtOps.INSTANCE,new CompoundTag()).getOrThrow();
        data.setCapacityTrace(A,1000);data.setCapacityTrace(B,1000);data.setBalanceTrace(A,100);data.setBalanceTrace(B,20);
        data.reserve(TX,TERMS,1);data.prepareRepair(TX,plan());return data;
    }
    private static CompoundTag encode(Codec<PlayerCurrencyData> codec,PlayerCurrencyData data){return (CompoundTag)codec.encodeStart(NbtOps.INSTANCE,data).getOrThrow();}
    private static CompoundTag player(CompoundTag custody){
        var root=new CompoundTag();root.put(RepairCustody.KEY,custody.copy());root.put("test_returns",new ListTag());
        var neo=new CompoundTag();neo.put(ClassData.ROOT_TAG,root);var file=new CompoundTag();file.put("NeoForgeData",neo);
        file.put("Inventory",new ListTag());return file;
    }
    private static CompoundTag root(CompoundTag file){return file.getCompoundOrEmpty("NeoForgeData").getCompoundOrEmpty(ClassData.ROOT_TAG);}
    /** Model only the native participant-file boundary; production projection/receipt methods are used. */
    private static CompoundTag apply(RepairCommitPlan plan,UUID owner,String outcome,CompoundTag file){
        var next=file.copy();var r=root(next);
        if(RepairTransactions.receiptMatches(r.getCompoundOrEmpty(RepairTransactions.RECEIPT),owner,TX,outcome))return next;
        var projected=plan.project(owner,r.getCompoundOrEmpty(RepairCustody.KEY),outcome.equals(AccountTransfer.COMMITTED));
        var returns=(ListTag)r.get("test_returns");for(var image:RepairCustodyImages.held(projected,false))returns.add(image);
        r.remove(RepairCustody.KEY);r.put(RepairTransactions.RECEIPT,RepairTransactions.receiptImage(owner,TX,outcome));return next;
    }
    private static int count(CompoundTag file,String id){int count=0;for(var t:(ListTag)root(file).get("test_returns")){
        var item=(CompoundTag)t;if(id.equals(item.getStringOr("id","")))count+=item.getIntOr("count",1);
    }return count;}
    private static int damage(CompoundTag file){for(var t:(ListTag)root(file).get("test_returns")){
        var item=(CompoundTag)t;if(item.getStringOr("id","").equals("minecraft:iron_sword"))return item.getCompoundOrEmpty("components").getIntOr("minecraft:damage",0);
    }return -1;}
    private record Cut(String label,CompoundTag money,CompoundTag customer,CompoundTag provider){}
    private static Cut cut(String label,CompoundTag money,CompoundTag a,CompoundTag b){return new Cut(label,money.copy(),a.copy(),b.copy());}
    public static void main(String[] args)throws Exception{
        var codec=codec();var p=plan();var data=fresh(codec);
        check(data.pendingRepair(A).orElseThrow().equals(TX)&&data.pendingRepair(B).orElseThrow().equals(TX),"Both owners indexed before item commit");
        var loaded=codec.parse(NbtOps.INSTANCE,encode(codec,data)).getOrThrow();
        check(loaded.repairPlan(TX).orElseThrow().equals(p),"Exact full plan round-trips in account payload");
        check(loaded.pendingRepair(B).orElseThrow().equals(TX),"Owner index reconstructs after restart");
        check(loaded.reservedDebit(A)==7&&loaded.reservedCredit(B)==7,"Plan does not create second balances or holds");
        var start=data.startRepair(TX,400);
        check(start.startedTick()==400&&start.durationTicks()==80,"Channel captures accepted duration");
        check(data.startRepair(TX,410).equals(start),"Repeated start cannot extend or shorten channel");
        var timedData=data;reject(()->timedData.commitRepair(TX,479,2),"Cannot commit a tick before channel finishes");
        check(data.getBalanceTrace(A)==100&&data.getBalanceTrace(B)==20,"Early commit moves no money");
        var committed=data.commitRepair(TX,480,2);
        check(committed.status().equals(AccountTransfer.COMMITTED)&&data.getBalanceTrace(A)==93&&data.getBalanceTrace(B)==27,"Exact completion commits both balances");
        data.change(A,"",1,"test","",0,"after-repair",false);
        check(data.commitRepair(TX,500,3).equals(committed)&&data.getBalanceTrace(A)==94,"Completion replay preserves later balance changes");
        var projection=start.project(A,start.customerBefore(),true);
        check(projection.getCompoundOrEmpty("target").getCompoundOrEmpty("components").getIntOr("minecraft:damage",-1)==31,"Only approved damage changes");
        check(projection.getCompoundOrEmpty("cursor").equals(start.customerBefore().getCompoundOrEmpty("cursor")),"Customer cursor is never consumed as payment");
        check(RepairCustodyImages.held(start.project(B,start.providerBefore(),true),true).isEmpty(),"Committed provider components consumed once");
        check(RepairCustodyImages.held(start.project(B,start.providerBefore(),false),true).size()==2,"Cancelled exact component lots returned");
        check(start.project(A,start.customerBefore(),false).equals(start.customerBefore()),"Cancel preserves original damage and all metadata");
        var foreign=start.customerBefore();foreign.putString("owner",B.toString());
        reject(()->start.project(A,foreign,true),"Mismatched owner image cannot be replaced from journal");
        reject(()->start.project(A,new CompoundTag(),true),"Missing player evidence cannot create a replacement item");
        var changed=start.customerBefore();changed.getCompoundOrEmpty("target").putString("id","minecraft:diamond_sword");
        reject(()->start.project(A,changed,true),"Changed live item requires review");
        var after=start.repairedItem();after.getCompoundOrEmpty("components").getCompoundOrEmpty("minecraft:custom_data").putString("bound_owner",B.toString());
        check(!RepairCommitPlan.durabilityOnly(start.customerBefore().getCompoundOrEmpty("target"),after),"Repair cannot change ownership/custom data");
        var countChanged=start.repairedItem();countChanged.putInt("count",2);
        check(!RepairCommitPlan.durabilityOnly(start.customerBefore().getCompoundOrEmpty("target"),countChanged),"Repair cannot duplicate target count");
        var pristine=start.repairedItem();pristine.getCompoundOrEmpty("components").putInt("minecraft:damage",0);
        check(RepairCommitPlan.durabilityOnly(start.customerBefore().getCompoundOrEmpty("target"),pristine),"Full repair remains valid");
        pristine.getCompoundOrEmpty("components").remove("minecraft:damage");
        check(RepairCommitPlan.durabilityOnly(start.customerBefore().getCompoundOrEmpty("target"),pristine),"Default damage omission preserves native ItemStack encoding");
        var escaped=start.customerBefore();for(String key:List.copyOf(escaped.keySet()))escaped.remove(key);check(!start.customerBefore().isEmpty(),"Plan getters cannot mutate journal evidence");
        reject(()->p.start(600),"Ready deadline is exclusive at its exact boundary");
        check(!p.elapsed(1000)&&!start.elapsed(479)&&start.elapsed(480),"Not-started and partial channels remain uncommittable");
        var receipt=RepairTransactions.receiptImage(A,TX,AccountTransfer.COMMITTED);
        check(RepairTransactions.receiptMatches(receipt,A,TX,AccountTransfer.COMMITTED),"Participant receipt binds transaction, owner and decision");
        check(!RepairTransactions.receiptMatches(receipt,B,TX,AccountTransfer.COMMITTED),"Other owner cannot reuse receipt");
        check(!RepairTransactions.receiptMatches(receipt,A,SESSION,AccountTransfer.COMMITTED),"Other transaction cannot reuse receipt");
        check(!RepairTransactions.receiptMatches(receipt,A,TX,AccountTransfer.CANCELLED),"Cancel receipt cannot acknowledge a commit");
        reject(()->RepairTransactions.receiptImage(A,TX,AccountTransfer.RESERVED),"Undecided receipt is invalid");
        var undecided=fresh(codec);reject(()->undecided.acknowledgeRepair(TX,A),"Cannot acknowledge before a terminal account decision");
        var corrupt=encode(codec,fresh(codec));corrupt.remove("transfers");
        reject(()->codec.parse(NbtOps.INSTANCE,corrupt).getOrThrow(),"Orphan plan cannot load as empty currency state");
        var old=new CompoundTag();var legacy=codec.parse(NbtOps.INSTANCE,old).getOrThrow();
        check(legacy.pendingRepair(A).isEmpty(),"Pre-plan save defaults to no pending repairs");
        // Enumerate interruption points around independent account/customer/provider native saves.
        data=fresh(codec);data.startRepair(TX,400);
        var diskMoney=encode(codec,data);var diskA=player(p.customerBefore());var diskB=player(p.providerBefore());
        var cuts=new ArrayList<Cut>();cuts.add(cut("ready/start saved",diskMoney,diskA,diskB));
        data.commitRepair(TX,480,2);cuts.add(cut("decision in memory only",diskMoney,diskA,diskB));
        diskMoney=encode(codec,data);cuts.add(cut("decision saved",diskMoney,diskA,diskB));
        var memA=apply(data.repairPlan(TX).orElseThrow(),A,AccountTransfer.COMMITTED,diskA);
        cuts.add(cut("customer projection not saved",diskMoney,diskA,diskB));diskA=memA;
        cuts.add(cut("customer receipt saved",diskMoney,diskA,diskB));data.acknowledgeRepair(TX,A);
        cuts.add(cut("customer acknowledgement in memory",diskMoney,diskA,diskB));diskMoney=encode(codec,data);
        cuts.add(cut("customer acknowledgement saved",diskMoney,diskA,diskB));
        var memB=apply(data.repairPlan(TX).orElseThrow(),B,AccountTransfer.COMMITTED,diskB);
        cuts.add(cut("provider projection not saved",diskMoney,diskA,diskB));diskB=memB;
        cuts.add(cut("provider receipt saved",diskMoney,diskA,diskB));data.acknowledgeRepair(TX,B);
        cuts.add(cut("terminal plan pruning in memory",diskMoney,diskA,diskB));diskMoney=encode(codec,data);
        cuts.add(cut("terminal plan pruning saved",diskMoney,diskA,diskB));
        var directory=java.nio.file.Files.createTempDirectory("cosmic-repair-commit-");
        try{
            for(var cut:cuts){
                NbtIo.writeCompressed(cut.money(),directory.resolve("account.dat"));NbtIo.writeCompressed(cut.customer(),directory.resolve("customer.dat"));NbtIo.writeCompressed(cut.provider(),directory.resolve("provider.dat"));
                var recovered=codec.parse(NbtOps.INSTANCE,NbtIo.readCompressed(directory.resolve("account.dat"),NbtAccounter.unlimitedHeap())).getOrThrow();
                var a=NbtIo.readCompressed(directory.resolve("customer.dat"),NbtAccounter.unlimitedHeap());var b=NbtIo.readCompressed(directory.resolve("provider.dat"),NbtAccounter.unlimitedHeap());
                boolean didCommit=recovered.transfer(TX).orElseThrow().status().equals(AccountTransfer.COMMITTED);
                recovered.cancelReservations("dragoon_repair",10,"restart");
                String outcome=recovered.transfer(TX).orElseThrow().status();
                for(UUID owner:List.of(A,B))if(recovered.pendingRepair(owner).isPresent()){
                    var plan=recovered.repairPlan(TX).orElseThrow();
                    if(owner.equals(A))a=apply(plan,owner,outcome,a);else b=apply(plan,owner,outcome,b);
                    recovered.acknowledgeRepair(TX,owner);
                }
                check(recovered.getBalanceTrace(A)==(didCommit?93:100)&&recovered.getBalanceTrace(B)==(didCommit?27:20),cut.label()+": paired money exactly once");
                check(count(a,"minecraft:iron_sword")==1&&damage(a)==(didCommit?31:81),cut.label()+": one target, correct durability");
                check(count(b,"minecraft:iron_ingot")== (didCommit?0:2),cut.label()+": components consumed only with saved decision");
                check(count(a,"minecraft:apple")==5&&count(b,"minecraft:bread")==2,cut.label()+": cursors preserved for respective owners");
                check(count(b,"minecraft:iron_sword")==0&&count(a,"minecraft:iron_ingot")==0,cut.label()+": no ownership transfer");
                check(recovered.pendingRepair(A).isEmpty()&&recovered.pendingRepair(B).isEmpty()&&recovered.repairPlan(TX).isEmpty(),cut.label()+": full images pruned only after receipts");
                var stableMoney=encode(codec,recovered);var restarted=codec.parse(NbtOps.INSTANCE,stableMoney).getOrThrow();
                check(restarted.cancelReservations("dragoon_repair",11,"again")==0&&encode(codec,restarted).equals(stableMoney),cut.label()+": repeated restart never refunds or charges");
                check(RepairTransactions.receiptMatches(root(a).getCompoundOrEmpty(RepairTransactions.RECEIPT),A,TX,outcome)
                        &&RepairTransactions.receiptMatches(root(b).getCompoundOrEmpty(RepairTransactions.RECEIPT),B,TX,outcome),cut.label()+": both participant receipts survive");
            }
        }finally{for(String file:List.of("account.dat","customer.dat","provider.dat"))java.nio.file.Files.deleteIfExists(directory.resolve(file));java.nio.file.Files.deleteIfExists(directory);}
        check(!RepairQuoteRules.poll(false,177,177),"Idle server does no repair polling");
        check(!RepairQuoteRules.poll(true,176,177),"A channel never finishes before its exact due tick");
        check(RepairQuoteRules.poll(true,177,177),"Off-grid channel completion does not wait for twentieth-tick poll");
        check(RepairQuoteRules.poll(true,160,177),"Range/lifecycle watchdog retains existing bounded periodic poll");
        check(RepairQuoteRules.poll(true,739,739),"Ready expiry also wakes on the exact deadline");
        System.out.println(checks+" repair commit and interrupted-save checks passed");
    }
}
