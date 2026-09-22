package com.talesforge.masternpc.client.gui.element;

import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.function.DoubleConsumer;

public class ValueSlider extends AbstractSliderButton {
    private final Component label;
    private final double min, max;
    private final DoubleConsumer onChange;
    private final double sliderStep;

    public ValueSlider(int x, int y, int width, int height, Component label,
                       double min, double max, double current, DoubleConsumer onChange) {
        this(x, y, width, height, label, min, max, current, 1.0, onChange);
    }

    public ValueSlider(int x, int y, int width, int height, Component label,
                       double min, double max, double current, double sliderStep, DoubleConsumer onChange) {
        super(x, y, width, height, Component.empty(), (max == min) ? 0.0 : (current - min) / (max - min));

        this.label = label;
        this.min = min;
        this.max = max;
        this.onChange = onChange;
        this.sliderStep = sliderStep;

        updateMessage();
    }

    private double real() {
        return Mth.lerp(this.value, min, max);
    }

    @Override
    protected void updateMessage() {
        setMessage(Component.empty().append(label).append(String.format(": %.2f", real())));
    }

    @Override
    protected void applyValue() {
        // Sticking to the step: round this.value to the nearest multiple of the step value
        if (sliderStep > 0 && max != min) {
            double realValue = Mth.lerp(this.value, min, max);
            double snappedReal = Math.round(realValue / sliderStep) * sliderStep;
            snappedReal = Math.max(min, Math.min(max, snappedReal));
            this.value = (snappedReal - min) / (max - min);
        }
        onChange.accept(real());
    }
}
