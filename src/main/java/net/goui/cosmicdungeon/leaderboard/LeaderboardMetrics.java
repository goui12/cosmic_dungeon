package net.goui.cosmicdungeon.leaderboard;
import net.goui.cosmicdungeon.network.LeaderboardPayloads.Metric;
import net.goui.cosmicdungeon.mixin.StatsCounterAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.stats.*;
import net.minecraft.resources.ResourceLocation;
import java.util.*;
import java.util.function.ToLongFunction;
public final class LeaderboardMetrics{
    public static final String DEFAULT="legacy|completions";
    public static final String TRAVEL="summary|blocks_traveled";
    public static final Map<String,String> CUSTOM=Map.of(
        "doors_unlocked","Doors unlocked (since update)","hostile_kills","Attributed hostile kills (since update)",
        "lesser_harvests","D1 Lesser Blooms harvested (since update)","logins","Logins (since update)",
        "dimension_changes","Dimension changes (since update)");
    public static final Map<String,String> LEGACY=Map.of("spectral_blooms","D1 recovered Spectral Blooms",
        "lesser_blooms","D1 retained Lesser Blooms","completions","Dungeons completed",
        "successful_kills","D1 kills in successful runs");
    private static final List<ResourceLocation> TRAVEL_STATS=List.of(
        Stats.WALK_ONE_CM,Stats.CROUCH_ONE_CM,Stats.SPRINT_ONE_CM,Stats.WALK_ON_WATER_ONE_CM,
        Stats.FALL_ONE_CM,Stats.CLIMB_ONE_CM,Stats.FLY_ONE_CM,Stats.WALK_UNDER_WATER_ONE_CM,
        Stats.MINECART_ONE_CM,Stats.BOAT_ONE_CM,Stats.PIG_ONE_CM,Stats.HAPPY_GHAST_ONE_CM,
        Stats.HORSE_ONE_CM,Stats.AVIATE_ONE_CM,Stats.SWIM_ONE_CM,Stats.STRIDER_ONE_CM);
    private LeaderboardMetrics(){}
    /** This curated list is also the server's metric allowlist. */
    public static List<Metric> catalog(){
        return List.of(
            new Metric(DEFAULT,"Dungeons completed"),
            new Metric("minecraft:mined|cosmicdungeon:cosmic_mob_spawner","Cosmic mob spawners broken"),
            new Metric("minecraft:custom|minecraft:mob_kills","Mobs killed"),
            new Metric("minecraft:custom|minecraft:deaths","Death count"),
            new Metric(TRAVEL,"Blocks traveled"),
            new Metric("cosmic|doors_unlocked","Doors unlocked"),
            new Metric("cosmic|lesser_harvests","Lesser Blooms harvested"),
            new Metric("minecraft:custom|minecraft:play_time","Time played"));
    }
    /** Add native centimeters across movement modes, then floor once to whole blocks. */
    static long traveledBlocks(ToLongFunction<ResourceLocation> count){
        long blocks=0;int centimeters=0;
        for(var stat:TRAVEL_STATS){
            long value=Math.max(0,count.applyAsLong(stat)),whole=value/100;
            centimeters+=(int)(value%100);
            if(centimeters>=100){whole++;centimeters-=100;}
            if(blocks>Long.MAX_VALUE-whole)return Long.MAX_VALUE;
            blocks+=whole;
        }
        return blocks;
    }
    @SuppressWarnings({"rawtypes","unchecked"})
    public static String format(String key,long value){
        String[] parts=key.split("\\|",-1);
        if(parts.length==2&&!parts[0].equals("total")&&value<=Integer.MAX_VALUE){
            var id=ResourceLocation.tryParse(parts[0]);
            var type=id==null?null:BuiltInRegistries.STAT_TYPE.getOptional(id).orElse(null);
            var entryId=ResourceLocation.tryParse(parts[1]);
            var entry=type==null||entryId==null?null:type.getRegistry().getOptional(entryId).orElse(null);
            if(entry!=null)return ((StatType)type).get(entry).format((int)value);
        }
        return String.format(Locale.ROOT,"%,d",value);
    }
    @SuppressWarnings({"rawtypes","unchecked"})
    public static long nativeValue(StatsCounter counter,String key){
        if(key.equals(TRAVEL))return traveledBlocks(stat->counter.getValue(Stats.CUSTOM.get(stat)));
        String[] parts=key.split("\\|",-1);
        if(parts.length!=2)return 0;
        boolean total=parts[0].equals("total");String typeId=total?parts[1]:parts[0];
        var type=BuiltInRegistries.STAT_TYPE.getOptional(ResourceLocation.parse(typeId)).orElse(null);
        if(type==null)return 0;
        if(!total){
            var value=type.getRegistry().getOptional(ResourceLocation.parse(parts[1])).orElse(null);
            return value==null?0:Math.max(0,counter.getValue((StatType)type,value));
        }
        long sum=0;var values=((StatsCounterAccess)counter).cosmicdungeon$stats();
        synchronized(values){for(var entry:values.object2IntEntrySet())if(entry.getKey().getType()==type)sum+=Math.max(0,entry.getIntValue());}
        return sum;
    }
}
