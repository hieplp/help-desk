package dev.hieplp.order.calculator.model;

import dev.hieplp.order.calculator.spi.TabularRow;

import java.math.BigDecimal;

/**
 * Read-only view of the row currently being processed, handed to custom
 * {@link ComputedColumn}s.
 */
public interface RowView {

    /**
     * Pass-through access to the input row.
     */
    String text(String column);

    /**
     * Numeric access to the input row (see {@link TabularRow#number}).
     */
    BigDecimal number(String column);

    /**
     * A value already computed for {@code type} on the current line — lets
     * custom columns build on the built-in totals (e.g. tax amount =
     * AFTER − BEFORE).
     */
    BigDecimal computed(ComputedType type);
}
