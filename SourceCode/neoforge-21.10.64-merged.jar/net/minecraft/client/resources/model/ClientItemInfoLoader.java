package net.minecraft.client.resources.model;

import com.google.gson.JsonElement;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.DataResult.Error;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.client.multiplayer.ClientRegistryLayer;
import net.minecraft.client.renderer.item.ClientItem;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.PlaceholderLookupProvider;
import net.minecraft.util.StrictJsonParser;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class ClientItemInfoLoader {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final FileToIdConverter LISTER = FileToIdConverter.json("items");

    public static CompletableFuture<ClientItemInfoLoader.LoadedClientInfos> scheduleLoad(ResourceManager resourceManager, Executor executor) {
        RegistryAccess.Frozen registryaccess$frozen = ClientRegistryLayer.createRegistryAccess().compositeAccess();
        return CompletableFuture.<Map<ResourceLocation, Resource>>supplyAsync(() -> LISTER.listMatchingResources(resourceManager), executor)
            .thenCompose(
                p_399357_ -> {
                    List<CompletableFuture<ClientItemInfoLoader.PendingLoad>> list = new ArrayList<>(p_399357_.size());
                    p_399357_.forEach(
                        (p_399361_, p_399362_) -> list.add(
                            CompletableFuture.supplyAsync(
                                () -> {
                                    ResourceLocation resourcelocation = LISTER.fileToId(p_399361_);

                                    try {
                                        ClientItemInfoLoader.PendingLoad clientiteminfoloader$pendingload;
                                        try (Reader reader = p_399362_.openAsReader()) {
                                            PlaceholderLookupProvider placeholderlookupprovider = new PlaceholderLookupProvider(registryaccess$frozen);
                                            DynamicOps<JsonElement> dynamicops = placeholderlookupprovider.createSerializationContext(JsonOps.INSTANCE);
                                            ClientItem clientitem = ClientItem.CODEC
                                                .parse(dynamicops, StrictJsonParser.parse(reader))
                                                .ifError(
                                                    p_390349_ -> LOGGER.error(
                                                        "Couldn't parse item model '{}' from pack '{}': {}",
                                                        resourcelocation,
                                                        p_399362_.sourcePackId(),
                                                        p_390349_.message()
                                                    )
                                                )
                                                .result()
                                                .map(
                                                    p_399364_ -> placeholderlookupprovider.hasRegisteredPlaceholders()
                                                        ? p_399364_.withRegistrySwapper(placeholderlookupprovider.createSwapper())
                                                        : p_399364_
                                                )
                                                .orElse(null);
                                            clientiteminfoloader$pendingload = new ClientItemInfoLoader.PendingLoad(resourcelocation, clientitem);
                                        }

                                        return clientiteminfoloader$pendingload;
                                    } catch (Exception exception) {
                                        LOGGER.error("Failed to open item model {} from pack '{}'", p_399361_, p_399362_.sourcePackId(), exception);
                                        return new ClientItemInfoLoader.PendingLoad(resourcelocation, null);
                                    }
                                },
                                executor
                            )
                        )
                    );
                    return Util.sequence(list).thenApply(p_390406_ -> {
                        Map<ResourceLocation, ClientItem> map = new HashMap<>();

                        for (ClientItemInfoLoader.PendingLoad clientiteminfoloader$pendingload : p_390406_) {
                            if (clientiteminfoloader$pendingload.clientItemInfo != null) {
                                map.put(clientiteminfoloader$pendingload.id, clientiteminfoloader$pendingload.clientItemInfo);
                            }
                        }

                        return new ClientItemInfoLoader.LoadedClientInfos(map);
                    });
                }
            );
    }

    @OnlyIn(Dist.CLIENT)
    public record LoadedClientInfos(Map<ResourceLocation, ClientItem> contents) {
    }

    @OnlyIn(Dist.CLIENT)
    record PendingLoad(ResourceLocation id, @Nullable ClientItem clientItemInfo) {
    }
}
