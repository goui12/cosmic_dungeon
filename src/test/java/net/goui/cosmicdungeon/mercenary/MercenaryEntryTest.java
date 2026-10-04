package net.goui.cosmicdungeon.mercenary;
import com.mojang.serialization.Codec;
import net.goui.cosmicdungeon.economy.*;
import net.goui.cosmicdungeon.dungeon.*;
import net.goui.cosmicdungeon.npc.tamsin.*;
import net.goui.cosmicdungeon.network.PartyPayloads;
import net.goui.cosmicdungeon.item.identity.ClassItemOwnership;
import net.minecraft.nbt.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.entity.EquipmentSlot;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
final class MercenaryEntryTest {
    @org.junit.jupiter.api.BeforeAll static void config()throws Exception{
        var spec=net.goui.cosmicdungeon.Config.SPEC;
        var defaults=com.electronwill.nightconfig.toml.TomlFormat.newConfig();spec.correct(defaults);
        var ctor=Class.forName("net.neoforged.fml.config.LoadedConfig").getDeclaredConstructor(
            com.electronwill.nightconfig.core.CommentedConfig.class,java.nio.file.Path.class,net.neoforged.fml.config.ModConfig.class);
        ctor.setAccessible(true);
        spec.acceptConfig((net.neoforged.fml.config.IConfigSpec.ILoadedConfig)ctor.newInstance(defaults,null,null));
    }
    @org.junit.jupiter.api.AfterAll static void unloadConfig(){net.goui.cosmicdungeon.Config.SPEC.acceptConfig(null);}

    private final UUID a=UUID.randomUUID(),b=UUID.randomUUID();
    private final D1PartyLobby.Anchor anchor=new D1PartyLobby.Anchor(UUID.randomUUID(),"minecraft:overworld",0);
    private D1PartyLobby lobby(int cap){
        var l=new D1PartyLobby();assertNull(l.create(a,anchor,0,"Hires",cap));return l;
    }
    private void join(D1PartyLobby l,UUID member){
        assertNull(l.invite(a,member,anchor,0,100,6));
        assertNull(l.accept(member,l.invitation(member).token(),1));
        assertNull(l.joinAccepted(member,2,6,true));
    }
    @Test void oneHireConsumesCapacityAndCannotReplaceWithoutRelease(){
        var l=lobby(2);var p=l.party(a);
        assertNull(l.hire(a,p.revision(),"theurgist"));assertEquals(2,p.occupied());
        assertNotNull(l.hire(a,p.revision(),"bogatyr"));assertNotNull(l.invite(a,b,anchor,0,100,6));
        assertNotNull(l.capacity(a,p.revision(),1));assertNotNull(l.hire(b,p.revision(),"bogatyr"));
        assertNotNull(l.hire(a,p.revision()-1,""));assertEquals(1,p.hires().size());
        assertNull(l.hire(a,p.revision(),""));assertEquals(1,p.occupied());
    }
    @Test void acceptedInvitationDoesNotOverfillAfterHire(){
        var l=lobby(2);var p=l.party(a);assertNull(l.invite(a,b,anchor,0,100,6));
        assertNull(l.accept(b,l.invitation(b).token(),1));assertNull(l.hire(a,p.revision(),"bogatyr"));
        assertNotNull(l.joinAccepted(b,2,6,true));assertNull(l.party(b));
    }
    @Test void changingHireClearsReadinessAndDepartureReleasesOnlyOwnHire(){
        var l=lobby(6);join(l,b);var p=l.party(a);
        assertNull(l.hire(a,p.revision(),"bogatyr"));assertNull(l.hire(b,p.revision(),"theurgist"));
        assertNull(l.begin(a,p.revision(),Map.of(a,"bogatyr",b,"theurgist")));assertNull(l.ready(a,p.revision()));
        assertNull(l.hire(b,p.revision(),""));assertTrue(p.ready().isEmpty());
        assertNull(l.hire(b,p.revision(),"theurgist"));l.remove(b);
        assertEquals(Map.of(a,"bogatyr"),p.hires());assertEquals(2,p.occupied());
    }
    @Test void queuedAndPreparingHiresAreImmutable(){
        var l=lobby(2);var p=l.party(a);assertNull(l.hire(a,p.revision(),"theurgist"));
        assertNull(l.begin(a,p.revision(),Map.of(a,"bogatyr")));assertNull(l.ready(a,p.revision()));
        assertNull(l.queue(a,p.revision()));assertNotNull(l.hire(a,p.revision(),""));
        l.startCountdown(p,0,1);assertTrue(l.prepare(p,1));assertNotNull(l.hire(a,p.revision(),""));
    }
    @Test void slotsAndOwnersAreDistinctAndPasteRealClass(){
        var l=lobby(6);join(l,b);var p=l.party(a);
        assertNull(l.hire(a,p.revision(),"theurgist"));assertNull(l.hire(b,p.revision(),"dragoon"));
        var hires=p.contracts();assertEquals(List.of(3,4),hires.stream().map(MercenaryContract::slot).toList());
        assertEquals(2,hires.stream().map(MercenaryContract::id).distinct().count());
        var plan=DungeonStartupSchematicPlan.buildPlan(List.of("bogatyr","judicator","theurgist","dragoon"));
        assertEquals("d1_theurgist.schem",plan.requests().get(2).schematicFilename());
        assertEquals("blankslot",plan.normalizedClassSlots().get(4));assertEquals(36,plan.requests().size());
    }
    @SuppressWarnings("unchecked") private Codec<PlayerCurrencyData> codec()throws Exception{
        var f=PlayerCurrencyData.class.getDeclaredField("CODEC");f.setAccessible(true);return (Codec<PlayerCurrencyData>)f.get(null);
    }
    private PlayerCurrencyData data()throws Exception{
        var root=new CompoundTag();var balances=new CompoundTag();balances.putLong(a.toString(),900);balances.putLong(b.toString(),600);
        root.put("balances",balances);return codec().parse(NbtOps.INSTANCE,root).getOrThrow();
    }
    private PlayerCurrencyData reload(PlayerCurrencyData d)throws Exception{return codec().parse(NbtOps.INSTANCE,codec().encodeStart(NbtOps.INSTANCE,d).getOrThrow()).getOrThrow();}
    @Test void feeReservationSurvivesReloadWithoutChargingAndCancelsExactlyOnce()throws Exception{
        var d=data();assertTrue(d.reserveMercenaryFees(5,Map.of(a,500L,b,500L)));
        assertEquals(900,d.getBalanceTrace(a));assertEquals(400,d.availableTrace(a));assertEquals(100,d.availableTrace(b));
        d=reload(d);assertEquals(Set.of(5L),d.pendingMercenaryFeeRuns());d.settleMercenaryFees(5,false);d.settleMercenaryFees(5,true);
        assertEquals(900,d.getBalanceTrace(a));assertEquals(600,d.getBalanceTrace(b));
        assertEquals(900,reload(d).availableTrace(a));
    }
    @Test void successfulGroupDebitIsAtomicAndReplaySafe()throws Exception{
        var d=data();assertTrue(d.reserveMercenaryFees(7,Map.of(a,500L,b,500L)));d=reload(d);
        d.settleMercenaryFees(7,true);d=reload(d);d.settleMercenaryFees(7,true);d.settleMercenaryFees(7,false);
        assertEquals(400,d.getBalanceTrace(a));assertEquals(100,d.getBalanceTrace(b));assertTrue(d.pendingMercenaryFeeRuns().isEmpty());
        assertFalse(d.reserveMercenaryFees(7,Map.of(a,500L,b,500L)));
    }
    @Test void insufficientMemberRejectsWholeGroupAndExistingHoldsCount()throws Exception{
        var d=data();assertFalse(d.reserveMercenaryFees(9,Map.of(a,500L,b,700L)));
        assertEquals(900,d.availableTrace(a));assertTrue(d.pendingMercenaryFeeRuns().isEmpty());
        assertTrue(d.reserveMercenaryFees(10,Map.of(a,500L)));assertFalse(d.reserveMercenaryFees(11,Map.of(a,500L)));
        assertThrows(IllegalArgumentException.class,()->d.reserveMercenaryFees(10,Map.of(a,400L)));
    }
    @Test void oldAccountDefaultsAndZeroCostAreValid()throws Exception{
        var d=data();assertTrue(d.pendingMercenaryFeeRuns().isEmpty());assertTrue(d.reserveMercenaryFees(4,Map.of(a,0L)));
        d.settleMercenaryFees(4,true);assertEquals(900,reload(d).getBalanceTrace(a));
    }
    @Test void rejectedSecondLedgerRowRestoresWholeGroup()throws Exception{
        var d=data();assertTrue(d.reserveMercenaryFees(15,Map.of(a,500L,b,500L)));
        var field=PlayerCurrencyData.class.getDeclaredField("ledger");field.setAccessible(true);
        ((CompoundTag)field.get(d)).putLong("sequence",Long.MAX_VALUE-1);
        var before=codec().encodeStart(NbtOps.INSTANCE,d).getOrThrow();
        assertThrows(ArithmeticException.class,()->d.settleMercenaryFees(15,true));
        assertEquals(before,codec().encodeStart(NbtOps.INSTANCE,d).getOrThrow());
        assertEquals(900,d.getBalanceTrace(a));assertEquals(600,d.getBalanceTrace(b));
    }
    @Test void hireQuoteIsFrozenAndFutureClassesStayUnavailable(){
        var l=lobby(6);var p=l.party(a);
        assertNotNull(l.hire(a,p.revision(),"deadeye",500));assertNotNull(l.hire(a,p.revision(),"metalmancer",500));
        assertNull(l.hire(a,p.revision(),"theurgist",777));assertEquals(777,p.contracts().getFirst().fee());
        assertNull(l.capacity(a,p.revision(),3));assertEquals(777,p.hireFee(a));
    }
    @Test void malformedFeesFailClosed(){
        assertThrows(IllegalArgumentException.class,()->new MercenaryFee(Map.of(a,-1L),"reserved"));
        assertThrows(IllegalArgumentException.class,()->new MercenaryContract(a,a,"bogatyr",1,500));
        assertThrows(IllegalArgumentException.class,()->new MercenaryContract(UUID.randomUUID(),a,"bogatyr",7,500));
    }
    private MercenaryContract starter(){return new MercenaryContract(a,b,"bogatyr",2,0);}
    private static net.minecraft.world.item.alchemy.PotionBrewing recipes(){
        var builder=new net.minecraft.world.item.alchemy.PotionBrewing.Builder(net.minecraft.world.flag.FeatureFlags.DEFAULT_FLAGS);
        net.minecraft.world.item.alchemy.PotionBrewing.addVanillaMixes(builder);return builder.build();
    }
    @Test void usableStarterCopiesPreserveAllAuthoredStacksWithoutUnusedSpareGear(){
        var armor=new ItemStack(Items.DIAMOND_CHESTPLATE);armor.set(DataComponents.CUSTOM_NAME,Component.literal("Authored mail"));
        armor.setDamageValue(17);var bow=new ItemStack(Items.BOW);var spare=new ItemStack(Items.BOW);
        var rockets=new ItemStack(Items.FIREWORK_ROCKET,32);rockets.set(DataComponents.CUSTOM_NAME,Component.literal("Emergency rockets"));
        var original=List.of(armor,bow,spare,rockets,new ItemStack(Items.ARROW,32),new ItemStack(Items.NETHER_WART,4));var before=original.stream().map(ItemStack::copy).toList();
        var plan=MercenaryEquipment.plan(original,starter(),54,recipes());assertNotNull(plan);
        assertTrue(ItemStack.matches(armor,plan.equipment().get(EquipmentSlot.CHEST)));
        assertTrue(ItemStack.matches(bow,plan.equipment().get(EquipmentSlot.MAINHAND)));
        assertEquals(2,plan.supplies().size());assertTrue(plan.supplies().get(0).is(Items.ARROW));assertTrue(plan.supplies().get(1).is(Items.NETHER_WART));
        for(int i=0;i<original.size();i++)assertTrue(ItemStack.matches(before.get(i),original.get(i)));
        assertNotSame(armor,plan.equipment().get(EquipmentSlot.CHEST));
        assertNull(MercenaryEquipment.plan(original,starter(),1,recipes()));
    }
    @Test void foreignOrMalformedBoundStackIsNeverTaken(){
        var stack=new ItemStack(Items.BOW);var tag=new CompoundTag();tag.putString(ClassItemOwnership.KEY,b.toString());
        stack.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));
        assertNull(MercenaryEquipment.plan(List.of(stack),starter(),54,recipes()));
        tag.putString(ClassItemOwnership.KEY,"broken");stack.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));
        assertNull(MercenaryEquipment.plan(List.of(stack),starter(),54,recipes()));
    }
    @Test void packetCarriesFeeAndOwnReservation(){
        var view=new PartyPayloads.View(4,new PartyPayloads.State(2,"ASSEMBLY",true,6,0,-1),
            List.of(new PartyPayloads.Member("Mercenary","theurgist",true,false)),
            new PartyPayloads.Invite("","",false,false),PartyPayloads.Recruitment.EMPTY,"HARD",new PartyPayloads.Hire("theurgist",1234));
        var buf=Unpooled.buffer();try{PartyPayloads.View.STREAM_CODEC.encode(buf,view);assertEquals(view,PartyPayloads.View.STREAM_CODEC.decode(buf));}
        finally{buf.release();}
    }
    @Test void runRecordOldShapeAndTransitionsPreserveContract(){
        var old=new DungeonRunRegistryData.RunRecord(1,"dungeon_1","minecraft:overworld",0,List.of("cosmicdungeon:d1_instance_1"),1,"ACTIVE","",0,List.of(a),List.of(),List.of());
        var encoded=(CompoundTag)DungeonRunRegistryData.RunRecord.CODEC.encodeStart(NbtOps.INSTANCE,old).getOrThrow();
        encoded.remove("mercenaries");assertTrue(DungeonRunRegistryData.RunRecord.CODEC.parse(NbtOps.INSTANCE,encoded).getOrThrow().mercenaries().isEmpty());
        var contract=new MercenaryContract(UUID.randomUUID(),a,"theurgist",2,500);var run=old.withMercenaries(List.of(contract));
        run=DungeonRunRegistryData.RunRecord.CODEC.parse(NbtOps.INSTANCE,DungeonRunRegistryData.RunRecord.CODEC.encodeStart(NbtOps.INSTANCE,run).getOrThrow()).getOrThrow();
        assertEquals(List.of(contract),run.withState(DungeonRunState.RESETTING,DungeonResetReason.ABANDONED).mercenaries());
        assertEquals(List.of(contract),run.withDifficulty(DungeonDifficulty.Profile.LEGACY).mercenaries());
        assertEquals(List.of(contract),run.withoutPlayer(a).mercenaries());
        assertTrue(MercenaryLifecycle.admitted(run,contract,contract.id(),"cosmicdungeon:d1_instance_1"));
        assertFalse(MercenaryLifecycle.admitted(run,contract,UUID.randomUUID(),"cosmicdungeon:d1_instance_1"));
        assertFalse(MercenaryLifecycle.admitted(run,contract,contract.id(),"minecraft:overworld"));
        assertFalse(MercenaryLifecycle.admitted(run.withoutPlayer(a),contract,contract.id(),"cosmicdungeon:d1_instance_1"));
        assertFalse(MercenaryLifecycle.admitted(run.withCompletionExited(a),contract,contract.id(),"cosmicdungeon:d1_instance_1"));
    }
}
