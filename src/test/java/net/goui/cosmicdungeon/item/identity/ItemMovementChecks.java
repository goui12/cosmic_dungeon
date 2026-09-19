package net.goui.cosmicdungeon.item.identity;

import java.util.Map;

public final class ItemMovementChecks {
    private static int checks;
    private static void check(boolean ok,String label){checks++;if(!ok)throw new AssertionError(label);}
    public static void main(String[] args){
        check(!ItemMovementPolicy.mayDrop(true),"Class/no-drop cannot be thrown");
        check(ItemMovementPolicy.mayDrop(false),"Droppable dungeon loot retains environmental loss");
        check(!ItemMovementPolicy.mayInsert(true,false),"Shared chest rejects a restricted item");
        check(ItemMovementPolicy.mayInsert(true,true),"Owner inventory/private service remains usable");
        check(ItemMovementPolicy.mayInsert(false,false),"Ordinary loot can use ordinary containers");
        check(!ItemMovementPolicy.mayNest(true,true),"Portable container cannot hide restricted gear");
        check(ItemMovementPolicy.mayNest(false,true),"Ordinary bundle contents remain usable");
        check(ItemMovementPolicy.mayNest(true,false),"Normal own-inventory rearrangement remains usable");
        check(!ItemMovementPolicy.mayShift(true,true,false),"Unknown quick-move route cannot leak protected input");
        check(!ItemMovementPolicy.mayShift(true,false,false),"Unknown multi-container quick-move needs deliberate pickup");
        check(ItemMovementPolicy.mayShift(true,true,true),"Own inventory/ender chest shift remains usable");
        check(ItemMovementPolicy.mayShift(false,true,false),"Ordinary shift-click remains usable");
        var before=Map.of("item","minecraft:bow","count","1","damage","37","lore","authored");
        var after=Map.of("item","minecraft:bow","count","1","damage","37","lore","authored","identity","loophole");
        var p=new ItemAuthoringPlan<>("token",100,before,after);
        check(!p.applied(),"Preview creates no applied state");
        check(p.image(false).equals(after)&&p.image(true).equals(before),"Exact original and replacement images retained");
        check(p.accepts("token",99,true,before,false,Map::equals),"Exact live preview may apply");
        check(!p.accepts("other",99,true,before,false,Map::equals),"Other user's token rejected");
        check(!p.accepts("token",100,true,before,false,Map::equals),"Expiry boundary rejected");
        check(!p.accepts("token",99,false,before,false,Map::equals),"Moved/replaced container rejected");
        check(!p.accepts("token",99,true,Map.of("item","minecraft:bow"),false,Map::equals),"Missing/changed component rejected");
        check(!p.accepts("token",99,true,before,true,Map::equals),"Undo before apply rejected");
        p.committed(false);
        check(p.applied(),"Successful adapter mutation advances state");
        check(!p.accepts("token",99,true,before,false,Map::equals),"Repeated apply rejected");
        check(p.accepts("token",99,true,after,true,Map::equals),"Exact post-image can undo");
        check(!p.accepts("token",99,true,before,true,Map::equals),"Modified post-image cannot be overwritten");
        p.committed(true);
        check(!p.applied(),"Undo restores phase; adapter removes its token");
        check(!p.accepts("token",99,true,after,true,Map::equals),"Repeated undo rejected");
        check(!p.accepts("token",99,true,before,false,Map::equals),"An undone plan cannot reapply even if retained");
        System.out.println(checks+" item movement/adoption checks passed");
    }
}
