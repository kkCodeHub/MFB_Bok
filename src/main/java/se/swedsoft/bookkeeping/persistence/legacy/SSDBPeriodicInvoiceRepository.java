package se.swedsoft.bookkeeping.persistence.legacy;

import se.swedsoft.bookkeeping.data.SSPeriodicInvoice;
import se.swedsoft.bookkeeping.data.system.SSDB;
import se.swedsoft.bookkeeping.persistence.PeriodicInvoiceRepository;

import java.util.List;
import java.util.Optional;

/**
 * Legacy {@link PeriodicInvoiceRepository} implementation delegating to {@link SSDB}.
 */
public class SSDBPeriodicInvoiceRepository implements PeriodicInvoiceRepository {

    private final SSDB db;

    /**
     * Creates a repository backed by the given {@link SSDB} instance.
     *
     * @param db the SSDB instance to delegate to; must not be {@code null}
     */
    public SSDBPeriodicInvoiceRepository(SSDB db) {
        if (db == null) {
            throw new NullPointerException("db must not be null");
        }
        this.db = db;
    }

    /** {@inheritDoc} */
    @Override
    public List<SSPeriodicInvoice> findAll() {
        return db.getPeriodicInvoices();
    }

    /** {@inheritDoc} */
    @Override
    public Optional<SSPeriodicInvoice> findByPeriodicInvoice(SSPeriodicInvoice periodicInvoice) {
        return db.getPeriodicInvoice(periodicInvoice);
    }

    /** {@inheritDoc} */
    @Override
    public void add(SSPeriodicInvoice periodicInvoice) {
        db.addPeriodicInvoice(periodicInvoice);
    }

    /** {@inheritDoc} */
    @Override
    public void update(SSPeriodicInvoice periodicInvoice) {
        db.updatePeriodicInvoice(periodicInvoice);
    }

    /** {@inheritDoc} */
    @Override
    public void delete(SSPeriodicInvoice periodicInvoice) {
        db.deletePeriodicInvoice(periodicInvoice);
    }
}

