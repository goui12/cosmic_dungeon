package net.goui.cosmicdungeon.potion;

import java.util.List;
import net.goui.cosmicdungeon.effect.ModMobEffects;
import net.goui.cosmicdungeon.playerclass.theurgist.VitalityPotions;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.alchemy.Potion;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.*;

public final class ModPotions {
    public static final DeferredRegister<Potion> POTIONS=DeferredRegister.create(Registries.POTION,"cosmicdungeon");
    public static final List<DeferredHolder<Potion,Potion>> VITALITY=VitalityPotions.RANKS.stream()
            .map(rank->POTIONS.register(rank.id(),()->new Potion("cosmicdungeon."+rank.id(),
                    new MobEffectInstance(ModMobEffects.VITALITY,VitalityPotions.DURATION,rank.amplifier())))).toList();
    private ModPotions(){}
    public static void register(IEventBus bus){POTIONS.register(bus);}
}
