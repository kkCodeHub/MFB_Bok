/*
 * 2005-2010
 * $Id$
 */
package se.swedsoft.bookkeeping.data;


import se.swedsoft.bookkeeping.data.system.SSDB;
import se.swedsoft.bookkeeping.gui.SSMainFrame;
import se.swedsoft.bookkeeping.gui.util.SSBundle;
import se.swedsoft.bookkeeping.gui.util.table.SSTableSearchable;
import se.swedsoft.bookkeeping.util.SSDateUtil;

import javax.swing.*;
import java.io.IOException;
import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.*;


/**
 * V2 target model for accounting years.
 *
 * <p>This is the supported accounting-year representation in active V2 code paths.</p>
 */
public class SSNewAccountingYear implements Serializable, SSTableSearchable {

    // / Constant for serialization versioning.
    @Serial
    private static final long serialVersionUID = 1L;

    private Integer iId;

    private LocalDate iFrom;

    private LocalDate iTo;

    private SSAccountPlan iPlan;

    private Map<SSAccount, BigDecimal> iInBalance;

    private SSBudget iBudget;

    // Hybrid Light: snapshot metadata for account plan (Release 1)
    private Integer iAccountPlanSchemaVersion;
    private String iAccountPlanCompressionFlag;
    private String iAccountPlanChecksum;
    private Integer iAccountPlanSnapshotVersion;
    private java.time.Instant iAccountPlanUpdatedAt;
    private String iAccountPlanUpdatedBy;
    private String iAccountPlanName;

    // Dirty-flag for active account plan (tracks unsaved changes)
    private transient boolean iAccountPlanDirtyFlag;

    /**
     * Default constructor.
     */
    public SSNewAccountingYear() {
        iId = 0;
        iFrom = SSDateUtil.today();
        iTo = SSDateUtil.today();
        iInBalance = new HashMap<>();
        iBudget = new SSBudget();
        // Initialize snapshot metadata
        iAccountPlanSchemaVersion = 1;
        iAccountPlanCompressionFlag = "gzip";
        iAccountPlanSnapshotVersion = 0;
        iAccountPlanDirtyFlag = false;
    }

    /**
     *
     * @param pAccountingYear
     */
    public SSNewAccountingYear(SSNewAccountingYear pAccountingYear) {
        this();
        setData(pAccountingYear);
    }


    /**
     * Sets the data of the accountingyear to the same as the parameter
     *
     * Note that the data aren't copied
     *
     * @param pAccountingYear
     */
    public void setData(SSNewAccountingYear pAccountingYear) {
        iId = pAccountingYear.iId;
        iFrom = pAccountingYear.iFrom;
        iTo = pAccountingYear.iTo;
        iInBalance = pAccountingYear.iInBalance;
        iBudget = pAccountingYear.iBudget;
        iPlan = pAccountingYear.iPlan;
        iAccountPlanSchemaVersion = pAccountingYear.iAccountPlanSchemaVersion;
        iAccountPlanCompressionFlag = pAccountingYear.iAccountPlanCompressionFlag;
        iAccountPlanChecksum = pAccountingYear.iAccountPlanChecksum;
        iAccountPlanSnapshotVersion = pAccountingYear.iAccountPlanSnapshotVersion;
        iAccountPlanUpdatedAt = pAccountingYear.iAccountPlanUpdatedAt;
        iAccountPlanUpdatedBy = pAccountingYear.iAccountPlanUpdatedBy;
        iAccountPlanName = pAccountingYear.iAccountPlanName;
        iAccountPlanDirtyFlag = pAccountingYear.iAccountPlanDirtyFlag;
    }

    /**
     *
     * @return the id
     */
    public Integer getId() {
        return iId;
    }

    public void setId(Integer pId) {
        iId = pId;
    }


    /**
     * @return the from date as a LocalDate
     */
    public LocalDate getLocalFrom() {
        return iFrom;
    }

    /**
     * @param pFrom the from date as a LocalDate
     */
    public void setLocalFrom(LocalDate pFrom) {
        iFrom = pFrom;
    }


    /**
     * @return the to date as a LocalDate
     */
    public LocalDate getLocalTo() {
        return iTo;
    }

    /**
     * @param pTo the to date as a LocalDate
     */
    public void setLocalTo(LocalDate pTo) {
        iTo = pTo;
    }

    /**
     *
     * @return the account plan
     */
    public SSAccountPlan getAccountPlan() {
        if (iPlan == null) {
            iPlan = new SSAccountPlan();
        }

        return iPlan;
    }

    /**
     *
     * @param pAccountPlan
     */
    public void setAccountPlan(SSAccountPlan pAccountPlan) {
        iPlan = pAccountPlan;
    }

    /**
     *
     * @return the budget for the year
     */
    public SSBudget getBudget() {
        // Make shure the budget know that we are the owning year
        iBudget.setYear(this);

        return iBudget;
    }

    /**
     *
     * @param iBudget
     */
    public void setBudget(SSBudget iBudget) {
        this.iBudget = iBudget == null ? new SSBudget() : iBudget;
    }

    /**
     *
     * @return the in balance
     */
    public Map<SSAccount, BigDecimal> getInBalance() {
        return iInBalance;
    }

    /**
     *
     * @param pInBalance
     */
    public void setInBalance(Map<SSAccount, BigDecimal> pInBalance) {
        iInBalance = pInBalance == null ? new HashMap<>() : pInBalance;
    }

    /**
     * Returns the vouchers for the year
     *
     * @return the vouchers
     */
    public List<SSVoucher> getVouchers() {
        return se.swedsoft.bookkeeping.data.system.SSAccountingContext.getVouchers(this);
    }

    /**
     * Returns the accounts in the current acccountplan.
     *
     * @return A List of the current accounts or null.
     */
    public List<SSAccount> getAccounts() {
        if (iPlan != null) {
            return iPlan.getAccounts();
        }
        return Collections.emptyList();
    }

    /**
     * Returns the active accounts in the current acccountplan.
     *
     * @return A List of the active accounts or null.
     */
    public List<SSAccount> getActiveAccounts() {
        if (iPlan != null) {
            return iPlan.getActiveAccounts();
        }
        return Collections.emptyList();
    }

    /**
     * Returns the render string to be shown in the tables
     *
     * @return The searchable string
     */
    public String toRenderString() {
        DateTimeFormatter fmt = DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT);

        return iFrom.format(fmt) + " - " + iTo.format(fmt);
    }

    public String toString() {
        DateTimeFormatter fmt = DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT);

        StringBuilder sb = new StringBuilder();

        sb.append(iFrom.format(fmt));
        sb.append(' ');
        sb.append(SSBundle.getBundle().getString("date.separator"));
        sb.append(' ');
        sb.append(iTo.format(fmt));

        return sb.toString();
    }

    /**
     *
     * @param pAccount
     * @param pAmount
     */
    public void setInBalance(SSAccount pAccount, BigDecimal pAmount) {
        if (iInBalance == null) {
            iInBalance = new HashMap<>();
        }
        iInBalance.put(pAccount, pAmount);
    }

    /**
     *
     * @param pAccount
     *
     * @return
     */
    public BigDecimal getInBalance(SSAccount pAccount) {
        if (iInBalance == null) {
            iInBalance = new HashMap<>();
        }
        BigDecimal amount = iInBalance.get(pAccount);

        if (amount == null) {
            amount = new BigDecimal(0);
        }
        return amount;
    }

    /**
     * Snapshot metadata: schema version for account plan storage.
     */
    public Integer getAccountPlanSchemaVersion() {
        return iAccountPlanSchemaVersion;
    }

    public void setAccountPlanSchemaVersion(Integer pVersion) {
        iAccountPlanSchemaVersion = pVersion;
    }

    /**
     * Snapshot metadata: compression flag ("gzip" or "none").
     */
    public String getAccountPlanCompressionFlag() {
        return iAccountPlanCompressionFlag;
    }

    public void setAccountPlanCompressionFlag(String pFlag) {
        iAccountPlanCompressionFlag = pFlag;
    }

    /**
     * Snapshot metadata: SHA-256 checksum of compressed data.
     */
    public String getAccountPlanChecksum() {
        return iAccountPlanChecksum;
    }

    public void setAccountPlanChecksum(String pChecksum) {
        iAccountPlanChecksum = pChecksum;
    }

    /**
     * Snapshot metadata: incremented on each write.
     */
    public Integer getAccountPlanSnapshotVersion() {
        return iAccountPlanSnapshotVersion;
    }

    public void setAccountPlanSnapshotVersion(Integer pVersion) {
        iAccountPlanSnapshotVersion = pVersion;
    }

    /**
     * Snapshot metadata: timestamp of last update.
     */
    public java.time.Instant getAccountPlanUpdatedAt() {
        return iAccountPlanUpdatedAt;
    }

    public void setAccountPlanUpdatedAt(java.time.Instant pTimestamp) {
        iAccountPlanUpdatedAt = pTimestamp;
    }

    /**
     * Snapshot metadata: user who last updated the plan.
     */
    public String getAccountPlanUpdatedBy() {
        return iAccountPlanUpdatedBy;
    }

    public void setAccountPlanUpdatedBy(String pUserId) {
        iAccountPlanUpdatedBy = pUserId;
    }

    /**
     * Snapshot metadata: name of the account plan.
     */
    public String getAccountPlanName() {
        return iAccountPlanName;
    }

    public void setAccountPlanName(String pName) {
        iAccountPlanName = pName;
    }

    /**
     * Dirty flag: indicates whether the active account plan has unsaved changes.
     * This is transient and not persisted (resets on load).
     */
    public boolean isAccountPlanDirty() {
        return iAccountPlanDirtyFlag;
    }

    public void setAccountPlanDirty(boolean pDirty) {
        iAccountPlanDirtyFlag = pDirty;
    }

    /**
     *
     * @param iMainFrame The main frame
     */
    public static void openWarningDialogNoYearData(SSMainFrame iMainFrame) {
        String message = SSBundle.getBundle().getString("accountingYear.no.year.message");
        String title = SSBundle.getBundle().getString("accountingYear.no.year.title");

        JOptionPane.showMessageDialog(iMainFrame, message, title,
                JOptionPane.INFORMATION_MESSAGE);
    }

    // //////////////////////////////////////////////////////////////////
    /*
     public static interface SSNewAccountingYearListener{
     public void yearLoaded(SSNewCompany iCompany, SSNewAccountingYear iAccountingYear);
     }

     private static List<SSNewAccountingYearListener> iListeners = new LinkedList<>();

     public void addListener(SSNewAccountingYearListener iListener){
     iListeners.add(iListener);
     }

     private void notifyListeners(SSNewCompany iCompany, SSNewAccountingYear iAccountingYear){
     for(SSNewAccountingYearListener iListener: iListeners){
     iListener.yearLoaded(iCompany, iAccountingYear);
     }
     }  */

    public boolean equals(Object obj) {
        if (!(obj instanceof SSNewAccountingYear)) {
            return false;
        }
        return iId.equals(((SSNewAccountingYear) obj).iId);
    }

}
