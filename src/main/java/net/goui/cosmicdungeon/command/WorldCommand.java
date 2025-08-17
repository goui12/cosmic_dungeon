package net.goui.cosmicdungeon.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

public final class WorldCommand {
    private WorldCommand() {}

    // Tab-complete: aliases + all currently LOADED level keys
    private static final SuggestionProvider<CommandSourceStack> DIM_SUGGEST = (ctx, builder) -> {
        var source = ctx.getSource();
        List<String> names = new ArrayList<>();

        // Friendly aliases
        names.add("world");
        names.add("overworld");
        names.add("nether");
        names.add("end");

        // Loaded dimensions (e.g., minecraft:overworld, cosmicdungeon:dungeon_1, etc.)
        for (ResourceKey<Level> k : source.getServer().levelKeys()) {
            String id = k.location().toString();
            if (!names.contains(id)) names.add(id);
        }

        return SharedSuggestionProvider.suggest(names, builder);
    };

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("world")
                .requires(src -> src.hasPermission(0)) // change to 2 for OP-only
                .then(Commands.argument("target", StringArgumentType.word())
                        .suggests(DIM_SUGGEST)
                        .executes(ctx -> {
                            String arg = StringArgumentType.getString(ctx, "target");
                            CommandSourceStack source = ctx.getSource();
                            ServerPlayer player = source.getPlayerOrException();

                            ResourceKey<Level> dimKey = resolveDimensionKey(arg);
                            if (dimKey == null) {
                                source.sendFailure(Component.literal("Unknown dimension: " + arg));
                                return 0;
                            }

                            ServerLevel dest = source.getServer().getLevel(dimKey);
                            if (dest == null) {
                                source.sendFailure(Component.literal("Dimension not loaded: " + dimKey.location()));
                                return 0;
                            }

                            // Start with the world's configured spawn
                            BlockPos spawn = dest.getSharedSpawnPos();

                            // Find a safe standable position near/at spawn
                            BlockPos safe = ensureStandable(dest, spawn);

                            // Orientation: use world spawn angle
                            float yaw = dest.getSharedSpawnAngle();
                            float pitch = 0f;

                            // Center on block
                            double x = safe.getX() + 0.5;
                            double y = safe.getY();
                            double z = safe.getZ() + 0.5;

                            // Cross-dimension teleport (absolute position/rotation)
                            player.teleportTo(dest, x, y, z, java.util.Set.of(), yaw, pitch, false);

                            source.sendSuccess(() -> Component.literal(
                                    "Teleported to " + dimKey.location() +
                                            " spawn (" + safe.getX() + " " + safe.getY() + " " + safe.getZ() + ")"
                            ), false);
                            return 1;
                        })
                )
        );
    }

    private static BlockPos ensureStandable(ServerLevel level, BlockPos pos) {
        var m = pos.mutable();
        int minY = level.getMinY();
        int maxY = minY + level.getLogicalHeight() - 1; // <- was getMaxBuildHeight()

        // Climb up until there is solid ground and 2 air blocks
        for (int y = Math.max(minY + 1, m.getY()); y < maxY - 1; y++) {
            m.setY(y);
            if (isStandable(level, m)) return m.immutable();
        }

        // Fallback: world surface at spawn XZ
        int surfaceY = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE, pos.getX(), pos.getZ());
        m.set(pos.getX(), Math.max(surfaceY, minY + 1), pos.getZ());

        // Nudge upward a bit if needed
        for (int y = m.getY(); y < Math.min(m.getY() + 8, maxY - 1); y++) {
            m.setY(y);
            if (isStandable(level, m)) return m.immutable();
        }
        return m.immutable();
    }


    private static boolean isStandable(ServerLevel level, BlockPos pos) {
        var below = pos.below();
        var feet = level.getBlockState(pos);
        var head = level.getBlockState(pos.above());

        // solid ground below (sturdy upward face), empty feet & head (no collision), and no fluid
        boolean sturdyBelow = level.getBlockState(below).isFaceSturdy(level, below, Direction.UP);
        boolean noFluid = level.getFluidState(pos).isEmpty() && level.getFluidState(pos.above()).isEmpty();
        boolean emptySpace = feet.getCollisionShape(level, pos).isEmpty()
                && head.getCollisionShape(level, pos.above()).isEmpty();

        return sturdyBelow && noFluid && emptySpace;
    }

    private static ResourceKey<Level> resolveDimensionKey(String input) {
        String s = input.toLowerCase();
        if (s.equals("world") || s.equals("overworld") || s.equals("ow")) return Level.OVERWORLD;
        if (s.equals("nether")) return Level.NETHER;
        if (s.equals("end") || s.equals("the_end")) return Level.END;

        ResourceLocation id = s.contains(":") ? ResourceLocation.tryParse(s) : ResourceLocation.tryBuild("cosmicdungeon", s);
        if (id == null) return null;

        // Build a Level key like minecraft:overworld or yourmod:foo
        return ResourceKey.create(Registries.DIMENSION, id);
    }
}
