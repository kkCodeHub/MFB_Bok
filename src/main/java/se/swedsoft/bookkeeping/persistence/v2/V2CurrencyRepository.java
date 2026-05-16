package se.swedsoft.bookkeeping.persistence.v2;

import se.swedsoft.bookkeeping.data.common.SSCurrency;
import se.swedsoft.bookkeeping.data.system.SSDB;
import se.swedsoft.bookkeeping.persistence.CurrencyRepository;

import java.util.List;
import java.util.Optional;

/**
 * V2 {@link CurrencyRepository} implementation backed by the normalized V2 schema.
 *
 * <p>Delegates CRUD operations to {@link SSDB}, which internally routes all reads
 * and writes against the relational {@code tbl_currency} table when
 * {@code fribok.schema.version=v2} is active.</p>
 */
public class V2CurrencyRepository implements CurrencyRepository {

    private final SSDB db;

    /**
     * Creates a V2 currency repository backed by the given {@link SSDB} instance.
     *
     * @param db the SSDB instance; must not be {@code null}
     * @throws NullPointerException if {@code db} is {@code null}
     */
    public V2CurrencyRepository(SSDB db) {
        if (db == null) {
            throw new NullPointerException("db must not be null");
        }
        this.db = db;
    }

    /** {@inheritDoc} */
    @Override
    public List<SSCurrency> findAll() {
        return db.getCurrencies();
    }

    /** {@inheritDoc} */
    @Override
    public Optional<SSCurrency> findByCode(String code) {
        if (code == null) {
            return Optional.empty();
        }
        SSCurrency probe = new SSCurrency();
        probe.setName(code);
        return db.getCurrency(probe);
    }

    /** {@inheritDoc} */
    @Override
    public void add(SSCurrency currency) {
        db.addCurrency(currency);
    }

    /** {@inheritDoc} */
    @Override
    public void update(SSCurrency currency) {
        db.updateCurrency(currency);
    }

    /** {@inheritDoc} */
    @Override
    public void delete(SSCurrency currency) {
        db.deleteCurrency(currency);
    }
}

