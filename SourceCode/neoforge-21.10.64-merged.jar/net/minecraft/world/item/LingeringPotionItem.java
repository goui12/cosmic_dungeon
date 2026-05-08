package net.minecraft.world.item;

import net.minecraft.core.Position;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractThrownPotion;
import net.minecraft.world.entity.projectile.ThrownLingeringPotion;
import net.minecraft.world.level.Level;

public class LingeringPotionItem extends ThrowablePotionItem {
    public LingeringPotionItem(Item.Properties p_42836_) {
        super(p_42836_);
    }

    @Override
    public InteractionResult use(Level p_42843_, Player p_42844_, InteractionHand p_42845_) {
        p_42843_.playSound(
            null,
            p_42844_.getX(),
            p_42844_.getY(),
            p_42844_.getZ(),
            SoundEvents.LINGERING_POTION_THROW,
            SoundSource.NEUTRAL,
            0.5F,
            0.4F / (p_42843_.getRandom().nextFloat() * 0.4F + 0.8F)
        );
        return super.use(p_42843_, p_42844_, p_42845_);
    }

    @Override
    protected AbstractThrownPotion createPotion(ServerLevel p_400122_, LivingEntity p_400210_, ItemStack p_399574_) {
        return new ThrownLingeringPotion(p_400122_, p_400210_, p_399574_);
    }

    @Override
    protected AbstractThrownPotion createPotion(Level p_399818_, Position p_399889_, ItemStack p_399694_) {
        return new ThrownLingeringPotion(p_399818_, p_399889_.x(), p_399889_.y(), p_399889_.z(), p_399694_);
    }
}
