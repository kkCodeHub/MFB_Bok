/*
 * 2005-2010
 * $Id$
 */
package se.swedsoft.bookkeeping.data;


import se.swedsoft.bookkeeping.data.base.SSSaleRow;
import se.swedsoft.bookkeeping.data.system.SSDB;
import se.swedsoft.bookkeeping.gui.util.table.SSTableSearchable;
import se.swedsoft.bookkeeping.util.SSDateUtil;

import java.io.Serializable;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;


/**
 * V2 target model for projects.
 *
 * <p>This is the supported project representation in active V2 code paths.</p>
 */
public class SSNewProject implements Serializable, SSTableSearchable {

    // Constant for serialization versioning.
    static final long serialVersionUID = 1L;

    private String iNumber;
    private String iName;

    private String iDescription;

    private boolean iConcluded;

    private LocalDate iConcludedDate;

    /**
     * Default constructor.
     */
    public SSNewProject() {}

    /**
     *
     * @param pNumber
     * @param pName
     * @param pDescription
     */
    public SSNewProject(String pNumber, String pName, String pDescription) {
        iNumber = pNumber;
        iName = pName;
        iDescription = pDescription;
        iConcluded = false;
        iConcludedDate = null;
    }


    // /////////////////////////////////////////////////////////////////////////

    // /////////////////////////////////////////////////////////////////////////

    /**
     *
     * @return
     */
    public String getNumber() {
        return iNumber;
    }

    /**
     *
     * @param number
     */
    public void setNumber(String number) {
        iNumber = number;
    }

    // /////////////////////////////////////////////////////////////////////////

    /**
     *
     * @return
     */
    public String getName() {
        return iName;
    }

    /**
     *
     * @param name
     */
    public void setName(String name) {
        iName = name;
    }

    // /////////////////////////////////////////////////////////////////////////

    /**
     *
     * @return
     */
    public String getDescription() {
        return iDescription;
    }

    /**
     *
     * @param description
     */
    public void setDescription(String description) {
        iDescription = description;
    }

    // /////////////////////////////////////////////////////////////////////////

    /**
     *
     * @return
     */
    public boolean getConcluded() {
        return iConcluded;
    }

    /**
     *
     * @param pConcluded
     */
    public void setConcluded(boolean pConcluded) {
        iConcluded = pConcluded;
    }

    // /////////////////////////////////////////////////////////////////////////

    /**
     *
     * @return
     */
    public Date getConcludedDate() {
        return SSDateUtil.toDate(iConcludedDate);
    }

    /**
     *
     * @param pConcluded
     */
    public void setConcludedDate(Date pConcluded) {
        iConcludedDate = SSDateUtil.toLocalDate(pConcluded);
    }

    public LocalDate getLocalConcludedDate() {
        return iConcludedDate;
    }

    public void setLocalConcludedDate(LocalDate pConcluded) {
        iConcludedDate = pConcluded;
    }

    // /////////////////////////////////////////////////////////////////////////

    /**
     *
     * @param iDate
     * @return
     */
    /**
     * Returns true if this project is concluded on or before {@code iDate}.
     *
     * @param iDate the reference date
     * @return true if concluded
     */
    public boolean isConcluded(LocalDate iDate) {
        return iConcluded && iConcludedDate != null && iDate != null
                && !iConcludedDate.isAfter(iDate);
    }

    // /////////////////////////////////////////////////////////////////////////

    public String toString() {
        DateFormat iFormat = DateFormat.getDateInstance(DateFormat.SHORT);

        StringBuilder sb = new StringBuilder();

        sb.append(iNumber);
        sb.append(" - ");
        sb.append(iName);
        sb.append(", ");
        sb.append(iDescription);
        if (iConcluded) {
            sb.append("(Concluded ");
            sb.append(iFormat.format(iConcludedDate));
            sb.append(") ");
        }
        return sb.toString();
    }

    /**
     *
     * @param obj
     * @return
     */
    public boolean equals(Object obj) {
        if (obj instanceof SSNewProject) {
            return ((SSNewProject) obj).iNumber.equals(iNumber);
        }

        return false;
    }

    /**
     * @return
     */
    public String toRenderString() {
        return iNumber;
    }

    public BigDecimal getProjectRevenueForMonth(SSMonth iMonth) {
        Double iInvoiceSum = 0.0;
        List<SSInvoice> iInvoices = se.swedsoft.bookkeeping.data.system.SSSalesContext.getInvoices();

        for (SSInvoice iInvoice : iInvoices) {
            if (iMonth.isDateInMonth(iInvoice.getLocalDate())) {
                for (SSSaleRow iRow : iInvoice.getRows()) {
                    if (iRow.getProjectNr() != null) {
                        if (iRow.getProjectNr().equals(iNumber) && iRow.getSum().isPresent()) {
                            iInvoiceSum += iRow.getSum().get().doubleValue()
                                    * iInvoice.getCurrencyRate().doubleValue();
                        }
                    }
                }
            }
        }

        List<SSCreditInvoice> iCreditInvoices = se.swedsoft.bookkeeping.data.system.SSSalesContext.getCreditInvoices();
        Double iCreditInvoiceSum = 0.0;

        for (SSCreditInvoice iCreditInvoice : iCreditInvoices) {
            if (iMonth.isDateInMonth(iCreditInvoice.getLocalDate())) {
                for (SSSaleRow iRow : iCreditInvoice.getRows()) {
                    if (iRow.getProjectNr() != null) {
                        if (iRow.getProjectNr().equals(iNumber) && iRow.getSum().isPresent()) {
                            iCreditInvoiceSum += iRow.getSum().get().doubleValue()
                                    * iCreditInvoice.getCurrencyRate().doubleValue();
                        }
                    }
                }
            }
        }
        return new BigDecimal(iInvoiceSum - iCreditInvoiceSum);
    }

}
