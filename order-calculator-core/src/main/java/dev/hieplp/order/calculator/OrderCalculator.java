package dev.hieplp.order.calculator;

import dev.hieplp.order.calculator.error.ErrorStrategy;
import dev.hieplp.order.calculator.error.OrderProcessingException;
import dev.hieplp.order.calculator.error.RowError;
import dev.hieplp.order.calculator.model.*;
import dev.hieplp.order.calculator.spi.TabularRow;
import dev.hieplp.order.calculator.spi.TabularSink;
import dev.hieplp.order.calculator.spi.TabularSource;

import java.math.BigDecimal;
import java.util.*;

/**
 * The format-agnostic engine: streams rows from a {@link TabularSource},
 * enriches them per {@link OrderMetadata}, writes them plus a summary row to a
 * {@link TabularSink}, and returns order totals.
 *
 * <p>Stateless and thread-safe; sources and sinks are not.
 */
public final class OrderCalculator {

    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final String DEFAULT_BEFORE_NAME = "total_price_before_tax";
    private static final String DEFAULT_AFTER_NAME = "total_price_after_tax";

    private OrderCalculator() {
    }

    // ---- schema ----

    /**
     * Explicit layout, or input columns + the two totals.
     */
    private static List<OutputColumn> resolveLayout(List<String> header, OrderMetadata meta) {
        if (!meta.outputColumns().isEmpty()) {
            return meta.outputColumns();
        }
        List<OutputColumn> layout = new ArrayList<>(header.size() + 2);
        for (String col : header) {
            layout.add(new OutputColumn.PassThrough(col, col));
        }
        layout.add(new OutputColumn.Computed(ComputedType.LINE_TOTAL_BEFORE_TAX, DEFAULT_BEFORE_NAME));
        layout.add(new OutputColumn.Computed(ComputedType.LINE_TOTAL_AFTER_TAX, DEFAULT_AFTER_NAME));
        return layout;
    }

    /**
     * Schema errors are always fatal, raised before processing.
     */
    private static void validateSchema(
            List<String> header, List<OutputColumn> layout,
            OrderMetadata meta
    ) {
        if (header.isEmpty()) {
            throw new OrderProcessingException("missing header row");
        }

        Set<String> seen = new HashSet<>();
        for (String col : header) {
            if (!seen.add(col)) {
                throw new OrderProcessingException("duplicate header name: '" + col + "'");
            }
        }

        for (var entry : meta.bindings().entrySet()) {
            if (!seen.contains(entry.getValue())) {
                throw new OrderProcessingException(
                        "missing bound column '" + entry.getValue() + "' for role " + entry.getKey()
                );
            }
        }

        Set<String> outputNames = new HashSet<>();
        for (OutputColumn col : layout) {
            if (!outputNames.add(col.outputName())) {
                throw new OrderProcessingException("duplicate output name: '" + col.outputName() + "'");
            }

            if (col instanceof OutputColumn.PassThrough p && !seen.contains(p.inputName())) {
                throw new OrderProcessingException(
                        "output column references missing input: '" + p.inputName() + "'"
                );
            }
        }

        String markerCol = meta.summaryMarkerColumn();
        if (markerCol != null && !outputNames.contains(markerCol)) {
            throw new OrderProcessingException(
                    "summary marker column is not an output column: '" + markerCol + "'"
            );
        }
    }

    // ---- row loop helpers ----

    private static boolean hasNext(Iterator<TabularRow> rows) {
        try {
            return rows.hasNext();
        } catch (RuntimeException e) {
            throw new OrderProcessingException("failed reading input rows", e);
        }
    }

    private static TabularRow next(Iterator<TabularRow> rows) {
        try {
            return rows.next();
        } catch (RuntimeException e) {
            throw new OrderProcessingException("failed reading input row", e);
        }
    }

    private static BigDecimal number(TabularRow row, String column) {
        try {
            return row.number(column);
        } catch (RuntimeException e) {
            throw new OrderProcessingException(
                    new RowError(
                            row.lineNumber(),
                            column,
                            safeText(row, column),
                            message(e)
                    )
            );
        }
    }

    private static String safeText(TabularRow row, String column) {
        try {
            return row.text(column);
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static void checkNegative(TabularRow row, String column, BigDecimal value,
                                      OrderMetadata meta) {
        if (!meta.allowNegative() && value.signum() < 0) {
            throw new OrderProcessingException(
                    new RowError(
                            row.lineNumber(),
                            column,
                            value.toPlainString(),
                            "negative value not allowed")
            );
        }
    }

    private static BigDecimal scale(BigDecimal value, OrderMetadata meta) {
        return value.setScale(meta.outputScale(), meta.roundingMode());
    }

    private static List<CellValue> buildCells(
            TabularRow row,
            List<OutputColumn> layout,
            BigDecimal beforeOut,
            BigDecimal afterOut
    ) {
        RowView view = new LineView(row, beforeOut, afterOut);
        List<CellValue> cells = new ArrayList<>(layout.size());
        for (OutputColumn col : layout) {
            switch (col) {
                case OutputColumn.PassThrough p -> cells.add(CellValue.text(text(row, p.inputName())));
                case OutputColumn.Computed c -> cells.add(CellValue.number(c.type().pick(beforeOut, afterOut)));
                case OutputColumn.Custom cu -> cells.add(cu.function().compute(view));
            }
        }
        return cells;
    }

    private static String text(TabularRow row, String column) {
        try {
            return row.text(column);
        } catch (RuntimeException e) {
            throw new OrderProcessingException(
                    new RowError(
                            row.lineNumber(),
                            column,
                            null,
                            message(e)
                    )
            );
        }
    }

    /**
     * Marker in the configured (or first) column, totals numeric, rest empty.
     */
    private static void writeSummaryRow(
            TabularSink sink,
            List<OutputColumn> layout,
            OrderMetadata meta,
            BigDecimal totalBefore,
            BigDecimal totalAfter
    ) {
        String markerCol = Objects.requireNonNullElse(
                meta.summaryMarkerColumn(), layout.get(0).outputName()
        );
        List<CellValue> cells = new ArrayList<>(layout.size());
        for (OutputColumn col : layout) {
            if (col.outputName().equals(markerCol)) {
                cells.add(CellValue.text(meta.summaryMarker()));
            } else if (col instanceof OutputColumn.Computed c) {
                cells.add(CellValue.number(scale(c.type().pick(totalBefore, totalAfter), meta)));
            } else {
                cells.add(CellValue.empty());
            }
        }
        sink.writeRow(cells);
    }

    // ---- sink ----

    private static String message(RuntimeException e) {
        String msg = e.getMessage();
        return msg != null ? msg : e.getClass().getSimpleName();
    }

    /**
     * Runs the pipeline: schema validation → streaming row loop (compute,
     * accumulate, write) → summary row → {@link OrderResult}.
     */
    public static OrderResult process(TabularSource source, TabularSink sink, OrderMetadata metadata) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(sink, "sink");
        Objects.requireNonNull(metadata, "metadata");

        List<String> header = source.header();
        List<OutputColumn> layout = resolveLayout(header, metadata);
        validateSchema(header, layout, metadata);

        sink.writeHeader(layout.stream().map(OutputColumn::outputName).toList());

        String qtyCol = metadata.columnName(ColumnRole.QUANTITY);
        String priceCol = metadata.columnName(ColumnRole.UNIT_PRICE);
        String vatCol = metadata.columnName(ColumnRole.VAT_PERCENT);

        long lineCount = 0;
        long skipped = 0;
        BigDecimal totalBefore = BigDecimal.ZERO;
        BigDecimal totalAfter = BigDecimal.ZERO;
        List<RowError> errors = new ArrayList<>();

        Iterator<TabularRow> rows = source.rows().iterator();
        while (hasNext(rows)) {
            TabularRow row = next(rows);
            List<CellValue> cells;
            BigDecimal before;
            BigDecimal after;
            BigDecimal beforeOut;
            BigDecimal afterOut;
            try {
                BigDecimal qty = number(row, qtyCol);
                BigDecimal price = number(row, priceCol);
                BigDecimal vat = number(row, vatCol);
                checkNegative(row, qtyCol, qty, metadata);
                checkNegative(row, priceCol, price, metadata);
                checkNegative(row, vatCol, vat, metadata);

                before = qty.multiply(price);
                after = before.multiply(BigDecimal.ONE.add(vat.divide(HUNDRED)));
                beforeOut = scale(before, metadata);
                afterOut = scale(after, metadata);

                cells = buildCells(row, layout, beforeOut, afterOut);
            } catch (RuntimeException e) {
                RowError err = e instanceof OrderProcessingException ope && ope.rowError() != null
                        ? ope.rowError()
                        : new RowError(row.lineNumber(), null, null, message(e));
                if (metadata.errorStrategy() == ErrorStrategy.FAIL_FAST) {
                    throw new OrderProcessingException(err);
                }
                errors.add(err);
                skipped++;
                continue;
            }

            sink.writeRow(cells);
            lineCount++;
            totalBefore = totalBefore.add(metadata.sumRoundedLines() ? beforeOut : before);
            totalAfter = totalAfter.add(metadata.sumRoundedLines() ? afterOut : after);
        }

        writeSummaryRow(sink, layout, metadata, totalBefore, totalAfter);
        return new OrderResult(lineCount, skipped, totalBefore, totalAfter, errors);
    }

    private record LineView(
            TabularRow row,
            BigDecimal beforeOut,
            BigDecimal afterOut
    ) implements RowView {

        public String text(String column) {
            return row.text(column);
        }

        public BigDecimal number(String column) {
            return row.number(column);
        }

        public BigDecimal computed(ComputedType type) {
            return type.pick(beforeOut, afterOut);
        }

    }
}
