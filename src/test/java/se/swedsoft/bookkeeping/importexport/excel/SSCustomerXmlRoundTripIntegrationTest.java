package se.swedsoft.bookkeeping.importexport.excel;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import se.swedsoft.bookkeeping.data.SSCustomer;
import se.swedsoft.bookkeeping.data.system.SSMasterdataContext;
import se.swedsoft.bookkeeping.importexport.util.SSTestFileHelper;
import se.swedsoft.bookkeeping.testsupport.data.SSTestDataFactory;
import se.swedsoft.bookkeeping.testsupport.system.SSV2DatabaseFixture;

import java.sql.Connection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * Integration test for customer XML export/import roundtrip.
 */
@Tag("integration")
class SSCustomerXmlRoundTripIntegrationTest {

    private static final String JDBC_URL = "jdbc:hsqldb:mem:fribok_test_v2_customer_xml_roundtrip";
    private static final String CUSTOMER_NUMBER = "C-XML-IT-001";
    private static Connection connection;

    @BeforeAll
    static void setupV2Database() throws Exception {
        connection = SSV2DatabaseFixture.openDatabase(JDBC_URL);
    }

    @AfterAll
    static void teardownV2Database() throws Exception {
        SSV2DatabaseFixture.closeDatabase(connection);
    }

    @BeforeEach
    void resetState() {
        SSV2DatabaseFixture.clearState();
        deleteCustomerIfPresent();
    }

    @AfterEach
    void cleanupState() {
        deleteCustomerIfPresent();
        SSV2DatabaseFixture.clearState();
    }

    @Test
    void xmlExportAndImportRoundTripsCustomerCoreFields() throws Exception {
        SSCustomer iCustomer = SSTestDataFactory.xmlRoundTripCustomer(CUSTOMER_NUMBER);

        SSTestFileHelper.withTempFile("fribok-customer-xml-roundtrip", ".xml", iFile -> {
            new SSCustomerExporter(iFile.toFile(), List.of(iCustomer)).doXMLExport();
            new SSCustomerImporter(iFile.toFile()).doImport();
            SSV2DatabaseFixture.clearState();

            SSCustomer iImportedCustomer = SSMasterdataContext.getCustomer(CUSTOMER_NUMBER)
                    .orElseThrow(() -> new AssertionError("Imported customer was not found"));

            assertThat(iImportedCustomer.getName()).isEqualTo("XML Kund AB");
            assertThat(iImportedCustomer.getEMail()).isEqualTo("xml.kund@fribok.se");
            assertThat(iImportedCustomer.getPhone1()).isEqualTo("08-123456");
            assertThat(iImportedCustomer.getInvoiceAddress().getName()).isEqualTo("Faktura XML AB");
            assertThat(iImportedCustomer.getInvoiceAddress().getAddress1()).isEqualTo("Fakturagatan 1");
            assertThat(iImportedCustomer.getInvoiceAddress().getCity()).isEqualTo("Stockholm");
            assertThat(iImportedCustomer.getDeliveryAddress().getName()).isEqualTo("Leverans XML AB");
            assertThat(iImportedCustomer.getDeliveryAddress().getAddress1()).isEqualTo("Leveransgatan 2");
            assertThat(iImportedCustomer.getDeliveryAddress().getCity()).isEqualTo("Uppsala");
        });
    }

    private void deleteCustomerIfPresent() {
        SSMasterdataContext.getCustomer(CUSTOMER_NUMBER).ifPresent(SSMasterdataContext::deleteCustomer);
        SSV2DatabaseFixture.clearState();
    }
}
