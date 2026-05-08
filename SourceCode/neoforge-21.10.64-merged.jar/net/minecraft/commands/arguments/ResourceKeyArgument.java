package net.minecraft.commands.arguments;

import com.google.gson.JsonObject;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;

public class ResourceKeyArgument<T> implements ArgumentType<ResourceKey<T>> {
    private static final Collection<String> EXAMPLES = Arrays.asList("foo", "foo:bar", "012");
    private static final DynamicCommandExceptionType ERROR_INVALID_FEATURE = new DynamicCommandExceptionType(
        p_304101_ -> Component.translatableEscape("commands.place.feature.invalid", p_304101_)
    );
    private static final DynamicCommandExceptionType ERROR_INVALID_STRUCTURE = new DynamicCommandExceptionType(
        p_304100_ -> Component.translatableEscape("commands.place.structure.invalid", p_304100_)
    );
    private static final DynamicCommandExceptionType ERROR_INVALID_TEMPLATE_POOL = new DynamicCommandExceptionType(
        p_304102_ -> Component.translatableEscape("commands.place.jigsaw.invalid", p_304102_)
    );
    private static final DynamicCommandExceptionType ERROR_INVALID_RECIPE = new DynamicCommandExceptionType(
        p_378839_ -> Component.translatableEscape("recipe.notFound", p_378839_)
    );
    private static final DynamicCommandExceptionType ERROR_INVALID_ADVANCEMENT = new DynamicCommandExceptionType(
        p_378835_ -> Component.translatableEscape("advancement.advancementNotFound", p_378835_)
    );
    private static final com.mojang.brigadier.exceptions.SimpleCommandExceptionType ERROR_NO_RECIPES_ON_CLIENT = new com.mojang.brigadier.exceptions.SimpleCommandExceptionType(
            Component.translatable("commands.neoforge.vanilla.resource_key.no_recipes_on_client")
    );
    final ResourceKey<? extends Registry<T>> registryKey;

    public ResourceKeyArgument(ResourceKey<? extends Registry<T>> registryKey) {
        this.registryKey = registryKey;
    }

    public static <T> ResourceKeyArgument<T> key(ResourceKey<? extends Registry<T>> registryKey) {
        return new ResourceKeyArgument<>(registryKey);
    }

    public static <T> ResourceKey<T> getRegistryKey(
        CommandContext<CommandSourceStack> context, String argument, ResourceKey<Registry<T>> registryKey, DynamicCommandExceptionType exception
    ) throws CommandSyntaxException {
        ResourceKey<?> resourcekey = context.getArgument(argument, ResourceKey.class);
        Optional<ResourceKey<T>> optional = resourcekey.cast(registryKey);
        return optional.orElseThrow(() -> exception.create(resourcekey.location()));
    }

    private static <T> Registry<T> getRegistry(CommandContext<CommandSourceStack> context, ResourceKey<? extends Registry<T>> registryKey) {
        return context.getSource().getServer().registryAccess().lookupOrThrow(registryKey);
    }

    private static <T> Holder.Reference<T> resolveKey(
        CommandContext<CommandSourceStack> context, String argument, ResourceKey<Registry<T>> registryKey, DynamicCommandExceptionType exception
    ) throws CommandSyntaxException {
        ResourceKey<T> resourcekey = getRegistryKey(context, argument, registryKey, exception);
        return getRegistry(context, registryKey).get(resourcekey).orElseThrow(() -> exception.create(resourcekey.location()));
    }

    public static Holder.Reference<ConfiguredFeature<?, ?>> getConfiguredFeature(CommandContext<CommandSourceStack> context, String argument) throws CommandSyntaxException {
        return resolveKey(context, argument, Registries.CONFIGURED_FEATURE, ERROR_INVALID_FEATURE);
    }

    public static Holder.Reference<Structure> getStructure(CommandContext<CommandSourceStack> context, String argument) throws CommandSyntaxException {
        return resolveKey(context, argument, Registries.STRUCTURE, ERROR_INVALID_STRUCTURE);
    }

    public static Holder.Reference<StructureTemplatePool> getStructureTemplatePool(CommandContext<CommandSourceStack> context, String argument) throws CommandSyntaxException {
        return resolveKey(context, argument, Registries.TEMPLATE_POOL, ERROR_INVALID_TEMPLATE_POOL);
    }

    public static RecipeHolder<?> getRecipe(CommandContext<CommandSourceStack> context, String argument) throws CommandSyntaxException {
        if (context.getSource().getUnsidedLevel().isClientSide()) {
            throw ERROR_NO_RECIPES_ON_CLIENT.create();
        }
        RecipeManager recipemanager = context.getSource().getServer().getRecipeManager();
        ResourceKey<Recipe<?>> resourcekey = getRegistryKey(context, argument, Registries.RECIPE, ERROR_INVALID_RECIPE);
        return recipemanager.byKey(resourcekey).orElseThrow(() -> ERROR_INVALID_RECIPE.create(resourcekey.location()));
    }

    public static AdvancementHolder getAdvancement(CommandContext<CommandSourceStack> context, String argument) throws CommandSyntaxException {
        ResourceKey<Advancement> resourcekey = getRegistryKey(context, argument, Registries.ADVANCEMENT, ERROR_INVALID_ADVANCEMENT);
        AdvancementHolder advancementholder = context.getSource().getAdvancement(resourcekey.location());
        if (advancementholder == null) {
            throw ERROR_INVALID_ADVANCEMENT.create(resourcekey.location());
        } else {
            return advancementholder;
        }
    }

    public ResourceKey<T> parse(StringReader reader) throws CommandSyntaxException {
        ResourceLocation resourcelocation = ResourceLocation.read(reader);
        return ResourceKey.create(this.registryKey, resourcelocation);
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        return SharedSuggestionProvider.listSuggestions(context, builder, this.registryKey, SharedSuggestionProvider.ElementSuggestionType.ELEMENTS);
    }

    @Override
    public Collection<String> getExamples() {
        return EXAMPLES;
    }

    public static class Info<T> implements ArgumentTypeInfo<ResourceKeyArgument<T>, ResourceKeyArgument.Info<T>.Template> {
        public void serializeToNetwork(ResourceKeyArgument.Info<T>.Template template, FriendlyByteBuf buffer) {
            buffer.writeResourceKey(template.registryKey);
        }

        public ResourceKeyArgument.Info<T>.Template deserializeFromNetwork(FriendlyByteBuf buffer) {
            return new ResourceKeyArgument.Info.Template(buffer.readRegistryKey());
        }

        public void serializeToJson(ResourceKeyArgument.Info<T>.Template template, JsonObject json) {
            json.addProperty("registry", template.registryKey.location().toString());
        }

        public ResourceKeyArgument.Info<T>.Template unpack(ResourceKeyArgument<T> argument) {
            return new ResourceKeyArgument.Info.Template(argument.registryKey);
        }

        public final class Template implements ArgumentTypeInfo.Template<ResourceKeyArgument<T>> {
            final ResourceKey<? extends Registry<T>> registryKey;

            Template(ResourceKey<? extends Registry<T>> registryKey) {
                this.registryKey = registryKey;
            }

            public ResourceKeyArgument<T> instantiate(CommandBuildContext context) {
                return new ResourceKeyArgument<>(this.registryKey);
            }

            @Override
            public ArgumentTypeInfo<ResourceKeyArgument<T>, ?> type() {
                return Info.this;
            }
        }
    }
}
