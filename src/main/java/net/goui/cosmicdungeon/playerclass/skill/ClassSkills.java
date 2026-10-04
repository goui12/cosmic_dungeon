package net.goui.cosmicdungeon.playerclass.skill;

import net.goui.cosmicdungeon.auth.AccessPolicy;
import net.goui.cosmicdungeon.dungeon.*;
import net.goui.cosmicdungeon.playerclass.api.*;
import net.goui.cosmicdungeon.progression.PlayerProgressionData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.projectile.*;
import net.minecraft.world.item.*;

/** No inventory rewriting, client trust, tick scans or per-hit disk writes. */
public final class ClassSkills {
    public static final String SHOT = "cosmicdungeon_skill_attack_v1";
    private ClassSkills() {}
    public record Attack(ServerPlayer player, String cls, String skill, CompoundTag action) {}
    public static DungeonRunRegistryData.RunRecord run(ServerPlayer player) {
        if (!player.isAlive() || player.isSpectator() || AccessPolicy.isDeveloper(player)) return null;
        var run = DungeonRunRegistryData.get(player.level().getServer())
                .findRunForInstanceDimension(player.level().dimension()).orElse(null);
        if (run == null || run.stateEnum() != DungeonRunState.ACTIVE || !run.containsPlayer(player.getUUID())
                || run.completionExitedPlayers().contains(player.getUUID())) return null;
        String cls = ClassData.getClassId(player);
        if (run.dungeonId().equals("dungeon_1") && (cls.equals("deadeye") || cls.equals("metalmancer")
                || net.goui.cosmicdungeon.dungeon.d1.D1RunData.get(player.level().getServer()).sealed(run.runId()))) return null;
        return run;
    }
    public static String weapon(ItemStack stack) {
        if (stack.is(ItemTags.SWORDS)) return "sword";
        if (stack.is(Items.MACE)) return "mace";
        if (stack.is(Items.TRIDENT)) return "trident";
        if (stack.getItem() instanceof BowItem) return "bow";
        if (stack.getItem() instanceof CrossbowItem) return "crossbow";
        if (stack.getItem() instanceof PotionItem) return "potions";
        return "";
    }
    public static Attack held(ServerPlayer player, ItemStack stack) {
        String cls = ClassData.getClassId(player), skill = weapon(stack);
        return run(player) != null && ClassSkillRules.known(cls, skill)
                && ClassItemEquipmentGuard.canUse(player, stack) ? new Attack(player,cls,skill,new CompoundTag()) : null;
    }
    public static void capture(Entity projectile, LivingEntity owner, ItemStack weapon) {
        if (!(owner instanceof ServerPlayer player) || projectile.getPersistentData().contains(SHOT)) return;
        var attack = held(player, weapon);
        CompoundTag data = new CompoundTag();
        // Persist an explicit empty snapshot too; delayed admission cannot train under a later class.
        if (attack != null) {
            data=SkillAttackSnapshot.create(player.getUUID(),attack.cls(),attack.skill(),run(player).runId());
        }
        projectile.getPersistentData().put(SHOT, data);
    }
    public static Attack projectile(Entity projectile) {
        var data = projectile.getPersistentData().getCompoundOrEmpty(SHOT);
        Entity owner = projectile instanceof Projectile p ? p.getOwner()
                : projectile instanceof AreaEffectCloud c ? c.getOwner() : null;
        if (!(owner instanceof ServerPlayer player) || owner.level() != projectile.level()) return null;
        var run = run(player); String cls = data.getStringOr("class", ""), skill = data.getStringOr("skill", "");
        if (run == null || !SkillAttackSnapshot.matches(data,player.getUUID(),ClassData.getClassId(player),run.runId())) return null;
        return new Attack(player,cls,skill,data);
    }
    public static Attack attack(DamageSource source) {
        if (net.goui.cosmicdungeon.playerclass.dragoon.DragoonPassiveEvents.chaining()) return null;
        var potion = SkillPotions.current();
        if (potion != null && source.getEntity() == potion.player()) return potion;
        if (!(source.getEntity() instanceof ServerPlayer player)) return null;
        if (source.getDirectEntity() instanceof Projectile p &&
                (source.is(DamageTypes.ARROW) || source.is(DamageTypes.TRIDENT)
                || source.is(DamageTypes.FIREWORKS) || source.is(DamageTypes.INDIRECT_MAGIC))) return projectile(p);
        if (source.is(DamageTypes.PLAYER_ATTACK) && source.getDirectEntity() == player) {
            var attack=held(player, player.getMainHandItem());
            return attack != null && ClassSkillRules.melee(attack.skill()) ? attack : null;
        }
        return null;
    }
    public static boolean enemy(LivingEntity target, ServerPlayer owner) {
        return target instanceof Enemy && !(target instanceof OwnableEntity) && !target.isAlliedTo(owner)
                && target.level() == owner.level();
    }
    public static boolean ally(LivingEntity target, ServerPlayer owner) {
        var run = run(owner);
        if (run == null || target.level() != owner.level() || !target.isAlive()) return false;
        if (target instanceof ServerPlayer p) return run(p) != null && run.containsPlayer(p.getUUID());
        return target instanceof OwnableEntity owned && owned.getOwnerReference() != null
                && run.containsPlayer(owned.getOwnerReference().getUUID());
    }
    public static int xp(ServerPlayer player, String cls, String skill) {
        return PlayerProgressionData.get(player.level().getServer()).skillXp(player.getUUID(), cls + "." + skill);
    }
    public static double bonus(Attack attack, double capBonus) {
        return attack == null ? 0 : ClassSkillRules.bonus(ClassSkillConfig.level(xp(attack.player(),attack.cls(),attack.skill())),
                ClassSkillConfig.CAP.get(),capBonus);
    }
    public static double damage(Attack attack) {
        if (attack == null || attack.skill().equals("potions")) return 1;
        return 1 + bonus(attack,ClassSkillConfig.DAMAGE.get(attack.cls()+"."+attack.skill()).get());
    }
    public static int award(Attack attack, int amount, boolean boundedAction) {
        if (attack == null || amount <= 0 || run(attack.player()) == null) return 0;
        int spent = attack.action().getIntOr("xp_spent",0);
        if (boundedAction) amount=ClassSkillRules.budget(spent,amount,ClassSkillConfig.ACTION_CAP.get());
        if (amount <= 0) return 0;
        var data=PlayerProgressionData.get(attack.player().level().getServer());
        String key=attack.cls()+"."+attack.skill(); int old=data.skillXp(attack.player().getUUID(),key);
        int next=data.addSkillXp(attack.player().getUUID(),key,amount,ClassSkillConfig.xpCap());
        if (boundedAction) attack.action().putInt("xp_spent",spent+amount);
        int level=ClassSkillConfig.level(next);
        if (level>ClassSkillConfig.level(old)) attack.player().displayClientMessage(Component.literal(
                ClassItemUtil.displayNameForClass(attack.cls())+" "+attack.skill()+" reached level "+level+"!"),false);
        return next-old;
    }
}
