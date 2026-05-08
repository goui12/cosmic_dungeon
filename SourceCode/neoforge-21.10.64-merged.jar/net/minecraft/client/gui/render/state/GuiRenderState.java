package net.minecraft.client.gui.render.state;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.SharedConstants;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.gui.render.state.pip.PictureInPictureRenderState;
import net.minecraft.client.renderer.RenderPipelines;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix3x2f;

@OnlyIn(Dist.CLIENT)
public class GuiRenderState {
    private static final int DEBUG_RECTANGLE_COLOR = 2000962815;
    private final List<GuiRenderState.Node> strata = new ArrayList<>();
    private int firstStratumAfterBlur = Integer.MAX_VALUE;
    private GuiRenderState.Node current;
    private final Set<Object> itemModelIdentities = new HashSet<>();
    @Nullable
    private ScreenRectangle lastElementBounds;

    public GuiRenderState() {
        this.nextStratum();
    }

    public void nextStratum() {
        this.current = new GuiRenderState.Node(null);
        this.strata.add(this.current);
    }

    public void blurBeforeThisStratum() {
        if (this.firstStratumAfterBlur != Integer.MAX_VALUE) {
            throw new IllegalStateException("Can only blur once per frame");
        } else {
            this.firstStratumAfterBlur = this.strata.size() - 1;
        }
    }

    public void up() {
        if (this.current.up == null) {
            this.current.up = new GuiRenderState.Node(this.current);
        }

        this.current = this.current.up;
    }

    public void submitItem(GuiItemRenderState renderState) {
        if (this.findAppropriateNode(renderState)) {
            this.itemModelIdentities.add(renderState.itemStackRenderState().getModelIdentity());
            this.current.submitItem(renderState);
            this.sumbitDebugRectangleIfEnabled(renderState.bounds());
        }
    }

    public void submitText(GuiTextRenderState renderState) {
        if (this.findAppropriateNode(renderState)) {
            this.current.submitText(renderState);
            this.sumbitDebugRectangleIfEnabled(renderState.bounds());
        }
    }

    public void submitPicturesInPictureState(PictureInPictureRenderState renderState) {
        if (this.findAppropriateNode(renderState)) {
            this.current.submitPicturesInPictureState(renderState);
            this.sumbitDebugRectangleIfEnabled(renderState.bounds());
        }
    }

    public void submitGuiElement(GuiElementRenderState renderState) {
        if (this.findAppropriateNode(renderState)) {
            this.current.submitGuiElement(renderState);
            this.sumbitDebugRectangleIfEnabled(renderState.bounds());
        }
    }

    private void sumbitDebugRectangleIfEnabled(@Nullable ScreenRectangle debugRectangle) {
        if (SharedConstants.DEBUG_RENDER_UI_LAYERING_RECTANGLES && debugRectangle != null) {
            this.up();
            this.current
                .submitGuiElement(
                    new ColoredRectangleRenderState(
                        RenderPipelines.GUI, TextureSetup.noTexture(), new Matrix3x2f(), 0, 0, 10000, 10000, 2000962815, 2000962815, debugRectangle
                    )
                );
        }
    }

    private boolean findAppropriateNode(ScreenArea screenArea) {
        ScreenRectangle screenrectangle = screenArea.bounds();
        if (screenrectangle == null) {
            return false;
        } else {
            if (this.lastElementBounds != null && this.lastElementBounds.encompasses(screenrectangle)) {
                this.up();
            } else {
                this.navigateToAboveHighestElementWithIntersectingBounds(screenrectangle);
            }

            this.lastElementBounds = screenrectangle;
            return true;
        }
    }

    private void navigateToAboveHighestElementWithIntersectingBounds(ScreenRectangle rectangle) {
        GuiRenderState.Node guirenderstate$node = this.strata.getLast();

        while (guirenderstate$node.up != null) {
            guirenderstate$node = guirenderstate$node.up;
        }

        boolean flag = false;

        while (!flag) {
            flag = this.hasIntersection(rectangle, guirenderstate$node.elementStates)
                || this.hasIntersection(rectangle, guirenderstate$node.itemStates)
                || this.hasIntersection(rectangle, guirenderstate$node.textStates)
                || this.hasIntersection(rectangle, guirenderstate$node.picturesInPictureStates);
            if (guirenderstate$node.parent == null) {
                break;
            }

            if (!flag) {
                guirenderstate$node = guirenderstate$node.parent;
            }
        }

        this.current = guirenderstate$node;
        if (flag) {
            this.up();
        }
    }

    private boolean hasIntersection(ScreenRectangle rectangle, @Nullable List<? extends ScreenArea> screenAreas) {
        if (screenAreas != null) {
            for (ScreenArea screenarea : screenAreas) {
                ScreenRectangle screenrectangle = screenarea.bounds();
                if (screenrectangle != null && screenrectangle.intersects(rectangle)) {
                    return true;
                }
            }
        }

        return false;
    }

    public void submitBlitToCurrentLayer(BlitRenderState renderState) {
        this.current.submitGuiElement(renderState);
    }

    public void submitGlyphToCurrentLayer(GuiElementRenderState renderState) {
        this.current.submitGlyph(renderState);
    }

    public Set<Object> getItemModelIdentities() {
        return this.itemModelIdentities;
    }

    public void forEachElement(Consumer<GuiElementRenderState> action, GuiRenderState.TraverseRange traverseRange) {
        this.traverse(p_436488_ -> {
            if (p_436488_.elementStates != null || p_436488_.glyphStates != null) {
                if (p_436488_.elementStates != null) {
                    for (GuiElementRenderState guielementrenderstate : p_436488_.elementStates) {
                        action.accept(guielementrenderstate);
                    }
                }

                if (p_436488_.glyphStates != null) {
                    for (GuiElementRenderState guielementrenderstate1 : p_436488_.glyphStates) {
                        action.accept(guielementrenderstate1);
                    }
                }
            }
        }, traverseRange);
    }

    public void forEachItem(Consumer<GuiItemRenderState> action) {
        GuiRenderState.Node guirenderstate$node = this.current;
        this.traverse(p_418061_ -> {
            if (p_418061_.itemStates != null) {
                this.current = p_418061_;

                for (GuiItemRenderState guiitemrenderstate : p_418061_.itemStates) {
                    action.accept(guiitemrenderstate);
                }
            }
        }, GuiRenderState.TraverseRange.ALL);
        this.current = guirenderstate$node;
    }

    public void forEachText(Consumer<GuiTextRenderState> action) {
        GuiRenderState.Node guirenderstate$node = this.current;
        this.traverse(p_421279_ -> {
            if (p_421279_.textStates != null) {
                for (GuiTextRenderState guitextrenderstate : p_421279_.textStates) {
                    this.current = p_421279_;
                    action.accept(guitextrenderstate);
                }
            }
        }, GuiRenderState.TraverseRange.ALL);
        this.current = guirenderstate$node;
    }

    public void forEachPictureInPicture(Consumer<PictureInPictureRenderState> action) {
        GuiRenderState.Node guirenderstate$node = this.current;
        this.traverse(p_418372_ -> {
            if (p_418372_.picturesInPictureStates != null) {
                this.current = p_418372_;

                for (PictureInPictureRenderState pictureinpicturerenderstate : p_418372_.picturesInPictureStates) {
                    action.accept(pictureinpicturerenderstate);
                }
            }
        }, GuiRenderState.TraverseRange.ALL);
        this.current = guirenderstate$node;
    }

    public void sortElements(Comparator<GuiElementRenderState> comparator) {
        this.traverse(p_448686_ -> {
            if (p_448686_.elementStates != null) {
                if (SharedConstants.DEBUG_SHUFFLE_UI_RENDERING_ORDER) {
                    Collections.shuffle(p_448686_.elementStates);
                }

                p_448686_.elementStates.sort(comparator);
            }
        }, GuiRenderState.TraverseRange.ALL);
    }

    private void traverse(Consumer<GuiRenderState.Node> action, GuiRenderState.TraverseRange traverseRange) {
        int i = 0;
        int j = this.strata.size();
        if (traverseRange == GuiRenderState.TraverseRange.BEFORE_BLUR) {
            j = Math.min(this.firstStratumAfterBlur, this.strata.size());
        } else if (traverseRange == GuiRenderState.TraverseRange.AFTER_BLUR) {
            i = this.firstStratumAfterBlur;
        }

        for (int k = i; k < j; k++) {
            GuiRenderState.Node guirenderstate$node = this.strata.get(k);
            this.traverse(guirenderstate$node, action);
        }
    }

    private void traverse(GuiRenderState.Node node, Consumer<GuiRenderState.Node> action) {
        action.accept(node);
        if (node.up != null) {
            this.traverse(node.up, action);
        }
    }

    public void reset() {
        this.itemModelIdentities.clear();
        this.strata.clear();
        this.firstStratumAfterBlur = Integer.MAX_VALUE;
        this.nextStratum();
    }

    @OnlyIn(Dist.CLIENT)
    static class Node {
        @Nullable
        public final GuiRenderState.Node parent;
        @Nullable
        public GuiRenderState.Node up;
        @Nullable
        public List<GuiElementRenderState> elementStates;
        @Nullable
        public List<GuiElementRenderState> glyphStates;
        @Nullable
        public List<GuiItemRenderState> itemStates;
        @Nullable
        public List<GuiTextRenderState> textStates;
        @Nullable
        public List<PictureInPictureRenderState> picturesInPictureStates;

        Node(@Nullable GuiRenderState.Node parent) {
            this.parent = parent;
        }

        public void submitItem(GuiItemRenderState renderState) {
            if (this.itemStates == null) {
                this.itemStates = new ArrayList<>();
            }

            this.itemStates.add(renderState);
        }

        public void submitText(GuiTextRenderState renderState) {
            if (this.textStates == null) {
                this.textStates = new ArrayList<>();
            }

            this.textStates.add(renderState);
        }

        public void submitPicturesInPictureState(PictureInPictureRenderState renderState) {
            if (this.picturesInPictureStates == null) {
                this.picturesInPictureStates = new ArrayList<>();
            }

            this.picturesInPictureStates.add(renderState);
        }

        public void submitGuiElement(GuiElementRenderState renderState) {
            if (this.elementStates == null) {
                this.elementStates = new ArrayList<>();
            }

            this.elementStates.add(renderState);
        }

        public void submitGlyph(GuiElementRenderState renderState) {
            if (this.glyphStates == null) {
                this.glyphStates = new ArrayList<>();
            }

            this.glyphStates.add(renderState);
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static enum TraverseRange {
        ALL,
        BEFORE_BLUR,
        AFTER_BLUR;
    }
}
