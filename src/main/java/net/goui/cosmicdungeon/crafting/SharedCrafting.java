package net.goui.cosmicdungeon.crafting;

import java.util.*;
import java.util.function.Predicate;
import net.goui.cosmicdungeon.auth.AccessPolicy;
import net.goui.cosmicdungeon.dungeon.*;
import net.goui.cosmicdungeon.dungeon.d1.D1RunData;
import net.goui.cosmicdungeon.dungeon.metrics.DungeonMetricsService;
import net.goui.cosmicdungeon.npc.tamsin.GroupResourceConsent;
import net.goui.cosmicdungeon.playerclass.api.ClassData;
import net.goui.cosmicdungeon.transaction.InventoryTransactionGuard;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/** Trusted server recipe API. No client-supplied ingredients/output or public crafting packet. */
public final class SharedCrafting {
    public record Requirement(String key, int count, Predicate<ItemStack> matches) {
        public Requirement {
            if (key == null || key.isBlank() || key.length() > 128 || count < 1 || count > 64 || matches == null)
                throw new IllegalArgumentException("Crafting requirement");
        }
    }
    public record Recipe(String id, String skill, Set<String> classes, List<Requirement> requirements, ItemStack output, CompoundTag fuel, boolean cursor, String potionSource) {
        public Recipe {
            if (id == null || id.isBlank() || id.length() > 128 || skill == null || skill.isBlank() || skill.length() > 128)
                throw new IllegalArgumentException("Crafting recipe identity");
            fuel = fuel == null ? null : fuel.copy();
            if (fuel != null) BrewingFuel.validatePlan(fuel);
            if (cursor && (fuel == null || output.getCount() != 1)) throw new IllegalArgumentException("Cursor crafts require one fueled potion");
            classes = Set.copyOf(classes); requirements = List.copyOf(requirements); output = output.copy();
            if (potionSource != null && (fuel == null || requirements.stream().noneMatch(r -> r.key().equals(potionSource) && r.count() == 1)))
                throw new IllegalArgumentException("Potion source must identify one fueled input bottle");
            if (classes.isEmpty() || classes.size() > 16 || requirements.isEmpty() || requirements.size() > 16
                    || requirements.stream().map(Requirement::key).distinct().count() != requirements.size()
                    || requirements.stream().mapToInt(Requirement::count).sum() > 64
                    || output.isEmpty() || output.getCount() > output.getMaxStackSize())
                throw new IllegalArgumentException("Crafting recipe bounds");
        }
        public Recipe(String id, String skill, Set<String> classes, List<Requirement> requirements, ItemStack output, CompoundTag fuel, boolean cursor) {
            this(id, skill, classes, requirements, output, fuel, cursor, null);
        }
        public Recipe(String id, String skill, Set<String> classes, List<Requirement> requirements, ItemStack output, CompoundTag fuel) {
            this(id, skill, classes, requirements, output, fuel, false);
        }
        public Recipe(String id, String skill, Set<String> classes, List<Requirement> requirements, ItemStack output) {
            this(id, skill, classes, requirements, output, null, false);
        }
        @Override public CompoundTag fuel() { return fuel == null ? null : fuel.copy(); }
        @Override public ItemStack output() { return output.copy(); }
    }
    public record Availability(int own, int shared) {}
    private static final class Cache {
        final int tick, dirty;
        final NonNullList<ItemStack> items;
        Map<String, Integer> potions;
        Cache(int tick, int dirty, NonNullList<ItemStack> items) { this.tick = tick; this.dirty = dirty; this.items = items; }
    }
    private static final Map<ServerPlayer, Cache> CACHE = new WeakHashMap<>();
    private SharedCrafting() {}
    public static void forget(ServerPlayer player) { CACHE.remove(player); }
    public static void stopped() { CACHE.clear(); }
    private static Cache snapshot(ServerPlayer player, boolean fresh) {
        int tick = player.level().getServer().getTickCount(), dirty = player.getInventory().getTimesChanged();
        var cache = CACHE.get(player);
        if (fresh || cache == null || cache.dirty != dirty || tick - cache.tick >= 5 || tick < cache.tick) {
            if (CraftingWork.probe != null) CraftingWork.probe.inventoryScans++;
            cache = new Cache(tick, dirty, ChopTravelRecovery.inventory(player)); CACHE.put(player, cache);
        }
        return cache;
    }
    /** Immutable projection shares the existing snapshot's dirty/expiry/lifecycle; eligibility remains live per viewer. */
    static Map<String, Integer> potionStock(ServerPlayer player) {
        var cache = snapshot(player, false);
        if (cache.potions == null) {
            if (CraftingWork.probe != null) {
                CraftingWork.probe.stockBuilds++; CraftingWork.probe.stockSlots += cache.items.size();
            }
            cache.potions = PotionRecipeSelection.stock(cache.items);
        }
        return cache.potions;
    }
    static NonNullList<ItemStack> inventory(ServerPlayer player, boolean fresh) {
        var cache = snapshot(player, fresh);
        if (CraftingWork.probe != null) {
            CraftingWork.probe.inventoryCopies++; CraftingWork.probe.copiedSlots += cache.items.size();
        }
        var copy = NonNullList.withSize(cache.items.size(), ItemStack.EMPTY);
        for (int i = 0; i < copy.size(); i++) copy.set(i, cache.items.get(i).copy()); return copy;
    }
    private static boolean online(ServerPlayer p) {
        return p != null && p.connection != null && !p.hasDisconnected() && p.connection.isAcceptingMessages()
                && p.level().getServer().getPlayerList().getPlayer(p.getUUID()) == p;
    }
    static boolean availableOwner(ServerPlayer p, boolean reserved) { return availableOwner(p, reserved, false); }
    static boolean availableOwner(ServerPlayer p, boolean reserved, boolean allowCursor) {
        return online(p) && p.isAlive() && !p.isDeadOrDying() && !p.isSpectator() && !AccessPolicy.isDeveloper(p)
                && p.containerMenu == p.inventoryMenu && (allowCursor || p.inventoryMenu.getCarried().isEmpty())
                && !(reserved ? InventoryTransactionGuard.blockedExceptCrafting(p) : InventoryTransactionGuard.blocked(p));
    }
    private static DungeonRunRegistryData.RunRecord run(ServerPlayer p, long expected, Recipe recipe) {
        return run(p, expected, recipe.classes());
    }
    static DungeonRunRegistryData.RunRecord run(ServerPlayer p, long expected, Set<String> classes) {
        if (!online(p) || !classes.contains(ClassData.getClassId(p))) return null;
        var run = DungeonRunRegistryData.get(p.level().getServer()).findRunForPlayer(p.getUUID()).orElse(null);
        return run != null && run.runId() == expected && run.stateEnum() == DungeonRunState.ACTIVE
                && run.dungeonId().equals("dungeon_1") && run.containsDimension(p.level().dimension())
                && !run.isCompletionExited(p.getUUID()) && !D1RunData.get(p.level().getServer()).sealed(expected) ? run : null;
    }
    static List<ServerPlayer> participants(ServerPlayer crafter, DungeonRunRegistryData.RunRecord run) {
        var players = new ArrayList<ServerPlayer>(); players.add(crafter);
        for (UUID id : run.orderedPlayers()) {
            var donor = crafter.level().getServer().getPlayerList().getPlayer(id);
            if (!id.equals(crafter.getUUID()) && availableOwner(donor, false)
                    && GroupResourceConsent.canContribute(crafter, donor, run.runId())) players.add(donor);
        }
        return List.copyOf(players);
    }
    private static Map<String, Integer> requirements(Recipe recipe) {
        var result = new TreeMap<String, Integer>(); recipe.requirements().forEach(r -> result.put(r.key(), r.count())); return result;
    }
    private static Map<String, Integer> counts(Recipe recipe, NonNullList<ItemStack> inventory) {
        var counts = new TreeMap<String, Integer>();
        for (var stack : inventory) {
            if (stack.isEmpty()) continue;
            var matches = recipe.requirements().stream().filter(r -> r.matches().test(stack)).toList();
            if (matches.size() > 1) throw new IllegalArgumentException("Overlapping ingredient predicates");
            if (matches.size() == 1) counts.merge(matches.getFirst().key(), Math.min(4096, stack.getCount()),
                    (a, b) -> (int)Math.min(4096, (long)a + b));
        }
        return counts;
    }
    public static Availability preview(ServerPlayer player, long expected, Recipe recipe) {
        var run = run(player, expected, recipe);
        if (run == null || !availableOwner(player, false, recipe.cursor()) || SharedCraftData.get(player.level().getServer()).pendingRun(expected) != null)
            return new Availability(0, 0);
        var stocks = new ArrayList<SharedResourcePlanner.Stock>();
        for (var owner : participants(player, run)) stocks.add(new SharedResourcePlanner.Stock(owner.getUUID(),
                DungeonMetricsService.donationRank(owner.level().getServer(), expected, owner.getUUID()), counts(recipe, inventory(owner, false))));
        int own = SharedResourcePlanner.capacity(requirements(recipe), List.of(stocks.getFirst()));
        return new Availability(own, SharedResourcePlanner.capacity(requirements(recipe), stocks));
    }
    public static boolean craft(ServerPlayer player, long expected, Recipe recipe) {
        var plan = prepare(player, expected, recipe);
        if (plan == null) return false;
        return SharedCraftTransactions.execute(player, plan, () -> {
            var current = run(player, expected, recipe);
            if (current == null) return false;
            for (UUID owner : plan.owners()) {
                var donor = player.level().getServer().getPlayerList().getPlayer(owner);
                if (!availableOwner(donor, true, recipe.cursor() && owner.equals(player.getUUID())) || (!owner.equals(player.getUUID())
                        && !GroupResourceConsent.canContribute(player, donor, expected))) return false;
            }
            return true;
        });
    }
    /** Package-visible for native recovery tests; creates no reservation and mutates no owner. */
    static SharedCraftPlan prepare(ServerPlayer player, long expected, Recipe recipe) {
        var run = run(player, expected, recipe);
        if (run == null || !availableOwner(player, false, recipe.cursor()) || SharedCraftData.get(player.level().getServer()).pendingRun(expected) != null) return null;
        var server = player.level().getServer();
        DungeonMetricsService.begin(server, run, false);
        var players = participants(player, run); var inventories = new LinkedHashMap<UUID, NonNullList<ItemStack>>();
        var stocks = new ArrayList<SharedResourcePlanner.Stock>();
        for (var owner : players) {
            var items = inventory(owner, true); inventories.put(owner.getUUID(), items);
            stocks.add(new SharedResourcePlanner.Stock(owner.getUUID(), DungeonMetricsService.donationRank(server, expected, owner.getUUID()), counts(recipe, items)));
        }
        long cursor = DungeonMetricsService.donationCursor(server, expected);
        var allocation = SharedResourcePlanner.plan(player.getUUID(), requirements(recipe), stocks, cursor);
        if (allocation == null) return null;
        ItemStack produced = recipe.output();
        if (recipe.potionSource() != null) {
            var debit = allocation.debits().stream().filter(d -> d.ingredient().equals(recipe.potionSource())).findFirst().orElseThrow();
            var requirement = recipe.requirements().stream().filter(r -> r.key().equals(recipe.potionSource())).findFirst().orElseThrow();
            var source = inventories.get(debit.owner()).stream().filter(s -> !s.isEmpty() && requirement.matches().test(s)).findFirst().orElseThrow();
            if (!net.goui.cosmicdungeon.trade.TradeItemTransfer.mayOffer(source, debit.owner())) return null;
            var owned = net.goui.cosmicdungeon.trade.TradeItemTransfer.forRecipient(source, debit.owner(), player.getUUID());
            produced = PotionStacks.brewed(owned, produced);
        }
        var owners = new CompoundTag(); var donorRanks = new CompoundTag();
        var selected = new TreeSet<UUID>(); selected.add(player.getUUID()); allocation.debits().forEach(d -> selected.add(d.owner()));
        for (UUID id : selected) {
            var before = inventories.get(id); var after = NonNullList.withSize(before.size(), ItemStack.EMPTY);
            for (int i = 0; i < before.size(); i++) after.set(i, before.get(i).copy());
            var debits = new CompoundTag();
            for (var debit : allocation.debits()) if (debit.owner().equals(id)) {
                var requirement = recipe.requirements().stream().filter(r -> r.key().equals(debit.ingredient())).findFirst().orElseThrow();
                int remaining = debit.count();
                for (int slot = 0; slot < after.size() && remaining > 0; slot++) {
                    var stack = after.get(slot); if (stack.isEmpty() || !requirement.matches().test(stack)) continue;
                    int count = Math.min(remaining, stack.getCount()); var row = new CompoundTag();
                    row.putString("item", BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
                    row.putString("ingredient", debit.ingredient()); row.putInt("count", count); debits.put(Integer.toString(slot), row);
                    stack.shrink(count); remaining -= count;
                }
                if (remaining != 0) throw new IllegalStateException("Live ingredient allocation differs");
            }
            if (id.equals(player.getUUID()) && !recipe.cursor() && !insert(after, produced)) return null;
            var owner = new CompoundTag(); owner.put("before", ChopTravelRecovery.encode(player, before));
            owner.put("after", ChopTravelRecovery.encode(player, after)); owner.put("debits", debits); owner.putBoolean("ack", false);
            owners.put(id.toString(), owner);
        }
        var image = new CompoundTag(); image.putInt("schema", 1); image.putString("id", UUID.randomUUID().toString());
        image.putString("recipient", player.getStringUUID()); image.putString("recipient_name", player.getGameProfile().name());
        image.putLong("run", expected); image.putLong("sequence", Math.addExact(DungeonMetricsService.resourceSequence(server, expected), 1));
        image.putLong("at", System.currentTimeMillis()); image.putLong("donation_before", cursor); image.putLong("donation_after", allocation.sequence());
        image.putString("recipe", recipe.id()); image.putString("skill", recipe.skill()); image.put("owners", owners);
        allocation.donatedAfter().forEach((owner, rank) -> donorRanks.putLong(owner.toString(), rank)); image.put("donated", donorRanks);
        var output = NonNullList.withSize(player.getInventory().getContainerSize(), ItemStack.EMPTY); output.set(0, produced);
        image.put("output", ChopTravelRecovery.encode(player, output)); image.putBoolean("committed", false); image.putBoolean("metrics_ack", false);
        if (recipe.fuel() != null) { image.putInt("schema", 2); image.put("fuel", recipe.fuel()); }
        if (recipe.cursor()) {
            var result = PotionOutputPlacement.cursor(player.inventoryMenu.getCarried(), produced);
            if (result == null) return null;
            image.putInt("schema", 3);
            image.put("cursor_before", net.goui.cosmicdungeon.playerclass.dragoon.repair.RepairCustody.encode(player, player.inventoryMenu.getCarried()));
            image.put("cursor_after", net.goui.cosmicdungeon.playerclass.dragoon.repair.RepairCustody.encode(player, result));
        }
        var plan = new SharedCraftPlan(image); SharedCraftTransactions.validateImages(player, plan); return plan;
    }
    /** Apply a detached plan only after full capacity is known; failed partial merges change nothing. */
    static boolean insert(NonNullList<ItemStack> inventory, ItemStack output) {
        var result = PotionOutputPlacement.inventoryStack(inventory, output);
        if (result == null) return false;
        for (int i = 0; i < inventory.size(); i++) inventory.set(i, result.get(i));
        return true;
    }
}
