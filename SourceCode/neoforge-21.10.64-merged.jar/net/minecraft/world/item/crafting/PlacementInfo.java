package net.minecraft.world.item.crafting;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PlacementInfo {
    public static final int EMPTY_SLOT = -1;
    public static final PlacementInfo NOT_PLACEABLE = new PlacementInfo(List.of(), IntList.of());
    private final List<Ingredient> ingredients;
    private final IntList slotsToIngredientIndex;

    private PlacementInfo(List<Ingredient> ingredients, IntList slotsToIngredientIndex) {
        this.ingredients = ingredients;
        this.slotsToIngredientIndex = slotsToIngredientIndex;
    }

    public static PlacementInfo create(Ingredient ingredient) {
        return ingredient.isEmpty() ? NOT_PLACEABLE : new PlacementInfo(List.of(ingredient), IntList.of(0));
    }

    public static PlacementInfo createFromOptionals(List<Optional<Ingredient>> optionals) {
        int i = optionals.size();
        List<Ingredient> list = new ArrayList<>(i);
        IntList intlist = new IntArrayList(i);
        int j = 0;

        for (Optional<Ingredient> optional : optionals) {
            if (optional.isPresent()) {
                Ingredient ingredient = optional.get();
                if (ingredient.isEmpty()) {
                    return NOT_PLACEABLE;
                }

                list.add(ingredient);
                intlist.add(j++);
            } else {
                intlist.add(-1);
            }
        }

        return new PlacementInfo(list, intlist);
    }

    public static PlacementInfo create(List<Ingredient> ingredients) {
        int i = ingredients.size();
        IntList intlist = new IntArrayList(i);

        for (int j = 0; j < i; j++) {
            Ingredient ingredient = ingredients.get(j);
            if (ingredient.isEmpty()) {
                return NOT_PLACEABLE;
            }

            intlist.add(j);
        }

        return new PlacementInfo(ingredients, intlist);
    }

    public IntList slotsToIngredientIndex() {
        return this.slotsToIngredientIndex;
    }

    public List<Ingredient> ingredients() {
        return this.ingredients;
    }

    public boolean isImpossibleToPlace() {
        return this.slotsToIngredientIndex.isEmpty();
    }
}
