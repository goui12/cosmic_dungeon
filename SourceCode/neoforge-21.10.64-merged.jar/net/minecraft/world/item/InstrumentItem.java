package net.minecraft.world.item;

import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.component.InstrumentComponent;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;

public class InstrumentItem extends Item {
    public InstrumentItem(Item.Properties properties) {
        super(properties);
    }

    public static ItemStack create(Item item, Holder<Instrument> instrument) {
        ItemStack itemstack = new ItemStack(item);
        itemstack.set(DataComponents.INSTRUMENT, new InstrumentComponent(instrument));
        return itemstack;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack itemstack = player.getItemInHand(hand);
        Optional<? extends Holder<Instrument>> optional = this.getInstrument(itemstack, player.registryAccess());
        if (optional.isPresent()) {
            Instrument instrument = optional.get().value();
            player.startUsingItem(hand);
            play(level, player, instrument);
            player.getCooldowns().addCooldown(itemstack, Mth.floor(instrument.useDuration() * 20.0F));
            player.awardStat(Stats.ITEM_USED.get(this));
            return InteractionResult.CONSUME;
        } else {
            return InteractionResult.FAIL;
        }
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        Optional<Holder<Instrument>> optional = this.getInstrument(stack, entity.registryAccess());
        return optional.<Integer>map(p_360033_ -> Mth.floor(p_360033_.value().useDuration() * 20.0F)).orElse(0);
    }

    private Optional<Holder<Instrument>> getInstrument(ItemStack stack, HolderLookup.Provider registries) {
        InstrumentComponent instrumentcomponent = stack.get(DataComponents.INSTRUMENT);
        return instrumentcomponent != null ? instrumentcomponent.unwrap(registries) : Optional.empty();
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.TOOT_HORN;
    }

    private static void play(Level level, Player player, Instrument instrument) {
        SoundEvent soundevent = instrument.soundEvent().value();
        float f = instrument.range() / 16.0F;
        level.playSound(player, player, soundevent, SoundSource.RECORDS, f, 1.0F);
        level.gameEvent(GameEvent.INSTRUMENT_PLAY, player.position(), GameEvent.Context.of(player));
    }
}
