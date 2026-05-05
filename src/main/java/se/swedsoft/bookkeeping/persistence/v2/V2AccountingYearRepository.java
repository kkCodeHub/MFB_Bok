package se.swedsoft.bookkeeping.persistence.v2;

import se.swedsoft.bookkeeping.data.SSNewAccountingYear;
import se.swedsoft.bookkeeping.data.system.SSDB;
import se.swedsoft.bookkeeping.persistence.AccountingYearRepository;

import java.util.List;
import java.util.Optional;

/**
 * V2 {@link AccountingYearRepository} implementation backed by {@link SSDB}.
 */
public class V2AccountingYearRepository implements AccountingYearRepository {

    private static final String SCHEMA_PROPERTY = "fribok.schema.version";

    private final SSDB db;

    public V2AccountingYearRepository(SSDB db) {
        if (db == null) {
            throw new NullPointerException("db must not be null");
        }
        if (!"v2".equalsIgnoreCase(System.getProperty(SCHEMA_PROPERTY, "v1"))) {
            throw new IllegalStateException("V2AccountingYearRepository requires fribok.schema.version=v2");
        }
        this.db = db;
    }

    @Override
    public List<SSNewAccountingYear> findAll() {
        return db.getYears();
    }

    @Override
    public Optional<SSNewAccountingYear> findCurrent() {
        return Optional.ofNullable(db.getCurrentYear());
    }

    @Override
    public void add(SSNewAccountingYear year) {
        db.addAccountingYear(year);
    }

    @Override
    public void update(SSNewAccountingYear year) {
        db.updateAccountingYear(year);
    }

    @Override
    public void delete(SSNewAccountingYear year) {
        db.deleteAccountingYear(year);
    }
}

