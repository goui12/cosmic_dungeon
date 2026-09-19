package net.goui.cosmicdungeon.npc.inn;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.*;
import java.util.*;
/** A configured region approves its existing beds; no world or schematic is rewritten. */
public final class InnData extends SavedData {
    private static final Codec<InnData> CODEC=RecordCodecBuilder.create(i->i.group(
        Codec.STRING.optionalFieldOf("dimension","").forGetter(d->d.dimension),
        BlockPos.CODEC.optionalFieldOf("minimum",BlockPos.ZERO).forGetter(d->d.minimum),
        BlockPos.CODEC.optionalFieldOf("maximum",BlockPos.ZERO).forGetter(d->d.maximum),
        Codec.unboundedMap(Codec.STRING,BlockPos.CODEC).optionalFieldOf("beds",Map.of()).forGetter(d->d.beds)
    ).apply(i,InnData::new));
    private static final SavedDataType<InnData> TYPE=new SavedDataType<>("cosmicdungeon_inn_v1",InnData::new,CODEC);
    private String dimension="";private BlockPos minimum=BlockPos.ZERO,maximum=BlockPos.ZERO;
    private final Map<String,BlockPos> beds=new HashMap<>();
    private InnData(){}
    private InnData(String dimension,BlockPos minimum,BlockPos maximum,Map<String,BlockPos> beds){
        this.dimension=dimension;this.minimum=minimum;this.maximum=maximum;this.beds.putAll(beds);
    }
    public static InnData get(MinecraftServer server){return server.overworld().getDataStorage().computeIfAbsent(TYPE);}
    public boolean contains(ServerLevel level,BlockPos pos){
        return dimension.equals(level.dimension().location().toString())&&pos.getX()>=minimum.getX()&&pos.getX()<=maximum.getX()
                &&pos.getY()>=minimum.getY()&&pos.getY()<=maximum.getY()&&pos.getZ()>=minimum.getZ()&&pos.getZ()<=maximum.getZ();
    }
    public void region(ServerLevel level,BlockPos a,BlockPos b){
        String next=level.dimension().location().toString();
        if(!dimension.equals(next))beds.clear();
        dimension=next;minimum=new BlockPos(Math.min(a.getX(),b.getX()),Math.min(a.getY(),b.getY()),Math.min(a.getZ(),b.getZ()));
        maximum=new BlockPos(Math.max(a.getX(),b.getX()),Math.max(a.getY(),b.getY()),Math.max(a.getZ(),b.getZ()));setDirty();
    }
    public String dimension(){return dimension;}
    public BlockPos bed(UUID player){return beds.get(player.toString());}
    public void bed(UUID player,BlockPos pos){beds.put(player.toString(),pos.immutable());setDirty();}
}
