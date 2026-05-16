package se.swedsoft.bookkeeping.persistence.v2;

import se.swedsoft.bookkeeping.data.common.SSPaymentTerm;
import se.swedsoft.bookkeeping.data.system.SSDB;
import se.swedsoft.bookkeeping.persistence.PaymentTermRepository;

import java.util.List;
import java.util.Optional;

/**
 * V2 {@link PaymentTermRepository} implementation backed by the normalized V2 schema.
 *
 * <p>Delegates CRUD operations to {@link SSDB}, which internally routes all reads
 * and writes against the relational {@code tbl_paymentterm} table when
 * {@code fribok.schema.version=v2} is active.</p>
 */
public class V2PaymentTermRepository implements PaymentTermRepository {

    private final SSDB db;

    /**
     * Creates a V2 payment-term repository backed by the given {@link SSDB} instance.
     *
     * @param db the SSDB instance; must not be {@code null}
     * @throws NullPointerException if {@code db} is {@code null}
     */
    public V2PaymentTermRepository(SSDB db) {
        if (db == null) {
            throw new NullPointerException("db must not be null");
        }
        this.db = db;
    }

    /** {@inheritDoc} */
    @Override
    public List<SSPaymentTerm> findAll() {
        return db.getPaymentTerms();
    }

    /** {@inheritDoc} */
    @Override
    public Optional<SSPaymentTerm> findByName(String name) {
        if (name == null) {
            return Optional.empty();
        }
        return db.getPaymentTerm(name);
    }

    /** {@inheritDoc} */
    @Override
    public void add(SSPaymentTerm paymentTerm) {
        db.addPaymentTerm(paymentTerm);
    }

    /** {@inheritDoc} */
    @Override
    public void update(SSPaymentTerm paymentTerm) {
        db.updatePaymentTerm(paymentTerm);
    }

    /** {@inheritDoc} */
    @Override
    public void delete(SSPaymentTerm paymentTerm) {
        db.deletePaymentTerm(paymentTerm);
    }
}

