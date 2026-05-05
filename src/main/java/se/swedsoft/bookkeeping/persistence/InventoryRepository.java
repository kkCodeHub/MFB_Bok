package se.swedsoft.bookkeeping.persistence;

import se.swedsoft.bookkeeping.data.SSInventory;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for {@link SSInventory} persistence operations.
 */
public interface InventoryRepository {

    /**
     * Returns all inventory documents for the current company.
     *
     * @return mutable list of inventories; never {@code null}
     */
    List<SSInventory> findAll();

    /**
     * Looks up an inventory document by number in the current company.
     *
     * @param inventory reference inventory; must not be {@code null}
     * @return an {@link Optional} containing the stored inventory, or empty if not found
     */
    Optional<SSInventory> findByInventory(SSInventory inventory);

    /**
     * Persists a new inventory document for the current company.
     *
     * @param inventory the inventory to add; must not be {@code null}
     */
    void add(SSInventory inventory);

    /**
     * Updates an existing inventory record.
     *
     * @param inventory the inventory with updated values; must not be {@code null}
     */
    void update(SSInventory inventory);

    /**
     * Deletes an inventory document from the current company.
     *
     * @param inventory the inventory to delete; must not be {@code null}
     */
    void delete(SSInventory inventory);
}

