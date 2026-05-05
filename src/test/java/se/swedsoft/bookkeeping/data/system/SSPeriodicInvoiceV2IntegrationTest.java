package se.swedsoft.bookkeeping.data.system;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import se.swedsoft.bookkeeping.data.SSInvoice;
import se.swedsoft.bookkeeping.data.SSNewAccountingYear;
import se.swedsoft.bookkeeping.data.SSNewCompany;
import se.swedsoft.bookkeeping.data.SSPeriodicInvoice;
import se.swedsoft.bookkeeping.data.base.SSSaleRow;
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
 * Integration slice for periodic invoice CRUD against schema V2.
 */
@Tag("integration")
class SSPeriodicInvoiceV2IntegrationTest {

    private static final String JDBC_URL = "jdbc:hsqldb:mem:fribok_test_v2_periodicinvoice";

    private static Connection connection;

    @BeforeAll
    static void setupV2Schema() throws Exception {
        System.setProperty("fribok.schema.version", "v2");

        Class.forName("org.hsqldb.jdbcDriver");
        connection = DriverManager.getConnection(JDBC_URL, "sa", "");

        SSDB.getInstance().startupLocal(connection);

        Integer companyId = createCompany("V2 Periodic Invoice Test Company AB");
        SSNewCompany company = new SSNewCompany();
        company.setId(companyId);
        company.setName("V2 Periodic Invoice Test Company AB");
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
    void addAndFetchPeriodicInvoiceWithTemplateRowsInSchemaV2() {
        SSPeriodicInvoice periodicInvoice = periodicInvoice("PINV-V2-CUST-001", "V2 Periodic Customer AB");
        periodicInvoice.getTemplate().getRows().add(row("P-PINV-001", "Subscription", new BigDecimal("1200.00"), 1, 3010));
        periodicInvoice.getTemplate().getRows().add(row("P-PINV-002", "Support", new BigDecimal("300.00"), 2, 3041));

        SSDB.getInstance().addPeriodicInvoice(periodicInvoice);

        assertThat(periodicInvoice.getNumber()).isGreaterThan(0);

        SSDB.getInstance().clearLists();
        Optional<SSPeriodicInvoice> fetched = SSDB.getInstance().getPeriodicInvoice(periodicInvoice);
        assertThat(fetched).isPresent();
        assertThat(fetched.get().getDescription()).isEqualTo("Periodic template in V2");
        assertThat(fetched.get().getCount()).isEqualTo(3);
        assertThat(fetched.get().getPeriod()).isEqualTo(1);
        assertThat(fetched.get().getLocalDate()).isEqualTo(LocalDate.of(2025, 7, 1));
        assertThat(fetched.get().getLocalPeriodStart()).isEqualTo(LocalDate.of(2025, 7, 1));
        assertThat(fetched.get().getLocalPeriodEnd()).isEqualTo(LocalDate.of(2025, 7, 31));
        assertThat(fetched.get().isAppendInformation()).isTrue();
        assertThat(fetched.get().getTemplate().getCustomerNr()).isEqualTo("PINV-V2-CUST-001");
        assertThat(fetched.get().getTemplate().getCustomerName()).isEqualTo("V2 Periodic Customer AB");
        assertThat(fetched.get().getTemplate().getText()).isEqualTo("Periodic invoice text in V2");
        assertThat(fetched.get().getTemplate().getRows()).hasSize(2);
        assertThat(fetched.get().getTemplate().getRows().get(0).getDescription()).isEqualTo("Subscription");
        assertThat(fetched.get().getTemplate().getRows().get(0).getUnitprice()).isEqualByComparingTo("1200.00");

        SSDB.getInstance().deletePeriodicInvoice(periodicInvoice);
    }

    @Test
    void updateAndDeletePeriodicInvoiceInSchemaV2() {
        SSPeriodicInvoice periodicInvoice = periodicInvoice("PINV-V2-CUST-002", "Before V2 Periodic Update");
        periodicInvoice.getTemplate().getRows().add(row("P-PINV-003", "Initial row", new BigDecimal("100.00"), 1, 3010));
        SSDB.getInstance().addPeriodicInvoice(periodicInvoice);

        SSDB.getInstance().clearLists();
        Optional<SSPeriodicInvoice> fetched = SSDB.getInstance().getPeriodicInvoice(periodicInvoice);
        assertThat(fetched).isPresent();

        SSPeriodicInvoice updatedPeriodicInvoice = fetched.get();
        updatedPeriodicInvoice.setDescription("After V2 Periodic Update");
        updatedPeriodicInvoice.setCount(6);
        updatedPeriodicInvoice.setPeriod(2);
        updatedPeriodicInvoice.setAppendInformation(false);
        SSInvoice updatedTemplate = updatedPeriodicInvoice.getTemplate();
        updatedTemplate.setCustomerName("After V2 Periodic Customer Update");
        updatedTemplate.setText("Updated periodic invoice text in V2");
        updatedTemplate.setLocalDueDate(LocalDate.of(2025, 9, 10));
        updatedTemplate.getRows().clear();
        updatedTemplate.getRows().add(row("P-PINV-004", "Updated row", new BigDecimal("750.00"), 3, 3041));
        SSDB.getInstance().updatePeriodicInvoice(updatedPeriodicInvoice);

        SSDB.getInstance().clearLists();
        Optional<SSPeriodicInvoice> updated = SSDB.getInstance().getPeriodicInvoice(periodicInvoice);
        assertThat(updated).isPresent();
        assertThat(updated.get().getDescription()).isEqualTo("After V2 Periodic Update");
        assertThat(updated.get().getCount()).isEqualTo(6);
        assertThat(updated.get().getPeriod()).isEqualTo(2);
        assertThat(updated.get().isAppendInformation()).isFalse();
        assertThat(updated.get().getTemplate().getCustomerName()).isEqualTo("After V2 Periodic Customer Update");
        assertThat(updated.get().getTemplate().getText()).isEqualTo("Updated periodic invoice text in V2");
        assertThat(updated.get().getTemplate().getLocalDueDate()).isEqualTo(LocalDate.of(2025, 9, 10));
        assertThat(updated.get().getTemplate().getRows()).hasSize(1);
        assertThat(updated.get().getTemplate().getRows().get(0).getDescription()).isEqualTo("Updated row");
        assertThat(updated.get().getTemplate().getRows().get(0).getQuantity()).isEqualTo(3);

        Integer periodicInvoiceNumber = updated.get().getNumber();
        SSDB.getInstance().deletePeriodicInvoice(updated.get());
        SSDB.getInstance().clearLists();
        List<SSPeriodicInvoice> all = SSDB.getInstance().getPeriodicInvoices();
        assertThat(all).extracting(SSPeriodicInvoice::getNumber).doesNotContain(periodicInvoiceNumber);
    }

    private static SSPeriodicInvoice periodicInvoice(String customerNr, String customerName) {
        SSPeriodicInvoice periodicInvoice = new SSPeriodicInvoice();
        periodicInvoice.setLocalDate(LocalDate.of(2025, 7, 1));
        periodicInvoice.setCount(3);
        periodicInvoice.setPeriod(1);
        periodicInvoice.setDescription("Periodic template in V2");
        periodicInvoice.setLocalPeriodStart(LocalDate.of(2025, 7, 1));
        periodicInvoice.setLocalPeriodEnd(LocalDate.of(2025, 7, 31));
        periodicInvoice.setAppendPeriod(true);
        periodicInvoice.setAppendInformation(true);
        periodicInvoice.setInformation("Detta ar faktura [FAK] av [TOT].");

        SSInvoice template = periodicInvoice.getTemplate();
        template.setCurrency(null);
        template.setCustomerNr(customerNr);
        template.setCustomerName(customerName);
        template.setLocalDueDate(LocalDate.of(2025, 8, 14));
        template.setCurrencyRate(new BigDecimal("10.50"));
        template.setText("Periodic invoice text in V2");
        template.setYourOrderNumber("ORDER-V2-PERIODIC-001");
        template.setStockInfluencing(true);

        return periodicInvoice;
    }

    private static SSSaleRow row(String productNr, String description, BigDecimal unitPrice, int quantity,
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
        throw new IllegalStateException("Could not create test company for schema V2 periodic invoice test");
    }
}

