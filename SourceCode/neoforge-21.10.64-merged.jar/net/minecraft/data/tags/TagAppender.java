package net.minecraft.data.tags;

import java.util.Arrays;
import java.util.Collection;
import java.util.function.Function;
import java.util.stream.Stream;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagBuilder;
import net.minecraft.tags.TagKey;

public interface TagAppender<E, T> extends net.neoforged.neoforge.common.extensions.ITagAppenderExtension<E, T> {
    TagAppender<E, T> add(E block);

    default TagAppender<E, T> add(E... elements) {
        return this.addAll(Arrays.stream(elements));
    }

    default TagAppender<E, T> addAll(Collection<E> elements) {
        elements.forEach(this::add);
        return this;
    }

    default TagAppender<E, T> addAll(Stream<E> stream) {
        stream.forEach(this::add);
        return this;
    }

    TagAppender<E, T> addOptional(E block);

    TagAppender<E, T> addTag(TagKey<T> tag);

    TagAppender<E, T> addOptionalTag(TagKey<T> tag);

    static <T> TagAppender<ResourceKey<T>, T> forBuilder(final TagBuilder builder) {
        return new TagAppender<ResourceKey<T>, T>() {
            public TagAppender<ResourceKey<T>, T> add(ResourceKey<T> p_421714_) {
                builder.addElement(p_421714_.location());
                return this;
            }

            public TagAppender<ResourceKey<T>, T> addOptional(ResourceKey<T> p_422270_) {
                builder.addOptionalElement(p_422270_.location());
                return this;
            }

            @Override
            public TagAppender<ResourceKey<T>, T> addTag(TagKey<T> p_422380_) {
                builder.addTag(p_422380_.location());
                return this;
            }

            @Override
            public TagAppender<ResourceKey<T>, T> addOptionalTag(TagKey<T> p_422597_) {
                builder.addOptionalTag(p_422597_.location());
                return this;
            }

            @Override
            public TagAppender<ResourceKey<T>, T> add(net.minecraft.tags.TagEntry entry) {
                builder.add(entry);
                return this;
            }

            @Override
            public TagAppender<ResourceKey<T>, T> replace(boolean value) {
                builder.replace(value);
                return this;
            }

            @Override
            public TagAppender<ResourceKey<T>, T> remove(final ResourceKey<T> resourceKey) {
                builder.removeElement(resourceKey.location());
                return this;
            }

            @Override
            public TagAppender<ResourceKey<T>, T> remove(TagKey<T> tag) {
                builder.removeTag(tag.location());
                return this;
            }
        };
    }

    default <U> TagAppender<U, T> map(final Function<U, E> mapper) {
        final TagAppender<E, T> tagappender = this;
        return new TagAppender<U, T>() {
            @Override
            public TagAppender<U, T> add(U p_422138_) {
                tagappender.add(mapper.apply(p_422138_));
                return this;
            }

            @Override
            public TagAppender<U, T> addOptional(U p_422325_) {
                tagappender.addOptional(mapper.apply(p_422325_));
                return this;
            }

            @Override
            public TagAppender<U, T> addTag(TagKey<T> p_421614_) {
                tagappender.addTag(p_421614_);
                return this;
            }

            @Override
            public TagAppender<U, T> addOptionalTag(TagKey<T> p_421878_) {
                tagappender.addOptionalTag(p_421878_);
                return this;
            }

            @Override
            public TagAppender<U, T> add(net.minecraft.tags.TagEntry entry) {
                tagappender.add(entry);
                return this;
            }

            @Override
            public TagAppender<U, T> replace(boolean value) {
                tagappender.replace(value);
                return this;
            }

            @Override
            public TagAppender<U, T> remove(final U u) {
                tagappender.remove(mapper.apply(u));
                return this;
            }

            @Override
            public TagAppender<U, T> remove(TagKey<T> tag) {
                tagappender.remove(tag);
                return this;
            }
        };
    }
}
