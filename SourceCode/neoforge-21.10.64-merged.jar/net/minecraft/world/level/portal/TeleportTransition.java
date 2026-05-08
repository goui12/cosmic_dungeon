package net.minecraft.world.level.portal;

import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundLevelEventPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.phys.Vec3;

public record TeleportTransition(
    ServerLevel newLevel,
    Vec3 position,
    Vec3 deltaMovement,
    float yRot,
    float xRot,
    boolean missingRespawnBlock,
    boolean asPassenger,
    Set<Relative> relatives,
    TeleportTransition.PostTeleportTransition postTeleportTransition
) {
    public static final TeleportTransition.PostTeleportTransition DO_NOTHING = p_379662_ -> {};
    public static final TeleportTransition.PostTeleportTransition PLAY_PORTAL_SOUND = TeleportTransition::playPortalSound;
    public static final TeleportTransition.PostTeleportTransition PLACE_PORTAL_TICKET = TeleportTransition::placePortalTicket;

    public TeleportTransition(
        ServerLevel p_379776_, Vec3 p_379412_, Vec3 p_379320_, float p_380257_, float p_379610_, TeleportTransition.PostTeleportTransition p_380303_
    ) {
        this(p_379776_, p_379412_, p_379320_, p_380257_, p_379610_, Set.of(), p_380303_);
    }

    public TeleportTransition(
        ServerLevel p_380133_,
        Vec3 p_379861_,
        Vec3 p_380308_,
        float p_379941_,
        float p_380119_,
        Set<Relative> p_379959_,
        TeleportTransition.PostTeleportTransition p_379425_
    ) {
        this(p_380133_, p_379861_, p_380308_, p_379941_, p_380119_, false, false, p_379959_, p_379425_);
    }

    private static void playPortalSound(Entity entity) {
        if (entity instanceof ServerPlayer serverplayer) {
            serverplayer.connection.send(new ClientboundLevelEventPacket(1032, BlockPos.ZERO, 0, false));
        }
    }

    private static void placePortalTicket(Entity entity) {
        entity.placePortalTicket(BlockPos.containing(entity.position()));
    }

    public static TeleportTransition createDefault(ServerPlayer player, TeleportTransition.PostTeleportTransition postTeleportTransition) {
        ServerLevel serverlevel = player.level().getServer().findRespawnDimension();
        LevelData.RespawnData leveldata$respawndata = serverlevel.getRespawnData();
        return new TeleportTransition(
            serverlevel,
            findAdjustedSharedSpawnPos(serverlevel, player),
            Vec3.ZERO,
            leveldata$respawndata.yaw(),
            leveldata$respawndata.pitch(),
            false,
            false,
            Set.of(),
            postTeleportTransition
        );
    }

    public static TeleportTransition missingRespawnBlock(ServerPlayer player, TeleportTransition.PostTeleportTransition postTeleportTransition) {
        ServerLevel serverlevel = player.level().getServer().findRespawnDimension();
        LevelData.RespawnData leveldata$respawndata = serverlevel.getRespawnData();
        return new TeleportTransition(
            serverlevel,
            findAdjustedSharedSpawnPos(serverlevel, player),
            Vec3.ZERO,
            leveldata$respawndata.yaw(),
            leveldata$respawndata.pitch(),
            true,
            false,
            Set.of(),
            postTeleportTransition
        );
    }

    private static Vec3 findAdjustedSharedSpawnPos(ServerLevel level, Entity entity) {
        return entity.adjustSpawnLocation(level, level.getRespawnData().pos()).getBottomCenter();
    }

    public TeleportTransition withRotation(float yRot, float xRot) {
        return new TeleportTransition(
            this.newLevel(),
            this.position(),
            this.deltaMovement(),
            yRot,
            xRot,
            this.missingRespawnBlock(),
            this.asPassenger(),
            this.relatives(),
            this.postTeleportTransition()
        );
    }

    public TeleportTransition withPosition(Vec3 position) {
        return new TeleportTransition(
            this.newLevel(),
            position,
            this.deltaMovement(),
            this.yRot(),
            this.xRot(),
            this.missingRespawnBlock(),
            this.asPassenger(),
            this.relatives(),
            this.postTeleportTransition()
        );
    }

    public TeleportTransition transitionAsPassenger() {
        return new TeleportTransition(
            this.newLevel(),
            this.position(),
            this.deltaMovement(),
            this.yRot(),
            this.xRot(),
            this.missingRespawnBlock(),
            true,
            this.relatives(),
            this.postTeleportTransition()
        );
    }

    @FunctionalInterface
    public interface PostTeleportTransition {
        void onTransition(Entity entity);

        default TeleportTransition.PostTeleportTransition then(TeleportTransition.PostTeleportTransition postTeleportTransition) {
            return p_380407_ -> {
                this.onTransition(p_380407_);
                postTeleportTransition.onTransition(p_380407_);
            };
        }
    }
}
