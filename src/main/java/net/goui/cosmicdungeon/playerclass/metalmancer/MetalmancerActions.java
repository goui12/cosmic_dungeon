package net.goui.cosmicdungeon.playerclass.metalmancer;

import net.goui.cosmicdungeon.entity.ModEntities;
import net.goui.cosmicdungeon.playerclass.api.ClassData;
import net.goui.cosmicdungeon.playerclass.api.ClassNbtUtil;
import net.goui.cosmicdungeon.entity.MetalmancerGolemEntity;
import net.goui.cosmicdungeon.playerclass.ore.SatchelApi;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.Random;

/**
 * Central server-side handler for all Metalmancer actions.
 *
 * All item uses (staffs, shaping file, magnets, etc.) should send a C2S action id and let this
 * class enforce cooldowns, ore costs, and trigger golem / projectile / resonance effects.
 *
 * NBT layout:
 *   player.persistentData["cosmicdungeon"]["metalmancer"] -> mmRoot
 * This mmRoot tag holds all per-player Metalmancer cooldowns and future state.
 */
public final class MetalmancerActions {
    private MetalmancerActions() {}

    // Public action ids used by ClassNet / client items.
    public static final String ACTION_MAGNET_L      = "magnet_l";
    public static final String ACTION_MAGNET_R      = "magnet_r";
    public static final String ACTION_FILE_L        = "file_l";
    public static final String ACTION_FILE_R        = "file_r";
    public static final String ACTION_STAFF_SUMMON  = "staff_summon";
    public static final String ACTION_STAFF_REFORGE = "staff_reforge";

    // Per-player PD subtree under cosmicdungeon.metalmancer
    private static final String KEY_MM_ROOT      = "metalmancer";
    private static final String KEY_CD_MAGNET_L  = "cd_magnet_l";
    private static final String KEY_CD_MAGNET_R  = "cd_magnet_r";
    private static final String KEY_CD_FILE_L    = "cd_file_l";
    private static final String KEY_CD_FILE_R    = "cd_file_r";
    private static final String KEY_CD_STAFF_SUM = "cd_staff_summon";
    private static final String KEY_CD_STAFF_REF = "cd_staff_reforge";

    // Cooldowns in ticks (20 tps)
    // These are T1 values. Higher tiers will either use different action IDs
    // or tier-aware branching inside each handler.
    private static final int CD_MAGNET_L   = 20;        // 1 s
    private static final int CD_MAGNET_R   = 20 * 15;   // 15 s
    private static final int CD_FILE_L     = 20 * 8;    // 8 s (T1 Manual Forage rate: ~30 ore/min at EV=4)
    private static final int CD_FILE_R     = 20 * 15;   // 15 s (Golem Recall)
    private static final int CD_STAFF_SUM  = 20 * 30;   // 30 s (Bronze Golem "summon time" – currently used as CD)
    private static final int CD_STAFF_REF  = 20 * 5;    // 5 s (Reforge window)

    // Lore: T1 Bronze Golem summon ore cost.
    private static final int ORE_COST_STAFF_SUM_T1 = 40;

    private static final Random RNG = new Random();

    /**
     * Entry point from ClassNet.C2S_Action.
     * @param sp       server player
     * @param actionId logical action id like "magnet_l", "file_r", etc.
     */
    public static void handleAction(ServerPlayer sp, String actionId) {
        // Only act if they are actually Metalmancer (double-gated with ClassNet)
        if (!ClassNbtUtil.isMetalmancer(sp)) return;

        long now = sp.level().getGameTime();
        CompoundTag mmRoot = getOrCreateMmRoot(sp);

        switch (actionId) {
            case ACTION_MAGNET_L      -> doMagnetLeft(sp, mmRoot, now);
            case ACTION_MAGNET_R      -> doMagnetRight(sp, mmRoot, now);
            case ACTION_FILE_L        -> doFileLeft(sp, mmRoot, now);
            case ACTION_FILE_R        -> doFileRight(sp, mmRoot, now);
            case ACTION_STAFF_SUMMON  -> doStaffSummon(sp, mmRoot, now);
            case ACTION_STAFF_REFORGE -> doStaffReforge(sp, mmRoot, now);
            default -> {
                // Unknown id: ignore for now; you can add logging if you want.
            }
        }

        saveMmRoot(sp, mmRoot);
    }

    /* ----------------- PD helpers ----------------- */

    private static CompoundTag getOrCreateMmRoot(ServerPlayer sp) {
        CompoundTag root = sp.getPersistentData()
                .getCompoundOrEmpty(ClassData.ROOT_TAG)
                .copy();
        return root.getCompound(KEY_MM_ROOT).orElseGet(CompoundTag::new);
    }

    private static void saveMmRoot(ServerPlayer sp, CompoundTag mmRoot) {
        CompoundTag root = sp.getPersistentData()
                .getCompoundOrEmpty(ClassData.ROOT_TAG)
                .copy();
        root.put(KEY_MM_ROOT, mmRoot);
        sp.getPersistentData().put(ClassData.ROOT_TAG, root);
    }

    private static boolean isOffCooldown(CompoundTag mmRoot, String key, long now) {
        long readyTick = mmRoot.getLongOr(key, 0L);
        return now >= readyTick;
    }

    private static void setCooldown(CompoundTag mmRoot, String key, long now, int durationTicks) {
        mmRoot.putLong(key, now + durationTicks);
    }

    /* ----------------- helper: active golem check ----------------- */

    /**
     * Returns true if this Metalmancer already has an active golem nearby.
     *
     * We search a fairly large radius around the player; in practice the pet should
     * always be somewhere in that space. If you ever end up doing cross-dimension
     * golems, you can add a dimension-wide tracker later.
     */
    private static boolean hasActiveGolem(ServerLevel level, ServerPlayer sp) {
        AABB searchBox = sp.getBoundingBox().inflate(128.0D); // 128-block radius cube
        List<MetalmancerGolemEntity> golems = level.getEntitiesOfClass(
                MetalmancerGolemEntity.class,
                searchBox,
                golem -> golem.isAlive() && sp.getUUID().equals(golem.getOwnerId())
        );
        return !golems.isEmpty();
    }

    /* ----------------- action implementations ----------------- */

    // Crude / Refined / Aetheric / Transcendent Resonance Magnet – Left click
    // Lore (T1):
    //   Ore Cost: 3
    //   Damage: 2.5 hearts
    //   Cooldown: 1 second
    //   Range: 10 blocks
    private static void doMagnetLeft(ServerPlayer sp, CompoundTag mmRoot, long now) {
        if (!isOffCooldown(mmRoot, KEY_CD_MAGNET_L, now)) return;

        // T1 ore cost: 3
        if (!SatchelApi.trySpend(sp, 3)) return;

        // TODO: spawn ore projectiles

        setCooldown(mmRoot, KEY_CD_MAGNET_L, now, CD_MAGNET_L);
    }

    // Crude / Refined / Aetheric / Transcendent Resonance Magnet – Right click
    // Lore (T1):
    //   Ore Cost: 9
    //   Damage: 2.5 hearts / sec for 3 s
    //   Chain range: 3 blocks between targets
    //   Initial target: within 5 blocks
    //   Cooldown: 15 s
    private static void doMagnetRight(ServerPlayer sp, CompoundTag mmRoot, long now) {
        if (!isOffCooldown(mmRoot, KEY_CD_MAGNET_R, now)) return;

        // T1 ore cost: 9
        if (!SatchelApi.trySpend(sp, 9)) return;

        // TODO: 3s chaining vortex

        setCooldown(mmRoot, KEY_CD_MAGNET_R, now, CD_MAGNET_R);
    }

    // Shaping File – Left click (Manual Forage)
    // Lore (T1):
    //   Base dice: 1d6 + 1 (EV = 4)
    //   Cooldown: 8s => ~30 ore/min at EV 4
    private static void doFileLeft(ServerPlayer sp, CompoundTag mmRoot, long now) {
        if (!isOffCooldown(mmRoot, KEY_CD_FILE_L, now)) return;

        // 1d6 + 1 → 2..7 ore (expected value 4)
        int gain = RNG.nextInt(6) + 1 + 1;
        SatchelApi.add(sp, gain);

        setCooldown(mmRoot, KEY_CD_FILE_L, now, CD_FILE_L);
    }

    // Shaping File – Right click (Golem Recall)
    // Lore:
    //   - No ore cost
    //   - 15s cooldown
    //   - Only works when golem is not in combat
    private static void doFileRight(ServerPlayer sp, CompoundTag mmRoot, long now) {
        if (!isOffCooldown(mmRoot, KEY_CD_FILE_R, now)) return;

        // TODO: find current golem owned by this player and recall it

        setCooldown(mmRoot, KEY_CD_FILE_R, now, CD_FILE_R);
    }

    // Summoning Staff – Right-click (Summon Golem)
    // Lore (Bronze Golem, T1):
    //   - Summon Time: 30s (channeling window)
    //   - Health: 20 hearts
    //   - Damage: 3 hearts @ 2.0s per strike (1.5 DPS)
    //
    // For now we treat the 30s as a simple cooldown and spawn immediately.
    private static void doStaffSummon(ServerPlayer sp, CompoundTag mmRoot, long now) {
        if (!isOffCooldown(mmRoot, KEY_CD_STAFF_SUM, now)) return;

        ServerLevel level = (ServerLevel) sp.level();

        // NEW: prevent multiple active golems per Metalmancer
        if (hasActiveGolem(level, sp)) {
            sp.sendSystemMessage(
                    Component.translatable("message.cosmicdungeon.metalmancer.golem_already_active")
                            .withStyle(ChatFormatting.YELLOW)
            );
            return;
        }

        // Lore: summoning a Bronze Golem consumes a large chunk of ore
        if (!SatchelApi.trySpend(sp, ORE_COST_STAFF_SUM_T1)) {
            sp.sendSystemMessage(
                    Component.translatable("message.cosmicdungeon.metalmancer.not_enough_ore")
                            .withStyle(ChatFormatting.RED)
            );
            return;
        }

        EntityType<MetalmancerGolemEntity> type = ModEntities.METALMANCER_GOLEM.get();
        if (type == null) {
            // Refund ore if something is catastrophically wrong with registration
            SatchelApi.add(sp, ORE_COST_STAFF_SUM_T1);
            sp.sendSystemMessage(
                    Component.literal("[Metalmancer] Golem EntityType is NULL")
                            .withStyle(ChatFormatting.RED)
            );
            return;
        }

        // Create the golem via the registered factory (Level, EntitySpawnReason)
        MetalmancerGolemEntity golem = type.create(level, EntitySpawnReason.MOB_SUMMONED);
        if (golem == null) {
            // Refund ore if instantiation fails
            SatchelApi.add(sp, ORE_COST_STAFF_SUM_T1);
            sp.sendSystemMessage(
                    Component.literal("[Metalmancer] Failed to create golem instance")
                            .withStyle(ChatFormatting.RED)
            );
            return;
        }

        // Position in front of the player
        double dist = 1.5;
        double yawRad = Math.toRadians(sp.getYRot());

        double x = sp.getX() - Math.sin(yawRad) * dist;
        double y = sp.getY();
        double z = sp.getZ() + Math.cos(yawRad) * dist;

        golem.setPos(x, y, z);
        golem.setYRot(sp.getYRot());
        golem.setYHeadRot(sp.getYRot());
        golem.yBodyRot = sp.getYRot();

        // Bind this golem to the summoner (owner UUID) so follow/resonance/recall
        // can all key off that relationship.
        golem.setOwner(sp);

        level.addFreshEntity(golem);

        setCooldown(mmRoot, KEY_CD_STAFF_SUM, now, CD_STAFF_SUM);
        sp.sendSystemMessage(
                Component.literal("Bronze Golem summoned.")
                        .withStyle(ChatFormatting.GREEN)
        );
    }

    // Summoning Staff – Sneak-right-click (Reforge)
    // Lore (Bronze Golem, T1):
    //   - Manual Reforging Cost: 20 ore / heart
    //   - Reforge Time: 5s
    //   - Self-Reforge (idle) later: 10 ore / heart at 2x efficiency
    private static void doStaffReforge(ServerPlayer sp, CompoundTag mmRoot, long now) {
        if (!isOffCooldown(mmRoot, KEY_CD_STAFF_REF, now)) return;

        // TODO: locate the owner's golem and apply ore-based healing

        setCooldown(mmRoot, KEY_CD_STAFF_REF, now, CD_STAFF_REF);
    }
}
