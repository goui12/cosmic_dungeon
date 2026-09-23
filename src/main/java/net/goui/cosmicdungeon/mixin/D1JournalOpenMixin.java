package net.goui.cosmicdungeon.mixin;

import net.goui.cosmicdungeon.achievement.d1.D1JournalService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.component.DataComponents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
public abstract class D1JournalOpenMixin {
    @Inject(method = "openItemGui", at = @At("RETURN"))
    private void cosmicdungeon$journalOpened(ItemStack stack, InteractionHand hand, CallbackInfo ci) {
        var player = (ServerPlayer)(Object)this;
        if (player.getItemInHand(hand) == stack && stack.has(DataComponents.WRITTEN_BOOK_CONTENT))
            D1JournalService.opened(player, stack, null);
    }
}
