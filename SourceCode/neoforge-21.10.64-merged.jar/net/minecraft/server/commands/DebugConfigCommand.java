package net.minecraft.server.commands;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceOrIdArgument;
import net.minecraft.commands.arguments.UuidArgument;
import net.minecraft.core.Holder;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.ClientboundShowDialogPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.dialog.Dialog;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerConfigurationPacketListenerImpl;

public class DebugConfigCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext context) {
        dispatcher.register(
            Commands.literal("debugconfig")
                .requires(Commands.hasPermission(3))
                .then(
                    Commands.literal("config")
                        .then(
                            Commands.argument("target", EntityArgument.player())
                                .executes(p_294114_ -> config(p_294114_.getSource(), EntityArgument.getPlayer(p_294114_, "target")))
                        )
                )
                .then(
                    Commands.literal("unconfig")
                        .then(
                            Commands.argument("target", UuidArgument.uuid())
                                .suggests(
                                    (p_295936_, p_294731_) -> SharedSuggestionProvider.suggest(getUuidsInConfig(p_295936_.getSource().getServer()), p_294731_)
                                )
                                .executes(p_294910_ -> unconfig(p_294910_.getSource(), UuidArgument.getUuid(p_294910_, "target")))
                        )
                )
                .then(
                    Commands.literal("dialog")
                        .then(
                            Commands.argument("target", UuidArgument.uuid())
                                .suggests(
                                    (p_426945_, p_426946_) -> SharedSuggestionProvider.suggest(getUuidsInConfig(p_426945_.getSource().getServer()), p_426946_)
                                )
                                .then(
                                    Commands.argument("dialog", ResourceOrIdArgument.dialog(context))
                                        .executes(
                                            p_426944_ -> showDialog(
                                                (CommandSourceStack)p_426944_.getSource(),
                                                UuidArgument.getUuid(p_426944_, "target"),
                                                ResourceOrIdArgument.getDialog(p_426944_, "dialog")
                                            )
                                        )
                                )
                        )
                )
        );
    }

    private static Iterable<String> getUuidsInConfig(MinecraftServer server) {
        Set<String> set = new HashSet<>();

        for (Connection connection : server.getConnection().getConnections()) {
            if (connection.getPacketListener() instanceof ServerConfigurationPacketListenerImpl serverconfigurationpacketlistenerimpl) {
                set.add(serverconfigurationpacketlistenerimpl.getOwner().id().toString());
            }
        }

        return set;
    }

    private static int config(CommandSourceStack source, ServerPlayer target) {
        GameProfile gameprofile = target.getGameProfile();
        target.connection.switchToConfig();
        source.sendSuccess(() -> Component.literal("Switched player " + gameprofile.name() + "(" + gameprofile.id() + ") to config mode"), false);
        return 1;
    }

    @Nullable
    private static ServerConfigurationPacketListenerImpl findConfigPlayer(MinecraftServer server, UUID target) {
        for (Connection connection : server.getConnection().getConnections()) {
            if (connection.getPacketListener() instanceof ServerConfigurationPacketListenerImpl serverconfigurationpacketlistenerimpl
                && serverconfigurationpacketlistenerimpl.getOwner().id().equals(target)) {
                return serverconfigurationpacketlistenerimpl;
            }
        }

        return null;
    }

    private static int unconfig(CommandSourceStack source, UUID target) {
        ServerConfigurationPacketListenerImpl serverconfigurationpacketlistenerimpl = findConfigPlayer(source.getServer(), target);
        if (serverconfigurationpacketlistenerimpl != null) {
            serverconfigurationpacketlistenerimpl.returnToWorld();
            return 1;
        } else {
            source.sendFailure(Component.literal("Can't find player to unconfig"));
            return 0;
        }
    }

    private static int showDialog(CommandSourceStack source, UUID target, Holder<Dialog> dialog) {
        ServerConfigurationPacketListenerImpl serverconfigurationpacketlistenerimpl = findConfigPlayer(source.getServer(), target);
        if (serverconfigurationpacketlistenerimpl != null) {
            serverconfigurationpacketlistenerimpl.send(new ClientboundShowDialogPacket(dialog));
            return 1;
        } else {
            source.sendFailure(Component.literal("Can't find player to talk to"));
            return 0;
        }
    }
}
