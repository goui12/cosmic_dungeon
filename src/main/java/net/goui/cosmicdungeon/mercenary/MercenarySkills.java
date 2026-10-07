package net.goui.cosmicdungeon.mercenary;

import java.util.List;
import java.util.UUID;
import net.goui.cosmicdungeon.dungeon.*;
import net.goui.cosmicdungeon.dungeon.d1.D1RunData;
import net.goui.cosmicdungeon.network.PartyPayloads;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

/** One existing run counter per contract/skill. No entity copy or lifetime player XP. */
public final class MercenarySkills {
    private MercenarySkills() {}
    static String key(UUID mercenary, MercenarySkill skill) {
        return "mercenary_skill:"+mercenary+":"+skill.id();
    }
    static int successes(D1RunData data, long run, MercenaryContract contract, MercenarySkill skill) {
        return run>0 && skill.supports(contract) ? data.count(run,key(contract.id(),skill)) : 0;
    }
    public static int level(MercenaryEntity entity, MercenarySkill skill) {
        if (!(entity.level() instanceof ServerLevel level)) return skill.levelFor(0);
        return skill.levelFor(successes(D1RunData.get(level.getServer()),entity.runId(),entity.contract(),skill));
    }
    public static List<PartyPayloads.Skill> snapshot(MinecraftServer server, long run, MercenaryContract contract) {
        var data=D1RunData.get(server);
        return MercenarySkill.forContract(contract).stream()
                .map(skill->new PartyPayloads.Skill(skill.id(),successes(data,run,contract,skill))).toList();
    }
    static boolean admitted(DungeonRunRegistryData.RunRecord run, MercenaryContract contract,
                            UUID entity, String dimension, MercenarySkill skill, boolean dormant) {
        return !dormant && skill.supports(contract)
                && MercenaryLifecycle.admitted(run,contract,entity,dimension);
    }
    /** Returns the new total; callers already checked a real successful action. */
    static int record(D1RunData data, DungeonRunRegistryData.RunRecord run,
                      MercenaryContract contract, MercenarySkill skill) {
        if (run==null || !skill.supports(contract) || run.stateEnum()!=DungeonRunState.ACTIVE
                || !"dungeon_1".equals(run.dungeonId()) || !run.mercenaries().contains(contract)
                || !run.containsPlayer(contract.hirer()) || run.isCompletionExited(contract.hirer())
                || data.sealed(run.runId())) return -1;
        int next=MercenarySkill.advance(successes(data,run.runId(),contract,skill));
        data.setCount(run.runId(),key(contract.id(),skill),next);
        return next;
    }
    /** Server-only callback for a verified summon, potion effect, cast or credited hostile kill. */
    public static boolean success(MercenaryEntity entity, MercenarySkill skill) {
        if (!(entity.level() instanceof ServerLevel level) || !entity.isAlive()) return false;
        var server=level.getServer();
        var run=DungeonRunRegistryData.get(server).getRun(entity.runId()).orElse(null);
        if (!admitted(run,entity.contract(),entity.getUUID(),level.dimension().location().toString(),skill,entity.dormant())
                || MercenaryBrain.hirer(entity)==null) return false;
        var recovery=PendingDungeonRecoveryData.get(server);
        var handoff=recovery.handoff(entity.contract().hirer());
        if (recovery.completed(entity.contract().hirer())>=run.runId()
                || handoff!=null && handoff.run()==run.runId() && handoff.kind().equals("cleanup")) return false;
        var data=D1RunData.get(server);
        int before=successes(data,run.runId(),entity.contract(),skill);
        int after=record(data,run,entity.contract(),skill);
        if (after<=before) return false;
        int oldLevel=skill.levelFor(before), newLevel=skill.levelFor(after);
        if (newLevel>oldLevel) {
            var message=Component.literal(entity.contract().name()+" reached level "+newLevel+" in "+skill.title()+"!");
            for (var id:run.orderedPlayers().stream().limit(6).toList()) {
                var player=server.getPlayerList().getPlayer(id);
                if (player!=null && !run.isCompletionExited(id) && run.containsDimension(player.level().dimension()))
                    player.sendSystemMessage(message);
            }
        }
        return true;
    }
}
