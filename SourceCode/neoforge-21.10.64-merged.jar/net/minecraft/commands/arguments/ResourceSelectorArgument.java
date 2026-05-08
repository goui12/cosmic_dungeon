package net.minecraft.commands.arguments;

import com.google.gson.JsonObject;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import org.apache.commons.io.FilenameUtils;

public class ResourceSelectorArgument<T> implements ArgumentType<Collection<Holder.Reference<T>>> {
    private static final Collection<String> EXAMPLES = List.of("minecraft:*", "*:asset", "*");
    public static final Dynamic2CommandExceptionType ERROR_NO_MATCHES = new Dynamic2CommandExceptionType(
        (p_397946_, p_397580_) -> Component.translatableEscape("argument.resource_selector.not_found", p_397946_, p_397580_)
    );
    final ResourceKey<? extends Registry<T>> registryKey;
    private final HolderLookup<T> registryLookup;

    ResourceSelectorArgument(CommandBuildContext buildContext, ResourceKey<? extends Registry<T>> registryKey) {
        this.registryKey = registryKey;
        this.registryLookup = buildContext.lookupOrThrow(registryKey);
    }

    public Collection<Holder.Reference<T>> parse(StringReader reader) throws CommandSyntaxException {
        String s = ensureNamespaced(readPattern(reader));
        List<Holder.Reference<T>> list = this.registryLookup.listElements().filter(p_397003_ -> matches(s, p_397003_.key().location())).toList();
        if (list.isEmpty()) {
            throw ERROR_NO_MATCHES.createWithContext(reader, s, this.registryKey.location());
        } else {
            return list;
        }
    }

    public static <T> Collection<Holder.Reference<T>> parse(StringReader parse, HolderLookup<T> lookup) {
        String s = ensureNamespaced(readPattern(parse));
        return lookup.listElements().filter(p_397120_ -> matches(s, p_397120_.key().location())).toList();
    }

    private static String readPattern(StringReader reader) {
        int i = reader.getCursor();

        while (reader.canRead() && isAllowedPatternCharacter(reader.peek())) {
            reader.skip();
        }

        return reader.getString().substring(i, reader.getCursor());
    }

    private static boolean isAllowedPatternCharacter(char c) {
        return ResourceLocation.isAllowedInResourceLocation(c) || c == '*' || c == '?';
    }

    private static String ensureNamespaced(String name) {
        return !name.contains(":") ? "minecraft:" + name : name;
    }

    private static boolean matches(String string, ResourceLocation location) {
        return FilenameUtils.wildcardMatch(location.toString(), string);
    }

    public static <T> ResourceSelectorArgument<T> resourceSelector(CommandBuildContext buildContext, ResourceKey<? extends Registry<T>> registryKey) {
        return new ResourceSelectorArgument<>(buildContext, registryKey);
    }

    public static <T> Collection<Holder.Reference<T>> getSelectedResources(CommandContext<CommandSourceStack> context, String name) {
        return context.getArgument(name, Collection.class);
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        return SharedSuggestionProvider.listSuggestions(context, builder, this.registryKey, SharedSuggestionProvider.ElementSuggestionType.ELEMENTS);
    }

    @Override
    public Collection<String> getExamples() {
        return EXAMPLES;
    }

    public static class Info<T> implements ArgumentTypeInfo<ResourceSelectorArgument<T>, ResourceSelectorArgument.Info<T>.Template> {
        public void serializeToNetwork(ResourceSelectorArgument.Info<T>.Template template, FriendlyByteBuf buffer) {
            buffer.writeResourceKey(template.registryKey);
        }

        public ResourceSelectorArgument.Info<T>.Template deserializeFromNetwork(FriendlyByteBuf buffer) {
            return new ResourceSelectorArgument.Info.Template(buffer.readRegistryKey());
        }

        public void serializeToJson(ResourceSelectorArgument.Info<T>.Template template, JsonObject json) {
            json.addProperty("registry", template.registryKey.location().toString());
        }

        public ResourceSelectorArgument.Info<T>.Template unpack(ResourceSelectorArgument<T> argument) {
            return new ResourceSelectorArgument.Info.Template(argument.registryKey);
        }

        public final class Template implements ArgumentTypeInfo.Template<ResourceSelectorArgument<T>> {
            final ResourceKey<? extends Registry<T>> registryKey;

            Template(ResourceKey<? extends Registry<T>> registryKey) {
                this.registryKey = registryKey;
            }

            public ResourceSelectorArgument<T> instantiate(CommandBuildContext context) {
                return new ResourceSelectorArgument<>(context, this.registryKey);
            }

            @Override
            public ArgumentTypeInfo<ResourceSelectorArgument<T>, ?> type() {
                return Info.this;
            }
        }
    }
}
