package se.swedsoft.bookkeeping.persistence.v2;

import se.swedsoft.bookkeeping.data.SSPeriodicInvoice;
import se.swedsoft.bookkeeping.data.system.SSDB;
import se.swedsoft.bookkeeping.persistence.PeriodicInvoiceRepository;

import java.util.List;
import java.util.Optional;

/**
 * V2 {@link PeriodicInvoiceRepository} implementation backed by the V2 schema.
 */
public class V2PeriodicInvoiceRepository implements PeriodicInvoiceRepository {

    private static final String SCHEMA_PROPERTY = "fribok.schema.version";

    private final SSDB db;

    /**
     * Creates a V2 periodic-invoice repository backed by the given {@link SSDB} instance.
     *
     * @param db the SSDB instance; must not be {@code null}
     * @throws NullPointerException  if {@code db} is {@code null}
     * @throws IllegalStateException if {@code fribok.schema.version} is not {@code v2}
     */
    public V2PeriodicInvoiceRepository(SSDB db) {
        if (db == null) {
            throw new NullPointerException("db must not be null");
        }
        if (!"v2".equalsIgnoreCase(System.getProperty(SCHEMA_PROPERTY, "v1"))) {
            throw new IllegalStateException("V2PeriodicInvoiceRepository requires fribok.schema.version=v2");
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

