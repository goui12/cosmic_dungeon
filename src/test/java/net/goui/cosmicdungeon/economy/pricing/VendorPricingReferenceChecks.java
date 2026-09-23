package net.goui.cosmicdungeon.economy.pricing;

import com.electronwill.nightconfig.toml.TomlFormat;
import net.goui.cosmicdungeon.config.VendorCatalog;
import net.goui.cosmicdungeon.config.VendorPricesConfig;
import net.goui.cosmicdungeon.economy.CurrencyAmount;
import net.goui.cosmicdungeon.faction.NpcFactionService;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Random;

/** Source-versioned reference cases; no web access or Minecraft registry bootstrap. */
public final class VendorPricingReferenceChecks {
    private static int checks;
    private static void check(boolean ok,String why) { checks++; if (!ok) throw new AssertionError(why); }
    private static BigDecimal d(String value) { return new BigDecimal(value); }
    public static void main(String[] args) throws Exception {
        var defaults = TomlFormat.newConfig();
        VendorPricesConfig.SPEC.correct(defaults);
        var loaded=Class.forName("net.neoforged.fml.config.LoadedConfig").getDeclaredConstructor(
                com.electronwill.nightconfig.core.CommentedConfig.class,java.nio.file.Path.class,
                net.neoforged.fml.config.ModConfig.class);
        loaded.setAccessible(true);
        VendorPricesConfig.SPEC.acceptConfig((net.neoforged.fml.config.IConfigSpec.ILoadedConfig)
                loaded.newInstance(defaults,null,null));
        var purchases = new LinkedHashMap<String,Long>();
        var retail = new LinkedHashMap<String,Long>();
        int sourceRows=0, exceptions=0;
        try (var input = VendorPricingReferenceChecks.class.getResourceAsStream("/d1/pricing-master-2026-08-19.csv");
             var reader = new BufferedReader(new InputStreamReader(java.util.Objects.requireNonNull(input),StandardCharsets.UTF_8))) {
            reader.readLine();
            for (String line; (line=reader.readLine()) != null;) {
                String[] f=line.split(",");
                String group=f[0].equals("item") ? "Universal." : "Enchantments.";
                String purchaseKey=f[0].equals("item") ? ".purchaseTrace" : ".purchasePerLevelTrace";
                String retailKey=f[0].equals("item") ? ".retailTrace" : ".retailPerLevelTrace";
                long purchase=Long.parseLong(f[2]), listed=Long.parseLong(f[3]);
                check(((Number)defaults.get(group+f[1]+purchaseKey)).longValue()==purchase,"Source purchase "+f[1]);
                check(((Number)defaults.get(group+f[1]+retailKey)).longValue()==listed,"Source retail "+f[1]);
                check(f[4].equals("1B3hQLrrOkZeRPG1tomd54rd7v7OKQ_PDNozHH-DIffY"),"Versioned authority");
                if (f[0].equals("item")) {
                    purchases.put(f[1],purchase);
                    retail.put(f[1],NpcFactionService.adjusted(listed,.90));
                    if (listed != BigDecimal.valueOf(purchase).multiply(d("1.20")).setScale(0,RoundingMode.CEILING).longValueExact()) exceptions++;
                }
                sourceRows++;
            }
        }
        check(sourceRows==182 && purchases.size()==141,"Complete current item/enchantment schedule");
        check(exceptions>10,"Manual/category prices retained instead of overwritten with universal markup");
        var caps=VendorConversionRules.ceilings(k->purchases.getOrDefault(k,-1L),k->retail.getOrDefault(k,-1L));
        String[] potionKeys={"healing","strong_healing","regeneration","strength","night_vision","fire_resistance","long_water_breathing","invisibility"};
        long[] expected={16,18,16,11,30,12,11,33};
        for (int i=0;i<potionKeys.length;i++)
            check(caps.get("potion__"+potionKeys[i])==expected[i],"Reviewed brew "+potionKeys[i]);
        check(caps.get("splash_potion__healing")==17,"Three healing bottles share one powder and one fuel use");
        check(caps.get("splash_potion__poison")==14,"Unlisted poison intermediate has production cost only");
        check(caps.get("splash_potion__invisibility")==34,"Cheaper purchased intermediate cannot create vendor value");
        check(caps.get("lingering_potion__healing")==26,"Returned dragon-breath bottle included among outputs");
        check(caps.get("lingering_potion__regeneration")==26,"Unstocked intermediate does not become sale-eligible");
        check(caps.get("glass_bottle")==3 && caps.get("bucket")==3,"Container-cycle ceilings");
        for (String key:new String[]{"baked_potato","dried_kelp"}) check(caps.get(key)==1,"No cooking inflation "+key);
        for (String key:new String[]{"beef","chicken","cod","mutton","porkchop","rabbit","salmon"})
            check(caps.get("cooked_"+key)==2,"Zero-fuel campfire route "+key);
        check(!VendorConversionRules.covered("potion__awkward") && !VendorConversionRules.covered("diamond_sword"),
                "Reference intermediates do not create catalogue eligibility or touch equipment");
        check(VendorConversionRules.appliesTo("potion__healing",ItemTransferPolicy.Action.VENDOR_SALE),
                "Reviewed vendor conversions use the ceiling");
        check(!VendorConversionRules.appliesTo("potion__healing",ItemTransferPolicy.Action.TRADE),
                "Vendor conversion restrictions do not restrict player-to-player pricing");
        var changed=new HashMap<>(purchases);
        changed.put("potion__invisibility",-1L);
        var disabled=VendorConversionRules.ceilings(k->changed.getOrDefault(k,-1L),k->retail.getOrDefault(k,-1L));
        check(disabled.get("potion__invisibility")==-1 && disabled.get("splash_potion__invisibility")==-1,"Disabled input and dependent output reject");
        check(disabled.get("potion__healing")==16 && disabled.get("glass_bottle")==3,"Disabled later potion cannot disable unrelated D1 prices");
        changed.put("potion__invisibility",48L); changed.put("glistering_melon_slice",-1L);
        var missing=VendorConversionRules.ceilings(k->changed.getOrDefault(k,-1L),k->retail.getOrDefault(k,-1L));
        check(missing.get("potion__healing")==-1 && missing.get("lingering_potion__healing")==-1,"Missing consumed-input price rejects dependent conversions");
        check(missing.get("potion__regeneration")==16,"Missing ingredient stays local to its routes");
        changed.clear();changed.putAll(purchases);changed.put("honey_bottle",0L);
        var zero=VendorConversionRules.ceilings(k->changed.getOrDefault(k,-1L),k->retail.getOrDefault(k,-1L));
        check(zero.get("glass_bottle")==0 && zero.get("potion__healing")==13,"Approved zero input cannot be washed through bottle/brew");
        changed.put("blaze_powder",-1L);
        check(VendorConversionRules.ceilings(k->changed.getOrDefault(k,-1L),k->retail.getOrDefault(k,-1L)).get("potion__healing")==-1,"Unknown brewing fuel fails closed");
        var cheap=new HashMap<>(retail);cheap.put("potion__night_vision",1L);
        var retailCap=VendorConversionRules.ceilings(k->purchases.getOrDefault(k,-1L),k->cheap.getOrDefault(k,-1L));
        check(retailCap.get("potion__invisibility")<caps.get("potion__invisibility"),"Cheaper direct retail intermediate tightens downstream quote");
        check(VendorConversionRules.ceiling(d("100"),d("100"),BigDecimal.ZERO,1)==99,"Equal retail total forbidden");
        check(VendorConversionRules.ceiling(d("100"),d("6.01"),BigDecimal.ZERO,3)==2,"Fractional strict ceiling");
        check(VendorConversionRules.ceiling(d("100"),d("6.00"),BigDecimal.ZERO,3)==1,"Exact integral strict ceiling");
        check(VendorConversionRules.ceiling(d("10"),null,d("3"),3)==2,"Returned output cannot be credited as consumed");
        check(VendorConversionRules.ceiling(null,d("10"),BigDecimal.ZERO,3)==-1,"Missing input is unpriced, not zero surrender");
        check(VendorConversionRules.ceiling(d("10"),d("0"),BigDecimal.ZERO,3)==-1,"Free retail cannot satisfy strict positive margin");
        // Integer cents oracle independently checks exact whole-Trace inequalities.
        var random=new Random(350035);
        for (int i=0;i<500;i++) {
            long in=random.nextInt(100000), buy=random.nextInt(100000), other=random.nextInt(10000);int count=1+random.nextInt(64);
            long budget=Math.min(in-other,buy-1-other);
            long oracle=budget<0 ? -1 : budget/(100L*count);
            check(VendorConversionRules.ceiling(BigDecimal.valueOf(in,2),BigDecimal.valueOf(buy,2),BigDecimal.valueOf(other,2),count)==oracle,"Integer oracle "+i);
        }
        var reads=new HashMap<String,Integer>();
        VendorConversionRules.ceilings(k->{reads.merge("p:"+k,1,Integer::sum);return purchases.getOrDefault(k,-1L);},
                k->{reads.merge("r:"+k,1,Integer::sum);return retail.getOrDefault(k,-1L);});
        check(reads.values().stream().allMatch(n->n==1) && reads.size()<100,"Bounded per-quote config snapshot");
        var bucket=VendorPriceBreakdown.calculate(50,0,0,0,caps.get("bucket"),2);
        check(bucket.base()==100 && bucket.adjustments()==-94 && bucket.total()==6,"Source list price and live adjustment visible");
        check(VendorPriceBreakdown.calculate(50,0,0,0,1L,2).total()==2,"Existing lower item cap remains authoritative");
        // Calculator version20, 2026-08-20. Synthetic boundary cases exercise its documented modes.
        calculator("3",".75",null,null,2,3);
        calculator("0","1",null,null,1,2);
        calculator("3.5","1",null,null,4,5);
        calculator("2.49","1",null,null,2,3);
        calculator(null,null,"50",null,50,60);
        calculator(null,null,"0",null,0,1);
        calculator(null,null,"18","24",18,24);
        calculator(null,null,"20","48",20,48);
        calculator(null,null,"2.5","3.5",3,4);
        check(CurrencyAmount.ofTrace(12345).formatNormalized().equals("1A 2C 3S 4M 5T"),"Calculator denomination example, compact game notation");
        check(CurrencyAmount.ofTrace(0).formatNormalized().equals("0T"),"Zero display");
        check(NpcFactionService.adjusted(5,.90)==5 && NpcFactionService.adjusted(15,.90)==14,"Faction half-up differs from base retail ceil");
        for (String key:new String[]{"fishing_rod","enchanted_book","firework_rocket","diamond_shovel","diamond_spear"})
            check(VendorCatalog.item(key)==null,"No inferred base price "+key);
        check(VendorCatalog.enchantment("fortune")==null && VendorCatalog.enchantment("lunge")==null,"Excluded/future enchantments remain unpriced");
        check(VendorCatalog.enchantment("sweeping_edge")==VendorCatalog.enchantment("sweeping"),
                "Native Sweeping Edge shares existing config and overrides");
        for (var entry:net.goui.cosmicdungeon.item.identity.D1LootCatalog.entries()) {
            var quote=NamedLootPricing.quote(entry.id(),entry.baseItem(),entry.defaultPurchaseTrace(),null,1);
            check(quote.traceValue()==entry.defaultPurchaseTrace(),"Named table remains final whole-item value "+entry.id());
        }
        check(VendorPricesConfig.conversionRetail("potion__night_vision")==36,"Native potion offer alias");
        var night=VendorPricesConfig.find(net.minecraft.resources.ResourceLocation.parse("cosmicdungeon:d1/brewing_store"),
                net.minecraft.resources.ResourceLocation.parse("minecraft:potion_of_night_vision"));
        try {
            night.retail().set(1L);
            check(VendorPricesConfig.conversionRetail("potion__night_vision")==1,"Changed live offer tightens conversion floor");
            night.retail().set(-1L);
            check(VendorPricesConfig.conversionRetail("potion__night_vision")==36,"Disabled offer is excluded from listed floor");
        } finally { night.retail().set(36L); }
        check(VendorPricesConfig.conversionRetail("beef")==3,"Raw-food offer alias");
        check(VendorPricesConfig.conversionRetail("potion__strong_healing")==48,"Potency II offer alias");
        check(VendorPricesConfig.conversionRetail("potion__long_water_breathing")==48,"Extended offer alias without enabling D2 stock");
        check(VendorPricesConfig.conversionRetail("glistering_melon_slice")==24,"Ingredient legacy alias");
        System.out.println(checks+" vendor source/calculator/conversion checks passed");
    }
    private static void calculator(String usd,String modifier,String manual,String override,long expectedPurchase,long expectedRetail) {
        long purchase=manual==null ? Math.max(1,d(usd).multiply(d(modifier)).setScale(0,RoundingMode.HALF_UP).longValueExact())
                : Math.max(0,d(manual).setScale(0,RoundingMode.HALF_UP).longValueExact());
        long retail=override==null ? Math.max(1,BigDecimal.valueOf(purchase).multiply(d("1.20")).setScale(0,RoundingMode.CEILING).longValueExact())
                : Math.max(1,d(override).setScale(0,RoundingMode.HALF_UP).longValueExact());
        check(purchase==expectedPurchase && retail==expectedRetail,"Version20 formula/manual/override fixture");
    }
}
