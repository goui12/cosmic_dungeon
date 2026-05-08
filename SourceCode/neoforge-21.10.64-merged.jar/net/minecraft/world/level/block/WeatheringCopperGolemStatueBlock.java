package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.animal.coppergolem.CopperGolem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.CopperGolemStatueBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class WeatheringCopperGolemStatueBlock extends CopperGolemStatueBlock implements WeatheringCopper {
    public static final MapCodec<WeatheringCopperGolemStatueBlock> CODEC = RecordCodecBuilder.mapCodec(
        p_434180_ -> p_434180_.group(WeatheringCopper.WeatherState.CODEC.fieldOf("weathering_state").forGetter(ChangeOverTimeBlock::getAge), propertiesCodec())
            .apply(p_434180_, WeatheringCopperGolemStatueBlock::new)
    );

    @Override
    public MapCodec<WeatheringCopperGolemStatueBlock> codec() {
        return CODEC;
    }

    public WeatheringCopperGolemStatueBlock(WeatheringCopper.WeatherState p_433117_, BlockBehaviour.Properties p_433942_) {
        super(p_433117_, p_433942_);
    }

    @Override
    protected boolean isRandomlyTicking(BlockState p_434087_) {
        return WeatheringCopper.getNext(p_434087_.getBlock()).isPresent();
    }

    @Override
    protected void randomTick(BlockState p_435078_, ServerLevel p_434964_, BlockPos p_433978_, RandomSource p_433413_) {
        this.changeOverTime(p_435078_, p_434964_, p_433978_, p_433413_);
    }

    public WeatheringCopper.WeatherState getAge() {
        return this.getWeatheringState();
    }

    @Override
    protected InteractionResult useItemOn(
        ItemStack p_433666_, BlockState p_433930_, Level p_435157_, BlockPos p_435733_, Player p_434811_, InteractionHand p_434251_, BlockHitResult p_433254_
    ) {
        if (p_435157_.getBlockEntity(p_435733_) instanceof CopperGolemStatueBlockEntity coppergolemstatueblockentity) {
            if (!p_433666_.is(ItemTags.AXES)) {
                if (p_433666_.is(Items.HONEYCOMB)) {
                    return InteractionResult.PASS;
                }

                this.updatePose(p_435157_, p_433930_, p_435733_, p_434811_);
                return InteractionResult.SUCCESS;
            }

            if (this.getAge().equals(WeatheringCopper.WeatherState.UNAFFECTED)) {
                CopperGolem coppergolem = coppergolemstatueblockentity.removeStatue(p_433930_);
                p_433666_.hurtAndBreak(1, p_434811_, p_434251_.asEquipmentSlot());
                if (coppergolem != null) {
                    p_435157_.addFreshEntity(coppergolem);
                    p_435157_.removeBlock(p_435733_, false);
                    return InteractionResult.SUCCESS;
                }
            }
        }

        return InteractionResult.PASS;
    }
}
