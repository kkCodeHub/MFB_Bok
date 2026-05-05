package se.swedsoft.bookkeeping.persistence.legacy;

import se.swedsoft.bookkeeping.data.SSTender;
import se.swedsoft.bookkeeping.data.system.SSDB;
import se.swedsoft.bookkeeping.persistence.TenderRepository;

import java.util.List;
import java.util.Optional;

/**
 * Legacy {@link TenderRepository} implementation delegating to {@link SSDB}.
 */
public class SSDBTenderRepository implements TenderRepository {

    private final SSDB db;

    /**
     * Creates a repository backed by the given {@link SSDB} instance.
     *
     * @param db the SSDB instance to delegate to; must not be {@code null}
     */
    public SSDBTenderRepository(SSDB db) {
        if (db == null) {
            throw new NullPointerException("db must not be null");
        }
        this.db = db;
    }

    /** {@inheritDoc} */
    @Override
    public List<SSTender> findAll() {
        return db.getTenders();
    }

    /** {@inheritDoc} */
    @Override
    public Optional<SSTender> findByTender(SSTender tender) {
        return db.getTender(tender);
    }

    /** {@inheritDoc} */
    @Override
    public List<SSTender> findAll(List<SSTender> subset) {
        return db.getTenders(subset);
    }

    /** {@inheritDoc} */
    @Override
    public void add(SSTender tender) {
        db.addTender(tender);
    }

    /** {@inheritDoc} */
    @Override
    public void update(SSTender tender) {
        db.updateTender(tender);
    }

    /** {@inheritDoc} */
    @Override
    public void delete(SSTender tender) {
        db.deleteTender(tender);
    }
}

