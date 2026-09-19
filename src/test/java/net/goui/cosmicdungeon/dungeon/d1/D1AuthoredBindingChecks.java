package net.goui.cosmicdungeon.dungeon.d1;

import net.goui.cosmicdungeon.economy.D1MobRewardRules;
import net.goui.cosmicdungeon.vendor.VendorBindingRules;
import net.goui.cosmicdungeon.block.entity.CosmicSpawnDataRules;
import net.minecraft.nbt.*;
import net.minecraft.world.phys.*;
import java.util.*;
import java.nio.file.*;

/** Production rules and native compressed NBT; no world or server is launched. */
public final class D1AuthoredBindingChecks {
    private static int checks;
    private static final String ORIGIN="cosmic_spawner_-12_64_300";
    private static final Map<String,Long> REWARDS=Map.of("weak",2L,"common",3L,"tough",4L,"elite",6L,"miniboss",10L,"boss",100L);
    private static void check(boolean ok,String label){checks++;if(!ok)throw new AssertionError(label);}
    private static D1MobRewardRules.Result reward(CompoundTag tag,List<String> encounters,List<String> mobs) {
        return D1MobRewardRules.resolve("minecraft:zombie",Set.of(ORIGIN),tag,mobs,encounters,REWARDS);
    }
    private static void rewards() {
        var empty=new CompoundTag();
        for(var entry:REWARDS.entrySet()) {
            var result=reward(empty,List.of(),List.of("minecraft:zombie="+entry.getKey()));
            check(result.valid()&&result.trace()==entry.getValue(),"configured category "+entry.getKey());
            result=reward(empty,List.of(ORIGIN+"="+entry.getKey()),List.of("minecraft:zombie=weak"));
            check(result.valid()&&result.trace()==entry.getValue(),"authored spawner overrides type "+entry.getKey());
            var data=new CompoundTag();data.putString(D1MobRewardRules.CATEGORY,entry.getKey());
            result=reward(data,List.of(ORIGIN+"=boss"),List.of("minecraft:zombie=weak"));
            check(result.valid()&&result.trace()==entry.getValue(),"explicit entity overrides table");
        }
        for(String value:List.of("0","7","1000000000",Long.toString(Long.MAX_VALUE))) {
            var result=reward(empty,List.of(ORIGIN+"="+value),List.of());
            check(result.valid()&&result.trace()==Long.parseLong(value),"whole Trace full range");
        }
        for(Tag value:List.of(ByteTag.valueOf((byte)7),ShortTag.valueOf((short)7),IntTag.valueOf(7),LongTag.valueOf(7))) {
            var data=new CompoundTag();data.put(D1MobRewardRules.TRACE,value);
            check(reward(data,List.of(),List.of()).trace()==7,"native integer accepted");
            data.putString(D1MobRewardRules.CATEGORY,"boss");
            check(!reward(data,List.of(),List.of()).valid(),"ambiguous persistent settings denied");
        }
        for(Tag value:List.of(FloatTag.valueOf(7F),DoubleTag.valueOf(7D),DoubleTag.valueOf(Double.NaN),
                StringTag.valueOf("7"),new CompoundTag(),LongTag.valueOf(-1))) {
            var data=new CompoundTag();data.put(D1MobRewardRules.TRACE,value);
            check(!reward(data,List.of(),List.of("minecraft:zombie=boss")).valid(),"malformed override cannot fall back");
        }
        for(String value:List.of("","future","-1","+1"," 2","2.5","9223372036854775808")) {
            check(!reward(empty,List.of(ORIGIN+"="+value),List.of("minecraft:zombie=boss")).valid(),"invalid encounter fails closed");
        }
        check(!reward(empty,List.of(ORIGIN+"=weak",ORIGIN+"=boss"),List.of()).valid(),"duplicate encounter denied");
        check(!reward(empty,List.of(),List.of("minecraft:zombie=weak","minecraft:zombie=weak")).valid(),"duplicate entity denied even identical");
        check(!reward(empty,List.of(),List.of()).valid(),"unknown entity gets warning");
        for(String invalid:List.of("cosmic_spawner_1_2","cosmic_spawner_01_2_3","cosmic_spawner_1_2_2147483648","cosmic_spawner_x_2_3")) {
            check(!D1MobRewardRules.marker(invalid),"malformed coordinate tag");
            check(!D1MobRewardRules.resolve("minecraft:zombie",Set.of(invalid),empty,List.of("minecraft:zombie=boss"),List.of(),REWARDS).valid(),"no spoofed provenance");
        }
        check(!D1MobRewardRules.resolve("minecraft:zombie",Set.of(),empty,List.of("minecraft:zombie=boss"),List.of(),REWARDS).valid(),"ordinary world mob excluded");
        check(!D1MobRewardRules.resolve("minecraft:zombie",Set.of(ORIGIN,"cosmic_spawner_1_2_3"),empty,List.of("minecraft:zombie=boss"),List.of(),REWARDS).valid(),"multiple origins cannot choose payout");
        check(D1MobRewardRules.spawnerEntry(ORIGIN+"=boss"),"config origin accepted");
        check(!D1MobRewardRules.spawnerEntry(ORIGIN+"=boss=7"),"extra config separator rejected");
    }
    private static void spawnData()throws Exception {
        var authored=new CompoundTag();authored.putString("id","minecraft:zombie");
        authored.putString("CustomName","Authored encounter");authored.putInt("Age",123);
        var persistent=new CompoundTag();persistent.putString(D1MobRewardRules.CATEGORY,"miniboss");
        authored.put("NeoForgeData",persistent);var tags=new ListTag();tags.add(StringTag.valueOf("authored_tag"));authored.put("Tags",tags);
        var equipment=new CompoundTag();equipment.putString("mainhand","unchanged");authored.put("equipment",equipment);
        var baseline=authored.copy();
        var tagged=CosmicSpawnDataRules.tagged(authored,ORIGIN,"minecraft:pig");
        check(authored.equals(baseline),"input NBT never mutated");
        check(CosmicSpawnDataRules.canTag(tagged),"valid authored tags remain spawnable");
        check(tagged.getCompoundOrEmpty("NeoForgeData").equals(persistent),"authored reward survives");
        check(tagged.getCompoundOrEmpty("equipment").equals(equipment),"authored equipment survives");
        check(tagged.getStringOr("id","").equals("minecraft:zombie"),"fallback never replaces authored type");
        check(tagged.getListOrEmpty("Tags").size()==2,"original tag plus one origin");
        check(CosmicSpawnDataRules.tagged(tagged,ORIGIN,"minecraft:pig").equals(tagged),"repeated decoration idempotent");
        // Full SpawnData.CODEC initializes Minecraft loot registries and belongs to licensed TEST.
        var encoded=new CompoundTag();encoded.put("entity",tagged);
        var path=Files.createTempFile("d1-b34-spawn-", ".nbt");
        try {
            NbtIo.writeCompressed(encoded,path);
            var read=NbtIo.readCompressed(path,NbtAccounter.create(1024*1024));
            check(read.getCompoundOrEmpty("entity").equals(tagged),"tagged entity native NBT disk round trip");
        } finally {Files.deleteIfExists(path);}
        for(Tag bad:List.of(IntTag.valueOf(2),StringTag.valueOf("damaged"))) {
            var malformed=baseline.copy();malformed.put("Tags",bad);
            check(!CosmicSpawnDataRules.canTag(malformed),"malformed origin pauses spawner without replacing data");
            check(CosmicSpawnDataRules.tagged(malformed,ORIGIN,"minecraft:pig").equals(malformed),"malformed evidence retained, never crashes or invents origin");
        }
        check(CosmicSpawnDataRules.tagged(new CompoundTag(),ORIGIN,"minecraft:zombie").getStringOr("id","").equals("minecraft:zombie"),"legacy type-only fallback");
        // Multiple old origins remain evidence and the reward resolver rejects them.
        var second=CosmicSpawnDataRules.tagged(tagged,"cosmic_spawner_1_2_3","minecraft:pig");
        check(second.getListOrEmpty("Tags").size()==3,"no automatic provenance migration");
    }
    private static void bindings() {
        String food="cosmicdungeon:d1/food_vendor",inn=VendorBindingRules.BELUZON;
        var data=new CompoundTag();data.putString("author_note","retain");
        check(VendorBindingRules.canAssign(data,food,"minecraft:villager",false),"new reviewed assignment allowed");
        check(!VendorBindingRules.canAssign(data,food,"minecraft:villager",true),"Tamsin/Watson protected");
        check(!VendorBindingRules.canAssign(data,inn,"minecraft:villager",false),"Beluzon cannot become villager");
        check(VendorBindingRules.canAssign(data,inn,"minecraft:creaking",false),"native Beluzon allowed");
        for(int flags=0;flags<8;flags++) {
            var state=new CompoundTag();boolean visible=(flags&1)!=0,invulnerable=(flags&2)!=0,noAi=(flags&4)!=0;
            VendorBindingRules.capture(state,StringTag.valueOf("Original"),visible,invulnerable,noAi);
            var before=VendorBindingRules.before(state);
            check(before.visible()==visible&&before.invulnerable()==invulnerable&&before.noAi()==noAi,"exact original flags");
            var copy=state.copy();VendorBindingRules.capture(state,null,false,false,false);
            check(copy.equals(state),"snapshot never overwritten");
            state.putString(VendorBindingRules.PROFILE,food);
            check(VendorBindingRules.canAssign(state,food,"minecraft:villager",false),"same identity may refresh");
            check(!VendorBindingRules.canAssign(state,inn,"minecraft:creaking",false),"no silent role replacement");
        }
        check(VendorBindingRules.before(data)==null,"legacy original flags not invented");
        data.putInt(VendorBindingRules.PROFILE,8);
        check(!VendorBindingRules.canAssign(data,food,"minecraft:villager",false),"malformed profile occupied");
        data.remove(VendorBindingRules.PROFILE);VendorBindingRules.capture(data,null,true,true,false);
        check(!VendorBindingRules.canAssign(data,food,"minecraft:villager",false),"orphan prior state held for review");
        check(VendorBindingRules.before(data).name()==null,"original no-name retained");
        for(String field:List.of("schema","has_name","visible","invulnerable","no_ai")) {
            var malformed=data.copy();malformed.getCompoundOrEmpty(VendorBindingRules.BEFORE).putString(field,"bad");
            boolean rejected=false;try{VendorBindingRules.before(malformed);}catch(IllegalArgumentException expected){rejected=true;}
            check(rejected,"malformed snapshot cannot clear "+field);
        }
        for(int d1=0;d1<=4;d1++)for(int d2=0;d2<=4;d2++) {
            check(VendorBindingRules.tier("D1",d1,d2)==d1,"D2 cannot unlock D1 offer");
            check(VendorBindingRules.tier("D2",d1,d2)==d2,"D1 cannot unlock D2 offer");
        }
        check(VendorBindingRules.tier(null,1,4)==1&&VendorBindingRules.tier("other",4,4)==0,"legacy and unknown systems");
        Vec3 eye=Vec3.ZERO,end=new Vec3(0,0,6);
        check(VendorBindingRules.hit(eye,end,new AABB(-.3,-.3,4,.3,.3,5)).isPresent(),"crosshair target");
        check(VendorBindingRules.hit(eye,end,new AABB(.5,-.3,1,1.2,.3,2)).isEmpty(),"closer adjacent mob not selected");
        check(VendorBindingRules.hit(eye,new Vec3(0,0,3),new AABB(-.3,-.3,4,.3,.3,5)).isEmpty(),"solid block clip prevents binding behind wall");
        check(VendorBindingRules.hit(eye,end,new AABB(-.3,-.3,-2,.3,.3,-1)).isEmpty(),"entity behind player ignored");
        check(VendorBindingRules.hit(eye,end,new AABB(-1,-1,-1,1,1,1)).orElse(-1.0)==0,"eye inside entity is nearest hit");
    }
    public static void main(String[] args)throws Exception {
        rewards();spawnData();bindings();
        System.out.println("D1 authored reward/binding checks passed: "+checks);
    }
}
