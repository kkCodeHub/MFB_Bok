package se.swedsoft.bookkeeping.persistence.v2;

import org.fribok.bookkeeping.data.util.ConnectionSecurity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.swedsoft.bookkeeping.calc.util.SSAutoIncrement;
import se.swedsoft.bookkeeping.data.SSAddress;
import se.swedsoft.bookkeeping.data.SSNewAccountingYear;
import se.swedsoft.bookkeeping.data.SSNewCompany;
import se.swedsoft.bookkeeping.data.SSStandardText;
import se.swedsoft.bookkeeping.data.common.SSCurrency;
import se.swedsoft.bookkeeping.data.common.SSDefaultAccount;
import se.swedsoft.bookkeeping.data.common.SSDeliveryTerm;
import se.swedsoft.bookkeeping.data.common.SSDeliveryWay;
import se.swedsoft.bookkeeping.data.common.SSPaymentTerm;
import se.swedsoft.bookkeeping.data.common.SSUnit;
import se.swedsoft.bookkeeping.data.system.SSCompanyValidationRules;
import se.swedsoft.bookkeeping.data.util.SSMailServer;
import se.swedsoft.bookkeeping.persistence.Repositories;
import se.swedsoft.bookkeeping.persistence.v2.schema.SSSchemaBuilder;
import se.swedsoft.bookkeeping.util.SSUtil;

import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * V2 company repository backed by the normalized V2 schema.
 */
public class V2CompanyRepository {

    @FunctionalInterface
    public interface RollbackHandler {
        void rollbackCurrentTransaction() throws SQLException;
    }

    private static final Logger LOG = LoggerFactory.getLogger(V2CompanyRepository.class);
    private static final List<String> COMPANY_AUTO_INCREMENT_KEYS =
            SSCompanyValidationRules.companyAutoIncrementKeys();

    private final Connection connection;
    private final RollbackHandler rollbackHandler;
    private final Function<SSNewCompany, List<SSNewAccountingYear>> accountingYearsLoader;
    private final Consumer<SSNewAccountingYear> accountingYearRemover;

    /**
     * @param connection the active database connection; must not be {@code null}
     * @param rollbackHandler rollback delegate; must not be {@code null}
     * @param accountingYearsLoader loader for accounting years by company; must not be {@code null}
     * @param accountingYearRemover remover for accounting years; must not be {@code null}
     */
    public V2CompanyRepository(Connection connection,
                               RollbackHandler rollbackHandler,
                               Function<SSNewCompany, List<SSNewAccountingYear>> accountingYearsLoader,
                               Consumer<SSNewAccountingYear> accountingYearRemover) {
        if (connection == null) {
            throw new NullPointerException("connection must not be null");
        }
        if (rollbackHandler == null) {
            throw new NullPointerException("rollbackHandler must not be null");
        }
        if (accountingYearsLoader == null) {
            throw new NullPointerException("accountingYearsLoader must not be null");
        }
        if (accountingYearRemover == null) {
            throw new NullPointerException("accountingYearRemover must not be null");
        }
        this.connection = connection;
        this.rollbackHandler = rollbackHandler;
        this.accountingYearsLoader = accountingYearsLoader;
        this.accountingYearRemover = accountingYearRemover;
    }

    public List<SSNewCompany> findAll() {
        if (isConnectionClosed()) {
            return Collections.emptyList();
        }
        try {
            reconcileCatalogAgainstSchemas();
            List<SSNewCompany> companies = new LinkedList<>();
            List<CatalogEntry> catalogEntries = loadCatalogEntries();
            for (CatalogEntry catalogEntry : catalogEntries) {
                Optional<SSNewCompany> loadedCompany = loadCompanyFromSchema(catalogEntry.schemaName, catalogEntry.companyId);
                if (loadedCompany.isPresent()) {
                    SSNewCompany company = loadedCompany.get();
                    company.setSchemaName(catalogEntry.schemaName);
                    companies.add(company);
                } else {
                    SSNewCompany company = new SSNewCompany();
                    company.setId(catalogEntry.companyId);
                    company.setName(catalogEntry.companyName);
                    company.setSchemaName(catalogEntry.schemaName);
                    companies.add(company);
                }
            }

            Set<String> catalogSchemas = new HashSet<>();
            for (CatalogEntry entry : catalogEntries) {
                if (!SSUtil.isNullOrEmpty(entry.schemaName)) {
                    catalogSchemas.add(entry.schemaName.toUpperCase());
                }
            }

            for (String schemaName : listCompanySchemas()) {
                if (catalogSchemas.contains(schemaName.toUpperCase())) {
                    continue;
                }
                companies.add(buildAttentionCompanyRow(schemaName));
            }

            return companies;
        } catch (SQLException e) {
            throw handleFailure("load companies", e);
        } catch (RuntimeException e) {
            Throwable cause = e.getCause();
            if (cause instanceof SQLException) {
                throw handleFailure("load companies", (SQLException) cause);
            }
            throw e;
        }
    }

    public Optional<SSNewCompany> findById(SSNewCompany pCompany) {
        if (pCompany == null || isConnectionClosed()) {
            return Optional.empty();
        }
        if (!SSUtil.isNullOrEmpty(pCompany.getSchemaName())) {
            return findBySchemaName(pCompany.getSchemaName());
        }
        if (pCompany.getId() == null) {
            return Optional.empty();
        }
        try {
            Optional<CatalogEntry> catalogEntry = findCatalogEntryByCompanyId(pCompany.getId());
            if (catalogEntry.isPresent()) {
                return findBySchemaName(catalogEntry.get().schemaName);
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw handleFailure("find company by id '" + pCompany.getId() + "'", e);
        } catch (RuntimeException e) {
            Throwable cause = e.getCause();
            if (cause instanceof SQLException) {
                throw handleFailure("find company by id '" + pCompany.getId() + "'", (SQLException) cause);
            }
            throw e;
        }
    }

    /**
     * Finds a company by its name.
     *
     * @param pName the company name to search for; {@code null} returns empty
     * @return an {@link Optional} containing the matching company, or empty if not found
     */
    public Optional<SSNewCompany> findByName(String pName) {
        if (pName == null || isConnectionClosed()) {
            return Optional.empty();
        }
        try {
            Optional<CatalogEntry> catalogEntry = findCatalogEntryByCompanyName(pName);
            if (catalogEntry.isPresent()) {
                return findBySchemaName(catalogEntry.get().schemaName);
            }
            for (String schemaName : listCompanySchemas()) {
                Optional<SSNewCompany> company = findBySchemaName(schemaName);
                if (company.isPresent() && pName.equals(company.get().getName())) {
                    return company;
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw handleFailure("find company by name '" + pName + "'", e);
        }
    }

    public void add(SSNewCompany pCompany) {
        if (pCompany == null || isConnectionClosed()) {
            return;
        }
        try {
            Integer companyId = null;
            String schemaName = null;
            boolean schemaCreated = false;
            String previousSchema = getCurrentSchema();
            try {
                companyId = insertCatalogPlaceholder(pCompany.getName());
                schemaName = createSchemaName(companyId);
                updateCatalogSchemaName(companyId, schemaName);
                createSchema(schemaName);
                schemaCreated = true;

                setSchema(schemaName);
                new SSSchemaBuilder(connection).createCompanyTables();
                insertCompanyWithExplicitId(companyId, pCompany);
                pCompany.setId(companyId);
                replaceCompanyStandardTexts(companyId, pCompany.getStandardTexts());
                replaceCompanyDefaultAccounts(companyId, pCompany.getDefaultAccounts());
                replaceCompanyAutoIncrements(companyId, pCompany.getAutoIncrement());
                activateCompany(companyId);
                connection.commit();
            } catch (Exception e) {
                rollbackHandler.rollbackCurrentTransaction();
                try {
                    setSchema("PUBLIC");
                    if (schemaCreated && schemaName != null) {
                        dropSchema(schemaName);
                    }
                    if (companyId != null) {
                        deleteCatalogRow(companyId);
                    }
                } catch (SQLException cleanupError) {
                    LOG.error("Failed cleanup after company creation failure", cleanupError);
                }
                throw e;
            } finally {
                setSchema(previousSchema);
            }
        } catch (SQLException e) {
            throw handleFailure("add company", e);
        } catch (Exception e) {
            if (e instanceof RuntimeException) {
                Throwable cause = e.getCause();
                if (cause instanceof SQLException) {
                    throw handleFailure("add company", (SQLException) cause);
                }
            }
            throw new IllegalStateException("Unable to add company", e);
        }
    }

    public void update(SSNewCompany pCompany) {
        if (pCompany == null || isConnectionClosed()) {
            return;
        }
        try {
            Optional<CatalogEntry> catalogEntry = findCatalogEntryByCompanyId(pCompany.getId());
            String previousSchema = getCurrentSchema();
            try {
                if (catalogEntry.isPresent()) {
                    setSchema(catalogEntry.get().schemaName);
                }
                try (PreparedStatement statement = connection.prepareStatement(
                        "UPDATE tbl_company SET name=?,phone=?,phone2=?,telefax=?,residence=?,web_address=?," +
                                "smtp_address=?,smtp_name=?,smtp_port=?,smtp_bcc_addresses=?,smtp_auth=?,smtp_connection_security=?," +
                                "smtp_username=?,smtp_password=?,email=?,contact_person=?,tax_registered=?,corporate_id=?," +
                                "logotype=?,swish_image=?,swish_text=?,bank=?,vat_number=?,bank_account=?,plusgiro=?,iban=?,swift=?," +
                                "delay_interest=?,reminder_fee=?,estimated_delivery=?,taxrate1=?,taxrate2=?," +
                                "taxrate3=?,weight_unit=?,volume_unit=?,currency_code=?,standard_unit=?," +
                                "default_payment_term=?,default_delivery_term=?,default_delivery_way=?,addr_name=?," +
                                "addr_address=?,addr_street=?,addr_zipcode=?,addr_city=?,addr_country=?," +
                                "del_addr_name=?,del_addr_address=?,del_addr_street=?,del_addr_zipcode=?," +
                                "del_addr_city=?,del_addr_country=? WHERE id=?")) {
                    int index = bindCompany(statement, pCompany, 1);
                    statement.setObject(index, pCompany.getId());
                    statement.executeUpdate();
                }
                replaceCompanyStandardTexts(pCompany.getId(), pCompany.getStandardTexts());
                replaceCompanyDefaultAccounts(pCompany.getId(), pCompany.getDefaultAccounts());
                replaceCompanyAutoIncrements(pCompany.getId(), pCompany.getAutoIncrement());
                updateCatalogCompanyName(pCompany.getId(), pCompany.getName());
            } finally {
                setSchema(previousSchema);
            }
            connection.commit();
        } catch (SQLException e) {
            throw handleFailure("update company", e);
        }
    }

    public void delete(SSNewCompany pCompany) {
        if (pCompany == null || isConnectionClosed()) {
            return;
        }
        try {
            if (!SSUtil.isNullOrEmpty(pCompany.getSchemaName())) {
                deleteBySchemaName(pCompany.getSchemaName());
                return;
            }
            if (pCompany.getId() == null) {
                return;
            }
            Optional<CatalogEntry> catalogEntry = findCatalogEntryByCompanyId(pCompany.getId());
            if (catalogEntry.isPresent()) {
                String previousSchema = getCurrentSchema();
                String droppedSchema = catalogEntry.get().schemaName;
                if ("co_0".equalsIgnoreCase(droppedSchema)) {
                    throw new IllegalStateException("The protected company schema 'co_0' cannot be deleted.");
                }
                try {
                    setSchema("PUBLIC");
                    dropSchema(droppedSchema);
                    deleteCatalogRow(catalogEntry.get().companyId);
                } finally {
                    if (previousSchema != null && !previousSchema.equalsIgnoreCase(droppedSchema)) {
                        setSchema(previousSchema);
                    } else {
                        setSchema("PUBLIC");
                    }
                }
                connection.commit();
                return;
            }

            executeDeleteWithCommit("DELETE FROM tbl_company WHERE id=?", pCompany.getId());
        } catch (SQLException e) {
            throw handleFailure("delete company", e);
        }
    }

    public void activateCompany(Integer companyId) {
        if (companyId == null || isConnectionClosed()) {
            return;
        }
        try {
            try (PreparedStatement clear = connection.prepareStatement(
                    "UPDATE PUBLIC.tbl_company_catalog SET is_active=FALSE")) {
                clear.executeUpdate();
            }
            try (PreparedStatement set = connection.prepareStatement(
                    "UPDATE PUBLIC.tbl_company_catalog SET is_active=TRUE WHERE catalog_id=?")) {
                set.setObject(1, companyId);
                set.executeUpdate();
            }
            connection.commit();
        } catch (SQLException e) {
            throw handleFailure("activate company '" + companyId + "'", e);
        }
    }

    public Optional<String> findSchemaNameByCompanyId(Integer companyId) {
        if (companyId == null || isConnectionClosed()) {
            return Optional.empty();
        }
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT schema_name FROM PUBLIC.tbl_company_catalog WHERE catalog_id=?")) {
            statement.setObject(1, companyId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.ofNullable(resultSet.getString("schema_name"));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw handleFailure("find schema for company '" + companyId + "'", e);
        }
    }

    public Optional<String> findActiveCompanyName() {
        if (isConnectionClosed()) {
            return Optional.empty();
        }
        try {
            return findActiveCatalogEntry().map(entry -> entry.companyName);
        } catch (SQLException e) {
            throw handleFailure("find active company", e);
        }
    }

    public void applyActiveCompanySchema() {
        if (isConnectionClosed()) {
            return;
        }
        try {
            Optional<CatalogEntry> activeEntry = findActiveCatalogEntry();
            if (activeEntry.isPresent()) {
                setSchema(activeEntry.get().schemaName);
            } else {
                setSchema("PUBLIC");
            }
        } catch (SQLException e) {
            throw handleFailure("apply active company schema", e);
        }
    }

    public void activateAndApplySchema(Integer companyId) {
        if (companyId == null || isConnectionClosed()) {
            return;
        }
        activateCompany(companyId);
        findSchemaNameByCompanyId(companyId).ifPresent(schemaName -> {
            try {
                setSchema(schemaName);
            } catch (SQLException e) {
                throw new IllegalStateException("Failed to set schema for company '" + companyId + "'.", e);
            }
        });
    }

    public Optional<SSNewCompany> findBySchemaName(String schemaName) {
        if (SSUtil.isNullOrEmpty(schemaName) || isConnectionClosed()) {
            return Optional.empty();
        }
        try {
            Optional<SSNewCompany> company = loadFirstCompanyRow(schemaName);
            if (company.isEmpty()) {
                return Optional.empty();
            }
            Optional<CatalogEntry> catalogEntry = findCatalogEntryBySchemaName(schemaName);
            if (catalogEntry.isPresent()) {
                company.get().setId(catalogEntry.get().companyId);
            } else {
                company.get().setId(null);
            }
            company.get().setSchemaName(schemaName);
            return company;
        } catch (SQLException e) {
            throw handleFailure("find company by schema '" + schemaName + "'", e);
        }
    }

    public SSNewCompany ensureCompanyInSchema(String schemaName, SSNewCompany companyTemplate) {
        if (SSUtil.isNullOrEmpty(schemaName) || companyTemplate == null || isConnectionClosed()) {
            throw new IllegalArgumentException("schemaName and companyTemplate are required");
        }
        try {
            Optional<CatalogEntry> catalogEntry = findCatalogEntryBySchemaName(schemaName);
            if (catalogEntry.isEmpty()) {
                throw new IllegalStateException("No catalog entry found for schema '" + schemaName + "'.");
            }

            CatalogEntry entry = catalogEntry.get();
            String previousSchema = getCurrentSchema();
            try {
                setSchema(schemaName);
                Optional<SSNewCompany> existing = loadFirstCompanyRow(schemaName);
                if (existing.isPresent()) {
                    SSNewCompany merged = existing.get();
                    merged.setId(entry.companyId);
                    merged.setSchemaName(schemaName);
                    merged.setName(companyTemplate.getName());
                    merged.setContactPerson(companyTemplate.getContactPerson());
                    merged.setLogotype(companyTemplate.getLogotype());
                    update(merged);
                    return findBySchemaName(schemaName).orElse(merged);
                }

                insertCompanyWithExplicitId(entry.companyId, companyTemplate);
                updateCatalogCompanyName(entry.companyId, companyTemplate.getName());
                connection.commit();

                SSNewCompany created = findBySchemaName(schemaName).orElseGet(() -> {
                    SSNewCompany fallback = new SSNewCompany();
                    fallback.setId(entry.companyId);
                    fallback.setSchemaName(schemaName);
                    fallback.setName(companyTemplate.getName());
                    fallback.setContactPerson(companyTemplate.getContactPerson());
                    fallback.setLogotype(companyTemplate.getLogotype());
                    return fallback;
                });
                return created;
            } finally {
                setSchema(previousSchema);
            }
        } catch (SQLException e) {
            throw handleFailure("ensure company in schema '" + schemaName + "'", e);
        }
    }

    public void registerOrActivateCompanySchema(String schemaName, String companyName) {
        if (SSUtil.isNullOrEmpty(schemaName) || isConnectionClosed()) {
            return;
        }
        try {
            ensureCatalogRowForSchema(schemaName, companyName);
            Optional<CatalogEntry> entry = findCatalogEntryBySchemaName(schemaName);
            if (entry.isPresent()) {
                activateCompany(entry.get().companyId);
            }
            setSchema(schemaName);
        } catch (SQLException e) {
            throw handleFailure("register or activate schema '" + schemaName + "'", e);
        }
    }

    public void deleteBySchemaName(String schemaName) {
        if (SSUtil.isNullOrEmpty(schemaName) || isConnectionClosed()) {
            return;
        }
        if ("co_0".equalsIgnoreCase(schemaName)) {
            throw new IllegalStateException("The protected company schema 'co_0' cannot be deleted.");
        }
        try {
            String previousSchema = getCurrentSchema();
            try {
                setSchema("PUBLIC");
                dropSchema(schemaName);
                deleteCatalogRowsBySchemaName(schemaName);
            } finally {
                if (previousSchema != null && !previousSchema.equalsIgnoreCase(schemaName)) {
                    setSchema(previousSchema);
                } else {
                    setSchema("PUBLIC");
                }
            }
            connection.commit();
        } catch (SQLException e) {
            throw handleFailure("delete company schema '" + schemaName + "'", e);
        }
    }

    private boolean isConnectionClosed() {
        try {
            return connection.isClosed();
        } catch (SQLException e) {
            throw handleFailure("check database connection", e);
        }
    }

    private void reconcileCatalogAgainstSchemas() throws SQLException {
        Set<String> existingSchemas = new HashSet<>();
        for (String schemaName : listCompanySchemas()) {
            existingSchemas.add(schemaName.toUpperCase());
        }
        for (CatalogEntry catalogEntry : loadCatalogEntries()) {
            if (SSUtil.isNullOrEmpty(catalogEntry.schemaName)) {
                continue;
            }
            if (!existingSchemas.contains(catalogEntry.schemaName.toUpperCase())) {
                LOG.warn("Removing catalog row for missing schema '{}' (catalog id={})",
                        catalogEntry.schemaName, catalogEntry.companyId);
                deleteCatalogRow(catalogEntry.companyId);
            }
        }
        connection.commit();
    }

    private List<String> listCompanySchemas() throws SQLException {
        List<String> schemaNames = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT SCHEMA_NAME FROM INFORMATION_SCHEMA.SCHEMATA "
                        + "WHERE UPPER(SCHEMA_NAME) LIKE 'CO\\_%' ESCAPE '\\' "
                        + "ORDER BY SCHEMA_NAME");
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                schemaNames.add(resultSet.getString("SCHEMA_NAME"));
            }
        }
        return schemaNames;
    }

    private Optional<SSNewCompany> loadCompanyFromSchema(String schemaName, Integer companyIdHint) throws SQLException {
        if (SSUtil.isNullOrEmpty(schemaName)) {
            return Optional.empty();
        }
        final SSNewCompany[] companyHolder = new SSNewCompany[1];
        withSchema(schemaName, () -> {
            try {
                if (companyIdHint != null) {
                    try (PreparedStatement statement = connection.prepareStatement("SELECT * FROM tbl_company WHERE id=?")) {
                        statement.setObject(1, companyIdHint);
                        try (ResultSet resultSet = statement.executeQuery()) {
                            if (resultSet.next()) {
                                companyHolder[0] = mapCompany(resultSet);
                                companyHolder[0].setId(companyIdHint);
                                companyHolder[0].setSchemaName(schemaName);
                                return;
                            }
                        }
                    }
                }

                try (PreparedStatement statement = connection.prepareStatement(
                        "SELECT * FROM tbl_company ORDER BY id FETCH FIRST 1 ROWS ONLY");
                     ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        companyHolder[0] = mapCompany(resultSet);
                        companyHolder[0].setSchemaName(schemaName);
                    }
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
        return Optional.ofNullable(companyHolder[0]);
    }

    private Optional<SSNewCompany> loadFirstCompanyRow(String schemaName) throws SQLException {
        return loadCompanyFromSchema(schemaName, null);
    }

    private SSNewCompany buildAttentionCompanyRow(String schemaName) throws SQLException {
        Optional<SSNewCompany> company = Optional.empty();
        try {
            company = loadFirstCompanyRow(schemaName);
        } catch (RuntimeException e) {
            LOG.warn("Unable to read company row from schema '{}'", schemaName, e);
        }
        SSNewCompany row = company.orElseGet(SSNewCompany::new);
        if (SSUtil.isNullOrEmpty(row.getName())) {
            row.setName(schemaName);
        }
        row.setId(null);
        row.setSchemaName(schemaName);
        row.setNeedsAttention(true);
        row.setCorrupt(company.isEmpty());
        return row;
    }

    private List<CatalogEntry> loadCatalogEntries() throws SQLException {
        List<CatalogEntry> entries = new LinkedList<>();
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT catalog_id,schema_name,company_name,is_active FROM PUBLIC.tbl_company_catalog ORDER BY catalog_id")) {
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    entries.add(new CatalogEntry(
                            resultSet.getInt("catalog_id"),
                            resultSet.getString("schema_name"),
                            resultSet.getString("company_name"),
                            resultSet.getBoolean("is_active")));
                }
            }
        } catch (SQLException e) {
            LOG.debug("Catalog lookup unavailable, fallback to single-schema mode", e);
        }
        return entries;
    }

    private Optional<CatalogEntry> findCatalogEntryByCompanyId(Integer companyId) throws SQLException {
        if (companyId == null) {
            return Optional.empty();
        }
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT catalog_id,schema_name,company_name,is_active FROM PUBLIC.tbl_company_catalog WHERE catalog_id=?")) {
            statement.setObject(1, companyId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(new CatalogEntry(
                            resultSet.getInt("catalog_id"),
                            resultSet.getString("schema_name"),
                            resultSet.getString("company_name"),
                            resultSet.getBoolean("is_active")));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            LOG.debug("Catalog lookup by id unavailable, fallback to single-schema mode", e);
            return Optional.empty();
        }
    }

    private Optional<CatalogEntry> findCatalogEntryByCompanyName(String companyName) throws SQLException {
        if (companyName == null) {
            return Optional.empty();
        }
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT catalog_id,schema_name,company_name,is_active FROM PUBLIC.tbl_company_catalog WHERE company_name=?")) {
            statement.setObject(1, companyName);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(new CatalogEntry(
                            resultSet.getInt("catalog_id"),
                            resultSet.getString("schema_name"),
                            resultSet.getString("company_name"),
                            resultSet.getBoolean("is_active")));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            LOG.debug("Catalog lookup by name unavailable, fallback to single-schema mode", e);
            return Optional.empty();
        }
    }

    private Optional<CatalogEntry> findCatalogEntryBySchemaName(String schemaName) throws SQLException {
        if (schemaName == null) {
            return Optional.empty();
        }
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT catalog_id,schema_name,company_name,is_active FROM PUBLIC.tbl_company_catalog WHERE schema_name=?")) {
            statement.setObject(1, schemaName);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(new CatalogEntry(
                            resultSet.getInt("catalog_id"),
                            resultSet.getString("schema_name"),
                            resultSet.getString("company_name"),
                            resultSet.getBoolean("is_active")));
                }
                return Optional.empty();
            }
        }
    }

    private Optional<CatalogEntry> findActiveCatalogEntry() throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT catalog_id,schema_name,company_name,is_active FROM PUBLIC.tbl_company_catalog "
                        + "WHERE is_active=TRUE ORDER BY catalog_id FETCH FIRST 1 ROWS ONLY");
             ResultSet resultSet = statement.executeQuery()) {
            if (resultSet.next()) {
                return Optional.of(new CatalogEntry(
                        resultSet.getInt("catalog_id"),
                        resultSet.getString("schema_name"),
                        resultSet.getString("company_name"),
                        resultSet.getBoolean("is_active")));
            }
            return Optional.empty();
        } catch (SQLException e) {
            LOG.debug("Active catalog lookup unavailable", e);
            return Optional.empty();
        }
    }

    private void withSchema(String schemaName, Runnable action) throws SQLException {
        String previousSchema = getCurrentSchema();
        try {
            setSchema(schemaName);
            action.run();
        } finally {
            setSchema(previousSchema);
        }
    }

    private String getCurrentSchema() throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("VALUES CURRENT_SCHEMA");
             ResultSet resultSet = statement.executeQuery()) {
            if (resultSet.next()) {
                return resultSet.getString(1);
            }
            return "PUBLIC";
        }
    }

    private void setSchema(String schemaName) throws SQLException {
        String safeSchema = SSUtil.isNullOrEmpty(schemaName) ? "PUBLIC" : schemaName;
        try (Statement statement = connection.createStatement()) {
            statement.execute("SET SCHEMA " + quoteIdentifier(safeSchema));
        }
    }

    private String quoteIdentifier(String identifier) {
        return "\"" + identifier.replace("\"", "\"\"") + "\"";
    }

    private Integer insertCatalogPlaceholder(String companyName) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO PUBLIC.tbl_company_catalog(schema_name,company_name,is_active) VALUES(?,?,FALSE)",
                Statement.RETURN_GENERATED_KEYS)) {
            statement.setObject(1, "PENDING");
            statement.setObject(2, companyName == null ? "" : companyName);
            statement.executeUpdate();
            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1);
                }
            }
        }
        throw new SQLException("Failed to create catalog entry");
    }

    private void updateCatalogSchemaName(Integer companyId, String schemaName) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE PUBLIC.tbl_company_catalog SET schema_name=? WHERE catalog_id=?")) {
            statement.setObject(1, schemaName);
            statement.setObject(2, companyId);
            statement.executeUpdate();
        }
    }

    private void updateCatalogCompanyName(Integer companyId, String companyName) throws SQLException {
        if (companyId == null) {
            return;
        }
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE PUBLIC.tbl_company_catalog SET company_name=? WHERE catalog_id=?")) {
            statement.setObject(1, companyName == null ? "" : companyName);
            statement.setObject(2, companyId);
            statement.executeUpdate();
        } catch (SQLException e) {
            LOG.debug("Catalog company name sync unavailable", e);
        }
    }

    private void ensureCatalogRowForSchema(String schemaName, String companyName) throws SQLException {
        Optional<CatalogEntry> existing = findCatalogEntryBySchemaName(schemaName);
        if (existing.isPresent()) {
            updateCatalogCompanyName(existing.get().companyId, companyName);
            updateCatalogSchemaName(existing.get().companyId, schemaName);
            return;
        }
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO PUBLIC.tbl_company_catalog(schema_name,company_name,is_active) VALUES(?,?,FALSE)")) {
            statement.setObject(1, schemaName);
            statement.setObject(2, companyName == null ? "" : companyName);
            statement.executeUpdate();
        }
    }

    private void deleteCatalogRow(Integer companyId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "DELETE FROM PUBLIC.tbl_company_catalog WHERE catalog_id=?")) {
            statement.setObject(1, companyId);
            statement.executeUpdate();
        }
    }

    private void deleteCatalogRowsBySchemaName(String schemaName) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "DELETE FROM PUBLIC.tbl_company_catalog WHERE schema_name=?")) {
            statement.setObject(1, schemaName);
            statement.executeUpdate();
        }
    }

    private String createSchemaName(Integer companyId) throws SQLException {
        if (companyId == null) {
            throw new IllegalArgumentException("Company id required for schema naming");
        }
        int highestSchemaNumber = 0;
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT schema_name FROM PUBLIC.tbl_company_catalog");
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                String schemaName = resultSet.getString("schema_name");
                if (schemaName == null || !schemaName.startsWith("co_")) {
                    continue;
                }
                String suffix = schemaName.substring(3);
                try {
                    int schemaNumber = Integer.parseInt(suffix);
                    if (schemaNumber > highestSchemaNumber) {
                        highestSchemaNumber = schemaNumber;
                    }
                } catch (NumberFormatException ignored) {
                    // Ignore non-standard schema names and continue scanning known co_N entries.
                }
            }
        }
        int nextSchemaNumber = highestSchemaNumber < 1 ? 1 : highestSchemaNumber + 1;
        return "co_" + nextSchemaNumber;
    }

    private void createSchema(String schemaName) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("CREATE SCHEMA " + quoteIdentifier(schemaName));
        }
    }

    private void dropSchema(String schemaName) throws SQLException {
        if (schemaName == null || "PUBLIC".equalsIgnoreCase(schemaName)) {
            return;
        }
        try (Statement statement = connection.createStatement()) {
            statement.execute("DROP SCHEMA " + quoteIdentifier(schemaName) + " CASCADE");
        }
    }

    private SSNewCompany companyWithId(Integer companyId) {
        SSNewCompany company = new SSNewCompany();
        company.setId(companyId);
        return company;
    }

    private void insertCompanyWithExplicitId(Integer companyId, SSNewCompany company) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO tbl_company(id,name,phone,phone2,telefax,residence,web_address,smtp_address,smtp_name,smtp_port," +
                        "smtp_bcc_addresses,smtp_auth,smtp_connection_security,smtp_username,smtp_password,email," +
                        "contact_person,tax_registered,corporate_id,logotype,swish_image,swish_text,bank,vat_number,bank_account," +
                        "plusgiro,iban,swift,delay_interest,reminder_fee,estimated_delivery,taxrate1,taxrate2," +
                        "taxrate3,weight_unit,volume_unit,currency_code,standard_unit,default_payment_term," +
                        "default_delivery_term,default_delivery_way,addr_name,addr_address,addr_street," +
                        "addr_zipcode,addr_city,addr_country,del_addr_name,del_addr_address,del_addr_street," +
                        "del_addr_zipcode,del_addr_city,del_addr_country) VALUES(" +
                        "?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)")) {
            statement.setObject(1, companyId);
            bindCompany(statement, company, 2);
            statement.executeUpdate();
        }
    }

    private void executeDeleteWithCommit(String sql, Object value) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, value);
            statement.executeUpdate();
        }
        connection.commit();
    }

    private static final class CatalogEntry {
        private final Integer companyId;
        private final String schemaName;
        private final String companyName;
        private final boolean active;

        private CatalogEntry(Integer companyId, String schemaName, String companyName, boolean active) {
            this.companyId = companyId;
            this.schemaName = schemaName;
            this.companyName = companyName;
            this.active = active;
        }
    }

    private SSNewCompany mapCompany(ResultSet resultSet) throws SQLException {
        SSNewCompany company = new SSNewCompany();
        company.setId(resultSet.getInt("id"));
        company.setName(resultSet.getString("name"));
        company.setPhone(resultSet.getString("phone"));
        company.setPhone2(resultSet.getString("phone2"));
        company.setTelefax(resultSet.getString("telefax"));
        company.setResidence(resultSet.getString("residence"));
        company.setHomepage(resultSet.getString("web_address"));
        String smtpAddress = resultSet.getString("smtp_address");
        company.setSMTP(smtpAddress);
        company.setMailServer(mapCompanyMailServer(resultSet, smtpAddress));
        company.setEMail(resultSet.getString("email"));
        company.setContactPerson(resultSet.getString("contact_person"));
        company.setTaxRegistered(resultSet.getBoolean("tax_registered"));
        company.setCorporateID(resultSet.getString("corporate_id"));
        company.setLogotype(resultSet.getString("logotype"));
        company.setSwishImagePath(resultSet.getString("swish_image"));
        company.setSwishText(resultSet.getString("swish_text"));
        company.setBank(resultSet.getString("bank"));
        company.setVATNumber(resultSet.getString("vat_number"));
        company.setBankGiroNumber(resultSet.getString("bank_account"));
        company.setPlusGiroNumber(resultSet.getString("plusgiro"));
        company.setIBAN(resultSet.getString("iban"));
        company.setBIC(resultSet.getString("swift"));
        company.setDelayInterest(resultSet.getBigDecimal("delay_interest"));
        company.setReminderfee(resultSet.getBigDecimal("reminder_fee"));
        company.setEstimatedDelivery(resultSet.getString("estimated_delivery"));
        company.setTaxrate1(resultSet.getBigDecimal("taxrate1"));
        company.setTaxrate2(resultSet.getBigDecimal("taxrate2"));
        company.setTaxrate3(resultSet.getBigDecimal("taxrate3"));
        company.setWeightUnit(resultSet.getString("weight_unit"));
        company.setVolumeUnit(resultSet.getString("volume_unit"));

        String currencyCode = resultSet.getString("currency_code");
        if (currencyCode != null) {
            company.setCurrency(new SSCurrency(currencyCode, currencyCode));
        }

        String unitName = resultSet.getString("standard_unit");
        if (unitName != null) {
            company.setStandardUnit(new SSUnit(unitName, unitName));
        }

        String paymentTerm = resultSet.getString("default_payment_term");
        if (paymentTerm != null) {
            company.setPaymentTerm(V2RepositoryHelpers.resolvePaymentTerm(paymentTerm));
        }

        String deliveryTerm = resultSet.getString("default_delivery_term");
        if (deliveryTerm != null) {
            company.setDeliveryTerm(new SSDeliveryTerm(deliveryTerm, deliveryTerm));
        }

        String deliveryWay = resultSet.getString("default_delivery_way");
        if (deliveryWay != null) {
            company.setDeliveryWay(new SSDeliveryWay(deliveryWay, deliveryWay));
        }

        company.setAddress(V2RepositoryHelpers.mapAddress(resultSet, "addr"));
        company.setDeliveryAddress(V2RepositoryHelpers.mapAddress(resultSet, "del_addr"));
        company.setStandardTexts(loadCompanyStandardTexts(company.getId()));
        company.setDefaultAccounts(loadCompanyDefaultAccounts(company.getId()));
        loadCompanyAutoIncrements(company.getId(), company.getAutoIncrement());
        return company;
    }

    private SSMailServer mapCompanyMailServer(ResultSet resultSet, String smtpAddress) throws SQLException {
        if (SSUtil.isNullOrEmpty(smtpAddress)) {
            return null;
        }

        Integer port = (Integer) resultSet.getObject("smtp_port");
        if (port == null || port <= 0) {
            port = 25;
        }

        boolean auth = resultSet.getBoolean("smtp_auth");
        if (resultSet.wasNull()) {
            auth = false;
        }

        String connectionSecurityText = resultSet.getString("smtp_connection_security");
        ConnectionSecurity connectionSecurity = ConnectionSecurity.NONE;
        if (!SSUtil.isNullOrEmpty(connectionSecurityText)) {
            try {
                connectionSecurity = ConnectionSecurity.valueOf(connectionSecurityText);
            } catch (IllegalArgumentException ignored) {
                connectionSecurity = ConnectionSecurity.NONE;
            }
        }

        String name = resultSet.getString("smtp_name");
        if (SSUtil.isNullOrEmpty(name)) {
            name = "NONAME";
        }

        String username = resultSet.getString("smtp_username");
        String password = resultSet.getString("smtp_password");
        if (auth) {
            if (username == null) {
                username = "";
            }
            if (password == null) {
                password = "";
            }
        }

        try {
            URI uri = new URI(null, null, smtpAddress, port, null, null, null);
            return new SSMailServer(
                    name,
                    uri,
                    resultSet.getString("smtp_bcc_addresses"),
                    auth,
                    connectionSecurity,
                    username,
                    password);
        } catch (URISyntaxException e) {
            LOG.warn("Invalid SMTP host '{}' for company mail server", smtpAddress, e);
            return null;
        }
    }

    private int bindCompany(PreparedStatement statement, SSNewCompany company, int index) throws SQLException {
        SSNewCompany safeCompany = company == null ? new SSNewCompany() : company;
        SSCompanyValidationRules.validateCompanyStringLengths(safeCompany);
        String swift = SSCompanyValidationRules.normalizeSwift(safeCompany.getBIC());
        safeCompany.setBIC(swift);

        SSMailServer mailServer = safeCompany.getMailServer();
        String smtpHost = safeCompany.getSMTP();
        String smtpName = null;
        Integer smtpPort = null;
        String smtpBcc = null;
        boolean smtpAuth = false;
        String smtpConnectionSecurity = null;
        String smtpUsername = null;
        String smtpPassword = null;

        if (mailServer != null) {
            if (mailServer.getURI() != null) {
                String hostFromServer = mailServer.getURI().getHost();
                if (!SSUtil.isNullOrEmpty(hostFromServer)) {
                    smtpHost = hostFromServer;
                }
                int portFromServer = mailServer.getURI().getPort();
                if (portFromServer > 0) {
                    smtpPort = portFromServer;
                }
            }

            smtpName = mailServer.getName();
            smtpBcc = mailServer.getBccAddresses();
            smtpAuth = mailServer.isAuth();
            ConnectionSecurity connectionSecurity = mailServer.getConnectionSecurity();
            smtpConnectionSecurity = connectionSecurity == null ? null : connectionSecurity.name();
            smtpUsername = mailServer.getUsername();
            smtpPassword = mailServer.getPassword();
        }

        safeCompany.setSMTP(smtpHost);

        statement.setObject(index++, safeCompany.getName());
        statement.setObject(index++, safeCompany.getPhone());
        statement.setObject(index++, safeCompany.getPhone2());
        statement.setObject(index++, safeCompany.getTelefax());
        statement.setObject(index++, safeCompany.getResidence());
        statement.setObject(index++, safeCompany.getHomepage());
        statement.setObject(index++, safeCompany.getSMTP());
        statement.setObject(index++, smtpName);
        statement.setObject(index++, smtpPort);
        statement.setObject(index++, smtpBcc);
        statement.setBoolean(index++, smtpAuth);
        statement.setObject(index++, smtpConnectionSecurity);
        statement.setObject(index++, smtpUsername);
        statement.setObject(index++, smtpPassword);
        statement.setObject(index++, safeCompany.getEMail());
        statement.setObject(index++, safeCompany.getContactPerson());
        statement.setBoolean(index++, safeCompany.getTaxRegistered());
        statement.setObject(index++, safeCompany.getCorporateID());
        statement.setObject(index++, safeCompany.getLogotype());
        statement.setObject(index++, safeCompany.getSwishImagePath());
        statement.setObject(index++, safeCompany.getSwishText());
        statement.setObject(index++, safeCompany.getBank());
        statement.setObject(index++, safeCompany.getVATNumber());
        statement.setObject(index++, safeCompany.getBankGiroNumber());
        statement.setObject(index++, safeCompany.getPlusGiroNumber());
        statement.setObject(index++, safeCompany.getIBAN());
        statement.setObject(index++, swift);
        statement.setObject(index++, safeCompany.getDelayInterest());
        statement.setObject(index++, safeCompany.getReminderfee());
        statement.setObject(index++, safeCompany.getEstimatedDelivery());
        statement.setObject(index++, safeCompany.getTaxRate1());
        statement.setObject(index++, safeCompany.getTaxRate2());
        statement.setObject(index++, safeCompany.getTaxRate3());
        statement.setObject(index++, safeCompany.getWeightUnit());
        statement.setObject(index++, safeCompany.getVolumeUnit());
        statement.setObject(index++, safeCompany.getCurrency() == null ? null : safeCompany.getCurrency().getName());
        statement.setObject(index++, safeCompany.getStandardUnit() == null ? null : safeCompany.getStandardUnit().getName());
        statement.setObject(index++, safeCompany.getPaymentTerm() == null ? null : safeCompany.getPaymentTerm().getName());
        statement.setObject(index++, safeCompany.getDeliveryTerm() == null ? null : safeCompany.getDeliveryTerm().getName());
        statement.setObject(index++, safeCompany.getDeliveryWay() == null ? null : safeCompany.getDeliveryWay().getName());

        index = V2RepositoryHelpers.bindAddress(statement, index, safeCompany.getAddress());
        index = V2RepositoryHelpers.bindAddress(statement, index, safeCompany.getDeliveryAddress());
        return index;
    }

    private SSAddress mapAddress(ResultSet resultSet, String prefix) throws SQLException {
        SSAddress address = new SSAddress();
        address.setName(resultSet.getString(prefix + "_name"));
        address.setAddress1(resultSet.getString(prefix + "_address"));
        address.setAddress2(resultSet.getString(prefix + "_street"));
        address.setZipCode(resultSet.getString(prefix + "_zipcode"));
        address.setCity(resultSet.getString(prefix + "_city"));
        address.setCountry(resultSet.getString(prefix + "_country"));
        return address;
    }

    private int bindAddress(PreparedStatement statement, int index, SSAddress address) throws SQLException {
        SSAddress safeAddress = address == null ? new SSAddress() : address;
        statement.setObject(index++, safeAddress.getName());
        statement.setObject(index++, safeAddress.getAddress1());
        statement.setObject(index++, safeAddress.getAddress2());
        statement.setObject(index++, safeAddress.getZipCode());
        statement.setObject(index++, safeAddress.getCity());
        statement.setObject(index++, safeAddress.getCountry());
        return index;
    }

    private Map<SSStandardText, String> loadCompanyStandardTexts(Integer companyId) throws SQLException {
        Map<SSStandardText, String> texts = new HashMap<>();
        if (companyId == null) {
            return texts;
        }
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT text_type,text_value FROM tbl_company_standard_text WHERE company_id=?")) {
            statement.setObject(1, companyId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    String type = resultSet.getString("text_type");
                    if (type == null) {
                        continue;
                    }
                    try {
                        texts.put(SSStandardText.valueOf(type), resultSet.getString("text_value"));
                    } catch (IllegalArgumentException ignored) {
                        // Ignore unknown text keys to keep backward compatibility with older data.
                    }
                }
            }
        }
        return texts;
    }

    private Map<SSDefaultAccount, Integer> loadCompanyDefaultAccounts(Integer companyId) throws SQLException {
        Map<SSDefaultAccount, Integer> accounts = new HashMap<>();
        if (companyId == null) {
            return accounts;
        }
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT account_type,account_nr FROM tbl_company_default_account WHERE company_id=?")) {
            statement.setObject(1, companyId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    String type = resultSet.getString("account_type");
                    if (type == null) {
                        continue;
                    }
                    try {
                        accounts.put(SSDefaultAccount.valueOf(type), (Integer) resultSet.getObject("account_nr"));
                    } catch (IllegalArgumentException ignored) {
                        // Ignore unknown account keys to keep backward compatibility with older data.
                    }
                }
            }
        }
        return accounts;
    }

    private void loadCompanyAutoIncrements(Integer companyId, SSAutoIncrement autoIncrement) throws SQLException {
        if (companyId == null || autoIncrement == null) {
            return;
        }
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT counter_key,next_number FROM tbl_company_autoincrement WHERE company_id=?")) {
            statement.setObject(1, companyId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    String key = resultSet.getString("counter_key");
                    Integer number = (Integer) resultSet.getObject("next_number");
                    if (key != null && number != null) {
                        autoIncrement.setNumber(key, number);
                    }
                }
            }
        }
    }

    private void replaceCompanyStandardTexts(Integer companyId, Map<SSStandardText, String> texts) throws SQLException {
        if (companyId == null) {
            return;
        }
        try (PreparedStatement delete = connection.prepareStatement(
                "DELETE FROM tbl_company_standard_text WHERE company_id=?")) {
            delete.setObject(1, companyId);
            delete.executeUpdate();
        }
        if (texts == null || texts.isEmpty()) {
            return;
        }
        try (PreparedStatement insert = connection.prepareStatement(
                "INSERT INTO tbl_company_standard_text(company_id,text_type,text_value) VALUES(?,?,?)")) {
            for (Map.Entry<SSStandardText, String> entry : texts.entrySet()) {
                if (entry.getKey() == null) {
                    continue;
                }
                insert.setObject(1, companyId);
                insert.setObject(2, entry.getKey().name());
                insert.setObject(3, entry.getValue());
                insert.executeUpdate();
            }
        }
    }

    private void replaceCompanyDefaultAccounts(Integer companyId, Map<SSDefaultAccount, Integer> accounts)
            throws SQLException {
        if (companyId == null) {
            return;
        }
        try (PreparedStatement delete = connection.prepareStatement(
                "DELETE FROM tbl_company_default_account WHERE company_id=?")) {
            delete.setObject(1, companyId);
            delete.executeUpdate();
        }
        if (accounts == null || accounts.isEmpty()) {
            return;
        }
        try (PreparedStatement insert = connection.prepareStatement(
                "INSERT INTO tbl_company_default_account(company_id,account_type,account_nr) VALUES(?,?,?)")) {
            for (Map.Entry<SSDefaultAccount, Integer> entry : accounts.entrySet()) {
                if (entry.getKey() == null || entry.getValue() == null) {
                    continue;
                }
                insert.setObject(1, companyId);
                insert.setObject(2, entry.getKey().name());
                insert.setObject(3, entry.getValue());
                insert.executeUpdate();
            }
        }
    }

    private void replaceCompanyAutoIncrements(Integer companyId, SSAutoIncrement autoIncrement) throws SQLException {
        if (companyId == null || autoIncrement == null) {
            return;
        }
        SSCompanyValidationRules.validateAutoIncrementsNonNegative(autoIncrement, COMPANY_AUTO_INCREMENT_KEYS);
        try (PreparedStatement delete = connection.prepareStatement(
                "DELETE FROM tbl_company_autoincrement WHERE company_id=?")) {
            delete.setObject(1, companyId);
            delete.executeUpdate();
        }
        try (PreparedStatement insert = connection.prepareStatement(
                "INSERT INTO tbl_company_autoincrement(company_id,counter_key,next_number) VALUES(?,?,?)")) {
            for (String key : COMPANY_AUTO_INCREMENT_KEYS) {
                int number = autoIncrement.getNumber(key);
                if (number <= 0) {
                    continue;
                }
                insert.setObject(1, companyId);
                insert.setObject(2, key);
                insert.setObject(3, number);
                insert.executeUpdate();
            }
        }
    }

    private RuntimeException handleFailure(String operation, SQLException cause) {
        LOG.error("Failed to {}", operation, cause);
        try {
            rollbackHandler.rollbackCurrentTransaction();
        } catch (SQLException rollbackException) {
            cause.addSuppressed(rollbackException);
            LOG.error("Failed to rollback company transaction after {}", operation, rollbackException);
        }
        return new IllegalStateException("Failed to " + operation + ".", cause);
    }
}
