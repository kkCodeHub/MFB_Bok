package se.swedsoft.bookkeeping.persistence;

import se.swedsoft.bookkeeping.data.SSSupplier;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for {@link SSSupplier} persistence operations.
 *
 * <p>Implementations decouple the rest of the application from the
 * underlying storage mechanism (currently HSQLDB via {@code SSDB}).</p>
 */
public interface SupplierRepository {

    /**
     * Returns all suppliers for the current company.
     *
     * @return mutable list of suppliers; never {@code null}
     */
    List<SSSupplier> findAll();

    /**
     * Looks up a supplier by object identity (uses its supplier-number).
     *
     * @param supplier reference supplier; must not be {@code null}
     * @return an {@link Optional} containing the stored supplier, or empty if not found
     */
    Optional<SSSupplier> findBySupplier(SSSupplier supplier);

    /**
     * Returns only those suppliers from {@code subset} that exist in the repository.
     *
     * @param subset candidate list; must not be {@code null}
     * @return filtered list; never {@code null}
     */
    List<SSSupplier> findAll(List<SSSupplier> subset);

    /**
     * Persists a new supplier for the current company.
     *
     * @param supplier the supplier to add; must not be {@code null}
     */
    void add(SSSupplier supplier);

    /**
     * Updates an existing supplier record.
     *
     * @param supplier the supplier with updated values; must not be {@code null}
     */
    void update(SSSupplier supplier);

    /**
     * Deletes a supplier from the current company.
     *
     * @param supplier the supplier to delete; must not be {@code null}
     */
    void delete(SSSupplier supplier);
}


