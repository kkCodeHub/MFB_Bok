package org.fribok.bookkeeping.api;

import java.sql.SQLException;
import java.util.List;

/**
 * Service for company catalog API operations.
 */
public final class CompanyCatalogService {
    private final CompanyCatalogRepository repository;

    public CompanyCatalogService(CompanyCatalogRepository repository) {
        if (repository == null) {
            throw new NullPointerException("repository must not be null");
        }
        this.repository = repository;
    }

    public List<String> getCompanies() throws SQLException {
        return repository.getCompanyNames();
    }
}
