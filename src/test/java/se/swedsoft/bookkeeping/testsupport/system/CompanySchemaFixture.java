package se.swedsoft.bookkeeping.testsupport.system;

import se.swedsoft.bookkeeping.data.SSNewCompany;
import se.swedsoft.bookkeeping.data.system.SSCompanyYearContext;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public final class CompanySchemaFixture {

    private CompanySchemaFixture() {}

    public static Integer createCompany(Connection connection, String companyName) throws SQLException {
        SSNewCompany company = new SSNewCompany();
        company.setName(companyName);
        SSCompanyYearContext.addCompany(company);
        if (company.getId() == null) {
            throw new IllegalStateException("Could not create company via company-schema fixture");
        }
        return company.getId();
    }

    public static String currentSchema(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("VALUES CURRENT_SCHEMA");
             ResultSet resultSet = statement.executeQuery()) {
            if (resultSet.next()) {
                return resultSet.getString(1);
            }
            return null;
        }
    }

    public static int countCatalogRows(Connection connection, Integer companyId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT COUNT(*) FROM PUBLIC.tbl_company_catalog WHERE catalog_id=?")) {
            statement.setObject(1, companyId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt(1);
                }
                return 0;
            }
        }
    }
}
