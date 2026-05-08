package net.minecraft.util.datafix.fixes;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Dynamic;
import it.unimi.dsi.fastutil.ints.Int2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap.Entry;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.util.CrudeIncrementalIntIdentityHashBiMap;
import net.minecraft.util.datafix.ExtraDataFixUtils;
import net.minecraft.util.datafix.PackedBitStorage;
import org.slf4j.Logger;

public class ChunkPalettedStorageFix extends DataFix {
    private static final int NORTH_WEST_MASK = 128;
    private static final int WEST_MASK = 64;
    private static final int SOUTH_WEST_MASK = 32;
    private static final int SOUTH_MASK = 16;
    private static final int SOUTH_EAST_MASK = 8;
    private static final int EAST_MASK = 4;
    private static final int NORTH_EAST_MASK = 2;
    private static final int NORTH_MASK = 1;
    static final Logger LOGGER = LogUtils.getLogger();
    private static final int SIZE = 4096;

    public ChunkPalettedStorageFix(Schema outputSchema, boolean changesType) {
        super(outputSchema, changesType);
    }

    public static String getName(Dynamic<?> data) {
        return data.get("Name").asString("");
    }

    public static String getProperty(Dynamic<?> data, String key) {
        return data.get("Properties").get(key).asString("");
    }

    public static int idFor(CrudeIncrementalIntIdentityHashBiMap<Dynamic<?>> palette, Dynamic<?> data) {
        int i = palette.getId(data);
        if (i == -1) {
            i = palette.add(data);
        }

        return i;
    }

    private Dynamic<?> fix(Dynamic<?> dynamic) {
        Optional<? extends Dynamic<?>> optional = dynamic.get("Level").result();
        return optional.isPresent() && optional.get().get("Sections").asStreamOpt().result().isPresent()
            ? dynamic.set("Level", new ChunkPalettedStorageFix.UpgradeChunk((Dynamic<?>)optional.get()).write())
            : dynamic;
    }

    @Override
    public TypeRewriteRule makeRule() {
        Type<?> type = this.getInputSchema().getType(References.CHUNK);
        Type<?> type1 = this.getOutputSchema().getType(References.CHUNK);
        return this.writeFixAndRead("ChunkPalettedStorageFix", type, type1, this::fix);
    }

    public static int getSideMask(boolean west, boolean east, boolean north, boolean south) {
        int i = 0;
        if (north) {
            if (east) {
                i |= 2;
            } else if (west) {
                i |= 128;
            } else {
                i |= 1;
            }
        } else if (south) {
            if (west) {
                i |= 32;
            } else if (east) {
                i |= 8;
            } else {
                i |= 16;
            }
        } else if (east) {
            i |= 4;
        } else if (west) {
            i |= 64;
        }

        return i;
    }

    static class DataLayer {
        private static final int SIZE = 2048;
        private static final int NIBBLE_SIZE = 4;
        private final byte[] data;

        public DataLayer() {
            this.data = new byte[2048];
        }

        public DataLayer(byte[] data) {
            this.data = data;
            if (data.length != 2048) {
                throw new IllegalArgumentException("ChunkNibbleArrays should be 2048 bytes not: " + data.length);
            }
        }

        public int get(int x, int y, int z) {
            int i = this.getPosition(y << 8 | z << 4 | x);
            return this.isFirst(y << 8 | z << 4 | x) ? this.data[i] & 15 : this.data[i] >> 4 & 15;
        }

        private boolean isFirst(int packedPos) {
            return (packedPos & 1) == 0;
        }

        private int getPosition(int packedPos) {
            return packedPos >> 1;
        }
    }

    public static enum Direction {
        DOWN(ChunkPalettedStorageFix.Direction.AxisDirection.NEGATIVE, ChunkPalettedStorageFix.Direction.Axis.Y),
        UP(ChunkPalettedStorageFix.Direction.AxisDirection.POSITIVE, ChunkPalettedStorageFix.Direction.Axis.Y),
        NORTH(ChunkPalettedStorageFix.Direction.AxisDirection.NEGATIVE, ChunkPalettedStorageFix.Direction.Axis.Z),
        SOUTH(ChunkPalettedStorageFix.Direction.AxisDirection.POSITIVE, ChunkPalettedStorageFix.Direction.Axis.Z),
        WEST(ChunkPalettedStorageFix.Direction.AxisDirection.NEGATIVE, ChunkPalettedStorageFix.Direction.Axis.X),
        EAST(ChunkPalettedStorageFix.Direction.AxisDirection.POSITIVE, ChunkPalettedStorageFix.Direction.Axis.X);

        private final ChunkPalettedStorageFix.Direction.Axis axis;
        private final ChunkPalettedStorageFix.Direction.AxisDirection axisDirection;

        private Direction(ChunkPalettedStorageFix.Direction.AxisDirection axisDirection, ChunkPalettedStorageFix.Direction.Axis axis) {
            this.axis = axis;
            this.axisDirection = axisDirection;
        }

        public ChunkPalettedStorageFix.Direction.AxisDirection getAxisDirection() {
            return this.axisDirection;
        }

        public ChunkPalettedStorageFix.Direction.Axis getAxis() {
            return this.axis;
        }

        public static enum Axis {
            X,
            Y,
            Z;
        }

        public static enum AxisDirection {
            POSITIVE(1),
            NEGATIVE(-1);

            private final int step;

            private AxisDirection(int step) {
                this.step = step;
            }

            public int getStep() {
                return this.step;
            }
        }
    }

    static class MappingConstants {
        static final BitSet VIRTUAL = new BitSet(256);
        static final BitSet FIX = new BitSet(256);
        static final Dynamic<?> PUMPKIN = ExtraDataFixUtils.blockState("minecraft:pumpkin");
        static final Dynamic<?> SNOWY_PODZOL = ExtraDataFixUtils.blockState("minecraft:podzol", Map.of("snowy", "true"));
        static final Dynamic<?> SNOWY_GRASS = ExtraDataFixUtils.blockState("minecraft:grass_block", Map.of("snowy", "true"));
        static final Dynamic<?> SNOWY_MYCELIUM = ExtraDataFixUtils.blockState("minecraft:mycelium", Map.of("snowy", "true"));
        static final Dynamic<?> UPPER_SUNFLOWER = ExtraDataFixUtils.blockState("minecraft:sunflower", Map.of("half", "upper"));
        static final Dynamic<?> UPPER_LILAC = ExtraDataFixUtils.blockState("minecraft:lilac", Map.of("half", "upper"));
        static final Dynamic<?> UPPER_TALL_GRASS = ExtraDataFixUtils.blockState("minecraft:tall_grass", Map.of("half", "upper"));
        static final Dynamic<?> UPPER_LARGE_FERN = ExtraDataFixUtils.blockState("minecraft:large_fern", Map.of("half", "upper"));
        static final Dynamic<?> UPPER_ROSE_BUSH = ExtraDataFixUtils.blockState("minecraft:rose_bush", Map.of("half", "upper"));
        static final Dynamic<?> UPPER_PEONY = ExtraDataFixUtils.blockState("minecraft:peony", Map.of("half", "upper"));
        static final Map<String, Dynamic<?>> FLOWER_POT_MAP = DataFixUtils.make(Maps.newHashMap(), p_363273_ -> {
            p_363273_.put("minecraft:air0", ExtraDataFixUtils.blockState("minecraft:flower_pot"));
            p_363273_.put("minecraft:red_flower0", ExtraDataFixUtils.blockState("minecraft:potted_poppy"));
            p_363273_.put("minecraft:red_flower1", ExtraDataFixUtils.blockState("minecraft:potted_blue_orchid"));
            p_363273_.put("minecraft:red_flower2", ExtraDataFixUtils.blockState("minecraft:potted_allium"));
            p_363273_.put("minecraft:red_flower3", ExtraDataFixUtils.blockState("minecraft:potted_azure_bluet"));
            p_363273_.put("minecraft:red_flower4", ExtraDataFixUtils.blockState("minecraft:potted_red_tulip"));
            p_363273_.put("minecraft:red_flower5", ExtraDataFixUtils.blockState("minecraft:potted_orange_tulip"));
            p_363273_.put("minecraft:red_flower6", ExtraDataFixUtils.blockState("minecraft:potted_white_tulip"));
            p_363273_.put("minecraft:red_flower7", ExtraDataFixUtils.blockState("minecraft:potted_pink_tulip"));
            p_363273_.put("minecraft:red_flower8", ExtraDataFixUtils.blockState("minecraft:potted_oxeye_daisy"));
            p_363273_.put("minecraft:yellow_flower0", ExtraDataFixUtils.blockState("minecraft:potted_dandelion"));
            p_363273_.put("minecraft:sapling0", ExtraDataFixUtils.blockState("minecraft:potted_oak_sapling"));
            p_363273_.put("minecraft:sapling1", ExtraDataFixUtils.blockState("minecraft:potted_spruce_sapling"));
            p_363273_.put("minecraft:sapling2", ExtraDataFixUtils.blockState("minecraft:potted_birch_sapling"));
            p_363273_.put("minecraft:sapling3", ExtraDataFixUtils.blockState("minecraft:potted_jungle_sapling"));
            p_363273_.put("minecraft:sapling4", ExtraDataFixUtils.blockState("minecraft:potted_acacia_sapling"));
            p_363273_.put("minecraft:sapling5", ExtraDataFixUtils.blockState("minecraft:potted_dark_oak_sapling"));
            p_363273_.put("minecraft:red_mushroom0", ExtraDataFixUtils.blockState("minecraft:potted_red_mushroom"));
            p_363273_.put("minecraft:brown_mushroom0", ExtraDataFixUtils.blockState("minecraft:potted_brown_mushroom"));
            p_363273_.put("minecraft:deadbush0", ExtraDataFixUtils.blockState("minecraft:potted_dead_bush"));
            p_363273_.put("minecraft:tallgrass2", ExtraDataFixUtils.blockState("minecraft:potted_fern"));
            p_363273_.put("minecraft:cactus0", ExtraDataFixUtils.blockState("minecraft:potted_cactus"));
        });
        static final Map<String, Dynamic<?>> SKULL_MAP = DataFixUtils.make(Maps.newHashMap(), p_363674_ -> {
            mapSkull(p_363674_, 0, "skeleton", "skull");
            mapSkull(p_363674_, 1, "wither_skeleton", "skull");
            mapSkull(p_363674_, 2, "zombie", "head");
            mapSkull(p_363674_, 3, "player", "head");
            mapSkull(p_363674_, 4, "creeper", "head");
            mapSkull(p_363674_, 5, "dragon", "head");
        });
        static final Map<String, Dynamic<?>> DOOR_MAP = DataFixUtils.make(Maps.newHashMap(), p_363442_ -> {
            mapDoor(p_363442_, "oak_door");
            mapDoor(p_363442_, "iron_door");
            mapDoor(p_363442_, "spruce_door");
            mapDoor(p_363442_, "birch_door");
            mapDoor(p_363442_, "jungle_door");
            mapDoor(p_363442_, "acacia_door");
            mapDoor(p_363442_, "dark_oak_door");
        });
        static final Map<String, Dynamic<?>> NOTE_BLOCK_MAP = DataFixUtils.make(Maps.newHashMap(), p_361924_ -> {
            for (int i = 0; i < 26; i++) {
                p_361924_.put("true" + i, ExtraDataFixUtils.blockState("minecraft:note_block", Map.of("powered", "true", "note", String.valueOf(i))));
                p_361924_.put("false" + i, ExtraDataFixUtils.blockState("minecraft:note_block", Map.of("powered", "false", "note", String.valueOf(i))));
            }
        });
        private static final Int2ObjectMap<String> DYE_COLOR_MAP = DataFixUtils.make(new Int2ObjectOpenHashMap<>(), p_360500_ -> {
            p_360500_.put(0, "white");
            p_360500_.put(1, "orange");
            p_360500_.put(2, "magenta");
            p_360500_.put(3, "light_blue");
            p_360500_.put(4, "yellow");
            p_360500_.put(5, "lime");
            p_360500_.put(6, "pink");
            p_360500_.put(7, "gray");
            p_360500_.put(8, "light_gray");
            p_360500_.put(9, "cyan");
            p_360500_.put(10, "purple");
            p_360500_.put(11, "blue");
            p_360500_.put(12, "brown");
            p_360500_.put(13, "green");
            p_360500_.put(14, "red");
            p_360500_.put(15, "black");
        });
        static final Map<String, Dynamic<?>> BED_BLOCK_MAP = DataFixUtils.make(Maps.newHashMap(), p_361617_ -> {
            for (Entry<String> entry : DYE_COLOR_MAP.int2ObjectEntrySet()) {
                if (!Objects.equals(entry.getValue(), "red")) {
                    addBeds(p_361617_, entry.getIntKey(), entry.getValue());
                }
            }
        });
        static final Map<String, Dynamic<?>> BANNER_BLOCK_MAP = DataFixUtils.make(Maps.newHashMap(), p_364554_ -> {
            for (Entry<String> entry : DYE_COLOR_MAP.int2ObjectEntrySet()) {
                if (!Objects.equals(entry.getValue(), "white")) {
                    addBanners(p_364554_, 15 - entry.getIntKey(), entry.getValue());
                }
            }
        });
        static final Dynamic<?> AIR = ExtraDataFixUtils.blockState("minecraft:air");

        private MappingConstants() {
        }

        private static void mapSkull(Map<String, Dynamic<?>> map, int id, String skullType, String suffix) {
            map.put(id + "north", ExtraDataFixUtils.blockState("minecraft:" + skullType + "_wall_" + suffix, Map.of("facing", "north")));
            map.put(id + "east", ExtraDataFixUtils.blockState("minecraft:" + skullType + "_wall_" + suffix, Map.of("facing", "east")));
            map.put(id + "south", ExtraDataFixUtils.blockState("minecraft:" + skullType + "_wall_" + suffix, Map.of("facing", "south")));
            map.put(id + "west", ExtraDataFixUtils.blockState("minecraft:" + skullType + "_wall_" + suffix, Map.of("facing", "west")));

            for (int i = 0; i < 16; i++) {
                map.put(
                    "" + id + i, ExtraDataFixUtils.blockState("minecraft:" + skullType + "_" + suffix, Map.of("rotation", String.valueOf(i)))
                );
            }
        }

        private static void mapDoor(Map<String, Dynamic<?>> map, String doorId) {
            String s = "minecraft:" + doorId;
            map.put(
                "minecraft:" + doorId + "eastlowerleftfalsefalse",
                ExtraDataFixUtils.blockState(s, Map.of("facing", "east", "half", "lower", "hinge", "left", "open", "false", "powered", "false"))
            );
            map.put(
                "minecraft:" + doorId + "eastlowerleftfalsetrue",
                ExtraDataFixUtils.blockState(s, Map.of("facing", "east", "half", "lower", "hinge", "left", "open", "false", "powered", "true"))
            );
            map.put(
                "minecraft:" + doorId + "eastlowerlefttruefalse",
                ExtraDataFixUtils.blockState(s, Map.of("facing", "east", "half", "lower", "hinge", "left", "open", "true", "powered", "false"))
            );
            map.put(
                "minecraft:" + doorId + "eastlowerlefttruetrue",
                ExtraDataFixUtils.blockState(s, Map.of("facing", "east", "half", "lower", "hinge", "left", "open", "true", "powered", "true"))
            );
            map.put(
                "minecraft:" + doorId + "eastlowerrightfalsefalse",
                ExtraDataFixUtils.blockState(s, Map.of("facing", "east", "half", "lower", "hinge", "right", "open", "false", "powered", "false"))
            );
            map.put(
                "minecraft:" + doorId + "eastlowerrightfalsetrue",
                ExtraDataFixUtils.blockState(s, Map.of("facing", "east", "half", "lower", "hinge", "right", "open", "false", "powered", "true"))
            );
            map.put(
                "minecraft:" + doorId + "eastlowerrighttruefalse",
                ExtraDataFixUtils.blockState(s, Map.of("facing", "east", "half", "lower", "hinge", "right", "open", "true", "powered", "false"))
            );
            map.put(
                "minecraft:" + doorId + "eastlowerrighttruetrue",
                ExtraDataFixUtils.blockState(s, Map.of("facing", "east", "half", "lower", "hinge", "right", "open", "true", "powered", "true"))
            );
            map.put(
                "minecraft:" + doorId + "eastupperleftfalsefalse",
                ExtraDataFixUtils.blockState(s, Map.of("facing", "east", "half", "upper", "hinge", "left", "open", "false", "powered", "false"))
            );
            map.put(
                "minecraft:" + doorId + "eastupperleftfalsetrue",
                ExtraDataFixUtils.blockState(s, Map.of("facing", "east", "half", "upper", "hinge", "left", "open", "false", "powered", "true"))
            );
            map.put(
                "minecraft:" + doorId + "eastupperlefttruefalse",
                ExtraDataFixUtils.blockState(s, Map.of("facing", "east", "half", "upper", "hinge", "left", "open", "true", "powered", "false"))
            );
            map.put(
                "minecraft:" + doorId + "eastupperlefttruetrue",
                ExtraDataFixUtils.blockState(s, Map.of("facing", "east", "half", "upper", "hinge", "left", "open", "true", "powered", "true"))
            );
            map.put(
                "minecraft:" + doorId + "eastupperrightfalsefalse",
                ExtraDataFixUtils.blockState(s, Map.of("facing", "east", "half", "upper", "hinge", "right", "open", "false", "powered", "false"))
            );
            map.put(
                "minecraft:" + doorId + "eastupperrightfalsetrue",
                ExtraDataFixUtils.blockState(s, Map.of("facing", "east", "half", "upper", "hinge", "right", "open", "false", "powered", "true"))
            );
            map.put(
                "minecraft:" + doorId + "eastupperrighttruefalse",
                ExtraDataFixUtils.blockState(s, Map.of("facing", "east", "half", "upper", "hinge", "right", "open", "true", "powered", "false"))
            );
            map.put(
                "minecraft:" + doorId + "eastupperrighttruetrue",
                ExtraDataFixUtils.blockState(s, Map.of("facing", "east", "half", "upper", "hinge", "right", "open", "true", "powered", "true"))
            );
            map.put(
                "minecraft:" + doorId + "northlowerleftfalsefalse",
                ExtraDataFixUtils.blockState(s, Map.of("facing", "north", "half", "lower", "hinge", "left", "open", "false", "powered", "false"))
            );
            map.put(
                "minecraft:" + doorId + "northlowerleftfalsetrue",
                ExtraDataFixUtils.blockState(s, Map.of("facing", "north", "half", "lower", "hinge", "left", "open", "false", "powered", "true"))
            );
            map.put(
                "minecraft:" + doorId + "northlowerlefttruefalse",
                ExtraDataFixUtils.blockState(s, Map.of("facing", "north", "half", "lower", "hinge", "left", "open", "true", "powered", "false"))
            );
            map.put(
                "minecraft:" + doorId + "northlowerlefttruetrue",
                ExtraDataFixUtils.blockState(s, Map.of("facing", "north", "half", "lower", "hinge", "left", "open", "true", "powered", "true"))
            );
            map.put(
                "minecraft:" + doorId + "northlowerrightfalsefalse",
                ExtraDataFixUtils.blockState(s, Map.of("facing", "north", "half", "lower", "hinge", "right", "open", "false", "powered", "false"))
            );
            map.put(
                "minecraft:" + doorId + "northlowerrightfalsetrue",
                ExtraDataFixUtils.blockState(s, Map.of("facing", "north", "half", "lower", "hinge", "right", "open", "false", "powered", "true"))
            );
            map.put(
                "minecraft:" + doorId + "northlowerrighttruefalse",
                ExtraDataFixUtils.blockState(s, Map.of("facing", "north", "half", "lower", "hinge", "right", "open", "true", "powered", "false"))
            );
            map.put(
                "minecraft:" + doorId + "northlowerrighttruetrue",
                ExtraDataFixUtils.blockState(s, Map.of("facing", "north", "half", "lower", "hinge", "right", "open", "true", "powered", "true"))
            );
            map.put(
                "minecraft:" + doorId + "northupperleftfalsefalse",
                ExtraDataFixUtils.blockState(s, Map.of("facing", "north", "half", "upper", "hinge", "left", "open", "false", "powered", "false"))
            );
            map.put(
                "minecraft:" + doorId + "northupperleftfalsetrue",
                ExtraDataFixUtils.blockState(s, Map.of("facing", "north", "half", "upper", "hinge", "left", "open", "false", "powered", "true"))
            );
            map.put(
                "minecraft:" + doorId + "northupperlefttruefalse",
                ExtraDataFixUtils.blockState(s, Map.of("facing", "north", "half", "upper", "hinge", "left", "open", "true", "powered", "false"))
            );
            map.put(
                "minecraft:" + doorId + "northupperlefttruetrue",
                ExtraDataFixUtils.blockState(s, Map.of("facing", "north", "half", "upper", "hinge", "left", "open", "true", "powered", "true"))
            );
            map.put(
                "minecraft:" + doorId + "northupperrightfalsefalse",
                ExtraDataFixUtils.blockState(s, Map.of("facing", "north", "half", "upper", "hinge", "right", "open", "false", "powered", "false"))
            );
            map.put(
                "minecraft:" + doorId + "northupperrightfalsetrue",
                ExtraDataFixUtils.blockState(s, Map.of("facing", "north", "half", "upper", "hinge", "right", "open", "false", "powered", "true"))
            );
            map.put(
                "minecraft:" + doorId + "northupperrighttruefalse",
                ExtraDataFixUtils.blockState(s, Map.of("facing", "north", "half", "upper", "hinge", "right", "open", "true", "powered", "false"))
            );
            map.put(
                "minecraft:" + doorId + "northupperrighttruetrue",
                ExtraDataFixUtils.blockState(s, Map.of("facing", "north", "half", "upper", "hinge", "right", "open", "true", "powered", "true"))
            );
            map.put(
                "minecraft:" + doorId + "southlowerleftfalsefalse",
                ExtraDataFixUtils.blockState(s, Map.of("facing", "south", "half", "lower", "hinge", "left", "open", "false", "powered", "false"))
            );
            map.put(
                "minecraft:" + doorId + "southlowerleftfalsetrue",
                ExtraDataFixUtils.blockState(s, Map.of("facing", "south", "half", "lower", "hinge", "left", "open", "false", "powered", "true"))
            );
            map.put(
                "minecraft:" + doorId + "southlowerlefttruefalse",
                ExtraDataFixUtils.blockState(s, Map.of("facing", "south", "half", "lower", "hinge", "left", "open", "true", "powered", "false"))
            );
            map.put(
                "minecraft:" + doorId + "southlowerlefttruetrue",
                ExtraDataFixUtils.blockState(s, Map.of("facing", "south", "half", "lower", "hinge", "left", "open", "true", "powered", "true"))
            );
            map.put(
                "minecraft:" + doorId + "southlowerrightfalsefalse",
                ExtraDataFixUtils.blockState(s, Map.of("facing", "south", "half", "lower", "hinge", "right", "open", "false", "powered", "false"))
            );
            map.put(
                "minecraft:" + doorId + "southlowerrightfalsetrue",
                ExtraDataFixUtils.blockState(s, Map.of("facing", "south", "half", "lower", "hinge", "right", "open", "false", "powered", "true"))
            );
            map.put(
                "minecraft:" + doorId + "southlowerrighttruefalse",
                ExtraDataFixUtils.blockState(s, Map.of("facing", "south", "half", "lower", "hinge", "right", "open", "true", "powered", "false"))
            );
            map.put(
                "minecraft:" + doorId + "southlowerrighttruetrue",
                ExtraDataFixUtils.blockState(s, Map.of("facing", "south", "half", "lower", "hinge", "right", "open", "true", "powered", "true"))
            );
            map.put(
                "minecraft:" + doorId + "southupperleftfalsefalse",
                ExtraDataFixUtils.blockState(s, Map.of("facing", "south", "half", "upper", "hinge", "left", "open", "false", "powered", "false"))
            );
            map.put(
                "minecraft:" + doorId + "southupperleftfalsetrue",
                ExtraDataFixUtils.blockState(s, Map.of("facing", "south", "half", "upper", "hinge", "left", "open", "false", "powered", "true"))
            );
            map.put(
                "minecraft:" + doorId + "southupperlefttruefalse",
                ExtraDataFixUtils.blockState(s, Map.of("facing", "south", "half", "upper", "hinge", "left", "open", "true", "powered", "false"))
            );
            map.put(
                "minecraft:" + doorId + "southupperlefttruetrue",
                ExtraDataFixUtils.blockState(s, Map.of("facing", "south", "half", "upper", "hinge", "left", "open", "true", "powered", "true"))
            );
            map.put(
                "minecraft:" + doorId + "southupperrightfalsefalse",
                ExtraDataFixUtils.blockState(s, Map.of("facing", "south", "half", "upper", "hinge", "right", "open", "false", "powered", "false"))
            );
            map.put(
                "minecraft:" + doorId + "southupperrightfalsetrue",
                ExtraDataFixUtils.blockState(s, Map.of("facing", "south", "half", "upper", "hinge", "right", "open", "false", "powered", "true"))
            );
            map.put(
                "minecraft:" + doorId + "southupperrighttruefalse",
                ExtraDataFixUtils.blockState(s, Map.of("facing", "south", "half", "upper", "hinge", "right", "open", "true", "powered", "false"))
            );
            map.put(
                "minecraft:" + doorId + "southupperrighttruetrue",
                ExtraDataFixUtils.blockState(s, Map.of("facing", "south", "half", "upper", "hinge", "right", "open", "true", "powered", "true"))
            );
            map.put(
                "minecraft:" + doorId + "westlowerleftfalsefalse",
                ExtraDataFixUtils.blockState(s, Map.of("facing", "west", "half", "lower", "hinge", "left", "open", "false", "powered", "false"))
            );
            map.put(
                "minecraft:" + doorId + "westlowerleftfalsetrue",
                ExtraDataFixUtils.blockState(s, Map.of("facing", "west", "half", "lower", "hinge", "left", "open", "false", "powered", "true"))
            );
            map.put(
                "minecraft:" + doorId + "westlowerlefttruefalse",
                ExtraDataFixUtils.blockState(s, Map.of("facing", "west", "half", "lower", "hinge", "left", "open", "true", "powered", "false"))
            );
            map.put(
                "minecraft:" + doorId + "westlowerlefttruetrue",
                ExtraDataFixUtils.blockState(s, Map.of("facing", "west", "half", "lower", "hinge", "left", "open", "true", "powered", "true"))
            );
            map.put(
                "minecraft:" + doorId + "westlowerrightfalsefalse",
                ExtraDataFixUtils.blockState(s, Map.of("facing", "west", "half", "lower", "hinge", "right", "open", "false", "powered", "false"))
            );
            map.put(
                "minecraft:" + doorId + "westlowerrightfalsetrue",
                ExtraDataFixUtils.blockState(s, Map.of("facing", "west", "half", "lower", "hinge", "right", "open", "false", "powered", "true"))
            );
            map.put(
                "minecraft:" + doorId + "westlowerrighttruefalse",
                ExtraDataFixUtils.blockState(s, Map.of("facing", "west", "half", "lower", "hinge", "right", "open", "true", "powered", "false"))
            );
            map.put(
                "minecraft:" + doorId + "westlowerrighttruetrue",
                ExtraDataFixUtils.blockState(s, Map.of("facing", "west", "half", "lower", "hinge", "right", "open", "true", "powered", "true"))
            );
            map.put(
                "minecraft:" + doorId + "westupperleftfalsefalse",
                ExtraDataFixUtils.blockState(s, Map.of("facing", "west", "half", "upper", "hinge", "left", "open", "false", "powered", "false"))
            );
            map.put(
                "minecraft:" + doorId + "westupperleftfalsetrue",
                ExtraDataFixUtils.blockState(s, Map.of("facing", "west", "half", "upper", "hinge", "left", "open", "false", "powered", "true"))
            );
            map.put(
                "minecraft:" + doorId + "westupperlefttruefalse",
                ExtraDataFixUtils.blockState(s, Map.of("facing", "west", "half", "upper", "hinge", "left", "open", "true", "powered", "false"))
            );
            map.put(
                "minecraft:" + doorId + "westupperlefttruetrue",
                ExtraDataFixUtils.blockState(s, Map.of("facing", "west", "half", "upper", "hinge", "left", "open", "true", "powered", "true"))
            );
            map.put(
                "minecraft:" + doorId + "westupperrightfalsefalse",
                ExtraDataFixUtils.blockState(s, Map.of("facing", "west", "half", "upper", "hinge", "right", "open", "false", "powered", "false"))
            );
            map.put(
                "minecraft:" + doorId + "westupperrightfalsetrue",
                ExtraDataFixUtils.blockState(s, Map.of("facing", "west", "half", "upper", "hinge", "right", "open", "false", "powered", "true"))
            );
            map.put(
                "minecraft:" + doorId + "westupperrighttruefalse",
                ExtraDataFixUtils.blockState(s, Map.of("facing", "west", "half", "upper", "hinge", "right", "open", "true", "powered", "false"))
            );
            map.put(
                "minecraft:" + doorId + "westupperrighttruetrue",
                ExtraDataFixUtils.blockState(s, Map.of("facing", "west", "half", "upper", "hinge", "right", "open", "true", "powered", "true"))
            );
        }

        private static void addBeds(Map<String, Dynamic<?>> map, int id, String bedColor) {
            map.put(
                "southfalsefoot" + id,
                ExtraDataFixUtils.blockState("minecraft:" + bedColor + "_bed", Map.of("facing", "south", "occupied", "false", "part", "foot"))
            );
            map.put(
                "westfalsefoot" + id,
                ExtraDataFixUtils.blockState("minecraft:" + bedColor + "_bed", Map.of("facing", "west", "occupied", "false", "part", "foot"))
            );
            map.put(
                "northfalsefoot" + id,
                ExtraDataFixUtils.blockState("minecraft:" + bedColor + "_bed", Map.of("facing", "north", "occupied", "false", "part", "foot"))
            );
            map.put(
                "eastfalsefoot" + id,
                ExtraDataFixUtils.blockState("minecraft:" + bedColor + "_bed", Map.of("facing", "east", "occupied", "false", "part", "foot"))
            );
            map.put(
                "southfalsehead" + id,
                ExtraDataFixUtils.blockState("minecraft:" + bedColor + "_bed", Map.of("facing", "south", "occupied", "false", "part", "head"))
            );
            map.put(
                "westfalsehead" + id,
                ExtraDataFixUtils.blockState("minecraft:" + bedColor + "_bed", Map.of("facing", "west", "occupied", "false", "part", "head"))
            );
            map.put(
                "northfalsehead" + id,
                ExtraDataFixUtils.blockState("minecraft:" + bedColor + "_bed", Map.of("facing", "north", "occupied", "false", "part", "head"))
            );
            map.put(
                "eastfalsehead" + id,
                ExtraDataFixUtils.blockState("minecraft:" + bedColor + "_bed", Map.of("facing", "east", "occupied", "false", "part", "head"))
            );
            map.put(
                "southtruehead" + id,
                ExtraDataFixUtils.blockState("minecraft:" + bedColor + "_bed", Map.of("facing", "south", "occupied", "true", "part", "head"))
            );
            map.put(
                "westtruehead" + id,
                ExtraDataFixUtils.blockState("minecraft:" + bedColor + "_bed", Map.of("facing", "west", "occupied", "true", "part", "head"))
            );
            map.put(
                "northtruehead" + id,
                ExtraDataFixUtils.blockState("minecraft:" + bedColor + "_bed", Map.of("facing", "north", "occupied", "true", "part", "head"))
            );
            map.put(
                "easttruehead" + id,
                ExtraDataFixUtils.blockState("minecraft:" + bedColor + "_bed", Map.of("facing", "east", "occupied", "true", "part", "head"))
            );
        }

        private static void addBanners(Map<String, Dynamic<?>> map, int id, String bannerColor) {
            for (int i = 0; i < 16; i++) {
                map.put(i + "_" + id, ExtraDataFixUtils.blockState("minecraft:" + bannerColor + "_banner", Map.of("rotation", String.valueOf(i))));
            }

            map.put("north_" + id, ExtraDataFixUtils.blockState("minecraft:" + bannerColor + "_wall_banner", Map.of("facing", "north")));
            map.put("south_" + id, ExtraDataFixUtils.blockState("minecraft:" + bannerColor + "_wall_banner", Map.of("facing", "south")));
            map.put("west_" + id, ExtraDataFixUtils.blockState("minecraft:" + bannerColor + "_wall_banner", Map.of("facing", "west")));
            map.put("east_" + id, ExtraDataFixUtils.blockState("minecraft:" + bannerColor + "_wall_banner", Map.of("facing", "east")));
        }

        static {
            FIX.set(2);
            FIX.set(3);
            FIX.set(110);
            FIX.set(140);
            FIX.set(144);
            FIX.set(25);
            FIX.set(86);
            FIX.set(26);
            FIX.set(176);
            FIX.set(177);
            FIX.set(175);
            FIX.set(64);
            FIX.set(71);
            FIX.set(193);
            FIX.set(194);
            FIX.set(195);
            FIX.set(196);
            FIX.set(197);
            VIRTUAL.set(54);
            VIRTUAL.set(146);
            VIRTUAL.set(25);
            VIRTUAL.set(26);
            VIRTUAL.set(51);
            VIRTUAL.set(53);
            VIRTUAL.set(67);
            VIRTUAL.set(108);
            VIRTUAL.set(109);
            VIRTUAL.set(114);
            VIRTUAL.set(128);
            VIRTUAL.set(134);
            VIRTUAL.set(135);
            VIRTUAL.set(136);
            VIRTUAL.set(156);
            VIRTUAL.set(163);
            VIRTUAL.set(164);
            VIRTUAL.set(180);
            VIRTUAL.set(203);
            VIRTUAL.set(55);
            VIRTUAL.set(85);
            VIRTUAL.set(113);
            VIRTUAL.set(188);
            VIRTUAL.set(189);
            VIRTUAL.set(190);
            VIRTUAL.set(191);
            VIRTUAL.set(192);
            VIRTUAL.set(93);
            VIRTUAL.set(94);
            VIRTUAL.set(101);
            VIRTUAL.set(102);
            VIRTUAL.set(160);
            VIRTUAL.set(106);
            VIRTUAL.set(107);
            VIRTUAL.set(183);
            VIRTUAL.set(184);
            VIRTUAL.set(185);
            VIRTUAL.set(186);
            VIRTUAL.set(187);
            VIRTUAL.set(132);
            VIRTUAL.set(139);
            VIRTUAL.set(199);
        }
    }

    static class Section {
        private final CrudeIncrementalIntIdentityHashBiMap<Dynamic<?>> palette = CrudeIncrementalIntIdentityHashBiMap.create(32);
        private final List<Dynamic<?>> listTag;
        private final Dynamic<?> section;
        private final boolean hasData;
        final Int2ObjectMap<IntList> toFix = new Int2ObjectLinkedOpenHashMap<>();
        final IntList update = new IntArrayList();
        public final int y;
        private final Set<Dynamic<?>> seen = Sets.newIdentityHashSet();
        private final int[] buffer = new int[4096];

        public Section(Dynamic<?> section) {
            this.listTag = Lists.newArrayList();
            this.section = section;
            this.y = section.get("Y").asInt(0);
            this.hasData = section.get("Blocks").result().isPresent();
        }

        public Dynamic<?> getBlock(int index) {
            if (index >= 0 && index <= 4095) {
                Dynamic<?> dynamic = this.palette.byId(this.buffer[index]);
                return dynamic == null ? ChunkPalettedStorageFix.MappingConstants.AIR : dynamic;
            } else {
                return ChunkPalettedStorageFix.MappingConstants.AIR;
            }
        }

        public void setBlock(int index, Dynamic<?> block) {
            if (this.seen.add(block)) {
                this.listTag.add("%%FILTER_ME%%".equals(ChunkPalettedStorageFix.getName(block)) ? ChunkPalettedStorageFix.MappingConstants.AIR : block);
            }

            this.buffer[index] = ChunkPalettedStorageFix.idFor(this.palette, block);
        }

        public int upgrade(int sides) {
            if (!this.hasData) {
                return sides;
            } else {
                ByteBuffer bytebuffer = this.section.get("Blocks").asByteBufferOpt().result().get();
                ChunkPalettedStorageFix.DataLayer chunkpalettedstoragefix$datalayer = this.section
                    .get("Data")
                    .asByteBufferOpt()
                    .map(p_15214_ -> new ChunkPalettedStorageFix.DataLayer(DataFixUtils.toArray(p_15214_)))
                    .result()
                    .orElseGet(ChunkPalettedStorageFix.DataLayer::new);
                ChunkPalettedStorageFix.DataLayer chunkpalettedstoragefix$datalayer1 = this.section
                    .get("Add")
                    .asByteBufferOpt()
                    .map(p_15208_ -> new ChunkPalettedStorageFix.DataLayer(DataFixUtils.toArray(p_15208_)))
                    .result()
                    .orElseGet(ChunkPalettedStorageFix.DataLayer::new);
                this.seen.add(ChunkPalettedStorageFix.MappingConstants.AIR);
                ChunkPalettedStorageFix.idFor(this.palette, ChunkPalettedStorageFix.MappingConstants.AIR);
                this.listTag.add(ChunkPalettedStorageFix.MappingConstants.AIR);

                for (int i = 0; i < 4096; i++) {
                    int j = i & 15;
                    int k = i >> 8 & 15;
                    int l = i >> 4 & 15;
                    int i1 = chunkpalettedstoragefix$datalayer1.get(j, k, l) << 12
                        | (bytebuffer.get(i) & 255) << 4
                        | chunkpalettedstoragefix$datalayer.get(j, k, l);
                    if (ChunkPalettedStorageFix.MappingConstants.FIX.get(i1 >> 4)) {
                        this.addFix(i1 >> 4, i);
                    }

                    if (ChunkPalettedStorageFix.MappingConstants.VIRTUAL.get(i1 >> 4)) {
                        int j1 = ChunkPalettedStorageFix.getSideMask(j == 0, j == 15, l == 0, l == 15);
                        if (j1 == 0) {
                            this.update.add(i);
                        } else {
                            sides |= j1;
                        }
                    }

                    this.setBlock(i, BlockStateData.getTag(i1));
                }

                return sides;
            }
        }

        private void addFix(int index, int value) {
            IntList intlist = this.toFix.get(index);
            if (intlist == null) {
                intlist = new IntArrayList();
                this.toFix.put(index, intlist);
            }

            intlist.add(value);
        }

        public Dynamic<?> write() {
            Dynamic<?> dynamic = this.section;
            if (!this.hasData) {
                return dynamic;
            } else {
                dynamic = dynamic.set("Palette", dynamic.createList(this.listTag.stream()));
                int i = Math.max(4, DataFixUtils.ceillog2(this.seen.size()));
                PackedBitStorage packedbitstorage = new PackedBitStorage(i, 4096);

                for (int j = 0; j < this.buffer.length; j++) {
                    packedbitstorage.set(j, this.buffer[j]);
                }

                dynamic = dynamic.set("BlockStates", dynamic.createLongList(Arrays.stream(packedbitstorage.getRaw())));
                dynamic = dynamic.remove("Blocks");
                dynamic = dynamic.remove("Data");
                return dynamic.remove("Add");
            }
        }
    }

    static final class UpgradeChunk {
        private int sides;
        private final ChunkPalettedStorageFix.Section[] sections = new ChunkPalettedStorageFix.Section[16];
        private final Dynamic<?> level;
        private final int x;
        private final int z;
        private final Int2ObjectMap<Dynamic<?>> blockEntities = new Int2ObjectLinkedOpenHashMap<>(16);

        public UpgradeChunk(Dynamic<?> level) {
            this.level = level;
            this.x = level.get("xPos").asInt(0) << 4;
            this.z = level.get("zPos").asInt(0) << 4;
            level.get("TileEntities").asStreamOpt().ifSuccess(p_15241_ -> p_15241_.forEach(p_145228_ -> {
                int l3 = p_145228_.get("x").asInt(0) - this.x & 15;
                int i4 = p_145228_.get("y").asInt(0);
                int j4 = p_145228_.get("z").asInt(0) - this.z & 15;
                int k4 = i4 << 8 | j4 << 4 | l3;
                if (this.blockEntities.put(k4, (Dynamic<?>)p_145228_) != null) {
                    ChunkPalettedStorageFix.LOGGER.warn("In chunk: {}x{} found a duplicate block entity at position: [{}, {}, {}]", this.x, this.z, l3, i4, j4);
                }
            }));
            boolean flag = level.get("convertedFromAlphaFormat").asBoolean(false);
            level.get("Sections").asStreamOpt().ifSuccess(p_15235_ -> p_15235_.forEach(p_145226_ -> {
                ChunkPalettedStorageFix.Section chunkpalettedstoragefix$section1 = new ChunkPalettedStorageFix.Section((Dynamic<?>)p_145226_);
                this.sides = chunkpalettedstoragefix$section1.upgrade(this.sides);
                this.sections[chunkpalettedstoragefix$section1.y] = chunkpalettedstoragefix$section1;
            }));

            for (ChunkPalettedStorageFix.Section chunkpalettedstoragefix$section : this.sections) {
                if (chunkpalettedstoragefix$section != null) {
                    for (Entry<IntList> entry : chunkpalettedstoragefix$section.toFix.int2ObjectEntrySet()) {
                        int i = chunkpalettedstoragefix$section.y << 12;
                        switch (entry.getIntKey()) {
                            case 2:
                                for (int i3 : entry.getValue()) {
                                    i3 |= i;
                                    Dynamic<?> dynamic11 = this.getBlock(i3);
                                    if ("minecraft:grass_block".equals(ChunkPalettedStorageFix.getName(dynamic11))) {
                                        String s11 = ChunkPalettedStorageFix.getName(this.getBlock(relative(i3, ChunkPalettedStorageFix.Direction.UP)));
                                        if ("minecraft:snow".equals(s11) || "minecraft:snow_layer".equals(s11)) {
                                            this.setBlock(i3, ChunkPalettedStorageFix.MappingConstants.SNOWY_GRASS);
                                        }
                                    }
                                }
                                break;
                            case 3:
                                for (int l2 : entry.getValue()) {
                                    l2 |= i;
                                    Dynamic<?> dynamic10 = this.getBlock(l2);
                                    if ("minecraft:podzol".equals(ChunkPalettedStorageFix.getName(dynamic10))) {
                                        String s10 = ChunkPalettedStorageFix.getName(this.getBlock(relative(l2, ChunkPalettedStorageFix.Direction.UP)));
                                        if ("minecraft:snow".equals(s10) || "minecraft:snow_layer".equals(s10)) {
                                            this.setBlock(l2, ChunkPalettedStorageFix.MappingConstants.SNOWY_PODZOL);
                                        }
                                    }
                                }
                                break;
                            case 25:
                                for (int k2 : entry.getValue()) {
                                    k2 |= i;
                                    Dynamic<?> dynamic9 = this.removeBlockEntity(k2);
                                    if (dynamic9 != null) {
                                        String s9 = Boolean.toString(dynamic9.get("powered").asBoolean(false))
                                            + (byte)Math.min(Math.max(dynamic9.get("note").asInt(0), 0), 24);
                                        this.setBlock(
                                            k2,
                                            ChunkPalettedStorageFix.MappingConstants.NOTE_BLOCK_MAP
                                                .getOrDefault(s9, ChunkPalettedStorageFix.MappingConstants.NOTE_BLOCK_MAP.get("false0"))
                                        );
                                    }
                                }
                                break;
                            case 26:
                                for (int j2 : entry.getValue()) {
                                    j2 |= i;
                                    Dynamic<?> dynamic8 = this.getBlockEntity(j2);
                                    Dynamic<?> dynamic14 = this.getBlock(j2);
                                    if (dynamic8 != null) {
                                        int k3 = dynamic8.get("color").asInt(0);
                                        if (k3 != 14 && k3 >= 0 && k3 < 16) {
                                            String s15 = ChunkPalettedStorageFix.getProperty(dynamic14, "facing")
                                                + ChunkPalettedStorageFix.getProperty(dynamic14, "occupied")
                                                + ChunkPalettedStorageFix.getProperty(dynamic14, "part")
                                                + k3;
                                            if (ChunkPalettedStorageFix.MappingConstants.BED_BLOCK_MAP.containsKey(s15)) {
                                                this.setBlock(j2, ChunkPalettedStorageFix.MappingConstants.BED_BLOCK_MAP.get(s15));
                                            }
                                        }
                                    }
                                }
                                break;
                            case 64:
                            case 71:
                            case 193:
                            case 194:
                            case 195:
                            case 196:
                            case 197:
                                for (int i2 : entry.getValue()) {
                                    i2 |= i;
                                    Dynamic<?> dynamic7 = this.getBlock(i2);
                                    if (ChunkPalettedStorageFix.getName(dynamic7).endsWith("_door")) {
                                        Dynamic<?> dynamic13 = this.getBlock(i2);
                                        if ("lower".equals(ChunkPalettedStorageFix.getProperty(dynamic13, "half"))) {
                                            int j3 = relative(i2, ChunkPalettedStorageFix.Direction.UP);
                                            Dynamic<?> dynamic15 = this.getBlock(j3);
                                            String s16 = ChunkPalettedStorageFix.getName(dynamic13);
                                            if (s16.equals(ChunkPalettedStorageFix.getName(dynamic15))) {
                                                String s1 = ChunkPalettedStorageFix.getProperty(dynamic13, "facing");
                                                String s2 = ChunkPalettedStorageFix.getProperty(dynamic13, "open");
                                                String s3 = flag ? "left" : ChunkPalettedStorageFix.getProperty(dynamic15, "hinge");
                                                String s4 = flag ? "false" : ChunkPalettedStorageFix.getProperty(dynamic15, "powered");
                                                this.setBlock(i2, ChunkPalettedStorageFix.MappingConstants.DOOR_MAP.get(s16 + s1 + "lower" + s3 + s2 + s4));
                                                this.setBlock(j3, ChunkPalettedStorageFix.MappingConstants.DOOR_MAP.get(s16 + s1 + "upper" + s3 + s2 + s4));
                                            }
                                        }
                                    }
                                }
                                break;
                            case 86:
                                for (int l1 : entry.getValue()) {
                                    l1 |= i;
                                    Dynamic<?> dynamic6 = this.getBlock(l1);
                                    if ("minecraft:carved_pumpkin".equals(ChunkPalettedStorageFix.getName(dynamic6))) {
                                        String s8 = ChunkPalettedStorageFix.getName(this.getBlock(relative(l1, ChunkPalettedStorageFix.Direction.DOWN)));
                                        if ("minecraft:grass_block".equals(s8) || "minecraft:dirt".equals(s8)) {
                                            this.setBlock(l1, ChunkPalettedStorageFix.MappingConstants.PUMPKIN);
                                        }
                                    }
                                }
                                break;
                            case 110:
                                for (int k1 : entry.getValue()) {
                                    k1 |= i;
                                    Dynamic<?> dynamic5 = this.getBlock(k1);
                                    if ("minecraft:mycelium".equals(ChunkPalettedStorageFix.getName(dynamic5))) {
                                        String s7 = ChunkPalettedStorageFix.getName(this.getBlock(relative(k1, ChunkPalettedStorageFix.Direction.UP)));
                                        if ("minecraft:snow".equals(s7) || "minecraft:snow_layer".equals(s7)) {
                                            this.setBlock(k1, ChunkPalettedStorageFix.MappingConstants.SNOWY_MYCELIUM);
                                        }
                                    }
                                }
                                break;
                            case 140:
                                for (int j1 : entry.getValue()) {
                                    j1 |= i;
                                    Dynamic<?> dynamic4 = this.removeBlockEntity(j1);
                                    if (dynamic4 != null) {
                                        String s6 = dynamic4.get("Item").asString("") + dynamic4.get("Data").asInt(0);
                                        this.setBlock(
                                            j1,
                                            ChunkPalettedStorageFix.MappingConstants.FLOWER_POT_MAP
                                                .getOrDefault(s6, ChunkPalettedStorageFix.MappingConstants.FLOWER_POT_MAP.get("minecraft:air0"))
                                        );
                                    }
                                }
                                break;
                            case 144:
                                for (int i1 : entry.getValue()) {
                                    i1 |= i;
                                    Dynamic<?> dynamic3 = this.getBlockEntity(i1);
                                    if (dynamic3 != null) {
                                        String s5 = String.valueOf(dynamic3.get("SkullType").asInt(0));
                                        String s13 = ChunkPalettedStorageFix.getProperty(this.getBlock(i1), "facing");
                                        String s14;
                                        if (!"up".equals(s13) && !"down".equals(s13)) {
                                            s14 = s5 + s13;
                                        } else {
                                            s14 = s5 + dynamic3.get("Rot").asInt(0);
                                        }

                                        dynamic3.remove("SkullType");
                                        dynamic3.remove("facing");
                                        dynamic3.remove("Rot");
                                        this.setBlock(
                                            i1,
                                            ChunkPalettedStorageFix.MappingConstants.SKULL_MAP
                                                .getOrDefault(s14, ChunkPalettedStorageFix.MappingConstants.SKULL_MAP.get("0north"))
                                        );
                                    }
                                }
                                break;
                            case 175:
                                for (int l : entry.getValue()) {
                                    l |= i;
                                    Dynamic<?> dynamic2 = this.getBlock(l);
                                    if ("upper".equals(ChunkPalettedStorageFix.getProperty(dynamic2, "half"))) {
                                        Dynamic<?> dynamic12 = this.getBlock(relative(l, ChunkPalettedStorageFix.Direction.DOWN));
                                        String s12 = ChunkPalettedStorageFix.getName(dynamic12);
                                        switch (s12) {
                                            case "minecraft:sunflower":
                                                this.setBlock(l, ChunkPalettedStorageFix.MappingConstants.UPPER_SUNFLOWER);
                                                break;
                                            case "minecraft:lilac":
                                                this.setBlock(l, ChunkPalettedStorageFix.MappingConstants.UPPER_LILAC);
                                                break;
                                            case "minecraft:tall_grass":
                                                this.setBlock(l, ChunkPalettedStorageFix.MappingConstants.UPPER_TALL_GRASS);
                                                break;
                                            case "minecraft:large_fern":
                                                this.setBlock(l, ChunkPalettedStorageFix.MappingConstants.UPPER_LARGE_FERN);
                                                break;
                                            case "minecraft:rose_bush":
                                                this.setBlock(l, ChunkPalettedStorageFix.MappingConstants.UPPER_ROSE_BUSH);
                                                break;
                                            case "minecraft:peony":
                                                this.setBlock(l, ChunkPalettedStorageFix.MappingConstants.UPPER_PEONY);
                                        }
                                    }
                                }
                                break;
                            case 176:
                            case 177:
                                for (int j : entry.getValue()) {
                                    j |= i;
                                    Dynamic<?> dynamic = this.getBlockEntity(j);
                                    Dynamic<?> dynamic1 = this.getBlock(j);
                                    if (dynamic != null) {
                                        int k = dynamic.get("Base").asInt(0);
                                        if (k != 15 && k >= 0 && k < 16) {
                                            String s = ChunkPalettedStorageFix.getProperty(dynamic1, entry.getIntKey() == 176 ? "rotation" : "facing")
                                                + "_"
                                                + k;
                                            if (ChunkPalettedStorageFix.MappingConstants.BANNER_BLOCK_MAP.containsKey(s)) {
                                                this.setBlock(j, ChunkPalettedStorageFix.MappingConstants.BANNER_BLOCK_MAP.get(s));
                                            }
                                        }
                                    }
                                }
                        }
                    }
                }
            }
        }

        @Nullable
        private Dynamic<?> getBlockEntity(int index) {
            return this.blockEntities.get(index);
        }

        @Nullable
        private Dynamic<?> removeBlockEntity(int index) {
            return this.blockEntities.remove(index);
        }

        public static int relative(int data, ChunkPalettedStorageFix.Direction direction) {
            return switch (direction.getAxis()) {
                case X -> {
                    int k = (data & 15) + direction.getAxisDirection().getStep();
                    yield k >= 0 && k <= 15 ? data & -16 | k : -1;
                }
                case Y -> {
                    int j = (data >> 8) + direction.getAxisDirection().getStep();
                    yield j >= 0 && j <= 255 ? data & 0xFF | j << 8 : -1;
                }
                case Z -> {
                    int i = (data >> 4 & 15) + direction.getAxisDirection().getStep();
                    yield i >= 0 && i <= 15 ? data & -241 | i << 4 : -1;
                }
            };
        }

        private void setBlock(int index, Dynamic<?> block) {
            if (index >= 0 && index <= 65535) {
                ChunkPalettedStorageFix.Section chunkpalettedstoragefix$section = this.getSection(index);
                if (chunkpalettedstoragefix$section != null) {
                    chunkpalettedstoragefix$section.setBlock(index & 4095, block);
                }
            }
        }

        @Nullable
        private ChunkPalettedStorageFix.Section getSection(int index) {
            int i = index >> 12;
            return i < this.sections.length ? this.sections[i] : null;
        }

        public Dynamic<?> getBlock(int index) {
            if (index >= 0 && index <= 65535) {
                ChunkPalettedStorageFix.Section chunkpalettedstoragefix$section = this.getSection(index);
                return chunkpalettedstoragefix$section == null
                    ? ChunkPalettedStorageFix.MappingConstants.AIR
                    : chunkpalettedstoragefix$section.getBlock(index & 4095);
            } else {
                return ChunkPalettedStorageFix.MappingConstants.AIR;
            }
        }

        public Dynamic<?> write() {
            Dynamic<?> dynamic = this.level;
            if (this.blockEntities.isEmpty()) {
                dynamic = dynamic.remove("TileEntities");
            } else {
                dynamic = dynamic.set("TileEntities", dynamic.createList(this.blockEntities.values().stream()));
            }

            Dynamic<?> dynamic1 = dynamic.emptyMap();
            List<Dynamic<?>> list = Lists.newArrayList();

            for (ChunkPalettedStorageFix.Section chunkpalettedstoragefix$section : this.sections) {
                if (chunkpalettedstoragefix$section != null) {
                    list.add(chunkpalettedstoragefix$section.write());
                    dynamic1 = dynamic1.set(
                        String.valueOf(chunkpalettedstoragefix$section.y),
                        dynamic1.createIntList(Arrays.stream(chunkpalettedstoragefix$section.update.toIntArray()))
                    );
                }
            }

            Dynamic<?> dynamic2 = dynamic.emptyMap();
            dynamic2 = dynamic2.set("Sides", dynamic2.createByte((byte)this.sides));
            dynamic2 = dynamic2.set("Indices", dynamic1);
            return dynamic.set("UpgradeData", dynamic2).set("Sections", dynamic2.createList(list.stream()));
        }
    }
}
