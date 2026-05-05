package se.swedsoft.bookkeeping.persistence;

import se.swedsoft.bookkeeping.data.SSSupplierCreditInvoice;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for {@link SSSupplierCreditInvoice} persistence operations.
 */
public interface SupplierCreditInvoiceRepository {

    /**
     * Returns all supplier credit invoices for the current company.
     *
     * @return mutable list of supplier credit invoices; never {@code null}
     */
    List<SSSupplierCreditInvoice> findAll();

    /**
     * Looks up a supplier credit invoice by number in the current company.
     *
     * @param supplierCreditInvoice reference supplier credit invoice; must not be {@code null}
     * @return an {@link Optional} containing the stored credit invoice, or empty if not found
     */
    Optional<SSSupplierCreditInvoice> findBySupplierCreditInvoice(
            SSSupplierCreditInvoice supplierCreditInvoice);

    /**
     * Persists a new supplier credit invoice for the current company.
     *
     * @param supplierCreditInvoice the supplier credit invoice to add; must not be {@code null}
     */
    void add(SSSupplierCreditInvoice supplierCreditInvoice);

    /**
     * Updates an existing supplier credit invoice record.
     *
     * @param supplierCreditInvoice the supplier credit invoice with updated values; must not be {@code null}
     */
    void update(SSSupplierCreditInvoice supplierCreditInvoice);

    /**
     * Deletes a supplier credit invoice from the current company.
     *
     * @param supplierCreditInvoice the supplier credit invoice to delete; must not be {@code null}
     */
    void delete(SSSupplierCreditInvoice supplierCreditInvoice);
}

