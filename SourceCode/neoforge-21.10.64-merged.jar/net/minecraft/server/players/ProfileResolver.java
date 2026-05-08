package net.minecraft.server.players;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.authlib.yggdrasil.ProfileResult;
import com.mojang.datafixers.util.Either;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.util.StringUtil;

public interface ProfileResolver {
    Optional<GameProfile> fetchByName(String name);

    Optional<GameProfile> fetchById(UUID id);

    default Optional<GameProfile> fetchByNameOrId(Either<String, UUID> nameOrId) {
        return nameOrId.map(this::fetchByName, this::fetchById);
    }

    public static class Cached implements ProfileResolver {
        private final LoadingCache<String, Optional<GameProfile>> profileCacheByName;
        final LoadingCache<UUID, Optional<GameProfile>> profileCacheById;

        public Cached(final MinecraftSessionService sessionService, final UserNameToIdResolver resolver) {
            this.profileCacheById = CacheBuilder.newBuilder()
                .expireAfterAccess(Duration.ofMinutes(10L))
                .maximumSize(256L)
                .build(new CacheLoader<UUID, Optional<GameProfile>>() {
                    public Optional<GameProfile> load(UUID p_439902_) {
                        ProfileResult profileresult = sessionService.fetchProfile(p_439902_, true);
                        return Optional.ofNullable(profileresult).map(ProfileResult::profile);
                    }
                });
            this.profileCacheByName = CacheBuilder.newBuilder()
                .expireAfterAccess(Duration.ofMinutes(10L))
                .maximumSize(256L)
                .build(new CacheLoader<String, Optional<GameProfile>>() {
                    public Optional<GameProfile> load(String p_440372_) {
                        return resolver.get(p_440372_).flatMap(p_439371_ -> Cached.this.profileCacheById.getUnchecked(p_439371_.id()));
                    }
                });
        }

        @Override
        public Optional<GameProfile> fetchByName(String name) {
            return StringUtil.isValidPlayerName(name) ? this.profileCacheByName.getUnchecked(name) : Optional.empty();
        }

        @Override
        public Optional<GameProfile> fetchById(UUID id) {
            return this.profileCacheById.getUnchecked(id);
        }
    }
}
