package se.swedsoft.bookkeeping.data.system;


import com.fasterxml.jackson.databind.JsonNode;
import org.fribok.bookkeeping.app.Path;
import se.swedsoft.bookkeeping.calc.math.*;
import se.swedsoft.bookkeeping.data.*;
import se.swedsoft.bookkeeping.data.common.*;
import se.swedsoft.bookkeeping.gui.SSMainFrame;
import se.swedsoft.bookkeeping.gui.util.dialogs.SSErrorDialog;
import se.swedsoft.bookkeeping.persistence.Repositories;
import se.swedsoft.bookkeeping.persistence.v2.schema.SSSchemaBuilder;
import se.swedsoft.bookkeeping.importexport.excel.SSAccountPlanDefaultResourceDiscovery;
import se.swedsoft.bookkeeping.importexport.excel.SSAccountPlanImporter;
import se.swedsoft.bookkeeping.importexport.excel.SSAccountPlanLoader;
import se.swedsoft.bookkeeping.importexport.util.SSImportException;
import se.swedsoft.bookkeeping.util.SSUtil;
import se.swedsoft.bookkeeping.data.system.trigger.SSTriggerSchemaService;

import java.beans.PropertyChangeListener;
import java.io.*;
import java.math.BigDecimal;
import java.sql.*;
import java.util.*;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class SSDB {
    private static final Logger LOG = LoggerFactory.getLogger(SSDB.class);

    private static final String SCHEMA_VERSION_PROPERTY = "fribok.schema.version";
    private static final String SCHEMA_V2 = "v2";
    private static final String DEFAULT_SCHEMA_NAME = "PUBLIC";
    private static final String DEMO_SCHEMA_NAME = "co_0";
    private static final String DEMO_COMPANY_NAME = "Demoföretaget";
    private static final String SEED_STATE_TABLE = "PUBLIC.tbl_seed_state";
    private static final String SEED_PUBLIC_FILE = "seed/Seed_Public.json";
    private static final String SEED_COMPANY_FILE = "seed/Seed_Company_Demo.json";
    private static final String SEED_VER_FAKT_FILE = "seed/Seed_Demo_VerFakt.json";

    private static SSDB cInstance;

    public static final Object iSyncObject = new Object();

    private SSNewCompany iCurrentCompany;
    private SSNewAccountingYear iCurrentYear;

    List<SSProduct> iProducts;
    List<SSCustomer> iCustomers;
    List<SSSupplier> iSuppliers;
    List<SSAutoDist> iAutoDists;

    List<SSInpayment> iInpayments;
    List<SSTender> iTenders;
    List<SSOrder> iOrders;
    List<SSInvoice> iInvoices;
    List<SSCreditInvoice> iCreditInvoices;
    List<SSPeriodicInvoice> iPeriodicInvoices;

    List<SSOutpayment> iOutpayments;
    List<SSPurchaseOrder> iPurchaseOrders;
    List<SSSupplierInvoice> iSupplierInvoices;
    List<SSSupplierCreditInvoice> iSupplierCreditInvoices;

    List<SSInventory> iInventories;
    List<SSIndelivery> iIndeliveries;
    List<SSOutdelivery> iOutdeliveries;

    List<SSVoucher> iVouchers;
    List<SSOwnReport> iOwnReports;

    private final SSDBEventBus iEventBus = new SSDBEventBus();
    private final SSTriggerSchemaService iTriggerSchemaService;

    private Connection iConnection;

    public static SSDB getInstance() {
        if (cInstance == null) {
            cInstance = new SSDB();
        }
        return cInstance;
    }

    private SSDB() {
        iTriggerSchemaService = new SSTriggerSchemaService(() -> iConnection);
    }

    public void startupLocal(Connection pConnection) throws SQLException {
 //       LOG.info("KK startupLocal, SSDB.java");
        prepareStartupConnection(pConnection);
        failIfLegacySingleSchemaDatabase();
        createNewTables();
        Repositories.init(this);
        checkImportDefaultAccountPlans();
        ensureCatalogBootstrapForCurrentSchema();
        ensureDemoSeedIsRun();
        validateSchemaContract();
        iTriggerSchemaService.rebuildLocalTriggers();
        initializeCurrentCompanyAndYear();
        logStartupV2Mode();
    }

    void prepareStartupConnection(Connection pConnection) throws SQLException {
        iConnection = pConnection;
        iConnection.setAutoCommit(false);

        // SSDB is a singleton across test classes and app lifecycle; clear any
        // company/year state tied to a previous DB connection.
        iCurrentCompany = null;
        iCurrentYear = null;
        clearCachedLists();

        // V2-only mode: force schema selection to V2 for all runtimes/tests.
        String iDetectedSchemaVersion = SCHEMA_V2;
        System.setProperty(SCHEMA_VERSION_PROPERTY, SCHEMA_V2);
    }

    public void initializeCurrentCompanyAndYear() {
        Repositories.companies().applyActiveCompanySchema();
        Optional<String> iActiveCatalogCompanyName = Repositories.companies().findActiveCompanyName();

        List<SSNewCompany> iCompanies = loadCompanies();
        if (iCompanies.isEmpty()) {
            setCurrentCompany(null);
            setCurrentYear(null);
            return;
        }

        if (!iActiveCatalogCompanyName.isPresent()) {
            setCurrentCompany(null);
            setCurrentYear(null);
            return;
        }

        if (iCurrentCompany == null || Repositories.companies().findById(iCurrentCompany).isEmpty()) {
            SSNewCompany iSelectedCompany;
            String iCatalogCompanyName = iActiveCatalogCompanyName.get();
            iSelectedCompany = Repositories.companies().findByName(iCatalogCompanyName).orElse(iCompanies.get(0));
            if (!Objects.equals(iCatalogCompanyName, iSelectedCompany.getName())) {
                showUiWarning("Företagsnamn avviker mellan katalog och företagsdata. Kontrollera företagsuppgifter.");
            }
            setCurrentCompany(iSelectedCompany);
        }

        if (iCurrentYear == null && iCurrentCompany != null) {
            Optional<SSNewAccountingYear> iDemoYear = getAccountingYearByRangeV2(
                    iCurrentCompany,
                    java.time.LocalDate.of(2025, 1, 1),
                    java.time.LocalDate.of(2025, 12, 31));

            if (iDemoYear.isPresent()) {
                openYear(iDemoYear.get());
            } else {
                loadYearsForCompany(iCurrentCompany).stream()
                        .max(Comparator.comparing(SSNewAccountingYear::getLocalTo))
                        .ifPresent(this::openYear);
            }
        }
    }

    private void seedDemoEntitiesFromJson() {
        try {
            setSchema(DEMO_SCHEMA_NAME);
            JsonNode seedRoot = SSJsonSeedDataLoader.loadSeedFile("seed/Seed_Demo.json");
            seedCustomers(seedRoot);
            seedSuppliers(seedRoot);
            seedProducts(seedRoot);
            LOG.info("Successfully seeded demo customers, suppliers and products");
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read seed/Seed_Demo.json", e);
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to set schema " + DEMO_SCHEMA_NAME + " for demo seed", e);
        }
        seedVouchersAndInvoicesFromJson();
    }

    private void seedVouchersAndInvoicesFromJson() {
        try {
            JsonNode seedRoot = SSJsonSeedDataLoader.loadSeedFile(SEED_VER_FAKT_FILE);
            seedVouchers(seedRoot);
            seedInvoices(seedRoot);
            LOG.info("Successfully seeded demo vouchers and invoices from {}", SEED_VER_FAKT_FILE);
        } catch (IOException e) {
            LOG.warn("Could not read seed file '{}': {}", SEED_VER_FAKT_FILE, e.getMessage());
        }
    }

    /**
     * Seeds vouchers from the "Verifikationer" array in the given JSON node.
     * The voucher date is adjusted to the accounting year if the year part does not match.
     * If no accounting year exists, vouchers are skipped.
     *
     * @param seedRoot the parsed JSON root
     */
    private void seedVouchers(JsonNode seedRoot) {
        List<SSNewAccountingYear> years = loadYearsForCompany(iCurrentCompany);
        if (years.isEmpty()) {
            LOG.warn("No accounting years found for company '{}'; skipping voucher seed",
                    iCurrentCompany != null ? iCurrentCompany.getName() : "null");
            return;
        }
        SSNewAccountingYear seedYear = years.stream()
                .filter(y -> y.getLocalTo() != null)
                .max((a, b) -> a.getLocalTo().compareTo(b.getLocalTo()))
                .orElse(years.get(0));

        for (JsonNode verNode : SSJsonSeedDataLoader.getArrayObjects(seedRoot, "Verifikationer")) {
            try {
                java.time.LocalDate rawDate = requiredDate(verNode, "Datum");
                java.time.LocalDate voucherDate = adjustDateToYear(rawDate, seedYear);

                SSVoucher voucher = new SSVoucher();
                voucher.setLocalDate(voucherDate);
                voucher.setDescription(requiredText(verNode, "Beskrivning"));

                for (JsonNode rowNode : SSJsonSeedDataLoader.getArrayObjects(verNode, "Rader")) {
                    SSVoucherRow row = new SSVoucherRow();
                    row.setAccountNr(rowNode.has("Konto") ? rowNode.get("Konto").asInt() : null);
                    row.setDebet(optionalBigDecimal(rowNode, "Debet"));
                    row.setCredit(optionalBigDecimal(rowNode, "Kredit"));
                    voucher.addVoucherRow(row);
                }

                Repositories.vouchers().addWithAutoNumber(voucher);
            } catch (Exception e) {
                LOG.warn("Skipping voucher '{}': {}",
                        SSJsonSeedDataLoader.getStringFieldOrNull(verNode, "Beskrivning"), e.getMessage());
            }
        }
    }

    /**
     * Seeds invoices from the "Fakturor" array in the given JSON node.
     * Customer and product positions are 1-based row numbers in their respective tables.
     * Invoice date uses the accounting year's start year with today's month and day.
     * Payment term is the second entry in tbl_paymentterm.
     * Duplicates are accepted; each seed run creates new invoices with auto-assigned numbers.
     *
     * @param seedRoot the parsed JSON root
     */
    private void seedInvoices(JsonNode seedRoot) {
        List<SSCustomer> customers = Repositories.customers().findAll();
        List<SSProduct> products = Repositories.products().findAll();

        List<se.swedsoft.bookkeeping.data.common.SSPaymentTerm> paymentTerms =
                Repositories.paymentTerms().findAll();
        se.swedsoft.bookkeeping.data.common.SSPaymentTerm paymentTerm =
                paymentTerms.size() >= 2 ? paymentTerms.get(1)
                : !paymentTerms.isEmpty() ? paymentTerms.get(0)
                : null;

        List<SSNewAccountingYear> years = loadYearsForCompany(iCurrentCompany);
        SSNewAccountingYear seedYear = years.stream()
                .filter(y -> y.getLocalTo() != null)
                .max((a, b) -> a.getLocalTo().compareTo(b.getLocalTo()))
                .orElse(years.isEmpty() ? null : years.get(0));

        java.time.LocalDate invoiceDate = buildInvoiceDate(seedYear);
        java.time.LocalDate dueDate = paymentTerm != null
                ? paymentTerm.addDaysToLocalDate(invoiceDate)
                : invoiceDate;

        for (JsonNode fakturaNode : SSJsonSeedDataLoader.getArrayObjects(seedRoot, "Fakturor")) {
            try {
                int customerPos = fakturaNode.get("Kund-id").asInt();
                if (customerPos < 1 || customerPos > customers.size()) {
                    LOG.warn("Skipping invoice: Kund-id {} out of range (available customers: {})",
                            customerPos, customers.size());
                    continue;
                }
                SSCustomer customer = customers.get(customerPos - 1);

                SSInvoice invoice = new SSInvoice(se.swedsoft.bookkeeping.data.common.SSInvoiceType.NORMAL);
                invoice.setVoucher(null);
                invoice.setLocalDate(invoiceDate);
                invoice.setLocalDueDate(dueDate);
                invoice.setPaymentTerm(paymentTerm);
                invoice.setCustomerNr(customer.getNumber());
                invoice.setCustomerName(customer.getName());
                invoice.setOurContactPerson(customer.getOurContactPerson());
                invoice.setYourContactPerson(customer.getYourContactPerson());
                invoice.setInvoiceAddress(customer.getInvoiceAddress());
                invoice.setDeliveryAddress(customer.getDeliveryAddress());

                for (JsonNode rowNode : SSJsonSeedDataLoader.getArrayObjects(fakturaNode, "Rader")) {
                    int productPos = rowNode.get("Produktnr").asInt();
                    if (productPos < 1 || productPos > products.size()) {
                        LOG.warn("Skipping invoice row: Produktnr {} out of range (available products: {})",
                                productPos, products.size());
                        continue;
                    }
                    SSProduct product = products.get(productPos - 1);
                    se.swedsoft.bookkeeping.data.base.SSSaleRow row =
                            new se.swedsoft.bookkeeping.data.base.SSSaleRow(product);
                    row.setQuantity(rowNode.get("Antal").asInt() * 10);
                    invoice.getRows().add(row);
                }

                if (!invoice.getRows().isEmpty()) {
                    Repositories.invoices().add(invoice);
                }
            } catch (Exception e) {
                LOG.warn("Skipping invoice node: {}", e.getMessage());
            }
        }
    }

    /**
     * Adjusts the year part of the given date to match the accounting year's start year.
     * Month and day are preserved. If the resulting date is invalid (e.g. Feb 29 in a non-leap year),
     * the day is clamped to the first of the month.
     *
     * @param date the original date
     * @param year the target accounting year
     * @return a date within the accounting year
     */
    private java.time.LocalDate adjustDateToYear(java.time.LocalDate date, SSNewAccountingYear year) {
        if (year == null || year.getLocalFrom() == null) {
            return date;
        }
        int targetYear = year.getLocalFrom().getYear();
        if (date.getYear() == targetYear) {
            return date;
        }
        try {
            return java.time.LocalDate.of(targetYear, date.getMonthValue(), date.getDayOfMonth());
        } catch (java.time.DateTimeException e) {
            return java.time.LocalDate.of(targetYear, date.getMonthValue(), 1);
        }
    }

    /**
     * Builds the invoice date using the accounting year's start year combined with
     * today's month and day. Falls back to today if no accounting year is available.
     *
     * @param year the accounting year to derive the year component from
     * @return the computed invoice date
     */
    private java.time.LocalDate buildInvoiceDate(SSNewAccountingYear year) {
        java.time.LocalDate today = java.time.LocalDate.now();
        if (year == null || year.getLocalFrom() == null) {
            return today;
        }
        int targetYear = year.getLocalFrom().getYear();
        try {
            return java.time.LocalDate.of(targetYear, today.getMonthValue(), today.getDayOfMonth());
        } catch (java.time.DateTimeException e) {
            return java.time.LocalDate.of(targetYear, today.getMonthValue(), 1);
        }
    }

    /**
     * Reads an optional BigDecimal field from the given JSON node.
     * Returns {@code null} if the field is absent or unparseable.
     *
     * @param node      the JSON object node
     * @param fieldName the field to read
     * @return the value as BigDecimal, or {@code null}
     */
    private BigDecimal optionalBigDecimal(JsonNode node, String fieldName) {
        if (node == null || !node.has(fieldName)) {
            return null;
        }
        String raw = node.get(fieldName).asText();
        try {
            return new BigDecimal(raw.replace(',', '.'));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void seedCustomers(JsonNode seedRoot) {
        for (JsonNode customerNode : SSJsonSeedDataLoader.getArrayObjects(seedRoot, "Kunder")) {
            String number = requiredText(customerNode, "Kund-id");
            Optional<SSCustomer> existingCustomer = Repositories.customers().findByNumber(number);
            SSCustomer customer = existingCustomer.orElseGet(SSCustomer::new);
            customer.setNumber(number);
            customer.setName(requiredText(customerNode, "Namn"));
            customer.setYourContactPerson(optionalText(customerNode, "Er kontaktperson"));
            customer.setEMail(optionalText(customerNode, "E-post"));
            applyInvoiceAddress(customer, customerNode);

            if (existingCustomer.isPresent()) {
                Repositories.customers().update(customer);
            } else {
                Repositories.customers().add(customer);
            }
        }
    }

    private void seedSuppliers(JsonNode seedRoot) {
        for (JsonNode supplierNode : SSJsonSeedDataLoader.getArrayObjects(seedRoot, "Leverantörer")) {
            String number = requiredText(supplierNode, "Leverantörs-id");
            SSSupplier probe = new SSSupplier();
            probe.setNumber(number);
            Optional<SSSupplier> existingSupplier = Repositories.suppliers().findBySupplier(probe);
            SSSupplier supplier = existingSupplier.orElseGet(SSSupplier::new);
            supplier.setNumber(number);
            supplier.setName(requiredText(supplierNode, "Namn"));
            supplier.setYourContact(optionalText(supplierNode, "Er kontaktperson"));
            supplier.setEMail(optionalText(supplierNode, "E-post"));
            applySupplierAddress(supplier, supplierNode);

            if (existingSupplier.isPresent()) {
                Repositories.suppliers().update(supplier);
            } else {
                Repositories.suppliers().add(supplier);
            }
        }
    }

    private void seedProducts(JsonNode seedRoot) {
        for (JsonNode productNode : SSJsonSeedDataLoader.getArrayObjects(seedRoot, "Produkter")) {
            String number = requiredText(productNode, "Produktnummer");
            Optional<SSProduct> existingProduct = Repositories.products().findByNumber(number);
            SSProduct product = existingProduct.orElseGet(SSProduct::new);
            product.setNumber(number);
            product.setDescription(requiredText(productNode, "Beskrivning"));
            product.setSellingPrice(requiredBigDecimal(productNode, "Försäljningspris"));

            if (existingProduct.isPresent()) {
                Repositories.products().update(product);
            } else {
                Repositories.products().add(product);
            }
        }
    }

    private void applyInvoiceAddress(SSCustomer customer, JsonNode node) {
        SSAddress address = customer.getInvoiceAddress();
        if (address == null) {
            address = new SSAddress();
            customer.setInvoiceAddress(address);
        }
        boolean hasAddress = false;
        String address1 = optionalText(node, "Adress 1");
        String address2 = optionalText(node, "Adress 2");
        String zipCode = optionalText(node, "Postnummer");
        String city = optionalText(node, "Ort");
        String country = optionalText(node, "Land");
        String name = optionalText(node, "Adressnamn");

        if (name != null) {
            address.setName(name);
            hasAddress = true;
        }
        if (address1 != null) {
            address.setAddress1(address1);
            hasAddress = true;
        }
        if (address2 != null) {
            address.setAddress2(address2);
            hasAddress = true;
        }
        if (zipCode != null) {
            address.setZipCode(zipCode);
            hasAddress = true;
        }
        if (city != null) {
            address.setCity(city);
            hasAddress = true;
        }
        if (country != null) {
            address.setCountry(country);
            hasAddress = true;
        }
        if (!hasAddress) {
            customer.setInvoiceAddress(address);
        }
    }

    private void applySupplierAddress(SSSupplier supplier, JsonNode node) {
        SSAddress address = supplier.getAddress();
        if (address == null) {
            address = new SSAddress();
            supplier.setAddress(address);
        }
        boolean hasAddress = false;
        String address1 = optionalText(node, "Adress 1");
        String address2 = optionalText(node, "Adress 2");
        String zipCode = optionalText(node, "Postnummer");
        String city = optionalText(node, "Ort");
        String country = optionalText(node, "Land");
        String name = optionalText(node, "Adressnamn");

        if (name != null) {
            address.setName(name);
            hasAddress = true;
        }
        if (address1 != null) {
            address.setAddress1(address1);
            hasAddress = true;
        }
        if (address2 != null) {
            address.setAddress2(address2);
            hasAddress = true;
        }
        if (zipCode != null) {
            address.setZipCode(zipCode);
            hasAddress = true;
        }
        if (city != null) {
            address.setCity(city);
            hasAddress = true;
        }
        if (country != null) {
            address.setCountry(country);
            hasAddress = true;
        }
        if (!hasAddress) {
            supplier.setAddress(address);
        }
    }

    private BigDecimal requiredBigDecimal(JsonNode node, String fieldName) {
        if (node == null || !node.has(fieldName)) {
            throw new IllegalStateException("Missing decimal field '" + fieldName + "' in seed JSON.");
        }
        String raw = node.get(fieldName).asText();
        try {
            return new BigDecimal(raw.replace(',', '.'));
        } catch (NumberFormatException e) {
            throw new IllegalStateException("Invalid decimal '" + raw + "' in field '" + fieldName + "'.", e);
        }
    }

    void logStartupV2Mode() {
        LOG.info("startupLocal running in schema V2 mode; skipping legacy last-company restore");
    }

    public void init(boolean iShowDialog) {
        if (iCurrentCompany == null) {
            return;
        }

        loadProducts();
        loadCustomers();
        loadSuppliers();
        loadAutoDists();

        loadInpayments();
        loadTenders();
        loadOrders();
        loadInvoices();
        loadCreditInvoices();
        loadPeriodicInvoices();

        loadOutpayments();
        loadPurchaseOrders();
        loadSupplierInvoices();
        loadSupplierCreditInvoices();

        loadInventories();
        loadIndeliveries();
        loadOutdeliveries();

        loadOwnReports();

        SSInvoiceMath.iSaldoMap = null;
        SSInvoiceMath.calculateSaldos();
        SSCustomerMath.iInvoicesForCustomers = null;
        SSCustomerMath.getInvoicesForCustomers();
        SSSupplierInvoiceMath.iSaldoMap = null;
        SSSupplierInvoiceMath.calculateSaldos();
        SSSupplierMath.iInvoicesForSuppliers = null;
        SSSupplierMath.getInvoicesForSuppliers();
        // SSOrderMath.setInvoiceForOrders();
        initYear(false);

    }

    public void initYear(boolean iShowLoadingDialog) {
        if (iCurrentYear == null) {
            return;
        }

        iVouchers = null;
        getCurrentYear();
        loadVouchers();

    }

    public void shutdown() {
        try {
            if (!iConnection.isClosed()) {
                try (Statement iStatement = iConnection.createStatement()) {
                    iStatement.executeQuery("SHUTDOWN");
                }
                iConnection.close();
            }
        } catch (SQLException e) {
            LOG.error("Unexpected error", e);
        }
    }

    public void shutdownCompact() {
        try {
            try (Statement iStatement = iConnection.createStatement()) {
                iStatement.executeQuery("SHUTDOWN COMPACT");
            }
            iConnection.close();
        } catch (SQLException e) {
            LOG.error("Unexpected error", e);
        }
    }

    public void loadLocalDatabase() {
        // loadLocalDatabase enbart använd vid inläsning av säkerhetsbackup.
//        LOG.info("KK loadLocalDatabase, SSDB.java");
        try {
            if (iConnection != null) {
                iConnection.close();
            }
        } catch (SQLException e) {
            LOG.error("Unexpected error", e);
        }
        try {
            Class.forName("org.hsqldb.jdbcDriver");
        } catch (ClassNotFoundException e) {
            LOG.info("ERROR: failed to load HSQLDB JDBC driver.");
            LOG.error("Unexpected error", e);
            return;
        }

        try {
            File dbDir = new File(Path.get(Path.USER_DATA), "db");
            iConnection = DriverManager.getConnection(
                    "jdbc:hsqldb:file:" + dbDir.getAbsolutePath() + File.separator + "JFSDB", "sa", "");
            iConnection.setAutoCommit(false);
            failIfLegacySingleSchemaDatabase();
            createNewTables();
            //  Inlagda metoder för att testa inläsning av säkerhetsbackup ab DB.
            Repositories.init(this);
            checkImportDefaultAccountPlans();
            ensureCatalogBootstrapForCurrentSchema();
            validateSchemaContract();
            iTriggerSchemaService.rebuildLocalTriggers();
            initializeCurrentCompanyAndYear();
            logStartupV2Mode();

        } catch (SQLException e) {
            LOG.error("Unexpected error", e);
        }
    }

    private void ensureDemoSeedIsRun() {
        try {
            if (iConnection == null || iConnection.isClosed()) {
                return;
            }

            boolean seedDone = isSeedAlreadyDone();
            if (seedDone) {
                return;
            }

            Optional<String> demoSchemaName = loadActiveCatalogSchemaName();
            if (!demoSchemaName.isPresent() || !demoSchemaName.get().equals(DEMO_SCHEMA_NAME)) {
                return;
            }

            // Seed PUBLIC schema tables first (Currency, Unit, PaymentTerm, DeliveryTerm, DeliveryWay)
            seedPublicTables();

            // Then seed demo company + year + account plan in co_0 from JSON
            seedDemoCompanyAndAccountingYear();

            // Seed demo customers, suppliers and products from JSON.
            seedDemoEntitiesFromJson();

            setSeedDone(true);
            iConnection.commit();
        } catch (SQLException e) {
            LOG.error("Unexpected error during demo seed", e);
            try {
                iConnection.rollback();
            } catch (SQLException ignored) {}
        } catch (Exception e) {
            LOG.error("Unexpected error during JSON seeding", e);
            try {
                iConnection.rollback();
            } catch (SQLException ignored) {}
        }
    }

    private boolean isSeedAlreadyDone() throws SQLException {
        try (PreparedStatement iStatement = iConnection.prepareStatement(
                "SELECT seed_done FROM " + SEED_STATE_TABLE)) {
            try (ResultSet iResultSet = iStatement.executeQuery()) {
                if (iResultSet.next()) {
                    return iResultSet.getBoolean("seed_done");
                }
            }
        }
        return false;
    }

    private Optional<SSNewAccountingYear> getAccountingYearByRangeV2(
            SSNewCompany iCompany,
            java.time.LocalDate iFrom,
            java.time.LocalDate iTo) {
        if (iCompany == null || iFrom == null || iTo == null || iConnection == null) {
            return Optional.empty();
        }

        try (PreparedStatement iStatement = iConnection.prepareStatement(
                "SELECT * FROM tbl_accountingyear WHERE companyid=? AND from_date=? AND to_date=?")) {
            iStatement.setObject(1, iCompany.getId());
            iStatement.setObject(2, java.sql.Date.valueOf(iFrom));
            iStatement.setObject(3, java.sql.Date.valueOf(iTo));

            try (ResultSet iResultSet = iStatement.executeQuery()) {
                if (iResultSet.next()) {
                    return Optional.of(Repositories.accountingYears().mapAccountingYear(iResultSet));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            LOG.error("Unexpected error", e);
            return Optional.empty();
        }
    }

    private void seedDemoCompanyAndAccountingYear() {
        try {
            setSchema(DEMO_SCHEMA_NAME);
            JsonNode seedRoot = SSJsonSeedDataLoader.loadSeedFile(SEED_COMPANY_FILE);
            JsonNode companyNode = requiredObject(seedRoot, "Företag");

            SSNewCompany seededCompany = ensureDemoCompanyInCo0(companyNode);
            setCurrentCompany(seededCompany);

            JsonNode yearNode = requiredObject(companyNode, "Bokföringsår");
            java.time.LocalDate fromDate = requiredDate(yearNode, "Från");
            java.time.LocalDate toDate = requiredDate(yearNode, "Till");
            if (toDate.isBefore(fromDate)) {
                throw new IllegalStateException("Invalid Bokföringsår in " + SEED_COMPANY_FILE + ": Till before Från.");
            }

            if (getAccountingYearByRangeV2(seededCompany, fromDate, toDate).isPresent()) {
                return;
            }

            SSAccountPlan templatePlan = resolveSeedAccountPlan(companyNode);
            SSAccountPlan yearPlan = createSeedYearPlan(templatePlan, seededCompany.getName(), fromDate);

            SSNewAccountingYear accountingYear = new SSNewAccountingYear();
            accountingYear.setLocalFrom(fromDate);
            accountingYear.setLocalTo(toDate);
            accountingYear.setAccountPlan(yearPlan);
            Repositories.accountingYears().add(accountingYear);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read " + SEED_COMPANY_FILE, e);
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to seed demo company/year in schema " + DEMO_SCHEMA_NAME, e);
        }
    }

    private SSNewCompany ensureDemoCompanyInCo0(JsonNode companyNode) throws SQLException {
        String companyName = requiredText(companyNode, "Företagsnamn");
        String contactPerson = optionalText(companyNode, "Kontaktperson");
        String logotype = optionalText(companyNode, "Logotyp");

        SSNewCompany template = new SSNewCompany();
        template.setName(companyName);
        template.setContactPerson(contactPerson);
        template.setLogotype(logotype);

        return Repositories.companies().ensureCompanyInSchema(DEMO_SCHEMA_NAME, template);
    }

    private SSAccountPlan resolveSeedAccountPlan(JsonNode companyNode) throws IOException {
        String requestedPlanName = requiredText(companyNode, "Kontoplan");
        List<SSAccountPlan> availablePlans = new LinkedList<>(Repositories.accountPlans().findAll());
        availablePlans.sort(Comparator.comparing(SSAccountPlan::getId, Comparator.nullsLast(Integer::compareTo)));

        SSAccountPlan selectedPlan = null;
        for (SSAccountPlan plan : availablePlans) {
            if (plan != null && requestedPlanName.equals(plan.getName())) {
                selectedPlan = plan;
                break;
            }
        }
        if (selectedPlan == null && !availablePlans.isEmpty()) {
            selectedPlan = availablePlans.get(0);
            LOG.warn("Requested account plan '{}' not found. Falling back to first available plan '{}'.",
                    requestedPlanName, selectedPlan.getName());
        }
        if (selectedPlan == null) {
            throw new IllegalStateException("No account plans found in PUBLIC.tbl_accountplan. Seed cannot continue.");
        }
        if (selectedPlan.isTemplatePlan()) {
            return SSAccountPlanLoader.loadPlan(selectedPlan);
        }
        return selectedPlan;
    }

    private SSAccountPlan createSeedYearPlan(SSAccountPlan templatePlan, String companyName, java.time.LocalDate fromDate) {
        SSAccountPlan yearPlan = new SSAccountPlan(templatePlan);
        int startYear = fromDate.getYear();
        String safeCompanyName = companyName == null ? "" : companyName.trim();

        String planName = safeCompanyName.isEmpty()
                ? Integer.toString(startYear)
                : safeCompanyName + " " + startYear;
        yearPlan.setName(planName);
        yearPlan.setExcelPath(null);
        yearPlan.setDefaultPlan(false);

        String baseName = templatePlan.getName();
        if (baseName == null || baseName.trim().isEmpty()) {
            baseName = planName;
        }
        yearPlan.setBaseName(baseName);
        return yearPlan;
    }

    private JsonNode requiredObject(JsonNode node, String fieldName) {
        if (node == null || !node.has(fieldName) || !node.get(fieldName).isObject()) {
            throw new IllegalStateException("Missing object field '" + fieldName + "' in seed JSON.");
        }
        return node.get(fieldName);
    }

    private String requiredText(JsonNode node, String fieldName) {
        String value = optionalText(node, fieldName);
        if (value == null || value.isEmpty()) {
            throw new IllegalStateException("Missing text field '" + fieldName + "' in seed JSON.");
        }
        return value;
    }

    private String optionalText(JsonNode node, String fieldName) {
        String value = SSJsonSeedDataLoader.getStringFieldOrNull(node, fieldName);
        return value == null ? null : value.trim();
    }

    private java.time.LocalDate requiredDate(JsonNode node, String fieldName) {
        String raw = requiredText(node, fieldName);
        try {
            return java.time.LocalDate.parse(raw);
        } catch (Exception e) {
            throw new IllegalStateException("Invalid date '" + raw + "' in field '" + fieldName + "'.", e);
        }
    }

    private void executeSqlScriptResource(String iResourcePath) {
        String iCurrentStatement = null;
        try {
            String q = SSUtil.readResourceToString(iResourcePath);
            StringBuilder scriptBuilder = new StringBuilder();

            for (String line : q.split("\\r?\\n")) {
                if (!line.trim().startsWith("--")) {
                    scriptBuilder.append(line).append('\n');
                }
            }

            for (String statement : scriptBuilder.toString().split(";")) {
                String trimmed = statement.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }
                iCurrentStatement = trimmed;
                try (PreparedStatement iStatement = iConnection.prepareStatement(trimmed)) {
                    iStatement.executeUpdate();
                }
            }

            iConnection.commit();
            LOG.info("Executed SQL seed script: {}", iResourcePath);
        } catch (Exception e) {
            LOG.error("Failed SQL seed script '{}' near statement: {}", iResourcePath, iCurrentStatement, e);
            try {
                iConnection.rollback();
            } catch (SQLException ignored) {}
            throw new IllegalStateException(
                    "Failed SQL seed script '" + iResourcePath + "' near statement: " + iCurrentStatement,
                    e);
        }
    }

    private void logV2DemoSeedSummary() {
        try (PreparedStatement iStatement = iConnection.prepareStatement(
                "SELECT c.name AS cname, " +
                        "(SELECT COUNT(*) FROM tbl_customer cu WHERE cu.companyid=c.id) AS customer_count, " +
                        "(SELECT COUNT(*) FROM tbl_product p WHERE p.companyid=c.id) AS product_count, " +
                        "(SELECT COUNT(*) FROM tbl_supplier s WHERE s.companyid=c.id) AS supplier_count, " +
                        "(SELECT COUNT(*) FROM tbl_accountingyear y WHERE y.companyid=c.id AND y.from_date=? AND y.to_date=?) AS year_count, " +
                        "(SELECT COUNT(*) FROM tbl_voucher v JOIN tbl_accountingyear y2 ON y2.id=v.yearid WHERE y2.companyid=c.id AND y2.from_date=? AND y2.to_date=?) AS voucher_count " +
                        "FROM tbl_company c WHERE c.name=?")) {
            java.sql.Date iFrom = java.sql.Date.valueOf(java.time.LocalDate.of(2024, 1, 1));
            java.sql.Date iTo = java.sql.Date.valueOf(java.time.LocalDate.of(2024, 12, 31));
            iStatement.setObject(1, iFrom);
            iStatement.setObject(2, iTo);
            iStatement.setObject(3, iFrom);
            iStatement.setObject(4, iTo);
//            iStatement.setObject(5, "DemofÃƒÆ’Ã‚Â¶retaget");
            iStatement.setObject(5, "Demoföretaget");

            try (ResultSet iResultSet = iStatement.executeQuery()) {
                if (iResultSet.next()) {
                    LOG.info(
                            "V2 demo seed complete: company='{}', year=2025, years={}, customers={}, products={}, suppliers={}, vouchers={}",
                            iResultSet.getString("cname"),
                            iResultSet.getInt("year_count"),
                            iResultSet.getInt("customer_count"),
                            iResultSet.getInt("product_count"),
                            iResultSet.getInt("supplier_count"),
                            iResultSet.getInt("voucher_count"));
                }
            }
        } catch (SQLException e) {
            LOG.error("Unexpected error", e);
        }
    }

    /* Create default account plans if no account plan exists in DB */
    private void checkImportDefaultAccountPlans() {
        try {
            if (iConnection == null || iConnection.isClosed()) {
                return;
            }

            List<String> discoveredDefaultPaths = SSAccountPlanDefaultResourceDiscovery.discoverDefaultExcelResources(
                    SSDB.class.getClassLoader());
            Set<String> validDefaultPaths = new HashSet<>();

            for (String path : discoveredDefaultPaths) {
                String fileName = getFileName(path);
                try (InputStream is = SSDB.class.getClassLoader().getResourceAsStream(path)) {
                    if (is == null) {
                        LOG.warn("Default account-plan resource not found: {}", path);
                        continue;
                    }

                    SSAccountPlan plan = SSAccountPlanLoader.readPlan(is);
                    SSAccountPlanImporter.validateFileNameAgainstPlanName(fileName, plan);
                    plan.setExcelPath(path);
                    plan.setDefaultPlan(true);

                    upsertDefaultAccountPlan(plan);
                    validDefaultPaths.add(path);
                } catch (SSImportException | IOException | SQLException e) {
                    LOG.warn("Skipping invalid default account-plan '{}': {}", path, e.getMessage());
                }
            }

            cleanupRemovedDefaultAccountPlans(validDefaultPaths);
        } catch (SQLException | IOException e) {
            LOG.error("Unexpected error", e);
        }
    }

    private void upsertDefaultAccountPlan(SSAccountPlan plan) throws SQLException {
        Integer existingId = null;
        try (PreparedStatement exists = iConnection.prepareStatement("SELECT id FROM tbl_accountplan WHERE name=?")) {
            exists.setObject(1, plan.getName());
            try (ResultSet resultSet = exists.executeQuery()) {
                if (resultSet.next()) {
                    existingId = resultSet.getInt(1);
                }
            }
        }

        if (existingId != null) {
            plan.setId(existingId);
            Repositories.accountPlans().update(plan);
            return;
        }
        Repositories.accountPlans().add(plan);
    }

    private void cleanupRemovedDefaultAccountPlans(Set<String> validDefaultPaths) {
        for (SSAccountPlan existing : Repositories.accountPlans().findAll()) {
            if (!existing.isDefaultPlan()) {
                continue;
            }
            String excelPath = existing.getExcelPath();
            if (excelPath == null || !validDefaultPaths.contains(excelPath)) {
                Repositories.accountPlans().delete(existing);
            }
        }
    }

    private String getFileName(String path) {
        if (path == null) {
            return null;
        }
        int separatorIndex = path.lastIndexOf('/');
        return separatorIndex < 0 ? path : path.substring(separatorIndex + 1);
    }

    public void deleteDatabaseFiles() {
        try {
            try (PreparedStatement iStatement = iConnection.prepareStatement("SHUTDOWN")) {
                iStatement.executeUpdate();
            }
            iConnection.close();
        } catch (SQLException e) {
            LOG.error("Unexpected error", e);
            try {
                iConnection.rollback();
            } catch (SQLException ignored) {}
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    e.getMessage());
        }
        File iDbDir = new File(Path.get(Path.USER_DATA), "db");
        File iPropFile = new File(iDbDir, "JFSDB.properties");
        File iScriptFile = new File(iDbDir, "JFSDB.script");
        File iDataFile = new File(iDbDir, "JFSDB.data");
        File iBackupFile = new File(iDbDir, "JFSDB.backup");
        File iLogFile = new File(iDbDir, "JFSDB.log");

        if (iPropFile.exists()) {
            iPropFile.delete();
        }
        if (iScriptFile.exists()) {
            iScriptFile.delete();
        }
        if (iDataFile.exists()) {
            iDataFile.delete();
        }
        if (iBackupFile.exists()) {
            iBackupFile.delete();
        }
        if (iLogFile.exists()) {
            iLogFile.delete();
        }
    }

    public void clearCachedLists() {
        clearCachedListsInternal();
    }

    void clearCachedListsInternal() {
        iProducts = null;
        iCustomers = null;
        iSuppliers = null;
        iAutoDists = null;
        iInpayments = null;
        iTenders = null;
        iOrders = null;
        iInvoices = null;
        iCreditInvoices = null;
        iPeriodicInvoices = null;
        iOutpayments = null;
        iPurchaseOrders = null;
        iSupplierInvoices = null;
        iSupplierCreditInvoices = null;
        iInventories = null;
        iIndeliveries = null;
        iOutdeliveries = null;
        iOwnReports = null;
    }

    void clearYearCachesInternal() {
        iVouchers = null;
    }

    public void setCurrentCompany(SSNewCompany iCompany) {
        if (iCompany == null) {
            iCurrentCompany = null;
            clearCachedListsInternal();
            notifyListeners("COMPANY", iCurrentCompany, null);
            return;
        }
        iCurrentCompany = Repositories.companies().findById(iCompany).orElse(iCompany);
        if (iCurrentCompany.getId() != null) {
            Repositories.companies().activateAndApplySchema(iCurrentCompany.getId());
        } else if (iCurrentCompany.getSchemaName() != null) {
            Repositories.companies().registerOrActivateCompanySchema(iCurrentCompany.getSchemaName(),
                    iCurrentCompany.getName());
            iCurrentCompany = Repositories.companies().findBySchemaName(iCurrentCompany.getSchemaName())
                    .orElse(iCurrentCompany);
        }
        clearCachedListsInternal();
        notifyListeners("COMPANY", iCurrentCompany, null);
    }

    public SSNewCompany getCurrentCompany() {
        return iCurrentCompany;
    }


    public void setCurrentYear(SSNewAccountingYear iYear) {
        if (iYear == null) {
            iCurrentYear = null;
            clearYearCachesInternal();
            notifyListeners("YEAR", iCurrentYear, null);
            return;
        }
        Repositories.accountingYears().open(iYear);
    }

    public void applyOpenedYearFromRepository(SSNewAccountingYear iYear) {
        iCurrentYear = iYear;
        clearYearCachesInternal();
        notifyListeners("YEAR", iCurrentYear, null);
    }

    /**
     * Opens an accounting year as the active year using the open/close lifecycle.
     * <p>
     * The account plan snapshot stored in {@code TBL_accountingyear} is read and
     * copied into {@code TBL_account} rows (keyed by {@code accountingyear_id}).
     * </p>
     *
     * @param iYear the year to open; must not be {@code null}
     */
    public void openYear(SSNewAccountingYear iYear) {
        Repositories.accountingYears().open(iYear);
    }


    public SSNewAccountingYear getCurrentYear() {
        return Repositories.accountingYears().findById(iCurrentYear).orElse(null);
    }

    public List<SSNewCompany> loadCompanies() {
        try {
            return new LinkedList<>(Repositories.companies().findAll());
        } catch (RuntimeException e) {
            LOG.error("Unexpected error", e);
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    cause.getMessage());
            return new LinkedList<>();
        }
    }

    public List<SSNewAccountingYear> loadYearsForCompany(SSNewCompany iCompany) {
        return Repositories.accountingYears().findForCompany(iCompany);
    }

    public Optional<SSNewAccountingYear> getPreviousYear() {
        return Repositories.accountingYears().findPrevious();
    }

    /**
     *
     * Adds a property listerner to the database, the avaiable properties is:
     *   IO      : I/O event
     *   COMPANY : Changed active company
     *   YEAR    : Changed active year
     *
     * @param pProperty
     * @param pPropertyChangeListener
     */
    public void addPropertyChangeListener(String pProperty, PropertyChangeListener pPropertyChangeListener) {
        iEventBus.addPropertyChangeListener(pProperty, pPropertyChangeListener);
    }

    /**
     *
     * @param pProperty
     * @param pNewValue
     * @param pOldValue
     */
    public void notifyListeners(String pProperty, Object pNewValue, Object pOldValue) {
        iEventBus.notifyListeners(this, pProperty, pNewValue, pOldValue);
    }

    public List<SSVoucher> loadVouchers() {
        List<SSVoucher> cached = iVouchers;
        if (cached != null) {
            return cached;
        }
        List<SSVoucher> freshList = iCurrentYear == null
                ? new LinkedList<>()
                : new LinkedList<>(Repositories.vouchers().findByYear(iCurrentYear));
        iVouchers = freshList;
        return iVouchers;
    }

    /**
     * Retuns the account plan for the current year
     *
     * @return the acoount plan for the current year
     */
    public SSAccountPlan findCurrentAccountPlan() {

        if (iCurrentYear != null) {
            return iCurrentYear.getAccountPlan();
        }
        return new SSAccountPlan("Default");
    }

    private void failIfLegacySingleSchemaDatabase() throws SQLException {
        if (iConnection == null || iConnection.isClosed()) {
            return;
        }
        boolean iHasCompanyTable = tableExistsInSchema(DEFAULT_SCHEMA_NAME, "TBL_COMPANY");
        boolean iHasCatalogTable = tableExistsInSchema(DEFAULT_SCHEMA_NAME, "TBL_COMPANY_CATALOG");
        if (iHasCompanyTable && !iHasCatalogTable) {
            throw new SQLException("Legacy single-schema database detected. Migration is not supported.");
        }
    }

    private boolean tableExistsInSchema(String schemaName, String tableNameUpper) throws SQLException {
        String sql = "SELECT 1 FROM INFORMATION_SCHEMA.TABLES WHERE UPPER(TABLE_SCHEMA)=? AND UPPER(TABLE_NAME)=?";
        try (PreparedStatement iStatement = iConnection.prepareStatement(sql)) {
            iStatement.setString(1, schemaName.toUpperCase(Locale.ROOT));
            iStatement.setString(2, tableNameUpper.toUpperCase(Locale.ROOT));
            try (ResultSet iResultSet = iStatement.executeQuery()) {
                return iResultSet.next();
            }
        }
    }

    private boolean columnExistsInSchema(String schemaName, String tableNameUpper, String columnNameUpper) throws SQLException {
        String sql = "SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS WHERE UPPER(TABLE_SCHEMA)=? AND UPPER(TABLE_NAME)=? "
                + "AND UPPER(COLUMN_NAME)=?";
        try (PreparedStatement iStatement = iConnection.prepareStatement(sql)) {
            iStatement.setString(1, schemaName.toUpperCase(Locale.ROOT));
            iStatement.setString(2, tableNameUpper.toUpperCase(Locale.ROOT));
            iStatement.setString(3, columnNameUpper.toUpperCase(Locale.ROOT));
            try (ResultSet iResultSet = iStatement.executeQuery()) {
                return iResultSet.next();
            }
        }
    }

    private void validateSchemaContract() throws SQLException {
        if (!tableExistsInSchema(DEFAULT_SCHEMA_NAME, "TBL_COMPANY_CATALOG")) {
            throw new SQLException("Database schema mismatch: missing PUBLIC.TBL_COMPANY_CATALOG");
        }
        if (!tableExistsInSchema(DEFAULT_SCHEMA_NAME, "TBL_ACCOUNTPLAN")) {
            throw new SQLException("Database schema mismatch: missing PUBLIC.TBL_ACCOUNTPLAN");
        }

        Optional<String> iActiveSchemaOptional = loadActiveCatalogSchemaName();
        if (iActiveSchemaOptional.isEmpty()) {
            throw new SQLException("Database schema mismatch: no active company schema in PUBLIC.TBL_COMPANY_CATALOG");
        }

        String iActiveSchema = iActiveSchemaOptional.get();
        if (!schemaExists(iActiveSchema)) {
            throw new SQLException("Database schema mismatch: active schema does not exist: " + iActiveSchema);
        }

        requireTable(iActiveSchema, "TBL_COMPANY");
        requireTable(iActiveSchema, "TBL_ACCOUNTINGYEAR");
        requireTable(iActiveSchema, "TBL_ACCOUNT");
        requireTable(iActiveSchema, "TBL_PRODUCT");
        requireTable(iActiveSchema, "TBL_CUSTOMER");
        requireTable(iActiveSchema, "TBL_SUPPLIER");
        requireTable(iActiveSchema, "TBL_INVOICE");

        requireColumn(iActiveSchema, "TBL_COMPANY", "ID");
        requireColumn(iActiveSchema, "TBL_COMPANY", "NAME");
        requireColumn(iActiveSchema, "TBL_ACCOUNTINGYEAR", "ID");
        requireColumn(iActiveSchema, "TBL_ACCOUNTINGYEAR", "COMPANYID");
        requireColumn(iActiveSchema, "TBL_ACCOUNTINGYEAR", "FROM_DATE");
        requireColumn(iActiveSchema, "TBL_ACCOUNTINGYEAR", "TO_DATE");
        requireColumn(iActiveSchema, "TBL_ACCOUNT", "ACCOUNTINGYEAR_ID");
        requireColumn(iActiveSchema, "TBL_ACCOUNT", "NUMBER");
        requireColumn(iActiveSchema, "TBL_PRODUCT", "NUMBER");
        requireColumn(iActiveSchema, "TBL_CUSTOMER", "NUMBER");
        requireColumn(iActiveSchema, "TBL_SUPPLIER", "NUMBER");
        requireColumn(iActiveSchema, "TBL_INVOICE", "NUMBER");
    }

    private void requireTable(String schemaName, String tableNameUpper) throws SQLException {
        if (!tableExistsInSchema(schemaName, tableNameUpper)) {
            throw new SQLException("Database schema mismatch: missing " + schemaName + "." + tableNameUpper);
        }
    }

    private void requireColumn(String schemaName, String tableNameUpper, String columnNameUpper) throws SQLException {
        if (!columnExistsInSchema(schemaName, tableNameUpper, columnNameUpper)) {
            throw new SQLException("Database schema mismatch: missing column "
                    + schemaName + "." + tableNameUpper + "." + columnNameUpper);
        }
    }

    private void ensureCatalogBootstrapForCurrentSchema() {
        try {
            if (!tableExistsInSchema(DEFAULT_SCHEMA_NAME, "TBL_COMPANY_CATALOG")) {
                return;
            }
            ensureSeedStateTable();

            int iCatalogRows = 0;
            try (PreparedStatement iCountStatement = iConnection.prepareStatement(
                    "SELECT COUNT(*) FROM PUBLIC.tbl_company_catalog");
                 ResultSet iCountResult = iCountStatement.executeQuery()) {
                if (iCountResult.next()) {
                    iCatalogRows = iCountResult.getInt(1);
                }
            }

            if (iCatalogRows == 0) {
                setSchema(DEFAULT_SCHEMA_NAME);
                if (schemaExists(DEMO_SCHEMA_NAME)) {
                    dropSchema(DEMO_SCHEMA_NAME);
                }
                createSchemaIfMissing(DEMO_SCHEMA_NAME);
                setSchema(DEMO_SCHEMA_NAME);

                SSSchemaBuilder iBuilder = new SSSchemaBuilder(iConnection);
                iBuilder.createCompanyTables();

                String iCompanyName = resolvePreferredCompanyNameInCurrentSchema();
                if (iCompanyName == null) {
                    iCompanyName = DEMO_COMPANY_NAME;
                }

                try (PreparedStatement iInsertStatement = iConnection.prepareStatement(
                        "INSERT INTO PUBLIC.tbl_company_catalog(schema_name, company_name, is_active) VALUES (?, ?, TRUE)")) {
                    iInsertStatement.setString(1, DEMO_SCHEMA_NAME);
                    iInsertStatement.setString(2, iCompanyName);
                    iInsertStatement.executeUpdate();
                }
                iConnection.commit();
                return;
            }

            Optional<String> iActiveSchemaName = loadActiveCatalogSchemaName();
            if (iActiveSchemaName.isPresent()) {
                setSchema(iActiveSchemaName.get());
                new SSSchemaBuilder(iConnection).createCompanyTables();
                iConnection.commit();
            }
        } catch (SQLException e) {
            LOG.error("Unexpected error", e);
            try {
                iConnection.rollback();
            } catch (SQLException ignored) {}
        }
    }

    private void ensureSeedStateTable() throws SQLException {
        try (PreparedStatement iCreateTable = iConnection.prepareStatement(
                "CREATE TABLE IF NOT EXISTS " + SEED_STATE_TABLE + " (seed_done BOOLEAN DEFAULT FALSE NOT NULL)")) {
            iCreateTable.executeUpdate();
        }
        try (PreparedStatement iCountStatement = iConnection.prepareStatement(
                "SELECT COUNT(*) FROM " + SEED_STATE_TABLE);
             ResultSet iCountResult = iCountStatement.executeQuery()) {
            if (iCountResult.next() && iCountResult.getInt(1) == 0) {
                try (PreparedStatement iInsert = iConnection.prepareStatement(
                        "INSERT INTO " + SEED_STATE_TABLE + "(seed_done) VALUES(FALSE)")) {
                    iInsert.executeUpdate();
                }
            }
        }
    }

    private void setSeedDone(boolean seedDone) throws SQLException {
        try (PreparedStatement iUpdate = iConnection.prepareStatement(
                "UPDATE " + SEED_STATE_TABLE + " SET seed_done=?")) {
            iUpdate.setBoolean(1, seedDone);
            iUpdate.executeUpdate();
        }
    }

    private Optional<String> loadActiveCatalogSchemaName() throws SQLException {
        try (PreparedStatement iStatement = iConnection.prepareStatement(
                "SELECT schema_name FROM PUBLIC.tbl_company_catalog WHERE is_active=TRUE "
                        + "ORDER BY catalog_id FETCH FIRST 1 ROWS ONLY");
             ResultSet iResultSet = iStatement.executeQuery()) {
            if (iResultSet.next()) {
                return Optional.ofNullable(iResultSet.getString("schema_name"));
            }
            return Optional.empty();
        }
    }

    private String resolvePreferredCompanyNameInCurrentSchema() throws SQLException {
        try (PreparedStatement iCompanyStatement = iConnection.prepareStatement(
                "SELECT name FROM tbl_company ORDER BY CASE WHEN name=? THEN 0 ELSE 1 END, id FETCH FIRST 1 ROWS ONLY")) {
            iCompanyStatement.setString(1, DEMO_COMPANY_NAME);
            try (ResultSet iResultSet = iCompanyStatement.executeQuery()) {
                if (iResultSet.next()) {
                    return iResultSet.getString(1);
                }
                return null;
            }
        }
    }

    private void createSchemaIfMissing(String schemaName) throws SQLException {
        try (Statement iStatement = iConnection.createStatement()) {
            iStatement.execute("CREATE SCHEMA " + quoteIdentifier(schemaName));
        } catch (SQLException e) {
            String iMessage = e.getMessage();
            if (iMessage == null || !iMessage.toLowerCase(Locale.ROOT).contains("already exists")) {
                throw e;
            }
        }
    }

    private boolean schemaExists(String schemaName) throws SQLException {
        try (PreparedStatement iStatement = iConnection.prepareStatement(
                "SELECT 1 FROM INFORMATION_SCHEMA.SCHEMATA WHERE UPPER(SCHEMA_NAME)=?")) {
            iStatement.setString(1, schemaName.toUpperCase(Locale.ROOT));
            try (ResultSet iResultSet = iStatement.executeQuery()) {
                return iResultSet.next();
            }
        }
    }

    private void dropSchema(String schemaName) throws SQLException {
        try (Statement iStatement = iConnection.createStatement()) {
            iStatement.execute("DROP SCHEMA " + quoteIdentifier(schemaName) + " CASCADE");
        }
    }

    private void setSchema(String schemaName) throws SQLException {
        String iSafeSchema = schemaName == null || schemaName.trim().isEmpty()
                ? DEFAULT_SCHEMA_NAME
                : schemaName;
        try (Statement iStatement = iConnection.createStatement()) {
            iStatement.execute("SET SCHEMA " + quoteIdentifier(iSafeSchema));
        }
    }

    private String quoteIdentifier(String identifier) {
        return "\"" + identifier.replace("\"", "\"\"") + "\"";
    }

    private void showUiWarning(String message) {
        LOG.warn(message);
        if (java.awt.GraphicsEnvironment.isHeadless()) {
            return;
        }
        javax.swing.JOptionPane.showMessageDialog(
                SSMainFrame.getInstance(),
                message,
                "Varning",
                javax.swing.JOptionPane.WARNING_MESSAGE);
    }

    /**
     * Rolls back the current database transaction.
     *
     * @throws SQLException if the rollback fails
     */
    public void rollbackCurrentTransaction() throws SQLException {
        iConnection.rollback();
    }

    /**
     * Returns the current database connection.
     *
     * @return the active connection
     */
    public Connection getConnection() {
        return iConnection;
    }


    // //////////////////////////////////////////////////////////////////////////////////////

    // ---- V2 database API for SSCurrency repository ----

    // //////////////////////////////////////////////////////////////////////////////////////

    public List<SSProduct> loadProducts() {
        if (iProducts != null) {
            return iProducts;
        }
        iProducts = new LinkedList<>();

        if (iCurrentCompany == null) {
            return iProducts;
        }
        try {
            iProducts.addAll(Repositories.products().findAll());
        } catch (RuntimeException e) {
            LOG.error("Unexpected error", e);
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    cause.getMessage());
        }
        return iProducts;
    }

    // //////////////////////////////////////////////////////////////////////////////////////

    public List<SSCustomer> loadCustomers() {
        if (iCustomers != null) {
            return iCustomers;
        }
        iCustomers = new LinkedList<>();
        try {
            iCustomers.addAll(Repositories.customers().findAll());
        } catch (RuntimeException e) {
            LOG.error("Unexpected error", e);
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    cause.getMessage());
        }
        return iCustomers;
    }

    // //////////////////////////////////////////////////////////////////////////////////////

    /**
     * Returns the suppliers for the current company.
     *
     * @return  A List of suppliers or an empty list.
     */
    public List<SSSupplier> loadSuppliers() {
        if (iSuppliers != null) {
            return iSuppliers;
        }
        iSuppliers = new LinkedList<>();
        try {
            iSuppliers.addAll(Repositories.suppliers().findAll());
        } catch (RuntimeException e) {
            LOG.error("Unexpected error", e);
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    cause.getMessage());
        }
        return iSuppliers;
    }

    // //////////////////////////////////////////////////////////////////////////////////////

    /**
     * Returns the autodistributions for the current company.
     *
     * @return  A List of autodists or an empty list.
     */
    public List<SSAutoDist> loadAutoDists() {
        if (iAutoDists != null) {
            return iAutoDists;
        }
        try {
            iAutoDists = new LinkedList<>(Repositories.autoDists().findAll());
        } catch (RuntimeException e) {
            LOG.error("Unexpected error", e);
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    e.getCause() == null ? e.getMessage() : e.getCause().getMessage());
        }
        if (iAutoDists == null) {
            iAutoDists = new LinkedList<>();
        }
        return iAutoDists;
    }

    // //////////////////////////////////////////////////////////////////////////////////////

    /**
     * Returns the tenders in the current company.
     *
     * @return  A List of tenders or an empty list.
     */
    public List<SSTender> loadTenders() {
        if (iTenders != null) {
            return iTenders;
        }
        try {
            iTenders = new LinkedList<>(Repositories.tenders().findAll());
        } catch (RuntimeException e) {
            LOG.error("Unexpected error", e);
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    cause.getMessage());
        }
        if (iTenders == null) {
            iTenders = new LinkedList<>();
        }
        return iTenders;
    }

    // //////////////////////////////////////////////////////////////////////////////////////

    public List<SSOrder> loadOrders() {
        if (iOrders != null) {
            return iOrders;
        }
        try {
            iOrders = new LinkedList<>(Repositories.orders().findAll());
        } catch (RuntimeException e) {
            LOG.error("Unexpected error", e);
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    cause.getMessage());
        }
        if (iOrders == null) {
            iOrders = new LinkedList<>();
        }
        return iOrders;
    }

    // //////////////////////////////////////////////////////////////////////////////////////

    public List<SSInvoice> loadInvoices() {
        if (iInvoices != null) {
            return iInvoices;
        }
        try {
            iInvoices = new LinkedList<>(Repositories.invoices().findAll());
        } catch (RuntimeException e) {
            LOG.error("Unexpected error", e);
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    cause.getMessage());
        }
        if (iInvoices == null) {
            iInvoices = new LinkedList<>();
        }
        return iInvoices;
    }


    /**
     * Returns the inpayments in the current company.
     *
     * @return  A List of invoices or an empty list.
     */
    public List<SSInpayment> loadInpayments() {
        if (iInpayments != null) {
            return iInpayments;
        }
        try {
            iInpayments = new LinkedList<>(Repositories.inpayments().findAll());
        } catch (RuntimeException e) {
            LOG.error("Unexpected error", e);
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    cause.getMessage());
        }
        if (iInpayments == null) {
            iInpayments = new LinkedList<>();
        }
        return iInpayments;
    }


    /**
     * Returns the outpayments in the current company.
     *
     * @return  A List of outpayments or an empty list.
     */
    public List<SSOutpayment> loadOutpayments() {
        if (iOutpayments != null) {
            return iOutpayments;
        }
        try {
            iOutpayments = new LinkedList<>(Repositories.outpayments().findAll());
        } catch (RuntimeException e) {
            LOG.error("Unexpected error", e);
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    cause.getMessage());
        }
        if (iOutpayments == null) {
            iOutpayments = new LinkedList<>();
        }
        return iOutpayments;
    }


    // //////////////////////////////////////////////////////////////////////////////////////

    /**
     * Returns the credit invoices in the current company.
     *
     * @return  A List of invoices or an empty list.
     */
    public List<SSCreditInvoice> loadCreditInvoices() {
        if (iCreditInvoices != null) {
            return iCreditInvoices;
        }
        try {
            iCreditInvoices = new LinkedList<>(Repositories.creditInvoices().findAll());
        } catch (RuntimeException e) {
            LOG.error("Unexpected error", e);
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    cause.getMessage());
        }
        if (iCreditInvoices == null) {
            iCreditInvoices = new LinkedList<>();
        }
        return iCreditInvoices;
    }


    // //////////////////////////////////////////////////////////////////////////////////////

    /**
     * Returns the periodic invoices in the current company.
     *
     * @return  A List of periodic invoices or an empty list.
     */
    public List<SSPeriodicInvoice> loadPeriodicInvoices() {
        if (iPeriodicInvoices != null) {
            return iPeriodicInvoices;
        }
        try {
            iPeriodicInvoices = new LinkedList<>(Repositories.periodicInvoices().findAll());
        } catch (RuntimeException e) {
            LOG.error("Unexpected error", e);
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    cause.getMessage());
        }
        if (iPeriodicInvoices == null) {
            iPeriodicInvoices = new LinkedList<>();
        }
        return iPeriodicInvoices;
    }


    // //////////////////////////////////////////////////////////////////////////////////////

    /**
     * Returns the purchase orders in the current company.
     *
     * @return  A List of orders or an empty list.
     */
    public List<SSPurchaseOrder> loadPurchaseOrders() {
        if (iPurchaseOrders != null) {
            return iPurchaseOrders;
        }
        try {
            iPurchaseOrders = new LinkedList<>(Repositories.purchaseOrders().findAll());
        } catch (RuntimeException e) {
            LOG.error("Unexpected error", e);
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    cause.getMessage());
        }
        if (iPurchaseOrders == null) {
            iPurchaseOrders = new LinkedList<>();
        }
        return iPurchaseOrders;
    }

    // //////////////////////////////////////////////////////////////////////////////////////

    /**
     * Returns the supplier invoices in the current company.
     *
     * @return  A List of invoices or an empty list.
     */
    public List<SSSupplierInvoice> loadSupplierInvoices() {
        if (iSupplierInvoices != null) {
            return iSupplierInvoices;
        }
        try {
            iSupplierInvoices = new LinkedList<>(Repositories.supplierInvoices().findAll());
        } catch (RuntimeException e) {
            LOG.error("Unexpected error", e);
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    cause.getMessage());
        }
        if (iSupplierInvoices == null) {
            iSupplierInvoices = new LinkedList<>();
        }
        return iSupplierInvoices;
    }

    // //////////////////////////////////////////////////////////////////////////////////////

    /**
     * Returns the credit invoices in the current company.
     *
     * @return  A List of invoices or an empty list.
     */
    public List<SSSupplierCreditInvoice> loadSupplierCreditInvoices() {
        if (iSupplierCreditInvoices != null) {
            return iSupplierCreditInvoices;
        }
        try {
            iSupplierCreditInvoices = new LinkedList<>(Repositories.supplierCreditInvoices().findAll());
        } catch (RuntimeException e) {
            LOG.error("Unexpected error", e);
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    cause.getMessage());
        }
        if (iSupplierCreditInvoices == null) {
            iSupplierCreditInvoices = new LinkedList<>();
        }
        return iSupplierCreditInvoices;
    }

    // //////////////////////////////////////////////////////////////////////////////////////

    /**
     *
     * @return
     */
    public List<SSInventory> loadInventories() {
        if (iInventories != null) {
            return iInventories;
        }

        try {
            iInventories = new LinkedList<>(Repositories.inventories().findAll());
        } catch (RuntimeException e) {
            LOG.error("Unexpected error", e);
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    cause.getMessage());
        }
        if (iInventories == null) {
            iInventories = new LinkedList<>();
        }
        return iInventories;
    }

    // //////////////////////////////////////////////////////////////////////////////////////

    /**
     *
     * @return
     */
    public List<SSIndelivery> loadIndeliveries() {
        if (iIndeliveries != null) {
            return iIndeliveries;
        }
        try {
            iIndeliveries = new LinkedList<>(Repositories.indeliveries().findAll());
        } catch (RuntimeException e) {
            LOG.error("Unexpected error", e);
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    cause.getMessage());
        }
        if (iIndeliveries == null) {
            iIndeliveries = new LinkedList<>();
        }
        return iIndeliveries;
    }

    // //////////////////////////////////////////////////////////////////////////////////////

    /**
     *
     * @return
     */
    public List<SSOutdelivery> loadOutdeliveries() {
        if (iOutdeliveries != null) {
            return iOutdeliveries;
        }
        try {
            iOutdeliveries = new LinkedList<>(Repositories.outdeliveries().findAll());
        } catch (RuntimeException e) {
            LOG.error("Unexpected error", e);
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error",
                    cause.getMessage());
        }
        if (iOutdeliveries == null) {
            iOutdeliveries = new LinkedList<>();
        }
        return iOutdeliveries;
    }

    // /////////////////////////////////////////////////////////////////////////////

    private void seedPublicTables() throws SQLException {
        try {
            setSchema("PUBLIC");
            JsonNode seedData = SSJsonSeedDataLoader.loadSeedFile(SEED_PUBLIC_FILE);

            seedCurrencies(seedData);
            seedUnits(seedData);
            seedPaymentTerms(seedData);
            seedDeliveryTerms(seedData);
            seedDeliveryWays(seedData);

            LOG.info("Successfully seeded PUBLIC schema tables");
        } catch (IOException e) {
            LOG.error("Failed to load Seed_Public.json", e);
            throw new IllegalStateException("Cannot load seed data for PUBLIC tables", e);
        }
    }

    private void seedCurrencies(JsonNode seedData) {
        List<JsonNode> currencies = SSJsonSeedDataLoader.getArrayObjects(seedData, "Valuta");
        for (JsonNode currency : currencies) {
            String code = requiredText(currency, "Kod");
            String description = optionalText(currency, "Beskrivning");

            SSCurrency ssCurrency = new SSCurrency(code, description);
            if (Repositories.currencies().findByCode(code).isPresent()) {
                Repositories.currencies().update(ssCurrency);
            } else {
                Repositories.currencies().add(ssCurrency);
            }
        }
    }

    private void seedUnits(JsonNode seedData) {
        List<JsonNode> units = SSJsonSeedDataLoader.getArrayObjects(seedData, "Standardenhet");
        for (JsonNode unit : units) {
            String name = requiredText(unit, "Namn");
            String description = optionalText(unit, "Beskrivning");

            SSUnit ssUnit = new SSUnit(name, description);
            if (Repositories.units().findByName(name).isPresent()) {
                Repositories.units().update(ssUnit);
            } else {
                Repositories.units().add(ssUnit);
            }
        }
    }

    private void seedPaymentTerms(JsonNode seedData) {
        List<JsonNode> terms = SSJsonSeedDataLoader.getArrayObjects(seedData, "Betalningsvillkor");
        for (JsonNode term : terms) {
            String name = requiredText(term, "Namn");
            String description = optionalText(term, "Beskrivning");
            Integer days = term.has("Dagar") && !term.get("Dagar").isNull()
                    ? term.get("Dagar").asInt()
                    : null;

            SSPaymentTerm ssPaymentTerm = new SSPaymentTerm(name, description);
            ssPaymentTerm.setDays(days);
            if (Repositories.paymentTerms().findByName(name).isPresent()) {
                Repositories.paymentTerms().update(ssPaymentTerm);
            } else {
                Repositories.paymentTerms().add(ssPaymentTerm);
            }
        }
    }

    private void seedDeliveryTerms(JsonNode seedData) {
        List<JsonNode> terms = SSJsonSeedDataLoader.getArrayObjects(seedData, "Leveransvillkor");
        for (JsonNode term : terms) {
            String name = requiredText(term, "Namn");
            String description = optionalText(term, "Beskrivning");

            SSDeliveryTerm ssDeliveryTerm = new SSDeliveryTerm(name, description);
            if (Repositories.deliveryTerms().findByName(name).isPresent()) {
                Repositories.deliveryTerms().update(ssDeliveryTerm);
            } else {
                Repositories.deliveryTerms().add(ssDeliveryTerm);
            }
        }
    }

    private void seedDeliveryWays(JsonNode seedData) {
        List<JsonNode> ways = SSJsonSeedDataLoader.getArrayObjects(seedData, "Leveranssätt");
        for (JsonNode way : ways) {
            String name = requiredText(way, "Namn");
            String description = optionalText(way, "Beskrivning");

            SSDeliveryWay ssDeliveryWay = new SSDeliveryWay(name, description);
            if (Repositories.deliveryWays().findByName(name).isPresent()) {
                Repositories.deliveryWays().update(ssDeliveryWay);
            } else {
                Repositories.deliveryWays().add(ssDeliveryWay);
            }
        }
    }

    // /////////////////////////////////////////////////////////////////////////////

    public List<SSOwnReport> loadOwnReports() {
        if (iOwnReports != null) {
            return iOwnReports;
        }
        try {
            iOwnReports = new LinkedList<>(Repositories.ownReports().findAll());
        } catch (IllegalStateException e) {
            LOG.error("Unexpected error loading own reports", e);
            SSErrorDialog.showDialog(SSMainFrame.getInstance(), "SQL Error", e.getMessage());
            iOwnReports = new LinkedList<>();
        }
        return iOwnReports;
    }

    // /////////////////////////////////////////////////////////////////////////////

    public void createNewTables() {
        if (iConnection == null) {
            return;
        }
        try {
            SSSchemaBuilder builder = new SSSchemaBuilder(iConnection);
            builder.createPublicTables();
            iConnection.commit();
        } catch (SQLException e) {
            LOG.error("Unexpected error in createNewTables", e);
        }
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();

        sb.append("se.swedsoft.bookkeeping.data.system.SSDB");
        sb.append("{iAutoDists=").append(iAutoDists);
        sb.append(", iConnection=").append(iConnection);
        sb.append(", iCreditInvoices=").append(iCreditInvoices);
        sb.append(", iCurrentCompany=").append(iCurrentCompany);
        sb.append(", iCurrentYear=").append(iCurrentYear);
        sb.append(", iCustomers=").append(iCustomers);
        sb.append(", iIndeliveries=").append(iIndeliveries);
        sb.append(", iInpayments=").append(iInpayments);
        sb.append(", iInventories=").append(iInventories);
        sb.append(", iInvoices=").append(iInvoices);
        sb.append(", iEventBus=").append(iEventBus);
        sb.append(", iOrders=").append(iOrders);
        sb.append(", iOutdeliveries=").append(iOutdeliveries);
        sb.append(", iOutpayments=").append(iOutpayments);
        sb.append(", iOwnReports=").append(iOwnReports);
        sb.append(", iPeriodicInvoices=").append(iPeriodicInvoices);
        sb.append(", iProducts=").append(iProducts);
        sb.append(", iPurchaseOrders=").append(iPurchaseOrders);
        sb.append(", iSupplierCreditInvoices=").append(iSupplierCreditInvoices);
        sb.append(", iSupplierInvoices=").append(iSupplierInvoices);
        sb.append(", iSuppliers=").append(iSuppliers);
        sb.append(", iTenders=").append(iTenders);
        sb.append(", iVouchers=").append(iVouchers);
        sb.append('}');
        return sb.toString();
    }
}
