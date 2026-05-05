package se.swedsoft.bookkeeping.persistence.v2;

import se.swedsoft.bookkeeping.data.SSInvoice;
import se.swedsoft.bookkeeping.data.system.SSDB;
import se.swedsoft.bookkeeping.persistence.InvoiceRepository;

import java.util.List;
import java.util.Optional;

/**
 * V2 {@link InvoiceRepository} implementation backed by the V2 schema.
 */
public class V2InvoiceRepository implements InvoiceRepository {

    private static final String SCHEMA_PROPERTY = "fribok.schema.version";

    private final SSDB db;

    /**
     * Creates a V2 invoice repository backed by the given {@link SSDB} instance.
     *
     * @param db the SSDB instance; must not be {@code null}
     * @throws NullPointerException  if {@code db} is {@code null}
     * @throws IllegalStateException if {@code fribok.schema.version} is not {@code v2}
     */
    public V2InvoiceRepository(SSDB db) {
        if (db == null) {
            throw new NullPointerException("db must not be null");
        }
        if (!"v2".equalsIgnoreCase(System.getProperty(SCHEMA_PROPERTY, "v1"))) {
            throw new IllegalStateException("V2InvoiceRepository requires fribok.schema.version=v2");
        }
        this.db = db;
    }

    /** {@inheritDoc} */
    @Override
    public List<SSInvoice> findAll() {
        return db.getInvoices();
    }

    /** {@inheritDoc} */
    @Override
    public Optional<SSInvoice> findByInvoice(SSInvoice invoice) {
        return db.getInvoice(invoice);
    }

    /** {@inheritDoc} */
    @Override
    public List<SSInvoice> findAll(List<SSInvoice> subset) {
        return db.getInvoices(subset);
    }

    /** {@inheritDoc} */
    @Override
    public void add(SSInvoice invoice) {
        db.addInvoice(invoice);
    }

    /** {@inheritDoc} */
    @Override
    public void update(SSInvoice invoice) {
        db.updateInvoice(invoice);
    }

    /** {@inheritDoc} */
    @Override
    public void delete(SSInvoice invoice) {
        db.deleteInvoice(invoice);
    }
}

