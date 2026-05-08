package net.minecraft.world.item;

import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.Spawner;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class SpawnEggItem extends Item {
    private static final Map<EntityType<?>, SpawnEggItem> BY_ID = Maps.newIdentityHashMap();

    public SpawnEggItem(Item.Properties properties) {
        super(properties);
        TypedEntityData<EntityType<?>> typedentitydata = this.components().get(DataComponents.ENTITY_DATA);
        if (typedentitydata != null) {
            BY_ID.put(typedentitydata.type(), this);
        }
    }

    /**
     * Called when this item is used when targeting a Block
     */
    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (!(level instanceof ServerLevel serverlevel)) {
            return InteractionResult.SUCCESS;
        } else {
            ItemStack $$4 = context.getItemInHand();
            BlockPos $$5 = context.getClickedPos();
            Direction $$6 = context.getClickedFace();
            BlockState $$7 = level.getBlockState($$5);
            if (level.getBlockEntity($$5) instanceof Spawner spawner) {
                EntityType<?> entitytype = this.getType($$4);
                if (entitytype == null) {
                    return InteractionResult.FAIL;
                } else if (!serverlevel.getServer().isSpawnerBlockEnabled()) {
                    if (context.getPlayer() instanceof ServerPlayer serverplayer) {
                        serverplayer.sendSystemMessage(Component.translatable("advMode.notEnabled.spawner"));
                    }

                    return InteractionResult.FAIL;
                } else {
                    spawner.setEntityId(entitytype, level.getRandom());
                    level.sendBlockUpdated($$5, $$7, $$7, 3);
                    level.gameEvent(context.getPlayer(), GameEvent.BLOCK_CHANGE, $$5);
                    $$4.shrink(1);
                    return InteractionResult.SUCCESS;
                }
            } else {
                BlockPos blockpos1;
                if ($$7.getCollisionShape(level, $$5).isEmpty()) {
                    blockpos1 = $$5;
                } else {
                    blockpos1 = $$5.relative($$6);
                }

                return this.spawnMob(context.getPlayer(), $$4, level, blockpos1, true, !Objects.equals($$5, blockpos1) && $$6 == Direction.UP);
            }
        }
    }

    private InteractionResult spawnMob(
        @Nullable LivingEntity source, ItemStack stack, Level level, BlockPos pos, boolean shouldOffsetY, boolean shouldOffsetYMore
    ) {
        EntityType<?> entitytype = this.getType(stack);
        if (entitytype == null) {
            return InteractionResult.FAIL;
        } else if (!entitytype.isAllowedInPeaceful() && level.getDifficulty() == Difficulty.PEACEFUL) {
            return InteractionResult.FAIL;
        } else {
            if (entitytype.spawn((ServerLevel)level, stack, source, pos, EntitySpawnReason.SPAWN_ITEM_USE, shouldOffsetY, shouldOffsetYMore) != null) {
                stack.consume(1, source);
                level.gameEvent(source, GameEvent.ENTITY_PLACE, pos);
            }

            return InteractionResult.SUCCESS;
        }
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack itemstack = player.getItemInHand(hand);
        BlockHitResult blockhitresult = getPlayerPOVHitResult(level, player, ClipContext.Fluid.SOURCE_ONLY);
        if (blockhitresult.getType() != HitResult.Type.BLOCK) {
            return InteractionResult.PASS;
        } else if (level instanceof ServerLevel serverlevel) {
            BlockPos $$7 = blockhitresult.getBlockPos();
            if (!(level.getBlockState($$7).getBlock() instanceof LiquidBlock)) {
                return InteractionResult.PASS;
            } else if (level.mayInteract(player, $$7) && player.mayUseItemAt($$7, blockhitresult.getDirection(), itemstack)) {
                InteractionResult interactionresult = this.spawnMob(player, itemstack, level, $$7, false, false);
                if (interactionresult == InteractionResult.SUCCESS) {
                    player.awardStat(Stats.ITEM_USED.get(this));
                }

                return interactionresult;
            } else {
                return InteractionResult.FAIL;
            }
        } else {
            return InteractionResult.SUCCESS;
        }
    }

    public boolean spawnsEntity(ItemStack stack, EntityType<?> entityType) {
        return Objects.equals(this.getType(stack), entityType);
    }

    @Nullable
    public static SpawnEggItem byId(@Nullable EntityType<?> type) {
        return BY_ID.get(type);
    }

    public static Iterable<SpawnEggItem> eggs() {
        return Iterables.unmodifiableIterable(BY_ID.values());
    }

    @Nullable
    public EntityType<?> getType(ItemStack stack) {
        TypedEntityData<EntityType<?>> typedentitydata = stack.get(DataComponents.ENTITY_DATA);
        return typedentitydata != null ? typedentitydata.type() : null;
    }

    @Override
    public FeatureFlagSet requiredFeatures() {
        return Optional.ofNullable(this.components().get(DataComponents.ENTITY_DATA))
            .map(TypedEntityData::type)
            .map(EntityType::requiredFeatures)
            .orElseGet(FeatureFlagSet::of);
    }

    public Optional<Mob> spawnOffspringFromSpawnEgg(
        Player player, Mob p_mob, EntityType<? extends Mob> entityType, ServerLevel serverLevel, Vec3 pos, ItemStack stack
    ) {
        if (!this.spawnsEntity(stack, entityType)) {
            return Optional.empty();
        } else {
            Mob mob;
            if (p_mob instanceof AgeableMob) {
                mob = ((AgeableMob)p_mob).getBreedOffspring(serverLevel, (AgeableMob)p_mob);
            } else {
                mob = entityType.create(serverLevel, EntitySpawnReason.SPAWN_ITEM_USE);
            }

            if (mob == null) {
                return Optional.empty();
            } else {
                mob.setBaby(true);
                if (!mob.isBaby()) {
                    return Optional.empty();
                } else {
                    mob.snapTo(pos.x(), pos.y(), pos.z(), 0.0F, 0.0F);
                    mob.applyComponentsFromItemStack(stack);
                    serverLevel.addFreshEntityWithPassengers(mob);
                    stack.consume(1, player);
                    return Optional.of(mob);
                }
            }
        }
    }

    @Override
    public boolean shouldPrintOpWarning(ItemStack stack, @Nullable Player player) {
        if (player != null && player.getPermissionLevel() >= 2) {
            TypedEntityData<EntityType<?>> typedentitydata = stack.get(DataComponents.ENTITY_DATA);
            if (typedentitydata != null) {
                return typedentitydata.type().onlyOpCanSetNbt();
            }
        }

        return false;
    }

    public static final net.minecraft.core.dispenser.DispenseItemBehavior DEFAULT_DISPENSE_BEHAVIOR = new net.minecraft.core.dispenser.DefaultDispenseItemBehavior() {
        @Override
        protected ItemStack execute(net.minecraft.core.dispenser.BlockSource source, ItemStack egg) {
            Direction direction = source.state().getValue(net.minecraft.world.level.block.DispenserBlock.FACING);
            EntityType<?> entitytype = ((SpawnEggItem)egg.getItem()).getType(egg);

            try {
                entitytype.spawn(
                        source.level(), egg, null, source.pos().relative(direction), EntitySpawnReason.DISPENSER, direction != Direction.UP, false
                );
            } catch (Exception exception) {
                LOGGER.error("Error while dispensing spawn egg from dispenser at {}", source.pos(), exception);
                return ItemStack.EMPTY;
            }

            egg.shrink(1);
            source.level().gameEvent(null, GameEvent.ENTITY_PLACE, source.pos());
            return egg;
        }
    };

    /**
     * {@return the dispense behavior to register by default}
     */
    @Nullable
    protected net.minecraft.core.dispenser.DispenseItemBehavior createDispenseBehavior() {
        return DEFAULT_DISPENSE_BEHAVIOR;
    }

    @net.neoforged.bus.api.SubscribeEvent(priority = net.neoforged.bus.api.EventPriority.LOWEST)
    private static void registerDispenseBehavior(final net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent event) {
        event.enqueueWork(() -> eggs().forEach(egg -> {
            if (!net.minecraft.world.level.block.DispenserBlock.DISPENSER_REGISTRY.containsKey(egg)) {
                var beh = egg.createDispenseBehavior();
                if (beh != null) {
                    net.minecraft.world.level.block.DispenserBlock.registerBehavior(egg, beh);
                }
            }
        }));
    }
}
