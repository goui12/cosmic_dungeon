package net.goui.cosmicdungeon.playerclass.theurgist;

import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerPlayer;
import net.goui.cosmicdungeon.playerclass.api.ClassData;

/** Persistent server game-time deadline. Class changes, deaths and run cleanup never reset it. */
public final class ResurrectionCooldown {
    public static final String KEY="theurgist_resurrection_v1";
    public static final int TICKS=20*180;
    private ResurrectionCooldown(){}
    public static CompoundTag read(CompoundTag root){
        if(root.contains(KEY)&&!(root.get(KEY) instanceof CompoundTag))throw new IllegalArgumentException("Invalid resurrection cooldown");
        var tag=root.getCompoundOrEmpty(KEY).copy();deadline(tag);return tag;
    }
    public static long deadline(CompoundTag tag){
        if(tag.isEmpty())return 0;
        if(!(tag.get("schema") instanceof IntTag)||tag.getIntOr("schema",0)!=1
                ||!(tag.get("ready_at") instanceof LongTag)||tag.getLongOr("ready_at",-1)<0)
            throw new IllegalArgumentException("Unsupported resurrection cooldown; original data preserved");
        return tag.getLongOr("ready_at",0);
    }
    public static int seconds(CompoundTag tag,long now){
        if(now<0)throw new IllegalArgumentException("Negative server time");
        long end=deadline(tag);return end<=now?0:(int)Math.min(180,1+(end-now-1)/20);
    }
    public static CompoundTag start(CompoundTag before,long now){
        if(seconds(before,now)!=0)throw new IllegalStateException("Resurrection is cooling down");
        var next=before.copy();next.putInt("schema",1);next.putLong("ready_at",Math.addExact(now,TICKS));return next;
    }
    public static int seconds(ServerPlayer player){
        return seconds(read(player.getPersistentData().getCompoundOrEmpty(ClassData.ROOT_TAG)),
                player.level().getServer().overworld().getGameTime());
    }
}
