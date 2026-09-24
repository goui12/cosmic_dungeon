package net.goui.cosmicdungeon.npc.tamsin;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;
import net.goui.cosmicdungeon.Config;
import net.goui.cosmicdungeon.auth.AccessPolicy;
import net.goui.cosmicdungeon.block.entity.ClassSelectorBlockEntity;
import net.goui.cosmicdungeon.dungeon.DungeonDefinitions;
import net.goui.cosmicdungeon.dungeon.DungeonInstanceSlots;
import net.goui.cosmicdungeon.npc.NpcIdentityData;
import net.goui.cosmicdungeon.npc.NpcIdentityService;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.phys.Vec3;

/** Developer-triggered, bounded placement using the existing durable Tamsin identity. */
public final class TamsinPlacement {
    public static final TamsinPlacement INSTANCE = new TamsinPlacement();

    private TamsinPlacement() {}

    public MutableComponent button(ServerPlayer player, BlockPos selector) {
        String command = "/d1 tamsin place " + player.level().dimension().location() + " "
                + selector.getX() + " " + selector.getY() + " " + selector.getZ();
        return Component.literal("[Tamsin]").withStyle(style -> style
                .withColor(ChatFormatting.LIGHT_PURPLE).withUnderlined(true)
                .withClickEvent(new ClickEvent.RunCommand(command))
                .withHoverEvent(new HoverEvent.ShowText(Component.literal(
                        "Place Tamsin beside this selector. Replaces Tamsin at another selector."))));
    }

    public LiteralArgumentBuilder<CommandSourceStack> command() {
        return Commands.literal("place")
                .requires(source -> source.getPlayer() != null && AccessPolicy.isDeveloper(source.getPlayer()))
                .then(Commands.argument("dimension", ResourceLocationArgument.id())
                        .then(Commands.argument("selector", BlockPosArgument.blockPos())
                                .executes(context -> place(context.getSource(),
                                        ResourceLocationArgument.getId(context, "dimension"),
                                        BlockPosArgument.getBlockPos(context, "selector")))));
    }

    private int place(CommandSourceStack source, ResourceLocation dimension, BlockPos selector) {
        ServerPlayer player = source.getPlayer();
        if (player == null || !AccessPolicy.isDeveloper(player) || !player.isAlive() || player.isSpectator())
            return fail(source, "Only a nearby Developer can place Tamsin.");
        ServerLevel level = player.level();
        double range = Config.SELECTOR_RANGE.get();
        if (source.getLevel() != level || !dimension.equals(level.dimension().location())
                || player.distanceToSqr(Vec3.atCenterOf(selector)) > range * range
                || !level.hasChunkAt(selector)
                || !(level.getBlockEntity(selector) instanceof ClassSelectorBlockEntity))
            return fail(source, "Right-click a nearby class selector again before placing Tamsin.");
        var template = DungeonInstanceSlots.templateDimensionForPhysical(source.getServer(), level.dimension());
        if (DungeonDefinitions.DUNGEON_1.containsDimension(template))
            return fail(source, "Place Tamsin beside a selector in the starting area, outside Dungeon 1.");

        var identities = NpcIdentityData.get(source.getServer());
        var data = TamsinData.get(source.getServer());
        var binding = new TamsinData.Binding(dimension.toString(), selector.asLong());
        String owner = identities.owner(NpcIdentityService.TAMSIN);
        if (owner != null && !owner.isEmpty()) {
            UUID id = UUID.fromString(owner);
            var existing = level.getEntity(id);
            if (binding.equals(data.binding(id)) && existing != null && existing.isAlive()
                    && existing.distanceToSqr(Vec3.atCenterOf(selector)) <= range * range) {
                source.sendSuccess(() -> Component.literal("Tamsin is already beside this selector at "
                        + existing.blockPosition().toShortString() + "."), false);
                return 1;
            }
        }

        Villager npc = EntityType.VILLAGER.create(level, EntitySpawnReason.COMMAND);
        if (npc == null) return fail(source, "Could not create Tamsin; the current Tamsin is unchanged.");
        npc.setCustomName(Component.literal("Tamsin Vane"));
        npc.setCustomNameVisible(true);
        npc.setNoAi(true);
        npc.setInvulnerable(true);
        npc.setSilent(true);
        npc.setPersistenceRequired();
        var spot = findSpot(selector, player.position(), pos -> clear(level, npc, pos)
                && npc.distanceToSqr(Vec3.atCenterOf(selector)) <= range * range);
        if (spot.isEmpty()) {
            npc.discard();
            return fail(source, "Clear a two-block-high space on solid ground beside the selector, then click [Tamsin] again.");
        }
        BlockPos pos = spot.get();
        float yaw = (float) (Math.toDegrees(Math.atan2(player.getZ() - (pos.getZ() + 0.5),
                player.getX() - (pos.getX() + 0.5))) - 90);
        npc.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, yaw, 0);
        npc.setYHeadRot(yaw);
        npc.setYBodyRot(yaw);
        // The binding must exist during EntityJoinLevelEvent so the identity service can validate it.
        // Its insertion transaction preserves the old owner if another mod refuses the new NPC.
        boolean added = bindAndSpawn(data, npc.getUUID(), binding, () -> NpcIdentityService.spawn(npc));
        if (!added) return fail(source, "Tamsin could not be placed; the previous Tamsin is unchanged.");
        source.sendSuccess(() -> Component.literal("Tamsin placed at " + pos.toShortString()
                + " and linked to this selector; replaces the previous Tamsin.")
                .withStyle(ChatFormatting.GREEN), true);
        return 1;
    }

    /** Only 36 nearby candidates; predicates must not load absent chunks. */
    Optional<BlockPos> findSpot(BlockPos selector, Vec3 player, Predicate<BlockPos> clear) {
        var candidates = new ArrayList<BlockPos>(36);
        for (int y : new int[] {0, 1, -1}) {
            for (int x = -2; x <= 2; x++) {
                for (int z = -2; z <= 2; z++) {
                    int radius = Math.max(Math.abs(x), Math.abs(z));
                    if (radius == 1 || (radius == 2 && (x == 0 || z == 0)))
                        candidates.add(selector.offset(x, y, z));
                }
            }
        }
        candidates.sort(Comparator.comparingInt((BlockPos pos) -> Math.abs(pos.getY() - selector.getY()))
                .thenComparingDouble(pos -> player.distanceToSqr(Vec3.atBottomCenterOf(pos))));
        return candidates.stream().filter(clear).findFirst();
    }

    private boolean clear(ServerLevel level, Villager npc, BlockPos pos) {
        if (pos.getY() <= level.getMinY() || pos.getY() + 2 > level.getMaxY()
                || !level.hasChunksAt(pos.offset(-1, -1, -1), pos.offset(1, 2, 1))
                || !level.getWorldBorder().isWithinBounds(pos)
                || !level.getFluidState(pos).isEmpty() || !level.getFluidState(pos.above()).isEmpty()
                || !level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP))
            return false;
        npc.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        return level.noCollision(npc, npc.getBoundingBox());
    }

    boolean bindAndSpawn(TamsinData data, UUID npc, TamsinData.Binding binding, BooleanSupplier spawn) {
        var previous = data.binding(npc);
        boolean added = false;
        data.bind(npc, binding);
        try {
            added = spawn.getAsBoolean();
            return added;
        } finally {
            if (!added) {
                if (previous == null) data.unbind(npc); else data.bind(npc, previous);
            }
        }
    }

    private int fail(CommandSourceStack source, String message) {
        source.sendFailure(Component.literal(message));
        return 0;
    }
}
