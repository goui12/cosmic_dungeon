package net.goui.cosmicdungeon.mercenary;

import com.mojang.serialization.Codec;
import java.util.*;
import net.goui.cosmicdungeon.economy.*;
import net.goui.cosmicdungeon.dungeon.*;
import net.goui.cosmicdungeon.network.PartyPayloads;
import net.minecraft.nbt.*;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

final class MercenaryRecoveryTest {
    @BeforeAll static void config()throws Exception{
        var spec=net.goui.cosmicdungeon.Config.SPEC;
        var defaults=com.electronwill.nightconfig.toml.TomlFormat.newConfig();spec.correct(defaults);
        var ctor=Class.forName("net.neoforged.fml.config.LoadedConfig").getDeclaredConstructor(
            com.electronwill.nightconfig.core.CommentedConfig.class,java.nio.file.Path.class,net.neoforged.fml.config.ModConfig.class);
        ctor.setAccessible(true);
        spec.acceptConfig((net.neoforged.fml.config.IConfigSpec.ILoadedConfig)ctor.newInstance(defaults,null,null));
    }
    @AfterAll static void unload(){net.goui.cosmicdungeon.Config.SPEC.acceptConfig(null);}
    private final UUID owner=UUID.randomUUID(),donor=UUID.randomUUID(),merc=UUID.randomUUID(),death=UUID.randomUUID();
    @SuppressWarnings("unchecked") private Codec<PlayerCurrencyData> codec()throws Exception{
        var f=PlayerCurrencyData.class.getDeclaredField("CODEC");f.setAccessible(true);return (Codec<PlayerCurrencyData>)f.get(null);
    }
    private PlayerCurrencyData accounts(long first,long second)throws Exception{
        var root=new CompoundTag();var balances=new CompoundTag();balances.putLong(owner.toString(),first);balances.putLong(donor.toString(),second);
        root.put("balances",balances);return codec().parse(NbtOps.INSTANCE,root).getOrThrow();
    }
    private PlayerCurrencyData reload(PlayerCurrencyData data)throws Exception{
        return codec().parse(NbtOps.INSTANCE,codec().encodeStart(NbtOps.INSTANCE,data).getOrThrow()).getOrThrow();
    }
    @Test void oneDeathChargesExactlyOnePayerAcrossRepeatClicksAndReload()throws Exception{
        var data=accounts(50,50);assertTrue(data.payMercenaryRevive(1,merc,death,12000,owner,25));
        data=reload(data);assertTrue(data.payMercenaryRevive(1,merc,death,12000,donor,25));
        assertTrue(data.payMercenaryRevive(1,merc,death,12000,owner,25));
        assertEquals(25,data.getBalanceTrace(owner));assertEquals(50,data.getBalanceTrace(donor));
        assertTrue(MercenaryRevivePayment.paid(data,1,merc,death,12000));
        var op=data.operation(MercenaryRevivePayment.id(1,merc,death,12000)).orElseThrow();
        assertEquals(owner,op.owner());assertTrue(op.acknowledged());assertTrue(op.plan().isEmpty());
        assertTrue(data.pendingOperation(owner).isEmpty());
    }
    @Test void insufficientFundsDoNotPoisonLaterDonationAndHoldsAreRespected()throws Exception{
        var data=accounts(24,50);assertFalse(data.payMercenaryRevive(1,merc,death,12000,owner,25));
        assertFalse(MercenaryRevivePayment.paid(data,1,merc,death,12000));
        assertTrue(data.reserveMercenaryFees(2,Map.of(donor,30L)));
        assertFalse(data.payMercenaryRevive(1,merc,death,12000,donor,25));
        data.settleMercenaryFees(2,false);assertTrue(data.payMercenaryRevive(1,merc,death,12000,donor,25));
        assertEquals(24,reload(data).getBalanceTrace(owner));assertEquals(25,data.getBalanceTrace(donor));
    }
    @Test void eachDeathAndRunHasItsOwnEntitlement()throws Exception{
        var data=accounts(100,100);assertTrue(data.payMercenaryRevive(1,merc,death,12000,owner,25));
        assertFalse(MercenaryRevivePayment.paid(data,1,merc,death,24000));
        assertFalse(MercenaryRevivePayment.paid(data,2,merc,death,12000));
        UUID sameTickDeath=UUID.randomUUID();
        assertFalse(MercenaryRevivePayment.paid(data,1,merc,sameTickDeath,12000));
        assertTrue(data.payMercenaryRevive(1,merc,sameTickDeath,12000,owner,25));
        assertEquals(50,data.getBalanceTrace(owner));
        assertTrue(data.payMercenaryRevive(1,merc,death,24000,donor,25));
        assertEquals(75,reload(data).getBalanceTrace(donor));
    }
    @Test void failedLedgerWriteRestoresExactAccountImage()throws Exception{
        var data=accounts(50,50);var f=PlayerCurrencyData.class.getDeclaredField("ledger");f.setAccessible(true);
        ((CompoundTag)f.get(data)).putLong("sequence",Long.MAX_VALUE);
        var before=codec().encodeStart(NbtOps.INSTANCE,data).getOrThrow();
        assertThrows(ArithmeticException.class,()->data.payMercenaryRevive(1,merc,death,12000,owner,25));
        assertEquals(before,codec().encodeStart(NbtOps.INSTANCE,data).getOrThrow());
    }
    @Test void oldAccountsLoadAndZeroFeeIsReplaySafe()throws Exception{
        var data=accounts(0,0);assertTrue(data.payMercenaryRevive(1,merc,death,12000,owner,0));
        assertTrue(MercenaryRevivePayment.paid(reload(data),1,merc,death,12000));
        assertEquals(0,data.getBalanceTrace(owner));
        assertThrows(IllegalArgumentException.class,()->data.payMercenaryRevive(0,merc,death,12000,owner,25));
        assertThrows(IllegalArgumentException.class,()->data.payMercenaryRevive(1,merc,death,12001,owner,-1));
    }
    @Test void newHirePriceIs50AndReviveIsHalfTheFrozenContractFee(){
        assertEquals(50,MercenaryConfig.HIRE_TRACE.get());
        assertEquals(25,MercenaryRevival.price(new MercenaryContract(merc,owner,"theurgist",2,50)));
        assertEquals(250,MercenaryRevival.price(new MercenaryContract(merc,owner,"theurgist",2,500)));
    }
    @Test void oldRestAutomaticallyGetsStableLegacyIdentityAndNewDeathsRoundTrip(){
        var old=new MercenaryRest(12000,"cosmicdungeon:d1_instance_1",45,16);
        var tag=(CompoundTag)MercenaryRest.CODEC.encodeStart(NbtOps.INSTANCE,old).getOrThrow();tag.remove("death");
        var loaded=MercenaryRest.CODEC.parse(NbtOps.INSTANCE,tag).getOrThrow();assertEquals(old,loaded);
        var fresh=new MercenaryRest(old.until(),old.dimension(),old.position(),old.flags(),death);
        assertEquals(fresh,MercenaryRest.CODEC.parse(NbtOps.INSTANCE,MercenaryRest.CODEC.encodeStart(NbtOps.INSTANCE,fresh).getOrThrow()).getOrThrow());
        assertNotEquals(MercenaryRevivePayment.id(1,merc,old.death(),old.until()),MercenaryRevivePayment.id(1,merc,fresh.death(),fresh.until()));
        tag.putString("death","corrupt");assertTrue(MercenaryRest.CODEC.parse(NbtOps.INSTANCE,tag).error().isPresent());
    }
    @Test void onlyActiveSameInstanceGroupMembersMayContribute(){
        String dim="cosmicdungeon:d1_instance_1",nether="cosmicdungeon:d1_instance_1_nether";
        var contract=new MercenaryContract(merc,owner,"theurgist",3,50);
        var run=new DungeonRunRegistryData.RunRecord(1,"dungeon_1","minecraft:overworld",0,List.of(dim,nether),1,
                "ACTIVE","",0,List.of(owner,donor),List.of(),List.of()).withMercenaries(List.of(contract));
        var rest=new MercenaryRest(12000,dim,0,16);
        assertTrue(MercenaryRevival.admitted(run,contract,rest,owner,dim));
        assertTrue(MercenaryRevival.admitted(run,contract,rest,donor,dim));
        assertTrue(MercenaryRevival.admitted(run,contract,rest,donor,nether));
        assertFalse(MercenaryRevival.admitted(run,contract,rest,UUID.randomUUID(),dim));
        assertFalse(MercenaryRevival.admitted(run,contract,rest,donor,"minecraft:overworld"));
        assertFalse(MercenaryRevival.admitted(run.withCompletionExited(donor),contract,rest,donor,dim));
        assertFalse(MercenaryRevival.admitted(run.withCompletionExited(owner),contract,rest,donor,dim));
        assertFalse(MercenaryRevival.admitted(run.withState(DungeonRunState.RESETTING,DungeonResetReason.ABANDONED),contract,rest,donor,dim));
        assertFalse(MercenaryRevival.admitted(run,contract,null,donor,dim));
        assertFalse(MercenaryRevival.admitted(run,null,rest,donor,dim));
    }
    @Test void helpEligibilityIsRecheckedAfterFundsOrAccountReadinessChange(){
        assertTrue(MercenaryRevival.helpAvailable(24,25,true));
        assertFalse(MercenaryRevival.helpAvailable(25,25,true));
        assertFalse(MercenaryRevival.helpAvailable(50,25,true));
        assertFalse(MercenaryRevival.helpAvailable(0,25,false));
        assertFalse(MercenaryRevival.helpAvailable(0,0,true));
    }
    @Test void regenerationWaits15SecondsThenHealsHalfAPointPerSecond(){
        var timer=new MercenaryRegeneration();
        for(int i=0;i<30;i++)assertEquals(0,timer.advance(10,false));
        for(int i=0;i<8;i++){assertEquals(0,timer.advance(10,false));assertEquals(.5F,timer.advance(10,false));}
    }
    @Test void attacksAndDamageResetQuietAndPartialHealingTime(){
        var timer=new MercenaryRegeneration();for(int i=0;i<31;i++)timer.advance(10,false);
        assertEquals(0,timer.advance(10,true));
        for(int i=0;i<31;i++)assertEquals(0,timer.advance(10,false));
        timer.combat();for(int i=0;i<15;i++)assertEquals(0,timer.advance(20,false));
        assertEquals(.5F,timer.advance(20,false));
        assertThrows(IllegalArgumentException.class,()->timer.advance(1000,false));
    }
    @Test void recoveryControlsAndInsufficientPromptRoundTripWithExactDeathIdentity(){
        var row=new PartyPayloads.Mercenary("Edmund","Hirer",0,0,500,"RESPAWNING",
                new PartyPayloads.Recovery(merc.toString(),12000,25,true));
        var prompt=new PartyPayloads.RevivePrompt(merc.toString(),12000,"Edmund");
        var buf=Unpooled.buffer();try{
            PartyPayloads.Mercenary.CODEC.encode(buf,row);assertEquals(row,PartyPayloads.Mercenary.CODEC.decode(buf));
            PartyPayloads.RevivePrompt.STREAM_CODEC.encode(buf,prompt);assertEquals(prompt,PartyPayloads.RevivePrompt.STREAM_CODEC.decode(buf));
            assertEquals(0,buf.readableBytes());
        }finally{buf.release();}
    }
}
