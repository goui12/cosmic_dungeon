package net.minecraft.world.item.crafting;

import com.google.common.annotations.VisibleForTesting;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.RecipeDisplayEntry;
import net.minecraft.world.item.crafting.display.RecipeDisplayId;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;

public class RecipeManager extends SimplePreparableReloadListener<RecipeMap> implements RecipeAccess {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<ResourceKey<RecipePropertySet>, RecipeManager.IngredientExtractor> RECIPE_PROPERTY_SETS = Map.of(
        RecipePropertySet.SMITHING_ADDITION,
        p_380841_ -> p_380841_ instanceof SmithingRecipe smithingrecipe ? smithingrecipe.additionIngredient() : Optional.empty(),
        RecipePropertySet.SMITHING_BASE,
        p_399417_ -> p_399417_ instanceof SmithingRecipe smithingrecipe ? Optional.of(smithingrecipe.baseIngredient()) : Optional.empty(),
        RecipePropertySet.SMITHING_TEMPLATE,
        p_380847_ -> p_380847_ instanceof SmithingRecipe smithingrecipe ? smithingrecipe.templateIngredient() : Optional.empty(),
        RecipePropertySet.FURNACE_INPUT,
        forSingleInput(RecipeType.SMELTING),
        RecipePropertySet.BLAST_FURNACE_INPUT,
        forSingleInput(RecipeType.BLASTING),
        RecipePropertySet.SMOKER_INPUT,
        forSingleInput(RecipeType.SMOKING),
        RecipePropertySet.CAMPFIRE_INPUT,
        forSingleInput(RecipeType.CAMPFIRE_COOKING)
    );
    private static final FileToIdConverter RECIPE_LISTER = FileToIdConverter.registry(Registries.RECIPE);
    private final HolderLookup.Provider registries;
    private RecipeMap recipes = RecipeMap.EMPTY;
    private Map<ResourceKey<RecipePropertySet>, RecipePropertySet> propertySets = Map.of();
    private SelectableRecipe.SingleInputSet<StonecutterRecipe> stonecutterRecipes = SelectableRecipe.SingleInputSet.empty();
    private List<RecipeManager.ServerDisplayInfo> allDisplays = List.of();
    private Map<ResourceKey<Recipe<?>>, List<RecipeManager.ServerDisplayInfo>> recipeToDisplay = Map.of();
    private Object2IntMap<ResourceKey<net.minecraft.world.item.crafting.Recipe<?>>> recipePriorities = it.unimi.dsi.fastutil.objects.Object2IntMaps.emptyMap(); // Neo: recipe priorities set from RecipePriorityManager.

    public RecipeManager(HolderLookup.Provider registries) {
        this.registries = registries;
    }

    /**
     * Performs any reloading that can be done off-thread, such as file IO
     */
    protected RecipeMap prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
        SortedMap<ResourceLocation, Recipe<?>> sortedmap = new TreeMap<>();
        SimpleJsonResourceReloadListener.scanDirectory(
            resourceManager, RECIPE_LISTER, new net.neoforged.neoforge.common.conditions.ConditionalOps<>(this.registries.createSerializationContext(JsonOps.INSTANCE), getContext()), Recipe.CODEC, sortedmap // Neo: add condition context
        );
        List<RecipeHolder<?>> list = new ArrayList<>(sortedmap.size());
        sortedmap.forEach((p_379232_, p_379233_) -> {
            ResourceKey<Recipe<?>> resourcekey = ResourceKey.create(Registries.RECIPE, p_379232_);
            RecipeHolder<?> recipeholder = new RecipeHolder<>(resourcekey, p_379233_);
            list.add(recipeholder);
        });
        return RecipeMap.create(list);
    }

    protected void apply(RecipeMap object, ResourceManager resourceManager, ProfilerFiller profiler) {
        this.recipes = object;
        LOGGER.info("Loaded {} recipes", object.values().size());
    }

    public void finalizeRecipeLoading(FeatureFlagSet enabledFeatures) {
        this.recipes.order(this.recipePriorities); // Neo: order recipes by priority.
        List<SelectableRecipe.SingleInputEntry<StonecutterRecipe>> list = new ArrayList<>();
        List<RecipeManager.IngredientCollector> list1 = RECIPE_PROPERTY_SETS.entrySet()
            .stream()
            .map(p_380840_ -> new RecipeManager.IngredientCollector(p_380840_.getKey(), p_380840_.getValue()))
            .toList();
        this.recipes
            .values()
            .forEach(
                p_380845_ -> {
                    Recipe<?> recipe = p_380845_.value();
                    if (!recipe.isSpecial() && recipe.placementInfo().isImpossibleToPlace()) {
                        LOGGER.warn("Recipe {} can't be placed due to empty ingredients and will be ignored", p_380845_.id().location());
                    } else {
                        list1.forEach(p_380839_ -> p_380839_.accept(recipe));
                        if (recipe instanceof StonecutterRecipe stonecutterrecipe
                            && isIngredientEnabled(enabledFeatures, stonecutterrecipe.input())
                            && stonecutterrecipe.resultDisplay().isEnabled(enabledFeatures)) {
                            list.add(
                                new SelectableRecipe.SingleInputEntry<>(
                                    stonecutterrecipe.input(),
                                    new SelectableRecipe<>(stonecutterrecipe.resultDisplay(), Optional.of((RecipeHolder<StonecutterRecipe>)p_380845_))
                                )
                            );
                        }
                    }
                }
            );
        this.propertySets = list1.stream().collect(Collectors.toUnmodifiableMap(p_380846_ -> p_380846_.key, p_380852_ -> p_380852_.asPropertySet(enabledFeatures)));
        this.stonecutterRecipes = new SelectableRecipe.SingleInputSet<>(list);
        this.allDisplays = unpackRecipeInfo(this.recipes.values(), enabledFeatures);
        this.recipeToDisplay = this.allDisplays
            .stream()
            .collect(Collectors.groupingBy(p_379212_ -> p_379212_.parent.id(), IdentityHashMap::new, Collectors.toList()));
    }

    static List<Ingredient> filterDisabled(FeatureFlagSet enabledFeatures, List<Ingredient> ingredients) {
        ingredients.removeIf(p_379230_ -> !isIngredientEnabled(enabledFeatures, p_379230_));
        return ingredients;
    }

    private static boolean isIngredientEnabled(FeatureFlagSet enabledFeatures, Ingredient ingredient) {
        return ingredient.items().allMatch(p_379224_ -> p_379224_.value().isEnabled(enabledFeatures));
    }

    public <I extends RecipeInput, T extends Recipe<I>> Optional<RecipeHolder<T>> getRecipeFor(
        RecipeType<T> recipeType, I input, Level level, @Nullable ResourceKey<Recipe<?>> recipe
    ) {
        RecipeHolder<T> recipeholder = recipe != null ? this.byKeyTyped(recipeType, recipe) : null;
        return this.getRecipeFor(recipeType, input, level, recipeholder);
    }

    public <I extends RecipeInput, T extends Recipe<I>> Optional<RecipeHolder<T>> getRecipeFor(
        RecipeType<T> recipeType, I input, Level level, @Nullable RecipeHolder<T> lastRecipe
    ) {
        return lastRecipe != null && lastRecipe.value().matches(input, level)
            ? Optional.of(lastRecipe)
            : this.getRecipeFor(recipeType, input, level);
    }

    public <I extends RecipeInput, T extends Recipe<I>> Optional<RecipeHolder<T>> getRecipeFor(RecipeType<T> recipeType, I input, Level level) {
        return this.recipes.getRecipesFor(recipeType, input, level).findFirst();
    }

    public Optional<RecipeHolder<?>> byKey(ResourceKey<Recipe<?>> key) {
        return Optional.ofNullable(this.recipes.byKey(key));
    }

    @Nullable
    private <T extends Recipe<?>> RecipeHolder<T> byKeyTyped(RecipeType<T> type, ResourceKey<Recipe<?>> key) {
        RecipeHolder<?> recipeholder = this.recipes.byKey(key);
        return (RecipeHolder<T>)(recipeholder != null && recipeholder.value().getType().equals(type) ? recipeholder : null);
    }

    public Map<ResourceKey<RecipePropertySet>, RecipePropertySet> getSynchronizedItemProperties() {
        return this.propertySets;
    }

    public SelectableRecipe.SingleInputSet<StonecutterRecipe> getSynchronizedStonecutterRecipes() {
        return this.stonecutterRecipes;
    }

    @Override
    public RecipePropertySet propertySet(ResourceKey<RecipePropertySet> propertySet) {
        return this.propertySets.getOrDefault(propertySet, RecipePropertySet.EMPTY);
    }

    @Override
    public SelectableRecipe.SingleInputSet<StonecutterRecipe> stonecutterRecipes() {
        return this.stonecutterRecipes;
    }

    public Collection<RecipeHolder<?>> getRecipes() {
        return this.recipes.values();
    }

    @Nullable
    public RecipeManager.ServerDisplayInfo getRecipeFromDisplay(RecipeDisplayId display) {
        int i = display.index();
        return i >= 0 && i < this.allDisplays.size() ? this.allDisplays.get(i) : null;
    }

    public void listDisplaysForRecipe(ResourceKey<Recipe<?>> recipe, Consumer<RecipeDisplayEntry> output) {
        List<RecipeManager.ServerDisplayInfo> list = this.recipeToDisplay.get(recipe);
        if (list != null) {
            list.forEach(p_379228_ -> output.accept(p_379228_.display));
        }
    }

    @VisibleForTesting
    protected static RecipeHolder<?> fromJson(ResourceKey<Recipe<?>> p_recipe, JsonObject json, HolderLookup.Provider registries) {
        Recipe<?> recipe = Recipe.CODEC.parse(registries.createSerializationContext(JsonOps.INSTANCE), json).getOrThrow(JsonParseException::new);
        return new RecipeHolder<>(p_recipe, recipe);
    }

    public static <I extends RecipeInput, T extends Recipe<I>> RecipeManager.CachedCheck<I, T> createCheck(final RecipeType<T> recipeType) {
        return new RecipeManager.CachedCheck<I, T>() {
            @Nullable
            private ResourceKey<Recipe<?>> lastRecipe;

            @Override
            public Optional<RecipeHolder<T>> getRecipeFor(I p_344742_, ServerLevel p_379891_) {
                RecipeManager recipemanager = p_379891_.recipeAccess();
                Optional<RecipeHolder<T>> optional = recipemanager.getRecipeFor(recipeType, p_344742_, p_379891_, this.lastRecipe);
                if (optional.isPresent()) {
                    RecipeHolder<T> recipeholder = optional.get();
                    this.lastRecipe = recipeholder.id();
                    return Optional.of(recipeholder);
                } else {
                    return Optional.empty();
                }
            }
        };
    }

    private static List<RecipeManager.ServerDisplayInfo> unpackRecipeInfo(Iterable<RecipeHolder<?>> recipes, FeatureFlagSet enabledFeatures) {
        List<RecipeManager.ServerDisplayInfo> list = new ArrayList<>();
        Object2IntMap<String> object2intmap = new Object2IntOpenHashMap<>();

        for (RecipeHolder<?> recipeholder : recipes) {
            Recipe<?> recipe = recipeholder.value();
            OptionalInt optionalint;
            if (recipe.group().isEmpty()) {
                optionalint = OptionalInt.empty();
            } else {
                optionalint = OptionalInt.of(object2intmap.computeIfAbsent(recipe.group(), p_379226_ -> object2intmap.size()));
            }

            Optional<List<Ingredient>> optional;
            if (recipe.isSpecial()) {
                optional = Optional.empty();
            } else {
                optional = Optional.of(recipe.placementInfo().ingredients());
            }

            for (RecipeDisplay recipedisplay : recipe.display()) {
                if (recipedisplay.isEnabled(enabledFeatures)) {
                    int i = list.size();
                    RecipeDisplayId recipedisplayid = new RecipeDisplayId(i);
                    RecipeDisplayEntry recipedisplayentry = new RecipeDisplayEntry(
                        recipedisplayid, recipedisplay, optionalint, recipe.recipeBookCategory(), optional
                    );
                    list.add(new RecipeManager.ServerDisplayInfo(recipedisplayentry, recipeholder));
                }
            }
        }

        return list;
    }

    private static RecipeManager.IngredientExtractor forSingleInput(RecipeType<? extends SingleItemRecipe> recipeType) {
        return p_380850_ -> p_380850_.getType() == recipeType && p_380850_ instanceof SingleItemRecipe singleitemrecipe
            ? Optional.of(singleitemrecipe.input())
            : Optional.empty();
    }

    // Neo: expose recipe map
    public RecipeMap recipeMap() {
        return this.recipes;
    }

    // Neo: set recipe priority map
    public void setPriorityMap(Object2IntMap<ResourceKey<net.minecraft.world.item.crafting.Recipe<?>>> recipePriorities) {
        this.recipePriorities = recipePriorities;
    }

    public interface CachedCheck<I extends RecipeInput, T extends Recipe<I>> {
        Optional<RecipeHolder<T>> getRecipeFor(I input, ServerLevel level);
    }

    public static class IngredientCollector implements Consumer<Recipe<?>> {
        final ResourceKey<RecipePropertySet> key;
        private final RecipeManager.IngredientExtractor extractor;
        private final List<Ingredient> ingredients = new ArrayList<>();

        protected IngredientCollector(ResourceKey<RecipePropertySet> key, RecipeManager.IngredientExtractor extractor) {
            this.key = key;
            this.extractor = extractor;
        }

        public void accept(Recipe<?> recipe) {
            this.extractor.apply(recipe).ifPresent(this.ingredients::add);
        }

        public RecipePropertySet asPropertySet(FeatureFlagSet enabledFeatures) {
            return RecipePropertySet.create(RecipeManager.filterDisabled(enabledFeatures, this.ingredients));
        }
    }

    @FunctionalInterface
    public interface IngredientExtractor {
        Optional<Ingredient> apply(Recipe<?> recipe);
    }

    public record ServerDisplayInfo(RecipeDisplayEntry display, RecipeHolder<?> parent) {
    }
}
