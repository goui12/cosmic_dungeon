package net.goui.cosmicdungeon.trade;

import java.util.UUID;
import net.goui.cosmicdungeon.component.ModDataComponents;
import net.goui.cosmicdungeon.item.identity.ClassItemOwnership;
import net.goui.cosmicdungeon.playerclass.dragoon.repair.RepairCustody;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.*;

/** Copy-only projection after the durable trade decision; never touch cancellation/cursor images. */
public final class TradeItemTransfer {
    private TradeItemTransfer() {}
    public static boolean mayOffer(ItemStack stack, UUID donor) {
        return net.goui.cosmicdungeon.economy.pricing.ItemTransferRules.tradeEligible(stack)
                && ownsContents(stack, donor, 0, new int[]{256});
    }
    private static boolean ownsContents(ItemStack stack, UUID donor, int depth, int[] budget) {
        if (stack.isEmpty()) return true;
        if (depth > 8 || --budget[0] < 0) return false;
        if (ClassItemOwnership.present(stack) && !donor.equals(ClassItemOwnership.owner(stack))) return false;
        var chopOwner = stack.get(ModDataComponents.CHOP_OWNER.get());
        if (chopOwner != null && !donor.equals(chopOwner)) return false;
        var bundle = stack.get(DataComponents.BUNDLE_CONTENTS);
        if (bundle != null) for (var child : bundle.items())
            if (!ownsContents(child, donor, depth + 1, budget)) return false;
        var container = stack.get(DataComponents.CONTAINER);
        if (container != null) for (var child : container.nonEmptyItems())
            if (!ownsContents(child, donor, depth + 1, budget)) return false;
        return true;
    }
    public static ItemStack forRecipient(ItemStack original, UUID donor, UUID recipient) {
        if (original.isEmpty()) return ItemStack.EMPTY;
        // Old saved committed offers still project even if current eligibility rules have changed,
        // but malformed/foreign ownership can never be rewritten into valid custody.
        if (!ownsContents(original, donor, 0, new int[]{256}))
            throw new IllegalArgumentException("Trade offer contains foreign or malformed ownership");
        return copy(original, recipient, 0, new int[]{256});
    }
    private static ItemStack copy(ItemStack original, UUID recipient, int depth, int[] budget) {
        if (original.isEmpty()) return ItemStack.EMPTY;
        if (depth > 8 || --budget[0] < 0) throw new IllegalArgumentException("Trade item nesting exceeds inspection budget");
        var stack = original.copy();
        if (ClassItemOwnership.present(stack) || ClassItemOwnership.attuned(stack))
            CustomData.update(DataComponents.CUSTOM_DATA, stack,
                    tag -> tag.putString(ClassItemOwnership.KEY, recipient.toString()));
        if (stack.has(ModDataComponents.CHOP_OWNER.get())) stack.set(ModDataComponents.CHOP_OWNER.get(), recipient);
        var target=stack.get(ModDataComponents.DUNGEON_RETURN_TARGET.get());
        if (stack.has(ModDataComponents.CHOP_OWNER.get()) && target!=null)
            stack.set(ModDataComponents.DUNGEON_RETURN_TARGET.get(),new net.goui.cosmicdungeon.dungeon.DungeonReturnTarget(
                    recipient,target.runId(),target.dimensionId(),target.x(),target.y(),target.z(),target.yaw(),target.pitch()));
        var bundle = stack.get(DataComponents.BUNDLE_CONTENTS);
        if (bundle != null) {
            var items = bundle.itemCopyStream().map(item -> copy(item, recipient, depth + 1, budget)).toList();
            var changed = new BundleContents.Mutable(new BundleContents(items));
            if (bundle.hasSelectedItem()) changed.toggleSelectedItem(bundle.getSelectedItem());
            stack.set(DataComponents.BUNDLE_CONTENTS, changed.toImmutable());
        }
        var container = stack.get(DataComponents.CONTAINER);
        if (container != null) stack.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(
                container.stream().map(item -> copy(item, recipient, depth + 1, budget)).toList()));
        return stack;
    }
    public static CompoundTag projectOffers(ServerPlayer player, CompoundTag projected) {
        TradeCustodyImages.validate(projected, player.getUUID());
        var offers = new ListTag();
        for (var image : (ListTag) projected.get("offers")) {
            var item = RepairCustody.decode(player, (CompoundTag) image);
            offers.add(RepairCustody.encode(player, forRecipient(item, UUID.fromString(projected.getStringOr("peer", "")), player.getUUID())));
        }
        return TradeCustodyImages.live(projected, offers, projected.getCompoundOrEmpty("cursor"));
    }
}
