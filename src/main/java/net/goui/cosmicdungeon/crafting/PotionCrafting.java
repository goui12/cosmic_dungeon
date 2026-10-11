package net.goui.cosmicdungeon.crafting;

import java.util.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.PotionContents;

/** Server-only selection/transaction facade for the later inventory palette. No client-controlled recipes. */
public final class PotionCrafting {
    public record Offer(int own, int shared, BrewingFuel.View fuel, int steps, int powders) {}
    private record Catalog(FeatureFlagSet flags, PotionRecipeCatalog recipes, PotionRecipeSelection selection) {}
    private record Stock(Map<String, Integer> own, Map<String, Integer> pooled) {}
    private static final Map<MinecraftServer, Catalog> CATALOGS = new WeakHashMap<>();
    private PotionCrafting() {}
    private static Catalog data(ServerPlayer player) {
        var server = player.level().getServer(); var flags = player.level().enabledFeatures(); var old = CATALOGS.get(server);
        if (old == null || !old.flags().equals(flags)) {
            var recipes = PotionRecipeCatalog.vanilla(flags); old = new Catalog(flags, recipes, new PotionRecipeSelection(recipes)); CATALOGS.put(server, old);
        }
        return old;
    }
    public static PotionRecipeCatalog catalog(ServerPlayer player) { return data(player).recipes(); }
    public static BrewingFuel.View fuel(ServerPlayer player) {
        var state = SharedCraftData.get(player.level().getServer()).fuel(player.getUUID());
        return new BrewingFuel.View(BrewingFuel.charges(state), BrewingFuel.PER_POWDER);
    }
    private static Stock stock(ServerPlayer player, net.goui.cosmicdungeon.dungeon.DungeonRunRegistryData.RunRecord run, boolean fresh) {
        var stocks = new ArrayList<Map<String, Integer>>();
        for (var owner : SharedCrafting.participants(player, run))
            stocks.add(PotionRecipeSelection.stock(SharedCrafting.inventory(owner, fresh)));
        return new Stock(stocks.getFirst(), PotionRecipeSelection.combine(stocks));
    }
    public static Offer quote(ServerPlayer player, long expected, String targetId) {
        var catalog = data(player); var target = catalog.recipes().target(targetId); var fuel = fuel(player);
        var run = target == null ? null : SharedCrafting.run(player, expected, target.classes());
        if (run == null || !SharedCrafting.availableOwner(player, false) || SharedCraftData.get(player.level().getServer()).pendingRun(expected) != null)
            return new Offer(0, 0, fuel, 0, 0);
        var stock = stock(player, run, false); var planner = catalog.selection();
        var choice = planner.choose(targetId, stock.own(), fuel.charges());
        if (choice == null) choice = planner.choose(targetId, stock.pooled(), fuel.charges());
        return new Offer(planner.capacity(targetId, stock.own(), fuel.charges()), planner.capacity(targetId, stock.pooled(), fuel.charges()),
                fuel, choice == null ? 0 : choice.fuel().steps(), choice == null ? 0 : choice.fuel().powders());
    }
    private static SharedCrafting.Recipe recipe(ServerPlayer player, long expected, String targetId) {
        var catalog = data(player); var target = catalog.recipes().target(targetId);
        var run = target == null ? null : SharedCrafting.run(player, expected, target.classes());
        var journal = SharedCraftData.get(player.level().getServer());
        if (run == null || !SharedCrafting.availableOwner(player, false) || journal.pendingRun(expected) != null) return null;
        var fuel = journal.fuel(player.getUUID()); var stock = stock(player, run, true);
        var choice = catalog.selection().choose(targetId, stock.own(), BrewingFuel.charges(fuel));
        if (choice == null) choice = catalog.selection().choose(targetId, stock.pooled(), BrewingFuel.charges(fuel));
        if (choice == null) return null;
        var route = choice.option().route(); var requirements = new ArrayList<SharedCrafting.Requirement>();
        requirements.add(new SharedCrafting.Requirement(PotionRecipeSelection.inputKey(route), 1, stack -> PotionRecipeSelection.matchesInput(route, stack)));
        var ingredients = new LinkedHashMap<Item, Integer>();
        route.ingredients().forEach(item -> ingredients.merge(item, 1, Integer::sum));
        if (choice.fuel().powders() > 0) ingredients.merge(Items.BLAZE_POWDER, choice.fuel().powders(), Integer::sum);
        ingredients.forEach((item, count) -> requirements.add(new SharedCrafting.Requirement(PotionRecipeSelection.itemKey(item), count, stack -> stack.is(item))));
        var output = PotionContents.createItemStack(target.container(), target.potion());
        return new SharedCrafting.Recipe(target.id(), output.getHoverName().getString(), target.classes(), requirements, output, BrewingFuel.plan(fuel, route.steps()));
    }
    static SharedCraftPlan prepare(ServerPlayer player, long expected, String targetId) {
        var recipe = recipe(player, expected, targetId); return recipe == null ? null : SharedCrafting.prepare(player, expected, recipe);
    }
    public static boolean craft(ServerPlayer player, long expected, String targetId) {
        var recipe = recipe(player, expected, targetId); return recipe != null && SharedCrafting.craft(player, expected, recipe);
    }
    public static void stopped() { CATALOGS.clear(); }
}
