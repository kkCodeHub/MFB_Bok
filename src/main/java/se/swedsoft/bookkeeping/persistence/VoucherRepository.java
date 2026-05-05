package se.swedsoft.bookkeeping.persistence;

import se.swedsoft.bookkeeping.data.SSNewAccountingYear;
import se.swedsoft.bookkeeping.data.SSVoucher;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for {@link SSVoucher} persistence operations.
 */
public interface VoucherRepository {

    /**
     * Returns all vouchers for a specific accounting year.
     *
     * @param year accounting year
     * @return vouchers in that year
     */
    List<SSVoucher> findByYear(SSNewAccountingYear year);

    /**
     * Looks up a voucher by number in a specific accounting year.
     *
     * @param year accounting year
     * @param number voucher number
     * @return matching voucher, or empty when not found
     */
    Optional<SSVoucher> findByNumber(SSNewAccountingYear year, int number);

    /**
     * Persists a new voucher.
     *
     * @param voucher voucher to add
     */
    void add(SSVoucher voucher);

    /**
     * Updates an existing voucher.
     *
     * @param voucher voucher to update
     */
    void update(SSVoucher voucher);

    /**
     * Deletes an existing voucher.
     *
     * @param voucher voucher to delete
     */
    void delete(SSVoucher voucher);
}

