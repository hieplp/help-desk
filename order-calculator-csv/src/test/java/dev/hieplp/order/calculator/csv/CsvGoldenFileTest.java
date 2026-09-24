package dev.hieplp.order.calculator.csv;

import dev.hieplp.order.calculator.OrderCalculator;
import dev.hieplp.order.calculator.OrderMetadata;
import dev.hieplp.order.calculator.OrderResult;
import dev.hieplp.order.calculator.model.ColumnRole;
import dev.hieplp.order.calculator.spi.TabularSink;
import dev.hieplp.order.calculator.spi.TabularSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Golden-file coverage: input CSV → expected output CSV under
 * src/test/resources.
 */
class CsvGoldenFileTest {

    @TempDir
    Path dir;

    private static Path resource(String name) throws URISyntaxException {
        return Path.of(CsvGoldenFileTest.class.getResource("/" + name).toURI());
    }

    @Test
    void csvToCsvProducesEnrichedRowsAndSummaryRow() throws Exception {
        OrderMetadata meta = OrderMetadata.builder()
                .bind(ColumnRole.QUANTITY, "quantity")
                .bind(ColumnRole.UNIT_PRICE, "price_per_item")
                .bind(ColumnRole.VAT_PERCENT, "vat")
                .build();

        Path output = dir.resolve("out.csv");
        OrderResult result;
        try (TabularSource source = Csv.source(resource("orders.csv"));
             TabularSink sink = Csv.sink(output)) {
            result = OrderCalculator.process(source, sink, meta);
        }

        assertEquals(3, result.lineCount());
        assertEquals(0, result.skippedCount());
        assertEquals(new BigDecimal("35.49"), result.orderTotalBeforeTax());
        assertEquals(new BigDecimal("39.44"), result.orderTotalAfterTax());
        assertEquals(Files.readAllLines(resource("expected.csv")), Files.readAllLines(output));
    }
}
