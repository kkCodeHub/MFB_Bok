package se.swedsoft.bookkeeping.persistence.legacy;

import se.swedsoft.bookkeeping.data.SSCreditInvoice;
import se.swedsoft.bookkeeping.data.system.SSDB;
import se.swedsoft.bookkeeping.persistence.CreditInvoiceRepository;

import java.util.List;
import java.util.Optional;

/**
 * Legacy {@link CreditInvoiceRepository} implementation delegating to {@link SSDB}.
 */
public class SSDBCreditInvoiceRepository implements CreditInvoiceRepository {

    private final SSDB db;

    /**
     * Creates a repository backed by the given {@link SSDB} instance.
     *
     * @param db the SSDB instance to delegate to; must not be {@code null}
     */
    public SSDBCreditInvoiceRepository(SSDB db) {
        if (db == null) {
            throw new NullPointerException("db must not be null");
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

