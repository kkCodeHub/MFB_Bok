package se.swedsoft.bookkeeping.persistence.v2;

import se.swedsoft.bookkeeping.data.SSSupplier;
import se.swedsoft.bookkeeping.data.system.SSDB;
import se.swedsoft.bookkeeping.persistence.SupplierRepository;

import java.util.List;
import java.util.Optional;

/**
 * V2 {@link SupplierRepository} implementation backed by the normalized V2 schema.
 *
 * <p>Delegates CRUD operations to {@link SSDB}, which internally routes all reads
 * and writes against the relational {@code tbl_supplier} table when
 * {@code fribok.schema.version=v2} is active.  The constructor enforces that the
 * V2 property is set so callers get an explicit failure rather than silent
 * fall-through to the V1 serialisation path.</p>
 */
public class V2SupplierRepository implements SupplierRepository {

    private static final String SCHEMA_PROPERTY = "fribok.schema.version";

    private final SSDB db;

    /**
     * Creates a V2 supplier repository backed by the given {@link SSDB} instance.
     *
     * @param db the SSDB instance; must not be {@code null}
     * @throws NullPointerException  if {@code db} is {@code null}
     * @throws IllegalStateException if {@code fribok.schema.version} is not {@code v2}
     */
    public V2SupplierRepository(SSDB db) {
        if (db == null) {
            throw new NullPointerException("db must not be null");
        }
        if (!"v2".equalsIgnoreCase(System.getProperty(SCHEMA_PROPERTY, "v1"))) {
            throw new IllegalStateException(
                    "V2SupplierRepository requires fribok.schema.version=v2");
        }
        this.db = db;
    }

    /** {@inheritDoc} */
    @Override
    public List<SSSupplier> findAll() {
        return db.getSuppliers();
    }

    /** {@inheritDoc} */
    @Override
    public Optional<SSSupplier> findBySupplier(SSSupplier supplier) {
        return db.getSupplier(supplier);
    }

    /** {@inheritDoc} */
    @Override
    public List<SSSupplier> findAll(List<SSSupplier> subset) {
        return db.getSuppliers(subset);
    }

    /** {@inheritDoc} */
    @Override
    public void add(SSSupplier supplier) {
        db.addSupplier(supplier);
    }

    /** {@inheritDoc} */
    @Override
    public void update(SSSupplier supplier) {
        db.updateSupplier(supplier);
    }

    /** {@inheritDoc} */
    @Override
    public void delete(SSSupplier supplier) {
        db.deleteSupplier(supplier);
    }
}

