package se.swedsoft.bookkeeping.importexport.excel;


import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import se.swedsoft.bookkeeping.data.SSCustomer;
import se.swedsoft.bookkeeping.data.system.SSDB;
import se.swedsoft.bookkeeping.importexport.util.SSExportException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * User: Andreas Lago
 * Date: 2006-aug-01
 * Time: 11:32:25
 * $Id$
 */
public class SSCustomerExporter {

    private static final Logger LOG = LoggerFactory.getLogger(SSCustomerExporter.class);

    // Column names
    public static final String KUNDNUMMER = "Kund-id";
    public static final String NAMN = "Namn";
    public static final String TELEFON1 = "Telefon1";
    public static final String TELEFON2 = "Telefon2";
    public static final String FAX = "Fax";
    public static final String EPOST = "Epost";
    public static final String KONTAKTPERSON = "Kontaktperson";
    public static final String ORGANISATIONSNUMMER = "Organisationsnummer";
    public static final String BANKGIRO = "Bankgiro";
    public static final String PLUSGIRO = "Plusgiro";
    public static final String FAKTURAADRESS_NAMN = "Fakturaadress.Namn";
    public static final String FAKTURAADRESS_ADRESS1 = "Fakturaadress.Adress1";
    public static final String FAKTURAADRESS_ADRESS2 = "Fakturaadress.Adress2";
    public static final String FAKTURAADRESS_POSTNUMMER = "Fakturaadress.Postnummer";
    public static final String FAKTURAADRESS_POSTORT = "Fakturaadress.Postort";
    public static final String FAKTURAADRESS_LAND = "Fakturaadress.Land";
    public static final String LEVERANSADRESS_NAMN = "Leveransadress.Namn";
    public static final String LEVERANSADRESS_ADRESS1 = "Leveransadress.Adress1";
    public static final String LEVERANSADRESS_ADRESS2 = "Leveransadress.Adress2";
    public static final String LEVERANSADRESS_POSTNUMMER = "Leveransadress.Postnummer";
    public static final String LEVERANSADRESS_POSTORT = "Leveransadress.Postort";
    public static final String LEVERANSADRESS_LAND = "Leveransadress.Land";

    private final File iFile;
    private final List<SSCustomer> iCustomers;

    /**
     * Creates an exporter that uses all customers from the database.
     *
     * @param iFile destination Excel/XML file
     */
    public SSCustomerExporter(File iFile) {
        this.iFile = iFile;
        iCustomers = se.swedsoft.bookkeeping.data.system.SSSalesContext.getCustomers();
    }

    /**
     * Creates an exporter with an explicit customer list.
     *
     * @param iFile destination Excel/XML file
     * @param iCustomers customers to export
     */
    public SSCustomerExporter(File iFile, List<SSCustomer> iCustomers) {
        this.iFile = iFile;
        this.iCustomers = iCustomers;
    }

    /**
     * Exports customers to Excel format.
     *
     * @throws IOException if writing to disk fails
     * @throws SSExportException if workbook export fails
     */
    public void export()  throws IOException, SSExportException {
        try (Workbook iWorkbook = new XSSFWorkbook();
             FileOutputStream iOut = new FileOutputStream(iFile)) {
            Sheet iSheet = iWorkbook.createSheet("Kunder");

            writeCustomers(iSheet, iWorkbook);

            iWorkbook.write(iOut);
        } catch (RuntimeException e) {
            throw new SSExportException(e.getLocalizedMessage());
        }

    }

    /**
     * Writes all customers to the provided worksheet.
     *
     * @param pSheet writable destination sheet
     * @throws WriteException if sheet writing fails
     */
    private void writeCustomers(Sheet pSheet, Workbook pWorkbook) {
        Row iColumns = pSheet.createRow(0);
        CellStyle iHeaderStyle = pWorkbook.createCellStyle();

        iHeaderStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        iHeaderStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        setHeaderCell(iColumns, 0, KUNDNUMMER, iHeaderStyle);
        setHeaderCell(iColumns, 1, NAMN, iHeaderStyle);
        setHeaderCell(iColumns, 2, TELEFON1, iHeaderStyle);
        setHeaderCell(iColumns, 3, TELEFON2, iHeaderStyle);
        setHeaderCell(iColumns, 4, FAX, iHeaderStyle);
        setHeaderCell(iColumns, 5, EPOST, iHeaderStyle);
        setHeaderCell(iColumns, 6, KONTAKTPERSON, iHeaderStyle);
        setHeaderCell(iColumns, 7, ORGANISATIONSNUMMER, iHeaderStyle);
        setHeaderCell(iColumns, 8, BANKGIRO, iHeaderStyle);
        setHeaderCell(iColumns, 9, PLUSGIRO, iHeaderStyle);
        setHeaderCell(iColumns, 10, FAKTURAADRESS_NAMN, iHeaderStyle);
        setHeaderCell(iColumns, 11, FAKTURAADRESS_ADRESS1, iHeaderStyle);
        setHeaderCell(iColumns, 12, FAKTURAADRESS_ADRESS2, iHeaderStyle);
        setHeaderCell(iColumns, 13, FAKTURAADRESS_POSTNUMMER, iHeaderStyle);
        setHeaderCell(iColumns, 14, FAKTURAADRESS_POSTORT, iHeaderStyle);
        setHeaderCell(iColumns, 15, FAKTURAADRESS_LAND, iHeaderStyle);
        setHeaderCell(iColumns, 16, LEVERANSADRESS_NAMN, iHeaderStyle);
        setHeaderCell(iColumns, 17, LEVERANSADRESS_ADRESS1, iHeaderStyle);
        setHeaderCell(iColumns, 18, LEVERANSADRESS_ADRESS2, iHeaderStyle);
        setHeaderCell(iColumns, 19, LEVERANSADRESS_POSTNUMMER, iHeaderStyle);
        setHeaderCell(iColumns, 20, LEVERANSADRESS_POSTORT, iHeaderStyle);
        setHeaderCell(iColumns, 21, LEVERANSADRESS_LAND, iHeaderStyle);

        int iRowIndex = 1;
        for (SSCustomer iCustomer : iCustomers) {
            Row iRow = pSheet.createRow(iRowIndex++);

            setStringCell(iRow, 0, iCustomer.getNumber());
            setStringCell(iRow, 1, iCustomer.getName());
            setStringCell(iRow, 2, iCustomer.getPhone1());
            setStringCell(iRow, 3, iCustomer.getPhone2());
            setStringCell(iRow, 4, iCustomer.getTelefax());
            setStringCell(iRow, 5, iCustomer.getEMail());
            setStringCell(iRow, 6, iCustomer.getYourContactPerson());
            setStringCell(iRow, 7, iCustomer.getRegistrationNumber());
            setStringCell(iRow, 8, iCustomer.getBankgiro());
            setStringCell(iRow, 9, iCustomer.getPlusgiro());

            setStringCell(iRow, 10, iCustomer.getInvoiceAddress().getName());
            setStringCell(iRow, 11, iCustomer.getInvoiceAddress().getAddress1());
            setStringCell(iRow, 12, iCustomer.getInvoiceAddress().getAddress2());
            setStringCell(iRow, 13, iCustomer.getInvoiceAddress().getZipCode());
            setStringCell(iRow, 14, iCustomer.getInvoiceAddress().getCity());
            setStringCell(iRow, 15, iCustomer.getInvoiceAddress().getCountry());

            setStringCell(iRow, 16, iCustomer.getDeliveryAddress().getName());
            setStringCell(iRow, 17, iCustomer.getDeliveryAddress().getAddress1());
            setStringCell(iRow, 18, iCustomer.getDeliveryAddress().getAddress2());
            setStringCell(iRow, 19, iCustomer.getDeliveryAddress().getZipCode());
            setStringCell(iRow, 20, iCustomer.getDeliveryAddress().getCity());
            setStringCell(iRow, 21, iCustomer.getDeliveryAddress().getCountry());
        }

        for (int i = 0; i <= 21; i++) {
            pSheet.autoSizeColumn(i);
        }
    }

    private void setHeaderCell(Row pRow, int pColumn, String pValue, CellStyle pStyle) {
        Cell iCell = pRow.createCell(pColumn);

        iCell.setCellValue(pValue == null ? "" : pValue);
        iCell.setCellStyle(pStyle);
    }

    private void setStringCell(Row pRow, int pColumn, String pValue) {
        pRow.createCell(pColumn).setCellValue(pValue == null ? "" : pValue);
    }

    public void doXMLExport() {

        Document iXmlDoc = createDocument();
        Element iRoot = iXmlDoc.createElement("Customers");

        for (SSCustomer iCustomer : iCustomers) {
            Element iElement = iXmlDoc.createElementNS(null, "Customer");

            Element iSubElement = iXmlDoc.createElementNS(null, "CustomerNo");

            iElement.appendChild(iSubElement);
            Node iNode = createTextNode(iXmlDoc,
                    iCustomer.getNumber() == null ? "" : iCustomer.getNumber());

            iSubElement.appendChild(iNode);

            iSubElement = iXmlDoc.createElementNS(null, "CustomerName");
            iElement.appendChild(iSubElement);
            iNode = createTextNode(iXmlDoc, iCustomer.getName());
            iSubElement.appendChild(iNode);

            iSubElement = iXmlDoc.createElementNS(null, "OurContactPerson");
            iElement.appendChild(iSubElement);
            iNode = createTextNode(iXmlDoc, iCustomer.getOurContactPerson());
            iSubElement.appendChild(iNode);

            iSubElement = iXmlDoc.createElementNS(null, "YourContactPerson");
            iElement.appendChild(iSubElement);
            iNode = createTextNode(iXmlDoc, iCustomer.getYourContactPerson());
            iSubElement.appendChild(iNode);

            iSubElement = iXmlDoc.createElementNS(null, "CurrencyCode");
            iElement.appendChild(iSubElement);
            iNode = createTextNode(iXmlDoc,
                    iCustomer.getInvoiceCurrency() == null
                            ? ""
                            : iCustomer.getInvoiceCurrency().getName());
            iSubElement.appendChild(iNode);

            iSubElement = iXmlDoc.createElementNS(null, "PaymentTerms");
            iElement.appendChild(iSubElement);
            iNode = createTextNode(iXmlDoc,
                    iCustomer.getPaymentTerm() == null
                            ? ""
                            : iCustomer.getPaymentTerm().getName());
            iSubElement.appendChild(iNode);

            iSubElement = iXmlDoc.createElementNS(null, "DeliveryTerms");
            iElement.appendChild(iSubElement);
            iNode = createTextNode(iXmlDoc,
                    iCustomer.getDeliveryTerm() == null
                            ? ""
                            : iCustomer.getDeliveryTerm().getName());
            iSubElement.appendChild(iNode);

            iSubElement = iXmlDoc.createElementNS(null, "DeliveryMethod");
            iElement.appendChild(iSubElement);
            iNode = createTextNode(iXmlDoc,
                    iCustomer.getDeliveryWay() == null
                            ? ""
                            : iCustomer.getDeliveryWay().getName());
            iSubElement.appendChild(iNode);

            iSubElement = iXmlDoc.createElementNS(null, "TaxFree");
            iElement.appendChild(iSubElement);
            iNode = createTextNode(iXmlDoc, Boolean.toString(iCustomer.getTaxFree()));
            iSubElement.appendChild(iNode);

            iSubElement = iXmlDoc.createElementNS(null, "EuSaleCommodity");
            iElement.appendChild(iSubElement);
            iNode = createTextNode(iXmlDoc,
                    Boolean.toString(iCustomer.getEuSaleCommodity()));
            iSubElement.appendChild(iNode);

            iSubElement = iXmlDoc.createElementNS(null, "EuSaleThirdPartCommodity");
            iElement.appendChild(iSubElement);
            iNode = createTextNode(iXmlDoc,
                    Boolean.toString(iCustomer.getEuSaleYhirdPartCommodity()));
            iSubElement.appendChild(iNode);

            iSubElement = iXmlDoc.createElementNS(null, "HideUnitPrice");
            iElement.appendChild(iSubElement);
            iNode = createTextNode(iXmlDoc, Boolean.toString(iCustomer.getHideUnitprice()));
            iSubElement.appendChild(iNode);

            iSubElement = iXmlDoc.createElementNS(null, "VATRegNo");
            iElement.appendChild(iSubElement);
            iNode = createTextNode(iXmlDoc,
                    iCustomer.getVATNumber() == null ? "" : iCustomer.getVATNumber());
            iSubElement.appendChild(iNode);

            iSubElement = iXmlDoc.createElementNS(null, "Email");
            iElement.appendChild(iSubElement);
            iNode = createTextNode(iXmlDoc,
                    iCustomer.getEMail() == null ? "" : iCustomer.getEMail());
            iSubElement.appendChild(iNode);

            iSubElement = iXmlDoc.createElementNS(null, "CompanyNo");
            iElement.appendChild(iSubElement);
            iNode = createTextNode(iXmlDoc,
                    iCustomer.getRegistrationNumber() == null
                            ? ""
                            : iCustomer.getRegistrationNumber());
            iSubElement.appendChild(iNode);

            iSubElement = iXmlDoc.createElementNS(null, "Telefax");
            iElement.appendChild(iSubElement);
            iNode = createTextNode(iXmlDoc,
                    iCustomer.getTelefax() == null ? "" : iCustomer.getTelefax());
            iSubElement.appendChild(iNode);

            iSubElement = iXmlDoc.createElementNS(null, "Telephone");
            iElement.appendChild(iSubElement);
            iNode = createTextNode(iXmlDoc,
                    iCustomer.getPhone1() == null ? "" : iCustomer.getPhone1());
            iSubElement.appendChild(iNode);

            iSubElement = iXmlDoc.createElementNS(null, "Telephone2");
            iElement.appendChild(iSubElement);
            iNode = createTextNode(iXmlDoc,
                    iCustomer.getPhone2() == null ? "" : iCustomer.getPhone2());
            iSubElement.appendChild(iNode);

            iSubElement = iXmlDoc.createElementNS(null, "CreditLimit");
            iElement.appendChild(iSubElement);
            iNode = createTextNode(iXmlDoc,
                    iCustomer.getCreditLimit() == null
                            ? ""
                            : iCustomer.getCreditLimit().toString());
            iSubElement.appendChild(iNode);

            iSubElement = iXmlDoc.createElementNS(null, "Discount");
            iElement.appendChild(iSubElement);
            iNode = createTextNode(iXmlDoc,
                    iCustomer.getDiscount() == null
                            ? ""
                            : iCustomer.getDiscount().toString());
            iSubElement.appendChild(iNode);

            iSubElement = iXmlDoc.createElementNS(null, "BgNo");
            iElement.appendChild(iSubElement);
            iNode = createTextNode(iXmlDoc,
                    iCustomer.getBankgiro() == null ? "" : iCustomer.getBankgiro());
            iSubElement.appendChild(iNode);

            iSubElement = iXmlDoc.createElementNS(null, "PgNo");
            iElement.appendChild(iSubElement);
            iNode = createTextNode(iXmlDoc,
                    iCustomer.getPlusgiro() == null ? "" : iCustomer.getPlusgiro());
            iSubElement.appendChild(iNode);

            iSubElement = iXmlDoc.createElementNS(null, "CreditLimit");
            iElement.appendChild(iSubElement);
            iNode = createTextNode(iXmlDoc,
                    iCustomer.getCreditLimit() == null
                            ? ""
                            : iCustomer.getCreditLimit().toString());
            iSubElement.appendChild(iNode);

            iSubElement = iXmlDoc.createElementNS(null, "InvoiceName");
            iElement.appendChild(iSubElement);
            iNode = createTextNode(iXmlDoc,
                    iCustomer.getInvoiceAddress() == null
                            ? ""
                            : iCustomer.getInvoiceAddress().getName());
            iSubElement.appendChild(iNode);

            iSubElement = iXmlDoc.createElementNS(null, "InvoiceAddress1");
            iElement.appendChild(iSubElement);
            iNode = createTextNode(iXmlDoc,
                    iCustomer.getInvoiceAddress() == null
                            ? ""
                            : iCustomer.getInvoiceAddress().getAddress1());
            iSubElement.appendChild(iNode);

            iSubElement = iXmlDoc.createElementNS(null, "InvoiceAddress2");
            iElement.appendChild(iSubElement);
            iNode = createTextNode(iXmlDoc,
                    iCustomer.getInvoiceAddress() == null
                            ? ""
                            : iCustomer.getInvoiceAddress().getAddress2());
            iSubElement.appendChild(iNode);

            iSubElement = iXmlDoc.createElementNS(null, "InvoicePostCode");
            iElement.appendChild(iSubElement);
            iNode = createTextNode(iXmlDoc,
                    iCustomer.getInvoiceAddress() == null
                            ? ""
                            : iCustomer.getInvoiceAddress().getZipCode());
            iSubElement.appendChild(iNode);

            iSubElement = iXmlDoc.createElementNS(null, "InvoicePostOffice");
            iElement.appendChild(iSubElement);
            iNode = createTextNode(iXmlDoc,
                    iCustomer.getInvoiceAddress() == null
                            ? ""
                            : iCustomer.getInvoiceAddress().getCity());
            iSubElement.appendChild(iNode);

            iSubElement = iXmlDoc.createElementNS(null, "InvoiceCountry");
            iElement.appendChild(iSubElement);
            iNode = createTextNode(iXmlDoc,
                    iCustomer.getInvoiceAddress() == null
                            ? ""
                            : iCustomer.getInvoiceAddress().getCountry());
            iSubElement.appendChild(iNode);

            iSubElement = iXmlDoc.createElementNS(null, "DeliveryName");
            iElement.appendChild(iSubElement);
            iNode = createTextNode(iXmlDoc,
                    iCustomer.getDeliveryAddress() == null
                            ? ""
                            : iCustomer.getDeliveryAddress().getName());
            iSubElement.appendChild(iNode);

            iSubElement = iXmlDoc.createElementNS(null, "DeliveryAddress1");
            iElement.appendChild(iSubElement);
            iNode = createTextNode(iXmlDoc,
                    iCustomer.getDeliveryAddress() == null
                            ? ""
                            : iCustomer.getDeliveryAddress().getAddress1());
            iSubElement.appendChild(iNode);

            iSubElement = iXmlDoc.createElementNS(null, "DeliveryAddress2");
            iElement.appendChild(iSubElement);
            iNode = createTextNode(iXmlDoc,
                    iCustomer.getDeliveryAddress() == null
                            ? ""
                            : iCustomer.getDeliveryAddress().getAddress2());
            iSubElement.appendChild(iNode);

            iSubElement = iXmlDoc.createElementNS(null, "DeliveryPostCode");
            iElement.appendChild(iSubElement);
            iNode = createTextNode(iXmlDoc,
                    iCustomer.getDeliveryAddress() == null
                            ? ""
                            : iCustomer.getDeliveryAddress().getZipCode());
            iSubElement.appendChild(iNode);

            iSubElement = iXmlDoc.createElementNS(null, "DeliveryPostOffice");
            iElement.appendChild(iSubElement);
            iNode = createTextNode(iXmlDoc,
                    iCustomer.getDeliveryAddress() == null
                            ? ""
                            : iCustomer.getDeliveryAddress().getCity());
            iSubElement.appendChild(iNode);

            iSubElement = iXmlDoc.createElementNS(null, "DeliveryCountry");
            iElement.appendChild(iSubElement);
            iNode = createTextNode(iXmlDoc,
                    iCustomer.getDeliveryAddress() == null
                            ? ""
                            : iCustomer.getDeliveryAddress().getCountry());
            iSubElement.appendChild(iNode);

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
        } catch (IOException | TransformerException e) {
            LOG.error("Unexpected error", e);
        }
    }

    private Node createTextNode(Document pDocument, String pValue) {
        return pDocument.createTextNode(pValue == null ? "" : pValue);
    }

    private Document createDocument() {
        try {
            return DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        } catch (ParserConfigurationException e) {
            throw new IllegalStateException("Could not create XML document", e);
        }
    }

    @Override
    public String toString() {
        return "se.swedsoft.bookkeeping.importexport.excel.SSCustomerExporter"
                + "{iCustomers=" + iCustomers
                + ", iFile=" + iFile
                + '}';
    }
}
