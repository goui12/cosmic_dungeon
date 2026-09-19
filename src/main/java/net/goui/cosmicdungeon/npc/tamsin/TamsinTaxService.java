package net.goui.cosmicdungeon.npc.tamsin;

import net.goui.cosmicdungeon.Config;
import net.goui.cosmicdungeon.achievement.CosmicAchievementIds;
import net.goui.cosmicdungeon.auth.AccessPolicy;
import net.goui.cosmicdungeon.dungeon.DungeonRunRegistryData;
import net.goui.cosmicdungeon.item.identity.ItemProvenanceService;
import net.goui.cosmicdungeon.menu.ClassSelectorMenu;
import net.goui.cosmicdungeon.network.*;
import net.goui.cosmicdungeon.playerclass.api.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import java.util.*;

/** Doc 1dIuaeMFMZaaWo7AS1zQ51kXSbdPkb9tmUm864MBs2Q0: exact one-item, zero-currency payment.
 * No vendor pricing, retail stock, sale restrictions, class restrictions or durability formula. */
public final class TamsinTaxService {
    private TamsinTaxService() {}
    public record Quote(String token, int slot, ItemStack stack, long expires) {}
    private static String name(ItemStack stack) {
        var identity = ItemProvenanceService.named(stack);
        return identity != null && TamsinTaxRules.ELIGIBLE.contains(identity.id()) ? identity.name() : null;
    }
    private static boolean session(ServerPlayer player, ClassSelectorMenu menu) {
        return menu.stage() == TamsinFlow.Stage.TAX && menu.tamsinNpc() != null && menu.stillValid(player)
                && player.isAlive() && !player.isSpectator() && !AccessPolicy.isDeveloper(player)
                && DungeonRunRegistryData.get(player.level().getServer()).findRunForPlayer(player.getUUID()).isEmpty();
    }
    public static void show(ServerPlayer player) {
        if (!(player.containerMenu instanceof ClassSelectorMenu menu) || !session(player, menu)) return;
        List<TamsinTaxPayloads.Choice> choices = new ArrayList<>();
        if (TamsinTaxProgress.available(player)) {
            for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
                var stack = player.getInventory().getItem(slot); String label = name(stack);
                if (label != null && !stack.isEmpty())
                    choices.add(new TamsinTaxPayloads.Choice(slot, label, stack.getCount()));
            }
        }
        var quote = menu.taxQuote;
        if (quote != null && (player.level().getServer().overworld().getGameTime() >= quote.expires()
                || !ItemStack.matches(quote.stack(), player.getInventory().getItem(quote.slot())))) {
            menu.taxQuote = null; quote = null;
        }
        ModNetwork.sendTo(player, new TamsinTaxPayloads.View(menu.containerId, List.copyOf(choices),
                quote == null ? "" : quote.token(), quote == null ? "" : name(quote.stack())));
    }
    public static void action(ServerPlayer player, TamsinTaxPayloads.Action request) {
        if (!(player.containerMenu instanceof ClassSelectorMenu menu) || menu.containerId != request.containerId()
                || !session(player, menu)) return;
        long now = player.level().getServer().overworld().getGameTime();
        if (now - menu.lastTaxActionTick < Config.PARTY_ACTION_TICKS.get()) return;
        menu.lastTaxActionTick = now;
        switch (request.action()) {
            case "later" -> {
                menu.taxQuote = null;
                boolean selected = net.goui.cosmicdungeon.block.custom.ClassSelectorTeleportUtil
                        .isReadyEligibleClass(ClassData.getClassId(player));
                menu.setStage(TamsinFlow.initial(TamsinService.accepted(player), selected));
                ClassNet.sendSelectorDataTo(player);
            }
            case "cancel" -> { menu.taxQuote = null; show(player); }
            case "choose" -> {
                if (!TamsinTaxProgress.available(player) || request.slot() < 0
                        || request.slot() >= player.getInventory().getContainerSize()) return;
                var stack = player.getInventory().getItem(request.slot());
                if (stack.isEmpty() || name(stack) == null) { show(player); return; }
                menu.taxQuote = new Quote(UUID.randomUUID().toString(), request.slot(), stack.copy(),
                        now + Config.TAX_CONFIRM_SECONDS.get() * 20L);
                show(player);
            }
            case "confirm" -> {
                var quote = menu.taxQuote; menu.taxQuote = null;
                if (quote == null) { show(player); return; }
                var current = player.getInventory().getItem(quote.slot());
                if (!TamsinTaxRules.confirmation(true, TamsinTaxProgress.available(player), quote.token(),
                        request.token(), now, quote.expires(), ItemStack.matches(current, quote.stack()))
                        || name(current) == null) {
                    player.sendSystemMessage(Component.literal("That offer changed or expired. Choose again."));
                    show(player); return;
                }
                var advancement = player.level().getServer().getAdvancements().get(CosmicAchievementIds.TAMSIN_TAX);
                if (advancement == null || !advancement.value().criteria().containsKey("triggered")) {
                    player.sendSystemMessage(Component.literal("Tamsin cannot accept payment right now."));
                    show(player); return;
                }
                String id = ItemProvenanceService.named(current).id();
                if (TamsinTaxReceipt.surrender(player, quote.slot(), id)) {
                    player.sendSystemMessage(Component.literal("A deal is a deal. Tamsin has received her cut."));
                    player.closeContainer();
                } else if (player.connection.isAcceptingMessages()) {
                    player.sendSystemMessage(Component.literal("Payment could not be completed."));
                    show(player);
                }
            }
            default -> { }
        }
    }
}
