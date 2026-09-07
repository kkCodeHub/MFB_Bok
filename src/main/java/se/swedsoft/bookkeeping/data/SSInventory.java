package se.swedsoft.bookkeeping.data;


import se.swedsoft.bookkeeping.persistence.Repositories;
import se.swedsoft.bookkeeping.util.SSDateUtil;

import java.time.LocalDate;
import java.util.LinkedList;
import java.util.List;


/**
 * User: Andreas Lago
 * Date: 2006-sep-18
 * Time: 14:50:29
 */
public class SSInventory {

    private Integer iNumber;

    private LocalDate iDate;

    private String iText;

    private List<SSInventoryRow> iRows;

    /**
     *
     */
    public SSInventory() {
        this(true);
    }

    /**
     * Creates an inventory and optionally performs automatic numbering.
     *
     * @param autoIncrement true to call {@link #doAutoIncrement()}, false for data-mapping scenarios
     */
    public SSInventory(boolean autoIncrement) {
        iNumber = 0;
        iDate = SSDateUtil.today();
        iText = "";
        iRows = new LinkedList<>();

        if (autoIncrement) {
            doAutoIncrement();
        }
    }

    /**
     * Copy constructor
     *
     * @param iInventory
     */
    public SSInventory(SSInventory iInventory) {
        copyFrom(iInventory);
    }

    // /////////////////////////////////////////////////////////////////////////////////////

    /**
     */
    public void doAutoIncrement() {
        iNumber = 1;

        List<SSInventory> iInventories = Repositories.inventories().findAll();

        for (SSInventory iInventory : iInventories) {
            if (iInventory.iNumber >= iNumber) {
                iNumber = iInventory.iNumber + 1;
            }
        }
    }

    // /////////////////////////////////////////////////////////////////////////////////////

    /**
     *
     * @param iInventory
     */
    public void copyFrom(SSInventory iInventory) {
        iNumber = iInventory.iNumber;
        iDate = iInventory.iDate;
        iText = iInventory.iText;
        iRows = new LinkedList<>();

        for (SSInventoryRow iRow : iInventory.iRows) {
            iRows.add(new SSInventoryRow(iRow));
        }
    }

    // /////////////////////////////////////////////////////////////////////////////////////
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

    // /////////////////////////////////////////////////////////////////////////////////////

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

    // /////////////////////////////////////////////////////////////////////////////////////

    /**
     *
     * @return
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
     * @return
     */
    public List<SSInventoryRow> getRows() {
        return iRows;
    }

    /**
     *
     * @param iRows
     */
    public void setRows(List<SSInventoryRow> iRows) {
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

        for (SSInventoryRow iRow : iRows) {
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
        if (!(obj instanceof SSInventory)) {
            return false;
        }
        return iNumber.equals(((SSInventory) obj).iNumber);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();

        sb.append("se.swedsoft.bookkeeping.data.SSInventory");
        sb.append("{iDate=").append(iDate);
        sb.append(", iNumber=").append(iNumber);
        sb.append(", iRows=").append(iRows);
        sb.append(", iText='").append(iText).append('\'');
        sb.append('}');
        return sb.toString();
    }

}
