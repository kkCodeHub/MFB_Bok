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

import se.swedsoft.bookkeeping.data.SSAddress;
import se.swedsoft.bookkeeping.data.SSCustomer;
import se.swedsoft.bookkeeping.data.SSNewCompany;
import se.swedsoft.bookkeeping.data.common.SSCurrency;
import se.swedsoft.bookkeeping.data.common.SSDeliveryTerm;
import se.swedsoft.bookkeeping.data.common.SSDeliveryWay;
import se.swedsoft.bookkeeping.data.common.SSPaymentTerm;
import se.swedsoft.bookkeeping.data.system.SSEventTriggerSyncContext;

/**
 * V2 customer repository backed by the normalized V2 schema.
 *
 * <p>Reads and writes customers directly against {@code tbl_customer}.
 * Rollback handling is delegated through an injected handler.</p>
 */
public class V2CustomerRepository {

    @FunctionalInterface
    public interface RollbackHandler {
        void rollbackCurrentTransaction() throws SQLException;
    }

    private static final Logger LOG = LoggerFactory.getLogger(V2CustomerRepository.class);

    private final Connection connection;
    private final Supplier<SSNewCompany> currentCompany;
    private final RollbackHandler rollbackHandler;

    /**
     * Creates a V2 customer repository with explicit DB dependencies.
     *
     * @param connection      the active database connection; must not be {@code null}
     * @param currentCompany  supplier for the current company; must not be {@code null}
     * @param rollbackHandler rollback delegate; must not be {@code null}
     */
    public V2CustomerRepository(Connection connection,
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
     * Returns all customers for the current company.
     *
     * @return list of customers; never {@code null}
     */
    public List<SSCustomer> findAll() {
        SSNewCompany company = currentCompany.get();
        if (company == null) {
            return Collections.emptyList();
        }
        try {
            List<SSCustomer> list = new LinkedList<>();
            Integer iMax = -1;
            while (true) {
                try (PreparedStatement st = connection.prepareStatement(
                        "SELECT * FROM tbl_customer WHERE companyid=? AND id>?")) {
                    st.setObject(1, company.getId());
                    st.setObject(2, iMax);
                    st.setMaxRows(1024);
                    try (ResultSet rs = st.executeQuery()) {
                        int count = 0;
                        while (rs.next()) {
                            iMax = rs.getInt(1);
                            list.add(mapCustomer(rs));
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
            throw handleFailure("load customers", e);
        }
    }

    /**
     * Finds a customer by its number (exact match, case-insensitive).
     *
     * @param customerNumber the customer number
     * @return optional customer
     */
    public Optional<SSCustomer> findByNumber(String customerNumber) {
        if (customerNumber == null) {
            return Optional.empty();
        }
        SSNewCompany company = currentCompany.get();
        if (company == null) {
            return Optional.empty();
        }
        try {
            try (PreparedStatement st = connection.prepareStatement(
                    "SELECT * FROM tbl_customer WHERE LOWER(number)=LOWER(?) AND companyid=?")) {
                st.setObject(1, customerNumber);
                st.setObject(2, company.getId());
                try (ResultSet rs = st.executeQuery()) {
                    if (rs.next()) {
                        return Optional.of(mapCustomer(rs));
                    }
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw handleFailure("find customer by number '" + customerNumber + "'", e);
        }
    }

    /**
     * Finds a customer matching the given customer's number.
     *
     * @param customer the customer to look up
     * @return optional customer
     */
    public Optional<SSCustomer> findByCustomer(SSCustomer customer) {
        if (customer == null) {
            return Optional.empty();
        }
        SSNewCompany company = currentCompany.get();
        if (company == null) {
            return Optional.empty();
        }
        try {
            try (PreparedStatement st = connection.prepareStatement(
                    "SELECT * FROM tbl_customer WHERE number=? AND companyid=?")) {
                st.setObject(1, customer.getNumber());
                st.setObject(2, company.getId());
                try (ResultSet rs = st.executeQuery()) {
                    if (rs.next()) {
                        return Optional.of(mapCustomer(rs));
                    }
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw handleFailure("find customer '" + customer.getNumber() + "'", e);
        }
    }

    /**
     * Filters a list of customers against those that exist in the database.
     *
     * @param customers the subset to filter
     * @return filtered list; never {@code null}
     */
    public List<SSCustomer> findAll(List<SSCustomer> customers) {
        if (customers == null) {
            return Collections.emptyList();
        }
        SSNewCompany company = currentCompany.get();
        if (company == null) {
            return Collections.emptyList();
        }
        try {
            List<SSCustomer> result = new LinkedList<>();
            for (SSCustomer c : customers) {
                try (PreparedStatement st = connection.prepareStatement(
                        "SELECT * FROM tbl_customer WHERE number=? AND companyid=?")) {
                    st.setObject(1, c.getNumber());
                    st.setObject(2, company.getId());
                    try (ResultSet rs = st.executeQuery()) {
                        if (rs.next()) {
                            result.add(mapCustomer(rs));
                        }
                    }
                }
            }
            return result;
        } catch (SQLException e) {
            throw handleFailure("filter customers", e);
        }
    }

    /**
     * Inserts a new customer.
     *
     * @param customer the customer to add; ignored if {@code null}
     */
    public void add(SSCustomer customer) {
        if (customer == null) {
            return;
        }
        SSNewCompany company = currentCompany.get();
        if (company == null) {
            return;
        }
        try {
            try (PreparedStatement st = connection.prepareStatement(
                    "INSERT INTO tbl_customer(" +
                            "number,companyid,name,email,phone,phone2,telefax,registration_number," +
                            "our_contact,your_contact,vat_number,bankgiro,plusgiro,account_number," +
                            "clearing_number,eu_sale_commodity,eu_sale_third_part,vat_free_sale," +
                            "hide_unitprice,credit_limit,discount,comment,currency_code,payment_term," +
                            "delivery_term,delivery_way,inv_addr_name,inv_addr_address,inv_addr_street," +
                            "inv_addr_zipcode,inv_addr_city,inv_addr_country,del_addr_name,del_addr_address," +
                            "del_addr_street,del_addr_zipcode,del_addr_city,del_addr_country) " +
                            "VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)")) {
                int i = bindCustomer(st, 1, customer, company);
                st.executeUpdate();
                connection.commit();
                SSEventTriggerSyncContext.triggerAction("NEWCUSTOMER", "TBL_CUSTOMER", customer.getNumber());
            }
        } catch (SQLException e) {
            throw handleFailure("add customer '" + customer.getNumber() + "'", e);
        }
    }

    /**
     * Updates an existing customer.
     *
     * @param customer the customer to update; ignored if {@code null}
     */
    public void update(SSCustomer customer) {
        if (customer == null) {
            return;
        }
        SSNewCompany company = currentCompany.get();
        if (company == null) {
            return;
        }
        try {
            try (PreparedStatement st = connection.prepareStatement(
                    "UPDATE tbl_customer SET " +
                            "name=?,email=?,phone=?,phone2=?,telefax=?,registration_number=?," +
                            "our_contact=?,your_contact=?,vat_number=?,bankgiro=?,plusgiro=?," +
                            "account_number=?,clearing_number=?,eu_sale_commodity=?,eu_sale_third_part=?," +
                            "vat_free_sale=?,hide_unitprice=?,credit_limit=?,discount=?,comment=?," +
                            "currency_code=?,payment_term=?,delivery_term=?,delivery_way=?," +
                            "inv_addr_name=?,inv_addr_address=?,inv_addr_street=?,inv_addr_zipcode=?," +
                            "inv_addr_city=?,inv_addr_country=?,del_addr_name=?,del_addr_address=?," +
                            "del_addr_street=?,del_addr_zipcode=?,del_addr_city=?,del_addr_country=? " +
                            "WHERE number=? AND companyid=?")) {
                int i = 1;
                st.setObject(i++, customer.getName());
                st.setObject(i++, customer.getEMail());
                st.setObject(i++, customer.getPhone1());
                st.setObject(i++, customer.getPhone2());
                st.setObject(i++, customer.getTelefax());
                st.setObject(i++, customer.getRegistrationNumber());
                st.setObject(i++, customer.getOurContactPerson());
                st.setObject(i++, customer.getYourContactPerson());
                st.setObject(i++, customer.getVATNumber());
                st.setObject(i++, customer.getBankgiro());
                st.setObject(i++, customer.getPlusgiro());
                st.setObject(i++, customer.getAccountNumber());
                st.setObject(i++, customer.getClearingNumber());
                st.setObject(i++, customer.getEuSaleCommodity());
                st.setObject(i++, customer.getEuSaleYhirdPartCommodity());
                st.setObject(i++, customer.getTaxFree());
                st.setObject(i++, customer.getHideUnitprice());
                st.setObject(i++, customer.getCreditLimit());
                st.setObject(i++, customer.getDiscount());
                st.setObject(i++, customer.getComment());
                st.setObject(i++, currencyCode(customer));
                st.setObject(i++, customer.getPaymentTerm() == null ? null : customer.getPaymentTerm().getName());
                st.setObject(i++, customer.getDeliveryTerm() == null ? null : customer.getDeliveryTerm().getName());
                st.setObject(i++, customer.getDeliveryWay() == null ? null : customer.getDeliveryWay().getName());
                i = V2RepositoryHelpers.bindAddress(st, i, customer.getInvoiceAddress());
                i = V2RepositoryHelpers.bindAddress(st, i, customer.getDeliveryAddress());
                st.setObject(i++, customer.getNumber());
                st.setObject(i, company.getId());
                st.executeUpdate();
                connection.commit();
                SSEventTriggerSyncContext.triggerAction("EDITCUSTOMER", "TBL_CUSTOMER", customer.getNumber());
            }
        } catch (SQLException e) {
            throw handleFailure("update customer '" + customer.getNumber() + "'", e);
        }
    }

    /**
     * Deletes a customer.
     *
     * @param customer the customer to delete; ignored if {@code null}
     */
    public void delete(SSCustomer customer) {
        if (customer == null) {
            return;
        }
        SSNewCompany company = currentCompany.get();
        if (company == null) {
            return;
        }
        try {
            try (PreparedStatement st = connection.prepareStatement(
                    "DELETE FROM tbl_customer WHERE number=? AND companyid=?")) {
                st.setObject(1, customer.getNumber());
                st.setObject(2, company.getId());
                st.executeUpdate();
                connection.commit();
                SSEventTriggerSyncContext.triggerAction("DELETECUSTOMER", "TBL_CUSTOMER", customer.getNumber());
            }
        } catch (SQLException e) {
            if (isForeignKeyConstraintViolation(e)) {
                LOG.warn("Delete blocked for customer '{}' because it is in use", customer.getNumber());
                return;
            }
            throw handleFailure("delete customer '" + customer.getNumber() + "'", e);
        }
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private SSCustomer mapCustomer(ResultSet rs) throws SQLException {
        SSCustomer c = new SSCustomer();
        c.setNumber(rs.getString("number"));
        c.setName(rs.getString("name"));
        c.setEMail(rs.getString("email"));
        c.setPhone1(rs.getString("phone"));
        c.setPhone2(rs.getString("phone2"));
        c.setTelefax(rs.getString("telefax"));
        c.setRegistrationNumber(rs.getString("registration_number"));
        c.setOurContactPerson(rs.getString("our_contact"));
        c.setYourContactPerson(rs.getString("your_contact"));
        c.setVATNumber(rs.getString("vat_number"));
        c.setBankgiro(rs.getString("bankgiro"));
        c.setPlusgiro(rs.getString("plusgiro"));
        c.setAccountNumber(rs.getString("account_number"));
        c.setClearingNumber(rs.getString("clearing_number"));
        c.setEuSaleCommodity(rs.getBoolean("eu_sale_commodity"));
        c.setEuSaleYhirdPartCommodity(rs.getBoolean("eu_sale_third_part"));
        c.setTaxFree(rs.getBoolean("vat_free_sale"));
        c.setHideUnitprice(rs.getBoolean("hide_unitprice"));
        c.setCreditLimit(rs.getBigDecimal("credit_limit"));
        c.setDiscount(rs.getBigDecimal("discount"));
        c.setComment(rs.getString("comment"));

        String currencyCode = rs.getString("currency_code");
        if (currencyCode != null) {
            c.setInvoiceCurrency(new SSCurrency(currencyCode, currencyCode));
        }
        String paymentTerm = rs.getString("payment_term");
        if (paymentTerm != null) {
            c.setPaymentTerm(V2RepositoryHelpers.resolvePaymentTerm(paymentTerm));
        }
        String deliveryTerm = rs.getString("delivery_term");
        if (deliveryTerm != null) {
            c.setDeliveryTerm(new SSDeliveryTerm(deliveryTerm, deliveryTerm));
        }
        String deliveryWay = rs.getString("delivery_way");
        if (deliveryWay != null) {
            c.setDeliveryWay(new SSDeliveryWay(deliveryWay, deliveryWay));
        }
        c.setInvoiceAddress(V2RepositoryHelpers.mapAddress(rs, "inv_addr"));
        c.setDeliveryAddress(V2RepositoryHelpers.mapAddress(rs, "del_addr"));
        return c;
    }


    private int bindCustomer(PreparedStatement st, int i, SSCustomer customer, SSNewCompany company)
            throws SQLException {
        st.setObject(i++, customer.getNumber());
        st.setObject(i++, company.getId());
        st.setObject(i++, customer.getName());
        st.setObject(i++, customer.getEMail());
        st.setObject(i++, customer.getPhone1());
        st.setObject(i++, customer.getPhone2());
        st.setObject(i++, customer.getTelefax());
        st.setObject(i++, customer.getRegistrationNumber());
        st.setObject(i++, customer.getOurContactPerson());
        st.setObject(i++, customer.getYourContactPerson());
        st.setObject(i++, customer.getVATNumber());
        st.setObject(i++, customer.getBankgiro());
        st.setObject(i++, customer.getPlusgiro());
        st.setObject(i++, customer.getAccountNumber());
        st.setObject(i++, customer.getClearingNumber());
        st.setObject(i++, customer.getEuSaleCommodity());
        st.setObject(i++, customer.getEuSaleYhirdPartCommodity());
        st.setObject(i++, customer.getTaxFree());
        st.setObject(i++, customer.getHideUnitprice());
        st.setObject(i++, customer.getCreditLimit());
        st.setObject(i++, customer.getDiscount());
        st.setObject(i++, customer.getComment());
        st.setObject(i++, currencyCode(customer));
        st.setObject(i++, customer.getPaymentTerm() == null ? null : customer.getPaymentTerm().getName());
        st.setObject(i++, customer.getDeliveryTerm() == null ? null : customer.getDeliveryTerm().getName());
        st.setObject(i++, customer.getDeliveryWay() == null ? null : customer.getDeliveryWay().getName());
        i = V2RepositoryHelpers.bindAddress(st, i, customer.getInvoiceAddress());
        i = V2RepositoryHelpers.bindAddress(st, i, customer.getDeliveryAddress());
        return i;
    }

    private String currencyCode(SSCustomer customer) {
        try {
            SSCurrency c = customer.getInvoiceCurrency();
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
            LOG.error("Failed to rollback customer transaction after {}", operation, rollbackException);
        }
        return new IllegalStateException("Failed to " + operation + ".", cause);
    }
}
