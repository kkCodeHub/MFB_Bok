package se.swedsoft.bookkeeping.persistence.v2;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import se.swedsoft.bookkeeping.data.SSAddress;
import se.swedsoft.bookkeeping.data.SSNewAccountingYear;
import se.swedsoft.bookkeeping.data.SSVoucher;
import se.swedsoft.bookkeeping.data.common.SSPaymentTerm;
import se.swedsoft.bookkeeping.data.system.SSCompanyYearContext;
import se.swedsoft.bookkeeping.persistence.Repositories;

/**
 * Shared, package-private helpers used by V2 repository classes.
 *
 * <p>All methods are static; this class is not instantiable.</p>
 */
public final class V2RepositoryHelpers {

    public V2RepositoryHelpers() {}

    /**
     * Maps address columns from a {@link ResultSet} row into an {@link SSAddress}.
     *
     * @param rs     the result set positioned at the current row
     * @param prefix column name prefix, e.g. {@code "inv_addr"} or {@code "del_addr"}
     * @return a populated {@link SSAddress}; never {@code null}
     * @throws SQLException on JDBC error
     */
    public static SSAddress mapAddress(ResultSet rs, String prefix) throws SQLException {
        SSAddress a = new SSAddress();
        a.setName(rs.getString(prefix + "_name"));
        a.setAddress1(rs.getString(prefix + "_address"));
        a.setAddress2(rs.getString(prefix + "_street"));
        a.setZipCode(rs.getString(prefix + "_zipcode"));
        a.setCity(rs.getString(prefix + "_city"));
        a.setCountry(rs.getString(prefix + "_country"));
        return a;
    }

    /**
     * Binds address fields to consecutive {@link PreparedStatement} parameters.
     *
     * @param st   the prepared statement
     * @param i    the 1-based parameter index to start at
     * @param addr the address to bind; {@code null} is treated as an empty address
     * @return the next unused parameter index
     * @throws SQLException on JDBC error
     */
    static int bindAddress(PreparedStatement st, int i, SSAddress addr) throws SQLException {
        SSAddress safe = addr == null ? new SSAddress() : addr;
        st.setObject(i++, safe.getName());
        st.setObject(i++, safe.getAddress1());
        st.setObject(i++, safe.getAddress2());
        st.setObject(i++, safe.getZipCode());
        st.setObject(i++, safe.getCity());
        st.setObject(i++, safe.getCountry());
        return i;
    }

    /**
     * Resolves voucher row id for the given voucher number in current accounting year.
     *
     * @param connection active JDBC connection
     * @param voucher voucher carrying the voucher number
     * @return voucher row id, or {@code null} if not found
     * @throws SQLException on JDBC error
     */
    static Integer getVoucherIdByNumber(Connection connection, SSVoucher voucher) throws SQLException {
        SSNewAccountingYear currentYear = SSCompanyYearContext.getCurrentYear();
        if (connection == null || voucher == null || currentYear == null || currentYear.getId() == null) {
            return null;
        }

        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT id FROM tbl_voucher WHERE number=? AND yearid=?")) {
            statement.setObject(1, voucher.getNumber());
            statement.setObject(2, currentYear.getId());
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt(1);
                }
                return null;
            }
        }
    }

    /**
     * Resolves voucher number for a voucher row id.
     *
     * @param connection active JDBC connection
     * @param voucherId voucher row id
     * @return voucher number, or {@code null} if not found
     * @throws SQLException on JDBC error
     */
    static Integer getVoucherNumberForId(Connection connection, Integer voucherId) throws SQLException {
        if (connection == null || voucherId == null) {
            return null;
        }

        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT number FROM tbl_voucher WHERE id=?")) {
            statement.setObject(1, voucherId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt(1);
                }
                return null;
            }
        }
    }

    static SSPaymentTerm resolvePaymentTerm(String paymentTermName) {
        if (paymentTermName == null) {
            return null;
        }
        return Repositories.paymentTerms().findByName(paymentTermName)
                .orElseGet(() -> new SSPaymentTerm(paymentTermName, paymentTermName));
    }
}
