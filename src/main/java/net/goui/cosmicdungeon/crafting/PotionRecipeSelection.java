package net.goui.cosmicdungeon.crafting;

import java.util.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.PotionContents;

/** Precomputed distinct costs; preview simulates depletion and fuel instead of adding overlapping capacities. */
public final class PotionRecipeSelection {
    public record Option(PotionRecipeCatalog.Route route, Map<String, Integer> materials) {
        public Option { materials = Map.copyOf(materials); }
    }
    public record Choice(Option option, BrewingFuel.Cost fuel, Map<String, Integer> requirements) {
        public Choice { requirements = Map.copyOf(requirements); }
    }
    private static final String POWDER = itemKey(Items.BLAZE_POWDER);
    private final Map<String, List<Option>> options;
    public PotionRecipeSelection(PotionRecipeCatalog catalog) {
        var all = new HashMap<String, List<Option>>();
        for (var target : catalog.targets()) {
            var distinct = new LinkedHashMap<Map<String, Integer>, Option>();
            for (var route : catalog.routes(target)) {
                var materials = new TreeMap<String, Integer>(); materials.put(inputKey(route), 1);
                for (var item : route.ingredients()) materials.merge(itemKey(item), 1, Integer::sum);
                var option = new Option(route, materials); distinct.putIfAbsent(option.materials(), option);
            }
            all.put(target.id(), List.copyOf(distinct.values()));
        }
        options = Map.copyOf(all);
    }
    public static String itemKey(Item item) { return "item/" + BuiltInRegistries.ITEM.getKey(item); }
    public static String inputKey(PotionRecipeCatalog.Route route) {
        return "potion/" + BuiltInRegistries.ITEM.getKey(route.inputContainer()) + "/" + BuiltInRegistries.POTION.getKey(route.inputPotion().value());
    }
    public static boolean matchesInput(PotionRecipeCatalog.Route route, ItemStack stack) {
        return stack.is(route.inputContainer()) && new PotionContents(route.inputPotion()).equals(stack.get(DataComponents.POTION_CONTENTS));
    }
    public static Map<String, Integer> stock(Iterable<ItemStack> stacks) {
        var result = new HashMap<String, Integer>();
        for (var stack : stacks) {
            if (stack.isEmpty()) continue;
            String key = itemKey(stack.getItem());
            if (stack.is(Items.POTION) || stack.is(Items.SPLASH_POTION) || stack.is(Items.LINGERING_POTION)) {
                var contents = stack.get(DataComponents.POTION_CONTENTS);
                if (contents == null || contents.potion().isEmpty() || !contents.equals(new PotionContents(contents.potion().orElseThrow()))) continue;
                key = "potion/" + BuiltInRegistries.ITEM.getKey(stack.getItem()) + "/" + BuiltInRegistries.POTION.getKey(contents.potion().orElseThrow().value());
            }
            result.merge(key, Math.min(4096, stack.getCount()), (a, b) -> Math.min(4096, a + b));
        }
        return Map.copyOf(result);
    }
    public static Map<String, Integer> combine(Collection<Map<String, Integer>> stocks) {
        var result = new HashMap<String, Integer>();
        for (var stock : stocks) stock.forEach((key, amount) -> result.merge(key, amount, (a, b) -> Math.min(4096, a + b)));
        return result;
    }
    public Choice choose(String target, Map<String, Integer> stock, int charges) {
        if (CraftingWork.probe != null) CraftingWork.probe.plannerChoices++;
        for (var option : options.getOrDefault(target, List.of())) {
            var cost = BrewingFuel.cost(charges, option.route().steps()); boolean fits = true;
            for (var entry : option.materials().entrySet()) {
                int needed = entry.getValue() + (entry.getKey().equals(POWDER) ? cost.powders() : 0);
                if (stock.getOrDefault(entry.getKey(), 0) < needed) { fits = false; break; }
            }
            if (!fits || !option.materials().containsKey(POWDER) && stock.getOrDefault(POWDER, 0) < cost.powders()) continue;
            var requirements = new TreeMap<>(option.materials());
            if (cost.powders() > 0) requirements.merge(POWDER, cost.powders(), Integer::sum);
            return new Choice(option, cost, requirements);
        }
        return null;
    }
    public int capacity(String target, Map<String, Integer> stock, int charges) {
        var remaining = new HashMap<>(stock); int count = 0;
        while (count < 64) {
            var choice = choose(target, remaining, charges); if (choice == null) break;
            choice.requirements().forEach((key, amount) -> remaining.compute(key, (ignored, old) -> old - amount));
            charges = choice.fuel().remaining(); count++;
        }
        return count;
    }
}
