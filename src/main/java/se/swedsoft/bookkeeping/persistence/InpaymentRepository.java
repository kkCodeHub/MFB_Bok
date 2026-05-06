package se.swedsoft.bookkeeping.persistence;

import se.swedsoft.bookkeeping.data.SSInpayment;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for {@link SSInpayment} persistence operations.
 */
public interface InpaymentRepository {

    /**
     * Returns all inpayments for the current company.
     *
     * @return mutable list of inpayments; never {@code null}
     */
    List<SSInpayment> findAll();

    /**
     * Looks up an inpayment by number in the current company.
     *
     * @param inpayment reference inpayment; must not be {@code null}
     * @return an {@link Optional} containing the stored inpayment, or empty if not found
     */
    Optional<SSInpayment> findByInpayment(SSInpayment inpayment);

    /**
     * Persists a new inpayment for the current company.
     *
     * @param inpayment the inpayment to add; must not be {@code null}
     */
    void add(SSInpayment inpayment);

    /**
     * Updates an existing inpayment record.
     *
     * @param inpayment the inpayment with updated values; must not be {@code null}
     */
    void update(SSInpayment inpayment);

    /**
     * Deletes an inpayment from the current company.
     *
     * @param inpayment the inpayment to delete; must not be {@code null}
     */
    void delete(SSInpayment inpayment);
}

