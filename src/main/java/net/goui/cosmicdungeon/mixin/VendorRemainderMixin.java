package net.goui.cosmicdungeon.mixin;
import net.goui.cosmicdungeon.component.ModDataComponents;
import net.goui.cosmicdungeon.economy.pricing.VendorPricingService;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.UseRemainder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
/** Pricing Master conversion rule: returned containers never create extra vendor value. */
@Mixin(ItemStack.class)
public abstract class VendorRemainderMixin {
    @Inject(method="applyAfterUseComponentSideEffects",at=@At("HEAD"))
    private void cosmicdungeon$capRemainder(LivingEntity entity,ItemStack original,CallbackInfoReturnable<ItemStack> ci){
        if(entity.level().isClientSide())return;
        var remainder=original.get(DataComponents.USE_REMAINDER);
        if(remainder==null||original.isEmpty())return;
        var output=remainder.convertInto().copy();
        long inputValue=VendorPricingService.getSellValue(original.copyWithCount(1),"default").traceValue();
        long outputValue=VendorPricingService.getSellValue(output,"default").traceValue();
        if(outputValue>inputValue){
            output.set(ModDataComponents.VENDOR_PURCHASE_CAP.get(),Math.max(0,inputValue/Math.max(1,output.getCount())));
            // 'original' is the private before-use copy, not the inventory stack or an authored chest.
            original.set(DataComponents.USE_REMAINDER,new UseRemainder(output));
        }
    }
}
