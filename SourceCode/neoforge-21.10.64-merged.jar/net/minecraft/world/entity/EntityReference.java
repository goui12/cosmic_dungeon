package net.minecraft.world.entity;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.players.OldUsersConverter;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.entity.UUIDLookup;
import net.minecraft.world.level.entity.UniquelyIdentifyable;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public final class EntityReference<StoredEntityType extends UniquelyIdentifyable> {
    private static final Codec<? extends EntityReference<?>> CODEC = UUIDUtil.CODEC.xmap(EntityReference::new, EntityReference::getUUID);
    private static final StreamCodec<ByteBuf, ? extends EntityReference<?>> STREAM_CODEC = UUIDUtil.STREAM_CODEC
        .map(EntityReference::new, EntityReference::getUUID);
    private Either<UUID, StoredEntityType> entity;

    public static <Type extends UniquelyIdentifyable> Codec<EntityReference<Type>> codec() {
        return (Codec<EntityReference<Type>>)CODEC;
    }

    public static <Type extends UniquelyIdentifyable> StreamCodec<ByteBuf, EntityReference<Type>> streamCodec() {
        return (StreamCodec<ByteBuf, EntityReference<Type>>)STREAM_CODEC;
    }

    private EntityReference(StoredEntityType entity) {
        this.entity = Either.right(entity);
    }

    private EntityReference(UUID uuid) {
        this.entity = Either.left(uuid);
    }

    @Nullable
    public static <T extends UniquelyIdentifyable> EntityReference<T> of(@Nullable T entity) {
        return entity != null ? new EntityReference<>(entity) : null;
    }

    public static <T extends UniquelyIdentifyable> EntityReference<T> of(UUID id) {
        return new EntityReference<>(id);
    }

    public UUID getUUID() {
        return this.entity.map(p_394562_ -> (UUID)p_394562_, UniquelyIdentifyable::getUUID);
    }

    @Nullable
    public StoredEntityType getEntity(UUIDLookup<? extends UniquelyIdentifyable> uuidLookup, Class<StoredEntityType> entityClass) {
        Optional<StoredEntityType> optional = this.entity.right();
        if (optional.isPresent()) {
            StoredEntityType storedentitytype = optional.get();
            if (!storedentitytype.isRemoved()) {
                return storedentitytype;
            }

            this.entity = Either.left(storedentitytype.getUUID());
        }

        Optional<UUID> optional1 = this.entity.left();
        if (optional1.isPresent()) {
            StoredEntityType storedentitytype1 = this.resolve(uuidLookup.lookup(optional1.get()), entityClass);
            if (storedentitytype1 != null && !storedentitytype1.isRemoved()) {
                this.entity = Either.right(storedentitytype1);
                return storedentitytype1;
            }
        }

        return null;
    }

    @Nullable
    public StoredEntityType getEntity(Level level, Class<StoredEntityType> entityClass) {
        return Player.class.isAssignableFrom(entityClass)
            ? this.getEntity(level::getPlayerInAnyDimension, entityClass)
            : this.getEntity(level::getEntityInAnyDimension, entityClass);
    }

    @Nullable
    private StoredEntityType resolve(@Nullable UniquelyIdentifyable entity, Class<StoredEntityType> entityClass) {
        return entity != null && entityClass.isAssignableFrom(entity.getClass()) ? entityClass.cast(entity) : null;
    }

    public boolean matches(StoredEntityType entity) {
        return this.getUUID().equals(entity.getUUID());
    }

    public void store(ValueOutput output, String key) {
        output.store(key, UUIDUtil.CODEC, this.getUUID());
    }

    public static void store(@Nullable EntityReference<?> key, ValueOutput output, String uuid) {
        if (key != null) {
            key.store(output, uuid);
        }
    }

    @Nullable
    public static <StoredEntityType extends UniquelyIdentifyable> StoredEntityType get(
        @Nullable EntityReference<StoredEntityType> reference, Level level, Class<StoredEntityType> entityClass
    ) {
        return reference != null ? reference.getEntity(level, entityClass) : null;
    }

    @Nullable
    public static Entity getEntity(@Nullable EntityReference<Entity> reference, Level level) {
        return get(reference, level, Entity.class);
    }

    @Nullable
    public static LivingEntity getLivingEntity(@Nullable EntityReference<LivingEntity> reference, Level level) {
        return get(reference, level, LivingEntity.class);
    }

    @Nullable
    public static Player getPlayer(@Nullable EntityReference<Player> reference, Level level) {
        return get(reference, level, Player.class);
    }

    @Nullable
    public static <StoredEntityType extends UniquelyIdentifyable> EntityReference<StoredEntityType> read(ValueInput input, String key) {
        return input.read(key, EntityReference.<StoredEntityType>codec()).orElse(null);
    }

    @Nullable
    public static <StoredEntityType extends UniquelyIdentifyable> EntityReference<StoredEntityType> readWithOldOwnerConversion(
        ValueInput input, String key, Level level
    ) {
        Optional<UUID> optional = input.read(key, UUIDUtil.CODEC);
        return optional.isPresent()
            ? of(optional.get())
            : input.getString(key)
                .map(p_409192_ -> OldUsersConverter.convertMobOwnerIfNecessary(level.getServer(), p_409192_))
                .map(EntityReference<StoredEntityType>::new)
                .orElse(null);
    }

    @Override
    public boolean equals(Object other) {
        return other == this ? true : other instanceof EntityReference<?> entityreference && this.getUUID().equals(entityreference.getUUID());
    }

    @Override
    public int hashCode() {
        return this.getUUID().hashCode();
    }
}
