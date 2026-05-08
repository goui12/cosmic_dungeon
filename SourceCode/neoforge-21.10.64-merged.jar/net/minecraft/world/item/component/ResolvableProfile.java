package net.minecraft.world.item.component;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.PropertyMap;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import io.netty.buffer.ByteBuf;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.players.ProfileResolver;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;

public abstract sealed class ResolvableProfile implements TooltipProvider permits ResolvableProfile.Static, ResolvableProfile.Dynamic {
    private static final Codec<ResolvableProfile> FULL_CODEC = RecordCodecBuilder.create(
        p_450892_ -> p_450892_.group(
                Codec.mapEither(ExtraCodecs.STORED_GAME_PROFILE, ResolvableProfile.Partial.MAP_CODEC).forGetter(ResolvableProfile::unpack),
                PlayerSkin.Patch.MAP_CODEC.forGetter(ResolvableProfile::skinPatch)
            )
            .apply(p_450892_, ResolvableProfile::create)
    );
    public static final Codec<ResolvableProfile> CODEC = Codec.withAlternative(FULL_CODEC, ExtraCodecs.PLAYER_NAME, ResolvableProfile::createUnresolved);
    public static final StreamCodec<ByteBuf, ResolvableProfile> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.either(ByteBufCodecs.GAME_PROFILE, ResolvableProfile.Partial.STREAM_CODEC),
        ResolvableProfile::unpack,
        PlayerSkin.Patch.STREAM_CODEC,
        ResolvableProfile::skinPatch,
        ResolvableProfile::create
    );
    protected final GameProfile partialProfile;
    protected final PlayerSkin.Patch skinPatch;

    private static ResolvableProfile create(Either<GameProfile, ResolvableProfile.Partial> partialProfile, PlayerSkin.Patch skinPatch) {
        return partialProfile.map(
            p_450889_ -> new ResolvableProfile.Static(Either.left(p_450889_), skinPatch),
            p_450891_ -> (ResolvableProfile)(p_450891_.properties.isEmpty() && p_450891_.id.isPresent() != p_450891_.name.isPresent()
                ? p_450891_.name
                    .<ResolvableProfile>map(p_450897_ -> new ResolvableProfile.Dynamic(Either.left(p_450897_), skinPatch))
                    .orElseGet(() -> new ResolvableProfile.Dynamic(Either.right(p_450891_.id.get()), skinPatch))
                : new ResolvableProfile.Static(Either.right(p_450891_), skinPatch))
        );
    }

    public static ResolvableProfile createResolved(GameProfile gameProfile) {
        return new ResolvableProfile.Static(Either.left(gameProfile), PlayerSkin.Patch.EMPTY);
    }

    public static ResolvableProfile createUnresolved(String name) {
        return new ResolvableProfile.Dynamic(Either.left(name), PlayerSkin.Patch.EMPTY);
    }

    public static ResolvableProfile createUnresolved(UUID id) {
        return new ResolvableProfile.Dynamic(Either.right(id), PlayerSkin.Patch.EMPTY);
    }

    protected abstract Either<GameProfile, ResolvableProfile.Partial> unpack();

    protected ResolvableProfile(GameProfile partialProfile, PlayerSkin.Patch skinPatch) {
        this.partialProfile = partialProfile;
        this.skinPatch = skinPatch;
    }

    public abstract CompletableFuture<GameProfile> resolveProfile(ProfileResolver resolver);

    public GameProfile partialProfile() {
        return this.partialProfile;
    }

    public PlayerSkin.Patch skinPatch() {
        return this.skinPatch;
    }

    static GameProfile createPartialProfile(Optional<String> username, Optional<UUID> id, PropertyMap properties) {
        String s = username.orElse("");
        UUID uuid = id.orElseGet(() -> username.map(UUIDUtil::createOfflinePlayerUUID).orElse(Util.NIL_UUID));
        return new GameProfile(uuid, s, properties);
    }

    public abstract Optional<String> name();

    public static final class Dynamic extends ResolvableProfile {
        private static final Component DYNAMIC_TOOLTIP = Component.translatable("component.profile.dynamic").withStyle(ChatFormatting.GRAY);
        private final Either<String, UUID> nameOrId;

        Dynamic(Either<String, UUID> nameOrId, PlayerSkin.Patch patch) {
            super(ResolvableProfile.createPartialProfile(nameOrId.left(), nameOrId.right(), PropertyMap.EMPTY), patch);
            this.nameOrId = nameOrId;
        }

        @Override
        public Optional<String> name() {
            return this.nameOrId.left();
        }

        @Override
        public boolean equals(Object other) {
            return this == other
                || other instanceof ResolvableProfile.Dynamic resolvableprofile$dynamic
                    && this.nameOrId.equals(resolvableprofile$dynamic.nameOrId)
                    && this.skinPatch.equals(resolvableprofile$dynamic.skinPatch);
        }

        @Override
        public int hashCode() {
            int i = 31 + this.nameOrId.hashCode();
            return 31 * i + this.skinPatch.hashCode();
        }

        @Override
        protected Either<GameProfile, ResolvableProfile.Partial> unpack() {
            return Either.right(new ResolvableProfile.Partial(this.nameOrId.left(), this.nameOrId.right(), PropertyMap.EMPTY));
        }

        @Override
        public CompletableFuture<GameProfile> resolveProfile(ProfileResolver resolver) {
            return CompletableFuture.supplyAsync(() -> resolver.fetchByNameOrId(this.nameOrId).orElse(this.partialProfile), Util.nonCriticalIoPool());
        }

        @Override
        public void addToTooltip(Item.TooltipContext context, Consumer<Component> tooltipAdder, TooltipFlag flag, DataComponentGetter componentGetter) {
            tooltipAdder.accept(DYNAMIC_TOOLTIP);
        }
    }

    protected record Partial(Optional<String> name, Optional<UUID> id, PropertyMap properties) {
        public static final ResolvableProfile.Partial EMPTY = new ResolvableProfile.Partial(Optional.empty(), Optional.empty(), PropertyMap.EMPTY);
        static final MapCodec<ResolvableProfile.Partial> MAP_CODEC = RecordCodecBuilder.mapCodec(
            p_442475_ -> p_442475_.group(
                    ExtraCodecs.PLAYER_NAME.optionalFieldOf("name").forGetter(ResolvableProfile.Partial::name),
                    UUIDUtil.CODEC.optionalFieldOf("id").forGetter(ResolvableProfile.Partial::id),
                    ExtraCodecs.PROPERTY_MAP.optionalFieldOf("properties", PropertyMap.EMPTY).forGetter(ResolvableProfile.Partial::properties)
                )
                .apply(p_442475_, ResolvableProfile.Partial::new)
        );
        public static final StreamCodec<ByteBuf, ResolvableProfile.Partial> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.PLAYER_NAME.apply(ByteBufCodecs::optional),
            ResolvableProfile.Partial::name,
            UUIDUtil.STREAM_CODEC.apply(ByteBufCodecs::optional),
            ResolvableProfile.Partial::id,
            ByteBufCodecs.GAME_PROFILE_PROPERTIES,
            ResolvableProfile.Partial::properties,
            ResolvableProfile.Partial::new
        );

        private GameProfile createProfile() {
            return ResolvableProfile.createPartialProfile(this.name, this.id, this.properties);
        }
    }

    public static final class Static extends ResolvableProfile {
        public static final ResolvableProfile.Static EMPTY = new ResolvableProfile.Static(Either.right(ResolvableProfile.Partial.EMPTY), PlayerSkin.Patch.EMPTY);
        private final Either<GameProfile, ResolvableProfile.Partial> contents;

        Static(Either<GameProfile, ResolvableProfile.Partial> contents, PlayerSkin.Patch patch) {
            super(contents.map(p_439472_ -> (GameProfile)p_439472_, ResolvableProfile.Partial::createProfile), patch);
            this.contents = contents;
        }

        @Override
        public CompletableFuture<GameProfile> resolveProfile(ProfileResolver resolver) {
            return CompletableFuture.completedFuture(this.partialProfile);
        }

        @Override
        protected Either<GameProfile, ResolvableProfile.Partial> unpack() {
            return this.contents;
        }

        @Override
        public Optional<String> name() {
            return this.contents.map(p_442477_ -> Optional.of(p_442477_.name()), p_439586_ -> p_439586_.name);
        }

        @Override
        public boolean equals(Object other) {
            return this == other
                || other instanceof ResolvableProfile.Static resolvableprofile$static
                    && this.contents.equals(resolvableprofile$static.contents)
                    && this.skinPatch.equals(resolvableprofile$static.skinPatch);
        }

        @Override
        public int hashCode() {
            int i = 31 + this.contents.hashCode();
            return 31 * i + this.skinPatch.hashCode();
        }

        @Override
        public void addToTooltip(Item.TooltipContext context, Consumer<Component> tooltipAdder, TooltipFlag flag, DataComponentGetter componentGetter) {
        }
    }
}
