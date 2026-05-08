package net.minecraft.world.item;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import java.util.Optional;
import java.util.function.Function;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;

public record EitherHolder<T>(Either<Holder<T>, ResourceKey<T>> contents) {
    public EitherHolder(Holder<T> p_350710_) {
        this(Either.left(p_350710_));
    }

    public EitherHolder(ResourceKey<T> p_350883_) {
        this(Either.right(p_350883_));
    }

    public static <T> Codec<EitherHolder<T>> codec(ResourceKey<Registry<T>> registryKey, Codec<Holder<T>> codec) {
        return Codec.either(
                codec,
                ResourceKey.codec(registryKey).comapFlatMap(p_350331_ -> DataResult.error(() -> "Cannot parse as key without registry"), Function.identity())
            )
            .xmap(EitherHolder::new, EitherHolder::contents);
    }

    public static <T> StreamCodec<RegistryFriendlyByteBuf, EitherHolder<T>> streamCodec(
        ResourceKey<Registry<T>> registryKey, StreamCodec<RegistryFriendlyByteBuf, Holder<T>> streamCodec
    ) {
        return StreamCodec.composite(ByteBufCodecs.either(streamCodec, ResourceKey.streamCodec(registryKey)), EitherHolder::contents, EitherHolder::new);
    }

    public Optional<T> unwrap(Registry<T> registry) {
        return this.contents.map(p_400939_ -> Optional.of(p_400939_.value()), registry::getOptional);
    }

    public Optional<Holder<T>> unwrap(HolderLookup.Provider registries) {
        return this.contents.map(Optional::of, p_400937_ -> registries.get((ResourceKey<T>)p_400937_).map(p_400938_ -> (Holder<T>)p_400938_));
    }

    public Optional<ResourceKey<T>> key() {
        return this.contents.map(Holder::unwrapKey, Optional::of);
    }
}
