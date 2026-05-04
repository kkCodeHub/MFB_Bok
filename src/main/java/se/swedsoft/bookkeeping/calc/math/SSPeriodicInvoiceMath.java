package se.swedsoft.bookkeeping.calc.math;


import se.swedsoft.bookkeeping.data.SSInvoice;
import se.swedsoft.bookkeeping.data.SSPeriodicInvoice;
import se.swedsoft.bookkeeping.data.system.SSDB;
import se.swedsoft.bookkeeping.util.SSDateUtil;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * User: Andreas Lago
 * Date: 2006-aug-14
 * Time: 09:53:50
 */
public class SSPeriodicInvoiceMath {
    private SSPeriodicInvoiceMath() {}

    /**
     * Returns a list of all new periodic invoices.
     * @return
     */
    public static Map<SSPeriodicInvoice, List<SSInvoice>> getPeriodicInvoices() {
        List<SSPeriodicInvoice> iPeriodicInvoices = SSDB.getInstance().getPeriodicInvoices();

        Map<SSPeriodicInvoice, List<SSInvoice>> iResult = new HashMap<>();

        LocalDate iNow = SSDateUtil.today();

        for (SSPeriodicInvoice iPeriodicInvoice : iPeriodicInvoices) {
            List<SSInvoice> iInvoices = iPeriodicInvoice.getInvoices(iNow);

            iResult.put(iPeriodicInvoice, iInvoices);
        }
        return iResult;

    }

    /**
     *
     * @return
     */
    public static boolean hasPendingPeriodicInvoices() {
        Map<SSPeriodicInvoice, List<SSInvoice>> iPeriodicInvoices = getPeriodicInvoices();

        for (Map.Entry<SSPeriodicInvoice, List<SSInvoice>> ssPeriodicInvoiceListEntry : iPeriodicInvoices.entrySet()) {
            List<SSInvoice> iInvoices = ssPeriodicInvoiceListEntry.getValue();

            if (!iInvoices.isEmpty()) {
                return true;
            }
        }
        return false;
    }

}
