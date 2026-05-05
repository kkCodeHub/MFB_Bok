package se.swedsoft.bookkeeping.persistence;

import se.swedsoft.bookkeeping.data.SSPurchaseOrder;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for {@link SSPurchaseOrder} persistence operations.
 */
public interface PurchaseOrderRepository {

    /**
     * Returns all purchase orders for the current company.
     *
     * @return mutable list of purchase orders; never {@code null}
     */
    List<SSPurchaseOrder> findAll();

    /**
     * Looks up a purchase order by number in the current company.
     *
     * @param purchaseOrder reference purchase order; must not be {@code null}
     * @return an {@link Optional} containing the stored purchase order, or empty if not found
     */
    Optional<SSPurchaseOrder> findByPurchaseOrder(SSPurchaseOrder purchaseOrder);

    /**
     * Returns only those purchase orders from {@code subset} that exist in the repository.
     *
     * @param subset candidate list; must not be {@code null}
     * @return filtered list; never {@code null}
     */
    List<SSPurchaseOrder> findAll(List<SSPurchaseOrder> subset);

    /**
     * Persists a new purchase order for the current company.
     *
     * @param purchaseOrder the purchase order to add; must not be {@code null}
     */
    void add(SSPurchaseOrder purchaseOrder);

    /**
     * Updates an existing purchase order record.
     *
     * @param purchaseOrder the purchase order with updated values; must not be {@code null}
     */
    void update(SSPurchaseOrder purchaseOrder);

    /**
     * Deletes a purchase order from the current company.
     *
     * @param purchaseOrder the purchase order to delete; must not be {@code null}
     */
    void delete(SSPurchaseOrder purchaseOrder);
}

