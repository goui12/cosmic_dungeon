package net.minecraft.client.renderer.block.model;

import com.mojang.math.Quadrant;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import net.minecraft.client.resources.model.BlockModelRotation;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public record Variant(ResourceLocation modelLocation, Variant.SimpleModelState modelState) implements BlockModelPart.Unbaked {
    public static final MapCodec<Variant> MAP_CODEC = RecordCodecBuilder.mapCodec(
        p_409079_ -> p_409079_.group(
                ResourceLocation.CODEC.fieldOf("model").forGetter(Variant::modelLocation), Variant.SimpleModelState.MAP_CODEC.forGetter(Variant::modelState)
            )
            .apply(p_409079_, Variant::new)
    );
    public static final Codec<Variant> CODEC = MAP_CODEC.codec();

    public Variant(ResourceLocation p_404842_) {
        this(p_404842_, Variant.SimpleModelState.DEFAULT);
    }

    public Variant withXRot(Quadrant xRot) {
        return this.withState(this.modelState.withX(xRot));
    }

    public Variant withYRot(Quadrant yRot) {
        return this.withState(this.modelState.withY(yRot));
    }

    public Variant withUvLock(boolean uvLock) {
        return this.withState(this.modelState.withUvLock(uvLock));
    }

    public Variant withModel(ResourceLocation modelLocation) {
        return new Variant(modelLocation, this.modelState);
    }

    public Variant withState(Variant.SimpleModelState modelState) {
        return new Variant(this.modelLocation, modelState);
    }

    public Variant with(VariantMutator mutator) {
        return mutator.apply(this);
    }

    @Override
    public BlockModelPart bake(ModelBaker modelBaker) {
        return SimpleModelWrapper.bake(modelBaker, this.modelLocation, this.modelState.asModelState());
    }

    @Override
    public void resolveDependencies(ResolvableModel.Resolver resolver) {
        resolver.markDependency(this.modelLocation);
    }

    @OnlyIn(Dist.CLIENT)
    public record SimpleModelState(Quadrant x, Quadrant y, boolean uvLock) {
        public static final MapCodec<Variant.SimpleModelState> MAP_CODEC = RecordCodecBuilder.mapCodec(
            p_405304_ -> p_405304_.group(
                    Quadrant.CODEC.optionalFieldOf("x", Quadrant.R0).forGetter(Variant.SimpleModelState::x),
                    Quadrant.CODEC.optionalFieldOf("y", Quadrant.R0).forGetter(Variant.SimpleModelState::y),
                    Codec.BOOL.optionalFieldOf("uvlock", false).forGetter(Variant.SimpleModelState::uvLock)
                )
                .apply(p_405304_, Variant.SimpleModelState::new)
        );
        public static final Variant.SimpleModelState DEFAULT = new Variant.SimpleModelState(Quadrant.R0, Quadrant.R0, false);

        public ModelState asModelState() {
            BlockModelRotation blockmodelrotation = BlockModelRotation.by(this.x, this.y);
            return (ModelState)(this.uvLock ? blockmodelrotation.withUvLock() : blockmodelrotation);
        }

        public Variant.SimpleModelState withX(Quadrant xRot) {
            return new Variant.SimpleModelState(xRot, this.y, this.uvLock);
        }

        public Variant.SimpleModelState withY(Quadrant yRot) {
            return new Variant.SimpleModelState(this.x, yRot, this.uvLock);
        }

        public Variant.SimpleModelState withUvLock(boolean uvLock) {
            return new Variant.SimpleModelState(this.x, this.y, uvLock);
        }
    }
}
