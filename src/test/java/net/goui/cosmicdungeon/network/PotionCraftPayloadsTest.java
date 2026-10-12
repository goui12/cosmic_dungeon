package net.goui.cosmicdungeon.network;
import java.util.*;
import io.netty.buffer.Unpooled;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.*;
import net.goui.cosmicdungeon.network.PotionCraftPayloads.*;
import static net.goui.cosmicdungeon.network.PotionCraftPayloads.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
final class PotionCraftPayloadsTest {
    @Test void exactActionAndOpenCloseWireRoundTrip() {
        for (boolean shift : new boolean[] {false, true}) {
            var action = new PotionCraftPayloads.Action(32, 71, "minecraft:strong_healing_splash", shift);
            var b = Unpooled.buffer();
            try { PotionCraftPayloads.Action.STREAM_CODEC.encode(b, action); assertEquals(action, PotionCraftPayloads.Action.STREAM_CODEC.decode(b)); assertEquals(0, b.readableBytes()); }
            finally { b.release(); }
            b = Unpooled.buffer();
            try { var request = new PotionCraftPayloads.Request(shift); PotionCraftPayloads.Request.STREAM_CODEC.encode(b, request);
                assertEquals(request, PotionCraftPayloads.Request.STREAM_CODEC.decode(b)); }
            finally { b.release(); }
        }
    }
    @Test void invalidGenerationsTargetBoundsAndQuoteCountsFailBeforeAllocation() {
        assertThrows(IllegalArgumentException.class, () -> new PotionCraftPayloads.Action(0, 0, "minecraft:healing_splash", false));
        assertThrows(IllegalArgumentException.class, () -> new PotionCraftPayloads.Action(1, -1, "minecraft:healing_splash", false));
        for (var id : List.of("no namespace", "minecraft:" + "x".repeat(129), "minecraft:../bad?"))
            assertThrows(IllegalArgumentException.class, () -> new PotionCraftPayloads.Action(1, 0, id, false));
        assertThrows(IllegalArgumentException.class, () -> new PotionCraftPayloads.View(1, 1, "judicator", 0, 20, List.of()));
        assertThrows(IllegalArgumentException.class, () -> new PotionCraftPayloads.View(0, 1, "theurgist", 0, 20, List.of()));
        assertThrows(IllegalArgumentException.class, () -> new PotionCraftPayloads.View(1, 1, "theurgist", 20, 20, List.of()));
        var raw = Unpooled.buffer();
        try {
            var b = new RegistryFriendlyByteBuf(raw, RegistryAccess.EMPTY);
            b.writeVarLong(1); b.writeVarLong(1); b.writeUtf("theurgist"); b.writeVarInt(0); b.writeVarInt(20); b.writeVarInt(Integer.MAX_VALUE);
            assertThrows(IllegalArgumentException.class, () -> PotionCraftPayloads.View.STREAM_CODEC.decode(b));
        } finally { raw.release(); }
    }
    @Test void inactiveViewWireRequiresNoRegistryItems() {
        var raw = Unpooled.buffer();
        try { var b = new RegistryFriendlyByteBuf(raw, RegistryAccess.EMPTY); var empty = PotionCraftPayloads.View.empty(91);
            PotionCraftPayloads.View.STREAM_CODEC.encode(b, empty); assertEquals(empty, PotionCraftPayloads.View.STREAM_CODEC.decode(b)); }
        finally { raw.release(); }
    }

    private static ItemStack output(){return PotionContents.createItemStack(Items.SPLASH_POTION,Potions.HEALING);}
    private static Material input(int own,int shared){return new Material("potion/minecraft:potion/minecraft:healing",1,own,shared);}
    private static Material item(String id,int count,int own,int shared){return new Material("item/minecraft:"+id,count,own,shared);}
    private static Entry entry(int own,int shared,int steps,int powders,List<Material> rows) {
        return new Entry("minecraft:healing_splash",output(),own,shared,steps,powders,rows);
    }
    @Test void canonicalStockIdentitiesAndEveryMaterialQuantityBoundaryAreBounded() {
        assertEquals("item/minecraft:blaze_powder",item("blaze_powder",17,4096,4096).key());
        assertEquals(4096,input(4096,4096).shared());
        for(String key:List.of("", "minecraft:blaze_powder", "item/no namespace", "item/minecraft:../bad?",
                "item/minecraft:blaze_powder/extra", "potion/minecraft:potion", "potion/minecraft:potion/minecraft:water/extra",
                "item/minecraft:"+"x".repeat(129), "potion/minecraft:"+ "x".repeat(129)+"/minecraft:water")) {
            assertThrows(IllegalArgumentException.class,()->new Material(key,1,0,1),key);
        }
        for(int needed:new int[]{0,18})assertThrows(IllegalArgumentException.class,()->item("blaze_powder",needed,4096,4096));
        for(int own:new int[]{-1,4097})assertThrows(IllegalArgumentException.class,()->input(own,4096));
        assertThrows(IllegalArgumentException.class,()->input(2,1));
        assertThrows(IllegalArgumentException.class,()->input(0,4097));
    }
    @Test void selectedRouteRequiresOneExactPotionInputAndMatchingStepFuelDebits() {
        var valid=entry(1,1,1,1,List.of(input(1,1),item("gunpowder",1,1,1),item("blaze_powder",1,1,1)));
        assertEquals(3,valid.materials().size());
        assertThrows(IllegalArgumentException.class,()->entry(1,1,1,1,List.of()));
        assertThrows(IllegalArgumentException.class,()->entry(1,1,1,0,List.of(item("gunpowder",1,1,1),input(1,1))));
        assertThrows(IllegalArgumentException.class,()->entry(1,1,1,0,List.of(
                new Material("potion/minecraft:potion/minecraft:healing",2,2,2),item("gunpowder",1,1,1))));
        assertThrows(IllegalArgumentException.class,()->entry(1,1,1,0,List.of(input(1,1),input(1,1))));
        assertThrows(IllegalArgumentException.class,()->entry(1,1,2,0,List.of(input(1,1),item("gunpowder",1,1,1))));
        assertThrows(IllegalArgumentException.class,()->entry(1,1,1,1,List.of(input(1,1),item("gunpowder",2,2,2))));
        assertThrows(IllegalArgumentException.class,()->entry(1,1,1,0,List.of(input(0,1),item("gunpowder",1,1,1))));
        assertThrows(IllegalArgumentException.class,()->entry(0,1,1,0,List.of(input(0,0),item("gunpowder",1,0,1))));
        assertThrows(IllegalArgumentException.class,()->entry(0,0,1,0,List.of()));
        assertThrows(IllegalArgumentException.class,()->entry(0,0,0,0,List.of(input(0,1))));
        assertDoesNotThrow(()->entry(0,0,0,0,List.of()));
    }
    @Test void maximumRowsCountsAndDefensiveCopiesKeepQuotesBounded() {
        var rows=new ArrayList<Material>();rows.add(input(4096,4096));
        for(int i=0;i<16;i++)rows.add(item("fixture_"+i,1,4096,4096));
        rows.add(item("blaze_powder",1,4096,4096));
        var max=entry(64,64,16,1,rows);
        assertEquals(MAX_MATERIALS,max.materials().size());
        rows.clear();assertEquals(18,max.materials().size(),"Constructor copies source rows");
        assertThrows(UnsupportedOperationException.class,()->max.materials().clear());
        var stack=max.output();stack.setCount(9);assertEquals(1,max.output().getCount());
        var overflow=new ArrayList<>(max.materials());overflow.add(item("extra",1,4096,4096));
        assertThrows(IllegalArgumentException.class,()->entry(64,64,16,1,overflow));
        var all=new ArrayList<Entry>();
        for(int i=0;i<MAX_ENTRIES;i++)all.add(new Entry("minecraft:fixture_"+i,output(),64,64,16,1,max.materials()));
        var view=new View(1,0,"theurgist",0,20,all);assertEquals(MAX_ENTRIES,view.entries().size());
        all.add(new Entry("minecraft:overflow",output(),64,64,16,1,max.materials()));
        assertThrows(IllegalArgumentException.class,()->new View(1,0,"theurgist",0,20,all));
        assertThrows(IllegalArgumentException.class,()->new View(1,0,"theurgist",0,20,List.of(max,max)));
    }
}
