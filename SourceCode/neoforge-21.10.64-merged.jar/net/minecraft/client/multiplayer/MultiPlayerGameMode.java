package net.minecraft.client.multiplayer;

import com.google.common.collect.Lists;
import com.google.common.primitives.Shorts;
import com.google.common.primitives.SignedBytes;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;
import net.minecraft.SharedConstants;
import net.minecraft.client.ClientRecipeBook;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.multiplayer.prediction.BlockStatePredictionHandler;
import net.minecraft.client.multiplayer.prediction.PredictiveAction;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.HashedStack;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerGamePacketListener;
import net.minecraft.network.protocol.game.ServerboundContainerButtonClickPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.network.protocol.game.ServerboundContainerSlotStateChangedPacket;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundPickItemFromBlockPacket;
import net.minecraft.network.protocol.game.ServerboundPickItemFromEntityPacket;
import net.minecraft.network.protocol.game.ServerboundPlaceRecipePacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.network.protocol.game.ServerboundSetCreativeModeSlotPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.StatsCounter;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HasCustomInventoryScreen;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.display.RecipeDisplayId;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.GameMasterBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.apache.commons.lang3.mutable.MutableObject;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class MultiPlayerGameMode {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final Minecraft minecraft;
    private final ClientPacketListener connection;
    private BlockPos destroyBlockPos = new BlockPos(-1, -1, -1);
    private ItemStack destroyingItem = ItemStack.EMPTY;
    private float destroyProgress;
    private float destroyTicks;
    private int destroyDelay;
    private boolean isDestroying;
    private GameType localPlayerMode = GameType.DEFAULT_MODE;
    @Nullable
    private GameType previousLocalPlayerMode;
    private int carriedIndex;

    public MultiPlayerGameMode(Minecraft minecraft, ClientPacketListener connection) {
        this.minecraft = minecraft;
        this.connection = connection;
    }

    /**
     * Sets player capabilities depending on current gametype.
     */
    public void adjustPlayer(Player player) {
        this.localPlayerMode.updatePlayerAbilities(player.getAbilities());
    }

    public void setLocalMode(GameType localPlayerMode, @Nullable GameType previousLocalPlayerMode) {
        this.localPlayerMode = localPlayerMode;
        this.previousLocalPlayerMode = previousLocalPlayerMode;
        this.localPlayerMode.updatePlayerAbilities(this.minecraft.player.getAbilities());
    }

    /**
     * Sets the game type for the player.
     */
    public void setLocalMode(GameType type) {
        if (type != this.localPlayerMode) {
            this.previousLocalPlayerMode = this.localPlayerMode;
        }

        this.localPlayerMode = type;
        this.localPlayerMode.updatePlayerAbilities(this.minecraft.player.getAbilities());
    }

    public boolean canHurtPlayer() {
        return this.localPlayerMode.isSurvival();
    }

    public boolean destroyBlock(BlockPos pos) {
        if (this.minecraft.player.blockActionRestricted(this.minecraft.level, pos, this.localPlayerMode)) {
            return false;
        } else {
            Level level = this.minecraft.level;
            BlockState blockstate = level.getBlockState(pos);
            if (!this.minecraft.player.getMainHandItem().canDestroyBlock(blockstate, level, pos, this.minecraft.player)) {
                return false;
            } else {
                Block block = blockstate.getBlock();
                if (block instanceof GameMasterBlock && !this.minecraft.player.canUseGameMasterBlocks()) {
                    return false;
                } else if (blockstate.isAir()) {
                    return false;
                } else {
                    BlockState removedBlockState =
                    block.playerWillDestroy(level, pos, blockstate, this.minecraft.player);
                    FluidState fluidstate = level.getFluidState(pos);
                    boolean flag = blockstate.onDestroyedByPlayer(level, pos, minecraft.player, minecraft.player.getMainHandItem().copy(), false, fluidstate);
                    if (flag) {
                        block.destroy(level, pos, removedBlockState);
                    }

                    if (SharedConstants.DEBUG_BLOCK_BREAK) {
                        LOGGER.error("client broke {} {} -> {}", pos, blockstate, level.getBlockState(pos));
                    }

                    return flag;
                }
            }
        }
    }

    /**
     * Called when the player is hitting a block with an item.
     */
    public boolean startDestroyBlock(BlockPos loc, Direction face) {
        if (this.minecraft.player.blockActionRestricted(this.minecraft.level, loc, this.localPlayerMode)) {
            return false;
        } else if (!this.minecraft.level.getWorldBorder().isWithinBounds(loc)) {
            return false;
        } else {
            if (this.minecraft.player.getAbilities().instabuild) {
                BlockState blockstate = this.minecraft.level.getBlockState(loc);
                this.minecraft.getTutorial().onDestroyBlock(this.minecraft.level, loc, blockstate, 1.0F);
                if (SharedConstants.DEBUG_BLOCK_BREAK) {
                    LOGGER.info("Creative start {} {}", loc, blockstate);
                }

                this.startPrediction(this.minecraft.level, p_233757_ -> {
                    if (!net.neoforged.neoforge.common.CommonHooks.onLeftClickBlock(this.minecraft.player, loc, face, ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK).isCanceled())
                    this.destroyBlock(loc);
                    return new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK, loc, face, p_233757_);
                });
                this.destroyDelay = 5;
            } else if (!this.isDestroying || !this.sameDestroyTarget(loc)) {
                if (this.isDestroying) {
                    if (SharedConstants.DEBUG_BLOCK_BREAK) {
                        LOGGER.info("Abort old break {} {}", loc, this.minecraft.level.getBlockState(loc));
                    }

                    this.connection
                        .send(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK, this.destroyBlockPos, face));
                }
                net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.LeftClickBlock event = net.neoforged.neoforge.common.CommonHooks.onLeftClickBlock(this.minecraft.player, loc, face, ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK);

                BlockState blockstate1 = this.minecraft.level.getBlockState(loc);
                this.minecraft.getTutorial().onDestroyBlock(this.minecraft.level, loc, blockstate1, 0.0F);
                if (SharedConstants.DEBUG_BLOCK_BREAK) {
                    LOGGER.info("Start break {} {}", loc, blockstate1);
                }

                this.startPrediction(this.minecraft.level, p_233728_ -> {
                    boolean flag = !blockstate1.isAir();
                    if (flag && this.destroyProgress == 0.0F) {
                        if (event.getUseBlock() != net.minecraft.util.TriState.FALSE)
                        blockstate1.attack(this.minecraft.level, loc, this.minecraft.player);
                    }

                    ServerboundPlayerActionPacket packet = new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK, loc, face, p_233728_);
                    if (event.getUseItem().isFalse()) return packet;
                    if (flag && blockstate1.getDestroyProgress(this.minecraft.player, this.minecraft.player.level(), loc) >= 1.0F) {
                        this.destroyBlock(loc);
                    } else {
                        this.isDestroying = true;
                        this.destroyBlockPos = loc;
                        this.destroyingItem = this.minecraft.player.getMainHandItem();
                        this.destroyProgress = 0.0F;
                        this.destroyTicks = 0.0F;
                        this.minecraft.level.destroyBlockProgress(this.minecraft.player.getId(), this.destroyBlockPos, this.getDestroyStage());
                    }

                    return packet;
                });
            }

            return true;
        }
    }

    public void stopDestroyBlock() {
        if (this.isDestroying) {
            BlockState blockstate = this.minecraft.level.getBlockState(this.destroyBlockPos);
            this.minecraft.getTutorial().onDestroyBlock(this.minecraft.level, this.destroyBlockPos, blockstate, -1.0F);
            if (SharedConstants.DEBUG_BLOCK_BREAK) {
                LOGGER.info("Stop dest {} {}", this.destroyBlockPos, blockstate);
            }

            this.connection
                .send(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK, this.destroyBlockPos, Direction.DOWN));
            this.isDestroying = false;
            this.destroyProgress = 0.0F;
            this.minecraft.level.destroyBlockProgress(this.minecraft.player.getId(), this.destroyBlockPos, -1);
            this.minecraft.player.resetAttackStrengthTicker();
        }
    }

    public boolean continueDestroyBlock(BlockPos posBlock, Direction directionFacing) {
        this.ensureHasSentCarriedItem();
        if (this.destroyDelay > 0) {
            this.destroyDelay--;
            return true;
        } else if (this.minecraft.player.getAbilities().instabuild && this.minecraft.level.getWorldBorder().isWithinBounds(posBlock)) {
            this.destroyDelay = 5;
            BlockState blockstate1 = this.minecraft.level.getBlockState(posBlock);
            this.minecraft.getTutorial().onDestroyBlock(this.minecraft.level, posBlock, blockstate1, 1.0F);
            if (SharedConstants.DEBUG_BLOCK_BREAK) {
                LOGGER.info("Creative cont {} {}", posBlock, blockstate1);
            }

            this.startPrediction(this.minecraft.level, p_233753_ -> {
                if (!net.neoforged.neoforge.common.CommonHooks.onLeftClickBlock(this.minecraft.player, posBlock, directionFacing, ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK).isCanceled())
                this.destroyBlock(posBlock);
                return new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK, posBlock, directionFacing, p_233753_);
            });
            return true;
        } else if (this.sameDestroyTarget(posBlock)) {
            BlockState blockstate = this.minecraft.level.getBlockState(posBlock);
            if (blockstate.isAir()) {
                this.isDestroying = false;
                return false;
            } else {
                this.destroyProgress = this.destroyProgress + blockstate.getDestroyProgress(this.minecraft.player, this.minecraft.player.level(), posBlock);
                if (this.destroyTicks % 4.0F == 0.0F && !net.neoforged.neoforge.client.extensions.common.IClientBlockExtensions.of(blockstate).playHitSound(blockstate, this.minecraft.level, posBlock, directionFacing, this.minecraft.getSoundManager())) {
                    SoundType soundtype = blockstate.getSoundType(this.minecraft.level, posBlock, this.minecraft.player);
                    this.minecraft
                        .getSoundManager()
                        .play(
                            new SimpleSoundInstance(
                                soundtype.getHitSound(),
                                SoundSource.BLOCKS,
                                (soundtype.getVolume() + 1.0F) / 8.0F,
                                soundtype.getPitch() * 0.5F,
                                SoundInstance.createUnseededRandom(),
                                posBlock
                            )
                        );
                }

                this.destroyTicks++;
                this.minecraft.getTutorial().onDestroyBlock(this.minecraft.level, posBlock, blockstate, Mth.clamp(this.destroyProgress, 0.0F, 1.0F));
                if (net.neoforged.neoforge.common.CommonHooks.onClientMineHold(this.minecraft.player, posBlock, directionFacing).getUseItem().isFalse()) return true;
                if (this.destroyProgress >= 1.0F) {
                    this.isDestroying = false;
                    if (SharedConstants.DEBUG_BLOCK_BREAK) {
                        LOGGER.info("Finished breaking {} {}", posBlock, blockstate);
                    }

                    this.startPrediction(this.minecraft.level, p_233739_ -> {
                        this.destroyBlock(posBlock);
                        return new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK, posBlock, directionFacing, p_233739_);
                    });
                    this.destroyProgress = 0.0F;
                    this.destroyTicks = 0.0F;
                    this.destroyDelay = 5;
                }

                this.minecraft.level.destroyBlockProgress(this.minecraft.player.getId(), this.destroyBlockPos, this.getDestroyStage());
                return true;
            }
        } else {
            return this.startDestroyBlock(posBlock, directionFacing);
        }
    }

    private void startPrediction(ClientLevel level, PredictiveAction action) {
        try (BlockStatePredictionHandler blockstatepredictionhandler = level.getBlockStatePredictionHandler().startPredicting()) {
            int i = blockstatepredictionhandler.currentSequence();
            Packet<ServerGamePacketListener> packet = action.predict(i);
            this.connection.send(packet);
        }
    }

    public void tick() {
        this.ensureHasSentCarriedItem();
        if (this.connection.getConnection().isConnected()) {
            this.connection.getConnection().tick();
        } else {
            this.connection.getConnection().handleDisconnection();
        }
    }

    private boolean sameDestroyTarget(BlockPos pos) {
        ItemStack itemstack = this.minecraft.player.getMainHandItem();
        return pos.equals(this.destroyBlockPos) && !destroyingItem.shouldCauseBlockBreakReset(itemstack);
    }

    private void ensureHasSentCarriedItem() {
        int i = this.minecraft.player.getInventory().getSelectedSlot();
        if (i != this.carriedIndex) {
            this.carriedIndex = i;
            this.connection.send(new ServerboundSetCarriedItemPacket(this.carriedIndex));
        }
    }

    public InteractionResult useItemOn(LocalPlayer player, InteractionHand hand, BlockHitResult result) {
        this.ensureHasSentCarriedItem();
        if (!this.minecraft.level.getWorldBorder().isWithinBounds(result.getBlockPos())) {
            return InteractionResult.FAIL;
        } else {
            MutableObject<InteractionResult> mutableobject = new MutableObject<>();
            this.startPrediction(this.minecraft.level, p_233745_ -> {
                mutableobject.setValue(this.performUseItemOn(player, hand, result));
                return new ServerboundUseItemOnPacket(hand, result, p_233745_);
            });
            return mutableobject.getValue();
        }
    }

    private InteractionResult performUseItemOn(LocalPlayer player, InteractionHand hand, BlockHitResult p_result) {
        BlockPos blockpos = p_result.getBlockPos();
        ItemStack itemstack = player.getItemInHand(hand);
        net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock event = net.neoforged.neoforge.common.CommonHooks.onRightClickBlock(player, hand, blockpos, p_result);
        if (event.isCanceled()) {
            return event.getCancellationResult();
        }
        if (this.localPlayerMode == GameType.SPECTATOR) {
            return InteractionResult.CONSUME;
        } else {
            UseOnContext useoncontext = new UseOnContext(player, hand, p_result);
            if (event.getUseItem() != net.minecraft.util.TriState.FALSE) {
                InteractionResult result = itemstack.onItemUseFirst(useoncontext);
                if (result != InteractionResult.PASS) {
                    return result;
                }
            }
            boolean flag = !player.getMainHandItem().doesSneakBypassUse(player.level(), blockpos, player) || !player.getOffhandItem().doesSneakBypassUse(player.level(), blockpos, player);
            boolean flag1 = player.isSecondaryUseActive() && flag;
            if (event.getUseBlock().isTrue() || (event.getUseBlock().isDefault() && !flag1)) {
                BlockState blockstate = this.minecraft.level.getBlockState(blockpos);
                if (!this.connection.isFeatureEnabled(blockstate.getBlock().requiredFeatures())) {
                    return InteractionResult.FAIL;
                }

                InteractionResult interactionresult = blockstate.useItemOn(
                    player.getItemInHand(hand), this.minecraft.level, player, hand, p_result
                );
                if (interactionresult.consumesAction()) {
                    return interactionresult;
                }

                if (interactionresult instanceof InteractionResult.TryEmptyHandInteraction && hand == InteractionHand.MAIN_HAND) {
                    InteractionResult interactionresult1 = blockstate.useWithoutItem(this.minecraft.level, player, p_result);
                    if (interactionresult1.consumesAction()) {
                        return interactionresult1;
                    }
                }
            }

            if (event.getUseItem().isFalse()) {
                return InteractionResult.PASS;
            }
            if (event.getUseItem().isTrue() || (!itemstack.isEmpty() && !player.getCooldowns().isOnCooldown(itemstack))) {
                InteractionResult interactionresult2;
                if (player.hasInfiniteMaterials()) {
                    int i = itemstack.getCount();
                    interactionresult2 = itemstack.useOn(useoncontext);
                    itemstack.setCount(i);
                } else {
                    interactionresult2 = itemstack.useOn(useoncontext);
                }

                return interactionresult2;
            } else {
                return InteractionResult.PASS;
            }
        }
    }

    public InteractionResult useItem(Player player, InteractionHand hand) {
        if (this.localPlayerMode == GameType.SPECTATOR) {
            return InteractionResult.PASS;
        } else {
            this.ensureHasSentCarriedItem();
            MutableObject<InteractionResult> mutableobject = new MutableObject<>();
            this.startPrediction(
                this.minecraft.level,
                p_359151_ -> {
                    ServerboundUseItemPacket serverbounduseitempacket = new ServerboundUseItemPacket(
                        hand, p_359151_, player.getYRot(), player.getXRot()
                    );
                    ItemStack itemstack = player.getItemInHand(hand);
                    if (player.getCooldowns().isOnCooldown(itemstack)) {
                        mutableobject.setValue(InteractionResult.PASS);
                        return serverbounduseitempacket;
                    } else {
                        InteractionResult cancelResult = net.neoforged.neoforge.common.CommonHooks.onItemRightClick(player, hand);
                        if (cancelResult != null) {
                            mutableobject.setValue(cancelResult);
                            return serverbounduseitempacket;
                        }
                        InteractionResult interactionresult = itemstack.use(this.minecraft.level, player, hand);
                        ItemStack itemstack1;
                        if (interactionresult instanceof InteractionResult.Success interactionresult$success) {
                            itemstack1 = Objects.requireNonNullElseGet(
                                interactionresult$success.heldItemTransformedTo(), () -> player.getItemInHand(hand)
                            );
                        } else {
                            itemstack1 = player.getItemInHand(hand);
                        }

                        if (itemstack1 != itemstack) {
                            player.setItemInHand(hand, itemstack1);
                            if (itemstack1.isEmpty())
                                net.neoforged.neoforge.event.EventHooks.onPlayerDestroyItem(player, itemstack, hand);
                        }

                        mutableobject.setValue(interactionresult);
                        return serverbounduseitempacket;
                    }
                }
            );
            return mutableobject.getValue();
        }
    }

    public LocalPlayer createPlayer(ClientLevel level, StatsCounter stats, ClientRecipeBook recipeBook) {
        return this.createPlayer(level, stats, recipeBook, Input.EMPTY, false);
    }

    public LocalPlayer createPlayer(ClientLevel level, StatsCounter stats, ClientRecipeBook recipeBook, Input lastSentInput, boolean wasSprinting) {
        return new LocalPlayer(this.minecraft, level, this.connection, stats, recipeBook, lastSentInput, wasSprinting);
    }

    /**
     * Attacks an entity
     */
    public void attack(Player player, Entity targetEntity) {
        this.ensureHasSentCarriedItem();
        this.connection.send(ServerboundInteractPacket.createAttackPacket(targetEntity, player.isShiftKeyDown()));
        if (this.localPlayerMode != GameType.SPECTATOR) {
            player.attack(targetEntity);
            player.resetAttackStrengthTicker();
        }
    }

    /**
     * Handles right-clicking an entity, sends a packet to the server.
     */
    public InteractionResult interact(Player player, Entity target, InteractionHand hand) {
        this.ensureHasSentCarriedItem();
        this.connection.send(ServerboundInteractPacket.createInteractionPacket(target, player.isShiftKeyDown(), hand));
        return (InteractionResult)(this.localPlayerMode == GameType.SPECTATOR ? InteractionResult.PASS : player.interactOn(target, hand));
    }

    /**
     * Handles right-clicking an entity from the entities side, sends a packet to the server.
     */
    public InteractionResult interactAt(Player player, Entity target, EntityHitResult ray, InteractionHand hand) {
        this.ensureHasSentCarriedItem();
        Vec3 vec3 = ray.getLocation().subtract(target.getX(), target.getY(), target.getZ());
        this.connection.send(ServerboundInteractPacket.createInteractionPacket(target, player.isShiftKeyDown(), hand, vec3));
        if (this.localPlayerMode == GameType.SPECTATOR) return InteractionResult.PASS; // don't fire for spectators to match non-specific EntityInteract
        InteractionResult cancelResult = net.neoforged.neoforge.common.CommonHooks.onInteractEntityAt(player, target, ray, hand);
        if(cancelResult != null) return cancelResult;
        return (InteractionResult)(this.localPlayerMode == GameType.SPECTATOR ? InteractionResult.PASS : target.interactAt(player, vec3, hand));
    }

    public void handleInventoryMouseClick(int containerId, int slotId, int mouseButton, ClickType clickType, Player player) {
        AbstractContainerMenu abstractcontainermenu = player.containerMenu;
        if (containerId != abstractcontainermenu.containerId) {
            LOGGER.warn("Ignoring click in mismatching container. Click in {}, player has {}.", containerId, abstractcontainermenu.containerId);
        } else {
            NonNullList<Slot> nonnulllist = abstractcontainermenu.slots;
            int i = nonnulllist.size();
            List<ItemStack> list = Lists.newArrayListWithCapacity(i);

            for (Slot slot : nonnulllist) {
                list.add(slot.getItem().copy());
            }

            abstractcontainermenu.clicked(slotId, mouseButton, clickType, player);
            Int2ObjectMap<HashedStack> int2objectmap = new Int2ObjectOpenHashMap<>();

            for (int j = 0; j < i; j++) {
                ItemStack itemstack = list.get(j);
                ItemStack itemstack1 = nonnulllist.get(j).getItem();
                if (!ItemStack.matches(itemstack, itemstack1)) {
                    int2objectmap.put(j, HashedStack.create(itemstack1, this.connection.decoratedHashOpsGenenerator()));
                }
            }

            HashedStack hashedstack = HashedStack.create(abstractcontainermenu.getCarried(), this.connection.decoratedHashOpsGenenerator());
            this.connection
                .send(
                    new ServerboundContainerClickPacket(
                        containerId,
                        abstractcontainermenu.getStateId(),
                        Shorts.checkedCast(slotId),
                        SignedBytes.checkedCast(mouseButton),
                        clickType,
                        int2objectmap,
                        hashedstack
                    )
                );
        }
    }

    public void handlePlaceRecipe(int containerId, RecipeDisplayId recipe, boolean useMaxItems) {
        this.connection.send(new ServerboundPlaceRecipePacket(containerId, recipe, useMaxItems));
    }

    /**
     * GuiEnchantment uses this during multiplayer to tell PlayerControllerMP to send a packet indicating the enchantment action the player has taken.
     */
    public void handleInventoryButtonClick(int containerId, int buttonId) {
        this.connection.send(new ServerboundContainerButtonClickPacket(containerId, buttonId));
    }

    /**
     * Used in PlayerControllerMP to update the server with an ItemStack in a slot.
     */
    public void handleCreativeModeItemAdd(ItemStack stack, int slotId) {
        if (this.minecraft.player.hasInfiniteMaterials() && this.connection.isFeatureEnabled(stack.getItem().requiredFeatures())) {
            this.connection.send(new ServerboundSetCreativeModeSlotPacket(slotId, stack));
        }
    }

    /**
     * Sends a Packet107 to the server to drop the item on the ground
     */
    public void handleCreativeModeItemDrop(ItemStack stack) {
        boolean flag = this.minecraft.screen instanceof AbstractContainerScreen && !(this.minecraft.screen instanceof CreativeModeInventoryScreen);
        if (this.minecraft.player.hasInfiniteMaterials()
            && !flag
            && !stack.isEmpty()
            && this.connection.isFeatureEnabled(stack.getItem().requiredFeatures())) {
            this.connection.send(new ServerboundSetCreativeModeSlotPacket(-1, stack));
            this.minecraft.player.getDropSpamThrottler().increment();
        }
    }

    public void releaseUsingItem(Player player) {
        this.ensureHasSentCarriedItem();
        this.connection.send(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.RELEASE_USE_ITEM, BlockPos.ZERO, Direction.DOWN));
        player.releaseUsingItem();
    }

    public boolean hasExperience() {
        return this.localPlayerMode.isSurvival();
    }

    public boolean hasMissTime() {
        return !this.localPlayerMode.isCreative();
    }

    public boolean isServerControlledInventory() {
        return this.minecraft.player.isPassenger() && this.minecraft.player.getVehicle() instanceof HasCustomInventoryScreen;
    }

    public boolean isAlwaysFlying() {
        return this.localPlayerMode == GameType.SPECTATOR;
    }

    @Nullable
    public GameType getPreviousPlayerMode() {
        return this.previousLocalPlayerMode;
    }

    public GameType getPlayerMode() {
        return this.localPlayerMode;
    }

    public boolean isDestroying() {
        return this.isDestroying;
    }

    public int getDestroyStage() {
        return this.destroyProgress > 0.0F ? (int)(this.destroyProgress * 10.0F) : -1;
    }

    public void handlePickItemFromBlock(BlockPos pos, boolean includeData) {
        this.connection.send(new ServerboundPickItemFromBlockPacket(pos, includeData));
    }

    public void handlePickItemFromEntity(Entity entity, boolean includeData) {
        this.connection.send(new ServerboundPickItemFromEntityPacket(entity.getId(), includeData));
    }

    public void handleSlotStateChanged(int slotId, int containerId, boolean newState) {
        this.connection.send(new ServerboundContainerSlotStateChangedPacket(slotId, containerId, newState));
    }
}
