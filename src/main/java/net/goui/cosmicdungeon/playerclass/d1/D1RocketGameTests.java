package net.goui.cosmicdungeon.playerclass.d1;

import java.util.*;
import net.goui.cosmicdungeon.entity.ModEntities;
import net.goui.cosmicdungeon.mercenary.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

public final class D1RocketGameTests {
    private D1RocketGameTests(){}
    public static void explosion(GameTestHelper helper){
        var level=helper.getLevel();var origin=helper.absoluteVec(new Vec3(.5,10.5,.5));
        var owner=helper.makeMockPlayer(GameType.SURVIVAL);owner.setPos(origin);
        var rocket=new FireworkRocketEntity(level,new ItemStack(Items.FIREWORK_ROCKET),owner,origin.x,origin.y,origin.z,true);
        var hostile=EntityType.ZOMBIE.create(level,EntitySpawnReason.COMMAND);
        var shielded=EntityType.ZOMBIE.create(level,EntitySpawnReason.COMMAND);
        var cow=EntityType.COW.create(level,EntitySpawnReason.COMMAND);
        var wolf=EntityType.WOLF.create(level,EntitySpawnReason.COMMAND);
        var merc=new MercenaryEntity(ModEntities.MERCENARY.get(),level);
        merc.initialize(Long.MAX_VALUE-108,new MercenaryContract(UUID.randomUUID(),owner.getUUID(),"pyroclast",2,50));
        wolf.setOwnerReference(EntityReference.of(owner.getUUID()));
        var targets=List.of(hostile,shielded,cow,wolf,merc);
        var wall=new LinkedHashMap<BlockPos,net.minecraft.world.level.block.state.BlockState>();
        try{
            hostile.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200);hostile.setHealth(200);
            hostile.setPos(origin.add(-1,0,0));shielded.setPos(origin.add(3,0,0));
            cow.setPos(origin.add(0,0,-1));wolf.setPos(origin.add(0,0,1));merc.setPos(origin.add(0,0,2));
            for(var target:targets){target.setNoAi(true);level.addFreshEntity(target);}
            for(int y=0;y<3;y++){
                var pos=BlockPos.containing(origin).offset(2,y,0);wall.put(pos,level.getBlockState(pos));
                level.setBlockAndUpdate(pos,Blocks.STONE.defaultBlockState());
            }
            var health=targets.stream().map(LivingEntity::getHealth).toList();
            helper.assertTrue(D1RocketAbilities.burst(rocket,level,owner,120),Component.literal("Real enemy damage reports a successful burst"));
            helper.assertTrue(hostile.getHealth()<health.get(0)-80,Component.literal("Close enemy receives high native firework damage"));
            for(int i=1;i<targets.size();i++)helper.assertTrue(targets.get(i).getHealth()==health.get(i),
                    Component.literal("Walls, neutral animals, owned wolves and mercenaries are protected"));
            helper.assertTrue(!D1RocketAbilities.enemy(owner,owner),Component.literal("Shooter never counts as enemy"));
            hostile.setInvulnerable(true);
            helper.assertTrue(!D1RocketAbilities.burst(rocket,level,owner,120),Component.literal("Blocked or invulnerable targets earn no success"));
            helper.succeed();
        }finally{targets.forEach(Entity::discard);wall.forEach(level::setBlockAndUpdate);}
    }
    public static void denied(GameTestHelper helper){
        var level=helper.getLevel();var owner=helper.makeMockPlayer(GameType.SURVIVAL);
        var ordinary=new FireworkRocketEntity(level,new ItemStack(Items.FIREWORK_ROCKET),owner,0,100,0,true);
        helper.assertTrue(!D1RocketAbilities.explode(ordinary,level),Component.literal("Unrelated vanilla rockets retain native handling"));
        var merc=new MercenaryEntity(ModEntities.MERCENARY.get(),level);
        merc.initialize(Long.MAX_VALUE-109,new MercenaryContract(UUID.randomUUID(),UUID.randomUUID(),"pyroclast",2,50));
        var denied=new FireworkRocketEntity(level,new ItemStack(net.goui.cosmicdungeon.item.ModItems.CINDERBITE.get()),merc,0,100,0,true);
        helper.assertTrue(D1RocketAbilities.explode(denied,level)&&D1RocketAbilities.explode(denied,level),
                Component.literal("Inactive and already-spent mercenary rockets cannot regain vanilla damage or crash"));
        helper.assertTrue(MercenarySkills.level(merc,MercenarySkill.FIREWORKS)==1,Component.literal("Denied burst earns no skill credit"));
        helper.succeed();
    }
}
