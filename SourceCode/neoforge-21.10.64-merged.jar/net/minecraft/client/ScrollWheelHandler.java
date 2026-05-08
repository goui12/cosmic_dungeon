package net.minecraft.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Vector2i;

@OnlyIn(Dist.CLIENT)
public class ScrollWheelHandler {
    private double accumulatedScrollX;
    private double accumulatedScrollY;

    public Vector2i onMouseScroll(double xOffset, double yOffset) {
        if (this.accumulatedScrollX != 0.0 && Math.signum(xOffset) != Math.signum(this.accumulatedScrollX)) {
            this.accumulatedScrollX = 0.0;
        }

        if (this.accumulatedScrollY != 0.0 && Math.signum(yOffset) != Math.signum(this.accumulatedScrollY)) {
            this.accumulatedScrollY = 0.0;
        }

        this.accumulatedScrollX += xOffset;
        this.accumulatedScrollY += yOffset;
        int i = (int)this.accumulatedScrollX;
        int j = (int)this.accumulatedScrollY;
        if (i == 0 && j == 0) {
            return new Vector2i(0, 0);
        } else {
            this.accumulatedScrollX -= i;
            this.accumulatedScrollY -= j;
            return new Vector2i(i, j);
        }
    }

    public static int getNextScrollWheelSelection(double yOffset, int selected, int selectionSize) {
        int i = (int)Math.signum(yOffset);
        selected -= i;
        selected = Math.max(-1, selected);

        while (selected < 0) {
            selected += selectionSize;
        }

        while (selected >= selectionSize) {
            selected -= selectionSize;
        }

        return selected;
    }
}
