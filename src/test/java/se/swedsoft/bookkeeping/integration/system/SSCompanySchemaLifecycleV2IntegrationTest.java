package se.swedsoft.bookkeeping.integration.system;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import se.swedsoft.bookkeeping.data.SSNewCompany;
import se.swedsoft.bookkeeping.data.system.SSCompanyYearContext;
import se.swedsoft.bookkeeping.data.system.SSDB;
import se.swedsoft.bookkeeping.persistence.Repositories;
import se.swedsoft.bookkeeping.testsupport.system.CompanySchemaFixture;
import se.swedsoft.bookkeeping.testsupport.system.SSV2DatabaseFixture;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

@Tag("integration")
class SSCompanySchemaLifecycleV2IntegrationTest {

    private static final String JDBC_URL = "jdbc:hsqldb:mem:fribok_test_company_schema_lifecycle_v2";

    private static Connection connection;

    @BeforeAll
    static void setupV2Database() throws Exception {
        connection = SSV2DatabaseFixture.openDatabase(JDBC_URL);
    }

    @AfterAll
    static void teardownV2Database() throws Exception {
        SSV2DatabaseFixture.closeDatabase(connection);
    }

    @BeforeEach
    void resetState() {
        SSV2DatabaseFixture.clearState();
    }

    @Test
    void setCurrentCompanySwitchesSchema() throws Exception {
        Integer companyId = CompanySchemaFixture.createCompany(connection, uniqueName("Schema Switch"));
        SSNewCompany company = new SSNewCompany();
        company.setId(companyId);
        company.setName("ignored");

        SSCompanyYearContext.setCurrentCompany(company);

        String currentSchema = CompanySchemaFixture.currentSchema(connection);
        assertThat(currentSchema).startsWithIgnoringCase("co_");
        assertThat(currentSchema).isNotEqualToIgnoringCase("PUBLIC");
    }

    @Test
    void protectedCo0SchemaCannotBeDeleted() {
        SSNewCompany demoCompany = SSCompanyYearContext.getCompanies().stream()
                .filter(company -> company.getName() != null
                        && (company.getName().startsWith("Demoföretaget")
                            || company.getName().startsWith("DemofÃ¶retaget")
                            || company.getName().startsWith("Demof├")))
                .findFirst()
                .orElseThrow();

        Throwable thrown = catchThrowable(() -> SSCompanyYearContext.deleteCompany(demoCompany));
        assertThat(thrown).isNotNull();
        assertThat(thrown.getMessage()).contains("co_0");
        assertThat(schemaExistsQuietly("CO_0")).isTrue();
    }

    @Test
    void deleteCompanyDropsSchemaAndCatalogRow() throws Exception {
        Integer companyId = CompanySchemaFixture.createCompany(connection, uniqueName("Delete Schema"));
        SSNewCompany company = SSCompanyYearContext.getCompanies().stream()
                .filter(row -> companyId.equals(row.getId()))
                .findFirst()
                .orElseThrow();
        SSCompanyYearContext.setCurrentCompany(company);

        SSCompanyYearContext.deleteCompany(company);

        assertThat(CompanySchemaFixture.countCatalogRows(connection, companyId)).isZero();
        assertThat(schemaExists("CO_" + companyId)).isFalse();
        assertThat(activeCompanyCount()).isZero();
    }

    @Test
    void failedCreateCleansUpCatalogAndSchema() throws Exception {
        String failingName = "Failing Company " + LocalDateTime.now();
        SSNewCompany company = new SSNewCompany();
        company.setName(failingName);
        company.setCorporateID(repeat('X', 400));

        Throwable thrown = catchThrowable(() -> SSCompanyYearContext.addCompany(company));
        assertThat(thrown).isNotNull();
        assertThat(remainingCatalogRowsByName(failingName)).isZero();
    }

    @Test
    void startupRecoveryWithoutActiveCatalogLeavesNoActiveCompany() throws Exception {
        Integer a = CompanySchemaFixture.createCompany(connection, uniqueName("No Active A"));
        Integer b = CompanySchemaFixture.createCompany(connection, uniqueName("No Active B"));
        assertThat(a).isNotEqualTo(b);

        try (PreparedStatement clear = connection.prepareStatement(
                "UPDATE PUBLIC.tbl_company_catalog SET is_active=FALSE")) {
            clear.executeUpdate();
            connection.commit();
        }
        SSDB.getInstance().setCurrentCompany(null);

        SSDB.getInstance().initializeCurrentCompanyAndYear();

        assertThat(SSDB.getInstance().getCurrentCompany()).isNull();
        assertThat(activeCompanyCount()).isZero();
    }

    @Test
    void startupWithEmptyCatalogRecreatesCo0AndReseedsDemo() throws Exception {
        String markerCustomer = "RESEED-CHECK-" + System.nanoTime();
        try (PreparedStatement useCo0 = connection.prepareStatement("SET SCHEMA \"co_0\"")) {
            useCo0.executeUpdate();
        }
        try (PreparedStatement insertMarker = connection.prepareStatement(
                "INSERT INTO tbl_customer(number, companyid, name) "
                        + "SELECT ?, c.id, ? FROM tbl_company c FETCH FIRST 1 ROWS ONLY")) {
            insertMarker.setString(1, markerCustomer);
            insertMarker.setString(2, "Marker customer");
            insertMarker.executeUpdate();
            connection.commit();
        }

        try (PreparedStatement clearCatalog = connection.prepareStatement("DELETE FROM PUBLIC.tbl_company_catalog")) {
            clearCatalog.executeUpdate();
            connection.commit();
        }

        se.swedsoft.bookkeeping.data.system.SSSystemConfigContext.startupLocal(connection);

        assertThat(activeCompanyCount()).isEqualTo(1);
        assertThat(schemaNameForActiveCatalogRow()).contains("co_0");
        assertThat(customerExistsInSchema("co_0", markerCustomer)).isFalse();
    }

    @Test
    void reconcileRemovesCatalogRowsForMissingSchemas() throws Exception {
        Integer companyId = CompanySchemaFixture.createCompany(connection, uniqueName("Missing Schema"));
        String schemaName = schemaNameForCatalogId(companyId).orElseThrow();

        try (PreparedStatement drop = connection.prepareStatement("DROP SCHEMA \"" + schemaName + "\" CASCADE")) {
            drop.executeUpdate();
            connection.commit();
        }

        SSCompanyYearContext.getCompanies();

        assertThat(CompanySchemaFixture.countCatalogRows(connection, companyId)).isZero();
    }

    @Test
    void orphanSchemaIsListedAndCanBeRegistered() throws Exception {
        Integer companyId = CompanySchemaFixture.createCompany(connection, uniqueName("Orphan Schema"));
        String schemaName = schemaNameForCatalogId(companyId).orElseThrow();

        try (PreparedStatement deleteCatalog = connection.prepareStatement(
                "DELETE FROM PUBLIC.tbl_company_catalog WHERE catalog_id=?")) {
            deleteCatalog.setObject(1, companyId);
            deleteCatalog.executeUpdate();
            connection.commit();
        }

        SSNewCompany orphan = SSCompanyYearContext.getCompanies().stream()
                .filter(company -> schemaName.equalsIgnoreCase(company.getSchemaName()))
                .findFirst()
                .orElseThrow();
        assertThat(orphan.isNeedsAttention()).isTrue();
        assertThat(orphan.getId()).isNull();

        Repositories.companies().registerOrActivateCompanySchema(orphan.getSchemaName(), orphan.getName());

        Integer restoredCatalogId = catalogIdForSchema(schemaName).orElseThrow();
        assertThat(CompanySchemaFixture.countCatalogRows(connection, restoredCatalogId)).isEqualTo(1);
        assertThat(activeCompanyCount()).isEqualTo(1);
    }

    @Test
    void orphanSchemaCanBeDeletedWithoutCatalogRow() throws Exception {
        Integer companyId = CompanySchemaFixture.createCompany(connection, uniqueName("Delete Orphan"));
        String schemaName = schemaNameForCatalogId(companyId).orElseThrow();

        try (PreparedStatement deleteCatalog = connection.prepareStatement(
                "DELETE FROM PUBLIC.tbl_company_catalog WHERE catalog_id=?")) {
            deleteCatalog.setObject(1, companyId);
            deleteCatalog.executeUpdate();
            connection.commit();
        }

        SSNewCompany orphan = new SSNewCompany();
        orphan.setSchemaName(schemaName);
        orphan.setName("orphan");

        SSCompanyYearContext.deleteCompany(orphan);

        assertThat(schemaExists(schemaName.toUpperCase())).isFalse();
    }

    private static boolean schemaExists(String schemaNameUpper) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.SCHEMATA WHERE UPPER(SCHEMA_NAME)=?")) {
            statement.setString(1, schemaNameUpper);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() && resultSet.getInt(1) > 0;
            }
        }
    }

    private static boolean schemaExistsQuietly(String schemaNameUpper) {
        try {
            return schemaExists(schemaNameUpper);
        } catch (Exception e) {
            return false;
        }
    }

    private static int activeCompanyCount() throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT COUNT(*) FROM PUBLIC.tbl_company_catalog WHERE is_active=TRUE");
             ResultSet resultSet = statement.executeQuery()) {
            return resultSet.next() ? resultSet.getInt(1) : 0;
        }
    }

    private static int remainingCatalogRowsByName(String companyName) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT COUNT(*) FROM PUBLIC.tbl_company_catalog WHERE company_name=?")) {
            statement.setString(1, companyName);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getInt(1) : 0;
            }
        }
    }

    private static Optional<String> schemaNameForCatalogId(Integer companyId) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT schema_name FROM PUBLIC.tbl_company_catalog WHERE catalog_id=?")) {
            statement.setObject(1, companyId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.ofNullable(resultSet.getString(1));
                }
            }
        }
        return Optional.empty();
    }

    private static Optional<Integer> catalogIdForSchema(String schemaName) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT catalog_id FROM PUBLIC.tbl_company_catalog WHERE schema_name=?")) {
            statement.setString(1, schemaName);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(resultSet.getInt(1));
                }
            }
        }
        return Optional.empty();
    }

    private static Optional<String> schemaNameForActiveCatalogRow() throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT schema_name FROM PUBLIC.tbl_company_catalog WHERE is_active=TRUE ORDER BY catalog_id FETCH FIRST 1 ROWS ONLY");
             ResultSet resultSet = statement.executeQuery()) {
            if (resultSet.next()) {
                return Optional.ofNullable(resultSet.getString(1));
            }
        }
        return Optional.empty();
    }

    private static boolean customerExistsInSchema(String schemaName, String customerNumber) throws Exception {
        try (PreparedStatement useSchema = connection.prepareStatement("SET SCHEMA \"" + schemaName + "\"")) {
            useSchema.executeUpdate();
        }
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT COUNT(*) FROM tbl_customer WHERE number=?")) {
            statement.setString(1, customerNumber);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() && resultSet.getInt(1) > 0;
            }
        } finally {
            try (PreparedStatement resetSchema = connection.prepareStatement("SET SCHEMA PUBLIC")) {
                resetSchema.executeUpdate();
            }
        }
    }

    private static String uniqueName(String prefix) {
        return prefix + " " + System.nanoTime();
    }

    private static String repeat(char c, int count) {
        StringBuilder builder = new StringBuilder(count);
        for (int i = 0; i < count; i++) {
            builder.append(c);
        }
        return builder.toString();
    }
}
