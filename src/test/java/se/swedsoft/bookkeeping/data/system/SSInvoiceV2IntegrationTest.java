package se.swedsoft.bookkeeping.data.system;

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
 * Integration slice for invoice CRUD against schema V2.
 */
@Tag("integration")
class SSInvoiceV2IntegrationTest {

    private static final String JDBC_URL = "jdbc:hsqldb:mem:fribok_test_v2_invoice";

    private static Connection connection;

    @BeforeAll
    static void setupV2Schema() throws Exception {
        System.setProperty("fribok.schema.version", "v2");

        Class.forName("org.hsqldb.jdbcDriver");
        connection = DriverManager.getConnection(JDBC_URL, "sa", "");

        SSDB.getInstance().startupLocal(connection);

        Integer companyId = createCompany("V2 Invoice Test Company AB");
        SSNewCompany company = new SSNewCompany();
        company.setId(companyId);
        company.setName("V2 Invoice Test Company AB");
        SSDB.getInstance().setCurrentCompany(company);

        SSNewAccountingYear year = new SSNewAccountingYear(
                se.swedsoft.bookkeeping.util.SSDateUtil.toDate(LocalDate.of(2025, 1, 1)),
                se.swedsoft.bookkeeping.util.SSDateUtil.toDate(LocalDate.of(2025, 12, 31)));
        SSDB.getInstance().addAccountingYear(year);
        SSDB.getInstance().setCurrentYear(year);
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
    void addAndFetchInvoiceWithRowsInSchemaV2() {
        SSInvoice invoice = invoice("INV-V2-CUST-001", "V2 Invoice Customer AB");
        invoice.getRows().add(invoiceRow("P-INV-001", "Consulting", new BigDecimal("1250.00"), 2, 3010));
        invoice.getRows().add(invoiceRow("P-INV-002", "Support", new BigDecimal("500.00"), 1, 3041));

        SSDB.getInstance().addInvoice(invoice);

        assertThat(invoice.getNumber()).isGreaterThan(0);

        SSDB.getInstance().clearLists();
        Optional<SSInvoice> fetched = SSDB.getInstance().getInvoice(invoice);
        assertThat(fetched).isPresent();
        assertThat(fetched.get().getCustomerNr()).isEqualTo("INV-V2-CUST-001");
        assertThat(fetched.get().getCustomerName()).isEqualTo("V2 Invoice Customer AB");
        assertThat(fetched.get().getType()).isEqualTo(SSInvoiceType.CASH);
        assertThat(fetched.get().getCurrencyRate()).isEqualByComparingTo("10.50");
        assertThat(fetched.get().getLocalDate()).isEqualTo(LocalDate.of(2025, 7, 15));
        assertThat(fetched.get().getLocalDueDate()).isEqualTo(LocalDate.of(2025, 8, 14));
        assertThat(fetched.get().getText()).isEqualTo("Invoice text in V2");
        assertThat(fetched.get().getRows()).hasSize(2);
        assertThat(fetched.get().getRows().get(0).getDescription()).isEqualTo("Consulting");
        assertThat(fetched.get().getRows().get(0).getUnitprice()).isEqualByComparingTo("1250.00");
        assertThat(fetched.get().getRows().get(0).getQuantity()).isEqualTo(2);
        assertThat(fetched.get().getRows().get(0).getTaxCode()).isEqualTo(SSTaxCode.TAXRATE_1);
        assertThat(fetched.get().getRows().get(0).getAccountNr()).isEqualTo(3010);

        SSDB.getInstance().deleteInvoice(invoice);
    }

    @Test
    void updateAndDeleteInvoiceInSchemaV2() {
        SSInvoice invoice = invoice("INV-V2-CUST-002", "Before V2 Invoice Update");
        invoice.getRows().add(invoiceRow("P-INV-003", "Initial row", new BigDecimal("100.00"), 1, 3010));
        SSDB.getInstance().addInvoice(invoice);

        SSDB.getInstance().clearLists();
        Optional<SSInvoice> fetched = SSDB.getInstance().getInvoice(invoice);
        assertThat(fetched).isPresent();

        SSInvoice updatedInvoice = fetched.get();
        updatedInvoice.setCustomerName("After V2 Invoice Update");
        updatedInvoice.setText("Updated invoice text in V2");
        updatedInvoice.setLocalDueDate(LocalDate.of(2025, 9, 1));
        updatedInvoice.getRows().clear();
        updatedInvoice.getRows().add(invoiceRow("P-INV-004", "Updated row", new BigDecimal("750.00"), 3, 3041));
        SSDB.getInstance().updateInvoice(updatedInvoice);

        SSDB.getInstance().clearLists();
        Optional<SSInvoice> updated = SSDB.getInstance().getInvoice(invoice);
        assertThat(updated).isPresent();
        assertThat(updated.get().getCustomerName()).isEqualTo("After V2 Invoice Update");
        assertThat(updated.get().getText()).isEqualTo("Updated invoice text in V2");
        assertThat(updated.get().getLocalDueDate()).isEqualTo(LocalDate.of(2025, 9, 1));
        assertThat(updated.get().getRows()).hasSize(1);
        assertThat(updated.get().getRows().get(0).getDescription()).isEqualTo("Updated row");
        assertThat(updated.get().getRows().get(0).getQuantity()).isEqualTo(3);

        Integer invoiceNumber = updated.get().getNumber();
        SSDB.getInstance().deleteInvoice(updated.get());
        SSDB.getInstance().clearLists();
        List<SSInvoice> all = SSDB.getInstance().getInvoices();
        assertThat(all).extracting(SSInvoice::getNumber).doesNotContain(invoiceNumber);
    }

    private static SSInvoice invoice(String customerNr, String customerName) {
        SSInvoice invoice = new SSInvoice();
        invoice.setCustomerNr(customerNr);
        invoice.setCustomerName(customerName);
        invoice.setLocalDate(LocalDate.of(2025, 7, 15));
        invoice.setLocalDueDate(LocalDate.of(2025, 8, 14));
        invoice.setCurrencyRate(new BigDecimal("10.50"));
        invoice.setType(SSInvoiceType.CASH);
        invoice.setText("Invoice text in V2");
        invoice.setYourOrderNumber("ORDER-V2-001");
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
        throw new IllegalStateException("Could not create test company for schema V2 invoice test");
    }
}

