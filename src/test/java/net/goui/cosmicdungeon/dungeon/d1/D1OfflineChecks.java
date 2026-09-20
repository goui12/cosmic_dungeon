package net.goui.cosmicdungeon.dungeon.d1;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.toml.TomlFormat;
import net.goui.cosmicdungeon.Config;
import net.goui.cosmicdungeon.config.VendorPricesConfig;
import net.neoforged.neoforge.common.ModConfigSpec;
import java.nio.file.Files;
import java.nio.file.Path;

public final class D1OfflineChecks {
    private static int configChecks;
    private static void check(boolean ok, String name) { configChecks++; if (!ok) throw new AssertionError(name); }
    public static void main(String[] args) throws Exception {
        // Match mod startup: define configuration before any saved-account service is exercised.
        Config.registerFileFormat();
        var runtimeDefaults=TomlFormat.newConfig();Config.SPEC.correct(runtimeDefaults);
        // NeoForge seals ILoadedConfig. Construct its native in-memory wrapper for this fixture;
        // no path or mod container is supplied, and all values are corrected before acceptance.
        var loadedConfig=Class.forName("net.neoforged.fml.config.LoadedConfig").getDeclaredConstructor(
                com.electronwill.nightconfig.core.CommentedConfig.class,Path.class,net.neoforged.fml.config.ModConfig.class);
        loadedConfig.setAccessible(true);
        Config.SPEC.acceptConfig((net.neoforged.fml.config.IConfigSpec.ILoadedConfig)loadedConfig.newInstance(runtimeDefaults,null,null));
        net.goui.cosmicdungeon.playerclass.d1.D1CombatChecks.main(args);
        net.goui.cosmicdungeon.playerclass.bogatyr.BogatyrBehaviourChecks.main(args);
        net.goui.cosmicdungeon.playerclass.bogatyr.BogatyrArchiveChecks.main(args);
        net.goui.cosmicdungeon.playerclass.bogatyr.BogatyrDirectoryChecks.main(args);
        net.goui.cosmicdungeon.playerclass.bogatyr.BogatyrCareChecks.main(args);
        net.goui.cosmicdungeon.achievement.d1.D1JournalRewardChecks.main(args);
        net.goui.cosmicdungeon.achievement.d1.D1RoomAchievementChecks.main(args);
        net.goui.cosmicdungeon.item.identity.ItemMovementChecks.main(args);
        net.goui.cosmicdungeon.item.identity.ItemLifecycleChecks.main(args);
        net.goui.cosmicdungeon.item.identity.ProtectedRecoveryChecks.main(args);
        net.goui.cosmicdungeon.npc.tamsin.TamsinTaxChecks.main(args);
        net.goui.cosmicdungeon.item.identity.D1ItemIdentityChecks.main(args);
        net.goui.cosmicdungeon.item.identity.D1AdoptionChecks.main(args);
        net.goui.cosmicdungeon.npc.tamsin.D1PartyChecks.main(args);
        net.goui.cosmicdungeon.npc.tamsin.TamsinChecks.main(args);
        net.goui.cosmicdungeon.economy.pricing.VendorPriceBreakdownChecks.main(args);
        net.goui.cosmicdungeon.economy.pricing.VendorPricingReferenceChecks.main(args);
        net.goui.cosmicdungeon.vendor.VendorSaleQuoteChecks.main(args);
        D1AuthoredBindingChecks.main(args);
        D1ObjectiveRulesTest.main(args);
        D1SavedDataChecks.main(args);
        WatsonOutcomeChecks.main(args);
        net.goui.cosmicdungeon.economy.AccountTransferChecks.main(args);
        net.goui.cosmicdungeon.playerclass.dragoon.repair.RepairCustodyChecks.main(args);
        net.goui.cosmicdungeon.playerclass.dragoon.repair.RepairCommitChecks.main(args);
        net.goui.cosmicdungeon.trade.TradeCustodyChecks.main(args);
        net.goui.cosmicdungeon.trade.TradeCommitChecks.main(args);
        net.goui.cosmicdungeon.vendor.CommerceChecks.main(args);
        net.goui.cosmicdungeon.economy.EconomyLedgerChecks.main(args);
        net.goui.cosmicdungeon.economy.WealthReviewChecks.main(args);
        net.goui.cosmicdungeon.economy.LegacyCurrencyChecks.main(args);
        net.goui.cosmicdungeon.economy.DeathCurrencyChecks.main(args);
        net.goui.cosmicdungeon.npc.inn.InnChecks.main(args);
        net.goui.cosmicdungeon.npc.inn.InnHookChecks.main(args);
        net.goui.cosmicdungeon.dungeon.ChopTravelChecks.main(args);
        net.goui.cosmicdungeon.dungeon.ChopRecoveryChecks.main(args);
        net.goui.cosmicdungeon.dungeon.InventoryHandoffChecks.main(args);
        var gameplay = TomlFormat.newConfig();
        Config.SPEC.correct(gameplay);
        check(((Number)gameplay.get("ItemProtection.recoveryStacksPerClaim")).intValue()==32,
                "Protected return processing has a bounded configurable claim budget");
        check(Config.SPEC.isCorrect(gameplay), "Generated gameplay defaults validate");
        gameplay.set("Dragoon.chainLightningCandidateLimit",64);Config.SPEC.correct(gameplay);
        check(((Number)gameplay.get("Dragoon.chainLightningCandidateLimit")).intValue()==64,"Dragoon candidate override retained");
        gameplay.set("Dragoon.chainLightningCandidateLimit",0);Config.SPEC.correct(gameplay);
        check(((Number)gameplay.get("Dragoon.chainLightningCandidateLimit")).intValue()==1,"Zero candidate budget rejected");
        gameplay.set("Pyroclast.rocketCandidateLimit",1025);Config.SPEC.correct(gameplay);
        check(((Number)gameplay.get("Pyroclast.rocketCandidateLimit")).intValue()==1024,"Rocket work cap enforced");
        gameplay.set("Pyroclast.rocketCandidateLimit",32);Config.SPEC.correct(gameplay);
        check(((Number)gameplay.get("Pyroclast.rocketCandidateLimit")).intValue()==32,"Rocket candidate override retained");
        gameplay.remove("Dragoon.chainLightningCandidateLimit");gameplay.remove("Pyroclast.rocketCandidateLimit");
        Config.SPEC.correct(gameplay);
        check(((Number)gameplay.get("Dragoon.chainLightningCandidateLimit")).intValue()==256
                &&((Number)gameplay.get("Pyroclast.rocketCandidateLimit")).intValue()==256,"Missing new keys receive defaults");
        check(((Number)gameplay.get("Beluzon.bondQuoteSeconds")).intValue()==30,"Inn quote lifetime default");
        check(((Number)gameplay.get("Beluzon.bondInteractionRange")).doubleValue()==8,"Inn server range default");
        check(((Number)gameplay.get("BeatrixFarrow.chopRecoveryPollTicks")).intValue()==20,"Chop recovery local poll interval");
        check(((Number)gameplay.get("BeatrixFarrow.chopCookRange")).doubleValue()==6,"Chop cooking range default");
        gameplay.set("Beluzon.bondInteractionRange",12.0);Config.SPEC.correct(gameplay);
        check(((Number)gameplay.get("Beluzon.bondInteractionRange")).doubleValue()==12,"Developer Inn range retained");
        gameplay.set("BeatrixFarrow.chopRecoveryPollTicks",0);Config.SPEC.correct(gameplay);
        check(((Number)gameplay.get("BeatrixFarrow.chopRecoveryPollTicks")).intValue()==10,"Zero Chop polling interval rejected");
        check(((Number)gameplay.get("NpcFaction.vendorBindingRange")).doubleValue()==6,"Vendor authoring range default");
        gameplay.set("NpcFaction.vendorBindingRange",12.0);Config.SPEC.correct(gameplay);
        check(((Number)gameplay.get("NpcFaction.vendorBindingRange")).doubleValue()==12,"Developer binding range retained");
        check(((java.util.List<?>)gameplay.get("Economy.registeredSpawnerRewards")).isEmpty(),"No guessed encounter coordinates");
        gameplay.set("Economy.registeredSpawnerRewards",java.util.List.of("cosmic_spawner_-1_64_3=miniboss"));
        Config.SPEC.correct(gameplay);
        check(((java.util.List<?>)gameplay.get("Economy.registeredSpawnerRewards")).size()==1,"Authored encounter config retained");
        check(((Number)gameplay.get("Economy.deathLossThresholdTrace")).longValue()==20,"Death threshold default");
        check(((Number)gameplay.get("Economy.deathLossFraction")).doubleValue()==.02,"Death percentage default");
        check(((Number)gameplay.get("Economy.minimumDeathLossTrace")).longValue()==1,"Death minimum default");
        check(((Number)gameplay.get("Economy.deathDropWorkPerTick")).intValue()==8,"Death recovery has a global work budget");
        check(((Number)gameplay.get("Economy.deathDropSnapshotIntervalTicks")).intValue()==100,"Death checkpoint cadence");
        check(((Number)gameplay.get("Economy.deathDropRecoveryRetryTicks")).intValue()==100,"No every-tick disk retry");
        gameplay.set("Economy.deathDropWorkPerTick",3);Config.SPEC.correct(gameplay);
        check(((Number)gameplay.get("Economy.deathDropWorkPerTick")).intValue()==3,"Developer recovery budget retained");
        check(((Number)gameplay.get("Economy.earlyWealthReviewTrace")).longValue()==500_000
                &&((Number)gameplay.get("Economy.highWealthReviewTrace")).longValue()==80_000_000
                &&((Number)gameplay.get("Economy.maximumWealthReviewTrace")).longValue()==100_000_000,"Canonical three wealth thresholds");
        check(((Number)gameplay.get("Economy.wealthReviewIntervalTicks")).intValue()==100,"Wealth review cadence");
        check(((Number)gameplay.get("Economy.wealthReviewWorkPerInterval")).intValue()==8,"Wealth review global budget");
        gameplay.set("Economy.wealthReviewWorkPerInterval",0);Config.SPEC.correct(gameplay);
        check(((Number)gameplay.get("Economy.wealthReviewWorkPerInterval")).intValue()==1,"Zero review budget rejected");
        gameplay.set("Economy.wealthReviewIntervalTicks",400);Config.SPEC.correct(gameplay);
        check(((Number)gameplay.get("Economy.wealthReviewIntervalTicks")).intValue()==400,"Developer review cadence retained");
        check(((Number)gameplay.get("Economy.ledgerFlushIntervalTicks")).intValue()==1200,"Ledger flush defaults to one minute");
        check(((Number)gameplay.get("Economy.ledgerRowsPerFlush")).intValue()==256,"Ledger archive work is bounded");
        gameplay.set("Economy.ledgerRowsPerFlush",64);Config.SPEC.correct(gameplay);
        check(((Number)gameplay.get("Economy.ledgerRowsPerFlush")).intValue()==64,"Ledger developer budget is retained");
        check(((Number)gameplay.get("Bogatyr.wolfDurationMinutes")).intValue()==0,"Wolf bond is permanent by default");
        check(((Number)gameplay.get("Bogatyr.expiryRetryTicks")).intValue()==20,"Expired armor handoff retry bounded");
        check(((Number)gameplay.get("Bogatyr.threatPollTicks")).intValue()==20,"Owner threat cache default");
        check(((Number)gameplay.get("Bogatyr.threatCandidateLimit")).intValue()==64,"Threat inspection budget");
        check(((Number)gameplay.get("Bogatyr.threatQueriesPerTick")).intValue()==8,"Global threat query budget");
        gameplay.set("Bogatyr.wolfDurationMinutes",1);Config.SPEC.correct(gameplay);
        check(((Number)gameplay.get("Bogatyr.wolfDurationMinutes")).intValue()==1,"Positive duration override retained");
        gameplay.set("Bogatyr.wolfDurationMinutes",-1);Config.SPEC.correct(gameplay);
        check(((Number)gameplay.get("Bogatyr.wolfDurationMinutes")).intValue()==0,"Negative duration cannot expire pets");
        check(((Number)gameplay.get("Bogatyr.recoveryConcurrentChunks")).intValue()==2,"Recovery chunk concurrency bound");
        check(((Number)gameplay.get("Bogatyr.recoveryChunkTimeoutTicks")).intValue()==200,"Recovery loading timeout");
        check(((Number)gameplay.get("Bogatyr.recoverySnapshotsPerTick")).intValue()==2,"Global companion snapshot budget");
        check(((Number)gameplay.get("Bogatyr.recoveryReviewSeconds")).intValue()==120,"Developer review expiry default");
        gameplay.set("Bogatyr.recoveryReviewSeconds",30);Config.SPEC.correct(gameplay);
        check(((Number)gameplay.get("Bogatyr.recoveryReviewSeconds")).intValue()==30,"Developer review expiry override retained");
        gameplay.set("Bogatyr.recoveryReviewSeconds",0);Config.SPEC.correct(gameplay);
        check(((Number)gameplay.get("Bogatyr.recoveryReviewSeconds")).intValue()==5,"Review cannot have an unbounded zero lifetime");
        check(((Number)gameplay.get("Bogatyr.companionCommandIntervalTicks")).intValue()==20,"Companion command save throttle default");
        gameplay.set("Bogatyr.companionCommandIntervalTicks",40);Config.SPEC.correct(gameplay);
        check(((Number)gameplay.get("Bogatyr.companionCommandIntervalTicks")).intValue()==40,"Developer command interval override retained");
        gameplay.set("Bogatyr.companionCommandIntervalTicks",0);Config.SPEC.correct(gameplay);
        check(((Number)gameplay.get("Bogatyr.companionCommandIntervalTicks")).intValue()==1,"Command interval cannot disable throttling");

        check(((Number)gameplay.get("Bogatyr.pupGrowthFraction")).doubleValue()==.10,"Ten-percent pup default");
        gameplay.set("Bogatyr.pupGrowthFraction",.25);Config.SPEC.correct(gameplay);
        check(((Number)gameplay.get("Bogatyr.pupGrowthFraction")).doubleValue()==.25,"Pup growth override retained");
        gameplay.set("Bogatyr.pupGrowthFraction",-1.0);Config.SPEC.correct(gameplay);
        check(((Number)gameplay.get("Bogatyr.pupGrowthFraction")).doubleValue()==0,"Negative growth clamped");
        check(((Number)gameplay.get("Achievements.synchronousPealBellCount")).intValue()==6,"Six distinct bell default");
        check(((Number)gameplay.get("Achievements.candleScanBlocksPerTick")).intValue()==1024,"Candle scan has global tick budget");
        check(((Number)gameplay.get("Achievements.candleScanMaxRoomBlocks")).intValue()==1048576,"Candle room binding has volume bound");
        check(((Number)gameplay.get("Dragoon.repairReadyTicks")).intValue()==600,"Repair Ready defaults to thirty seconds");
        gameplay.set("Dragoon.repairReadyTicks",200);Config.SPEC.correct(gameplay);
        check(((Number)gameplay.get("Dragoon.repairReadyTicks")).intValue()==200,"Repair Ready window is configurable");
        gameplay.set("Dragoon.repairReadyTicks",0);Config.SPEC.correct(gameplay);
        check(((Number)gameplay.get("Dragoon.repairReadyTicks")).intValue()==20,"Ready lifetime must be positive");
        check(((Number) gameplay.get("Dragoon.chainLightningChance")).doubleValue() == 0.03, "Canon chain probability");
        check(((Number) gameplay.get("JohnWatson.gatheringRadius")).doubleValue() == 16.0, "Watson radius");
        check(((Number)gameplay.get("JohnWatson.outcomeRecoveryPollTicks")).intValue()==100,"Watson save recovery cadence");
        gameplay.set("JohnWatson.outcomeRecoveryPollTicks",200);Config.SPEC.correct(gameplay);
        check(((Number)gameplay.get("JohnWatson.outcomeRecoveryPollTicks")).intValue()==200,"Watson recovery override retained");
        gameplay.set("JohnWatson.outcomeRecoveryPollTicks",0);Config.SPEC.correct(gameplay);
        check(((Number)gameplay.get("JohnWatson.outcomeRecoveryPollTicks")).intValue()==20,"Watson cannot retry disk every tick");
        check(((Number) gameplay.get("Economy.defaultAccountCapacityTrace")).longValue() == 100000000L, "Account capacity");
        check(((Number)gameplay.get("Theurgist.verdant_jolt.power")).doubleValue()==0.8,"Newest Verdant overview wins");
        check(((Number)gameplay.get("Pyroclast.cindermaul.power")).doubleValue()==15,"Newest rocket overview wins");
        check(((Number)gameplay.get("Economy.partyRewardRadius")).doubleValue()==60,"Canon currency eligibility range");
        check(((Number)gameplay.get("TamsinVane.minimumPartySize")).intValue()==3,"D1 minimum party");
        check(((Number)gameplay.get("TamsinVane.invitationLifetimeSeconds")).intValue()==600,"Pending onboarding expiry default");
        check(((Number)gameplay.get("TamsinVane.partyPollTicks")).intValue()==20,"Bounded party polling default");
        check(((Number)gameplay.get("TamsinVane.invitationCooldownTicks")).intValue()==100,"Invitation cooldown default");
        check(((Number)gameplay.get("TamsinVane.partyActionCooldownTicks")).intValue()==4,"Action cooldown default");
        check(((Number)gameplay.get("TamsinVane.baseCampPollTicks")).intValue()==20,"Camp polling default");
        check(((Number)gameplay.get("TamsinVane.baseCampDiscoveryRadius")).doubleValue()==4.0,"Explicit camp radius default");
        check(((Number)gameplay.get("TamsinVane.taxConfirmationSeconds")).intValue()==30,"Tax quote expiry default");
        check(((Number)gameplay.get("ItemProtection.authoringPreviewSeconds")).intValue()==120,"Adoption window default");
        check(((Number)gameplay.get("ItemProtection.authoringContainerRange")).doubleValue()==16.0,"Adoption range default");
        gameplay.set("Dragoon.chainLightningChance", -1.0);
        Config.SPEC.correct(gameplay);
        check(((Number) gameplay.get("Dragoon.chainLightningChance")).doubleValue() == 0.0, "Invalid probability clamped to lower bound");
        var prices = TomlFormat.newConfig();
        VendorPricesConfig.SPEC.correct(prices);
        check(VendorPricesConfig.SPEC.isCorrect(prices), "Generated price defaults validate");
        check(((Number) prices.get("NatonWhitlock.torch.retailTrace")).longValue() == 2L, "Naton documented price");
        check(((Number) prices.get("BeatrixFarrow.raw_farrows_chop.retailTrace")).longValue() == 2000L, "Chop documented price");
        check(((Number) prices.get("Universal.potion__strong_healing.purchaseTrace")).longValue() == 20L, "Potion variant price");
        for (var entry : net.goui.cosmicdungeon.item.identity.D1LootCatalog.entries())
            check(((Number)prices.get("NamedDungeon1." + entry.id() + ".purchaseTrace")).longValue() == entry.defaultPurchaseTrace(),
                    "Named D1 default " + entry.id());
        for (long override : new long[]{-1, 0, 321}) {
            prices.set("NamedDungeon1.recovered_spyglass.purchaseTrace", override);
            VendorPricesConfig.SPEC.correct(prices);
            check(((Number)prices.get("NamedDungeon1.recovered_spyglass.purchaseTrace")).longValue() == override,
                    "Named disabled/zero/custom override retained");
        }
        prices.set("NatonWhitlock.torch.retailTrace", 37L);
        VendorPricesConfig.SPEC.correct(prices);
        check(((Number) prices.get("NatonWhitlock.torch.retailTrace")).longValue() == 37L, "Valid developer override retained");
        Path directory = Path.of(args[0]);
        Files.createDirectories(directory);
        export(Config.SPEC, directory.resolve("CosmicDungeon.config"));
        export(VendorPricesConfig.SPEC, directory.resolve("all_vendors_prices.config"));
        System.out.println((configChecks-2)+" config checks and two .config round trips passed");
    }
    private static void export(ModConfigSpec spec, Path path) {
        try (var file = CommentedFileConfig.builder(path).sync().build()) {
            file.clear(); spec.correct(file); file.save(); file.load();
            check(spec.isCorrect(file), "Round-trip config is valid: " + path.getFileName());
        }
    }
}
