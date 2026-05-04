package se.swedsoft.bookkeeping;


import org.hsqldb.Trigger;
import se.swedsoft.bookkeeping.data.system.SSDB;


/**
 * HSQLDB trigger handler for the local/embedded database.
 *
 * <p>Implements {@link Trigger} so that HSQLDB can call {@link #fire} when an
 * {@code AFTER INSERT/UPDATE/DELETE} trigger fires.  The handler delegates to
 * {@link SSDB#triggerAction} which updates the in-memory entity lists and
 * refreshes any open GUI frames.</p>
 */
public class SSTriggerHandler implements Trigger {

    public void fire(int type, String trigName, String tabName, Object[] oldRow, Object[] newRow) {
        if (type == UPDATE_BEFORE_ROW) {
            oldRow = null;
        }
        String iNumber = null;
        Integer iCompanyId = null;

        if (trigName.contains("PROJECT") || trigName.contains("RESULTUNIT")
                || trigName.contains("VOUCHERTEMPLATE") || trigName.contains("OWNREPORT")) {
            if (oldRow != null) {
                iNumber = oldRow[0].toString();
                iCompanyId = (Integer) oldRow[2];
            }

            if (newRow != null) {
                iNumber = newRow[0].toString();
                iCompanyId = (Integer) newRow[2];
            }
        } else {
            // "Normala objekt"
            if (oldRow != null) {
                iNumber = oldRow[1].toString();
                iCompanyId = (Integer) oldRow[3];
            }

            if (newRow != null) {
                iNumber = newRow[1].toString();
                iCompanyId = (Integer) newRow[3];
            }
        }

        if (iCompanyId != null && SSDB.getInstance().getCurrentCompany() != null) {

            if (iCompanyId.equals(SSDB.getInstance().getCurrentCompany().getId())
                    || (tabName != null && tabName.equals("TBL_VOUCHER")
                    && SSDB.getInstance().getCurrentYear() != null
                    && iCompanyId.equals(SSDB.getInstance().getCurrentYear().getId()))) {
                SSDB.getInstance().triggerAction(trigName, tabName, iNumber);
            }
        }

    }
}
