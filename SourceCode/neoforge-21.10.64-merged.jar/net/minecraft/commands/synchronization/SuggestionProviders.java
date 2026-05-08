package net.minecraft.commands.synchronization;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;

public class SuggestionProviders {
    private static final Map<ResourceLocation, SuggestionProvider<SharedSuggestionProvider>> PROVIDERS_BY_NAME = new HashMap<>();
    private static final ResourceLocation ID_ASK_SERVER = ResourceLocation.withDefaultNamespace("ask_server");
    public static final SuggestionProvider<SharedSuggestionProvider> ASK_SERVER = register(
        ID_ASK_SERVER, (p_121673_, p_121674_) -> p_121673_.getSource().customSuggestion(p_121673_)
    );
    public static final SuggestionProvider<SharedSuggestionProvider> AVAILABLE_SOUNDS = register(
        ResourceLocation.withDefaultNamespace("available_sounds"),
        (p_121667_, p_121668_) -> SharedSuggestionProvider.suggestResource(p_121667_.getSource().getAvailableSounds(), p_121668_)
    );
    public static final SuggestionProvider<SharedSuggestionProvider> SUMMONABLE_ENTITIES = register(
        ResourceLocation.withDefaultNamespace("summonable_entities"),
        (p_359328_, p_359329_) -> SharedSuggestionProvider.suggestResource(
            BuiltInRegistries.ENTITY_TYPE.stream().filter(p_247987_ -> p_247987_.isEnabled(p_359328_.getSource().enabledFeatures()) && p_247987_.canSummon()),
            p_359329_,
            EntityType::getKey,
            EntityType::getDescription
        )
    );

    public static <S extends SharedSuggestionProvider> SuggestionProvider<S> register(
        ResourceLocation name, SuggestionProvider<SharedSuggestionProvider> provider
    ) {
        SuggestionProvider<SharedSuggestionProvider> suggestionprovider = PROVIDERS_BY_NAME.putIfAbsent(name, provider);
        if (suggestionprovider != null) {
            throw new IllegalArgumentException("A command suggestion provider is already registered with the name '" + name + "'");
        } else {
            return (SuggestionProvider<S>)new SuggestionProviders.RegisteredSuggestion(name, provider);
        }
    }

    public static <S extends SharedSuggestionProvider> SuggestionProvider<S> cast(SuggestionProvider<SharedSuggestionProvider> provider) {
        return (SuggestionProvider<S>)provider;
    }

    public static <S extends SharedSuggestionProvider> SuggestionProvider<S> getProvider(ResourceLocation name) {
        return cast(PROVIDERS_BY_NAME.getOrDefault(name, ASK_SERVER));
    }

    /**
     * Gets the ID for the given provider. If the provider is not a wrapped one created via {@link #register}, then it returns {@link #ASK_SERVER_ID} instead, as there is no known ID but ASK_SERVER always works.
     */
    public static ResourceLocation getName(SuggestionProvider<?> provider) {
        return provider instanceof SuggestionProviders.RegisteredSuggestion suggestionproviders$registeredsuggestion
            ? suggestionproviders$registeredsuggestion.name
            : ID_ASK_SERVER;
    }

    record RegisteredSuggestion(ResourceLocation name, SuggestionProvider<SharedSuggestionProvider> delegate)
        implements SuggestionProvider<SharedSuggestionProvider> {
        @Override
        public CompletableFuture<Suggestions> getSuggestions(CommandContext<SharedSuggestionProvider> context, SuggestionsBuilder builder) throws CommandSyntaxException {
            return this.delegate.getSuggestions(context, builder);
        }
    }
}
