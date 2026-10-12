package net.goui.cosmicdungeon.playerclass.bogatyr;
import java.util.UUID;
import net.minecraft.nbt.*;
/** New selections overlay, never rewrite, legacy selections. Unknown save shapes remain held. */
final class WolfModeData {
    static final String KEY="wolf_pack_modes",LEGACY="wolf_modes";
    record State(WolfMode mode,boolean writable){}
    private static final State DEFAULT=new State(WolfMode.PATROL,true),HOLD=new State(WolfMode.STAY,false);
    private WolfModeData(){}
    static State read(CompoundTag root,UUID owner,long run){
        if(owner==null||run<=0)return HOLD;
        String id=owner+"/"+run;
        if(root.contains(KEY)){
            if(!(root.get(KEY) instanceof CompoundTag modes)||modes.getIntOr("schema",-1)!=2)return HOLD;
            if(modes.contains(id)){
                if(!(modes.get(id) instanceof CompoundTag value)||!(value.get("epoch") instanceof LongTag)
                        ||value.getLongOr("epoch",-1)<0)return HOLD;
                try{var mode=WolfMode.valueOf(value.getStringOr("mode",""));
                    return mode.active()?new State(mode,true):HOLD;
                }catch(IllegalArgumentException unknown){return HOLD;}
            }
        }
        if(root.contains(LEGACY)){
            if(!(root.get(LEGACY) instanceof CompoundTag modes)||modes.getIntOr("schema",-1)!=1)return HOLD;
            if(modes.contains(id)){
                if(!(modes.get(id) instanceof CompoundTag value))return HOLD;
                try{if(WolfMode.valueOf(value.getStringOr("mode","")).active())return HOLD;}
                catch(IllegalArgumentException unknown){return HOLD;}
            }
        }
        return DEFAULT;
    }
    static long epoch(CompoundTag root,UUID owner,long run){
        return root.getCompoundOrEmpty(KEY).getCompoundOrEmpty(owner+"/"+run).getLongOr("epoch",0);
    }
    static boolean write(CompoundTag root,UUID owner,long run,WolfMode mode){
        if(mode==null||!mode.active()||!read(root,owner,run).writable())return false;
        if(read(root,owner,run).mode()==mode)return true;
        long previous=epoch(root,owner,run);if(previous==Long.MAX_VALUE)return false;
        var modes=root.getCompoundOrEmpty(KEY).copy();modes.putInt("schema",2);
        String id=owner+"/"+run;var value=modes.getCompoundOrEmpty(id).copy();
        value.putString("mode",mode.name());value.putLong("epoch",previous+1);
        modes.put(id,value);root.put(KEY,modes);return true;
    }
}
