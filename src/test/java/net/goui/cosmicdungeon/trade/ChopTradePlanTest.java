package net.goui.cosmicdungeon.trade;
import java.util.UUID;
import net.goui.cosmicdungeon.dungeon.ChopOwnershipData;
import net.goui.cosmicdungeon.dungeon.DungeonReturnTarget;
import net.goui.cosmicdungeon.component.ModDataComponents;
import net.minecraft.nbt.*;
import net.minecraft.world.item.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ChopTradePlanTest {
    private final UUID a=UUID.randomUUID(),b=UUID.randomUUID(),tx=UUID.randomUUID(),session=UUID.randomUUID();
    private CompoundTag images() {
        var result=new CompoundTag();result.putInt("schema",1);result.putBoolean("applied",false);
        var before=ChopOwnershipData.issuedImage(UUID.randomUUID());
        result.put("first_before",before);result.put("second_before",new CompoundTag());
        result.put("first_after",new CompoundTag());result.put("second_after",before.copy());return result;
    }
    private TradeCommitPlan plan(CompoundTag chops) {
        return new TradeCommitPlan(a,b,TradeCustodyImages.bind(TradeCustodyImages.open(session,a,b,0),tx),
                TradeCustodyImages.bind(TradeCustodyImages.open(session,b,a,0),tx),false,false,chops);
    }
    @Test void registryProofSurvivesCodecAndIndependentOwnerAcknowledgements() {
        var before=images();var plan=plan(before);before.getCompoundOrEmpty("first_before").putString("token","mutated source");
        var encoded=TradeCommitPlan.CODEC.encodeStart(NbtOps.INSTANCE,plan.chopsApplied()).getOrThrow();
        var loaded=TradeCommitPlan.CODEC.parse(NbtOps.INSTANCE,encoded).getOrThrow();
        assertTrue(loaded.chops().getBooleanOr("applied",false));
        assertTrue(loaded.acknowledge(a).chops().getBooleanOr("applied",false));
        assertFalse(plan.chops().getBooleanOr("applied",false));
        assertEquals(plan.chops().getCompoundOrEmpty("first_before"),loaded.chops().getCompoundOrEmpty("second_after"));
    }
    @Test void oldOrdinaryPlansLoadAndMalformedFutureOrDuplicatingTransfersFailClosed() {
        var old=(CompoundTag)TradeCommitPlan.CODEC.encodeStart(NbtOps.INSTANCE,plan(new CompoundTag())).getOrThrow();old.remove("chops");
        assertTrue(TradeCommitPlan.CODEC.parse(NbtOps.INSTANCE,old).getOrThrow().chops().isEmpty());
        var invalid=images();invalid.putInt("schema",2);assertThrows(IllegalArgumentException.class,()->plan(invalid));
        var duplicate=images();duplicate.put("first_after",duplicate.getCompoundOrEmpty("first_before").copy());
        assertThrows(IllegalArgumentException.class,()->plan(duplicate));
    }
    @Test void projectionTransfersReturnIdentityOnACopyAndPreservesDestination() {
        var original=new ItemStack(Items.COOKED_PORKCHOP);
        original.set(ModDataComponents.CHOP_OWNER.get(),a);
        original.set(ModDataComponents.CHOP_TOKEN.get(),UUID.randomUUID());
        var target=new DungeonReturnTarget(a,42,"minecraft:overworld",1,2,3,4,5);
        original.set(ModDataComponents.DUNGEON_RETURN_TARGET.get(),target);
        var copied=TradeItemTransfer.forRecipient(original,a,b);
        assertEquals(a,original.get(ModDataComponents.CHOP_OWNER.get()));assertEquals(target,original.get(ModDataComponents.DUNGEON_RETURN_TARGET.get()));
        assertEquals(b,copied.get(ModDataComponents.CHOP_OWNER.get()));
        assertEquals(new DungeonReturnTarget(b,42,"minecraft:overworld",1,2,3,4,5),copied.get(ModDataComponents.DUNGEON_RETURN_TARGET.get()));
        assertEquals(original.get(ModDataComponents.CHOP_TOKEN.get()),copied.get(ModDataComponents.CHOP_TOKEN.get()));
    }
}
