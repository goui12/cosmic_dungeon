package net.goui.cosmicdungeon.economy;
import net.neoforged.neoforge.common.ModConfigSpec;
import java.util.*;
public final class D1EconomyConfig {
    public static ModConfigSpec.DoubleValue REWARD_RADIUS,DEATH_PERCENT;
    public static ModConfigSpec.IntValue VENDOR_QUOTE_TICKS,LEDGER_FLUSH_TICKS,LEDGER_ROWS_PER_FLUSH,DEATH_WORK_PER_TICK,DEATH_SNAPSHOT_TICKS,DEATH_RETRY_TICKS;
    public static ModConfigSpec.LongValue FIRST_TRACE,WEALTH_EARLY,WEALTH_HIGH,WEALTH_MAX,DEATH_THRESHOLD,DEATH_MIN;
    public static final Map<String,ModConfigSpec.LongValue> REWARDS=new LinkedHashMap<>();
    public static ModConfigSpec.ConfigValue<List<? extends String>> MOB_CATEGORIES;
    private D1EconomyConfig(){}
    public static void define(ModConfigSpec.Builder b){
        b.comment("Attunement Fragment Economy Internal, 17ufIuIy0VhLmB_V-6sZ7sCaUCZuGZUkHrgJLVpEcS28, 2026-08-18.")
                .push("Economy");
        LEDGER_FLUSH_TICKS=b.comment("Background ledger archive cadence; explicit commerce commits verify their own writes immediately.").defineInRange("ledgerFlushIntervalTicks",1200,20,24000);
        LEDGER_ROWS_PER_FLUSH=b.comment("Maximum evidence rows archived in one flush; remaining rows are retained in the account outbox.").defineInRange("ledgerRowsPerFlush",256,16,1024);
        VENDOR_QUOTE_TICKS=b.comment("Ticks before a displayed vendor sale quote expires; 20 ticks per second. No items move until confirmation.")
                .defineInRange("vendorQuoteLifetimeTicks",600,20,6000);
        REWARD_RADIUS=b.comment("Inclusive block distance, using a dead unrespawned member's death position.")
                .defineInRange("partyRewardRadius",60.0,1.0,256.0);
        FIRST_TRACE=b.comment("First Trace / Learn Trace are the same one-time achievement; Q&A D77.")
                .defineInRange("firstTraceReward",5L,0L,1_000_000L);
        WEALTH_EARLY=b.defineInRange("earlyWealthReviewTrace",500_000L,1L,Long.MAX_VALUE);
        WEALTH_HIGH=b.defineInRange("highWealthReviewTrace",80_000_000L,1L,Long.MAX_VALUE);
        WEALTH_MAX=b.defineInRange("maximumWealthReviewTrace",100_000_000L,1L,Long.MAX_VALUE);
        DEATH_WORK_PER_TICK=b.comment("Maximum logical drop records checked per server tick; never force-load chunks.").defineInRange("deathDropWorkPerTick",8,1,128);
        DEATH_SNAPSHOT_TICKS=b.comment("Minimum ticks between live projection checkpoints into account memory. Native item aging/despawn is unchanged.").defineInRange("deathDropSnapshotIntervalTicks",100,20,1200);
        DEATH_RETRY_TICKS=b.comment("Retry cadence after an uncertain save; no per-tick disk retries.").defineInRange("deathDropRecoveryRetryTicks",100,20,1200);
        DEATH_THRESHOLD=b.comment("Death loss is zero below this balance; otherwise floor(balance*fraction), with the minimum capped at available balance.")
                .defineInRange("deathLossThresholdTrace",20L,0L,Long.MAX_VALUE);
        DEATH_PERCENT=b.defineInRange("deathLossFraction",0.02,0.0,1.0);
        DEATH_MIN=b.defineInRange("minimumDeathLossTrace",1L,0L,Long.MAX_VALUE);
        b.push("MobRewards");
        for(var pair:Map.of("weak",2L,"common",3L,"tough",4L,"elite",6L,"miniboss",10L,"boss",100L).entrySet())
            REWARDS.put(pair.getKey(),b.defineInRange(pair.getKey(),pair.getValue(),0L,1_000_000_000L));
        b.pop();
        MOB_CATEGORIES=b.comment("Explicit entity-type registration. Each entry is namespace:entity=category or namespace:entity=wholeTrace.",
                "Initial ordinary-mob categories are developer defaults chosen from the supplied baseline table, not encounter-specific canon.",
                "Only mobs carrying the existing Cosmic Spawner provenance tag qualify. Existing spawner contents are untouched.",
                "An encounter may override with exactly one persistent cosmicdungeon.reward_category or cosmicdungeon.reward_trace value.",
                "Unknown or duplicate registrations award zero and warn instead of guessing from max health.")
                .defineListAllowEmpty("registeredMobRewards",List.of(
                    "minecraft:zombie=common","minecraft:skeleton=common","minecraft:husk=tough","minecraft:drowned=common",
                    "minecraft:spider=common","minecraft:cave_spider=weak","minecraft:creeper=tough","minecraft:stray=tough",
                    "minecraft:bogged=tough","minecraft:enderman=tough","minecraft:witch=elite","minecraft:silverfish=weak",
                    "minecraft:blaze=tough","minecraft:wither_skeleton=tough","minecraft:ghast=elite","minecraft:piglin=common",
                    "minecraft:piglin_brute=elite","minecraft:zombified_piglin=common","minecraft:hoglin=tough",
                    "minecraft:magma_cube=common","minecraft:slime=weak","minecraft:endermite=weak",
                    "minecraft:guardian=tough","minecraft:elder_guardian=miniboss","minecraft:warden=boss","minecraft:wither=boss",
                    "minecraft:ender_dragon=boss"),()->"minecraft:zombie=common",v->v instanceof String s&&s.matches("[a-z0-9_.-]+:[a-z0-9_./-]+=[a-z0-9_]+"));
        b.pop();
    }
}
