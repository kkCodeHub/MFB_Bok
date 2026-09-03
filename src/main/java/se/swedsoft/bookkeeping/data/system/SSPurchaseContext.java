package se.swedsoft.bookkeeping.data.system;

import se.swedsoft.bookkeeping.data.SSPurchaseOrder;
import se.swedsoft.bookkeeping.data.SSSupplier;
import se.swedsoft.bookkeeping.data.SSSupplierCreditInvoice;
import se.swedsoft.bookkeeping.data.SSSupplierInvoice;
import se.swedsoft.bookkeeping.persistence.Repositories;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Domain service for the Purchase domain (domain #5 in the 12-domain target architecture).
 *
 * <p>Covers the full purchase flow: suppliers, purchase orders, supplier invoices,
 * and supplier credit invoices.</p>
 *
 * <p>All write operations delegate to the repository layer; no business logic
 * resides in this class.</p>
 */
public final class SSPurchaseContext {

    private SSPurchaseContext() {}

    // -------------------------------------------------------------------------
    // Supplier CRUD
    // -------------------------------------------------------------------------

    /**
     * Returns all suppliers for the current company.
     *
     * @return list of suppliers; never {@code null}
     */
    public static List<SSSupplier> getSuppliers() {
        return Repositories.suppliers().findAll();
    }

    /**
     * Returns only those suppliers from {@code pSuppliers} that exist in the repository.
     *
     * @param pSuppliers candidate list; must not be {@code null}
     * @return filtered list; never {@code null}
     */
    public static List<SSSupplier> getSuppliers(List<SSSupplier> pSuppliers) {
        return Repositories.suppliers().findAll(pSuppliers);
    }

    /**
     * Looks up a supplier by reference.
     *
     * @param pSupplier reference supplier; must not be {@code null}
     * @return an {@link Optional} containing the supplier, or empty if not found
     */
    public static Optional<SSSupplier> getSupplier(SSSupplier pSupplier) {
        return Repositories.suppliers().findBySupplier(pSupplier);
    }

    /**
     * Persists a new supplier.
     *
     * @param pSupplier the supplier to add; must not be {@code null}
     */
    public static void addSupplier(SSSupplier pSupplier) {
        Repositories.suppliers().add(pSupplier);
    }

    /**
     * Updates an existing supplier.
     *
     * @param pSupplier the supplier with updated values; must not be {@code null}
     */
    public static void updateSupplier(SSSupplier pSupplier) {
        Repositories.suppliers().update(pSupplier);
    }

    /**
     * Deletes a supplier.
     *
     * @param pSupplier the supplier to delete; must not be {@code null}
     */
    public static void deleteSupplier(SSSupplier pSupplier) {
        Repositories.suppliers().delete(pSupplier);
    }

    // -------------------------------------------------------------------------
    // Purchase Order CRUD
    // -------------------------------------------------------------------------

    /**
     * Returns all purchase orders for the current company.
     *
     * @return list of purchase orders; never {@code null}
     */
    public static List<SSPurchaseOrder> getPurchaseOrders() {
        return Repositories.purchaseOrders().findAll();
    }

    /**
     * Returns only those purchase orders from {@code pPurchaseOrders} that exist in the repository.
     *
     * @param pPurchaseOrders candidate list; must not be {@code null}
     * @return filtered list; never {@code null}
     */
    public static List<SSPurchaseOrder> getPurchaseOrders(List<SSPurchaseOrder> pPurchaseOrders) {
        return Repositories.purchaseOrders().findAll(pPurchaseOrders);
    }

    /**
     * Looks up a purchase order by reference.
     *
     * @param pPurchaseOrder reference purchase order; must not be {@code null}
     * @return an {@link Optional} containing the purchase order, or empty if not found
     */
    public static Optional<SSPurchaseOrder> getPurchaseOrder(SSPurchaseOrder pPurchaseOrder) {
        return Repositories.purchaseOrders().findByPurchaseOrder(pPurchaseOrder);
    }

    /**
     * Persists a new purchase order.
     *
     * @param pPurchaseOrder the purchase order to add; must not be {@code null}
     */
    public static void addPurchaseOrder(SSPurchaseOrder pPurchaseOrder) {
        Repositories.purchaseOrders().add(pPurchaseOrder);
    }

    /**
     * Updates an existing purchase order.
     *
     * @param pPurchaseOrder the purchase order with updated values; must not be {@code null}
     */
    public static void updatePurchaseOrder(SSPurchaseOrder pPurchaseOrder) {
        Repositories.purchaseOrders().update(pPurchaseOrder);
    }

    /**
     * Deletes a purchase order.
     *
     * @param pPurchaseOrder the purchase order to delete; must not be {@code null}
     */
    public static void deletePurchaseOrder(SSPurchaseOrder pPurchaseOrder) {
        Repositories.purchaseOrders().delete(pPurchaseOrder);
    }

    // -------------------------------------------------------------------------
    // Supplier Invoice CRUD
    // -------------------------------------------------------------------------

    /**
     * Returns all supplier invoices for the current company.
     *
     * @return list of supplier invoices; never {@code null}
     */
    public static List<SSSupplierInvoice> getSupplierInvoices() {
        return Repositories.supplierInvoices().findAll();
    }

    /**
     * Returns only those supplier invoices from {@code pSupplierInvoices} that exist in the repository.
     *
     * @param pSupplierInvoices candidate list; must not be {@code null}
     * @return filtered list; never {@code null}
     */
    public static List<SSSupplierInvoice> getSupplierInvoices(List<SSSupplierInvoice> pSupplierInvoices) {
        return Repositories.supplierInvoices().findAll(pSupplierInvoices);
    }

    /**
     * Looks up a supplier invoice by reference.
     *
     * @param pSupplierInvoice reference supplier invoice; must not be {@code null}
     * @return an {@link Optional} containing the supplier invoice, or empty if not found
     */
    public static Optional<SSSupplierInvoice> getSupplierInvoice(SSSupplierInvoice pSupplierInvoice) {
        return Repositories.supplierInvoices().findBySupplierInvoice(pSupplierInvoice);
    }

    /**
     * Persists a new supplier invoice.
     *
     * @param pSupplierInvoice the supplier invoice to add; must not be {@code null}
     */
    public static void addSupplierInvoice(SSSupplierInvoice pSupplierInvoice) {
        Repositories.supplierInvoices().add(pSupplierInvoice);
    }

    /**
     * Updates an existing supplier invoice.
     *
     * @param pSupplierInvoice the supplier invoice with updated values; must not be {@code null}
     */
    public static void updateSupplierInvoice(SSSupplierInvoice pSupplierInvoice) {
        Repositories.supplierInvoices().update(pSupplierInvoice);
    }

    /**
     * Deletes a supplier invoice.
     *
     * @param pSupplierInvoice the supplier invoice to delete; must not be {@code null}
     */
    public static void deleteSupplierInvoice(SSSupplierInvoice pSupplierInvoice) {
        Repositories.supplierInvoices().delete(pSupplierInvoice);
    }

    // -------------------------------------------------------------------------
    // Supplier Credit Invoice CRUD
    // -------------------------------------------------------------------------

    /**
     * Returns all supplier credit invoices for the current company.
     *
     * @return list of supplier credit invoices; never {@code null}
     */
    public static List<SSSupplierCreditInvoice> getSupplierCreditInvoices() {
        return Repositories.supplierCreditInvoices().findAll();
    }

    /**
     * Returns only those supplier credit invoices from {@code pSupplierCreditInvoices}
     * that exist in the repository.
     *
     * @param pSupplierCreditInvoices candidate list; must not be {@code null}
     * @return filtered list; never {@code null}
     */
    public static List<SSSupplierCreditInvoice> getSupplierCreditInvoices(
            List<SSSupplierCreditInvoice> pSupplierCreditInvoices) {
        if (pSupplierCreditInvoices == null || pSupplierCreditInvoices.isEmpty()) {
            return new ArrayList<>();
        }
        List<SSSupplierCreditInvoice> result = new ArrayList<>();
        for (SSSupplierCreditInvoice sci : pSupplierCreditInvoices) {
            Repositories.supplierCreditInvoices().findBySupplierCreditInvoice(sci).ifPresent(result::add);
        }
        return result;
    }

    /**
     * Looks up a supplier credit invoice by reference.
     *
     * @param pSupplierCreditInvoice reference supplier credit invoice; must not be {@code null}
     * @return an {@link Optional} containing the supplier credit invoice, or empty if not found
     */
    public static Optional<SSSupplierCreditInvoice> getSupplierCreditInvoice(
            SSSupplierCreditInvoice pSupplierCreditInvoice) {
        return Repositories.supplierCreditInvoices().findBySupplierCreditInvoice(pSupplierCreditInvoice);
    }

    /**
     * Persists a new supplier credit invoice.
     *
     * @param pSupplierCreditInvoice the supplier credit invoice to add; must not be {@code null}
     */
    public static void addSupplierCreditInvoice(SSSupplierCreditInvoice pSupplierCreditInvoice) {
        Repositories.supplierCreditInvoices().add(pSupplierCreditInvoice);
    }

    /**
     * Updates an existing supplier credit invoice.
     *
     * @param pSupplierCreditInvoice the supplier credit invoice with updated values; must not be {@code null}
     */
    public static void updateSupplierCreditInvoice(SSSupplierCreditInvoice pSupplierCreditInvoice) {
        Repositories.supplierCreditInvoices().update(pSupplierCreditInvoice);
    }

    /**
     * Deletes a supplier credit invoice.
     *
     * @param pSupplierCreditInvoice the supplier credit invoice to delete; must not be {@code null}
     */
    public static void deleteSupplierCreditInvoice(SSSupplierCreditInvoice pSupplierCreditInvoice) {
        Repositories.supplierCreditInvoices().delete(pSupplierCreditInvoice);
    }
}
