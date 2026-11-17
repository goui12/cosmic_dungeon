package net.goui.cosmicdungeon.playerclass.skill;

import java.util.function.IntSupplier;

/** Scoped on the server thread; secondary ability damage cannot refill or recursively activate skills. */
@net.neoforged.fml.common.EventBusSubscriber(modid="cosmicdungeon")
public final class SecondarySkillDamage {
    private static final String LAST_SECONDARY="cosmicdungeon_last_secondary_damage_v1";
    /** Native kill credit remembers a player and a mob separately, preferring the player. */
    @net.neoforged.bus.api.SubscribeEvent(priority=net.neoforged.bus.api.EventPriority.LOWEST)
    public static void remember(net.neoforged.neoforge.event.entity.living.LivingDamageEvent.Post event){
        var victim=event.getEntity();
        if(victim.level().isClientSide()||!victim.getType().is(net.minecraft.tags.EntityTypeTags.SENSITIVE_TO_SMITE)
                ||!(event.getSource().getEntity() instanceof net.minecraft.world.entity.LivingEntity actor))return;
        float damage=event.getNewDamage()+event.getReduction(
                net.neoforged.neoforge.common.damagesource.DamageContainer.Reduction.ABSORPTION);
        if(!(damage>0)||!Float.isFinite(damage))return;
        String key=provenanceKey(actor);
        boolean deniedProjectile=actor instanceof net.minecraft.server.level.ServerPlayer
                &&event.getSource().getDirectEntity() instanceof net.minecraft.world.entity.projectile.Projectile projectile
                &&ClassSkills.projectile(projectile)==null;
        if(active()||deniedProjectile)victim.getPersistentData().putString(key,actor.getUUID().toString());
        else victim.getPersistentData().remove(key);
    }
    private static String provenanceKey(net.minecraft.world.entity.LivingEntity actor){
        return LAST_SECONDARY+(actor instanceof net.minecraft.world.entity.player.Player?":player":":mob");
    }
    public static boolean remembered(net.minecraft.world.entity.LivingEntity target,net.minecraft.world.entity.LivingEntity actor){
        return actor.getUUID().toString().equals(target.getPersistentData().getStringOr(provenanceKey(actor),""));
    }
    private static final ThreadLocal<Boolean> ACTIVE=ThreadLocal.withInitial(()->false);
    private SecondarySkillDamage(){}
    public static boolean active(){return ACTIVE.get();}
    public static int run(IntSupplier action){
        boolean previous=ACTIVE.get();ACTIVE.set(true);
        try{return action.getAsInt();}finally{if(previous)ACTIVE.set(true);else ACTIVE.remove();}
    }
}
