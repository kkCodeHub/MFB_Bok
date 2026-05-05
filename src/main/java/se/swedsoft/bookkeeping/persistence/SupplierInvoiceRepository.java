package se.swedsoft.bookkeeping.persistence;

import se.swedsoft.bookkeeping.data.SSSupplierInvoice;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for {@link SSSupplierInvoice} persistence operations.
 */
public interface SupplierInvoiceRepository {

    /**
     * Returns all supplier invoices for the current company.
     *
     * @return mutable list of supplier invoices; never {@code null}
     */
    List<SSSupplierInvoice> findAll();

    /**
     * Looks up a supplier invoice by number in the current company.
     *
     * @param supplierInvoice reference supplier invoice; must not be {@code null}
     * @return an {@link Optional} containing the stored invoice, or empty if not found
     */
    Optional<SSSupplierInvoice> findBySupplierInvoice(SSSupplierInvoice supplierInvoice);

    /**
     * Returns only those supplier invoices from {@code subset} that exist in the repository.
     *
     * @param subset candidate list; must not be {@code null}
     * @return filtered list; never {@code null}
     */
    List<SSSupplierInvoice> findAll(List<SSSupplierInvoice> subset);

    /**
     * Persists a new supplier invoice for the current company.
     *
     * @param supplierInvoice the supplier invoice to add; must not be {@code null}
     */
    void add(SSSupplierInvoice supplierInvoice);

    /**
     * Updates an existing supplier invoice record.
     *
     * @param supplierInvoice the supplier invoice with updated values; must not be {@code null}
     */
    void update(SSSupplierInvoice supplierInvoice);

    /**
     * Deletes a supplier invoice from the current company.
     *
     * @param supplierInvoice the supplier invoice to delete; must not be {@code null}
     */
    void delete(SSSupplierInvoice supplierInvoice);
}

