package net.goui.cosmicdungeon.mixin;
import java.util.EnumSet;
import net.goui.cosmicdungeon.playerclass.bogatyr.TargetSelectorFlags;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import org.spongepowered.asm.mixin.*;
/** Read-only native flag access; no global tick hooks, new fields or selector rewrites. */
@Mixin(GoalSelector.class)
public abstract class BogatyrSelectorMixin implements TargetSelectorFlags {
    @Shadow @Final private EnumSet<Goal.Flag> disabledFlags;
    @Override public boolean cosmicdungeon$targetDisabled(){return disabledFlags.contains(Goal.Flag.TARGET);}
}
