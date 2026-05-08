package net.minecraft.world.entity.animal;

import java.util.Optional;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

public interface Bucketable {
    boolean fromBucket();

    void setFromBucket(boolean fromBucket);

    void saveToBucketTag(ItemStack stack);

    void loadFromBucketTag(CompoundTag tag);

    ItemStack getBucketItemStack();

    SoundEvent getPickupSound();

    @Deprecated
    static void saveDefaultDataToBucketTag(Mob mob, ItemStack bucket) {
        bucket.copyFrom(DataComponents.CUSTOM_NAME, mob);
        CustomData.update(DataComponents.BUCKET_ENTITY_DATA, bucket, p_331213_ -> {
            if (mob.isNoAi()) {
                p_331213_.putBoolean("NoAI", mob.isNoAi());
            }

            if (mob.isSilent()) {
                p_331213_.putBoolean("Silent", mob.isSilent());
            }

            if (mob.isNoGravity()) {
                p_331213_.putBoolean("NoGravity", mob.isNoGravity());
            }

            if (mob.hasGlowingTag()) {
                p_331213_.putBoolean("Glowing", mob.hasGlowingTag());
            }

            if (mob.isInvulnerable()) {
                p_331213_.putBoolean("Invulnerable", mob.isInvulnerable());
            }

            p_331213_.putFloat("Health", mob.getHealth());
        });
    }

    @Deprecated
    static void loadDefaultDataFromBucketTag(Mob mob, CompoundTag tag) {
        tag.getBoolean("NoAI").ifPresent(mob::setNoAi);
        tag.getBoolean("Silent").ifPresent(mob::setSilent);
        tag.getBoolean("NoGravity").ifPresent(mob::setNoGravity);
        tag.getBoolean("Glowing").ifPresent(mob::setGlowingTag);
        tag.getBoolean("Invulnerable").ifPresent(mob::setInvulnerable);
        tag.getFloat("Health").ifPresent(mob::setHealth);
    }

    static <T extends LivingEntity & Bucketable> Optional<InteractionResult> bucketMobPickup(Player player, InteractionHand hand, T entity) {
        ItemStack itemstack = player.getItemInHand(hand);
        if (itemstack.getItem() == Items.WATER_BUCKET && entity.isAlive()) {
            entity.playSound(entity.getPickupSound(), 1.0F, 1.0F);
            ItemStack itemstack1 = entity.getBucketItemStack();
            entity.saveToBucketTag(itemstack1);
            ItemStack itemstack2 = ItemUtils.createFilledResult(itemstack, player, itemstack1, false);
            player.setItemInHand(hand, itemstack2);
            Level level = entity.level();
            if (!level.isClientSide()) {
                CriteriaTriggers.FILLED_BUCKET.trigger((ServerPlayer)player, itemstack1);
            }

            entity.discard();
            return Optional.of(InteractionResult.SUCCESS);
        } else {
            return Optional.empty();
        }
    }
}
