package net.goui.cosmicdungeon;

import com.electronwill.nightconfig.core.file.FormatDetector;
import com.electronwill.nightconfig.toml.TomlFormat;
import net.neoforged.neoforge.common.ModConfigSpec;
import java.util.LinkedHashMap;
import java.util.Map;

/** Server-authoritative tuning. Units and canon/implementation choices belong beside each setting. */
public final class Config {
    private Config() {}
    private static final ModConfigSpec.Builder B = new ModConfigSpec.Builder();
    public static final Map<String, ModConfigSpec.DoubleValue> CLASS_DAMAGE = new LinkedHashMap<>();
    public static final ModConfigSpec.IntValue ACTIVITY_POLL_SECONDS;
    public static final ModConfigSpec.IntValue ITEM_AUTHORING_SECONDS;
    public static final ModConfigSpec.IntValue PROTECTED_RECOVERY_BATCH;
    public static final ModConfigSpec.DoubleValue ITEM_AUTHORING_RANGE;
    public static final ModConfigSpec.IntValue BASE_CAMP_POLL_TICKS, TAX_CONFIRM_SECONDS;
    public static final ModConfigSpec.DoubleValue BASE_CAMP_RADIUS;
    public static final ModConfigSpec.IntValue CHOP_COOK_TICKS, CHOP_RECOVERY_POLL_TICKS, INN_QUOTE_SECONDS;
    public static final ModConfigSpec.DoubleValue INN_RANGE, CHOP_COOK_RANGE;
    public static final ModConfigSpec.IntValue BLOOM_ACTIVITY_WINDOW_SECONDS, LINK_DEAD_SECONDS, FLAG_DISCONNECT_GRACE_SECONDS;
    public static final ModConfigSpec.IntValue BELL_WINDOW_TICKS, BELL_COUNT;
    public static final ModConfigSpec.IntValue CANDLE_SCAN_BUDGET, CANDLE_SCAN_MAX_VOLUME;
    public static final ModConfigSpec.IntValue MUSIC_DISC_COUNT;
    public static final ModConfigSpec.IntValue CANDLE_COLOR_COUNT;
    public static final ModConfigSpec.IntValue PIGLIN_HEAD_COUNT, PIGLIN_POLL_TICKS, COMPANIONSHIP_SECONDS, RIFT_COOLDOWN_TICKS, RIFT_RETRY_TICKS;
    public static final ModConfigSpec.DoubleValue WATSON_RADIUS;
    public static final ModConfigSpec.IntValue WATSON_POLL_TICKS, WATSON_RECOVERY_POLL_TICKS;
    public static final ModConfigSpec.BooleanValue INSTANT_BREWING;
    public static final ModConfigSpec.IntValue BREW_TICKS;
    public static final ModConfigSpec.DoubleValue CHAIN_CHANCE;
    public static final ModConfigSpec.DoubleValue CHAIN_RADIUS;
    public static final ModConfigSpec.IntValue CHAIN_TARGET_LIMIT;
    public static final ModConfigSpec.IntValue CHAIN_CANDIDATE_LIMIT;
    public static final ModConfigSpec.DoubleValue CHAIN_DAMAGE;
    public static final ModConfigSpec.IntValue REPAIR_POLL_SECONDS;
    public static final ModConfigSpec.LongValue ACCOUNT_CAPACITY;
    public static final ModConfigSpec.DoubleValue WOLF_HEALTH;
    public static final ModConfigSpec.DoubleValue WOLF_DAMAGE;
    public static final ModConfigSpec.DoubleValue WOLF_SPEED;
    public static final ModConfigSpec.DoubleValue WOLF_FOLLOW_RANGE;
    public static final ModConfigSpec.DoubleValue WOLF_TELEPORT_DISTANCE;
    public static final ModConfigSpec.IntValue WOLF_CAP;
    public static final ModConfigSpec.DoubleValue WOLF_RAW_HEAL, WOLF_COOKED_HEAL;
    public static final ModConfigSpec.IntValue WOLF_BREED_TICKS;
    public static final ModConfigSpec.DoubleValue WOLF_TAME_CHANCE;
    public static final ModConfigSpec.DoubleValue WOLF_PUP_GROWTH;
    public static final ModConfigSpec.IntValue WOLF_RECOVERY_CHUNKS, WOLF_RECOVERY_LOAD_TICKS, WOLF_RECOVERY_SNAPSHOTS, WOLF_COMMAND_TICKS, WOLF_REVIEW_SECONDS;
    public static final ModConfigSpec.IntValue WOLF_DURATION_MINUTES, WOLF_EXPIRY_POLL_TICKS;
    public static final ModConfigSpec.IntValue WOLF_THREAT_POLL_TICKS, WOLF_THREAT_CANDIDATES, WOLF_THREAT_SCANS;
    public static final ModConfigSpec.IntValue PYRO_GRAVEL_COST;
    public static final ModConfigSpec.IntValue PYRO_FLINT_COST;
    public static final ModConfigSpec.IntValue PYRO_GUNPOWDER_YIELD;
    public static final ModConfigSpec.DoubleValue PASSIVE_REPAIR_HEALTH_COST, PASSIVE_REPAIR_HEALTH_GATE;
    public static final ModConfigSpec.IntValue REPAIR_STEP_TICKS, REPAIR_READY_TICKS;
    public static final ModConfigSpec.DoubleValue REPAIR_RANGE;
    public static final ModConfigSpec.IntValue NPC_BLOOM_GAIN, NPC_KILL_LOSS;
    public static final ModConfigSpec.DoubleValue VENDOR_BINDING_RANGE;
    public static final ModConfigSpec.DoubleValue CORDIAL_RETAIL, WARM_RETAIL, ALLY_RETAIL;
    public static final ModConfigSpec.IntValue MIN_PARTY, READY_COUNTDOWN_SECONDS, SELECTOR_SESSION_SECONDS;
    public static final ModConfigSpec.DoubleValue SELECTOR_RANGE;
    public static final ModConfigSpec.IntValue PARTY_INVITATION_SECONDS, PARTY_INVITE_COOLDOWN_TICKS,
            PARTY_POLL_TICKS, PARTY_ACTION_TICKS;
    public static final ModConfigSpec.IntValue MENU_BALANCE_POLL_TICKS, SPAWNER_MAINTENANCE_BUDGET;
    public static final ModConfigSpec SPEC;
    static {
        B.comment("Implementation work bounds; not lore or damage modifiers.").push("Performance");
        MENU_BALANCE_POLL_TICKS=B.comment("Server ticks between open vendor/trade/repair balance polls; unchanged values send no packet.")
                .defineInRange("menuBalancePollTicks",20,5,200);
        SPAWNER_MAINTENANCE_BUDGET=B.comment("Global maximum queued spawner maintenance visits per server tick, shared fairly between loaded spawners. New entity presets apply at admission.")
                .defineInRange("spawnerMaintenanceVisitsPerTick",512,16,4096);
        B.pop();
        B.comment("D1 only. Run objectives reset with the instance; lifetime records remain.",
                "Cameron Q&A D10/D20/D23/D69, 2026-09-16.").push("Dungeon1");
        ACTIVITY_POLL_SECONDS = B.comment("Seconds between player activity samples; Q&A D10.")
                .defineInRange("activityPollSeconds", 15, 1, 60);
        BLOOM_ACTIVITY_WINDOW_SECONDS=B.comment("Seconds either side of spectral Bloom collection; Bloom and NPC Unlock System.")
                .defineInRange("bloomActivityWindowSeconds",300,1,3600);
        LINK_DEAD_SECONDS=B.comment("Disconnected instance members are removed after this period.")
                .defineInRange("linkDeadRemovalSeconds",600,60,86400);
        FLAG_DISCONNECT_GRACE_SECONDS=B.comment("Plant Flags waits five minutes after a member disconnects.")
                .defineInRange("flagDisconnectGraceSeconds",300,0,3600);
        B.pop();
        B.comment("Tamsin Vane Internal: D1 parties contain three to six personally ready players.").push("TamsinVane");
        BASE_CAMP_POLL_TICKS=B.comment("Server ticks between personal Base Camp proximity checks. Implementation default.")
                .defineInRange("baseCampPollTicks",20,1,200);
        BASE_CAMP_RADIUS=B.comment("Blocks around the explicitly authored base_camp binding; no guessed location. Implementation default.")
                .defineInRange("baseCampDiscoveryRadius",4.0,0.5,32.0);
        TAX_CONFIRM_SECONDS=B.comment("Seconds before a selected Tax item confirmation expires. Implementation default.")
                .defineInRange("taxConfirmationSeconds",30,5,300);
        MIN_PARTY=B.defineInRange("minimumPartySize",3,3,6);
        READY_COUNTDOWN_SECONDS=B.defineInRange("readyCountdownSeconds",5,1,60);
        SELECTOR_SESSION_SECONDS=B.defineInRange("selectorSessionSeconds",300,30,3600);
        SELECTOR_RANGE=B.defineInRange("selectorInteractionRange",16.0,1.0,64.0);
        PARTY_INVITATION_SECONDS=B.comment("Pending invitations expire after these seconds, including onboarding. Implementation default.")
                .defineInRange("invitationLifetimeSeconds",600,30,3600);
        PARTY_INVITE_COOLDOWN_TICKS=B.comment("Minimum server ticks between successful invitations from one player.")
                .defineInRange("invitationCooldownTicks",100,20,1200);
        PARTY_POLL_TICKS=B.comment("Bounded lobby eligibility/countdown check interval; 20 ticks is one second.")
                .defineInRange("partyPollTicks",20,5,100);
        PARTY_ACTION_TICKS=B.comment("Minimum server ticks between group or Tax menu mutations from one player.")
                .defineInRange("partyActionCooldownTicks",4,1,20);
        B.pop();
        B.comment("Protected equipment recovery and developer-only adoption.").push("ItemProtection");
        PROTECTED_RECOVERY_BATCH=B.comment("Maximum eligible overflow or stored-inventory stacks processed by one /d1 recover or /d1 claim command.")
                .defineInRange("recoveryStacksPerClaim",32,1,128);
        ITEM_AUTHORING_SECONDS=B.comment("Seconds to apply or undo one explicit held/container item adoption.")
                .defineInRange("authoringPreviewSeconds",120,10,1800);
        ITEM_AUTHORING_RANGE=B.comment("Maximum blocks to an already-loaded container selected for adoption.")
                .defineInRange("authoringContainerRange",16.0,1.0,64.0);
        B.pop();
        B.push("SharedTravel");
        RIFT_COOLDOWN_TICKS=B.comment("Successful rift cooldown; preserves the existing twelve-tick delay.")
                .defineInRange("riftCooldownTicks",12,1,1200);
        RIFT_RETRY_TICKS=B.comment("Minimum delay after a denied or unsafe rift attempt; bounds repeated checks/messages.")
                .defineInRange("riftRetryTicks",40,20,1200);
        COMPANIONSHIP_SECONDS=B.comment("Legacy Potion of Companionship cooldown and selection lifetime, seconds.",
                "Preserves the existing five-minute value; no new recipe or unlock is inferred.")
                .defineInRange("companionshipSeconds",300,1,3600);
        B.pop();
        B.push("Achievements");
        PIGLIN_HEAD_COUNT=B.comment("Debloated Achievements!C20: six characters wearing vanilla Piglin Heads simultaneously at Camp 4.")
                .defineInRange("piglinHeadPlayers",6,1,6);
        PIGLIN_POLL_TICKS=B.comment("Ticks between bounded Camp 4 roster checks; no entity or block scan.")
                .defineInRange("piglinHeadPollTicks",20,1,200);
        BELL_WINDOW_TICKS = B.comment("20 ticks = one second. Six DIFFERENT bells, same instance.",
                "D70 mentions both two and six seconds. Default two seconds matches 'at once'; set 120 for six.",
                "Source: 1L_CmsTIWQ9TADh_1T18G_ukG61y1oExpH6tgzM9IKUw.")
                .defineInRange("synchronousPealWindowTicks", 40, 1, 1200);
        BELL_COUNT = B.comment("Distinct successfully rung physical bells in Camp 5. Canon: six.")
                .defineInRange("synchronousPealBellCount",6,1,64);
        CANDLE_SCAN_BUDGET = B.comment("Global maximum room blocks examined each server tick after candle changes.")
                .defineInRange("candleScanBlocksPerTick",1024,64,8192);
        CANDLE_SCAN_MAX_VOLUME = B.comment("Maximum authored Wither-room bounding volume. Oversized bindings are logged and rejected.")
                .defineInRange("candleScanMaxRoomBlocks",1048576,4096,16777216);
        MUSIC_DISC_COUNT = B.comment("Distinct music discs actually played in one D1 run. Q&A D74.")
                .defineInRange("recordedSoundDiscCount", 7, 1, 64);
        CANDLE_COLOR_COUNT = B.comment("Different lit candle colors on chiseled tuff. Q&A D71; Wither variants deferred.")
                .defineInRange("sixfoldVigilColors", 6, 1, 16);
        B.pop();
        B.push("Beluzon");
        INN_QUOTE_SECONDS=B.comment("Seconds before a one-time bond quote expires. Implementation default; price lives in all_vendors_prices.config.")
                .defineInRange("bondQuoteSeconds",30,5,300);
        INN_RANGE=B.comment("Blocks from the existing profiled Creaking when confirming a bond. Implementation default.")
                .defineInRange("bondInteractionRange",8.0,1.0,32.0);
        B.pop();
        B.push("BeatrixFarrow");
        CHOP_COOK_TICKS=B.comment("Farrow Chop Internal: approximately four seconds. Q&A D25 permits any lit campfire.")
                .defineInRange("chopCookTicks",80,1,1200);
        CHOP_RECOVERY_POLL_TICKS=B.comment("Ticks between owner-local stale-return checks. No world scan.")
                .defineInRange("chopRecoveryPollTicks",20,10,200);
        CHOP_COOK_RANGE=B.comment("Blocks allowed from the lit campfire while cooking. Implementation default.")
                .defineInRange("chopCookRange",6.0,1.0,16.0);
        B.pop();
        B.push("JohnWatson");
        WATSON_RADIUS = B.comment("Blocks: every active instance member must gather within this radius. Q&A D23/D56.")
                .defineInRange("gatheringRadius", 16.0, 1.0, 128.0);
        WATSON_POLL_TICKS = B.comment("Server ticks between bounded checks of configured Watson locations.")
                .defineInRange("gatheringPollTicks", 20, 1, 1200);
        WATSON_RECOVERY_POLL_TICKS = B.comment("Ticks between recovery attempts; at most one pending outcome per poll. Offline inputs wait without disk retries.")
                .defineInRange("outcomeRecoveryPollTicks", 100, 20, 1200);
        B.pop();
        B.push("Economy");
        ACCOUNT_CAPACITY = B.comment("Trace; source 17ufIuIy0VhLmB_V-6sZ7sCaUCZuGZUkHrgJLVpEcS28.",
                "Existing explicit player capacity overrides remain authoritative.")
                .defineInRange("defaultAccountCapacityTrace", 100_000_000L, 0L, Long.MAX_VALUE);
        B.pop();
        for (String cls : new String[]{"bogatyr", "dragoon", "judicator", "pyroclast",
                "theurgist", "venefex", "deadeye", "metalmancer"}) {
            B.push(Character.toUpperCase(cls.charAt(0)) + cls.substring(1));
            CLASS_DAMAGE.put(cls, B.comment("Final outgoing damage multiplier; 1 preserves the source-defined base.",
                    "Deadeye/Metalmancer remain unavailable in D1; this does not enable later classes.")
                    .defineInRange("damageMultiplier", 1.0, 0.0, 100.0));
            B.pop();
        }
        B.comment("Wolf (Internal), edited 2026-04-25: 10-3IgopUqHKyPHuZDKlpa64JMYXmq-_8GgKtQFhLX3c.").push("Bogatyr");
        WOLF_HEALTH = B.comment("Health points; two points per heart.").defineInRange("wolfHealth", 20.0, 1.0, 1024.0);
        WOLF_DAMAGE = B.comment("Attack damage in health points.").defineInRange("wolfDamage", 4.0, 0.0, 1024.0);
        WOLF_SPEED = B.defineInRange("wolfMovementSpeed", 0.3, 0.0, 2.0);
        WOLF_FOLLOW_RANGE = B.comment("Blocks.").defineInRange("wolfFollowRange", 10.0, 1.0, 128.0);
        WOLF_TELEPORT_DISTANCE = B.comment("Blocks from owner; safe positions only.").defineInRange("wolfTeleportDistance", 20.0, 2.0, 128.0);
        WOLF_RAW_HEAL=B.comment("Health points restored by raw meat: two hearts.")
                .defineInRange("rawMeatHealing",4.0,0.0,1024.0);
        WOLF_COOKED_HEAL=B.comment("Health points restored by cooked meat: four hearts.")
                .defineInRange("cookedMeatHealing",8.0,0.0,1024.0);
        WOLF_BREED_TICKS=B.comment("Wolf Internal: five seconds of breeding.")
                .defineInRange("breedingTicks",100,1,1200);
        WOLF_CAP = B.comment("Active companions per Bogatyr. Tamed bonds are permanent by default.",
                "Unloaded existing wolves and prepared deliveries count. Stored archives keep ownership but free active slots.",
                "TODO(D27/D28, D2+): temporary scapula recruits and scaling totem auras; see Bogatyr deferred notes.")
                .defineInRange("maxWolves", 5, 1, 32);
        WOLF_TAME_CHANCE = B.comment("Probability per bone.").defineInRange("wolfTameChance", 0.33, 0.0, 1.0);
        WOLF_PUP_GROWTH = B.comment("Fraction of remaining pup growth per meat item. Wolf Internal: 10 percent.",
                "Rounded up to a server tick. Feeding an injured pup both heals and grows it; zero disables growth acceleration.")
                .defineInRange("pupGrowthFraction", 0.10, 0.0, 1.0);
        WOLF_RECOVERY_CHUNKS=B.comment("Maximum concurrently requested companion chunks; no forced chunks.")
                .defineInRange("recoveryConcurrentChunks",2,1,8);
        WOLF_RECOVERY_LOAD_TICKS=B.comment("Ticks before an unresolved chunk request can be retried; missing wolves hold reset.")
                .defineInRange("recoveryChunkTimeoutTicks",200,20,2400);
        WOLF_RECOVERY_SNAPSHOTS=B.comment("Maximum companion snapshot/transfer operations per tick across resets and recall commands.")
                .defineInRange("recoverySnapshotsPerTick",2,1,16);
        WOLF_COMMAND_TICKS=B.comment("Minimum ticks between owner companion recall/recovery attempts; limits save and chunk requests.")
                .defineInRange("companionCommandIntervalTicks",20,1,1200);
        WOLF_REVIEW_SECONDS=B.comment("Developer companion review preview lifetime in seconds; apply rechecks exact saved state and live ownership.")
                .defineInRange("recoveryReviewSeconds",120,5,600);
        WOLF_DURATION_MINUTES=B.comment("Wolf duration in minutes: set to 0 for permanent (newest Wolf Internal default).",
                "Positive values count loaded companion time; unload/archive/zero setting pauses the counter.",
                "Changing the positive duration uses the existing active-time counter. On expiry the wolf is released",
                "and worn armor becomes an owner-only world drop at its location, subject to ordinary world hazards.",
                "The permanent companion cap is maxWolves above; no second conflicting cap is introduced.")
                .defineInRange("wolfDurationMinutes",0,0,525600);
        WOLF_EXPIRY_POLL_TICKS=B.comment("Ticks between expired-bond release attempts; canceled armor drops retain the wolf.")
                .defineInRange("expiryRetryTicks",20,1,1200);
        WOLF_THREAT_POLL_TICKS=B.comment("Ticks between source-ranked threat selection; shared per-owner candidate cache.")
                .defineInRange("threatPollTicks",20,1,200);
        WOLF_THREAT_CANDIDATES=B.comment("Maximum nearby mobs inspected per spatial threat query; follow range sets radius.")
                .defineInRange("threatCandidateLimit",64,8,256);
        WOLF_THREAT_SCANS=B.comment("Global maximum owner threat queries per server tick.")
                .defineInRange("threatQueriesPerTick",8,1,32);
        B.pop();
        B.push("Dragoon");
        CHAIN_CHANCE = B.comment("Probability per successful trident hit; debloated Classes!S4.")
                .defineInRange("chainLightningChance", 0.03, 0.0, 1.0);
        CHAIN_RADIUS = B.comment("Blocks. Server line-of-sight approximation for 'on screen'; bounded implementation choice.")
                .defineInRange("chainLightningRadius", 32.0, 1.0, 128.0);
        CHAIN_TARGET_LIMIT = B.comment("Safety bound on additional visible hostile targets; original victim is not hit twice.")
                .defineInRange("chainLightningTargetLimit", 64, 1, 256);
        CHAIN_CANDIDATE_LIMIT = B.comment("Maximum nearby mobs inspected per chain trigger; bounds candidate storage and LOS work. Crowded scenes may omit targets.")
                .defineInRange("chainLightningCandidateLimit", 256, 1, 1024);
        CHAIN_DAMAGE = B.comment("Multiplier of the triggering hit's final damage.")
                .defineInRange("chainLightningDamageMultiplier", 1.0, 0.0, 100.0);
        PASSIVE_REPAIR_HEALTH_COST = B.comment("Health points per durability point. Repair 2.0: one heart.")
                .defineInRange("passiveRepairHealthCost", 2.0, 0.0, 100.0);
        PASSIVE_REPAIR_HEALTH_GATE = B.comment("Must be above this fraction of maximum health.")
                .defineInRange("passiveRepairHealthGate", 0.50, 0.0, 1.0);
        REPAIR_STEP_TICKS = B.comment("Ticks per selected 25-percent increment. Full weapon kits take four increments.")
                .defineInRange("repairStepTicks", 40, 1, 1200);
        REPAIR_READY_TICKS = B.comment("Repair 2.0: start deadline after both players Ready. Default thirty seconds.")
                .defineInRange("repairReadyTicks", 600, 20, 12000);
        REPAIR_RANGE = B.comment("Maximum distance between Dragoon and customer, inclusive.")
                .defineInRange("repairLinkRange", 3.0, 1.0, 32.0);
        REPAIR_POLL_SECONDS = B.comment("Passive armor repair polling interval; conservative Q&A D75.")
                .defineInRange("passiveRepairPollSeconds", 15, 1, 300);
        B.pop();
        B.push("Theurgist");
        INSTANT_BREWING = B.comment("Q&A D14: instant brewing for Theurgists; all other player classes cannot open brewing GUI.")
                .define("instantBrewing", true);
        BREW_TICKS = B.comment("Ticks remaining after the vanilla recipe/fuel check. One tick is effectively instant.")
                .defineInRange("brewTicks", 1, 1, 400);
        B.pop();
        B.push("Pyroclast");
        PYRO_GRAVEL_COST = B.comment("Flint Alchemy: gravel consumed per tool use.").defineInRange("gravelCost", 1, 1, 64);
        PYRO_FLINT_COST = B.comment("Flint consumed per tool use.").defineInRange("flintCost", 1, 1, 64);
        PYRO_GUNPOWDER_YIELD = B.comment("Gunpowder produced per tool use.").defineInRange("gunpowderYield", 1, 1, 64);
        B.pop();
        B.comment("NPC and Vendor Faction, 11Cwgha2loiAQfMJwKLWEC_dD3jir3VybyfFvrNZBUyY, 2026-08-18.")
                .push("NpcFaction");
        VENDOR_BINDING_RANGE = B.comment("Maximum block distance for developer vendor assign, clear and info; line of sight is required.")
                .defineInRange("vendorBindingRange", 6.0, 1.0, 32.0);
        NPC_BLOOM_GAIN = B.defineInRange("pointsPerLesserBloom", 1, 0, 100);
        NPC_KILL_LOSS = B.defineInRange("pointsLostPerNpcKill", 26, 0, 200);
        CORDIAL_RETAIL = B.comment("Faction 5-49. Applied once to complete subtotal, half-up whole Trace.")
                .defineInRange("cordialRetailMultiplier", 1.10, 0.01, 100.0);
        WARM_RETAIL = B.comment("Faction 50-99.").defineInRange("warmlyRetailMultiplier", 1.0, 0.01, 100.0);
        ALLY_RETAIL = B.comment("Faction 100.").defineInRange("allyRetailMultiplier", 0.90, 0.01, 100.0);
        B.pop();
        net.goui.cosmicdungeon.economy.D1EconomyConfig.define(B);
        net.goui.cosmicdungeon.playerclass.d1.D1AbilityConfig.define(B);
        SPEC = B.build();
    }

    public static void registerFileFormat() {
        FormatDetector.registerExtension("config", TomlFormat.instance());
    }

    public static double outgoingDamage(String classId) {
        var value = CLASS_DAMAGE.get(classId);
        return value == null ? 1.0 : value.get();
    }
}
