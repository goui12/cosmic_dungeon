package net.minecraft.client.gui.screens.dialog.input;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.MapCodec;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.layouts.CommonLayouts;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.dialog.action.Action;
import net.minecraft.server.dialog.input.BooleanInput;
import net.minecraft.server.dialog.input.InputControl;
import net.minecraft.server.dialog.input.NumberRangeInput;
import net.minecraft.server.dialog.input.SingleOptionInput;
import net.minecraft.server.dialog.input.TextInput;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class InputControlHandlers {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<MapCodec<? extends InputControl>, InputControlHandler<?>> HANDLERS = new HashMap<>();

    private static <T extends InputControl> void register(MapCodec<T> p_425691_, InputControlHandler<? super T> p_426293_) {
        HANDLERS.put(p_425691_, p_426293_);
    }

    @Nullable
    private static <T extends InputControl> InputControlHandler<T> get(T p_426260_) {
        return (InputControlHandler<T>)HANDLERS.get(p_426260_.mapCodec());
    }

    public static <T extends InputControl> void createHandler(T p_426216_, Screen p_425515_, InputControlHandler.Output p_425712_) {
        InputControlHandler<T> inputcontrolhandler = get(p_426216_);
        if (inputcontrolhandler == null) {
            LOGGER.warn("Unrecognized input control {}", p_426216_);
        } else {
            inputcontrolhandler.addControl(p_426216_, p_425515_, p_425712_);
        }
    }

    public static void bootstrap() {
        register(TextInput.MAP_CODEC, new InputControlHandlers.TextInputHandler());
        register(SingleOptionInput.MAP_CODEC, new InputControlHandlers.SingleOptionHandler());
        register(BooleanInput.MAP_CODEC, new InputControlHandlers.BooleanHandler());
        register(NumberRangeInput.MAP_CODEC, new InputControlHandlers.NumberRangeHandler());
    }

    @OnlyIn(Dist.CLIENT)
    static class BooleanHandler implements InputControlHandler<BooleanInput> {
        public void addControl(final BooleanInput p_426172_, Screen p_426214_, InputControlHandler.Output p_426199_) {
            Font font = p_426214_.getFont();
            final Checkbox checkbox = Checkbox.builder(p_426172_.label(), font).selected(p_426172_.initial()).build();
            p_426199_.accept(checkbox, new Action.ValueGetter() {
                @Override
                public String asTemplateSubstitution() {
                    return checkbox.selected() ? p_426172_.onTrue() : p_426172_.onFalse();
                }

                @Override
                public Tag asTag() {
                    return ByteTag.valueOf(checkbox.selected());
                }
            });
        }
    }

    @OnlyIn(Dist.CLIENT)
    static class NumberRangeHandler implements InputControlHandler<NumberRangeInput> {
        public void addControl(NumberRangeInput p_425719_, Screen p_426306_, InputControlHandler.Output p_425726_) {
            float f = p_425719_.rangeInfo().initialSliderValue();
            final InputControlHandlers.NumberRangeHandler.SliderImpl inputcontrolhandlers$numberrangehandler$sliderimpl = new InputControlHandlers.NumberRangeHandler.SliderImpl(
                p_425719_, f
            );
            p_425726_.accept(inputcontrolhandlers$numberrangehandler$sliderimpl, new Action.ValueGetter() {
                @Override
                public String asTemplateSubstitution() {
                    return inputcontrolhandlers$numberrangehandler$sliderimpl.stringValueToSend();
                }

                @Override
                public Tag asTag() {
                    return FloatTag.valueOf(inputcontrolhandlers$numberrangehandler$sliderimpl.floatValueToSend());
                }
            });
        }

        @OnlyIn(Dist.CLIENT)
        static class SliderImpl extends AbstractSliderButton {
            private final NumberRangeInput input;

            SliderImpl(NumberRangeInput p_426229_, double p_426263_) {
                super(0, 0, p_426229_.width(), 20, computeMessage(p_426229_, p_426263_), p_426263_);
                this.input = p_426229_;
            }

            @Override
            protected void updateMessage() {
                this.setMessage(computeMessage(this.input, this.value));
            }

            @Override
            protected void applyValue() {
            }

            public String stringValueToSend() {
                return sliderValueToString(this.input, this.value);
            }

            public float floatValueToSend() {
                return scaledValue(this.input, this.value);
            }

            private static float scaledValue(NumberRangeInput p_428329_, double p_428472_) {
                return p_428329_.rangeInfo().computeScaledValue((float)p_428472_);
            }

            private static String sliderValueToString(NumberRangeInput p_426239_, double p_425642_) {
                return valueToString(scaledValue(p_426239_, p_425642_));
            }

            private static Component computeMessage(NumberRangeInput p_426318_, double p_426116_) {
                return p_426318_.computeLabel(sliderValueToString(p_426318_, p_426116_));
            }

            private static String valueToString(float p_427421_) {
                int i = (int)p_427421_;
                return i == p_427421_ ? Integer.toString(i) : Float.toString(p_427421_);
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    static class SingleOptionHandler implements InputControlHandler<SingleOptionInput> {
        public void addControl(SingleOptionInput p_425545_, Screen p_425696_, InputControlHandler.Output p_426241_) {
            CycleButton.Builder<SingleOptionInput.Entry> builder = CycleButton.builder(SingleOptionInput.Entry::displayOrDefault)
                .withValues(p_425545_.entries())
                .displayOnlyValue(!p_425545_.labelVisible());
            Optional<SingleOptionInput.Entry> optional = p_425545_.initial();
            if (optional.isPresent()) {
                builder = builder.withInitialValue(optional.get());
            }

            CycleButton<SingleOptionInput.Entry> cyclebutton = builder.create(0, 0, p_425545_.width(), 20, p_425545_.label());
            p_426241_.accept(cyclebutton, Action.ValueGetter.of(() -> cyclebutton.getValue().id()));
        }
    }

    @OnlyIn(Dist.CLIENT)
    static class TextInputHandler implements InputControlHandler<TextInput> {
        public void addControl(TextInput p_425780_, Screen p_425598_, InputControlHandler.Output p_425996_) {
            Font font = p_425598_.getFont();
            LayoutElement layoutelement;
            final Supplier<String> supplier;
            if (p_425780_.multiline().isPresent()) {
                TextInput.MultilineOptions textinput$multilineoptions = p_425780_.multiline().get();
                int i = textinput$multilineoptions.height().orElseGet(() -> {
                    int j = textinput$multilineoptions.maxLines().orElse(4);
                    return Math.min(9 * j + 8, 512);
                });
                MultiLineEditBox multilineeditbox = MultiLineEditBox.builder().build(font, p_425780_.width(), i, CommonComponents.EMPTY);
                multilineeditbox.setCharacterLimit(p_425780_.maxLength());
                textinput$multilineoptions.maxLines().ifPresent(multilineeditbox::setLineLimit);
                multilineeditbox.setValue(p_425780_.initial());
                layoutelement = multilineeditbox;
                supplier = multilineeditbox::getValue;
            } else {
                EditBox editbox = new EditBox(font, p_425780_.width(), 20, p_425780_.label());
                editbox.setMaxLength(p_425780_.maxLength());
                editbox.setValue(p_425780_.initial());
                layoutelement = editbox;
                supplier = editbox::getValue;
            }

            LayoutElement layoutelement1 = (LayoutElement)(p_425780_.labelVisible()
                ? CommonLayouts.labeledElement(font, layoutelement, p_425780_.label())
                : layoutelement);
            p_425996_.accept(layoutelement1, new Action.ValueGetter() {
                @Override
                public String asTemplateSubstitution() {
                    return StringTag.escapeWithoutQuotes(supplier.get());
                }

                @Override
                public Tag asTag() {
                    return StringTag.valueOf(supplier.get());
                }
            });
        }
    }
}
