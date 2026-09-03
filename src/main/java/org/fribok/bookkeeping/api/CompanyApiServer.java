package org.fribok.bookkeeping.api;

import com.sun.net.httpserver.HttpServer;
import org.fribok.bookkeeping.app.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.swedsoft.bookkeeping.data.system.SSSystemConfigContext;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.Executors;

/**
 * HTTP API server for company catalog read operations.
 */
public final class CompanyApiServer {
    private static final Logger LOG = LoggerFactory.getLogger(CompanyApiServer.class);
    private static final String API_KEY_ENV = "FRIBOK_API_KEY";
    private static final String API_PORT_PROPERTY = "fribok.api.port";
    private static final String API_HOST_PROPERTY = "fribok.api.host";
    private static final String DEFAULT_API_KEY = "minhemliganyckel";
    private static final String DEFAULT_HOST = "127.0.0.1";
    private static final int DEFAULT_PORT = 8080;

    private final HttpServer httpServer;
    private final String host;
    private final int port;

    private CompanyApiServer(HttpServer httpServer, String host, int port) {
        if (httpServer == null) {
            throw new NullPointerException("httpServer must not be null");
        }
        this.httpServer = httpServer;
        this.host = host;
        this.port = port;
    }

    /**
     * Starts the API server using the active application DB connection.
     *
     * @param connection active database connection; must not be {@code null}
     * @return running API server instance
     * @throws IOException if the HTTP server cannot be started
     */
    public static CompanyApiServer start(Connection connection) throws IOException {
        if (connection == null) {
            throw new NullPointerException("connection must not be null");
        }
        String configuredApiKey = System.getenv(API_KEY_ENV);
        if (configuredApiKey == null || configuredApiKey.isBlank()) {
            configuredApiKey = DEFAULT_API_KEY;
            LOG.warn("FRIBOK_API_KEY is not set. Using fallback API key for local startup.");
        }
        String host = System.getProperty(API_HOST_PROPERTY, DEFAULT_HOST);
        int port = Integer.parseInt(System.getProperty(API_PORT_PROPERTY, String.valueOf(DEFAULT_PORT)));
        return start(connection, configuredApiKey, host, port);
    }

    /**
     * Starts the API server using explicit runtime settings.
     *
     * @param connection active database connection; must not be {@code null}
     * @param apiKey API key expected in X-API-Key; must not be blank
     * @param host host interface to bind; must not be blank
     * @param port TCP port to bind
     * @return running API server instance
     * @throws IOException if the HTTP server cannot be started
     */
    public static CompanyApiServer start(Connection connection, String apiKey, String host, int port) throws IOException {
        if (connection == null) {
            throw new NullPointerException("connection must not be null");
        }
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalArgumentException("apiKey must not be blank");
        }
        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException("host must not be blank");
        }
        if (port <= 0 || port > 65535) {
            throw new IllegalArgumentException("port must be between 1 and 65535");
        }

        CompanyCatalogRepository repository = new CompanyCatalogRepository(connection);
        CompanyCatalogService service = new CompanyCatalogService(repository);

        HttpServer server = HttpServer.create(new InetSocketAddress(host, port), 0);
        server.createContext("/api/v1/companies", new CompanyApiHandler(service, apiKey));
        server.setExecutor(Executors.newFixedThreadPool(4));
        server.start();

        LOG.info("Company API running on http://{}:{}/api/v1/companies", host, port);
        LOG.info("Configure HTTPS termination in front of this endpoint before external exposure.");
        return new CompanyApiServer(server, host, port);
    }

    /**
     * Stops the running API server.
     */
    public void stop() {
        httpServer.stop(0);
        LOG.info("Company API stopped on http://{}:{}/api/v1/companies", host, port);
    }

    public static void main(String[] args) {
        Connection connection = null;
        CompanyApiServer server = null;
        try {
            connection = startupDatabase();
            server = start(connection);

            Connection finalConnection = connection;
            CompanyApiServer finalServer = server;
            Runtime.getRuntime().addShutdownHook(new Thread(() -> shutdown(finalServer, finalConnection)));
        } catch (SQLException | IOException | RuntimeException e) {
            LOG.error("Failed to start Company API server", e);
            shutdown(server, connection);
            throw new RuntimeException(e);
        }
    }

    private static Connection startupDatabase() throws SQLException {
        loadHsqldbDriver();
        File userDataDir = Path.get(Path.USER_DATA);
        if (!userDataDir.exists() && !userDataDir.mkdirs()) {
            throw new SQLException("Unable to create user data directory: " + userDataDir.getAbsolutePath());
        }
        File dbDir = new File(userDataDir, "db");
        if (!dbDir.exists() && !dbDir.mkdirs()) {
            throw new SQLException("Unable to create db directory: " + dbDir.getAbsolutePath());
        }
        Connection connection = DriverManager.getConnection(
                "jdbc:hsqldb:file:" + dbDir.getAbsolutePath() + File.separator + "JFSDB", "sa", "");
        SSSystemConfigContext.startupLocal(connection);
        return connection;
    }

    private static void loadHsqldbDriver() throws SQLException {
        try {
            Class.forName("org.hsqldb.jdbc.JDBCDriver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("Unable to load HSQLDB driver", e);
        }
    }

    private static void shutdown(CompanyApiServer server, Connection connection) {
        if (server != null) {
            server.stop();
        }
        try {
            SSSystemConfigContext.shutdown();
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException e) {
                    LOG.warn("Failed to close API database connection", e);
                }
            }
        }
    }
}
