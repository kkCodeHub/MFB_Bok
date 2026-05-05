package se.swedsoft.bookkeeping.persistence;

import se.swedsoft.bookkeeping.data.SSTender;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for {@link SSTender} persistence operations.
 */
public interface TenderRepository {

    /**
     * Returns all tenders for the current company.
     *
     * @return mutable list of tenders; never {@code null}
     */
    List<SSTender> findAll();

    /**
     * Looks up a tender by number in the current company.
     *
     * @param tender reference tender; must not be {@code null}
     * @return an {@link Optional} containing the stored tender, or empty if not found
     */
    Optional<SSTender> findByTender(SSTender tender);

    /**
     * Returns only those tenders from {@code subset} that exist in the repository.
     *
     * @param subset candidate list; must not be {@code null}
     * @return filtered list; never {@code null}
     */
    List<SSTender> findAll(List<SSTender> subset);

    /**
     * Persists a new tender for the current company.
     *
     * @param tender the tender to add; must not be {@code null}
     */
    void add(SSTender tender);

    /**
     * Updates an existing tender record.
     *
     * @param tender the tender with updated values; must not be {@code null}
     */
    void update(SSTender tender);

    /**
     * Deletes a tender from the current company.
     *
     * @param tender the tender to delete; must not be {@code null}
     */
    void delete(SSTender tender);
}

