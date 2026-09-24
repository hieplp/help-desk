package dev.hieplp.order.calculator.model;

import java.math.BigDecimal;

/**
 * Built-in computed column types.
 */
public enum ComputedType {
    /**
     * quantity × unit_price
     */
    LINE_TOTAL_BEFORE_TAX,
    /**
     * line_total_before_tax × (1 + vat/100)
     */
    LINE_TOTAL_AFTER_TAX;

    /**
     * Picks the matching value from a (beforeTax, afterTax) pair.
     */
    public BigDecimal pick(BigDecimal beforeTax, BigDecimal afterTax) {
        return this == LINE_TOTAL_BEFORE_TAX ? beforeTax : afterTax;
    }
}
