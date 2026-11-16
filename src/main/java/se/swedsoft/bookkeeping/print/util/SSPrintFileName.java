package se.swedsoft.bookkeeping.print.util;


import java.time.LocalDate;

/**
 * Builds suggested file names for printed sales documents.
 */
public final class SSPrintFileName {

    private SSPrintFileName() {}

    /**
     * Builds a name on the form {@code <prefix> <number>, <customer> <year>}.
     * Missing parts are left out and characters that are illegal in file names are removed.
     *
     * @param pPrefix document type prefix
     * @param pNumber document number, may be null
     * @param pCustomerNr customer number, may be null
     * @param pDate document date, may be null
     * @return the file name without extension
     */
    public static String build(String pPrefix, Integer pNumber, String pCustomerNr, LocalDate pDate) {
        StringBuilder iName = new StringBuilder(pPrefix == null ? "" : pPrefix.trim());

        if (pNumber != null) {
            iName.append(' ').append(pNumber);
        }
        String iCustomerNr = pCustomerNr == null ? "" : pCustomerNr.trim();

        if (!iCustomerNr.isEmpty()) {
            iName.append(',').append(' ').append(iCustomerNr);
        }
        if (pDate != null) {
            iName.append(' ').append(pDate.getYear());
        }
        return sanitize(iName.toString());
    }

    /**
     * Removes characters that are not allowed in file names and parentheses.
     *
     * @param pName raw name
     * @return sanitized name
     */
    public static String sanitize(String pName) {
        if (pName == null) {
            return "";
        }
        return pName.replaceAll("[\\\\/:*?\"<>|()\\p{Cntrl}]", "").replaceAll("\\s+", " ").trim();
    }
}
