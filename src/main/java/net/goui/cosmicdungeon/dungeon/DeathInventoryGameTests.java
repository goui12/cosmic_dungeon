package net.goui.cosmicdungeon.dungeon;

import com.mojang.authlib.GameProfile;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.goui.cosmicdungeon.dungeon.d1.D1RunData;
import net.goui.cosmicdungeon.dungeon.d1.DeathInventoryLayout;
import net.goui.cosmicdungeon.dungeon.d1.DeathInventoryRecovery;
import net.goui.cosmicdungeon.mercenary.MercenaryResurrectionState;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

/** Native death/drop/pickup coverage for latest-death organization. */
public final class DeathInventoryGameTests {
    private DeathInventoryGameTests() {}

    private static final class Fixture implements AutoCloseable {
        final GameTestHelper helper;
        final ServerLevel level;
        final long runId;
        final ServerPlayer player;
        final D1RunData data;
        final Map<Long, DungeonRunRegistryData.RunRecord> runs;
        final Map<UUID, ServerPlayer> players;
        final List<ServerPlayer> online;

        @SuppressWarnings("unchecked")
        Fixture(GameTestHelper helper, long runId) {
            this.helper = helper;
            this.runId = runId;
            level = helper.getLevel();
            var server = level.getServer();
            player = FakePlayerFactory.get(level, new GameProfile(UUID.randomUUID(), "DeathLayoutTest"));
            player.connection.player = player;
            player.setPos(helper.absoluteVec(new net.minecraft.world.phys.Vec3(.5, 10.5, .5)));
            try {
                var rf = DungeonRunRegistryData.class.getDeclaredField("runsById");
                rf.setAccessible(true);
                runs = (Map<Long, DungeonRunRegistryData.RunRecord>)rf.get(DungeonRunRegistryData.get(server));
                var pf = PlayerList.class.getDeclaredField("playersByUUID");
                pf.setAccessible(true);
                players = (Map<UUID, ServerPlayer>)pf.get(server.getPlayerList());
                var lf = PlayerList.class.getDeclaredField("players");
                lf.setAccessible(true);
                online = (List<ServerPlayer>)lf.get(server.getPlayerList());
            } catch (ReflectiveOperationException error) {
                throw new IllegalStateException("Native fixture unavailable", error);
            }
            var run = new DungeonRunRegistryData.RunRecord(runId, "dungeon_1", "minecraft:overworld", 0,
                    List.of(level.dimension().location().toString()), 1, "ACTIVE", "", 0,
                    List.of(player.getUUID()), List.of(), List.of());
            if (runs.containsKey(runId) || players.containsKey(player.getUUID()))
                throw new IllegalStateException("Fixture collision");
            runs.put(runId, run);
            players.put(player.getUUID(), player);
            online.add(player);
            data = D1RunData.get(server);
        }

        void check(boolean condition, String message) {
            helper.assertTrue(condition, Component.literal(message));
        }

        ItemEntity drop(DeathInventoryRecovery.Provenance provenance) {
            return level.getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(4),
                    item -> provenance.equals(DeathInventoryRecovery.provenance(item))).stream().findFirst().orElse(null);
        }

        @Override public void close() {
            players.remove(player.getUUID());
            online.removeIf(p -> p.getUUID().equals(player.getUUID()));
            if (!player.isRemoved()) level.removePlayerImmediately(player, Entity.RemovalReason.DISCARDED);
            runs.remove(runId);
            data.clearRun(runId);
        }
    }

    public static void lifecycle(GameTestHelper helper) {
        try (var f = new Fixture(helper, Long.MAX_VALUE - 301)) {
            var server = f.level.getServer();
            var keep = f.level.getGameRules().getRule(GameRules.RULE_KEEPINVENTORY);
            boolean oldKeep = keep.get();
            java.util.function.Consumer<LivingDeathEvent> cancel = event -> {
                if (event.getEntity() == f.player) event.setCanceled(true);
            };
            try {
                keep.set(false, server);
                f.player.getInventory().setItem(0, new ItemStack(Items.DIAMOND, 3));
                f.player.getInventory().setItem(10, new ItemStack(Items.EMERALD, 2));
                f.player.getInventory().setItem(Inventory.SLOT_OFFHAND, new ItemStack(Items.TORCH, 4));

                NeoForge.EVENT_BUS.addListener(cancel);
                try {
                    f.player.setHealth(0);
                    f.player.die(f.player.damageSources().generic());
                    f.check(DeathInventoryLayout.read(f.data, f.runId, f.player) == null,
                            "Cancelled death created a layout");
                    f.check(MercenaryResurrectionState.death(f.data, f.runId, f.player.getUUID()) == null,
                            "Cancelled death created a death token");
                } finally {
                    NeoForge.EVENT_BUS.unregister(cancel);
                }

                f.player.setHealth(0);
                f.player.die(f.player.damageSources().generic());
                var death = MercenaryResurrectionState.death(f.data, f.runId, f.player.getUUID());
                var layout = DeathInventoryLayout.read(f.data, f.runId, f.player);
                f.check(death != null && layout != null && layout.death().equals(death.id()),
                        "Real death did not create one canonical layout/token");
                f.check(layout.entry(0) != null && layout.entry(10) != null
                                && layout.entry(Inventory.SLOT_OFFHAND) != null,
                        "Layout omitted inventory/hotbar/offhand contents");
                var diamond = f.drop(new DeathInventoryRecovery.Provenance(
                        f.player.getUUID(), death.id(), f.runId, 0));
                f.check(diamond != null && diamond.getItem().getCount() == 3,
                        "Physical death drop lost slot provenance");
                var same = death.id();
                net.goui.cosmicdungeon.mercenary.MercenaryResurrection.prepareDeath(f.player);
                f.check(same.equals(MercenaryResurrectionState.death(
                                f.data, f.runId, f.player.getUUID()).id()),
                        "Repeated death preparation replaced the latest-death token");
                // Existing run serialization must retain layouts without changing legacy save shapes.
                try {
                    var field = D1RunData.class.getDeclaredField("CODEC");
                    field.setAccessible(true);
                    @SuppressWarnings("unchecked")
                    var codec = (com.mojang.serialization.Codec<D1RunData>)field.get(null);
                    var encoded = codec.encodeStart(com.mojang.serialization.JsonOps.INSTANCE, f.data).getOrThrow();
                    var decoded = codec.parse(com.mojang.serialization.JsonOps.INSTANCE, encoded).getOrThrow();
                    var loaded = DeathInventoryLayout.read(decoded, f.runId, f.player);
                    f.check(loaded != null && loaded.death().equals(same)
                                    && loaded.entry(10).template().getCount() == 2,
                            "Latest layout did not survive run save/load");
                    var legacy = codec.parse(com.mojang.serialization.JsonOps.INSTANCE,
                            com.google.gson.JsonParser.parseString("{\"runs\":[]}")).getOrThrow();
                    f.check(DeathInventoryLayout.read(legacy, f.runId, f.player) == null,
                            "Old saves must have no synthetic death layout");
                } catch (ReflectiveOperationException error) {
                    throw new IllegalStateException(error);
                }

                f.player.setHealth(f.player.getMaxHealth());
                f.check(DeathInventoryRecovery.beginPickup(f.player, diamond) != null,
                        "Latest physical drop should remain eligible after life is restored");
                net.goui.cosmicdungeon.mercenary.MercenaryResurrection.respawned(
                        new net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerRespawnEvent(f.player, false));
                f.player.getInventory().setItem(8, new ItemStack(Items.APPLE));
                f.player.setHealth(0);
                f.player.die(f.player.damageSources().generic());
                var newer = DeathInventoryLayout.read(f.data, f.runId, f.player);
                f.check(newer != null && !newer.death().equals(same) && newer.entry(8) != null,
                        "New real death must replace previous history");
                f.player.setHealth(f.player.getMaxHealth());
                f.check(DeathInventoryRecovery.beginPickup(f.player, diamond) == null && !diamond.isRemoved(),
                        "Old-death item must remain physical but lose automatic organization");
                f.data.clearRun(f.runId);
                f.check(DeathInventoryLayout.read(f.data, f.runId, f.player) == null,
                        "Run retirement must clear the latest layout");
                helper.succeed();
            } finally {
                keep.set(oldKeep, server);
                f.level.getEntitiesOfClass(ItemEntity.class, f.player.getBoundingBox().inflate(8)).forEach(Entity::discard);
            }
        }
    }

    public static void pickup(GameTestHelper helper) {
        try (var f = new Fixture(helper, Long.MAX_VALUE - 302)) {
            var server = f.level.getServer();
            var keep = f.level.getGameRules().getRule(GameRules.RULE_KEEPINVENTORY);
            boolean oldKeep = keep.get();
            try {
                keep.set(false, server);
                f.player.getInventory().setItem(15, new ItemStack(Items.EMERALD, 5));
                f.player.setHealth(0);
                f.player.die(f.player.damageSources().generic());
                var death = MercenaryResurrectionState.death(f.data, f.runId, f.player.getUUID());
                var emerald = f.drop(new DeathInventoryRecovery.Provenance(
                        f.player.getUUID(), death.id(), f.runId, 15));
                f.check(emerald != null, "Expected marked emerald drop");
                f.player.setHealth(f.player.getMaxHealth());
                emerald.setNoPickUpDelay();
                emerald.playerTouch(f.player);
                f.check(f.player.getInventory().getItem(15).is(Items.EMERALD)
                                && f.player.getInventory().getItem(15).getCount() == 5,
                        "Recovered physical stack was not returned to its empty original slot");
                f.check(f.player.getInventory().getItem(0).isEmpty(),
                        "Recovery left the native first-free-slot copy behind");
            } finally {
                keep.set(oldKeep, server);
                f.level.getEntitiesOfClass(ItemEntity.class, f.player.getBoundingBox().inflate(8)).forEach(Entity::discard);
            }
        }
        try (var f = new Fixture(helper, Long.MAX_VALUE - 303)) {
            var server = f.level.getServer();
            var keep = f.level.getGameRules().getRule(GameRules.RULE_KEEPINVENTORY);
            boolean oldKeep = keep.get();
            try {
                keep.set(false, server);
                f.player.getInventory().setItem(4, new ItemStack(Items.DIAMOND, 2));
                f.player.setHealth(0);
                f.player.die(f.player.damageSources().generic());
                var death = MercenaryResurrectionState.death(f.data, f.runId, f.player.getUUID());
                var diamond = f.drop(new DeathInventoryRecovery.Provenance(
                        f.player.getUUID(), death.id(), f.runId, 4));
                f.check(diamond != null, "Expected marked diamond drop");
                f.player.setHealth(f.player.getMaxHealth());
                f.player.getInventory().setItem(4, new ItemStack(Items.STONE, 1));
                diamond.setNoPickUpDelay();
                diamond.playerTouch(f.player);
                f.check(f.player.getInventory().getItem(4).is(Items.STONE)
                                && f.player.getInventory().countItem(Items.DIAMOND) == 2,
                        "Occupied new contents were overwritten or recovered items were lost");
            } finally {
                keep.set(oldKeep, server);
                f.level.getEntitiesOfClass(ItemEntity.class, f.player.getBoundingBox().inflate(8)).forEach(Entity::discard);
            }
        }
        try (var f = new Fixture(helper, Long.MAX_VALUE - 304)) {
            var keep = f.level.getGameRules().getRule(GameRules.RULE_KEEPINVENTORY);
            boolean oldKeep = keep.get();
            try {
                keep.set(false, f.level.getServer());
                f.player.getInventory().setItem(15, new ItemStack(Items.EMERALD, 5));
                f.player.setHealth(0);
                f.player.die(f.player.damageSources().generic());
                var death = MercenaryResurrectionState.death(f.data, f.runId, f.player.getUUID());
                var emerald = f.drop(new DeathInventoryRecovery.Provenance(
                        f.player.getUUID(), death.id(), f.runId, 15));
                f.check(emerald != null, "Expected marked partial-pickup drop");
                f.player.setHealth(f.player.getMaxHealth());
                for (int slot = 0; slot < 36; slot++)
                    f.player.getInventory().setItem(slot, new ItemStack(Items.STONE, 64));
                f.player.getInventory().setItem(0, new ItemStack(Items.EMERALD, 63));
                emerald.setNoPickUpDelay();
                emerald.playerTouch(f.player);
                f.check(emerald.getItem().getCount() == 4
                                && f.player.getInventory().getItem(0).getCount() == 64
                                && f.player.getInventory().getItem(15).is(Items.STONE),
                        "Partial pickup must preserve occupied target and remaining physical count");
                f.player.getInventory().setItem(15, ItemStack.EMPTY);
                emerald.playerTouch(f.player);
                f.check(f.player.getInventory().getItem(15).getCount() == 4
                                && f.player.getInventory().getItem(0).getCount() == 64,
                        "Later recovery must not move preexisting matching stacks");
            } finally {
                keep.set(oldKeep, f.level.getServer());
                f.level.getEntitiesOfClass(ItemEntity.class, f.player.getBoundingBox().inflate(8)).forEach(Entity::discard);
            }
        }
        try (var f = new Fixture(helper, Long.MAX_VALUE - 305)) {
            var keep = f.level.getGameRules().getRule(GameRules.RULE_KEEPINVENTORY);
            boolean oldKeep = keep.get();
            try {
                keep.set(false, f.level.getServer());
                f.player.getInventory().setItem(15, new ItemStack(Items.EMERALD, 5));
                f.player.setHealth(0);
                f.player.die(f.player.damageSources().generic());
                var death = MercenaryResurrectionState.death(f.data, f.runId, f.player.getUUID());
                var emerald = f.drop(new DeathInventoryRecovery.Provenance(
                        f.player.getUUID(), death.id(), f.runId, 15));
                f.check(emerald != null, "Expected guarded pickup drop");
                f.player.setHealth(f.player.getMaxHealth());
                var unmarked = new ItemEntity(f.level, f.player.getX(), f.player.getY(), f.player.getZ(),
                        new ItemStack(Items.EMERALD));
                f.check(!DeathInventoryRecovery.mayMerge(emerald, unmarked),
                        "Unrelated physical items must not inherit death provenance");
                unmarked.getPersistentData().merge(emerald.getPersistentData().copy());
                f.check(DeathInventoryRecovery.mayMerge(emerald, unmarked),
                        "Identical death-slot provenance must remain merge-compatible");
                unmarked.getPersistentData().getCompoundOrEmpty("cosmicdungeon_death_inventory_v1")
                        .putString("owner", UUID.randomUUID().toString());
                f.check(DeathInventoryRecovery.beginPickup(f.player, unmarked) == null
                                && !DeathInventoryRecovery.mayMerge(emerald, unmarked),
                        "Another owner's drops cannot be organized or merged as this owner's");
                f.player.inventoryMenu.setCarried(new ItemStack(Items.STONE));
                f.check(DeathInventoryRecovery.beginPickup(f.player, emerald) == null,
                        "Cursor custody must block optional organization");
                emerald.setNoPickUpDelay();
                emerald.playerTouch(f.player);
                f.check(f.player.getInventory().countItem(Items.EMERALD) == 5
                                && f.player.getInventory().getItem(15).isEmpty(),
                        "Unsafe organization must still allow ordinary native pickup");
                f.player.inventoryMenu.setCarried(ItemStack.EMPTY);
            } finally {
                keep.set(oldKeep, f.level.getServer());
                f.level.getEntitiesOfClass(ItemEntity.class, f.player.getBoundingBox().inflate(8)).forEach(Entity::discard);
            }
        }
        helper.succeed();
    }
}
