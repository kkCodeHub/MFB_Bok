package se.swedsoft.bookkeeping.persistence.v2;

import se.swedsoft.bookkeeping.data.common.SSUnit;
import se.swedsoft.bookkeeping.data.system.SSDB;
import se.swedsoft.bookkeeping.persistence.UnitRepository;

import java.util.List;
import java.util.Optional;

/**
 * V2 {@link UnitRepository} implementation backed by the normalized V2 schema.
 *
 * <p>Delegates CRUD operations to {@link SSDB}, which internally routes all reads
 * and writes against the relational {@code tbl_unit} table when
 * {@code fribok.schema.version=v2} is active.</p>
 */
public class V2UnitRepository implements UnitRepository {

    private final SSDB db;

    /**
     * Creates a V2 unit repository backed by the given {@link SSDB} instance.
     *
     * @param db the SSDB instance; must not be {@code null}
     * @throws NullPointerException if {@code db} is {@code null}
     */
    public V2UnitRepository(SSDB db) {
        if (db == null) {
            throw new NullPointerException("db must not be null");
        }
        this.db = db;
    }

    /** {@inheritDoc} */
    @Override
    public List<SSUnit> findAll() {
        return db.getUnits();
    }

    /** {@inheritDoc} */
    @Override
    public Optional<SSUnit> findByName(String name) {
        if (name == null) {
            return Optional.empty();
        }
        return db.getUnit(name);
    }

    /** {@inheritDoc} */
    @Override
    public void add(SSUnit unit) {
        db.addUnit(unit);
    }

    /** {@inheritDoc} */
    @Override
    public void update(SSUnit unit) {
        db.updateUnit(unit);
    }

    /** {@inheritDoc} */
    @Override
    public void delete(SSUnit unit) {
        db.deleteUnit(unit);
    }
}

