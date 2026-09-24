package dev.hieplp.order.calculator.excel;

import dev.hieplp.order.calculator.OrderCalculator;
import dev.hieplp.order.calculator.OrderMetadata;
import dev.hieplp.order.calculator.OrderResult;
import dev.hieplp.order.calculator.csv.Csv;
import dev.hieplp.order.calculator.model.ColumnRole;
import dev.hieplp.order.calculator.spi.TabularSink;
import dev.hieplp.order.calculator.spi.TabularSource;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.BufferedWriter;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * SPEC §11 streaming smoke test: 1M rows csv→xlsx (SXSSF write path) and
 * xlsx→csv (StAX read path) — constant memory both directions.
 */
class StreamingSmokeTest {

    private static final int ROWS = 1_000_000;

    @TempDir
    Path dir;

    private static OrderMetadata meta() {
        return OrderMetadata.builder()
                .bind(ColumnRole.QUANTITY, "quantity")
                .bind(ColumnRole.UNIT_PRICE, "price_per_item")
                .bind(ColumnRole.VAT_PERCENT, "vat")
                .build();
    }

    @Test
    void oneMillionRowsCsvToXlsxStreams() throws Exception {
        Path csvIn = dir.resolve("big.csv");
        try (BufferedWriter w = Files.newBufferedWriter(csvIn)) {
            w.write("product_id,quantity,price_per_item,vat\n");
            for (int i = 0; i < ROWS; i++) {
                w.write("P" + i + ",1,1.00,0\n");
            }
        }

        Path xlsx = dir.resolve("big.xlsx");
        try (TabularSource source = Csv.source(csvIn);
             TabularSink sink = Excel.sink(xlsx)) {
            OrderResult r = OrderCalculator.process(source, sink, meta());
            assertEquals(ROWS, r.lineCount());
            assertEquals(0, r.orderTotalBeforeTax()
                    .compareTo(new BigDecimal(ROWS).setScale(2)));
        }
        assertTrue(Files.size(xlsx) > 0);
    }

    @Test
    void oneMillionRowsXlsxToCsvStreams() throws Exception {
        // Numeric cells must be real numbers — pass-through columns of an
        // enriched file are text by design, so generate the input directly.
        Path xlsxIn = dir.resolve("big-in.xlsx");
        try (SXSSFWorkbook wb = new SXSSFWorkbook();
             var out = Files.newOutputStream(xlsxIn)) {
            Sheet sheet = wb.createSheet("Sheet1");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("product_id");
            header.createCell(1).setCellValue("quantity");
            header.createCell(2).setCellValue("price_per_item");
            header.createCell(3).setCellValue("vat");
            for (int i = 0; i < ROWS; i++) {
                Row row = sheet.createRow(i + 1);
                row.createCell(0).setCellValue("P" + i);
                row.createCell(1).setCellValue(1.0);
                row.createCell(2).setCellValue(1.0);
                row.createCell(3).setCellValue(0.0);
            }
            wb.write(out);
        }

        Path csvOut = dir.resolve("big-out.csv");
        try (TabularSource source = Excel.source(xlsxIn);
             TabularSink sink = Csv.sink(csvOut)) {
            OrderResult r = OrderCalculator.process(source, sink, meta());
            assertEquals(ROWS, r.lineCount());
            assertEquals(0, r.orderTotalBeforeTax()
                    .compareTo(new BigDecimal(ROWS).setScale(2)));
        }

        long lines = Files.lines(csvOut).count();
        assertEquals(ROWS + 2, lines); // header + data + TOTAL
    }
}
