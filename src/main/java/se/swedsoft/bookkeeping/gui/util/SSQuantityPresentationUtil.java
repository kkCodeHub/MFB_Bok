package se.swedsoft.bookkeeping.gui.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Converts quantity values between storage format (tenths as Integer) and
 * UI format (decimal quantity shown to users).
 */
public final class SSQuantityPresentationUtil {

    private SSQuantityPresentationUtil() {}

    /**
     * Converts stored tenths quantity to a decimal UI value.
     *
     * @param storedTenths quantity in tenths (e.g. 25 = 2.5)
     * @return decimal quantity for UI display, or {@code null} if input is null
     */
    public static BigDecimal toDisplayQuantity(Integer storedTenths) {
        if (storedTenths == null) {
            return null;
        }
        return BigDecimal.valueOf(storedTenths, 1);
    }

    /**
     * Converts a UI quantity object to stored tenths.
     *
     * @param uiValue UI value from table editor
     * @return quantity in tenths for storage, or {@code null} if input is null
     */
    public static Integer toStoredTenths(Object uiValue) {
        if (uiValue == null) {
            return null;
        }
        if (uiValue instanceof Integer) {
            return (Integer) uiValue;
        }
        if (uiValue instanceof BigDecimal) {
            return toStoredTenths((BigDecimal) uiValue);
        }
        if (uiValue instanceof Number) {
            Number iNumber = (Number) uiValue;
            return toStoredTenths(BigDecimal.valueOf(iNumber.doubleValue()));
        }
        return null;
    }

    /**
     * Converts decimal UI quantity to stored tenths.
     *
     * @param uiQuantity decimal quantity from UI
     * @return quantity in tenths
     */
    public static Integer toStoredTenths(BigDecimal uiQuantity) {
        if (uiQuantity == null) {
            return null;
        }
        return uiQuantity.movePointRight(1)
                .setScale(0, RoundingMode.HALF_UP)
                .intValue();
    }
}

