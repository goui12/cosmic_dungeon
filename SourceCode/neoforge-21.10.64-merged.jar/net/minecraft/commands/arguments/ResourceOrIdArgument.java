package net.minecraft.commands.arguments;

import com.mojang.brigadier.ImmutableStringReader;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DynamicOps;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.SnbtGrammar;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.dialog.Dialog;
import net.minecraft.util.parsing.packrat.Atom;
import net.minecraft.util.parsing.packrat.Dictionary;
import net.minecraft.util.parsing.packrat.NamedRule;
import net.minecraft.util.parsing.packrat.Scope;
import net.minecraft.util.parsing.packrat.Term;
import net.minecraft.util.parsing.packrat.commands.Grammar;
import net.minecraft.util.parsing.packrat.commands.ResourceLocationParseRule;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctions;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public class ResourceOrIdArgument<T> implements ArgumentType<Holder<T>> {
    private static final Collection<String> EXAMPLES = List.of("foo", "foo:bar", "012", "{}", "true");
    public static final DynamicCommandExceptionType ERROR_FAILED_TO_PARSE = new DynamicCommandExceptionType(
        p_335811_ -> Component.translatableEscape("argument.resource_or_id.failed_to_parse", p_335811_)
    );
    public static final Dynamic2CommandExceptionType ERROR_NO_SUCH_ELEMENT = new Dynamic2CommandExceptionType(
        (p_421294_, p_421295_) -> Component.translatableEscape("argument.resource_or_id.no_such_element", p_421294_, p_421295_)
    );
    public static final DynamicOps<Tag> OPS = NbtOps.INSTANCE;
    private final HolderLookup.Provider registryLookup;
    private final Optional<? extends HolderLookup.RegistryLookup<T>> elementLookup;
    private final Codec<T> codec;
    private final Grammar<ResourceOrIdArgument.Result<T, Tag>> grammar;
    private final ResourceKey<? extends Registry<T>> registryKey;

    protected ResourceOrIdArgument(CommandBuildContext registryLookup, ResourceKey<? extends Registry<T>> registryKey, Codec<T> codec) {
        this.registryLookup = registryLookup;
        this.elementLookup = registryLookup.lookup(registryKey);
        this.registryKey = registryKey;
        this.codec = codec;
        this.grammar = createGrammar(registryKey, OPS);
    }

    public static <T, O> Grammar<ResourceOrIdArgument.Result<T, O>> createGrammar(ResourceKey<? extends Registry<T>> registryKey, DynamicOps<O> ops) {
        Grammar<O> grammar = SnbtGrammar.createParser(ops);
        Dictionary<StringReader> dictionary = new Dictionary<>();
        Atom<ResourceOrIdArgument.Result<T, O>> atom = Atom.of("result");
        Atom<ResourceLocation> atom1 = Atom.of("id");
        Atom<O> atom2 = Atom.of("value");
        dictionary.put(atom1, ResourceLocationParseRule.INSTANCE);
        dictionary.put(atom2, grammar.top().value());
        NamedRule<StringReader, ResourceOrIdArgument.Result<T, O>> namedrule = dictionary.put(
            atom, Term.alternative(dictionary.named(atom1), dictionary.named(atom2)), p_421293_ -> {
                ResourceLocation resourcelocation = p_421293_.get(atom1);
                if (resourcelocation != null) {
                    return new ResourceOrIdArgument.ReferenceResult<>(ResourceKey.create(registryKey, resourcelocation));
                } else {
                    O o = p_421293_.getOrThrow(atom2);
                    return new ResourceOrIdArgument.InlineResult<>(o);
                }
            }
        );
        return new Grammar<>(dictionary, namedrule);
    }

    public static ResourceOrIdArgument.LootTableArgument lootTable(CommandBuildContext context) {
        return new ResourceOrIdArgument.LootTableArgument(context);
    }

    public static Holder<LootTable> getLootTable(CommandContext<CommandSourceStack> context, String name) throws CommandSyntaxException {
        return getResource(context, name);
    }

    public static ResourceOrIdArgument.LootModifierArgument lootModifier(CommandBuildContext context) {
        return new ResourceOrIdArgument.LootModifierArgument(context);
    }

    public static Holder<LootItemFunction> getLootModifier(CommandContext<CommandSourceStack> context, String name) {
        return getResource(context, name);
    }

    public static ResourceOrIdArgument.LootPredicateArgument lootPredicate(CommandBuildContext context) {
        return new ResourceOrIdArgument.LootPredicateArgument(context);
    }

    public static Holder<LootItemCondition> getLootPredicate(CommandContext<CommandSourceStack> context, String name) {
        return getResource(context, name);
    }

    public static ResourceOrIdArgument.DialogArgument dialog(CommandBuildContext context) {
        return new ResourceOrIdArgument.DialogArgument(context);
    }

    public static Holder<Dialog> getDialog(CommandContext<CommandSourceStack> context, String name) {
        return getResource(context, name);
    }

    private static <T> Holder<T> getResource(CommandContext<CommandSourceStack> context, String name) {
        return context.getArgument(name, Holder.class);
    }

    @Nullable
    public Holder<T> parse(StringReader reader) throws CommandSyntaxException {
        return this.parse(reader, this.grammar, OPS);
    }

    @Nullable
    private <O> Holder<T> parse(StringReader reader, Grammar<ResourceOrIdArgument.Result<T, O>> grammar, DynamicOps<O> ops) throws CommandSyntaxException {
        ResourceOrIdArgument.Result<T, O> result = grammar.parseForCommands(reader);
        return this.elementLookup.isEmpty()
            ? null
            : result.parse(reader, this.registryLookup, ops, this.codec, (HolderLookup.RegistryLookup<T>)this.elementLookup.get());
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        return SharedSuggestionProvider.listSuggestions(context, builder, this.registryKey, SharedSuggestionProvider.ElementSuggestionType.ELEMENTS);
    }

    @Override
    public Collection<String> getExamples() {
        return EXAMPLES;
    }

    public static class DialogArgument extends ResourceOrIdArgument<Dialog> {
        protected DialogArgument(CommandBuildContext context) {
            super(context, Registries.DIALOG, Dialog.DIRECT_CODEC);
        }
    }

    public record InlineResult<T, O>(O value) implements ResourceOrIdArgument.Result<T, O> {
        @Override
        public Holder<T> parse(
            ImmutableStringReader p_421858_,
            HolderLookup.Provider p_422417_,
            DynamicOps<O> p_421627_,
            Codec<T> p_422188_,
            HolderLookup.RegistryLookup<T> p_421743_
        ) throws CommandSyntaxException {
            return Holder.direct(
                p_422188_.parse(p_422417_.createSerializationContext(p_421627_), this.value)
                    .getOrThrow(p_421517_ -> ResourceOrIdArgument.ERROR_FAILED_TO_PARSE.createWithContext(p_421858_, p_421517_))
            );
        }
    }

    public static class LootModifierArgument extends ResourceOrIdArgument<LootItemFunction> {
        protected LootModifierArgument(CommandBuildContext context) {
            super(context, Registries.ITEM_MODIFIER, LootItemFunctions.ROOT_CODEC);
        }
    }

    public static class LootPredicateArgument extends ResourceOrIdArgument<LootItemCondition> {
        protected LootPredicateArgument(CommandBuildContext context) {
            super(context, Registries.PREDICATE, LootItemCondition.DIRECT_CODEC);
        }
    }

    public static class LootTableArgument extends ResourceOrIdArgument<LootTable> {
        protected LootTableArgument(CommandBuildContext context) {
            super(context, Registries.LOOT_TABLE, LootTable.DIRECT_CODEC);
        }
    }

    public record ReferenceResult<T, O>(ResourceKey<T> key) implements ResourceOrIdArgument.Result<T, O> {
        @Override
        public Holder<T> parse(
            ImmutableStringReader p_422449_,
            HolderLookup.Provider p_422045_,
            DynamicOps<O> p_421653_,
            Codec<T> p_422608_,
            HolderLookup.RegistryLookup<T> p_421710_
        ) throws CommandSyntaxException {
            return p_421710_.get(this.key)
                .orElseThrow(() -> ResourceOrIdArgument.ERROR_NO_SUCH_ELEMENT.createWithContext(p_422449_, this.key.location(), this.key.registry()));
        }
    }

    public sealed interface Result<T, O> permits ResourceOrIdArgument.InlineResult, ResourceOrIdArgument.ReferenceResult {
        Holder<T> parse(
            ImmutableStringReader reader,
            HolderLookup.Provider registryLookup,
            DynamicOps<O> ops,
            Codec<T> codec,
            HolderLookup.RegistryLookup<T> elementLookup
        ) throws CommandSyntaxException;
    }
}
