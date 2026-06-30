package net.goui.cosmicdungeon.datagen;

import net.goui.cosmicdungeon.CosmicDungeonMod;
import net.goui.cosmicdungeon.achievement.CosmicAchievementIds;
import net.goui.cosmicdungeon.block.ModBlocks;
import net.goui.cosmicdungeon.item.ModItems;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementRequirements;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.critereon.ImpossibleTrigger;
import net.minecraft.advancements.critereon.PlayerTrigger;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.advancements.AdvancementSubProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.item.Items;

import java.util.List;
import java.util.function.Consumer;

public final class ModAdvancementProvider implements AdvancementSubProvider {
    private static final List<ResourceLocation> ACHIEVEMENTS = CosmicAchievementIds.ALL;
    private static final ResourceLocation ROOT = id("root");
    private static final ResourceLocation BLOOMS_ROOT = id("blooms");
    private static final ResourceLocation PYROCLAST_ROOT = id("pyroclast");

    @Override
    public void generate(HolderLookup.Provider registries, Consumer<AdvancementHolder> saver) {
        saveTickAdvancement(
                saver,
                ROOT,
                null,
                ModItems.SHATTERED_REALITY_OF_SHUDDE_MELL.get(),
                Component.literal("Cosmic Dungeon"),
                Component.literal("The ascent of insanity."),
                id("gui/advancements/region_ghost_glass"),
                false,
                false
        );
        saveTickAdvancement(
                saver,
                id("dungeon_monsters"),
                ROOT,
                ModItems.TIDAL_MACE.get(),
                Component.literal("Monster Compendium"),
                Component.literal("Can you kill them all?"),
                null,
                false,
                false
        );
        saveTickAdvancement(
                saver,
                id("player_classes"),
                ROOT,
                ModItems.BENT_ROD_OF_MELTED_SHAVINGS.get(),
                Component.literal("Player Classes"),
                Component.literal("Can you play them all?"),
                null,
                false,
                false
        );
        saveTickAdvancement(
                saver,
                BLOOMS_ROOT,
                ROOT,
                ModBlocks.BLOOM_OF_QUIET_ASSURANCE.get(),
                Component.translatable("advancements." + CosmicDungeonMod.MOD_ID + ".blooms.root.title"),
                Component.translatable("advancements." + CosmicDungeonMod.MOD_ID + ".blooms.root.desc"),
                null,
                false,
                false
        );
        saveTickAdvancement(
                saver,
                PYROCLAST_ROOT,
                ROOT,
                Items.GUNPOWDER,
                Component.translatable("advancements." + CosmicDungeonMod.MOD_ID + ".pyroclast.root.title"),
                Component.translatable("advancements." + CosmicDungeonMod.MOD_ID + ".pyroclast.root.desc"),
                null,
                false,
                false
        );

        saveBloom(saver, "quiet_assurance", ModBlocks.BLOOM_OF_QUIET_ASSURANCE.get());
        saveBloom(saver, "gentle_lies", ModBlocks.BLOOM_OF_GENTLE_LIES.get());
        saveBloom(saver, "waning_mercy", ModBlocks.BLOOM_OF_WANING_MERCY.get());
        saveBloom(saver, "constricting_bonds", ModBlocks.BLOOM_OF_CONSTRICTING_BONDS.get());
        saveBloom(saver, "unspoken_resignation", ModBlocks.BLOOM_OF_UNSPOKEN_RESIGNATION.get());
        saveBloom(saver, "elegy", ModBlocks.BLOOM_OF_ELEGY.get());

        for (ResourceLocation id : ACHIEVEMENTS) {
            if (id.getPath().startsWith("achievements/")) {
                saveGenericAchievement(saver, id);
            } else if (id.equals(CosmicAchievementIds.PYROCLAST_BOOM)) {
                saveManualAchievement(
                        saver,
                        id,
                        PYROCLAST_ROOT,
                        Items.TNT,
                        "advancements." + CosmicDungeonMod.MOD_ID + ".pyroclast.boom",
                        "triggered"
                );
            }
        }
    }

    private static void saveGenericAchievement(Consumer<AdvancementHolder> saver, ResourceLocation id) {
        String key = id.getPath().substring("achievements/".length());
        saveManualAchievement(saver, id, ROOT, Items.PAPER, "advancements." + CosmicDungeonMod.MOD_ID + ".achievements." + key, "triggered");
    }

    private static void saveBloom(Consumer<AdvancementHolder> saver, String key, ItemLike icon) {
        saveManualAchievement(saver, id("blooms/bloom_of_" + key), BLOOMS_ROOT, icon, "advancements." + CosmicDungeonMod.MOD_ID + ".blooms." + key, "shared");
    }

    private static void saveTickAdvancement(Consumer<AdvancementHolder> saver, ResourceLocation id, ResourceLocation parent, ItemLike icon,
                                            Component title, Component description, ResourceLocation background, boolean showToast,
                                            boolean announceChat) {
        Advancement.Builder builder = Advancement.Builder.advancement()
                .display(icon, title, description, background, AdvancementType.TASK, showToast, announceChat, false)
                .addCriterion("tick", PlayerTrigger.TriggerInstance.tick())
                .requirements(AdvancementRequirements.allOf(List.of("tick")));
        if (parent != null) builder.parent(parent);
        builder.save(saver, id.toString());
    }

    private static void saveManualAchievement(Consumer<AdvancementHolder> saver, ResourceLocation id, ResourceLocation parent, ItemLike icon,
                                              String translationBase, String criterionName) {
        Advancement.Builder.advancement()
                .parent(parent)
                .display(
                        icon,
                        Component.translatable(translationBase + ".title"),
                        Component.translatable(translationBase + ".desc"),
                        null,
                        AdvancementType.TASK,
                        true,
                        true,
                        false
                )
                .addCriterion(criterionName, new Criterion<>(CriteriaTriggers.IMPOSSIBLE, new ImpossibleTrigger.TriggerInstance()))
                .requirements(AdvancementRequirements.allOf(List.of(criterionName)))
                .save(saver, id.toString());
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(CosmicDungeonMod.MOD_ID, path);
    }
}
