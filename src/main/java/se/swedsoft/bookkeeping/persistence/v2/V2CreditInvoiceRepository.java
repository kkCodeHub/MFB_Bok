package se.swedsoft.bookkeeping.persistence.v2;

import se.swedsoft.bookkeeping.data.SSCreditInvoice;
import se.swedsoft.bookkeeping.data.system.SSDB;
import se.swedsoft.bookkeeping.persistence.CreditInvoiceRepository;

import java.util.List;
import java.util.Optional;

/**
 * V2 {@link CreditInvoiceRepository} implementation backed by the V2 schema.
 */
public class V2CreditInvoiceRepository implements CreditInvoiceRepository {

    private static final String SCHEMA_PROPERTY = "fribok.schema.version";

    private final SSDB db;

    /**
     * Creates a V2 credit-invoice repository backed by the given {@link SSDB} instance.
     *
     * @param db the SSDB instance; must not be {@code null}
     * @throws NullPointerException  if {@code db} is {@code null}
     * @throws IllegalStateException if {@code fribok.schema.version} is not {@code v2}
     */
    public V2CreditInvoiceRepository(SSDB db) {
        if (db == null) {
            throw new NullPointerException("db must not be null");
        }
        if (!"v2".equalsIgnoreCase(System.getProperty(SCHEMA_PROPERTY, "v1"))) {
            throw new IllegalStateException("V2CreditInvoiceRepository requires fribok.schema.version=v2");
        }
        this.db = db;
    }

    /** {@inheritDoc} */
    @Override
    public List<SSCreditInvoice> findAll() {
        return db.getCreditInvoices();
    }

    /** {@inheritDoc} */
    @Override
    public Optional<SSCreditInvoice> findByCreditInvoice(SSCreditInvoice creditInvoice) {
        return db.getCreditInvoice(creditInvoice);
    }

    /** {@inheritDoc} */
    @Override
    public List<SSCreditInvoice> findAll(List<SSCreditInvoice> subset) {
        return db.getCreditInvoices(subset);
    }

    /** {@inheritDoc} */
    @Override
    public void add(SSCreditInvoice creditInvoice) {
        db.addCreditInvoice(creditInvoice);
    }

    /** {@inheritDoc} */
    @Override
    public void update(SSCreditInvoice creditInvoice) {
        db.updateCreditInvoice(creditInvoice);
    }

    /** {@inheritDoc} */
    @Override
    public void delete(SSCreditInvoice creditInvoice) {
        db.deleteCreditInvoice(creditInvoice);
    }
}

