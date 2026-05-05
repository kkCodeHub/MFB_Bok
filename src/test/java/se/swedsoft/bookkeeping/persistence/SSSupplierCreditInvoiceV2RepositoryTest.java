package se.swedsoft.bookkeeping.persistence;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import se.swedsoft.bookkeeping.data.SSNewAccountingYear;
import se.swedsoft.bookkeeping.data.SSNewCompany;
import se.swedsoft.bookkeeping.data.SSSupplierCreditInvoice;
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
 * Integration tests for supplier-credit-invoice repository wiring in schema V2.
 */
@Tag("integration")
class SSSupplierCreditInvoiceV2RepositoryTest {

    private static final String JDBC_URL = "jdbc:hsqldb:mem:fribok_test_v2_suppliercreditinvoice_repo";

    private static Connection connection;

    @BeforeAll
    static void setupV2Schema() throws Exception {
        System.setProperty("fribok.schema.version", "v2");

        Class.forName("org.hsqldb.jdbcDriver");
        connection = DriverManager.getConnection(JDBC_URL, "sa", "");

        SSDB.getInstance().startupLocal(connection);

        Integer companyId = createCompany("V2 Supplier Credit Invoice Repo Test AB");
        SSNewCompany company = new SSNewCompany();
        company.setId(companyId);
        company.setName("V2 Supplier Credit Invoice Repo Test AB");
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
    void repositoriesInitUsesV2SupplierCreditInvoiceRepository() {
        assertThat(Repositories.isSchemaV2()).isTrue();
        assertThat(Repositories.supplierCreditInvoices()).isNotNull();
    }

    @Test
    void addAndFetchSupplierCreditInvoiceViaRepository() {
        SSSupplierCreditInvoice supplierCreditInvoice = supplierCreditInvoice(
                "SUP-CR-REPO-001", "Repo Supplier Credit AB", "REF-CR-REPO-001", 1101);
        supplierCreditInvoice.getRows().add(
                invoiceRow("P-CR-REPO-001", "Repo row", new BigDecimal("125.00"), 2, 4010));

        Repositories.supplierCreditInvoices().add(supplierCreditInvoice);
        assertThat(supplierCreditInvoice.getNumber()).isGreaterThan(0);

        SSDB.getInstance().clearLists();
        Optional<SSSupplierCreditInvoice> fetched = Repositories.supplierCreditInvoices()
                .findBySupplierCreditInvoice(supplierCreditInvoice);
        assertThat(fetched).isPresent();
        assertThat(fetched.get().getSupplierName()).isEqualTo("Repo Supplier Credit AB");
        assertThat(fetched.get().getCreditingNr()).isEqualTo(1101);
        assertThat(fetched.get().getRows()).hasSize(1);
        assertThat(fetched.get().getRows().get(0).getDescription()).isEqualTo("Repo row");

        Repositories.supplierCreditInvoices().delete(fetched.get());
    }

    @Test
    void updateAndDeleteSupplierCreditInvoiceViaRepository() {
        SSSupplierCreditInvoice supplierCreditInvoice = supplierCreditInvoice(
                "SUP-CR-REPO-002", "Before Repo Update", "REF-CR-REPO-002", 1102);
        supplierCreditInvoice.getRows().add(
                invoiceRow("P-CR-REPO-002", "Before update row", new BigDecimal("100.00"), 1, 4010));
        Repositories.supplierCreditInvoices().add(supplierCreditInvoice);

        SSDB.getInstance().clearLists();
        Optional<SSSupplierCreditInvoice> fetched = Repositories.supplierCreditInvoices()
                .findBySupplierCreditInvoice(supplierCreditInvoice);
        assertThat(fetched).isPresent();

        SSSupplierCreditInvoice updatedInvoice = fetched.get();
        updatedInvoice.setSupplierName("After Repo Update");
        updatedInvoice.setReferencenumber("REF-CR-REPO-UPDATED");
        updatedInvoice.setCreditingNr(2202);
        updatedInvoice.getRows().clear();
        updatedInvoice.getRows().add(
                invoiceRow("P-CR-REPO-003", "After update row", new BigDecimal("750.00"), 3, 4041));
        Repositories.supplierCreditInvoices().update(updatedInvoice);

        SSDB.getInstance().clearLists();
        Optional<SSSupplierCreditInvoice> updated = Repositories.supplierCreditInvoices()
                .findBySupplierCreditInvoice(supplierCreditInvoice);
        assertThat(updated).isPresent();
        assertThat(updated.get().getSupplierName()).isEqualTo("After Repo Update");
        assertThat(updated.get().getReferencenumber()).isEqualTo("REF-CR-REPO-UPDATED");
        assertThat(updated.get().getCreditingNr()).isEqualTo(2202);
        assertThat(updated.get().getRows()).hasSize(1);
        assertThat(updated.get().getRows().get(0).getDescription()).isEqualTo("After update row");

        Integer number = updated.get().getNumber();
        Repositories.supplierCreditInvoices().delete(updated.get());
        SSDB.getInstance().clearLists();
        List<SSSupplierCreditInvoice> all = Repositories.supplierCreditInvoices().findAll();
        assertThat(all).extracting(SSSupplierCreditInvoice::getNumber).doesNotContain(number);
    }

    private static SSSupplierCreditInvoice supplierCreditInvoice(
            String supplierNr,
            String supplierName,
            String referenceNumber,
            Integer creditingNr) {
        SSSupplierCreditInvoice supplierCreditInvoice = new SSSupplierCreditInvoice();
        supplierCreditInvoice.setCreditingNr(creditingNr);
        supplierCreditInvoice.setSupplierNr(supplierNr);
        supplierCreditInvoice.setSupplierName(supplierName);
        supplierCreditInvoice.setReferencenumber(referenceNumber);
        supplierCreditInvoice.setLocalDate(LocalDate.of(2025, 7, 15));
        supplierCreditInvoice.setLocalDueDate(LocalDate.of(2025, 8, 14));
        supplierCreditInvoice.setCurrencyRate(new BigDecimal("10.00"));
        supplierCreditInvoice.setTaxSum(new BigDecimal("100.00"));
        supplierCreditInvoice.setRoundingSum(new BigDecimal("0.25"));
        supplierCreditInvoice.setEntered(false);
        supplierCreditInvoice.setStockInfluencing(true);
        supplierCreditInvoice.setBGCEntered(false);
        return supplierCreditInvoice;
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
                "Could not create test company for supplier-credit-invoice repository V2 integration test");
    }
}

