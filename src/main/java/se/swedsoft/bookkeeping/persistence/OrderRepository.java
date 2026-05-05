package se.swedsoft.bookkeeping.persistence;

import se.swedsoft.bookkeeping.data.SSOrder;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for {@link SSOrder} persistence operations.
 */
public interface OrderRepository {

    /**
     * Returns all orders for the current company.
     *
     * @return mutable list of orders; never {@code null}
     */
    List<SSOrder> findAll();

    /**
     * Looks up an order by number in the current company.
     *
     * @param order reference order; must not be {@code null}
     * @return an {@link Optional} containing the stored order, or empty if not found
     */
    Optional<SSOrder> findByOrder(SSOrder order);

    /**
     * Returns only those orders from {@code subset} that exist in the repository.
     *
     * @param subset candidate list; must not be {@code null}
     * @return filtered list; never {@code null}
     */
    List<SSOrder> findAll(List<SSOrder> subset);

    /**
     * Persists a new order for the current company.
     *
     * @param order the order to add; must not be {@code null}
     */
    void add(SSOrder order);

    /**
     * Updates an existing order record.
     *
     * @param order the order with updated values; must not be {@code null}
     */
    void update(SSOrder order);

    /**
     * Deletes an order from the current company.
     *
     * @param order the order to delete; must not be {@code null}
     */
    void delete(SSOrder order);
}

