package se.swedsoft.bookkeeping.persistence;

import se.swedsoft.bookkeeping.data.common.SSPaymentTerm;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for {@link SSPaymentTerm} persistence operations.
 *
 * <p>Reference data (payment terms) is global — not per company.</p>
 */
public interface PaymentTermRepository {

    /**
     * Returns all payment terms.
     *
     * @return mutable list of payment terms; never {@code null}
     */
    List<SSPaymentTerm> findAll();

    /**
     * Looks up a payment term by its name.
     *
     * @param name the payment term name; must not be {@code null}
     * @return an {@link Optional} containing the payment term, or empty if not found
     */
    Optional<SSPaymentTerm> findByName(String name);

    /**
     * Persists a new payment term.
     *
     * @param paymentTerm the payment term to add; must not be {@code null}
     */
    void add(SSPaymentTerm paymentTerm);

    /**
     * Updates an existing payment term record.
     *
     * @param paymentTerm the payment term with updated values; must not be {@code null}
     */
    void update(SSPaymentTerm paymentTerm);

    /**
     * Deletes a payment term.
     *
     * @param paymentTerm the payment term to delete; must not be {@code null}
     */
    void delete(SSPaymentTerm paymentTerm);
}

