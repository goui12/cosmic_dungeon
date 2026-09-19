package net.goui.cosmicdungeon.vendor;

import net.goui.cosmicdungeon.economy.CurrencyService;
import net.goui.cosmicdungeon.economy.pricing.VendorPrice;
import net.goui.cosmicdungeon.economy.pricing.VendorPricingService;
import net.goui.cosmicdungeon.network.VendorPayloads;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetPlayerInventoryPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public final class VendorService {
    private VendorService() {}

    public static VendorPayloads.S2C_OpenVendor buildOpenPayload(ServerPlayer sp, Entity vendor, VendorProfile profile) {
        Set<String> unlocked = new HashSet<>();
        List<VendorPayloads.S2C_OpenVendor.OfferView> offers = new ArrayList<>();
        for (VendorOffer offer : profile.buyOffers()) {
            if (VendorMenuState.isOfferUnlocked(sp, profile, offer)) unlocked.add(offer.id().toString());
            offers.add(new VendorPayloads.S2C_OpenVendor.OfferView(
                    offer.id().toString(),
                    offer.result().copy(),
                    offer.result().getHoverName().getString(),
                    offer.result().getCount(),
                    net.goui.cosmicdungeon.faction.NpcFactionService.retail(sp, net.goui.cosmicdungeon.config.VendorPricesConfig.retail(profile.id(), offer)),
                    "trace"
            ));
        }
        String pricingGroup = profile.buyback() != null && profile.buyback().pricingGroup() != null && !profile.buyback().pricingGroup().isBlank()
                ? profile.buyback().pricingGroup()
                : "default";
        return new VendorPayloads.S2C_OpenVendor(vendor.getId(), profile.id().toString(), vendorDisplayName(vendor, profile), profile.storeDisplayName(), CurrencyService.getBalanceTrace(sp), pricingGroup, List.copyOf(offers), List.copyOf(unlocked));
    }

    private static String vendorDisplayName(Entity vendor, VendorProfile profile) {
        Component customName = vendor.getCustomName();
        if (customName != null && !customName.getString().isBlank()) {
            return customName.getString();
        }
        return profile.displayName();
    }

    public static VendorPayloads.S2C_VendorPurchaseResult tryPurchase(ServerPlayer sp, int vendorEntityId, String offerIdRaw) {
        if (sp.containerMenu instanceof net.goui.cosmicdungeon.menu.VendorMenu menu) menu.takeSaleQuote();
        VendorContext context = validateVendor(sp, vendorEntityId, false);
        if (!context.ok()) return fail(sp, context.failMessage());
        VendorProfile profile = context.profile();

        Optional<VendorOffer> offerOpt = profile.buyOffers().stream().filter(o -> o.id().toString().equals(offerIdRaw)).findFirst();
        if (offerOpt.isEmpty()) return fail(sp, "Offer not found.");
        VendorOffer offer = offerOpt.get();

        if (!VendorMenuState.isOfferUnlocked(sp, profile, offer)) return fail(sp, "Offer locked.");

        if (VendorStock.exhausted(sp,profile,offer)) {
            return fail(sp, "Purchase limit reached.");
        }

        long traceCost = net.goui.cosmicdungeon.faction.NpcFactionService.retail(sp, net.goui.cosmicdungeon.config.VendorPricesConfig.retail(profile.id(), offer));
        if (traceCost < 0L) return fail(sp, "Invalid offer cost.");
        if (CurrencyService.getBalanceTrace(sp) < traceCost) return fail(sp, "Not enough Trace.");

        final ItemStack toGive;
        try { toGive = net.goui.cosmicdungeon.item.identity.ItemProvenanceService.vendorCopy(offer.result()); }
        catch (IllegalArgumentException invalid) { return fail(sp, "This offer is unavailable."); }
        boolean rawChop = toGive.is(net.goui.cosmicdungeon.item.ModItems.RAW_FARROWS_CHOP.get());
        if(rawChop && !net.goui.cosmicdungeon.dungeon.ChopOwnershipService.canBuy(sp))
            return fail(sp,"You already own a Farrow's Chop.");
        if(rawChop)net.goui.cosmicdungeon.dungeon.ChopOwnershipService.preparePurchase(sp,toGive);
        ItemStack issued = toGive.copy();
        if (!canFitPurchase(sp, toGive)) {
            return fail(sp, "Inventory full.");
        }

        var id=java.util.UUID.randomUUID();var outputs=new net.minecraft.nbt.ListTag();
        outputs.add(CommerceCustodyImages.lot(-1,net.goui.cosmicdungeon.playerclass.dragoon.repair.RepairCustody.encode(sp,toGive)));
        var details=new net.minecraft.nbt.CompoundTag();details.putString("profile",profile.id().toString());details.putString("offer",offer.id().toString());details.putLong("retail_trace",traceCost);
        VendorStock.describe(sp,profile,offer,details);if(rawChop)CommerceTransactions.describeChop(sp,details,issued,false);
        Component purchasedName=toGive.getHoverName();
        if(!CommerceTransactions.execute(sp,id,-traceCost,"vendor_retail",sp.level().getEntity(vendorEntityId).getUUID().toString(),
                CommerceTransactions.plan(sp,id,new net.minecraft.nbt.ListTag(),outputs,details)))return fail(sp,"Purchase did not complete. Any pending receipt is preserved for recovery.");

        long newBalance = CurrencyService.getBalanceTrace(sp);
        sp.sendSystemMessage(Component.literal("Purchased ").withStyle(ChatFormatting.GREEN)
                .append(Component.literal(purchasedName.getString()).withStyle(ChatFormatting.YELLOW))
                .append(Component.literal(" for ").withStyle(ChatFormatting.WHITE))
                .append(Component.literal(traceCost + " Trace").withStyle(ChatFormatting.AQUA))
                .append(Component.literal(".").withStyle(ChatFormatting.WHITE)));
        return new VendorPayloads.S2C_VendorPurchaseResult(true, "Purchase complete.", newBalance);
    }

    private static boolean canFitPurchase(ServerPlayer sp, ItemStack toGive) {
        if (toGive.isEmpty()) return false;

        int remaining = toGive.getCount();
        for (ItemStack existing : sp.getInventory().getNonEquipmentItems()) {
            if (existing.isEmpty()) {
                remaining -= sp.getInventory().getMaxStackSize(toGive);
            } else if (ItemStack.isSameItemSameComponents(existing, toGive) && existing.isStackable()) {
                remaining -= Math.max(0, sp.getInventory().getMaxStackSize(existing) - existing.getCount());
            }

            if (remaining <= 0) return true;
        }

        ItemStack offhand = sp.getInventory().getItem(Inventory.SLOT_OFFHAND);
        if (!offhand.isEmpty() && ItemStack.isSameItemSameComponents(offhand, toGive) && offhand.isStackable()) {
            remaining -= Math.max(0, sp.getInventory().getMaxStackSize(offhand) - offhand.getCount());
        }

        return remaining <= 0;
    }

    public static net.minecraft.network.protocol.common.custom.CustomPacketPayload trySellSelected(
            ServerPlayer sp, int vendorEntityId, List<Integer> slotIndexes) {
        return quoteSale(sp, vendorEntityId, slotIndexes, false);
    }

    public static net.minecraft.network.protocol.common.custom.CustomPacketPayload trySellAll(ServerPlayer sp, int vendorEntityId) {
        return quoteSale(sp, vendorEntityId, java.util.stream.IntStream.range(0, sp.getInventory().getContainerSize()).boxed().toList(), true);
    }

    private static net.minecraft.network.protocol.common.custom.CustomPacketPayload quoteSale(
            ServerPlayer sp, int vendorEntityId, List<Integer> slots, boolean all) {
        if (sp.containerMenu instanceof net.goui.cosmicdungeon.menu.VendorMenu menu) menu.takeSaleQuote();
        VendorContext context = validateVendor(sp, vendorEntityId, true);
        if (!context.ok()) return fail(sp, context.failMessage());
        if (slots == null || slots.isEmpty() || slots.size() > 41) return fail(sp, "Select items to offer.");
        List<VendorSaleQuote.Line<ItemStack>> lines = new ArrayList<>();
        Set<Integer> seen = new HashSet<>();
        for (Integer slot : slots) {
            if (slot == null || slot < 0 || slot >= sp.getInventory().getContainerSize() || !seen.add(slot))
                return fail(sp, "Invalid item selection.");
            ItemStack stack = sp.getInventory().getItem(slot);
            VendorPrice price = VendorPricingService.getSellValue(stack, context.vendorType());
            boolean owned = !stack.is(net.goui.cosmicdungeon.item.ModItems.RAW_FARROWS_CHOP.get())
                    || net.goui.cosmicdungeon.dungeon.ChopOwnershipService.owned(sp, stack);
            if (stack.isEmpty() || !owned || !price.approved() || price.traceValue() < 0) {
                if (all) continue;
                if (price.debugSource().equals("restricted:UNCLASSIFIED_EQUIPMENT"))
                    return fail(sp, "This equipment has not been approved for sale.");
                return fail(sp, "An item is unavailable, restricted, or has no approved price.");
            }
            // Bulk sales never silently include zero-value surrender.
            if (all && price.traceValue() == 0) continue;
            lines.add(new VendorSaleQuote.Line<>(slot, stack, price.traceValue(), price.breakdown(), ItemStack::copy, ItemStack::matches));
        }
        if (lines.isEmpty()) return fail(sp, "No approved items selected.");
        final VendorSaleQuote<ItemStack> quote;
        try {
            quote = new VendorSaleQuote<>(vendorEntityId, sp.level().getGameTime(),
                    net.goui.cosmicdungeon.economy.D1EconomyConfig.VENDOR_QUOTE_TICKS.get(), lines);
        } catch (ArithmeticException | IllegalArgumentException invalid) {
            return fail(sp, "Sale value or selection is invalid.");
        }
        if (!CurrencyService.canDeposit(sp, quote.total())) return fail(sp, "You do not have enough currency capacity.");
        ((net.goui.cosmicdungeon.menu.VendorMenu) sp.containerMenu).setSaleQuote(quote);
        return new VendorPayloads.S2C_VendorSaleQuote(sp.containerMenu.containerId, quote.token(), quote.total(),
                quote.lines().stream().map(line -> new VendorPayloads.S2C_VendorSaleQuote.Line(line.stack(), line.breakdown())).toList());
    }

    public static VendorPayloads.S2C_VendorPurchaseResult confirmSale(ServerPlayer sp, int containerId, String token, boolean confirm) {
        if (!(sp.containerMenu instanceof net.goui.cosmicdungeon.menu.VendorMenu menu) || menu.containerId != containerId)
            return fail(sp, "Vendor session changed. Request a new quote.");
        VendorSaleQuote<ItemStack> quote = menu.takeSaleQuote();
        if (quote == null || !quote.consume(token, sp.level().getGameTime()))
            return fail(sp, "Sale quote expired or changed. Request a new quote.");
        if (!confirm) return new VendorPayloads.S2C_VendorPurchaseResult(false, "Sale cancelled.", CurrencyService.getBalanceTrace(sp));
        VendorContext context = validateVendor(sp, quote.vendorEntityId(), true);
        if (!context.ok()) return fail(sp, context.failMessage());
        for (VendorSaleQuote.Line<ItemStack> line : quote.lines()) {
            ItemStack current = sp.getInventory().getItem(line.slot());
            VendorPrice price = VendorPricingService.getSellValue(current, context.vendorType());
            if (!price.approved() || !line.matches(current, price))
                return fail(sp, "An item or price changed. Request a new quote.");
            if (current.is(net.goui.cosmicdungeon.item.ModItems.RAW_FARROWS_CHOP.get())
                    && !net.goui.cosmicdungeon.dungeon.ChopOwnershipService.owned(sp, current))
                return fail(sp, "Only the owner can sell this Raw Chop.");
        }
        return commitSale(sp, quote, quote.total(), quote.token(),
                sp.level().getEntity(quote.vendorEntityId()).getUUID().toString(), "Sale complete.");
    }

    private static VendorPayloads.S2C_VendorPurchaseResult commitSale(ServerPlayer sp, VendorSaleQuote<ItemStack> quote, long payout, String quoteId, String vendorId, String resultMessage) {
        if (!CurrencyService.canDeposit(sp, payout)) return fail(sp, "You do not have enough currency capacity.");

        var inputs=new net.minecraft.nbt.ListTag();var details=new net.minecraft.nbt.CompoundTag();var prices=new net.minecraft.nbt.ListTag();
        var slotsToSell=quote.lines().stream().map(VendorSaleQuote.Line::slot).toList();
        for(var line:quote.lines()){
            var stack=line.stack();inputs.add(CommerceCustodyImages.lot(line.slot(),net.goui.cosmicdungeon.playerclass.dragoon.repair.RepairCustody.encode(sp,stack)));
            var price=new net.minecraft.nbt.CompoundTag();price.putInt("slot",line.slot());price.putLong("base",line.breakdown().base());price.putLong("enchantments",line.breakdown().enchantments());price.putLong("curses",line.breakdown().curses());price.putLong("adjustments",line.breakdown().adjustments());price.putLong("total",line.trace());prices.add(price);
            if(stack.is(net.goui.cosmicdungeon.item.ModItems.RAW_FARROWS_CHOP.get()))CommerceTransactions.describeChop(sp,details,stack,true);
        }
        details.put("prices",prices);var id=java.util.UUID.fromString(quoteId);
        if(!CommerceTransactions.execute(sp,id,payout,"vendor_sale",vendorId,CommerceTransactions.plan(sp,id,inputs,new net.minecraft.nbt.ListTag(),details)))
            return fail(sp,"Sale did not complete. Any pending receipt is preserved for recovery.");
        for(int slot:slotsToSell)sp.connection.send(new ClientboundSetPlayerInventoryPacket(slot,sp.getInventory().getItem(slot)));

        sp.sendSystemMessage(Component.literal("Sold ").withStyle(ChatFormatting.GREEN)
                .append(Component.literal(slotsToSell.size() + " stack" + (slotsToSell.size() == 1 ? "" : "s")).withStyle(ChatFormatting.YELLOW))
                .append(Component.literal(" for ").withStyle(ChatFormatting.WHITE))
                .append(Component.literal(payout + " Trace").withStyle(ChatFormatting.AQUA))
                .append(Component.literal(".").withStyle(ChatFormatting.WHITE)));
        return new VendorPayloads.S2C_VendorPurchaseResult(true, resultMessage, CurrencyService.getBalanceTrace(sp));
    }

    private static VendorContext validateVendor(ServerPlayer sp, int vendorEntityId, boolean requireBuyback) {
        if (!sp.isAlive() || sp.isSpectator()) return VendorContext.fail("Cannot trade right now.");
        if (!CurrencyService.transactionsAllowed(sp)) return VendorContext.fail("Wait for the dungeon reset to finish.");
        if (!(sp.containerMenu instanceof net.goui.cosmicdungeon.menu.VendorMenu))
            return VendorContext.fail("Open the vendor interface first.");
        Entity vendor = sp.level().getEntity(vendorEntityId);
        if (vendor == null || !vendor.isAlive() ||!((net.goui.cosmicdungeon.menu.VendorMenu)sp.containerMenu).matches(vendor)) {
            return VendorContext.fail("Vendor no longer exists.");
        }
        if (sp.distanceToSqr(vendor) > 64.0D) return VendorContext.fail("Too far from vendor.");

        if (VendorAssignmentService.hasOtherRole(vendor)) return VendorContext.fail("NPC binding needs developer review.");
        ResourceLocation profileId = VendorAssignmentService.getProfileId(vendor);
        if (profileId == null) return VendorContext.fail("Vendor is not assigned.");

        VendorProfile profile = VendorProfileManager.INSTANCE.get(profileId);
        if (profile == null) return VendorContext.fail("Vendor profile missing.");

        VendorMenuState.UnlockResult vendorUnlocked = VendorMenuState.unlockState(sp, profile);
        if (!vendorUnlocked.unlocked()) return VendorContext.fail("Vendor locked: " + vendorUnlocked.reason());
        if (requireBuyback && profile.buyback() == null) return VendorContext.fail("This vendor will not buy that.");

        return VendorContext.ok(profile);
    }

    private static VendorPayloads.S2C_VendorPurchaseResult fail(ServerPlayer sp, String msg) {
        sp.sendSystemMessage(Component.literal(msg).withStyle(ChatFormatting.RED));
        return new VendorPayloads.S2C_VendorPurchaseResult(false, msg, CurrencyService.getBalanceTrace(sp));
    }

    private record VendorContext(boolean ok, String failMessage, VendorProfile profile) {
        static VendorContext ok(VendorProfile profile) { return new VendorContext(true, "", profile); }
        static VendorContext fail(String message) { return new VendorContext(false, message, null); }
        String vendorType() {
            if (profile == null || profile.buyback() == null || profile.buyback().pricingGroup() == null || profile.buyback().pricingGroup().isBlank()) return "default";
            return profile.buyback().pricingGroup();
        }
    }
}
