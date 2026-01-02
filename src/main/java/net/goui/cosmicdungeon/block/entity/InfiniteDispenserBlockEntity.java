package net.goui.cosmicdungeon.block.entity;

import net.goui.cosmicdungeon.CosmicDungeonMod;
import net.goui.cosmicdungeon.util.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderSet;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DispenserMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class InfiniteDispenserBlockEntity extends BaseContainerBlockEntity implements net.minecraft.world.RandomizableContainer {
    private static final int SLOTS = 9;
    private static final ItemStack FAKE_BOW = new ItemStack(Items.BOW);

    private NonNullList<ItemStack> items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);

    @Nullable private ResourceKey<LootTable> lootTable;
    private long lootTableSeed;

    public InfiniteDispenserBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.INFINITE_DISPENSER.get(), pos, state);
    }

    // ---------- Container ----------
    @Override public int getContainerSize() { return SLOTS; }
    @Override protected NonNullList<ItemStack> getItems() { return items; }
    @Override protected void setItems(NonNullList<ItemStack> items) { this.items = items; }

    // ---------- Menu / name ----------
    @Override
    protected AbstractContainerMenu createMenu(int id, Inventory inv) {
        unpackLootTable(inv.player);
        return new DispenserMenu(id, inv, this);
    }

    @Override
    protected net.minecraft.network.chat.Component getDefaultName() {
        return net.minecraft.network.chat.Component.translatable("container." + CosmicDungeonMod.MOD_ID + ".infinite_dispenser");
    }

    // ---------- Save / load ----------
    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        if (!tryLoadLootTable(input)) {
            this.items = NonNullList.withSize(getContainerSize(), ItemStack.EMPTY);
            ContainerHelper.loadAllItems(input, this.items);
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (!trySaveLootTable(output)) {
            ContainerHelper.saveAllItems(output, this.items);
        }
    }

    // ---------- Lootable ----------
    @Override public @Nullable ResourceKey<LootTable> getLootTable() { return lootTable; }
    @Override public void setLootTable(@Nullable ResourceKey<LootTable> key) { this.lootTable = key; }
    @Override public long getLootTableSeed() { return lootTableSeed; }
    @Override public void setLootTableSeed(long seed) { this.lootTableSeed = seed; }

    // ---------- Queries ----------
    public int findFirstShootableSlot() {
        unpackLootTable(null);
        for (int i = 0; i < items.size(); i++) {
            ItemStack s = items.get(i);
            if (!s.isEmpty() && isShootable(level, s)) {
                return i;
            }
        }
        return -1;
    }

    // ---------- Action: shoot (no consumption) ----------
    public void shootStack(Level level, BlockPos pos, net.minecraft.core.Direction facing, ItemStack stack) {
        if (level.isClientSide()) return;

        Vec3 dir    = facing.getUnitVec3();
        Vec3 origin = Vec3.atCenterOf(pos).add(dir.scale(0.7));
        double power = 1.1;

        try {
            if (stack.is(Items.ARROW) || stack.is(Items.TIPPED_ARROW) || stack.is(Items.SPECTRAL_ARROW)) {
                ItemStack pickup = stack.copyWithCount(1);
                Arrow arrow = new Arrow(level, origin.x, origin.y, origin.z, pickup, FAKE_BOW);
                arrow.pickup = AbstractArrow.Pickup.DISALLOWED;
                arrow.setDeltaMovement(dir.scale(power));
                level.addFreshEntity(arrow);
                playShoot(level, pos);
                return;
            }
        } catch (Throwable ignored) {}

        playFail(level, pos);
    }

    public void dropAllContents(Level level, BlockPos pos) {
        if (level.isClientSide()) return;
        for (ItemStack s : items) {
            if (!s.isEmpty()) {
                level.addFreshEntity(new ItemEntity(level,
                        pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, s));
            }
        }
        items.clear();
        setChanged();
    }

    private void playShoot(Level level, BlockPos pos) {
        level.levelEvent(1000, pos, 0); // click
        level.levelEvent(2000, pos, 0); // puff
    }

    private void playFail(Level level, BlockPos pos) {
        level.levelEvent(1001, pos, 0); // fail click
    }

    private static boolean isShootable(Level level, ItemStack s) {
        if (s.isEmpty()) return false;
        try {
            var lookup = level.registryAccess().lookupOrThrow(Registries.ITEM);
            HolderSet<Item> set = lookup.getOrThrow(ModTags.Items.INFINITE_SHOOTABLES);
            if (set.size() > 0) {
                return s.is(ModTags.Items.INFINITE_SHOOTABLES);
            }
        } catch (Throwable ignored) {}
        return s.is(Items.ARROW) || s.is(Items.TIPPED_ARROW) || s.is(Items.SPECTRAL_ARROW);
    }
}
