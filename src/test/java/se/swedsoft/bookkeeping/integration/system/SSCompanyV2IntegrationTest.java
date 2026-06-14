package se.swedsoft.bookkeeping.integration.system;

import se.swedsoft.bookkeeping.data.system.SSDB;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import se.swedsoft.bookkeeping.data.SSAddress;
import se.swedsoft.bookkeeping.data.SSNewCompany;
import se.swedsoft.bookkeeping.data.SSStandardText;
import se.swedsoft.bookkeeping.data.common.SSDefaultAccount;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("integration")
class SSCompanyV2IntegrationTest {

    private static final String JDBC_URL = "jdbc:hsqldb:mem:fribok_test_s-s-c-o-m-p-a-n-y-v2-i-n-t-e-g-r-a-t-i-o-n-t-e-s-t";

    private static Connection connection;

    @BeforeAll
    static void setupV2Schema() throws Exception {
        System.setProperty("fribok.schema.version", "v2");

        Class.forName("org.hsqldb.jdbcDriver");
        connection = DriverManager.getConnection(JDBC_URL, "sa", "");

        se.swedsoft.bookkeeping.data.system.SSSystemConfigContext.startupLocal(connection);
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
    }

    @AfterEach
    void clearStateAfterTest() {
        se.swedsoft.bookkeeping.data.system.SSEventTriggerSyncContext.clearCachedLists();
    }

    @Test
    void addAndFetchCompanyWithFullV2ColumnsAndChildTables() {
        SSNewCompany company = new SSNewCompany();
        company.setName("V2 Full Company AB");
        company.setPhone("08-100100");
        company.setPhone2("08-100200");
        company.setTelefax("08-100300");
        company.setResidence("Stockholm");
        company.setHomepage("https://example.se");
        company.setSMTP("smtp.example.se");
        company.setEMail("info@example.se");
        company.setContactPerson("Anna Admin");
        company.setTaxRegistered(true);
        company.setCorporateID("556677-8899");
        company.setLogotype("C:/logos/v2.png");
        company.setBank("Nordbanken");
        company.setVATNumber("SE556677889901");
        company.setBankGiroNumber("123-4567");
        company.setPlusGiroNumber("12 34 56-7");
        company.setIBAN("SE3550000000054910000003");
        company.setBIC("ESSESESS");
        company.setDelayInterest(new BigDecimal("8.50"));
        company.setReminderfee(new BigDecimal("60.00"));
        company.setEstimatedDelivery("5 dagar");
        company.setTaxrate1(new BigDecimal("25.00"));
        company.setTaxrate2(new BigDecimal("12.00"));
        company.setTaxrate3(new BigDecimal("6.00"));
        company.setWeightUnit("kg");
        company.setVolumeUnit("m3");

        SSAddress postal = new SSAddress("V2 Full Company AB", "Main Street 1", "Floor 2", "11122", "Stockholm", "SE");
        SSAddress delivery = new SSAddress("V2 Full Company AB", "Warehouse 9", "Gate B", "33344", "Uppsala", "SE");
        company.setAddress(postal);
        company.setDeliveryAddress(delivery);

        Map<SSStandardText, String> standardTexts = new HashMap<>();
        standardTexts.put(SSStandardText.Email, "Hej frÃ¥n V2-test");
        standardTexts.put(SSStandardText.Tender, "Offerttext V2");
        company.setStandardTexts(standardTexts);

        Map<SSDefaultAccount, Integer> defaultAccounts = new HashMap<>();
        defaultAccounts.put(SSDefaultAccount.Sales, 3051);
        defaultAccounts.put(SSDefaultAccount.InPayment, 1930);
        company.setDefaultAccounts(defaultAccounts);

        se.swedsoft.bookkeeping.data.system.SSCompanyYearContext.addCompany(company);

        assertThat(company.getId()).isNotNull();

        Optional<SSNewCompany> fetchedOpt = se.swedsoft.bookkeeping.data.system.SSCompanyYearContext.getCompany(company);
        assertThat(fetchedOpt).isPresent();

        SSNewCompany fetched = fetchedOpt.get();
        assertThat(fetched.getName()).isEqualTo("V2 Full Company AB");
        assertThat(fetched.getPhone()).isEqualTo("08-100100");
        assertThat(fetched.getPhone2()).isEqualTo("08-100200");
        assertThat(fetched.getTelefax()).isEqualTo("08-100300");
        assertThat(fetched.getResidence()).isEqualTo("Stockholm");
        assertThat(fetched.getHomepage()).isEqualTo("https://example.se");
        assertThat(fetched.getSMTP()).isEqualTo("smtp.example.se");
        assertThat(fetched.getEMail()).isEqualTo("info@example.se");
        assertThat(fetched.getContactPerson()).isEqualTo("Anna Admin");
        assertThat(fetched.getTaxRegistered()).isTrue();
        assertThat(fetched.getCorporateID()).isEqualTo("556677-8899");
        assertThat(fetched.getLogotype()).isEqualTo("C:/logos/v2.png");
        assertThat(fetched.getBank()).isEqualTo("Nordbanken");
        assertThat(fetched.getVATNumber()).isEqualTo("SE556677889901");
        assertThat(fetched.getBankGiroNumber()).isEqualTo("123-4567");
        assertThat(fetched.getPlusGiroNumber()).isEqualTo("12 34 56-7");
        assertThat(fetched.getIBAN()).isEqualTo("SE3550000000054910000003");
        assertThat(fetched.getBIC()).isEqualTo("ESSESESS");
        assertThat(fetched.getDelayInterest()).isEqualByComparingTo("8.50");
        assertThat(fetched.getReminderfee()).isEqualByComparingTo("60.00");
        assertThat(fetched.getEstimatedDelivery()).isEqualTo("5 dagar");
        assertThat(fetched.getTaxRate1()).isEqualByComparingTo("25.00");
        assertThat(fetched.getTaxRate2()).isEqualByComparingTo("12.00");
        assertThat(fetched.getTaxRate3()).isEqualByComparingTo("6.00");
        assertThat(fetched.getWeightUnit()).isEqualTo("kg");
        assertThat(fetched.getVolumeUnit()).isEqualTo("m3");

        assertThat(fetched.getAddress().getAddress1()).isEqualTo("Main Street 1");
        assertThat(fetched.getAddress().getAddress2()).isEqualTo("Floor 2");
        assertThat(fetched.getAddress().getZipCode()).isEqualTo("11122");
        assertThat(fetched.getAddress().getCity()).isEqualTo("Stockholm");
        assertThat(fetched.getAddress().getCountry()).isEqualTo("SE");

        assertThat(fetched.getDeliveryAddress().getAddress1()).isEqualTo("Warehouse 9");
        assertThat(fetched.getDeliveryAddress().getAddress2()).isEqualTo("Gate B");
        assertThat(fetched.getDeliveryAddress().getZipCode()).isEqualTo("33344");
        assertThat(fetched.getDeliveryAddress().getCity()).isEqualTo("Uppsala");
        assertThat(fetched.getDeliveryAddress().getCountry()).isEqualTo("SE");

        assertThat(fetched.getStandardTexts()).containsEntry(SSStandardText.Email, "Hej frÃ¥n V2-test");
        assertThat(fetched.getStandardTexts()).containsEntry(SSStandardText.Tender, "Offerttext V2");
        assertThat(fetched.getDefaultAccounts()).containsEntry(SSDefaultAccount.Sales, 3051);
        assertThat(fetched.getDefaultAccounts()).containsEntry(SSDefaultAccount.InPayment, 1930);

        se.swedsoft.bookkeeping.data.system.SSCompanyYearContext.deleteCompany(fetched);
    }

    @Test
    void updateCompanyReplacesChildRowsInV2() {
        SSNewCompany company = new SSNewCompany();
        company.setName("V2 Update Company AB");

        Map<SSStandardText, String> initialTexts = new HashMap<>();
        initialTexts.put(SSStandardText.Email, "FÃ¶rsta text");
        company.setStandardTexts(initialTexts);

        Map<SSDefaultAccount, Integer> initialAccounts = new HashMap<>();
        initialAccounts.put(SSDefaultAccount.Sales, 3051);
        company.setDefaultAccounts(initialAccounts);

        se.swedsoft.bookkeeping.data.system.SSCompanyYearContext.addCompany(company);

        Optional<SSNewCompany> fetchedOpt = se.swedsoft.bookkeeping.data.system.SSCompanyYearContext.getCompany(company);
        assertThat(fetchedOpt).isPresent();

        SSNewCompany fetched = fetchedOpt.get();
        fetched.setPhone("010-202020");

        Map<SSStandardText, String> updatedTexts = new HashMap<>();
        updatedTexts.put(SSStandardText.Reminder, "Ny pÃ¥minnelsetext");
        fetched.setStandardTexts(updatedTexts);

        Map<SSDefaultAccount, Integer> updatedAccounts = new HashMap<>();
        updatedAccounts.put(SSDefaultAccount.InPayment, 1910);
        fetched.setDefaultAccounts(updatedAccounts);

        se.swedsoft.bookkeeping.data.system.SSCompanyYearContext.updateCompany(fetched);
        se.swedsoft.bookkeeping.data.system.SSEventTriggerSyncContext.clearCachedLists();

        Optional<SSNewCompany> updatedOpt = se.swedsoft.bookkeeping.data.system.SSCompanyYearContext.getCompany(fetched);
        assertThat(updatedOpt).isPresent();

        SSNewCompany updated = updatedOpt.get();
        assertThat(updated.getPhone()).isEqualTo("010-202020");
        assertThat(updated.getStandardTexts()).containsEntry(SSStandardText.Reminder, "Ny pÃ¥minnelsetext");
        assertThat(updated.getStandardTexts()).doesNotContainKey(SSStandardText.Email);
        assertThat(updated.getDefaultAccounts()).containsEntry(SSDefaultAccount.InPayment, 1910);
        assertThat(updated.getDefaultAccounts()).doesNotContainKey(SSDefaultAccount.Sales);

        se.swedsoft.bookkeeping.data.system.SSCompanyYearContext.deleteCompany(updated);
    }

    @Test
    void setCurrentCompanyWithThinObjectLoadsFullCompanyDataInV2() {
        SSNewCompany company = new SSNewCompany();
        company.setName("V2 Thin Resolve Company AB");
        company.setCorporateID("559900-1122");
        company.setPhone("070-1234567");

        Map<SSStandardText, String> standardTexts = new HashMap<>();
        standardTexts.put(SSStandardText.Email, "Thin resolve text");
        company.setStandardTexts(standardTexts);

        Map<SSDefaultAccount, Integer> defaultAccounts = new HashMap<>();
        defaultAccounts.put(SSDefaultAccount.Sales, 3051);
        company.setDefaultAccounts(defaultAccounts);

        se.swedsoft.bookkeeping.data.system.SSCompanyYearContext.addCompany(company);

        SSNewCompany thinCompany = new SSNewCompany();
        thinCompany.setId(company.getId());
        thinCompany.setName(company.getName());

        SSDB.getInstance().setCurrentCompany(thinCompany);

        SSNewCompany resolved = SSDB.getInstance().getCurrentCompany();
        assertThat(resolved).isNotNull();
        assertThat(resolved.getId()).isEqualTo(company.getId());
        assertThat(resolved.getCorporateID()).isEqualTo("559900-1122");
        assertThat(resolved.getPhone()).isEqualTo("070-1234567");
        assertThat(resolved.getStandardTexts()).containsEntry(SSStandardText.Email, "Thin resolve text");
        assertThat(resolved.getDefaultAccounts()).containsEntry(SSDefaultAccount.Sales, 3051);

        se.swedsoft.bookkeeping.data.system.SSCompanyYearContext.deleteCompany(resolved);
    }

    @Test
    void getCompaniesReturnsFullCompanyDataInV2() {
        SSNewCompany company = new SSNewCompany();
        company.setName("V2 Company List Full Data AB");
        company.setCorporateID("556600-7788");
        company.setPhone("08-777888");

        Map<SSStandardText, String> standardTexts = new HashMap<>();
        standardTexts.put(SSStandardText.Email, "List full text");
        company.setStandardTexts(standardTexts);

        Map<SSDefaultAccount, Integer> defaultAccounts = new HashMap<>();
        defaultAccounts.put(SSDefaultAccount.InPayment, 1930);
        company.setDefaultAccounts(defaultAccounts);

        se.swedsoft.bookkeeping.data.system.SSCompanyYearContext.addCompany(company);
        se.swedsoft.bookkeeping.data.system.SSEventTriggerSyncContext.clearCachedLists();

        List<SSNewCompany> companies = se.swedsoft.bookkeeping.data.system.SSCompanyYearContext.getCompanies();
        Optional<SSNewCompany> fetchedOpt = companies.stream()
                .filter(c -> company.getId().equals(c.getId()))
                .findFirst();

        assertThat(fetchedOpt).isPresent();

        SSNewCompany fetched = fetchedOpt.get();
        assertThat(fetched.getName()).isEqualTo("V2 Company List Full Data AB");
        assertThat(fetched.getCorporateID()).isEqualTo("556600-7788");
        assertThat(fetched.getPhone()).isEqualTo("08-777888");
        assertThat(fetched.getStandardTexts()).containsEntry(SSStandardText.Email, "List full text");
        assertThat(fetched.getDefaultAccounts()).containsEntry(SSDefaultAccount.InPayment, 1930);

        se.swedsoft.bookkeeping.data.system.SSCompanyYearContext.deleteCompany(fetched);
    }
}


