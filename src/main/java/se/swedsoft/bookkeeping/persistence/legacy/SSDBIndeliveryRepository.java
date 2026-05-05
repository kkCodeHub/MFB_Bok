package se.swedsoft.bookkeeping.persistence.legacy;

import se.swedsoft.bookkeeping.data.SSIndelivery;
import se.swedsoft.bookkeeping.data.system.SSDB;
import se.swedsoft.bookkeeping.persistence.IndeliveryRepository;

import java.util.List;
import java.util.Optional;

/**
 * Legacy {@link IndeliveryRepository} implementation delegating to {@link SSDB}.
 */
public class SSDBIndeliveryRepository implements IndeliveryRepository {

    private final SSDB db;

    /**
     * Creates a repository backed by the given {@link SSDB} instance.
     *
     * @param db the SSDB instance to delegate to; must not be {@code null}
     */
    public SSDBIndeliveryRepository(SSDB db) {
        if (db == null) {
            throw new NullPointerException("db must not be null");
        }
        this.db = db;
    }

    /** {@inheritDoc} */
    @Override
    public List<SSIndelivery> findAll() {
        return db.getIndeliveries();
    }

    /** {@inheritDoc} */
    @Override
    public Optional<SSIndelivery> findByIndelivery(SSIndelivery indelivery) {
        return db.getIndelivery(indelivery);
    }

    /** {@inheritDoc} */
    @Override
    public void add(SSIndelivery indelivery) {
        db.addIndelivery(indelivery);
    }

    /** {@inheritDoc} */
    @Override
    public void update(SSIndelivery indelivery) {
        db.updateIndelivery(indelivery);
    }

    /** {@inheritDoc} */
    @Override
    public void delete(SSIndelivery indelivery) {
        db.deleteIndelivery(indelivery);
    }
}

