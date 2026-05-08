package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;

public class WeatheringCopperChestBlock extends CopperChestBlock implements WeatheringCopper {
    public static final MapCodec<WeatheringCopperChestBlock> CODEC = RecordCodecBuilder.mapCodec(
        p_435330_ -> p_435330_.group(
                WeatheringCopper.WeatherState.CODEC.fieldOf("weathering_state").forGetter(CopperChestBlock::getState),
                BuiltInRegistries.SOUND_EVENT.byNameCodec().fieldOf("open_sound").forGetter(ChestBlock::getOpenChestSound),
                BuiltInRegistries.SOUND_EVENT.byNameCodec().fieldOf("close_sound").forGetter(ChestBlock::getCloseChestSound),
                propertiesCodec()
            )
            .apply(p_435330_, WeatheringCopperChestBlock::new)
    );

    @Override
    public MapCodec<WeatheringCopperChestBlock> codec() {
        return CODEC;
    }

    public WeatheringCopperChestBlock(WeatheringCopper.WeatherState p_434329_, SoundEvent p_432951_, SoundEvent p_433574_, BlockBehaviour.Properties p_433282_) {
        super(p_434329_, p_432951_, p_433574_, p_433282_);
    }

    @Override
    protected boolean isRandomlyTicking(BlockState p_433311_) {
        return WeatheringCopper.getNext(p_433311_.getBlock()).isPresent();
    }

    @Override
    protected void randomTick(BlockState p_434420_, ServerLevel p_435516_, BlockPos p_433681_, RandomSource p_436035_) {
        if (!p_434420_.getValue(ChestBlock.TYPE).equals(ChestType.RIGHT)
            && p_435516_.getBlockEntity(p_433681_) instanceof ChestBlockEntity chestblockentity
            && chestblockentity.getEntitiesWithContainerOpen().isEmpty()) {
            this.changeOverTime(p_434420_, p_435516_, p_433681_, p_436035_);
        }
    }

    public WeatheringCopper.WeatherState getAge() {
        return this.getState();
    }

    @Override
    public boolean isWaxed() {
        return false;
    }
}
