package se.swedsoft.bookkeeping.persistence;

import se.swedsoft.bookkeeping.data.SSNewAccountingYear;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for {@link SSNewAccountingYear} persistence operations.
 */
public interface AccountingYearRepository {

    /**
     * Returns all accounting years for the current company.
     *
     * @return mutable list of accounting years; never {@code null}
     */
    List<SSNewAccountingYear> findAll();

    /**
     * Returns the currently active accounting year.
     *
     * @return active year, or empty when none is selected
     */
    Optional<SSNewAccountingYear> findCurrent();

    /**
     * Persists a new accounting year.
     *
     * @param year accounting year to add
     */
    void add(SSNewAccountingYear year);

    /**
     * Updates an existing accounting year.
     *
     * @param year accounting year to update
     */
    void update(SSNewAccountingYear year);

    /**
     * Deletes an existing accounting year.
     *
     * @param year accounting year to delete
     */
    void delete(SSNewAccountingYear year);
}

