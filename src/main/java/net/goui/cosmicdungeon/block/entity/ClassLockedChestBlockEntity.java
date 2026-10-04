package net.goui.cosmicdungeon.block.entity;

import net.goui.cosmicdungeon.block.custom.ClassLocked;
import net.goui.cosmicdungeon.block.custom.ClassLockedChestBlock;
import net.goui.cosmicdungeon.playerclass.api.ClassNbtUtil;
import java.util.Objects;
import javax.annotation.Nullable;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.ContainerUser;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class ClassLockedChestBlockEntity extends RandomizableContainerBlockEntity implements Container {

    private static final int SIZE = 27;
    private static final int OPEN_CLOSE_TICKS = 14;

    private static final int EVENT_OPEN = 1;
    private static final int EVENT_CLOSE = 2;

    private NonNullList<net.minecraft.world.item.ItemStack> items =
            NonNullList.withSize(SIZE, net.minecraft.world.item.ItemStack.EMPTY);

    @Nullable private ClassChestOwnership slotOwner;
    @Nullable private Component slotOwnerLabel;

    // client-only animation state
    private boolean isOpen = false;
    private float lidProgressO = 0.0F;
    private float lidProgress  = 0.0F;

    public ClassLockedChestBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CLASS_LOCKED_CHEST.get(), pos, state);
    }

    // ------------------------------------------------------------
    // Class lock check
    // ------------------------------------------------------------
    public boolean canOpen(Player player) {
        if (slotOwner != null && !slotOwner.permits(player.getUUID())) return false;
        if (player instanceof ServerPlayer sp) {
            if (net.goui.cosmicdungeon.auth.AccessPolicy.isDeveloper(sp)) return true;

            BlockState st = getBlockState();
            if (st.getBlock() instanceof ClassLocked locked) {
                String required = locked.requiredClassId();
                String have = ClassNbtUtil.getClassId(sp);
                return required == null || required.equals(have);
            }
        }
        return true;
    }

    @Nullable
    public Component getSlotOwnerLabel() { return slotOwnerLabel; }
    public boolean ownedBy(java.util.UUID id){return slotOwner!=null&&id.equals(slotOwner.playerId());}
    public boolean permitsMercenary(java.util.UUID id,String classId){
        return (slotOwner==null||slotOwner.permits(id))
                &&(!(getBlockState().getBlock() instanceof ClassLocked locked)
                ||locked.requiredClassId()==null||locked.requiredClassId().equals(classId));
    }

    private void readSlotOwner(ValueInput input) {
        slotOwner = ClassChestOwnership.load(input);
        slotOwnerLabel = slotOwner == null ? null : slotOwner.displayName();
    }

    /** Called only for newly pasted physical slot chests; never reads or rewrites inventory. */
    public void bindSlotOwner(ClassChestOwnership owner) {
        if (level == null || level.isClientSide() || !level.getServer().isSameThread())
            throw new IllegalStateException("Slot chest ownership must be assigned by the server");
        Objects.requireNonNull(owner);
        if (owner.equals(slotOwner)) return;
        slotOwner = owner;
        slotOwnerLabel = owner.displayName();
        setChanged();
        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 2);
    }

    public Component denialMessage(Player player) {
        if (slotOwner != null && !slotOwner.permits(player.getUUID())) {
            return slotOwner.playerId() == null
                    ? Component.translatable("message.cosmicdungeon.class_chest.unassigned")
                    : Component.translatable("message.cosmicdungeon.class_chest.not_owner", slotOwnerLabel);
        }
        String required = getBlockState().getBlock() instanceof ClassLocked locked ? locked.requiredClassId() : "";
        return Component.literal("Only " + required + " can open this chest.");
    }

    @Override
    public boolean canPlaceItem(int slot, net.minecraft.world.item.ItemStack stack) {
        return slotOwner == null;
    }

    @Override
    public boolean canTakeItem(Container target, int slot, net.minecraft.world.item.ItemStack stack) {
        return slotOwner == null;
    }

    /** Only owner information is sent to nearby clients; chest contents use the existing menu. */
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        var output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, registries);
        if (slotOwner != null) slotOwner.save(output);
        return output.buildResult();
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void handleUpdateTag(ValueInput input) { readSlotOwner(input); }

    @Override
    public void onDataPacket(Connection connection, ValueInput input) { readSlotOwner(input); }

    // ------------------------------------------------------------
    // Menu
    // ------------------------------------------------------------
    @Override
    protected Component getDefaultName() {
        BlockState state = getBlockState();

        if (state.getBlock() instanceof ClassLocked locked) {
            String classId = locked.requiredClassId();

            if (classId != null && !classId.isBlank()) {
                return Component.translatable(
                        "container.cosmicdungeon.class_locked_chest." + classId
                );
            }
        }

        // Fallback (should normally never show)
        return Component.translatable("container.cosmicdungeon.class_locked_chest");
    }


    @Override
    protected AbstractContainerMenu createMenu(int id, Inventory inv) {
        return ChestMenu.threeRows(id, inv, this);
    }

    @Override
    public boolean stillValid(Player player) {
        return canOpen(player) && Container.stillValidBlockEntity(this, player);
    }

    // ------------------------------------------------------------
    // OPEN / CLOSE = animation authority
    // ------------------------------------------------------------
    @Override
    public void startOpen(ContainerUser user) {
        Level level = getLevel();
        if (level == null || level.isClientSide()) return;

        LivingEntity le = user.getLivingEntity();
        if (!(le instanceof Player p)) return;
        if (!canOpen(p)) return;

        level.blockEvent(worldPosition, getBlockState().getBlock(), EVENT_OPEN, 0);

        if (getBlockState().getBlock() instanceof ClassLockedChestBlock chest) {
            ClassLockedChestBlock.playChestSound(level, worldPosition, chest.getOpenSound());
        }
    }

    @Override
    public void stopOpen(ContainerUser user) {
        Level level = getLevel();
        if (level == null || level.isClientSide()) return;

        level.blockEvent(worldPosition, getBlockState().getBlock(), EVENT_CLOSE, 0);

        if (getBlockState().getBlock() instanceof ClassLockedChestBlock chest) {
            ClassLockedChestBlock.playChestSound(level, worldPosition, chest.getCloseSound());
        }
    }

    // ------------------------------------------------------------
    // Block events (client sync)
    // ------------------------------------------------------------
    @Override
    public boolean triggerEvent(int id, int param) {
        if (id == EVENT_OPEN) {
            this.isOpen = true;
            return true;
        }
        if (id == EVENT_CLOSE) {
            this.isOpen = false;
            return true;
        }
        return super.triggerEvent(id, param);
    }

    // ------------------------------------------------------------
    // Lid animation (client)
    // ------------------------------------------------------------
    public static void clientTick(Level level, BlockPos pos, BlockState state, ClassLockedChestBlockEntity be) {
        be.lidProgressO = be.lidProgress;

        float target = be.isOpen ? 1.0F : 0.0F;
        float step = 1.0F / OPEN_CLOSE_TICKS;

        if (be.lidProgress < target) {
            be.lidProgress = Math.min(target, be.lidProgress + step);
        } else if (be.lidProgress > target) {
            be.lidProgress = Math.max(target, be.lidProgress - step);
        }
    }

    public float getLidProgress(float partialTick) {
        return this.lidProgressO + (this.lidProgress - this.lidProgressO) * partialTick;
    }


    // ------------------------------------------------------------
    // Inventory persistence and optional slot ownership (legacy items preserved)
    // ------------------------------------------------------------
    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.items = NonNullList.withSize(SIZE, net.minecraft.world.item.ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input, this.items);
        readSlotOwner(input);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, this.items);
        if (slotOwner != null) slotOwner.save(output);
    }

    @Override
    public int getContainerSize() {
        return SIZE;
    }

    @Override
    protected NonNullList<net.minecraft.world.item.ItemStack> getItems() {
        return items;
    }

    @Override
    protected void setItems(NonNullList<net.minecraft.world.item.ItemStack> items) {
        this.items = items;
    }
}
