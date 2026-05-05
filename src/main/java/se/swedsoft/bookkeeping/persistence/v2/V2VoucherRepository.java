package se.swedsoft.bookkeeping.persistence.v2;

import se.swedsoft.bookkeeping.data.SSNewAccountingYear;
import se.swedsoft.bookkeeping.data.SSVoucher;
import se.swedsoft.bookkeeping.data.system.SSDB;
import se.swedsoft.bookkeeping.persistence.VoucherRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * V2 {@link VoucherRepository} implementation backed by {@link SSDB}.
 */
public class V2VoucherRepository implements VoucherRepository {

    private static final String SCHEMA_PROPERTY = "fribok.schema.version";

    private final SSDB db;

    public V2VoucherRepository(SSDB db) {
        if (db == null) {
            throw new NullPointerException("db must not be null");
        }
        if (!"v2".equalsIgnoreCase(System.getProperty(SCHEMA_PROPERTY, "v1"))) {
            throw new IllegalStateException("V2VoucherRepository requires fribok.schema.version=v2");
        }
        this.db = db;
    }

    @Override
    public List<SSVoucher> findByYear(SSNewAccountingYear year) {
        return db.getVouchers(year);
    }

    @Override
    public Optional<SSVoucher> findByNumber(SSNewAccountingYear year, int number) {
        return db.getVoucher(year, number);
    }

    @Override
    public void add(SSVoucher voucher) {
        db.setCurrentYear(resolveYear(voucher));
        db.addVoucher(voucher, true);
    }

    @Override
    public void update(SSVoucher voucher) {
        db.setCurrentYear(resolveYear(voucher));
        db.updateVoucher(voucher);
    }

    @Override
    public void delete(SSVoucher voucher) {
        db.setCurrentYear(resolveYear(voucher));
        db.deleteVoucher(voucher);
    }

    private SSNewAccountingYear resolveYear(SSVoucher voucher) {
        if (voucher == null) {
            throw new NullPointerException("voucher must not be null");
        }

        LocalDate voucherDate = voucher.getLocalDate();
        if (voucherDate == null) {
            throw new IllegalArgumentException("voucher date must be set to resolve accounting year");
        }

        for (SSNewAccountingYear year : db.getYears()) {
            LocalDate from = year.getLocalFrom();
            LocalDate to = year.getLocalTo();
            if (from != null && to != null && !voucherDate.isBefore(from) && !voucherDate.isAfter(to)) {
                return year;
            }
        }

        throw new IllegalArgumentException("No accounting year matches voucher date: " + voucherDate);
    }
}

