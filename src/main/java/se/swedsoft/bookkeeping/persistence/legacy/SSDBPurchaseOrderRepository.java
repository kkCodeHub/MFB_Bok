package se.swedsoft.bookkeeping.persistence.legacy;

import se.swedsoft.bookkeeping.data.SSPurchaseOrder;
import se.swedsoft.bookkeeping.data.system.SSDB;
import se.swedsoft.bookkeeping.persistence.PurchaseOrderRepository;

import java.util.List;
import java.util.Optional;

/**
 * Legacy {@link PurchaseOrderRepository} implementation delegating to {@link SSDB}.
 */
public class SSDBPurchaseOrderRepository implements PurchaseOrderRepository {

    private final SSDB db;

    /**
     * Creates a repository backed by the given {@link SSDB} instance.
     *
     * @param db the SSDB instance to delegate to; must not be {@code null}
     */
    public SSDBPurchaseOrderRepository(SSDB db) {
        if (db == null) {
            throw new NullPointerException("db must not be null");
        }
        this.db = db;
    }

    /** {@inheritDoc} */
    @Override
    public List<SSPurchaseOrder> findAll() {
        return db.getPurchaseOrders();
    }

    /** {@inheritDoc} */
    @Override
    public Optional<SSPurchaseOrder> findByPurchaseOrder(SSPurchaseOrder purchaseOrder) {
        return db.getPurchaseOrder(purchaseOrder);
    }

    /** {@inheritDoc} */
    @Override
    public List<SSPurchaseOrder> findAll(List<SSPurchaseOrder> subset) {
        return db.getPurchaseOrders(subset);
    }

    /** {@inheritDoc} */
    @Override
    public void add(SSPurchaseOrder purchaseOrder) {
        db.addPurchaseOrder(purchaseOrder);
    }

    /** {@inheritDoc} */
    @Override
    public void update(SSPurchaseOrder purchaseOrder) {
        db.updatePurchaseOrder(purchaseOrder);
    }

    /** {@inheritDoc} */
    @Override
    public void delete(SSPurchaseOrder purchaseOrder) {
        db.deletePurchaseOrder(purchaseOrder);
    }
}

