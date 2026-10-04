package net.goui.cosmicdungeon.mercenary;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.*;

/** Per-mercenary/run receipts: a copied container slot cannot replenish on revisit or reload. */
public final class MercenaryLootMemory {
    static final int MAX_CLAIMS=4096;
    private record Claim(ResourceLocation dimension,long position,int slot){
        static final Codec<Claim> CODEC=RecordCodecBuilder.create(i->i.group(
                ResourceLocation.CODEC.fieldOf("dimension").forGetter(Claim::dimension),
                Codec.LONG.fieldOf("position").forGetter(Claim::position),
                Codec.intRange(0,255).fieldOf("slot").forGetter(Claim::slot)).apply(i,Claim::new));
    }
    private static final Codec<List<Claim>> CODEC=Claim.CODEC.listOf(0,MAX_CLAIMS);
    private final Set<Claim> claims=new LinkedHashSet<>();
    private boolean blocked;
    public boolean available(ResourceLocation dimension,long position,int slot){
        return !blocked&&slot>=0&&slot<256&&claims.size()<MAX_CLAIMS&&!claims.contains(new Claim(dimension,position,slot));
    }
    public void remember(ResourceLocation dimension,long position,int slot){
        if(available(dimension,position,slot))claims.add(new Claim(dimension,position,slot));
    }
    public void save(ValueOutput out){
        out.putInt("mercenary_loot_version",1);
        out.putBoolean("mercenary_loot_blocked",blocked);
        out.store("mercenary_loot_claims",CODEC,List.copyOf(claims));
    }
    public static MercenaryLootMemory load(ValueInput in){
        var memory=new MercenaryLootMemory();
        int version=in.getIntOr("mercenary_loot_version",0);
        if(version==0)return memory; // Old entities retain their gear/supplies and start an empty receipt book.
        var saved=in.read("mercenary_loot_claims",CODEC);
        memory.blocked=version!=1||in.getBooleanOr("mercenary_loot_blocked",false)||saved.isEmpty();
        saved.ifPresent(memory.claims::addAll);
        return memory;
    }
}
