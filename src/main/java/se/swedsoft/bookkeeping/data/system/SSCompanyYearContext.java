package se.swedsoft.bookkeeping.data.system;

import se.swedsoft.bookkeeping.data.SSNewAccountingYear;
import se.swedsoft.bookkeeping.data.SSNewCompany;
import se.swedsoft.bookkeeping.persistence.Repositories;

import java.beans.PropertyChangeListener;
import java.util.List;
import java.util.Optional;

/**
 * Domain service for the Company/Year domain (domain #1 in the 12-domain target architecture).
 *
 * <p>Manages company records, accounting years, year lifecycle (open/close),
 * and the PropertyChange event bus for company/year state changes.</p>
 *
 * <p>Year list and open/close operations delegate to the repository layer.
 * Company-level operations and the event bus still delegate to {@link SSDB}
 * where no repository equivalent exists yet.</p>
 */
public final class SSCompanyYearContext {

    private SSCompanyYearContext() {}

    /**
     * Sets the currently active company.
     *
     * @param pCompany the company to activate; must not be {@code null}
     */
    public static void setCurrentCompany(SSNewCompany pCompany) {
        SSSystemConfigContext.getDatabase().setCurrentCompany(pCompany);
    }


    public static void initializeCurrentCompanyAndYear() {
        SSSystemConfigContext.getDatabase().initializeCurrentCompanyAndYear();
    }

    /**
     * Returns the currently active company.
     *
     * @return the current company, or {@code null} if none is selected
     */
    public static SSNewCompany getCurrentCompany() {
        return SSSystemConfigContext.getDatabase().getCurrentCompany();
    }

    /**
     * Persists a new company.
     *
     * @param pCompany the company to add; must not be {@code null}
     */
    public static void addCompany(SSNewCompany pCompany) {
        Repositories.companies().add(pCompany);
    }

    /**
     * Updates an existing company.
     *
     * @param pCompany the company with updated values; must not be {@code null}
     */
    public static void updateCompany(SSNewCompany pCompany) {
        Repositories.companies().update(pCompany);
    }

    /**
     * Deletes a company.
     *
     * @param pCompany the company to delete; must not be {@code null}
     */
    public static void deleteCompany(SSNewCompany pCompany) {
        Repositories.companies().delete(pCompany);
    }

    /**
     * Sets the currently active accounting year.
     *
     * @param pYear the year to activate; must not be {@code null}
     */
    public static void setCurrentYear(SSNewAccountingYear pYear) {
        SSSystemConfigContext.getDatabase().setCurrentYear(pYear);
    }

    /**
     * Returns the currently active accounting year.
     *
     * @return the current year, or {@code null} if none is selected
     */
    public static SSNewAccountingYear getCurrentYear() {
        return SSSystemConfigContext.getDatabase().getCurrentYear();
    }

    /**
     * Opens an accounting year, making it the active working year.
     *
     * @param pYear the year to open; must not be {@code null}
     */
    public static void openYear(SSNewAccountingYear pYear) {
        Repositories.accountingYears().open(pYear);
    }

    /**
     * Closes an accounting year, flushing working-copy changes to the snapshot.
     *
     * @param pYear the year to close; must not be {@code null}
     */
    public static void closeYear(SSNewAccountingYear pYear) {
        Repositories.accountingYears().close(pYear);
    }

    /**
     * Returns whether the given accounting year can be opened.
     *
     * @param pYear the year to check; must not be {@code null}
     * @return {@code true} if the year may be opened
     */
    public static boolean canOpenAccountingYear(SSNewAccountingYear pYear) {
        return Repositories.accountingYears().canOpen(pYear);
    }

    /**
     * Returns all companies in the database.
     *
     * @return list of companies; never {@code null}
     */
    public static List<SSNewCompany> getCompanies() {
        return Repositories.companies().findAll();
    }

    /**
     * Looks up a company by reference.
     *
     * @param pCompany reference company; must not be {@code null}
     * @return an {@link Optional} containing the company, or empty if not found
     */
    public static Optional<SSNewCompany> getCompany(SSNewCompany pCompany) {
        return Repositories.companies().findById(pCompany);
    }

    /**
     * Returns all accounting years for the current company.
     *
     * @return list of accounting years; never {@code null}
     */
    public static List<SSNewAccountingYear> getYears() {
        return Repositories.accountingYears().findAll();
    }

    /**
     * Persists a new accounting year.
     *
     * @param pYear the accounting year to add; must not be {@code null}
     */
    public static void addAccountingYear(SSNewAccountingYear pYear) {
        Repositories.accountingYears().add(pYear);
    }

    /**
     * Updates an existing accounting year.
     *
     * @param pYear the accounting year with updated values; must not be {@code null}
     */
    public static void updateAccountingYear(SSNewAccountingYear pYear) {
        Repositories.accountingYears().update(pYear);
    }

    /**
     * Deletes an accounting year.
     *
     * @param pYear the accounting year to delete; must not be {@code null}
     */
    public static void deleteAccountingYear(SSNewAccountingYear pYear) {
        Repositories.accountingYears().delete(pYear);
    }

    /**
     * Returns all accounting years belonging to the given company.
     *
     * @param pCompany the company; must not be {@code null}
     * @return list of accounting years; never {@code null}
     */
    public static List<SSNewAccountingYear> getYearsForCompany(SSNewCompany pCompany) {
        return Repositories.accountingYears().findForCompany(pCompany);
    }

    /**
     * Looks up an accounting year by reference.
     *
     * @param pAccountingYear reference year; must not be {@code null}
     * @return an {@link Optional} containing the year, or empty if not found
     */
    public static Optional<SSNewAccountingYear> getAccountingYear(SSNewAccountingYear pAccountingYear) {
        return Repositories.accountingYears().findById(pAccountingYear);
    }

    /**
     * Returns the accounting year immediately preceding the current year.
     *
     * @return an {@link Optional} containing the previous year, or empty if none
     */
    public static Optional<SSNewAccountingYear> getPreviousYear() {
        return Repositories.accountingYears().findPrevious();
    }

    /**
     * Returns the most recently created accounting year.
     *
     * @return an {@link Optional} containing the last year, or empty if none
     */
    public static Optional<SSNewAccountingYear> getLastYear() {
        return Repositories.accountingYears().findLast();
    }

    /**
     * Registers a {@link PropertyChangeListener} for the given property.
     *
     * @param pProperty the property name; must not be {@code null}
     * @param pListener the listener to register; must not be {@code null}
     */
    public static void addPropertyChangeListener(String pProperty, PropertyChangeListener pListener) {
        SSSystemConfigContext.getDatabase().addPropertyChangeListener(pProperty, pListener);
    }

    /**
     * Fires a property-change event to all registered listeners.
     *
     * @param pProperty  the property name; must not be {@code null}
     * @param pNewValue  the new property value; may be {@code null}
     * @param pOldValue  the old property value; may be {@code null}
     */
    public static void notifyListeners(String pProperty, Object pNewValue, Object pOldValue) {
        SSSystemConfigContext.getDatabase().notifyListeners(pProperty, pNewValue, pOldValue);
    }

    public static void applyOpenedYearFromRepository(SSNewAccountingYear pYear) {
        SSSystemConfigContext.getDatabase().applyOpenedYearFromRepository(pYear);
    }
}
