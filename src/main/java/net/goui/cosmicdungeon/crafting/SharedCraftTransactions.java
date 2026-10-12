package net.goui.cosmicdungeon.crafting;

import java.util.*;
import java.util.function.BooleanSupplier;
import net.goui.cosmicdungeon.dungeon.ChopTravelRecovery;
import net.goui.cosmicdungeon.dungeon.metrics.*;
import net.goui.cosmicdungeon.playerclass.api.ClassData;
import net.goui.cosmicdungeon.transaction.PlayerSaveProof;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/** The established reservation/commit/owner-receipt protocol, generalized to several physical inventories. */
public final class SharedCraftTransactions {
    public static final String CUSTODY = "shared_craft_reservation_v1", RECEIPT = "shared_craft_receipt_v1";
    private static final Set<ServerPlayer> HOLDS = Collections.newSetFromMap(new WeakHashMap<>());
    private SharedCraftTransactions() {}
    private static CompoundTag root(ServerPlayer p) { return p.getPersistentData().getCompoundOrEmpty(ClassData.ROOT_TAG); }
    private static void root(ServerPlayer p, CompoundTag value) { p.getPersistentData().put(ClassData.ROOT_TAG, value); }
    private static void hold(ServerPlayer p, Exception failure) {
        if (!HOLDS.add(p)) return;
        PotionCursorCustody.freeze(p);
        com.mojang.logging.LogUtils.getLogger().error("Shared craft preserved for recovery: {}", p.getUUID(), failure);
        p.connection.disconnect(Component.literal("Your shared craft needs a save check. Its materials and output are reserved; reconnect to recover."));
    }
    public static boolean blocked(ServerPlayer p) {
        if (HOLDS.contains(p) || root(p).contains(CUSTODY)) return true;
        try { return SharedCraftData.get(p.level().getServer()).pending(p.getUUID()) != null; }
        catch (RuntimeException ex) { hold(p, ex); return true; }
    }
    public static boolean beforeInventoryChange(ServerPlayer p) { return !HOLDS.contains(p) && reconcile(p) && !blocked(p); }
    public static boolean readyForCleanup(MinecraftServer server, List<UUID> owners) {
        for (UUID owner : owners) {
            var p = server.getPlayerList().getPlayer(owner);
            if (p != null && blocked(p) && !reconcile(p)) return false;
        }
        for (UUID owner : owners) if (SharedCraftData.get(server).pending(owner) != null) return false;
        return true;
    }
    public static void validateImages(ServerPlayer registryOwner, SharedCraftPlan plan) {
        var outputImage = ChopTravelRecovery.decode(registryOwner, plan.tag("output"));
        ItemStack output = outputImage.getFirst();
        if (output.isEmpty() || output.getCount() > output.getMaxStackSize()
                || outputImage.stream().skip(1).anyMatch(s -> !s.isEmpty())) throw new IllegalArgumentException("Invalid crafting output");
        if (plan.cursor()) {
            var before = net.goui.cosmicdungeon.playerclass.dragoon.repair.RepairCustody.decode(registryOwner, plan.cursor(false));
            var expected = PotionOutputPlacement.cursor(before, output);
            if (expected == null || !net.goui.cosmicdungeon.playerclass.dragoon.repair.RepairCustody.encode(registryOwner, expected).equals(plan.cursor(true)))
                throw new IllegalArgumentException("Crafting cursor changes unrelated items/components");
        }
        if (plan.hasFuel()) {
            long powder = plan.owners().stream().flatMap(owner -> plan.debits(owner).stream())
                    .filter(debit -> debit.item().equals("minecraft:blaze_powder")).mapToLong(SharedCraftPlan.Debit::count).sum();
            if (powder < plan.fuel().getIntOr("powders", -1)) throw new IllegalArgumentException("Fuel has no physical powder debit");
        }
        for (UUID owner : plan.owners()) {
            var before = ChopTravelRecovery.decode(registryOwner, plan.inventory(owner, false));
            for (var debit : plan.debits(owner)) {
                if (debit.slot() >= before.size()) throw new IllegalArgumentException("Missing crafting slot");
                var stack = before.get(debit.slot());
                if (stack.isEmpty() || !BuiltInRegistries.ITEM.getKey(stack.getItem()).toString().equals(debit.item())
                        || stack.getCount() < debit.count()) throw new IllegalArgumentException("Crafting debit differs from reserved item");
                stack.shrink(debit.count());
            }
            if (owner.equals(plan.recipient()) && !plan.cursor() && !SharedCrafting.insert(before, output)) throw new IllegalArgumentException("Crafting output no longer fits image");
            if (!ChopTravelRecovery.encode(registryOwner, before).equals(plan.inventory(owner, true)))
                throw new IllegalArgumentException("Crafting image changes unrelated slots/components");
        }
    }
    static boolean execute(ServerPlayer crafter, SharedCraftPlan plan, BooleanSupplier revalidate) {
        var server = crafter.level().getServer(); var participants = new ArrayList<ServerPlayer>();
        for (UUID id : plan.owners()) {
            var p = server.getPlayerList().getPlayer(id);
            if (!SharedCrafting.availableOwner(p, false, plan.cursor() && id.equals(plan.recipient()))) return false;
            participants.add(p);
        }
        try {
            if (plan.cursor()) PotionCursorCustody.track(crafter);
            validateImages(crafter, plan);
            if (!revalidate.getAsBoolean() || plan.cursor() && !PotionCursorCustody.image(crafter).equals(plan.cursor(false))) return false;
            for (var p : participants) if (!ChopTravelRecovery.saveInventory(p).equals(plan.inventory(p.getUUID(), false))
                            || plan.cursor() && p.getUUID().equals(plan.recipient()) && !PotionCursorCustody.image(p).equals(plan.cursor(false))) return false;
            try {
                DungeonMetricsService.validateResources(server, new ResourceMetricsBatch(DungeonMetricsService.archive(server).worldId(),
                        plan.run(), plan.id(), plan.sequence(), plan.donationBefore(), plan.donationAfter(), plan.donated(), plan.events()));
            } catch (java.io.IOException ex) { throw new java.io.UncheckedIOException(ex); }
            var data = SharedCraftData.get(server);
            if (!data.fuelMatches(plan)) return false;
            data.reserve(plan);
            if (!data.flushVerified()) throw new IllegalStateException("Craft reservation not verified");
            for (var p : participants) {
                var next = root(p).copy(); next.put(CUSTODY, plan.reservation(p.getUUID())); root(p, next);
                if (!PlayerSaveProof.save(p)) throw new IllegalStateException("Craft owner reservation not verified");
            }
            boolean valid = data.fuelMatches(plan) && revalidate.getAsBoolean()
                    && (!plan.cursor() || PotionCursorCustody.image(crafter).equals(plan.cursor(false)))
                    && DungeonMetricsService.resourceSequence(server, plan.run()) + 1 == plan.sequence()
                    && DungeonMetricsService.donationCursor(server, plan.run()) == plan.donationBefore();
            for (var p : participants) valid &= ChopTravelRecovery.saveInventory(p).equals(plan.inventory(p.getUUID(), false));
            if (!valid) {
                for (var p : participants) if (!reconcile(p)) throw new IllegalStateException("Cancelled craft recovery pending");
                return false;
            }
            data.commit(plan.id());
            if (!data.flushVerified()) throw new IllegalStateException("Craft commit decision not verified");
            for (var p : participants) if (!reconcile(p)) throw new IllegalStateException("Craft owner settlement incomplete");
            return SharedCraftData.get(server).pendingRun(plan.run()) == null;
        } catch (RuntimeException ex) {
            participants.forEach(p -> hold(p, ex)); return false;
        } finally { participants.forEach(SharedCrafting::forget); }
    }
    public static boolean reconcile(ServerPlayer p) {
        if (HOLDS.contains(p)) return false;
        try {
            var owner = PlayerSaveProof.ownerCompound(p.getPersistentData(), ClassData.ROOT_TAG);
            var custody = PlayerSaveProof.ownerCompound(owner, CUSTODY);
            var receipt = PlayerSaveProof.ownerReceipt(owner, RECEIPT, p.getUUID(), 3, "id");
            if (!receipt.isEmpty() && (receipt.getLongOr("run", 0) <= 0 || receipt.getLongOr("sequence", -1) < 0
                    || receipt.getBoolean("committed").isEmpty()))
                throw new IllegalStateException("Unsupported owner receipt field " + RECEIPT + "; original save requires review.");
            var server = p.level().getServer(); var data = SharedCraftData.get(server); var plan = data.pending(p.getUUID());
            if (plan == null) {
                if (owner.contains(CUSTODY)) throw new IllegalStateException("Craft reservation has no world decision");
                PotionCursorCustody.restore(p);
                return true;
            }
            validateImages(p, plan);
            if (!receipt.isEmpty() && UUID.fromString(receipt.getStringOr("id", "")).equals(plan.id())
                    && !plan.receipt(p.getUUID()).equals(receipt))
                throw new IllegalStateException("Craft current-operation receipt conflicts with journal; original save requires review");
            if (!plan.recoverable(p.getUUID(), custody, receipt))
                throw new IllegalStateException("Craft owner proof differs from journal");
            // Exact journal proof must pass before restoring a saved cursor into live inventory.
            PotionCursorCustody.restore(p);
            if (!data.flushVerified()) throw new IllegalStateException("Craft decision readback failed");
            boolean newlyApplied = !plan.receipt(p.getUUID()).equals(receipt);
            if (newlyApplied) {
                if (plan.committed()) {
                    if (!ChopTravelRecovery.saveInventory(p).equals(plan.inventory(p.getUUID(), false))
                            || plan.cursor() && p.getUUID().equals(plan.recipient()) && !PotionCursorCustody.image(p).equals(plan.cursor(false)))
                        throw new IllegalStateException("Craft inventory differs from reserved source");
                    var after = ChopTravelRecovery.decode(p, plan.inventory(p.getUUID(), true));
                    for (int slot = 0; slot < after.size(); slot++) p.getInventory().setItem(slot, after.get(slot).copy());
                    p.getInventory().setChanged();
                    if (plan.cursor() && p.getUUID().equals(plan.recipient())) PotionCursorCustody.apply(p, plan.cursor(true));
                }
                var next = root(p).copy(); next.remove(CUSTODY); next.put(RECEIPT, plan.receipt(p.getUUID())); root(p, next);
            }
            if (!PlayerSaveProof.save(p)) throw new IllegalStateException("Craft owner receipt not verified");
            data.acknowledge(plan.id(), p.getUUID());
            if (!data.flushVerified()) throw new IllegalStateException("Craft acknowledgement not verified");
            // The durable owner receipt suppresses repeat messages on ordinary replay.
            if (newlyApplied && plan.committed() && !p.getUUID().equals(plan.recipient())) {
                var quantities = new LinkedHashMap<String, Integer>();
                for (var debit : plan.debits(p.getUUID())) quantities.merge(debit.item(), debit.count(), Integer::sum);
                var before = ChopTravelRecovery.decode(p, plan.inventory(p.getUUID(), false));
                for (var entry : quantities.entrySet()) {
                    var debit = plan.debits(p.getUUID()).stream().filter(d -> d.item().equals(entry.getKey())).findFirst().orElseThrow();
                    p.sendSystemMessage(Component.literal("You have contributed a(n) ").append(before.get(debit.slot()).getHoverName())
                            .append(Component.literal(" (x" + entry.getValue() + ") to " + plan.recipientName() + "'s " + plan.skill() + ".")));
                }
            }
            finish(server, data.pending(p.getUUID()));
            p.inventoryMenu.broadcastChanges(); SharedCrafting.forget(p); return true;
        } catch (Exception ex) { hold(p, ex); return false; }
    }
    private static void finish(MinecraftServer server, SharedCraftPlan plan) throws java.io.IOException {
        if (plan == null || !plan.ownersAcknowledged()) return;
        var data = SharedCraftData.get(server);
        if (plan.committed() && !plan.metricsAcknowledged()) {
            var batch = new ResourceMetricsBatch(DungeonMetricsService.archive(server).worldId(), plan.run(), plan.id(),
                    plan.sequence(), plan.donationBefore(), plan.donationAfter(), plan.donated(), plan.events());
            DungeonMetricsService.recordResources(server, batch);
            data.acknowledgeMetrics(plan.id());
            if (!data.flushVerified()) throw new IllegalStateException("Craft metrics receipt not verified");
        }
        if (!data.retireVerified(plan.id())) throw new IllegalStateException("Craft journal retirement not verified");
    }
    public static void stopped() { HOLDS.clear(); }
}
