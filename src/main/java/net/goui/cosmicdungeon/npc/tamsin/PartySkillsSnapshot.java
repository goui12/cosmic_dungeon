package net.goui.cosmicdungeon.npc.tamsin;

import java.util.*;
import net.goui.cosmicdungeon.network.*;
import net.goui.cosmicdungeon.mercenary.*;
import net.goui.cosmicdungeon.playerclass.skill.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

/** Uses existing persistent skill readers and active authority; never loads chunks or entities. */
public final class PartySkillsSnapshot {
    private PartySkillsSnapshot(){}
    public static PartySkills from(List<ClassSkillPayloads.Track> tracks){
        return tracks.isEmpty()?PartySkills.NONE:new PartySkills(tracks.getFirst().abbreviation(),tracks.getFirst().level(),List.of());
    }
    public static PartySkills mercenary(MinecraftServer server,long run,MercenaryContract contract){
        return from(SignatureSkills.mercenarySnapshot(server,run,contract));
    }
    public static PartySkills capture(LivingEntity actor,boolean active){
        PartySkills skills=actor instanceof ServerPlayer p?from(SignatureSkills.playerSnapshot(p))
                :actor instanceof MercenaryEntity m&&m.contract()!=null?mercenary(actor.level().getServer(),m.runId(),m.contract()):PartySkills.NONE;
        if(!active||skills.abbreviation().isEmpty())return skills;
        var abilities=new ArrayList<PartySkills.Ability>();
        String cls=actor instanceof ServerPlayer p?net.goui.cosmicdungeon.playerclass.api.ClassData.getClassId(p)
                :actor instanceof MercenaryEntity m?m.contract().classId():"";
        var skill=SignatureSkill.forClass(cls).orElse(null);
        if(skill!=null&&ChargedSkillState.limit(skill)>0&&ChargedClassSkills.active(actor,skill)){
            int charge=ChargedClassSkills.charge(actor,skill);
            boolean armed=skill==SignatureSkill.CHAIN_LIGHTNING&&ChargedClassSkills.armed(actor);
            var slot=ClassSkillActions.primary(cls,skills.level(),charge,armed,true);
            abilities.add(new PartySkills.Ability(slot.title(),0,0,charge,ChargedSkillState.limit(skill),armed));
        }
        ClassBuffs.selected(actor).ifPresent(buff->{
            int remaining=ClassBuffs.remaining(actor);
            abilities.add(new PartySkills.Ability(buff.title(),remaining==0?0:actor.level().getServer().overworld().getGameTime()+remaining,
                    ClassBuff.COOLDOWN,0,0,false));
        });
        return new PartySkills(skills.abbreviation(),skills.level(),abilities);
    }
}
