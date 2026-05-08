package net.minecraft.client.renderer.debug;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.DebugEntityNameGenerator;
import net.minecraft.util.debug.DebugBeeInfo;
import net.minecraft.util.debug.DebugGoalInfo;
import net.minecraft.util.debug.DebugHiveInfo;
import net.minecraft.util.debug.DebugSubscriptions;
import net.minecraft.util.debug.DebugValueAccess;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class BeeDebugRenderer implements DebugRenderer.SimpleDebugRenderer {
    private static final boolean SHOW_GOAL_FOR_ALL_BEES = true;
    private static final boolean SHOW_NAME_FOR_ALL_BEES = true;
    private static final boolean SHOW_HIVE_FOR_ALL_BEES = true;
    private static final boolean SHOW_FLOWER_POS_FOR_ALL_BEES = true;
    private static final boolean SHOW_TRAVEL_TICKS_FOR_ALL_BEES = true;
    private static final boolean SHOW_GOAL_FOR_SELECTED_BEE = true;
    private static final boolean SHOW_NAME_FOR_SELECTED_BEE = true;
    private static final boolean SHOW_HIVE_FOR_SELECTED_BEE = true;
    private static final boolean SHOW_FLOWER_POS_FOR_SELECTED_BEE = true;
    private static final boolean SHOW_TRAVEL_TICKS_FOR_SELECTED_BEE = true;
    private static final boolean SHOW_HIVE_MEMBERS = true;
    private static final boolean SHOW_BLACKLISTS = true;
    private static final int MAX_RENDER_DIST_FOR_HIVE_OVERLAY = 30;
    private static final int MAX_RENDER_DIST_FOR_BEE_OVERLAY = 30;
    private static final int MAX_TARGETING_DIST = 8;
    private static final float TEXT_SCALE = 0.02F;
    private static final int ORANGE = -23296;
    private static final int GRAY = -3355444;
    private static final int PINK = -98404;
    private final Minecraft minecraft;
    @Nullable
    private UUID lastLookedAtUuid;

    public BeeDebugRenderer(Minecraft minecraft) {
        this.minecraft = minecraft;
    }

    @Override
    public void render(
        PoseStack poseStack, MultiBufferSource bufferSource, double camX, double camY, double camZ, DebugValueAccess debugValueAccess, Frustum frustum
    ) {
        this.doRender(poseStack, bufferSource, debugValueAccess);
        if (!this.minecraft.player.isSpectator()) {
            this.updateLastLookedAtUuid();
        }
    }

    private void doRender(PoseStack poseStack, MultiBufferSource bufferSource, DebugValueAccess debugValueAccess) {
        BlockPos blockpos = this.getCamera().getBlockPosition();
        debugValueAccess.forEachEntity(DebugSubscriptions.BEES, (p_448721_, p_448722_) -> {
            if (this.minecraft.player.closerThan(p_448721_, 30.0)) {
                DebugGoalInfo debuggoalinfo = debugValueAccess.getEntityValue(DebugSubscriptions.GOAL_SELECTORS, p_448721_);
                this.renderBeeInfo(poseStack, bufferSource, p_448721_, p_448722_, debuggoalinfo);
            }
        });
        this.renderFlowerInfos(poseStack, bufferSource, debugValueAccess);
        Map<BlockPos, Set<UUID>> map = this.createHiveBlacklistMap(debugValueAccess);
        debugValueAccess.forEachBlock(DebugSubscriptions.BEE_HIVES, (p_448732_, p_448733_) -> {
            if (blockpos.closerThan(p_448732_, 30.0)) {
                highlightHive(poseStack, bufferSource, p_448732_);
                Set<UUID> set = map.getOrDefault(p_448732_, Set.of());
                this.renderHiveInfo(poseStack, bufferSource, p_448732_, p_448733_, set, debugValueAccess);
            }
        });
        this.getGhostHives(debugValueAccess).forEach((p_269699_, p_269700_) -> {
            if (blockpos.closerThan(p_269699_, 30.0)) {
                this.renderGhostHive(poseStack, bufferSource, p_269699_, (List<String>)p_269700_);
            }
        });
    }

    private Map<BlockPos, Set<UUID>> createHiveBlacklistMap(DebugValueAccess debugValueAccess) {
        Map<BlockPos, Set<UUID>> map = new HashMap<>();
        debugValueAccess.forEachEntity(DebugSubscriptions.BEES, (p_448716_, p_448717_) -> {
            for (BlockPos blockpos : p_448717_.blacklistedHives()) {
                map.computeIfAbsent(blockpos, p_293649_ -> new HashSet<>()).add(p_448716_.getUUID());
            }
        });
        return map;
    }

    private void renderFlowerInfos(PoseStack poseStack, MultiBufferSource bufferSource, DebugValueAccess debugValueAccess) {
        Map<BlockPos, Set<UUID>> map = new HashMap<>();
        debugValueAccess.forEachEntity(DebugSubscriptions.BEES, (p_448743_, p_448744_) -> {
            if (p_448744_.flowerPos().isPresent()) {
                map.computeIfAbsent(p_448744_.flowerPos().get(), p_448745_ -> new HashSet<>()).add(p_448743_.getUUID());
            }
        });
        map.forEach((p_448740_, p_448741_) -> {
            Set<String> set = p_448741_.stream().map(DebugEntityNameGenerator::getEntityName).collect(Collectors.toSet());
            int i = 1;
            DebugRenderer.renderTextOverBlock(poseStack, bufferSource, set.toString(), p_448740_, i++, -256, 0.02F);
            DebugRenderer.renderTextOverBlock(poseStack, bufferSource, "Flower", p_448740_, i++, -1, 0.02F);
            float f = 0.05F;
            DebugRenderer.renderFilledBox(poseStack, bufferSource, p_448740_, 0.05F, 0.8F, 0.8F, 0.0F, 0.3F);
        });
    }

    private static String getBeeUuidsAsString(Collection<UUID> beeUuids) {
        if (beeUuids.isEmpty()) {
            return "-";
        } else {
            return beeUuids.size() > 3
                ? beeUuids.size() + " bees"
                : beeUuids.stream().map(DebugEntityNameGenerator::getEntityName).collect(Collectors.toSet()).toString();
        }
    }

    private static void highlightHive(PoseStack poseStack, MultiBufferSource buffer, BlockPos hivePos) {
        float f = 0.05F;
        DebugRenderer.renderFilledBox(poseStack, buffer, hivePos, 0.05F, 0.2F, 0.2F, 1.0F, 0.3F);
    }

    private void renderGhostHive(PoseStack poseStack, MultiBufferSource buffer, BlockPos hivePos, List<String> ghostHives) {
        float f = 0.05F;
        DebugRenderer.renderFilledBox(poseStack, buffer, hivePos, 0.05F, 0.2F, 0.2F, 1.0F, 0.3F);
        DebugRenderer.renderTextOverBlock(poseStack, buffer, ghostHives.toString(), hivePos, 0, -256, 0.02F);
        DebugRenderer.renderTextOverBlock(poseStack, buffer, "Ghost Hive", hivePos, 1, -65536, 0.02F);
    }

    private void renderHiveInfo(
        PoseStack poseStack, MultiBufferSource bufferSource, BlockPos pos, DebugHiveInfo hiveInfo, Collection<UUID> blacklistedBees, DebugValueAccess debugValueAccess
    ) {
        int i = 0;
        if (!blacklistedBees.isEmpty()) {
            renderTextOverHive(poseStack, bufferSource, "Blacklisted by " + getBeeUuidsAsString(blacklistedBees), pos, hiveInfo, i++, -65536);
        }

        renderTextOverHive(poseStack, bufferSource, "Out: " + getBeeUuidsAsString(this.getHiveMembers(pos, debugValueAccess)), pos, hiveInfo, i++, -3355444);
        if (hiveInfo.occupantCount() == 0) {
            renderTextOverHive(poseStack, bufferSource, "In: -", pos, hiveInfo, i++, -256);
        } else if (hiveInfo.occupantCount() == 1) {
            renderTextOverHive(poseStack, bufferSource, "In: 1 bee", pos, hiveInfo, i++, -256);
        } else {
            renderTextOverHive(poseStack, bufferSource, "In: " + hiveInfo.occupantCount() + " bees", pos, hiveInfo, i++, -256);
        }

        renderTextOverHive(poseStack, bufferSource, "Honey: " + hiveInfo.honeyLevel(), pos, hiveInfo, i++, -23296);
        renderTextOverHive(
            poseStack, bufferSource, hiveInfo.type().getName().getString() + (hiveInfo.sedated() ? " (sedated)" : ""), pos, hiveInfo, i++, -1
        );
    }

    private void renderBeeInfo(PoseStack poseStack, MultiBufferSource bufferSource, Entity bee, DebugBeeInfo beeInfo, @Nullable DebugGoalInfo goalInfo) {
        boolean flag = this.isBeeSelected(bee);
        int i = 0;
        DebugRenderer.renderTextOverMob(poseStack, bufferSource, bee, i++, beeInfo.toString(), -1, 0.03F);
        if (beeInfo.hivePos().isEmpty()) {
            DebugRenderer.renderTextOverMob(poseStack, bufferSource, bee, i++, "No hive", -98404, 0.02F);
        } else {
            DebugRenderer.renderTextOverMob(
                poseStack, bufferSource, bee, i++, "Hive: " + this.getPosDescription(bee, beeInfo.hivePos().get()), -256, 0.02F
            );
        }

        if (beeInfo.flowerPos().isEmpty()) {
            DebugRenderer.renderTextOverMob(poseStack, bufferSource, bee, i++, "No flower", -98404, 0.02F);
        } else {
            DebugRenderer.renderTextOverMob(
                poseStack, bufferSource, bee, i++, "Flower: " + this.getPosDescription(bee, beeInfo.flowerPos().get()), -256, 0.02F
            );
        }

        if (goalInfo != null) {
            for (DebugGoalInfo.DebugGoal debuggoalinfo$debuggoal : goalInfo.goals()) {
                if (debuggoalinfo$debuggoal.isRunning()) {
                    DebugRenderer.renderTextOverMob(poseStack, bufferSource, bee, i++, debuggoalinfo$debuggoal.name(), -16711936, 0.02F);
                }
            }
        }

        if (beeInfo.travelTicks() > 0) {
            int j = beeInfo.travelTicks() < 2400 ? -3355444 : -23296;
            DebugRenderer.renderTextOverMob(poseStack, bufferSource, bee, i++, "Travelling: " + beeInfo.travelTicks() + " ticks", j, 0.02F);
        }
    }

    private static void renderTextOverHive(
        PoseStack poseStack, MultiBufferSource bufferSource, String text, BlockPos pos, DebugHiveInfo hiveInfo, int line, int color
    ) {
        DebugRenderer.renderTextOverBlock(poseStack, bufferSource, text, pos, line, color, 0.02F);
    }

    private Camera getCamera() {
        return this.minecraft.gameRenderer.getMainCamera();
    }

    private String getPosDescription(Entity entity, BlockPos pos) {
        double d0 = pos.distToCenterSqr(entity.position());
        double d1 = Math.round(d0 * 10.0) / 10.0;
        return pos.toShortString() + " (dist " + d1 + ")";
    }

    private boolean isBeeSelected(Entity entity) {
        return Objects.equals(this.lastLookedAtUuid, entity.getUUID());
    }

    private Collection<UUID> getHiveMembers(BlockPos pos, DebugValueAccess debugValueAccess) {
        Set<UUID> set = new HashSet<>();
        debugValueAccess.forEachEntity(DebugSubscriptions.BEES, (p_448725_, p_448726_) -> {
            if (p_448726_.hasHive(pos)) {
                set.add(p_448725_.getUUID());
            }
        });
        return set;
    }

    private Map<BlockPos, List<String>> getGhostHives(DebugValueAccess debugValueAccess) {
        Map<BlockPos, List<String>> map = new HashMap<>();
        debugValueAccess.forEachEntity(DebugSubscriptions.BEES, (p_448736_, p_448737_) -> {
            if (p_448737_.hivePos().isPresent() && debugValueAccess.getBlockValue(DebugSubscriptions.BEE_HIVES, p_448737_.hivePos().get()) == null) {
                map.computeIfAbsent(p_448737_.hivePos().get(), p_113140_ -> Lists.newArrayList()).add(DebugEntityNameGenerator.getEntityName(p_448736_));
            }
        });
        return map;
    }

    private void updateLastLookedAtUuid() {
        DebugRenderer.getTargetedEntity(this.minecraft.getCameraEntity(), 8).ifPresent(p_113059_ -> this.lastLookedAtUuid = p_113059_.getUUID());
    }
}
