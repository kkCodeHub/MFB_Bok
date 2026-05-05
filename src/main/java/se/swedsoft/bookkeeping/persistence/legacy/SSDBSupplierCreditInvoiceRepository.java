package se.swedsoft.bookkeeping.persistence.legacy;

import se.swedsoft.bookkeeping.data.SSSupplierCreditInvoice;
import se.swedsoft.bookkeeping.data.system.SSDB;
import se.swedsoft.bookkeeping.persistence.SupplierCreditInvoiceRepository;

import java.util.List;
import java.util.Optional;

/**
 * Legacy {@link SupplierCreditInvoiceRepository} implementation delegating to {@link SSDB}.
 */
public class SSDBSupplierCreditInvoiceRepository implements SupplierCreditInvoiceRepository {

    private final SSDB db;

    /**
     * Creates a repository backed by the given {@link SSDB} instance.
     *
     * @param db the SSDB instance to delegate to; must not be {@code null}
     */
    public SSDBSupplierCreditInvoiceRepository(SSDB db) {
        if (db == null) {
            throw new NullPointerException("db must not be null");
        }
        this.db = db;
    }

    /** {@inheritDoc} */
    @Override
    public List<SSSupplierCreditInvoice> findAll() {
        return db.getSupplierCreditInvoices();
    }

    /** {@inheritDoc} */
    @Override
    public Optional<SSSupplierCreditInvoice> findBySupplierCreditInvoice(
            SSSupplierCreditInvoice supplierCreditInvoice) {
        return db.getSupplierCreditInvoice(supplierCreditInvoice);
    }

    /** {@inheritDoc} */
    @Override
    public void add(SSSupplierCreditInvoice supplierCreditInvoice) {
        db.addSupplierCreditInvoice(supplierCreditInvoice);
    }

    /** {@inheritDoc} */
    @Override
    public void update(SSSupplierCreditInvoice supplierCreditInvoice) {
        db.updateSupplierCreditInvoice(supplierCreditInvoice);
    }

    /** {@inheritDoc} */
    @Override
    public void delete(SSSupplierCreditInvoice supplierCreditInvoice) {
        db.deleteSupplierCreditInvoice(supplierCreditInvoice);
    }
}

