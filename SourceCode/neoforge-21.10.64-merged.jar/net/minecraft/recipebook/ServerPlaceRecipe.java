package net.minecraft.recipebook;

import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.StackedItemContents;
import net.minecraft.world.inventory.RecipeBookMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;

public class ServerPlaceRecipe<R extends Recipe<?>> {
    private static final int ITEM_NOT_FOUND = -1;
    private final Inventory inventory;
    private final ServerPlaceRecipe.CraftingMenuAccess<R> menu;
    private final boolean useMaxItems;
    private final int gridWidth;
    private final int gridHeight;
    private final List<Slot> inputGridSlots;
    private final List<Slot> slotsToClear;

    public static <I extends RecipeInput, R extends Recipe<I>> RecipeBookMenu.PostPlaceAction placeRecipe(
        ServerPlaceRecipe.CraftingMenuAccess<R> menu,
        int gridWidth,
        int gridHeight,
        List<Slot> inputGridSlots,
        List<Slot> slotsToClear,
        Inventory inventory,
        RecipeHolder<R> recipe,
        boolean useMaxItems,
        boolean isCreative
    ) {
        ServerPlaceRecipe<R> serverplacerecipe = new ServerPlaceRecipe<>(menu, inventory, useMaxItems, gridWidth, gridHeight, inputGridSlots, slotsToClear);
        if (!isCreative && !serverplacerecipe.testClearGrid()) {
            return RecipeBookMenu.PostPlaceAction.NOTHING;
        } else {
            StackedItemContents stackeditemcontents = new StackedItemContents();
            inventory.fillStackedContents(stackeditemcontents);
            menu.fillCraftSlotsStackedContents(stackeditemcontents);
            return serverplacerecipe.tryPlaceRecipe(recipe, stackeditemcontents);
        }
    }

    private ServerPlaceRecipe(
        ServerPlaceRecipe.CraftingMenuAccess<R> menu,
        Inventory inventory,
        boolean useMaxItems,
        int gridWidth,
        int gridHeight,
        List<Slot> inputGridSlots,
        List<Slot> slotsToClear
    ) {
        this.menu = menu;
        this.inventory = inventory;
        this.useMaxItems = useMaxItems;
        this.gridWidth = gridWidth;
        this.gridHeight = gridHeight;
        this.inputGridSlots = inputGridSlots;
        this.slotsToClear = slotsToClear;
    }

    private RecipeBookMenu.PostPlaceAction tryPlaceRecipe(RecipeHolder<R> recipe, StackedItemContents stackedItemContents) {
        if (stackedItemContents.canCraft(recipe.value(), null)) {
            this.placeRecipe(recipe, stackedItemContents);
            this.inventory.setChanged();
            return RecipeBookMenu.PostPlaceAction.NOTHING;
        } else {
            this.clearGrid();
            this.inventory.setChanged();
            return RecipeBookMenu.PostPlaceAction.PLACE_GHOST_RECIPE;
        }
    }

    private void clearGrid() {
        for (Slot slot : this.slotsToClear) {
            ItemStack itemstack = slot.getItem().copy();
            this.inventory.placeItemBackInInventory(itemstack, false);
            slot.set(itemstack);
        }

        this.menu.clearCraftingContent();
    }

    private void placeRecipe(RecipeHolder<R> recipe, StackedItemContents stackedItemContents) {
        boolean flag = this.menu.recipeMatches(recipe);
        int i = stackedItemContents.getBiggestCraftableStack(recipe.value(), null);
        if (flag) {
            for (Slot slot : this.inputGridSlots) {
                ItemStack itemstack = slot.getItem();
                if (!itemstack.isEmpty() && Math.min(i, itemstack.getMaxStackSize()) < itemstack.getCount() + 1) {
                    return;
                }
            }
        }

        int j = this.calculateAmountToCraft(i, flag);
        List<Holder<Item>> list = new ArrayList<>();
        if (stackedItemContents.canCraft(recipe.value(), j, list::add)) {
            int k = clampToMaxStackSize(j, list);
            if (k != j) {
                list.clear();
                if (!stackedItemContents.canCraft(recipe.value(), k, list::add)) {
                    return;
                }
            }

            this.clearGrid();
            PlaceRecipeHelper.placeRecipe(
                this.gridWidth,
                this.gridHeight,
                recipe.value(),
                recipe.value().placementInfo().slotsToIngredientIndex(),
                (p_389375_, p_389376_, p_389377_, p_389378_) -> {
                    if (p_389375_ != -1) {
                        Slot slot1 = this.inputGridSlots.get(p_389376_);
                        Holder<Item> holder = list.get(p_389375_);
                        int l = k;

                        while (l > 0) {
                            l = this.moveItemToGrid(slot1, holder, l);
                            if (l == -1) {
                                return;
                            }
                        }
                    }
                }
            );
        }
    }

    private static int clampToMaxStackSize(int amount, List<Holder<Item>> items) {
        for (Holder<Item> holder : items) {
            amount = Math.min(amount, holder.value().getDefaultMaxStackSize());
        }

        return amount;
    }

    private int calculateAmountToCraft(int max, boolean recipeMatches) {
        if (this.useMaxItems) {
            return max;
        } else if (recipeMatches) {
            int i = Integer.MAX_VALUE;

            for (Slot slot : this.inputGridSlots) {
                ItemStack itemstack = slot.getItem();
                if (!itemstack.isEmpty() && i > itemstack.getCount()) {
                    i = itemstack.getCount();
                }
            }

            if (i != Integer.MAX_VALUE) {
                i++;
            }

            return i;
        } else {
            return 1;
        }
    }

    private int moveItemToGrid(Slot slot, Holder<Item> item, int count) {
        ItemStack itemstack = slot.getItem();
        int i = this.inventory.findSlotMatchingCraftingIngredient(item, itemstack);
        if (i == -1) {
            return -1;
        } else {
            ItemStack itemstack1 = this.inventory.getItem(i);
            ItemStack itemstack2;
            if (count < itemstack1.getCount()) {
                itemstack2 = this.inventory.removeItem(i, count);
            } else {
                itemstack2 = this.inventory.removeItemNoUpdate(i);
            }

            int j = itemstack2.getCount();
            if (itemstack.isEmpty()) {
                slot.set(itemstack2);
            } else {
                itemstack.grow(j);
            }

            return count - j;
        }
    }

    private boolean testClearGrid() {
        List<ItemStack> list = Lists.newArrayList();
        int i = this.getAmountOfFreeSlotsInInventory();

        for (Slot slot : this.inputGridSlots) {
            ItemStack itemstack = slot.getItem().copy();
            if (!itemstack.isEmpty()) {
                int j = this.inventory.getSlotWithRemainingSpace(itemstack);
                if (j == -1 && list.size() <= i) {
                    for (ItemStack itemstack1 : list) {
                        if (ItemStack.isSameItem(itemstack1, itemstack)
                            && itemstack1.getCount() != itemstack1.getMaxStackSize()
                            && itemstack1.getCount() + itemstack.getCount() <= itemstack1.getMaxStackSize()) {
                            itemstack1.grow(itemstack.getCount());
                            itemstack.setCount(0);
                            break;
                        }
                    }

                    if (!itemstack.isEmpty()) {
                        if (list.size() >= i) {
                            return false;
                        }

                        list.add(itemstack);
                    }
                } else if (j == -1) {
                    return false;
                }
            }
        }

        return true;
    }

    private int getAmountOfFreeSlotsInInventory() {
        int i = 0;

        for (ItemStack itemstack : this.inventory.getNonEquipmentItems()) {
            if (itemstack.isEmpty()) {
                i++;
            }
        }

        return i;
    }

    public interface CraftingMenuAccess<T extends Recipe<?>> {
        void fillCraftSlotsStackedContents(StackedItemContents stackedItemContents);

        void clearCraftingContent();

        boolean recipeMatches(RecipeHolder<T> recipe);
    }
}
