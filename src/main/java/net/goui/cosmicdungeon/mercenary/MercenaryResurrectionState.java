package net.goui.cosmicdungeon.mercenary;

import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.UUID;
import net.goui.cosmicdungeon.dungeon.d1.D1RunData;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

/** Bounded values in existing run storage: one latest death per member and one timer per contract. */
public final class MercenaryResurrectionState {
    public static final int UNLOCK_LEVEL=10, COOLDOWN_TICKS=20*180, PROTECTION_TICKS=20*5;
    private MercenaryResurrectionState(){}
    public record Death(UUID id,String dimension,Vec3 position,float yaw,float pitch){
        public Death{
            if(id==null||dimension==null||dimension.length()>128||ResourceLocation.tryParse(dimension)==null
                    ||position==null||!Double.isFinite(position.x)||!Double.isFinite(position.y)||!Double.isFinite(position.z)
                    ||Math.abs(position.x)>30_000_000||Math.abs(position.y)>30_000_000||Math.abs(position.z)>30_000_000
                    ||!Float.isFinite(yaw)||!Float.isFinite(pitch))throw new IllegalArgumentException("Invalid death location");
        }
        public static final Codec<Death> CODEC=RecordCodecBuilder.create(i->i.group(
                UUIDUtil.CODEC.fieldOf("id").forGetter(Death::id),
                Codec.STRING.fieldOf("dimension").forGetter(Death::dimension),
                Vec3.CODEC.fieldOf("position").forGetter(Death::position),
                Codec.FLOAT.fieldOf("yaw").forGetter(Death::yaw),
                Codec.FLOAT.fieldOf("pitch").forGetter(Death::pitch)
        ).apply(i,Death::new));
    }
    static String deathKey(UUID player){return "mercenary_resurrection_death:"+player;}
    static String cooldownKey(UUID mercenary){return "mercenary_resurrection_ready:"+mercenary;}
    static String encode(Death death){return Death.CODEC.encodeStart(JsonOps.INSTANCE,death).getOrThrow().toString();}
    static Death decode(String value){
        if(value==null||value.length()>1024)return null;
        try{return Death.CODEC.parse(JsonOps.INSTANCE,JsonParser.parseString(value)).result().orElse(null);}
        catch(RuntimeException ignored){return null;}
    }
    public static Death death(D1RunData data,long run,UUID player){
        var values=data.values(run,deathKey(player));
        return values.size()==1?decode(values.getFirst()):null;
    }
    static void remember(D1RunData data,long run,UUID player,Death death){
        data.setValue(run,deathKey(player),encode(death));
    }
    static void clearDeath(D1RunData data,long run,UUID player){data.setValue(run,deathKey(player),null);}
    static long readyAt(D1RunData data,long run,UUID mercenary){
        var values=data.values(run,cooldownKey(mercenary));
        if(values.isEmpty())return 0; // Existing worlds and newly instantiated runs start ready.
        if(values.size()!=1)return Long.MAX_VALUE;
        try{long value=Long.parseLong(values.getFirst());return value>=0?value:Long.MAX_VALUE;}
        catch(NumberFormatException ignored){return Long.MAX_VALUE;}
    }
    public static int seconds(long deadline,long now){
        if(deadline<=now)return 0;
        return (int)((Math.min((long)COOLDOWN_TICKS,deadline-Math.max(0,now))+19)/20);
    }
    static boolean protectedAt(long deadline,long now){
        return now>=0&&deadline>now&&deadline-now<=PROTECTION_TICKS;
    }
    /** Consume before native respawn; an uncertain save or failed respawn never permits a duplicate attempt. */
    static boolean consume(D1RunData data,long run,UUID player,UUID death,UUID mercenary,long now){
        var current=death(data,run,player);
        if(current==null||!current.id().equals(death)||readyAt(data,run,mercenary)>now
                ||now<0||now>Long.MAX_VALUE-COOLDOWN_TICKS)return false;
        clearDeath(data,run,player);
        data.setValue(run,cooldownKey(mercenary),Long.toString(now+COOLDOWN_TICKS));
        return true;
    }
}
