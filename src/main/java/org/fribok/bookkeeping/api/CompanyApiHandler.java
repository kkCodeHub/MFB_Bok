package org.fribok.bookkeeping.api;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.fribok.bookkeeping.api.model.ApiErrorResponse;
import org.fribok.bookkeeping.api.model.GetCompaniesResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.SQLException;
import java.net.URLDecoder;
import java.util.List;

/**
 * HTTP handler for the "Get Companies" API endpoint.
 */
public final class CompanyApiHandler implements HttpHandler {
    private static final Logger LOG = LoggerFactory.getLogger(CompanyApiHandler.class);
    private static final String REQUEST_NAME = "Get Companies";
    private static final String API_KEY_HEADER = "X-API-Key";
    private static final String JSON_CONTENT_TYPE = "application/json; charset=utf-8";
    private static final String ROUTE = "/api/v1/companies";

    private final CompanyCatalogService service;
    private final String expectedApiKey;

    public CompanyApiHandler(CompanyCatalogService service, String expectedApiKey) {
        if (service == null) {
            throw new NullPointerException("service must not be null");
        }
        this.service = service;
        this.expectedApiKey = expectedApiKey;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            if (!ROUTE.equals(exchange.getRequestURI().getPath())) {
                writeError(exchange, 404, new ApiErrorResponse("NOT_FOUND", "Unknown route"));
                return;
            }

            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.getResponseHeaders().set("Allow", "GET");
                writeError(exchange, 405, new ApiErrorResponse("METHOD_NOT_ALLOWED", "Only GET is supported"));
                return;
            }

            String requestName = getQueryParameter(exchange.getRequestURI().getRawQuery(), "request");
            if (requestName != null && !REQUEST_NAME.equals(requestName)) {
                writeError(exchange, 400, new ApiErrorResponse("BAD_REQUEST", "request must be 'Get Companies'"));
                return;
            }

            String accept = exchange.getRequestHeaders().getFirst("Accept");
            if (accept != null && !accept.contains("application/json") && !accept.contains("*/*")) {
                writeError(exchange, 406, new ApiErrorResponse("NOT_ACCEPTABLE", "Only application/json is supported"));
                return;
            }

            if (expectedApiKey == null || expectedApiKey.isBlank()) {
                writeError(exchange, 503, new ApiErrorResponse("API_KEY_NOT_CONFIGURED", "API key is not configured"));
                return;
            }

            String providedApiKey = exchange.getRequestHeaders().getFirst(API_KEY_HEADER);
            LOG.info("Incoming API request: method={}, path={}, remote={}, apiKey={}",
                    exchange.getRequestMethod(),
                    exchange.getRequestURI(),
                    exchange.getRemoteAddress(),
                    providedApiKey);
            if (!isAuthorized(providedApiKey)) {
                exchange.getResponseHeaders().set("WWW-Authenticate", "ApiKey");
                writeError(exchange, 401, new ApiErrorResponse("UNAUTHORIZED", "Missing or invalid API key"));
                return;
            }

            List<String> companies = service.getCompanies();
            writeOk(exchange, new GetCompaniesResponse(REQUEST_NAME, companies));
        } catch (SQLException e) {
            LOG.error("Failed to fetch companies for API response", e);
            writeError(exchange, 500, new ApiErrorResponse("DATABASE_ERROR", "Unable to fetch company data"));
        } finally {
            exchange.close();
        }
    }

    private boolean isAuthorized(String providedApiKey) {
        if (providedApiKey == null || providedApiKey.isBlank()) {
            return false;
        }
        return MessageDigest.isEqual(
                providedApiKey.getBytes(StandardCharsets.UTF_8),
                expectedApiKey.getBytes(StandardCharsets.UTF_8));
    }

    private void writeOk(HttpExchange exchange, GetCompaniesResponse response) throws IOException {
        StringBuilder json = new StringBuilder();
        json.append("{\"request\":\"").append(escapeJson(response.getRequest())).append("\",\"companies\":[");
        List<String> companies = response.getCompanies();
        for (int index = 0; index < companies.size(); index++) {
            if (index > 0) {
                json.append(',');
            }
            json.append("{\"companyName\":\"").append(escapeJson(companies.get(index))).append("\"}");
        }
        json.append("]}");
        writeResponse(exchange, 200, json.toString());
    }

    private void writeError(HttpExchange exchange, int statusCode, ApiErrorResponse errorResponse) throws IOException {
        String json = "{\"error\":{\"code\":\"" + escapeJson(errorResponse.getCode())
                + "\",\"message\":\"" + escapeJson(errorResponse.getMessage()) + "\"}}";
        writeResponse(exchange, statusCode, json);
    }

    private void writeResponse(HttpExchange exchange, int statusCode, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", JSON_CONTENT_TYPE);
        exchange.sendResponseHeaders(statusCode, bytes.length);
        exchange.getResponseBody().write(bytes);
    }

    private String escapeJson(String input) {
        StringBuilder escaped = new StringBuilder();
        for (int index = 0; index < input.length(); index++) {
            char character = input.charAt(index);
            switch (character) {
                case '\\':
                    escaped.append("\\\\");
                    break;
                case '"':
                    escaped.append("\\\"");
                    break;
                case '\b':
                    escaped.append("\\b");
                    break;
                case '\f':
                    escaped.append("\\f");
                    break;
                case '\n':
                    escaped.append("\\n");
                    break;
                case '\r':
                    escaped.append("\\r");
                    break;
                case '\t':
                    escaped.append("\\t");
                    break;
                default:
                    if (character < 0x20) {
                        escaped.append(String.format("\\u%04x", (int) character));
                    } else {
                        escaped.append(character);
                    }
            }
        }
        return escaped.toString();
    }

    private String getQueryParameter(String query, String parameterName) {
        if (query == null || query.isBlank()) {
            return null;
        }
        String[] parts = query.split("&");
        for (String part : parts) {
            int separatorIndex = part.indexOf('=');
            if (separatorIndex <= 0) {
                continue;
            }
            String key = decode(part.substring(0, separatorIndex));
            if (parameterName.equals(key)) {
                return decode(part.substring(separatorIndex + 1));
            }
        }
        return null;
    }

    private String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }
}
