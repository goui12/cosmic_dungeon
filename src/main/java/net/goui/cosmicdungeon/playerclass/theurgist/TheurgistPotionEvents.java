package net.goui.cosmicdungeon.playerclass.theurgist;

import net.minecraft.world.entity.projectile.AbstractThrownPotion;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

@EventBusSubscriber(modid="cosmicdungeon")
public final class TheurgistPotionEvents {
    private TheurgistPotionEvents(){}
    @SubscribeEvent public static void admitted(EntityJoinLevelEvent event){
        if(!event.getLevel().isClientSide()&&!event.loadedFromDisk()&&event.getEntity() instanceof AbstractThrownPotion potion)
            TheurgistPotionProtection.capture(potion);
    }
}
