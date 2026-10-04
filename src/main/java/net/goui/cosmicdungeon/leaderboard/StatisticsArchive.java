package net.goui.cosmicdungeon.leaderboard;
import com.google.gson.*;
import com.mojang.serialization.*;
import com.mojang.datafixers.DataFixer;
import net.minecraft.util.StrictJsonParser;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.nbt.NbtUtils;
import java.nio.file.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
/** Read-only, bounded native stats input. Never writes, repairs or fabricates historical totals. */
public final class StatisticsArchive{
    public static final int MAX_FILE_BYTES=4*1024*1024;
    private StatisticsArchive(){}
    public static long read(Path path,String metric,DataFixer fixer)throws IOException{
        if(!Files.isRegularFile(path,LinkOption.NOFOLLOW_LINKS))throw new IOException("Not a regular stats file");
        byte[] bytes;
        try(var in=Files.newInputStream(path)){bytes=in.readNBytes(MAX_FILE_BYTES+1);}
        if(bytes.length>MAX_FILE_BYTES)throw new IOException("Statistics file exceeds read limit");
        var dynamic=new Dynamic<JsonElement>(JsonOps.INSTANCE,StrictJsonParser.parse(new String(bytes,StandardCharsets.UTF_8)));
        dynamic=DataFixTypes.STATS.updateToCurrentVersion(fixer,dynamic,NbtUtils.getDataVersion(dynamic,1343));
        return value(dynamic.getValue(),metric);
    }
    public static long value(JsonElement document,String metric){
        if(!document.isJsonObject())throw new IllegalArgumentException("Invalid stats document");
        var root=document.getAsJsonObject();var stats=root.getAsJsonObject("stats");
        if(stats==null)throw new IllegalArgumentException("Missing native stats object");
        if(metric.equals(LeaderboardMetrics.TRAVEL)){
            var movement=stats.getAsJsonObject("minecraft:custom");
            return movement==null?0:LeaderboardMetrics.traveledBlocks(stat->count(movement.get(stat.toString())));
        }
        String[] parts=metric.split("\\|",-1);if(parts.length!=2)throw new IllegalArgumentException("Invalid metric");
        boolean total=parts[0].equals("total");var group=stats.getAsJsonObject(total?parts[1]:parts[0]);
        if(group==null)return 0;
        if(!total)return count(group.get(parts[1]));
        long value=0;for(var entry:group.entrySet()){long n=count(entry.getValue());value=value>Long.MAX_VALUE-n?Long.MAX_VALUE:value+n;}
        return value;
    }
    private static long count(JsonElement e){
        if(e==null)return 0;
        if(!e.isJsonPrimitive()||!e.getAsJsonPrimitive().isNumber())throw new IllegalArgumentException("Invalid stat number");
        long n=e.getAsBigDecimal().longValueExact();
        if(n<0)throw new IllegalArgumentException("Negative stat");
        return n;
    }
}
