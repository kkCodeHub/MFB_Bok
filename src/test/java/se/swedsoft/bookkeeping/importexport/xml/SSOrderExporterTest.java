package se.swedsoft.bookkeeping.importexport.xml;


import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import se.swedsoft.bookkeeping.data.SSOrder;
import se.swedsoft.bookkeeping.importexport.util.SSTestFileHelper;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * Tests for {@link SSOrderExporter} XML export.
 */
class SSOrderExporterTest {

    @Test
    void exportWritesBasicOrderFieldsToXml() throws Exception {
        SSOrder iOrder = new SSOrder();
        iOrder.setNumber(4711);
        iOrder.setLocalDate(LocalDate.of(2026, 5, 20));
        iOrder.setCustomerNr("K-XML-ORD-001");
        iOrder.setCustomerName("Orderkund XML AB");
        iOrder.setText("Testorder XML");
        iOrder.setOurContactPerson("Anna");
        iOrder.setYourContactPerson("Bertil");

        SSTestFileHelper.withTempFile("fribok-order-export", ".xml", iFile -> {
            new SSOrderExporter(iFile.toFile(), List.of(iOrder)).doExport();

            Document iDocument = SSTestFileHelper.parseXml(iFile);

            assertThat(iDocument.getDocumentElement().getNodeName()).isEqualTo("Orders");
            assertThat(iDocument.getElementsByTagName("Order").getLength()).isEqualTo(1);
            assertThat(iDocument.getElementsByTagName("SellerOrderNo").item(0).getTextContent())
                    .isEqualTo("4711");
            assertThat(iDocument.getElementsByTagName("OrderDate").item(0).getTextContent())
                    .isEqualTo("2026-05-20");
            assertThat(iDocument.getElementsByTagName("CustomerNumber").item(0).getTextContent())
                    .isEqualTo("K-XML-ORD-001");
            assertThat(iDocument.getElementsByTagName("CustomerName").item(0).getTextContent())
                    .isEqualTo("Orderkund XML AB");
            assertThat(iDocument.getElementsByTagName("Text").item(0).getTextContent())
                    .isEqualTo("Testorder XML");
        });
    }

    @Test
    void exportPreservesSpecialCharactersAndEmptyDate() throws Exception {
        SSOrder iOrder = new SSOrder();
        iOrder.setNumber(4712);
        iOrder.setLocalDate(null);
        iOrder.setCustomerNr("K-XML-ORD-002");
        iOrder.setCustomerName("Kund & Söner <AB>");
        iOrder.setText("Rad 1 & Rad 2 <test> \"ok\"");

        SSTestFileHelper.withTempFile("fribok-order-export-special", ".xml", iFile -> {
            new SSOrderExporter(iFile.toFile(), List.of(iOrder)).doExport();

            Document iDocument = SSTestFileHelper.parseXml(iFile);

            assertThat(iDocument.getElementsByTagName("OrderDate").item(0).getTextContent())
                    .isEqualTo("");
            assertThat(iDocument.getElementsByTagName("CustomerName").item(0).getTextContent())
                    .isEqualTo("Kund & Söner <AB>");
            assertThat(iDocument.getElementsByTagName("Text").item(0).getTextContent())
                    .isEqualTo("Rad 1 & Rad 2 <test> \"ok\"");
        });
    }
}
