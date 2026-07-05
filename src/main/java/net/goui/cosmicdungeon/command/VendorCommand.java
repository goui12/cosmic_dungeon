package net.goui.cosmicdungeon.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.goui.cosmicdungeon.auth.AccessPolicy;
import net.goui.cosmicdungeon.vendor.VendorAccessService;
import net.goui.cosmicdungeon.vendor.VendorAssignmentService;
import net.goui.cosmicdungeon.vendor.VendorOffer;
import net.goui.cosmicdungeon.vendor.VendorProfile;
import net.goui.cosmicdungeon.vendor.VendorProfileManager;
import net.goui.cosmicdungeon.vendor.VendorProfileResolver;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
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
                                .suggests((ctx, b) -> SharedSuggestionProvider.suggest(VendorProfileResolver.suggestions(), b))
                                .executes(ctx -> profile(ctx.getSource(), StringArgumentType.getString(ctx, "profileId")))))
                .then(Commands.literal("assign")
                        .requires(AccessPolicy::requireDeveloperOrConsole)
                        .then(Commands.argument("profileId", StringArgumentType.word())
                                .suggests((ctx, b) -> SharedSuggestionProvider.suggest(VendorProfileResolver.suggestions(), b))
                                .executes(ctx -> assign(ctx.getSource(), StringArgumentType.getString(ctx, "profileId")))))
                .then(Commands.literal("clear").requires(AccessPolicy::requireDeveloperOrConsole).executes(ctx -> clear(ctx.getSource())))
                .then(Commands.literal("info").requires(AccessPolicy::requireDeveloperOrConsole).executes(ctx -> info(ctx.getSource())))
                .then(Commands.literal("spawn")
                        .requires(AccessPolicy::requireDeveloperOrConsole)
                        .then(Commands.argument("profileId", StringArgumentType.word())
                                .suggests((ctx, b) -> SharedSuggestionProvider.suggest(VendorProfileResolver.suggestions(), b))
                                .executes(ctx -> spawn(ctx.getSource(), StringArgumentType.getString(ctx, "profileId"), "villager"))
                                .then(Commands.argument("mobType", StringArgumentType.word())
                                        .suggests((ctx, b) -> SharedSuggestionProvider.suggest(BuiltInRegistries.ENTITY_TYPE.keySet().stream()
                                                .filter(id -> BuiltInRegistries.ENTITY_TYPE.getValue(id) != EntityType.PLAYER)
                                                .map(ResourceLocation::toString), b))
                                        .executes(ctx -> spawn(ctx.getSource(), StringArgumentType.getString(ctx, "profileId"), StringArgumentType.getString(ctx, "mobType"))))))
                .then(Commands.literal("access")
                        .requires(AccessPolicy::requireDeveloperOrConsole)
                        .then(Commands.argument("profileId", StringArgumentType.word())
                                .suggests((ctx, b) -> SharedSuggestionProvider.suggest(VendorProfileResolver.suggestions(), b))
                                .executes(ctx -> access(ctx.getSource(), StringArgumentType.getString(ctx, "profileId")))))
                .executes(ctx -> usage(ctx.getSource())));
    }

    private static int usage(CommandSourceStack src) {
        src.sendFailure(Component.literal("Usage: ").withStyle(ChatFormatting.RED)
                .append(Component.literal("/vendor list|reload|profile <profileId>|assign <profileId>|clear|info|spawn <profileId>|access <profileId>").withStyle(ChatFormatting.YELLOW)));
        return 0;
    }

    private static int list(CommandSourceStack src) {
        var ids = VendorProfileManager.INSTANCE.listProfileIds();
        src.sendSuccess(() -> Component.literal("Loaded vendor profiles: ").withStyle(ChatFormatting.GREEN)
                .append(Component.literal(String.valueOf(ids.size())).withStyle(ChatFormatting.AQUA)), false);
        ids.stream().sorted(java.util.Comparator.comparing(ResourceLocation::toString)).forEach(id -> src.sendSuccess(() ->
                Component.literal(" - ").withStyle(ChatFormatting.DARK_GRAY)
                        .append(Component.literal(VendorProfileResolver.shortName(id)).withStyle(ChatFormatting.YELLOW))
                        .append(Component.literal("  (").withStyle(ChatFormatting.DARK_GRAY))
                        .append(idComponent(id))
                        .append(Component.literal(")").withStyle(ChatFormatting.DARK_GRAY)), false));
        return 1;
    }

    private static int reload(CommandSourceStack src) {
        if (src.getServer() == null) { fail(src, "Server unavailable."); return 0; }
        VendorProfileManager.INSTANCE.reloadNow(src.getServer().getResourceManager());
        src.sendSuccess(() -> Component.literal("Vendor profiles reloaded.").withStyle(ChatFormatting.GREEN), true);
        return 1;
    }

    private static int profile(CommandSourceStack src, String profileIdRaw) {
        ResourceLocation id = resolveOrFail(src, profileIdRaw);
        if (id == null) return 0;
        VendorProfile profile = VendorProfileManager.INSTANCE.get(id);
        src.sendSuccess(() -> Component.literal("Profile ").withStyle(ChatFormatting.WHITE)
                .append(Component.literal(profile.displayName()).withStyle(ChatFormatting.GOLD))
                .append(Component.literal(" ").withStyle(ChatFormatting.WHITE))
                .append(Component.literal("(").withStyle(ChatFormatting.DARK_GRAY))
                .append(idComponent(profile.id()))
                .append(Component.literal(")").withStyle(ChatFormatting.DARK_GRAY)), false);
        src.sendSuccess(() -> Component.literal(" type=").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(profile.vendorType()).withStyle(ChatFormatting.YELLOW))
                .append(Component.literal(", requiredVillageAccess=").withStyle(ChatFormatting.GRAY))
                .append(booleanComponent(profile.requiredVillageAccess()))
                .append(Component.literal(", requiredNpcSystem=").withStyle(ChatFormatting.GRAY))
                .append(secondary(String.valueOf(profile.requiredNpcSystem())))
                .append(Component.literal(", requiredNpcTier=").withStyle(ChatFormatting.GRAY))
                .append(secondary(String.valueOf(profile.requiredNpcTier())))
                .append(Component.literal(", requiredFaction=").withStyle(ChatFormatting.GRAY))
                .append(secondary(String.valueOf(profile.requiredFactionId())))
                .append(Component.literal(", requiredFactionTier=").withStyle(ChatFormatting.GRAY))
                .append(secondary(String.valueOf(profile.requiredFactionTier()))), false);
        for (VendorOffer offer : profile.buyOffers()) src.sendSuccess(() -> Component.literal(" offer ").withStyle(ChatFormatting.GRAY)
                .append(idComponent(offer.id()))
                .append(Component.literal(" -> ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(offer.result().getCount() + "x ").withStyle(ChatFormatting.AQUA))
                .append(Component.literal(offer.result().getItemHolder().unwrapKey().map(k -> k.location().toString()).orElse("unknown")).withStyle(ChatFormatting.YELLOW))
                .append(Component.literal(" cost=").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(offer.cost().amount() + " " + offer.cost().denomination().id()).withStyle(ChatFormatting.AQUA)), false);
        return 1;
    }

    private static int assign(CommandSourceStack src, String profileIdRaw) {
        ServerPlayer sp = src.getPlayer(); if (sp == null) { fail(src, "Player context required."); return 0; }
        ResourceLocation id = resolveOrFail(src, profileIdRaw); if (id == null) return 0;
        Mob villager = lookedMob(sp); if (villager == null) { fail(src, "Look at a mob within 6 blocks."); return 0; }
        if (!VendorAssignmentService.assignProfile(villager, id)) { fail(src, "Failed assigning vendor profile."); return 0; }
        src.sendSuccess(() -> Component.literal("Assigned ").withStyle(ChatFormatting.GREEN)
                .append(profileName(id))
                .append(Component.literal(" to mob ").withStyle(ChatFormatting.WHITE))
                .append(Component.literal(villager.getUUID().toString()).withStyle(ChatFormatting.DARK_GRAY)), true);
        return 1;
    }

    private static int clear(CommandSourceStack src) {
        ServerPlayer sp = src.getPlayer(); if (sp == null) { fail(src, "Player context required."); return 0; }
        Mob villager = lookedMob(sp); if (villager == null) { fail(src, "Look at a mob within 6 blocks."); return 0; }
        ResourceLocation old = VendorAssignmentService.getProfileId(villager); if (old == null) { fail(src, "Mob has no vendor profile."); return 0; }
        VendorAssignmentService.clearProfile(villager);
        src.sendSuccess(() -> Component.literal("Cleared vendor profile ").withStyle(ChatFormatting.GREEN).append(profileName(old)), true);
        return 1;
    }

    private static int info(CommandSourceStack src) {
        ServerPlayer sp = src.getPlayer(); if (sp == null) { fail(src, "Player context required."); return 0; }
        Mob villager = lookedMob(sp); if (villager == null) { fail(src, "Look at a mob within 6 blocks."); return 0; }
        ResourceLocation id = VendorAssignmentService.getProfileId(villager);
        if (id == null) { src.sendSuccess(() -> Component.literal("Mob is not an assigned vendor shell.").withStyle(ChatFormatting.GRAY), false); return 1; }
        src.sendSuccess(() -> Component.literal("Mob vendor profile: ").withStyle(ChatFormatting.WHITE).append(profileName(id)), false);
        return 1;
    }

    private static int spawn(CommandSourceStack src, String profileIdRaw, String mobTypeRaw) {
        ServerPlayer sp = src.getPlayer(); if (sp == null) { fail(src, "Player context required."); return 0; }
        ResourceLocation id = resolveOrFail(src, profileIdRaw); if (id == null) return 0;
        ResourceLocation mobTypeId = parseEntityTypeId(mobTypeRaw);
        EntityType<?> entityType = BuiltInRegistries.ENTITY_TYPE.getValue(mobTypeId);
        if (entityType == null) { fail(src, "Unknown mob type: " + mobTypeRaw); return 0; }
        Entity entity = entityType.create(sp.level(), EntitySpawnReason.COMMAND);
        if (!(entity instanceof Mob vendorMob)) { fail(src, "Entity type is not a mob vendor shell: " + mobTypeId); return 0; }
        vendorMob.snapTo(sp.getX(), sp.getY(), sp.getZ(), sp.getYRot(), sp.getXRot());
        if (!sp.level().addFreshEntity(vendorMob)) { fail(src, "Could not spawn vendor entity: " + mobTypeId); return 0; }
        if (!VendorAssignmentService.assignProfile(vendorMob, id)) {
            vendorMob.discard();
            fail(src, "Failed assigning vendor profile.");
            return 0;
        }
        src.sendSuccess(() -> Component.literal("Spawned vendor ").withStyle(ChatFormatting.GREEN)
                .append(Component.literal(mobTypeId.toString()).withStyle(ChatFormatting.AQUA))
                .append(Component.literal(" with profile ").withStyle(ChatFormatting.GREEN))
                .append(profileName(id)), true);
        return 1;
    }

    private static int access(CommandSourceStack src, String profileIdRaw) {
        ServerPlayer sp = src.getPlayer(); if (sp == null) { fail(src, "Player context required."); return 0; }
        ResourceLocation id = resolveOrFail(src, profileIdRaw); if (id == null) return 0;
        VendorProfile profile = VendorProfileManager.INSTANCE.get(id);
        VendorAccessService.AccessResult result = VendorAccessService.evaluate(sp, profile);
        src.sendSuccess(() -> Component.literal("Vendor access ").withStyle(ChatFormatting.WHITE)
                .append(Component.literal(result.allowed() ? "ALLOWED" : "DENIED").withStyle(result.allowed() ? ChatFormatting.GREEN : ChatFormatting.RED))
                .append(Component.literal(" for ").withStyle(ChatFormatting.WHITE))
                .append(profileName(id))
                .append(Component.literal(": ").withStyle(ChatFormatting.WHITE))
                .append(Component.literal(result.message()).withStyle(result.allowed() ? ChatFormatting.GREEN : ChatFormatting.RED)), false);
        return result.allowed() ? 1 : 0;
    }

    private static ResourceLocation resolveOrFail(CommandSourceStack src, String raw) {
        VendorProfileResolver.Result result = VendorProfileResolver.resolve(raw);
        switch (result.status()) {
            case RESOLVED -> { return result.id(); }
            case INVALID -> fail(src, "Invalid profile id: " + raw);
            case UNKNOWN -> fail(src, "Unknown profile: " + raw);
            case AMBIGUOUS -> src.sendFailure(Component.literal("Ambiguous vendor profile alias ").withStyle(ChatFormatting.RED)
                    .append(Component.literal(raw).withStyle(ChatFormatting.YELLOW))
                    .append(Component.literal(". Use a full id: ").withStyle(ChatFormatting.RED))
                    .append(Component.literal(result.matchList()).withStyle(ChatFormatting.GRAY)));
        }
        return null;
    }

    private static void fail(CommandSourceStack src, String message) {
        src.sendFailure(Component.literal(message).withStyle(ChatFormatting.RED));
    }

    private static MutableComponent profileName(ResourceLocation id) {
        return Component.literal(VendorProfileResolver.shortName(id)).withStyle(ChatFormatting.YELLOW)
                .append(Component.literal(" (").withStyle(ChatFormatting.DARK_GRAY))
                .append(idComponent(id))
                .append(Component.literal(")").withStyle(ChatFormatting.DARK_GRAY));
    }

    private static MutableComponent idComponent(ResourceLocation id) {
        return Component.literal(id.toString()).withStyle(ChatFormatting.GRAY);
    }

    private static MutableComponent secondary(String value) {
        return Component.literal(value).withStyle(ChatFormatting.GRAY);
    }

    private static MutableComponent booleanComponent(boolean value) {
        return Component.literal(String.valueOf(value)).withStyle(value ? ChatFormatting.GREEN : ChatFormatting.RED);
    }

    private static ResourceLocation parseEntityTypeId(String raw) {
        ResourceLocation direct = ResourceLocation.tryParse(raw);
        if (direct != null && raw.contains(":")) return direct;
        return ResourceLocation.fromNamespaceAndPath("minecraft", raw);
    }

    private static Mob lookedMob(ServerPlayer sp) {
        Vec3 eye = sp.getEyePosition();
        Vec3 end = eye.add(sp.getLookAngle().scale(6.0D));
        AABB box = new AABB(eye, end).inflate(1.5D);
        Mob best = null;
        double bestDist = Double.MAX_VALUE;
        for (Mob v : sp.level().getEntitiesOfClass(Mob.class, box, e -> e.isAlive())) {
            double d = v.distanceToSqr(sp);
            if (d < bestDist) { bestDist = d; best = v; }
        }
        return best;
    }
}
