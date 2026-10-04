package net.goui.cosmicdungeon.leaderboard;
import net.goui.cosmicdungeon.network.LeaderboardPayloads.Metric;
import net.goui.cosmicdungeon.mixin.StatsCounterAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.stats.*;
import net.minecraft.resources.ResourceLocation;
import java.util.*;
public final class LeaderboardMetrics{
    public static final String DEFAULT="minecraft:custom|minecraft:mob_kills";
    public static final Map<String,String> CUSTOM=Map.of(
        "doors_unlocked","Doors unlocked (since update)","hostile_kills","Attributed hostile kills (since update)",
        "lesser_harvests","D1 Lesser Blooms harvested (since update)","logins","Logins (since update)",
        "dimension_changes","Dimension changes (since update)");
    public static final Map<String,String> LEGACY=Map.of("spectral_blooms","D1 recovered Spectral Blooms",
        "lesser_blooms","D1 retained Lesser Blooms","completions","D1 completions",
        "successful_kills","D1 kills in successful runs");
    private LeaderboardMetrics(){}
    public static String words(String raw){
        String s=raw.substring(raw.indexOf(':')+1).replace('_',' ').replace('/',' ');
        return s.isEmpty()?s:Character.toUpperCase(s.charAt(0))+s.substring(1);
    }
    public static List<Metric> catalog(){
        List<Metric> result=new ArrayList<>();
        CUSTOM.forEach((key,label)->result.add(new Metric("cosmic|"+key,label)));
        LEGACY.forEach((key,label)->result.add(new Metric("legacy|"+key,label)));
        for(var type:BuiltInRegistries.STAT_TYPE){
            String id=BuiltInRegistries.STAT_TYPE.getKey(type).toString();
            if(type!=Stats.CUSTOM)result.add(new Metric("total|"+id,"Total "+words(id).toLowerCase(Locale.ROOT)));
            for(var value:type.getRegistry().keySet()){
                String label=type==Stats.CUSTOM?words(value.toString()):words(id)+" / "+words(value.toString());
                if(!value.getNamespace().equals("minecraft"))label+=" ["+value.getNamespace()+"]";
                if(id.equals("minecraft:mined")&&value.toString().equals("cosmicdungeon:cosmic_mob_spawner"))label="Cosmic Spawners destroyed";
                String key=id+"|"+value;
                if(key.length()<=160)result.add(new Metric(key,label.substring(0,Math.min(160,label.length()))));
            }
        }
        result.sort(Comparator.comparing(Metric::label,String.CASE_INSENSITIVE_ORDER).thenComparing(Metric::key));
        return List.copyOf(result);
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
