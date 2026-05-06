package se.swedsoft.bookkeeping.persistence.legacy;

import se.swedsoft.bookkeeping.data.SSOutpayment;
import se.swedsoft.bookkeeping.data.system.SSDB;
import se.swedsoft.bookkeeping.persistence.OutpaymentRepository;

import java.util.List;
import java.util.Optional;

/**
 * Legacy {@link OutpaymentRepository} implementation delegating to {@link SSDB}.
 */
public class SSDBOutpaymentRepository implements OutpaymentRepository {

    private final SSDB db;

    /**
     * Creates a repository backed by the given {@link SSDB} instance.
     *
     * @param db the SSDB instance to delegate to; must not be {@code null}
     */
    public SSDBOutpaymentRepository(SSDB db) {
        if (db == null) {
            throw new NullPointerException("db must not be null");
        }
        this.db = db;
    }

    /** {@inheritDoc} */
    @Override
    public List<SSOutpayment> findAll() {
        return db.getOutpayments();
    }

    /** {@inheritDoc} */
    @Override
    public Optional<SSOutpayment> findByOutpayment(SSOutpayment outpayment) {
        return db.getOutpayment(outpayment);
    }

    /** {@inheritDoc} */
    @Override
    public void add(SSOutpayment outpayment) {
        db.addOutpayment(outpayment);
    }

    /** {@inheritDoc} */
    @Override
    public void update(SSOutpayment outpayment) {
        db.updateOutpayment(outpayment);
    }

    /** {@inheritDoc} */
    @Override
    public void delete(SSOutpayment outpayment) {
        db.deleteOutpayment(outpayment);
    }
}

