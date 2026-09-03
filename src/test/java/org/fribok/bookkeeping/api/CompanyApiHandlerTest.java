package org.fribok.bookkeeping.api;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CompanyApiHandlerTest {
    private Connection connection;

    @AfterEach
    void tearDown() throws SQLException {
        if (connection != null) {
            try (Statement statement = connection.createStatement()) {
                statement.execute("SHUTDOWN");
            }
            connection.close();
        }
    }

    @Test
    void shouldReturnCompaniesAsJsonForAuthorizedRequest() throws Exception {
        connection = DriverManager.getConnection("jdbc:hsqldb:mem:company_api_test;sql.syntax_pgs=true", "sa", "");
        try (Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE PUBLIC.tbl_company_catalog(company_name VARCHAR(255))");
            statement.execute("INSERT INTO PUBLIC.tbl_company_catalog(company_name) VALUES ('Acme AB')");
            statement.execute("INSERT INTO PUBLIC.tbl_company_catalog(company_name) VALUES ('Beta AB')");
        }

        CompanyCatalogRepository repository = new CompanyCatalogRepository(connection);
        CompanyCatalogService service = new CompanyCatalogService(repository);
        CompanyApiHandler handler = new CompanyApiHandler(service, "secret-key");

        TestHttpExchange exchange = new TestHttpExchange("GET", "/api/v1/companies");
        exchange.getRequestHeaders().add("Accept", "application/json");
        exchange.getRequestHeaders().add("X-API-Key", "secret-key");

        handler.handle(exchange);

        assertThat(exchange.getStatusCode()).isEqualTo(200);
        assertThat(exchange.getResponseBodyAsString()).contains("\"request\":\"Get Companies\"");
        assertThat(exchange.getResponseBodyAsString()).contains("\"companyName\":\"Acme AB\"");
        assertThat(exchange.getResponseBodyAsString()).contains("\"companyName\":\"Beta AB\"");
        assertThat(exchange.getResponseHeaders().get("Content-Type")).isEqualTo(List.of("application/json; charset=utf-8"));
    }

    private static final class TestHttpExchange extends HttpExchange {
        private final Headers requestHeaders = new Headers();
        private final Headers responseHeaders = new Headers();
        private final String method;
        private final URI uri;
        private final ByteArrayOutputStream responseBody = new ByteArrayOutputStream();
        private int statusCode = -1;

        private TestHttpExchange(String method, String path) {
            this.method = method;
            this.uri = URI.create(path);
        }

        @Override
        public Headers getRequestHeaders() {
            return requestHeaders;
        }

        @Override
        public Headers getResponseHeaders() {
            return responseHeaders;
        }

        @Override
        public URI getRequestURI() {
            return uri;
        }

        @Override
        public String getRequestMethod() {
            return method;
        }

        @Override
        public com.sun.net.httpserver.HttpContext getHttpContext() {
            return null;
        }

        @Override
        public void close() {
        }

        @Override
        public InputStream getRequestBody() {
            return new ByteArrayInputStream(new byte[0]);
        }

        @Override
        public OutputStream getResponseBody() {
            return responseBody;
        }

        @Override
        public void sendResponseHeaders(int rCode, long responseLength) {
            this.statusCode = rCode;
        }

        @Override
        public InetSocketAddress getRemoteAddress() {
            return null;
        }

        @Override
        public int getResponseCode() {
            return statusCode;
        }

        @Override
        public InetSocketAddress getLocalAddress() {
            return null;
        }

        @Override
        public String getProtocol() {
            return "HTTP/1.1";
        }

        @Override
        public Object getAttribute(String name) {
            return null;
        }

        @Override
        public void setAttribute(String name, Object value) {
        }

        @Override
        public void setStreams(InputStream i, OutputStream o) {
        }

        @Override
        public HttpPrincipal getPrincipal() {
            return null;
        }

        private int getStatusCode() {
            return statusCode;
        }

        private String getResponseBodyAsString() {
            return responseBody.toString(java.nio.charset.StandardCharsets.UTF_8);
        }
    }
}
