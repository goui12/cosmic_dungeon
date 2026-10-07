package net.goui.cosmicdungeon.block.entity;

import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;

/** Server-side, per-player title throttle; no persisted state or tick work. */
public final class CosmicSpawnerFeedback {
    private static final Map<ServerPlayer, Integer> LAST_WARNING = new WeakHashMap<>();
    private CosmicSpawnerFeedback() {}
    public static void warn(ServerPlayer player) {
        if (player.hasInfiniteMaterials() || player.isSpectator() || player.getMainHandItem().is(ItemTags.PICKAXES)) return;
        Integer last = LAST_WARNING.get(player);
        if (last != null && player.tickCount >= last && (long) player.tickCount - last < 40) return;
        LAST_WARNING.put(player, player.tickCount);
        player.connection.send(new ClientboundSetTitlesAnimationPacket(3, 30, 7));
        player.connection.send(new ClientboundSetTitleTextPacket(
                Component.literal("You need a pickaxe to break that!").withStyle(ChatFormatting.YELLOW)));
    }
}
