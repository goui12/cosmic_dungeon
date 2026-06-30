package net.goui.cosmicdungeon.region.quest;

import net.goui.cosmicdungeon.achievement.plantflags.PlantFlagData;
import net.goui.cosmicdungeon.achievement.plantflags.PlantFlagService;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class PlantFlagsRegionQuestHandler implements RegionQuestHandler {
    public static final String QUEST_NAME = "plant_flags";

    @Override
    public String questName() {
        return QUEST_NAME;
    }

    @Override
    public int status(CommandSourceStack source) {
        String line = PlantFlagService.statusLine(source.getServer());
        source.sendSuccess(() -> Component.literal("Region quest " + QUEST_NAME + ": " + line).withStyle(ChatFormatting.GRAY), false);
        return 1;
    }

    @Override
    public int reset(CommandSourceStack source) {
        PlantFlagService.clearForRun(source.getServer(), -1L);
        source.sendSuccess(() -> Component.literal("Region quest " + QUEST_NAME + " state reset.").withStyle(ChatFormatting.YELLOW), true);
        return 1;
    }

    @Override
    public int setRegionPos(CommandSourceStack source, boolean isPos1) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer sp = source.getPlayerOrException();
        PlantFlagData data = PlantFlagData.get(source.getServer());
        String dim = sp.level().dimension().location().toString();
        if (isPos1) {
            data.setRegion(dim, sp.blockPosition(), data.regionPos2());
        } else {
            data.setRegion(dim, data.regionPos1(), sp.blockPosition());
        }
        source.sendSuccess(() -> Component.literal("Region quest " + QUEST_NAME + " " + (isPos1 ? "pos1" : "pos2") + " set at " + sp.blockPosition()).withStyle(ChatFormatting.GREEN), true);
        return 1;
    }

    @Override
    public int completeDebug(CommandSourceStack source) {
        boolean ok = PlantFlagService.completeIfReady(source.getServer());
        source.sendSuccess(() -> Component.literal(ok ? "Region quest " + QUEST_NAME + " completed." : "Region quest " + QUEST_NAME + " not ready.").withStyle(ChatFormatting.YELLOW), true);
        return ok ? 1 : 0;
    }
}
