package se.swedsoft.bookkeeping.persistence.v2;

import se.swedsoft.bookkeeping.data.SSOutdelivery;
import se.swedsoft.bookkeeping.data.system.SSDB;
import se.swedsoft.bookkeeping.persistence.OutdeliveryRepository;

import java.util.List;
import java.util.Optional;

/**
 * V2 {@link OutdeliveryRepository} implementation backed by the V2 schema.
 */
public class V2OutdeliveryRepository implements OutdeliveryRepository {

    private static final String SCHEMA_PROPERTY = "fribok.schema.version";

    private final SSDB db;

    /**
     * Creates a V2 outdelivery repository backed by the given {@link SSDB} instance.
     *
     * @param db the SSDB instance; must not be {@code null}
     * @throws NullPointerException  if {@code db} is {@code null}
     * @throws IllegalStateException if {@code fribok.schema.version} is not {@code v2}
     */
    public V2OutdeliveryRepository(SSDB db) {
        if (db == null) {
            throw new NullPointerException("db must not be null");
        }
        if (!"v2".equalsIgnoreCase(System.getProperty(SCHEMA_PROPERTY, "v1"))) {
            throw new IllegalStateException("V2OutdeliveryRepository requires fribok.schema.version=v2");
        }
        this.db = db;
    }

    /** {@inheritDoc} */
    @Override
    public List<SSOutdelivery> findAll() {
        return db.getOutdeliveries();
    }

    /** {@inheritDoc} */
    @Override
    public Optional<SSOutdelivery> findByOutdelivery(SSOutdelivery outdelivery) {
        return db.getOutdelivery(outdelivery);
    }

    /** {@inheritDoc} */
    @Override
    public void add(SSOutdelivery outdelivery) {
        db.addOutdelivery(outdelivery);
    }

    /** {@inheritDoc} */
    @Override
    public void update(SSOutdelivery outdelivery) {
        db.updateOutdelivery(outdelivery);
    }

    /** {@inheritDoc} */
    @Override
    public void delete(SSOutdelivery outdelivery) {
        db.deleteOutdelivery(outdelivery);
    }
}

