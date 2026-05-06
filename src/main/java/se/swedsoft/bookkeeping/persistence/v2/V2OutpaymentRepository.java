package se.swedsoft.bookkeeping.persistence.v2;

import se.swedsoft.bookkeeping.data.SSOutpayment;
import se.swedsoft.bookkeeping.data.system.SSDB;
import se.swedsoft.bookkeeping.persistence.OutpaymentRepository;

import java.util.List;
import java.util.Optional;

/**
 * V2 {@link OutpaymentRepository} implementation backed by the V2 schema.
 */
public class V2OutpaymentRepository implements OutpaymentRepository {

    private static final String SCHEMA_PROPERTY = "fribok.schema.version";

    private final SSDB db;

    /**
     * Creates a V2 outpayment repository backed by the given {@link SSDB} instance.
     *
     * @param db the SSDB instance; must not be {@code null}
     * @throws NullPointerException  if {@code db} is {@code null}
     * @throws IllegalStateException if {@code fribok.schema.version} is not {@code v2}
     */
    public V2OutpaymentRepository(SSDB db) {
        if (db == null) {
            throw new NullPointerException("db must not be null");
        }
        if (!"v2".equalsIgnoreCase(System.getProperty(SCHEMA_PROPERTY, "v1"))) {
            throw new IllegalStateException("V2OutpaymentRepository requires fribok.schema.version=v2");
        }
        this.db = db;
    }

    /** {@inheritDoc} */
    @Override
    public List<SSOutpayment> findAll() {
        return db.getOutpayments();
    }

    /** {@inheritDoc} */
    @Override
    public Optional<SSOutpayment> findByOutpayment(SSOutpayment outpayment) {
        return db.getOutpayment(outpayment);
    }

    /** {@inheritDoc} */
    @Override
    public void add(SSOutpayment outpayment) {
        db.addOutpayment(outpayment);
    }

    /** {@inheritDoc} */
    @Override
    public void update(SSOutpayment outpayment) {
        db.updateOutpayment(outpayment);
    }

    /** {@inheritDoc} */
    @Override
    public void delete(SSOutpayment outpayment) {
        db.deleteOutpayment(outpayment);
    }
}

