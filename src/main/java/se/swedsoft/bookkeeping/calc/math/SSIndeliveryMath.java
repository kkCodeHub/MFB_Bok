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
     *
     * @param iInventory
     * @param pTo
     * @return
     */
    public static boolean inPeriod(SSIndelivery iInventory, Date pTo) {
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
    public static boolean inPeriod(SSIndelivery iInventory, Date pFrom, Date pTo) {
        LocalDate iDate = iInventory.getLocalDate();
        LocalDate iFrom = SSDateUtil.toLocalDate(pFrom);
        LocalDate iTo = SSDateUtil.toLocalDate(pTo);

        return iDate != null && iFrom != null && iTo != null
                && !iDate.isBefore(iFrom) && !iDate.isAfter(iTo);
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
                Integer iReserved = iIndeliveryCount.get(iRow.getProductNr()) == null
                        ? iRow.getChange()
                        : iIndeliveryCount.get(iRow.getProductNr()) + iRow.getChange();

                iIndeliveryCount.put(iRow.getProductNr(), iReserved);
            }
        }
        return iIndeliveryCount;
    }

}
