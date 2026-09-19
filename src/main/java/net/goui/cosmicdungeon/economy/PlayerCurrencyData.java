package net.goui.cosmicdungeon.economy;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.goui.cosmicdungeon.playerclass.dragoon.repair.RepairCommitPlan;
import net.goui.cosmicdungeon.trade.TradeCommitPlan;

import java.util.*;
import java.nio.file.*;
import net.minecraft.nbt.*;
import net.minecraft.world.level.storage.LevelResource;

public final class PlayerCurrencyData extends SavedData {
    public static final String SAVE_ID = "cosmicdungeon_player_currency_v1";
    public static final long DEFAULT_CAPACITY_TRACE = 100_000_000L;

    private static final Codec<UUID> UUID_CODEC = Codec.STRING.xmap(UUID::fromString, UUID::toString);
    private static final Codec<Map<UUID, Long>> UUID_LONG_MAP_CODEC = Codec.unboundedMap(UUID_CODEC, Codec.LONG);

    private static final Codec<PlayerCurrencyData> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            UUID_LONG_MAP_CODEC.optionalFieldOf("balances", Map.of()).forGetter(data -> data.balanceByPlayer),
            UUID_LONG_MAP_CODEC.optionalFieldOf("capacity_overrides", Map.of()).forGetter(data -> data.capacityOverrideByPlayer),
            Codec.unboundedMap(Codec.STRING,Codec.LONG).optionalFieldOf("receipts",Map.of()).forGetter(data->data.receipts),
            Codec.STRING.listOf().optionalFieldOf("wealth_crossings",java.util.List.of()).forGetter(data->java.util.List.copyOf(data.wealthCrossings)),
            Codec.unboundedMap(UUID_CODEC,AccountTransfer.CODEC).optionalFieldOf("transfers",Map.of()).forGetter(data->data.transfers),
            Codec.unboundedMap(UUID_CODEC,RepairCommitPlan.CODEC).optionalFieldOf("repair_plans",Map.of()).forGetter(data->data.repairPlans),
            Codec.unboundedMap(UUID_CODEC,TradeCommitPlan.CODEC).optionalFieldOf("trade_plans",Map.of()).forGetter(data->data.tradePlans),
            Codec.unboundedMap(UUID_CODEC,AccountOperation.CODEC).optionalFieldOf("operations",Map.of()).forGetter(data->data.operations),
            CompoundTag.CODEC.optionalFieldOf("ledger",new CompoundTag()).forGetter(data->data.ledger),
            CompoundTag.CODEC.optionalFieldOf("death_currency",new DeathCurrencyState().save()).forGetter(data->data.deaths.save())
    ).apply(inst, PlayerCurrencyData::fromCodec));

    public static final SavedDataType<PlayerCurrencyData> TYPE = new SavedDataType<>(
            SAVE_ID,
            PlayerCurrencyData::new,
            CODEC
    );

    private final Map<UUID, Long> balanceByPlayer = new HashMap<>();
    private final Map<UUID, Long> capacityOverrideByPlayer = new HashMap<>();

    private final Map<String,Long> receipts=new HashMap<>();
    private final java.util.Set<String> wealthCrossings=new java.util.HashSet<>();
    private final Map<UUID,AccountTransfer> transfers=new LinkedHashMap<>();
    private final Map<UUID,Set<UUID>> reservedTransfersByOwner=new HashMap<>();
    private final Map<UUID,RepairCommitPlan> repairPlans=new LinkedHashMap<>();
    private final Map<UUID,UUID> repairByOwner=new HashMap<>();
    private final Map<UUID,TradeCommitPlan> tradePlans=new LinkedHashMap<>();
    private final Map<UUID,UUID> tradeByOwner=new HashMap<>();
    private final Map<UUID,AccountOperation> operations=new LinkedHashMap<>();
    private final Map<UUID,UUID> operationByOwner=new HashMap<>();
    private CompoundTag ledger=new CompoundTag();
    private DeathCurrencyState deaths=new DeathCurrencyState();
    private final Map<UUID,Long> heldDebit=new HashMap<>(),heldCredit=new HashMap<>();
    private static final Set<MinecraftServer> VALIDATED=Collections.newSetFromMap(new WeakHashMap<>());
    private MinecraftServer server;
    private PlayerCurrencyData() {}

    private static PlayerCurrencyData fromCodec(Map<UUID, Long> balances, Map<UUID, Long> overrides,
                                                Map<String,Long> receipts,java.util.List<String> crossings,Map<UUID,AccountTransfer> transfers,Map<UUID,RepairCommitPlan> repairPlans,Map<UUID,TradeCommitPlan> tradePlans,Map<UUID,AccountOperation> operations,CompoundTag ledger,CompoundTag deathCurrency) {
        PlayerCurrencyData data = new PlayerCurrencyData();
        if (balances != null) data.balanceByPlayer.putAll(balances);
        if (overrides != null) data.capacityOverrideByPlayer.putAll(overrides);
        data.receipts.putAll(receipts);data.wealthCrossings.addAll(crossings);
        if(data.balanceByPlayer.values().stream().anyMatch(v->v<0)
                ||data.capacityOverrideByPlayer.values().stream().anyMatch(v->v<0)
                ||data.receipts.values().stream().anyMatch(v->v<0))
            throw new IllegalArgumentException("Negative saved account state requires review");
        transfers.forEach(data::recordTransfer);
        if(data.heldDebit.entrySet().stream().anyMatch(e->e.getValue()>data.getBalanceTrace(e.getKey())))
            throw new IllegalArgumentException("Saved reservations exceed their account balances");
        repairPlans.forEach((id,plan)->{
            data.validateRepairPlan(id,plan);data.repairPlans.put(id,plan);data.indexRepair(id,plan);
        });
        tradePlans.forEach((id,plan)->{data.validateTradePlan(id,plan);data.tradePlans.put(id,plan);data.indexTrade(id,plan);});
        data.ledger=ledger.copy();data.validateLedger();
        data.deaths=DeathCurrencyState.load(deathCurrency);
        for(var record:data.deaths.entries.values())if(!record.active()&&record.before()!=data.getBalanceTrace(record.owner()))
            throw new IllegalArgumentException("Pending death account image mismatch");
        operations.forEach(data::loadOperation);
        if(data.heldDebit.entrySet().stream().anyMatch(e->e.getValue()>data.getBalanceTrace(e.getKey())))throw new IllegalArgumentException("Operation reservations exceed balance");
        return data;
    }

    public static PlayerCurrencyData get(MinecraftServer server) {
        if (server == null) throw new IllegalArgumentException("server is null");
        if(!VALIDATED.contains(server)){
            try{if(Files.exists(path(server)))read(server);}
            catch(Exception error){throw new IllegalStateException("Currency save requires recovery; original file preserved",error);}
            VALIDATED.add(server);
        }
        ServerLevel overworld = server.overworld();
        var data=overworld.getDataStorage().computeIfAbsent(TYPE);data.server=server;data.initializeLedger();return data;
    }

    private static Path path(MinecraftServer server){
        return server.getWorldPath(LevelResource.ROOT).resolve("data/"+SAVE_ID+".dat");
    }
    private static PlayerCurrencyData read(MinecraftServer server)throws java.io.IOException{
        var root=NbtIo.readCompressed(path(server),NbtAccounter.create(64L*1024*1024));
        var body=root.getCompound("data").orElseThrow(()->new java.io.IOException("Missing currency data body"));
        return CODEC.parse(NbtOps.INSTANCE,body).getOrThrow();
    }
    /** Explicit readback: vanilla SavedData saveAndJoin catches I/O failures internally. */
    public boolean flushVerified(){
        if(server==null)return false;
        try{
            setDirty();server.overworld().getDataStorage().saveAndJoin();
            var saved=read(server);
            boolean matches=balanceByPlayer.equals(saved.balanceByPlayer)&&capacityOverrideByPlayer.equals(saved.capacityOverrideByPlayer)
                    &&receipts.equals(saved.receipts)&&wealthCrossings.equals(saved.wealthCrossings)&&transfers.equals(saved.transfers)
                    &&repairPlans.equals(saved.repairPlans)&&tradePlans.equals(saved.tradePlans)
                    &&operations.equals(saved.operations)&&ledger.equals(saved.ledger)&&deaths.save().equals(saved.deaths.save());
            if(!matches){setDirty();return false;}
            var pending=ledger.getCompoundOrEmpty("outbox");
            if(!pending.isEmpty()){
                var batch=new TreeMap<Long,CompoundTag>();
                pending.keySet().stream().map(Long::parseLong).sorted().limit(D1EconomyConfig.LEDGER_ROWS_PER_FLUSH.get()).forEach(id->batch.put(id,pending.getCompoundOrEmpty(Long.toString(id)).copy()));
                EconomyLedger.archive(server.getWorldPath(LevelResource.ROOT).resolve("data/cosmic_economy_ledger_v1"),batch);
                for(long id:batch.keySet())pending.remove(Long.toString(id));ledger.put("outbox",pending);setDirty();
                server.overworld().getDataStorage().saveAndJoin();
                if(!ledger.equals(read(server).ledger))return false;
                // Remaining rows still share the verified account image; archive in bounded later batches.
            }
            return true;
        }catch(Exception error){
            com.mojang.logging.LogUtils.getLogger().error("Currency save verification failed; transaction requires reconciliation",error);
            setDirty();return false;
        }
    }
    private static void adjustHold(Map<UUID,Long> map,UUID owner,long amount){
        long value=Math.addExact(map.getOrDefault(owner,0L),amount);
        if(value<0)throw new IllegalStateException("Negative account reservation");
        if(value==0)map.remove(owner);else map.put(owner,value);
    }
    private void index(AccountTransfer transfer,int sign){
        if(!transfer.reserved())return;
        var terms=transfer.terms();
        adjustHold(heldDebit,terms.first(),sign*terms.firstPays());
        adjustHold(heldCredit,terms.second(),sign*terms.firstPays());
        adjustHold(heldDebit,terms.second(),sign*terms.secondPays());
        adjustHold(heldCredit,terms.first(),sign*terms.secondPays());
    }
    private void recordTransfer(UUID id,AccountTransfer transfer){
        var old=transfers.get(id);if(old!=null)index(old,-1);
        transfers.put(id,transfer);index(transfer,1);
        if(old!=null&&old.reserved())for(UUID owner:List.of(old.terms().first(),old.terms().second())){
            var ids=reservedTransfersByOwner.get(owner);
            if(ids!=null){ids.remove(id);if(ids.isEmpty())reservedTransfersByOwner.remove(owner);}
        }
        if(transfer.reserved())for(UUID owner:List.of(transfer.terms().first(),transfer.terms().second()))
            reservedTransfersByOwner.computeIfAbsent(owner,k->new HashSet<>()).add(id);
    }

    public Optional<DeathCurrencyRecord> deathDrop(UUID id){return Optional.ofNullable(deaths.entries.get(id));}
    public List<UUID> deathIds(){return List.copyOf(deaths.entries.keySet());}
    public boolean pendingDeath(UUID owner){return deaths.pending.containsKey(owner);}
    public boolean deathSeen(UUID owner,UUID life){return life.equals(deaths.latest.get(owner));}
    private boolean deathBlocked(UUID owner){
        return pendingDeath(owner)||(server!=null&&DeathCurrencyService.suspended(server));
    }
    private void requireDeathReady(UUID owner){
        if(deathBlocked(owner))throw new IllegalStateException("Death currency awaits durable reconciliation");
    }
    /** A frozen death intent prevents later spending while existing commerce reservations unwind. */
    public boolean prepareDeath(DeathCurrencyRecord record){
        var existing=deaths.entries.get(record.id());
        if(existing!=null){
            if(!existing.owner().equals(record.owner()))throw new IllegalArgumentException("Foreign death ID");
            return false;
        }
        if(deathSeen(record.owner(),record.id()))return false;
        if(pendingDeath(record.owner())||record.active()||record.before()!=getBalanceTrace(record.owner()))
            throw new IllegalStateException("Previous death or changed account requires reconciliation");
        deaths.entries.put(record.id(),record);deaths.latest.put(record.owner(),record.id());
        deaths.pending.put(record.owner(),record.id());setDirty();return true;
    }
    /** Debit, logical drop, and ledger become one authoritative image. No separate entity-file commit. */
    public boolean commitDeath(UUID id){
        var record=deaths.entries.get(id);if(record==null||record.active())return false;
        UUID owner=record.owner();
        // Death cancels only undecided money reservations. Existing item custody plans remain
        // for their normal owner-local return/acknowledgement path, including offline participants.
        for(UUID transaction:List.copyOf(reservedTransfersByOwner.getOrDefault(owner,Set.of()))){
            var transfer=transfers.get(transaction);
            cancelTransfer(transaction,transfer.terms(),System.currentTimeMillis(),"player_death");
        }
        UUID operationId=operationByOwner.get(owner);
        if(operationId!=null&&operations.get(operationId).reserved())
            decideOperation(operationId,false,System.currentTimeMillis());
        if(reservedDebit(owner)!=0||reservedCredit(owner)!=0)return false;
        if(getBalanceTrace(owner)!=record.before())throw new IllegalStateException("Pending death balance changed");
        long after=record.before()-record.amount();
        journal(EconomyLedger.row(id.toString(),owner,accountName(owner),"death_debit",-record.amount(),
                record.before(),after,id.toString(),record.run(),"committed",record.created(),record.save()));
        balanceByPlayer.put(owner,after);deaths.pending.remove(owner,id);
        if(record.amount()==0)deaths.entries.remove(id);else deaths.entries.put(id,record.activate());
        setDirty();return true;
    }
    /** Entire amount transfers directly into the collecting account, with no partial pickup. */
    public boolean collectDeath(UUID id,UUID collector,long now){
        var record=deaths.entries.get(id);
        if(record==null||!record.active()||deathBlocked(collector)||!canDeposit(collector,record.amount()))return false;
        long before=getBalanceTrace(collector),after=Math.addExact(before,record.amount());
        var details=record.save();details.putString("collector",collector.toString());
        journal(EconomyLedger.row(id.toString(),collector,accountName(collector),"death_pickup",record.amount(),
                before,after,record.owner().toString(),record.run(),"committed",now,details));
        balanceByPlayer.put(collector,after);deaths.entries.remove(id);setDirty();
        CurrencyAudit.report(server,collector,accountName(collector),id.toString(),"death_pickup",record.amount(),
                before,after,0,record.owner().toString(),record.run(),"committed");
        return true;
    }
    /** Destruction removes supply, not a second debit from the original owner's account. */
    public boolean destroyDeath(UUID id,String reason,long now){
        var record=deaths.entries.get(id);if(record==null||!record.active())return false;
        var details=record.save();details.putLong("drop_destroyed_trace",record.amount());details.putString("reason",reason);
        long balance=getBalanceTrace(record.owner());
        journal(EconomyLedger.row(id.toString(),record.owner(),accountName(record.owner()),"death_despawn",-record.amount(),
                balance,balance,id.toString(),record.run(),"committed",now,details));
        deaths.entries.remove(id);setDirty();return true;
    }
    public void snapshotDeath(UUID id,String dimension,CompoundTag image){
        var old=deaths.entries.get(id);if(old==null||!old.active())return;
        var next=old.snapshot(dimension,image);
        if(!next.equals(old)){deaths.entries.put(id,next);setDirty();}
    }

    public Optional<AccountTransfer> transfer(UUID id){return Optional.ofNullable(transfers.get(id));}
    public long reservedDebit(UUID player){return heldDebit.getOrDefault(player,0L);}
    public long reservedCredit(UUID player){return heldCredit.getOrDefault(player,0L);}
    public long availableTrace(UUID player){return Math.max(0,getBalanceTrace(player)-reservedDebit(player));}
    public long availableCapacity(UUID player){
        long balance=getBalanceTrace(player),capacity=getCapacityTrace(player),reserved=reservedCredit(player);
        if(balance>=capacity)return 0;
        return Math.max(0,capacity-balance-reserved);
    }
    public AccountTransfer reserve(UUID id,AccountTransfer.Terms terms,long timestamp){
        Objects.requireNonNull(id);Objects.requireNonNull(terms);
        requireDeathReady(terms.first());requireDeathReady(terms.second());
        var previous=transfers.get(id);
        if(previous!=null){
            if(!previous.terms().equals(terms))throw new IllegalArgumentException("Transaction identifier reused with different terms");
            return previous;
        }
        String reason="";
        if(availableTrace(terms.first())<terms.firstPays()||availableTrace(terms.second())<terms.secondPays())reason="insufficient_available_balance";
        else if(availableCapacity(terms.first())<terms.secondPays()||availableCapacity(terms.second())<terms.firstPays())reason="insufficient_available_capacity";
        long first=getBalanceTrace(terms.first()),second=getBalanceTrace(terms.second());
        var entry=new AccountTransfer(terms,reason.isEmpty()?AccountTransfer.RESERVED:AccountTransfer.REJECTED,timestamp,
                first,second,first,second,reason);
        transferJournal(id,entry);recordTransfer(id,entry);setDirty();return entry;
    }
    public boolean reservationValid(UUID id,AccountTransfer.Terms terms){
        var entry=transfers.get(id);if(entry==null||!entry.reserved()||!entry.terms().equals(terms))return false;
        if(getBalanceTrace(terms.first())<reservedDebit(terms.first())||getBalanceTrace(terms.second())<reservedDebit(terms.second()))return false;
        return (terms.secondPays()==0||creditFits(terms.first()))&&(terms.firstPays()==0||creditFits(terms.second()));
    }
    private boolean creditFits(UUID player){
        long balance=getBalanceTrace(player),capacity=getCapacityTrace(player);
        return balance<=capacity&&reservedCredit(player)<=capacity-balance;
    }
    public AccountTransfer commitTransfer(UUID id,AccountTransfer.Terms terms,long timestamp){
        var previous=transfers.get(id);
        if(previous==null||!previous.terms().equals(terms))throw new IllegalArgumentException("Missing or mismatched reserved transfer");
        if(previous.status().equals(AccountTransfer.COMMITTED))return previous;
        requireDeathReady(terms.first());requireDeathReady(terms.second());
        if(!reservationValid(id,terms))return previous;
        long first=getBalanceTrace(terms.first()),second=getBalanceTrace(terms.second());
        long firstAfter=Math.addExact(first-terms.firstPays(),terms.secondPays());
        long secondAfter=Math.addExact(second-terms.secondPays(),terms.firstPays());
        var committed=new AccountTransfer(terms,AccountTransfer.COMMITTED,timestamp,first,second,firstAfter,secondAfter,"");
        // Both balances and the idempotent receipt become one authoritative SavedData image.
        transferJournal(id,committed);recordTransfer(id,committed);
        balanceByPlayer.put(terms.first(),firstAfter);balanceByPlayer.put(terms.second(),secondAfter);
        setDirty();return committed;
    }
    public AccountTransfer cancelTransfer(UUID id,AccountTransfer.Terms terms,long timestamp,String reason){
        var previous=transfers.get(id);
        if(previous==null||!previous.terms().equals(terms))throw new IllegalArgumentException("Missing or mismatched reserved transfer");
        if(!previous.reserved())return previous;
        long first=getBalanceTrace(terms.first()),second=getBalanceTrace(terms.second());
        var cancelled=new AccountTransfer(terms,AccountTransfer.CANCELLED,timestamp,first,second,first,second,reason);
        transferJournal(id,cancelled);recordTransfer(id,cancelled);setDirty();return cancelled;
    }
    /** Only RESERVED (no payment committed) entries of this exact workflow can expire on restart.
     * Future item-journal committing states must reconcile first, never pass through this method. */
    public int cancelReservations(String type,long timestamp,String reason){
        int count=0;
        for(var entry:List.copyOf(transfers.entrySet())){
            var transfer=entry.getValue();
            if(transfer.reserved()&&transfer.terms().type().equals(type)){
                var cancelled=cancelTransfer(entry.getKey(),transfer.terms(),timestamp,reason);
                reportTransfer(entry.getKey(),cancelled);count++;
            }
        }
        return count;
    }
    private void transferJournal(UUID id,AccountTransfer receipt){
        var terms=receipt.terms();var details=new CompoundTag();details.putLong("first_pays",terms.firstPays());details.putLong("second_pays",terms.secondPays());details.putString("first_owner",terms.first().toString());
        if(tradePlans.containsKey(id))details.put("trade_plan",TradeCommitPlan.CODEC.encodeStart(NbtOps.INSTANCE,tradePlans.get(id)).getOrThrow());
        if(repairPlans.containsKey(id))details.put("repair_plan",RepairCommitPlan.CODEC.encodeStart(NbtOps.INSTANCE,repairPlans.get(id)).getOrThrow());
        var savedLedger=ledger.copy();try{
            journal(EconomyLedger.row(id.toString(),terms.first(),accountName(terms.first()),terms.type(),terms.secondPays()-terms.firstPays(),receipt.firstBefore(),receipt.firstAfter(),terms.second().toString(),terms.run(),receipt.status(),receipt.timestamp(),details));
            journal(EconomyLedger.row(id.toString(),terms.second(),accountName(terms.second()),terms.type(),terms.firstPays()-terms.secondPays(),receipt.secondBefore(),receipt.secondAfter(),terms.first().toString(),terms.run(),receipt.status(),receipt.timestamp(),details));
        }catch(RuntimeException failure){ledger=savedLedger;throw failure;}
    }
    public void recordAttempt(String id,UUID owner,String type,String related,long run,String status,CompoundTag details){
        long balance=getBalanceTrace(owner);journal(EconomyLedger.row(id,owner,accountName(owner),type,0,balance,balance,related,run,status,System.currentTimeMillis(),details));
    }
    public void reportTransfer(UUID id,AccountTransfer receipt){
        var terms=receipt.terms();
        CurrencyAudit.report(server,terms.first(),accountName(terms.first()),id.toString(),terms.type(),
                terms.secondPays()-terms.firstPays(),receipt.firstBefore(),receipt.firstAfter(),0,
                terms.second().toString(),terms.run(),receipt.status());
        CurrencyAudit.report(server,terms.second(),accountName(terms.second()),id.toString(),terms.type(),
                terms.firstPays()-terms.secondPays(),receipt.secondBefore(),receipt.secondAfter(),0,
                terms.first().toString(),terms.run(),receipt.status());
    }
    private String accountName(UUID id){
        var player=server==null?null:server.getPlayerList().getPlayer(id);
        return player==null?"":player.getName().getString();
    }
    private void validateRepairPlan(UUID id,RepairCommitPlan plan){
        var transfer=transfers.get(id);
        if(!id.equals(plan.transaction())||transfer==null||!transfer.terms().type().equals("dragoon_repair")
                ||!transfer.terms().first().equals(plan.customer())||!transfer.terms().second().equals(plan.provider())
                ||transfer.terms().secondPays()!=0
                ||(transfer.reserved()&&(plan.customerAck()||plan.providerAck()))
                ||(transfer.status().equals(AccountTransfer.COMMITTED)&&plan.startedTick()<0))
            throw new IllegalArgumentException("Unbound repair decision");
    }
    private void indexRepair(UUID id,RepairCommitPlan plan){
        for(UUID owner:List.of(plan.customer(),plan.provider()))if(!plan.acknowledged(owner)){
            var old=repairByOwner.putIfAbsent(owner,id);
            if(old!=null&&!old.equals(id))throw new IllegalStateException("Player has multiple unsettled repairs");
        }
    }
    public Optional<RepairCommitPlan> repairPlan(UUID id){return Optional.ofNullable(repairPlans.get(id));}
    public Optional<UUID> pendingRepair(UUID owner){return Optional.ofNullable(repairByOwner.get(owner));}
    public void prepareRepair(UUID id,RepairCommitPlan plan){
        validateRepairPlan(id,plan);
        var existing=repairPlans.get(id);
        if(existing!=null){if(!existing.equals(plan))throw new IllegalStateException("Immutable repair quote changed");return;}
        if(!transfers.get(id).reserved()||plan.customerAck()||plan.providerAck()||plan.startedTick()!= -1)
            throw new IllegalStateException("Repair cannot prepare from terminal state");
        for(UUID owner:List.of(plan.customer(),plan.provider()))
            if(repairByOwner.containsKey(owner)||tradeByOwner.containsKey(owner)||operationByOwner.containsKey(owner))throw new IllegalStateException("Prior item transaction awaits recovery");
        repairPlans.put(id,plan);indexRepair(id,plan);setDirty();
    }
    public RepairCommitPlan startRepair(UUID id,long now){
        var plan=repairPlans.get(id);
        if(plan==null||!transfers.get(id).reserved())throw new IllegalStateException("No reserved repair");
        var next=plan.start(now);repairPlans.put(id,next);setDirty();return next;
    }
    public AccountTransfer commitRepair(UUID id,long now,long timestamp){
        var plan=repairPlans.get(id);var transfer=transfers.get(id);
        if(plan==null||transfer==null||!plan.elapsed(now))throw new IllegalStateException("Repair has not completed its channel");
        return commitTransfer(id,transfer.terms(),timestamp);
    }
    public void acknowledgeRepair(UUID id,UUID owner){
        var plan=repairPlans.get(id);if(plan==null)throw new IllegalStateException("No pending repair plan");
        var transfer=transfers.get(id);if(transfer.reserved())throw new IllegalStateException("Cannot acknowledge an undecided repair");
        var next=plan.acknowledge(owner);repairByOwner.remove(owner,id);
        // Participant receipts already share their verified inventory files. Keep the compact
        // immutable financial receipt; discard large item images after both acknowledgements.
        if(next.customerAck()&&next.providerAck())repairPlans.remove(id);else repairPlans.put(id,next);
        setDirty();
    }
    private void validateTradePlan(UUID id,TradeCommitPlan plan){
        var transfer=transfers.get(id);
        if(!id.equals(plan.transaction())||transfer==null||!transfer.terms().type().equals("player_trade")
                ||!transfer.terms().first().equals(plan.first())||!transfer.terms().second().equals(plan.second())
                ||transfer.terms().run()!=plan.firstBefore().getLongOr("run",-1)
                ||(transfer.reserved()&&(plan.firstAck()||plan.secondAck())))throw new IllegalArgumentException("Unbound trade decision");
    }
    private void indexTrade(UUID id,TradeCommitPlan plan){
        for(UUID owner:List.of(plan.first(),plan.second()))if(!plan.acknowledged(owner)){
            var old=tradeByOwner.putIfAbsent(owner,id);if(old!=null&&!old.equals(id))throw new IllegalStateException("Multiple unsettled trades");
        }
    }
    public Optional<TradeCommitPlan> tradePlan(UUID id){return Optional.ofNullable(tradePlans.get(id));}
    public Optional<UUID> pendingTrade(UUID owner){return Optional.ofNullable(tradeByOwner.get(owner));}
    public void prepareTrade(UUID id,TradeCommitPlan plan){
        validateTradePlan(id,plan);var old=tradePlans.get(id);
        if(old!=null){if(!old.equals(plan))throw new IllegalStateException("Immutable trade changed");return;}
        if(!transfers.get(id).reserved()||plan.firstAck()||plan.secondAck())throw new IllegalStateException("Trade is terminal");
        for(UUID owner:List.of(plan.first(),plan.second()))if(tradeByOwner.containsKey(owner)||repairByOwner.containsKey(owner)||operationByOwner.containsKey(owner))throw new IllegalStateException("Prior transaction awaits recovery");
        tradePlans.put(id,plan);indexTrade(id,plan);setDirty();
    }
    public AccountTransfer commitTrade(UUID id,long timestamp){
        if(!tradePlans.containsKey(id)){
            var receipt=transfers.get(id);if(receipt!=null&&!receipt.reserved())return receipt;
            throw new IllegalStateException("Trade has no verified item plan");
        }
        return commitTransfer(id,transfers.get(id).terms(),timestamp);
    }
    public void acknowledgeTrade(UUID id,UUID owner){
        var plan=tradePlans.get(id);if(plan==null||transfers.get(id).reserved())throw new IllegalStateException("Trade is undecided");
        var next=plan.acknowledge(owner);tradeByOwner.remove(owner,id);
        // Terminal item evidence is retained in the account outbox or verified ledger pages.
        if(next.firstAck()&&next.secondAck())tradePlans.remove(id);else tradePlans.put(id,next);setDirty();
    }
    private void initializeLedger(){
        if(ledger.isEmpty()){
            java.math.BigInteger baseline=java.math.BigInteger.ZERO;for(long amount:balanceByPlayer.values())baseline=baseline.add(java.math.BigInteger.valueOf(amount));
            ledger.putInt("schema",1);ledger.putLong("sequence",0);ledger.putString("baseline",baseline.toString());
            ledger.putString("generation","0");ledger.putString("sink","0");ledger.putString("administrative","0");ledger.putString("transfer","0");ledger.put("outbox",new CompoundTag());setDirty();
        }
        if(ledger.getIntOr("schema",0)!=1||ledger.getLongOr("sequence",-1)<0||!(ledger.get("outbox") instanceof CompoundTag))throw new IllegalArgumentException("Ledger requires schema review");
        for(String key:List.of("baseline","generation","sink","administrative","transfer"))new java.math.BigInteger(ledger.getStringOr(key,""));
    }
    private void validateLedger(){
        initializeLedger();var outbox=ledger.getCompoundOrEmpty("outbox");
        if(outbox.size()>4096)throw new IllegalArgumentException("Excess ledger backlog");
        for(String key:outbox.keySet()){
            long id=Long.parseLong(key);if(id<1||id>ledger.getLongOr("sequence",0)||!key.equals(Long.toString(id))||!(outbox.get(key) instanceof CompoundTag row))throw new IllegalArgumentException("Invalid ledger sequence");
            EconomyLedger.validateRow(row);
        }
    }
    private void journal(CompoundTag row){
        initializeLedger();EconomyLedger.validateRow(row);var outbox=ledger.getCompoundOrEmpty("outbox");
        if(outbox.size()>=4096)throw new IllegalStateException("Economy ledger backlog requires recovery before more transactions");
        long sequence=Math.addExact(ledger.getLongOr("sequence",0),1);
        long delta=Math.subtractExact(row.getLongOr("after",0),row.getLongOr("before",0));
        String category=row.getStringOr("category","");long transferred=row.getStringOr("type","").equals("death_debit")?-delta:Math.max(0,delta);
        if(category.equals("transfer")&&row.getStringOr("status","").equals(AccountTransfer.COMMITTED)){
            var detail=row.getCompoundOrEmpty("details");
            if(detail.contains("first_pays"))transferred=detail.getStringOr("first_owner","").equals(row.getStringOr("player",""))
                    ?detail.getLongOr("second_pays",0):detail.getLongOr("first_pays",0);
        }
        long adjustment=switch(category){case "sink" -> row.getStringOr("type","").equals("death_despawn")
                ?row.getCompoundOrEmpty("details").getLongOr("drop_destroyed_trace",0):-delta;case "transfer" -> transferred;default -> delta;};
        var total=new java.math.BigInteger(ledger.getStringOr(category,"0")).add(java.math.BigInteger.valueOf(adjustment));
        var days=EconomyReports.daily(ledger.getCompoundOrEmpty("recent_days"),row,adjustment);
        var reviews=EconomyReports.finalReviews(ledger.getCompoundOrEmpty("final_reviews"),row,D1EconomyConfig.WEALTH_MAX.get());
        // Compute everything that can reject before changing either balance or authoritative outbox.
        outbox.put(Long.toString(sequence),row.copy());ledger.putLong("sequence",sequence);ledger.put("outbox",outbox);
        ledger.putString(category,total.toString());ledger.put("recent_days",days);ledger.put("final_reviews",reviews);
        if(row.getStringOr("status","").equals(AccountTransfer.COMMITTED)){
            var last=ledger.getCompoundOrEmpty("last_transactions");last.putString(row.getStringOr("player",""),row.getStringOr("transaction",""));ledger.put("last_transactions",last);
        }
        setDirty();
    }
    private void operationJournal(UUID id,AccountOperation op){
        var details=op.plan();if(op.status().equals(AccountTransfer.REJECTED)&&op.delta()>0)details.putLong("cap_rejected_trace",op.delta());
        journal(EconomyLedger.row(id.toString(),op.owner(),accountName(op.owner()),op.kind(),op.delta(),op.before(),op.after(),op.related(),op.run(),op.status(),op.timestamp(),details));
    }
    private void loadOperation(UUID id,AccountOperation op){
        if(!op.acknowledged()){
            net.goui.cosmicdungeon.vendor.CommerceCustodyImages.validate(op.plan(),op.owner());
            if(!id.toString().equals(op.plan().getStringOr("transaction",""))||op.run()!=op.plan().getLongOr("run",-1))throw new IllegalArgumentException("Unbound account item operation");
            if(operationByOwner.putIfAbsent(op.owner(),id)!=null||repairByOwner.containsKey(op.owner())||tradeByOwner.containsKey(op.owner()))throw new IllegalStateException("Multiple owner transactions");
        }
        operations.put(id,op);operationHolds(op,1);
    }
    private void operationHolds(AccountOperation op,int sign){if(op.reserved())adjustHold(op.delta()<0?heldDebit:heldCredit,op.owner(),Math.abs(op.delta())*sign);}
    private void putOperation(UUID id,AccountOperation op){var old=operations.get(id);if(old!=null)operationHolds(old,-1);operations.put(id,op);operationHolds(op,1);setDirty();}
    public Optional<AccountOperation> operation(UUID id){return Optional.ofNullable(operations.get(id));}
    public Optional<UUID> pendingOperation(UUID owner){return Optional.ofNullable(operationByOwner.get(owner));}
    public AccountOperation reserveOperation(UUID id,UUID owner,long delta,String kind,String related,long run,CompoundTag plan,long now){
        var old=operations.get(id);if(old!=null){if(!old.owner().equals(owner)||old.delta()!=delta||!old.kind().equals(kind)||!old.related().equals(related)||old.run()!=run||(!old.acknowledged()&&!old.plan().equals(plan)))throw new IllegalArgumentException("Operation ID reused with changed terms");return old;}
        requireDeathReady(owner);
        net.goui.cosmicdungeon.vendor.CommerceCustodyImages.validate(plan,owner);
        if(!id.toString().equals(plan.getStringOr("transaction",""))||run!=plan.getLongOr("run",-1))throw new IllegalArgumentException("Wrong operation plan");
        if(operationByOwner.containsKey(owner)||repairByOwner.containsKey(owner)||tradeByOwner.containsKey(owner))throw new IllegalStateException("Prior item transaction awaits recovery");
        if(delta==Long.MIN_VALUE)throw new IllegalArgumentException("Invalid debit");
        long balance=getBalanceTrace(owner);boolean fits=delta<0?availableTrace(owner)>=-delta:availableCapacity(owner)>=delta;
        var op=new AccountOperation(owner,delta,kind,related,run,fits?AccountTransfer.RESERVED:AccountTransfer.REJECTED,now,balance,balance,plan,false,false);
        operationJournal(id,op);putOperation(id,op);operationByOwner.put(owner,id);return op;
    }
    public AccountOperation prepareOperation(UUID id){var op=operations.get(id);if(op==null)throw new IllegalStateException("Missing operation");var next=op.armed();putOperation(id,next);return next;}
    public AccountOperation decideOperation(UUID id,boolean commit,long now){
        var old=operations.get(id);if(old==null)throw new IllegalStateException("Missing operation");if(!old.reserved())return old;
        if(commit)requireDeathReady(old.owner());
        boolean fits=old.delta()<0?getBalanceTrace(old.owner())>=reservedDebit(old.owner()):old.delta()==0||creditFits(old.owner());
        if(commit&&(!old.prepared()||!fits))throw new IllegalStateException("Operation not prepared or reservation no longer valid");
        var next=old.decide(commit?AccountTransfer.COMMITTED:AccountTransfer.CANCELLED,getBalanceTrace(old.owner()),now);
        operationJournal(id,next);putOperation(id,next);if(commit)balanceByPlayer.put(old.owner(),next.after());setDirty();return next;
    }
    public void acknowledgeOperation(UUID id,UUID owner){
        var op=operations.get(id);if(op==null||!op.owner().equals(owner))throw new IllegalArgumentException("Foreign operation acknowledgement");
        putOperation(id,op.acknowledge());operationByOwner.remove(owner,id);setDirty();
    }
    public boolean ledgerPending(){return !ledger.getCompoundOrEmpty("outbox").isEmpty();}
    public CompoundTag finalReviews(){initializeLedger();return ledger.getCompoundOrEmpty("final_reviews").copy();}
    public boolean claimDailyReport(long day){initializeLedger();if(ledger.getLongOr("report_day",Long.MIN_VALUE)>=day)return false;ledger.putLong("report_day",day);setDirty();return true;}
    public CompoundTag supplySnapshot(){initializeLedger();var result=new CompoundTag();
        for(String key:ledger.keySet())if(!Set.of("outbox","last_transactions","final_reviews").contains(key))result.put(key,ledger.get(key).copy());
        result.putInt("final_review_count",ledger.getCompoundOrEmpty("final_reviews").size());result.remove("final_reviews");
        java.math.BigInteger balances=java.math.BigInteger.ZERO;for(long amount:balanceByPlayer.values())balances=balances.add(java.math.BigInteger.valueOf(amount));
        var expected=new java.math.BigInteger(ledger.getStringOr("baseline","0")).add(new java.math.BigInteger(ledger.getStringOr("generation","0"))).subtract(new java.math.BigInteger(ledger.getStringOr("sink","0"))).add(new java.math.BigInteger(ledger.getStringOr("administrative","0")));
        var amounts=balanceByPlayer.values().stream().sorted().toList();
        for(int percentile:List.of(50,90,99)){
            int index=amounts.isEmpty()?0:(int)Math.ceil(percentile/100.0*amounts.size())-1;
            result.putLong("p"+percentile,amounts.isEmpty()?0:amounts.get(Math.max(0,index)));
        }
        result.putDouble("median_trace",amounts.isEmpty()?0.0:(amounts.size()%2==1?amounts.get(amounts.size()/2)
                :amounts.get(amounts.size()/2-1)/2.0+amounts.get(amounts.size()/2)/2.0));
        result.putInt("accounts",balanceByPlayer.size());result.putString("balances",balances.toString());result.putString("expected",expected.toString());result.putString("active_death_drops",deaths.activeSupply().toString());result.putInt("pending_deaths",deaths.pending.size());
        result.putString("total_supply",balances.add(deaths.activeSupply()).toString());
        result.putString("difference",balances.add(deaths.activeSupply()).subtract(expected).toString());return result;
    }
    public long getBalanceTrace(UUID playerId) {
        if (playerId == null) return 0L;
        return Math.max(0L, balanceByPlayer.getOrDefault(playerId, 0L));
    }

    public long getCapacityTrace(UUID playerId) {
        if (playerId == null) return net.goui.cosmicdungeon.Config.ACCOUNT_CAPACITY.get();
        Long override=capacityOverrideByPlayer.get(playerId);
        return override!=null?Math.max(0L,override):net.goui.cosmicdungeon.Config.ACCOUNT_CAPACITY.get();
    }

    public void setBalanceTrace(UUID playerId, long traceAmount) {
        if (playerId == null) return;
        requireDeathReady(playerId);
        long clamped = Math.max(0L, traceAmount);
        if(clamped<reservedDebit(playerId))throw new IllegalStateException("Balance is reserved by an active transaction");
        long previousBalance=getBalanceTrace(playerId);
        if(reservedCredit(playerId)>0&&clamped>previousBalance&&clamped-previousBalance>availableCapacity(playerId))
            throw new IllegalStateException("Account receiving capacity is reserved or exceeded");
        if(previousBalance!=clamped)journal(EconomyLedger.row(UUID.randomUUID().toString(),playerId,accountName(playerId),"admin_adjustment",clamped-previousBalance,previousBalance,clamped,"",0,"committed",System.currentTimeMillis(),new CompoundTag()));
        // Capacity gates deposits; changing configuration must never truncate existing balances.

        if (clamped == 0L) {
            if (balanceByPlayer.remove(playerId) != null) setDirty();
            return;
        }

        balanceByPlayer.put(playerId, clamped);
        setDirty();
    }

    public void setCapacityTrace(UUID playerId, long capacityTrace) {
        if (playerId == null) return;
        long clamped = Math.max(0L, capacityTrace);
        // An explicit administrator override stays explicit even when it equals today's default.
        capacityOverrideByPlayer.put(playerId, clamped);
        setDirty();
    }

    public boolean canDeposit(UUID playerId, long traceAmount) {
        if (playerId == null || traceAmount < 0L) return false;
        return traceAmount<=availableCapacity(playerId);
    }

    public boolean tryDeposit(UUID playerId, long traceAmount) {
        return traceAmount>=0&&change(playerId,"",traceAmount,"legacy_credit","",0,UUID.randomUUID().toString(),false)>=0;
    }
    public boolean tryWithdraw(UUID playerId, long traceAmount) {
        return traceAmount>=0&&change(playerId,"",-traceAmount,"legacy_debit","",0,UUID.randomUUID().toString(),false)>=0;
    }
    /** Returns absolute amount committed, or -1 on rejection. All callers run on the server thread. */
    public long change(UUID playerId,String name,long delta,String type,String related,long run,String tx,boolean partial){
        if(playerId==null||tx==null||delta==Long.MIN_VALUE||deathBlocked(playerId))return -1;
        String key=tx+"|"+playerId;
        if(receipts.containsKey(key))return receipts.get(key);
        long before=getBalanceTrace(playerId),amount=delta<0?-delta:delta,rejected=0,after=before;
        if(delta>=0){
            long room=availableCapacity(playerId);
            if(amount>room){
                if(!partial){var denied=new CompoundTag();denied.putLong("cap_rejected_trace",amount);journal(EconomyLedger.row(tx,playerId,name,type,delta,before,before,related,run,"rejected_capacity",System.currentTimeMillis(),denied));CurrencyAudit.report(server,playerId,name,tx,type,delta,before,before,amount,related,run,"rejected_capacity");return -1;}
                rejected=amount-room;amount=room;
            }
            after=before+amount;
        }else{
            if(availableTrace(playerId)<amount){journal(EconomyLedger.row(tx,playerId,name,type,delta,before,before,related,run,"rejected_balance",System.currentTimeMillis(),new CompoundTag()));CurrencyAudit.report(server,playerId,name,tx,type,delta,before,before,0,related,run,"rejected_balance");return -1;}
            after=before-amount;
        }
        var details=new CompoundTag();details.putLong("cap_rejected_trace",rejected);
        journal(EconomyLedger.row(tx,playerId,name,type,delta,before,after,related,run,"committed",System.currentTimeMillis(),details));
        // Receipt and account share one SavedData payload, so ordinary save/restart cannot replay a reward.
        balanceByPlayer.put(playerId,after);
        if(tx.startsWith("mob:")||tx.startsWith("inn:")||tx.equals("first_trace"))receipts.put(key,amount);
        setDirty();
        CurrencyAudit.report(server,playerId,name,tx,type,delta,before,after,rejected,related,run,"committed");
        return amount;
    }
    public boolean hasReceipt(String transaction,UUID player){return receipts.containsKey(transaction+"|"+player);}
    public boolean markThreshold(UUID player,long threshold){
        boolean changed=wealthCrossings.add(player+"|"+threshold);if(changed)setDirty();return changed;
    }
    /** Drop finished-run receipt details after the run is removed; full structured evidence remains in the account outbox or verified ledger pages. */
    public void clearRunReceipts(long run){
        if(receipts.keySet().removeIf(key->key.startsWith("mob:"+run+":")))setDirty();
    }
    // TODO(M03, remaining legacy boundaries): Economy Internal (2026-08-18),
    // 17ufIuIy0VhLmB_V-6sZ7sCaUCZuGZUkHrgJLVpEcS28. Merchant/repair/trade owner receipts
    // and paged native item ledger now exist. Verify native interrupted saves on licensed TEST;
    // migrate old physical denomination pickups and Inn/travel entitlement boundaries separately.
    // Never infer uniqueness from a partial manually restored player/account/world backup.
    // TODO(M08, licensed TEST verification): the 2026-08-18 Economy Internal death protocol is
    // implemented in this account image and DeathCurrencyService. Exercise native interrupted saves,
    // simultaneous pickup, portal/chunk reload, item-expiry hooks, and full run reset on TEST.
    // Restore playerdata, account/ledger, and dimensions together; partial manual restores are unsafe.

    public void clear(UUID playerId) {
        if (playerId == null) return;
        requireDeathReady(playerId);
        if(reservedDebit(playerId)>0||reservedCredit(playerId)>0)
            throw new IllegalStateException("Account has active transaction reservations");
        long before=getBalanceTrace(playerId);
        if(before!=0)journal(EconomyLedger.row(UUID.randomUUID().toString(),playerId,accountName(playerId),"admin_adjustment",-before,before,0,"",0,"committed",System.currentTimeMillis(),new CompoundTag()));
        boolean removed = balanceByPlayer.remove(playerId) != null;
        removed |= capacityOverrideByPlayer.remove(playerId) != null;
        if (removed) setDirty();
    }
}
