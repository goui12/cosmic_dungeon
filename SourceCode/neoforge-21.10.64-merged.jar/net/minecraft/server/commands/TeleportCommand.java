package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.Coordinates;
import net.minecraft.commands.arguments.coordinates.RotationArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

public class TeleportCommand {
    private static final SimpleCommandExceptionType INVALID_POSITION = new SimpleCommandExceptionType(
        Component.translatable("commands.teleport.invalidPosition")
    );

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralCommandNode<CommandSourceStack> literalcommandnode = dispatcher.register(
            Commands.literal("teleport")
                .requires(Commands.hasPermission(2))
                .then(
                    Commands.argument("location", Vec3Argument.vec3())
                        .executes(
                            p_379024_ -> teleportToPos(
                                p_379024_.getSource(),
                                Collections.singleton(p_379024_.getSource().getEntityOrException()),
                                p_379024_.getSource().getLevel(),
                                Vec3Argument.getCoordinates(p_379024_, "location"),
                                null,
                                null
                            )
                        )
                )
                .then(
                    Commands.argument("destination", EntityArgument.entity())
                        .executes(
                            p_139049_ -> teleportToEntity(
                                p_139049_.getSource(),
                                Collections.singleton(p_139049_.getSource().getEntityOrException()),
                                EntityArgument.getEntity(p_139049_, "destination")
                            )
                        )
                )
                .then(
                    Commands.argument("targets", EntityArgument.entities())
                        .then(
                            Commands.argument("location", Vec3Argument.vec3())
                                .executes(
                                    p_379026_ -> teleportToPos(
                                        p_379026_.getSource(),
                                        EntityArgument.getEntities(p_379026_, "targets"),
                                        p_379026_.getSource().getLevel(),
                                        Vec3Argument.getCoordinates(p_379026_, "location"),
                                        null,
                                        null
                                    )
                                )
                                .then(
                                    Commands.argument("rotation", RotationArgument.rotation())
                                        .executes(
                                            p_379025_ -> teleportToPos(
                                                p_379025_.getSource(),
                                                EntityArgument.getEntities(p_379025_, "targets"),
                                                p_379025_.getSource().getLevel(),
                                                Vec3Argument.getCoordinates(p_379025_, "location"),
                                                RotationArgument.getRotation(p_379025_, "rotation"),
                                                null
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
                                                            p_379023_ -> teleportToPos(
                                                                p_379023_.getSource(),
                                                                EntityArgument.getEntities(p_379023_, "targets"),
                                                                p_379023_.getSource().getLevel(),
                                                                Vec3Argument.getCoordinates(p_379023_, "location"),
                                                                null,
                                                                new LookAt.LookAtEntity(
                                                                    EntityArgument.getEntity(p_379023_, "facingEntity"), EntityAnchorArgument.Anchor.FEET
                                                                )
                                                            )
                                                        )
                                                        .then(
                                                            Commands.argument("facingAnchor", EntityAnchorArgument.anchor())
                                                                .executes(
                                                                    p_379021_ -> teleportToPos(
                                                                        p_379021_.getSource(),
                                                                        EntityArgument.getEntities(p_379021_, "targets"),
                                                                        p_379021_.getSource().getLevel(),
                                                                        Vec3Argument.getCoordinates(p_379021_, "location"),
                                                                        null,
                                                                        new LookAt.LookAtEntity(
                                                                            EntityArgument.getEntity(p_379021_, "facingEntity"),
                                                                            EntityAnchorArgument.getAnchor(p_379021_, "facingAnchor")
                                                                        )
                                                                    )
                                                                )
                                                        )
                                                )
                                        )
                                        .then(
                                            Commands.argument("facingLocation", Vec3Argument.vec3())
                                                .executes(
                                                    p_379022_ -> teleportToPos(
                                                        p_379022_.getSource(),
                                                        EntityArgument.getEntities(p_379022_, "targets"),
                                                        p_379022_.getSource().getLevel(),
                                                        Vec3Argument.getCoordinates(p_379022_, "location"),
                                                        null,
                                                        new LookAt.LookAtPosition(Vec3Argument.getVec3(p_379022_, "facingLocation"))
                                                    )
                                                )
                                        )
                                )
                        )
                        .then(
                            Commands.argument("destination", EntityArgument.entity())
                                .executes(
                                    p_139011_ -> teleportToEntity(
                                        p_139011_.getSource(),
                                        EntityArgument.getEntities(p_139011_, "targets"),
                                        EntityArgument.getEntity(p_139011_, "destination")
                                    )
                                )
                        )
                )
        );
        dispatcher.register(Commands.literal("tp").requires(Commands.hasPermission(2)).redirect(literalcommandnode));
    }

    private static int teleportToEntity(CommandSourceStack source, Collection<? extends Entity> targets, Entity destination) throws CommandSyntaxException {
        for (Entity entity : targets) {
            performTeleport(
                source,
                entity,
                (ServerLevel)destination.level(),
                destination.getX(),
                destination.getY(),
                destination.getZ(),
                EnumSet.noneOf(Relative.class),
                destination.getYRot(),
                destination.getXRot(),
                null
            );
        }

        if (targets.size() == 1) {
            source.sendSuccess(
                () -> Component.translatable(
                    "commands.teleport.success.entity.single", targets.iterator().next().getDisplayName(), destination.getDisplayName()
                ),
                true
            );
        } else {
            source.sendSuccess(() -> Component.translatable("commands.teleport.success.entity.multiple", targets.size(), destination.getDisplayName()), true);
        }

        return targets.size();
    }

    private static int teleportToPos(
        CommandSourceStack source,
        Collection<? extends Entity> targets,
        ServerLevel level,
        Coordinates position,
        @Nullable Coordinates rotation,
        @Nullable LookAt lookAt
    ) throws CommandSyntaxException {
        Vec3 vec3 = position.getPosition(source);
        Vec2 vec2 = rotation == null ? null : rotation.getRotation(source);

        for (Entity entity : targets) {
            Set<Relative> set = getRelatives(position, rotation, entity.level().dimension() == level.dimension());
            if (vec2 == null) {
                performTeleport(source, entity, level, vec3.x, vec3.y, vec3.z, set, entity.getYRot(), entity.getXRot(), lookAt);
            } else {
                performTeleport(source, entity, level, vec3.x, vec3.y, vec3.z, set, vec2.y, vec2.x, lookAt);
            }
        }

        if (targets.size() == 1) {
            source.sendSuccess(
                () -> Component.translatable(
                    "commands.teleport.success.location.single",
                    targets.iterator().next().getDisplayName(),
                    formatDouble(vec3.x),
                    formatDouble(vec3.y),
                    formatDouble(vec3.z)
                ),
                true
            );
        } else {
            source.sendSuccess(
                () -> Component.translatable(
                    "commands.teleport.success.location.multiple", targets.size(), formatDouble(vec3.x), formatDouble(vec3.y), formatDouble(vec3.z)
                ),
                true
            );
        }

        return targets.size();
    }

    private static Set<Relative> getRelatives(Coordinates position, @Nullable Coordinates rotation, boolean absolute) {
        Set<Relative> set = Relative.direction(position.isXRelative(), position.isYRelative(), position.isZRelative());
        Set<Relative> set1 = absolute ? Relative.position(position.isXRelative(), position.isYRelative(), position.isZRelative()) : Set.of();
        Set<Relative> set2 = rotation == null ? Relative.ROTATION : Relative.rotation(rotation.isYRelative(), rotation.isXRelative());
        return Relative.union(set, set1, set2);
    }

    private static String formatDouble(double value) {
        return String.format(Locale.ROOT, "%f", value);
    }

    private static void performTeleport(
        CommandSourceStack source,
        Entity target,
        ServerLevel level,
        double x,
        double y,
        double z,
        Set<Relative> relatives,
        float yRot,
        float xRot,
        @Nullable LookAt lookAt
    ) throws CommandSyntaxException {
        net.neoforged.neoforge.event.entity.EntityTeleportEvent.TeleportCommand event = net.neoforged.neoforge.event.EventHooks.onEntityTeleportCommand(target, x, y, z);
        if (event.isCanceled()) {
             return;
        }
        x = event.getTargetX();
        y = event.getTargetY();
        z = event.getTargetZ();

        BlockPos blockpos = BlockPos.containing(x, y, z);
        if (!Level.isInSpawnableBounds(blockpos)) {
            throw INVALID_POSITION.create();
        } else {
            double d0 = relatives.contains(Relative.X) ? x - target.getX() : x;
            double d1 = relatives.contains(Relative.Y) ? y - target.getY() : y;
            double d2 = relatives.contains(Relative.Z) ? z - target.getZ() : z;
            float f = relatives.contains(Relative.Y_ROT) ? yRot - target.getYRot() : yRot;
            float f1 = relatives.contains(Relative.X_ROT) ? xRot - target.getXRot() : xRot;
            float f2 = Mth.wrapDegrees(f);
            float f3 = Mth.wrapDegrees(f1);
            if (target.teleportTo(level, d0, d1, d2, relatives, f2, f3, true)) {
                if (lookAt != null) {
                    lookAt.perform(source, target);
                }

                if (!(target instanceof LivingEntity livingentity && livingentity.isFallFlying())) {
                    target.setDeltaMovement(target.getDeltaMovement().multiply(1.0, 0.0, 1.0));
                    target.setOnGround(true);
                }

                if (target instanceof PathfinderMob pathfindermob) {
                    pathfindermob.getNavigation().stop();
                }
            }
        }
    }
}
