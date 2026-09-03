package org.fribok.bookkeeping.api;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Repository for company catalog API reads.
 */
public final class CompanyCatalogRepository {
    private final Connection connection;

    public CompanyCatalogRepository(Connection connection) {
        if (connection == null) {
            throw new NullPointerException("connection must not be null");
        }
        this.connection = connection;
    }

    public List<String> getCompanyNames() throws SQLException {
        List<String> companies = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT company_name FROM PUBLIC.tbl_company_catalog ORDER BY company_name");
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                String companyName = resultSet.getString("company_name");
                if (companyName != null) {
                    companies.add(companyName);
                }
            }
        }
        return companies;
    }
}
