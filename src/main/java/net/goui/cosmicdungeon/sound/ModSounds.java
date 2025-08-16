package net.goui.cosmicdungeon.sound;

import net.goui.cosmicdungeon.CosmicDungeonMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(Registries.SOUND_EVENT, CosmicDungeonMod.MOD_ID);

    public static final DeferredHolder<SoundEvent, SoundEvent> CHICKEN_SOUND_1 = register("chicken_sound_1");
    public static final DeferredHolder<SoundEvent, SoundEvent> CHICKEN_SOUND_2 = register("chicken_sound_2");
    public static final DeferredHolder<SoundEvent, SoundEvent> CHICKEN_SOUND_3 = register("chicken_sound_3");
    public static final DeferredHolder<SoundEvent, SoundEvent> CHICKEN_SOUND_4 = register("chicken_sound_4");
    public static final DeferredHolder<SoundEvent, SoundEvent> CHICKEN_SOUND_5 = register("chicken_sound_5");
    public static final DeferredHolder<SoundEvent, SoundEvent> CHICKEN_SOUND_6 = register("chicken_sound_6");
    public static final DeferredHolder<SoundEvent, SoundEvent> CHICKEN_SOUND_7 = register("chicken_sound_7");

    private static DeferredHolder<SoundEvent, SoundEvent> register(String name) {
        return SOUND_EVENTS.register(name,
                () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(CosmicDungeonMod.MOD_ID, name)));
    }

    public static void registerSounds(net.neoforged.bus.api.IEventBus eventBus) {
        SOUND_EVENTS.register(eventBus);
    }
}
