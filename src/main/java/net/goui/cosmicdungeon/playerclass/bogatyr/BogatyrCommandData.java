package net.goui.cosmicdungeon.playerclass.bogatyr;

import com.mojang.serialization.Codec;
import java.util.*;
import net.goui.cosmicdungeon.transaction.SavedDataProof;
import net.minecraft.nbt.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.*;

/** Durable ambiguous-outcome guard. Original wolf/player images are kept until one exact debit is saved. */
public final class BogatyrCommandData extends SavedData {
    public static final String ID="cosmicdungeon_wolf_commands_v1";
    public static final Codec<BogatyrCommandData> CODEC=CompoundTag.CODEC.xmap(BogatyrCommandData::new,BogatyrCommandData::image);
    private static final SavedDataType<BogatyrCommandData> TYPE=new SavedDataType<>(ID,BogatyrCommandData::new,CODEC);
    private final CompoundTag image;
    private MinecraftServer server;
    private BogatyrCommandData(){image=new CompoundTag();image.putInt("schema",1);image.put("pending",new CompoundTag());}
    public BogatyrCommandData(CompoundTag image){
        this.image=image.copy();
        if(!(image.get("schema") instanceof IntTag)||image.getIntOr("schema",0)!=1||!(image.get("pending") instanceof CompoundTag))
            throw new IllegalArgumentException("Unsupported wolf command journal");
        for(var key:image.getCompoundOrEmpty("pending").keySet())
            validate(UUID.fromString(key),image.getCompoundOrEmpty("pending").getCompound(key).orElseThrow());
    }
    private static void validate(UUID owner,CompoundTag plan){
        UUID id=UUID.fromString(plan.getStringOr("id",""));long run=plan.getLongOr("run",0);
        var kind=net.goui.cosmicdungeon.network.BogatyrPayloads.Kind.valueOf(plan.getStringOr("kind",""));
        String phase=plan.getStringOr("phase","");int cost=plan.getIntOr("cost",-1);
        if(run<=0||!Set.of("prepared","entities_saved").contains(phase)||!(plan.get("ledger") instanceof CompoundTag)
                ||!(plan.get("wolves") instanceof ListTag)||!(plan.get("cost") instanceof IntTag)||cost<0||cost>600)
            throw new IllegalArgumentException("Invalid pending wolf command");
        var root=new CompoundTag();root.put(net.goui.cosmicdungeon.playerclass.resource.ClassResourceLedger.KEY,plan.getCompoundOrEmpty("ledger"));
        var ledger=net.goui.cosmicdungeon.playerclass.resource.ClassResourceLedger.forRun(root,run);
        if(ledger.runId()!=plan.getCompoundOrEmpty("ledger").getLongOr("run_id",0)
                ||cost>ledger.amount(net.goui.cosmicdungeon.playerclass.resource.ClassResourceKind.KIBBLE))
            throw new IllegalArgumentException("Wolf command run/budget mismatch");
        var originals=images(owner,run,plan.getListOrEmpty("wolves"),null);
        if(kind==net.goui.cosmicdungeon.network.BogatyrPayloads.Kind.SUMMON?!originals.isEmpty():originals.isEmpty())
            throw new IllegalArgumentException("Wolf command source images do not match kind");
        if(phase.equals("prepared")){
            if(cost!=0||plan.contains("outcomes"))throw new IllegalArgumentException("Unproven command outcome");
        }else{
            if(!(plan.get("outcomes") instanceof ListTag))throw new IllegalArgumentException("Missing command outcomes");
            var outcomes=images(owner,run,plan.getListOrEmpty("outcomes"),id);
            if(cost!=outcomes.size()*WolfCommandRules.unitCost(kind)
                    ||(kind==net.goui.cosmicdungeon.network.BogatyrPayloads.Kind.SUMMON?outcomes.size()>1:!originals.containsAll(outcomes)))
                throw new IllegalArgumentException("Wolf command outcome cost/identity mismatch");
        }
    }
    private static Set<UUID> images(UUID owner,long run,ListTag images,UUID receipt){
        var ids=new HashSet<UUID>();
        for(var value:images){
            if(!(value instanceof CompoundTag wolf)||!wolf.getStringOr("id","").equals("minecraft:wolf")
                    ||wolf.read("Owner",net.minecraft.core.UUIDUtil.CODEC).filter(owner::equals).isEmpty()
                    ||wolf.getCompoundOrEmpty("NeoForgeData").getLongOr(BogatyrWolfEvents.RUN,0)!=run
                    ||!Float.isFinite(wolf.getFloatOr("Health",0))||wolf.getFloatOr("Health",0)<=0)
                throw new IllegalArgumentException("Invalid command wolf image");
            UUID uuid=wolf.read("UUID",net.minecraft.core.UUIDUtil.CODEC).orElseThrow();
            if(!ids.add(uuid)||receipt!=null&&!wolf.getCompoundOrEmpty("NeoForgeData").getStringOr(BogatyrCommands.RECEIPT,"").equals(receipt.toString()))
                throw new IllegalArgumentException("Invalid wolf command receipt");
        }
        return ids;
    }
    public static BogatyrCommandData get(MinecraftServer server){
        SavedDataProof.validate(server,ID,CODEC);
        var data=server.overworld().getDataStorage().computeIfAbsent(TYPE);data.server=server;return data;
    }
    public CompoundTag image(){return image.copy();}
    public CompoundTag pending(UUID owner){return image.getCompoundOrEmpty("pending").getCompound(owner.toString()).map(CompoundTag::copy).orElse(null);}
    public void put(UUID owner,CompoundTag plan){
        validate(owner,plan);
        var pending=image.getCompoundOrEmpty("pending").copy();pending.put(owner.toString(),plan.copy());image.put("pending",pending);setDirty();
    }
    public void remove(UUID owner){image.getCompoundOrEmpty("pending").remove(owner.toString());setDirty();}
    public boolean flushVerified(){return SavedDataProof.save(server,ID,CODEC,this);}
}
