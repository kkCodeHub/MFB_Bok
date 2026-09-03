package se.swedsoft.bookkeeping.data.common;


/**
 * Reasons that lock invoice change/cancel operations.
 */
public enum SSInvoiceLockReason {
    PRINTED,
    ENTERED,
    CREDITED,
    PAID,
    CANCELLED
}
