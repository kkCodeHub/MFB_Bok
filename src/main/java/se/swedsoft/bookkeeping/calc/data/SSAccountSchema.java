package se.swedsoft.bookkeeping.calc.data;


import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import se.swedsoft.bookkeeping.data.SSAccountPlanType;
import se.swedsoft.bookkeeping.data.SSNewAccountingYear;

import java.io.IOException;
import java.io.InputStream;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Date: 2006-feb-27
 * Time: 12:48:13
 */
public class SSAccountSchema  {    private static final Logger LOG = LoggerFactory.getLogger(SSAccountSchema.class);


    private final List<SSAccountGroup> iResultGroups;

    private final List<SSAccountGroup> iBalanceGroups;

    /**
     *
     */
    private SSAccountSchema() {
        iResultGroups = new LinkedList<>();
        iBalanceGroups = new LinkedList<>();
    }

    /**
     * Returns the result groups
     * @return result groups in schema order
     */
    public List<SSAccountGroup> getResultGroups() {
        return iResultGroups;
    }

    /**
     * Returns the balance groups
     * @return balance groups in schema order
     */
    public List<SSAccountGroup> getBalanceGroups() {
        return iBalanceGroups;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append("ResultGroups: {\n");
        for (SSAccountGroup iLevelOne : iResultGroups) {
            sb.append(iLevelOne);
        }
        sb.append("}\n");

        sb.append("BalanceGroups: {\n");
        for (SSAccountGroup iLevelOne : iBalanceGroups) {
            sb.append(iLevelOne);
        }
        sb.append("}\n");

        return sb.toString();
    }

    private static final Map<String, SSAccountSchema> iSchemaCache = new HashMap<>();

    /**
     * Returns a schema loaded from a classpath resource.
     *
     * @param iSchema the schema resource name
     * @return cached or newly loaded schema instance
     */
    public static SSAccountSchema getAccountSchema(String iSchema) {
        SSAccountSchema iAccountSchema;

        if (iSchemaCache.containsKey(iSchema)) {
            iAccountSchema = iSchemaCache.get(iSchema);
        } else {
            InputStream is = SSAccountSchema.class.getResourceAsStream(
                    "/account/" + iSchema);

            iAccountSchema = createAccountSchema(is);
            iSchemaCache.put(iSchema, iAccountSchema);
        }

        return iAccountSchema;
    }

    /**
     * Returns the schema for the provided accounting year.
     *
     * @param pYearData accounting year configuration
     * @return matching account schema
     */
    public static SSAccountSchema getAccountSchema(SSNewAccountingYear pYearData) {
        String iSchema = SSAccountPlanType.getDefault().getSchema();

        if (pYearData != null && pYearData.getAccountPlan() != null
                && pYearData.getAccountPlan().getType() != null) {
            iSchema = pYearData.getAccountPlan().getType().getSchema();
        }

        return getAccountSchema(iSchema);
    }

    /**
     * Loads an account schema from the classpath.
     *
     * @param is the schema stream
     * @return the parsed schema
     */
    private static SSAccountSchema createAccountSchema(InputStream is) {
        SSAccountSchema iSchema = new SSAccountSchema();

        XMLReader iReader;

        try {
            SAXParserFactory iFactory = SAXParserFactory.newInstance();
            SAXParser iParser = iFactory.newSAXParser();
            iReader = iParser.getXMLReader();
        } catch (ParserConfigurationException | SAXException e) {
            LOG.error("Unexpected error", e);
            return iSchema;
        }

        iReader.setContentHandler(new AccountGroupLoader(iSchema));

        try {
            iReader.parse(new InputSource(is));
        } catch (SAXException | IOException ex) {
            LOG.error("Unexpected error", ex);
        }
        return iSchema;
    }

    /**
     *
     */
    private static class AccountGroupLoader extends DefaultHandler {

        private final SSAccountSchema iSchema;

        private List<SSAccountGroup> iLevelOne;

        private final Stack<SSAccountGroup> iLevelTwo;

        /**
         * Creates a SAX loader for account groups.
         *
         * @param pSchema target schema to populate
         */
        public AccountGroupLoader(SSAccountSchema pSchema) {
            iSchema = pSchema;
            iLevelOne = null;
            iLevelTwo = new Stack<>();
        }

        /**
         * Creates a group from XML attributes.
         *
         * @param iAttributes the source attributes
         * @return the created group
         */
        private SSAccountGroup createGroup(Attributes iAttributes) {
            String iId = iAttributes.getValue("id");
            String iBundle = iAttributes.getValue("bundle");
            String iFromAccount = iAttributes.getValue("fromAccount");
            String iToAccount = iAttributes.getValue("toAccount");

            SSAccountGroup iAccountGroup = new SSAccountGroup();

            iAccountGroup.setBundle(iBundle);
            iAccountGroup.setId(Integer.decode(iId));

            if (iFromAccount != null) {
                iAccountGroup.setFromAccount(Integer.decode(iFromAccount));
            }
            if (iToAccount != null) {
                iAccountGroup.setToAccount(Integer.decode(iToAccount));
            }

            add(iAccountGroup);

            return iAccountGroup;
        }

        /**
         * Adds a group to the current tree level.
         *
         * @param pGroup the group to add
         */
        private void add(SSAccountGroup pGroup) {
            if (iLevelTwo.isEmpty()) {
                iLevelOne.add(pGroup);
            } else {
                iLevelTwo.peek().addAccountGroup(pGroup);
            }
        }

        private String getElementName(String localName, String qName) {
            return (localName != null && !localName.isEmpty()) ? localName : qName;
        }

        /**
         * Handles the start of an XML element.
         *
         * @param uri the namespace URI
         * @param localName the local name
         * @param qName the qualified name
         * @param iAttributes the element attributes
         */
        @Override
        public void startElement(String uri, String localName, String qName, Attributes iAttributes) {
            String iElementName = getElementName(localName, qName);

            if ("result".equalsIgnoreCase(iElementName)) {
                iLevelOne = iSchema.iResultGroups;
            }
            if ("balance".equalsIgnoreCase(iElementName)) {
                iLevelOne = iSchema.iBalanceGroups;
            }

            if ("group".equalsIgnoreCase(iElementName)) {
                iLevelTwo.push(createGroup(iAttributes));
            }

        }

        /**
         * Handles the end of an XML element.
         *
         * @param uri the namespace URI
         * @param localName the local name
         * @param qName the qualified name
         */
        @Override
        public void endElement(String uri, String localName, String qName) {
            String iElementName = getElementName(localName, qName);

            if ("result".equalsIgnoreCase(iElementName)) {
                iLevelOne = null;
            }
            if ("balance".equalsIgnoreCase(iElementName)) {
                iLevelOne = null;
            }
            if ("group".equalsIgnoreCase(iElementName)) {
                iLevelTwo.pop();
            }

        }

        @Override
        public String toString() {
            return "se.swedsoft.bookkeeping.calc.data.SSAccountSchema.AccountGroupLoader"
                    + "{iLevelOne=" + iLevelOne
                    + ", iLevelTwo=" + iLevelTwo
                    + ", iSchema=" + iSchema
                    + '}';
        }
    }
}
