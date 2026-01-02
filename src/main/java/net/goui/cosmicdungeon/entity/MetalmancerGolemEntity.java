package net.goui.cosmicdungeon.entity;

import net.goui.cosmicdungeon.entity.ai.goal.MetalmancerGolemFollowOwnerGoal;
import net.goui.cosmicdungeon.entity.ai.goal.MetalmancerGolemProtectOwnerGoal;
import net.goui.cosmicdungeon.entity.ai.goal.MetalmancerGolemResonanceGoal;
import net.goui.cosmicdungeon.playerclass.metalmancer.MetalmancerResonanceTracker;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.UUID;

public class MetalmancerGolemEntity extends PathfinderMob {
    public final AnimationState walkAnimation   = new AnimationState();
    public final AnimationState attackAnimation = new AnimationState();
    public final AnimationState summonAnimation = new AnimationState();
    public final AnimationState deathAnimation  = new AnimationState();
    public final AnimationState idle1Animation  = new AnimationState();
    public final AnimationState idle2Animation  = new AnimationState();
    public final AnimationState idle3Animation  = new AnimationState();

    // Metalmancer owner binding
    private UUID ownerId;

    // True while this golem is standing still in synced resonance with its owner.
    private boolean resonating;

    // Desired follow distance
    public static final double FOLLOW_RADIUS = 4.0D;
    public static final double FOLLOW_RADIUS_SQR = FOLLOW_RADIUS * FOLLOW_RADIUS;

    // Summon animation length: 1.7917 s * 20 tps ≈ 36 ticks
    private static final int SUMMON_ANIMATION_TICKS = 36;

    public MetalmancerGolemEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.xpReward = 0; // summoned pet; tweak if you want drops/xp
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 40.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.4D)
                .add(Attributes.ATTACK_DAMAGE, 6.0D)
                .add(Attributes.FOLLOW_RANGE, 24.0D);
    }

    /* --------------------------- Owner binding --------------------------- */

    public void setOwner(Player owner) {
        if (owner != null) {
            this.ownerId = owner.getUUID();
        }
    }

    public Player getOwner() {
        return (this.ownerId != null) ? this.level().getPlayerByUUID(this.ownerId) : null;
    }

    public UUID getOwnerId() {
        return this.ownerId;
    }

    /* --------------------------- Resonance state --------------------------- */

    public boolean isResonating() {
        return this.resonating;
    }

    /**
     * Server-side: mark this golem as resonating or not, and update the
     * global MetalmancerResonanceTracker so SatchelIdleTicker can double ore.
     */
    public void setResonating(boolean resonating) {
        if (this.resonating == resonating) return;
        this.resonating = resonating;

        if (!this.level().isClientSide() && this.ownerId != null) {
            MetalmancerResonanceTracker.setGolemResonating(this.ownerId, resonating);
        }
    }

    /* --------------------------- AI goals --------------------------- */

    @Override
    protected void registerGoals() {
        // --- Core movement / combat goals ---
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0D, true));

        // Synced resonance goal: stand still & look at owner while owner is resonating.
        this.goalSelector.addGoal(3, new MetalmancerGolemResonanceGoal(this, FOLLOW_RADIUS));

        // Follow owner to stay within the desired orbit.
        this.goalSelector.addGoal(4, new MetalmancerGolemFollowOwnerGoal(this, 1.0D, FOLLOW_RADIUS, 2.0D));

        // Face the owner (or other nearby players) and idle look.
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));

        // --- Targeting goals ---
        // Protect owner: attack mobs that owner is attacking OR that are attacking owner.
        this.targetSelector.addGoal(1, new MetalmancerGolemProtectOwnerGoal(this, 16.0D));
    }

    /* --------------------------- Saving / loading --------------------------- */

    @Override
    protected void addAdditionalSaveData(ValueOutput out) {
        super.addAdditionalSaveData(out);

        out.putBoolean("Resonating", this.resonating);

        if (this.ownerId != null) {
            out.putString("Owner", this.ownerId.toString());
        }
    }

    @Override
    protected void readAdditionalSaveData(ValueInput in) {
        super.readAdditionalSaveData(in);

        this.resonating = in.getBooleanOr("Resonating", false);

        in.getString("Owner").ifPresent(s -> {
            try {
                this.ownerId = UUID.fromString(s);
            } catch (IllegalArgumentException ignored) {
                this.ownerId = null;
            }
        });

        // Re-register resonance contribution if we load a resonating golem
        if (!this.level().isClientSide() && this.ownerId != null && this.resonating) {
            MetalmancerResonanceTracker.setGolemResonating(this.ownerId, true);
        }
    }

    @Override
    public void remove(RemovalReason reason) {
        // Ensure we clear resonance contribution if this golem despawns/dies.
        if (!this.level().isClientSide() && this.ownerId != null) {
            MetalmancerResonanceTracker.setGolemResonating(this.ownerId, false);
        }
        super.remove(reason);
    }

    /* --------------------------- Client-side animation driving --------------------------- */

    @Override
    public void tick() {
        super.tick();

        if (this.level().isClientSide()) {
            // Play the summon animation only for the first ~36 ticks after spawn.
            if (this.tickCount < SUMMON_ANIMATION_TICKS) {
                this.summonAnimation.startIfStopped(this.tickCount);
            } else {
                this.summonAnimation.stop();
            }
        }
    }
}
