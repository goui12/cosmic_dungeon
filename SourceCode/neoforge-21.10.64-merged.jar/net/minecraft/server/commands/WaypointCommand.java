package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ColorArgument;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.HexColorArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.commands.arguments.WaypointArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.waypoints.Waypoint;
import net.minecraft.world.waypoints.WaypointStyleAsset;
import net.minecraft.world.waypoints.WaypointStyleAssets;
import net.minecraft.world.waypoints.WaypointTransmitter;

public class WaypointCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext buildContext) {
        dispatcher.register(
            Commands.literal("waypoint")
                .requires(Commands.hasPermission(2))
                .then(Commands.literal("list").executes(p_415978_ -> listWaypoints(p_415978_.getSource())))
                .then(
                    Commands.literal("modify")
                        .then(
                            Commands.argument("waypoint", EntityArgument.entity())
                                .then(
                                    Commands.literal("color")
                                        .then(
                                            Commands.argument("color", ColorArgument.color())
                                                .executes(
                                                    p_416036_ -> setWaypointColor(
                                                        p_416036_.getSource(),
                                                        WaypointArgument.getWaypoint(p_416036_, "waypoint"),
                                                        ColorArgument.getColor(p_416036_, "color")
                                                    )
                                                )
                                        )
                                        .then(
                                            Commands.literal("hex")
                                                .then(
                                                    Commands.argument("color", HexColorArgument.hexColor())
                                                        .executes(
                                                            p_415965_ -> setWaypointColor(
                                                                p_415965_.getSource(),
                                                                WaypointArgument.getWaypoint(p_415965_, "waypoint"),
                                                                HexColorArgument.getHexColor(p_415965_, "color")
                                                            )
                                                        )
                                                )
                                        )
                                        .then(
                                            Commands.literal("reset")
                                                .executes(
                                                    p_416058_ -> resetWaypointColor(p_416058_.getSource(), WaypointArgument.getWaypoint(p_416058_, "waypoint"))
                                                )
                                        )
                                )
                                .then(
                                    Commands.literal("style")
                                        .then(
                                            Commands.literal("reset")
                                                .executes(
                                                    p_419433_ -> setWaypointStyle(
                                                        p_419433_.getSource(), WaypointArgument.getWaypoint(p_419433_, "waypoint"), WaypointStyleAssets.DEFAULT
                                                    )
                                                )
                                        )
                                        .then(
                                            Commands.literal("set")
                                                .then(
                                                    Commands.argument("style", ResourceLocationArgument.id())
                                                        .executes(
                                                            p_419432_ -> setWaypointStyle(
                                                                p_419432_.getSource(),
                                                                WaypointArgument.getWaypoint(p_419432_, "waypoint"),
                                                                ResourceKey.create(
                                                                    WaypointStyleAssets.ROOT_ID, ResourceLocationArgument.getId(p_419432_, "style")
                                                                )
                                                            )
                                                        )
                                                )
                                        )
                                )
                        )
                )
        );
    }

    private static int setWaypointStyle(CommandSourceStack source, WaypointTransmitter waypoint, ResourceKey<WaypointStyleAsset> style) {
        mutateIcon(source, waypoint, p_419435_ -> p_419435_.style = style);
        source.sendSuccess(() -> Component.translatable("commands.waypoint.modify.style"), false);
        return 0;
    }

    private static int setWaypointColor(CommandSourceStack source, WaypointTransmitter waypoint, ChatFormatting color) {
        mutateIcon(source, waypoint, p_416671_ -> p_416671_.color = Optional.of(color.getColor()));
        source.sendSuccess(
            () -> Component.translatable("commands.waypoint.modify.color", Component.literal(color.getName()).withStyle(color)), false
        );
        return 0;
    }

    private static int setWaypointColor(CommandSourceStack source, WaypointTransmitter waypoint, Integer color) {
        mutateIcon(source, waypoint, p_415935_ -> p_415935_.color = Optional.of(color));
        source.sendSuccess(
            () -> Component.translatable(
                "commands.waypoint.modify.color", Component.literal(String.format("%06X", ARGB.color(0, color))).withColor(color)
            ),
            false
        );
        return 0;
    }

    private static int resetWaypointColor(CommandSourceStack source, WaypointTransmitter waypoint) {
        mutateIcon(source, waypoint, p_415983_ -> p_415983_.color = Optional.empty());
        source.sendSuccess(() -> Component.translatable("commands.waypoint.modify.color.reset"), false);
        return 0;
    }

    private static int listWaypoints(CommandSourceStack source) {
        ServerLevel serverlevel = source.getLevel();
        Set<WaypointTransmitter> set = serverlevel.getWaypointManager().transmitters();
        String s = serverlevel.dimension().location().toString();
        if (set.isEmpty()) {
            source.sendSuccess(() -> Component.translatable("commands.waypoint.list.empty", s), false);
            return 0;
        } else {
            Component component = ComponentUtils.formatList(
                set.stream()
                    .map(
                        p_450808_ -> {
                            if (p_450808_ instanceof LivingEntity livingentity) {
                                BlockPos blockpos = livingentity.blockPosition();
                                return livingentity.getFeedbackDisplayName()
                                    .copy()
                                    .withStyle(
                                        p_415705_ -> p_415705_.withClickEvent(
                                                new ClickEvent.SuggestCommand(
                                                    "/execute in " + s + " run tp @s " + blockpos.getX() + " " + blockpos.getY() + " " + blockpos.getZ()
                                                )
                                            )
                                            .withHoverEvent(new HoverEvent.ShowText(Component.translatable("chat.coordinates.tooltip")))
                                            .withColor(p_450808_.waypointIcon().color.orElse(-1))
                                    );
                            } else {
                                return Component.literal(p_450808_.toString());
                            }
                        }
                    )
                    .toList(),
                Function.identity()
            );
            source.sendSuccess(() -> Component.translatable("commands.waypoint.list.success", set.size(), s, component), false);
            return set.size();
        }
    }

    private static void mutateIcon(CommandSourceStack source, WaypointTransmitter waypoint, Consumer<Waypoint.Icon> mutator) {
        ServerLevel serverlevel = source.getLevel();
        serverlevel.getWaypointManager().untrackWaypoint(waypoint);
        mutator.accept(waypoint.waypointIcon());
        serverlevel.getWaypointManager().trackWaypoint(waypoint);
    }
}
