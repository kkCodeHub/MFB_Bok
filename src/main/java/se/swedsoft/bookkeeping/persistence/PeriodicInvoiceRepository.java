package se.swedsoft.bookkeeping.persistence;

import se.swedsoft.bookkeeping.data.SSPeriodicInvoice;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for {@link SSPeriodicInvoice} persistence operations.
 */
public interface PeriodicInvoiceRepository {

    /**
     * Returns all periodic invoices for the current company.
     *
     * @return mutable list of periodic invoices; never {@code null}
     */
    List<SSPeriodicInvoice> findAll();

    /**
     * Looks up a periodic invoice by number in the current company.
     *
     * @param periodicInvoice reference periodic invoice; must not be {@code null}
     * @return an {@link Optional} containing the stored periodic invoice, or empty if not found
     */
    Optional<SSPeriodicInvoice> findByPeriodicInvoice(SSPeriodicInvoice periodicInvoice);

    /**
     * Persists a new periodic invoice for the current company.
     *
     * @param periodicInvoice the periodic invoice to add; must not be {@code null}
     */
    void add(SSPeriodicInvoice periodicInvoice);

    /**
     * Updates an existing periodic invoice record.
     *
     * @param periodicInvoice the periodic invoice with updated values; must not be {@code null}
     */
    void update(SSPeriodicInvoice periodicInvoice);

    /**
     * Deletes a periodic invoice from the current company.
     *
     * @param periodicInvoice the periodic invoice to delete; must not be {@code null}
     */
    void delete(SSPeriodicInvoice periodicInvoice);
}

