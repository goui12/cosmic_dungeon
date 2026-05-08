package net.minecraft.world.level.block.entity;

import java.util.function.Predicate;
import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;

public interface ListBackedContainer extends Container {
    NonNullList<ItemStack> getItems();

    default int count() {
        return (int)this.getItems().stream().filter(Predicate.not(ItemStack::isEmpty)).count();
    }

    @Override
    default int getContainerSize() {
        return this.getItems().size();
    }

    @Override
    default void clearContent() {
        this.getItems().clear();
    }

    @Override
    default boolean isEmpty() {
        return this.getItems().stream().allMatch(ItemStack::isEmpty);
    }

    /**
     * Returns the stack in the given slot.
     */
    @Override
    default ItemStack getItem(int slot) {
        return this.getItems().get(slot);
    }

    /**
     * Removes up to a specified number of items from an inventory slot and returns them in a new stack.
     */
    @Override
    default ItemStack removeItem(int slot, int amount) {
        ItemStack itemstack = ContainerHelper.removeItem(this.getItems(), slot, amount);
        if (!itemstack.isEmpty()) {
            this.setChanged();
        }

        return itemstack;
    }

    /**
     * Removes a stack from the given slot and returns it.
     */
    @Override
    default ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.removeItem(this.getItems(), slot, this.getMaxStackSize());
    }

    /**
     * Returns {@code true} if automation is allowed to insert the given stack (ignoring stack size) into the given slot. For guis use Slot.isItemValid
     */
    @Override
    default boolean canPlaceItem(int slot, ItemStack stack) {
        return this.acceptsItemType(stack) && (this.getItem(slot).isEmpty() || this.getItem(slot).getCount() < this.getMaxStackSize(stack));
    }

    default boolean acceptsItemType(ItemStack stack) {
        return true;
    }

    /**
     * Sets the given item stack to the specified slot in the inventory (can be crafting or armor sections).
     */
    @Override
    default void setItem(int slot, ItemStack stack) {
        this.setItemNoUpdate(slot, stack);
        this.setChanged();
    }

    default void setItemNoUpdate(int slot, ItemStack stack) {
        this.getItems().set(slot, stack);
        stack.limitSize(this.getMaxStackSize(stack));
    }
}
