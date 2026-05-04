/*
 * 2005-2010
 * $Id$
 */
package se.swedsoft.bookkeeping.data;


import se.swedsoft.bookkeeping.gui.SSMainFrame;
import se.swedsoft.bookkeeping.gui.util.SSBundle;
import se.swedsoft.bookkeeping.gui.util.table.SSTableSearchable;
import se.swedsoft.bookkeeping.util.SSDateUtil;

import javax.swing.*;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.math.BigDecimal;
import java.rmi.server.UID;
import java.text.DateFormat;
import java.time.LocalDate;
import java.util.*;


/**
 */
public class SSAccountingYear implements Serializable, SSTableSearchable {

    // / Constant for serialization versioning.
    static final long serialVersionUID = 1L;

    private UID iId;

    private LocalDate iFrom;

    private LocalDate iTo;

    private SSAccountPlan iPlan;

    private Map<SSAccount, BigDecimal> iInBalance;

    private List<SSVoucher> iVouchers;

    private SSBudget iBudget;

    /**
     * Default constructor.
     */
    public SSAccountingYear() {
        iId = new UID();
        iFrom = SSDateUtil.today();
        iTo = SSDateUtil.today();
        iInBalance = new HashMap<>();
        iVouchers = new LinkedList<>();
        iBudget = new SSBudget();
    }

    /**
     *
     * @param pFrom
     * @param pTo
     */
    public SSAccountingYear(Date pFrom, Date pTo) {
        this();
        iFrom = SSDateUtil.toLocalDate(pFrom);
        iTo = SSDateUtil.toLocalDate(pTo);
    }

    /**
     *
     * @param pAccountingYear
     */
    public SSAccountingYear(SSAccountingYear pAccountingYear) {
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
    public void setData(SSAccountingYear pAccountingYear) {
        iId = pAccountingYear.iId;
        iFrom = pAccountingYear.iFrom;
        iTo = pAccountingYear.iTo;
        iInBalance = pAccountingYear.iInBalance;
        iVouchers = pAccountingYear.iVouchers;
        iBudget = pAccountingYear.iBudget;
        iPlan = pAccountingYear.iPlan;
    }

    /**
     *
     * @return the id
     */
    public UID getId() {
        return iId;
    }

    /**
     *
     * @return the from date
     */
    @Deprecated
    public Date getFrom() {
        return SSDateUtil.toDate(iFrom);
    }

    /**
     *
     * @param pFrom
     */
    @Deprecated
    public void setFrom(Date pFrom) {
        iFrom = SSDateUtil.toLocalDate(pFrom);
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
     *
     * @return the todate
     */
    @Deprecated
    public Date getTo() {
        return SSDateUtil.toDate(iTo);
    }

    /**
     *
     * @param pTo
     */
    @Deprecated
    public void setTo(Date pTo) {
        iTo = SSDateUtil.toLocalDate(pTo);
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
        // iBudget.setYear( this );

        return iBudget;
    }

    /**
     *
     * @param iBudget
     */
    public void setBudget(SSBudget iBudget) {
        this.iBudget = iBudget;
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
        iInBalance = pInBalance;
    }

    /**
     * Returns the vouchers for the year
     *
     * @return the vouchers
     */
    public List<SSVoucher> getVouchers() {
        return iVouchers;
    }

    /**
     * Set the vouchers for the year
     *
     * @param pVouchers the vouchers
     */
    public void setVouchers(List<SSVoucher> pVouchers) {
        iVouchers = pVouchers;
    }

    public void updateVoucher(SSVoucher pVoucher) {
        for (int i = 0; i < iVouchers.size(); i++) {
            SSVoucher iVoucher = iVouchers.get(i);

            if (iVoucher.getNumber() == pVoucher.getNumber()) {
                iVouchers.remove(i);
                iVouchers.add(i, pVoucher);
            }
        }
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
        DateFormat iFormat = DateFormat.getDateInstance(DateFormat.SHORT);

        return iFormat.format(SSDateUtil.toDate(iFrom)) + " - " + iFormat.format(SSDateUtil.toDate(iTo));
    }

    public String toString() {
        DateFormat iFormat = DateFormat.getDateInstance(DateFormat.SHORT);

        StringBuilder sb = new StringBuilder();

        sb.append(iFormat.format(SSDateUtil.toDate(iFrom)));
        sb.append(' ');
        sb.append(SSBundle.getBundle().getString("date.separator"));
        sb.append(' ');
        sb.append(iFormat.format(SSDateUtil.toDate(iTo)));

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
     * Custom deserialization that handles both old (Date) and new (LocalDate) field formats.
     *
     * <p>Pre-migration serialized streams stored {@code iFrom} and {@code iTo} as
     * {@code java.util.Date}.  This method reads them as raw objects and converts
     * via {@link SSDateUtil#readLocalDate(Object)}.
     */
    @SuppressWarnings("unchecked")
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        ObjectInputStream.GetField fields = in.readFields();
        iId = (UID) fields.get("iId", null);
        iFrom = SSDateUtil.readLocalDate(fields.get("iFrom", null));
        iTo = SSDateUtil.readLocalDate(fields.get("iTo", null));
        iPlan = (SSAccountPlan) fields.get("iPlan", null);
        iInBalance = (Map<SSAccount, BigDecimal>) fields.get("iInBalance", null);
        iVouchers = (List<SSVoucher>) fields.get("iVouchers", null);
        iBudget = (SSBudget) fields.get("iBudget", null);
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
     public static interface SSAccountingYearListener{
     public void yearLoaded(SSCompany iCompany, SSAccountingYear iAccountingYear);
     }

     private static List<SSAccountingYearListener> iListeners = new LinkedList<>();

     public void addListener(SSAccountingYearListener iListener){
     iListeners.add(iListener);
     }

     private void notifyListeners(SSCompany iCompany, SSAccountingYear iAccountingYear){
     for(SSAccountingYearListener iListener: iListeners){
     iListener.yearLoaded(iCompany, iAccountingYear);
     }
     }  */

}
