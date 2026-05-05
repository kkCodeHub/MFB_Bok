package se.swedsoft.bookkeeping.persistence.v2;

import se.swedsoft.bookkeeping.data.SSSupplierCreditInvoice;
import se.swedsoft.bookkeeping.data.system.SSDB;
import se.swedsoft.bookkeeping.persistence.SupplierCreditInvoiceRepository;

import java.util.List;
import java.util.Optional;

/**
 * V2 {@link SupplierCreditInvoiceRepository} implementation backed by the V2 schema.
 */
public class V2SupplierCreditInvoiceRepository implements SupplierCreditInvoiceRepository {

    private static final String SCHEMA_PROPERTY = "fribok.schema.version";

    private final SSDB db;

    /**
     * Creates a V2 supplier-credit-invoice repository backed by the given {@link SSDB} instance.
     *
     * @param db the SSDB instance; must not be {@code null}
     * @throws NullPointerException  if {@code db} is {@code null}
     * @throws IllegalStateException if {@code fribok.schema.version} is not {@code v2}
     */
    public V2SupplierCreditInvoiceRepository(SSDB db) {
        if (db == null) {
            throw new NullPointerException("db must not be null");
        }
        if (!"v2".equalsIgnoreCase(System.getProperty(SCHEMA_PROPERTY, "v1"))) {
            throw new IllegalStateException(
                    "V2SupplierCreditInvoiceRepository requires fribok.schema.version=v2");
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

