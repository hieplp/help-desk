package dev.hieplp.order.calculator;

import dev.hieplp.order.calculator.error.ErrorStrategy;
import dev.hieplp.order.calculator.error.RowError;

import java.math.BigDecimal;
import java.util.List;

/**
 * Outcome of one {@link OrderCalculator#process} call.
 *
 * @param lineCount           data rows successfully processed
 * @param skippedCount        rows skipped under {@link ErrorStrategy#SKIP_AND_COLLECT}
 * @param orderTotalBeforeTax Σ line totals before tax
 * @param orderTotalAfterTax  Σ line totals after tax
 * @param errors              collected row errors (empty on FAIL_FAST success)
 */
public record OrderResult(
        long lineCount,
        long skippedCount,
        BigDecimal orderTotalBeforeTax,
        BigDecimal orderTotalAfterTax,
        List<RowError> errors
) {

    public OrderResult {
        errors = List.copyOf(errors);
    }
}
