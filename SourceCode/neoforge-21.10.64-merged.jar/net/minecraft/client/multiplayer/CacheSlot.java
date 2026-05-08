package net.minecraft.client.multiplayer;

import java.util.function.Function;
import javax.annotation.Nullable;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class CacheSlot<C extends CacheSlot.Cleaner<C>, D> {
    private final Function<C, D> operation;
    @Nullable
    private C context;
    @Nullable
    private D value;

    public CacheSlot(Function<C, D> operation) {
        this.operation = operation;
    }

    public D compute(C context) {
        if (context == this.context && this.value != null) {
            return this.value;
        } else {
            D d = this.operation.apply(context);
            this.value = d;
            this.context = context;
            context.registerForCleaning(this);
            return d;
        }
    }

    public void clear() {
        this.value = null;
        this.context = null;
    }

    @FunctionalInterface
    @OnlyIn(Dist.CLIENT)
    public interface Cleaner<C extends CacheSlot.Cleaner<C>> {
        void registerForCleaning(CacheSlot<C, ?> cacheSlot);
    }
}
