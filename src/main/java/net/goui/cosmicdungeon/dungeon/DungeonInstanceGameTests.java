package net.goui.cosmicdungeon.dungeon;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapCodec;
import net.goui.cosmicdungeon.CosmicDungeonMod;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestInstance;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Rotation;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.UUID;

public final class DungeonInstanceGameTests {
    private static final ResourceLocation ENVIRONMENT = id("dungeon_instances");
    private static final ResourceLocation EMPTY_STRUCTURE = ResourceLocation.withDefaultNamespace("empty");

    private DungeonInstanceGameTests() {}

    public static void register(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition> environment = event.registerEnvironment(ENVIRONMENT, new TestEnvironmentDefinition.AllOf());
        register(event, environment, "slot_mapping", DungeonInstanceGameTests::slotMapping);
        register(event, environment, "legacy_run_codec", DungeonInstanceGameTests::legacyRunCodec);
        register(event, environment, "farrows_chop_target_codec", DungeonInstanceGameTests::farrowsChopTargetCodec);
        register(event, environment, "inventory_escrow_codec", DungeonInstanceGameTests::inventoryEscrowCodec);
        register(event, environment, "startup_plan_one_player", DungeonInstanceGameTests::startupPlanOnePlayer);
        register(event, environment, "startup_plan_four_players", DungeonInstanceGameTests::startupPlanFourPlayers);
        register(event, environment, "startup_plan_six_players", DungeonInstanceGameTests::startupPlanSixPlayers);
        register(event, environment, "startup_plan_duplicate_classes", DungeonInstanceGameTests::startupPlanDuplicateClasses);
        register(event, environment, "startup_definition_integrity", DungeonInstanceGameTests::startupDefinitionIntegrity);
        register(event, environment, "startup_plan_party_counts", DungeonInstanceGameTests::startupPlanPartyCounts);
        register(event, environment, "startup_plan_blank_entry", DungeonInstanceGameTests::startupPlanBlankEntry);
    }

    private static void slotMapping(GameTestHelper helper) {
        var mapping = DungeonInstanceSlots.mapping(DungeonDefinitions.DUNGEON_1, 10);
        helper.assertTrue(mapping.get(DungeonDefinitions.DUNGEON_1.primaryDimension()).equals(DungeonInstanceSlots.primary(10)),
                net.minecraft.network.chat.Component.literal("primary template did not map to slot primary"));
        helper.assertTrue(mapping.get(DungeonDefinitions.DUNGEON_1.dimensions().get(1)).equals(DungeonInstanceSlots.nether(10)),
                net.minecraft.network.chat.Component.literal("linked Nether template did not map to the same logical slot"));
        helper.assertTrue(DungeonInstanceSlots.slotOf(DungeonInstanceSlots.nether(10)).orElse(-1) == 10,
                net.minecraft.network.chat.Component.literal("slot id was not recovered from physical Nether dimension"));
        helper.succeed();
    }

    private static void legacyRunCodec(GameTestHelper helper) {
        String json = """
                {"run_id":1,"dungeon_id":"dungeon_1","selector_dimension":"minecraft:overworld",
                "selector_pos":0,"dungeon_dimension_ids":["cosmicdungeon:dungeon_1"],"state":"ACTIVE",
                "ordered_players":[]}
                """;
        DungeonRunRegistryData.RunRecord decoded = DungeonRunRegistryData.RunRecord.CODEC
                .parse(JsonOps.INSTANCE, JsonParser.parseString(json)).result().orElse(null);
        helper.assertTrue(decoded != null, net.minecraft.network.chat.Component.literal("legacy run record did not decode"));
        helper.assertTrue(decoded.instanceSlot() == 0, net.minecraft.network.chat.Component.literal("legacy run did not receive the migration sentinel slot"));
        helper.succeed();
    }

    private static void farrowsChopTargetCodec(GameTestHelper helper) {
        DungeonReturnTarget expected = new DungeonReturnTarget(UUID.fromString("00000000-0000-0000-0000-000000000001"),
                42L, DungeonInstanceSlots.primary(3).location().toString(), 1.25D, 64.0D, -7.5D, 90.0F, 12.0F);
        var encoded = DungeonReturnTarget.CODEC.encodeStart(JsonOps.INSTANCE, expected).result().orElse(null);
        DungeonReturnTarget decoded = encoded == null ? null
                : DungeonReturnTarget.CODEC.parse(JsonOps.INSTANCE, encoded).result().orElse(null);
        helper.assertTrue(expected.equals(decoded), net.minecraft.network.chat.Component.literal("Farrow's Chop return target did not round-trip"));
        helper.succeed();
    }

    private static void inventoryEscrowCodec(GameTestHelper helper) {
        CompoundTag dungeon = new CompoundTag();
        dungeon.putString("marker", "dungeon");
        CompoundTag outside = new CompoundTag();
        outside.putString("marker", "outside");
        DungeonInventoryEscrowData.Entry expected = new DungeonInventoryEscrowData.Entry(42L,
                UUID.fromString("00000000-0000-0000-0000-000000000002"), dungeon, outside, true);
        var encoded = DungeonInventoryEscrowData.Entry.CODEC.encodeStart(NbtOps.INSTANCE, expected).result().orElse(null);
        DungeonInventoryEscrowData.Entry decoded = encoded == null ? null
                : DungeonInventoryEscrowData.Entry.CODEC.parse(NbtOps.INSTANCE, encoded).result().orElse(null);
        helper.assertTrue(decoded != null && decoded.runId() == expected.runId()
                        && decoded.playerId().equals(expected.playerId()) && decoded.outsideActive()
                        && "dungeon".equals(decoded.dungeonInventory().getString("marker").orElse(""))
                        && "outside".equals(decoded.outsideInventory().getString("marker").orElse("")),
                net.minecraft.network.chat.Component.literal("inventory escrow did not round-trip"));
        helper.succeed();
    }

    private static void startupPlanOnePlayer(GameTestHelper helper) {
        var plan = DungeonStartupSchematicPlan.buildPlan(List.of("pyroclast"));
        assertPlanSize(helper, plan);
        Set<String> groups = new HashSet<>();
        Set<String> blankFiles = new HashSet<>();
        int blankRequests = 0;
        for (var request : plan.requests()) {
            groups.add(request.groupId());
            if (request.logicalSlot() == 1) {
                assertTest(helper, "pyroclast".equals(request.classId()), "one-player slot 1 class changed");
            } else {
                assertTest(helper, DungeonStartupSchematicPlan.BLANK_SLOT_CLASS.equals(request.classId()),
                        "unoccupied one-player slot was not blank");
            }
            if (DungeonStartupSchematicPlan.BLANK_SLOT_CLASS.equals(request.classId())) {
                blankRequests++;
                blankFiles.add(request.schematicFilename());
            }
        }
        assertTest(helper, groups.size() == 6, "one-player plan did not contain six groups");
        assertTest(helper, blankRequests == 30, "one-player plan did not contain 30 blank requests");
        assertTest(helper, blankFiles.equals(Set.of(
                "d1_blankslot.schem", "d1_b1_blankslot.schem", "d1_b2_blankslot.schem",
                "d1_b3_blankslot.schem", "d1_b4_blankslot.schem", "d1_b5_blankslot.schem")),
                "blank-slot filenames did not match all six group prefixes");
        helper.succeed();
    }

    private static void startupPlanFourPlayers(GameTestHelper helper) {
        List<String> classes = List.of("theurgist", "dragoon", "theurgist", "bogatyr");
        var plan = DungeonStartupSchematicPlan.buildPlan(classes);
        assertPlanSize(helper, plan);
        int blanks = 0;
        for (var request : plan.requests()) {
            String expected = request.logicalSlot() <= classes.size()
                    ? classes.get(request.logicalSlot() - 1) : DungeonStartupSchematicPlan.BLANK_SLOT_CLASS;
            assertTest(helper, expected.equals(request.classId()), "four-player class order changed");
            if (DungeonStartupSchematicPlan.BLANK_SLOT_CLASS.equals(request.classId())) blanks++;
        }
        assertTest(helper, blanks == 12, "four-player plan did not contain 12 blank requests");
        helper.succeed();
    }

    private static void startupPlanSixPlayers(GameTestHelper helper) {
        List<String> classes = List.of("pyroclast", "theurgist", "dragoon", "bogatyr", "judicator", "venefex");
        var plan = DungeonStartupSchematicPlan.buildPlan(classes);
        assertPlanSize(helper, plan);
        for (var request : plan.requests()) {
            assertTest(helper, classes.get(request.logicalSlot() - 1).equals(request.classId()),
                    "six-player class order changed");
            assertTest(helper, !DungeonStartupSchematicPlan.BLANK_SLOT_CLASS.equals(request.classId()),
                    "six-player plan unexpectedly used blankslot");
        }
        helper.succeed();
    }

    private static void startupPlanDuplicateClasses(GameTestHelper helper) {
        var plan = DungeonStartupSchematicPlan.buildPlan(List.of(
                "pyroclast", "pyroclast", "pyroclast", "pyroclast", "pyroclast", "pyroclast"));
        assertPlanSize(helper, plan);
        for (int slot = 1; slot <= 6; slot++) {
            int logicalSlot = slot;
            long count = plan.requests().stream().filter(request -> request.logicalSlot() == logicalSlot).count();
            assertTest(helper, count == 6, "duplicate class removed a logical slot request");
        }
        helper.succeed();
    }

    private static void startupDefinitionIntegrity(GameTestHelper helper) {
        List<String> expectedGroups = List.of(
                "d1_start", "d1_b1_chests", "d1_b2_chests",
                "d1_b3_chests", "d1_b4_chests", "d1_b5_chests");
        int[][][] expected = {
                {{694, -59, 65, 0}, {701, -59, 65, 0}, {708, -59, 65, 0},
                        {703, -59, 71, 180}, {696, -59, 71, 180}, {689, -59, 71, 180}},
                {{638, -60, 59, 180}, {638, -59, 59, 180}, {638, -58, 59, 180},
                        {639, -60, 59, 180}, {639, -59, 59, 180}, {639, -58, 59, 180}},
                {{625, -20, 103, 90}, {625, -19, 103, 90}, {625, -18, 103, 90},
                        {625, -20, 102, 90}, {625, -19, 102, 90}, {625, -18, 102, 90}},
                {{627, -1, 116, 180}, {627, 0, 116, 180}, {627, 1, 116, 180},
                        {628, -1, 116, 180}, {628, 0, 116, 180}, {628, 1, 116, 180}},
                {{621, 22, 70, 0}, {613, 22, 70, 0}, {606, 22, 70, 0},
                        {608, 22, 69, 180}, {615, 22, 69, 180}, {622, 22, 69, 180}},
                {{1645, 72, 4210, 270}, {1645, 73, 4210, 270}, {1645, 74, 4210, 270},
                        {1645, 72, 4211, 270}, {1645, 73, 4211, 270}, {1645, 74, 4211, 270}}
        };
        var groups = DungeonStartupSchematicPlan.groups();
        assertTest(helper, groups.size() == 6, "startup definition did not contain six groups");
        Set<net.minecraft.core.BlockPos> destinations = new HashSet<>();
        for (int groupIndex = 0; groupIndex < groups.size(); groupIndex++) {
            var group = groups.get(groupIndex);
            assertTest(helper, expectedGroups.get(groupIndex).equals(group.id()), "startup group order changed");
            assertTest(helper, group.placements().size() == 6, "startup group did not contain six placements");
            for (int slotIndex = 0; slotIndex < 6; slotIndex++) {
                var placement = group.placements().get(slotIndex);
                int[] row = expected[groupIndex][slotIndex];
                assertTest(helper, placement.logicalSlot() == slotIndex + 1, "logical slots were not 1 through 6");
                assertTest(helper, placement.destination().equals(new net.minecraft.core.BlockPos(row[0], row[1], row[2])),
                        "startup destination changed");
                assertTest(helper, placement.rotationDegrees() == row[3], "startup rotation changed");
                assertTest(helper, destinations.add(placement.destination()), "startup destinations were not unique");
            }
        }
        var plan = DungeonStartupSchematicPlan.buildPlan(List.of("pyroclast"));
        assertPlanSize(helper, plan);
        assertTest(helper, plan.requests().stream().allMatch(request -> request.pasteAir()
                        && request.schematicFilename().endsWith(".schem") && request.destination() != null),
                "startup requests did not preserve air, filename, or destination contracts");
        helper.succeed();
    }

    private static void startupPlanPartyCounts(GameTestHelper helper) {
        assertRejected(helper, null, "null party classes were accepted");
        assertRejected(helper, List.of(), "empty party classes were accepted");
        assertRejected(helper, List.of("a", "b", "c", "d", "e", "f", "g"),
                "oversized party classes were silently truncated");
        helper.succeed();
    }

    private static void startupPlanBlankEntry(GameTestHelper helper) {
        var withNull = DungeonStartupSchematicPlan.buildPlan(java.util.Arrays.asList("pyroclast", null));
        var withBlank = DungeonStartupSchematicPlan.buildPlan(List.of("pyroclast", "  "));
        assertPlanSize(helper, withNull);
        assertPlanSize(helper, withBlank);
        assertTest(helper, DungeonStartupSchematicPlan.BLANK_SLOT_CLASS.equals(withNull.normalizedClassSlots().get(1)),
                "null class entry did not normalize to blankslot");
        assertTest(helper, DungeonStartupSchematicPlan.BLANK_SLOT_CLASS.equals(withBlank.normalizedClassSlots().get(1)),
                "blank class entry did not normalize to blankslot");
        helper.succeed();
    }

    private static void assertPlanSize(GameTestHelper helper,
                                       DungeonStartupSchematicPlan.StartupPastePlan plan) {
        assertTest(helper, plan.requests().size() == DungeonStartupSchematicPlan.EXPECTED_OPERATION_COUNT,
                "startup plan did not contain 36 requests");
        for (var group : DungeonStartupSchematicPlan.groups()) {
            long count = plan.requests().stream().filter(request -> request.groupId().equals(group.id())).count();
            assertTest(helper, count == 6, "startup plan group did not contain six requests");
        }
    }

    private static void assertRejected(GameTestHelper helper, List<String> classes, String message) {
        boolean rejected = false;
        try {
            DungeonStartupSchematicPlan.buildPlan(classes);
        } catch (IllegalArgumentException expected) {
            rejected = true;
        }
        assertTest(helper, rejected, message);
    }

    private static void assertTest(GameTestHelper helper, boolean condition, String message) {
        helper.assertTrue(condition, net.minecraft.network.chat.Component.literal(message));
    }

    private static void register(RegisterGameTestsEvent event, Holder<TestEnvironmentDefinition> environment,
                                 String name, Consumer<GameTestHelper> test) {
        TestData<Holder<TestEnvironmentDefinition>> data = new TestData<>(environment, EMPTY_STRUCTURE, 20, 0, true, Rotation.NONE);
        event.registerTest(id(name), new DirectGameTestInstance(test, data));
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(CosmicDungeonMod.MOD_ID, path);
    }

    private static final class DirectGameTestInstance extends GameTestInstance {
        private final Consumer<GameTestHelper> test;
        private DirectGameTestInstance(Consumer<GameTestHelper> test, TestData<Holder<TestEnvironmentDefinition>> data) {
            super(data);
            this.test = test;
        }
        @Override public void run(GameTestHelper helper) { test.accept(helper); }
        @Override @SuppressWarnings({"unchecked", "rawtypes"})
        public MapCodec<? extends GameTestInstance> codec() { return (MapCodec) FunctionGameTestInstance.CODEC; }
        @Override protected net.minecraft.network.chat.MutableComponent typeDescription() {
            return net.minecraft.network.chat.Component.literal("direct cosmic dungeon instance test");
        }
    }
}
