package se.swedsoft.bookkeeping.calc.math;


import se.swedsoft.bookkeeping.data.SSCustomer;
import se.swedsoft.bookkeeping.data.SSInpayment;
import se.swedsoft.bookkeeping.data.SSInpaymentRow;
import se.swedsoft.bookkeeping.data.SSInvoice;
import se.swedsoft.bookkeeping.data.system.SSDB;
import se.swedsoft.bookkeeping.util.SSDateUtil;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;


/**
 * User: Andreas Lago
 * Date: 2006-apr-07
 * Time: 15:18:22
 */
public class SSInpaymentMath {
    private SSInpaymentMath() {}

    /**
     *
     * @param iInpayment
     * @param pFrom
     * @param pTo
     * @return
     */
    public static boolean inPeriod(SSInpayment iInpayment, Date pFrom, Date pTo) {
        LocalDate iDate = iInpayment.getLocalDate();
        LocalDate iFrom = SSDateUtil.toLocalDate(pFrom);
        LocalDate iTo = SSDateUtil.toLocalDate(pTo);

        return iDate != null && iFrom != null && iTo != null
                && !iDate.isBefore(iFrom) && !iDate.isAfter(iTo);
    }

    /**
     * Convers a value from a row currency to the company currency
     *
     * @param iRow
     * @param iValue
     * @return the converted value
     */
    public static BigDecimal convertToLocal(SSInpaymentRow iRow, BigDecimal iValue) {
        BigDecimal iCurrencyRate = iRow.getCurrencyRate();

        if (iCurrencyRate != null) {
            iValue = iValue.multiply(iCurrencyRate);
        }

        return iValue;
    }

    /**
     * Get the sum of the inpayments for the supplied sales in the sales currency
     *
     * @param iInpayment
     * @param iInvoice
     * @return the sum
     */
    public static BigDecimal getSumForInvoice(SSInpayment iInpayment, SSInvoice iInvoice) {

        BigDecimal iSum = new BigDecimal(0);

        for (SSInpaymentRow iRow: iInpayment.getRows()) {
            BigDecimal iRowValue = iRow.getValue();

            if (iRowValue != null && iRow.isPaying(iInvoice)) {
                iSum = iSum.add(iRowValue);
            }
        }

        return iSum;
    }

    /**
     * Get the sum of all inpayments for the supplied sales in the sales currency
     *
     * @param iInvoice
     * @return the sum
     */
    public static BigDecimal getSumForInvoice(SSInvoice iInvoice) {
        List<SSInpayment> iInpayments = SSDB.getInstance().getInpayments();

        BigDecimal iSum = new BigDecimal(0);

        for (SSInpayment iInpayment : iInpayments) {
            BigDecimal iRowSum = getSumForInvoice(iInpayment, iInvoice);

            iSum = iSum.add(iRowSum);
        }

        return iSum;
    }

    public static HashMap<Integer, BigDecimal> getSumsForInvoices() {
        HashMap<Integer, BigDecimal> iSums = new HashMap<>();

        List<SSInpayment> iInpayments = SSDB.getInstance().getInpayments();

        for (SSInpayment iInpayment : iInpayments) {
            for (SSInpaymentRow iRow : iInpayment.getRows()) {
                if (iRow.getValue() != null) {
                    if (iSums.containsKey(iRow.getInvoiceNr())) {
                        iSums.put(iRow.getInvoiceNr(),
                                iSums.get(iRow.getInvoiceNr()).add(iRow.getValue()));
                    } else {
                        iSums.put(iRow.getInvoiceNr(), iRow.getValue());
                    }
                }
            }
        }
        return iSums;
    }

    public static HashMap<Integer, BigDecimal> getSumsForInvoices(Date iDate) {
        HashMap<Integer, BigDecimal> iSums = new HashMap<>();

        List<SSInpayment> iInpayments = SSDB.getInstance().getInpayments();
        LocalDate localDate = SSDateUtil.toLocalDate(iDate);

        for (SSInpayment iInpayment : iInpayments) {
            if (iInpayment.getLocalDate() != null && localDate != null
                    && !iInpayment.getLocalDate().isAfter(localDate)) {
                for (SSInpaymentRow iRow : iInpayment.getRows()) {
                    if (iRow.getValue() != null) {
                        if (iSums.containsKey(iRow.getInvoiceNr())) {
                            iSums.put(iRow.getInvoiceNr(),
                                    iSums.get(iRow.getInvoiceNr()).add(iRow.getValue()));
                        } else {
                            iSums.put(iRow.getInvoiceNr(), iRow.getValue());
                        }
                    }
                }
            }
        }
        return iSums;
    }

    /**
     * Get the sum of all inpayments for the supplied sales in the sales currency up and including to the selected date
     *
     * @param iInvoice
     * @param iDate
     * @return the sum
     */
    public static BigDecimal getSumForInvoice(SSInvoice iInvoice, Date iDate) {
        List<SSInpayment> iInpayments = SSDB.getInstance().getInpayments();

        LocalDate localDate = SSDateUtil.toLocalDate(iDate);
        BigDecimal iSum = new BigDecimal(0);

        for (SSInpayment iInpayment : iInpayments) {
            LocalDate iCurrent = iInpayment.getLocalDate();

            BigDecimal iRowSum = getSumForInvoice(iInpayment, iInvoice);

            if (iCurrent != null && localDate != null && !iCurrent.isAfter(localDate)) {
                iSum = iSum.add(iRowSum);
            }
        }

        return iSum;
    }

    /**
     * Get the date of the last inpayment for the supplied sales, or
     *
     * @param iInvoice
     * @return the sum
     */
    public static Date getLastInpaymentForInvoice(SSInvoice iInvoice) {

        List<SSInpayment> iInpayments = SSDB.getInstance().getInpayments();

        LocalDate iDate = null;

        for (SSInpayment iInpayment : iInpayments) {
            LocalDate iRowDate = iInpayment.getLocalDate();

            if (iInpayment.isPaying(iInvoice)
                    && iRowDate != null
                    && (iDate == null || iRowDate.isAfter(iDate))) {
                iDate = iRowDate;
            }

        }

        return SSDateUtil.toDate(iDate);
    }

    /**
     * Get the sum for the inpayment in the payment currency
     *
     * @param iInpayment
     * @return the sum
     */
    public static BigDecimal getSum(SSInpayment iInpayment) {

        BigDecimal iSum = new BigDecimal(0);

        for (SSInpaymentRow iRow: iInpayment.getRows()) {
            BigDecimal iValue = iRow.getValue();

            if (iValue != null) {
                // Convert to the local currency
                iValue = convertToLocal(iRow, iValue);

                iSum = iSum.add(iValue);
            }
        }

        return iSum;

    }

    /**
     * Get the saldo for the sales of the current row in the sales currency, returns empty if no sales
     *
     * @param iInpaymentRow
     * @return the saldo
     */
    public static Optional<BigDecimal> getSaldo(SSInpaymentRow iInpaymentRow) {
        SSInvoice iInvoice = iInpaymentRow.getInvoice(SSDB.getInstance().getInvoices());

        if (iInvoice == null) {
            return Optional.empty();
        }

        return Optional.of(SSInvoiceMath.getSaldo(iInvoice.getNumber()));
    }

    /**
     * Get the difference between the value of the sales in the sales currency and the payed value, in the company currency such as
     *
     * return (Value of row) * (payment currency rate - sales currency rate)
     *
     * @param iInpaymentRow
     * @return the currency rate difference
     */
    public static Optional<BigDecimal> getCurrencyRateDifference(SSInpaymentRow iInpaymentRow) {
        BigDecimal iPaymentRate = iInpaymentRow.getCurrencyRate();
        BigDecimal iCurrencyRate = iInpaymentRow.getInvoiceCurrencyRate();
        BigDecimal iValue = iInpaymentRow.getValue();

        if (iPaymentRate == null || iCurrencyRate == null || iValue == null) {
            return Optional.empty();
        }

        return Optional.of(iValue.multiply(iPaymentRate.subtract(iCurrencyRate)));
    }

    /**
     * Get the currency rate difference for the whole inpayment, in the company currency
     *
     * @param iInpayment
     * @return the currency rate difference
     */
    public static BigDecimal getCurrencyRateDifference(SSInpayment iInpayment) {
        BigDecimal iSum = new BigDecimal(0);

        for (SSInpaymentRow iRow: iInpayment.getRows()) {
            Optional<BigDecimal> iRowSum = getCurrencyRateDifference(iRow);

            if (iRowSum.isPresent()) {
                iSum = iSum.add(iRowSum.get());
            }
        }

        return iSum;

    }

    /**
     *
     * @param iInpayment
     * @param iCustomer
     * @return
     */
    public static boolean hasCustomer(SSInpayment iInpayment, SSCustomer iCustomer) {
        List<SSInvoice> iInvoices = SSDB.getInstance().getInvoices();

        for (SSInpaymentRow iRow : iInpayment.getRows()) {
            SSInvoice iInvoice = iRow.getInvoice(iInvoices);

            if (iInvoice != null && iInvoice.hasCustomer(iCustomer)) {
                return true;
            }
        }
        return false;
    }

    /**
     *
     * @param iInpayment
     * @param iInvoice
     * @return
     */
    public static boolean hasInvoice(SSInpayment iInpayment, SSInvoice iInvoice) {

        for (SSInpaymentRow iRow : iInpayment.getRows()) {
            if (iRow.isPaying(iInvoice)) {
                return true;
            }
        }
        return false;
    }
}
