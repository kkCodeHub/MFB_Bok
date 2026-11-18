package se.swedsoft.bookkeeping.calc.util;


import se.swedsoft.bookkeeping.calc.math.SSAccountMath;
import se.swedsoft.bookkeeping.calc.math.SSVoucherMath;
import se.swedsoft.bookkeeping.data.SSAccount;
import se.swedsoft.bookkeeping.data.SSVoucher;
import se.swedsoft.bookkeeping.data.SSVoucherRow;
import se.swedsoft.bookkeeping.data.system.SSDB;
import se.swedsoft.bookkeeping.gui.util.SSBundle;
import se.swedsoft.bookkeeping.util.SSDateUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DateFormat;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;


/**
 */
public class SSVATUtil {
    private SSVATUtil() {}

    /**
     * @param creditMinusDebetSum The map of the summirised vouchers for the account
     *
     * @return The result
     */
    public static BigDecimal getVatToPayOrRetrieve(Map<SSAccount, BigDecimal> creditMinusDebetSum) {
	// Lagt till momskoder för importmoms: UI1 UI2 UI3
        return SSAccountMath.getSumByVATCodeForAccounts(creditMinusDebetSum, "10", "11",
                "12", "30", "31", "32", "48", "60", "61", "62");
    }

    /**
     * @param creditMinusDebetSum The map of the summirised vouchers for the account
     *
     * @return The rounded result
     */
    public static BigDecimal getVatToPayOrRetrieveRounded(Map<SSAccount, BigDecimal> creditMinusDebetSum) {
        return getVatToPayOrRetrieve(creditMinusDebetSum).setScale(0, RoundingMode.DOWN);
    }

    /**
     * Adds a new account to the required lists
     * @param account
     * @param accounts
     * @param creditMinusDebetSum
     * @param debetMinusCreditSum
     */
    private static void addAccount(SSAccount account, List<SSAccount>accounts, Map<SSAccount, BigDecimal> creditMinusDebetSum, Map<SSAccount, BigDecimal> debetMinusCreditSum) {
        accounts.add(account);
        creditMinusDebetSum.put(account, new BigDecimal(0));
        debetMinusCreditSum.put(account, new BigDecimal(0));
    }

    public static SSVoucher generateVATVoucher(String name, LocalDate iDateFrom, LocalDate iDateTo, SSAccount iAccountR1, SSAccount iAccountR2, SSAccount iAccountA) {

        DateFormat iFormat = DateFormat.getDateInstance(DateFormat.SHORT);

        String iDescription = String.format(
                SSBundle.getBundle().getString("vatreport2015.voucherdescription"),
                iFormat.format(SSDateUtil.toDate(iDateFrom)), iFormat.format(SSDateUtil.toDate(iDateTo)));
	// Lagt till momskoder för importmoms: UI1 UI2 UI3
        List<SSAccount> iAccounts = SSAccountMath.getAccountsByVATCode(
                se.swedsoft.bookkeeping.data.system.SSAccountingContext.getAccounts(), "10", "11", "12", "30", "31", "32",
                "48", "60", "61", "62");
        List<SSVoucher> iVouchers = SSVoucherMath.getVouchers(
                se.swedsoft.bookkeeping.data.system.SSAccountingContext.getVouchers(), iDateFrom, iDateTo);

        Map<SSAccount, BigDecimal> iCreditMinusDebetSum = SSVoucherMath.getCreditMinusDebetSum(
                iVouchers);
        Map<SSAccount, BigDecimal> debetMinusCreditSum = SSVoucherMath.getDebetMinusCreditSum(
                iVouchers);

        SSVoucher    iVoucher = new SSVoucher();

        iVoucher.doAutoIncrecement();
        iVoucher.setDescription(iDescription);
        iVoucher.setLocalDate(iDateTo);

        BigDecimal iSum = new BigDecimal(0);
        SSVoucherRow iRow;

        for (SSAccount iAccount : iAccounts) {

            BigDecimal iValue = iCreditMinusDebetSum.get(iAccount);

            if (iValue == null || iValue.signum() == 0) {
                continue;
            }

            iRow = new SSVoucherRow();
            iRow.setAccount(iAccount);
            iRow.setValue(iValue);

            iVoucher.addVoucherRow(iRow);

            iSum = iSum.add(iValue);
        }

        iRow = new SSVoucherRow();

        BigDecimal iRounded = iSum.setScale(0, RoundingMode.DOWN);

        if (iRounded.signum() > 0) {
            iRow.setAccount(iAccountR2);
            iRow.setCredit(iRounded);
        } else {
            iRow.setAccount(iAccountR1);
            iRow.setDebet(iRounded.abs());
        }

        iVoucher.addVoucherRow(iRow);

        if (iRounded.subtract(iSum).signum() != 0) {
            iRow = new SSVoucherRow();
            iRow.setAccount(iAccountA);
            iRow.setValue(iRounded.subtract(iSum));

            iVoucher.addVoucherRow(iRow);
        }

        return iVoucher;
    }

}
