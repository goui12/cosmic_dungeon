package net.goui.cosmicdungeon.npc.inn;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.goui.cosmicdungeon.transaction.SavedDataProof;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.*;
import java.util.*;

/** A configured region approves existing beds; changing it never deletes old personal bindings. */
public final class InnData extends SavedData {
    private static final String ID="cosmicdungeon_inn_v1";
    private static final Codec<InnData> CODEC=RecordCodecBuilder.create(i->i.group(
        Codec.STRING.optionalFieldOf("dimension","").forGetter(d->d.dimension),
        BlockPos.CODEC.optionalFieldOf("minimum",BlockPos.ZERO).forGetter(d->d.minimum),
        BlockPos.CODEC.optionalFieldOf("maximum",BlockPos.ZERO).forGetter(d->d.maximum),
        Codec.unboundedMap(Codec.STRING,BlockPos.CODEC).optionalFieldOf("beds",Map.of()).forGetter(d->d.beds),
        Codec.unboundedMap(Codec.STRING,Codec.STRING).optionalFieldOf("bed_dimensions",Map.of()).forGetter(d->d.bedDimensions)
    ).apply(i,InnData::new));
    private static final SavedDataType<InnData> TYPE=new SavedDataType<>(ID,InnData::new,CODEC);
    private String dimension="";private BlockPos minimum=BlockPos.ZERO,maximum=BlockPos.ZERO;
    private final Map<String,BlockPos> beds=new HashMap<>();
    private final Map<String,String> bedDimensions=new HashMap<>();
    private MinecraftServer server;
    private boolean unverified;
    private InnData(){}
    private InnData(String dimension,BlockPos minimum,BlockPos maximum,Map<String,BlockPos> beds,Map<String,String> dimensions){
        if(!dimension.isEmpty()&&net.minecraft.resources.ResourceLocation.tryParse(dimension)==null)
            throw new IllegalArgumentException("Invalid Inn dimension");
        if(minimum.getX()>maximum.getX()||minimum.getY()>maximum.getY()||minimum.getZ()>maximum.getZ())
            throw new IllegalArgumentException("Inverted Inn region");
        this.dimension=dimension;this.minimum=minimum;this.maximum=maximum;
        beds.forEach((owner,pos)->{UUID.fromString(owner);this.beds.put(owner,pos);
            String world=dimensions.getOrDefault(owner,dimension);
            if(net.minecraft.resources.ResourceLocation.tryParse(world)==null)throw new IllegalArgumentException("Invalid bed dimension");
            bedDimensions.put(owner,world);});
        if(!beds.keySet().containsAll(dimensions.keySet()))throw new IllegalArgumentException("Orphan Inn bed dimension");
    }
    public static InnData get(MinecraftServer server){
        SavedDataProof.validate(server,ID,CODEC);
        var data=server.overworld().getDataStorage().computeIfAbsent(TYPE);data.server=server;return data;
    }
    public boolean flushVerified(){unverified=!SavedDataProof.save(server,ID,CODEC,this);return !unverified;}
    public boolean ready(){return !unverified||flushVerified();}
    public boolean contains(ServerLevel level,BlockPos pos){return contains(level.dimension().location().toString(),pos);}
    public boolean contains(String world,BlockPos pos){
        return !dimension.isEmpty()&&dimension.equals(world)&&pos.getX()>=minimum.getX()&&pos.getX()<=maximum.getX()
                &&pos.getY()>=minimum.getY()&&pos.getY()<=maximum.getY()&&pos.getZ()>=minimum.getZ()&&pos.getZ()<=maximum.getZ();
    }
    public void region(ServerLevel level,BlockPos a,BlockPos b){region(level.dimension().location().toString(),a,b);}
    void region(String world,BlockPos a,BlockPos b){
        if(net.minecraft.resources.ResourceLocation.tryParse(world)==null)throw new IllegalArgumentException("Invalid Inn dimension");
        dimension=world;minimum=new BlockPos(Math.min(a.getX(),b.getX()),Math.min(a.getY(),b.getY()),Math.min(a.getZ(),b.getZ()));
        maximum=new BlockPos(Math.max(a.getX(),b.getX()),Math.max(a.getY(),b.getY()),Math.max(a.getZ(),b.getZ()));setDirty();
    }
    public String dimension(){return dimension;}
    public String bedDimension(UUID player){return bedDimensions.getOrDefault(player.toString(),"");}
    public BlockPos bed(UUID player){return beds.get(player.toString());}
    public void bed(UUID player,BlockPos pos){
        beds.put(player.toString(),pos.immutable());bedDimensions.put(player.toString(),dimension);setDirty();
    }
}
