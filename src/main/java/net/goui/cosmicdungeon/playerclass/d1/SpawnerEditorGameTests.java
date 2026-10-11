package net.goui.cosmicdungeon.playerclass.d1;

import java.util.*;
import net.goui.cosmicdungeon.auth.*;
import net.goui.cosmicdungeon.block.entity.*;
import net.goui.cosmicdungeon.network.SpawnerEditorPayloads.*;
import net.goui.cosmicdungeon.spawner.editor.*;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.*;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.Vec3;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.TagValueInput;

/** Native authority/session/application regressions. No preview or test mobs are added to the world. */
public final class SpawnerEditorGameTests {
    private SpawnerEditorGameTests(){}
    private static void developer(CosmicSpawnerGameTests.Fixture f){
        RankStore.get(f.level).setRank(f.player.getUUID(),Rank.DEVELOPER);
    }
    private static void look(CosmicSpawnerGameTests.Fixture f,BlockPos pos){
        f.player.lookAt(EntityAnchorArgument.Anchor.EYES,Vec3.atCenterOf(pos));
    }
    private static View view(CosmicSpawnerGameTests.Fixture f){
        return f.packets.stream().filter(p->p instanceof ClientboundCustomPayloadPacket)
                .map(p->((ClientboundCustomPayloadPacket)p).payload()).filter(p->p instanceof View)
                .map(p->(View)p).reduce((a,b)->b).orElseThrow();
    }
    private static SpawnerEditorDraft cap(SpawnerEditorDraft d,int cap){
        return new SpawnerEditorDraft(d.entityId(),d.name(),d.captain(),d.boss(),cap,d.minDelay(),d.maxDelay(),
                d.silent(),d.noAi(),d.invulnerable(),d.persistent(),d.nameVisible());
    }
    private static CosmicSpawnerBlockEntity setup(CosmicSpawnerGameTests.Fixture f){
        developer(f);var pos=f.spawner(0,1,3);
        var be=(CosmicSpawnerBlockEntity)f.level.getBlockEntity(pos);be.setSpawnerEntityId("minecraft:zombie");look(f,pos);return be;
    }
    private static void clear(CosmicSpawnerGameTests.Fixture f){
        RankStore.get(f.level).clearRank(f.player.getUUID());
        SpawnerEditorService.logout(new net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent(f.player));
    }
    public static void sessions(GameTestHelper helper){
        try(var f=new CosmicSpawnerGameTests.Fixture(helper,Long.MAX_VALUE-141);
            var second=new CosmicSpawnerGameTests.Fixture(helper,Long.MAX_VALUE-142)){
            try{
                var be=setup(f);var before=be.saveWithoutMetadata(f.level.registryAccess());
                f.check(SpawnerEditorService.open(f.player)==1,"Developer opens looked-at spawner");
                var first=view(f);SpawnerEditorService.request(f.player,new Request(first.session(),Kind.CANCEL,cap(first.draft(),-1)));
                f.check(before.equals(be.saveWithoutMetadata(f.level.registryAccess())),"Cancel with invalid local text never mutates data");
                SpawnerEditorService.open(f.player);first=view(f);
                SpawnerEditorService.request(f.player,new Request(UUID.randomUUID(),Kind.APPLY,cap(first.draft(),9)));
                f.check(before.equals(be.saveWithoutMetadata(f.level.registryAccess())),"Forged session cannot mutate target");
                developer(second);second.player.setPos(f.player.position());look(second,be.getBlockPos());
                f.check(SpawnerEditorService.open(second.player)==1,"Concurrent editor opens same target");
                var other=view(second);
                SpawnerEditorService.request(f.player,new Request(first.session(),Kind.APPLY,cap(first.draft(),9)));
                f.check(view(f).status()==Status.APPLIED&&be.getSpawnerMobCap()==9,"Valid draft applies once");
                SpawnerEditorService.request(second.player,new Request(other.session(),Kind.APPLY,cap(other.draft(),12)));
                f.check(view(second).status()==Status.CLOSED&&be.getSpawnerMobCap()==9,"Concurrent stale editor cannot overwrite first change");
                SpawnerEditorService.request(f.player,new Request(first.session(),Kind.APPLY,cap(first.draft(),15)));
                f.check(be.getSpawnerMobCap()==9,"Replayed Apply cannot mutate completed session");helper.succeed();
            }finally{clear(f);clear(second);}
        }
    }
    public static void access(GameTestHelper helper){
        try(var f=new CosmicSpawnerGameTests.Fixture(helper,Long.MAX_VALUE-143)){
            try{
                var be=setup(f);var before=be.saveWithoutMetadata(f.level.registryAccess());
                SpawnerEditorService.open(f.player);var session=view(f);
                RankStore.get(f.level).setRank(f.player.getUUID(),Rank.DUNGEONEER);f.packets.clear();
                SpawnerEditorService.hover(f.player,be.getBlockPos());
                f.check(f.packets.isEmpty(),"Dungeoneer forged hover request yields no detail packet");
                SpawnerEditorService.request(f.player,new Request(session.session(),Kind.APPLY,cap(session.draft(),9)));
                f.check(view(f).status()==Status.CLOSED&&before.equals(be.saveWithoutMetadata(f.level.registryAccess())),"Fresh rank loss rejects Apply");
                f.check(SpawnerEditorService.open(f.player)==0,"Dungeoneer cannot open editor");
                developer(f);SpawnerEditorService.open(f.player);session=view(f);
                f.player.setPos(f.player.position().add(12,0,0));
                SpawnerEditorService.request(f.player,new Request(session.session(),Kind.APPLY,cap(session.draft(),9)));
                f.check(before.equals(be.saveWithoutMetadata(f.level.registryAccess())),"Out-of-range Apply cannot mutate");
                f.player.setPos(f.origin);look(f,be.getBlockPos());SpawnerEditorService.open(f.player);session=view(f);
                f.level.removeBlock(be.getBlockPos(),false);f.spawner(0,1,3);
                var replaced=(CosmicSpawnerBlockEntity)f.level.getBlockEntity(be.getBlockPos());
                SpawnerEditorService.request(f.player,new Request(session.session(),Kind.APPLY,cap(session.draft(),9)));
                f.check(view(f).status()==Status.CLOSED&&replaced.getSpawnerMobCap()==0,"Replacement identity invalidates the session");
                SpawnerEditorService.hover(f.player,replaced.getBlockPos());
                f.check(f.packets.stream().anyMatch(p->p instanceof ClientboundCustomPayloadPacket c&&c.payload() instanceof HoverView),
                        "Actual Developer hover needs no toggle");
                f.check(!replaced.getUpdateTag(f.level.registryAccess()).contains("SpawnerPreset"),"Public chunk update contains no detailed preset");
                helper.succeed();
            }finally{clear(f);}
        }
    }
    public static void flags(GameTestHelper helper){
        try(var f=new CosmicSpawnerGameTests.Fixture(helper,Long.MAX_VALUE-144)){
            var zombie=new Zombie(EntityType.ZOMBIE,f.level);
            var sword=new ItemStack(Items.DIAMOND_SWORD);sword.setDamageValue(17);zombie.setItemSlot(EquipmentSlot.MAINHAND,sword.copy());
            zombie.setCustomName(Component.literal("Weighted authored name"));
            zombie.getPersistentData().putString("cosmicdungeon:spawner_intrinsic_drops","authored-marker");
            var before=SpawnerEditorDraft.empty();
            var after=new SpawnerEditorDraft("minecraft:zombie","",false,false,0,200,800,true,true,true,true,true);
            var tag=SpawnerEditorConfig.patch(new CompoundTag(),before,after).getCompoundOrEmpty("SpawnerPreset");
            // Explicitly clear an existing name, as the UI does when its baseline name was nonempty.
            var named=new SpawnerEditorDraft("minecraft:zombie","Weighted authored name",false,false,0,200,800,false,false,false,false,false);
            tag=SpawnerEditorConfig.patch(new CompoundTag(),named,after).getCompoundOrEmpty("SpawnerPreset");
            var preset=CosmicSpawnerPreset.load(TagValueInput.create(ProblemReporter.DISCARDING,f.level.registryAccess(),tag));
            preset.applyToEntity(zombie);
            f.check(zombie.isSilent()&&zombie.isNoAi()&&zombie.isInvulnerable()&&zombie.isPersistenceRequired()&&zombie.isCustomNameVisible(),
                    "All five generic flags affect the actual native mob");
            f.check(zombie.getCustomName()==null,"Explicit clear overrides weighted and maintained mob names");
            f.check(ItemStack.matches(sword,zombie.getMainHandItem()),"Metadata-only preset leaves authored native equipment untouched");
            f.check(zombie.getPersistentData().getStringOr("cosmicdungeon:spawner_intrinsic_drops","").equals("authored-marker"),
                    "Metadata-only preset leaves entity intrinsic loot untouched");
            var captain=new SpawnerEditorDraft("minecraft:pillager","",true,false,0,200,800,false,false,false,false,false);
            var captainTag=SpawnerEditorConfig.patch(new CompoundTag(),before,captain).getCompoundOrEmpty("SpawnerPreset");
            var pillager=new Pillager(EntityType.PILLAGER,f.level);
            CosmicSpawnerPreset.load(TagValueInput.create(ProblemReporter.DISCARDING,f.level.registryAccess(),captainTag)).applyToEntity(pillager);
            f.check(pillager.isPatrolLeader()&&!pillager.isPersistenceRequired(),"Captain does not override an unchecked Persistent flag");
            zombie.discard();pillager.discard();helper.succeed();
        }
    }
}
