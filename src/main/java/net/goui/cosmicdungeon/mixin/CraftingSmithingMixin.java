package net.goui.cosmicdungeon.mixin;
import net.goui.cosmicdungeon.crafting.CraftingPolicy;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(ItemCombinerMenu.class)
public abstract class CraftingSmithingMixin {
    @Shadow @Final protected Player player;
    @Inject(method="slotsChanged",at=@At("RETURN"))
    private void cosmicdungeon$preview(Container input,CallbackInfo ci) {
        if((Object)this instanceof SmithingMenu menu && player instanceof ServerPlayer server) CraftingPolicy.refresh(menu,server);
    }
}
