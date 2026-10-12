package net.goui.cosmicdungeon.spawner.editor;

import java.util.*;
import net.goui.cosmicdungeon.block.entity.CosmicSpawnerPreset.Slot;

/** Only bounded item/enchantment identifiers cross C2S, never client-authored item components. */
public record SpawnerEquipmentDraft(List<Edit> slots) {
    public static final int MAX_ENCHANTMENTS=64;
    public SpawnerEquipmentDraft {
        slots=List.copyOf(slots);
        if(slots.size()!=Slot.values().length)throw new IllegalArgumentException("Six equipment slots required.");
    }
    public enum Origin { KEEP, ITEM, HELD, CLEAR }
    public record Enchant(String id,int level) {
        public Enchant { Objects.requireNonNull(id); if(id.length()>256)throw new IllegalArgumentException("Enchantment ID too long."); }
    }
    public record Edit(Origin origin,String itemId,boolean advanced,List<Enchant> enchantments) {
        public Edit {
            Objects.requireNonNull(origin);Objects.requireNonNull(itemId);enchantments=List.copyOf(enchantments);
            if(itemId.length()>256||enchantments.size()>MAX_ENCHANTMENTS)throw new IllegalArgumentException("Equipment draft too large.");
        }
        public static Edit keep(){return new Edit(Origin.KEEP,"",false,List.of());}
        public boolean changed(){return origin!=Origin.KEEP||!enchantments.isEmpty();}
    }
    public static SpawnerEquipmentDraft unchanged(){return new SpawnerEquipmentDraft(Collections.nCopies(6,Edit.keep()));}
    public Edit get(Slot slot){return slots.get(slot.ordinal());}
    public SpawnerEquipmentDraft with(Slot slot,Edit edit){var copy=new ArrayList<>(slots);copy.set(slot.ordinal(),edit);return new SpawnerEquipmentDraft(copy);}
    public boolean dirty(){return slots.stream().anyMatch(Edit::changed);}
}
