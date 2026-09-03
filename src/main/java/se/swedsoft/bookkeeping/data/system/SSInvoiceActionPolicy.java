package se.swedsoft.bookkeeping.data.system;

import se.swedsoft.bookkeeping.calc.math.SSCreditInvoiceMath;
import se.swedsoft.bookkeeping.calc.math.SSInpaymentMath;
import se.swedsoft.bookkeeping.data.SSInvoice;
import se.swedsoft.bookkeeping.data.common.SSInvoiceLockReason;

import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.List;

import static se.swedsoft.bookkeeping.util.SSUtil.verifyNotNull;

/**
 * Central policy for invoice lifecycle actions.
 *
 * <p>This class is the single source of truth for status-driven permissions such as:
 * edit, cancel, physical delete, uncancel, print/e-mail, journal, reminder and interest selection.</p>
 *
 * <p>Rule model in short:</p>
 * <ul>
 *   <li>Hard locks: printed, entered, credited, paid.</li>
 *   <li>Soft-delete status: cancelled.</li>
 *   <li>Physical delete: allowed only for highest invoice number and only when cancel would be allowed.</li>
 *   <li>Uncancel: allowed only for cancelled invoice that is currently highest number.</li>
 * </ul>
 */
public final class SSInvoiceActionPolicy {
    private SSInvoiceActionPolicy() {}

    /**
     * Returns all current lock reasons for an invoice.
     *
     * @param pInvoice invoice to evaluate
     * @return lock reasons, empty when invoice is fully editable
     */
    public static EnumSet<SSInvoiceLockReason> getLockReasons(SSInvoice pInvoice) {
        verifyNotNull("invoice", pInvoice);

        EnumSet<SSInvoiceLockReason> iReasons = getHardLockReasons(pInvoice);
        if (pInvoice.isCancelled()) {
            iReasons.add(SSInvoiceLockReason.CANCELLED);
        }
        return iReasons;
    }

    /**
     * Returns true when the invoice can be changed.
     *
     * @param pInvoice invoice to evaluate
     * @return true if editable
     */
    public static boolean canEdit(SSInvoice pInvoice) {
        return getLockReasons(pInvoice).isEmpty();
    }

    /**
     * Returns true when the invoice can be cancelled.
     *
     * @param pInvoice invoice to evaluate
     * @return true if cancel is allowed
     */
    public static boolean canCancel(SSInvoice pInvoice) {
        verifyNotNull("invoice", pInvoice);
        return !pInvoice.isCancelled() && getHardLockReasons(pInvoice).isEmpty();
    }

    /**
     * Returns true when the invoice can be physically deleted.
     *
     * @param pInvoice                invoice to evaluate
     * @param pIsHighestInvoiceNumber true only when invoice is highest number in series
     * @return true if physical delete is allowed
     */
    public static boolean canDeletePhysically(SSInvoice pInvoice, boolean pIsHighestInvoiceNumber) {
        verifyNotNull("invoice", pInvoice);
        return pIsHighestInvoiceNumber && canCancel(pInvoice);
    }

    /**
     * Returns true when the invoice is currently highest number in the supplied series.
     *
     * @param pInvoice  invoice to evaluate
     * @param pInvoices invoice series to evaluate against
     * @return true if invoice has highest number in series
     */
    public static boolean isHighestInvoiceNumber(SSInvoice pInvoice, List<SSInvoice> pInvoices) {
        verifyNotNull("invoice", pInvoice);
        verifyNotNull("invoices", pInvoices);

        Integer iInvoiceNumber = pInvoice.getNumber();
        if (iInvoiceNumber == null) {
            return false;
        }

        Integer iHighestNumber = null;
        for (SSInvoice iCurrent : pInvoices) {
            Integer iCurrentNumber = iCurrent.getNumber();
            if (iCurrentNumber != null && (iHighestNumber == null || iCurrentNumber > iHighestNumber)) {
                iHighestNumber = iCurrentNumber;
            }
        }
        return iInvoiceNumber.equals(iHighestNumber);
    }

    /**
     * Returns true when the invoice can be physically deleted in current series.
     *
     * @param pInvoice  invoice to evaluate
     * @param pInvoices full invoice series for current company
     * @return true if physical delete is allowed
     */
    public static boolean canDeletePhysically(SSInvoice pInvoice, List<SSInvoice> pInvoices) {
        return canDeletePhysically(pInvoice, isHighestInvoiceNumber(pInvoice, pInvoices));
    }

    /**
     * Returns true when a cancelled invoice can be uncancelled.
     *
     * @param pInvoice                invoice to evaluate
     * @param pIsHighestInvoiceNumber true only when invoice is highest number in series
     * @return true if uncancel is allowed
     */
    public static boolean canUncancel(SSInvoice pInvoice, boolean pIsHighestInvoiceNumber) {
        verifyNotNull("invoice", pInvoice);
        return pInvoice.isCancelled() && pIsHighestInvoiceNumber;
    }

    /**
     * Returns true when a cancelled invoice can be uncancelled in current series.
     *
     * @param pInvoice  invoice to evaluate
     * @param pInvoices full invoice series for current company
     * @return true if uncancel is allowed
     */
    public static boolean canUncancel(SSInvoice pInvoice, List<SSInvoice> pInvoices) {
        return canUncancel(pInvoice, isHighestInvoiceNumber(pInvoice, pInvoices));
    }

    /**
     * Returns true when the invoice can be printed.
     *
     * @param pInvoice invoice to evaluate
     * @return true if print is allowed
     */
    public static boolean canPrint(SSInvoice pInvoice) {
        verifyNotNull("invoice", pInvoice);
        return !pInvoice.isCancelled();
    }

    /**
     * Returns true when the invoice can be sent by e-mail.
     *
     * @param pInvoice invoice to evaluate
     * @return true if e-mail send is allowed
     */
    public static boolean canSendByEmail(SSInvoice pInvoice) {
        return canPrint(pInvoice);
    }

    /**
     * Returns true when the invoice can be included in posting/journal flow.
     *
     * @param pInvoice invoice to evaluate
     * @return true if posting is allowed
     */
    public static boolean canPostToJournal(SSInvoice pInvoice) {
        verifyNotNull("invoice", pInvoice);
        return !pInvoice.isCancelled() && !pInvoice.isEntered();
    }

    /**
     * Returns true when the invoice can be selected for reminder flow.
     *
     * @param pInvoice invoice to evaluate
     * @return true if reminder selection is allowed
     */
    public static boolean canSelectForReminder(SSInvoice pInvoice) {
        return canPrint(pInvoice);
    }

    /**
     * Returns true when the invoice can be selected for interest invoice flow.
     *
     * @param pInvoice invoice to evaluate
     * @return true if interest flow selection is allowed
     */
    public static boolean canSelectForInterestInvoicing(SSInvoice pInvoice) {
        return canPrint(pInvoice);
    }

    /**
     * Returns true when the invoice can be used in inpayment flow.
     *
     * @param pInvoice invoice to evaluate
     * @return true if inpayment is allowed
     */
    public static boolean canRegisterInpayment(SSInvoice pInvoice) {
        return canPrint(pInvoice);
    }

    /**
     * Returns true when the invoice can be used in credit invoice flow.
     *
     * @param pInvoice invoice to evaluate
     * @return true if crediting is allowed
     */
    public static boolean canCreateCreditInvoice(SSInvoice pInvoice) {
        return canPrint(pInvoice);
    }

    private static EnumSet<SSInvoiceLockReason> getHardLockReasons(SSInvoice pInvoice) {
        EnumSet<SSInvoiceLockReason> iReasons = EnumSet.noneOf(SSInvoiceLockReason.class);

        if (pInvoice.isPrinted()) {
            iReasons.add(SSInvoiceLockReason.PRINTED);
        }
        if (pInvoice.isEntered()) {
            iReasons.add(SSInvoiceLockReason.ENTERED);
        }
        if (hasPositiveValue(SSCreditInvoiceMath.getSumForInvoice(pInvoice))) {
            iReasons.add(SSInvoiceLockReason.CREDITED);
        }
        if (hasPositiveValue(SSInpaymentMath.getSumForInvoice(pInvoice))) {
            iReasons.add(SSInvoiceLockReason.PAID);
        }

        return iReasons;
    }

    private static boolean hasPositiveValue(BigDecimal pValue) {
        return pValue != null && pValue.signum() > 0;
    }
}
