package net.goui.cosmicdungeon.classsystem;

import java.util.List;

public final class ClassConfig {
    public List<ClassEntry> classes;

    public static final class ClassEntry {
        public String name; // e.g. "pyroclast"
        public List<Rule> rules;
    }

    /** A Rule can set a base or add a modifier to an attribute. */
    public static final class Rule {
        /** Registry key of the attribute, e.g. "minecraft:attack_speed" */
        public String attribute;

        /** One of: "base", "add", "mul_base", "mul_total" */
        public String mode;

        /** Numeric value. For "base", this becomes the base value. For others, the modifier amount. */
        public double value;

        /** Optional readable label; used as modifier name in NBT. */
        public String label;
    }
}
