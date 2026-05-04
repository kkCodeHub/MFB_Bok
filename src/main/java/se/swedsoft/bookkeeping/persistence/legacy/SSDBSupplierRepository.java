package se.swedsoft.bookkeeping.persistence.legacy;

import se.swedsoft.bookkeeping.data.SSSupplier;
import se.swedsoft.bookkeeping.data.system.SSDB;
import se.swedsoft.bookkeeping.persistence.SupplierRepository;

import java.util.List;
import java.util.Optional;

/**
 * Legacy {@link SupplierRepository} implementation that delegates all operations
 * to the existing {@link SSDB} singleton.
 *
 * <p>This class exists as a thin adapter so that callers can depend on the
 * {@code SupplierRepository} interface rather than {@code SSDB} directly.</p>
 */
public class SSDBSupplierRepository implements SupplierRepository {

    private final SSDB db;

    /**
     * Creates a repository backed by the given {@link SSDB} instance.
     *
     * @param db the SSDB instance to delegate to; must not be {@code null}
     */
    public SSDBSupplierRepository(SSDB db) {
        if (db == null) {
            throw new NullPointerException("db must not be null");
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


