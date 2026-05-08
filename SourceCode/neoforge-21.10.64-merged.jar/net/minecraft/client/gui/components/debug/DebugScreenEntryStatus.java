package net.minecraft.client.gui.components.debug;

import net.minecraft.util.StringRepresentable;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public enum DebugScreenEntryStatus implements StringRepresentable {
    ALWAYS_ON("alwaysOn"),
    IN_F3("inF3"),
    NEVER("never");

    public static final StringRepresentable.EnumCodec<DebugScreenEntryStatus> CODEC = StringRepresentable.fromEnum(DebugScreenEntryStatus::values);
    private final String name;

    private DebugScreenEntryStatus(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }
}
