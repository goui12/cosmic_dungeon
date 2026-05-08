package com.mojang.blaze3d.opengl;

import com.mojang.logging.LogUtils;
import java.util.Set;
import java.util.function.Supplier;
import net.minecraft.util.StringUtil;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.lwjgl.opengl.EXTDebugLabel;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GLCapabilities;
import org.lwjgl.opengl.KHRDebug;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public abstract class GlDebugLabel {
    private static final Logger LOGGER = LogUtils.getLogger();

    public void applyLabel(GlBuffer buffer) {
    }

    public void applyLabel(GlTexture texture) {
    }

    public void applyLabel(GlShaderModule shaderModule) {
    }

    public void applyLabel(GlProgram program) {
    }

    public void applyLabel(VertexArrayCache.VertexArray vertexArray) {
    }

    public void pushDebugGroup(Supplier<String> label) {
    }

    public void popDebugGroup() {
    }

    public static GlDebugLabel create(GLCapabilities capabilities, boolean renderDebugLabels, Set<String> enabledExtensions) {
        if (renderDebugLabels) {
            if (capabilities.GL_KHR_debug && GlDevice.USE_GL_KHR_debug) {
                enabledExtensions.add("GL_KHR_debug");
                return new GlDebugLabel.Core();
            }

            if (capabilities.GL_EXT_debug_label && GlDevice.USE_GL_EXT_debug_label) {
                enabledExtensions.add("GL_EXT_debug_label");
                return new GlDebugLabel.Ext();
            }

            LOGGER.warn("Debug labels unavailable: neither KHR_debug nor EXT_debug_label are supported");
        }

        return new GlDebugLabel.Empty();
    }

    public boolean exists() {
        return false;
    }

    @OnlyIn(Dist.CLIENT)
    static class Core extends GlDebugLabel {
        private final int maxLabelLength = GL11.glGetInteger(33512);

        @Override
        public void applyLabel(GlBuffer p_410733_) {
            Supplier<String> supplier = p_410733_.label;
            if (supplier != null) {
                KHRDebug.glObjectLabel(33504, p_410733_.handle, StringUtil.truncateStringIfNecessary(supplier.get(), this.maxLabelLength, true));
            }
        }

        @Override
        public void applyLabel(GlTexture p_410170_) {
            KHRDebug.glObjectLabel(5890, p_410170_.id, StringUtil.truncateStringIfNecessary(p_410170_.getLabel(), this.maxLabelLength, true));
        }

        @Override
        public void applyLabel(GlShaderModule p_410868_) {
            KHRDebug.glObjectLabel(33505, p_410868_.getShaderId(), StringUtil.truncateStringIfNecessary(p_410868_.getDebugLabel(), this.maxLabelLength, true));
        }

        @Override
        public void applyLabel(GlProgram p_409865_) {
            KHRDebug.glObjectLabel(33506, p_409865_.getProgramId(), StringUtil.truncateStringIfNecessary(p_409865_.getDebugLabel(), this.maxLabelLength, true));
        }

        @Override
        public void applyLabel(VertexArrayCache.VertexArray p_410688_) {
            KHRDebug.glObjectLabel(32884, p_410688_.id, StringUtil.truncateStringIfNecessary(p_410688_.format.toString(), this.maxLabelLength, true));
        }

        @Override
        public void pushDebugGroup(Supplier<String> p_420037_) {
            KHRDebug.glPushDebugGroup(33354, 0, p_420037_.get());
        }

        @Override
        public void popDebugGroup() {
            KHRDebug.glPopDebugGroup();
        }

        @Override
        public boolean exists() {
            return true;
        }
    }

    @OnlyIn(Dist.CLIENT)
    static class Empty extends GlDebugLabel {
    }

    @OnlyIn(Dist.CLIENT)
    static class Ext extends GlDebugLabel {
        @Override
        public void applyLabel(GlBuffer p_410006_) {
            Supplier<String> supplier = p_410006_.label;
            if (supplier != null) {
                EXTDebugLabel.glLabelObjectEXT(37201, p_410006_.handle, StringUtil.truncateStringIfNecessary(supplier.get(), 256, true));
            }
        }

        @Override
        public void applyLabel(GlTexture p_410324_) {
            EXTDebugLabel.glLabelObjectEXT(5890, p_410324_.id, StringUtil.truncateStringIfNecessary(p_410324_.getLabel(), 256, true));
        }

        @Override
        public void applyLabel(GlShaderModule p_410674_) {
            EXTDebugLabel.glLabelObjectEXT(35656, p_410674_.getShaderId(), StringUtil.truncateStringIfNecessary(p_410674_.getDebugLabel(), 256, true));
        }

        @Override
        public void applyLabel(GlProgram p_409850_) {
            EXTDebugLabel.glLabelObjectEXT(35648, p_409850_.getProgramId(), StringUtil.truncateStringIfNecessary(p_409850_.getDebugLabel(), 256, true));
        }

        @Override
        public void applyLabel(VertexArrayCache.VertexArray p_409615_) {
            EXTDebugLabel.glLabelObjectEXT(32884, p_409615_.id, StringUtil.truncateStringIfNecessary(p_409615_.format.toString(), 256, true));
        }

        @Override
        public boolean exists() {
            return true;
        }
    }
}
