package se.swedsoft.bookkeeping.persistence;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import se.swedsoft.bookkeeping.data.SSInvoice;
import se.swedsoft.bookkeeping.data.SSNewAccountingYear;
import se.swedsoft.bookkeeping.data.SSNewCompany;
import se.swedsoft.bookkeeping.data.base.SSSaleRow;
import se.swedsoft.bookkeeping.data.common.SSInvoiceType;
import se.swedsoft.bookkeeping.data.common.SSTaxCode;
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
 * Integration tests for invoice repository wiring in schema V2.
 */
@Tag("integration")
class SSInvoiceV2RepositoryTest {

    private static final String JDBC_URL = "jdbc:hsqldb:mem:fribok_test_v2_invoice_repo";

    private static Connection connection;

    @BeforeAll
    static void setupV2Schema() throws Exception {
        System.setProperty("fribok.schema.version", "v2");

        Class.forName("org.hsqldb.jdbcDriver");
        connection = DriverManager.getConnection(JDBC_URL, "sa", "");

        SSDB.getInstance().startupLocal(connection);

        Integer companyId = createCompany("V2 Invoice Repo Test AB");
        SSNewCompany company = new SSNewCompany();
        company.setId(companyId);
        company.setName("V2 Invoice Repo Test AB");
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
    void repositoriesInitUsesV2InvoiceRepository() {
        assertThat(Repositories.isSchemaV2()).isTrue();
        assertThat(Repositories.invoices()).isNotNull();
    }

    @Test
    void addAndFetchInvoiceViaRepository() {
        SSInvoice invoice = invoice("INV-REPO-CUST-001", "Repo Invoice Customer AB");
        invoice.getRows().add(invoiceRow("P-INV-REPO-001", "Repo row", new BigDecimal("1250.00"), 2, 3010));

        Repositories.invoices().add(invoice);
        assertThat(invoice.getNumber()).isGreaterThan(0);

        SSDB.getInstance().clearLists();
        Optional<SSInvoice> fetched = Repositories.invoices().findByInvoice(invoice);
        assertThat(fetched).isPresent();
        assertThat(fetched.get().getCustomerName()).isEqualTo("Repo Invoice Customer AB");
        assertThat(fetched.get().getRows()).hasSize(1);
        assertThat(fetched.get().getRows().get(0).getDescription()).isEqualTo("Repo row");

        Repositories.invoices().delete(fetched.get());
    }

    @Test
    void updateAndDeleteInvoiceViaRepository() {
        SSInvoice invoice = invoice("INV-REPO-CUST-002", "Before Invoice Repo Update");
        invoice.getRows().add(invoiceRow("P-INV-REPO-002", "Before update row", new BigDecimal("100.00"), 1, 3010));
        Repositories.invoices().add(invoice);

        SSDB.getInstance().clearLists();
        Optional<SSInvoice> fetched = Repositories.invoices().findByInvoice(invoice);
        assertThat(fetched).isPresent();

        SSInvoice updatedInvoice = fetched.get();
        updatedInvoice.setCustomerName("After Invoice Repo Update");
        updatedInvoice.setText("Updated invoice text via repository");
        updatedInvoice.setLocalDueDate(LocalDate.of(2025, 9, 1));
        updatedInvoice.getRows().clear();
        updatedInvoice.getRows().add(invoiceRow("P-INV-REPO-003", "After update row", new BigDecimal("750.00"), 3, 3041));
        Repositories.invoices().update(updatedInvoice);

        SSDB.getInstance().clearLists();
        Optional<SSInvoice> updated = Repositories.invoices().findByInvoice(invoice);
        assertThat(updated).isPresent();
        assertThat(updated.get().getCustomerName()).isEqualTo("After Invoice Repo Update");
        assertThat(updated.get().getText()).isEqualTo("Updated invoice text via repository");
        assertThat(updated.get().getLocalDueDate()).isEqualTo(LocalDate.of(2025, 9, 1));
        assertThat(updated.get().getRows()).hasSize(1);
        assertThat(updated.get().getRows().get(0).getDescription()).isEqualTo("After update row");

        Integer number = updated.get().getNumber();
        Repositories.invoices().delete(updated.get());
        SSDB.getInstance().clearLists();
        List<SSInvoice> all = Repositories.invoices().findAll();
        assertThat(all).extracting(SSInvoice::getNumber).doesNotContain(number);
    }

    private static SSInvoice invoice(String customerNr, String customerName) {
        SSInvoice invoice = new SSInvoice();
        invoice.setCustomerNr(customerNr);
        invoice.setCustomerName(customerName);
        invoice.setLocalDate(LocalDate.of(2025, 7, 15));
        invoice.setLocalDueDate(LocalDate.of(2025, 8, 14));
        invoice.setCurrencyRate(new BigDecimal("10.50"));
        invoice.setType(SSInvoiceType.CASH);
        invoice.setText("Invoice text in repository test");
        invoice.setYourOrderNumber("ORDER-REPO-INV-001");
        return invoice;
    }

    private static SSSaleRow invoiceRow(String productNr, String description, BigDecimal unitPrice, int quantity,
                                        int accountNumber) {
        SSSaleRow row = new SSSaleRow();
        row.setProductNr(productNr);
        row.setDescription(description);
        row.setUnitprice(unitPrice);
        row.setQuantity(quantity);
        row.setUnit(new SSUnit("st", "st"));
        row.setTaxCode(SSTaxCode.TAXRATE_1);
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
        throw new IllegalStateException("Could not create test company for invoice repository V2 integration test");
    }
}

