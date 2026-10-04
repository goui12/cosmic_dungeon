package net.goui.cosmicdungeon.client.screen;

/** Keeps inventory HUD and controls strictly left of inventory slots, even at the minimum GUI size. */
public record D1PartyHudLayout(int x, int y, int width, int height) {
    public static D1PartyHudLayout forView(int inventoryLeft, int members) {
        int width = inventoryLeft < 0 ? 224 : Math.max(24, Math.min(224, inventoryLeft - 16));
        return new D1PartyHudLayout(8, 8, width, members == 0 ? 44 : 32 + Math.clamp(members, 1, 6) * 12);
    }
    public static D1PartyHudLayout withRecipeBook(int screenWidth) {
        return new D1PartyHudLayout(8, 8, Math.min(152, (screenWidth - 24) / 2), 24);
    }
    public int controlsY() { return y + height + 4; }
}
