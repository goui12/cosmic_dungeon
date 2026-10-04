package net.goui.cosmicdungeon.mixin;
import net.minecraft.stats.*;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
/** Read-only view; callers copy values on the server thread. */
@Mixin(StatsCounter.class)
public interface StatsCounterAccess{
    @Accessor("stats") Object2IntMap<Stat<?>> cosmicdungeon$stats();
}
