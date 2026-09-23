package net.goui.cosmicdungeon.mixin;
import net.goui.cosmicdungeon.crafting.CraftingPolicy;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(StonecutterMenu.class)
public abstract class CraftingStonecutterMixin {
    @Unique private ServerPlayer cosmicdungeon$owner;
    @Inject(method="<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/inventory/ContainerLevelAccess;)V",at=@At("RETURN"))
    private void cosmicdungeon$owner(int id,Inventory inv,ContainerLevelAccess access,CallbackInfo ci) {
        if(inv.player instanceof ServerPlayer player) cosmicdungeon$owner=player;
    }
    @Inject(method="setupResultSlot",at=@At("RETURN"))
    private void cosmicdungeon$preview(int id,CallbackInfo ci) {
        if(cosmicdungeon$owner!=null) CraftingPolicy.refresh((AbstractContainerMenu)(Object)this,cosmicdungeon$owner);
    }
}
