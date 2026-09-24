package dev.hieplp.order.calculator;

import dev.hieplp.order.calculator.error.ErrorStrategy;
import dev.hieplp.order.calculator.error.OrderProcessingException;
import dev.hieplp.order.calculator.model.ColumnRole;
import dev.hieplp.order.calculator.model.ComputedType;
import dev.hieplp.order.calculator.model.OutputColumn;
import org.junit.jupiter.api.Test;

import java.math.RoundingMode;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OrderMetadataTest {

    @Test
    void buildsWhenAllRolesBound() {
        OrderMetadata meta = OrderMetadata.builder()
                .bind(ColumnRole.QUANTITY, "quantity")
                .bind(ColumnRole.UNIT_PRICE, "price_per_item")
                .bind(ColumnRole.VAT_PERCENT, "vat")
                .build();

        assertEquals("quantity", meta.columnName(ColumnRole.QUANTITY));
        assertEquals("price_per_item", meta.columnName(ColumnRole.UNIT_PRICE));
        assertEquals("vat", meta.columnName(ColumnRole.VAT_PERCENT));

        assertEquals(2, meta.outputScale());
        assertEquals(RoundingMode.HALF_UP, meta.roundingMode());
        assertTrue(meta.sumRoundedLines());
        assertEquals(ErrorStrategy.FAIL_FAST, meta.errorStrategy());
        assertEquals("TOTAL", meta.summaryMarker());
        assertNull(meta.summaryMarkerColumn());
        assertTrue(meta.outputColumns().isEmpty());
    }

    @Test
    void failsWhenRoleBindingMissing() {
        OrderMetadata.Builder builder = OrderMetadata.builder()
                .bind(ColumnRole.QUANTITY, "quantity")
                .bind(ColumnRole.UNIT_PRICE, "price_per_item");

        assertThrows(OrderProcessingException.class, builder::build);
    }

    @Test
    void keepsExplicitOutputColumns() {
        OrderMetadata meta = OrderMetadata.builder()
                .bind(ColumnRole.QUANTITY, "quantity")
                .bind(ColumnRole.UNIT_PRICE, "price_per_item")
                .bind(ColumnRole.VAT_PERCENT, "vat")
                .outputColumns(
                        OutputColumn.passThrough("product_id"),
                        OutputColumn.computed(ComputedType.LINE_TOTAL_BEFORE_TAX, "total_price_before_tax"),
                        OutputColumn.computed(ComputedType.LINE_TOTAL_AFTER_TAX, "total_price_after_tax"))
                .build();

        List<OutputColumn> cols = meta.outputColumns();
        assertEquals(3, cols.size());
        assertInstanceOf(OutputColumn.PassThrough.class, cols.get(0));
        assertInstanceOf(OutputColumn.Computed.class, cols.get(1));
        assertEquals("product_id", cols.get(0).outputName());
        assertEquals("total_price_after_tax", cols.get(2).outputName());
    }

    @Test
    void rejectsNegativeOutputScale() {
        OrderMetadata.Builder builder = OrderMetadata.builder()
                .bind(ColumnRole.QUANTITY, "q")
                .bind(ColumnRole.UNIT_PRICE, "p")
                .bind(ColumnRole.VAT_PERCENT, "v")
                .outputScale(-1);

        assertThrows(OrderProcessingException.class, builder::build);
    }
}
