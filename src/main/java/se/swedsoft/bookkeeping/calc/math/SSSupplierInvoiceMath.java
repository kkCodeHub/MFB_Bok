package se.swedsoft.bookkeeping.calc.math;


import se.swedsoft.bookkeeping.data.*;
import se.swedsoft.bookkeeping.data.system.SSDB;
import se.swedsoft.bookkeeping.util.SSDateUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.Optional;
import java.util.stream.Collectors;


/**
 * User: Andreas Lago
 * Date: 2006-jun-14
 * Time: 09:13:54
 */
public class SSSupplierInvoiceMath {
    private SSSupplierInvoiceMath() {}

    /**
     *
     * @param iSupplierInvoice
     * @param pFrom
     * @param pTo
     * @return
     */
    public static boolean inPeriod(SSSupplierInvoice iSupplierInvoice, Date pFrom, Date pTo) {
        return inPeriod(iSupplierInvoice, SSDateUtil.toLocalDate(pFrom), SSDateUtil.toLocalDate(pTo));
    }

    public static boolean inPeriod(SSSupplierInvoice iSupplierInvoice, LocalDate pFrom, LocalDate pTo) {
        LocalDate iDate = iSupplierInvoice.getLocalDate();

        return iDate != null && pFrom != null && pTo != null
                && !iDate.isBefore(pFrom) && !iDate.isAfter(pTo);
    }

    /**
     *
     * @param iSupplierInvoice
     * @param pTo
     * @return
     */
    public static boolean inPeriod(SSSupplierInvoice iSupplierInvoice, Date pTo) {
        return inPeriod(iSupplierInvoice, SSDateUtil.toLocalDate(pTo));
    }

    public static boolean inPeriod(SSSupplierInvoice iSupplierInvoice, LocalDate pTo) {
        LocalDate iDate = iSupplierInvoice.getLocalDate();

        return iDate != null && pTo != null && !iDate.isAfter(pTo);
    }

    /**
     * Convers a value from a sales currency to the company currency
     *
     * @param iSupplierInvoice the supplier invoice
     * @param iValue
     * @return the converted value
     */
    public static BigDecimal convertToLocal(SSSupplierInvoice iSupplierInvoice, BigDecimal iValue) {
        BigDecimal iCurrencyRate = iSupplierInvoice.getCurrencyRate();

        if (iCurrencyRate != null) {
            iValue = iValue.multiply(iCurrencyRate);
        }

        return iValue;
    }

    public static BigDecimal convertToLocal(Integer iSupplierInvoiceNr, BigDecimal iValue) {
        SSSupplierInvoice iSupplierInvoice = new SSSupplierInvoice();

        iSupplierInvoice.setNumber(iSupplierInvoiceNr);
        iSupplierInvoice = SSDB.getInstance().getSupplierInvoice(iSupplierInvoice).orElse(null);

        BigDecimal iCurrencyRate = iSupplierInvoice.getCurrencyRate();

        if (iCurrencyRate != null) {
            iValue = iValue.multiply(iCurrencyRate);
        }

        return iValue;
    }

    /**
     *
     * @param iInvoice
     * @return
     */
    public static boolean expired(SSSupplierInvoice iInvoice) {
        LocalDate dueDate = iInvoice.getLocalDueDate();

        return dueDate != null && SSDateUtil.today().isAfter(dueDate);

    }

    /**
     * Get the total sum for the supplier invoice.
     *
     * @param iSupplierInvoice
     * @return the total sum
     */
    public static BigDecimal getNetSum(SSSupplierInvoice iSupplierInvoice) {
        BigDecimal iTotalSum = new BigDecimal(0);

        for (SSSupplierInvoiceRow iRow : iSupplierInvoice.getRows()) {
            BigDecimal iRowSum = iRow.getSum().orElse(null);

            if (iRowSum != null) {
                iTotalSum = iTotalSum.add(iRowSum);
            }

        }
        return iTotalSum;
    }

    /**
     * Get the total sum for the supplier invoice.
     *
     * @param iSupplierInvoice
     * @return the total sum
     */
    public static BigDecimal getTotalSum(SSSupplierInvoice iSupplierInvoice) {
        BigDecimal iNetSum = getNetSum(iSupplierInvoice);
        BigDecimal iTaxSum = iSupplierInvoice.getTaxSum();
        BigDecimal iRoundingSum = iSupplierInvoice.getRoundingSum();

        return iTaxSum == null
                ? (iRoundingSum == null ? iNetSum : iNetSum.add(iRoundingSum))
                : (iRoundingSum == null
                        ? iNetSum.add(iTaxSum)
                        : iNetSum.add(iTaxSum).add(iRoundingSum));
    }

    /**
     *
     * @param iSupplierInvoice
     * @return
     */
    public static BigDecimal getSaldo(SSSupplierInvoice iSupplierInvoice) {

        BigDecimal iTotalSum = getTotalSum(iSupplierInvoice);
        BigDecimal iCredited = SSSupplierCreditInvoiceMath.getSumForInvoice(
                iSupplierInvoice);
        BigDecimal iPayed = SSOutpaymentMath.getSumForInvoice(iSupplierInvoice);

        return iTotalSum.subtract(iCredited).subtract(iPayed);
    }

    public static BigDecimal getSaldo(Integer iSupplierInvoiceNumber) {
        if (iSaldoMap.containsKey(iSupplierInvoiceNumber)) {
            return iSaldoMap.get(iSupplierInvoiceNumber);
        } else {
            return new BigDecimal(0);
        }
    }

    /*
     * Returns the partial saldo for the sales, in the sales currency up
     * to and including the selected date
     *
     * @param iInvoice
     * @param iDate The end date to calculate up to
     *
     * @return  the saldo
     */
    public static BigDecimal getSaldo(SSSupplierInvoice iInvoice, Date iDate) {
        return getSaldo(iInvoice, SSDateUtil.toLocalDate(iDate));
    }

    public static BigDecimal getSaldo(SSSupplierInvoice iInvoice, LocalDate iDate) {

        BigDecimal iTotalSum = getTotalSum(iInvoice);

        BigDecimal iCreditingSum = SSSupplierCreditInvoiceMath.getSumForInvoice(iInvoice,
                SSDateUtil.toDate(iDate));
        BigDecimal iInpaymentSum = SSOutpaymentMath.getSumForInvoice(iInvoice, SSDateUtil.toDate(iDate));

        iTotalSum = iTotalSum.subtract(iCreditingSum);
        iTotalSum = iTotalSum.subtract(iInpaymentSum);

        return iTotalSum;
    }

    public static HashMap<Integer, BigDecimal> iSaldoMap;

    public static void calculateSaldos() {
        if (iSaldoMap == null) {
            iSaldoMap = new HashMap<>();
        }
        HashMap<Integer, BigDecimal> iOutpaymentSum = SSOutpaymentMath.getSumsForSupplierInvoices();

        HashMap<Integer, BigDecimal> iSupplierCreditInvoiceSum = SSSupplierCreditInvoiceMath.getSumsForSupplierInvoices();

        List<SSSupplierInvoice> iSupplierInvoices = SSDB.getInstance().getSupplierInvoices();

        for (SSSupplierInvoice iSupplierInvoice : iSupplierInvoices) {
            BigDecimal iTotalSum = getTotalSum(iSupplierInvoice);

            if (iOutpaymentSum.containsKey(iSupplierInvoice.getNumber())) {
                iTotalSum = iTotalSum.subtract(
                        iOutpaymentSum.get(iSupplierInvoice.getNumber()));
            }

            if (iSupplierCreditInvoiceSum.containsKey(iSupplierInvoice.getNumber())) {
                iTotalSum = iTotalSum.subtract(
                        iSupplierCreditInvoiceSum.get(iSupplierInvoice.getNumber()));
            }

            iSaldoMap.put(iSupplierInvoice.getNumber(), iTotalSum);
        }
    }

    public static Map<Integer, BigDecimal> getSaldos(Date iDate) {
        return getSaldos(SSDateUtil.toLocalDate(iDate));
    }

    public static Map<Integer, BigDecimal> getSaldos(LocalDate iDate) {
        Map<Integer, BigDecimal> iSaldos = new HashMap<>();

        HashMap<Integer, BigDecimal> iOutpaymentSum = SSOutpaymentMath.getSumsForSupplierInvoices(
                SSDateUtil.toDate(iDate));

        HashMap<Integer, BigDecimal> iSupplierCreditInvoiceSum = SSSupplierCreditInvoiceMath.getSumsForSupplierInvoices(
                SSDateUtil.toDate(iDate));

        List<SSSupplierInvoice> iSupplierInvoices = SSDB.getInstance().getSupplierInvoices();

        for (SSSupplierInvoice iSupplierInvoice : iSupplierInvoices) {

            BigDecimal iTotalSum = getTotalSum(iSupplierInvoice);

            if (iOutpaymentSum.containsKey(iSupplierInvoice.getNumber())) {
                iTotalSum = iTotalSum.subtract(
                        iOutpaymentSum.get(iSupplierInvoice.getNumber()));
            }

            if (iSupplierCreditInvoiceSum.containsKey(iSupplierInvoice.getNumber())) {
                iTotalSum = iTotalSum.subtract(
                        iSupplierCreditInvoiceSum.get(iSupplierInvoice.getNumber()));
            }

            iSaldos.put(iSupplierInvoice.getNumber(), iTotalSum);
        }
        return iSaldos;
    }

    /**
     * Returns all invoices and saldos up to and including the specified date
     *
     * @param iInvoices The invoices
     * @param iDate The end date
     *
     * @return map of the invoices and their saldo
     */
    
    /* public static Map<SSSupplierInvoice, BigDecimal> getSaldo(List<SSSupplierInvoice> iInvoices, Date iDate) {
     Map<SSSupplierInvoice, BigDecimal> iSaldos = new HashMap<>();

     // Ceil the date so the before and after comparisions will be correct
     iDate = SSDateMath.ceil(iDate);

     // Loop through the invoices
     for (SSSupplierInvoice iInvoice : iInvoices) {
     Date iCurrent = iInvoice.getDate();

     // Only put invoices that is added before the specified date
     if( iCurrent.before(iDate)){
     BigDecimal iSaldo = getSaldo(iInvoice, iDate);

     iSaldos.put(iInvoice, iSaldo);
     }
     }
     return iSaldos;
     } */

    public static Map<SSSupplierInvoice, BigDecimal> getSaldo(List<SSSupplierInvoice> iInvoices, Date iDate) {
        return getSaldo(iInvoices, SSDateUtil.toLocalDate(iDate));
    }

    public static Map<SSSupplierInvoice, BigDecimal> getSaldo(List<SSSupplierInvoice> iInvoices, LocalDate iDate) {
        Map<SSSupplierInvoice, BigDecimal> iSaldos = new HashMap<>();

        HashMap<Integer, BigDecimal> iOutpaymentSum = SSOutpaymentMath.getSumsForSupplierInvoices(
                SSDateUtil.toDate(iDate));

        HashMap<Integer, BigDecimal> iSupplierCreditInvoiceSum = SSSupplierCreditInvoiceMath.getSumsForSupplierInvoices(
                SSDateUtil.toDate(iDate));

        // Loop through the invoices
        for (SSSupplierInvoice iInvoice : iInvoices) {
            LocalDate iCurrent = iInvoice.getLocalDate();

            // Only put invoices that is added before the specified date
            if (iCurrent != null && iDate != null && !iCurrent.isAfter(iDate)) {
                BigDecimal iSum = getTotalSum(iInvoice);

                if (iOutpaymentSum.containsKey(iInvoice.getNumber())) {
                    iSum = iSum.subtract(iOutpaymentSum.get(iInvoice.getNumber()));
                }

                if (iSupplierCreditInvoiceSum.containsKey(iInvoice.getNumber())) {
                    iSum = iSum.subtract(
                            iSupplierCreditInvoiceSum.get(iInvoice.getNumber()));
                }

                iSaldos.put(iInvoice, iSum.setScale(2, RoundingMode.HALF_UP));
                // BigDecimal iSaldo = getSaldo(iInvoice, iDate);
                // iSaldos.put(iInvoice, iSaldo.setScale(2, RoundingMode.HALF_UP));
            }
        }
        return iSaldos;
    }

    /**
     * Returns the sum of all saldos up to and including the specified date
     *
     * @param iInvoices The invoices
     * @param iDate The end date
     *
     * @return the saldo sum
     */
    public static BigDecimal getSaldoSum(List<SSSupplierInvoice> iInvoices, Date iDate) {
        return getSaldoSum(iInvoices, SSDateUtil.toLocalDate(iDate));
    }

    public static BigDecimal getSaldoSum(List<SSSupplierInvoice> iInvoices, LocalDate iDate) {
        Map<SSSupplierInvoice, BigDecimal> iSaldos = getSaldo(iInvoices, iDate);

        BigDecimal iSum = new BigDecimal(0);

        for (SSSupplierInvoice iInvoice : iInvoices) {
            iSum = iSum.add(iSaldos.get(iInvoice));
        }

        return iSum;
    }

    /**
     * Returns all invoices for the current supplier
     *
     * @param iSupplier
     * @return the invoices for the supplier
     */
    public static List<SSSupplierInvoice> getInvoicesForSupplier(SSSupplier iSupplier) {
        return getInvoicesForSupplier(SSDB.getInstance().getSupplierInvoices(), iSupplier);
    }

    /**
     * Returns all invoices for the current supplier
     *
     * @param iInvoices
     * @param iSupplier
     * @return the invoices for the customer
     */
    public static List<SSSupplierInvoice> getInvoicesForSupplier(List<SSSupplierInvoice> iInvoices, SSSupplier iSupplier) {
        return iInvoices.stream()
                .filter(iInvoice -> iInvoice.hasSupplier(iSupplier))
                .collect(Collectors.toList());
    }

    /**
     * Returns all invoices for the current supplier
     *
     * @param iSupplier
     * @param iDate
     * @return the invoices for the supplier
     */
    public static List<SSSupplierInvoice> getInvoicesForSupplier(SSSupplier iSupplier, Date iDate) {
        return getInvoicesForSupplier(iSupplier, SSDateUtil.toLocalDate(iDate));
    }

    public static List<SSSupplierInvoice> getInvoicesForSupplier(SSSupplier iSupplier, LocalDate iDate) {
        return getInvoicesForSupplier(SSDB.getInstance().getSupplierInvoices(), iSupplier,
                iDate);
    }

    /**
     * Returns all invoices for the current supplier
     *
     * @param iInvoices
     * @param iSupplier
     * @param iDate
     * @return the invoices for the customer
     */
    public static List<SSSupplierInvoice> getInvoicesForSupplier(List<SSSupplierInvoice> iInvoices, SSSupplier iSupplier, Date iDate) {
        return getInvoicesForSupplier(iInvoices, iSupplier, SSDateUtil.toLocalDate(iDate));
    }

    public static List<SSSupplierInvoice> getInvoicesForSupplier(List<SSSupplierInvoice> iInvoices, SSSupplier iSupplier, LocalDate iDate) {
        return iInvoices.stream()
                .filter(iInvoice -> iInvoice.hasSupplier(iSupplier) && inPeriod(iInvoice, iDate))
                .collect(Collectors.toList());
    }

    /**
     *
     * @param iSupplierInvoice
     * @param iProduct
     * @return
     */
    public static Integer getProductCount(SSSupplierInvoice iSupplierInvoice, SSProduct iProduct) {

        Integer iCount = 0;

        for (SSSupplierInvoiceRow iRow : iSupplierInvoice.getRows()) {
            SSProduct iRowProduct = iRow.getProduct();

            // Skip if no product or no quantity
            if (iRowProduct == null || iRow.getQuantity() == null) {
                continue;
            }

            // This is the product we want to get the quantity for
            if (iRowProduct.equals(iProduct)) {
                iCount = iCount + iRow.getQuantity();
            }
            // Get the quantity if this is a parcel product
            if (iRowProduct.isParcel()) {
                Integer iQuantity = SSProductMath.getProductCount(iRowProduct, iProduct);

                iCount = iCount + iRow.getQuantity() * iQuantity;
            }
        }
        return iCount;
    }

    /**
     *
     * @param iSupplierInvoices
     * @param iNumber
     * @return
     */
    public static Optional<SSSupplierInvoice> getSupplierInvoice(List<SSSupplierInvoice> iSupplierInvoices, int iNumber) {

        for (SSSupplierInvoice iSupplierInvoice : iSupplierInvoices) {

            if (iSupplierInvoice.getNumber() == iNumber) {
                return Optional.of(iSupplierInvoice);
            }
        }
        return Optional.empty();
    }

    /**
     *
     * @param iSupplierInvoices
     * @param iNumber
     * @return
     */
    public static Optional<SSSupplierInvoice> getSupplierInvoiceByNumber(List<SSSupplierInvoice> iSupplierInvoices, Integer iNumber) {

        for (SSSupplierInvoice iSupplierInvoice : iSupplierInvoices) {

            if (iSupplierInvoice.getNumber().equals(iNumber)) {
                return Optional.of(iSupplierInvoice);
            }
        }
        return Optional.empty();
    }

    /**
     *
     * @param iSupplierInvoices
     * @param iNumber
     * @return
     */
    public static Optional<SSSupplierInvoice> getSupplierInvoiceByReference(List<SSSupplierInvoice> iSupplierInvoices, String iNumber) {

        for (SSSupplierInvoice iSupplierInvoice : iSupplierInvoices) {

            String iReferencenumber = iSupplierInvoice.getReferencenumber();

            if (iReferencenumber.length() > 0 && iReferencenumber.equals(iNumber)) {
                return Optional.of(iSupplierInvoice);
            }
        }
        return Optional.empty();
    }

    /**
     *
     * @param iSupplierInvoice
     * @param iRow
     * @return
     */
    public static Optional<SSSupplierInvoiceRow> getMatchingRow(SSSupplierInvoice iSupplierInvoice, SSPurchaseOrderRow iRow) {

        String iProductNr = iRow.getProductNr();

        if (iProductNr == null) {
            return Optional.empty();
        }

        for (SSSupplierInvoiceRow iCurrent : iSupplierInvoice.getRows()) {

            if (iProductNr.equals(iCurrent.getProductNr())) {
                return Optional.of(iCurrent);
            }
        }
        return Optional.empty();

    }

    /**
     * Returns the invoices where the saldo different from zero
     *
     * @return list of invoices
     */
    public static List<SSSupplierInvoice> getNonPayedOrCreditedInvoices() {
        return getNonPayedOrCreditedInvoices(SSDB.getInstance().getSupplierInvoices());
    }

    /**
     * Returns the invoices where the saldo is different from zero
     *
     * @param iInvoices
     * @return list of invoices
     */
    public static List<SSSupplierInvoice> getNonPayedOrCreditedInvoices(List<SSSupplierInvoice> iInvoices) {
        List<SSSupplierInvoice> iFiltered = new LinkedList<>();

        for (SSSupplierInvoice iInvoice : iInvoices) {
            BigDecimal iSaldo = getSaldo(iInvoice.getNumber());

            if (iSaldo.signum() != 0) {
                iFiltered.add(iInvoice);
            }
        }
        return iFiltered;
    }

    public static Map<String, Integer> getStockInfluencing(List<SSSupplierInvoice> iSupplierInvoices) {
        Map<String, Integer> iSupplierInvoiceCount = new HashMap<>();
        List<String> iParcelProducts = new LinkedList<>();
        List<SSProduct> iProducts = new LinkedList<>(
                SSDB.getInstance().getProducts());

        for (SSProduct iProduct : iProducts) {
            if (iProduct.isParcel() && iProduct.getNumber() != null) {
                iParcelProducts.add(iProduct.getNumber());
            }
        }
        for (SSSupplierInvoice iSupplierInvoice : iSupplierInvoices) {
            for (SSSupplierInvoiceRow iRow : iSupplierInvoice.getRows()) {
                if (iRow.getQuantity() == null) {
                    continue;
                }
                Integer iReserved;

                if (iParcelProducts.contains(iRow.getProductNr())) {
                    SSProduct iProduct = iRow.getProduct();

                    if (iProduct != null) {
                        for (SSProductRow iProductRow : iProduct.getParcelRows()) {
                            iReserved = iSupplierInvoiceCount.get(
                                    iProductRow.getProductNr())
                                            == null
                                                    ? iProductRow.getQuantity()
                                                            * iRow.getQuantity()
                                                            : iSupplierInvoiceCount.get(
                                                                    iProductRow.getProductNr())
                                                                            + (iProductRow.getQuantity()
                                                                                    * iRow.getQuantity());
                            iSupplierInvoiceCount.put(iProductRow.getProductNr(),
                                    iReserved);
                        }
                    }
                } else {
                    iReserved = iSupplierInvoiceCount.get(iRow.getProductNr()) == null
                            ? iRow.getQuantity()
                            : iSupplierInvoiceCount.get(iRow.getProductNr())
                                    + iRow.getQuantity();
                    iSupplierInvoiceCount.put(iRow.getProductNr(), iReserved);
                }
            }
        }
        return iSupplierInvoiceCount;
    }

}
