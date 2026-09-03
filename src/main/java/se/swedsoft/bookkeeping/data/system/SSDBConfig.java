package se.swedsoft.bookkeeping.data.system;


import org.fribok.bookkeeping.app.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.swedsoft.bookkeeping.data.SSNewAccountingYear;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Optional;


/**
 *
 * $Id$
 */
public class SSDBConfig {    private static final Logger LOG = LoggerFactory.getLogger(SSDBConfig.class);


    private static final File CONFIG_FILE = new File(Path.get(Path.USER_CONF),
            "database.config");

    private static Integer iCompanyId;

    private static Integer iYearId;

    private SSDBConfig() {}

    public static Integer getCompanyId() {
        return iCompanyId;
    }

    public static void setCompanyId(Integer iId) {
        iCompanyId = iId;

        try {
            Document iDocument = loadDocument();
            iDocument.getDocumentElement().setAttribute("company",
                    iCompanyId == null ? "" : iCompanyId.toString());

            writeDocument(iDocument);
        } catch (IOException | ParserConfigurationException | SAXException | TransformerException ex) {
            LOG.error("Unexpected error", ex);
        }
    }

    public static Integer getYearId() {
        return iYearId;
    }

    public static void setYearId(Integer pCompanyId, Integer iId) {
        iYearId = iId;

        try {
            Document iDocument = loadDocument();
            iDocument.getDocumentElement().setAttribute("year",
                    iYearId == null ? "" : iYearId.toString());

            boolean iExists = false;
            NodeList iCompanyElements = iDocument.getDocumentElement().getElementsByTagName(
                    "company");

            for (int i = 0; i < iCompanyElements.getLength(); i++) {
                Node iCompanyNode = iCompanyElements.item(i);

                if (iCompanyNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element iCompanyElement = (Element) iCompanyNode;
                    Integer iCompanyElementId = Integer.parseInt(
                            iCompanyElement.getAttribute("id"));

                    if (iCompanyElementId.equals(pCompanyId)) {
                        iCompanyElement.setAttribute("yearid",
                                iId == null ? "" : iId.toString());
                        iExists = true;
                    }
                }
            }
            if (!iExists) {
                Element iCompanyElement = iDocument.createElement("company");

                iCompanyElement.setAttribute("id", pCompanyId.toString());
                iCompanyElement.setAttribute("yearid", iId == null ? "" : iId.toString());
                iDocument.getDocumentElement().appendChild(iCompanyElement);
            }

            writeDocument(iDocument);

        } catch (IOException | ParserConfigurationException | SAXException | TransformerException ex) {
            LOG.error("Unexpected error", ex);
        }
    }

    public static Optional<SSNewAccountingYear> loadCompanySetting(Integer pCompanyId) {
        if (pCompanyId == null) {
            return Optional.empty();
        }

        try {
            Document iDocument = loadDocument();
            NodeList iCompanyElements = iDocument.getDocumentElement().getElementsByTagName(
                    "company");

            for (int i = 0; i < iCompanyElements.getLength(); i++) {
                Node iCompanyNode = iCompanyElements.item(i);

                if (iCompanyNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element iCompanyElement = (Element) iCompanyNode;
                    Integer iCompanyElementId = Integer.parseInt(
                            iCompanyElement.getAttribute("id"));

                    if (iCompanyElementId.equals(pCompanyId)) {
                        String iResult = iCompanyElement.getAttribute("yearid");

                        if (iResult.isEmpty()) {
                            return Optional.empty();
                        }
                        SSNewAccountingYear iYear = new SSNewAccountingYear();

                        iYear.setId(Integer.parseInt(iResult));
                        return se.swedsoft.bookkeeping.data.system.SSCompanyYearContext.getAccountingYear(iYear);
                    }
                }
            }

        } catch (IOException | SAXException | ParserConfigurationException ex) {
            LOG.error("Unexpected error", ex);
        }
        return Optional.empty();
    }

    static {
        load();
    }

    /*
     * Create a config file if not found
    */
    private static void createIfNotExists() throws IOException {
        File parent = CONFIG_FILE.getParentFile();
        if (parent != null && !parent.exists()) {
            if (!parent.mkdirs()) {
                LOG.warn("Failed to create config directory: {}", parent);
            }
        }
        if (CONFIG_FILE.createNewFile()) {
            LOG.info("Creating database config file.");

            try {
                DocumentBuilder iBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
                Document iDocument = iBuilder.newDocument();
                iDocument.appendChild(iDocument.createElement("database"));
                writeDocument(iDocument);
            } catch (ParserConfigurationException | TransformerException ex) {
                throw new IOException(ex);
            }
        }
    }

    /**
     *
     */
    public static void load() {
        try {
            createIfNotExists();
            Document iDocument = loadDocument();

            String iCompany = null;

            if (iDocument.getDocumentElement().hasAttribute("company")) {
                iCompany = iDocument.getDocumentElement().getAttribute(
                        "company");
            }
            if (iCompany != null && !iCompany.isEmpty()) {
                iCompanyId = Integer.parseInt(iCompany);
            }

            String iYear = null;

            if (iDocument.getDocumentElement().hasAttribute("year")) {
                iYear = iDocument.getDocumentElement().getAttribute("year");
            }
            if (iYear != null && !iYear.isEmpty()) {
                iYearId = Integer.parseInt(iYear);
            }

        } catch (IOException | ParserConfigurationException | SAXException ex) {
            LOG.error("Unexpected error", ex);
        }
    }

    private static Document loadDocument() throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilderFactory iFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder iBuilder = iFactory.newDocumentBuilder();

        try (FileInputStream iInputStream = new FileInputStream(CONFIG_FILE)) {
            return iBuilder.parse(iInputStream);
        }
    }

    private static void writeDocument(Document pDocument) throws ParserConfigurationException, TransformerException, IOException {
        TransformerFactory iFactory = TransformerFactory.newInstance();
        Transformer iTransformer = iFactory.newTransformer();

        iTransformer.setOutputProperty(OutputKeys.METHOD, "xml");
        iTransformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        iTransformer.setOutputProperty(OutputKeys.INDENT, "yes");
        iTransformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "1");

        try (FileOutputStream iOutputStream = new FileOutputStream(CONFIG_FILE)) {
            iTransformer.transform(new DOMSource(pDocument), new StreamResult(iOutputStream));
        }
    }
}
