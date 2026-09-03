package se.swedsoft.bookkeeping.integration.system;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import se.swedsoft.bookkeeping.data.SSNewCompany;
import se.swedsoft.bookkeeping.data.SSVoucherTemplate;
import se.swedsoft.bookkeeping.data.system.SSAccountingContext;
import se.swedsoft.bookkeeping.data.system.SSCompanyYearContext;
import se.swedsoft.bookkeeping.data.system.SSEventTriggerSyncContext;
import se.swedsoft.bookkeeping.data.system.SSSystemConfigContext;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration slice for voucher-template CRUD against schema V2.
 */
@Tag("integration")
class SSVoucherTemplateV2IntegrationTest {

    private static final String JDBC_URL = "jdbc:hsqldb:mem:fribok_test_s-s-v-o-u-c-h-e-r-t-e-m-p-l-a-t-e-v2-i-n-t-e-g-r-a-t-i-o-n-t-e-s-t";

    private static Connection connection;

    @BeforeAll
    static void setupV2Schema() throws Exception {
        System.setProperty("fribok.schema.version", "v2");

        Class.forName("org.hsqldb.jdbcDriver");
        connection = DriverManager.getConnection(JDBC_URL, "sa", "");

        SSSystemConfigContext.startupLocal(connection);

        Integer companyId = createCompany("V2 Voucher Template Test Company AB");
        SSNewCompany company = new SSNewCompany();
        company.setId(companyId);
        company.setName("V2 Voucher Template Test Company AB");
        SSCompanyYearContext.setCurrentCompany(company);
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
        SSEventTriggerSyncContext.clearCachedLists();
    }

    @AfterEach
    void clearStateAfterTest() {
        SSEventTriggerSyncContext.clearCachedLists();
    }

    @Test
    void addAndFetchVoucherTemplateWithRowsInSchemaV2() {
        SSVoucherTemplate template = voucherTemplate("VT-REPO-001", LocalDateTime.of(2025, 5, 6, 10, 15));
        template.getRows().add(templateRow(1910, true));
        template.getRows().add(templateRow(3010, false));

        SSAccountingContext.addVoucherTemplate(template);

        List<SSVoucherTemplate> all = SSAccountingContext.getVoucherTemplates();
        assertThat(all).extracting(SSVoucherTemplate::getDescription).contains("VT-REPO-001");

        SSVoucherTemplate fetched = all.stream()
                .filter(current -> "VT-REPO-001".equals(current.getDescription()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Expected voucher template not found"));

        assertThat(fetched.getLocalDateTime()).isEqualTo(LocalDateTime.of(2025, 5, 6, 10, 15));
        assertThat(fetched.getRows()).hasSize(2);
        assertThat(fetched.getRows().get(0).getAccountNr()).isEqualTo(1910);
        assertThat(fetched.getRows().get(0).getDebet()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(fetched.getRows().get(1).getAccountNr()).isEqualTo(3010);
        assertThat(fetched.getRows().get(1).getCredit()).isEqualByComparingTo(BigDecimal.ZERO);

        SSAccountingContext.deleteVoucherTemplate(fetched);
    }

    @Test
    void subsetAndDeleteVoucherTemplateInSchemaV2() {
        SSVoucherTemplate template = voucherTemplate("VT-REPO-002", LocalDateTime.of(2025, 5, 6, 11, 0));
        template.getRows().add(templateRow(2610, true));
        SSAccountingContext.addVoucherTemplate(template);

        SSVoucherTemplate probe = new SSVoucherTemplate();
        probe.setDescription("VT-REPO-002");

        List<SSVoucherTemplate> subset = SSAccountingContext.getVoucherTemplates(Collections.singletonList(probe));
        assertThat(subset).hasSize(1);
        assertThat(subset.get(0).getDescription()).isEqualTo("VT-REPO-002");
        assertThat(subset.get(0).getRows()).hasSize(1);

        SSAccountingContext.deleteVoucherTemplate(subset.get(0));
        List<SSVoucherTemplate> afterDelete = SSAccountingContext.getVoucherTemplates();
        assertThat(afterDelete).extracting(SSVoucherTemplate::getDescription).doesNotContain("VT-REPO-002");
    }

    private static SSVoucherTemplate voucherTemplate(String description, LocalDateTime dateTime) {
        SSVoucherTemplate template = new SSVoucherTemplate();
        template.setDescription(description);
        template.setLocalDateTime(dateTime);
        return template;
    }

    private static SSVoucherTemplate.SSVoucherTemplateRow templateRow(int accountNr, boolean debet) {
        SSVoucherTemplate.SSVoucherTemplateRow row = new SSVoucherTemplate.SSVoucherTemplateRow();
        row.setAccountNr(accountNr);
        if (debet) {
            row.setDebet(BigDecimal.ZERO);
        } else {
            row.setCredit(BigDecimal.ZERO);
        }
        return row;
    }

    private static Integer createCompany(String name) throws Exception {
        se.swedsoft.bookkeeping.data.SSNewCompany company = new se.swedsoft.bookkeeping.data.SSNewCompany();
        company.setName(name);
        se.swedsoft.bookkeeping.data.system.SSCompanyYearContext.addCompany(company);
        return company.getId();
    }
}


