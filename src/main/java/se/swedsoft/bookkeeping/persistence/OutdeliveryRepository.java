package se.swedsoft.bookkeeping.persistence;

import se.swedsoft.bookkeeping.data.SSOutdelivery;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for {@link SSOutdelivery} persistence operations.
 */
public interface OutdeliveryRepository {

    /**
     * Returns all outdeliveries for the current company.
     *
     * @return mutable list of outdeliveries; never {@code null}
     */
    List<SSOutdelivery> findAll();

    /**
     * Looks up an outdelivery by number in the current company.
     *
     * @param outdelivery reference outdelivery; must not be {@code null}
     * @return an {@link Optional} containing the stored outdelivery, or empty if not found
     */
    Optional<SSOutdelivery> findByOutdelivery(SSOutdelivery outdelivery);

    /**
     * Persists a new outdelivery for the current company.
     *
     * @param outdelivery the outdelivery to add; must not be {@code null}
     */
    void add(SSOutdelivery outdelivery);

    /**
     * Updates an existing outdelivery record.
     *
     * @param outdelivery the outdelivery with updated values; must not be {@code null}
     */
    void update(SSOutdelivery outdelivery);

    /**
     * Deletes an outdelivery from the current company.
     *
     * @param outdelivery the outdelivery to delete; must not be {@code null}
     */
    void delete(SSOutdelivery outdelivery);
}

