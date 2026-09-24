# order-calculator

A small Java library that reads order line items from a **tabular source**
(CSV, Excel, or your own format), computes per-line totals before and after
tax, appends an order-level summary row, and writes the result to a **tabular
sink** — in any format combination.

```
in.csv ──▶ ┌──────────────┐ ──▶ out.xlsx
in.xlsx ──▶│ OrderCalculator│ ──▶ out.csv
custom ───▶└──────────────┘ ──▶ custom
```

Two things are never hard-coded:

- **Schema** — your column names are bound to roles (`QUANTITY`, `UNIT_PRICE`,
  `VAT_PERCENT`) via metadata. Any header names work.
- **Format** — input and output formats are independent. CSV → Excel,
  Excel → CSV, same → same, or a custom format via the SPI.

## What it does

For each input row:

- `line_before_tax = QUANTITY × UNIT_PRICE`
- `line_after_tax  = line_before_tax × (1 + VAT_PERCENT / 100)`

Then appends one summary row (`TOTAL` marker + order totals). All math is
`BigDecimal`, rounded per line (default scale 2, `HALF_UP`), and totals
accumulate the rounded line values so displayed lines always match the totals.

Everything streams — O(1) memory in row count, both directions.

## Example

Input `in.csv`:

```csv
product_id,quantity,price_per_item,vat
P1,2,10.00,10
P2,1,5.50,20
P3,3,3.33,8.5
```

Output `out.csv` (or an `.xlsx` with real numeric cells):

```csv
product_id,quantity,price_per_item,vat,total_price_before_tax,total_price_after_tax
P1,2,10.00,10,20.00,22.00
P2,1,5.50,20,5.50,6.60
P3,3,3.33,8.5,9.99,10.84
TOTAL,,,,35.49,39.44
```

## Modules

| Module | Contents | Dependencies |
|---|---|---|
| `order-calculator-core` | Engine, metadata, SPI | none |
| `order-calculator-csv` | CSV source/sink | Apache Commons CSV |
| `order-calculator-excel` | `.xlsx` source/sink (streaming) | Apache POI |
| `order-calculator-app` | Demo app (not a library) | all of the above |

Depend only on the format modules you need — a CSV-only consumer never pulls
POI's dependency tree.

**Requirements:** Java 21+. Every module ships a `module-info.java`; JPMS names
are `dev.hieplp.order.calculator`, `dev.hieplp.order.calculator.csv`,
`dev.hieplp.order.calculator.excel`.

## Getting it

Not published to Maven Central yet. Build and install to the local Maven repo:

```bash
./gradlew publishToMavenLocal
```

Then depend on it (Gradle):

```kotlin
dependencies {
    implementation("dev.hieplp.order:order-calculator-csv:1.0-SNAPSHOT")
    implementation("dev.hieplp.order:order-calculator-excel:1.0-SNAPSHOT") // if needed
}
```

Or, inside this repo, use project dependencies:

```kotlin
implementation(project(":order-calculator-csv"))
```

## Quick start

```java
import dev.hieplp.order.calculator.*;
import dev.hieplp.order.calculator.csv.Csv;
import dev.hieplp.order.calculator.excel.Excel;
import dev.hieplp.order.calculator.model.ColumnRole;

// 1. Bind your column names to roles
OrderMetadata meta = OrderMetadata.builder()
        .bind(ColumnRole.QUANTITY,   "quantity")
        .bind(ColumnRole.UNIT_PRICE, "price_per_item")
        .bind(ColumnRole.VAT_PERCENT,"vat")
        .build();

// 2. Pick a source and a sink — formats mix freely
try (var source = Csv.source(Path.of("in.csv"));
     var sink   = Excel.sink(Path.of("out.xlsx"))) {

    // 3. Run
    OrderResult result = OrderCalculator.process(source, sink, meta);
    System.out.println(result.lineCount() + " lines, total "
            + result.orderTotalAfterTax());
}
```

`OrderCalculator` is stateless and thread-safe. Sources and sinks are
single-use and not thread-safe — close them (try-with-resources) so files
flush.

### Sources and sinks

```java
// CSV — Path, Reader / Writer; charset and CSVFormat overridable
Csv.source(Path.of("in.csv"));
Csv.source(Path.of("in.csv"), StandardCharsets.ISO_8859_1);
Csv.source(reader, myCsvFormat);
Csv.sink(Path.of("out.csv"));
Csv.sink(writer);

// Excel (.xlsx) — Path, InputStream / OutputStream
Excel.source(Path.of("in.xlsx"));
Excel.source(in, new Excel.SourceConfig(0, null, 1)   // sheet, name, header row
                     .sheetName("Orders"));
Excel.sink(Path.of("out.xlsx"));
Excel.sink(out, new Excel.SinkConfig("Totals"));
```

CSV defaults: header row, `,` delimiter, `"` quoting, trimmed cells, UTF-8.
Excel defaults: first sheet, header row 0, data ends at the first fully blank
row (`strictBlankRows(true)` on `SourceConfig` turns a blank row into a row
error instead); reads stream row-by-row through the POI event model and writes
via SXSSF — constant memory in row count both ways; computed columns are
written as real numeric cells.

One caveat worth knowing: pass-through cells always travel as *text*. In a
CSV → xlsx file, numeric-looking columns are therefore stored as text cells —
re-reading that file treats them as numbers-stored-as-text (a row error, per
the spec). Chained pipelines should keep role columns numeric or re-export
from the original input.

## Customizing output

`OrderMetadata.builder()` knobs:

| Method | Default | Effect |
|---|---|---|
| `outputColumns(...)` | input columns + 2 totals | Explicit ordered output layout |
| `summaryMarker(String)` | `"TOTAL"` | Summary-row marker text |
| `summaryMarkerColumn(String)` | first output column | Where the marker lands |
| `outputScale(int)` | `2` | Decimal places on computed values |
| `roundingMode(RoundingMode)` | `HALF_UP` | Rounding for computed values |
| `sumRoundedLines(boolean)` | `true` | Totals sum rounded line values |
| `errorStrategy(ErrorStrategy)` | `FAIL_FAST` | Row-error behavior |
| `allowNegative(boolean)` | `false` | Accept negative qty/price/vat |

Explicit layout — rename, reorder, drop columns, add custom computed ones:

```java
OrderMetadata meta = OrderMetadata.builder()
        .bind(ColumnRole.QUANTITY,   "qty")
        .bind(ColumnRole.UNIT_PRICE, "price")
        .bind(ColumnRole.VAT_PERCENT,"vat")
        .outputColumns(
                OutputColumn.passThrough("product_id", "SKU"),   // renamed
                OutputColumn.passThrough("qty"),
                OutputColumn.computed(ComputedType.LINE_TOTAL_BEFORE_TAX, "net"),
                OutputColumn.computed(ComputedType.LINE_TOTAL_AFTER_TAX,  "gross"),
                // custom column: tax amount = gross − net
                OutputColumn.computed(
                        row -> CellValue.number(
                                row.computed(ComputedType.LINE_TOTAL_AFTER_TAX)
                                   .subtract(row.computed(ComputedType.LINE_TOTAL_BEFORE_TAX))),
                        "tax_amount"))
        .build();
```

## Error handling

Two error classes:

- **Schema errors** — always fatal, thrown before any row is processed:
  missing bound column, duplicate headers, duplicate output names, output
  column referencing a missing input, missing header row.
- **Row errors** — bad cell content (non-numeric quantity, empty price,
  negative values when disallowed, CSV field-count mismatch). Controlled by
  `ErrorStrategy`:
  - `FAIL_FAST` (default): throw `OrderProcessingException` carrying a
    `RowError` (`lineNumber`, `column`, `rawValue`, `message`).
  - `SKIP_AND_COLLECT`: skip the row, collect the `RowError` in
    `OrderResult.errors()`, totals cover good rows only.

```java
OrderResult r = OrderCalculator.process(source, sink, meta);
r.errors().forEach(e ->
        System.err.println("line " + e.lineNumber() + ": " + e.message()));
```

IO/parse failures surface as `OrderProcessingException` wrapping the cause.
On failure the output file may be partially written.

## Custom formats (SPI)

Implement three interfaces in `dev.hieplp.order.calculator.spi` to support any
tabular format (JSON, fixed-width, a database cursor, …):

```java
interface TabularSource extends AutoCloseable {
    List<String> header() throws IOException;
    Stream<TabularRow> rows() throws IOException;   // lazy, one row at a time
}

interface TabularRow {
    String text(String column);        // display value for pass-through cells
    BigDecimal number(String column);  // native numeric value for role columns
    long lineNumber();                 // 1-based, for error diagnostics
}

interface TabularSink extends AutoCloseable {
    void writeHeader(List<String> columns) throws IOException;
    void writeRow(List<CellValue> cells) throws IOException;
}
```

`CellValue` is typed on purpose: `CellValue.number(...)` must stay a real
number in formats that support it (Excel numeric cells); text formats render
`toPlainString()`. `TabularRow.number()` must return the stored number, not a
parsed display string — `1,234.56` as text would corrupt the value.

## Try the demo

`order-calculator-app` generates a sample order in CSV and XLSX, runs all four
format pairs, and prints each `OrderResult`:

```bash
./gradlew :order-calculator-app:run
# inputs/outputs land in order-calculator-app/build/demo/
```

## Build & test

```bash
./gradlew build        # compile + test all modules
./gradlew test         # tests only (JUnit 5, incl. golden-file format-pair tests)
```

## License

Apache-2.0
