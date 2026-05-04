package se.swedsoft.bookkeeping.data.system;


import org.apache.xerces.parsers.DOMParser;
import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.XMLSerializer;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.fribok.bookkeeping.app.Path;
import se.swedsoft.bookkeeping.data.SSNewAccountingYear;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Optional;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributeListImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


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
        DOMParser iParser = new DOMParser();

        try {
            iParser.parse(new InputSource(new FileInputStream(CONFIG_FILE)));
            iParser.getDocument().getDocumentElement().setAttribute("company",
                    iCompanyId == null ? "" : iCompanyId.toString());

            // Write back the database path to the config file.
            OutputFormat iFormat = new OutputFormat(iParser.getDocument());
            XMLSerializer serializer = new XMLSerializer(new FileOutputStream(CONFIG_FILE),
                    iFormat);

            serializer.serialize(iParser.getDocument());
        } catch (IOException | SAXException ex) {
            LOG.error("Unexpected error", ex);
        }
    }

    public static Integer getYearId() {
        return iYearId;
    }

    public static void setYearId(Integer pCompanyId, Integer iId) {
        iYearId = iId;
        DOMParser iParser = new DOMParser();

        try {
            iParser.parse(new InputSource(new FileInputStream(CONFIG_FILE)));
            iParser.getDocument().getDocumentElement().setAttribute("year",
                    iYearId == null ? "" : iYearId.toString());

            boolean iExists = false;
            NodeList iCompanyElements = iParser.getDocument().getDocumentElement().getElementsByTagName(
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
                Element iCompanyElement = iParser.getDocument().createElement("company");

                iCompanyElement.setAttribute("id", pCompanyId.toString());
                iCompanyElement.setAttribute("yearid", iId == null ? "" : iId.toString());
                iParser.getDocument().getDocumentElement().appendChild(iCompanyElement);
            }

            // Write back the database path to the config file.
            OutputFormat iFormat = new OutputFormat(iParser.getDocument());
            XMLSerializer serializer = new XMLSerializer(new FileOutputStream(CONFIG_FILE),
                    iFormat);

            serializer.serialize(iParser.getDocument());

        } catch (IOException | SAXException ex) {
            LOG.error("Unexpected error", ex);
        }
    }

    public static Optional<SSNewAccountingYear> loadCompanySetting(Integer pCompanyId) {
        if (pCompanyId == null) {
            return Optional.empty();
        }
        DOMParser iParser = new DOMParser();

        try {
            iParser.parse(new InputSource(new FileInputStream(CONFIG_FILE)));
            NodeList iCompanyElements = iParser.getDocument().getDocumentElement().getElementsByTagName(
                    "company");

            for (int i = 0; i < iCompanyElements.getLength(); i++) {
                Node iCompanyNode = iCompanyElements.item(i);

                if (iCompanyNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element iCompanyElement = (Element) iCompanyNode;
                    Integer iCompanyElementId = Integer.parseInt(
                            iCompanyElement.getAttribute("id"));

                    if (iCompanyElementId.equals(pCompanyId)) {
                        String iResult = iCompanyElement.getAttribute("yearid");

                        if (iResult == null || iResult.length() == 0) {
                            return Optional.empty();
                        }
                        SSNewAccountingYear iYear = new SSNewAccountingYear();

                        iYear.setId(Integer.parseInt(iResult));
                        return SSDB.getInstance().getAccountingYear(iYear);
                    }
                }
            }

        } catch (IOException | SAXException ex) {
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
            parent.mkdirs();
        }
        if (CONFIG_FILE.createNewFile()) {
            LOG.info("Creating database config file.");
            
            XMLSerializer serializer = new XMLSerializer(new FileOutputStream(CONFIG_FILE),
                new OutputFormat("XML", "UTF-8", true));

            try {
                serializer.startDocument();
                serializer.startElement("database", new AttributeListImpl());
                serializer.endElement("database");
                serializer.endDocument();
//                serializer.serialize(new );
            } catch (SAXException ex) {
            }
        }
    }

    /**
     *
     */
    public static void load() {

        DOMParser iParser = new DOMParser();

        try {
            createIfNotExists();
            
            // parser.set(false)
            iParser.parse(new InputSource(new FileInputStream(CONFIG_FILE)));

            String iCompany = null;

            if (iParser.getDocument().getDocumentElement().hasAttribute("company")) {
                iCompany = iParser.getDocument().getDocumentElement().getAttribute(
                        "company");
            }
            if (iCompany != null && iCompany.length() != 0) {
                iCompanyId = Integer.parseInt(iCompany);
            }

            String iYear = null;

            if (iParser.getDocument().getDocumentElement().hasAttribute("year")) {
                iYear = iParser.getDocument().getDocumentElement().getAttribute("year");
            }
            if (iYear != null && iYear.length() != 0) {
                iYearId = Integer.parseInt(iYear);
            }

        } catch (IOException | SAXException ex) {
            LOG.error("Unexpected error", ex);
        }
    }
}
