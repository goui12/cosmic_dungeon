package net.minecraft.client.data.models.blockstates;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import net.minecraft.client.data.models.MultiVariant;
import net.minecraft.client.renderer.block.model.BlockModelDefinition;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.block.model.VariantMutator;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.Property;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class MultiVariantGenerator implements BlockModelDefinitionGenerator {
    private final Block block;
    private final List<MultiVariantGenerator.Entry> entries;
    private final Set<Property<?>> seenProperties;

    MultiVariantGenerator(Block block, List<MultiVariantGenerator.Entry> entries, Set<Property<?>> seenProperties) {
        this.block = block;
        this.entries = entries;
        this.seenProperties = seenProperties;
    }

    static Set<Property<?>> validateAndExpandProperties(Set<Property<?>> seenProperties, Block block, PropertyDispatch<?> propertyDispatch) {
        List<Property<?>> list = propertyDispatch.getDefinedProperties();
        list.forEach(p_403953_ -> {
            if (block.getStateDefinition().getProperty(p_403953_.getName()) != p_403953_) {
                throw new IllegalStateException("Property " + p_403953_ + " is not defined for block " + block);
            } else if (seenProperties.contains(p_403953_)) {
                throw new IllegalStateException("Values of property " + p_403953_ + " already defined for block " + block);
            }
        });
        Set<Property<?>> set = new HashSet<>(seenProperties);
        set.addAll(list);
        return set;
    }

    public MultiVariantGenerator with(PropertyDispatch<VariantMutator> propertyDispatch) {
        Set<Property<?>> set = validateAndExpandProperties(this.seenProperties, this.block, propertyDispatch);
        List<MultiVariantGenerator.Entry> list = this.entries.stream().flatMap(p_403955_ -> p_403955_.apply(propertyDispatch)).toList();
        return new MultiVariantGenerator(this.block, list, set);
    }

    public MultiVariantGenerator with(VariantMutator mutator) {
        List<MultiVariantGenerator.Entry> list = this.entries.stream().flatMap(p_403950_ -> p_403950_.apply(mutator)).toList();
        return new MultiVariantGenerator(this.block, list, this.seenProperties);
    }

    // Neo: copy of the apply(...VariantMutator...) methods to support UnbakedMutators. Must be renamed due to type erasure for PropertyDispatch
    public MultiVariantGenerator withUnbaked(PropertyDispatch<net.neoforged.neoforge.client.model.generators.blockstate.UnbakedMutator> dispatch) {
        Set<Property<?>> set = validateAndExpandProperties(this.seenProperties, this.block, dispatch);
        List<MultiVariantGenerator.Entry> list = this.entries.stream().flatMap(e -> e.applyToUnbaked(dispatch)).toList();
        return new MultiVariantGenerator(this.block, list, set);
    }

    public MultiVariantGenerator withUnbaked(net.neoforged.neoforge.client.model.generators.blockstate.UnbakedMutator dispatch) {
        List<MultiVariantGenerator.Entry> list = this.entries.stream().flatMap(e -> e.applyToUnbaked(dispatch)).toList();
        return new MultiVariantGenerator(this.block, list, this.seenProperties);
    }

    @Override
    public BlockModelDefinition create() {
        Map<String, BlockStateModel.Unbaked> map = new HashMap<>();

        for (MultiVariantGenerator.Entry multivariantgenerator$entry : this.entries) {
            map.put(multivariantgenerator$entry.properties.getKey(), multivariantgenerator$entry.variant.toUnbaked());
        }

        return new BlockModelDefinition(Optional.of(new BlockModelDefinition.SimpleModelSelectors(map)), Optional.empty());
    }

    @Override
    public Block block() {
        return this.block;
    }

    public static MultiVariantGenerator.Empty dispatch(Block block) {
        return new MultiVariantGenerator.Empty(block);
    }

    public static MultiVariantGenerator dispatch(Block block, MultiVariant variants) {
        return new MultiVariantGenerator(block, List.of(new MultiVariantGenerator.Entry(PropertyValueList.EMPTY, variants)), Set.of());
    }

    @OnlyIn(Dist.CLIENT)
    public static class Empty {
        private final Block block;

        public Empty(Block block) {
            this.block = block;
        }

        public MultiVariantGenerator with(PropertyDispatch<MultiVariant> propertyDispatch) {
            Set<Property<?>> set = MultiVariantGenerator.validateAndExpandProperties(Set.of(), this.block, propertyDispatch);
            List<MultiVariantGenerator.Entry> list = propertyDispatch.getEntries()
                .entrySet()
                .stream()
                .map(p_409006_ -> new MultiVariantGenerator.Entry(p_409006_.getKey(), p_409006_.getValue()))
                .toList();
            return new MultiVariantGenerator(this.block, list, set);
        }
    }

    @OnlyIn(Dist.CLIENT)
    record Entry(PropertyValueList properties, MultiVariant variant) {
        public Stream<MultiVariantGenerator.Entry> apply(PropertyDispatch<VariantMutator> propertyDispatch) {
            return propertyDispatch.getEntries().entrySet().stream().map(p_409007_ -> {
                PropertyValueList propertyvaluelist = this.properties.extend(p_409007_.getKey());
                MultiVariant multivariant = this.variant.with(p_409007_.getValue());
                return new MultiVariantGenerator.Entry(propertyvaluelist, multivariant);
            });
        }

        public Stream<MultiVariantGenerator.Entry> apply(VariantMutator mutator) {
            return Stream.of(new MultiVariantGenerator.Entry(this.properties, this.variant.with(mutator)));
        }

        // Neo: Support for unbaked mutators
        public Stream<MultiVariantGenerator.Entry> applyToUnbaked(PropertyDispatch<net.neoforged.neoforge.client.model.generators.blockstate.UnbakedMutator> dispatch) {
            return dispatch.getEntries().entrySet().stream().map(entry -> {
                return new MultiVariantGenerator.Entry(properties.extend(entry.getKey()), variant.with(entry.getValue()));
            });
        }

        public Stream<MultiVariantGenerator.Entry> applyToUnbaked(net.neoforged.neoforge.client.model.generators.blockstate.UnbakedMutator mutator) {
            return Stream.of(new MultiVariantGenerator.Entry(properties, variant.with(mutator)));
        }
    }
}
