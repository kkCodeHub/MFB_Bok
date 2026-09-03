package se.swedsoft.bookkeeping.data;


import se.swedsoft.bookkeeping.calc.math.SSAccountMath;
import se.swedsoft.bookkeeping.util.SSDateUtil;

import java.io.IOException;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DateFormat;
import java.time.LocalDate;
import java.util.*;
import java.util.Optional;


/**
 * User: Fredrik Stigsson
 * Date: 2006-jan-27
 * Time: 10:58:42
 */
public class SSBudget implements Serializable {

    /**
     * Constant for serialization versioning.
     */
    static final long serialVersionUID = 1L;

    private transient SSNewAccountingYear iAccountingYear;

    private Date iFrom;

    private Date iTo;

    private Map<SSMonth, Map<SSAccount, BigDecimal>> iBudget;

    /**
     * Default constructor
     */
    public SSBudget() {
        iFrom = SSDateUtil.toDate(SSDateUtil.today());
        iTo = SSDateUtil.toDate(SSDateUtil.today());
        iAccountingYear = null;
        iBudget = new HashMap<>();
    }

    /**
     * Copy constructor
     * @param pSource
     */
    public SSBudget(SSBudget pSource) {
        iFrom = pSource.iFrom;
        iTo = pSource.iTo;
        iAccountingYear = pSource.iAccountingYear;
        iBudget = new HashMap<>();

        Map<SSMonth, Map<SSAccount, BigDecimal>> iSource = pSource.getBudget();

        for (Map.Entry<SSMonth, Map<SSAccount, BigDecimal>> ssMonthMapEntry : iSource.entrySet()) {
            Map<SSAccount, BigDecimal> iMonthlyBudget = new HashMap<>();

            iMonthlyBudget.putAll(ssMonthMapEntry.getValue());

            iBudget.put(ssMonthMapEntry.getKey(), iMonthlyBudget);
        }

    }

    /**
     *
     */
    public void clear() {
        iBudget = createBudgetForYear();
    }

    /**
     * @return the from date as a {@link LocalDate}
     */
    public LocalDate getLocalFrom() {
        return SSDateUtil.toLocalDate(iFrom);
    }

    /**
     * @return the to date as a {@link LocalDate}
     */
    public LocalDate getLocalTo() {
        return SSDateUtil.toLocalDate(iTo);
    }

    /**
     * Returns the accounting year for this budget
     *
     * @return the accounting year
     */
    public SSNewAccountingYear getAccountingYear() {
        return iAccountingYear;
    }

    /**
     * @return The accounts
     */
    public List<SSAccount> getAccounts() {
        if (iAccountingYear != null) {
            return SSAccountMath.getResultAccounts(iAccountingYear);
        }
        return Collections.emptyList();
    }

    /**
     * @return The months
     */
    public List<SSMonth> getMonths() {
        if (iBudget == null) {
            iBudget = createBudgetForYear();
        }
        List<SSMonth> iMonths = new LinkedList<>();

        for (SSMonth iMonth: iBudget.keySet()) {
            iMonths.add(iMonth);
        }

        Collections.sort(iMonths, (o1, o2) -> o1.getLocalFrom().compareTo(o2.getLocalFrom()));

        return iMonths;
    }

    /**
     * @param pMonth
     * @return The months
     */
    public Optional<SSMonth> getMonth(SSMonth pMonth) {
        for (SSMonth iMonth: iBudget.keySet()) {
            if (iMonth.equals(pMonth)) {
                return Optional.of(iMonth);
            }
        }
        return Optional.empty();
    }

    /**
     * Sets the current year, if the from and to dates differes from the internal current the montly distribution
     * will be lost.
     *
     * @param pAccountingYear The year
     */
    public void setYear(SSNewAccountingYear pAccountingYear) {
        iAccountingYear = pAccountingYear;

        if (!iAccountingYear.getLocalFrom().equals(SSDateUtil.toLocalDate(iFrom))
                || !iAccountingYear.getLocalTo().equals(SSDateUtil.toLocalDate(iTo))) {
            Map<Integer, Map<SSAccount, BigDecimal>> iExistingByMonthNumber = snapshotBudgetByMonthNumber();

            iFrom = SSDateUtil.toDate(iAccountingYear.getLocalFrom());
            iTo = SSDateUtil.toDate(iAccountingYear.getLocalTo());

            iBudget = createEmptyBudgetForYear();
            restoreBudgetByMonthNumber(iExistingByMonthNumber);
        }
    }

    private Map<Integer, Map<SSAccount, BigDecimal>> snapshotBudgetByMonthNumber() {
        Map<Integer, Map<SSAccount, BigDecimal>> iSnapshot = new HashMap<>();
        if (iBudget == null) {
            return iSnapshot;
        }

        for (Map.Entry<SSMonth, Map<SSAccount, BigDecimal>> iEntry : iBudget.entrySet()) {
            if (iEntry.getKey() == null || iEntry.getKey().getLocalFrom() == null || iEntry.getValue() == null) {
                continue;
            }

            int iMonthNumber = iEntry.getKey().getLocalFrom().getMonthValue();
            Map<SSAccount, BigDecimal> iTarget = iSnapshot.computeIfAbsent(iMonthNumber, k -> new HashMap<>());
            iTarget.putAll(iEntry.getValue());
        }
        return iSnapshot;
    }

    private void restoreBudgetByMonthNumber(Map<Integer, Map<SSAccount, BigDecimal>> iSnapshot) {
        if (iSnapshot == null || iSnapshot.isEmpty() || iBudget == null) {
            return;
        }

        for (SSMonth iMonth : getMonths()) {
            if (iMonth == null || iMonth.getLocalFrom() == null) {
                continue;
            }

            Map<SSAccount, BigDecimal> iSavedMonth = iSnapshot.get(iMonth.getLocalFrom().getMonthValue());
            if (iSavedMonth == null || iSavedMonth.isEmpty()) {
                continue;
            }

            Map<SSAccount, BigDecimal> iCurrentMonth = iBudget.get(iMonth);
            if (iCurrentMonth != null) {
                iCurrentMonth.putAll(iSavedMonth);
            }
        }
    }

    /**
     *
     * @return
     */
    public Map<SSMonth, Map<SSAccount, BigDecimal>> getBudget() {
        if (iBudget == null) {
            iBudget = createBudgetForYear();
        }
        return iBudget;
    }

    /**
     *
     * @param pMonth
     * @return the budget for a month
     */
    public Map<SSAccount, BigDecimal> getBudget(SSMonth pMonth) {
        if (iBudget == null) {
            iBudget = createBudgetForYear();
        }
        return iBudget.get(pMonth);
    }

    /**
     * Sets the budget sum for an account. This will be spread over the year
     *
     * @param pAccount The account to set the sum to.
     * @param pValue The value
     */
    public void setSumForAccount(SSAccount pAccount, BigDecimal pValue) {
        List<SSMonth> iMonths = getMonths();

        if (pValue == null || pValue.signum() == 0) {

            // Delete the value for each month
            for (SSMonth iMonth : iMonths) {
                iBudget.get(iMonth).remove(pAccount);
            }
            return;
        }

        // If we have no months we cannot set any sum
        if (iMonths.isEmpty()) {
            return;
        }
         // Get the number of months as a bigdecimal for our calculations
         BigDecimal numMonths = new BigDecimal(iMonths.size());

         // Make shure we have 2 decimals for the sum, else the accuracy of the divission will be of
         pValue = pValue.setScale(2, RoundingMode.HALF_UP);
         // Get the sum to be added per month
         BigDecimal sumPerMonth = pValue.divide(numMonths, 2, RoundingMode.FLOOR);
        // Get the last few ören that differs from the total sum
        BigDecimal remainder = pValue.subtract(sumPerMonth.multiply(numMonths));

        // Set the value for each month to the wanted one
        for (SSMonth iMonth : iMonths) {
            iBudget.get(iMonth).put(pAccount, sumPerMonth);
        }
        // Add the remainder to the last month
        addValueToMonth(iMonths.get(iMonths.size() - 1), pAccount, remainder);
    }

    /**
     * Add a value to a month
     *
     * @param pMonth The month
     * @param pAccount
     * @param pValue The value
     */
    public void addValueToMonth(SSMonth pMonth, SSAccount pAccount, BigDecimal pValue) {
        BigDecimal current = iBudget.get(pMonth).get(pAccount);

        if (current != null) {
            iBudget.get(pMonth).put(pAccount, current.add(pValue));
        } else {
            iBudget.get(pMonth).put(pAccount, pValue);
        }

    }

    /**
     * Get the budget sum for an account.
     *
     * @param pAccount The account to get the sum from.
     *
     * @return The sum
     */
    public BigDecimal getSumForAccount(SSAccount pAccount) {
        BigDecimal iSum = new BigDecimal(0);

        for (Map.Entry<SSMonth, Map<SSAccount, BigDecimal>> ssMonthMapEntry : iBudget.entrySet()) {
            BigDecimal iValue = ssMonthMapEntry.getValue().get(pAccount);

            if (iValue == null) {
                continue;
            }

            iSum = iSum.add(iValue);
        }
        return iSum.signum() == 0 ? null : iSum;
    }

    /**
     * Get the budget sum for an account.
     *
     * @param pAccount The account to get the sum from.
     * @param pFrom the start date
     * @param pTo the end date
     *
     * @return The sum
     */
    public BigDecimal getSumForAccount(SSAccount pAccount, LocalDate pFrom, LocalDate pTo) {
        BigDecimal iSum = new BigDecimal(0);

        for (Map.Entry<SSMonth, Map<SSAccount, BigDecimal>> ssMonthMapEntry : iBudget.entrySet()) {
            BigDecimal iValue = ssMonthMapEntry.getValue().get(pAccount);

            if (iValue == null || !ssMonthMapEntry.getKey().isBetween(pFrom, pTo)) {
                continue;
            }

            iSum = iSum.add(iValue);
        }
        return iSum.signum() == 0 ? null : iSum;
    }

    /**
     * Get the budget sum for all accounts.
     *
     * @return The sum
     */
    public Map<SSAccount, BigDecimal> getSumForAccounts() {
        Map<SSAccount, BigDecimal> sum = new HashMap<>();

        for (SSAccount account: getAccounts()) {
            sum.put(account, getSumForAccount(account));
        }
        return sum;
    }

    /**
     * Get the budget sum for all accounts.
     *
     * @param pFrom the start date
     * @param pTo the end date
     * @return The sum
     */
    public Map<SSAccount, BigDecimal> getSumForAccounts(LocalDate pFrom, LocalDate pTo) {
        Map<SSAccount, BigDecimal> sum = new HashMap<>();

        for (SSAccount account: getAccounts()) {
            sum.put(account, getSumForAccount(account, pFrom, pTo));
        }
        return sum;
    }

    /**
     * Sets the budget value for an account and month.
     *
     * @param pAccount The account to set the value to.
     * @param pMonth The month to set the value to.
     * @param pValue The value
     */
    public void setSaldoForAccountAndMonth(SSAccount pAccount, SSMonth pMonth, BigDecimal pValue) {
        Map<SSAccount, BigDecimal> iMonthlyBudget = iBudget.get(pMonth);

        if (iMonthlyBudget != null) {

            if (pValue == null || pValue.signum() == 0) {
                iMonthlyBudget.put(pAccount, null);
            } else {
                iMonthlyBudget.put(pAccount, pValue);
            }
        }
    }

    /**
     * Get the budget value for an account and month.
     *
     * @param pAccount The account to get value sum from.
     * @param pMonth The month to set the value to.
     *
     * @return The value
     */
    public Optional<BigDecimal> getValueForAccountAndMonth(SSAccount pAccount, SSMonth pMonth) {

        Map<SSAccount, BigDecimal> iMonthlyBudget = iBudget.get(pMonth);

        if (iMonthlyBudget != null) {
            return Optional.ofNullable(iMonthlyBudget.get(pAccount));
        }
        return Optional.empty();
    }

    /**
     * Breaks a accounting year into it's months
     *
     * @return the new map
     */
    private Map <SSMonth, Map<SSAccount, BigDecimal>> createBudgetForYear() {
        Map<SSMonth, Map<SSAccount, BigDecimal>> iNewBudget = createEmptyBudgetForYear();
        Map<SSAccount, BigDecimal> iSum = getSumForAccounts();

        // Set the sums
        for (Map.Entry<SSAccount, BigDecimal> ssAccountBigDecimalEntry : iSum.entrySet()) {
            setSumForAccount(ssAccountBigDecimalEntry.getKey(),
                    ssAccountBigDecimalEntry.getValue());
        }
        return iNewBudget;
    }

    private Map<SSMonth, Map<SSAccount, BigDecimal>> createEmptyBudgetForYear() {
        Map<SSMonth, Map<SSAccount, BigDecimal>> iNewBudget = new HashMap<>();
        List<SSMonth> iMonths = SSMonth.splitYearIntoMonths(iAccountingYear);

        for (SSMonth iMonth : iMonths) {
            iNewBudget.put(iMonth, new HashMap<>());
        }
        return iNewBudget;
    }

    public String toString() {
        DateFormat format = DateFormat.getDateInstance(DateFormat.SHORT);

        StringBuffer b = new StringBuffer();

        b.append("Budget for ");
        b.append(format.format(iFrom));
        b.append(" to");
        b.append(format.format(iTo));

        for (SSMonth iMonth: getMonths()) {
            b.append("Month: ");
            b.append(iMonth);
            b.append("{\n");

            for (SSAccount iAccount: iBudget.get(iMonth).keySet()) {
                b.append("  Account: \n");
                b.append("  ");
                b.append(iAccount);
                b.append("    Sum:");
                b.append("    ");
                b.append(iBudget.get(iMonth).get(iAccount));
                b.append('\n');
            }
            b.append("}\n");
        }

        return b.toString();
    }

}
