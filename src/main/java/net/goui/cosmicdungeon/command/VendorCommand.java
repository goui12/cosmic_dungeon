package net.goui.cosmicdungeon.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.goui.cosmicdungeon.auth.AccessPolicy;
import net.goui.cosmicdungeon.vendor.VendorAssignmentService;
import net.goui.cosmicdungeon.vendor.VendorAccessService;
import net.goui.cosmicdungeon.vendor.VendorOffer;
import net.goui.cosmicdungeon.vendor.VendorProfile;
import net.goui.cosmicdungeon.vendor.VendorProfileManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class VendorCommand {
    private VendorCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("vendor")
                .then(Commands.literal("list").requires(AccessPolicy::requireDeveloperOrConsole).executes(ctx -> list(ctx.getSource())))
                .then(Commands.literal("reload").requires(AccessPolicy::requireDeveloperOrConsole).executes(ctx -> reload(ctx.getSource())))
                .then(Commands.literal("profile")
                        .requires(AccessPolicy::requireDeveloperOrConsole)
                        .then(Commands.argument("profileId", StringArgumentType.word())
                                .suggests((ctx, b) -> SharedSuggestionProvider.suggest(
                                        VendorProfileManager.INSTANCE.listProfileIds().stream().map(ResourceLocation::toString), b))
                                .executes(ctx -> profile(ctx.getSource(), StringArgumentType.getString(ctx, "profileId")))))
                .then(Commands.literal("assign")
                        .requires(AccessPolicy::requireDeveloperOrConsole)
                        .then(Commands.argument("profileId", StringArgumentType.word())
                                .suggests((ctx, b) -> SharedSuggestionProvider.suggest(
                                        VendorProfileManager.INSTANCE.listProfileIds().stream().map(ResourceLocation::toString), b))
                                .executes(ctx -> assign(ctx.getSource(), StringArgumentType.getString(ctx, "profileId")))))
                .then(Commands.literal("clear").requires(AccessPolicy::requireDeveloperOrConsole).executes(ctx -> clear(ctx.getSource())))
                .then(Commands.literal("info").requires(AccessPolicy::requireDeveloperOrConsole).executes(ctx -> info(ctx.getSource())))
                .then(Commands.literal("spawn")
                        .requires(AccessPolicy::requireDeveloperOrConsole)
                        .then(Commands.argument("profileId", StringArgumentType.word())
                                .suggests((ctx, b) -> SharedSuggestionProvider.suggest(
                                        VendorProfileManager.INSTANCE.listProfileIds().stream().map(ResourceLocation::toString), b))
                                .executes(ctx -> spawn(ctx.getSource(), StringArgumentType.getString(ctx, "profileId")))))
                .then(Commands.literal("access")
                        .requires(AccessPolicy::requireDeveloperOrConsole)
                        .then(Commands.argument("profileId", StringArgumentType.word())
                                .suggests((ctx, b) -> SharedSuggestionProvider.suggest(
                                        VendorProfileManager.INSTANCE.listProfileIds().stream().map(ResourceLocation::toString), b))
                                .executes(ctx -> access(ctx.getSource(), StringArgumentType.getString(ctx, "profileId")))))
                .executes(ctx -> usage(ctx.getSource())));
    }

    private static int usage(CommandSourceStack src) { src.sendFailure(Component.literal("Usage: /vendor list|reload|profile <profileId>|assign <profileId>|clear|info|spawn <profileId>|access <profileId>")); return 0; }
    private static int list(CommandSourceStack src) { var ids = VendorProfileManager.INSTANCE.listProfileIds(); src.sendSuccess(() -> Component.literal("Loaded vendor profiles: " + ids.size()), false); for (ResourceLocation id : ids) src.sendSuccess(() -> Component.literal(" - " + id), false); return 1; }
    private static int reload(CommandSourceStack src) { if (src.getServer() == null) { src.sendFailure(Component.literal("Server unavailable.")); return 0; } VendorProfileManager.INSTANCE.reloadNow(src.getServer().getResourceManager()); src.sendSuccess(() -> Component.literal("Vendor profiles reloaded."), true); return 1; }

    private static int profile(CommandSourceStack src, String profileIdRaw) {
        ResourceLocation id = ResourceLocation.tryParse(profileIdRaw); if (id == null) { src.sendFailure(Component.literal("Invalid profile id: " + profileIdRaw)); return 0; }
        VendorProfile profile = VendorProfileManager.INSTANCE.get(id); if (profile == null) { src.sendFailure(Component.literal("Unknown profile: " + id)); return 0; }
        src.sendSuccess(() -> Component.literal("Profile " + profile.id() + " (" + profile.displayName() + ")"), false);
        src.sendSuccess(() -> Component.literal(" type=" + profile.vendorType() + ", requiredVillageAccess=" + profile.requiredVillageAccess() + ", requiredNpcSystem=" + profile.requiredNpcSystem() + ", requiredNpcTier=" + profile.requiredNpcTier() + ", requiredFaction=" + profile.requiredFactionId() + ", requiredFactionTier=" + profile.requiredFactionTier()), false);
        for (VendorOffer offer : profile.buyOffers()) src.sendSuccess(() -> Component.literal(" offer " + offer.id() + " -> " + offer.result().getCount() + "x " + offer.result().getItemHolder().unwrapKey().map(k -> k.location().toString()).orElse("unknown") + " cost=" + offer.cost().amount() + " " + offer.cost().denomination().id()), false);
        return 1;
    }

    private static int assign(CommandSourceStack src, String profileIdRaw) {
        ServerPlayer sp = src.getPlayer(); if (sp == null) { src.sendFailure(Component.literal("Player context required.")); return 0; }
        ResourceLocation id = ResourceLocation.tryParse(profileIdRaw); if (id == null || VendorProfileManager.INSTANCE.get(id) == null) { src.sendFailure(Component.literal("Unknown profile: " + profileIdRaw)); return 0; }
        Villager villager = lookedVillager(sp); if (villager == null) { src.sendFailure(Component.literal("Look at a villager within 6 blocks.")); return 0; }
        if (!VendorAssignmentService.assignProfile(villager, id)) { src.sendFailure(Component.literal("Failed assigning vendor profile.")); return 0; }
        src.sendSuccess(() -> Component.literal("Assigned " + id + " to villager " + villager.getUUID()), true); return 1;
    }

    private static int clear(CommandSourceStack src) {
        ServerPlayer sp = src.getPlayer(); if (sp == null) { src.sendFailure(Component.literal("Player context required.")); return 0; }
        Villager villager = lookedVillager(sp); if (villager == null) { src.sendFailure(Component.literal("Look at a villager within 6 blocks.")); return 0; }
        ResourceLocation old = VendorAssignmentService.getProfileId(villager); if (old == null) { src.sendFailure(Component.literal("Villager has no vendor profile.")); return 0; }
        VendorAssignmentService.clearProfile(villager); src.sendSuccess(() -> Component.literal("Cleared vendor profile " + old), true); return 1;
    }

    private static int info(CommandSourceStack src) {
        ServerPlayer sp = src.getPlayer(); if (sp == null) { src.sendFailure(Component.literal("Player context required.")); return 0; }
        Villager villager = lookedVillager(sp); if (villager == null) { src.sendFailure(Component.literal("Look at a villager within 6 blocks.")); return 0; }
        ResourceLocation id = VendorAssignmentService.getProfileId(villager); if (id == null) { src.sendSuccess(() -> Component.literal("Villager is not an assigned vendor shell."), false); return 1; }
        src.sendSuccess(() -> Component.literal("Villager vendor profile: " + id), false); return 1;
    }

    private static int spawn(CommandSourceStack src, String profileIdRaw) {
        ServerPlayer sp = src.getPlayer(); if (sp == null) { src.sendFailure(Component.literal("Player context required.")); return 0; }
        ResourceLocation id = ResourceLocation.tryParse(profileIdRaw); if (id == null || VendorProfileManager.INSTANCE.get(id) == null) { src.sendFailure(Component.literal("Unknown profile: " + profileIdRaw)); return 0; }
        Villager villager = EntityType.VILLAGER.create(sp.level(), EntitySpawnReason.COMMAND); if (villager == null) { src.sendFailure(Component.literal("Could not create villager.")); return 0; }
        villager.snapTo(sp.getX(), sp.getY(), sp.getZ(), sp.getYRot(), sp.getXRot());
        if (!sp.level().addFreshEntity(villager)) { src.sendFailure(Component.literal("Could not spawn villager entity.")); return 0; }
        if (!VendorAssignmentService.assignProfile(villager, id)) {
            villager.discard();
            src.sendFailure(Component.literal("Failed assigning vendor profile."));
            return 0;
        }
        src.sendSuccess(() -> Component.literal("Spawned vendor villager with profile " + id), true); return 1;
    }

    private static int access(CommandSourceStack src, String profileIdRaw) {
        ServerPlayer sp = src.getPlayer(); if (sp == null) { src.sendFailure(Component.literal("Player context required.")); return 0; }
        ResourceLocation id = ResourceLocation.tryParse(profileIdRaw); if (id == null) { src.sendFailure(Component.literal("Invalid profile id: " + profileIdRaw)); return 0; }
        VendorProfile profile = VendorProfileManager.INSTANCE.get(id); if (profile == null) { src.sendFailure(Component.literal("Unknown profile: " + id)); return 0; }
        VendorAccessService.AccessResult result = VendorAccessService.evaluate(sp, profile);
        src.sendSuccess(() -> Component.literal("Vendor access " + (result.allowed() ? "ALLOWED" : "DENIED") + " for " + id + ": " + result.message()), false);
        return result.allowed() ? 1 : 0;
    }

    private static Villager lookedVillager(ServerPlayer sp) {
        Vec3 eye = sp.getEyePosition();
        Vec3 end = eye.add(sp.getLookAngle().scale(6.0D));
        AABB box = new AABB(eye, end).inflate(1.5D);
        Villager best = null;
        double bestDist = Double.MAX_VALUE;
        for (Villager v : sp.level().getEntitiesOfClass(Villager.class, box, e -> e.isAlive())) {
            double d = v.distanceToSqr(sp);
            if (d < bestDist) { bestDist = d; best = v; }
        }
        return best;
    }
}
