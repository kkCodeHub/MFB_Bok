package se.swedsoft.bookkeeping.persistence;

import se.swedsoft.bookkeeping.data.SSOutpayment;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for {@link SSOutpayment} persistence operations.
 */
public interface OutpaymentRepository {

    /**
     * Returns all outpayments for the current company.
     *
     * @return mutable list of outpayments; never {@code null}
     */
    List<SSOutpayment> findAll();

    /**
     * Looks up an outpayment by number in the current company.
     *
     * @param outpayment reference outpayment; must not be {@code null}
     * @return an {@link Optional} containing the stored outpayment, or empty if not found
     */
    Optional<SSOutpayment> findByOutpayment(SSOutpayment outpayment);

    /**
     * Persists a new outpayment for the current company.
     *
     * @param outpayment the outpayment to add; must not be {@code null}
     */
    void add(SSOutpayment outpayment);

    /**
     * Updates an existing outpayment record.
     *
     * @param outpayment the outpayment with updated values; must not be {@code null}
     */
    void update(SSOutpayment outpayment);

    /**
     * Deletes an outpayment from the current company.
     *
     * @param outpayment the outpayment to delete; must not be {@code null}
     */
    void delete(SSOutpayment outpayment);
}

