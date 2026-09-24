package dev.hieplp.order.calculator;

import dev.hieplp.order.calculator.error.ErrorStrategy;
import dev.hieplp.order.calculator.error.OrderProcessingException;
import dev.hieplp.order.calculator.model.ColumnRole;
import dev.hieplp.order.calculator.model.OutputColumn;

import java.math.RoundingMode;
import java.util.*;

/**
 * Caller-supplied metadata — the "nothing is fixed" part: binds semantic roles
 * to actual input column names and describes the output layout. Immutable.
 *
 * @param bindings            role → input column name; every {@link ColumnRole} required
 * @param outputColumns       ordered output columns; empty = default layout —
 *                            input columns in order, then the two totals
 * @param summaryMarker       literal written into the marker column of the summary row
 * @param summaryMarkerColumn output column receiving {@code summaryMarker};
 *                            {@code null} → first column
 * @param outputScale         decimal places for computed output values
 * @param roundingMode        rounding applied when scaling
 * @param sumRoundedLines     whether order totals accumulate rounded line values
 * @param errorStrategy       row-level error handling; schema errors are always fatal
 * @param allowNegative       whether negative quantity/price/vat values are accepted
 */
public record OrderMetadata(
        Map<ColumnRole, String> bindings,
        List<OutputColumn> outputColumns,
        String summaryMarker,
        String summaryMarkerColumn,
        int outputScale,
        RoundingMode roundingMode,
        boolean sumRoundedLines,
        ErrorStrategy errorStrategy,
        boolean allowNegative
) {

    public OrderMetadata {
        Objects.requireNonNull(bindings, "bindings");
        for (ColumnRole role : ColumnRole.values()) {
            if (!bindings.containsKey(role)) {
                throw new OrderProcessingException("missing required role binding: " + role);
            }
        }

        bindings = Collections.unmodifiableMap(new EnumMap<>(bindings));
        outputColumns = List.copyOf(outputColumns);

        Objects.requireNonNull(summaryMarker, "summaryMarker");
        if (outputScale < 0) {
            throw new OrderProcessingException("outputScale must be >= 0, got " + outputScale);
        }
        Objects.requireNonNull(roundingMode, "roundingMode");
        Objects.requireNonNull(errorStrategy, "errorStrategy");
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Input column name bound to {@code role}.
     */
    public String columnName(ColumnRole role) {
        return bindings.get(role);
    }

    public static final class Builder {

        private final EnumMap<ColumnRole, String> bindings = new EnumMap<>(ColumnRole.class);

        private List<OutputColumn> outputColumns = List.of();

        private String summaryMarker = "TOTAL";

        private String summaryMarkerColumn;

        private int outputScale = 2;

        private RoundingMode roundingMode = RoundingMode.HALF_UP;

        private boolean sumRoundedLines = true;

        private ErrorStrategy errorStrategy = ErrorStrategy.FAIL_FAST;

        private boolean allowNegative;

        private Builder() {
        }

        public Builder bind(ColumnRole role, String columnName) {
            bindings.put(
                    Objects.requireNonNull(role, "role"),
                    Objects.requireNonNull(columnName, "columnName")
            );
            return this;
        }

        /**
         * Explicit output layout; omit for the default layout.
         */
        public Builder outputColumns(List<OutputColumn> columns) {
            this.outputColumns = List.copyOf(Objects.requireNonNull(columns, "columns"));
            return this;
        }

        public Builder outputColumns(OutputColumn... columns) {
            return outputColumns(List.of(columns));
        }

        public Builder summaryMarker(String marker) {
            this.summaryMarker = Objects.requireNonNull(marker, "marker");
            return this;
        }

        /**
         * Column receiving {@link #summaryMarker}; default = first output column.
         */
        public Builder summaryMarkerColumn(String columnName) {
            this.summaryMarkerColumn = Objects.requireNonNull(columnName, "columnName");
            return this;
        }

        public Builder outputScale(int scale) {
            this.outputScale = scale;
            return this;
        }

        public Builder roundingMode(RoundingMode mode) {
            this.roundingMode = Objects.requireNonNull(mode, "mode");
            return this;
        }

        public Builder sumRoundedLines(boolean value) {
            this.sumRoundedLines = value;
            return this;
        }

        public Builder errorStrategy(ErrorStrategy strategy) {
            this.errorStrategy = Objects.requireNonNull(strategy, "strategy");
            return this;
        }

        public Builder allowNegative(boolean value) {
            this.allowNegative = value;
            return this;
        }

        public OrderMetadata build() {
            return new OrderMetadata(
                    bindings,
                    outputColumns,
                    summaryMarker,
                    summaryMarkerColumn,
                    outputScale,
                    roundingMode,
                    sumRoundedLines,
                    errorStrategy,
                    allowNegative
            );
        }
    }
}
