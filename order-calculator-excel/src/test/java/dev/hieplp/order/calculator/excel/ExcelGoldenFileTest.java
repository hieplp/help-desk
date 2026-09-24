package dev.hieplp.order.calculator.excel;

import dev.hieplp.order.calculator.OrderCalculator;
import dev.hieplp.order.calculator.OrderMetadata;
import dev.hieplp.order.calculator.OrderResult;
import dev.hieplp.order.calculator.csv.Csv;
import dev.hieplp.order.calculator.model.ColumnRole;
import dev.hieplp.order.calculator.spi.TabularSink;
import dev.hieplp.order.calculator.spi.TabularSource;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Cross-format golden coverage: xlsx→csv, csv→xlsx, xlsx→xlsx. xlsx outputs
 * are compared cell-by-cell via POI; computed columns must be numeric cells.
 */
class ExcelGoldenFileTest {

    private static final Object[][] SAMPLE = {
            {"product_id", "quantity", "price_per_item", "vat"},
            {"P1", 2, 10.00, 10},
            {"P2", 1, 5.50, 20},
            {"P3", 3, 3.33, 8.5},
    };

    /**
     * Expected cells when the source is CSV — pass-through text is preserved.
     */
    private static final String[][] FROM_CSV = {
            {"product_id", "quantity", "price_per_item", "vat",
                    "total_price_before_tax", "total_price_after_tax"},
            {"P1", "2", "10.00", "10", "20.00", "22.00"},
            {"P2", "1", "5.50", "20", "5.50", "6.60"},
            {"P3", "3", "3.33", "8.5", "9.99", "10.84"},
            {"TOTAL", "", "", "", "35.49", "39.44"},
    };

    /**
     * Same output from an xlsx source — DataFormatter renders 10.00 as "10".
     */
    private static final String[][] FROM_XLSX = {
            {"product_id", "quantity", "price_per_item", "vat",
                    "total_price_before_tax", "total_price_after_tax"},
            {"P1", "2", "10", "10", "20.00", "22.00"},
            {"P2", "1", "5.5", "20", "5.50", "6.60"},
            {"P3", "3", "3.33", "8.5", "9.99", "10.84"},
            {"TOTAL", "", "", "", "35.49", "39.44"},
    };

    private static final Set<Integer> NUMERIC_COLS = Set.of(4, 5);

    @TempDir
    Path dir;

    private OrderMetadata meta;
    private Path csvIn;
    private Path xlsxIn;

    @BeforeEach
    void setUp() throws Exception {
        meta = OrderMetadata.builder()
                .bind(ColumnRole.QUANTITY, "quantity")
                .bind(ColumnRole.UNIT_PRICE, "price_per_item")
                .bind(ColumnRole.VAT_PERCENT, "vat")
                .build();
        csvIn = dir.resolve("orders.csv");
        xlsxIn = dir.resolve("orders.xlsx");
        Files.writeString(csvIn, """
                product_id,quantity,price_per_item,vat
                P1,2,10.00,10
                P2,1,5.50,20
                P3,3,3.33,8.5
                """);
        try (Workbook wb = new XSSFWorkbook(); var out = Files.newOutputStream(xlsxIn)) {
            Sheet sheet = wb.createSheet("Sheet1");
            for (Object[] row : SAMPLE) {
                Row r = sheet.createRow(sheet.getPhysicalNumberOfRows());
                for (int c = 0; c < row.length; c++) {
                    if (row[c] instanceof Number n) {
                        r.createCell(c).setCellValue(n.doubleValue());
                    } else {
                        r.createCell(c).setCellValue(row[c].toString());
                    }
                }
            }
            wb.write(out);
        }
    }

    @Test
    void xlsxToCsvProducesEnrichedRowsAndSummaryRow() throws Exception {
        Path out = dir.resolve("xlsx-to-csv.csv");
        OrderResult r = process(Excel.source(xlsxIn), Csv.sink(out));
        assertResult(r);
        assertEquals(List.of(
                "product_id,quantity,price_per_item,vat,total_price_before_tax,total_price_after_tax",
                "P1,2,10,10,20.00,22.00",
                "P2,1,5.5,20,5.50,6.60",
                "P3,3,3.33,8.5,9.99,10.84",
                "TOTAL,,,,35.49,39.44"), Files.readAllLines(out));
    }

    @Test
    void csvToXlsxProducesNumericTotals() throws Exception {
        Path out = dir.resolve("csv-to-xlsx.xlsx");
        OrderResult r = process(Csv.source(csvIn), Excel.sink(out));
        assertResult(r);
        assertSheet(out, FROM_CSV);
    }

    @Test
    void xlsxToXlsxProducesNumericTotals() throws Exception {
        Path out = dir.resolve("xlsx-to-xlsx.xlsx");
        OrderResult r = process(Excel.source(xlsxIn), Excel.sink(out));
        assertResult(r);
        assertSheet(out, FROM_XLSX);
    }

    private OrderResult process(TabularSource source, TabularSink sink) throws Exception {
        try (source; sink) {
            return OrderCalculator.process(source, sink, meta);
        }
    }

    private static void assertResult(OrderResult r) {
        assertEquals(3, r.lineCount());
        assertEquals(0, r.skippedCount());
        assertEquals(new BigDecimal("35.49"), r.orderTotalBeforeTax());
        assertEquals(new BigDecimal("39.44"), r.orderTotalAfterTax());
    }

    private static void assertSheet(Path xlsx, String[][] expected) throws Exception {
        DataFormatter formatter = new DataFormatter();
        try (Workbook wb = WorkbookFactory.create(xlsx.toFile())) {
            Sheet sheet = wb.getSheetAt(0);
            assertEquals(expected.length - 1, sheet.getLastRowNum());
            for (int r = 0; r < expected.length; r++) {
                Row row = sheet.getRow(r);
                for (int c = 0; c < expected[r].length; c++) {
                    Cell cell = row.getCell(c);
                    String want = expected[r][c];
                    if (r > 0 && NUMERIC_COLS.contains(c)) {
                        assertEquals(CellType.NUMERIC, cell.getCellType(), "cell " + r + "," + c);
                        assertEquals(0, new BigDecimal(want).compareTo(
                                BigDecimal.valueOf(cell.getNumericCellValue())), "cell " + r + "," + c);
                    } else {
                        assertEquals(want, formatter.formatCellValue(cell), "cell " + r + "," + c);
                    }
                }
            }
        }
    }
}
