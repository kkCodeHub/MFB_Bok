package se.swedsoft.bookkeeping.calc.math;


import se.swedsoft.bookkeeping.data.*;
import se.swedsoft.bookkeeping.data.system.SSDB;
import se.swedsoft.bookkeeping.persistence.Repositories;
import se.swedsoft.bookkeeping.util.SSDateUtil;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;


/**
 * User: Andreas Lago
 * Date: 2006-jun-14
 * Time: 09:13:54
 */
public class SSSupplierCreditInvoiceMath {
    private SSSupplierCreditInvoiceMath() {}

    /**
     * Returns true if the supplier credit invoice's date falls within [pFrom, pTo] inclusive.
     *
     * @param iSupplierInvoice the invoice to test
     * @param pFrom            the start of the period
     * @param pTo              the end of the period
     * @return true if within the period
     */
    public static boolean inPeriod(SSSupplierCreditInvoice iSupplierInvoice, LocalDate pFrom, LocalDate pTo) {
        LocalDate iDate = iSupplierInvoice.getLocalDate();

        return iDate != null && pFrom != null && pTo != null
                && !iDate.isBefore(pFrom) && !iDate.isAfter(pTo);
    }

    /**
     * Get the sum for the credit sales in the sales currency
     *
     * @param iInvoice
     * @return the sum
     */
    public static BigDecimal getSumForInvoice(SSSupplierInvoice iInvoice) {
        // Get all credit invoices from the db
        List<SSSupplierCreditInvoice> iCreditInvoices = Repositories.supplierCreditInvoices().findAll();

        BigDecimal iSum = new BigDecimal(0);

        for (SSSupplierCreditInvoice iCreditInvoice : iCreditInvoices) {
            BigDecimal iRowSum = SSSupplierInvoiceMath.getTotalSum(iCreditInvoice);

            if (iRowSum != null && iCreditInvoice.isCrediting(iInvoice)) {
                iSum = iSum.add(iRowSum);
            }
        }
        return iSum;
    }

    /**
     * Get the sum for the credit sales in the sales currency up and including to the selected date
     *
     * @param iInvoice
     * @param iDate
     * @return the sum
     */
    public static BigDecimal getSumForInvoice(SSSupplierInvoice iInvoice, Date iDate) {
        // Get all credit invoices from the db
        List<SSSupplierCreditInvoice> iCreditInvoices = Repositories.supplierCreditInvoices().findAll();

        LocalDate localDate = SSDateUtil.toLocalDate(iDate);
        BigDecimal iSum = new BigDecimal(0);

        for (SSSupplierCreditInvoice iCreditInvoice : iCreditInvoices) {
            LocalDate iCurrent = iCreditInvoice.getLocalDate();

            BigDecimal iRowSum = SSSupplierInvoiceMath.getTotalSum(iCreditInvoice);

            if (iRowSum != null && iCreditInvoice.isCrediting(iInvoice)
                    && iCurrent != null && localDate != null && !iCurrent.isAfter(localDate)) {
                iSum = iSum.add(iRowSum);
            }
        }
        return iSum;
    }

    public static HashMap<Integer, BigDecimal> getSumsForSupplierInvoices() {
        HashMap<Integer, BigDecimal> iSums = new HashMap<>();

        List<SSSupplierCreditInvoice> iSupplierCreditInvoices = Repositories.supplierCreditInvoices().findAll();

        for (SSSupplierCreditInvoice iSupplierCreditInvoice : iSupplierCreditInvoices) {
            BigDecimal iRowSum = SSSupplierInvoiceMath.getTotalSum(iSupplierCreditInvoice);

            if (iRowSum != null && iSupplierCreditInvoice.getCreditingNr() != null) {
                if (iSums.containsKey(iSupplierCreditInvoice.getCreditingNr())) {
                    iSums.put(iSupplierCreditInvoice.getCreditingNr(),
                            iSums.get(iSupplierCreditInvoice.getCreditingNr()).add(iRowSum));
                } else {
                    iSums.put(iSupplierCreditInvoice.getCreditingNr(), iRowSum);
                }
            }
        }
        return iSums;
    }

    public static HashMap<Integer, BigDecimal> getSumsForSupplierInvoices(Date iDate) {
        HashMap<Integer, BigDecimal> iSums = new HashMap<>();
        LocalDate localDate = SSDateUtil.toLocalDate(iDate);

        List<SSSupplierCreditInvoice> iSupplierCreditInvoices = Repositories.supplierCreditInvoices().findAll();

        for (SSSupplierCreditInvoice iSupplierCreditInvoice : iSupplierCreditInvoices) {
            if (iSupplierCreditInvoice.getLocalDate() != null && localDate != null
                    && !iSupplierCreditInvoice.getLocalDate().isAfter(localDate)) {
                BigDecimal iRowSum = SSSupplierInvoiceMath.getTotalSum(
                        iSupplierCreditInvoice);

                if (iRowSum != null && iSupplierCreditInvoice.getCreditingNr() != null) {
                    if (iSums.containsKey(iSupplierCreditInvoice.getCreditingNr())) {
                        iSums.put(iSupplierCreditInvoice.getCreditingNr(),
                                iSums.get(iSupplierCreditInvoice.getCreditingNr()).add(
                                iRowSum));
                    } else {
                        iSums.put(iSupplierCreditInvoice.getCreditingNr(), iRowSum);
                    }
                }
            }
        }
        return iSums;
    }

    public static Map<String, Integer> getStockInfluencing(List<SSSupplierCreditInvoice> iSupplierCreditInvoices) {
        Map<String, Integer> iSupplierCreditInvoiceCount = new HashMap<>();
        List<String> iParcelProducts = new LinkedList<>();
        List<SSProduct> iProducts = new LinkedList<>(
                se.swedsoft.bookkeeping.data.system.SSProductContext.getProducts());

        for (SSProduct iProduct : iProducts) {
            if (iProduct.isParcel() && iProduct.getNumber() != null) {
                iParcelProducts.add(iProduct.getNumber());
            }
        }
        for (SSSupplierCreditInvoice iSupplierCreditInvoice : iSupplierCreditInvoices) {
            for (SSSupplierInvoiceRow iRow : iSupplierCreditInvoice.getRows()) {
                if (iRow.getQuantity() == null) {
                    continue;
                }
                Integer iReserved;

                if (iParcelProducts.contains(iRow.getProductNr())) {
                    SSProduct iProduct = iRow.getProduct();

                    if (iProduct != null) {
                        for (SSProductRow iProductRow : iProduct.getParcelRows()) {
                            iReserved = iSupplierCreditInvoiceCount.get(
                                    iProductRow.getProductNr())
                                            == null
                                                    ? iProductRow.getQuantity()
                                                            * iRow.getQuantity()
                                                            : iSupplierCreditInvoiceCount.get(
                                                                    iProductRow.getProductNr())
                                                                            + (iProductRow.getQuantity()
                                                                                    * iRow.getQuantity());
                            iSupplierCreditInvoiceCount.put(iProductRow.getProductNr(),
                                    iReserved);
                        }
                    }
                } else {
                    iReserved = iSupplierCreditInvoiceCount.get(iRow.getProductNr())
                            == null
                                    ? iRow.getQuantity()
                                    : iSupplierCreditInvoiceCount.get(iRow.getProductNr())
                                            + iRow.getQuantity();
                    iSupplierCreditInvoiceCount.put(iRow.getProductNr(), iReserved);
                }
            }
        }
        return iSupplierCreditInvoiceCount;
    }

}
