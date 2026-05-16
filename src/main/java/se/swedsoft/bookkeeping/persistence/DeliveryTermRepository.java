package se.swedsoft.bookkeeping.persistence;

import se.swedsoft.bookkeeping.data.common.SSDeliveryTerm;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for {@link SSDeliveryTerm} persistence operations.
 *
 * <p>Reference data (delivery terms) is global — not per company.</p>
 */
public interface DeliveryTermRepository {

    /**
     * Returns all delivery terms.
     *
     * @return mutable list of delivery terms; never {@code null}
     */
    List<SSDeliveryTerm> findAll();

    /**
     * Looks up a delivery term by its name.
     *
     * @param name the delivery term name; must not be {@code null}
     * @return an {@link Optional} containing the delivery term, or empty if not found
     */
    Optional<SSDeliveryTerm> findByName(String name);

    /**
     * Persists a new delivery term.
     *
     * @param deliveryTerm the delivery term to add; must not be {@code null}
     */
    void add(SSDeliveryTerm deliveryTerm);

    /**
     * Updates an existing delivery term record.
     *
     * @param deliveryTerm the delivery term with updated values; must not be {@code null}
     */
    void update(SSDeliveryTerm deliveryTerm);

    /**
     * Deletes a delivery term.
     *
     * @param deliveryTerm the delivery term to delete; must not be {@code null}
     */
    void delete(SSDeliveryTerm deliveryTerm);
}

