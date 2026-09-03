package se.swedsoft.bookkeeping.importexport.xml;


import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import se.swedsoft.bookkeeping.data.SSOrder;
import se.swedsoft.bookkeeping.data.base.SSSaleRow;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * User: Andreas Lago
 * Date: 2007-mar-29
 * Time: 15:16:52
 * $Id$
 */
public class SSOrderExporter {    private static final Logger LOG = LoggerFactory.getLogger(SSOrderExporter.class);


    private final List<SSOrder> iItems;

    private final File iFile;

    public SSOrderExporter(File pFile, List<SSOrder> pOrders) {

        iItems = pOrders;
        iFile = pFile;
    }

    public void doExport() {

        Document iXmlDoc = createDocument();
        Element iRoot = iXmlDoc.createElement("Orders");

        for (SSOrder iOrder : iItems) {
            Element iElement = iXmlDoc.createElementNS(null, "Order");

            Element iSubElement = iXmlDoc.createElementNS(null, "SellerOrderNo");

            iElement.appendChild(iSubElement);
            Node iNode = createTextNode(iXmlDoc,
                    iOrder.getNumber() == null ? "" : iOrder.getNumber().toString());

            iSubElement.appendChild(iNode);

            iSubElement = iXmlDoc.createElementNS(null, "OrderDate");
            iElement.appendChild(iSubElement);
            DateTimeFormatter iFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd");

            LocalDate iOrderDate = iOrder.getLocalDate();
            iNode = createTextNode(iXmlDoc, iOrderDate == null ? "" : iOrderDate.format(iFormat));
            iSubElement.appendChild(iNode);

            iSubElement = iXmlDoc.createElementNS(null, "CustomerNumber");
            iElement.appendChild(iSubElement);
            iNode = createTextNode(iXmlDoc, iOrder.getCustomerNr());
            iSubElement.appendChild(iNode);

            iSubElement = iXmlDoc.createElementNS(null, "CustomerName");
            iElement.appendChild(iSubElement);
            iNode = createTextNode(iXmlDoc, iOrder.getCustomerName());
            iSubElement.appendChild(iNode);

            iSubElement = iXmlDoc.createElementNS(null, "OurContactPerson");
            iElement.appendChild(iSubElement);
            iNode = createTextNode(iXmlDoc, iOrder.getOurContactPerson());
            iSubElement.appendChild(iNode);

            iSubElement = iXmlDoc.createElementNS(null, "YourContactPerson");
            iElement.appendChild(iSubElement);
            iNode = createTextNode(iXmlDoc, iOrder.getYourContactPerson());
            iSubElement.appendChild(iNode);

            iSubElement = iXmlDoc.createElementNS(null, "DelayInterest");
            iElement.appendChild(iSubElement);
            iNode = createTextNode(iXmlDoc,
                    iOrder.getDelayInterest() == null ? "" : iOrder.getDelayInterest().toString());
            iSubElement.appendChild(iNode);

            iSubElement = iXmlDoc.createElementNS(null, "CurrencyCode");
            iElement.appendChild(iSubElement);
            iNode = createTextNode(iXmlDoc,
                    iOrder.getCurrency() == null ? "" : iOrder.getCurrency().getName());
            iSubElement.appendChild(iNode);

            iSubElement = iXmlDoc.createElementNS(null, "PaymentTerms");
            iElement.appendChild(iSubElement);
            iNode = createTextNode(iXmlDoc,
                    iOrder.getPaymentTerm() == null ? "" : iOrder.getPaymentTerm().getName());
            iSubElement.appendChild(iNode);

            iSubElement = iXmlDoc.createElementNS(null, "DeliveryTerms");
            iElement.appendChild(iSubElement);
            iNode = createTextNode(iXmlDoc,
                    iOrder.getDeliveryTerm() == null ? "" : iOrder.getDeliveryTerm().getName());
            iSubElement.appendChild(iNode);

            iSubElement = iXmlDoc.createElementNS(null, "DeliveryMethod");
            iElement.appendChild(iSubElement);
            iNode = createTextNode(iXmlDoc,
                    iOrder.getDeliveryWay() == null ? "" : iOrder.getDeliveryWay().getName());
            iSubElement.appendChild(iNode);

            iSubElement = iXmlDoc.createElementNS(null, "TaxFree");
            iElement.appendChild(iSubElement);
            iNode = createTextNode(iXmlDoc, Boolean.toString(iOrder.getTaxFree()));
            iSubElement.appendChild(iNode);

            iSubElement = iXmlDoc.createElementNS(null, "Text");
            iElement.appendChild(iSubElement);
            iNode = createTextNode(iXmlDoc,
                    iOrder.getText() == null ? "" : iOrder.getText());
            iSubElement.appendChild(iNode);

            iSubElement = iXmlDoc.createElementNS(null, "TaxRate1");
            iElement.appendChild(iSubElement);
            iNode = createTextNode(iXmlDoc,
                    iOrder.getTaxRate1() == null ? "" : iOrder.getTaxRate1().toString());
            iSubElement.appendChild(iNode);

            iSubElement = iXmlDoc.createElementNS(null, "TaxRate2");
            iElement.appendChild(iSubElement);
            iNode = createTextNode(iXmlDoc,
                    iOrder.getTaxRate2() == null ? "" : iOrder.getTaxRate2().toString());
            iSubElement.appendChild(iNode);

            iSubElement = iXmlDoc.createElementNS(null, "TaxRate3");
            iElement.appendChild(iSubElement);
            iNode = createTextNode(iXmlDoc,
                    iOrder.getTaxRate3() == null ? "" : iOrder.getTaxRate3().toString());
            iSubElement.appendChild(iNode);

            iSubElement = iXmlDoc.createElementNS(null, "EuSaleCommodity");
            iElement.appendChild(iSubElement);
            iNode = createTextNode(iXmlDoc, Boolean.toString(iOrder.getEuSaleCommodity()));
            iSubElement.appendChild(iNode);

            iSubElement = iXmlDoc.createElementNS(null, "EuSaleThirdPartCommodity");
            iElement.appendChild(iSubElement);
            iNode = createTextNode(iXmlDoc,
                    Boolean.toString(iOrder.getEuSaleThirdPartCommodity()));
            iSubElement.appendChild(iNode);

            iSubElement = iXmlDoc.createElementNS(null, "VATRegNo");
            iElement.appendChild(iSubElement);
            iNode = createTextNode(iXmlDoc,
                    iOrder.getCustomer() == null
                            ? ""
                            : iOrder.getCustomer().getVATNumber());
            iSubElement.appendChild(iNode);

            iSubElement = iXmlDoc.createElementNS(null, "Email");
            iElement.appendChild(iSubElement);
            iNode = createTextNode(iXmlDoc,
                    iOrder.getCustomer() == null ? "" : iOrder.getCustomer().getEMail());
            iSubElement.appendChild(iNode);

            iSubElement = iXmlDoc.createElementNS(null, "CompanyNo");
            iElement.appendChild(iSubElement);
            iNode = createTextNode(iXmlDoc,
                    iOrder.getCustomer() == null
                            ? ""
                            : iOrder.getCustomer().getRegistrationNumber());
            iSubElement.appendChild(iNode);

            iSubElement = iXmlDoc.createElementNS(null, "Telefax");
            iElement.appendChild(iSubElement);
            iNode = createTextNode(iXmlDoc,
                    iOrder.getCustomer() == null ? "" : iOrder.getCustomer().getTelefax());
            iSubElement.appendChild(iNode);

            iSubElement = iXmlDoc.createElementNS(null, "Telephone");
            iElement.appendChild(iSubElement);
            iNode = createTextNode(iXmlDoc,
                    iOrder.getCustomer() == null ? "" : iOrder.getCustomer().getPhone1());
            iSubElement.appendChild(iNode);

            iSubElement = iXmlDoc.createElementNS(null, "Telephone2");
            iElement.appendChild(iSubElement);
            iNode = createTextNode(iXmlDoc,
                    iOrder.getCustomer() == null ? "" : iOrder.getCustomer().getPhone2());
            iSubElement.appendChild(iNode);

            iSubElement = iXmlDoc.createElementNS(null, "InvoiceName");
            iElement.appendChild(iSubElement);
            iNode = createTextNode(iXmlDoc,
                    iOrder.getInvoiceAddress() == null
                            ? ""
                            : iOrder.getInvoiceAddress().getName());
            iSubElement.appendChild(iNode);

            iSubElement = iXmlDoc.createElementNS(null, "InvoiceAddress1");
            iElement.appendChild(iSubElement);
            iNode = createTextNode(iXmlDoc,
                    iOrder.getInvoiceAddress() == null
                            ? ""
                            : iOrder.getInvoiceAddress().getAddress1());
            iSubElement.appendChild(iNode);

            iSubElement = iXmlDoc.createElementNS(null, "InvoiceAddress2");
            iElement.appendChild(iSubElement);
            iNode = createTextNode(iXmlDoc,
                    iOrder.getInvoiceAddress() == null
                            ? ""
                            : iOrder.getInvoiceAddress().getAddress2());
            iSubElement.appendChild(iNode);

            iSubElement = iXmlDoc.createElementNS(null, "InvoicePostCode");
            iElement.appendChild(iSubElement);
            iNode = createTextNode(iXmlDoc,
                    iOrder.getInvoiceAddress() == null
                            ? ""
                            : iOrder.getInvoiceAddress().getZipCode());
            iSubElement.appendChild(iNode);

            iSubElement = iXmlDoc.createElementNS(null, "InvoicePostOffice");
            iElement.appendChild(iSubElement);
            iNode = createTextNode(iXmlDoc,
                    iOrder.getInvoiceAddress() == null
                            ? ""
                            : iOrder.getInvoiceAddress().getCity());
            iSubElement.appendChild(iNode);

            iSubElement = iXmlDoc.createElementNS(null, "InvoiceCountry");
            iElement.appendChild(iSubElement);
            iNode = createTextNode(iXmlDoc,
                    iOrder.getInvoiceAddress() == null
                            ? ""
                            : iOrder.getInvoiceAddress().getCountry());
            iSubElement.appendChild(iNode);

            iSubElement = iXmlDoc.createElementNS(null, "DeliveryName");
            iElement.appendChild(iSubElement);
            iNode = createTextNode(iXmlDoc,
                    iOrder.getDeliveryAddress() == null
                            ? ""
                            : iOrder.getDeliveryAddress().getName());
            iSubElement.appendChild(iNode);

            iSubElement = iXmlDoc.createElementNS(null, "DeliveryAddress1");
            iElement.appendChild(iSubElement);
            iNode = createTextNode(iXmlDoc,
                    iOrder.getDeliveryAddress() == null
                            ? ""
                            : iOrder.getDeliveryAddress().getAddress1());
            iSubElement.appendChild(iNode);

            iSubElement = iXmlDoc.createElementNS(null, "DeliveryAddress2");
            iElement.appendChild(iSubElement);
            iNode = createTextNode(iXmlDoc,
                    iOrder.getDeliveryAddress() == null
                            ? ""
                            : iOrder.getDeliveryAddress().getAddress2());
            iSubElement.appendChild(iNode);

            iSubElement = iXmlDoc.createElementNS(null, "DeliveryPostCode");
            iElement.appendChild(iSubElement);
            iNode = createTextNode(iXmlDoc,
                    iOrder.getDeliveryAddress() == null
                            ? ""
                            : iOrder.getDeliveryAddress().getZipCode());
            iSubElement.appendChild(iNode);

            iSubElement = iXmlDoc.createElementNS(null, "DeliveryPostOffice");
            iElement.appendChild(iSubElement);
            iNode = createTextNode(iXmlDoc,
                    iOrder.getDeliveryAddress() == null
                            ? ""
                            : iOrder.getDeliveryAddress().getCity());
            iSubElement.appendChild(iNode);

            iSubElement = iXmlDoc.createElementNS(null, "DeliveryCountry");
            iElement.appendChild(iSubElement);
            iNode = createTextNode(iXmlDoc,
                    iOrder.getDeliveryAddress() == null
                            ? ""
                            : iOrder.getDeliveryAddress().getCountry());
            iSubElement.appendChild(iNode);

            Element iRoot2 = iXmlDoc.createElement("Detail");

            for (SSSaleRow iRow : iOrder.getRows()) {
                iSubElement = iXmlDoc.createElementNS(null, "ArticleRow");

                Element iSubElement2 = iXmlDoc.createElementNS(null, "SellerArticleNo");

                iNode = createTextNode(iXmlDoc,
                        iRow.getProductNr() == null ? "" : iRow.getProductNr());
                iSubElement2.appendChild(iNode);
                iSubElement.appendChild(iSubElement2);

                iSubElement2 = iXmlDoc.createElementNS(null, "ArticleDescription");
                iNode = createTextNode(iXmlDoc,
                        iRow.getDescription() == null ? "" : iRow.getDescription());
                iSubElement2.appendChild(iNode);
                iSubElement.appendChild(iSubElement2);

                iSubElement2 = iXmlDoc.createElementNS(null, "UnitPrice");
                iNode = createTextNode(iXmlDoc,
                        iRow.getUnitprice() == null ? "" : iRow.getUnitprice().toString());
                iSubElement2.appendChild(iNode);
                iSubElement.appendChild(iSubElement2);

                iSubElement2 = iXmlDoc.createElementNS(null, "QuantityOrdered");
                iNode = createTextNode(iXmlDoc, tenthsToDecimalString(iRow.getQuantity()));
                iSubElement2.appendChild(iNode);
                iSubElement.appendChild(iSubElement2);

                iSubElement2 = iXmlDoc.createElementNS(null, "Unit");
                iNode = createTextNode(iXmlDoc,
                        iRow.getUnit() == null ? "" : iRow.getUnit().getName());
                iSubElement2.appendChild(iNode);
                iSubElement.appendChild(iSubElement2);

                iSubElement2 = iXmlDoc.createElementNS(null, "TotalLineDiscountPercent");
                iNode = createTextNode(iXmlDoc,
                        iRow.getDiscount() == null ? "" : iRow.getDiscount().toString());
                iRow.getNormalizedDiscount();
                iSubElement2.appendChild(iNode);
                iSubElement.appendChild(iSubElement2);

                iSubElement2 = iXmlDoc.createElementNS(null, "SumOfLine");
                iNode = createTextNode(iXmlDoc,
                        iRow.getSum().map(BigDecimal::toString).orElse(""));
                iSubElement2.appendChild(iNode);
                iSubElement.appendChild(iSubElement2);

                iSubElement2 = iXmlDoc.createElementNS(null, "VATPercentage");
                iNode = createTextNode(iXmlDoc,
                        iRow.getTaxCode() == null ? "" : iRow.getTaxCode().toString());
                iSubElement2.appendChild(iNode);
                iSubElement.appendChild(iSubElement2);

                iRoot2.appendChild(iSubElement);
            }
            iElement.appendChild(iRoot2);

            iRoot.appendChild(iElement);
        }
        iXmlDoc.appendChild(iRoot);
        try (FileOutputStream fos = new FileOutputStream(iFile.getAbsolutePath())) {
            Transformer transformer = TransformerFactory.newInstance().newTransformer();

            transformer.setOutputProperty(OutputKeys.METHOD, "xml");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "1");
            transformer.transform(new DOMSource(iXmlDoc), new StreamResult(fos));
        } catch (IOException e) {
            LOG.error("Unexpected error", e);
        } catch (javax.xml.transform.TransformerException e) {
            LOG.error("Unexpected error", e);
        }
    }

    @Override
    public String toString() {
        return "se.swedsoft.bookkeeping.importexport.xml.SSOrderExporter"
                + "{iFile=" + iFile
                + ", iItems=" + iItems
                + '}';
    }

    private Node createTextNode(Document pDocument, String pValue) {
        return pDocument.createTextNode(pValue == null ? "" : pValue);
    }

    /**
     * Converts a stored tenths quantity to a plain decimal string suitable for export.
     * E.g. {@code 25} (internal tenths) → {@code "2.5"} (UI decimal).
     *
     * @param tenths quantity stored as tenths, or {@code null}
     * @return plain decimal string, or empty string if input is null
     */
    static String tenthsToDecimalString(Integer tenths) {
        if (tenths == null) {
            return "";
        }
        return BigDecimal.valueOf(tenths, 1).toPlainString();
    }

    private Document createDocument() {
        try {
            return DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        } catch (ParserConfigurationException e) {
            throw new IllegalStateException("Could not create XML document", e);
        }
    }
}
