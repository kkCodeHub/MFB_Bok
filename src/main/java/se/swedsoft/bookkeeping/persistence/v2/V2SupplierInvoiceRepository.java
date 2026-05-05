package se.swedsoft.bookkeeping.persistence.v2;

import se.swedsoft.bookkeeping.data.SSSupplierInvoice;
import se.swedsoft.bookkeeping.data.system.SSDB;
import se.swedsoft.bookkeeping.persistence.SupplierInvoiceRepository;

import java.util.List;
import java.util.Optional;

/**
 * V2 {@link SupplierInvoiceRepository} implementation backed by the V2 schema.
 */
public class V2SupplierInvoiceRepository implements SupplierInvoiceRepository {

    private static final String SCHEMA_PROPERTY = "fribok.schema.version";

    private final SSDB db;

    /**
     * Creates a V2 supplier-invoice repository backed by the given {@link SSDB} instance.
     *
     * @param db the SSDB instance; must not be {@code null}
     * @throws NullPointerException  if {@code db} is {@code null}
     * @throws IllegalStateException if {@code fribok.schema.version} is not {@code v2}
     */
    public V2SupplierInvoiceRepository(SSDB db) {
        if (db == null) {
            throw new NullPointerException("db must not be null");
        }
        if (!"v2".equalsIgnoreCase(System.getProperty(SCHEMA_PROPERTY, "v1"))) {
            throw new IllegalStateException(
                    "V2SupplierInvoiceRepository requires fribok.schema.version=v2");
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

