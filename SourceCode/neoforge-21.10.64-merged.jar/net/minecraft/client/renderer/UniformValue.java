package net.minecraft.client.renderer;

import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.StringRepresentable;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix4fc;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector3i;
import org.joml.Vector4f;

@OnlyIn(Dist.CLIENT)
public interface UniformValue {
    Codec<UniformValue> CODEC = UniformValue.Type.CODEC.dispatch(UniformValue::type, p_418396_ -> p_418396_.valueCodec);

    void writeTo(Std140Builder builder);

    void addSize(Std140SizeCalculator builder);

    UniformValue.Type type();

    @OnlyIn(Dist.CLIENT)
    public record FloatUniform(float value) implements UniformValue {
        public static final Codec<UniformValue.FloatUniform> CODEC = Codec.FLOAT.xmap(UniformValue.FloatUniform::new, UniformValue.FloatUniform::value);

        @Override
        public void writeTo(Std140Builder p_418143_) {
            p_418143_.putFloat(this.value);
        }

        @Override
        public void addSize(Std140SizeCalculator p_418390_) {
            p_418390_.putFloat();
        }

        @Override
        public UniformValue.Type type() {
            return UniformValue.Type.FLOAT;
        }
    }

    @OnlyIn(Dist.CLIENT)
    public record IVec3Uniform(Vector3i value) implements UniformValue {
        public static final Codec<UniformValue.IVec3Uniform> CODEC = ExtraCodecs.VECTOR3I
            .xmap(UniformValue.IVec3Uniform::new, UniformValue.IVec3Uniform::value);

        @Override
        public void writeTo(Std140Builder p_418141_) {
            p_418141_.putIVec3(this.value);
        }

        @Override
        public void addSize(Std140SizeCalculator p_418431_) {
            p_418431_.putIVec3();
        }

        @Override
        public UniformValue.Type type() {
            return UniformValue.Type.IVEC3;
        }
    }

    @OnlyIn(Dist.CLIENT)
    public record IntUniform(int value) implements UniformValue {
        public static final Codec<UniformValue.IntUniform> CODEC = Codec.INT.xmap(UniformValue.IntUniform::new, UniformValue.IntUniform::value);

        @Override
        public void writeTo(Std140Builder p_418460_) {
            p_418460_.putInt(this.value);
        }

        @Override
        public void addSize(Std140SizeCalculator p_418327_) {
            p_418327_.putInt();
        }

        @Override
        public UniformValue.Type type() {
            return UniformValue.Type.INT;
        }
    }

    @OnlyIn(Dist.CLIENT)
    public record Matrix4x4Uniform(Matrix4fc value) implements UniformValue {
        public static final Codec<UniformValue.Matrix4x4Uniform> CODEC = ExtraCodecs.MATRIX4F
            .xmap(UniformValue.Matrix4x4Uniform::new, UniformValue.Matrix4x4Uniform::value);

        @Override
        public void writeTo(Std140Builder p_418041_) {
            p_418041_.putMat4f(this.value);
        }

        @Override
        public void addSize(Std140SizeCalculator p_418487_) {
            p_418487_.putMat4f();
        }

        @Override
        public UniformValue.Type type() {
            return UniformValue.Type.MATRIX4X4;
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static enum Type implements StringRepresentable {
        INT("int", UniformValue.IntUniform.CODEC),
        IVEC3("ivec3", UniformValue.IVec3Uniform.CODEC),
        FLOAT("float", UniformValue.FloatUniform.CODEC),
        VEC2("vec2", UniformValue.Vec2Uniform.CODEC),
        VEC3("vec3", UniformValue.Vec3Uniform.CODEC),
        VEC4("vec4", UniformValue.Vec4Uniform.CODEC),
        MATRIX4X4("matrix4x4", UniformValue.Matrix4x4Uniform.CODEC);

        public static final StringRepresentable.EnumCodec<UniformValue.Type> CODEC = StringRepresentable.fromEnum(UniformValue.Type::values);
        private final String name;
        final MapCodec<? extends UniformValue> valueCodec;

        private Type(String name, Codec<? extends UniformValue> codec) {
            this.name = name;
            this.valueCodec = codec.fieldOf("value");
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }
    }

    @OnlyIn(Dist.CLIENT)
    public record Vec2Uniform(Vector2f value) implements UniformValue {
        public static final Codec<UniformValue.Vec2Uniform> CODEC = ExtraCodecs.VECTOR2F.xmap(UniformValue.Vec2Uniform::new, UniformValue.Vec2Uniform::value);

        @Override
        public void writeTo(Std140Builder p_418356_) {
            p_418356_.putVec2(this.value);
        }

        @Override
        public void addSize(Std140SizeCalculator p_418037_) {
            p_418037_.putVec2();
        }

        @Override
        public UniformValue.Type type() {
            return UniformValue.Type.VEC2;
        }
    }

    @OnlyIn(Dist.CLIENT)
    public record Vec3Uniform(Vector3f value) implements UniformValue {
        public static final Codec<UniformValue.Vec3Uniform> CODEC = ExtraCodecs.VECTOR3F.xmap(UniformValue.Vec3Uniform::new, UniformValue.Vec3Uniform::value);

        @Override
        public void writeTo(Std140Builder p_418183_) {
            p_418183_.putVec3(this.value);
        }

        @Override
        public void addSize(Std140SizeCalculator p_418181_) {
            p_418181_.putVec3();
        }

        @Override
        public UniformValue.Type type() {
            return UniformValue.Type.VEC3;
        }
    }

    @OnlyIn(Dist.CLIENT)
    public record Vec4Uniform(Vector4f value) implements UniformValue {
        public static final Codec<UniformValue.Vec4Uniform> CODEC = ExtraCodecs.VECTOR4F.xmap(UniformValue.Vec4Uniform::new, UniformValue.Vec4Uniform::value);

        @Override
        public void writeTo(Std140Builder p_418009_) {
            p_418009_.putVec4(this.value);
        }

        @Override
        public void addSize(Std140SizeCalculator p_418414_) {
            p_418414_.putVec4();
        }

        @Override
        public UniformValue.Type type() {
            return UniformValue.Type.VEC4;
        }
    }
}
