package se.swedsoft.bookkeeping.persistence.v2;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.SQLException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.swedsoft.bookkeeping.data.SSNewCompany;
import se.swedsoft.bookkeeping.data.SSSupplier;
import se.swedsoft.bookkeeping.data.common.SSCurrency;
import se.swedsoft.bookkeeping.data.common.SSDeliveryTerm;
import se.swedsoft.bookkeeping.data.common.SSDeliveryWay;
import se.swedsoft.bookkeeping.data.common.SSPaymentTerm;
import se.swedsoft.bookkeeping.data.system.SSEventTriggerSyncContext;

/**
 * V2 supplier repository backed by the normalized V2 schema.
 *
 * <p>Reads and writes suppliers directly against {@code tbl_supplier}.
 * Rollback handling is delegated through an injected handler.</p>
 */
public class V2SupplierRepository {

    @FunctionalInterface
    public interface RollbackHandler {
        void rollbackCurrentTransaction() throws SQLException;
    }

    private static final Logger LOG = LoggerFactory.getLogger(V2SupplierRepository.class);

    private final Connection connection;
    private final Supplier<SSNewCompany> currentCompany;
    private final RollbackHandler rollbackHandler;

    /**
     * Creates a V2 supplier repository with explicit DB dependencies.
     *
     * @param connection      the active database connection; must not be {@code null}
     * @param currentCompany  supplier for the current company; must not be {@code null}
     * @param rollbackHandler rollback delegate; must not be {@code null}
     */
    public V2SupplierRepository(Connection connection,
                                Supplier<SSNewCompany> currentCompany,
                                RollbackHandler rollbackHandler) {
        if (connection == null) {
            throw new NullPointerException("connection must not be null");
        }
        if (currentCompany == null) {
            throw new NullPointerException("currentCompany must not be null");
        }
        if (rollbackHandler == null) {
            throw new NullPointerException("rollbackHandler must not be null");
        }
        this.connection = connection;
        this.currentCompany = currentCompany;
        this.rollbackHandler = rollbackHandler;
    }

    /**
     * Returns all suppliers for the current company.
     *
     * @return list of suppliers; never {@code null}
     */
    public List<SSSupplier> findAll() {
        SSNewCompany company = currentCompany.get();
        if (company == null) {
            return Collections.emptyList();
        }
        try {
            List<SSSupplier> list = new LinkedList<>();
            Integer iMax = -1;
            while (true) {
                try (PreparedStatement st = connection.prepareStatement(
                        "SELECT * FROM tbl_supplier WHERE companyid=? AND id>?")) {
                    st.setObject(1, company.getId());
                    st.setObject(2, iMax);
                    st.setMaxRows(1024);
                    try (ResultSet rs = st.executeQuery()) {
                        int count = 0;
                        while (rs.next()) {
                            iMax = rs.getInt(1);
                            list.add(mapSupplier(rs));
                            count++;
                        }
                        if (count != 1024) {
                            break;
                        }
                    }
                }
            }
            return list;
        } catch (SQLException e) {
            throw handleFailure("load suppliers", e);
        }
    }

    /**
     * Finds a supplier matching the given supplier's number.
     *
     * @param supplier the supplier to look up
     * @return optional supplier
     */
    public Optional<SSSupplier> findBySupplier(SSSupplier supplier) {
        if (supplier == null) {
            return Optional.empty();
        }
        SSNewCompany company = currentCompany.get();
        if (company == null) {
            return Optional.empty();
        }
        try {
            try (PreparedStatement st = connection.prepareStatement(
                    "SELECT * FROM tbl_supplier WHERE number=? AND companyid=?")) {
                st.setObject(1, supplier.getNumber());
                st.setObject(2, company.getId());
                try (ResultSet rs = st.executeQuery()) {
                    if (rs.next()) {
                        return Optional.of(mapSupplier(rs));
                    }
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw handleFailure("find supplier '" + supplier.getNumber() + "'", e);
        }
    }

    /**
     * Filters a list of suppliers against those that exist in the database.
     *
     * @param suppliers the subset to filter
     * @return filtered list; never {@code null}
     */
    public List<SSSupplier> findAll(List<SSSupplier> suppliers) {
        if (suppliers == null) {
            return Collections.emptyList();
        }
        SSNewCompany company = currentCompany.get();
        if (company == null) {
            return Collections.emptyList();
        }
        try {
            List<SSSupplier> result = new LinkedList<>();
            for (SSSupplier s : suppliers) {
                try (PreparedStatement st = connection.prepareStatement(
                        "SELECT * FROM tbl_supplier WHERE number=? AND companyid=?")) {
                    st.setObject(1, s.getNumber());
                    st.setObject(2, company.getId());
                    try (ResultSet rs = st.executeQuery()) {
                        if (rs.next()) {
                            result.add(mapSupplier(rs));
                        }
                    }
                }
            }
            return result;
        } catch (SQLException e) {
            throw handleFailure("filter suppliers", e);
        }
    }

    /**
     * Inserts a new supplier.
     *
     * @param supplier the supplier to add; ignored if {@code null}
     */
    public void add(SSSupplier supplier) {
        if (supplier == null) {
            return;
        }
        SSNewCompany company = currentCompany.get();
        if (company == null) {
            return;
        }
        try {
            try (PreparedStatement st = connection.prepareStatement(
                    "INSERT INTO tbl_supplier(" +
                            "number,companyid,name,phone,phone2,telefax,email,homepage," +
                            "registration_number,your_contact,our_contact,our_customer_nr," +
                            "bankgiro,plusgiro,outpayment_number,comment,currency_code,payment_term," +
                            "delivery_term,delivery_way,addr_name,addr_address,addr_street,addr_zipcode," +
                            "addr_city,addr_country) " +
                            "VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)")) {
                int i = bindSupplier(st, 1, supplier, company);
                st.executeUpdate();
                connection.commit();
                SSEventTriggerSyncContext.triggerAction("NEWSUPPLIER", "TBL_SUPPLIER", supplier.getNumber());
            }
        } catch (SQLException e) {
            throw handleFailure("add supplier '" + supplier.getNumber() + "'", e);
        }
    }

    /**
     * Updates an existing supplier.
     *
     * @param supplier the supplier to update; ignored if {@code null}
     */
    public void update(SSSupplier supplier) {
        if (supplier == null) {
            return;
        }
        SSNewCompany company = currentCompany.get();
        if (company == null) {
            return;
        }
        try {
            try (PreparedStatement st = connection.prepareStatement(
                    "UPDATE tbl_supplier SET " +
                            "name=?,phone=?,phone2=?,telefax=?,email=?,homepage=?,registration_number=?," +
                            "your_contact=?,our_contact=?,our_customer_nr=?,bankgiro=?,plusgiro=?," +
                            "outpayment_number=?,comment=?,currency_code=?,payment_term=?,delivery_term=?," +
                            "delivery_way=?,addr_name=?,addr_address=?,addr_street=?,addr_zipcode=?," +
                            "addr_city=?,addr_country=? WHERE number=? AND companyid=?")) {
                int i = 1;
                st.setObject(i++, supplier.getName());
                st.setObject(i++, supplier.getPhone1());
                st.setObject(i++, supplier.getPhone2());
                st.setObject(i++, supplier.getTelefax());
                st.setObject(i++, supplier.getEMail());
                st.setObject(i++, supplier.getHomepage());
                st.setObject(i++, supplier.getRegistrationNumber());
                st.setObject(i++, supplier.getYourContact());
                st.setObject(i++, supplier.getOurContact());
                st.setObject(i++, supplier.getOurCustomerNr());
                st.setObject(i++, supplier.getBankgiro());
                st.setObject(i++, supplier.getPlusgiro());
                st.setObject(i++, supplier.getOutpaymentNumber());
                st.setObject(i++, supplier.getComment());
                st.setObject(i++, currencyCode(supplier));
                st.setObject(i++, supplier.getPaymentTerm() == null ? null : supplier.getPaymentTerm().getName());
                st.setObject(i++, supplier.getDeliveryTerm() == null ? null : supplier.getDeliveryTerm().getName());
                st.setObject(i++, supplier.getDeliveryWay() == null ? null : supplier.getDeliveryWay().getName());
                i = V2RepositoryHelpers.bindAddress(st, i, supplier.getAddress());
                st.setObject(i++, supplier.getNumber());
                st.setObject(i, company.getId());
                st.executeUpdate();
                connection.commit();
                SSEventTriggerSyncContext.triggerAction("EDITSUPPLIER", "TBL_SUPPLIER", supplier.getNumber());
            }
        } catch (SQLException e) {
            throw handleFailure("update supplier '" + supplier.getNumber() + "'", e);
        }
    }

    /**
     * Deletes a supplier.
     *
     * @param supplier the supplier to delete; ignored if {@code null}
     */
    public void delete(SSSupplier supplier) {
        if (supplier == null) {
            return;
        }
        SSNewCompany company = currentCompany.get();
        if (company == null) {
            return;
        }
        try {
            try (PreparedStatement st = connection.prepareStatement(
                    "DELETE FROM tbl_supplier WHERE number=? AND companyid=?")) {
                st.setObject(1, supplier.getNumber());
                st.setObject(2, company.getId());
                st.executeUpdate();
                connection.commit();
                SSEventTriggerSyncContext.triggerAction("DELETESUPPLIER", "TBL_SUPPLIER", supplier.getNumber());
            }
        } catch (SQLException e) {
            if (isForeignKeyConstraintViolation(e)) {
                LOG.warn("Delete blocked for supplier '{}' because it is in use", supplier.getNumber());
                return;
            }
            throw handleFailure("delete supplier '" + supplier.getNumber() + "'", e);
        }
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private SSSupplier mapSupplier(ResultSet rs) throws SQLException {
        SSSupplier s = new SSSupplier();
        s.setNumber(rs.getString("number"));
        s.setName(rs.getString("name"));
        s.setPhone1(rs.getString("phone"));
        s.setPhone2(rs.getString("phone2"));
        s.setTelefax(rs.getString("telefax"));
        s.setEMail(rs.getString("email"));
        s.setHomepage(rs.getString("homepage"));
        s.setRegistrationNumber(rs.getString("registration_number"));
        s.setYourContact(rs.getString("your_contact"));
        s.setOurContact(rs.getString("our_contact"));
        s.setOurCustomerNr(rs.getString("our_customer_nr"));
        s.setBankGiro(rs.getString("bankgiro"));
        s.setPlusGiro(rs.getString("plusgiro"));
        s.setOutpaymentNumber((Integer) rs.getObject("outpayment_number"));
        s.setComment(rs.getString("comment"));

        String currencyCode = rs.getString("currency_code");
        if (currencyCode != null) {
            s.setCurrency(new SSCurrency(currencyCode, currencyCode));
        }
        String paymentTerm = rs.getString("payment_term");
        if (paymentTerm != null) {
            s.setPaymentTerm(V2RepositoryHelpers.resolvePaymentTerm(paymentTerm));
        }
        String deliveryTerm = rs.getString("delivery_term");
        if (deliveryTerm != null) {
            s.setDeliveryTerm(new SSDeliveryTerm(deliveryTerm, deliveryTerm));
        }
        String deliveryWay = rs.getString("delivery_way");
        if (deliveryWay != null) {
            s.setDeliveryWay(new SSDeliveryWay(deliveryWay, deliveryWay));
        }
        s.setAddress(V2RepositoryHelpers.mapAddress(rs, "addr"));
        return s;
    }

    private int bindSupplier(PreparedStatement st, int i, SSSupplier supplier, SSNewCompany company)
            throws SQLException {
        st.setObject(i++, supplier.getNumber());
        st.setObject(i++, company.getId());
        st.setObject(i++, supplier.getName());
        st.setObject(i++, supplier.getPhone1());
        st.setObject(i++, supplier.getPhone2());
        st.setObject(i++, supplier.getTelefax());
        st.setObject(i++, supplier.getEMail());
        st.setObject(i++, supplier.getHomepage());
        st.setObject(i++, supplier.getRegistrationNumber());
        st.setObject(i++, supplier.getYourContact());
        st.setObject(i++, supplier.getOurContact());
        st.setObject(i++, supplier.getOurCustomerNr());
        st.setObject(i++, supplier.getBankgiro());
        st.setObject(i++, supplier.getPlusgiro());
        st.setObject(i++, supplier.getOutpaymentNumber());
        st.setObject(i++, supplier.getComment());
        st.setObject(i++, currencyCode(supplier));
        st.setObject(i++, supplier.getPaymentTerm() == null ? null : supplier.getPaymentTerm().getName());
        st.setObject(i++, supplier.getDeliveryTerm() == null ? null : supplier.getDeliveryTerm().getName());
        st.setObject(i++, supplier.getDeliveryWay() == null ? null : supplier.getDeliveryWay().getName());
        i = V2RepositoryHelpers.bindAddress(st, i, supplier.getAddress());
        return i;
    }

    private String currencyCode(SSSupplier supplier) {
        try {
            SSCurrency c = supplier.getCurrency();
            return c == null ? null : c.getName();
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private boolean isForeignKeyConstraintViolation(SQLException e) {
        for (Throwable current = e; current != null; current = current.getCause()) {
            if (current instanceof SQLIntegrityConstraintViolationException) {
                return true;
            }
            if (current instanceof SQLException) {
                String state = ((SQLException) current).getSQLState();
                if (state != null && state.startsWith("23")) {
                    return true;
                }
            }
        }
        return false;
    }

    private RuntimeException handleFailure(String operation, SQLException cause) {
        LOG.error("Failed to {}", operation, cause);
        try {
            rollbackHandler.rollbackCurrentTransaction();
        } catch (SQLException rollbackException) {
            cause.addSuppressed(rollbackException);
            LOG.error("Failed to rollback supplier transaction after {}", operation, rollbackException);
        }
        return new IllegalStateException("Failed to " + operation + ".", cause);
    }
}
