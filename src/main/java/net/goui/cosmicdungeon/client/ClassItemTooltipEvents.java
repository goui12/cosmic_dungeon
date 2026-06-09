package net.goui.cosmicdungeon.client;

import net.goui.cosmicdungeon.playerclass.api.ClassItemUtil;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

public final class ClassItemTooltipEvents {
    private ClassItemTooltipEvents() {}

    public static void onItemTooltip(ItemTooltipEvent event) {
        String classId = ClassItemUtil.getClassAttunement(event.getItemStack());
        if (classId == null) return;

        event.getToolTip().add(Component.literal(ClassItemUtil.displayNameForClass(classId))
                .withStyle(style -> style
                        .withColor(ClassItemUtil.colorForClass(classId))
                        .withBold(true)
                        .withItalic(true)));
    }
}
