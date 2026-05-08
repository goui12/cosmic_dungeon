package net.minecraft.world.entity.item;

import java.util.Objects;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.TraceableEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

public class ItemEntity extends Entity implements TraceableEntity {
    private static final EntityDataAccessor<ItemStack> DATA_ITEM = SynchedEntityData.defineId(ItemEntity.class, EntityDataSerializers.ITEM_STACK);
    private static final float FLOAT_HEIGHT = 0.1F;
    public static final float EYE_HEIGHT = 0.2125F;
    private static final int LIFETIME = 6000;
    private static final int INFINITE_PICKUP_DELAY = 32767;
    private static final int INFINITE_LIFETIME = -32768;
    private static final int DEFAULT_HEALTH = 5;
    private static final short DEFAULT_AGE = 0;
    private static final short DEFAULT_PICKUP_DELAY = 0;
    private int age = 0;
    private int pickupDelay = 0;
    private int health = 5;
    @Nullable
    private EntityReference<Entity> thrower;
    @Nullable
    private UUID target;
    public final float bobOffs = this.random.nextFloat() * (float) Math.PI * 2.0F;
    /**
     * The maximum age of this EntityItem. The item is expired once this is reached.
     */
    public int lifespan = ItemEntity.LIFETIME;

    public ItemEntity(EntityType<? extends ItemEntity> entityType, Level level) {
        super(entityType, level);
        this.setYRot(this.random.nextFloat() * 360.0F);
    }

    public ItemEntity(Level level, double posX, double posY, double posZ, ItemStack itemStack) {
        this(level, posX, posY, posZ, itemStack, level.random.nextDouble() * 0.2 - 0.1, 0.2, level.random.nextDouble() * 0.2 - 0.1);
    }

    public ItemEntity(
        Level level, double posX, double posY, double posZ, ItemStack itemStack, double deltaX, double deltaY, double deltaZ
    ) {
        this(EntityType.ITEM, level);
        this.setPos(posX, posY, posZ);
        this.setDeltaMovement(deltaX, deltaY, deltaZ);
        this.setItem(itemStack);
        this.lifespan = (itemStack.getItem() == null ? ItemEntity.LIFETIME : itemStack.getEntityLifespan(level));
    }

    @Override
    public boolean dampensVibrations() {
        return this.getItem().is(ItemTags.DAMPENS_VIBRATIONS);
    }

    @Nullable
    @Override
    public Entity getOwner() {
        return EntityReference.getEntity(this.thrower, this.level());
    }

    /**
     * Prepares this entity in new dimension by copying NBT data from entity in old dimension
     */
    @Override
    public void restoreFrom(Entity entity) {
        super.restoreFrom(entity);
        if (entity instanceof ItemEntity itementity) {
            this.thrower = itementity.thrower;
        }
    }

    @Override
    protected Entity.MovementEmission getMovementEmission() {
        return Entity.MovementEmission.NONE;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_ITEM, ItemStack.EMPTY);
    }

    @Override
    protected double getDefaultGravity() {
        return 0.04;
    }

    @Override
    public void tick() {
        if (getItem().onEntityItemUpdate(this)) return;
        if (this.getItem().isEmpty()) {
            this.discard();
        } else {
            super.tick();
            if (this.pickupDelay > 0 && this.pickupDelay != 32767) {
                this.pickupDelay--;
            }

            this.xo = this.getX();
            this.yo = this.getY();
            this.zo = this.getZ();
            Vec3 vec3 = this.getDeltaMovement();
            net.neoforged.neoforge.fluids.FluidType fluidType = this.getMaxHeightFluidType();
            if (!fluidType.isAir() && !fluidType.isVanilla() && this.getFluidTypeHeight(fluidType) > 0.1F) fluidType.setItemMovement(this);
            else
            if (this.isInWater() && this.getFluidHeight(FluidTags.WATER) > 0.1F) {
                this.setUnderwaterMovement();
            } else if (this.isInLava() && this.getFluidHeight(FluidTags.LAVA) > 0.1F) {
                this.setUnderLavaMovement();
            } else {
                this.applyGravity();
            }

            if (this.level().isClientSide()) {
                this.noPhysics = false;
            } else {
                this.noPhysics = !this.level().noCollision(this, this.getBoundingBox().deflate(1.0E-7));
                if (this.noPhysics) {
                    this.moveTowardsClosestSpace(this.getX(), (this.getBoundingBox().minY + this.getBoundingBox().maxY) / 2.0, this.getZ());
                }
            }

            if (!this.onGround() || this.getDeltaMovement().horizontalDistanceSqr() > 1.0E-5F || (this.tickCount + this.getId()) % 4 == 0) {
                this.move(MoverType.SELF, this.getDeltaMovement());
                this.applyEffectsFromBlocks();
                float f = 0.98F;
                if (this.onGround()) {
                    BlockPos groundPos = getBlockPosBelowThatAffectsMyMovement();
                    f = this.level().getBlockState(groundPos).getFriction(level(), groundPos, this) * 0.98F;
                }

                this.setDeltaMovement(this.getDeltaMovement().multiply(f, 0.98, f));
                if (this.onGround()) {
                    Vec3 vec31 = this.getDeltaMovement();
                    if (vec31.y < 0.0) {
                        this.setDeltaMovement(vec31.multiply(1.0, -0.5, 1.0));
                    }
                }
            }

            boolean flag = Mth.floor(this.xo) != Mth.floor(this.getX())
                || Mth.floor(this.yo) != Mth.floor(this.getY())
                || Mth.floor(this.zo) != Mth.floor(this.getZ());
            int i = flag ? 2 : 40;
            if (this.tickCount % i == 0 && !this.level().isClientSide() && this.isMergable()) {
                this.mergeWithNeighbours();
            }

            if (this.age != -32768) {
                this.age++;
            }

            this.hasImpulse = this.hasImpulse | this.updateInWaterStateAndDoFluidPushing();
            if (!this.level().isClientSide()) {
                double d0 = this.getDeltaMovement().subtract(vec3).lengthSqr();
                if (d0 > 0.01) {
                    this.hasImpulse = true;
                }
            }

            ItemStack item = this.getItem();
            if (!this.level().isClientSide() && this.age >= lifespan) {
            	  // Clamping to MAX_VALUE -1 as age is a Short and going above that would produce an infinite lifespan implicitly (accidentally)
                this.lifespan = Mth.clamp(lifespan + net.neoforged.neoforge.event.EventHooks.onItemExpire(this), 0, Short.MAX_VALUE - 1);
                if (this.age >= lifespan) {
                    this.discard();
                }
            }
            if (item.isEmpty() && !this.isRemoved()) {
                this.discard();
            }
        }
    }

    @Override
    public BlockPos getBlockPosBelowThatAffectsMyMovement() {
        return this.getOnPos(0.999999F);
    }

    private void setUnderwaterMovement() {
        this.setFluidMovement(0.99F);
    }

    private void setUnderLavaMovement() {
        this.setFluidMovement(0.95F);
    }

    private void setFluidMovement(double multiplier) {
        Vec3 vec3 = this.getDeltaMovement();
        this.setDeltaMovement(vec3.x * multiplier, vec3.y + (vec3.y < 0.06F ? 5.0E-4F : 0.0F), vec3.z * multiplier);
    }

    private void mergeWithNeighbours() {
        if (this.isMergable()) {
            for (ItemEntity itementity : this.level()
                .getEntitiesOfClass(ItemEntity.class, this.getBoundingBox().inflate(0.5, 0.0, 0.5), p_186268_ -> p_186268_ != this && p_186268_.isMergable())) {
                if (itementity.isMergable()) {
                    this.tryToMerge(itementity);
                    if (this.isRemoved()) {
                        break;
                    }
                }
            }
        }
    }

    private boolean isMergable() {
        ItemStack itemstack = this.getItem();
        return this.isAlive() && this.pickupDelay != 32767 && this.age != -32768 && this.age < 6000 && itemstack.getCount() < itemstack.getMaxStackSize();
    }

    private void tryToMerge(ItemEntity itemEntity) {
        ItemStack itemstack = this.getItem();
        ItemStack itemstack1 = itemEntity.getItem();
        if (Objects.equals(this.target, itemEntity.target) && areMergable(itemstack, itemstack1)) {
            if (itemstack1.getCount() < itemstack.getCount()) {
                merge(this, itemstack, itemEntity, itemstack1);
            } else {
                merge(itemEntity, itemstack1, this, itemstack);
            }
        }
    }

    public static boolean areMergable(ItemStack destinationStack, ItemStack originStack) {
        return originStack.getCount() + destinationStack.getCount() > originStack.getMaxStackSize() ? false : ItemStack.isSameItemSameComponents(destinationStack, originStack);
    }

    public static ItemStack merge(ItemStack destinationStack, ItemStack originStack, int amount) {
        int i = Math.min(Math.min(destinationStack.getMaxStackSize(), amount) - destinationStack.getCount(), originStack.getCount());
        ItemStack itemstack = destinationStack.copyWithCount(destinationStack.getCount() + i);
        originStack.shrink(i);
        return itemstack;
    }

    private static void merge(ItemEntity destinationEntity, ItemStack destinationStack, ItemStack originStack) {
        ItemStack itemstack = merge(destinationStack, originStack, 64);
        destinationEntity.setItem(itemstack);
    }

    private static void merge(ItemEntity destinationEntity, ItemStack destinationStack, ItemEntity originEntity, ItemStack originStack) {
        merge(destinationEntity, destinationStack, originStack);
        destinationEntity.pickupDelay = Math.max(destinationEntity.pickupDelay, originEntity.pickupDelay);
        destinationEntity.age = Math.min(destinationEntity.age, originEntity.age);
        if (originStack.isEmpty()) {
            originEntity.discard();
        }
    }

    @Override
    public boolean fireImmune() {
        return !this.getItem().canBeHurtBy(this.damageSources().inFire()) || super.fireImmune();
    }

    @Override
    protected boolean shouldPlayLavaHurtSound() {
        return this.health <= 0 ? true : this.tickCount % 10 == 0;
    }

    @Override
    public final boolean hurtClient(DamageSource damageSource) {
        return this.isInvulnerableToBase(damageSource) ? false : this.getItem().canBeHurtBy(damageSource);
    }

    @Override
    public final boolean hurtServer(ServerLevel level, DamageSource damageSource, float amount) {
        if (this.isInvulnerableToBase(damageSource)) {
            return false;
        } else if (!level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING) && damageSource.getEntity() instanceof Mob) {
            return false;
        } else if (!this.getItem().canBeHurtBy(damageSource)) {
            return false;
        } else {
            this.markHurt();
            this.health = (int)(this.health - amount);
            this.gameEvent(GameEvent.ENTITY_DAMAGE, damageSource.getEntity());
            if (this.health <= 0) {
                this.getItem().onDestroyed(this, damageSource);
                this.discard();
            }

            return true;
        }
    }

    @Override
    public boolean ignoreExplosion(Explosion explosion) {
        return explosion.shouldAffectBlocklikeEntities() ? super.ignoreExplosion(explosion) : true;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        output.putShort("Health", (short)this.health);
        output.putShort("Age", (short)this.age);
        output.putShort("PickupDelay", (short)this.pickupDelay);
        output.putInt("Lifespan", this.lifespan);
        EntityReference.store(this.thrower, output, "Thrower");
        output.storeNullable("Owner", UUIDUtil.CODEC, this.target);
        if (!this.getItem().isEmpty()) {
            output.store("Item", ItemStack.CODEC, this.getItem());
        }
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        this.health = input.getShortOr("Health", (short)5);
        this.age = input.getShortOr("Age", (short)0);
        this.pickupDelay = input.getShortOr("PickupDelay", (short)0);
        this.lifespan = input.getIntOr("Lifespan", ItemEntity.LIFETIME);
        this.target = input.read("Owner", UUIDUtil.CODEC).orElse(null);
        this.thrower = EntityReference.read(input, "Thrower");
        this.setItem(input.read("Item", ItemStack.CODEC).orElse(ItemStack.EMPTY));
        if (this.getItem().isEmpty()) {
            this.discard();
        }
    }

    /**
     * Called by a player entity when they collide with an entity
     */
    @Override
    public void playerTouch(Player entity) {
        if (!this.level().isClientSide()) {
            ItemStack itemstack = this.getItem();
            Item item = itemstack.getItem();
            int i = itemstack.getCount();

            // Neo: Fire item pickup pre/post and adjust handling logic to adhere to the event result.
            var result = net.neoforged.neoforge.event.EventHooks.fireItemPickupPre(this, entity).canPickup();
            if (result.isFalse()) {
                return;
            }

            // Make a copy of the original stack for use in ItemEntityPickupEvent.Post
            ItemStack originalCopy = itemstack.copy();
            // Subvert the vanilla conditions (pickup delay and target check) if the result is true.
            if ((result.isTrue() || this.pickupDelay == 0 && (this.target == null || this.target.equals(entity.getUUID()))) && entity.getInventory().add(itemstack)) {
                // Fire ItemEntityPickupEvent.Post
                net.neoforged.neoforge.event.EventHooks.fireItemPickupPost(this, entity, originalCopy);
                // Update `i` to reflect the actual pickup amount. Vanilla is wrong here and always reports the whole stack.
                i = originalCopy.getCount() - itemstack.getCount();

                entity.take(this, i);
                if (itemstack.isEmpty()) {
                    this.discard();
                    itemstack.setCount(i);
                }

                entity.awardStat(Stats.ITEM_PICKED_UP.get(item), i);
                entity.onItemPickup(this);
            }
        }
    }

    @Override
    public Component getName() {
        Component component = this.getCustomName();
        return component != null ? component : this.getItem().getItemName();
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    @Nullable
    @Override
    public Entity teleport(TeleportTransition teleportTransition) {
        Entity entity = super.teleport(teleportTransition);
        if (!this.level().isClientSide() && entity instanceof ItemEntity itementity) {
            itementity.mergeWithNeighbours();
        }

        return entity;
    }

    public ItemStack getItem() {
        return this.getEntityData().get(DATA_ITEM);
    }

    /**
     * Sets the item that this entity represents.
     */
    public void setItem(ItemStack stack) {
        this.getEntityData().set(DATA_ITEM, stack);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (DATA_ITEM.equals(key)) {
            this.getItem().setEntityRepresentation(this);
        }
    }

    public void setTarget(@Nullable UUID target) {
        this.target = target;
    }

    /**
     * Neo: Add getter for ItemEntity's {@link ItemEntity#target target}.
     * @return The {@link ItemEntity#target target} that can pick up this item entity (if {@code null}, anyone can pick it up)
     */
    @Nullable
    public UUID getTarget() {
        return this.target;
    }

    public void setThrower(Entity thrower) {
        this.thrower = EntityReference.of(thrower);
    }

    public int getAge() {
        return this.age;
    }

    public void setDefaultPickUpDelay() {
        this.pickupDelay = 10;
    }

    public void setNoPickUpDelay() {
        this.pickupDelay = 0;
    }

    public void setNeverPickUp() {
        this.pickupDelay = 32767;
    }

    public void setPickUpDelay(int pickupDelay) {
        this.pickupDelay = pickupDelay;
    }

    public boolean hasPickUpDelay() {
        return this.pickupDelay > 0;
    }

    public void setUnlimitedLifetime() {
        this.age = -32768;
    }

    public void setExtendedLifetime() {
        this.age = -6000;
    }

    public void makeFakeItem() {
        this.setNeverPickUp();
        this.age = getItem().getEntityLifespan(this.level()) - 1;
    }

    public static float getSpin(float age, float bobOffset) {
        return age / 20.0F + bobOffset;
    }

    @Override
    public SoundSource getSoundSource() {
        return SoundSource.AMBIENT;
    }

    @Override
    public float getVisualRotationYInDegrees() {
        return 180.0F - getSpin(this.getAge() + 0.5F, this.bobOffs) / (float) (Math.PI * 2) * 360.0F;
    }

    @Override
    public SlotAccess getSlot(int slot) {
        return slot == 0 ? SlotAccess.of(this::getItem, this::setItem) : super.getSlot(slot);
    }
}
