package se.swedsoft.bookkeeping.persistence;

import se.swedsoft.bookkeeping.data.SSIndelivery;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for {@link SSIndelivery} persistence operations.
 */
public interface IndeliveryRepository {

    /**
     * Returns all indeliveries for the current company.
     *
     * @return mutable list of indeliveries; never {@code null}
     */
    List<SSIndelivery> findAll();

    /**
     * Looks up an indelivery by number in the current company.
     *
     * @param indelivery reference indelivery; must not be {@code null}
     * @return an {@link Optional} containing the stored indelivery, or empty if not found
     */
    Optional<SSIndelivery> findByIndelivery(SSIndelivery indelivery);

    /**
     * Persists a new indelivery for the current company.
     *
     * @param indelivery the indelivery to add; must not be {@code null}
     */
    void add(SSIndelivery indelivery);

    /**
     * Updates an existing indelivery record.
     *
     * @param indelivery the indelivery with updated values; must not be {@code null}
     */
    void update(SSIndelivery indelivery);

    /**
     * Deletes an indelivery from the current company.
     *
     * @param indelivery the indelivery to delete; must not be {@code null}
     */
    void delete(SSIndelivery indelivery);
}

