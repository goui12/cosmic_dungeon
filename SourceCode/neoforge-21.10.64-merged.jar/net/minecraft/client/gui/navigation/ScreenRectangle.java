package net.minecraft.client.gui.navigation;

import javax.annotation.Nullable;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix3x2f;
import org.joml.Vector2f;

@OnlyIn(Dist.CLIENT)
public record ScreenRectangle(ScreenPosition position, int width, int height) {
    private static final ScreenRectangle EMPTY = new ScreenRectangle(0, 0, 0, 0);

    public ScreenRectangle(int p_265721_, int p_265116_, int p_265225_, int p_265493_) {
        this(new ScreenPosition(p_265721_, p_265116_), p_265225_, p_265493_);
    }

    public static ScreenRectangle empty() {
        return EMPTY;
    }

    public static ScreenRectangle of(ScreenAxis axis, int primaryPosition, int secondaryPosition, int primaryLength, int secondaryLength) {
        return switch (axis) {
            case HORIZONTAL -> new ScreenRectangle(primaryPosition, secondaryPosition, primaryLength, secondaryLength);
            case VERTICAL -> new ScreenRectangle(secondaryPosition, primaryPosition, secondaryLength, primaryLength);
        };
    }

    public ScreenRectangle step(ScreenDirection direction) {
        return new ScreenRectangle(this.position.step(direction), this.width, this.height);
    }

    public int getLength(ScreenAxis axis) {
        return switch (axis) {
            case HORIZONTAL -> this.width;
            case VERTICAL -> this.height;
        };
    }

    public int getBoundInDirection(ScreenDirection direction) {
        ScreenAxis screenaxis = direction.getAxis();
        return direction.isPositive() ? this.position.getCoordinate(screenaxis) + this.getLength(screenaxis) - 1 : this.position.getCoordinate(screenaxis);
    }

    public ScreenRectangle getBorder(ScreenDirection direction) {
        int i = this.getBoundInDirection(direction);
        ScreenAxis screenaxis = direction.getAxis().orthogonal();
        int j = this.getBoundInDirection(screenaxis.getNegative());
        int k = this.getLength(screenaxis);
        return of(direction.getAxis(), i, j, 1, k).step(direction);
    }

    public boolean overlaps(ScreenRectangle rectangle) {
        return this.overlapsInAxis(rectangle, ScreenAxis.HORIZONTAL) && this.overlapsInAxis(rectangle, ScreenAxis.VERTICAL);
    }

    public boolean overlapsInAxis(ScreenRectangle rectangle, ScreenAxis axis) {
        int i = this.getBoundInDirection(axis.getNegative());
        int j = rectangle.getBoundInDirection(axis.getNegative());
        int k = this.getBoundInDirection(axis.getPositive());
        int l = rectangle.getBoundInDirection(axis.getPositive());
        return Math.max(i, j) <= Math.min(k, l);
    }

    public int getCenterInAxis(ScreenAxis axis) {
        return (this.getBoundInDirection(axis.getPositive()) + this.getBoundInDirection(axis.getNegative())) / 2;
    }

    @Nullable
    public ScreenRectangle intersection(ScreenRectangle rectangle) {
        int i = Math.max(this.left(), rectangle.left());
        int j = Math.max(this.top(), rectangle.top());
        int k = Math.min(this.right(), rectangle.right());
        int l = Math.min(this.bottom(), rectangle.bottom());
        return i < k && j < l ? new ScreenRectangle(i, j, k - i, l - j) : null;
    }

    public boolean intersects(ScreenRectangle rectangle) {
        return this.left() < rectangle.right() && this.right() > rectangle.left() && this.top() < rectangle.bottom() && this.bottom() > rectangle.top();
    }

    public boolean encompasses(ScreenRectangle rectangle) {
        return rectangle.left() >= this.left() && rectangle.top() >= this.top() && rectangle.right() <= this.right() && rectangle.bottom() <= this.bottom();
    }

    public int top() {
        return this.position.y();
    }

    public int bottom() {
        return this.position.y() + this.height;
    }

    public int left() {
        return this.position.x();
    }

    public int right() {
        return this.position.x() + this.width;
    }

    public boolean containsPoint(int x, int y) {
        return x >= this.left() && x < this.right() && y >= this.top() && y < this.bottom();
    }

    public ScreenRectangle transformAxisAligned(Matrix3x2f pos) {
        Vector2f vector2f = pos.transformPosition(this.left(), this.top(), new Vector2f());
        Vector2f vector2f1 = pos.transformPosition(this.right(), this.bottom(), new Vector2f());
        return new ScreenRectangle(Mth.floor(vector2f.x), Mth.floor(vector2f.y), Mth.floor(vector2f1.x - vector2f.x), Mth.floor(vector2f1.y - vector2f.y));
    }

    public ScreenRectangle transformMaxBounds(Matrix3x2f pos) {
        Vector2f vector2f = pos.transformPosition(this.left(), this.top(), new Vector2f());
        Vector2f vector2f1 = pos.transformPosition(this.right(), this.top(), new Vector2f());
        Vector2f vector2f2 = pos.transformPosition(this.left(), this.bottom(), new Vector2f());
        Vector2f vector2f3 = pos.transformPosition(this.right(), this.bottom(), new Vector2f());
        float f = Math.min(Math.min(vector2f.x(), vector2f2.x()), Math.min(vector2f1.x(), vector2f3.x()));
        float f1 = Math.max(Math.max(vector2f.x(), vector2f2.x()), Math.max(vector2f1.x(), vector2f3.x()));
        float f2 = Math.min(Math.min(vector2f.y(), vector2f2.y()), Math.min(vector2f1.y(), vector2f3.y()));
        float f3 = Math.max(Math.max(vector2f.y(), vector2f2.y()), Math.max(vector2f1.y(), vector2f3.y()));
        return new ScreenRectangle(Mth.floor(f), Mth.floor(f2), Mth.ceil(f1 - f), Mth.ceil(f3 - f2));
    }
}
