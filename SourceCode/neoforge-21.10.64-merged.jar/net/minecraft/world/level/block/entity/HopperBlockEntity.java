package net.minecraft.world.level.block.entity;

import java.util.List;
import java.util.function.BooleanSupplier;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.WorldlyContainerHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.HopperMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;

public class HopperBlockEntity extends RandomizableContainerBlockEntity implements Hopper {
    public static final int MOVE_ITEM_SPEED = 8;
    public static final int HOPPER_CONTAINER_SIZE = 5;
    private static final int[][] CACHED_SLOTS = new int[54][];
    private static final int NO_COOLDOWN_TIME = -1;
    private static final Component DEFAULT_NAME = Component.translatable("container.hopper");
    private NonNullList<ItemStack> items = NonNullList.withSize(5, ItemStack.EMPTY);
    private int cooldownTime = -1;
    private long tickedGameTime;
    private Direction facing;

    public HopperBlockEntity(BlockPos pos, BlockState blockState) {
        super(BlockEntityType.HOPPER, pos, blockState);
        this.facing = blockState.getValue(HopperBlock.FACING);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.items = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
        if (!this.tryLoadLootTable(input)) {
            ContainerHelper.loadAllItems(input, this.items);
        }

        this.cooldownTime = input.getIntOr("TransferCooldown", -1);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (!this.trySaveLootTable(output)) {
            ContainerHelper.saveAllItems(output, this.items);
        }

        output.putInt("TransferCooldown", this.cooldownTime);
    }

    @Override
    public int getContainerSize() {
        return this.items.size();
    }

    @Override
    public ItemStack removeItem(int index, int count) {
        this.unpackLootTable(null);
        return ContainerHelper.removeItem(this.getItems(), index, count);
    }

    @Override
    public void setItem(int index, ItemStack stack) {
        this.unpackLootTable(null);
        this.getItems().set(index, stack);
        stack.limitSize(this.getMaxStackSize(stack));
    }
    // Neo: Make the hopper cooldown time transactional (it may change when items are inserted)
    private final net.neoforged.neoforge.transfer.transaction.SnapshotJournal<Integer> cooldownTimeJournal = new net.neoforged.neoforge.transfer.transaction.SnapshotJournal<>() {
        @Override
        protected Integer createSnapshot() {
            return cooldownTime;
        }
        @Override
        protected void revertToSnapshot(Integer snapshot) {
            cooldownTime = snapshot;
        }
    };

    // Neo: Checks if the hopper was empty just before the insertion of `amountChange` at index `slot`.
    private boolean wasEmpty(int slot, int amountChange) {
        if (items.get(slot).getCount() != amountChange) return false;
        for (int i = 0; i < items.size(); ++i) {
            if (i != slot && !items.get(i).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onTransfer(int slot, int amountChange, net.neoforged.neoforge.transfer.transaction.TransactionContext transaction) {
        // If we just went from empty to something in the hopper, reset the cooldown.
        if (amountChange > 0 && !isOnCustomCooldown() && wasEmpty(slot, amountChange)) {
            cooldownTimeJournal.updateSnapshots(transaction);
            // This cooldown is always set to 8 in vanilla with one exception:
            // Hopper -> Hopper transfer sets this cooldown to 7 when this hopper
            // has not been updated as recently as the one pushing items into it.
            // Hopper <-> hopper interactions don't use transactions so we don't need to worry about that.
            setCooldown(MOVE_ITEM_SPEED);
        }
    }

    @Override
    public void setBlockState(BlockState blockState) {
        super.setBlockState(blockState);
        this.facing = blockState.getValue(HopperBlock.FACING);
    }

    @Override
    protected Component getDefaultName() {
        return DEFAULT_NAME;
    }

    public static void pushItemsTick(Level level, BlockPos pos, BlockState state, HopperBlockEntity blockEntity) {
        blockEntity.cooldownTime--;
        blockEntity.tickedGameTime = level.getGameTime();
        if (!blockEntity.isOnCooldown()) {
            blockEntity.setCooldown(0);
            tryMoveItems(level, pos, state, blockEntity, () -> suckInItems(level, blockEntity));
        }
    }

    private static boolean tryMoveItems(Level level, BlockPos pos, BlockState state, HopperBlockEntity blockEntity, BooleanSupplier validator) {
        if (level.isClientSide()) {
            return false;
        } else {
            if (!blockEntity.isOnCooldown() && state.getValue(HopperBlock.ENABLED)) {
                boolean flag = false;
                if (!blockEntity.isEmpty()) {
                    flag = ejectItems(level, pos, blockEntity);
                }

                if (!blockEntity.inventoryFull()) {
                    flag |= validator.getAsBoolean();
                }

                if (flag) {
                    blockEntity.setCooldown(8);
                    setChanged(level, pos, state);
                    return true;
                }
            }

            return false;
        }
    }

    private boolean inventoryFull() {
        for (ItemStack itemstack : this.items) {
            if (itemstack.isEmpty() || itemstack.getCount() != itemstack.getMaxStackSize()) {
                return false;
            }
        }

        return true;
    }

    private static boolean ejectItems(Level level, BlockPos pos, HopperBlockEntity blockEntity) {
        var containerOrHandler = getContainerOrHandlerAt(level, pos.relative(blockEntity.facing), blockEntity.facing.getOpposite());
        if (containerOrHandler.isEmpty()) {
            return false;
        } else if (containerOrHandler.container() != null) {
            Container container = containerOrHandler.container();
            Direction direction = blockEntity.facing.getOpposite();
            if (isFullContainer(container, direction)) {
                return false;
            } else {
                for (int i = 0; i < blockEntity.getContainerSize(); i++) {
                    ItemStack itemstack = blockEntity.getItem(i);
                    if (!itemstack.isEmpty()) {
                        int j = itemstack.getCount();
                        ItemStack itemstack1 = addItem(blockEntity, container, blockEntity.removeItem(i, 1), direction);
                        if (itemstack1.isEmpty()) {
                            container.setChanged();
                            return true;
                        }

                        itemstack.setCount(j);
                        if (j == 1) {
                            blockEntity.setItem(i, itemstack);
                        }
                    }
                }

                return false;
            }
        } else {
            return net.neoforged.neoforge.transfer.item.VanillaInventoryCodeHooks.insertHook(blockEntity, containerOrHandler.itemHandler());
        }
    }

    private static int[] getSlots(Container container, Direction direction) {
        if (container instanceof WorldlyContainer worldlycontainer) {
            return worldlycontainer.getSlotsForFace(direction);
        } else {
            int i = container.getContainerSize();
            if (i < CACHED_SLOTS.length) {
                int[] aint = CACHED_SLOTS[i];
                if (aint != null) {
                    return aint;
                } else {
                    int[] aint1 = createFlatSlots(i);
                    CACHED_SLOTS[i] = aint1;
                    return aint1;
                }
            } else {
                return createFlatSlots(i);
            }
        }
    }

    private static int[] createFlatSlots(int size) {
        int[] aint = new int[size];
        int i = 0;

        while (i < aint.length) {
            aint[i] = i++;
        }

        return aint;
    }

    /**
     * @return {@code false} if the {@code container} has any room to place items in
     */
    private static boolean isFullContainer(Container container, Direction direction) {
        int[] aint = getSlots(container, direction);

        for (int i : aint) {
            ItemStack itemstack = container.getItem(i);
            if (itemstack.getCount() < itemstack.getMaxStackSize()) {
                return false;
            }
        }

        return true;
    }

    public static boolean suckInItems(Level level, Hopper hopper) {
        BlockPos blockpos = BlockPos.containing(hopper.getLevelX(), hopper.getLevelY() + 1.0, hopper.getLevelZ());
        BlockState blockstate = level.getBlockState(blockpos);
        var containerOrHandler = getSourceContainerOrHandler(level, hopper, blockpos, blockstate);
        if (containerOrHandler.container() != null) {
            Container container = containerOrHandler.container();
            Direction direction = Direction.DOWN;

            for (int i : getSlots(container, direction)) {
                if (tryTakeInItemFromSlot(hopper, container, i, direction)) {
                    return true;
                }
            }

            return false;
        } else if (containerOrHandler.itemHandler() != null) {
            return net.neoforged.neoforge.transfer.item.VanillaInventoryCodeHooks.extractHook(hopper, containerOrHandler.itemHandler());
        } else {
            boolean flag = hopper.isGridAligned()
                && blockstate.isCollisionShapeFullBlock(level, blockpos)
                && !blockstate.is(BlockTags.DOES_NOT_BLOCK_HOPPERS);
            if (!flag) {
                for (ItemEntity itementity : getItemsAtAndAbove(level, hopper)) {
                    if (addItem(hopper, itementity)) {
                        return true;
                    }
                }
            }

            return false;
        }
    }

    /**
     * Pulls from the specified slot in the container and places in any available slot in the hopper.
     * @return {@code true} if the entire stack was moved.
     */
    private static boolean tryTakeInItemFromSlot(Hopper hopper, Container container, int slot, Direction direction) {
        ItemStack itemstack = container.getItem(slot);
        if (!itemstack.isEmpty() && canTakeItemFromContainer(hopper, container, itemstack, slot, direction)) {
            int i = itemstack.getCount();
            ItemStack itemstack1 = addItem(container, hopper, container.removeItem(slot, 1), null);
            if (itemstack1.isEmpty()) {
                container.setChanged();
                return true;
            }

            itemstack.setCount(i);
            if (i == 1) {
                container.setItem(slot, itemstack);
            }
        }

        return false;
    }

    public static boolean addItem(Container container, ItemEntity item) {
        boolean flag = false;
        ItemStack itemstack = item.getItem().copy();
        ItemStack itemstack1 = addItem(null, container, itemstack, null);
        if (itemstack1.isEmpty()) {
            flag = true;
            item.setItem(ItemStack.EMPTY);
            item.discard();
        } else {
            item.setItem(itemstack1);
        }

        return flag;
    }

    /**
     * Attempts to place the passed stack in the container, using as many slots as required.
     * @return any leftover stack
     */
    public static ItemStack addItem(@Nullable Container source, Container destination, ItemStack stack, @Nullable Direction direction) {
        if (destination instanceof WorldlyContainer worldlycontainer && direction != null) {
            int[] aint = worldlycontainer.getSlotsForFace(direction);

            for (int k = 0; k < aint.length && !stack.isEmpty(); k++) {
                stack = tryMoveInItem(source, destination, stack, aint[k], direction);
            }
        } else {
            int i = destination.getContainerSize();

            for (int j = 0; j < i && !stack.isEmpty(); j++) {
                stack = tryMoveInItem(source, destination, stack, j, direction);
            }
        }

        return stack;
    }

    private static boolean canPlaceItemInContainer(Container container, ItemStack stack, int slot, @Nullable Direction direction) {
        return !container.canPlaceItem(slot, stack)
            ? false
            : !(container instanceof WorldlyContainer worldlycontainer && !worldlycontainer.canPlaceItemThroughFace(slot, stack, direction));
    }

    private static boolean canTakeItemFromContainer(Container source, Container destination, ItemStack stack, int slot, Direction direction) {
        return !destination.canTakeItem(source, slot, stack)
            ? false
            : !(destination instanceof WorldlyContainer worldlycontainer && !worldlycontainer.canTakeItemThroughFace(slot, stack, direction));
    }

    private static ItemStack tryMoveInItem(@Nullable Container source, Container destination, ItemStack stack, int slot, @Nullable Direction direction) {
        ItemStack itemstack = destination.getItem(slot);
        if (canPlaceItemInContainer(destination, stack, slot, direction)) {
            boolean flag = false;
            boolean flag1 = destination.isEmpty();
            if (itemstack.isEmpty()) {
                destination.setItem(slot, stack);
                stack = ItemStack.EMPTY;
                flag = true;
            } else if (canMergeItems(itemstack, stack)) {
                int i = stack.getMaxStackSize() - itemstack.getCount();
                int j = Math.min(stack.getCount(), i);
                stack.shrink(j);
                itemstack.grow(j);
                flag = j > 0;
            }

            if (flag) {
                if (flag1 && destination instanceof HopperBlockEntity hopperblockentity1 && !hopperblockentity1.isOnCustomCooldown()) {
                    int k = 0;
                    if (source instanceof HopperBlockEntity hopperblockentity && hopperblockentity1.tickedGameTime >= hopperblockentity.tickedGameTime) {
                        k = 1;
                    }

                    hopperblockentity1.setCooldown(8 - k);
                }

                destination.setChanged();
            }
        }

        return stack;
    }

    @Nullable
    private static Container getAttachedContainer(Level level, BlockPos pos, HopperBlockEntity blockEntity) {
        return getContainerAt(level, pos.relative(blockEntity.facing));
    }

    @Nullable
    private static Container getSourceContainer(Level level, Hopper hopper, BlockPos pos, BlockState state) {
        return getContainerAt(level, pos, state, hopper.getLevelX(), hopper.getLevelY() + 1.0, hopper.getLevelZ());
    }

    public static List<ItemEntity> getItemsAtAndAbove(Level level, Hopper hopper) {
        AABB aabb = hopper.getSuckAabb().move(hopper.getLevelX() - 0.5, hopper.getLevelY() - 0.5, hopper.getLevelZ() - 0.5);
        return level.getEntitiesOfClass(ItemEntity.class, aabb, EntitySelector.ENTITY_STILL_ALIVE);
    }

    /**
 * @deprecated Use IItemHandler capability instead. To preserve Container-specific
 *             interactions, use {@link #getContainerOrHandlerAt} and handle both
 *             cases.
 */
    @Deprecated
    @Nullable
    public static Container getContainerAt(Level level, BlockPos pos) {
        return getContainerAt(level, pos, level.getBlockState(pos), pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
    }

    @Nullable
    private static Container getContainerAt(Level level, BlockPos pos, BlockState state, double x, double y, double z) {
        Container container = getBlockContainer(level, pos, state);
        if (container == null) {
            container = getEntityContainer(level, x, y, z);
        }

        return container;
    }

    @Nullable
    private static Container getBlockContainer(Level level, BlockPos pos, BlockState state) {
        Block block = state.getBlock();
        if (block instanceof WorldlyContainerHolder) {
            return ((WorldlyContainerHolder)block).getContainer(state, level, pos);
        } else if (state.hasBlockEntity() && level.getBlockEntity(pos) instanceof Container container) {
            if (container instanceof ChestBlockEntity && block instanceof ChestBlock) {
                container = ChestBlock.getContainer((ChestBlock)block, state, level, pos, true);
            }

            return container;
        } else {
            return null;
        }
    }

    @Nullable
    private static Container getEntityContainer(Level level, double x, double y, double z) {
        List<Entity> list = level.getEntities(
            (Entity)null,
            new AABB(x - 0.5, y - 0.5, z - 0.5, x + 0.5, y + 0.5, z + 0.5),
            EntitySelector.CONTAINER_ENTITY_SELECTOR
        );
        return !list.isEmpty() ? (Container)list.get(level.random.nextInt(list.size())) : null;
    }

    private static net.neoforged.neoforge.transfer.item.ContainerOrHandler getSourceContainerOrHandler(Level p_155597_, Hopper p_155598_, BlockPos p_326315_, BlockState p_326093_) {
        return getContainerOrHandlerAt(p_155597_, p_326315_, p_326093_, p_155598_.getLevelX(), p_155598_.getLevelY() + 1.0, p_155598_.getLevelZ(), Direction.DOWN);
    }

    public static net.neoforged.neoforge.transfer.item.ContainerOrHandler getContainerOrHandlerAt(Level level, BlockPos pos, @Nullable Direction side) {
        return getContainerOrHandlerAt(
                level, pos, level.getBlockState(pos), (double)pos.getX() + 0.5, (double)pos.getY() + 0.5, (double)pos.getZ() + 0.5, side
        );
    }

    private static net.neoforged.neoforge.transfer.item.ContainerOrHandler getContainerOrHandlerAt(Level level, BlockPos pos, BlockState state, double x, double y, double z, @Nullable Direction side) {
        Container container = getBlockContainer(level, pos, state);
        if (container != null) {
            return new net.neoforged.neoforge.transfer.item.ContainerOrHandler(container, null);
        }
        var blockItemHandler = level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.Item.BLOCK, pos, state, null, side);
        if (blockItemHandler != null) {
            return new net.neoforged.neoforge.transfer.item.ContainerOrHandler(null, blockItemHandler);
        }
        return net.neoforged.neoforge.transfer.item.VanillaInventoryCodeHooks.getEntityContainerOrHandler(level, x, y, z, side);
    }

    private static boolean canMergeItems(ItemStack stack1, ItemStack stack2) {
        return stack1.getCount() <= stack1.getMaxStackSize() && ItemStack.isSameItemSameComponents(stack1, stack2);
    }

    @Override
    public double getLevelX() {
        return this.worldPosition.getX() + 0.5;
    }

    @Override
    public double getLevelY() {
        return this.worldPosition.getY() + 0.5;
    }

    @Override
    public double getLevelZ() {
        return this.worldPosition.getZ() + 0.5;
    }

    @Override
    public boolean isGridAligned() {
        return true;
    }

    public void setCooldown(int cooldownTime) {
        this.cooldownTime = cooldownTime;
    }

    private boolean isOnCooldown() {
        return this.cooldownTime > 0;
    }

    public boolean isOnCustomCooldown() {
        return this.cooldownTime > 8;
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return this.items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> items) {
        this.items = items;
    }

    public static void entityInside(Level level, BlockPos pos, BlockState state, Entity entity, HopperBlockEntity blockEntity) {
        if (entity instanceof ItemEntity itementity
            && !itementity.getItem().isEmpty()
            && entity.getBoundingBox().move(-pos.getX(), -pos.getY(), -pos.getZ()).intersects(blockEntity.getSuckAabb())) {
            tryMoveItems(level, pos, state, blockEntity, () -> addItem(blockEntity, itementity));
        }
    }

    @Override
    protected AbstractContainerMenu createMenu(int id, Inventory player) {
        return new HopperMenu(id, player, this);
    }

    public long getLastUpdateTime() {
        return this.tickedGameTime;
    }
}
