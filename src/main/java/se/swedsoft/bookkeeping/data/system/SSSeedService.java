package se.swedsoft.bookkeeping.data.system;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.swedsoft.bookkeeping.data.SSAccountPlan;
import se.swedsoft.bookkeeping.data.SSAddress;
import se.swedsoft.bookkeeping.data.SSCustomer;
import se.swedsoft.bookkeeping.data.SSInvoice;
import se.swedsoft.bookkeeping.data.SSNewAccountingYear;
import se.swedsoft.bookkeeping.data.SSNewCompany;
import se.swedsoft.bookkeeping.data.SSProduct;
import se.swedsoft.bookkeeping.data.SSSupplier;
import se.swedsoft.bookkeeping.data.SSVoucher;
import se.swedsoft.bookkeeping.data.SSVoucherRow;
import se.swedsoft.bookkeeping.data.common.SSCurrency;
import se.swedsoft.bookkeeping.data.common.SSDeliveryTerm;
import se.swedsoft.bookkeeping.data.common.SSDeliveryWay;
import se.swedsoft.bookkeeping.data.common.SSPaymentTerm;
import se.swedsoft.bookkeeping.data.common.SSUnit;
import se.swedsoft.bookkeeping.data.common.SSInvoiceType;
import se.swedsoft.bookkeeping.data.base.SSSaleRow;
import se.swedsoft.bookkeeping.importexport.excel.SSAccountPlanLoader;
import se.swedsoft.bookkeeping.persistence.Repositories;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Shared seed helper service for parsing and resolving seed data.
 */
public class SSSeedService {
    private static final Logger LOG = LoggerFactory.getLogger(SSSeedService.class);
    private static final String DEMO_SCHEMA_NAME = "co_0";
    private static final String SEED_STATE_TABLE = "PUBLIC.tbl_seed_state";
    private static final String SEED_PUBLIC_FILE = "seed/Seed_Public.json";
    private static final String SEED_COMPANY_FILE = "seed/Seed_Company_Demo.json";
    private static final String SEED_DEMO_FILE = "seed/Seed_Demo.json";
    private static final String SEED_VER_FAKT_FILE = "seed/Seed_Demo_VerFakt.json";

    public JsonNode requiredObject(JsonNode node, String fieldName) {
        if (node == null || !node.has(fieldName) || !node.get(fieldName).isObject()) {
            throw new IllegalStateException("Missing object field '" + fieldName + "' in seed JSON.");
        }
        return node.get(fieldName);
    }

    public String requiredText(JsonNode node, String fieldName) {
        String value = optionalText(node, fieldName);
        if (value == null || value.isEmpty()) {
            throw new IllegalStateException("Missing text field '" + fieldName + "' in seed JSON.");
        }
        return value;
    }

    public String optionalText(JsonNode node, String fieldName) {
        String value = SSJsonSeedDataLoader.getStringFieldOrNull(node, fieldName);
        return value == null ? null : value.trim();
    }

    public LocalDate requiredDate(JsonNode node, String fieldName) {
        String raw = requiredText(node, fieldName);
        try {
            return LocalDate.parse(raw);
        } catch (Exception e) {
            throw new IllegalStateException("Invalid date '" + raw + "' in field '" + fieldName + "'.", e);
        }
    }

    public Optional<LocalDate> optionalDate(JsonNode node, String fieldName) {
        String raw = optionalText(node, fieldName);
        if (raw == null || raw.isEmpty()) {
            return Optional.empty();
        }
        try {
            return Optional.of(LocalDate.parse(raw));
        } catch (Exception e) {
            throw new IllegalStateException("Invalid date '" + raw + "' in field '" + fieldName + "'.", e);
        }
    }

    public BigDecimal optionalBigDecimal(JsonNode node, String fieldName) {
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

    public BigDecimal requiredBigDecimal(JsonNode node, String fieldName) {
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

    public Optional<Boolean> optionalBoolean(JsonNode node, String fieldName) {
        if (node == null || !node.has(fieldName)) {
            return Optional.empty();
        }
        String raw = node.get(fieldName).asText();
        if (raw == null) {
            return Optional.empty();
        }
        String normalized = raw.trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            return Optional.empty();
        }
        if ("true".equals(normalized) || "1".equals(normalized) || "ja".equals(normalized)) {
            return Optional.of(Boolean.TRUE);
        }
        if ("false".equals(normalized) || "0".equals(normalized) || "nej".equals(normalized)) {
            return Optional.of(Boolean.FALSE);
        }
        throw new IllegalStateException("Invalid boolean '" + raw + "' in field '" + fieldName + "'.");
    }

    public String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    public String optionalScalarText(JsonNode node, String fieldName) {
        if (node == null || !node.has(fieldName)) {
            return null;
        }
        JsonNode valueNode = node.get(fieldName);
        if (valueNode == null || valueNode.isNull() || valueNode.isContainerNode()) {
            return null;
        }
        String value = valueNode.asText();
        if (value == null) {
            return null;
        }
        value = value.trim();
        return value.isEmpty() ? null : value;
    }

    public LocalDate adjustDateToYear(LocalDate date, SSNewAccountingYear year) {
        if (year == null || year.getLocalFrom() == null) {
            return date;
        }
        int targetYear = year.getLocalFrom().getYear();
        if (date.getYear() == targetYear) {
            return date;
        }
        try {
            return LocalDate.of(targetYear, date.getMonthValue(), date.getDayOfMonth());
        } catch (java.time.DateTimeException e) {
            return LocalDate.of(targetYear, date.getMonthValue(), 1);
        }
    }

    public LocalDate buildInvoiceDate(SSNewAccountingYear year) {
        LocalDate today = LocalDate.now();
        if (year == null || year.getLocalFrom() == null) {
            return today;
        }
        int targetYear = year.getLocalFrom().getYear();
        try {
            return LocalDate.of(targetYear, today.getMonthValue(), today.getDayOfMonth());
        } catch (java.time.DateTimeException e) {
            return LocalDate.of(targetYear, today.getMonthValue(), 1);
        }
    }

    public Optional<SSNewAccountingYear> getAccountingYearByRangeV2(
            Connection connection, SSNewCompany company, LocalDate from, LocalDate to) {
        if (company == null || from == null || to == null || connection == null) {
            return Optional.empty();
        }

        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT * FROM tbl_accountingyear WHERE companyid=? AND from_date=? AND to_date=?")) {
            statement.setObject(1, company.getId());
            statement.setObject(2, java.sql.Date.valueOf(from));
            statement.setObject(3, java.sql.Date.valueOf(to));

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(Repositories.accountingYears().mapAccountingYear(resultSet));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            LOG.error("Unexpected error", e);
            return Optional.empty();
        }
    }

    public void seedPublicTables(Connection connection) throws IOException {
        JsonNode seedData = SSJsonSeedDataLoader.loadSeedFile(SEED_PUBLIC_FILE);
        seedCurrencies(seedData, connection);
        seedUnits(seedData, connection);
        seedPaymentTerms(seedData, connection);
        seedDeliveryTerms(seedData, connection);
        seedDeliveryWays(seedData, connection);
        LOG.info("Successfully seeded PUBLIC schema tables");
    }

    public boolean isSeedAlreadyDone(Connection connection) throws SQLException {
        try (PreparedStatement iStatement = connection.prepareStatement(
                "SELECT seed_done FROM " + SEED_STATE_TABLE)) {
            try (ResultSet iResultSet = iStatement.executeQuery()) {
                if (iResultSet.next()) {
                    return iResultSet.getBoolean("seed_done");
                }
            }
        }
        return false;
    }

    public void setSeedDone(Connection connection, boolean seedDone) throws SQLException {
        try (PreparedStatement iUpdate = connection.prepareStatement(
                "UPDATE " + SEED_STATE_TABLE + " SET seed_done=?")) {
            iUpdate.setBoolean(1, seedDone);
            iUpdate.executeUpdate();
        }
    }

    public void runDemoSeedIfNeeded(
            Connection connection,
            Consumer<String> setSchemaCallback,
            Consumer<SSNewCompany> setCompanyCallback) {
        try {
            if (connection == null || connection.isClosed()) {
                return;
            }
            if (setSchemaCallback == null) {
                throw new IllegalArgumentException("setSchemaCallback must not be null");
            }

            if (isSeedAlreadyDone(connection)) {
                return;
            }

            Optional<String> demoSchemaName = loadActiveCatalogSchemaName(connection);
            if (demoSchemaName.isEmpty() || !DEMO_SCHEMA_NAME.equals(demoSchemaName.get())) {
                return;
            }

            setSchemaCallback.accept("PUBLIC");
            seedPublicTables(connection);

            setSchemaCallback.accept(DEMO_SCHEMA_NAME);
            SSNewCompany seededCompany = seedDemoCompanyAndAccountingYear(connection, setCompanyCallback);
            seedDemoEntitiesFromJson(connection);
            seedVouchersAndInvoicesFromJson(connection, seededCompany);

            setSeedDone(connection, true);
            connection.commit();
        } catch (SQLException e) {
            LOG.error("Unexpected error during demo seed", e);
            try {
                connection.rollback();
            } catch (SQLException ignored) {
            }
        } catch (Exception e) {
            LOG.error("Unexpected error during JSON seeding", e);
            try {
                connection.rollback();
            } catch (SQLException ignored) {
            }
        }
    }

    private void seedCurrencies(JsonNode seedData, Connection connection) {
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

    private void seedUnits(JsonNode seedData, Connection connection) {
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

    private void seedPaymentTerms(JsonNode seedData, Connection connection) {
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

    private void seedDeliveryTerms(JsonNode seedData, Connection connection) {
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

    private void seedDeliveryWays(JsonNode seedData, Connection connection) {
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

    public SSNewCompany seedDemoCompanyAndAccountingYear(
            Connection connection,
            Consumer<SSNewCompany> currentCompanySetter) throws IOException, SQLException {
        JsonNode seedRoot = SSJsonSeedDataLoader.loadSeedFile(SEED_COMPANY_FILE);
        JsonNode companyNode = requiredObject(seedRoot, "Företag");

        SSNewCompany seededCompany = ensureDemoCompanyInSchema(DEMO_SCHEMA_NAME, companyNode);
        if (currentCompanySetter != null) {
            currentCompanySetter.accept(seededCompany);
        }

        JsonNode yearNode = requiredObject(companyNode, "Bokföringsår");
        LocalDate fromDate = requiredDate(yearNode, "Från");
        LocalDate toDate = requiredDate(yearNode, "Till");
        if (toDate.isBefore(fromDate)) {
            throw new IllegalStateException("Invalid Bokföringsår in " + SEED_COMPANY_FILE + ": Till before Från.");
        }

        if (getAccountingYearByRangeV2(connection, seededCompany, fromDate, toDate).isPresent()) {
            return seededCompany;
        }

        SSAccountPlan templatePlan = resolveSeedAccountPlan(companyNode);
        SSAccountPlan yearPlan = createSeedYearPlan(templatePlan, seededCompany.getName(), fromDate);

        SSNewAccountingYear accountingYear = new SSNewAccountingYear();
        accountingYear.setLocalFrom(fromDate);
        accountingYear.setLocalTo(toDate);
        accountingYear.setAccountPlan(yearPlan);
        Repositories.accountingYears().add(accountingYear);
        return seededCompany;
    }

    public void seedDemoEntitiesFromJson(Connection connection) throws IOException {
        JsonNode seedRoot = SSJsonSeedDataLoader.loadSeedFile(SEED_DEMO_FILE);
        seedCustomers(seedRoot);
        seedSuppliers(seedRoot);
        seedProducts(seedRoot);
        LOG.info("Successfully seeded demo customers, suppliers and products");
    }

    private Optional<String> loadActiveCatalogSchemaName(Connection connection) throws SQLException {
        try (PreparedStatement iStatement = connection.prepareStatement(
                "SELECT schema_name FROM PUBLIC.tbl_company_catalog WHERE is_active=TRUE "
                        + "ORDER BY catalog_id FETCH FIRST 1 ROWS ONLY");
             ResultSet iResultSet = iStatement.executeQuery()) {
            if (iResultSet.next()) {
                return Optional.ofNullable(iResultSet.getString("schema_name"));
            }
            return Optional.empty();
        }
    }

    private void seedVouchersAndInvoicesFromJson(Connection connection, SSNewCompany currentCompany) {
        try {
            JsonNode seedRoot = SSJsonSeedDataLoader.loadSeedFile(SEED_VER_FAKT_FILE);
            List<SSNewAccountingYear> years = Repositories.accountingYears().findForCompany(currentCompany);
            seedVouchers(seedRoot, currentCompany, years);
            seedInvoices(seedRoot, currentCompany, years);
            LOG.info("Successfully seeded demo vouchers and invoices from {}", SEED_VER_FAKT_FILE);
        } catch (IOException e) {
            LOG.warn("Could not read seed file '{}': {}", SEED_VER_FAKT_FILE, e.getMessage());
        }
    }

    public void seedCustomers(JsonNode seedRoot) {
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

    public void seedSuppliers(JsonNode seedRoot) {
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

    public void seedProducts(JsonNode seedRoot) {
        for (JsonNode productNode : SSJsonSeedDataLoader.getArrayObjects(seedRoot, "Produkter")) {
            String number = requiredText(productNode, "Produktnummer");
            Optional<SSProduct> existingProduct = Repositories.products().findByNumber(number);
            SSProduct product = existingProduct.orElseGet(SSProduct::new);
            product.setNumber(number);
            product.setDescription(requiredText(productNode, "Beskrivning"));
            product.setSellingPrice(requiredBigDecimal(productNode, "Försäljningspris"));
            BigDecimal purchasePrice = optionalBigDecimal(productNode, "Inköpspris");
            if (purchasePrice != null) {
                product.setPurchasePrice(purchasePrice);
            }
            product.setOnlyWholeQuantity(optionalBoolean(productNode, "Hela antal").orElse(true));

            if (existingProduct.isPresent()) {
                Repositories.products().update(product);
            } else {
                Repositories.products().add(product);
            }
        }
    }

    public void seedVouchers(JsonNode seedRoot, SSNewCompany currentCompany, List<SSNewAccountingYear> years) {
        if (years == null || years.isEmpty()) {
            LOG.warn("No accounting years found for company '{}'; skipping voucher seed",
                    currentCompany != null ? currentCompany.getName() : "null");
            return;
        }
        SSNewAccountingYear seedYear = years.stream()
                .filter(y -> y.getLocalTo() != null)
                .max((a, b) -> a.getLocalTo().compareTo(b.getLocalTo()))
                .orElse(years.get(0));

        for (JsonNode verNode : SSJsonSeedDataLoader.getArrayObjects(seedRoot, "Verifikationer")) {
            try {
                LocalDate rawDate = requiredDate(verNode, "Datum");
                LocalDate voucherDate = adjustDateToYear(rawDate, seedYear);

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

    public void seedInvoices(JsonNode seedRoot, SSNewCompany currentCompany, List<SSNewAccountingYear> years) {
        List<SSCustomer> customers = Repositories.customers().findAll();
        List<SSProduct> products = Repositories.products().findAll();

        List<SSPaymentTerm> paymentTerms = Repositories.paymentTerms().findAll();
        SSPaymentTerm defaultPaymentTerm = resolveDefaultPaymentTerm(paymentTerms, currentCompany);

        SSNewAccountingYear seedYear = years.stream()
                .filter(y -> y.getLocalTo() != null)
                .max((a, b) -> a.getLocalTo().compareTo(b.getLocalTo()))
                .orElse(years.isEmpty() ? null : years.get(0));

        LocalDate defaultInvoiceDate = buildInvoiceDate(seedYear);

        for (JsonNode fakturaNode : SSJsonSeedDataLoader.getArrayObjects(seedRoot, "Fakturor")) {
            try {
                Optional<SSCustomer> customer = resolveSeedCustomer(customers, fakturaNode);
                if (customer.isEmpty()) {
                    LOG.warn("Skipping invoice: Kund-id '{}' not found", optionalScalarText(fakturaNode, "Kund-id"));
                    continue;
                }

                LocalDate invoiceDate = optionalDate(fakturaNode, "Fakturadatum")
                        .map(date -> adjustDateToYear(date, seedYear))
                        .orElse(defaultInvoiceDate);
                SSPaymentTerm paymentTerm = resolveSeedPaymentTerm(paymentTerms, fakturaNode).orElse(defaultPaymentTerm);
                LocalDate dueDate = paymentTerm != null ? paymentTerm.addDaysToLocalDate(invoiceDate) : invoiceDate;

                SSInvoice invoice = new SSInvoice(SSInvoiceType.NORMAL);
                invoice.setVoucher(null);
                invoice.setLocalDate(invoiceDate);
                invoice.setLocalDueDate(dueDate);
                invoice.setPaymentTerm(paymentTerm);
                invoice.setCustomerNr(customer.get().getNumber());
                invoice.setCustomerName(customer.get().getName());
                invoice.setOurContactPerson(customer.get().getOurContactPerson());
                invoice.setYourContactPerson(customer.get().getYourContactPerson());
                invoice.setInvoiceAddress(customer.get().getInvoiceAddress());
                invoice.setDeliveryAddress(customer.get().getDeliveryAddress());

                for (JsonNode rowNode : SSJsonSeedDataLoader.getArrayObjects(fakturaNode, "Rader")) {
                    Optional<SSProduct> product = resolveSeedProduct(products, rowNode);
                    if (product.isEmpty()) {
                        LOG.warn("Skipping invoice row: Produktnr '{}' not found", optionalScalarText(rowNode, "Produktnr"));
                        continue;
                    }
                    SSSaleRow row = new SSSaleRow(product.get());
                    BigDecimal quantity = requiredBigDecimal(rowNode, "Antal");
                    row.setQuantity(quantity.movePointRight(1).setScale(0, RoundingMode.HALF_UP).intValue());
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

    private SSNewCompany ensureDemoCompanyInSchema(String demoSchemaName, JsonNode companyNode) throws SQLException {
        SSNewCompany template = new SSNewCompany();
        applyDemoCompanySettings(template, companyNode);

        SSNewCompany seededCompany = Repositories.companies().ensureCompanyInSchema(demoSchemaName, template);
        applyDemoCompanySettings(seededCompany, companyNode);
        Repositories.companies().update(seededCompany);
        return Repositories.companies().findBySchemaName(demoSchemaName).orElse(seededCompany);
    }

    private void applyDemoCompanySettings(SSNewCompany company, JsonNode companyNode) {
        company.setName(requiredText(companyNode, "Företagsnamn"));
        company.setCorporateID(requiredText(companyNode, "Organisationsnummer"));
        company.setLogotype(optionalText(companyNode, "Logotyp"));
        company.setDelayInterest(requiredBigDecimal(companyNode, "Dröjsmålsränta"));
        company.setReminderfee(requiredBigDecimal(companyNode, "Påminnelseavgift"));
        company.setContactPerson(optionalText(companyNode, "Kontaktperson"));
        company.setSwishImagePath(optionalText(companyNode, "SWISH"));
        company.setSwishText(optionalText(companyNode, "Ledtext swish"));

        SSAddress address = company.getAddress();
        if (address == null) {
            address = new SSAddress();
            company.setAddress(address);
        }
        address.setName(optionalText(companyNode, "Adress.Namn"));
        address.setAddress1(optionalText(companyNode, "Adress.Adress 1"));
        address.setZipCode(optionalText(companyNode, "Adress.Postnummer"));

        String city = optionalText(companyNode, "Adress.Postort");
        if (city == null) {
            city = optionalText(companyNode, "Adress.Ort");
        }
        address.setCity(city);
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

    private SSAccountPlan createSeedYearPlan(SSAccountPlan templatePlan, String companyName, LocalDate fromDate) {
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

    private void applyInvoiceAddress(SSCustomer customer, JsonNode node) {
        SSAddress address = customer.getInvoiceAddress();
        if (address == null) {
            address = new SSAddress();
            customer.setInvoiceAddress(address);
        }
        boolean hasAddress = false;
        String address1 = firstNonBlank(optionalText(node, "Fakturaadress.Adress 1"), optionalText(node, "Adress 1"));
        String address2 = firstNonBlank(optionalText(node, "Fakturaadress.Adress 2"), optionalText(node, "Adress 2"));
        String zipCode = firstNonBlank(optionalText(node, "Fakturaadress.Postnummer"), optionalText(node, "Postnummer"));
        String city = firstNonBlank(
                optionalText(node, "Fakturaadress.Postort"),
                optionalText(node, "Fakturaadress.Ort"),
                optionalText(node, "Postort"),
                optionalText(node, "Ort"));
        String country = firstNonBlank(optionalText(node, "Fakturaadress.Land"), optionalText(node, "Land"));
        String name = firstNonBlank(optionalText(node, "Fakturaadress.Namn"), optionalText(node, "Adressnamn"));

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
        String address1 = firstNonBlank(optionalText(node, "Adress.Adress 1"), optionalText(node, "Adress 1"));
        String address2 = firstNonBlank(optionalText(node, "Adress.Adress 2"), optionalText(node, "Adress 2"));
        String zipCode = firstNonBlank(optionalText(node, "Adress.Postnummer"), optionalText(node, "Postnummer"));
        String city = firstNonBlank(
                optionalText(node, "Adress.Postort"),
                optionalText(node, "Adress.Ort"),
                optionalText(node, "Postort"),
                optionalText(node, "Ort"));
        String country = firstNonBlank(optionalText(node, "Adress.Land"), optionalText(node, "Land"));
        String name = firstNonBlank(optionalText(node, "Adress.Namn"), optionalText(node, "Adressnamn"));

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

    public SSPaymentTerm resolveDefaultPaymentTerm(List<SSPaymentTerm> paymentTerms, SSNewCompany currentCompany) {
        if (paymentTerms == null || paymentTerms.isEmpty()) {
            return null;
        }
        String companyDefault = currentCompany == null || currentCompany.getPaymentTerm() == null
                ? null
                : currentCompany.getPaymentTerm().getName();
        if (companyDefault != null) {
            for (SSPaymentTerm term : paymentTerms) {
                if (term != null && companyDefault.equals(term.getName())) {
                    return term;
                }
            }
        }
        for (SSPaymentTerm term : paymentTerms) {
            if (term != null && "30 dagar".equals(term.getName())) {
                return term;
            }
        }
        return paymentTerms.get(0);
    }

    public Optional<SSPaymentTerm> resolveSeedPaymentTerm(List<SSPaymentTerm> paymentTerms, JsonNode invoiceNode) {
        String termName = optionalText(invoiceNode, "Betalningsvillkor");
        if (termName == null || paymentTerms == null) {
            return Optional.empty();
        }
        for (SSPaymentTerm term : paymentTerms) {
            if (term != null && termName.equals(term.getName())) {
                return Optional.of(term);
            }
        }
        throw new IllegalStateException("Unknown Betalningsvillkor '" + termName + "' in seed JSON.");
    }

    public Optional<SSCustomer> resolveSeedCustomer(List<SSCustomer> customers, JsonNode invoiceNode) {
        String customerKey = optionalScalarText(invoiceNode, "Kund-id");
        if (customerKey == null) {
            throw new IllegalStateException("Missing text field 'Kund-id' in seed JSON.");
        }
        try {
            int customerPos = Integer.parseInt(customerKey);
            if (customerPos >= 1 && customerPos <= customers.size()) {
                return Optional.of(customers.get(customerPos - 1));
            }
        } catch (NumberFormatException ignored) {
            // Non-numeric customer reference is treated as explicit customer number.
        }
        for (SSCustomer customer : customers) {
            if (customer != null && customerKey.equals(customer.getNumber())) {
                return Optional.of(customer);
            }
        }
        return Optional.empty();
    }

    public Optional<SSProduct> resolveSeedProduct(List<SSProduct> products, JsonNode rowNode) {
        String productKey = optionalScalarText(rowNode, "Produktnr");
        if (productKey == null) {
            throw new IllegalStateException("Missing text field 'Produktnr' in seed JSON.");
        }
        try {
            int productPos = Integer.parseInt(productKey);
            if (productPos >= 1 && productPos <= products.size()) {
                return Optional.of(products.get(productPos - 1));
            }
        } catch (NumberFormatException ignored) {
            // Non-numeric product reference is treated as explicit product number.
        }
        for (SSProduct product : products) {
            if (product != null && productKey.equals(product.getNumber())) {
                return Optional.of(product);
            }
        }
        return Optional.empty();
    }
}
