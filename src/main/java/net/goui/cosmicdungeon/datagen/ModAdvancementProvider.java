package net.goui.cosmicdungeon.datagen;

import net.goui.cosmicdungeon.CosmicDungeonMod;
import net.goui.cosmicdungeon.achievement.CosmicAchievementIds;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementRequirements;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.advancements.critereon.ImpossibleTrigger;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.advancements.AdvancementSubProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;

import java.util.List;
import java.util.function.Consumer;

public final class ModAdvancementProvider implements AdvancementSubProvider {
    private static final List<ResourceLocation> ACHIEVEMENTS = CosmicAchievementIds.ALL;

    @Override
    public void generate(HolderLookup.Provider registries, Consumer<AdvancementHolder> saver) {
        for (ResourceLocation id : ACHIEVEMENTS) {
            String key = id.getPath().substring("achievements/".length());
            String base = "advancements." + CosmicDungeonMod.MOD_ID + ".achievements." + key;
            Advancement.Builder.advancement()
                    .display(
                            Items.PAPER,
                            Component.translatable(base + ".title"),
                            Component.translatable(base + ".desc"),
                            null,
                            AdvancementType.TASK,
                            true,
                            true,
                            false
                    )
                    .addCriterion("triggered", ImpossibleTrigger.TriggerInstance.simple())
                    .requirements(AdvancementRequirements.allOf(List.of("triggered")))
                    .save(saver, id.toString());
        }
    }
}
