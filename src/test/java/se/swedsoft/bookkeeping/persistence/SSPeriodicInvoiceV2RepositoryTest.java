package se.swedsoft.bookkeeping.persistence;

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
import se.swedsoft.bookkeeping.data.system.SSDB;
import se.swedsoft.bookkeeping.persistence.v2.V2PeriodicInvoiceRepository;

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
 * Integration tests for periodic-invoice repository wiring in schema V2.
 */
@Tag("integration")
class SSPeriodicInvoiceV2RepositoryTest {

    private static final String JDBC_URL = "jdbc:hsqldb:mem:fribok_test_v2_periodicinvoice_repo";

    private static Connection connection;

    @BeforeAll
    static void setupV2Schema() throws Exception {
        System.setProperty("fribok.schema.version", "v2");

        Class.forName("org.hsqldb.jdbcDriver");
        connection = DriverManager.getConnection(JDBC_URL, "sa", "");

        se.swedsoft.bookkeeping.data.system.SSSystemConfigContext.startupLocal(connection);

        Integer companyId = createCompany("V2 Periodic Invoice Repo Test AB");
        SSNewCompany company = new SSNewCompany();
        company.setId(companyId);
        company.setName("V2 Periodic Invoice Repo Test AB");
        SSDB.getInstance().setCurrentCompany(company);

        SSNewAccountingYear year = new SSNewAccountingYear();
        year.setLocalFrom(LocalDate.of(2025, 1, 1));
        year.setLocalTo(LocalDate.of(2025, 12, 31));
        se.swedsoft.bookkeeping.data.system.SSCompanyYearContext.addAccountingYear(year);
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
        se.swedsoft.bookkeeping.data.system.SSEventTriggerSyncContext.clearCachedLists();
        SSDB.getInstance().getCurrentYear();
    }

    @Test
    void repositoriesInitUsesV2PeriodicInvoiceRepository() {
        assertThat(Repositories.isSchemaV2()).isTrue();
        assertThat(Repositories.periodicInvoices()).isInstanceOf(V2PeriodicInvoiceRepository.class);
    }

    @Test
    void addAndFetchPeriodicInvoiceViaRepository() {
        SSPeriodicInvoice periodicInvoice = periodicInvoice("PINV-REPO-CUST-001", "Repo Periodic Customer AB");
        periodicInvoice.getTemplate().getRows().add(
                row("P-PINV-REPO-001", "Repo row", new BigDecimal("1200.00"), 1, 3010));

        Repositories.periodicInvoices().add(periodicInvoice);
        assertThat(periodicInvoice.getNumber()).isGreaterThan(0);

        se.swedsoft.bookkeeping.data.system.SSEventTriggerSyncContext.clearCachedLists();
        Optional<SSPeriodicInvoice> fetched = Repositories.periodicInvoices().findByPeriodicInvoice(periodicInvoice);
        assertThat(fetched).isPresent();
        assertThat(fetched.get().getDescription()).isEqualTo("Periodic template in repository test");
        assertThat(fetched.get().getTemplate().getCustomerName()).isEqualTo("Repo Periodic Customer AB");
        assertThat(fetched.get().getTemplate().getRows()).hasSize(1);
        assertThat(fetched.get().getTemplate().getRows().get(0).getDescription()).isEqualTo("Repo row");

        Repositories.periodicInvoices().delete(fetched.get());
    }

    @Test
    void updateAndDeletePeriodicInvoiceViaRepository() {
        SSPeriodicInvoice periodicInvoice = periodicInvoice("PINV-REPO-CUST-002", "Before Periodic Repo Update");
        periodicInvoice.getTemplate().getRows().add(
                row("P-PINV-REPO-002", "Before update row", new BigDecimal("100.00"), 1, 3010));
        Repositories.periodicInvoices().add(periodicInvoice);

        se.swedsoft.bookkeeping.data.system.SSEventTriggerSyncContext.clearCachedLists();
        Optional<SSPeriodicInvoice> fetched = Repositories.periodicInvoices().findByPeriodicInvoice(periodicInvoice);
        assertThat(fetched).isPresent();

        SSPeriodicInvoice updatedPeriodicInvoice = fetched.get();
        updatedPeriodicInvoice.setDescription("After Periodic Repo Update");
        updatedPeriodicInvoice.setCount(6);
        updatedPeriodicInvoice.setPeriod(2);
        updatedPeriodicInvoice.setAppendInformation(false);
        SSInvoice updatedTemplate = updatedPeriodicInvoice.getTemplate();
        updatedTemplate.setCustomerName("After Periodic Customer Repo Update");
        updatedTemplate.setText("Updated periodic invoice text via repository");
        updatedTemplate.setLocalDueDate(LocalDate.of(2025, 9, 10));
        updatedTemplate.getRows().clear();
        updatedTemplate.getRows().add(row("P-PINV-REPO-003", "After update row", new BigDecimal("750.00"), 3, 3041));
        Repositories.periodicInvoices().update(updatedPeriodicInvoice);

        se.swedsoft.bookkeeping.data.system.SSEventTriggerSyncContext.clearCachedLists();
        Optional<SSPeriodicInvoice> updated = Repositories.periodicInvoices().findByPeriodicInvoice(periodicInvoice);
        assertThat(updated).isPresent();
        assertThat(updated.get().getDescription()).isEqualTo("After Periodic Repo Update");
        assertThat(updated.get().getCount()).isEqualTo(6);
        assertThat(updated.get().getPeriod()).isEqualTo(2);
        assertThat(updated.get().isAppendInformation()).isFalse();
        assertThat(updated.get().getTemplate().getCustomerName()).isEqualTo("After Periodic Customer Repo Update");
        assertThat(updated.get().getTemplate().getText()).isEqualTo("Updated periodic invoice text via repository");
        assertThat(updated.get().getTemplate().getLocalDueDate()).isEqualTo(LocalDate.of(2025, 9, 10));
        assertThat(updated.get().getTemplate().getRows()).hasSize(1);
        assertThat(updated.get().getTemplate().getRows().get(0).getDescription()).isEqualTo("After update row");

        Integer number = updated.get().getNumber();
        Repositories.periodicInvoices().delete(updated.get());
        se.swedsoft.bookkeeping.data.system.SSEventTriggerSyncContext.clearCachedLists();
        List<SSPeriodicInvoice> all = Repositories.periodicInvoices().findAll();
        assertThat(all).extracting(SSPeriodicInvoice::getNumber).doesNotContain(number);
    }

    private static SSPeriodicInvoice periodicInvoice(String customerNr, String customerName) {
        SSPeriodicInvoice periodicInvoice = new SSPeriodicInvoice();
        periodicInvoice.setLocalDate(LocalDate.of(2025, 7, 1));
        periodicInvoice.setCount(3);
        periodicInvoice.setPeriod(1);
        periodicInvoice.setDescription("Periodic template in repository test");
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
        template.setText("Periodic invoice text in repository test");
        template.setYourOrderNumber("ORDER-REPO-PERIODIC-001");
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
        se.swedsoft.bookkeeping.data.SSNewCompany company = new se.swedsoft.bookkeeping.data.SSNewCompany();
        company.setName(name);
        se.swedsoft.bookkeeping.data.system.SSCompanyYearContext.addCompany(company);
        return company.getId();
    }
}

