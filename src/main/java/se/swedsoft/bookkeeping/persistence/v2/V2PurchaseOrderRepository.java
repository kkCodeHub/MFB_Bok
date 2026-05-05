package se.swedsoft.bookkeeping.persistence.v2;

import se.swedsoft.bookkeeping.data.SSPurchaseOrder;
import se.swedsoft.bookkeeping.data.system.SSDB;
import se.swedsoft.bookkeeping.persistence.PurchaseOrderRepository;

import java.util.List;
import java.util.Optional;

/**
 * V2 {@link PurchaseOrderRepository} implementation backed by the V2 schema.
 */
public class V2PurchaseOrderRepository implements PurchaseOrderRepository {

    private static final String SCHEMA_PROPERTY = "fribok.schema.version";

    private final SSDB db;

    /**
     * Creates a V2 purchase-order repository backed by the given {@link SSDB} instance.
     *
     * @param db the SSDB instance; must not be {@code null}
     * @throws NullPointerException  if {@code db} is {@code null}
     * @throws IllegalStateException if {@code fribok.schema.version} is not {@code v2}
     */
    public V2PurchaseOrderRepository(SSDB db) {
        if (db == null) {
            throw new NullPointerException("db must not be null");
        }
        if (!"v2".equalsIgnoreCase(System.getProperty(SCHEMA_PROPERTY, "v1"))) {
            throw new IllegalStateException("V2PurchaseOrderRepository requires fribok.schema.version=v2");
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

