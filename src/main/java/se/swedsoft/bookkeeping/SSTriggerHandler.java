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
                iNumber = getSpecialIdentifier(tabName, oldRow);
                iCompanyId = getSpecialCompanyId(oldRow);
            }

            if (newRow != null) {
                iNumber = getSpecialIdentifier(tabName, newRow);
                iCompanyId = getSpecialCompanyId(newRow);
            }
        } else {
            // "Normala objekt"
            if (oldRow != null) {
                iNumber = oldRow[1].toString();
                iCompanyId = getNormalCompanyId(oldRow);
            }

            if (newRow != null) {
                iNumber = newRow[1].toString();
                iCompanyId = getNormalCompanyId(newRow);
            }
        }

        if (iCompanyId != null && se.swedsoft.bookkeeping.data.system.SSCompanyYearContext.getCurrentCompany() != null) {

            if (iCompanyId.equals(se.swedsoft.bookkeeping.data.system.SSCompanyYearContext.getCurrentCompany().getId())
                    || (tabName != null && tabName.equals("TBL_VOUCHER")
                    && se.swedsoft.bookkeeping.data.system.SSCompanyYearContext.getCurrentYear() != null
                    && iCompanyId.equals(se.swedsoft.bookkeeping.data.system.SSCompanyYearContext.getCurrentYear().getId()))) {
                se.swedsoft.bookkeeping.data.system.SSEventTriggerSyncContext.triggerAction(trigName, tabName, iNumber);
            }
        }

    }

    private String getSpecialIdentifier(String tabName, Object[] row) {
        if (row == null) {
            return null;
        }
        if ("TBL_VOUCHERTEMPLATE".equals(tabName) && row.length > 1 && row[1] != null) {
            return row[1].toString();
        }
        return row[0] == null ? null : row[0].toString();
    }

    private Integer getSpecialCompanyId(Object[] row) {
        if (row == null) {
            return null;
        }
        if (row.length > 1 && row[1] instanceof Integer) {
            return (Integer) row[1];
        }
        if (row.length > 2 && row[2] instanceof Integer) {
            return (Integer) row[2];
        }
        return null;
    }

    private Integer getNormalCompanyId(Object[] row) {
        if (row == null) {
            return null;
        }
        if (row.length > 2 && row[2] instanceof Integer) {
            return (Integer) row[2];
        }
        if (row.length > 3 && row[3] instanceof Integer) {
            return (Integer) row[3];
        }
        return null;
    }
}
