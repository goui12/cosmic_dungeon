package net.goui.cosmicdungeon.crafting;

import java.util.UUID;
import net.minecraft.nbt.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BrewingFuelTest {
    private static final UUID OWNER = new UUID(0, 707);
    private static SharedCraftData data() {
        var tag = new CompoundTag(); tag.putInt("schema", 1); tag.putString("future", "retained"); return new SharedCraftData(tag);
    }
    private static SharedCraftPlan plan(CompoundTag fuel, int steps) {
        var tag = new CompoundTag(); tag.putInt("schema", 2); tag.putString("id", UUID.randomUUID().toString());
        tag.putString("recipient", OWNER.toString()); tag.putLong("run", 7); tag.putLong("sequence", 1); tag.putLong("at", 0);
        tag.putLong("donation_before", 0); tag.putLong("donation_after", 0);
        tag.putString("recipe", "minecraft:healing_splash"); tag.putString("skill", "Healing"); tag.putString("recipient_name", "Crafter");
        tag.putBoolean("committed", false); tag.putBoolean("metrics_ack", false); tag.put("donated", new CompoundTag());
        var output = new CompoundTag(); output.putString("fixture", "potion"); tag.put("output", output);
        var owner = new CompoundTag(); owner.putBoolean("ack", false);
        owner.put("before", new CompoundTag()); owner.put("after", new CompoundTag());
        var debit = new CompoundTag(); debit.putString("item", "minecraft:blaze_powder"); debit.putString("ingredient", "fuel"); debit.putInt("count", 1);
        var debits = new CompoundTag(); debits.put("0", debit); owner.put("debits", debits);
        var owners = new CompoundTag(); owners.put(OWNER.toString(), owner); tag.put("owners", owners);
        tag.put("fuel", BrewingFuel.plan(fuel, steps)); return new SharedCraftPlan(tag);
    }
    private static SharedCraftData reload(SharedCraftData data) {
        return SharedCraftData.CODEC.parse(NbtOps.INSTANCE, SharedCraftData.CODEC.encodeStart(NbtOps.INSTANCE, data).getOrThrow()).getOrThrow();
    }
    @Test void everyAllowedStepAndChargeConservesFuel() {
        for (int charges=0; charges<20; charges++) for (int steps=1; steps<=16; steps++) {
            var cost=BrewingFuel.cost(charges,steps);
            assertEquals(charges+20*cost.powders(),steps+cost.remaining());
            assertEquals(charges<steps ? 1 : 0,cost.powders());
            assertTrue(cost.remaining()>=0&&cost.remaining()<20);
        }
        int charges=0,powders=0;
        for(int i=0;i<21;i++){var cost=BrewingFuel.cost(charges,1);charges=cost.remaining();powders+=cost.powders();}
        assertEquals(2,powders);assertEquals(19,charges);
        for(int invalidCharge:new int[]{-1,20}) assertThrows(IllegalArgumentException.class,()->BrewingFuel.cost(invalidCharge,1));
        for(int steps:new int[]{0,17}) assertThrows(IllegalArgumentException.class,()->BrewingFuel.cost(0,steps));
    }
    @Test void preparedCancelUpgradesJournalWithoutSpendingFuel() {
        var data=data(); var p=plan(data.fuel(OWNER),3); data.reserve(p);
        assertEquals(2,data.image().getIntOr("schema",0)); assertEquals(0,BrewingFuel.charges(data.fuel(OWNER)));
        data=reload(data); assertEquals(p.id(),data.pending(OWNER).id()); assertEquals("retained",data.image().getStringOr("future",""));
        data.acknowledge(p.id(),OWNER); data.retire(p.id()); data=reload(data);
        assertNull(data.pending(OWNER)); assertEquals(BrewingFuel.empty(),data.fuel(OWNER));
    }
    @Test void committedReloadAndRetirementKeepChargesAndRejectDoubleCommit() {
        var data=data();var p=plan(data.fuel(OWNER),3);data.reserve(p);data.commit(p.id());
        final var committed=data;assertThrows(IllegalStateException.class,()->committed.commit(p.id()));
        data=reload(data);assertTrue(data.pending(OWNER).committed());assertEquals(17,BrewingFuel.charges(data.fuel(OWNER)));
        assertEquals(1,data.fuel(OWNER).getLongOr("revision",-1));
        data.acknowledge(p.id(),OWNER);data.acknowledgeMetrics(p.id());data.retire(p.id());data=reload(data);
        assertEquals(17,BrewingFuel.charges(data.fuel(OWNER)));assertTrue(data.pending().isEmpty());
        final var restored=data;assertThrows(IllegalStateException.class,()->restored.reserve(plan(BrewingFuel.empty(),3)));
        var second=plan(data.fuel(OWNER),3);data.reserve(second);data.commit(second.id());
        assertEquals(14,BrewingFuel.charges(reload(data).fuel(OWNER)));
    }
    @Test void malformedOrMismatchedLedgerDoesNotAlterOriginal() {
        var data=data();var p=plan(data.fuel(OWNER),3);data.reserve(p);
        var prepared=data.image();prepared.getCompoundOrEmpty(BrewingFuel.KEY).put(OWNER.toString(),p.fuel().getCompoundOrEmpty("after"));
        var copy=prepared.copy();assertThrows(IllegalArgumentException.class,()->new SharedCraftData(prepared));assertEquals(copy,prepared);
        data.commit(p.id());var committed=data.image();committed.getCompoundOrEmpty(BrewingFuel.KEY).remove(OWNER.toString());
        assertThrows(IllegalArgumentException.class,()->new SharedCraftData(committed));
        var future=data.image();future.putInt("schema",3);assertThrows(IllegalArgumentException.class,()->new SharedCraftData(future));
        var bad=data.image();bad.getCompoundOrEmpty(BrewingFuel.KEY).getCompoundOrEmpty(OWNER.toString()).putInt("schema",2);
        assertThrows(IllegalArgumentException.class,()->new SharedCraftData(bad));
    }
    @Test void supportedUnknownFieldsSurviveAndForeignLedgerIsPreserved() {
        var state=BrewingFuel.empty();state.putString("future_record","kept");
        var raw=data().image();raw.putInt("schema",2);var ledger=new CompoundTag();ledger.put(OWNER.toString(),state);
        UUID foreign=new UUID(0,708);var foreignState=BrewingFuel.empty();foreignState.putInt("charges",9);ledger.put(foreign.toString(),foreignState);
        raw.put(BrewingFuel.KEY,ledger);var data=new SharedCraftData(raw);var p=plan(data.fuel(OWNER),3);
        data.reserve(p);data.commit(p.id());data=reload(data);
        assertEquals("kept",data.fuel(OWNER).getStringOr("future_record",""));assertEquals(foreignState,data.fuel(foreign));
        var view=data.fuel(OWNER);view.putInt("charges",0);assertEquals(17,BrewingFuel.charges(data.fuel(OWNER)));
    }
    @Test void invalidTransitionsOverflowAndLegacyCollisionFailClosed() {
        for(int mode=0;mode<5;mode++){
            var p=BrewingFuel.plan(BrewingFuel.empty(),3);
            switch(mode){case 0->p.putInt("schema",2);case 1->p.putLong("steps",3);case 2->p.putInt("powders",0);
                case 3->p.getCompoundOrEmpty("after").putInt("charges",16);default->p.getCompoundOrEmpty("after").putLong("revision",2);}
            assertThrows(IllegalArgumentException.class,()->BrewingFuel.validatePlan(p));
        }
        var max=BrewingFuel.empty();max.putLong("revision",Long.MAX_VALUE);assertThrows(ArithmeticException.class,()->BrewingFuel.plan(max,1));
        var raw=data().image();raw.putString(BrewingFuel.KEY,"unknown legacy value");var data=new SharedCraftData(raw);
        assertThrows(IllegalStateException.class,()->data.reserve(plan(BrewingFuel.empty(),3)));assertEquals(raw,data.image());
    }
    @Test void legacyPendingPlansRemainFuelFreeInUpgradedJournal() {
        var tag=plan(BrewingFuel.empty(),3).image();tag.putInt("schema",1);tag.remove("fuel");var p=new SharedCraftPlan(tag);
        var data=data();data.reserve(p);data=reload(data);data.commit(p.id());data.acknowledge(p.id(),OWNER);data.acknowledgeMetrics(p.id());data.retire(p.id());
        assertEquals(1,data.image().getIntOr("schema",0));assertEquals(BrewingFuel.empty(),data.fuel(OWNER));
    }
}
