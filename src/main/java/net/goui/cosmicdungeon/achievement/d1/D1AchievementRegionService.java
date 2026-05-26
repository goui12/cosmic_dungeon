package net.goui.cosmicdungeon.achievement.d1;

import net.goui.cosmicdungeon.region.RegionRegistryData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

public final class D1AchievementRegionService {
    public static final String WOODLAND_MANOR = "d1_woodland_manor";
    public static final String WITHER_ROOM = "d1_wither_room";
    public static final String CAMP_5 = "d1_camp_5";

    private D1AchievementRegionService() {}

    public static boolean inRegion(ServerLevel level, BlockPos pos, String regionName) {
        RegionRegistryData data = RegionRegistryData.get(level);
        return data.get(regionName)
                .filter(r -> r.dimensionId().equals(level.dimension().location().toString()))
                .filter(r -> data.contains(r, pos))
                .isPresent();
    }
}
