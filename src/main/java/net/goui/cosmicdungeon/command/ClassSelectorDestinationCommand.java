// file: src/main/java/net/goui/cosmicdungeon/command/ClassSelectorDestinationCommand.java
package net.goui.cosmicdungeon.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.goui.cosmicdungeon.auth.AccessPolicy;
import net.goui.cosmicdungeon.block.ModBlocks;
import net.goui.cosmicdungeon.block.entity.ClassSelectorBlockEntity;
import net.goui.cosmicdungeon.rift.RiftRegistryData;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.List;

public final class ClassSelectorDestinationCommand {
    private ClassSelectorDestinationCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("classselector")
                        .requires(AccessPolicy::requireDeveloperOrConsole)

                        // ---- UI helpers ----
                        .then(Commands.literal("ui")
                                .then(Commands.literal("dest")
                                        .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                                .executes(ctx -> uiDest(ctx.getSource(), BlockPosArgument.getBlockPos(ctx, "pos")))))
                                .then(Commands.literal("destslot")
                                        .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                                .then(Commands.argument("slot", IntegerArgumentType.integer(1, 64))
                                                        .executes(ctx -> uiDestSlot(
                                                                ctx.getSource(),
                                                                BlockPosArgument.getBlockPos(ctx, "pos"),
                                                                IntegerArgumentType.getInteger(ctx, "slot")
                                                        )))))
                                .then(Commands.literal("players")
                                        .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                                .executes(ctx -> uiPlayers(ctx.getSource(), BlockPosArgument.getBlockPos(ctx, "pos")))))
                        )

                        // ---- legacy destination config (optional fallback) ----
                        .then(Commands.literal("dest")
                                .then(Commands.literal("set")
                                        .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                                .then(Commands.argument("destination", StringArgumentType.string())
                                                        .executes(ctx -> cmdSetDest(
                                                                ctx.getSource(),
                                                                BlockPosArgument.getBlockPos(ctx, "pos"),
                                                                StringArgumentType.getString(ctx, "destination")
                                                        )))))
                                .then(Commands.literal("clear")
                                        .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                                .executes(ctx -> cmdClearDest(
                                                        ctx.getSource(),
                                                        BlockPosArgument.getBlockPos(ctx, "pos")
                                                ))))
                        )

                        // ---- per-slot destination config (NEW) ----
                        .then(Commands.literal("destslot")
                                .then(Commands.literal("set")
                                        .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                                .then(Commands.argument("slot", IntegerArgumentType.integer(1, 64))
                                                        .then(Commands.argument("destination", StringArgumentType.string())
                                                                .executes(ctx -> cmdSetDestSlot(
                                                                        ctx.getSource(),
                                                                        BlockPosArgument.getBlockPos(ctx, "pos"),
                                                                        IntegerArgumentType.getInteger(ctx, "slot"),
                                                                        StringArgumentType.getString(ctx, "destination")
                                                                ))))))
                                .then(Commands.literal("clear")
                                        .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                                .then(Commands.argument("slot", IntegerArgumentType.integer(1, 64))
                                                        .executes(ctx -> cmdClearDestSlot(
                                                                ctx.getSource(),
                                                                BlockPosArgument.getBlockPos(ctx, "pos"),
                                                                IntegerArgumentType.getInteger(ctx, "slot")
                                                        )))))
                        )

                        // ---- player count config ----
                        .then(Commands.literal("players")
                                .then(Commands.literal("set")
                                        .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                                .then(Commands.argument("count", IntegerArgumentType.integer(1, 64))
                                                        .executes(ctx -> cmdSetPlayers(
                                                                ctx.getSource(),
                                                                BlockPosArgument.getBlockPos(ctx, "pos"),
                                                                IntegerArgumentType.getInteger(ctx, "count")
                                                        )))))
                        )
        );
    }

    /* ============================ UI: slot overview ============================ */

    private static int uiDest(CommandSourceStack src, BlockPos pos) {
        ServerPlayer p = src.getPlayer();
        if (p == null) return 0;
        if (!(p.level() instanceof ServerLevel level)) return 0;

        if (!AccessPolicy.isDeveloper(p)) return 0;
        if (p.blockPosition().distManhattan(pos) > 16) {
            src.sendFailure(Component.literal("Too far from class selector to configure.").withStyle(ChatFormatting.RED));
            return 0;
        }

        if (level.getBlockState(pos).getBlock() != ModBlocks.CLASS_SELECTOR_BLOCK.get()) {
            src.sendFailure(Component.literal("Not a class selector block: ")
                    .append(Component.literal(pos.toShortString()).withStyle(ChatFormatting.YELLOW)));
            return 0;
        }

        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof ClassSelectorBlockEntity csbe)) {
            src.sendFailure(Component.literal("Missing ClassSelector block entity at that position.").withStyle(ChatFormatting.RED));
            return 0;
        }

        int max = csbe.getMaxPlayers();

        src.sendSuccess(() -> Component.literal("Configure slot rift destinations for ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(pos.toShortString()).withStyle(ChatFormatting.AQUA)), false);

        src.sendSuccess(() -> Component.literal("Max players: ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(String.valueOf(max)).withStyle(ChatFormatting.YELLOW)), false);

        // Optional fallback display
        String fallback = csbe.getDestinationName();
        src.sendSuccess(() -> Component.literal("Fallback destination (legacy): ").withStyle(ChatFormatting.DARK_GRAY)
                .append(Component.literal(fallback.isBlank() ? "(none)" : fallback).withStyle(ChatFormatting.GRAY)), false);

        String pxyz = pos.getX() + " " + pos.getY() + " " + pos.getZ();

        for (int slot = 1; slot <= max; slot++) {
            String current = csbe.getSlotDestination(slot);
            boolean has = current != null && !current.isBlank();

            String cfgCmd = "/classselector ui destslot " + pxyz + " " + slot;
            String clrCmd = "/classselector destslot clear " + pxyz + " " + slot;

            MutableComponent line = Component.literal(" - Slot ").withStyle(ChatFormatting.DARK_GRAY)
                    .append(Component.literal(String.valueOf(slot)).withStyle(ChatFormatting.AQUA))
                    .append(Component.literal(": ").withStyle(ChatFormatting.DARK_GRAY))
                    // show configured destination name (or "(none)")
                    .append(Component.literal(has ? current : "(none)").withStyle(has ? ChatFormatting.YELLOW : ChatFormatting.GRAY))
                    .append(Component.literal(" ").withStyle(ChatFormatting.DARK_GRAY))
                    // [CONFIGURE]
                    .append(Component.literal("[CONFIGURE]").withStyle(
                            Style.EMPTY.withColor(ChatFormatting.AQUA)
                                    .withUnderlined(true)
                                    .withClickEvent(new ClickEvent.RunCommand(cfgCmd))
                    ));

            // [CLEAR] only if something is set
            if (has) {
                line = line.append(Component.literal(" ").withStyle(ChatFormatting.DARK_GRAY))
                        .append(Component.literal("[CLEAR]").withStyle(
                                Style.EMPTY.withColor(ChatFormatting.RED)
                                        .withUnderlined(true)
                                        .withClickEvent(new ClickEvent.RunCommand(clrCmd))
                        ));
            }

            p.sendSystemMessage(line);
        }

        return 1;
    }

    /* ============================ UI: pick destination for one slot ============================ */

    private static int uiDestSlot(CommandSourceStack src, BlockPos pos, int slot) {
        ServerPlayer p = src.getPlayer();
        if (p == null) return 0;
        if (!(p.level() instanceof ServerLevel level)) return 0;

        if (!AccessPolicy.isDeveloper(p)) return 0;
        if (p.blockPosition().distManhattan(pos) > 16) {
            src.sendFailure(Component.literal("Too far from class selector to configure.").withStyle(ChatFormatting.RED));
            return 0;
        }

        if (level.getBlockState(pos).getBlock() != ModBlocks.CLASS_SELECTOR_BLOCK.get()) {
            src.sendFailure(Component.literal("Not a class selector block: ")
                    .append(Component.literal(pos.toShortString()).withStyle(ChatFormatting.YELLOW)));
            return 0;
        }

        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof ClassSelectorBlockEntity csbe)) {
            src.sendFailure(Component.literal("Missing ClassSelector block entity at that position.").withStyle(ChatFormatting.RED));
            return 0;
        }

        int max = csbe.getMaxPlayers();
        if (slot < 1 || slot > max) {
            src.sendFailure(Component.literal("Slot out of range for this selector (1.." + max + ").").withStyle(ChatFormatting.RED));
            return 0;
        }

        RiftRegistryData data = RiftRegistryData.get(level.getServer());
        List<String> dests = data.listDestinationNamesSorted();

        String current = csbe.getSlotDestination(slot);

        String pxyz = pos.getX() + " " + pos.getY() + " " + pos.getZ();

        src.sendSuccess(() -> Component.literal("Slot ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(String.valueOf(slot)).withStyle(ChatFormatting.AQUA))
                .append(Component.literal(" destination for ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(pos.toShortString()).withStyle(ChatFormatting.AQUA)), false);

        src.sendSuccess(() -> Component.literal("Current: ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(current.isBlank() ? "(none)" : current).withStyle(ChatFormatting.YELLOW)), false);

        // Clear always available here
        src.sendSuccess(() -> Component.literal("[clear slot " + slot + "]")
                .withStyle(Style.EMPTY.withColor(ChatFormatting.RED).withUnderlined(true)
                        .withClickEvent(new ClickEvent.RunCommand("/classselector destslot clear " + pxyz + " " + slot))), false);

        if (dests.isEmpty()) {
            src.sendSuccess(() -> Component.literal("(no rift destinations exist — use /rd new <name>)").withStyle(ChatFormatting.DARK_GRAY), false);
            return 1;
        }

        for (String name : dests) {
            if (name == null || name.isBlank()) continue;

            String safe = name.replace("\"", "");
            String cmd = "/classselector destslot set " + pxyz + " " + slot + " \"" + safe + "\"";

            MutableComponent line = Component.literal(" - ").withStyle(ChatFormatting.DARK_GRAY)
                    .append(Component.literal(name).withStyle(
                            Style.EMPTY.withColor(ChatFormatting.AQUA)
                                    .withUnderlined(true)
                                    .withClickEvent(new ClickEvent.RunCommand(cmd))
                    ));

            p.sendSystemMessage(line);
        }

        return 1;
    }

    /* ============================ UI: player count list ============================ */

    private static int uiPlayers(CommandSourceStack src, BlockPos pos) {
        ServerPlayer p = src.getPlayer();
        if (p == null) return 0;
        if (!(p.level() instanceof ServerLevel level)) return 0;

        if (!AccessPolicy.isDeveloper(p)) return 0;
        if (p.blockPosition().distManhattan(pos) > 16) {
            src.sendFailure(Component.literal("Too far from class selector to configure.").withStyle(ChatFormatting.RED));
            return 0;
        }

        if (level.getBlockState(pos).getBlock() != ModBlocks.CLASS_SELECTOR_BLOCK.get()) {
            src.sendFailure(Component.literal("Not a class selector block: ")
                    .append(Component.literal(pos.toShortString()).withStyle(ChatFormatting.YELLOW)));
            return 0;
        }

        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof ClassSelectorBlockEntity csbe)) {
            src.sendFailure(Component.literal("Missing ClassSelector block entity at that position.").withStyle(ChatFormatting.RED));
            return 0;
        }

        int current = csbe.getMaxPlayers();

        src.sendSuccess(() -> Component.literal("Configure dungeon player count for ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(pos.toShortString()).withStyle(ChatFormatting.AQUA)), false);

        src.sendSuccess(() -> Component.literal("Current max players: ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(String.valueOf(current)).withStyle(ChatFormatting.YELLOW)), false);

        for (int n = 1; n <= 8; n++) {
            String cmd = "/classselector players set " + pos.getX() + " " + pos.getY() + " " + pos.getZ() + " " + n;

            MutableComponent line = Component.literal(" - ").withStyle(ChatFormatting.DARK_GRAY)
                    .append(Component.literal(String.valueOf(n)).withStyle(
                            Style.EMPTY.withColor(n == current ? ChatFormatting.GREEN : ChatFormatting.AQUA)
                                    .withUnderlined(true)
                                    .withClickEvent(new ClickEvent.RunCommand(cmd))
                    ));

            p.sendSystemMessage(line);
        }

        return 1;
    }

    /* ============================ legacy destination config ============================ */

    private static int cmdSetDest(CommandSourceStack src, BlockPos pos, String destinationRaw) {
        ServerPlayer p = src.getPlayer();
        if (p == null) return 0;
        if (!(p.level() instanceof ServerLevel level)) return 0;

        if (!AccessPolicy.isDeveloper(p)) return 0;
        if (p.blockPosition().distManhattan(pos) > 16) {
            src.sendFailure(Component.literal("Too far from class selector to configure.").withStyle(ChatFormatting.RED));
            return 0;
        }

        if (level.getBlockState(pos).getBlock() != ModBlocks.CLASS_SELECTOR_BLOCK.get()) {
            src.sendFailure(Component.literal("Not a class selector block: ")
                    .append(Component.literal(pos.toShortString()).withStyle(ChatFormatting.YELLOW)));
            return 0;
        }

        String dest = cleanDestinationArg(destinationRaw);
        if (dest.isBlank()) {
            src.sendFailure(Component.literal("Destination cannot be empty.").withStyle(ChatFormatting.RED));
            return 0;
        }

        RiftRegistryData data = RiftRegistryData.get(level.getServer());
        if (!data.destinationExists(dest)) {
            src.sendFailure(Component.literal("Unknown destination: ")
                    .append(Component.literal(dest).withStyle(ChatFormatting.YELLOW)));
            return 0;
        }

        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof ClassSelectorBlockEntity csbe)) {
            src.sendFailure(Component.literal("Missing ClassSelector block entity at that position.").withStyle(ChatFormatting.RED));
            return 0;
        }

        csbe.setDestinationName(dest);

        src.sendSuccess(() -> Component.literal("Set fallback destination to ").withStyle(ChatFormatting.GREEN)
                        .append(Component.literal(dest).withStyle(ChatFormatting.AQUA)),
                false);
        return 1;
    }

    private static int cmdClearDest(CommandSourceStack src, BlockPos pos) {
        ServerPlayer p = src.getPlayer();
        if (p == null) return 0;
        if (!(p.level() instanceof ServerLevel level)) return 0;

        if (!AccessPolicy.isDeveloper(p)) return 0;

        if (p.blockPosition().distManhattan(pos) > 16) {
            src.sendFailure(Component.literal("Too far from class selector to configure.").withStyle(ChatFormatting.RED));
            return 0;
        }

        if (level.getBlockState(pos).getBlock() != ModBlocks.CLASS_SELECTOR_BLOCK.get()) {
            src.sendFailure(Component.literal("Not a class selector block: ")
                    .append(Component.literal(pos.toShortString()).withStyle(ChatFormatting.YELLOW)));
            return 0;
        }

        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof ClassSelectorBlockEntity csbe)) {
            src.sendFailure(Component.literal("Missing ClassSelector block entity at that position.").withStyle(ChatFormatting.RED));
            return 0;
        }

        csbe.setDestinationName("");

        src.sendSuccess(() -> Component.literal("Cleared fallback destination.").withStyle(ChatFormatting.GREEN), false);
        return 1;
    }

    /* ============================ per-slot destination config ============================ */

    private static int cmdSetDestSlot(CommandSourceStack src, BlockPos pos, int slot, String destinationRaw) {
        ServerPlayer p = src.getPlayer();
        if (p == null) return 0;
        if (!(p.level() instanceof ServerLevel level)) return 0;

        if (!AccessPolicy.isDeveloper(p)) return 0;

        if (p.blockPosition().distManhattan(pos) > 16) {
            src.sendFailure(Component.literal("Too far from class selector to configure.").withStyle(ChatFormatting.RED));
            return 0;
        }

        if (level.getBlockState(pos).getBlock() != ModBlocks.CLASS_SELECTOR_BLOCK.get()) {
            src.sendFailure(Component.literal("Not a class selector block: ")
                    .append(Component.literal(pos.toShortString()).withStyle(ChatFormatting.YELLOW)));
            return 0;
        }

        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof ClassSelectorBlockEntity csbe)) {
            src.sendFailure(Component.literal("Missing ClassSelector block entity at that position.").withStyle(ChatFormatting.RED));
            return 0;
        }

        int max = csbe.getMaxPlayers();
        if (slot < 1 || slot > max) {
            src.sendFailure(Component.literal("Slot out of range for this selector (1.." + max + ").").withStyle(ChatFormatting.RED));
            return 0;
        }

        String dest = cleanDestinationArg(destinationRaw);
        if (dest.isBlank()) {
            src.sendFailure(Component.literal("Destination cannot be empty.").withStyle(ChatFormatting.RED));
            return 0;
        }

        RiftRegistryData data = RiftRegistryData.get(level.getServer());
        if (!data.destinationExists(dest)) {
            src.sendFailure(Component.literal("Unknown destination: ")
                    .append(Component.literal(dest).withStyle(ChatFormatting.YELLOW)));
            return 0;
        }

        csbe.setSlotDestination(slot, dest);

        src.sendSuccess(() -> Component.literal("Set slot ").withStyle(ChatFormatting.GREEN)
                .append(Component.literal(String.valueOf(slot)).withStyle(ChatFormatting.AQUA))
                .append(Component.literal(" destination to ").withStyle(ChatFormatting.GREEN))
                .append(Component.literal(dest).withStyle(ChatFormatting.AQUA)), false);

        return 1;
    }

    private static int cmdClearDestSlot(CommandSourceStack src, BlockPos pos, int slot) {
        ServerPlayer p = src.getPlayer();
        if (p == null) return 0;
        if (!(p.level() instanceof ServerLevel level)) return 0;

        if (!AccessPolicy.isDeveloper(p)) return 0;

        if (p.blockPosition().distManhattan(pos) > 16) {
            src.sendFailure(Component.literal("Too far from class selector to configure.").withStyle(ChatFormatting.RED));
            return 0;
        }

        if (level.getBlockState(pos).getBlock() != ModBlocks.CLASS_SELECTOR_BLOCK.get()) {
            src.sendFailure(Component.literal("Not a class selector block: ")
                    .append(Component.literal(pos.toShortString()).withStyle(ChatFormatting.YELLOW)));
            return 0;
        }

        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof ClassSelectorBlockEntity csbe)) {
            src.sendFailure(Component.literal("Missing ClassSelector block entity at that position.").withStyle(ChatFormatting.RED));
            return 0;
        }

        int max = csbe.getMaxPlayers();
        if (slot < 1 || slot > max) {
            src.sendFailure(Component.literal("Slot out of range for this selector (1.." + max + ").").withStyle(ChatFormatting.RED));
            return 0;
        }

        csbe.clearSlotDestination(slot);

        src.sendSuccess(() -> Component.literal("Cleared slot ").withStyle(ChatFormatting.GREEN)
                .append(Component.literal(String.valueOf(slot)).withStyle(ChatFormatting.AQUA))
                .append(Component.literal(" destination.").withStyle(ChatFormatting.GREEN)), false);

        return 1;
    }

    /* ============================ players config ============================ */

    private static int cmdSetPlayers(CommandSourceStack src, BlockPos pos, int count) {
        ServerPlayer p = src.getPlayer();
        if (p == null) return 0;
        if (!(p.level() instanceof ServerLevel level)) return 0;

        if (!AccessPolicy.isDeveloper(p)) return 0;

        if (p.blockPosition().distManhattan(pos) > 16) {
            src.sendFailure(Component.literal("Too far from class selector to configure.").withStyle(ChatFormatting.RED));
            return 0;
        }

        if (level.getBlockState(pos).getBlock() != ModBlocks.CLASS_SELECTOR_BLOCK.get()) {
            src.sendFailure(Component.literal("Not a class selector block: ")
                    .append(Component.literal(pos.toShortString()).withStyle(ChatFormatting.YELLOW)));
            return 0;
        }

        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof ClassSelectorBlockEntity csbe)) {
            src.sendFailure(Component.literal("Missing ClassSelector block entity at that position.").withStyle(ChatFormatting.RED));
            return 0;
        }

        csbe.setMaxPlayers(count);

        src.sendSuccess(() -> Component.literal("Set max players to ").withStyle(ChatFormatting.GREEN)
                .append(Component.literal(String.valueOf(csbe.getMaxPlayers())).withStyle(ChatFormatting.AQUA)), false);
        return 1;
    }

    private static String cleanDestinationArg(String raw) {
        if (raw == null) return "";
        String s = raw.trim();
        if (s.length() >= 2 && s.startsWith("\"") && s.endsWith("\"")) {
            s = s.substring(1, s.length() - 1).trim();
        }
        return s;
    }
}
