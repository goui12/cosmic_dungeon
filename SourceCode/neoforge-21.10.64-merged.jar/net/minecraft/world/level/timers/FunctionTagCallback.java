package net.minecraft.world.level.timers;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.functions.CommandFunction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerFunctionManager;

public record FunctionTagCallback(ResourceLocation tagId) implements TimerCallback<MinecraftServer> {
    public static final MapCodec<FunctionTagCallback> CODEC = RecordCodecBuilder.mapCodec(
        p_404756_ -> p_404756_.group(ResourceLocation.CODEC.fieldOf("Name").forGetter(FunctionTagCallback::tagId)).apply(p_404756_, FunctionTagCallback::new)
    );

    public void handle(MinecraftServer obj, TimerQueue<MinecraftServer> manager, long gameTime) {
        ServerFunctionManager serverfunctionmanager = obj.getFunctions();

        for (CommandFunction<CommandSourceStack> commandfunction : serverfunctionmanager.getTag(this.tagId)) {
            serverfunctionmanager.execute(commandfunction, serverfunctionmanager.getGameLoopSender());
        }
    }

    @Override
    public MapCodec<FunctionTagCallback> codec() {
        return CODEC;
    }
}
