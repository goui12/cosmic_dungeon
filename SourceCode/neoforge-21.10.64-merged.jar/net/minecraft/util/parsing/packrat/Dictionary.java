package net.minecraft.util.parsing.packrat;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import javax.annotation.Nullable;

public class Dictionary<S> {
    private final Map<Atom<?>, Dictionary.Entry<S, ?>> terms = new IdentityHashMap<>();

    public <T> NamedRule<S, T> put(Atom<T> name, Rule<S, T> rule) {
        Dictionary.Entry<S, T> entry = (Dictionary.Entry<S, T>)this.terms.computeIfAbsent(name, Dictionary.Entry::new);
        if (entry.value != null) {
            throw new IllegalArgumentException("Trying to override rule: " + name);
        } else {
            entry.value = rule;
            return entry;
        }
    }

    public <T> NamedRule<S, T> putComplex(Atom<T> name, Term<S> term, Rule.RuleAction<S, T> ruleAction) {
        return this.put(name, Rule.fromTerm(term, ruleAction));
    }

    public <T> NamedRule<S, T> put(Atom<T> name, Term<S> term, Rule.SimpleRuleAction<S, T> ruleAction) {
        return this.put(name, Rule.fromTerm(term, ruleAction));
    }

    public void checkAllBound() {
        List<? extends Atom<?>> list = this.terms.entrySet().stream().filter(p_409764_ -> p_409764_.getValue() == null).map(Map.Entry::getKey).toList();
        if (!list.isEmpty()) {
            throw new IllegalStateException("Unbound names: " + list);
        }
    }

    public <T> NamedRule<S, T> getOrThrow(Atom<T> name) {
        return (NamedRule<S, T>)Objects.requireNonNull(this.terms.get(name), () -> "No rule called " + name);
    }

    public <T> NamedRule<S, T> forward(Atom<T> name) {
        return this.getOrCreateEntry(name);
    }

    private <T> Dictionary.Entry<S, T> getOrCreateEntry(Atom<T> name) {
        return (Dictionary.Entry<S, T>)this.terms.computeIfAbsent(name, Dictionary.Entry::new);
    }

    public <T> Term<S> named(Atom<T> name) {
        return new Dictionary.Reference<>(this.getOrCreateEntry(name), name);
    }

    public <T> Term<S> namedWithAlias(Atom<T> name, Atom<T> alias) {
        return new Dictionary.Reference<>(this.getOrCreateEntry(name), alias);
    }

    static class Entry<S, T> implements NamedRule<S, T>, Supplier<String> {
        private final Atom<T> name;
        @Nullable
        Rule<S, T> value;

        private Entry(Atom<T> name) {
            this.name = name;
        }

        @Override
        public Atom<T> name() {
            return this.name;
        }

        @Override
        public Rule<S, T> value() {
            return Objects.requireNonNull(this.value, this);
        }

        public String get() {
            return "Unbound rule " + this.name;
        }
    }

    record Reference<S, T>(Dictionary.Entry<S, T> ruleToParse, Atom<T> nameToStore) implements Term<S> {
        @Override
        public boolean parse(ParseState<S> p_409926_, Scope p_409751_, Control p_409959_) {
            T t = p_409926_.parse(this.ruleToParse);
            if (t == null) {
                return false;
            } else {
                p_409751_.put(this.nameToStore, t);
                return true;
            }
        }
    }
}
