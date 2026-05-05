package se.swedsoft.bookkeeping.persistence.legacy;

import se.swedsoft.bookkeeping.data.SSOrder;
import se.swedsoft.bookkeeping.data.system.SSDB;
import se.swedsoft.bookkeeping.persistence.OrderRepository;

import java.util.List;
import java.util.Optional;

/**
 * Legacy {@link OrderRepository} implementation delegating to {@link SSDB}.
 */
public class SSDBOrderRepository implements OrderRepository {

    private final SSDB db;

    /**
     * Creates a repository backed by the given {@link SSDB} instance.
     *
     * @param db the SSDB instance to delegate to; must not be {@code null}
     */
    public SSDBOrderRepository(SSDB db) {
        if (db == null) {
            throw new NullPointerException("db must not be null");
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

