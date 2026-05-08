package net.minecraft.world.level.saveddata.maps;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import io.netty.buffer.ByteBuf;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundMapItemDataPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.MapDecorations;
import net.minecraft.world.item.component.MapItemColor;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import org.slf4j.Logger;

public class MapItemSavedData extends SavedData {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int MAP_SIZE = 128;
    private static final int HALF_MAP_SIZE = 64;
    public static final int MAX_SCALE = 4;
    public static final int TRACKED_DECORATION_LIMIT = 256;
    private static final String FRAME_PREFIX = "frame-";
    public static final Codec<MapItemSavedData> CODEC = RecordCodecBuilder.create(
        p_400959_ -> p_400959_.group(
                Level.RESOURCE_KEY_CODEC.fieldOf("dimension").forGetter(p_400957_ -> p_400957_.dimension),
                Codec.INT.fieldOf("xCenter").forGetter(p_400958_ -> p_400958_.centerX),
                Codec.INT.fieldOf("zCenter").forGetter(p_400961_ -> p_400961_.centerZ),
                Codec.BYTE.optionalFieldOf("scale", (byte)0).forGetter(p_400962_ -> p_400962_.scale),
                Codec.BYTE_BUFFER.fieldOf("colors").forGetter(p_400963_ -> ByteBuffer.wrap(p_400963_.colors)),
                Codec.BOOL.optionalFieldOf("trackingPosition", true).forGetter(p_400965_ -> p_400965_.trackingPosition),
                Codec.BOOL.optionalFieldOf("unlimitedTracking", false).forGetter(p_400966_ -> p_400966_.unlimitedTracking),
                Codec.BOOL.optionalFieldOf("locked", false).forGetter(p_400964_ -> p_400964_.locked),
                MapBanner.CODEC.listOf().optionalFieldOf("banners", List.of()).forGetter(p_400960_ -> List.copyOf(p_400960_.bannerMarkers.values())),
                MapFrame.CODEC.listOf().optionalFieldOf("frames", List.of()).forGetter(p_400956_ -> List.copyOf(p_400956_.frameMarkers.values()))
            )
            .apply(p_400959_, MapItemSavedData::new)
    );
    public final int centerX;
    public final int centerZ;
    public final ResourceKey<Level> dimension;
    private final boolean trackingPosition;
    private final boolean unlimitedTracking;
    public final byte scale;
    public byte[] colors = new byte[16384];
    public final boolean locked;
    private final List<MapItemSavedData.HoldingPlayer> carriedBy = Lists.newArrayList();
    private final Map<Player, MapItemSavedData.HoldingPlayer> carriedByPlayers = Maps.newHashMap();
    private final Map<String, MapBanner> bannerMarkers = Maps.newHashMap();
    final Map<String, MapDecoration> decorations = Maps.newLinkedHashMap();
    private final Map<String, MapFrame> frameMarkers = Maps.newHashMap();
    private int trackedDecorationCount;

    public static SavedDataType<MapItemSavedData> type(MapId mapId) {
        return new SavedDataType<>(mapId.key(), () -> {
            throw new IllegalStateException("Should never create an empty map saved data");
        }, CODEC, DataFixTypes.SAVED_DATA_MAP_DATA);
    }

    private MapItemSavedData(
        int x, int z, byte scale, boolean trackingPosition, boolean unlimitedTracking, boolean locked, ResourceKey<Level> dimension
    ) {
        this.scale = scale;
        this.centerX = x;
        this.centerZ = z;
        this.dimension = dimension;
        this.trackingPosition = trackingPosition;
        this.unlimitedTracking = unlimitedTracking;
        this.locked = locked;
    }

    private MapItemSavedData(
        ResourceKey<Level> dimension,
        int x,
        int z,
        byte scale,
        ByteBuffer colors,
        boolean trackingPosition,
        boolean unlimitedTracking,
        boolean locked,
        List<MapBanner> banners,
        List<MapFrame> frames
    ) {
        this(x, z, (byte)Mth.clamp(scale, 0, 4), trackingPosition, unlimitedTracking, locked, dimension);
        if (colors.array().length == 16384) {
            this.colors = colors.array();
        }

        for (MapBanner mapbanner : banners) {
            this.bannerMarkers.put(mapbanner.getId(), mapbanner);
            this.addDecoration(
                mapbanner.getDecoration(), null, mapbanner.getId(), mapbanner.pos().getX(), mapbanner.pos().getZ(), 180.0, mapbanner.name().orElse(null)
            );
        }

        for (MapFrame mapframe : frames) {
            this.frameMarkers.put(mapframe.getId(), mapframe);
            this.addDecoration(
                MapDecorationTypes.FRAME, null, getFrameKey(mapframe.entityId()), mapframe.pos().getX(), mapframe.pos().getZ(), mapframe.rotation(), null
            );
        }
    }

    public static MapItemSavedData createFresh(
        double x, double z, byte scale, boolean trackingPosition, boolean unlimitedTracking, ResourceKey<Level> dimension
    ) {
        int i = 128 * (1 << scale);
        int j = Mth.floor((x + 64.0) / i);
        int k = Mth.floor((z + 64.0) / i);
        int l = j * i + i / 2 - 64;
        int i1 = k * i + i / 2 - 64;
        return new MapItemSavedData(l, i1, scale, trackingPosition, unlimitedTracking, false, dimension);
    }

    public static MapItemSavedData createForClient(byte scale, boolean locked, ResourceKey<Level> dimension) {
        return new MapItemSavedData(0, 0, scale, false, false, locked, dimension);
    }

    public MapItemSavedData locked() {
        MapItemSavedData mapitemsaveddata = new MapItemSavedData(
            this.centerX, this.centerZ, this.scale, this.trackingPosition, this.unlimitedTracking, true, this.dimension
        );
        mapitemsaveddata.bannerMarkers.putAll(this.bannerMarkers);
        mapitemsaveddata.decorations.putAll(this.decorations);
        mapitemsaveddata.trackedDecorationCount = this.trackedDecorationCount;
        System.arraycopy(this.colors, 0, mapitemsaveddata.colors, 0, this.colors.length);
        return mapitemsaveddata;
    }

    public MapItemSavedData scaled() {
        return createFresh(this.centerX, this.centerZ, (byte)Mth.clamp(this.scale + 1, 0, 4), this.trackingPosition, this.unlimitedTracking, this.dimension);
    }

    private static Predicate<ItemStack> mapMatcher(ItemStack stack) {
        MapId mapid = stack.get(DataComponents.MAP_ID);
        return p_330169_ -> p_330169_ == stack ? true : p_330169_.is(stack.getItem()) && Objects.equals(mapid, p_330169_.get(DataComponents.MAP_ID));
    }

    /**
     * Adds the player passed to the list of visible players and checks to see which players are visible
     */
    public void tickCarriedBy(Player p_player, ItemStack mapStack) {
        if (!this.carriedByPlayers.containsKey(p_player)) {
            MapItemSavedData.HoldingPlayer mapitemsaveddata$holdingplayer = new MapItemSavedData.HoldingPlayer(p_player);
            this.carriedByPlayers.put(p_player, mapitemsaveddata$holdingplayer);
            this.carriedBy.add(mapitemsaveddata$holdingplayer);
        }

        Predicate<ItemStack> predicate = mapMatcher(mapStack);
        if (!p_player.getInventory().contains(predicate)) {
            this.removeDecoration(p_player.getPlainTextName());
        }

        for (int i = 0; i < this.carriedBy.size(); i++) {
            MapItemSavedData.HoldingPlayer mapitemsaveddata$holdingplayer1 = this.carriedBy.get(i);
            Player player = mapitemsaveddata$holdingplayer1.player;
            String s = player.getPlainTextName();
            if (!player.isRemoved() && (player.getInventory().contains(predicate) || mapStack.isFramed())) {
                if (!mapStack.isFramed() && player.level().dimension() == this.dimension && this.trackingPosition) {
                    this.addDecoration(MapDecorationTypes.PLAYER, player.level(), s, player.getX(), player.getZ(), player.getYRot(), null);
                }
            } else {
                this.carriedByPlayers.remove(player);
                this.carriedBy.remove(mapitemsaveddata$holdingplayer1);
                this.removeDecoration(s);
            }

            if (!player.equals(p_player) && hasMapInvisibilityItemEquipped(player)) {
                this.removeDecoration(s);
            }
        }

        if (mapStack.isFramed() && this.trackingPosition) {
            ItemFrame itemframe = mapStack.getFrame();
            BlockPos blockpos = itemframe.getPos();
            MapFrame mapframe1 = this.frameMarkers.get(MapFrame.frameId(blockpos));
            if (mapframe1 != null && itemframe.getId() != mapframe1.entityId() && this.frameMarkers.containsKey(mapframe1.getId())) {
                this.removeDecoration(getFrameKey(mapframe1.entityId()));
            }

            MapFrame mapframe2 = new MapFrame(blockpos, itemframe.getDirection().get2DDataValue() * 90, itemframe.getId());
            this.addDecoration(
                MapDecorationTypes.FRAME,
                p_player.level(),
                getFrameKey(itemframe.getId()),
                blockpos.getX(),
                blockpos.getZ(),
                itemframe.getDirection().get2DDataValue() * 90,
                null
            );
            MapFrame mapframe = this.frameMarkers.put(mapframe2.getId(), mapframe2);
            if (!mapframe2.equals(mapframe)) {
                this.setDirty();
            }
        }

        MapDecorations mapdecorations = mapStack.getOrDefault(DataComponents.MAP_DECORATIONS, MapDecorations.EMPTY);
        if (!this.decorations.keySet().containsAll(mapdecorations.decorations().keySet())) {
            mapdecorations.decorations().forEach((p_423425_, p_423426_) -> {
                if (!this.decorations.containsKey(p_423425_)) {
                    this.addDecoration(p_423426_.type(), p_player.level(), p_423425_, p_423426_.x(), p_423426_.z(), p_423426_.rotation(), null);
                }
            });
        }
    }

    private static boolean hasMapInvisibilityItemEquipped(Player player) {
        for (EquipmentSlot equipmentslot : EquipmentSlot.values()) {
            if (equipmentslot != EquipmentSlot.MAINHAND
                && equipmentslot != EquipmentSlot.OFFHAND
                && player.getItemBySlot(equipmentslot).is(ItemTags.MAP_INVISIBILITY_EQUIPMENT)) {
                return true;
            }
        }

        return false;
    }

    public void removeDecoration(String identifier) {
        MapDecoration mapdecoration = this.decorations.remove(identifier);
        if (mapdecoration != null && mapdecoration.type().value().trackCount()) {
            this.trackedDecorationCount--;
        }

        this.setDecorationsDirty();
    }

    public static void addTargetDecoration(ItemStack stack, BlockPos pos, String type, Holder<MapDecorationType> mapDecorationType) {
        MapDecorations.Entry mapdecorations$entry = new MapDecorations.Entry(mapDecorationType, pos.getX(), pos.getZ(), 180.0F);
        stack.update(DataComponents.MAP_DECORATIONS, MapDecorations.EMPTY, p_330166_ -> p_330166_.withDecoration(type, mapdecorations$entry));
        if (mapDecorationType.value().hasMapColor()) {
            stack.set(DataComponents.MAP_COLOR, new MapItemColor(mapDecorationType.value().mapColor()));
        }
    }

    public void addDecoration(
        Holder<MapDecorationType> decorationType,
        @Nullable LevelAccessor level,
        String id,
        double x,
        double z,
        double yRot,
        @Nullable Component displayName
    ) {
        int i = 1 << this.scale;
        float f = (float)(x - this.centerX) / i;
        float f1 = (float)(z - this.centerZ) / i;
        MapItemSavedData.MapDecorationLocation mapitemsaveddata$mapdecorationlocation = this.calculateDecorationLocationAndType(
            decorationType, level, yRot, f, f1
        );
        if (mapitemsaveddata$mapdecorationlocation == null) {
            this.removeDecoration(id);
        } else {
            MapDecoration mapdecoration = new MapDecoration(
                mapitemsaveddata$mapdecorationlocation.type(),
                mapitemsaveddata$mapdecorationlocation.x(),
                mapitemsaveddata$mapdecorationlocation.y(),
                mapitemsaveddata$mapdecorationlocation.rot(),
                Optional.ofNullable(displayName)
            );
            MapDecoration mapdecoration1 = this.decorations.put(id, mapdecoration);
            if (!mapdecoration.equals(mapdecoration1)) {
                if (mapdecoration1 != null && mapdecoration1.type().value().trackCount()) {
                    this.trackedDecorationCount--;
                }

                if (mapitemsaveddata$mapdecorationlocation.type().value().trackCount()) {
                    this.trackedDecorationCount++;
                }

                this.setDecorationsDirty();
            }
        }
    }

    @Nullable
    private MapItemSavedData.MapDecorationLocation calculateDecorationLocationAndType(
        Holder<MapDecorationType> decorationType, @Nullable LevelAccessor level, double yRot, float x, float z
    ) {
        byte b0 = clampMapCoordinate(x);
        byte b1 = clampMapCoordinate(z);
        if (decorationType.is(MapDecorationTypes.PLAYER)) {
            Pair<Holder<MapDecorationType>, Byte> pair = this.playerDecorationTypeAndRotation(decorationType, level, yRot, x, z);
            return pair == null ? null : new MapItemSavedData.MapDecorationLocation(pair.getFirst(), b0, b1, pair.getSecond());
        } else {
            return !isInsideMap(x, z) && !this.unlimitedTracking
                ? null
                : new MapItemSavedData.MapDecorationLocation(decorationType, b0, b1, this.calculateRotation(level, yRot));
        }
    }

    @Nullable
    private Pair<Holder<MapDecorationType>, Byte> playerDecorationTypeAndRotation(
        Holder<MapDecorationType> decorationType, @Nullable LevelAccessor level, double yRot, float x, float z
    ) {
        if (isInsideMap(x, z)) {
            return Pair.of(decorationType, this.calculateRotation(level, yRot));
        } else {
            Holder<MapDecorationType> holder = this.decorationTypeForPlayerOutsideMap(x, z);
            return holder == null ? null : Pair.of(holder, (byte)0);
        }
    }

    private byte calculateRotation(@Nullable LevelAccessor level, double yRot) {
        if (this.dimension == Level.NETHER && level != null) {
            int i = (int)(level.getLevelData().getDayTime() / 10L);
            return (byte)(i * i * 34187121 + i * 121 >> 15 & 15);
        } else {
            double d0 = yRot < 0.0 ? yRot - 8.0 : yRot + 8.0;
            return (byte)(d0 * 16.0 / 360.0);
        }
    }

    private static boolean isInsideMap(float x, float z) {
        int i = 63;
        return x >= -63.0F && z >= -63.0F && x <= 63.0F && z <= 63.0F;
    }

    @Nullable
    private Holder<MapDecorationType> decorationTypeForPlayerOutsideMap(float x, float z) {
        int i = 320;
        boolean flag = Math.abs(x) < 320.0F && Math.abs(z) < 320.0F;
        if (flag) {
            return MapDecorationTypes.PLAYER_OFF_MAP;
        } else {
            return this.unlimitedTracking ? MapDecorationTypes.PLAYER_OFF_LIMITS : null;
        }
    }

    private static byte clampMapCoordinate(float coord) {
        int i = 63;
        if (coord <= -63.0F) {
            return -128;
        } else {
            return coord >= 63.0F ? 127 : (byte)(coord * 2.0F + 0.5);
        }
    }

    @Nullable
    public Packet<?> getUpdatePacket(MapId mapId, Player player) {
        MapItemSavedData.HoldingPlayer mapitemsaveddata$holdingplayer = this.carriedByPlayers.get(player);
        return mapitemsaveddata$holdingplayer == null ? null : mapitemsaveddata$holdingplayer.nextUpdatePacket(mapId);
    }

    private void setColorsDirty(int x, int z) {
        this.setDirty();

        for (MapItemSavedData.HoldingPlayer mapitemsaveddata$holdingplayer : this.carriedBy) {
            mapitemsaveddata$holdingplayer.markColorsDirty(x, z);
        }
    }

    private void setDecorationsDirty() {
        this.carriedBy.forEach(MapItemSavedData.HoldingPlayer::markDecorationsDirty);
    }

    public MapItemSavedData.HoldingPlayer getHoldingPlayer(Player player) {
        MapItemSavedData.HoldingPlayer mapitemsaveddata$holdingplayer = this.carriedByPlayers.get(player);
        if (mapitemsaveddata$holdingplayer == null) {
            mapitemsaveddata$holdingplayer = new MapItemSavedData.HoldingPlayer(player);
            this.carriedByPlayers.put(player, mapitemsaveddata$holdingplayer);
            this.carriedBy.add(mapitemsaveddata$holdingplayer);
        }

        return mapitemsaveddata$holdingplayer;
    }

    public boolean toggleBanner(LevelAccessor accessor, BlockPos pos) {
        double d0 = pos.getX() + 0.5;
        double d1 = pos.getZ() + 0.5;
        int i = 1 << this.scale;
        double d2 = (d0 - this.centerX) / i;
        double d3 = (d1 - this.centerZ) / i;
        int j = 63;
        if (d2 >= -63.0 && d3 >= -63.0 && d2 <= 63.0 && d3 <= 63.0) {
            MapBanner mapbanner = MapBanner.fromWorld(accessor, pos);
            if (mapbanner == null) {
                return false;
            }

            if (this.bannerMarkers.remove(mapbanner.getId(), mapbanner)) {
                this.removeDecoration(mapbanner.getId());
                this.setDirty();
                return true;
            }

            if (!this.isTrackedCountOverLimit(256)) {
                this.bannerMarkers.put(mapbanner.getId(), mapbanner);
                this.addDecoration(mapbanner.getDecoration(), accessor, mapbanner.getId(), d0, d1, 180.0, mapbanner.name().orElse(null));
                this.setDirty();
                return true;
            }
        }

        return false;
    }

    public void checkBanners(BlockGetter reader, int x, int z) {
        Iterator<MapBanner> iterator = this.bannerMarkers.values().iterator();

        while (iterator.hasNext()) {
            MapBanner mapbanner = iterator.next();
            if (mapbanner.pos().getX() == x && mapbanner.pos().getZ() == z) {
                MapBanner mapbanner1 = MapBanner.fromWorld(reader, mapbanner.pos());
                if (!mapbanner.equals(mapbanner1)) {
                    iterator.remove();
                    this.removeDecoration(mapbanner.getId());
                    this.setDirty();
                }
            }
        }
    }

    public Collection<MapBanner> getBanners() {
        return this.bannerMarkers.values();
    }

    public void removedFromFrame(BlockPos pos, int entityId) {
        this.removeDecoration(getFrameKey(entityId));
        this.frameMarkers.remove(MapFrame.frameId(pos));
        this.setDirty();
    }

    public boolean updateColor(int x, int z, byte color) {
        byte b0 = this.colors[x + z * 128];
        if (b0 != color) {
            this.setColor(x, z, color);
            return true;
        } else {
            return false;
        }
    }

    public void setColor(int x, int z, byte color) {
        this.colors[x + z * 128] = color;
        this.setColorsDirty(x, z);
    }

    public boolean isExplorationMap() {
        for (MapDecoration mapdecoration : this.decorations.values()) {
            if (mapdecoration.type().value().explorationMapElement()) {
                return true;
            }
        }

        return false;
    }

    public void addClientSideDecorations(List<MapDecoration> decorations) {
        this.decorations.clear();
        this.trackedDecorationCount = 0;

        for (int i = 0; i < decorations.size(); i++) {
            MapDecoration mapdecoration = decorations.get(i);
            this.decorations.put("icon-" + i, mapdecoration);
            if (mapdecoration.type().value().trackCount()) {
                this.trackedDecorationCount++;
            }
        }
    }

    public Iterable<MapDecoration> getDecorations() {
        return this.decorations.values();
    }

    public boolean isTrackedCountOverLimit(int trackedCount) {
        return this.trackedDecorationCount >= trackedCount;
    }

    private static String getFrameKey(int entityId) {
        return "frame-" + entityId;
    }

    public class HoldingPlayer {
        public final Player player;
        private boolean dirtyData = true;
        /**
         * The lowest dirty x value
         */
        private int minDirtyX;
        /**
         * The lowest dirty z value
         */
        private int minDirtyY;
        /**
         * The highest dirty x value
         */
        private int maxDirtyX = 127;
        /**
         * The highest dirty z value
         */
        private int maxDirtyY = 127;
        private boolean dirtyDecorations = true;
        private int tick;
        public int step;

        HoldingPlayer(Player player) {
            this.player = player;
        }

        private MapItemSavedData.MapPatch createPatch() {
            int i = this.minDirtyX;
            int j = this.minDirtyY;
            int k = this.maxDirtyX + 1 - this.minDirtyX;
            int l = this.maxDirtyY + 1 - this.minDirtyY;
            byte[] abyte = new byte[k * l];

            for (int i1 = 0; i1 < k; i1++) {
                for (int j1 = 0; j1 < l; j1++) {
                    abyte[i1 + j1 * k] = MapItemSavedData.this.colors[i + i1 + (j + j1) * 128];
                }
            }

            return new MapItemSavedData.MapPatch(i, j, k, l, abyte);
        }

        @Nullable
        Packet<?> nextUpdatePacket(MapId mapId) {
            MapItemSavedData.MapPatch mapitemsaveddata$mappatch;
            if (this.dirtyData) {
                this.dirtyData = false;
                mapitemsaveddata$mappatch = this.createPatch();
            } else {
                mapitemsaveddata$mappatch = null;
            }

            Collection<MapDecoration> collection;
            if (this.dirtyDecorations && this.tick++ % 5 == 0) {
                this.dirtyDecorations = false;
                collection = MapItemSavedData.this.decorations.values();
            } else {
                collection = null;
            }

            return collection == null && mapitemsaveddata$mappatch == null
                ? null
                : new ClientboundMapItemDataPacket(mapId, MapItemSavedData.this.scale, MapItemSavedData.this.locked, collection, mapitemsaveddata$mappatch);
        }

        void markColorsDirty(int x, int z) {
            if (this.dirtyData) {
                this.minDirtyX = Math.min(this.minDirtyX, x);
                this.minDirtyY = Math.min(this.minDirtyY, z);
                this.maxDirtyX = Math.max(this.maxDirtyX, x);
                this.maxDirtyY = Math.max(this.maxDirtyY, z);
            } else {
                this.dirtyData = true;
                this.minDirtyX = x;
                this.minDirtyY = z;
                this.maxDirtyX = x;
                this.maxDirtyY = z;
            }
        }

        private void markDecorationsDirty() {
            this.dirtyDecorations = true;
        }
    }

    record MapDecorationLocation(Holder<MapDecorationType> type, byte x, byte y, byte rot) {
    }

    public record MapPatch(int startX, int startY, int width, int height, byte[] mapColors) {
        public static final StreamCodec<ByteBuf, Optional<MapItemSavedData.MapPatch>> STREAM_CODEC = StreamCodec.of(
            MapItemSavedData.MapPatch::write, MapItemSavedData.MapPatch::read
        );

        private static void write(ByteBuf buffer, Optional<MapItemSavedData.MapPatch> mapPatch) {
            if (mapPatch.isPresent()) {
                MapItemSavedData.MapPatch mapitemsaveddata$mappatch = mapPatch.get();
                buffer.writeByte(mapitemsaveddata$mappatch.width);
                buffer.writeByte(mapitemsaveddata$mappatch.height);
                buffer.writeByte(mapitemsaveddata$mappatch.startX);
                buffer.writeByte(mapitemsaveddata$mappatch.startY);
                FriendlyByteBuf.writeByteArray(buffer, mapitemsaveddata$mappatch.mapColors);
            } else {
                buffer.writeByte(0);
            }
        }

        private static Optional<MapItemSavedData.MapPatch> read(ByteBuf buffer) {
            int i = buffer.readUnsignedByte();
            if (i > 0) {
                int j = buffer.readUnsignedByte();
                int k = buffer.readUnsignedByte();
                int l = buffer.readUnsignedByte();
                byte[] abyte = FriendlyByteBuf.readByteArray(buffer);
                return Optional.of(new MapItemSavedData.MapPatch(k, l, i, j, abyte));
            } else {
                return Optional.empty();
            }
        }

        public void applyToMap(MapItemSavedData savedData) {
            for (int i = 0; i < this.width; i++) {
                for (int j = 0; j < this.height; j++) {
                    savedData.setColor(this.startX + i, this.startY + j, this.mapColors[i + j * this.width]);
                }
            }
        }
    }
}
