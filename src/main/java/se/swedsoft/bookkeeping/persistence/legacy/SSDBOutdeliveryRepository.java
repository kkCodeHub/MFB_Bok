package se.swedsoft.bookkeeping.persistence.legacy;

import se.swedsoft.bookkeeping.data.SSOutdelivery;
import se.swedsoft.bookkeeping.data.system.SSDB;
import se.swedsoft.bookkeeping.persistence.OutdeliveryRepository;

import java.util.List;
import java.util.Optional;

/**
 * Legacy {@link OutdeliveryRepository} implementation delegating to {@link SSDB}.
 */
public class SSDBOutdeliveryRepository implements OutdeliveryRepository {

    private final SSDB db;

    /**
     * Creates a repository backed by the given {@link SSDB} instance.
     *
     * @param db the SSDB instance to delegate to; must not be {@code null}
     */
    public SSDBOutdeliveryRepository(SSDB db) {
        if (db == null) {
            throw new NullPointerException("db must not be null");
        }
        this.db = db;
    }

    /** {@inheritDoc} */
    @Override
    public List<SSOutdelivery> findAll() {
        return db.getOutdeliveries();
    }

    /** {@inheritDoc} */
    @Override
    public Optional<SSOutdelivery> findByOutdelivery(SSOutdelivery outdelivery) {
        return db.getOutdelivery(outdelivery);
    }

    /** {@inheritDoc} */
    @Override
    public void add(SSOutdelivery outdelivery) {
        db.addOutdelivery(outdelivery);
    }

    /** {@inheritDoc} */
    @Override
    public void update(SSOutdelivery outdelivery) {
        db.updateOutdelivery(outdelivery);
    }

    /** {@inheritDoc} */
    @Override
    public void delete(SSOutdelivery outdelivery) {
        db.deleteOutdelivery(outdelivery);
    }
}

