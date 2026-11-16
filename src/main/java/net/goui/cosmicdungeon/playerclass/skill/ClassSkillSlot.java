package net.goui.cosmicdungeon.playerclass.skill;

/** Stable generic input slots. Changing class never changes the native keybinding identity. */
public enum ClassSkillSlot {
    PRIMARY("primary", "Primary class skill"),
    SKILL_1("skill_1", "Class skill 1"),
    SKILL_2("skill_2", "Class skill 2"),
    SKILL_3("skill_3", "Class skill 3");
    private final String id, label;
    ClassSkillSlot(String id, String label) { this.id=id; this.label=label; }
    public String id() { return id; }
    public String label() { return label; }
}
