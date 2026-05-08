package net.minecraft.util.context;

import com.google.common.collect.Sets;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import javax.annotation.Nullable;
import org.jetbrains.annotations.Contract;

public class ContextMap {
    // Neo: Add EMPTY map for convenience.
    public static final ContextMap EMPTY = new Builder().create(ContextKeySet.EMPTY);

    private final Map<ContextKey<?>, Object> params;

    ContextMap(Map<ContextKey<?>, Object> params) {
        this.params = params;
    }

    public boolean has(ContextKey<?> key) {
        return this.params.containsKey(key);
    }

    public <T> T getOrThrow(ContextKey<T> key) {
        T t = (T)this.params.get(key);
        if (t == null) {
            throw new NoSuchElementException(key.name().toString());
        } else {
            return t;
        }
    }

    @Nullable
    public <T> T getOptional(ContextKey<T> key) {
        return (T)this.params.get(key);
    }

    @Nullable
    @Contract("_,!null->!null; _,_->_")
    public <T> T getOrDefault(ContextKey<T> key, @Nullable T defaultValue) {
        return (T)this.params.getOrDefault(key, defaultValue);
    }

    public static class Builder {
        private final Map<ContextKey<?>, Object> params = new IdentityHashMap<>();

        public <T> ContextMap.Builder withParameter(ContextKey<T> key, T value) {
            this.params.put(key, value);
            return this;
        }

        public <T> ContextMap.Builder withOptionalParameter(ContextKey<T> key, @Nullable T value) {
            if (value == null) {
                this.params.remove(key);
            } else {
                this.params.put(key, value);
            }

            return this;
        }

        public <T> T getParameter(ContextKey<T> key) {
            T t = (T)this.params.get(key);
            if (t == null) {
                throw new NoSuchElementException(key.name().toString());
            } else {
                return t;
            }
        }

        @Nullable
        public <T> T getOptionalParameter(ContextKey<T> key) {
            return (T)this.params.get(key);
        }

        public ContextMap create(ContextKeySet contextKeySet) {
            Set<ContextKey<?>> set = Sets.difference(this.params.keySet(), contextKeySet.allowed());
            if (false && !set.isEmpty()) {
                throw new IllegalArgumentException("Parameters not allowed in this parameter set: " + set);
            } else {
                Set<ContextKey<?>> set1 = Sets.difference(contextKeySet.required(), this.params.keySet());
                if (!set1.isEmpty()) {
                    throw new IllegalArgumentException("Missing required parameters: " + set1);
                } else {
                    return new ContextMap(this.params);
                }
            }
        }
    }
}
