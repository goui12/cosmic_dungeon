package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import java.util.function.Consumer;
import net.minecraft.SharedConstants;
import net.minecraft.WorldVersion;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackType;

public class VersionCommand {
    private static final Component HEADER = Component.translatable("commands.version.header");
    private static final Component STABLE = Component.translatable("commands.version.stable.yes");
    private static final Component UNSTABLE = Component.translatable("commands.version.stable.no");

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, boolean isDedicatedServer) {
        dispatcher.register(Commands.literal("version").requires(Commands.hasPermission(isDedicatedServer ? 2 : 0)).executes(p_416547_ -> {
            CommandSourceStack commandsourcestack = p_416547_.getSource();
            commandsourcestack.sendSystemMessage(HEADER);
            dumpVersion(commandsourcestack::sendSystemMessage);
            return 1;
        }));
    }

    public static void dumpVersion(Consumer<Component> output) {
        WorldVersion worldversion = SharedConstants.getCurrentVersion();
        output.accept(Component.translatable("commands.version.id", worldversion.id()));
        output.accept(Component.translatable("commands.version.name", worldversion.name()));
        output.accept(Component.translatable("commands.version.data", worldversion.dataVersion().version()));
        output.accept(Component.translatable("commands.version.series", worldversion.dataVersion().series()));
        output.accept(
            Component.translatable("commands.version.protocol", worldversion.protocolVersion(), "0x" + Integer.toHexString(worldversion.protocolVersion()))
        );
        output.accept(Component.translatable("commands.version.build_time", Component.translationArg(worldversion.buildTime())));
        output.accept(Component.translatable("commands.version.pack.resource", worldversion.packVersion(PackType.CLIENT_RESOURCES).toString()));
        output.accept(Component.translatable("commands.version.pack.data", worldversion.packVersion(PackType.SERVER_DATA).toString()));
        output.accept(worldversion.stable() ? STABLE : UNSTABLE);
    }
}
