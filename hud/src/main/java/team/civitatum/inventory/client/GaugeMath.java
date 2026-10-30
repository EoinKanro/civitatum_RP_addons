package team.civitatum.inventory.client;

/**
 * What other mods' getters return, as a bar from 0 to 1. They return pixels of their own bar, raw ticks or flags,
 * so each value is brought to one scale here.
 */
final class GaugeMath {
    /** {@code value} over {@code divisor}; a flag counts as full or empty, anything else as empty. */
    static double fraction(Object value, double divisor) {
        return Math.clamp(number(value) / divisor, 0, 1);
    }

    /** One value over another, such as time cooked over time needed; empty while nothing is needed. */
    static double ratio(Object done, Object total) {
        double whole = number(total);
        return whole > 0 ? Math.clamp(number(done) / whole, 0, 1) : 0;
    }

    private static double number(Object value) {
        if (value instanceof Boolean flag) return flag ? 1 : 0;
        return value instanceof Number n ? n.doubleValue() : 0;
    }

    private GaugeMath() {}
}
