package se.swedsoft.bookkeeping.data.system;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import se.swedsoft.bookkeeping.data.SSNewAccountingYear;
import se.swedsoft.bookkeeping.data.SSNewCompany;
import se.swedsoft.bookkeeping.data.SSOrder;
import se.swedsoft.bookkeeping.data.SSTender;
import se.swedsoft.bookkeeping.data.base.SSSaleRow;
import se.swedsoft.bookkeeping.data.common.SSCurrency;
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
 * Integration slice for tender CRUD against schema V2.
 */
@Tag("integration")
class SSTenderV2IntegrationTest {

    private static final String JDBC_URL = "jdbc:hsqldb:mem:fribok_test_v2_tender";

    private static Connection connection;

    @BeforeAll
    static void setupV2Schema() throws Exception {
        System.setProperty("fribok.schema.version", "v2");

        Class.forName("org.hsqldb.jdbcDriver");
        connection = DriverManager.getConnection(JDBC_URL, "sa", "");

        SSDB.getInstance().startupLocal(connection);

        Integer companyId = createCompany("V2 Tender Test Company AB");
        SSNewCompany company = new SSNewCompany();
        company.setId(companyId);
        company.setName("V2 Tender Test Company AB");
        company.setCurrency(new SSCurrency("SEK", "SEK"));
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
    void addAndFetchTenderWithRowsInSchemaV2() {
        SSTender tender = tender("TEND-V2-CUST-001", "V2 Tender Customer AB");
        tender.getRows().add(tenderRow("P-TEN-001", "Consulting", new BigDecimal("1200.00"), 2, 3010));
        tender.getRows().add(tenderRow("P-TEN-002", "Support", new BigDecimal("500.00"), 1, 3041));

        SSDB.getInstance().addTender(tender);

        assertThat(tender.getNumber()).isGreaterThan(0);

        SSDB.getInstance().clearLists();
        Optional<SSTender> fetched = SSDB.getInstance().getTender(tender);
        assertThat(fetched).isPresent();
        assertThat(fetched.get().getCustomerNr()).isEqualTo("TEND-V2-CUST-001");
        assertThat(fetched.get().getCustomerName()).isEqualTo("V2 Tender Customer AB");
        assertThat(fetched.get().getCurrencyRate()).isEqualByComparingTo("10.50");
        assertThat(fetched.get().getLocalDate()).isEqualTo(LocalDate.of(2025, 7, 25));
        assertThat(fetched.get().getLocalExpires()).isEqualTo(LocalDate.of(2025, 8, 30));
        assertThat(fetched.get().getText()).isEqualTo("Tender text in V2");
        assertThat(fetched.get().getOrderNr()).isEqualTo(94001);
        assertThat(fetched.get().getRows()).hasSize(2);
        assertThat(fetched.get().getRows().get(0).getDescription()).isEqualTo("Consulting");
        assertThat(fetched.get().getRows().get(0).getUnitprice()).isEqualByComparingTo("1200.00");
        assertThat(fetched.get().getRows().get(0).getQuantity()).isEqualTo(2);

        SSDB.getInstance().deleteTender(tender);
    }

    @Test
    void updateAndDeleteTenderInSchemaV2() {
        SSTender tender = tender("TEND-V2-CUST-002", "Before V2 Tender Update");
        tender.getRows().add(tenderRow("P-TEN-003", "Initial row", new BigDecimal("100.00"), 1, 3010));
        SSDB.getInstance().addTender(tender);

        SSDB.getInstance().clearLists();
        Optional<SSTender> fetched = SSDB.getInstance().getTender(tender);
        assertThat(fetched).isPresent();

        SSTender updatedTender = fetched.get();
        updatedTender.setCustomerName("After V2 Tender Update");
        updatedTender.setText("Updated tender text in V2");
        updatedTender.setCurrency(null);
        updatedTender.setLocalExpires(LocalDate.of(2025, 9, 10));
        updatedTender.setOrder(null);
        updatedTender.getRows().clear();
        updatedTender.getRows().add(tenderRow("P-TEN-004", "Updated row", new BigDecimal("750.00"), 3, 3041));
        SSDB.getInstance().updateTender(updatedTender);

        SSDB.getInstance().clearLists();
        Optional<SSTender> updated = SSDB.getInstance().getTender(tender);
        assertThat(updated).isPresent();
        assertThat(updated.get().getCustomerName()).isEqualTo("After V2 Tender Update");
        assertThat(updated.get().getText()).isEqualTo("Updated tender text in V2");
        assertThat(updated.get().getLocalExpires()).isEqualTo(LocalDate.of(2025, 9, 10));
        assertThat(updated.get().getOrderNr()).isNull();
        assertThat(updated.get().getRows()).hasSize(1);
        assertThat(updated.get().getRows().get(0).getDescription()).isEqualTo("Updated row");
        assertThat(updated.get().getRows().get(0).getQuantity()).isEqualTo(3);

        Integer tenderNumber = updated.get().getNumber();
        SSDB.getInstance().deleteTender(updated.get());
        SSDB.getInstance().clearLists();
        List<SSTender> all = SSDB.getInstance().getTenders();
        assertThat(all).extracting(SSTender::getNumber).doesNotContain(tenderNumber);
    }

    private static SSTender tender(String customerNr, String customerName) {
        SSTender tender = new SSTender();
        tender.setCurrency(null);
        tender.setCustomerNr(customerNr);
        tender.setCustomerName(customerName);
        tender.setLocalDate(LocalDate.of(2025, 7, 25));
        tender.setLocalExpires(LocalDate.of(2025, 8, 30));
        tender.setCurrencyRate(new BigDecimal("10.50"));
        tender.setText("Tender text in V2");
        SSOrder order = new SSOrder();
        order.setNumber(94001);
        tender.setOrder(order);
        return tender;
    }

    private static SSSaleRow tenderRow(String productNr, String description, BigDecimal unitPrice, int quantity,
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
        throw new IllegalStateException("Could not create test company for schema V2 tender test");
    }
}

