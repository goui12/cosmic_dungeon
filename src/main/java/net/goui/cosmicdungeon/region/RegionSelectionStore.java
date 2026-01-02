package net.goui.cosmicdungeon.region;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class RegionSelectionStore {
    private RegionSelectionStore() {}

    private static final Map<UUID, Selection> SELECTIONS = new ConcurrentHashMap<>();

    public static Optional<Selection> get(Player player) {
        return Optional.ofNullable(SELECTIONS.get(player.getUUID()));
    }

    public static void clear(Player player) {
        SELECTIONS.remove(player.getUUID());
    }

    public static Result setPos1(Player player, ResourceKey<Level> dimension, BlockPos pos) {
        final String dimId = dimension.location().toString();

        Selection sel = SELECTIONS.computeIfAbsent(player.getUUID(), id -> new Selection(dimId, null, null));

        // If pos2 exists and dimension differs, refuse.
        if (sel.pos2 != null && sel.dimensionId != null && !sel.dimensionId.equals(dimId)) {
            return Result.fail("Selection is tied to dimension " + sel.dimensionId + ". Cannot set Pos1 in " + dimId + ".");
        }

        sel.dimensionId = dimId;
        sel.pos1 = pos.immutable();

        return Result.ok(dimId);
    }

    public static Result setPos2(Player player, ResourceKey<Level> dimension, BlockPos pos) {
        final String dimId = dimension.location().toString();

        Selection sel = SELECTIONS.computeIfAbsent(player.getUUID(), id -> new Selection(dimId, null, null));

        // If pos1 exists and dimension differs, refuse.
        if (sel.pos1 != null && sel.dimensionId != null && !sel.dimensionId.equals(dimId)) {
            return Result.fail("Selection is tied to dimension " + sel.dimensionId + ". Cannot set Pos2 in " + dimId + ".");
        }

        sel.dimensionId = dimId;
        sel.pos2 = pos.immutable();

        return Result.ok(dimId);
    }

    public record Result(boolean ok, String dimensionId, String message) {
        public static Result ok(String dimId) {
            return new Result(true, dimId, "");
        }

        public static Result fail(String msg) {
            return new Result(false, "", msg);
        }
    }

    public static final class Selection {
        private String dimensionId;
        private BlockPos pos1;
        private BlockPos pos2;

        private Selection(String dimensionId, BlockPos pos1, BlockPos pos2) {
            this.dimensionId = dimensionId;
            this.pos1 = pos1;
            this.pos2 = pos2;
        }

        public String dimensionId() {
            return dimensionId == null ? "" : dimensionId;
        }

        public Optional<BlockPos> pos1() {
            return Optional.ofNullable(pos1);
        }

        public Optional<BlockPos> pos2() {
            return Optional.ofNullable(pos2);
        }
    }
}
