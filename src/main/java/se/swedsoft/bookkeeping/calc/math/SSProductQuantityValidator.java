package se.swedsoft.bookkeeping.calc.math;

import se.swedsoft.bookkeeping.data.SSProduct;

/**
 * Validation utility for product quantity rules.
 *
 * <p>Quantities are represented internally as tenths (i.e. integer value * 10),
 * so a quantity of {@code 25} represents 2.5 units.</p>
 *
 * <p>Products flagged with <em>hela antal endast</em> (whole quantities only)
 * require that the tenths value is divisible by {@code 10}, meaning no decimal
 * fraction is allowed.</p>
 */
public final class SSProductQuantityValidator {

    /** Tenths per whole unit — one whole unit equals {@value #TENTHS_PER_UNIT} tenths. */
    public static final int TENTHS_PER_UNIT = 10;

    private SSProductQuantityValidator() {}

    /**
     * Returns {@code true} when the given quantity (in tenths) satisfies the
     * product's whole-quantity rule.
     *
     * <ul>
     *   <li>If {@code product} or {@code quantityTenths} is {@code null}, the
     *       quantity is considered valid.</li>
     *   <li>If {@link SSProduct#isOnlyWholeQuantity()} is {@code true}, the
     *       tenths value must be divisible by {@value #TENTHS_PER_UNIT}.</li>
     *   <li>Otherwise any tenths value is valid.</li>
     * </ul>
     *
     * @param product        the product whose rules shall be applied
     * @param quantityTenths the quantity expressed in tenths (e.g. 25 = 2.5 units)
     * @return {@code true} if the quantity is valid for the product
     */
    public static boolean isValidQuantity(SSProduct product, Integer quantityTenths) {
        if (product == null || quantityTenths == null) {
            return true;
        }
        if (product.isOnlyWholeQuantity()) {
            return quantityTenths % TENTHS_PER_UNIT == 0;
        }
        return true;
    }

    /**
     * Returns {@code true} when the given quantity (in tenths) is a whole number
     * of units, regardless of any product rule.
     *
     * @param quantityTenths the quantity expressed in tenths
     * @return {@code true} if the decimal fraction is zero
     */
    public static boolean isWholeQuantity(Integer quantityTenths) {
        if (quantityTenths == null) {
            return true;
        }
        return quantityTenths % TENTHS_PER_UNIT == 0;
    }
}

