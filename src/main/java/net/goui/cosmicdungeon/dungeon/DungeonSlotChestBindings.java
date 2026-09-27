package net.goui.cosmicdungeon.dungeon;

import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.transform.AffineTransform;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import net.goui.cosmicdungeon.block.custom.ClassLockedChestBlock;
import net.goui.cosmicdungeon.block.entity.ClassChestOwnership;
import net.goui.cosmicdungeon.block.entity.ClassLockedChestBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;

/** One startup batch's immutable slot owners; no runtime roster lookup or world scan. */
public final class DungeonSlotChestBindings {
    private final List<ClassChestOwnership> owners;
    private final Map<String, List<BlockVector3>> sourceChests = new HashMap<>();
    private final Map<BlockPos, ClassChestOwnership> assigned = new HashMap<>();

    public DungeonSlotChestBindings(List<ClassChestOwnership> owners) {
        this.owners = List.copyOf(owners);
        if (owners.isEmpty() || owners.size() > DungeonStartupSchematicPlan.LOGICAL_SLOT_COUNT)
            throw new IllegalArgumentException("Expected one to six occupied slot owners");
        var ids = new HashSet<java.util.UUID>();
        for (var owner : this.owners)
            if (owner.playerId() == null || !ids.add(owner.playerId()))
                throw new IllegalArgumentException("Each occupied slot must have a distinct player UUID");
    }

    public ClassChestOwnership ownerFor(int logicalSlot) {
        if (logicalSlot < 1 || logicalSlot > DungeonStartupSchematicPlan.LOGICAL_SLOT_COUNT)
            throw new IllegalArgumentException("Invalid logical slot");
        return logicalSlot <= owners.size() ? owners.get(logicalSlot - 1) : ClassChestOwnership.UNASSIGNED;
    }

    /** Match WorldEdit ExtentBlockCopy: rotate relative to clipboard origin, then add destination. */
    public static BlockPos pastedPosition(BlockVector3 source, BlockVector3 origin, BlockPos destination, int rotation) {
        if (rotation != 0 && rotation != 90 && rotation != 180 && rotation != 270)
            throw new IllegalArgumentException("Unsupported startup rotation");
        BlockVector3 offset = new AffineTransform().rotateY(rotation)
                .apply(source.subtract(origin).toVector3()).toBlockPoint();
        return destination.offset(offset.x(), offset.y(), offset.z());
    }

    public void bindPasted(ServerLevel level, Clipboard clipboard, DungeonStartupSchematicPlan.PasteRequest request) {
        if (!level.getServer().isSameThread() || DungeonInstanceSlots.slotOf(level.dimension()).isEmpty())
            throw new IllegalStateException("Slot ownership requires a prepared physical instance on the server thread");
        List<BlockVector3> positions = sourceChests.computeIfAbsent(request.schematicFilename(),
                ignored -> findClassChests(clipboard));
        ClassChestOwnership owner = ownerFor(request.logicalSlot());
        for (BlockVector3 source : positions) {
            BlockPos target = pastedPosition(source, clipboard.getOrigin(), request.destination(), request.rotationDegrees());
            ClassChestOwnership previous = assigned.putIfAbsent(target, owner);
            if (previous != null && !previous.equals(owner))
                throw new IllegalStateException("Different player slots overlap a class chest at " + target);
            if (!(level.getBlockEntity(target) instanceof ClassLockedChestBlockEntity chest))
                throw new IllegalStateException("Pasted class chest is missing at " + target);
            chest.bindSlotOwner(owner);
        }
    }

    private static List<BlockVector3> findClassChests(Clipboard clipboard) {
        List<BlockVector3> result = new ArrayList<>();
        for (BlockVector3 position : clipboard.getRegion()) {
            String id = clipboard.getBlock(position).getBlockType().getId();
            if (!id.startsWith("cosmicdungeon:") || !id.endsWith("_chest")) continue;
            if (BuiltInRegistries.BLOCK.getValue(ResourceLocation.parse(id)) instanceof ClassLockedChestBlock)
                result.add(BlockVector3.at(position.x(), position.y(), position.z()));
        }
        return List.copyOf(result);
    }
}
