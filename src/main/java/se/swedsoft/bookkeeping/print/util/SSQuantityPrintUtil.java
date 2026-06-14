package se.swedsoft.bookkeeping.print.util;

import java.math.BigDecimal;

/**
 * Quantity presentation helpers for print/report output.
 * Domain values are stored as tenths (e.g. 25 == 2.5) and must be
 * converted at the presentation boundary.
 */
public final class SSQuantityPrintUtil {

    private SSQuantityPrintUtil() {}

    /**
     * Converts a tenths quantity to a decimal value used in printed reports.
     *
     * @param tenths quantity in tenths
     * @return quantity as decimal, or null if input is null
     */
    public static BigDecimal toDisplay(Integer tenths) {
        if (tenths == null) {
            return null;
        }
        return BigDecimal.valueOf(tenths, 1);
    }
}

