package se.swedsoft.bookkeeping.persistence.v2;

import se.swedsoft.bookkeeping.data.SSOrder;
import se.swedsoft.bookkeeping.data.system.SSDB;
import se.swedsoft.bookkeeping.persistence.OrderRepository;

import java.util.List;
import java.util.Optional;

/**
 * V2 {@link OrderRepository} implementation backed by the V2 schema.
 */
public class V2OrderRepository implements OrderRepository {

    private static final String SCHEMA_PROPERTY = "fribok.schema.version";

    private final SSDB db;

    /**
     * Creates a V2 order repository backed by the given {@link SSDB} instance.
     *
     * @param db the SSDB instance; must not be {@code null}
     * @throws NullPointerException  if {@code db} is {@code null}
     * @throws IllegalStateException if {@code fribok.schema.version} is not {@code v2}
     */
    public V2OrderRepository(SSDB db) {
        if (db == null) {
            throw new NullPointerException("db must not be null");
        }
        if (!"v2".equalsIgnoreCase(System.getProperty(SCHEMA_PROPERTY, "v1"))) {
            throw new IllegalStateException("V2OrderRepository requires fribok.schema.version=v2");
        }
        this.db = db;
    }

    /** {@inheritDoc} */
    @Override
    public List<SSOrder> findAll() {
        return db.getOrders();
    }

    /** {@inheritDoc} */
    @Override
    public Optional<SSOrder> findByOrder(SSOrder order) {
        return db.getOrder(order);
    }

    /** {@inheritDoc} */
    @Override
    public List<SSOrder> findAll(List<SSOrder> subset) {
        return db.getOrders(subset);
    }

    /** {@inheritDoc} */
    @Override
    public void add(SSOrder order) {
        db.addOrder(order);
    }

    /** {@inheritDoc} */
    @Override
    public void update(SSOrder order) {
        db.updateOrder(order);
    }

    /** {@inheritDoc} */
    @Override
    public void delete(SSOrder order) {
        db.deleteOrder(order);
    }
}

