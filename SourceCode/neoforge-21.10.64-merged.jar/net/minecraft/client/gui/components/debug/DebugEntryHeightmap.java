package net.minecraft.client.gui.components.debug;

import com.google.common.collect.Maps;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class DebugEntryHeightmap implements DebugScreenEntry {
    private static final Map<Heightmap.Types, String> HEIGHTMAP_NAMES = Maps.newEnumMap(
        Map.of(
            Heightmap.Types.WORLD_SURFACE_WG,
            "SW",
            Heightmap.Types.WORLD_SURFACE,
            "S",
            Heightmap.Types.OCEAN_FLOOR_WG,
            "OW",
            Heightmap.Types.OCEAN_FLOOR,
            "O",
            Heightmap.Types.MOTION_BLOCKING,
            "M",
            Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
            "ML"
        )
    );
    public static final ResourceLocation GROUP = ResourceLocation.withDefaultNamespace("heightmaps");

    @Override
    public void display(DebugScreenDisplayer p_434600_, @Nullable Level p_433805_, @Nullable LevelChunk p_433046_, @Nullable LevelChunk p_434738_) {
        Minecraft minecraft = Minecraft.getInstance();
        Entity entity = minecraft.getCameraEntity();
        if (entity != null && minecraft.level != null && p_433046_ != null) {
            BlockPos blockpos = entity.blockPosition();
            List<String> list = new ArrayList<>();
            StringBuilder stringbuilder = new StringBuilder("CH");

            for (Heightmap.Types heightmap$types : Heightmap.Types.values()) {
                if (heightmap$types.sendToClient()) {
                    stringbuilder.append(" ")
                        .append(HEIGHTMAP_NAMES.get(heightmap$types))
                        .append(": ")
                        .append(p_433046_.getHeight(heightmap$types, blockpos.getX(), blockpos.getZ()));
                }
            }

            list.add(stringbuilder.toString());
            stringbuilder.setLength(0);
            stringbuilder.append("SH");

            for (Heightmap.Types heightmap$types1 : Heightmap.Types.values()) {
                if (heightmap$types1.keepAfterWorldgen()) {
                    stringbuilder.append(" ").append(HEIGHTMAP_NAMES.get(heightmap$types1)).append(": ");
                    if (p_434738_ != null) {
                        stringbuilder.append(p_434738_.getHeight(heightmap$types1, blockpos.getX(), blockpos.getZ()));
                    } else {
                        stringbuilder.append("??");
                    }
                }
            }

            list.add(stringbuilder.toString());
            p_434600_.addToGroup(GROUP, list);
        }
    }
}
