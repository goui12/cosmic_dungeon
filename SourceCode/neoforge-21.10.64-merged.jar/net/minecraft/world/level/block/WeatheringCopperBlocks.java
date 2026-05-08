package net.minecraft.world.level.block;

import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableList;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.apache.commons.lang3.function.TriFunction;

public record WeatheringCopperBlocks(
    Block unaffected, Block exposed, Block weathered, Block oxidized, Block waxed, Block waxedExposed, Block waxedWeathered, Block waxedOxidized
) {
    public static <WaxedBlock extends Block, WeatheringBlock extends Block & WeatheringCopper> WeatheringCopperBlocks create(
        String id,
        TriFunction<String, Function<BlockBehaviour.Properties, Block>, BlockBehaviour.Properties, Block> register,
        Function<BlockBehaviour.Properties, WaxedBlock> waxedBlockGetter,
        BiFunction<WeatheringCopper.WeatherState, BlockBehaviour.Properties, WeatheringBlock> weatheredBlockGetter,
        Function<WeatheringCopper.WeatherState, BlockBehaviour.Properties> propertiesGetter
    ) {
        return new WeatheringCopperBlocks(
            register.apply(
                id,
                p_436799_ -> weatheredBlockGetter.apply(WeatheringCopper.WeatherState.UNAFFECTED, p_436799_),
                propertiesGetter.apply(WeatheringCopper.WeatherState.UNAFFECTED)
            ),
            register.apply(
                "exposed_" + id,
                p_436771_ -> weatheredBlockGetter.apply(WeatheringCopper.WeatherState.EXPOSED, p_436771_),
                propertiesGetter.apply(WeatheringCopper.WeatherState.EXPOSED)
            ),
            register.apply(
                "weathered_" + id,
                p_436712_ -> weatheredBlockGetter.apply(WeatheringCopper.WeatherState.WEATHERED, p_436712_),
                propertiesGetter.apply(WeatheringCopper.WeatherState.WEATHERED)
            ),
            register.apply(
                "oxidized_" + id,
                p_436766_ -> weatheredBlockGetter.apply(WeatheringCopper.WeatherState.OXIDIZED, p_436766_),
                propertiesGetter.apply(WeatheringCopper.WeatherState.OXIDIZED)
            ),
            register.apply("waxed_" + id, waxedBlockGetter::apply, propertiesGetter.apply(WeatheringCopper.WeatherState.UNAFFECTED)),
            register.apply("waxed_exposed_" + id, waxedBlockGetter::apply, propertiesGetter.apply(WeatheringCopper.WeatherState.EXPOSED)),
            register.apply("waxed_weathered_" + id, waxedBlockGetter::apply, propertiesGetter.apply(WeatheringCopper.WeatherState.WEATHERED)),
            register.apply("waxed_oxidized_" + id, waxedBlockGetter::apply, propertiesGetter.apply(WeatheringCopper.WeatherState.OXIDIZED))
        );
    }

    public ImmutableBiMap<Block, Block> weatheringMapping() {
        return ImmutableBiMap.of(this.unaffected, this.exposed, this.exposed, this.weathered, this.weathered, this.oxidized);
    }

    public ImmutableBiMap<Block, Block> waxedMapping() {
        return ImmutableBiMap.of(
            this.unaffected, this.waxed, this.exposed, this.waxedExposed, this.weathered, this.waxedWeathered, this.oxidized, this.waxedOxidized
        );
    }

    public ImmutableList<Block> asList() {
        return ImmutableList.of(
            this.unaffected, this.waxed, this.exposed, this.waxedExposed, this.weathered, this.waxedWeathered, this.oxidized, this.waxedOxidized
        );
    }

    public void forEach(Consumer<Block> action) {
        action.accept(this.unaffected);
        action.accept(this.exposed);
        action.accept(this.weathered);
        action.accept(this.oxidized);
        action.accept(this.waxed);
        action.accept(this.waxedExposed);
        action.accept(this.waxedWeathered);
        action.accept(this.waxedOxidized);
    }
}
