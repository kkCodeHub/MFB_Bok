package se.swedsoft.bookkeeping.persistence;

import se.swedsoft.bookkeeping.data.common.SSCurrency;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for {@link SSCurrency} persistence operations.
 *
 * <p>Implementations decouple the rest of the application from the
 * underlying storage mechanism (currently HSQLDB via {@code SSDB}).
 * Reference data (currencies) is global — not per company.</p>
 */
public interface CurrencyRepository {

    /**
     * Returns all currencies.
     *
     * @return mutable list of currencies; never {@code null}
     */
    List<SSCurrency> findAll();

    /**
     * Looks up a currency by its code.
     *
     * @param code the currency code; must not be {@code null}
     * @return an {@link Optional} containing the currency, or empty if not found
     */
    Optional<SSCurrency> findByCode(String code);

    /**
     * Persists a new currency.
     *
     * @param currency the currency to add; must not be {@code null}
     */
    void add(SSCurrency currency);

    /**
     * Updates an existing currency record.
     *
     * @param currency the currency with updated values; must not be {@code null}
     */
    void update(SSCurrency currency);

    /**
     * Deletes a currency.
     *
     * @param currency the currency to delete; must not be {@code null}
     */
    void delete(SSCurrency currency);
}

