package net.goui.cosmicdungeon.command;

import com.mojang.brigadier.CommandDispatcher;
import net.goui.cosmicdungeon.achievement.AchievementCounterData;
import net.goui.cosmicdungeon.achievement.BindingIdolAchievements;
import net.goui.cosmicdungeon.achievement.CosmicAdvancementUtil;
import net.goui.cosmicdungeon.achievement.VitalExchangeAchievements;
import net.goui.cosmicdungeon.achievement.d1.D1AchievementRegionService;
import net.goui.cosmicdungeon.region.RegionRegistryData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.goui.cosmicdungeon.auth.AccessPolicy;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.commands.arguments.StringArgumentType;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.commands.arguments.item.ItemInput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public final class AchievementCommand {
    private AchievementCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("achievement")
                .requires(AccessPolicy::requireDeveloperOrConsole)
                .then(Commands.literal("grant")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("achievementId", ResourceLocationArgument.id())
                                        .executes(ctx -> grant(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"), ResourceLocationArgument.getId(ctx, "achievementId"))))))
                .then(Commands.literal("counters")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> counters(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"))))
                        .then(Commands.literal("reset")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(ctx -> reset(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"))))))
                .then(Commands.literal("vitalexchange")
                        .then(Commands.argument("provider", EntityArgument.player())
                                .then(Commands.argument("receiver", EntityArgument.player())
                                        .then(Commands.argument("item", ItemArgument.item())
                                                .executes(ctx -> vitalExchange(
                                                        ctx.getSource(),
                                                        EntityArgument.getPlayer(ctx, "provider"),
                                                        EntityArgument.getPlayer(ctx, "receiver"),
                                                        ItemArgument.getItem(ctx, "item")
                                                ))))))
                                .then(Commands.literal("d1regions")
                        .then(Commands.literal("status").executes(ctx -> d1RegionStatus(ctx.getSource())))
                        .then(Commands.literal("set")
                                .then(Commands.argument("regionName", StringArgumentType.word())
                                        .then(Commands.literal("pos1").executes(ctx -> d1RegionSet(ctx.getSource(), StringArgumentType.getString(ctx, "regionName"), true)))
                                        .then(Commands.literal("pos2").executes(ctx -> d1RegionSet(ctx.getSource(), StringArgumentType.getString(ctx, "regionName"), false))))) )
                .then(Commands.literal("d1")
                        .then(Commands.literal("debug")
                                .then(Commands.argument("achievementName", StringArgumentType.word())
                                        .then(Commands.argument("player", EntityArgument.player())
                                                .executes(ctx -> d1Debug(ctx.getSource(), StringArgumentType.getString(ctx, "achievementName"), EntityArgument.getPlayer(ctx, "player")))))))
                .then(Commands.literal("idol")
                        .then(Commands.literal("return")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(ctx -> idolReturn(ctx.getSource(), EntityArgument.getPlayer(ctx, "player")))))
                        .then(Commands.literal("provide")
                                .then(Commands.argument("provider", EntityArgument.player())
                                        .then(Commands.argument("receiver", EntityArgument.player())
                                                .executes(ctx -> idolProvide(
                                                        ctx.getSource(),
                                                        EntityArgument.getPlayer(ctx, "provider"),
                                                        EntityArgument.getPlayer(ctx, "receiver")
                                                )))))));
    }

    private static int grant(CommandSourceStack src, ServerPlayer target, ResourceLocation id) {
        CosmicAdvancementUtil.grant(target, id);
        src.sendSuccess(() -> Component.literal("Granted advancement " + id + " to " + target.getName().getString() + " (criterion: triggered)."), true);
        return 1;
    }

    private static int counters(CommandSourceStack src, ServerPlayer target) {
        var rec = AchievementCounterData.get(target.level().getServer()).get(target.getUUID());
        src.sendSuccess(() -> Component.literal("Achievement counters for " + target.getName().getString() + ":"), false);
        src.sendSuccess(() -> Component.literal(" - bindingIdolReturns: " + rec.bindingIdolReturns()), false);
        src.sendSuccess(() -> Component.literal(" - bindingIdolProvided: " + rec.bindingIdolProvided()), false);
        src.sendSuccess(() -> Component.literal(" - vitalExchangeMask: " + rec.vitalExchangeMask()), false);
        src.sendSuccess(() -> Component.literal(" - d1MusicDiscMask: " + rec.d1MusicDiscMask()), false);
        src.sendSuccess(() -> Component.literal(" - genericCounter1: " + rec.genericCounter1()), false);
        src.sendSuccess(() -> Component.literal(" - genericCounter2: " + rec.genericCounter2()), false);
        return 1;
    }

    private static int idolReturn(CommandSourceStack src, ServerPlayer revivedPlayer) {
        BindingIdolAchievements.recordReturnedThroughBindingIdol(revivedPlayer);
        src.sendSuccess(() -> Component.literal("Recorded binding idol return for " + revivedPlayer.getName().getString() + "."), true);
        return 1;
    }

    private static int idolProvide(CommandSourceStack src, ServerPlayer provider, ServerPlayer receiver) {
        BindingIdolAchievements.recordProvidedBindingIdol(provider, receiver);
        src.sendSuccess(() -> Component.literal("Recorded binding idol provision from " + provider.getName().getString() + " to " + receiver.getName().getString() + "."), true);
        return 1;
    }

    private static int vitalExchange(CommandSourceStack src, ServerPlayer provider, ServerPlayer receiver, ItemInput itemInput) {
        ItemStack stack = itemInput.createItemStack(1, false);
        VitalExchangeAchievements.recordVitalExchange(provider, receiver, stack);
        src.sendSuccess(() -> Component.literal("Recorded vital exchange from " + provider.getName().getString() + " to " + receiver.getName().getString() + " using " + itemInput.getItemName() + "."), true);
        return 1;
    }

    private static int reset(CommandSourceStack src, ServerPlayer target) {
        AchievementCounterData.get(target.level().getServer()).reset(target.getUUID());
        src.sendSuccess(() -> Component.literal("Reset achievement counters for " + target.getName().getString() + "."), true);
        return 1;
    }

    private static int d1RegionSet(CommandSourceStack src, String regionName, boolean pos1) {
        ServerPlayer player = src.getPlayerOrException();
        RegionRegistryData data = RegionRegistryData.get(src.getLevel());
        String dim = src.getLevel().dimension().location().toString();
        String normalized = regionName.toLowerCase(java.util.Locale.ROOT);
        BlockPos pos = player.blockPosition();
        var existing = data.get(normalized).orElse(null);
        BlockPos a = pos1 ? pos : (existing == null ? pos : existing.min());
        BlockPos b = pos1 ? (existing == null ? pos : existing.max()) : pos;
        if (existing != null) data.delete(normalized);
        data.create(normalized, dim, a, b);
        src.sendSuccess(() -> Component.literal("Set " + normalized + " " + (pos1 ? "pos1" : "pos2") + " at " + pos.toShortString()), true);
        return 1;
    }

    private static int d1RegionStatus(CommandSourceStack src) {
        RegionRegistryData data = RegionRegistryData.get(src.getLevel());
        for (String n : java.util.List.of(D1AchievementRegionService.WOODLAND_MANOR, D1AchievementRegionService.WITHER_ROOM, D1AchievementRegionService.CAMP_5)) {
            var opt = data.get(n);
            if (opt.isPresent()) src.sendSuccess(() -> Component.literal(n + ": set").withStyle(ChatFormatting.GREEN), false);
            else src.sendSuccess(() -> Component.literal(n + ": unset").withStyle(ChatFormatting.YELLOW), false);
        }
        return 1;
    }

    private static int d1Debug(CommandSourceStack src, String achievementName, ServerPlayer player) {
        String k = achievementName.toLowerCase(java.util.Locale.ROOT);
        ResourceLocation id = switch (k) {
            case "tired_not_broken" -> net.goui.cosmicdungeon.achievement.CosmicAchievementIds.TIRED_NOT_BROKEN;
            case "sixfold_vigil" -> net.goui.cosmicdungeon.achievement.CosmicAchievementIds.SIXFOLD_VIGIL;
            case "sixfold_vigil_after_dissolution" -> net.goui.cosmicdungeon.achievement.CosmicAchievementIds.SIXFOLD_VIGIL_AFTER_DISSOLUTION;
            case "sixfold_vigil_lone_adversary" -> net.goui.cosmicdungeon.achievement.CosmicAchievementIds.SIXFOLD_VIGIL_LONE_ADVERSARY;
            case "sixfold_vigil_twin_manifestation" -> net.goui.cosmicdungeon.achievement.CosmicAchievementIds.SIXFOLD_VIGIL_TWIN_MANIFESTATION;
            case "cycle_of_recorded_sound" -> net.goui.cosmicdungeon.achievement.CosmicAchievementIds.CYCLE_OF_RECORDED_SOUND;
            case "synchronous_peal" -> net.goui.cosmicdungeon.achievement.CosmicAchievementIds.SYNCHRONOUS_PEAL;
            default -> null;
        };
        if (id == null) { src.sendFailure(Component.literal("Unknown D1 achievement name.")); return 0; }
        CosmicAdvancementUtil.grant(player, id);
        src.sendSuccess(() -> Component.literal("Granted " + id + " to " + player.getGameProfile().getName()), true);
        return 1;
    }
}
