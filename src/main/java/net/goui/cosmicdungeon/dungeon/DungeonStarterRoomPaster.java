package net.goui.cosmicdungeon.dungeon;

import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.transform.AffineTransform;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.session.SessionManager;
import com.sk89q.worldedit.neoforge.NeoForgeAdapter;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.io.File;
import java.io.FileInputStream;

public final class DungeonStarterRoomPaster {

    private DungeonStarterRoomPaster() {}

    private record Slot(int x, int y, int z, boolean rotate) {}

    private static final Slot[] SLOTS = new Slot[] {
            new Slot(694, -59, 65, false),
            new Slot(701, -59, 65, false),
            new Slot(708, -59, 65, false),

            new Slot(703, -59, 71, true),
            new Slot(696, -59, 71, true),
            new Slot(689, -59, 71, true)
    };

    public static void pasteRooms(ServerLevel level, ServerPlayer actorPlayer, String[] slotClasses) {

        if (actorPlayer == null || level == null) {
            System.out.println("[CosmicDungeon] ❌ Invalid paste call (null actor or level).");
            return;
        }

        System.out.println("=== CosmicDungeon API PASTE START ===");
        System.out.println("Actor: " + actorPlayer.getName().getString());
        System.out.println("Dimension: " + level.dimension().location());

        try {
            // 🔥 Actor (WorldEdit context)
            Actor actor = NeoForgeAdapter.adaptPlayer(actorPlayer);

            // 🔥 FORCE world binding (THIS is the missing stability piece)
            var weWorld = NeoForgeAdapter.adapt(level);

            SessionManager manager = WorldEdit.getInstance().getSessionManager();
            LocalSession session = manager.get(actor);

            // 🧊 Freeze player (prevent movement during paste)
            actorPlayer.setDeltaMovement(0, 0, 0);
            actorPlayer.hurtMarked = true;

            for (int i = 0; i < SLOTS.length; i++) {

                Slot slot = SLOTS[i];

                String classId = (slotClasses != null && i < slotClasses.length)
                        ? slotClasses[i]
                        : null;

                if (classId == null || classId.isBlank()) {
                    classId = "blankslot";
                }

                String schemName = "d1_" + classId;

                System.out.println("[CosmicDungeon] Pasting " + schemName + " at slot " + (i + 1));

                File schemFile = new File("config/worldedit/schematics/" + schemName + ".schem");

                if (!schemFile.exists()) {
                    System.out.println("[CosmicDungeon] ❌ Missing schematic: " + schemFile.getAbsolutePath());
                    continue;
                }

                Clipboard clipboard;
                try (ClipboardReader reader =
                             ClipboardFormats.findByFile(schemFile)
                                     .getReader(new FileInputStream(schemFile))) {

                    clipboard = reader.read();
                }

                ClipboardHolder holder = new ClipboardHolder(clipboard);

                if (slot.rotate) {
                    holder.setTransform(new AffineTransform().rotateY(180));
                }

                // Store clipboard in session (optional but good practice)
                session.setClipboard(holder);

                try (EditSession editSession = WorldEdit.getInstance().newEditSession(weWorld)) {

                    Operation operation = holder
                            .createPaste(editSession)
                            .to(BlockVector3.at(slot.x, slot.y, slot.z))
                            .ignoreAirBlocks(false)
                            .build();

                    Operations.complete(operation);
                }
            }

            // 🧊 Keep player still briefly after paste
            actorPlayer.setDeltaMovement(0, 0, 0);

            System.out.println("=== CosmicDungeon API PASTE COMPLETE ===");

        } catch (Exception e) {
            System.out.println("[CosmicDungeon] ❌ Paste failure:");
            e.printStackTrace();
        }
    }
}