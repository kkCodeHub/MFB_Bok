package se.swedsoft.bookkeeping.integration.system;

import se.swedsoft.bookkeeping.data.system.SSDB;
import se.swedsoft.bookkeeping.data.system.SSSalesContext;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration slice for invoice CRUD against schema V2.
 */
@Tag("integration")
class SSInvoiceV2IntegrationTest {

    private static final String JDBC_URL = "jdbc:hsqldb:mem:fribok_test_s-s-i-n-v-o-i-c-e-v2-i-n-t-e-g-r-a-t-i-o-n-t-e-s-t";

    private static Connection connection;

    @BeforeAll
    static void setupV2Schema() throws Exception {
        System.setProperty("fribok.schema.version", "v2");

        Class.forName("org.hsqldb.jdbcDriver");
        connection = DriverManager.getConnection(JDBC_URL, "sa", "");

        se.swedsoft.bookkeeping.data.system.SSSystemConfigContext.startupLocal(connection);

        Integer companyId = createCompany("V2 Invoice Test Company AB");
        SSNewCompany company = new SSNewCompany();
        company.setId(companyId);
        company.setName("V2 Invoice Test Company AB");
        SSDB.getInstance().setCurrentCompany(company);

        SSNewAccountingYear year = new SSNewAccountingYear();
        year.setLocalFrom(LocalDate.of(2025, 1, 1));
        year.setLocalTo(LocalDate.of(2025, 12, 31));
        se.swedsoft.bookkeeping.data.system.SSCompanyYearContext.addAccountingYear(year);
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
    void resetState() {
        se.swedsoft.bookkeeping.data.system.SSEventTriggerSyncContext.clearCachedLists();
        SSDB.getInstance().getCurrentYear();
    }

    @AfterEach
    void clearStateAfterTest() {
        se.swedsoft.bookkeeping.data.system.SSEventTriggerSyncContext.clearCachedLists();
    }

    @Test
    void addAndFetchInvoiceWithRowsInSchemaV2() {
        SSInvoice invoice = invoice("INV-V2-CUST-001", "V2 Invoice Customer AB");
        invoice.getRows().add(invoiceRow("P-INV-001", "Consulting", new BigDecimal("1250.00"), 20, 3010));
        invoice.getRows().add(invoiceRow("P-INV-002", "Support", new BigDecimal("500.00"), 10, 3041));

        SSSalesContext.addInvoice(invoice);

        assertThat(invoice.getNumber()).isGreaterThan(0);
        assertThat(loadPersistedInvoiceRowCounts(invoice.getNumber())).containsExactly(20, 10);

        se.swedsoft.bookkeeping.data.system.SSEventTriggerSyncContext.clearCachedLists();
        Optional<SSInvoice> fetched = SSSalesContext.getInvoice(invoice);
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
        assertThat(fetched.get().getRows().get(0).getQuantity()).isEqualTo(20);
        assertThat(fetched.get().getRows().get(0).getTaxCode()).isEqualTo(SSTaxCode.TAXRATE_1);
        assertThat(fetched.get().getRows().get(0).getAccountNr()).isEqualTo(3010);

        SSSalesContext.deleteInvoice(invoice);
    }

    @Test
    void updateAndDeleteInvoiceInSchemaV2() {
        SSInvoice invoice = invoice("INV-V2-CUST-002", "Before V2 Invoice Update");
        invoice.getRows().add(invoiceRow("P-INV-003", "Initial row", new BigDecimal("100.00"), 10, 3010));
        SSSalesContext.addInvoice(invoice);

        se.swedsoft.bookkeeping.data.system.SSEventTriggerSyncContext.clearCachedLists();
        Optional<SSInvoice> fetched = SSSalesContext.getInvoice(invoice);
        assertThat(fetched).isPresent();

        SSInvoice updatedInvoice = fetched.get();
        updatedInvoice.setCustomerName("After V2 Invoice Update");
        updatedInvoice.setText("Updated invoice text in V2");
        updatedInvoice.setLocalDueDate(LocalDate.of(2025, 9, 1));
        updatedInvoice.getRows().clear();
        updatedInvoice.getRows().add(invoiceRow("P-INV-004", "Updated row", new BigDecimal("750.00"), 30, 3041));
        SSSalesContext.updateInvoice(updatedInvoice);
        assertThat(loadPersistedInvoiceRowCounts(updatedInvoice.getNumber())).containsExactly(30);

        se.swedsoft.bookkeeping.data.system.SSEventTriggerSyncContext.clearCachedLists();
        Optional<SSInvoice> updated = SSSalesContext.getInvoice(invoice);
        assertThat(updated).isPresent();
        assertThat(updated.get().getCustomerName()).isEqualTo("After V2 Invoice Update");
        assertThat(updated.get().getText()).isEqualTo("Updated invoice text in V2");
        assertThat(updated.get().getLocalDueDate()).isEqualTo(LocalDate.of(2025, 9, 1));
        assertThat(updated.get().getRows()).hasSize(1);
        assertThat(updated.get().getRows().get(0).getDescription()).isEqualTo("Updated row");
        assertThat(updated.get().getRows().get(0).getQuantity()).isEqualTo(30);

        Integer invoiceNumber = updated.get().getNumber();
        SSSalesContext.deleteInvoice(updated.get());
        se.swedsoft.bookkeeping.data.system.SSEventTriggerSyncContext.clearCachedLists();
        List<SSInvoice> all = SSSalesContext.getInvoices();
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
        se.swedsoft.bookkeeping.data.SSNewCompany company = new se.swedsoft.bookkeeping.data.SSNewCompany();
        company.setName(name);
        se.swedsoft.bookkeeping.data.system.SSCompanyYearContext.addCompany(company);
        return company.getId();
    }

    private static List<Integer> loadPersistedInvoiceRowCounts(Integer invoiceNumber) {
        List<Integer> counts = new ArrayList<>();
        Integer companyId = SSDB.getInstance().getCurrentCompany().getId();
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT r.count FROM tbl_invoice_row r "
                        + "JOIN tbl_invoice i ON i.id = r.invoice_id "
                        + "WHERE i.number=? AND i.companyid=? ORDER BY r.id")) {
            statement.setObject(1, invoiceNumber);
            statement.setObject(2, companyId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    counts.add((Integer) resultSet.getObject(1));
                }
            }
            return counts;
        } catch (Exception e) {
            throw new IllegalStateException("Could not read persisted invoice row counts", e);
        }
    }
}


