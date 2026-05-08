package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.Coordinates;
import net.minecraft.commands.arguments.coordinates.RotationArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec2;

public class RotateCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("rotate")
                .requires(Commands.hasPermission(2))
                .then(
                    Commands.argument("target", EntityArgument.entity())
                        .then(
                            Commands.argument("rotation", RotationArgument.rotation())
                                .executes(
                                    p_379342_ -> rotate(
                                        p_379342_.getSource(),
                                        EntityArgument.getEntity(p_379342_, "target"),
                                        RotationArgument.getRotation(p_379342_, "rotation")
                                    )
                                )
                        )
                        .then(
                            Commands.literal("facing")
                                .then(
                                    Commands.literal("entity")
                                        .then(
                                            Commands.argument("facingEntity", EntityArgument.entity())
                                                .executes(
                                                    p_379526_ -> rotate(
                                                        p_379526_.getSource(),
                                                        EntityArgument.getEntity(p_379526_, "target"),
                                                        new LookAt.LookAtEntity(
                                                            EntityArgument.getEntity(p_379526_, "facingEntity"), EntityAnchorArgument.Anchor.FEET
                                                        )
                                                    )
                                                )
                                                .then(
                                                    Commands.argument("facingAnchor", EntityAnchorArgument.anchor())
                                                        .executes(
                                                            p_380018_ -> rotate(
                                                                p_380018_.getSource(),
                                                                EntityArgument.getEntity(p_380018_, "target"),
                                                                new LookAt.LookAtEntity(
                                                                    EntityArgument.getEntity(p_380018_, "facingEntity"),
                                                                    EntityAnchorArgument.getAnchor(p_380018_, "facingAnchor")
                                                                )
                                                            )
                                                        )
                                                )
                                        )
                                )
                                .then(
                                    Commands.argument("facingLocation", Vec3Argument.vec3())
                                        .executes(
                                            p_379579_ -> rotate(
                                                p_379579_.getSource(),
                                                EntityArgument.getEntity(p_379579_, "target"),
                                                new LookAt.LookAtPosition(Vec3Argument.getVec3(p_379579_, "facingLocation"))
                                            )
                                        )
                                )
                        )
                )
        );
    }

    private static int rotate(CommandSourceStack source, Entity entity, Coordinates coordinates) {
        Vec2 vec2 = coordinates.getRotation(source);
        float f = coordinates.isYRelative() ? vec2.y - entity.getYRot() : vec2.y;
        float f1 = coordinates.isXRelative() ? vec2.x - entity.getXRot() : vec2.x;
        entity.forceSetRotation(f, coordinates.isYRelative(), f1, coordinates.isXRelative());
        source.sendSuccess(() -> Component.translatable("commands.rotate.success", entity.getDisplayName()), true);
        return 1;
    }

    private static int rotate(CommandSourceStack source, Entity entity, LookAt lookAt) {
        lookAt.perform(source, entity);
        source.sendSuccess(() -> Component.translatable("commands.rotate.success", entity.getDisplayName()), true);
        return 1;
    }
}
