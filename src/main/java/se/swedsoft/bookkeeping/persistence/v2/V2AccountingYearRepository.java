package se.swedsoft.bookkeeping.persistence.v2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.swedsoft.bookkeeping.data.SSAccount;
import se.swedsoft.bookkeeping.data.SSAccountPlan;
import se.swedsoft.bookkeeping.data.SSBudget;
import se.swedsoft.bookkeeping.data.SSMonth;
import se.swedsoft.bookkeeping.data.SSNewAccountingYear;
import se.swedsoft.bookkeeping.data.SSNewCompany;
import se.swedsoft.bookkeeping.gui.SSMainFrame;
import se.swedsoft.bookkeeping.gui.util.SSBundle;
import se.swedsoft.bookkeeping.gui.util.dialogs.SSErrorDialog;
import se.swedsoft.bookkeeping.persistence.snapshot.AccountPlanSnapshot;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * V2 accounting-year repository backed by direct SQL.
 */
public class V2AccountingYearRepository {

    @FunctionalInterface
    public interface RollbackHandler {
        void rollbackCurrentTransaction() throws SQLException;
    }

    private static final Logger LOG = LoggerFactory.getLogger(V2AccountingYearRepository.class);

    private final Connection connection;
    private final Supplier<SSNewCompany> currentCompanySupplier;
    private final Supplier<SSNewAccountingYear> currentYearSupplier;
    private final Consumer<SSNewAccountingYear> openedYearConsumer;
    private final RollbackHandler rollbackHandler;

    public V2AccountingYearRepository(Connection connection,
                                      Supplier<SSNewCompany> currentCompanySupplier,
                                      Supplier<SSNewAccountingYear> currentYearSupplier,
                                      Consumer<SSNewAccountingYear> openedYearConsumer,
                                      RollbackHandler rollbackHandler) {
        if (connection == null) {
            throw new NullPointerException("connection must not be null");
        }
        if (currentCompanySupplier == null) {
            throw new NullPointerException("currentCompanySupplier must not be null");
        }
        if (currentYearSupplier == null) {
            throw new NullPointerException("currentYearSupplier must not be null");
        }
        if (openedYearConsumer == null) {
            throw new NullPointerException("openedYearConsumer must not be null");
        }
        if (rollbackHandler == null) {
            throw new NullPointerException("rollbackHandler must not be null");
        }
        this.connection = connection;
        this.currentCompanySupplier = currentCompanySupplier;
        this.currentYearSupplier = currentYearSupplier;
        this.openedYearConsumer = openedYearConsumer;
        this.rollbackHandler = rollbackHandler;
    }

    public List<SSNewAccountingYear> findAll() {
        SSNewCompany currentCompany = currentCompanySupplier.get();
        if (currentCompany == null) {
            return new LinkedList<>();
        }
        return findForCompany(currentCompany);
    }

    public Optional<SSNewAccountingYear> findCurrent() {
        return Optional.ofNullable(currentYearSupplier.get());
    }

    public void add(SSNewAccountingYear year) {
        if (year == null) {
            return;
        }
        SSNewCompany currentCompany = currentCompanySupplier.get();
        if (currentCompany == null) {
            return;
        }
        try {
            byte[] snapshot = null;
            String checksum = null;
            if (year.getAccountPlan() != null) {
                String compressionFlag = year.getAccountPlanCompressionFlag();
                snapshot = AccountPlanSnapshot.serialize(year.getAccountPlan(), compressionFlag);
                if (snapshot != null) {
                    checksum = AccountPlanSnapshot.calculateChecksum(snapshot);
                }
            }
            String snapshotText = snapshot == null ? null : Base64.getEncoder().encodeToString(snapshot);

            try (PreparedStatement iStatement = connection.prepareStatement(
                    "INSERT INTO tbl_accountingyear(companyid,from_date,to_date,accountplan_id,"
                            + "accountplan,accountplan_schema_version,accountplan_compression_flag,"
                            + "accountplan_checksum,accountplan_snapshot_version) VALUES(?,?,?,?,?,?,?,?,?)",
                    Statement.RETURN_GENERATED_KEYS)) {

                iStatement.setObject(1, currentCompany.getId());
                iStatement.setObject(2, java.sql.Date.valueOf(year.getLocalFrom()));
                iStatement.setObject(3, java.sql.Date.valueOf(year.getLocalTo()));
                iStatement.setObject(4, year.getAccountPlan() == null ? null : year.getAccountPlan().getId());
                iStatement.setString(5, snapshotText);
                iStatement.setObject(6, AccountPlanSnapshot.getSchemaVersion());
                iStatement.setObject(7, year.getAccountPlanCompressionFlag());
                iStatement.setObject(8, checksum);
                iStatement.setObject(9, snapshotText == null ? 0 : 1);
                iStatement.executeUpdate();

                try (ResultSet iKeys = iStatement.getGeneratedKeys()) {
                    if (iKeys.next()) {
                        year.setId(iKeys.getInt(1));
                    }
                }
            }

            replaceYearBalances(year);
            replaceBudgetRows(year);
            connection.commit();
        } catch (SQLException e) {
            handleSqlError(e);
        }
    }

    public void update(SSNewAccountingYear year) {
        if (year == null) {
            return;
        }
        try {
            try (PreparedStatement iStatement = connection.prepareStatement(
                    "UPDATE tbl_accountingyear SET from_date=?,to_date=?,accountplan_id=?,"
                            + "accountplan=?,accountplan_schema_version=?,accountplan_compression_flag=?,"
                            + "accountplan_checksum=?,accountplan_snapshot_version=?,accountplan_updated_at=?,"
                            + "accountplan_updated_by=?,accountplan_name=?,accountplan_dirty_flag=? WHERE id=?")) {

                iStatement.setObject(1, java.sql.Date.valueOf(year.getLocalFrom()));
                iStatement.setObject(2, java.sql.Date.valueOf(year.getLocalTo()));
                iStatement.setObject(3, year.getAccountPlan() == null ? null : year.getAccountPlan().getId());

                byte[] snapshot = null;
                String checksum = null;
                Integer snapshotVersion = year.getAccountPlanSnapshotVersion();
                if (year.getAccountPlan() != null) {
                    String compressionFlag = year.getAccountPlanCompressionFlag();
                    snapshot = AccountPlanSnapshot.serialize(year.getAccountPlan(), compressionFlag);
                    if (snapshot != null) {
                        checksum = AccountPlanSnapshot.calculateChecksum(snapshot);
                        snapshotVersion = (snapshotVersion != null ? snapshotVersion : 0) + 1;
                    }
                }

                String snapshotText = snapshot == null ? null : Base64.getEncoder().encodeToString(snapshot);
                iStatement.setString(4, snapshotText);
                iStatement.setObject(5, AccountPlanSnapshot.getSchemaVersion());
                iStatement.setObject(6, year.getAccountPlanCompressionFlag());
                iStatement.setObject(7, checksum);
                iStatement.setObject(8, snapshotVersion);
                iStatement.setObject(9, java.sql.Timestamp.from(java.time.Instant.now()));
                iStatement.setObject(10, year.getAccountPlanUpdatedBy());
                iStatement.setObject(11, year.getAccountPlan() == null ? null : year.getAccountPlan().getName());
                iStatement.setObject(12, false);
                iStatement.setObject(13, year.getId());
                iStatement.executeUpdate();
            }

            SSNewAccountingYear currentYear = currentYearSupplier.get();
            if (currentYear != null && currentYear.getId() != null && currentYear.getId().equals(year.getId())) {
                replaceYearAccountRows(year.getId(), year.getAccountPlan());
                openedYearConsumer.accept(year);
            }

            replaceYearBalances(year);
            replaceBudgetRows(year);
            connection.commit();
        } catch (SQLException e) {
            handleSqlError(e);
        }
    }

    public void delete(SSNewAccountingYear year) {
        if (year == null) {
            return;
        }
        Integer yearId = year.getId();
        if (yearId == null) {
            return;
        }
        try {
            executeDelete(
                    "DELETE FROM tbl_invoice_row WHERE invoice_id IN (SELECT id FROM tbl_invoice WHERE voucher_id IN " +
                            "(SELECT id FROM tbl_voucher WHERE yearid=?))",
                    yearId);
            executeDelete(
                    "DELETE FROM tbl_creditinvoice_row WHERE creditinvoice_id IN (SELECT id FROM tbl_creditinvoice WHERE " +
                            "voucher_id IN (SELECT id FROM tbl_voucher WHERE yearid=?))",
                    yearId);
            executeDelete(
                    "DELETE FROM tbl_supplierinvoice_row WHERE supplierinvoice_id IN (SELECT id FROM tbl_supplierinvoice " +
                            "WHERE voucher_id IN (SELECT id FROM tbl_voucher WHERE yearid=?) OR correction_voucher_id IN " +
                            "(SELECT id FROM tbl_voucher WHERE yearid=?))",
                    yearId,
                    yearId);
            executeDelete(
                    "DELETE FROM tbl_suppliercreditinvoice_row WHERE suppliercreditinvoice_id IN (SELECT id FROM " +
                            "tbl_suppliercreditinvoice WHERE voucher_id IN (SELECT id FROM tbl_voucher WHERE yearid=?) OR " +
                            "correction_voucher_id IN (SELECT id FROM tbl_voucher WHERE yearid=?))",
                    yearId,
                    yearId);
            executeDelete(
                    "DELETE FROM tbl_inpayment_row WHERE inpayment_id IN (SELECT id FROM tbl_inpayment WHERE voucher_id IN " +
                            "(SELECT id FROM tbl_voucher WHERE yearid=?) OR difference_voucher_id IN " +
                            "(SELECT id FROM tbl_voucher WHERE yearid=?))",
                    yearId,
                    yearId);
            executeDelete(
                    "DELETE FROM tbl_outpayment_row WHERE outpayment_id IN (SELECT id FROM tbl_outpayment WHERE voucher_id " +
                            "IN (SELECT id FROM tbl_voucher WHERE yearid=?) OR difference_voucher_id IN " +
                            "(SELECT id FROM tbl_voucher WHERE yearid=?))",
                    yearId,
                    yearId);
            executeDelete("DELETE FROM tbl_invoice WHERE voucher_id IN (SELECT id FROM tbl_voucher WHERE yearid=?)", yearId);
            executeDelete("DELETE FROM tbl_creditinvoice WHERE voucher_id IN (SELECT id FROM tbl_voucher WHERE yearid=?)", yearId);
            executeDelete(
                    "DELETE FROM tbl_supplierinvoice WHERE voucher_id IN (SELECT id FROM tbl_voucher WHERE yearid=?) OR " +
                            "correction_voucher_id IN (SELECT id FROM tbl_voucher WHERE yearid=?)",
                    yearId,
                    yearId);
            executeDelete(
                    "DELETE FROM tbl_suppliercreditinvoice WHERE voucher_id IN (SELECT id FROM tbl_voucher WHERE yearid=?) " +
                            "OR correction_voucher_id IN (SELECT id FROM tbl_voucher WHERE yearid=?)",
                    yearId,
                    yearId);
            executeDelete(
                    "DELETE FROM tbl_inpayment WHERE voucher_id IN (SELECT id FROM tbl_voucher WHERE yearid=?) OR " +
                            "difference_voucher_id IN (SELECT id FROM tbl_voucher WHERE yearid=?)",
                    yearId,
                    yearId);
            executeDelete(
                    "DELETE FROM tbl_outpayment WHERE voucher_id IN (SELECT id FROM tbl_voucher WHERE yearid=?) OR " +
                            "difference_voucher_id IN (SELECT id FROM tbl_voucher WHERE yearid=?)",
                    yearId,
                    yearId);
            executeDelete("DELETE FROM tbl_voucher_row WHERE voucher_id IN (SELECT id FROM tbl_voucher WHERE yearid=?)", yearId);
            executeDelete("DELETE FROM tbl_voucher WHERE yearid=?", yearId);
            executeDelete("DELETE FROM tbl_year_balance WHERE year_id=?", yearId);
            executeDelete("DELETE FROM tbl_budget_row WHERE year_id=?", yearId);
            executeDelete("DELETE FROM tbl_account WHERE accountingyear_id=?", yearId);
            executeDelete("DELETE FROM tbl_accountingyear WHERE id=?", yearId);
            connection.commit();
        } catch (SQLException e) {
            handleSqlError(e);
        }
    }

    public void open(SSNewAccountingYear year) {
        if (year == null || year.getId() == null) {
            return;
        }

        SSNewAccountingYear currentYear = currentYearSupplier.get();
        if (currentYear != null && currentYear.getId() != null && !currentYear.getId().equals(year.getId())) {
            close(currentYear);
        }

        try {
            SSAccountPlan snapshot = loadAccountPlanSnapshot(year.getId());
            SSAccountPlan activePlan = year.getAccountPlan();
            if (snapshot != null) {
                if (activePlan == null) {
                    activePlan = new SSAccountPlan();
                    activePlan.copyFrom(snapshot);
                    activePlan.setId(null);
                } else {
                    activePlan.setAccounts(snapshot.getAccounts());
                }
                year.setAccountPlan(activePlan);
            }

            replaceYearAccountRows(year.getId(), activePlan != null ? activePlan : snapshot);
            connection.commit();
            openedYearConsumer.accept(year);
        } catch (SQLException e) {
            handleSqlError(e);
        }
    }

    public void close(SSNewAccountingYear year) {
        if (year == null || year.getId() == null) {
            return;
        }
        try {
            List<SSAccount> liveAccounts = loadAccountsForYear(year.getId());
            SSAccountPlan plan = year.getAccountPlan();
            if (plan == null) {
                plan = new SSAccountPlan();
            }
            if (!liveAccounts.isEmpty()) {
                plan.setAccounts(liveAccounts);
                year.setAccountPlan(plan);
            }

            String compressionFlag = year.getAccountPlanCompressionFlag();
            byte[] snapshotBytes = AccountPlanSnapshot.serialize(plan, compressionFlag);
            String checksum = snapshotBytes != null ? AccountPlanSnapshot.calculateChecksum(snapshotBytes) : null;
            String snapshotText = snapshotBytes != null ? Base64.getEncoder().encodeToString(snapshotBytes) : null;
            Integer snapshotVersion = (year.getAccountPlanSnapshotVersion() != null
                    ? year.getAccountPlanSnapshotVersion() : 0) + 1;

            try (PreparedStatement iStatement = connection.prepareStatement(
                    "UPDATE tbl_accountingyear SET accountplan=?,accountplan_checksum=?,"
                            + "accountplan_snapshot_version=?,accountplan_updated_at=? WHERE id=?")) {
                iStatement.setString(1, snapshotText);
                iStatement.setObject(2, checksum);
                iStatement.setObject(3, snapshotVersion);
                iStatement.setObject(4, java.sql.Timestamp.from(java.time.Instant.now()));
                iStatement.setObject(5, year.getId());
                iStatement.executeUpdate();
            }

            try (PreparedStatement iDelete = connection.prepareStatement("DELETE FROM tbl_account")) {
                iDelete.executeUpdate();
            }

            connection.commit();
            year.setAccountPlanSnapshotVersion(snapshotVersion);
            year.setAccountPlanChecksum(checksum);
        } catch (SQLException e) {
            handleSqlError(e);
        }
    }

    public List<SSNewAccountingYear> findForCompany(SSNewCompany company) {
        List<SSNewAccountingYear> years = new LinkedList<>();
        if (company == null) {
            return years;
        }
        try {
            try (PreparedStatement iStatement = connection.prepareStatement(
                    "SELECT * FROM tbl_accountingyear WHERE companyid=?")) {
                iStatement.setObject(1, company.getId());
                try (ResultSet iResultSet = iStatement.executeQuery()) {
                    while (iResultSet.next()) {
                        years.add(mapAccountingYear(iResultSet));
                    }
                }
            }
        } catch (SQLException e) {
            handleSqlError(e);
        }
        return years;
    }

    public Optional<SSNewAccountingYear> findById(SSNewAccountingYear yearProbe) {
        if (yearProbe == null) {
            return Optional.empty();
        }
        try {
            try (PreparedStatement iStatement = connection.prepareStatement(
                    "SELECT * FROM tbl_accountingyear WHERE id=?")) {
                iStatement.setObject(1, yearProbe.getId());
                try (ResultSet iResultSet = iStatement.executeQuery()) {
                    if (iResultSet.next()) {
                        return Optional.of(mapAccountingYear(iResultSet));
                    }
                }
            }
        } catch (SQLException e) {
            handleSqlError(e);
        }
        return Optional.empty();
    }

    public Optional<SSNewAccountingYear> findPrevious() {
        SSNewAccountingYear currentYear = currentYearSupplier.get();
        if (currentYear == null || currentYear.getLocalFrom() == null) {
            return Optional.empty();
        }
        List<SSNewAccountingYear> years = findAll();
        java.time.LocalDate dayBeforeCurrent = currentYear.getLocalFrom().minusDays(1);
        for (SSNewAccountingYear year : years) {
            if (dayBeforeCurrent.equals(year.getLocalTo())) {
                return Optional.of(year);
            }
        }
        return Optional.empty();
    }

    public Optional<SSNewAccountingYear> findLast() {
        List<SSNewAccountingYear> years = findAll();
        java.time.LocalDate lastDate = null;
        SSNewAccountingYear lastYear = null;
        for (SSNewAccountingYear year : years) {
            java.time.LocalDate iTo = year.getLocalTo();
            if (iTo == null) {
                continue;
            }
            if (lastDate == null || iTo.isAfter(lastDate)) {
                lastDate = iTo;
                lastYear = year;
            }
        }
        return Optional.ofNullable(lastYear);
    }

    public boolean canOpen(SSNewAccountingYear year) {
        if (year == null || year.getId() == null) {
            return true;
        }
        if (hasValidAccountPlanSnapshot(year.getId())) {
            return true;
        }
        showMissingSnapshotDialog();
        return false;
    }

    public boolean hasAccountRows(Integer yearId) {
        if (yearId == null) {
            return false;
        }
        try (PreparedStatement iStatement = connection.prepareStatement(
                "SELECT COUNT(*) FROM tbl_account WHERE accountingyear_id=?")) {
            iStatement.setObject(1, yearId);
            try (ResultSet iResultSet = iStatement.executeQuery()) {
                if (!iResultSet.next()) {
                    return false;
                }
                return iResultSet.getInt(1) > 0;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to check account rows for year " + yearId, e);
        }
    }

    public SSNewAccountingYear mapAccountingYear(ResultSet iResultSet) throws SQLException {
        SSNewAccountingYear iAccountingYear = new SSNewAccountingYear();
        iAccountingYear.setId(iResultSet.getInt("id"));

        java.sql.Date iFromDate = iResultSet.getDate("from_date");
        if (iFromDate != null) {
            iAccountingYear.setLocalFrom(iFromDate.toLocalDate());
        }

        java.sql.Date iToDate = iResultSet.getDate("to_date");
        if (iToDate != null) {
            iAccountingYear.setLocalTo(iToDate.toLocalDate());
        }

        SSAccountPlan iYearPlan = new SSAccountPlan();
        List<SSAccount> liveAccounts = loadAccountsForYear(iAccountingYear.getId());
        if (!liveAccounts.isEmpty()) {
            SSAccountPlan snapshotPlan = loadAccountPlanSnapshot(iAccountingYear.getId());
            if (snapshotPlan != null) {
                iYearPlan.copyFrom(snapshotPlan);
                iYearPlan.setId(null);
            }
            iYearPlan.setAccounts(liveAccounts);
        } else {
            SSAccountPlan snapshotPlan = loadAccountPlanSnapshot(iAccountingYear.getId());
            if (snapshotPlan != null) {
                iYearPlan.copyFrom(snapshotPlan);
                iYearPlan.setId(null);
            }
        }
        iAccountingYear.setAccountPlan(iYearPlan);

        Integer schemaVersion = (Integer) iResultSet.getObject("accountplan_schema_version");
        String compressionFlag = iResultSet.getString("accountplan_compression_flag");
        String checksum = iResultSet.getString("accountplan_checksum");
        Integer snapshotVersion = (Integer) iResultSet.getObject("accountplan_snapshot_version");
        java.sql.Timestamp updatedAt = iResultSet.getTimestamp("accountplan_updated_at");
        String updatedBy = iResultSet.getString("accountplan_updated_by");
        String planName = iResultSet.getString("accountplan_name");

        if (iAccountingYear.getAccountPlan() != null && iAccountingYear.getAccountPlan().getName() == null) {
            iAccountingYear.getAccountPlan().setName(planName);
        }

        iAccountingYear.setAccountPlanSchemaVersion(schemaVersion);
        iAccountingYear.setAccountPlanCompressionFlag(compressionFlag);
        iAccountingYear.setAccountPlanChecksum(checksum);
        iAccountingYear.setAccountPlanSnapshotVersion(snapshotVersion);
        if (updatedAt != null) {
            iAccountingYear.setAccountPlanUpdatedAt(updatedAt.toInstant());
        }
        iAccountingYear.setAccountPlanUpdatedBy(updatedBy);
        iAccountingYear.setAccountPlanName(planName);

        iAccountingYear.setInBalance(loadYearBalances(iAccountingYear));
        iAccountingYear.setBudget(loadBudget(iAccountingYear));
        return iAccountingYear;
    }

    private Map<SSAccount, BigDecimal> loadYearBalances(SSNewAccountingYear iAccountingYear) throws SQLException {
        Map<SSAccount, BigDecimal> iBalances = new HashMap<>();
        if (iAccountingYear == null || iAccountingYear.getId() == null) {
            return iBalances;
        }
        try (PreparedStatement iStatement = connection.prepareStatement(
                "SELECT account_nr,balance FROM tbl_year_balance WHERE year_id=?")) {
            iStatement.setObject(1, iAccountingYear.getId());
            try (ResultSet iResultSet = iStatement.executeQuery()) {
                while (iResultSet.next()) {
                    Integer iAccountNr = (Integer) iResultSet.getObject("account_nr");
                    if (iAccountNr == null) {
                        continue;
                    }
                    SSAccount iAccount = iAccountingYear.getAccountPlan() == null
                            ? null : iAccountingYear.getAccountPlan().getAccount(iAccountNr);
                    if (iAccount == null) {
                        iAccount = new SSAccount();
                        iAccount.setNumber(iAccountNr);
                    }
                    iBalances.put(iAccount, iResultSet.getBigDecimal("balance"));
                }
                return iBalances;
            }
        }
    }

    private SSBudget loadBudget(SSNewAccountingYear iAccountingYear) throws SQLException {
        SSBudget iBudget = new SSBudget();
        if (iAccountingYear == null || iAccountingYear.getId() == null) {
            return iBudget;
        }

        iBudget.setYear(iAccountingYear);

        try (PreparedStatement iStatement = connection.prepareStatement(
                "SELECT account_nr,month,amount FROM tbl_budget_row WHERE year_id=?")) {
            iStatement.setObject(1, iAccountingYear.getId());
            try (ResultSet iResultSet = iStatement.executeQuery()) {
                List<SSMonth> iMonths = iBudget.getMonths();
                while (iResultSet.next()) {
                    Integer iAccountNr = (Integer) iResultSet.getObject("account_nr");
                    Integer iMonthNumber = (Integer) iResultSet.getObject("month");
                    BigDecimal iAmount = iResultSet.getBigDecimal("amount");
                    if (iAccountNr == null || iMonthNumber == null || iAmount == null) {
                        continue;
                    }

                    SSAccount iAccount = iAccountingYear.getAccountPlan() == null
                            ? null : iAccountingYear.getAccountPlan().getAccount(iAccountNr);
                    if (iAccount == null) {
                        iAccount = new SSAccount();
                        iAccount.setNumber(iAccountNr);
                    }

                    for (SSMonth iMonth : iMonths) {
                        if (iMonth.getLocalFrom() != null && iMonth.getLocalFrom().getMonthValue() == iMonthNumber) {
                            iBudget.setSaldoForAccountAndMonth(iAccount, iMonth, iAmount);
                            break;
                        }
                    }
                }
                return iBudget;
            }
        }
    }

    private void replaceYearBalances(SSNewAccountingYear iAccountingYear) throws SQLException {
        if (iAccountingYear == null || iAccountingYear.getId() == null) {
            return;
        }

        try (PreparedStatement iDelete = connection.prepareStatement(
                "DELETE FROM tbl_year_balance WHERE year_id=?")) {
            iDelete.setObject(1, iAccountingYear.getId());
            iDelete.executeUpdate();
        }

        Map<SSAccount, BigDecimal> iBalances = iAccountingYear.getInBalance();
        if (iBalances == null) {
            return;
        }

        for (Map.Entry<SSAccount, BigDecimal> iEntry : iBalances.entrySet()) {
            SSAccount iAccount = iEntry.getKey();
            BigDecimal iAmount = iEntry.getValue();
            if (iAccount == null || iAccount.getNumber() == null || iAmount == null) {
                continue;
            }

            try (PreparedStatement iInsert = connection.prepareStatement(
                    "INSERT INTO tbl_year_balance(year_id,account_nr,balance) VALUES(?,?,?)")) {
                iInsert.setObject(1, iAccountingYear.getId());
                iInsert.setObject(2, iAccount.getNumber());
                iInsert.setObject(3, iAmount);
                iInsert.executeUpdate();
            }
        }
    }

    private void replaceBudgetRows(SSNewAccountingYear iAccountingYear) throws SQLException {
        if (iAccountingYear == null || iAccountingYear.getId() == null) {
            return;
        }

        try (PreparedStatement iDelete = connection.prepareStatement(
                "DELETE FROM tbl_budget_row WHERE year_id=?")) {
            iDelete.setObject(1, iAccountingYear.getId());
            iDelete.executeUpdate();
        }

        SSBudget iBudget = iAccountingYear.getBudget();
        if (iBudget == null) {
            return;
        }

        iBudget.setYear(iAccountingYear);

        for (SSMonth iMonth : iBudget.getMonths()) {
            Integer iMonthNumber = iMonth.getLocalFrom() == null ? null : iMonth.getLocalFrom().getMonthValue();
            if (iMonthNumber == null) {
                continue;
            }

            Map<SSAccount, BigDecimal> iMonthBudget = iBudget.getBudget(iMonth);
            if (iMonthBudget == null) {
                continue;
            }

            for (Map.Entry<SSAccount, BigDecimal> iEntry : iMonthBudget.entrySet()) {
                SSAccount iAccount = iEntry.getKey();
                BigDecimal iAmount = iEntry.getValue();
                if (iAccount == null || iAccount.getNumber() == null || iAmount == null) {
                    continue;
                }

                try (PreparedStatement iInsert = connection.prepareStatement(
                        "INSERT INTO tbl_budget_row(year_id,account_nr,month,amount) VALUES(?,?,?,?)")) {
                    iInsert.setObject(1, iAccountingYear.getId());
                    iInsert.setObject(2, iAccount.getNumber());
                    iInsert.setObject(3, iMonthNumber);
                    iInsert.setObject(4, iAmount);
                    iInsert.executeUpdate();
                }
            }
        }
    }
    /**
     * Loads an account plan snapshot from a year, decompressing and deserializing it.
     * Validates checksum and schema version.
     * Returns null if no snapshot exists or validation fails.
     */
    private SSAccountPlan loadAccountPlanSnapshot(Integer yearId) {
        if (yearId == null) {
            return null;
        }

        try {
            String snapshotText;
            String compressionFlag;
            String storedChecksum;
            Integer schemaVersion;
            try (PreparedStatement iStatement = connection.prepareStatement(
                    "SELECT accountplan, accountplan_compression_flag, accountplan_checksum,"
                            + " accountplan_schema_version FROM tbl_accountingyear WHERE id=?")) {
                iStatement.setObject(1, yearId);
                try (ResultSet iResultSet = iStatement.executeQuery()) {
                    if (!iResultSet.next()) {
                        return null;
                    }
                    snapshotText = iResultSet.getString("accountplan");
                    compressionFlag = iResultSet.getString("accountplan_compression_flag");
                    storedChecksum = iResultSet.getString("accountplan_checksum");
                    schemaVersion = (Integer) iResultSet.getObject("accountplan_schema_version");
                }
            }

            if (snapshotText == null || snapshotText.isEmpty()) {
                return null;
            }

            byte[] decodedSnapshotData;
            try {
                decodedSnapshotData = Base64.getDecoder().decode(snapshotText.trim());
            } catch (IllegalArgumentException e) {
                LOG.warn("Invalid Base64 snapshot data for year {}", yearId, e);
                return null;
            }

            if (storedChecksum != null) {
                String calculatedChecksum = AccountPlanSnapshot.calculateChecksum(decodedSnapshotData);
                if (!storedChecksum.equals(calculatedChecksum)) {
                    LOG.warn("Checksum mismatch for account plan snapshot in year {}", yearId);
                    return null;
                }
            }

            if (schemaVersion == null || schemaVersion != AccountPlanSnapshot.getSchemaVersion()) {
                LOG.warn("Unsupported snapshot schema version: {}", schemaVersion);
                return null;
            }

            if (compressionFlag == null || compressionFlag.isEmpty()) {
                LOG.warn("Missing compression flag for account plan snapshot in year {}", yearId);
                return null;
            }

            return AccountPlanSnapshot.deserialize(decodedSnapshotData, compressionFlag);

        } catch (SQLException e) {
            LOG.error("Error loading account plan snapshot for year {}", yearId, e);
            return null;
        }
    }

    private boolean hasValidAccountPlanSnapshot(Integer yearId) {
        return loadAccountPlanSnapshot(yearId) != null;
    }

    private List<SSAccount> loadAccountsForYear(Integer yearId) throws SQLException {
        List<SSAccount> iAccounts = new LinkedList<>();
        if (yearId == null) {
            return iAccounts;
        }

        try (PreparedStatement iStatement = connection.prepareStatement(
                "SELECT * FROM tbl_account WHERE accountingyear_id=? ORDER BY number")) {
            iStatement.setObject(1, yearId);
            try (ResultSet iResultSet = iStatement.executeQuery()) {
                while (iResultSet.next()) {
                    SSAccount iAccount = new SSAccount();
                    iAccount.setNumber(iResultSet.getInt("number"));
                    iAccount.setDescription(iResultSet.getString("description"));
                    iAccount.setSRUCode(iResultSet.getString("sru_code"));
                    iAccount.setVATCode(iResultSet.getString("vat_code"));
                    iAccount.setReportCode(iResultSet.getString("report_code"));
                    iAccount.setActive(iResultSet.getBoolean("active"));
                    iAccount.setProjectRequired(iResultSet.getBoolean("project_required"));
                    iAccount.setResultUnitRequired(iResultSet.getBoolean("result_unit_required"));
                    iAccounts.add(iAccount);
                }
                return iAccounts;
            }
        }
    }

    private void replaceYearAccountRows(Integer yearId, SSAccountPlan accountPlan) throws SQLException {
        if (yearId == null) {
            return;
        }

        try (PreparedStatement iDelete = connection.prepareStatement("DELETE FROM tbl_account")) {
            iDelete.executeUpdate();
        }

        if (accountPlan == null) {
            return;
        }

        for (SSAccount iAccount : accountPlan.getAccounts()) {
            if (iAccount == null || iAccount.getNumber() == null) {
                continue;
            }

            try (PreparedStatement iInsert = connection.prepareStatement(
                    "INSERT INTO tbl_account(accountingyear_id,number,description,sru_code,vat_code,report_code,active,project_required,result_unit_required) VALUES(?,?,?,?,?,?,?,?,?)")) {
                iInsert.setObject(1, yearId);
                iInsert.setObject(2, iAccount.getNumber());
                iInsert.setObject(3, iAccount.getDescription());
                iInsert.setObject(4, iAccount.getSRUCode());
                iInsert.setObject(5, iAccount.getVATCode());
                iInsert.setObject(6, iAccount.getReportCode());
                iInsert.setBoolean(7, iAccount.isActive());
                iInsert.setBoolean(8, iAccount.isProjectRequired());
                iInsert.setBoolean(9, iAccount.isResultUnitRequired());
                iInsert.executeUpdate();
            }
        }
    }

    private void executeDelete(String sql, Object... values) throws SQLException {
        try (PreparedStatement iStatement = connection.prepareStatement(sql)) {
            for (int i = 0; i < values.length; i++) {
                iStatement.setObject(i + 1, values[i]);
            }
            iStatement.executeUpdate();
        }
    }

    private void showMissingSnapshotDialog() {
        ResourceBundle iBundle = SSBundle.getBundle();
        String iTitle = iBundle.containsKey("accountingyear.snapshot.missing.title")
                ? iBundle.getString("accountingyear.snapshot.missing.title")
                : "Missing account plan snapshot";
        String iMessage = iBundle.containsKey("accountingyear.snapshot.missing.message")
                ? iBundle.getString("accountingyear.snapshot.missing.message")
                : "Selected year has no account plan snapshot and cannot be opened. Delete and recreate the year.";

        if (java.awt.GraphicsEnvironment.isHeadless()) {
            LOG.warn("{}: {}", iTitle, iMessage);
            return;
        }
        javax.swing.JOptionPane.showMessageDialog(SSMainFrame.getInstance(), iMessage, iTitle,
                javax.swing.JOptionPane.WARNING_MESSAGE);
    }

    private void handleSqlError(SQLException e) {
        LOG.error("Unexpected error", e);
        try {
            rollbackHandler.rollbackCurrentTransaction();
        } catch (SQLException ignored) {
            // best effort rollback
        }
        SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error", e.getMessage());
    }
}
