package net.goui.cosmicdungeon.mercenary;
import java.util.*;
import net.goui.cosmicdungeon.block.entity.ClassLockedChestBlockEntity;
import net.goui.cosmicdungeon.dungeon.*;
import net.goui.cosmicdungeon.economy.*;
import net.goui.cosmicdungeon.entity.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
/** Synchronous startup work only. No scans, tick AI, or external item generation. */
public final class MercenaryEntry {
    private MercenaryEntry(){}
    public static boolean affordable(MinecraftServer server,List<MercenaryContract> hires){
        for(var hire:hires){
            var owner=server.getPlayerList().getPlayer(hire.hirer());
            if(owner==null||!CurrencyService.transactionsAllowed(owner)||CurrencyService.getAvailableTrace(owner)<hire.fee())return false;
        }
        return true;
    }
    public static boolean reserve(MinecraftServer server,long run,List<MercenaryContract> hires){
        if(hires.isEmpty())return true;
        var registry=DungeonRunRegistryData.get(server);
        if(!registry.attachMercenaries(run,hires))return false;
        var charges=new LinkedHashMap<UUID,Long>();hires.forEach(h->charges.put(h.hirer(),h.fee()));
        var accounts=PlayerCurrencyData.get(server);
        return accounts.reserveMercenaryFees(run,charges)&&accounts.flushVerified();
    }
    public static boolean spawn(ServerLevel level,long run,MercenaryContract hire,BlockPos destination,
            Map<UUID,List<BlockPos>> starterChests){
        var positions=starterChests.getOrDefault(hire.id(),List.of());
        if(positions.isEmpty())return false;
        var chests=new ArrayList<ClassLockedChestBlockEntity>();
        for(var pos:positions){
            if(!(level.getBlockEntity(pos) instanceof ClassLockedChestBlockEntity chest)||!chest.ownedBy(hire.id()))return false;
            if(!(chest.getBlockState().getBlock() instanceof net.goui.cosmicdungeon.block.custom.ClassLocked locked)
                    ||!hire.classId().equals(locked.requiredClassId()))return false;
            chests.add(chest);
        }
        var entity=ModEntities.MERCENARY.get().create(level,EntitySpawnReason.TRIGGERED);
        if(entity==null)return false;
        entity.initialize(run,hire);entity.setPos(destination.getX()+.5,destination.getY(),destination.getZ()+.5);
        if(!MercenaryEquipment.equip(entity,chests))return false;
        return level.addFreshEntity(entity);
    }
    public static void retryFees(MinecraftServer server){
        var accounts=PlayerCurrencyData.get(server);
        var pending=accounts.pendingMercenaryFeeRuns();
        if(pending.isEmpty())return;
        var runs=DungeonRunRegistryData.get(server);
        boolean changed=false;
        for(long run:pending){
            if(runs.starting(run))continue;
            var record=runs.getRun(run).orElse(null);
            if(record==null)continue; // Missing evidence needs startup recovery.
            try{accounts.settleMercenaryFees(run,true);changed=true;}
            catch(RuntimeException failure){return;} // Keep the existing reservation and retry without hot-loop logs.
        }
        if(changed)accounts.flushVerified();
    }
    /** Before startup recovery removes markers: a missing commit receipt means no payment. */
    public static void recoverFees(MinecraftServer server){
        var accounts=PlayerCurrencyData.get(server);
        var runs=DungeonRunRegistryData.get(server);
        var pending=accounts.pendingMercenaryFeeRuns();
        for(long run:pending){
            var record=runs.getRun(run).orElse(null);
            // A missing run is not proof of successful entry; retain a safe cancellation.
            accounts.settleMercenaryFees(run,record!=null&&!runs.starting(run));
        }
        if(!pending.isEmpty()&&!accounts.flushVerified())
            throw new IllegalStateException("Mercenary payment recovery could not be verified");
    }
}
