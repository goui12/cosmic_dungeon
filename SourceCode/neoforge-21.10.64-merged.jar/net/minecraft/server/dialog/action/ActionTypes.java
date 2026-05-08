package net.minecraft.server.dialog.action;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.resources.ResourceLocation;

public class ActionTypes {
    public static MapCodec<? extends Action> bootstrap(Registry<MapCodec<? extends Action>> registry) {
        StaticAction.WRAPPED_CODECS
            .forEach((p_428338_, p_428460_) -> Registry.register(registry, ResourceLocation.withDefaultNamespace(p_428338_.getSerializedName()), p_428460_));
        Registry.register(registry, ResourceLocation.withDefaultNamespace("dynamic/run_command"), CommandTemplate.MAP_CODEC);
        return Registry.register(registry, ResourceLocation.withDefaultNamespace("dynamic/custom"), CustomAll.MAP_CODEC);
    }
}
