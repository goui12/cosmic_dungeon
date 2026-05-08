package net.minecraft.client.resources;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.hash.Hashing;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.SignatureState;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.authlib.minecraft.MinecraftProfileTextures;
import com.mojang.authlib.minecraft.MinecraftProfileTexture.Type;
import com.mojang.authlib.properties.Property;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.client.renderer.texture.SkinTextureDownloader;
import net.minecraft.core.ClientAsset;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Services;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.entity.player.PlayerSkin;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class SkinManager {
    static final Logger LOGGER = LogUtils.getLogger();
    private final Services services;
    final SkinTextureDownloader skinTextureDownloader;
    private final LoadingCache<SkinManager.CacheKey, CompletableFuture<Optional<PlayerSkin>>> skinCache;
    private final SkinManager.TextureCache skinTextures;
    private final SkinManager.TextureCache capeTextures;
    private final SkinManager.TextureCache elytraTextures;

    public SkinManager(Path skinDirectory, final Services services, SkinTextureDownloader skinTextureDownloader, final Executor executor) {
        this.services = services;
        this.skinTextureDownloader = skinTextureDownloader;
        this.skinTextures = new SkinManager.TextureCache(skinDirectory, Type.SKIN);
        this.capeTextures = new SkinManager.TextureCache(skinDirectory, Type.CAPE);
        this.elytraTextures = new SkinManager.TextureCache(skinDirectory, Type.ELYTRA);
        this.skinCache = CacheBuilder.newBuilder()
            .expireAfterAccess(Duration.ofSeconds(15L))
            .build(
                new CacheLoader<SkinManager.CacheKey, CompletableFuture<Optional<PlayerSkin>>>() {
                    public CompletableFuture<Optional<PlayerSkin>> load(SkinManager.CacheKey cacheKey) {
                        return CompletableFuture.<MinecraftProfileTextures>supplyAsync(() -> {
                                Property property = cacheKey.packedTextures();
                                if (property == null) {
                                    return MinecraftProfileTextures.EMPTY;
                                } else {
                                    MinecraftProfileTextures minecraftprofiletextures = services.sessionService().unpackTextures(property);
                                    if (minecraftprofiletextures.signatureState() == SignatureState.INVALID) {
                                        SkinManager.LOGGER
                                            .warn("Profile contained invalid signature for textures property (profile id: {})", cacheKey.profileId());
                                    }

                                    return minecraftprofiletextures;
                                }
                            }, Util.backgroundExecutor().forName("unpackSkinTextures"))
                            .thenComposeAsync(p_307130_ -> SkinManager.this.registerTextures(cacheKey.profileId(), p_307130_), executor)
                            .handle((p_451340_, p_389368_) -> {
                                if (p_389368_ != null) {
                                    SkinManager.LOGGER.warn("Failed to load texture for profile {}", cacheKey.profileId, p_389368_);
                                }

                                return Optional.ofNullable(p_451340_);
                            });
                    }
                }
            );
    }

    public Supplier<PlayerSkin> createLookup(GameProfile profile, boolean requireSecure) {
        CompletableFuture<Optional<PlayerSkin>> completablefuture = this.get(profile);
        PlayerSkin playerskin = DefaultPlayerSkin.get(profile);
        if (SharedConstants.DEBUG_DEFAULT_SKIN_OVERRIDE) {
            return () -> playerskin;
        } else {
            Optional<PlayerSkin> optional = completablefuture.getNow(null);
            if (optional != null) {
                PlayerSkin playerskin1 = optional.filter(p_450748_ -> !requireSecure || p_450748_.secure()).orElse(playerskin);
                return () -> playerskin1;
            } else {
                return () -> completablefuture.getNow(Optional.empty()).filter(p_450750_ -> !requireSecure || p_450750_.secure()).orElse(playerskin);
            }
        }
    }

    public CompletableFuture<Optional<PlayerSkin>> get(GameProfile profile) {
        if (SharedConstants.DEBUG_DEFAULT_SKIN_OVERRIDE) {
            PlayerSkin playerskin = DefaultPlayerSkin.get(profile);
            return CompletableFuture.completedFuture(Optional.of(playerskin));
        } else {
            Property property = this.services.sessionService().getPackedTextures(profile);
            return this.skinCache.getUnchecked(new SkinManager.CacheKey(profile.id(), property));
        }
    }

    CompletableFuture<PlayerSkin> registerTextures(UUID uuid, MinecraftProfileTextures textures) {
        MinecraftProfileTexture minecraftprofiletexture = textures.skin();
        CompletableFuture<ClientAsset.Texture> completablefuture;
        PlayerModelType playermodeltype;
        if (minecraftprofiletexture != null) {
            completablefuture = this.skinTextures.getOrLoad(minecraftprofiletexture);
            playermodeltype = PlayerModelType.byLegacyServicesName(minecraftprofiletexture.getMetadata("model"));
        } else {
            PlayerSkin playerskin = DefaultPlayerSkin.get(uuid);
            completablefuture = CompletableFuture.completedFuture(playerskin.body());
            playermodeltype = playerskin.model();
        }

        MinecraftProfileTexture minecraftprofiletexture2 = textures.cape();
        CompletableFuture<ClientAsset.Texture> completablefuture1 = minecraftprofiletexture2 != null
            ? this.capeTextures.getOrLoad(minecraftprofiletexture2)
            : CompletableFuture.completedFuture(null);
        MinecraftProfileTexture minecraftprofiletexture1 = textures.elytra();
        CompletableFuture<ClientAsset.Texture> completablefuture2 = minecraftprofiletexture1 != null
            ? this.elytraTextures.getOrLoad(minecraftprofiletexture1)
            : CompletableFuture.completedFuture(null);
        return CompletableFuture.allOf(completablefuture, completablefuture1, completablefuture2)
            .thenApply(
                p_450756_ -> new PlayerSkin(
                    completablefuture.join(),
                    completablefuture1.join(),
                    completablefuture2.join(),
                    playermodeltype,
                    textures.signatureState() == SignatureState.SIGNED
                )
            );
    }

    @OnlyIn(Dist.CLIENT)
    record CacheKey(UUID profileId, @Nullable Property packedTextures) {
    }

    @OnlyIn(Dist.CLIENT)
    class TextureCache {
        private final Path root;
        private final Type type;
        private final Map<String, CompletableFuture<ClientAsset.Texture>> textures = new Object2ObjectOpenHashMap<>();

        TextureCache(Path root, Type type) {
            this.root = root;
            this.type = type;
        }

        public CompletableFuture<ClientAsset.Texture> getOrLoad(MinecraftProfileTexture texture) {
            String s = texture.getHash();
            CompletableFuture<ClientAsset.Texture> completablefuture = this.textures.get(s);
            if (completablefuture == null) {
                completablefuture = this.registerTexture(texture);
                this.textures.put(s, completablefuture);
            }

            return completablefuture;
        }

        private CompletableFuture<ClientAsset.Texture> registerTexture(MinecraftProfileTexture texture) {
            String s = Hashing.sha1().hashUnencodedChars(texture.getHash()).toString();
            ResourceLocation resourcelocation = this.getTextureLocation(s);
            Path path = this.root.resolve(s.length() > 2 ? s.substring(0, 2) : "xx").resolve(s);
            return SkinManager.this.skinTextureDownloader.downloadAndRegisterSkin(resourcelocation, path, texture.getUrl(), this.type == Type.SKIN);
        }

        private ResourceLocation getTextureLocation(String name) {
            String s = switch (this.type) {
                case SKIN -> "skins";
                case CAPE -> "capes";
                case ELYTRA -> "elytra";
            };
            return ResourceLocation.withDefaultNamespace(s + "/" + name);
        }
    }
}
