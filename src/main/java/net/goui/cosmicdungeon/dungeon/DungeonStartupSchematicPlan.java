package net.goui.cosmicdungeon.dungeon;

import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Pure definitions and plan construction for the fixed Dungeon 1 startup schematic batch. */
public final class DungeonStartupSchematicPlan {
    public static final int LOGICAL_SLOT_COUNT = 6;
    public static final int EXPECTED_OPERATION_COUNT = 36;
    public static final String BLANK_SLOT_CLASS = "blankslot";
    private static final String NONE_CLASS = "none";

    private static final List<String> REQUIRED_GROUP_ORDER = List.of(
            "d1_start", "d1_b1_chests", "d1_b2_chests",
            "d1_b3_chests", "d1_b4_chests", "d1_b5_chests"
    );
    private static final Set<Integer> SUPPORTED_ROTATIONS = Set.of(0, 90, 180, 270);
    private static final List<PasteGroup> GROUPS = List.of(
            group("d1_start", "d1_",
                    placement(1, 694, -59, 65, 0), placement(2, 701, -59, 65, 0),
                    placement(3, 708, -59, 65, 0), placement(4, 703, -59, 71, 180),
                    placement(5, 696, -59, 71, 180), placement(6, 689, -59, 71, 180)),
            group("d1_b1_chests", "d1_b1_",
                    placement(1, 638, -60, 59, 180), placement(2, 638, -59, 59, 180),
                    placement(3, 638, -58, 59, 180), placement(4, 639, -60, 59, 180),
                    placement(5, 639, -59, 59, 180), placement(6, 639, -58, 59, 180)),
            group("d1_b2_chests", "d1_b2_",
                    placement(1, 625, -20, 103, 90), placement(2, 625, -19, 103, 90),
                    placement(3, 625, -18, 103, 90), placement(4, 625, -20, 102, 90),
                    placement(5, 625, -19, 102, 90), placement(6, 625, -18, 102, 90)),
            group("d1_b3_chests", "d1_b3_",
                    placement(1, 627, -1, 116, 180), placement(2, 627, 0, 116, 180),
                    placement(3, 627, 1, 116, 180), placement(4, 628, -1, 116, 180),
                    placement(5, 628, 0, 116, 180), placement(6, 628, 1, 116, 180)),
            group("d1_b4_chests", "d1_b4_",
                    placement(1, 621, 22, 70, 0), placement(2, 613, 22, 70, 0),
                    placement(3, 606, 22, 70, 0), placement(4, 608, 22, 69, 180),
                    placement(5, 615, 22, 69, 180), placement(6, 622, 22, 69, 180)),
            group("d1_b5_chests", "d1_b5_",
                    placement(1, 1645, 72, 4210, 270), placement(2, 1645, 73, 4210, 270),
                    placement(3, 1645, 74, 4210, 270), placement(4, 1645, 72, 4211, 270),
                    placement(5, 1645, 73, 4211, 270), placement(6, 1645, 74, 4211, 270))
    );

    static {
        validateDefinitions(GROUPS);
    }

    private DungeonStartupSchematicPlan() {}

    public record PasteGroup(String id, String schematicPrefix, boolean pasteAir,
                             List<SlotPlacement> placements) {
        public PasteGroup {
            placements = placements == null ? null : List.copyOf(placements);
        }
    }

    public record SlotPlacement(int logicalSlot, BlockPos destination, int rotationDegrees) {}

    public record PasteRequest(String groupId, int logicalSlot, String classId,
                               String schematicFilename, BlockPos destination,
                               int rotationDegrees, boolean pasteAir) {}

    public record StartupPastePlan(List<String> normalizedClassSlots, List<PasteRequest> requests) {
        public StartupPastePlan {
            normalizedClassSlots = List.copyOf(normalizedClassSlots);
            requests = List.copyOf(requests);
        }
    }


    public static List<PasteGroup> groups() {
        return GROUPS;
    }

    /** Pure plan construction: no filesystem, player, level, or WorldEdit access. */
    public static StartupPastePlan buildPlan(List<String> orderedOccupiedClasses) {
        if (orderedOccupiedClasses == null) {
            throw new IllegalArgumentException("Ordered occupied classes must not be null.");
        }
        if (orderedOccupiedClasses.isEmpty() || orderedOccupiedClasses.size() > LOGICAL_SLOT_COUNT) {
            throw new IllegalArgumentException("Dungeon startup requires between 1 and 6 occupied class entries.");
        }

        List<String> normalized = new ArrayList<>(LOGICAL_SLOT_COUNT);
        for (String classId : orderedOccupiedClasses) {
            if (NONE_CLASS.equals(classId)) {
                throw new IllegalArgumentException("Dungeon startup cannot use the none class.");
            }
            normalized.add(classId == null || classId.isBlank() ? BLANK_SLOT_CLASS : classId);
        }
        while (normalized.size() < LOGICAL_SLOT_COUNT) {
            normalized.add(BLANK_SLOT_CLASS);
        }

        List<PasteRequest> requests = new ArrayList<>(EXPECTED_OPERATION_COUNT);
        for (PasteGroup group : GROUPS) {
            for (SlotPlacement placement : group.placements()) {
                String classId = normalized.get(placement.logicalSlot() - 1);
                requests.add(new PasteRequest(group.id(), placement.logicalSlot(), classId,
                        group.schematicPrefix() + classId + ".schem", placement.destination(),
                        placement.rotationDegrees(), group.pasteAir()));
            }
        }

        StartupPastePlan plan = new StartupPastePlan(normalized, requests);
        validatePlan(plan);
        return plan;
    }

    private static void validateDefinitions(List<PasteGroup> groups) {
        if (groups == null || groups.size() != REQUIRED_GROUP_ORDER.size()) {
            throw new IllegalStateException("Dungeon startup must define exactly six paste groups.");
        }
        for (int groupIndex = 0; groupIndex < groups.size(); groupIndex++) {
            PasteGroup group = groups.get(groupIndex);
            if (group == null || !REQUIRED_GROUP_ORDER.get(groupIndex).equals(group.id())
                    || group.schematicPrefix() == null || group.schematicPrefix().isBlank() || !group.pasteAir()
                    || group.placements() == null || group.placements().size() != LOGICAL_SLOT_COUNT) {
                throw new IllegalStateException("Invalid dungeon startup paste group at index " + groupIndex + ".");
            }
            for (int slotIndex = 0; slotIndex < LOGICAL_SLOT_COUNT; slotIndex++) {
                SlotPlacement placement = group.placements().get(slotIndex);
                if (placement == null || placement.logicalSlot() != slotIndex + 1 || placement.destination() == null
                        || !SUPPORTED_ROTATIONS.contains(placement.rotationDegrees())) {
                    throw new IllegalStateException("Invalid placement in group " + group.id() + ".");
                }
            }
        }
    }

    private static void validatePlan(StartupPastePlan plan) {
        if (plan == null || plan.normalizedClassSlots().size() != LOGICAL_SLOT_COUNT
                || plan.requests().size() != EXPECTED_OPERATION_COUNT) {
            throw new IllegalArgumentException("Dungeon startup plan must contain six class slots and 36 requests.");
        }
        for (String classId : plan.normalizedClassSlots()) {
            if (classId == null || classId.isBlank()) {
                throw new IllegalArgumentException("Normalized class slots must not be blank.");
            }
            if (NONE_CLASS.equals(classId)) {
                throw new IllegalArgumentException("Normalized class slots must not use the none class.");
            }
        }
        Set<String> logicalRequests = new HashSet<>();
        for (PasteRequest request : plan.requests()) {
            if (request == null || request.destination() == null || request.schematicFilename() == null
                    || request.schematicFilename().isBlank() || !request.pasteAir()
                    || !SUPPORTED_ROTATIONS.contains(request.rotationDegrees())
                    || request.logicalSlot() < 1 || request.logicalSlot() > LOGICAL_SLOT_COUNT
                    || request.classId() == null || request.classId().isBlank()
                    || NONE_CLASS.equals(request.classId()) || request.schematicFilename().endsWith("_none.schem")
                    || !logicalRequests.add(request.groupId() + ":" + request.logicalSlot())) {
                throw new IllegalArgumentException("Invalid or duplicate dungeon startup paste request.");
            }
        }
    }

    private static PasteGroup group(String id, String prefix, SlotPlacement... placements) {
        return new PasteGroup(id, prefix, true, List.of(placements));
    }

    private static SlotPlacement placement(int logicalSlot, int x, int y, int z, int rotation) {
        return new SlotPlacement(logicalSlot, new BlockPos(x, y, z), rotation);
    }
}
