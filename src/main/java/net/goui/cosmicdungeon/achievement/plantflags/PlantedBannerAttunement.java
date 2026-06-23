package net.goui.cosmicdungeon.achievement.plantflags;

import net.goui.cosmicdungeon.playerclass.api.ClassItemUtil;
import net.minecraft.world.item.ItemStack;

public record PlantedBannerAttunement(String classId, int dungeon, int tier, long trace) {
    public static PlantedBannerAttunement from(ItemStack stack) {
        String classId = ClassItemUtil.getClassAttunement(stack);
        Integer dungeon = ClassItemUtil.getDungeon(stack);
        Integer tier = ClassItemUtil.getTier(stack);
        Long trace = ClassItemUtil.getTraceValue(stack);
        if (classId == null || dungeon == null || tier == null || trace == null) return null;
        return new PlantedBannerAttunement(classId, dungeon, tier, trace);
    }
}
