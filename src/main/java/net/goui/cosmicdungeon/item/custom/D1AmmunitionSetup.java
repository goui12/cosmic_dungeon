package net.goui.cosmicdungeon.item.custom;

import net.goui.cosmicdungeon.CosmicDungeonMod;
import net.goui.cosmicdungeon.item.ModItems;
import net.minecraft.world.level.block.DispenserBlock;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

/** Native dispenser behaviors; ownerless projectiles still fail the existing D1 effect gate. */
@EventBusSubscriber(modid = CosmicDungeonMod.MOD_ID)
public final class D1AmmunitionSetup {
    private D1AmmunitionSetup() {}
    @SubscribeEvent
    public static void setup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> ModItems.d1Ammunition().forEach(item ->
                DispenserBlock.registerProjectileBehavior(item.get())));
    }
}
