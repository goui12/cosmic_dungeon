package net.goui.cosmicdungeon.playerclass.pyroclast;

import net.goui.cosmicdungeon.achievement.CosmicAchievementIds;
import net.goui.cosmicdungeon.achievement.CosmicAdvancementUtil;
import net.goui.cosmicdungeon.playerclass.api.ClassKeys;
import net.goui.cosmicdungeon.playerclass.api.ClassNbtUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static net.goui.cosmicdungeon.CosmicDungeonMod.MOD_ID;

@EventBusSubscriber(modid = MOD_ID)
public final class PyroclastGunpowderEvents {
    private static final String HINT_TEXT = "Craft: Gravel + Flint = Gunpowder";
    private static final long HINT_COOLDOWN_MS = 5_000L;
    private static final Map<UUID, Long> LAST_HINT_MESSAGE_MS = new ConcurrentHashMap<>();

    private PyroclastGunpowderEvents() {}

    @SubscribeEvent
    public static void onItemPickupPre(ItemEntityPickupEvent.Pre event) {
        if (!(event.getPlayer() instanceof ServerPlayer serverPlayer)) return;
        if (!isPyroclast(serverPlayer)) return;

        ItemEntity itemEntity = event.getItemEntity();
        ItemStack stack = itemEntity.getItem();
        if (!stack.is(Items.GRAVEL) && !stack.is(Items.FLINT)) return;

        boolean hasOtherIngredient = stack.is(Items.GRAVEL)
                ? serverPlayer.getInventory().contains(new ItemStack(Items.FLINT))
                : serverPlayer.getInventory().contains(new ItemStack(Items.GRAVEL));
        if (hasOtherIngredient) {
            showCraftHint(serverPlayer);
        }
    }

    @SubscribeEvent
    public static void onItemCrafted(PlayerEvent.ItemCraftedEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer serverPlayer)) return;
        if (!isPyroclast(serverPlayer)) return;
        if (!event.getCrafting().is(Items.GUNPOWDER)) return;
        if (!isPyroclastGunpowderCraft(event.getInventory())) return;

        CosmicAdvancementUtil.grant(serverPlayer, CosmicAchievementIds.PYROCLAST_BOOM);
    }

    private static boolean isPyroclast(ServerPlayer player) {
        return ClassKeys.CLASS_ID_PYROCLAST.equals(ClassNbtUtil.getClassId(player));
    }

    private static boolean isPyroclastGunpowderCraft(Container inventory) {
        int gravel = 0;
        int flint = 0;
        int other = 0;
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.isEmpty()) continue;
            if (stack.is(Items.GRAVEL)) gravel += stack.getCount();
            else if (stack.is(Items.FLINT)) flint += stack.getCount();
            else other += stack.getCount();
        }
        return gravel == 1 && flint == 1 && other == 0;
    }

    private static void showCraftHint(ServerPlayer player) {
        long now = System.currentTimeMillis();
        long last = LAST_HINT_MESSAGE_MS.getOrDefault(player.getUUID(), 0L);
        if (now - last < HINT_COOLDOWN_MS) return;

        LAST_HINT_MESSAGE_MS.put(player.getUUID(), now);
        Component message = Component.literal(HINT_TEXT).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD);
        player.connection.send(new ClientboundSetTitlesAnimationPacket(10, 60, 20));
        player.connection.send(new ClientboundSetTitleTextPacket(message));
        player.displayClientMessage(message, false);
    }
}
