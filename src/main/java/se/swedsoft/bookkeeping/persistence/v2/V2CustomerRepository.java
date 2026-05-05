package se.swedsoft.bookkeeping.persistence.v2;

import se.swedsoft.bookkeeping.data.SSCustomer;
import se.swedsoft.bookkeeping.data.system.SSDB;
import se.swedsoft.bookkeeping.persistence.CustomerRepository;

import java.util.List;
import java.util.Optional;

/**
 * V2 {@link CustomerRepository} implementation backed by the normalized V2 schema.
 *
 * <p>Delegates CRUD operations to {@link SSDB}, which internally routes all reads
 * and writes against the relational {@code tbl_customer} table when
 * {@code fribok.schema.version=v2} is active.  This class enforces that the V2
 * property is set at construction time so callers get a fast, explicit failure
 * rather than silent fall-through to the serialisation-based V1 path.</p>
 *
 * <p>The longer-term intent is to replace the SSDB delegation with direct JDBC
 * statements once the God-object refactor progresses further.  Until then this
 * class provides the V2-committed side of the {@link
 * se.swedsoft.bookkeeping.persistence.Repositories} factory.</p>
 */
public class V2CustomerRepository implements CustomerRepository {

    private static final String SCHEMA_PROPERTY = "fribok.schema.version";

    private final SSDB db;

    /**
     * Creates a V2 customer repository backed by the given {@link SSDB} instance.
     *
     * @param db the SSDB instance; must not be {@code null}
     * @throws NullPointerException     if {@code db} is {@code null}
     * @throws IllegalStateException    if {@code fribok.schema.version} is not {@code v2}
     */
    public V2CustomerRepository(SSDB db) {
        if (db == null) {
            throw new NullPointerException("db must not be null");
        }
        if (!"v2".equalsIgnoreCase(System.getProperty(SCHEMA_PROPERTY, "v1"))) {
            throw new IllegalStateException(
                    "V2CustomerRepository requires fribok.schema.version=v2");
        }
        this.db = db;
    }

    /** {@inheritDoc} */
    @Override
    public List<SSCustomer> findAll() {
        return db.getCustomers();
    }

    /** {@inheritDoc} */
    @Override
    public Optional<SSCustomer> findByNumber(String customerNumber) {
        return db.getCustomer(customerNumber);
    }

    /** {@inheritDoc} */
    @Override
    public Optional<SSCustomer> findByCustomer(SSCustomer customer) {
        return db.getCustomer(customer);
    }

    /** {@inheritDoc} */
    @Override
    public List<SSCustomer> findAll(List<SSCustomer> subset) {
        return db.getCustomers(subset);
    }

    /** {@inheritDoc} */
    @Override
    public void add(SSCustomer customer) {
        db.addCustomer(customer);
    }

    /** {@inheritDoc} */
    @Override
    public void update(SSCustomer customer) {
        db.updateCustomer(customer);
    }

    /** {@inheritDoc} */
    @Override
    public void delete(SSCustomer customer) {
        db.deleteCustomer(customer);
    }
}

