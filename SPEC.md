# order-calculator — Specification

Version: 0.2 (draft)
Stack: Java 21 · Gradle (Kotlin DSL, multi-module) · Apache Commons CSV · Apache POI

## 1. Purpose

A reusable Java library that reads an order's line items from a **tabular
source** (CSV, Excel, …), computes per-line totals before and after tax,
appends order-level totals as a summary row, and writes the enriched result to
a **tabular sink** (CSV, Excel, …).

Two "nothing is fixed" guarantees:

- **Schema**: input column names and the output column list are supplied by the
  caller as metadata.
- **Format**: input and output formats are independent — CSV in → Excel out,
  Excel in → CSV out, same→same, or a custom format via SPI.

## 2. Terminology

| Term | Meaning |
|---|---|
| Line item | One data row in the input |
| `TabularSource` | Format-specific reader: header + streaming rows |
| `TabularSink` | Format-specific writer: header + streaming rows |
| Computed column | Output column derived by the library (e.g. line total) |
| Pass-through column | Output column copied verbatim from the input |
| Summary row | Trailing row carrying order-level totals |
| Metadata | `OrderMetadata` — role bindings + output layout + knobs |

## 3. Architecture: format-agnostic core

```
┌──────────────┐   rows    ┌─────────────────────┐   rows    ┌────────────┐
│ TabularSource│──────────▶│  engine (core)      │──────────▶│ TabularSink│
│ csv · excel  │  header   │  bind roles · math  │  header   │ csv · excel│
│   · custom   │           │  accumulate · errors│           │   · custom │
└──────────────┘           └─────────────────────┘           └────────────┘
```

- The engine never sees a file format — only `TabularSource`/`TabularSink`.
- New formats are added by implementing the SPI, either inside the repo
  (shipped modules) or by consumers (custom SPI impl).

### SPI (in `core`)

```java
interface TabularSource extends AutoCloseable {
    List<String> header();              // logical column names
    Stream<TabularRow> rows();          // streaming, one row at a time
}

interface TabularRow {
    String text(String column);               // pass-through value as text
    BigDecimal number(String column);         // typed numeric access
    long lineNumber();                        // for RowError diagnostics
}

interface TabularSink extends AutoCloseable {
    void writeHeader(List<String> columns);
    void writeRow(List<CellValue> cells);
}

sealed interface CellValue permits CellValue.Text, CellValue.Number {
    static CellValue text(String s) { ... }
    static CellValue number(BigDecimal n) { ... }
}
```

Why `CellValue` is typed: pass-through cells travel as **text** (display value),
but computed totals travel as `BigDecimal` — so the Excel sink writes real
numeric cells (sortable/summable in Excel) while the CSV sink renders
`toPlainString()`. Same `writeRow` call, correct per-format behavior.

Why `TabularRow.number()`: Excel cells store real numbers. String-parsing a
formatted display value like `1,234.56` would corrupt `BigDecimal` parsing, so
numeric role columns must be read natively per format.

### Shipped format modules

| Module | Source | Sink | Notes |
|---|---|---|---|
| `order-calculator-core` | — | — | engine, metadata, SPI. Zero deps. |
| `order-calculator-csv` | ✓ | ✓ | Apache Commons CSV |
| `order-calculator-excel` | ✓ | ✓ | Apache POI, `.xlsx` only |

### CSV format config (defaults)

Header row, `,` delimiter, `"` quoting, trim, UTF-8. `CSVFormat` + `Charset`
overridable.

### Excel format config (defaults)

- `.xlsx` read & write (`.xls` = open question, likely out of scope for v1).
- Read: first sheet by default (`sheet(index|name)` override); `headerRowIndex`
  default `0` for files with title rows; data ends at first fully blank row.
- Cell reads: numeric role columns via `getNumericCellValue()` →
  `BigDecimal.valueOf(...)`; pass-through via `DataFormatter` (what-you-see
  text). Formula cells → last cached value.
- Write: SXSSF streaming (constant memory on large sheets); computed columns
  written as numeric cells; summary marker as text; sheet name configurable.

### Format selection

```java
TabularSource in  = Sources.csv(path);            // or Sources.excel(...)
TabularSink   out = Sinks.excel(path);            // or Sinks.csv(...)
// convenience:
TabularSource in2 = Sources.detect(path);         // by extension: .csv, .xlsx
```

`detect` is a convenience only; explicit construction is the primary API.
Unknown extension → `OrderProcessingException`. Custom formats: callers
implement the SPI directly — no service-loader magic in v1.

## 4. Functional requirements

### 4.1 Input

- FR-1: Input must have a header row (CSV: first record; Excel: `headerRowIndex`). Absent header = schema error.
- FR-2: Sources/sinks are built from `Path`, `Reader`/`InputStream`, `Writer`/`OutputStream` where the format permits (Excel: `Path`/`InputStream`/`OutputStream`).
- FR-3: Three numeric inputs are bound by **role**, not name:
  - `QUANTITY` — decimal (may be fractional)
  - `UNIT_PRICE` — price per item
  - `VAT_PERCENT` — percentage number (`10` = 10%)
- FR-4: `OrderMetadata` binds roles → actual header names. Any names are legal.
- FR-5: All unbound columns are pass-through attributes. `product_id` means nothing to the engine.
- FR-6: Streaming end-to-end — O(1) memory in row count for both reading and writing.

### 4.2 Computation

- FR-7: `line_before_tax = QUANTITY × UNIT_PRICE`; `line_after_tax = line_before_tax × (1 + VAT_PERCENT / 100)`
- FR-8: `order_before_tax = Σ line_before_tax`; `order_after_tax = Σ line_after_tax`
- FR-9: Totals accumulate **rounded** line values (displayed lines == totals). Toggle: `sumRoundedLines` (default true).

### 4.3 Output

- FR-10: Output column list is ordered metadata: `PassThrough(inputName[, outputName])` | `Computed(type, name)`.
- FR-11: Built-in `ComputedType`: `LINE_TOTAL_BEFORE_TAX`, `LINE_TOTAL_AFTER_TAX`.
- FR-12: Custom computed columns via `ComputedColumn` SPI: `(RowView) -> CellValue`.
- FR-13: Default output layout = input columns in order + the two totals appended.
- FR-14: Input row order preserved.

### 4.4 Summary row

- FR-15: Exactly one summary row appended.
- FR-16: Contents: configurable marker literal (default `"TOTAL"`) in the first output column (marker column configurable); `order_before_tax` / `order_after_tax` as **numeric** `CellValue`s in their columns; empty cells elsewhere (custom computed columns stay empty unless they opt into an aggregation hook — open question §14).
- FR-17: Header-only input → header + `TOTAL` row with `0.00` totals.

## 5. Public API (sketch)

Package root: `dev.hieplp.order.calculator` (engine, `OrderMetadata`, `OrderResult`) with `.model` (domain/value types), `.spi` (`TabularSource`/`TabularSink`/`TabularRow`), and `.error` (`ErrorStrategy`, `RowError`, `OrderProcessingException`) sub-packages — format modules add `.csv` / `.excel`.

```java
OrderMetadata meta = OrderMetadata.builder()
    .bind(ColumnRole.QUANTITY,    "quantity")
    .bind(ColumnRole.UNIT_PRICE,  "price_per_item")
    .bind(ColumnRole.VAT_PERCENT, "vat")
    .outputColumns(List.of(
        OutputColumn.passThrough("product_id"),
        OutputColumn.passThrough("quantity"),
        OutputColumn.passThrough("price_per_item"),
        OutputColumn.passThrough("vat"),
        OutputColumn.computed(ComputedType.LINE_TOTAL_BEFORE_TAX, "total_price_before_tax"),
        OutputColumn.computed(ComputedType.LINE_TOTAL_AFTER_TAX,  "total_price_after_tax")
    ))
    .build();

OrderCalculator calc = OrderCalculator.create();   // stateless, thread-safe

OrderResult r1 = calc.process(Sources.csv(inCsv),    Sinks.excel(outXlsx), meta);  // CSV → Excel
OrderResult r2 = calc.process(Sources.excel(inXlsx), Sinks.csv(outCsv),    meta);  // Excel → CSV
OrderResult r3 = calc.process(readerSource, writerSink, meta);                     // streams

record OrderResult(
    long lineCount,
    long skippedCount,
    BigDecimal orderTotalBeforeTax,
    BigDecimal orderTotalAfterTax,
    List<RowError> errors
) {}
```

| Type | Kind | Notes |
|---|---|---|
| `OrderMetadata` | immutable, builder | role bindings, output columns, rounding, error strategy |
| `ColumnRole` | enum | `QUANTITY`, `UNIT_PRICE`, `VAT_PERCENT` |
| `ComputedType` | enum | `LINE_TOTAL_BEFORE_TAX`, `LINE_TOTAL_AFTER_TAX` |
| `OutputColumn` | sealed interface | `PassThrough` / `Computed` |
| `ComputedColumn` | SPI interface | custom derived columns |
| `TabularSource` / `TabularSink` / `TabularRow` / `CellValue` | SPI | format abstraction |
| `Sources` / `Sinks` | factory classes | `csv(...)`, `excel(...)`, `detect(path)` |
| `OrderCalculator` | class | stateless, thread-safe engine |
| `OrderResult`, `RowError` | records | `RowError`: `lineNumber, column, rawValue, message` |
| `OrderProcessingException` | unchecked | wraps IO + processing failures |

All public types immutable. `module-info.java` in every module (Commons CSV and
POI are both named modules).

## 6. Numbers & rounding

- `BigDecimal` only; no `double` in the pipeline.
- Text parsing: plain decimal notation only; reject thousands separators and
  scientific notation (flag to relax = open question).
- Excel numeric reads bypass text formatting entirely (`getNumericCellValue`).
- Default scale **2**, `HALF_UP`, applied per line before write & accumulate.
- `QUANTITY` is never rounded (operand, not result).
- Decimal `VAT_PERCENT` supported (`8.5`); `VAT/100` is exact in `BigDecimal`.
- Numeric output: `toPlainString()` at scale (`20.00`, never `2E+1`); in Excel
  sinks, written as numeric cells.
- Knobs: `outputScale`, `roundingMode`, `sumRoundedLines`.

## 7. Error handling

- `ErrorStrategy`: `FAIL_FAST` (default) | `SKIP_AND_COLLECT` (skip row, record
  `RowError`, totals over good rows only).
- **Schema errors** (always fatal, before processing): missing bound column,
  output column referencing missing input, duplicate header names, duplicate
  output names, missing header row, unbound required role.
- **Row errors** (per strategy): non-numeric/empty role cells (incl. Excel
  text-cell-in-numeric-column), field-count mismatch (CSV), blank Excel row
  inside data region treated as end-of-data not error (config: `strictBlankRows`),
  negative values rejected unless `allowNegative`.
- IO/parse failures → `OrderProcessingException` wrapping the cause. Partial
  output on failure is documented, not silently swallowed.

## 8. Edge cases (must be tested)

| Case | Behavior |
|---|---|
| `vat = 0` | after == before |
| `quantity = 0` or fractional `1.5` | works |
| Decimal vat `8.5` | works |
| Empty input / header only | §4.4 |
| Missing bound column | schema error |
| `abc` in quantity; too few/many CSV fields | row error |
| Duplicate headers | schema error |
| Excel: numbers stored as text | row error (or text-parse fallback — open question) |
| Excel: blank row mid-data | end of data (default) |
| Excel: formula cells | cached value |
| Excel write of 1M rows | SXSSF, constant memory |
| Cross-format: Excel → CSV | numeric cells → plain strings at scale |
| 1M+ rows any format | streaming, no full buffering |

## 9. Non-functional requirements

- Java 21 toolchain; records, sealed types.
- Engine + metadata immutable/thread-safe; sources/sinks single-use, not thread-safe (documented).
- O(1) memory; ≥ 100k rows/s target.
- Dependencies per module only: `core` = none; `csv` = commons-csv; `excel` = poi-ooxml(-lite). All pinned ≥ 7 days old. JUnit 5 test-only.
- No logging framework — diagnostics via `OrderResult`/`RowError`.
- License + maven-publish metadata before release; sources & javadoc jars.

## 10. Build — multi-module Gradle (Kotlin DSL)

```
settings.gradle.kts: include("order-calculator-core", "order-calculator-csv", "order-calculator-excel")

order-calculator-core   : no deps; engine + SPI + metadata
order-calculator-csv    : api(core); api("org.apache.commons:commons-csv:<pinned>")
order-calculator-excel  : api(core); api("org.apache.poi:poi-ooxml(-lite):<pinned>")

all: java toolchain 21, java-library, maven-publish, JUnit 5
```

Consumers depend only on the format modules they need — a CSV-only user never
pulls POI's dependency tree.

## 11. Testing requirements

- Unit: math/rounding, metadata validation, `ErrorStrategy`s, `CellValue` semantics.
- Golden-file tests per format pair: csv→csv, csv→xlsx, xlsx→csv, xlsx→xlsx (xlsx compared cell-by-cell via POI in tests).
- §8 edge-case matrix.
- Streaming/memory smoke test (1M rows, csv→xlsx).
- Public-API-only tests.

## 12. End-to-end example

Metadata: `quantity→QUANTITY`, `price_per_item→UNIT_PRICE`, `vat→VAT_PERCENT`; default layout.

Input (`in.csv`):

```csv
product_id,quantity,price_per_item,vat
P1,2,10.00,10
P2,1,5.50,20
P3,3,3.33,8.5
```

Output (`out.csv`, or an `.xlsx` with identical cell values):

```csv
product_id,quantity,price_per_item,vat,total_price_before_tax,total_price_after_tax
P1,2,10.00,10,20.00,22.00
P2,1,5.50,20,5.50,6.60
P3,3,3.33,8.5,9.99,10.84
TOTAL,,,,35.49,39.44
```

`OrderResult` → `lineCount=3, orderTotalBeforeTax=35.49, orderTotalAfterTax=39.44`.

## 13. Out of scope (v1)

- Multi-order files / grouping by order key (extension: `RowGrouper`)
- `.xls` (legacy binary), JSON/Parquet shipped modules (custom SPI only)
- VAT-inclusive pricing, jurisdiction rules, currency codes
- Auto-detection beyond file extension

## 14. Open questions

- [ ] Final group/artifact coordinates, license.
- [ ] Aggregation hook so custom `ComputedColumn`s can fill the summary row?
- [ ] `poi-ooxml` vs `poi-ooxml-lite` for the excel module.

## Appendix A — Product Owner questionnaire (yes/no)

Each question is answerable yes/no. "Default" is what v1 assumes if the
question goes unanswered — answering "no" to a default-yes (or vice versa)
changes scope and must be fed back into the spec.

### Input data

| # | Question | Default if unanswered |
|---|---|---|
| 1 | Is every input file guaranteed to have a header row? | Yes — missing header is an error |
| 2 | Is there always exactly one order per file? | Yes — multi-order grouping is out of scope |
| 3 | Can `quantity` be fractional (e.g. 1.5 kg)? | Yes — supported |
| 4 | Can `quantity` be negative (returns / credit lines)? | No — rejected as row error |
| 5 | Can `price_per_item` be negative (discounts, credit notes)? | No — rejected as row error |
| 6 | Can `vat` be 0%? | Yes — after-tax = before-tax |
| 7 | Can `vat` be fractional (e.g. 8.5%)? | Yes — supported |
| 8 | Can blank rows appear in the middle of an Excel data region? | No — first blank row = end of data |
| 9 | Can Excel inputs contain numbers stored as text? | No — treated as row error |

### Numbers & rounding

| # | Question | Default if unanswered |
|---|---|---|
| 10 | Is 2 decimal places always correct (no JPY/KWD-style 0- or 3-decimal currencies)? | Yes — scale 2, configurable |
| 11 | Must the order total equal the sum of the *displayed* (rounded) line totals? | Yes — totals accumulate rounded lines |
| 12 | May input numbers contain separators (`1,234.56`) or decimal comma (`10,50`)? | No — rejected as row error |

### Error handling

| # | Question | Default if unanswered |
|---|---|---|
| 13 | Should one bad row abort the whole file? | Yes — `FAIL_FAST` default (`SKIP_AND_COLLECT` opt-in) |
| 14 | Is an empty cell in quantity/price/vat an error? | Yes — never silently treated as 0 |
| 15 | Should row errors ever be written into the output file? | No — reported via `OrderResult` only |

### Output

| # | Question | Default if unanswered |
|---|---|---|
| 16 | Is a single `TOTAL` row at the bottom the required layout? | Yes |
| 17 | Should the summary row also carry a total-quantity figure? | No — only the two money totals |
| 18 | Do you need a tax-amount column (`after − before`) per line and/or in totals? | No — available via custom computed column |
| 19 | Must computed totals be *numeric* cells in Excel output (not text)? | Yes |

### Formats & scale

| # | Question | Default if unanswered |
|---|---|---|
| 20 | Is `.xlsx` enough — no legacy `.xls` files? | Yes — `.xlsx` only |
| 21 | Can input files exceed ~100k rows? | Assume yes — design streams regardless |
| 22 | Any other formats needed soon (JSON, fixed-width, Google Sheets)? | No — covered by SPI, not shipped |
