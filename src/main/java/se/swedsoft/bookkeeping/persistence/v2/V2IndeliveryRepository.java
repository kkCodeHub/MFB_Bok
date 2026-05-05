package se.swedsoft.bookkeeping.persistence.v2;

import se.swedsoft.bookkeeping.data.SSIndelivery;
import se.swedsoft.bookkeeping.data.system.SSDB;
import se.swedsoft.bookkeeping.persistence.IndeliveryRepository;

import java.util.List;
import java.util.Optional;

/**
 * V2 {@link IndeliveryRepository} implementation backed by the V2 schema.
 */
public class V2IndeliveryRepository implements IndeliveryRepository {

    private static final String SCHEMA_PROPERTY = "fribok.schema.version";

    private final SSDB db;

    /**
     * Creates a V2 indelivery repository backed by the given {@link SSDB} instance.
     *
     * @param db the SSDB instance; must not be {@code null}
     * @throws NullPointerException  if {@code db} is {@code null}
     * @throws IllegalStateException if {@code fribok.schema.version} is not {@code v2}
     */
    public V2IndeliveryRepository(SSDB db) {
        if (db == null) {
            throw new NullPointerException("db must not be null");
        }
        if (!"v2".equalsIgnoreCase(System.getProperty(SCHEMA_PROPERTY, "v1"))) {
            throw new IllegalStateException("V2IndeliveryRepository requires fribok.schema.version=v2");
        }
        this.db = db;
    }

    /** {@inheritDoc} */
    @Override
    public List<SSIndelivery> findAll() {
        return db.getIndeliveries();
    }

    /** {@inheritDoc} */
    @Override
    public Optional<SSIndelivery> findByIndelivery(SSIndelivery indelivery) {
        return db.getIndelivery(indelivery);
    }

    /** {@inheritDoc} */
    @Override
    public void add(SSIndelivery indelivery) {
        db.addIndelivery(indelivery);
    }

    /** {@inheritDoc} */
    @Override
    public void update(SSIndelivery indelivery) {
        db.updateIndelivery(indelivery);
    }

    /** {@inheritDoc} */
    @Override
    public void delete(SSIndelivery indelivery) {
        db.deleteIndelivery(indelivery);
    }
}

