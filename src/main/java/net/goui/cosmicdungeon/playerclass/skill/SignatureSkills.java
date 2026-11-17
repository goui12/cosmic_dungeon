package net.goui.cosmicdungeon.playerclass.skill;

import java.util.*;
import net.goui.cosmicdungeon.dungeon.d1.D1RunData;
import net.goui.cosmicdungeon.mercenary.*;
import net.goui.cosmicdungeon.network.ClassSkillPayloads;
import net.goui.cosmicdungeon.playerclass.api.ClassData;
import net.goui.cosmicdungeon.progression.PlayerProgressionData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/** Signature progress uses the existing player save and existing mercenary run store. No duplicate owner database. */
public final class SignatureSkills {
    private SignatureSkills(){}
    public static int playerProgress(ServerPlayer player,SignatureSkill skill){
        return PlayerProgressionData.get(player.level().getServer()).signatureProgress(player.getUUID(),skill);
    }
    public static int playerLevel(ServerPlayer player,SignatureSkill skill){
        return skill.level(playerProgress(player,skill),ClassSkillConfig.BASE.get(),ClassSkillConfig.STEP.get());
    }
    public static List<ClassSkillPayloads.Track> playerSnapshot(ServerPlayer player){
        return SignatureSkill.forClass(ClassData.getClassId(player)).map(skill->{
            int raw=playerProgress(player,skill),base=ClassSkillConfig.BASE.get(),step=ClassSkillConfig.STEP.get();
            return List.of(new ClassSkillPayloads.Track(skill.id(),skill.title(),skill.abbreviation(),
                    skill.level(raw,base,step),skill.withinLevel(raw,base,step),skill.nextLevelCost(raw,base,step)));
        }).orElse(List.of());
    }
    /** Called only from actual verified server success paths, never from an input packet. */
    public static boolean success(ServerPlayer player,SignatureSkill skill){
        if(skill==SignatureSkill.POSITIVE_POTIONS)throw new IllegalArgumentException("Positive potion support retains its XP award units");
        if(ClassSkills.run(player)==null||!skill.classId().equals(ClassData.getClassId(player)))return false;
        var data=PlayerProgressionData.get(player.level().getServer());
        int before=data.signatureProgress(player.getUUID(),skill);
        return data.addSignatureProgress(player.getUUID(),skill,1,skill.threshold(skill.cap(),250,50))>before;
    }
    public static List<ClassSkillPayloads.Track> mercenarySnapshot(MinecraftServer server,long run,MercenaryContract contract){
        var skill=SignatureSkill.forClass(contract.classId()).orElse(null);
        if(skill==null||run<=0)return List.of();
        if(skill==SignatureSkill.SMITE){
            int raw=D1RunData.get(server).count(run,smiteKey(contract.id()));
            return List.of(new ClassSkillPayloads.Track(skill.id(),skill.title(),skill.abbreviation(),
                    skill.level(raw,250,50),skill.withinLevel(raw,250,50),skill.nextLevelCost(raw,250,50)));
        }
        var source=MercenarySkills.snapshot(server,run,contract).stream().filter(s->s.id().equals(skill.legacy().id())).findFirst().orElseThrow();
        int level=skill.legacy().levelFor(source.successes());
        long start=skill.legacy().thresholdFor(level);
        long next=level>=65536?0:skill.legacy().thresholdFor(level+1)-start;
        return List.of(new ClassSkillPayloads.Track(skill.id(),skill.title(),skill.abbreviation(),level,
                next==0?0:(int)(source.successes()-start),(int)next));
    }
    public static String smiteKey(UUID contract){return "mercenary_signature:"+contract+":smite";}
}
