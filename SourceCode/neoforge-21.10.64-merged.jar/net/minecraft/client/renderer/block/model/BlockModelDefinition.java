package net.minecraft.client.renderer.block.model;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.minecraft.client.renderer.block.model.multipart.MultiPartModel;
import net.minecraft.client.renderer.block.model.multipart.Selector;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.StateHolder;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public record BlockModelDefinition(
    Optional<BlockModelDefinition.SimpleModelSelectors> simpleModels, Optional<BlockModelDefinition.MultiPartDefinition> multiPart
    , Optional<net.neoforged.neoforge.client.model.block.CustomBlockModelDefinition> customDefinition
) {
    static final Logger LOGGER = LogUtils.getLogger();
    public static final com.mojang.serialization.MapCodec<BlockModelDefinition> VANILLA_CODEC = RecordCodecBuilder.<BlockModelDefinition>mapCodec(
            p_409074_ -> p_409074_.group(
                    BlockModelDefinition.SimpleModelSelectors.CODEC.optionalFieldOf("variants").forGetter(BlockModelDefinition::simpleModels),
                    BlockModelDefinition.MultiPartDefinition.CODEC.optionalFieldOf("multipart").forGetter(BlockModelDefinition::multiPart)
                )
                .apply(p_409074_, BlockModelDefinition::new)
        )
        .validate(
            p_409078_ -> p_409078_.simpleModels().isEmpty() && p_409078_.multiPart().isEmpty()
                ? DataResult.error(() -> "Neither 'variants' nor 'multipart' found")
                : DataResult.success(p_409078_)
        );
    public static final Codec<BlockModelDefinition> CODEC = net.neoforged.neoforge.client.model.block.BlockStateModelHooks.makeDefinitionCodec();

    public BlockModelDefinition(Optional<BlockModelDefinition.SimpleModelSelectors> simpleModels, Optional<BlockModelDefinition.MultiPartDefinition> multiPart) {
        this(simpleModels, multiPart, Optional.empty());
    }

    // Neo: convenience constructor for datagen of custom definitions
    public BlockModelDefinition(net.neoforged.neoforge.client.model.block.CustomBlockModelDefinition customDefinition) {
        this(Optional.empty(), Optional.empty(), Optional.of(customDefinition));
    }

    public Map<BlockState, BlockStateModel.UnbakedRoot> instantiate(StateDefinition<Block, BlockState> stateDefinition, Supplier<String> sourceSupplier) {
        if (this.customDefinition.isPresent()) {
            return this.customDefinition.get().instantiate(stateDefinition, sourceSupplier);
        }
        return this.instantiateVanilla(stateDefinition, sourceSupplier);
    }

    // Neo: split off original implementation as a separate method to allow custom definitions to instantiate the original definition and post-process the result
    public Map<BlockState, BlockStateModel.UnbakedRoot> instantiateVanilla(StateDefinition<Block, BlockState> p_360641_, Supplier<String> p_405739_) {
        Map<BlockState, BlockStateModel.UnbakedRoot> map = new IdentityHashMap<>();
        this.simpleModels.ifPresent(p_409073_ -> p_409073_.instantiate(p_360641_, p_405739_, (p_409076_, p_409077_) -> {
            BlockStateModel.UnbakedRoot blockstatemodel$unbakedroot = map.put(p_409076_, p_409077_);
            if (blockstatemodel$unbakedroot != null) {
                throw new IllegalArgumentException("Overlapping definition on state: " + p_409076_);
            }
        }));
        this.multiPart.ifPresent(p_409069_ -> {
            List<BlockState> list = p_360641_.getPossibleStates();
            BlockStateModel.UnbakedRoot blockstatemodel$unbakedroot = p_409069_.instantiate(p_360641_);

            for (BlockState blockstate : list) {
                map.putIfAbsent(blockstate, blockstatemodel$unbakedroot);
            }
        });
        return map;
    }

    @OnlyIn(Dist.CLIENT)
    public record MultiPartDefinition(List<Selector> selectors) {
        public static final Codec<BlockModelDefinition.MultiPartDefinition> CODEC = ExtraCodecs.nonEmptyList(Selector.CODEC.listOf())
            .xmap(BlockModelDefinition.MultiPartDefinition::new, BlockModelDefinition.MultiPartDefinition::selectors);

        public MultiPartModel.Unbaked instantiate(StateDefinition<Block, BlockState> stateDefinition) {
            Builder<MultiPartModel.Selector<BlockStateModel.Unbaked>> builder = ImmutableList.builderWithExpectedSize(this.selectors.size());

            for (Selector selector : this.selectors) {
                builder.add(new MultiPartModel.Selector<>(selector.instantiate(stateDefinition), selector.variant()));
            }

            return new MultiPartModel.Unbaked(builder.build());
        }
    }

    @OnlyIn(Dist.CLIENT)
    public record SimpleModelSelectors(Map<String, BlockStateModel.Unbaked> models) {
        public static final Codec<BlockModelDefinition.SimpleModelSelectors> CODEC = ExtraCodecs.nonEmptyMap(
                Codec.unboundedMap(Codec.STRING, BlockStateModel.Unbaked.CODEC)
            )
            .xmap(BlockModelDefinition.SimpleModelSelectors::new, BlockModelDefinition.SimpleModelSelectors::models);

        public void instantiate(
            StateDefinition<Block, BlockState> stateDefinition, Supplier<String> sourceSupplier, BiConsumer<BlockState, BlockStateModel.UnbakedRoot> output
        ) {
            this.models
                .forEach(
                    (p_410328_, p_410410_) -> {
                        try {
                            Predicate<StateHolder<Block, BlockState>> predicate = VariantSelector.predicate(stateDefinition, p_410328_);
                            BlockStateModel.UnbakedRoot blockstatemodel$unbakedroot = p_410410_.asRoot();

                            for (BlockState blockstate : stateDefinition.getPossibleStates()) {
                                if (predicate.test(blockstate)) {
                                    output.accept(blockstate, blockstatemodel$unbakedroot);
                                }
                            }
                        } catch (Exception exception) {
                            BlockModelDefinition.LOGGER
                                .warn("Exception loading blockstate definition: '{}' for variant: '{}': {}", sourceSupplier.get(), p_410328_, exception.getMessage());
                        }
                    }
                );
        }
    }
}
