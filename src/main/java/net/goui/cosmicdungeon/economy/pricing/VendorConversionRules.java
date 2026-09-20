package net.goui.cosmicdungeon.economy.pricing;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.ToLongFunction;

/**
 * Reviewed 1.21.10 conversion subset; never creates catalogue eligibility or stock.
 * Pricing Master 2.0 (2026-08-19): total outputs cannot exceed consumed inputs and
 * must be strictly below the cheapest faction-adjusted retail route. D83 forbids
 * the milk/container exploit. All balance inputs come from server configuration.
 */
public final class VendorConversionRules {
    private VendorConversionRules() {}
    private static final BigDecimal THREE = BigDecimal.valueOf(3);
    private static final BigDecimal FUEL_SHARE = new BigDecimal("0.05"); // Native twenty brews/powder.
    private static final Map<String,String> COOKED = Map.ofEntries(
            Map.entry("baked_potato","potato"), Map.entry("dried_kelp","kelp"),
            Map.entry("cooked_beef","beef"), Map.entry("cooked_chicken","chicken"),
            Map.entry("cooked_cod","cod"), Map.entry("cooked_mutton","mutton"),
            Map.entry("cooked_porkchop","porkchop"), Map.entry("cooked_rabbit","rabbit"),
            Map.entry("cooked_salmon","salmon"));
    private record Brew(String output,String input,String ingredient,boolean returnsBottle) {}
    // Ingredient-first and container-first orders have the same consumed quantities.
    // Unlisted intermediate potions carry production cost only, never sale eligibility.
    private static final Brew[] BREWS = {
            new Brew("potion__awkward","potion__water","nether_wart",false),
            new Brew("potion__healing","potion__awkward","glistering_melon_slice",false),
            new Brew("potion__strong_healing","potion__healing","glowstone_dust",false),
            new Brew("potion__regeneration","potion__awkward","ghast_tear",false),
            new Brew("potion__strength","potion__awkward","blaze_powder",false),
            new Brew("potion__night_vision","potion__awkward","golden_carrot",false),
            new Brew("potion__fire_resistance","potion__awkward","magma_cream",false),
            new Brew("potion__water_breathing","potion__awkward","pufferfish",false),
            new Brew("potion__long_water_breathing","potion__water_breathing","redstone",false),
            new Brew("potion__invisibility","potion__night_vision","fermented_spider_eye",false),
            new Brew("potion__poison","potion__awkward","spider_eye",false),
            new Brew("splash_potion__healing","potion__healing","gunpowder",false),
            new Brew("splash_potion__poison","potion__poison","gunpowder",false),
            new Brew("splash_potion__invisibility","potion__invisibility","gunpowder",false),
            new Brew("splash_potion__regeneration","potion__regeneration","gunpowder",false),
            new Brew("lingering_potion__healing","splash_potion__healing","dragon_breath",true),
            new Brew("lingering_potion__regeneration","splash_potion__regeneration","dragon_breath",true)
    };
    private static final Set<String> HIDDEN = Set.of("potion__awkward","potion__water",
            "potion__water_breathing","potion__poison","splash_potion__regeneration");
    public static boolean appliesTo(String key,ItemTransferPolicy.Action action) {
        // Pricing Master: player-to-player value remains unrestricted.
        return action == ItemTransferPolicy.Action.VENDOR_SALE && covered(key);
    }
    public static boolean covered(String key) {
        if (COOKED.containsKey(key) || key.equals("bucket") || key.equals("glass_bottle")) return true;
        for (Brew brew : BREWS) if (brew.output.equals(key) && !HIDDEN.contains(key)) return true;
        return false;
    }
    private record Cost(BigDecimal purchase,BigDecimal retail) {}
    private static BigDecimal amount(long value) { return value < 0 ? null : BigDecimal.valueOf(value); }
    private static BigDecimal min(BigDecimal a,BigDecimal b) {
        return a == null ? b : b == null ? a : a.min(b);
    }
    private static BigDecimal plus(BigDecimal a,BigDecimal b) {
        return a == null || b == null ? null : a.add(b);
    }
    private static BigDecimal scale(BigDecimal a,BigDecimal factor) {
        return a == null ? null : a.multiply(factor);
    }
    /** Whole-Trace output budget; fractional fuel is retained until the final floor. */
    public static long ceiling(BigDecimal inputValue,BigDecimal retailCost,
                               BigDecimal otherOutputValue,int yield) {
        if (yield < 1 || inputValue == null || otherOutputValue == null
                || inputValue.signum() < 0 || otherOutputValue.signum() < 0)
            return -1;
        BigDecimal budget = inputValue.subtract(otherOutputValue);
        if (retailCost != null) {
            if (retailCost.signum() < 0) return -1;
            // Largest integral total strictly below retail cost (works at exact integers).
            budget = budget.min(retailCost.subtract(otherOutputValue)
                    .setScale(0,RoundingMode.CEILING).subtract(BigDecimal.ONE));
        }
        return budget.signum() < 0 ? -1
                : budget.divide(BigDecimal.valueOf(yield),0,RoundingMode.FLOOR).longValueExact();
    }
    /**
     * Bounded monotone closure handles drink -> bottle -> brew -> drink loops.
     * Negative values mean unpriced, not an approved zero-Trace surrender.
     * Retail inputs are conservative per-unit floors from actual config, already faction adjusted.
     */
    public static Map<String,Long> ceilings(ToLongFunction<String> rawPurchase,ToLongFunction<String> rawRetail) {
        // One snapshot per quote; repeated graph paths do not repeatedly scan offer configuration.
        ToLongFunction<String> purchase = memoized(rawPurchase), retail = memoized(rawRetail);
        Map<String,Long> result = new LinkedHashMap<>();
        long bottle = purchase.applyAsLong("glass_bottle");
        for (int pass=0;pass<16;pass++) {
            long previousBottle = bottle;
            bottle = returnedCeiling(bottle,"honey_bottle",purchase,retail);
            bottle = returnedCeiling(bottle,"dragon_breath",purchase,retail);
            Map<String,Cost> costs = new LinkedHashMap<>();
            // Water itself is free; the bottle is retained in each resulting potion.
            costs.put("potion__water",new Cost(scale(amount(bottle),THREE),scale(amount(retail.applyAsLong("glass_bottle")),THREE)));
            Cost fuel = new Cost(amount(purchase.applyAsLong("blaze_powder")),
                    amount(retail.applyAsLong("blaze_powder")));
            for (Brew brew : BREWS) {
                Cost input = costs.get(brew.input);
                BigDecimal consumed = plus(input.purchase,
                        plus(amount(purchase.applyAsLong(brew.ingredient)),scale(fuel.purchase,FUEL_SHARE)));
                BigDecimal bought = plus(input.retail,
                        plus(amount(retail.applyAsLong(brew.ingredient)),scale(fuel.retail,FUEL_SHARE)));
                BigDecimal returned = brew.returnsBottle ? amount(bottle) : BigDecimal.ZERO;
                if (HIDDEN.contains(brew.output)) {
                    // Keep the exact three-bottle batch budget, including fractional fuel.
                    costs.put(brew.output,new Cost(consumed,bought));
                    continue;
                }
                long value = Math.min(purchase.applyAsLong(brew.output),
                        ceiling(consumed,bought,returned,3));
                result.put(brew.output,value);
                BigDecimal manufacturedRetail = bought == null || returned == null ? null
                        : bought.subtract(returned).max(BigDecimal.ZERO);
                costs.put(brew.output,new Cost(scale(amount(value),THREE),
                        min(scale(amount(retail.applyAsLong(brew.output)),THREE),manufacturedRetail)));
            }
            for (String key : result.keySet())
                if (key.startsWith("potion__") && result.get(key) >= 0)
                    bottle = Math.min(bottle,result.get(key));
            // Disabled/unpriced potions are not evidence of a zero-priced drink.
            // VendorRemainderMixin separately gives their returned container a zero cap.
            if (bottle == previousBottle) break;
            if (pass == 15) {
                // An unsupported pathological configuration cannot escape the work bound.
                result.replaceAll((key,value)->-1L); bottle=-1;
            }
        }
        result.put("glass_bottle",bottle);
        result.put("bucket",returnedCeiling(purchase.applyAsLong("bucket"),"milk_bucket",purchase,retail));
        COOKED.forEach((output,input)-> result.put(output,Math.min(purchase.applyAsLong(output),
                ceiling(amount(purchase.applyAsLong(input)),amount(retail.applyAsLong(input)),BigDecimal.ZERO,1))));
        return Map.copyOf(result);
    }
    private static ToLongFunction<String> memoized(ToLongFunction<String> source) {
        Map<String,Long> values = new java.util.HashMap<>();
        return key -> values.computeIfAbsent(key, source::applyAsLong);
    }
    private static long returnedCeiling(long current,String input,ToLongFunction<String> purchase,
                                        ToLongFunction<String> retail) {
        if (purchase.applyAsLong(input) < 0) return current;
        return Math.min(current,ceiling(amount(purchase.applyAsLong(input)),
                amount(retail.applyAsLong(input)),BigDecimal.ZERO,1));
    }
    // Cooking caps deliberately credit ZERO fuel value: campfire cooking consumes no fuel.
    // A furnace/smoker may consume extra resources, but cannot justify a higher global price.
    // TODO(M12/M106, release gate): General crafting is unavailable in Gear Trading 2.0
    // (2026-08-18). Verify that restriction in authored D1 worlds, including ExtraInventoryMenu,
    // automated crafters and data packs. Before approving another recipe, audit ALL outputs,
    // yield, consumed fuel, returned containers, reusable equipment and every cheaper route.
    // Never grant eligibility from a candidate catalogue or strip existing item-specific caps.
    // Custom recipes/fuels and washing a restricted component through native conversion need
    // licensed integration coverage; this finite vanilla schedule is not a whole-recipe proof.
}
