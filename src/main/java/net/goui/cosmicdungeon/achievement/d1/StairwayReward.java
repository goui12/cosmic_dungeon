package net.goui.cosmicdungeon.achievement.d1;

import net.goui.cosmicdungeon.CosmicDungeonMod;
import net.goui.cosmicdungeon.achievement.*;
import net.goui.cosmicdungeon.playerclass.api.ClassData;
import net.goui.cosmicdungeon.transaction.SingleItemCommit;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.item.*;
import net.minecraft.world.level.storage.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.slf4j.LoggerFactory;
import java.util.*;

/** D76 confirms an Elytra. One ordinary full-durability vanilla Elytra, once per UUID.
 * Delivery and receipt share the SAME player save; advancement is a replayable projection.
 * No unapproved one-flight/destruction behavior is inferred from the older misspelled note. */
@EventBusSubscriber(modid = CosmicDungeonMod.MOD_ID)
public final class StairwayReward {
    private static final String KEY = "stairway_reward_v1";
    private StairwayReward() {}
    public static boolean valid(CompoundTag receipt, UUID owner) {
        try {
            UUID.fromString(receipt.getStringOr("transaction", ""));
            var item = receipt.getCompoundOrEmpty("delivered");
            int slot = receipt.getIntOr("slot", -1);
            int schema = receipt.getIntOr("schema", 0);
            long run = receipt.getLongOr("run", -1);
            boolean context = schema == 1 && run > 0
                    || schema == 2 && receipt.get("run") instanceof LongTag && run == 0
                    && receipt.getStringOr("dimension", "").equals("minecraft:overworld");
            return context && owner.toString().equals(receipt.getStringOr("owner", ""))
                    && slot >= 0 && slot < 36
                    && item.getStringOr("id", "").equals("minecraft:elytra") && item.getIntOr("count", 1) == 1;
        } catch (IllegalArgumentException invalid) { return false; }
    }
    private static CompoundTag state(ServerPlayer player) {
        return player.getPersistentData().getCompoundOrEmpty(ClassData.ROOT_TAG).getCompoundOrEmpty(KEY);
    }
    private static CompoundTag receiptIn(CompoundTag player) {
        return player.getCompoundOrEmpty("NeoForgeData").getCompoundOrEmpty(ClassData.ROOT_TAG).getCompoundOrEmpty(KEY);
    }
    public static boolean savedSnapshotMatches(CompoundTag expected, CompoundTag saved) {
        var receipt = receiptIn(expected);
        try {
            if (!valid(receipt, UUID.fromString(receipt.getStringOr("owner", "")))) return false;
        } catch (IllegalArgumentException invalid) { return false; }
        return expected.get("Inventory") instanceof ListTag && receipt.equals(receiptIn(saved))
                && Objects.equals(expected.get("Inventory"), saved.get("Inventory"))
                && Objects.equals(expected.get("equipment"), saved.get("equipment"));
    }
    private static CompoundTag snapshot(ServerPlayer player) {
        var problems = new ProblemReporter.Collector();
        var output = TagValueOutput.createWithContext(problems, player.registryAccess());
        player.saveWithoutId(output);
        if (!problems.isEmpty()) throw new IllegalStateException(problems.getReport());
        return output.buildResult();
    }
    private static boolean persist(ServerPlayer player, CompoundTag expected) throws Exception {
        var server = player.level().getServer();
        boolean owner = server.isSingleplayerOwner(player.nameAndId());
        if (owner) server.saveEverything(true, true, true);
        else server.getPlayerList().getPlayerIo().save(player);
        var path = server.getWorldPath(LevelResource.PLAYER_DATA_DIR).resolve(player.getStringUUID() + ".dat");
        if (!savedSnapshotMatches(expected, NbtIo.readCompressed(path, NbtAccounter.unlimitedHeap()))) return false;
        if (owner) {
            var level = NbtIo.readCompressed(server.getWorldPath(LevelResource.LEVEL_DATA_FILE), NbtAccounter.unlimitedHeap());
            return savedSnapshotMatches(expected, level.getCompoundOrEmpty("Data").getCompoundOrEmpty("Player"));
        }
        return true;
    }
    /** Caller verified the real authored World Spawn chest menu (MASTER Achievements!B18/C18).
     * Existing schema1 instance receipts remain valid; new schema2 claims are outside any run. */
    public static boolean deliver(ServerPlayer player, long runId, BlockPos chest) {
        if (player == null || runId != 0 || chest == null || !player.isAlive() || player.isSpectator()
                || net.goui.cosmicdungeon.auth.AccessPolicy.isDeveloper(player)
                || !player.level().dimension().equals(net.minecraft.world.level.Level.OVERWORLD)
                || net.goui.cosmicdungeon.transaction.InventoryTransactionGuard.blocked(player)) return false;
        var holder = player.level().getServer().getAdvancements().get(CosmicAchievementIds.STAIRWAY_TO_HEAVEN);
        if (holder == null) return false;
        var old = state(player);
        if (player.getPersistentData().getCompoundOrEmpty(ClassData.ROOT_TAG).contains(KEY)) {
            if (valid(old, player.getUUID())) { award(player); return true; }
            player.sendSystemMessage(Component.literal("This reward needs a developer's save check.")); return false;
        }
        // Grandfather already-earned legacy advancements; never create another item on upgrade.
        if (player.getAdvancements().getOrStartProgress(holder).isDone()) return true;
        int slot = player.getInventory().getFreeSlot();
        if (slot < 0) {
            player.sendSystemMessage(Component.literal("Make room, then reopen this chest to claim your Elytra.")); return false;
        }
        var originalRoot = player.getPersistentData().getCompoundOrEmpty(ClassData.ROOT_TAG).copy();
        CompoundTag expected;
        try {
            var reward = new ItemStack(Items.ELYTRA);
            var problems = new ProblemReporter.Collector();
            var output = TagValueOutput.createWithContext(problems, player.registryAccess());
            output.store("delivered", ItemStack.CODEC, reward);
            if (!problems.isEmpty()) throw new IllegalStateException(problems.getReport());
            var receipt = output.buildResult();
            receipt.putInt("schema", 2); receipt.putString("owner", player.getStringUUID());
            receipt.putString("transaction", UUID.randomUUID().toString()); receipt.putInt("slot", slot);
            receipt.putLong("run", runId); receipt.putLong("chest", chest.asLong());
            receipt.putString("dimension", player.level().dimension().location().toString());
            var root = originalRoot.copy(); root.put(KEY, receipt);
            player.getInventory().setItem(slot, reward);
            player.getPersistentData().put(ClassData.ROOT_TAG, root);
            expected = snapshot(player);
        } catch (RuntimeException serializationFailure) {
            player.getInventory().setItem(slot, ItemStack.EMPTY);
            player.getPersistentData().put(ClassData.ROOT_TAG, originalRoot);
            LoggerFactory.getLogger("CosmicDungeon|Stairway").error("Reward snapshot failed for {}", player.getUUID(), serializationFailure);
            return false;
        }
        var result = SingleItemCommit.finish(() -> persist(player, expected), () -> award(player));
        player.getInventory().setChanged(); player.inventoryMenu.broadcastChanges();
        if (result != SingleItemCommit.Result.COMMITTED) {
            // Save may have succeeded: keep the staged pair. Never redeliver/refund on uncertainty.
            LoggerFactory.getLogger("CosmicDungeon|Stairway").error("Reward save verification failed for {}", player.getUUID());
            player.connection.disconnect(Component.literal("Your reward needs a save check. Please reconnect."));
            return false;
        }
        return true;
    }
    private static void award(ServerPlayer player) { CosmicAdvancementUtil.grant(player, CosmicAchievementIds.STAIRWAY_TO_HEAVEN); }
    @SubscribeEvent public static void login(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && valid(state(player), player.getUUID())) award(player);
    }
    // TODO(M79/M81, licensed TEST): bind the actual uppermost World Spawn chest after authored-world review.
    // Fault-inject at player save/readback boundaries on dedicated and integrated TEST.
    // Verify full inventory/reopen, clone, already-earned legacy saves, and failed-run item loss.
    // This receipt prevents duplicate delivery; it does not exempt the Elytra from D1 failure loss.
}
