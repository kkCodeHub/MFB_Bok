package se.swedsoft.bookkeeping.calc.math;


import se.swedsoft.bookkeeping.data.SSIndelivery;
import se.swedsoft.bookkeeping.data.SSIndeliveryRow;
import se.swedsoft.bookkeeping.data.SSProduct;
import se.swedsoft.bookkeeping.util.SSDateUtil;

import java.time.LocalDate;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * User: Andreas Lago
 * Date: 2006-sep-22
 * Time: 16:34:16
 */
public class SSIndeliveryMath {
    private SSIndeliveryMath() {}

    /**
     * Returns true if the indelivery's date is on or before pTo.
     *
     * @param iInventory the indelivery to test
     * @param pTo        the upper bound
     * @return true if within the period
     */
    public static boolean inPeriod(SSIndelivery iInventory, LocalDate pTo) {
        LocalDate iDate = iInventory.getLocalDate();

        return iDate != null && pTo != null && !iDate.isAfter(pTo);
    }

    /**
     * Returns true if the indelivery's date falls within [pFrom, pTo] inclusive.
     *
     * @param iInventory the indelivery to test
     * @param pFrom      the start of the period
     * @param pTo        the end of the period
     * @return true if within the period
     */
    public static boolean inPeriod(SSIndelivery iInventory, LocalDate pFrom, LocalDate pTo) {
        LocalDate iDate = iInventory.getLocalDate();

        return iDate != null && pFrom != null && pTo != null
                && !iDate.isBefore(pFrom) && !iDate.isAfter(pTo);
    }

    /**
     *
     * @param iIndelivery
     * @return
     */
    public static Integer getTotalCount(SSIndelivery iIndelivery) {
        Integer iCount = 0;

        for (SSIndeliveryRow iRow : iIndelivery.getRows()) {
            if (iRow.getChange() != null) {
                iCount = iCount + iRow.getChange();
            }

        }
        return iCount;
    }

    /**
     *
     * @param iIndelivery
     * @param iProduct
     * @return
     */
    public static boolean hasProduct(SSIndelivery iIndelivery, SSProduct iProduct) {

        for (SSIndeliveryRow iRow : iIndelivery.getRows()) {
            if (iRow.hasProduct(iProduct)) {
                return true;
            }
        }
        return false;
    }

    public static Map<String, Integer> getStockInfluencing(List<SSIndelivery> iIndeliveries) {
        Map<String, Integer> iIndeliveryCount = new HashMap<>();

        for (SSIndelivery iIndelivery : iIndeliveries) {
            for (SSIndeliveryRow iRow : iIndelivery.getRows()) {
                if (iRow.getChange() == null) {
                    continue;
                }
                String iProductNr = iRow.getProductNr();
                if (iProductNr == null && iRow.getProduct() != null) {
                    iProductNr = iRow.getProduct().getNumber();
                }
                if (iProductNr == null) {
                    continue;
                }
                Integer iReserved = iIndeliveryCount.get(iProductNr) == null
                        ? iRow.getChange()
                        : iIndeliveryCount.get(iProductNr) + iRow.getChange();

                iIndeliveryCount.put(iProductNr, iReserved);
            }
        }
        return iIndeliveryCount;
    }

}
