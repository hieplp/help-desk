package dev.hieplp.order.calculator.excel;

import dev.hieplp.order.calculator.OrderCalculator;
import dev.hieplp.order.calculator.OrderMetadata;
import dev.hieplp.order.calculator.OrderResult;
import dev.hieplp.order.calculator.error.ErrorStrategy;
import dev.hieplp.order.calculator.error.OrderProcessingException;
import dev.hieplp.order.calculator.model.ColumnRole;
import dev.hieplp.order.calculator.spi.TabularSink;
import dev.hieplp.order.calculator.spi.TabularSource;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Edge-case coverage for the streaming xlsx source (SPEC §8).
 */
class ExcelEdgeCaseTest {

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

    private static void header(Sheet sheet, int rowIndex) {
        Row row = sheet.createRow(rowIndex);
        String[] cols = {"product_id", "quantity", "price_per_item", "vat"};
        for (int i = 0; i < cols.length; i++) {
            row.createCell(i).setCellValue(cols[i]);
        }
    }

    private static void dataRow(Sheet sheet, int rowIndex, Object... cells) {
        Row row = sheet.createRow(rowIndex);
        for (int i = 0; i < cells.length; i++) {
            Cell cell = row.createCell(i);
            if (cells[i] instanceof Number n) {
                cell.setCellValue(n.doubleValue());
            } else {
                cell.setCellValue(cells[i].toString());
            }
        }
    }

    private static OrderResult process(TabularSource source, OrderMetadata meta) {
        try (source; TabularSink sink = dev.hieplp.order.calculator.csv.Csv.sink(new StringWriter())) {
            return OrderCalculator.process(source, sink, meta);
        } catch (OrderProcessingException e) {
            throw e;
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    private Path writeXlsx(String name, Consumer<Sheet> populate) throws Exception {
        Path file = dir.resolve(name);
        try (Workbook wb = new XSSFWorkbook();
             var out = Files.newOutputStream(file)) {
            Sheet sheet = wb.createSheet("Sheet1");
            populate.accept(sheet);
            wb.write(out);
        }
        return file;
    }

    @Test
    void textCellInNumericColumnIsRowError() throws Exception {
        Path file = writeXlsx("text.xlsx", sheet -> {
            header(sheet, 0);
            dataRow(sheet, 1, "P1", "not-a-number", 10.00, 10);
        });

        OrderProcessingException e = assertThrows(OrderProcessingException.class,
                () -> process(Excel.source(file), meta()));
        assertEquals("quantity", e.rowError().column());
        assertEquals(1, e.rowError().lineNumber());
    }

    @Test
    void blankRowEndsDataRegion() throws Exception {
        Path file = writeXlsx("blank.xlsx", sheet -> {
            header(sheet, 0);
            dataRow(sheet, 1, "P1", 2, 10.00, 10);
            dataRow(sheet, 2, "P2", 1, 5.50, 20);
            sheet.createRow(3); // blank row — end of data
            dataRow(sheet, 4, "P3", 3, 3.33, 8.5);
        });

        OrderResult r = process(Excel.source(file), meta());
        assertEquals(2, r.lineCount());
        assertEquals(new BigDecimal("25.50"), r.orderTotalBeforeTax());
    }

    @Test
    void strictBlankRowsReportsRowError() throws Exception {
        Path file = writeXlsx("strict.xlsx", sheet -> {
            header(sheet, 0);
            dataRow(sheet, 1, "P1", 2, 10.00, 10);
            sheet.createRow(2); // blank row inside data
            dataRow(sheet, 3, "P2", 1, 5.50, 20);
        });
        Excel.SourceConfig strict = Excel.SourceConfig.defaults().strictBlankRows(true);

        OrderResult r = process(Excel.source(file, strict), lenient());
        assertEquals(2, r.lineCount());
        assertEquals(1, r.skippedCount());
        assertEquals(1, r.errors().size());
        assertEquals(2, r.errors().get(0).lineNumber());
    }

    @Test
    void strictBlankRowsFailFastAborts() throws Exception {
        Path file = writeXlsx("strict-ff.xlsx", sheet -> {
            header(sheet, 0);
            dataRow(sheet, 1, "P1", 2, 10.00, 10);
            sheet.createRow(2);
            dataRow(sheet, 3, "P2", 1, 5.50, 20);
        });
        Excel.SourceConfig strict = Excel.SourceConfig.defaults().strictBlankRows(true);

        assertThrows(OrderProcessingException.class,
                () -> process(Excel.source(file, strict), meta()));
    }

    @Test
    void strictBlankRowsContinuesAfterRowGap() throws Exception {
        Path file = writeXlsx("strict-gap.xlsx", sheet -> {
            header(sheet, 0);
            dataRow(sheet, 1, "P1", 2, 10.00, 10);
            // rows 2 and 3 absent from the XML — a gap
            dataRow(sheet, 4, "P2", 1, 5.50, 20);
        });
        Excel.SourceConfig strict = Excel.SourceConfig.defaults().strictBlankRows(true);

        OrderResult r = process(Excel.source(file, strict), lenient());
        assertEquals(2, r.lineCount());          // the row after the gap is still read
        assertEquals(1, r.skippedCount());       // one error for the blank gap
        assertEquals(2, r.errors().get(0).lineNumber());
        assertEquals(new BigDecimal("25.50"), r.orderTotalBeforeTax());
    }

    @Test
    void rowGapEndsDataRegion() throws Exception {
        // row index 3 absent from the sheet entirely — a hole in the XML
        Path file = writeXlsx("gap.xlsx", sheet -> {
            header(sheet, 0);
            dataRow(sheet, 1, "P1", 2, 10.00, 10);
            dataRow(sheet, 4, "P3", 3, 3.33, 8.5);
        });

        OrderResult r = process(Excel.source(file), meta());
        assertEquals(1, r.lineCount());
    }

    @Test
    void duplicateHeaderIsSchemaError() throws Exception {
        Path file = writeXlsx("dup.xlsx", sheet -> {
            Row row = sheet.createRow(0);
            row.createCell(0).setCellValue("quantity");
            row.createCell(1).setCellValue("quantity");
            row.createCell(2).setCellValue("price_per_item");
            row.createCell(3).setCellValue("vat");
            dataRow(sheet, 1, 2, 10.00, 10);
        });

        OrderProcessingException e = assertThrows(OrderProcessingException.class,
                () -> process(Excel.source(file), meta()));
        assertTrue(e.getMessage().contains("duplicate header"));
    }

    @Test
    void missingHeaderRowIsSchemaError() throws Exception {
        Path file = writeXlsx("empty.xlsx", sheet -> {
        });

        OrderProcessingException e = assertThrows(OrderProcessingException.class,
                () -> process(Excel.source(file), meta()));
        assertTrue(e.getMessage().contains("missing header"));
    }

    @Test
    void missingBoundColumnIsSchemaError() throws Exception {
        Path file = writeXlsx("missing-col.xlsx", sheet -> {
            Row row = sheet.createRow(0);
            row.createCell(0).setCellValue("quantity");
            row.createCell(1).setCellValue("vat");
            dataRow(sheet, 1, 2, 10);
        });
        OrderMetadata meta = OrderMetadata.builder()
                .bind(ColumnRole.QUANTITY, "quantity")
                .bind(ColumnRole.UNIT_PRICE, "price_per_item")
                .bind(ColumnRole.VAT_PERCENT, "vat")
                .build();

        assertThrows(OrderProcessingException.class,
                () -> process(Excel.source(file), meta));
    }

    @Test
    void formulaCellUsesCachedValue() throws Exception {
        Path file = dir.resolve("formula.xlsx");
        try (Workbook wb = new XSSFWorkbook();
             var out = Files.newOutputStream(file)) {
            Sheet sheet = wb.createSheet("Sheet1");
            header(sheet, 0);
            Row row = sheet.createRow(1);
            row.createCell(0).setCellValue("P1");
            Cell qty = row.createCell(1);
            qty.setCellFormula("2*5");
            row.createCell(2).setCellValue(10.00);
            row.createCell(3).setCellValue(10);
            FormulaEvaluator evaluator = wb.getCreationHelper().createFormulaEvaluator();
            evaluator.evaluateFormulaCell(qty); // stores cached result 10
            wb.write(out);
        }

        OrderResult r = process(Excel.source(file), meta());
        assertEquals(1, r.lineCount());
        assertEquals(new BigDecimal("100.00"), r.orderTotalBeforeTax());
        assertEquals(new BigDecimal("110.00"), r.orderTotalAfterTax());
    }

    @Test
    void formulaWithTextResultInNumericColumnIsRowError() throws Exception {
        Path file = dir.resolve("formula-str.xlsx");
        try (Workbook wb = new XSSFWorkbook();
             var out = Files.newOutputStream(file)) {
            Sheet sheet = wb.createSheet("Sheet1");
            header(sheet, 0);
            Row row = sheet.createRow(1);
            row.createCell(0).setCellValue("P1");
            Cell qty = row.createCell(1);
            qty.setCellFormula("\"oops\"&\"!\"");
            row.createCell(2).setCellValue(10.00);
            row.createCell(3).setCellValue(10);
            wb.getCreationHelper().createFormulaEvaluator().evaluateFormulaCell(qty);
            wb.write(out);
        }

        OrderProcessingException e = assertThrows(OrderProcessingException.class,
                () -> process(Excel.source(file), meta()));
        assertEquals("quantity", e.rowError().column());
    }

    @Test
    void headerRowIndexSkipsTitleRows() throws Exception {
        Path file = writeXlsx("title.xlsx", sheet -> {
            Row title = sheet.createRow(0);
            title.createCell(0).setCellValue("Order export — do not edit");
            header(sheet, 1);
            dataRow(sheet, 2, "P1", 2, 10.00, 10);
        });
        Excel.SourceConfig config = Excel.SourceConfig.defaults().headerRowIndex(1);

        OrderResult r = process(Excel.source(file, config), meta());
        assertEquals(1, r.lineCount());
        assertEquals(new BigDecimal("20.00"), r.orderTotalBeforeTax());
    }

    @Test
    void selectsSheetByName() throws Exception {
        Path file = dir.resolve("named.xlsx");
        try (Workbook wb = new XSSFWorkbook();
             var out = Files.newOutputStream(file)) {
            Sheet wrong = wb.createSheet("Scratch");
            wrong.createRow(0).createCell(0).setCellValue("unrelated");
            Sheet orders = wb.createSheet("Orders");
            header(orders, 0);
            dataRow(orders, 1, "P1", 2, 10.00, 10);
            wb.write(out);
        }
        Excel.SourceConfig config = Excel.SourceConfig.defaults().sheetName("Orders");

        OrderResult r = process(Excel.source(file, config), meta());
        assertEquals(1, r.lineCount());
    }

    @Test
    void missingSheetNameIsError() throws Exception {
        Path file = writeXlsx("nosheet.xlsx", sheet -> header(sheet, 0));
        Excel.SourceConfig config = Excel.SourceConfig.defaults().sheetName("Nope");

        OrderProcessingException e = assertThrows(OrderProcessingException.class,
                () -> process(Excel.source(file, config), meta()));
        assertTrue(e.getMessage().contains("no sheet named"));
    }

    @Test
    void missingSheetIndexIsError() throws Exception {
        Path file = writeXlsx("noidx.xlsx", sheet -> header(sheet, 0));
        Excel.SourceConfig config = new Excel.SourceConfig(7, null, 0);

        OrderProcessingException e = assertThrows(OrderProcessingException.class,
                () -> process(Excel.source(file, config), meta()));
        assertTrue(e.getMessage().contains("no sheet at index"));
    }

    @Test
    void readsFromInputStream() throws Exception {
        Path file = writeXlsx("stream.xlsx", sheet -> {
            header(sheet, 0);
            dataRow(sheet, 1, "P1", 2, 10.00, 10);
        });

        try (var in = new ByteArrayInputStream(Files.readAllBytes(file));
             TabularSource source = Excel.source(in);
             TabularSink sink = dev.hieplp.order.calculator.csv.Csv.sink(new StringWriter())) {
            OrderResult r = OrderCalculator.process(source, sink, meta());
            assertEquals(1, r.lineCount());
        }
    }

    @Test
    void formattedNumericPassThroughUsesDisplayText() throws Exception {
        Path file = dir.resolve("fmt.xlsx");
        try (Workbook wb = new XSSFWorkbook();
             var out = Files.newOutputStream(file)) {
            Sheet sheet = wb.createSheet("Sheet1");
            Row row = sheet.createRow(0);
            row.createCell(0).setCellValue("day");
            row.createCell(1).setCellValue("quantity");
            row.createCell(2).setCellValue("price_per_item");
            row.createCell(3).setCellValue("vat");

            CellStyle dateStyle = wb.createCellStyle();
            dateStyle.setDataFormat(wb.getCreationHelper()
                    .createDataFormat().getFormat("yyyy-mm-dd"));
            Row data = sheet.createRow(1);
            Cell date = data.createCell(0);
            date.setCellValue(45000.0); // 2023-03-15 in the 1900 date system
            date.setCellStyle(dateStyle);
            data.createCell(1).setCellValue(2);
            data.createCell(2).setCellValue(10.00);
            data.createCell(3).setCellValue(10);
            wb.write(out);
        }

        StringWriter out = new StringWriter();
        try (TabularSource source = Excel.source(file);
             TabularSink sink = dev.hieplp.order.calculator.csv.Csv.sink(out)) {
            OrderCalculator.process(source, sink, meta());
        }
        List<String> lines = out.toString().lines().toList();
        assertTrue(lines.get(1).startsWith("2023-03-15,"), lines.get(1));
    }
}
