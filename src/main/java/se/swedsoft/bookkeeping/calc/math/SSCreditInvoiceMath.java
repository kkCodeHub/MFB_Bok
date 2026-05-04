package se.swedsoft.bookkeeping.calc.math;


import se.swedsoft.bookkeeping.data.*;
import se.swedsoft.bookkeeping.data.base.SSSaleRow;
import se.swedsoft.bookkeeping.data.system.SSDB;
import se.swedsoft.bookkeeping.util.SSDateUtil;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;


/**
 * User: Andreas Lago
 * Date: 2006-apr-11
 * Time: 09:22:51
 */
public class SSCreditInvoiceMath extends SSInvoiceMath {

    /**
     * Get the sum for the credit sales in the sales currency
     *
     * @param iInvoice
     * @return the sum
     */
    public static BigDecimal getSumForInvoice(SSInvoice iInvoice) {
        // Get all credit invoices from the db
        List<SSCreditInvoice> iCreditInvoices = SSDB.getInstance().getCreditInvoices();

        BigDecimal iSum = new BigDecimal(0);

        for (SSCreditInvoice iCreditInvoice : iCreditInvoices) {
            BigDecimal iRowSum = getTotalSum(iCreditInvoice);

            if (iRowSum != null && iCreditInvoice.isCrediting(iInvoice)) {
                iSum = iSum.add(iRowSum);
            }
        }
        return iSum;
    }

    public static HashMap<Integer, BigDecimal> getSumsForInvoices() {
        HashMap<Integer, BigDecimal> iSums = new HashMap<>();

        List<SSCreditInvoice> iCreditInvoices = SSDB.getInstance().getCreditInvoices();

        for (SSCreditInvoice iCreditInvoice : iCreditInvoices) {
            BigDecimal iRowSum = getTotalSum(iCreditInvoice);

            if (iRowSum != null && iCreditInvoice.getCreditingNr() != null) {
                if (iSums.containsKey(iCreditInvoice.getCreditingNr())) {
                    iSums.put(iCreditInvoice.getCreditingNr(),
                            iSums.get(iCreditInvoice.getCreditingNr()).add(iRowSum));
                } else {
                    iSums.put(iCreditInvoice.getCreditingNr(), iRowSum);
                }
            }
        }
        return iSums;
    }

    public static HashMap<Integer, BigDecimal> getSumsForInvoices(Date iDate) {
        HashMap<Integer, BigDecimal> iSums = new HashMap<>();

        List<SSCreditInvoice> iCreditInvoices = SSDB.getInstance().getCreditInvoices();
        LocalDate localDate = SSDateUtil.toLocalDate(iDate);

        for (SSCreditInvoice iCreditInvoice : iCreditInvoices) {
            if (iCreditInvoice.getLocalDate() != null && localDate != null
                    && !iCreditInvoice.getLocalDate().isAfter(localDate)) {
                BigDecimal iRowSum = getTotalSum(iCreditInvoice);

                if (iRowSum != null && iCreditInvoice.getCreditingNr() != null) {
                    if (iSums.containsKey(iCreditInvoice.getCreditingNr())) {
                        iSums.put(iCreditInvoice.getCreditingNr(),
                                iSums.get(iCreditInvoice.getCreditingNr()).add(iRowSum));
                    } else {
                        iSums.put(iCreditInvoice.getCreditingNr(), iRowSum);
                    }
                }
            }
        }
        return iSums;
    }

    /**
     * Get the sum for the credit sales in the sales currency up and including to the selected date
     *
     * @param iInvoice
     * @param iDate
     * @return the sum
     */
    public static BigDecimal getSumForInvoice(SSInvoice iInvoice, Date iDate) {
        // Get all credit invoices from the db
        List<SSCreditInvoice> iCreditInvoices = SSDB.getInstance().getCreditInvoices();

        LocalDate localDate = SSDateUtil.toLocalDate(iDate);
        BigDecimal iSum = new BigDecimal(0);

        for (SSCreditInvoice iCreditInvoice : iCreditInvoices) {
            LocalDate iCurrent = iCreditInvoice.getLocalDate();

            BigDecimal iRowSum = getTotalSum(iCreditInvoice);

            if (iRowSum != null && iCreditInvoice.isCrediting(iInvoice)
                    && iCurrent != null && localDate != null && !iCurrent.isAfter(localDate)) {
                iSum = iSum.add(iRowSum);
            }
        }
        return iSum;
    }

    /**
     *
     * @param iInvoice
     * @return list of credit invoices
     */
    public static List<SSCreditInvoice> getCreditInvoicesForInvoice(SSInvoice iInvoice) {
        return getCreditInvoicesForInvoice(SSDB.getInstance().getCreditInvoices(),
                iInvoice);

    }

    /**
     *
     * @param iCreditInvoices
     * @param iInvoice
     * @return list of credit invoices
     */
    public static List<SSCreditInvoice> getCreditInvoicesForInvoice(List<SSCreditInvoice> iCreditInvoices, SSInvoice iInvoice) {
        return iCreditInvoices.stream()
                .filter(iCreditInvoice -> iCreditInvoice.isCrediting(iInvoice))
                .collect(Collectors.toList());
    }

    /**
     * Returns all credit invoices for the current customer
     *
     * @param iCustomer
     * @return the credit invoices for the customer
     */
    public static List<SSCreditInvoice> getCreditInvoicesForCustomer(SSCustomer iCustomer) {

        return getCreditInvoicesForCustomer(SSDB.getInstance().getCreditInvoices(),
                iCustomer);
    }

    /**
     * Returns all credit invoices for the current customer
     *
     * @param iCreditInvoices
     * @param iCustomer
     * @return the credit invoices for the customer
     */
    public static List<SSCreditInvoice> getCreditInvoicesForCustomer(List<SSCreditInvoice> iCreditInvoices, SSCustomer iCustomer) {
        return iCreditInvoices.stream()
                .filter(iCreditInvoice -> iCreditInvoice.hasCustomer(iCustomer))
                .collect(Collectors.toList());
    }

    public static Map<String, List<SSCreditInvoice>> getCreditInvoicesforCustomers() {
        List<SSCreditInvoice> iCreditInvoices = SSDB.getInstance().getCreditInvoices();
        Map<String, List<SSCreditInvoice>> iMap = new HashMap<>();

        for (SSCreditInvoice iCreditInvoice : iCreditInvoices) {
            if (iCreditInvoice.getCustomerNr() != null) {
                if (iMap.containsKey(iCreditInvoice.getCustomerNr())) {
                    iMap.get(iCreditInvoice.getCustomerNr()).add(iCreditInvoice);
                } else {
                    List<SSCreditInvoice> iTemp = new LinkedList<>();

                    iTemp.add(iCreditInvoice);
                    iMap.put(iCreditInvoice.getCustomerNr(), iTemp);
                }
            }
        }
        return iMap;
    }

}
