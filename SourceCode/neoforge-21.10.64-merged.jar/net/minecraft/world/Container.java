package net.minecraft.world;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ContainerUser;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public interface Container extends Clearable, Iterable<ItemStack>, net.neoforged.neoforge.common.extensions.ContainerExtension {
    float DEFAULT_DISTANCE_BUFFER = 4.0F;

    int getContainerSize();

    boolean isEmpty();

    /**
     * Returns the stack in the given slot.
     */
    ItemStack getItem(int slot);

    /**
     * Removes up to a specified number of items from an inventory slot and returns them in a new stack.
     */
    ItemStack removeItem(int slot, int amount);

    /**
     * Removes a stack from the given slot and returns it.
     */
    ItemStack removeItemNoUpdate(int slot);

    /**
     * Sets the given item stack to the specified slot in the inventory (can be crafting or armor sections).
     */
    void setItem(int slot, ItemStack stack);

    default int getMaxStackSize() {
        return 99;
    }

    default int getMaxStackSize(ItemStack stack) {
        return Math.min(this.getMaxStackSize(), stack.getMaxStackSize());
    }

    void setChanged();

    /**
     * Don't rename this method to canInteractWith due to conflicts with Container
     */
    boolean stillValid(Player player);

    default void startOpen(ContainerUser user) {
    }

    default void stopOpen(ContainerUser user) {
    }

    default List<ContainerUser> getEntitiesWithContainerOpen() {
        return List.of();
    }

    /**
     * Returns {@code true} if automation is allowed to insert the given stack (ignoring stack size) into the given slot. For guis use Slot.isItemValid
     */
    default boolean canPlaceItem(int slot, ItemStack stack) {
        return true;
    }

    /**
     * {@return {@code true} if the given stack can be extracted into the target inventory}
     *
     * @param target the container into which the item should be extracted
     * @param slot   the slot from which to extract the item
     * @param stack  the item to extract
     */
    default boolean canTakeItem(Container target, int slot, ItemStack stack) {
        return true;
    }

    /**
     * Returns the total amount of the specified item in this inventory. This method does not check for nbt.
     */
    default int countItem(Item item) {
        int i = 0;

        for (ItemStack itemstack : this) {
            if (itemstack.getItem().equals(item)) {
                i += itemstack.getCount();
            }
        }

        return i;
    }

    /**
     * Returns {@code true} if any item from the passed set exists in this inventory.
     */
    default boolean hasAnyOf(Set<Item> set) {
        return this.hasAnyMatching(p_216873_ -> !p_216873_.isEmpty() && set.contains(p_216873_.getItem()));
    }

    default boolean hasAnyMatching(Predicate<ItemStack> predicate) {
        for (ItemStack itemstack : this) {
            if (predicate.test(itemstack)) {
                return true;
            }
        }

        return false;
    }

    static boolean stillValidBlockEntity(BlockEntity blockEntity, Player player) {
        return stillValidBlockEntity(blockEntity, player, 4.0F);
    }

    static boolean stillValidBlockEntity(BlockEntity blockEntity, Player player, float distance) {
        Level level = blockEntity.getLevel();
        BlockPos blockpos = blockEntity.getBlockPos();
        if (level == null) {
            return false;
        } else {
            return level.getBlockEntity(blockpos) != blockEntity ? false : player.canInteractWithBlock(blockpos, distance);
        }
    }

    @Override
    default Iterator<ItemStack> iterator() {
        return new Container.ContainerIterator(this);
    }

    public static class ContainerIterator implements Iterator<ItemStack> {
        private final Container container;
        private int index;
        private final int size;

        public ContainerIterator(Container container) {
            this.container = container;
            this.size = container.getContainerSize();
        }

        @Override
        public boolean hasNext() {
            return this.index < this.size;
        }

        public ItemStack next() {
            if (!this.hasNext()) {
                throw new NoSuchElementException();
            } else {
                return this.container.getItem(this.index++);
            }
        }
    }
}
