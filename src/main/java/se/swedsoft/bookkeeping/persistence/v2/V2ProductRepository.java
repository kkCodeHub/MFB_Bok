package se.swedsoft.bookkeeping.persistence.v2;

import se.swedsoft.bookkeeping.data.SSProduct;
import se.swedsoft.bookkeeping.data.system.SSDB;
import se.swedsoft.bookkeeping.persistence.ProductRepository;

import java.util.List;
import java.util.Optional;

/**
 * V2 {@link ProductRepository} implementation backed by the normalized V2 schema.
 *
 * <p>Delegates CRUD operations to {@link SSDB}, which internally routes all reads
 * and writes against {@code tbl_product} and its child table
 * {@code tbl_product_account} when {@code fribok.schema.version=v2} is active.
 * The constructor enforces that the V2 property is set so callers get an explicit
 * failure rather than silent fall-through to the V1 serialisation path.</p>
 */
public class V2ProductRepository implements ProductRepository {

    private static final String SCHEMA_PROPERTY = "fribok.schema.version";

    private final SSDB db;

    /**
     * Creates a V2 product repository backed by the given {@link SSDB} instance.
     *
     * @param db the SSDB instance; must not be {@code null}
     * @throws NullPointerException  if {@code db} is {@code null}
     * @throws IllegalStateException if {@code fribok.schema.version} is not {@code v2}
     */
    public V2ProductRepository(SSDB db) {
        if (db == null) {
            throw new NullPointerException("db must not be null");
        }
        if (!"v2".equalsIgnoreCase(System.getProperty(SCHEMA_PROPERTY, "v1"))) {
            throw new IllegalStateException(
                    "V2ProductRepository requires fribok.schema.version=v2");
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

