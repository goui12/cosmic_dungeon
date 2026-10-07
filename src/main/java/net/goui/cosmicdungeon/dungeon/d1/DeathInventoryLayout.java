package net.goui.cosmicdungeon.dungeon.d1;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * One bounded, non-custodial image of the latest real D1 death inventory.
 * The stored stacks are matching templates only; they are never used to recreate items.
 */
public final class DeathInventoryLayout {
    private static final int VERSION = 1;
    private static final int MAX_ENTRIES = 64;
    private static final int MAX_ENCODED_CHARS = 262_144;

    private DeathInventoryLayout() {}

    public record Entry(int slot, ItemStack template) {
        public Entry {
            if (slot < 0 || slot >= 128 || template == null || template.isEmpty())
                throw new IllegalArgumentException("Invalid death inventory entry");
            template = template.copy();
        }
    }

    public record Snapshot(UUID death, String dimension, Vec3 position, List<Entry> entries) {
        public Snapshot {
            if (death == null || dimension == null || dimension.isBlank() || dimension.length() > 128
                    || position == null || !Double.isFinite(position.x) || !Double.isFinite(position.y)
                    || !Double.isFinite(position.z) || entries == null || entries.size() > MAX_ENTRIES)
                throw new IllegalArgumentException("Invalid death inventory snapshot");
            var seen = new HashSet<Integer>();
            var copies = new ArrayList<Entry>(entries.size());
            for (var entry : entries) {
                if (entry == null || !seen.add(entry.slot()))
                    throw new IllegalArgumentException("Duplicate death inventory slot");
                copies.add(new Entry(entry.slot(), entry.template()));
            }
            entries = List.copyOf(copies);
        }

        public Entry entry(int slot) {
            for (var entry : entries) if (entry.slot() == slot) return entry;
            return null;
        }
    }

    public record Move(int sourceSlot, int count) {
        public Move {
            if (sourceSlot < 0 || count < 1) throw new IllegalArgumentException("Invalid recovery move");
        }
    }

    public record Plan(List<Move> moves, int addToTarget) {
        public static final Plan EMPTY = new Plan(List.of(), 0);
        public Plan {
            moves = List.copyOf(moves);
            if (addToTarget < 0) throw new IllegalArgumentException("Invalid target amount");
            int total = 0;
            for (var move : moves) total = Math.addExact(total, move.count());
            if (total != addToTarget) throw new IllegalArgumentException("Recovery plan is not conservative");
        }
        public boolean empty() { return addToTarget == 0; }
    }

    static String key(UUID player) {
        return "death_inventory_layout:" + player;
    }

    public static Snapshot capture(D1RunData data, long run, ServerPlayer player, UUID death) {
        var inventory = player.getInventory();
        var entries = new ArrayList<Entry>();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            var stack = inventory.getItem(slot);
            if (!stack.isEmpty()) entries.add(new Entry(slot, stack));
        }
        var snapshot = new Snapshot(death, player.level().dimension().location().toString(),
                player.position(), entries);
        String encoded = encode(player, snapshot);
        if (encoded.length() > MAX_ENCODED_CHARS)
            throw new IllegalArgumentException("Death inventory layout is too large");
        data.setValue(run, key(player.getUUID()), encoded);
        return snapshot;
    }

    public static Snapshot read(D1RunData data, long run, ServerPlayer player) {
        var values = data.values(run, key(player.getUUID()));
        if (values.size() != 1 || values.getFirst().length() > MAX_ENCODED_CHARS) return null;
        try {
            var snapshot = decode(player, values.getFirst());
            int size = player.getInventory().getContainerSize();
            for (var entry : snapshot.entries()) if (entry.slot() >= size) return null;
            return snapshot;
        } catch (RuntimeException malformed) {
            return null;
        }
    }

    public static void clear(D1RunData data, long run, UUID player) {
        data.setValue(run, key(player), null);
    }

    private static String encode(ServerPlayer player, Snapshot snapshot) {
        var root = new JsonObject();
        root.addProperty("v", VERSION);
        root.addProperty("death", snapshot.death().toString());
        root.addProperty("dimension", snapshot.dimension());
        root.addProperty("x", snapshot.position().x);
        root.addProperty("y", snapshot.position().y);
        root.addProperty("z", snapshot.position().z);
        var entries = new JsonArray();
        var ops = player.registryAccess().createSerializationContext(JsonOps.INSTANCE);
        for (var entry : snapshot.entries()) {
            var json = new JsonObject();
            json.addProperty("slot", entry.slot());
            json.add("item", ItemStack.CODEC.encodeStart(ops, entry.template()).getOrThrow());
            entries.add(json);
        }
        root.add("entries", entries);
        return root.toString();
    }

    private static Snapshot decode(ServerPlayer player, String encoded) {
        var root = JsonParser.parseString(encoded).getAsJsonObject();
        if (root.get("v").getAsInt() != VERSION) throw new IllegalArgumentException("Unknown death layout version");
        UUID death = UUID.fromString(root.get("death").getAsString());
        String dimension = root.get("dimension").getAsString();
        var position = new Vec3(root.get("x").getAsDouble(), root.get("y").getAsDouble(), root.get("z").getAsDouble());
        var ops = player.registryAccess().createSerializationContext(JsonOps.INSTANCE);
        var entries = new ArrayList<Entry>();
        var array = root.getAsJsonArray("entries");
        if (array.size() > MAX_ENTRIES) throw new IllegalArgumentException("Too many death layout entries");
        for (var element : array) {
            var json = element.getAsJsonObject();
            int slot = json.get("slot").getAsInt();
            var stack = ItemStack.CODEC.parse(ops, json.get("item")).getOrThrow();
            entries.add(new Entry(slot, stack));
        }
        return new Snapshot(death, dimension, position, entries);
    }

    private static int matchingCount(ItemStack stack, ItemStack template) {
        return stack != null && !stack.isEmpty() && ItemStack.isSameItemSameComponents(stack, template)
                ? stack.getCount() : 0;
    }

    /**
     * Pure planner: only post-pickup increases matching the actual recovered stack may move.
     * Preexisting counts are never removed, and an occupied incompatible destination is untouched.
     */
    public static Plan plan(List<ItemStack> before, List<ItemStack> after, ItemStack template,
                            int targetSlot, int targetLimit, boolean targetMayPlace) {
        if (before == null || after == null || before.size() != after.size() || template == null
                || template.isEmpty() || targetSlot < 0 || targetSlot >= after.size()
                || targetLimit < 1 || !targetMayPlace) return Plan.EMPTY;

        var target = after.get(targetSlot);
        if (!target.isEmpty() && !ItemStack.isSameItemSameComponents(target, template)) return Plan.EMPTY;
        int limit = Math.min(Math.max(1, targetLimit), template.getMaxStackSize());
        int room = Math.max(0, limit - matchingCount(target, template));
        if (room == 0) return Plan.EMPTY;

        var deltas = new int[after.size()];
        int movable = 0;
        for (int slot = 0; slot < after.size(); slot++) {
            int delta = matchingCount(after.get(slot), template) - matchingCount(before.get(slot), template);
            if (delta > 0) {
                deltas[slot] = delta;
                if (slot != targetSlot) movable = Math.addExact(movable, delta);
            }
        }
        int desired = Math.min(room, movable);
        if (desired <= 0) return Plan.EMPTY;

        var moves = new ArrayList<Move>();
        int left = desired;
        for (int slot = 0; slot < deltas.length && left > 0; slot++) {
            if (slot == targetSlot || deltas[slot] <= 0) continue;
            int take = Math.min(left, deltas[slot]);
            moves.add(new Move(slot, take));
            left -= take;
        }
        return new Plan(moves, desired - left);
    }
}
