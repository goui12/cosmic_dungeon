package net.goui.cosmicdungeon.spawner.editor;

import net.goui.cosmicdungeon.block.entity.CosmicSpawnerBlockEntity;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;

/** Field-specific edits on a detached server snapshot; no authored stacks or weighted potentials rebuilt. */
public final class SpawnerEditorConfig {
    private SpawnerEditorConfig() {}
    private static CompoundTag child(CompoundTag t,String k){return t.getCompound(k).orElseGet(CompoundTag::new);}
    public static SpawnerEditorDraft read(CosmicSpawnerBlockEntity be,CompoundTag data){
        var preset=be.getSpawnerPreset();var entity=child(child(data,"SpawnData"),"entity");
        var flags=child(child(data,"SpawnerPreset"),"entityFlags");
        String name=preset!=null&&(preset.getCustomName()!=null||child(data,"SpawnerPreset").getBooleanOr("nameOverride",false))
                ?(preset.getCustomName()==null?"":preset.getCustomName().getString()):
                entity.read("CustomName",ComponentSerialization.CODEC).map(Component::getString).orElse("");
        boolean captain=preset!=null&&preset.isIllagerCaptainVariant();
        return new SpawnerEditorDraft(be.getSpawnerEntityId(),name,captain,be.isBossOneShot(),
                be.getSpawnerMobCap(),be.getSpawnerMinSpawnDelay(),be.getSpawnerMaxSpawnDelay(),
                flag(flags,entity,"Silent",false),flag(flags,entity,"NoAI",false),
                flag(flags,entity,"Invulnerable",false),flag(flags,entity,"PersistenceRequired",captain),
                flag(flags,entity,"CustomNameVisible",false));
    }
    private static boolean flag(CompoundTag f,CompoundTag e,String k,boolean fallback){
        return f.getBoolean(k).orElseGet(()->e.getBooleanOr(k,fallback));
    }
    public static CompoundTag comparable(CompoundTag source){
        var result=source.copy();result.remove("Delay");result.remove("CosmicSpawnBlocked");return result;
    }
    public static CompoundTag patch(CompoundTag source,SpawnerEditorDraft before,SpawnerEditorDraft after){
        if(!after.error().isEmpty())throw new IllegalArgumentException(after.error());
        var out=source.copy();
        if(before.boss()!=after.boss()){out.putBoolean("BossOneShot",after.boss());if(after.boss())out.putBoolean("BossHasSpawned",false);}
        if(before.cap()!=after.cap())out.putInt("SpawnerMobCap",after.cap());
        if(before.minDelay()!=after.minDelay())out.putInt("MinSpawnDelay",after.minDelay());
        if(before.maxDelay()!=after.maxDelay())out.putInt("MaxSpawnDelay",after.maxDelay());
        boolean entityChanged=!before.entityId().equals(after.entityId());
        boolean nameChanged=!before.name().equals(after.name());
        boolean traits=entityChanged||nameChanged||before.captain()!=after.captain()||
                before.silent()!=after.silent()||before.noAi()!=after.noAi()||
                before.invulnerable()!=after.invulnerable()||before.persistent()!=after.persistent()||
                before.nameVisible()!=after.nameVisible();
        if(!traits)return out;
        var preset=preset(out,after.entityId());
        var spawn=child(out,"SpawnData");var entity=child(spawn,"entity");
        if(entityChanged){
            out.putString("SpawnerEntityId",after.entityId());preset.putString("entityType",after.entityId());
            entity.putString("id",after.entityId());
        }
        if(nameChanged){
            preset.putBoolean("nameOverride",true);
            if(after.name().isEmpty()){preset.remove("customName");entity.remove("CustomName");}
            else{
                var name=Component.literal(after.name());
                preset.store("customName",ComponentSerialization.flatRestrictedCodec(8192),name);
                entity.store("CustomName",ComponentSerialization.CODEC,name);
            }
        }
        if(before.captain()!=after.captain()){
            preset.putBoolean("illagerCaptainVariant",after.captain());preset.putBoolean("captainOverride",true);
        }
        var flags=child(preset,"entityFlags");
        if(before.captain()!=after.captain()) flags.putBoolean("PersistenceRequired",after.persistent());
        put(flags,entity,"Silent",before.silent(),after.silent());
        put(flags,entity,"NoAI",before.noAi(),after.noAi());
        put(flags,entity,"Invulnerable",before.invulnerable(),after.invulnerable());
        put(flags,entity,"PersistenceRequired",before.persistent(),after.persistent());
        put(flags,entity,"CustomNameVisible",before.nameVisible(),after.nameVisible());
        if(!flags.isEmpty())preset.put("entityFlags",flags);
        if(entityChanged||nameChanged){spawn.put("entity",entity);out.put("SpawnData",spawn);}
        out.put("SpawnerPreset",preset);return out;
    }
    static CompoundTag preset(CompoundTag out,String entityId){
        var preset=child(out,"SpawnerPreset");
        if(!out.contains("SpawnerPreset")){
            preset.putInt("presetVersion",5);preset.putInt("equipmentMask",0);preset.putInt("dropMask",0);
            preset.putBoolean("intrinsicDropsOverride",false);preset.putString("entityType",entityId);
        }
        return preset;
    }
    private static void put(CompoundTag flags,CompoundTag entity,String key,boolean old,boolean value){
        if(old!=value){flags.putBoolean(key,value);entity.putBoolean(key,value);}
    }
}
