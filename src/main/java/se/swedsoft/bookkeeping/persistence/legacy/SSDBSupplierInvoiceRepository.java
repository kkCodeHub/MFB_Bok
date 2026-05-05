package se.swedsoft.bookkeeping.persistence.legacy;

import se.swedsoft.bookkeeping.data.SSSupplierInvoice;
import se.swedsoft.bookkeeping.data.system.SSDB;
import se.swedsoft.bookkeeping.persistence.SupplierInvoiceRepository;

import java.util.List;
import java.util.Optional;

/**
 * Legacy {@link SupplierInvoiceRepository} implementation delegating to {@link SSDB}.
 */
public class SSDBSupplierInvoiceRepository implements SupplierInvoiceRepository {

    private final SSDB db;

    /**
     * Creates a repository backed by the given {@link SSDB} instance.
     *
     * @param db the SSDB instance to delegate to; must not be {@code null}
     */
    public SSDBSupplierInvoiceRepository(SSDB db) {
        if (db == null) {
            throw new NullPointerException("db must not be null");
        }
        this.db = db;
    }

    /** {@inheritDoc} */
    @Override
    public List<SSSupplierInvoice> findAll() {
        return db.getSupplierInvoices();
    }

    /** {@inheritDoc} */
    @Override
    public Optional<SSSupplierInvoice> findBySupplierInvoice(SSSupplierInvoice supplierInvoice) {
        return db.getSupplierInvoice(supplierInvoice);
    }

    /** {@inheritDoc} */
    @Override
    public List<SSSupplierInvoice> findAll(List<SSSupplierInvoice> subset) {
        return db.getSupplierInvoices(subset);
    }

    /** {@inheritDoc} */
    @Override
    public void add(SSSupplierInvoice supplierInvoice) {
        db.addSupplierInvoice(supplierInvoice);
    }

    /** {@inheritDoc} */
    @Override
    public void update(SSSupplierInvoice supplierInvoice) {
        db.updateSupplierInvoice(supplierInvoice);
    }

    /** {@inheritDoc} */
    @Override
    public void delete(SSSupplierInvoice supplierInvoice) {
        db.deleteSupplierInvoice(supplierInvoice);
    }
}

