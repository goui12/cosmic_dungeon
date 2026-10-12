package net.goui.cosmicdungeon.spawner.editor;
import java.util.*;
import net.goui.cosmicdungeon.block.entity.*;
import net.goui.cosmicdungeon.block.entity.CosmicSpawnerPreset.Slot;
import net.goui.cosmicdungeon.spawner.editor.SpawnerLootDraft.*;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.TagValueInput;

public final class SpawnerLootConfig {
    private SpawnerLootConfig(){}
    public static Snapshot snapshot(CompoundTag source,String entityId,MinecraftServer server){
        var tag=source.getCompoundOrEmpty("SpawnerPreset");
        var preset=source.contains("SpawnerPreset")?CosmicSpawnerPreset.load(TagValueInput.create(ProblemReporter.DISCARDING,server.registryAccess(),tag)):new CosmicSpawnerPreset();
        int mask=source.contains("SpawnerPreset")?tag.getIntOr("dropMask",63):0;
        var gear=new ArrayList<Float>();
        for(var slot:Slot.values())gear.add((mask&(1<<slot.ordinal()))!=0?preset.getDropChance(slot):-1F);
        var rules=preset.getConfiguredIntrinsicDropRuleList().stream().map(r->new Rule(r.id(),r.itemId().toString(),r.chance(),r.count(),r.kind())).toList();
        var summary=CosmicSpawnerLootTableInspector.inspect(server,ResourceLocation.parse(entityId));
        var defaults=summary.defaultDrops().stream().limit(256).map(d->new DefaultDrop(d.itemId().toString(),
                d.chance().numeric()?String.format(Locale.ROOT,"%.2f%% base; loot context may alter results",d.chance().chance()*100):
                        d.chance().label()+"; no fixed percentage")).toList();
        String note=summary.missingReason().orElse("Active default loot table. Overrides replace matching native loot and bypass its conditions; custom additions are independent.");
        return new Snapshot(gear,rules,defaults,note,entityId);
    }
    private static boolean unchanged(CompoundTag raw,Rule row){
        if(raw.isEmpty())return false;
        var id=ResourceLocation.tryParse(raw.getStringOr("item",""));if(id==null)return false;
        CosmicSpawnerPreset.IntrinsicDropRule.Kind kind;
        try{kind=CosmicSpawnerPreset.IntrinsicDropRule.Kind.valueOf(raw.getStringOr("kind","UNKNOWN_CONFIGURED"));}
        catch(IllegalArgumentException ex){kind=CosmicSpawnerPreset.IntrinsicDropRule.Kind.UNKNOWN_CONFIGURED;}
        var normalized=new CosmicSpawnerPreset.IntrinsicDropRule(raw.getStringOr("id",row.id()),id,raw.getFloatOr("chance",1),raw.getIntOr("count",1),kind);
        return row.equals(new Rule(normalized.id(),normalized.itemId().toString(),normalized.chance(),normalized.count(),normalized.kind()));
    }
    public static CompoundTag patch(CompoundTag source,String entityId,SpawnerLootDraft draft){
        for(float chance:draft.gear())if(!Float.isFinite(chance)||(chance!=-1&&(chance<0||chance>1)))
            throw new IllegalArgumentException("Gear percentages must be 0–100.");
        var ids=new HashSet<String>();
        if(draft.replaceRules())for(var rule:draft.rules()){
            if(!rule.id().matches("[A-Za-z0-9_-]{1,64}")||!ids.add(rule.id()))throw new IllegalArgumentException("Loot rule IDs must be unique and valid.");
            if(unchanged(source.getCompoundOrEmpty("SpawnerPreset").getCompoundOrEmpty("intrinsicDropRules").getCompoundOrEmpty(rule.id()),rule))continue;
            var id=ResourceLocation.tryParse(rule.itemId());var item=id==null?null:BuiltInRegistries.ITEM.getOptional(id).orElse(null);
            if(item==null||item==Items.AIR)throw new IllegalArgumentException("Choose a registered loot item.");
            if(!Float.isFinite(rule.chance())||rule.chance()<0||rule.chance()>1||rule.count()<1||rule.count()>64)
                throw new IllegalArgumentException("Loot requires 0–100% and count1–64.");
        }
        if(!draft.dirty())return source;
        var out=source.copy();var preset=SpawnerEditorConfig.preset(out,entityId);int mask=preset.getIntOr("dropMask",63);
        for(var slot:Slot.values()){
            float chance=draft.gear().get(slot.ordinal());if(chance==-1)continue;
            mask|=1<<slot.ordinal();preset.putFloat("drop_"+slot.id,chance);
        }
        preset.putInt("dropMask",mask);
        if(draft.replaceRules()){
            var old=preset.getCompoundOrEmpty("intrinsicDropRules");var rules=new CompoundTag();var legacy=new CompoundTag();
            for(var row:draft.rules()){
                var value=old.getCompoundOrEmpty(row.id()).copy();
                if(unchanged(value,row)){rules.put(row.id(),value);legacy.putFloat(row.itemId(),row.chance());continue;}
                value.putString("id",row.id());value.putString("item",row.itemId());value.putFloat("chance",row.chance());
                value.putInt("count",row.count());value.putString("kind",row.kind().name());rules.put(row.id(),value);
                legacy.putFloat(row.itemId(),row.chance());
            }
            preset.put("intrinsicDropRules",rules);preset.put("intrinsicDrops",legacy);preset.putBoolean("intrinsicDropsOverride",true);
        }
        out.put("SpawnerPreset",preset);return out;
    }
}
