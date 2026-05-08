package net.minecraft.commands;

import com.mojang.serialization.Codec;
import java.util.Optional;
import net.minecraft.commands.functions.CommandFunction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.ServerFunctionManager;

public class CacheableFunction {
    public static final Codec<CacheableFunction> CODEC = ResourceLocation.CODEC.xmap(CacheableFunction::new, CacheableFunction::getId);
    private final ResourceLocation id;
    private boolean resolved;
    private Optional<CommandFunction<CommandSourceStack>> function = Optional.empty();

    public CacheableFunction(ResourceLocation id) {
        this.id = id;
    }

    public Optional<CommandFunction<CommandSourceStack>> get(ServerFunctionManager functionManager) {
        if (!this.resolved) {
            this.function = functionManager.get(this.id);
            this.resolved = true;
        }

        return this.function;
    }

    public ResourceLocation getId() {
        return this.id;
    }

    @Override
    public boolean equals(Object other) {
        return other == this ? true : other instanceof CacheableFunction cacheablefunction && this.getId().equals(cacheablefunction.getId());
    }
}
