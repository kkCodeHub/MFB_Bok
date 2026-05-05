package se.swedsoft.bookkeeping.persistence.legacy;

import se.swedsoft.bookkeeping.data.SSInventory;
import se.swedsoft.bookkeeping.data.system.SSDB;
import se.swedsoft.bookkeeping.persistence.InventoryRepository;

import java.util.List;
import java.util.Optional;

/**
 * Legacy {@link InventoryRepository} implementation delegating to {@link SSDB}.
 */
public class SSDBInventoryRepository implements InventoryRepository {

    private final SSDB db;

    /**
     * Creates a repository backed by the given {@link SSDB} instance.
     *
     * @param db the SSDB instance to delegate to; must not be {@code null}
     */
    public SSDBInventoryRepository(SSDB db) {
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

