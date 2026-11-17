package net.goui.cosmicdungeon.playerclass.theurgist;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;

/** Family enforcement also covers native/non-palette applications. */
@EventBusSubscriber(modid="cosmicdungeon")
public final class VitalityEffectEvents {
    private VitalityEffectEvents(){}
    @SubscribeEvent public static void applicable(MobEffectEvent.Applicable event){
        var incoming=event.getEffectInstance();
        if(VitalityPotions.isVitality(incoming)&&!VitalityPotions.useful(event.getEntity(),incoming.getAmplifier()))
            event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
    }
}
