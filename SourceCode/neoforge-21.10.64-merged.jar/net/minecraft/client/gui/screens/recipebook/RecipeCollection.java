package net.minecraft.client.gui.screens.recipebook;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.world.entity.player.StackedItemContents;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.RecipeDisplayEntry;
import net.minecraft.world.item.crafting.display.RecipeDisplayId;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class RecipeCollection {
    public static final RecipeCollection EMPTY = new RecipeCollection(List.of());
    private final List<RecipeDisplayEntry> entries;
    private final Set<RecipeDisplayId> craftable = new HashSet<>();
    private final Set<RecipeDisplayId> selected = new HashSet<>();

    public RecipeCollection(List<RecipeDisplayEntry> entries) {
        this.entries = entries;
    }

    public void selectRecipes(StackedItemContents stackedItemContents, Predicate<RecipeDisplay> filter) {
        for (RecipeDisplayEntry recipedisplayentry : this.entries) {
            boolean flag = filter.test(recipedisplayentry.display());
            if (flag) {
                this.selected.add(recipedisplayentry.id());
            } else {
                this.selected.remove(recipedisplayentry.id());
            }

            if (flag && recipedisplayentry.canCraft(stackedItemContents)) {
                this.craftable.add(recipedisplayentry.id());
            } else {
                this.craftable.remove(recipedisplayentry.id());
            }
        }
    }

    public boolean isCraftable(RecipeDisplayId recipe) {
        return this.craftable.contains(recipe);
    }

    public boolean hasCraftable() {
        return !this.craftable.isEmpty();
    }

    public boolean hasAnySelected() {
        return !this.selected.isEmpty();
    }

    public List<RecipeDisplayEntry> getRecipes() {
        return this.entries;
    }

    public List<RecipeDisplayEntry> getSelectedRecipes(RecipeCollection.CraftableStatus craftableStatus) {
        Predicate<RecipeDisplayId> predicate = switch (craftableStatus) {
            case ANY -> this.selected::contains;
            case CRAFTABLE -> this.craftable::contains;
            case NOT_CRAFTABLE -> p_378796_ -> this.selected.contains(p_378796_) && !this.craftable.contains(p_378796_);
        };
        List<RecipeDisplayEntry> list = new ArrayList<>();

        for (RecipeDisplayEntry recipedisplayentry : this.entries) {
            if (predicate.test(recipedisplayentry.id())) {
                list.add(recipedisplayentry);
            }
        }

        return list;
    }

    @OnlyIn(Dist.CLIENT)
    public static enum CraftableStatus {
        ANY,
        CRAFTABLE,
        NOT_CRAFTABLE;
    }
}
