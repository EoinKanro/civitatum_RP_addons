package team.civitatum.inventory.client;

import java.util.function.DoubleConsumer;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;

public final class InventorySlider extends AbstractSliderButton {
    /** Slot scale moves in 5% steps, transparency and the HUD sizes in 1% steps. */
    private static final double SCALE_STEPS = 20, PERCENT_STEPS = 100;
    private final String label;
    private final double min, max;
    private final DoubleConsumer change;

    public InventorySlider(String label, double current, double min, double max, DoubleConsumer change) {
        super(0, 0, 158, 18, Component.empty(), (current - min) / (max - min));
        this.label = label;
        this.min = min;
        this.max = max;
        this.change = change;
        updateMessage();
    }

    @Override
    protected void updateMessage() {
        setMessage(CivitatumInventoryScreen.tr(label, Math.round((min + (max - min) * value) * 100)));
    }

    @Override
    protected void applyValue() {
        // The slot scale is the only slider that starts at 1 (100%).
        double steps = min == 1 ? SCALE_STEPS : PERCENT_STEPS;
        double actual = Math.round((min + (max - min) * value) * steps) / steps;
        value = (actual - min) / (max - min);
        change.accept(actual);
    }

    @Override
    public void onRelease(double x, double y) {
        super.onRelease(x, y);
        InventoryPreferences.get().save();
    }
}
