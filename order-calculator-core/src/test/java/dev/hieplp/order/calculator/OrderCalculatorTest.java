package dev.hieplp.order.calculator;

import dev.hieplp.order.calculator.error.ErrorStrategy;
import dev.hieplp.order.calculator.error.OrderProcessingException;
import dev.hieplp.order.calculator.error.RowError;
import dev.hieplp.order.calculator.model.CellValue;
import dev.hieplp.order.calculator.model.ColumnRole;
import dev.hieplp.order.calculator.model.ComputedType;
import dev.hieplp.order.calculator.model.OutputColumn;
import dev.hieplp.order.calculator.spi.TabularRow;
import dev.hieplp.order.calculator.spi.TabularSink;
import dev.hieplp.order.calculator.spi.TabularSource;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Engine coverage through the public SPI only — doubles as proof that custom
 * formats can be added by implementing {@link TabularSource}/{@link TabularSink}.
 */
class OrderCalculatorTest {

    // ---- in-memory SPI stubs ----

    private static TabularSource source(List<String> header, String[]... rows) {
        return new TabularSource() {
            @Override
            public List<String> header() {
                return header;
            }

            @Override
            public Stream<TabularRow> rows() {
                return IntStream.range(0, rows.length)
                        .mapToObj(i -> stubRow(header, rows[i], i + 1));
            }

            @Override
            public void close() {
            }
        };
    }

    private static TabularRow stubRow(List<String> header, String[] cells, long line) {
        return new TabularRow() {
            private int indexOf(String column) {
                int i = header.indexOf(column);
                if (i < 0) {
                    throw new IllegalArgumentException("unknown column '" + column + "'");
                }
                return i;
            }

            @Override
            public String text(String column) {
                int i = indexOf(column);
                return i < cells.length ? cells[i] : "";
            }

            @Override
            public BigDecimal number(String column) {
                String raw = text(column);
                if (raw.isEmpty()) {
                    throw new IllegalStateException("empty cell");
                }
                return new BigDecimal(raw);
            }

            @Override
            public long lineNumber() {
                return line;
            }
        };
    }

    private static String render(CellValue cell) {
        return switch (cell) {
            case CellValue.Text t -> t.value();
            case CellValue.Number n -> n.value().toPlainString();
        };
    }

    private static List<String> render(List<CellValue> row) {
        return row.stream().map(OrderCalculatorTest::render).toList();
    }

    private static OrderMetadata.Builder meta() {
        return OrderMetadata.builder()
                .bind(ColumnRole.QUANTITY, "qty")
                .bind(ColumnRole.UNIT_PRICE, "price")
                .bind(ColumnRole.VAT_PERCENT, "vat");
    }

    private static TabularSource sampleSource() {
        return source(
                List.of("sku", "qty", "price", "vat"),
                new String[]{"P1", "2", "10.00", "10"},
                new String[]{"P2", "1", "5.50", "20"},
                new String[]{"P3", "3", "3.33", "8.5"});
    }

    private static BigDecimal scale(BigDecimal v) {
        return v.setScale(2, RoundingMode.HALF_UP);
    }

    // ---- happy path ----

    @Test
    void defaultLayoutEnrichesRowsAndAppendsSummary() {
        CapturingSink sink = new CapturingSink();
        OrderResult r = OrderCalculator.process(sampleSource(), sink, meta().build());

        assertEquals(3, r.lineCount());
        assertEquals(0, r.skippedCount());
        assertEquals(new BigDecimal("35.49"), r.orderTotalBeforeTax());
        assertEquals(new BigDecimal("39.44"), r.orderTotalAfterTax());
        assertTrue(r.errors().isEmpty());

        assertEquals(
                List.of("sku", "qty", "price", "vat",
                        "total_price_before_tax", "total_price_after_tax"),
                sink.header);
        assertEquals(List.of(
                        List.of("P1", "2", "10.00", "10", "20.00", "22.00"),
                        List.of("P2", "1", "5.50", "20", "5.50", "6.60"),
                        List.of("P3", "3", "3.33", "8.5", "9.99", "10.84"),
                        List.of("TOTAL", "", "", "", "35.49", "39.44")),
                sink.rows.stream().map(OrderCalculatorTest::render).toList());
    }

    @Test
    void computedCellsAreTypedNumbersAndPassThroughIsText() {
        CapturingSink sink = new CapturingSink();
        OrderCalculator.process(sampleSource(), sink, meta().build());

        List<CellValue> first = sink.rows.get(0);
        assertInstanceOf(CellValue.Text.class, first.get(0));
        assertInstanceOf(CellValue.Number.class, first.get(4));
        assertInstanceOf(CellValue.Number.class, first.get(5));

        List<CellValue> summary = sink.rows.get(sink.rows.size() - 1);
        assertInstanceOf(CellValue.Number.class, summary.get(4));
        assertInstanceOf(CellValue.Number.class, summary.get(5));
    }

    @Test
    void headerOnlyInputWritesZeroSummary() {
        CapturingSink sink = new CapturingSink();
        OrderResult r = OrderCalculator.process(
                source(List.of("sku", "qty", "price", "vat")), sink, meta().build());

        assertEquals(0, r.lineCount());
        assertEquals(new BigDecimal("0.00"), scale(r.orderTotalBeforeTax()));
        assertEquals(new BigDecimal("0.00"), scale(r.orderTotalAfterTax()));
        assertEquals(List.of("TOTAL", "", "", "", "0.00", "0.00"),
                render(sink.rows.get(sink.rows.size() - 1)));
    }

    @Test
    void zeroVatMeansAfterEqualsBefore() {
        CapturingSink sink = new CapturingSink();
        OrderResult r = OrderCalculator.process(
                source(List.of("qty", "price", "vat"), new String[]{"2", "10.00", "0"}),
                sink, meta().build());

        assertEquals(new BigDecimal("20.00"), r.orderTotalBeforeTax());
        assertEquals(new BigDecimal("20.00"), r.orderTotalAfterTax());
        assertEquals(List.of("2", "10.00", "0", "20.00", "20.00"),
                render(sink.rows.get(0)));
    }

    // ---- math ----

    @Test
    void fractionalQuantityAndDecimalVat() {
        CapturingSink sink = new CapturingSink();
        OrderResult r = OrderCalculator.process(
                source(List.of("qty", "price", "vat"), new String[]{"1.5", "4.00", "8.5"}),
                sink, meta().build());

        assertEquals(new BigDecimal("6.00"), r.orderTotalBeforeTax());
        assertEquals(new BigDecimal("6.51"), r.orderTotalAfterTax());
    }

    @Test
    void roundsHalfUpAtOutputScale() {
        CapturingSink sink = new CapturingSink();
        OrderResult r = OrderCalculator.process(
                source(List.of("qty", "price", "vat"), new String[]{"1", "2.005", "0"}),
                sink, meta().build());

        assertEquals(new BigDecimal("2.01"), r.orderTotalBeforeTax());
        assertEquals("2.01", render(sink.rows.get(0)).get(3));
    }

    @Test
    void honorsConfiguredRoundingModeAndScale() {
        CapturingSink sink = new CapturingSink();
        OrderResult r = OrderCalculator.process(
                source(List.of("qty", "price", "vat"), new String[]{"1", "2.005", "0"}),
                sink, meta().roundingMode(RoundingMode.DOWN).outputScale(3).build());

        assertEquals(new BigDecimal("2.005"), r.orderTotalBeforeTax());
    }

    @Test
    void unroundedAccumulationWhenSumRoundedLinesOff() {
        // 3 × 3.333 = 9.999 (rounds to 10.00 per line)
        TabularSource in = source(List.of("qty", "price", "vat"), new String[]{"3", "3.333", "0"});

        CapturingSink rounded = new CapturingSink();
        OrderResult r1 = OrderCalculator.process(
                source(List.of("qty", "price", "vat"), new String[]{"3", "3.333", "0"}),
                rounded, meta().build());
        assertEquals(new BigDecimal("10.00"), r1.orderTotalBeforeTax());
        assertEquals("10.00", render(rounded.rows.get(1)).get(3));

        OrderResult r2 = OrderCalculator.process(
                in, new CapturingSink(), meta().sumRoundedLines(false).build());
        assertEquals(new BigDecimal("9.999"), r2.orderTotalBeforeTax());
    }

    @Test
    void failFastThrowsCarryingRowError() {
        TabularSource in = source(
                List.of("sku", "qty", "price", "vat"),
                new String[]{"P1", "abc", "10.00", "10"});
        CapturingSink sink = new CapturingSink();

        OrderProcessingException e = assertThrows(OrderProcessingException.class,
                () -> OrderCalculator.process(in, sink, meta().build()));

        RowError err = e.rowError();
        assertEquals(1, err.lineNumber());
        assertEquals("qty", err.column());
        assertEquals("abc", err.rawValue());
        assertEquals(0, sink.rows.size()); // header written, no data rows
    }

    // ---- error handling ----

    @Test
    void failFastLeavesPartialOutputBehind() {
        TabularSource in = source(
                List.of("sku", "qty", "price", "vat"),
                new String[]{"P1", "2", "10.00", "10"},
                new String[]{"P2", "x", "5.50", "20"},
                new String[]{"P3", "1", "1.00", "0"});
        CapturingSink sink = new CapturingSink();

        assertThrows(OrderProcessingException.class,
                () -> OrderCalculator.process(in, sink, meta().build()));
        assertEquals(1, sink.rows.size()); // only the good first row
    }

    @Test
    void skipAndCollectSkipsBadRowsAndTotalsGoodOnes() {
        TabularSource in = source(
                List.of("sku", "qty", "price", "vat"),
                new String[]{"P1", "2", "10.00", "10"},
                new String[]{"P2", "x", "5.50", "20"},
                new String[]{"P3", "1", "1.00", "0"});
        CapturingSink sink = new CapturingSink();

        OrderResult r = OrderCalculator.process(
                in, sink, meta().errorStrategy(ErrorStrategy.SKIP_AND_COLLECT).build());

        assertEquals(2, r.lineCount());
        assertEquals(1, r.skippedCount());
        assertEquals(1, r.errors().size());
        assertEquals(2, r.errors().get(0).lineNumber());
        assertEquals(new BigDecimal("21.00"), r.orderTotalBeforeTax());
        assertEquals(new BigDecimal("23.00"), r.orderTotalAfterTax());
        assertEquals(3, sink.rows.size()); // 2 good rows + summary
    }

    @Test
    void emptyRoleCellIsAnErrorNotZero() {
        TabularSource in = source(
                List.of("qty", "price", "vat"), new String[]{"", "10.00", "10"});

        OrderProcessingException e = assertThrows(OrderProcessingException.class,
                () -> OrderCalculator.process(in, new CapturingSink(), meta().build()));
        assertEquals("qty", e.rowError().column());
    }

    @Test
    void negativeValuesRejectedUnlessAllowed() {
        TabularSource in = source(
                List.of("qty", "price", "vat"), new String[]{"-1", "10.00", "10"});

        assertThrows(OrderProcessingException.class,
                () -> OrderCalculator.process(
                        source(List.of("qty", "price", "vat"), new String[]{"-1", "10.00", "10"}),
                        new CapturingSink(), meta().build()));

        CapturingSink sink = new CapturingSink();
        OrderResult r = OrderCalculator.process(
                in, sink, meta().allowNegative(true).build());
        assertEquals(new BigDecimal("-10.00"), r.orderTotalBeforeTax());
    }

    @Test
    void missingBoundColumnIsSchemaError() {
        TabularSource in = source(List.of("sku", "qty", "price", "vat"),
                new String[]{"P1", "2", "10.00", "10"});
        OrderMetadata meta = OrderMetadata.builder()
                .bind(ColumnRole.QUANTITY, "missing_qty")
                .bind(ColumnRole.UNIT_PRICE, "price")
                .bind(ColumnRole.VAT_PERCENT, "vat")
                .build();

        assertThrows(OrderProcessingException.class,
                () -> OrderCalculator.process(in, new CapturingSink(), meta));
    }

    // ---- schema errors (always fatal) ----

    @Test
    void duplicateHeaderIsSchemaError() {
        TabularSource in = source(List.of("qty", "qty", "price", "vat"),
                new String[]{"2", "2", "10.00", "10"});

        assertThrows(OrderProcessingException.class,
                () -> OrderCalculator.process(in, new CapturingSink(), meta().build()));
    }

    @Test
    void missingHeaderRowIsSchemaError() {
        assertThrows(OrderProcessingException.class,
                () -> OrderCalculator.process(
                        source(List.of()), new CapturingSink(), meta().build()));
    }

    @Test
    void duplicateOutputNameIsSchemaError() {
        TabularSource in = source(List.of("qty", "price", "vat"),
                new String[]{"2", "10.00", "10"});
        OrderMetadata meta = meta()
                .outputColumns(
                        OutputColumn.passThrough("qty", "dup"),
                        OutputColumn.passThrough("price", "dup"))
                .build();

        assertThrows(OrderProcessingException.class,
                () -> OrderCalculator.process(in, new CapturingSink(), meta));
    }

    @Test
    void passThroughOfMissingInputIsSchemaError() {
        TabularSource in = source(List.of("qty", "price", "vat"),
                new String[]{"2", "10.00", "10"});
        OrderMetadata meta = meta()
                .outputColumns(OutputColumn.passThrough("nope"))
                .build();

        assertThrows(OrderProcessingException.class,
                () -> OrderCalculator.process(in, new CapturingSink(), meta));
    }

    @Test
    void unknownMarkerColumnIsSchemaError() {
        TabularSource in = source(List.of("qty", "price", "vat"),
                new String[]{"2", "10.00", "10"});
        OrderMetadata meta = meta().summaryMarkerColumn("nope").build();

        assertThrows(OrderProcessingException.class,
                () -> OrderCalculator.process(in, new CapturingSink(), meta));
    }

    @Test
    void explicitLayoutReordersRenamesAndDrops() {
        CapturingSink sink = new CapturingSink();
        OrderMetadata meta = meta()
                .outputColumns(
                        OutputColumn.passThrough("sku", "SKU"),
                        OutputColumn.computed(ComputedType.LINE_TOTAL_BEFORE_TAX, "net"),
                        OutputColumn.computed(ComputedType.LINE_TOTAL_AFTER_TAX, "gross"))
                .build();
        OrderCalculator.process(
                source(List.of("sku", "qty", "price", "vat"),
                        new String[]{"P1", "2", "10.00", "10"}),
                sink, meta);

        assertEquals(List.of("SKU", "net", "gross"), sink.header);
        assertEquals(List.of("P1", "20.00", "22.00"), render(sink.rows.get(0)));
        assertEquals(List.of("TOTAL", "20.00", "22.00"), render(sink.rows.get(1)));
    }

    // ---- output layout ----

    @Test
    void customComputedColumnCanUseComputedTotals() {
        CapturingSink sink = new CapturingSink();
        OrderMetadata meta = meta()
                .outputColumns(
                        OutputColumn.passThrough("qty"),
                        OutputColumn.computed(ComputedType.LINE_TOTAL_BEFORE_TAX, "net"),
                        OutputColumn.computed(ComputedType.LINE_TOTAL_AFTER_TAX, "gross"),
                        OutputColumn.computed(
                                row -> CellValue.number(
                                        row.computed(ComputedType.LINE_TOTAL_AFTER_TAX)
                                                .subtract(row.computed(ComputedType.LINE_TOTAL_BEFORE_TAX))),
                                "tax_amount"))
                .build();
        OrderCalculator.process(
                source(List.of("qty", "price", "vat"), new String[]{"2", "10.00", "10"}),
                sink, meta);

        assertEquals("2.00", render(sink.rows.get(0)).get(3));
        // custom columns stay empty in the summary row
        assertEquals("", render(sink.rows.get(1)).get(3));
    }

    @Test
    void customComputedColumnReturningNullIsRowError() {
        OrderMetadata meta = meta()
                .outputColumns(OutputColumn.computed(row -> null, "bad"))
                .build();

        assertThrows(OrderProcessingException.class,
                () -> OrderCalculator.process(
                        source(List.of("qty", "price", "vat"), new String[]{"2", "10.00", "10"}),
                        new CapturingSink(), meta));
    }

    @Test
    void summaryMarkerPlacementIsConfigurable() {
        CapturingSink sink = new CapturingSink();
        OrderMetadata meta = meta()
                .summaryMarker("SUM")
                .summaryMarkerColumn("price")
                .build();
        OrderCalculator.process(sampleSource(), sink, meta);

        List<String> summary = render(sink.rows.get(sink.rows.size() - 1));
        assertEquals(List.of("", "", "SUM", "", "35.49", "39.44"), summary);
    }

    @Test
    void inputRowOrderIsPreserved() {
        CapturingSink sink = new CapturingSink();
        OrderCalculator.process(
                source(List.of("sku", "qty", "price", "vat"),
                        new String[]{"C", "1", "1.00", "0"},
                        new String[]{"A", "1", "1.00", "0"},
                        new String[]{"B", "1", "1.00", "0"}),
                sink, meta().build());

        assertEquals("C", render(sink.rows.get(0)).get(0));
        assertEquals("A", render(sink.rows.get(1)).get(0));
        assertEquals("B", render(sink.rows.get(2)).get(0));
    }

    private static final class CapturingSink implements TabularSink {
        final List<String> header = new ArrayList<>();
        final List<List<CellValue>> rows = new ArrayList<>();

        @Override
        public void writeHeader(List<String> columns) {
            header.addAll(columns);
        }

        @Override
        public void writeRow(List<CellValue> cells) {
            rows.add(cells);
        }

        @Override
        public void close() {
        }
    }
}
