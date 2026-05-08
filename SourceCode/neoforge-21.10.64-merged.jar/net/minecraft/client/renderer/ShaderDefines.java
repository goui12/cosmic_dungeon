package net.minecraft.client.renderer;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public record ShaderDefines(Map<String, String> values, Set<String> flags) {
    public static final ShaderDefines EMPTY = new ShaderDefines(Map.of(), Set.of());
    public static final Codec<ShaderDefines> CODEC = RecordCodecBuilder.create(
        p_366546_ -> p_366546_.group(
                Codec.unboundedMap(Codec.STRING, Codec.STRING).optionalFieldOf("values", Map.of()).forGetter(ShaderDefines::values),
                Codec.STRING.listOf().xmap(Set::copyOf, List::copyOf).optionalFieldOf("flags", Set.of()).forGetter(ShaderDefines::flags)
            )
            .apply(p_366546_, ShaderDefines::new)
    );

    public static ShaderDefines.Builder builder() {
        return new ShaderDefines.Builder();
    }

    public ShaderDefines withOverrides(ShaderDefines defines) {
        if (this.isEmpty()) {
            return defines;
        } else if (defines.isEmpty()) {
            return this;
        } else {
            ImmutableMap.Builder<String, String> builder = ImmutableMap.builderWithExpectedSize(this.values.size() + defines.values.size());
            builder.putAll(this.values);
            builder.putAll(defines.values);
            ImmutableSet.Builder<String> builder1 = ImmutableSet.builderWithExpectedSize(this.flags.size() + defines.flags.size());
            builder1.addAll(this.flags);
            builder1.addAll(defines.flags);
            return new ShaderDefines(builder.buildKeepingLast(), builder1.build());
        }
    }

    public String asSourceDirectives() {
        StringBuilder stringbuilder = new StringBuilder();

        for (Entry<String, String> entry : this.values.entrySet()) {
            String s = entry.getKey();
            String s1 = entry.getValue();
            stringbuilder.append("#define ").append(s).append(" ").append(s1).append('\n');
        }

        for (String s2 : this.flags) {
            stringbuilder.append("#define ").append(s2).append('\n');
        }

        return stringbuilder.toString();
    }

    public boolean isEmpty() {
        return this.values.isEmpty() && this.flags.isEmpty();
    }

    @OnlyIn(Dist.CLIENT)
    public static class Builder {
        private final ImmutableMap.Builder<String, String> values = ImmutableMap.builder();
        private final ImmutableSet.Builder<String> flags = ImmutableSet.builder();

        Builder() {
        }

        public ShaderDefines.Builder define(String key, String value) {
            if (value.isBlank()) {
                throw new IllegalArgumentException("Cannot define empty string");
            } else {
                this.values.put(key, escapeNewLines(value));
                return this;
            }
        }

        private static String escapeNewLines(String str) {
            return str.replaceAll("\n", "\\\\\n");
        }

        public ShaderDefines.Builder define(String key, float value) {
            this.values.put(key, String.valueOf(value));
            return this;
        }

        public ShaderDefines.Builder define(String key, int value) {
            this.values.put(key, String.valueOf(value));
            return this;
        }

        public ShaderDefines.Builder define(String flag) {
            this.flags.add(flag);
            return this;
        }

        public ShaderDefines build() {
            return new ShaderDefines(this.values.build(), this.flags.build());
        }
    }
}
