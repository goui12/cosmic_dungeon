package net.goui.cosmicdungeon.client;

import net.goui.cosmicdungeon.dungeon.DungeonInstanceSlots;
import net.minecraft.client.Minecraft;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.RecipeBookMenu;
import net.minecraft.world.level.GameType;

/** Client presentation only; the server's existing CraftingPolicy still authorizes crafting. */
public final class DungeonInventoryRecipeBook {
    private DungeonInventoryRecipeBook() {}

    public static boolean hidden(RecipeBookMenu menu) {
        if (!(menu instanceof InventoryMenu)) return false;
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.gameMode == null
                || client.gameMode.getPlayerMode() != GameType.SURVIVAL) return false;

        // RankEnforcementEvents/RankCommand synchronize developer OP and dungeoneer de-OP.
        // Reuse that client permission hint without adding a second rank/network system.
        return !client.player.hasPermissions(1)
                && DungeonInstanceSlots.slotOf(client.player.level().dimension()).isPresent();
    }
}
