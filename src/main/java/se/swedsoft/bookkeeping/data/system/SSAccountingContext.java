package se.swedsoft.bookkeeping.data.system;

import se.swedsoft.bookkeeping.data.SSAccount;
import se.swedsoft.bookkeeping.data.SSAccountPlan;
import se.swedsoft.bookkeeping.data.SSAutoDist;
import se.swedsoft.bookkeeping.data.SSNewAccountingYear;
import se.swedsoft.bookkeeping.data.SSNewCompany;
import se.swedsoft.bookkeeping.data.SSNewProject;
import se.swedsoft.bookkeeping.data.SSNewResultUnit;
import se.swedsoft.bookkeeping.data.SSVoucher;
import se.swedsoft.bookkeeping.data.SSVoucherTemplate;
import se.swedsoft.bookkeeping.persistence.Repositories;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Domain service for the Accounting Core domain (domain #2 in the 12-domain target architecture).
 *
 * <p>Covers accounting years, account plans, vouchers, auto-distributions,
 * voucher templates, and shared accounting master data.</p>
 *
 * <p>All operations delegate to the repository layer via {@link Repositories}.</p>
 */
public final class SSAccountingContext {

    private SSAccountingContext() {
    }

    // -------------------------------------------------------------------------
    // Accounting Year operations
    // -------------------------------------------------------------------------

    /**
     * Returns the currently active accounting year.
     *
     * @return the active year, or {@code null} if none is selected
     */
    public static SSNewAccountingYear getCurrentYear() {
        return Repositories.accountingYears().findCurrent().orElse(null);
    }

    /**
     * Returns the current company from the Company/Year domain.
     *
     * @return the current company, or {@code null} if none is selected
     */
    public static SSNewCompany getCurrentCompany() {
        return SSCompanyYearContext.getCurrentCompany();
    }

    /**
     * Returns the underlying {@link SSDB} instance.
     *
     * @return the SSDB singleton; never {@code null} once initialised
     * @deprecated Access SSDB directly only when no repository alternative exists.
     */
    @Deprecated
    public static SSDB getDatabase() {
        return SSSystemConfigContext.getDatabase();
    }

    /**
     * Returns whether the given accounting year can be opened.
     *
     * @param year the year to check; must not be {@code null}
     * @return {@code true} if the year may be opened
     */
    public static boolean canOpenAccountingYear(SSNewAccountingYear year) {
        return Repositories.accountingYears().canOpen(year);
    }

    /**
     * Persists a new accounting year.
     *
     * @param year the accounting year to add; must not be {@code null}
     */
    public static void addAccountingYear(SSNewAccountingYear year) {
        Repositories.accountingYears().add(year);
    }

    /**
     * Updates an existing accounting year.
     *
     * @param year the accounting year with updated values; must not be {@code null}
     */
    public static void updateAccountingYear(SSNewAccountingYear year) {
        Repositories.accountingYears().update(year);
    }

    /**
     * Deletes an accounting year.
     *
     * @param year the accounting year to delete; must not be {@code null}
     */
    public static void deleteAccountingYear(SSNewAccountingYear year) {
        Repositories.accountingYears().delete(year);
    }

    // -------------------------------------------------------------------------
    // Account Plan operations
    // -------------------------------------------------------------------------

    /**
     * Returns all account plans for the current company.
     *
     * @return list of account plans; never {@code null}
     */
    public static List<SSAccountPlan> getAccountPlans() {
        return Repositories.accountPlans().findAll();
    }

    /**
     * Looks up an account plan by reference.
     *
     * @param plan reference account plan; may be {@code null}
     * @return an {@link Optional} containing the plan, or empty if not found or {@code plan} is null
     */
    public static Optional<SSAccountPlan> getAccountPlan(SSAccountPlan plan) {
        if (plan == null) {
            return Optional.empty();
        }
        return Repositories.accountPlans().findById(plan.getId());
    }

    /**
     * Deletes an account plan.
     *
     * @param plan the account plan to delete; must not be {@code null}
     */
    public static void deleteAccountPlan(SSAccountPlan plan) {
        Repositories.accountPlans().delete(plan);
    }

    /**
     * Persists a new account plan.
     *
     * @param plan the account plan to add; must not be {@code null}
     */
    public static void addAccountPlan(SSAccountPlan plan) {
        Repositories.accountPlans().add(plan);
    }

    /**
     * Updates an existing account plan.
     *
     * @param plan the account plan with updated values; must not be {@code null}
     */
    public static void updateAccountPlan(SSAccountPlan plan) {
        Repositories.accountPlans().update(plan);
    }

    /**
     * Returns the account plan for the currently active accounting year.
     *
     * @return current account plan, or a default empty plan if no year is active
     */
    public static SSAccountPlan getCurrentAccountPlan() {
        SSNewAccountingYear currentYear = getCurrentYear();
        if (currentYear != null) {
            return currentYear.getAccountPlan();
        }
        return new SSAccountPlan("Default");
    }

    /**
     * Returns an account from the current account plan by account number.
     *
     * @param accountNumber account number to look up
     * @return matching account, or {@code null} if no plan/account exists
     */
    public static SSAccount getAccount(Integer accountNumber) {
        SSAccount planAccount = null;
        SSAccountPlan plan = getCurrentAccountPlan();
        if (plan != null) {
            planAccount = plan.getAccount(accountNumber);
        }
        return planAccount;
    }

    // -------------------------------------------------------------------------
    // Voucher operations
    // -------------------------------------------------------------------------

    /**
     * Returns all vouchers for the current accounting year.
     *
     * @return list of vouchers; empty list if no year is active
     */
    public static List<SSVoucher> getVouchers() {
        SSNewAccountingYear currentYear = getCurrentYear();
        if (currentYear == null) {
            return Collections.emptyList();
        }
        return Repositories.vouchers().findByYear(currentYear);
    }

    /**
     * Returns all vouchers for the given accounting year.
     *
     * @param year the accounting year; may be {@code null}
     * @return list of vouchers; empty list if {@code year} is null
     */
    public static List<SSVoucher> getVouchers(SSNewAccountingYear year) {
        if (year == null) {
            return Collections.emptyList();
        }
        return Repositories.vouchers().findByYear(year);
    }

    /**
     * Looks up a voucher by reference in the current accounting year.
     *
     * @param voucher reference voucher; may be {@code null}
     * @return an {@link Optional} containing the voucher, or empty if not found
     */
    public static Optional<SSVoucher> getVoucher(SSVoucher voucher) {
        SSNewAccountingYear currentYear = getCurrentYear();
        if (currentYear == null || voucher == null) {
            return Optional.empty();
        }
        return Repositories.vouchers().findByNumber(currentYear, voucher.getNumber());
    }

    /**
     * Returns the highest voucher number in the current accounting year, or {@code 0} if none.
     *
     * @return last voucher number, or {@code 0}
     */
    public static int getLastVoucherNumber() {
        return Repositories.vouchers().findLastNumber();
    }

    /**
     * Returns whether the current year contains a voucher with the given number.
     *
     * @param voucherNumber voucher number to check; may be {@code null}
     * @return {@code true} if found
     */
    public static boolean hasVoucher(Integer voucherNumber) {
        SSNewAccountingYear currentYear = getCurrentYear();
        if (currentYear == null || voucherNumber == null) {
            return false;
        }
        return Repositories.vouchers().findByNumber(currentYear, voucherNumber).isPresent();
    }

    /**
     * Returns whether any account rows exist for the given accounting year id.
     *
     * @param yearId the accounting year database id
     * @return {@code true} if account rows exist
     */
    public static boolean hasAccountRowsForYear(Integer yearId) {
        return Repositories.accountingYears().hasAccountRows(yearId);
    }

    /**
     * Returns only those vouchers from {@code vouchers} that exist in the current year.
     *
     * @param vouchers candidate list; may be {@code null} or empty
     * @return filtered list; never {@code null}
     */
    public static List<SSVoucher> getVouchers(List<SSVoucher> vouchers) {
        if (vouchers == null || vouchers.isEmpty()) {
            return Collections.emptyList();
        }
        List<SSVoucher> matching = new ArrayList<>();
        for (SSVoucher voucher : vouchers) {
            getVoucher(voucher).ifPresent(matching::add);
        }
        return matching;
    }

    /**
     * Deletes a voucher.
     *
     * @param voucher the voucher to delete; must not be {@code null}
     */
    public static void deleteVoucher(SSVoucher voucher) {
        Repositories.vouchers().delete(voucher);
    }

    /**
     * Persists a new voucher.
     *
     * <p>When {@code iHasNumber} is {@code true} the voucher number is preserved as-is.
     * When {@code false} the repository assigns the next available number.</p>
     *
     * @param voucher     the voucher to add; must not be {@code null}
     * @param iHasNumber  {@code true} if the voucher already carries a valid number
     */
    public static void addVoucher(SSVoucher voucher, boolean iHasNumber) {
        if (iHasNumber) {
            Repositories.vouchers().add(voucher);
        } else {
            Repositories.vouchers().addWithAutoNumber(voucher);
        }
    }

    /**
     * Updates an existing voucher.
     *
     * @param voucher the voucher with updated values; must not be {@code null}
     */
    public static void updateVoucher(SSVoucher voucher) {
        Repositories.vouchers().update(voucher);
    }

    // -------------------------------------------------------------------------
    // AutoDist operations
    // -------------------------------------------------------------------------

    /**
     * Returns all auto-distributions for the current company.
     *
     * @return list of auto-distributions; never {@code null}
     */
    public static List<SSAutoDist> getAutoDists() {
        return Repositories.autoDists().findAll();
    }

    /**
     * Looks up an auto-distribution by reference.
     *
     * @param autoDist reference auto-distribution; must not be {@code null}
     * @return an {@link Optional} containing the auto-distribution, or empty if not found
     */
    public static Optional<SSAutoDist> getAutoDist(SSAutoDist autoDist) {
        return Repositories.autoDists().findByAutoDist(autoDist);
    }

    /**
     * Returns only those auto-distributions from {@code autoDists} that exist in the repository.
     *
     * @param autoDists candidate list; must not be {@code null}
     * @return filtered list; never {@code null}
     */
    public static List<SSAutoDist> getAutoDists(List<SSAutoDist> autoDists) {
        return Repositories.autoDists().findAll(autoDists);
    }

    /**
     * Deletes an auto-distribution.
     *
     * @param autoDist the auto-distribution to delete; must not be {@code null}
     */
    public static void deleteAutoDist(SSAutoDist autoDist) {
        Repositories.autoDists().delete(autoDist);
    }

    /**
     * Persists a new auto-distribution.
     *
     * @param autoDist the auto-distribution to add; must not be {@code null}
     */
    public static void addAutoDist(SSAutoDist autoDist) {
        Repositories.autoDists().add(autoDist);
    }

    /**
     * Updates an existing auto-distribution.
     *
     * @param autoDist the auto-distribution with updated values; must not be {@code null}
     * @param original the original auto-distribution before edit; must not be {@code null}
     */
    public static void updateAutoDist(SSAutoDist autoDist, SSAutoDist original) {
        Repositories.autoDists().update(autoDist, original);
    }

    // -------------------------------------------------------------------------
    // VoucherTemplate operations
    // -------------------------------------------------------------------------

    /**
     * Returns all voucher templates for the current company.
     *
     * @return list of voucher templates; never {@code null}
     */
    public static List<SSVoucherTemplate> getVoucherTemplates() {
        return Repositories.voucherTemplates().findAll();
    }

    /**
     * Returns only those voucher templates from {@code templates} that exist in the repository.
     *
     * @param templates candidate list; must not be {@code null}
     * @return filtered list; never {@code null}
     */
    public static List<SSVoucherTemplate> getVoucherTemplates(List<SSVoucherTemplate> templates) {
        return Repositories.voucherTemplates().findAll(templates);
    }

    /**
     * Deletes a voucher template.
     *
     * @param template the voucher template to delete; must not be {@code null}
     */
    public static void deleteVoucherTemplate(SSVoucherTemplate template) {
        Repositories.voucherTemplates().delete(template);
    }

    /**
     * Persists a new voucher template.
     *
     * @param template the voucher template to add; must not be {@code null}
     */
    public static void addVoucherTemplate(SSVoucherTemplate template) {
        Repositories.voucherTemplates().add(template);
    }

    // -------------------------------------------------------------------------
    // Shared master data used by accounting UIs
    // -------------------------------------------------------------------------

    /**
     * Returns all accounts from the current account plan.
     *
     * @return list of accounts; empty list if no account plan is active
     */
    public static List<SSAccount> getAccounts() {
        SSAccountPlan currentPlan = getCurrentAccountPlan();
        if (currentPlan == null) {
            return Collections.emptyList();
        }
        return currentPlan.getAccounts();
    }

    /**
     * Returns all projects from the Project &amp; ResultUnit domain.
     *
     * @return list of projects; never {@code null}
     */
    public static List<SSNewProject> getProjects() {
        return SSProjectContext.getProjects();
    }

    /**
     * Returns all result units from the Project &amp; ResultUnit domain.
     *
     * @return list of result units; never {@code null}
     */
    public static List<SSNewResultUnit> getResultUnits() {
        return SSResultUnitContext.getResultUnits();
    }

 //   private static SSDB database() {
 //       return SSDBAccess.database();
 //   }
}
