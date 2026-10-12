package net.goui.cosmicdungeon.spawner.editor;
import java.util.*;
import net.goui.cosmicdungeon.block.entity.CosmicSpawnerPreset.IntrinsicDropRule.Kind;

/** Fractions retain authored precision; -1 gear values preserve current ownership/settings. */
public record SpawnerLootDraft(List<Float> gear,boolean replaceRules,List<Rule> rules) {
    public static final int MAX_RULES=128;
    public SpawnerLootDraft {
        gear=List.copyOf(gear);rules=List.copyOf(rules);
        if(gear.size()!=6||rules.size()>MAX_RULES)throw new IllegalArgumentException("Loot draft exceeds its bounds.");
    }
    public record Rule(String id,String itemId,float chance,int count,Kind kind) {
        public Rule {
            Objects.requireNonNull(id);Objects.requireNonNull(itemId);Objects.requireNonNull(kind);
            if(id.length()>64||itemId.length()>256)throw new IllegalArgumentException("Loot identifier too long.");
        }
    }
    public record DefaultDrop(String itemId,String chanceLabel) {}
    public record Snapshot(List<Float> gear,List<Rule> rules,List<DefaultDrop> defaults,String note,String entityId) {
        public Snapshot {
            gear=List.copyOf(gear);rules=List.copyOf(rules);defaults=List.copyOf(defaults);
            if(gear.size()!=6||rules.size()>MAX_RULES||defaults.size()>256)throw new IllegalArgumentException("Loot snapshot exceeds editor limits.");
        }
        public static Snapshot empty(){return new Snapshot(Collections.nCopies(6,-1F),List.of(),List.of(),"","minecraft:pig");}
    }
    public static SpawnerLootDraft unchanged(){return new SpawnerLootDraft(Collections.nCopies(6,-1F),false,List.of());}
    public boolean dirty(){return replaceRules||gear.stream().anyMatch(v->v!=-1F);}
}
