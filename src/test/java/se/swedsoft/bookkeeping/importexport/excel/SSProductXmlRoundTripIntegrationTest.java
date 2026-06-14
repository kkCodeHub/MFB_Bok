package se.swedsoft.bookkeeping.importexport.excel;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import se.swedsoft.bookkeeping.data.SSProduct;
import se.swedsoft.bookkeeping.data.system.SSProductContext;
import se.swedsoft.bookkeeping.importexport.util.SSTestFileHelper;
import se.swedsoft.bookkeeping.testsupport.data.SSTestDataFactory;
import se.swedsoft.bookkeeping.testsupport.system.SSV2DatabaseFixture;

import java.sql.Connection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * Integration test for product XML export/import roundtrip.
 */
@Tag("integration")
class SSProductXmlRoundTripIntegrationTest {

    private static final String JDBC_URL = "jdbc:hsqldb:mem:fribok_test_v2_product_xml_roundtrip";
    private static final String PRODUCT_NUMBER = "P-XML-IT-001";
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
        deleteProductIfPresent();
    }

    @AfterEach
    void cleanupState() {
        deleteProductIfPresent();
        SSV2DatabaseFixture.clearState();
    }

    @Test
    void xmlExportAndImportRoundTripsProductCoreFields() throws Exception {
        SSProduct iProduct = SSTestDataFactory.xmlRoundTripProduct(PRODUCT_NUMBER);

        SSTestFileHelper.withTempFile("fribok-product-xml-roundtrip", ".xml", iFile -> {
            new SSProductExporter(iFile.toFile(), List.of(iProduct)).doXMLExport();
            new SSProductImporter(iFile.toFile()).doXMLImport();
            SSV2DatabaseFixture.clearState();

            SSProduct iImportedProduct = SSProductContext.getProduct(PRODUCT_NUMBER);
            if (iImportedProduct == null) {
                throw new AssertionError("Imported product was not found");
            }

            assertThat(iImportedProduct.getDescription()).isEqualTo("XML Produkt");
            assertThat(iImportedProduct.getSellingPrice()).isEqualByComparingTo("123.45");
            assertThat(iImportedProduct.getPurchasePrice()).isEqualByComparingTo("67.89");
            assertThat(iImportedProduct.getWarehouseLocation()).isEqualTo("A-01");
            assertThat(iImportedProduct.getOrderpoint()).isEqualTo(7);
            assertThat(iImportedProduct.getOrdercount()).isEqualTo(14);
        });
    }

    private void deleteProductIfPresent() {
        SSProduct iProduct = SSProductContext.getProduct(PRODUCT_NUMBER);
        if (iProduct != null) {
            SSProductContext.deleteProduct(iProduct);
        }
        SSV2DatabaseFixture.clearState();
    }
}
