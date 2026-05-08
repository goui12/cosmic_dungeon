package net.minecraft.client.renderer;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.textures.GpuTextureView;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.client.gui.font.GlyphRenderTypes;
import net.minecraft.client.renderer.blockentity.SkullBlockRenderer;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.client.resources.SkinManager;
import net.minecraft.server.players.ProfileResolver;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.world.item.component.ResolvableProfile;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class PlayerSkinRenderCache {
    public static final RenderType DEFAULT_PLAYER_SKIN_RENDER_TYPE = playerSkinRenderType(DefaultPlayerSkin.getDefaultSkin());
    public static final Duration CACHE_DURATION = Duration.ofMinutes(5L);
    private final LoadingCache<ResolvableProfile, CompletableFuture<Optional<PlayerSkinRenderCache.RenderInfo>>> renderInfoCache = CacheBuilder.newBuilder()
        .expireAfterAccess(CACHE_DURATION)
        .build(
            new CacheLoader<ResolvableProfile, CompletableFuture<Optional<PlayerSkinRenderCache.RenderInfo>>>() {
                public CompletableFuture<Optional<PlayerSkinRenderCache.RenderInfo>> load(ResolvableProfile p_440714_) {
                    return p_440714_.resolveProfile(PlayerSkinRenderCache.this.profileResolver)
                        .thenCompose(
                            p_450739_ -> PlayerSkinRenderCache.this.skinManager
                                .get(p_450739_)
                                .thenApply(
                                    p_450734_ -> p_450734_.map(
                                        p_450737_ -> PlayerSkinRenderCache.this.new RenderInfo(p_450739_, p_450737_, p_440714_.skinPatch())
                                    )
                                )
                        );
                }
            }
        );
    private final LoadingCache<ResolvableProfile, PlayerSkinRenderCache.RenderInfo> defaultSkinCache = CacheBuilder.newBuilder()
        .expireAfterAccess(CACHE_DURATION)
        .build(new CacheLoader<ResolvableProfile, PlayerSkinRenderCache.RenderInfo>() {
            public PlayerSkinRenderCache.RenderInfo load(ResolvableProfile profile) {
                GameProfile gameprofile = profile.partialProfile();
                return PlayerSkinRenderCache.this.new RenderInfo(gameprofile, DefaultPlayerSkin.get(gameprofile), profile.skinPatch());
            }
        });
    final TextureManager textureManager;
    final SkinManager skinManager;
    final ProfileResolver profileResolver;

    public PlayerSkinRenderCache(TextureManager textureManager, SkinManager skinManager, ProfileResolver profileResolver) {
        this.textureManager = textureManager;
        this.skinManager = skinManager;
        this.profileResolver = profileResolver;
    }

    public PlayerSkinRenderCache.RenderInfo getOrDefault(ResolvableProfile profile) {
        PlayerSkinRenderCache.RenderInfo playerskinrendercache$renderinfo = this.lookup(profile).getNow(Optional.empty()).orElse(null);
        return playerskinrendercache$renderinfo != null ? playerskinrendercache$renderinfo : this.defaultSkinCache.getUnchecked(profile);
    }

    public Supplier<PlayerSkinRenderCache.RenderInfo> createLookup(ResolvableProfile profile) {
        PlayerSkinRenderCache.RenderInfo playerskinrendercache$renderinfo = this.defaultSkinCache.getUnchecked(profile);
        CompletableFuture<Optional<PlayerSkinRenderCache.RenderInfo>> completablefuture = this.renderInfoCache.getUnchecked(profile);
        Optional<PlayerSkinRenderCache.RenderInfo> optional = completablefuture.getNow(null);
        if (optional != null) {
            PlayerSkinRenderCache.RenderInfo playerskinrendercache$renderinfo1 = optional.orElse(playerskinrendercache$renderinfo);
            return () -> playerskinrendercache$renderinfo1;
        } else {
            return () -> completablefuture.getNow(Optional.empty()).orElse(playerskinrendercache$renderinfo);
        }
    }

    public CompletableFuture<Optional<PlayerSkinRenderCache.RenderInfo>> lookup(ResolvableProfile profile) {
        return this.renderInfoCache.getUnchecked(profile);
    }

    static RenderType playerSkinRenderType(PlayerSkin skin) {
        return SkullBlockRenderer.getPlayerSkinRenderType(skin.body().texturePath());
    }

    @OnlyIn(Dist.CLIENT)
    public final class RenderInfo {
        private final GameProfile gameProfile;
        private final PlayerSkin playerSkin;
        @Nullable
        private RenderType itemRenderType;
        @Nullable
        private GpuTextureView textureView;
        @Nullable
        private GlyphRenderTypes glyphRenderTypes;

        public RenderInfo(GameProfile gameProfile, PlayerSkin playerSkin, PlayerSkin.Patch patch) {
            this.gameProfile = gameProfile;
            this.playerSkin = playerSkin.with(patch);
        }

        public GameProfile gameProfile() {
            return this.gameProfile;
        }

        public PlayerSkin playerSkin() {
            return this.playerSkin;
        }

        public RenderType renderType() {
            if (this.itemRenderType == null) {
                this.itemRenderType = PlayerSkinRenderCache.playerSkinRenderType(this.playerSkin);
            }

            return this.itemRenderType;
        }

        public GpuTextureView textureView() {
            if (this.textureView == null) {
                this.textureView = PlayerSkinRenderCache.this.textureManager.getTexture(this.playerSkin.body().texturePath()).getTextureView();
            }

            return this.textureView;
        }

        public GlyphRenderTypes glyphRenderTypes() {
            if (this.glyphRenderTypes == null) {
                this.glyphRenderTypes = GlyphRenderTypes.createForColorTexture(this.playerSkin.body().texturePath());
            }

            return this.glyphRenderTypes;
        }

        @Override
        public boolean equals(Object other) {
            return this == other
                || other instanceof PlayerSkinRenderCache.RenderInfo playerskinrendercache$renderinfo
                    && this.gameProfile.equals(playerskinrendercache$renderinfo.gameProfile)
                    && this.playerSkin.equals(playerskinrendercache$renderinfo.playerSkin);
        }

        @Override
        public int hashCode() {
            int i = 1;
            i = 31 * i + this.gameProfile.hashCode();
            return 31 * i + this.playerSkin.hashCode();
        }
    }
}
