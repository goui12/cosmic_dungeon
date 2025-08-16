package net.goui.cosmicdungeon.common.properties;

import net.goui.cosmicdungeon.common.color.AmethystColor;
import net.minecraft.world.level.block.state.properties.EnumProperty;

public class ModProperties {
    public static final EnumProperty<AmethystColor> COLOR =
            EnumProperty.create("color", AmethystColor.class);
}