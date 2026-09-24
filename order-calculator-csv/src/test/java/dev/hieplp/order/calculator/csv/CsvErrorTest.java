package dev.hieplp.order.calculator.csv;

import dev.hieplp.order.calculator.OrderCalculator;
import dev.hieplp.order.calculator.OrderMetadata;
import dev.hieplp.order.calculator.OrderResult;
import dev.hieplp.order.calculator.error.ErrorStrategy;
import dev.hieplp.order.calculator.error.OrderProcessingException;
import dev.hieplp.order.calculator.model.ColumnRole;
import dev.hieplp.order.calculator.spi.TabularSink;
import dev.hieplp.order.calculator.spi.TabularSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Row-error and schema-error coverage for the CSV format.
 */
class CsvErrorTest {

    @TempDir
    Path dir;

    private static OrderMetadata meta() {
        return OrderMetadata.builder()
                .bind(ColumnRole.QUANTITY, "quantity")
                .bind(ColumnRole.UNIT_PRICE, "price_per_item")
                .bind(ColumnRole.VAT_PERCENT, "vat")
                .build();
    }

    private static OrderMetadata lenient() {
        return OrderMetadata.builder()
                .bind(ColumnRole.QUANTITY, "quantity")
                .bind(ColumnRole.UNIT_PRICE, "price_per_item")
                .bind(ColumnRole.VAT_PERCENT, "vat")
                .errorStrategy(ErrorStrategy.SKIP_AND_COLLECT)
                .build();
    }

    private static OrderProcessingException run(String csv) {
        StringWriter out = new StringWriter();
        try (TabularSource source = Csv.source(new StringReader(csv));
             TabularSink sink = Csv.sink(out)) {
            return assertThrows(OrderProcessingException.class,
                    () -> OrderCalculator.process(source, sink, meta()));
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    @Test
    void nonNumericCellIsRowError() {
        OrderProcessingException e = run("quantity,price_per_item,vat\nabc,10.00,10\n");
        assertEquals(1, e.rowError().lineNumber());
        assertEquals("quantity", e.rowError().column());
        assertEquals("abc", e.rowError().rawValue());
    }

    @Test
    void scientificNotationIsRejected() {
        OrderProcessingException e = run("quantity,price_per_item,vat\n1E2,10.00,10\n");
        assertEquals("quantity", e.rowError().column());
        assertEquals("1E2", e.rowError().rawValue());
    }

    @Test
    void thousandsSeparatorIsRejected() {
        OrderProcessingException e = run("quantity,price_per_item,vat\n2,\"1,234.56\",10\n");
        assertEquals("price_per_item", e.rowError().column());
    }

    @Test
    void emptyCellIsRowError() {
        OrderProcessingException e = run("quantity,price_per_item,vat\n2,,10\n");
        assertEquals("price_per_item", e.rowError().column());
    }

    @Test
    void negativeValueIsRowError() {
        OrderProcessingException e = run("quantity,price_per_item,vat\n-2,10.00,10\n");
        assertEquals("quantity", e.rowError().column());
    }

    @Test
    void tooFewFieldsIsRowError() {
        OrderProcessingException e = run("quantity,price_per_item,vat\n2,10.00\n");
        assertEquals(1, e.rowError().lineNumber());
    }

    @Test
    void tooManyFieldsIsRowError() {
        OrderProcessingException e = run("quantity,price_per_item,vat\n2,10.00,10,EXTRA\n");
        assertEquals(1, e.rowError().lineNumber());
    }

    @Test
    void duplicateHeaderIsSchemaError() {
        OrderProcessingException e = run("quantity,quantity,vat\n2,10.00,10\n");
        assertTrue(e.getMessage().contains("duplicate header"));
    }

    @Test
    void emptyFileIsMissingHeaderError() {
        OrderProcessingException e = run("");
        assertEquals("missing header row", e.getMessage());
    }

    @Test
    void skipAndCollectGathersAllBadRows() {
        StringWriter out = new StringWriter();
        try (TabularSource source = Csv.source(new StringReader(
                "quantity,price_per_item,vat\n"
                        + "2,10.00,10\n"
                        + "x,10.00,10\n"
                        + "3,10.00\n"
                        + "1,2.00,0\n"));
             TabularSink sink = Csv.sink(out)) {
            OrderResult r = OrderCalculator.process(source, sink, lenient());
            assertEquals(2, r.lineCount());
            assertEquals(2, r.skippedCount());
            assertEquals(2, r.errors().size());
            assertEquals(2, r.errors().get(0).lineNumber());
            assertEquals(3, r.errors().get(1).lineNumber());
            assertEquals(new BigDecimal("22.00"), r.orderTotalBeforeTax());
            assertEquals(new BigDecimal("24.00"), r.orderTotalAfterTax());
        } catch (Exception e) {
            throw new AssertionError(e);
        }
        String[] lines = out.toString().split("\n");
        assertEquals(4, lines.length); // header + 2 good rows + TOTAL
        assertTrue(lines[3].startsWith("TOTAL"));
    }

    @Test
    void headerOnlyFileProducesZeroTotals() throws Exception {
        Path out = dir.resolve("out.csv");
        try (TabularSource source = Csv.source(new StringReader("quantity,price_per_item,vat\n"));
             TabularSink sink = Csv.sink(out)) {
            OrderResult r = OrderCalculator.process(source, sink, meta());
            assertEquals(0, r.lineCount());
        }
        assertEquals(
                java.util.List.of(
                        "quantity,price_per_item,vat,total_price_before_tax,total_price_after_tax",
                        "TOTAL,,,0.00,0.00"),
                Files.readAllLines(out));
    }
}
