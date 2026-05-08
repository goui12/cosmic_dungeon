package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import java.util.Collection;
import java.util.Collections;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.commands.arguments.coordinates.Coordinates;
import net.minecraft.commands.arguments.coordinates.RotationArgument;
import net.minecraft.commands.arguments.coordinates.WorldCoordinates;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.phys.Vec2;

public class SetSpawnCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("spawnpoint")
                .requires(Commands.hasPermission(2))
                .executes(
                    p_450787_ -> setSpawn(
                        p_450787_.getSource(),
                        Collections.singleton(p_450787_.getSource().getPlayerOrException()),
                        BlockPos.containing(p_450787_.getSource().getPosition()),
                        WorldCoordinates.ZERO_ROTATION
                    )
                )
                .then(
                    Commands.argument("targets", EntityArgument.players())
                        .executes(
                            p_450786_ -> setSpawn(
                                p_450786_.getSource(),
                                EntityArgument.getPlayers(p_450786_, "targets"),
                                BlockPos.containing(p_450786_.getSource().getPosition()),
                                WorldCoordinates.ZERO_ROTATION
                            )
                        )
                        .then(
                            Commands.argument("pos", BlockPosArgument.blockPos())
                                .executes(
                                    p_450785_ -> setSpawn(
                                        p_450785_.getSource(),
                                        EntityArgument.getPlayers(p_450785_, "targets"),
                                        BlockPosArgument.getSpawnablePos(p_450785_, "pos"),
                                        WorldCoordinates.ZERO_ROTATION
                                    )
                                )
                                .then(
                                    Commands.argument("rotation", RotationArgument.rotation())
                                        .executes(
                                            p_450793_ -> setSpawn(
                                                p_450793_.getSource(),
                                                EntityArgument.getPlayers(p_450793_, "targets"),
                                                BlockPosArgument.getSpawnablePos(p_450793_, "pos"),
                                                RotationArgument.getRotation(p_450793_, "rotation")
                                            )
                                        )
                                )
                        )
                )
        );
    }

    private static int setSpawn(CommandSourceStack source, Collection<ServerPlayer> targets, BlockPos pos, Coordinates rotation) {
        ResourceKey<Level> resourcekey = source.getLevel().dimension();
        Vec2 vec2 = rotation.getRotation(source);
        float f = vec2.y;
        float f1 = vec2.x;

        for (ServerPlayer serverplayer : targets) {
            serverplayer.setRespawnPosition(new ServerPlayer.RespawnConfig(LevelData.RespawnData.of(resourcekey, pos, f, f1), true), false);
        }

        String s = resourcekey.location().toString();
        if (targets.size() == 1) {
            source.sendSuccess(
                () -> Component.translatable(
                    "commands.spawnpoint.success.single",
                    pos.getX(),
                    pos.getY(),
                    pos.getZ(),
                    f,
                    f1,
                    source.getLevel().getDescription(), // Neo: Use dimension translation, if one exists
                    targets.iterator().next().getDisplayName()
                ),
                true
            );
        } else {
            source.sendSuccess(
                () -> Component.translatable(
                    "commands.spawnpoint.success.multiple", pos.getX(), pos.getY(), pos.getZ(), f, f1, s, targets.size()
                ),
                true
            );
        }

        return targets.size();
    }
}
