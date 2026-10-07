package net.goui.cosmicdungeon.playerclass.bogatyr;

import java.util.UUID;
import net.minecraft.nbt.*;

/** Additive owner/run state; old saves default to Defensive, unknown future state stays untouched and passive. */
final class WolfModeData {
    static final String KEY="wolf_modes";
    record State(WolfMode mode,boolean writable){}
    private static final State DEFAULT=new State(WolfMode.DEFENSIVE,true),HOLD=new State(WolfMode.STAND_GROUND,false);
    private WolfModeData(){}
    static State read(CompoundTag root,UUID owner,long run){
        if(owner==null||run<=0)return HOLD;
        if(!root.contains(KEY))return DEFAULT;
        if(!(root.get(KEY) instanceof CompoundTag modes)||modes.getIntOr("schema",-1)!=1)return HOLD;
        String key=owner+"/"+run;
        if(!modes.contains(key))return DEFAULT;
        if(!(modes.get(key) instanceof CompoundTag value))return HOLD;
        try{return new State(WolfMode.valueOf(value.getStringOr("mode","")),true);}
        catch(IllegalArgumentException unknown){return HOLD;}
    }
    static boolean write(CompoundTag root,UUID owner,long run,WolfMode mode){
        if(mode==null||!read(root,owner,run).writable())return false;
        var modes=root.getCompoundOrEmpty(KEY).copy();modes.putInt("schema",1);
        String key=owner+"/"+run;var value=modes.getCompoundOrEmpty(key).copy();
        value.putString("mode",mode.name());modes.put(key,value);root.put(KEY,modes);return true;
    }
}
