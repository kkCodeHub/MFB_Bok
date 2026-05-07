package se.swedsoft.bookkeeping.persistence.v2;

import se.swedsoft.bookkeeping.data.SSPeriodicInvoice;
import se.swedsoft.bookkeeping.data.system.SSDB;
import se.swedsoft.bookkeeping.persistence.PeriodicInvoiceRepository;

import java.util.List;
import java.util.Optional;

/**
 * V2 {@link PeriodicInvoiceRepository} implementation for the post-cutover
 * periodic-invoice domain.
 */
public class V2PeriodicInvoiceRepository implements PeriodicInvoiceRepository {

    private final SSDB db;

    /**
     * Creates a V2 periodic-invoice repository backed by the given {@link SSDB} instance.
     *
     * @param db the SSDB instance; must not be {@code null}
     * @throws NullPointerException if {@code db} is {@code null}
     */
    public V2PeriodicInvoiceRepository(SSDB db) {
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

