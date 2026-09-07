package se.swedsoft.bookkeeping.data;


import se.swedsoft.bookkeeping.calc.math.SSDateMath;
import se.swedsoft.bookkeeping.calc.math.SSInvoiceMath;
import se.swedsoft.bookkeeping.data.base.SSSaleRow;
import se.swedsoft.bookkeeping.data.common.SSInvoiceType;
import se.swedsoft.bookkeeping.data.system.SSDB;
import se.swedsoft.bookkeeping.gui.util.SSBundle;
import se.swedsoft.bookkeeping.util.SSDateUtil;

import java.io.IOException;
import java.text.DateFormat;
import java.time.LocalDate;
import java.util.*;
import java.util.Optional;


/**
 * User: Andreas Lago
 * Date: 2006-aug-11
 * Time: 09:10:23
 */
public class SSPeriodicInvoice  {

    private static final long serialVersionUID = 4800991425088361649L;

    private Integer iNumber;
    // Fakturamallen
    private SSInvoice iTemplate;

    // Start Datum
    private LocalDate iDate;
    // Antal fakturor
    private Integer iCount;
    // Perioden i månader
    private Integer iPeriod;
    // Beskrivning
    private String iDescription;

    private LocalDate iPeriodStart;

    private LocalDate iPeriodEnd;

    private boolean iAppendPeriod;

    private boolean iAppendInformation;

    private String iInformation;

    // Fakturor
    private List<SSInvoice> iInvoices;
    // Tillagda fakturor
    private Map<Integer, Boolean> iAdded;

    // ////////////////////////////////////////////////////////////////////////////////

    /**
     *
     */
    public SSPeriodicInvoice() {
        iDate = SSDateUtil.today();
        iCount = 1;
        iPeriod = 1;
        iAppendPeriod = false;
        iAppendInformation = false;
        iInformation = "Detta är faktura [FAK] av [TOT].";
        iPeriodStart = SSDateMath.getFirstDayInMonth(iDate);
        iPeriodEnd = SSDateMath.getLastDayInMonth(iDate);
        iInvoices = new LinkedList<>();
        iAdded = new HashMap<>();
        doAutoIncrecement();
    }

    /**
     *
     * @param iPeriodicInvoice
     */
    public SSPeriodicInvoice(SSPeriodicInvoice iPeriodicInvoice) {
        copyFrom(iPeriodicInvoice);
    }

    /**
     *
     * @param iPeriodicInvoice
     */
    public void copyFrom(SSPeriodicInvoice iPeriodicInvoice) {
        iNumber = iPeriodicInvoice.iNumber;
        iPeriod = iPeriodicInvoice.iPeriod;
        iDate = iPeriodicInvoice.iDate;
        iCount = iPeriodicInvoice.iCount;
        iDescription = iPeriodicInvoice.iDescription;
        iPeriodStart = iPeriodicInvoice.iPeriodStart;
        iPeriodEnd = iPeriodicInvoice.iPeriodEnd;
        iAppendPeriod = iPeriodicInvoice.iAppendPeriod;
        iAppendInformation = iPeriodicInvoice.iAppendInformation;
        iInformation = iPeriodicInvoice.iInformation;
        iTemplate = new SSInvoice(iPeriodicInvoice.iTemplate);
        iInvoices = new LinkedList<>();
        iAdded = new HashMap<>();
        iTemplate.setCurrency(iPeriodicInvoice.getTemplate().getCurrency());
        iTemplate.setCurrencyRate(iPeriodicInvoice.getTemplate().getCurrencyRate());

        for (SSInvoice iInvoice : iPeriodicInvoice.iInvoices) {
            boolean isAdded = iPeriodicInvoice.isAdded(iInvoice);

            iInvoices.add(new SSInvoice(iInvoice));
            iAdded.put(iInvoice.getNumber(), isAdded);
        }
    }

    // //////////////////////////////////////////////////

    /**
     * Auto increment the sales number
     */
    public void doAutoIncrecement() {
        List<SSPeriodicInvoice> iPeriodicInvoices = se.swedsoft.bookkeeping.data.system.SSSalesContext.getPeriodicInvoices();

        int iMax = 0;

        for (SSPeriodicInvoice iPeriodicInvoice: iPeriodicInvoices) {

            if (iPeriodicInvoice.iNumber != null && iPeriodicInvoice.iNumber > iMax) {
                iMax = iPeriodicInvoice.iNumber;
            }
        }
        iNumber = iMax + 1;
    }

    // //////////////////////////////////////////////////

    /**
     *
     * @return
     */
    public Integer getNumber() {
        return iNumber;
    }

    /**
     *
     * @param iNumber
     */
    public void setNumber(Integer iNumber) {
        this.iNumber = iNumber;
    }

    // //////////////////////////////////////////////////

    /**
     *
     * @return
     */
    public SSInvoice getTemplate() {
        if (iTemplate == null) {
            iTemplate = new SSInvoice(SSInvoiceType.NORMAL);
        }

        return iTemplate;
    }

    /**
     *
     * @param iTemplate
     */
    public void setTemplate(SSInvoice iTemplate) {
        this.iTemplate = iTemplate;

        // createInvoices();
    }

    // //////////////////////////////////////////////////

    /**
     * @return the date as a LocalDate
     */
    public LocalDate getLocalDate() {
        return iDate;
    }

    /**
     * @param iDate the date as a LocalDate
     */
    public void setLocalDate(LocalDate iDate) {
        this.iDate = iDate;
    }

    // //////////////////////////////////////////////////

    /**
     *
     * @return
     */
    public Integer getCount() {
        return iCount;
    }

    /**
     *
     * @param iValue
     */
    public void setCount(Integer iValue) {
        iCount = iValue;

        // createInvoices();
    }

    // //////////////////////////////////////////////////

    /**
     *
     * @return
     */
    public Integer getPeriod() {
        return iPeriod;
    }

    /**
     *
     * @param iValue
     */
    public void setPeriod(Integer iValue) {
        iPeriod = iValue;
        // createInvoices();
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
     * @return the period start date as a LocalDate
     */
    public LocalDate getLocalPeriodStart() {
        return iPeriodStart;
    }

    /**
     * @param iPeriodStart the period start date as a LocalDate
     */
    public void setLocalPeriodStart(LocalDate iPeriodStart) {
        this.iPeriodStart = iPeriodStart;
    }

    // //////////////////////////////////////////////////

    /**
     * @return the period end date as a LocalDate
     */
    public LocalDate getLocalPeriodEnd() {
        return iPeriodEnd;
    }

    /**
     * @param iPeriodEnd the period end date as a LocalDate
     */
    public void setLocalPeriodEnd(LocalDate iPeriodEnd) {
        this.iPeriodEnd = iPeriodEnd;
    }

    // //////////////////////////////////////////////////

    /**
     *
     * @return
     */
    public boolean getAppendPeriod() {
        return iAppendPeriod;
    }

    /**
     *
     * @param iAppendPeriod
     */

    public void setAppendPeriod(boolean iAppendPeriod) {
        this.iAppendPeriod = iAppendPeriod;

        // createInvoices();
    }

    public boolean isAppendInformation() {
        return iAppendInformation;
    }

    public void setAppendInformation(boolean iAppendInformation) {
        this.iAppendInformation = iAppendInformation;
    }

    public String getInformation() {
        return iInformation;
    }

    public void setInformation(String iInformation) {
        this.iInformation = iInformation;
    }

    // //////////////////////////////////////////////////

    /**
     *
     * @param iInvoice
     * @return
     */
    public boolean isAdded(SSInvoice iInvoice) {
        if (iAdded == null) {
            iAdded = new HashMap<>();
        }

        Integer iNumber = iInvoice.getNumber();

        if (iNumber != null) {
            Boolean added = iAdded.get(iNumber);
            return added != null && added;
        } else {
            return true;
        }
    }

    /**
     *
     * @param iInvoice
     */
    public void setAdded(SSInvoice iInvoice) {
        if (iAdded == null) {
            iAdded = new HashMap<>();
        }

        Integer iNumber = iInvoice.getNumber();

        if (iNumber != null) {
            iAdded.put(iNumber, true);
        }
    }

    /**
     *
     * @param iInvoice
     */
    public void setNotAdded(SSInvoice iInvoice) {
        if (iAdded == null) {
            return;
        }

        Integer iNumber = iInvoice.getNumber();

        if (iNumber != null) {
            iAdded.put(iNumber, false);
        }
    }

    /**
     *
     * @return
     */
    private Map<Integer, Boolean> getAdded() {
        if (iAdded == null) {
            iAdded = new HashMap<>();
        }
        return iAdded;
    }

    // //////////////////////////////////////////////////

    /**
     *
     * @return
     */
    public List<SSInvoice> getInvoices() {
        if (iInvoices == null) {
            iInvoices = new LinkedList<>();
        }

        return iInvoices;
    }

    /**
     * Returns all invoices that isnt added up till the selected date
     *
     * @param iDate
     * @return
     */
    public List<SSInvoice> getInvoices(LocalDate iDate) {
        List<SSInvoice> iFiltered = new LinkedList<>();

        for (SSInvoice iInvoice : getInvoices()) {

            // Skip the invoice if its already added
            if (isAdded(iInvoice)) {
                continue;
            }

            // The invoice date is before the date
            if (SSInvoiceMath.inPeriod(iInvoice, iDate)) {
                iFiltered.add(iInvoice);
            }
        }
        return iFiltered;
    }

    /**
     * Returns the date of the next invoice
     *
     * @return the date
     */
    public Optional<LocalDate> getNextLocalDate() {

        for (SSInvoice iInvoice : getInvoices()) {
            // Skip the invoice if its already added
            if (isAdded(iInvoice)) {
                continue;
            }

            return Optional.ofNullable(iInvoice.getLocalDate());
        }
        return Optional.empty();
    }

    /**
     *
     */
    public void createInvoices() {
        iInvoices = new LinkedList<>();

        if (iTemplate == null) {
            return;
        }

        // Normalize legacy/null values so generated invoices can still be built.
        if (iCount == null || iCount <= 0) {
            iCount = 1;
        }
        if (iPeriod == null || iPeriod <= 0) {
            iPeriod = 1;
        }
        if (iDate == null) {
            iDate = SSDateUtil.today();
        }
        if (iPeriodStart == null) {
            iPeriodStart = SSDateMath.getFirstDayInMonth(iDate);
        }
        if (iPeriodEnd == null) {
            iPeriodEnd = SSDateMath.addMonths(iPeriodStart, iPeriod).minusDays(1);
        }

        LocalDate iDate = this.iDate;
        LocalDate iPeriodStart = this.iPeriodStart;
        LocalDate iPeriodEnd = this.iPeriodEnd;

        DateFormat iFormat = DateFormat.getDateInstance(DateFormat.SHORT);

        for (int i = 0; i < iCount; i++) {
            SSInvoice iInvoice = new SSInvoice(iTemplate);

            iInvoice.setLocalDate(iDate);
            iInvoice.setDueDate();
            iInvoice.setNumber(i);
            iInvoice.setOrderNumbers(iTemplate.getOrderNumbers());

            if (iAppendPeriod) {
                String strPeriodStart = iFormat.format(SSDateUtil.toDate(iPeriodStart));
                String strPeriodEnd = iFormat.format(SSDateUtil.toDate(iPeriodEnd));

                SSSaleRow iRow = new SSSaleRow();

                iRow.setDescription(
                        String.format(
                                SSBundle.getBundle().getString(
                                        "periodicinvoiceframe.invoiceperiod"),
                                        strPeriodStart,
                                        strPeriodEnd));
                iRow.setQuantity(null);
                iRow.setUnitprice(null);
                iRow.setTaxCode(null);

                iInvoice.getRows().add(iRow);
            }

            if (iAppendInformation) {
                String iInformationText = iInformation;

                if (iInformationText.contains("[FAK]")) {
                    iInformationText = iInformationText.replace("[FAK]",
                            String.valueOf(i + 1));
                }
                if (iInformationText.contains("[TOT]")) {
                    iInformationText = iInformationText.replace("[TOT]",
                            String.valueOf(iCount));
                }

                SSSaleRow iRow = new SSSaleRow();

                iRow.setDescription(iInformationText);
                iRow.setQuantity(null);
                iRow.setUnitprice(null);
                iRow.setTaxCode(null);
                iInvoice.getRows().add(iRow);
            }

            iDate = SSDateMath.addMonths(iDate, iPeriod);
            iPeriodStart = SSDateMath.addMonths(iPeriodStart, iPeriod);
            iPeriodEnd = SSDateMath.addMonths(iPeriodStart, iPeriod).minusDays(1);
            iInvoices.add(iInvoice);
        }

        if (iAdded == null || iAdded.size() != iInvoices.size()) {
            iAdded = new HashMap<>();
            for (SSInvoice iInvoice : iInvoices) {
                Integer iNumber = iInvoice.getNumber();

                iAdded.put(iNumber, false);
            }
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof SSPeriodicInvoice)) {
            return false;
        }
        SSPeriodicInvoice other = (SSPeriodicInvoice) obj;
        return Objects.equals(iNumber, other.iNumber);
    }

    @Override
    public int hashCode() {
        return Objects.hash(iNumber);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();

        sb.append("se.swedsoft.bookkeeping.data.SSPeriodicInvoice");
        sb.append("{iAdded=").append(iAdded);
        sb.append(", iAppendInformation=").append(iAppendInformation);
        sb.append(", iAppendPeriod=").append(iAppendPeriod);
        sb.append(", iCount=").append(iCount);
        sb.append(", iDate=").append(iDate);
        sb.append(", iDescription='").append(iDescription).append('\'');
        sb.append(", iInformation='").append(iInformation).append('\'');
        sb.append(", iInvoices=").append(iInvoices);
        sb.append(", iNumber=").append(iNumber);
        sb.append(", iPeriod=").append(iPeriod);
        sb.append(", iPeriodEnd=").append(iPeriodEnd);
        sb.append(", iPeriodStart=").append(iPeriodStart);
        sb.append(", iTemplate=").append(iTemplate);
        sb.append('}');
        return sb.toString();
    }

}
