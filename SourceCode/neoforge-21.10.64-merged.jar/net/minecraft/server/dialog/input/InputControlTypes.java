package net.minecraft.server.dialog.input;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;

public class InputControlTypes {
    public static MapCodec<? extends InputControl> bootstrap(Registry<MapCodec<? extends InputControl>> p_425818_) {
        Registry.register(p_425818_, ResourceLocation.withDefaultNamespace("boolean"), BooleanInput.MAP_CODEC);
        Registry.register(p_425818_, ResourceLocation.withDefaultNamespace("number_range"), NumberRangeInput.MAP_CODEC);
        Registry.register(p_425818_, ResourceLocation.withDefaultNamespace("single_option"), SingleOptionInput.MAP_CODEC);
        return Registry.register(p_425818_, ResourceLocation.withDefaultNamespace("text"), TextInput.MAP_CODEC);
    }
}
