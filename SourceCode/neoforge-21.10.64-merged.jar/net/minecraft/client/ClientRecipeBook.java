package net.minecraft.client;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Table;
import com.google.common.collect.ImmutableList.Builder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.Set;
import net.minecraft.client.gui.screens.recipebook.RecipeCollection;
import net.minecraft.client.gui.screens.recipebook.SearchRecipeBookCategory;
import net.minecraft.stats.RecipeBook;
import net.minecraft.world.item.crafting.ExtendedRecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.display.RecipeDisplayEntry;
import net.minecraft.world.item.crafting.display.RecipeDisplayId;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ClientRecipeBook extends RecipeBook {
    private final Map<RecipeDisplayId, RecipeDisplayEntry> known = new HashMap<>();
    private final Set<RecipeDisplayId> highlight = new HashSet<>();
    private Map<ExtendedRecipeBookCategory, List<RecipeCollection>> collectionsByTab = Map.of();
    private List<RecipeCollection> allCollections = List.of();

    public void add(RecipeDisplayEntry recipe) {
        this.known.put(recipe.id(), recipe);
    }

    public void remove(RecipeDisplayId recipe) {
        this.known.remove(recipe);
        this.highlight.remove(recipe);
    }

    public void clear() {
        this.known.clear();
        this.highlight.clear();
    }

    public boolean willHighlight(RecipeDisplayId recipe) {
        return this.highlight.contains(recipe);
    }

    public void removeHighlight(RecipeDisplayId recipe) {
        this.highlight.remove(recipe);
    }

    public void addHighlight(RecipeDisplayId recipe) {
        this.highlight.add(recipe);
    }

    public void rebuildCollections() {
        Map<RecipeBookCategory, List<List<RecipeDisplayEntry>>> map = categorizeAndGroupRecipes(this.known.values());
        Map<ExtendedRecipeBookCategory, List<RecipeCollection>> map1 = new HashMap<>();
        Builder<RecipeCollection> builder = ImmutableList.builder();
        map.forEach(
            (p_380933_, p_378767_) -> map1.put(
                p_380933_, p_378767_.stream().map(RecipeCollection::new).peek(builder::add).collect(ImmutableList.toImmutableList())
            )
        );

        for (SearchRecipeBookCategory searchrecipebookcategory : SearchRecipeBookCategory.values()) {
            map1.put(
                searchrecipebookcategory,
                searchrecipebookcategory.includedCategories()
                    .stream()
                    .flatMap(p_380954_ -> map1.getOrDefault(p_380954_, List.of()).stream())
                    .collect(ImmutableList.toImmutableList())
            );
        }
        // Neo: Do the same for modded search categories.
        for (var entry : net.neoforged.neoforge.client.RecipeBookManager.getSearchCategories().entrySet()) {
            map1.put(
                entry.getKey(),
                entry.getValue()
                    .stream()
                    .flatMap(category -> map1.getOrDefault(category, List.of()).stream())
                    .collect(ImmutableList.toImmutableList())
            );
        }

        this.collectionsByTab = Map.copyOf(map1);
        this.allCollections = builder.build();
    }

    private static Map<RecipeBookCategory, List<List<RecipeDisplayEntry>>> categorizeAndGroupRecipes(Iterable<RecipeDisplayEntry> recipes) {
        Map<RecipeBookCategory, List<List<RecipeDisplayEntry>>> map = new HashMap<>();
        Table<RecipeBookCategory, Integer, List<RecipeDisplayEntry>> table = HashBasedTable.create();

        for (RecipeDisplayEntry recipedisplayentry : recipes) {
            RecipeBookCategory recipebookcategory = recipedisplayentry.category();
            OptionalInt optionalint = recipedisplayentry.group();
            if (optionalint.isEmpty()) {
                map.computeIfAbsent(recipebookcategory, p_381000_ -> new ArrayList<>()).add(List.of(recipedisplayentry));
            } else {
                List<RecipeDisplayEntry> list = table.get(recipebookcategory, optionalint.getAsInt());
                if (list == null) {
                    list = new ArrayList<>();
                    table.put(recipebookcategory, optionalint.getAsInt(), list);
                    map.computeIfAbsent(recipebookcategory, p_380998_ -> new ArrayList<>()).add(list);
                }

                list.add(recipedisplayentry);
            }
        }

        return map;
    }

    public List<RecipeCollection> getCollections() {
        return this.allCollections;
    }

    public List<RecipeCollection> getCollection(ExtendedRecipeBookCategory category) {
        return this.collectionsByTab.getOrDefault(category, Collections.emptyList());
    }
}
