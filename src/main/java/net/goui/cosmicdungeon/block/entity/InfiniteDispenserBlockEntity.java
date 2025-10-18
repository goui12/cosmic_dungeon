package net.goui.cosmicdungeon.block.entity;

import net.goui.cosmicdungeon.CosmicDungeonMod;
import net.goui.cosmicdungeon.util.ModTags;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DispenserMenu;
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
    private NonNullList<ItemStack>  items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);

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
        debug(level, worldPosition, "Menu created (id=" + id + ")");
        return new DispenserMenu(id, inv, this);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container." + CosmicDungeonMod.MOD_ID + ".infinite_dispenser");
    }

    // ---------- Save / load ----------
    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        if (!tryLoadLootTable(input)) {
            this.items = NonNullList.withSize(getContainerSize(), ItemStack.EMPTY);
            ContainerHelper.loadAllItems(input, this.items);
        }
        debug(level, worldPosition, "Loaded NBT. LootTable=" + (lootTable != null) + " ItemsPresent=" + items.stream().anyMatch(s -> !s.isEmpty()));
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (!trySaveLootTable(output)) {
            ContainerHelper.saveAllItems(output, this.items);
        }
        debug(level, worldPosition, "Saved NBT. LootTable=" + (lootTable != null));
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
            if (!s.isEmpty()) {
                debug(level, worldPosition, "Check slot " + i + ": " + s.getHoverName().getString() + (s.is(ModTags.Items.INFINITE_SHOOTABLES) ? " [IN TAG]" : " [not in tag]"));
                if (s.is(ModTags.Items.INFINITE_SHOOTABLES)) return i;
            } else {
                debug(level, worldPosition, "Check slot " + i + ": empty");
            }
        }
        debug(level, worldPosition, "No shootable item found.");
        return -1;
    }

    // ---------- Action: shoot (no consumption) ----------
    public void shootStack(Level level, BlockPos pos, net.minecraft.core.Direction facing, ItemStack stack) {
        if (level.isClientSide()) return;

        String name = stack.getHoverName().getString();
        debug(level, pos, "Attempt shoot: " + name + " | Facing=" + facing);

        // We only support arrow-type items. Keep it simple: spawn a vanilla Arrow for all.
        // (Spectral/tipped will not carry special effects here — intentional simplification.)
        Vec3 dir = facing.getUnitVec3();
        Vec3 origin = Vec3.atCenterOf(pos).add(dir.scale(0.7));
        double power = 1.1;

        try {
            // Accept only these items (must also be in the tag):
// --- inside shootStack(...) right where you construct the arrow ---
            if (stack.is(Items.ARROW) || stack.is(Items.TIPPED_ARROW) || stack.is(Items.SPECTRAL_ARROW)) {
                // Spawn at the computed origin using the position-based ctor
                Arrow arrow = new Arrow(level, origin.x, origin.y, origin.z, stack.copyWithCount(1), ItemStack.EMPTY);

                // infinite pickup
                arrow.pickup = net.minecraft.world.entity.projectile.AbstractArrow.Pickup.DISALLOWED;

                // velocity & orientation
                arrow.setDeltaMovement(dir.scale(power));
                arrow.setYRot((float)(Math.toDegrees(Math.atan2(dir.z, dir.x)) - 90.0));
                arrow.setXRot((float)(-Math.toDegrees(Math.atan2(
                        dir.y, Math.sqrt(dir.x * dir.x + dir.z * dir.z)))));

                level.addFreshEntity(arrow);
                playShoot(level, pos);
                debug(level, pos, "Spawned projectile: Arrow (from " + name + ")");
                return;
            }

        } catch (Throwable t) {
            debug(level, pos, "Projectile spawn threw: " + t.getClass().getSimpleName());
        }

        // If we get here, item was in tag but we didn't spawn a projectile (shouldn't happen if tag is arrows only).
        playFail(level, pos);
        debug(level, pos, "Projectile spawn failed for: " + name + " — played fail event.");
    }

    public void dropAllContents(Level level, BlockPos pos) {
        if (level.isClientSide()) return;
        int count = 0;
        for (ItemStack s : items) {
            if (!s.isEmpty()) {
                count += s.getCount();
                level.addFreshEntity(new ItemEntity(level,
                        pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, s));
            }
        }
        items.clear();
        setChanged();
        debug(level, pos, "Dropped all contents (" + count + " items).");
    }

    private void playShoot(Level level, BlockPos pos) {
        level.levelEvent(1000, pos, 0); // click
        level.levelEvent(2000, pos, 0); // puff
        debug(level, pos, "Played shoot SFX/particles.");
    }

    private void playFail(Level level, BlockPos pos) {
        level.levelEvent(1001, pos, 0); // fail click
        debug(level, pos, "Played fail SFX.");
    }

    /* ---------- chat debug helper ---------- */
    private static void debug(Level level, BlockPos pos, String msg) {
        if (level == null || level.isClientSide()) return;
        String prefixed = "[InfiniteDispenser/BE] " + msg + " @ " + pos.getX() + "," + pos.getY() + "," + pos.getZ();
        if (level instanceof ServerLevel sl) {
            for (ServerPlayer sp : sl.players()) {
                double dx = sp.getX() - (pos.getX() + 0.5);
                double dy = sp.getY() - (pos.getY() + 0.5);
                double dz = sp.getZ() - (pos.getZ() + 0.5);
                if ((dx*dx + dy*dy + dz*dz) <= (48 * 48)) {
                    sp.sendSystemMessage(Component.literal(prefixed).withStyle(ChatFormatting.DARK_AQUA));
                }
            }
        }
    }
}
