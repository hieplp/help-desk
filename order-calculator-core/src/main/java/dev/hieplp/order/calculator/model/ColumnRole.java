package dev.hieplp.order.calculator.model;

import dev.hieplp.order.calculator.OrderMetadata;

/**
 * The three numeric inputs needed for the math, identified by role — the actual
 * column names are bound in {@link OrderMetadata}.
 */
public enum ColumnRole {
    QUANTITY,
    UNIT_PRICE,
    VAT_PERCENT
}
