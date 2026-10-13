package net.goui.cosmicdungeon.economy;

import java.util.*;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Approved F6 additions supplement saved nonempty configs without overwriting authored decisions. */
final class D1MobRewardCatalogTest {
    private static final String ORIGIN="cosmic_spawner_-12_64_300";
    private static final Map<String,Long> VALUES=Map.of("weak",2L,"common",3L,"tough",4L,"elite",6L,"miniboss",10L,"boss",100L);
    private static final List<String> SAVED_27=List.of(
        "minecraft:zombie=common","minecraft:skeleton=common","minecraft:husk=tough","minecraft:drowned=common",
        "minecraft:spider=common","minecraft:cave_spider=weak","minecraft:creeper=tough","minecraft:stray=tough",
        "minecraft:bogged=tough","minecraft:enderman=tough","minecraft:witch=elite","minecraft:silverfish=weak",
        "minecraft:blaze=tough","minecraft:wither_skeleton=tough","minecraft:ghast=elite","minecraft:piglin=common",
        "minecraft:piglin_brute=elite","minecraft:zombified_piglin=common","minecraft:hoglin=tough",
        "minecraft:magma_cube=common","minecraft:slime=weak","minecraft:endermite=weak",
        "minecraft:guardian=tough","minecraft:elder_guardian=miniboss","minecraft:warden=boss","minecraft:wither=boss",
        "minecraft:ender_dragon=boss");
    private static Map<String,String> approved(){
        var result=new LinkedHashMap<String,String>();
        result.put("minecraft:vex","weak");
        for(String id:List.of("minecraft:zombie_villager","minecraft:pillager","minecraft:phantom",
                "cosmicdungeon:goblin_ambusher","cosmicdungeon:cthonian_gnawling"))result.put(id,"common");
        for(String id:List.of("minecraft:shulker","minecraft:breeze","minecraft:vindicator","minecraft:zoglin",
                "cosmicdungeon:magma_glob","cosmicdungeon:crystal_creeper"))result.put(id,"tough");
        for(String id:List.of("minecraft:creaking","minecraft:evoker","minecraft:illusioner",
                "cosmicdungeon:stone_warden"))result.put(id,"elite");
        for(String id:List.of("minecraft:giant","minecraft:ravager"))result.put(id,"miniboss");
        return result;
    }
    private static D1MobRewardRules.Result resolve(String id,List<String> mobs){
        return resolve(id,mobs,List.of(),new CompoundTag(),VALUES);
    }
    private static D1MobRewardRules.Result resolve(String id,List<String> mobs,List<String> spawners,
            CompoundTag data,Map<String,Long> values){
        return D1MobRewardRules.resolve(id,Set.of(ORIGIN),data,mobs,spawners,values);
    }
    private static void payout(long expected,D1MobRewardRules.Result result){
        assertTrue(result.valid(),result.warning());assertEquals(expected,result.trace());
    }
    @Test void catalogRetainsEveryOriginalRegistrationAndExactlyTheEighteenApprovedAdditions(){
        assertEquals(approved(),D1MobRewardCatalog.additions());
        var expected=new LinkedHashMap<String,String>();
        for(String entry:SAVED_27){var pair=entry.split("=");expected.put(pair[0],pair[1]);}
        expected.putAll(approved());
        assertEquals(expected,D1MobRewardCatalog.registrations());
        var defaults=D1MobRewardCatalog.defaults();
        assertEquals(45,defaults.size());assertEquals(45,new HashSet<>(defaults).size());
        assertEquals(new HashSet<>(SAVED_27),new HashSet<>(defaults.subList(0,27)));
        assertEquals(expected.entrySet().stream().map(e->e.getKey()+"="+e.getValue()).collect(java.util.stream.Collectors.toSet()),
                new HashSet<>(defaults));
    }
    @Test void savedOriginalConfigReceivesEveryApprovedMissingRewardWithoutMutation(){
        var saved=new ArrayList<>(SAVED_27);var before=List.copyOf(saved);
        for(var entry:approved().entrySet())payout(VALUES.get(entry.getValue()),resolve(entry.getKey(),saved));
        assertEquals(before,saved);
        for(String entry:SAVED_27){var pair=entry.split("=");payout(VALUES.get(pair[1]),resolve(pair[0],saved));}
    }
    @Test void nonemptyCustomConfigReceivesOnlyAdditionsAndDoesNotRestoreRemovedOldTypes(){
        var custom=List.of("minecraft:zombie=17");
        payout(17,resolve("minecraft:zombie",custom));
        for(var entry:approved().entrySet())payout(VALUES.get(entry.getValue()),resolve(entry.getKey(),custom));
        assertFalse(resolve("minecraft:skeleton",custom).valid());
        assertFalse(resolve("minecraft:ender_dragon",custom).valid());
    }
    @Test void explicitZeroNumericAndCategoryEntriesOverrideFallback(){
        for(String id:approved().keySet()){
            payout(0,resolve(id,List.of(id+"=0")));
            payout(19,resolve(id,List.of(id+"=19")));
            payout(100,resolve(id,List.of(id+"=boss")));
        }
    }
    @Test void duplicateAndMalformedExplicitEntriesFailClosedInsteadOfFallingBack(){
        for(String id:approved().keySet()){
            for(var entries:List.of(List.of(id+"=0",id+"=common"),List.of(id+"=common",id+"=common"),
                    List.of(id),List.of(id+"="),List.of(id+"=unknown"),List.of(id+"=common=7"),
                    List.of(id+"=-1"),List.of(id+"=9223372036854775808"))){
                var result=resolve(id,entries);assertFalse(result.valid(),id+" "+entries);assertEquals(0,result.trace());
            }
        }
    }
    @Test void emptyConfigDisablesFallbackAndUnsupportedTypesStayUnregistered(){
        for(String id:approved().keySet())assertFalse(resolve(id,List.of()).valid(),id);
        for(String id:List.of("minecraft:wolf","minecraft:villager","cosmicdungeon:mercenary",
                "cosmicdungeon:metalmancer_golem","cosmicdungeon:d1_arrow","othermod:shulker","minecraft:future_monster"))
            assertFalse(resolve(id,SAVED_27).valid(),id);
        // Explicit custom registrations remain an authored choice, not inferred catalog coverage.
        payout(7,resolve("othermod:monster",List.of("othermod:monster=7")));
    }
    @Test void categoryConfigurationControlsFallbackAndNeverGuessesMissingOrNegativeValues(){
        var configured=new HashMap<>(VALUES);configured.put("common",31L);
        payout(31,resolve("minecraft:zombie_villager",SAVED_27,List.of(),new CompoundTag(),configured));
        configured.put("tough",0L);
        payout(0,resolve("minecraft:shulker",SAVED_27,List.of(),new CompoundTag(),configured));
        configured.remove("common");
        assertFalse(resolve("minecraft:zombie_villager",SAVED_27,List.of(),new CompoundTag(),configured).valid());
        configured.put("common",-1L);
        assertFalse(resolve("minecraft:zombie_villager",SAVED_27,List.of(),new CompoundTag(),configured).valid());
    }
    @Test void spawnerAndEntityOverridesKeepTheirExistingPrecedenceAndFailureSemantics(){
        var id="minecraft:shulker";var mobs=List.of(id+"=0");var tag=new CompoundTag();
        payout(10,resolve(id,mobs,List.of(ORIGIN+"=miniboss"),tag,VALUES));
        tag.putString(D1MobRewardRules.CATEGORY,"elite");
        payout(6,resolve(id,mobs,List.of(ORIGIN+"=miniboss"),tag,VALUES));
        tag.remove(D1MobRewardRules.CATEGORY);tag.putLong(D1MobRewardRules.TRACE,0);
        payout(0,resolve(id,mobs,List.of(ORIGIN+"=miniboss"),tag,VALUES));
        tag.putString(D1MobRewardRules.CATEGORY,"elite");
        assertFalse(resolve(id,mobs,List.of(),tag,VALUES).valid());
        tag=new CompoundTag();
        assertFalse(resolve(id,SAVED_27,List.of(ORIGIN+"=unknown"),tag,VALUES).valid());
        assertFalse(resolve(id,SAVED_27,List.of(ORIGIN+"=weak",ORIGIN+"=weak"),tag,VALUES).valid());
    }
    @Test void additionsStillRequireOneValidSpawnerOrigin(){
        for(var tags:List.of(Set.<String>of(),Set.of("cosmic_spawner_bad"),Set.of(ORIGIN,"cosmic_spawner_1_2_3")))
            assertFalse(D1MobRewardRules.resolve("minecraft:zombie_villager",tags,new CompoundTag(),
                    SAVED_27,List.of(),VALUES).valid());
    }
}
