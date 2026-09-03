package se.swedsoft.bookkeeping.persistence;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import se.swedsoft.bookkeeping.data.SSNewCompany;
import se.swedsoft.bookkeeping.data.SSVoucherTemplate;
import se.swedsoft.bookkeeping.data.system.SSDB;
import se.swedsoft.bookkeeping.persistence.v2.V2VoucherTemplateRepository;

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
 * Integration tests for voucher-template repository wiring in schema V2.
 */
@Tag("integration")
class SSVoucherTemplateV2RepositoryTest {

    private static final String JDBC_URL = "jdbc:hsqldb:mem:fribok_test_v2_vouchertemplate_repo";

    private static Connection connection;

    @BeforeAll
    static void setupV2Schema() throws Exception {
        System.setProperty("fribok.schema.version", "v2");

        Class.forName("org.hsqldb.jdbcDriver");
        connection = DriverManager.getConnection(JDBC_URL, "sa", "");

        se.swedsoft.bookkeeping.data.system.SSSystemConfigContext.startupLocal(connection);

        Integer companyId = createCompany("V2 Voucher Template Repo Test AB");
        SSNewCompany company = new SSNewCompany();
        company.setId(companyId);
        company.setName("V2 Voucher Template Repo Test AB");
        SSDB.getInstance().setCurrentCompany(company);

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
    }

    @Test
    void repositoriesInitUsesV2VoucherTemplateRepository() {
        assertThat(Repositories.isSchemaV2()).isTrue();
        assertThat(Repositories.voucherTemplates()).isInstanceOf(V2VoucherTemplateRepository.class);
    }

    @Test
    void addAndFetchVoucherTemplateViaRepository() {
        SSVoucherTemplate template = voucherTemplate("VT-REPO-R-001", LocalDateTime.of(2025, 5, 7, 9, 30));
        template.getRows().add(templateRow(1910, true));
        template.getRows().add(templateRow(3010, false));

        Repositories.voucherTemplates().add(template);

        List<SSVoucherTemplate> all = Repositories.voucherTemplates().findAll();
        assertThat(all).extracting(SSVoucherTemplate::getDescription).contains("VT-REPO-R-001");

        SSVoucherTemplate fetched = all.stream()
                .filter(current -> "VT-REPO-R-001".equals(current.getDescription()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Expected voucher template not found"));

        assertThat(fetched.getRows()).hasSize(2);
        assertThat(fetched.getRows().get(0).getAccountNr()).isEqualTo(1910);
        assertThat(fetched.getRows().get(1).getAccountNr()).isEqualTo(3010);

        Repositories.voucherTemplates().delete(fetched);
    }

    @Test
    void subsetAndDeleteVoucherTemplateViaRepository() {
        SSVoucherTemplate template = voucherTemplate("VT-REPO-R-002", LocalDateTime.of(2025, 5, 7, 10, 0));
        template.getRows().add(templateRow(2610, true));
        Repositories.voucherTemplates().add(template);

        SSVoucherTemplate probe = new SSVoucherTemplate();
        probe.setDescription("VT-REPO-R-002");

        List<SSVoucherTemplate> subset = Repositories.voucherTemplates().findAll(Collections.singletonList(probe));
        assertThat(subset).hasSize(1);
        assertThat(subset.get(0).getDescription()).isEqualTo("VT-REPO-R-002");

        Repositories.voucherTemplates().delete(subset.get(0));
        List<SSVoucherTemplate> afterDelete = Repositories.voucherTemplates().findAll();
        assertThat(afterDelete).extracting(SSVoucherTemplate::getDescription).doesNotContain("VT-REPO-R-002");
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


