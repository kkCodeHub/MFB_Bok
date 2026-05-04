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
     *
     * @param iInventory
     * @param pTo
     * @return
     */
    public static boolean inPeriod(SSOutdelivery iInventory, Date pTo) {
        LocalDate iDate = iInventory.getLocalDate();
        LocalDate iTo = SSDateUtil.toLocalDate(pTo);

        return iDate != null && iTo != null && !iDate.isAfter(iTo);

    }

    /**
     *
     * @param iInventory
     * @param pFrom
     * @param pTo
     * @return
     */
    public static boolean inPeriod(SSOutdelivery iInventory, Date pFrom, Date pTo) {
        LocalDate iDate = iInventory.getLocalDate();
        LocalDate iFrom = SSDateUtil.toLocalDate(pFrom);
        LocalDate iTo = SSDateUtil.toLocalDate(pTo);

        return iDate != null && iFrom != null && iTo != null
                && !iDate.isBefore(iFrom) && !iDate.isAfter(iTo);
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
