package net.goui.cosmicdungeon.block.custom;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** One cached common-side shape, derived from the bundled model, not resource-pack overrides. */
public final class ClassSelectorShape {
    public static final ClassSelectorShape INSTANCE = loadBundled();
    private static final double GRID = 0.5; // Half a model pixel; native shapes use axis-aligned boxes.
    private final VoxelShape shape;

    public ClassSelectorShape(Reader model) {
        JsonArray elements = JsonParser.parseReader(model).getAsJsonObject().getAsJsonArray("elements");
        if (elements == null || elements.isEmpty() || elements.size() > 256) {
            throw new IllegalArgumentException("Invalid class selector geometry");
        }
        VoxelShape result = Shapes.empty();
        for (var element : elements) {
            for (Box box : new Part(element.getAsJsonObject()).boxes()) {
                result = Shapes.joinUnoptimized(result, box.shape(), BooleanOp.OR);
            }
        }
        shape = result.optimize();
    }

    public VoxelShape shape() {
        return shape;
    }

    private static ClassSelectorShape loadBundled() {
        var stream = ClassSelectorShape.class.getResourceAsStream(
                "/assets/cosmicdungeon/models/block/class_selector_block.json");
        if (stream == null) throw new IllegalStateException("Missing class selector model");
        try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return new ClassSelectorShape(reader);
        } catch (java.io.IOException exception) {
            throw new IllegalStateException("Cannot read class selector model", exception);
        }
    }

    private record Point(double u, double v) {}

    private record Box(double[] from, double[] to) {
        VoxelShape shape() {
            return Block.box(from[0], from[1], from[2], to[0], to[1], to[2]);
        }
    }

    private static final class Part {
        private final double[] from;
        private final double[] to;
        private final JsonObject rotation;

        Part(JsonObject element) {
            from = vector(element.getAsJsonArray("from"));
            to = vector(element.getAsJsonArray("to"));
            rotation = element.has("rotation") ? element.getAsJsonObject("rotation") : null;
            for (int axis = 0; axis < 3; axis++) {
                if (from[axis] < -16 || to[axis] > 32 || from[axis] >= to[axis]) {
                    throw new IllegalArgumentException("Invalid selector model bounds");
                }
            }
        }

        List<Box> boxes() {
            if (rotation == null || rotation.get("angle").getAsDouble() == 0) {
                return List.of(new Box(snap(from, false), snap(to, true)));
            }
            int normal = switch (rotation.get("axis").getAsString()) {
                case "x" -> 0;
                case "y" -> 1;
                case "z" -> 2;
                default -> throw new IllegalArgumentException("Invalid selector rotation axis");
            };
            int u = (normal + 1) % 3;
            int v = (normal + 2) % 3;
            double[] origin = vector(rotation.getAsJsonArray("origin"));
            double angle = Math.toRadians(rotation.get("angle").getAsDouble());
            if (!Double.isFinite(angle) || Math.abs(angle) > Math.PI / 4) {
                throw new IllegalArgumentException("Invalid selector rotation angle");
            }
            double scale = rotation.has("rescale") && rotation.get("rescale").getAsBoolean()
                    ? 1 / Math.cos(angle) : 1;
            List<Point> polygon = new ArrayList<>(4);
            polygon.add(rotate(from[u], from[v], origin[u], origin[v], angle, scale));
            polygon.add(rotate(to[u], from[v], origin[u], origin[v], angle, scale));
            polygon.add(rotate(to[u], to[v], origin[u], origin[v], angle, scale));
            polygon.add(rotate(from[u], to[v], origin[u], origin[v], angle, scale));
            double low = polygon.stream().mapToDouble(Point::v).min().orElseThrow();
            double high = polygon.stream().mapToDouble(Point::v).max().orElseThrow();
            List<Box> boxes = new ArrayList<>();
            for (double bottom = down(low); bottom < high - 1.0e-8; bottom += GRID) {
                List<Point> strip = clip(clip(polygon, bottom, true), bottom + GRID, false);
                if (strip.isEmpty()) continue;
                double left = strip.stream().mapToDouble(Point::u).min().orElseThrow();
                double right = strip.stream().mapToDouble(Point::u).max().orElseThrow();
                if (right - left < 1.0e-8) continue;
                double[] a = snap(from, false), b = snap(to, true);
                a[u] = down(left); b[u] = up(right);
                a[v] = bottom; b[v] = bottom + GRID;
                boxes.add(new Box(a, b));
            }
            return boxes;
        }

        private static Point rotate(double u, double v, double ou, double ov, double angle, double scale) {
            double du = u - ou, dv = v - ov;
            return new Point(ou + scale * (du * Math.cos(angle) - dv * Math.sin(angle)),
                    ov + scale * (du * Math.sin(angle) + dv * Math.cos(angle)));
        }

        private static List<Point> clip(List<Point> polygon, double boundary, boolean above) {
            List<Point> result = new ArrayList<>();
            if (polygon.isEmpty()) return result;
            Point previous = polygon.getLast();
            boolean wasInside = above ? previous.v >= boundary : previous.v <= boundary;
            for (Point point : polygon) {
                boolean inside = above ? point.v >= boundary : point.v <= boundary;
                if (inside != wasInside) {
                    double t = (boundary - previous.v) / (point.v - previous.v);
                    result.add(new Point(previous.u + t * (point.u - previous.u), boundary));
                }
                if (inside) result.add(point);
                previous = point; wasInside = inside;
            }
            return result;
        }

        private static double[] vector(JsonArray values) {
            if (values == null || values.size() != 3) throw new IllegalArgumentException("Invalid model vector");
            double[] result = new double[3];
            for (int i = 0; i < 3; i++) {
                result[i] = values.get(i).getAsDouble();
                if (!Double.isFinite(result[i])) throw new IllegalArgumentException("Non-finite model vector");
            }
            return result;
        }

        private static double[] snap(double[] values, boolean ceiling) {
            return new double[] {ceiling ? up(values[0]) : down(values[0]),
                    ceiling ? up(values[1]) : down(values[1]), ceiling ? up(values[2]) : down(values[2])};
        }

        private static double down(double value) { return Math.floor((value + 1.0e-8) / GRID) * GRID; }
        private static double up(double value) { return Math.ceil((value - 1.0e-8) / GRID) * GRID; }
    }
}
