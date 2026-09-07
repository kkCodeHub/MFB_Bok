package se.swedsoft.bookkeeping.data.common;


import se.swedsoft.bookkeeping.gui.util.table.SSTableSearchable;
import java.time.LocalDate;
import java.util.LinkedList;
import java.util.List;


/**
 * User: Andreas Lago
 * Date: 2006-mar-20
 * Time: 16:00:24
 */
public class SSPaymentTerm implements SSTableSearchable {

    // Constant for serialization versioning.
    static final long serialVersionUID = 1L;

    private String iName;

    private String iDescription;

    private Integer iDays;

    /**
     * Constructor.
     */
    public SSPaymentTerm() {}

    /**
     * Constructor.
     *
     * @param pName
     * @param pDescription
     */
    public SSPaymentTerm(String pName, String pDescription) {
        iName = pName;
        iDescription = pDescription;
    }

    // //////////////////////////////////////////////////
    public void dispose() {
        iName = null;
        iDescription = null;
        iDays = null;
    }

    /**
     *
     * @return the name
     */
    public String getName() {
        return iName;
    }

    /**
     *
     * @param iName
     */
    public void setName(String iName) {
        this.iName = iName;
    }

    // //////////////////////////////////////////////////

    /**
     *
     * @return
     */
    public String getDescription() {
        return iDescription;
    }

    /**
     *
     * @param iDescription
     */
    public void setDescription(String iDescription) {
        this.iDescription = iDescription;
    }

    // //////////////////////////////////////////////////

    /**
     * Returns the configured number of days for this payment term.
     *
     * @return number of days, or {@code null} when not explicitly set
     */
    public Integer getDays() {
        return iDays;
    }

    /**
     * Sets the configured number of days for this payment term.
     *
     * @param iDays number of days
     */
    public void setDays(Integer iDays) {
        this.iDays = iDays;
    }

    // //////////////////////////////////////////////////

    /**
     * Returns the number of days for this payment term.
     *
     * <p>Primary source is the explicit {@code days} field. If missing, this method
     * falls back to legacy behavior where the name was interpreted as a number.</p>
     *
     * @return number of days, never {@code null}
     */
    public Integer decodeValue() {
        if (iDays != null) {
            return iDays;
        }
        try {
            return Integer.decode(iName);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * Adds the decoded number of days to the given date.
     *
     * @param iDate base date to add days to
     * @return a new LocalDate with days added to the provided date
     */
    public LocalDate addDaysToLocalDate(LocalDate iDate) {
        LocalDate iBaseDate = iDate != null ? iDate : LocalDate.now();
        int iDays = decodeValue();

        return iBaseDate.plusDays(iDays);
    }

    // //////////////////////////////////////////////////

    /**
     * Returns the render string to be shown in the tables
     *
     * @return The searchable string
     */
    public String toRenderString() {
        return iName;
    }

    public boolean equals(Object obj) {
        if (obj instanceof SSPaymentTerm) {
            SSPaymentTerm iUnit = (SSPaymentTerm) obj;

            return iName.equals(iUnit.iName);
        }
        return false;
    }

    public String toString() {
        return iDescription;
    }

    /**
     *
     * @return
     */
    public static List<SSPaymentTerm> getDefaultPaymentTerms() {
        List<SSPaymentTerm> iPaymentTerms = new LinkedList<>();

        iPaymentTerms.add(new SSPaymentTerm("K", "Kontant"));
        iPaymentTerms.add(new SSPaymentTerm("PF", "Postförskott"));
        iPaymentTerms.add(new SSPaymentTerm("30", "30 dagar netto"));
        iPaymentTerms.add(new SSPaymentTerm("52", "10 dagar netto"));
        iPaymentTerms.add(new SSPaymentTerm("10", "10 dagar netto"));

        return iPaymentTerms;
    }
}
