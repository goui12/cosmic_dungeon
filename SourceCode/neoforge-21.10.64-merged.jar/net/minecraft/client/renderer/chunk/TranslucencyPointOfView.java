package net.minecraft.client.renderer.chunk;

import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class TranslucencyPointOfView {
    private int x;
    private int y;
    private int z;

    public static TranslucencyPointOfView of(Vec3 pos, long chunkPos) {
        return new TranslucencyPointOfView().set(pos, chunkPos);
    }

    public TranslucencyPointOfView set(Vec3 pos, long chunkPos) {
        this.x = getCoordinate(pos.x(), SectionPos.x(chunkPos));
        this.y = getCoordinate(pos.y(), SectionPos.y(chunkPos));
        this.z = getCoordinate(pos.z(), SectionPos.z(chunkPos));
        return this;
    }

    private static int getCoordinate(double coord, int chunkCoord) {
        int i = SectionPos.blockToSectionCoord(coord) - chunkCoord;
        return Mth.clamp(i, -1, 1);
    }

    public boolean isAxisAligned() {
        return this.x == 0 || this.y == 0 || this.z == 0;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        } else {
            return !(other instanceof TranslucencyPointOfView translucencypointofview)
                ? false
                : this.x == translucencypointofview.x && this.y == translucencypointofview.y && this.z == translucencypointofview.z;
        }
    }
}
