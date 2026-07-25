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
