package net.minecraft.world.entity.player;

import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import java.util.Map;
import java.util.function.Predicate;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.core.Holder;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetPlayerInventoryPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.ItemStackWithSlot;
import net.minecraft.world.Nameable;
import net.minecraft.world.entity.EntityEquipment;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class Inventory implements Container, Nameable {
    public static final int POP_TIME_DURATION = 5;
    public static final int INVENTORY_SIZE = 36;
    public static final int SELECTION_SIZE = 9;
    public static final int SLOT_OFFHAND = 40;
    public static final int SLOT_BODY_ARMOR = 41;
    public static final int SLOT_SADDLE = 42;
    public static final int NOT_FOUND_INDEX = -1;
    public static final Int2ObjectMap<EquipmentSlot> EQUIPMENT_SLOT_MAPPING = new Int2ObjectArrayMap<>(
        Map.of(
            EquipmentSlot.FEET.getIndex(36),
            EquipmentSlot.FEET,
            EquipmentSlot.LEGS.getIndex(36),
            EquipmentSlot.LEGS,
            EquipmentSlot.CHEST.getIndex(36),
            EquipmentSlot.CHEST,
            EquipmentSlot.HEAD.getIndex(36),
            EquipmentSlot.HEAD,
            40,
            EquipmentSlot.OFFHAND,
            41,
            EquipmentSlot.BODY,
            42,
            EquipmentSlot.SADDLE
        )
    );
    private static final Component DEFAULT_NAME = Component.translatable("container.inventory");
    private final NonNullList<ItemStack> items = NonNullList.withSize(36, ItemStack.EMPTY);
    private int selected;
    public final Player player;
    private final EntityEquipment equipment;
    private int timesChanged;

    public Inventory(Player player, EntityEquipment equipment) {
        this.player = player;
        this.equipment = equipment;
    }

    public int getSelectedSlot() {
        return this.selected;
    }

    public void setSelectedSlot(int slot) {
        if (!isHotbarSlot(slot)) {
            throw new IllegalArgumentException("Invalid selected slot");
        } else {
            this.selected = slot;
        }
    }

    public ItemStack getSelectedItem() {
        return this.items.get(this.selected);
    }

    public ItemStack setSelectedItem(ItemStack stack) {
        return this.items.set(this.selected, stack);
    }

    public static int getSelectionSize() {
        return 9;
    }

    public NonNullList<ItemStack> getNonEquipmentItems() {
        return this.items;
    }

    private boolean hasRemainingSpaceForItem(ItemStack destination, ItemStack origin) {
        return !destination.isEmpty()
            && ItemStack.isSameItemSameComponents(destination, origin)
            && destination.isStackable()
            && destination.getCount() < this.getMaxStackSize(destination);
    }

    public int getFreeSlot() {
        for (int i = 0; i < this.items.size(); i++) {
            if (this.items.get(i).isEmpty()) {
                return i;
            }
        }

        return -1;
    }

    public void addAndPickItem(ItemStack stack) {
        this.setSelectedSlot(this.getSuitableHotbarSlot());
        if (!this.items.get(this.selected).isEmpty()) {
            int i = this.getFreeSlot();
            if (i != -1) {
                this.items.set(i, this.items.get(this.selected));
            }
        }

        this.items.set(this.selected, stack);
    }

    public void pickSlot(int index) {
        this.setSelectedSlot(this.getSuitableHotbarSlot());
        ItemStack itemstack = this.items.get(this.selected);
        this.items.set(this.selected, this.items.get(index));
        this.items.set(index, itemstack);
    }

    public static boolean isHotbarSlot(int index) {
        return index >= 0 && index < 9;
    }

    /**
     * Finds the stack or an equivalent one in the main inventory
     */
    public int findSlotMatchingItem(ItemStack stack) {
        for (int i = 0; i < this.items.size(); i++) {
            if (!this.items.get(i).isEmpty() && ItemStack.isSameItemSameComponents(stack, this.items.get(i))) {
                return i;
            }
        }

        return -1;
    }

    public static boolean isUsableForCrafting(ItemStack stack) {
        return !stack.isDamaged() && !stack.isEnchanted() && !stack.has(DataComponents.CUSTOM_NAME);
    }

    public int findSlotMatchingCraftingIngredient(Holder<Item> item, ItemStack stack) {
        for (int i = 0; i < this.items.size(); i++) {
            ItemStack itemstack = this.items.get(i);
            if (!itemstack.isEmpty()
                && itemstack.is(item)
                && isUsableForCrafting(itemstack)
                && (stack.isEmpty() || ItemStack.isSameItemSameComponents(stack, itemstack))) {
                return i;
            }
        }

        return -1;
    }

    public int getSuitableHotbarSlot() {
        for (int i = 0; i < 9; i++) {
            int j = (this.selected + i) % 9;
            if (this.items.get(j).isEmpty()) {
                return j;
            }
        }

        for (int k = 0; k < 9; k++) {
            int l = (this.selected + k) % 9;
            if (!this.items.get(l).isNotReplaceableByPickAction(this.player, l)) {
                return l;
            }
        }

        return this.selected;
    }

    public int clearOrCountMatchingItems(Predicate<ItemStack> stackPredicate, int maxCount, Container inventory) {
        int i = 0;
        boolean flag = maxCount == 0;
        i += ContainerHelper.clearOrCountMatchingItems(this, stackPredicate, maxCount - i, flag);
        i += ContainerHelper.clearOrCountMatchingItems(inventory, stackPredicate, maxCount - i, flag);
        ItemStack itemstack = this.player.containerMenu.getCarried();
        i += ContainerHelper.clearOrCountMatchingItems(itemstack, stackPredicate, maxCount - i, flag);
        if (itemstack.isEmpty()) {
            this.player.containerMenu.setCarried(ItemStack.EMPTY);
        }

        return i;
    }

    /**
     * This function stores as many items of an ItemStack as possible in a matching slot and returns the quantity of left over items.
     */
    private int addResource(ItemStack stack) {
        int i = this.getSlotWithRemainingSpace(stack);
        if (i == -1) {
            i = this.getFreeSlot();
        }

        return i == -1 ? stack.getCount() : this.addResource(i, stack);
    }

    private int addResource(int slot, ItemStack stack) {
        int i = stack.getCount();
        ItemStack itemstack = this.getItem(slot);
        if (itemstack.isEmpty()) {
            itemstack = stack.copyWithCount(0);
            this.setItem(slot, itemstack);
        }

        int j = this.getMaxStackSize(itemstack) - itemstack.getCount();
        int k = Math.min(i, j);
        if (k == 0) {
            return i;
        } else {
            i -= k;
            itemstack.grow(k);
            itemstack.setPopTime(5);
            return i;
        }
    }

    /**
     * Stores a stack in the player's inventory. It first tries to place it in the selected slot in the player's hotbar, then the offhand slot, then any available/empty slot in the player's inventory.
     */
    public int getSlotWithRemainingSpace(ItemStack stack) {
        if (this.hasRemainingSpaceForItem(this.getItem(this.selected), stack)) {
            return this.selected;
        } else if (this.hasRemainingSpaceForItem(this.getItem(40), stack)) {
            return 40;
        } else {
            for (int i = 0; i < this.items.size(); i++) {
                if (this.hasRemainingSpaceForItem(this.items.get(i), stack)) {
                    return i;
                }
            }

            return -1;
        }
    }

    public void tick() {
        for (int i = 0; i < this.items.size(); i++) {
            ItemStack itemstack = this.getItem(i);
            if (!itemstack.isEmpty()) {
                itemstack.inventoryTick(this.player.level(), this.player, i == this.selected ? EquipmentSlot.MAINHAND : null);
            }
        }
    }

    /**
     * Adds the stack to the first empty slot in the player's inventory. Returns {@code false} if it's not possible to place the entire stack in the inventory.
     */
    public boolean add(ItemStack stack) {
        return this.add(-1, stack);
    }

    /**
     * Adds the stack to the specified slot in the player's inventory. Returns {@code false} if it's not possible to place the entire stack in the inventory.
     */
    public boolean add(int slot, ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        } else {
            try {
                if (stack.isDamaged()) {
                    if (slot == -1) {
                        slot = this.getFreeSlot();
                    }

                    if (slot >= 0) {
                        this.items.set(slot, stack.copyAndClear());
                        this.items.get(slot).setPopTime(5);
                        return true;
                    } else if (this.player.hasInfiniteMaterials()) {
                        stack.setCount(0);
                        return true;
                    } else {
                        return false;
                    }
                } else {
                    int i;
                    do {
                        i = stack.getCount();
                        if (slot == -1) {
                            stack.setCount(this.addResource(stack));
                        } else {
                            stack.setCount(this.addResource(slot, stack));
                        }
                    } while (!stack.isEmpty() && stack.getCount() < i);

                    if (stack.getCount() == i && this.player.hasInfiniteMaterials()) {
                        stack.setCount(0);
                        return true;
                    } else {
                        return stack.getCount() < i;
                    }
                }
            } catch (Throwable throwable) {
                CrashReport crashreport = CrashReport.forThrowable(throwable, "Adding item to inventory");
                CrashReportCategory crashreportcategory = crashreport.addCategory("Item being added");
                crashreportcategory.setDetail("Registry Name", () -> String.valueOf(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem())));
                crashreportcategory.setDetail("Item Class", () -> stack.getItem().getClass().getName());
                crashreportcategory.setDetail("Item ID", Item.getId(stack.getItem()));
                crashreportcategory.setDetail("Item data", stack.getDamageValue());
                crashreportcategory.setDetail("Item name", () -> stack.getHoverName().getString());
                throw new ReportedException(crashreport);
            }
        }
    }

    public void placeItemBackInInventory(ItemStack stack) {
        this.placeItemBackInInventory(stack, true);
    }

    public void placeItemBackInInventory(ItemStack stack, boolean sendPacket) {
        while (!stack.isEmpty()) {
            int i = this.getSlotWithRemainingSpace(stack);
            if (i == -1) {
                i = this.getFreeSlot();
            }

            if (i == -1) {
                this.player.drop(stack, false);
                break;
            }

            int j = stack.getMaxStackSize() - this.getItem(i).getCount();
            if (this.add(i, stack.split(j)) && sendPacket && this.player instanceof ServerPlayer serverplayer) {
                serverplayer.connection.send(this.createInventoryUpdatePacket(i));
            }
        }
    }

    public ClientboundSetPlayerInventoryPacket createInventoryUpdatePacket(int slot) {
        return new ClientboundSetPlayerInventoryPacket(slot, this.getItem(slot).copy());
    }

    /**
     * Removes up to a specified number of items from an inventory slot and returns them in a new stack.
     */
    @Override
    public ItemStack removeItem(int index, int count) {
        if (index < this.items.size()) {
            return ContainerHelper.removeItem(this.items, index, count);
        } else {
            EquipmentSlot equipmentslot = EQUIPMENT_SLOT_MAPPING.get(index);
            if (equipmentslot != null) {
                ItemStack itemstack = this.equipment.get(equipmentslot);
                if (!itemstack.isEmpty()) {
                    return itemstack.split(count);
                }
            }

            return ItemStack.EMPTY;
        }
    }

    public void removeItem(ItemStack stack) {
        for (int i = 0; i < this.items.size(); i++) {
            if (this.items.get(i) == stack) {
                this.items.set(i, ItemStack.EMPTY);
                return;
            }
        }

        for (EquipmentSlot equipmentslot : EQUIPMENT_SLOT_MAPPING.values()) {
            ItemStack itemstack = this.equipment.get(equipmentslot);
            if (itemstack == stack) {
                this.equipment.set(equipmentslot, ItemStack.EMPTY);
                return;
            }
        }
    }

    /**
     * Removes a stack from the given slot and returns it.
     */
    @Override
    public ItemStack removeItemNoUpdate(int index) {
        if (index < this.items.size()) {
            ItemStack itemstack = this.items.get(index);
            this.items.set(index, ItemStack.EMPTY);
            return itemstack;
        } else {
            EquipmentSlot equipmentslot = EQUIPMENT_SLOT_MAPPING.get(index);
            return equipmentslot != null ? this.equipment.set(equipmentslot, ItemStack.EMPTY) : ItemStack.EMPTY;
        }
    }

    /**
     * Sets the given item stack to the specified slot in the inventory (can be crafting or armor sections).
     */
    @Override
    public void setItem(int index, ItemStack stack) {
        if (index < this.items.size()) {
            this.items.set(index, stack);
        }

        EquipmentSlot equipmentslot = EQUIPMENT_SLOT_MAPPING.get(index);
        if (equipmentslot != null) {
            this.equipment.set(equipmentslot, stack);
        }
    }

    public void save(ValueOutput.TypedOutputList<ItemStackWithSlot> output) {
        for (int i = 0; i < this.items.size(); i++) {
            ItemStack itemstack = this.items.get(i);
            if (!itemstack.isEmpty()) {
                output.add(new ItemStackWithSlot(i, itemstack));
            }
        }
    }

    public void load(ValueInput.TypedInputList<ItemStackWithSlot> input) {
        this.items.clear();

        for (ItemStackWithSlot itemstackwithslot : input) {
            if (itemstackwithslot.isValidInContainer(this.items.size())) {
                this.setItem(itemstackwithslot.slot(), itemstackwithslot.stack());
            }
        }
    }

    @Override
    public int getContainerSize() {
        return this.items.size() + EQUIPMENT_SLOT_MAPPING.size();
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack itemstack : this.items) {
            if (!itemstack.isEmpty()) {
                return false;
            }
        }

        for (EquipmentSlot equipmentslot : EQUIPMENT_SLOT_MAPPING.values()) {
            if (!this.equipment.get(equipmentslot).isEmpty()) {
                return false;
            }
        }

        return true;
    }

    /**
     * Returns the stack in the given slot.
     */
    @Override
    public ItemStack getItem(int index) {
        if (index < this.items.size()) {
            return this.items.get(index);
        } else {
            EquipmentSlot equipmentslot = EQUIPMENT_SLOT_MAPPING.get(index);
            return equipmentslot != null ? this.equipment.get(equipmentslot) : ItemStack.EMPTY;
        }
    }

    @Override
    public Component getName() {
        return DEFAULT_NAME;
    }

    public void dropAll() {
        for (int i = 0; i < this.items.size(); i++) {
            ItemStack itemstack = this.items.get(i);
            if (!itemstack.isEmpty()) {
                this.player.drop(itemstack, true, false);
                this.items.set(i, ItemStack.EMPTY);
            }
        }

        this.equipment.dropAll(this.player);
    }

    @Override
    public void setChanged() {
        this.timesChanged++;
    }

    public int getTimesChanged() {
        return this.timesChanged;
    }

    /**
     * Don't rename this method to canInteractWith due to conflicts with Container
     */
    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    /**
     * Returns {@code true} if the specified {@link net.minecraft.world.item.ItemStack} exists in the inventory.
     */
    public boolean contains(ItemStack stack) {
        for (ItemStack itemstack : this) {
            if (!itemstack.isEmpty() && ItemStack.isSameItemSameComponents(itemstack, stack)) {
                return true;
            }
        }

        return false;
    }

    public boolean contains(TagKey<Item> tag) {
        for (ItemStack itemstack : this) {
            if (!itemstack.isEmpty() && itemstack.is(tag)) {
                return true;
            }
        }

        return false;
    }

    public boolean contains(Predicate<ItemStack> predicate) {
        for (ItemStack itemstack : this) {
            if (predicate.test(itemstack)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Copy the ItemStack contents from another InventoryPlayer instance
     */
    public void replaceWith(Inventory playerInventory) {
        for (int i = 0; i < this.getContainerSize(); i++) {
            this.setItem(i, playerInventory.getItem(i));
        }

        this.setSelectedSlot(playerInventory.getSelectedSlot());
    }

    @Override
    public void clearContent() {
        this.items.clear();
        this.equipment.clear();
    }

    public void fillStackedContents(StackedItemContents contents) {
        for (ItemStack itemstack : this.items) {
            contents.accountSimpleStack(itemstack);
        }
    }

    /**
     * @param removeStack Whether to remove the entire stack of items. If {@code false
     *                    }, removes a single item.
     */
    public ItemStack removeFromSelected(boolean removeStack) {
        ItemStack itemstack = this.getSelectedItem();
        return itemstack.isEmpty() ? ItemStack.EMPTY : this.removeItem(this.selected, removeStack ? itemstack.getCount() : 1);
    }
}
