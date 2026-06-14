package se.swedsoft.bookkeeping.persistence.legacy;

import se.swedsoft.bookkeeping.data.SSProduct;
import se.swedsoft.bookkeeping.data.system.SSDB;
import se.swedsoft.bookkeeping.persistence.ProductRepository;

import java.util.List;
import java.util.Optional;

/**
 * Legacy {@link ProductRepository} implementation that delegates all operations
 * to the existing {@link SSDB} singleton.
 *
 * <p>This class exists as a thin adapter so that callers can depend on the
 * {@code ProductRepository} interface rather than {@code SSDB} directly.</p>
 */
public class SSDBProductRepository implements ProductRepository {

    private final SSDB db;

    /**
     * Creates a repository backed by the given {@link SSDB} instance.
     *
     * @param db the SSDB instance to delegate to; must not be {@code null}
     */
    public SSDBProductRepository(SSDB db) {
        if (db == null) {
            throw new NullPointerException("db must not be null");
        }
        this.db = db;
    }

    /** {@inheritDoc} */
    @Override
    public List<SSProduct> findAll() {
        return db.getProducts();
    }

    /** {@inheritDoc} */
    @Override
    public Optional<SSProduct> findByNumber(String productNumber) {
        return db.getProduct(productNumber);
    }

    /** {@inheritDoc} */
    @Override
    public Optional<SSProduct> findByProduct(SSProduct product) {
        return db.getProduct(product);
    }

    /** {@inheritDoc} */
    @Override
    public List<SSProduct> findAll(List<SSProduct> subset) {
        return db.getProducts(subset);
    }

    /** {@inheritDoc} */
    @Override
    public void add(SSProduct product) {
        db.addProduct(product);
    }

    /** {@inheritDoc} */
    @Override
    public void update(SSProduct product) {
        db.updateProduct(product);
    }

    /** {@inheritDoc} */
    @Override
    public void delete(SSProduct product) {
        db.deleteProduct(product);
    }
}


