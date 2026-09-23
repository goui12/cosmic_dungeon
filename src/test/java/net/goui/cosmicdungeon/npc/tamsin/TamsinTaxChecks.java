package net.goui.cosmicdungeon.npc.tamsin;

import net.goui.cosmicdungeon.item.identity.D1LootCatalog;
import net.goui.cosmicdungeon.network.TamsinTaxPayloads;
import net.goui.cosmicdungeon.playerclass.api.ClassData;
import net.goui.cosmicdungeon.transaction.SingleItemCommit;
import net.minecraft.nbt.CompoundTag;
import io.netty.buffer.Unpooled;
import java.io.IOException;
import java.util.*;

public final class TamsinTaxChecks {
    private static int checks;
    private static void check(boolean ok, String message) {
        checks++; if (!ok) throw new AssertionError(message);
    }
    public static void main(String[] args) {
        check(TamsinTaxRules.ELIGIBLE.size() == 23, "Tax source explicitly lists 23 identities");
        for (String id : TamsinTaxRules.ELIGIBLE) check(D1LootCatalog.find(id) != null, "Approved identity exists: " + id);
        check(!TamsinTaxRules.ELIGIBLE.contains("future_dungeon_drop"), "Future catalogue additions do not qualify");
        check(!TamsinTaxRules.qualifies(0, 4, false), "Completion without personal camp cannot qualify");
        check(!TamsinTaxRules.qualifies(4, 0, false), "Camp without success cannot qualify");
        check(!TamsinTaxRules.qualifies(4, 3, false), "An earlier success does not qualify");
        check(TamsinTaxRules.qualifies(4, 4, false), "Camp and success in same run qualify");
        check(TamsinTaxRules.qualifies(4, 9, false), "Discovery survives failed runs");
        check(!TamsinTaxRules.qualifies(4, 9, true), "Any receipt, including unknown schema, prevents another charge");
        check(!TamsinTaxRules.qualifies(-1, 9, false), "Invalid migrated proof cannot qualify");
        check(TamsinTaxRules.confirmation(true,true,"token","token",99,100,true), "Live exact selection");
        check(!TamsinTaxRules.confirmation(false,true,"token","token",99,100,true), "Wrong NPC/menu/range");
        check(!TamsinTaxRules.confirmation(true,false,"token","token",99,100,true), "Eligibility rechecked");
        check(!TamsinTaxRules.confirmation(true,true,"token","other",99,100,true), "Forged/replayed quote");
        check(!TamsinTaxRules.confirmation(true,true,"","",99,100,true), "No pending quote");
        check(!TamsinTaxRules.confirmation(true,true,"token","token",100,100,true), "Exact expiry boundary");
        check(!TamsinTaxRules.confirmation(true,true,"token","token",99,100,false), "Changed count/components/slot");
        UUID owner = UUID.randomUUID();
        var receipt = receipt(owner);
        check(TamsinTaxReceipt.valid(receipt, owner), "Owned schema-one receipt");
        check(!TamsinTaxReceipt.valid(receipt, UUID.randomUUID()), "Copied receipt cannot redeem for another UUID");
        var corrupt = receipt.copy(); corrupt.putInt("schema", 2);
        check(!TamsinTaxReceipt.valid(corrupt, owner), "Unknown schema is not silently applied");
        corrupt = receipt.copy(); corrupt.remove("after");
        check(!TamsinTaxReceipt.valid(corrupt, owner), "Missing post-image rejected");
        corrupt = receipt.copy(); corrupt.putString("transaction", "bad");
        check(!TamsinTaxReceipt.valid(corrupt, owner), "Malformed transaction rejected");
        corrupt = receipt.copy(); corrupt.getCompoundOrEmpty("before").putInt("count", 2);
        check(!TamsinTaxReceipt.valid(corrupt, owner), "Missing remainder cannot redeem a two-item stack");
        corrupt = receipt.copy(); corrupt.getCompoundOrEmpty("before").putString("id","minecraft:paper");
        check(!TamsinTaxReceipt.valid(corrupt, owner), "Mismatched receipt item image rejected");
        var stackReceipt = receipt.copy();
        stackReceipt.getCompoundOrEmpty("before").putInt("count", 2);
        var remainder = stackReceipt.getCompoundOrEmpty("before").copy(); remainder.putInt("count", 1);
        stackReceipt.put("after", remainder);
        check(TamsinTaxReceipt.valid(stackReceipt, owner), "Exactly one removed from stacked items");
        stackReceipt.getCompoundOrEmpty("after").putString("extra_component", "changed");
        check(!TamsinTaxReceipt.valid(stackReceipt, owner), "Other item components cannot change in the receipt");
        var snapshot = snapshot(receipt, 1);
        check(TamsinTaxReceipt.savedSnapshotMatches(snapshot, snapshot.copy()), "Receipt and inventory saved together");
        var wrongInventory = snapshot.copy(); wrongInventory.putInt("Inventory", 2);
        check(!TamsinTaxReceipt.savedSnapshotMatches(snapshot, wrongInventory), "Old inventory readback rejected");
        var wrongEquipment = snapshot.copy(); wrongEquipment.putInt("equipment", 5);
        check(!TamsinTaxReceipt.savedSnapshotMatches(snapshot, wrongEquipment), "Armor/offhand readback included");
        check(!TamsinTaxReceipt.savedSnapshotMatches(snapshot, new CompoundTag()), "Missing saved receipt rejected");
        var otherReceipt = receipt.copy(); otherReceipt.putString("transaction", UUID.randomUUID().toString());
        check(!TamsinTaxReceipt.savedSnapshotMatches(snapshot, snapshot(otherReceipt, 1)), "Another transaction cannot acknowledge this write");
        crashMatrix();
        var bytes = Unpooled.buffer();
        var action = new TamsinTaxPayloads.Action(7, "confirm", 40, UUID.randomUUID().toString());
        TamsinTaxPayloads.Action.STREAM_CODEC.encode(bytes, action);
        check(action.equals(TamsinTaxPayloads.Action.STREAM_CODEC.decode(bytes)), "Bounded action packet round trip");
        bytes.clear();
        var choices = new ArrayList<TamsinTaxPayloads.Choice>();
        for (int i = 0; i < 43; i++) choices.add(new TamsinTaxPayloads.Choice(i, "Recovered Spyglass", 64));
        var view = new TamsinTaxPayloads.View(7, choices, action.token(), "Recovered Spyglass");
        TamsinTaxPayloads.View.STREAM_CODEC.encode(bytes, view);
        check(view.equals(TamsinTaxPayloads.View.STREAM_CODEC.decode(bytes)), "All carried slots survive view packet");
        bytes.release();
        System.out.println(checks + " Tamsin Tax checks passed");
    }
    private static CompoundTag receipt(UUID owner) {
        var tag = new CompoundTag();
        tag.putInt("schema",1); tag.putString("owner",owner.toString()); tag.putString("transaction",UUID.randomUUID().toString());
        tag.putString("item","recovered_spyglass"); tag.putInt("quantity",1);
        var before = new CompoundTag(); before.putString("id","minecraft:spyglass"); before.putInt("count",1);
        tag.put("before",before); tag.put("after",new CompoundTag());
        return tag;
    }
    private static CompoundTag snapshot(CompoundTag receipt, int inventory) {
        var tax = new CompoundTag(); tax.put("receipt",receipt);
        var root = new CompoundTag(); root.put(TamsinTaxProgress.KEY,tax);
        var neo = new CompoundTag(); neo.put(ClassData.ROOT_TAG,root);
        var player = new CompoundTag(); player.put("NeoForgeData",neo);
        player.putInt("Inventory",inventory); player.putInt("equipment",3);
        return player;
    }
    private record Disk(int items, boolean receipt) {}
    private static final class PowerLoss extends Error {}
    private static void crashMatrix() {
        // Independent model of a player's authoritative snapshot, starting with two identical items.
        for (int stage = 0; stage < 5; stage++) {
            final int fail = stage;
            Disk[] disk = {new Disk(2, false)};
            boolean[] awarded = {false};
            try {
                var result = SingleItemCommit.finish(() -> {
                    if (fail == 0) throw new IOException("Before write");
                    disk[0] = new Disk(1, true);
                    if (fail == 1) throw new IOException("Readback failed after write");
                    if (fail == 2) return false;
                    return true;
                }, () -> { if (fail == 3) throw new PowerLoss(); awarded[0] = true; });
                check((result == SingleItemCommit.Result.COMMITTED) == (fail == 4), "Only verified write reports success " + stage);
            } catch (PowerLoss expected) { check(stage == 3, "Crash after commit before achievement"); }
            if (stage < 4) check(!awarded[0], "No premature achievement " + stage);
            // Login trusts the loaded same-file receipt; projection never changes item count.
            for (int login = 0; login < 3; login++) {
                if (disk[0].receipt()) awarded[0] = true;
                check(disk[0].items() == (stage == 0 ? 2 : 1), "Repeated recovery never consumes/refunds " + stage);
            }
            check(awarded[0] == (stage > 0), "Old snapshot retries; new snapshot recovers award " + stage);
        }
        // Integrated host authority is level.dat; playerdata alone cannot acknowledge payment.
        var oldHost = new Disk(2, false); var newPlayerdata = new Disk(1, true);
        boolean verified = oldHost.receipt() && newPlayerdata.receipt();
        check(!verified, "Integrated owner needs both persisted copies");
        check(oldHost.items() == 2 && !oldHost.receipt(), "Crash before host save reloads the complete old transaction");
    }
}
