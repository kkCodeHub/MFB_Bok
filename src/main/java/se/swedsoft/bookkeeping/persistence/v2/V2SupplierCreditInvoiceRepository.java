package se.swedsoft.bookkeeping.persistence.v2;

import se.swedsoft.bookkeeping.data.SSSupplierCreditInvoice;
import se.swedsoft.bookkeeping.data.system.SSDB;
import se.swedsoft.bookkeeping.persistence.SupplierCreditInvoiceRepository;

import java.util.List;
import java.util.Optional;

/**
 * V2 {@link SupplierCreditInvoiceRepository} implementation for the
 * post-cutover supplier-credit-invoice domain.
 */
public class V2SupplierCreditInvoiceRepository implements SupplierCreditInvoiceRepository {

    private final SSDB db;

    /**
     * Creates a V2 supplier-credit-invoice repository backed by the given {@link SSDB} instance.
     *
     * @param db the SSDB instance; must not be {@code null}
     * @throws NullPointerException if {@code db} is {@code null}
     */
    public V2SupplierCreditInvoiceRepository(SSDB db) {
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

