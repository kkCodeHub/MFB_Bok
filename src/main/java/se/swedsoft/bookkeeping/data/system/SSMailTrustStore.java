package se.swedsoft.bookkeeping.data.system;

import org.fribok.bookkeeping.app.Path;
import org.fribok.bookkeeping.data.util.ConnectionSecurity;
import se.swedsoft.bookkeeping.data.util.SSMailServer;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Helper methods for inspecting and storing SMTP trust material in the app-specific truststore.
 */
public final class SSMailTrustStore {

    private static final String TRUSTSTORE_DIR = "mail";
    private static final String TRUSTSTORE_NAME = "fribok-mail-truststore.p12";
    private static final char[] TRUSTSTORE_PASSWORD = "changeit".toCharArray();
    private static final int SMTP_TEST_TIMEOUT_MS = 20_000;

    private SSMailTrustStore() {
    }

    /**
     * Returns the app-specific mail truststore file.
     *
     * @return the truststore file
     */
    public static File getTrustStoreFile() {
        return new File(new File(Path.get(Path.USER_CONF), TRUSTSTORE_DIR), TRUSTSTORE_NAME);
    }

    /**
     * Inspects the certificate chain presented by the SMTP server.
     *
     * @param server the mail server to inspect
     * @return the certificate info
     * @throws IOException if the connection fails
     * @throws GeneralSecurityException if TLS setup fails
     */
    public static MailCertificateInfo inspect(SSMailServer server) throws IOException, GeneralSecurityException {
        if (server == null || server.getURI() == null) {
            throw new IllegalArgumentException("server");
        }

        String host = server.getURI().getHost();
        int port = server.getURI().getPort();
        if (port <= 0) {
            port = server.isSSL() ? 465 : 587;
        }

        ConnectionSecurity connectionSecurity = server.getConnectionSecurity();
        if (connectionSecurity == ConnectionSecurity.SSL_TLS || server.isSSL()) {
            return inspectImplicitTls(host, port, connectionSecurity);
        }
        if (connectionSecurity == ConnectionSecurity.STARTTLS || server.isStartTLS()) {
            return inspectStartTls(host, port, connectionSecurity);
        }

        throw new IllegalArgumentException("Mail server does not use TLS");
    }

    /**
     * Imports the inspected certificate chain into the app-specific truststore.
     *
     * @param info the certificate info
     * @throws IOException if the truststore cannot be written
     * @throws GeneralSecurityException if the truststore cannot be loaded
     */
    public static void importCertificates(MailCertificateInfo info) throws IOException, GeneralSecurityException {
        if (info == null || info.getCertificates().isEmpty()) {
            throw new IllegalArgumentException("info");
        }

        File trustStoreFile = getTrustStoreFile();
        File parent = trustStoreFile.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs() && !parent.exists()) {
            throw new IOException("Unable to create truststore directory: " + parent.getAbsolutePath());
        }

        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        if (trustStoreFile.isFile()) {
            try (FileInputStream inputStream = new FileInputStream(trustStoreFile)) {
                keyStore.load(inputStream, TRUSTSTORE_PASSWORD);
            }
        } else {
            keyStore.load(null, TRUSTSTORE_PASSWORD);
        }

        int index = 0;
        for (X509Certificate certificate : info.getCertificates()) {
            String alias = buildAlias(info.getHost(), certificate, index++);
            keyStore.setCertificateEntry(alias, certificate);
        }

        try (OutputStream outputStream = new FileOutputStream(trustStoreFile)) {
            keyStore.store(outputStream, TRUSTSTORE_PASSWORD);
        }
    }

    /**
     * Formats a human-readable description of the certificate chain.
     *
     * @param info the certificate info
     * @return the formatted description
     */
    public static String buildDescription(MailCertificateInfo info) {
        StringBuilder description = new StringBuilder();
        description.append(String.format(Locale.ROOT, "Server: %s:%d%n", info.getHost(), info.getPort()));
        if (info.getConnectionSecurity() != null) {
            description.append(String.format(Locale.ROOT, "Connection: %s%n", info.getConnectionSecurity().getName()));
        }

        int index = 1;
        for (X509Certificate certificate : info.getCertificates()) {
            description.append(String.format(Locale.ROOT, "%nCertificate %d:%n", index++));
            description.append(describeCertificate(certificate));
        }

        return description.toString();
    }

    /**
     * Returns a compact description of a certificate.
     *
     * @param certificate the certificate
     * @return the formatted certificate description
     */
    public static String describeCertificate(X509Certificate certificate) {
        StringBuilder description = new StringBuilder();
        description.append("  Subject: ").append(certificate.getSubjectX500Principal().getName()).append('\n');
        description.append("  Issuer: ").append(certificate.getIssuerX500Principal().getName()).append('\n');
        description.append("  Valid from: ").append(certificate.getNotBefore()).append('\n');
        description.append("  Valid until: ").append(certificate.getNotAfter()).append('\n');
        description.append("  SHA-256: ").append(fingerprintSha256(certificate)).append('\n');
        return description.toString();
    }

    private static MailCertificateInfo inspectImplicitTls(String host, int port, ConnectionSecurity connectionSecurity)
            throws IOException, GeneralSecurityException {
        SSLSocketFactory factory = createTrustAllContext().getSocketFactory();
        try (SSLSocket socket = (SSLSocket) factory.createSocket(host, port)) {
            socket.setUseClientMode(true);
            socket.startHandshake();
            return createInfo(host, port, connectionSecurity, socket.getSession().getPeerCertificates());
        }
    }

    private static MailCertificateInfo inspectStartTls(String host, int port, ConnectionSecurity connectionSecurity)
            throws IOException, GeneralSecurityException {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), SMTP_TEST_TIMEOUT_MS);
            socket.setSoTimeout(SMTP_TEST_TIMEOUT_MS);

            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.US_ASCII));
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.US_ASCII));

            readSmtpResponse(reader);
            writeCommand(writer, "EHLO fribok");
            readSmtpResponse(reader);
            writeCommand(writer, "STARTTLS");
            String startTlsResponse = readSmtpResponse(reader);
            if (!startTlsResponse.startsWith("220")) {
                throw new IOException("SMTP server did not accept STARTTLS: " + startTlsResponse);
            }

            SSLSocketFactory factory = createTrustAllContext().getSocketFactory();
            try (SSLSocket sslSocket = (SSLSocket) factory.createSocket(socket, host, port, true)) {
                sslSocket.setUseClientMode(true);
                sslSocket.startHandshake();
                return createInfo(host, port, connectionSecurity, sslSocket.getSession().getPeerCertificates());
            }
        }
    }

    private static SSLContext createTrustAllContext() throws GeneralSecurityException {
        TrustManager[] trustManagers = new TrustManager[] { new X509TrustManager() {
            @Override
            public void checkClientTrusted(X509Certificate[] chain, String authType) {
            }

            @Override
            public void checkServerTrusted(X509Certificate[] chain, String authType) {
            }

            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return new X509Certificate[0];
            }
        } };

        SSLContext context = SSLContext.getInstance("TLS");
        context.init(null, trustManagers, new SecureRandom());
        return context;
    }

    private static MailCertificateInfo createInfo(String host, int port, ConnectionSecurity connectionSecurity,
                                                  Certificate[] peerCertificates) throws GeneralSecurityException {
        List<X509Certificate> certificates = new ArrayList<>();
        if (peerCertificates != null) {
            for (Certificate certificate : peerCertificates) {
                if (certificate instanceof X509Certificate) {
                    certificates.add((X509Certificate) certificate);
                }
            }
        }

        if (certificates.isEmpty()) {
            throw new GeneralSecurityException("No X509 certificates were presented by the server");
        }

        return new MailCertificateInfo(host, port, connectionSecurity, Collections.unmodifiableList(certificates));
    }

    private static String buildAlias(String host, X509Certificate certificate, int index) throws GeneralSecurityException {
        String fingerprint = fingerprintSha256(certificate).replace(':', '-');
        return String.format(Locale.ROOT, "%s-%d-%s", sanitizeAliasPart(host), index, fingerprint);
    }

    private static String sanitizeAliasPart(String value) {
        if (value == null || value.isEmpty()) {
            return "mail";
        }

        StringBuilder sanitized = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (Character.isLetterOrDigit(ch) || ch == '-' || ch == '_') {
                sanitized.append(ch);
            } else {
                sanitized.append('_');
            }
        }
        return sanitized.toString();
    }

    private static void writeCommand(BufferedWriter writer, String command) throws IOException {
        writer.write(command);
        writer.write("\r\n");
        writer.flush();
    }

    private static String readSmtpResponse(BufferedReader reader) throws IOException {
        StringBuilder response = new StringBuilder();
        String line;

        do {
            line = reader.readLine();
            if (line == null) {
                throw new EOFException("Unexpected end of SMTP stream");
            }
            if (response.length() > 0) {
                response.append('\n');
            }
            response.append(line);
        } while (line.length() >= 4 && line.charAt(3) == '-');

        return response.toString();
    }

    private static String fingerprintSha256(X509Certificate certificate) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] fingerprint = digest.digest(certificate.getEncoded());
            StringBuilder hex = new StringBuilder(fingerprint.length * 3 - 1);
            for (int i = 0; i < fingerprint.length; i++) {
                if (i > 0) {
                    hex.append(':');
                }
                hex.append(String.format(Locale.ROOT, "%02X", fingerprint[i] & 0xFF));
            }
            return hex.toString();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Unable to calculate certificate fingerprint", e);
        }
    }

    /**
     * Certificate chain data presented by the SMTP server.
     */
    public static final class MailCertificateInfo {
        private final String host;
        private final int port;
        private final ConnectionSecurity connectionSecurity;
        private final List<X509Certificate> certificates;

        private MailCertificateInfo(String host, int port, ConnectionSecurity connectionSecurity,
                                    List<X509Certificate> certificates) {
            this.host = host;
            this.port = port;
            this.connectionSecurity = connectionSecurity;
            this.certificates = certificates;
        }

        /**
         * @return the server host
         */
        public String getHost() {
            return host;
        }

        /**
         * @return the server port
         */
        public int getPort() {
            return port;
        }

        /**
         * @return the connection security mode
         */
        public ConnectionSecurity getConnectionSecurity() {
            return connectionSecurity;
        }

        /**
         * @return the certificate chain
         */
        public List<X509Certificate> getCertificates() {
            return certificates;
        }

        /**
         * @return the first certificate in the chain
         */
        public X509Certificate getPrimaryCertificate() {
            return certificates.isEmpty() ? null : certificates.get(0);
        }
    }
}



