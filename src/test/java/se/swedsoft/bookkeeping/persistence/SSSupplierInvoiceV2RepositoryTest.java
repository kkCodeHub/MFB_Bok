package se.swedsoft.bookkeeping.persistence;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import se.swedsoft.bookkeeping.data.SSNewAccountingYear;
import se.swedsoft.bookkeeping.data.SSNewCompany;
import se.swedsoft.bookkeeping.data.SSSupplierInvoice;
import se.swedsoft.bookkeeping.data.SSSupplierInvoiceRow;
import se.swedsoft.bookkeeping.data.common.SSUnit;
import se.swedsoft.bookkeeping.data.system.SSDB;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for supplier-invoice repository wiring in schema V2.
 */
@Tag("integration")
class SSSupplierInvoiceV2RepositoryTest {

    private static final String JDBC_URL = "jdbc:hsqldb:mem:fribok_test_v2_supplierinvoice_repo";

    private static Connection connection;

    @BeforeAll
    static void setupV2Schema() throws Exception {
        System.setProperty("fribok.schema.version", "v2");

        Class.forName("org.hsqldb.jdbcDriver");
        connection = DriverManager.getConnection(JDBC_URL, "sa", "");

        SSDB.getInstance().startupLocal(connection);

        Integer companyId = createCompany("V2 Supplier Invoice Repo Test AB");
        SSNewCompany company = new SSNewCompany();
        company.setId(companyId);
        company.setName("V2 Supplier Invoice Repo Test AB");
        SSDB.getInstance().setCurrentCompany(company);

        SSNewAccountingYear year = new SSNewAccountingYear(
                se.swedsoft.bookkeeping.util.SSDateUtil.toDate(LocalDate.of(2025, 1, 1)),
                se.swedsoft.bookkeeping.util.SSDateUtil.toDate(LocalDate.of(2025, 12, 31)));
        SSDB.getInstance().addAccountingYear(year);
        SSDB.getInstance().setCurrentYear(year);

        Repositories.init(SSDB.getInstance());
    }

    @AfterAll
    static void teardownV2Schema() throws Exception {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } finally {
            System.clearProperty("fribok.schema.version");
        }
    }

    @BeforeEach
    void clearCaches() {
        SSDB.getInstance().clearLists();
        SSDB.getInstance().getCurrentYear();
    }

    @Test
    void repositoriesInitUsesV2SupplierInvoiceRepository() {
        assertThat(Repositories.isSchemaV2()).isTrue();
        assertThat(Repositories.supplierInvoices()).isNotNull();
    }

    @Test
    void addAndFetchSupplierInvoiceViaRepository() {
        SSSupplierInvoice supplierInvoice = supplierInvoice("SUP-REPO-001", "Repo Supplier AB", "REF-REPO-001");
        supplierInvoice.getRows().add(invoiceRow("P-REPO-001", "Repo row", new BigDecimal("125.00"), 2, 4010));

        Repositories.supplierInvoices().add(supplierInvoice);
        assertThat(supplierInvoice.getNumber()).isGreaterThan(0);

        SSDB.getInstance().clearLists();
        Optional<SSSupplierInvoice> fetched = Repositories.supplierInvoices().findBySupplierInvoice(supplierInvoice);
        assertThat(fetched).isPresent();
        assertThat(fetched.get().getSupplierName()).isEqualTo("Repo Supplier AB");
        assertThat(fetched.get().getRows()).hasSize(1);
        assertThat(fetched.get().getRows().get(0).getDescription()).isEqualTo("Repo row");

        Repositories.supplierInvoices().delete(fetched.get());
    }

    @Test
    void updateAndDeleteSupplierInvoiceViaRepository() {
        SSSupplierInvoice supplierInvoice = supplierInvoice("SUP-REPO-002", "Before Repo Update", "REF-REPO-002");
        supplierInvoice.getRows().add(
                invoiceRow("P-REPO-002", "Before update row", new BigDecimal("100.00"), 1, 4010));
        Repositories.supplierInvoices().add(supplierInvoice);

        SSDB.getInstance().clearLists();
        Optional<SSSupplierInvoice> fetched = Repositories.supplierInvoices().findBySupplierInvoice(supplierInvoice);
        assertThat(fetched).isPresent();

        SSSupplierInvoice updatedInvoice = fetched.get();
        updatedInvoice.setSupplierName("After Repo Update");
        updatedInvoice.setReferencenumber("REF-REPO-UPDATED");
        updatedInvoice.getRows().clear();
        updatedInvoice.getRows().add(
                invoiceRow("P-REPO-003", "After update row", new BigDecimal("750.00"), 3, 4041));
        Repositories.supplierInvoices().update(updatedInvoice);

        SSDB.getInstance().clearLists();
        Optional<SSSupplierInvoice> updated = Repositories.supplierInvoices().findBySupplierInvoice(supplierInvoice);
        assertThat(updated).isPresent();
        assertThat(updated.get().getSupplierName()).isEqualTo("After Repo Update");
        assertThat(updated.get().getReferencenumber()).isEqualTo("REF-REPO-UPDATED");
        assertThat(updated.get().getRows()).hasSize(1);
        assertThat(updated.get().getRows().get(0).getDescription()).isEqualTo("After update row");

        Integer number = updated.get().getNumber();
        Repositories.supplierInvoices().delete(updated.get());
        SSDB.getInstance().clearLists();
        List<SSSupplierInvoice> all = Repositories.supplierInvoices().findAll();
        assertThat(all).extracting(SSSupplierInvoice::getNumber).doesNotContain(number);
    }

    private static SSSupplierInvoice supplierInvoice(String supplierNr, String supplierName, String referenceNumber) {
        SSSupplierInvoice supplierInvoice = new SSSupplierInvoice();
        supplierInvoice.setSupplierNr(supplierNr);
        supplierInvoice.setSupplierName(supplierName);
        supplierInvoice.setReferencenumber(referenceNumber);
        supplierInvoice.setLocalDate(LocalDate.of(2025, 7, 10));
        supplierInvoice.setLocalDueDate(LocalDate.of(2025, 8, 9));
        supplierInvoice.setCurrencyRate(new BigDecimal("10.00"));
        supplierInvoice.setTaxSum(new BigDecimal("100.00"));
        supplierInvoice.setRoundingSum(new BigDecimal("0.25"));
        supplierInvoice.setEntered(false);
        supplierInvoice.setStockInfluencing(true);
        supplierInvoice.setBGCEntered(false);
        return supplierInvoice;
    }

    private static SSSupplierInvoiceRow invoiceRow(
            String productNr,
            String description,
            BigDecimal unitPrice,
            int quantity,
            int accountNumber) {
        SSSupplierInvoiceRow row = new SSSupplierInvoiceRow();
        row.setProductNr(productNr);
        row.setDescription(description);
        row.setUnitprice(unitPrice);
        row.setQuantity(quantity);
        row.setUnit(new SSUnit("st", "st"));
        row.setUnitFreight(new BigDecimal("3.50"));
        row.setAccountNr(accountNumber);
        row.setProjectNr("PRJ-1");
        row.setResultUnitNr("RES-1");
        return row;
    }

    private static Integer createCompany(String name) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO tbl_company(name) VALUES (?)", Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, name);
            statement.executeUpdate();
            connection.commit();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        }
        throw new IllegalStateException(
                "Could not create test company for supplier-invoice repository V2 integration test");
    }
}

