package se.swedsoft.bookkeeping.data.system;

import se.swedsoft.bookkeeping.calc.math.SSOutpaymentMath;
import se.swedsoft.bookkeeping.calc.math.SSSupplierCreditInvoiceMath;
import se.swedsoft.bookkeeping.data.SSSupplierCreditInvoice;
import se.swedsoft.bookkeeping.data.SSSupplierInvoice;

import java.math.BigDecimal;
import java.util.List;

import static se.swedsoft.bookkeeping.util.SSUtil.verifyNotNull;

/**
 * Central policy for supplier invoice delete actions.
 */
public final class SSSupplierInvoiceActionPolicy {
    private SSSupplierInvoiceActionPolicy() {}

    /**
     * Returns true when the supplier invoice can be cancelled.
     *
     * @param pInvoice invoice to evaluate
     * @return true if cancel is allowed
     */
    public static boolean canCancel(SSSupplierInvoice pInvoice) {
        verifyNotNull("invoice", pInvoice);
        return !pInvoice.isCancelled() && canDelete(pInvoice);
    }

    /**
     * Returns true when the supplier credit invoice can be cancelled.
     *
     * @param pInvoice invoice to evaluate
     * @return true if cancel is allowed
     */
    public static boolean canCancel(SSSupplierCreditInvoice pInvoice) {
        verifyNotNull("invoice", pInvoice);
        return !pInvoice.isCancelled() && canDelete(pInvoice);
    }

    /**
     * Returns true when a cancelled supplier invoice can be uncancelled.
     *
     * @param pInvoice invoice to evaluate
     * @param pInvoices full supplier invoice series for current company
     * @return true if uncancel is allowed
     */
    public static boolean canUncancel(SSSupplierInvoice pInvoice, List<? extends SSSupplierInvoice> pInvoices) {
        verifyNotNull("invoice", pInvoice);
        return pInvoice.isCancelled() && isHighestSupplierInvoiceNumber(pInvoice, pInvoices);
    }

    /**
     * Returns true when a cancelled supplier credit invoice can be uncancelled.
     *
     * @param pInvoice invoice to evaluate
     * @param pInvoices full supplier credit invoice series for current company
     * @return true if uncancel is allowed
     */
    public static boolean canUncancel(SSSupplierCreditInvoice pInvoice,
                                      List<? extends SSSupplierCreditInvoice> pInvoices) {
        verifyNotNull("invoice", pInvoice);
        return pInvoice.isCancelled() && isHighestSupplierInvoiceNumber(pInvoice, pInvoices);
    }

    /**
     * Returns true when the supplier invoice can be physically deleted.
     *
     * @param pInvoice invoice to evaluate
     * @param pInvoices full supplier invoice series for current company
     * @return true if physical delete is allowed
     */
    public static boolean canDeletePhysically(SSSupplierInvoice pInvoice,
                                              List<? extends SSSupplierInvoice> pInvoices) {
        return canDeletePhysically(pInvoice, isHighestSupplierInvoiceNumber(pInvoice, pInvoices));
    }

    /**
     * Returns true when the supplier credit invoice can be physically deleted.
     *
     * @param pInvoice invoice to evaluate
     * @param pInvoices full supplier credit invoice series for current company
     * @return true if physical delete is allowed
     */
    public static boolean canDeletePhysically(SSSupplierCreditInvoice pInvoice,
                                              List<? extends SSSupplierCreditInvoice> pInvoices) {
        return canDeletePhysically(pInvoice, isHighestSupplierInvoiceNumber(pInvoice, pInvoices));
    }

    /**
     * Returns true when the supplier invoice can be physically deleted.
     *
     * @param pInvoice invoice to evaluate
     * @param pIsHighestInvoiceNumber true only when invoice is highest number in series
     * @return true if physical delete is allowed
     */
    public static boolean canDeletePhysically(SSSupplierInvoice pInvoice, boolean pIsHighestInvoiceNumber) {
        verifyNotNull("invoice", pInvoice);
        return pIsHighestInvoiceNumber && canCancel(pInvoice);
    }

    /**
     * Returns true when the supplier credit invoice can be physically deleted.
     *
     * @param pInvoice invoice to evaluate
     * @param pIsHighestInvoiceNumber true only when invoice is highest number in series
     * @return true if physical delete is allowed
     */
    public static boolean canDeletePhysically(SSSupplierCreditInvoice pInvoice,
                                              boolean pIsHighestInvoiceNumber) {
        verifyNotNull("invoice", pInvoice);
        return pIsHighestInvoiceNumber && canCancel(pInvoice);
    }

    /**
     * Returns true when the invoice is currently highest number in the supplied series.
     *
     * @param pInvoice invoice to evaluate
     * @param pInvoices invoice series to evaluate against
     * @return true if invoice has highest number in series
     */
    public static boolean isHighestSupplierInvoiceNumber(SSSupplierInvoice pInvoice,
                                                         List<? extends SSSupplierInvoice> pInvoices) {
        verifyNotNull("invoice", pInvoice);
        verifyNotNull("invoices", pInvoices);

        Integer iInvoiceNumber = pInvoice.getNumber();
        if (iInvoiceNumber == null) {
            return false;
        }

        Integer iHighestNumber = null;
        for (SSSupplierInvoice iCurrent : pInvoices) {
            Integer iCurrentNumber = iCurrent.getNumber();
            if (iCurrentNumber != null && (iHighestNumber == null || iCurrentNumber > iHighestNumber)) {
                iHighestNumber = iCurrentNumber;
            }
        }
        return iInvoiceNumber.equals(iHighestNumber);
    }

    /**
     * Returns true when the supplier invoice can be used to create a supplier credit invoice.
     *
     * @param pInvoice invoice to evaluate
     * @return true if supplier credit creation is allowed
     */
    public static boolean canCreateCreditInvoice(SSSupplierInvoice pInvoice) {
        verifyNotNull("invoice", pInvoice);
        return !pInvoice.isCancelled();
    }

    private static boolean canDelete(SSSupplierInvoice pInvoice) {
        return !pInvoice.isEntered()
                && !hasPositiveValue(SSSupplierCreditInvoiceMath.getSumForInvoice(pInvoice))
                && !hasPositiveValue(SSOutpaymentMath.getSumForInvoice(pInvoice));
    }

    private static boolean canDelete(SSSupplierCreditInvoice pInvoice) {
        return !pInvoice.isEntered()
                && !hasPositiveValue(SSOutpaymentMath.getSumForInvoice(pInvoice));
    }

    private static boolean hasPositiveValue(BigDecimal pValue) {
        return pValue != null && pValue.signum() > 0;
    }
}
