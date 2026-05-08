package net.minecraft.world.level.timers;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.functions.CommandFunction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerFunctionManager;

public record FunctionCallback(ResourceLocation functionId) implements TimerCallback<MinecraftServer> {
    public static final MapCodec<FunctionCallback> CODEC = RecordCodecBuilder.mapCodec(
        p_404622_ -> p_404622_.group(ResourceLocation.CODEC.fieldOf("Name").forGetter(FunctionCallback::functionId)).apply(p_404622_, FunctionCallback::new)
    );

    public void handle(MinecraftServer obj, TimerQueue<MinecraftServer> manager, long gameTime) {
        ServerFunctionManager serverfunctionmanager = obj.getFunctions();
        serverfunctionmanager.get(this.functionId)
            .ifPresent(p_305770_ -> serverfunctionmanager.execute((CommandFunction<CommandSourceStack>)p_305770_, serverfunctionmanager.getGameLoopSender()));
    }

    @Override
    public MapCodec<FunctionCallback> codec() {
        return CODEC;
    }
}
