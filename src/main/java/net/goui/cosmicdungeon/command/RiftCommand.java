package net.goui.cosmicdungeon.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.goui.cosmicdungeon.block.ModBlocks;
import net.goui.cosmicdungeon.rift.RiftRegistryData;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import java.util.List;
import java.util.Locale;

public final class RiftCommand {
    private RiftCommand() {}

    private static final int MAX_TILES = 64 * 64;

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("rift")
                        .requires(src -> src.hasPermission(2))

                        // ---------------- LIST ----------------
                        .then(Commands.literal("list")
                                .executes(ctx -> cmdList(ctx.getSource(), null))
                                .then(Commands.argument("dimension", StringArgumentType.word())
                                        .executes(ctx -> cmdList(
                                                ctx.getSource(),
                                                StringArgumentType.getString(ctx, "dimension")
                                        )))
                        )

                        // ---------------- DELETE ----------------
                        .then(Commands.literal("delete")

                                // coords (current dimension)
                                .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                        .executes(ctx -> cmdDeleteByPos(
                                                ctx.getSource(),
                                                null,
                                                BlockPosArgument.getBlockPos(ctx, "pos")
                                        )))

                                // name (current dimension)
                                .then(Commands.argument("name", StringArgumentType.greedyString())
                                        .executes(ctx -> cmdDeleteByName(
                                                ctx.getSource(),
                                                null,
                                                StringArgumentType.getString(ctx, "name")
                                        )))

                                // dimension-prefixed
                                .then(Commands.argument("dimension", StringArgumentType.word())

                                        .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                                .executes(ctx -> cmdDeleteByPos(
                                                        ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "dimension"),
                                                        BlockPosArgument.getBlockPos(ctx, "pos")
                                                )))

                                        .then(Commands.argument("name", StringArgumentType.greedyString())
                                                .executes(ctx -> cmdDeleteByName(
                                                        ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "dimension"),
                                                        StringArgumentType.getString(ctx, "name")
                                                )))
                                )
                        )
        );
    }

    /* ==================== LIST ==================== */

    private static int cmdList(CommandSourceStack src, String dimensionArgRaw) {
        ServerPlayer p = src.getPlayer();
        if (p == null) return 0;
        if (!(p.level() instanceof ServerLevel current)) return 0;

        ServerLevel level = dimensionArgRaw == null
                ? current
                : resolveLevel(current, dimensionArgRaw);

        if (level == null) {
            src.sendFailure(Component.literal("Unknown dimension: ")
                    .append(Component.literal(String.valueOf(dimensionArgRaw)).withStyle(ChatFormatting.YELLOW)));
            return 0;
        }

        RiftRegistryData data = RiftRegistryData.get(level);
        List<RiftRegistryData.PortalRecord> all = data.listAllPortalsSorted();
        ResourceLocation dimId = level.dimension().location();

        src.sendSuccess(() ->
                        Component.literal("Rifts in ").withStyle(ChatFormatting.DARK_AQUA)
                                .append(Component.literal(dimId.toString()).withStyle(ChatFormatting.AQUA)),
                false
        );

        if (dimensionArgRaw != null) {
            src.sendSuccess(() ->
                            Component.literal("FYI: ")
                                    .withStyle(ChatFormatting.GOLD)
                                    .append(Component.literal("Chat [delete] buttons act in your current dimension only.")
                                            .withStyle(ChatFormatting.GRAY)),
                    false
            );
        }

        int shown = 0;

        for (var pr : all) {
            BlockPos anchor = BlockPos.of(pr.anchorLong());
            if (level.getBlockState(anchor).getBlock() != ModBlocks.COSMIC_RIFT_TILE.get()) continue;

            shown++;

            String name = pr.portalName().isBlank() ? "(unnamed rift)" : pr.portalName();
            String dest = pr.destinationName().isBlank() ? "(no destination)" : pr.destinationName();

            MutableComponent line = Component.literal(" - ").withStyle(ChatFormatting.GRAY)
                    .append(Component.literal(name).withStyle(ChatFormatting.WHITE))
                    .append(Component.literal(" → ").withStyle(ChatFormatting.DARK_GRAY))
                    .append(Component.literal(dest).withStyle(ChatFormatting.YELLOW))
                    .append(Component.literal(" @ ").withStyle(ChatFormatting.DARK_GRAY))
                    .append(copyable(anchor.toShortString(), anchor.toShortString()))
                    .append(Component.literal(" "))
                    .append(Component.literal("[delete]").withStyle(
                            Style.EMPTY.withColor(ChatFormatting.RED).withUnderlined(true)
                                    .withClickEvent(new ClickEvent.RunCommand(
                                            "/rift delete " + anchor.getX() + " " + anchor.getY() + " " + anchor.getZ()
                                    ))
                    ));

            src.sendSuccess(() -> line, false);
        }

        if (shown == 0) {
            src.sendSuccess(() -> Component.literal(" - (none)").withStyle(ChatFormatting.GRAY), false);
        }

        return 1;
    }

    /* ==================== DELETE BY POS ==================== */

    private static int cmdDeleteByPos(CommandSourceStack src, String dimRaw, BlockPos anchor) {
        ServerPlayer p = src.getPlayer();
        if (p == null) return 0;
        if (!(p.level() instanceof ServerLevel current)) return 0;

        ServerLevel level = dimRaw == null ? current : resolveLevel(current, dimRaw);
        if (level == null) {
            src.sendFailure(Component.literal("Unknown dimension: ")
                    .append(Component.literal(String.valueOf(dimRaw)).withStyle(ChatFormatting.YELLOW)));
            return 0;
        }

        if (level.getBlockState(anchor).getBlock() != ModBlocks.COSMIC_RIFT_TILE.get()) {
            src.sendFailure(Component.literal("Not a rift tile: ")
                    .append(Component.literal(anchor.toShortString()).withStyle(ChatFormatting.YELLOW)));
            return 0;
        }

        int destroyed = destroyConnectedRift(level, anchor, p);

        src.sendSuccess(() ->
                        Component.literal("Deleted rift (")
                                .withStyle(ChatFormatting.GREEN)
                                .append(Component.literal(String.valueOf(destroyed)).withStyle(ChatFormatting.AQUA))
                                .append(Component.literal(" tiles)")),
                false
        );
        return 1;
    }

    /* ==================== DELETE BY NAME ==================== */

    private static int cmdDeleteByName(CommandSourceStack src, String dimRaw, String nameRaw) {
        ServerPlayer p = src.getPlayer();
        if (p == null) return 0;
        if (!(p.level() instanceof ServerLevel current)) return 0;

        ServerLevel level = dimRaw == null ? current : resolveLevel(current, dimRaw);
        if (level == null) {
            src.sendFailure(Component.literal("Unknown dimension: ")
                    .append(Component.literal(String.valueOf(dimRaw)).withStyle(ChatFormatting.YELLOW)));
            return 0;
        }

        String query = nameRaw.trim();
        RiftRegistryData data = RiftRegistryData.get(level);
        List<RiftRegistryData.PortalRecord> all = data.listAllPortalsSorted();

        RiftRegistryData.PortalRecord match = null;
        int count = 0;

        for (var pr : all) {
            if (!pr.portalName().equalsIgnoreCase(query)) continue;
            BlockPos a = BlockPos.of(pr.anchorLong());
            if (level.getBlockState(a).getBlock() == ModBlocks.COSMIC_RIFT_TILE.get()) {
                match = pr;
                count++;
            }
        }

        if (count == 0) {
            src.sendFailure(Component.literal("No rift named ")
                    .append(Component.literal(query).withStyle(ChatFormatting.YELLOW)));
            return 0;
        }

        if (count > 1) {
            src.sendFailure(Component.literal("Multiple rifts named ")
                    .append(Component.literal(query).withStyle(ChatFormatting.YELLOW))
                    .append(Component.literal(" — delete by coordinates.")));
            return 0;
        }

        BlockPos anchor = BlockPos.of(match.anchorLong());
        int destroyed = destroyConnectedRift(level, anchor, p);

        src.sendSuccess(() ->
                        Component.literal("Deleted rift ")
                                .withStyle(ChatFormatting.GREEN)
                                .append(Component.literal(query).withStyle(ChatFormatting.AQUA))
                                .append(Component.literal(" ("))
                                .append(Component.literal(String.valueOf(destroyed)).withStyle(ChatFormatting.AQUA))
                                .append(Component.literal(" tiles)")),
                false
        );

        return 1;
    }

    /* ==================== CORE DELETE ==================== */

    private static int destroyConnectedRift(ServerLevel level, BlockPos origin, ServerPlayer actor) {
        Block rift = ModBlocks.COSMIC_RIFT_TILE.get();
        int y = origin.getY();

        LongOpenHashSet visited = new LongOpenHashSet();
        LongArrayFIFOQueue q = new LongArrayFIFOQueue();

        visited.add(origin.asLong());
        q.enqueue(origin.asLong());

        while (!q.isEmpty() && visited.size() <= MAX_TILES) {
            BlockPos cur = BlockPos.of(q.dequeueLong());

            for (BlockPos n : new BlockPos[]{cur.north(), cur.south(), cur.east(), cur.west()}) {
                if (n.getY() != y) continue;
                long k = n.asLong();
                if (visited.contains(k)) continue;
                if (level.getBlockState(n).getBlock() != rift) continue;

                visited.add(k);
                q.enqueue(k);
            }
        }

        RiftRegistryData.get(level).onRiftTilesBroken(level, visited);

        LongIterator it = visited.iterator();
        while (it.hasNext()) {
            level.destroyBlock(BlockPos.of(it.nextLong()), false, actor);
        }

        return visited.size();
    }

    /* ==================== DIM RESOLVE ==================== */

    private static ServerLevel resolveLevel(ServerLevel ctx, String raw) {
        String s = raw.toLowerCase(Locale.ROOT);

        ResourceKey<Level> key;
        if (s.equals("overworld")) key = Level.OVERWORLD;
        else if (s.equals("nether")) key = Level.NETHER;
        else if (s.equals("end")) key = Level.END;
        else {
            if (!s.contains(":")) s = "cosmicdungeon:" + s;
            ResourceLocation rl = ResourceLocation.tryParse(s);
            if (rl == null) return null;
            key = ResourceKey.create(Registries.DIMENSION, rl);
        }

        return ctx.getServer().getLevel(key);
    }

    private static MutableComponent copyable(String v, String c) {
        return Component.literal(v).withStyle(
                Style.EMPTY.withUnderlined(true)
                        .withClickEvent(new ClickEvent.CopyToClipboard(c))
                        .withHoverEvent(new HoverEvent.ShowText(Component.literal("Click to copy")))
        );
    }
}
