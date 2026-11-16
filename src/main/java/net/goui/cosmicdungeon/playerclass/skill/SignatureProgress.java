package net.goui.cosmicdungeon.playerclass.skill;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.*;

/** Additive v1 subrecord. Unknown future track entries are retained, but cannot be awarded through this API. */
public record SignatureProgress(int version, Map<String,Integer> values) {
    public static final int VERSION=1;
    public static final SignatureProgress EMPTY=new SignatureProgress(VERSION,Map.of());
    private static final Codec<Map<String,Integer>> VALUES=Codec.unboundedMap(
            Codec.STRING,Codec.intRange(0,Integer.MAX_VALUE)).validate(v->
            v.size()<=128&&v.keySet().stream().allMatch(k->!k.isBlank()&&k.length()<=128)
                    ?DataResult.success(Map.copyOf(v)):DataResult.error(()->"Invalid signature progress entries"));
    public static final Codec<SignatureProgress> CODEC=RecordCodecBuilder.create(i->i.group(
            Codec.intRange(VERSION,VERSION).optionalFieldOf("version",VERSION).forGetter(SignatureProgress::version),
            VALUES.optionalFieldOf("values",Map.of()).forGetter(SignatureProgress::values)
    ).apply(i,SignatureProgress::new));
    public SignatureProgress {
        values=Map.copyOf(values);
        if(version!=VERSION||values.size()>128||values.entrySet().stream().anyMatch(e->
                e.getKey().isBlank()||e.getKey().length()>128||e.getValue()<0))
            throw new IllegalArgumentException("Invalid signature progress");
    }
    public int get(SignatureSkill skill,int legacyPotionXp){
        return values.getOrDefault(skill.id(),skill==SignatureSkill.POSITIVE_POTIONS?Math.max(0,legacyPotionXp):0);
    }
    public SignatureProgress add(SignatureSkill skill,int amount,int legacyPotionXp,long cap){
        Objects.requireNonNull(skill);
        if(amount<0)throw new IllegalArgumentException("Negative signature award");
        int old=get(skill,legacyPotionXp), next=ClassSkillRules.addXp(old,amount,cap);
        if(next==old)return this;
        var updated=new HashMap<>(values);updated.put(skill.id(),next);
        return new SignatureProgress(VERSION,updated);
    }
}
