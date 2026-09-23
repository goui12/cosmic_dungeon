package net.goui.cosmicdungeon.crafting;

import java.util.List;
import net.goui.cosmicdungeon.Config;
import net.goui.cosmicdungeon.CosmicDungeonMod;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import net.neoforged.bus.api.SubscribeEvent;

@EventBusSubscriber(modid = CosmicDungeonMod.MOD_ID)
public final class CraftingConfig {
    private CraftingConfig() {}
    public static ModConfigSpec.BooleanValue ENABLED;
    public static ModConfigSpec.ConfigValue<List<? extends String>> PLAYERS, AUTOMATION;
    public static void define(ModConfigSpec.Builder b) {
        b.comment("Economy/Trading: general crafting unavailable; only approved recipes.",
                "Retained Theurgist reagent docs: Fermented Spider Eye, Magma Cream, Glistering Melon.",
                "Server policy; client recipe-book suggestions are not authorization.").push("Crafting");
        ENABLED = b.comment("Enforce the allowlists. False explicitly permits ordinary recipes; protected inputs remain guarded.")
                .define("enabled", true);
        PLAYERS = b.comment("recipe_id|class_id; repeat a recipe for multiple classes. * means all players.",
                "No recipe-ID wildcards. Empty denies all player recipes. Limit 4096 entries.")
                .defineListAllowEmpty("playerRecipes", CraftingRules.DEFAULT_PLAYERS,
                        () -> "minecraft:fermented_spider_eye|theurgist", CraftingRules::playerEntry);
        AUTOMATION = b.comment("Explicit recipe IDs for Crafters and cooking blocks (furnace/blast furnace/smoker/campfire).",
                "Machines have no player class. Empty denies automated production. Existing output remains retrievable.",
                "Native recipe remainders and fuel consumption are preserved; approving a recipe approves its native remainders.")
                .defineListAllowEmpty("automatedRecipes", List.<String>of(),
                        () -> "minecraft:fermented_spider_eye", CraftingRules::recipeEntry);
        b.pop();
        // TODO(D2): Hidebound Wolf Armor (1wYiddQdy_g4jt0LQLsvGoGo84KENclU0AagISUUVIB8)
        // is explicitly Dungeon 2 T1: six leather plus two twine/string. Do not seed a D1 recipe.
        // TODO(D82): custom Glowstone/Redstone brewing conversions are deferred beyond D1;
        // the reagent recipe allowlist does not implement or authorize those conversions.
    }
    static CraftingRules snapshot() {
        try { return CraftingRules.parse(ENABLED.get(), PLAYERS.get(), AUTOMATION.get()); }
        catch (IllegalArgumentException ex) {
            com.mojang.logging.LogUtils.getLogger().error("Crafting policy rejected; all recipes denied", ex);
            return new CraftingRules(true, java.util.Map.of(), java.util.Set.of());
        }
    }
    @SubscribeEvent public static void loading(ModConfigEvent.Loading event) { changed(event); }
    @SubscribeEvent public static void reloading(ModConfigEvent.Reloading event) { changed(event); }
    private static void changed(ModConfigEvent event) {
        if (event.getConfig().getSpec() != Config.SPEC) return;
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return; // Client config synchronization must never change server policy.
        var snapshot = snapshot();
        // A file-watcher thread must not change policy between output transfer and native remainders.
        server.execute(() -> {
            CraftingPolicy.install(snapshot);
            CraftingPolicy.validate(server);
            server.getPlayerList().getPlayers().forEach(CraftingPolicy::refreshPlayer);
        });
    }
}
