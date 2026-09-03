package se.swedsoft.bookkeeping.calc.math;


import se.swedsoft.bookkeeping.data.SSInventory;
import se.swedsoft.bookkeeping.data.SSInventoryRow;
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
public class SSInventoryMath {
    private SSInventoryMath() {}

    /**
     * Returns true if the inventory's date is on or before pTo.
     *
     * @param iInventory the inventory to test
     * @param pTo        the upper bound
     * @return true if within the period
     */
    public static boolean inPeriod(SSInventory iInventory, LocalDate pTo) {
        LocalDate iDate = iInventory.getLocalDate();

        return iDate != null && pTo != null && !iDate.isAfter(pTo);
    }

    /**
     * Returns true if the inventory's date falls within [pFrom, pTo] inclusive.
     *
     * @param iInventory the inventory to test
     * @param pFrom      the start of the period
     * @param pTo        the end of the period
     * @return true if within the period
     */
    public static boolean inPeriod(SSInventory iInventory, LocalDate pFrom, LocalDate pTo) {
        LocalDate iDate = iInventory.getLocalDate();

        return iDate != null && pFrom != null && pTo != null
                && !iDate.isBefore(pFrom) && !iDate.isAfter(pTo);
    }

    /**
     *
     * @param iInventory
     * @param iProduct
     * @return
     */
    public static boolean hasProduct(SSInventory iInventory, SSProduct iProduct) {

        for (SSInventoryRow iRow : iInventory.getRows()) {
            if (iRow.hasProduct(iProduct)) {
                return true;
            }
        }
        return false;
    }

    public static Map<String, Integer> getStockInfluencing(List<SSInventory> iInventories) {
        Map<String, Integer> iInventoryCount = new HashMap<>();

        for (SSInventory iInventory : iInventories) {
            for (SSInventoryRow iRow : iInventory.getRows()) {
                if (iRow.getChange() == null) {
                    continue;
                }
                Integer iReserved = iInventoryCount.get(iRow.getProductNr()) == null
                        ? iRow.getChange()
                        : iInventoryCount.get(iRow.getProductNr()) + iRow.getChange();

                iInventoryCount.put(iRow.getProductNr(), iReserved);
            }
        }
        return iInventoryCount;
    }

}
