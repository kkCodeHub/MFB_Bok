package se.swedsoft.bookkeeping.persistence;

import se.swedsoft.bookkeeping.data.SSInvoice;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for {@link SSInvoice} persistence operations.
 */
public interface InvoiceRepository {

    /**
     * Returns all invoices for the current company.
     *
     * @return mutable list of invoices; never {@code null}
     */
    List<SSInvoice> findAll();

    /**
     * Looks up an invoice by number in the current company.
     *
     * @param invoice reference invoice; must not be {@code null}
     * @return an {@link Optional} containing the stored invoice, or empty if not found
     */
    Optional<SSInvoice> findByInvoice(SSInvoice invoice);

    /**
     * Returns only those invoices from {@code subset} that exist in the repository.
     *
     * @param subset candidate list; must not be {@code null}
     * @return filtered list; never {@code null}
     */
    List<SSInvoice> findAll(List<SSInvoice> subset);

    /**
     * Persists a new invoice for the current company.
     *
     * @param invoice the invoice to add; must not be {@code null}
     */
    void add(SSInvoice invoice);

    /**
     * Updates an existing invoice record.
     *
     * @param invoice the invoice with updated values; must not be {@code null}
     */
    void update(SSInvoice invoice);

    /**
     * Deletes an invoice from the current company.
     *
     * @param invoice the invoice to delete; must not be {@code null}
     */
    void delete(SSInvoice invoice);
}

