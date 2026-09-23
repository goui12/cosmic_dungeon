package net.goui.cosmicdungeon.item.identity;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.goui.cosmicdungeon.auth.AccessPolicy;
import net.goui.cosmicdungeon.economy.pricing.ItemTransferRules;
import net.minecraft.commands.*;
import net.minecraft.network.chat.Component;

/** Explicit developer adoption preserves the held stack. There is no player-facing stamping packet. */
public final class D1ItemAuthoring {
    private D1ItemAuthoring() {}
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        D1ItemAdoption.register(dispatcher);
        dispatcher.register(Commands.literal("d1").then(Commands.literal("item")
                .requires(AccessPolicy::requireDeveloperOrConsole)
                .then(Commands.literal("inspect").executes(ctx -> inspect(ctx.getSource())))
                .then(Commands.literal("approve_loot").executes(ctx -> approve(ctx.getSource(), "")))
                .then(Commands.literal("named").then(Commands.argument("identity", StringArgumentType.word())
                        .suggests((ctx, b) -> SharedSuggestionProvider.suggest(D1LootCatalog.ids(), b))
                        .executes(ctx -> approve(ctx.getSource(), StringArgumentType.getString(ctx, "identity")))))));
    }
    private static int inspect(CommandSourceStack source) {
        var player = source.getPlayer();
        if (player == null) { source.sendFailure(Component.literal("Hold the item in game.")); return 0; }
        var stack = player.getMainHandItem();
        var provenance = stack.isEmpty() ? null : ItemProvenanceService.read(stack);
        String status = stack.isEmpty() ? "Empty hand." : provenance != null ? provenance.encode()
                : ItemProvenanceService.present(stack) ? "Unknown/invalid provenance; item retained, transfers blocked."
                : "Unclassified. Display names establish no identity.";
        source.sendSuccess(() -> Component.literal(status), false);
        if (!stack.isEmpty()) {
            var marker=stack.get(net.goui.cosmicdungeon.component.ModDataComponents.D1_ABILITY.get());
            var ability=net.goui.cosmicdungeon.playerclass.d1.D1AbilityIdentity.identify(stack);
            source.sendSuccess(() -> Component.literal("Ammunition: marker="+marker+", recognized="+ability
                    +"; applied enchantments="+ItemProvenanceService.enchantments(stack)),false);
        }
        return 1;
    }
    private static int approve(CommandSourceStack source, String identity) {
        return D1ItemAdoption.previewHeld(source, identity);
    }
    // TODO(M72): review and approve legacy template/chest/drop mappings, including unloaded
    // storage, before rollout. Held adoption is deliberate developer work, not proof of a
    // completed migration. No bulk name matching, item recreation, spawner replacement or
    // class attunement removal is permitted by these commands.
}
