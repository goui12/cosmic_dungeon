package net.goui.cosmicdungeon.mixin;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
/** Explicit editor flag can both enable and disable native persistence without reloading the mob. */
@Mixin(Mob.class)
public interface MobPersistenceAccess {
    @Accessor("persistenceRequired") void cosmicdungeon$persistence(boolean value);
}
