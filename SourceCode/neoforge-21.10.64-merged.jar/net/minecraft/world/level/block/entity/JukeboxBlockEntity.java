package net.minecraft.world.level.block.entity;

import com.google.common.annotations.VisibleForTesting;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.Container;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.JukeboxSong;
import net.minecraft.world.item.JukeboxSongPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.JukeboxBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.ticks.ContainerSingleItem;

public class JukeboxBlockEntity extends BlockEntity implements ContainerSingleItem.BlockContainerSingleItem {
    public static final String SONG_ITEM_TAG_ID = "RecordItem";
    public static final String TICKS_SINCE_SONG_STARTED_TAG_ID = "ticks_since_song_started";
    private ItemStack item = ItemStack.EMPTY;
    private final JukeboxSongPlayer jukeboxSongPlayer = new JukeboxSongPlayer(this::onSongChanged, this.getBlockPos());

    public JukeboxBlockEntity(BlockPos pos, BlockState blockState) {
        super(BlockEntityType.JUKEBOX, pos, blockState);
    }

    public JukeboxSongPlayer getSongPlayer() {
        return this.jukeboxSongPlayer;
    }

    public void onSongChanged() {
        this.level.updateNeighborsAt(this.getBlockPos(), this.getBlockState().getBlock());
        this.setChanged();
    }

    private void notifyItemChangedInJukebox(boolean hasRecord) {
        if (this.level != null && this.level.getBlockState(this.getBlockPos()) == this.getBlockState()) {
            this.level.setBlock(this.getBlockPos(), this.getBlockState().setValue(JukeboxBlock.HAS_RECORD, hasRecord), 2);
            this.level.gameEvent(GameEvent.BLOCK_CHANGE, this.getBlockPos(), GameEvent.Context.of(this.getBlockState()));
        }
    }

    public void popOutTheItem() {
        if (this.level != null && !this.level.isClientSide()) {
            BlockPos blockpos = this.getBlockPos();
            ItemStack itemstack = this.getTheItem();
            if (!itemstack.isEmpty()) {
                this.removeTheItem();
                Vec3 vec3 = Vec3.atLowerCornerWithOffset(blockpos, 0.5, 1.01, 0.5).offsetRandom(this.level.random, 0.7F);
                ItemStack itemstack1 = itemstack.copy();
                ItemEntity itementity = new ItemEntity(this.level, vec3.x(), vec3.y(), vec3.z(), itemstack1);
                itementity.setDefaultPickUpDelay();
                this.level.addFreshEntity(itementity);
            }
        }
    }

    public static void tick(Level level, BlockPos pos, BlockState state, JukeboxBlockEntity jukebox) {
        jukebox.jukeboxSongPlayer.tick(level, state);
    }

    public int getComparatorOutput() {
        return JukeboxSong.fromStack(this.level.registryAccess(), this.item).map(Holder::value).map(JukeboxSong::comparatorOutput).orElse(0);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        ItemStack itemstack = input.read("RecordItem", ItemStack.CODEC).orElse(ItemStack.EMPTY);
        if (!this.item.isEmpty() && !ItemStack.isSameItemSameComponents(itemstack, this.item)) {
            this.jukeboxSongPlayer.stop(this.level, this.getBlockState());
        }

        this.item = itemstack;
        input.getLong("ticks_since_song_started")
            .ifPresent(
                p_421419_ -> JukeboxSong.fromStack(input.lookup(), this.item)
                    .ifPresent(p_409491_ -> this.jukeboxSongPlayer.setSongWithoutPlaying((Holder<JukeboxSong>)p_409491_, p_421419_))
            );
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (!this.getTheItem().isEmpty()) {
            output.store("RecordItem", ItemStack.CODEC, this.getTheItem());
        }

        if (this.jukeboxSongPlayer.getSong() != null) {
            output.putLong("ticks_since_song_started", this.jukeboxSongPlayer.getTicksSinceSongStarted());
        }
    }

    @Override
    public ItemStack getTheItem() {
        return this.item;
    }

    @Override
    public ItemStack splitTheItem(int amount) {
        ItemStack itemstack = this.item;
        this.setTheItem(ItemStack.EMPTY);
        return itemstack;
    }

    // Neo: Support deferring non-transactional notification and song player update until the end of a transaction
    private final net.neoforged.neoforge.transfer.transaction.RootCommitJournal itemChangedJournal =
            new net.neoforged.neoforge.transfer.transaction.RootCommitJournal(() -> {
                if (!isRemoved()) {
                    itemChanged();
                }
            });
    @Override
    public void onTransfer(int slot, int amountChange, net.neoforged.neoforge.transfer.transaction.TransactionContext transaction) {
        itemChangedJournal.updateSnapshots(transaction);
    }

    @Override
    public void setItem(int slot, ItemStack stack, boolean insideTransaction) {
        if (slot == 0) {
            if (insideTransaction) {
                this.item = stack;
            } else {
                setTheItem(stack);
            }
        }
    }
    @Override
    public void setTheItem(ItemStack item) {
        this.item = item;
        itemChanged();
    }
    private void itemChanged() {
        boolean flag = !this.item.isEmpty();
        Optional<Holder<JukeboxSong>> optional = JukeboxSong.fromStack(this.level.registryAccess(), this.item);
        this.notifyItemChangedInJukebox(flag);
        if (flag && optional.isPresent()) {
            this.jukeboxSongPlayer.play(this.level, optional.get());
        } else {
            this.jukeboxSongPlayer.stop(this.level, this.getBlockState());
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        this.level.gameEvent(GameEvent.JUKEBOX_STOP_PLAY, this.getBlockPos(), GameEvent.Context.of(this.getBlockState()));
        this.level.levelEvent(1011, this.getBlockPos(), 0);
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public BlockEntity getContainerBlockEntity() {
        return this;
    }

    /**
     * Returns {@code true} if automation is allowed to insert the given stack (ignoring stack size) into the given slot. For guis use Slot.isItemValid
     */
    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return stack.has(DataComponents.JUKEBOX_PLAYABLE) && this.getItem(slot).isEmpty();
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
        return target.hasAnyMatching(ItemStack::isEmpty);
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        this.popOutTheItem();
    }

    @VisibleForTesting
    public void setSongItemWithoutPlaying(ItemStack stack) {
        this.item = stack;
        JukeboxSong.fromStack(this.level.registryAccess(), stack)
            .ifPresent(p_350672_ -> this.jukeboxSongPlayer.setSongWithoutPlaying((Holder<JukeboxSong>)p_350672_, 0L));
        this.level.updateNeighborsAt(this.getBlockPos(), this.getBlockState().getBlock());
        this.setChanged();
    }

    @VisibleForTesting
    public void tryForcePlaySong() {
        JukeboxSong.fromStack(this.level.registryAccess(), this.getTheItem())
            .ifPresent(p_350319_ -> this.jukeboxSongPlayer.play(this.level, (Holder<JukeboxSong>)p_350319_));
    }
}
