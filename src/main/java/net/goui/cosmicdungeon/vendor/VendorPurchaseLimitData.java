package net.goui.cosmicdungeon.vendor;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class VendorPurchaseLimitData extends SavedData {
    public static final String SAVE_ID = "cosmicdungeon_vendor_purchase_limits_v1";

    private static final Codec<Map<String, Integer>> PURCHASES_CODEC = Codec.unboundedMap(Codec.STRING, Codec.INT);

    private static final Codec<VendorPurchaseLimitData> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            PURCHASES_CODEC.optionalFieldOf("purchases", Map.of()).forGetter(data -> data.purchasesByKey)
    ).apply(inst, VendorPurchaseLimitData::fromCodec));

    public static final SavedDataType<VendorPurchaseLimitData> TYPE = new SavedDataType<>(
            SAVE_ID,
            VendorPurchaseLimitData::new,
            CODEC
    );

    private final Map<String, Integer> purchasesByKey = new HashMap<>();

    private VendorPurchaseLimitData() {}

    private static VendorPurchaseLimitData fromCodec(Map<String, Integer> purchases) {
        VendorPurchaseLimitData data = new VendorPurchaseLimitData();
        if (purchases != null) {
            purchases.forEach((key, count) -> {
                if (key != null && !key.isBlank() && count != null && count > 0) {
                    data.purchasesByKey.put(key, count);
                }
            });
        }
        return data;
    }

    public static VendorPurchaseLimitData get(MinecraftServer server) {
        if (server == null) throw new IllegalArgumentException("server is null");
        ServerLevel overworld = server.overworld();
        return overworld.getDataStorage().computeIfAbsent(TYPE);
    }

    public int getPurchaseCount(UUID playerId, ResourceLocation profileId, ResourceLocation offerId) {
        if (playerId == null || profileId == null || offerId == null) return 0;
        return Math.max(0, purchasesByKey.getOrDefault(key(playerId, profileId, offerId), 0));
    }

    public boolean hasReachedLimit(UUID playerId, ResourceLocation profileId, ResourceLocation offerId, Integer maxPurchasesPerPlayer) {
        if (maxPurchasesPerPlayer == null || maxPurchasesPerPlayer <= 0) return false;
        return getPurchaseCount(playerId, profileId, offerId) >= maxPurchasesPerPlayer;
    }

    public void recordPurchase(UUID playerId, ResourceLocation profileId, ResourceLocation offerId) {
        if (playerId == null || profileId == null || offerId == null) return;
        String key = key(playerId, profileId, offerId);
        int current = Math.max(0, purchasesByKey.getOrDefault(key, 0));
        purchasesByKey.put(key, current == Integer.MAX_VALUE ? Integer.MAX_VALUE : current + 1);
        setDirty();
    }

    private static String key(UUID playerId, ResourceLocation profileId, ResourceLocation offerId) {
        return playerId + "|" + profileId + "|" + offerId;
    }
}
