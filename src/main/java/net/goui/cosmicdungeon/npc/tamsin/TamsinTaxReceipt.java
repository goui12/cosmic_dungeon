package net.goui.cosmicdungeon.npc.tamsin;

import net.goui.cosmicdungeon.CosmicDungeonMod;
import net.goui.cosmicdungeon.achievement.*;
import net.goui.cosmicdungeon.playerclass.api.ClassData;
import net.goui.cosmicdungeon.transaction.SingleItemCommit;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import org.slf4j.LoggerFactory;
import java.util.*;

/** Tax-only, one-player snapshot journal. The removed item and receipt are in the SAME vanilla
 * player save; an advancement is a replayable projection of that committed receipt.
 * Minecraft 1.21.10 saves equipped items separately from Inventory; verify both.
 * Integrated owners load Data.Player from level.dat, so verify that authoritative copy too. */
@EventBusSubscriber(modid = CosmicDungeonMod.MOD_ID)
public final class TamsinTaxReceipt {
    private TamsinTaxReceipt() {}
    public static boolean valid(CompoundTag receipt, UUID owner) {
        try {
            UUID.fromString(receipt.getStringOr("transaction", ""));
            return receipt.getIntOr("schema", 0) == 1
                    && owner.toString().equals(receipt.getStringOr("owner", ""))
                    && TamsinTaxRules.ELIGIBLE.contains(receipt.getStringOr("item", ""))
                    && receipt.getIntOr("quantity", 0) == 1
                    && validImages(receipt);
        } catch (IllegalArgumentException malformed) { return false; }
    }
    private static boolean validImages(CompoundTag receipt) {
        if (!(receipt.get("before") instanceof CompoundTag beforeImage)
                || !(receipt.get("after") instanceof CompoundTag afterImage)) return false;
        var before = beforeImage.copy();
        var after = afterImage.copy();
        var identity = net.goui.cosmicdungeon.item.identity.D1LootCatalog.find(receipt.getStringOr("item", ""));
        int count = before.getIntOr("count", 1);
        if (identity == null || !identity.baseItem().equals(before.getStringOr("id", "")) || count < 1) return false;
        if (count == 1) return after.isEmpty();
        if (after.getIntOr("count", 1) != count - 1) return false;
        before.remove("count"); after.remove("count");
        return before.equals(after);
    }
    public static boolean savedSnapshotMatches(CompoundTag expected, CompoundTag saved) {
        var expectedReceipt = receiptIn(expected); var actualReceipt = receiptIn(saved);
        return !expectedReceipt.isEmpty() && expectedReceipt.equals(actualReceipt)
                && Objects.equals(expected.get("Inventory"), saved.get("Inventory"))
                && Objects.equals(expected.get("equipment"), saved.get("equipment"));
    }
    private static CompoundTag receiptIn(CompoundTag player) {
        return player.getCompoundOrEmpty("NeoForgeData").getCompoundOrEmpty(ClassData.ROOT_TAG)
                .getCompoundOrEmpty(TamsinTaxProgress.KEY).getCompoundOrEmpty("receipt");
    }
    private static CompoundTag snapshot(ServerPlayer player) {
        var problems = new ProblemReporter.Collector();
        var output = TagValueOutput.createWithContext(problems, player.registryAccess());
        player.saveWithoutId(output);
        if (!problems.isEmpty()) throw new IllegalStateException("Player serialization failed: " + problems.getReport());
        return output.buildResult();
    }
    private static boolean persist(ServerPlayer player, CompoundTag expected) throws Exception {
        var server = player.level().getServer();
        boolean owner = server.isSingleplayerOwner(player.nameAndId());
        if (owner) server.saveEverything(true, true, true);
        else server.getPlayerList().getPlayerIo().save(player);
        var file = server.getWorldPath(LevelResource.PLAYER_DATA_DIR).resolve(player.getStringUUID() + ".dat");
        if (!savedSnapshotMatches(expected, NbtIo.readCompressed(file, NbtAccounter.unlimitedHeap()))) return false;
        if (owner) {
            var level = NbtIo.readCompressed(server.getWorldPath(LevelResource.LEVEL_DATA_FILE), NbtAccounter.unlimitedHeap());
            return savedSnapshotMatches(expected, level.getCompoundOrEmpty("Data").getCompoundOrEmpty("Player"));
        }
        return true;
    }
    /** Caller validated the menu, expiry, eligibility and entire stack immediately before entry. */
    public static boolean surrender(ServerPlayer player, int slot, String itemId) {
        var originalState = TamsinTaxProgress.read(player).copy();
        if (originalState.contains("receipt")) return false;
        var original = player.getInventory().getItem(slot).copy();
        var after = original.copy(); after.shrink(1);
        CompoundTag expected;
        try {
            var problems = new ProblemReporter.Collector();
            var images = TagValueOutput.createWithContext(problems, player.registryAccess());
            images.store("before", ItemStack.OPTIONAL_CODEC, original);
            images.store("after", ItemStack.OPTIONAL_CODEC, after);
            if (!problems.isEmpty()) throw new IllegalStateException(problems.getReport());
            var receipt = images.buildResult();
            receipt.putInt("schema", 1); receipt.putString("owner", player.getStringUUID());
            receipt.putString("transaction", UUID.randomUUID().toString());
            receipt.putString("item", itemId); receipt.putInt("quantity", 1); receipt.putInt("slot", slot);
            receipt.putLong("time_ms", System.currentTimeMillis());
            var next = originalState.copy(); next.put("receipt", receipt);
            player.getInventory().setItem(slot, after);
            TamsinTaxProgress.write(player, next);
            expected = snapshot(player);
        } catch (RuntimeException encodeFailure) {
            // No write has begun: reverting the staged memory is safe.
            player.getInventory().setItem(slot, original);
            TamsinTaxProgress.write(player, originalState);
            LoggerFactory.getLogger("CosmicDungeon|Tax").error("Tax snapshot failed for {}", player.getUUID(), encodeFailure);
            return false;
        }
        var result = SingleItemCommit.finish(() -> persist(player, expected), () -> award(player));
        player.getInventory().setChanged(); player.inventoryMenu.broadcastChanges();
        if (result != SingleItemCommit.Result.COMMITTED) {
            // A write may have succeeded. Never refund or consume again on an uncertain result.
            // Keep the paired snapshot in memory; ordinary disconnect saves may finish it.
            LoggerFactory.getLogger("CosmicDungeon|Tax").error("Tax save verification failed for {}; reconnect reconciliation required", player.getUUID());
            player.connection.disconnect(Component.literal("Your payment needs a save check. Please reconnect."));
            return false;
        }
        return true;
    }
    private static void award(ServerPlayer player) {
        CosmicAdvancementUtil.grant(player, CosmicAchievementIds.TAMSIN_TAX);
    }
    @SubscribeEvent
    public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        var receipt = TamsinTaxProgress.read(player).getCompoundOrEmpty("receipt");
        if (valid(receipt, player.getUUID())) award(player); // Loaded snapshot already includes removal.
    }
    // TODO(M03/M115, Trace/Trade docs): this one-player surrender is NOT a currency or trade
    // ledger. Multi-owner inventory escrow, account deltas, supply reconciliation and crash
    // recovery require durable cross-file coordination before those follow-up refactors.
    // TODO(M39, licensed TEST): kill/restart at each save boundary on dedicated and integrated
    // servers; verify stale .dat_old/level.dat recovery, disk errors, clone and achievement replay.
    // Arbitrary manual restores or storage corruption are not reconciled by this snapshot protocol.
}
