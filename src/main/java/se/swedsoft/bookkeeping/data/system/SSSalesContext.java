package se.swedsoft.bookkeeping.data.system;

import se.swedsoft.bookkeeping.data.SSCreditInvoice;
import se.swedsoft.bookkeeping.data.SSCustomer;
import se.swedsoft.bookkeeping.data.SSInvoice;
import se.swedsoft.bookkeeping.data.SSOrder;
import se.swedsoft.bookkeeping.data.SSPeriodicInvoice;
import se.swedsoft.bookkeeping.data.SSTender;
import se.swedsoft.bookkeeping.persistence.Repositories;

import java.util.List;
import java.util.Optional;

/**
 * Domain service for the Sales domain (domain #4 in the 12-domain target architecture).
 *
 * <p>Covers the full sales flow: quotes/tenders, orders, invoices, credit invoices,
 * and periodic invoices.</p>
 *
 * <p>All write operations delegate to the repository layer; no business logic
 * resides in this class.</p>
 */
public final class SSSalesContext {

    private SSSalesContext() {}

    // -------------------------------------------------------------------------
    // Customer CRUD
    // -------------------------------------------------------------------------

    /**
     * Returns all customers for the current company.
     *
     * @return list of customers; never {@code null}
     */
    public static List<SSCustomer> getCustomers() {
        return Repositories.customers().findAll();
    }

    /**
     * Returns only those customers from {@code pCustomers} that exist in the repository.
     *
     * @param pCustomers candidate list; must not be {@code null}
     * @return filtered list; never {@code null}
     */
    public static List<SSCustomer> getCustomers(List<SSCustomer> pCustomers) {
        return Repositories.customers().findAll(pCustomers);
    }

    /**
     * Looks up a customer by number.
     *
     * @param pNumber customer number; must not be {@code null}
     * @return an {@link Optional} containing the customer, or empty if not found
     */
    public static Optional<SSCustomer> getCustomer(String pNumber) {
        return Repositories.customers().findByNumber(pNumber);
    }

    /**
     * Persists a new customer.
     *
     * @param pCustomer the customer to add; must not be {@code null}
     */
    public static void addCustomer(SSCustomer pCustomer) {
        Repositories.customers().add(pCustomer);
    }

    /**
     * Updates an existing customer.
     *
     * @param pCustomer the customer with updated values; must not be {@code null}
     */
    public static void updateCustomer(SSCustomer pCustomer) {
        Repositories.customers().update(pCustomer);
    }

    /**
     * Deletes a customer.
     *
     * @param pCustomer the customer to delete; must not be {@code null}
     */
    public static void deleteCustomer(SSCustomer pCustomer) {
        Repositories.customers().delete(pCustomer);
    }

    // -------------------------------------------------------------------------
    // Tender (quote/offer) CRUD
    // -------------------------------------------------------------------------

    /**
     * Returns all tenders for the current company.
     *
     * @return list of tenders; never {@code null}
     */
    public static List<SSTender> getTenders() {
        return Repositories.tenders().findAll();
    }

    /**
     * Returns only those tenders from {@code pTenders} that exist in the repository.
     *
     * @param pTenders candidate list; must not be {@code null}
     * @return filtered list; never {@code null}
     */
    public static List<SSTender> getTenders(List<SSTender> pTenders) {
        return Repositories.tenders().findAll(pTenders);
    }

    /**
     * Looks up a tender by reference.
     *
     * @param pTender reference tender; must not be {@code null}
     * @return an {@link Optional} containing the tender, or empty if not found
     */
    public static Optional<SSTender> getTender(SSTender pTender) {
        return Repositories.tenders().findByTender(pTender);
    }

    /**
     * Persists a new tender.
     *
     * @param pTender the tender to add; must not be {@code null}
     */
    public static void addTender(SSTender pTender) {
        Repositories.tenders().add(pTender);
    }

    /**
     * Updates an existing tender.
     *
     * @param pTender the tender with updated values; must not be {@code null}
     */
    public static void updateTender(SSTender pTender) {
        Repositories.tenders().update(pTender);
    }

    /**
     * Deletes a tender.
     *
     * @param pTender the tender to delete; must not be {@code null}
     */
    public static void deleteTender(SSTender pTender) {
        Repositories.tenders().delete(pTender);
    }

    // -------------------------------------------------------------------------
    // Order CRUD
    // -------------------------------------------------------------------------

    /**
     * Returns all orders for the current company.
     *
     * @return list of orders; never {@code null}
     */
    public static List<SSOrder> getOrders() {
        return Repositories.orders().findAll();
    }

    /**
     * Returns only those orders from {@code pOrders} that exist in the repository.
     *
     * @param pOrders candidate list; must not be {@code null}
     * @return filtered list; never {@code null}
     */
    public static List<SSOrder> getOrders(List<SSOrder> pOrders) {
        return Repositories.orders().findAll(pOrders);
    }

    /**
     * Looks up an order by reference.
     *
     * @param pOrder reference order; must not be {@code null}
     * @return an {@link Optional} containing the order, or empty if not found
     */
    public static Optional<SSOrder> getOrder(SSOrder pOrder) {
        return Repositories.orders().findByOrder(pOrder);
    }

    /**
     * Persists a new order.
     *
     * @param pOrder the order to add; must not be {@code null}
     */
    public static void addOrder(SSOrder pOrder) {
        Repositories.orders().add(pOrder);
    }

    /**
     * Updates an existing order.
     *
     * @param pOrder the order with updated values; must not be {@code null}
     */
    public static void updateOrder(SSOrder pOrder) {
        Repositories.orders().update(pOrder);
    }

    /**
     * Deletes an order.
     *
     * @param pOrder the order to delete; must not be {@code null}
     */
    public static void deleteOrder(SSOrder pOrder) {
        Repositories.orders().delete(pOrder);
    }

    // -------------------------------------------------------------------------
    // Invoice CRUD
    // -------------------------------------------------------------------------

    /**
     * Returns all invoices for the current company.
     *
     * @return list of invoices; never {@code null}
     */
    public static List<SSInvoice> getInvoices() {
        return Repositories.invoices().findAll();
    }

    /**
     * Returns only those invoices from {@code pInvoices} that exist in the repository.
     *
     * @param pInvoices candidate list; must not be {@code null}
     * @return filtered list; never {@code null}
     */
    public static List<SSInvoice> getInvoices(List<SSInvoice> pInvoices) {
        return Repositories.invoices().findAll(pInvoices);
    }

    /**
     * Looks up an invoice by reference.
     *
     * @param pInvoice reference invoice; must not be {@code null}
     * @return an {@link Optional} containing the invoice, or empty if not found
     */
    public static Optional<SSInvoice> getInvoice(SSInvoice pInvoice) {
        return Repositories.invoices().findByInvoice(pInvoice);
    }

    /**
     * Persists a new invoice.
     *
     * @param pInvoice the invoice to add; must not be {@code null}
     */
    public static void addInvoice(SSInvoice pInvoice) {
        Repositories.invoices().add(pInvoice);
    }

    /**
     * Updates an existing invoice.
     *
     * @param pInvoice the invoice with updated values; must not be {@code null}
     */
    public static void updateInvoice(SSInvoice pInvoice) {
        Repositories.invoices().update(pInvoice);
    }

    /**
     * Deletes an invoice.
     *
     * <p>This path represents physical delete. Before the invoice row is removed,
     * all linked customer orders are unlinked from the invoice.</p>
     *
     * @param pInvoice the invoice to delete; must not be {@code null}
     */
    public static void deleteInvoice(SSInvoice pInvoice) {
        removeInvoiceReferenceFromOrders(pInvoice);
        Repositories.invoices().delete(pInvoice);
    }

    /**
     * Returns the maximum invoice number from tbl_invoice for the current company.
     *
     * @return the highest invoice number, or -1 if no invoices exist
     */
    public static int getMaxInvoiceId() {
        return Repositories.invoices().getMaxInvoiceId();
    }

    private static void removeInvoiceReferenceFromOrders(SSInvoice pInvoice) {
        for (SSOrder iOrder : Repositories.orders().findAll()) {
            if (iOrder.hasInvoice(pInvoice)) {
                iOrder.setInvoice(null);
                Repositories.orders().update(iOrder);
            }
        }
    }

    // -------------------------------------------------------------------------
    // Credit Invoice CRUD
    // -------------------------------------------------------------------------

    /**
     * Returns all credit invoices for the current company.
     *
     * @return list of credit invoices; never {@code null}
     */
    public static List<SSCreditInvoice> getCreditInvoices() {
        return Repositories.creditInvoices().findAll();
    }

    /**
     * Returns only those credit invoices from {@code pCreditInvoices} that exist in the repository.
     *
     * @param pCreditInvoices candidate list; must not be {@code null}
     * @return filtered list; never {@code null}
     */
    public static List<SSCreditInvoice> getCreditInvoices(List<SSCreditInvoice> pCreditInvoices) {
        return Repositories.creditInvoices().findAll(pCreditInvoices);
    }

    /**
     * Looks up a credit invoice by reference.
     *
     * @param pCreditInvoice reference credit invoice; must not be {@code null}
     * @return an {@link Optional} containing the credit invoice, or empty if not found
     */
    public static Optional<SSCreditInvoice> getCreditInvoice(SSCreditInvoice pCreditInvoice) {
        return Repositories.creditInvoices().findByCreditInvoice(pCreditInvoice);
    }

    /**
     * Persists a new credit invoice.
     *
     * @param pCreditInvoice the credit invoice to add; must not be {@code null}
     */
    public static void addCreditInvoice(SSCreditInvoice pCreditInvoice) {
        Repositories.creditInvoices().add(pCreditInvoice);
    }

    /**
     * Updates an existing credit invoice.
     *
     * @param pCreditInvoice the credit invoice with updated values; must not be {@code null}
     */
    public static void updateCreditInvoice(SSCreditInvoice pCreditInvoice) {
        Repositories.creditInvoices().update(pCreditInvoice);
    }

    /**
     * Deletes a credit invoice.
     *
     * @param pCreditInvoice the credit invoice to delete; must not be {@code null}
     */
    public static void deleteCreditInvoice(SSCreditInvoice pCreditInvoice) {
        Repositories.creditInvoices().delete(pCreditInvoice);
    }

    // -------------------------------------------------------------------------
    // Periodic Invoice CRUD
    // -------------------------------------------------------------------------

    /**
     * Returns all periodic invoices for the current company.
     *
     * @return list of periodic invoices; never {@code null}
     */
    public static List<SSPeriodicInvoice> getPeriodicInvoices() {
        return Repositories.periodicInvoices().findAll();
    }

    /**
     * Looks up a periodic invoice by reference.
     *
     * @param pPeriodicInvoice reference periodic invoice; must not be {@code null}
     * @return an {@link Optional} containing the periodic invoice, or empty if not found
     */
    public static Optional<SSPeriodicInvoice> getPeriodicInvoice(SSPeriodicInvoice pPeriodicInvoice) {
        return Repositories.periodicInvoices().findByPeriodicInvoice(pPeriodicInvoice);
    }

    /**
     * Persists a new periodic invoice.
     *
     * @param pPeriodicInvoice the periodic invoice to add; must not be {@code null}
     */
    public static void addPeriodicInvoice(SSPeriodicInvoice pPeriodicInvoice) {
        Repositories.periodicInvoices().add(pPeriodicInvoice);
    }

    /**
     * Updates an existing periodic invoice.
     *
     * @param pPeriodicInvoice the periodic invoice with updated values; must not be {@code null}
     */
    public static void updatePeriodicInvoice(SSPeriodicInvoice pPeriodicInvoice) {
        Repositories.periodicInvoices().update(pPeriodicInvoice);
    }

    /**
     * Deletes a periodic invoice.
     *
     * @param pPeriodicInvoice the periodic invoice to delete; must not be {@code null}
     */
    public static void deletePeriodicInvoice(SSPeriodicInvoice pPeriodicInvoice) {
        Repositories.periodicInvoices().delete(pPeriodicInvoice);
    }
}
