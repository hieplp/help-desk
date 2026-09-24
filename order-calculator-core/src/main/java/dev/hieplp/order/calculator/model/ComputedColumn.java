package dev.hieplp.order.calculator.model;

/**
 * Caller-provided derived column.
 */
@FunctionalInterface
public interface ComputedColumn {

    /**
     * Derives one output cell for the current row.
     */
    CellValue compute(RowView row);
}
