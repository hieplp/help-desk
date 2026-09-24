package dev.hieplp.order.app;

import dev.hieplp.order.calculator.OrderCalculator;
import dev.hieplp.order.calculator.OrderMetadata;
import dev.hieplp.order.calculator.OrderResult;
import dev.hieplp.order.calculator.csv.Csv;
import dev.hieplp.order.calculator.excel.Excel;
import dev.hieplp.order.calculator.model.ColumnRole;
import dev.hieplp.order.calculator.spi.TabularSink;
import dev.hieplp.order.calculator.spi.TabularSource;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * Test application for the order-calculator library. Generates the sample
 * order in both CSV and XLSX form, then runs the engine across all four
 * source→sink format pairs and prints each {@link OrderResult}.
 *
 * <p>Inputs and outputs land in {@code order-calculator-app/build/demo/}.
 */
public final class Main {

    private static final Path DIR = Path.of("build", "demo");

    private static final Object[][] SAMPLE = {
        {"product_id", "quantity", "price_per_item", "vat"},
        {"P1", 2, 10.00, 10},
        {"P2", 1, 5.50, 20},
        {"P3", 3, 3.33, 8.5},
    };

    private Main() {
    }

    public static void main(String[] args) throws IOException {
        Files.createDirectories(DIR);
        Path csvIn = DIR.resolve("orders.csv");
        Path xlsxIn = DIR.resolve("orders.xlsx");
        writeSampleCsv(csvIn);
        writeSampleXlsx(xlsxIn);
        OrderMetadata meta = OrderMetadata.builder()
                .bind(ColumnRole.QUANTITY, "quantity")
                .bind(ColumnRole.UNIT_PRICE, "price_per_item")
                .bind(ColumnRole.VAT_PERCENT, "vat")
                .build();
        run("csv -> csv", () -> process(
                Csv.source(csvIn), Csv.sink(DIR.resolve("out-csv-csv.csv")), meta));
        run("csv -> xlsx", () -> process(
                Csv.source(csvIn), Excel.sink(DIR.resolve("out-csv-xlsx.xlsx")), meta));
        run("xlsx -> csv", () -> process(
                Excel.source(xlsxIn), Csv.sink(DIR.resolve("out-xlsx-csv.csv")), meta));
        run("xlsx -> xlsx", () -> process(
                Excel.source(xlsxIn), Excel.sink(DIR.resolve("out-xlsx-xlsx.xlsx")), meta));

        System.out.println("files: " + DIR.toAbsolutePath());
    }

    private static OrderResult process(TabularSource source, TabularSink sink, OrderMetadata meta) {
        try (source; sink) {
            return OrderCalculator.process(source, sink, meta);
        }
    }

    private static void run(String label, Callable<OrderResult> job) {
        try {
            OrderResult r = job.call();
            System.out.printf("%-13s ok — %d line(s), skipped=%d, before=%s, after=%s%n",
                    label, r.lineCount(), r.skippedCount(),
                    r.orderTotalBeforeTax().toPlainString(),
                    r.orderTotalAfterTax().toPlainString());
            for (var e : r.errors()) {
                System.out.printf("  row %d [%s] %s — %s%n",
                        e.lineNumber(), e.column(), e.rawValue(), e.message());
            }
        } catch (Exception e) {
            System.out.printf("%-13s failed — %s%n", label, e.getMessage());
        }
    }

    private static void writeSampleCsv(Path path) throws IOException {
        var sb = new StringBuilder();
        for (Object[] row : SAMPLE) {
            for (int c = 0; c < row.length; c++) {
                if (c > 0) {
                    sb.append(',');
                }
                sb.append(row[c]);
            }
            sb.append('\n');
        }
        Files.writeString(path, sb.toString(), StandardCharsets.UTF_8);
    }

    private static void writeSampleXlsx(Path path) throws IOException {
        try (var wb = new XSSFWorkbook()) {
            var sheet = wb.createSheet("Sheet1");
            for (Object[] row : SAMPLE) {
                var excelRow = sheet.createRow(sheet.getPhysicalNumberOfRows());
                for (int c = 0; c < row.length; c++) {
                    var cell = excelRow.createCell(c);
                    if (row[c] instanceof Number n) {
                        cell.setCellValue(n.doubleValue());
                    } else {
                        cell.setCellValue(row[c].toString());
                    }
                }
            }

            try (var out = Files.newOutputStream(path)) {
                wb.write(out);
            }
        }
    }
}
