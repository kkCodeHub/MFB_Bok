package se.swedsoft.bookkeeping.persistence.v2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.swedsoft.bookkeeping.data.SSNewCompany;
import se.swedsoft.bookkeeping.data.SSVoucherTemplate;
import se.swedsoft.bookkeeping.data.system.SSEventTriggerSyncContext;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Full SQL V2 repository for the VoucherTemplate domain.
 */
public class V2VoucherTemplateRepository {

    @FunctionalInterface
    public interface RollbackHandler {
        void rollbackCurrentTransaction() throws SQLException;
    }

    private static final Logger LOG = LoggerFactory.getLogger(V2VoucherTemplateRepository.class);

    private final Connection connection;
    private final Supplier<SSNewCompany> currentCompanySupplier;
    private final RollbackHandler rollbackHandler;

    public V2VoucherTemplateRepository(Connection connection,
                                       Supplier<SSNewCompany> currentCompanySupplier,
                                       RollbackHandler rollbackHandler) {
        if (connection == null) {
            throw new NullPointerException("connection must not be null");
        }
        if (currentCompanySupplier == null) {
            throw new NullPointerException("currentCompanySupplier must not be null");
        }
        if (rollbackHandler == null) {
            throw new NullPointerException("rollbackHandler must not be null");
        }
        this.connection = connection;
        this.currentCompanySupplier = currentCompanySupplier;
        this.rollbackHandler = rollbackHandler;
    }

    public List<SSVoucherTemplate> findAll() {
        SSNewCompany currentCompany = currentCompanySupplier.get();
        if (currentCompany == null || currentCompany.getId() == null) {
            return Collections.emptyList();
        }

        try {
            List<SSVoucherTemplate> templates = new LinkedList<>();
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT * FROM tbl_vouchertemplate WHERE companyid=?")) {
                statement.setObject(1, currentCompany.getId());
                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        templates.add(mapVoucherTemplateV2(resultSet));
                    }
                }
            }
            return templates;
        } catch (SQLException e) {
            throw handleFailure("load voucher templates", e);
        }
    }

    public List<SSVoucherTemplate> findAll(List<SSVoucherTemplate> subset) {
        if (subset == null) {
            return Collections.emptyList();
        }
        SSNewCompany currentCompany = currentCompanySupplier.get();
        if (currentCompany == null || currentCompany.getId() == null) {
            return Collections.emptyList();
        }

        try {
            List<SSVoucherTemplate> templates = new LinkedList<>();
            for (SSVoucherTemplate candidate : subset) {
                if (candidate == null || candidate.getDescription() == null) {
                    continue;
                }
                try (PreparedStatement statement = connection.prepareStatement(
                        "SELECT * FROM tbl_vouchertemplate WHERE name=? AND companyid=?")) {
                    statement.setObject(1, candidate.getDescription());
                    statement.setObject(2, currentCompany.getId());
                    try (ResultSet resultSet = statement.executeQuery()) {
                        if (resultSet.next()) {
                            templates.add(mapVoucherTemplateV2(resultSet));
                        }
                    }
                }
            }
            return templates;
        } catch (SQLException e) {
            throw handleFailure("filter voucher templates", e);
        }
    }

    public void add(SSVoucherTemplate voucherTemplate) {
        if (voucherTemplate == null) {
            return;
        }
        SSNewCompany currentCompany = currentCompanySupplier.get();
        if (currentCompany == null || currentCompany.getId() == null) {
            return;
        }

        try {
            Integer templateId = null;
            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO tbl_vouchertemplate(name,companyid,description,template_date) VALUES(?,?,?,?)",
                    Statement.RETURN_GENERATED_KEYS)) {
                statement.setObject(1, voucherTemplate.getDescription());
                statement.setObject(2, currentCompany.getId());
                statement.setObject(3, voucherTemplate.getDescription());
                if (voucherTemplate.getLocalDateTime() == null) {
                    statement.setNull(4, Types.TIMESTAMP);
                } else {
                    statement.setTimestamp(4, Timestamp.valueOf(voucherTemplate.getLocalDateTime()));
                }
                statement.executeUpdate();
                try (ResultSet keys = statement.getGeneratedKeys()) {
                    if (keys.next()) {
                        templateId = keys.getInt(1);
                    }
                }
            }

            if (templateId == null) {
                templateId = getVoucherTemplateIdV2(voucherTemplate.getDescription(), currentCompany.getId());
            }
            if (templateId != null) {
                replaceVoucherTemplateRowsV2(templateId, voucherTemplate);
            }

            connection.commit();
            SSEventTriggerSyncContext.triggerAction(
                    "NEWVOUCHERTEMPLATE", "TBL_VOUCHERTEMPLATE", voucherTemplate.getDescription());
        } catch (SQLException e) {
            throw handleFailure("add voucher template '" + voucherTemplate.getDescription() + "'", e);
        }
    }

    public void delete(SSVoucherTemplate voucherTemplate) {
        if (voucherTemplate == null) {
            return;
        }
        SSNewCompany currentCompany = currentCompanySupplier.get();
        if (currentCompany == null || currentCompany.getId() == null) {
            return;
        }

        try {
            Integer templateId = getVoucherTemplateIdV2(voucherTemplate.getDescription(), currentCompany.getId());
            if (templateId != null) {
                try (PreparedStatement deleteRows = connection.prepareStatement(
                        "DELETE FROM tbl_vouchertemplate_row WHERE vouchertemplate_id=?")) {
                    deleteRows.setObject(1, templateId);
                    deleteRows.executeUpdate();
                }
            }

            try (PreparedStatement statement = connection.prepareStatement(
                    "DELETE FROM tbl_vouchertemplate WHERE name=? AND companyid=?")) {
                statement.setObject(1, voucherTemplate.getDescription());
                statement.setObject(2, currentCompany.getId());
                statement.executeUpdate();
            }

            connection.commit();
            SSEventTriggerSyncContext.triggerAction(
                    "DELETEVOUCHERTEMPLATE", "TBL_VOUCHERTEMPLATE", voucherTemplate.getDescription());
        } catch (SQLException e) {
            throw handleFailure("delete voucher template '" + voucherTemplate.getDescription() + "'", e);
        }
    }

    private List<SSVoucherTemplate.SSVoucherTemplateRow> getVoucherTemplateRowsV2(Integer templateId) throws SQLException {
        List<SSVoucherTemplate.SSVoucherTemplateRow> rows = new LinkedList<>();
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT * FROM tbl_vouchertemplate_row WHERE vouchertemplate_id=? ORDER BY id")) {
            statement.setObject(1, templateId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    SSVoucherTemplate.SSVoucherTemplateRow row = new SSVoucherTemplate.SSVoucherTemplateRow();
                    row.setAccountNr((Integer) resultSet.getObject("account_nr"));
                    Boolean debet = (Boolean) resultSet.getObject("is_debet");
                    if (Boolean.TRUE.equals(debet)) {
                        row.setDebet(BigDecimal.ZERO);
                    } else {
                        row.setCredit(BigDecimal.ZERO);
                    }
                    rows.add(row);
                }
            }
        }
        return rows;
    }

    private void replaceVoucherTemplateRowsV2(Integer templateId, SSVoucherTemplate template) throws SQLException {
        try (PreparedStatement delete = connection.prepareStatement(
                "DELETE FROM tbl_vouchertemplate_row WHERE vouchertemplate_id=?")) {
            delete.setObject(1, templateId);
            delete.executeUpdate();
        }

        for (SSVoucherTemplate.SSVoucherTemplateRow row : template.getRows()) {
            try (PreparedStatement insert = connection.prepareStatement(
                    "INSERT INTO tbl_vouchertemplate_row(vouchertemplate_id,account_nr,is_debet) VALUES(?,?,?)")) {
                insert.setObject(1, templateId);
                insert.setObject(2, row.getAccountNr());
                insert.setObject(3, row.getDebet() != null);
                insert.executeUpdate();
            }
        }
    }

    private Integer getVoucherTemplateIdV2(String name, Integer companyId) throws SQLException {
        if (name == null || companyId == null) {
            return null;
        }
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT id FROM tbl_vouchertemplate WHERE name=? AND companyid=?")) {
            statement.setObject(1, name);
            statement.setObject(2, companyId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt(1);
                }
                return null;
            }
        }
    }

    private SSVoucherTemplate mapVoucherTemplateV2(ResultSet resultSet) throws SQLException {
        SSVoucherTemplate template = new SSVoucherTemplate();
        String name = resultSet.getString("name");
        String description = resultSet.getString("description");
        template.setDescription(name == null ? description : name);

        Timestamp templateDate = resultSet.getTimestamp("template_date");
        if (templateDate != null) {
            template.setLocalDateTime(templateDate.toLocalDateTime());
        }

        template.getRows().clear();
        template.getRows().addAll(getVoucherTemplateRowsV2(resultSet.getInt("id")));
        return template;
    }

    private RuntimeException handleFailure(String operation, SQLException cause) {
        LOG.error("Failed to {}", operation, cause);
        try {
            rollbackHandler.rollbackCurrentTransaction();
        } catch (SQLException rollbackException) {
            cause.addSuppressed(rollbackException);
            LOG.error("Failed to rollback voucher-template transaction after {}", operation, rollbackException);
        }
        return new IllegalStateException("Failed to " + operation + ".", cause);
    }
}



