package net.goui.cosmicdungeon.mercenary;

import java.util.*;
import net.goui.cosmicdungeon.entity.ModEntities;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.*;

/** Real native damage/projectile/splash/cloud paths; no player connection or client launch. */
public final class AlliedWolfGameTests {
    private AlliedWolfGameTests() {}
    private static void check(GameTestHelper helper, boolean value, String message) {
        helper.assertTrue(value, Component.literal(message));
    }
    private static Wolf playerWolf(ServerLevel level, Player owner) {
        var wolf = new Wolf(EntityType.WOLF, level);
        wolf.setTame(true, true);
        wolf.setOwnerReference(EntityReference.of(owner.getUUID()));
        // The existing saved ownership marker must protect the pet even without an online owner.
        wolf.getPersistentData().putString("cosmicdungeon.bogatyr_owner", owner.getUUID().toString());
        wolf.setNoAi(true); wolf.setHealth(wolf.getMaxHealth());
        return wolf;
    }
    private static MercenaryEntity mercenary(ServerLevel level, Player owner) {
        var merc = new MercenaryEntity(ModEntities.MERCENARY.get(), level);
        merc.initialize(Long.MAX_VALUE-112,
                new MercenaryContract(UUID.randomUUID(), owner.getUUID(), "bogatyr", 2, 50));
        return merc;
    }
    public static void damage(GameTestHelper helper) {
        var level=helper.getLevel();var owner=helper.makeMockPlayer(GameType.SURVIVAL);
        var teammate=helper.makeMockPlayer(GameType.SURVIVAL);var merc=mercenary(level,owner);
        var playerWolf=playerWolf(level,owner);var hiredWolf=new Wolf(EntityType.WOLF,level);
        MercenaryWolves.prepare(hiredWolf,merc,owner);
        var hostile=EntityType.ZOMBIE.create(level,EntitySpawnReason.COMMAND);
        check(helper,hostile!=null,"Native hostile control exists");
        for(var wolf:List.of(playerWolf,hiredWolf)) {
            check(helper,MercenaryWolves.protectedCompanion(wolf),"Both player and mercenary wolves are protected");
            wolf.setItemSlot(EquipmentSlot.BODY,new ItemStack(Items.WOLF_ARMOR));
            float hp=wolf.getHealth();int armor=wolf.getBodyArmorItem().getDamageValue();
            var rocket=new FireworkRocketEntity(level,new ItemStack(Items.FIREWORK_ROCKET),teammate,0,0,0,true);
            var allied=List.of(wolf.damageSources().playerAttack(owner),wolf.damageSources().playerAttack(teammate),
                    wolf.damageSources().mobAttack(merc),wolf.damageSources().mobAttack(playerWolf),
                    wolf.damageSources().fireworks(rocket,teammate));
            for(var source:allied) {
                wolf.invulnerableTime=0;
                check(helper,!wolf.hurtServer(level,source,6),"Allied direct/AOE damage is rejected");
                check(helper,wolf.getHealth()==hp&&wolf.getBodyArmorItem().getDamageValue()==armor,
                        "Friendly fire consumes neither hitpoints nor wolf armor");
            }
            wolf.setItemSlot(EquipmentSlot.BODY,ItemStack.EMPTY);wolf.invulnerableTime=0;
            check(helper,wolf.hurtServer(level,wolf.damageSources().mobAttack(hostile),2)&&wolf.getHealth()<hp,
                    "Hostile damage still lands");
            wolf.invulnerableTime=0;hp=wolf.getHealth();
            check(helper,wolf.hurtServer(level,wolf.damageSources().generic(),1)&&wolf.getHealth()<hp,
                    "Unattributed/environmental damage is not blanket immunity");
        }
        var wild=new Wolf(EntityType.WOLF,level);float hp=wild.getHealth();
        check(helper,!MercenaryWolves.protectedCompanion(wild)
                &&wild.hurtServer(level,wild.damageSources().playerAttack(teammate),1)&&wild.getHealth()<hp,
                "Wild wolves retain vanilla damage");
        helper.succeed();
    }
    private static Arrow arrow(ServerLevel level, LivingEntity source, Vec3 target) {
        var stack=new ItemStack(Items.TIPPED_ARROW);
        stack.set(DataComponents.POTION_CONTENTS,new PotionContents(Potions.POISON));
        var arrow=new Arrow(level,source,stack,null);
        arrow.setPos(target.add(-2,.4,0));arrow.setDeltaMovement(4,0,0);
        arrow.setNoGravity(true);arrow.setRemainingFireTicks(100);
        return arrow;
    }
    public static void projectiles(GameTestHelper helper) {
        var level=helper.getLevel();var owner=helper.makeMockPlayer(GameType.SURVIVAL);
        var wolf=playerWolf(level,owner);var pos=helper.absoluteVec(new Vec3(1.5,6,1.5));
        wolf.setPos(pos);var hostile=EntityType.SKELETON.create(level,EntitySpawnReason.COMMAND);
        check(helper,hostile!=null,"Native ranged hostile exists");
        var friendly=arrow(level,owner,pos);var enemy=arrow(level,hostile,pos);
        try {
            check(helper,level.addFreshEntity(wolf),"Owned wolf joined native test world");
            check(helper,MercenaryBrain.friendlySource(friendly),"Arrow resolves its real allied owner");
            float hp=wolf.getHealth();friendly.tick();
            check(helper,wolf.getHealth()==hp&&!wolf.isOnFire()&&!wolf.hasEffect(MobEffects.POISON),
                    "Actual flaming tipped-arrow tick cannot wound, ignite or poison allied wolf");
            check(helper,!friendly.isRemoved(),"Friendly impact does not delete recoverable ammunition");
            enemy.tick();
            check(helper,wolf.getHealth()<hp&&wolf.isOnFire()&&wolf.hasEffect(MobEffects.POISON),
                    "Hostile flaming tipped-arrow tick still wounds and applies side effects");
        } finally { wolf.discard();friendly.discard();enemy.discard(); }
        helper.succeed();
    }
    private static void splash(ServerLevel level, LivingEntity caster, Wolf target,
                               net.minecraft.core.Holder<Potion> potion) {
        var item=new ItemStack(Items.SPLASH_POTION);
        item.set(DataComponents.POTION_CONTENTS,new PotionContents(potion));
        var shot=new ThrownSplashPotion(level,caster,item);
        shot.setPos(target.position());
        shot.onHitAsPotion(level,item,new EntityHitResult(target));
        shot.discard();
    }
    private static void cloud(ServerLevel level, LivingEntity caster, Wolf target,
                              net.minecraft.core.Holder<Potion> potion) {
        var pos=target.position();var cloud=new AreaEffectCloud(level,pos.x,pos.y,pos.z);
        cloud.setOwner(caster);cloud.setRadius(3);cloud.setWaitTime(0);cloud.setDuration(200);
        cloud.setPotionContents(new PotionContents(potion));cloud.tickCount=5;
        cloud.tick();cloud.discard();
    }
    public static void potions(GameTestHelper helper) {
        var level=helper.getLevel();var owner=helper.makeMockPlayer(GameType.SURVIVAL);
        var wolf=playerWolf(level,owner);wolf.setPos(helper.absoluteVec(new Vec3(1.5,6,1.5)));
        var hostile=EntityType.WITCH.create(level,EntitySpawnReason.COMMAND);
        check(helper,hostile!=null,"Native hostile potion caster exists");
        try {
            check(helper,level.addFreshEntity(wolf),"Potion target joined native world");
            float hp=wolf.getHealth();
            splash(level,owner,wolf,Potions.HARMING);splash(level,owner,wolf,Potions.POISON);
            cloud(level,owner,wolf,Potions.HARMING);cloud(level,owner,wolf,Potions.POISON);
            check(helper,wolf.getHealth()==hp&&!wolf.hasEffect(MobEffects.POISON),
                    "Allied native splash and lingering instant/timed harm are rejected");
            check(helper,!wolf.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,200),owner),
                    "Direct allied harmful effects are rejected");
            wolf.setHealth(wolf.getMaxHealth()-8);hp=wolf.getHealth();
            splash(level,owner,wolf,Potions.HEALING);cloud(level,owner,wolf,Potions.REGENERATION);
            check(helper,wolf.getHealth()>hp&&wolf.hasEffect(MobEffects.REGENERATION),
                    "Native allied healing and beneficial lingering effects still apply");
            wolf.removeAllEffects();wolf.invulnerableTime=0;hp=wolf.getHealth();
            splash(level,hostile,wolf,Potions.HARMING);cloud(level,hostile,wolf,Potions.POISON);
            check(helper,wolf.getHealth()<hp&&wolf.hasEffect(MobEffects.POISON),
                    "Hostile native splash harm and lingering poison remain effective");
        } finally { wolf.discard(); }
        helper.succeed();
    }
}
