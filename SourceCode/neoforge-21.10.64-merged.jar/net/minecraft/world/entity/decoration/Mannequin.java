package net.minecraft.world.entity.decoration;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class Mannequin extends Avatar {
    protected static final EntityDataAccessor<ResolvableProfile> DATA_PROFILE = SynchedEntityData.defineId(
        Mannequin.class, EntityDataSerializers.RESOLVABLE_PROFILE
    );
    private static final EntityDataAccessor<Boolean> DATA_IMMOVABLE = SynchedEntityData.defineId(Mannequin.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Optional<Component>> DATA_DESCRIPTION = SynchedEntityData.defineId(
        Mannequin.class, EntityDataSerializers.OPTIONAL_COMPONENT
    );
    private static final byte ALL_LAYERS = (byte)Arrays.stream(PlayerModelPart.values())
        .mapToInt(PlayerModelPart::getMask)
        .reduce(0, (p_445827_, p_445561_) -> p_445827_ | p_445561_);
    private static final Set<Pose> VALID_POSES = Set.of(Pose.STANDING, Pose.CROUCHING, Pose.SWIMMING, Pose.FALL_FLYING, Pose.SLEEPING);
    public static final Codec<Pose> POSE_CODEC = Pose.CODEC
        .validate(
            p_450882_ -> VALID_POSES.contains(p_450882_)
                ? DataResult.success(p_450882_)
                : DataResult.error(() -> "Invalid pose: " + p_450882_.getSerializedName())
        );
    private static final Codec<Byte> LAYERS_CODEC = PlayerModelPart.CODEC
        .listOf()
        .xmap(
            p_445977_ -> (byte)p_445977_.stream().mapToInt(PlayerModelPart::getMask).reduce(ALL_LAYERS, (p_447293_, p_446747_) -> p_447293_ & ~p_446747_),
            p_446234_ -> Arrays.stream(PlayerModelPart.values()).filter(p_445539_ -> (p_446234_ & p_445539_.getMask()) == 0).toList()
        );
    public static final ResolvableProfile DEFAULT_PROFILE = ResolvableProfile.Static.EMPTY;
    private static final Component DEFAULT_DESCRIPTION = Component.translatable("entity.minecraft.mannequin.label");
    protected static EntityType.EntityFactory<Mannequin> constructor = Mannequin::new;
    private static final String PROFILE_FIELD = "profile";
    private static final String HIDDEN_LAYERS_FIELD = "hidden_layers";
    private static final String MAIN_HAND_FIELD = "main_hand";
    private static final String POSE_FIELD = "pose";
    private static final String IMMOVABLE_FIELD = "immovable";
    private static final String DESCRIPTION_FIELD = "description";
    private static final String HIDE_DESCRIPTION_FIELD = "hide_description";
    private Component description = DEFAULT_DESCRIPTION;
    private boolean hideDescription = false;

    public Mannequin(EntityType<Mannequin> entityType, Level level) {
        super(entityType, level);
        this.entityData.set(DATA_PLAYER_MODE_CUSTOMISATION, ALL_LAYERS);
    }

    protected Mannequin(Level level) {
        this(EntityType.MANNEQUIN, level);
    }

    @Nullable
    public static Mannequin create(EntityType<Mannequin> entityType, Level level) {
        return constructor.create(entityType, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_PROFILE, DEFAULT_PROFILE);
        builder.define(DATA_IMMOVABLE, false);
        builder.define(DATA_DESCRIPTION, Optional.of(DEFAULT_DESCRIPTION));
    }

    protected ResolvableProfile getProfile() {
        return this.entityData.get(DATA_PROFILE);
    }

    private void setProfile(ResolvableProfile profile) {
        this.entityData.set(DATA_PROFILE, profile);
    }

    private boolean getImmovable() {
        return this.entityData.get(DATA_IMMOVABLE);
    }

    private void setImmovable(boolean immovable) {
        this.entityData.set(DATA_IMMOVABLE, immovable);
    }

    @Nullable
    protected Component getDescription() {
        return this.entityData.get(DATA_DESCRIPTION).orElse(null);
    }

    private void setDescription(Component description) {
        this.description = description;
        this.updateDescription();
    }

    private void setHideDescription(boolean hideDescription) {
        this.hideDescription = hideDescription;
        this.updateDescription();
    }

    private void updateDescription() {
        this.entityData.set(DATA_DESCRIPTION, this.hideDescription ? Optional.empty() : Optional.of(this.description));
    }

    @Override
    protected boolean isImmobile() {
        return this.getImmovable() || super.isImmobile();
    }

    @Override
    public boolean isEffectiveAi() {
        return !this.getImmovable() && super.isEffectiveAi();
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.store("profile", ResolvableProfile.CODEC, this.getProfile());
        output.store("hidden_layers", LAYERS_CODEC, this.entityData.get(DATA_PLAYER_MODE_CUSTOMISATION));
        output.store("main_hand", HumanoidArm.CODEC, this.getMainArm());
        output.store("pose", POSE_CODEC, this.getPose());
        output.putBoolean("immovable", this.getImmovable());
        Component component = this.getDescription();
        if (component != null) {
            if (!component.equals(DEFAULT_DESCRIPTION)) {
                output.store("description", ComponentSerialization.CODEC, component);
            }
        } else {
            output.putBoolean("hide_description", true);
        }
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        input.read("profile", ResolvableProfile.CODEC).ifPresent(this::setProfile);
        this.entityData.set(DATA_PLAYER_MODE_CUSTOMISATION, input.read("hidden_layers", LAYERS_CODEC).orElse(ALL_LAYERS));
        this.setMainArm(input.read("main_hand", HumanoidArm.CODEC).orElse(DEFAULT_MAIN_HAND));
        this.setPose(input.read("pose", POSE_CODEC).orElse(Pose.STANDING));
        this.setImmovable(input.getBooleanOr("immovable", false));
        this.setHideDescription(input.getBooleanOr("hide_description", false));
        this.setDescription(input.read("description", ComponentSerialization.CODEC).orElse(DEFAULT_DESCRIPTION));
    }

    @Nullable
    @Override
    public <T> T get(DataComponentType<? extends T> component) {
        return component == DataComponents.PROFILE ? castComponentValue((DataComponentType<T>)component, this.getProfile()) : super.get(component);
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter componentGetter) {
        this.applyImplicitComponentIfPresent(componentGetter, DataComponents.PROFILE);
        super.applyImplicitComponents(componentGetter);
    }

    @Override
    protected <T> boolean applyImplicitComponent(DataComponentType<T> component, T value) {
        if (component == DataComponents.PROFILE) {
            this.setProfile(castComponentValue(DataComponents.PROFILE, value));
            return true;
        } else {
            return super.applyImplicitComponent(component, value);
        }
    }
}
