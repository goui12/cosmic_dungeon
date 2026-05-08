package net.minecraft.client.player;

import com.mojang.authlib.GameProfile;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.ClientAvatarEntity;
import net.minecraft.client.entity.ClientAvatarState;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.numbers.StyledFormat;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Parrot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.ReadOnlyScoreInfo;
import net.minecraft.world.scores.Scoreboard;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public abstract class AbstractClientPlayer extends Player implements ClientAvatarEntity {
    @Nullable
    private PlayerInfo playerInfo;
    private final boolean showExtraEars;
    private final ClientAvatarState clientAvatarState = new ClientAvatarState();

    public AbstractClientPlayer(ClientLevel clientLevel, GameProfile gameProfile) {
        super(clientLevel, gameProfile);
        this.showExtraEars = "deadmau5".equals(this.getGameProfile().name());
    }

    @Nullable
    @Override
    public GameType gameMode() {
        PlayerInfo playerinfo = this.getPlayerInfo();
        return playerinfo != null ? playerinfo.getGameMode() : null;
    }

    @Nullable
    protected PlayerInfo getPlayerInfo() {
        if (this.playerInfo == null) {
            this.playerInfo = Minecraft.getInstance().getConnection().getPlayerInfo(this.getUUID());
        }

        return this.playerInfo;
    }

    @Override
    public void tick() {
        this.clientAvatarState.tick(this.position(), this.getDeltaMovement());
        super.tick();
    }

    protected void addWalkedDistance(float walkDistance) {
        this.clientAvatarState.addWalkDistance(walkDistance);
    }

    @Override
    public ClientAvatarState avatarState() {
        return this.clientAvatarState;
    }

    @Nullable
    @Override
    public Component belowNameDisplay() {
        Scoreboard scoreboard = this.level().getScoreboard();
        Objective objective = scoreboard.getDisplayObjective(DisplaySlot.BELOW_NAME);
        if (objective != null) {
            ReadOnlyScoreInfo readonlyscoreinfo = scoreboard.getPlayerScoreInfo(this, objective);
            Component component = ReadOnlyScoreInfo.safeFormatValue(readonlyscoreinfo, objective.numberFormatOrDefault(StyledFormat.NO_STYLE));
            return Component.empty().append(component).append(CommonComponents.SPACE).append(objective.getDisplayName());
        } else {
            return null;
        }
    }

    @Override
    public PlayerSkin getSkin() {
        PlayerInfo playerinfo = this.getPlayerInfo();
        return playerinfo == null ? DefaultPlayerSkin.get(this.getUUID()) : playerinfo.getSkin();
    }

    @Nullable
    @Override
    public Parrot.Variant getParrotVariantOnShoulder(boolean left) {
        return (left ? this.getShoulderParrotLeft() : this.getShoulderParrotRight()).orElse(null);
    }

    @Override
    public void rideTick() {
        super.rideTick();
        this.avatarState().resetBob();
    }

    @Override
    public void aiStep() {
        this.updateBob();
        super.aiStep();
    }

    protected void updateBob() {
        float f;
        if (this.onGround() && !this.isDeadOrDying() && !this.isSwimming()) {
            f = Math.min(0.1F, (float)this.getDeltaMovement().horizontalDistance());
        } else {
            f = 0.0F;
        }

        this.avatarState().updateBob(f);
    }

    public float getFieldOfViewModifier(boolean isFirstPerson, float fovEffectScale) {
        float f = 1.0F;
        if (this.getAbilities().flying) {
            f *= 1.1F;
        }

        float f1 = this.getAbilities().getWalkingSpeed();
        if (f1 != 0.0F) {
            float f2 = (float)this.getAttributeValue(Attributes.MOVEMENT_SPEED) / f1;
            f *= (f2 + 1.0F) / 2.0F;
        }

        if (this.isUsingItem()) {
            if (this.getUseItem().is(Items.BOW)) {
                float f3 = Math.min(this.getTicksUsingItem() / 20.0F, 1.0F);
                f *= 1.0F - Mth.square(f3) * 0.15F;
            } else if (isFirstPerson && this.isScoping()) {
                return 0.1F;
            }
        }

        return net.neoforged.neoforge.client.ClientHooks.getFieldOfViewModifier(this, f, fovEffectScale);
    }

    @Override
    public boolean showExtraEars() {
        return this.showExtraEars;
    }
}
