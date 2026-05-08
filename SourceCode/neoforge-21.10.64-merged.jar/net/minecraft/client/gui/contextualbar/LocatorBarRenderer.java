package net.minecraft.client.gui.contextualbar;

import java.util.UUID;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.WaypointStyle;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.TickRateManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.waypoints.PartialTickSupplier;
import net.minecraft.world.waypoints.TrackedWaypoint;
import net.minecraft.world.waypoints.Waypoint;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class LocatorBarRenderer implements ContextualBarRenderer {
    private static final ResourceLocation LOCATOR_BAR_BACKGROUND = ResourceLocation.withDefaultNamespace("hud/locator_bar_background");
    private static final ResourceLocation LOCATOR_BAR_ARROW_UP = ResourceLocation.withDefaultNamespace("hud/locator_bar_arrow_up");
    private static final ResourceLocation LOCATOR_BAR_ARROW_DOWN = ResourceLocation.withDefaultNamespace("hud/locator_bar_arrow_down");
    private static final int DOT_SIZE = 9;
    private static final int VISIBLE_DEGREE_RANGE = 60;
    private static final int ARROW_WIDTH = 7;
    private static final int ARROW_HEIGHT = 5;
    private static final int ARROW_LEFT = 1;
    private static final int ARROW_PADDING = 1;
    private final Minecraft minecraft;

    public LocatorBarRenderer(Minecraft minecraft) {
        this.minecraft = minecraft;
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
        guiGraphics.blitSprite(
            RenderPipelines.GUI_TEXTURED, LOCATOR_BAR_BACKGROUND, this.left(this.minecraft.getWindow()), this.top(this.minecraft.getWindow()), 182, 5
        );
    }

    @Override
    public void render(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
        int i = this.top(this.minecraft.getWindow());
        Entity entity = this.minecraft.getCameraEntity();
        if (entity != null) {
            Level level = entity.level();
            TickRateManager tickratemanager = level.tickRateManager();
            PartialTickSupplier partialticksupplier = p_437152_ -> deltaTracker.getGameTimeDeltaPartialTick(!tickratemanager.isEntityFrozen(p_437152_));
            this.minecraft
                .player
                .connection
                .getWaypointManager()
                .forEachWaypoint(
                    entity,
                    p_437160_ -> {
                        if (!p_437160_.id().left().map(p_437154_ -> p_437154_.equals(entity.getUUID())).orElse(false)) {
                            double d0 = p_437160_.yawAngleToCamera(level, this.minecraft.gameRenderer.getMainCamera(), partialticksupplier);
                            if (!(d0 <= -60.0) && !(d0 > 60.0)) {
                                int j = Mth.ceil((guiGraphics.guiWidth() - 9) / 2.0F);
                                Waypoint.Icon waypoint$icon = p_437160_.icon();
                                WaypointStyle waypointstyle = this.minecraft.getWaypointStyles().get(waypoint$icon.style);
                                float f = Mth.sqrt((float)p_437160_.distanceSquared(entity));
                                ResourceLocation resourcelocation = waypointstyle.sprite(f);
                                int k = waypoint$icon.color
                                    .orElseGet(
                                        () -> p_437160_.id()
                                            .map(
                                                p_419413_ -> ARGB.setBrightness(ARGB.color(255, p_419413_.hashCode()), 0.9F),
                                                p_419414_ -> ARGB.setBrightness(ARGB.color(255, p_419414_.hashCode()), 0.9F)
                                            )
                                    );
                                int l = Mth.floor(d0 * 173.0 / 2.0 / 60.0);
                                guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, resourcelocation, j + l, i - 2, 9, 9, k);
                                TrackedWaypoint.PitchDirection trackedwaypoint$pitchdirection = p_437160_.pitchDirectionToCamera(
                                    level, this.minecraft.gameRenderer, partialticksupplier
                                );
                                if (trackedwaypoint$pitchdirection != TrackedWaypoint.PitchDirection.NONE) {
                                    int i1;
                                    ResourceLocation resourcelocation1;
                                    if (trackedwaypoint$pitchdirection == TrackedWaypoint.PitchDirection.DOWN) {
                                        i1 = 6;
                                        resourcelocation1 = LOCATOR_BAR_ARROW_DOWN;
                                    } else {
                                        i1 = -6;
                                        resourcelocation1 = LOCATOR_BAR_ARROW_UP;
                                    }

                                    guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, resourcelocation1, j + l + 1, i + i1, 7, 5);
                                }
                            }
                        }
                    }
                );
        }
    }
}
