package net.minecraft.world.level.block.entity;

import com.mojang.logging.LogUtils;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.ChiseledBookShelfBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.slf4j.Logger;

public class ChiseledBookShelfBlockEntity extends BlockEntity implements ListBackedContainer {
    public static final int MAX_BOOKS_IN_STORAGE = 6;
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int DEFAULT_LAST_INTERACTED_SLOT = -1;
    private final NonNullList<ItemStack> items = NonNullList.withSize(6, ItemStack.EMPTY);
    private int lastInteractedSlot = -1;

    public ChiseledBookShelfBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityType.CHISELED_BOOKSHELF, pos, state);
    }

    private void updateState(int slot) {
        if (slot >= 0 && slot < 6) {
            this.lastInteractedSlot = slot;
            BlockState blockstate = this.getBlockState();

            for (int i = 0; i < ChiseledBookShelfBlock.SLOT_OCCUPIED_PROPERTIES.size(); i++) {
                boolean flag = !this.getItem(i).isEmpty();
                BooleanProperty booleanproperty = ChiseledBookShelfBlock.SLOT_OCCUPIED_PROPERTIES.get(i);
                blockstate = blockstate.setValue(booleanproperty, flag);
            }

            Objects.requireNonNull(this.level).setBlock(this.worldPosition, blockstate, 3);
            this.level.gameEvent(GameEvent.BLOCK_CHANGE, this.worldPosition, GameEvent.Context.of(blockstate));
        } else {
            LOGGER.error("Expected slot 0-5, got {}", slot);
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.items.clear();
        ContainerHelper.loadAllItems(input, this.items);
        this.lastInteractedSlot = input.getIntOr("last_interacted_slot", -1);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, this.items, true);
        output.putInt("last_interacted_slot", this.lastInteractedSlot);
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public boolean acceptsItemType(ItemStack stack) {
        return stack.is(ItemTags.BOOKSHELF_BOOKS);
    }

    /**
     * Removes up to a specified number of items from an inventory slot and returns them in a new stack.
     */
    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack itemstack = Objects.requireNonNullElse(this.getItems().get(slot), ItemStack.EMPTY);
        this.getItems().set(slot, ItemStack.EMPTY);
        if (!itemstack.isEmpty()) {
            this.updateState(slot);
        }

        return itemstack;
    }

    /**
     * Sets the given item stack to the specified slot in the inventory (can be crafting or armor sections).
     */
    @Override
    public void setItem(int slot, ItemStack stack) {
        setItem(slot, stack, false);
    }

    /**
     * Sets the given item stack to the specified slot in the inventory (can be crafting or armor sections).
     */
    // Neo: Skip side-effects if insideTransaction is true so the caller can defer them until the transaction commits
    @Override
    public void setItem(int slot, ItemStack stack, boolean insideTransaction) {
        if (this.acceptsItemType(stack)) {
            this.getItems().set(slot, stack);
            if (!insideTransaction) {
                this.updateState(slot);
            }
        } else if (stack.isEmpty()) {
            if (insideTransaction) {
                // Skip the updateState call in removeItem
                this.getItems().set(slot, stack);
                return;
            }
            this.removeItem(slot, this.getMaxStackSize());
        }
    }

    // Neo: Make lastInteractedSlot transactional, and defer updateState until end of a transaction
    private final net.neoforged.neoforge.transfer.transaction.SnapshotJournal<Integer> lastInteractedSlotJournal = new net.neoforged.neoforge.transfer.transaction.SnapshotJournal<>() {
        @Override
        protected Integer createSnapshot() {
            return lastInteractedSlot;
        }
        @Override
        protected void revertToSnapshot(Integer snapshot) {
            lastInteractedSlot = snapshot;
        }
        @Override
        protected void onRootCommit(Integer originalState) {
            // If the block entity was removed, skip updateState to avoid the setBlock call that would overwrite the current block
            if (!isRemoved()) {
                updateState(lastInteractedSlot);
            }
        }
    };

    @Override
    public void onTransfer(int slot, int amountChange, net.neoforged.neoforge.transfer.transaction.TransactionContext transaction) {
        this.lastInteractedSlotJournal.updateSnapshots(transaction);
        lastInteractedSlot = slot;
    }

    /**
     * {@return {@code true} if the given stack can be extracted into the target inventory}
     *
     * @param target the container into which the item should be extracted
     * @param slot   the slot from which to extract the item
     * @param stack  the item to extract
     */
    @Override
    public boolean canTakeItem(Container target, int slot, ItemStack stack) {
        return target.hasAnyMatching(
            p_437333_ -> p_437333_.isEmpty()
                ? true
                : ItemStack.isSameItemSameComponents(stack, p_437333_)
                    && p_437333_.getCount() + stack.getCount() <= target.getMaxStackSize(p_437333_)
        );
    }

    @Override
    public NonNullList<ItemStack> getItems() {
        return this.items;
    }

    /**
     * Don't rename this method to canInteractWith due to conflicts with Container
     */
    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    public int getLastInteractedSlot() {
        return this.lastInteractedSlot;
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter componentGetter) {
        super.applyImplicitComponents(componentGetter);
        componentGetter.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyInto(this.items);
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        components.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(this.items));
    }

    @Override
    public void removeComponentsFromTag(ValueOutput output) {
        output.discard("Items");
    }
}
