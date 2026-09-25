package se.swedsoft.bookkeeping.print.util;

import se.swedsoft.bookkeeping.data.SSVoucher;

/**
 * Immutable print helper for voucher reference presentation.
 * Combines voucher series and voucher number into one display value.
 */
public final class SSVoucherPrintReference {
    private final String iSeries;
    private final Integer iNumber;

    /**
     * Creates a print reference from explicit series and number.
     *
     * @param series series code, usually A-Z
     * @param number voucher number
     */
    public SSVoucherPrintReference(String series, Integer number) {
        iSeries = normalizeSeries(series);
        iNumber = number;
    }

    /**
     * Creates a print reference from a voucher.
     *
     * @param voucher voucher to read values from
     * @return reference value object
     */
    public static SSVoucherPrintReference fromVoucher(SSVoucher voucher) {
        if (voucher == null) {
            return new SSVoucherPrintReference(null, null);
        }
        return new SSVoucherPrintReference(voucher.getSeries(), voucher.getNumber());
    }

    /**
     * Formats a voucher directly to display text in format "A 123".
     *
     * @param voucher voucher source
     * @return display text
     */
    public static String toDisplayString(SSVoucher voucher) {
        return fromVoucher(voucher).toDisplayString();
    }

    /**
     * Formats series and number directly to display text in format "A 123".
     *
     * @param series series code
     * @param number voucher number
     * @return display text
     */
    public static String toDisplayString(String series, Integer number) {
        return new SSVoucherPrintReference(series, number).toDisplayString();
    }

    /**
     * Gets the normalized series.
     *
     * @return series, empty string when missing
     */
    public String getSeries() {
        return iSeries;
    }

    /**
     * Gets the number.
     *
     * @return number, or null when missing
     */
    public Integer getNumber() {
        return iNumber;
    }

    /**
     * Returns the printable voucher reference in format "A 123".
     *
     * @return formatted voucher reference
     */
    public String toDisplayString() {
        if (iSeries.isEmpty() && iNumber == null) {
            return "";
        }
        if (iSeries.isEmpty()) {
            return String.valueOf(iNumber);
        }
        if (iNumber == null) {
            return iSeries;
        }
        return iSeries + " " + iNumber;
    }

    private static String normalizeSeries(String series) {
        if (series == null) {
            return "";
        }
        String normalized = series.trim();
        if (normalized.isEmpty()) {
            return "";
        }
        return normalized.toUpperCase();
    }
}
