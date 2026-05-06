package se.swedsoft.bookkeeping.persistence.legacy;

import se.swedsoft.bookkeeping.data.SSInpayment;
import se.swedsoft.bookkeeping.data.system.SSDB;
import se.swedsoft.bookkeeping.persistence.InpaymentRepository;

import java.util.List;
import java.util.Optional;

/**
 * Legacy {@link InpaymentRepository} implementation delegating to {@link SSDB}.
 */
public class SSDBInpaymentRepository implements InpaymentRepository {

    private final SSDB db;

    /**
     * Creates a repository backed by the given {@link SSDB} instance.
     *
     * @param db the SSDB instance to delegate to; must not be {@code null}
     */
    public SSDBInpaymentRepository(SSDB db) {
        if (db == null) {
            throw new NullPointerException("db must not be null");
        }
        this.db = db;
    }

    /** {@inheritDoc} */
    @Override
    public List<SSInpayment> findAll() {
        return db.getInpayments();
    }

    /** {@inheritDoc} */
    @Override
    public Optional<SSInpayment> findByInpayment(SSInpayment inpayment) {
        return db.getInpayment(inpayment);
    }

    /** {@inheritDoc} */
    @Override
    public void add(SSInpayment inpayment) {
        db.addInpayment(inpayment);
    }

    /** {@inheritDoc} */
    @Override
    public void update(SSInpayment inpayment) {
        db.updateInpayment(inpayment);
    }

    /** {@inheritDoc} */
    @Override
    public void delete(SSInpayment inpayment) {
        db.deleteInpayment(inpayment);
    }
}

