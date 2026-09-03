package se.swedsoft.bookkeeping.data.system;

import se.swedsoft.bookkeeping.data.SSInpayment;
import se.swedsoft.bookkeeping.data.SSOutpayment;
import se.swedsoft.bookkeeping.persistence.Repositories;

import java.util.List;
import java.util.Optional;

/**
 * Domain service for the Payments domain (domain #6 in the 12-domain target architecture).
 *
 * <p>Covers customer inpayments and supplier outpayments, including matching
 * and payment-status management.</p>
 *
 * <p>All write operations delegate to the repository layer; no business logic
 * resides in this class.</p>
 */
public final class SSPaymentContext {

    private SSPaymentContext() {}

    // -------------------------------------------------------------------------
    // Inpayment CRUD
    // -------------------------------------------------------------------------

    /**
     * Returns all inpayments for the current company.
     *
     * @return list of inpayments; never {@code null}
     */
    public static List<SSInpayment> getInpayments() {
        return Repositories.inpayments().findAll();
    }

    /**
     * Looks up an inpayment by reference.
     *
     * @param pInpayment reference inpayment; must not be {@code null}
     * @return an {@link Optional} containing the inpayment, or empty if not found
     */
    public static Optional<SSInpayment> getInpayment(SSInpayment pInpayment) {
        return Repositories.inpayments().findByInpayment(pInpayment);
    }

    /**
     * Persists a new inpayment.
     *
     * @param pInpayment the inpayment to add; must not be {@code null}
     */
    public static void addInpayment(SSInpayment pInpayment) {
        Repositories.inpayments().add(pInpayment);
    }

    /**
     * Updates an existing inpayment.
     *
     * @param pInpayment the inpayment with updated values; must not be {@code null}
     */
    public static void updateInpayment(SSInpayment pInpayment) {
        Repositories.inpayments().update(pInpayment);
    }

    /**
     * Deletes an inpayment.
     *
     * @param pInpayment the inpayment to delete; must not be {@code null}
     */
    public static void deleteInpayment(SSInpayment pInpayment) {
        Repositories.inpayments().delete(pInpayment);
    }

    // -------------------------------------------------------------------------
    // Outpayment CRUD
    // -------------------------------------------------------------------------

    /**
     * Returns all outpayments for the current company.
     *
     * @return list of outpayments; never {@code null}
     */
    public static List<SSOutpayment> getOutpayments() {
        return Repositories.outpayments().findAll();
    }

    /**
     * Looks up an outpayment by reference.
     *
     * @param pOutpayment reference outpayment; must not be {@code null}
     * @return an {@link Optional} containing the outpayment, or empty if not found
     */
    public static Optional<SSOutpayment> getOutpayment(SSOutpayment pOutpayment) {
        return Repositories.outpayments().findByOutpayment(pOutpayment);
    }

    /**
     * Persists a new outpayment.
     *
     * @param pOutpayment the outpayment to add; must not be {@code null}
     */
    public static void addOutpayment(SSOutpayment pOutpayment) {
        Repositories.outpayments().add(pOutpayment);
    }

    /**
     * Updates an existing outpayment.
     *
     * @param pOutpayment the outpayment with updated values; must not be {@code null}
     */
    public static void updateOutpayment(SSOutpayment pOutpayment) {
        Repositories.outpayments().update(pOutpayment);
    }

    /**
     * Deletes an outpayment.
     *
     * @param pOutpayment the outpayment to delete; must not be {@code null}
     */
    public static void deleteOutpayment(SSOutpayment pOutpayment) {
        Repositories.outpayments().delete(pOutpayment);
    }
}
