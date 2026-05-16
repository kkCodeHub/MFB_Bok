package se.swedsoft.bookkeeping.persistence;

import se.swedsoft.bookkeeping.data.common.SSUnit;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for {@link SSUnit} persistence operations.
 *
 * <p>Implementations decouple the rest of the application from the
 * underlying storage mechanism (currently HSQLDB via {@code SSDB}).
 * Reference data (units) is global — not per company.</p>
 */
public interface UnitRepository {

    /**
     * Returns all units.
     *
     * @return mutable list of units; never {@code null}
     */
    List<SSUnit> findAll();

    /**
     * Looks up a unit by its name.
     *
     * @param name the unit name; must not be {@code null}
     * @return an {@link Optional} containing the unit, or empty if not found
     */
    Optional<SSUnit> findByName(String name);

    /**
     * Persists a new unit.
     *
     * @param unit the unit to add; must not be {@code null}
     */
    void add(SSUnit unit);

    /**
     * Updates an existing unit record (description only; name is the primary key).
     *
     * @param unit the unit with updated values; must not be {@code null}
     */
    void update(SSUnit unit);

    /**
     * Deletes a unit.
     *
     * @param unit the unit to delete; must not be {@code null}
     */
    void delete(SSUnit unit);
}

