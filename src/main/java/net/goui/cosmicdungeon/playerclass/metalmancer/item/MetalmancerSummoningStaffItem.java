package net.goui.cosmicdungeon.playerclass.metalmancer.item;

import net.goui.cosmicdungeon.playerclass.api.ClassNet;
import net.goui.cosmicdungeon.playerclass.metalmancer.MetalmancerActions;
import net.goui.cosmicdungeon.playerclass.metalmancer.MetalmancerItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.goui.cosmicdungeon.playerclass.api.ClassNet;

import java.util.function.Consumer;

/**
 * Shared summoning staff base for all Metalmancer tiers.
 *
 * Two concrete items (Tier 1 & Tier 2) are registered in MetalmancerItems,
 * both using this class with a different tier index.
 *
 * Right-click:
 *  - normal: staff_summon
 *  - sneaking: staff_reforge
 *
 * Left-click semantics can later be moved to events if you want to
 * strictly match L/R click docs; for now we use sneak to distinguish.
 */
public class MetalmancerSummoningStaffItem extends MetalmancerOnlyItem {

    public record StaffStats(
            int summonTimeTicks,
            int reforgeTimeTicks,
            int golemHealthHalfHearts,
            int golemDamageHalfHearts,
            int attackCooldownTicks,
            int reforgeOrePerHeart,
            int selfReforgeOrePerHeart,
            int regenOrePerMinute
    ) {
    }

    // Tier index (1 = Bent Rod of Melted Shavings, 2 = Erzfuehler, etc.)
    private final int tier;

    public MetalmancerSummoningStaffItem(Properties properties, int tier) {
        super(properties.stacksTo(1));
        this.tier = tier;
    }

    public int getTier() {
        return this.tier;
    }

    public StaffStats getStats() {
        return switch (tier) {
            case 1 -> TIER_1_STATS;
            case 2 -> TIER_2_STATS;
            // TODO: add more tiers here later
            default -> TIER_1_STATS;
        };
    }

    // ===== Tier tables (all tunable later) =====

    // Bent Rod of Melted Shavings — D2 T1
    private static final StaffStats TIER_1_STATS = new StaffStats(
            20 * 30,   // summonTimeTicks: 30.0 s
            20 * 5,    // reforgeTimeTicks: 5.0 s
            20 * 2,    // golemHealthHalfHearts: 20 hearts -> 40 half-hearts
            3 * 2,     // golemDamageHalfHearts: 3 hearts -> 6
            20 * 2,    // attackCooldownTicks: 2.0 s / strike
            20,        // reforgeOrePerHeart
            10,        // selfReforgeOrePerHeart
            30         // regenOrePerMinute
    );

    // Erzfühler (Ore-Feeler) — D2 T2
    private static final StaffStats TIER_2_STATS = new StaffStats(
            (int) (20 * 28.5f), // summonTimeTicks: 28.5 s
            (int) (20 * 4.5f),  // reforgeTimeTicks: 4.5 s
            25 * 2,             // golemHealthHalfHearts: 25 hearts -> 50
            (int) (3.5f * 2),   // golemDamageHalfHearts: 3.5 hearts -> 7
            (int) (20 * 1.8f),  // attackCooldownTicks: 1.8 s / strike
            22,                 // reforgeOrePerHeart
            11,                 // selfReforgeOrePerHeart
            33                  // regenOrePerMinute
    );

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // Only Metalmancers can actually use this
        if (!allowed(player)) {
            return InteractionResult.PASS;
        }

        if (level.isClientSide()) {
            // For now:
            //  - normal right-click = summon
            //  - sneak right-click = reforge
            String actionId = player.isShiftKeyDown()
                    ? MetalmancerActions.ACTION_STAFF_REFORGE
                    : MetalmancerActions.ACTION_STAFF_SUMMON;

            var conn = Minecraft.getInstance().getConnection();
            if (conn != null) {
                conn.send(new ServerboundCustomPayloadPacket(new ClassNet.C2S_Action(actionId)));
            }

        }

        // Let the swing / use animation play and mark as handled
        return level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER;
    }

    @Override
    public void appendHoverText(ItemStack stack,
                                Item.TooltipContext context,
                                TooltipDisplay tooltipDisplay,
                                Consumer<Component> tooltipAdder,
                                TooltipFlag flag) {
        // Call vanilla / parent behavior first
        super.appendHoverText(stack, context, tooltipDisplay, tooltipAdder, flag);

        // Bent Rod of Melted Shavings
        if (stack.is(MetalmancerItems.BENT_ROD_OF_MELTED_SHAVINGS.get())) {
            tooltipAdder.accept(Component.translatable(
                    "tooltip.cosmicdungeon.bent_rod_of_melted_shavings.codex"
            ).withStyle(ChatFormatting.GOLD));

            tooltipAdder.accept(Component.translatable(
                    "tooltip.cosmicdungeon.bent_rod_of_melted_shavings.definition"
            ).withStyle(ChatFormatting.GRAY));

            tooltipAdder.accept(Component.translatable(
                    "tooltip.cosmicdungeon.bent_rod_of_melted_shavings.lore"
            ).withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        }
        // Erzfühler
        else if (stack.is(MetalmancerItems.ERZFUEHLER.get())) {
            tooltipAdder.accept(Component.translatable(
                    "tooltip.cosmicdungeon.erzfuehler.codex"
            ).withStyle(ChatFormatting.GOLD));

            tooltipAdder.accept(Component.translatable(
                    "tooltip.cosmicdungeon.erzfuehler.definition"
            ).withStyle(ChatFormatting.GRAY));

            tooltipAdder.accept(Component.translatable(
                    "tooltip.cosmicdungeon.erzfuehler.lore"
            ).withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        }
    }
}
