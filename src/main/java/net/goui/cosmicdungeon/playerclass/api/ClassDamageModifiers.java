package net.goui.cosmicdungeon.playerclass.api;

import net.goui.cosmicdungeon.Config;
import net.goui.cosmicdungeon.CosmicDungeonMod;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

@EventBusSubscriber(modid = CosmicDungeonMod.MOD_ID)
public final class ClassDamageModifiers {
    private ClassDamageModifiers() {}
    @SubscribeEvent
    public static void scaleDamage(LivingIncomingDamageEvent event) {
        if (net.goui.cosmicdungeon.playerclass.dragoon.DragoonPassiveEvents.chaining()) return;
        if (event.getSource().getEntity() instanceof ServerPlayer player) {
            double multiplier = Config.outgoingDamage(ClassData.getClassId(player));
            event.setAmount((float) Math.min(Float.MAX_VALUE, event.getAmount() * multiplier));
        }
    }
}
