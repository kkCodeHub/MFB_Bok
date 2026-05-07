package se.swedsoft.bookkeeping.persistence.v2;

import se.swedsoft.bookkeeping.data.SSInventory;
import se.swedsoft.bookkeeping.data.system.SSDB;
import se.swedsoft.bookkeeping.persistence.InventoryRepository;

import java.util.List;
import java.util.Optional;

/**
 * V2 {@link InventoryRepository} implementation for the post-cutover
 * inventory domain.
 */
public class V2InventoryRepository implements InventoryRepository {

    private final SSDB db;

    /**
     * Creates a V2 inventory repository backed by the given {@link SSDB} instance.
     *
     * @param db the SSDB instance; must not be {@code null}
     * @throws NullPointerException if {@code db} is {@code null}
     */
    public V2InventoryRepository(SSDB db) {
        if (db == null) {
            throw new NullPointerException("db must not be null");
        }
        this.db = db;
    }

    /** {@inheritDoc} */
    @Override
    public List<SSInventory> findAll() {
        return db.getInventories();
    }

    /** {@inheritDoc} */
    @Override
    public Optional<SSInventory> findByInventory(SSInventory inventory) {
        return db.getInventory(inventory);
    }

    /** {@inheritDoc} */
    @Override
    public void add(SSInventory inventory) {
        db.addInventory(inventory);
    }

    /** {@inheritDoc} */
    @Override
    public void update(SSInventory inventory) {
        db.updateInventory(inventory);
    }

    /** {@inheritDoc} */
    @Override
    public void delete(SSInventory inventory) {
        db.deleteInventory(inventory);
    }
}

