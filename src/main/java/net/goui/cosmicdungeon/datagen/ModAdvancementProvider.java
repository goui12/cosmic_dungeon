package net.goui.cosmicdungeon.datagen;

import java.util.List;
import java.util.function.Consumer;
import net.goui.cosmicdungeon.CosmicDungeonMod;
import net.goui.cosmicdungeon.achievement.CosmicAchievementIds;
import net.goui.cosmicdungeon.block.ModBlocks;
import net.goui.cosmicdungeon.item.ModItems;
import net.minecraft.advancements.*;
import net.minecraft.advancements.critereon.ImpossibleTrigger;
import net.minecraft.advancements.critereon.PlayerTrigger;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.advancements.AdvancementSubProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;

/** Lore-defined achievements; retired displayless IDs preserve existing player progress. */
public final class ModAdvancementProvider implements AdvancementSubProvider {
    private static final ResourceLocation ROOT = id("root");
    private static final ResourceLocation BLOOMS_ROOT = id("blooms");

    @Override
    public void generate(HolderLookup.Provider registries, Consumer<AdvancementHolder> saver) {
        category(saver, ROOT, null, ModItems.ATTUNEMENT_TRACE.get(), "root");
        category(saver, BLOOMS_ROOT, ROOT, ModBlocks.BLOOM_OF_QUIET_ASSURANCE.get(), "blooms.root");
        bloom(saver, "quiet_assurance", ModBlocks.BLOOM_OF_QUIET_ASSURANCE.get());
        bloom(saver, "gentle_lies", ModBlocks.BLOOM_OF_GENTLE_LIES.get());
        bloom(saver, "waning_mercy", ModBlocks.BLOOM_OF_WANING_MERCY.get());
        bloom(saver, "constricting_bonds", ModBlocks.BLOOM_OF_CONSTRICTING_BONDS.get());
        bloom(saver, "unspoken_resignation", ModBlocks.BLOOM_OF_UNSPOKEN_RESIGNATION.get());
        bloom(saver, "elegy", ModBlocks.BLOOM_OF_ELEGY.get());
        for (ResourceLocation achievement : CosmicAchievementIds.ALL) {
            String key = achievement.getPath().substring("achievements/".length());
            manual(saver, achievement, ROOT, icon(key), "achievements." + key, "triggered");
        }
        // No display, toast, rewards or automatic criteria: these are not player achievements.
        // First-trade progress still retires the existing CAPS LOCK onboarding prompt.
        retired(saver, CosmicAchievementIds.FIRST_PLAYER_TRADE, "triggered");
        retired(saver, CosmicAchievementIds.PYROCLAST_BOOM, "triggered");
        retired(saver, id("dungeon_monsters"), "tick");
        retired(saver, id("player_classes"), "tick");
        retired(saver, id("pyroclast"), "tick");
    }

    private static ItemLike icon(String key) {
        return switch (key) {
            case "im_rich" -> ModItems.ATTUNEMENT_TRACE.get();
            case "the_tamsin_tax" -> Items.SPYGLASS;
            case "plant_flags" -> Items.WHITE_BANNER;
            case "contract_fulfilled", "entangled_obligor", "votary_of_the_idol",
                 "bound_devotee", "bonded_thrall", "contract_enforced", "binding_creditor",
                 "votary_of_the_ledger", "warden_of_the_ledger", "sovereign_of_the_bond" -> Items.TOTEM_OF_UNDYING;
            case "tired_not_broken" -> Items.PHANTOM_MEMBRANE;
            case "vital_exchange_1" -> ModItems.SCINTILLA_VITALIS.get();
            case "vital_exchange_2" -> ModItems.LUX_VITALIS.get();
            case "vital_exchange_3" -> ModItems.MENDING_STING.get();
            case "vital_exchange_4" -> ModItems.VERDANT_JOLT.get();
            case "sixfold_vigil" -> Items.CANDLE;
            case "sixfold_vigil_after_dissolution" -> Items.WHITE_CANDLE;
            case "sixfold_vigil_lone_adversary" -> Items.WITHER_SKELETON_SKULL;
            case "sixfold_vigil_twin_manifestation" -> Items.NETHER_STAR;
            case "cycle_of_recorded_sound" -> Items.MUSIC_DISC_CAT;
            case "synchronous_peal" -> Items.BELL;
            case "nostalgia_bait" -> ModItems.FARROWS_CHOP.get();
            case "wolves_in_piglin_clothing" -> Items.PIGLIN_HEAD;
            case "fire_escape" -> Items.MAGMA_CREAM;
            case "librarian_1" -> Items.WRITTEN_BOOK;
            case "shulker_express" -> Items.SHULKER_SHELL;
            case "stairway_to_heaven" -> Items.ELYTRA;
            default -> throw new IllegalArgumentException("Missing lore achievement icon: " + key);
        };
    }

    private static Component text(String key) {
        return Component.translatable("advancements." + CosmicDungeonMod.MOD_ID + "." + key);
    }

    private static void category(Consumer<AdvancementHolder> saver, ResourceLocation id,
                                 ResourceLocation parent, ItemLike icon, String key) {
        Advancement.Builder builder = Advancement.Builder.advancement()
                .display(icon, text(key + ".title"), text(key + ".desc"), null,
                        AdvancementType.TASK, false, false, false)
                .addCriterion("tick", PlayerTrigger.TriggerInstance.tick())
                .requirements(AdvancementRequirements.allOf(List.of("tick")));
        if (parent != null) builder.parent(parent);
        builder.save(saver, id.toString());
    }

    private static void bloom(Consumer<AdvancementHolder> saver, String key, ItemLike icon) {
        manual(saver, id("blooms/bloom_of_" + key), BLOOMS_ROOT, icon, "blooms." + key, "shared");
    }

    private static void manual(Consumer<AdvancementHolder> saver, ResourceLocation id, ResourceLocation parent,
                               ItemLike icon, String key, String criterion) {
        Advancement.Builder.advancement().parent(parent)
                .display(icon, text(key + ".title"), text(key + ".desc"), null, AdvancementType.TASK,
                        true, false, id.equals(CosmicAchievementIds.TAMSIN_TAX))
                .addCriterion(criterion, impossible())
                .requirements(AdvancementRequirements.allOf(List.of(criterion)))
                .save(saver, id.toString());
    }

    private static void retired(Consumer<AdvancementHolder> saver, ResourceLocation id, String criterion) {
        Advancement.Builder.advancement().parent(ROOT).addCriterion(criterion, impossible()).save(saver, id.toString());
    }

    private static Criterion<ImpossibleTrigger.TriggerInstance> impossible() {
        return new Criterion<>(CriteriaTriggers.IMPOSSIBLE, new ImpossibleTrigger.TriggerInstance());
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(CosmicDungeonMod.MOD_ID, path);
    }
}
