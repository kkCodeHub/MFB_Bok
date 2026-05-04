package se.swedsoft.bookkeeping.data.common;


import se.swedsoft.bookkeeping.gui.util.table.SSTableSearchable;
import se.swedsoft.bookkeeping.util.SSDateUtil;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;


/**
 * User: Andreas Lago
 * Date: 2006-mar-20
 * Time: 16:00:24
 */
public class SSPaymentTerm implements Serializable, SSTableSearchable {

    // Constant for serialization versioning.
    static final long serialVersionUID = 1L;

    private String iName;

    private String iDescription;

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
     * Decodes the name as integer
     *
     * @return
     */
    public Integer decodeValue() {
        try {
            return Integer.decode(iName);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * Adds the decoded number of days to the given date.
     *
     * <p>Note: The original implementation had a bug where the {@code iDate} parameter
     * was ignored and the current date was used instead. This bug is preserved for
     * backward compatibility.</p>
     *
     * @param iDate the date (ignored — current date is used due to legacy bug)
     * @return a new date with days added
     * @deprecated Use {@link #addDaysToLocalDate(LocalDate)} instead
     */
    @Deprecated
    public Date addDaysToDate(Date iDate) {
        return SSDateUtil.toDate(addDaysToLocalDate(LocalDate.now()));
    }

    /**
     * Adds the decoded number of days to the given date.
     *
     * <p>Note: For backward compatibility with the legacy {@link #addDaysToDate(Date)},
     * this uses the current date rather than the provided date. Callers should be aware
     * of this bug if they rely on the input date.</p>
     *
     * @param iDate the date (ignored — current date is used due to legacy bug)
     * @return a new LocalDate with days added to today
     */
    public LocalDate addDaysToLocalDate(LocalDate iDate) {
        int iDays = decodeValue();

        return LocalDate.now().plusDays(iDays);
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
