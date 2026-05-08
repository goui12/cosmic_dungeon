package net.minecraft.client.multiplayer;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.ClientRecipeBook;
import net.minecraft.client.gui.screens.recipebook.RecipeCollection;
import net.minecraft.client.searchtree.FullTextSearchTree;
import net.minecraft.client.searchtree.IdSearchTree;
import net.minecraft.client.searchtree.SearchTree;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.crafting.display.RecipeDisplayEntry;
import net.minecraft.world.item.crafting.display.SlotDisplayContext;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SessionSearchTrees {
    private static final SessionSearchTrees.Key RECIPE_COLLECTIONS = new SessionSearchTrees.Key();
    public static final SessionSearchTrees.Key CREATIVE_NAMES = new SessionSearchTrees.Key();
    public static final SessionSearchTrees.Key CREATIVE_TAGS = new SessionSearchTrees.Key();
    private CompletableFuture<SearchTree<ItemStack>> creativeByNameSearch = CompletableFuture.completedFuture(SearchTree.empty());
    private CompletableFuture<SearchTree<ItemStack>> creativeByTagSearch = CompletableFuture.completedFuture(SearchTree.empty());
    private CompletableFuture<SearchTree<RecipeCollection>> recipeSearch = CompletableFuture.completedFuture(SearchTree.empty());
    private final Map<SessionSearchTrees.Key, Runnable> reloaders = new IdentityHashMap<>();

    private void register(SessionSearchTrees.Key key, Runnable reloader) {
        reloader.run();
        this.reloaders.put(key, reloader);
    }

    public void rebuildAfterLanguageChange() {
        for (Runnable runnable : this.reloaders.values()) {
            runnable.run();
        }
    }

    private static Stream<String> getTooltipLines(Stream<ItemStack> items, Item.TooltipContext context, TooltipFlag tooltipFlag) {
        return items.<Component>flatMap(p_344980_ -> p_344980_.getTooltipLines(context, null, tooltipFlag).stream())
            .map(p_345615_ -> ChatFormatting.stripFormatting(p_345615_.getString()).trim())
            .filter(p_346341_ -> !p_346341_.isEmpty());
    }

    public void updateRecipes(ClientRecipeBook recipeBook, Level level) {
        this.register(
            RECIPE_COLLECTIONS,
            () -> {
                List<RecipeCollection> list = recipeBook.getCollections();
                RegistryAccess registryaccess = level.registryAccess();
                Registry<Item> registry = registryaccess.lookupOrThrow(Registries.ITEM);
                Item.TooltipContext item$tooltipcontext = Item.TooltipContext.of(registryaccess);
                ContextMap contextmap = SlotDisplayContext.fromLevel(level);
                TooltipFlag tooltipflag = net.neoforged.neoforge.client.ClientTooltipFlag.of(TooltipFlag.Default.NORMAL);
                CompletableFuture<?> completablefuture = this.recipeSearch;
                this.recipeSearch = CompletableFuture.supplyAsync(
                    () -> new FullTextSearchTree<>(
                        p_378821_ -> getTooltipLines(
                            p_378821_.getRecipes().stream().flatMap(p_380802_ -> p_380802_.resultItems(contextmap).stream()), item$tooltipcontext, tooltipflag
                        ),
                        p_378813_ -> p_378813_.getRecipes()
                            .stream()
                            .flatMap(p_380804_ -> p_380804_.resultItems(contextmap).stream())
                            .map(p_378810_ -> registry.getKey(p_378810_.getItem())),
                        list
                    ),
                    Util.backgroundExecutor()
                );
                completablefuture.cancel(true);
            }
        );
    }

    public SearchTree<RecipeCollection> recipes() {
        return this.recipeSearch.join();
    }

    public void updateCreativeTags(List<ItemStack> items) {
        this.updateCreativeTags(items, CREATIVE_TAGS);
    }

    public void updateCreativeTags(List<ItemStack> items, SessionSearchTrees.Key key) {
        this.register(
            key,
            () -> {
                CompletableFuture<?> completablefuture = net.neoforged.neoforge.client.CreativeModeTabSearchRegistry.getTagSearchTree(key);
                net.neoforged.neoforge.client.CreativeModeTabSearchRegistry.putTagSearchTree(key, CompletableFuture.supplyAsync(
                    () -> new IdSearchTree<>(p_344728_ -> p_344728_.getTags().map(TagKey::location), items), Util.backgroundExecutor()
                ));
                completablefuture.cancel(true);
            }
        );
    }

    public SearchTree<ItemStack> creativeTagSearch() {
        return this.creativeTagSearch(CREATIVE_TAGS);
    }

    public SearchTree<ItemStack> creativeTagSearch(SessionSearchTrees.Key key) {
        return net.neoforged.neoforge.client.CreativeModeTabSearchRegistry.getTagSearchTree(key).join();
    }

    public void updateCreativeTooltips(HolderLookup.Provider registries, List<ItemStack> items) {
        this.updateCreativeTooltips(registries, items, CREATIVE_NAMES);
    }

    public void updateCreativeTooltips(HolderLookup.Provider registries, List<ItemStack> items, SessionSearchTrees.Key key) {
        this.register(
            key,
            () -> {
                Item.TooltipContext item$tooltipcontext = Item.TooltipContext.of(registries);
                TooltipFlag tooltipflag = net.neoforged.neoforge.client.ClientTooltipFlag.of(TooltipFlag.Default.NORMAL.asCreative());
                CompletableFuture<?> completablefuture = net.neoforged.neoforge.client.CreativeModeTabSearchRegistry.getNameSearchTree(key);
                net.neoforged.neoforge.client.CreativeModeTabSearchRegistry.putNameSearchTree(key, CompletableFuture.supplyAsync(
                    () -> new FullTextSearchTree<>(
                        p_345006_ -> getTooltipLines(Stream.of(p_345006_), item$tooltipcontext, tooltipflag),
                        p_345861_ -> p_345861_.getItemHolder().unwrapKey().map(ResourceKey::location).stream(),
                        items
                    ),
                    Util.backgroundExecutor()
                ));
                completablefuture.cancel(true);
            }
        );
    }

    public SearchTree<ItemStack> creativeNameSearch() {
        return this.creativeNameSearch(CREATIVE_NAMES);
    }

    public SearchTree<ItemStack> creativeNameSearch(SessionSearchTrees.Key key) {
        return net.neoforged.neoforge.client.CreativeModeTabSearchRegistry.getNameSearchTree(key).join();
    }

    @OnlyIn(Dist.CLIENT)
    public static class Key {
    }
}
