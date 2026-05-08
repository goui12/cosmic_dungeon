package net.minecraft.world.entity;

import java.util.Set;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.scores.Scoreboard;

public enum ConversionType {
    SINGLE(true) {
        @Override
        void convert(Mob p_371283_, Mob p_371774_, ConversionParams p_371350_) {
            Entity entity = p_371283_.getFirstPassenger();
            p_371774_.copyPosition(p_371283_);
            p_371774_.setDeltaMovement(p_371283_.getDeltaMovement());
            if (entity != null) {
                entity.stopRiding();
                entity.boardingCooldown = 0;

                for (Entity entity1 : p_371774_.getPassengers()) {
                    entity1.stopRiding();
                    entity1.remove(Entity.RemovalReason.DISCARDED);
                }

                entity.startRiding(p_371774_);
            }

            Entity entity2 = p_371283_.getVehicle();
            if (entity2 != null) {
                p_371283_.stopRiding();
                p_371774_.startRiding(entity2, false, false);
            }

            if (p_371350_.keepEquipment()) {
                for (EquipmentSlot equipmentslot : EquipmentSlot.VALUES) {
                    ItemStack itemstack = p_371283_.getItemBySlot(equipmentslot);
                    if (!itemstack.isEmpty()) {
                        p_371774_.setItemSlot(equipmentslot, itemstack.copyAndClear());
                        p_371774_.setDropChance(equipmentslot, p_371283_.getDropChances().byEquipment(equipmentslot));
                    }
                }
            }

            p_371774_.fallDistance = p_371283_.fallDistance;
            p_371774_.setSharedFlag(7, p_371283_.isFallFlying());
            p_371774_.lastHurtByPlayerMemoryTime = p_371283_.lastHurtByPlayerMemoryTime;
            p_371774_.hurtTime = p_371283_.hurtTime;
            p_371774_.yBodyRot = p_371283_.yBodyRot;
            p_371774_.setOnGround(p_371283_.onGround());
            p_371283_.getSleepingPos().ifPresent(p_371774_::setSleepingPos);
            Entity entity3 = p_371283_.getLeashHolder();
            if (entity3 != null) {
                p_371774_.setLeashedTo(entity3, true);
            }

            this.convertCommon(p_371283_, p_371774_, p_371350_);
        }
    },
    SPLIT_ON_DEATH(false) {
        @Override
        void convert(Mob p_371507_, Mob p_371702_, ConversionParams p_371413_) {
            Entity entity = p_371507_.getFirstPassenger();
            if (entity != null) {
                entity.stopRiding();
            }

            Entity entity1 = p_371507_.getLeashHolder();
            if (entity1 != null) {
                p_371507_.dropLeash();
            }

            this.convertCommon(p_371507_, p_371702_, p_371413_);
        }
    };

    private static final Set<DataComponentType<?>> COMPONENTS_TO_COPY = Set.of(DataComponents.CUSTOM_NAME, DataComponents.CUSTOM_DATA);
    private final boolean discardAfterConversion;

    ConversionType(boolean discardAfterConversion) {
        this.discardAfterConversion = discardAfterConversion;
    }

    public boolean shouldDiscardAfterConversion() {
        return this.discardAfterConversion;
    }

    abstract void convert(Mob oldMob, Mob newMob, ConversionParams conversionParams);

    void convertCommon(Mob oldMob, Mob newMob, ConversionParams conversionParams) {
        newMob.setAbsorptionAmount(oldMob.getAbsorptionAmount());

        for (MobEffectInstance mobeffectinstance : oldMob.getActiveEffects()) {
            newMob.addEffect(new MobEffectInstance(mobeffectinstance));
        }

        if (oldMob.isBaby()) {
            newMob.setBaby(true);
        }

        if (oldMob instanceof AgeableMob ageablemob && newMob instanceof AgeableMob ageablemob1) {
            ageablemob1.setAge(ageablemob.getAge());
            ageablemob1.forcedAge = ageablemob.forcedAge;
            ageablemob1.forcedAgeTimer = ageablemob.forcedAgeTimer;
        }

        Brain<?> brain = oldMob.getBrain();
        Brain<?> brain1 = newMob.getBrain();
        if (brain.checkMemory(MemoryModuleType.ANGRY_AT, MemoryStatus.REGISTERED) && brain.hasMemoryValue(MemoryModuleType.ANGRY_AT)) {
            brain1.setMemory(MemoryModuleType.ANGRY_AT, brain.getMemory(MemoryModuleType.ANGRY_AT));
        }

        if (conversionParams.preserveCanPickUpLoot()) {
            newMob.setCanPickUpLoot(oldMob.canPickUpLoot());
        }

        newMob.setLeftHanded(oldMob.isLeftHanded());
        newMob.setNoAi(oldMob.isNoAi());
        if (oldMob.isPersistenceRequired()) {
            newMob.setPersistenceRequired();
        }

        newMob.setCustomNameVisible(oldMob.isCustomNameVisible());
        newMob.setSharedFlagOnFire(oldMob.isOnFire());
        newMob.setInvulnerable(oldMob.isInvulnerable());
        newMob.setNoGravity(oldMob.isNoGravity());
        newMob.setPortalCooldown(oldMob.getPortalCooldown());
        newMob.setSilent(oldMob.isSilent());
        oldMob.getTags().forEach(newMob::addTag);

        for (DataComponentType<?> datacomponenttype : COMPONENTS_TO_COPY) {
            copyComponent(oldMob, newMob, datacomponenttype);
        }

        if (conversionParams.team() != null) {
            Scoreboard scoreboard = newMob.level().getScoreboard();
            scoreboard.addPlayerToTeam(newMob.getStringUUID(), conversionParams.team());
            if (oldMob.getTeam() != null && oldMob.getTeam() == conversionParams.team()) {
                scoreboard.removePlayerFromTeam(oldMob.getStringUUID(), oldMob.getTeam());
            }
        }

        if (oldMob instanceof Zombie zombie && zombie.canBreakDoors() && newMob instanceof Zombie zombie1) {
            zombie1.setCanBreakDoors(true);
        }
    }

    private static <T> void copyComponent(Mob oldMob, Mob newMob, DataComponentType<T> component) {
        T t = oldMob.get(component);
        if (t != null) {
            newMob.setComponent(component, t);
        }
    }
}
