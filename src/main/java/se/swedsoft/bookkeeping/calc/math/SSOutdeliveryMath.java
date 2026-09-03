package se.swedsoft.bookkeeping.calc.math;


import se.swedsoft.bookkeeping.data.SSOutdelivery;
import se.swedsoft.bookkeeping.data.SSOutdeliveryRow;
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
public class SSOutdeliveryMath {
    private SSOutdeliveryMath() {}

    /**
     * Returns true if the outdelivery's date is on or before pTo.
     *
     * @param iInventory the outdelivery to test
     * @param pTo        the upper bound
     * @return true if within the period
     */
    public static boolean inPeriod(SSOutdelivery iInventory, LocalDate pTo) {
        LocalDate iDate = iInventory.getLocalDate();

        return iDate != null && pTo != null && !iDate.isAfter(pTo);
    }

    /**
     * Returns true if the outdelivery's date falls within [pFrom, pTo] inclusive.
     *
     * @param iInventory the outdelivery to test
     * @param pFrom      the start of the period
     * @param pTo        the end of the period
     * @return true if within the period
     */
    public static boolean inPeriod(SSOutdelivery iInventory, LocalDate pFrom, LocalDate pTo) {
        LocalDate iDate = iInventory.getLocalDate();

        return iDate != null && pFrom != null && pTo != null
                && !iDate.isBefore(pFrom) && !iDate.isAfter(pTo);
    }

    /**
     *
     * @param iOutdelivery
     * @return
     */
    public static Integer getTotalCount(SSOutdelivery iOutdelivery) {
        Integer iCount = 0;

        for (SSOutdeliveryRow iRow : iOutdelivery.getRows()) {
            if (iRow.getChange() != null) {
                iCount = iCount + iRow.getChange();
            }

        }
        return iCount;
    }

    /**
     *
     * @param iOutdelivery
     * @param iProduct
     * @return
     */
    public static boolean hasProduct(SSOutdelivery iOutdelivery, SSProduct iProduct) {

        for (SSOutdeliveryRow iRow : iOutdelivery.getRows()) {
            if (iRow.hasProduct(iProduct)) {
                return true;
            }
        }
        return false;
    }

    public static Map<String, Integer> getStockInfluencing(List<SSOutdelivery> iOutdeliveries) {
        Map<String, Integer> iOutdeliveryCount = new HashMap<>();

        for (SSOutdelivery iOutdelivery : iOutdeliveries) {
            for (SSOutdeliveryRow iRow : iOutdelivery.getRows()) {
                if (iRow.getChange() == null) {
                    continue;
                }
                Integer iReserved = iOutdeliveryCount.get(iRow.getProductNr()) == null
                        ? iRow.getChange()
                        : iOutdeliveryCount.get(iRow.getProductNr()) + iRow.getChange();

                iOutdeliveryCount.put(iRow.getProductNr(), iReserved);
            }
        }
        return iOutdeliveryCount;
    }
}
