package se.swedsoft.bookkeeping.persistence.legacy;

import se.swedsoft.bookkeeping.data.SSNewAccountingYear;
import se.swedsoft.bookkeeping.data.system.SSDB;
import se.swedsoft.bookkeeping.persistence.AccountingYearRepository;

import java.util.List;
import java.util.Optional;

/**
 * Legacy {@link AccountingYearRepository} implementation backed by {@link SSDB}.
 */
public class SSDBAccountingYearRepository implements AccountingYearRepository {

    private final SSDB db;

    public SSDBAccountingYearRepository(SSDB db) {
        if (db == null) {
            throw new NullPointerException("db must not be null");
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

    @Override
    public void open(SSNewAccountingYear year) {
        db.openYear(year);
    }

    @Override
    public void close(SSNewAccountingYear year) {
        db.closeYear(year);
    }
}


