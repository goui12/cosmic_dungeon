package net.goui.cosmicdungeon.dungeon.d1;
import com.google.gson.*;
import com.mojang.serialization.*;
import net.goui.cosmicdungeon.vendor.VendorPurchaseLimitData;
import net.goui.cosmicdungeon.economy.PlayerCurrencyData;
import net.goui.cosmicdungeon.economy.D1RewardRules;
import net.minecraft.resources.ResourceLocation;
import java.util.*;
public final class D1SavedDataChecks {
    private static int checks;
    private static void check(boolean yes,String label){checks++;if(!yes)throw new AssertionError(label);}
    @SuppressWarnings("unchecked") private static <T> Codec<T> codec(Class<T> type)throws Exception{
        var field=type.getDeclaredField("CODEC");field.setAccessible(true);return (Codec<T>)field.get(null);
    }
    private static <T> T decode(Class<T> type,String json)throws Exception{return codec(type).parse(JsonOps.INSTANCE,JsonParser.parseString(json)).getOrThrow();}
    private static <T> T roundTrip(Class<T> type,T value)throws Exception{
        var encoded=codec(type).encodeStart(JsonOps.INSTANCE,value).getOrThrow();
        return codec(type).parse(JsonOps.INSTANCE,encoded).getOrThrow();
    }
    public static void main(String[] args)throws Exception{
        UUID player=UUID.fromString("00000000-0000-0000-0000-000000000001");
        var profile=ResourceLocation.parse("cosmicdungeon:d1/food_vendor");var offer=ResourceLocation.parse("cosmicdungeon:bread");
        String key=player+"|"+profile+"|"+offer;
        var stock=decode(VendorPurchaseLimitData.class,"{\"purchases\":{\""+key+"\":2}}");
        var morning=VendorPurchaseLimitData.class.getDeclaredMethod("morning",long.class);morning.setAccessible(true);
        morning.invoke(stock,17L);check(stock.getPurchaseCount(player,profile,offer)==2,"Old stock anchors without wiping purchases");
        morning.invoke(stock,16L);check(stock.getPurchaseCount(player,profile,offer)==2,"Time rollback never refills stock");
        morning.invoke(stock,18L);check(stock.getPurchaseCount(player,profile,offer)==0,"New morning refills stock");
        stock.recordPurchase(player,profile,offer);morning.invoke(stock,18L);
        check(stock.getPurchaseCount(player,profile,offer)==1,"Reopen same morning never refills stock");
        check(roundTrip(VendorPurchaseLimitData.class,stock).getPurchaseCount(player,profile,offer)==1,"Stock survives save round trip");
        var currency=decode(PlayerCurrencyData.class,"{\"balances\":{\""+player+"\":80000000},\"capacity_overrides\":{\""+player+"\":100}}");
        check(currency.getBalanceTrace(player)==80000000L,"Old account retained even when capacity is lower");
        check(!currency.hasReceipt("first_trace",player),"Old accounts decode absent receipts");
        check(roundTrip(PlayerCurrencyData.class,currency).getBalanceTrace(player)==80000000L,"Account codec keeps 64-bit balances");
        var run=decode(D1RunData.class,"{}");
        check(run.recordUnique(41,"discs","cat"),"First record accepted");
        check(!run.recordUnique(41,"discs","cat"),"Same instance duplicate rejected");
        check(run.recordUnique(42,"discs","cat"),"Separate instance remains independent");
        run.setCount(42,"lesser:"+player,6);run.clearRun(41);
        check(run.values(41,"discs").isEmpty()&&run.values(42,"discs").size()==1,"Reset clears only its instance");
        check(roundTrip(D1RunData.class,run).count(42,"lesser:"+player)==6,"Run objective persists through restart");
        check(run.achievementCredits(player).isEmpty(),"Old objective file decodes missing shared credits");
        UUID peer=UUID.fromString("00000000-0000-0000-0000-000000000002");
        String bell="cosmicdungeon:synchronous_peal";
        check(run.creditShared(42,bell,List.of(player,peer)),"First shared award records both recipients");
        check(run.values(42,"awarded").contains(bell),"Instance marker accompanies permanent entitlements");
        check(run.achievementCredits(peer).equals(List.of(bell)),"Offline peer entitlement is explicit");
        check(!run.creditShared(42,bell,List.of(player)),"Duplicate trigger cannot change recipients");
        var savedCredit=roundTrip(D1RunData.class,run);
        check(savedCredit.achievementCredits(player).equals(List.of(bell)),"Shared credit survives save reload");
        savedCredit.clearRun(42);
        check(savedCredit.values(42,"awarded").isEmpty(),"Run award marker resets");
        check(savedCredit.achievementCredits(peer).equals(List.of(bell)),"Earned offline credit survives cleanup");
        check(savedCredit.creditShared(43,bell,List.of(player)),"New instance can meet its objective independently");
        check(savedCredit.achievementCredits(player).size()==1,"Repeated runs do not duplicate permanent entitlements");
        check(!savedCredit.creditShared(43,"empty",List.of()),"No recipients cannot consume the instance milestone");
        check(!savedCredit.values(43,"awarded").contains("empty"),"Rejected empty award leaves progress available");
        check(roundTrip(D1RunData.class,savedCredit).achievementCredits(peer).equals(List.of(bell)),
                "Restart after reset still retains disconnected peer's award");
        var personal=decode(D1RunData.class,"{}");
        String librarian="cosmicdungeon:librarian_1";
        check(personal.creditPersonal(51,librarian,player),"Personal journal award recorded");
        check(personal.achievementCredits(peer).isEmpty(),"Reading never grants an unread peer's journal award");
        check(personal.creditPersonal(51,librarian,peer),"Another reader can independently finish same instance");
        check(!personal.creditPersonal(51,librarian,player),"Repeat read cannot duplicate personal entitlement");
        personal.clearRun(51);
        check(personal.values(51,"awarded:"+player).isEmpty(),"Per-run journal award marker resets");
        check(roundTrip(D1RunData.class,personal).achievementCredits(player).equals(List.of(librarian)),
                "Earned personal journal credit survives cleanup and save reload");
        check(personal.creditPersonal(52,librarian,player)&&personal.achievementCredits(player).size()==1,
                "A later run remains independent without duplicate lifetime credit");
        var lifetime=decode(D1LifetimeData.class,"{\"players\":{\""+player+"\":{\"lesser_blooms\":7}}}");
        check(lifetime.complete(player,100),"First completion accepted");
        check(!lifetime.complete(player,100)&&!lifetime.complete(player,99),"Duplicate or older completion cannot award twice");
        lifetime.recordLesserBlooms(player,2);lifetime=roundTrip(D1LifetimeData.class,lifetime);
        check(lifetime.totals(player).completions()==1&&lifetime.totals(player).spectralBlooms()==6&&lifetime.totals(player).lesserBlooms()==9,
                "Lifetime completion and collection survive independently");

        check(lifetime.totals(player).successfulKills()==0,"Old lifetime save defaults kills to zero");
        var killRun=decode(D1RunData.class,"{}");
        check(killRun.recordKill(200,player)==1 && killRun.recordKill(200,player)==2,"Run kills count");
        UUID other=new UUID(0,99);
        killRun.recordKill(201,player);
        killRun.recordKill(200,other);
        check(killRun.count(200,"kills:"+player)==2 && killRun.count(201,"kills:"+player)==1
                && killRun.count(200,"kills:"+other)==1,"Kills isolate instance and player");
        killRun=roundTrip(D1RunData.class,killRun);
        check(killRun.count(200,"kills:"+player)==2,"Pending kills survive normal reload");
        check(lifetime.complete(player,200,killRun.count(200,"kills:"+player)),"Successful run commits kills");
        check(!lifetime.complete(player,200,99)&&lifetime.totals(player).successfulKills()==2,"Replay cannot add kills");
        killRun.clearRun(201);
        check(lifetime.totals(player).successfulKills()==2,"Discarding failed run leaves lifetime unchanged");
        lifetime=roundTrip(D1LifetimeData.class,lifetime);
        check(lifetime.totals(player).successfulKills()==2 && lifetime.totals(player).lesserBlooms()==9,
                "Optional kill field round-trips without losing old totals");
        killRun.setCount(202,"kills:"+player,Integer.MAX_VALUE);
        check(killRun.recordKill(202,player)==Integer.MAX_VALUE,"Run kill count saturates");
        var order=new ArrayList<UUID>();for(int i=1;i<=6;i++)order.add(new UUID(0,i));
        var all=Set.copyOf(order);var totals=new HashMap<UUID,Long>();int cursor=0;
        for(int kill=0;kill<3;kill++){
            var split=D1RewardRules.split(10,order,all,cursor);cursor=split.nextCursor();
            check(split.shares().values().stream().mapToLong(Long::longValue).sum()==10,"No remainder is lost");
            split.shares().forEach((id,value)->totals.merge(id,value,Long::sum));
        }
        check(totals.values().stream().allMatch(value->value==5),"Remainder rotation is fair over three kills");
        var eligible=Set.of(order.get(1),order.get(4),order.get(5));
        var split=D1RewardRules.split(5,order,eligible,4);
        check(split.shares().get(order.get(1))==1&&split.shares().get(order.get(4))==2&&split.shares().get(order.get(5))==2,
                "Ineligible stable slots are skipped");
        check(split.nextCursor()==0,"Remainder cursor resumes after last recipient");
        check(D1RewardRules.split(2,order,all,0).shares().values().stream().mapToLong(Long::longValue).sum()==2,
                "Pool smaller than party is distributed");
        check(D1RewardRules.split(100,order,Set.of(),0).shares().isEmpty(),"No eligible party creates no payout");
        System.out.println(checks+" saved-data migration and reward checks passed");
    }
}
