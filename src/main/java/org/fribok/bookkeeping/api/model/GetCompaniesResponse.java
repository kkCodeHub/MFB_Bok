package org.fribok.bookkeeping.api.model;

import java.util.List;

/**
 * JSON payload for "Get Companies".
 *
 * <p>Requirements to call this API:
 * <ul>
 *   <li>Use HTTPS in front of this endpoint (direct TLS or reverse proxy termination).</li>
 *   <li>Use HTTP GET against <code>/api/v1/companies</code>.</li>
 *   <li>Send header <code>X-API-Key</code> with the configured API key.</li>
 *   <li>Send header <code>Accept: application/json</code>.</li>
 * </ul>
 * </p>
 */
public final class GetCompaniesResponse {
    private final String request;
    private final List<String> companies;

    public GetCompaniesResponse(String request, List<String> companies) {
        this.request = request;
        this.companies = companies;
    }

    public String getRequest() {
        return request;
    }

    public List<String> getCompanies() {
        return companies;
    }
}
