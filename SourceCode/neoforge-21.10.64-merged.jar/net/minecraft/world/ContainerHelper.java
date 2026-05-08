package net.minecraft.world;

import java.util.List;
import java.util.function.Predicate;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class ContainerHelper {
    public static final String TAG_ITEMS = "Items";

    public static ItemStack removeItem(List<ItemStack> stacks, int index, int amount) {
        return index >= 0 && index < stacks.size() && !stacks.get(index).isEmpty() && amount > 0
            ? stacks.get(index).split(amount)
            : ItemStack.EMPTY;
    }

    public static ItemStack takeItem(List<ItemStack> stacks, int index) {
        return index >= 0 && index < stacks.size() ? stacks.set(index, ItemStack.EMPTY) : ItemStack.EMPTY;
    }

    public static void saveAllItems(ValueOutput output, NonNullList<ItemStack> items) {
        saveAllItems(output, items, true);
    }

    public static void saveAllItems(ValueOutput output, NonNullList<ItemStack> items, boolean allowEmpty) {
        ValueOutput.TypedOutputList<ItemStackWithSlot> typedoutputlist = output.list("Items", ItemStackWithSlot.CODEC);

        for (int i = 0; i < items.size(); i++) {
            ItemStack itemstack = items.get(i);
            if (!itemstack.isEmpty()) {
                typedoutputlist.add(new ItemStackWithSlot(i, itemstack));
            }
        }

        if (typedoutputlist.isEmpty() && !allowEmpty) {
            output.discard("Items");
        }
    }

    public static void loadAllItems(ValueInput input, NonNullList<ItemStack> items) {
        for (ItemStackWithSlot itemstackwithslot : input.listOrEmpty("Items", ItemStackWithSlot.CODEC)) {
            if (itemstackwithslot.isValidInContainer(items.size())) {
                items.set(itemstackwithslot.slot(), itemstackwithslot.stack());
            }
        }
    }

    /**
     * Clears items from the inventory matching a predicate.
     * @return The amount of items cleared
     *
     * @param maxItems The maximum amount of items to be cleared. A negative value
     *                 means unlimited and 0 means count how many items are found that
     *                 could be cleared.
     */
    public static int clearOrCountMatchingItems(Container container, Predicate<ItemStack> itemPredicate, int maxItems, boolean simulate) {
        int i = 0;

        for (int j = 0; j < container.getContainerSize(); j++) {
            ItemStack itemstack = container.getItem(j);
            int k = clearOrCountMatchingItems(itemstack, itemPredicate, maxItems - i, simulate);
            if (k > 0 && !simulate && itemstack.isEmpty()) {
                container.setItem(j, ItemStack.EMPTY);
            }

            i += k;
        }

        return i;
    }

    public static int clearOrCountMatchingItems(ItemStack stack, Predicate<ItemStack> itemPredicate, int maxItems, boolean simulate) {
        if (stack.isEmpty() || !itemPredicate.test(stack)) {
            return 0;
        } else if (simulate) {
            return stack.getCount();
        } else {
            int i = maxItems < 0 ? stack.getCount() : Math.min(maxItems, stack.getCount());
            stack.shrink(i);
            return i;
        }
    }
}
