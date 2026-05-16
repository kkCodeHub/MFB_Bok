package se.swedsoft.bookkeeping.persistence.v2;

import se.swedsoft.bookkeeping.data.common.SSDeliveryWay;
import se.swedsoft.bookkeeping.data.system.SSDB;
import se.swedsoft.bookkeeping.persistence.DeliveryWayRepository;

import java.util.List;
import java.util.Optional;

/**
 * V2 {@link DeliveryWayRepository} implementation backed by the normalized V2 schema.
 *
 * <p>Delegates CRUD operations to {@link SSDB}, which internally routes all reads
 * and writes against the relational {@code tbl_deliveryway} table when
 * {@code fribok.schema.version=v2} is active.</p>
 */
public class V2DeliveryWayRepository implements DeliveryWayRepository {

    private final SSDB db;

    /**
     * Creates a V2 delivery-way repository backed by the given {@link SSDB} instance.
     *
     * @param db the SSDB instance; must not be {@code null}
     * @throws NullPointerException if {@code db} is {@code null}
     */
    public V2DeliveryWayRepository(SSDB db) {
        if (db == null) {
            throw new NullPointerException("db must not be null");
        }
        this.db = db;
    }

    /** {@inheritDoc} */
    @Override
    public List<SSDeliveryWay> findAll() {
        return db.getDeliveryWays();
    }

    /** {@inheritDoc} */
    @Override
    public Optional<SSDeliveryWay> findByName(String name) {
        if (name == null) {
            return Optional.empty();
        }
        return db.getDeliveryWay(name);
    }

    /** {@inheritDoc} */
    @Override
    public void add(SSDeliveryWay deliveryWay) {
        db.addDeliveryWay(deliveryWay);
    }

    /** {@inheritDoc} */
    @Override
    public void update(SSDeliveryWay deliveryWay) {
        db.updateDeliveryWay(deliveryWay);
    }

    /** {@inheritDoc} */
    @Override
    public void delete(SSDeliveryWay deliveryWay) {
        db.deleteDeliveryWay(deliveryWay);
    }
}

