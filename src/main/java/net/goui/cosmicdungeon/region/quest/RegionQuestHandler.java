package net.goui.cosmicdungeon.region.quest;

import net.minecraft.commands.CommandSourceStack;

public interface RegionQuestHandler {
    String questName();

    int status(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException;

    int reset(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException;

    int setRegionPos(CommandSourceStack source, boolean isPos1) throws com.mojang.brigadier.exceptions.CommandSyntaxException;

    int completeDebug(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException;
}
