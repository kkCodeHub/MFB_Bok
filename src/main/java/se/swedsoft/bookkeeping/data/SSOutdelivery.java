package se.swedsoft.bookkeeping.data;


import se.swedsoft.bookkeeping.persistence.Repositories;
import se.swedsoft.bookkeeping.util.SSDateUtil;

import java.time.LocalDate;
import java.util.LinkedList;
import java.util.List;


/**
 * User: Andreas Lago
 * Date: 2006-sep-25
 * Time: 09:31:58
 */
public class SSOutdelivery  {

    private static final long serialVersionUID = -5537699296769492741L;

    private Integer iNumber;

    private LocalDate iDate;

    private String iText;

    private List<SSOutdeliveryRow> iRows;

    /**
     *
     */
    public SSOutdelivery() {
        this(true);
    }

    /**
     * Constructor that optionally skips auto-increment; use {@code false} when
     * mapping rows from the database to avoid recursive repository calls.
     *
     * @param autoIncrement whether to call {@link #doAutoIncrement()}
     */
    public SSOutdelivery(boolean autoIncrement) {
        iDate = SSDateUtil.today();
        iText = null;
        iRows = new LinkedList<>();
        if (autoIncrement) {
            doAutoIncrement();
        }
    }

    /**
     * Copy constructor
     *
     * @param iOutdelivery
     */
    public SSOutdelivery(SSOutdelivery iOutdelivery) {
        copyFrom(iOutdelivery);
    }

    // /////////////////////////////////////////////////////////////////////////////////////

    /**
     *
     */
    public void doAutoIncrement() {
        iNumber = 1;

        List<SSOutdelivery> iOutdeliveries = Repositories.outdeliveries().findAll();

        for (SSOutdelivery iOutdelivery : iOutdeliveries) {
            if (iOutdelivery.iNumber >= iNumber) {
                iNumber = iOutdelivery.iNumber + 1;
            }
        }

    }

    /**
     *
     * @param iOutdelivery
     */
    public void copyFrom(SSOutdelivery iOutdelivery) {
        iNumber = iOutdelivery.iNumber;
        iDate = iOutdelivery.iDate;
        iText = iOutdelivery.iText;
        iRows = new LinkedList<>();

        for (SSOutdeliveryRow iRow : iOutdelivery.iRows) {
            iRows.add(new SSOutdeliveryRow(iRow));
        }
    }

    // /////////////////////////////////////////////////////////////////////////////////////

    /**
     *
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

    // /////////////////////////////////////////////////////////////////////////////////////

    public LocalDate getLocalDate() {
        return iDate;
    }

    public void setLocalDate(LocalDate iDate) {
        this.iDate = iDate;
    }

    // /////////////////////////////////////////////////////////////////////////////////////

    /**
     *
     */
    public String getText() {
        return iText;
    }

    /**
     *
     * @param iText
     */
    public void setText(String iText) {
        this.iText = iText;
    }

    // /////////////////////////////////////////////////////////////////////////////////////

    /**
     *
     */
    public List<SSOutdeliveryRow> getRows() {
        return iRows;
    }

    /**
     *
     * @param iRows
     */
    public void setRows(List<SSOutdeliveryRow> iRows) {
        this.iRows = iRows;
    }

    // /////////////////////////////////////////////////////////////////////////////////////

    /**
     * Return the correction for the supplied product.
     *
     * @param iProduct
     * @return the correction
     */
    public Integer getChange(SSProduct iProduct) {
        Integer iSum = 0;

        for (SSOutdeliveryRow iRow : iRows) {
            if (iRow.hasProduct(iProduct)) {
                Integer iChange = iRow.getChange();

                if (iChange != null) {
                    iSum = iSum + iChange;
                }
            }

        }

        return iSum;

    }

    public boolean equals(Object obj) {
        if (!(obj instanceof SSOutdelivery)) {
            return false;
        }
        return iNumber.equals(((SSOutdelivery) obj).iNumber);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();

        sb.append("se.swedsoft.bookkeeping.data.SSOutdelivery");
        sb.append("{iDate=").append(iDate);
        sb.append(", iNumber=").append(iNumber);
        sb.append(", iRows=").append(iRows);
        sb.append(", iText='").append(iText).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
