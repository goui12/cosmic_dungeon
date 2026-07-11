package net.goui.cosmicdungeon.vendor;

import com.google.gson.*;
import com.mojang.logging.LogUtils;
import net.goui.cosmicdungeon.economy.CurrencyDenomination;
import net.goui.cosmicdungeon.faction.FactionDefinitions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.InactiveProfiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.alchemy.PotionContents;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.util.*;

public final class VendorProfileManager extends SimplePreparableReloadListener<Map<ResourceLocation, VendorProfile>> {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String ROOT = "vendor_profiles";

    public static final VendorProfileManager INSTANCE = new VendorProfileManager();

    private volatile Map<ResourceLocation, VendorProfile> profiles = Map.of();

    private VendorProfileManager() {}

    @Override
    protected Map<ResourceLocation, VendorProfile> prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
        Map<ResourceLocation, VendorProfile> loaded = new LinkedHashMap<>();
        var resources = resourceManager.listResources(ROOT, rl -> rl.getPath().endsWith(".json"));

        for (Map.Entry<ResourceLocation, net.minecraft.server.packs.resources.Resource> e : resources.entrySet()) {
            ResourceLocation resourceRl = e.getKey();
            try (BufferedReader reader = e.getValue().openAsReader()) {
                JsonObject root = GsonHelper.fromJson(GSON, reader, JsonObject.class);
                if (root == null) {
                    LOGGER.error("[VendorProfiles] Empty/invalid JSON at {}", resourceRl);
                    continue;
                }

                ResourceLocation id = parseProfileId(resourceRl, root);
                VendorProfile profile = parseProfile(id, root);
                loaded.put(id, profile);
            } catch (Exception ex) {
                LOGGER.error("[VendorProfiles] Failed parsing {}: {}", resourceRl, ex.getMessage());
            }
        }
        return Map.copyOf(loaded);
    }

    @Override
    protected void apply(Map<ResourceLocation, VendorProfile> prepared, ResourceManager resourceManager, ProfilerFiller profiler) {
        this.profiles = prepared;
        LOGGER.info("[VendorProfiles] Loaded {} vendor profile(s)", prepared.size());
    }

    public VendorProfile get(ResourceLocation profileId) {
        return profiles.get(profileId);
    }

    public Set<ResourceLocation> listProfileIds() {
        return profiles.keySet();
    }

    public void reloadNow(ResourceManager manager) {
        Map<ResourceLocation, VendorProfile> prepared = prepare(manager, InactiveProfiler.INSTANCE);
        apply(prepared, manager, InactiveProfiler.INSTANCE);
    }

    private static ResourceLocation parseProfileId(ResourceLocation resourceRl, JsonObject root) {
        if (root.has("id")) {
            String raw = GsonHelper.getAsString(root, "id");
            ResourceLocation parsed = ResourceLocation.tryParse(raw);
            if (parsed != null) return parsed;
            throw new JsonParseException("Invalid profile id: " + raw);
        }

        String path = resourceRl.getPath();
        String suffix = ROOT + "/";
        int idx = path.indexOf(suffix);
        String relativePath = idx >= 0 ? path.substring(idx + suffix.length()) : path;
        if (relativePath.endsWith(".json")) {
            relativePath = relativePath.substring(0, relativePath.length() - 5);
        }
        return ResourceLocation.fromNamespaceAndPath(resourceRl.getNamespace(), relativePath);
    }

    private static VendorProfile parseProfile(ResourceLocation id, JsonObject root) {
        String displayName = GsonHelper.getAsString(root, "displayName", id.toString());
        String storeDisplayName = GsonHelper.getAsString(root, "storeDisplayName", displayNameFromProfileId(id));
        String vendorType = GsonHelper.getAsString(root, "vendorType", "generic");

        ResourceLocation factionId = null;
        if (root.has("requiredFaction")) {
            String raw = GsonHelper.getAsString(root, "requiredFaction");
            factionId = ResourceLocation.tryParse(raw);
            if (factionId == null) throw new JsonParseException("Invalid requiredFaction id: " + raw);
            if (FactionDefinitions.get(factionId) == null) throw new JsonParseException("Unknown requiredFaction id: " + raw);
        }

        Integer requiredFactionTier = root.has("requiredFactionTier") ? GsonHelper.getAsInt(root, "requiredFactionTier") : null;
        boolean requiredVillageAccess = GsonHelper.getAsBoolean(root, "requiredVillageAccess", false);
        String requiredNpcSystem = root.has("requiredNpcSystem") ? GsonHelper.getAsString(root, "requiredNpcSystem") : null;
        Integer requiredNpcTier = root.has("requiredNpcTier") ? GsonHelper.getAsInt(root, "requiredNpcTier") : null;

        List<VendorOffer> offers = parseOffers(id, GsonHelper.getAsJsonArray(root, "buyOffers", new JsonArray()));

        VendorProfile.BuybackConfig buyback = parseBuyback(root);

        return new VendorProfile(id, displayName, storeDisplayName, vendorType, requiredVillageAccess, requiredNpcSystem, requiredNpcTier, factionId, requiredFactionTier, List.copyOf(offers), buyback);
    }

    private static String displayNameFromProfileId(ResourceLocation id) {
        String path = id.getPath();
        int slash = path.lastIndexOf('/');
        String raw = slash >= 0 ? path.substring(slash + 1) : path;
        String[] words = raw.split("[_\\s-]+");
        StringBuilder displayName = new StringBuilder();
        for (String word : words) {
            if (word.isBlank()) continue;
            if (!displayName.isEmpty()) displayName.append(' ');
            displayName.append(Character.toUpperCase(word.charAt(0)));
            if (word.length() > 1) displayName.append(word.substring(1).toLowerCase(Locale.ROOT));
        }
        return displayName.isEmpty() ? "Vendor" : displayName.toString();
    }

    private static List<VendorOffer> parseOffers(ResourceLocation profileId, JsonArray offersArray) {
        List<VendorOffer> offers = new ArrayList<>();
        for (JsonElement el : offersArray) {
            JsonObject obj = el.getAsJsonObject();
            String rawOfferId = GsonHelper.getAsString(obj, "offerId");
            ResourceLocation offerId = ResourceLocation.tryParse(rawOfferId);
            if (offerId == null) {
                offerId = ResourceLocation.tryBuild(profileId.getNamespace(), profileId.getPath() + "/" + rawOfferId);
            }
            if (offerId == null) throw new JsonParseException("Invalid offer id: " + rawOfferId);

            ItemStackDef resultDef = parseItemStackDef(GsonHelper.getAsJsonObject(obj, "result"));
            validateItemExists(resultDef.itemId());

            VendorOffer.Cost cost = parseCost(GsonHelper.getAsJsonObject(obj, "cost"));

            offers.add(new VendorOffer(
                    offerId,
                    resultDef.toStack(),
                    cost,
                    obj.has("maxUses") ? GsonHelper.getAsInt(obj, "maxUses") : null,
                    obj.has("requiredFactionTier") ? GsonHelper.getAsInt(obj, "requiredFactionTier") : null,
                    obj.has("requiredProgressionFlag") ? GsonHelper.getAsString(obj, "requiredProgressionFlag") : null,
                    obj.has("requiredNpcTier") ? GsonHelper.getAsInt(obj, "requiredNpcTier") : null
            ));
        }
        return offers;
    }

    private static VendorProfile.BuybackConfig parseBuyback(JsonObject root) {
        if (!root.has("buyback")) {
            return new VendorProfile.BuybackConfig(null, List.of());
        }
        JsonObject buybackObj = GsonHelper.getAsJsonObject(root, "buyback");
        String pricingGroup = buybackObj.has("pricingGroup") ? GsonHelper.getAsString(buybackObj, "pricingGroup") : null;
        List<VendorProfile.BuybackRule> rules = new ArrayList<>();
        JsonArray rulesArray = GsonHelper.getAsJsonArray(buybackObj, "rules", new JsonArray());
        for (JsonElement el : rulesArray) {
            JsonObject o = el.getAsJsonObject();
            String rawItem = GsonHelper.getAsString(o, "item");
            ResourceLocation itemId = ResourceLocation.tryParse(rawItem);
            if (itemId == null) throw new JsonParseException("Invalid buyback item id: " + rawItem);
            validateItemExists(itemId);
            long min = GsonHelper.getAsLong(o, "minTrace", 0L);
            long max = GsonHelper.getAsLong(o, "maxTrace", Long.MAX_VALUE);
            double multiplier = GsonHelper.getAsDouble(o, "multiplier", 1.0d);
            rules.add(new VendorProfile.BuybackRule(itemId, min, max, multiplier));
        }
        return new VendorProfile.BuybackConfig(pricingGroup, List.copyOf(rules));
    }

    private static VendorOffer.Cost parseCost(JsonObject o) {
        long amount = GsonHelper.getAsLong(o, "amount");
        String denomRaw = GsonHelper.getAsString(o, "denomination", "trace");
        CurrencyDenomination denomination = CurrencyDenomination.fromId(denomRaw);
        if (denomination == null) {
            throw new JsonParseException("Invalid denomination in offer cost: " + denomRaw);
        }
        return new VendorOffer.Cost(amount, denomination);
    }

    private static ItemStackDef parseItemStackDef(JsonObject o) {
        String rawItem = GsonHelper.getAsString(o, "item");
        ResourceLocation itemId = ResourceLocation.tryParse(rawItem);
        if (itemId == null) throw new JsonParseException("Invalid item id: " + rawItem);
        int count = GsonHelper.getAsInt(o, "count", 1);
        if (count <= 0) throw new JsonParseException("Item count must be > 0");

        ResourceLocation potionId = null;
        if (o.has("potion")) {
            String rawPotion = GsonHelper.getAsString(o, "potion");
            potionId = ResourceLocation.tryParse(rawPotion);
            if (potionId == null) {
                potionId = ResourceLocation.tryBuild(ResourceLocation.DEFAULT_NAMESPACE, rawPotion);
            }
            if (potionId == null) throw new JsonParseException("Invalid potion id: " + rawPotion);
            validatePotionExists(potionId);
        }

        return new ItemStackDef(itemId, count, potionId);
    }

    private static void validateItemExists(ResourceLocation itemId) {
        if (BuiltInRegistries.ITEM.getValue(itemId) == null) {
            throw new JsonParseException("Unknown item id: " + itemId);
        }
    }

    private static void validatePotionExists(ResourceLocation potionId) {
        if (!BuiltInRegistries.POTION.containsKey(potionId)) {
            throw new JsonParseException("Unknown potion id: " + potionId);
        }
    }

    private record ItemStackDef(ResourceLocation itemId, int count, ResourceLocation potionId) {
        net.minecraft.world.item.ItemStack toStack() {
            var item = BuiltInRegistries.ITEM.getValue(itemId);
            if (item == null) {
                throw new JsonParseException("Unknown item id: " + itemId);
            }
            var stack = new net.minecraft.world.item.ItemStack(item, count);
            if (potionId != null) {
                var potion = BuiltInRegistries.POTION.get(potionId)
                        .orElseThrow(() -> new JsonParseException("Unknown potion id: " + potionId));
                stack.set(DataComponents.POTION_CONTENTS, new PotionContents(potion));
            }
            return stack;
        }
    }
}
