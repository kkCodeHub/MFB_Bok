package se.swedsoft.bookkeeping.data.system.trigger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.swedsoft.bookkeeping.data.system.SSDB;
import se.swedsoft.bookkeeping.data.system.SSTriggerRuntime;
import se.swedsoft.bookkeeping.gui.SSMainFrame;
import se.swedsoft.bookkeeping.gui.util.dialogs.SSErrorDialog;

import java.util.Arrays;
import java.util.List;

public final class SSEventTriggerDispatcher {

    private static final Logger LOG = LoggerFactory.getLogger(SSEventTriggerDispatcher.class);

    private final List<SSTriggerCategoryHandler> iHandlers;

    /**
     * Controls whether trigger dispatching is bypassed for the current thread.
     * Defaults to {@code false} — dispatching is active.
     */
    private final ThreadLocal<Boolean> iBypassDispatch = ThreadLocal.withInitial(() -> Boolean.FALSE);

    public SSEventTriggerDispatcher(SSDB pDatabase) {
        SSTriggerRuntime iRuntime = new SSTriggerRuntime(pDatabase);
        iHandlers = Arrays.asList(
                new MasterdataTriggerHandler(iRuntime),
                new SalesTriggerHandler(iRuntime),
                new CustomerPaymentTriggerHandler(iRuntime),
                new PurchaseSupplierTriggerHandler(iRuntime),
                new InventoryTriggerHandler(iRuntime),
                new AccountingTriggerHandler(iRuntime),
                new ReportTriggerHandler(iRuntime)
        );
    }

    /**
     * Routes a trigger event through all registered handlers.
     *
     * @return {@code true} if a handler claimed the trigger, {@code false} otherwise
     */
    public boolean dispatch(String pTriggerName, String pTableName, String pNumber) {
        for (SSTriggerCategoryHandler iHandler : iHandlers) {
            if (iHandler.handle(pTriggerName, pTableName, pNumber)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Full trigger-action entry point: respects bypass flag, shows error dialog on
     * unhandled triggers, and catches {@link NumberFormatException}.
     */
    public synchronized void dispatchAction(String pTriggerName, String pTableName, String pNumber) {
        try {
            if (!iBypassDispatch.get()) {
                if (dispatch(pTriggerName, pTableName, pNumber)) {
                    return;
                }
                LOG.error("Trigger '{}' could not be dispatched; showing error dialog to user", pTriggerName);
                SSErrorDialog.showDialog(
                        SSMainFrame.getInstance(),
                        "Systemfel: Datauppdatering misslyckades",
                        "Ändringar från databasen kunde inte läsas in för trigger: " + pTriggerName + "\n\n" +
                        "Vänligen stäng och öppna företaget igen för att uppdatera data från databasen."
                );
            }
        } catch (NumberFormatException e) {
            LOG.error("Trigger '{}' failed due to invalid number format '{}': {}",
                    pTriggerName, pNumber, e.getMessage(), e);
        }
    }
}


