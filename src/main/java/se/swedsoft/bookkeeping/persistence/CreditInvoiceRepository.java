package se.swedsoft.bookkeeping.persistence;

import se.swedsoft.bookkeeping.data.SSCreditInvoice;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for {@link SSCreditInvoice} persistence operations.
 */
public interface CreditInvoiceRepository {

    /**
     * Returns all credit invoices for the current company.
     *
     * @return mutable list of credit invoices; never {@code null}
     */
    List<SSCreditInvoice> findAll();

    /**
     * Looks up a credit invoice by number in the current company.
     *
     * @param creditInvoice reference credit invoice; must not be {@code null}
     * @return an {@link Optional} containing the stored credit invoice, or empty if not found
     */
    Optional<SSCreditInvoice> findByCreditInvoice(SSCreditInvoice creditInvoice);

    /**
     * Returns only those credit invoices from {@code subset} that exist in the repository.
     *
     * @param subset candidate list; must not be {@code null}
     * @return filtered list; never {@code null}
     */
    List<SSCreditInvoice> findAll(List<SSCreditInvoice> subset);

    /**
     * Persists a new credit invoice for the current company.
     *
     * @param creditInvoice the credit invoice to add; must not be {@code null}
     */
    void add(SSCreditInvoice creditInvoice);

    /**
     * Updates an existing credit invoice record.
     *
     * @param creditInvoice the credit invoice with updated values; must not be {@code null}
     */
    void update(SSCreditInvoice creditInvoice);

    /**
     * Deletes a credit invoice from the current company.
     *
     * @param creditInvoice the credit invoice to delete; must not be {@code null}
     */
    void delete(SSCreditInvoice creditInvoice);
}

