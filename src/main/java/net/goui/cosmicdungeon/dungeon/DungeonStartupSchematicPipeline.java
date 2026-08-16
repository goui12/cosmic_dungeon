package net.goui.cosmicdungeon.dungeon;

import com.mojang.logging.LogUtils;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.transform.AffineTransform;
import com.sk89q.worldedit.neoforge.NeoForgeAdapter;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.world.World;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.slf4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/** Executes a validated Dungeon 1 startup schematic plan against one prepared physical level. */
public final class DungeonStartupSchematicPipeline {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final File SCHEMATIC_DIRECTORY = new File("config/worldedit/schematics");

    private DungeonStartupSchematicPipeline() {}

    public sealed interface PasteBatchResult permits PasteBatchSuccess, PasteBatchFailure {}

    public record PasteBatchSuccess(int completedOperations) implements PasteBatchResult {}

    public record PasteBatchFailure(String groupId, int logicalSlot, String schematicFilename,
                                    BlockPos destination, int rotationDegrees, int completedOperations,
                                    String message, Throwable cause) implements PasteBatchResult {}

    public static PasteBatchResult execute(ServerLevel targetLevel,
                                           DungeonStartupSchematicPlan.StartupPastePlan plan) {
        Objects.requireNonNull(targetLevel, "Prepared physical target level must not be null.");
        Objects.requireNonNull(plan, "Startup paste plan must not be null.");
        if (plan.requests().size() != DungeonStartupSchematicPlan.EXPECTED_OPERATION_COUNT) {
            throw new IllegalArgumentException("Dungeon startup plan must contain 36 requests.");
        }
        if (!targetLevel.getServer().isSameThread()) {
            throw new IllegalStateException("Dungeon startup schematics must execute on the server thread.");
        }

        LOGGER.info("[DungeonStartupSchematics] Starting batch in physical dimension {} with {} operations.",
                targetLevel.dimension().location(), plan.requests().size());
        World worldEditWorld;
        try {
            worldEditWorld = NeoForgeAdapter.adapt(targetLevel);
        } catch (Exception exception) {
            DungeonStartupSchematicPlan.PasteRequest first = plan.requests().getFirst();
            String message = exception.getMessage() == null
                    ? exception.getClass().getSimpleName() : exception.getMessage();
            LOGGER.error("[DungeonStartupSchematics] Could not adapt physical target {} for group={}, slot={}, "
                            + "file={}, destination={}, rotation={}, completed=0/{}.",
                    targetLevel.dimension().location(), first.groupId(), first.logicalSlot(),
                    first.schematicFilename(), first.destination(), first.rotationDegrees(),
                    DungeonStartupSchematicPlan.EXPECTED_OPERATION_COUNT, exception);
            return new PasteBatchFailure(first.groupId(), first.logicalSlot(), first.schematicFilename(),
                    first.destination(), first.rotationDegrees(), 0, message, exception);
        }
        Map<String, Clipboard> clipboardCache = new HashMap<>();
        int completed = 0;

        for (DungeonStartupSchematicPlan.PasteRequest request : plan.requests()) {
            try {
                Clipboard clipboard = clipboardCache.get(request.schematicFilename());
                if (clipboard == null) {
                    clipboard = loadClipboard(request.schematicFilename());
                    clipboardCache.put(request.schematicFilename(), clipboard);
                }

                ClipboardHolder holder = new ClipboardHolder(clipboard);
                if (request.rotationDegrees() != 0) {
                    holder.setTransform(new AffineTransform().rotateY(request.rotationDegrees()));
                }

                try (EditSession editSession = WorldEdit.getInstance().newEditSession(worldEditWorld)) {
                    BlockPos destination = request.destination();
                    Operation operation = holder.createPaste(editSession)
                            .to(BlockVector3.at(destination.getX(), destination.getY(), destination.getZ()))
                            .ignoreAirBlocks(!request.pasteAir())
                            .build();
                    Operations.complete(operation);
                }
                completed++;
            } catch (Exception exception) {
                String message = exception.getMessage() == null
                        ? exception.getClass().getSimpleName() : exception.getMessage();
                LOGGER.error("[DungeonStartupSchematics] Batch failed: group={}, slot={}, file={}, destination={}, "
                                + "rotation={}, completed={}/{}.",
                        request.groupId(), request.logicalSlot(), request.schematicFilename(), request.destination(),
                        request.rotationDegrees(), completed, DungeonStartupSchematicPlan.EXPECTED_OPERATION_COUNT,
                        exception);
                return new PasteBatchFailure(request.groupId(), request.logicalSlot(), request.schematicFilename(),
                        request.destination(), request.rotationDegrees(), completed, message, exception);
            }
        }

        LOGGER.info("[DungeonStartupSchematics] Completed {} operations in physical dimension {}.",
                completed, targetLevel.dimension().location());
        return new PasteBatchSuccess(completed);
    }

    private static Clipboard loadClipboard(String schematicFilename) throws Exception {
        File schematic = new File(SCHEMATIC_DIRECTORY, schematicFilename);
        ClipboardFormat format = ClipboardFormats.findByFile(schematic);
        if (format == null) {
            throw new IllegalArgumentException("Unsupported schematic format for " + schematicFilename);
        }
        try (FileInputStream input = new FileInputStream(schematic);
             ClipboardReader reader = format.getReader(input)) {
            return reader.read();
        }
    }
}
