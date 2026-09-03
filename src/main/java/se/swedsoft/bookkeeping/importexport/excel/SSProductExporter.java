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
import se.swedsoft.bookkeeping.data.SSProduct;
import se.swedsoft.bookkeeping.data.SSProductRow;
import se.swedsoft.bookkeeping.data.SSStock;
import se.swedsoft.bookkeeping.data.SSSupplier;
import se.swedsoft.bookkeeping.data.common.SSDefaultAccount;
import se.swedsoft.bookkeeping.data.system.SSDB;
import se.swedsoft.bookkeeping.data.system.SSProductContext;
import se.swedsoft.bookkeeping.importexport.util.SSExportException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * User: Andreas Lago
 * Date: 2006-aug-01
 * Time: 11:32:25
 * $Id$
 */
public class SSProductExporter {

    private static final Logger LOG = LoggerFactory.getLogger(SSProductExporter.class);

    public static final String PRODUKTNUMMER = "Produkt-id";
    public static final String BESKRIVNING = "Beskrivning";
    public static final String FORSALJNINGSPRIS = "Försäljningspris";
    public static final String INKOPSPRIS = "Inköpspris";
    public static final String ENHETSFRAKT = "Enhetsfrakt";
    public static final String MOMS = "Moms";
    public static final String ENHET = "Enhet";
    public static final String VIKT = "Vikt";
    public static final String VOLYM = "Volym";
    public static final String LEVERANTOR = "Leverantör";
    public static final String LEVERANTORENS_ARTIKEL_NUMMER = "Leverantörens artikelnummer";
    public static final String BESTALLNINGSPUNKT = "Beställningspunkt";
    public static final String LAGERPLATS = "Lagerplats";
    public static final String LAGERANTAL = "Lagerantal";
    public static final String DISPONIBELT = "Disponibelt";
    public static final String LAGERPRIS = "Lagerpris";

    private final File iFile;
    private final List<SSProduct> iProducts;

    /**
     * Creates an exporter that uses all products from the database.
     *
     * @param iFile destination Excel/XML file
     */
    public SSProductExporter(File iFile) {
        this.iFile = iFile;
        iProducts = SSProductContext.getProducts();
    }

    /**
     * Creates an exporter with an explicit product list.
     *
     * @param iFile destination Excel/XML file
     * @param iProducts products to export
     */
    public SSProductExporter(File iFile, List<SSProduct> iProducts) {
        this.iFile = iFile;
        this.iProducts = iProducts;
    }

    /**
     * Exports products to Excel format.
     *
     * @throws IOException if writing to disk fails
     */
    public void doExport() throws IOException {
        try (Workbook iWorkbook = new XSSFWorkbook();
             FileOutputStream iOut = new FileOutputStream(iFile)) {
            Sheet iSheet = iWorkbook.createSheet("Produkter");

            writeSheet(iSheet, iWorkbook);

            iWorkbook.write(iOut);
        } catch (RuntimeException e) {
            throw new SSExportException(e.getLocalizedMessage());
        }

    }

    /**
     * Writes all products to the provided worksheet.
     *
     * @param pSheet writable destination sheet
     */
    private void writeSheet(Sheet pSheet, Workbook pWorkbook) {
        Row iColumns = pSheet.createRow(0);
        CellStyle iHeaderStyle = pWorkbook.createCellStyle();

        iHeaderStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        iHeaderStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        setHeaderCell(iColumns, 0, PRODUKTNUMMER, iHeaderStyle);
        setHeaderCell(iColumns, 1, BESKRIVNING, iHeaderStyle);
        setHeaderCell(iColumns, 2, FORSALJNINGSPRIS, iHeaderStyle);
        setHeaderCell(iColumns, 3, INKOPSPRIS, iHeaderStyle);
        setHeaderCell(iColumns, 4, ENHETSFRAKT, iHeaderStyle);
        setHeaderCell(iColumns, 5, MOMS, iHeaderStyle);
        setHeaderCell(iColumns, 6, ENHET, iHeaderStyle);
        setHeaderCell(iColumns, 7, VIKT, iHeaderStyle);
        setHeaderCell(iColumns, 8, VOLYM, iHeaderStyle);
        setHeaderCell(iColumns, 9, LEVERANTOR, iHeaderStyle);
        setHeaderCell(iColumns, 10, LEVERANTORENS_ARTIKEL_NUMMER, iHeaderStyle);
        setHeaderCell(iColumns, 11, BESTALLNINGSPUNKT, iHeaderStyle);
        setHeaderCell(iColumns, 12, LAGERPLATS, iHeaderStyle);
        setHeaderCell(iColumns, 13, LAGERANTAL, iHeaderStyle);
        setHeaderCell(iColumns, 14, DISPONIBELT, iHeaderStyle);
        setHeaderCell(iColumns, 15, LAGERPRIS, iHeaderStyle);

        SSStock iStock = new SSStock(true);

        int iRowIndex = 1;

        for (SSProduct iProduct: iProducts) {
            SSSupplier iMainsupplier = iProduct.getSupplier(
                    se.swedsoft.bookkeeping.data.system.SSPurchaseContext.getSuppliers());

            Row iRow = pSheet.createRow(iRowIndex);

            setStringCell(iRow, 0, iProduct.getNumber());
            setStringCell(iRow, 1, iProduct.getDescription());
            setNumberCell(iRow, 2, iProduct.getSellingPrice());
            setNumberCell(iRow, 3, iProduct.getPurchasePrice());
            setNumberCell(iRow, 4, iProduct.getUnitFreight());
            setNumberCell(iRow, 5, iProduct.getTaxRate().orElse(null));
            setStringCell(iRow, 6,
                    iProduct.getUnit() == null ? "" : iProduct.getUnit().getName());
            setNumberCell(iRow, 7, iProduct.getWeight());
            setNumberCell(iRow, 8, iProduct.getVolume());

            setStringCell(iRow, 9, iMainsupplier == null ? "" : iMainsupplier.getNumber());
            setStringCell(iRow, 10, iProduct.getSupplierProductNr());
            setNumberCell(iRow, 11,
                    iProduct.getOrderpoint() == null ? 0 : iProduct.getOrderpoint());

            setStringCell(iRow, 12, iProduct.getWarehouseLocation());
            setNumberCell(iRow, 13, BigDecimal.valueOf(iStock.getQuantity(iProduct), 1));
            setNumberCell(iRow, 14, BigDecimal.valueOf(iStock.getAvaiable(iProduct), 1));
            setNumberCell(iRow, 15,
                    iProduct.getStockPrice() == null ? 0 : iProduct.getStockPrice());
            iRowIndex++;
        }

        for (int i = 0; i <= 15; i++) {
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

    private void setNumberCell(Row pRow, int pColumn, Number pValue) {
        if (pValue == null) {
            pRow.createCell(pColumn).setCellValue("");
            return;
        }
        pRow.createCell(pColumn).setCellValue(pValue.doubleValue());
    }

    public void doXMLExport() {

        Document iXmlDoc = createDocument();
        Element iRoot = iXmlDoc.createElement("Products");

        for (SSProduct iProduct : iProducts) {
            Element iElement = iXmlDoc.createElementNS(null, "Product");

            Element iSubElement = iXmlDoc.createElementNS(null, "ProductNo");

            iElement.appendChild(iSubElement);
            Node iNode = createTextNode(iXmlDoc,
                    iProduct.getNumber() == null ? "" : iProduct.getNumber());

            iSubElement.appendChild(iNode);

            iSubElement = iXmlDoc.createElementNS(null, "ProductDescription");
            iElement.appendChild(iSubElement);
            iNode = createTextNode(iXmlDoc, iProduct.getDescription());
            iSubElement.appendChild(iNode);

            iSubElement = iXmlDoc.createElementNS(null, "UnitPrice");
            iElement.appendChild(iSubElement);
            iNode = createTextNode(iXmlDoc,
                    iProduct.getSellingPrice() == null ? "" : iProduct.getSellingPrice().toString());
            iSubElement.appendChild(iNode);

            iSubElement = iXmlDoc.createElementNS(null, "TaxRate");
            iElement.appendChild(iSubElement);
            iNode = createTextNode(iXmlDoc,
                    iProduct.getTaxRate().map(Object::toString).orElse(""));
            iSubElement.appendChild(iNode);

            iSubElement = iXmlDoc.createElementNS(null, "PurchasePrice");
            iElement.appendChild(iSubElement);
            iNode = createTextNode(iXmlDoc,
                    iProduct.getPurchasePrice() == null ? "" : iProduct.getPurchasePrice().toString());
            iSubElement.appendChild(iNode);

            iSubElement = iXmlDoc.createElementNS(null, "UnitFreight");
            iElement.appendChild(iSubElement);
            iNode = createTextNode(iXmlDoc,
                    iProduct.getUnitFreight() == null ? "" : iProduct.getUnitFreight().toString());
            iSubElement.appendChild(iNode);

            iSubElement = iXmlDoc.createElementNS(null, "Unit");
            iElement.appendChild(iSubElement);
            iNode = createTextNode(iXmlDoc,
                    iProduct.getUnit() == null ? "" : iProduct.getUnit().getName());
            iSubElement.appendChild(iNode);

            iSubElement = iXmlDoc.createElementNS(null, "Weight");
            iElement.appendChild(iSubElement);
            iNode = createTextNode(iXmlDoc,
                    iProduct.getWeight() == null ? "" : iProduct.getWeight().toString());
            iSubElement.appendChild(iNode);

            iSubElement = iXmlDoc.createElementNS(null, "Volume");
            iElement.appendChild(iSubElement);
            iNode = createTextNode(iXmlDoc,
                    iProduct.getVolume() == null ? "" : iProduct.getVolume().toString());
            iSubElement.appendChild(iNode);

            iSubElement = iXmlDoc.createElementNS(null, "SaleAccount");
            iElement.appendChild(iSubElement);
            iNode = createTextNode(iXmlDoc,
                    iProduct.getDefaultAccount(SSDefaultAccount.Sales) == null
                            ? ""
                            : iProduct.getDefaultAccount(SSDefaultAccount.Sales).toString());
            iSubElement.appendChild(iNode);

            iSubElement = iXmlDoc.createElementNS(null, "PurchaseAccount");
            iElement.appendChild(iSubElement);
            iNode = createTextNode(iXmlDoc,
                    iProduct.getDefaultAccount(SSDefaultAccount.Purchases) == null
                            ? ""
                            : iProduct.getDefaultAccount(SSDefaultAccount.Purchases).toString());
            iSubElement.appendChild(iNode);

            iSubElement = iXmlDoc.createElementNS(null, "Expired");
            iElement.appendChild(iSubElement);
            iNode = createTextNode(iXmlDoc, Boolean.toString(iProduct.isExpired()));
            iSubElement.appendChild(iNode);

            iSubElement = iXmlDoc.createElementNS(null, "StockProduct");
            iElement.appendChild(iSubElement);
            iNode = createTextNode(iXmlDoc, Boolean.toString(iProduct.isStockProduct()));
            iSubElement.appendChild(iNode);

            iSubElement = iXmlDoc.createElementNS(null, "WarehouseLocation");
            iElement.appendChild(iSubElement);
            iNode = createTextNode(iXmlDoc, iProduct.getWarehouseLocation());
            iSubElement.appendChild(iNode);

            iSubElement = iXmlDoc.createElementNS(null, "OrderPoint");
            iElement.appendChild(iSubElement);
            iNode = createTextNode(iXmlDoc,
                    iProduct.getOrderpoint() == null ? "" : iProduct.getOrderpoint().toString());
            iSubElement.appendChild(iNode);

            iSubElement = iXmlDoc.createElementNS(null, "OrderCount");
            iElement.appendChild(iSubElement);
            iNode = createTextNode(iXmlDoc, tenthsToDecimalString(iProduct.getOrdercount()));
            iSubElement.appendChild(iNode);

            iSubElement = iXmlDoc.createElementNS(null, "Supplier");
            iElement.appendChild(iSubElement);
            iNode = createTextNode(iXmlDoc, iProduct.getSupplierNr());
            iSubElement.appendChild(iNode);

            iSubElement = iXmlDoc.createElementNS(null, "SupplierProductNo");
            iElement.appendChild(iSubElement);
            iNode = createTextNode(iXmlDoc, iProduct.getSupplierProductNr());
            iSubElement.appendChild(iNode);

            iSubElement = iXmlDoc.createElementNS(null, "EnProductDescription");
            iElement.appendChild(iSubElement);
            iNode = createTextNode(iXmlDoc, iProduct.getDescription(Locale.forLanguageTag("en")).orElse(null));
            iSubElement.appendChild(iNode);

            Element iRoot2 = iXmlDoc.createElement("Detail");

            for (SSProductRow iRow : iProduct.getParcelRows()) {
                iSubElement = iXmlDoc.createElementNS(null, "ParcelRow");

                Element iSubElement2 = iXmlDoc.createElementNS(null, "IncludedProductNo");

                iNode = createTextNode(iXmlDoc, iRow.getProductNr());
                iSubElement2.appendChild(iNode);
                iSubElement.appendChild(iSubElement2);

                iSubElement2 = iXmlDoc.createElementNS(null, "RowQuantity");
                iNode = createTextNode(iXmlDoc, tenthsToDecimalString(iRow.getQuantity()));
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
        } catch (IOException | TransformerException e) {
            LOG.error("Unexpected error", e);
        }
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

    @Override
    public String toString() {
        return "se.swedsoft.bookkeeping.importexport.excel.SSProductExporter"
                + "{iFile=" + iFile
                + ", iProducts=" + iProducts
                + '}';
    }
}
