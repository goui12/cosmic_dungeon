package net.minecraft.client.resources;

import com.mojang.blaze3d.platform.NativeImage;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class MapTextureManager implements AutoCloseable {
    private final Int2ObjectMap<MapTextureManager.MapInstance> maps = new Int2ObjectOpenHashMap<>();
    final TextureManager textureManager;

    public MapTextureManager(TextureManager textureManager) {
        this.textureManager = textureManager;
    }

    public void update(MapId id, MapItemSavedData data) {
        this.getOrCreateMapInstance(id, data).forceUpload();
    }

    public ResourceLocation prepareMapTexture(MapId id, MapItemSavedData data) {
        MapTextureManager.MapInstance maptexturemanager$mapinstance = this.getOrCreateMapInstance(id, data);
        maptexturemanager$mapinstance.updateTextureIfNeeded();
        return maptexturemanager$mapinstance.location;
    }

    public void resetData() {
        for (MapTextureManager.MapInstance maptexturemanager$mapinstance : this.maps.values()) {
            maptexturemanager$mapinstance.close();
        }

        this.maps.clear();
    }

    private MapTextureManager.MapInstance getOrCreateMapInstance(MapId id, MapItemSavedData data) {
        return this.maps.compute(id.id(), (p_362953_, p_362729_) -> {
            if (p_362729_ == null) {
                return new MapTextureManager.MapInstance(p_362953_, data);
            } else {
                p_362729_.replaceMapData(data);
                return (MapTextureManager.MapInstance)p_362729_;
            }
        });
    }

    @Override
    public void close() {
        this.resetData();
    }

    @OnlyIn(Dist.CLIENT)
    class MapInstance implements AutoCloseable {
        private MapItemSavedData data;
        private final DynamicTexture texture;
        private boolean requiresUpload = true;
        final ResourceLocation location;

        MapInstance(int id, MapItemSavedData data) {
            this.data = data;
            this.texture = new DynamicTexture(() -> "Map " + id, 128, 128, true);
            this.location = ResourceLocation.withDefaultNamespace("map/" + id);
            MapTextureManager.this.textureManager.register(this.location, this.texture);
        }

        void replaceMapData(MapItemSavedData data) {
            boolean flag = this.data != data;
            this.data = data;
            this.requiresUpload |= flag;
        }

        public void forceUpload() {
            this.requiresUpload = true;
        }

        void updateTextureIfNeeded() {
            if (this.requiresUpload) {
                NativeImage nativeimage = this.texture.getPixels();
                if (nativeimage != null) {
                    for (int i = 0; i < 128; i++) {
                        for (int j = 0; j < 128; j++) {
                            int k = j + i * 128;
                            nativeimage.setPixel(j, i, MapColor.getColorFromPackedId(this.data.colors[k]));
                        }
                    }
                }

                this.texture.upload();
                this.requiresUpload = false;
            }
        }

        @Override
        public void close() {
            this.texture.close();
        }
    }
}
