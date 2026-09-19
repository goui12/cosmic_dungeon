package net.goui.cosmicdungeon.economy;

import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import java.math.*;
import java.util.*;

/** Economy Internal 17ufIuIy0VhLmB_V-6sZ7sCaUCZuGZUkHrgJLVpEcS28, 2026-08-18:
 * one recoverable logical drop, ordinary ItemEntity age/lifespan, whole-amount collection.
 * The entity image and account decision share one save; the world entity is only a projection. */
public record DeathCurrencyRecord(UUID id,UUID owner,long before,long amount,long run,long created,
                                  String dimension,CompoundTag image,boolean active) {
    public static final String MARKER="cosmic_death_trace_v1";
    public DeathCurrencyRecord {
        Objects.requireNonNull(id);Objects.requireNonNull(owner);Objects.requireNonNull(image);
        if(before<0||amount<0||amount>before||run<0||created<0||(active&&amount==0))
            throw new IllegalArgumentException("Invalid death currency terms");
        ResourceLocation.parse(dimension);
        validateImage(id,image);image=image.copy();
    }
    @Override public CompoundTag image(){return image.copy();}
    public static long loss(long balance,long threshold,double fraction,long minimum){
        if(balance<0||threshold<0||minimum<0||!Double.isFinite(fraction)||fraction<0||fraction>1)
            throw new IllegalArgumentException("Invalid death loss settings");
        if(balance<threshold)return 0;
        long percent=BigDecimal.valueOf(balance).multiply(BigDecimal.valueOf(fraction)).setScale(0,RoundingMode.FLOOR).longValueExact();
        return Math.min(balance,Math.max(minimum,percent));
    }
    private static void validateImage(UUID id,CompoundTag image){
        if(!image.getStringOr("death_projection_id","").equals(id.toString())
                ||!image.getCompoundOrEmpty("NeoForgeData").getStringOr(MARKER,"").equals(id.toString())
                ||!image.read("UUID",net.minecraft.core.UUIDUtil.CODEC).orElseThrow().equals(id)
                ||!(image.get("Age") instanceof NumericTag)||image.getShortOr("Age",(short)-1)<0
                ||!(image.get("Lifespan") instanceof NumericTag)||image.getIntOr("Lifespan",-1)<0
                ||!(image.get("PickupDelay") instanceof NumericTag)||image.getShortOr("PickupDelay",(short)-1)<0
                ||!(image.get("Health") instanceof NumericTag)||!(image.get("Item") instanceof CompoundTag)
                ||image.contains("Owner")||image.contains("Passengers"))
            throw new IllegalArgumentException("Invalid ordinary death drop image");
        var stack=image.getCompoundOrEmpty("Item");
        if(!stack.getStringOr("id","").equals("cosmicdungeon:attunement_trace")||stack.getIntOr("count",1)!=1
                ||!stack.getCompoundOrEmpty("components").getCompoundOrEmpty("minecraft:custom_data").getStringOr(MARKER,"").equals(id.toString()))
            throw new IllegalArgumentException("Death visual is not the bound single Trace projection");
        var pos=image.getListOrEmpty("Pos");
        if(pos.size()!=3)throw new IllegalArgumentException("Missing death position");
        for(int i=0;i<3;i++)if(!Double.isFinite(pos.getDouble(i).orElseThrow()))throw new IllegalArgumentException("Nonfinite death position");
    }
    public DeathCurrencyRecord activate(){return new DeathCurrencyRecord(id,owner,before,amount,run,created,dimension,image,true);}
    public DeathCurrencyRecord snapshot(String dim,CompoundTag nativeImage){
        return new DeathCurrencyRecord(id,owner,before,amount,run,created,dim,nativeImage,active);
    }
    public CompoundTag save(){
        var tag=new CompoundTag();tag.putString("id",id.toString());tag.putString("owner",owner.toString());
        tag.putLong("before",before);tag.putLong("amount",amount);tag.putLong("run",run);tag.putLong("created",created);
        tag.putString("dimension",dimension);tag.put("image",image.copy());tag.putBoolean("active",active);return tag;
    }
    public static DeathCurrencyRecord load(CompoundTag tag){
        for(String key:List.of("before","amount","run","created","active"))if(!(tag.get(key) instanceof NumericTag))
            throw new IllegalArgumentException("Missing death record number "+key);
        return new DeathCurrencyRecord(UUID.fromString(tag.getStringOr("id","")),UUID.fromString(tag.getStringOr("owner","")),
                tag.getLongOr("before",-1),tag.getLongOr("amount",-1),tag.getLongOr("run",-1),tag.getLongOr("created",-1),
                tag.getStringOr("dimension",""),tag.getCompound("image").orElseThrow(),tag.getBooleanOr("active",false));
    }
}
