package net.goui.cosmicdungeon.item.identity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import net.goui.cosmicdungeon.economy.pricing.*;
import java.util.*;
import static net.goui.cosmicdungeon.economy.pricing.ItemTransferPolicy.*;

/** Offline domain/codec fixtures; actual Minecraft ItemStack/menu behavior needs licensed TEST. */
public final class D1ItemIdentityChecks {
    private static int checks;
    private static void check(boolean ok, String message) { checks++; if (!ok) throw new AssertionError(message); }
    private static Facts facts(boolean protectedItem, boolean noTrade, boolean noSale,
                               boolean present, boolean valid, boolean required, boolean repair, boolean repairValid) {
        return new Facts(false, protectedItem, noTrade, noSale, present, valid, required, repair, repairValid);
    }
    private static void both(Facts facts, Rejection expected, String label) {
        check(evaluate(Action.TRADE, facts) == expected, label + " trade");
        check(evaluate(Action.VENDOR_SALE, facts) == expected, label + " sale");
    }
    public static void main(String[] args) {
        checks = 0;
        check(D1LootCatalog.entries().size() == 23 && D1LootCatalog.ids().size() == 23, "Exactly 23 unique D1 identities");
        check(D1LootCatalog.find("recovered_spyglass").defaultPurchaseTrace() == 900, "Spyglass exception");
        check(D1LootCatalog.find("ranseur_of_the_fallen_dragoon").baseItem().equals("minecraft:trident"), "Ranseur vanilla base");
        check(D1LootCatalog.find("can_opener").baseItem().equals("minecraft:mace"), "Can Opener vanilla base");
        check(D1LootCatalog.find("Recovered Spyglass") == null && D1LootCatalog.find("d2_item") == null, "Display names and future items grant no identity");
        for (var entry : D1LootCatalog.entries()) {
            var value = new ItemProvenance(ItemProvenance.LOOT, entry.id(), entry.baseItem());
            check(ItemProvenance.parse(value.encode()).orElseThrow().equals(value), "Round trip " + entry.id());
            var json = Codec.STRING.encodeStart(JsonOps.INSTANCE, value.encode()).getOrThrow();
            check(ItemProvenance.parse(Codec.STRING.parse(JsonOps.INSTANCE, json).getOrThrow()).orElseThrow().equals(value), "Persistent component codec " + entry.id());
            check(value.matches(entry.baseItem()) && !value.matches("minecraft:stick"), "Exact vanilla base " + entry.id());
            var price = NamedLootPricing.quote(entry.id(), entry.baseItem(), entry.defaultPurchaseTrace(), null, 1);
            check(price.approved() && price.traceValue() == entry.defaultPurchaseTrace(), "Authoritative named total " + entry.id());
            check(price.breakdown().enchantments() == 0 && price.breakdown().curses() == 0, "No double enchantment value " + entry.id());
            check(NamedLootPricing.quote(entry.id(), entry.baseItem(), 77, null, 3).traceValue() == 231, "Configured unit times count " + entry.id());
            check(!NamedLootPricing.quote(entry.id(), "minecraft:stick", 900, null, 1).approved(), "Wrong base rejected " + entry.id());
            check(NamedLootPricing.quote(entry.id(), entry.baseItem(), 900, 50L, 2).traceValue() == 100, "Conversion cap still wins " + entry.id());
        }
        for (String bad : Arrays.asList(null, "", "Recovered Spyglass", "recovered_spyglass",
                "2|dungeon_loot|recovered_spyglass|minecraft:spyglass",
                "1|crafted|recovered_spyglass|minecraft:spyglass",
                "1|vendor_retail|recovered_spyglass|minecraft:spyglass",
                "1|dungeon_loot|recovered_spyglass|minecraft:stick",
                "1|dungeon_loot|unknown|minecraft:spyglass",
                "1|dungeon_loot|recovered_spyglass|minecraft:spyglass|extra",
                "1|dungeon_loot||other:diamond_sword", "x".repeat(193)))
            check(ItemProvenance.parse(bad).isEmpty(), "Malformed/unknown identity fails closed");
        String future = "2|dungeon_loot|recovered_spyglass|minecraft:spyglass";
        var futureJson = Codec.STRING.encodeStart(JsonOps.INSTANCE, future).getOrThrow();
        check(Codec.STRING.parse(JsonOps.INSTANCE, futureJson).getOrThrow().equals(future), "Unknown version preserved without becoming eligible");
        for (String origin : List.of(ItemProvenance.LOOT, ItemProvenance.RETAIL)) {
            var generic = new ItemProvenance(origin, "", "minecraft:diamond_sword");
            check(ItemProvenance.parse(generic.encode()).orElseThrow().itemId().isEmpty(), "Generic proof cannot invent named identity");
        }
        check(!NamedLootPricing.quote("recovered_spyglass", "minecraft:spyglass", -1, null, 1).approved(), "Disabled named price is not zero surrender");
        var zero = NamedLootPricing.quote("recovered_spyglass", "minecraft:spyglass", 0, null, 1);
        check(zero.approved() && zero.traceValue() == 0, "Explicit zero price still needs existing surrender confirmation");
        check(!NamedLootPricing.quote("recovered_spyglass", "minecraft:spyglass", Long.MAX_VALUE, null, 2).approved(), "Overflow rejected");
        check(!NamedLootPricing.quote("recovered_spyglass", "minecraft:spyglass", 900, null, 0).approved(), "Empty stack rejected");
        both(facts(false,false,false,false,false,false,false,false), Rejection.NONE, "Ordinary non-equipment retains eligibility");
        both(facts(false,false,false,false,false,true,false,false), Rejection.UNCLASSIFIED_EQUIPMENT, "Unmarked equipment");
        both(facts(false,false,false,true,false,false,false,false), Rejection.INVALID_PROVENANCE, "Malformed data even on non-equipment");
        both(facts(false,false,false,true,false,true,false,false), Rejection.INVALID_PROVENANCE, "Wrong identity/type");
        both(facts(false,false,false,true,true,true,false,false), Rejection.NONE, "Trusted ordinary/named equipment");
        both(facts(true,false,false,true,true,true,false,false), Rejection.PROTECTED, "Provenance cannot remove binding or class restriction");
        both(facts(false,false,false,false,false,true,true,true), Rejection.NONE, "Valid marked weapon repair kit");
        both(facts(false,false,false,false,false,true,true,false), Rejection.INVALID_REPAIR_COMPONENT, "Forged repair marker cannot bypass provenance");
        both(facts(false,false,false,true,true,true,true,true), Rejection.INVALID_REPAIR_COMPONENT, "Repair/gear identity collision");
        var noSale = facts(false,false,true,true,true,true,false,false);
        check(evaluate(Action.TRADE, noSale) == Rejection.NONE, "No-sale alone does not prohibit trade");
        check(evaluate(Action.VENDOR_SALE, noSale) == Rejection.NO_SALE, "No-sale blocks vendor");
        var noTrade = facts(false,true,false,true,true,true,false,false);
        check(evaluate(Action.TRADE, noTrade) == Rejection.NO_TRADE, "No-trade blocks trade");
        check(evaluate(Action.VENDOR_SALE, noTrade) == Rejection.NONE, "No-trade alone does not prohibit vendor");
        both(new Facts(true,false,false,false,false,false,false,false,false), Rejection.EMPTY, "Empty item");
        // An offer can become restricted after insertion. The same final guard must reject it.
        check(evaluate(Action.TRADE, facts(false,false,false,true,true,true,false,false)) == Rejection.NONE
                && evaluate(Action.TRADE, facts(true,false,false,true,true,true,false,false)) == Rejection.PROTECTED,
                "Eligibility revoked after quote/offer is denied at final validation");
        System.out.println(checks + " D1 item identity/provenance/pricing checks passed");
    }
}
