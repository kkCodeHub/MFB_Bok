package se.swedsoft.bookkeeping.persistence.v2;

import se.swedsoft.bookkeeping.data.SSInventory;
import se.swedsoft.bookkeeping.data.system.SSDB;
import se.swedsoft.bookkeeping.persistence.InventoryRepository;

import java.util.List;
import java.util.Optional;

/**
 * V2 {@link InventoryRepository} implementation backed by the V2 schema.
 */
public class V2InventoryRepository implements InventoryRepository {

    private static final String SCHEMA_PROPERTY = "fribok.schema.version";

    private final SSDB db;

    /**
     * Creates a V2 inventory repository backed by the given {@link SSDB} instance.
     *
     * @param db the SSDB instance; must not be {@code null}
     * @throws NullPointerException  if {@code db} is {@code null}
     * @throws IllegalStateException if {@code fribok.schema.version} is not {@code v2}
     */
    public V2InventoryRepository(SSDB db) {
        if (db == null) {
            throw new NullPointerException("db must not be null");
        }
        if (!"v2".equalsIgnoreCase(System.getProperty(SCHEMA_PROPERTY, "v1"))) {
            throw new IllegalStateException("V2InventoryRepository requires fribok.schema.version=v2");
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

