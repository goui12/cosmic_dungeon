package net.minecraft.world.level.storage.loot;

import com.google.common.collect.Maps;
import java.util.Map;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.context.ContextKey;
import net.minecraft.util.context.ContextKeySet;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.item.ItemStack;

public class LootParams {
    private final ServerLevel level;
    private final ContextMap params;
    private final Map<ResourceLocation, LootParams.DynamicDrop> dynamicDrops;
    private final float luck;

    public LootParams(ServerLevel level, ContextMap params, Map<ResourceLocation, LootParams.DynamicDrop> dynamicDrops, float luck) {
        this.level = level;
        this.params = params;
        this.dynamicDrops = dynamicDrops;
        this.luck = luck;
    }

    public ServerLevel getLevel() {
        return this.level;
    }

    public ContextMap contextMap() {
        return this.params;
    }

    public void addDynamicDrops(ResourceLocation location, Consumer<ItemStack> consumer) {
        LootParams.DynamicDrop lootparams$dynamicdrop = this.dynamicDrops.get(location);
        if (lootparams$dynamicdrop != null) {
            lootparams$dynamicdrop.add(consumer);
        }
    }

    public float getLuck() {
        return this.luck;
    }

    public static class Builder {
        private final ServerLevel level;
        private final ContextMap.Builder params = new ContextMap.Builder();
        private final Map<ResourceLocation, LootParams.DynamicDrop> dynamicDrops = Maps.newHashMap();
        private float luck;

        public Builder(ServerLevel level) {
            this.level = level;
        }

        public ServerLevel getLevel() {
            return this.level;
        }

        public <T> LootParams.Builder withParameter(ContextKey<T> parameter, T value) {
            this.params.withParameter(parameter, value);
            return this;
        }

        public <T> LootParams.Builder withOptionalParameter(ContextKey<T> parameter, @Nullable T value) {
            this.params.withOptionalParameter(parameter, value);
            return this;
        }

        public <T> T getParameter(ContextKey<T> parameter) {
            return this.params.getParameter(parameter);
        }

        @Nullable
        public <T> T getOptionalParameter(ContextKey<T> parameter) {
            return this.params.getOptionalParameter(parameter);
        }

        public LootParams.Builder withDynamicDrop(ResourceLocation name, LootParams.DynamicDrop dynamicDrop) {
            LootParams.DynamicDrop lootparams$dynamicdrop = this.dynamicDrops.put(name, dynamicDrop);
            if (lootparams$dynamicdrop != null) {
                throw new IllegalStateException("Duplicated dynamic drop '" + this.dynamicDrops + "'");
            } else {
                return this;
            }
        }

        public LootParams.Builder withLuck(float luck) {
            this.luck = luck;
            return this;
        }

        public LootParams create(ContextKeySet contextKeySet) {
            ContextMap contextmap = this.params.create(contextKeySet);
            return new LootParams(this.level, contextmap, this.dynamicDrops, this.luck);
        }
    }

    @FunctionalInterface
    public interface DynamicDrop {
        void add(Consumer<ItemStack> output);
    }
}
