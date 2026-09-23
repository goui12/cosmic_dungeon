package net.goui.cosmicdungeon.economy;

import net.minecraft.nbt.*;
import java.util.*;

/** Economy Internal, 2026-08-18: exactly one registration, never inferred from health/name. */
public final class D1MobRewardRules {
    public static final String PREFIX="cosmic_spawner_";
    public static final String CATEGORY="cosmicdungeon.reward_category", TRACE="cosmicdungeon.reward_trace";
    public record Result(long trace,String source,String warning) {
        public boolean valid(){return warning.isEmpty();}
    }
    private D1MobRewardRules(){}
    private static Result fail(String reason){return new Result(0,"none",reason);}
    public static boolean marker(String value) {
        if(value==null||!value.startsWith(PREFIX))return false;
        String[] xyz=value.substring(PREFIX.length()).split("_",-1);
        if(xyz.length!=3)return false;
        try {
            for(String axis:xyz)if(!Integer.toString(Integer.parseInt(axis)).equals(axis))return false;
            return true;
        } catch(NumberFormatException bad){return false;}
    }
    public static boolean spawnerEntry(Object value) {
        if(!(value instanceof String s))return false;
        String[] pair=s.split("=",-1);
        return pair.length==2&&marker(pair[0])&&pair[1].matches("[a-z0-9_]+");
    }
    public static Result resolve(String entity,Set<String> tags,CompoundTag data,
            List<? extends String> mobs,List<? extends String> spawners,Map<String,Long> categories) {
        String origin=null;
        for(String tag:tags)if(tag.startsWith(PREFIX)) {
            if(!marker(tag))return fail("malformed spawner provenance");
            if(origin!=null)return fail("multiple spawner origins");
            origin=tag;
        }
        if(origin==null)return fail("missing spawner provenance");
        boolean category=data.contains(CATEGORY),amount=data.contains(TRACE);
        if(category&&amount)return fail("both category and explicit Trace");
        if(amount) {
            Tag tag=data.get(TRACE);
            // Floating NBT must not truncate silently into an authorized whole-Trace reward.
            if(!(tag instanceof ByteTag||tag instanceof ShortTag||tag instanceof IntTag||tag instanceof LongTag))
                return fail("explicit Trace is not an integer");
            long trace=((NumericTag)tag).longValue();
            return trace<0?fail("negative explicit Trace"):new Result(trace,"entity override","");
        }
        if(category) {
            if(!(data.get(CATEGORY) instanceof StringTag))return fail("category is not text");
            return value(data.getStringOr(CATEGORY,""),"entity override",categories);
        }
        Result encounter=registered(origin,spawners,"spawner "+origin,categories);
        if(encounter!=null)return encounter;
        Result type=registered(entity,mobs,"entity type "+entity,categories);
        return type==null?fail("unregistered dungeon mob"):type;
    }
    private static Result registered(String key,List<? extends String> entries,String source,Map<String,Long> categories) {
        String value=null;
        for(String entry:entries) {
            String[] pair=entry.split("=",2);
            if(!pair[0].equals(key))continue;
            if(value!=null)return fail("duplicate reward registration");
            if(pair.length!=2)return fail("malformed reward registration");
            value=pair[1];
        }
        return value==null?null:value(value,source,categories);
    }
    private static Result value(String value,String source,Map<String,Long> categories) {
        Long configured=categories.get(value);
        if(configured!=null)return configured<0?fail("negative reward category"):new Result(configured,source,"");
        if(!value.matches("[0-9]+"))return fail("unknown reward category");
        try{return new Result(Long.parseLong(value),source,"");}
        catch(NumberFormatException bad){return fail("Trace value exceeds signed 64-bit range");}
    }
    // TODO(M05/M103, licensed authored-world review): Economy Internal
    // 17ufIuIy0VhLmB_V-6sZ7sCaUCZuGZUkHrgJLVpEcS28 requires one explicit category/value.
    // Review actual D1 encounter coordinates/presets before populating registeredSpawnerRewards.
    // Coordinate keys intentionally apply to matching copies of a D1 instance. Never infer boss
    // payouts from health, custom names or the one-shot flag; never replace a placed spawner.
}
