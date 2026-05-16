package se.swedsoft.bookkeeping.persistence.v2;

import se.swedsoft.bookkeeping.data.common.SSDeliveryTerm;
import se.swedsoft.bookkeeping.data.system.SSDB;
import se.swedsoft.bookkeeping.persistence.DeliveryTermRepository;

import java.util.List;
import java.util.Optional;

/**
 * V2 {@link DeliveryTermRepository} implementation backed by the normalized V2 schema.
 *
 * <p>Delegates CRUD operations to {@link SSDB}, which internally routes all reads
 * and writes against the relational {@code tbl_deliveryterm} table when
 * {@code fribok.schema.version=v2} is active.</p>
 */
public class V2DeliveryTermRepository implements DeliveryTermRepository {

    private final SSDB db;

    /**
     * Creates a V2 delivery-term repository backed by the given {@link SSDB} instance.
     *
     * @param db the SSDB instance; must not be {@code null}
     * @throws NullPointerException if {@code db} is {@code null}
     */
    public V2DeliveryTermRepository(SSDB db) {
        if (db == null) {
            throw new NullPointerException("db must not be null");
        }
        this.db = db;
    }

    /** {@inheritDoc} */
    @Override
    public List<SSDeliveryTerm> findAll() {
        return db.getDeliveryTerms();
    }

    /** {@inheritDoc} */
    @Override
    public Optional<SSDeliveryTerm> findByName(String name) {
        if (name == null) {
            return Optional.empty();
        }
        return db.getDeliveryTerm(name);
    }

    /** {@inheritDoc} */
    @Override
    public void add(SSDeliveryTerm deliveryTerm) {
        db.addDeliveryTerm(deliveryTerm);
    }

    /** {@inheritDoc} */
    @Override
    public void update(SSDeliveryTerm deliveryTerm) {
        db.updateDeliveryTerm(deliveryTerm);
    }

    /** {@inheritDoc} */
    @Override
    public void delete(SSDeliveryTerm deliveryTerm) {
        db.deleteDeliveryTerm(deliveryTerm);
    }
}

