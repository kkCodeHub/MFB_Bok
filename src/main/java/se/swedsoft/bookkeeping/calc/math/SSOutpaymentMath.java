package se.swedsoft.bookkeeping.calc.math;


import se.swedsoft.bookkeeping.data.SSOutpayment;
import se.swedsoft.bookkeeping.data.SSOutpaymentRow;
import se.swedsoft.bookkeeping.data.SSSupplierInvoice;
import se.swedsoft.bookkeeping.data.system.SSDB;
import se.swedsoft.bookkeeping.persistence.Repositories;
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
public class SSOutpaymentMath {
    private SSOutpaymentMath() {}

    /**
     * Returns true if the outpayment's date falls within [pFrom, pTo] inclusive.
     *
     * @param iOutpayment the outpayment to test
     * @param pFrom       the start of the period
     * @param pTo         the end of the period
     * @return true if within the period
     */
    public static boolean inPeriod(SSOutpayment iOutpayment, LocalDate pFrom, LocalDate pTo) {
        LocalDate iDate = iOutpayment.getLocalDate();

        return iDate != null && pFrom != null && pTo != null
                && !iDate.isBefore(pFrom) && !iDate.isAfter(pTo);
    }

    /**
     * Outpayments are handled in a single currency. No conversion is applied.
     *
     * @param iRow   the outpayment row
     * @param iValue the amount
     * @return the same amount
     */
    public static BigDecimal convertToLocal(SSOutpaymentRow iRow, BigDecimal iValue) {
        return iValue;
    }

    /**
     * Get the sum for the outpayment in the single outpayment currency.
     *
     * @param iOutpayment the outpayment
     * @return the sum
     */
    public static BigDecimal getSum(SSOutpayment iOutpayment) {

        BigDecimal iSum = new BigDecimal(0);

        for (SSOutpaymentRow iRow: iOutpayment.getRows()) {
            BigDecimal iValue = iRow.getValue();

            if (iValue != null) {
                iSum = iSum.add(iValue);
            }
        }

        return iSum;

    }

    /**
     * Get remaining supplier invoice saldo after the row payment is applied.
     *
     * @param iOutpaymentRow the outpayment row
     * @return remaining saldo after payment
     */
    public static Optional<BigDecimal> getCurrencyRateDifference(SSOutpaymentRow iOutpaymentRow) {
        Optional<BigDecimal> iInvoiceSaldo = getSaldo(iOutpaymentRow);
        BigDecimal iPaymentValue = iOutpaymentRow.getValue();

        if (iInvoiceSaldo.isEmpty() || iPaymentValue == null) {
            return Optional.empty();
        }

        return Optional.of(iInvoiceSaldo.get().subtract(iPaymentValue));
    }

    /**
     * Get the saldo for the supplier invoice of the current row in the invoice currency, returns empty if no invoice.
     *
     * @param iOutpaymentRow the outpayment row
     * @return the saldo
     */
    public static Optional<BigDecimal> getSaldo(SSOutpaymentRow iOutpaymentRow) {
        SSSupplierInvoice iInvoice = iOutpaymentRow.getSupplierInvoice(Repositories.supplierInvoices().findAll());

        if (iInvoice == null) {
            return Optional.empty();
        }

        return Optional.of(SSSupplierInvoiceMath.getSaldo(iInvoice.getNumber()));
    }

    /**
     * Resolve default outpayment amount from supplier invoice saldo.
     * Negative saldo is clamped to zero.
     *
     * @param iInvoice the supplier invoice
     * @return default outpayment amount
     */
    public static BigDecimal getOutpaymentAmountForInvoice(SSSupplierInvoice iInvoice) {
        if (iInvoice == null) {
            return BigDecimal.ZERO.setScale(2);
        }
        BigDecimal iSaldo = SSSupplierInvoiceMath.getSaldo(iInvoice);
        if (iSaldo.signum() < 0) {
            return BigDecimal.ZERO.setScale(2);
        }
        return iSaldo;
    }

    /**
     * Get total remaining saldo after payment across all outpayment rows.
     *
     * @param iOutpayment the outpayment
     * @return summed remaining saldo
     */
    public static BigDecimal getCurrencyRateDifference(SSOutpayment iOutpayment) {
        BigDecimal iSum = new BigDecimal(0);

        for (SSOutpaymentRow iRow: iOutpayment.getRows()) {
            Optional<BigDecimal> iRowSum = getCurrencyRateDifference(iRow);

            if (iRowSum.isPresent()) {
                iSum = iSum.add(iRowSum.get());
            }
        }

        return iSum;

    }

    /**
     * Get the sum of the inpayments for the supplied sales in the sales currency
     *
     * @param iOutpayment
     * @param iInvoice
     * @return the sum
     */
    public static BigDecimal getSumForInvoice(SSOutpayment iOutpayment, SSSupplierInvoice iInvoice) {

        BigDecimal iSum = new BigDecimal(0);

        for (SSOutpaymentRow iRow: iOutpayment.getRows()) {
            BigDecimal iRowValue = iRow.getValue();

            if (iRowValue != null && iRow.isPaying(iInvoice)) {
                iSum = iSum.add(iRowValue);
            }
        }

        return iSum;
    }

    /**
     * Get the sum for the credit sales in the sales currency
     *
     * @param iInvoice
     * @return the sum
     */
    public static BigDecimal getSumForInvoice(SSSupplierInvoice iInvoice) {
        // Get all outpayments from SSDB cache instead of Repositories to ensure fresh/updated data
        List<SSOutpayment> iOutpayments = se.swedsoft.bookkeeping.data.system.SSPaymentContext.getOutpayments();

        BigDecimal iSum = new BigDecimal(0);

        for (SSOutpayment iOutpayment : iOutpayments) {
            BigDecimal iRowSum = getSumForInvoice(iOutpayment, iInvoice);

            iSum = iSum.add(iRowSum);
        }

        return iSum;
    }

    public static HashMap<Integer, BigDecimal> getSumsForSupplierInvoices() {
        HashMap<Integer, BigDecimal> iSums = new HashMap<>();

        // Use SSDB cache instead of Repositories to ensure fresh/updated data
        List<SSOutpayment> iOutpayments = se.swedsoft.bookkeeping.data.system.SSPaymentContext.getOutpayments();

        for (SSOutpayment iOutpayment : iOutpayments) {
            for (SSOutpaymentRow iRow : iOutpayment.getRows()) {
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

    public static HashMap<Integer, BigDecimal> getSumsForSupplierInvoices(Date iDate) {
        HashMap<Integer, BigDecimal> iSums = new HashMap<>();

        // Use SSDB cache instead of Repositories to ensure fresh/updated data
        List<SSOutpayment> iOutpayments = se.swedsoft.bookkeeping.data.system.SSPaymentContext.getOutpayments();
        LocalDate localDate = SSDateUtil.toLocalDate(iDate);

        for (SSOutpayment iOutpayment : iOutpayments) {
            if (iOutpayment.getLocalDate() != null && localDate != null
                    && !iOutpayment.getLocalDate().isAfter(localDate)) {
                for (SSOutpaymentRow iRow : iOutpayment.getRows()) {
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
     * Get the sum of all outpayments for the supplied supplier invoice in the invoice currency up and including to the selected date
     *
     * @param iInvoice
     * @param iDate
     * @return the sum
     */
    public static BigDecimal getSumForInvoice(SSSupplierInvoice iInvoice, Date iDate) {
        // Use SSDB cache instead of Repositories to ensure fresh/updated data
        List<SSOutpayment> iOutpayments = se.swedsoft.bookkeeping.data.system.SSPaymentContext.getOutpayments();

        LocalDate localDate = SSDateUtil.toLocalDate(iDate);
        BigDecimal iSum = new BigDecimal(0);

        for (SSOutpayment iOutpayment : iOutpayments) {
            LocalDate iCurrent = iOutpayment.getLocalDate();

            BigDecimal iRowSum = getSumForInvoice(iOutpayment, iInvoice);

            if (iCurrent != null && localDate != null && !iCurrent.isAfter(localDate)) {
                iSum = iSum.add(iRowSum);
            }
        }

        return iSum;
    }

    /**
     *
     * @param iOutpayment
     * @param iInvoice
     * @return
     */
    public static boolean hasInvoice(SSOutpayment iOutpayment, SSSupplierInvoice iInvoice) {

        for (SSOutpaymentRow iRow : iOutpayment.getRows()) {
            if (iRow.isPaying(iInvoice)) {
                return true;
            }
        }
        return false;
    }

}
