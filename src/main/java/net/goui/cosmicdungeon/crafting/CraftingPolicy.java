package net.goui.cosmicdungeon.crafting;

import net.goui.cosmicdungeon.item.identity.ItemMovementRules;
import net.goui.cosmicdungeon.item.identity.ItemProvenanceService;
import net.goui.cosmicdungeon.playerclass.api.ClassData;
import net.goui.cosmicdungeon.playerclass.dragoon.repair.RepairComponents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import java.util.*;

/** Authorization only: vanilla retains ingredient consumption, native remainders and output movement. */
public final class CraftingPolicy {
    private CraftingPolicy() {}
    private static volatile CraftingRules rules = CraftingRules.parse(true, CraftingRules.DEFAULT_PLAYERS, List.of());
    public static void install(CraftingRules value) { rules = value; }
    public static boolean player(ServerPlayer player, RecipeHolder<?> recipe) {
        return recipe != null && rules.player(recipe.id().location().toString(), ClassData.getClassId(player));
    }
    public static boolean automated(RecipeHolder<?> recipe) {
        return recipe != null && rules.automated(recipe.id().location().toString());
    }
    public static boolean lookup(RecipeType<?> type, RecipeInput input, RecipeHolder<?> recipe) {
        if (recipe == null || !safe(input)) return false;
        String id = recipe.id().location().toString();
        return type == RecipeType.CRAFTING || type == RecipeType.SMITHING || type == RecipeType.STONECUTTING
                ? rules.known(id) : rules.automated(id);
    }
    public static boolean safe(RecipeInput input) {
        for (int i = 0; i < input.size(); i++) {
            var stack = input.getItem(i);
            if (RepairComponents.marked(stack) || ItemProvenanceService.present(stack)
                    || ItemMovementRules.flags(stack).privateStorage()) return false;
        }
        return true;
    }
    public static void validate(MinecraftServer server) {
        Set<String> present = new HashSet<>();
        server.getRecipeManager().getRecipes().forEach(recipe -> present.add(recipe.id().location().toString()));
        Set<String> configured = new TreeSet<>(rules.players().keySet());
        configured.addAll(rules.automation()); configured.removeAll(present);
        if (!configured.isEmpty()) com.mojang.logging.LogUtils.getLogger()
                .warn("Crafting allowlist IDs missing from current datapacks (denied): {}", configured);
    }
    public static void refreshPlayer(ServerPlayer player) {
        refresh(player.inventoryMenu, player);
        if (player.containerMenu != player.inventoryMenu) refresh(player.containerMenu, player);
    }
    /** Rebuild against current recipes before taking an output: handles reloads, class changes and shift loops. */
    public static boolean refresh(AbstractContainerMenu menu, ServerPlayer player) {
        Slot output;
        RecipeHolder<?> selected = null;
        ItemStack result = ItemStack.EMPTY;
        var level = player.level();
        if (menu instanceof AbstractCraftingMenu crafting) {
            output = crafting.getResultSlot();
            var input = CraftingInput.of(crafting.getGridWidth(), crafting.getGridHeight(),
                    crafting.getInputGridSlots().stream().map(Slot::getItem).toList());
            var match = level.getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, level);
            if (match.isPresent() && player(player, match.get())
                    && ((ResultContainer) output.container).setRecipeUsed(player, match.get())) {
                selected = match.get();
                result = match.get().value().assemble(input, level.registryAccess());
            }
        } else if (menu instanceof SmithingMenu) {
            output = menu.getSlot(3);
            var input = new SmithingRecipeInput(menu.getSlot(0).getItem(), menu.getSlot(1).getItem(), menu.getSlot(2).getItem());
            var match = level.getServer().getRecipeManager().getRecipeFor(RecipeType.SMITHING, input, level);
            if (match.isPresent() && player(player, match.get())) {
                selected = match.get(); result = match.get().value().assemble(input, level.registryAccess());
            }
        } else if (menu instanceof StonecutterMenu) {
            output = menu.getSlot(1);
            var previous = ((ResultContainer) output.container).getRecipeUsed();
            var input = new SingleRecipeInput(menu.getSlot(0).getItem());
            if (previous != null && safe(input)) {
                var current = level.getServer().getRecipeManager().byKey(previous.id()).orElse(null);
                if (current != null && current.value() instanceof StonecutterRecipe recipe
                        && player(player, current) && recipe.matches(input, level)) {
                    selected = current; result = recipe.assemble(input, level.registryAccess());
                }
            }
        } else return false;
        if (!result.isEmpty() && !result.isItemEnabled(level.enabledFeatures())) {
            selected = null; result = ItemStack.EMPTY;
        }
        ((ResultContainer) output.container).setRecipeUsed(selected);
        boolean changed = !ItemStack.matches(output.getItem(), result);
        if (changed) { output.set(result); menu.broadcastChanges(); }
        return changed;
    }
}
