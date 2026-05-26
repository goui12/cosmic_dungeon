package net.goui.cosmicdungeon.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.goui.cosmicdungeon.auth.AccessPolicy;
import net.goui.cosmicdungeon.vendor.VendorOffer;
import net.goui.cosmicdungeon.vendor.VendorProfile;
import net.goui.cosmicdungeon.vendor.VendorProfileManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public final class VendorCommand {
    private VendorCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("vendor")
                .then(Commands.literal("list")
                        .requires(AccessPolicy::requireDeveloperOrConsole)
                        .executes(ctx -> list(ctx.getSource())))
                .then(Commands.literal("reload")
                        .requires(AccessPolicy::requireDeveloperOrConsole)
                        .executes(ctx -> reload(ctx.getSource())))
                .then(Commands.literal("profile")
                        .requires(AccessPolicy::requireDeveloperOrConsole)
                        .then(Commands.argument("profileId", StringArgumentType.word())
                                .suggests((ctx, b) -> SharedSuggestionProvider.suggest(
                                        VendorProfileManager.INSTANCE.listProfileIds().stream().map(ResourceLocation::toString), b))
                                .executes(ctx -> profile(ctx.getSource(), StringArgumentType.getString(ctx, "profileId")))))
                .executes(ctx -> usage(ctx.getSource())));
    }

    private static int usage(CommandSourceStack src) {
        src.sendFailure(Component.literal("Usage: /vendor list|reload|profile <profileId>"));
        return 0;
    }

    private static int list(CommandSourceStack src) {
        var ids = VendorProfileManager.INSTANCE.listProfileIds();
        src.sendSuccess(() -> Component.literal("Loaded vendor profiles: " + ids.size()), false);
        for (ResourceLocation id : ids) {
            src.sendSuccess(() -> Component.literal(" - " + id), false);
        }
        return 1;
    }

    private static int reload(CommandSourceStack src) {
        if (src.getServer() == null) {
            src.sendFailure(Component.literal("Server unavailable."));
            return 0;
        }
        VendorProfileManager.INSTANCE.reloadNow(src.getServer().getResourceManager());
        src.sendSuccess(() -> Component.literal("Vendor profiles reloaded."), true);
        return 1;
    }

    private static int profile(CommandSourceStack src, String profileIdRaw) {
        ResourceLocation id = ResourceLocation.tryParse(profileIdRaw);
        if (id == null) {
            src.sendFailure(Component.literal("Invalid profile id: " + profileIdRaw));
            return 0;
        }
        VendorProfile profile = VendorProfileManager.INSTANCE.get(id);
        if (profile == null) {
            src.sendFailure(Component.literal("Unknown profile: " + id));
            return 0;
        }
        src.sendSuccess(() -> Component.literal("Profile " + profile.id() + " (" + profile.displayName() + ")"), false);
        src.sendSuccess(() -> Component.literal(" type=" + profile.vendorType()
                + ", requiredFaction=" + profile.requiredFactionId()
                + ", requiredFactionTier=" + profile.requiredFactionTier()
                + ", requiredFlag=" + profile.requiredProgressionFlag()
                + ", requiredNpcTier=" + profile.requiredNpcTier()), false);
        for (VendorOffer offer : profile.buyOffers()) {
            src.sendSuccess(() -> Component.literal(" offer " + offer.id() + " -> "
                    + offer.result().getCount() + "x " + offer.result().getItemHolder().unwrapKey().map(k -> k.location().toString()).orElse("unknown")
                    + " cost=" + offer.cost().amount() + " " + offer.cost().denomination().id()), false);
        }
        return 1;
    }
}
