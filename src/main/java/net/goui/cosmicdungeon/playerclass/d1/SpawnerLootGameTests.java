package net.goui.cosmicdungeon.playerclass.d1;

import java.util.*;
import net.goui.cosmicdungeon.auth.*;
import net.goui.cosmicdungeon.block.entity.*;
import net.goui.cosmicdungeon.block.entity.CosmicSpawnerPreset.IntrinsicDropRule;
import net.goui.cosmicdungeon.command.*;
import net.goui.cosmicdungeon.network.SpawnerEditorPayloads.*;
import net.goui.cosmicdungeon.spawner.editor.*;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;

public final class SpawnerLootGameTests {
    private SpawnerLootGameTests(){}
    private static View view(CosmicSpawnerGameTests.Fixture f){
        return f.packets.stream().filter(p->p instanceof ClientboundCustomPayloadPacket)
            .map(p->((ClientboundCustomPayloadPacket)p).payload()).filter(p->p instanceof View).map(p->(View)p).reduce((a,b)->b).orElseThrow();
    }
    private static LibraryView reply(CosmicSpawnerGameTests.Fixture f){
        return f.packets.stream().filter(p->p instanceof ClientboundCustomPayloadPacket)
            .map(p->((ClientboundCustomPayloadPacket)p).payload()).filter(p->p instanceof LibraryView).map(p->(LibraryView)p).reduce((a,b)->b).orElseThrow();
    }
    private static CosmicSpawnerBlockEntity setup(CosmicSpawnerGameTests.Fixture f){
        RankStore.get(f.level).setRank(f.player.getUUID(),Rank.DEVELOPER);
        var pos=f.spawner(0,1,3);var be=(CosmicSpawnerBlockEntity)f.level.getBlockEntity(pos);be.setSpawnerEntityId("minecraft:zombie");
        f.player.lookAt(EntityAnchorArgument.Anchor.EYES,Vec3.atCenterOf(pos));return be;
    }
    private static void clear(CosmicSpawnerGameTests.Fixture f){
        RankStore.get(f.level).clearRank(f.player.getUUID());
        SpawnerEditorService.logout(new net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent(f.player));
    }
    private static SpawnerEditorDraft cap(SpawnerEditorDraft d,int cap){
        return new SpawnerEditorDraft(d.entityId(),d.name(),d.captain(),d.boss(),cap,d.minDelay(),d.maxDelay(),d.silent(),d.noAi(),d.invulnerable(),d.persistent(),d.nameVisible());
    }
    private static SpawnerLootDraft.Rule rule(String id,String item,float chance,int count,IntrinsicDropRule.Kind kind){
        return new SpawnerLootDraft.Rule(id,"minecraft:"+item,chance,count,kind);
    }
    public static void drafts(GameTestHelper helper){
        try(var f=new CosmicSpawnerGameTests.Fixture(helper,Long.MAX_VALUE-161)){
            try{
                var be=setup(f);SpawnerEditorService.open(f.player);var v=view(f);
                var before=be.saveWithoutMetadata(f.level.registryAccess());
                for(var bad:List.of(rule("bad","bone",Float.NaN,1,IntrinsicDropRule.Kind.CUSTOM_ADDITION),
                    rule("bad","bone",1,65,IntrinsicDropRule.Kind.CUSTOM_ADDITION),
                    rule("bad","missing_batch16_item",1,1,IntrinsicDropRule.Kind.CUSTOM_ADDITION))){
                    SpawnerEditorService.request(f.player,new Request(v.session(),Kind.APPLY,cap(v.draft(),12),SpawnerEquipmentDraft.unchanged(),
                        new SpawnerLootDraft(Collections.nCopies(6,-1f),true,List.of(bad))));
                    f.check(view(f).status()==Status.INVALID&&before.equals(be.saveWithoutMetadata(f.level.registryAccess())),"Invalid loot cannot partially apply core fields");
                }
                var loot=new SpawnerLootDraft(List.of(0f,-1f,-1f,-1f,-1f,1f),true,List.of(
                    rule("first","bone",1,2,IntrinsicDropRule.Kind.CUSTOM_ADDITION),
                    rule("second","bone",0,3,IntrinsicDropRule.Kind.CUSTOM_ADDITION)));
                SpawnerEditorService.request(f.player,new Request(v.session(),Kind.CANCEL,v.draft(),SpawnerEquipmentDraft.unchanged(),loot));
                f.check(before.equals(be.saveWithoutMetadata(f.level.registryAccess())),"Cancel discards all loot edits");
                SpawnerEditorService.open(f.player);v=view(f);
                SpawnerEditorService.request(f.player,new Request(v.session(),Kind.APPLY,v.draft(),SpawnerEquipmentDraft.unchanged(),loot));
                f.check(view(f).status()==Status.APPLIED,"Valid bounded loot applies");
                var saved=be.saveWithoutMetadata(f.level.registryAccess());var preset=saved.getCompoundOrEmpty("SpawnerPreset");
                f.check(preset.getIntOr("dropMask",0)==33&&preset.getIntOr("equipmentMask",-1)==0,"Gear percentages claim only edited drop slots");
                var fresh=CosmicSpawnerPreset.load(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING,f.level.registryAccess(),preset));
                f.check(fresh.getConfiguredIntrinsicDropRules().keySet().containsAll(List.of("first","second")),"Same-item rules survive separate stable IDs");
                helper.succeed();
            }finally{clear(f);}
        }
    }
    public static void outcomes(GameTestHelper helper){
        try(var f=new CosmicSpawnerGameTests.Fixture(helper,Long.MAX_VALUE-162)){
            var mob=new Zombie(EntityType.ZOMBIE,f.level);
            try{
                var sword=new ItemStack(Items.DIAMOND_SWORD);mob.setItemSlot(EquipmentSlot.MAINHAND,sword.copy());
                var rules=new LinkedHashMap<String,IntrinsicDropRule>();
                rules.put("remove",new IntrinsicDropRule("remove",ResourceLocation.withDefaultNamespace("rotten_flesh"),0,1,IntrinsicDropRule.Kind.DEFAULT_OVERRIDE));
                rules.put("add1",new IntrinsicDropRule("add1",ResourceLocation.withDefaultNamespace("bone"),1,2,IntrinsicDropRule.Kind.CUSTOM_ADDITION));
                rules.put("add2",new IntrinsicDropRule("add2",ResourceLocation.withDefaultNamespace("bone"),1,3,IntrinsicDropRule.Kind.CUSTOM_ADDITION));
                rules.put("gear",new IntrinsicDropRule("gear",ResourceLocation.withDefaultNamespace("diamond_sword"),0,1,IntrinsicDropRule.Kind.DEFAULT_OVERRIDE));
                CosmicSpawnerEntityIntrinsicDropData.write(mob,rules);
                var drops=new ArrayList<ItemEntity>();
                drops.add(new ItemEntity(f.level,0,0,0,new ItemStack(Items.ROTTEN_FLESH,3)));
                drops.add(new ItemEntity(f.level,0,0,0,new ItemStack(Items.BONE,1)));
                drops.add(new ItemEntity(f.level,0,0,0,sword.copy()));
                CosmicSpawnerIntrinsicDropEvents.onLivingDrops(new LivingDropsEvent(mob,f.level.damageSources().generic(),drops,true));
                f.check(drops.stream().noneMatch(e->e.getItem().is(Items.ROTTEN_FLESH)),"Zero-percent override removes native item");
                f.check(drops.stream().filter(e->e.getItem().is(Items.BONE)).mapToInt(e->e.getItem().getCount()).sum()==6,"Independent same-item additions preserve native item and both full-percent rolls");
                f.check(drops.stream().filter(e->ItemStack.matches(sword,e.getItem())).count()==1,"Intrinsic override preserves matching equipment");
                drops.forEach(ItemEntity::discard);helper.succeed();
            }finally{mob.discard();}
        }
    }
    private static LibraryRequest action(CosmicSpawnerGameTests.Fixture f,View v,LibraryKind kind,String name,SpawnerEditorDraft draft,int slot){
        f.player.tickCount+=5;var q=new LibraryRequest(v.session(),UUID.randomUUID(),kind,name,slot,draft,SpawnerEquipmentDraft.unchanged(),SpawnerLootDraft.unchanged());
        SpawnerEditorService.library(f.player,q);return q;
    }
    private static void legacyOverlay(CosmicSpawnerGameTests.Fixture f,CosmicSpawnerBlockEntity be,String name)throws Exception{
        var authored=be.saveWithoutMetadata(f.level.registryAccess());var preset=new net.minecraft.nbt.CompoundTag();
        preset.putInt("presetVersion",5);preset.putString("entityType","minecraft:zombie");preset.putInt("equipmentMask",1);preset.putInt("dropMask",1);
        preset.store("customName",net.minecraft.network.chat.ComponentSerialization.flatRestrictedCodec(8192),net.minecraft.network.chat.Component.literal("Authored guard"));
        preset.putBoolean("nameOverride",true);preset.putBoolean("illagerCaptainVariant",false);preset.putBoolean("captainOverride",true);
        var sword=new ItemStack(Items.IRON_SWORD);sword.setDamageValue(17);
        preset.put("eq_mainhand",ItemStack.CODEC.encodeStart(f.level.registryAccess().createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE),sword).getOrThrow());
        preset.putFloat("drop_mainhand",0.375F);var flags=new net.minecraft.nbt.CompoundTag();flags.putBoolean("Silent",true);flags.putLong("futureFlag",42L);preset.put("entityFlags",flags);
        preset.putLongArray("futurePreset",new long[]{7L,Long.MAX_VALUE});var rules=new net.minecraft.nbt.CompoundTag();var rule=new net.minecraft.nbt.CompoundTag();
        rule.putString("id","authored");rule.putString("item","minecraft:bone");rule.putFloat("chance",0.25F);rule.putInt("count",3);rule.putString("kind","CUSTOM_ADDITION");rules.put("authored",rule);preset.put("intrinsicDropRules",rules);
        authored.put("SpawnerPreset",preset);var potentials=new net.minecraft.nbt.ListTag();var potential=new net.minecraft.nbt.CompoundTag();
        var spawn=new net.minecraft.nbt.CompoundTag();var entity=new net.minecraft.nbt.CompoundTag();entity.putString("id","minecraft:zombie");entity.putString("FutureSpawn","keep");
        spawn.put("entity",entity);potential.put("data",spawn);potential.putInt("weight",3);potentials.add(potential);authored.put("SpawnPotentials",potentials);
        SpawnerEditorService.applyConfiguration(f.player,be,authored);var before=be.saveWithoutMetadata(f.level.registryAccess());
        var expected=before.getCompoundOrEmpty("SpawnerPreset").copy();expected.putString("entityType","minecraft:skeleton");
        var legacy=new com.google.gson.JsonObject();legacy.addProperty("formatVersion",3);legacy.addProperty("entityTypeId","minecraft:skeleton");
        legacy.addProperty("minSpawnDelay",80);legacy.addProperty("maxSpawnDelay",140);legacy.addProperty("spawnerMobCap",12);
        java.nio.file.Files.writeString(SpawnerPresetFileStore.presetPath(f.level.getServer(),name),legacy.toString(),java.nio.file.StandardOpenOption.CREATE_NEW);
        SpawnerEditorService.open(f.player);var opened=view(f);action(f,opened,LibraryKind.LOAD,name,opened.draft(),0);var loaded=view(f);
        f.check(loaded.presetLoaded()&&loaded.draft().entityId().equals("minecraft:skeleton"),"Legacy load retains imported entity identity in detached draft");
        f.check(before.equals(be.saveWithoutMetadata(f.level.registryAccess())),"Legacy load does not apply before confirmation");
        SpawnerEditorService.request(f.player,new Request(loaded.session(),Kind.APPLY,loaded.draft()));
        f.check(view(f).status()==Status.APPLIED&&be.getSpawnerEntityId().equals("minecraft:skeleton"),"Legacy Apply cannot restore the old preset entity identity");
        var after=be.saveWithoutMetadata(f.level.registryAccess());
        f.check(expected.equals(after.getCompoundOrEmpty("SpawnerPreset")),"Legacy overlay preserves authored gear components, loot, flags, captain state and unknown fields");
        f.check(before.get("SpawnPotentials").equals(after.get("SpawnPotentials")),"Legacy overlay preserves existing weighted spawn choices");
        be.loadWithComponents(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING,f.level.registryAccess(),after));
        f.check(be.getSpawnerEntityId().equals("minecraft:skeleton")&&expected.equals(be.saveWithoutMetadata(f.level.registryAccess()).getCompoundOrEmpty("SpawnerPreset")),"Legacy overlay identity and authored data survive actual block reload");
        SpawnerPresetFileStore.deletePreset(f.level.getServer(),name);
    }
    public static void library(GameTestHelper helper){
        try(var f=new CosmicSpawnerGameTests.Fixture(helper,Long.MAX_VALUE-163)){
            String name="batch16_test_"+UUID.randomUUID().toString().replace("-","");var server=f.level.getServer();
            try{
                var be=setup(f);SpawnerEditorService.open(f.player);var v=view(f);var before=be.saveWithoutMetadata(f.level.registryAccess());
                var q=action(f,v,LibraryKind.SAVE,name,cap(v.draft(),12),0);
                f.check(reply(f).success()&&SpawnerPresetFileStore.listPresetNames(server).contains(name),"Save creates named file");
                f.check(before.equals(be.saveWithoutMetadata(f.level.registryAccess())),"Save never applies live draft");
                SpawnerEditorService.library(f.player,q);f.check(reply(f).success(),"Duplicate save operation replays success without writing twice");
                action(f,v,LibraryKind.LOAD,name,v.draft(),0);var loaded=view(f);
                f.check(loaded.presetLoaded()&&!loaded.session().equals(v.session())&&loaded.draft().cap()==12,"Load creates independent dirty draft and new nonce");
                f.check(before.equals(be.saveWithoutMetadata(f.level.registryAccess())),"Load alone leaves live configuration unchanged");
                SpawnerEditorService.request(f.player,new Request(loaded.session(),Kind.CANCEL,loaded.draft()));
                f.check(before.equals(be.saveWithoutMetadata(f.level.registryAccess())),"Cancel after Load preserves target");
                SpawnerEditorService.open(f.player);v=view(f);action(f,v,LibraryKind.LOAD,name,v.draft(),0);loaded=view(f);
                SpawnerEditorService.request(f.player,new Request(loaded.session(),Kind.APPLY,loaded.draft()));
                f.check(view(f).status()==Status.APPLIED&&be.getSpawnerMobCap()==12,"Explicit Apply commits loaded configuration");
                SpawnerEditorService.open(f.player);v=view(f);
                for(int i=1;i<=5;i++)action(f,v,LibraryKind.BIND,name,v.draft(),i);
                f.check(SpawnerCommand.presetShortcuts(f.player).equals(Collections.nCopies(5,name)),"All existing five shortcuts bind named preset");
                be.setSpawnerMobCap(3);
                f.check(SpawnerCommand.loadPresetFromKeybind(f.player,5)==1&&be.getSpawnerMobCap()==12,"Existing shortcut executes the full saved configuration");
                SpawnerEditorService.open(f.player);v=view(f);
                action(f,v,LibraryKind.DELETE,name,v.draft(),0);
                f.check(reply(f).success()&&!SpawnerPresetFileStore.listPresetNames(server).contains(name)&&be.getSpawnerMobCap()==12,"Delete removes file only");
                legacyOverlay(f,be,name);
                SpawnerEditorService.open(f.player);v=view(f);
                RankStore.get(f.level).clearRank(f.player.getUUID());
                action(f,v,LibraryKind.SAVE,name,v.draft(),0);
                f.check(view(f).status()==Status.CLOSED&&!SpawnerPresetFileStore.listPresetNames(server).contains(name),"Revoked Developer cannot save a preset");
                helper.succeed();
            }catch(Exception ex){throw new RuntimeException(ex);}
            finally{
                try{SpawnerPresetFileStore.deletePreset(server,name);}catch(Exception ignored){}
                clear(f);
            }
        }
    }
}
